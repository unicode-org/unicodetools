package org.unicode.text.UCA;

import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.UCD_Types;
import org.unicode.text.utility.Utility;

/**
 * Constants and helper functions for fractional UCA. We use this small class for shared definitions
 * so that large classes need not depend on each other.
 *
 * @since 2013-jan-02 (some of these pulled out of {@link FractionalUCA})
 */
public final class Fractional {
    public static final int SPECIAL_BASE = 0xF0;
    public static final int IMPLICIT_BASE_BYTE = 0xE0;
    public static final int IMPLICIT_MAX_BYTE = IMPLICIT_BASE_BYTE + 4;

    /** Common (default) secondary weight. */
    public static final int COMMON_SEC = 5;
    /**
     * Top of the byte range for compressing a sequence of common secondary weights into a single
     * sort key byte.
     *
     * <p>The bytes {@link #COMMON_SEC_TOP}+1 .. {@link #FIRST_SEC_ASSIGNED}-1 provide a large gap
     * for tailoring secondary-after a primary CE with a common secondary.
     */
    public static final int COMMON_SEC_TOP = 0x45;
    /** First non-common secondary weight byte used in FractionalUCA.txt. */
    public static final int FIRST_SEC_ASSIGNED = 0x70;
    /**
     * First secondary weight byte of any secondary CE (primary ignorable). Not actually assigned in
     * FractionalUCA.txt.
     *
     * <p>The bytes {@link #FIRST_IGNORABLE_SEC} .. {@link #FIRST_IGNORABLE_SEC_ASSIGNED}-1 provide
     * a gap for tailoring secondary-before the first secondary CE.
     */
    public static final int FIRST_IGNORABLE_SEC = 0x80;
    /** First secondary weight byte of any secondary CE in FractionalUCA.txt. */
    public static final int FIRST_IGNORABLE_SEC_ASSIGNED = 0x82;
    /**
     * First secondary weight byte after secondary CEs in FractionalUCA.txt.
     *
     * <p>The bytes {@link #FIRST_IGNORABLE_SEC_RESERVED} .. 0xFF provide a gap for tailoring
     * secondary-after the last secondary CE.
     */
    public static final int FIRST_IGNORABLE_SEC_RESERVED = 0xFC;

    /**
     * Common (default) tertiary weight. It has been the same as {@link #COMMON_SEC} but need not
     * be.
     *
     * <p>The bytes {@link #COMMON_TER}+1 .. {@link #FIRST_TER_ASSIGNED}-1 provide a gap for
     * tailoring tertiary-after a primary or secondary CE with a common tertiary.
     */
    public static final int COMMON_TER = 5;
    /** First non-common tertiary weight byte used in FractionalUCA.txt. */
    public static final int FIRST_TER_ASSIGNED = 0x10;
    /**
     * First non-common tertiary weight byte used in FractionalUCA.txt for primary+secondary with
     * very few distinct tertiary weights.
     */
    public static final int FIRST_TER_FEW_ASSIGNED = 0x20;
    /**
     * First tertiary weight byte of any tertiary CE (secondary ignorable). Not actually assigned in
     * FractionalUCA.txt.
     *
     * <p>The bytes {@link #FIRST_IGNORABLE_TER} .. {@link #FIRST_IGNORABLE_TER_ASSIGNED}-1 provide
     * a gap for tailoring tertiary-before the first tertiary CE.
     */
    public static final int FIRST_IGNORABLE_TER = 0x3C;
    /**
     * First tertiary weight byte of any tertiary CE in FractionalUCA.txt. There are none in UCA 6.3
     * or earlier.
     */
    public static final int FIRST_IGNORABLE_TER_ASSIGNED = 0x3D;
    /**
     * First tertiary weight byte after tertiary CEs in FractionalUCA.txt.
     *
     * <p>The bytes {@link #FIRST_IGNORABLE_TER_RESERVED} .. 0x3F provide a gap for tailoring
     * tertiary-after the last tertiary CE.
     */
    public static final int FIRST_IGNORABLE_TER_RESERVED = 0x3E;

    public static int getLeadByte(long weight) {
        final int w = (int) weight;
        for (int shift = 24; shift >= 0; shift -= 8) {
            final int b = w >>> shift;
            if (b != 0) {
                return b;
            }
        }
        return 0;
    }

    public static class WeightIterator {
        private int b1;
        private int limitByte1;
        private int b2;
        private int limitByte2;
        private int numSingles;
        private int numTwoByteWeights;
        private int inc;

        /**
         * Constructs a weight iterator that returns numWeights single-byte and/or two-byte weights.
         * For a mix of weight lengths, the second byte is incremented by about desiredInc2. If
         * there are very few weights, then they are all single-byte weights with at least a
         * one-byte gap. If there are very many weights, then they are all two-byte weights with at
         * least a two-byte gap.
         *
         * @param b1 lowest byte 1 value
         * @param limitByte1 exclusive-limit byte 1 value
         * @param limitByte2 exclusive-limit byte 2 value
         * @param numWeights the number of weights to return
         * @param desiredInc2 the desired byte 2 increment for a medium number of weights
         * @throws IllegalArgumentException if too many weights are requested
         */
        private WeightIterator(
                int b1, int limitByte1, int limitByte2, int numWeights, int desiredInc2) {
            this.b1 = b1;
            this.limitByte1 = limitByte1;
            b2 = 2;
            this.limitByte2 = limitByte2;

            if (numWeights <= 0) {
                return;
            }

            final int numBytes1 = limitByte1 - b1;
            inc = numBytes1 / numWeights;
            if (inc >= 2) {
                // Single byte for each weight.
                numSingles = numWeights;
                return;
            }

            final int numBytes2 = limitByte2 - 2;
            // int numTwoByteLeads = (numWeights + desiredInc2 - 1) / desiredInc2;
            int numTwoByteLeads = (numWeights * desiredInc2 + numBytes2 - 1) / numBytes2;
            if (numTwoByteLeads <= numBytes1) {
                // Some single-byte weights, some two-byte weights.
                for (; ; ) {
                    numSingles = (numBytes1 - numTwoByteLeads) / 2;
                    numTwoByteWeights = numWeights - numSingles;
                    if (numTwoByteWeights * desiredInc2 <= (numTwoByteLeads - 1) * numBytes2) {
                        // if (numTwoByteWeights <= (numTwoByteLeads - 1) * desiredInc2) {
                        // After rounding, and after subtracting the single-byte weights,
                        // we do not need as many two-byte lead bytes.
                        --numTwoByteLeads;
                        continue;
                    } else {
                        break;
                    }
                }
                if (numSingles >= 1) {
                    inc = (numBytes1 - numTwoByteLeads) / numSingles;
                    assert inc >= 2;
                    return;
                }
            }

            // Only two-byte weights.
            final int maxWeights = numBytes1 * numBytes2;
            // There must be at least a 1-weight gap between any pair of weights.
            inc = maxWeights / numWeights;
            if (inc >= 2) {
                numTwoByteWeights = numWeights;
                return;
            }

            throw new IllegalArgumentException(
                    String.format(
                            "Too many weights for one/two-byters %02X 02..%02X %02X",
                            b1, limitByte1 - 1, limitByte2 - 1));
        }

        /**
         * Returns a single-byte or two-byte weight and increments the iterator state. Returns 0
         * after all requested weights have been returned.
         */
        public int nextWeight() {
            if (numSingles > 0) {
                assert b1 < limitByte1;
                final int w = b1;
                b1 += inc;
                if (--numSingles == 0 && numTwoByteWeights > 0) {
                    // Switch to two-byte weights.
                    inc = ((limitByte1 - b1) * (limitByte2 - 2)) / numTwoByteWeights;
                    assert inc >= 2;
                }
                return w;
            }
            if (numTwoByteWeights > 0) {
                assert b1 < limitByte1;
                final int w = (b1 << 8) | b2;
                b2 += inc;
                while (b2 >= limitByte2) {
                    ++b1;
                    b2 = 2 + b2 - limitByte2;
                }
                --numTwoByteWeights;
                return w;
            }
            return 0;
        }
    }

    public static WeightIterator assignSecondaryWeightsForPrimaryCEs(int numWeights) {
        return new WeightIterator(FIRST_SEC_ASSIGNED, FIRST_IGNORABLE_SEC, 0x100, numWeights, 32);
    }

    public static WeightIterator assignSecondaryWeightsForSecondaryCEs(int numWeights) {
        return new WeightIterator(
                FIRST_IGNORABLE_SEC_ASSIGNED, FIRST_IGNORABLE_SEC_RESERVED, 0x100, numWeights, 32);
    }

    public static WeightIterator assignTertiaryWeightsForSecondaryCEs(int numWeights) {
        return new WeightIterator(
                numWeights <= 2 ? FIRST_TER_FEW_ASSIGNED : FIRST_TER_ASSIGNED,
                FIRST_IGNORABLE_TER,
                0x40,
                numWeights,
                20);
    }

    public static WeightIterator assignTertiaryWeightsForTertiaryCEs(int numWeights) {
        return new WeightIterator(
                FIRST_IGNORABLE_TER_ASSIGNED, FIRST_IGNORABLE_TER_RESERVED, 0x40, numWeights, 20);
    }

    public static void hexBytes(long x, StringBuffer result) {
        final int oldLength = result.length();
        // byte lastb = 1;
        for (int shift = 24; shift >= 0; shift -= 8) {
            final byte b = (byte) (x >>> shift);
            if (b != 0) {
                if (result.length() != oldLength) {
                    result.append(" ");
                }
                result.append(Utility.hex(b));
                // if (lastb == 0) System.err.println(" bad zero byte: " + result);
            }
            // lastb = b;
        }
    }

    public static String hexBytes(long x) {
        final StringBuffer temp = new StringBuffer();
        hexBytes(x, temp);
        return temp.toString();
    }

    /* package */ static short getFixedScript(int ch) {
        short script = Default.ucd().getScript(ch);
        // HACK
        if (ch == 0x0F7E || ch == 0x0F7F) {
            if (script != UCD_Types.TIBETAN_SCRIPT) {
                throw new IllegalArgumentException("Illegal script values");
            }
            // script = TIBETAN_SCRIPT;
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
}
