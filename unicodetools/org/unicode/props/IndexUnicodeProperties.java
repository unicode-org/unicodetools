package org.unicode.props;

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.unicode.draft.CldrUtility.VariableReplacer;
import org.unicode.draft.UnicodeDataInput;
import org.unicode.draft.UnicodeDataInput.ItemReader;
import org.unicode.draft.UnicodeDataOutput;
import org.unicode.draft.UnicodeDataOutput.ItemWriter;
import org.unicode.props.PropertyNames.Named;
import org.unicode.props.PropertyUtilities.Merge;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

import com.google.common.base.Splitter;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeProperty;
import com.ibm.icu.impl.Relation;
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
    static final String SET_SEPARATOR = "|";
    /**
     * Control file caching
     */
    static final boolean GZIP = true;
    static final boolean SIMPLE_COMPRESSION = true;
    static final boolean FILE_CACHE = System.getProperty("DISABLE_PROP_FILE_CACHE") == null;

    /**
     * Debugging
     */
    static final boolean SHOW_DEFAULTS = false;
    private static final boolean CHECK_PROPERTY_STATUS = false;
    static final UcdProperty CHECK_PROPERTY = null; // UcdProperty.Bidi_Class; // UcdProperty.Numeric_Value; //

    static Normalizer2 NFD = Normalizer2.getNFDInstance(); //
    //static Normalizer2 NFD2 = Normalizer2.getInstance(null, "NFC", Mode.DECOMPOSE);

    static final boolean SHOW_LOADED = false;

    public final static String FIELD_SEPARATOR = "; ";
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

    //static final Joiner JOIN = new Joiner(FIELD_SEPARATOR);

    static Map<VersionInfo, IndexUnicodeProperties> version2IndexUnicodeProperties = new HashMap<>();

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

    final VersionInfo ucdVersion;
    final boolean oldVersion;
    final EnumMap<UcdProperty, UnicodeMap<String>> property2UnicodeMap = new EnumMap<UcdProperty, UnicodeMap<String>>(UcdProperty.class);
    private final Set<String> fileNames = new TreeSet<String>();
    private final Map<UcdProperty, Long> cacheFileSize = new EnumMap<UcdProperty, Long>(UcdProperty.class);

    public Map<UcdProperty, Long> getCacheFileSize() {
        return Collections.unmodifiableMap(cacheFileSize);
    }

    static final Transform<String, String>    fromNumericPinyin   = Transliterator.getInstance("NumericPinyin-Latin;nfc");

    static final Merge<String> ALPHABETIC_JOINER = new Merge<String>() {
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
                    + prop2.getCardinality() + " for " + prop2);
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
            throw new UnicodePropertyException("Mismatch in class data,  expected " 
                    + prop2.getCardinality() + " for " + prop2);
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

    public UnicodeMap<String> load(UcdProperty prop2) {
        if (prop2 == CHECK_PROPERTY) {
            int debug = 0;
        }
        UnicodeMap<String> data0 = property2UnicodeMap.get(prop2);
        if (data0 != null) {
            return data0;
        }

        final PropertyParsingInfo fileInfo = PropertyParsingInfo.property2PropertyInfo.get(prop2);
        final String fullFilename = fileInfo.getFullFileName(ucdVersion);
        final String fileName = fileInfo.getFileName(ucdVersion);

        if (FILE_CACHE) {
            data0 = getCachedMap(prop2, fullFilename);
            if (data0 != null) {
                property2UnicodeMap.put(prop2, data0.freeze());
                return data0;
            }
        }

        fileInfo.parseSourceFile(this, fullFilename, fileName);
        return property2UnicodeMap.get(prop2);
    }

    void storeCachedMap(UcdProperty prop2, UnicodeMap<String> data) {
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
            cacheFileSize.put(prop2, cacheFile.length());
        } catch (final Exception e) {
            return null;
        }
        try (final FileInputStream fis = new FileInputStream(cacheFile);
                final InputStream gs = GZIP ? new GZIPInputStream(fis) : fis;
                final DataInputStream in = new DataInputStream(gs);) {
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
            throw new UnicodePropertyException(e);
        }
    }



    // # @missing: 0000..10FFFF; cjkIRG_KPSource; <none>
    // # @missing: 0000..10FFFF; Other

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
            final DefaultValueType result = PropertyParsingInfo.property2PropertyInfo.get(prop).defaultValueType;
            if (result.property == null) {
                return result;
            }
            prop = result.property;
        }
    }

    public static String getDefaultValue(UcdProperty prop) {
        return PropertyParsingInfo.property2PropertyInfo.get(prop).defaultValue;
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
