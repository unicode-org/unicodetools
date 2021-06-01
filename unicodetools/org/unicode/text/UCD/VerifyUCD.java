/**
 *******************************************************************************
 * Copyright (C) 1996-2001, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /home/cvsroot/unicodetools/org/unicode/text/UCD/VerifyUCD.java,v $
 *
 *******************************************************************************
 */

package org.unicode.text.UCD;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.BitSet;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.unicode.text.utility.ChainException;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.UTF32;
import org.unicode.text.utility.Utility;

import com.ibm.icu.text.CanonicalIterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public class VerifyUCD implements UCD_Types {
    static final boolean DEBUG = false;

    static void checkDecompFolding() {

        final UnicodeSet sum = new UnicodeSet();
        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            Utility.dot(cp);
            if (!Default.ucd().isAllocated(cp)) {
                continue;
            }
            final byte cat = Default.ucd().getCategory(cp);
            if (cat == UNASSIGNED || cat == PRIVATE_USE) {
                continue;
            }
            final String decomp = Default.nfd().normalize(cp);
            final String foldDecomp = Default.ucd().getCase(decomp, FULL, FOLD);
            final int d0 = Default.ucd().getCombiningClass(decomp.charAt(0));
            final int dL = Default.ucd().getCombiningClass(decomp.charAt(decomp.length()-1));
            final int f0 = Default.ucd().getCombiningClass(foldDecomp.charAt(0));
            final int fL = Default.ucd().getCombiningClass(foldDecomp.charAt(decomp.length()-1));
            if (d0 != f0 || dL != fL) {
                Utility.fixDot();
                System.out.println();
                System.out.println("Exception: " + Default.ucd().getCodeAndName(cp));
                System.out.println("Decomp: " + Default.ucd().getCodeAndName(decomp));
                System.out.println("FoldedDecomp: " + Default.ucd().getCodeAndName(foldDecomp));
                System.out.println("d0: " + d0 + ", "
                        + "dL: " + dL + ", "
                        + "f0: " + f0 + ", "
                        + "fL: " + fL
                        );
                sum.add(cp);
            }
        }
        System.out.println("Set: " + sum.toPattern(true));
    }

    static void oneTime() {

        final int[] testSet = {0x10000, 'a', 0xE0000, '\u0221'}; // 10000
        for (final int item : testSet) {
            System.out.println(Default.ucd().getCode(item));

            boolean ass = Default.ucd().isAssigned(item);
            System.out.println(ass ? " assigned" : " unassigned");
            ass = Default.ucd().isAllocated(item);
            System.out.println(ass ? " allocated" : " unallocated");

            String name = Default.ucd().getName(item, SHORT);
            System.out.println(" " + name);
            name = Default.ucd().getName(item);
            System.out.println(" " + name);

            System.out.println();
        }
    }

    static final byte NC = UNUSED_CATEGORY;

    static final NumberFormat format = NumberFormat.getInstance();
    static {
        format.setMinimumFractionDigits(0);
        format.setGroupingUsed(true);
    }

    static abstract class SimpleProp {
        abstract String getTitle();
        abstract short getUnallocatedProp();
        abstract short getProp(int cp);
        abstract String getName(short prop);
        abstract String getCode(short prop);

        byte[] subtotalBreaks = null;

        byte[] cumulativeTotalBreaks = null;

        byte[] permute = null;

        short getPermutation(short prop) {
            if (permute == null) {
                return prop;
            }
            if (prop >= permute.length) {
                return prop;
            }
            return permute[prop];
        }

        boolean doTotal(short prop, boolean sub) {
            final byte[] myBreak = sub ? subtotalBreaks : cumulativeTotalBreaks;
            if (myBreak == null) {
                return false;
            }
            for (final byte element : myBreak) {
                if (element == prop) {
                    return true;
                }
            }
            return false;
        }
    }

    static class CatProp extends SimpleProp {
        @Override
        String getTitle() {
            return "General Category";
        }
        @Override
        short getUnallocatedProp() {
            return Cn;
        }

        @Override
        short getProp(int cp) {
            final byte cat = Default.ucd().getCategory(cp);
            if (cat == Cn && Default.ucd().getBinaryProperty(cp, Noncharacter_Code_Point)) {
                return NC;
            }
            return cat;
        }
        @Override
        String getCode(short prop) {
            if (prop >= LIMIT_CATEGORY) {
                return "???" + prop;
            }
            if (prop == NC) {
                return "NC";
            }
            Default.ucd();
            return UCD.getCategoryID_fromIndex(prop);
        }
        @Override
        String getName(short prop) {
            if (prop >= LIMIT_CATEGORY) {
                return "???" + prop;
            }
            if (prop == NC) {
                return "Noncharacter";
            }
            Default.ucd();
            String name = UCD.getCategoryID_fromIndex(prop, LONG);
            if (prop == Cn) {
                name += " - NC";
            }
            return name;
        }

        {
            permute = new byte[] {
                    Lu, Ll, Lt, Lo, Lm,
                    Mn, Me, Mc,
                    Nd, Nl, No,
                    Pd, Pc, Ps, Pi, Pe, Pf, Po,
                    Sc, Sm, Sk, So,
                    Zs, Zl, Zp,
                    Cc, Cf, Co, Cs, NC, Cn};

            subtotalBreaks = new byte[] {Lm, Mc, No, Po, So, Zp, Cs, Cn};

            cumulativeTotalBreaks = new byte[] {Cf};
        }
    }

    static class ScriptProp extends SimpleProp {
        @Override
        String getTitle() {
            return "Script";
        }
        @Override
        short getUnallocatedProp() {
            return COMMON_SCRIPT;
        }

        @Override
        short getProp(int cp) {
            return Default.ucd().getScript(cp);
        }
        @Override
        String getCode(short prop) {
            if (prop >= LIMIT_SCRIPT) {
                return "???" + prop;
            }
            Default.ucd();
            return UCD.getScriptID_fromIndex(prop, SHORT);
        }
        @Override
        String getName(short prop) {
            if (prop >= LIMIT_SCRIPT) {
                return "???" + prop;
            }
            Default.ucd();
            return UCD.getScriptID_fromIndex(prop, LONG);
        }
        @Override
        short getPermutation(short prop) {
            if (prop == LIMIT_SCRIPT-1) {
                return COMMON_SCRIPT;
            }
            if (prop == LIMIT_SCRIPT-2) {
                return INHERITED_SCRIPT;
            }
            if (prop >= LIMIT_SCRIPT) {
                return prop;
            }
            if (prop >= INHERITED_SCRIPT-1) {
                return (byte)(prop+2);
            }
            return (byte)(prop+1);
        }
        {
            cumulativeTotalBreaks = new byte[] {TAGBANWA_SCRIPT};
        }
    }

    static SimpleProp CAT_PROP = new CatProp();
    static SimpleProp SCRIPT_PROP = new ScriptProp();

    public static void statistics() throws IOException {
        statistics(CAT_PROP);
        System.out.println("<p>");
        statistics(SCRIPT_PROP);
    }

    static final int PROPMAX = 200;

    public static void statistics(SimpleProp prop) throws IOException {
        final int[][] count = new int[PROPMAX][5];
        final int[][] sample = new int[PROPMAX][5];
        final int[] subtotalCount = new int[5];
        final int[] totalCount = new int[5];



        short cat;
        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            Utility.dot(cp);
            if (!Default.ucd().isAllocated(cp)) {
                cat = prop.getUnallocatedProp();
                setSample(count[cat], sample[cat], 0, cp);
                continue;
            }
            cat = prop.getProp(cp);
            setSample(count[cat], sample[cat], 0, cp);

            if (checkNormalizer(Default.nfd(), cp)) {
                setSample(count[cat], sample[cat], NFD+1, cp);
            }
            if (checkNormalizer(Default.nfc(), cp)) {
                setSample(count[cat], sample[cat], NFC+1, cp);
            }
            if (checkNormalizer(Default.nfkd(), cp)) {
                setSample(count[cat], sample[cat], NFKD+1, cp);
            }
            if (checkNormalizer(Default.nfkc(), cp)) {
                setSample(count[cat], sample[cat], NFKC+1, cp);
            }

        }

        Utility.fixDot();

        System.out.println("<table border='1' cellspacing='0' cellpadding='4'>");
        System.out.print("<tr><th class='tt' colspan='2'>" + prop.getTitle() + "</th><th class='tn' colspan='2'>Count");
        for (byte j = 0; j < 4; ++j) {
            System.out.println("</th><th class='tn' colspan='2'>" + UCD_Names.NF_NAME[j]);
        }
        System.out.println("</th></tr>");

        for (short ii = 0; ii < count.length; ++ii) {
            final short i = prop.getPermutation(ii);
            // System.out.println(prop.getCode(ii) + ", " + ii + " => " + prop.getCode(i) + ", " + i);
            if (count[i][0] == 0) {
                continue;
            }

            final String code = prop.getCode(i);
            final String name = prop.getName(i);

            System.out.println(" <tr><th class='t'>" + code + "</th><th class='t'>" + name + "</th>");
            for (byte j = 0; j < 5; ++j) {
                if (count[i][j] == 0) {
                    System.out.println("<td colspan='2'> </td>");
                } else {
                    System.out.println("  <td class='n'><b>" + format.format(count[i][j]) + "</b></td>");
                    System.out.println("  <td class='s'><div title='" +
                            Default.ucd().getCodeAndName(sample[i][j]) + "'>" + quote(sample[i][j]) + "</div></td>");
                }
                subtotalCount[j] += count[i][j];
                totalCount[j] += count[i][j];
            }
            System.out.println(" </tr>");
            if (prop.doTotal(i, true)) {
                printTotals("Subtotal", subtotalCount, true);
            }
            if (prop.doTotal(i, false)) {
                printTotals("Cummulative Total", totalCount, false);
            }
        }
        printTotals("Total", totalCount, false);
        System.out.println("</table>");
    }

    static public String quote(int cp) {
        final byte cat2 = Default.ucd().getCategory(cp);
        if (cat2 == Zs || cat2 == Zp || cat2 == Zl) {
            return "&nbsp;";
        }
        if (cat2 == Cc || cat2 == Cs) {
            return "??";
        }
        if (cat2 == Mn || cat2 == Me || cat2 == Mc) {
            return "&#x25CC;&#" + cp + ";";
        }
        return "&#" + cp + ";";
    }

    static public void setSample(int[] count, int[] array, int index, int cp) {
        count[index]++;
        final int value = array[index];
        if (value == 0) {
            array[index] = cp;
        } else if (Default.ucd().isAllocated(cp)) {
            final int ncount1 = getNFCount(value, index);
            final int ncount2 = getNFCount(cp, index);
            if (ncount1 != ncount2) {
                if (ncount1 > ncount2) {
                    array[index] = cp;
                }
                return;
            }
            final short cat1 = CAT_PROP.getPermutation(CAT_PROP.getProp(value));
            final short cat2 = CAT_PROP.getPermutation(CAT_PROP.getProp(cp));
            if (cat1 > cat2) {
                array[index] = cp;
            }
        }
    }

    public static int getNFCount(int cp, int index) {
        int count = 0;
        final boolean nfc1 = checkNormalizer(Default.nfc(), cp);
        final boolean nfd1 = checkNormalizer(Default.nfd(), cp);
        final boolean nfkc1 = checkNormalizer(Default.nfkc(), cp);
        final boolean nfkd1 = checkNormalizer(Default.nfkd(), cp);
        if (nfc1) {
            count += 1;
        }
        if (nfd1) {
            count += 2;
        }
        if (nfkc1) {
            count += 4;
        }
        if (nfkd1) {
            count += 8;
        }
        return count;
    }


    public static void printTotals(String title, int[] subtotalCount, boolean zeroit) {
        System.out.println(" <tr><th class='tt' colspan='2'>" + title + "</th>");
        for (byte j = 0; j < subtotalCount.length; ++j) {
            System.out.println("  <td class='tn' colspan='2'>"
                    + (subtotalCount[j] == 0 ? "" : format.format(subtotalCount[j])) + "</td>");
            if (zeroit) {
                subtotalCount[j] = 0;
            }
        }
    }

    public static boolean checkNormalizer(Normalizer x, int cp) {
        final boolean result = !x.isNormalized(cp);
        if (false) {
            final String s = x.normalize(cp);
            final boolean sResult = !s.equals(UTF16.valueOf(cp));
            if (result != sResult) {
                System.out.println("Failure with " + x + " at " + Default.ucd().getCodeAndName(cp));
            }
        }
        return result;
    }

    public static void checkBIDI() {


        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            Utility.dot(cp);
            if (!Default.ucd().isAllocated(cp)) {
                continue;
            }

            if (Default.nfd().isNormalized(cp)) {
                continue;
            }

            final String decomp = Default.nfd().normalize(cp);
            final String comp = Default.nfc().normalize(cp);
            final String source = UTF16.valueOf(cp);

            final String bidiDecomp = getBidi(decomp, true);
            final String bidiComp = getBidi(comp, true);
            final String bidiSource = getBidi(source, true);

            if (!bidiDecomp.equals(bidiSource) || !bidiComp.equals(bidiSource)) {
                Utility.fixDot();
                System.out.println(Default.ucd().getCodeAndName(cp) + ": " + getBidi(source, false));
                System.out.println("\tNFC: " + Default.ucd().getCodeAndName(comp) + ": " + getBidi(comp, false));
                System.out.println("\tNFD: " + Default.ucd().getCodeAndName(decomp) + ": " + getBidi(decomp, false));
            }
        }
    }

    public static String getBidi(String s, boolean compact) {
        String result = "";
        byte lastBidi = -1;
        int cp;
        for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(s, i);
            byte bidi = Default.ucd().getBidiClass(cp);
            if (compact) {
                if (bidi == BIDI_NSM) {
                    if (lastBidi != -1) {
                        bidi = lastBidi;
                    }
                }
                if (bidi == lastBidi && bidi != BIDI_ES && bidi != BIDI_CS) {
                    continue;
                }
            }
            result += Default.ucd().getCase(
                    Default.ucd().getBidiClassID_fromIndex(bidi, SHORT), FULL, TITLE);
            lastBidi = bidi;
        }
        return result;
    }

    public static void verify() throws IOException {


        checkIdentical("ea=h", "dt=nar");
        checkIdentical("ea=f", "dt=wide");
        checkIdentical("gc=ps", "lb=op");
        checkIdentical("lb=sg", "gc=cs");

        /*
For LB we now have:

GC:Ps == LB:OP
GC:Nd && !(EA:F)

Try these on for size, and report any discrepancies

>GC:L& && EA:W -> LB:ID
>GC:L& && EA:A -> LB:AI
>GC:L& && EA:N -> LB:AL
>GC:L& && EA:Na -> LB:AL

plus

>LB:ID contains Ideo:T

Also, try these rules

GC:S# && EA:W -> LB:ID
GC:S# && EA:A -> LB:AI
GC:S# && EA:N -> LB:AL
GC:S# && EA:Na -> LB:AL

where S# is Sm | Sk | So

these will generate exceptions, but I need to see the list to them before I
can help you narrow these down.

>The trivial ones that I could glean from reading the TR are
>LB:SG == GC:Cs
>GC:Pi -> LB:QU
>GC:Pf -> LB:QU
>GC:Mc -> LB:CM
>GC:Me -> LB:CM
>GC:Mn -> LB:CM
>GC:Pe -> LB:CL
         */
    }

    static final void checkCase3 () {


        checkNF_AndCase("\u0130", true);
        checkNF_AndCase("\u0131", true);

        UCDProperty softdot = null;
        final CanonicalIterator cit = new CanonicalIterator("a");
        final UnicodeSet badChars = new UnicodeSet();

        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            Utility.dot(cp);
            if (!Default.ucd().isAllocated(cp)) {
                continue;
            }
            final byte cat = Default.ucd().getCategory(cp);
            // check if canonical equivalents are case-mapped to canonical equivalents
            if (cat != PRIVATE_USE && cat != SURROGATE) {
                String str = UTF16.valueOf(cp);
                if (!checkNF_AndCase(str, false)) {
                    badChars.add(cp);
                }
                //if (Default.ucd.getScript(cp) != GREEK_SCRIPT) continue;
                str += "\u0334";
                try {
                    //System.out.println("Check " + Default.ucd.getCodeAndName(str));
                    cit.setSource(str);
                    while (true) {
                        final String s = cit.next();
                        if (s == null) {
                            break;
                        }
                        if (s.equals(str))
                        {
                            continue; // don't check twice
                        }

                        //System.out.println("  Checking " + Default.ucd.getCodeAndName(s));
                        if (!checkNF_AndCase(s, false)) {
                            badChars.add(cp);
                        }
                    }
                } catch (final StringIndexOutOfBoundsException e) {
                    System.out.println("Problem with " + Default.ucd().getCodeAndName(str));
                    throw e;
                }

            }

            if (false) {
                if (softdot == null) {
                    softdot = DerivedProperty.make(Type_i, Default.ucd());
                }
                if (Default.ucd().getBinaryProperty(cp, Soft_Dotted) !=
                        softdot.hasValue(cp)) {
                    System.out.println("FAIL: " + Default.ucd().getCodeAndName(cp));
                    System.out.println("Soft_Dotted='" + Default.ucd().getBinaryPropertiesID(cp, Soft_Dotted)
                            + "', DerivedSD=" + softdot.getValue(cp) + "'");
                }
            }

        }
        System.out.println();
        Utility.showSetNames("", badChars, false, Default.ucd());
    }

    static void checkIdentical(String ubpName1, String ubpName2) {
        final UCDProperty prop1 = UnifiedBinaryProperty.make(ubpName1, Default.ucd());
        final UnicodeSet set1 = prop1.getSet();
        final UCDProperty prop2 = UnifiedBinaryProperty.make(ubpName2, Default.ucd());
        final UnicodeSet set2 = prop2.getSet();
        final UnicodeSet set1minus2 = new UnicodeSet(set1);
        set1minus2.removeAll(set2);
        final UnicodeSet set2minus1 = new UnicodeSet(set2);
        set2minus1.removeAll(set1);

        if (set1minus2.isEmpty() && set2minus1.isEmpty()) {
            System.out.println("PASS: " + prop1.getFullName(LONG) + " == " + prop2.getFullName(LONG));
            System.out.println();
            return;
        }
        System.out.println("FAIL: " + prop1.getFullName(LONG) + " != " + prop2.getFullName(LONG));
        if (!set1minus2.isEmpty()) {
            System.out.println(" In " + prop1.getFullName(LONG) + " but not " + prop2.getFullName(LONG));
            Utility.showSetNames("  " + prop1.getFullName(SHORT) + ": ", set1minus2, false, Default.ucd());
        }
        if (!set2minus1.isEmpty()) {
            System.out.println(" In " + prop2.getFullName(LONG) + " but not " + prop1.getFullName(LONG));
            Utility.showSetNames("  " + prop2.getFullName(SHORT) + ": ", set2minus1, false, Default.ucd());
        }
        System.out.println();
    }

    static boolean checkNF_AndCase(String source, boolean both) {
        boolean result = true;
        final String decomp = Default.nfd().normalize(source);
        if (!decomp.equals(source)) {

            result &= checkNFC("Lower", source, decomp, Default.ucd().getCase(source, FULL, LOWER), Default.ucd().getCase(decomp, FULL, LOWER));
            result &= checkNFC("Upper", source, decomp, Default.ucd().getCase(source, FULL, UPPER), Default.ucd().getCase(decomp, FULL, UPPER));
            result &= checkNFC("Title", source, decomp, Default.ucd().getCase(source, FULL, TITLE), Default.ucd().getCase(decomp, FULL, TITLE));
            result &= checkNFC("Fold", source, decomp, Default.ucd().getCase(source, FULL, FOLD), Default.ucd().getCase(decomp, FULL, FOLD));

            if (!both) {
                return result;
            }

            result &= checkNFC("SLower", source, decomp, Default.ucd().getCase(source, SIMPLE, LOWER), Default.ucd().getCase(decomp, SIMPLE, LOWER));
            result &= checkNFC("SUpper", source, decomp, Default.ucd().getCase(source, SIMPLE, UPPER), Default.ucd().getCase(decomp, SIMPLE, UPPER));
            result &= checkNFC("STitle", source, decomp, Default.ucd().getCase(source, SIMPLE, TITLE), Default.ucd().getCase(decomp, SIMPLE, TITLE));
            result &= checkNFC("SFold", source, decomp, Default.ucd().getCase(source, SIMPLE, TITLE), Default.ucd().getCase(decomp, SIMPLE, TITLE));
        }
        return result;
    }

    static final boolean SHOW_NFC_DIFFERENCE = false;

    static boolean checkNFC(String label, String source, String decomp, String casedCp, String casedDecomp) {
        if (!Default.nfd().normalize(casedCp).equals(Default.nfd().normalize(casedDecomp))) {
            if (SHOW_NFC_DIFFERENCE) {
                Utility.fixDot();
                System.out.println("FAIL CASE CE: " + label + " (" + Default.ucd().getCodeAndName(source) + ")");
                System.out.println("\t" + Default.ucd().getCode(source) + " => " + Default.ucd().getCode(casedCp));
                System.out.println("\t" + Default.ucd().getCode(decomp) + " => " + Default.ucd().getCode(casedDecomp));
            }
            return false;
        }
        return true;
    }

    

    /*
        System.out.println(Default.ucd.toString(0x0387));
        System.out.println(Default.ucd.toString(0x00B7));
        System.out.println(Default.ucd.toString(0x03a3));
        System.out.println(Default.ucd.toString(0x03c2));
        System.out.println(Default.ucd.toString(0x03c3));
        System.out.println(Default.ucd.toString(0x0069));
        System.out.println(Default.ucd.toString(0x0130));
        System.out.println(Default.ucd.toString(0x0131));
        System.out.println(Default.ucd.toString(0x0345));
     */

    static void checkAgainstOtherVersion(String otherVersion) {

        final UCD ucd2 = UCD.make(otherVersion);
        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            final UData curr = Default.ucd().get(cp, true);
            final UData other = ucd2.get(cp, true);
            if (!curr.equals(other)) {
                System.out.println("Difference at " + Default.ucd().getCodeAndName(cp));
                System.out.println(curr);
                System.out.println(curr);
                System.out.println();
            }
        }
    }

    static void generateXML() throws IOException {

        final String filename = "UCD.xml";
        final PrintWriter log = Utility.openPrintWriterGenDir("log/" + filename, Utility.LATIN1_UNIX);

        //log.println('\uFEFF');
        log.println("<ucd>");

        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            Utility.dot(cp);
            if (!Default.ucd().isRepresented(cp)) {
                continue;
            }
            if (cp == 0xE0026 || cp == 0x20000) {
                System.out.println("debug");
            }
            log.println(Default.ucd().toString(cp));
        }

        log.println("</ucd>");
        log.close();
    }

    static final byte MIXED = (byte)(UNCASED + 1);

    public static void checkCase() throws IOException {

        Utility.fixDot();
        System.out.println("checkCase");

        final String test = "The qui'ck br\u2019own 'fox jum\u00ADped ov\u200Ber th\u200Ce lazy dog.";

        final String ttest = Default.ucd().getCase(test, FULL, TITLE);

        final PrintWriter titleTest = Utility.openPrintWriterGenDir("log/TestTitle.txt", Utility.LATIN1_UNIX);
        titleTest.println(test);
        titleTest.println(ttest);
        titleTest.close();

        System.out.println(Default.ucd().getCase("ABC,DE'F G\u0308H", FULL, TITLE));
        final String fileName = "CaseDifferences.txt";
        final PrintWriter log = Utility.openPrintWriterGenDir("log/" + fileName, Utility.LATIN1_UNIX);

        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            Utility.dot(cp);
            if (!Default.ucd().isRepresented(cp) || Default.ucd().isPUA(cp)) {
                continue;
            }
            if (cp == '\u3371') {
                System.out.println("debug");
            }
            final String x = Default.nfkd().normalize(cp);
            final String xu = Default.ucd().getCase(x, FULL, UPPER);
            final String xl = Default.ucd().getCase(x, FULL, LOWER);
            final String xt = Default.ucd().getCase(x, FULL, TITLE);

            byte caseCat = MIXED;
            if (xu.equals(xl)) {
                caseCat = UNCASED;
            } else if (x.equals(xl)) {
                caseCat = LOWER;
            } else if (x.equals(xu)) {
                caseCat = UPPER;
            } else if (x.equals(xt)) {
                caseCat = TITLE;
            }

            final byte cat = Default.ucd().getCategory(cp);
            final boolean otherLower = Default.ucd().getBinaryProperty(cp, Other_Lowercase);
            final boolean otherUpper = Default.ucd().getBinaryProperty(cp, Other_Uppercase);
            final byte oldCaseCat = (cat == Lu || otherUpper) ? UPPER
                    : (cat == Ll || otherLower) ? LOWER
                            : (cat == Lt) ? TITLE
                                    : UNCASED;

            if (caseCat != oldCaseCat) {
                log.println(UTF32.valueOf32(cp)
                        + "\t" + names[caseCat]
                                + "\t" + names[oldCaseCat]
                                        + "\t" + Default.ucd().getCategoryID_fromIndex(cat)
                                        + "\t" + lowerNames[otherLower ? 1 : 0]
                                                + "\t" + upperNames[otherUpper ? 1 : 0]
                                                        + "\t" + Default.ucd().getCodeAndName(cp)
                                                        + "\t" + Default.ucd().getCodeAndName(x)
                                                        + "\t" + Default.ucd().getCodeAndName(xu)
                                                        + "\t" + Default.ucd().getCodeAndName(xl)
                                                        + "\t" + Default.ucd().getCodeAndName(xt)
                        );
            }
        }

        log.close();
    }

    public static void checkCase2(boolean longForm) throws IOException {

        Utility.fixDot();
        System.out.println("checkCase");

        /*String tx1 = "\u0391\u0342\u0345";
        String ux1 = "\u0391\u0342\u0399";
        String ctx1 = nfc.normalize(tx1);
        String ctx2 = nfc.normalize(ux1); // wrong??

        //System.out.println(Default.ucd.getCase("ABC,DE'F G\u0308H", FULL, TITLE));
         */


        final String fileName = "CaseNormalizationDifferences.txt";
        final PrintWriter log = Utility.openPrintWriterGenDir("log/" + fileName, Utility.LATIN1_UNIX);

        log.println("Differences between case(normalize(cp)) and normalize(case(cp))");
        log.println("u, l, t - upper, lower, title");
        log.println("c, d - nfc, nfd");

        //Utility.DOTMASK = 0x7F;

        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            Utility.dot(cp);
            if (!Default.ucd().isRepresented(cp) || Default.ucd().isPUA(cp)) {
                continue;
            }
            if (cp == '\u0130') {
                System.out.println("debug");
            }

            final String x = UTF32.valueOf32(cp);
            final String dx = Default.nfd().normalize(cp);
            final String cx = Default.nfc().normalize(cp);

            final String ux = Default.ucd().getCase(x, FULL, UPPER);
            final String lx = Default.ucd().getCase(x, FULL, LOWER);
            final String tx = Default.ucd().getCase(x, FULL, TITLE);

            if (x.equals(dx) && dx.equals(cx) && cx.equals(ux) && ux.equals(lx) && lx.equals(tx)) {
                continue;
            }

            final String cux = Default.nfc().normalize(ux);
            final String clx = Default.nfc().normalize(lx);
            final String ctx = Default.nfc().normalize(tx);

            if (x.equals(cx)) {
                boolean needBreak = false;
                if (!clx.equals(lx)) {
                    needBreak = true;
                }
                if (!ctx.equals(tx)) {
                    needBreak = true;
                }
                if (!cux.equals(ux)) {
                    needBreak = true;
                }

                if (needBreak) {
                    log.println("# Was not NFC:");
                    log.println(
                            "## " + Utility.hex(x) + "; "
                                    + Utility.hex(lx) + "; "
                                    + Utility.hex(tx) + "; "
                                    + Utility.hex(ux) + "; # "
                                    + Default.ucd().getName(x));
                    log.println("#   should be:");
                    log.println(
                            Utility.hex(x) + "; "
                                    + Utility.hex(clx) + "; "
                                    + Utility.hex(ctx) + "; "
                                    + Utility.hex(cux) + "; # "
                                    + Default.ucd().getName(x));
                    log.println();
                }
            }

            final String dux = Default.nfd().normalize(ux);
            final String dlx = Default.nfd().normalize(lx);
            final String dtx = Default.nfd().normalize(tx);



            final String startdx = getMarks(dx, false);
            final String enddx = getMarks(dx, true);

            final String startdux = getMarks(dux, false);
            final String enddux = getMarks(dux, true);

            final String startdtx = getMarks(dtx, false);
            final String enddtx = getMarks(dtx, true);

            final String startdlx = getMarks(dlx, false);
            final String enddlx = getMarks(dlx, true);

            // If the new marks don't occur in the old decomposition, we got a problem!

            if (!startdx.startsWith(startdux) || !startdx.startsWith(startdtx) || !startdx.startsWith(startdlx)
                    || !enddx.endsWith(enddux) || !enddx.endsWith(enddtx) || !enddx.endsWith(enddlx)) {
                log.println("Combining Class Difference for " + Default.ucd().getCodeAndName(x));
                log.println("x:  " + Default.ucd().getCodeAndName(dx) + ", " + Utility.hex(startdx) + ", " + Utility.hex(enddx));
                log.println("ux: " + Default.ucd().getCodeAndName(dux) + ", " + Utility.hex(startdux) + ", " + Utility.hex(enddux));
                log.println("tx: " + Default.ucd().getCodeAndName(dtx) + ", " + Utility.hex(startdtx) + ", " + Utility.hex(enddtx));
                log.println("lx: " + Default.ucd().getCodeAndName(dlx) + ", " + Utility.hex(startdlx) + ", " + Utility.hex(enddlx));
                log.println();
            }


            if (!longForm) {
                continue;
            }

            final String udx = Default.ucd().getCase(dx, FULL, UPPER);
            final String ldx = Default.ucd().getCase(dx, FULL, LOWER);
            final String tdx = Default.ucd().getCase(dx, FULL, TITLE);

            final String ucx = Default.ucd().getCase(cx, FULL, UPPER);
            final String lcx = Default.ucd().getCase(cx, FULL, LOWER);
            final String tcx = Default.ucd().getCase(cx, FULL, TITLE);

            final String dudx = Default.nfd().normalize(udx);
            final String dldx = Default.nfd().normalize(ldx);
            final String dtdx = Default.nfd().normalize(tdx);

            final String cucx = Default.nfc().normalize(ucx);
            final String clcx = Default.nfc().normalize(lcx);
            final String ctcx = Default.nfc().normalize(tcx);


            if (!dux.equals(udx)
                    || !dlx.equals(ldx)
                    || !dtx.equals(tdx)
                    || !cux.equals(ucx)
                    || !clx.equals(lcx)
                    || !ctx.equals(tcx)
                    || !dux.equals(dudx)
                    || !dlx.equals(dldx)
                    || !dtx.equals(dtdx)
                    || !cux.equals(cucx)
                    || !clx.equals(clcx)
                    || !ctx.equals(ctcx)
                    ) {
                log.println();
                log.println("Difference at " + Default.ucd().getCodeAndName(cp));
                if (!x.equals(ux)) {
                    log.println("\tu(cp):\t" + Default.ucd().getCodeAndName(ux));
                }
                if (!x.equals(lx)) {
                    log.println("\tl(cp):\t" + Default.ucd().getCodeAndName(lx));
                }
                if (!tx.equals(ux)) {
                    log.println("\tt(cp):\t" + Default.ucd().getCodeAndName(tx));
                }
                if (!x.equals(dx)) {
                    log.println("\td(cp):\t" + Default.ucd().getCodeAndName(dx));
                }
                if (!x.equals(cx)) {
                    log.println("\tc(cp):\t" + Default.ucd().getCodeAndName(cx));
                }

                if (!dux.equals(udx)) {
                    log.println();
                    log.println("\td(u(cp)):\t" + Default.ucd().getCodeAndName(dux));
                    log.println("\tu(d(cp)):\t" + Default.ucd().getCodeAndName(udx));
                }
                if (!dlx.equals(ldx)) {
                    log.println();
                    log.println("\td(l(cp)):\t" + Default.ucd().getCodeAndName(dlx));
                    log.println("\tl(d(cp)):\t" + Default.ucd().getCodeAndName(ldx));
                }
                if (!dtx.equals(tdx)) {
                    log.println();
                    log.println("\td(t(cp)):\t" + Default.ucd().getCodeAndName(dtx));
                    log.println("\tt(d(cp)):\t" + Default.ucd().getCodeAndName(tdx));
                }

                if (!cux.equals(ucx)) {
                    log.println();
                    log.println("\tc(u(cp)):\t" + Default.ucd().getCodeAndName(cux));
                    log.println("\tu(c(cp)):\t" + Default.ucd().getCodeAndName(ucx));
                }
                if (!clx.equals(lcx)) {
                    log.println();
                    log.println("\tc(l(cp)):\t" + Default.ucd().getCodeAndName(clx));
                    log.println("\tl(c(cp)):\t" + Default.ucd().getCodeAndName(lcx));
                }
                if (!ctx.equals(tcx)) {
                    log.println();
                    log.println("\tc(t(cp)):\t" + Default.ucd().getCodeAndName(ctx));
                    log.println("\tt(c(cp)):\t" + Default.ucd().getCodeAndName(tcx));
                }

                // ...........

                if (!udx.equals(dudx)) {
                    log.println();
                    log.println("\tu(d(cp)):\t" + Default.ucd().getCodeAndName(udx));
                    log.println("\td(u(d(cp))):\t" + Default.ucd().getCodeAndName(dudx));
                }
                if (!ldx.equals(dldx)) {
                    log.println();
                    log.println("\tl(d(cp)):\t" + Default.ucd().getCodeAndName(ldx));
                    log.println("\td(l(d(cp))):\t" + Default.ucd().getCodeAndName(dldx));
                }
                if (!tdx.equals(dtdx)) {
                    log.println();
                    log.println("\tt(d(cp)):\t" + Default.ucd().getCodeAndName(tdx));
                    log.println("\td(t(d(cp))):\t" + Default.ucd().getCodeAndName(dtdx));
                }

                if (!ucx.equals(cucx)) {
                    log.println();
                    log.println("\tu(c(cp)):\t" + Default.ucd().getCodeAndName(ucx));
                    log.println("\tc(u(c(cp))):\t" + Default.ucd().getCodeAndName(cucx));
                }
                if (!lcx.equals(clcx)) {
                    log.println();
                    log.println("\tl(c(cp)):\t" + Default.ucd().getCodeAndName(lcx));
                    log.println("\tc(l(c(cp))):\t" + Default.ucd().getCodeAndName(clcx));
                }
                if (!tcx.equals(ctcx)) {
                    log.println();
                    log.println("\tt(c(cp)):\t" + Default.ucd().getCodeAndName(tcx));
                    log.println("\tc(t(c(cp))):\t" + Default.ucd().getCodeAndName(ctcx));
                }
            }
        }

        log.close();
    }

    public static String getMarks(String s, boolean doEnd) {
        int cp;
        if (!doEnd) {
            for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
                cp = UTF16.charAt(s, i);
                final int cc = Default.ucd().getCombiningClass(cp);
                if (cc == 0) {
                    return s.substring(0, i);
                }
            }
        } else {
            for (int i = s.length(); i > 0; i -= UTF16.getCharCount(cp)) {
                cp = UTF16.charAt(s, i-1); // will go 2 before if necessary
                final int cc = Default.ucd().getCombiningClass(cp);
                if (cc == 0) {
                    return s.substring(i);
                }
            }
        }
        return s;
    }

    static final String names[] = {"LOWER", "TITLE", "UPPER", "(UNC)", "MIXED"};
    static final String names2[] = {"LOWER", "TITLE", "UPPER", "FOLD"};
    static final String lowerNames[] = {"", "Other_Lower"};
    static final String upperNames[] = {"", "Other_Upper"};

    public static void CheckCaseFold() {

        System.out.println("Checking Case Fold");
        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            Utility.dot(cp);
            if (!Default.ucd().isAssigned(cp) || Default.ucd().isPUA(cp)) {
                continue;
            }

            boolean failed = false;
            final String fullTest = Default.ucd().getCase(Default.ucd().getCase(cp, FULL, UPPER), FULL, LOWER);
            final String simpleTest = Default.ucd().getCase(Default.ucd().getCase(cp, SIMPLE, UPPER), SIMPLE, LOWER);

            final String full = Default.ucd().getCase(cp, FULL, FOLD);
            final String simple = Default.ucd().getCase(cp, SIMPLE, FOLD);

            final String realTest = "\u0360" + UTF16.valueOf(cp) + "\u0334";

            final int ccc = Default.ucd().getCombiningClass(cp);

            for (byte style = FOLD; style < LIMIT_CASE; ++style) {

                final String fold_NFD = Default.nfd().normalize(Default.ucd().getCase(realTest, FULL, style));
                final String NFD_fold = Default.ucd().getCase(Default.nfd().normalize(realTest), FULL, style);
                if (!fold_NFD.equals(NFD_fold)) {
                    Utility.fixDot();
                    System.out.println("Case check fails at " + Default.ucd().getCodeAndName(cp));
                    System.out.println("\t" + names2[style] + ", then NFD: " + Default.ucd().getCodeAndName(fold_NFD));
                    System.out.println("\tNFD, then " + names2[style] + ": " + Default.ucd().getCodeAndName(NFD_fold));
                    failed = true;
                }
            }

            /*

            int ccc = Default.ucd.getCombiningClass(cp);

            int cp2;
            for (int i = 0; i < full.length(); i += UTF16.getCharCount(cp2)) {
                cp2 = UTF16.charAt(full, i);
                int ccc2 = Default.ucd.getCombiningClass(cp2);
                if (ccc2 != ccc) {
                    System.out.println("Case fold CCC fails at " + Default.ucd.getCodeAndName(cp));
                    System.out.println("\tFull case folding:" + ccc2 + ", " + Default.ucd.getCodeAndName(full));
                    System.out.println("\tccc:" + ccc);
                    System.out.println("\tccc:" + ccc2 + ", " + Default.ucd.getCodeAndName(cp2));
                    failed = true;
                }
            }

             */

            if (!full.equals(fullTest)) {
                Utility.fixDot();
                System.out.println("Case fold fails at " + Default.ucd().getCodeAndName(cp));
                System.out.println("  fullFold(ch):             " + Default.ucd().getCodeAndName(full));
                System.out.println("  fullUpper(fullLower(ch)): " + Default.ucd().getCodeAndName(fullTest));
                failed = true;
            }
            if (!simple.equals(simpleTest)) {
                Utility.fixDot();
                if (!failed) {
                    System.out.println("Case fold fails at " + Default.ucd().getCodeAndName(cp));
                }
                System.out.println("  simpleFold(ch):               " + Default.ucd().getCodeAndName(simple));
                System.out.println("  simpleUpper(simpleLower(ch)): " + Default.ucd().getCodeAndName(simpleTest));
                failed = true;
            }
            if (failed) {
                System.out.println();
            }
        }
    }

    public static void compareBlueberry() {


        final UnicodeSet NameStartChar = new UnicodeSet("[A-Z:_a-z\\u00C0-\\u02FF"
                + "\\u0370-\\u037D\\u037F-\\u2027\\u202A-\\u218F\\u2800-\\uD7FF"
                + "\\uE000-\\uFDCF\\uFDE0-\\uFFEF\\U00010000-\\U0010FFFF]");
        System.out.println("NameStartChar:");
        System.out.println("\t" + NameStartChar.toPattern(true));

        final UnicodeSet NameChar = new UnicodeSet("[-.0-9\\u00b7\\u0300-\\u036F]");
        System.out.println("NameChar-:");
        System.out.println("\t" + NameChar.toPattern(true));
        NameChar.addAll(NameStartChar);
        System.out.println("NameChar:");
        System.out.println("\t" + NameChar.toPattern(true));

        final UCDProperty IDstart = DerivedProperty.make(Mod_ID_Start, Default.ucd());
        final UCDProperty IDcontinue = DerivedProperty.make(Mod_ID_Continue_NO_Cf, Default.ucd());

        final UnicodeSet IDContinueMinusNameChar = new UnicodeSet();
        final UnicodeSet IDStartMinusNameChar = new UnicodeSet();
        final UnicodeSet IDStartMinusNameStartChar = new UnicodeSet();
        final UnicodeSet UnassignedMinusNameChar = new UnicodeSet();

        for (int cp = 0; cp < 0x10FFFF; ++cp) {
            Utility.dot(cp);

            if (Default.ucd().isPUA(cp)) {
                continue;
            }
            if (!Default.ucd().isAssigned(cp) && !NameChar.contains(cp)) {
                UnassignedMinusNameChar.add(cp);
            } else if (IDcontinue.hasValue(cp) && !NameChar.contains(cp)) {
                IDContinueMinusNameChar.add(cp);
            } else if (IDstart.hasValue(cp)) {
                if (!NameChar.contains(cp)) {
                    IDStartMinusNameChar.add(cp);
                } else if (!NameStartChar.contains(cp)) {
                    IDStartMinusNameStartChar.add(cp);
                }
            }
        }
        System.out.println("IDContinueMinusNameChar: ");
        System.out.println("\t" + IDContinueMinusNameChar.toPattern(true));
        Utility.showSetNames("\t", IDContinueMinusNameChar, false, Default.ucd());
        System.out.println("IDStartMinusNameChar: ");
        System.out.println("\t" + IDStartMinusNameChar.toPattern(true));
        System.out.println("IDStartMinusNameStartChar: ");
        System.out.println("\t" + IDStartMinusNameStartChar.toPattern(true));
        System.out.println("UnassignedMinusNameChar: ");
        System.out.println("\t" + UnassignedMinusNameChar.toPattern(true));
    }

    public static void VerifyIDN() throws IOException {

        System.out.println("VerifyIDN");

        System.out.println();
        System.out.println("Checking Map");
        System.out.println();

        final BitSet mappedOut = new BitSet();
        int errorCount = verifyUTFMap(mappedOut);

        final BitSet unassigned = getIDNList("IDN-Unassigned.txt");
        final BitSet prohibited = getIDNList("IDN-Prohibited.txt");
        final BitSet guessSet = guessIDN();

        System.out.println();
        System.out.println("Checking Prohibited and Unassigned");
        System.out.println();
        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            Utility.dot(cp);
            if (mappedOut.get(cp)) {
                continue;
            }

            final boolean ucdUnassigned = !Default.ucd().isAllocated(cp);
            final boolean idnUnassigned = unassigned.get(cp);
            final boolean guess = guessSet.get(cp);
            final boolean idnProhibited = prohibited.get(cp);

            if (ucdUnassigned && !idnUnassigned) {
                showError("?UCD Unassigned but not IDN Unassigned", cp, "");
                ++errorCount;
            } else if (!ucdUnassigned && idnUnassigned) {
                showError("?Not UCD Unassigned but IDN Unassigned", cp, "");
                ++errorCount;
            }

            if (idnProhibited && unassigned.get(cp)) {
                showError("?Both IDN Unassigned AND IDN Prohibited", cp, "");
                ++errorCount;
            }

            if (guess && !idnProhibited) {
                showError("?UCD ?prohibited? but not IDN Prohibited ", cp, "");
                ++errorCount;
            } else if (!guess && idnProhibited) {
                showError("?Not UCD ?prohibited? but IDN Prohibited ", cp, "");
                ++errorCount;
            }

            if (cp == 0x3131) {
                System.out.println("Debug: " + idnProhibited
                        + ", " + idnUnassigned
                        + ", " + !Default.nfkd().isNormalized(cp)
                        + ", " + Default.ucd().getCodeAndName(Default.nfkc().normalize(cp))
                        + ", " + Default.ucd().getCodeAndName(Default.nfc().normalize(cp)));
            }

            if (!idnProhibited && ! idnUnassigned && !Default.nfkd().isNormalized(cp)) {
                final String kc = Default.nfkc().normalize(cp);
                final String c = Default.nfc().normalize(cp);
                if (kc.equals(c)) {
                    continue;
                }
                int cp2;
                boolean excluded = false;
                for (int j = 0; j < kc.length(); j += UTF16.getCharCount(cp2)) {
                    cp2 = UTF16.charAt(kc, j);
                    if (prohibited.get(cp2)) {
                        showError("Prohibited with NFKC, but output with NFC", cp, "");
                        excluded = true;
                        break;
                    }
                }
                if (!excluded) {
                    showError("Remapped to core abstract character with NFKC (but not NFC)", cp, ""); // , "\t=> " + Default.ucd.getCodeAndName(kc));
                }
            }

        }
        System.out.println("Writing IDNCheck.txt");


        final PrintWriter log = Utility.openPrintWriterGenDir("log/IDNCheck.txt", Utility.LATIN1_UNIX);
        log.println("IDN Check");
        log.println("Total Errors: " + errorCount);

        final Iterator it = idnMap.keySet().iterator();
        while (it.hasNext()) {
            final String description = (String) it.next();
            final Map map = (Map) idnMap.get(description);
            log.println();
            log.println(description);
            log.println("Total: " + map.size());
            log.println();

            final Iterator it2 = map.keySet().iterator();
            while (it2.hasNext()) {
                final Object key = it2.next();
                final String line = (String) map.get(key);
                log.println("  " + line);
            }
        }
        log.close();
    }

    static Map idnMap = new java.util.HashMap();

    static void showError(String description, int cp, String option) {
        Map probe = (Map) idnMap.get(description);
        if (probe == null) {
            probe = new TreeMap();
            idnMap.put(description, probe);
        }
        probe.put(new Integer(cp), Default.ucd().getCodeAndName(cp) + " (" + Default.ucd().getCategoryID(cp) + ")" + option);
    }

    static void showDifferences(PrintWriter log, UnicodeSet s1, String name1, UnicodeSet s2, String name2, boolean both) {
        if (!s1.equals(s2)) {
            log.println();
            log.println("In " + name1 + ", but NOT " + name2);
            Utility.showSetNames(log," ", new UnicodeSet(s1).removeAll(s2), false, false, Default.ucd());
            log.println();
            log.println("NOT in " + name1 + ", but in " + name2);
            Utility.showSetNames(log," ", new UnicodeSet(s2).removeAll(s1), false, false, Default.ucd());
            log.println();
            if (both) {
                log.println("In both " + name1 + " AND " + name2);
                Utility.showSetNames(log," ", new UnicodeSet(s2).retainAll(s1), false, false, Default.ucd());
                log.println();
            }
        }
    }


    public static void genIDN() throws IOException {
        final PrintWriter out = new PrintWriter(System.out);

        final PrintWriter log = Utility.openPrintWriterGenDir("log/IDN-tables.txt", Utility.LATIN1_UNIX);

        /*UnicodeSet y = UnifiedBinaryProperty.make(CATEGORY + FORMAT).getSet();
        UnicodeSet x = new UnicodeSet(0xE0001,0xE007F).retainAll(y);

        System.out.println("y: " + y.toPattern(true));
        System.out.println("x: " + x.toPattern(true));
        Utility.showSetNames(out, "* ", x, false, true, Default.ucd);
        out.flush();
         */


        // table1
        System.out.println("Getting Basics");
        final UnicodeSet unassigned = UnifiedBinaryProperty.make(CATEGORY + UNASSIGNED).getSet();
        System.out.print(".");
        final UnicodeSet lineSeparators = UnifiedBinaryProperty.make(CATEGORY+LINE_SEPARATOR).getSet();
        System.out.print(".");
        final UnicodeSet paraSeparators = UnifiedBinaryProperty.make(CATEGORY+PARAGRAPH_SEPARATOR).getSet();
        System.out.print(".");
        final UnicodeSet spaceSeparators = UnifiedBinaryProperty.make(CATEGORY+SPACE_SEPARATOR).getSet();
        System.out.print(".");
        final UnicodeSet noncharacters = UnifiedBinaryProperty.make(BINARY_PROPERTIES + Noncharacter_Code_Point).getSet();
        System.out.print(".");
        final UnicodeSet deprecated = UnifiedBinaryProperty.make(BINARY_PROPERTIES + Deprecated).getSet();
        System.out.print(".");
        final UnicodeSet format = UnifiedBinaryProperty.make(CATEGORY + FORMAT).getSet();
        System.out.print(".");
        final UnicodeSet bidi_control = UnifiedBinaryProperty.make(BINARY_PROPERTIES+Bidi_Control).getSet();
        System.out.print(".");
        final UnicodeSet binary_IDS = UnifiedBinaryProperty.make(BINARY_PROPERTIES+IDS_BinaryOperator).getSet();
        System.out.print(".");
        final UnicodeSet trinary_IDS = UnifiedBinaryProperty.make(BINARY_PROPERTIES+IDS_TrinaryOperator).getSet();
        System.out.print(".");
        final UnicodeSet whitespace = UnifiedBinaryProperty.make(BINARY_PROPERTIES+White_space).getSet();
        whitespace.addAll(spaceSeparators); // bug.
        System.out.print(".");

        final UnicodeSet defaultIgnorable = UnifiedBinaryProperty.make(DERIVED + DefaultIgnorable).getSet();
        System.out.print(".");

        final UnicodeSet privateUse = UnifiedBinaryProperty.make(CATEGORY+PRIVATE_USE).getSet();
        System.out.print(".");
        final UnicodeSet control = UnifiedBinaryProperty.make(CATEGORY+Cc).getSet();
        System.out.print(".");
        final UnicodeSet surrogate = UnifiedBinaryProperty.make(CATEGORY+SURROGATE).getSet();

        System.out.println("Building Sets");
        // small test:

        if (DEBUG) {
            showDifferences(log, whitespace, "White_Space",
                    new UnicodeSet(spaceSeparators).addAll(lineSeparators).addAll(paraSeparators), "Separators", true);

            showDifferences(log, UnifiedBinaryProperty.make(DERIVED + ID_Start).getSet(), "ID_Start",
                    UnifiedBinaryProperty.make(DERIVED + Mod_ID_Start).getSet(), "XID_Start", false);

            showDifferences(log, UnifiedBinaryProperty.make(DERIVED + ID_Continue_NO_Cf).getSet(), "ID_Continue",
                    UnifiedBinaryProperty.make(DERIVED + Mod_ID_Continue_NO_Cf).getSet(), "XID_Continue", false);

            System.out.println("Done with Test");
        }

        final UnicodeSet A1 = new UnicodeSet(unassigned).removeAll(noncharacters);

        // special code for B1

        /*
B1, old
00AD; SOFT HYPHEN
1806; MONGOLIAN TODO SOFT HYPHEN
180B; MONGOLIAN FREE VARIATION SELECTOR ONE
180C; MONGOLIAN FREE VARIATION SELECTOR TWO
180D; MONGOLIAN FREE VARIATION SELECTOR THREE
200B; ZERO WIDTH SPACE
200C; ZERO WIDTH NON-JOINER
200D; ZERO WIDTH JOINER
FEFF; ZERO WIDTH NO-BREAK SPACE
         */

        final UnicodeSet B1 = new UnicodeSet().add(0xAD).add(0x1806).add(0x034F); // START WITH soft hyphen, mongolian soft hyphen, grapheme joiner
        // THEN ADD default ignorables or format characters that are *variation* or *zero width*
        final UnicodeSet temp = new UnicodeSet(defaultIgnorable).addAll(format).addAll(spaceSeparators)
                .removeAll(surrogate).removeAll(control); // remove some just to avoid clutter when debugging.
        final UnicodeSetIterator it = new UnicodeSetIterator(temp);
        while(it.next()) {
            if (!Default.ucd().isAssigned(it.codepoint)) {
                continue;
            }
            final String name = Default.ucd().getName(it.codepoint);
            System.out.print(Default.ucd().getCodeAndName(it.codepoint));

            if (name.indexOf("VARIATION") >= 0 || name.indexOf("ZERO") >= 0
                    || name.indexOf("WORD JOINER") >= 0) {
                B1.add(it.codepoint);
                System.out.print("*");
            }
            System.out.println();
        }

        final UnicodeSet C1 = new UnicodeSet(whitespace).removeAll(control).removeAll(lineSeparators)
                .removeAll(paraSeparators);

        final UnicodeSet C2 = new UnicodeSet(defaultIgnorable).removeAll(unassigned).removeAll(surrogate)
                .addAll(control).addAll(format).addAll(lineSeparators).addAll(paraSeparators);

        final UnicodeSet C3 = new UnicodeSet(privateUse);

        final UnicodeSet C4 = new UnicodeSet(noncharacters);

        final UnicodeSet C5 = new UnicodeSet(surrogate);

        final UnicodeSet C6 = new UnicodeSet(0xFFF9, 0xFFFC).add(0xFFFD);

        final UnicodeSet C7 = new UnicodeSet(binary_IDS).addAll(trinary_IDS);

        final UnicodeSet C8 = new UnicodeSet(deprecated).addAll(bidi_control);

        final UnicodeSet C9 = new UnicodeSet(0xE0001,0xE007F).retainAll(format);
        //Utility.showSetNames(out, "\t&&& ", C9, false, true, Default.ucd);
        //out.flush();


        // FIX UP SETS!!
        B1.removeAll(C6);
        B1.removeAll(C8);
        B1.removeAll(C9);

        C1.removeAll(B1);

        C2.removeAll(B1);
        C2.removeAll(C6);
        C2.removeAll(C8);
        C2.removeAll(C9);

        System.out.println("Check that A1, B1, C1..9 are disjoint");

        final UnicodeSet[] test = {A1, B1, C1, C2, C3, C4, C5, C6, C7, C8, C9};
        final String[] testNames = {"A1", "B1", "C1", "C2", "C3", "C4", "C5", "C6", "C7", "C8", "C9"};
        final UnicodeSet union = new UnicodeSet();

        for (int i = 0; i < test.length; ++i) {
            union.addAll(test[i]);
            for (int j = i + 1; j < test.length; ++j) {
                if (test[i].containsNone(test[j])) {
                    continue;
                }
                log.println(testNames[i] + " and " + testNames[j] + " intersect!");
                final UnicodeSet intersection = new UnicodeSet(test[i]).retainAll(test[j]);
                Utility.showSetNames(log,"  ", intersection, false, true, Default.ucd());
                log.println();
            }
        }

        System.out.println("Check that union works");

        final UnicodeSet[] badChars = {unassigned, noncharacters, deprecated, format,
                control, surrogate, privateUse, binary_IDS, trinary_IDS, whitespace, defaultIgnorable,
                lineSeparators, paraSeparators, spaceSeparators};
        final UnicodeSet badCharUnion = new UnicodeSet();
        for (final UnicodeSet badChar : badChars) {
            badCharUnion.addAll(badChar);
        }

        showDifferences(log, union, "(A1+B1+C1-C9)",
                badCharUnion,
                "(Whitespace+Deprecated+DefaultIgnorable+Separator+Other (cont/format/surr/priv/unass))", false);

        System.out.println("Generating B2, B3");

        log.println("Generating B2, B3");
        final Map B2 = new TreeMap();
        final Map B3 = new TreeMap();
        Integer tempInteger = null;

        for (int i = 0; i < 0x10FFFF; ++i) {
            final int cat = Default.ucd().getCategory(i);
            if (!Default.ucd().isAssigned(i)) {
                continue;
            }
            //if (cat == Cc || cat == Cf || cat == Co || cat == Cn) continue; // we can skip these
            //if (Default.ucd.hasComputableName(i)) continue;
            tempInteger = null;

            final String original = UTF16.valueOf(i);
            final String caseFold = Default.ucd().getCase(i, FULL, FOLD);
            if (!original.equals(caseFold)) {
                tempInteger = new Integer(i);
                B2.put(tempInteger, caseFold);
                B3.put(tempInteger, caseFold);
            }

            final String b = Default.nfkc().normalize(caseFold);
            final String c = Default.nfkc().normalize(Default.ucd().getCase(b, FULL, FOLD));

            if (!c.equals(b)) {
                if (tempInteger != null) {
                    if (DEBUG) {
                        log.println("Possible Conflict");
                        log.println("    " + Default.ucd().getCodeAndName(i));
                        log.println(" => " + Default.ucd().getCodeAndName(caseFold));
                        log.println(" => " + Default.ucd().getCodeAndName(c));
                    }
                } else {
                    tempInteger = new Integer(i);
                    if (DEBUG) {
                        log.println("    " + Default.ucd().getCodeAndName(i));
                        log.println(" => " + Default.ucd().getCodeAndName(c));
                    }
                }
                if (DEBUG) {
                    log.println();
                }
                B2.put(tempInteger, c);
            }
        }


        // PRINTOUT

        printIDN_Table(log, "A.1", "Unassigned code points in Unicode " + Default.ucd().getVersion(), A1);
        printIDN_Table(log, "B.1", "Commonly mapped to nothing", B1);

        printIDN_Map(log, "B.2", "Mapping for lowercase used with NFKC", B2, B3);

        printIDN_Map(log, "B.3", "Mapping for lowercase used with no normalization", B3, B2);

        printIDN_Table(log, "C.1", "Space characters", C1);
        printIDN_Table(log, "C.2", "Control characters", C2);
        printIDN_Table(log, "C.3", "Private use", C3);
        printIDN_Table(log, "C.4", "Non-character code points", C4);
        printIDN_Table(log, "C.5", "Surrogate codes", C5);
        printIDN_Table(log, "C.6", "Inappropriate for plain text", C6);
        printIDN_Table(log, "C.7", "Inappropriate for canonical representation", C7);
        printIDN_Table(log, "C.8", "Change display properties (or deprecated)", C8);
        printIDN_Table(log, "C.9", "Tagging characters", C9);

        System.out.println("Done");
        log.close();
    }

    public static void printIDN_Map(PrintWriter log, String tableNumber, String description, Map map, Map other) {
        System.out.println(tableNumber+ " " + description);
        log.println("");
        log.println(tableNumber+ " " + description);
        log.println("");
        log.println("----- Start Table " + tableNumber + " -----");
        final Iterator it = map.keySet().iterator();
        while(it.hasNext()) {
            final Integer key = (Integer) it.next();
            final String value = (String) map.get(key);
            final int cp = key.intValue();
            log.println(Utility.hex(cp, 4) + "; " + Utility.hex(value, 4) + "; "
                    + (!value.equals(other.get(key))? "***" : "")
                    + Default.ucd().getName(cp));
        }
        log.println("----- End Table " + tableNumber + " -----");
    }

    public static void printIDN_Table(PrintWriter log, String tableNumber, String description, UnicodeSet set) {
        System.out.println(tableNumber+ " " + description);
        log.println("");
        log.println(tableNumber+ " " + description);
        log.println("");
        log.println("----- Start Table " + tableNumber + " -----");
        Utility.showSetNames(log, "", set, false, true, Default.ucd());
        log.println("----- End Table " + tableNumber + " -----");
    }

    public static BitSet guessIDN() {
        final BitSet result = new BitSet();
        for (int cp = 0; cp < 0x10FFFF; ++cp) {
            final int cat = Default.ucd().getCategory(cp);
            // 5.1 Currently-prohibited ASCII characters

            if (cp < 0x80 && cp != '-' && !(cat == Lu || cat == Ll || cat == Nd)) {
                result.set(cp);
            }

            // 5.2 Space characters

            if (cat == Zs) {
                result.set(cp);
            }

            // 5.3 Control characters
            if (cat == Cc || cat == Zp || cat == Zl) {
                result.set(cp);
            }

            // exclude those reserved for Cf
            /*if (0x2060 <= cp && cp <= 0x206F) result.set(cp);
            if (0xFFF0 <= cp && cp <= 0xFFFC) result.set(cp);
            if (0xE0000 <= cp && cp <= 0xE0FFF) result.set(cp);
             */

            // 5.4 Private use and replacement characters

            if (cat == Co) {
                result.set(cp);
            }
            if (cp == 0xFFFD) {
                result.set(cp);
            }

            // 5.5 Non-character code points
            if (Default.ucd().getBinaryProperty(cp, Noncharacter_Code_Point)) {
                result.set(cp);
            }

            // 5.6 Surrogate codes
            if (cat == Cs) {
                result.set(cp);
            }

            // 5.7 Inappropriate for plain text

            if (cat == Cf) {
                result.set(cp);
            }
            if (cp == 0xFFFC) {
                result.set(cp);
            }

            // 5.8 Inappropriate for domain names

            if (isIDS(cp)) {
                result.set(cp);
            }

            // 5.9 Change display properties
            // Cf, checked above

            // 5.10 Inappropriate characters from common input mechanisms
            if (cp == 0x3002) {
                result.set(cp);
            }

            // 5.11 Tagging characters
            // Cf, checked above
        }
        return result;
    }

    static boolean isIDS(int cp) { return 0x2FF0 <= cp && cp <= 0x2FFB; }


    /*
5.1 Currently-prohibited ASCII characters

Some of the ASCII characters that are currently prohibited in host names
by [STD13] are also used in protocol elements such as URIs [URI]. The other
characters in the range U+0000 to U+007F that are not currently allowed
are also prohibited in host name parts to reserve them for future use in
protocol elements.

0000-002C; [ASCII CONTROL CHARACTERS and SPACE through ,]
002E-002F; [ASCII . through /]
003A-0040; [ASCII : through @]
005B-0060; [ASCII [ through `]
007B-007F; [ASCII { through DEL]

5.2 Space characters

Space characters would make visual transcription of URLs nearly
impossible and could lead to user entry errors in many ways.

0020; SPACE
00A0; NO-BREAK SPACE
1680; OGHAM SPACE MARK
2000; EN QUAD
2001; EM QUAD
2002; EN SPACE
2003; EM SPACE
2004; THREE-PER-EM SPACE
2005; FOUR-PER-EM SPACE
2006; SIX-PER-EM SPACE
2007; FIGURE SPACE
2008; PUNCTUATION SPACE
2009; THIN SPACE
200A; HAIR SPACE
202F; NARROW NO-BREAK SPACE
3000; IDEOGRAPHIC SPACE

5.3 Control characters

Control characters cannot be seen and can cause unpredictable results
when displayed.

0000-001F; [CONTROL CHARACTERS]
007F; DELETE
0080-009F; [CONTROL CHARACTERS]
2028; LINE SEPARATOR
2029; PARAGRAPH SEPARATOR
206A-206F; [CONTROL CHARACTERS]
FFF9-FFFC; [CONTROL CHARACTERS]
1D173-1D17A; [MUSICAL CONTROL CHARACTERS]

5.4 Private use and replacement characters

Because private-use characters do not have defined meanings, they are
prohibited. The private-use characters are:

E000-F8FF; [PRIVATE USE, PLANE 0]
F0000-FFFFD; [PRIVATE USE, PLANE 15]
100000-10FFFD; [PRIVATE USE, PLANE 16]

The replacement character (U+FFFD) has no known semantic definition in a
name, and is often displayed by renderers to indicate "there would be
some character here, but it cannot be rendered". For example, on a
computer with no Asian fonts, a name with three ideographs might be
rendered with three replacement characters.

FFFD; REPLACEMENT CHARACTER

5.5 Non-character code points

Non-character code points are code points that have been allocated in
ISO/IEC 10646 but are not characters. Because they are already assigned,
they are guaranteed not to later change into characters.

FDD0-FDEF; [NONCHARACTER CODE POINTS]
FFFE-FFFF; [NONCHARACTER CODE POINTS]
1FFFE-1FFFF; [NONCHARACTER CODE POINTS]
2FFFE-2FFFF; [NONCHARACTER CODE POINTS]
3FFFE-3FFFF; [NONCHARACTER CODE POINTS]
4FFFE-4FFFF; [NONCHARACTER CODE POINTS]
5FFFE-5FFFF; [NONCHARACTER CODE POINTS]
6FFFE-6FFFF; [NONCHARACTER CODE POINTS]
7FFFE-7FFFF; [NONCHARACTER CODE POINTS]
8FFFE-8FFFF; [NONCHARACTER CODE POINTS]
9FFFE-9FFFF; [NONCHARACTER CODE POINTS]
AFFFE-AFFFF; [NONCHARACTER CODE POINTS]
BFFFE-BFFFF; [NONCHARACTER CODE POINTS]
CFFFE-CFFFF; [NONCHARACTER CODE POINTS]
DFFFE-DFFFF; [NONCHARACTER CODE POINTS]
EFFFE-EFFFF; [NONCHARACTER CODE POINTS]
FFFFE-FFFFF; [NONCHARACTER CODE POINTS]
10FFFE-10FFFF; [NONCHARACTER CODE POINTS]

5.6 Surrogate codes

The following code points are permanently reserved for use as surrogate
code values in the UTF-16 encoding, will never be assigned to
characters, and are therefore prohibited:

D800-DFFF; [SURROGATE CODES]

5.7 Inappropriate for plain text

The following characters should not appear in regular text.

FFF9; INTERLINEAR ANNOTATION ANCHOR
FFFA; INTERLINEAR ANNOTATION SEPARATOR
FFFB; INTERLINEAR ANNOTATION TERMINATOR
FFFC; OBJECT REPLACEMENT CHARACTER

5.8 Inappropriate for domain names

The ideographic description characters allow different sequences of
characters to be rendered the same way, which makes them inappropriate
for host names that must have a single canonical representation.

2FF0-2FFB; [IDEOGRAPHIC DESCRIPTION CHARACTERS]

5.9 Change display properties

The following characters, some of which are deprecated in ISO/IEC 10646,
can cause changes in display or the order in which characters appear
when rendered.

200E; LEFT-TO-RIGHT MARK
200F; RIGHT-TO-LEFT MARK
202A; LEFT-TO-RIGHT EMBEDDING
202B; RIGHT-TO-LEFT EMBEDDING
202C; POP DIRECTIONAL FORMATTING
202D; LEFT-TO-RIGHT OVERRIDE
202E; RIGHT-TO-LEFT OVERRIDE
206A; INHIBIT SYMMETRIC SWAPPING
206B; ACTIVATE SYMMETRIC SWAPPING
206C; INHIBIT ARABIC FORM SHAPING
206D; ACTIVATE ARABIC FORM SHAPING
206E; NATIONAL DIGIT SHAPES
206F; NOMINAL DIGIT SHAPES

5.10 Inappropriate characters from common input mechanisms

U+3002 is used as if it were U+002E in many input mechanisms,
particularly in Asia. This prohibition allows input mechanisms to safely
map U+3002 to U+002E before doing nameprep without worrying about
preventing users from accessing legitimate host name parts.

3002; IDEOGRAPHIC FULL STOP

5.11 Tagging characters

The following characters are used for tagging text and are invisible.

E0001; LANGUAGE TAG
E0020-E007F; [TAGGING CHARACTERS]
     */


    public static int verifyUTFMap(BitSet mappedOut) throws IOException {
        int errorCount = 0;
        final BufferedReader input = new BufferedReader(new FileReader(Settings.UnicodeTools.IDN_DIR + "IDN-Mapping.txt"),32*1024);
        String line = "";
        final Map idnFold = new TreeMap();
        final Map idnWhy = new HashMap();
        try {
            final String[] parts = new String[20];
            for (int lineNumber = 1; ; ++lineNumber) {
                line = input.readLine();
                if (line == null) {
                    break;
                }
                if ((lineNumber % 500) == 0) {
                    Utility.fixDot();
                    System.out.println("//" + lineNumber + ": '" + line + "'");
                }

                if (line.length() == 0) {
                    continue;
                }
                if (line.charAt(0) == '-') {
                    continue;
                }

                final int count = Utility.split(line,';',parts);
                if (count != 3) {
                    throw new ChainException("Incorrect # of fields in IDN folding, line = {0}",
                            new String[] {line});
                }

                final String key = Utility.fromHex(parts[0]);
                if (UTF16.countCodePoint(key) != 1) {
                    throw new ChainException("First IDN field not single character: " + line, null);
                }
                final int cp = UTF16.charAt(key, 0);
                if (!Default.ucd().isAssigned(cp) || Default.ucd().isPUA(cp)) {
                    throw new ChainException("IDN character unassigned or PUA: " + line, null);
                }
                String value = Utility.fromHex(parts[1]);
                final String reason = parts[2].trim();

                if (reason.equals("Map out")) {
                    value = Utility.fromHex(parts[1]);
                    Utility.fixDot();
                    showError("Mapping Out: ", cp, "");
                    mappedOut.set(cp);
                }
                idnFold.put(key, value);
                idnWhy.put(key, reason);
            }

            for (int cp = 0; cp <= 0x10FFFF; ++cp) {
                Utility.dot(cp);
                if (!Default.ucd().isAssigned(cp) || Default.ucd().isPUA(cp)) {
                    continue;
                }
                if (mappedOut.get(cp)) {
                    continue;
                }

                final String key = UTF32.valueOf32(cp);
                String value = (String)idnFold.get(key);
                if (value == null) {
                    value = key;
                }
                final String reason = (String)idnWhy.get(key);
                final String ucdFold = Default.ucd().getCase(cp, FULL, FOLD, "I");
                if (!ucdFold.equals(value)) {
                    final String b = Default.nfkc().normalize(Default.ucd().getCase(cp, FULL, FOLD, "I"));
                    final String c = Default.nfkc().normalize(Default.ucd().getCase(b, FULL, FOLD, "I"));

                    if (c.equals(value)) {
                        continue;
                    }
                    Utility.fixDot();

                    System.out.println("Mismatch: " + Default.ucd().getCodeAndName(cp));
                    System.out.println("  UCD Case Fold: <" + Default.ucd().getCodeAndName(ucdFold) + ">");
                    System.out.println("  IDN Map [" + reason + "]: <" + Default.ucd().getCodeAndName(value) + ">");
                    errorCount++;
                }
            }
        } finally {
            input.close();
        }
        return errorCount;
    }

    static BitSet getIDNList(String file) throws IOException {
        final BufferedReader input = new BufferedReader(new FileReader(Settings.UnicodeTools.IDN_DIR + file),32*1024);
        final BitSet result = new BitSet();
        String line;
        try {
            final String[] parts = new String[20];
            for (int lineNumber = 1; ; ++lineNumber) {
                line = input.readLine();
                if (line == null) {
                    break;
                }
                if ((lineNumber % 500) == 0) {
                    Utility.fixDot();
                    System.out.println("//" + lineNumber + ": '" + line + "'");
                }

                final int commentPos = line.indexOf(';');
                if (commentPos >= 0) {
                    line = line.substring(0,commentPos);
                }
                line = line.trim();
                if (line.length() == 0) {
                    continue;
                }
                if (line.charAt(0) == '-') {
                    continue;
                }

                final int count = Utility.split(line,'-',parts);
                if (count > 2) {
                    throw new ChainException("Incorrect # of fields in IDN list", null);
                }
                final int start = Utility.codePointFromHex(parts[0]);
                final int end = count == 1 ? start : Utility.codePointFromHex(parts[1]);

                for (int i = start; i <= end; ++i) {
                    result.set(i);
                }
            }
        } finally {
            input.close();
        }
        return result;
    }

    /*
                    + "\n#  Generated from <2060..206F, FFF0..FFFB, E0000..E0FFF>"
                    + "\n#    + Other_Default_Ignorable_Code_Point + (Cf + Cc + Cs - White_Space)";
     */

    public static void diffIgnorable () {


        final UnicodeSet control = UnifiedBinaryProperty.make(CATEGORY + Cf, Default.ucd()).getSet();

        System.out.println("Cf");
        Utility.showSetNames("", control, false, Default.ucd());

        control.addAll(UnifiedBinaryProperty.make(CATEGORY + Cc, Default.ucd()).getSet());

        System.out.println("Cf + Cc");
        Utility.showSetNames("", control, false, Default.ucd());

        control.addAll(UnifiedBinaryProperty.make(CATEGORY + Cs, Default.ucd()).getSet());

        System.out.println("Cf + Cc + Cs");
        Utility.showSetNames("", control, false, Default.ucd());

        control.removeAll(UnifiedBinaryProperty.make(BINARY_PROPERTIES + White_space, Default.ucd()).getSet());

        System.out.println("Cf + Cc + Cs - WhiteSpace");
        Utility.showSetNames("", control, false, Default.ucd());

        control.add(0x2060,0x206f).add(0xFFF0,0xFFFB).add(0xE0000,0xE0FFF);

        System.out.println("(Cf + Cc + Cs - WhiteSpace) + ranges");
        Utility.showSetNames("", control, false, Default.ucd());

        final UnicodeSet odicp = UnifiedBinaryProperty.make(BINARY_PROPERTIES + Other_Default_Ignorable_Code_Point, Default.ucd()).getSet();

        odicp.removeAll(control);

        System.out.println("Minimal Default Ignorable Code Points");
        Utility.showSetNames("", odicp, true, Default.ucd());
    }


    public static void IdentifierTest() {
        final String x = normalize(UTF32.valueOf32(0x10300), 4) ;
        getCategoryID(x);

        /*
        Changes Category: U+10300 OLD ITALIC LETTER A
   nfx_cp: U+D800 <surrogate-D800>
 isIdentifier(nfx_cp, true): false
   cat(nfx_cp): Cs
 isIdentifierStart(cp, true): true
   cat(cp): Lo
         */

        for (int j = 0; j < 5; ++j) {
            System.out.println();
            System.out.println("Testing Identifier Closure for " + NAMES[j]);
            System.out.println();
            for (int cp = 0; cp < 0x10FFFF; ++cp) {
                Utility.dot(cp);
                if (!Default.ucd().isAssigned(cp)) {
                    continue;
                }
                if (Default.ucd().isPUA(cp)) {
                    continue;
                }
                if (isNormalized(cp, j)) {
                    continue;
                }

//                if (cp == 0xFDFB || cp == 0x0140) {
//                    System.out.println("debug point");
//                }

                final boolean norm;
                final boolean plain;

                final String x_cp = 'x' + UTF32.valueOf32(cp);
                final String nfx_x_cp = normalize(x_cp, j);
                if (true) {
                    throw new RuntimeException("Fix plain & norm, 4 instances!!");
                }
                // plain = Default.ucd.isIdentifier(x_cp, true);
                //norm = Default.ucd.isIdentifier(nfx_x_cp, true);
                if (plain & !norm) {
                    Utility.fixDot();
                    System.out.println("*Not Identifier: " + Default.ucd().getCodeAndName(cp));
                    System.out.println("    nfx_x_cp: " + Default.ucd().getCodeAndName(nfx_x_cp));

                    System.out.println("  isIdentifier(nfx_x_cp, true): " + norm);
                    System.out.println("    cat(nfx_x_cp): " + getCategoryID(nfx_x_cp));

                    System.out.println("  isIdentifier(x_cp, true): " + plain);
                    System.out.println("    cat(x_cp): " + getCategoryID(x_cp));
                    continue;
                }

                final String nfx_cp = normalize(UTF32.valueOf32(cp), j);
                // plain = Default.ucd.isIdentifierStart(cp, true);
                // norm = Default.ucd.isIdentifier(nfx_cp, true);
                if (plain & !norm) {
                    Utility.fixDot();
                    System.out.println(" Changes Category: " + Default.ucd().getCodeAndName(cp));
                    System.out.println("    nfx_cp: " + Default.ucd().getCodeAndName(nfx_cp));

                    System.out.println("  isIdentifier(nfx_cp, true): " + norm);
                    System.out.println("    cat(nfx_cp): " + getCategoryID(nfx_cp));

                    System.out.println("  isIdentifierStart(cp, true): " + plain);
                    System.out.println("    cat(cp): " + Default.ucd().getCategoryID(cp));
                    System.out.println();
                    continue;
                }
            }
        }
    }

    static String getCategoryID(String s) {
        if (UTF16.countCodePoint(s) == 1) {
            return Default.ucd().getCategoryID(s.codePointAt(0));
        }
        final StringBuffer result = new StringBuffer();
        int cp;
        for (int i = 0; i < s.length(); i += Character.charCount(cp)) {
            cp = UTF16.charAt(s, i);
            if (i != 0) {
                result.append(' ');
            }
            result.append(Default.ucd().getCategoryID(cp));
        }
        return result.toString();
    }

    static String normalize(String s, int j) {
        if (j < 4) {
            return Default.nf(j).normalize(s);
        }
        return Default.ucd().getCase(s, FULL, FOLD);
    }

    static boolean isNormalized(int cp, int j) {
        if (j < 4) {
            return !Default.nf(j).isNormalized(cp);
        }
        return false;
    }

    private static final String[] NAMES = {"Default.nfd", "NFC", "NFKD", "NFKC", "Fold"};

    public static void NFTest() {
        for (int j = 0; j < 4; ++j) {
            final Normalizer nfx = Default.nf(j);
            System.out.println();
            System.out.println("Testing isNormalized for " + NAMES[j]);
            System.out.println();
            for (int i = 0; i < 0x10FFFF; ++i) {
                Utility.dot(i);
                if (!Default.ucd().isAssigned(i)) {
                    continue;
                }
                if (Default.ucd().isPUA(i)) {
                    continue;
                }
                final String s = nfx.normalize(i);
                final boolean differs = !s.equals(UTF32.valueOf32(i));
                final boolean call = !nfx.isNormalized(i);
                if (differs != call) {
                    Utility.fixDot();
                    System.out.println("Problem: differs: " + differs
                            + ", call: " + call + " " + Default.ucd().getCodeAndName(i));
                }
            }

        }
    }

    static final int EXCEPTION_FLAG = 0x8000000;

    public static void checkScripts() throws IOException {

        boolean ok;
        final Map m = new TreeMap();
        final UnicodeSet exceptions = ScriptExceptions.getExceptions();
        int maxScriptLen = 0;
        final UnicodeSet show = new UnicodeSet();
        show.add(0x2071);
        show.add(0x207F);

        for (int i = 0; i < 0x10FFFF; ++i) {
            if (!Default.ucd().isAssigned(i)) {
                continue;
            }
            byte cat = Default.ucd().getCategory(i);
            final short script = Default.ucd().getScript(i);
            switch (cat) {
            case Lo: case Lt: case Ll: case Lu: case Lm: case Mc: case Sk:
                ok = script != INHERITED_SCRIPT && script != COMMON_SCRIPT;
                break;
            case Mn: case Me:
                ok = script == INHERITED_SCRIPT;
                break;
            default:
                ok = script == COMMON_SCRIPT;
                break;
            }
            if (show.contains(i)) {
                System.out.println(Default.ucd().getCodeAndName(i)
                        + "; " + Default.ucd().getScriptID(i)
                        + "; " + Default.ucd().getCategoryID(i)
                        );
            }
            if (!ok) {
                if (cat == Ll || cat == Lt) {
                    cat = Lu;
                }
                int intKey = (cat << 8) + script;
                if (exceptions.contains(i)) {
                    intKey |= EXCEPTION_FLAG;
                }
                final Integer key = new Integer(intKey);
                UnicodeSet us = (UnicodeSet) m.get(key);
                if (us == null) {
                    us = new UnicodeSet();
                    m.put(key, us);
                }
                us.add(i);
                final int len = Default.ucd().getScriptID(i).length();
                if (maxScriptLen < len) {
                    maxScriptLen = len;
                }
            }
        }

        final PrintWriter log = Utility.openPrintWriterGenDir("log/CheckScriptsLog.txt", Utility.LATIN1_UNIX);

        final Iterator it = m.keySet().iterator();
        while (it.hasNext()) {
            final Integer key = (Integer) it.next();
            final int intKey = key.intValue();
            final UnicodeSet badChars = (UnicodeSet) m.get(key);
            final int ranges = badChars.getRangeCount();
            for (int kk = 0; kk < ranges; ++kk) {
                final int start = badChars.getRangeStart(kk);
                final int end = badChars.getRangeEnd(kk);
                final String code = Utility.hex(start) + (start != end ? ".." + Utility.hex(end) : "");
                final String scriptName = Default.ucd().getScriptID(start);
                String title = "FAIL";
                if ((intKey & EXCEPTION_FLAG) != 0) {
                    title = "EXCEPTION";
                }
                log.println(title + ": " + code + "; " + Utility.repeat(" ", 14 - code.length())
                        + scriptName + Utility.repeat(" ", maxScriptLen-scriptName.length())
                        + " # (" + LCgetCategoryID(start) + ") " + Default.ucd().getName(start)
                        + (start != end ? ".." + Default.ucd().getName(end) : "")
                        );
            }
            log.println();
        }
        log.close();
    }

    static public String LCgetCategoryID(int cp) {
        final byte cat = Default.ucd().getCategory(cp);
        if (cat == Lu || cat == Lt || cat == Ll) {
            return "LC";
        }
        return Default.ucd().getCategoryID(cp);
    }

    static public void verifyNormalizationStability() {

        verifyNormalizationStability2("3.1.0");
        verifyNormalizationStability2("3.0.0");
    }

    static public void verifyNormalizationStability2(String version) {

        // Default.nfd.normalizationDiffers(0x10300);

        final UCD older = UCD.make(version); // Default.ucd.getPreviousVersion();

        final Normalizer oldNFC = new Normalizer(UCD_Types.NFC, older.getVersion());
        final Normalizer oldNFD = new Normalizer(UCD_Types.NFD, older.getVersion());
        final Normalizer oldNFKC = new Normalizer(UCD_Types.NFKC, older.getVersion());
        final Normalizer oldNFKD = new Normalizer(UCD_Types.NFKD, older.getVersion());

        System.out.println("Testing " + Default.nfd().getUCDVersion() + " against " + oldNFD.getUCDVersion());

        for (int i = 0; i <= 0x10FFFF; ++i) {
            Utility.dot(i);
            if (!Default.ucd().isAssigned(i)) {
                continue;
            }
            final byte cat = Default.ucd().getCategory(i);
            if (cat == Cs || cat == PRIVATE_USE) {
                continue;
            }

            if (i == 0x5e) {
                System.out.println("debug");
                final String test1 = Default.nfkd().normalize(i);
                final String test2 = oldNFKD.normalize(i);
                System.out.println("Testing (new/old)" + Default.ucd().getCodeAndName(i));
                System.out.println("\t" + Default.ucd().getCodeAndName(test1));
                System.out.println("\t" + Default.ucd().getCodeAndName(test2));
            }

            if (older.isAssigned(i)) {

                final int newCan = Default.ucd().getCombiningClass(i);
                final int oldCan = older.getCombiningClass(i);
                if (newCan != oldCan) {
                    System.out.println("FAILS CCC STABILITY: " + newCan + " != " + oldCan
                            + "; " + Default.ucd().getCodeAndName(i));
                }

                verifyEquals(i, "NFD STABILITY (new/old)", Default.nfd().normalize(i), oldNFD.normalize(i));
                verifyEquals(i, "NFC STABILITY (new/old)", Default.nfc().normalize(i), oldNFC.normalize(i));
                verifyEquals(i, "NFKD STABILITY (new/old)", Default.nfkd().normalize(i), oldNFKD.normalize(i));
                verifyEquals(i, "NFKC STABILITY (new/old)", Default.nfkc().normalize(i), oldNFKC.normalize(i));

            } else {
                // not in older version.
                // (1) If there is a decomp, and it is composed of all OLD characters, then it must NOT compose
                if (!Default.nfd().isNormalized(i)) {
                    final String decomp = Default.nfd().normalize(i);
                    if (noneHaveCategory(decomp, Cn, older)) {
                        final String recomp = Default.nfc().normalize(decomp);
                        if (recomp.equals(UTF16.valueOf(i))) {
                            Utility.fixDot();
                            System.out.println("FAILS COMP STABILITY: " + Default.ucd().getCodeAndName(i));
                            System.out.println("\t" + Default.ucd().getCodeAndName(decomp));
                            System.out.println("\t" + Default.ucd().getCodeAndName(recomp));
                            System.out.println();
                            throw new IllegalArgumentException("Comp stability");
                        }
                    }
                }
            }
        }
    }

    public static boolean noneHaveCategory(String s, byte cat, UCD ucd) {
        int cp;
        for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(s, i);
            final byte cat2 = ucd.getCategory(i);
            if (cat == cat2) {
                return false;
            }
        }
        return true;
    }

    public static void verifyEquals(int cp, String message, String a, String b) {
        if (!a.equals(b)) {
            Utility.fixDot();
            System.out.println("FAILS " + message + ": " + Default.ucd().getCodeAndName(cp));
            System.out.println("\t" + Default.ucd().getCodeAndName(a));
            System.out.println("\t" + Default.ucd().getCodeAndName(b));
            System.out.println();
        }
    }

    public static void checkAgainstUInfo() {
        /*
        Default.ucd = UCD.make(Default.Default.ucdVersion);
        UData x = new UData();
        x.fleshOut();

        System.out.println(Default.ucd.toString(0x1E0A));

        UInfo.init();
        System.out.println("Cross-checking against old implementation");
        System.out.println("Version: " + Default.ucd.getVersion() + ", " + new Date(Default.ucd.getDate()));
        for (int i = 0; i <= 0xFFFF; ++i) {
            Utility.dot(i);

            if ((i & 0x0FFF) == 0) System.out.println("#" + Utility.hex(i));
            try {
                check(i, Default.ucd.getName(i), UInfo.getName((char)i), "Name");
                check(i, Default.ucd.getCategory(i), UInfo.getCategory((char)i), UCD_Names.GC, "GeneralCategory");
                check(i, Default.ucd.getCombiningClass(i), UInfo.getCanonicalClass((char)i), "CanonicalClass");
                check(i, Default.ucd.getBidiClass(i), UInfo.getBidiClass((char)i), UCD_Names.BC, "BidiClass");
                check(i, Default.ucd.getDecompositionMapping(i), UInfo.getDecomposition((char)i), "Decomposition");
                check(i, Default.ucd.getDecompositionType(i), UInfo.getDecompositionType((char)i), UCD_Names.DT, "DecompositionType");
                check(i, Default.ucd.getNumericValue(i), UInfo.getNumeric((char)i), "NumericValue");
                check(i, Default.ucd.getNumericType(i), UInfo.getNumericType((char)i), UCD_Names.NT, "NumericType");

                check(i, Default.ucd.getCase(i, SIMPLE, LOWER), UInfo.getLowercase((char)i), "SimpleLowercase");
                check(i, Default.ucd.getCase(i, SIMPLE, UPPER), UInfo.getUppercase((char)i), "SimpleUppercase");
                check(i, Default.ucd.getCase(i, SIMPLE, TITLE), UInfo.getTitlecase((char)i), "SimpleTitlecase");
                //check(i, Default.ucd.getSimpleCaseFolding(i), UInfo.getSimpleCaseFolding((char)i));

                if (Default.ucd.getSpecialCase(i).length() == 0) {  // NORMAL
                    check(i, Default.ucd.getCase(i, FULL, LOWER), UInfo.toLowercase((char)i, ""), "FullLowercase");
                    check(i, Default.ucd.getCase(i, FULL, UPPER), UInfo.toUppercase((char)i, ""), "FullUppercase");
                    check(i, Default.ucd.getCase(i, FULL, TITLE), UInfo.toTitlecase((char)i, ""), "FullTitlecase");
                } else {                                    // SPECIAL
                    check(i, Default.ucd.getCase(i, SIMPLE, LOWER), UInfo.toLowercase((char)i, ""), "FullLowercase");
                    check(i, Default.ucd.getCase(i, SIMPLE, UPPER), UInfo.toUppercase((char)i, ""), "FullUppercase");
                    check(i, Default.ucd.getCase(i, SIMPLE, TITLE), UInfo.toTitlecase((char)i, ""), "FullTitlecase");
                }
                // check(i, Default.ucd.getFullCaseFolding(i), UInfo.getFullCaseFolding((char)i));

                check(i, Default.ucd.getSpecialCase(i).toUpperCase(), UInfo.getCaseCondition((char)i).toUpperCase(), "SpecialCase");
                check(i, Default.ucd.getLineBreak(i), UInfo.getLineBreakType((char)i), UCD_Names.LB, "LineBreak");
                check(i, Default.ucd.getEastAsianWidth(i), UInfo.getEastAsianWidthType((char)i), UCD_Names.EA, "EastAsian");

                int props = Default.ucd.getBinaryProperties(i);
                check(i, (props>>BidiMirrored) & 1, UInfo.getMirrored((char)i), UCD_Names.YN_TABLE, "BidiMirroring");
                check(i, (props>>CompositionExclusion) & 1, UInfo.isCompositionExcluded((char)i)?1:0, UCD_Names.YN_TABLE, "Comp-Exclusion");

            } catch (Exception e) {
                Utility.fixDot();

                System.out.println("Error: " + Utility.hex(i) + " " + e.getClass().getName() + e.getMessage());
                e.printStackTrace();
            }
        }
         */
    }


    public static void check(int cp, boolean x, boolean y, String[] names, String type) {
        check(cp, x ? 1 : 0, y ? 1 : 0, names, type);
    }

    public static void check(int cp, int x, int y, String[] names, String type) {
        if (x == y) {
            return;
        }
        showLast(cp);
        Utility.fixDot();
        System.out.println("  " + type + ": "
                + Utility.getName(x, names) + " (" + x  + ") " + " != "
                + Utility.getName(y, names) + " (" + y  + ") ") ;
    }

    public static void check(int cp, int x, int y, String type) {
        if (x == y) {
            return;
        }
        showLast(cp);
        Utility.fixDot();
        System.out.println("  " + type + ": " + x + " != " + y) ;
    }

    public static void check(int cp, double x, double y, String type) {
        if (!(x > y) && !(x < y))
        {
            return;   // funny syntax to catch NaN
        }
        showLast(cp);
        Utility.fixDot();
        System.out.println("  " + type + ": " + x + " != " + y) ;
    }

    public static void check(int cp, String x, String y, String type) {
        if (x != null && x.equals(y)) {
            return;
        }
        if (x != null && y != null
                && x.length() > 0 && y.length() > 0
                && x.charAt(0) == '<' && y.charAt(0) == '<') {
            if (x.startsWith("<unassigned") && y.equals("<reserved>")) {
                return;
            }
            if (y.equals("<control>")) {
                return;
            }
            if (x.startsWith("<surrogate") && y.indexOf("Surrogate") != -1) {
                return;
            }
            if (x.startsWith("<private use") && y.startsWith("<Private Use")) {
                return;
            }
        }
        showLast(cp);
        Utility.fixDot();
        System.out.println("  " + type + ": " + Utility.quoteJavaString(x) + " != " + Utility.quoteJavaString(y));
    }


    static int lastShowed = -1;
    static boolean showCanonicalDecomposition = false;

    static void showLast(int cp) {
        if (lastShowed != cp) {
            Utility.fixDot();
            System.out.println();
            final String s = Default.ucd().getDecompositionMapping(cp);
            System.out.print(Default.ucd().getCodeAndName(cp));
            if (showCanonicalDecomposition && !s.equals(UTF32.valueOf32(cp))) {
                System.out.print(" => " + Default.ucd().getCodeAndName(s));
            }
            System.out.println();
            lastShowed = cp;
        }
    }

    public static void test1() {


        for (int i = 0x19; i < 0x10FFFF; ++i) {

            System.out.println(Utility.hex(i) + " " + Utility.quoteJavaString(Default.ucd().getName(i)));

            System.out.print("    "
                    + ", gc=" + Default.ucd().getCategoryID(i)
                    + ", bc=" + Default.ucd().getBidiClassID(i)
                    + ", cc=" + Default.ucd().getCombiningClassID(i)
                    + ", ea=" + Default.ucd().getEastAsianWidthID(i)
                    + ", lb=" + Default.ucd().getLineBreakID(i)
                    + ", dt=" + Default.ucd().getDecompositionTypeID(i)
                    + ", nt=" + Default.ucd().getNumericTypeID(i)
                    + ", nv=" + Default.ucd().getNumericValue(i)
                    );
            for (int j = 0; j < UCD_Types.LIMIT_BINARY_PROPERTIES; ++j) {
                if (Default.ucd().getBinaryProperty(i,j)) {
                    System.out.print(", " + UCD_Names.BP[j]);
                }
            }
            System.out.println();

            System.out.println("    "
                    + ", dm=" + Utility.quoteJavaString(Default.ucd().getDecompositionMapping(i))
                    + ", slc=" + Utility.quoteJavaString(Default.ucd().getCase(i, SIMPLE, LOWER))
                    + ", stc=" + Utility.quoteJavaString(Default.ucd().getCase(i, SIMPLE, TITLE))
                    + ", suc=" + Utility.quoteJavaString(Default.ucd().getCase(i, SIMPLE, UPPER))
                    + ", flc=" + Utility.quoteJavaString(Default.ucd().getCase(i, FULL, LOWER))
                    + ", ftc=" + Utility.quoteJavaString(Default.ucd().getCase(i, FULL, TITLE))
                    + ", fuc=" + Utility.quoteJavaString(Default.ucd().getCase(i, FULL, UPPER))
                    + ", sc=" + Utility.quoteJavaString(Default.ucd().getSpecialCase(i))
                    );

            if (i > 0x180) {
                i = 3 * i / 2;
            }
        }
    }

    static void checkCanonicalProperties() {

        System.out.println(Default.ucd().toString(0x1E0A));

        System.out.println("Cross-checking canonical equivalence");
        System.out.println("Version: " + Default.ucd().getVersion() + ", " + new Date(Default.ucd().getDate()));
        showCanonicalDecomposition = true;
        for (int q = 1; q < 2; ++q) {
            for (int i = 0; i <= 0x10FFFF; ++i) {
                Utility.dot(i);
                if (i == 0x0387) {
                    System.out.println("debug?");
                }
                final byte type = Default.ucd().getDecompositionType(i);
                if (type != CANONICAL) {
                    continue;
                }

                final String s = Default.ucd().getDecompositionMapping(i);
                final int slen = UTF16.countCodePoint(s);
                final int j = s.codePointAt(0);
                try {
                    if (q == 0) {
                        check(i, Default.ucd().getCategory(i), Default.ucd().getCategory(j), UCD_Names.GENERAL_CATEGORY, "GeneralCategory");
                        check(i, Default.ucd().getCombiningClass(i), Default.ucd().getCombiningClass(j), "CanonicalClass");
                        check(i, Default.ucd().getBidiClass(i), Default.ucd().getBidiClass(j), UCD_Names.BIDI_CLASS, "BidiClass");
                        check(i, Default.ucd().getNumericValue(i), Default.ucd().getNumericValue(j), "NumericValue");
                        check(i, Default.ucd().getNumericType(i), Default.ucd().getNumericType(j), UCD_Names.LONG_NUMERIC_TYPE, "NumericType");

                        if (false) {
                            for (byte k = LOWER; k < LIMIT_CASE; ++k) {
                                check(i, Default.ucd().getCase(i, SIMPLE, k), Default.ucd().getCase(j, SIMPLE, k), "Simple("+k+")");
                                check(i, Default.ucd().getCase(i, FULL, k), Default.ucd().getCase(j, FULL, k), "Full("+k+")");
                            }
                        }

                        if (slen == 1) {
                            check(i, Default.ucd().getSpecialCase(i), Default.ucd().getSpecialCase(j), "SpecialCase");
                        }

                        for (byte k = 0; k < LIMIT_BINARY_PROPERTIES; ++k) {
                            if (k == Hex_Digit) {
                                continue;
                            }
                            if (k == Radical) {
                                continue;
                            }
                            if (k == UnifiedIdeograph) {
                                continue;
                            }
                            if (k == CompositionExclusion) {
                                continue;
                            }
                            check(i, Default.ucd().getBinaryProperty(i, k), Default.ucd().getBinaryProperty(j, k), UCD_Names.YN_TABLE, Default.ucd().getBinaryPropertiesID_fromIndex(k));
                        }
                    } else {
                        //check(i, Default.ucd.getLineBreak(i), Default.ucd.getLineBreak(j), UCD_Names.LB, "LineBreak");
                        //check(i, Default.ucd.getEastAsianWidth(i), Default.ucd.getEastAsianWidth(j), UCD_Names.EA, "EastAsian");
                    }

                } catch (final Exception e) {
                    System.out.println("Error: " + Utility.hex(i) + " " + e.getClass().getName() + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    static void checkSpeed() {
        final int count = 1000000;
        int sum = 0;
        long start, end;

        final java.text.NumberFormat nf = java.text.NumberFormat.getPercentInstance();

        start = System.currentTimeMillis();
        for (int i = count; i >= 0; --i) {
            sum += dummy0(i).length();
        }
        end = System.currentTimeMillis();
        final double base = end - start;

        System.out.println("unsynchronized static char[]: " + nf.format((end - start)/base));

        start = System.currentTimeMillis();
        for (int i = count; i >= 0; --i) {
            sum += dummy2(i).length();
        }
        end = System.currentTimeMillis();
        System.out.println("synchronized static char[]: " + nf.format((end - start)/base));

        start = System.currentTimeMillis();
        for (int i = count; i >= 0; --i) {
            sum += dummy1(i).length();
        }
        end = System.currentTimeMillis();
        System.out.println("char[] each time: " + nf.format((end - start)/base));

        start = System.currentTimeMillis();
        for (int i = count; i >= 0; --i) {
            sum += dummy3(i).length();
        }
        end = System.currentTimeMillis();
        System.out.println("two valueofs: " + nf.format((end - start)/base));

        System.out.println(sum);
    }

    static String dummy1(int a) {
        final char[] temp = new char[2];
        temp[0] = (char)(a >>> 16);
        temp[1] = (char)a;
        return new String(temp);
    }

    static char[] temp2 = new char[2];

    static String dummy2(int a) {
        synchronized (temp2) {
            temp2[0] = (char)(a >>> 16);
            temp2[1] = (char)a;
            return new String(temp2);
        }
    }

    static String dummy0(int a) {
        temp2[0] = (char)(a >>> 16);
        temp2[1] = (char)a;
        return new String(temp2);
    }

    static String dummy3(int a) {
        return String.valueOf((char)(a >>> 16)) + (char)a;
    }

}
