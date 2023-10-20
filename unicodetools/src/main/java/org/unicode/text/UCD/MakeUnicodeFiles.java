package org.unicode.text.UCD;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.VersionInfo;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.Tabber;
import org.unicode.cldr.util.props.UnicodeLabel;
import org.unicode.props.BagFormatter;
import org.unicode.props.DefaultValues;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Bidi_Class_Values;
import org.unicode.props.UcdPropertyValues.Block_Values;
import org.unicode.props.UcdPropertyValues.East_Asian_Width_Values;
import org.unicode.props.UcdPropertyValues.Line_Break_Values;
import org.unicode.props.UnicodeProperty;
import org.unicode.text.UCD.MakeUnicodeFiles.Format.PrintStyle;
import org.unicode.text.utility.ChainException;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.UnicodeDataFile;
import org.unicode.text.utility.Utility;
import org.unicode.tools.Segmenter;

public class MakeUnicodeFiles {
    static boolean DEBUG = false;

    public static void main(String[] args) throws IOException {
        generateFile();
        System.out.println("DONE");
    }

    static class Format {
        public static Format theFormat = new Format(); // singleton

        Map<String, PrintStyle> printStyleMap =
                new TreeMap<String, PrintStyle>(UnicodeProperty.PROPERTY_COMPARATOR);
        Map<String, PrintStyle> filePrintStyleMap = new TreeMap<String, PrintStyle>();
        static PrintStyle DEFAULT_PRINT_STYLE = new PrintStyle();
        Map<String, List<String>> fileToPropertySet = new TreeMap<String, List<String>>();
        Map<String, String> fileToComments = new TreeMap<String, String>();
        Map<String, String> fileToDirectory = new TreeMap<String, String>();
        Map<String, List<String>> propertyToOrderedValues = new TreeMap<String, List<String>>();
        Map<String, Map<String, String>> propertyToValueToComments =
                new TreeMap<String, Map<String, String>>();
        Map<String, String> hackMap = new HashMap<String, String>();
        UnicodeProperty.MapFilter hackMapFilter;
        String[] filesToDo;

        private Format() {
            build();
        }

        void printFileComments(PrintWriter pw, String filename) {
            final String fileComments = fileToComments.get(filename);
            if (fileComments != null) {
                pw.println(fileComments);
            }
        }

        private void addPrintStyle(String options) {
            final PrintStyle result = new PrintStyle();
            printStyleMap.put(result.parse(options), result);
        }

        public PrintStyle getPrintStyle(String propname) {
            final PrintStyle result = printStyleMap.get(propname);
            if (result != null) {
                return result;
            }
            if (DEBUG) {
                System.out.println("Using default style!");
            }
            return DEFAULT_PRINT_STYLE;
        }

        public static class PrintStyle {
            boolean noLabel = false;
            boolean makeUppercase = false;
            boolean makeFirstLetterLowercase = false;
            boolean orderByRangeStart = false;
            boolean interleaveValues = false;
            // Whether the file should be produced in the style of VerticalOrientation.txt and the
            // Unicode 15.1 and later LineBreak.txt and EastAsianWidth.txt, which are all generated
            // in that format by some other tool.
            boolean kenFile = false;
            // Whether the file should be produced in the style of IndicPositionalCategory.txt and
            // IndicSyllabicCategory.txt, which are both generated in that format by some other
            // tool.
            boolean roozbehFile = false;
            // Whether to separate values of enumerated properties using a line of equal signs.
            boolean separateValues = true;
            boolean hackValues = false;
            boolean mergeRanges = true;
            String nameStyle = "none";
            String valueStyle = "long";
            String skipValue = null;
            String skipUnassigned = null;
            String longValueHeading = null;
            boolean sortNumeric = false;

            String parse(String options) {
                Matcher matcher = Pattern.compile("([^\" \t]|\"[^\"]*\")+").matcher(options);
                matcher.find();
                String firstPiece = matcher.group();
                while (matcher.find()) {
                    final String piece = matcher.group();
                    // binary
                    if (piece.equals("noLabel")) {
                        noLabel = true;
                    } else if (piece.equals("makeUppercase")) {
                        makeUppercase = true;
                    } else if (piece.equals("makeFirstLetterLowercase")) {
                        makeFirstLetterLowercase = true;
                    } else if (piece.equals("orderByRangeStart")) {
                        orderByRangeStart = true;
                    } else if (piece.equals("valueList")) {
                        interleaveValues = true;
                    } else if (piece.equals("kenFile")) {
                        kenFile = true;
                    } else if (piece.equals("roozbehFile")) {
                        roozbehFile = true;
                    } else if (piece.startsWith("separateValues=")) {
                        separateValues = afterEqualsBoolean(piece);
                    } else if (piece.equals("hackValues")) {
                        hackValues = true;
                    } else if (piece.equals("sortNumeric")) {
                        sortNumeric = true;
                    } else if (piece.equals("mergeRanges")) {
                        mergeRanges = true;
                    } else if (piece.startsWith("mergeRanges")) {
                        mergeRanges = afterEqualsBoolean(piece);
                    } else if (piece.startsWith("valueStyle=")) {
                        valueStyle = afterEquals(piece);
                    } else if (piece.startsWith("nameStyle=")) {
                        nameStyle = afterEquals(piece);
                    } else if (piece.startsWith("longValueHeading=")) {
                        longValueHeading = afterEquals(piece);
                    } else if (piece.startsWith("skipValue=")) {
                        if (skipUnassigned != null) {
                            throw new IllegalArgumentException(
                                    "Can't have both skipUnassigned and skipValue");
                        }
                        skipValue = afterEquals(piece);
                    } else if (piece.startsWith("skipUnassigned=")) {
                        if (skipValue != null) {
                            throw new IllegalArgumentException(
                                    "Can't have both skipUnassigned and skipValue");
                        }
                        skipUnassigned = afterEquals(piece);
                    } else if (piece.length() != 0) {
                        throw new IllegalArgumentException(
                                "Illegal PrintStyle Parameter: " + piece + " in " + firstPiece);
                    }
                }
                return firstPiece;
            }

            private boolean afterEqualsBoolean(String piece) {
                final String value = afterEquals(piece);
                if (value.equalsIgnoreCase("true")) {
                    return true;
                } else if (value.equalsIgnoreCase("false")) {
                    return false;
                }
                throw new IllegalArgumentException(
                        "Value in <" + piece + "> must be 'true' or 'false'");
            }

            @Override
            public String toString() {
                final Class<? extends PrintStyle> myClass = getClass();
                String result = myClass.getName() + "\n";
                final Field[] myFields = myClass.getDeclaredFields();
                for (final Field myField : myFields) {
                    String value = "<private>";
                    try {
                        final Object obj = myField.get(this);
                        if (obj == null) {
                            value = "<null>";
                        } else {
                            value = obj.toString();
                        }
                    } catch (final Exception e) {
                    }
                    result += "\t" + myField.getName() + "=<" + value + ">\n";
                }
                return result;
            }
        }

        void addValueComments(String property, String value, String comments) {
            if (DEBUG) {
                showPVC(property, value, comments);
            }
            Map<String, String> valueToComments = propertyToValueToComments.get(property);
            if (valueToComments == null) {
                valueToComments = new TreeMap<String, String>();
                propertyToValueToComments.put(property, valueToComments);
            }
            valueToComments.put(value, comments);
            if (DEBUG && property.equals("BidiClass")) {
                getValueComments(property, value);
            }
        }

        private void showPVC(String property, String value, String comments) {
            System.out.println(
                    "Putting Property: <"
                            + property
                            + ">, Value: <"
                            + value
                            + ">, Comments: <"
                            + comments
                            + ">");
        }

        String getValueComments(String property, String value) {
            final Map<String, String> valueToComments = propertyToValueToComments.get(property);
            String result = null;
            if (valueToComments != null) {
                result = valueToComments.get(value);
            }
            if (DEBUG) {
                System.out.println(
                        "Getting Property: <"
                                + property
                                + ">, Value: <"
                                + value
                                + ">, Comment: <"
                                + result
                                + ">");
            }
            return result;
        }

        Map<String, String> getValue2CommentsMap(String property) {
            return propertyToValueToComments.get(property);
        }

        // Returns strings without U+0022 QUOTATION MARK (") unchanged.
        // Strings that contain " must be enclosed in them, and are returned unquoted, with "" as
        // the escape sequence, thus:
        //   meow       ↦ meow
        //   "meow"     ↦ meow
        //   """meow""" ↦ "meow"
        static String unquote(String source) {
            String contents = source;
            if (source.charAt(0) == '"' && source.charAt(source.length() - 1) == '"') {
                contents = source.substring(1, source.length() - 1);
            }
            if (contents.matches("(?<!\")(\"\")*\"(?!\")")) {
                throw new IllegalArgumentException(
                        "Syntax error: improper quotation marks in " + source);
            }
            return contents.replace("\"\"", "\"");
        }

        static String afterEquals(String source) {
            return unquote(source.substring(source.indexOf('=') + 1));
        }

        static String afterWhitespace(String source) {
            // Note: don't need to be international
            for (int i = 0; i < source.length(); ++i) {
                final char ch = source.charAt(i);
                if (Character.isWhitespace(ch)) {
                    return source.substring(i).trim();
                }
            }
            return "";
        }

        private void build() {
            BufferedReader br = null;
            try {
                br =
                        Utility.openReadFile(
                                Settings.SRC_UCD_DIR + "MakeUnicodeFiles.txt", Utility.UTF8);
                String file = null, property = null, value = "", comments = "";
                while (true) {
                    String line = br.readLine();
                    if (line == null) {
                        break;
                    }
                    line = line.trim();
                    if (line.length() == 0) {
                        if (comments.length() != 0) {
                            // Preserve blank lines between comments.
                            comments += "\n";
                        }
                        continue;
                    }
                    if (DEBUG) {
                        System.out.println("\t" + line);
                    }
                    final String lineValue = afterWhitespace(line);
                    if (line.startsWith("Format:")) {
                        if (property == null) {
                            var style = new PrintStyle();
                            filePrintStyleMap.put(style.parse(file + " " + lineValue), style);
                        } else {
                            addPrintStyle(property + " " + lineValue); // fix later
                        }
                    } else if (line.startsWith("#")) {
                        if (comments.length() != 0) {
                            comments += "\n";
                        }
                        comments += line;
                    } else {
                        // end of comments, roll up
                        comments = comments.trim();
                        if (comments.length() != 0) {
                            if (property != null) {
                                addValueComments(property, value, comments);
                            } else {
                                addFileComments(file, comments);
                            }
                            comments = "";
                        }
                        if (line.startsWith("Generate:")) {
                            filesToDo = Utility.split(lineValue.trim(), ' ');
                            if (filesToDo.length == 0
                                    || (filesToDo.length == 1 && filesToDo[0].length() == 0)) {
                                filesToDo = new String[] {".*"};
                            }
                        } else if (line.startsWith("CopyrightYear:")) {
                            Default.setYear(lineValue);
                        } else if (line.startsWith("File:")) {
                            final int p2 = lineValue.lastIndexOf('/');
                            file = lineValue.substring(p2 + 1);
                            if (p2 >= 0) {
                                fileToDirectory.put(file, lineValue.substring(0, p2 + 1));
                            }
                            property = null;
                        } else if (line.startsWith("Property:")) {
                            property = lineValue;
                            addPropertyToFile(file, property);
                            value = "";
                        } else if (line.startsWith("Value:")) {
                            value = lineValue;
                            final var values =
                                    propertyToOrderedValues.computeIfAbsent(
                                            property, k -> new ArrayList<String>());
                            values.add(value);
                        } else if (line.startsWith("HackName:")) {
                            final String regularItem = Utility.getUnskeleton(lineValue, true);
                            hackMap.put(regularItem, lineValue);
                        } else if (line.startsWith("FinalComments")) {
                            break;
                        } else {
                            throw new IllegalArgumentException("Unknown command: " + line);
                        }
                    }
                }
            } catch (final IOException e) {
                e.printStackTrace();
                throw new IllegalArgumentException("File missing");
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException ignored) {
                    }
                }
            }
            hackMapFilter = new UnicodeProperty.MapFilter(hackMap);
            write();
        }

        private void write() {
            final TreeMap<String, String> fileoptions = new TreeMap<String, String>();
            for (final Iterator<String> it = fileToPropertySet.keySet().iterator();
                    it.hasNext(); ) {
                final String key = it.next();
                if (DEBUG) {
                    System.out.println();
                    System.out.println("File:\t" + key);
                }
                final List<String> propList2 = fileToPropertySet.get(key);
                if (propList2 == null) {
                    System.out.println("SPECIAL");
                    continue;
                }
                for (final Iterator<String> pIt = propList2.iterator(); pIt.hasNext(); ) {
                    final String prop = pIt.next();
                    final String options = fileoptions.get(prop);
                    if (DEBUG) {
                        System.out.println();
                        System.out.println("Property:\t" + prop);
                        if (options != null) {
                            System.out.println("Format:\t" + options);
                        }
                    }
                    final Map<String, String> vc = getValue2CommentsMap(prop);
                    if (vc == null) {
                        continue;
                    }
                    for (final Iterator<String> it2 = vc.keySet().iterator(); it2.hasNext(); ) {
                        final String value = it2.next();
                        final String comment = vc.get(value);
                        if (DEBUG) {
                            if (!value.equals("")) {
                                System.out.println("Value:\t" + value);
                            }
                            System.out.println(comment);
                        }
                    }
                }
            }
        }

        private void addFileComments(String filename, String comment) {
            fileToComments.put(filename, comment);
        }

        private void addPropertyToFile(String filename, String property) {
            List<String> properties = fileToPropertySet.get(filename);
            if (properties == null) {
                properties = new ArrayList<String>(1);
                fileToPropertySet.put(filename, properties);
            }
            properties.add(property);
        }

        public List<String> getPropertiesFromFile(String filename) {
            return fileToPropertySet.get(filename);
        }

        public Set<String> getFiles() {
            return fileToPropertySet.keySet();
        }
    }

    public static void generateFile() throws IOException {
        for (final String element : Format.theFormat.filesToDo) {
            final String fileNamePattern = element.trim();
            final Matcher matcher =
                    Pattern.compile(fileNamePattern, Pattern.CASE_INSENSITIVE).matcher("");
            final Iterator<String> it = Format.theFormat.getFiles().iterator();
            boolean gotOne = false;
            while (it.hasNext()) {
                final String filename = it.next();
                if (!matcher.reset(filename).find()) {
                    continue;
                }
                generateFile(filename);
                gotOne = true;
            }
            if (!gotOne) {
                throw new IllegalArgumentException("Non-matching file name: " + fileNamePattern);
            }
        }
    }

    public static void generateFile(String filename) throws IOException {
        String outputDir = "UCD/" + Default.ucdVersion() + '/';
        if (filename.endsWith("Aliases")) {
            if (filename.endsWith("ValueAliases")) {
                generateValueAliasFile(filename);
            } else {
                generateAliasFile(filename);
            }
        } else if (filename.startsWith("NamedSequences")) {
            GenerateNamedSequences.generate(filename);
        } else if (filename.contains("ScriptNfkc")) {
            generateScriptNfkc(filename);
        } else {
            switch (filename) {
                case "unihan":
                    writeUnihan(outputDir + "unihan/");
                    break;
                case "NormalizationTest":
                    GenerateData.writeNormalizerTestSuite(outputDir, "NormalizationTest");
                    break;
                case "BidiTest":
                    doBidiTest(filename);
                    break;
                case "CaseFolding":
                    GenerateCaseFolding.makeCaseFold(false);
                    break;
                case "SpecialCasing":
                    GenerateCaseFolding.generateSpecialCasing(false);
                    break;
                case "StandardizedVariants":
                    GenerateStandardizedVariants.generate();
                    break;
                case "GraphemeBreakTest":
                    new GenerateBreakTest.GenerateGraphemeBreakTest(
                                    Default.ucd(), Segmenter.Target.FOR_UCD)
                            .run();
                    break;
                case "WordBreakTest":
                    new GenerateBreakTest.GenerateWordBreakTest(
                                    Default.ucd(), Segmenter.Target.FOR_UCD)
                            .run();
                    break;
                case "LineBreakTest":
                    new GenerateBreakTest.GenerateLineBreakTest(
                                    Default.ucd(), Segmenter.Target.FOR_UCD)
                            .run();
                    break;
                case "SentenceBreakTest":
                    new GenerateBreakTest.GenerateSentenceBreakTest(
                                    Default.ucd(), Segmenter.Target.FOR_UCD)
                            .run();
                    break;
                case "GraphemeBreakTest-cldr":
                    new GenerateBreakTest.GenerateGraphemeBreakTest(
                                    Default.ucd(), Segmenter.Target.FOR_CLDR)
                            .run();
                    break;
                case "DerivedName":
                case "DerivedLabel":
                    generateDerivedName(filename);
                    break;
                case "UnicodeData":
                    generateUnicodeData(filename);
                    break;
                default:
                    generatePropertyFile(filename);
                    break;
            }
        }
    }

    private static void generateDerivedName(String filename) throws IOException {
        boolean isLabel = filename.contains("Label");
        final String dir = Format.theFormat.fileToDirectory.get(filename);
        final UnicodeDataFile udf =
                UnicodeDataFile.openAndWriteHeader(
                        "UCD/" + Default.ucdVersion() + '/' + dir, filename);
        final PrintWriter pw = udf.out;
        Format.theFormat.printFileComments(pw, filename);
        final UCD ucd = Default.ucd();

        final UnicodeMap<String> names = new UnicodeMap<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i <= 0x10FFFF; ++i) {
            if (i > 0xFF && !ucd.isAssigned(i) && !ucd.isNoncharacter(i)) {
                continue;
            }
            String name = ucd.getName(i);
            if (name.startsWith("<") != isLabel) {
                continue;
            }
            if (seen.contains(name)) {
                throw new IllegalArgumentException("Duplicate name or label");
            }
            seen.add(name);
            String hex = Utility.hex(i);
            name = name.replace(hex, "*");
            names.put(i, name);
        }
        names.freeze();
        UnicodeLabel nameProp =
                new UnicodeProperty.UnicodeMapProperty()
                        .set(names)
                        .setMain("X", "X", UnicodeProperty.STRING, Default.ucdVersion());

        final BagFormatter bf = new BagFormatter();
        bf.setHexValue(false)
                .setMergeRanges(true)
                .setNameSource(null)
                .setLabelSource(null)
                .setShowCount(false)
                .setValueSource(nameProp)
                .setRangeBreakSource(
                        new UnicodeLabel.Constant("")) // prevent breaking on category boundaries
                .showSetNames(pw, UnicodeSet.ALL_CODE_POINTS);

        pw.println();
        pw.println("# EOF");
        udf.close();
    }

    private static void generateScriptNfkc(String filename) throws IOException {
        final String dir = Format.theFormat.fileToDirectory.get(filename);
        final UnicodeDataFile udf =
                UnicodeDataFile.openAndWriteHeader(
                        "UCD/" + Default.ucdVersion() + '/' + dir, filename);
        final PrintWriter pw = udf.out;
        Format.theFormat.printFileComments(pw, filename);
        final UCD ucd = Default.ucd();

        final BitSet normScripts = new BitSet();
        final UnicodeMap<R3<Integer, String, String>> results =
                new UnicodeMap<R3<Integer, String, String>>();
        for (int i = 0; i <= 0x10FFFF; ++i) {
            final byte dt = ucd.getDecompositionType(i);
            if (dt == UCD_Types.NONE) {
                continue;
            }
            final String norm = Default.nfkc().normalize(i);
            final short script = ucd.getScript(i);
            final BitSet scripts = ucd.getScripts(norm, normScripts);
            scripts.clear(UCD_Types.COMMON_SCRIPT);
            scripts.clear(UCD_Types.INHERITED_SCRIPT);
            final int expectedCount =
                    script == UCD_Types.COMMON_SCRIPT || script == UCD_Types.INHERITED_SCRIPT
                            ? 0
                            : 1;
            if (scripts.cardinality() != expectedCount) {
                results.put(
                        i,
                        Row.of(
                                Character.codePointCount(norm, 0, norm.length()),
                                UCD.getScriptID_fromIndex(script, UCD_Types.LONG),
                                ucd.getScriptIDs(norm, " ", UCD_Types.LONG)));
            }
        }
        results.freeze();
        final BagFormatter bf =
                new BagFormatter(ToolUnicodePropertySource.make(Default.ucdVersion()));
        pw.println("");

        for (final R3<Integer, String, String> value :
                results.values(new TreeSet<R3<Integer, String, String>>())) {
            final UnicodeSet uset = results.getSet(value);
            pw.println(
                    "#\t" + value.get1() + "\t=>\t" + value.get2() + "\t" + uset.toPattern(false));
            pw.println("");
            pw.println(bf.showSetNames(uset));
        }

        udf.close();
    }

    private static void generateUnicodeData(String filename) throws IOException {
        final UnicodeDataFile udf =
                UnicodeDataFile.openAndWriteHeader("UCD/" + Default.ucdVersion() + '/', filename);
        final PrintWriter pw = udf.out;
        var source = ToolUnicodePropertySource.make(Default.ucdVersion());

        final BagFormatter bf = new BagFormatter();
        bf.setHexValue(false)
                .setMergeRanges(true)
                .setRangeBreakSource(null)
                .setNoSpacesBeforeSemicolon()
                .setMinSpacesAfterSemicolon(0)
                .setUnicodeDataStyleRanges(true)
                .setNameSource(null)
                .setLabelSource(null)
                .setValueSource(new UnicodeDataHack(source))
                .setShowCount(false)
                .setShowTotal(false)
                .showSetNames(pw, new UnicodeSet(0, 0x10FFFF));
        udf.close();
    }

    private static void doBidiTest(String filename) throws IOException {
        final UnicodeDataFile udf =
                UnicodeDataFile.openAndWriteHeader("UCD/" + Default.ucdVersion() + '/', filename);
        final PrintWriter pw = udf.out;
        Format.theFormat.printFileComments(pw, filename);
        org.unicode.bidi.BidiConformanceTestBuilder.write(pw);
        udf.close();
    }

    private static void writeUnihan(String directory) throws IOException {
        final Map<String, UnicodeMap<String>> props = getUnihanProps();
        final UnicodeProperty.Factory toolFactory =
                ToolUnicodePropertySource.make(Default.ucdVersion());
        final UnicodeSet unassigned =
                toolFactory.getSet("gc=cn").addAll(toolFactory.getSet("gc=cs"));

        final Format.PrintStyle ps = new Format.PrintStyle();
        ps.noLabel = true;

        for (final String propName : props.keySet()) {
            final UnicodeDataFile udf =
                    UnicodeDataFile.openAndWriteHeader(directory, propName)
                            .setSkipCopyright(Settings.SKIP_COPYRIGHT);
            final PrintWriter pw = udf.out;

            final BagFormatter bf = new BagFormatter();
            bf.setHexValue(false)
                    .setMergeRanges(true)
                    .setNameSource(null)
                    .setLabelSource(null)
                    .setShowCount(false);

            final UnicodeProperty prop =
                    new UnicodeProperty.UnicodeMapProperty()
                            .set(props.get(propName))
                            .setMain(
                                    propName,
                                    propName,
                                    UnicodeProperty.STRING,
                                    Default.ucdVersion());
            final String name = prop.getName();
            System.out.println(
                    "Property: " + name + "; " + UnicodeProperty.getTypeName(prop.getType()));
            pw.println();
            pw.println(SEPARATOR);
            pw.println();
            pw.println("# Property:\t" + propName);

            final UnicodeMap<String> map = props.get(propName);

            if (map.getAvailableValues().size() < 100) {
                writeEnumeratedValues(pw, bf, unassigned, prop, ps);
            } else {
                bf.setValueSource(prop).showSetNames(pw, new UnicodeSet(0, 0x10FFFF));
            }
        }
    }

    private static Map<String, UnicodeMap<String>> getUnihanProps() {
        final Map<String, UnicodeMap<String>> unihanProps =
                new TreeMap<String, UnicodeMap<String>>();
        try {
            final BufferedReader in =
                    Utility.openUnicodeFile("Unihan", Default.ucdVersion(), true, Utility.UTF8);
            int lineCounter = 0;
            while (true) {
                Utility.dot(++lineCounter);

                String line = in.readLine();
                if (line == null) {
                    break;
                }
                if (line.length() < 6) {
                    continue;
                }
                if (line.charAt(0) == '#') {
                    continue;
                }
                line = line.trim();

                final int tabPos = line.indexOf('\t');
                final int tabPos2 = line.indexOf('\t', tabPos + 1);

                final String property = line.substring(tabPos + 1, tabPos2).trim();
                UnicodeMap<String> result = unihanProps.get(property);
                if (result == null) {
                    unihanProps.put(property, result = new UnicodeMap<String>());
                }

                final String scode = line.substring(2, tabPos).trim();
                final int code = Integer.parseInt(scode, 16);
                final String propertyValue = line.substring(tabPos2 + 1).trim();
                result.put(code, propertyValue);
            }
            in.close();
        } catch (final Exception e) {
            throw new ChainException("Han File Processing Exception", null, e);
        } finally {
            Utility.fixDot();
        }
        return unihanProps;
    }

    static final String SEPARATOR = "# ================================================";

    public static void generateAliasFile(String filename) throws IOException {
        final UnicodeDataFile udf =
                UnicodeDataFile.openAndWriteHeader("UCD/" + Default.ucdVersion() + '/', filename)
                        .setSkipCopyright(Settings.SKIP_COPYRIGHT);
        final PrintWriter pw = udf.out;
        final UnicodeProperty.Factory ups = ToolUnicodePropertySource.make(Default.ucdVersion());
        final TreeSet<String> sortedSet = new TreeSet<String>(CASELESS_COMPARATOR);
        final Tabber.MonoTabber mt =
                (Tabber.MonoTabber)
                        new Tabber.MonoTabber().add(25, Tabber.LEFT).add(30, Tabber.LEFT);
        int count = 0;

        for (int i = UnicodeProperty.LIMIT_TYPE - 1; i >= UnicodeProperty.BINARY; --i) {
            if ((i & UnicodeProperty.EXTENDED_MASK) != 0) {
                continue;
            }
            final List<String> list = ups.getAvailableNames(1 << i);
            // if (list.size() == 0) continue;
            sortedSet.clear();
            final StringBuffer buffer = new StringBuffer();
            for (final Iterator<String> it = list.iterator(); it.hasNext(); ) {
                final String propAlias = it.next();

                final UnicodeProperty up = ups.getProperty(propAlias);
                final List<String> aliases = up.getNameAliases();
                String firstAlias = aliases.get(0).toString();
                if (firstAlias.isEmpty()) {
                    throw new IllegalArgumentException("Internal error");
                }
                if (aliases.size() == 1) {
                    sortedSet.add(mt.process(firstAlias + "\t; " + firstAlias));
                } else {
                    buffer.setLength(0);
                    boolean isFirst = true;
                    for (final Iterator<String> it2 = aliases.iterator(); it2.hasNext(); ) {
                        if (isFirst) {
                            isFirst = false;
                        } else {
                            buffer.append("\t; ");
                        }
                        buffer.append(it2.next());
                    }
                    if (aliases.size() == 1) {
                        // repeat
                        buffer.append("\t; ").append(firstAlias);
                    }
                    sortedSet.add(mt.process(buffer.toString()));
                }
            }
            if (i == UnicodeProperty.STRING) {
                for (final String element : specialString) {
                    sortedSet.add(mt.process(element));
                }
            } else if (i == UnicodeProperty.MISC) {
                for (final String element : specialMisc) {
                    sortedSet.add(mt.process(element));
                }
            }
            pw.println();
            pw.println(SEPARATOR);
            pw.println("# " + UnicodeProperty.getTypeName(i) + " Properties");
            pw.println(SEPARATOR);
            for (final Iterator<String> it = sortedSet.iterator(); it.hasNext(); ) {
                pw.println(it.next());
                count++;
            }
        }
        pw.println();
        pw.println(SEPARATOR);
        pw.println("# Total:    " + count);
        pw.println();
        pw.println("# EOF");
        udf.close();
    }

    static String[] specialMisc = {
        // "isc\t; ISO_Comment",
        // "na1\t; Unicode_1_Name",
        // "URS\t; Unicode_Radical_Stroke"
    };

    static String[] specialString = {
        "dm\t; Decomposition_Mapping",
        "lc\t; Lowercase_Mapping",
        // "scc\t; Special_Case_Condition",
        // "sfc\t; Simple_Case_Folding",
        "slc\t; Simple_Lowercase_Mapping",
        "stc\t; Simple_Titlecase_Mapping",
        "suc\t; Simple_Uppercase_Mapping",
        "tc\t; Titlecase_Mapping",
        "uc\t; Uppercase_Mapping"
    };

    static String[] specialGC = {
        "gc\t;\tC\t;\tOther\t# Cc | Cf | Cn | Co | Cs",
        "gc\t;\tL\t;\tLetter\t# Ll | Lm | Lo | Lt | Lu",
        "gc\t;\tLC\t;\tCased_Letter\t# Ll | Lt | Lu",
        "gc\t;\tM\t;\tMark\t;\tCombining_Mark\t# Mc | Me | Mn",
        "gc\t;\tN\t;\tNumber\t# Nd | Nl | No",
        "gc\t;\tP\t;\tPunctuation\t;\tpunct\t# Pc | Pd | Pe | Pf | Pi | Po | Ps",
        "gc\t;\tS\t;\tSymbol\t# Sc | Sk | Sm | So",
        "gc\t;\tZ\t;\tSeparator\t# Zl | Zp | Zs"
    };

    static final RuleBasedCollator CASELESS_COMPARATOR =
            (RuleBasedCollator) Collator.getInstance(ULocale.ROOT);

    static {
        CASELESS_COMPARATOR.setNumericCollation(true);
        CASELESS_COMPARATOR.freeze();
    }

    // PropertyValueAliases.txt
    public static void generateValueAliasFile(String filename) throws IOException {
        String outputDir = "UCD/" + Default.ucdVersion() + '/';
        final UnicodeDataFile udf =
                UnicodeDataFile.openAndWriteHeader(outputDir, filename)
                        .setSkipCopyright(Settings.SKIP_COPYRIGHT);
        final UnicodeDataFile diff =
                UnicodeDataFile.openAndWriteHeader(outputDir + "extra/", "diff");
        final PrintWriter pw = udf.out;
        final PrintWriter diffOut = diff.out;
        Format.theFormat.printFileComments(pw, filename);
        final UnicodeProperty.Factory toolFactory =
                ToolUnicodePropertySource.make(Default.ucdVersion());
        final UnicodeProperty.Factory lastFactory =
                ToolUnicodePropertySource.make(Utility.getPreviousUcdVersion(Default.ucdVersion()));
        final UnicodeSet lastDefined =
                new UnicodeSet(lastFactory.getSet("gc=cn")).complement().freeze();

        final BagFormatter bf = new BagFormatter(toolFactory);
        final BagFormatter bfdiff = new BagFormatter(toolFactory);
        final StringBuffer buffer = new StringBuffer();
        final Set<String> sortedSet = new TreeSet<String>(CASELESS_COMPARATOR);

        // gc ; C         ; Other                            # Cc | Cf | Cn | Co | Cs
        // 123456789012345678901234567890123

        // sc ; Arab      ; Arabic

        final Tabber.MonoTabber mt2 =
                (Tabber.MonoTabber)
                        new Tabber.MonoTabber()
                                .add(3, Tabber.LEFT)
                                .add(2, Tabber.LEFT) // ;
                                .add(33, Tabber.LEFT)
                                .add(2, Tabber.LEFT) // ;
                                .add(33, Tabber.LEFT)
                                .add(2, Tabber.LEFT) // ;
                                .add(33, Tabber.LEFT);

        // ccc; 216; ATAR ; Attached_Above_Right
        final Tabber.MonoTabber mt3 =
                (Tabber.MonoTabber)
                        new Tabber.MonoTabber()
                                .add(3, Tabber.LEFT)
                                .add(2, Tabber.LEFT) // ;
                                .add(3, Tabber.RIGHT)
                                .add(2, Tabber.LEFT) // ;
                                .add(27, Tabber.LEFT)
                                .add(2, Tabber.LEFT) // ;
                                .add(33, Tabber.LEFT)
                                .add(2, Tabber.LEFT) // ;
                                .add(33, Tabber.LEFT);

        // final Set<String> skipNames = new HashSet<String>(Arrays.asList("Lowercase_Mapping",
        // "Uppercase_Mapping", "Titlecase_Mapping"));

        for (final Iterator<String> it = toolFactory.getAvailableNames().iterator();
                it.hasNext(); ) {
            final String propName = it.next();
            final UnicodeProperty up = toolFactory.getProperty(propName);
            final int type = up.getType();
            if ((type & UnicodeProperty.EXTENDED_MASK) != 0) {
                continue;
            }
            //            if (skipNames.contains(propName)) {
            //                continue;
            //            }

            final String shortProp = up.getFirstNameAlias();
            sortedSet.clear();
            final boolean isJamoShortName = propName.equals("Jamo_Short_Name");
            final boolean isJoiningGroup = propName.equals("Joining_Group");

            if (isJamoShortName
                    || ((1 << type)
                                    & (UnicodeProperty.STRING_OR_MISC_MASK
                                            | (1 << UnicodeProperty.NUMERIC)))
                            == 0) {
                for (final String value : up.getAvailableValues()) {
                    if (propName.equals("Script")
                            && up.getSet(value).isEmpty()
                            && !value.equals("Katakana_Or_Hiragana")) {
                        continue;
                    }
                    final List<String> l = up.getValueAliases(value);
                    // HACK
                    if (isJoiningGroup && value.equals("Hamzah_On_Ha_Goal")) {
                        continue;
                    }
                    if (DEBUG) {
                        System.out.println(value + "\t" + bf.join(l));
                    }

                    // HACK
                    Tabber mt = mt2;
                    if (l.size() == 1) {
                        if (propName.equals("Canonical_Combining_Class")) {
                            continue;
                        }
                        l.add(0, l.get(0)); // double up
                    } else if (propName.equals("Canonical_Combining_Class")) {
                        if (l.size() == 2) {
                            l.add(l.get(1)); // double up final value
                        }
                        mt = mt3;
                    } else if (l.size() == 2 && propName.equals("Decomposition_Type")) {
                        l.add(0, l.get(0)); // double up
                    }
                    if (UnicodeProperty.equalNames(value, "Cyrillic_Supplement")) {
                        l.add("Cyrillic_Supplementary");
                    }

                    buffer.setLength(0);
                    buffer.append(shortProp);
                    for (final Iterator<String> it3 = l.iterator(); it3.hasNext(); ) {
                        buffer.append("\t; \t" + it3.next());
                    }

                    sortedSet.add(mt.process(buffer.toString()));
                }
            }
            // HACK
            boolean isGC = propName.equals("General_Category");
            if (isGC) {
                for (final String element : specialGC) {
                    sortedSet.add(mt2.process(element));
                }
            }
            final boolean isCcc = propName.equals("Canonical_Combining_Class");
            pw.println();
            pw.println("# " + propName + " (" + shortProp + ")");
            pw.println();
            for (final Object element : sortedSet) {
                final String line = (String) element;
                pw.println(line);
                if (isCcc && line.contains("132")) {
                    pw.println("ccc; 133; CCC133                     ; CCC133 # RESERVED");
                }
            }
            if (propName.equals("Bidi_Mirroring_Glyph")
                    || propName.equals("Equivalent_Unified_Ideograph")
                    || propName.equals("NFKC_Casefold")
                    || propName.equals("NFKC_Simple_Casefold")
                    || propName.equals("Script_Extensions")) {
                // Action item [172-A71]: Don't print @missing lines
                // for properties whose specific data files already contain such lines.
            } else if (sortedSet.size() == 0 || isGC || isJamoShortName) {
                printDefaultValueComment(pw, propName, up, true, null);
            } else if (propName.equals("Bidi_Paired_Bracket_Type")) {
                printDefaultValueComment(pw, propName, up, true, "n");
            }

            // now add to differences
            if (up.isType(UnicodeProperty.STRING_OR_MISC_MASK)) {
                continue;
            }
            sortedSet.clear();
            sortedSet.addAll(up.getAvailableValues());
            final UnicodeProperty lastProp = lastFactory.getProperty(propName);
            for (final String value : sortedSet) {
                UnicodeSet set;
                try {
                    set = up.getSet(value);
                    if (set == null) {
                        continue;
                    }
                } catch (final Exception e) {
                    System.err.println(e);
                    continue;
                }
                final UnicodeSet lastSet = lastProp.getSet(value);
                if (lastSet != null) {
                    set = new UnicodeSet(set).removeAll(lastSet);
                }

                final UnicodeSet changedValues = new UnicodeSet(set).retainAll(lastDefined);
                if (changedValues.size() != 0) {
                    bfdiff.setValueSource(value);
                    bfdiff.setMergeRanges(false);
                    bfdiff.setLabelSource(lastProp);
                    final String line = "# " + up.getName() + "=" + value;

                    diffOut.println("");
                    diffOut.println(line);
                    diffOut.println("");
                    System.out.println(line);
                    bfdiff.showSetNames(diffOut, changedValues);
                    diffOut.flush();
                }
                //        UnicodeSet newValues = new UnicodeSet(set).retainAll(newCharacters);
                //        if (changedValues.size() != 0) {
                //        pw.println("# NEW " + up.getName() + "=" + value);
                //        bfdiff.showSetNames(diffOut, newValues);
                //        }
            }
        }
        pw.println();
        pw.println("# EOF");
        udf.close();
        diff.close();
    }

    private static void printDefaultValueComment(
            PrintWriter pw,
            String propName,
            UnicodeProperty up,
            boolean showPropName,
            String defaultValue) {
        if (Default.ucd().isAllocated(0xE0000)) {
            throw new IllegalArgumentException(
                    "The paradigm 'default value' code point needs fixing!");
        }
        if (defaultValue != null) {
            // ok
        } else if (propName.equals("Bidi_Mirroring_Glyph")
                || propName.equals("Bidi_Paired_Bracket")
                || propName.equals("ISO_Comment")
                || propName.equals("Name")
                || propName.equals("Unicode_Radical_Stroke")
                || propName.equals("Unicode_1_Name")
                || propName.equals("Jamo_Short_Name")) {
            defaultValue = "<none>";
        } else if (propName.equals("Script_Extensions")) {
            defaultValue = "<script>";
        } else if (up.getType() == UnicodeProperty.NUMERIC) {
            defaultValue = "NaN";
        } else if (up.getType() == UnicodeProperty.STRING) {
            defaultValue = "<code point>";
        } else if (up.getType() == UnicodeProperty.MISC) {
            defaultValue = "<none>";
        } else {
            defaultValue = up.getValue(0xE0000);
        }

        pw.println(
                "# @missing: 0000..10FFFF; "
                        + (showPropName ? propName + "; " : "")
                        + defaultValue);
    }

    public static void generatePropertyFile(String filename) throws IOException {
        String dir = Format.theFormat.fileToDirectory.get(filename);
        if (dir == null) {
            dir = "";
        }
        dir = "UCD/" + Default.ucdVersion() + '/' + dir;
        final UnicodeDataFile udf =
                UnicodeDataFile.openAndWriteHeader(dir, filename)
                        .setSkipCopyright(Settings.SKIP_COPYRIGHT);
        final PrintWriter pwFile = udf.out;
        // bf2.openUTF8Writer(UCD_Types.GEN_DIR, "Test" + filename + ".txt");
        Format.theFormat.printFileComments(pwFile, filename);
        final UnicodeProperty.Factory toolFactory =
                ToolUnicodePropertySource.make(Default.ucdVersion());
        final UnicodeSet unassigned =
                toolFactory.getSet("gc=cn").addAll(toolFactory.getSet("gc=cs"));
        // System.out.println(unassigned.toPattern(true));
        // .removeAll(toolFactory.getSet("noncharactercodepoint=Yes"));

        final List<String> propList = Format.theFormat.getPropertiesFromFile(filename);
        for (final Iterator<String> propIt = propList.iterator(); propIt.hasNext(); ) {
            final StringWriter pwReal = new StringWriter();
            final PrintWriter pwProp = new PrintWriter(pwReal);

            final BagFormatter bf = new BagFormatter(toolFactory);
            final String nextPropName = propIt.next();
            UnicodeProperty prop;
            String name;
            try {
                prop = toolFactory.getProperty(nextPropName);
                name = prop.getName();
                System.out.println(
                        "Property: " + name + "; " + UnicodeProperty.getTypeName(prop.getType()));
            } catch (final Exception e) {
                throw new IllegalArgumentException("No property for name: " + nextPropName);
            }
            final Format.PrintStyle ps =
                    Format.theFormat.filePrintStyleMap.getOrDefault(
                            filename, Format.theFormat.getPrintStyle(name));
            if (!ps.kenFile) {
                pwProp.println();
                if (!ps.separateValues) {
                    pwProp.println();
                }
                pwProp.println(SEPARATOR);
            }
            final String propComment = Format.theFormat.getValueComments(name, "");
            if (!ps.kenFile) {
                if (propComment != null && propComment.length() != 0) {
                    pwProp.println();
                    pwProp.println(propComment);
                } else if (!prop.isType(UnicodeProperty.BINARY_MASK)) {
                    pwProp.println();
                    if (ps.roozbehFile) {
                        pwProp.println("# Property: " + name);
                    } else {
                        pwProp.println("# Property:\t" + name);
                    }
                }
            }

            if (DEBUG) {
                System.out.println(ps.toString());
            }

            if (!prop.isType(UnicodeProperty.BINARY_MASK)
                    && (ps.skipUnassigned != null || ps.skipValue != null)
                    && !ps.kenFile) {
                String v = ps.skipValue;
                if (v == null) {
                    v = ps.skipUnassigned;
                }
                if (!v.equals("<code point>")) {
                    final String v2 = prop.getFirstValueAlias(v);
                    if (UnicodeProperty.compareNames(v, v2) != 0) {
                        v = v + " (" + v2 + ")";
                    }
                }
                pwProp.println(ps.roozbehFile ? "#" : "");
                pwProp.println("#  All code points not explicitly listed for " + prop.getName());
                pwProp.println(
                        "#  have the value "
                                + v
                                + (ps.roozbehFile && v.equals("NA") ? " (not applicable)." : "."));
            }

            if (!ps.interleaveValues && prop.isType(UnicodeProperty.BINARY_MASK)) {
                if (DEBUG) {
                    System.out.println("Resetting Binary Values");
                }
                if (ps.skipValue == null) {
                    ps.skipValue = UCD_Names.NO;
                    ps.valueStyle = "none";
                }
                if (ps.nameStyle.equals("none")) {
                    ps.nameStyle = "long";
                }
            }

            if (ps.noLabel) {
                bf.setLabelSource(null);
            }
            if (ps.nameStyle.equals("none")) {
                bf.setPropName(null);
            } else if (ps.nameStyle.equals("short")) {
                bf.setPropName(prop.getFirstNameAlias());
            } else {
                bf.setPropName(name);
            }

            if (ps.kenFile) {
                writeKenFile(pwProp, bf, prop, ps);
            } else if (ps.interleaveValues) {
                writeInterleavedValues(pwProp, bf, prop, ps);
            } else if (prop.isType(UnicodeProperty.STRING_OR_MISC_MASK)
                    && !prop.getName().equals("Script_Extensions")) {
                writeStringValues(pwProp, bf, prop, ps);
                // } else if (prop.isType(UnicodeProperty.BINARY_MASK)) {
                //   writeBinaryValues(pw, bf, prop);
            } else {
                writeEnumeratedValues(pwProp, bf, unassigned, prop, ps);
            }
            pwFile.append(pwReal.getBuffer());
        }
        pwFile.println();
        pwFile.println("# EOF");
        pwFile.flush();
        udf.close();
    }

    private static void writeEnumeratedValues(
            PrintWriter pw,
            BagFormatter bf,
            UnicodeSet unassigned,
            UnicodeProperty prop,
            Format.PrintStyle ps) {
        if (DEBUG) {
            System.out.println("Writing Enumerated Values: " + prop.getName());
        }

        final boolean numeric = prop.getName().equals("Numeric_Value");
        bf.setValueSource(
                new UnicodeProperty.FilteredProperty(prop, Format.theFormat.hackMapFilter));

        Collection<String> aliases = prop.getAvailableValues();
        if (ps.orderByRangeStart) {
            if (DEBUG) {
                System.out.println("Reordering");
            }
            final TreeSet<String> temp2 = new TreeSet<String>(new RangeStartComparator(prop));
            temp2.addAll(aliases);
            aliases = temp2;
        }
        if (ps.roozbehFile) {
            aliases.removeIf(alias -> UnicodeProperty.compareNames(alias, ps.skipValue) == 0);
            if (!Format.theFormat
                    .propertyToOrderedValues
                    .get(prop.getName())
                    .containsAll(aliases)) {
                final TreeSet<String> missingAliases = new TreeSet<String>(aliases);
                missingAliases.removeAll(
                        Format.theFormat.propertyToOrderedValues.get(prop.getName()));
                throw new IllegalArgumentException(
                        "All values must be listed when using roozbehFile; missing "
                                + missingAliases);
            }
            aliases = Format.theFormat.propertyToOrderedValues.get(prop.getName());
        }
        if (ps.sortNumeric) {
            if (DEBUG) {
                System.out.println("Reordering");
            }
            final TreeSet<String> temp2 = new TreeSet<String>(NUMERIC_STRING_COMPARATOR);
            temp2.addAll(aliases);
            aliases = temp2;
        }
        if (DEBUG) {
            System.out.println("SPOT-CHECK: " + prop.getValue(0xE000));
        }

        UnicodeMap<Bidi_Class_Values> defaultBidiValues = null;
        UnicodeMap<East_Asian_Width_Values> defaultEaValues = null;
        UnicodeMap<Line_Break_Values> defaultLbValues = null;
        if (prop.getName().equals("Bidi_Class")) {
            VersionInfo versionInfo = Default.ucdVersionInfo();
            defaultBidiValues =
                    DefaultValues.BidiClass.forVersion(
                            versionInfo, DefaultValues.BidiClass.Option.OMIT_BN);
        } else if (prop.getName().equals("East_Asian_Width")) {
            VersionInfo versionInfo = Default.ucdVersionInfo();
            defaultEaValues = DefaultValues.EastAsianWidth.forVersion(versionInfo);
        } else if (prop.getName().equals("Line_Break")) {
            VersionInfo versionInfo = Default.ucdVersionInfo();
            defaultLbValues = DefaultValues.LineBreak.forVersion(versionInfo);
        }

        final String missing = ps.skipUnassigned != null ? ps.skipUnassigned : ps.skipValue;
        if (missing != null && !missing.equals(UCD_Names.NO)) {
            pw.println(ps.roozbehFile ? "#" : "");
            final String propName = bf.getPropName();
            //      if (propName == null) propName = "";
            //      else if (propName.length() != 0) propName = propName + "; ";
            // pw.println("# @missing: 0000..10FFFF; " + propName + missing);
            printDefaultValueComment(
                    pw, propName, prop, propName != null && propName.length() != 0, missing);
            if (prop.getName().equals("Bidi_Class")) {
                Bidi_Class_Values overallDefault = Bidi_Class_Values.forName(missing);
                writeEnumeratedMissingValues(pw, overallDefault, defaultBidiValues);
            } else if (prop.getName().equals("East_Asian_Width")) {
                East_Asian_Width_Values overallDefault = East_Asian_Width_Values.forName(missing);
                writeEnumeratedMissingValues(pw, overallDefault, defaultEaValues);
            } else if (prop.getName().equals("Line_Break")) {
                Line_Break_Values overallDefault = Line_Break_Values.forName(missing);
                writeEnumeratedMissingValues(pw, overallDefault, defaultLbValues);
            }
        }
        if (!ps.separateValues) {
            pw.println();
            pw.println(SEPARATOR.replace('=', '-'));
        }
        for (final Iterator<String> it = aliases.iterator(); it.hasNext(); ) {
            final String value = it.next();
            if (DEBUG) {
                System.out.println("Getting value " + value);
            }
            final UnicodeSet s = prop.getSet(value);
            final String valueComment = Format.theFormat.getValueComments(prop.getName(), value);

            if (DEBUG) {
                System.out.println(
                        "Value:\t"
                                + value
                                + "\t"
                                + prop.getFirstValueAlias(value)
                                + "\tskip:"
                                + ps.skipValue);
                System.out.println("Value Comment\t" + valueComment);
                System.out.println(s.toPattern(true));
            }

            final int totalSize = s.size();
            if (totalSize == 0) {
                if (!"Canonical_Combining_Class".equals(prop.getName())) {
                    System.out.println("\tSkipping Empty: " + prop.getName() + "=" + value);
                }
                continue;
            }

            if (UnicodeProperty.compareNames(value, ps.skipValue) == 0) {
                if (DEBUG) {
                    System.out.println("Skipping: " + value);
                }
                continue;
            }

            bf.setFullTotal(totalSize);
            if (UnicodeProperty.compareNames(value, ps.skipUnassigned) == 0) {
                if (DEBUG) {
                    System.out.println("Removing Unassigneds: " + value);
                }
                s.removeAll(unassigned);
            } else if (defaultBidiValues != null) {
                Bidi_Class_Values bidiValue = Bidi_Class_Values.forName(value);
                if (defaultBidiValues.containsValue(bidiValue)) {
                    // We assume that unassigned code points that have this value
                    // according to the props data also have this value according to the defaults.
                    // Otherwise we would need to intersect defaultBidiValues.keySet(bidiValue)
                    // with the unassigned set before removing from s.
                    s.removeAll(unassigned);
                }
            } else if (defaultEaValues != null) {
                East_Asian_Width_Values eaValue = East_Asian_Width_Values.forName(value);
                if (defaultEaValues.containsValue(eaValue)) {
                    // We assume that unassigned code points that have this value
                    // according to the props data also have this value according to the defaults.
                    // Otherwise we would need to intersect defaultEaValues.keySet(eaValue)
                    // with the unassigned set before removing from s.
                    s.removeAll(unassigned);
                }
            } else if (defaultLbValues != null) {
                Line_Break_Values lbValue = Line_Break_Values.forName(value);
                if (defaultLbValues.containsValue(lbValue)) {
                    // We assume that unassigned code points that have this value
                    // according to the props data also have this value according to the defaults.
                    // Otherwise we would need to intersect defaultEaValues.keySet(eaValue)
                    // with the unassigned set before removing from s.
                    s.removeAll(unassigned);
                }
            }

            // if (s.size() == 0) continue;
            // if (unassigned.containsAll(s)) continue; // skip if all unassigned
            // if (s.contains(0xD0000)) continue; // skip unassigned

            boolean nonLongValue = false;
            String displayValue = value;
            if (ps.valueStyle.equals("none")) {
                displayValue = null;
                nonLongValue = true;
            } else if (ps.valueStyle.equals("short")) {
                displayValue = prop.getFirstValueAlias(displayValue);
                nonLongValue = true;
                if (DEBUG) {
                    System.out.println("Changing value " + displayValue);
                }
            }
            if (ps.makeUppercase && displayValue != null) {
                displayValue = displayValue.toUpperCase(Locale.ENGLISH);
                if (DEBUG) {
                    System.out.println("Changing value2 " + displayValue);
                }
            }
            if (ps.makeFirstLetterLowercase && displayValue != null) {
                // NOTE: this is ok since we are only working in ASCII
                displayValue =
                        displayValue.substring(0, 1).toLowerCase(Locale.ENGLISH)
                                + displayValue.substring(1);
                if (DEBUG) {
                    System.out.println("Changing value2 " + displayValue);
                }
            }
            if (numeric) {
                displayValue += " ; ; " + dumbFraction(displayValue, "");
                if (DEBUG) {
                    System.out.println("Changing value3 " + displayValue);
                }
            }
            if (DEBUG) {
                System.out.println("Setting value " + displayValue);
            }
            bf.setValueSource(displayValue);

            if (!prop.isType(UnicodeProperty.BINARY_MASK)) {
                pw.println();
                if (ps.separateValues) {
                    pw.println(SEPARATOR);
                }
                if (nonLongValue) {
                    if (ps.separateValues) {
                        pw.println();
                    }
                    pw.println("# " + prop.getName() + "=" + value);
                }
            }

            if (valueComment != null) {
                pw.println();
                pw.println(valueComment);
            }
            //            if (false && ps.longValueHeading != null) {
            //                String headingValue = value;
            //                if (ps.longValueHeading == "ccc") {
            //                    headingValue = Utility.replace(value, "_", "");
            //                    final char c = headingValue.charAt(0);
            //                    if ('0' <= c && c <= '9') {
            //                        headingValue = "Other Combining Class";
            //                    }
            //                }
            //                pw.println();
            //                pw.println("# " + headingValue);
            //            }
            pw.println();
            // if (s.size() != 0)
            bf.setMergeRanges(ps.mergeRanges);
            bf.setShowTotal(!ps.roozbehFile);
            if (ps.roozbehFile) {
                bf.setRangeBreakSource(
                        ToolUnicodePropertySource.make(Default.ucdVersion()).getProperty("Block"));
            }
            bf.showSetNames(pw, s);
            if (DEBUG) {
                System.out.println(bf.showSetNames(s));
            }
        }
    }

    private static <T> void writeEnumeratedMissingValues(
            PrintWriter pw, T overallDefault, UnicodeMap<T> defaultValues) {
        VersionInfo versionInfo = Default.ucdVersionInfo();
        IndexUnicodeProperties props = IndexUnicodeProperties.make(versionInfo);
        UnicodeMap<Block_Values> blocks = props.loadEnum(UcdProperty.Block);
        Iterator<UnicodeMap.EntryRange<Block_Values>> blockIter = blocks.entryRanges().iterator();
        UnicodeMap.EntryRange<Block_Values> blockRange = null;

        for (UnicodeMap.EntryRange<T> range : defaultValues.entryRanges()) {
            if (range.value == overallDefault) {
                continue;
            }
            int start = range.codepoint;
            int end = range.codepointEnd;
            pw.println();
            // Skip blocks before this default-value range.
            while ((blockRange == null || blockRange.codepointEnd < start) && blockIter.hasNext()) {
                blockRange = blockIter.next();
            }
            // Print blocks that overlap with this default-value range.
            while (blockRange.codepoint <= end) {
                writeBlockOverlappingWithMissingRange(pw, start, end, blockRange);
                if (blockRange.codepointEnd > end || !blockIter.hasNext()) {
                    break;
                }
                blockRange = blockIter.next();
            }
            pw.printf("# @missing: %04X..%04X; %s\n", start, end, range.value);
        }
    }

    /**
     * Called by {@link org.unicode.tools.emoji.GenerateEmojiData} but implemented here to keep
     * similar functions next to each other.
     *
     * <p>Assumes that the overall default is "No".
     */
    // Note 2022-jul-13: Not actually used yet.
    // For reasons see the comments on DefaultValues.ExtendedPictographic.
    public static void writeBinaryMissingValues(
            PrintWriter pw, VersionInfo versionInfo, String propName, UnicodeSet defaultYesSet) {
        IndexUnicodeProperties props = IndexUnicodeProperties.make(versionInfo);
        UnicodeMap<Block_Values> blocks = props.loadEnum(UcdProperty.Block);
        Iterator<UnicodeMap.EntryRange<Block_Values>> blockIter = blocks.entryRanges().iterator();
        UnicodeMap.EntryRange<Block_Values> blockRange = null;

        for (UnicodeSet.EntryRange range : defaultYesSet.ranges()) {
            int start = range.codepoint;
            int end = range.codepointEnd;
            pw.println();
            // Skip blocks before this default-value range.
            while ((blockRange == null || blockRange.codepointEnd < start) && blockIter.hasNext()) {
                blockRange = blockIter.next();
            }
            // Print blocks that overlap with this default-value range.
            while (blockRange.codepoint <= end) {
                writeBlockOverlappingWithMissingRange(pw, start, end, blockRange);
                if (blockRange.codepointEnd > end || !blockIter.hasNext()) {
                    break;
                }
                blockRange = blockIter.next();
            }
            pw.printf("# @missing: %04X..%04X; %s; Yes\n", start, end, propName);
        }
    }

    private static void writeBlockOverlappingWithMissingRange(
            PrintWriter pw, int start, int end, UnicodeMap.EntryRange<Block_Values> blockRange) {
        if (blockRange.value != Block_Values.No_Block) {
            String partial =
                    blockRange.codepoint < start || blockRange.codepointEnd > end
                            ? " (partial)"
                            : "";
            pw.printf(
                    "# %04X..%04X %s%s\n",
                    blockRange.codepoint, blockRange.codepointEnd, blockRange.value, partial);
        }
    }

    // static NumberFormat nf = NumberFormat.getInstance();
    static Comparator<String> NUMERIC_STRING_COMPARATOR =
            new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    if (o1 == o2) {
                        return 0;
                    }
                    if (o1 == null) {
                        return -1;
                    }
                    if (o2 == null) {
                        return 1;
                    }
                    return Double.compare(Double.parseDouble(o1), Double.parseDouble(o2));
                }
            };
    /*
    private static void writeBinaryValues(
    PrintWriter pw,
    BagFormatter bf,
    UnicodeProperty prop) {
    if (DEBUG) System.out.println("Writing Binary Values: " + prop.getName());
    UnicodeSet s = prop.getSet(UCD_Names.True);
    bf.setValueSource(prop.getName());
    bf.showSetNames(pw, s);
    }
      */

    private static void writeKenFile(
            PrintWriter pw, BagFormatter bf, UnicodeProperty prop, PrintStyle ps) {
        if (DEBUG) {
            System.out.println("Writing Ken-style File: " + prop.getName());
        }
        printDefaultValueComment(
                pw,
                prop.getName(),
                prop,
                /*showPropName=*/ false,
                prop.getFirstValueAlias(ps.skipValue));
        var source = ToolUnicodePropertySource.make(Default.ucdVersion());
        UnicodeProperty generalCategory = source.getProperty("General_Category");
        UnicodeProperty block = source.getProperty("Block");
        // Ranges do not span blocks, even when characters are unassigned, except in ideographic
        // planes,
        // where Cn ranges are allowed to extend from the unassigned part of one block into the
        // No_Block void beyond.
        Map<String, String> ignoreBlocksInCJKVPlanes = new HashMap<String, String>();
        for (char ext = 'B'; ext <= 'I'; ++ext) {
            ignoreBlocksInCJKVPlanes.put("CJK_Ext_" + ext, "NB");
        }
        UnicodeProperty blockOrIdeographicPlane =
                new UnicodeProperty.FilteredProperty(
                        block, new UnicodeProperty.MapFilter(ignoreBlocksInCJKVPlanes));
        UnicodeSet omitted =
                generalCategory.getSet("Unassigned").retainAll(prop.getSet(ps.skipValue));
        bf.setValueSource(prop)
                .setRangeBreakSource(blockOrIdeographicPlane)
                .setMinSpacesBeforeSemicolon(1)
                .setValueWidthOverride(3)
                .setMinSpacesBeforeComment(0)
                .setRefinedLabelSource(generalCategory)
                .setCountWidth(7)
                .setMergeRanges(ps.mergeRanges)
                .setShowTotal(false)
                .showSetNames(pw, new UnicodeSet(0, 0x10FFFF).removeAll(omitted));
    }

    private static void writeInterleavedValues(
            PrintWriter pw, BagFormatter bf, UnicodeProperty prop, PrintStyle ps) {
        if (DEBUG) {
            System.out.println("Writing Interleaved Values: " + prop.getName());
        }
        pw.println();
        bf.setValueSource(new UnicodeProperty.FilteredProperty(prop, new RestoreSpacesFilter(ps)))
                .setNameSource(null)
                .setLabelSource(null)
                .setRangeBreakSource(null)
                .setShowCount(false)
                .setMergeRanges(ps.mergeRanges)
                .showSetNames(pw, new UnicodeSet(0, 0x10FFFF));
    }

    private static void writeStringValues(
            PrintWriter pw, BagFormatter bf, UnicodeProperty prop, PrintStyle ps) {
        if (DEBUG) {
            System.out.println("Writing String Values: " + prop.getName());
        }
        pw.println();
        final var shownSet = new UnicodeSet();
        if (ps.skipValue == null) {
            shownSet.addAll(UnicodeSet.ALL_CODE_POINTS);
        } else {
            for (int c = 0; c <= 0x10FFFF; ++c) {
                final String value = prop.getValue(c);
                final String skipValue =
                        ps.skipValue.equals("<code point>") ? Character.toString(c) : ps.skipValue;
                if (!value.equals(skipValue)) {
                    shownSet.add(c);
                }
            }
        }
        bf.setValueSource(prop)
                .setHexValue(true)
                .setMergeRanges(ps.mergeRanges)
                .showSetNames(pw, shownSet);
    }

    static class RangeStartComparator implements Comparator<String> {
        UnicodeProperty prop;
        CompareProperties.UnicodeSetComparator comp = new CompareProperties.UnicodeSetComparator();

        RangeStartComparator(UnicodeProperty prop) {
            this.prop = prop;
        }

        @Override
        public int compare(String o1, String o2) {
            final UnicodeSet s1 = prop.getSet(o1);
            final UnicodeSet s2 = prop.getSet(o2);
            if (true) {
                System.out.println(
                        "comparing "
                                + o1
                                + ", "
                                + o2
                                + s1.toPattern(true)
                                + "?"
                                + s2.toPattern(true)
                                + ", "
                                + comp.compare(s1, s2));
            }
            return comp.compare(s1, s2);
        }
    }

    static class RestoreSpacesFilter extends UnicodeProperty.StringFilter {
        String skipValue;
        /**
         * @param ps
         */
        public RestoreSpacesFilter(PrintStyle ps) {
            skipValue = ps.skipValue;
            if (skipValue == null) {
                skipValue = ps.skipUnassigned;
            }
        }

        @Override
        public String remap(String original) {
            // ok, because doesn't change length
            final String mod = Format.theFormat.hackMap.get(original);
            if (mod != null) {
                original = mod;
            }
            if (original.equals(skipValue)) {
                return null;
            }
            return original.replace('_', ' ');
        }
    }

    //    static Comparator<String> CASELESS_COMPARATOR = new Comparator<String>() {
    //        @Override
    //        public int compare(String s, String t) {
    //            return s.compareToIgnoreCase(t);
    //        }
    //    };

    public static void showDiff() throws IOException {
        final PrintWriter out =
                FileUtilities.openUTF8Writer(Settings.Output.GEN_DIR, "propertyDifference.txt");
        try {
            showDifferences(out, "4.0.1", "LB", "GC");
            showDifferences(out, "4.0.1", "East Asian Width", "LB");
            showDifferences(out, "4.0.1", "East Asian Width", "GC");
        } finally {
            out.close();
        }
    }

    public static void showAllDiff() throws IOException {
        final PrintWriter out =
                FileUtilities.openUTF8Writer(Settings.Output.GEN_DIR, "propertyDifference.txt");
        try {
            final UnicodeProperty.Factory fac = ToolUnicodePropertySource.make("4.0.1");
            final List<String> props =
                    fac.getAvailableNames(
                            (1 << UnicodeProperty.BINARY) | (1 << UnicodeProperty.ENUMERATED)
                            // | (1<<UnicodeProperty.CATALOG)
                            );
            final Set<String> skipList = new HashSet<String>();
            skipList.add("Age");
            skipList.add("Joining_Group");
            skipList.add("Canonical_Combining_Class");

            for (final Iterator<String> it = props.iterator(); it.hasNext(); ) {
                final String prop1 = it.next();
                for (final Iterator<String> it2 = props.iterator(); it2.hasNext(); ) {
                    final String prop2 = it2.next();
                    if (prop1.equals(prop2)) {
                        continue;
                    }
                    if (skipList.contains(prop2)) {
                        continue;
                    }
                    System.out.println(prop1 + " vs. " + prop2);
                    showDifferences(out, fac.getProperty(prop1), fac.getProperty(prop2), false);
                    out.flush();
                }
            }
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            out.close();
        }
    }

    static NumberFormat nf = NumberFormat.getIntegerInstance(Locale.ENGLISH);

    static void showDifferences(PrintWriter out, String version, String prop1, String prop2)
            throws IOException {
        final UnicodeProperty p1 = ToolUnicodePropertySource.make(version).getProperty(prop1);
        final UnicodeProperty p2 = ToolUnicodePropertySource.make(version).getProperty(prop2);
        showDifferences(out, p1, p2, true);
    }

    static void showDifferences(
            PrintWriter out, UnicodeProperty p1, UnicodeProperty p2, boolean doOverlaps)
            throws IOException {
        // out.println("Comparing " + p1.getName() + " and " + p2.getName());
        System.out.println("Comparing " + p1.getName() + " and " + p2.getName());
        final String pn1 = '$' + p1.getName();
        final String pn2 = '$' + p2.getName();
        final UnicodeSet intersection = new UnicodeSet();
        final UnicodeSet disjoint = new UnicodeSet();
        final String skip1 = p1.getValue(0xEFFFD);
        final String skip2 = p2.getValue(0xEFFFD);
        main:
        for (final Iterator<String> it1 = p1.getAvailableValues().iterator(); it1.hasNext(); ) {
            final String v1 = it1.next();
            if (v1.equals(skip1)) {
                continue;
            }
            final UnicodeSet s1 = p1.getSet(v1);
            if (s1.size() == 0) {
                continue;
            }
            final String pv1 = pn1 + (v1.equals(UCD_Names.YES) ? "" : ":" + v1);
            // v1 += " (" + p1.getFirstValueAlias(v1) + ")";
            System.out.println(v1);
            // out.println();
            // out.println(v1 + " [" + nf.format(s1.size()) + "]");

            // create some containers so that the output is organized reasonably
            String contains = "";
            String overlaps = "";
            final UnicodeSet containsSet = new UnicodeSet();
            final Set<String> overlapsSet = new TreeSet<String>();
            for (final Iterator<String> it2 = p2.getAvailableValues().iterator(); it2.hasNext(); ) {
                final String v2 = it2.next();
                if (v2.equals(skip2)) {
                    continue;
                }
                final UnicodeSet s2 = p2.getSet(v2);
                if (s2.size() == 0) {
                    continue;
                }
                // v2 += "(" + p2.getFirstValueAlias(v2) + ")";
                // v2 = p2.getFirstValueAlias(v2);
                final String pv2 = pn2 + (v2.equals(UCD_Names.YES) ? "" : ":" + v2);
                if (s1.containsNone(s2)) {
                    continue;
                }
                if (s1.equals(s2)) {
                    out.println(pv1 + "\t= " + pv2);
                    continue main; // since they are partitions, we can stop here
                } else if (s2.containsAll(s1)) {
                    // out.println(pv1 + "\t\u2282 " + pv2);
                    continue main; // partition, stop
                } else if (s1.containsAll(s2)) {
                    if (contains.length() != 0) {
                        contains += " ";
                    }
                    contains += pv2;
                    containsSet.addAll(s2);
                    if (containsSet.size() == s1.size()) {
                        break;
                    }
                } else if (doOverlaps) { // doesn't contain, isn't contained
                    if (overlaps.length() != 0) {
                        overlaps += "\n\t";
                    }
                    intersection.clear().addAll(s2).retainAll(s1);
                    disjoint.clear().addAll(s1).removeAll(s2);
                    overlaps +=
                            "\u2283 "
                                    + v2
                                    + " ["
                                    + nf.format(intersection.size())
                                    + "]"
                                    + " \u2285 "
                                    + v2
                                    + " ["
                                    + nf.format(disjoint.size())
                                    + "]";
                    overlapsSet.add(v2);
                }
            }
            if (contains.length() != 0) {
                out.println(
                        pv1
                                + (containsSet.size() == s1.size() ? "\t= " : "\t\u2283 ")
                                + "["
                                + contains
                                + "]");
            }
            if (overlaps.length() != 0) {
                out.println("\t" + overlaps);
            }
            //                if (false && overlapsSet.size() != 0) {
            //                    out.println("\t\u2260\u2284\u2285");
            //                    for (final Iterator<String> it3 = overlapsSet.iterator();
            // it3.hasNext();) {
            //                        final String v3 = it3.next();
            //                        final UnicodeSet s3 = p2.getSet(v3);
            //                        out.println("\t" + v3);
            //                        bf.showSetDifferences(out,v1,s1,v3,s3);
            //                    }
            //                }
        }
    }

    static class UnicodeDataHack extends UnicodeLabel {
        // private final UnicodeProperty.Factory factory;
        private final UnicodeProperty name;
        private final UnicodeProperty bidiMirrored;
        private final UnicodeProperty numericValue;
        private final UnicodeProperty numericType;
        private final UnicodeProperty decompositionValue;
        private final UnicodeProperty decompositionType;
        private final UnicodeProperty bidiClass;
        private final UnicodeProperty combiningClass;
        private final UnicodeProperty category;
        private final UnicodeProperty unicode1Name;
        private final UnicodeProperty simpleUppercaseMapping;
        private final UnicodeProperty simpleLowercaseMapping;
        private final UnicodeProperty simpleTitlecaseMapping;
        private final UnicodeProperty block;
        private final Map<String, String> rangeBlocks;

        UnicodeDataHack(UnicodeProperty.Factory factory) {
            // this.factory = factory;
            name = factory.getProperty("Name");
            category = factory.getProperty("General_Category");
            combiningClass = factory.getProperty("Canonical_Combining_Class");
            bidiClass = factory.getProperty("Bidi_Class");
            decompositionType = factory.getProperty("Decomposition_Type");
            decompositionValue = factory.getProperty("Decomposition_Mapping");
            numericType = factory.getProperty("Numeric_Type");
            numericValue = factory.getProperty("Numeric_Value");
            bidiMirrored = factory.getProperty("Bidi_Mirrored");
            unicode1Name = factory.getProperty("Unicode_1_Name");
            simpleUppercaseMapping = factory.getProperty("Simple_Uppercase_Mapping");
            simpleLowercaseMapping = factory.getProperty("Simple_Lowercase_Mapping");
            simpleTitlecaseMapping = factory.getProperty("Simple_Titlecase_Mapping");
            block = factory.getProperty("Block");

            rangeBlocks = new HashMap<>();
            for (char c = 'A'; c <= 'Z'; ++c) {
                rangeBlocks.put(
                        "CJK_Unified_Ideographs_Extension_" + c, "CJK Ideograph Extension " + c);
            }
            rangeBlocks.put("CJK_Unified_Ideographs", "CJK Ideograph");
            rangeBlocks.put("Hangul_Syllables", "Hangul Syllable");
            rangeBlocks.put("High_Surrogates", "Non Private Use High Surrogate");
            rangeBlocks.put("High_Private_Use_Surrogates", "Private Use High Surrogate");
            rangeBlocks.put("Low_Surrogates", "Low Surrogate");
            rangeBlocks.put("Private_Use_Area", "Private Use");
            rangeBlocks.put("Tangut", "Tangut Ideograph");
            rangeBlocks.put("Tangut_Supplement", "Tangut Ideograph Supplement");
            rangeBlocks.put("Supplementary_Private_Use_Area_A", "Plane 15 Private Use");
            rangeBlocks.put("Supplementary_Private_Use_Area_B", "Plane 16 Private Use");
        }

        @Override
        public int getMaxWidth(boolean isShort) {
            return 1729;
        }

        @Override
        public String getValue(int codepoint, boolean isShort) {
            final String gc = category.getValue(codepoint, true);
            if (gc == "Cn") {
                return null;
            }
            final String blk = block.getValue(codepoint);
            final boolean isHangulSyllable = blk.equals("Hangul_Syllables");
            final String[] fields = new String[15];
            Arrays.fill(fields, "");

            if (rangeBlocks.containsKey(blk)) {
                fields[1] =
                        "<" + rangeBlocks.get(blk) + ", " + BagFormatter.RANGE_PLACEHOLDER + ">";
            } else {
                fields[1] = name.getValue(codepoint);
                if (fields[1].startsWith("<control")) {
                    fields[1] = "<control>";
                }
            }

            fields[2] = gc;
            fields[3] = combiningClass.getValue(codepoint, true);
            fields[4] = bidiClass.getValue(codepoint, true);

            // Field 5.
            final String dt = decompositionType.getValue(codepoint);
            if (!isHangulSyllable && !dt.equals("None")) {
                if (!dt.equals("Canonical")) {
                    fields[5] = "<" + dt.toLowerCase().replace("nobreak", "noBreak") + "> ";
                }
                fields[5] += Utility.hex(decompositionValue.getValue(codepoint));
            }

            final String nt = numericType.getValue(codepoint);
            if (!nt.equals("None") && !fields[1].startsWith("<CJK")) {
                final String nv =
                        dumbFraction(numericValue.getValue(codepoint), name.getValue(codepoint));
                if (nt.equals("Decimal")) {
                    fields[6] = fields[7] = fields[8] = nv;
                } else if (nt.equals("Digit")) {
                    fields[7] = fields[8] = nv;
                } else if (nt.equals("Numeric")) {
                    fields[8] = nv;
                }
            }

            fields[9] = bidiMirrored.getValue(codepoint, true);

            fields[10] = unicode1Name.getValue(codepoint);
            // Field 11 is ISO_Comment; obsolete, deprecated, and stabilized; always empty.
            final String suc = simpleUppercaseMapping.getValue(codepoint);
            if (!suc.equals(Character.toString(codepoint))) {
                fields[12] = Utility.hex(suc);
            }
            final String slc = simpleLowercaseMapping.getValue(codepoint);
            if (!slc.equals(Character.toString(codepoint))) {
                fields[13] = Utility.hex(slc);
            }
            final String stc = simpleTitlecaseMapping.getValue(codepoint);
            if (!stc.equals(Character.toString(codepoint)) || !stc.equals(suc)) {
                fields[14] = Utility.hex(stc);
            }
            return String.join(";", Arrays.copyOfRange(fields, 1, fields.length));
        }
    }

    /*
    static class PropertySymbolTable implements SymbolTable {
    static boolean DEBUG = false;
    UnicodeProperty.Factory factory;
    //static Matcher identifier = Pattern.compile("([:letter:] [\\_\\-[:letter:][:number:]]*)").matcher("");

     PropertySymbolTable (UnicodeProperty.Factory factory) {
     this.factory = factory;
     }

     public char[] lookup(String s) {
     if (DEBUG) System.out.println("\tLooking up " + s);
     int pos = s.indexOf('=');
     if (pos < 0) return null; // should never happen
     UnicodeProperty prop = factory.getProperty(s.substring(0,pos));
     if (prop == null) {
     throw new IllegalArgumentException("Invalid Property: " + s + "\nUse "
     + showSet(factory.getAvailableNames()));
     }
     String value = s.substring(pos+1);
     UnicodeSet set = prop.getSet(value);
     if (set.size() == 0) {
     throw new IllegalArgumentException("Empty Property-Value: " + s + "\nUse "
     + showSet(prop.getAvailableValues()));
     }
     if (DEBUG) System.out.println("\tReturning " + set.toPattern(true));
     return set.toPattern(true).toCharArray(); // really ugly
     }

     private String showSet(List list) {
     StringBuffer result = new StringBuffer("[");
     boolean first = true;
     for (Iterator it = list.iterator(); it.hasNext();) {
     if (!first) result.append(", ");
     else first = false;
     result.append(it.next().toString());
     }
     result.append("]");
     return result.toString();
     }

     public UnicodeMatcher lookupMatcher(int ch) {
     return null;
     }

     public String parseReference(String text, ParsePosition pos, int limit) {
     if (DEBUG) System.out.println("\tParsing <" + text.substring(pos.getIndex(),limit) + ">");
     int start = pos.getIndex();
     int i = getIdentifier(text, start, limit);
     if (i == start) return null;
     String prop = text.substring(start, i);
     String value = UCD_Names.True;
     if (i < limit) {
     int cp = text.charAt(i);
     if (cp == ':' || cp == '=') {
     int j = getIdentifier(text, i+1, limit);
     value = text.substring(i+1, j);
     i = j;
     }
     }
     pos.setIndex(i);
     if (DEBUG) System.out.println("\tParsed <" + prop + ">=<" + value + ">");
     return prop + '=' + value;
     }

     private int getIdentifier(String text, int start, int limit) {
     if (DEBUG) System.out.println("\tGetID <" + text.substring(start,limit) + ">");
     int cp = 0;
     int i;
     for (i = start; i < limit; i += UTF16.getCharCount(cp)) {
     cp = UTF16.charAt(text, i);
     if (!com.ibm.icu.lang.UCharacter.isUnicodeIdentifierPart(cp)) {
     break;
     }
     }
     if (DEBUG) System.out.println("\tGotID <" + text.substring(start,i) + ">");
     return i;
     }

     };

     /* getCombo(UnicodeProperty.Factory factory, String line) {
      UnicodeSet result = new UnicodeSet();
      String[] pieces = Utility.split(line, '+');
      for (int i = 0; i < pieces.length; ++i) {
      String[] parts = Utility.split(pieces[i],':');
      String prop = parts[0].trim();
      String value = UCD_Names.True;
      if (parts.length > 1) value = parts[1].trim();
      UnicodeProperty p = factory.getProperty(prop);
      result.addAll(p.getSet(value));
      }
      return result;
      }
      */

    // quick and dirty fractionator
    private static String dumbFraction(String toolValue, String name) {
        if (toolValue.indexOf('.') < 0) {
            return toolValue;
        }
        if (toolValue.equals("0.0")) {
            return "0";
        }
        if (toolValue.endsWith(".0")) {
            return toolValue.substring(0, toolValue.length() - 2);
        }
        final double value = Double.parseDouble(toolValue);
        Map<Integer, String> names = new TreeMap<>();
        names.put(1, "n/a");
        names.put(2, "HALF");
        names.put(3, "THIRD");
        names.put(4, "FOURTH");
        names.put(5, "FIFTH");
        names.put(6, "SIXTH");
        names.put(7, "SEVENTH");
        names.put(8, "EIGHTH");
        names.put(9, "NINTH");
        names.put(10, "TENTH");
        names.put(12, "TWELFTH");
        names.put(16, "SIXTEENTH");
        names.put(20, "TWENTIETH");
        names.put(40, "FORTIETH");
        names.put(32, "THIRTY-SECOND");
        names.put(64, "SIXTY-FOURTH");
        names.put(80, "EIGHTIETH");
        names.put(160, "ONE-HUNDRED-AND-SIXTIETH");
        names.put(320, "THREE-HUNDRED-AND-TWENTIETH");
        List<Integer> denominators = new ArrayList<>(names.keySet());
        // Prefer denominators that are in the name, and among those prefer
        // those with the longest name (so that we use sixty-fourths, not
        // fourths, when both work).  Otherwise prefer smaller denominators.
        denominators.sort(
                (m, n) -> {
                    final boolean m_in_name = name.contains(names.get(m));
                    final boolean n_in_name = name.contains(names.get(n));
                    if (m_in_name != n_in_name) {
                        return Boolean.compare(n_in_name, m_in_name);
                    }
                    if (m_in_name) {
                        return Integer.compare(names.get(n).length(), names.get(m).length());
                    } else {
                        return m.compareTo(n);
                    }
                });
        for (int i : denominators) {
            final double numerator = value * i;
            final long rounded = Math.round(numerator);
            if (Math.abs(numerator - rounded) < 0.000001d) {
                if (i == 1) {
                    return rounded + "";
                }
                return rounded + "/" + i;
            }
        }
        return toolValue;
    }
}

/*
static class OrderedMap {
HashMap map = new HashMap();
ArrayList keys = new ArrayList();
void put(Object o, Object t) {
map.put(o,t);
keys.add(o);
}
List keyset() {
return keys;
}
}
*/
