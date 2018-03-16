/**
 *******************************************************************************
 * Copyright (C) 1996-2001, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /home/cvsroot/unicodetools/org/unicode/text/UCD/GenerateBreakTest.java,v $
 *
 *******************************************************************************
 */

package org.unicode.text.UCD;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;

import org.unicode.cldr.util.props.UnicodeProperty;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;
import org.unicode.props.UcdPropertyValues.Binary;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.UnicodeDataFile;
import org.unicode.text.utility.Utility;
import org.unicode.text.utility.UtilityBase;
import org.unicode.tools.Segmenter;
import org.unicode.tools.Segmenter.Builder;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

abstract public class GenerateBreakTest implements UCD_Types {

    private static final String DEBUG_STRING = "\u0001\u0061\u2060";
    private static final boolean DEBUG_RULE_REPLACEMENT = true;

    private static final String DOCTYPE =
            "<!DOCTYPE HTML PUBLIC '-//W3C//DTD HTML 4.01 Transitional//EN' 'http://www.w3.org/TR/html4/loose.dtd'>";

    // hack for now
    static final String sampleEmoji = "🛑";
    static final String sampleEXP = "✁";

    static boolean DEBUG = false;
    static final boolean SHOW_TYPE = false;
    UCD ucd;
    Normalizer nfd;
    Normalizer nfkd;

    OldUnicodeMap sampleMap = null;
    OldUnicodeMap map = new OldUnicodeMap();
    UnicodeProperty prop;

    // ====================== Main ===========================

    public static void main(String[] args) throws IOException {
        System.out.println("Remember to add length marks (half & full) and other punctuation for sentence, with FF61");
        //Default.setUCD();
        new GenerateGraphemeBreakTest(Default.ucd()).run();
        new GenerateWordBreakTest(Default.ucd()).run();
        new GenerateLineBreakTest(Default.ucd()).run();
        new GenerateSentenceBreakTest(Default.ucd()).run();
    }

    GenerateBreakTest(UCD ucd) {
        this.ucd = ucd;
        nfd = new Normalizer(UCD_Types.NFD, ucd.getVersion());
        nfkd = new Normalizer(UCD_Types.NFKD, ucd.getVersion());
        /*
        public void fillMap(String propName) {
        	List list = y.getAvailableValues();
        	for (Iterator it = list.iterator(); it.hasNext();) {
        		String label = (String) it.next();
        		map.add(label, y.getSet(label));
        	}
        }
         */
    }

    static final ToolUnicodePropertySource unicodePropertySource =
            ToolUnicodePropertySource.make(Default.ucdVersion());

    Set<String> labels = new HashSet<String>();

    int addToMap(String label) {
        labels.add(label);
        final UnicodeSet s = prop.getSet(label);
        if (s == null || s.size() == 0) {
            throw new IllegalArgumentException("Bad value: " + prop.getName() + ", " + label);
        }
        return map.add(label, s);
    }

    int addToMapLast(String label) {
        final int result = addToMap(label);
        final Set<String> values = new HashSet<String>(prop.getAvailableValues());
        if (!values.equals(labels)) {
            throw new IllegalArgumentException("Missing Property Values: " + prop.getName()
                    + ": " + values.removeAll(labels));
        }
        return result;
    }

    // COMMON STUFF for Hangul
    /*
    static final byte hNot = -1, hL = 0, hV = 1, hT = 2, hLV = 3, hLVT = 4, hLIMIT = 5;
    static final String[] hNames = {"L", "V", "T", "LV", "LVT"};


    static byte getHangulType(int cp) {
        if (ucd.isLeadingJamo(cp)) return hL;
        if (ucd.isVowelJamo(cp)) return hV;
        if (ucd.isTrailingJamo(cp)) return hT;
        if (ucd.isHangulSyllable(cp)) {
            if (ucd.isDoubleHangul(cp)) return hLV;
            return hLVT;
        }
        return hNot;
    }
     */

    /* static {
        setUCD();
    }
     */

    public static boolean onCodepointBoundary(String s, int offset) {
        if (offset < 0 || offset > s.length()) {
            return false;
        }
        if (offset == 0 || offset == s.length()) {
            return true;
        }
        if (UTF16.isLeadSurrogate(s.charAt(offset-1))
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
            if (((1<<cat) & MARK_MASK) != 0) {
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
        final UCDProperty[]  INFOPROPS = {UnifiedProperty.make(CATEGORY), UnifiedProperty.make(LINE_BREAK)};
        final GenerateBreakTest[] tests = {
                new GenerateGraphemeBreakTest(ucd),
                new GenerateWordBreakTest(ucd),
                new GenerateLineBreakTest(ucd),
        };
        tests[0].isBreak("\u0300\u0903", 1);
        final Normalizer nfd = new Normalizer(UCD_Types.NFD, ucd.getVersion());

        System.out.println("Check Decomps");
        //System.out.println("otherExtendSet: " + ((GenerateGraphemeBreakTest)tests[0]).otherExtendSet.toPattern(true));
        //Utility.showSetNames("", ((GenerateGraphemeBreakTest)tests[0]).otherExtendSet, false, ucd);

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
                        System.out.println(j  + ": " + test2.fileName);
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
                result.append(prop2.getPropertyName(SHORT)).append('=').append(prop2.getValue(cp,SHORT));
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
            final int catMask = 1<<cat;
            switch(status) {
            case 0: if ((catMask & BASE_MASK) == 0) {
                return false;
            }
            status = 1;
            break;
            case 1: if ((catMask & NONSPACING_MARK_MASK) == 0) {
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


    /*
    static UnicodeSet extraAlpha = new UnicodeSet("[\\u02B9-\\u02BA\\u02C2-\\u02CF\\u02D2-\\u02DF\\u02E5-\\u02ED\\u05F3]");
    static UnicodeSet alphabeticSet = UnifiedBinaryProperty.make(DERIVED | PropAlphabetic).getSet()
        .addAll(extraAlpha);

    static UnicodeSet ideographicSet = UnifiedBinaryProperty.make(BINARY_PROPERTIES | Ideographic).getSet();

    static {
        if (false) System.out.println("alphabetic: " + alphabeticSet.toPattern(true));
    }
     */


    void generateTerminalClosure() {
        final UnicodeSet midLetterSet = new UnicodeSet("[\u0027\u002E\u003A\u00AD\u05F3\u05F4\u2019\uFE52\uFE55\uFF07\uFF0E\uFF1A]");

        final UnicodeSet ambigSentPunct = new UnicodeSet("[\u002E\u0589\u06D4]");

        final UnicodeSet sentPunct = new UnicodeSet("[\u0021\u003F\u0387\u061F\u0964\u203C\u203D\u2048\u2049"
                + "\u3002\ufe52\ufe57\uff01\uff0e\uff1f\uff61]");

        final UnicodeSet terminals = UnifiedBinaryProperty.make(BINARY_PROPERTIES | Terminal_Punctuation).getSet();
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
        /*

        UnicodeSet sentencePunctuation = new UnicodeSet("[\u0021\003F          ; Terminal_Punctuation # Po       QUESTION MARK
037E          ; Terminal_Punctuation # Po       GREEK QUESTION MARK
061F          ; Terminal_Punctuation # Po       ARABIC QUESTION MARK
06D4          ; Terminal_Punctuation # Po       ARABIC FULL STOP
203C..203D    ; Terminal_Punctuation # Po   [2] DOUBLE EXCLAMATION MARK..INTERROBANG
3002          ; Terminal_Punctuation # Po       IDEOGRAPHIC FULL STOP
2048..2049    ; Terminal_Punctuation # Po   [2] QUESTION EXCLAMATION MARK..EXCLAMATION QUESTION MARK
         */

    }

    //============================

    protected String currentRule;
    protected String fileName;
    protected String propertyName;
    protected List<String> samples = new ArrayList<String>(); // should have one per property value, for the cross chart and before+after test
    protected List<String> extraSamples = new ArrayList<String>(); // extras that are used in before+after test
    protected List<String> extraSingleSamples = new ArrayList<String>(); // extras that are just added straight, no before+after, and also appear on charts
    protected Set<String> extraTestSamples = new LinkedHashSet<>(); // extras that are just added to tests, not to charts
    protected int tableLimit = -1;

    protected int[] skippedSamples = new int[100];
    protected boolean didSkipSamples = false;

    private final String[] ruleList = new String[100];
    private int ruleListCount = 0;
    protected boolean collectingRules = false;
    protected boolean needsFullBreakSample = true;
    protected Map<String,String> variables;

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
        //printLine(out, samples[LB_ZW], "", samples[LB_CL]);
        //printLine(out, samples[LB_ZW], " ", samples[LB_CL]);

        final UnicodeDataFile fc = UnicodeDataFile.openHTMLAndWriteHeader(MakeUnicodeFiles.MAIN_OUTPUT_DIRECTORY + "auxiliary/", fileName + "BreakTest").setSkipCopyright(Settings.SKIP_COPYRIGHT);
        final PrintWriter out = fc.out;

        /*        PrintWriter out = Utility.openPrintWriter("auxiliary/"
            + fileName + "BreakTest-"
            + ucd.getVersion()
            + ".html", Utility.UTF8_WINDOWS);
         */
        out.println(DOCTYPE);
        out.println("<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'>");
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
        out.println("<p>This page illustrates the application of the " + propertyName + " specification. "
                + "The material here is informative, not normative.</p> "
                + "<p>The first chart shows where breaks would appear between different sample characters or strings. "
                + "The sample characters are chosen mechanically to represent the different properties used by the specification.</p>"
                + "<p>Each cell shows the break-status for the position between the character(s) in its row header and the character(s) in its column header. "
                + "The × symbol indicates no break, while the ÷ symbol indicated a break. "
                + "The cells with × are also shaded to make it easier to scan the table. "
                + "For example, in the cell at the intersection of the row headed by “CR” and the column headed by “LF”, there is a × symbol, "
                + "indicating that there is no break between CR and LF.</p>");
        out.print("<p>");
        if (fileName.equals("Grapheme") || fileName.equals("Word")) {
            out.print("After the heavy blue line in the table are additional rows, either with different sample characters or for sequences"
                + (fileName.equals("Word") ? ", such as “ALetter MidLetter”. " : ". "));
        }
        out.println("Some column headers may be composed, reflecting “treat as” or “ignore” rules.</p>");
        out.print("<p>If your browser handles titles (tooltips), then hovering the mouse over the row header will show a sample character of that type. "
                + "Hovering over a column header will show the sample character, plus its abbreviated general category and script. "
                + "Hovering over the intersected cells shows the rule number that produces the break-status. "
                + "For example, hovering over the cell at the intersection of ");
        switch(fileName) {
        case "Line":
            out.print("H3 and JT shows ×, with the rule 26.03. "); break;
        case "Grapheme":
            out.print("LVT and T shows ×, with the rule 8.0. "); break;
        case "Word":
            out.print("ExtendNumLet and ALetter shows ×, with the rule 13.2. "); break;
        case "Sentence":
            out.print("ATerm and Close shows ×, with the rule 9.0. "); break;
        }
        out.print("Checking below the table, ");
        switch(fileName) {
        case "Line":
            out.print("rule 26.03 is “JT | H3 × JT”"); break;
        case "Grapheme":
            out.print("rule 8.0 is “( LVT | T) × T”"); break;
        case "Word":
            out.print("rule 13.2 is “ExtendNumLet × (AHLetter | Numeric | Katakana)”"); break;
        case "Sentence":
            out.print("rule 9.0 is “SATerm Close* × ( Close | Sp | ParaSep )”"); break;
        }
        out.println(", which is the one that applies to that case. "
                + "Note that a rule is invoked only when no lower-numbered rules have applied.</p>");
        if (fileName.equals("Line")) {
            out.println("<p>The " + propertyName + " tests use tailoring of numbers described in Example 7 of Section 8.2, “Examples of Customization” of UAX #14.</p>");
        }
        generateTable(out);


        if (false) {
            out.println("<h3>Character Type Breakdown</h3>");
            out.println("<table border='1' cellspacing='0' width='100%'>");
            for (int i = 0; i < sampleMap.size(); ++i) {
                out.println("<tr><th>" + sampleMap.getLabelFromIndex(i)
                        + "</th><td>" + sampleMap.getSetFromIndex(i)
                        + "</td></tr>");
            }
            out.println("</table>");
        }

        out.println("<hr width='50%'>\n" +
                "<div align='center'>\n" +
                "<center>\n" +
                "<table cellspacing='0' cellpadding='0' border='0'>\n" +
                "<tr>\n" +
                "<td><a href='http://www.unicode.org/unicode/copyright.html'>\n" +
                "<img src='http://www.unicode.org/img/hb_notice.gif' border='0' alt='Access to Copyright and terms of use' width='216' height='50'></a></td>\n" +
                "</tr>\n" +
                "</table>\n" +
                "<script language='Javascript' type='text/javascript' src='http://www.unicode.org/webscripts/lastModified.js'>\n" +
                "</script>\n" +
                "</center>\n" +
                "</div>");
        for (int i = 0; i < 50; ++i) {
            out.println("<br>"); // leave blank lines so scroll-to-top works.
        }
        fc.close();

        generateTest(false, fileName, propertyName);

    }

    private void generateTest(boolean shortVersion, String fileName, String propertyName) throws IOException {
        TreeMap<Double,String> rulesFound = new TreeMap<>();

        final List<String> testCases = new ArrayList<String>();
        // do main test

        final UnicodeDataFile fc = UnicodeDataFile.openAndWriteHeader(MakeUnicodeFiles.MAIN_OUTPUT_DIRECTORY + "auxiliary/", fileName + "BreakTest"
                + (shortVersion ? "_SHORT" : "")).setSkipCopyright(Settings.SKIP_COPYRIGHT);
        final PrintWriter out = fc.out;
        /*        PrintWriter out = Utility.openPrintWriter("TR29/" + fileName + "BreakTest"
            + (shortVersion ? "_SHORT" : "")
            + "-" + ucd.getVersion()
            + ".txt", Utility.UTF8_WINDOWS);
         */
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
        out.println("#\t- (x) the " + propertyName + " property value for the sample character");
        out.println("#\t- [x] the rule that determines whether there is a break or not,");
        out.println("#\t   as listed in the Rules section of " + fileName + "BreakTest.html");
        if (fileName.equals("Line")) {
            out.println("#");
            out.println("# Note:");
            out.println("#  The " + propertyName + " tests use tailoring of numbers described in");
            out.println("#  Example 7 of Section 8.2, \"Examples of Customization\" of UAX #14.");
        }
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
                    printLine(out, testCase, !shortVersion /*&& isFirst */, false, rulesFound);
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
            //throw new IllegalArgumentException
            System.err.println("***Rules missing from TESTS for " + fileName + ": " + numbers 
                    + "You will need to add samples that trigger those rules. "
                    + "See https://sites.google.com/site/unicodetools/home/changing-ucd-properties#TOC-Adding-Segmentation-Sample-Strings");
        }
        for (Entry<Double, String> entry : rulesFound.entrySet()) {
            System.out.println("\"" + escaper.transform(entry.getValue()) + "\",\t\t//" + entry.getKey());
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

    abstract public boolean isBreak(String source, int offset);

    abstract public String fullBreakSample();

    abstract public byte getType (int cp);

    public byte getSampleType (int cp) {
        return getType(cp);
    }

    public int mapType(int input) {
        return input;
    }

    public boolean highlightTableEntry(int x, int y, String s) {
        return false;
    }

    abstract public String getTypeID(int s);

    public String getTypeID(String s) {
        if (s == null) {
            return "<null>";
        }
        if (s.length() == 1) {
            return getTypeID(s.charAt(0));
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

    public String getTableEntry(String before, String after, String[] ruleOut) {
        final boolean normalBreak = isBreak(before + after, before.length());
        final String normalRule = getRule();
        ruleOut[0] = normalRule;
        return normalBreak ? BREAK : NOBREAK;
    }

    public byte getResolvedType(int cp) {
        return getType(cp);
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
            result.append(", gc=" + UCD.getCategoryID_fromIndex(ucd.getCategory(cp),SHORT));
            result.append(", sc=" + UCD.getScriptID_fromIndex(ucd.getScript(cp),SHORT));
            //result.append(", lb=" + ucd.getLineBreakID_fromIndex(ucd.getLineBreak(cp))
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
            types += "<th " + width + " class='lbclass' title='" + getInfo(after) + "'>" + h + "</th>";


            //codes += "<th " + width + " title='" + getInfo(after) + "'>" + Utility.hex(after) + "</th>";
        }

        out.println("<tr><th " + width + "></th>" + types + "</tr>");
        // out.println("<tr><th " + width + "></th><th " + width + "></th>" + codes + "</tr>");

        final String[] rule = new String[1];
        final String[] rule2 = new String[1];
        for (int type = 0; type < samples.size(); ++type) {
            if (type == tableLimit) {
                out.println("<tr><td bgcolor='#0000FF' colSpan='" + (tableLimit + 1) + "' style='font-size: 1px'>&nbsp;</td></tr>");
            }
            final String before = samples.get(type);
            if (before == null) {
                continue;
            }

            final String h = getTypeID(before);
            String line = "<tr><th class='lbclass' title='" + ucd.getCodeAndName(before) + "'>" + h + "</th>";

            for (int type2 = 0; type2 < tableLimit; ++type2) {

                final String after = samples.get(type2);
                if (after == null) {
                    continue;
                }

                final String t = getTableEntry(before, after, rule);
                String background = "";
                final String t2 = getTableEntry(before, after, rule2);
                if (highlightTableEntry(type, type2, t)) {
                    background = " bgcolor='#FFFF00'";
                }
                if (!t.equals(t2)) {
                    if (t.equals(NOBREAK)) {
                        background = " bgcolor='#CCFFFF'";
                    } else {
                        background = " bgcolor='#FFFF00'";
                    }
                } else if (t.equals(NOBREAK)) {
                    background = " bgcolor='#CCCCFF'";
                }
                line += "<th title='" + rule[0] + "'" + background + " class='pairItem'>" + t + "</th>";
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
        if (needsFullBreakSample ) {
            collectingRules = true;
            isBreak(fullBreakSample(), 1);
            collectingRules = false;
        }

        out.println("<h3>" + linkAndAnchor("rules", "Rules") + "</h3>");
        out.print("<p>This section shows the rules. They are mechanically modified for programmatic generation of the tables and test code, and"
                + " thus do not match the UAX rules precisely. "
                + "In particular:</p>"
                + "<ol>"
                + "<li>The rules are cast into a form that is more like regular expressions.</li>"
                + "<li>The rules “sot " + (fileName.equals("Line") ? "×" : "÷") + "”, “÷ eot”, and “÷ Any” are added mechanically, and have artificial numbers.</li>"
                + "<li>The rules are given decimal numbers using tenths, and are written without prefix. For example, ");
        switch(fileName) {
        case "Line":
            out.print("rule LB21a is given the number 21.1"); break;
        case "Grapheme":
            out.print("rule GB9a is given the number 9.1"); break;
        case "Word":
            out.print("rule WB13a is given the number 13.1"); break;
        case "Sentence":
            out.print("rule SB8a is given the number 8.1"); break;
        }
        out.print(".</li>"
                + "<li>Any “treat as” or “ignore” rules are handled as discussed in UAX #"
                + (fileName.equals("Line") ? "14" : "29")
                + ", and thus reflected in a transformation of the rules usually not visible here. ");
        if (fileName.equals("Line")) {
            out.print("Where it does show up, an extra variable like CM+ may appear, and the rule may be recast. ");
        }
        out.print("In addition, final rules like “Any ÷ Any” may be recast as the equivalent expression “÷ Any”.</li><li>");
        if (fileName.equals("Line")) {
            out.print("Where a rule has multiple parts (lines), each one is numbered using hundredths, "
                + "such as 21.01) × BA, 21.02) × HY, ... ");
        }
        out.println("In some cases, the numbering and form of a rule is changed due to “treat as” rules.</li>"
                + "</ol>" + "<p>For the original rules"
                + (fileName.equals("Word") || fileName.equals("Sentence") ? " and the macro values they use" : "")
                + ", see UAX #"
                + (fileName.equals("Line") ? "14" : "29")
                + ".</p>");
        //out.println("<ul style='list-style-type: none'>");
        out.println("<table>");
        // same pattern, but require _ at the end.
        final Matcher identifierMatcher = Segmenter.IDENTIFIER_PATTERN.matcher("");
        for (int ii = 0; ii < ruleListCount; ++ii) {
            String ruleString = ruleList[ii];
            int pos = 0;
            while (true) {
                if (!identifierMatcher.reset(ruleString).find(pos)) {
                    break;
                }
                final String variable = identifierMatcher.group();
                if (!variable.endsWith("_")) {
                    pos = identifierMatcher.end();
                    continue;
                }
                final String replacement = variables.get(variable);
                if (replacement == null) {
                    throw new IllegalArgumentException("Can't find variable: " + variable);
                }
                final String prefix = ruleString.substring(0,identifierMatcher.start());
                final String suffix = ruleString.substring(identifierMatcher.end());
                if (DEBUG_RULE_REPLACEMENT) {
                    System.out.println("Replacing " + prefix + "$$" + variable + "$$" + suffix + "\t by \t" + replacement);
                }
                ruleString = prefix + replacement + suffix;
                pos = identifierMatcher.start() + replacement.length();
            }
            String cleanRule = ruleString.replaceAll("[$]","");
            if (!isBreak("a",0)) {
                cleanRule = cleanRule.replace("sot ÷", "sot ×");
            }
            final int parenPos = cleanRule.indexOf(')');
            final String ruleNumber = cleanRule.substring(0,parenPos);
            final String ruleBody = cleanRule.substring(parenPos+1).trim();
            int breakPoint = ruleBody.indexOf('×');
            if (breakPoint < 0) {
                breakPoint = ruleBody.indexOf('÷');
            }
            out.println("<tr><th style='text-align:right'>" + linkAndAnchor("r" + ruleNumber, ruleNumber) + "</th>" +
                    "<td style='text-align:right'>" + ruleBody.substring(0,breakPoint)
                    + "</td><td>" + ruleBody.substring(breakPoint, breakPoint+1)
                    + "</td><td>" + ruleBody.substring(breakPoint+1)
                    + "</td></tr>");
            //out.println("<li>" + cleanRule + "</li>");
        }
        out.println("</table>");
        //out.println("</ul>");

        Map<Double, String> rulesFound = new TreeMap<>();

        if (extraSingleSamples.size() > 0) {
            out.println("<h3>" + linkAndAnchor("samples", "Sample Strings") + "</h3>");
            out.println("<p>" +
                    "The following samples illustrate the application of the rules. " +
                    "The blue lines indicate possible break points. " +
                    "If your browser supports titles (tooltips), then positioning the mouse over each character will show its name, " +
                    "while positioning between characters shows the number of the rule responsible for the break-status." +
                    "</p>");
            out.println("<table>");
            for (int ii = 0; ii < extraSingleSamples.size(); ++ii) {
                final String ruleNumber = String.valueOf(ii+1);
                out.println("<tr><th style='text-align:right'>" + linkAndAnchor("s" + ruleNumber, ruleNumber) + "</th><td><font size='5'>");
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
            final String ruleNumber = ruleString.substring(0,parenPos);
            results.add(Double.parseDouble(ruleNumber));
        }
        return results;
    }

    public String linkAndAnchor(String anchor, String text) {
        return ("<a href='#" + anchor + "' name='" + anchor + "'>" + text + "</a>");
    }

    static final String BREAK = "\u00F7";
    static final String NOBREAK = "\u00D7";

    public void printLine(PrintWriter out, String source, boolean comments, boolean html, Map<Double, String> rulesFound) {
        int cp;
        final StringBuffer string = new StringBuffer();
        final StringBuffer comment = new StringBuffer("\t# ");
        boolean hasBreak = isBreak(source, 0);
        addToRules(rulesFound, source, hasBreak);

        String status;
        if (html) {
            status = hasBreak ? " style='border-right: 1px solid blue'" : "";
            string.append("<span title='" + getRule() + "'><span" + status + ">&nbsp;</span>&nbsp;</span>");
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
                string.append("<span title='" +
                        Utility.quoteXML(ucd.getCodeAndName(cp) + " (" + getTypeID(cp) + ")", true)
                        + "'>"
                        + Utility.quoteXML(UtilityBase.getDisplay(cp), true)
                        + "</span>");
                string.append("<span title='" + getRule() + "'><span" + status + ">&nbsp;</span>&nbsp;</span>");
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

        // what we want is a list of sample characters. In the simple case, this is just one per type.
        // However, if there are characters that have different types (when recommended or not), then
        // we want a type for each cross-section

        /**
         * Set of lb values that already have sample characters.
         * Faster to test than Map.contains(lb) because
         * it avoids creating an Integer object for each code point.
         */
        final BitSet bitset = new BitSet();
        /**
         * Maps lb values to sample characters.
         * We do not really need this map -- we could add each sample character directly to samples --
         * but adding them in sorted order by lb value stabilizes the output.
         */
        final Map<Integer, String> lbToSampleChar = new TreeMap<Integer, String>();

        for (int i = 1; i <= 0x10FFFF; ++i) {
            if (!ucd.isAllocated(i)) {
                continue;
            }
            if (0xD800 <= i && i <= 0xDFFF) {
                continue;
            }
            if (DEBUG && i == 0x1100) {
                System.out.println("debug");
            }
            final byte lb = getSampleType(i);
            if (skipType(lb)) {
                skippedSamples[lb] = i;
                didSkipSamples = true;
                continue;
            }

            if (!bitset.get(lb)) {
                bitset.set(lb);
                // Unassigned U+50005 is better than PUA U+E000
                // because implementations may override PUA properties.
                // A supplementary code point also adds to implementation code coverage.
                String sample;
                if (i == 0xE000 && getSampleType(0x50005) == lb) {
                    sample = UTF16.valueOf(0x50005);
                } else {
                    sample = UTF16.valueOf(i);
                }
                lbToSampleChar.put(new Integer(lb), sample);
            }
        }

        samples.addAll(lbToSampleChar.values());

        tableLimit = samples.size();

        // now add values that are different
        if (extraSamples.size() > 0) {
            samples.addAll(extraSamples);
        }
    }

    public int findLastNon(String source, int offset, byte notLBType) {
        int cp;
        for (int i = offset-1; i >= 0; i -= UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(source, i);
            final byte f = getResolvedType(cp);
            if (f != notLBType) {
                return i;
            }
        }
        return -1;
    }

    public static UnicodeSet getSet(UCD ucd, int prop, byte propValue) {
        return UnifiedBinaryProperty.make(prop | propValue, ucd).getSet();
    }

    static public class Context {
        public int cpBefore2, cpBefore, cpAfter, cpAfter2;
        public byte tBefore2, tBefore, tAfter, tAfter2;
        @Override
        public String toString() {
            return "["
                    + Utility.hex(cpBefore2) + "(" + tBefore2 + "), "
                    + Utility.hex(cpBefore) + "(" + tBefore + "), "
                    + Utility.hex(cpAfter) + "(" + tAfter + "), "
                    + Utility.hex(cpAfter2) + "(" + tAfter2 + ")]";
        }
    }

    public void getGraphemeBases(MyBreakIterator graphemeIterator, String source, int offset, int ignoreType, Context context) {
        context.cpBefore2 = context.cpBefore = context.cpAfter = context.cpAfter2 = -1;
        context.tBefore2 = context.tBefore = context.tAfter = context.tAfter2 = -1;
        //if (DEBUG_GRAPHEMES) System.out.println(Utility.hex(source) + "; " + offset + "; " + ignoreType);

        //MyBreakIterator graphemeIterator = new MyBreakIterator(new GenerateGraphemeBreakTest(ucd));

        graphemeIterator.set(source, offset);
        while (true) {
            final int cp = graphemeIterator.previousBase();
            if (cp == -1) {
                break;
            }
            final byte t = getResolvedType(cp);
            if (t == ignoreType) {
                continue;
            }

            if (context.cpBefore == -1) {
                context.cpBefore = cp;
                context.tBefore = t;
            } else {
                context.cpBefore2 = cp;
                context.tBefore2 = t;
                break;
            }
        }
        graphemeIterator.set(source, offset);
        while (true) {
            final int cp = graphemeIterator.nextBase();
            if (cp == -1) {
                break;
            }
            final byte t = getResolvedType(cp);
            if (t == ignoreType) {
                continue;
            }

            if (context.cpAfter == -1) {
                context.cpAfter = cp;
                context.tAfter = t;
            } else {
                context.cpAfter2 = cp;
                context.tAfter2 = t;
                break;
            }
        }
    }


    //==============================================

    static class XGenerateBreakTest extends GenerateBreakTest {
        Segmenter seg;
        String sample;
        {
            needsFullBreakSample = false;
        }

        public XGenerateBreakTest(UCD ucd, Segmenter.Builder segBuilder, String sample, String filename,
                String[] extraSamples, String[] extraSingleSamples) {
            super(ucd);
            seg = segBuilder.make();
            this.sample = sample;
            final List<String> rules = segBuilder.getRules();
            collectingRules = true;
            for (final Iterator<String> it = rules.iterator(); it.hasNext();) {
                final String rule = it.next();
                setRule(rule);
            }
            variables = segBuilder.getOriginalVariables();
            collectingRules = false;
            map.add("Other", new UnicodeSet(0,0x10FFFF));
            final UnicodeMap<String> segSamples = seg.getSamples();
            final Collection<String> x = segSamples.getAvailableValues();
            for (final Iterator<String> it = x.iterator(); it.hasNext();) {
                final String label = it.next();
                map.add(label, segSamples.keySet(label), true, false);
            }
            fileName = filename;
            propertyName = (filename.equals("Grapheme") ? "Grapheme_Cluster" : fileName)
                    + "_Break";
            sampleMap = map;
            this.extraSamples.addAll(Arrays.asList(extraSamples));

            this.extraSingleSamples.addAll(Arrays.asList(
                    "\r\na\n\u0308",
                    "a\u0308",
                    " \u200D\u0646",
                    "\u0646\u200D "
                    ));
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
            return map.getLabel(cp);
        }

        // stuff that subclasses need to override
        @Override
        public byte getType(int cp) {
            return (byte) map.getIndex(cp);
        }
    }

    static class Sampler {
        final UnicodeProperty prop;
        Sampler(String propName) {
            prop = unicodePropertySource.getProperty(propName);
        }
        String get(String value) {
            return get(value,1);
        }
        String get(String value, int count) {
            for (String s : prop.getSet(value)) {
                if (--count == 0) {
                    return s;
                }
            }
            throw new IllegalArgumentException(prop.getName() + ":" + value 
                    + " doesn't have " + count + " values");
        }
    }

    static final IndexUnicodeProperties IUP = IndexUnicodeProperties.make(Default.ucdVersion());
    static final String sampleEBase = IUP.load(UcdProperty.Emoji_Modifier_Base).keySet().iterator().next();
    static final String sampleEMod = IUP.load(UcdProperty.Emoji_Modifier).keySet().iterator().next();

    static class GenerateGraphemeBreakTest extends XGenerateBreakTest {
        public GenerateGraphemeBreakTest(UCD ucd) {
            super(ucd, 
                    Segmenter.make(ToolUnicodePropertySource.make(ucd.getVersion()),"GraphemeClusterBreak"), 
                    "aa", 
                    "Grapheme",
                    new String[]{unicodePropertySource.getSet("GC=Cn").iterator().next(), "\uD800"},
                    new String[]{});
            
            Sampler GCB = new Sampler("GCB");
            this.extraSingleSamples.addAll(Arrays.asList(
                    GCB.get("L") + GCB.get("L"),
                    GCB.get("LV") + GCB.get("T") + GCB.get("L"),
                    GCB.get("LVT") + GCB.get("T") + GCB.get("L"),
                    GCB.get("RI") + GCB.get("RI",2) + GCB.get("RI",3) + "b",
                    "a" + GCB.get("RI") + GCB.get("RI",2) + GCB.get("RI",3) + "b",
                    "a" + GCB.get("RI") + GCB.get("RI",2) + GCB.get("ZWJ") + GCB.get("RI",3) + "b",
                    "a" + GCB.get("RI") + GCB.get("ZWJ") + GCB.get("RI",2) + GCB.get("RI",3) + "b",
                    "a" + GCB.get("RI") + GCB.get("RI",2) + GCB.get("RI",3) + GCB.get("RI",4) + "b",
                    "a" + GCB.get("ZWJ"),
                    "a" + "\u0308" + "b",
                    "a" + GCB.get("SpacingMark") + "b",
                    "a" + GCB.get("Prepend") + "b",
                    //"a" + GCB.get("LinkingConsonant") + GCB.get("Virama") + GCB.get("LinkingConsonant") + "b",
                    //"a" + GCB.get("Virama") + GCB.get("LinkingConsonant") + "b",
                    //"a" + GCB.get("ZWJ") + GCB.get("LinkingConsonant") + "b",
                    
                    //GCB.get("E_Base") + GCB.get("E_Modifier") + GCB.get("E_Base"),
                    //"a" + GCB.get("E_Modifier") + GCB.get("E_Base"),
                    
                    sampleEBase + sampleEMod + sampleEBase,
                    "a" + sampleEMod + sampleEBase,
                    "a" + sampleEMod + sampleEBase + GCB.get("ZWJ") + sampleEmoji,

                    sampleEmoji + GCB.get("ZWJ") + sampleEmoji,
                    "a" + GCB.get("ZWJ") + sampleEmoji,
                    sampleEXP + GCB.get("ZWJ") + sampleEXP,
                    "a" + GCB.get("ZWJ") + sampleEXP

                    //GCB.get("ZWJ") + GCB.get("EBG") + GCB.get("E_Modifier"),
                    //GCB.get("ZWJ") + GCB.get("Glue_After_Zwj"),
                    //GCB.get("ZWJ") + GCB.get("EBG"),
                    //GCB.get("EBG") + GCB.get("EBG")
                    ));
        }
    }

    static class GenerateLineBreakTest extends XGenerateBreakTest {
        public GenerateLineBreakTest(UCD ucd) {
            super(ucd, Segmenter.make(ToolUnicodePropertySource.make(ucd.getVersion()),"LineBreak"), "aa", "Line",
                    new String[]{}, 
                    new String[]{
                "\u000Bぁ",     //4.0
                "\rぁ",     //5.02
                "\u0085ぁ",     //5.04
                "\u200D☝",     //8.1
                "ぁ\u2060",     //11.01
                "\u2060ぁ",     //11.02
                "ぁ̈ ",      //12.2
                "\u200D ",     //12.3
                "\u200D/",     //13.04
                "——",       //17.0
                "ぁ￼",       //20.01
                "￼ぁ",       //20.02
                "ぁ-",       //21.02
                "ก․",       //22.01
                "!․",       //22.02
                "․․",       //22.04
                "0․",       //22.05
                "☝%",       //23.01
                "ก0",       //23.02
                "$☝",       //24.01
                "$ก",       //24.02
                "%ก",       //24.03
                "ᄀ\u1160",     //26.01
                "\u1160\u1160",       //26.02
                "ᆨᆨ",       //26.03
                "\u1160․",     //27.01
                "\u1160%",     //27.02
                "$\u1160",     //27.03
                "☝🏻",      //30.2
                "final",        //999.0

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
            });
        }
        @Override
        public boolean isBreak(String source, int offset) {
            return offset == 0 ? false : super.isBreak(source, offset);
        }
        @Override
        public List<String> genTestItems(String before, String after, List<String> results) {
            super.genTestItems(before,after,results);
            results.add(before + " " + after);
            return results;
        }
    }

    static class GenerateSentenceBreakTest extends XGenerateBreakTest {
        public GenerateSentenceBreakTest(UCD ucd) {
            super(ucd, makeSegmenter(ucd), "aa", "Sentence",
                    new String[]{},
                    getExtraSamples());
        }
        private static Builder makeSegmenter(UCD ucd) {
            final Builder result = Segmenter.make(ToolUnicodePropertySource.make(ucd.getVersion()),"SentenceBreak");
            final Segmenter segmenter = result.make();
            final boolean failure = segmenter.breaksAt("etc.)\u2019 \u2018(the", 7);
            if (failure) {
                throw new IllegalArgumentException();
            }
            return result;
        }
        static String[] getExtraSamples() {
            final GenerateBreakTest grapheme = new GenerateGraphemeBreakTest(Default.ucd());
            String[] extraSingleSamples = new String[] {
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
            };
            final String[] temp = new String [extraSingleSamples.length * 2];
            System.arraycopy(extraSingleSamples, 0, temp, 0, extraSingleSamples.length);
            for (int i = 0; i < extraSingleSamples.length; ++i) {
                temp[i+extraSingleSamples.length] = insertEverywhere(extraSingleSamples[i], "\u2060", grapheme);
            }
            extraSingleSamples = temp;
            return extraSingleSamples;
        }
    }

    static class GenerateWordBreakTest extends XGenerateBreakTest {
        public GenerateWordBreakTest(UCD ucd) {
            super(ucd, 
                    Segmenter.make(ToolUnicodePropertySource.make(ucd.getVersion()),"WordBreak"), 
                    "aa", 
                    "Word",
                    new String[] {
                /*"\uFF70", "\uFF65", "\u30FD", */ "a\u2060",
                "a:",
                "a'",
                "a'\u2060",
                "a,",
                "1:",
                "1'",
                "1,",
                "1.\u2060",
            },
            new String[]{}
            );
            System.out.println();
            Sampler WB = new Sampler("WB");
            this.extraSingleSamples.addAll(Arrays.asList(
                    WB.get("ALetter") + WB.get("ALetter") + WB.get("ALetter"),
                    WB.get("ALetter") + WB.get("MidLetter") + WB.get("ALetter"),
                    WB.get("ALetter") + WB.get("MidLetter") + WB.get("MidLetter") + WB.get("ALetter"),
                    WB.get("Hebrew_Letter") + WB.get("Single_Quote"),
                    WB.get("Hebrew_Letter") + WB.get("Double_Quote") + WB.get("Hebrew_Letter"),
                    WB.get("ALetter") + WB.get("Numeric") + WB.get("Numeric") + WB.get("ALetter"),
                    WB.get("Numeric") + WB.get("MidNum") + WB.get("Numeric"),
                    WB.get("Numeric") + WB.get("MidNum") + WB.get("MidNum") + WB.get("Numeric"),
                    WB.get("Katakana") + WB.get("Katakana"),
                    WB.get("ALetter") + WB.get("ExtendNumLet") 
                    + WB.get("Numeric") + WB.get("ExtendNumLet") 
                    + WB.get("Katakana") + WB.get("ExtendNumLet"),
                    WB.get("ALetter") + WB.get("ExtendNumLet") + WB.get("ExtendNumLet") + WB.get("ALetter"),
                    WB.get("RI") + WB.get("RI",2) + WB.get("RI",3) + "b",
                    "a" + WB.get("RI") + WB.get("RI",2) + WB.get("RI",3) + "b",
                    "a" + WB.get("RI") + WB.get("RI",2) + WB.get("ZWJ") + WB.get("RI",3) + "b",
                    "a" + WB.get("RI") + WB.get("ZWJ") + WB.get("RI",2) + WB.get("RI",3) + "b",
                    "a" + WB.get("RI") + WB.get("RI",2) + WB.get("RI",3) + WB.get("RI",4) + "b",
                    sampleEBase + sampleEMod + sampleEBase,
                    
                    sampleEmoji + WB.get("ZWJ") + sampleEmoji,
                    "a" + WB.get("ZWJ") + sampleEmoji,
                    sampleEXP + WB.get("ZWJ") + sampleEXP,
                    "a" + WB.get("ZWJ") + sampleEXP,

                    sampleEmoji + sampleEMod,
                    WB.get("ZWJ") + sampleEmoji + sampleEMod,
                    WB.get("ZWJ") +sampleEmoji,
                    WB.get("ZWJ") + sampleEmoji,
                    sampleEmoji + sampleEmoji,
                    "a" + "\u0308" + WB.get("ZWJ") + "\u0308" + "b",
                    "a  b"
                    ));

            // 1. ÷ (Numeric|ALetter) ÷ (MidLetter|MidNum|MidNumLet) ÷ (MidLetter|MidNum|MidNumLet) ÷ (Numeric|ALetter) ÷
            // 2. ÷ (Numeric|ALetter) × ExtendNumLet × (Numeric|ALetter) ÷ (MidLetter|MidNum|MidNumLet) ÷ (MidLetter|MidNum|MidNumLet) ÷ (Numeric|ALetter) ÷
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
        static String[] getExtraSamples() {
            final GenerateBreakTest grapheme = new GenerateGraphemeBreakTest(Default.ucd());
            final String [] temp = {
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
            final String[] extraSingleSamples = new String [temp.length * 2];
            System.arraycopy(temp, 0, extraSingleSamples, 0, temp.length);
            for (int i = 0; i < temp.length; ++i) {
                extraSingleSamples[i+temp.length] = insertEverywhere(temp[i], "\u2060", grapheme);
            }

            return extraSingleSamples;
        }
    }

    //static class OLDGenerateGraphemeBreakTest extends GenerateBreakTest {

    //OLDGenerateGraphemeBreakTest(UCD ucd) {
    //super(ucd);
    //fileName = "Grapheme";
    //sampleMap = map;
    //}

    //Object foo = prop = unicodePropertySource.getProperty("Grapheme_Cluster_Break");

    //final int
    //CR =    addToMap("CR"),
    //LF =    addToMap("LF"),
    //Control = addToMap("Control"),
    //Extend = addToMap("Extend"),
    //L =     addToMap("L"),
    //V =     addToMap("V"),
    //T =     addToMap("T"),
    //LV =    addToMap("LV"),
    //LVT =   addToMap("LVT"),
    //Other = addToMapLast("Other");

    //// stuff that subclasses need to override
    //public String getTypeID(int cp) {
    //return map.getLabel(cp);
    //}

    //// stuff that subclasses need to override
    //public byte getType(int cp) {
    //return (byte) map.getIndex(cp);
    //}

    //public String fullBreakSample() {
    //return "aa";
    //}

    //public boolean isBreak(String source, int offset) {

    //setRule("1: sot ÷");
    //if (offset < 0 || offset > source.length()) return false;
    //if (offset == 0) return true;

    //setRule("2: ÷ eot");
    //if (offset == source.length()) return true;

    //// UTF-16: never break in the middle of a code point
    //if (!onCodepointBoundary(source, offset)) return false;

    //// now get the character before and after, and their types


    //int cpBefore = UTF16.charAt(source, offset-1);
    //int cpAfter = UTF16.charAt(source, offset);

    //byte before = getResolvedType(cpBefore);
    //byte after = getResolvedType(cpAfter);

    //setRule("3: CR × LF");
    //if (before == CR && after == LF) return false;

    //setRule("4: ( Control | CR | LF ) ÷");
    //if (before == CR || before == LF || before == Control) return true;

    //setRule("5: ÷ ( Control | CR | LF )");
    //if (after == Control || after == LF || after == CR) return true;

    //setRule("6: L × ( L | V | LV | LVT )");
    //if (before == L && (after == L || after == V || after == LV || after == LVT)) return false;

    //setRule("7: ( LV | V ) × ( V | T )");
    //if ((before == LV || before == V) && (after == V || after == T)) return false;

    //setRule("8: ( LVT | T ) × T");
    //if ((before == LVT || before == T) && (after == T)) return false;

    //setRule("9: × Extend");
    //if (after == Extend) return false;

    //// Otherwise break after all characters.
    //setRule("10: Any ÷ Any");
    //return true;

    //}

    //}

    //==============================================

    //static class XGenerateWordBreakTest extends GenerateBreakTest {

    //GenerateGraphemeBreakTest grapheme;
    //MyBreakIterator breaker;
    //Context context = new Context();

    //XGenerateWordBreakTest(UCD ucd) {
    //super(ucd);
    //grapheme = new GenerateGraphemeBreakTest(ucd);
    //breaker = new MyBreakIterator(grapheme);
    //fileName = "Word";
    //sampleMap = map;
    //extraSamples = new String[] {
    ///*"\uFF70", "\uFF65", "\u30FD", */ "a\u2060", "a:", "a'", "a'\u2060", "a,", "1:", "1'", "1,",  "1.\u2060"
    //};

    //String [] temp = {"can't", "can\u2019t", "ab\u00ADby", "a$-34,567.14%b", "3a" };
    //extraSingleSamples = new String [temp.length * 2];
    //System.arraycopy(temp, 0, extraSingleSamples, 0, temp.length);
    //for (int i = 0; i < temp.length; ++i) {
    //extraSingleSamples[i+temp.length] = insertEverywhere(temp[i], "\u2060", grapheme);
    //}

    //if (false) Utility.showSetDifferences("Katakana", map.getSetFromIndex(Katakana),
    //"Script=Katakana", getSet(ucd, SCRIPT, KATAKANA_SCRIPT), false, ucd);

    //}

    //Object foo = prop = unicodePropertySource.getProperty("Word_Break");

    ////static String LENGTH = "[\u30FC\uFF70]";
    ////static String HALFWIDTH_KATAKANA = "[\uFF66-\uFF9F]";
    ////static String KATAKANA_ITERATION = "[\u30FD\u30FE]";
    ////static String HIRAGANA_ITERATION = "[\u309D\u309E]";

    //final int
    //Format =    addToMap("Format"),
    //Katakana =    addToMap("Katakana"),
    //ALetter = addToMap("ALetter"),
    //MidLetter = addToMap("MidLetter"),
    ////MidNumLet =     addToMap("MidNumLet"),
    //MidNum =     addToMap("MidNum"),
    //Numeric =     addToMap("Numeric"),
    //ExtendNumLet =     addToMap("ExtendNumLet"),
    //Other = addToMapLast("Other");

    //// stuff that subclasses need to override
    //public String getTypeID(int cp) {
    //return map.getLabel(cp);
    //}

    //// stuff that subclasses need to override
    //public byte getType(int cp) {
    //return (byte) map.getIndex(cp);
    //}

    //public String fullBreakSample() {
    //return " a";
    //}

    //public int genTestItems(String before, String after, String[] results) {
    //results[0] = before + after;
    //results[1] = 'a' + before + "\u0301\u0308" + after + "\u0301\u0308" + 'a';
    //results[2] = 'a' + before + "\u0301\u0308" + samples[MidLetter] + after + "\u0301\u0308" + 'a';
    //results[3] = 'a' + before + "\u0301\u0308" + samples[MidNum] + after + "\u0301\u0308" + 'a';
    //return 3;
    //}

    //public boolean isBreak(String source, int offset) {

    //setRule("1: sot ÷");
    //if (offset < 0 || offset > source.length()) return false;

    //if (offset == 0) return true;

    //setRule("2: ÷ eot");
    //if (offset == source.length()) return true;

    //// Treat a grapheme cluster as if it were a single character:
    //// the first base character, if there is one; otherwise the first character.

    //setRule("3: GC -> FC");
    //if (!grapheme.isBreak( source,  offset)) return false;

    //setRule("4: X Format* -> X");
    //byte afterChar = getResolvedType(source.charAt(offset));
    //if (afterChar == Format) return false;

    //// now get the base character before and after, and their types

    //getGraphemeBases(breaker, source, offset, Format, context);

    //byte before = context.tBefore;
    //byte after = context.tAfter;
    //byte before2 = context.tBefore2;
    //byte after2 = context.tAfter2;

    ////Don't break between most letters

    //setRule("5: ALetter × ALetter");
    //if (before == ALetter && after == ALetter) return false;

    //// Don’t break letters across certain punctuation

    //setRule("6: ALetter × MidLetter ALetter");
    //if (before == ALetter && after == MidLetter && after2 == ALetter) return false;

    //setRule("7: ALetter (MidLetter | MidNumLet) × ALetter");
    //if (before2 == ALetter && before == MidLetter && after == ALetter) return false;

    //// Don’t break within sequences of digits, or digits adjacent to letters.

    //setRule("8: Numeric × Numeric");
    //if (before == Numeric && after == Numeric) return false;

    //setRule("9: ALetter × Numeric");
    //if (before == ALetter && after == Numeric) return false;

    //setRule("10: Numeric × ALetter");
    //if (before == Numeric && after == ALetter) return false;


    //// Don’t break within sequences like: '-3.2'
    //setRule("11: Numeric (MidNum | MidNumLet) × Numeric");
    //if (before2 == Numeric && before == MidNum && after == Numeric) return false;

    //setRule("12: Numeric × (MidNum | MidNumLet) Numeric");
    //if (before == Numeric && after == MidNum && after2 == Numeric) return false;

    //// Don't break between Katakana

    //setRule("13: Katakana × Katakana");
    //if (before == Katakana && after == Katakana) return false;

    //// Do not break from extenders
    //setRule("13a: (ALetter | Numeric | Katakana | ExtendNumLet)  	×  	ExtendNumLet");
    //if ((before == ALetter || before == Numeric || before == Katakana || before == ExtendNumLet) && after == ExtendNumLet) return false;

    //setRule("13b: ExtendNumLet 	× 	(ALetter | Numeric | Katakana)");
    //if (before == ExtendNumLet && (after == ALetter || after == Numeric || after == Katakana)) return false;

    //// Otherwise break always.
    //setRule("14: Any ÷ Any");
    //return true;

    //}

    //}

    // ========================================

    //static class XGenerateLineBreakTest extends GenerateBreakTest {

    //GenerateGraphemeBreakTest grapheme;
    //MyBreakIterator breaker;
    //Context context = new Context();

    //XGenerateLineBreakTest(UCD ucd) {
    //super(ucd);
    //grapheme = new GenerateGraphemeBreakTest(ucd);
    //breaker = new MyBreakIterator(grapheme);

    //sampleMap = map;
    //fileName = "Line";
    //extraSingleSamples = new String[] {"can't", "can\u2019t", "ab\u00ADby",
    //"-3",
    //"e.g.",
    //"\u4e00.\u4e00.",
    //"a  b",
    //"a  \u200bb",
    //"a \u0308b",
    //"1\u0308b(a)-(b)",
    //};
    //}

    //// all the other items are supplied in UCD_TYPES

    ///*static byte LB_L = LB_LIMIT + hL, LB_V = LB_LIMIT + hV, LB_T = LB_LIMIT + hT,
    //LB_LV = LB_LIMIT + hLV, LB_LVT = LB_LIMIT + hLVT, LB_SUP = LB_LIMIT + hLIMIT,
    //LB2_LIMIT = (byte)(LB_SUP + 1);
    //*/

    ///*
    //private byte[] AsmusOrderToMyOrder = {
    //LB_OP, LB_CL, LB_QU, LB_GL, LB_NS, LB_EX, LB_SY, LB_IS, LB_PR, LB_PO,
    //LB_NU, LB_AL, LB_ID, LB_IN, LB_HY, LB_BA, LB_BB, LB_B2, LB_ZW, LB_CM,
    //// missing from Pair Table
    //LB_SP, LB_BK, LB_CR, LB_LF,
    //// resolved types below
    //LB_CB, LB_AI, LB_SA, LB_SG, LB_XX,
    //// 3 JAMO CLASSES, plus supplementary
    //LB_L, LB_V, LB_T, LB_LV, LB_LVT, LB_SUP
    //};

    //private byte[] MyOrderToAsmusOrder = new byte[AsmusOrderToMyOrder.length];
    //{
    //for (byte i = 0; i < AsmusOrderToMyOrder.length; ++i) {
    //MyOrderToAsmusOrder[AsmusOrderToMyOrder[i]] = i;
    //}
    //*/

    //{
    ////System.out.println("Adding Linebreak");
    //for (int i = 0; i <= 0x10FFFF; ++i) {
    //map.put(i, ucd.getLineBreak(i));
    //}
    //for (int i = 0; i < LB_LIMIT; ++i) {
    //map.setLabel(i, ucd.getLineBreakID_fromIndex((byte)i, SHORT));
    //}
    ////System.out.println(map.getSetFromIndex(LB_CL));
    ////System.out.println("Done adding Linebreak");
    //}

    //public int mapType(int input) {
    //int old = input;
    //switch (input) {
    //case LB_BA: input = 16; break;
    //case LB_BB: input = 17; break;
    //case LB_B2: input = 18; break;
    //case LB_ZW: input = 19; break;
    //case LB_CM: input = 20; break;
    //case LB_WJ: input = 21; break;

    //case LB_SP: input = 22; break;
    //case LB_BK: input = 23; break;
    //case LB_NL: input = 24; break;
    //case LB_CR: input = 25; break;
    //case LB_LF: input = 26; break;

    //case LB_CB: input = 27; break;
    //case LB_SA: input = 28; break;
    //case LB_AI: input = 29; break;
    //case LB_SG: input = 30; break;
    //}
    ////if (old != input) System.out.println(old + " => " + input);
    //return input;
    //}


    //public void sampleDescription(PrintWriter out) {
    //out.println("# Samples:");
    //out.println("# The test currently takes all pairs of linebreak types*,");
    //out.println("# picks a sample for each type, and generates three strings: ");
    //out.println("#\t- the pair alone");
    //out.println("#\t- the pair alone with an imbeded space");
    //out.println("#\t- the pair alone with embedded combining marks");
    //out.println("# The sample for each type is simply the first code point (above NULL)");
    //out.println("# with that property.");
    //out.println("# * Note:");
    //out.println("#\t- SG is omitted");
    //out.println("#\t- 3 different Jamo characters and a supplementary character are added");
    //out.println("#\t  The syllable types for the Jamo (L, V, T) are displayed in comments");
    //out.println("#\t  instead of the linebreak property");
    //out.println("#");
    //}

    //// stuff that subclasses need to override
    //public int genTestItems(String before, String after, String[] results) {
    //results[0] = before + after;
    //results[1] = before + " " + after;
    //results[2] = before + "\u0301\u0308" + after;
    //return 3;
    //}

    //// stuff that subclasses need to override
    //boolean skipType(int type) {
    //return type == LB_AI || type == LB_SA || type == LB_SG || type == LB_XX
    //|| type == LB_CB || type == LB_CR || type == LB_BK || type == LB_LF
    //|| type == LB_NL || type == LB_SP;
    //}

    //// stuff that subclasses need to override
    //public String getTypeID(int cp) {
    ///*
    //byte result = getType(cp);
    //if (result == LB_SUP) return "SUP";
    //if (result >= LB_LIMIT) return hNames[result - LB_LIMIT];
    //*/
    //// return ucd.getLineBreakID_fromIndex(cp); // AsmusOrderToMyOrder[result]);
    //return ucd.getLineBreakID(cp); // AsmusOrderToMyOrder[result]);
    //}

    //public String fullBreakSample() {
    //return ")a";
    //}

    //// stuff that subclasses need to override
    //public byte getType(int cp) {
    ///*if (cp > 0xFFFF) return LB_SUP;
    //byte result = getHangulType(cp);
    //if (result != hNot) return (byte)(result + LB_LIMIT);
    //*/
    //// return MyOrderToAsmusOrder[ucd.getLineBreak(cp)];
    //return ucd.getLineBreak(cp);
    //}

    //public String getTableEntry(String before, String after, String[] ruleOut) {
    //String t = "_"; // break
    //boolean spaceBreak = isBreak(before + " " + after, before.length()+1);
    //String spaceRule = getRule();

    //boolean spaceBreak2 = isBreak(before + " " + after, before.length());
    //String spaceRule2 = getRule();

    //boolean normalBreak = isBreak(before + after, before.length());
    //String normalRule = getRule();

    //ruleOut[0] = normalRule;
    //if (!normalBreak) {
    //if (!spaceBreak && !spaceBreak2) {
    //t = "^"; // don't break, even with intervening spaces
    //} else {
    //t = "%"; // don't break, but break with intervening spaces
    //}
    //if (!spaceRule2.equals(normalRule)) {
    //ruleOut[0] += " [" + spaceRule2 + "]";
    //}
    //if (!spaceRule.equals(normalRule) && !spaceRule.equals(spaceRule2)) {
    //ruleOut[0] += " {" + spaceRule + "}";
    //}
    //}
    //return t;
    //}

    //public boolean highlightTableEntry(int x, int y, String s) {
    //return false;
    ///*
    //try {
    //return !oldLineBreak[x][y].equals(s);
    //} catch (Exception e) {}
    //return true;
    //*/
    //}

    ///*
    //String[][] oldLineBreak = {
    //{"^",	"^",	"^",	"^",	"^",	"^",	"^",	"^",	"^",	"^",	"^",	"^",	"^",	"^",	"^",	"^",	"^",	"^",	"^",	"%"},
    //{"_",	"^",	"%",	"%",	"^",	"^",	"^",	"^",	"",	"%",	"_",	"_",	"_",	"_",	"%",	"%",	"_",	"_",	"^",	"%"},
    //{"^",	"^",	"%",	"%",	"%",	"^",	"^",	"^",	"%",	"%",	"%",	"%",	"%",	"%",	"%",	"%",	"%",	"%",	"^",	"%"},
    //{"%",	"^",	"%",	"%",	"%",	"^",	"^",	"^",	"%",	"%",	"%",	"%",	"%",	"%",	"%",	"%",	"%",	"%",	"^",	"%"},
    //{"_",	"^",	"%",	"%",	"%",	"^",	"^",	"^",	"_",	"_",	"_",	"_",	"_",	"_",	"%",	"%",	"_",	"_",	"^",	"%"},
    //{"_",	"^",	"%",	"%",	"%",	"^",	"^",	"^",	"_",	"_",	"_",	"_",	"_",	"_",	"%",	"%",	"_",	"_",	"^",	"%"},
    //{"_",	"^",	"%",	"%",	"%",	"^",	"^",	"^",	"_",	"_",	"%",	"_",	"_",	"_",	"%",	"%",	"_",	"_",	"^",	"%"},
    //{"_",	"^",	"%",	"%",	"%",	"^",	"^",	"^",	"_",	"_",	"%",	"_",	"_",	"_",	"%",	"%",	"_",	"_",	"^",	"%"},
    //{"%",	"^",	"%",	"%",	"%",	"^",	"^",	"^",	"_",	"_",	"%",	"%",	"%",	"_",	"%",	"%",	"_",	"_",	"^",	"%"},
    //{"_",	"^",	"%",	"%",	"%",	"^",	"^",	"^",	"_",	"_",	"_",	"_",	"_",	"_",	"%",	"%",	"_",	"_",	"^",	"%"},
    //{"_",	"^",	"%",	"%",	"%",	"^",	"^",	"^",	"_",	"%",	"%",	"%",	"_",	"%",	"%",	"%",	"_",	"_",	"^",	"%"},
    //{"_",	"^",	"%",	"%",	"%",	"^",	"^",	"^",	"_",	"_",	"%",	"%",	"_",	"%",	"%",	"%",	"_",	"_",	"^",	"%"},
    //{"_",	"^",	"%",	"%",	"%",	"^",	"^",	"^",	"_",	"%",	"_",	"_",	"_",	"%",	"%",	"%",	"_",	"_",	"^",	"%"},
    //{"_",	"^",	"%",	"%",	"%",	"^",	"^",	"^",	"_",	"_",	"_",	"_",	"_",	"%",	"%",	"%",	"_",	"_",	"^",	"%"},
    //{"_",	"^",	"%",	"%",	"%",	"^",	"^",	"^",	"_",	"_",	"_",	"_",	"_",	"_",	"%",	"%",	"_",	"_",	"^",	"%"},
    //{"_",	"^",	"%",	"%",	"%",	"^",	"^",	"^",	"_",	"_",	"_",	"_",	"_",	"_",	"%",	"%",	"_",	"_",	"^",	"%"},
    //{"%",	"^",	"%",	"%",	"%",	"^",	"^",	"^",	"%",	"%",	"%",	"%",	"%",	"%",	"%",	"%",	"%",	"%",	"^",	"%"},
    //{"_",	"^",	"%",	"%",	"%",	"^",	"^",	"^",	"_",	"_",	"_",	"_",	"_",	"_",	"%",	"%",	"_",	"^",	"^",	"%"},
    //{"_",	"_",	"_",	"_",	"_",	"_",	"_",	"_",	"_",	"_",	"_",	"_",	"_",	"_",	"_",	"_",	"_",	"_",	"^",	"%"},
    //{"_",	"^",	"%",	"%",	"%",	"^",	"^",	"^",	"_",	"_",	"%",	"%",	"_",	"%",	"%",	"%",	"_",	"_",	"^",	"%"}
    //};
    //*/

    //public byte getResolvedType (int cp) {
    //// LB 1  Assign a line break category to each character of the input.
    //// Resolve AI, CB, SA, SG, XX into other line break classes depending on criteria outside this algorithm.
    //byte result = getType(cp);
    //switch (result) {
    //case LB_AI: result = LB_AI; break;
    //// case LB_CB: result = LB_ID; break;
    //case LB_SA: result = LB_AL; break;
    //// case LB_SG: result = LB_XX; break; Surrogates; will never occur
    //case LB_XX: result = LB_AL; break;
    //}
    ///*
    //if (recommended) {
    //if (getHangulType(cp) != hNot) {
    //result = LB_ID;
    //}
    //}
    //*/

    //return result;
    //}

    //public byte getSampleType (int cp) {
    //if (ucd.getHangulSyllableType(cp) != NA) return LB_XX;
    //return getType(cp);
    //}


    //// find out whether there is a break at offset
    //// WARNING: as a side effect, sets "rule"

    //public boolean isBreak(String source, int offset) {

    //// LB 1  Assign a line break category to each character of the input.
    //// Resolve AI, CB, SA, SG, XX into other line break classes depending on criteria outside this algorithm.
    //// this is taken care of in the getResolvedType function

    //// LB 2a  Never break at the start of text

    //setRule("2a: × sot");
    //if (offset <= 0) return false;

    //// LB 2b  Always break at the end of text

    //setRule("2b: ! eot");
    //if (offset >= source.length()) return true;


    //// UTF-16: never break in the middle of a code point

    //// now get the base character before and after, and their types

    //getGraphemeBases(breaker, source, offset, -1, context);

    //byte before = context.tBefore;
    //byte after = context.tAfter;
    //byte before2 = context.tBefore2;
    //byte after2 = context.tAfter2;


    ////if (!onCodepointBoundary(source, offset)) return false;


    //// now get the character before and after, and their types


    ////int cpBefore = UTF16.charAt(source, offset-1);
    ////int cpAfter = UTF16.charAt(source, offset);

    ////byte before = getResolvedType(cpBefore);
    ////byte after = getResolvedType(cpAfter);


    //setRule("3a: CR × LF ; ( BK | CR | LF | NL ) !");

    //// Always break after hard line breaks (but never between CR and LF).
    //// CR ^ LF
    //if (before == LB_CR && after == LB_LF) return false;
    //if (before == LB_BK || before == LB_LF || before == LB_CR) return true;

    ////LB 3b  Don’t break before hard line breaks.
    //setRule("3b: × ( BK | CR | LF )");
    //if (after == LB_BK || after == LB_LF || after == LB_CR) return false;

    //// LB 4  Don’t break before spaces or zero-width space.
    //setRule("4: × ( SP | ZW )");
    //if (after == LB_SP || after == LB_ZW) return false;

    //// LB 5 Break after zero-width space.
    //setRule("5: ZW ÷");
    //if (before == LB_ZW) return true;

    //// LB 6  Don’t break graphemes (before combining marks, around virama or on sequences of conjoining Jamos.
    //setRule("6: DGC -> FC");
    //if (!grapheme.isBreak( source,  offset)) return false;

    ///*
    //if (before == LB_L && (after == LB_L || after == LB_V || after == LB_LV || after == LB_LVT)) return false;
    //if ((before == LB_LV || before == LB_V) && (after == LB_V || after == LB_T)) return false;
    //if ((before == LB_LVT || before == LB_T) && (after == LB_T)) return false;
    //*/

    //byte backBase = -1;
    //boolean setBase = false;
    //if (before == LB_CM) {
    //setBase = true;
    //int backOffset = findLastNon(source, offset, LB_CM);
    //if (backOffset >= 0) {
    //backBase = getResolvedType(UTF16.charAt(source, backOffset));
    //}
    //}


    //// LB 7  In all of the following rules, if a space is the base character for a combining mark,
    //// the space is changed to type ID. In other words, break before SP CM* in the same cases as
    //// one would break before an ID.
    //setRule("7: SP CM* -> ID");
    //if (setBase && backBase == LB_SP) before = LB_ID;
    //if (after == LB_SP && after2 == LB_CM) after = LB_ID;

    //setRule("7a: X CM* -> X");
    //if (after == LB_CM) return false;
    //if (setBase && backBase != -1) before = LB_ID;

    //setRule("7b: CM -> AL");
    //if (setBase && backBase == -1) before = LB_AL;


    //// LB 8  Don’t break before ‘]’ or ‘!’ or ‘;’ or ‘/’,  even after spaces.
    //// × CL, × EX, × IS, × SY
    //setRule("8: × ( CL | EX | IS | SY )");
    //if (after == LB_CL || after == LB_EX || after == LB_SY | after == LB_IS) return false;


    //// find the last non-space character; we will need it
    //byte lastNonSpace = before;
    //if (lastNonSpace == LB_SP) {
    //int backOffset = findLastNon(source, offset, LB_SP);
    //if (backOffset >= 0) {
    //lastNonSpace = getResolvedType(UTF16.charAt(source, backOffset));
    //}
    //}

    //// LB 9  Don’t break after ‘[’, even after spaces.
    //// OP SP* ×
    //setRule("9: OP SP* ×");
    //if (lastNonSpace == LB_OP) return false;

    //// LB 10  Don’t break within ‘�?[’, , even with intervening spaces.
    //// QU SP* × OP
    //setRule("10: QU SP* × OP");
    //if (lastNonSpace == LB_QU && after == LB_OP) return false;

    //// LB 11  Don’t break within ‘]h’, even with intervening spaces.
    //// CL SP* × NS
    //setRule("11: CL SP* × NS");
    //if (lastNonSpace == LB_CL && after == LB_NS) return false;

    //// LB 11a  Don’t break within ‘——’, even with intervening spaces.
    //// B2 × B2
    //setRule("11a: B2 × B2");
    //if (lastNonSpace == LB_B2 && after == LB_B2) return false;


    //// LB 13  Don’t break before or after NBSP or WORD JOINER
    //// × GL
    //// GL ×

    //setRule("11b: × WJ ; WJ ×");
    //if (after == LB_WJ || before == LB_WJ) return false;

    //// [Note: by this time, all of the "X" in the table are accounted for. We can safely break after spaces.]

    //// LB 12  Break after spaces
    //setRule("12: SP ÷");
    //if (before == LB_SP) return true;

    //// LB 13  Don’t break before or after NBSP or WORD JOINER
    //setRule("13: × GL ; GL ×");
    //if (after == LB_GL || before == LB_GL) return false;

    //// LB 14  Don’t break before or after ‘�?’
    //setRule("14: × QU ; QU ×");
    //if (before == LB_QU || after == LB_QU) return false;

    //// LB 14a  Break before and after CB
    //setRule("14a: ÷ CB ; CB ÷");
    //if (before == LB_CB || after == LB_CB) return true;

    //// LB 15  Don’t break before hyphen-minus, other hyphens, fixed-width spaces,
    //// small kana and other non- starters,  or after acute accents:

    //setRule("15: × ( BA | HY | NS ) ; BB ×");
    //if (after == LB_NS) return false;
    //if (after == LB_HY) return false;
    //if (after == LB_BA) return false;
    //if (before == LB_BB) return false;


    ////setRule("15a: HY × NU"); // NEW
    ////if (before == LB_HY && after == LB_NU) return false;

    //// LB 16  Don’t break between two ellipses, or between letters or numbers and ellipsis:
    //// Examples: ’9...’, ‘a...’, ‘H...’
    //setRule("16: ( AL | ID | IN | NU ) × IN");
    //if ((before == LB_NU || before == LB_AL || before == LB_ID) && after == LB_IN) return false;
    //if (before == LB_IN && after == LB_IN) return false;

    //// Don't break alphanumerics.
    //// LB 17  Don’t break within ‘a9’, ‘3a’, or ‘H%’
    //// Numbers are of the form PR ? ( OP | HY ) ? NU (NU | IS) * CL ?  PO ?
    //// Examples:   $(12.35)    2,1234    (12)¢    12.54¢
    //// This is approximated with the following rules. (Some cases already handled above,
    //// like ‘9,’, ‘[9’.)
    //setRule("17: ID × PO ; AL × NU; NU × AL");
    //if (before == LB_ID && after == LB_PO) return false;
    //if (before == LB_AL && after == LB_NU) return false;
    //if (before == LB_NU && after == LB_AL) return false;

    //// LB 18  Don’t break between the following pairs of classes.
    //// CL × PO
    //// HY × NU
    //// IS × NU
    //// NU × NU
    //// NU × PO
    //// PR × AL
    //// PR × HY
    //// PR × ID
    //// PR × NU
    //// PR × OP
    //// SY × NU
    //// Example pairs: ‘$9’, ‘$[’, ‘$-‘, ‘-9’, ‘/9’, ‘99’, ‘,9’,  ‘9%’ ‘]%’

    //setRule("18: CL × PO ; NU × PO ; ( IS | NU | HY | PR | SY ) × NU ; PR × ( AL | HY | ID | OP )");
    //if (before == LB_CL && after == LB_PO) return false;
    //if (before == LB_IS && after == LB_NU) return false;
    //if (before == LB_NU && after == LB_NU) return false;
    //if (before == LB_NU && after == LB_PO) return false;

    //if (before == LB_HY && after == LB_NU) return false;

    //if (before == LB_PR && after == LB_AL) return false;
    //if (before == LB_PR && after == LB_HY) return false;
    //if (before == LB_PR && after == LB_ID) return false;
    //if (before == LB_PR && after == LB_NU) return false;
    //if (before == LB_PR && after == LB_OP) return false;

    //if (before == LB_SY && after == LB_NU) return false;

    //// LB 15b  Break after hyphen-minus, and before acute accents:
    //setRule("18b: HY ÷ ; ÷ BB");
    //if (before == LB_HY) return true;
    //if (after == LB_BB) return true;

    //// LB 19  Don’t break between alphabetics (“at�?)
    //// AL × AL

    //setRule("19: AL × AL");
    //if (before == LB_AL && after == LB_AL) return false;

    //// LB 20  Break everywhere else
    //// ALL ÷
    //// ÷ ALL

    //if (ucd.getCompositeVersion() > 0x040000) {
    //setRule("19b: IS × AL");
    //if (before == LB_IS && after == LB_AL) return false;
    //}

    //// LB 20  Break everywhere else
    //// ALL ÷
    //// ÷ ALL

    //setRule("20: ALL ÷ ; ÷ ALL");
    //return true;
    //}
    //}

    //==============================================

    //static class XGenerateSentenceBreakTest extends GenerateBreakTest {

    //GenerateGraphemeBreakTest grapheme;
    //MyBreakIterator breaker;

    //XGenerateSentenceBreakTest(UCD ucd) {
    //super(ucd);
    //grapheme = new GenerateGraphemeBreakTest(ucd);
    //breaker = new MyBreakIterator(grapheme);

    //fileName = "Sentence";
    //extraSamples = new String[] {
    //};

    //extraSingleSamples = new String[] {
    //"(\"Go.\") (He did.)",
    //"(\u201CGo?\u201D) (He did.)",
    //"U.S.A\u0300. is",
    //"U.S.A\u0300? He",
    //"U.S.A\u0300.",
    //"3.4",
    //"c.d",
    //"etc.)\u2019 \u2018(the",
    //"etc.)\u2019 \u2018(The",
    //"the resp. leaders are",
    //"\u5B57.\u5B57",
    //"etc.\u5B83",
    //"etc.\u3002",
    //"\u5B57\u3002\u5B83",
    //};
    //String[] temp = new String [extraSingleSamples.length * 2];
    //System.arraycopy(extraSingleSamples, 0, temp, 0, extraSingleSamples.length);
    //for (int i = 0; i < extraSingleSamples.length; ++i) {
    //temp[i+extraSingleSamples.length] = insertEverywhere(extraSingleSamples[i], "\u2060", grapheme);
    //}
    //extraSingleSamples = temp;

    //}

    //Object foo = prop = unicodePropertySource.getProperty("Sentence_Break");

    //final int
    //CR =    addToMap("CR"),
    //LF =    addToMap("LF"),
    //Extend =    addToMap("Extend"),
    //Sep =    addToMap("Sep"),
    //Format =    addToMap("Format"),
    //Sp = addToMap("Sp"),
    //Lower = addToMap("Lower"),
    //Upper = addToMap("Upper"),
    //OLetter = addToMap("OLetter"),
    //Numeric =     addToMap("Numeric"),
    //ATerm =     addToMap("ATerm"),
    //STerm =    addToMap("STerm"),
    //Close =     addToMap("Close"),
    //SContinue =     addToMap("SContinue"),
    //Other = addToMapLast("Other");

    //// stuff that subclasses need to override
    //public String getTypeID(int cp) {
    //return map.getLabel(cp);
    //}

    //public String fullBreakSample() {
    //return "!a";
    //}

    //// stuff that subclasses need to override
    //public byte getType(int cp) {
    //return (byte) map.getIndex(cp);
    //}

    ///*LB_XX = 0, LB_OP = 1, LB_CL = 2, LB_QU = 3, LB_GL = 4, LB_NS = 5, LB_EX = 6, LB_SY = 7,
    //LB_IS = 8, LB_PR = 9, LB_PO = 10, LB_NU = 11, LB_AL = 12, LB_ID = 13, LB_IN = 14, LB_HY = 15,
    //LB_CM = 16, LB_BB = 17, LB_BA = 18, LB_SP = 19, LB_BK = 20, LB_CR = 21, LB_LF = 22, LB_CB = 23,
    //LB_SA = 24, LB_AI = 25, LB_B2 = 26, LB_SG = 27, LB_ZW = 28,
    //LB_NL = 29,
    //LB_WJ = 30,
    //*/
    ///*
    //static final byte Format = 0, Sep = 1, Sp = 2, OLetter = 3, Lower = 4, Upper = 5,
    //Numeric = 6, Close = 7, ATerm = 8, Term = 9, Other = 10,
    //LIMIT = Other + 1;

    //static final String[] Names = {"Format", "Sep", "Sp", "OLetter", "Lower", "Upper", "Numeric",
    //"Close", "ATerm", "Term", "Other" };


    //static UnicodeSet sepSet = new UnicodeSet("[\\u000a\\u000d\\u0085\\u2029\\u2028]");
    //static UnicodeSet atermSet = new UnicodeSet("[\\u002E]");
    //static UnicodeSet termSet = new UnicodeSet(
    //"[\\u0021\\u003F\\u0589\\u061f\\u06d4\\u0700-\\u0702\\u0934"
    //+ "\\u1362\\u1367\\u1368\\u104A\\u104B\\u166E"
    //+ "\\u1803\\u1809\\u203c\\u203d"
    //+ "\\u2048\\u2049\\u3002\\ufe52\\ufe57\\uff01\\uff0e\\uff1f\\uff61]");

    //static UnicodeProperty lowercaseProp = UnifiedBinaryProperty.make(DERIVED | PropLowercase);
    //static UnicodeProperty uppercaseProp = UnifiedBinaryProperty.make(DERIVED | PropUppercase);

    //UnicodeSet linebreakNS = UnifiedBinaryProperty.make(LINE_BREAK | LB_NU).getSet();
    //*/

    ///*
    //// stuff that subclasses need to override
    //public String getTypeID(int cp) {
    //byte type = getType(cp);
    //return Names[type];
    //}

    //// stuff that subclasses need to override
    //public byte getType(int cp) {
    //byte cat = ucd.getCategory(cp);

    //if (cat == Cf) return Format;
    //if (sepSet.contains(cp)) return Sep;
    //if (ucd.getBinaryProperty(cp, White_space)) return Sp;
    //if (linebreakNS.contains(cp)) return Numeric;
    //if (lowercaseProp.hasValue(cp)) return Lower;
    //if (uppercaseProp.hasValue(cp) || cat == Lt) return Upper;
    //if (alphabeticSet.contains(cp)) return OLetter;
    //if (atermSet.contains(cp)) return ATerm;
    //if (termSet.contains(cp)) return Term;
    //if (cat == Po || cat == Pe
    //|| ucd.getLineBreak(cp) == LB_QU) return Close;
    //return Other;
    //}
    //*/

    //public int genTestItems(String before, String after, String[] results) {
    //results[0] = before + after;
    ///*
    //results[1] = 'a' + before + "\u0301\u0308" + after + "\u0301\u0308" + 'a';
    //results[2] = 'a' + before + "\u0301\u0308" + samples[MidLetter] + after + "\u0301\u0308" + 'a';
    //results[3] = 'a' + before + "\u0301\u0308" + samples[MidNum] + after + "\u0301\u0308" + 'a';
    //*/
    //return 1;
    //}

    //static Context context = new Context();

    //public boolean isBreak(String source, int offset) {

    //// Break at the start and end of text.
    //setRule("1: sot ÷");
    //if (offset < 0 || offset > source.length()) return false;

    //if (offset == 0) return true;

    //setRule("2: ÷ eot");
    //if (offset == source.length()) return true;

    //setRule("3: Sep ÷");
    //byte beforeChar = getResolvedType(source.charAt(offset-1));
    //if (beforeChar == Sep) return true;

    //// Treat a grapheme cluster as if it were a single character:
    //// the first base character, if there is one; otherwise the first character.

    //setRule("4: GC -> FC");
    //if (!grapheme.isBreak( source,  offset)) return false;

    //// Ignore interior Format characters. That is, ignore Format characters in all subsequent rules.
    //setRule("5: X Format* -> X");
    //byte afterChar = getResolvedType(source.charAt(offset));
    //if (afterChar == Format) return false;

    //getGraphemeBases(breaker, source, offset, Format, context);
    //byte before = context.tBefore;
    //byte after = context.tAfter;
    //byte before2 = context.tBefore2;
    //byte after2 = context.tAfter2;

    //// HACK COPY for rule collection!
    //if (collectingRules) {
    //setRule("6: ATerm × ( Numeric | Lower )");
    //setRule("7: Upper ATerm × Upper");
    //// setRule("8: ATerm Close* Sp* × ( ¬(OLetter | Upper | Lower) )* Lower");
    //setRule("8: ATerm Close* Sp* × ( ¬(OLetter | Upper | Lower | Sep | CR | LF | STerm | ATerm) )* Lower");
    //setRule("8a: STerm | ATerm) Close* Sp* × (SContinue | STerm | ATerm)");
    //setRule("9: ( Term | ATerm ) Close* × ( Close | Sp | Sep )");
    //setRule("10: ( Term | ATerm ) Close* Sp × ( Sp | Sep )");
    //setRule("11: ( Term | ATerm ) Close* Sp* ÷");
    //setRule("12: Any × Any");
    //collectingRules = false;
    //}

    //// Do not break after ambiguous terminators like period, if immediately followed by a number or lowercase letter, is between uppercase letters, or if the first following letter (optionally after certain punctuation) is lowercase. For example, a period may be an abbreviation or numeric period, and not mark the end of a sentence.

    //if (before == ATerm) {
    //setRule("6: ATerm × ( Numeric | Lower )");
    //if (after == Lower || after == Numeric) return false;
    //setRule("7: Upper ATerm × Upper");
    //if (DEBUG_GRAPHEMES) System.out.println(context + ", " + Upper);
    //if (before2 == Upper && after == Upper) return false;
    //}

    //// The following cases are all handled together.

    //// First we loop backwards, checking for the different types.

    //MyBreakIterator graphemeIterator = new MyBreakIterator(grapheme);
    //graphemeIterator.set(source, offset);

    //int state = 0;
    //int lookAfter = -1;
    //int cp;
    //byte t;
    //boolean gotSpace = false;
    //boolean gotClose = false;

    //behindLoop:
    //while (true) {
    //cp = graphemeIterator.previousBase();
    //if (cp == -1) break;
    //t = getResolvedType(cp);
    //if (SHOW_TYPE) System.out.println(ucd.getCodeAndName(cp) + ", " + getTypeID(cp));

    //if (t == Format) continue;  // ignore all formats!

    //switch (state) {
    //case 0:
    //if (t == Sp) {
    //// loop as long as we have Space
    //gotSpace = true;
    //continue behindLoop;
    //} else if (t == Close) {
    //gotClose = true;
    //state = 1;    // go to close loop
    //continue behindLoop;
    //}
    //break;
    //case 1:
    //if (t == Close) {
    //// loop as long as we have Close
    //continue behindLoop;
    //}
    //break;
    //}
    //if (t == ATerm) {
    //lookAfter = ATerm;
    //} else if (t == STerm) {
    //lookAfter = STerm;
    //}
    //break;
    //}

    //// if we didn't find ATerm or Term, bail

    //if (lookAfter == -1) {
    //// Otherwise, do not break
    //// Any × Any (11)
    //setRule("12: Any × Any");
    //return false;
    //}

    //// ATerm Close* Sp*×(¬( OLetter))* Lower(8)

    //// Break after sentence terminators, but include closing punctuation, trailing spaces, and (optionally) a paragraph separator.
    //// ( Term | ATerm ) Close*×( Close | Sp | Sep )(9)
    //// ( Term | ATerm ) Close* Sp×( Sp | Sep )(10)
    //// ( Term | ATerm ) Close* Sp*÷(11)


    //// We DID find one. Loop to see if the right side is ok.

    //graphemeIterator.set(source, offset);
    //boolean isFirst = true;
    //while (true) {
    //cp = graphemeIterator.nextBase();
    //if (cp == -1) break;
    //t = getResolvedType(cp);
    //if (SHOW_TYPE) System.out.println(ucd.getCodeAndName(cp) + ", " + getTypeID(cp));

    //if (t == Format) continue;  // skip format characters!

    //if (isFirst) {
    //isFirst = false;
    //if (lookAfter == ATerm && t == Upper) {
    //setRule("8: ATerm Close* Sp* × ( ¬(OLetter | Upper | Lower | Sep | CR | LF | STerm | ATerm) )* Lower");
    //return false;
    //}
    //if (gotSpace) {
    //if (t == Sp || t == Sep) {
    //setRule("10: ( Term | ATerm ) Close* Sp × ( Sp | Sep )");
    //return false;
    //}
    //} else if (t == Close || t == Sp || t == Sep) {
    //setRule("9: ( Term | ATerm ) Close* × ( Close | Sp | Sep )");
    //return false;
    //}
    //if (lookAfter == STerm) break;
    //}

    //// at this point, we have an ATerm. All other conditions are ok, but we need to verify 6
    //if (t != OLetter && t != Upper && t != Lower) continue;
    //if (t == Lower) {
    //setRule("8: ATerm Close* Sp* × ( ¬(OLetter | Upper | Lower) )* Lower");
    //return false;
    //}
    //break;
    //}
    //setRule("11: ( Term | ATerm ) Close* Sp* ÷");
    //return true;
    //}
    //}

    static final boolean DEBUG_GRAPHEMES = false;
    static final Transliterator escaper = Transliterator.createFromRules(
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
            //if (DEBUG_GRAPHEMES) System.out.println(Utility.hex(string) + "; " + offset);
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
            //if (DEBUG_GRAPHEMES) System.out.println(Utility.hex(result));
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
            //if (DEBUG_GRAPHEMES) System.out.println(Utility.hex(result));
            return result;
        }
    }
    /*
     * 
     *         if (false) {

            PrintWriter log = Utility.openPrintWriter("Diff.txt", Utility.UTF8_WINDOWS);
            UnicodeSet Term = new UnicodeSet(
                "[\\u0021\\u003F\\u0589\\u061F\\u06D4\\u0700\\u0701\\u0702\\u0964\\u1362\\u1367"
                + "\\u1368\\u104A\\u104B\\u166E\\u1803\\u1809\\u203C\\u203D\\u2047\\u2048\\u2049"
                + "\\u3002\\uFE52\\uFE57\\uFF01\\uFF0E\\uFF1F\\uFF61]");
            UnicodeSet terminal_punctuation = getSet(BINARY_PROPERTIES, Terminal_Punctuation);
            UnicodeMap names = new UnicodeMap();
            names.add("Pd", getSet(CATEGORY, Pd));
            names.add("Ps", getSet(CATEGORY, Ps));
            names.add("Pe", getSet(CATEGORY, Pe));
            names.add("Pc", getSet(CATEGORY, Pc));
            names.add("Po", getSet(CATEGORY, Po));
            names.add("Pi", getSet(CATEGORY, Pi));
            names.add("Pf", getSet(CATEGORY, Pf));

            Utility.showSetDifferences(log, "Term", Term, "Terminal_Punctuation", terminal_punctuation, true, true, names, ucd);
            Utility.showSetDifferences(log, "Po", getSet(CATEGORY, Po), "Terminal_Punctuation", terminal_punctuation, true, true, names, ucd);
            log.close();

            if (true) return;

            UnicodeSet whitespace = getSet(BINARY_PROPERTIES, White_space);
            UnicodeSet space = getSet(CATEGORY, Zs).addAll(getSet(CATEGORY, Zp)).addAll(getSet(CATEGORY, Zl));
            Utility.showSetDifferences("White_Space", whitespace, "Z", space, true, ucd);

            UnicodeSet isSpace = new UnicodeSet();
            UnicodeSet isSpaceChar = new UnicodeSet();
            UnicodeSet isWhitespace = new UnicodeSet();
            for (int i = 0; i <= 0xFFFF; ++i) {
                if (Character.isSpace((char)i)) isSpace.add(i);
                if (Character.isSpaceChar((char)i)) isSpaceChar.add(i);
                if (Character.isWhitespace((char)i)) isWhitespace.add(i);
            }
            Utility.showSetDifferences("White_Space", whitespace, "isSpace", isSpace, true, ucd);
            Utility.showSetDifferences("White_Space", whitespace, "isSpaceChar", isSpaceChar, true, ucd);
            Utility.showSetDifferences("White_Space", whitespace, "isWhitespace", isWhitespace, true, ucd);
            return;
        }

        if (DEBUG) {
            checkDecomps();

            Utility.showSetNames("", new UnicodeSet("[\u034F\u00AD\u1806[:DI:]-[:Cs:]-[:Cn:]]"), true, ucd);

            System.out.println("*** Extend - Cf");

            generateTerminalClosure();

            GenerateWordBreakTest gwb = new GenerateWordBreakTest();
            PrintWriter systemPrintWriter = new PrintWriter(System.out);
            gwb.printLine(systemPrintWriter, "n\u0308't", true, true, false);
            systemPrintWriter.flush();
            //showSet("sepSet", GenerateSentenceBreakTest.sepSet);
            //showSet("atermSet", GenerateSentenceBreakTest.atermSet);
            //showSet("termSet", GenerateSentenceBreakTest.termSet);
        }

        if (true) {
            GenerateBreakTest foo = new GenerateLineBreakTest();
            //foo.isBreak("(\"Go.\") (He did)", 5, true);
            foo.isBreak("\u4e00\u4300", 1, true);
            /*
            GenerateSentenceBreakTest foo = new GenerateSentenceBreakTest();
            //foo.isBreak("(\"Go.\") (He did)", 5, true);
            foo.isBreak("3.4", 2, true);
     * /
        }

        new GenerateGraphemeBreakTest().run();
        new GenerateWordBreakTest().run();
        new GenerateLineBreakTest().run();
        new GenerateSentenceBreakTest().run();

        //if (true) return; // cut short for now

    }

     */

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
        for (String s : us) {
            if (--count <= 0) {
                return s;
            }
        }
        throw new IllegalArgumentException();
    }
}
