package org.unicode.props;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.RegexUtilities;
import org.unicode.idna.Regexes;
import org.unicode.props.IndexUnicodeProperties.DefaultValueType;
import org.unicode.props.PropertyUtilities.Merge;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

import com.google.common.base.Splitter;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Relation;
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
                start=end=-1;
            } else if (source.isEmpty()) {
                string = "";
                start=end=-1;
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
    public final SpecialProperty special;

    public String oldFile;
    public VersionInfo maxOldVersion = MIN_VERSION;

    String defaultValue;
    DefaultValueType defaultValueType = DefaultValueType.LITERAL;
    //UnicodeMap<String> data;
    //final Set<String> errors = new LinkedHashSet<String>();
    Pattern regex = null;
    private ValueCardinality multivalued = ValueCardinality.Singleton;
    private Pattern multivaluedSplit = SPACE;
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
    static EnumMap<UcdProperty, PropertyParsingInfo> property2PropertyInfo =
            new EnumMap<UcdProperty, PropertyParsingInfo>(UcdProperty.class);
    static Relation<String,PropertyParsingInfo> file2PropertyInfoSet =
            Relation.of(new HashMap<String, Set<PropertyParsingInfo>>(), HashSet.class);


    public PropertyParsingInfo(String file, UcdProperty property,
            int fieldNumber, SpecialProperty special) {
        this.file = file;
        this.property = property;
        this.fieldNumber = fieldNumber;
        this.special = special;
    }

    static final Pattern VERSION = Pattern.compile("v\\d+\\.\\d+");
    
    private static void fromStrings(String... propertyInfo) {
        if (propertyInfo.length < 2 || propertyInfo.length > 4) {
            throw new UnicodePropertyException("Must have 2 to 4 args: " + Arrays.asList(propertyInfo));
        }
        String _file = propertyInfo[0];
        final String propName = propertyInfo[1];
        UcdProperty _property = UcdProperty.forString(propName);

        String last = propertyInfo[propertyInfo.length-1];
        if (VERSION.matcher(last).matches()) {
        	propertyInfo[propertyInfo.length-1] = "";
            PropertyParsingInfo result = property2PropertyInfo.get(_property);
            result.oldFile = _file;
            result.maxOldVersion = VersionInfo.getInstance(last.substring(1));
            file2PropertyInfoSet.put(_file, result);
            return;
        }
        
        int temp = 1;
        if (propertyInfo.length > 2 && !propertyInfo[2].isEmpty()) {
            temp = Integer.parseInt(propertyInfo[2]);
        }
        int _fieldNumber = temp;

        SpecialProperty _special = propertyInfo.length < 4 || propertyInfo[3].isEmpty()
                ? SpecialProperty.None
                        : SpecialProperty.valueOf(propertyInfo[3]);
        PropertyParsingInfo result = new PropertyParsingInfo(_file, _property, _fieldNumber, _special);

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
        String filename;
        if (file.startsWith("Unihan") && ucdVersionRequested.compareTo(V13) >= 0) {
            filename = property.toString();
        } else {
            filename = useOldFile(ucdVersionRequested) ? oldFile : file;
        }
        return filename;
    }

    private static final VersionInfo V13 = VersionInfo.getInstance(13);

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
                    String msg = String.format("%s: %04X..%04X  %s", property, intRange.start, intRange.end, string);
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
            final PropertyParsingInfo propInfo = property == UcdProperty.Script_Extensions ? getPropertyInfo(UcdProperty.Script) : this;
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
            multivaluedSplit = NO_SPLIT;
        }
        if (multivalued2.endsWith("_COMMA")) {
            multivaluedSplit = COMMA;
            multivalued = ValueCardinality.Unordered;
            return;
        }
        multivalued = toMultiValued.get(multivalued2);
    }

    private static final class UcdFileStats {
        int lineCount;
        boolean containsEOF;
    }

    private static final class UcdLine implements Iterator<UcdLine> {
        private enum State { LOOK, HAVE_NEXT, NO_NEXT }

        private static final Pattern MISSING = Pattern.compile(
                "\\s*#\\s*@(missing|empty):?\\s*(.+)\\s*");

        enum Contents { DATA, MISSING, EMPTY }

        private final Matcher splitter;
        private final boolean withRange;
        private final boolean withMissing;
        private final Iterator<String> rawLines;
        private State state = State.LOOK;
        String line;  // original line for logging and error messages
        String line2;  // modified line for parsing
        private final UcdFileStats stats;
        Contents contents = Contents.DATA;
        ArrayList<String> partsList = new ArrayList<>();
        String[] parts = null;
        final IntRange intRange = new IntRange();

        UcdLine(Pattern splitPattern, boolean withRange, boolean withMissing,
                Iterator<String> rawLines, UcdFileStats stats) {
            splitter = splitPattern.matcher("");
            this.withRange = withRange;
            this.withMissing = withMissing;
            this.rawLines = rawLines;
            this.stats = stats;
        }

        @Override
        public boolean hasNext() {
            if (state == State.NO_NEXT) {
                return false;
            }
            if (state == State.LOOK) {
                contents = Contents.DATA;
                do {
                    if (!rawLines.hasNext()) {
                        state = State.NO_NEXT;
                        return false;
                    }
                    line = line2 = rawLines.next();
                    ++stats.lineCount;
                    final int hashPos = line2.indexOf('#');
                    if (hashPos >= 0) {
                        if (line2.contains("# EOF")) {
                            stats.containsEOF = true;
                        } else {
                            if (line2.contains("@missing")) {  // quick test
                                // # @missing: 0000..10FFFF; cjkIRG_KPSource; <none>
                                if (!withMissing) {
                                    throw new IllegalArgumentException(
                                            "Unhandled @missing line: " + line);
                                }
                                final Matcher missingMatcher = MISSING.matcher(line2);
                                if (!missingMatcher.matches()) {
                                    System.err.println(RegexUtilities.showMismatch(MISSING, line2));
                                    throw new UnicodePropertyException(
                                            "Bad @missing statement: " + line);
                                }
                                contents = missingMatcher.group(1).equals("empty") ?
                                        Contents.EMPTY : Contents.MISSING;
                                line2 = missingMatcher.group(2);
                            }
                        }
                        if (contents == Contents.DATA) {
                            line2 = line2.substring(0, hashPos);
                        }
                    }
                    line2 = line2.trim();
                    if (line2.startsWith("\ufeff")) {
                        line2 = line2.substring(1).trim();
                    }
                } while (line2.isEmpty());
                partsList.clear();
                parts = Regexes.split(splitter.reset(line2), line2, partsList, parts);
                // 3400;<CJK Ideograph Extension A, First>;Lo;0;L;;;;;N;;;;;
                // 4DB5;<CJK Ideograph Extension A, Last>;Lo;0;L;;;;;N;;;;;
                // U+4F70   kAccountingNumeric  100
                if (withRange || contents != Contents.DATA) {
                    try {
                        intRange.set(parts[0]);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("line: " + line, e);
                    }
                    if (contents != Contents.DATA &&
                            (intRange.start != 0 || intRange.end != 0x10FFFF)) {
                        System.err.println("Unexpected range: " + line);
                    }
                }
                state = State.HAVE_NEXT;
            }
            return true;  // HAVE_NEXT
        }

        @Override
        public UcdLine next() {
            if (state != State.HAVE_NEXT && !hasNext()) {
                throw new NoSuchElementException();
            }
            state = State.LOOK;
            return this;
        }
    }

    /**
     * Low-level parser for lines of Unicode data files with TAB or semicolon separators.
     * Skips comments and empty lines.
     * Handles @missing lines, if this option is turned on.
     * Parses a range of code points from the first field, unless this option is turned off.
     */
    private static final class UcdLineParser implements Iterable<UcdLine> {
        private boolean withTabs = false;
        private boolean withRange = true;
        private boolean withMissing = false;
        private final Iterable<String> rawLines;
        final UcdFileStats stats = new UcdFileStats();

        UcdLineParser(Iterable<String> rawLines) {
            this.rawLines = rawLines;
        }

        UcdLineParser withTabs(boolean t) {
            withTabs = t;
            return this;
        }

        UcdLineParser withRange(boolean r) {
            withRange = r;
            return this;
        }

        UcdLineParser withMissing(boolean m) {
            withMissing = m;
            return this;
        }

        @Override
        public Iterator<UcdLine> iterator() {
            return new UcdLine(
                    withTabs ? TAB : SEMICOLON, withRange, withMissing,
                    rawLines.iterator(), stats);
        }
    }

    static void parseSourceFile(
            IndexUnicodeProperties indexUnicodeProperties,
            final String fullFilename, final String fileName) {
        FileType fileType = file2Type.get(fileName);
        if (fileType == null) {
            fileType = FileType.Field;
        }
    
        final Set<PropertyParsingInfo> propInfoSetRaw = file2PropertyInfoSet.get(fileName);
        final Set<PropertyParsingInfo> propInfoSet = new LinkedHashSet<>();
        for (final PropertyParsingInfo propInfo : propInfoSetRaw) {
            // the propInfoSet has all the properties, even those that are not in this version of the file
            if (!fileName.equals(propInfo.getFileName(indexUnicodeProperties.ucdVersion))) {
                continue;
            }
            propInfoSet.add(propInfo);
        }
    
        for (final PropertyParsingInfo propInfo : propInfoSet) {
            PropertyUtilities.putNew(indexUnicodeProperties.property2UnicodeMap,
                    propInfo.property, new UnicodeMap<String>());
        }
    
        // if a file is not in a given version of Unicode, we skip it.
        if (fullFilename == null) {
            if (IndexUnicodeProperties.SHOW_LOADED) {
                System.out.println(
                        "Version\t" + indexUnicodeProperties.getUcdVersion() +
                        "\tFile doesn't exist: " + fileName);
            }
        } else {
            indexUnicodeProperties.getFileNames().add(fullFilename);
            UcdLineParser parser = new UcdLineParser(FileUtilities.in("", fullFilename));
            if (fileName.startsWith("Unihan") || fileName.startsWith("k")) {
                parser.withTabs(true);
            }
            PropertyParsingInfo propInfo;
    
            switch (fileType) {
            case CJKRadicals:
                propInfo = property2PropertyInfo.get(UcdProperty.CJK_Radical);
                parseCJKRadicalsFile(parser.withRange(false), propInfo,
                        indexUnicodeProperties.property2UnicodeMap.get(propInfo.property));
                break;
            case NamedSequences:
                parseNamedSequencesFile(parser.withRange(false),
                        indexUnicodeProperties, propInfoSet);
                break;
            case PropertyValue:
                parsePropertyValueFile(parser.withMissing(true), indexUnicodeProperties);
                break;
            case Confusables:
                parseConfusablesFile(parser, indexUnicodeProperties, propInfoSet);
                break;
            case StandardizedVariants:
                parseStandardizedVariantsFile(parser, indexUnicodeProperties, propInfoSet);
                break;
            case NameAliases:
                parseNameAliasesFile(parser, indexUnicodeProperties, propInfoSet);
                break;
            case HackField:
                parseUnicodeDataFile(parser, indexUnicodeProperties, propInfoSet);
                break;
            case Field:
                if (propInfoSet.size() == 1 &&
                        (propInfo = propInfoSet.iterator().next()).special == SpecialProperty.None &&
                        propInfo.fieldNumber == 1) {
                    parseSimpleFieldFile(parser.withMissing(true), propInfo,
                            indexUnicodeProperties.property2UnicodeMap.get(propInfo.property));
                } else {
                    parseFieldFile(parser.withMissing(true), indexUnicodeProperties, propInfoSet);
                }
                break;
            case List:
                if (propInfoSet.size() == 1) {
                    propInfo = propInfoSet.iterator().next();
                    assert propInfo.property.getType() == PropertyType.Binary;
                    parseListFile(parser, propInfo,
                            indexUnicodeProperties.property2UnicodeMap.get(propInfo.property));
                } else {
                    throw new UnicodePropertyException(
                            "List files must have only one property, and must be Boolean");
                }
                break;
            default:
                throw new UnicodePropertyException();
            }

            //            if (property2UnicodeMap.get(UcdProperty.Script).values().contains("Hatran")) {
            //                int x = 1;
            //            }
            if (IndexUnicodeProperties.SHOW_LOADED) {
                System.out.println(
                        "Version\t" + indexUnicodeProperties.getUcdVersion() +
                        "\tLoaded: " + fileName + "\tlines: " + parser.stats.lineCount +
                        (parser.stats.containsEOF ? "" : "\t*NO '# EOF'"));
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
                indexUnicodeProperties.internalStoreCachedMap(Settings.BIN_DIR, propInfo.property, data);
            }
        }
    }

    private static void parseCJKRadicalsFile(UcdLineParser parser,
            PropertyParsingInfo propInfo, UnicodeMap<String> data) {
        /*
         * CJKRadicals
         * The first field is the radical number.
         * The second field is the CJK Radical character.
         * The third field is the CJK Unified Ideograph.
         * 1; 2F00; 4E00
         */
        for (UcdLine line : parser) {
            IntRange intRange = line.intRange;
            String[] parts = line.parts;
            intRange.set(parts[1]);
            propInfo.put(data, intRange, parts[0]);
            intRange.set(parts[2]);
            propInfo.put(data, intRange, parts[0]);
        }
    }

    private static void parseNamedSequencesFile(UcdLineParser parser,
            IndexUnicodeProperties indexUnicodeProperties, Set<PropertyParsingInfo> propInfoSet) {
        for (UcdLine line : parser) {
            line.intRange.set(line.parts[1]);
            for (final PropertyParsingInfo propInfo : propInfoSet) {
                final UnicodeMap<String> data =
                        indexUnicodeProperties.property2UnicodeMap.get(propInfo.property);
                propInfo.put(data, line.intRange, line.parts[0]);
            }
        }
    }

    private static void parsePropertyValueFile(UcdLineParser parser,
            IndexUnicodeProperties indexUnicodeProperties) {
        for (UcdLine line : parser) {
            String propName = line.parts[1];
            UcdProperty item = UcdProperty.forString(propName);
                
            if (item == null) {
                throw new IllegalArgumentException(
                        "Missing property enum in UcdProperty for " + propName +
                        "\nSee https://sites.google.com/site/unicodetools/home/newunicodeproperties");
            }

            PropertyParsingInfo propInfo;
            try {
                propInfo = property2PropertyInfo.get(item);
            } catch (Exception e) {
                throw new IllegalArgumentException(line.line, e);
            }
            //                    if (!propInfoSet.contains(propInfo)) {
            //                        throw new UnicodePropertyException("Property not listed for file: " + propInfo);
            //                    }
            String value;
            if (line.parts.length == 2) {
                assert propInfo.property.getType() == PropertyType.Binary;
                value = "Yes";
            } else {
                value = propInfo.property.getType() == PropertyType.Binary ? "Yes" : line.parts[2];
                // The value should not be an empty string.
                // Exception: NFKC_Casefold does remove some characters by mapping them to nothing.
                assert !value.isEmpty() || propInfo.property == UcdProperty.NFKC_Casefold;
                if (propInfo.property == UcdProperty.kMandarin) {
                    if (indexUnicodeProperties.oldVersion) {
                        value = IndexUnicodeProperties.fromNumericPinyin.
                                transform(value.toLowerCase(Locale.ENGLISH));
                    }
                }
            }
            if (line.contents == UcdLine.Contents.DATA) {
                final UnicodeMap<String> data;
                try {
                    data = indexUnicodeProperties.property2UnicodeMap.get(propInfo.property);
                } catch (Exception e) {
                    throw new IllegalArgumentException(line.line, e);
                }
                propInfo.put(data, line.intRange, value);
            } else {
                setPropDefault(propInfo.property, value, line.line,
                        line.contents == UcdLine.Contents.EMPTY);
            }
        }
    }

    private static void parseConfusablesFile(UcdLineParser parser,
            IndexUnicodeProperties indexUnicodeProperties, Set<PropertyParsingInfo> propInfoSet) {
        for (UcdLine line : parser) {
            IntRange intRange = line.intRange;
            String[] parts = line.parts;
            String propName = "Confusable_" + parts[2];
            final UcdProperty ucdProp = UcdProperty.forString(propName);
            if (ucdProp == null) {
                throw new IllegalArgumentException(
                        "Missing property enum in UcdProperty for " + propName +
                        "\nSee https://sites.google.com/site/unicodetools/home/newunicodeproperties");
            }
            final PropertyParsingInfo propInfo = property2PropertyInfo.get(ucdProp);
            if (!propInfoSet.contains(propInfo)) {
                throw new UnicodePropertyException("Property not listed for file: " + propInfo);
            }
            final UnicodeMap<String> data = indexUnicodeProperties.property2UnicodeMap.get(propInfo.property);
            propInfo.put(data, intRange, parts[1]);
            intRange.set(parts[1]);
            if (intRange.string == null) {
                if (!data.containsKey(intRange.start)) {
                    propInfo.put(data, intRange, parts[1]);
                }
            } else if (!intRange.string.isEmpty() && !data.containsKey(intRange.string)) {
                propInfo.put(data, intRange, parts[1]);
            }
        }
    }

    private static void parseStandardizedVariantsFile(UcdLineParser parser,
            IndexUnicodeProperties indexUnicodeProperties, Set<PropertyParsingInfo> propInfoSet) {
        for (UcdLine line : parser) {
            String[] parts = line.parts;
            if (!parts[2].isEmpty()) {
                parts[1] = parts[1] + " (" + parts[2] + ")";
            }
            parseFields(line, indexUnicodeProperties, propInfoSet,
                    IndexUnicodeProperties.ALPHABETIC_JOINER, false);
        }
    }

    private static void parseNameAliasesFile(UcdLineParser parser,
            IndexUnicodeProperties indexUnicodeProperties, Set<PropertyParsingInfo> propInfoSet) {
        for (UcdLine line : parser) {
            parseFields(line, indexUnicodeProperties, propInfoSet,
                    IndexUnicodeProperties.ALPHABETIC_JOINER, false);
        }
    }

    private static void parseUnicodeDataFile(UcdLineParser parser,
            IndexUnicodeProperties indexUnicodeProperties, Set<PropertyParsingInfo> propInfoSet) {
        int lastCodepoint = 0;
        for (UcdLine line : parser) {
            String[] parts = line.parts;
            boolean hackHangul = false;
            if (parts[1].endsWith("Last>")) {
                line.intRange.start = lastCodepoint + 1;
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
                } else {
                    parts[1] = null;
                }
            } else if (parts[1].startsWith("CJK COMPATIBILITY IDEOGRAPH-")) {
                parts[1] = "CJK COMPATIBILITY IDEOGRAPH-#"; // hack for uniform data
            }
            lastCodepoint = line.intRange.end;
            if (!parts[5].isEmpty() && parts[5].indexOf('<') >= 0) {
                // Decomposition_Mapping: Remove the decomposition type.
                parts[5] = DECOMP_REMOVE.matcher(parts[5]).replaceAll("").trim();
            }
            parseFields(line, indexUnicodeProperties, propInfoSet, null, hackHangul);
        }
    }

    private static void parseFieldFile(UcdLineParser parser,
            IndexUnicodeProperties indexUnicodeProperties, Set<PropertyParsingInfo> propInfoSet) {
        for (UcdLine line : parser) {
            parseFields(line, indexUnicodeProperties, propInfoSet, null, false);
        }
    }

    private static void parseFields(UcdLine line,
            IndexUnicodeProperties indexUnicodeProperties, Set<PropertyParsingInfo> propInfoSet,
            Merge<String> merger, boolean hackHangul) {
        String[] parts = line.parts;
        if (line.contents == UcdLine.Contents.DATA) {
            for (final PropertyParsingInfo propInfo : propInfoSet) {
                final UnicodeMap<String> data =
                        indexUnicodeProperties.property2UnicodeMap.get(propInfo.property);
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
                default:
                    throw new UnicodePropertyException();
                }
                String value = propInfo.fieldNumber >= parts.length ? "" 
                        : parts[propInfo.fieldNumber];
                propInfo.put(data, line.intRange, value, merger,
                        hackHangul && propInfo.property == UcdProperty.Decomposition_Mapping);
            }
        } else {
            for (final PropertyParsingInfo propInfo : propInfoSet) {
                final String value = propInfo.fieldNumber < parts.length ?
                        parts[propInfo.fieldNumber] : null;
                setPropDefault(
                        propInfo.property, value, line.line,
                        line.contents == UcdLine.Contents.EMPTY);
            }
        }
    }

    private static void parseSimpleFieldFile(UcdLineParser parser,
            PropertyParsingInfo propInfo, UnicodeMap<String> data) {
        for (UcdLine line : parser) {
            if (line.contents == UcdLine.Contents.DATA) {
                propInfo.put(data, line.intRange, line.parts[1], null, false);
            } else {
                setPropDefault(
                        propInfo.property, line.parts[1], line.line,
                        line.contents == UcdLine.Contents.EMPTY);
            }
        }
    }

    private static void parseListFile(UcdLineParser parser,
            PropertyParsingInfo propInfo, UnicodeMap<String> data) {
        for (UcdLine line : parser) {
            propInfo.put(data, line.intRange, "Yes");
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

    public static void getRegexInfo(String line) {
        try {
            line = line.trim();
            if (line.startsWith("$")) {
                final String[] parts = EQUALS.split(line);
                IndexUnicodeProperties.vr.add(parts[0], IndexUnicodeProperties.vr.replace(parts[1]));
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
     * @param ucdVersion2 
     * @internal
     * @deprecated
     */
    public static Relation<String,PropertyParsingInfo> getFile2PropertyInfoSet() {
        return file2PropertyInfoSet;
    }

    static void setFile2PropertyInfoSet(Relation<String,PropertyParsingInfo> file2PropertyInfoSet) {
        PropertyParsingInfo.file2PropertyInfoSet = file2PropertyInfoSet;
    }

    public static void setPropDefault(UcdProperty prop, String value, String line, boolean isEmpty) {
        if (prop == IndexUnicodeProperties.CHECK_PROPERTY) {
            System.out.format("** %s %s %s %s\n", prop, value, line, isEmpty);
        }
        final PropertyParsingInfo propInfo = property2PropertyInfo.get(prop);
    
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

    private static void setDefaultValueForPropertyName(String line,
            String propName, String value, boolean isEmpty) {
        final UcdProperty ucdProp = UcdProperty.forString(propName);
        if (ucdProp == null) {
            throw new IllegalArgumentException(propName + " not defined. "
                    + "See https://sites.google.com/site/unicodetools/home/newunicodeproperties");
        }
        final PropertyParsingInfo propInfo = property2PropertyInfo.get(ucdProp);
        setPropDefault(propInfo.property, value, line, isEmpty);
    }

    static void init() {
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
                fromStrings(parts);
            }
        }
        // DO THESE FIRST (overrides values in files!)
        parseMissingFromValueAliases(
                FileUtilities.in(IndexUnicodeProperties.class, "ExtraPropertyAliases.txt"));
        parseMissingFromValueAliases(
                FileUtilities.in(IndexUnicodeProperties.class, "ExtraPropertyValueAliases.txt"));

        String propValueAliases = Utility.getMostRecentUnicodeDataFile(
                "PropertyValueAliases", GenerateEnums.ENUM_VERSION, true, false);
        if (propValueAliases == null) {
            throw new MissingResourceException(
                    "unable to find PropertyValueAliases.txt for version " +
                    GenerateEnums.ENUM_VERSION +
                    "; check the paths in class Settings and in your environment variables",
                    "PropertyParsingInfo", "");
        }
        parseMissingFromValueAliases(FileUtilities.in("", propValueAliases));

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
        for (UcdLine line : new UcdLineParser(aliasesLines).withRange(false).withMissing(true)) {
            if (line.contents == UcdLine.Contents.MISSING) {
                // # @missing: 0000..10FFFF; cjkIRG_KPSource; <none>
                setDefaultValueForPropertyName(line.line,
                        line.parts[1], line.parts[2], /* isEmpty=*/ false);
            }
        }
    }

    static {
        init();
    }
}
