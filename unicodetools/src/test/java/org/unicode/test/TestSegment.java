package org.unicode.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.BNF;
import org.unicode.cldr.util.MapComparator;
import org.unicode.cldr.util.props.UnicodePropertySymbolTable;
import org.unicode.jsp.UnicodeRegex;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.props.UcdPropertyValues.Binary;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.ToolUnicodePropertySource;
import org.unicode.text.utility.Utility;
import org.unicode.tools.Segmenter;
import org.unicode.tools.Segmenter.Builder;
import org.unicode.tools.Segmenter.Rule;

import com.google.common.base.Splitter;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeMap.Composer;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.XSymbolTable;

public class TestSegment {

    static final IndexUnicodeProperties IUP = IndexUnicodeProperties.make();
    static final UnicodeMap<Age_Values> AGE = IUP.loadEnum(UcdProperty.Age);
    static final UnicodeMap<General_Category_Values> GC = IUP.loadEnum(UcdProperty.General_Category);
    static final UnicodeSet WHITESPACE = IUP.loadEnum(UcdProperty.White_Space, Binary.class).getSet(Binary.Yes);

    private final String rules;
    private final Pattern bnf;

    TestSegment(String testBnf) {
        BNF foo;


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
                System.out.println("FAIL\t"
                        + title + "\t" + count
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

    /**
     * The goal is to partition Unicode characters into sets that behave the same for that segmenter, then pick an exemplar from each.
     * Ideally, these characters are the most common, and have the least "churn" over versions.
     * @return
     */
    static UnicodeMap<Enum> pickBestExemplars(UcdProperty firstProp, Map<String, UnicodeSet> extras, UnicodeMap<String> makesDifference) {
        IndexUnicodeProperties iup = IUP;

        UnicodeMap<String> mainMap = new UnicodeMap<>(iup.load(firstProp));

        UnicodeMap<String> partition = getPartition(extras);
        UnicodeMap<String> check = new UnicodeMap<String>(mainMap).composeWith(partition, PROP_COMPOSER);
        // now pick single values
        Set<String> rawValues = mainMap.values();
        UnicodeMap<Enum> result = new UnicodeMap<>();

        for (String value : rawValues) {
            Enum propValue2 = firstProp.getEnum(value);
            UnicodeSet uset = mainMap.getSet(value);
            for (String partitionValue : partition.values()) {
                UnicodeSet oSet = partition.getSet(partitionValue);
                if (oSet.containsSome(uset)
                        && !oSet.containsAll(uset)) {
                    String positive = getBestExemplar(new UnicodeSet(uset).retainAll(oSet));
                    String negative = getBestExemplar(new UnicodeSet(uset).removeAll(oSet));
                    result.put(positive, propValue2);
                    result.put(negative, propValue2);
                    makesDifference.put(positive, partitionValue);
                } else {
                    String positive = getBestExemplar(uset);
                    result.put(positive, propValue2);
                }
            }

//            // now process partitions that made a difference
//            if (value.equals("Control__No")) {
//                int debug = 0;
//            }
//            Enum mainValue = firstProp.getEnum(D_BAR.split(value).iterator().next());
//            result.put(getBestExemplar(uset), mainValue);
        }
        return result;
    }

    private static UnicodeMap<String> getPartition(Map<String, UnicodeSet> nameToSet) {
        UnicodeMap<String> result = null;

        for (Entry<String, UnicodeSet> entry : nameToSet.entrySet()) {
            String name = entry.getKey();
            UnicodeSet uset = entry.getValue();
            UnicodeMap<String> temp = new UnicodeMap<>();
            temp.putAll(uset, name);
            temp.putAll(new UnicodeSet(uset).complement(), "!" + name);

            if (result == null) {
                result = temp;
            } else {
                result.composeWith(temp, PROP_COMPOSER);
            }
        }
        return result;
    }

    static Splitter D_BAR = Splitter.on(Pattern.compile("__"));

    private static String getBestExemplar(UnicodeSet uset) {
        Set<String> set = uset.addAllTo(new TreeSet<String>(BestExemplarLess));
        return set.iterator().next();
    }

    private static Comparator<String> BestExemplarLess = new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            // age first
            Age_Values age1 = AGE.get(o1);
            Age_Values age2 = AGE.get(o2);
            if (age1 != age2) {
                return age1.ordinal() - age2.ordinal();
            }
            General_Category_Values gc1 = GC.get(o1);
            General_Category_Values gc2 = GC.get(o2);
            if (gc1 != gc2) {
                return bestGc(o1.codePointAt(0), gc1) - bestGc(o2.codePointAt(0), gc2);
            }
            return o1.compareTo(o2);
        }

        private int bestGc(int cp, General_Category_Values gc) {
            return gc == General_Category_Values.Control && !WHITESPACE.contains(cp)
                    ? gcMap.getNumericOrder(General_Category_Values.Unassigned)
                            : gcMap.getNumericOrder(gc);

        }
    };

    static MapComparator<General_Category_Values> gcMap = new MapComparator<>(Arrays.asList(
            General_Category_Values.Uppercase_Letter,
            General_Category_Values.Lowercase_Letter,
            General_Category_Values.Other_Letter,
            General_Category_Values.Titlecase_Letter,
            General_Category_Values.Modifier_Letter,


            General_Category_Values.Decimal_Number,
            General_Category_Values.Letter_Number,
            General_Category_Values.Other_Number,

            General_Category_Values.Dash_Punctuation,
            General_Category_Values.Connector_Punctuation,
            General_Category_Values.Open_Punctuation,
            General_Category_Values.Initial_Punctuation,
            General_Category_Values.Close_Punctuation,
            General_Category_Values.Final_Punctuation,
            General_Category_Values.Other_Punctuation,

            General_Category_Values.Currency_Symbol,
            General_Category_Values.Modifier_Symbol,
            General_Category_Values.Math_Symbol,
            General_Category_Values.Other_Symbol,

            General_Category_Values.Nonspacing_Mark,
            General_Category_Values.Spacing_Mark,
            General_Category_Values.Enclosing_Mark,

            General_Category_Values.Space_Separator,

            General_Category_Values.Format,
            General_Category_Values.Control,
            General_Category_Values.Line_Separator,
            General_Category_Values.Paragraph_Separator,

            General_Category_Values.Unassigned,
            General_Category_Values.Surrogate,
            General_Category_Values.Private_Use)
            );

    private static final Composer<String> PROP_COMPOSER = new Composer<String>() {
        @Override
        public String compose(int codePoint, String string, String a, String b) {
            return a == null ? (b == null ? null : "__" + b)
                    : b == null ? a + "__"
                            : a + "__" + b;
        }
    };

    private static <T> void show(UnicodeMap<T> exemplars) {
        for (T value : new TreeSet<>(exemplars.values())) {
            UnicodeSet uset = exemplars.getSet(value);
            for (String s : uset) {
                System.out.println(Utility.hex(s) + " ; " + exemplars.getValue(s) + " ; " + IUP.getName(s.codePointAt(0)));
            }
        }
    }



    public static void main(String[] args) {
        //checkExemplars();
        //if (true) return;

        testSegments();

    }

    private static void testSegments() {
        final XSymbolTable toolUPS = new UnicodePropertySymbolTable(IUP);
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
                "÷ 000D 000A ÷",
                "÷ 1F1E6 1F1E7 ÷",
                "÷ 1F1E6 1F1E7 ÷ 1F1E8 ÷",
                "÷ 1F1E6 1F1E7 ÷ 1F1E8 1F1E9 ÷"));

        TestSegment gc3 = new TestSegment("SegmentBnf" + "GraphemeBreak" + ".txt");
        gc3.test("Gb", "/Users/markdavis/Documents/workspace/unicode-draft/Public/UCD/auxiliary/","GraphemeBreak");
        gc3.test("GB+", Arrays.asList("÷ 1F1E6 1F1E7 ÷",
                "÷ 1F1E6 1F1E7 ÷ 1F1E8 ÷",
                "÷ 1F1E6 1F1E7 ÷ 1F1E8 1F1E9 ÷"));

        //        TestSegment gc4 = new TestSegment("SegmentBnf" + "WordBreak" + ".txt");
        //        gc3.test("Wb", "/Users/markdavis/Documents/workspace/unicode-draft/Public/UCD/auxiliary/","WordBreak");
    }

    private static void checkExemplars() {
        Map<String, UnicodeSet> extras = new LinkedHashMap<String, UnicodeSet>();
        extras.put(UcdProperty.Extended_Pictographic.getShortName(), IUP.loadBinary(UcdProperty.Extended_Pictographic));
        UnicodeMap<String> makesDifference = new UnicodeMap<>();

        UnicodeMap<Enum> exemplars = pickBestExemplars(UcdProperty.Grapheme_Cluster_Break,
                extras,
                makesDifference);
        show(exemplars);
        show(makesDifference);

        Builder segmenter = Segmenter.make(ToolUnicodePropertySource.make(Default.ucd().getVersion()),"GraphemeClusterBreak");

        getExemplarStrings(exemplars, segmenter);
    }

    private static <T> void getExemplarStrings(UnicodeMap<T> exemplars, Builder segmenter) {
        Map<Double, Rule> srules = segmenter.getProcessedRules();
        for (Entry<Double, Rule> entry : srules.entrySet()) {
            System.out.println(entry.getKey() + "\t\t" + entry.getValue());
        }

    }

}
