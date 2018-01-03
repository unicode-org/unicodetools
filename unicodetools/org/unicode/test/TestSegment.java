package org.unicode.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.BNF;
import org.unicode.cldr.util.Quoter;
import org.unicode.cldr.util.props.UnicodePropertySymbolTable;
import org.unicode.jsp.UnicodeRegex;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.text.utility.Utility;

import com.ibm.icu.impl.locale.XCldrStub.Splitter;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.XSymbolTable;

public class TestSegment {
    private final String rules;
    private final Pattern bnf;

    TestSegment(String testBnf) {
        StringBuilder generationRules = new StringBuilder();
        for (String line : FileUtilities.in(TestSegment.class, testBnf)) {
            if (line.startsWith("#")) {
                continue;
            }
            line = line.replace(" ","").replace("\n","").replace("\t","");
            generationRules.append(line);
            if (line.endsWith(";")) {
                generationRules.append('\n');
            }
        }
        rules = new UnicodeRegex().compileBnf(generationRules.toString());
        String fixed = UnicodeRegex.fix(rules);

        bnf = Pattern.compile(fixed);
    }

    private static Splitter SPACE = Splitter.on(' ').trimResults();

    static class TestCase {
        //÷ 0020 ÷ 0020 ÷   #  ÷ [0.2] SPACE (Other) ÷ [999.0] SPACE (Other) ÷ [0.3]

        final StringBuffer testLine = new StringBuffer();
        final List<Integer> breakPoints = new ArrayList<>();
        String comment;

        public void clear() {
            testLine.setLength(0);
            breakPoints.clear();
        }

        void set(String line) {
            int hash = line.indexOf('#');
            if (hash < 0) {
                comment = "";
            } else {
                comment = line.substring(hash);
                line = line.substring(0, hash);
            }
            clear();
            for (String piece : SPACE.split(line)) {
                switch (piece) {
                case "÷":
                    breakPoints.add(testLine.length());
                    break;
                case "":
                case "×":
                    break;
                default:
                    testLine.appendCodePoint(Integer.parseInt(piece,16));
                    break;
                }
            }
        }

    }

    public static List<Integer> getbreaks(Pattern bnf, List<Integer> results, CharSequence charSequence) {
        results.clear();
        results.add(0);
        int length = charSequence.length();
        if (length == 0) {
            return results;
        }
        Matcher matcher = bnf.matcher(charSequence);
        int end = 0;
        while (true) {
            if (matcher.lookingAt()) { // must match at least one character
                end = matcher.end();
            } else {
                end = Character.offsetByCodePoints(charSequence, end, 1);
            } 
            results.add(end);
            if (end == length) {
                break;
            }
            matcher.region(end, length);
        }
        return results;
    }

    private void test(String title, String dir, String filename) {
        Iterable<String> items = FileUtilities.in(dir, filename + "Test.txt");

        test(title, items);
    }

    private void test(String title, Iterable<String> items) {
        System.out.println(title + "\t" + rules);
        TestCase testCase = new TestCase();
        List<Integer> results = new ArrayList<>();
        int count = 0;
        for (String line : items) {
            ++count;
            testCase.set(line);
            if (testCase.testLine.length() == 0) {
                continue;
            }
            List<Integer> list = getbreaks(bnf, results, testCase.testLine);
            if (!testCase.breakPoints.equals(list)) {
                System.out.println(
                        title + "\t" + count
                        + ")\t" + testCase.testLine
                        + "\nhex:\t" + Utility.hex(testCase.testLine)
                        + "\ntest:\t" + testCase.breakPoints
                        + "\nebnf:\t" + list
                        );
                // for debugging
                list = getbreaks(bnf, results, testCase.testLine);
            }
        }
    }

    public static void main(String[] args) {
        IndexUnicodeProperties iup = IndexUnicodeProperties.make();
        final XSymbolTable toolUPS = new UnicodePropertySymbolTable(iup);
        UnicodeSet.setDefaultXSymbolTable(toolUPS);

        TestSegment gc = new TestSegment("SegmentBnf" + "Ccs" + ".txt");
        gc.test("Ccs", Arrays.asList("÷ 0020 ÷ 0020 ÷", 
                "÷ 0020 0308 ÷", 
                "÷ 0020 0308 ÷ 0061 ÷", 
                "÷ 0020 0301 0301 ÷ 0061 ÷", 
                "÷ 0061 ÷ 0062 ÷"));

        TestSegment gc2 = new TestSegment("SegmentBnf" + "GraphemeBreakSimple" + ".txt");
        gc2.test("GBS", Arrays.asList("÷ 0020 ÷ 0020 ÷", 
                "÷ 0020 0308 ÷", 
                "÷ 0020 0308 ÷ 0061 ÷", 
                "÷ 0020 0301 0301 ÷ 0061 ÷", 
                "÷ 0061 ÷ 0062 ÷",
                "÷ 000D 000A ÷"));

        TestSegment gc3 = new TestSegment("SegmentBnf" + "GraphemeBreak" + ".txt");
        gc3.test("Gb", "/Users/markdavis/Documents/workspace/unicode-draft/Public/UCD/auxiliary/","GraphemeBreak");
    }
}
