package org.unicode.props;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.RegexUtilities;
import org.unicode.idna.Regexes;
import org.unicode.props.IndexUnicodeProperties.DefaultValueType;
import org.unicode.props.PropertyUtilities.Merge;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.locale.XCldrStub.Splitter;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.VersionInfo;

/**
 * @internal
 */
public class PropertyParsingInfo implements Comparable<PropertyParsingInfo>{
    static class IntRange {
        int start;
        int end;
        String string;
        public PropertyParsingInfo.IntRange set(String source) {
            if (source.startsWith("U+")) {
                source = source.substring(2);
            }
            final int range = source.indexOf("..");
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

    enum SpecialProperty {None, Skip1FT, Skip1ST, SkipAny4, Rational}

    private static final VersionInfo MIN_VERSION = VersionInfo.getInstance(0,0,0,0);
    public final String file;
    public final UcdProperty property;
    public final int fieldNumber;
    public final PropertyParsingInfo.SpecialProperty special;

    public String oldFile;
    public VersionInfo maxOldVersion = MIN_VERSION;

    String defaultValue;
    DefaultValueType defaultValueType = DefaultValueType.LITERAL;
    //UnicodeMap<String> data;
    //final Set<String> errors = new LinkedHashSet<String>();
    Pattern regex = null;
    private ValueCardinality multivalued = ValueCardinality.Singleton;
    private Pattern multivaluedSplit = PropertyParsingInfo.SPACE;
    public String originalRegex;
    public final static Pattern TAB = Pattern.compile("[ ]*\t[ ]*");
    static final Pattern MISSING_PATTERN = Pattern.compile(
    "\\s*#\\s*@(missing|empty):?" +
            "\\s*([A-Fa-f0-9]+)..([A-Fa-f0-9]+)\\s*;" +
            "\\s*([^;]*)" +
            "(?:\\s*;\\s*([^;]*)" +
            "(?:\\s*;\\s*([^;]*))?)?\\s*" +
            ";?"
    );
    static final Pattern SIMPLE_MISSING_PATTERN = Pattern.compile("\\s*#\\s*@(missing|empty)");
    static final String CONSTRUCTED_NAME = "$HANGUL_SYLLABLE$";
    static Pattern SLASHX = Pattern.compile("\\\\x\\{([0-9A-Fa-f]{1,6})\\}");
    public final static Pattern NO_SPLIT = Pattern.compile("\\uFFFF");
    public final static Pattern SPACE = Pattern.compile("\\s+");
    public final static Pattern EQUALS = Pattern.compile("\\s*=\\s*");
    public final static Pattern COMMA = Pattern.compile("\\s*,\\s*");
    public final static Pattern DECOMP_REMOVE = Pattern.compile("\\{[^}]+\\}|\\<[^>]+\\>");
    /**
     * General constants
     */
    public final static Pattern SEMICOLON = Pattern.compile("\\s*;\\s*");
    static EnumMap<UcdProperty, PropertyParsingInfo> property2PropertyInfo = new EnumMap<UcdProperty,PropertyParsingInfo>(UcdProperty.class);
    static Relation<String,PropertyParsingInfo> file2PropertyInfoSet = Relation.of(new HashMap<String,Set<PropertyParsingInfo>>(), HashSet.class);


    public PropertyParsingInfo(String file, UcdProperty property,
            int fieldNumber, PropertyParsingInfo.SpecialProperty special) {
        this.file = file;
        this.property = property;
        this.fieldNumber = fieldNumber;
        this.special = special;
    }

    static final Pattern VERSION = Pattern.compile("v\\d+\\.\\d+");
    
    public static void fromStrings(String... propertyInfo) {
        if (propertyInfo.length < 2 || propertyInfo.length > 4) {
            throw new UnicodePropertyException("Must have 2 to 4 args: " + Arrays.asList(propertyInfo));
        }
        String _file = propertyInfo[0];
        final String propName = propertyInfo[1];
        UcdProperty _property = UcdProperty.forString(propName);

        String last = propertyInfo[propertyInfo.length-1];
        if (VERSION.matcher(last).matches()) {
        	propertyInfo[propertyInfo.length-1] = "";
            PropertyParsingInfo result = PropertyParsingInfo.property2PropertyInfo.get(_property);
            result.oldFile = _file;
            result.maxOldVersion = VersionInfo.getInstance(last.substring(1));
            PropertyParsingInfo.getFile2PropertyInfoSet().put(_file, result);
            return;
        }
        
        int temp = 1;
        if (propertyInfo.length > 2 && !propertyInfo[2].isEmpty()) {
            temp = Integer.parseInt(propertyInfo[2]);
        }
        int _fieldNumber = temp;

        PropertyParsingInfo.SpecialProperty _special = propertyInfo.length < 4 || propertyInfo[3].isEmpty()
                ? PropertyParsingInfo.SpecialProperty.None
                        : PropertyParsingInfo.SpecialProperty.valueOf(propertyInfo[3]);
        PropertyParsingInfo result = new PropertyParsingInfo(_file, _property, _fieldNumber, _special);

        try {
            PropertyUtilities.putNew(PropertyParsingInfo.property2PropertyInfo, _property, result);
            PropertyParsingInfo.getFile2PropertyInfoSet().put(_file, result);
        } catch (final Exception e) {
            throw new UnicodePropertyException("Can't find property for <" + propName + ">", e);
        }
    }

    @Override
    public String toString() {
        return file + " ;\t"
                + property + " ;\t"
                + oldFile + " ;\t"
                + maxOldVersion + " ;\t"
                + fieldNumber + " ;\t"
                + special + " ;\t"
                + defaultValue + " ;\t"
                + defaultValueType + " ;\t"
                + getMultivalued() + " ;\t"
                + getRegex() + " ;\t"
                ;
    }
    @Override
    public int compareTo(PropertyParsingInfo arg0) {
        int result;
        if (0 != (result = file.compareTo(arg0.file))) {
            return result;
        }
        if (0 != (result = property.toString().compareTo(arg0.property.toString()))) {
            return result;
        }
        return fieldNumber - arg0.fieldNumber;
    }

    public String getFullFileName(VersionInfo ucdVersionRequested) {
        return Utility.getMostRecentUnicodeDataFile(
                getFileName(ucdVersionRequested), 
                ucdVersionRequested.toString(), 
                true, 
                false);
    }

    public String getFileName(VersionInfo ucdVersionRequested) {
        return useOldFile(ucdVersionRequested) ? oldFile : file;
    }

    public boolean useOldFile(VersionInfo ucdVersionRequested) {
        return ucdVersionRequested.compareTo(maxOldVersion) <= 0;
    }

    public void put(UnicodeMap<String> data, PropertyParsingInfo.IntRange intRange, String string, Merge<String> merger) {
        put(data, intRange, string, merger, false);
    }

    public static final Normalizer2 NFD = Normalizer2.getNFDInstance();
    public static final Normalizer2 NFC = Normalizer2.getNFCInstance();

    public void put(UnicodeMap<String> data, PropertyParsingInfo.IntRange intRange, String string, Merge<String> merger, boolean hackHangul) {
        if (string != null && string.isEmpty() && property != UcdProperty.NFKC_Casefold) {
            string = null;
        }
        string = normalizeAndVerify(string);
        if (intRange.string != null) {
            PropertyUtilities.putNew(data, intRange.string, string, merger);
        } else {
            for (int codepoint = intRange.start; codepoint <= intRange.end; ++codepoint) {
                try {
                    if (hackHangul) {
                        String fullDecomp = NFD.getDecomposition(codepoint); // use ICU for Hangul decomposition
                        if (fullDecomp.length() > 2) {
                            fullDecomp = NFC.normalize(fullDecomp.substring(0,2)) + fullDecomp.substring(2);
                        }
                        PropertyUtilities.putNew(data, codepoint, fullDecomp, merger);
                    } else if (string == PropertyParsingInfo.CONSTRUCTED_NAME) {
                        PropertyUtilities.putNew(data, codepoint, UCharacter.getName(codepoint), merger); // use ICU for Hangul Name construction, constant
                    } else {
                        PropertyUtilities.putNew(data, codepoint, string, merger);
                    }
                } catch (final Exception e) {
                    throw new UnicodePropertyException(property + ":\t" + intRange.start + "..." + intRange.end + "\t" + string, e);
                }
            }
        }
    }

    public String normalizeAndVerify(String string) {
        switch (property.getType()) {
        case Enumerated:
        case Catalog:
        case Binary:
            string = normalizeEnum(string);
            break;
        case Numeric:
        case Miscellaneous:
            if (property==UcdProperty.Script_Extensions) {
                string = normalizeEnum(string);
            } else {
                string = checkRegex2(string);
            }
            break;
        case String:
            // check regex
            string = checkRegex2(string);
            if (string == null) {
                // nothing
            } else {
                try {
                    if (string.contains("|")) {
                        StringBuilder result = new StringBuilder();
                        for (String part : BAR.split(string)) {
                            result.append(Utility.fromHex(part));
                        }
                        string = result.toString();
                    } else {
                        string = Utility.fromHex(string);
                    }
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new UnicodePropertyException(property.toString());
                }
            }
            break;
        }
        return string;
    }
    
    static Splitter BAR = Splitter.on('|').trimResults();

    public String normalizeEnum(String string) {
        if (getMultivalued().isBreakable(string)) {
            final PropertyParsingInfo propInfo = property == UcdProperty.Script_Extensions ? PropertyParsingInfo.getPropertyInfo(UcdProperty.Script) : this;
            String[] parts = multivaluedSplit.split(string);
            if (parts.length > 1) {
                final StringBuilder newString = new StringBuilder();
                for (final String part : parts) {
                    if (newString.length() != 0) {
                        newString.append(IndexUnicodeProperties.SET_SEPARATOR);
                    }
                    newString.append(propInfo.checkEnum(part));
                }
                string = newString.toString();
            } else {
                string = propInfo.checkEnum(string);
            }
        } else {
            string = checkEnum(string);
        }
        return string;
    }

    public String checkRegex2(String string) {
        if (getRegex() == null) {
            IndexUnicodeProperties.getDataLoadingErrors().put(property, "Regex missing");
            return string;
        }
        if (string == null) {
            return string;
        }
        if (getMultivalued().isBreakable(string)) {
            String[] parts = multivaluedSplit.split(string);

            if (parts.length > 1) {
                final StringBuilder newString = new StringBuilder();
                for (final String part : parts) {
                    if (newString.length() != 0) {
                        newString.append(IndexUnicodeProperties.SET_SEPARATOR);
                    }
                    checkRegex(part);
                    newString.append(part);
                }
                string = newString.toString();
            } else {
                checkRegex(string);
            }
        } else {
            checkRegex(string);
        }
        return string;
    }
    public String checkEnum(String string) {
        final Enum item = string == null ? null : property.getEnum(string);
        if (item == null) {
            final String errorMessage = property + "\tBad enum value:\t" + string;
            IndexUnicodeProperties.getDataLoadingErrors().put(property, errorMessage);
        } else {
            string = item.toString();
        }
        return string;
    }

    public void checkRegex(String part) {
        if (!getRegex().matcher(part).matches()) {
            final String part2 = NFD.normalize(part);
            if (!getRegex().matcher(part2).matches()) {
                IndexUnicodeProperties.getDataLoadingErrors().put(property, "Regex failure: " + RegexUtilities.showMismatch(getRegex(), part));
            }
        }
    }
    public void put(UnicodeMap<String> data, PropertyParsingInfo.IntRange intRange, String string) {
        put(data, intRange, string, null);
    }
    public String getDefaultValue() {
        return defaultValue;
    }
    public ValueCardinality getMultivalued() {
        return multivalued;
    }
    public Pattern getRegex() {
        return regex;
    }

    enum FileType {Field, HackField, PropertyValue, List, CJKRadicals, NamedSequences, 
        NameAliases, StandardizedVariants, Confusables}
    static Map<String,FileType> file2Type = new HashMap<String,FileType>();


    static Map<String,ValueCardinality> toMultiValued = new HashMap<>();
    static {
        toMultiValued.put("N/A", ValueCardinality.Singleton);
        toMultiValued.put("space", ValueCardinality.Singleton);
        toMultiValued.put("SINGLE_VALUED", ValueCardinality.Singleton);
        toMultiValued.put("EXTENSIBLE", ValueCardinality.Singleton);
        toMultiValued.put("MULTI_VALUED", ValueCardinality.Unordered);
        for (final ValueCardinality multi : ValueCardinality.values()) {
            toMultiValued.put(multi.toString(), multi);
            toMultiValued.put(UCharacter.toUpperCase(multi.toString()), multi);
        }
    }

    public void setMultiValued(String multivalued2) {
        if (property == UcdProperty.Name_Alias || property == UcdProperty.Standardized_Variant) {
            multivaluedSplit = PropertyParsingInfo.NO_SPLIT;
        }
        if (multivalued2.endsWith("_COMMA")) {
            multivaluedSplit = PropertyParsingInfo.COMMA;
            multivalued = ValueCardinality.Unordered;
            return;
        }
        multivalued = toMultiValued.get(multivalued2);
    }

    void parseSourceFile(IndexUnicodeProperties indexUnicodeProperties, final String fullFilename, final String fileName) {
        FileType fileType = file2Type.get(fileName);
        if (fileType == null) {
            fileType = FileType.Field;
        }
    
        final Set<PropertyParsingInfo> propInfoSetRaw = PropertyParsingInfo.getFile2PropertyInfoSet().get(fileName);
        final Set<PropertyParsingInfo> propInfoSet = new LinkedHashSet<>();
        for (final PropertyParsingInfo propInfo : propInfoSetRaw) {
            // the propInfoSet has all the properties, even those that are not in this version of the file
            if (!fileName.equals(propInfo.getFileName(indexUnicodeProperties.ucdVersion))) {
                continue;
            }
            propInfoSet.add(propInfo);
        }
    
        for (final PropertyParsingInfo propInfo : propInfoSet) {
            PropertyUtilities.putNew(indexUnicodeProperties.property2UnicodeMap, propInfo.property, new UnicodeMap<String>());
        }
    
        // if a file is not in a given version of Unicode, we skip it.
        if (fullFilename == null) {
            if (IndexUnicodeProperties.SHOW_LOADED) {
                System.out.println("Version\t" + indexUnicodeProperties.getUcdVersion() + "\tFile doesn't exist: " + file);
            }
        } else {
            indexUnicodeProperties.getFileNames().add(fullFilename);
    
            final Matcher semicolon = PropertyParsingInfo.SEMICOLON.matcher("");
            final Matcher decompRemove = PropertyParsingInfo.DECOMP_REMOVE.matcher("");
            final Matcher tab = PropertyParsingInfo.TAB.matcher("");
            final PropertyParsingInfo.IntRange intRange = new PropertyParsingInfo.IntRange();
            int lastCodepoint = 0;
    
            int lineCount = 0;
            boolean containsEOF = false;
            Merge<String> merger = null;
    
            for (String line : FileUtilities.in("", fullFilename)) {
                ++lineCount;
                //                if (prop2 == UcdProperty.Script) {
                //                    System.out.println(line);
                //                }
                if (line.contains("10530")) {
                    final int y = 3;
                }
                final int hashPos = line.indexOf('#');
                if (hashPos >= 0) {
                    if (line.contains("# EOF")) {
                        containsEOF = true;
                    } else {
                        handleMissing(fileType, propInfoSet, line);
                    }
                    line = line.substring(0,hashPos);
                }
                line = line.trim();
                if (line.startsWith("\ufeff")) {
                    line = line.substring(1).trim();
                }
                if (line.isEmpty()) {
                    continue;
                }
                //HACK
                final String[] parts = Regexes.split(line.startsWith("U+") ? tab : semicolon, line.trim());
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
                boolean hackHangul = false;
    
                switch(fileType) {
                case CJKRadicals: {
                    final PropertyParsingInfo propInfo = PropertyParsingInfo.property2PropertyInfo.get(UcdProperty.CJK_Radical);
                    final UnicodeMap<String> data = indexUnicodeProperties.property2UnicodeMap.get(propInfo.property);
                    intRange.set(parts[1]);
                    propInfo.put(data, intRange, parts[0]);
                    intRange.set(parts[2]);
                    propInfo.put(data, intRange, parts[0]);
                    break;
                }
                case NamedSequences: {
                    for (final PropertyParsingInfo propInfo : propInfoSet) {
                        final UnicodeMap<String> data = indexUnicodeProperties.property2UnicodeMap.get(propInfo.property);
                        intRange.set(parts[1]);
                        propInfo.put(data, intRange, parts[0]);
                    }
                    break;
                }
                case PropertyValue: {
                    PropertyParsingInfo propInfo;
                    final UnicodeMap<String> data;
                    try {
                        propInfo = PropertyParsingInfo.property2PropertyInfo.get(UcdProperty.forString(parts[1]));
                        data = indexUnicodeProperties.property2UnicodeMap.get(propInfo.property);
                    } catch (Exception e) {
                        throw new IllegalArgumentException(Arrays.asList(parts).toString(), e);
                    }
                    //                    if (!propInfoSet.contains(propInfo)) {
                    //                        throw new UnicodePropertyException("Property not listed for file: " + propInfo);
                    //                    }
                    switch(propInfo.property.getType()) {
                    case Binary:
                        propInfo.put(data, intRange, "Yes"); break;
                    default:
                        String value = parts[2];
                        if (propInfo.property == UcdProperty.kMandarin) {
                            if (indexUnicodeProperties.oldVersion) {
                                value = IndexUnicodeProperties.fromNumericPinyin.transform(value.toLowerCase(Locale.ENGLISH));
                            }
                        }
                        propInfo.put(data, intRange, value); break;
                    }
                    break;
                }
                case Confusables: {
                    final PropertyParsingInfo propInfo = PropertyParsingInfo.property2PropertyInfo.get(UcdProperty.forString("Confusable_" + parts[2]));
                    if (!propInfoSet.contains(propInfo)) {
                        throw new UnicodePropertyException("Property not listed for file: " + propInfo);
                    }
                    final UnicodeMap<String> data = indexUnicodeProperties.property2UnicodeMap.get(propInfo.property);
                    propInfo.put(data, intRange, parts[1]);
                    break;
                }
                case StandardizedVariants:
                    if (!parts[2].isEmpty()) {
                        parts[1] = parts[1] + " (" + parts[2] + ")";
                    }
                    //$FALL-THROUGH$
                case NameAliases:
                    merger = IndexUnicodeProperties.ALPHABETIC_JOINER;
                    //$FALL-THROUGH$
                case HackField:
                    if (parts[1].endsWith("Last>")) {
                        intRange.start = lastCodepoint + 1;
                    }
                    if (parts[1].startsWith("<")) {
                        /*
 * 4DB5;<CJK Ideograph Extension A, Last>;Lo;0;L;;;;;N;;;;; 
 * 9FD5;<CJK Ideograph, Last>;Lo;0;L;;;;;N;;;;; 
 * D7A3;<Hangul Syllable, Last>;Lo;0;L;;;;;N;;;;; 
 * DB7F;<Non Private Use High Surrogate, Last>;Cs;0;L;;;;;N;;;;; 
 * DBFF;<Private Use High Surrogate, Last>;Cs;0;L;;;;;N;;;;; 
 * DFFF;<Low Surrogate, Last>;Cs;0;L;;;;;N;;;;; 
 * F8FF;<Private Use, Last>;Co;0;L;;;;;N;;;;; 
 * 187EC;<Tangut Ideograph, Last>;Lo;0;L;;;;;N;;;;; 
 * 2A6D6;<CJK Ideograph Extension B, Last>;Lo;0;L;;;;;N;;;;; 
 * 2B734;<CJK Ideograph Extension C, Last>;Lo;0;L;;;;;N;;;;; 
 * 2B81D;<CJK Ideograph Extension D, Last>;Lo;0;L;;;;;N;;;;; 
 * 2CEA1;<CJK Ideograph Extension E, Last>;Lo;0;L;;;;;N;;;;; 
 * FFFFD;<Plane 15 Private Use, Last>;Co;0;L;;;;;N;;;;; 
 * 10FFFD;<Plane 16 Private Use, Last>;Co;0;L;;;;;N;;;;; 
                         */
                        if (parts[1].contains("CJK Ideograph")) {
                            parts[1] = "CJK UNIFIED IDEOGRAPH-#";
                        } else if (parts[1].contains("Tangut Ideograph")) {
                            parts[1] = "TANGUT IDEOGRAPH-#";
                        } else if (parts[1].contains("Hangul Syllable")) {
                            parts[1] = PropertyParsingInfo.CONSTRUCTED_NAME;
                            hackHangul = true;
                        } else {
                            parts[1] = null;
                        }
                    } else if (parts[1].startsWith("CJK COMPATIBILITY IDEOGRAPH-")) {
                        parts[1] = "CJK COMPATIBILITY IDEOGRAPH-#"; // hack for uniform data
                    }
                    lastCodepoint = intRange.end;
                    //$FALL-THROUGH$
                case Field:
                {
                    for (final PropertyParsingInfo propInfo : propInfoSet) {
                        final UnicodeMap<String> data = indexUnicodeProperties.property2UnicodeMap.get(propInfo.property);
                        String string = propInfo.fieldNumber >= parts.length ? "" 
                                : parts[propInfo.fieldNumber];
                        switch(propInfo.special) {
                        case None:
                            break;
                        case Rational:
                            //                            int slashPos = string.indexOf('/');
                            //                            double rational;
                            //                            if (slashPos < 0) {
                            //                                rational = Double.parseDouble(string);
                            //                            } else {
                            //                                rational = Double.parseDouble(string.substring(0,slashPos)) / Double.parseDouble(string.substring(slashPos+1));
                            //                            }
                            //                            string = Double.toString(rational);
                            break;
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
                        default: throw new UnicodePropertyException();
                        }
                        if (fileType == FileType.HackField 
                                && propInfo.fieldNumber == 5 
                                && !string.isEmpty()
                                && string.indexOf('<') >= 0) { // remove decomposition type
                            string = decompRemove.reset(string).replaceAll("").trim();
                        }
                        propInfo.put(data, intRange, string, merger, hackHangul && propInfo.property == UcdProperty.Decomposition_Mapping);
                    }
                    merger = null;
                    break;
                }
                case List: {
                    if (propInfoSet.size() != 1) {
                        throw new UnicodePropertyException("List files must have only one property, and must be Boolean");
                    }
                    final PropertyParsingInfo propInfo = propInfoSet.iterator().next();
                    final UnicodeMap<String> data = indexUnicodeProperties.property2UnicodeMap.get(propInfo.property);
                    //prop.data.putAll(UnicodeSet.ALL_CODE_POINTS, "No");
                    propInfo.put(data, intRange, "Yes");
                    break;
                }
                default: throw new UnicodePropertyException();
                }
            }
            //            if (property2UnicodeMap.get(UcdProperty.Script).values().contains("Hatran")) {
            //                int x = 1;
            //            }
            if (IndexUnicodeProperties.SHOW_LOADED) {
                System.out.println("Version\t" + indexUnicodeProperties.getUcdVersion() + "\tLoaded: " + file + "\tlines: " + lineCount + (containsEOF ? "" : "\t*NO '# EOF'"));
            }
        }
        for (final PropertyParsingInfo propInfo : propInfoSet) {
            if (propInfo.property == IndexUnicodeProperties.CHECK_PROPERTY) {
                int debug = 0;
            }
            final UnicodeMap<String> data = indexUnicodeProperties.property2UnicodeMap.get(propInfo.property);
            final UnicodeSet nullValues = data.getSet(null);
            //            if (propInfo.defaultValue == null) {
            //                if (CHECK_MISSING != null) {
            //                    System.out.println("** Clearing null dv in " + propInfo.property);
            //                }
            //                propInfo.defaultValue = "<none>";
            //            }
            switch (propInfo.defaultValueType) {
            case Script: case Simple_Lowercase_Mapping: case Simple_Titlecase_Mapping: case Simple_Uppercase_Mapping:
                final UcdProperty sourceProp = propInfo.defaultValueType.property;
                final UnicodeMap<String> otherMap = indexUnicodeProperties.load(sourceProp); // recurse
                for (final String cp : nullValues) {
                    data.put(cp, otherMap.get(cp));
                }
                // propInfo.defaultValueType = property2PropertyInfo.get(sourceProp).defaultValueType; // reset to the type
                break;
            case LITERAL:
                data.putAll(nullValues, propInfo.getDefaultValue());
                break;
            case NONE:
                //data.putAll(nullValues, propInfo.defaultValue);
                // do nothing, already none;
                break;
            case CODE_POINT:
                // requires special handling later
                break;
            default:
                throw new UnicodePropertyException(); // unexpected error
            }
            data.freeze();
            if (IndexUnicodeProperties.FILE_CACHE) {
                indexUnicodeProperties.storeCachedMap(propInfo.property, data);
            }
        }
    }

    public static Pattern hackCompile(String regex) {
        if (regex.contains("\\x")) {
            final StringBuilder builder = new StringBuilder();
            final Matcher m = PropertyParsingInfo.SLASHX.matcher(regex);
            int start = 0;
            while (m.find()) {
                builder.append(regex.substring(start, m.start()));
                final int codePoint = Integer.parseInt(m.group(1),16);
                //System.out.println("\\x char:\t" + new StringBuilder().appendCodePoint(codePoint));
                builder.appendCodePoint(codePoint);
                start = m.end();
            }
            builder.append(regex.substring(start));
            regex = builder.toString();
        }
        return Pattern.compile(regex);
    }

    public static void getRegexInfo(String line) {
        try {
            if (line.startsWith("$")) {
                final String[] parts = PropertyParsingInfo.EQUALS.split(line);
                IndexUnicodeProperties.vr.add(parts[0], IndexUnicodeProperties.vr.replace(parts[1]));
                return;
            }
            if (line.isEmpty() || line.startsWith("#")) {
                return;
            }
            // have to do this painfully, since the regex may contain semicolons
            final Matcher m = PropertyParsingInfo.SEMICOLON.matcher(line);
            if (!m.find()) {
                throw new UnicodePropertyException("Bad semicolons in: " + line);
            }
            final String propName = line.substring(0, m.start());
            final int propNameEnd = m.end();
            if (!m.find()) {
                throw new UnicodePropertyException("Bad semicolons in: " + line);
            }
            final UcdProperty prop = UcdProperty.forString(propName);
            final PropertyParsingInfo propInfo = property2PropertyInfo.get(prop);
            final String multivalued = line.substring(propNameEnd, m.start());
            propInfo.setMultiValued(multivalued);
            if (propInfo.getMultivalued() == null) {
                throw new UnicodePropertyException("Bad multivalued in: " + line);
            }
            String regex = line.substring(m.end());
            propInfo.originalRegex = regex;
            regex = IndexUnicodeProperties.vr.replace(regex);
            //        if (!regex.equals(propInfo.originalRegex)) {
            //            regex = vr.replace(propInfo.originalRegex);
            //            System.out.println(propInfo.originalRegex + "=>" + regex);
            //        }
            if (regex.equals("null")) {
                propInfo.regex = null;
            } else {
                propInfo.regex = PropertyParsingInfo.hackCompile(regex);
            }
        } catch (final Exception e) {
            throw new UnicodePropertyException(line, e);
        }
    }

    public static PropertyParsingInfo getPropertyInfo(UcdProperty property) {
        return property2PropertyInfo.get(property);
    }

    /**
     * @param ucdVersion2 
     * @internal
     * @deprecated
     */
    public static Relation<String,PropertyParsingInfo> getFile2PropertyInfoSet() {
        return PropertyParsingInfo.file2PropertyInfoSet;
    }

    static void setFile2PropertyInfoSet(Relation<String,PropertyParsingInfo> file2PropertyInfoSet) {
        PropertyParsingInfo.file2PropertyInfoSet = file2PropertyInfoSet;
    }

    public static void setPropDefault(UcdProperty prop, String value, String line, boolean isEmpty) {
        if (prop == IndexUnicodeProperties.CHECK_PROPERTY) {
            System.out.format("** %s %s %s %s\n", prop, value, line, isEmpty);
        }
        final PropertyParsingInfo propInfo = PropertyParsingInfo.property2PropertyInfo.get(prop);
    
        if (value != null && !value.startsWith("<")) {
            value = propInfo.normalizeAndVerify(value);
        }
    
        if (propInfo.getDefaultValue() == null) {
            propInfo.defaultValueType = IndexUnicodeProperties.DefaultValueType.forString(value);
            propInfo.defaultValue = value;
            if (IndexUnicodeProperties.SHOW_DEFAULTS) {
                IndexUnicodeProperties.getDataLoadingErrors().put(prop, "**\t" + prop + "\t" + propInfo.defaultValueType + "\t" + propInfo.getDefaultValue());
            }
        } else if (propInfo.getDefaultValue().equals(value)) {
        } else {
            final String comment = "\t ** ERROR Will not change default for " + prop +
                    " to «" + value + "», retaining " + propInfo.getDefaultValue();
            //            propInfo.defaultValueType = DefaultValueType.forString(value);
            //            propInfo.defaultValue = value;
            IndexUnicodeProperties.getDataLoadingErrors().put(prop, comment);
        }
    }

    public static void handleMissing(FileType fileType, Set<PropertyParsingInfo> propInfoSet, String missing) {
        if (!missing.contains("@missing")) { // quick test
            return;
        }
        final Matcher simpleMissingMatcher = PropertyParsingInfo.SIMPLE_MISSING_PATTERN.matcher(missing);
        if (!simpleMissingMatcher.lookingAt()) {
            //            if (missing.contains("@missing")) {
            //                System.out.println("Skipping " + missing + "\t" + RegexUtilities.showMismatch(simpleMissingMatcher, missing));
            //            }
            return;
        }
        final Matcher missingMatcher = PropertyParsingInfo.MISSING_PATTERN.matcher(missing);
        if (!missingMatcher.matches()) {
            System.err.println(RegexUtilities.showMismatch(PropertyParsingInfo.MISSING_PATTERN, missing));
            throw new UnicodePropertyException("Bad @missing statement: " + missing);
        }
        final boolean isEmpty = missingMatcher.group(1).equals("empty");
        final int start = Integer.parseInt(missingMatcher.group(2),16);
        final int end = Integer.parseInt(missingMatcher.group(3),16);
        if (start != 0 || end != 0x10FFFF) {
            System.err.println("Unexpected range: " + missing);
        }
        // # @missing: 0000..10FFFF; cjkIRG_KPSource; <none>
    
        String value1 = missingMatcher.group(4);
        String value2 = missingMatcher.group(5);
        String value3 = missingMatcher.group(6);
        if (value1 != null) {
            value1 = value1.trim();
        }
        if (value2 != null) {
            value2 = value2.trim();
        }
        if (value3 != null) {
            value3 = value3.trim();
        }
    
        switch (fileType) {
        case Field: {
            for (final PropertyParsingInfo propInfo : propInfoSet) {
                final String value = propInfo.fieldNumber == 1 ? value1
                        : propInfo.fieldNumber == 2 ? value2
                                : value3;
                PropertyParsingInfo.setPropDefault(propInfo.property, value, missing, isEmpty);
            }
            break;
        }
        case PropertyValue:
        case Confusables: {
            final UcdProperty ucdProp = UcdProperty.forString(value1);
            final PropertyParsingInfo propInfo = PropertyParsingInfo.property2PropertyInfo.get(ucdProp);
            PropertyParsingInfo.setPropDefault(propInfo.property, value2, missing, isEmpty);
            break;
        }
        default:
            throw new IllegalArgumentException("Unhandled missing line: " + missing);
        }
    }

    static void init() {
        final Matcher semicolon = PropertyParsingInfo.SEMICOLON.matcher("");
        for (final String line : FileUtilities.in(IndexUnicodeProperties.class, "IndexUnicodeProperties.txt")) {
            if (line.startsWith("#") || line.isEmpty()) {
                continue;
            }
    
            final String[] parts = Regexes.split(semicolon, line.trim());
            // file ; property name ; derivation info
            //System.out.println(Arrays.asList(parts));
            if (parts[0].equals("FileType")) {
                PropertyUtilities.putNew(file2Type, parts[1], FileType.valueOf(parts[2]));
            } else {
                fromStrings(parts);
            }
        }
        // DO THESE FIRST (overrides values in files!)
        for (final String file : Arrays.asList("ExtraPropertyAliases.txt","ExtraPropertyValueAliases.txt")) {
            for (final String line : FileUtilities.in(IndexUnicodeProperties.class, file)) {
                PropertyParsingInfo.handleMissing(FileType.PropertyValue, null, line);
            }
        }
        for (final String line : FileUtilities.in("", Utility.getMostRecentUnicodeDataFile("PropertyValueAliases", GenerateEnums.ENUM_VERSION, true, false))) {
            PropertyParsingInfo.handleMissing(FileType.PropertyValue, null, line);
        }
        for (final String line : FileUtilities.in(IndexUnicodeProperties.class, "IndexPropertyRegex.txt")) {
            PropertyParsingInfo.getRegexInfo(line);
        }
    
        //        for (String line : FileUtilities.in(IndexUnicodeProperties.class, "Multivalued.txt")) {
        //            UcdProperty prop = UcdProperty.forString(line);
        //            PropertyParsingInfo propInfo = property2PropertyInfo.get(prop);
        //            propInfo.multivalued = Multivalued.MULTI_VALUED;
        //        }
    
        //        for (UcdProperty x : UcdProperty.values()) {
        //            if (property2PropertyInfo.containsKey(x.toString())) continue;
        //            if (SHOW_PROP_INFO) System.out.println("Missing: " + x);
        //        }
    }
    
    static {
        PropertyParsingInfo.init();
    }


}