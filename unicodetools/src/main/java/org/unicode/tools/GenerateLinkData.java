package org.unicode.tools;

import com.google.common.base.Splitter;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.SimpleFormatter;
import com.ibm.icu.text.UnicodeSet;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.NavigableMap;
import java.util.function.Consumer;
import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Rational.MutableLong;
import org.unicode.props.BagFormatter;
import org.unicode.props.UcdProperty;
import org.unicode.utilities.LinkUtilities;
import org.unicode.utilities.LinkUtilities.LinkScanner;
import org.unicode.utilities.LinkUtilities.LinkTermination;
import org.unicode.utilities.LinkUtilities.Part;

/**
 * Generate UTS #58 property and test files
 *
 * @throws IOException
 */
class GenerateLinkData {

    private static final Splitter SPLIT_TABS = Splitter.on('\t').omitEmptyStrings();
    private static final String HEADER_BASE =
            "# {0}.txt\n"
                    + "# Date: {1} \n"
                    + "# © {2} Unicode®, Inc.\n"
                    + "# Unicode and the Unicode Logo are registered trademarks of Unicode, Inc. in the U.S. and other countries.\n"
                    + "# For terms of use and license, see https://www.unicode.org/terms_of_use.html\n"
                    + "#\n"
                    + "# The usage and stability of these values is covered in https://www.unicode.org/reports/tr58/\n"
                    + "#\n";

    public static void main(String[] args) throws IOException {
        generatePropertyData();
        generateTestData();
    }

    static final long now = Instant.now().toEpochMilli();
    static final DateFormat dt = new SimpleDateFormat("y-MM-dd HH:mm:ss 'GMT'", Locale.ENGLISH);
    static final DateFormat dty = new SimpleDateFormat("y", Locale.ENGLISH);

    static final SimpleFormatter HEADER_PROP =
            SimpleFormatter.compile(
                    HEADER_BASE
                            + "\n"
                            + "# ================================================\n"
                            + "\n"
                            + "# Property:	{3}\n"
                            + "\n"
                            + "#  All code points not explicitly listed for {3}\n"
                            + "#  have the value {4}.\n"
                            + "\n"
                            + "# @missing: 0000..10FFFF; {4}\n"
                            + "\n"
                            + "# ================================================\n");

    static void writePropHeader(
            PrintWriter out, String filename, String propertyName, String missingValue) {
        out.println(
                HEADER_PROP.format(
                        filename, dt.format(now), dty.format(now), propertyName, missingValue));
    }

    static final SimpleFormatter HEADER_TEST =
            SimpleFormatter.compile(
                    HEADER_BASE
                            + "# Format: each line contains zero or more marked links, such as ⸠abc.com⸡\n"
                            + "# Operation: For each line.\n"
                            + "# • Create a copy of the line, with the characters ⸠ and ⸡ removed.\n"
                            + "# • Run link detection on the line, inserting ⸠ and ⸡ around each detected link.\n"
                            + "# • Report a failure if the result is not identical to the original line.\n"
                            + "# Empty lines, and lines starting with # are ignored.\n"
                            + "# Otherwise # is treated like any other character.\n"
                            + "# ================================================\n");

    static void writeTestHeader(PrintWriter out, String filename, String testName) {
        out.println(HEADER_TEST.format(filename, dt.format(now), dty.format(now), testName));
    }

    /** Generate property data for the UTS */
    static void generatePropertyData() {

        BagFormatter bf = new BagFormatter(LinkUtilities.IUP).setLineSeparator("\n");

        // LinkTermination.txt

        bf.setValueSource(LinkTermination.PROPERTY);
        bf.setLabelSource(LinkUtilities.IUP.getProperty(UcdProperty.Age));

        try (final PrintWriter out =
                FileUtilities.openUTF8Writer(LinkUtilities.DATA_DIR, "LinkTermination.txt"); ) {
            writePropHeader(out, "LinkTermination", "LinkTermination", "Hard");
            for (LinkTermination propValue : LinkTermination.NON_MISSING) {
                bf.showSetNames(out, propValue.base);
                out.println("");
                out.flush();
            }

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        // LinkPairedOpener.txt
        bf.setValueSource(LinkUtilities.getLinkPairedOpener());
        try (final PrintWriter out =
                FileUtilities.openUTF8Writer(LinkUtilities.DATA_DIR, "LinkPairedOpener.txt"); ) {
            writePropHeader(out, "LinkPairedOpener", "LinkPairedOpener", "undefined");
            bf.showSetNames(out, LinkTermination.Close.base);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void generateTestData() {

        BagFormatter bf = new BagFormatter(LinkUtilities.IUP).setLineSeparator("\n");

        // LinkTermination.txt

        bf.setValueSource(LinkTermination.PROPERTY);
        bf.setLabelSource(LinkUtilities.IUP.getProperty(UcdProperty.Age));

        // "/unicodetools/src/main/resources/org/unicode/tools/test_links_lt.txt"

        try (final PrintWriter out =
                FileUtilities.openUTF8Writer(LinkUtilities.DATA_DIR, "LinkDetectionTest.txt"); ) {
            writeTestHeader(out, "LinkDetectionTest", "LinkDetectionTest");

            out.println("# Test cases contributed by ICANN\n");

            Files.lines(Path.of(LinkUtilities.RESOURCE_DIR, "test_links_lt.txt"))
                    .forEach(
                            line -> {
                                if (line.startsWith("#") || line.isBlank()) {
                                    return;
                                }
                                List<String> parts = SPLIT_TABS.splitToList(line);
                                if (parts.size() != 2) {
                                    System.out.println("* Malformed? " + line);
                                    return;
                                }
                                String base = parts.get(0);
                                String expected = parts.get(1);
                                String actual = addBraces(base);
                                if (!actual.equals(expected)) {
                                    System.out.println(
                                            "* mismatch "
                                                    + base
                                                    + "\nexpected:\t"
                                                    + expected
                                                    + "\nactual:  \t"
                                                    + actual);
                                    return;
                                }
                                out.println(actual);
                            });

            out.println("\n# Other test cases\n");

            Files.lines(Path.of(LinkUtilities.RESOURCE_DIR, "test_links_plain.txt"))
                    .forEach(
                            line -> {
                                if (line.startsWith("#") || line.isBlank()) {
                                    out.println(line);
                                    return;
                                }
                                String actual = addBraces(line);
                                out.println(actual);
                            });

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        //        // LinkPairedOpener.txt
        //        bf.setValueSource(LinkUtilities.LINK_PAIRED_OPENER);
        //        try (final PrintWriter out =
        //                FileUtilities.openUTF8Writer(LinkUtilities.DATA_DIR,
        // "LinkFormattingTest.txt"); ) {
        //            writeHeader(out, "LinkPairedOpener", "LinkPairedOpener", "undefined");
        //            bf.showSetNames(out, LinkTermination.Close.base);
        //        } catch (IOException e) {
        //            throw new UncheckedIOException(e);
        //        }
    }

    private static String addBraces(String base) {
        LinkScanner ls = new LinkScanner(base, 0, base.length());
        StringBuilder result = new StringBuilder();

        int lastEnd = 0;
        while (ls.next()) {
            int start = ls.getLinkStart();
            int end = ls.getLinkEnd();

            result.append(base.substring(lastEnd, start))
                    .append("⸠")
                    .append(base.substring(start, end))
                    .append("⸡");
            lastEnd = end;
        }

        result.append(base.substring(lastEnd));

        return result.toString();
    }

    /**
     * Use the query https://w.wiki/CKpG to create a file. Put it in RESOURCE_DIR as
     * wikipedia1000raw.tsv, then run this class. It generates two new files, testUrls.txt and
     * testUrlsStats.txt
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
                                NavigableMap<Part, String> parts =
                                        Part.getParts(item, false); // splits and unescapes
                                hostCounter.add(parts.get(Part.HOST), 1);
                                url = LinkUtilities.minimalEscape(parts, true, escaped);
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
