package org.unicode.text.UCD;

import com.ibm.icu.dev.tool.UOption;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.Tabber;
import org.unicode.cldr.util.Tabber.HTMLTabber;
import org.unicode.cldr.util.props.UnicodeLabel;
import org.unicode.jsp.ICUPropertyFactory;
import org.unicode.props.BagFormatter;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.IndexUnicodeProperties.DefaultValueType;
import org.unicode.props.UcdProperty;
import org.unicode.props.UnicodeProperty;
import org.unicode.props.UnicodeProperty.Factory;
import org.unicode.text.utility.Settings;

public class TestUnicodeInvariants {
    private static final boolean DEBUG = false;

    // private static final Pattern IN_PATTERN = Pattern.compile("(.*)([≠=])(.*)");
    private static final boolean ICU_VERSION = false; // ignore the versions if this is true
    private static final Factory LATEST_PROPS = getProperties(Settings.latestVersion);
    private static final boolean SHOW_LOOKUP = false;
    private static int showRangeLimit = 20;
    static boolean doHtml = true;
    public static final String DEFAULT_FILE = "UnicodeInvariantTest.txt";
    public static final HTMLTabber htmlTabber = new Tabber.HTMLTabber();
    public static final boolean EMIT_GITHUB_ERRORS =
            System.getProperty("EMIT_GITHUB_ERRORS") != null;

    private static final int
            // HELP1 = 0,
            FILE = 1,
            RANGE = 2,
            TABLE = 3;

    private static final UOption[] options = {
        UOption.HELP_H(),
        UOption.create("file", 'f', UOption.REQUIRES_ARG),
        UOption.create("norange", 'n', UOption.NO_ARG),
        UOption.create("table", 't', UOption.NO_ARG),
    };

    public static void main(String[] args) throws IOException {
        UOption.parseArgs(args, options);

        String file = DEFAULT_FILE;
        if (options[FILE].doesOccur) {
            file = options[FILE].value;
        }
        doHtml = options[TABLE].doesOccur;

        doRange = !options[RANGE].doesOccur;
        System.out.println("File:\t" + file);
        System.out.println("Ranges?\t" + doRange);

        System.out.println("HTML?\t" + doHtml);

        testInvariants(file, null, doRange);
    }

    static Transliterator toHTML;

    static {
        final String BASE_RULES =
                "'<' > '&lt;' ;"
                        + "'<' < '&'[lL][Tt]';' ;"
                        + "'&' > '&amp;' ;"
                        + "'&' < '&'[aA][mM][pP]';' ;"
                        + "'>' < '&'[gG][tT]';' ;"
                        + "'\"' < '&'[qQ][uU][oO][tT]';' ; "
                        + "'' < '&'[aA][pP][oO][sS]';' ; ";

        final String CONTENT_RULES = "'>' > '&gt;' ;";

        final String HTML_RULES = BASE_RULES + CONTENT_RULES + "'\"' > '&quot;' ; ";

        final String HTML_RULES_CONTROLS =
                HTML_RULES
                        + "[[:di:]-[:cc:]-[:cs:]-[\\u200E\\u200F]] > ; " // remove, should ignore in
                        // rendering (but may not
                        // be in browser)
                        + "[[:nchar:][:cn:][:cs:][:co:][:cc:]-[:whitespace:]-[\\u200E\\u200F]] > \\uFFFD ; "; // should be missing glyph (but may not be in browser)
        //     + "([[:C:][:Z:][:whitespace:][:Default_Ignorable_Code_Point:]-[\\u0020]]) >
        // &hex/xml($1) ; "; // [\\u0080-\\U0010FFFF]

        toHTML =
                Transliterator.createFromRules(
                        "any-xml", HTML_RULES_CONTROLS, Transliterator.FORWARD);
    }

    enum Expected {
        empty,
        not_empty,
        irrelevant
    };

    static final UnicodeSet INVARIANT_RELATIONS = new UnicodeSet("[=\u2282\u2283\u2286\u2287∥≉]");

    private static PrintWriter out;

    /**
     * Fetch a reader for our input data.
     *
     * @param inputFile read from classpath
     * @return BufferedReader
     * @throws IOException
     */
    private static BufferedReader getInputReader(String inputFile) throws IOException {
        return FileUtilities.openFile(TestUnicodeInvariants.class, inputFile);
    }

    /**
     * @param inputFile file to input, defaults to DEFAULT_FILE
     * @param suffix Suffix for the test results report file, added after a hyphen if non-null.
     * @param doRange normally true
     * @return number of failures (0 is better)
     * @throws IOException
     */
    public static int testInvariants(String inputFile, String suffix, boolean doRange)
            throws IOException {
        if (inputFile == null) {
            inputFile = DEFAULT_FILE;
        }
        TestUnicodeInvariants.doRange = doRange;
        parseErrorCount = 0;
        testFailureCount = 0;
        boolean showScript = false;
        try (final PrintWriter out2 =
                FileUtilities.openUTF8Writer(
                        Settings.Output.GEN_DIR,
                        "UnicodeTestResults"
                                + (suffix == null ? "" : "-" + suffix)
                                + (doHtml ? ".html" : ".txt"))) {
            final StringWriter writer = new StringWriter();
            try (PrintWriter out3 = new PrintWriter(writer)) {
                out = out3;
                if (doHtml) {
                    out3.println(
                            "<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'>");
                    out3.println(
                            "<link rel='stylesheet' type='text/css' href='UnicodeTestResults.css'>");
                    out3.println("<title>Unicode Property Results</title>");
                    out3.println(
                            "<style>\n"
                                    + "p {margin:0}\n"
                                    + ".b {font-weight:bold; color:green}\n"
                                    + ".bb {font-weight:bold; color:blue}\n"
                                    + ".e, .f {color:red}\n"
                                    + ".s {color:gray}\n"
                                    + "code {white-space: pre}\n"
                                    + "</style>");
                    out3.println("</head><body><h1>#Unicode Invariant Results</h1>");
                } else {
                    out3.write('\uFEFF'); // BOM
                }
                final var noComments = new StringBuilder();
                final List<String> lines = new ArrayList<>();
                final List<Integer> lineBeginnings = new ArrayList();
                try (final BufferedReader in = getInputReader(inputFile)) {
                    in.lines()
                            .forEach(
                                    line -> {
                                        if (line.startsWith("\uFEFF")) {
                                            line = line.substring(1);
                                        }
                                        lines.add(line);
                                        lineBeginnings.add(noComments.length());
                                        final int pos = line.indexOf('#');
                                        if (pos >= 0) {
                                            line = line.substring(0, pos);
                                        }
                                        noComments.append(line.trim() + '\n');
                                    });
                }
                errorLister =
                        new BagFormatter()
                                .setMergeRanges(doRange)
                                .setLabelSource(null)
                                .setUnicodePropertyFactory(LATEST_PROPS)
                                // .setTableHtml("<table class='e'>")
                                .setShowLiteral(toHTML)
                                .setFixName(toHTML);
                errorLister.setShowTotal(false);
                if (doHtml) {
                    errorLister.setTabber(htmlTabber);
                }

                showLister =
                        new BagFormatter()
                                .setMergeRanges(doRange)
                                // .setLabelSource(null)
                                .setUnicodePropertyFactory(LATEST_PROPS)
                                // .setTableHtml("<table class='s'>")
                                .setShowLiteral(toHTML);
                showLister.setShowTotal(false);
                if (showScript) {
                    showLister.setValueSource(LATEST_PROPS.getProperty("script"));
                }
                if (doHtml) {
                    showLister.setTabber(htmlTabber);
                }

                final String source = noComments.toString();
                final Function<ParsePosition, Integer> getLineNumber =
                        position -> {
                            for (int i = 0; i < lineBeginnings.size(); ++i) {
                                if (lineBeginnings.get(i) > position.getIndex()) {
                                    return i; // 1-based line number.
                                }
                            }
                            return lineBeginnings.size();
                        };
                int lastPrintedLine = 0;
                final ParsePosition pp = new ParsePosition(0);
                boolean followingParseError = false;
                class IgnoringBlock {
                    public IgnoringBlock(Set<String> properties, int position) {
                        this.properties = properties;
                        this.position = new ParsePosition(position);
                    }

                    Set<String> properties;
                    ParsePosition position;
                }
                ;
                Stack<IgnoringBlock> ignoredProperties = new Stack<>();
                for (; ; ) {
                    final int statementStart = pp.getIndex();
                    final int statementLineNumber = getLineNumber.apply(pp);
                    final var nextToken = Lookahead.oneToken(pp, source);
                    while (lastPrintedLine < statementLineNumber) {
                        println(lines.get(lastPrintedLine++));
                    }
                    if (nextToken == null) {
                        break;
                    }
                    try {
                        if (nextToken.accept("Ignoring")) {
                            ignoredProperties.add(
                                    new IgnoringBlock(
                                            ignoringPropertiesLine(
                                                    pp, source, inputFile, getLineNumber),
                                            statementStart));
                        } else if (nextToken.accept("end")) {
                            ignoredProperties.pop();
                            expectToken("Ignoring", pp, source);
                            expectToken(";", pp, source);
                        } else if (!ignoredProperties.empty()) {
                            if (nextToken.accept("end")) {
                                ignoredProperties.pop();
                                expectToken("Ignoring", pp, source);
                                expectToken(";", pp, source);
                            } else if (nextToken.accept("Propertywise")) {
                                propertywiseLine(
                                        ignoredProperties.stream()
                                                .flatMap(b -> b.properties.stream())
                                                .collect(Collectors.toSet()),
                                        pp,
                                        source,
                                        inputFile,
                                        getLineNumber);
                            } else {
                                throw new ParseException(
                                        "Expected Propertywise test within Ignoring block",
                                        pp.getIndex());
                            }
                        } else if (nextToken.accept("Propertywise")) {
                            throw new ParseException(
                                    "Propertywise test should be within an Ignoring block",
                                    pp.getIndex());
                        } else if (nextToken.accept("Let")) {
                            letLine(pp, source);
                        } else if (nextToken.accept("In")) {
                            inLine(pp, source, inputFile, getLineNumber);
                        } else if (nextToken.accept("Map")) {
                            testMapLine(source, pp, getLineNumber);
                        } else if (nextToken.accept("ShowMap")) {
                            showMapLine(source, pp);
                        } else if (nextToken.accept("Show")) {
                            showLine(source, pp);
                        } else if (nextToken.accept("OnPairsOf")) {
                            equivalencesLine(source, pp, inputFile, getLineNumber);
                        } else {
                            pp.setIndex(statementStart);
                            testLine(source, pp, inputFile, getLineNumber);
                        }
                        followingParseError = false;
                    } catch (final Exception e) {
                        if (!followingParseError) {
                            final int lineNumber = getLineNumber.apply(pp);
                            while (lineNumber > lastPrintedLine) {
                                println(lines.get(lastPrintedLine++));
                            }
                            parseErrorCount =
                                    parseError(
                                            parseErrorCount,
                                            source,
                                            e,
                                            statementStart,
                                            inputFile,
                                            getLineNumber.apply(pp));
                        }
                        // Give up on the whole line, it is unlikely to contain anything we can
                        // parse.
                        // Try parsing the next line, but since that may be the rest of what we
                        // failed to parse,
                        // do not report errors until we successfully parse *something*.
                        final int nextLine = source.indexOf("\n", pp.getIndex());
                        if (nextLine >= 0) {
                            pp.setIndex(source.indexOf("\n", pp.getIndex()));
                            followingParseError = true;
                            continue;
                        } else {
                            break;
                        }
                    }
                }
                for (IgnoringBlock block : ignoredProperties) {
                    parseErrorCount =
                            parseError(
                                    parseErrorCount,
                                    source,
                                    new ParseException(
                                            "Unterminated Ignoring block for " + block.properties,
                                            block.position.getIndex()),
                                    block.position.getIndex(),
                                    inputFile,
                                    getLineNumber.apply(block.position));
                }
                println();
                println("**** SUMMARY ****");
                println();
                println("# ParseErrorCount=" + parseErrorCount);
                System.out.println("ParseErrorCount=" + parseErrorCount);
                println("# TestFailureCount=" + testFailureCount);
                System.out.println("TestFailureCount=" + testFailureCount);
                if (doHtml) {
                    out3.println("</body></html>");
                }
                out2.append(writer.getBuffer());
            }
        }
        return parseErrorCount + testFailureCount;
    }

    abstract static class PropertyPredicate {
        UnicodeSet valueSet;
        UnicodeProperty property1;

        public UnicodeMap<String> getFailures() {
            final UnicodeMap<String> failures = new UnicodeMap<>();

            for (final UnicodeSetIterator it = new UnicodeSetIterator(valueSet); it.next(); ) {
                final String failure = getFailure(it.codepoint);
                if (failure != null) {
                    failures.put(it.codepoint, failure);
                }
            }
            return failures;
        }

        // A description of the failure for the given codepoint, or null if the predicate holds.
        protected abstract String getFailure(int codepoint);
    }

    static class PropertyComparison extends PropertyPredicate {
        boolean shouldBeEqual;
        UnicodeProperty property2;

        @Override
        protected String getFailure(int codepoint) {
            final String value1 = property1.getValue(codepoint);
            final String value2 = property2.getValue(codepoint);
            final boolean areEqual = Objects.equals(value1, value2);
            if (areEqual == shouldBeEqual) {
                return null;
            } else {
                return value1 + (areEqual ? "=" : "≠") + value2;
            }
        }
    }

    static class PropertyValueContainment extends PropertyPredicate {
        boolean shouldBeInSet;
        UnicodeSet set;

        @Override
        protected String getFailure(int codepoint) {
            final String value = property1.getValue(codepoint);
            final boolean isInSet = set.contains(value);
            if (isInSet == shouldBeInSet) {
                return null;
            } else {
                return value + (isInSet ? "∈" : "∉") + set;
            }
        }
    }

    private static Set<String> ignoringPropertiesLine(
            ParsePosition pp,
            String source,
            String file,
            Function<ParsePosition, Integer> getLineNumber)
            throws ParseException {
        Set<String> excludedProperties = new HashSet<>();
        for (var next = Lookahead.oneToken(pp, source);
                !next.accept(":");
                next = Lookahead.oneToken(pp, source)) {
            excludedProperties.add(next.consume());
        }
        return excludedProperties;
    }

    private static void propertywiseLine(
            Set<String> ignoredProperties,
            ParsePosition pp,
            String source,
            String file,
            Function<ParsePosition, Integer> getLineNumber)
            throws ParseException {
        final UnicodeSet set = parseUnicodeSet(source, pp);
        if (set.hasStrings()) {
            throw new BackwardParseException(
                    "Set should contain only single code points for property comparison",
                    pp.getIndex());
        }
        if (Lookahead.oneToken(pp, source).accept("AreAlike")) {
            propertywiseAlikeLine(ignoredProperties, set, pp, source, file, getLineNumber);
        } else {
            propertywiseCorrespondenceLine(ignoredProperties, set, pp, source, file, getLineNumber);
        }
    }

    private static void propertywiseAlikeLine(
            Set<String> ignoredProperties,
            UnicodeSet set,
            ParsePosition pp,
            String source,
            String file,
            Function<ParsePosition, Integer> getLineNumber)
            throws ParseException {
        final var iup = IndexUnicodeProperties.make(Settings.latestVersion);
        final List<String> errorMessageLines = new ArrayList<>();
        for (var p : UcdProperty.values()) {
            final var property = iup.getProperty(p);
            if (property.getNameAliases().stream()
                    .anyMatch(alias -> ignoredProperties.contains(alias))) {
                continue;
            }
            final int first = set.charAt(0);
            String p1 = property.getValue(first);
            for (var range : set.ranges()) {
                for (int c = range.codepoint; c <= range.codepointEnd; ++c) {
                    if (c == first) {
                        continue;
                    }
                    String p2 = property.getValue(c);
                    if (!Objects.equals(p1, p2)) {
                        if (IndexUnicodeProperties.getResolvedDefaultValueType(p)
                                        != DefaultValueType.CODE_POINT
                                || !p1.equals(Character.toString(first))
                                || !p2.equals(Character.toString(c))) {
                            errorMessageLines.add(
                                    property.getName()
                                            + "("
                                            + Character.toString(first)
                                            + ")\t=\t"
                                            + p1
                                            + "\t≠\t"
                                            + p2
                                            + "\t=\t"
                                            + property.getName()
                                            + "("
                                            + Character.toString(c)
                                            + ")");
                        }
                    }
                }
            }
        }
        if (!errorMessageLines.isEmpty()) {
            testFailureCount++;
            printErrorLine("Test Failure", Side.START, testFailureCount);
            reportTestFailure(
                    file,
                    getLineNumber.apply(pp),
                    String.join("\n", errorMessageLines).replace('\t', ' '));
            out.println("<table class='f'>");
            for (String errorMessageLine : errorMessageLines) {
                out.println("<tr><td>");
                out.println(toHTML.transform(errorMessageLine).replace("\t", "</td><td>"));
                out.println("</tr></td>");
            }
            out.println("</table>");
            printErrorLine("Test Failure", Side.END, testFailureCount);
        }
    }

    private static void propertywiseCorrespondenceLine(
            Set<String> ignoredProperties,
            UnicodeSet firstSet,
            ParsePosition pp,
            String source,
            String file,
            Function<ParsePosition, Integer> getLineNumber)
            throws ParseException {
        final var iup = IndexUnicodeProperties.make(Settings.latestVersion);
        final List<String> errorMessageLines = new ArrayList<>();
        final List<UnicodeSet> sets = new ArrayList<>();
        sets.add(firstSet);
        expectToken(":", pp, source);
        do {
            final var set = parseUnicodeSet(source, pp);
            if (set.hasStrings()) {
                throw new BackwardParseException(
                        "Set should contain only single code points for property comparison",
                        pp.getIndex());
            }
            if (set.size() != firstSet.size()) {
                throw new BackwardParseException(
                        "Sets should have the same size for property correspondence (got "
                                + set.size()
                                + ", expected "
                                + firstSet.size()
                                + ")",
                        pp.getIndex());
            }
            sets.add(set);
        } while (Lookahead.oneToken(pp, source).accept(":"));
        final List<Integer> referenceCodePoints = new ArrayList<>();
        expectToken("CorrespondTo", pp, source);
        do {
            final var referenceSet = parseUnicodeSet(source, pp);
            if (referenceSet.hasStrings() || referenceSet.size() != 1) {
                throw new BackwardParseException(
                        "reference should be a single code point for property correspondence",
                        pp.getIndex());
            }
            referenceCodePoints.add(referenceSet.charAt(0));
        } while (Lookahead.oneToken(pp, source).accept(":"));
        if (referenceCodePoints.size() != sets.size()) {
            throw new BackwardParseException(
                    "Property correspondence requires as many reference code points as sets under test",
                    pp.getIndex());
        }

        class ExpectedPropertyDifference {
            public ExpectedPropertyDifference(String actualValueAlias, String referenceValueAlias) {
                this.actualValueAlias = actualValueAlias;
                this.referenceValueAlias = referenceValueAlias;
            }

            String actualValueAlias;
            String referenceValueAlias;
        }
        final Map<String, ExpectedPropertyDifference> expectedPropertyDifferences = new HashMap<>();
        if (Lookahead.oneToken(pp, source).accept("UpTo")) {
            expectToken(":", pp, source);
            do {
                String property = Lookahead.oneToken(pp, source).consume();
                expectToken("(", pp, source);
                String actualValueAlias = Lookahead.oneToken(pp, source).consume();
                expectToken("vs", pp, source);
                String referenceValueAlias = Lookahead.oneToken(pp, source).consume();
                expectToken(")", pp, source);
                expectedPropertyDifferences.put(
                        property,
                        new ExpectedPropertyDifference(actualValueAlias, referenceValueAlias));
            } while (Lookahead.oneToken(pp, source).accept(","));
        }
        for (var p : UcdProperty.values()) {
            final var property = iup.getProperty(p);
            if (property.getNameAliases().stream()
                    .anyMatch(alias -> ignoredProperties.contains(alias))) {
                continue;
            }
            ExpectedPropertyDifference expectedDifference = null;
            for (String alias : property.getNameAliases()) {
                expectedDifference = expectedPropertyDifferences.get(alias);
            }
            if (expectedDifference != null) {
                for (int k = 0; k < sets.size(); ++k) {
                    final int rk = referenceCodePoints.get(k);
                    final String pRk = property.getValue(rk);
                    if (!Objects.equals(pRk, expectedDifference.referenceValueAlias)) {
                        errorMessageLines.add(
                                property.getName()
                                        + "("
                                        + Character.toString(rk)
                                        + ")\t=\t"
                                        + pRk
                                        + "\t≠\t"
                                        + expectedDifference.referenceValueAlias);
                    }
                    final UnicodeSet set = sets.get(k);
                    for (int i = 0; i < set.size(); ++i) {
                        final int ck = set.charAt(i);
                        final String pCk = property.getValue(ck);
                        if (!Objects.equals(pCk, expectedDifference.actualValueAlias)) {
                            errorMessageLines.add(
                                    property.getName()
                                            + "("
                                            + Character.toString(ck)
                                            + ")\t=\t"
                                            + pCk
                                            + "\t≠\t"
                                            + expectedDifference.actualValueAlias);
                        }
                    }
                }
            } else {
                for (int k = 0; k < sets.size(); ++k) {
                    final UnicodeSet set = sets.get(k);
                    final int rk = referenceCodePoints.get(k);
                    final String pRk = property.getValue(rk);
                    loop_over_set:
                    for (int i = 0; i < set.size(); ++i) {
                        final int ck = set.charAt(i);
                        final String pCk = property.getValue(ck);
                        if (Objects.equals(pCk, pRk)) {
                            continue;
                        }
                        Integer lMatchingForReference = null;
                        for (int l = 0; l < sets.size(); ++l) {
                            final boolean pCkEqualsCl =
                                    Objects.equals(pCk, Character.toString(sets.get(l).charAt(i)));
                            final boolean pRkEqualsRl =
                                    Objects.equals(
                                            pRk, Character.toString(referenceCodePoints.get(l)));
                            if (pRkEqualsRl) {
                                lMatchingForReference = l;
                                if (pCkEqualsCl) {
                                    continue loop_over_set;
                                }
                            }
                        }
                        if (lMatchingForReference == null) {
                            errorMessageLines.add(
                                    property.getName()
                                            + "("
                                            + Character.toString(ck)
                                            + ")\t=\t"
                                            + pCk
                                            + "\t≠\t"
                                            + pRk
                                            + "\t=\t"
                                            + property.getName()
                                            + "("
                                            + Character.toString(rk)
                                            + ")");
                        } else {
                            errorMessageLines.add(
                                    property.getName()
                                            + "("
                                            + Character.toString(ck)
                                            + ")\t=\t"
                                            + pCk
                                            + "\t≠\t"
                                            + Character.toString(
                                                    sets.get(lMatchingForReference).charAt(i))
                                            + "\twhereas\t"
                                            + property.getName()
                                            + "("
                                            + Character.toString(rk)
                                            + ")\t=\t"
                                            + pRk);
                        }
                    }
                }
            }
        }
        if (!errorMessageLines.isEmpty()) {
            testFailureCount++;
            printErrorLine("Test Failure", Side.START, testFailureCount);
            reportTestFailure(
                    file,
                    getLineNumber.apply(pp),
                    String.join("\n", errorMessageLines).replace('\t', ' '));
            out.println("<table class='f'>");
            for (String errorMessageLine : errorMessageLines) {
                out.println("<tr><td>");
                out.println(toHTML.transform(errorMessageLine).replace("\t", "</td><td>"));
                out.println("</tr></td>");
            }
            out.println("</table>");
            printErrorLine("Test Failure", Side.END, testFailureCount);
        }
    }

    private static void equivalencesLine(
            String line,
            ParsePosition pp,
            String file,
            Function<ParsePosition, Integer> getLineNumber)
            throws ParseException {
        final UnicodeSet domain = parseUnicodeSet(line, pp);
        expectToken(",", pp, line);
        expectToken("EqualityOf", pp, line);
        final var leftProperty = CompoundProperty.of(LATEST_PROPS, line, pp);
        scan(PATTERN_WHITE_SPACE, line, pp, true);
        char relationOperator = line.charAt(pp.getIndex());
        pp.setIndex(pp.getIndex() + 1);
        expectToken("EqualityOf", pp, line);
        final var rightProperty = CompoundProperty.of(LATEST_PROPS, line, pp);

        boolean leftShouldImplyRight = false;
        boolean rightShouldImplyLeft = false;

        boolean negated = true;
        switch (relationOperator) {
            case '⇍':
                relationOperator = '⇐';
                break;
            case '⇎':
                relationOperator = '⇔';
                break;
            case '⇏':
                relationOperator = '⇒';
                break;
            default:
                negated = false;
        }

        switch (relationOperator) {
            case '⇐':
                rightShouldImplyLeft = true;
                break;
            case '⇔':
                leftShouldImplyRight = true;
                rightShouldImplyLeft = true;
                break;
            case '⇒':
                leftShouldImplyRight = true;
                break;
            default:
                throw new ParseException(line, pp.getIndex());
        }
        final var leftValues = new HashMap<String, String>();
        final var rightValues = new HashMap<String, String>();
        final var leftClasses = new HashMap<String, UnicodeSet>();
        final var rightClasses = new HashMap<String, UnicodeSet>();
        for (String element : domain) {
            final var leftValue = new StringBuilder();
            final var rightValue = new StringBuilder();
            for (int codepoint : element.codePoints().toArray()) {
                leftValue.append(leftProperty.getValue(codepoint));
                rightValue.append(rightProperty.getValue(codepoint));
            }
            leftValues.put(element, leftValue.toString());
            rightValues.put(element, rightValue.toString());
            leftClasses.computeIfAbsent(leftValue.toString(), (k) -> new UnicodeSet()).add(element);
            rightClasses
                    .computeIfAbsent(rightValue.toString(), (k) -> new UnicodeSet())
                    .add(element);
        }
        UnicodeSet remainingDomain = domain.cloneAsThawed();
        final var leftImpliesRightCounterexamples = new ArrayList<String>();
        final var rightImpliesLeftCounterexamples = new ArrayList<String>();

        // For the implication ⇒, produce at most one counterexample per equivalence class of the
        // left-hand-side equivalence relation: we do not want an example per pair of Unicode code
        // points!
        if (leftShouldImplyRight) {
            while (!remainingDomain.isEmpty()) {
                String representative = remainingDomain.iterator().next();
                UnicodeSet leftEquivalenceClass = leftClasses.get(leftValues.get(representative));
                UnicodeSet rightEquivalenceClass =
                        rightClasses.get(rightValues.get(representative));
                if (leftShouldImplyRight
                        && !rightEquivalenceClass.containsAll(leftEquivalenceClass)) {
                    final String counterexampleRhs =
                            leftEquivalenceClass
                                    .cloneAsThawed()
                                    .removeAll(rightEquivalenceClass)
                                    .iterator()
                                    .next();
                    leftImpliesRightCounterexamples.add(
                            "\t\t"
                                    + leftProperty.getNameAliases()
                                    + "("
                                    + representative
                                    + ") \t=\t "
                                    + leftProperty.getNameAliases()
                                    + "("
                                    + counterexampleRhs
                                    + ") \t=\t "
                                    + leftValues.get(representative)
                                    + " \tbut\t "
                                    + rightValues.get(representative)
                                    + " \t=\t "
                                    + rightProperty.getNameAliases()
                                    + "("
                                    + representative
                                    + ") \t≠\t "
                                    + rightProperty.getNameAliases()
                                    + "("
                                    + counterexampleRhs
                                    + ") \t=\t "
                                    + rightValues.get(counterexampleRhs));
                }
                remainingDomain.removeAll(leftEquivalenceClass);
            }
        }

        // Likewise, for the implication ⇐, produce at most one counterexample per equivalence class
        // of the
        // right-hand-side equivalence relation.
        remainingDomain = domain.cloneAsThawed();
        if (rightShouldImplyLeft) {
            while (!remainingDomain.isEmpty()) {
                String representative = remainingDomain.iterator().next();
                UnicodeSet leftEquivalenceClass = leftClasses.get(leftValues.get(representative));
                UnicodeSet rightEquivalenceClass =
                        rightClasses.get(rightValues.get(representative));
                if (!leftEquivalenceClass.containsAll(rightEquivalenceClass)) {
                    final String counterexampleRhs =
                            rightEquivalenceClass
                                    .cloneAsThawed()
                                    .removeAll(leftEquivalenceClass)
                                    .iterator()
                                    .next();
                    rightImpliesLeftCounterexamples.add(
                            leftValues.get(representative)
                                    + " \t=\t "
                                    + leftProperty.getNameAliases()
                                    + "("
                                    + representative
                                    + ") \t≠\t "
                                    + leftProperty.getNameAliases()
                                    + "("
                                    + counterexampleRhs
                                    + ") \t=\t "
                                    + rightValues.get(counterexampleRhs)
                                    + " \teven though\t "
                                    + rightValues.get(representative)
                                    + " \t=\t "
                                    + rightProperty.getNameAliases()
                                    + "("
                                    + representative
                                    + ") \t=\t "
                                    + rightProperty.getNameAliases()
                                    + "("
                                    + counterexampleRhs
                                    + ")\t\t");
                }
                remainingDomain.removeAll(rightEquivalenceClass);
            }
        }
        final var counterexamples = new ArrayList<>(leftImpliesRightCounterexamples);
        counterexamples.addAll(rightImpliesLeftCounterexamples);
        boolean failure = counterexamples.isEmpty() == negated;
        if (failure) {
            ++testFailureCount;
            printErrorLine("Test Failure", Side.START, testFailureCount);
        }
        final List<String> errorMessageLines = new ArrayList<>();
        if (counterexamples.isEmpty()) {
            errorMessageLines.add("There are no counterexamples to " + relationOperator + ".");
        } else {
            if (leftShouldImplyRight) {
                errorMessageLines.add(
                        "The implication ⇒ is " + leftImpliesRightCounterexamples.isEmpty() + ".");
            }
            if (rightShouldImplyLeft) {
                errorMessageLines.add(
                        "The implication ⇐ is " + rightImpliesLeftCounterexamples.isEmpty() + ".");
            }
        }
        for (var errorLine : errorMessageLines) {
            println(errorLine);
        }
        errorMessageLines.addAll(counterexamples);
        if (failure) {
            reportTestFailure(
                    file,
                    getLineNumber.apply(pp),
                    String.join("\n", errorMessageLines).replace('\t', ' '));
        }
        out.println(failure ? "<table class='f'>" : "<table>");
        for (String counterexample : counterexamples) {
            out.println("<tr><td>");
            out.println(toHTML.transform(counterexample).replace("\t", "</td><td>"));
            out.println("</tr></td>");
        }
        out.println("</table>");
        if (failure) {
            printErrorLine("Test Failure", Side.END, testFailureCount);
        }
    }

    private static void inLine(
            ParsePosition pp,
            String line,
            String file,
            Function<ParsePosition, Integer> getLineNumber)
            throws ParseException {
        final PropertyPredicate propertyPredicate = getPropertyPredicate(pp, line);
        final UnicodeMap<String> failures = propertyPredicate.getFailures();
        final UnicodeSet failureSet = failures.keySet();
        final int failureCount = failureSet.size();
        if (failureCount != 0) {
            testFailureCount++;
            printErrorLine("Test Failure", Side.START, testFailureCount);
            String errorMessage = "Got unexpected property values: " + failureCount;
            println("## " + errorMessage);

            final UnicodeLabel failureProp = new UnicodeProperty.UnicodeMapProperty().set(failures);
            errorLister.setValueSource(failureProp);

            var monoTable = new StringWriter();
            errorLister.setTabber(new Tabber.MonoTabber());
            errorLister.setLineSeparator("\n");
            errorLister.showSetNames(new PrintWriter(monoTable), failureSet);
            errorLister.setTabber(htmlTabber);
            reportTestFailure(
                    file, getLineNumber.apply(pp), errorMessage + "\n" + monoTable.toString());

            if (doHtml) {
                out.println("<table class='f'>");
            }
            errorLister.showSetNames(out, failureSet);
            if (doHtml) {
                out.println("</table>");
            }
            errorLister.setValueSource((UnicodeLabel) null);
            printErrorLine("Test Failure", Side.END, testFailureCount);
            println();
        }
    }

    // A one-token lookahead.
    // Tokens are defined as runs of [^\p{Pattern_White_Space}\p{Pattern_Syntax}],
    // or single code points in \p{Pattern_Syntax}.
    private static class Lookahead {
        // Advances pp through any pattern white space, then looks ahead one token.
        public static Lookahead oneToken(ParsePosition pp, String text) {
            scan(PATTERN_WHITE_SPACE, text, pp, true);
            return oneTokenNoSpace(pp, text);
        }

        // Returns null if pp is before pattern white space; otherwise, looks ahead one token.
        public static Lookahead oneTokenNoSpace(ParsePosition pp, String text) {
            ParsePosition next = new ParsePosition(pp.getIndex());
            if (next.getIndex() == text.length()) {
                return null;
            }
            int start = next.getIndex();
            if (PATTERN_SYNTAX.contains(text.codePointAt(start))) {
                final String result = Character.toString(text.codePointAt(start));
                next.setIndex(start + result.length());
                return new Lookahead(result, pp, next);
            } else {
                final String result = scan(PATTERN_SYNTAX_OR_WHITE_SPACE, text, next, false);
                return result.isEmpty() ? null : new Lookahead(result, pp, next);
            }
        }

        private Lookahead(String token, ParsePosition pp, ParsePosition next) {
            this.token = token;
            this.pp = pp;
            this.next = next;
        }

        // Advances the ParsePosition passed at construction past the token, and returns the token.
        public String consume() {
            pp.setIndex(next.getIndex());
            return token;
        }

        // If this token is expected, advances the ParsePosition passed at construction past the
        // token past it and returns true.
        // Otherwise, this function no effect and returns false.
        public boolean accept(String expected) {
            if (expected.equals(token)) {
                consume();
                return true;
            } else {
                return false;
            }
        }

        private final String token;
        private final ParsePosition pp;
        private final ParsePosition next;
    }

    private static void expectToken(String token, ParsePosition pp, String text)
            throws ParseException {
        if (!Lookahead.oneToken(pp, text).accept(token)) {
            throw new ParseException("Expected '" + token + "'", pp.getIndex());
        }
    }

    private static PropertyPredicate getPropertyPredicate(ParsePosition pp, String line)
            throws ParseException {
        PropertyPredicate predicate;

        final UnicodeSet valueSet = parseUnicodeSet(line, pp);
        expectToken(",", pp, line);
        final UnicodeProperty property1 = CompoundProperty.of(LATEST_PROPS, line, pp);
        final int cp = line.codePointAt(pp.getIndex());
        switch (cp) {
            case '=':
            case '≠':
                final var comparison = new PropertyComparison();
                comparison.shouldBeEqual = cp == '=';
                pp.setIndex(pp.getIndex() + 1);
                comparison.property2 = CompoundProperty.of(LATEST_PROPS, line, pp);
                predicate = comparison;
                break;
            case '∈':
            case '∉':
                final var containment = new PropertyValueContainment();
                containment.shouldBeInSet = cp == '∈';
                pp.setIndex(pp.getIndex() + 1);
                containment.set = parseUnicodeSet(line, pp);
                predicate = containment;
                break;
            default:
                throw new ParseException("Expected =|≠|∈|∉", pp.getIndex());
        }
        predicate.valueSet = valueSet;
        predicate.property1 = property1;
        scan(PATTERN_WHITE_SPACE, line, pp, true);
        return predicate;
    }

    static class CompoundProperty extends UnicodeProperty {
        static class FilterOrProp {
            enum Type {
                filter,
                prop,
                stringprop,
                sequenceTransformation,
            };

            private Type type;
            private UnicodeProperty prop;
            private UnicodeSet filter;
            private Function<List<String>, List<String>> sequenceTransformation;
            private Function<List<String>, String> sequenceReduction;
        }

        // TODO(egg): Consider bringing back Pattern_White_Space if requiring semicolons.
        private static final UnicodeSet PROPCHARS = new UnicodeSet("[a-zA-Z0-9.\\:\\-\\_\\u0020}]");
        private final List<FilterOrProp> propOrFilters = new ArrayList<FilterOrProp>();

        static UnicodeProperty of(
                UnicodeProperty.Factory propSource, String source, ParsePosition pp)
                throws ParseException {
            final CompoundProperty result = new CompoundProperty();
            while (true) {
                scan(PATTERN_WHITE_SPACE, source, pp, true);
                if (UnicodeSet.resemblesPattern(source, pp.getIndex())) {
                    final FilterOrProp propOrFilter = new FilterOrProp();
                    propOrFilter.filter = parseUnicodeSet(source, pp);
                    propOrFilter.type = FilterOrProp.Type.filter;
                    result.propOrFilters.add(propOrFilter);
                } else if (source.charAt(pp.getIndex()) == '(') {
                    final FilterOrProp propOrFilter = new FilterOrProp();
                    final var matcher =
                            Pattern.compile("(\\( *([^ )]+)(?: +([^)]+))? *\\)).*", Pattern.DOTALL)
                                    .matcher(source.subSequence(pp.getIndex(), source.length()));
                    if (!matcher.matches()) {
                        throw new IllegalArgumentException(
                                "Expected (<operation> <args>), got "
                                        + source.substring(
                                                pp.getIndex(),
                                                Math.min(pp.getIndex() + 50, source.length()))
                                        + "…");
                    }
                    propOrFilter.type = FilterOrProp.Type.sequenceTransformation;
                    final String expression = matcher.group(1);
                    final String operation = matcher.group(2);
                    final String args = matcher.group(3);
                    switch (operation) {
                        case "take":
                            {
                                final int count = Integer.parseInt(args);
                                propOrFilter.sequenceTransformation = s -> s.subList(0, count);
                                break;
                            }
                        case "drop":
                            {
                                final int count = Integer.parseInt(args);
                                propOrFilter.sequenceTransformation =
                                        s -> s.subList(count, s.size());
                                break;
                            }
                        case "delete-adjacent-duplicates":
                            {
                                propOrFilter.sequenceTransformation =
                                        s -> {
                                            if (s.isEmpty()) {
                                                return s;
                                            }
                                            int j = 0;
                                            for (int i = 1; i < s.size(); ++i) {
                                                if (!Objects.equals(s.get(i), s.get(j))) {
                                                    s.set(++j, s.get(i));
                                                }
                                            }
                                            s.subList(j + 1, s.size()).clear();
                                            return s;
                                        };
                                break;
                            }
                        case "prepend":
                            {
                                propOrFilter.sequenceTransformation =
                                        s -> {
                                            s.add(0, args);
                                            return s;
                                        };
                                break;
                            }
                        case "append":
                            {
                                propOrFilter.sequenceTransformation =
                                        s -> {
                                            s.add(args);
                                            return s;
                                        };
                                break;
                            }
                        case "string-join":
                            {
                                propOrFilter.sequenceReduction = s -> String.join("", s);
                                break;
                            }
                        case "constant":
                            {
                                propOrFilter.sequenceReduction = s -> args;
                                break;
                            }
                        default:
                            throw new IllegalArgumentException(
                                    "Unknown operation " + matcher.group(1));
                    }
                    result.propOrFilters.add(propOrFilter);
                    pp.setIndex(pp.getIndex() + expression.length());
                } else {
                    final String propName = scan(PROPCHARS, source, pp, true);
                    if (propName.length() > 0) {
                        final FilterOrProp propOrFilter = new FilterOrProp();
                        final VersionedProperty xprop =
                                VersionedProperty.forInvariantTesting().set(propName);
                        propOrFilter.prop = xprop.getProperty();
                        if (propOrFilter.prop == null) {
                            throw new IllegalArgumentException(
                                    "Can't create property for: " + propName);
                        }
                        propOrFilter.type =
                                propOrFilter.prop.getType() == UnicodeProperty.STRING
                                                || propOrFilter.prop.getType()
                                                        == UnicodeProperty.EXTENDED_STRING
                                        ? FilterOrProp.Type.stringprop
                                        : FilterOrProp.Type.prop;
                        result.propOrFilters.add(propOrFilter);
                    } else {
                        break;
                    }
                }
                scan(PATTERN_WHITE_SPACE, source, pp, true);
                final int pos = pp.getIndex();
                if (pos == source.length()) {
                    break;
                }
                final int cp = source.charAt(pos);
                if (cp != '*') {
                    break;
                }
                pp.setIndex(pos + 1);
                // keep looping
            }
            return result;
        }

        private CompoundProperty() {}

        @Override
        protected List<String> _getAvailableValues(List<String> result) {
            return propOrFilters.get(0).prop.getAvailableValues(result);
        }

        @Override
        protected List<String> _getNameAliases(List<String> result) {
            final StringBuffer name = new StringBuffer();
            for (int i = 0; i < propOrFilters.size(); ++i) {
                if (i != 0) {
                    name.append("*");
                }
                name.append(propOrFilters.get(i).prop.getFirstNameAlias());
            }
            result.add(name.toString());
            return result;
        }

        @Override
        protected String _getValue(int codepoint) {
            final StringBuffer buffer = new StringBuffer();
            String value = Character.toString(codepoint);
            List<String> values = null;
            int cp;

            for (int i = propOrFilters.size() - 1; i >= 0; --i) {
                final FilterOrProp propOrFilter = propOrFilters.get(i);
                switch (propOrFilter.type) {
                    case filter:
                        if (value == null) {
                            throw new IllegalArgumentException(
                                    "Cannot apply filter  "
                                            + propOrFilter.filter.toString()
                                            + " to sequence "
                                            + values);
                        }
                        buffer.setLength(0);
                        for (int j = 0; j < value.length(); j += UTF16.getCharCount(cp)) {
                            cp = UTF16.charAt(value, j);
                            if (!propOrFilter.filter.contains(cp)) {
                                continue;
                            }
                            buffer.appendCodePoint(cp);
                        }
                        value = buffer.toString();
                        break;
                    case stringprop:
                        if (value == null) {
                            throw new IllegalArgumentException(
                                    "Cannot apply string property "
                                            + propOrFilter.prop.getName()
                                            + " to sequence "
                                            + values);
                        }
                        buffer.setLength(0);
                        for (int j = 0; j < value.length(); j += UTF16.getCharCount(cp)) {
                            cp = UTF16.charAt(value, j);
                            final String value2 = propOrFilter.prop.getValue(cp);
                            buffer.append(value2);
                        }
                        value = buffer.toString();
                        break;
                    case prop:
                        if (value == null) {
                            throw new IllegalArgumentException(
                                    "Cannot apply enumerated property "
                                            + propOrFilter.prop.getName()
                                            + " to sequence "
                                            + values);
                        }
                        values = new ArrayList<>();
                        for (int j = 0; j < value.length(); j += UTF16.getCharCount(cp)) {
                            cp = UTF16.charAt(value, j);
                            final String value2 = propOrFilter.prop.getValue(cp);
                            values.add(value2);
                        }
                        value = null;
                        break;
                    case sequenceTransformation:
                        final boolean wasString = value != null;
                        if (wasString) {
                            values =
                                    value.codePoints()
                                            .mapToObj(Character::toString)
                                            .collect(
                                                    Collectors.toCollection(
                                                            () -> new ArrayList<>()));
                            value = null;
                        }
                        if (propOrFilter.sequenceTransformation != null) {
                            values = propOrFilter.sequenceTransformation.apply(values);
                            if (wasString) {
                                value = String.join("", values);
                                values = null;
                            }
                        } else {
                            value = propOrFilter.sequenceReduction.apply(values);
                            values = null;
                        }
                        break;
                }
            }
            if (value == null) {
                if (values.isEmpty()) {
                    return "";
                } else if (values.size() == 1) {
                    return values.get(0);
                } else {
                    throw new IllegalArgumentException(
                            "Compound property must return a string, not sequence " + values);
                }
            }
            return value;
        }

        @Override
        protected List<String> _getValueAliases(String valueAlias, List<String> result) {
            return propOrFilters.get(0).prop.getAvailableValues(result);
        }

        @Override
        protected String _getVersion() {
            return propOrFilters.get(0).prop.getVersion();
        }
    }

    private static void letLine(ParsePosition pp, String source) throws ParseException {
        expectToken("$", pp, source);
        final String variable = Lookahead.oneTokenNoSpace(pp, source).consume();
        expectToken("=", pp, source);
        final int valueStart = pp.getIndex();
        final UnicodeSet valueSet = parseUnicodeSet(source, pp);
        valueSet.complement().complement();

        symbolTable.add(variable, valueSet.toPattern(false));
        final String value = source.substring(valueStart, pp.getIndex());
        if (DEBUG) {
            System.out.println("Added variable: <" + variable + "><" + value + ">");
        }
        showSet(new ParsePosition(0), value);
    }

    private static void showLine(String source, ParsePosition pp) throws ParseException {
        if (Lookahead.oneToken(pp, source).accept("Each")) {
            showLister.setMergeRanges(false);
        }
        showSet(pp, source);
        showLister.setMergeRanges(doRange);
    }

    private static void showMapLine(String line, ParsePosition pp) {
        String part = line.substring(7).trim();
        pp.setErrorIndex(-1);
        if (part.startsWith("Each")) {
            part = part.substring(4).trim();
            showLister.setMergeRanges(false);
        }
        UnicodeMap<String> um = UMP.parse(part, pp);
        if (pp.getErrorIndex() != -1 || pp.getIndex() != part.length()) {
            throw new IllegalArgumentException(pp.toString());
        }
        showLister.setValueSource(new UnicodeProperty.UnicodeMapProperty().set(um));
        showLister.showSetNames(out, um.keySet());
        showLister.setMergeRanges(doRange);
    }

    private static void testLine(
            String source,
            ParsePosition pp,
            String file,
            Function<ParsePosition, Integer> getLineNumber)
            throws ParseException {
        char relation = 0;
        String rightSide = null;
        String leftSide = null;
        UnicodeSet leftSet = null;
        UnicodeSet rightSet = null;

        final int leftStart = pp.getIndex();
        leftSet = parseUnicodeSet(source, pp);
        leftSide = source.substring(leftStart, pp.getIndex());
        scan(PATTERN_WHITE_SPACE, source, pp, true);
        relation = source.charAt(pp.getIndex());
        checkRelation(pp, relation);
        pp.setIndex(pp.getIndex() + 1); // skip char
        final int rightStart = pp.getIndex();
        rightSet = parseUnicodeSet(source, pp);
        rightSide = source.substring(rightStart, pp.getIndex());

        Expected right_left = Expected.irrelevant;
        Expected rightAndLeft = Expected.irrelevant;
        Expected left_right = Expected.irrelevant;
        switch (relation) {
            case '=':
                right_left = left_right = Expected.empty;
                break;
            case '\u2282':
                right_left = Expected.not_empty;
                left_right = Expected.empty;
                break;
            case '\u2283':
                right_left = Expected.empty;
                left_right = Expected.not_empty;
                break;
            case '\u2286':
                left_right = Expected.empty;
                break;
            case '\u2287':
                right_left = Expected.empty;
                break;
            case '∥':
                rightAndLeft = Expected.empty;
                break;
            case '≉':
                right_left = Expected.not_empty;
                left_right = Expected.not_empty;
                rightAndLeft = Expected.not_empty;
                break;
            default:
                throw new IllegalArgumentException("Internal Error");
        }

        checkExpected(
                right_left,
                new UnicodeSet(rightSet).removeAll(leftSet),
                "In",
                rightSide,
                "But Not In",
                leftSide,
                file,
                getLineNumber.apply(pp));
        checkExpected(
                rightAndLeft,
                new UnicodeSet(rightSet).retainAll(leftSet),
                "In",
                rightSide,
                "And In",
                leftSide,
                file,
                getLineNumber.apply(pp));
        checkExpected(
                left_right,
                new UnicodeSet(leftSet).removeAll(rightSet),
                "In",
                leftSide,
                "But Not In",
                rightSide,
                file,
                getLineNumber.apply(pp));
    }

    public static void checkRelation(ParsePosition pp, char relation) throws ParseException {
        if (!INVARIANT_RELATIONS.contains(relation)) {
            throw new ParseException(
                    "Invalid relation «"
                            + relation
                            + "», must be one of "
                            + INVARIANT_RELATIONS.toPattern(false),
                    pp.getIndex());
        }
    }

    private static void checkExpected(
            Expected expected,
            UnicodeSet segment,
            String rightStatus,
            String rightSide,
            String leftStatus,
            String leftSide,
            String file,
            int lineNumber) {
        switch (expected) {
            case empty:
                if (segment.size() == 0) {
                    return;
                } else {
                    break;
                }
            case not_empty:
                if (segment.size() != 0) {
                    return;
                } else {
                    break;
                }
            case irrelevant:
                return;
        }
        testFailureCount++;
        printErrorLine("Test Failure", Side.START, testFailureCount);
        final var errorMessageLines =
                new String[] {
                    "Expected " + expected + ", got: " + segment.size() + "\t" + segment.toString(),
                    rightStatus + "\t" + rightSide,
                    leftStatus + "\t" + leftSide
                };
        var monoTable = new StringWriter();
        for (String line : errorMessageLines) {
            println("## " + line);
        }
        errorLister.setTabber(new Tabber.MonoTabber());
        errorLister.setLineSeparator("\n");
        errorLister.showSetNames(new PrintWriter(monoTable), segment);
        reportTestFailure(
                file,
                lineNumber,
                String.join("\n", errorMessageLines) + "\n" + monoTable.toString());
        errorLister.setTabber(htmlTabber);
        if (doHtml) {
            out.println("<table class='e'>");
        }
        errorLister.showSetNames(out, segment);
        if (doHtml) {
            out.println("</table>");
        }
        printErrorLine("Test Failure", Side.END, testFailureCount);
        println();
    }

    static UnicodeMapParser<String> UMP =
            UnicodeMapParser.create(
                    UnicodeMapParser.STRING_VALUE_PARSER,
                    new UnicodeMapParser.ChainedFactory(
                            getProperties(Settings.latestVersion),
                            IndexUnicodeProperties.make(Settings.latestVersion)),
                    new UnicodeMapParser.ChainedFactory(
                            getProperties(Settings.lastVersion),
                            IndexUnicodeProperties.make(Settings.lastVersion)));

    private static void testMapLine(
            String line, ParsePosition pp, Function<ParsePosition, Integer> getLineNumber)
            throws ParseException {
        char relation = 0;
        String rightSide = null;
        String leftSide = null;
        UnicodeMap<String> leftSet = null;
        UnicodeMap<String> rightSet = null;

        leftSet = UMP.parse(line, pp);
        leftSide = line.substring(3, pp.getIndex());
        scan(PATTERN_WHITE_SPACE, line, pp, true);
        relation = line.charAt(pp.getIndex());
        checkRelation(pp, relation);
        pp.setIndex(pp.getIndex() + 1); // skip char
        scan(PATTERN_WHITE_SPACE, line, pp, true);
        final int start = pp.getIndex();
        rightSet = UMP.parse(line, pp); // new UnicodeSet(line, pp, symbolTable);
        rightSide = line.substring(start, pp.getIndex());
        scan(PATTERN_WHITE_SPACE, line, pp, true);
        if (line.length() != pp.getIndex()) {
            throw new ParseException("Extra characters at end", pp.getIndex());
        }

        Expected right_left = Expected.irrelevant;
        Expected rightAndLeft = Expected.irrelevant;
        Expected left_right = Expected.irrelevant;
        switch (relation) {
            case '=':
                right_left = left_right = Expected.empty;
                break;
            case '\u2282':
                right_left = Expected.not_empty;
                left_right = Expected.empty;
                break;
            case '\u2283':
                right_left = Expected.empty;
                left_right = Expected.not_empty;
                break;
            case '\u2286':
                left_right = Expected.empty;
                break;
            case '\u2287':
                right_left = Expected.empty;
                break;
            case '∥':
                rightAndLeft = Expected.empty;
                break;
            case '≉':
                right_left = Expected.not_empty;
                left_right = Expected.not_empty;
                rightAndLeft = Expected.not_empty;
                break;
            default:
                throw new IllegalArgumentException("Internal Error");
        }

        checkExpected(
                right_left,
                UnicodeMapParser.removeAll(new UnicodeMap<String>().putAll(rightSet), leftSet),
                "In",
                rightSide,
                "But Not In",
                leftSide,
                getLineNumber.apply(pp));
        checkExpected(
                rightAndLeft,
                UnicodeMapParser.retainAll(new UnicodeMap<String>().putAll(rightSet), leftSet),
                "In",
                rightSide,
                "And In",
                leftSide,
                getLineNumber.apply(pp));
        checkExpected(
                left_right,
                UnicodeMapParser.removeAll(new UnicodeMap<String>().putAll(leftSet), rightSet),
                "In",
                leftSide,
                "But Not In",
                rightSide,
                getLineNumber.apply(pp));
    }

    private static void checkExpected(
            Expected expected,
            UnicodeMap<String> segment,
            String rightStatus,
            String rightSide,
            String leftStatus,
            String leftSide,
            int lineNumber) {
        switch (expected) {
            case empty:
                if (segment.size() == 0) {
                    return;
                } else {
                    break;
                }
            case not_empty:
                if (segment.size() != 0) {
                    return;
                } else {
                    break;
                }
            case irrelevant:
                return;
        }
        testFailureCount++;
        printErrorLine("Test Failure", Side.START, testFailureCount);
        println("## Expected " + expected + ", got: " + segment.size());
        println("## " + rightStatus + "\t" + rightSide);
        println("## " + leftStatus + "\t" + leftSide);
        println(segment.toString());
        //        if (doHtml) { out.println("<table class='e'>"); }
        //        errorLister.showSetNames(out, segment);
        //        if (doHtml) { out.println("</table>"); }
        printErrorLine("Test Failure", Side.END, testFailureCount);
        println();
    }

    static NumberFormat nf = NumberFormat.getIntegerInstance();

    static {
        nf.setGroupingUsed(true);
    }

    private static void showSet(ParsePosition pp, final String value) throws ParseException {
        UnicodeSet valueSet = parseUnicodeSet(value, pp);
        final int totalSize = valueSet.size();
        int abbreviated = 0;
        if (showRangeLimit >= 0) {
            final UnicodeSet shorter = new UnicodeSet();
            int rangeLimit = showRangeLimit;
            for (final UnicodeSetIterator it = new UnicodeSetIterator(valueSet);
                    it.nextRange() && rangeLimit > 0;
                    --rangeLimit) {
                if (it.codepoint == it.IS_STRING) {
                    continue; // TODO(egg): Show strings too.
                }
                shorter.add(it.codepoint, it.codepointEnd);
            }
            abbreviated = totalSize - shorter.size();
            valueSet = shorter;
        }
        if (doHtml) {
            out.println("<table class='s'>");
        }
        // Show the GC if it happens to be constant over a range, but do not split because of it:
        // We limit the output based on unsplit ranges.
        showLister
                .setLabelSource(null)
                .setRangeBreakSource(null)
                .setRefinedLabelSource(LATEST_PROPS.getProperty("General_Category"));
        showLister.showSetNames(out, valueSet);
        if (doHtml) {
            out.println("</table>");
        }
        println(
                "## Total:\t"
                        + nf.format(totalSize)
                        + (abbreviated == 0
                                ? ""
                                : "\t(omitting " + nf.format(abbreviated) + " from listing)"));
        println();
    }

    private static int parseError(
            int parseErrorCount,
            String source,
            Exception e,
            int statementStart,
            String file,
            int lineNumber) {
        parseErrorCount++;
        if (e instanceof ParseException) {
            final int index = ((ParseException) e).getErrorOffset();
            final int eol = source.indexOf("\n", index);
            source =
                    source.substring(statementStart, index)
                            + (e instanceof BackwardParseException ? "☜" : "☞")
                            + source.substring(index, eol >= 0 ? eol : source.length());
        } else {
            final int sol = source.lastIndexOf("\n", statementStart);
            final int eol = source.indexOf("\n", statementStart);
            source = source.substring(sol >= 0 ? sol : 0, eol >= 0 ? eol : source.length());
        }

        printErrorLine("Parse Failure", Side.START, parseErrorCount);
        println("**** PARSE ERROR:\t" + source);
        out.println("<pre>");
        final String message = e.getMessage();
        if (message != null) {
            println("##" + message);
        }
        reportParseError(file, lineNumber, message + "\n" + source);
        e.printStackTrace(out);

        out.println("</pre>");
        printErrorLine("Parse Error", Side.END, parseErrorCount);
        println();
        return parseErrorCount;
    }

    enum Side {
        START,
        END
    };

    private static void printErrorLine(String title, Side side, int testFailureCount) {
        title = title + " " + testFailureCount;
        println("**** " + side + " " + title + " ****", title.replace(' ', '_'));
    }

    private static final String BASE_RULES =
            ":: (hex-any/xml);"
                    + ":: (hex-any/xml10);"
                    + "'<' > '&lt;' ;"
                    + "'<' < '&'[lL][Tt]';' ;"
                    + "'&' > '&amp;' ;"
                    + "'&' < '&'[aA][mM][pP]';' ;"
                    + "'>' < '&'[gG][tT]';' ;"
                    + "'\"' < '&'[qQ][uU][oO][tT]';' ; "
                    + "'' < '&'[aA][pP][oO][sS]';' ; ";

    private static final String CONTENT_RULES = "'>' > '&gt;' ;";

    private static final String HTML_RULES = BASE_RULES + CONTENT_RULES + "'\"' > '&quot;' ; ";

    private static final String HTML_RULES_CONTROLS =
            HTML_RULES
                    + ":: [[:C:][:Z:][:whitespace:][:Default_Ignorable_Code_Point:] - [\\u0020\\u0009\\u000A]] hex/unicode ; "
                    + "\\u000A > '<br>'";

    public static final Transliterator toHTMLControl =
            Transliterator.createFromRules("any-html", HTML_RULES_CONTROLS, Transliterator.FORWARD);
    private static final UnicodeSet PATTERN_WHITE_SPACE =
            new UnicodeSet("\\p{pattern white space}").freeze();
    private static final UnicodeSet PATTERN_SYNTAX = new UnicodeSet("\\p{pattern syntax}").freeze();
    private static final UnicodeSet PATTERN_SYNTAX_OR_WHITE_SPACE =
            new UnicodeSet("[\\p{pattern white space}\\p{pattern syntax}]").freeze();

    private static int testFailureCount;
    private static int parseErrorCount;
    private static BagFormatter errorLister;
    private static BagFormatter showLister;
    private static ChainedSymbolTable symbolTable = new ChainedSymbolTable();
    private static boolean doRange;

    private static void println(String line) {
        println(line, null);
    }

    private static void println(String line, String anchor) {
        if (doHtml) {
            if (line.trim().length() == 0) {
                out.println("<br>");
            } else if (line.equals("##########################")) {
                out.println("<hr>");
            } else {
                line = toHTMLControl.transliterate(line);
                final int commentPos = line.indexOf('#');
                if (commentPos >= 0) {
                    String aClass = "b";
                    if (line.length() > commentPos + 1 && line.charAt(commentPos + 1) == '#') {
                        aClass = "bb";
                    }
                    out.println(
                            "<p><code>"
                                    + line.substring(0, commentPos)
                                    + "</code><span class='"
                                    + aClass
                                    + "'>"
                                    + line.substring(commentPos)
                                    + "</span></p>");
                } else if (line.startsWith("****")) {
                    out.println(
                            "<h2>"
                                    + (anchor == null ? "" : "<a name='" + anchor + "'>")
                                    + line
                                    + (anchor == null ? "" : "</a>")
                                    + "</h2>");
                } else {
                    out.println("<p><code>" + line + "</code></p>");
                }
            }
        } else {
            out.println(line);
        }
    }

    private static void println() {
        println("");
    }

    private static void reportParseError(String file, int lineNumber, String message) {
        reportError(file, lineNumber, "Parse error", message);
    }

    private static void reportTestFailure(String file, int lineNumber, String message) {
        reportError(file, lineNumber, "Invariant test failure", message);
    }

    private static void reportError(String file, int lineNumber, String title, String message) {
        if (EMIT_GITHUB_ERRORS) {
            System.err.println(
                    "::error file=unicodetools/src/main/resources/org/unicode/text/UCD/"
                            + file
                            + ",line="
                            + lineNumber
                            + ",title="
                            + title
                            + "::"
                            + message.replace("%", "%25").replace("\n", "%0A"));
        }
    }

    /** Should add to UnicodeSet */
    public static String scan(UnicodeSet unicodeSet, String line, ParsePosition pp, boolean in) {
        final int start = pp.getIndex();
        final int i = scan(unicodeSet, line, start, in);
        final String result = line.substring(start, i);
        pp.setIndex(i);
        return result;
    }

    private static int scan(UnicodeSet allowed, CharSequence line, int start, boolean in) {
        int cp = 0;
        int i;
        for (i = start; i < line.length(); i += UTF16.getCharCount(cp)) {
            cp = Character.codePointAt(line, i);
            if (allowed.contains(cp) != in) {
                break;
            }
        }
        return i;
    }

    private static Factory getProperties(final String version) {
        return ICU_VERSION ? ICUPropertyFactory.make() : ToolUnicodePropertySource.make(version);
    }

    static class ChainedSymbolTable extends UnicodeSet.XSymbolTable {

        private static final Comparator<String> LONGEST_FIRST =
                new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        final int len = o2.length() - o1.length();
                        if (len != 0) {
                            return len;
                        }
                        return o1.compareTo(o2);
                    }
                };

        Map<String, char[]> variables = new TreeMap<String, char[]>(LONGEST_FIRST);

        public void add(String variable, String value) {
            if (variables.containsKey(variable)) {
                throw new IllegalArgumentException("Attempt to reset variable " + variable);
            }
            variables.put(variable, value.toCharArray());
        }

        @Override
        public char[] lookup(String s) {
            if (SHOW_LOOKUP) {
                System.out.println(
                        "\tlookup: " + s + "\treturns\t" + String.valueOf(variables.get(s)));
            }
            return variables.get(s);
        }

        // Warning: this depends on pos being left alone unless a string is returned!!
        @Override
        public String parseReference(String text, ParsePosition pos, int limit) {
            //      for (String variable : variables.keySet()) {
            //        final int index = pos.getIndex();
            //        if (text.regionMatches(index, variable, 0, variable.length())) {
            //          pos.setIndex(index + variable.length());
            //          System.out.println("parseReference: " + variable + "\t in\t" + text);
            //          return variable;
            //        }
            //      }
            //      System.out.println("parseReference: missing" + "\t in\t" + text);
            //      return null;
            final int start = pos.getIndex();
            int i = start;
            while (i < limit) {
                final char c = text.charAt(i);
                if ((i == start && !UCharacter.isUnicodeIdentifierStart(c))
                        || !UCharacter.isUnicodeIdentifierPart(c)) {
                    break;
                }
                ++i;
            }
            if (i == start) { // No valid name chars
                return null;
            }
            pos.setIndex(i);
            return text.substring(start, i);
        }

        final VersionedProperty propertyVersion = VersionedProperty.forInvariantTesting();

        @Override
        public boolean applyPropertyAlias(
                String propertyName2, String propertyValue, UnicodeSet result) {
            result.clear();
            result.addAll(
                    propertyVersion
                            .set(propertyName2)
                            .getSet(propertyValue, symbolTable, symbolTable.variables));
            return true;
        }
    }

    // Some of our parse exceptions are thrown with a parse position before the problem.
    // However, others are thrown with the parse position after the problem, so the message must be
    // adjusted accordingly.
    public static class BackwardParseException extends ParseException {
        public BackwardParseException(String s, int errorOffset) {
            super(s, errorOffset);
        }
    }

    public static UnicodeSet parseUnicodeSet(String source, ParsePosition pp)
            throws ParseException {
        try {
            final var result = new UnicodeSet(source, pp, symbolTable);
            return result;
        } catch (IllegalArgumentException e) {
            // ICU produces unhelpful messages when parsing UnicodeSet deep into
            // a large string in a string that contains line terminators, as the
            // whole string is escaped and printed.
            final String message = e.getMessage().split(" at \"", 2)[0];
            throw new BackwardParseException(message, pp.getIndex());
        }
    }
}
