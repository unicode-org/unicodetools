package org.unicode.props;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.RegexUtilities;
import org.unicode.draft.CldrUtility.VariableReplacer;
import org.unicode.draft.UnicodeDataInput;
import org.unicode.draft.UnicodeDataInput.ItemReader;
import org.unicode.draft.UnicodeDataOutput;
import org.unicode.draft.UnicodeDataOutput.ItemWriter;
import org.unicode.idna.Regexes;
import org.unicode.props.PropertyNames.Named;
import org.unicode.props.PropertyUtilities.Joiner;
import org.unicode.props.PropertyUtilities.Merge;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

import com.google.common.base.Splitter;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeProperty;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.VersionInfo;

/**
 * TODO StandardizedVariants and NameSequences*
 * @author markdavis
 *
 */
public class IndexUnicodeProperties extends UnicodeProperty.Factory {
    private static final String SET_SEPARATOR = "|";
    /**
     * Control file caching
     */
    static final boolean GZIP = true;
    static final boolean SIMPLE_COMPRESSION = true;
    static final boolean FILE_CACHE = true; // true

    /**
     * Debugging
     */
    private static final boolean SHOW_DEFAULTS = false;
    private static final boolean CHECK_PROPERTY_STATUS = false;
    private static final UcdProperty CHECK_MISSING = null; // UcdProperty.Numeric_Value; //

    /**
     * General constants
     */
    public final static Pattern SEMICOLON = Pattern.compile("\\s*;\\s*");
    public final static Pattern DECOMP_REMOVE = Pattern.compile("\\{[^}]+\\}|\\<[^>]+\\>");
    public final static Pattern COMMA = Pattern.compile("\\s*,\\s*");
    public final static Pattern EQUALS = Pattern.compile("\\s*=\\s*");
    public final static Pattern SPACE = Pattern.compile("\\s+");
    public final static Pattern NO_SPLIT = Pattern.compile("\\uFFFF");
    static Pattern SLASHX = Pattern.compile("\\\\x\\{([0-9A-Fa-f]{1,6})\\}");
    static Normalizer2 NFD = Normalizer2.getNFDInstance(); //
    //static Normalizer2 NFD2 = Normalizer2.getInstance(null, "NFC", Mode.DECOMPOSE);

    static final String CONSTRUCTED_NAME = "$HANGUL_SYLLABLE$";

    static final Pattern SIMPLE_MISSING_PATTERN = Pattern.compile("\\s*#\\s*@(missing|empty)");

    static final Pattern MISSING_PATTERN = Pattern.compile(
            "\\s*#\\s*@(missing|empty):?" +
                    "\\s*([A-Fa-f0-9]+)..([A-Fa-f0-9]+)\\s*;" +
                    "\\s*([^;]*)" +
                    "(?:\\s*;\\s*([^;]*)" +
                    "(?:\\s*;\\s*([^;]*))?)?\\s*" +
                    ";?"
            );

    public final static String FIELD_SEPARATOR = "; ";
    public final static Pattern TAB = Pattern.compile("[ ]*\t[ ]*");
    private static final boolean SHOW_LOADED = false;
    private static final Relation<UcdProperty,String> DATA_LOADING_ERRORS
    = Relation.of(new EnumMap<UcdProperty,Set<String>>(UcdProperty.class), LinkedHashSet.class);

    public enum DefaultValueType {
        LITERAL(null),
        NONE(null),
        CODE_POINT(null),
        Script(UcdProperty.Script),
        Script_Extensions(UcdProperty.Script_Extensions),
        Simple_Lowercase_Mapping(UcdProperty.Simple_Lowercase_Mapping),
        Simple_Titlecase_Mapping(UcdProperty.Simple_Titlecase_Mapping),
        Simple_Uppercase_Mapping(UcdProperty.Simple_Uppercase_Mapping);
        static final HashMap<String, DefaultValueType> mapping = new HashMap<String, DefaultValueType>();
        static {
            mapping.put("<none>", NONE);
            mapping.put("<slc>", Simple_Lowercase_Mapping);
            mapping.put("<stc>", Simple_Titlecase_Mapping);
            mapping.put("<suc>", Simple_Uppercase_Mapping);
            mapping.put("<codepoint>", CODE_POINT);
            mapping.put("<code point>", CODE_POINT);
            mapping.put("<script>", Script);
            //mapping.put("NaN", LITERAL);
        }
        final UcdProperty property;
        static DefaultValueType forString(String string) {
            final DefaultValueType result = mapping.get(string);
            return result == null ? LITERAL : result;
        }
        DefaultValueType(UcdProperty prop) {
            property = prop;
        }
    }
    static VariableReplacer vr = new VariableReplacer();

    enum FileType {Field, HackField, PropertyValue, List, CJKRadicals, NamedSequences, 
        NameAliases, StandardizedVariants, Confusables}
    enum SpecialProperty {None, Skip1FT, Skip1ST, SkipAny4, Rational}

    static Map<String,ValueCardinality> toMultiValued = new HashMap();
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

    static final Joiner JOIN = new Joiner(FIELD_SEPARATOR);

    /**
     * @internal
     * @deprecated
     */
    public static class PropertyParsingInfo implements Comparable<PropertyParsingInfo>{
        private static final VersionInfo MIN_VERSION = VersionInfo.getInstance(0,0,0,0);
        public final String file;
        public final UcdProperty property;
        public final int fieldNumber;
        public final SpecialProperty special;

        public String oldFile;
        public VersionInfo maxOldVersion = MIN_VERSION;

        private String defaultValue;
        DefaultValueType defaultValueType = DefaultValueType.LITERAL;
        //UnicodeMap<String> data;
        //final Set<String> errors = new LinkedHashSet<String>();
        private Pattern regex = null;
        private ValueCardinality multivalued = ValueCardinality.Singleton;
        private Pattern multivaluedSplit = SPACE;
        public String originalRegex;


        public PropertyParsingInfo(String file, UcdProperty property,
                int fieldNumber, SpecialProperty special) {
            this.file = file;
            this.property = property;
            this.fieldNumber = fieldNumber;
            this.special = special;
        }

        public static void fromStrings(String... propertyInfo) {
            if (propertyInfo.length < 2 || propertyInfo.length > 4) {
                throw new UnicodePropertyException("Must have 2 to 4 args: " + Arrays.asList(propertyInfo));
            }
            String _file = propertyInfo[0];
            final String propName = propertyInfo[1];
            UcdProperty _property = UcdProperty.forString(propName);

            int temp = 1;
            if (propertyInfo.length > 2 && !propertyInfo[2].isEmpty()) {
                // HACK for old version
                if (propertyInfo[2].startsWith("v")) {
                    PropertyParsingInfo result = property2PropertyInfo.get(_property);
                    result.oldFile = _file;
                    result.maxOldVersion = VersionInfo.getInstance(propertyInfo[2].substring(1));
                    getFile2PropertyInfoSet().put(_file, result);
                    //                    System.err.println(_property + ", " + result);
                    return;
                }
                temp = Integer.parseInt(propertyInfo[2]);
            }
            int _fieldNumber = temp;

            SpecialProperty _special = propertyInfo.length < 4 || propertyInfo[3].isEmpty()
                    ? SpecialProperty.None
                            : SpecialProperty.valueOf(propertyInfo[3]);
            PropertyParsingInfo result = new PropertyParsingInfo(_file, _property, _fieldNumber, _special);

            try {
                PropertyUtilities.putNew(property2PropertyInfo, _property, result);
                getFile2PropertyInfoSet().put(_file, result);
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

        public void put(UnicodeMap<String> data, IntRange intRange, String string, Merge<String> merger) {
            put(data, intRange, string, merger, false);
        }

        public static final Normalizer2 NFD = Normalizer2.getNFDInstance();
        public static final Normalizer2 NFC = Normalizer2.getNFCInstance();

        public void put(UnicodeMap<String> data, IntRange intRange, String string, Merge<String> merger, boolean hackHangul) {
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
                        } else if (string == CONSTRUCTED_NAME) {
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
                        string = Utility.fromHex(string);
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

        public String normalizeEnum(String string) {
            if (getMultivalued().isBreakable(string)) {
                final PropertyParsingInfo propInfo = property == UcdProperty.Script_Extensions ? getPropertyInfo(UcdProperty.Script) : this;
                String[] parts = multivaluedSplit.split(string);
                if (parts.length > 1) {
                    final StringBuilder newString = new StringBuilder();
                    for (final String part : parts) {
                        if (newString.length() != 0) {
                            newString.append(SET_SEPARATOR);
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
                getDataLoadingErrors().put(property, "Regex missing");
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
                            newString.append(SET_SEPARATOR);
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
            final Enum item = property.getEnum(string);
            if (item == null) {
                final String errorMessage = property + "\tBad enum value:\t" + string;
                getDataLoadingErrors().put(property, errorMessage);
            } else {
                string = item.toString();
            }
            return string;
        }

        public void checkRegex(String part) {
            if (!getRegex().matcher(part).matches()) {
                final String part2 = NFD.normalize(part);
                if (!getRegex().matcher(part2).matches()) {
                    getDataLoadingErrors().put(property, "Regex failure: " + RegexUtilities.showMismatch(getRegex(), part));
                }
            }
        }
        public void put(UnicodeMap<String> data, IntRange intRange, String string) {
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

        public void setMultiValued(String multivalued2) {
            if (property == UcdProperty.Name_Alias || property == UcdProperty.Standardized_Variant) {
                multivaluedSplit = NO_SPLIT;
            }
            if (multivalued2.endsWith("_COMMA")) {
                multivaluedSplit = COMMA;
                multivalued = ValueCardinality.Unordered;
                return;
            }
            multivalued = toMultiValued.get(multivalued2);
        }
    }

    static Map<VersionInfo, IndexUnicodeProperties> version2IndexUnicodeProperties = new HashMap();

    private static EnumMap<UcdProperty, PropertyParsingInfo> property2PropertyInfo = new EnumMap<UcdProperty,PropertyParsingInfo>(UcdProperty.class);

    public static PropertyParsingInfo getPropertyInfo(UcdProperty property) {
        return property2PropertyInfo.get(property);
    }

    private static Relation<String,PropertyParsingInfo> file2PropertyInfoSet = Relation.of(new HashMap<String,Set<PropertyParsingInfo>>(), HashSet.class);

    static Map<String,FileType> file2Type = new HashMap<String,FileType>();

    static {
        final Matcher semicolon = SEMICOLON.matcher("");
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
                PropertyParsingInfo.fromStrings(parts);
            }
        }
        // DO THESE FIRST (overrides values in files!)
        for (final String file : Arrays.asList("ExtraPropertyAliases.txt","ExtraPropertyValueAliases.txt")) {
            for (final String line : FileUtilities.in(IndexUnicodeProperties.class, file)) {
                handleMissing(FileType.PropertyValue, null, line);
            }
        }
        for (final String line : FileUtilities.in("", Utility.getMostRecentUnicodeDataFile("PropertyValueAliases", GenerateEnums.ENUM_VERSION, true, false))) {
            handleMissing(FileType.PropertyValue, null, line);
        }
        for (final String line : FileUtilities.in(IndexUnicodeProperties.class, "IndexPropertyRegex.txt")) {
            getRegexInfo(line);
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

    public static void getRegexInfo(String line) {
        try {
            if (line.startsWith("$")) {
                final String[] parts = EQUALS.split(line);
                vr.add(parts[0], vr.replace(parts[1]));
                return;
            }
            if (line.isEmpty() || line.startsWith("#")) {
                return;
            }
            // have to do this painfully, since the regex may contain semicolons
            final Matcher m = SEMICOLON.matcher(line);
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
            regex = vr.replace(regex);
            //        if (!regex.equals(propInfo.originalRegex)) {
            //            regex = vr.replace(propInfo.originalRegex);
            //            System.out.println(propInfo.originalRegex + "=>" + regex);
            //        }
            if (regex.equals("null")) {
                propInfo.regex = null;
            } else {
                propInfo.regex = hackCompile(regex);
            }
        } catch (final Exception e) {
            throw new UnicodePropertyException(line, e);
        }
    }

    public static Pattern hackCompile(String regex) {
        if (regex.contains("\\x")) {
            final StringBuilder builder = new StringBuilder();
            final Matcher m = SLASHX.matcher(regex);
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

    private IndexUnicodeProperties(VersionInfo ucdVersion2) {
        ucdVersion = ucdVersion2;
        oldVersion = ucdVersion2.compareTo(GenerateEnums.ENUM_VERSION_INFO) < 0;
    }

    public static final IndexUnicodeProperties make(VersionInfo ucdVersion) {
        IndexUnicodeProperties newItem = version2IndexUnicodeProperties.get(ucdVersion);
        if (newItem == null) {
            version2IndexUnicodeProperties.put(ucdVersion, newItem = new IndexUnicodeProperties(ucdVersion));
        }
        return newItem;
    }

    public static final IndexUnicodeProperties make(String ucdVersion) {
        return make(VersionInfo.getInstance(ucdVersion));
    }

    public static final IndexUnicodeProperties make(UcdPropertyValues.Age_Values ucdVersion) {
        return make(VersionInfo.getInstance(ucdVersion.getShortName()));
    }

    private final VersionInfo ucdVersion;
    final boolean oldVersion;
    final EnumMap<UcdProperty, UnicodeMap<String>> property2UnicodeMap = new EnumMap<UcdProperty, UnicodeMap<String>>(UcdProperty.class);
    private final Set<String> fileNames = new TreeSet<String>();
    private final Map<UcdProperty, Long> cacheFileSize = new EnumMap<UcdProperty, Long>(UcdProperty.class);

    public Map<UcdProperty, Long> getCacheFileSize() {
        return Collections.unmodifiableMap(cacheFileSize);
    }

    static final Transform<String, String>    fromNumericPinyin   = Transliterator.getInstance("NumericPinyin-Latin;nfc");

    private static final Merge<String> ALPHABETIC_JOINER = new Merge<String>() {
        TreeSet<String> sorted = new TreeSet<String>();
        @Override
        public String merge(String first, String second) {
            sorted.clear();
            sorted.addAll(Arrays.asList(first.split(FIELD_SEPARATOR)));
            sorted.addAll(Arrays.asList(second.split(FIELD_SEPARATOR)));
            return CollectionUtilities.join(sorted, FIELD_SEPARATOR);
        }

    };

    // should be on UnicodeMap
    public static <T, V extends Collection<T>, U extends Map<T,UnicodeSet>> U invertSet(UnicodeMap<V> source, U target) {
        for (V valueSet : source.values()) {
            UnicodeSet uset = source.getSet(valueSet);
            for (T value : valueSet) {
                UnicodeSet s = target.get(value);
                if (s == null) {
                    target.put(value, s = new UnicodeSet());
                }
                s.addAll(uset);
            }
        }
        return target;
    }

    // should be on UnicodeMap
    public static <T, U extends Map<T,UnicodeSet>> U invert(UnicodeMap<T> source, U target) {
        for (T value : source.values()) {
            UnicodeSet uset = source.getSet(value);
            target.put(value, uset);
        }
        return target;
    }

    // should be on UnicodeMap
    public static <T> Map<T,UnicodeSet> freeze(Map<T,UnicodeSet> target) {
        for (UnicodeSet entry : target.values()) {
            entry.freeze();
        }
        return Collections.unmodifiableMap(target);
    }

    final Map<UcdProperty, UnicodeMap> SPECIAL_CACHE = new ConcurrentHashMap<>();
    final Map<UcdProperty, UnicodeMap<Integer>> INT_CACHE = new ConcurrentHashMap<>();
    final Map<UcdProperty, UnicodeMap<Double>> DOUBLE_CACHE = new ConcurrentHashMap<>();

    public UnicodeMap<Double> loadDouble(UcdProperty prop2) {
        UnicodeMap<Double> result = DOUBLE_CACHE.get(prop2);
        if (result == null) {
            result = new UnicodeMap<>();
            UnicodeMap<String> m = load(prop2);
            for (String value : m.values()) {
                double convertedValue;
                int pos = value.indexOf('/');
                if (pos >= 0) {
                    convertedValue = Integer.parseInt(value.substring(0,pos)) / (double)Integer.parseInt(value.substring(pos+1));
                } else {
                    convertedValue = Double.parseDouble(value);
                }
                UnicodeSet uset = m.getSet(value);
                result.putAll(uset, convertedValue);
            }
            DOUBLE_CACHE.put(prop2, result.freeze());
        }
        return result;
    }

    public UnicodeMap<Integer> loadInt(UcdProperty prop2) {
        UnicodeMap<Integer> result = INT_CACHE.get(prop2);
        if (result == null) {
            UnicodeMap<String> m = load(prop2);
            result = new UnicodeMap<>();
            for (String value : m.values()) {
                UnicodeSet uset = m.getSet(value);
                String shortValue = ((Named) prop2.getEnum(value)).getNames().getShortName();
                result.putAll(uset, Integer.parseInt(shortValue));
            }
            INT_CACHE.put(prop2, result.freeze());
        }
        return result;
    }

    public <T extends Enum<T>> UnicodeMap<T> loadEnum(UcdProperty prop2, Class<T> classIn) {
        UnicodeMap<T> result = SPECIAL_CACHE.get(prop2);
        if (result != null) {
            return result;
        }
        result = new UnicodeMap<>();
        if (classIn != prop2.getEnumClass()) {
            throw new UnicodePropertyException("Mismatch in class data,  expected " 
                    + prop2.getEnumClass());
        }
        UnicodeMap<String> m = load(prop2);
        for (String value : m.values()) {
            T enumv = (T) prop2.getEnum(value);
            UnicodeSet uset = m.getSet(value);
            result.putAll(uset, enumv);
        }
        SPECIAL_CACHE.put(prop2, result.freeze());
        return result;
    }

    static final Splitter SET_SPLITTER = Splitter.on(SET_SEPARATOR);

    public <T extends Enum<T>> UnicodeMap<Set<T>> loadEnumSet(UcdProperty prop2, Class enumClass) {
        UnicodeMap<Set<T>> result = SPECIAL_CACHE.get(prop2);
        if (result != null) {
            return result;
        }
        if (enumClass != prop2.getEnumClass() 
                || prop2.getCardinality() != ValueCardinality.Unordered) {
            throw new UnicodePropertyException("Mismatch in class data,  expected " 
                    + prop2.getEnumClass() 
                    + ", " + prop2.getCardinality());
        }
        if (enumClass != prop2.getEnumClass()) {
            throw new UnicodePropertyException("Mismatch in class enum,  expected " + prop2.getEnumClass());
        }
        result = new UnicodeMap<>();
        UnicodeMap<String> m = load(prop2);
        for (String value : m.values()) {
            Set<T> convertedValue = EnumSet.noneOf(enumClass);
            for (String s : SET_SPLITTER.split(value)) {
                T enumv = (T) prop2.getEnum(s);
                //System.out.println(s + " => " + enumv);
                convertedValue.add(enumv);
            }
            UnicodeSet uset = m.getSet(value);
            result.putAll(uset, Collections.unmodifiableSet(convertedValue));
        }
        SPECIAL_CACHE.put(prop2, result.freeze());
        return result;
    }

    public UnicodeMap<Set<String>> loadSet(UcdProperty prop2) {
        UnicodeMap<Set<String>> result = SPECIAL_CACHE.get(prop2);
        if (result != null) {
            return result;
        }
        result = new UnicodeMap<>();
        if (prop2.getCardinality() != ValueCardinality.Unordered) {
            throw new UnicodePropertyException("Mismatch in class data,  expected " 
                    + prop2.getCardinality());
        }
        UnicodeMap<String> m = load(prop2);
        // TODO cache
        for (String value : m.values()) {
            Set<String> convertedValue = new LinkedHashSet<>(SET_SPLITTER.splitToList(value));
            UnicodeSet uset = m.getSet(value);
            result.putAll(uset, Collections.unmodifiableSet(convertedValue));
        }
        SPECIAL_CACHE.put(prop2, result.freeze());
        return result;
    }

    public UnicodeMap<List<String>> loadList(UcdProperty prop2) {
        UnicodeMap<List<String>> result = SPECIAL_CACHE.get(prop2);
        if (result != null) {
            return result;
        }
        result = new UnicodeMap<>();
        if (prop2.getCardinality() != ValueCardinality.Ordered) {
            throw new UnicodePropertyException("Mismatch in class data,  expected " 
                    + prop2.getCardinality());
        }
        UnicodeMap<String> m = load(prop2);
        // TODO cache
        for (String value : m.values()) {
            UnicodeSet uset = m.getSet(value);
            result.putAll(uset, Collections.unmodifiableList(SET_SPLITTER.splitToList(value)));
        }
        SPECIAL_CACHE.put(prop2, result.freeze());
        return result;
    }


    public UnicodeMap<String> load(UcdProperty prop2) {
        UnicodeMap<String> data0 = property2UnicodeMap.get(prop2);
        if (data0 != null) {
            return data0;
        }

        final PropertyParsingInfo fileInfo = property2PropertyInfo.get(prop2);
        final String fullFilename = fileInfo.getFullFileName(ucdVersion);
        final String fileName = fileInfo.getFileName(ucdVersion);

        if (FILE_CACHE) {
            data0 = getCachedMap(prop2, fullFilename);
            if (data0 != null) {
                property2UnicodeMap.put(prop2, data0.freeze());
                return data0;
            }
        }

        FileType fileType = file2Type.get(fileName);
        if (fileType == null) {
            fileType = FileType.Field;
        }

        final Set<PropertyParsingInfo> propInfoSetRaw = getFile2PropertyInfoSet().get(fileName);
        final Set<PropertyParsingInfo> propInfoSet = new LinkedHashSet<>();
        for (final PropertyParsingInfo propInfo : propInfoSetRaw) {
            // the propInfoSet has all the properties, even those that are not in this version of the file
            if (!fileName.equals(propInfo.getFileName(ucdVersion))) {
                continue;
            }
            propInfoSet.add(propInfo);
        }

        for (final PropertyParsingInfo propInfo : propInfoSet) {
            PropertyUtilities.putNew(property2UnicodeMap, propInfo.property, new UnicodeMap<String>());
        }

        // if a file is not in a given version of Unicode, we skip it.
        if (fullFilename == null) {
            if (SHOW_LOADED) {
                System.out.println("Version\t" + getUcdVersion() + "\tFile doesn't exist: " + fileInfo.file);
            }
        } else {
            getFileNames().add(fullFilename);

            final Matcher semicolon = SEMICOLON.matcher("");
            final Matcher decompRemove = DECOMP_REMOVE.matcher("");
            final Matcher tab = TAB.matcher("");
            final IntRange intRange = new IntRange();
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
                    final PropertyParsingInfo propInfo = property2PropertyInfo.get(UcdProperty.CJK_Radical);
                    final UnicodeMap<String> data = property2UnicodeMap.get(propInfo.property);
                    intRange.set(parts[1]);
                    propInfo.put(data, intRange, parts[0]);
                    intRange.set(parts[2]);
                    propInfo.put(data, intRange, parts[0]);
                    break;
                }
                case NamedSequences: {
                    for (final PropertyParsingInfo propInfo : propInfoSet) {
                        final UnicodeMap<String> data = property2UnicodeMap.get(propInfo.property);
                        intRange.set(parts[1]);
                        propInfo.put(data, intRange, parts[0]);
                    }
                    break;
                }
                case PropertyValue: {
                    final PropertyParsingInfo propInfo = property2PropertyInfo.get(UcdProperty.forString(parts[1]));
                    final UnicodeMap<String> data = property2UnicodeMap.get(propInfo.property);
                    //                    if (!propInfoSet.contains(propInfo)) {
                    //                        throw new UnicodePropertyException("Property not listed for file: " + propInfo);
                    //                    }
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
                case Confusables: {
                    final PropertyParsingInfo propInfo = property2PropertyInfo.get(UcdProperty.forString("Confusable_" + parts[2]));
                    if (!propInfoSet.contains(propInfo)) {
                        throw new UnicodePropertyException("Property not listed for file: " + propInfo);
                    }
                    final UnicodeMap<String> data = property2UnicodeMap.get(propInfo.property);
                    propInfo.put(data, intRange, parts[1]);
                    break;
                }
                case StandardizedVariants:
                    if (!parts[2].isEmpty()) {
                        parts[1] = parts[1] + " (" + parts[2] + ")";
                    }
                    //$FALL-THROUGH$
                case NameAliases:
                    merger = ALPHABETIC_JOINER;
                    //$FALL-THROUGH$
                case HackField:
                    if (parts[1].endsWith("Last>")) {
                        intRange.start = lastCodepoint + 1;
                    }
                    if (parts[1].startsWith("<")) {
                        if (parts[1].contains("Ideograph")) {
                            parts[1] = "CJK UNIFIED IDEOGRAPH-#";
                        } else if (parts[1].contains("Hangul Syllable")) {
                            parts[1] = CONSTRUCTED_NAME;
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
                        final UnicodeMap<String> data = property2UnicodeMap.get(propInfo.property);
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
                    final UnicodeMap<String> data = property2UnicodeMap.get(propInfo.property);
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
            if (SHOW_LOADED) {
                System.out.println("Version\t" + getUcdVersion() + "\tLoaded: " + fileInfo.file + "\tlines: " + lineCount + (containsEOF ? "" : "\t*NO '# EOF'"));
            }
        }
        for (final PropertyParsingInfo propInfo : propInfoSet) {
            final UnicodeMap<String> data = property2UnicodeMap.get(propInfo.property);
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
                final UnicodeMap<String> otherMap = load(sourceProp); // recurse
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
            if (FILE_CACHE) {
                storeCachedMap(propInfo.property, data);
            }
        }
        return property2UnicodeMap.get(prop2);
    }

    private void storeCachedMap(UcdProperty prop2, UnicodeMap<String> data) {
        try {
            final String cacheFileDirName = Settings.BIN_DIR + getUcdVersion();
            final File cacheFileDir = new File(cacheFileDirName);
            if (!cacheFileDir.exists()) {
                cacheFileDir.mkdir();
            }
            final String cacheFileName = cacheFileDirName + "/" + prop2 + ".bin";
            final FileOutputStream fos = new FileOutputStream(cacheFileName);
            final OutputStream gz = GZIP ? new GZIPOutputStream(fos) : fos;
            final DataOutputStream out = new DataOutputStream(gz);
            final ItemWriter<String> stringWriter = new UnicodeDataOutput.StringWriter();
            if (SIMPLE_COMPRESSION) {
                final UnicodeDataOutput unicodeDataOutput = new UnicodeDataOutput();
                unicodeDataOutput.set(out, true).writeUnicodeMap(data, stringWriter);
            } else {
                UnicodeDataOutput.writeUnicodeMap(data, stringWriter, out);
            }
            out.flush();
            out.close();
            gz.close();
            fos.close();
            cacheFileSize.put(prop2, new File(cacheFileName).length());
        } catch (final IOException e) {
            throw new UnicodePropertyException(e);
        }
        // for verification
        final UnicodeMap<String> dup = getCachedMap(prop2, null);
        if (!data.equals(dup)) {
            throw new UnicodePropertyException("Failed storage");
        }
    }

    /**
     * Return null if there is no cached version.
     * @param prop2
     * @param sourceFileName
     * @return
     */
    private UnicodeMap<String> getCachedMap(UcdProperty prop2, String sourceFileName) {
        FileInputStream fis;
        File cacheFile;
        try {
            final String cacheFileName = Settings.BIN_DIR + getUcdVersion() + "/" + prop2 + ".bin";
            cacheFile = new File(cacheFileName);
            // if the source file is older than the cached, skip
            if (sourceFileName != null) {
                final File sourceFile = new File(sourceFileName);
                if (sourceFile.lastModified() > cacheFile.lastModified()) {
                    return null;
                }
            }
            fis = new FileInputStream(cacheFile);
            cacheFileSize.put(prop2, cacheFile.length());
        } catch (final Exception e) {
            return null;
        }
        try {
            final InputStream gs = GZIP ? new GZIPInputStream(fis) : fis;
            final DataInputStream in = new DataInputStream(gs);
            final ItemReader<String> stringReader = new UnicodeDataInput.StringReader();
            UnicodeMap<String> newItem;
            if (SIMPLE_COMPRESSION) {
                final UnicodeDataInput unicodeDataInput = new UnicodeDataInput();
                newItem = unicodeDataInput.set(in, true).readUnicodeMap(stringReader);
            } else {
                newItem = UnicodeDataInput.readUnicodeMap(stringReader, in);
            }
            in.close();
            gs.close();
            fis.close();
            cacheFileSize.put(prop2, cacheFile.length());
            return newItem.freeze();
        } catch (final IOException e) {
            throw new UnicodePropertyException(e);
        }
    }

    public static void handleMissing(FileType fileType, Set<PropertyParsingInfo> propInfoSet, String missing) {
        if (!missing.contains("@missing")) { // quick test
            return;
        }
        final Matcher simpleMissingMatcher = SIMPLE_MISSING_PATTERN.matcher(missing);
        if (!simpleMissingMatcher.lookingAt()) {
            //            if (missing.contains("@missing")) {
            //                System.out.println("Skipping " + missing + "\t" + RegexUtilities.showMismatch(simpleMissingMatcher, missing));
            //            }
            return;
        }
        final Matcher missingMatcher = MISSING_PATTERN.matcher(missing);
        if (!missingMatcher.matches()) {
            System.err.println(RegexUtilities.showMismatch(MISSING_PATTERN, missing));
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
                setPropDefault(propInfo.property, value, missing, isEmpty);
            }
            break;
        }
        case PropertyValue:
        case Confusables: {
            final UcdProperty ucdProp = UcdProperty.forString(value1);
            final PropertyParsingInfo propInfo = property2PropertyInfo.get(ucdProp);
            setPropDefault(propInfo.property, value2, missing, isEmpty);
            break;
        }
        default:
            throw new IllegalArgumentException("Unhandled missing line: " + missing);
        }
    }

    public static void setPropDefault(UcdProperty prop, String value, String line, boolean isEmpty) {
        if (prop == CHECK_MISSING) {
            System.out.format("** %s %s %s %s\n", prop, value, line, isEmpty);
        }
        final PropertyParsingInfo propInfo = property2PropertyInfo.get(prop);

        if (value != null && !value.startsWith("<")) {
            value = propInfo.normalizeAndVerify(value);
        }

        if (propInfo.getDefaultValue() == null) {
            propInfo.defaultValueType = DefaultValueType.forString(value);
            propInfo.defaultValue = value;
            if (SHOW_DEFAULTS) {
                getDataLoadingErrors().put(prop, "**\t" + prop + "\t" + propInfo.defaultValueType + "\t" + propInfo.getDefaultValue());
            }
        } else if (propInfo.getDefaultValue().equals(value)) {
        } else {
            final String comment = "\t ** ERROR Will not change default for " + prop +
                    " to «" + value + "», retaining " + propInfo.getDefaultValue();
            //            propInfo.defaultValueType = DefaultValueType.forString(value);
            //            propInfo.defaultValue = value;
            getDataLoadingErrors().put(prop, comment);
        }
    }

    // # @missing: 0000..10FFFF; cjkIRG_KPSource; <none>
    // # @missing: 0000..10FFFF; Other

    static void setFile2PropertyInfoSet(Relation<String,PropertyParsingInfo> file2PropertyInfoSet) {
        IndexUnicodeProperties.file2PropertyInfoSet = file2PropertyInfoSet;
    }

    /**
     * @param ucdVersion2 
     * @internal
     * @deprecated
     */
    public static Relation<String,PropertyParsingInfo> getFile2PropertyInfoSet() {
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

    public static String getResolvedValue(IndexUnicodeProperties props, UcdProperty prop, String codepoint, String value) {
        if (value == null && props != null) {
            if (getResolvedDefaultValueType(prop) == DefaultValueType.CODE_POINT) {
                return codepoint;
            }
        }
        if (prop == UcdProperty.Name && value != null && value.endsWith("-#")) {
            return value.substring(0,value.length()-1) + Utility.hex(codepoint);
        }
        return value;
    }

    public static String getResolvedValue(IndexUnicodeProperties props, UcdProperty prop, int codepoint, String value) {
        if (value == null && props != null) {
            if (getResolvedDefaultValueType(prop) == DefaultValueType.CODE_POINT) {
                return UTF16.valueOf(codepoint);
            }
        }
        if (prop == UcdProperty.Name && value != null && value.endsWith("-#")) {
            return value.substring(0,value.length()-1) + Utility.hex(codepoint);
        }
        return value;
    }

    public static DefaultValueType getResolvedDefaultValueType(UcdProperty prop) {
        while (true) {
            final DefaultValueType result = property2PropertyInfo.get(prop).defaultValueType;
            if (result.property == null) {
                return result;
            }
            prop = result.property;
        }
    }

    public static String getDefaultValue(UcdProperty prop) {
        return property2PropertyInfo.get(prop).defaultValue;
    }

    public String getResolvedValue(UcdProperty prop, String codepoint) {
        return getResolvedValue(this, prop, codepoint, this.getRawValue(prop, codepoint));
    }

    public String getResolvedValue(UcdProperty prop, int codepoint) {
        return getResolvedValue(this, prop, codepoint, this.getRawValue(prop, codepoint));
    }

    public String getRawValue(UcdProperty ucdProperty, String codepoint) {
        return load(ucdProperty).get(codepoint);
    }

    public String getRawValue(UcdProperty ucdProperty, int codepoint) {
        return load(ucdProperty).get(codepoint);
    }

    public static String normalizeValue(UcdProperty property, String propertyValue) {
        final PropertyParsingInfo info = getPropertyInfo(property);
        propertyValue = info.normalizeEnum(propertyValue);
        return propertyValue;
    }

    public List<UcdProperty> getAvailableUcdProperties() {
        return Arrays.asList(UcdProperty.values());
    }

    public static Relation<UcdProperty,String> getDataLoadingErrors() {
        return DATA_LOADING_ERRORS;
    }

    public Set<String> getFileNames() {
        return fileNames;
    }

    public VersionInfo getUcdVersion() {
        return ucdVersion;
    }

    class IndexUnicodeProperty extends UnicodeProperty.BaseProperty {

        private final UcdProperty prop;
        private final Map<String, PropertyNames> stringToNamedEnum;

        IndexUnicodeProperty(UcdProperty item) {
            this.prop = item;
            setName(prop.name());
            setType(prop.getType().getOldNumber());
            if (prop.getEnums() == null) {
                stringToNamedEnum = null;
            } else {
                Map<String, PropertyNames> _stringToNamedEnum = new HashMap<String, PropertyNames>();
                for (Enum enum2 : prop.getEnums()) {
                    Named namedEnum = (PropertyNames.Named) enum2;
                    PropertyNames names = namedEnum.getNames();
                    List<String> allNames = names.getAllNames();
                    for (String name : allNames) {
                        _stringToNamedEnum.put(name, names);
                    }
                }
                stringToNamedEnum = Collections.unmodifiableMap(_stringToNamedEnum);
            }
        }

        protected UnicodeMap<String> _getUnicodeMap() {
            return load(prop);
        }

        @Override
        protected String _getValue(int codepoint) {
            return _getUnicodeMap().get(codepoint);
        }

        @Override
        public List<String> _getNameAliases(List result) {
            result.addAll(prop.getNames().getAllNames());
            return result;
        }

        @Override
        protected List<String> _getAvailableValues(List result) {
            return _getUnicodeMap().getAvailableValues(result);
        }

        @Override
        protected List _getValueAliases(String valueAlias, List result) {
            if (stringToNamedEnum != null) {
                PropertyNames valueName = stringToNamedEnum.get(valueAlias);
                result.addAll(valueName.getAllNames());
            } else if (prop.getType() == PropertyType.Numeric) {
                if (_getUnicodeMap().containsValue(valueAlias)) {
                    result.add(valueAlias);
                }
            }
            return result;
        }
        //        @Override
        //        public boolean hasUniformUnassigned() {
        //            //throw new UnsupportedOperationException();
        //            return false;
        //        }
    }

    {
        for (UcdProperty item : UcdProperty.values()) {
            add(new IndexUnicodeProperty(item));
        }
    }

}
