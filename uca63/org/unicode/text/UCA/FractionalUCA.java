package org.unicode.text.UCA;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.unicode.cldr.util.Counter;
import org.unicode.text.UCA.UCA.AppendToCe;
import org.unicode.text.UCA.UCA.CollatorType;
import org.unicode.text.UCA.UCA_Statistics.RoBitSet;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.ToolUnicodePropertySource;
import org.unicode.text.UCD.UCD;
import org.unicode.text.UCD.UCD_Types;
import org.unicode.text.utility.ChainException;
import org.unicode.text.utility.DualWriter;
import org.unicode.text.utility.OldEquivalenceClass;
import org.unicode.text.utility.Pair;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Row;
import com.ibm.icu.text.CanonicalIterator;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public class FractionalUCA {
    static PrintWriter fractionalLog;
    static boolean DEBUG = false;
    static final boolean DEBUG_FW = false;

    public static int SPECIAL_LOWEST_DUCET = 1;
    public static int SPECIAL_HIGHEST_DUCET = 0x7FFF;

    static Map<Integer,String> reorderingTokenOverridesNonFractional = new TreeMap<Integer,String>();

    /**
     * FractionalUCA properties for a UCA primary weight.
     *
     * <p>TODO: There are many more arrays and bit sets that
     * map from UCA primary weights to some single properties.
     * Refactor them into this struct and map.
     */
    private static class UCAPrimaryProps {
        private static final Map<Integer, UCAPrimaryProps> map = new HashMap<Integer, UCAPrimaryProps>();

        /**
         * true if this primary is at the start of a reordering group.
         */
        public boolean startsGroup;
        /**
         * The reorder code of the group or script for which this is the first primary.
         * -1 if none.
         */
        public int reorderCodeIfFirst = -1;

        UCAPrimaryProps() {}

        /**
         * Returns the properties struct for the UCA primary weight.
         * Returns null if there is none yet.
         */
        public static UCAPrimaryProps get(int ucaPrimary) {
            UCAPrimaryProps props = map.get(ucaPrimary);
            return props != null ? props : null;
        }

        /**
         * Returns the properties struct for the UCA primary weight.
         * Creates and caches a new one if there is none yet.
         */
        public static UCAPrimaryProps getOrCreate(int ucaPrimary) {
            UCAPrimaryProps props = map.get(ucaPrimary);
            if (props == null) {
                map.put(ucaPrimary, props = new UCAPrimaryProps());
            }
            return props;
        }
    }

    private static class HighByteToReorderingToken {
        private ReorderingTokens[] highByteToReorderingToken = new ReorderingTokens[256];
        private boolean mergedScripts = false;
        private Counter<String> compressCount = new Counter<String>();

        {
            for (int i = 0; i < highByteToReorderingToken.length; ++i) {
                highByteToReorderingToken[i] = new ReorderingTokens();
            }
            // set special values
            highByteToReorderingToken[0].reorderingToken.add("TERMINATOR",1);
            highByteToReorderingToken[1].reorderingToken.add("LEVEL-SEPARATOR",1);
            highByteToReorderingToken[2].reorderingToken.add("FIELD-SEPARATOR",1);
            highByteToReorderingToken[3].reorderingToken.add("SPACE",1);
            for (int i = FractionalUCA.Variables.IMPLICIT_BASE_BYTE; i <= FractionalUCA.Variables.IMPLICIT_MAX_BYTE; ++i) {
                highByteToReorderingToken[i].reorderingToken.add("IMPLICIT",1);
            }
            for (int i = FractionalUCA.Variables.IMPLICIT_MAX_BYTE+1; i < FractionalUCA.Variables.SPECIAL_BASE; ++i) {
                highByteToReorderingToken[i].reorderingToken.add("TRAILING",1);
            }
            for (int i = FractionalUCA.Variables.SPECIAL_BASE; i <= 0xFF; ++i) {
                highByteToReorderingToken[i].reorderingToken.add("SPECIAL",1);
            }
        }

        void addScriptsIn(long fractionalPrimary, String value) {
            // don't add to 0
            int leadByte = getLeadByte(fractionalPrimary);
            if (leadByte != 0) {
                highByteToReorderingToken[leadByte].addInfoFrom(fractionalPrimary, value);
            }
        }

        public String getInfo(boolean doScripts) {
            if (!mergedScripts) {
                throw new IllegalArgumentException();
            }
            StringBuilder builder = new StringBuilder();
            Map<String, Counter<Integer>> map = new TreeMap<String, Counter<Integer>>();
            for (int i = 0; i < highByteToReorderingToken.length; ++i) {
                addScriptCats(map, i, doScripts ? highByteToReorderingToken[i].reorderingToken : highByteToReorderingToken[i].types);
            }
            appendReorderingTokenCatLine(builder, map, doScripts);
            return builder.toString();
        }

        private void addScriptCats(Map<String, Counter<Integer>> map, int i, Counter<String> scripts) {
            for (String script : scripts) {
                long count = scripts.get(script);
                Counter<Integer> scriptCounter = map.get(script);
                if (scriptCounter == null) map.put(script, scriptCounter = new Counter<Integer>());
                scriptCounter.add(i, count);
            }
        }

        private void appendReorderingTokenCatLine(StringBuilder builder, Map<String, Counter<Integer>> map, boolean doScripts) {
            for (String item : map.keySet()) {
                builder.append("[" +
                        (doScripts ? "reorderingTokens" : "categories") +
                        "\t").append(item).append('\t');
                Counter<Integer> counter2 = map.get(item);
                boolean first = true;
                for (Integer i : counter2) {
                    if (first) first = false; else builder.append(' ');

                    builder.append(Utility.hex(i, 2));
                    if (!doScripts) {
                        String stringScripts = CollectionUtilities.join(highByteToReorderingToken[i].reorderingToken.keySet(), " ");
                        if (stringScripts.length() != 0) {
                            builder.append('{').append(stringScripts).append("}");
                        }
                    }
                    builder.append('=').append(counter2.get(i));
                }
                builder.append(" ]\n");
            }
        }

        public String toString() {
            if (!mergedScripts) {
                throw new IllegalArgumentException();
            }
            StringBuilder result = new StringBuilder();
            for (int k = 0; k < highByteToReorderingToken.length; ++k) {
                ReorderingTokens tokens = highByteToReorderingToken[k];
                result.append("[top_byte\t" + Utility.hex(k,2) + "\t");
                tokens.appendTo(result, false);
                boolean isCompressible = compressCount.get(tokens.getRawReorderingTokens()) < 2;
                if (isCompressible != compressibleBytes.get(k)) {
                    throw new AssertionError(String.format(
                            "primary lead byte %02X: " +
                            "primary-weight-assignment compressibleBytes=%s != " +
                            "%s=HighByteToReorderingToken.isCompressible",
                            k,
                            compressibleBytes.get(k),
                            isCompressible));
                }
                if (isCompressible) {
                    result.append("\tCOMPRESS");
                }
                result.append(" ]");
                result.append("\t#\t");
                tokens.appendTo(result, true);
                result.append("\n");
            }
            return result.toString();
        }

        private void cleanup(Map<Integer,String> overrides, int firstScriptPrimary) {
            mergedScripts = true;
            ReorderingTokens[] mergedTokens = new ReorderingTokens[256];

            for (int k = 0; k < highByteToReorderingToken.length; ++k) {
                mergedTokens[k] = new ReorderingTokens(highByteToReorderingToken[k]);
            }
            // Any bytes that share scripts need to be combined
            // brute force, because we don't care about speed
            boolean fixedOne = true;
            while(fixedOne) {
                fixedOne = false;
                for (int k = 1; k < mergedTokens.length; ++k) {
                    if (mergedTokens[k-1].intersects(mergedTokens[k]) 
                            && !mergedTokens[k-1].equals(mergedTokens[k])) {
                        mergedTokens[k-1].or(mergedTokens[k]);
                        mergedTokens[k] = mergedTokens[k-1];
                        fixedOne = true;
                    }
                }
            }
            for (Entry<Integer, String> override : overrides.entrySet()) {
                int value = override.getKey();
                String tag = override.getValue();
                mergedTokens[value].setScripts(tag);
            }
            int firstScriptPrimaryHighByte = getLeadByte(firstScriptPrimary);
            for (int k = 0; k < highByteToReorderingToken.length; ++k) {
                if (mergedTokens[k].reorderingToken.isEmpty()) {
                    mergedTokens[k].setScripts("Hani");
                }
                highByteToReorderingToken[k] = mergedTokens[k];
                if (k > firstScriptPrimaryHighByte) {
                    compressCount.add(highByteToReorderingToken[k].getRawReorderingTokens(), 1);
                } else {
                    compressCount.add(highByteToReorderingToken[k].getRawReorderingTokens(), 2); // ensure > 1
                }
            }
        }
    }

    /**
     * Finds the minimum or maximum fractional collation element
     * among those added via {@link FCE#setValue(int, int, int, String)}.
     */
    private static class FCE {
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

        /**
         * Left-justifies the weight.
         * If the weight is not zero, then the lead byte is moved to bits 31..24.
         */
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
            return FractionalUCA.hexBytes(val);
        }

        long getValue(int zeroBasedLevel) {
            return key[zeroBasedLevel];
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

        public void bumpToFinalReorderingValue() {
            key[0] = (key[0] & 0xFF000000) | 0xFE0000;
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
        static final int  IMPLICIT_4BYTE_BOUNDARY = IMPLICIT_3BYTE_COUNT * OTHER_COUNT * LAST_COUNT
                //LAST_MULTIPLIER = OTHER_COUNT / LAST_COUNT,
                //LAST2_MULTIPLIER = OTHER_COUNT / LAST_COUNT2, IMPLICIT_BASE_3BYTE = (IMPLICIT_BASE_BYTE << 24) + 0x030300,
                //IMPLICIT_BASE_4BYTE = ((IMPLICIT_BASE_BYTE + IMPLICIT_3BYTE_COUNT) << 24) + 0x030303
                ;

        static final int secondaryDoubleStart = 0xD0;

        static final byte MULTIPLES = 0x20, COMPRESSED = 0x40, OTHER_MASK = 0x1F;
        static final BitSet compressSet = new BitSet();
    }

    /**
     * Computes valid FractionalUCA primary weights of desired byte lengths.
     * Always starts with the first primary weight after 02.
     * {@link PrimaryWeight#setToNext(int, boolean, boolean)} increments
     * one 1/2/3-byte weight to another 1/2/3-byte weight.
     */
    static class PrimaryWeight {
        static final int REORDERING_BOUNDARY_CHARACTER_MIN = 0xFDD0;
        static final int REORDERING_BOUNDARY_CHARACTER_MAX = 0xFDEF;

        static final int minByte1 = 2; // merge separator byte

        static final int minByte2 = 5; // 4 is ok, but we leave a gap of 1
        static final int minByte2plus = 10; // leave extra gap for bump.
        static final int maxByte2 = 0xFC; // FE is ok, but we leave a gap of 1

        static final int minByte3 = 4; // ...
        static final int maxByte3 = 0xFE; //.

        private int byte1 = minByte1;
        private int byte2;
        private int byte3;
        private int lastByteLength = 1;

        private BitSet bumpedFirstBytes = new BitSet();

        int getIntValue() {
            return (byte1 << 16) + (byte2 << 8) + byte3;
        }

        void setToNext(int newbyteLength, boolean newFirstByte, boolean scriptChange) {

            int oByte1 = byte1;
            int oByte2 = byte2;
            boolean specialBump = false;

            switch (lastByteLength) {
            case 1:
                switch (newbyteLength) {
                case 1:
                    specialBump = newFirstByte;
                    addTo1(2 + (newFirstByte ? 1 : 0));
                    break;
                case 2:
                    addTo1(1 + (scriptChange ? 1 : 0));
                    byte2 = 0x40; // newFirstByte ? minByte2plus : minByte2;
                    break;
                case 3:
                    addTo1(1);
                    byte2 = 0x40; // newFirstByte ? minByte2plus : minByte2;
                    byte3 = minByte3;
                    break;
                }
                break;
            case 2:
                switch (newbyteLength) {
                case 1:
                    addTo1(1);
                    byte2 = 0;
                    // TODO check gap
                    break;
                case 2:
                    addTo2(2, newFirstByte);
                    break;
                case 3:
                    addTo2(2, newFirstByte);
                    addTo3(8, false);
                    byte3 = minByte3;
                    break;
                }
                break;
            case 3:
                switch (newbyteLength) {
                case 1:
                    addTo1(1);
                    byte2 = byte3 = 0;
                    // TODO check gap
                    break;
                case 2:
                    addTo2(1, newFirstByte);
                    byte3 = 0;
                    break;
                case 3:
                    addTo3(8, newFirstByte);
                    break;
                }
                break;
            }

            // verify results
            // right bytes are filled in, as requested
            switch (newbyteLength) {
            case 1:
                assertTrue(byte1 != 0 && byte2 == 0 &&  byte3 == 0);
                break;
            case 2:
                assertTrue(byte1 != 0 && byte2 != 0 &&  byte3 == 0);
                break;
            case 3:
                assertTrue(byte1 != 0 && byte2 != 0 &&  byte3 != 0);
                break;
            }

            // neither is prefix of the other
            if (lastByteLength != newbyteLength) {
                int minLength = lastByteLength < newbyteLength ? lastByteLength : newbyteLength;
                switch (minLength) {
                case 1:
                    assertTrue(byte1 != oByte1);
                    break;
                case 2:
                    assertTrue(byte1 != oByte1 || byte2 != oByte2);
                    break;
                }
            }

            if (newFirstByte) {
                assertTrue(byte1 != oByte1);
            }

            lastByteLength = newbyteLength;
            if (specialBump) {
                bumpedFirstBytes.set(byte1-1);
            } else if (newFirstByte) {
                bumpedFirstBytes.set(byte1);
            }
        }

        private void assertTrue(boolean b) {
            if (!b) {
                throw new IllegalArgumentException();
            }
        }

        private void addTo3(int increment, boolean newFirstByte) {
            int oldByte1 = byte1;
            byte3 += increment;
            if (byte3 >= maxByte3) {
                byte3 = minByte3 + (byte3 - maxByte3);
                addTo2(1, newFirstByte);
            } else if (newFirstByte && oldByte1 == byte1) {
                addTo1(1);
                byte2 = minByte2plus;
                byte3 = minByte3;
            }
        }

        private void addTo2(int increment, boolean newFirstByte) {
            int oldByte1 = byte1;
            byte2 += increment;
            if (byte2 >= maxByte2) {
                byte1 += 1;
                byte2 = (newFirstByte ? minByte2plus : minByte2) + (byte2 - maxByte2);
            }
            if (newFirstByte && oldByte1 == byte1) {
                addTo1(1);
                byte2 = minByte2plus;
            }
        }

        private void addTo1(int increment) {
            byte1 += increment;
        }

        public String toString() {
            return Utility.hex(byte1, 2) + (byte2 == 0 ? "" : "." + Utility.hex(byte2, 2) 
                    + (byte3 == 0 ? "" : "." + Utility.hex(byte3, 2)));
        }

        public BitSet getBumpedFirstBytes() {
            return bumpedFirstBytes;
        }

        public int getMaxBoundary() {
            // TODO Auto-generated method stub
            return REORDERING_BOUNDARY_CHARACTER_MIN + bumpedFirstBytes.cardinality();
        }
    }

    public static class FractionalStatistics {
        private Map<Long,UnicodeSet> primaries = new TreeMap<Long,UnicodeSet>();
        private Map<Long,UnicodeSet> secondaries = new TreeMap<Long,UnicodeSet>();
        private Map<Long,UnicodeSet> tertiaries = new TreeMap<Long,UnicodeSet>();

        private void addToSet(Map<Long, UnicodeSet> map, int key0, String codepoints) {
            // shift up
            key0 = leftJustify(key0);
            Long key = (long)key0;
            UnicodeSet secondarySet = map.get(key);
            if (secondarySet == null) {
                map.put(key, secondarySet = new UnicodeSet());
            }
            secondarySet.add(codepoints);
        }
        private int leftJustify(int key0) {
            if (key0 != 0) {
                while ((key0 & 0xFF000000) == 0) {
                    key0 <<= 8;
                }
            }
            return key0;
        }
        public void show(Appendable summary) {
            /* We do not compute fractional implicit weights any more.
            checkImplicits();
            */
            try {
                show(Type.primary, primaries, summary);
                show(Type.secondary, secondaries, summary);
                show(Type.tertiary, tertiaries, summary);
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }
        /* We do not compute fractional implicit weights any more.
        private void checkImplicits() {
            TreeMap<Integer, Integer> implicitToCodepoint = new TreeMap<Integer, Integer>();
            for (int codepoint = 0; codepoint <= 0x10FFFF; ++codepoint) {
                int implicitWeight = leftJustify(implicit.getImplicitFromCodePoint(codepoint));
                implicitToCodepoint.put(implicitWeight, codepoint);
            }
            int lastImplicit = 0;
            int lastCodepoint = 0;
            for (Entry<Integer, Integer> entry : implicitToCodepoint.entrySet()) {
                int implicitWeight = entry.getKey();
                int codepoint = entry.getValue();
                if (lastImplicit != 0) {
                    try {
                        checkGap(Type.primary, lastImplicit, implicitWeight);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Strings:\t" 
                                + Default.ucd().getCodeAndName(lastCodepoint) 
                                + ", " + Default.ucd().getCodeAndName(codepoint), e);
                    }
                }
                lastImplicit = implicitWeight;
                lastCodepoint = codepoint;
            }
        }
        */

        private static final UnicodeSet AVOID_CODE_POINTS =  // doesn't depend on version
                new UnicodeSet("[[:C:][:Noncharacter_Code_Point:]]").freeze();

        private static String safeString(String s, int maxLength) {
            if (AVOID_CODE_POINTS.containsSome(s)) {
                StringBuilder sb = new StringBuilder(s);
                for (int i = 0; i < sb.length();) {
                    int c = sb.codePointAt(i);
                    int next = i + Character.charCount(c);
                    if (AVOID_CODE_POINTS.contains(c)) {
                        String replacement;
                        if (c <= 0xffff) {
                            replacement = String.format("\\u%04X", c);
                        } else {
                            replacement = String.format("\\U%08X", c);
                        }
                        sb.replace(i, next, replacement);
                        next = i + replacement.length();
                    }
                    i = next;
                }
                s = sb.toString();
            }

            if (s.length() > maxLength) {
                if (Character.isHighSurrogate(s.charAt(maxLength - 1))) {
                    --maxLength;
                }
                s = s.substring(0, maxLength) + "...";
            }
            return s;
        }
        private void show(Type type, Map<Long, UnicodeSet> map, Appendable summary) throws IOException {
            summary.append("\n# Start " + type + " statistics \n");
            long lastWeight = -1;
            for (Entry<Long, UnicodeSet> entry : map.entrySet()) {
                long weight = entry.getKey();
                if (lastWeight >= 0) {
                    checkGap(type, (int)lastWeight, (int)weight);
                }
                lastWeight = weight;

                UnicodeSet uset = entry.getValue();
                String pattern = uset.toPattern(false);
                summary.append(hexBytes(weight))
                .append('\t')
                .append(String.valueOf(uset.size()))
                .append('\t')
                .append(safeString(pattern, 100))
                .append('\n')
                ;
            }
            summary.append("# End " + type + "  statistics \n");
        }

        enum Type {
            primary(5), secondary(5), tertiary(5); // TODO figure out the right values, use constants
            final int gap;
            Type(int gap) {
                this.gap = gap;
            }
            public int gap() {
                return gap;
            }
        }

        /**
         * Check that there is enough room between weight1 and the weight2. Both
         * are left-justified: that is, of the form XXYYZZWW where XX is only
         * zero if the whole value is zero.
         * 
         * @param type
         * @param weight1
         * @param weight2
         */
        private static void checkGap(Type type, int weight1, int weight2) {
            if (weight1 >= weight2) {
                throw new IllegalArgumentException("Weights out of order: " + hexBytes(weight1) + ",\t" + hexBytes(weight2));
            }
            // find the first difference between bytes
            for (int i = 24; i >= 0; i -= 8) {
                int b1 = (int) ((weight1 >>> i) & 0xFF);
                int b2 = (int) ((weight2 >>> i) & 0xFF);
                // keep going until we find a byte difference
                if (b2 == b1) {
                    continue;
                }
                if (b2 > b1 + 27) {
                    // OK, there is a gap of 27 or greater. Example:
                    // AA BB CC 38
                    // AA BB CC 5C
                    return;
                }
                if (i != 0) { // if we are at not at the end

                    if (b2 > b1 + 1) {
                        // OK, there is a gap of 1 or greater, and we are not in the final byte. Example:
                        // AA 85
                        // AA 87
                        return;
                    }
                    // We now know that b2 == b1 +1

                    // get next bytes
                    i -= 8;
                    b1 = (int) ((weight1 >> i) & 0xFF);
                    b2 = 0x100 + (int) ((weight2 >> i) & 0xFF); // add 100 to express the true difference
                    if (b1 + type.gap() < b2) {
                        // OK, the gap is enough.
                        // AA 85
                        // AA 86 05
                        // or
                        // AA 85 FD
                        // AA 86 03
                        return;
                    }
                }
                throw new IllegalArgumentException("Weights too close: " + hexBytes(weight1) + ",\t" + hexBytes(weight2));
            }
            throw new IllegalArgumentException("Internal Error: " + hexBytes(weight1) + ",\t" + hexBytes(weight2));
        }

        private StringBuffer sb = new StringBuffer();

        private void printAndRecord(boolean show, String chr, int np, int ns, int nt, String comment) {
            try {
                addToSet(primaries, np, chr);
                addToSet(secondaries, ns, chr);
                addToSet(tertiaries, nt, chr);

                sb.setLength(0);
                if (show) {
                    sb.append(Utility.hex(chr)).append(";\t");
                }

                sb.append('[');
                FractionalUCA.hexBytes(np, sb);
                sb.append(", ");
                FractionalUCA.hexBytes(ns, sb);
                sb.append(", ");
                FractionalUCA.hexBytes(nt, sb);
                sb.append(']');

                if (comment != null) {
                    sb.append('\t').append(comment).append('\n');
                }

                fractionalLog.print(sb);
            } catch (Exception e) {
                throw new ChainException("Character is {0}", new String[] {Utility.hex(chr)}, e);
            }
        }

        private void printAndRecordCodePoint(boolean show, String chr, int cp, int ns, int nt, String comment) {
            try {
                // TODO: Do we need to record the new primary weight? -- addToSet(primaries, np, chr);
                addToSet(secondaries, ns, chr);
                addToSet(tertiaries, nt, chr);

                sb.setLength(0);
                if (show) {
                    sb.append(Utility.hex(chr)).append(";\t");
                }

                sb.append("[U+").append(Utility.hex(cp));
                if (ns != 0x500) {
                    sb.append(", ");
                    FractionalUCA.hexBytes(ns, sb);
                }
                if (ns != 0x500 || nt != 5) {
                    sb.append(", ");
                    FractionalUCA.hexBytes(nt, sb);
                }
                sb.append(']');

                if (comment != null) {
                    sb.append('\t').append(comment).append('\n');
                }

                fractionalLog.print(sb);
            } catch (Exception e) {
                throw new ChainException("Character is {0}", new String[] {Utility.hex(chr)}, e);
            }
        }
    }

    static void writeFractionalUCA(String filename) throws IOException {
        // TODO: This function and most fields should not be static,
        // so that use of this class for different data cannot cause values from
        // one run to bleed into the next.
        FractionalUCA.HighByteToReorderingToken highByteToScripts = new FractionalUCA.HighByteToReorderingToken();
        Map<Integer, String> reorderingTokenOverrides = new TreeMap<Integer, String>();

        /* We do not compute fractional implicit weights any more.
        FractionalUCA.checkImplicit();
        */
        FractionalUCA.checkFixes();

        FractionalUCA.Variables.variableHigh = CEList.getPrimary(getCollator().getVariableHighCE());
        UCA r = getCollator();
        RoBitSet secondarySet = r.getStatistics().getSecondarySet();

        // HACK for CJK
        //secondarySet.set(0x0040);

        FractionalStatistics fractionalStatistics = new FractionalStatistics();

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
        FractionalUCA.findBumps();

        System.out.println("Fixing Primaries");
        RoBitSet primarySet = getCollator().getStatistics().getPrimarySet();        

        FractionalUCA.primaryDelta = new int[65536];

        PrimaryWeight fractionalPrimary = new PrimaryWeight();

        // start at 1 so zero stays zero.
        for (int primary = primarySet.nextSetBit(1); primary >= 0; primary = primarySet.nextSetBit(primary+1)) {

            // we know that we need a primary weight for this item.
            // we base it on the last value, and whether we have a 1, 2, or 3-byte weight
            // if we change lengths, then we have to change the initial segment
            // if 'bumps' are set, then we change the first byte

            UCAPrimaryProps props = UCAPrimaryProps.get(primary);
            if (props != null) {  // TODO: Assign a fractional primary for this group or script.
                String groupComment =
                        props.startsGroup ?
                        " starts reordering group" : "";
                System.out.println(String.format(
                        "# %s first primary%s\n",
                        ReorderCodes.getName(props.reorderCodeIfFirst),
                        groupComment));
            }

            int currentByteLength = getFractionalLengthForUCAPrimary(primary);

            String old = fractionalPrimary.toString();
            fractionalPrimary.setToNext(
                    currentByteLength,
                    FractionalUCA.newLeadByteForUCAPrimary.get(primary),
                    primary == groupFirstPrimary[ReorderCodes.PUNCTUATION]);  // TODO: fix this HACK
            if (props != null && props.startsGroup && groupIsCompressible[props.reorderCodeIfFirst]) {
                // TODO: do this for the first-script primary once we assign that
                compressibleBytes.set(fractionalPrimary.byte1);
            }

            String newWeight = fractionalPrimary.toString();
            if (DEBUG_FW) {
                System.out.println(
                        currentByteLength
                        + ", " + (FractionalUCA.newLeadByteForUCAPrimary.get(primary) + "").toUpperCase().substring(0, 1)
                        + ", " + old
                        + " => " + newWeight
                        + "\t" + Utility.hex(Character.codePointAt(
                                getCollator().getRepresentativePrimary(primary),0)));
            }

            FractionalUCA.primaryDelta[primary] = fractionalPrimary.getIntValue();
        }

        BitSet bumpedFirstBytes = fractionalPrimary.getBumpedFirstBytes();


        // now translate!!
        String highCompat = UTF16.valueOf(0x2F805);

        System.out.println("Sorting");
        /**
         * ordered is a sorted map from UCA-type sort key string to its character string.
         * The sort key has U+0000 plus the character string appended.
         * This serves as a simple tie-breaker, although it is different from UCA's identical level.
         */
        Map<String, String> ordered = new TreeMap<String, String>();
        Set<String> contentsForCanonicalIteration = new TreeSet<String>();
        UCA.UCAContents ucac = getCollator().getContents(null);
        int ccounter = 0;
        while (true) {
            Utility.dot(ccounter++);
            String s = ucac.next();
            if (s == null) {
                break;
            }
            if (s.equals("\uFFFF") || s.equals("\uFFFE")) {
                continue; // Suppress the FFFF and FFFE, since we are adding them artificially later.
            }
            if (s.equals("\uFA36") || s.equals("\uF900") || s.equals("\u2ADC") || s.equals(highCompat)) {
                System.out.println(" * " + Default.ucd().getCodeAndName(s));
            }
            contentsForCanonicalIteration.add(s);
            ordered.put(getCollator().getSortKey(s, UCA.NON_IGNORABLE) + '\u0000' + s, s);
        }

        // Add canonically equivalent characters!!
        System.out.println("Start Adding canonical Equivalents2");
        int canCount = 0;

        System.out.println("Add missing decomposibles and non-characters");
        for (int i = 0; i <= 0x10FFFF; ++i) {
            if (Default.ucd().isNoncharacter(i)) {
                continue;
            }
            //            int bottomBits = i & 0xFFFF;
            //            if (bottomBits == 0xFFFE || bottomBits == 0xFFFF) {
            //                continue;
            //            }
            //
            if (!Default.ucd().isAllocated(i)) {
                continue;
            }
            if (Default.nfd().isNormalized(i)) {
                continue;
            }
            if (UCD.isHangulSyllable(i)) {
                continue;
            }
            String s = UTF16.valueOf(i);
            if (!contentsForCanonicalIteration.contains(s)) {
                contentsForCanonicalIteration.add(s);
                //            String sortKey = i == 0xFFFE ? String.valueOf(SPECIAL_LOWEST_DUCET) 
                //                    : i == 0xFFFF ? String.valueOf(SPECIAL_HIGHEST_DUCET) : 
                String sortKey = getCollator().getSortKey(s, UCA.NON_IGNORABLE);
                ordered.put(sortKey + '\u0000' + s, s);
                if (DEBUG) System.out.println(" + " + Default.ucd().getCodeAndName(s));
                canCount++;
            }
        }

        Set<String> additionalSet = new HashSet<String>();
        System.out.println("Loading canonical iterator");
        if (WriteCollationData.canIt == null) {
            WriteCollationData.canIt = new CanonicalIterator(".");
        }
        Iterator<String> it2 = contentsForCanonicalIteration.iterator();
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
                String sortKey = getCollator().getSortKey(s, UCA.NON_IGNORABLE);
                String nonDecompSortKey = getCollator().getSortKey(s, UCA.NON_IGNORABLE, false, AppendToCe.none);
                if (sortKey.equals(nonDecompSortKey)) {
                    continue;
                }

                if (first) {
                    System.out.println(" " + Default.ucd().getCodeAndName(key));
                    first = false;
                }
                System.out.println(" => " + Default.ucd().getCodeAndName(s));
                System.out.println("    old: " + UCA.toString(nonDecompSortKey));
                System.out.println("    new: " + UCA.toString(sortKey));
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
        final boolean TESTING = false;
        if (TESTING) {
            String sample = "\u3400\u3401\u4DB4\u4DB5\u4E00\u4E01\u9FA4\u9FA5\uAC00\uAC01\uD7A2\uD7A3";
            for (int i = 0; i < sample.length(); ++i) {
                String s = sample.substring(i, i+1);
                ordered.put(getCollator().getSortKey(s, UCA.NON_IGNORABLE) + '\u0000' + s, s);
            }
        }

        // add special reordering tokens
        int lastHighByte = 4;
        for (Entry<Integer, String> entry : reorderingTokenOverridesNonFractional.entrySet()) {
            int primary = entry.getKey();
            String token = entry.getValue();
            int highByte = getLeadByte(FractionalUCA.primaryDelta[primary]); 
            // Hack
            for (; lastHighByte < highByte; ++lastHighByte) { // 
                reorderingTokenOverrides.put(lastHighByte, token);
            }
            if ("DIGIT".equals(token)) {
                reorderingTokenOverrides.put(lastHighByte-1, "Latn");
            }
        }

        Utility.fixDot();
        System.out.println("Writing");

        String directory = UCA.getUCA_GEN_DIR() + "CollationAuxiliary" + File.separator;

        boolean shortPrint = false;
        PrintWriter longLog = Utility.openPrintWriter(directory, filename + ".txt", Utility.UTF8_WINDOWS);
        if (shortPrint) { 
            PrintWriter shortLog = Utility.openPrintWriter(directory, filename + "_SHORT.txt", Utility.UTF8_WINDOWS);
    
            fractionalLog = new PrintWriter(new DualWriter(shortLog, longLog));
        } else {
            fractionalLog = longLog;
        }

        String summaryFileName = filename + "_summary.txt";
        PrintWriter summary = Utility.openPrintWriter(directory, summaryFileName, Utility.UTF8_WINDOWS);

        String logFileName = filename + "_log.txt";
        PrintWriter log = Utility.openPrintWriter(UCA.getUCA_GEN_DIR() + "log" + File.separator, logFileName, Utility.UTF8_WINDOWS);
        //PrintWriter summary = new PrintWriter(new BufferedWriter(new FileWriter(directory + filename + "_summary.txt"), 32*1024));

        summary.println("# Summary of Fractional UCA Table, generated from standard UCA");
        WriteCollationData.writeVersionAndDate(summary, summaryFileName, true);
        summary.println("# Primary Ranges");
        //log.println("[Variable Low = " + UCA.toString(collator.getVariableLow()) + "]");
        //log.println("[Variable High = " + UCA.toString(collator.getVariableHigh()) + "]");

        int[] ces = new int[100];

        StringBuffer oldStr = new StringBuffer();

        OldEquivalenceClass secEq = new OldEquivalenceClass("\r\n#", 2, true);
        OldEquivalenceClass terEq = new OldEquivalenceClass("\r\n#", 2, true);
        String[] sampleEq = new String[500];
        int[] sampleLen = new int[500];

        Iterator<String> it = ordered.keySet().iterator();
        int oldFirstPrimary = CEList.getPrimary(UCA.TERMINATOR);
        // boolean wasVariable = false;

        fractionalLog.println("# Fractional UCA Table, generated from standard UCA");
        fractionalLog.println("# " + WriteCollationData.getNormalDate());
        fractionalLog.println("# VERSION: UCA=" + getCollator().getDataVersion() + ", UCD=" + getCollator().getUCDVersion());
        fractionalLog.println("# For a description of the format and usage, see");
        fractionalLog.println("#   http://www.unicode.org/reports/tr35/tr35-collation.html");
        fractionalLog.println();
        fractionalLog.println("[UCA version = " + getCollator().getDataVersion() + "]");

        // Print the Unified_Ideograph ranges in collation order.
        // As a result, a parser need not have this data available for the current Unicode version.
        UnicodeSet hanSet =
                ToolUnicodePropertySource.make(getCollator().getUCDVersion()).
                getProperty("Unified_Ideograph").getSet("True");
        UnicodeSet coreHanSet = (UnicodeSet) hanSet.clone();
        coreHanSet.retain(0x4e00, 0xffff);  // BlockCJK_Unified_Ideograph or CJK_Compatibility_Ideographs
        StringBuilder hanSB = new StringBuilder("[Unified_Ideograph");
        UnicodeSetIterator hanIter = new UnicodeSetIterator(coreHanSet);
        while (hanIter.nextRange()) {
            int start = hanIter.codepoint;
            int end = hanIter.codepointEnd;
            hanSB.append(' ').append(Utility.hex(start));
            if (start < end) {
                hanSB.append("..").append(Utility.hex(end));
            }
        }
        hanSet.remove(0x4e00, 0xffff);
        hanIter.reset(hanSet);
        while (hanIter.nextRange()) {
            int start = hanIter.codepoint;
            int end = hanIter.codepointEnd;
            hanSB.append(' ').append(Utility.hex(start));
            if (start < end) {
                hanSB.append("..").append(Utility.hex(end));
            }
        }
        hanSB.append("]");
        fractionalLog.println(hanSB);

        String lastChr = "";
        int lastNp = 0;
        //boolean doVariable = false;
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

        Map<Integer, Pair> fractBackMap = new TreeMap<Integer, Pair>();

        //int topVariable = -1;

        while (it.hasNext()) {
            Object sortKey = it.next();
            String chr = (String)ordered.get(sortKey);  

            char lastChar = chr.charAt(chr.length() - 1);
            if (Character.isHighSurrogate(lastChar)) {
                System.out.println("Skipping trailing surrogate: " + chr + "\t" + Utility.hex(chr));
                continue;
            }

            // get CEs and fix
            int len = getCollator().getCEs(chr, true, ces);  // TODO: CEList ces = getCollator().getCEList(chr, true);
            int firstPrimary = CEList.getPrimary(ces[0]);
            // String message = null;
            if (firstPrimary != oldFirstPrimary) {
                fractionalLog.println();
                //                boolean isVariable = getCollator().isVariable(ces[0]);
                //                if (isVariable != wasVariable) {
                //                    if (isVariable) {
                //                        message = "# START OF VARIABLE SECTION!!!";
                //                    } else {
                //                        //topVariable = FractionalUCA.primaryDelta[oldFirstPrimary];
                //                        doVariable = true;
                //                    }
                //                    //fractionalLog.println(message);
                //                    //fractionalLog.println();
                //                }
                //                wasVariable = isVariable;
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

                int pri = CEList.getPrimary(ces[q]);
                int sec = CEList.getSecondary(ces[q]); 
                int ter = CEList.getTertiary(ces[q]);

                oldStr.append(CEList.toString(ces[q]));// + "," + Integer.toString(ces[q],16);

                if (sec != 0x20) {
                    /* boolean changed = */ secEq.add(new Integer(sec), new Integer(pri));
                }
                if (ter != 0x2) {
                    /* boolean changed = */ terEq.add(new Integer(ter), new Integer((pri << 16) | sec));
                }

                if (sampleEq[sec] == null || sampleLen[sec] > len) {
                    sampleEq[sec] = chr;
                    sampleLen[sec] = len;
                }
                if (sampleEq[ter] == null || sampleLen[sec] > len) {
                    sampleEq[ter] = chr;
                    sampleLen[sec] = len;
                }

                // special treatment for unsupported!

                int np;
                int implicitCodePoint = 0;
                if (UCA.isImplicitLeadPrimary(pri)) {
                    if (DEBUG) {
                        System.out.println("DEBUG: " + CEList.toString(ces, len) 
                                + ", Current: " + q + ", " + Default.ucd().getCodeAndName(chr));
                    }
                    ++q;
                    oldStr.append(CEList.toString(ces[q]));// + "," + Integer.toString(ces[q],16);

                    int pri2 = CEList.getPrimary(ces[q]);
                    implicitCodePoint = UCA.ImplicitToCodePoint(pri, pri2);
                    if (DEBUG) {
                        System.out.println("Computing Unsupported CP as: "
                                + Utility.hex(pri)
                                + ", " + Utility.hex(pri2)
                                + " => " + Utility.hex(implicitCodePoint));
                    }

                    /* We do not compute fractional implicit weights any more.
                    // was: np = FractionalUCA.getImplicitPrimary(pri);
                    */
                    np = Variables.IMPLICIT_BASE_BYTE << 16;
                } else {
                    np = FractionalUCA.fixPrimary(pri);
                }

                int ns = FractionalUCA.fixSecondary(sec);
                int nt = FractionalUCA.fixTertiary(ter, chr);

                if (implicitCodePoint == 0 && pri == 0) {
                    if (chr.equals("\u01C6")) {
                        System.out.println("At dz-caron");
                    }
                    Integer key = new Integer(ces[q]);
                    Pair value = fractBackMap.get(key);
                    if (value == null
                            || (len < ((Integer)(value.first)).intValue())) {
                        fractBackMap.put(key, new Pair(new Integer(len), chr));
                    }
                }

                // Note: (q == firstCE) != isFirst when pri is an implicit primary
                // because we read pri2 and incremented q.
                if (q == firstCE) { // only look at first one
                    highByteToScripts.addScriptsIn(np, chr);
                }

                if (implicitCodePoint == 0) {
                    fractionalStatistics.printAndRecord(false, chr, np, ns, nt, null);
                } else {
                    fractionalStatistics.printAndRecordCodePoint(false, chr, implicitCodePoint, ns, nt, null);
                }

                if (isFirst) {
                    if (!FractionalUCA.sameTopByte(np, lastNp)) {
                        if (lastNp != 0) {
                            showRange("Last", summary, lastChr, lastNp);
                        }
                        summary.println();
                        //                        if (doVariable) {
                        //                            doVariable = false;
                        //                            summary.println("#" + message);
                        //                            summary.println();
                        //                        }
                        //                        int topByte = np >>> 16;
                        //                        if (bumpedFirstBytes.get(topByte)) {
                        //                            int specialLast = ((topByte-1) << 16) + 0xFE00;
                        //                            summary.println("*BUMP: " + hexBytes(specialLast) + "\n");
                        //                        }
                        showRange("First", summary, chr, np);
                        // summary.println("First:\t" + Utility.hex(np & FractionalUCA.Variables.INT_MASK) + ":\t" + WriteCollationData.ucd.getCodeAndName(UTF16.charAt(chr,0)));
                    }
                    lastNp = np;
                    isFirst = false;
                }

                // RECORD STATS
                // but ONLY if we are not part of an implicit

                if (implicitCodePoint == 0) {
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
                    } else if (getCollator().isVariable(ces[q])) {
                        firstVariable.setValue(np, ns, nt, chr);
                        lastVariable.setValue(np, ns, nt, chr); 
                    } else if (CEList.getPrimary(ces[q]) > UCA_Types.UNSUPPORTED_LIMIT) {        // Trailing (none currently)
                        System.out.println("Trailing: " 
                                + Default.ucd().getCodeAndName(chr) + ", "
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
                    ? Default.ucd().getName(UTF16.charAt(chr, 0)) + " ..."
                            : Default.ucd().getName(chr);

            String gcInfo = getStringTransform(chr, "/", ScriptTransform);
            String scriptInfo = getStringTransform(chr, "/", GeneralCategoryTransform);

            longLog.print("\t# " + gcInfo + " " + scriptInfo + "\t" + oldStr + "\t* " + name);
            fractionalLog.println();
            lastChr = chr;
        }

        int lastNonIgnorableTopByte = (int) (lastNonIgnorable.getValue(0) >>> 24);
        bumpedFirstBytes.set(lastNonIgnorableTopByte + 1);

        fractionalLog.println();
        fractionalLog.println("# SPECIAL MAX/MIN COLLATION ELEMENTS");
        fractionalLog.println();

        fractionalStatistics.printAndRecord(true, "\uFFFE", 0x020000, 2, 2, "# Special LOWEST primary, for merge/interleaving");
        fractionalStatistics.printAndRecord(true, "\uFFFF", 0xEFFE00, 5, 5, "# Special HIGHEST primary, for ranges");

        fractionalLog.println();
        fractionalLog.println("# SPECIAL FINAL VALUES for Script Reordering");
        fractionalLog.println();

        FakeString fakeString = new FakeString();
        fakeString.next();  // skip the boundary before spaces
        for (int i = bumpedFirstBytes.nextSetBit(4); i >= 0; i = bumpedFirstBytes.nextSetBit(i+1)) {
            int previousByte = i-1;
            fractionalStatistics.printAndRecord(true, fakeString.next(), (previousByte << 16) | 0xFE00, 5, 5, "# Special final value for reordering token");
        }

        // ADD HOMELESS COLLATION ELEMENTS
        fractionalLog.println();
        fractionalLog.println("# HOMELESS COLLATION ELEMENTS");

        Iterator<Integer> it3 = fractBackMap.keySet().iterator();
        while (it3.hasNext()) {
            Integer key = (Integer) it3.next();
            Pair pair = fractBackMap.get(key);
            if (((Integer)pair.first).intValue() < 2) {
                continue;
            }
            String sample = (String)pair.second;

            int ce = key.intValue();

            int np = FractionalUCA.fixPrimary(CEList.getPrimary(ce));
            int ns = FractionalUCA.fixSecondary(CEList.getSecondary(ce));
            int nt = FractionalUCA.fixTertiary(CEList.getTertiary(ce), sample);

            highByteToScripts.addScriptsIn(np, sample);
            fractionalStatistics.printAndRecord(true, fakeString.next(), np, ns, nt, null);

            longLog.print("\t# " + getCollator().getCEList(sample, true) + "\t* " + Default.ucd().getCodeAndName(sample));
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
            fractionalStatistics.printAndRecord(true, fakeString.next(), 0, 0, fakeTertiary, null);

            fractionalLog.println("\t# CONSTRUCTED FAKE SECONDARY-IGNORABLE");
        }

        int firstImplicit = Variables.IMPLICIT_BASE_BYTE;  // was FractionalUCA.getImplicitPrimary(UCD_Types.CJK_BASE);
        int lastImplicit = Variables.IMPLICIT_MAX_BYTE;  // was FractionalUCA.getImplicitPrimary(0x10FFFD);

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

        // fix last variable, lastNonIgnorable
        lastVariable.bumpToFinalReorderingValue();
        lastNonIgnorable.bumpToFinalReorderingValue();

        fractionalLog.println(firstVariable);
        fractionalLog.println(lastVariable);
        fractionalLog.println("[variable top = " + hexBytes(lastVariable.key[0]) + "]");

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

        //        fractionalLog.println("# Distinguished Variable-Top Values: the last of each range");
        //
        //        fractionalLog.println("[vt-none " + hexBytes(3) + "]");
        //        fractionalLog.println("[vt-space " + hexBytes(FractionalUCA.primaryDelta[spaceRange.getMaximum()]) + "]");
        //        fractionalLog.println("[vt-punct " + hexBytes(FractionalUCA.primaryDelta[punctuationRange.getMaximum()]) + "]");
        //        fractionalLog.println("[vt-symbol " + hexBytes(FractionalUCA.primaryDelta[symbolRange.getMaximum()]) + "]");
        //        fractionalLog.println("[vt-currency " + hexBytes(FractionalUCA.primaryDelta[currencyRange.getMaximum()]) + "]");
        //        fractionalLog.println("[vt-digit " + hexBytes(FractionalUCA.primaryDelta[digitRange.getMaximum()]) + "]");
        //        fractionalLog.println("[vt-ducet " + hexBytes(FractionalUCA.primaryDelta[ducetFirstNonVariable]) + "]");
        //        fractionalLog.println();


        //reorderingTokenOverrides.put(5, "SPACE");

        int firstScriptPrimary = getCollator().getStatistics().firstScript;
        int fractionalFirstScriptPrimary = FractionalUCA.primaryDelta[firstScriptPrimary];
        highByteToScripts.cleanup(reorderingTokenOverrides, fractionalFirstScriptPrimary);

        fractionalLog.println("# Top Byte => Reordering Tokens");
        fractionalLog.println(highByteToScripts.toString());
        fractionalLog.println();

        fractionalLog.println("# Reordering Tokens => Top Bytes");
        fractionalLog.println(highByteToScripts.getInfo(true));
        fractionalLog.println();

        fractionalLog.println("# General Categories => Top Byte");
        fractionalLog.println(highByteToScripts.getInfo(false));
        fractionalLog.println();


        fractionalLog.println();
        fractionalLog.println("# FIXED VALUES");

        // fractionalLog.println("# superceded! [top "  + lastNonIgnorable.formatFCE() + "]");
        fractionalLog.println("[fixed first implicit byte " + Utility.hex(FractionalUCA.Variables.IMPLICIT_BASE_BYTE,2) + "]");
        fractionalLog.println("[fixed last implicit byte " + Utility.hex(FractionalUCA.Variables.IMPLICIT_MAX_BYTE,2) + "]");
        fractionalLog.println("[fixed first trail byte " + Utility.hex(FractionalUCA.Variables.IMPLICIT_MAX_BYTE+1,2) + "]");
        fractionalLog.println("[fixed last trail byte " + Utility.hex(FractionalUCA.Variables.SPECIAL_BASE-1,2) + "]");
        fractionalLog.println("[fixed first special byte " + Utility.hex(FractionalUCA.Variables.SPECIAL_BASE,2) + "]");
        fractionalLog.println("[fixed last special byte " + Utility.hex(0xFF,2) + "]");

        showRange("Last", summary, lastChr, lastNp);
        //summary.println("Last:  " + Utility.hex(lastNp) + ", " + WriteCollationData.ucd.getCodeAndName(UTF16.charAt(lastChr, 0)));

        /*
            String sample = "\u3400\u3401\u4DB4\u4DB5\u4E00\u4E01\u9FA4\u9FA5\uAC00\uAC01\uD7A2\uD7A3";
            for (int i = 0; i < sample.length(); ++i) {
                char ch = sample.charAt(i);
                log.println(Utility.hex(ch) + " => " + Utility.hex(fixHan(ch))
                        + "          " + ucd.getName(ch));
            }
         */
        summary.println();
        /* We do not compute fractional implicit weights any more.
        summary.println("# First Implicit: " + Utility.hex(FractionalUCA.Variables.IMPLICIT_BASE_BYTE));  // was FractionalUCA.getImplicitPrimary(0)
        summary.println("# Last Implicit: " + Utility.hex(FractionalUCA.Variables.IMPLICIT_MAX_BYTE));  // was FractionalUCA.getImplicitPrimary(0x10FFFF)
        summary.println("# First CJK: " + Utility.hex(FractionalUCA.Variables.IMPLICIT_BASE_BYTE));  // was FractionalUCA.getImplicitPrimary(0x4E00)
        summary.println("# Last CJK: " + Utility.hex(FractionalUCA.Variables.IMPLICIT_BASE_BYTE));  // was FractionalUCA.getImplicitPrimary(0xFA2F), should have been dynamic
        summary.println("# First CJK_A: " + Utility.hex(FractionalUCA.Variables.IMPLICIT_BASE_BYTE + 1));  // was FractionalUCA.getImplicitPrimary(0x3400)
        summary.println("# Last CJK_A: " + Utility.hex(FractionalUCA.Variables.IMPLICIT_BASE_BYTE + 1));  // was FractionalUCA.getImplicitPrimary(0x4DBF), should have been dynamic
        */

        if (DEBUG) {
            /* We do not compute fractional implicit weights any more.
            boolean lastOne = false;
            for (int i = 0; i < 0x10FFFF; ++i) {
                boolean thisOne = UCD.isCJK_BASE(i) || UCD.isCJK_AB(i);
                if (thisOne != lastOne) {
                    summary.println("# Implicit Cusp: CJK=" + lastOne + ": " + Utility.hex(i-1) +
                            " => " + Utility.hex(FractionalUCA.Variables.INT_MASK &
                                                 FractionalUCA.getImplicitPrimary(i-1)));
                    summary.println("# Implicit Cusp: CJK=" + thisOne + ": " + Utility.hex(i) +
                            " => " + Utility.hex(FractionalUCA.Variables.INT_MASK &
                                                 FractionalUCA.getImplicitPrimary(i)));
                    lastOne = thisOne;
                }
            }
            */
            summary.println("Compact Secondary 153: " + FractionalUCA.compactSecondary[0x153]);
            summary.println("Compact Secondary 157: " + FractionalUCA.compactSecondary[0x157]);
        }



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
            int len = getCollator().getCEs(sampleEq[i], true, ces);
            int newval = i < 0x20 ? FractionalUCA.fixTertiary(i,sampleEq[i]) : FractionalUCA.fixSecondary(i);
            summary.print("# " + Utility.hex(i) + ": (" + Utility.hex(newval) + ") "
                    + Utility.hex(sampleEq[i]) + " ");
            for (int q = 0; q < len; ++q) {
                summary.print(CEList.toString(ces[q]));
            }
            summary.println(" " + Default.ucd().getName(sampleEq[i]));

        }
        fractionalStatistics.show(log);
        fractionalLog.close();
        summary.close();
        log.close();
    }

    private static class FakeString {
        char[] buffer = {'\uFDD0', 'A'};
        String next() {
            buffer[1]++;
            return new String(buffer);
        }
    }

    private static UCA getCollator() {
        return WriteCollationData.getCollator(CollatorType.cldrWithoutFFFx);
    }

    static Transform<Integer, String> ScriptTransform = new Transform<Integer, String>() {
        public String transform(Integer codePoint) {
            return Default.ucd().getScriptID(codePoint, UCD_Types.SHORT);
        }
    };

    static Transform<Integer, String> GeneralCategoryTransform  = new Transform<Integer, String>() {
        public String transform(Integer codePoint) {
            return Default.ucd().getCategoryID(codePoint, UCD_Types.SHORT);
        }
    };

    //    static class IcuEnumProp implements Transform<Integer, String> {
    //        private int propEnum;
    //        private int nameChoice;
    //
    //        /**
    //         * @param propEnum
    //         * @param nameChoice
    //         */
    //        public IcuEnumProp(int propEnum, int nameChoice) {
    //            super();
    //            this.propEnum = propEnum;
    //            this.nameChoice = nameChoice;
    //        }
    //
    //        public String transform(Integer source) {
    //            return propValue(source, propEnum, nameChoice);
    //        }
    //    }

    //  private static String propValue(int ch, int propEnum, int nameChoice) {
    //  return UCharacter.getPropertyValueName(propEnum, UCharacter.getIntPropertyValue(ch, propEnum), nameChoice);
    //}


    public static String getStringTransform(CharSequence string, CharSequence separator, Transform<Integer,String> prop) {
        return getStringTransform(string, separator, prop, new ArrayList<String>());
    }

    public static String getStringTransform(CharSequence string, CharSequence separator, Transform<Integer,String> prop, Collection<String> c) {
        c.clear();
        int cp;
        for (int i = 0; i < string.length(); i += Character.charCount(cp)) {
            cp = Character.codePointAt(string, i);
            c.add(prop.transform(cp));
        }
        StringBuffer result = new StringBuffer();
        for (String item : c) {
            if (result.length() != 0) {
                result.append(separator);
            }
            result.append(item);
        }
        return result.toString();
    }

    private static void showRange(String title, PrintWriter summary, String lastChr, int lastNp) {
        int ch = lastChr.codePointAt(0);
        summary.println("#\t" + title
                + "\t" + padHexBytes(lastNp) 
                + "\t" + ScriptTransform.transform(ch)
                + "\t" + GeneralCategoryTransform.transform(ch)
                + "\t" + Default.ucd().getCodeAndName(UTF16.charAt(lastChr,0)));
    }

    private static String padHexBytes(int lastNp) {
        String result = hexBytes(lastNp & FractionalUCA.Variables.INT_MASK);
        return result + Utility.repeat(" ", 11-result.length());
        // E3 9B 5F C8
    }

    /* We do not compute fractional implicit weights any more.
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

        FractionalUCA.showImplicit("# 3B9D", 0x3B9D);

        FractionalUCA.showImplicit("# First CJK", UCD_Types.CJK_BASE);
        FractionalUCA.showImplicit("# Last CJK", UCD_Types.CJK_LIMIT-1);
        FractionalUCA.showImplicit("# First CJK-compat", UCD_Types.CJK_COMPAT_USED_BASE);
        FractionalUCA.showImplicit("# Last CJK-compat", UCD_Types.CJK_COMPAT_USED_LIMIT-1);
        FractionalUCA.showImplicit("# First CJK_A", UCD_Types.CJK_A_BASE);
        FractionalUCA.showImplicit("# Last CJK_A", UCD_Types.CJK_A_LIMIT-1);
        FractionalUCA.showImplicit("# First CJK_B", UCD_Types.CJK_B_BASE);
        FractionalUCA.showImplicit("# Last CJK_B", UCD_Types.CJK_B_LIMIT-1);
        FractionalUCA.showImplicit("# First CJK_C", UCD_Types.CJK_C_BASE);
        FractionalUCA.showImplicit("# Last CJK_C", UCD_Types.CJK_C_LIMIT-1);
        FractionalUCA.showImplicit("# First CJK_D", UCD_Types.CJK_D_BASE);
        FractionalUCA.showImplicit("# Last CJK_D", UCD_Types.CJK_D_LIMIT-1);
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

                if (UCD.isCJK_BASE(i) || UCD_Types.CJK_COMPAT_USED_BASE <= i && i < UCD_Types.CJK_COMPAT_USED_LIMIT) {
                    if (batch != 0) {
                        continue;
                    }
                } else if (UCD.isCJK_AB(i)) {
                    if (batch != 1) {
                        continue;
                    }
                } else if (batch != 2) {
                    continue;
                }


                // test swapping

                int currSwap = ImplicitCEGenerator.swapCJK(i);
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
                final boolean TESTING = false;
                if (TESTING) {
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
    */

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
            int val = FractionalUCA.fixTertiary(i, ""); // not interested in case bits, so ok to pass in ""
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

    /* We do not compute fractional implicit weights any more.
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
                + " => " + Utility.hex(ImplicitCEGenerator.swapCJK(cp))
                + " => " + Utility.hex(FractionalUCA.Variables.INT_MASK & FractionalUCA.getImplicitPrimary(cp)));
    }

    static int getImplicitPrimaryFromSwapped(int cp) {
        return FractionalUCA.implicit.getImplicitFromRaw(cp);
    }
    */

    static int fixPrimary(int x) {
        return primaryDelta[x];
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

            if (top >= 149) top += 2; // HACK for backwards compatibility.

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

    static int fixTertiary(int x, String originalString) {
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
        int caseBits;
        if (GET_CASE_FROM_STRING) {
            caseBits = CaseBit.getPropertyCasing(originalString).getBits();
        } else {
            caseBits = CaseBit.getCaseFromTertiary(x).getBits();
        }
        if (caseBits != 0) {
            result |= caseBits;
        }
        return result;
    }

    static final boolean GET_CASE_FROM_STRING = false;

    static int[] compactSecondary;

    static void testCompatibilityCharacters() throws IOException {
        String fullFileName = "UCA_CompatComparison.txt";
        fractionalLog = Utility.openPrintWriter(UCA.getUCA_GEN_DIR() + File.separator + "log", fullFileName, Utility.UTF8_WINDOWS);

        int[] kenCes = new int[50];
        int[] markCes = new int[50];
        int[] kenComp = new int[50];
        Map<String, String> forLater = new TreeMap<String, String>();
        int count = 0;
        int typeLimit = UCD_Types.CANONICAL;
        boolean decompType = false;
        final boolean TESTING = false;
        if (TESTING) {
            typeLimit = UCD_Types.COMPATIBILITY;
            decompType = true;
        }

        // first find all the characters that cannot be generated "correctly"

        for (int i = 0; i < 0xFFFF; ++i) {
            int type = Default.ucd().getDecompositionType(i);
            if (type < typeLimit) {
                continue;
            }
            if (!getCollator().codePointHasExplicitMappings(i)) {
                continue;
            }
            // fix type
            type = WriteCollationData.getDecompType(i);

            String s = String.valueOf((char)i);
            int kenLen = getCollator().getCEs(s, decompType, kenCes); // true
            int markLen = FractionalUCA.fixCompatibilityCE(s, true, markCes, false);

            if (!WriteCollationData.arraysMatch(kenCes, kenLen, markCes, markLen)) {
                int kenCLen = FractionalUCA.fixCompatibilityCE(s, true, kenComp, true);
                String comp = CEList.toString(kenComp, kenCLen);

                if (WriteCollationData.arraysMatch(kenCes, kenLen, kenComp, kenCLen)) {
                    forLater.put((char)(FractionalUCA.Variables.COMPRESSED | type) + s, comp);
                    continue;
                }                
                if (type == UCD_Types.CANONICAL && WriteCollationData.multipleZeroPrimaries(markCes, markLen)) {
                    forLater.put((char)(FractionalUCA.Variables.MULTIPLES | type) + s, comp);
                    continue;
                }
                forLater.put((char)type + s, comp);
            }
        }

        Iterator<String> it = forLater.keySet().iterator();
        byte oldType = (byte)0xFF; // anything unique
        int caseCount = 0;
        WriteCollationData.writeVersionAndDate(fractionalLog, fullFileName, true);
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
                fractionalLog.println("    Decomposition Type = " + UCD.getDecompositionTypeID_fromIndex(rType));
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

            int kenLen = getCollator().getCEs(s, decompType, kenCes);
            String kenStr = CEList.toString(kenCes, kenLen);

            int markLen = FractionalUCA.fixCompatibilityCE(s, true, markCes, false);
            String markStr = CEList.toString(markCes, markLen);

            if ((type & FractionalUCA.Variables.COMPRESSED) != 0) {
                fractionalLog.println("COMPRESSED #" + (++count) + ": " + Default.ucd().getCodeAndName(s));
                fractionalLog.println("         : " + comp);
            } else {
                fractionalLog.println("DIFFERENCE #" + (++count) + ": " + Default.ucd().getCodeAndName(s));
                fractionalLog.println("generated : " + markStr);
                if (!markStr.equals(comp)) {
                    fractionalLog.println("compressed: " + comp);
                }
                fractionalLog.println("Ken's     : " + kenStr);
                String nfkd = Default.nfkd().normalize(s);
                fractionalLog.println("NFKD      : " + Default.ucd().getCodeAndName(nfkd));
                String nfd = Default.nfd().normalize(s);
                if (!nfd.equals(nfkd)) {
                    fractionalLog.println("NFD       : " + Default.ucd().getCodeAndName(nfd));
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

    /* We do not compute fractional implicit weights any more.
    static int getImplicitPrimary(int cp) {
        return FractionalUCA.implicit.getImplicitFromCodePoint(cp);
    }
    */

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

    /* We do not compute fractional implicit weights any more.
    static ImplicitCEGenerator implicit = new ImplicitCEGenerator(FractionalUCA.Variables.IMPLICIT_BASE_BYTE, FractionalUCA.Variables.IMPLICIT_MAX_BYTE);
    */

    //    static final boolean needsCaseBit(String x) {
    //        String s = Default.nfkd().normalize(x);
    //        if (!Default.ucd().getCase(s, FULL, LOWER).equals(s)) {
    //            return true;
    //        }
    //        if (!CaseBit.toSmallKana(s).equals(s)) {
    //            return true;
    //        }
    //        return false;
    //    }

    private static int ucaFirstNonVariable;

    /**
     * Analyzes the UCA primary weights.
     * <ul>
     * <li>Determines the lengths of the corresponding fractional weights.
     * <li>Sets a flag for the first UCA primary in each reordering group.
     * </ul>
     */
    private static void findBumps() {
        int[] groupChar = new int[ReorderCodes.LIMIT];

        BitSet threeByteSymbolPrimaries = new BitSet();
        UnicodeSet threeByteChars = new UnicodeSet();
        ucaFirstNonVariable = getCollator().getStatistics().firstDucetNonVariable;


        RoBitSet primarySet = getCollator().getStatistics().getPrimarySet();
        int firstScriptPrimary = getCollator().getStatistics().firstScript;

        for (int primary = primarySet.nextSetBit(0); primary >= 0; primary = primarySet.nextSetBit(primary+1)) {
            CharSequence ch2 = getCollator().getRepresentativePrimary(primary);
            int ch = Character.codePointAt(ch2,0);
            byte cat = FractionalUCA.getFixedCategory(ch);
            byte script = FractionalUCA.getFixedScript(ch);

            // see if we have an "infrequent" character: make it a 3 byte if so.
            // also collect data on primaries

            if (primary < firstScriptPrimary) {
                int reorderCode = ReorderCodes.getSpecialReorderCode(ch);
                if (primary > 0) {
                    if (groupFirstPrimary[reorderCode] == 0 || primary < groupFirstPrimary[reorderCode]) {
                        groupFirstPrimary[reorderCode] = primary;
                        groupChar[script] = ch;
                    }
                }
                if (ch < 0xFF || reorderCode == ReorderCodes.SPACE) {
                    // do nothing, assume Latin 1 is "frequent"
                } else if (cat == UCD_Types.OTHER_SYMBOL ||
                        cat == UCD_Types.MATH_SYMBOL ||
                        cat == UCD_Types.MODIFIER_SYMBOL) {
                    // Note: We do not test reorderCode == ReorderCodes.SYMBOL
                    // because that includes Lm etc.
                    threeByteSymbolPrimaries.set(primary);
                    threeByteChars.addAll(ch2.toString());
                } else {
                    if (script != UCD_Types.COMMON_SCRIPT &&
                            script != UCD_Types.INHERITED_SCRIPT &&
                            !MAJOR_SCRIPTS.get(script)) {
                        threeByteSymbolPrimaries.set(primary);
                        threeByteChars.addAll(ch2.toString());
                    }
                }
                continue;
            }

            if (script == UCD_Types.COMMON_SCRIPT || script == UCD_Types.INHERITED_SCRIPT || script == UCD_Types.Unknown_Script) {
                continue;
            }
            boolean TESTING = false;
            if (TESTING && script == UCD_Types.SYRIAC_SCRIPT) {
                System.out.println(Default.ucd().getName(ch));
            }

            // get least primary for script
            if (groupFirstPrimary[script] == 0 || groupFirstPrimary[script] > primary) {
                if (cat <= UCD_Types.OTHER_LETTER && cat != UCD_Types.Lm) {
                    groupFirstPrimary[script] = primary;
                    groupChar[script] = ch;
                    if (TESTING && script == UCD_Types.GREEK_SCRIPT) {
                        System.out.println("*" + Utility.hex(primary) + Default.ucd().getName(ch));
                    }
                }
            }
        }

        System.out.println("Bump at ducet: " + Utility.hex(ucaFirstNonVariable));
        System.out.println("3-byte primaries" + threeByteChars.toPattern(false));

        // capture in order the ranges that are major vs minor
        TreeMap<Integer, Row.R2<Boolean, Integer>> majorPrimary =
                new TreeMap<Integer, Row.R2<Boolean, Integer>>();

        // set bumps
        for (int reorderCode = 0; reorderCode < ReorderCodes.LIMIT; ++reorderCode) {
            int primary = groupFirstPrimary[reorderCode];
            if (primary > 0) {
                boolean isMajor;
                if (reorderCode < ReorderCodes.FIRST) {
                    isMajor = MAJOR_SCRIPTS.get(reorderCode);
                } else {
                    isMajor = true;
                    int nextGroupPrimary = reorderCode < ReorderCodes.DIGIT ?  // TODO: HACK
                            groupFirstPrimary[reorderCode + 1] :
                            groupFirstPrimary[UCD_Types.LATIN_SCRIPT];
                    reorderingTokenOverridesNonFractional.put(
                            nextGroupPrimary, ReorderCodes.getNameForSpecial(reorderCode));
                }
                majorPrimary.put(primary, Row.of(isMajor, reorderCode));
                UCAPrimaryProps props = UCAPrimaryProps.getOrCreate(primary);
                props.startsGroup = isMajor;
                props.reorderCodeIfFirst = reorderCode;
                if (isMajor) {
                    FractionalUCA.newLeadByteForUCAPrimary.set(primary);
                    System.out.println("Bumps:\t" + Utility.hex(primary) + " "
                            + ReorderCodes.getName(reorderCode) + " "
                            + Utility.hex(groupChar[reorderCode]) + " "
                            + Default.ucd().getName(groupChar[reorderCode]));
                }
            }
        }

        // now add ranges of primaries that are major, for selecting 2 byte vs 3 byte forms.
        int lastPrimary = 1;
        boolean lastMajor = true;
        int lastScript = UCD_Types.COMMON_SCRIPT;
        for (Entry<Integer, Row.R2<Boolean, Integer>> majorPrimaryEntry : majorPrimary.entrySet()) {
            int primary = majorPrimaryEntry.getKey();
            boolean major = majorPrimaryEntry.getValue().get0();
            int script = majorPrimaryEntry.getValue().get1();
            addMajorPrimaries(lastPrimary, primary-1, lastMajor, lastScript);
            if (lastScript == UCD_Types.HANGUL_SCRIPT) {
                for (int i = lastPrimary; i < primary; ++i) {
                    CharSequence ch2 = getCollator().getRepresentativePrimary(i);
                    if (!UCD.isModernJamo(Character.codePointAt(ch2, 0))) {
                        twoBytePrimaryForUCAPrimary.clear(i);
                    }
                }
            }
            lastPrimary = primary;
            lastMajor = major;
            lastScript = script;
        }
        int veryLastUCAPrimary = primarySet.size() - 1;
        addMajorPrimaries(lastPrimary, veryLastUCAPrimary, lastMajor, lastScript);
        twoBytePrimaryForUCAPrimary.andNot(threeByteSymbolPrimaries);

        char[][] singlePairs = {{'a','z'}, {' '}, {'0', '9'}, {'.'},  {','},}; // , {'\u3041', '\u30F3'}
        for (int j = 0; j < singlePairs.length; ++j) {
            char start = singlePairs[j][0];
            char end = singlePairs[j][singlePairs[j].length == 1 ? 0 : 1];
            for (char k = start; k <= end; ++k) {
                FractionalUCA.setSingleBytePrimaryFor(k);
            }
        }
    }

    private static boolean isThreeByteMajorScript(int script) {
        // Some scripts are "major" (and start reordering groups)
        // but have too many primaries for two bytes with a single lead byte.
        // If they are uncased, that is, they have mostly common secondary/tertiary weights,
        // then they lend themselves to using 3-byte primaries because
        // their CEs can be stored compactly as long-primary CEs,
        // and the then-possible primary sort key compression makes sort keys hardly longer.
        return
                script == UCD_Types.ETHIOPIC_SCRIPT ||
                script == UCD_Types.MYANMAR_SCRIPT;
    }

    private static boolean isTwoByteMinorScript(int script) {
        // Coptic is not a "major" script,
        // but it fits into the Greek lead byte even with 2-byte primaries.
        // This is desirable because Coptic is a cased script,
        // and the CEs for the uppercase characters cannot be stored as "long primary" CEs.
        // (They would have to use less efficient storage.)
        //
        // Similar for Glagolitic: Cased, fits into the second Cyrillic lead byte.
        //
        // Note: We could also do this for Deseret:
        // It is also cased and has relatively few primaries,
        // but making them two-byte primaries would take up too much space in its reordering group
        // and would push the group to two lead bytes and to not being compressible any more.
        // Not worth it.
        // At least *lowercase* Deseret sorts in code point order
        // and can therefore be stored as a compact range.
        return
                script == UCD_Types.COPTIC ||
                script == UCD_Types.GLAGOLITIC;
    }

    private static void addMajorPrimaries(int startPrimary, int endPrimary, boolean isMajor, int script) {
        if (isMajor ? !isThreeByteMajorScript(script) : isTwoByteMinorScript(script)) {
            twoBytePrimaryForUCAPrimary.set(startPrimary, endPrimary + 1);
        }
        System.out.println("Major:\t" + isMajor + "\t" + UCD.getScriptID_fromIndex((byte)script)
                + "\t" + hexBytes(startPrimary) + ".." + hexBytes(endPrimary));
    }

    private static int getLeadByte(long weight) {
        int w = (int)weight;
        for (int shift = 24; shift >= 0; shift -= 8) {
            int b = w >>> shift;
            if (b != 0) {
                return b;
            }
        }
        return 0;
    }

    private static void hexBytes(long x, StringBuffer result) {
        int oldLength = result.length();
        //byte lastb = 1;
        for (int shift = 24; shift >= 0; shift -= 8) {
            byte b = (byte)(x >>> shift);
            if (b != 0) {
                if (result.length() != oldLength) {
                    result.append(" ");
                }
                result.append(Utility.hex(b));
                //if (lastb == 0) System.err.println(" bad zero byte: " + result);
            }
            //lastb = b;
        }
    }

    private static String hexBytes(long x) {
        StringBuffer temp = new StringBuffer();
        hexBytes(x, temp);
        return temp.toString();
    }

    private static final BitSet MAJOR_SCRIPTS = new BitSet();
    static {
        for (byte i : new Byte[]{
                UCD_Types.ARABIC_SCRIPT,
                UCD_Types.ARMENIAN_SCRIPT,
                UCD_Types.BENGALI_SCRIPT,
                UCD_Types.BOPOMOFO_SCRIPT,
                UCD_Types.CYRILLIC_SCRIPT,
                UCD_Types.DEVANAGARI_SCRIPT,
                UCD_Types.ETHIOPIC_SCRIPT,
                UCD_Types.GEORGIAN_SCRIPT,
                UCD_Types.GREEK_SCRIPT,
                UCD_Types.GUJARATI_SCRIPT,
                UCD_Types.GURMUKHI_SCRIPT,
                UCD_Types.HAN_SCRIPT,
                UCD_Types.HANGUL_SCRIPT,
                UCD_Types.HEBREW_SCRIPT,
                UCD_Types.HIRAGANA_SCRIPT,
                UCD_Types.KANNADA_SCRIPT,
                UCD_Types.KATAKANA_SCRIPT,
                UCD_Types.KHMER_SCRIPT,
                UCD_Types.LAO_SCRIPT,
                UCD_Types.LATIN_SCRIPT,
                UCD_Types.MALAYALAM_SCRIPT,
                UCD_Types.MYANMAR_SCRIPT,
                UCD_Types.ORIYA_SCRIPT,
                UCD_Types.SINHALA_SCRIPT,
                UCD_Types.TAMIL_SCRIPT,
                UCD_Types.TELUGU_SCRIPT,
                UCD_Types.THAANA_SCRIPT,
                UCD_Types.THAI_SCRIPT,
                UCD_Types.TIBETAN_SCRIPT
        }) {
            MAJOR_SCRIPTS.set(i&0xFF);
        }
    }
    /* package */ static byte getFixedScript(int ch) {
        byte script = Default.ucd().getScript(ch);
        // HACK
        if (ch == 0x0F7E || ch == 0x0F7F) {
            if (script != UCD_Types.TIBETAN_SCRIPT) {
                throw new IllegalArgumentException("Illegal script values");
            }
            //script = TIBETAN_SCRIPT;
        }
        if (script == UCD_Types.HIRAGANA_SCRIPT) {
            script = UCD_Types.KATAKANA_SCRIPT;
        }
        return script;
    }

    /* package */ static byte getFixedCategory(int ch) {
        byte cat = Default.ucd().getCategory(ch);
        // HACK
        if (ch == 0x0F7E || ch == 0x0F7F) {
            cat = UCD_Types.OTHER_LETTER;
        }
        return cat;
    }


    /**
     * Helper class for reorder codes:
     * Script code (0..FF) or reordering group code
     * ({@link ReorderCodes#FIRST}..{@link ReorderCodes#LIMIT}).
     */
    private static class ReorderCodes {
        private static final int FIRST = 0x100;
        private static final int SPACE = 0x100;
        private static final int PUNCTUATION = 0x101;
        private static final int SYMBOL = 0x102;
        private static final int CURRENCY = 0x103;
        private static final int DIGIT = 0x104;
        private static final int LIMIT = 0x105;

        private static final String[] SPECIAL_NAMES = {
            "SPACE", "PUNCTUATION", "SYMBOL", "CURRENCY", "DIGIT"
        };

        private static final int getSpecialReorderCode(int ch) {
            byte cat = FractionalUCA.getFixedCategory(ch);
            switch (cat) {
            case UCD_Types.SPACE_SEPARATOR:
            case UCD_Types.LINE_SEPARATOR:
            case UCD_Types.PARAGRAPH_SEPARATOR:
            case UCD_Types.CONTROL:
                return SPACE;
            case UCD_Types.DASH_PUNCTUATION:
            case UCD_Types.START_PUNCTUATION:
            case UCD_Types.END_PUNCTUATION:
            case UCD_Types.CONNECTOR_PUNCTUATION:
            case UCD_Types.OTHER_PUNCTUATION:
            case UCD_Types.INITIAL_PUNCTUATION:
            case UCD_Types.FINAL_PUNCTUATION:
                return PUNCTUATION;
            case UCD_Types.OTHER_SYMBOL:
            case UCD_Types.MATH_SYMBOL:
            case UCD_Types.MODIFIER_SYMBOL:
                return SYMBOL;
            case UCD_Types.CURRENCY_SYMBOL:
                return CURRENCY;
            case UCD_Types.DECIMAL_DIGIT_NUMBER:
            case UCD_Types.LETTER_NUMBER:
            case UCD_Types.OTHER_NUMBER:
                return DIGIT;
            default:
                // Lm etc.
                return SYMBOL;
            }
        }

        private static final String getNameForSpecial(int reorderCode) {
            return SPECIAL_NAMES[reorderCode - FIRST];
        }

        private static final String getName(int reorderCode) {
            if (reorderCode < FIRST) {
                return UCD.getScriptID_fromIndex((byte) reorderCode);
            } else {
                return SPECIAL_NAMES[reorderCode - FIRST];
            }
        }
    }

    /**
     * First UCA primary weight per reordering group.
     */
    private static final int[] groupFirstPrimary = new int[ReorderCodes.LIMIT];
    /**
     * Reordering groups with sort-key-compressible fractional primary weights.
     * Only the first script in the group needs to be marked.
     * These are a subset of the {@link #MAJOR_SCRIPTS}.
     *
     * <p>Whether a group is compressible should be determined by whether all of its primaries
     * fit into one lead byte, but we need to know this before assigning
     * fractional primary weights so that we can assign them optimally.
     */
    private static final boolean[] groupIsCompressible = new boolean[ReorderCodes.LIMIT];

    static {
        groupIsCompressible[UCD_Types.GREEK_SCRIPT] = true;
        groupIsCompressible[UCD_Types.GEORGIAN_SCRIPT] = true;
        groupIsCompressible[UCD_Types.ARMENIAN_SCRIPT] = true;
        groupIsCompressible[UCD_Types.HEBREW_SCRIPT] = true;
        groupIsCompressible[UCD_Types.THAANA_SCRIPT] = true;
        groupIsCompressible[UCD_Types.ETHIOPIC_SCRIPT] = true;
        groupIsCompressible[UCD_Types.DEVANAGARI_SCRIPT] = true;
        groupIsCompressible[UCD_Types.BENGALI_SCRIPT] = true;
        groupIsCompressible[UCD_Types.GURMUKHI_SCRIPT] = true;
        groupIsCompressible[UCD_Types.GUJARATI_SCRIPT] = true;
        groupIsCompressible[UCD_Types.ORIYA_SCRIPT] = true;
        groupIsCompressible[UCD_Types.TAMIL_SCRIPT] = true;
        groupIsCompressible[UCD_Types.TELUGU_SCRIPT] = true;
        groupIsCompressible[UCD_Types.KANNADA_SCRIPT] = true;
        groupIsCompressible[UCD_Types.MALAYALAM_SCRIPT] = true;
        groupIsCompressible[UCD_Types.SINHALA_SCRIPT] = true;
        groupIsCompressible[UCD_Types.THAI_SCRIPT] = true;
        groupIsCompressible[UCD_Types.LAO_SCRIPT] = true;
        groupIsCompressible[UCD_Types.TIBETAN_SCRIPT] = true;
        groupIsCompressible[UCD_Types.MYANMAR_SCRIPT] = true;
        groupIsCompressible[UCD_Types.KHMER_SCRIPT] = true;
        groupIsCompressible[UCD_Types.HANGUL_SCRIPT] = true;
        groupIsCompressible[UCD_Types.KATAKANA_SCRIPT] = true;
        groupIsCompressible[UCD_Types.BOPOMOFO_SCRIPT] = true;
    }

    private static void setSingleBytePrimaryFor(char ch) {
        CEList ces = getCollator().getCEList(String.valueOf(ch), true);
        int firstPrimary = CEList.getPrimary(ces.at(0));
        FractionalUCA.singleBytePrimaryForUCAPrimary.set(firstPrimary);
        if (ch == 'a') {
            FractionalUCA.Variables.gapForA = firstPrimary;
        }
    }

    private static BitSet singleBytePrimaryForUCAPrimary = new BitSet();
    private static BitSet twoBytePrimaryForUCAPrimary = new BitSet();
    private static BitSet newLeadByteForUCAPrimary = new BitSet();
    /**
     * One flag per fractional primary lead byte for whether
     * the fractional weights that start with that byte are sort-key-compressible.
     */
    private static BitSet compressibleBytes = new BitSet(256);

    private static boolean isEven(int x) {
        return (x & 1) == 0;
    }

    private static int fixCompatibilityCE(String s, boolean decompose, int[] output, boolean compress) {
        byte type = WriteCollationData.getDecompType(UTF16.charAt(s, 0));
        //char ch = s.charAt(0);

        String decomp = Default.nfkd().normalize(s);
        int len = 0;
        int markLen = getCollator().getCEs(decomp, true, WriteCollationData.markCes);
        if (compress) {
            markLen = WriteCollationData.kenCompress(WriteCollationData.markCes, markLen);
        }

        //for (int j = 0; j < decomp.length(); ++j) {
        for (int k = 0; k < markLen; ++k) {
            int t = CEList.getTertiary(WriteCollationData.markCes[k]);
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
                    CEList.getPrimary(WriteCollationData.markCes[k]),
                    CEList.getSecondary(WriteCollationData.markCes[k]),
                    t);
            //}
        }
        return len;
    }

    private static int getFractionalLengthForUCAPrimary(int ucaPrimary) {
        if (singleBytePrimaryForUCAPrimary.get(ucaPrimary)) {
            return 1;
        }
        if (twoBytePrimaryForUCAPrimary.get(ucaPrimary)) {
            return 2;
        }
        return 3;
    }
}
