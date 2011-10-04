package org.unicode.props;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.idna.Regexes;
import org.unicode.props.PropertyNames.PropertyType;
import org.unicode.props.PropertyUtilities.Joiner;
import org.unicode.props.PropertyUtilities.Merge;
import org.unicode.text.utility.Utility;

import sun.text.normalizer.UTF16;

import com.ibm.icu.dev.test.util.Relation;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.dev.test.util.UnicodeProperty;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import com.sun.jdi.InternalException;

/**
 * TODO StandardizedVariants and NameSequences*
 * @author markdavis
 *
 */
public class IndexUnicodeProperties extends UnicodeProperty.Factory {
    public final static Pattern SEMICOLON = Pattern.compile("\\s*;\\s*");
    public final static String FIELD_SEPARATOR = "␣";
    public final static Pattern TAB = Pattern.compile("[ ]*\t[ ]*");
    static final boolean SHOW_PROP_INFO = false;
    private static final boolean SHOW_LOADED = false;

    enum SpecialValue {CODEPOINT, Simple_Lowercase_Mapping, Simple_Titlecase_Mapping, Simple_Uppercase_Mapping}

    enum FileType {Field, HackField, PropertyValue, List, CJKRadicals, NamedSequences, NameAliases}
    enum SpecialProperty {None, Skip1FT, Skip1ST, SkipAny4}

    static final Joiner JOIN = new Joiner(FIELD_SEPARATOR);

    static class PropertyInfo implements Comparable<PropertyInfo>{
        final String file;
        final UcdProperty property;
        final int fieldNumber;
        final SpecialProperty special;
        final String defaultValue;
        //UnicodeMap<String> data;
        //final Set<String> errors = new LinkedHashSet<String>();

        public PropertyInfo(String... propertyInfo) {
            this.file = propertyInfo[0];
            this.property = UcdProperty.forString(propertyInfo[1]);
            int temp = 1;
            try {
                temp = Integer.parseInt(propertyInfo[2]);
            } catch (Exception e) {}
            this.fieldNumber = temp;
            this.special = propertyInfo.length < 4 || propertyInfo[3].isEmpty()
            ? SpecialProperty.None 
                    : SpecialProperty.valueOf(propertyInfo[3]);
            if (propertyInfo.length < 5 || propertyInfo[4].isEmpty()) {
                this.defaultValue = 
                    property.getType() == PropertyType.String ? SpecialValue.CODEPOINT.toString() 
                            : property.getType() == PropertyType.Binary ? "No".intern() 
                                    : null;
            } else {
                String tempString = null;
                try {
                    SpecialValue tempValue = SpecialValue.valueOf(propertyInfo[4]);
                    tempString = tempValue.toString();
                } catch (Exception e) {
                    tempString = propertyInfo[4].intern();
                }
                this.defaultValue = tempString;
            }
        }
        public String toString() {
            return file + " ; " + property + " ; " + fieldNumber;
        }
        @Override
        public int compareTo(PropertyInfo arg0) {
            int result;
            if (0 != (result = file.compareTo(arg0.file))) {
                return result;
            }
            if (0 != (result = property.toString().compareTo(arg0.property.toString()))) {
                return result;
            }
            return fieldNumber - arg0.fieldNumber;
        }

        public void put(UnicodeMap<String> data, IntRange intRange, String string, Merge<String> merger) {
            //            switch (property.getType()) {
            //            case Enumerated:
            //            case Binary:
            //                Enum item = property.getEnum(string);
            //                if (item == null) {
            //                    System.out.println(property + "\tBad enum value: " + string);
            //                }
            //                break;
            //            }
            if (property.getType().equals(PropertyType.String) && !string.isEmpty()) {
                string = Utility.fromHex(string);
            }
            if (intRange.string != null) {
                PropertyUtilities.putNew(data, intRange.string, string, JOIN);
            } else {
                for (int codepoint = intRange.start; codepoint <= intRange.end; ++codepoint) {
                    try {
                        PropertyUtilities.putNew(data, codepoint, string, JOIN);
                    } catch (Exception e) {
                        System.err.println(property + ":\t" + e.getMessage());
                    }
                }
            }
        }
        public void put(UnicodeMap<String> data, IntRange intRange, String string) {
            put(data, intRange, string, null);
        }
    }

    static Map<String, IndexUnicodeProperties> version2IndexUnicodeProperties = new HashMap();

    static EnumMap<UcdProperty, PropertyInfo> property2PropertyInfo = new EnumMap<UcdProperty,PropertyInfo>(UcdProperty.class);

    private static Relation<String,PropertyInfo> file2PropertyInfoSet = Relation.of(new HashMap<String,Set<PropertyInfo>>(), HashSet.class);

    static Map<String,FileType> file2Type = new HashMap<String,FileType>();

    static {
        Matcher semicolon = SEMICOLON.matcher("");
        for (String line : FileUtilities.in(IndexUnicodeProperties.class, "IndexUnicodeProperties.txt")) {
            if (line.startsWith("#") || line.isEmpty()) continue;

            String[] parts = Regexes.split(semicolon, line.trim());
            // file ; property name ; derivation info
            //System.out.println(Arrays.asList(parts));
            if (parts[0].equals("FileType")) {
                PropertyUtilities.putNew(file2Type, parts[1], FileType.valueOf(parts[2]));
            } else {
                final UcdProperty prop = UcdProperty.forString(parts[1]);
                final PropertyInfo propertyInfo = new PropertyInfo(parts);
                try {
                    PropertyUtilities.putNew(property2PropertyInfo, prop, propertyInfo);
                    getFile2PropertyInfoSet().put(parts[0], propertyInfo);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Can't find property for <" + parts[1] + ">", e);
                }
            }
        }
        for (String file : new TreeSet<String>(getFile2PropertyInfoSet().keySet())) {
            Set<PropertyInfo> props = getFile2PropertyInfoSet().getAll(file);
            for (PropertyInfo prop : new TreeSet<PropertyInfo>(props)) {
                if (SHOW_PROP_INFO) System.out.println(prop);
            }
        }
        //        for (UcdProperty x : UcdProperty.values()) {
        //            if (property2PropertyInfo.containsKey(x.toString())) continue;
        //            if (SHOW_PROP_INFO) System.out.println("Missing: " + x);
        //        }
    }

    private IndexUnicodeProperties(String ucdVersion2) {
        ucdVersion = ucdVersion2;
        oldVersion = ucdVersion2.compareTo("6.1.0") < 0;
    }

    public static final IndexUnicodeProperties make(String ucdVersion) {
        IndexUnicodeProperties newItem = version2IndexUnicodeProperties.get(ucdVersion);
        if (newItem == null) {
            version2IndexUnicodeProperties.put(ucdVersion, newItem = new IndexUnicodeProperties(ucdVersion));
        }
        return newItem;
    }

    final String ucdVersion;
    final boolean oldVersion;
    final EnumMap<UcdProperty, UnicodeMap<String>> property2UnicodeMap = new EnumMap<UcdProperty, UnicodeMap<String>>(UcdProperty.class);
    final Set<String> fileNames = new TreeSet<String>();

    static final Transform<String, String>    fromNumericPinyin   = Transliterator.getInstance("NumericPinyin-Latin;nfc");

    public UnicodeMap<String> load(UcdProperty prop2) {
        UnicodeMap<String> data0 = property2UnicodeMap.get(prop2);
        if (data0 != null) {
            return data0;
        }
        PropertyInfo fileInfo = property2PropertyInfo.get(prop2);
        Set<PropertyInfo> propInfoSet = getFile2PropertyInfoSet().get(fileInfo.file);

        FileType fileType = file2Type.get(fileInfo.file);
        if (fileType == null) {
            fileType = FileType.Field;
        }

        for (PropertyInfo propInfo : propInfoSet) {
            PropertyUtilities.putNew(property2UnicodeMap, propInfo.property, new UnicodeMap<String>());
        }

        String fullFilename = Utility.getMostRecentUnicodeDataFile(fileInfo.file, ucdVersion, true, false);
        // if a file is not in a given version of Unicode, we skip it.
        if (fullFilename == null) {
            if (SHOW_LOADED) {
                System.out.println("Version\t" + ucdVersion + "\tFile doesn't exist: " + fileInfo.file);
            }
        } else {
            fileNames.add(fullFilename);

            Matcher semicolon = SEMICOLON.matcher("");
            Matcher tab = TAB.matcher("");
            IntRange intRange = new IntRange();
            int lastCodepoint = 0;

            int lineCount = 0;
            boolean containsEOF = false;
            Merge<String> merger = null;
            for (String line : FileUtilities.in("", fullFilename)) {
                ++lineCount;
                if (line.contains("9FCB")) {
                    int y = 3;
                }
                int hashPos = line.indexOf('#');
                if (hashPos >= 0) {
                    if (line.contains("# EOF")) {
                        containsEOF = true;
                    }
                    line = line.substring(0,hashPos);
                }
                line = line.trim();
                if (line.isEmpty()) continue;
                //HACK
                String[] parts = Regexes.split(line.startsWith("U+") ? tab : semicolon, line.trim());
                //System.out.println(line + "\t\t" + Arrays.asList(parts));
                // HACK RANGE
                // 3400;<CJK Ideograph Extension A, First>;Lo;0;L;;;;;N;;;;;
                // 4DB5;<CJK Ideograph Extension A, Last>;Lo;0;L;;;;;N;;;;;
                // U+4F70   kAccountingNumeric  100
                /*
                 * CJKRadicals The first field is the # radical number. The second
                 * field is the CJK Radical character. The third # field is the CJK
                 * Unified Ideograph. 1; 2F00; 4E00
                 */
                if (fileType != FileType.CJKRadicals && fileType != FileType.NamedSequences) {
                    intRange.set(parts[0]);
                }
                switch(fileType) {
                case CJKRadicals: {
                    PropertyInfo propInfo = property2PropertyInfo.get(UcdProperty.CJK_Radical);
                    UnicodeMap<String> data = property2UnicodeMap.get(propInfo.property);
                    intRange.set(parts[1]);
                    propInfo.put(data, intRange, parts[0]); 
                    intRange.set(parts[2]);
                    propInfo.put(data, intRange, parts[0]); 
                    break;
                }
                case NamedSequences: {
                    for (PropertyInfo propInfo : propInfoSet) {
                        UnicodeMap<String> data = property2UnicodeMap.get(propInfo.property);
                        intRange.set(parts[1]);
                        propInfo.put(data, intRange, parts[0]); 
                    }
                    break;
                }
                case PropertyValue: {
                    PropertyInfo propInfo = property2PropertyInfo.get(UcdProperty.forString(parts[1]));
                    UnicodeMap<String> data = property2UnicodeMap.get(propInfo.property);
                    if (!propInfoSet.contains(propInfo)) {
                        throw new IllegalArgumentException("Property not listed for file: " + propInfo);
                    }
                    switch(propInfo.property.getType()) {
                    case Binary:
                        propInfo.put(data, intRange, "Yes"); break;
                    default:
                        String value = parts[2];
                        if (propInfo.property == UcdProperty.kMandarin) {
                            if (oldVersion) {
                                value = fromNumericPinyin.transform(value.toLowerCase(Locale.ENGLISH));
                            }
                        }
                        propInfo.put(data, intRange, value); break;
                    }
                    break;
                }
                case NameAliases:
                    merger = JOIN;
                    //$FALL-THROUGH$
                case HackField: 
                    if (parts[1].endsWith("Last>")) {
                        intRange.start = lastCodepoint + 1;
                    }
                    if (parts[1].startsWith("<")) {
                        parts[1] = null;
                    }
                    lastCodepoint = intRange.end;
                    //$FALL-THROUGH$
                case Field:
                {
                    for (PropertyInfo propInfo : propInfoSet) {
                        UnicodeMap<String> data = property2UnicodeMap.get(propInfo.property);
                        switch(propInfo.special) {
                        case None: break;
                        case Skip1ST: 
                            if ("ST".contains(parts[1])) {
                                continue;
                            }
                            break;
                        case Skip1FT: 
                            if ("FT".contains(parts[1])) {
                                continue;
                            }
                            break;
                        case SkipAny4: 
                            if (!parts[4].isEmpty()) {
                                continue;
                            }
                            break;
                        default: throw new IllegalArgumentException();
                        }
                        String string = parts[propInfo.fieldNumber];
                        if (fileType == fileType.HackField && propInfo.fieldNumber == 5) {
                            int dtEnd = string.indexOf('>');
                            if (dtEnd >= 0) {
                                string = string.substring(dtEnd + 1).trim();
                            }
                        }
                        propInfo.put(data, intRange, string, merger);
                    }
                    merger = null;
                    break;
                }
                case List: {
                    if (propInfoSet.size() != 1) {
                        throw new IllegalArgumentException("List files must have only one property, and must be Boolean");
                    }
                    PropertyInfo propInfo = propInfoSet.iterator().next();
                    UnicodeMap<String> data = property2UnicodeMap.get(propInfo.property);
                    //prop.data.putAll(UnicodeSet.ALL_CODE_POINTS, "No");
                    propInfo.put(data, intRange, "Yes");
                    break;
                }
                default: throw new IllegalArgumentException();
                }
            }
            if (SHOW_LOADED) {
                System.out.println("Version\t" + ucdVersion + "\tLoaded: " + fileInfo.file + "\tlines: " + lineCount + (containsEOF ? "" : "\t*NO '# EOF'"));
            }
        }
        for (PropertyInfo propInfo : propInfoSet) {
            UnicodeMap<String> data = property2UnicodeMap.get(propInfo.property);
            if (propInfo.defaultValue != null) {
                UnicodeSet nullValues = data.getSet(null);
                data.putAll(nullValues, propInfo.defaultValue);
            }
            data.freeze();
        }
        return property2UnicodeMap.get(prop2);
    }

    static void setFile2PropertyInfoSet(Relation<String,PropertyInfo> file2PropertyInfoSet) {
        IndexUnicodeProperties.file2PropertyInfoSet = file2PropertyInfoSet;
    }

    static Relation<String,PropertyInfo> getFile2PropertyInfoSet() {
        return file2PropertyInfoSet;
    }

    private static class IntRange {
        int start;
        int end;
        String string;
        public IntRange set(String source) {
            if (source.startsWith("U+")) {
                source = source.substring(2);
            }
            int range = source.indexOf("..");
            if (range >= 0) {
                start = Integer.parseInt(source.substring(0,range),16);
                end = Integer.parseInt(source.substring(range+2),16);
                string = null;
            } else if (source.contains(" ")) {
                string = Utility.fromHex(source);
            } else {
                start = end = Integer.parseInt(source, 16);
                string = null;
            }
            return this;
        }
    }

    public static String getResolvedValue(IndexUnicodeProperties props, UcdProperty prop, int codepoint, String value) {
        if (value == null || value.isEmpty()) {
            value = property2PropertyInfo.get(prop).defaultValue;
        }
        if (props != null && (value == null || value.length() > 8)) {
            try {
                SpecialValue specialValue = SpecialValue.valueOf(value);
                switch (specialValue) {
                case CODEPOINT: 
                    return UTF16.valueOf(codepoint);
                case Simple_Lowercase_Mapping: 
                    return props.getResolvedValue(UcdProperty.Simple_Lowercase_Mapping, codepoint);
                case Simple_Titlecase_Mapping: 
                    return props.getResolvedValue(UcdProperty.Simple_Titlecase_Mapping, codepoint);
                case Simple_Uppercase_Mapping: 
                    return props.getResolvedValue(UcdProperty.Simple_Uppercase_Mapping, codepoint);
                default:
                    throw new InternalException();
                }
            } catch (Exception e) {}
        }
        return value;
    }

    public String getResolvedValue(UcdProperty prop, int codepoint) {
        return getResolvedValue(this, prop, codepoint, this.getRawValue(prop, codepoint));
    }
    
    public String getRawValue(UcdProperty ucdProperty, int codepoint) {
        return load(ucdProperty).get(codepoint);
    }

    //    static final class IndexUnicodeProperty extends UnicodeProperty.UnicodeMapProperty {
    //        
    //        private PropertyNames names;
    //        private String defaultValue;
    //        boolean initialized = false;
    //        
    //        IndexUnicodeProperty(PropertyNames names) {
    //            this.names = names;
    //        }
    //        
    //        protected String _getValue(int codepoint) {
    //            String lastValue = _getValue(codepoint);
    //            if (lastValue == SpecialValues.CODEPOINT.toString()) {
    //                return UTF16.valueOf(codepoint);
    //            }
    //            if (lastValue == null) {
    //                return defaultValue;
    //            }
    //            return lastValue;
    //        }
    //
    //        public List _getNameAliases(List result) { // TODO fix interface
    //            result.addAll(names.getAllNames());
    //            return result;
    //        }
    //    }
}
