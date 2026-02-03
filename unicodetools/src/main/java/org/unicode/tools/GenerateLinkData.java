package org.unicode.tools;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.SimpleFormatter;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.OutputInt;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Rational.MutableLong;
import org.unicode.cldr.util.props.UnicodeLabel;
import org.unicode.props.BagFormatter;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;
import org.unicode.text.utility.DiffingPrintWriter;
import org.unicode.utilities.LinkUtilities;
import org.unicode.utilities.LinkUtilities.LinkTermination;
import org.unicode.utilities.LinkUtilities.Part;
import org.unicode.utilities.LinkUtilities.UrlInternals;

/**
 * Generate UTS #58 property and test files
 *
 * <ul>
 *   <li>Generate the LinkFormatting.txt file. Use some static examples + testUrls.txt (see the
 *       LinkUtilitiesTest.java file for code).
 *   <li>Other fixes from props agenda
 *   <li>…
 * </ul>
 *
 * @throws IOException
 */
class GenerateLinkData {

    private static final Transliterator FIX_ODD =
            Transliterator.createFromRules(
                    "any-html",
                    ":: [[:C:][:Z:][:whitespace:][:Default_Ignorable_Code_Point:]] hex/unicode ; ",
                    Transliterator.FORWARD);

    private static final boolean ADDTEST = false; // set to true to generate LinkDetectionTestSource

    private static final Joiner JOIN_SEMI_SP = Joiner.on(" ;\t");
    private static final Joiner JOIN_N = Joiner.on('\n');
    private static final Joiner JOIN_N_HASH = Joiner.on("\n# ");
    private static final Splitter SPLIT_TABS = Splitter.on('\t').omitEmptyStrings().trimResults();
    private static final Splitter SPLIT_SEMI = Splitter.on(';').trimResults();

    private static final String HEADER_BASE =
            JOIN_N_HASH.join(
                    "# {0}.txt",
                    "Date: {1} ",
                    "© {2} Unicode®, Inc.",
                    "Unicode and the Unicode Logo are registered trademarks of Unicode, Inc. in the U.S. and other countries.",
                    "For terms of use and license, see https://www.unicode.org/terms_of_use.html",
                    "",
                    "The usage and stability of these values is covered in https://www.unicode.org/reports/tr58/",
                    "",
                    "================================================",
                    "");

    public static void main(String[] args) throws IOException {
        generatePropertyData();
        generateDetectionTestData();
        generateFormattingTestData();
    }

    static final Instant now = Instant.now();
    static final DateTimeFormatter dt =
            DateTimeFormatter.ofPattern("yyyy-MM-dd, HH:mm:ss' GMT'")
                    .withZone(ZoneId.of("UTC")); // Explicitly set to UTC/GMT
    static final DateTimeFormatter dty =
            DateTimeFormatter.ofPattern("y")
                    .withZone(ZoneId.of("UTC")); // Explicitly set to UTC/GMT

    static final SimpleFormatter HEADER_PROP_TERM =
            SimpleFormatter.compile(
                    HEADER_BASE
                            + JOIN_N_HASH.join(
                                    "",
                                    "Property:\t{3}",
                                    "Format",
                                    "",
                                    "Field 0: code point range",
                                    "Field 1: a {3} value",
                                    "         For more information, see https://www.unicode.org/reports/tr58/#property-data. ",
                                    "",
                                    "For the purpose of regular expressions, the property {3} is defined as",
                                    "an enumerated property of code points.",
                                    "The short name of the property is the same as its long name.",
                                    "The possible values are:  Include, Hard, Soft, Close, Open",
                                    "",
                                    "The short name of each value is the same as its long name.",
                                    "",
                                    " All code points not explicitly listed for {3}",
                                    " have the value {4}.",
                                    "",
                                    "@missing: 0000..10FFFF; {4}",
                                    "",
                                    "================================================",
                                    ""));

    static final SimpleFormatter HEADER_PROP_STRING =
            SimpleFormatter.compile(
                    HEADER_BASE
                            + JOIN_N_HASH.join(
                                    "",
                                    "Property:\t{3}",
                                    "Format",
                                    "",
                                    "Field 0: code point",
                                    "Field 1: code point",
                                    "         For more information, see https://www.unicode.org/reports/tr58/#property-data. ",
                                    "",
                                    "For the purpose of regular expressions, the property {3} is defined as",
                                    "a string property whose value is either a single code point or is {4}.",
                                    "",
                                    "The short name of the property is the same as its long name.",
                                    "",
                                    " All code points not explicitly listed for {3}",
                                    " have the value {4}.",
                                    "",
                                    "@missing: 0000..10FFFF; {4}",
                                    "",
                                    "================================================",
                                    ""));

    static final SimpleFormatter HEADER_PROP_BINARY =
            SimpleFormatter.compile(
                    HEADER_BASE
                            + JOIN_N_HASH.join(
                                    "",
                                    "Property:\t{3}",
                                    "Format",
                                    "",
                                    "Field 0: code point range",
                                    "         For more information, see https://www.unicode.org/reports/tr58/#property-data. ",
                                    "",
                                    "For the purpose of regular expressions, the property {3} is defined as",
                                    "a binary property.",
                                    "",
                                    "The short name of the property is the same as its long name.",
                                    "",
                                    " All code points not explicitly listed for {3}",
                                    " have the value {4}."
                                    //                            , ""
                                    //                            , "@missing: 0000..10FFFF; {4}"
                                    ,
                                    "",
                                    "================================================",
                                    ""));

    static final SimpleFormatter HEADER_DETECT_TEST =
            SimpleFormatter.compile(
                    HEADER_BASE
                            + JOIN_N_HASH.join(
                                    "",
                                    "Format:",
                                    "  Each line contains zero or more marked links, such as ⸠abc.com⸡",
                                    "",
                                    "Operation:",
                                    "  For each line.",
                                    "  • Create a copy of the line, with the characters ⸠ and ⸡ removed.",
                                    "  • Run link detection on the line, inserting ⸠ and ⸡ around each detected link.",
                                    "  • Report a failure if the result is not identical to the original line.",
                                    "  Empty lines, and lines starting with # are ignored.",
                                    "  Otherwise # is treated like any other character.",
                                    "================================================",
                                    ""));

    static final SimpleFormatter HEADER_FORMAT_TEST =
            SimpleFormatter.compile(
                    HEADER_BASE
                            + JOIN_N_HASH.join(
                                    "",
                                    "Format: This is not a file with semicolon-delimited fields.",
                                    "Instead, it consists of paired lines, separated by empty lines and/or comment lines.",
                                    "The first line of each pair is the source, fully escaped.",
                                    "The second line of each pair is the result, minimally escaped.",
                                    "",
                                    "Comments are lines that begin with a #.",
                                    "",
                                    "The fully-escaped field percent-escapes all literal syntax characters and all characters above ASCII.",
                                    "The minimally-escaped field is the more readable format described in UTS #58.",
                                    "Each pair also has a comment line for the internal structure of the URL.",
                                    "𝑺 = the schema",
                                    "𝑯 = the host (typically just a domain name) the internal structure is not broken down.",
                                    "𝑷 = indicates one of the labels in the path. So /seg1/seg2 becomes 𝑷=seg1 𝑷=seg1",
                                    "𝑸 = each key in the query. So &θ=ικλ&μ=γξο becomes 𝑸=θ 𝑽=ικλ 𝑸=μ 𝑽=γξο.",
                                    "𝑽 = a value for the preceding key",
                                    "𝑭 = a fragment. So #reserved:~:text=Reserved,into:~:text=open,closed becomes 𝑭=reserved 𝑫=Reserved,into 𝑫=text=open,closed",
                                    "𝑫 = a fragment-directive",
                                    "================================================",
                                    ""));

    static void writePropHeader(
            PrintWriter out,
            SimpleFormatter simpleFormatter,
            String filename,
            String propertyName,
            String missingValue) {
        out.println(
                simpleFormatter.format(
                        filename, dt.format(now), dty.format(now), propertyName, missingValue));
    }

    static void writeTestHeader(
            PrintWriter out, SimpleFormatter simpleFormatter, String filename, String testName) {
        out.println(simpleFormatter.format(filename, dt.format(now), dty.format(now), testName));
    }

    /** Generate property data for the UTS */
    static void generatePropertyData() {
        System.out.println("TLDs=\t" + Joiner.on(' ').join(LinkUtilities.TLDS));

        BagFormatter bf = new BagFormatter(LinkUtilities.IUP).setLineSeparator("\n");
        bf.setShowLiteral(FIX_ODD);

        // LinkTerm.txt

        bf.setValueSource(LinkTermination.PROPERTY);
        bf.setLabelSource(LinkUtilities.IUP.getProperty(UcdProperty.Age));

        try (final var out = new DiffingPrintWriter(LinkUtilities.DATA_DIR_DEV, "LinkTerm.txt"); ) {
            writePropHeader(out.tempPrintWriter, HEADER_PROP_TERM, "LinkTerm", "Link_Term", "Hard");
            for (LinkTermination propValue : LinkTermination.NON_MISSING) {
                bf.showSetNames(out.tempPrintWriter, propValue.base);
                out.println("");
                out.flush();
                System.out.println(propValue + "=\t" + propValue.base.toPattern(false));
            }
        }

        // LinkEmail.txt
        bf.setValueSource(UnicodeLabel.NULL);
        try (final var out =
                new DiffingPrintWriter(LinkUtilities.DATA_DIR_DEV, "LinkEmail.txt"); ) {
            writePropHeader(
                    out.tempPrintWriter,
                    HEADER_PROP_BINARY,
                    "LinkEmail",
                    "Link_Email",
                    UcdPropertyValues.Binary.No.toString());
            UnicodeSet linkEmailSet = LinkUtilities.LinkEmail.getSet(UcdPropertyValues.Binary.Yes);
            bf.showSetNames(out.tempPrintWriter, linkEmailSet);
            System.out.println("LinkEmail=\t" + linkEmailSet.toPattern(false));
        }

        // LinkBracket.txt
        bf.setValueSource(LinkUtilities.getLinkBracket());
        bf.setHexValue(true);
        bf.setShowDehexedValue(true);
        try (final var out =
                new DiffingPrintWriter(LinkUtilities.DATA_DIR_DEV, "LinkBracket.txt"); ) {
            writePropHeader(
                    out.tempPrintWriter,
                    HEADER_PROP_STRING,
                    "LinkBracket",
                    "Link_Bracket",
                    "<none>");
            bf.showSetNames(out.tempPrintWriter, LinkTermination.Close.base);
        }
    }

    /**
     * The format of the test file sources are: source<TAB>expected OR just source If there is an
     * expected value, then it is checked against what is generated.
     */
    static void generateDetectionTestData() {

        OutputInt errorCount = new OutputInt();

        try (final var out =
                new DiffingPrintWriter(LinkUtilities.DATA_DIR_DEV, "LinkDetectionTest.txt"); ) {
            writeTestHeader(
                    out.tempPrintWriter,
                    HEADER_DETECT_TEST,
                    "LinkDetectionTest",
                    "LinkDetectionTest");

            out.println("\n# Misc. test cases\n");

            Files.lines(Path.of(LinkUtilities.RESOURCE_DIR, "LinkDetectionTestSource.txt"))
                    .forEach(
                            line -> {
                                if (line.startsWith("#") || line.isBlank()) {
                                    out.println(line);
                                    return;
                                }
                                List<String> parts = SPLIT_TABS.splitToList(line);

                                String base = parts.get(0);
                                String actual = LinkUtilities.addBracesAroundDetectedLink(base);
                                String expected = parts.size() < 2 ? null : parts.get(1);

                                if (expected != null && !actual.equals(expected)) {
                                    System.out.println(
                                            "* mismatch "
                                                    + base
                                                    + "\nexpected:\t"
                                                    + expected
                                                    + "\nactual:  \t"
                                                    + actual);
                                    // for debugging
                                    LinkUtilities.addBracesAroundDetectedLink(base);
                                    return;
                                }

                                out.println((ADDTEST ? (line + "\t") : "") + actual);
                            });

            out.println("\n# Test cases contributed by ICANN\n");

            Files.lines(Path.of(LinkUtilities.RESOURCE_DIR, "LinkDetectionTestSourceICANN.txt"))
                    .forEach(
                            line -> {
                                if (line.startsWith("#") || line.isBlank()) {
                                    return;
                                }
                                List<String> parts = SPLIT_TABS.splitToList(line);
                                if (parts.size() != 2) {
                                    System.out.println("* Malformed? " + line);
                                    ++errorCount.value;
                                    return;
                                }
                                String base = parts.get(0);
                                String expected = parts.get(1);
                                String actual = LinkUtilities.addBracesAroundDetectedLink(base);
                                if (!actual.equals(expected)) {
                                    System.out.println(
                                            "* mismatch "
                                                    + base
                                                    + "\nexpected:\t"
                                                    + expected
                                                    + "\nactual:  \t"
                                                    + actual);
                                    // for debugging
                                    LinkUtilities.addBracesAroundDetectedLink(base);
                                    return;
                                }
                                out.println(actual);
                            });

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (errorCount.value != 0) {
            throw new IllegalArgumentException("Failures in writing test file: " + errorCount);
        }
    }

    static void generateFormattingTestData() {

        OutputInt errorCount = new OutputInt();

        try (final var out =
                new DiffingPrintWriter(LinkUtilities.DATA_DIR_DEV, "LinkFormattingTest.txt"); ) {
            writeTestHeader(
                    out.tempPrintWriter,
                    HEADER_FORMAT_TEST,
                    "LinkFormattingTest",
                    "LinkFormattingTest");

            out.tempPrintWriter.println("\n# Selected test cases\n");

            List<String> comments = new ArrayList<>();
            Output<Integer> lineCount = new Output<>(0);

            Files.lines(Path.of(LinkUtilities.RESOURCE_DIR, "LinkFormattingTestSource.txt"))
                    .forEach(
                            line -> {
                                ++lineCount.value;
                                if (line.isBlank()) {
                                    return;
                                }
                                if (line.startsWith("#")) {
                                    out.println(line);
                                    comments.add(line);
                                    return;
                                }

                                // process old format
                                // TODO convert the LinkFormattingTestSource.txt to the same format
                                // as the output

                                line = percentEscapeBackslash(line);

                                List<String> parts = SPLIT_SEMI.splitToList(line);
                                if (parts.size() < 5 || parts.size() > 6) {
                                    System.out.println("* Malformed? " + line);
                                    ++errorCount.value;
                                    return;
                                }

                                StringBuilder oldValue = new StringBuilder();
                                Part.PROTOCOL.appendPart(oldValue, parts.get(0));
                                Part.HOST.appendPart(oldValue, parts.get(1));
                                Part.PATH.appendPart(oldValue, parts.get(2));
                                Part.QUERY.appendPart(oldValue, parts.get(3));
                                Part.FRAGMENT.appendPart(oldValue, parts.get(4));
                                String fullSourcePath = oldValue.toString();

                                UrlInternals internals = UrlInternals.from(fullSourcePath);

                                String actual = internals.minimalEscape(false, null);

                                String expected = parts.size() < 6 ? null : parts.get(5);
                                if (expected != null && !actual.equals(expected)) {
                                    String title =
                                            "("
                                                    + lineCount
                                                    + ") Mismatch from: "
                                                    + Joiner.on('\n').join(comments);
                                    System.out.println(
                                            Joiner.on('\n')
                                                    .join(
                                                            title,
                                                            line,
                                                            internals,
                                                            fullSourcePath,
                                                            "expected:\t" + expected,
                                                            "actual:  \t" + actual));
                                    ++errorCount.value;
                                    // for debugging
                                    UrlInternals.from(fullSourcePath);
                                    internals.minimalEscape(false, null);
                                    comments.clear();
                                    return;
                                }
                                outputTestCase(out.tempPrintWriter, comments, internals, actual);
                            });

            out.println("\n# Wikipedia test cases\n");

            final UnicodeSet charactersSeen = new UnicodeSet(LinkTermination.Include.getBase());
            final UnicodeSet charactersSeenAtEnd =
                    new UnicodeSet(LinkTermination.Include.getBase());

            Files.lines(Path.of(LinkUtilities.RESOURCE_DIR, "testUrls.txt"))
                    .forEach(
                            line -> {
                                if (line.startsWith("#") || line.isBlank()) {
                                    return;
                                }
                                line = line.trim();
                                int wikiStart = line.indexOf("/wiki/");
                                if (wikiStart < 0) {
                                    return;
                                }
                                int lastCodePoint = line.codePointBefore(line.length());
                                String rest =
                                        line.substring(
                                                wikiStart + 6,
                                                line.length() - Character.charCount(lastCodePoint));
                                // skip if we don't see any new characters
                                int size = charactersSeen.size();
                                charactersSeen.addAll(rest);

                                int endSize = charactersSeenAtEnd.size();
                                charactersSeenAtEnd.add(lastCodePoint);

                                if (charactersSeen.size() == size
                                        && charactersSeenAtEnd.size() == endSize) {
                                    return;
                                }
                                // Divide into parts
                                UrlInternals internals = UrlInternals.from(line);

                                String actual = internals.minimalEscape(true, null);

                                outputTestCase(out.tempPrintWriter, comments, internals, actual);
                            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (errorCount.value != 0) {
            throw new IllegalArgumentException("Failures in writing test file: " + errorCount);
        }
    }

    private static void outputTestCase(
            final PrintWriter out, List<String> comments, UrlInternals internals, String actual) {
        String fullEscape = internals.fullEscape();
        out.println("# " + internals);
        out.println(fullEscape);
        out.println(actual);
        out.println();
        comments.clear();
    }

    private static String percentEscapeBackslash(String line) {
        StringBuilder result = new StringBuilder();
        boolean skipFirst = !line.startsWith("\\");
        for (String part : Splitter.on('\\').split(line)) {
            if (skipFirst) {
                result.append(part);
                skipFirst = false;
                continue;
            }
            int toEscape = part.codePointAt(0);
            LinkUtilities.appendPercentEscaped(result, toEscape, null);
            result.append(part.substring(Character.charCount(toEscape), part.length()));
        }
        return result.toString();
    }

    /**
     * Use the query https://w.wiki/CKpG to create a file. Put it in RESOURCE_DIR as
     * wikipedia1000raw.tsv, then run this class. It generates two new files, <br>
     * • testUrls.txt <br>
     * • testUrlsStats.txt
     *
     * <p>LinkificationTest.txt SerializationTest.txt
     *
     * @throws IOException
     */
    static void processTestFile() throws IOException {
        MutableLong skippedAscii = new MutableLong();
        MutableLong included = new MutableLong();
        final UnicodeSet skipIfOnly = new UnicodeSet("\\p{ascii}").freeze(); // [a-zA-Z0-9:/?#]
        final Path sourcePath =
                Path.of(LinkUtilities.RESOURCE_DIR + "wikipedia1000raw.tsv").toRealPath();
        final String toRemove = "http://www.wikidata.org/entity/Q";
        final Multimap<Long, String> output = TreeMultimap.create();
        final Counter<Integer> escaped = new Counter<>();
        final Counter<String> hostCounter = new Counter<>();
        MutableLong lines = new MutableLong();
        Consumer<? super String> action =
                line -> {
                    if (line.startsWith("item")) {
                        return;
                    }
                    lines.value++;
                    int fieldNumber = 0;
                    long qid = 0;
                    String url = null;
                    for (String item : Splitter.on('\t').split(line)) {
                        ++fieldNumber;
                        switch (fieldNumber) {
                            case 1:
                                if (!item.startsWith(toRemove)) {
                                    throw new IllegalArgumentException(line);
                                }
                                qid = Long.parseLong(item.substring(toRemove.length()));
                                break;
                            case 2:
                                UrlInternals parts = UrlInternals.from(item);
                                String host = parts.get(Part.HOST).get(0).get(0);
                                hostCounter.add(host, 1);
                                url = parts.minimalEscape(true, escaped);
                                break;
                        }
                    }
                    if (skipIfOnly.containsAll(url)) {
                        skippedAscii.value++;
                        return;
                    } else if (LinkUtilities.DEBUG && url.startsWith("https://en.wiki")) {
                        System.out.println("en non-ascii:\t" + url);
                    }
                    included.value++;
                    output.put(qid, url);
                    escaped.clear();
                };
        Files.lines(sourcePath).forEach(action);
        MutableLong lastQid = new MutableLong();
        System.out.println("Lines read: " + lines + "\tLines generated: " + output.size());
        lines.value = 0;
        try (PrintWriter output2 =
                FileUtilities.openUTF8Writer(LinkUtilities.RESOURCE_DIR, "testUrls.txt")) {
            output2.println("# Test file using the the 1000 pages every wiki should have.");
            output2.println("# Included urls: " + included);
            output2.println("# Skipping all-ASCII urls: " + skippedAscii);
            NumberFormat nf = NumberFormat.getPercentInstance();
            output.entries().stream()
                    .forEach(
                            x -> {
                                long qid = x.getKey();
                                if (qid != lastQid.value) {
                                    output2.println(
                                            LinkUtilities.JOIN_TAB.join(
                                                    "# Q" + qid,
                                                    nf.format(
                                                            lines.value / (double) output.size())));
                                    lastQid.value = qid;
                                }
                                lines.value++;
                                output2.println(x.getValue());
                            });
            output2.println("# EOF");
        }
        try (PrintWriter output2 =
                FileUtilities.openUTF8Writer(LinkUtilities.RESOURCE_DIR, "testUrlsStats.txt")) {
            hostCounter.getKeysetSortedByKey().stream()
                    .forEach(x -> output2.println(hostCounter.get(x) + "\t" + x));
        }
    }
}
