package org.unicode.tools;

import com.google.common.base.Splitter;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UnicodeSet;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NavigableMap;
import java.util.function.Consumer;
import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Rational.MutableLong;
import org.unicode.utilities.UrlUtilities;
import org.unicode.utilities.UrlUtilities.Part;

/**
 * Use the query https://w.wiki/CKpG to create a file. Put it in RESOURCE_DIR as
 * wikipedia1000raw.tsv, then run this class. It generates two new files, testUrls.txt and
 * testUrlsStats.txt
 *
 * @throws IOException
 */
class GenerateUrlTestData {

    public static void main(String[] args) throws IOException {
        processTestFile();
    }

    static void processTestFile() throws IOException {
        MutableLong skippedAscii = new MutableLong();
        MutableLong included = new MutableLong();
        final UnicodeSet skipIfOnly = new UnicodeSet("\\p{ascii}").freeze(); // [a-zA-Z0-9:/?#]
        final Path sourcePath =
                Path.of(UrlUtilities.RESOURCE_DIR + "wikipedia1000raw.tsv").toRealPath();
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
                                url = UrlUtilities.minimalEscape(parts, true, escaped);
                                break;
                        }
                    }
                    if (skipIfOnly.containsAll(url)) {
                        skippedAscii.value++;
                        return;
                    } else if (UrlUtilities.DEBUG && url.startsWith("https://en.wiki")) {
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
                FileUtilities.openUTF8Writer(UrlUtilities.RESOURCE_DIR, "testUrls.txt")) {
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
                                            UrlUtilities.JOIN_TAB.join(
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
                FileUtilities.openUTF8Writer(UrlUtilities.RESOURCE_DIR, "testUrlsStats.txt")) {
            hostCounter.getKeysetSortedByKey().stream()
                    .forEach(x -> output2.println(hostCounter.get(x) + "\t" + x));
        }
    }
}
