package org.unicode.props;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.UnicodeMap;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUException;
import com.ibm.icu.util.VersionInfo;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
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
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.unicode.draft.CldrUtility.VariableReplacer;
import org.unicode.draft.UnicodeDataInput;
import org.unicode.draft.UnicodeDataInput.ItemReader;
import org.unicode.draft.UnicodeDataOutput;
import org.unicode.draft.UnicodeDataOutput.ItemWriter;
import org.unicode.props.PropertyNames.Named;
import org.unicode.props.PropertyUtilities.Merge;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.props.UcdPropertyValues.Binary;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

/**
 * TODO StandardizedVariants and NameSequences*
 *
 * @author markdavis
 */
public class IndexUnicodeProperties extends UnicodeProperty.Factory {
    public static final String UNCHANGED_IN_BASE_VERSION = "👉 SEE OTHER VERSION OF UNICODE";
    static final String SET_SEPARATOR = "|";

    /** Control file caching */
    static final boolean GZIP = true;

    static final boolean SIMPLE_COMPRESSION = true;
    static final boolean FILE_CACHE = System.getProperty("DISABLE_PROP_FILE_CACHE") == null;

    /** Debugging */
    static final boolean SHOW_DEFAULTS = false;

    private static final boolean CHECK_PROPERTY_STATUS = false;
    static final UcdProperty CHECK_PROPERTY =
            null; // UcdProperty.Bidi_Class; // UcdProperty.Numeric_Value; //

    static Normalizer2 NFD = Normalizer2.getNFDInstance(); //
    // static Normalizer2 NFD2 = Normalizer2.getInstance(null, "NFC", Mode.DECOMPOSE);

    static final boolean SHOW_LOADED = false;

    public static final String FIELD_SEPARATOR = "; ";
    private static final Relation<UcdProperty, String> DATA_LOADING_ERRORS =
            Relation.of(
                    new EnumMap<UcdProperty, Set<String>>(UcdProperty.class), LinkedHashSet.class);

    public enum DefaultValueType {
        LITERAL(null),
        NONE(null),
        CODE_POINT(null),
        Script(UcdProperty.Script),
        Script_Extensions(UcdProperty.Script_Extensions),
        Simple_Lowercase_Mapping(UcdProperty.Simple_Lowercase_Mapping),
        Simple_Titlecase_Mapping(UcdProperty.Simple_Titlecase_Mapping),
        Simple_Uppercase_Mapping(UcdProperty.Simple_Uppercase_Mapping);
        static final HashMap<String, DefaultValueType> mapping =
                new HashMap<String, DefaultValueType>();

        static {
            mapping.put("<none>", NONE);
            mapping.put("<slc>", Simple_Lowercase_Mapping);
            mapping.put("<stc>", Simple_Titlecase_Mapping);
            mapping.put("<suc>", Simple_Uppercase_Mapping);
            mapping.put("<codepoint>", CODE_POINT);
            mapping.put("<code point>", CODE_POINT);
            mapping.put("<script>", Script);
            // mapping.put("NaN", LITERAL);
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

    // static final Joiner JOIN = new Joiner(FIELD_SEPARATOR);

    static Map<VersionInfo, IndexUnicodeProperties> version2IndexUnicodeProperties =
            new ConcurrentHashMap<>();

    private IndexUnicodeProperties(VersionInfo ucdVersion2, IndexUnicodeProperties base) {
        ucdVersion = ucdVersion2;
        oldVersion = ucdVersion2.compareTo(GenerateEnums.ENUM_VERSION_INFO) < 0;
        baseVersionProperties = base;
    }

    // TODO(egg): Too much stuff puts its hands in the raw maps to be able to do this by default.
    // Remove these static warts once https://github.com/unicode-org/unicodetools/issues/716 is
    // fixed.
    private static boolean incrementalProperties = false;

    public static synchronized void useIncrementalProperties() {
        if (!incrementalProperties && !version2IndexUnicodeProperties.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot switch to incremental storage after making IUPs");
        }
        incrementalProperties = true;
    }

    public static final synchronized IndexUnicodeProperties make(VersionInfo ucdVersion) {
        IndexUnicodeProperties newItem = version2IndexUnicodeProperties.get(ucdVersion);
        if (newItem == null) {
            Age_Values nextAge = Age_Values.Unassigned;
            for (int i = 0; i < Age_Values.values().length - 1; ++i) {
                final var version = VersionInfo.getInstance(Age_Values.values()[i].getShortName());
                if (version.equals(ucdVersion)) {
                    nextAge = Age_Values.values()[i + 1];
                }
            }
            IndexUnicodeProperties base =
                    !incrementalProperties || ucdVersion == Settings.LAST_VERSION_INFO
                            ? null
                            : nextAge == Age_Values.Unassigned
                                    ? make(Settings.LAST_VERSION_INFO)
                                    : make(nextAge);
            version2IndexUnicodeProperties.put(
                    ucdVersion, newItem = new IndexUnicodeProperties(ucdVersion, base));
        }
        return newItem;
    }

    public static final IndexUnicodeProperties make(String ucdVersion) {
        return make(VersionInfo.getInstance(ucdVersion));
    }

    public static final IndexUnicodeProperties make(UcdPropertyValues.Age_Values ucdVersion) {
        return make(VersionInfo.getInstance(ucdVersion.getShortName()));
    }

    public static final IndexUnicodeProperties make() {
        final Age_Values[] values = Age_Values.values();
        return make(values[values.length - 2]);
    }

    final VersionInfo ucdVersion;
    final boolean oldVersion;
    final IndexUnicodeProperties baseVersionProperties;
    final EnumMap<UcdProperty, UnicodeMap<String>> property2UnicodeMap =
            new EnumMap<UcdProperty, UnicodeMap<String>>(UcdProperty.class);
    private final Set<String> fileNames = new TreeSet<String>();
    private final Map<UcdProperty, Long> cacheFileSize =
            new EnumMap<UcdProperty, Long>(UcdProperty.class);

    public Map<UcdProperty, Long> getCacheFileSize() {
        return Collections.unmodifiableMap(cacheFileSize);
    }

    static final Transform<String, String> fromNumericPinyin =
            Transliterator.getInstance("NumericPinyin-Latin;nfc");

    static final Merge<String> ALPHABETIC_JOINER =
            new Merge<String>() {
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
    public static <T, V extends Collection<T>, U extends Map<T, UnicodeSet>> U invertSet(
            UnicodeMap<V> source, U target) {
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
    public static <T, U extends Map<T, UnicodeSet>> U invert(UnicodeMap<T> source, U target) {
        for (T value : source.values()) {
            UnicodeSet uset = source.getSet(value);
            target.put(value, uset);
        }
        return target;
    }

    // should be on UnicodeMap
    public static <T> Map<T, UnicodeSet> freeze(Map<T, UnicodeSet> target) {
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
                    convertedValue =
                            Integer.parseInt(value.substring(0, pos))
                                    / (double) Integer.parseInt(value.substring(pos + 1));
                } else {
                    String v = value;
                    if (prop2 == UcdProperty.kPrimaryNumeric
                            || prop2 == UcdProperty.kAccountingNumeric
                            || prop2 == UcdProperty.kOtherNumeric) {
                        // Unicode 15.1+: A character may have multiple Unihan numeric values.
                        pos = v.indexOf('|');
                        if (pos >= 0) {
                            v = value.substring(0, pos);
                        }
                    }
                    convertedValue = Double.parseDouble(v);
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
                if (prop2 == UcdProperty.Canonical_Combining_Class) { // hack
                    value = ((Named) prop2.getEnum(value)).getNames().getShortName();
                }
                result.putAll(uset, Integer.parseInt(value));
            }
            INT_CACHE.put(prop2, result.freeze());
        }
        return result;
    }

    public <T extends Enum<T>> UnicodeMap<T> loadEnum(UcdProperty prop2, Class<T> classIn) {
        if ((Class) classIn != (Class) prop2.getEnumClass()) {
            throw new UnicodePropertyException(
                    "Mismatch in class data,  expected " + prop2.getEnumClass());
        }
        //        UnicodeMap<T> result = SPECIAL_CACHE.get(prop2);
        //        if (result != null) {
        //            return result;
        //        }
        //        result = new UnicodeMap<>();
        //        UnicodeMap<String> m = load(prop2);
        //        for (String value : m.values()) {
        //            T enumv = (T) prop2.getEnum(value);
        //            UnicodeSet uset = m.getSet(value);
        //            result.putAll(uset, enumv);
        //        }
        //        SPECIAL_CACHE.put(prop2, result.freeze());
        return loadEnum(prop2);
    }

    public <T extends Enum<T>> UnicodeMap<T> loadEnum(UcdProperty prop2) {
        UnicodeMap<T> result = SPECIAL_CACHE.get(prop2);
        if (result != null) {
            return result;
        }
        result = new UnicodeMap<>();
        UnicodeMap<String> m = load(prop2);
        for (String value : m.values()) {
            T enumv = (T) prop2.getEnum(value);
            UnicodeSet uset = m.getSet(value);
            result.putAll(uset, enumv);
        }
        SPECIAL_CACHE.put(prop2, result.freeze());
        return result;
    }

    public UnicodeSet loadEnumSet(UcdProperty prop2, Enum value) {
        if (value.getClass() != prop2.getEnumClass()) {
            throw new UnicodePropertyException(
                    "Mismatch in class data,  expected " + prop2.getEnumClass());
        }
        UnicodeMap map = loadEnum(prop2);
        return map.getSet(value);
    }

    static final Splitter SET_SPLITTER = Splitter.on(SET_SEPARATOR);

    public <T extends Enum<T>> UnicodeMap<Set<T>> loadEnumSet(UcdProperty prop2, Class enumClass) {
        UnicodeMap<Set<T>> result = SPECIAL_CACHE.get(prop2);
        if (result != null) {
            return result;
        }
        if (enumClass != prop2.getEnumClass()
                || prop2.getCardinality() != ValueCardinality.Unordered) {
            throw new UnicodePropertyException(
                    "Mismatch in class data,  expected "
                            + prop2.getEnumClass()
                            + ", "
                            + prop2.getCardinality());
        }
        if (enumClass != prop2.getEnumClass()) {
            throw new UnicodePropertyException(
                    "Mismatch in class enum,  expected " + prop2.getEnumClass());
        }
        result = new UnicodeMap<>();
        UnicodeMap<String> m = load(prop2);
        for (String value : m.values()) {
            Set<T> convertedValue = EnumSet.noneOf(enumClass);
            for (String s : SET_SPLITTER.split(value)) {
                T enumv = (T) prop2.getEnum(s);
                // System.out.println(s + " => " + enumv);
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
            throw new UnicodePropertyException(
                    "Mismatch in class data,  expected " + prop2.getCardinality());
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
            throw new UnicodePropertyException(
                    "Mismatch in class data,  expected "
                            + prop2.getCardinality()
                            + " for "
                            + prop2);
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

    public UnicodeMap<List<Integer>> loadIntList(UcdProperty prop2) {
        UnicodeMap<List<Integer>> result = SPECIAL_CACHE.get(prop2);
        if (result != null) {
            return result;
        }
        result = new UnicodeMap<>();
        if (prop2.getCardinality() != ValueCardinality.Ordered) {
            throw new UnicodePropertyException(
                    "Mismatch in class data,  expected "
                            + prop2.getCardinality()
                            + " for "
                            + prop2);
        }
        UnicodeMap<String> m = load(prop2);
        // TODO cache
        for (String value : m.values()) {
            UnicodeSet uset = m.getSet(value);
            ArrayList<Integer> v = new ArrayList<Integer>();
            for (String s : SET_SPLITTER.splitToList(value)) {
                v.add(Integer.parseInt(s));
            }
            result.putAll(uset, Collections.unmodifiableList(v));
        }
        SPECIAL_CACHE.put(prop2, result.freeze());
        return result;
    }

    public synchronized boolean isLoaded(UcdProperty prop) {
        return property2UnicodeMap.get(prop) != null;
    }

    public synchronized UnicodeMap<String> load(UcdProperty prop2) {
        return load(prop2, false);
    }

    public synchronized UnicodeMap<String> load(UcdProperty prop2, boolean expectCacheHit) {
        String fullFilename = "?";
        try {
            if (prop2 == CHECK_PROPERTY) {
                int debug = 0;
            }
            UnicodeMap<String> data0 = property2UnicodeMap.get(prop2);
            if (data0 != null) {
                return data0;
            }

            final PropertyParsingInfo fileInfo =
                    PropertyParsingInfo.property2PropertyInfo.get(prop2);
            fullFilename = fileInfo.getFullFileName(ucdVersion);
            final String fileName = fileInfo.getFileName(ucdVersion);

            if (FILE_CACHE) {
                // TODO(egg): When using cached property data, most defaults do not get
                // loaded in PropertyParsingInfo, as that happens in parseSourceFile.
                // Only the ones from the Extra files are loaded.
                data0 = getCachedMap(prop2, fullFilename);
                if (data0 != null) {
                    property2UnicodeMap.put(prop2, data0.freeze());
                    return data0;
                }
            }

            if (expectCacheHit) {
                System.err.println("Failed to find cached " + prop2 + ", parsing from source");
            }

            PropertyParsingInfo.parseSourceFile(
                    this, baseVersionProperties, fullFilename, fileName);
            return property2UnicodeMap.get(prop2);
        } catch (Exception e) {
            throw new ICUException(prop2.toString() + "( from: " + fullFilename + ")", e);
        }
    }

    public void internalStoreCachedMap(String dir, UcdProperty prop2, UnicodeMap<String> data) {
        try {
            final var binDir = new File(dir);
            if (!binDir.exists()) {
                binDir.mkdir();
            }
            final String cacheFileDirName = dir + getUcdVersion();
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
     *
     * @param prop2
     * @param sourceFileName
     * @return
     */
    private UnicodeMap<String> getCachedMap(UcdProperty prop2, String sourceFileName) {
        File cacheFile;
        try {
            final String cacheFileName =
                    Settings.Output.BIN_DIR + getUcdVersion() + "/" + prop2 + ".bin";
            cacheFile = new File(cacheFileName);
            // if the source file is older than the cached, skip
            if (sourceFileName != null) {
                final File sourceFile = new File(sourceFileName);
                if (sourceFile.lastModified() > cacheFile.lastModified()) {
                    return null;
                }
            }
            cacheFileSize.put(prop2, cacheFile.length());
        } catch (final Exception e) {
            return null;
        }
        try (final FileInputStream fis = new FileInputStream(cacheFile);
                final InputStream gs = GZIP ? new GZIPInputStream(fis) : fis;
                final DataInputStream in = new DataInputStream(gs); ) {
            final ItemReader<String> stringReader = new UnicodeDataInput.StringReader();
            UnicodeMap<String> newItem;
            if (SIMPLE_COMPRESSION) {
                final UnicodeDataInput unicodeDataInput = new UnicodeDataInput();
                newItem = unicodeDataInput.set(in, true).readUnicodeMap(stringReader);
            } else {
                newItem = UnicodeDataInput.readUnicodeMap(stringReader, in);
            }
            cacheFileSize.put(prop2, cacheFile.length());
            return newItem.freeze();
        } catch (final IOException e) {
            return null;
        }
    }

    // # @missing: 0000..10FFFF; cjkIRG_KPSource; <none>
    // # @missing: 0000..10FFFF; Other

    public static String getResolvedValue(
            IndexUnicodeProperties props, UcdProperty prop, String codepoint, String value) {
        return props.getProperty(prop).getValue(codepoint.codePointAt(0));
    }

    public static String getResolvedValue(
            IndexUnicodeProperties props, UcdProperty prop, int codepoint, String value) {
        return props.getProperty(prop).getValue(codepoint);
    }

    UnicodeMap<String> nameMap;
    UnicodeMap<General_Category_Values> gcMap;

    public String getName(String cps, String separator) {
        StringBuffer result = new StringBuffer();
        for (int cp : CharSequences.codePoints(cps)) {
            if (result.length() != 0) {
                result.append(separator);
            }
            String name = getName(cp);
            result.append(name);
        }
        return result.toString();
    }

    public String getName(int cp) {
        if (nameMap == null) {
            nameMap = load(UcdProperty.Name);
            gcMap =
                    loadEnum(
                            UcdProperty.General_Category,
                            UcdPropertyValues.General_Category_Values.class);
        }
        String name = nameMap.get(cp);
        if (name == null) {
            final General_Category_Values gcv = gcMap.get(cp);
            name =
                    "<"
                            + (gcv == General_Category_Values.Unassigned
                                    ? "reserved"
                                    : gcv.name().toLowerCase(Locale.ENGLISH))
                            + "-"
                            + Utility.hex(cp)
                            + ">";
        } else if (name.endsWith("-#")) {
            name = name.substring(0, name.length() - 1) + Utility.hex(cp);
        }
        return name;
    }

    public static DefaultValueType getResolvedDefaultValueType(UcdProperty prop) {
        while (true) {
            final DefaultValueType result =
                    PropertyParsingInfo.property2PropertyInfo.get(prop).defaultValueType;
            if (result.property == null) {
                return result;
            }
            prop = result.property;
        }
    }

    public static String getDefaultValue(UcdProperty prop, VersionInfo version) {
        return PropertyParsingInfo.property2PropertyInfo.get(prop).getDefaultValue(version);
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
        final PropertyParsingInfo info = PropertyParsingInfo.getPropertyInfo(property);
        propertyValue = info.normalizeEnum(propertyValue);
        return propertyValue;
    }

    public List<UcdProperty> getAvailableUcdProperties() {
        return Arrays.asList(UcdProperty.values());
    }

    public static Relation<UcdProperty, String> getDataLoadingErrors() {
        return DATA_LOADING_ERRORS;
    }

    public Set<String> getFileNames() {
        return fileNames;
    }

    public VersionInfo getUcdVersion() {
        return ucdVersion;
    }

    //    public UnicodeProperty getProperty(UcdProperty ucdProperty) {
    //        return (UnicodeProperty) skeletonNames
    //        .get(toSkeleton(propertyAlias));
    //    }

    class IndexUnicodeProperty extends UnicodeProperty.BaseProperty {

        private final UcdProperty prop;
        private final Map<String, PropertyNames> stringToNamedEnum;
        private final Set<String> enumValueNames;
        // The set of code points for which the property value differs from that in
        // baseVersionProperties.
        // TODO(egg): Really, for which it may differ, but does not in the default case.
        private UnicodeSet diffSet;

        IndexUnicodeProperty(UcdProperty item) {
            this.prop = item;
            setName(prop.name());
            setType(prop.getType().getOldNumber());
            if (prop.getEnums() == null) {
                stringToNamedEnum = null;
                enumValueNames = null;
            } else {
                Map<String, PropertyNames> _stringToNamedEnum =
                        new HashMap<String, PropertyNames>();
                Set<String> _mainNames = new LinkedHashSet<String>();
                for (Enum enum2 : prop.getEnums()) {
                    Named namedEnum = (PropertyNames.Named) enum2;
                    PropertyNames names = namedEnum.getNames();
                    List<String> allNames = names.getAllNames();
                    for (String name : allNames) {
                        _stringToNamedEnum.put(name, names);
                    }
                    _mainNames.add(enum2.toString());
                }
                stringToNamedEnum = ImmutableMap.copyOf(_stringToNamedEnum);
                enumValueNames = ImmutableSet.copyOf(_mainNames);
            }
            if (PropertyParsingInfo.property2PropertyInfo.get(item).getMultivalued()
                    != ValueCardinality.Singleton) {
                setMultivalued(true);
                setDelimiter(SET_SEPARATOR);
            }
        }

        @Override
        public boolean isTrivial() {
            return _getRawUnicodeMap().isEmpty()
                    || ((_getRawUnicodeMap().stringKeys() == null
                                    || _getRawUnicodeMap().stringKeys().isEmpty())
                            && _getRawUnicodeMap()
                                    .keySet(_getRawUnicodeMap().getValue(0))
                                    .equals(UnicodeSet.ALL_CODE_POINTS));
        }

        @Override
        protected UnicodeMap<String> _getUnicodeMap() {
            var raw = _getRawUnicodeMap();
            if (prop == UcdProperty.Name
                    || raw.containsValue("<code point>")
                    || raw.containsValue("<codepoint>")) {
                final long start = System.currentTimeMillis();
                UnicodeMap<String> newMap = new UnicodeMap<>();
                for (UnicodeMap.EntryRange<String> range : raw.entryRanges()) {
                    if (range.codepoint == -1) {
                        newMap.put(range.string, range.value);
                    } else if (DefaultValueType.forString(range.value)
                                    == DefaultValueType.CODE_POINT
                            || (prop == UcdProperty.Name && range.value.endsWith("#"))) {
                        for (int c = range.codepoint; c <= range.codepointEnd; ++c) {
                            newMap.put(c, resolveValue(range.value, c));
                        }
                    } else {
                        newMap.putAll(range.codepoint, range.codepointEnd, range.value);
                    }
                }
                final long stop = System.currentTimeMillis();
                final long Δt_in_ms = stop - start;
                // We do not want to construct these UnicodeMaps that map most of the code space to
                // itself, not so much because building them is costly, but because whatever we do
                // on them is almost certainly a bad idea (for instance calling `values()` will be
                // extremely slow).  Log a trace so we can figure out where we are using this.
                System.out.println(
                        "Built " + prop + " " + ucdVersion + " map in " + Δt_in_ms + " ms");
                new Throwable().printStackTrace(System.out);

                return newMap;
            } else {
                return raw;
            }
        }

        protected UnicodeMap<String> _getRawUnicodeMap() {
            return load(prop);
        }

        private UnicodeSet getDiffSet() {
            if (diffSet == null) {
                diffSet =
                        _getRawUnicodeMap().keySet(UNCHANGED_IN_BASE_VERSION).complement().freeze();
            }
            return diffSet;
        }

        @Override
        protected String _getValue(int codepoint) {
            final String result = _getRawUnicodeMap().get(codepoint);
            return resolveValue(result, codepoint);
        }

        @Override
        public UnicodeSet getSet(PatternMatcher matcher, UnicodeSet result) {
            if (baseVersionProperties == null) {
                return super.getSet(matcher, result);
            }
            final long start = System.currentTimeMillis();
            final UnicodeSet baseSet = baseVersionProperties.getProperty(prop).getSet(matcher);
            final UnicodeSet matchingInThisVersion =
                    super.getSet(matcher, null).retainAll(getDiffSet());
            baseSet.addAll(matchingInThisVersion)
                    .removeAll(getDiffSet().cloneAsThawed().removeAll(matchingInThisVersion));
            if (result == null) {
                result = baseSet;
            } else {
                result.addAll(baseSet);
            }
            final long stop = System.currentTimeMillis();
            final long Δt_in_ms = stop - start;
            if (Δt_in_ms > 100) {
                System.out.println(
                        "Long getSet for U" + ucdVersion + ":" + prop + " (" + Δt_in_ms + " ms)");
            }
            // We only do the delta thing for code points; for strings, we need to do the lookup
            // directly (and clean whatever was added by walking through history).
            if (baseVersionProperties != null
                    && (result.hasStrings()
                            || (_getRawUnicodeMap().stringKeys() != null
                                    && !_getRawUnicodeMap().stringKeys().isEmpty()))) {
                result.removeAllStrings().addAll(super.getSet(matcher, new UnicodeSet()).strings());
            }
            return result;
        }

        private String resolveValue(String rawValue, int codepoint) {
            if (UNCHANGED_IN_BASE_VERSION.equals(rawValue)) {
                return baseVersionProperties.getProperty(prop).getValue(codepoint);
            }
            if (DefaultValueType.forString(rawValue) == DefaultValueType.CODE_POINT) {
                return Character.toString(codepoint);
            } else if (prop == UcdProperty.Name && rawValue != null && rawValue.endsWith("#")) {
                return rawValue.substring(0, rawValue.length() - 1) + Utility.hex(codepoint);
            } else {
                return rawValue;
            }
        }

        @Override
        public List<String> _getNameAliases(List result) {
            result.addAll(prop.getNames().getAllNames());
            return result;
        }

        @Override
        protected List<String> _getAvailableValues(List<String> result) {
            if (stringToNamedEnum != null) {
                result.addAll(enumValueNames);
                return result;
            }
            if (isMultivalued()) {
                HashSet<String> valueSet = new HashSet<>();
                for (var value : _getUnicodeMap().getAvailableValues()) {
                    for (var part : delimiterSplitter.split(value)) {
                        valueSet.add(part);
                    }
                }
                result.addAll(valueSet);
                return result;
            }
            return _getUnicodeMap().getAvailableValues(result);
        }

        @Override
        protected List _getValueAliases(String valueAlias, List result) {
            if (stringToNamedEnum != null) {
                PropertyNames valueName = stringToNamedEnum.get(valueAlias);
                if (valueName != null) {
                    result.addAll(valueName.getAllNames());
                }
            }
            if (!result.contains(valueAlias)) {
                // TODO(egg): We should not be constructing this map for this.
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

    public UnicodeProperty getProperty(UcdProperty ucdProperty) {
        return getProperty(ucdProperty.toString());
    }

    public UnicodeSet loadBinary(UcdProperty ucdProp) {
        return load(ucdProp).getSet(Binary.Yes.toString());
    }

    public static void loadUcdHistory(
            VersionInfo earliest, Consumer<VersionInfo> notifyLoaded, boolean expectCacheHit) {
        useIncrementalProperties();
        System.out.println(
                "Loading back to " + (earliest == null ? "the dawn of time" : earliest) + "...");
        Age_Values[] ages = Age_Values.values();
        final long overallStart = System.currentTimeMillis();
        for (int i = ages.length - 2; i >= 0; --i) {
            // Load in the order last (released, the base), latest (dev), penultimate,
            // antepenultimate, etc.
            final var age =
                    ages[
                            i == ages.length - 2
                                    ? ages.length - 3
                                    : i == ages.length - 3 ? ages.length - 2 : i];
            final long ucdStart = System.currentTimeMillis();
            System.out.println("Loading UCD " + age.getShortName() + "...");
            for (boolean unihan : new boolean[] {false, true}) {
                final long partStart = System.currentTimeMillis();
                final String name = unihan ? "Unihan" : "non-Unihan properties";
                final var properties = IndexUnicodeProperties.make(age.getShortName());
                for (UcdProperty property : UcdProperty.values()) {
                    if (property.getShortName().startsWith("cjk") == unihan) {
                        try {
                            properties.load(property, expectCacheHit);
                        } catch (ICUException e) {
                            e.printStackTrace();
                        }
                    }
                }
                System.out.println(
                        "Loaded "
                                + name
                                + " for "
                                + age.getShortName()
                                + " ("
                                + (System.currentTimeMillis() - partStart)
                                + " ms)");
            }
            System.out.println(
                    "Loaded UCD "
                            + age.getShortName()
                            + " in "
                            + (System.currentTimeMillis() - ucdStart)
                            + " ms");
            var version = VersionInfo.getInstance(age.getShortName());
            if (notifyLoaded != null) {
                notifyLoaded.accept(version);
            }
            if (version == earliest) {
                break;
            }
        }
        System.out.println(
                "Loaded "
                        + (earliest == null ? "all UCD history" : "UCD history back to " + earliest)
                        + " in "
                        + (System.currentTimeMillis() - overallStart) / 1000
                        + " s");
    }
}
