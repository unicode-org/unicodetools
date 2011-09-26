package org.unicode.props;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.Timer;
import org.unicode.idna.Regexes;
import org.unicode.text.UCD.Default;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.test.util.Relation;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.dev.test.util.UnicodeProperty;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

/**
 * TODO StandardizedVariants and NameSequences*
 * @author markdavis
 *
 */
public class IndexUnicodeProperties {
    public final static Pattern SEMICOLON = Pattern.compile("\\s*;\\s*");
    public final static Pattern TAB = Pattern.compile("[ ]*\t[ ]*");
    private static final boolean SHOW_PROP_INFO = false;
    private static final boolean SHOW_LOADED = false;

    enum FileType {Field, HackField, PropertyValue, List, CJKRadicals}
    enum SpecialProperty {None, Skip1FT, Skip1ST, SkipAny4}

    static class PropertyInfo implements Comparable<PropertyInfo>{

        public PropertyInfo(String... propertyInfo) {
            this.file = propertyInfo[0];
            this.property = UcdProperty.forString(propertyInfo[1]);
            int temp = 1;
            try {
                temp = Integer.parseInt(propertyInfo[2]);
            } catch (Exception e) {}
            this.fieldNumber = temp;
            this.special = propertyInfo.length < 4 
            ? SpecialProperty.None : SpecialProperty.valueOf(propertyInfo[3]);
        }
        public String toString() {
            return file + " ; " + property + " ; " + fieldNumber;
        }
        final String file;
        final UcdProperty property;
        final int fieldNumber;
        final SpecialProperty special;
        //UnicodeMap<String> data;
        //final Set<String> errors = new LinkedHashSet<String>();
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

        public void put(UnicodeMap<String> data, IntRange intRange, String string) {
            //            switch (property.getType()) {
            //            case Enumerated:
            //            case Binary:
            //                Enum item = property.getEnum(string);
            //                if (item == null) {
            //                    System.out.println(property + "\tBad enum value: " + string);
            //                }
            //                break;
            //            }
            if (intRange.string != null) {
                putNew(data, intRange.string, string);
            } else {
                for (int codepoint = intRange.start; codepoint <= intRange.end; ++codepoint) {
                    try {
                        putNew(data, codepoint, string);
                    } catch (Exception e) {
                        System.err.println(property + ":\t" + e.getMessage());
                    }
                }
            }
        }
    }

    static Map<String, IndexUnicodeProperties> version2IndexUnicodeProperties = new HashMap();

    static EnumMap<UcdProperty, PropertyInfo> property2PropertyInfo = new EnumMap<UcdProperty,PropertyInfo>(UcdProperty.class);

    static Relation<String,PropertyInfo> file2PropertyInfoSet = Relation.of(new HashMap<String,Set<PropertyInfo>>(), HashSet.class);
    static Map<String,FileType> file2Type = new HashMap<String,FileType>();

    static {
        Matcher semicolon = SEMICOLON.matcher("");
        for (String line : FileUtilities.in(IndexUnicodeProperties.class, "IndexUnicodeProperties.txt")) {
            if (line.startsWith("#") || line.isEmpty()) continue;

            String[] parts = Regexes.split(semicolon, line.trim());
            // file ; property name ; derivation info
            //System.out.println(Arrays.asList(parts));
            if (parts[0].equals("FileType")) {
                putNew(file2Type, parts[1], FileType.valueOf(parts[2]));
            } else {
                try {
                    final UcdProperty prop = UcdProperty.forString(parts[1]);
                    final PropertyInfo propertyInfo = new PropertyInfo(parts);
                    putNew(property2PropertyInfo, prop, propertyInfo);
                    file2PropertyInfoSet.put(parts[0], propertyInfo);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Can't find property for <" + parts[1] + ">", e);
                }
            }
        }
        for (String file : new TreeSet<String>(file2PropertyInfoSet.keySet())) {
            Set<PropertyInfo> props = file2PropertyInfoSet.getAll(file);
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

    static final <K, V, M extends Map<K,V>> M putNew(M map, K key, V value) {
        V oldValue = map.get(key);
        if (oldValue != null) {
            throw new IllegalArgumentException("Key already present in Map: " + key + ",\told: " + oldValue + ",\tnew: " + value);
        }
        map.put(key, value);
        return map;
    }

    static final <V> UnicodeMap<V> putNew(UnicodeMap<V> map, int key, V value) {
        V oldValue = map.get(key);
        if (oldValue != null) {
            throw new IllegalArgumentException("Key already present in UnicodeMap: " + Utility.hex(key) + ",\told: " + oldValue + ",\tnew: " + value);
        }
        map.put(key, value);
        return map;
    }

    static final <V> UnicodeMap<V> putNew(UnicodeMap<V> map, String key, V value) {
        V oldValue = map.get(key);
        if (oldValue != null) {
            throw new IllegalArgumentException("Key already present in UnicodeMap: " + Utility.hex(key) + ",\told: " + oldValue + ",\tnew: " + value);
        }
        map.put(key, value);
        return map;
    }

    static final <V, C extends Collection<V>> C addNew(C collection, V value) {
        if (collection.contains(value)) {
            throw new IllegalArgumentException("Value already present in Collection: " + value);
        }
        collection.add(value);
        return collection;
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
        String fullFilename = Utility.getMostRecentUnicodeDataFile(fileInfo.file, ucdVersion, true, false);
        Set<PropertyInfo> propInfoSet = file2PropertyInfoSet.get(fileInfo.file);
        fileNames.add(fullFilename);

        FileType fileType = file2Type.get(fileInfo.file);
        if (fileType == null) {
            fileType = FileType.Field;
        }

        for (PropertyInfo propInfo : propInfoSet) {
            putNew(property2UnicodeMap, propInfo.property, new UnicodeMap<String>());
        }
        Matcher semicolon = SEMICOLON.matcher("");
        Matcher tab = TAB.matcher("");
        IntRange intRange = new IntRange();
        int lastCodepoint = 0;

        int lineCount = 0;
        boolean containsEOF = false;
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
            if (fileType != FileType.CJKRadicals) {
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
                        if ("FT".contains(parts[1])) {
                            continue;
                        }
                        break;
                    case Skip1FT: 
                        if ("ST".contains(parts[1])) {
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
                    propInfo.put(data, intRange, parts[propInfo.fieldNumber]);
                }
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
            System.out.println("Loaded: " + fileInfo.file + "\tlines: " + lineCount + (containsEOF ? "" : "\t*NO '# EOF'"));
        }
        for (PropertyInfo propInfo : propInfoSet) {
            UnicodeMap<String> data = property2UnicodeMap.get(propInfo.property);
            data.freeze();
        }
        return property2UnicodeMap.get(prop2);
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

    public static void main(String[] args) throws Exception {
        boolean SHOW = false;
        boolean ALL = true;
        Timer total = new Timer();
        for (Entry<String, PropertyInfo> entry : file2PropertyInfoSet.keyValueSet()) {
            if (SHOW_PROP_INFO) System.out.println(entry.getKey() + " ; " + entry.getValue());
        }
        IndexUnicodeProperties last = IndexUnicodeProperties.make("6.0.0");
        UnicodeMap<String> gcLast = showValue(last, UcdProperty.General_Category, '\u00A7');
        showValue(last, UcdProperty.kMandarin, '\u5427');
        showValue(last, UcdProperty.General_Category, '\u5427');

        IndexUnicodeProperties latest = IndexUnicodeProperties.make(Default.ucdVersion());
        showValue(latest, UcdProperty.General_Category, '\u00A7');
        showValue(latest, UcdProperty.kMandarin, '\u5427');

        UnicodeSet ignore = new UnicodeSet();
        addAll(ignore, gcLast.getSet(null)); // separate for debugging
        addAll(ignore, gcLast.getSet("Cn"));
        addAll(ignore, gcLast.getSet("Co"));
        addAll(ignore, gcLast.getSet("Cc"));
        addAll(ignore, gcLast.getSet("Cs"));
        
        UnicodeSet retain = new UnicodeSet(ignore).complement().freeze();

        //        compare(UcdProperty.General_Category, last, latest, retain);
        //
        //        latest.show(UcdProperty.General_Category);

        List<UcdProperty> values = ALL ? Arrays.asList(UcdProperty.values()) : 
            Arrays.asList(
                    UcdProperty.kMandarin
//                    ,UcdProperty.CJK_Radical, 
//                    UcdProperty.Script_Extensions,
//                    UcdProperty.Emoji_DoCoMo,
//                    UcdProperty.Emoji_KDDI,
//                    UcdProperty.Emoji_SoftBank
            );

        if (SHOW || !ALL) {
            for (UcdProperty prop : values) {
                latest.show(prop);
            }
            //        show("Joining_Type", timer);
            //        show("Joining_Group", timer);
            //        show("General_Category", timer);
            //        show("Canonical_Combining_Class", timer);        
            //        //show("Name", timer);        
            //        show("Bidi_Class", timer);        
            //        show("kMandarin", timer);  
        } 
        if (!SHOW) {
            for (UcdProperty prop : values) {
                compare(prop, last, latest, retain);
            }
        }
        Set<String> latestFiles = latest.fileNames;
        File dir = new File("/Users/markdavis/Documents/workspace/DATA/UCD/6.1.0-Update");
        checkFiles(latestFiles, dir);
        total.stop();
        System.out.println(total.toString());
    }

    private static void addAll(UnicodeSet toSet, UnicodeSet set) {
        if (set.contains('\u5427')) {
            int y = 3;
        }
        toSet.addAll(set);
    }

    public static UnicodeMap<String> showValue(IndexUnicodeProperties last, UcdProperty ucdProperty, int codePoint) {
        UnicodeMap<String> gcLast = last.load(ucdProperty);
        System.out.println(last.ucdVersion + ", " + ucdProperty + "(" + Utility.hex(codePoint) + ")=" + gcLast.get(codePoint));
        return gcLast;
    }

    public static void checkFiles(Set<String> latestFiles, File dir) throws IOException {
        for (File file : dir.listFiles()) {
            String canonical = file.getCanonicalPath();
            if (file.isDirectory()) {
                checkFiles(latestFiles, file);
                continue;
            } else if (latestFiles.contains(canonical) 
                    || !canonical.endsWith(".txt") 
                    || file.toString().contains("Test")
                    || file.toString().contains("NamesList")
                    || file.toString().contains("NormalizationCorrections")
                    || file.toString().contains("PropertyValueAliases")
                    || file.toString().contains("PropertyAliases")
                    || file.toString().contains("ReadMe")
            ) {
                continue;
            }
            System.out.println("Not read for properties: " + file);
        }
    }

    private static void compare(UcdProperty prop, IndexUnicodeProperties last, IndexUnicodeProperties latest, UnicodeSet retain) {
        UnicodeMap<String> changes = new UnicodeMap<String>();
        UnicodeMap<String> lastMap = last.load(prop);
        UnicodeMap<String> latestMap = latest.load(prop);
        for (UnicodeSetIterator it = new UnicodeSetIterator(retain); it.next();) {
            String lastValue = it.codepoint == UnicodeSetIterator.IS_STRING
            ? lastMap.get(it.string)
                    : lastMap.get(it.codepoint);
            if (lastValue == null) lastValue = "∅";
            String latestValue = latestMap.get(it.codepoint);
            if (latestValue == null) latestValue = "∅";
            if (UnicodeProperty.equals(lastValue, latestValue)) {
                continue;
            }
            changes.put(it.codepoint, abbreviate(lastValue, 50) + "\t→\t" + abbreviate(latestValue, 50));
        }
        if (changes.size() == 0) {
            System.out.println(prop + "\tNO_CHANGES");
            return;
        }
        for (String value : new TreeSet<String>(changes.values())) {
            final UnicodeSet chars = changes.getSet(value);
            String charString = abbreviate(chars.toString(), 50);
            System.out.println(prop + "\t" + value + "\t" + chars.toPattern(false) + "\t" + charString);
        }
    }

    public static String abbreviate(String charString, int maxLength) {
        if (charString.length() > maxLength) {
            charString = charString.substring(0,50) + "…";
        }
        return charString;
    }

    public void show(UcdProperty prop) {
        Timer timer = new Timer();
        System.out.println(prop);
        timer.start();
        UnicodeMap<String> map = load(prop);
        timer.stop();
        final Collection<String> values = map.values();
        String sample = abbreviate(values.toString(), 20);
        System.out.println("\ttime: " + timer + "\tcodepoints: " + map.size() + "\tvalues: " + values.size() + "\tsample: " + sample);
        //        for (String value : map.getAvailableValues()) {
        //            System.out.println("\t" + value + " " + map.getSet(value));
        //        }
    }
}
