/**
 * ****************************************************************************** Copyright (C)
 * 1996-2001, International Business Machines Corporation and * others. All Rights Reserved. *
 * ******************************************************************************
 *
 * <p>$Source: /home/cvsroot/unicodetools/org/unicode/text/UCD/GenerateBreakTest.java,v $
 *
 * <p>******************************************************************************
 */
package org.unicode.text.UCD;

import com.ibm.icu.impl.UnicodeMap;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.props.UnicodeProperty;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.UnicodeDataFile;
import org.unicode.text.utility.Utility;
import org.unicode.text.utility.UtilityBase;
import org.unicode.tools.Segmenter;
import org.unicode.tools.Segmenter.Builder;

public abstract class GenerateBreakTest implements UCD_Types {
    private static final String DEBUG_STRING = "\u0001\u0061\u2060";
    private static final boolean DEBUG_RULE_REPLACEMENT = true;

    static final IndexUnicodeProperties IUP = IndexUnicodeProperties.make(Default.ucdVersion());
    // hack for now
    static final String sampleEmoji = "🛑";
    static final String sampleEXP = "✁";
    static final String sampleEBase = "👶";
    static final String sampleEMod = "🏿";
    static final String zwj = "\u200D";
    static final String sampleMn = "\u0308";

    private static final String DOCTYPE =
            "<!DOCTYPE HTML PUBLIC '-//W3C//DTD HTML 4.01 Transitional//EN' 'http://www.w3.org/TR/html4/loose.dtd'>";

    static boolean DEBUG = false;
    static final boolean SHOW_TYPE = false;
    UCD ucd;
    Normalizer nfd;
    Normalizer nfkd;

    Segmenter segmenter;
    UnicodeMap<String> partition;
    UnicodeProperty prop;

    protected final Segmenter seg;

    // ====================== Main ===========================

    public static void main(String[] args) throws IOException {
        if (args.length > 0 && Arrays.asList(args).contains("CLDR_BREAK")) {
            throw new IllegalArgumentException(
                    "obsolete command-line argument CLDR_BREAK: set -DCLDR=true instead");
        }
        System.out.println(
                "Remember to add length marks (half & full) and other punctuation for sentence, with FF61");
        UCD ucd = Default.ucd();
        new GenerateGraphemeBreakTest(ucd, Segmenter.Target.FOR_UCD).run();
        new GenerateWordBreakTest(ucd, Segmenter.Target.FOR_UCD).run();
        new GenerateLineBreakTest(ucd, Segmenter.Target.FOR_UCD).run();
        new GenerateSentenceBreakTest(ucd, Segmenter.Target.FOR_UCD).run();
    }

    GenerateBreakTest(UCD ucd, Segmenter seg) {
        this.ucd = ucd;
        nfd = new Normalizer(UCD_Types.NFD, ucd.getVersion());
        nfkd = new Normalizer(UCD_Types.NFKD, ucd.getVersion());
        this.seg = seg;
    }

    static final ToolUnicodePropertySource unicodePropertySource =
            ToolUnicodePropertySource.make(Default.ucdVersion());

    Set<String> labels = new HashSet<String>();

    public static boolean onCodepointBoundary(String s, int offset) {
        if (offset < 0 || offset > s.length()) {
            return false;
        }
        if (offset == 0 || offset == s.length()) {
            return true;
        }
        if (UTF16.isLeadSurrogate(s.charAt(offset - 1))
                && UTF16.isTrailSurrogate(s.charAt(offset))) {
            return false;
        }
        return true;
    }

    // finds the first base character, or the first character if there is no base
    public int findFirstBase(String source, int start, int limit) {
        int cp;
        for (int i = start; i < limit; i += UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(source, i);
            final byte cat = ucd.getCategory(cp);
            if (((1 << cat) & MARK_MASK) != 0) {
                continue;
            }
            return cp;
        }
        return UTF16.charAt(source, start);
    }

    // quick & dirty routine
    static String insertEverywhere(String source, String insertion, GenerateBreakTest breaker) {
        String result = insertion;
        for (int i = 0; i < source.length(); ++i) {
            result += source.charAt(i);
            if (breaker.isBreak(source, i)) {
                result += insertion;
            }
        }
        return result + insertion;
    }

    static void checkDecomps(UCD ucd) {
        final UCDProperty[] INFOPROPS = {
            UnifiedProperty.make(CATEGORY), UnifiedProperty.make(LINE_BREAK)
        };
        final GenerateBreakTest[] tests = {
            new GenerateGraphemeBreakTest(ucd, Segmenter.Target.FOR_UCD),
            new GenerateWordBreakTest(ucd, Segmenter.Target.FOR_UCD),
            new GenerateLineBreakTest(ucd, Segmenter.Target.FOR_UCD),
        };
        tests[0].isBreak("\u0300\u0903", 1);
        final Normalizer nfd = new Normalizer(UCD_Types.NFD, ucd.getVersion());

        System.out.println("Check Decomps");

        for (final GenerateBreakTest test2 : tests) {
            for (int i = 0; i < 0x10FFFF; ++i) {
                if (!ucd.isAllocated(i)) {
                    continue;
                }
                if (UCD.isHangulSyllable(i)) {
                    continue;
                }
                if (nfd.isNormalized(i)) {
                    continue;
                }
                final String decomp = nfd.normalize(i);
                boolean shown = false;
                final String test = decomp;
                for (int j = 1; j < test.length(); ++j) {
                    if (test2.isBreak(test, j)) {
                        if (!shown) {
                            System.out.println(showData(ucd, UTF16.valueOf(i), INFOPROPS, "\n\t"));
                            System.out.println(" => " + showData(ucd, decomp, INFOPROPS, "\n\t"));
                            shown = true;
                        }
                        System.out.println(j + ": " + test2.fileName);
                    }
                }
            }
        }
    }

    static String showData(UCD ucd, String source, UCDProperty[] props, String separator) {
        final StringBuffer result = new StringBuffer();
        int cp;
        for (int i = 0; i < source.length(); i += UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(source, i);
            if (i != 0) {
                result.append(separator);
            }
            result.append(ucd.getCodeAndName(cp));
            for (final UCDProperty prop2 : props) {
                result.append(", ");
                result.append(prop2.getPropertyName(SHORT))
                        .append('=')
                        .append(prop2.getValue(cp, SHORT));
            }
        }
        return result.toString();
    }

    void showSet(String title, UnicodeSet set) {
        System.out.println(title + ": " + set.toPattern(true));
        Utility.showSetNames("", set, false, ucd);
    }

    // determines if string is of form Base NSM*
    boolean isBaseNSMStar(String source) {
        int cp;
        int status = 0;
        for (int i = 0; i < source.length(); i += UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(source, i);
            final byte cat = ucd.getCategory(cp);
            final int catMask = 1 << cat;
            switch (status) {
                case 0:
                    if ((catMask & BASE_MASK) == 0) {
                        return false;
                    }
                    status = 1;
                    break;
                case 1:
                    if ((catMask & NONSPACING_MARK_MASK) == 0) {
                        return false;
                    }
                    break;
            }
        }
        return true;
    }

    UnicodeSet getClosure(UnicodeSet source) {
        final UnicodeSet result = new UnicodeSet(source);
        for (int i = 0; i < 0x10FFFF; ++i) {
            if (!ucd.isAllocated(i)) {
                continue;
            }
            if (nfkd.isNormalized(i)) {
                continue;
            }
            final String decomp = nfkd.normalize(i);
            if (source.containsAll(decomp)) {
                result.add(i);
            }
        }
        return result;
    }

    void generateTerminalClosure() {
        final UnicodeSet midLetterSet =
                new UnicodeSet(
                        "[\u0027\u002E\u003A\u00AD\u05F3\u05F4\u2019\uFE52\uFE55\uFF07\uFF0E\uFF1A]");

        final UnicodeSet ambigSentPunct = new UnicodeSet("[\u002E\u0589\u06D4]");

        final UnicodeSet sentPunct =
                new UnicodeSet(
                        "[\u0021\u003F\u0387\u061F\u0964\u203C\u203D\u2048\u2049"
                                + "\u3002\ufe52\ufe57\uff01\uff0e\uff1f\uff61]");

        final UnicodeSet terminals =
                UnifiedBinaryProperty.make(BINARY_PROPERTIES | Terminal_Punctuation).getSet();
        final UnicodeSet extras = getClosure(terminals).removeAll(terminals);
        System.out.println("Current Terminal_Punctuation");
        Utility.showSetNames("", terminals, true, ucd);

        System.out.println("Missing Terminal_Punctuation");
        Utility.showSetNames("", extras, true, ucd);

        System.out.println("midLetterSet");
        System.out.println(midLetterSet.toPattern(true));
        Utility.showSetNames("", midLetterSet, true, ucd);

        System.out.println("ambigSentPunct");
        System.out.println(ambigSentPunct.toPattern(true));
        Utility.showSetNames("", ambigSentPunct, true, ucd);

        System.out.println("sentPunct");
        System.out.println(sentPunct.toPattern(true));
        Utility.showSetNames("", sentPunct, true, ucd);
    }

    // ============================

    protected String currentRule;
    protected String fileName;
    protected String propertyName;
    protected List<String> samples =
            new ArrayList<String>(); // should have one per property value, for the cross chart and
    // before+after test
    protected List<String> extraSamples =
            new ArrayList<String>(); // extras that are used in before+after test
    protected List<String> extraSingleSamples =
            new ArrayList<
                    String>(); // extras that are just added straight, no before+after, and also
    // appear on charts
    protected Set<String> extraTestSamples =
            new LinkedHashSet<>(); // extras that are just added to tests, not to charts
    protected int tableLimit = -1;

    protected int[] skippedSamples = new int[100];
    protected boolean didSkipSamples = false;

    private final String[] ruleList = new String[100];
    private int ruleListCount = 0;
    protected boolean collectingRules = false;
    protected boolean needsFullBreakSample = true;
    protected Map<String, String> variables;

    public void setRule(String rule) {
        if (collectingRules) {
            ruleList[ruleListCount++] = rule;
        }
        currentRule = rule;
    }

    public String getRule() {
        return currentRule;
    }

    public void run() throws IOException {
        findSamples();

        // test individual cases
        // printLine(out, samples[LB_ZW], "", samples[LB_CL]);
        // printLine(out, samples[LB_ZW], " ", samples[LB_CL]);

        boolean forCLDR = seg.target == Segmenter.Target.FOR_CLDR;
        String path = "UCD/" + ucd.getVersion() + '/' + (forCLDR ? "cldr/" : "auxiliary/");
        String extraPath = "UCD/" + ucd.getVersion() + "/extra/";
        String outFilename = fileName + "BreakTest";
        if (forCLDR) {
            outFilename = outFilename + "-cldr";
        }
        final UnicodeDataFile fc =
                UnicodeDataFile.openHTMLAndWriteHeader(path, outFilename)
                        .setSkipCopyright(Settings.SKIP_COPYRIGHT);
        final PrintWriter out = fc.out;

        out.println(DOCTYPE);
        out.println(
                "<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'>");
        out.println("<title>" + fileName + " Break Chart</title>");
        out.println("<style type='text/css'>");
        out.println("td, th { vertical-align: top }");
        out.println("</style></head>");

        out.println("<body bgcolor='#FFFFFF'>");
        out.println("<h2>" + propertyName + " Chart</h2>");
        out.println("<p><b>Unicode Version:</b> " + ucd.getVersion() + "</p>");
        if (!Settings.BUILD_FOR_COMPARE) {
            out.println("<p><b>Date:</b> " + Default.getDate() + "</p>");
        }
        out.println(
                "<p>This page illustrates the application of the "
                        + propertyName
                        + " specification. "
                        + "The material here is informative, not normative.</p> "
                        + "<p>The first chart shows where breaks would appear between different sample characters or strings. "
                        + "The sample characters are chosen mechanically to represent the different properties used by the specification.</p>"
                        + "<p>Each cell shows the break-status for the position between the character(s) in its row header and the character(s) in its column header. "
                        + (fileName.equals("Line")
                                ? "The symbol × indicates a prohibited break, even with intervening spaces;"
                                        + " the ÷ symbol indicates a (direct) break; the symbol ∻ indicates a"
                                        + " break only in the presence of an intervening space (an indirect break)."
                                : "The × symbol indicates no break, while the ÷ symbol indicates a break. ")
                        + "The cells with ×"
                        + (fileName.equals("Line") ? " or ∻ " : "")
                        + " are also shaded to make it easier to scan the table. "
                        + "For example, in the cell at the intersection of the row headed by “CR” and the column headed by “LF”, there is a × symbol, "
                        + "indicating that there is no break between CR and LF.</p>");
        out.print("<p>");
        if (fileName.equals("Grapheme") || fileName.equals("Word")) {
            out.print(
                    "After the heavy blue line in the table are additional rows, either with different sample characters or for sequences"
                            + (fileName.equals("Word") ? ", such as “ALetter MidLetter”. " : ". "));
        }
        out.println(
                "</p><p>In the row and column headers of the <a href='#table'>Table</a>, "
                        + "in the <a href='#rules'>Rules</a>, "
                        + "when hovering over characters in the <a href='#samples'>Samples</a>, "
                        + "and in the comments in the associated list of test cases <a href='"
                        + outFilename
                        + ".txt'>"
                        + outFilename
                        + ".txt</a>:</p>");
        out.println("<ol><li>The following sets are used:<ul>");
        final var mainProperty = IUP.getProperty(propertyName);
        for (var entry : variables.entrySet()) {
            final String variable = entry.getKey().substring(1);
            final String value = entry.getValue();
            if (variable.equals("sot")
                    || variable.equals("eot")
                    || variable.equals("Any")
                    || mainProperty
                            .getSet(variable)
                            .equals(
                                    new UnicodeSet(
                                            value, new ParsePosition(0), IUP.getXSymbolTable()))) {
                continue;
            }
            out.println("<li>");
            out.println(variable);
            out.println("=");
            out.println(value);
            out.println("</li>");
        }
        out.println("</ul></li>");
        out.println(
                "<li>Any other name that is a short property value alias for the "
                        + propertyName
                        + " property represents the set of characters with that property, e.g., LF=\\p{"
                        + propertyName
                        + "=LF}.</li>");
        out.println(
                "<li>The aforementioned sets are used to generate a partition of the code space in"
                        + " classes named X_Y for the intersection of X and Y and XmY for the"
                        + "  complement of Y in X.</li></ol>");
        out.println(
                "<p>Note that the resulting partition may be finer than needed for the algorithm.");
        if (propertyName.equals("Line_Break")) {
            out.println(
                    "For instance, characters in CMorig_EastAsian and CMorigmEastAsian behave"
                            + " identically in line breaking, as characters in these classes are"
                            + " remapped before EastAsian is used in the rules.");
        }
        out.println("</p>");
        out.print(
                "<p>If your browser handles titles (tooltips), then hovering the mouse over the row header will show a sample character of that type. "
                        + "Hovering over a column header will show the sample character, plus its abbreviated general category and script. "
                        + "Hovering over the intersected cells shows the rule number that produces the break-status. "
                        + "For example, hovering over the cell at the intersection of ");
        switch (fileName) {
            case "Line":
                out.print("H3 and JT shows ×, with the rule 26.03. ");
                break;
            case "Grapheme":
                out.print("LVT and T shows ×, with the rule 8.0. ");
                break;
            case "Word":
                out.print("ExtendNumLet and ALetter shows ×, with the rule 13.2. ");
                break;
            case "Sentence":
                out.print("ATerm and Close shows ×, with the rule 9.0. ");
                break;
        }
        out.print("Checking below the table, ");
        switch (fileName) {
            case "Line":
                out.print("rule 26.03 is “JT | H3 × JT”");
                break;
            case "Grapheme":
                out.print("rule 8.0 is “( LVT | T) × T”");
                break;
            case "Word":
                out.print("rule 13.2 is “ExtendNumLet × (AHLetter | Numeric | Katakana)”");
                break;
            case "Sentence":
                out.print("rule 9.0 is “SATerm Close* × ( Close | Sp | ParaSep )”");
                break;
        }
        out.println(
                ", which is the one that applies to that case. "
                        + "Note that a rule is invoked only when no lower-numbered rules have applied.</p>");
        generateTable(out);

        out.println(
                "<hr width='50%'>\n"
                        + "<div align='center'>\n"
                        + "<center>\n"
                        + "<table cellspacing='0' cellpadding='0' border='0'>\n"
                        + "<tr>\n"
                        + "<td><a href='https://www.unicode.org/copyright.html'>\n"
                        + "<img src='https://www.unicode.org/img/hb_notice.gif' border='0' "
                        + "alt='Access to Copyright and terms of use' width='216' height='50'></a></td>\n"
                        + "</tr>\n"
                        + "</table>\n"
                        + "</center>\n"
                        + "</div>");
        for (int i = 0; i < 50; ++i) {
            out.println("<br>"); // leave blank lines so scroll-to-top works.
        }
        fc.close();

        generateTest(false, path, outFilename, propertyName);
        generateCppOldMonkeys(extraPath, outFilename);
        generateJavaOldMonkeys(extraPath, outFilename);
    }

    private void generateCppOldMonkeys(String path, String outFilename) throws IOException {
        final UnicodeDataFile fc = UnicodeDataFile.openAndWriteHeader(path, outFilename + ".cpp");
        final PrintWriter out = fc.out;
        out.println();
        out.println("####### Instructions ##################################");
        out.println("# Copy the following lines into rbbitst.cpp in ICU4C, #");
        out.println(
                "# in the constructor of RBBIMeowMonkey, replacing the #"
                        .replace("Meow", outFilename.substring(0, 4).replace("Graph", "Char")));
        out.println("# existing block of generated code.                   #");
        out.println("#######################################################");
        out.println();
        out.println("    // --- NOLI ME TANGERE ---");
        out.println("    // Generated by GenerateBreakTest.java in the Unicode tools.");
        for (Segmenter.Builder.NamedRefinedSet part : segmenter.getPartitionDefinition()) {
            out.println(
                    "    partition.emplace_back(\""
                            + part.getName()
                            + "\", UnicodeSet(uR\"("
                            + part.getDefinition()
                            + ")\", status));");
        }
        out.println();
        for (Segmenter.SegmentationRule rule : segmenter.getRules()) {
            out.println("    rules.push_back(" + rule.toCppOldMonkeyString() + ");");
        }
        out.println("    // --- End of generated code. ---");
        fc.close();
    }

    private void generateJavaOldMonkeys(String path, String outFilename) throws IOException {
        final UnicodeDataFile fc = UnicodeDataFile.openAndWriteHeader(path, outFilename + ".java");
        final PrintWriter out = fc.out;
        out.println();
        out.println("####### Instructions ###################################");
        out.println("# Copy the following lines into RBBITestMonkey.java in #");
        out.println(
                "# ICU4J, in the constructor of RBBIMeowMonkey, replacing #"
                        .replace("Meow", outFilename.substring(0, 4).replace("Graph", "Char")));
        out.println("# the existing block of generated code.                #");
        out.println("########################################################");
        out.println();
        out.println("            // --- NOLI ME TANGERE ---");
        out.println("            // Generated by GenerateBreakTest.java in the Unicode tools.");
        for (Segmenter.Builder.NamedRefinedSet part : segmenter.getPartitionDefinition()) {
            out.println(
                    "            partition.add(new NamedSet(\""
                            + part.getName().replace("\\", "\\\\").replace("\"", "\\\"")
                            + "\", new UnicodeSet(\""
                            + part.getDefinition().replace("\\", "\\\\").replace("\"", "\\\"")
                            + "\")));");
        }
        out.println();
        for (Segmenter.SegmentationRule rule : segmenter.getRules()) {
            out.println("            rules.add(" + rule.toJavaOldMonkeyString() + ");");
        }
        out.println("            // --- End of generated code. ---");
        fc.close();
    }

    private void generateTest(
            boolean shortVersion, String path, String outFilename, String propertyName)
            throws IOException {
        TreeMap<Double, String> rulesFound = new TreeMap<>();

        final List<String> testCases = new ArrayList<String>();
        // do main test

        final UnicodeDataFile fc =
                UnicodeDataFile.openAndWriteHeader(
                                path, outFilename + (shortVersion ? "_SHORT" : ""))
                        .setSkipCopyright(Settings.SKIP_COPYRIGHT);
        final PrintWriter out = fc.out;

        int counter = 0;

        out.println("#");
        out.println("# Default " + propertyName + " Test");
        out.println("#");
        out.println("# Format:");
        out.println("# <string> (# <comment>)?");
        out.println("#  <string> contains hex Unicode code points, with");
        out.println("#\t" + BREAK + " wherever there is a break opportunity, and");
        out.println("#\t" + NOBREAK + " wherever there is not.");
        out.println("#  <comment> the format can change, but currently it shows:");
        out.println("#\t- the sample character name");
        out.println(
                "#\t- (x) the " + propertyName + " property value for the sample character and ");
        out.println("#\t  any other properties relevant to the algorithm, as described in ");
        out.println("#\t  " + fileName + "BreakTest.html");
        out.println("#\t- [x] the rule that determines whether there is a break or not,");
        out.println("#\t   as listed in the Rules section of " + fileName + "BreakTest.html");
        out.println("#");
        sampleDescription(out);
        out.println("# These samples may be extended or changed in the future.");
        out.println("#");

        for (int ii = 0; ii < samples.size(); ++ii) {
            final String before = samples.get(ii);

            for (int jj = 0; jj < samples.size(); ++jj) {
                Utility.dot(counter);
                final String after = samples.get(jj);

                // do line straight
                testCases.clear();
                genTestItems(before, after, testCases);
                genTestItems(before + "\u0308", after, testCases);
                for (final String testCase : testCases) {
                    printLine(out, testCase, !shortVersion, false, rulesFound);
                    ++counter;
                }
            }
        }

        for (int ii = 0; ii < extraSingleSamples.size(); ++ii) {
            printLine(out, extraSingleSamples.get(ii), true, false, rulesFound);
            ++counter;
        }

        for (String extraTestSample : extraTestSamples) {
            printLine(out, extraTestSample, true, false, rulesFound);
            ++counter;
        }

        out.println("#");
        out.println("# Lines: " + counter);
        out.println("#");
        out.println("# EOF");
        fc.close();
        Set<Double> numbers = getMissing(fileName, rulesFound);
        if (!numbers.isEmpty()) {
            // throw new IllegalArgumentException
            System.err.println(
                    "***Rules missing from TESTS for "
                            + fileName
                            + ": "
                            + numbers
                            + "You will need to add samples that trigger those rules. "
                            + "See https://sites.google.com/site/unicodetools/home/changing-ucd-properties#TOC-Adding-Segmentation-Sample-Strings");
        }
        for (Entry<Double, String> entry : rulesFound.entrySet()) {
            System.out.println(
                    "\"" + escaper.transform(entry.getValue()) + "\",\t\t//" + entry.getKey());
        }
    }

    private Set<Double> getMissing(String fileName, Map<Double, String> rulesFound) {
        Set<Double> numbers = getRuleNumbers();
        numbers.removeAll(rulesFound.keySet());
        if (fileName.equals("Grapheme")) {
            numbers.remove(9.2d); // Prepend is optional, and by default empty
        } else if (fileName.equals("Line")) {
            numbers.remove(0.2d); // sot is an artifact
        }
        return numbers;
    }

    public void sampleDescription(PrintWriter out) {}

    public abstract boolean isBreak(String source, int offset);

    public abstract String fullBreakSample();

    public int mapType(int input) {
        return input;
    }

    public abstract String getTypeID(int s);

    public String getTypeID(String s) {
        if (s == null) {
            return "<null>";
        }
        int cp1 = UnicodeSet.getSingleCodePoint(s);
        if (cp1 != Integer.MAX_VALUE) {
            return getTypeID(cp1);
        }
        final StringBuffer result = new StringBuffer();
        int cp;
        for (int i = 0; i < s.length(); i += Character.charCount(cp)) {
            cp = UTF16.charAt(s, i);
            if (i > 0) {
                result.append(" ");
            }
            result.append(getTypeID(cp));
        }
        return result.toString();
    }

    static final int DONE = -1;

    public int next(String source, int offset) {
        for (int i = offset + 1; i <= source.length(); ++i) {
            if (isBreak(source, i)) {
                return i;
            }
        }
        return DONE;
    }

    public int previous(String source, int offset) {
        for (int i = offset - 1; i >= 0; --i) {
            if (isBreak(source, i)) {
                return i;
            }
        }
        return DONE;
    }

    public List<String> genTestItems(String before, String after, List<String> results) {
        results.add(before + after);
        return results;
    }

    public String getTableEntry(String before, String after, List<String> rulesOut) {
        rulesOut.clear();
        final boolean normalBreak = isBreak(before + after, before.length());
        final String normalRule = getRule();
        rulesOut.add(normalRule);
        if (fileName.equals("Line")) {
            final boolean spaceBreak = isBreak(before + " " + after, before.length() + 1);
            final String spaceRule = getRule();
            if (normalBreak && !spaceBreak) {
                // Edge cases such as LF ÷ LF, but LF SP × LF.
                return BREAK;
            } else if (!spaceRule.equals(normalRule)) {
                rulesOut.add(spaceRule);
            }
            return normalBreak ? BREAK : spaceBreak ? INDIRECT_BREAK : NOBREAK;
        } else {
            return normalBreak ? BREAK : NOBREAK;
        }
    }

    boolean skipType(int type) {
        return false;
    }

    String getInfo(String s) {
        if (s == null || s.length() == 0) {
            return "NULL";
        }
        final StringBuffer result = new StringBuffer();
        int cp;
        for (int i = 0; i < s.length(); i += Character.charCount(cp)) {
            cp = UTF16.charAt(s, i);
            if (i > 0) {
                result.append(", ");
            }
            result.append(ucd.getCodeAndName(cp));
            result.append(", gc=" + UCD.getCategoryID_fromIndex(ucd.getCategory(cp), SHORT));
            result.append(", sc=" + UCD.getScriptID_fromIndex(ucd.getScript(cp), SHORT));
            // result.append(", lb=" + ucd.getLineBreakID_fromIndex(ucd.getLineBreak(cp))
            //    + "=" + ucd.getLineBreakID_fromIndex(ucd.getLineBreak(cp), LONG));
        }
        return result.toString();
    }

    public void generateTable(PrintWriter out) {
        out.println("<h3>" + linkAndAnchor("table", "Table") + "</h3>");
        final String width = "width='" + (100 / (tableLimit + 1)) + "%'";
        out.print("<table border='1' cellspacing='0' width='100%'>");
        String types = "";
        for (int type = 0; type < tableLimit; ++type) {
            final String after = samples.get(type);
            if (after == null) {
                continue;
            }

            final String h = getTypeID(after);
            types +=
                    "<th "
                            + width
                            + " class='lbclass' title='"
                            + getInfo(after)
                            + "'>"
                            + h
                            + "</th>";

            // codes += "<th " + width + " title='" + getInfo(after) + "'>" + Utility.hex(after) +
            // "</th>";
        }

        out.println("<tr><th " + width + "></th>" + types + "</tr>");
        // out.println("<tr><th " + width + "></th><th " + width + "></th>" + codes + "</tr>");

        final List<String> rules = new ArrayList<>();
        for (int type = 0; type < samples.size(); ++type) {
            if (type == tableLimit) {
                out.println(
                        "<tr><td bgcolor='#0000FF' colSpan='"
                                + (tableLimit + 1)
                                + "' style='font-size: 1px'>&nbsp;</td></tr>");
            }
            final String before = samples.get(type);
            if (before == null) {
                continue;
            }

            final String h = getTypeID(before);
            String line =
                    "<tr><th class='lbclass' title='"
                            + ucd.getCodeAndName(before)
                            + "'>"
                            + h
                            + "</th>";

            for (int type2 = 0; type2 < tableLimit; ++type2) {

                final String after = samples.get(type2);
                if (after == null) {
                    continue;
                }

                final String t = getTableEntry(before, after, rules);
                String background = "";
                if (t.contains(NOBREAK)) {
                    background = " bgcolor='#CCCCFF'";
                } else if (!t.equals(BREAK)) {
                    background = " bgcolor='#CCFFCC'";
                }
                line +=
                        "<th title='"
                                + String.join(" ", rules)
                                + "'"
                                + background
                                + " class='pairItem'>"
                                + t
                                + "</th>";
            }
            out.println(line + "</tr>");
        }
        out.println("</table>");

        if (didSkipSamples) {
            out.println("<p><b>Suppressed:</b> ");
            for (final int skippedSample : skippedSamples) {
                if (skippedSample > 0) {
                    final String tmp = UTF16.valueOf(skippedSample);
                    out.println("<span title='" + getInfo(tmp) + "'>" + getTypeID(tmp) + "</span>");
                }
            }
            out.println("</p>");
        }

        // gather the data for the rules
        if (needsFullBreakSample) {
            collectingRules = true;
            isBreak(fullBreakSample(), 1);
            collectingRules = false;
        }

        out.println("<h3>" + linkAndAnchor("rules", "Rules") + "</h3>");
        out.print(
                "<p>This section shows the rules. They are mechanically modified for programmatic generation of the tables and test code, and"
                        + " thus do not match the UAX rules precisely. "
                        + "In particular:</p>"
                        + "<ol>"
                        + "<li>The rules are cast into a form that is more like regular expressions.</li>"
                        + "<li>The rules “sot "
                        + (fileName.equals("Line") ? "×" : "÷")
                        + "”, “÷ eot”, and “÷ Any” are added mechanically, and have artificial numbers.</li>"
                        + "<li>The rules are given decimal numbers using tenths, and are written without prefix. For example, ");
        switch (fileName) {
            case "Line":
                out.print("rule LB21a is given the number 21.1");
                break;
            case "Grapheme":
                out.print("rule GB9a is given the number 9.1");
                break;
            case "Word":
                out.print("rule WB13a is given the number 13.1");
                break;
            case "Sentence":
                out.print("rule SB8a is given the number 8.1");
                break;
        }
        out.print(
                ".</li>"
                        + "<li>Final rules like “Any ÷ Any” may be recast as the equivalent expression “÷ Any”.</li><li>");
        if (fileName.equals("Line")) {
            out.print(
                    "Where a rule has multiple parts (lines), each one is numbered using hundredths, "
                            + "such as 21.01) × BA, 21.02) × HY, ... ");
        }
        out.println(
                "</li></ol>"
                        + "<p>For the original rules"
                        + (fileName.equals("Word") || fileName.equals("Sentence")
                                ? " and the macro values they use"
                                : "")
                        + ", see UAX #"
                        + (fileName.equals("Line") ? "14" : "29")
                        + ".</p>");
        // out.println("<ul style='list-style-type: none'>");
        out.println("<table>");
        for (int ii = 0; ii < ruleListCount; ++ii) {
            String cleanRule = ruleList[ii].replaceAll("[$]", "");
            if (!isBreak("a", 0)) {
                cleanRule = cleanRule.replace("sot ÷", "sot ×");
            }
            final int parenPos = cleanRule.indexOf(')');
            final String ruleNumber = cleanRule.substring(0, parenPos);
            final String ruleBody = cleanRule.substring(parenPos + 1).trim();
            int breakPoint = ruleBody.indexOf('×');
            if (breakPoint < 0) {
                breakPoint = ruleBody.indexOf('÷');
            }
            if (breakPoint < 0) {
                breakPoint = ruleBody.indexOf('→');
            }
            out.println(
                    "<tr><th style='text-align:right'>"
                            + linkAndAnchor("r" + ruleNumber, ruleNumber)
                            + "</th>"
                            + "<td style='text-align:right'>"
                            + ruleBody.substring(0, breakPoint)
                            + "</td><td>"
                            + ruleBody.substring(breakPoint, breakPoint + 1)
                            + "</td><td>"
                            + ruleBody.substring(breakPoint + 1)
                            + "</td></tr>");
            // out.println("<li>" + cleanRule + "</li>");
        }
        out.println("</table>");
        // out.println("</ul>");

        Map<Double, String> rulesFound = new TreeMap<>();

        if (extraSingleSamples.size() > 0) {
            out.println("<h3>" + linkAndAnchor("samples", "Sample Strings") + "</h3>");
            out.println(
                    "<p>"
                            + "The following samples illustrate the application of the rules. "
                            + "The blue lines indicate possible break points. "
                            + "If your browser supports titles (tooltips), then positioning the mouse over each character will show its name, "
                            + "while positioning between characters shows the number of the rule responsible for the break-status."
                            + "</p>");
            out.println("<table>");
            for (int ii = 0; ii < extraSingleSamples.size(); ++ii) {
                final String ruleNumber = String.valueOf(ii + 1);
                out.println(
                        "<tr><th style='text-align:right'>"
                                + linkAndAnchor("s" + ruleNumber, ruleNumber)
                                + "</th><td><font size='5'>");
                printLine(out, extraSingleSamples.get(ii), true, true, rulesFound);
                out.println("</font></td></tr>");
            }
            out.println("</table>");
        }
        Set<Double> numbers = getMissing(fileName, rulesFound);
        if (!numbers.isEmpty()) {
            System.err.println("***Rules missing from SAMPLES for " + fileName + ": " + numbers);
        }
    }

    Set<Double> getRuleNumbers() {
        Set<Double> results = new TreeSet<Double>();
        for (int ii = 0; ii < ruleListCount; ++ii) {
            String ruleString = ruleList[ii];
            final int parenPos = ruleString.indexOf(')');
            final String ruleNumber = ruleString.substring(0, parenPos);
            results.add(Double.parseDouble(ruleNumber));
        }
        return results;
    }

    public String linkAndAnchor(String anchor, String text) {
        return ("<a href='#" + anchor + "' name='" + anchor + "'>" + text + "</a>");
    }

    static final String BREAK = "÷";
    static final String INDIRECT_BREAK = "∻";
    static final String NOBREAK = "×";

    public void printLine(
            PrintWriter out,
            String source,
            boolean comments,
            boolean html,
            Map<Double, String> rulesFound) {
        int cp;
        final StringBuffer string = new StringBuffer();
        final StringBuffer comment = new StringBuffer("\t# ");
        boolean hasBreak = isBreak(source, 0);
        addToRules(rulesFound, source, hasBreak);

        String status;
        if (html) {
            status = hasBreak ? " style='border-right: 1px solid blue'" : "";
            string.append(
                    "<span title='"
                            + getRule()
                            + "'><span"
                            + status
                            + ">&nbsp;</span>&nbsp;</span>");
        } else {
            status = hasBreak ? BREAK : NOBREAK;
            string.append(status);
        }
        comment.append(' ').append(status).append(" [").append(getRule()).append(']');

        for (int offset = 0; offset < source.length(); offset += UTF16.getCharCount(cp)) {

            cp = UTF16.charAt(source, offset);
            hasBreak = isBreak(source, offset + UTF16.getCharCount(cp));
            addToRules(rulesFound, source, hasBreak);

            if (html) {
                status = hasBreak ? " style='border-right: 1px solid blue'" : "";
                string.append(
                        "<span title='"
                                + Utility.quoteXML(
                                        ucd.getCodeAndName(cp) + " (" + getTypeID(cp) + ")", true)
                                + "'>"
                                + Utility.quoteXML(UtilityBase.getDisplay(cp), true)
                                + "</span>");
                string.append(
                        "<span title='"
                                + getRule()
                                + "'><span"
                                + status
                                + ">&nbsp;</span>&nbsp;</span>\n");
            } else {
                if (string.length() > 0) {
                    string.append(' ');
                    comment.append(' ');
                }

                status = hasBreak ? BREAK : NOBREAK;

                string.append(Utility.hex(cp));
                comment.append(ucd.getName(cp) + " (" + getTypeID(cp) + ")");
                string.append(' ').append(status);
                comment.append(' ').append(status).append(" [").append(getRule()).append(']');
            }
        }

        if (comments && !html) {
            string.append(comment);
        }
        out.println(string);
        if (DEBUG) {
            System.out.println("*" + string);
        }
    }

    private void addToRules(Map<Double, String> rulesFound, String source, boolean hasBreak) {
        final String rule = getRule();
        final double key = Double.parseDouble(rule);
        String last = rulesFound.get(key);
        if (last != null && last.length() < source.length()) {
            return; // use the shortest sample
        }
        rulesFound.put(key, source);
        if (!hasBreak) {
            rulesFound.put(999d, "final");
        }
    }

    public void findSamples() {
        // Pick a sample for each class of the partition.
        // The sample is picked among the oldest characters in the class for classes containing
        // assigned characters.  For classes containing only unassigned code points, the last code
        // point is used.  This makes the sample stable when new characters are encoded; however,
        // the choice of sample can be affected by changes to relevant property assignments of
        // existing characters.
        final var gc = IUP.getProperty(UcdProperty.General_Category);
        final var unassigned = gc.getSet("Cn");
        final var surrogate = gc.getSet("Cs");
        final var privateUse = gc.getSet("Co");
        final var noncharacters =
                IUP.getProperty(UcdProperty.Noncharacter_Code_Point).getSet("True");
        final var age = IUP.getProperty(UcdProperty.Age);

        for (String partName : partition.getAvailableValues()) {
            final UnicodeSet part = partition.getSet(partName);
            final UnicodeSet assigned =
                    part.cloneAsThawed()
                            .removeAll(unassigned)
                            .removeAll(surrogate)
                            .removeAll(privateUse);
            if (assigned.isEmpty()) {
                final UnicodeSet nonCsCoNChar =
                        part.cloneAsThawed()
                                .removeAll(surrogate)
                                .removeAll(privateUse)
                                .removeAll(noncharacters);
                if (nonCsCoNChar.isEmpty()) {
                    System.out.println(
                            "Skipping "
                                    + partName
                                    + " which only applies to surrogate, private use, or noncharacter code points");
                    continue;
                }
                samples.add(Character.toString(nonCsCoNChar.charAt(nonCsCoNChar.size() - 1)));
            } else {
                for (var version : Age_Values.values()) {
                    final UnicodeSet assignedAtVersion = age.getSet(version).retainAll(assigned);
                    if (!assignedAtVersion.isEmpty()) {
                        samples.add(Character.toString(assignedAtVersion.charAt(0)));
                        break;
                    }
                }
            }
        }

        tableLimit = samples.size();

        // now add values that are different
        if (extraSamples.size() > 0) {
            samples.addAll(extraSamples);
        }
    }

    public static UnicodeSet getSet(UCD ucd, int prop, byte propValue) {
        return UnifiedBinaryProperty.make(prop | propValue, ucd).getSet();
    }

    public static class Context {
        public int cpBefore2, cpBefore, cpAfter, cpAfter2;
        public byte tBefore2, tBefore, tAfter, tAfter2;

        @Override
        public String toString() {
            return "["
                    + Utility.hex(cpBefore2)
                    + "("
                    + tBefore2
                    + "), "
                    + Utility.hex(cpBefore)
                    + "("
                    + tBefore
                    + "), "
                    + Utility.hex(cpAfter)
                    + "("
                    + tAfter
                    + "), "
                    + Utility.hex(cpAfter2)
                    + "("
                    + tAfter2
                    + ")]";
        }
    }

    // ==============================================

    static class XGenerateBreakTest extends GenerateBreakTest {
        String sample;

        {
            needsFullBreakSample = false;
        }

        public XGenerateBreakTest(
                UCD ucd,
                Segmenter.Builder segBuilder,
                String sample,
                String filename,
                String[] extraSamples,
                String[] extraSingleSamples) {
            super(ucd, segBuilder.make());
            this.sample = sample;
            final List<String> rules = segBuilder.getRules();
            collectingRules = true;
            for (final Iterator<String> it = rules.iterator(); it.hasNext(); ) {
                final String rule = it.next();
                setRule(rule);
            }
            variables = segBuilder.getVariables();
            collectingRules = false;
            segmenter = seg;
            partition = seg.getSamples();
            fileName = filename;
            propertyName = (filename.equals("Grapheme") ? "Grapheme_Cluster" : fileName) + "_Break";
            this.extraSamples.addAll(Arrays.asList(extraSamples));

            this.extraSingleSamples.addAll(
                    Arrays.asList("\r\na\n\u0308", "a\u0308", " \u200D\u0646", "\u0646\u200D "));
            this.extraSingleSamples.addAll(Arrays.asList(extraSingleSamples));
        }

        @Override
        public boolean isBreak(String source, int offset) {
            final boolean result = seg.breaksAt(source, offset);
            setRule(String.valueOf(seg.getBreakRule()));
            return result;
        }

        @Override
        public String fullBreakSample() {
            return sample;
        }

        // stuff that subclasses need to override
        @Override
        public String getTypeID(int cp) {
            return partition.get(cp);
        }
    }

    static class Sampler {
        final UnicodeProperty prop;

        Sampler(String propName) {
            prop = unicodePropertySource.getProperty(propName);
        }

        String get(String value) {
            return get(value, 1);
        }

        String get(String value, int count) {
            for (String s : prop.getSet(value)) {
                if (--count == 0) {
                    return s;
                }
            }
            throw new IllegalArgumentException(
                    prop.getName() + ":" + value + " doesn't have " + count + " values");
        }
    }

    static class GenerateGraphemeBreakTest extends XGenerateBreakTest {

        public GenerateGraphemeBreakTest(UCD ucd, Segmenter.Target target) {
            super(
                    ucd,
                    Segmenter.make(
                            ToolUnicodePropertySource.make(ucd.getVersion()),
                            "GraphemeClusterBreak",
                            target),
                    "aa",
                    "Grapheme",
                    new String[] {unicodePropertySource.getSet("GC=Cn").iterator().next()},
                    new String[] {});

            System.out.println("Target: " + seg.target);

            Sampler GCB = new Sampler("GCB");
            this.extraSingleSamples.addAll(
                    Arrays.asList(
                            GCB.get("L") + GCB.get("L"),
                            GCB.get("LV") + GCB.get("T") + GCB.get("L"),
                            GCB.get("LVT") + GCB.get("T") + GCB.get("L"),
                            GCB.get("RI") + GCB.get("RI", 2) + GCB.get("RI", 3) + "b",
                            "a" + GCB.get("RI") + GCB.get("RI", 2) + GCB.get("RI", 3) + "b",
                            "a" + GCB.get("RI") + GCB.get("RI", 2) + zwj + GCB.get("RI", 3) + "b",
                            "a" + GCB.get("RI") + zwj + GCB.get("RI", 2) + GCB.get("RI", 3) + "b",
                            "a"
                                    + GCB.get("RI")
                                    + GCB.get("RI", 2)
                                    + GCB.get("RI", 3)
                                    + GCB.get("RI", 4)
                                    + "b",
                            "a" + zwj,
                            "a" + "\u0308" + "b",
                            "a" + GCB.get("SpacingMark") + "b",
                            "a" + GCB.get("Prepend") + "b",

                            // "a" + GCB.get("LinkingConsonant") + GCB.get("Virama") +
                            // GCB.get("LinkingConsonant") + "b",
                            // "a" + GCB.get("Virama") + GCB.get("LinkingConsonant") + "b",
                            // "a" + zwj + GCB.get("LinkingConsonant") + "b",

                            // GCB.get("E_Base") + GCB.get("E_Modifier") + GCB.get("E_Base"),
                            // "a" + GCB.get("E_Modifier") + GCB.get("E_Base"),

                            sampleEBase + sampleEMod + sampleEBase,
                            "a" + sampleEMod + sampleEBase,
                            "a" + sampleEMod + sampleEBase + zwj + sampleEmoji,
                            sampleEBase + sampleEMod + sampleMn + zwj + sampleEBase + sampleEMod,
                            sampleEmoji + zwj + sampleEmoji,
                            "a" + zwj + sampleEmoji,
                            sampleEXP + zwj + sampleEXP,
                            "a" + zwj + sampleEXP,

                            // zwj + GCB.get("EBG") + GCB.get("E_Modifier"),
                            // zwj + GCB.get("Glue_After_Zwj"),
                            // zwj + GCB.get("EBG"),
                            // GCB.get("EBG") + GCB.get("EBG")

                            "क" + "त",
                            "क" + "\u094D" + "त",
                            "क" + "\u094D" + "\u094D" + "त",
                            "क" + "\u094D" + zwj + "त",
                            "क" + "\u093C" + zwj + "\u094D" + "त",
                            "क" + "\u093C" + "\u094D" + zwj + "त",
                            "क" + "\u094D" + "त" + '\u094D' + "य",
                            "क" + "\u094D" + "a",
                            "a" + "\u094D" + "त",
                            "?" + "\u094D" + "त",
                            "क" + "\u094D\u094D" + "त",
                            // From L2/14-131, §3.2; made into a single EGC by 179-C31.
                            // This test would have caught ICU-22956.
                            "સૻ્સૻ"));
        }
    }

    static class GenerateLineBreakTest extends XGenerateBreakTest {
        public GenerateLineBreakTest(UCD ucd, Segmenter.Target target) {
            super(
                    ucd,
                    Segmenter.make(
                            ToolUnicodePropertySource.make(ucd.getVersion()), "LineBreak", target),
                    "aa",
                    "Line",
                    // extraSamples
                    new String[] {},
                    // extraSingleSamples
                    new String[] {
                        "\u000Bぁ", // 4.0
                        "\rぁ", // 5.02
                        "\u0085ぁ", // 5.04
                        "\u200D☝", // 8.1
                        "ぁ\u2060", // 11.01
                        "\u2060ぁ", // 11.02
                        "ぁ̈ ", // 12.2
                        "\u200D ", // 12.3
                        "\u200D/", // 13.04
                        "——", // 17.0
                        "ぁ￼", // 20.01
                        "￼ぁ", // 20.02
                        "ぁ-", // 21.02
                        "ก․", // 22.01
                        "!․", // 22.02
                        "․․", // 22.04
                        "0․", // 22.05
                        "☝%", // 23.01
                        "ก0", // 23.02
                        "$☝", // 24.01
                        "$ก", // 24.02
                        "%ก", // 24.03
                        "ᄀ\u1160", // 26.01
                        "\u1160\u1160", // 26.02
                        "ᆨᆨ", // 26.03
                        "\u1160․", // 27.01
                        "\u1160%", // 27.02
                        "$\u1160", // 27.03
                        "☝🏻", // 30.2
                        "final", // 999.0
                        "can't",
                        "can\u2019t",
                        "'can' not",
                        "can 'not'",
                        "bug(s)     ",
                        "bug(s)\u00a0     ",
                        "..ます。XMLの..",
                        "ab\u00ADby",
                        "-3",
                        "e.g.",
                        "\u4e00.\u4e00.",
                        "a  b",
                        "a  \u200bb",
                        "a \u0308b",
                        "1\u0308b(a)-(b)",
                        "give book(s).",
                        "ま(す)",
                        "find .com",
                        "equals .35 cents",
                        "(s)he",
                        "{s}he",
                        "ˈsIləb(ə)l",
                        "ˈsIləb{ə}l",
                        "code(s).",
                        "code(s.)",
                        "code(s)!",
                        "code(s!)",
                        "code\\(s\\)",
                        "code( s )",
                        "code{s}",
                        "code{s}.",
                        "code{s}!",
                        "code\\{s\\}",
                        "code{ s }",
                        "cod(e)…(s)",
                        "(cod(e)…)s",
                        "cod{e}…{s}",
                        "{cod{e}…}s",
                        "(con-)lang",
                        "(con\u00AD)lang",
                        "(con‑)lang",
                        "(con)-lang",
                        "(con)\u00ADlang",
                        "(con)‑lang",
                        "{con-}lang",
                        "{con\u00AD}lang",
                        "{con‑}lang",
                        "{con}-lang",
                        "{con}\u00ADlang",
                        "{con}‑lang",
                        "cre\u0301(e\u0301)(e)",
                        "cre\u0301[er|e\u0301(e)(s)]",
                        "cre\u0301{er|e\u0301(e)(s)}",
                        "ambigu(̈)(e\u0308)",
                        "ambigu(«̈»)(e\u0308)",
                        "ambigu(« ̈ »)(e\u0308)",
                        "ambigu« ( ̈ ) »(e\u0308)",
                        "ambigu«\u202F( ̈ )\u202F»(e\u0308)",
                        "ambigu{̈}(e\u0308)",
                        "ambigu{«̈»}(e\u0308)",
                        "ambigu{« ̈ »}(e\u0308)",
                        "ambigu« { ̈ } »(e\u0308)",
                        "ambigu«\u202F{ ̈ }\u202F»(e\u0308)",
                        "(czerwono\u00AD‑)niebieska",
                        "(czerwono\u00AD)‑niebieska",
                        "(czerwono)\u00AD‑niebieska",
                        "{czerwono\u00AD‑}niebieska",
                        "{czerwono\u00AD}‑niebieska",
                        "{czerwono}\u00AD‑niebieska",
                        "operator[](0);",
                        "operator[](){}",
                        "本(を)読む",
                        "本(「を」)読む",
                        "本「(を)」読む",
                        "本{を}読む",
                        "本{「を」}読む",
                        "本[(を)]読む",
                        "(ニュー・)ヨーク",
                        "(ニュー)・ヨーク",
                        "{ニュー・}ヨーク",
                        "{ニュー}・ヨーク",
                        "(ᡐᡆᡑᡆ᠆)ᠪᠢᠴᠢᠭ\u180C",
                        "(ᡐᡆᡑᡆ)᠆ᠪᠢᠴᠢᠭ\u180C",
                        "{ᡐᡆᡑᡆ᠆}ᠪᠢᠴᠢᠭ\u180C",
                        "{ᡐᡆᡑᡆ}᠆ᠪᠢᠴᠢᠭ\u180C",
                        "(http://)xn--a",
                        "{http://}xn--a",
                        "(0,1)+(2,3)⊕(−4,5)⊖(6,7)",
                        "{0,1}+{2,3}⊕{−4,5}⊖{6,7}",
                        "ab",
                        "ab ",
                        "ab c",
                        "aま",
                        "हिन्दी ",
                        "यसगुचितीयसा ",
                        "印本",
                        "読む",
                        "入力しエ",
                        "位。記",
                        "本。",
                        "険」の",
                        "しょう",
                        "まa本",
                        "없어요 or 못",
                        "まab ",
                        "で使",
                        "する",
                        "のパン",
                        "う　え　お」",
                        "る 은영 に",
                        "しょう。",
                        "ムの一",
                        "フリ",
                        "フリー百",
                        "ピュータで使用する",
                        "ターキーを押",
                        "ション",
                        "a.2 ",
                        "a.2 क",
                        "a.2 本",
                        "a.2　本",
                        "a.2　ま",
                        "a.2　3",
                        "ab. 2",
                        "A.1 못",
                        "봤어. A.2 볼",
                        "봐요. A.3 못",
                        "요. A.4 못",
                        "a.2　「",
                        "に「バ(ba)」や「ス",
                        "る「UKポンド」）、エ",
                        "は、「=rand()」と",
                        "で、「!」と",
                        "訳「す",
                        "て「봤어?」と",
                        "の「そ",
                        "は「エ",
                        "例：「あ　い",
                        "く、「평양은",
                        "に「제목(題名)은",
                        "典『ウィキ",
                        "で『英語",
                        "(s) 本",
                        "(s) ま",
                        "(s) ク",
                        "る。dog（犬）を",
                        "本（ま",
                        "本 (a",
                        "点 [編集]",
                        "a(s) ",
                        "（ザ・クイック・ブ",
                        "p（クイック・ブ",
                        "ab（ク",
                        "(印本)",
                        "ス（い",
                        "ド（ポ",
                        "ド (質",
                        "s)」ま",
                        "a）』",
                        "る」）は",
                        "ド」）、エ",
                        "rk)」も",
                        "ク(ab cd)」も",
                        "ン・マーク(ex",
                        "マー(ma)」な",
                        "ガワ」。こ",
                        "ク」ま",
                        "ワ」。こ",
                        "ク」ま、本",
                        "ク」、ク",
                        "ディア（ab）』",
                        "쪽이에요?」と聞",
                        "名)은 알아요?」と",
                        "貨) - (po",
                        "量) 〜 (po",
                        "ド重） 〜 力・重",
                        "ab\"（ま",
                        "は \"s\" ",
                        "は、\"The ",
                        "dog\" を",
                        "90\" と",
                        "ス・オーバー・ザ・レ",
                        "ス・ジャン",
                        "ン・フォック",
                        "イジー・ドッグ、和",
                        "メーション・マーク",
                        "ン・ク(a",
                        "ション・マ",
                        "本: ",
                        "本: ク",
                        "出典: フリー百",
                        "後…に",
                        "しょう。。。",
                        "き、!!、!!!と",
                        "は、?と!を",
                        "た、⁉(!?)の",
                        "や、⁈(?!)の",
                        "た ‽と",
                        "せ！100%の完",
                        "23本",
                        "ァベット26字を",
                        "例：£23",
                        "記号 £。",
                        "れる。qu",
                        "ま。",
                        "ま。ab ",
                        "る。数",
                        "る。こ",
                        "い。パ",
                        "ガワ」。これ",
                        "語のioの、2字を",
                        "、和",
                        "、タ",
                        "、か",
                        "、これでは ",
                        "し、abと",
                        // U+1F1E6 = base RI
                        "a\uD83C\uDDE6b",
                        "\uD83C\uDDF7\uD83C\uDDFA",
                        "\uD83C\uDDF7\uD83C\uDDFA\uD83C\uDDF8",
                        "\uD83C\uDDF7\uD83C\uDDFA\uD83C\uDDF8\uD83C\uDDEA",
                        "\uD83C\uDDF7\uD83C\uDDFA\u200B\uD83C\uDDF8\uD83C\uDDEA",
                        "\u05D0-\u05D0",
                        // Examples from L2/22-080R2, pp. 10 sq.
                        // NOTE(egg): This one does not use escape sequences,
                        // since it is in Kawi, which is outside the BMP.
                        "𑼦𑼂𑼭𑼦𑽂𑼦𑼱𑽁",
                        "\u1BD7\u1BEC\u1BD2\u1BEA\u1BC9\u1BF3\u1BC2\u1BE7\u1BC9\u1BF3",
                        "\u1B18\u1B27\u1B44\u200C\u1B2B\u1B38\u1B31\u1B44\u1B1D\u1B36",
                        // Dotted circle behaviour.  Balinese examples from
                        // L2/22-080R2, p. 20.
                        "e◌̂◌̣",
                        "◌᭄ᬬ",
                        "◌᭄◌᭄ᬬ",
                        // A Javanese cecak telu (nukta) and a subjoined consonant on the same
                        // dotted circle.
                        "◌꦳꧀ꦠ",
                        // Quotation mark examples from L2/23-063.  All spaces are U+0020 SPACE.
                        // Swedish.
                        "”Jo, når’n da ha gått ett stöck te, så kommer’n te e å, å i åa ä e ö.”\n"
                                + "”Vasa”, sa’n.\n"
                                + "”Å i åa ä e ö”, sa ja.",
                        "En gång undföll det honom dock, medan han släpade på det våta "
                                + "höet: »Varför är höet redan torrt och inkört där borta på "
                                + "Solbacken, och här hos oss är det vått?» — »Därför att de ha "
                                + "oftare sol än vi.»",
                        // French.
                        "vous me heurtez, vous dites : « Excusez-moi, » et vous croyez que cela "
                                + "suffit ?",
                        "j’ai dit : « Excusez-moi. » Il me semble donc que c’est assez.",
                        "Et vise au front mon père en criant : « Caramba ! »\u2028"
                                + "Le coup passa si près, que le chapeau tomba\u2028"
                                + "Et que le cheval fit un écart en arrière.\u2028"
                                + "« Donne-lui tout de même à boire, » dit mon père.",
                        "« Je me suis vengé […]\u2029"
                                + "» On ne me verra ni parler ni écrire ; vous aurez eu mes "
                                + "dernières paroles comme mes dernières adorations.\u2029"
                                + "» J. S. »",
                        // Vietnamese. Note that here we have a full stop *after* the quotation
                        // marks.
                        "— Không ai hãm bao giờ mà bây giờ hãm, thế nó mới « mới ».",
                        // ZWSP.
                        "Pas une citation »Zitat« Pas une citation non plus",
                        "« Citation »\u200BKein Zitat\u200B« Autre citation »",
                        // Various number examples for
                        // https://www.unicode.org/L2/L2024/24061.htm#179-A117
                        "start .789 end", // From https://unicode-org.atlassian.net/browse/ICU-12017
                        // various reasonable productions of the regex for numbers
                        //   (PR | PO)? (OP | HY)? IS? NU (NU | SY | IS)* (CL | CP)? (PR | PO)?
                        // separated by spaces.
                        "$-5 -.3 £(123.456) 123.€ +.25 1/2",
                        // Examples for LB20a:
                        // From L2/24-064R2 Section 5.13.
                        "the 3ms possessive pronominal suffix ( -šu )",
                        // From https://unicode-org.atlassian.net/browse/CLDR-3029.
                        "Mac Pro -tietokone",
                        // Examples for LB19/LB19a from L2/24-064R2 Section 5.5.
                        "子曰：“学而时习之，不亦说乎？有朋自远方来，不亦乐乎？人不知而不愠，不亦君子乎？”",
                        "子贡曰：“贫而无谄，富而无骄，何如？”子曰：“可也。未若贫而乐，富而好礼者也”。子贡曰：“《诗》云：‘如切如磋，如琢如磨。’其斯之谓与？”子曰：“赐也，始可与言《诗》已矣！吿诸往而知来者。”",
                        "哪一所中国学校乃“为各省派往日本游学之首倡”？",
                        "哪个商标以人名为名，因特色小吃“五台杂烩汤”而入选“新疆老字号”？",
                        "毕士悌（1901年—1936年），朝鲜籍红军将领",
                        "2000年获得了《IGN》的“Best Game Boy Strategy”奖。",
                        "Z-1“莱贝雷希特·马斯”号是德国国家海军暨战争海军于1930年代",
                        "Anmerkung: „White“ bzw. ‚白人‘ – in der Amtlichen Statistik",
                        // Examples for LB21a.
                        " ⁧John ו-Michael⁩;", // No break after ו-‏.
                        "וַֽיְהִי־כֵֽן׃", // Break after maqaf since Unicode 16.
                    });

            // Additions for Unicode 14 LB30b   [\p{Extended_Pictographic}&\p{Cn}] × EM
            ToolUnicodePropertySource propSource = ToolUnicodePropertySource.make(ucd.getVersion());
            UnicodeSet unassigned = propSource.getSet("gc=Cn");
            UnicodeSet extPict = propSource.getSet("ExtPict=yes");
            // [\p{Extended_Pictographic}&\p{Cn}]
            UnicodeSet extPictUnassigned = extPict.cloneAsThawed().retainAll(unassigned);
            String firstExtPictUnassigned = UTF16.valueOf(extPictUnassigned.charAt(0));
            // [\p{Extended_Pictographic}&\p{Cn}] × EM
            extraSingleSamples.add(firstExtPictUnassigned + sampleEMod);

            UnicodeSet lb_EBase = propSource.getSet("lb=EB");
            // [\p{Extended_Pictographic}-\p{Cn}-\p{lb=EB}]
            UnicodeSet extPictAssigned =
                    extPict.cloneAsThawed().removeAll(unassigned).removeAll(lb_EBase);
            String firstExtPictAssigned = UTF16.valueOf(extPictAssigned.charAt(0));
            // [\p{Extended_Pictographic}-\p{Cn}-\p{lb=EB}] ÷ EM
            extraSingleSamples.add(firstExtPictAssigned + sampleEMod);
        }

        @Override
        public boolean isBreak(String source, int offset) {
            return offset == 0 ? false : super.isBreak(source, offset);
        }

        @Override
        public List<String> genTestItems(String before, String after, List<String> results) {
            super.genTestItems(before, after, results);
            results.add(before + " " + after);
            return results;
        }
    }

    static class GenerateSentenceBreakTest extends XGenerateBreakTest {
        public GenerateSentenceBreakTest(UCD ucd, Segmenter.Target target) {
            super(
                    ucd,
                    makeSegmenter(ucd, target),
                    "aa",
                    "Sentence",
                    new String[] {},
                    getExtraSamples(ucd, target));
        }

        private static Builder makeSegmenter(UCD ucd, Segmenter.Target target) {
            final Builder result =
                    Segmenter.make(
                            ToolUnicodePropertySource.make(ucd.getVersion()),
                            "SentenceBreak",
                            target);
            final Segmenter segmenter = result.make();
            final boolean failure = segmenter.breaksAt("etc.)\u2019 \u2018(the", 7);
            if (failure) {
                throw new IllegalArgumentException();
            }
            return result;
        }

        static String[] getExtraSamples(UCD ucd, Segmenter.Target target) {
            final GenerateBreakTest grapheme = new GenerateGraphemeBreakTest(ucd, target);
            String[] extraSingleSamples =
                    new String[] {
                        "(\"Go.\") (He did.)",
                        "(\u201CGo?\u201D) (He did.)",
                        "U.S.A\u0300. is",
                        "U.S.A\u0300? He",
                        "U.S.A\u0300.",
                        "3.4",
                        "c.d",
                        "C.d",
                        "c.D",
                        "C.D",
                        "etc.)\u2019 the",
                        "etc.)\u2019 The",
                        "etc.)\u2019 \u2018(the",
                        "etc.)\u2019 \u2018(The",
                        "etc.)\u2019 \u0308the",
                        "etc.)\u2019 \u0308The",
                        "etc.)\u2019\u0308The",
                        "etc.)\n\u0308The",
                        "the resp. leaders are",
                        "\u5B57.\u5B57",
                        "etc.\u5B83",
                        "etc.\u3002",
                        "\u5B57\u3002\u5B83",
                        "!\u0020\u0020",
                        // Examples from ICU4X, see https://github.com/unicode-org/icu4x/pull/3126.
                        "a.",
                        "a.\r\n",
                        "a.\r\n ",
                        "a.\r\na",
                        "A.\r\nA",
                    };
            final String[] temp = new String[extraSingleSamples.length * 2];
            System.arraycopy(extraSingleSamples, 0, temp, 0, extraSingleSamples.length);
            for (int i = 0; i < extraSingleSamples.length; ++i) {
                temp[i + extraSingleSamples.length] =
                        insertEverywhere(extraSingleSamples[i], "\u2060", grapheme);
            }
            extraSingleSamples = temp;
            return extraSingleSamples;
        }
    }

    static class GenerateWordBreakTest extends XGenerateBreakTest {
        public GenerateWordBreakTest(UCD ucd, Segmenter.Target target) {
            super(
                    ucd,
                    Segmenter.make(
                            ToolUnicodePropertySource.make(ucd.getVersion()), "WordBreak", target),
                    "aa",
                    "Word",
                    new String[] {
                        "a\u2060", "a:", "a'", "a'\u2060", "a,", "1:", "1'", "1,", "1.\u2060",
                    },
                    new String[] {
                        // Last word of and end of ayah 1, from
                        // https://en.wikipedia.org/wiki/Al-Fatiha.
                        "ٱلرَّحِيمِ ۝١",
                        // TUS Figure 9-9, 1st line, preceded by the word “psalm”.
                        "ܡܙܡܘܪܐ ܏ܝܗ",
                        // TUS Figure 9-9, 3rd line (abbreviation of ܬܫܒܘܚܬܐ).
                        "ܬ܏ܫܒܘ",
                    });
            System.out.println();
            Sampler WB = new Sampler("WB");
            this.extraSingleSamples.addAll(
                    Arrays.asList(
                            WB.get("ALetter") + WB.get("ALetter") + WB.get("ALetter"),
                            WB.get("ALetter") + WB.get("MidLetter") + WB.get("ALetter"),
                            WB.get("ALetter")
                                    + WB.get("MidLetter")
                                    + WB.get("MidLetter")
                                    + WB.get("ALetter"),
                            WB.get("Hebrew_Letter") + WB.get("Single_Quote"),
                            WB.get("Hebrew_Letter")
                                    + WB.get("Double_Quote")
                                    + WB.get("Hebrew_Letter"),
                            WB.get("ALetter")
                                    + WB.get("Numeric")
                                    + WB.get("Numeric")
                                    + WB.get("ALetter"),
                            WB.get("Numeric") + WB.get("MidNum") + WB.get("Numeric"),
                            WB.get("Numeric")
                                    + WB.get("MidNum")
                                    + WB.get("MidNum")
                                    + WB.get("Numeric"),
                            WB.get("Katakana") + WB.get("Katakana"),
                            WB.get("ALetter")
                                    + WB.get("ExtendNumLet")
                                    + WB.get("Numeric")
                                    + WB.get("ExtendNumLet")
                                    + WB.get("Katakana")
                                    + WB.get("ExtendNumLet"),
                            WB.get("ALetter")
                                    + WB.get("ExtendNumLet")
                                    + WB.get("ExtendNumLet")
                                    + WB.get("ALetter"),
                            WB.get("RI") + WB.get("RI", 2) + WB.get("RI", 3) + "b",
                            "a" + WB.get("RI") + WB.get("RI", 2) + WB.get("RI", 3) + "b",
                            "a" + WB.get("RI") + WB.get("RI", 2) + zwj + WB.get("RI", 3) + "b",
                            "a" + WB.get("RI") + zwj + WB.get("RI", 2) + WB.get("RI", 3) + "b",
                            "a"
                                    + WB.get("RI")
                                    + WB.get("RI", 2)
                                    + WB.get("RI", 3)
                                    + WB.get("RI", 4)
                                    + "b",
                            sampleEBase + sampleEMod + sampleEBase,
                            sampleEmoji + zwj + sampleEmoji,
                            "a" + zwj + sampleEmoji,
                            sampleEXP + zwj + sampleEXP,
                            "a" + zwj + sampleEXP,
                            sampleEBase + sampleEMod + sampleMn + zwj + sampleEBase + sampleEMod,
                            sampleEmoji + sampleEMod,
                            zwj + sampleEmoji + sampleEMod,
                            zwj + sampleEmoji,
                            zwj + sampleEmoji,
                            sampleEmoji + sampleEmoji,
                            "a" + sampleMn + zwj + sampleMn + "b",
                            "a  b"));

            // 1. ÷ (Numeric|ALetter) ÷ (MidLetter|MidNum|MidNumLet) ÷ (MidLetter|MidNum|MidNumLet)
            // ÷ (Numeric|ALetter) ÷
            // 2. ÷ (Numeric|ALetter) × ExtendNumLet × (Numeric|ALetter) ÷
            // (MidLetter|MidNum|MidNumLet) ÷ (MidLetter|MidNum|MidNumLet) ÷ (Numeric|ALetter) ÷
            for (String numLet : Arrays.asList("1", "a")) {
                for (String mid : Arrays.asList(":", ".", ",")) {
                    for (String mid2 : Arrays.asList(":", ".", ",")) {
                        for (String numLet2 : Arrays.asList("1", "a")) {
                            extraTestSamples.add(numLet + mid + mid2 + numLet2);
                            for (String numLet3 : Arrays.asList("1", "a")) {
                                extraTestSamples.add(numLet + "_" + numLet3 + mid + mid2 + numLet2);
                            }
                        }
                    }
                }
            }
        }

        static String[] getExtraSamples(UCD ucd, Segmenter.Target target) {
            final GenerateBreakTest grapheme = new GenerateGraphemeBreakTest(ucd, target);
            final String[] temp = {
                "can't",
                "can\u2019t",
                "ab\u00ADby",
                "a$-34,567.14%b",
                "3a",
                "c.d",
                "C.d",
                "c.D",
                "C.D",
            };
            final String[] extraSingleSamples = new String[temp.length * 2];
            System.arraycopy(temp, 0, extraSingleSamples, 0, temp.length);
            for (int i = 0; i < temp.length; ++i) {
                extraSingleSamples[i + temp.length] = insertEverywhere(temp[i], "\u2060", grapheme);
            }

            return extraSingleSamples;
        }
    }

    static final boolean DEBUG_GRAPHEMES = false;
    static final Transliterator escaper =
            Transliterator.createFromRules(
                    "escape", "::[[:di:][:c:]] any-hex/c;", Transliterator.FORWARD);

    static class MyBreakIterator {
        int offset = 0;
        String string = "";
        GenerateBreakTest breaker;
        boolean recommended = true;

        MyBreakIterator(GenerateBreakTest breaker) {
            this.breaker = breaker; //  = new GenerateGraphemeBreakTest()
        }

        public MyBreakIterator set(String source, int offset) {
            // if (DEBUG_GRAPHEMES) System.out.println(Utility.hex(string) + "; " + offset);
            string = source;
            this.offset = offset;
            return this;
        }

        public int nextBase() {
            if (offset >= string.length()) {
                return -1;
            }
            final int result = UTF16.charAt(string, offset);
            for (++offset; offset < string.length(); ++offset) {
                if (breaker.isBreak(string, offset)) {
                    break;
                }
            }
            // if (DEBUG_GRAPHEMES) System.out.println(Utility.hex(result));
            return result;
        }

        public int previousBase() {
            if (offset <= 0) {
                return -1;
            }
            for (--offset; offset >= 0; --offset) {
                if (breaker.isBreak(string, offset)) {
                    break;
                }
            }
            final int result = UTF16.charAt(string, offset);
            // if (DEBUG_GRAPHEMES) System.out.println(Utility.hex(result));
            return result;
        }
    }

    public static String[] add(String[] strings1, String... strings2) {
        final ArrayList<String> result = new ArrayList<String>(Arrays.asList(strings1));
        result.addAll(Arrays.asList(strings2));
        return result.toArray(new String[strings1.length + strings2.length]);
    }

    public String getSample(UnicodeProperty prop2, String value) {
        return getSample(prop2, value, 1);
    }

    public String getSample(UnicodeProperty prop2, String value, int count) {
        UnicodeSet us = prop2.getSet(value);
        if (prop.getName().startsWith("Extended_Pictographic")) {
            count += 30;
        }
        for (String s : us) {
            if (--count <= 0) {
                return s;
            }
        }
        throw new IllegalArgumentException();
    }
}
