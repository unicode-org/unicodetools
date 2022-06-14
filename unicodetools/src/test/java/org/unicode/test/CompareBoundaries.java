// CompareBoundaries tests the alignment between Word and Sentence boundaries
// and collects sequences where a word segment straddles a sentence boundary
// (i.e., "Mr.Hamster" problems).
package org.unicode.test;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UTF16;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;
import org.unicode.props.UcdPropertyValues.Sentence_Break_Values;
import org.unicode.props.UcdPropertyValues.Word_Break_Values;
import org.unicode.text.UCD.ToolUnicodePropertySource;
import org.unicode.text.UCD.UCD;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;
import org.unicode.tools.Segmenter;
import org.unicode.tools.Segmenter.Builder;

public class CompareBoundaries {
    static final UCD ucd = UCD.makeLatestVersion();
    static final IndexUnicodeProperties iup = IndexUnicodeProperties.make(Settings.latestVersion);
    static final UnicodeMap<String> names = iup.load(UcdProperty.Name);
    static final UnicodeMap<Word_Break_Values> wbs =
            iup.loadEnum(UcdProperty.Word_Break, UcdPropertyValues.Word_Break_Values.class);
    static final UnicodeMap<Sentence_Break_Values> sbs =
            iup.loadEnum(UcdProperty.Sentence_Break, UcdPropertyValues.Sentence_Break_Values.class);
    static Map<String, Boolean> wbsbs = new HashMap<>();
    static final String SEP = "&";
    static final int BRKYES = '÷';
    static final int BRKNO = '×';
    // BRKMASK is applied to BRKNO (or BRKYES) to mark a boundary where WB and SB are inconsistent
    static final int BRKMASK = 0x80000000;
    static final int PROPWB = 'W';
    static final int PROPSB = 'S';
    static int probcount = 0;
    static final Builder wbb =
            Segmenter.make(ToolUnicodePropertySource.make(ucd.getVersion()), "WordBreak");
    static final Segmenter wbseg = wbb.make();
    static final Builder sbb =
            Segmenter.make(ToolUnicodePropertySource.make(ucd.getVersion()), "SentenceBreak");
    static final Segmenter sbseg = sbb.make();
    static Set<ArrayList<Integer>> problems = new HashSet<>();

    static class LabeledProblem {
        boolean dupe;
        ArrayList<Integer> prob;

        public LabeledProblem(boolean dupe, ArrayList<Integer> prob) {
            this.dupe = dupe;
            this.prob = prob;
        }
    }

    static ArrayList<LabeledProblem> reducedProblems = new ArrayList<>();
    // Complete partitioning of the Unicode codespace into WB&SB classes
    static final int[] WbSbExemplars = {
        0x000D, // WB&SB=CR&CR (CR CR)
        0x0022, // WB&SB=DQ&CL (Double_Quote Close)
        0x202F, // WB&SB=EX&SP (ExtendNumLet Sp)
        0x005F, // WB&SB=EX&XX (ExtendNumLet Other)
        0x0300, // WB&SB=Extend&EX (Extend Extend)
        0x1F3FB, // WB&SB=Extend&XX (Extend Other)
        0x00AD, // WB&SB=FO&FO (Format Format)
        0x05D0, // WB&SB=HL&LE (Hebrew_Letter OLetter)
        0x30A2, // WB&SB=KA&LE (Katakana OLetter)
        0x32D5, // WB&SB=KA&XX (Katakana Other)
        0x0294, // WB&SB=LE&LE (ALetter OLetter)
        0x0061, // WB&SB=LE&LO (ALetter Lower)
        0x0041, // WB&SB=LE&UP (ALetter Upper)
        0x02C4, // WB&SB=LE&XX (ALetter Other)
        0x000A, // WB&SB=LF&LF (LF LF)
        0x002E, // WB&SB=MB&AT (MidNumLet ATerm)
        0x2018, // WB&SB=MB&CL (MidNumLet Close)
        0xFF07, // WB&SB=MB&XX (MidNumLet Other)
        0x003A, // WB&SB=ML&SC (MidLetter SContinue)
        0x00B7, // WB&SB=ML&XX (MidLetter Other)
        0x066C, // WB&SB=MN&NU (MidNum Numeric)
        0x002C, // WB&SB=MN&SC (MidNum SContinue)
        0x0589, // WB&SB=MN&ST (MidNum STerm)
        0x003B, // WB&SB=MN&XX (MidNum Other)
        0x0085, // WB&SB=NL&SE (Newline Sep)
        0x000C, // WB&SB=NL&SP (Newline Sp)
        0x0031, // WB&SB=NU&NU (Numeric Numeric)
        0x1F1F7, // WB&SB=RI&XX (Regional_Indicator Other)
        0x0027, // WB&SB=SQ&CL (Single_Quote Close)
        0x0020, // WB&SB=WSegSpace&SP (WSegSpace Sp)
        0x0028, // WB&SB=XX&CL (Other Close)
        0x200B, // WB&SB=XX&FO (Other Format)
        0x3042, // WB&SB=XX&LE (Other OLetter)
        0x002D, // WB&SB=XX&SC (Other SContinue)
        0x0009, // WB&SB=XX&SP (Other Sp)
        0x0021, // WB&SB=XX&ST (Other STerm)
        0x2191, // WB&SB=XX&XX (Other Other)
        0x200D // WB&SB=ZWJ&EX (ZWJ Extend)
    };
    // Extenders used in finding duplicate problematic sequences,
    // a subset of WbSbExemplars
    static final int[] extenders = {
        0x202F, // WB&SB=EX&SP (ExtendNumLet Sp)
        0x005F, // WB&SB=EX&XX (ExtendNumLet Other)
        0x0300, // WB&SB=Extend&EX (Extend Extend)
        0x00AD, // WB&SB=FO&FO (Format Format)
    };

    public static void main(String[] args) {
        // Collect all pairs of WB&SP property values
        for (int cp = 0x0000; cp <= 0x10FFFF; ++cp) {
            String wbsb = getWbValue(cp) + SEP + getSbValue(cp);
            if (wbsbs.put(wbsb, false) == null) {
                //// System.out.println("0x" + Utility.hex(cp) + ", // WB&SB=" + wbsb + " (" +
                // wbs.get(cp) + " " + sbs.get(cp) + ")");
            }
        }

        // Check that there is one exemplar character per WB&SP value
        for (int exemplar : WbSbExemplars) {
            String wbsb = getWbValue(exemplar) + SEP + getSbValue(exemplar);
            Boolean visited = wbsbs.get(wbsb);
            if (visited != null) {
                if (visited == false) {
                    if (wbsbs.put(wbsb, true) != false) {
                        System.out.println(
                                "Error updating list of exemplars for exemplar U+"
                                        + Utility.hex(exemplar)
                                        + ".  Aborting.");
                        return;
                    }
                } else {
                    System.out.println(
                            "Two or more exemplar code points, including U+"
                                    + Utility.hex(exemplar)
                                    + ", have the same WB&SB value "
                                    + wbsb
                                    + ".  Exemplars need to be updated.");
                    return;
                }
            } else {
                System.out.println(
                        "Code point U+"
                                + Utility.hex(exemplar)
                                + " has unknown WB&SB value "
                                + wbsb
                                + ".  Values and/or exemplars need to be updated.");
                return;
            }
        }

        if (wbsbs.containsValue(false)) {
            System.out.println(
                    "Not all WB&SB values were visited.  Values and/or exemplars need to be regenerated.");
            return;
        }

        // Check that all extenders are included in the set of exemplars
        for (int n : extenders) {
            boolean included = false;
            for (int m : WbSbExemplars) {
                if (n == m) {
                    included = true;
                    break;
                }
            }
            if (!included) {
                System.out.println("The extenders must be included in the set of WB&SB exemplars.");
                return;
            }
        }

        // Generate sequences of exemplar characters of length 2 and compare WB and SB boundaries
        if (segmentSequences2() == false) {
            System.out.println("Investigate test errors (2).");
            return;
        }

        // Generate sequences of exemplar characters of length 3 and compare WB and SB boundaries,
        // matching any problematic sequences of shorter length
        if (segmentSequences3() == false) {
            System.out.println("Investigate test errors (3).");
            return;
        }

        // Mark as duplicates all problematic sequences of length 3 that differ only by Extend and
        // Format characters
        reduceDuplicates3();

        // Generate sequences of exemplar characters of length 4 and compare WB and SB boundaries,
        // matching any problematic sequences of shorter length
        if (segmentSequences4() == false) {
            System.out.println("Investigate test errors (4).");
            return;
        }

        // Mark as duplicates all problematic sequences of length 4 that differ only by Extend and
        // Format characters
        reduceDuplicates4();

        // Print out which sequences are duplicates
        for (int s = 0; s < reducedProblems.size(); ++s) {
            if (reducedProblems.get(s).dupe) {
                System.out.println("Duplicate #" + (s + 1));
            }
        }

        System.out.println("OK.");
    }

    // Compare WB and SB segmentation of sequences of length 2 characters
    private static boolean segmentSequences2() {
        int[] intseq = new int[2];
        for (int x0 : WbSbExemplars) {
            intseq[0] = x0;
            for (int x1 : WbSbExemplars) {
                intseq[1] = x1;
                StringBuilder chrseq = new StringBuilder(); // input UTF-16 sequence
                String[] wbclasses = new String[2 * 2 + 1]; // WB classes and rule numbers
                ArrayList<Integer> wbcodes =
                        new ArrayList<>(2 * 2 + 1); // code points and WB symbols
                String[] sbclasses = new String[2 * 2 + 1]; // SB classes and rule numbers
                ArrayList<Integer> sbcodes =
                        new ArrayList<>(2 * 2 + 1); // code points and SB symbols
                // https://stackoverflow.com/questions/28344312/hashset-usage-with-int-arrays

                // Sequence of UTF-16 code units
                for (int i = 0; i < 2; ++i) {
                    chrseq.append(UTF16.valueOf(intseq[i]));
                }

                // Determine WB and SB boundaries
                determineBoundaries(PROPWB, wbseg, chrseq, intseq, wbcodes, wbclasses);
                determineBoundaries(PROPSB, sbseg, chrseq, intseq, sbcodes, sbclasses);

                // Compare WB and SB boundaries and save problematic sequences
                for (int k = 0; k < wbcodes.size(); ++k) {
                    if (!wbcodes.get(k).equals(sbcodes.get(k))) {
                        if (wbcodes.get(k).equals(BRKNO) && sbcodes.get(k).equals(BRKYES)) {
                            // Word segment straddles a sentence boundary
                            ArrayList<Integer> problem =
                                    new ArrayList<>(sbcodes.subList(k - 1, k + 2));
                            problem.set(1, problem.get(1) | BRKMASK);

                            if (problems.add(problem)) {
                                // Save and print out the extracted problematic sequence
                                // and the WB and SB sequences from which the problem was extracted
                                saveAndPrintProblem(sbcodes, 1, 4);
                                printSequence("WB:", wbcodes, wbclasses);
                                printSequence("SB:", sbcodes, sbclasses);
                                System.out.println();
                            }
                        } else if (!wbcodes.get(k).equals(BRKYES)
                                || !sbcodes.get(k).equals(BRKNO)) {
                            // The only other expected difference is a word boundary within a
                            // sentence segment;
                            // anything else is an error
                            System.out.println(
                                    "Unexpected WB vs. SB mismatch: "
                                            + wbcodes.get(k)
                                            + "vs. "
                                            + sbcodes.get(k));
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    // Compare WB and SB segmentation of sequences of length 3 characters,
    // matching any problematic sequences of shorter length
    private static boolean segmentSequences3() {
        int[] intseq = new int[3];
        for (int x0 : WbSbExemplars) {
            intseq[0] = x0;
            for (int x1 : WbSbExemplars) {
                intseq[1] = x1;
                for (int x2 : WbSbExemplars) {
                    intseq[2] = x2;
                    StringBuilder chrseq = new StringBuilder(); // input UTF-16 sequence
                    String[] wbclasses = new String[2 * 3 + 1]; // WB classes and rule numbers
                    ArrayList<Integer> wbcodes =
                            new ArrayList<>(2 * 3 + 1); // code points and WB symbols
                    String[] sbclasses = new String[2 * 3 + 1]; // SB classes and rule numbers
                    ArrayList<Integer> sbcodes =
                            new ArrayList<>(2 * 3 + 1); // code points and SB symbols
                    // https://stackoverflow.com/questions/28344312/hashset-usage-with-int-arrays

                    // Sequence of UTF-16 code units
                    for (int i = 0; i < 3; ++i) {
                        chrseq.append(UTF16.valueOf(intseq[i]));
                    }

                    // Determine WB and SB boundaries
                    determineBoundaries(PROPWB, wbseg, chrseq, intseq, wbcodes, wbclasses);
                    determineBoundaries(PROPSB, sbseg, chrseq, intseq, sbcodes, sbclasses);

                    // Compare WB and SB boundaries and save problematic sequences
                    for (int k = 0; k < wbcodes.size(); ++k) {
                        if (!wbcodes.get(k).equals(sbcodes.get(k))) {
                            if (wbcodes.get(k).equals(BRKNO) && sbcodes.get(k).equals(BRKYES)) {
                                // Word segment straddles a sentence boundary
                                ArrayList<Integer> problem =
                                        new ArrayList<>(sbcodes.subList(k - 1, k + 2));
                                problem.set(1, problem.get(1) | BRKMASK);

                                if (!problems.contains(problem)) {
                                    // If a similar sequence of length 2 was not previously saved,
                                    // then add the sequence of length 3 (which has additional
                                    // context)
                                    problem = new ArrayList<>(sbcodes.subList(1, 6));
                                    if (!wbcodes.get(2).equals(sbcodes.get(2))) {
                                        problem.set(1, problem.get(1) | BRKMASK);
                                    }
                                    if (!wbcodes.get(4).equals(sbcodes.get(4))) {
                                        problem.set(3, problem.get(3) | BRKMASK);
                                    }

                                    if (problems.add(problem)) {
                                        // Save and print out the extracted problematic sequence
                                        // and the WB and SB sequences from which the problem was
                                        // extracted
                                        saveAndPrintProblem(sbcodes, 1, 6);
                                        printSequence("WB:", wbcodes, wbclasses);
                                        printSequence("SB:", sbcodes, sbclasses);
                                        System.out.println();
                                    }
                                }
                            } else if (!wbcodes.get(k).equals(BRKYES)
                                    || !sbcodes.get(k).equals(BRKNO)) {
                                // The only other expected difference is a word boundary within a
                                // sentence segment;
                                // anything else is an error
                                System.out.println(
                                        "Unexpected WB vs. SB mismatch: "
                                                + wbcodes.get(k)
                                                + "vs. "
                                                + sbcodes.get(k));
                                return false;
                            }
                        }
                    }
                }
            }
        }

        return true;
    }

    // Compare WB and SB segmentation of sequences of length 4 characters,
    // matching any problematic sequences of shorter length
    private static boolean segmentSequences4() {
        int[] intseq = new int[4];
        for (int x0 : WbSbExemplars) {
            intseq[0] = x0;
            for (int x1 : WbSbExemplars) {
                intseq[1] = x1;
                for (int x2 : WbSbExemplars) {
                    intseq[2] = x2;
                    for (int x3 : WbSbExemplars) {
                        intseq[3] = x3;
                        StringBuilder chrseq = new StringBuilder(); // input UTF-16 sequence
                        String[] wbclasses = new String[2 * 4 + 1]; // WB classes and rule numbers
                        ArrayList<Integer> wbcodes =
                                new ArrayList<>(2 * 4 + 1); // code points and WB symbols
                        String[] sbclasses = new String[2 * 4 + 1]; // SB classes and rule numbers
                        ArrayList<Integer> sbcodes =
                                new ArrayList<>(2 * 4 + 1); // code points and SB symbols
                        // https://stackoverflow.com/questions/28344312/hashset-usage-with-int-arrays

                        // Sequence of UTF-16 code units
                        for (int i = 0; i < 4; ++i) {
                            chrseq.append(UTF16.valueOf(intseq[i]));
                        }

                        // Determine WB and SB boundaries
                        determineBoundaries(PROPWB, wbseg, chrseq, intseq, wbcodes, wbclasses);
                        determineBoundaries(PROPSB, sbseg, chrseq, intseq, sbcodes, sbclasses);

                        // Compare WB and SB boundaries and save problematic sequences
                        for (int k = 0; k < wbcodes.size(); ++k) {
                            if (!wbcodes.get(k).equals(sbcodes.get(k))) {
                                if (wbcodes.get(k).equals(BRKNO) && sbcodes.get(k).equals(BRKYES)) {
                                    // Word segment straddles a sentence boundary
                                    ArrayList<Integer> problem1 =
                                            new ArrayList<>(sbcodes.subList(k - 1, k + 2));
                                    problem1.set(1, problem1.get(1) | BRKMASK);
                                    ArrayList<Integer> problem2 = null;
                                    if (k - 3 > 0) {
                                        problem2 = new ArrayList<>(sbcodes.subList(k - 3, k + 2));
                                        if (!wbcodes.get(k - 2).equals(sbcodes.get(k - 2))) {
                                            problem2.set(1, problem2.get(1) | BRKMASK);
                                        }
                                        if (!wbcodes.get(k).equals(sbcodes.get(k))) {
                                            problem2.set(3, problem2.get(3) | BRKMASK);
                                        }
                                    }
                                    ArrayList<Integer> problem3 = null;
                                    if (k + 3 < 8) {
                                        problem3 = new ArrayList<>(sbcodes.subList(k - 1, k + 4));
                                        if (!wbcodes.get(k).equals(sbcodes.get(k))) {
                                            problem3.set(1, problem3.get(1) | BRKMASK);
                                        }
                                        if (!wbcodes.get(k + 2).equals(sbcodes.get(k + 2))) {
                                            problem3.set(3, problem3.get(3) | BRKMASK);
                                        }
                                    }

                                    if (!(problems.contains(problem1)
                                            || problem2 != null && problems.contains(problem2)
                                            || problem3 != null && problems.contains(problem3))) {
                                        // If no similar sequence of length 2 or 3 was previously
                                        // saved,
                                        // then add the sequence of length 4 (which has additional
                                        // context)
                                        ArrayList<Integer> problem =
                                                new ArrayList<>(sbcodes.subList(1, 8));
                                        if (!wbcodes.get(2).equals(sbcodes.get(2))) {
                                            problem.set(1, problem.get(1) | BRKMASK);
                                        }
                                        if (!wbcodes.get(4).equals(sbcodes.get(4))) {
                                            problem.set(3, problem.get(3) | BRKMASK);
                                        }
                                        if (!wbcodes.get(6).equals(sbcodes.get(6))) {
                                            problem.set(5, problem.get(5) | BRKMASK);
                                        }

                                        if (problems.add(problem)) {
                                            // Save and print out the extracted problematic sequence
                                            // and the WB and SB sequences from which the problem
                                            // was extracted
                                            saveAndPrintProblem(sbcodes, 1, 8);
                                            printSequence("WB:", wbcodes, wbclasses);
                                            printSequence("SB:", sbcodes, sbclasses);
                                            System.out.println();
                                        }
                                    }
                                } else if (!wbcodes.get(k).equals(BRKYES)
                                        || !sbcodes.get(k).equals(BRKNO)) {
                                    // The only other expected difference is a word boundary within
                                    // a sentence segment;
                                    // anything else is an error
                                    System.out.println(
                                            "Unexpected WB vs. SB mismatch: "
                                                    + wbcodes.get(k)
                                                    + "vs. "
                                                    + sbcodes.get(k));
                                    return false;
                                }
                            }
                        }
                    }
                }
            }
        }

        return true;
    }

    private static String getWbValue(int cp) {
        return wbs.get(cp).getShortName();
    }

    private static String getSbValue(int cp) {
        return sbs.get(cp).getShortName();
    }

    private static void determineBoundaries(
            int propWbSb,
            Segmenter segmenter,
            StringBuilder chrSeq,
            int[] intSeq,
            ArrayList<Integer> codes,
            String[] classes) {
        int i, j, b;

        for (i = 0, j = 0; j < chrSeq.length(); ++i, ++j) {
            b = segmenter.breaksAt(chrSeq, j) ? BRKYES : BRKNO;
            classes[2 * i] = String.valueOf(segmenter.getBreakRule());
            classes[2 * i + 1] = propWbSb == PROPWB ? getWbValue(intSeq[i]) : getSbValue(intSeq[i]);
            codes.add(b);
            codes.add(intSeq[i]);
            if (intSeq[i] >= 0x10000) {
                ++j;
            }
        }
        b = segmenter.breaksAt(chrSeq, j) ? BRKYES : BRKNO;
        classes[2 * i] = String.valueOf(segmenter.getBreakRule());
        codes.add(b);
    }

    private static void printSequence(String prefix, ArrayList<Integer> codes, String[] classes) {
        System.out.print(prefix);
        for (int c = 0; c < codes.size(); ++c) {
            System.out.print(" ");
            int p = codes.get(c);
            System.out.print(p == BRKYES || p == BRKNO ? (char) p : Utility.hex(p));
            System.out.print("(" + classes[c] + ")");
        }
        System.out.println();
    }

    private static void saveAndPrintProblem(
            ArrayList<Integer> codes, int startIndex, int endIndex) {
        reducedProblems.add(new LabeledProblem(false, codes));

        System.out.print("Problem #" + ++probcount + ":");
        for (int c = startIndex; c < endIndex; ++c) {
            System.out.print(" ");
            int p = codes.get(c);
            System.out.print(p == BRKYES || p == BRKNO ? (char) p : Utility.hex(p));
        }
        System.out.println();
    }

    private static void reduceDuplicates3() {
        for (int s = 0; s < reducedProblems.size(); ++s) {
            ArrayList<Integer> ps = reducedProblems.get(s).prob;
            if (ps.size() / 2 > 2) {
                break;
            }
            reduceDuplicates(ps, 2);
        }
    }

    private static void reduceDuplicates4() {
        for (int s = 0; s < reducedProblems.size(); ++s) {
            ArrayList<Integer> ps = reducedProblems.get(s).prob;
            if (ps.size() / 2 == 2) {
                for (int e : extenders) {
                    for (int f : extenders) {
                        ArrayList<Integer> orig = new ArrayList<>(4);
                        for (int j = 1; j < ps.size(); j += 2) {
                            orig.add(ps.get(j));
                        }
                        orig.add(1, e);
                        orig.add(3, f);
                        checkDuplicates(orig);
                    }
                }
            } else if (ps.size() / 2 == 3) {
                reduceDuplicates(ps, 3);
            }
        }
    }

    private static void reduceDuplicates(ArrayList<Integer> ps, int count) {
        for (int e : extenders) {
            for (int i = 0; i < count; ++i) {
                ArrayList<Integer> orig = new ArrayList<>(count + 1);
                for (int j = 1; j < ps.size(); j += 2) {
                    orig.add(ps.get(j));
                }
                orig.add(i + 1, e);
                checkDuplicates(orig);
            }
        }
    }

    private static void checkDuplicates(ArrayList<Integer> orig) {
        int size = orig.size();
        for (int t = 0; t < reducedProblems.size(); ++t) {
            ArrayList<Integer> pt = reducedProblems.get(t).prob;
            if (pt.size() < 2 * size + 1) {
                continue;
            }
            if (pt.size() > 2 * size + 1) {
                break;
            }
            ArrayList<Integer> dupe = new ArrayList<>(size);
            for (int j = 1; j < 2 * size + 1; j += 2) {
                dupe.add(pt.get(j));
            }
            if (dupe.equals(orig)) {
                reducedProblems.get(t).dupe = true;
            }
        }
    }
}
