package org.unicode.text.UCA;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.Counter;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.UCD;
import org.unicode.text.UCD.UCD_Types;
import org.unicode.text.utility.ChainException;
import org.unicode.text.utility.DualWriter;
import org.unicode.text.utility.OldEquivalenceClass;
import org.unicode.text.utility.Pair;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.test.util.CollectionUtilities;
import com.ibm.icu.text.CanonicalIterator;
import com.ibm.icu.text.UTF16;
import org.unicode.text.UCA.FractionalUCA.Variables;

public class FractionalUCA implements UCD_Types, UCA_Types {
    static PrintWriter fractionalLog;
    static boolean DEBUG = true;

    static class HighByteToScripts {
        ScriptSet[] highByteToScripts = new ScriptSet[256];
        {
            for (int i = 0; i < highByteToScripts.length; ++i) {
                highByteToScripts[i] = new ScriptSet();
            }
            // set special values
            highByteToScripts[0].scripts.add("TERMINATOR",1);
            highByteToScripts[1].scripts.add("LEVEL-SEPARATOR",1);
            highByteToScripts[2].scripts.add("FIELD-SEPARATOR",1);
            highByteToScripts[3].scripts.add("COMPRESSION",1);
            //highByteToScripts[4].scripts.add("TAILORING_GAP",1);
            for (int i = FractionalUCA.Variables.IMPLICIT_BASE_BYTE; i <= FractionalUCA.Variables.IMPLICIT_MAX_BYTE; ++i) {
                highByteToScripts[i].scripts.add("IMPLICIT",1);
            }
            for (int i = FractionalUCA.Variables.IMPLICIT_MAX_BYTE+1; i < FractionalUCA.Variables.SPECIAL_BASE; ++i) {
                highByteToScripts[i].scripts.add("IMPLICIT",1);
            }
            for (int i = FractionalUCA.Variables.SPECIAL_BASE; i <= 0xFF; ++i) {
                highByteToScripts[i].scripts.add("SPECIAL",1);
            }
        }

        void addScriptsIn(long primary, String value) {
            for (int shift = 24; shift >= 0; shift -= 8) {
                int b = 0xFF & (int)(primary >>> shift);
                if (b != 0) {
                    highByteToScripts[b].addScriptsIn(primary, value);
                    return;
                }
            }
            // don't add to 0
        }

        public String toHighBytesToScripts(boolean doScripts) {
            StringBuilder builder = new StringBuilder();
            Map<String,Counter<Integer>> map = new TreeMap();
            for (int i = 0; i < highByteToScripts.length; ++i) {
                addScriptCats(map, i, doScripts ? highByteToScripts[i].scripts : highByteToScripts[i].types);
            }
            appendScriptCatLine(builder, map, doScripts);
            return builder.toString();
        }

        private void addScriptCats(Map<String, Counter<Integer>> map, int i, Counter<String> scripts) {
            for (String script : scripts) {
                long count = scripts.get(script);
                Counter<Integer> bitSet = map.get(script);
                if (bitSet == null) map.put(script, bitSet = new Counter<Integer>());
                bitSet.add(i, count);
            }
        }

        private void appendScriptCatLine(StringBuilder builder, Map<String, Counter<Integer>> map, boolean doScripts) {
            for (String item : map.keySet()) {
                builder.append("[script\t").append(item).append('\t');
                Counter<Integer> counter2 = map.get(item);
                boolean first = true;
                for (Integer i : counter2) {
                    if (first) first = false; else builder.append(' ');

                    builder.append(Utility.hex(i, 2));
                    if (!doScripts) {
                        String stringScripts = CollectionUtilities.join(highByteToScripts[i].scripts.keySet(), " ");
                        if (stringScripts.length() != 0) {
                            builder.append('{').append(stringScripts).append("}");
                        }
                    }
                    builder.append('=').append(counter2.get(i));
                }
                builder.append("\t]\n");
            }
        }

        public String toString() {
            ScriptSet[] merged = cleanup();
            StringBuilder result = new StringBuilder();
            for (int k = 0; k < merged.length; ++k) {
                ScriptSet bitSet = merged[k];
                result.append("[top_byte\t" + Utility.hex(k,2) + "\t");
                bitSet.toString(result, false);
                result.append("\t]");
                result.append("\t#\t");
                highByteToScripts[k].toString(result, true);
                result.append("\n");
            }
            return result.toString();
        }

        private ScriptSet[] cleanup() {
            ScriptSet[] merged = new ScriptSet[256];

            for (int k = 0; k < highByteToScripts.length; ++k) {
                merged[k] = new ScriptSet(highByteToScripts[k]);
            }
            // Any bytes that share scripts need to be combined
            // brute force, because we don't care about speed
            boolean fixedOne = true;
            while(fixedOne) {
                fixedOne = false;
                for (int k = 1; k < merged.length; ++k) {
                    if (merged[k-1].intersects(merged[k]) 
                            && !merged[k-1].equals(merged[k])) {
                        merged[k-1].or(merged[k]);
                        merged[k] = merged[k-1];
                        fixedOne = true;
                    }
                }
            }
            return merged;
        }
    }

    static class FCE {
        static final long UNDEFINED_MAX = Long.MAX_VALUE;
        static final long UNDEFINED_MIN = Long.MIN_VALUE;
        long[] key;
        boolean max;
        boolean debugShow = false;
        String source;
        String title;

        FCE (boolean max, String title) {
            this.max = max;
            this.title = title;
            if (max) {
                key = new long[] {UNDEFINED_MIN, UNDEFINED_MIN, UNDEFINED_MIN};    // make small!
            } else {
                key = new long[] {UNDEFINED_MAX, UNDEFINED_MAX, UNDEFINED_MAX};
            }
        }

        /*
        FCE (boolean max, int primary, int secondary, int tertiary) {
            this(max);
            key[0] = fixWeight(primary);
            key[1] = fixWeight(secondary);
            key[2] = fixWeight(tertiary);
        }

        FCE (boolean max, int primary) {
            this(max);
            key[0] = primary & INT_MASK;
        }
         */

        boolean isUnset() {
            return key[0] == UNDEFINED_MIN || key[0] == UNDEFINED_MAX;
        }

        long fixWeight(int weight) {
            long result = weight & FractionalUCA.Variables.INT_MASK;
            if (result == 0) {
                return result;
            }
            while ((result & 0xFF000000) == 0) {
                result <<= 8; // shift to top
            }
            return result;
        }

        String formatFCE() {
            return formatFCE(false);
        }

        String formatFCE(boolean showEmpty) {
            String b0 = getBuffer(key[0], false);
            boolean key0Defined = key[0] != UNDEFINED_MIN && key[0] != UNDEFINED_MAX;
            if (showEmpty && b0.length() == 0) {
                b0 = "X";
            }

            String b1 = getBuffer(key[1], key0Defined);
            boolean key1Defined = key[1] != UNDEFINED_MIN && key[1] != UNDEFINED_MAX;
            if (b1.length() != 0) {
                b1 = " " + b1;
            } else if (showEmpty) {
                b1 = " X";
            }

            String b2 = getBuffer(key[2], key0Defined || key1Defined);
            if (b2.length() != 0) {
                b2 = " " + b2;
            } else if (showEmpty) {
                b2 = " X";
            }

            return "[" + b0 + "," + b1  + "," + b2 + "]";
        }

        String getBuffer(long val, boolean haveHigher) {
            if (val == UNDEFINED_MIN) {
                return "?";
            } 
            if (val == UNDEFINED_MAX) {
                if (haveHigher) {
                    val = FractionalUCA.Variables.COMMON << 24;
                } else {
                    return "?";
                }
            }
            StringBuffer result = new StringBuffer();
            FractionalUCA.hexBytes(val, result);
            return result.toString();
        }

        long getValue(int zeroBasedLevel) {
            return key[zeroBasedLevel];
        }

        String getSource() {
            return source;
        }

        public String toString() {
            return toString(false);
        }

        String toString(boolean showEmpty) {
            String src = source.length() == 0 ? "CONSTRUCTED" : Default.ucd().getCodeAndName(source);
            return "[" + (max ? "last " : "first ") + title + " " + formatFCE(showEmpty) + "] # " + src;
        }

        void setValue(int npInt, int nsInt, int ntInt, String source) {
            if (debugShow) {
                System.out.println("Setting FCE: " 
                        + Utility.hex(npInt) + ", "  + Utility.hex(nsInt) + ", "  + Utility.hex(ntInt));
            }
            // to get the sign right!
            long np = fixWeight(npInt);
            long ns = fixWeight(nsInt);
            long nt = fixWeight(ntInt);
            if (max) {
                // return if the key is LEQ
                if (np < key[0]) {
                    return;
                }
                if (np == key[0]) {
                    if (ns < key[1]) {
                        return;
                    }
                    if (ns == key[1]) {
                        if (nt <= key[2]) {
                            return;
                        }
                    }
                }
            } else {
                // return if the key is GEQ
                if (np > key[0]) {
                    return;
                }
                if (np == key[0]) {
                    if (ns > key[1]) {
                        return;
                    }
                    if (ns == key[1]) {
                        if (nt >= key[2]) {
                            return;
                        }
                    }
                }
            }
            // we didn't bail, so reset!
            key[0] = np;
            key[1] = ns;
            key[2] = nt;
            this.source = source;
        }
    }

    static class Variables {
        static int variableHigh = 0;
        static final int COMMON = 5;

        static int gapForA = 0;
        static final long INT_MASK = 0xFFFFFFFFL;

        static final int  TOP                = 0xA0;
        static final int  SPECIAL_BASE       = 0xF0;
        static final int  BYTES_TO_AVOID     = 3, OTHER_COUNT = 256 - BYTES_TO_AVOID, LAST_COUNT = OTHER_COUNT / 2, LAST_COUNT2 = OTHER_COUNT / 21,
        IMPLICIT_3BYTE_COUNT = 1;
        static final int  IMPLICIT_BASE_BYTE = 0xE0;
        static final int  IMPLICIT_MAX_BYTE  = IMPLICIT_BASE_BYTE + 4;
        static final int  IMPLICIT_4BYTE_BOUNDARY = IMPLICIT_3BYTE_COUNT * OTHER_COUNT * LAST_COUNT, LAST_MULTIPLIER = OTHER_COUNT / LAST_COUNT,
        LAST2_MULTIPLIER = OTHER_COUNT / LAST_COUNT2, IMPLICIT_BASE_3BYTE = (IMPLICIT_BASE_BYTE << 24) + 0x030300,
        IMPLICIT_BASE_4BYTE = ((IMPLICIT_BASE_BYTE + IMPLICIT_3BYTE_COUNT) << 24) + 0x030303;

        // GET IMPLICIT PRIMARY WEIGHTS
        // Return value is left justified primary key

        static final int secondaryDoubleStart = 0xD0;
        static final int MARK_CODE_POINT = 0x40000000;

        static final byte MULTIPLES = 0x20, COMPRESSED = 0x40, OTHER_MASK = 0x1F;
        static final BitSet compressSet = new BitSet();

    }

    static void writeFractionalUCA(String filename) throws IOException {
        FractionalUCA.HighByteToScripts highByteToScripts = new FractionalUCA.HighByteToScripts();

        FractionalUCA.checkImplicit();
        FractionalUCA.checkFixes();

        FractionalUCA.Variables.variableHigh = WriteCollationData.collator.getVariableHigh() >> 16;
        BitSet secondarySet = WriteCollationData.collator.getWeightUsage(2);

        // HACK for CJK
        secondarySet.set(0x0040);

        int subtotal = 0;
        System.out.println("Fixing Secondaries");
        FractionalUCA.compactSecondary = new int[secondarySet.size()];
        for (int secondary = 0; secondary < FractionalUCA.compactSecondary.length; ++secondary) {
            if (secondarySet.get(secondary)) {
                FractionalUCA.compactSecondary[secondary] = subtotal++;
                /*System.out.println("compact[" + Utility.hex(secondary)
                        + "]=" + Utility.hex(compactSecondary[secondary])
                        + ", " + Utility.hex(fixSecondary(secondary)));*/
            }
        }
        System.out.println();

        //TO DO: find secondaries that don't overlap, and reassign

        System.out.println("Finding Bumps");        
        char[] representatives = new char[65536];
        FractionalUCA.findBumps(representatives);

        System.out.println("Fixing Primaries");
        BitSet primarySet = WriteCollationData.collator.getWeightUsage(1);        

        FractionalUCA.primaryDelta = new int[65536];

        // start at 1 so zero stays zero.
        for (int primary = 1; primary < 0xFFFF; ++primary) {
            if (primarySet.get(primary)) {
                FractionalUCA.primaryDelta[primary] = 2;
            } else if (primary == 0x1299) {
                System.out.println("WHOOPS! Missing weight");
            }
        }

        int bumpNextToo = 0;

        subtotal = (FractionalUCA.Variables.COMMON << 8) + FractionalUCA.Variables.COMMON; // skip forbidden bytes, leave gap
        int lastValue = 0;

        // start at 1 so zero stays zero.
        for (int primary = 1; primary < 0xFFFF; ++primary) {
            if (FractionalUCA.primaryDelta[primary] != 0) {

                // special handling for Jamo 3-byte forms

                if (isOldJamo(primary)) {
                    if (DEBUG) {
                        System.out.print("JAMO: " + Utility.hex(lastValue));
                    }
                    if ((lastValue & 0xFF0000) == 0) { // lastValue was 2-byte form
                        subtotal += FractionalUCA.primaryDelta[primary];  // we convert from relative to absolute
                        lastValue = FractionalUCA.primaryDelta[primary] = (subtotal << 8) + 0x10; // make 3 byte, leave gap
                    } else { // lastValue was 3-byte form
                        lastValue = FractionalUCA.primaryDelta[primary] = lastValue + 3;
                        int lastByte = lastValue&0xFF;
                        if (lastByte < 3) {
                            lastValue = FractionalUCA.primaryDelta[primary] = lastValue + 3;
                        }
                    }
                    if (DEBUG) {
                        System.out.println(" => " + Utility.hex(lastValue));
                    }
                    continue;
                }

                subtotal += FractionalUCA.primaryDelta[primary];  // we convert from relative to absolute

                if (FractionalUCA.singles.get(primary)) { 
                    subtotal = (subtotal & 0xFF00) + 0x100;
                    if (primary == FractionalUCA.Variables.gapForA) {
                        subtotal += 0x200;
                    }
                    if (bumpNextToo == 0x40) {
                        subtotal += 0x100; // make sure of gap between singles!!!
                    }
                    bumpNextToo = 0x40;
                } else if (primary > FractionalUCA.Variables.variableHigh) {
                    FractionalUCA.Variables.variableHigh = 0xFFFF; // never do again!
                    subtotal = (subtotal & 0xFF00) + 0x320 + bumpNextToo;
                    bumpNextToo = 0;
                } else if (bumpNextToo > 0 || FractionalUCA.bumps.get(primary)) {
                    subtotal = ((subtotal + 0x20) & 0xFF00) + 0x120 + bumpNextToo;
                    bumpNextToo = 0;
                } else {
                    int lastByte = subtotal & 0xFF;
                    // skip all values of FF, 00, 01, 02,
                    if (0 <= lastByte && lastByte < FractionalUCA.Variables.COMMON || lastByte == 0xFF) {
                        subtotal = ((subtotal + 1) & 0xFFFFFF00) + FractionalUCA.Variables.COMMON; // skip
                    }
                }
                lastValue = FractionalUCA.primaryDelta[primary] = subtotal;
            }
            // fixup for Kanji
            /*

                // WE DROP THIS: we are skipping all CJK values above, and will fix them separately

                int fixedCompat = remapUCA_CompatibilityIdeographToCp(primary);
                if (isFixedIdeograph(fixedCompat)) {
                    int CE = getImplicitPrimary(fixedCompat);

                    lastValue = primaryDelta[primary] = CE >>> 8; 
                }
             */
            //if ((primary & 0xFF) == 0) System.out.println(Utility.hex(primary) + " => " + hexBytes(primaryDelta[primary]));
        }


        // now translate!!
        String highCompat = UTF16.valueOf(0x2F805);

        System.out.println("Sorting");
        Map ordered = new TreeMap();
        Set contentsForCanonicalIteration = new TreeSet();
        UCA.UCAContents ucac = WriteCollationData.collator.getContents(UCA.FIXED_CE, null);
        int ccounter = 0;
        while (true) {
            Utility.dot(ccounter++);
            String s = ucac.next();
            if (s == null) {
                break;
            }
            if (s.equals("\uFA36") || s.equals("\uF900") || s.equals("\u2ADC") || s.equals(highCompat)) {
                System.out.println(" * " + WriteCollationData.ucd.getCodeAndName(s));
            }
            contentsForCanonicalIteration.add(s);
            ordered.put(WriteCollationData.collator.getSortKey(s, UCA.NON_IGNORABLE) + '\u0000' + s, s);
        }

        // Add canonically equivalent characters!!
        System.out.println("Start Adding canonical Equivalents2");
        int canCount = 0;

        System.out.println("Add missing decomposibles and non-characters");
        for (int i = 0; i < 0x10FFFF; ++i) {
            if (!WriteCollationData.ucd.isNoncharacter(i)) {
                if (!WriteCollationData.ucd.isAllocated(i)) {
                    continue;
                }
                if (Default.nfd().isNormalized(i)) {
                    continue;
                }
                if (WriteCollationData.ucd.isHangulSyllable(i)) {
                    continue;
                    //if (collator.getCEType(i) >= UCA.FIXED_CE) continue;
                }
            }
            String s = UTF16.valueOf(i);
            if (contentsForCanonicalIteration.contains(s)) {
                continue; // skip if already present
            }
            contentsForCanonicalIteration.add(s);
            ordered.put(WriteCollationData.collator.getSortKey(s, UCA.NON_IGNORABLE) + '\u0000' + s, s);
            System.out.println(" + " + WriteCollationData.ucd.getCodeAndName(s));
            canCount++;
        }

        Set additionalSet = new HashSet();
        System.out.println("Loading canonical iterator");
        if (WriteCollationData.canIt == null) {
            WriteCollationData.canIt = new CanonicalIterator(".");
        }
        Iterator it2 = contentsForCanonicalIteration.iterator();
        System.out.println("Adding any FCD equivalents that have different sort keys");
        while (it2.hasNext()) {
            String key = (String)it2.next();
            if (key == null) {
                System.out.println("Null Key");
                continue;
            }
            WriteCollationData.canIt.setSource(key);

            boolean first = true;
            while (true) {
                String s = WriteCollationData.canIt.next();
                if (s == null) {
                    break;
                }
                if (s.equals(key)) {
                    continue;
                }
                if (contentsForCanonicalIteration.contains(s)) {
                    continue;
                }
                if (additionalSet.contains(s)) {
                    continue;
                }


                // Skip anything that is not FCD.
                if (!Default.nfd().isFCD(s)) {
                    continue;
                }

                // We ONLY add if the sort key would be different
                // Than what we would get if we didn't decompose!!
                String sortKey = WriteCollationData.collator.getSortKey(s, UCA.NON_IGNORABLE);
                String nonDecompSortKey = WriteCollationData.collator.getSortKey(s, UCA.NON_IGNORABLE, false);
                if (sortKey.equals(nonDecompSortKey)) {
                    continue;
                }

                if (first) {
                    System.out.println(" " + WriteCollationData.ucd.getCodeAndName(key));
                    first = false;
                }
                System.out.println(" => " + WriteCollationData.ucd.getCodeAndName(s));
                System.out.println("    old: " + WriteCollationData.collator.toString(nonDecompSortKey));
                System.out.println("    new: " + WriteCollationData.collator.toString(sortKey));
                canCount++;
                additionalSet.add(s);
                ordered.put(sortKey + '\u0000' + s, s);
            }
        }
        System.out.println("Done Adding canonical Equivalents -- added " + canCount);
        /*

            for (int ch = 0; ch < 0x10FFFF; ++ch) {
                Utility.dot(ch);
                byte type = collator.getCEType(ch);
                if (type >= UCA.FIXED_CE && !nfd.hasDecomposition(ch))
                    continue;
                }
                String s = org.unicode.text.UTF16.valueOf(ch);
                ordered.put(collator.getSortKey(s, UCA.NON_IGNORABLE) + '\u0000' + s, s);
            }

            Hashtable multiTable = collator.getContracting();
            Enumeration enum = multiTable.keys();
            int ecount = 0;
            while (enum.hasMoreElements()) {
                Utility.dot(ecount++);
                String s = (String)enum.nextElement();
                ordered.put(collator.getSortKey(s, UCA.NON_IGNORABLE) + '\u0000' + s, s);
            }
         */
        // JUST FOR TESTING
        if (false) {
            String sample = "\u3400\u3401\u4DB4\u4DB5\u4E00\u4E01\u9FA4\u9FA5\uAC00\uAC01\uD7A2\uD7A3";
            for (int i = 0; i < sample.length(); ++i) {
                String s = sample.substring(i, i+1);
                ordered.put(WriteCollationData.collator.getSortKey(s, UCA.NON_IGNORABLE) + '\u0000' + s, s);
            }
        }

        Utility.fixDot();
        System.out.println("Writing");
        PrintWriter shortLog = new PrintWriter(new BufferedWriter(new FileWriter(WriteCollationData.collator.getUCA_GEN_DIR() + filename + "_SHORT.txt"), 32*1024));
        PrintWriter longLog = new PrintWriter(new BufferedWriter(new FileWriter(WriteCollationData.collator.getUCA_GEN_DIR() + filename + ".txt"), 32*1024));
        fractionalLog = new PrintWriter(new DualWriter(shortLog, longLog));

        PrintWriter summary = new PrintWriter(new BufferedWriter(new FileWriter(WriteCollationData.collator.getUCA_GEN_DIR() + filename + "_summary.txt"), 32*1024));
        //log.println("[Variable Low = " + UCA.toString(collator.getVariableLow()) + "]");
        //log.println("[Variable High = " + UCA.toString(collator.getVariableHigh()) + "]");

        int[] ces = new int[100];

        StringBuffer newPrimary = new StringBuffer();
        StringBuffer newSecondary = new StringBuffer();
        StringBuffer newTertiary = new StringBuffer();
        StringBuffer oldStr = new StringBuffer();

        OldEquivalenceClass secEq = new OldEquivalenceClass("\r\n#", 2, true);
        OldEquivalenceClass terEq = new OldEquivalenceClass("\r\n#", 2, true);
        String[] sampleEq = new String[500];
        int[] sampleLen = new int[500];

        Iterator it = ordered.keySet().iterator();
        int oldFirstPrimary = UCA.getPrimary(UCA.TERMINATOR);
        boolean wasVariable = false;

        fractionalLog.println("# Fractional UCA Table, generated from standard UCA");
        fractionalLog.println("# " + WriteCollationData.getNormalDate());
        fractionalLog.println("# VERSION: UCA=" + WriteCollationData.collator.getDataVersion() + ", UCD=" + WriteCollationData.collator.getUCDVersion());
        fractionalLog.println();
        fractionalLog.println("# Generated processed version, as described in ICU design document.");
        fractionalLog.println("# NOTES");
        fractionalLog.println("#  - Bugs in UCA data are NOT FIXED, except for the following problems:");
        fractionalLog.println("#    - canonical equivalents are decomposed directly (some beta UCA are wrong).");
        fractionalLog.println("#    - overlapping variable ranges are fixed.");
        fractionalLog.println("#  - Format is as follows:");
        fractionalLog.println("#      <codepoint> (' ' <codepoint>)* ';' ('L' | 'S') ';' <fractionalCE>+ ' # ' <UCA_CE> '# ' <name> ");
        fractionalLog.println("#    - zero weights are not printed");
        fractionalLog.println("#    - S: contains at least one lowercase or SMALL kana");
        fractionalLog.println("#    - L: otherwise");
        fractionalLog.println("#    - Different primaries are separated by a blank line.");
        fractionalLog.println("# WARNING");
        fractionalLog.println("#  - Differs from previous version in that MAX value was introduced at 1F.");
        fractionalLog.println("#    All tertiary values are shifted down by 1, filling the gap at 7!");
        fractionalLog.println();
        fractionalLog.println("[UCA version = " + WriteCollationData.collator.getDataVersion() + "]");


        String lastChr = "";
        int lastNp = 0;
        boolean doVariable = false;
        char[] codeUnits = new char[100];

        FractionalUCA.FCE firstTertiaryIgnorable = new FractionalUCA.FCE(false, "tertiary ignorable");
        FractionalUCA.FCE lastTertiaryIgnorable = new FractionalUCA.FCE(true, "tertiary ignorable");

        FractionalUCA.FCE firstSecondaryIgnorable = new FractionalUCA.FCE(false, "secondary ignorable");
        FractionalUCA.FCE lastSecondaryIgnorable = new FractionalUCA.FCE(true, "secondary ignorable");

        FractionalUCA.FCE firstTertiaryInSecondaryNonIgnorable = new FractionalUCA.FCE(false, "tertiary in secondary non-ignorable");
        FractionalUCA.FCE lastTertiaryInSecondaryNonIgnorable = new FractionalUCA.FCE(true, "tertiary in secondary non-ignorable");

        FractionalUCA.FCE firstPrimaryIgnorable = new FractionalUCA.FCE(false, "primary ignorable");
        FractionalUCA.FCE lastPrimaryIgnorable = new FractionalUCA.FCE(true, "primary ignorable");

        FractionalUCA.FCE firstSecondaryInPrimaryNonIgnorable = new FractionalUCA.FCE(false, "secondary in primary non-ignorable");
        FractionalUCA.FCE lastSecondaryInPrimaryNonIgnorable = new FractionalUCA.FCE(true, "secondary in primary non-ignorable");

        FractionalUCA.FCE firstVariable = new FractionalUCA.FCE(false, "variable");
        FractionalUCA.FCE lastVariable = new FractionalUCA.FCE(true, "variable");

        FractionalUCA.FCE firstNonIgnorable = new FractionalUCA.FCE(false, "regular");
        FractionalUCA.FCE lastNonIgnorable = new FractionalUCA.FCE(true, "regular");

        FractionalUCA.FCE firstImplicitFCE = new FractionalUCA.FCE(false, "implicit");
        FractionalUCA.FCE lastImplicitFCE = new FractionalUCA.FCE(true, "implicit");

        FractionalUCA.FCE firstTrailing = new FractionalUCA.FCE(false, "trailing");
        FractionalUCA.FCE lastTrailing = new FractionalUCA.FCE(true, "trailing");

        Map fractBackMap = new TreeMap();

        while (it.hasNext()) {
            Object sortKey = it.next();
            String chr = (String)ordered.get(sortKey);            

            // get CEs and fix
            int len = WriteCollationData.collator.getCEs(chr, true, ces);
            int firstPrimary = UCA.getPrimary(ces[0]);
            if (firstPrimary != oldFirstPrimary) {
                fractionalLog.println();
                boolean isVariable = WriteCollationData.collator.isVariable(ces[0]);
                if (isVariable != wasVariable) {
                    if (isVariable) {
                        fractionalLog.println("# START OF VARIABLE SECTION!!!");
                        summary.println("# START OF VARIABLE SECTION!!!");
                    } else {
                        fractionalLog.println("[variable top = " + Utility.hex(FractionalUCA.primaryDelta[oldFirstPrimary]) + "] # END OF VARIABLE SECTION!!!");
                        doVariable = true;
                    }
                    fractionalLog.println();
                }
                wasVariable = isVariable;
                oldFirstPrimary = firstPrimary;
            }
            oldStr.setLength(0);
            chr.getChars(0, chr.length(), codeUnits, 0);

            /* HACK
    006C 00B7; [42, 05, 05][, E5 B1, 05]	# [1262.0020.0002][0000.01AF.0002]	* LATIN SMALL LETTER L ...
    =>
    006C | 00B7; [, E5 B1, 05]	# [1262.0020.0002][0000.01AF.0002]	* LATIN SMALL LETTER L ...
    006C 0387; [42, 05, 05][, E5 B1, 05]	# [1262.0020.0002][0000.01AF.0002]	* LATIN SMALL LETTER L ...
    0140; [42, 05, 05][, E5 B1, 05]	# [1262.0020.0002][0000.01AF.0002]	* LATIN SMALL LETTER L WITH MIDDLE DOT
    004C 00B7; [42, 05, 8F][, E5 B1, 05]	# [1262.0020.0008][0000.01AF.0002]	* LATIN CAPITAL LETTER L ...
    004C 0387; [42, 05, 8F][, E5 B1, 05]	# [1262.0020.0008][0000.01AF.0002]	* LATIN CAPITAL LETTER L ...
    013F; [42, 05, 8F][, E5 B1, 05]	# [1262.0020.0008][0000.01AF.0002]	* LATIN CAPITAL LETTER L WITH MIDDLE DOT
             */
            boolean middleDotHack = chr.length() == 2
            && (codeUnits[0] == 'l' || codeUnits[0] == 'L')
            && (codeUnits[1] == '\u00B7' || codeUnits[1] == '\u0387');

            fractionalLog.print(com.ibm.icu.impl.Utility.hex(chr, 4, middleDotHack ? " | " : " ") + "; ");            

            //      if (middleDotHack) {
            //        log.print(Utility.hex(codeUnits, 0, chr.length(), middleDotHack ? " | " : " ") + "; ");            
            //      } else {
            //        log.print(Utility.hex(codeUnits, 0, chr.length(), " ") + "; ");
            //      }
            boolean nonePrinted = true;
            boolean isFirst = true;

            int firstCE = middleDotHack ? 1 : 0;

            for (int q = firstCE; q < len; ++q) {
                nonePrinted = false;
                newPrimary.setLength(0);
                newSecondary.setLength(0);
                newTertiary.setLength(0);

                int pri = UCA.getPrimary(ces[q]);
                int sec = UCA.getSecondary(ces[q]); 
                int ter = UCA.getTertiary(ces[q]);

                oldStr.append(CEList.toString(ces[q]));// + "," + Integer.toString(ces[q],16);

                // special treatment for unsupported!

                if (UCA.isImplicitLeadPrimary(pri)) {
                    if (DEBUG) {
                        System.out.println("DEBUG: " + CEList.toString(ces, len) 
                                + ", Current: " + q + ", " + WriteCollationData.ucd.getCodeAndName(chr));
                    }
                    ++q;
                    oldStr.append(CEList.toString(ces[q]));// + "," + Integer.toString(ces[q],16);

                    int pri2 = UCA.getPrimary(ces[q]);
                    // get old code point

                    int cp = UCA.ImplicitToCodePoint(pri, pri2);

                    // double check results!

                    int[] testImplicit = new int[2];
                    WriteCollationData.collator.CodepointToImplicit(cp, testImplicit);
                    boolean gotError = pri != testImplicit[0] || pri2 != testImplicit[1];
                    if (gotError) {
                        System.out.println("ERROR");
                    }
                    if (DEBUG || gotError) {
                        System.out.println("Computing Unsupported CP as: "
                                + Utility.hex(pri)
                                + ", " + Utility.hex(pri2)
                                + " => " + Utility.hex(cp)
                                + " => " + Utility.hex(testImplicit[0])
                                + ", " + Utility.hex(testImplicit[1])
                                // + ", " + Utility.hex(fixPrimary(pri) & INT_MASK)
                        );
                    }

                    pri = cp | FractionalUCA.Variables.MARK_CODE_POINT;
                }

                if (sec != 0x20) {
                    boolean changed = secEq.add(new Integer(sec), new Integer(pri));
                }
                if (ter != 0x2) {
                    boolean changed = terEq.add(new Integer(ter), new Integer((pri << 16) | sec));
                }

                if (sampleEq[sec] == null || sampleLen[sec] > len) {
                    sampleEq[sec] = chr;
                    sampleLen[sec] = len;
                }
                if (sampleEq[ter] == null || sampleLen[sec] > len) {
                    sampleEq[ter] = chr;
                    sampleLen[sec] = len;
                }

                if ((pri & FractionalUCA.Variables.MARK_CODE_POINT) == 0 && pri == 0) {
                    if (chr.equals("\u01C6")) {
                        System.out.println("At dz-caron");
                    }
                    Integer key = new Integer(ces[q]);
                    Pair value = (Pair) fractBackMap.get(key);
                    if (value == null
                            || (len < ((Integer)(value.first)).intValue())) {
                        fractBackMap.put(key, new Pair(new Integer(len), chr));
                    }
                }

                // int oldPrimaryValue = UCA.getPrimary(ces[q]);
                int np = FractionalUCA.fixPrimary(pri);
                int ns = FractionalUCA.fixSecondary(sec);
                int nt = FractionalUCA.fixTertiary(ter);

                try {
                    FractionalUCA.hexBytes(np, newPrimary);
                    if (q == firstCE) { // only look at first one
                        highByteToScripts.addScriptsIn(np, chr);
                    }
                    FractionalUCA.hexBytes(ns, newSecondary);
                    FractionalUCA.hexBytes(nt, newTertiary);
                } catch (Exception e) {
                    throw new ChainException("Character is {0}", new String[] {Utility.hex(chr)}, e);
                }
                if (isFirst) {
                    if (!FractionalUCA.sameTopByte(np, lastNp)) {
                        summary.println("Last:  " + Utility.hex(lastNp & FractionalUCA.Variables.INT_MASK) + " " + WriteCollationData.ucd.getCodeAndName(UTF16.charAt(lastChr,0)));
                        summary.println();
                        if (doVariable) {
                            doVariable = false;
                            summary.println("[variable top = " + Utility.hex(FractionalUCA.primaryDelta[firstPrimary]) + "] # END OF VARIABLE SECTION!!!");
                            summary.println();
                        }
                        summary.println("First: " + Utility.hex(np & FractionalUCA.Variables.INT_MASK) + ", " + WriteCollationData.ucd.getCodeAndName(UTF16.charAt(chr,0)));
                    }
                    lastNp = np;
                    isFirst = false;
                }
                fractionalLog.print("[" + newPrimary 
                        + ", " + newSecondary 
                        + ", " + newTertiary 
                        + "]");

                // RECORD STATS
                // but ONLY if we are not part of an implicit

                if ((pri & FractionalUCA.Variables.MARK_CODE_POINT) == 0) {
                    if (np != 0) {
                        firstSecondaryInPrimaryNonIgnorable.setValue(0, ns, 0, chr);
                        lastSecondaryInPrimaryNonIgnorable.setValue(0, ns, 0, chr);
                    }
                    if (ns != 0) {
                        firstTertiaryInSecondaryNonIgnorable.setValue(0, 0, nt & 0x3F, chr);
                        lastTertiaryInSecondaryNonIgnorable.setValue(0, 0, nt & 0x3F, chr);
                    }
                    if (np == 0 && ns == 0) {
                        firstSecondaryIgnorable.setValue(np, ns, nt, chr);
                        lastSecondaryIgnorable.setValue(np, ns, nt, chr); 
                    } else if (np == 0) {
                        firstPrimaryIgnorable.setValue(np, ns, nt, chr);
                        lastPrimaryIgnorable.setValue(np, ns, nt, chr); 
                    } else if (WriteCollationData.collator.isVariable(ces[q])) {
                        firstVariable.setValue(np, ns, nt, chr);
                        lastVariable.setValue(np, ns, nt, chr); 
                    } else if (UCA.getPrimary(ces[q]) > UCA_Types.UNSUPPORTED_LIMIT) {        // Trailing (none currently)
                        System.out.println("Trailing: " 
                                + WriteCollationData.ucd.getCodeAndName(chr) + ", "
                                + CEList.toString(ces[q]) + ", " 
                                + Utility.hex(pri) + ", " 
                                + Utility.hex(UCA_Types.UNSUPPORTED_LIMIT));
                        firstTrailing.setValue(np, ns, nt, chr);
                        lastTrailing.setValue(np, ns, nt, chr); 
                    } else {
                        firstNonIgnorable.setValue(np, ns, nt, chr);
                        lastNonIgnorable.setValue(np, ns, nt, chr); 
                    }
                }
            }
            if (nonePrinted) {
                fractionalLog.print("[,,]");
                oldStr.append(CEList.toString(0));
            }
            String name = UTF16.hasMoreCodePointsThan(chr, 1) 
            ? WriteCollationData.ucd.getName(UTF16.charAt(chr, 0)) + " ..."
                    : WriteCollationData.ucd.getName(chr);
            longLog.print("\t# " + oldStr + "\t* " + name);
            fractionalLog.println();
            lastChr = chr;
        }


        // ADD HOMELESS COLLATION ELEMENTS
        fractionalLog.println();
        fractionalLog.println("# HOMELESS COLLATION ELEMENTS");
        char fakeTrail = 'a';
        Iterator it3 = fractBackMap.keySet().iterator();
        while (it3.hasNext()) {
            Integer key = (Integer) it3.next();
            Pair pair = (Pair) fractBackMap.get(key);
            if (((Integer)pair.first).intValue() < 2) {
                continue;
            }
            String sample = (String)pair.second;

            int ce = key.intValue();

            int np = FractionalUCA.fixPrimary(UCA.getPrimary(ce));
            int ns = FractionalUCA.fixSecondary(UCA.getSecondary(ce));
            int nt = FractionalUCA.fixTertiary(UCA.getTertiary(ce));

            newPrimary.setLength(0);
            newSecondary.setLength(0);
            newTertiary.setLength(0);

            FractionalUCA.hexBytes(np, newPrimary);
            highByteToScripts.addScriptsIn(np, sample);
            FractionalUCA.hexBytes(ns, newSecondary);
            FractionalUCA.hexBytes(nt, newTertiary);

            fractionalLog.print(Utility.hex('\uFDD0' + "" + (char)(fakeTrail++)) + "; " 
            + "[, " + newSecondary + ", " + newTertiary + "]");
            longLog.print("\t# " + WriteCollationData.collator.getCEList(sample, true) + "\t* " + WriteCollationData.ucd.getCodeAndName(sample));
            fractionalLog.println();
        }

        // Since the UCA doesn't have secondary ignorables, fake them.
        int fakeTertiary = 0x3F03;
        if (firstSecondaryIgnorable.isUnset()) {
            System.out.println("No first/last secondary ignorable: resetting to HARD CODED, adding homeless");
            //long bound = lastTertiaryInSecondaryNonIgnorable.getValue(2);
            firstSecondaryIgnorable.setValue(0,0,fakeTertiary,"");
            lastSecondaryIgnorable.setValue(0,0,fakeTertiary,"");
            System.out.println(firstSecondaryIgnorable.formatFCE());
            // also add homeless
            newTertiary.setLength(0);
            FractionalUCA.hexBytes(fakeTertiary, newTertiary);
            fractionalLog.println(Utility.hex('\uFDD0' + "" + (char)(fakeTrail++)) + "; " 
                    + "[,, " + newTertiary 
                    + "]\t# CONSTRUCTED FAKE SECONDARY-IGNORABLE");
        }

        int firstImplicit = FractionalUCA.getImplicitPrimary(UCD_Types.CJK_BASE);
        int lastImplicit = FractionalUCA.getImplicitPrimary(0x10FFFD);

        fractionalLog.println();
        fractionalLog.println("# VALUES BASED ON UCA");

        if (firstTertiaryIgnorable.isUnset()) {
            firstTertiaryIgnorable.setValue(0,0,0,"");
            lastTertiaryIgnorable.setValue(0,0,0,"");
            System.out.println(firstSecondaryIgnorable.formatFCE());
        }

        fractionalLog.println(firstTertiaryIgnorable);
        fractionalLog.println(lastTertiaryIgnorable);

        fractionalLog.println("# Warning: Case bits are masked in the following");

        fractionalLog.println(firstTertiaryInSecondaryNonIgnorable.toString(true));
        fractionalLog.println(lastTertiaryInSecondaryNonIgnorable.toString(true));

        fractionalLog.println(firstSecondaryIgnorable);
        fractionalLog.println(lastSecondaryIgnorable);

        if (lastTertiaryInSecondaryNonIgnorable.getValue(2) >= firstSecondaryIgnorable.getValue(2)) {
            fractionalLog.println("# FAILURE: Overlap of tertiaries");
        }

        fractionalLog.println(firstSecondaryInPrimaryNonIgnorable.toString(true));
        fractionalLog.println(lastSecondaryInPrimaryNonIgnorable.toString(true));

        fractionalLog.println(firstPrimaryIgnorable);
        fractionalLog.println(lastPrimaryIgnorable);

        if (lastSecondaryInPrimaryNonIgnorable.getValue(1) >= firstPrimaryIgnorable.getValue(1)) {
            fractionalLog.println("# FAILURE: Overlap of secondaries");
        }

        fractionalLog.println(firstVariable);
        fractionalLog.println(lastVariable);

        fractionalLog.println(firstNonIgnorable);
        fractionalLog.println(lastNonIgnorable);

        firstImplicitFCE.setValue(firstImplicit, FractionalUCA.Variables.COMMON, FractionalUCA.Variables.COMMON, "");
        lastImplicitFCE.setValue(lastImplicit, FractionalUCA.Variables.COMMON, FractionalUCA.Variables.COMMON, "");

        fractionalLog.println(firstImplicitFCE); // "[first implicit " + (new FCE(false,firstImplicit, COMMON<<24, COMMON<<24)).formatFCE() + "]");
        fractionalLog.println(lastImplicitFCE); // "[last implicit " + (new FCE(false,lastImplicit, COMMON<<24, COMMON<<24)).formatFCE() + "]");

        if (firstTrailing.isUnset()) {
            System.out.println("No first/last trailing: resetting");
            firstTrailing.setValue(FractionalUCA.Variables.IMPLICIT_MAX_BYTE+1, FractionalUCA.Variables.COMMON, FractionalUCA.Variables.COMMON, "");
            lastTrailing.setValue(FractionalUCA.Variables.IMPLICIT_MAX_BYTE+1, FractionalUCA.Variables.COMMON, FractionalUCA.Variables.COMMON, "");
            System.out.println(firstTrailing.formatFCE());        
        }

        fractionalLog.println(firstTrailing);
        fractionalLog.println(lastTrailing);
        fractionalLog.println();

        fractionalLog.println("# SCRIPT VALUES for HIGH BYTES (letters only)");
        fractionalLog.println(highByteToScripts.toString());
        fractionalLog.println();

        fractionalLog.println("# HIGH BYTES for SCRIPTS (letters only)");
        fractionalLog.println(highByteToScripts.toHighBytesToScripts(true));
        fractionalLog.println();

        fractionalLog.println("# HIGH BYTES for GENERAL CATEGORIES (non-letters only)");
        fractionalLog.println(highByteToScripts.toHighBytesToScripts(false));
        fractionalLog.println();


        fractionalLog.println();
        fractionalLog.println("# FIXED VALUES");

        fractionalLog.println("# superceded! [top "  + lastNonIgnorable.formatFCE() + "]");
        fractionalLog.println("[fixed first implicit byte " + Utility.hex(FractionalUCA.Variables.IMPLICIT_BASE_BYTE,2) + "]");
        fractionalLog.println("[fixed last implicit byte " + Utility.hex(FractionalUCA.Variables.IMPLICIT_MAX_BYTE,2) + "]");
        fractionalLog.println("[fixed first trail byte " + Utility.hex(FractionalUCA.Variables.IMPLICIT_MAX_BYTE+1,2) + "]");
        fractionalLog.println("[fixed last trail byte " + Utility.hex(FractionalUCA.Variables.SPECIAL_BASE-1,2) + "]");
        fractionalLog.println("[fixed first special byte " + Utility.hex(FractionalUCA.Variables.SPECIAL_BASE,2) + "]");
        fractionalLog.println("[fixed last special byte " + Utility.hex(0xFF,2) + "]");


        summary.println("Last:  " + Utility.hex(lastNp) + ", " + WriteCollationData.ucd.getCodeAndName(UTF16.charAt(lastChr, 0)));

        /*
            String sample = "\u3400\u3401\u4DB4\u4DB5\u4E00\u4E01\u9FA4\u9FA5\uAC00\uAC01\uD7A2\uD7A3";
            for (int i = 0; i < sample.length(); ++i) {
                char ch = sample.charAt(i);
                log.println(Utility.hex(ch) + " => " + Utility.hex(fixHan(ch))
                        + "          " + ucd.getName(ch));
            }
         */
        summary.println();
        summary.println("# First Implicit: " + Utility.hex(FractionalUCA.Variables.INT_MASK & FractionalUCA.getImplicitPrimary(0)));
        summary.println("# Last Implicit: " + Utility.hex(FractionalUCA.Variables.INT_MASK & FractionalUCA.getImplicitPrimary(0x10FFFF)));
        summary.println("# First CJK: " + Utility.hex(FractionalUCA.Variables.INT_MASK & FractionalUCA.getImplicitPrimary(0x4E00)));
        summary.println("# Last CJK: " + Utility.hex(FractionalUCA.Variables.INT_MASK & FractionalUCA.getImplicitPrimary(0xFA2F)));
        summary.println("# First CJK_A: " + Utility.hex(FractionalUCA.Variables.INT_MASK & FractionalUCA.getImplicitPrimary(0x3400)));
        summary.println("# Last CJK_A: " + Utility.hex(FractionalUCA.Variables.INT_MASK & FractionalUCA.getImplicitPrimary(0x4DBF)));

        boolean lastOne = false;
        for (int i = 0; i < 0x10FFFF; ++i) {
            boolean thisOne = WriteCollationData.ucd.isCJK_BASE(i) || WriteCollationData.ucd.isCJK_AB(i);
            if (thisOne != lastOne) {
                summary.println("# Implicit Cusp: CJK=" + lastOne + ": " + Utility.hex(i-1) + " => " + Utility.hex(FractionalUCA.Variables.INT_MASK & FractionalUCA.getImplicitPrimary(i-1)));
                summary.println("# Implicit Cusp: CJK=" + thisOne + ": " + Utility.hex(i) + " => " + Utility.hex(FractionalUCA.Variables.INT_MASK & FractionalUCA.getImplicitPrimary(i)));
                lastOne = thisOne;
            }
        }

        summary.println("Compact Secondary 153: " + FractionalUCA.compactSecondary[0x153]);
        summary.println("Compact Secondary 157: " + FractionalUCA.compactSecondary[0x157]);


        summary.println();
        summary.println("# Disjoint classes for Secondaries");
        summary.println("#" + secEq.toString());

        summary.println();
        summary.println("# Disjoint classes for Tertiaries");
        summary.println("#" + terEq.toString());

        summary.println();
        summary.println("# Example characters for each TERTIARY value");
        summary.println();
        summary.println("# UCA : (FRAC) CODE [    UCA CE    ] Name");
        summary.println();

        for (int i = 0; i < sampleEq.length; ++i) {
            if (sampleEq[i] == null) {
                continue;
            }
            if (i == 0x20) {
                summary.println();
                summary.println("# Example characters for each SECONDARY value");
                summary.println();
                summary.println("# UCA : (FRAC) CODE [    UCA CE    ] Name");
                summary.println();
            }
            int len = WriteCollationData.collator.getCEs(sampleEq[i], true, ces);
            int newval = i < 0x20 ? FractionalUCA.fixTertiary(i) : FractionalUCA.fixSecondary(i);
            summary.print("# " + Utility.hex(i) + ": (" + Utility.hex(newval) + ") "
                    + Utility.hex(sampleEq[i]) + " ");
            for (int q = 0; q < len; ++q) {
                summary.print(CEList.toString(ces[q]));
            }
            summary.println(" " + WriteCollationData.ucd.getName(sampleEq[i]));

        }
        fractionalLog.close();
        summary.close();
    }

    static void checkImplicit() {
        System.out.println("Starting Implicit Check");

        long oldPrimary = 0;
        int oldChar = -1;
        int oldSwap = -1;

        // test monotonically increasing

        for (int i = 0; i < 0x21FFFF; ++i) {
            long newPrimary = FractionalUCA.Variables.INT_MASK & FractionalUCA.getImplicitPrimaryFromSwapped(i);
            if (newPrimary < oldPrimary) {
                throw new IllegalArgumentException(Utility.hex(i) + ": overlap: "
                        + Utility.hex(oldChar) + " (" + Utility.hex(oldPrimary) + ")"
                        + " > " + Utility.hex(i) + "(" + Utility.hex(newPrimary) + ")");
            }
            oldPrimary = newPrimary;
        }

        FractionalUCA.showImplicit("# First CJK", CJK_BASE);
        FractionalUCA.showImplicit("# Last CJK", CJK_LIMIT-1);
        FractionalUCA.showImplicit("# First CJK-compat", CJK_COMPAT_USED_BASE);
        FractionalUCA.showImplicit("# Last CJK-compat", CJK_COMPAT_USED_LIMIT-1);
        FractionalUCA.showImplicit("# First CJK_A", CJK_A_BASE);
        FractionalUCA.showImplicit("# Last CJK_A", CJK_A_LIMIT-1);
        FractionalUCA.showImplicit("# First CJK_B", CJK_B_BASE);
        FractionalUCA.showImplicit("# Last CJK_B", CJK_B_LIMIT-1);
        FractionalUCA.showImplicit("# First CJK_C", CJK_C_BASE);
        FractionalUCA.showImplicit("# Last CJK_C", CJK_C_LIMIT-1);
        FractionalUCA.showImplicit("# First CJK_D", CJK_D_BASE);
        FractionalUCA.showImplicit("# Last CJK_D", CJK_D_LIMIT-1);
        FractionalUCA.showImplicit("# First Other Implicit", 0);
        FractionalUCA.showImplicit("# Last Other Implicit", 0x10FFFF);

        FractionalUCA.showImplicit3("# FIRST", 0);
        FractionalUCA.showImplicit3("# Boundary-1", FractionalUCA.Variables.IMPLICIT_4BYTE_BOUNDARY-1);
        FractionalUCA.showImplicit3("# Boundary00", FractionalUCA.Variables.IMPLICIT_4BYTE_BOUNDARY);
        FractionalUCA.showImplicit3("# Boundary+1", FractionalUCA.Variables.IMPLICIT_4BYTE_BOUNDARY+1);
        FractionalUCA.showImplicit3("# LAST", 0x21FFFF);


        for (int batch = 0; batch < 3; ++batch) {
            // init each batch
            oldPrimary = 0;
            oldChar = -1;

            for (int i = 0; i <= 0x10FFFF; ++i) {

                // separate the three groups

                if (WriteCollationData.ucd.isCJK_BASE(i) || CJK_COMPAT_USED_BASE <= i && i < CJK_COMPAT_USED_LIMIT) {
                    if (batch != 0) {
                        continue;
                    }
                } else if (WriteCollationData.ucd.isCJK_AB(i)) {
                    if (batch != 1) {
                        continue;
                    }
                } else if (batch != 2) {
                    continue;
                }


                // test swapping

                int currSwap = Implicit.swapCJK(i);
                if (currSwap < oldSwap) {
                    throw new IllegalArgumentException(Utility.hex(i) + ": overlap: "
                            + Utility.hex(oldChar) + " (" + Utility.hex(oldSwap) + ")"
                            + " > " + Utility.hex(i) + "(" + Utility.hex(currSwap) + ")");
                }


                long newPrimary = FractionalUCA.Variables.INT_MASK & FractionalUCA.getImplicitPrimary(i);

                // test correct values


                if (newPrimary < oldPrimary && oldChar != -1) {
                    throw new IllegalArgumentException(Utility.hex(i) + ": overlap: "
                            + Utility.hex(oldChar) + " (" + Utility.hex(oldPrimary) + ")"
                            + " > " + Utility.hex(i) + "(" + Utility.hex(newPrimary) + ")");
                }


                long b0 = (newPrimary >> 24) & 0xFF;
                long b1 = (newPrimary >> 16) & 0xFF;
                long b2 = (newPrimary >> 8) & 0xFF;
                long b3 = newPrimary & 0xFF;

                if (b0 < FractionalUCA.Variables.IMPLICIT_BASE_BYTE || b0 > FractionalUCA.Variables.IMPLICIT_MAX_BYTE  || b1 < 3 || b2 < 3 || b3 == 1 || b3 == 2) {
                    throw new IllegalArgumentException(Utility.hex(i) + ": illegal byte value: " + Utility.hex(newPrimary)
                            + ", " + Utility.hex(b1) + ", " + Utility.hex(b2) + ", " + Utility.hex(b3));
                }

                // print range to look at

                if (false) {
                    int b = i & 0xFF;
                    if (b == 255 || b == 0 || b == 1) {
                        System.out.println(Utility.hex(i) + " => " + Utility.hex(newPrimary));
                    }
                }
                oldPrimary = newPrimary;
                oldChar = i;
            }
        }
        System.out.println("Successful Implicit Check!!");
    }

    static void checkFixes() {
        System.out.println("Checking Secondary/Tertiary Fixes");
        int lastVal = -1;
        for (int i = 0; i <= 0x16E; ++i) {
            if (i == 0x153) {
                System.out.println("debug");
            }
            int val = FractionalUCA.fixSecondary2(i, 999, 999); // HACK for UCA
            if (val <= lastVal) {
                throw new IllegalArgumentException(
                        "Unordered: " + Utility.hex(val) + " => " + Utility.hex(lastVal));
            }
            int top = val >>> 8;
        int bottom = val & 0xFF;
        if (top != 0 && (top < FractionalUCA.Variables.COMMON || top > 0xEF)
                || (top > FractionalUCA.Variables.COMMON && top < 0x87)
                || (bottom != 0 && (FractionalUCA.isEven(bottom) || bottom < FractionalUCA.Variables.COMMON || bottom > 0xFD))
                || (bottom == 0 && top != 0 && FractionalUCA.isEven(top))) {
            throw new IllegalArgumentException("Secondary out of range: " + Utility.hex(i) + " => " 
                    + Utility.hex(top) + ", " + Utility.hex(bottom));
        }
        }

        lastVal = -1;
        for (int i = 0; i <= 0x1E; ++i) {
            if (i == 1 || i == 7) {
                continue; // never occurs
            }
            int val = FractionalUCA.fixTertiary(i);
            val &= 0x7F; // mask off case bits
            if (val <= lastVal) {
                throw new IllegalArgumentException(
                        "Unordered: " + Utility.hex(val) + " => " + Utility.hex(lastVal));
            }
            if (val != 0 && (FractionalUCA.isEven(val) || val < FractionalUCA.Variables.COMMON || val > 0x3D)) {
                throw new IllegalArgumentException("Tertiary out of range: " + Utility.hex(i) + " => " 
                        + Utility.hex(val));
            }
        }
        System.out.println("END Checking Secondary/Tertiary Fixes");
    }

    static int[] primaryDelta;

    static void showImplicit(String title, int cp) {
        if (DEBUG) {
            FractionalUCA.showImplicit2(title + "-1", cp-1);
        }

        FractionalUCA.showImplicit2(title + "00", cp);

        if (DEBUG) {
            FractionalUCA.showImplicit2(title + "+1", cp+1);
        }
    }

    static void showImplicit3(String title, int cp) {
        System.out.println("*" + title + ":\t" + Utility.hex(cp)
                + " => " + Utility.hex(FractionalUCA.Variables.INT_MASK & FractionalUCA.getImplicitPrimaryFromSwapped(cp)));
    }

    static void showImplicit2(String title, int cp) {
        System.out.println(title + ":\t" + Utility.hex(cp)
                + " => " + Utility.hex(Implicit.swapCJK(cp))
                + " => " + Utility.hex(FractionalUCA.Variables.INT_MASK & FractionalUCA.getImplicitPrimary(cp)));
    }

    static int getImplicitPrimaryFromSwapped(int cp) {
        return FractionalUCA.implicit.getImplicitFromRaw(cp);
    }

    static int fixPrimary(int x) {
        int result = 0;
        if ((x & FractionalUCA.Variables.MARK_CODE_POINT) != 0) {
            result = FractionalUCA.getImplicitPrimary(x & ~FractionalUCA.Variables.MARK_CODE_POINT);
        } else {
            result = primaryDelta[x];
        }
        return result;
    }

    static int fixSecondary(int x) {
        x = FractionalUCA.compactSecondary[x];
        return FractionalUCA.fixSecondary2(x, FractionalUCA.compactSecondary[0x153], FractionalUCA.compactSecondary[0x157]);
    }

    static int fixSecondary2(int x, int gap1, int gap2) {
        int top = x;
        int bottom = 0;
        if (top == 0) {
            // ok, zero
        } else if (top == 1) {
            top = FractionalUCA.Variables.COMMON;
        } else {
            top *= 2; // create gap between elements. top is now 4 or more
            top += 0x80 + FractionalUCA.Variables.COMMON - 2; // insert gap to make top at least 87

            // lowest values are singletons. Others are 2 bytes
            if (top > FractionalUCA.Variables.secondaryDoubleStart) {
                top -= FractionalUCA.Variables.secondaryDoubleStart;
                top *= 4; // leave bigger gap just in case
                if (x > gap1) {
                    top += 256; // leave gap after COMBINING ENCLOSING KEYCAP (see below)
                }
                if (x > gap2) {
                    top += 64; // leave gap after RUNIC LETTER SHORT-TWIG-AR A (see below)
                }

                bottom = (top % FractionalUCA.Variables.LAST_COUNT) * 2 + FractionalUCA.Variables.COMMON;
                top = (top / FractionalUCA.Variables.LAST_COUNT) + FractionalUCA.Variables.secondaryDoubleStart;
            }
        }
        return (top << 8) | bottom;
    }

    static int fixTertiary(int x) {
        if (x == 0) {
            return x;
        }
        if (x == 1 || x == 7) {
            throw new IllegalArgumentException("Tertiary illegal: " + x);
        }
        // 2 => COMMON, 1 is unused
        int y = x < 7 ? x : x - 1; // we now use 1F = MAX. Causes a problem so we shift everything to fill a gap at 7 (unused).

        int result = 2 * (y - 2) + FractionalUCA.Variables.COMMON;

        if (result >= 0x3E) {
            throw new IllegalArgumentException("Tertiary too large: "
                    + Utility.hex(x) + " => " + Utility.hex(result));
        }

        // get case bits. 00 is low, 01 is mixed (never happens), 10 is high
        if (isUpperTertiary[x]) {
            result |= 0x80;
        } 
        return result;
    }

    static final boolean[] isUpperTertiary = new boolean[32];
    static {
        isUpperTertiary[0x8] = true;
        isUpperTertiary[0x9] = true;
        isUpperTertiary[0xa] = true;
        isUpperTertiary[0xb] = true;
        isUpperTertiary[0xc] = true;
        isUpperTertiary[0xe] = true;
        isUpperTertiary[0x11] = true;
        isUpperTertiary[0x12] = true;
        isUpperTertiary[0x1D] = true;
    }
    static int[] compactSecondary;

    static void testCompatibilityCharacters() throws IOException {
        String fullFileName = "UCA_CompatComparison.txt";
        fractionalLog = Utility.openPrintWriter(WriteCollationData.collator.getUCA_GEN_DIR(), fullFileName, Utility.UTF8_WINDOWS);

        int[] kenCes = new int[50];
        int[] markCes = new int[50];
        int[] kenComp = new int[50];
        Map forLater = new TreeMap();
        int count = 0;
        int typeLimit = UCD_Types.CANONICAL;
        boolean decompType = false;
        if (false) {
            typeLimit = UCD_Types.COMPATIBILITY;
            decompType = true;
        }

        // first find all the characters that cannot be generated "correctly"

        for (int i = 0; i < 0xFFFF; ++i) {
            int type = WriteCollationData.ucd.getDecompositionType(i);
            if (type < typeLimit) {
                continue;
            }
            int ceType = WriteCollationData.collator.getCEType((char)i);
            if (ceType >= WriteCollationData.collator.FIXED_CE) {
                continue;
            }
            // fix type
            type = WriteCollationData.getDecompType(i);

            String s = String.valueOf((char)i);
            int kenLen = WriteCollationData.collator.getCEs(s, decompType, kenCes); // true
            int markLen = FractionalUCA.fixCompatibilityCE(s, true, markCes, false);

            if (!WriteCollationData.arraysMatch(kenCes, kenLen, markCes, markLen)) {
                int kenCLen = FractionalUCA.fixCompatibilityCE(s, true, kenComp, true);
                String comp = CEList.toString(kenComp, kenCLen);

                if (WriteCollationData.arraysMatch(kenCes, kenLen, kenComp, kenCLen)) {
                    forLater.put((char)(FractionalUCA.Variables.COMPRESSED | type) + s, comp);
                    continue;
                }                
                if (type == WriteCollationData.ucd.CANONICAL && WriteCollationData.multipleZeroPrimaries(markCes, markLen)) {
                    forLater.put((char)(FractionalUCA.Variables.MULTIPLES | type) + s, comp);
                    continue;
                }
                forLater.put((char)type + s, comp);
            }
        }

        Iterator it = forLater.keySet().iterator();
        byte oldType = (byte)0xFF; // anything unique
        int caseCount = 0;
        WriteCollationData.writeVersionAndDate(fractionalLog, fullFileName);
        //log.println("# UCA Version: " + collator.getDataVersion() + "/" + collator.getUCDVersion());
        //log.println("Generated: " + getNormalDate());
        while (it.hasNext()) {
            String key = (String) it.next();
            byte type = (byte)key.charAt(0);
            if (type != oldType) {
                oldType = type;
                fractionalLog.println("===============================================================");
                fractionalLog.print("CASE " + (caseCount++) + ": ");
                byte rType = (byte)(type & FractionalUCA.Variables.OTHER_MASK);
                fractionalLog.println("    Decomposition Type = " + WriteCollationData.ucd.getDecompositionTypeID_fromIndex(rType));
                if ((type & FractionalUCA.Variables.COMPRESSED) != 0) {
                    fractionalLog.println("    Successfully Compressed a la Ken");
                    fractionalLog.println("    [XXXX.0020.YYYY][0000.ZZZZ.0002] => [XXXX.ZZZZ.YYYY]");
                } else if ((type & FractionalUCA.Variables.MULTIPLES) != 0) {
                    fractionalLog.println("    PLURAL ACCENTS");
                }
                fractionalLog.println("===============================================================");
                fractionalLog.println();
            }
            String s = key.substring(1);
            String comp = (String)forLater.get(key);

            int kenLen = WriteCollationData.collator.getCEs(s, decompType, kenCes);
            String kenStr = CEList.toString(kenCes, kenLen);

            int markLen = FractionalUCA.fixCompatibilityCE(s, true, markCes, false);
            String markStr = CEList.toString(markCes, markLen);

            if ((type & FractionalUCA.Variables.COMPRESSED) != 0) {
                fractionalLog.println("COMPRESSED #" + (++count) + ": " + WriteCollationData.ucd.getCodeAndName(s));
                fractionalLog.println("         : " + comp);
            } else {
                fractionalLog.println("DIFFERENCE #" + (++count) + ": " + WriteCollationData.ucd.getCodeAndName(s));
                fractionalLog.println("generated : " + markStr);
                if (!markStr.equals(comp)) {
                    fractionalLog.println("compressed: " + comp);
                }
                fractionalLog.println("Ken's     : " + kenStr);
                String nfkd = Default.nfkd().normalize(s);
                fractionalLog.println("NFKD      : " + WriteCollationData.ucd.getCodeAndName(nfkd));
                String nfd = Default.nfd().normalize(s);
                if (!nfd.equals(nfkd)) {
                    fractionalLog.println("NFD       : " + WriteCollationData.ucd.getCodeAndName(nfd));
                }
                //kenCLen = collator.getCEs(decomp, true, kenComp);
                //log.println("decomp ce: " + CEList.toString(kenComp, kenCLen));                   
            }
            fractionalLog.println();
        }
        fractionalLog.println("===============================================================");
        fractionalLog.println();
        fractionalLog.println("Compressible Secondaries");
        for (int i = 0; i < FractionalUCA.Variables.compressSet.size(); ++i) {
            if ((i & 0xF) == 0) {
                fractionalLog.println();
            }
            if (!FractionalUCA.Variables.compressSet.get(i)) {
                fractionalLog.print("-  ");
            } else {
                fractionalLog.print(Utility.hex(i, 3) + ", ");
            }
        }
        fractionalLog.close();
    }

    static int getImplicitPrimary(int cp) {
        return FractionalUCA.implicit.getSwappedImplicit(cp);
    }

    static boolean sameTopByte(int x, int y) {
        int x1 = x & 0xFF0000;
        int y1 = y & 0xFF0000;
        if (x1 != 0 || y1 != 0) {
            return x1 == y1;
        }
        x1 = x & 0xFF00;
        y1 = y & 0xFF00;
        return x1 == y1;
    }

    static Implicit implicit = new Implicit(FractionalUCA.Variables.IMPLICIT_BASE_BYTE, FractionalUCA.Variables.IMPLICIT_MAX_BYTE);

    static final boolean needsCaseBit(String x) {
        String s = Default.nfkd().normalize(x);
        if (!WriteCollationData.ucd.getCase(s, FULL, LOWER).equals(s)) {
            return true;
        }
        if (!FractionalUCA.toSmallKana(s).equals(s)) {
            return true;
        }
        return false;
    }

    static final String toSmallKana(String s) {
        // note: don't need to do surrogates; none exist
        boolean gotOne = false;
        FractionalUCA.toSmallKanaBuffer.setLength(0);
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            if ('\u3042' <= c && c <= '\u30EF') {
                switch(c - 0x3000) {
                case 0x42: case 0x44: case 0x46: case 0x48: case 0x4A: case 0x64: case 0x84: case 0x86: case 0x8F:
                case 0xA2: case 0xA4: case 0xA6: case 0xA8: case 0xAA: case 0xC4: case 0xE4: case 0xE6: case 0xEF:
                    --c; // maps to previous char
                    gotOne = true;
                    break;
                case 0xAB:
                    c = '\u30F5'; 
                    gotOne = true;
                    break;
                case 0xB1:
                    c = '\u30F6'; 
                    gotOne = true;
                    break;
                }
            }
            FractionalUCA.toSmallKanaBuffer.append(c);
        }
        if (gotOne) {
            return FractionalUCA.toSmallKanaBuffer.toString();
        }
        return s;
    }

    static final StringBuffer toSmallKanaBuffer = new StringBuffer();

    static void findBumps(char[] representatives) {
        int[] ces = new int[100];
        int[] scripts = new int[100];
        char[] scriptChar = new char[100];

        // find representatives

        for (char ch = 0; ch < 0xFFFF; ++ch) {
            byte type = WriteCollationData.collator.getCEType(ch);
            if (type < FIXED_CE) {
                int len = WriteCollationData.collator.getCEs(String.valueOf(ch), true, ces);
                int primary = UCA.getPrimary(ces[0]);
                if (primary < FractionalUCA.Variables.variableHigh) {
                    continue;
                }
                /*
                if (ch == 0x1160 || ch == 0x11A8) { // set bumps within Hangul L, V, T
                    bumps.set(primary);
                    continue;
                }
                 */
                byte script = FractionalUCA.getFixedScript(ch);
                //if (script == ucd.GREEK_SCRIPT) System.out.println(ucd.getName(ch));
                // get least primary for script
                if (scripts[script] == 0 || scripts[script] > primary) {
                    byte cat = FractionalUCA.getFixedCategory(ch);
                    if (cat <= WriteCollationData.ucd.OTHER_LETTER && cat != WriteCollationData.ucd.Lm) {
                        scripts[script] = primary;
                        scriptChar[script] = ch;
                        if (script == WriteCollationData.ucd.GREEK_SCRIPT) {
                            System.out.println("*" + Utility.hex(primary) + WriteCollationData.ucd.getName(ch));
                        }
                    }
                }
                // get representative char for primary
                if (representatives[primary] == 0 || representatives[primary] > ch) {
                    representatives[primary] = ch;
                }
            }
        }

        // set bumps
        for (int i = 0; i < scripts.length; ++i) {
            if (scripts[i] > 0) {
                FractionalUCA.bumps.set(scripts[i]);
                System.out.println(Utility.hex(scripts[i]) + " " + UCD.getScriptID_fromIndex((byte)i)
                        + " " + Utility.hex(scriptChar[i]) + " " + WriteCollationData.ucd.getName(scriptChar[i]));
            }
        }

        char[][] singlePairs = {{'a','z'}, {' ', ' '}}; // , {'\u3041', '\u30F3'}
        for (int j = 0; j < singlePairs.length; ++j) {
            for (char k = singlePairs[j][0]; k <= singlePairs[j][1]; ++k) {
                FractionalUCA.setSingle(k, ces);
            }
        }
        /*setSingle('\u0300', ces);
        setSingle('\u0301', ces);
        setSingle('\u0302', ces);
        setSingle('\u0303', ces);
        setSingle('\u0308', ces);
        setSingle('\u030C', ces);
         */

        FractionalUCA.bumps.set(0x089A); // lowest non-variable FRAGILE
        FractionalUCA.bumps.set(0x4E00); // lowest Kangxi

    }

    static void hexBytes(long x, StringBuffer result) {
        byte lastb = 1;
        for (int shift = 24; shift >= 0; shift -= 8) {
            byte b = (byte)(x >>> shift);
            if (b != 0) {
                if (result.length() != 0) {
                    result.append(" ");
                }
                result.append(Utility.hex(b));
                //if (lastb == 0) System.err.println(" bad zero byte: " + result);
            }
            lastb = b;
        }
    }

    static String hexBytes(long x) {
        StringBuffer temp = new StringBuffer();
        hexBytes(x, temp);
        return temp.toString();
    }

    static int fixHan(char ch) { // BUMP HANGUL, HAN
        if (ch < 0x3400 || ch > 0xD7A3) {
            return -1;
        }

        char ch2 = ch;
        if (ch >= 0xAC00) {
            ch2 -= (0xAC00 - 0x9FA5 - 1);
        }
        if (ch >= 0x4E00) {
            ch2 -= (0x4E00 - 0x4DB5 - 1);
        }

        return 0x6000 + (ch2-0x3400); // room to interleave
    }

    static byte getFixedScript(int ch) {
        byte script = WriteCollationData.ucd.getScript(ch);
        // HACK
        if (ch == 0x0F7E || ch == 0x0F7F) {
            script = TIBETAN_SCRIPT;
        }
        return script;
    }

    static byte getFixedCategory(int ch) {
        byte cat = WriteCollationData.ucd.getCategory(ch);
        // HACK
        if (ch == 0x0F7E || ch == 0x0F7F) {
            cat = WriteCollationData.ucd.OTHER_LETTER;
        }
        return cat;
    }

    static void setSingle(char ch, int[] ces) {
        WriteCollationData.collator.getCEs(String.valueOf(ch), true, ces);
        FractionalUCA.singles.set(UCA.getPrimary(ces[0]));
        if (ch == 'a') {
            FractionalUCA.Variables.gapForA = UCA.getPrimary(ces[0]);
        }
    }

    static BitSet singles = new BitSet();
    static BitSet bumps = new BitSet();

    static boolean isEven(int x) {
        return (x & 1) == 0;
    }

    static int fixCompatibilityCE(String s, boolean decompose, int[] output, boolean compress) {
        byte type = WriteCollationData.getDecompType(UTF16.charAt(s, 0));
        char ch = s.charAt(0);

        String decomp = Default.nfkd().normalize(s);
        int len = 0;
        int markLen = WriteCollationData.collator.getCEs(decomp, true, WriteCollationData.markCes);
        if (compress) {
            markLen = WriteCollationData.kenCompress(WriteCollationData.markCes, markLen);
        }

        //for (int j = 0; j < decomp.length(); ++j) {
        for (int k = 0; k < markLen; ++k) {
            int t = UCA.getTertiary(WriteCollationData.markCes[k]);
            t = CEList.remap(k, type, t);
            /*
                if (type != CANONICAL) {
                    if (0x3041 <= ch && ch <= 0x3094) t = 0xE; // hiragana
                    else if (0x30A1 <= ch && ch <= 0x30FA) t = 0x11; // katakana
                }
                switch (type) {
                    case COMPATIBILITY: t = (t == 8) ? 0xA : 4; break;
                    case COMPAT_FONT:  t = (t == 8) ? 0xB : 5; break;
                    case COMPAT_NOBREAK: t = 0x1B; break;
                    case COMPAT_INITIAL: t = 0x17; break;
                    case COMPAT_MEDIAL: t = 0x18; break;
                    case COMPAT_FINAL: t = 0x19; break;
                    case COMPAT_ISOLATED: t = 0x1A; break;
                    case COMPAT_CIRCLE: t = (t == 0x11) ? 0x13 : (t == 8) ? 0xC : 6; break;
                    case COMPAT_SUPER: t = 0x14; break;
                    case COMPAT_SUB: t = 0x15; break;
                    case COMPAT_VERTICAL: t = 0x16; break;
                    case COMPAT_WIDE: t= (t == 8) ? 9 : 3; break;
                    case COMPAT_NARROW: t = (0xFF67 <= ch && ch <= 0xFF6F) ? 0x10 : 0x12; break;
                    case COMPAT_SMALL: t = (t == 0xE) ? 0xE : 0xF; break;
                    case COMPAT_SQUARE: t = (t == 8) ? 0x1D : 0x1C; break;
                    case COMPAT_FRACTION: t = 0x1E; break;
                }
             */
            output[len++] = UCA.makeKey(
                    UCA.getPrimary(WriteCollationData.markCes[k]),
                    UCA.getSecondary(WriteCollationData.markCes[k]),
                    t);
            //}
        }
        return len;
    }


    /*
1112; H   # HANGUL CHOSEONG HIEUH
1161; A   # HANGUL JUNGSEONG A
1175; I   # HANGUL JUNGSEONG I
11A8; G   # HANGUL JONGSEONG KIYEOK
11C2; H   # HANGUL JONGSEONG HIEUH
11F9;HANGUL JONGSEONG YEORINHIEUH;Lo;0;L;;;;;N;;;;;
     */
    static boolean gotInfo = false;
    static int oldJamo1, oldJamo2, oldJamo3, oldJamo4, oldJamo5, oldJamo6;

    static boolean isOldJamo(int primary) {
        if (!gotInfo) {
            int[] temp = new int[20];
            WriteCollationData.collator.getCEs("\u1112", true, temp);
            oldJamo1 = temp[0] >> 16;
        WriteCollationData.collator.getCEs("\u1161", true, temp);
        oldJamo2 = temp[0] >> 16;
        WriteCollationData.collator.getCEs("\u1175", true, temp);
        oldJamo3 = temp[0] >> 16;
        WriteCollationData.collator.getCEs("\u11A8", true, temp);
        oldJamo4 = temp[0] >> 16;
            WriteCollationData.collator.getCEs("\u11C2", true, temp);
            oldJamo5 = temp[0] >> 16;
            WriteCollationData.collator.getCEs("\u11F9", true, temp);
            oldJamo6 = temp[0] >> 16;
        gotInfo = true;
        }
        return primary > oldJamo1 && primary < oldJamo2
        || primary > oldJamo3 && primary < oldJamo4
        || primary > oldJamo5 && primary <= oldJamo6;
    }

}
