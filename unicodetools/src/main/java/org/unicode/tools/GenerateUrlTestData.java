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
import java.util.Date;
import java.util.NavigableMap;
import java.util.function.Consumer;
import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Rational.MutableLong;
import org.unicode.cldr.util.props.UnicodeLabel;
import org.unicode.props.BagFormatter;
import org.unicode.props.UcdProperty;
import org.unicode.text.UCD.UCDProperty;
import org.unicode.text.utility.Settings;
import org.unicode.utilities.UrlUtilities;
import org.unicode.utilities.UrlUtilities.LinkTermination;
import org.unicode.utilities.UrlUtilities.Part;

/**
 * Generate UTS #58 property and test files
 *
 * @throws IOException
 */
class GenerateUrlTestData {
	
    public static void main(String[] args) throws IOException {
        // processTestFile();
        generateData();
    }
    
    static final long now = Instant.now().toEpochMilli();
    static final DateFormat dt = new SimpleDateFormat("y-MM-dd HH:mm:ss 'GMT'");
    static final DateFormat dty = new SimpleDateFormat("y");

    static final SimpleFormatter HEADER = SimpleFormatter.compile(
    		"# {0}.txt\n"
    		+ "# Date: {1} \n"
    		+ "# © {2} Unicode®, Inc.\n"
    		+ "# Unicode and the Unicode Logo are registered trademarks of Unicode, Inc. in the U.S. and other countries.\n"
    		+ "# For terms of use and license, see https://www.unicode.org/terms_of_use.html\n"
    		+ "#\n"
    		+ "# Note that characters may change property values between releases.\n"
    		+ "# For more information, see: https://www.unicode.org/reports/tr58/\n"
    		+ "#\n"
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
    
    static void writeHeader(PrintWriter out, String filename, String propertyName, String missingValue) {
    	 out.println(HEADER.format(filename, dt.format(now), dty.format(now), propertyName, missingValue));
    }
    
/** 
 * Generate property data for the UTS
 */
    static void generateData() {
    	
        BagFormatter bf = new BagFormatter(UrlUtilities.IUP).setLineSeparator("\n");

    	// LinkTermination.txt
        
        bf.setValueSource(LinkTermination.PROPERTY);
        bf.setLabelSource(UrlUtilities.IUP.getProperty(UcdProperty.Age));
        
        try (final PrintWriter out =
        		FileUtilities.openUTF8Writer(UrlUtilities.DATA_DIR, "LinkTermination.txt");
        		) {
    		writeHeader(out, "LinkTermination", "LinkTermination", "Hard");
        	for (LinkTermination propValue : LinkTermination.NON_MISSING) {
	            bf.showSetNames(out, propValue.base);
	            out.println("");
	            out.flush();
        	}

        } catch (IOException e) {
			throw new UncheckedIOException(e);
		}

    	// LinkPairedOpener.txt
        bf.setValueSource(UrlUtilities.LINK_PAIRED_OPENER);
        try (final PrintWriter out =
        		FileUtilities.openUTF8Writer(UrlUtilities.DATA_DIR, "LinkPairedOpener.txt");
        		) {
    		writeHeader(out, "LinkPairedOpener", "LinkPairedOpener", "undefined");
            bf.showSetNames(out, LinkTermination.Close.base);
        } catch (IOException e) {
			throw new UncheckedIOException(e);
		}
    }

    /**
     * Use the query https://w.wiki/CKpG to create a file. Put it in RESOURCE_DIR as
     * wikipedia1000raw.tsv, then run this class. It generates two new files, testUrls.txt and
     * testUrlsStats.txt
     * 
     * LinkificationTest.txt
     * SerializationTest.txt
     *
     * @throws IOException
     */

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
