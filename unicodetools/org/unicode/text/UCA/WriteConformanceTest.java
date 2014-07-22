package org.unicode.text.UCA;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.TreeMap;

import org.unicode.text.UCA.UCA.AppendToCe;
import org.unicode.text.UCA.UCA.CollatorType;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.Normalizer;
import org.unicode.text.UCD.UCD;
import org.unicode.text.utility.Utility;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.CanonicalIterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

public class WriteConformanceTest {
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_SHOW_ITERATION = true;
    private static int addCounter = 0;

    private static final UnicodeSet RTL = new UnicodeSet("[[:bc=r:][:bc=al:][:bc=an:]]").freeze();

    private static final boolean SKIP_SPECIAL_TIBETAN = false;

    private static final char LOW_ACCENT = '\u0334';

    private static final String   SUPPLEMENTARY_ACCENT         = UTF16.valueOf(0x1D165);
    private static final String   COMPLETELY_IGNOREABLE        = "\u0001";
    private static final String   COMPLETELY_IGNOREABLE_ACCENT = "\u0591";
    private static final String[] CONTRACTION_TEST = { SUPPLEMENTARY_ACCENT, COMPLETELY_IGNOREABLE, COMPLETELY_IGNOREABLE_ACCENT };

    private static CanonicalIterator canIt = null;
    private static TreeMap<String, String> sortedD = new TreeMap<String, String>();

    private static UCD ucd;
    private static UCA collator;
    private static Normalizer nfd;
    private static Normalizer nfkd;
    private static UnicodeSet hanNotInCPOrder;

    static void writeConformance(String filename, byte option, boolean shortPrint, CollatorType collatorType) throws IOException {
        ucd = Default.ucd();
        collator = WriteCollationData.getCollator(collatorType);
        nfd = Default.nfd();

        /*
         * U+01D5 LATIN CAPITAL LETTER U WITH DIAERESIS AND MACRON => U+00DC
         * LATIN CAPITAL LETTER U WITH DIAERESIS, U+0304 COMBINING MACRON
         */
        if (DEBUG) {
            final String[] testList = { "\u0000", "\u0000\u0300", "\u0300", "\u0301", "\u0020", "\u0020\u0300", "A", "\u3192", "\u3220", "\u0344", "\u0385", "\uF934", "U", "U\u0308", "\u00DC", "\u00DC\u0304", "U\u0308\u0304" };
            // load the library first
            collator.getCEList("a", true);

            for (final String t : testList) {
                System.out.println();
                System.out.println(ucd.getCodeAndName(t));

                final CEList ces = collator.getCEList(t, true);
                System.out.println("CEs:    " + ces);

                String test = collator.getSortKey(t, option, true, AppendToCe.tieBreaker);
                System.out.println("Decomp: " + UCA.toString(test));

                test = collator.getSortKey(t, option, false, AppendToCe.tieBreaker);
                System.out.println("No Dec: " + UCA.toString(test));
            }
        }

        final String fullFileName = "CollationTest"
                + (collatorType==CollatorType.cldr ? "_CLDR" : "")
                + (option == UCA_Types.NON_IGNORABLE ? "_NON_IGNORABLE" : "_SHIFTED")
                //+ (appendNfd ? "_NFD" : "")
                + (shortPrint ? "_SHORT" : "") + ".txt";

        final String directory = UCA.getUCA_GEN_DIR() + File.separator
                + (collatorType==CollatorType.cldr ? "CollationAuxiliary" : "CollationTest");

        final PrintWriter log = Utility.openPrintWriter(directory, fullFileName, Utility.UTF8_WINDOWS);
        // if (!shortPrint) log.write('\uFEFF');
        WriteCollationData.writeVersionAndDate(log, fullFileName, collatorType==CollatorType.cldr);

        System.out.println("Sorting");
        int counter = 0;

        final UCA.UCAContents cc = collator.getContents(null);
        cc.setDoEnableSamples(true);
        if (collatorType == CollatorType.cldr) {
            nfkd = Default.nfkd();
            hanNotInCPOrder = new RadicalStroke(collator.getUCDVersion()).getHanNotInCPOrder();
        }
        final UnicodeSet found2 = new UnicodeSet();

        while (true) {
            final String s = cc.next();
            if (s == null) {
                break;
            }

            if (SKIP_SPECIAL_TIBETAN && collatorType==CollatorType.ducet && s.length() > 1
                    && (s.startsWith("\u0FB2") || s.startsWith("\u0FB3"))) {
                continue;
            }

            found2.addAll(s);

            if (DEBUG_SHOW_ITERATION) {
                final int cp = s.codePointAt(0);
                if (cp == 0x1CD0 || !ucd.isAssigned(cp) || UCD.isCJK_BASE(cp)) {
                    System.out.println(ucd.getCodeAndName(s));
                }
            }
            Utility.dot(counter++);
            addStringX(s, option, AppendToCe.tieBreaker);
        }

        // Add special examples
        /*
         * addStringX("\u2024\u2024", option); addStringX("\u2024\u2024\u2024",
         * option); addStringX("\u2032\u2032", option);
         * addStringX("\u2032\u2032\u2032", option);
         * addStringX("\u2033\u2033\u2033", option); addStringX("\u2034\u2034",
         * option);
         */

        final UnicodeSet found = collator.getStatistics().found;
        if (!found2.containsAll(found2)) {
            System.out.println("In both: " + new UnicodeSet(found).retainAll(found2).toPattern(true));
            System.out.println("In UCA but not iteration: " + new UnicodeSet(found).removeAll(found2).toPattern(true));
            System.out.println("In iteration but not UCA: " + new UnicodeSet(found2).removeAll(found).toPattern(true));
            throw new IllegalArgumentException("Inconsistent data");

        }

        /*
         * for (int i = 0; i <= 0x10FFFF; ++i) { if (!ucd.isAssigned(i))
         * continue; addStringX(UTF32.valueOf32(i), option); }
         * 
         * Hashtable multiTable = collator.getContracting(); Enumeration enum =
         * multiTable.keys(); while (enum.hasMoreElements()) {
         * Utility.dot(counter++); addStringX((String)enum.nextElement(),
         * option); }
         * 
         * for (int i = 0; i < extraConformanceTests.length; ++i) { // put in
         * sample non-characters Utility.dot(counter++); String s =
         * UTF32.valueOf32(extraConformanceTests[i]); Utility.fixDot();
         * System.out.println("Adding: " + Utility.hex(s)); addStringX(s,
         * option); }
         * 
         * 
         * 
         * for (int i = 0; i < extraConformanceRanges.length; ++i) {
         * Utility.dot(counter++); int start = extraConformanceRanges[i][0]; int
         * end = extraConformanceRanges[i][1]; int increment = ((end - start +
         * 1) / 303) + 1; //System.out.println("Range: " + start + ", " + end +
         * ", " + increment); addStringX(start, option); for (int j = start+1; j
         * < end-1; j += increment) { addStringX(j, option); addStringX(j+1,
         * option); } addStringX(end-1, option); addStringX(end, option); }
         */

        Utility.fixDot();
        System.out.println("Total: " + sortedD.size());

        System.out.println("Writing");
        // String version = collator.getVersion();

        final Iterator<String> it = sortedD.keySet().iterator();

        final int level = (option == UCA_Types.NON_IGNORABLE ? 3 : 4);

        while (it.hasNext()) {
            Utility.dot(counter);
            String key = it.next();
            final String source = sortedD.get(key);
            final int fluff = key.charAt(key.length() - 1);
            key = key.substring(0, key.length() - fluff - 2);
            // String status = key.equals(lastKey) ? "*" : "";
            // lastKey = key;
            // log.println(source);
            char extra = source.charAt(source.length() - 1);
            if (UCharacter.isHighSurrogate(extra)) { // restore
                continue;
            }

            String clipped = source.substring(0, source.length() - 1);
            if (clipped.charAt(0) == LOW_ACCENT && extra != LOW_ACCENT) {
                extra = LOW_ACCENT;
                clipped = source.substring(1);
            }
            if (UCharacter.isLowSurrogate(extra)) { // restore
                clipped = source;
            }
            if (!shortPrint) {
                log.print(Utility.hex(source));
                String name = ucd.getName(clipped);
                if (name == null) {
                    name = ucd.getName(clipped);
                    System.out.println("Null name for " + Utility.hex(source));
                }
                String quoteOperand = WriteCollationData.quoteOperand(clipped);
                if (RTL.containsSome(quoteOperand)) {
                    quoteOperand = '\u200E' + quoteOperand + '\u200E';
                }
                log.print(
                        ";\t# (" + quoteOperand + ") "
                                + name
                                + "\t"
                                + UCA.toString(key, level));
            } else {
                log.print(Utility.hex(source));
            }
            log.println();
        }

        log.close();
        sortedD.clear();
        System.out.println("Done");
    }

    private static void addStringX(String s, byte option, AppendToCe appendToCe) {
        if (hanNotInCPOrder != null) {
            String kDecomp = nfkd.normalize(s);
            if (hanNotInCPOrder.containsSome(kDecomp)) {
                return;
            }
        }
        final int firstChar = s.codePointAt(0);
        addStringY(s + 'a', option, appendToCe);
        addStringY(s + 'b', option, appendToCe);
        addStringY(s + '?', option, appendToCe);
        addStringY(s + 'A', option, appendToCe);
        addStringY(s + '!', option, appendToCe);
        if (option == UCA_Types.SHIFTED && collator.isVariable(firstChar)) {
            addStringY(s + LOW_ACCENT, option, appendToCe);
        }

        // NOW, if the character decomposes, or is a combining mark (non-zero),
        // try combinations

        if (ucd.getCombiningClass(firstChar) > 0
                || !nfd.isNormalized(s) && !UCD.isHangulSyllable(firstChar)) {
            // if it ends with a non-starter, try the decompositions.
            final String decomp = nfd.normalize(s);
            if (ucd.getCombiningClass(decomp.codePointBefore(decomp.length())) > 0) {
                if (canIt == null) {
                    canIt = new CanonicalIterator(".");
                }
                canIt.setSource(s + LOW_ACCENT);
                int limit = 4;
                for (String can = canIt.next(); can != null; can = canIt.next()) {
                    if (s.equals(can)) {
                        continue;
                    }
                    if (--limit < 0) {
                        continue; // just include a sampling
                    }
                    addStringY(can, option, appendToCe);
                    // System.out.println(addCounter++ + " Adding " +
                    // Default.ucd.getCodeAndName(can));
                }
            }
        }
        if (UTF16.hasMoreCodePointsThan(s, 1)) {
            for (int i = 1; i < s.length(); ++i) {
                if (UTF16.isLeadSurrogate(s.charAt(i - 1))) {
                    continue; // skip if in middle of supplementary
                }

                for (final String element : CONTRACTION_TEST) {
                    final String extra = s.substring(0, i) + element + s.substring(i);
                    addStringY(extra + 'a', option, appendToCe);
                    if (DEBUG) {
                        System.out.println(addCounter++ + " Adding " + ucd.getCodeAndName(extra));
                    }
                }
            }
        }
    }

    private static void addStringY(String s, byte option, AppendToCe appendToCe) {
        final String colDbase = collator.getSortKey(s, option, true, appendToCe);
        sortedD.put(colDbase, s);
    }
}
