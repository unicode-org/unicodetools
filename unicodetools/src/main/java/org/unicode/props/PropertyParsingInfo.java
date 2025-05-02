package org.unicode.props;

import com.google.common.base.Splitter;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.UnicodeMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.VersionInfo;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.RegexUtilities;
import org.unicode.idna.Regexes;
import org.unicode.props.IndexUnicodeProperties.DefaultValueType;
import org.unicode.props.PropertyUtilities.Merge;
import org.unicode.props.UcdLineParser.IntRange;
import org.unicode.props.UcdLineParser.UcdLine.Contents;
import org.unicode.props.UcdPropertyValues.Binary;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

/**
 * @internal
 */
public class PropertyParsingInfo implements Comparable<PropertyParsingInfo> {
    enum SpecialProperty {
        None,
        Skip1FT,
        Skip1ST,
        SkipAny4,
    }

    private static final String NEW_UNICODE_PROPS_DOCS =
            "https://github.com/unicode-org/unicodetools/blob/main/docs/newunicodeproperties.md";
    private static final VersionInfo MIN_VERSION = VersionInfo.getInstance(0, 0, 0, 0);
    public final UcdProperty property;
    public final SpecialProperty special;

    /**
     * Maps from Unicode versions to field number. A property whose field number depends on the
     * version has more than one entry. A particular field number applies to the Unicode versions
     * after the previous-version entry, up to and including its own version.
     */
    TreeMap<VersionInfo, Integer> fieldNumbers;

    /**
     * Maps from Unicode versions to files. A property whose file depends on the version has more
     * than one entry. A particular file applies to the Unicode versions after the previous-version
     * entry, up to and including its own version.
     */
    TreeMap<VersionInfo, String> files;

    /**
     * Maps from Unicode versions to default values. A property whose default value depends on the
     * version has more than one entry. A particular default value applies to the Unicode versions
     * after the previous-version entry, up to and including its own version.
     */
    TreeMap<VersionInfo, String> defaultValues = new TreeMap<>();

    DefaultValueType defaultValueType = DefaultValueType.LITERAL;
    // UnicodeMap<String> data;
    // final Set<String> errors = new LinkedHashSet<String>();
    Pattern regex = null;
    private ValueCardinality multivalued = ValueCardinality.Singleton;
    private Pattern multivaluedSplit = SPACE;
    public String originalRegex;
    public static final Pattern TAB = Pattern.compile("[ ]*\t[ ]*");
    static final Pattern MISSING_PATTERN =
            Pattern.compile(
                    "\\s*#\\s*@(missing|empty):?"
                            + "\\s*([A-Fa-f0-9]+)..([A-Fa-f0-9]+)\\s*;"
                            + "\\s*([^;]*)"
                            + "(?:\\s*;\\s*([^;]*)"
                            + "(?:\\s*;\\s*([^;]*))?)?\\s*"
                            + ";?");
    static final Pattern SIMPLE_MISSING_PATTERN = Pattern.compile("\\s*#\\s*@(missing|empty)");
    static final String CONSTRUCTED_NAME = "$HANGUL_SYLLABLE$";
    static Pattern SLASHX = Pattern.compile("\\\\x\\{([0-9A-Fa-f]{1,6})\\}");
    public static final Pattern NO_SPLIT = Pattern.compile("\\uFFFF");
    public static final Pattern SPACE = Pattern.compile("\\s+");
    public static final Pattern EQUALS = Pattern.compile("\\s*=\\s*");
    public static final Pattern COMMA = Pattern.compile("\\s*,\\s*");
    public static final Pattern DECOMP_REMOVE = Pattern.compile("\\{[^}]+\\}|\\<[^>]+\\>");

    /** General constants */
    public static final Pattern SEMICOLON = Pattern.compile("\\s*;\\s*");

    static EnumMap<UcdProperty, PropertyParsingInfo> property2PropertyInfo =
            new EnumMap<UcdProperty, PropertyParsingInfo>(UcdProperty.class);
    static Relation<String, PropertyParsingInfo> file2PropertyInfoSet =
            Relation.of(new HashMap<String, Set<PropertyParsingInfo>>(), HashSet.class);

    public PropertyParsingInfo(
            String file, UcdProperty property, int fieldNumber, SpecialProperty special) {
        this.files = new TreeMap<>();
        files.put(Settings.LATEST_VERSION_INFO, file);
        this.property = property;
        this.fieldNumbers = new TreeMap<>();
        fieldNumbers.put(Settings.LATEST_VERSION_INFO, fieldNumber);
        this.special = special;
    }

    static final Pattern VERSION = Pattern.compile("v\\d+(\\.\\d+)+");

    private static void fromStrings(String... propertyInfo) {
        if (propertyInfo.length < 2 || propertyInfo.length > 4) {
            throw new UnicodePropertyException(
                    "Must have 2 to 4 args: " + Arrays.asList(propertyInfo));
        }
        String _file = propertyInfo[0];
        final String propName = propertyInfo[1];
        UcdProperty _property = UcdProperty.forString(propName);
        if (_property == null) {
            throw new IllegalArgumentException("No such property: " + propName);
        }

        String last = propertyInfo[propertyInfo.length - 1];

        int temp = 1;
        if (propertyInfo.length > 2
                && !propertyInfo[2].isEmpty()
                && !VERSION.matcher(propertyInfo[2]).matches()) {
            temp = Integer.parseInt(propertyInfo[2]);
        }
        int _fieldNumber = temp;

        if (VERSION.matcher(last).matches()) {
            propertyInfo[propertyInfo.length - 1] = "";
            PropertyParsingInfo result = property2PropertyInfo.get(_property);
            if (result == null) {
                throw new IllegalArgumentException(
                        "No modern info for property with old file record: " + propName);
            }
            result.files.put(VersionInfo.getInstance(last.substring(1)), _file);
            result.fieldNumbers.put(VersionInfo.getInstance(last.substring(1)), _fieldNumber);
            file2PropertyInfoSet.put(_file, result);
            return;
        }

        SpecialProperty _special =
                propertyInfo.length < 4 || propertyInfo[3].isEmpty()
                        ? SpecialProperty.None
                        : SpecialProperty.valueOf(propertyInfo[3]);
        PropertyParsingInfo result =
                new PropertyParsingInfo(_file, _property, _fieldNumber, _special);

        try {
            PropertyUtilities.putNew(property2PropertyInfo, _property, result);
            file2PropertyInfoSet.put(_file, result);
        } catch (final Exception e) {
            throw new UnicodePropertyException("Can't find property for <" + propName + ">", e);
        }
    }

    private static void fromUnihanProperty(UcdProperty prop) {
        String filename = prop.toString();
        if (file2PropertyInfoSet.containsKey(filename)) {
            return;
        }
        PropertyParsingInfo info = property2PropertyInfo.get(prop);
        if (info == null) {
            info = new PropertyParsingInfo(filename, prop, 1, SpecialProperty.None);
            property2PropertyInfo.put(prop, info);
        }
        file2PropertyInfoSet.put(filename, info);
    }

    @Override
    public String toString() {
        return files
                + " ;\t"
                + property
                + " ;\t"
                + fieldNumbers
                + " ;\t"
                + special
                + " ;\t"
                + defaultValues
                + " ;\t"
                + defaultValueType
                + " ;\t"
                + getMultivalued()
                + " ;\t"
                + getRegex()
                + " ;\t";
    }

    // TODO(egg): This compares a strange subset of the fields which may be a historical artefact.
    @Override
    public int compareTo(PropertyParsingInfo arg0) {
        int result;
        if (0
                != (result =
                        files.get(Settings.LATEST_VERSION_INFO)
                                .compareTo(arg0.files.get(Settings.LATEST_VERSION_INFO)))) {
            return result;
        }
        if (0 != (result = property.toString().compareTo(arg0.property.toString()))) {
            return result;
        }
        return fieldNumbers.get(Settings.LATEST_VERSION_INFO)
                - arg0.fieldNumbers.get(Settings.LATEST_VERSION_INFO);
    }

    public static String getFullFileName(UcdProperty prop, VersionInfo ucdVersion) {
        return getPropertyInfo(prop).getFullFileName(ucdVersion);
    }

    public String getFullFileName(VersionInfo ucdVersionRequested) {
        return Utility.getMostRecentUnicodeDataFile(
                getFileName(ucdVersionRequested), ucdVersionRequested.toString(), true, false);
    }

    public String getFileName(VersionInfo ucdVersionRequested) {
        String file = null;
        for (final var entry : files.entrySet()) {
            if (ucdVersionRequested.compareTo(entry.getKey()) <= 0) {
                file = entry.getValue();
                break;
            }
        }
        if (file.startsWith("Unihan") && ucdVersionRequested.compareTo(V13) >= 0) {
            return property.toString();
        } else {
            return file;
        }
    }

    public int getFieldNumber(VersionInfo ucdVersionRequested) {
        int fieldNumber = 0;
        if (fieldNumbers.size() == 1) {
            return fieldNumbers.values().iterator().next();
        }
        for (final var entry : fieldNumbers.entrySet()) {
            if (ucdVersionRequested.compareTo(entry.getKey()) <= 0) {
                fieldNumber = entry.getValue();
                break;
            }
        }
        return fieldNumber;
    }

    private static final VersionInfo V13 = VersionInfo.getInstance(13);

    public static final Normalizer2 NFD = Normalizer2.getNFDInstance();
    public static final Normalizer2 NFC = Normalizer2.getNFCInstance();

    public void put(
            UnicodeMap<String> data,
            UcdLineParser.IntRange intRange,
            String string,
            UnicodeProperty nextVersion) {
        put(data, intRange, string, null, nextVersion);
    }

    public void put(
            UnicodeMap<String> data,
            UcdLineParser.IntRange intRange,
            String string,
            Merge<String> merger,
            UnicodeProperty nextVersion) {
        put(data, null, intRange, string, merger, false, nextVersion);
    }

    public void put(
            UnicodeMap<String> data,
            UnicodeSet missingSet,
            UcdLineParser.IntRange intRange,
            String value,
            Merge<String> merger,
            boolean hackHangul,
            UnicodeProperty nextVersion) {
        if (value == null && property == UcdProperty.Idn_2008) {
            // The IDNA2008 Status field of the IDNA mapping table is treated as an enumerated
            // property by the tools, with an Extra @missing line with a value na.
            // Unusually, the file has data lines with no IDNA2008 Status field; the default should
            // apply to these ranges.
            return;
        }
        if (value != null
                && value.isEmpty()
                && property != UcdProperty.NFKC_Casefold
                && property != UcdProperty.NFKC_Simple_Casefold
                && property != UcdProperty.Jamo_Short_Name) {
            // TODO(egg): We probably should do this only exceptionally for UnicodeData.txt,
            // instead of by default for all but the few properties above.
            value = null;
        }
        value = normalizeAndVerify(value);
        if (intRange.string != null) {
            PropertyUtilities.putNew(data, intRange.string, value, merger);
        } else {
            for (int codepoint = intRange.start; codepoint <= intRange.end; ++codepoint) {
                String nextValue = null;
                if (nextVersion != null) {
                    nextValue = nextVersion.getValue(codepoint);
                }
                String insertedValue;
                try {
                    if (hackHangul) {
                        // Use ICU for Hangul decomposition.
                        String fullDecomp = NFD.getDecomposition(codepoint);
                        if (fullDecomp.length() > 2) {
                            fullDecomp =
                                    NFC.normalize(fullDecomp.substring(0, 2))
                                            + fullDecomp.substring(2);
                        }
                        insertedValue = fullDecomp;
                    } else if (value == CONSTRUCTED_NAME) {
                        // Use ICU for Hangul Name construction, constant.
                        insertedValue = UCharacter.getName(codepoint);
                    } else {
                        insertedValue = value;
                    }
                    if (nextVersion != null && Objects.equals(insertedValue, nextValue)) {
                        insertedValue = IndexUnicodeProperties.UNCHANGED_IN_BASE_VERSION;
                    }
                    PropertyUtilities.putNew(data, missingSet, codepoint, insertedValue, merger);
                } catch (final Exception e) {
                    String msg =
                            String.format(
                                    "%s: %04X..%04X  %s",
                                    property, intRange.start, intRange.end, value);
                    throw new UnicodePropertyException(msg, e);
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
                if (property == UcdProperty.Script_Extensions) {
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
            final PropertyParsingInfo propInfo =
                    property == UcdProperty.Script_Extensions
                            ? getPropertyInfo(UcdProperty.Script)
                            : this;
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
            throw new UnicodePropertyException(
                    "\tBad enum value for " + property + " :\t" + string);
        } else {
            string = item.toString();
        }
        return string;
    }

    public void checkRegex(String part) {
        if (!getRegex().matcher(part).matches()) {
            final String part2 = NFD.normalize(part);
            if (!getRegex().matcher(part2).matches()) {
                IndexUnicodeProperties.getDataLoadingErrors()
                        .put(
                                property,
                                "Regex failure: " + RegexUtilities.showMismatch(getRegex(), part));
            }
        }
    }

    public String getDefaultValue(VersionInfo version) {
        for (final var entry : defaultValues.entrySet()) {
            if (version.compareTo(entry.getKey()) <= 0) {
                return entry.getValue();
            }
        }
        // TODO(egg): Add plenty of @missing lines with <none> to ExtraPropertyValueAliases and make
        // this an exception.
        return null;
    }

    public ValueCardinality getMultivalued() {
        return multivalued;
    }

    public Pattern getRegex() {
        return regex;
    }

    enum FileType {
        Field,
        HackField,
        PropertyValue,
        List,
        CJKRadicals,
        NamedSequences,
        NameAliases,
        StandardizedVariants,
        Confusables,
    }

    static Map<String, FileType> file2Type = new HashMap<String, FileType>();

    static Map<String, ValueCardinality> toMultiValued = new HashMap<>();

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
            multivaluedSplit = NO_SPLIT;
        }
        if (multivalued2.endsWith("_COMMA")) {
            multivaluedSplit = COMMA;
            multivalued = ValueCardinality.Unordered;
            return;
        }
        multivalued = toMultiValued.get(multivalued2);
    }

    static void parseSourceFile(
            IndexUnicodeProperties indexUnicodeProperties,
            IndexUnicodeProperties nextProperties,
            final String fullFilename,
            final String fileName) {
        FileType fileType = file2Type.get(fileName);
        if (fileType == null) {
            fileType = FileType.Field;
        }

        final Set<PropertyParsingInfo> propInfoSetRaw = file2PropertyInfoSet.get(fileName);
        final Set<PropertyParsingInfo> propInfoSet = new LinkedHashSet<>();
        for (final PropertyParsingInfo propInfo : propInfoSetRaw) {
            // the propInfoSet has all the properties, even those that are not in this version of
            // the file
            if (!fileName.equals(propInfo.getFileName(indexUnicodeProperties.ucdVersion))) {
                continue;
            }
            propInfoSet.add(propInfo);
        }

        for (final PropertyParsingInfo propInfo : propInfoSet) {
            PropertyUtilities.putNew(
                    indexUnicodeProperties.property2UnicodeMap,
                    propInfo.property,
                    new UnicodeMap<String>());
        }

        // if a file is not in a given version of Unicode, we skip it.
        if (fullFilename == null) {
            if (IndexUnicodeProperties.SHOW_LOADED) {
                System.out.println(
                        "Version\t"
                                + indexUnicodeProperties.getUcdVersion()
                                + "\tFile doesn't exist: "
                                + fileName);
            }
        } else {
            indexUnicodeProperties.getFileNames().add(fullFilename);
            UcdLineParser parser = new UcdLineParser(FileUtilities.in("", fullFilename));
            if (fileName.startsWith("Unihan")
                    || fileName.startsWith("Unikemet")
                    || (fileName.endsWith("Sources") && !fileName.startsWith("Emoji"))
                    || fileName.startsWith("k")) {
                parser.withTabs(true);
            }
            PropertyParsingInfo propInfo;

            switch (fileType) {
                case CJKRadicals:
                    propInfo = property2PropertyInfo.get(UcdProperty.CJK_Radical);
                    parseCJKRadicalsFile(
                            parser.withRange(false),
                            propInfo,
                            indexUnicodeProperties.property2UnicodeMap.get(propInfo.property),
                            nextProperties == null
                                    ? null
                                    : nextProperties.getProperty(propInfo.property));
                    break;
                case NamedSequences:
                    parseNamedSequencesFile(
                            parser.withRange(false),
                            indexUnicodeProperties,
                            nextProperties,
                            propInfoSet);
                    break;
                case PropertyValue:
                    if (fileName.equals("PropList")
                            && indexUnicodeProperties.ucdVersion.compareTo(
                                            VersionInfo.UNICODE_3_1_0)
                                    < 0) {
                        parsePropertyDumpFile(fullFilename, indexUnicodeProperties, nextProperties);
                    } else {
                        parsePropertyValueFile(
                                parser.withMissing(true),
                                fileName,
                                indexUnicodeProperties,
                                nextProperties);
                    }
                    break;
                case Confusables:
                    parseConfusablesFile(
                            parser, indexUnicodeProperties, nextProperties, propInfoSet);
                    break;
                case StandardizedVariants:
                    parseStandardizedVariantsFile(
                            parser, indexUnicodeProperties, nextProperties, propInfoSet);
                    break;
                case NameAliases:
                    parseNameAliasesFile(
                            parser, indexUnicodeProperties, nextProperties, propInfoSet);
                    break;
                case HackField:
                    parseUnicodeDataFile(
                            parser, indexUnicodeProperties, nextProperties, propInfoSet);
                    break;
                case Field:
                    if (propInfoSet.size() == 1
                            && (propInfo = propInfoSet.iterator().next()).special
                                    == SpecialProperty.None
                            && propInfo.getFieldNumber(indexUnicodeProperties.ucdVersion) == 1) {
                        parseSimpleFieldFile(
                                parser.withMissing(true),
                                propInfo,
                                indexUnicodeProperties,
                                nextProperties == null
                                        ? null
                                        : nextProperties.getProperty(propInfo.property));
                    } else {
                        parseFieldFile(
                                parser.withMissing(true),
                                indexUnicodeProperties,
                                nextProperties,
                                propInfoSet);
                    }
                    break;
                case List:
                    if (propInfoSet.size() == 1) {
                        propInfo = propInfoSet.iterator().next();
                        assert propInfo.property.getType() == PropertyType.Binary;
                        parseListFile(
                                parser,
                                propInfo,
                                indexUnicodeProperties.property2UnicodeMap.get(propInfo.property),
                                nextProperties == null
                                        ? null
                                        : nextProperties.getProperty(propInfo.property));
                    } else {
                        throw new UnicodePropertyException(
                                "List files must have only one property, and must be Boolean");
                    }
                    break;
                default:
                    throw new UnicodePropertyException();
            }

            //            if
            // (property2UnicodeMap.get(UcdProperty.Script).values().contains("Hatran")) {
            //                int x = 1;
            //            }
            if (IndexUnicodeProperties.SHOW_LOADED) {
                System.out.println(
                        "Version\t"
                                + indexUnicodeProperties.getUcdVersion()
                                + "\tLoaded: "
                                + fileName
                                + "\tlines: "
                                + parser.getLineCount()
                                + (parser.containsEOF() ? "" : "\t*NO '# EOF'"));
            }
        }
        for (final PropertyParsingInfo propInfo : propInfoSet) {
            if (propInfo.property == IndexUnicodeProperties.CHECK_PROPERTY) {
                int debug = 0;
            }
            final UnicodeMap<String> data =
                    indexUnicodeProperties.property2UnicodeMap.get(propInfo.property);
            final UnicodeSet nullValues = data.getSet(null);
            //            if (propInfo.defaultValue == null) {
            //                if (CHECK_MISSING != null) {
            //                    System.out.println("** Clearing null dv in " + propInfo.property);
            //                }
            //                propInfo.defaultValue = "<none>";
            //            }
            switch (propInfo.defaultValueType) {
                    // TODO(egg): Consider also storing only the changed values here.
                case Script:
                case Simple_Lowercase_Mapping:
                case Simple_Titlecase_Mapping:
                case Simple_Uppercase_Mapping:
                    final var otherMap =
                            indexUnicodeProperties.load(propInfo.defaultValueType.property);
                    final UnicodeProperty otherProperty =
                            indexUnicodeProperties.getProperty(propInfo.defaultValueType.property);
                    final UnicodeProperty baseVersionOfThisProperty =
                            indexUnicodeProperties.baseVersionProperties != null
                                    ? indexUnicodeProperties.baseVersionProperties.getProperty(
                                            propInfo.property)
                                    : null;
                    for (final int cp : nullValues.codePoints()) {
                        // We cannot simply use the raw map otherMap for otherProperty, as it may
                        // use the UNCHANGED_IN_BASE_VERSION placeholder.
                        // If property X is defaulting to property Y, and property Y has the same
                        // assignment as its next version Y′, that does not mean that X has the same
                        // assignment as its next version X′.  If that happens though, we should use
                        // UNCHANGED_IN_BASE_VERSION.
                        if (otherMap.get(cp)
                                .equals(IndexUnicodeProperties.UNCHANGED_IN_BASE_VERSION)) {
                            if (Objects.equals(
                                    otherProperty.getValue(cp),
                                    baseVersionOfThisProperty.getValue(cp))) {
                                data.put(cp, IndexUnicodeProperties.UNCHANGED_IN_BASE_VERSION);
                            } else {
                                data.put(cp, otherProperty.getValue(cp));
                            }
                        } else {
                            data.put(cp, otherMap.getValue(cp));
                        }
                    }
                    // propInfo.defaultValueType =
                    // property2PropertyInfo.get(sourceProp).defaultValueType; // reset to the type
                    break;
                case LITERAL:
                    data.putAll(
                            nullValues,
                            propInfo.getDefaultValue(indexUnicodeProperties.ucdVersion));
                    break;
                case NONE:
                    // data.putAll(nullValues, propInfo.defaultValue);
                    // do nothing, already none;
                    break;
                case CODE_POINT:
                    // NOTE(egg): The naïve thing here would be
                    //   for (final String cp : nullValues) {
                    //     data.put(cp, cp);
                    //   }
                    // However, UnicodeMap is extremely slow with large numbers of values.
                    // Instead we fill it with <code point>, and let IndexUnicodeProperty resolve
                    // that.
                    data.putAll(
                            nullValues,
                            propInfo.getDefaultValue(indexUnicodeProperties.ucdVersion));
                    break;
                default:
                    throw new UnicodePropertyException(); // unexpected error
            }
            data.freeze();
            if (IndexUnicodeProperties.FILE_CACHE) {
                indexUnicodeProperties.internalStoreCachedMap(
                        Settings.Output.BIN_DIR, propInfo.property, data);
            }
        }
    }

    private static void parseCJKRadicalsFile(
            UcdLineParser parser,
            PropertyParsingInfo propInfo,
            UnicodeMap<String> data,
            UnicodeProperty nextVersion) {
        // Note: CJKRadicals.txt cannot be completely represented via a UnicodeMap.
        // See the comments in RadicalStroke.getCJKRadicals().
        /*
         * CJKRadicals
         * The first field is the radical number.
         * The second field is the CJK Radical character.
         * The third field is the CJK Unified Ideograph.
         * 1; 2F00; 4E00
         */
        for (UcdLineParser.UcdLine line : parser) {
            UcdLineParser.IntRange intRange = line.getRange();
            String[] parts = line.getParts();
            if (!parts[1].isEmpty()) {
                intRange.set(parts[1]);
                propInfo.put(data, intRange, parts[0], nextVersion);
            }
            intRange.set(parts[2]);
            propInfo.put(data, intRange, parts[0], nextVersion);
        }
    }

    private static void parseNamedSequencesFile(
            UcdLineParser parser,
            IndexUnicodeProperties indexUnicodeProperties,
            IndexUnicodeProperties nextProperties,
            Set<PropertyParsingInfo> propInfoSet) {
        for (UcdLineParser.UcdLine line : parser) {
            line.getRange().set(line.getParts()[1]);
            for (final PropertyParsingInfo propInfo : propInfoSet) {
                final UnicodeMap<String> data =
                        indexUnicodeProperties.property2UnicodeMap.get(propInfo.property);
                propInfo.put(
                        data,
                        line.getRange(),
                        line.getParts()[0],
                        nextProperties == null
                                ? null
                                : nextProperties.getProperty(propInfo.property));
            }
        }
    }

    private static void parsePropertyDumpFile(
            String fullFilename,
            IndexUnicodeProperties indexUnicodeProperties,
            IndexUnicodeProperties nextProperties) {
        final var dumpHeading = Pattern.compile("Property dump for: 0x[0-9A-F]{8} \\(([^()]+)\\)");
        final var dataLine =
                Pattern.compile("[0-9A-F]{4,6}(\\.\\.[0-9A-F]{4,6} +\\(\\d+ chars\\))?");
        PropertyParsingInfo propInfo = null;
        for (String line : FileUtilities.in("", fullFilename)) {
            final var heading = dumpHeading.matcher(line);
            if (heading.matches()) {
                String name = heading.group(1);
                propInfo = property2PropertyInfo.get(UcdProperty.forString(name));
                if (propInfo == null) {
                    if (name.equals("Not a Character")) {
                        // Appears in 3.0.1.  See also 84-M6 and 84-M7.
                        propInfo = property2PropertyInfo.get(UcdProperty.Noncharacter_Code_Point);
                    } else {
                        System.err.println("Ignoring unknown property in dump: " + name);
                    }
                }
                continue;
            }
            if (line.trim().equals("Discrepancy B: UnicodeData but not Unilib")) {
                // Unicode 2.1.8 includes a diff between Unilib properties used in 2.1.5 and 2.1.9
                // and something resembling the derivations that would later be used in Unicode 3.1,
                // but without the Other_* exceptions introduced in Unicode 3.1.
                // We follow Unilib for continuity.
                propInfo = null;
            }
            if (propInfo != null && dataLine.matcher(line).matches()) {
                var range = new UcdLineParser.IntRange();
                range.set(line.split(" ", 2)[0]);
                final var data = indexUnicodeProperties.property2UnicodeMap.get(propInfo.property);
                propInfo.put(
                        data,
                        range,
                        "Yes",
                        null,
                        nextProperties == null
                                ? null
                                : nextProperties.getProperty(propInfo.property));
            }
        }
    }

    private static void parsePropertyValueFile(
            UcdLineParser parser,
            String filename,
            IndexUnicodeProperties indexUnicodeProperties,
            IndexUnicodeProperties nextProperties) {
        for (UcdLineParser.UcdLine line : parser) {
            if (line.getOriginalLine().equals("U+")
                    && filename.startsWith("Unihan")
                    && indexUnicodeProperties.ucdVersion.getMajor() == 2) {
                // Truncated Unihan-1.txt in Unicode 2.0.
                return;
            }
            String propName = line.getParts()[1];
            UcdProperty item = UcdProperty.forString(propName);

            String extractedValue = null;
            if (item == null
                    && indexUnicodeProperties.ucdVersion.compareTo(VersionInfo.UNICODE_4_0) <= 0) {
                // DerivedNormalizationProps Version 4.0 and earlier is highly irregular.
                // It provides the NFMeow_QC assignments as a single field NFMEOW_Value,
                // and calls FC_NFKC_Closure FNC.  Since we need special handling for the former,
                // we also deal with FNC here instead of making it an Extra alias.
                if (propName.equals("FNC")) {
                    propName = "FC_NFKC_Closure";
                    item = UcdProperty.forString(propName);
                } else {
                    String[] parts = propName.split("_");
                    if (parts.length == 2
                            && (parts[1].equals("NO") | parts[1].equals("MAYBE"))
                            && (parts[0].startsWith("NF"))) {
                        propName = parts[0] + "_QC";
                        item = UcdProperty.forString(propName);
                        extractedValue = parts[1];
                    }
                }
            }

            if (item == null
                    && indexUnicodeProperties.ucdVersion == VersionInfo.UNICODE_3_1_1
                    && propName.equals("297")) {
                // Missing field 1 for in the record for U+64AC kPhonetic in Unihan 3.1.1.
                // See UAX #38:
                //   The Version 3.1.1 Unihan database file, Unihan-3.1.1.txt, includes the
                //   following anomalous record at line 246,442: U+64AC 297.
                extractedValue = propName;
                propName = "kPhonetic";
                item = UcdProperty.forString(propName);
            }

            if (item == null) {
                throw new IllegalArgumentException(
                        "Missing property enum in UcdProperty for "
                                + propName
                                + "\nSee "
                                + NEW_UNICODE_PROPS_DOCS
                                + ". At:"
                                + line.getOriginalLine());
            }

            PropertyParsingInfo propInfo;
            try {
                propInfo = property2PropertyInfo.get(item);
            } catch (Exception e) {
                throw new IllegalArgumentException(line.getOriginalLine(), e);
            }
            if (propInfo == null) {
                throw new IllegalArgumentException(
                        "need to define PropertyParsingInfo for " + item);
            }
            //                    if (!propInfoSet.contains(propInfo)) {
            //                        throw new UnicodePropertyException("Property not listed for
            // file: " + propInfo);
            //                    }
            String value;
            // The file emoji-sequences.txt has a comment-like field after the binary property.
            if (extractedValue == null
                    && (line.getParts().length == 2
                            || filename.equals("emoji/*/emoji-sequences")
                            || filename.equals("emoji/*/emoji-zwj-sequences"))) {
                if (propInfo.property.getType() != PropertyType.Binary) {
                    throw new IllegalArgumentException(
                            "Expected a value for "
                                    + propInfo.property.getType()
                                    + " property "
                                    + propName);
                }
                value = "Yes";
            } else {
                value = extractedValue != null ? extractedValue : line.getParts()[2];
                if (propInfo.property == UcdProperty.kJapaneseOn
                        && indexUnicodeProperties.ucdVersion.equals(VersionInfo.UNICODE_3_1_0)
                        && value.isEmpty()
                        && line.getParts().length == 4) {
                    // Extra tab in the kJapaneseOn record for U+4E00.
                    value = line.getParts()[3];
                }
                if (propInfo.property.getType() == PropertyType.Binary) {
                    if (line.getType() == Contents.DATA
                            && UcdPropertyValues.Binary.forName(value)
                                    != UcdPropertyValues.Binary.Yes) {
                        // Most binary properties have no value field, but at least kEH_NoRotate has
                        // Y.
                        throw new IllegalArgumentException(
                                "Unexpected value "
                                        + value
                                        + " for binary property "
                                        + propName
                                        + " in "
                                        + filename);
                    } else if (line.getType() == Contents.MISSING
                            && UcdPropertyValues.Binary.forName(value)
                                    != UcdPropertyValues.Binary.No) {
                        throw new IllegalArgumentException(
                                "Unexpected default "
                                        + value
                                        + " for binary property "
                                        + propName
                                        + " in "
                                        + filename);
                    }
                }
                // The value should not be an empty string.
                // Exception: NFKC_Casefold does remove some characters by mapping them to nothing.
                if (value.isEmpty()
                        && !(propInfo.property == UcdProperty.NFKC_Casefold
                                || propInfo.property == UcdProperty.NFKC_Simple_Casefold)) {
                    throw new IllegalArgumentException(
                            "Unexpected empty value for property "
                                    + propName
                                    + ":  "
                                    + line.getOriginalLine());
                }
                if (propInfo.property == UcdProperty.kMandarin) {
                    if (indexUnicodeProperties.oldVersion) {
                        value =
                                IndexUnicodeProperties.fromNumericPinyin.transform(
                                        value.toLowerCase(Locale.ENGLISH));
                    }
                }
            }
            if (line.getType() == UcdLineParser.UcdLine.Contents.DATA) {
                if (propInfo.getDefaultValue(indexUnicodeProperties.ucdVersion) == null) {
                    // Old versions of data files did not yet have @missing lines.
                    // Supply the default value before applying the first real data line.
                    String defaultValue = null;
                    switch (propInfo.property) {
                        case NFKC_Casefold:
                        case NFKC_Simple_Casefold:
                            defaultValue = "<code point>";
                            break;
                        default:
                            break;
                    }
                    if (defaultValue != null) {
                        setPropDefault(
                                propInfo.property,
                                defaultValue,
                                "hardcoded",
                                false,
                                indexUnicodeProperties.ucdVersion);
                    }
                }
                final UnicodeMap<String> data;
                try {
                    data = indexUnicodeProperties.property2UnicodeMap.get(propInfo.property);
                } catch (Exception e) {
                    throw new IllegalArgumentException(line.getOriginalLine(), e);
                }
                if (data == null) {
                    throw new IllegalArgumentException(
                            "No map for property "
                                    + propInfo.property
                                    + " when parsing file "
                                    + filename
                                    + "; property expected in "
                                    + propInfo.getFileName(indexUnicodeProperties.ucdVersion)
                                    + " in "
                                    + indexUnicodeProperties.ucdVersion);
                }
                if (data.isFrozen()) {
                    throw new IllegalArgumentException(
                            "Found record for frozen property "
                                    + propInfo.property
                                    + " when parsing file "
                                    + filename
                                    + "; property expected in "
                                    + propInfo.getFileName(indexUnicodeProperties.ucdVersion)
                                    + " in "
                                    + indexUnicodeProperties.ucdVersion);
                }
                // Unihan 4.0 and earlier implemented multivalued properties by repeating the
                // property value record instead of using a delimiter.
                var merger =
                        propInfo.property.getShortName().startsWith("cjk")
                                        && indexUnicodeProperties.ucdVersion.compareTo(
                                                        VersionInfo.UNICODE_4_0)
                                                <= 0
                                ? IndexUnicodeProperties.MULTIVALUED_JOINER
                                : null;
                final var originalMultivaluedSplit = propInfo.multivaluedSplit;
                // The first version of kPrimaryNumeric had spaces in values.
                if (propName.equals("kPrimaryNumeric")
                        && indexUnicodeProperties.ucdVersion.compareTo(VersionInfo.UNICODE_4_0)
                                <= 0) {
                    propInfo.multivaluedSplit = NO_SPLIT;
                }
                propInfo.put(
                        data,
                        line.getRange(),
                        value,
                        merger,
                        nextProperties == null
                                ? null
                                : nextProperties.getProperty(propInfo.property));
                propInfo.multivaluedSplit = originalMultivaluedSplit;
            } else {
                setPropDefault(
                        propInfo.property,
                        value,
                        line.getOriginalLine(),
                        line.getType() == UcdLineParser.UcdLine.Contents.EMPTY,
                        indexUnicodeProperties.ucdVersion);
            }
        }
    }

    private static void parseConfusablesFile(
            UcdLineParser parser,
            IndexUnicodeProperties indexUnicodeProperties,
            IndexUnicodeProperties nextProperties,
            Set<PropertyParsingInfo> propInfoSet) {
        for (UcdLineParser.UcdLine line : parser) {
            UcdLineParser.IntRange intRange = line.getRange();
            String[] parts = line.getParts();
            String propName = "Confusable_" + parts[2];
            final UcdProperty ucdProp = UcdProperty.forString(propName);
            if (ucdProp == null) {
                throw new IllegalArgumentException(
                        "Missing property enum in UcdProperty for "
                                + propName
                                + "\nSee "
                                + NEW_UNICODE_PROPS_DOCS);
            }
            final PropertyParsingInfo propInfo = property2PropertyInfo.get(ucdProp);
            if (!propInfoSet.contains(propInfo)) {
                throw new UnicodePropertyException("Property not listed for file: " + propInfo);
            }
            final UnicodeMap<String> data =
                    indexUnicodeProperties.property2UnicodeMap.get(propInfo.property);
            propInfo.put(
                    data,
                    intRange,
                    parts[1],
                    nextProperties == null ? null : nextProperties.getProperty(propInfo.property));
        }
    }

    private static void parseStandardizedVariantsFile(
            UcdLineParser parser,
            IndexUnicodeProperties indexUnicodeProperties,
            IndexUnicodeProperties nextProperties,
            Set<PropertyParsingInfo> propInfoSet) {
        for (UcdLineParser.UcdLine line : parser) {
            String[] parts = line.getParts();
            if (!parts[2].isEmpty()) {
                parts[1] = parts[1] + " (" + parts[2] + ")";
            }
            parseFields(
                    line,
                    indexUnicodeProperties,
                    nextProperties,
                    propInfoSet,
                    IndexUnicodeProperties.ALPHABETIC_JOINER,
                    false);
        }
    }

    private static void parseNameAliasesFile(
            UcdLineParser parser,
            IndexUnicodeProperties indexUnicodeProperties,
            IndexUnicodeProperties nextProperties,
            Set<PropertyParsingInfo> propInfoSet) {
        for (UcdLineParser.UcdLine line : parser) {
            parseFields(
                    line,
                    indexUnicodeProperties,
                    nextProperties,
                    propInfoSet,
                    IndexUnicodeProperties.MULTIVALUED_JOINER,
                    false);
        }
    }

    static final Set<Integer> BROKEN_UNICODEDATA_LINES_IN_2_1_5 =
            Set.of(
                    0xFA0E, 0xFA0F, 0xFA11, 0xFA13, 0xFA14, 0xFA1F, 0xFA21, 0xFA23, 0xFA24, 0xFA27,
                    0xFA28, 0xFA29);

    private static void parseUnicodeDataFile(
            UcdLineParser parser,
            IndexUnicodeProperties indexUnicodeProperties,
            IndexUnicodeProperties nextProperties,
            Set<PropertyParsingInfo> propInfoSet) {
        int lastCodepoint = 0;
        for (UcdLineParser.UcdLine line : parser) {
            String[] parts = line.getParts();
            boolean hackHangul = false;
            if (parts[1].endsWith("Last>")) {
                line.getRange().start = lastCodepoint + 1;
            }
            if (parts[1].startsWith("<")) {
                /*
                 * 4DBF;<CJK Ideograph Extension A, Last>;Lo;0;L;;;;;N;;;;;
                 * 9FFC;<CJK Ideograph, Last>;Lo;0;L;;;;;N;;;;;
                 * D7A3;<Hangul Syllable, Last>;Lo;0;L;;;;;N;;;;;
                 * DB7F;<Non Private Use High Surrogate, Last>;Cs;0;L;;;;;N;;;;;
                 * DBFF;<Private Use High Surrogate, Last>;Cs;0;L;;;;;N;;;;;
                 * DFFF;<Low Surrogate, Last>;Cs;0;L;;;;;N;;;;;
                 * F8FF;<Private Use, Last>;Co;0;L;;;;;N;;;;;
                 * 187F7;<Tangut Ideograph, Last>;Lo;0;L;;;;;N;;;;;
                 * 18D08;<Tangut Ideograph Supplement, Last>;Lo;0;L;;;;;N;;;;;
                 * 2A6DD;<CJK Ideograph Extension B, Last>;Lo;0;L;;;;;N;;;;;
                 * 2B734;<CJK Ideograph Extension C, Last>;Lo;0;L;;;;;N;;;;;
                 * 2B81D;<CJK Ideograph Extension D, Last>;Lo;0;L;;;;;N;;;;;
                 * 2CEA1;<CJK Ideograph Extension E, Last>;Lo;0;L;;;;;N;;;;;
                 * 2EBE0;<CJK Ideograph Extension F, Last>;Lo;0;L;;;;;N;;;;;
                 * 3134A;<CJK Ideograph Extension G, Last>;Lo;0;L;;;;;N;;;;;
                 * FFFFD;<Plane 15 Private Use, Last>;Co;0;L;;;;;N;;;;;
                 * 10FFFD;<Plane 16 Private Use, Last>;Co;0;L;;;;;N;;;;;
                 */
                if (parts[1].contains("CJK Ideograph")) {
                    parts[1] = "CJK UNIFIED IDEOGRAPH-#";
                } else if (parts[1].contains("Tangut Ideograph")) {
                    parts[1] = "TANGUT IDEOGRAPH-#";
                } else if (parts[1].contains("Hangul Syllable")) {
                    parts[1] = CONSTRUCTED_NAME;
                    hackHangul = true;
                } else if (parts[1].contains("CJK Compatibility Ideograph")) {
                    // Unicode 2.0 through 2.1.2 have
                    // F900;<CJK Compatibility Ideograph, First>;Lo;0;L;;;;;N;;;;;
                    // FA2D;<CJK Compatibility Ideograph, Last>;Lo;0;L;;;;;N;;;;;
                    // and this is replicated in the reconstructed 1.0.0 and 1.0.1 files.
                    parts[1] = "CJK COMPATIBILITY IDEOGRAPH-#";
                } else if (parts[1].equals("<CJK IDEOGRAPH REPRESENTATIVE>")) {
                    // UnicodeData-1.1.5.txt does not have ranges yet, instead it has a
                    // representative that is meant to apply to ranges defined elsewhere.
                    // We inject these ranges here.
                    parts[1] = "CJK UNIFIED IDEOGRAPH-#";
                    // Start is already at 0x4E00, the representative.
                    line.getRange().end = 0x9FA5;
                    parseFields(
                            line,
                            indexUnicodeProperties,
                            nextProperties,
                            propInfoSet,
                            null,
                            hackHangul);
                    line.getRange().start = 0xF900;
                    line.getRange().end = 0xFA2D;
                    parts[1] = "CJK COMPATIBILITY IDEOGRAPH-#";
                    parseFields(
                            line,
                            indexUnicodeProperties,
                            nextProperties,
                            propInfoSet,
                            null,
                            hackHangul);
                    // UnicodeData-1.1.5.txt is also missing the PUA, which was defined only in
                    // wording.  Inject it here while we are doing surgery on the surrounding CJK
                    // blocks.  Note that the PUA has its modern E000..F8FF in 1.1, see
                    // https://www.unicode.org/versions/Unicode1.1.0/ch02.pdf.
                    line.getRange().start = 0xE000;
                    line.getRange().end = 0xF8FF;
                    parts[1] = null;
                    parts[2] = "Co";
                    parseFields(
                            line,
                            indexUnicodeProperties,
                            nextProperties,
                            propInfoSet,
                            null,
                            hackHangul);
                } else {
                    parts[1] = null;
                }
            } else if (parts[1].startsWith("CJK COMPATIBILITY IDEOGRAPH-")) {
                parts[1] = "CJK COMPATIBILITY IDEOGRAPH-#"; // hack for uniform data
            }
            lastCodepoint = line.getRange().end;
            if (!parts[5].isEmpty() && parts[5].indexOf('<') >= 0) {
                // Decomposition_Mapping: Remove the decomposition type.
                parts[5] = DECOMP_REMOVE.matcher(parts[5]).replaceAll("").trim();
            }
            if (indexUnicodeProperties.ucdVersion == VersionInfo.UNICODE_2_1_5
                    && BROKEN_UNICODEDATA_LINES_IN_2_1_5.contains(line.getRange().start)) {
                // These lines have the form
                //   FA0E;CJK COMPATIBILITY IDEOGRAPH-FA0E;Lo;0;L;;;;N;;;;;;
                // Contrast 2.1.8
                //   FA0E;CJK COMPATIBILITY IDEOGRAPH-FA0E;Lo;0;L;;;;;N;;;;;
                parts[9] = parts[8];
                parts[8] = "";
            }
            parseFields(
                    line, indexUnicodeProperties, nextProperties, propInfoSet, null, hackHangul);
        }
    }

    private static void parseFieldFile(
            UcdLineParser parser,
            IndexUnicodeProperties indexUnicodeProperties,
            IndexUnicodeProperties nextProperties,
            Set<PropertyParsingInfo> propInfoSet) {
        for (UcdLineParser.UcdLine line : parser) {
            parseFields(line, indexUnicodeProperties, nextProperties, propInfoSet, null, false);
        }
    }

    private static void parseFields(
            UcdLineParser.UcdLine line,
            IndexUnicodeProperties indexUnicodeProperties,
            IndexUnicodeProperties nextProperties,
            Set<PropertyParsingInfo> propInfoSet,
            Merge<String> merger,
            boolean hackHangul) {
        String[] parts = line.getParts();
        if (line.getType() == UcdLineParser.UcdLine.Contents.DATA) {
            for (final PropertyParsingInfo propInfo : propInfoSet) {
                final UnicodeMap<String> data =
                        indexUnicodeProperties.property2UnicodeMap.get(propInfo.property);
                switch (propInfo.special) {
                    case None:
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
                    default:
                        throw new UnicodePropertyException();
                }
                String value =
                        propInfo.getFieldNumber(indexUnicodeProperties.ucdVersion) >= parts.length
                                ? null
                                : parts[propInfo.getFieldNumber(indexUnicodeProperties.ucdVersion)];
                if (propInfo.property == UcdProperty.Joining_Group
                        && indexUnicodeProperties.ucdVersion.compareTo(VersionInfo.UNICODE_4_0_1)
                                <= 0
                        && value.equals("<no shaping>")) {
                    value = "No_Joining_Group";
                }
                if (merger == null
                        && propInfo.property == UcdProperty.Uppercase_Mapping
                        && indexUnicodeProperties.ucdVersion == VersionInfo.UNICODE_2_1_8
                        && line.getRange().start == 0x1F80
                        && line.getRange().end == 0x1F80) {
                    // The first version of SpecialCasing.txt, version 2.1.8 has *three* lines for
                    // U+1F80:
                    // 1F80; 1F80; 1F88; 1F00 03B9; # GREEK SMALL LETTER ALPHA WITH PSILI AND
                    // YPOGEGRAMMENI
                    // 1F80; 1F80; 1F88; 1F08 03B9; # GREEK SMALL LETTER ALPHA WITH PSILI AND
                    // YPOGEGRAMMENI
                    // 1F80; 1F80; 1F88; 1F08 03B9; # GREEK CAPITAL LETTER ALPHA WITH PSILI AND
                    // PROSGEGRAMMENI
                    // We let the last one win, as it is less incorrect than the first; in 2.1.9,
                    // the line for U+1F80 is:
                    // 1F80; 1F80; 1F88; 1F08 0399; # GREEK SMALL LETTER ALPHA WITH PSILI AND
                    // YPOGEGRAMMENI
                    merger = new PropertyUtilities.Overrider();
                }
                propInfo.put(
                        data,
                        line.getMissingSet(),
                        line.getRange(),
                        value,
                        merger,
                        hackHangul && propInfo.property == UcdProperty.Decomposition_Mapping,
                        nextProperties == null
                                ? null
                                : nextProperties.getProperty(propInfo.property));
            }
        } else {
            for (final PropertyParsingInfo propInfo : propInfoSet) {
                final String value =
                        propInfo.getFieldNumber(indexUnicodeProperties.ucdVersion) < parts.length
                                ? parts[propInfo.getFieldNumber(indexUnicodeProperties.ucdVersion)]
                                : null;
                setPropDefault(
                        propInfo.property,
                        value,
                        line.getOriginalLine(),
                        line.getType() == UcdLineParser.UcdLine.Contents.EMPTY,
                        indexUnicodeProperties.ucdVersion);
            }
        }
    }

    private static void parseSimpleFieldFile(
            UcdLineParser parser,
            PropertyParsingInfo propInfo,
            IndexUnicodeProperties indexUnicodeProperties,
            UnicodeProperty nextVersion) {
        final UnicodeMap<String> data =
                indexUnicodeProperties.property2UnicodeMap.get(propInfo.property);
        final VersionInfo version = indexUnicodeProperties.ucdVersion;
        for (UcdLineParser.UcdLine line : parser) {
            if (line.getType() == UcdLineParser.UcdLine.Contents.DATA) {
                if (propInfo.getDefaultValue(version) == null) {
                    // Old versions of data files did not yet have @missing lines.
                    // Supply the default value before applying the first real data line.
                    String defaultValue = null;
                    switch (propInfo.property) {
                        case Bidi_Mirroring_Glyph:
                            defaultValue = "<none>";
                            break;
                        case Equivalent_Unified_Ideograph:
                            defaultValue = "<none>";
                            break;
                        case Script_Extensions:
                            defaultValue = "<script>";
                            break;
                        default:
                            break;
                    }
                    if (defaultValue != null) {
                        setPropDefault(
                                propInfo.property, defaultValue, "hardcoded", false, version);
                    }
                }
                if (line.getParts().length == 3 && propInfo.property == UcdProperty.Block) {
                    // The old Blocks files had First; Last; Block.
                    IntRange range = new IntRange();
                    range.start = Utility.codePointFromHex(line.getParts()[0]);
                    range.end = Utility.codePointFromHex(line.getParts()[1]);
                    // Unicode 2 puts FEFF both in Arabic Presentation Forms-B and in Specials.
                    // We are not going to make Block multivalued for that, so we let the second
                    // assignment win.
                    // This fits with assignments in Unicode 2.1.4..3.1.1 where
                    // Arabic Presentation Forms-B ended on FEFE and Specials was a
                    // split Block of FEFF & FFF0..FFFD.
                    // Since Unicode 3.2, blocks were contiguous xxx0..yyyF:
                    // https://www.unicode.org/reports/tr28/tr28-3.html#database
                    // The normative blocks defined in Blocks.txt have been adjusted slightly,
                    // in accordance with Unicode Technical Committee decisions.
                    // - Every block starts and ends on a column boundary.
                    //   That is, the last digit of the first code point in the block is always 0,
                    //   and the last digit of the final code point in the block is always F.
                    // - Every block is contiguous. [...]
                    propInfo.put(
                            data,
                            line.getMissingSet(),
                            range,
                            line.getParts()[2],
                            version.getMajor() == 2 ? new PropertyUtilities.Overrider() : null,
                            false,
                            nextVersion);
                    continue;
                } else if (propInfo.property == UcdProperty.Numeric_Value) {
                    String extractedValue = line.getParts()[1];
                    for (int cp = line.getRange().start; cp <= line.getRange().end; ++cp) {
                        String unicodeDataValue =
                                indexUnicodeProperties
                                        .getProperty(UcdProperty.Non_Unihan_Numeric_Value)
                                        .getValue(cp);
                        var range = new IntRange();
                        range.start = cp;
                        range.end = cp;
                        if (unicodeDataValue == null) {
                            if (!extractedValue.endsWith(".0")) {
                                throw new IllegalArgumentException(
                                        "Non-integer numeric value extracted from Unihan for "
                                                + Utility.hex(cp)
                                                + ": "
                                                + extractedValue);
                            }
                            propInfo.put(
                                    data,
                                    line.getMissingSet(),
                                    range,
                                    extractedValue.substring(0, extractedValue.length() - 2),
                                    null,
                                    false,
                                    nextVersion);
                        } else {
                            // Prior to Unicode 5.1, DerivedNumericValues.txt is useless for getting
                            // numeric values whose denominator is not a small power of two, as it
                            // only provides field 1, which is decimal with *mystery rounding* (in
                            // particular, enough digits to disambiguate between binary32 values).
                            // It is not normative either, so we use the value from UnicodeData.
                            // We use the values from DerivedNumericValues.txt when they are
                            // extracted from Unihan, as this avoids having to reconstruct old
                            // derivations here.  In particular, Unihan numeric properties do *not*
                            // feed into the Numeric_Value until 4.0; see
                            // https://www.unicode.org/L2/L2003/03039.htm#94-C4.
                            propInfo.put(
                                    data,
                                    line.getMissingSet(),
                                    range,
                                    unicodeDataValue,
                                    null,
                                    false,
                                    nextVersion);
                        }
                    }
                    continue;
                } else if (line.getParts().length != 2
                        && version.compareTo(VersionInfo.UNICODE_3_0_1) > 0) {
                    // Unicode 3.0 and earlier had name comments as an extra field.
                    throw new IllegalArgumentException(
                            "Too many fields in " + line.getOriginalLine());
                }
                propInfo.put(
                        data,
                        line.getMissingSet(),
                        line.getRange(),
                        line.getParts()[1],
                        null,
                        false,
                        nextVersion);
            } else {
                if (propInfo.property == UcdProperty.Numeric_Value
                        && line.getParts().length == 3
                        && line.getParts()[1].isEmpty()
                        && line.getParts()[2].equals("NaN")) {
                    // 5.1..6.1 have an improper line
                    // # @missing: 0000..10FFFF; ; NaN
                    // compare 6.2 and 6.3
                    // # @missing: 0000..10FFFF; NaN; ; NaN
                    // This causes the default for field 1 (which we use as the key for
                    // Numeric_Value, with some
                    // subsequent chicanery to actually get the data from UnicodeData) to be the
                    // empty string, rather
                    // than NaN.
                    // Before 5.1, there is no @missing line. After 6.3, the @missing line is in
                    // PropertyValueAliases,
                    // where it is independent of the format of the file specifying the property.
                    line.getParts()[1] = "NaN";
                }
                setPropDefault(
                        propInfo.property,
                        line.getParts()[1],
                        line.getOriginalLine(),
                        line.getType() == UcdLineParser.UcdLine.Contents.EMPTY,
                        version);
            }
        }
    }

    private static void parseListFile(
            UcdLineParser parser,
            PropertyParsingInfo propInfo,
            UnicodeMap<String> data,
            UnicodeProperty nextVersion) {
        for (UcdLineParser.UcdLine line : parser) {
            propInfo.put(data, line.getRange(), "Yes", nextVersion);
        }
    }

    public static Pattern hackCompile(String regex) {
        if (regex.contains("\\x")) {
            final StringBuilder builder = new StringBuilder();
            final Matcher m = SLASHX.matcher(regex);
            int start = 0;
            while (m.find()) {
                builder.append(regex.substring(start, m.start()));
                final int codePoint = Integer.parseInt(m.group(1), 16);
                // System.out.println("\\x char:\t" + new
                // StringBuilder().appendCodePoint(codePoint));
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
            line = line.trim();
            if (line.startsWith("$")) {
                final String[] parts = EQUALS.split(line);
                IndexUnicodeProperties.vr.add(
                        parts[0], IndexUnicodeProperties.vr.replace(parts[1]));
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
            regex = IndexUnicodeProperties.vr.replace(regex);
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

    public static PropertyParsingInfo getPropertyInfo(UcdProperty property) {
        return property2PropertyInfo.get(property);
    }

    /**
     * @internal
     * @deprecated
     */
    @Deprecated
    public static Relation<String, PropertyParsingInfo> getFile2PropertyInfoSet() {
        return file2PropertyInfoSet;
    }

    static void setFile2PropertyInfoSet(
            Relation<String, PropertyParsingInfo> file2PropertyInfoSet) {
        PropertyParsingInfo.file2PropertyInfoSet = file2PropertyInfoSet;
    }

    public static void setPropDefault(
            UcdProperty prop, String value, String line, boolean isEmpty, VersionInfo version) {
        if (prop == IndexUnicodeProperties.CHECK_PROPERTY) {
            System.out.format("** %s %s %s %s\n", prop, value, line, isEmpty);
        }
        final PropertyParsingInfo propInfo = property2PropertyInfo.get(prop);

        if (value != null && !value.startsWith("<")) {
            value = propInfo.normalizeAndVerify(value);
        }

        if (!propInfo.defaultValues.containsKey(version)) {
            propInfo.defaultValueType = IndexUnicodeProperties.DefaultValueType.forString(value);
            propInfo.defaultValues.put(version, value);
            if (IndexUnicodeProperties.SHOW_DEFAULTS) {
                IndexUnicodeProperties.getDataLoadingErrors()
                        .put(
                                prop,
                                "**\t"
                                        + prop
                                        + "\t"
                                        + propInfo.defaultValueType
                                        + "\t"
                                        + propInfo.getDefaultValue(version));
            }
        } else if (propInfo.getDefaultValue(version).equals(value)) {
        } else {
            final String comment =
                    "\t ** ERROR Will not change default for "
                            + prop
                            + " to «"
                            + value
                            + "», retaining "
                            + propInfo.getDefaultValue(version);
            //            propInfo.defaultValueType = DefaultValueType.forString(value);
            //            propInfo.defaultValue = value;
            IndexUnicodeProperties.getDataLoadingErrors().put(prop, comment);
        }
    }

    private static void setDefaultValueForPropertyName(
            String line, String propName, String value, boolean isEmpty, VersionInfo version) {
        final UcdProperty ucdProp = UcdProperty.forString(propName);
        if (ucdProp == null) {
            throw new IllegalArgumentException(
                    propName + " not defined. " + "See " + NEW_UNICODE_PROPS_DOCS);
        }
        final PropertyParsingInfo propInfo = property2PropertyInfo.get(ucdProp);
        setPropDefault(propInfo.property, value, line, isEmpty, version);
    }

    static void init() {
        final Matcher semicolon = SEMICOLON.matcher("");
        for (final String line :
                FileUtilities.in(IndexUnicodeProperties.class, "IndexUnicodeProperties.txt")) {
            if (line.startsWith("#") || line.isEmpty()) {
                continue;
            }

            final String[] parts = Regexes.split(semicolon, line.trim());
            // file ; property name ; derivation info
            // System.out.println(Arrays.asList(parts));
            if (parts[0].equals("FileType")) {
                PropertyUtilities.putNew(file2Type, parts[1], FileType.valueOf(parts[2]));
            } else {
                fromStrings(parts);
            }
        }
        // DO THESE FIRST (overrides values in files!)
        parseMissingFromValueAliases(
                FileUtilities.in(IndexUnicodeProperties.class, "ExtraPropertyAliases.txt"));
        parseMissingFromValueAliases(
                FileUtilities.in(IndexUnicodeProperties.class, "ExtraPropertyValueAliases.txt"));

        String propValueAliases =
                Utility.getMostRecentUnicodeDataFile(
                        "PropertyValueAliases", GenerateEnums.ENUM_VERSION, true, false);
        if (propValueAliases == null) {
            throw new MissingResourceException(
                    "unable to find PropertyValueAliases.txt for version "
                            + GenerateEnums.ENUM_VERSION
                            + "; check the paths in class Settings and in your environment variables",
                    "PropertyParsingInfo",
                    "");
        }
        parseMissingFromValueAliases(FileUtilities.in("", propValueAliases));

        for (final String line :
                FileUtilities.in(IndexUnicodeProperties.class, "IndexPropertyRegex.txt")) {
            getRegexInfo(line);
        }

        //        for (String line : FileUtilities.in(IndexUnicodeProperties.class,
        // "Multivalued.txt")) {
        //            UcdProperty prop = UcdProperty.forString(line);
        //            PropertyParsingInfo propInfo = property2PropertyInfo.get(prop);
        //            propInfo.multivalued = Multivalued.MULTI_VALUED;
        //        }

        //        for (UcdProperty x : UcdProperty.values()) {
        //            if (property2PropertyInfo.containsKey(x.toString())) continue;
        //            if (SHOW_PROP_INFO) System.out.println("Missing: " + x);
        //        }

        // Starting with Unicode 13, we preprocess the Unihan data using the
        // <Unicode Tools>/py/splitunihan.py script.
        // It parses the small number of large, multi-property Unihan*.txt files
        // and writes many smaller, single-property files like kTotalStrokes.txt.
        for (UcdProperty prop : UcdProperty.values()) {
            if (prop.getShortName().startsWith("cjk")) {
                fromUnihanProperty(prop);
            }
        }
    }

    private static void parseMissingFromValueAliases(Iterable<String> aliasesLines) {
        for (UcdLineParser.UcdLine line :
                new UcdLineParser(aliasesLines).withRange(false).withMissing(true)) {
            if (line.getType() == UcdLineParser.UcdLine.Contents.MISSING) {
                VersionInfo last_applicable_version = Settings.LATEST_VERSION_INFO;
                if (line.getParts().length > 3) {
                    final String suffix = line.getParts()[3];
                    if (!VERSION.matcher(suffix).matches()) {
                        throw new IllegalArgumentException(
                                "Expected version suffix, got " + suffix);
                    }
                    last_applicable_version = VersionInfo.getInstance(suffix.substring(1));
                }
                // # @missing: 0000..10FFFF; cjkIRG_KPSource; <none>
                setDefaultValueForPropertyName(
                        line.getOriginalLine(),
                        line.getParts()[1],
                        line.getParts()[2],
                        /* isEmpty= */ false,
                        last_applicable_version);
            }
        }
    }

    static {
        init();
    }
}
