package org.unicode.unittest;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.ToolUnicodePropertySource;
import org.unicode.text.utility.Utility;
import org.unicode.tools.Segmenter;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.CanonicalIterator;

public class TestSegmenter extends TestFmwk{
    public static void main(String[] args) {
	//System.setProperty("CLDR", "true");
	IndexUnicodeProperties iup = IndexUnicodeProperties.make();
	//        UnicodeSet emoji = iup
	//                .loadEnum(UcdProperty.Emoji, Binary.class)
	//                .getSet(Binary.Yes);
	//        System.out.println(emoji.size() + "\t" + emoji.toPattern(false));
	//
	//        UnicodeSet extended_pictographic = iup
	//                .loadEnum(UcdProperty.Extended_Pictographic, Binary.class)
	//                .getSet(Binary.Yes);
	//        System.out.println(extended_pictographic.size() + "\t" + extended_pictographic.toPattern(false));

	new TestSegmenter().run(args);
    }

    Segmenter gcb = Segmenter.make(ToolUnicodePropertySource.make(Default.ucdVersion()), "GraphemeClusterBreak").make();

    public void Test11() {
	String[][] tests = {
		{"🛑\u200d🛑", "|---"},
	};
	for (String[] test : tests) {
	    String s = test[0];
	    String expected = test[1];
	    int expectedIndex = 0;
	    for (int i = 0; i < s.length(); ++i) {
		boolean expectedBreak = i >= s.length() || Character.isLowSurrogate(s.charAt(i)) ? false : expected.charAt(expectedIndex++) == '|';
		boolean actualBreak = gcb.breaksAt(s, i);
		String title = s.substring(0, i) + "|" + s.substring(i);
		if (!assertEquals(title, expectedBreak, actualBreak)) {
		    gcb.breaksAt(s, i);
		}
	    }
	}
    }

    public void TestIndic() {
	System.out.println();

	CanonicalIterator it = new CanonicalIterator("");
	Set<String> seen = new HashSet<>();
	for (File file : new File(
		CLDRPaths.TEST_DATA + "segmentation/graphemeCluster")
		.listFiles()) {
		int lineCount = 0;
	    for (String line : FileUtilities.in(file)) {
		lineCount++;
		int subcount = 0;
		line = line.trim();
		if (line.startsWith("\uFEFF")) {
		    line = line.substring(1);
		}
		if (line.isEmpty() || line.startsWith("#")) {
		    continue;
		}
		List<String> parts = SEMI.splitToList(line);
		try {
		    //String sourceId = parts.get(0);
		    String source = parts.get(0);
		    String expected = parts.get(1);
		    StringBuilder cleaned = new StringBuilder();
		    extractBreakPoints(expected, cleaned);
		    String cleanedStr = cleaned.toString();
		    if (!source.equals(cleanedStr)) {
			errln("Expected value doesn't have same characters as source: "
				+ file.getName()
				+ "\t" + line + " // " + source + " ≠ " + cleanedStr
				+ "\t" + Utility.hex(source) + " ≠ " + Utility.hex(cleanedStr)
				);
			continue;
		    }

		    showBreakLines(file, lineCount, subcount, expected);
		    //            seen.clear();
		    //            seen.add(expected);
		    //            it.setSource(expected);
		    //            for (String line2 = it.next(); line2 != null; line2 = it.next()) {
		    //                line2 = line2.replace("\u037E", ";");
		    //                if (seen.contains(line)) {
		    //                    continue;
		    //                }
		    //                System.out.println(Utility.hex(line));
		    //                System.out.println(Utility.hex(line2));
		    //                showBreakLines(lineCount, subcount++, line2);
		    //                seen.add(line);
		    //            }
		} catch (Exception e) {
		    errln("Bad format for line: "
			    + file.getName()
			    + "\t" + line);
		}
	    } 
	}
    }

    static final Splitter SEMI = Splitter.on(';').trimResults();

    private void showBreakLines(File file, final int lineCount, final int subcount, final String expected) {
	try {
	    StringBuilder cleaned = new StringBuilder();
	    Set<Integer> expectedBreaks = extractBreakPoints(expected, cleaned);
	    String source = cleaned.toString();
	    Set<Integer> actualBreaks = extractBreakPoints(gcb, source);

	    if (!expectedBreaks.equals(actualBreaks)) {
		String actualForm = displayForm(source, actualBreaks);
		String expectedForm = displayForm(source, expectedBreaks);

		errln(file.getName() 
			+ "\t" + lineCount + ":" + subcount 
			+")\t" + source + "; expected: " + expectedForm + "; actual: " + actualForm);
	    }
	} catch (Exception e) {
	    throw new IllegalArgumentException(lineCount + ":" + subcount + ") " + expected,e);
	}
    }

    private String displayForm(String source, Set<Integer> breaks) {
	if (source.isEmpty()) {
	    return source;
	}
	StringBuilder result = new StringBuilder();
	result.append(source.charAt(0));
	for (int i = 1; i < source.length(); ++i) {
	    if (breaks.contains(i)) {
		result.append('➗');
	    }
	    result.append(source.charAt(i));
	}
	return result.toString();
    }

    private Set<Integer> extractBreakPoints(Segmenter gcb2, String source) {
	Set<Integer> result = new LinkedHashSet<>();
	for (int i = 0; i <= source.length(); ++i) {
	    boolean actualBreak = gcb.breaksAt(source, i);
	    if (actualBreak) {
		result.add(i);
	    }
	}
	return ImmutableSet.copyOf(result);
    }

    /**
     * extract break points. Always have break at start and end, even if not listed
     * @param expected
     * @return
     */
    private Set<Integer> extractBreakPoints(String expected, StringBuilder cleaned) {
	Set<Integer> result = new LinkedHashSet<>();
	result.add(0);
	int offset = 0;
	for (int i = 0; i < expected.length(); ++i) {
	    char cp = expected.charAt(i);
	    switch (cp) {
	    case '÷': case '➗':
		result.add(offset);
		break;
	    case '×': case '✖':
		result.add(offset);
		break;
	    default:
		if (cleaned != null) {
		    cleaned.append(cp);
		}
		offset++;
	    }
	}
	result.add(offset);
	return ImmutableSet.copyOf(result);
    }
}
