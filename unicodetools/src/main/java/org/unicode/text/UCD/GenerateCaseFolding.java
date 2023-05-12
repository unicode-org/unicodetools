/**
 * ****************************************************************************** Copyright (C)
 * 1996-2001, International Business Machines Corporation and * others. All Rights Reserved. *
 * ******************************************************************************
 *
 * <p>$Source: /home/cvsroot/unicodetools/org/unicode/text/UCD/GenerateCaseFolding.java,v $
 *
 * <p>******************************************************************************
 */
package org.unicode.text.UCD;

import com.ibm.icu.text.UTF16;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.BitSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.UnicodeDataFile;
import org.unicode.text.utility.UnicodeDataFile.FileInfix;
import org.unicode.text.utility.Utility;

public class GenerateCaseFolding implements UCD_Types {
    public static boolean DEBUG = false;
    public static boolean COMMENT_DIFFS = false; // ON if we want a comment on mappings != lowercase
    public static boolean PICK_SHORT =
            false; // picks short value for SIMPLE if in FULL, changes weighting
    public static boolean NF_CLOSURE =
            false; // picks short value for SIMPLE if in FULL, changes weighting
    static final int CHECK_CHAR = 0x130; // for debugging, change to actual character, otherwise -1

    // PICK_SHORT & NF_CLOSURE = false for old style

    /*public static void main(String[] args) throws java.io.IOException {
        makeCaseFold(arg[0]);
        //getAge();
    }
     */

    static PrintWriter log;

    public static void makeCaseFold(boolean normalized) throws java.io.IOException {
        PICK_SHORT = NF_CLOSURE = normalized;

        String suffix = FileInfix.getDefault().getFileSuffix(".txt");
        log =
                Utility.openPrintWriter(
                        Settings.Output.GEN_DIR + "/log",
                        "CaseFoldingLog" + suffix,
                        Utility.LATIN1_UNIX);
        System.out.println("Writing Log: " + "CaseFoldingLog" + suffix);

        System.out.println("Making Full Data");
        final Map<String, String> fullData = getCaseFolding(true, NF_CLOSURE, "");
        Utility.fixDot();

        System.out.println("Making Simple Data");
        final Map<String, String> simpleData = getCaseFolding(false, NF_CLOSURE, "");
        // write the data

        System.out.println("Making Turkish Full Data");
        final Map<String, String> fullDataTurkish = getCaseFolding(true, NF_CLOSURE, "tr");
        Utility.fixDot();

        System.out.println("Making Simple Data");
        final Map<String, String> simpleDataTurkish = getCaseFolding(false, NF_CLOSURE, "tr");
        // write the data

        Utility.fixDot();
        System.out.println("Writing");
        String filename = "CaseFolding";
        if (normalized) {
            filename += "-Normalized";
        }
        final String directory = "UCD/" + Default.ucd().getVersion() + '/';
        final UnicodeDataFile fc =
                UnicodeDataFile.openAndWriteHeader(directory, filename)
                        .setSkipCopyright(Settings.SKIP_COPYRIGHT);
        final PrintWriter out = fc.out;

        /*
        PrintWriter out = new PrintWriter(
            new BufferedWriter(
            new OutputStreamWriter(
                new FileOutputStream(directory + fileRoot + GenerateData.getFileSuffix()),
                "UTF8"),
            4*1024));
         */

        for (int ch = 0; ch <= 0x10FFFF; ++ch) {
            Utility.dot(ch);

            var normativeSCF = new StringBuilder();
            var normativeCF = new StringBuilder();

            try {
                if (!charsUsed.get(ch)) {
                    continue;
                }

                final String rFull = fullData.get(UTF16.valueOf(ch));
                final String rSimple = simpleData.get(UTF16.valueOf(ch));
                final String rFullTurkish = fullDataTurkish.get(UTF16.valueOf(ch));
                final String rSimpleTurkish = simpleDataTurkish.get(UTF16.valueOf(ch));
                if (rFull == null
                        && rSimple == null
                        && rFullTurkish == null
                        && rSimpleTurkish == null) {
                    continue;
                }

                // Hardcode variants of letter i.
                if (ch == 0x49) {
                    drawLine(out, ch, "C", "i", normativeSCF, normativeCF);
                    drawLine(out, ch, "T", "\u0131", normativeSCF, normativeCF);
                } else if (ch == 0x130) {
                    drawLine(out, ch, "F", "i\u0307", normativeSCF, normativeCF);
                    drawLine(out, ch, "T", "i", normativeSCF, normativeCF);
                } else if (ch == 0x131) {
                    // do nothing
                    // drawLine(out, ch, "I", "i");
                } else if (rFull != null && rFull.equals(rSimple)
                        || (PICK_SHORT && UTF16.countCodePoint(rFull) == 1)) {
                    drawLine(out, ch, "C", rFull, normativeSCF, normativeCF);
                } else {
                    if (rFull != null) {
                        drawLine(out, ch, "F", rFull, normativeSCF, normativeCF);
                    }
                    if (rSimple != null) {
                        drawLine(out, ch, "S", rSimple, normativeSCF, normativeCF);
                    }
                }
                if (rFullTurkish != null && !rFullTurkish.equals(rFull)) {
                    drawLine(out, ch, "T", rFullTurkish, normativeSCF, normativeCF);
                }
                if (rSimpleTurkish != null && !rSimpleTurkish.equals(rSimple)) {
                    drawLine(out, ch, "t", rSimpleTurkish, normativeSCF, normativeCF);
                }
            } finally {
                // We have two independent definitions of the case foldings.
                // Check that they are consistent. Eventually we should get rid of one of them, see
                // https://github.com/unicode-org/unicodetools/issues/426.
                if (normativeSCF.length() == 0) {
                    normativeSCF.append(UTF16.valueOf(ch));
                }
                if (normativeCF.length() == 0) {
                    normativeCF.append(UTF16.valueOf(ch));
                }
                final String ucdSCF = Default.ucd().getCase(ch, UCD.SIMPLE, UCD.FOLD);
                final String ucdCF = Default.ucd().getCase(ch, UCD.FULL, UCD.FOLD);
                if (!ucdSCF.equals(normativeSCF.toString())) {
                    throw new AssertionError(
                            String.format(
                                    "UCD.getCase(\"\\u%04X\", UCD.SIMPLE, UCD.FOLD)=\"%s\", should be \"%s\" per CaseFolding.txt",
                                    ch, ucdSCF, normativeSCF));
                }
                if (!ucdCF.equals(normativeCF.toString())) {
                    throw new AssertionError(
                            String.format(
                                    "UCD.getCase(\"\\u%04X\", UCD.FULL, UCD.FOLD)=\"%s\", should be \"%s\" per CaseFolding.txt",
                                    ch, ucdCF, normativeCF));
                }
            }
        }
        out.println("#");
        out.println("# EOF");
        fc.close();
        log.close();
    }

    /* Goal is following (with no entries for 0131 or 0069)

    0049; C; 0069; # LATIN CAPITAL LETTER I
    0049; T; 0131; # LATIN CAPITAL LETTER I

    0130; F; 0069 0307; # LATIN CAPITAL LETTER I WITH DOT ABOVE
    0130; T; 0069; # LATIN CAPITAL LETTER I WITH DOT ABOVE
         */

    static void drawLine(
            PrintWriter out,
            int ch,
            String type,
            String result,
            StringBuilder normativeSCF,
            StringBuilder normativeCF) {
        String comment = "";
        if (COMMENT_DIFFS) {
            final String lower = Default.ucd().getCase(UTF16.valueOf(ch), FULL, LOWER);
            if (!lower.equals(result)) {
                final String lower2 = Default.ucd().getCase(UTF16.valueOf(ch), FULL, LOWER);
                if (lower.equals(lower2)) {
                    comment = "[Diff " + Utility.hex(lower, " ") + "] ";
                } else {
                    Utility.fixDot();
                    System.out.println("PROBLEM WITH: " + Default.ucd().getCodeAndName(ch));
                    comment =
                            "[DIFF "
                                    + Utility.hex(lower, " ")
                                    + ", "
                                    + Utility.hex(lower2, " ")
                                    + "] ";
                }
            }
        }

        if (type == "C" || type == "S") {
            if (normativeSCF.length() != 0) {
                throw new AssertionError(
                        String.format("Conflicting SCF assignments for U+%04X", ch));
            }
            normativeSCF.append(result);
        }
        if (type == "C" || type == "F") {
            if (normativeCF.length() != 0) {
                throw new AssertionError(
                        String.format("Conflicting CF assignments for U+%04X", ch));
            }
            normativeCF.append(result);
        }

        out.println(
                Utility.hex(ch)
                        + "; "
                        + type
                        + "; "
                        + Utility.hex(result, " ")
                        + "; # "
                        + comment
                        + Default.ucd().getName(ch));
    }

    static int probeCh = 0x01f0;
    static String shower = UTF16.valueOf(probeCh);
    // Public only for unicode.text.UCD.UData.
    // We have two independent definitions of the case foldings.
    // Eventually we should get rid of one of them, see
    // https://github.com/unicode-org/unicodetools/issues/426.
    public static final int[] simpleAdditions = {
        // [175-A66] add Simple_Case_Folding mappings for U+1FD3, U+1FE3, and U+FB05, see L2/23-062;
        // for Unicode Version 15.1.
        // ΐ → ΐ
        // GREEK SMALL LETTER IOTA WITH DIALYTIKA AND OXIA →
        // GREEK SMALL LETTER IOTA WITH DIALYTIKA AND TONOS
        0x1FD3, 0x0390,
        // ΰ → ΰ
        // GREEK SMALL LETTER UPSILON WITH DIALYTIKA AND OXIA →
        // GREEK SMALL LETTER UPSILON WITH DIALYTIKA AND TONOS
        0x1FE3, 0x03B0,
        // ﬅ → ﬆ
        // LATIN SMALL LIGATURE LONG S T → LATIN SMALL LIGATURE ST
        0xFB05, 0xFB06
    };

    private static Map<String, String> getCaseFolding(
            boolean full, boolean nfClose, String condition) throws java.io.IOException {
        final Map<String, Set<String>> data = new TreeMap<String, Set<String>>();
        final Map<String, String> repChar = new TreeMap<String, String>();

        // get the equivalence classes

        for (int ch = 0; ch <= 0x10FFFF; ++ch) {
            Utility.dot(ch);
            // if ((ch & 0x3FF) == 0) System.out.println(Utility.hex(ch));
            if (!Default.ucd().isRepresented(ch)) {
                continue;
            }
            getClosure(ch, data, full, nfClose, condition);
        }

        // get the representative characters

        final Iterator<String> it = data.keySet().iterator();
        while (it.hasNext()) {
            final String s = it.next();
            final Set<String> set = data.get(s);
            show = set.contains(shower);
            if (show) {
                Utility.fixDot();
                System.out.println(toString(set));
            }

            // Pick the best available representative

            String rep = null;
            int repGood = 0;
            Iterator<String> it2 = set.iterator();
            while (it2.hasNext()) {
                final String s2 = it2.next();
                final int s2Good = goodness(s2, full, condition);
                if (s2Good > repGood) {
                    rep = s2;
                    repGood = s2Good;
                }
            }
            if (rep == null) {
                Utility.fixDot();
                System.err.println("No representative for: " + toString(set));
            } else if ((repGood & (NFC_FORMAT | ISLOWER)) != (NFC_FORMAT | ISLOWER)) {
                String message = "";
                if ((repGood & NFC_FORMAT) == 0) {
                    message += " [NOT NFC FORMAT]";
                }
                if ((repGood & ISLOWER) == 0) {
                    message += " [NOT LOWERCASE]";
                }
                Utility.fixDot();
                log.println("Non-Optimal Representative " + message);
                log.println(" Rep:\t" + Default.ucd().getCodeAndName(rep));
                log.println(" Set:\t" + toString(set, true, true));
            }

            log.println();
            log.println();
            log.println(rep + "\t#" + Default.ucd().getName(rep));

            // Add it for all the elements of the set

            it2 = set.iterator();
            while (it2.hasNext()) {
                final String s2 = it2.next();
                if (s2.equals(rep)) {
                    continue;
                }

                log.println(s2 + "\t#" + Default.ucd().getName(s2));

                if (UTF16.countCodePoint(s2) == 1) {
                    repChar.put(s2, rep);
                    charsUsed.set(s2.codePointAt(0));
                }
            }
        }

        // Additions that don't naturally fall out of the closure.
        if (!full) {
            for (int i = 0; i < simpleAdditions.length; i += 2) {
                int c1 = simpleAdditions[i];
                int c2 = simpleAdditions[i + 1];
                String s1 = UTF16.valueOf(c1);
                String s2 = UTF16.valueOf(c2);
                String t = repChar.get(s1);
                if (t != null) {
                    throw new IllegalArgumentException(
                            String.format(
                                    "GenerateCaseFolding: "
                                            + "Trying to add scf(U+%04X)→U+%04X (%s→%s) but "
                                            + "the source character already has a mapping to %s",
                                    c1, c2, s1, s2, t));
                }
                repChar.put(s1, s2);
            }
        }
        return repChar;
    }

    static BitSet charsUsed = new BitSet();
    static boolean show = false;
    static final int NFC_FORMAT = 64;
    static final int ISLOWER = 128;

    static int goodness(String s, boolean full, String condition) {
        if (s == null) {
            return 0;
        }
        int result = 32 - s.length();
        if (!PICK_SHORT) {
            result = s.length();
        }
        if (!full) {
            result <<= 8;
        }
        // Cherokee case-folds to uppercase letters which were encoded first.
        // It became bicameral in Unicode 8 with the addition of lowercase letters.
        int first = s.codePointAt(0);
        boolean isCherokee = 0x13A0 <= first && first <= 0x13FF; // original Cherokee block
        final String low;
        if (isCherokee) {
            low = upper(lower(s, full, condition), full, condition);
        } else {
            low = lower(upper(s, full, condition), full, condition);
        }
        if (s.equals(low)) {
            result |= ISLOWER;
        } else if (PICK_SHORT && Default.nfd().normalize(s).equals(Default.nfd().normalize(low))) {
            result |= ISLOWER;
        }

        if (s.equals(Default.nfc().normalize(s))) {
            result |= NFC_FORMAT;
        }

        if (show) {
            Utility.fixDot();
            System.out.println(Utility.hex(result) + ", " + Default.ucd().getCodeAndName(s));
        }
        return result;
    }

    /*
    static HashSet temp = new HashSet();
    static void normalize(HashSet set) {
        temp.clear();
        temp.addAll(set);
        set.clear();
        Iterator it = temp.iterator();
        while (it.hasNext()) {
            String s = (String) it.next();
            String s2 = KC.normalize(s);
            set.add(s);
            data2.put(s,set);
            if (!s.equals(s2)) {
                set.add(s2);
                data2.put(s2,set);
                System.err.println("Adding " + Utility.hex(s) + " by " + Utility.hex(s2));
            }
        }
    }
     */

    /*
           String
           String lower1 = Default.ucd.getLowercase(ch);
           String lower2 = Default.ucd.toLowercase(ch,option);

           char ch2 = Default.ucd.getLowercase(Default.ucd.getUppercase(ch).charAt(0)).charAt(0);
           //String lower1 = String.valueOf(Default.ucd.getLowercase(ch));
           //String lower = Default.ucd.toLowercase(ch2,option);
           String upper = Default.ucd.toUppercase(ch2,option);
           String lowerUpper = Default.ucd.toLowercase(upper,option);
           //String title = Default.ucd.toTitlecase(ch2,option);
           //String lowerTitle = Default.ucd.toLowercase(upper,option);

           if (ch != ch2 || lowerUpper.length() != 1 || ch != lowerUpper.charAt(0)) { //
               output.println(Utility.hex(ch)
                   + "; " + (lowerUpper.equals(lower1) ? "L" : lowerUpper.equals(lower2) ? "S" : "E")
                   + "; " + Utility.hex(lowerUpper," ")
                   + ";\t#" + Default.ucd.getName(ch)
                   );
               //if (!lowerUpper.equals(lower)) {
               //    output.println("Warning1: " + Utility.hex(lower) + " " + Default.ucd.getName(lower));
               //}
               //if (!lowerUpper.equals(lowerTitle)) {
               //    output.println("Warning2: " + Utility.hex(lowerTitle) + " " + Default.ucd.getName(lowerTitle));
               //}
           }
    */

    static void getClosure(
            int ch,
            Map<String, Set<String>> data,
            boolean full,
            boolean nfClose,
            String condition) {
        if (ch == '\u023F') {
            System.out.println("???");
        }
        final String charStr = UTF16.valueOf(ch);
        final String lowerStr = lower(charStr, full, condition);
        final String titleStr = title(charStr, full, condition);
        final String upperStr = upper(charStr, full, condition);
        if (charStr.equals(lowerStr) && charStr.equals(upperStr) && charStr.equals(titleStr)) {
            return;
        }
        if (DEBUG) {
            System.err.println("Closure for " + Utility.hex(ch));
        }

        // make new set
        final Set<String> set = new TreeSet<String>();
        set.add(charStr);

        // add cases to get started
        add(set, lowerStr, data);
        add(set, upperStr, data);
        add(set, titleStr, data);

        // close it
        main:
        while (true) {
            final Iterator<String> it = set.iterator();
            while (it.hasNext()) {
                final String s = it.next();
                // do funny stuff since we can't modify set while iterating
                // We don't do this because if the source is not normalized, we don't want to
                // normalize
                if (nfClose) {
                    if (add(set, Default.nfd().normalize(s), data)) {
                        continue main;
                    }
                    if (add(set, Default.nfc().normalize(s), data)) {
                        continue main;
                    }
                    if (add(set, Default.nfkd().normalize(s), data)) {
                        continue main;
                    }
                    if (add(set, Default.nfkc().normalize(s), data)) {
                        continue main;
                    }
                }
                if (add(set, lower(s, full, condition), data)) {
                    continue main;
                }
                if (add(set, title(s, full, condition), data)) {
                    continue main;
                }
                if (add(set, upper(s, full, condition), data)) {
                    continue main;
                }
            }
            break;
        }
        if (set.size() > 1) {
            data.put(charStr, set);
        }
    }

    static String lower(String s, boolean full, String condition) {
        final String result = lower2(s, full, condition);
        return result.replace('\u03C2', '\u03C3'); // HACK for lower
    }

    // These functions are no longer necessary, since Default.ucd is parameterized,
    // but it's not worth changing

    static String lower2(String s, boolean full, String condition) {
        /*if (!full) {
            if (s.length() != 1) return s;
            return Default.ucd.getCase(UTF32.char32At(s,0), SIMPLE, LOWER);
        }
         */
        return Default.ucd().getCase(s, full ? FULL : SIMPLE, LOWER, condition);
    }

    static String upper(String s, boolean full, String condition) {
        /* if (!full) {
            if (s.length() != 1) return s;
            return Default.ucd.getCase(UTF32.char32At(s,0), FULL, UPPER);
        }
         */
        return Default.ucd().getCase(s, full ? FULL : SIMPLE, UPPER, condition);
    }

    static String title(String s, boolean full, String condition) {
        /*if (!full) {
            if (s.length() != 1) return s;
            return Default.ucd.getCase(UTF32.char32At(s,0), FULL, TITLE);
        }
         */
        return Default.ucd().getCase(s, full ? FULL : SIMPLE, TITLE, condition);
    }

    static boolean add(Set<String> set, String s, Map<String, Set<String>> data) {
        if (set.contains(s)) {
            return false;
        }
        set.add(s);
        if (DEBUG) {
            System.err.println("adding: " + toString(set));
        }
        final Set<String> other = data.get(s);
        if (other != null && other != set) { // merge
            // make all the items in set point to merged set
            final Iterator<String> it = other.iterator();
            while (it.hasNext()) {
                data.put(it.next(), set);
            }
            set.addAll(other);
        }
        if (DEBUG) {
            System.err.println("done adding: " + toString(set));
        }
        return true;
    }

    static String toString(Set<String> set) {
        return toString(set, false, false);
    }

    static String toString(Set<String> set, boolean name, boolean crtab) {
        String result = "{";
        final Iterator<String> it2 = set.iterator();
        boolean first = true;
        while (it2.hasNext()) {
            final String s2 = it2.next();
            if (!first) {
                if (crtab) {
                    result += ";\n\t";
                } else {
                    result += "; ";
                }
            }
            first = false;
            if (name) {
                result += Default.ucd().getCodeAndName(s2);
            } else {
                result += Utility.hex(s2, " ");
            }
        }
        return result + "}";
    }

    static boolean specialNormalizationDiffers(int ch) {
        if (ch == 0x00DF) {
            return true; // es-zed
        }
        return !Default.nfkd().isNormalized(ch);
    }

    static String specialNormalization(String s) {
        if (s.equals("\u00DF")) {
            return "ss";
        }
        return Default.nfkd().normalize(s);
    }

    static boolean isExcluded(int ch) {
        // if (ch == 0x130) return true;                  // skip LATIN CAPITAL LETTER I WITH DOT
        // ABOVE
        if (ch == 0x0132 || ch == 0x0133) {
            return true; // skip IJ, ij
        }
        if (0x1F16A <= ch && ch <= 0x1F16C) {
            return true; // skip raised MC/MD/MR signs
        }
        if (ch == 0x037A) {
            return true; // skip GREEK YPOGEGRAMMENI
        }
        if (0x249C <= ch && ch <= 0x24B5) {
            return true; // skip PARENTHESIZED LATIN SMALL LETTER A..
        }
        if (0x20A8 <= ch && ch <= 0x217B) {
            return true; // skip Rupee..
        }

        final byte type = Default.ucd().getDecompositionType(ch);
        if (type == COMPAT_SQUARE) {
            return true;
        }
        // if (type == COMPAT_UNSPECIFIED) return true;
        return false;
    }

    static void generateSpecialCasing(boolean normalize) throws IOException {
        final Map<Integer, String> sorted = new TreeMap<Integer, String>();

        String suffix2 = "";
        if (normalize) {
            suffix2 = "-Normalized";
        }
        String suffix = FileInfix.getDefault().getFileSuffix(".txt");
        final PrintWriter log =
                Utility.openPrintWriter(
                        Settings.Output.GEN_DIR,
                        "log/SpecialCasingExceptions" + suffix2 + suffix,
                        Utility.LATIN1_UNIX);

        for (int ch = 0; ch <= 0x10FFFF; ++ch) {
            Utility.dot(ch);
            if (!Default.ucd().isRepresented(ch)) {
                continue;
            }
            if (!specialNormalizationDiffers(ch)) {
                continue;
            }
            if (0x1F110 <= ch && ch <= 0x1F12A || ch == 0x1F12D || ch == 0x1F12E) {
                continue;
            }

            final String lower = Default.nfc().normalize(Default.ucd().getCase(ch, SIMPLE, LOWER));
            final String upper = Default.nfc().normalize(Default.ucd().getCase(ch, SIMPLE, UPPER));
            final String title = Default.nfc().normalize(Default.ucd().getCase(ch, SIMPLE, TITLE));

            final String chstr = UTF16.valueOf(ch);

            final String decomp = specialNormalization(chstr);
            String flower = Default.nfc().normalize(Default.ucd().getCase(decomp, SIMPLE, LOWER));
            String fupper = Default.nfc().normalize(Default.ucd().getCase(decomp, SIMPLE, UPPER));
            String ftitle = Default.nfc().normalize(Default.ucd().getCase(decomp, SIMPLE, TITLE));

            String base = decomp;
            String blower = specialNormalization(lower);
            String bupper = specialNormalization(upper);
            String btitle = specialNormalization(title);

            if (true) {
                flower = Default.nfc().normalize(flower);
                fupper = Default.nfc().normalize(fupper);
                ftitle = Default.nfc().normalize(ftitle);
                base = Default.nfc().normalize(base);
                blower = Default.nfc().normalize(blower);
                bupper = Default.nfc().normalize(bupper);
                btitle = Default.nfc().normalize(btitle);
            }

            if (ch == CHECK_CHAR) {
                System.out.println("Code: " + Default.ucd().getCodeAndName(ch));
                System.out.println("Decomp: " + Default.ucd().getCodeAndName(decomp));
                System.out.println("Base: " + Default.ucd().getCodeAndName(base));
                System.out.println("SLower: " + Default.ucd().getCodeAndName(lower));
                System.out.println("FLower: " + Default.ucd().getCodeAndName(flower));
                System.out.println("BLower: " + Default.ucd().getCodeAndName(blower));
                System.out.println("STitle: " + Default.ucd().getCodeAndName(title));
                System.out.println("FTitle: " + Default.ucd().getCodeAndName(ftitle));
                System.out.println("BTitle: " + Default.ucd().getCodeAndName(btitle));
                System.out.println("SUpper: " + Default.ucd().getCodeAndName(upper));
                System.out.println("FUpper: " + Default.ucd().getCodeAndName(fupper));
                System.out.println("BUpper: " + Default.ucd().getCodeAndName(bupper));
            }

            // presumably if there is a single code point, it would already be in the simple
            // mappings

            if (UTF16.countCodePoint(flower) == 1
                    && UTF16.countCodePoint(fupper) == 1
                    && UTF16.countCodePoint(title) == 1) {
                if (ch == CHECK_CHAR) {
                    System.out.println(
                            "Skipping single code point: " + Default.ucd().getCodeAndName(ch));
                }
                continue;
            }

            // if there is no change from the base, skip

            if (flower.equals(base) && fupper.equals(base) && ftitle.equals(base)) {
                if (ch == CHECK_CHAR) {
                    System.out.println("Skipping equals base: " + Default.ucd().getCodeAndName(ch));
                }
                continue;
            }

            // fix special cases
            // if (flower.equals(blower) && fupper.equals(bupper) && ftitle.equals(btitle))
            // continue;
            if (flower.equals(blower)) {
                flower = lower;
            }
            if (fupper.equals(bupper)) {
                fupper = upper;
            }
            if (ftitle.equals(btitle)) {
                ftitle = title;
            }

            // if there are no changes from the original, or the expanded original, skip

            if (flower.equals(lower) && fupper.equals(upper) && ftitle.equals(title)) {
                if (ch == CHECK_CHAR) {
                    System.out.println("Skipping unchanged: " + Default.ucd().getCodeAndName(ch));
                }
                continue;
            }

            final String name = Default.ucd().getName(ch);

            final int order =
                    name.equals("LATIN SMALL LETTER SHARP S")
                            ? 1
                            : ch == 0x130
                                    ? 2
                                    : name.indexOf("ARMENIAN SMALL LIGATURE") >= 0
                                            ? 4
                                            : name.indexOf("LIGATURE") >= 0
                                                    ? 3
                                                    : name.indexOf("GEGRAMMENI") < 0
                                                            ? 5
                                                            : UTF16.countCodePoint(ftitle) == 1
                                                                    ? 6
                                                                    : UTF16.countCodePoint(fupper)
                                                                                    == 2
                                                                            ? 7
                                                                            : 8;

            if (ch == CHECK_CHAR) {
                System.out.println("Order: " + order + " for " + Default.ucd().getCodeAndName(ch));
            }

            // HACK
            final boolean denormalize = !normalize && order != 6 && order != 7;

            final String mapping =
                    Utility.hex(ch)
                            + "; "
                            + Utility.hex(
                                    flower.equals(base)
                                            ? chstr
                                            : denormalize
                                                    ? Default.nfd().normalize(flower)
                                                    : flower)
                            + "; "
                            + Utility.hex(
                                    ftitle.equals(base)
                                            ? chstr
                                            : denormalize
                                                    ? Default.nfd().normalize(ftitle)
                                                    : ftitle)
                            + "; "
                            + Utility.hex(
                                    fupper.equals(base)
                                            ? chstr
                                            : denormalize
                                                    ? Default.nfd().normalize(fupper)
                                                    : fupper)
                            + "; # "
                            + Default.ucd().getName(ch);

            // special exclusions
            if (isExcluded(ch)) {
                log.println("# " + mapping);
            } else {
                int x = ch;
                if (ch == 0x01F0) {
                    x = 0x03B1; // HACK to reorder the same
                }
                sorted.put((order << 24) | x, mapping);
            }
        }
        log.close();

        System.out.println("Writing");
        // String newFile = "DerivedData/SpecialCasing" + suffix2 +
        // UnicodeDataFile.getFileSuffix(true);
        // PrintWriter out = Utility.openPrintWriter(newFile, Utility.LATIN1_UNIX);

        final UnicodeDataFile udf =
                UnicodeDataFile.openAndWriteHeader(
                                "UCD/" + Default.ucd().getVersion() + '/',
                                "SpecialCasing" + suffix2)
                        .setSkipCopyright(Settings.SKIP_COPYRIGHT);
        final PrintWriter out = udf.out;

        /*       String[] batName = {""};
        String mostRecent = UnicodeDataFile.generateBat("DerivedData/", "SpecialCasing", suffix2 + UnicodeDataFile.getFileSuffix(true), batName);
        out.println("# SpecialCasing" + UnicodeDataFile.getFileSuffix(false));
        out.println(UnicodeDataFile.generateDateLine());
        out.println("#");
         */
        // Utility.appendFile("org/unicode/text/UCD/SpecialCasingHeader.txt", Utility.UTF8, out);

        final Iterator<Integer> it = sorted.keySet().iterator();
        int lastOrder = -1;
        while (it.hasNext()) {
            final Integer key = it.next();
            final String line = sorted.get(key);
            final int order = key.intValue() >> 24;
            if (order != lastOrder) {
                lastOrder = order;
                out.println();
                boolean skipLine = false;
                switch (order) {
                    case 1:
                        out.println("# The German es-zed is special--the normal mapping is to SS.");
                        out.println(
                                "# Note: the titlecase should never occur in practice. It is equal to titlecase(uppercase(<es-zed>))");
                        break;
                    case 2:
                        out.println(
                                "# Preserve canonical equivalence for I with dot. Turkic is handled below.");
                        break;
                    case 3:
                        out.println("# Ligatures");
                        break;
                    case 4:
                        skipLine = true;
                        break;
                    case 5:
                        out.println("# No corresponding uppercase precomposed character");
                        break;
                    case 6:
                        Utility.appendFile(
                                Settings.SRC_UCD_DIR + "SpecialCasingIota.txt", Utility.UTF8, out);
                        break;
                    case 7:
                        out.println(
                                "# Some characters with YPOGEGRAMMENI also have no corresponding titlecases");
                        break;
                    case 8:
                        skipLine = true;
                        break;
                }
                if (!skipLine) {
                    out.println();
                }
            }
            out.println(line);
        }
        Utility.appendFile(Settings.SRC_UCD_DIR + "SpecialCasingFooter.txt", Utility.UTF8, out);
        udf.close();
        // Utility.renameIdentical(mostRecent, Utility.getOutputName(newFile), batName[0]);
    }
}
