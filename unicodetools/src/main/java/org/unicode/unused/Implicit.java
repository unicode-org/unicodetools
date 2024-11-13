// Obsolete code. Moved here from org.unicode.text.UCA on 2014-apr-23 after svn r642.
// The UCA tools do not deal with ICU 1.8-52 style implicit fractional primary weights any more.
// ICU 53 uses a range-offset mechanism for Unified_Ideograph,
// and a different implicit algorithm for unassigned code points.
package org.unicode.unused;

import org.unicode.text.UCD.UCD_Types;
import org.unicode.text.utility.Utility;

/**
 * For generation of Implicit CEs
 *
 * @author Davis
 *     <p>Cleaned up so that changes can be made more easily. Old values: # First Implicit: E26A792D
 *     # Last Implicit: E3DC70C0 # First CJK: E0030300 # Last CJK: E0A9DD00 # First CJK_A: E0A9DF00
 *     # Last CJK_A: E0DE3100
 */
public class Implicit implements UCD_Types {

    /** constants */
    static final boolean DEBUG = true;

    static final long topByte = 0xFF000000L;
    static final long bottomByte = 0xFFL;
    static final long fourBytes = 0xFFFFFFFFL;

    static final int MAX_INPUT = 0x220001; // 2 * Unicode range + 2

    /**
     * Testing function
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        System.out.println("Start");
        try {
            final Implicit foo = new Implicit(0xE0, 0xE4);

            // int x = foo.getRawImplicit(0xF810);
            foo.getRawFromImplicit(0xE20303E7);

            final int gap4 = foo.getGap4();
            System.out.println("Gap4: " + gap4);
            final int gap3 = foo.getGap3();
            final int minTrail = foo.getMinTrail();
            final int maxTrail = foo.getMaxTrail();
            long last = 0;
            long current;
            for (int i = 0; i <= MAX_INPUT; ++i) {
                current = foo.getImplicitFromRaw(i) & fourBytes;

                // check that it round-trips AND that all intervening ones are illegal
                int roundtrip = foo.getRawFromImplicit((int) current);
                if (roundtrip != i) {
                    foo.throwError("No roundtrip", i);
                }
                if (last != 0) {
                    for (long j = last + 1; j < current; ++j) {
                        roundtrip = foo.getRawFromImplicit((int) j);
                        // raise an error if it *doesn't* find an error
                        if (roundtrip != -1) {
                            foo.throwError("Fails to recognize illegal", j);
                        }
                    }
                }
                // now do other consistency checks
                final long lastBottom = last & bottomByte;
                final long currentBottom = current & bottomByte;
                final long lastTop = last & topByte;
                final long currentTop = current & topByte;

                // do some consistency checks
                /*
                long gap = current - last;
                if (currentBottom != 0) { // if we are a 4-byte
                    // gap has to be at least gap4
                    // and gap from minTrail, maxTrail has to be at least gap4
                    if (gap <= gap4) foo.throwError("Failed gap4 between", i);
                    if (currentBottom < minTrail + gap4) foo.throwError("Failed gap4 before", i);
                    if (currentBottom > maxTrail - gap4) foo.throwError("Failed gap4 after", i);
                } else { // we are a three-byte
                    gap = gap >> 8; // move gap down for comparison.
                    long current3Bottom = (current >> 8) & bottomByte;
                    if (gap <= gap3) foo.throwError("Failed gap3 between ", i);
                    if (current3Bottom < minTrail + gap3) foo.throwError("Failed gap3 before", i);
                    if (current3Bottom > maxTrail - gap3) foo.throwError("Failed gap3 after", i);
                }
                 */
                // print out some values for spot-checking
                if (lastTop != currentTop || i == 0x10000 || i == 0x110000) {
                    foo.show(i - 3);
                    foo.show(i - 2);
                    foo.show(i - 1);
                    if (i == 0) {
                        // do nothing
                    } else if (lastBottom == 0 && currentBottom != 0) {
                        System.out.println("+ primary boundary, 4-byte CE's below");
                    } else if (lastTop != currentTop) {
                        System.out.println("+ primary boundary");
                    }
                    foo.show(i);
                    foo.show(i + 1);
                    foo.show(i + 2);
                    System.out.println("...");
                }
                last = current;
            }
            foo.show(MAX_INPUT - 2);
            foo.show(MAX_INPUT - 1);
            foo.show(MAX_INPUT);
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("End");
        }
    }

    private void throwError(String title, int cp) {
        throw new IllegalArgumentException(
                title
                        + "\t"
                        + Utility.hex(cp)
                        + "\t"
                        + Utility.hex(getImplicitFromRaw(cp) & fourBytes));
    }

    private void throwError(String title, long ce) {
        throw new IllegalArgumentException(title + "\t" + Utility.hex(ce & fourBytes));
    }

    private void show(int i) {
        if (i >= 0 && i <= MAX_INPUT) {
            System.out.println(
                    Utility.hex(i) + "\t" + Utility.hex(getImplicitFromRaw(i) & fourBytes));
        }
    }

    /** Precomputed by constructor */
    int final3Multiplier;

    int final4Multiplier;
    int final3Count;
    int final4Count;
    int medialCount;
    int min3Primary;
    int min4Primary;
    int max4Primary;
    int minTrail;
    int maxTrail;
    int max3Trail;
    int max4Trail;
    int min4Boundary;

    public int getGap4() {
        return final4Multiplier - 1;
    }

    public int getGap3() {
        return final3Multiplier - 1;
    }

    // old comment
    // we must skip all 00, 01, 02, FF bytes, so most bytes have 252 values
    // we must leave a gap of 01 between all values of the last byte, so the last byte has 126
    // values (3 byte case)
    // we shift so that HAN all has the same first primary, for compression.
    // for the 4 byte case, we make the gap as large as we can fit.

    /** Supply parameters for generating implicit CEs */
    public Implicit(int minPrimary, int maxPrimary) {
        // 13 is the largest 4-byte gap we can use without getting 2 four-byte forms.
        this(minPrimary, maxPrimary, 0x04, 0xFE, 1, 1);
    }

    /**
     * Set up to generate implicits.
     *
     * @param minPrimary
     * @param maxPrimary
     * @param minTrail final byte
     * @param maxTrail final byte
     * @param gap3 the gap we leave for tailoring for 3-byte forms
     * @param primaries3count number of 3-byte primarys we can use (normally 1)
     */
    public Implicit(
            int minPrimary,
            int maxPrimary,
            int minTrail,
            int maxTrail,
            int gap3,
            int primaries3count) {
        if (DEBUG) {
            System.out.println("minPrimary: " + Utility.hex(minPrimary));
            System.out.println("maxPrimary: " + Utility.hex(maxPrimary));
            System.out.println("minTrail: " + Utility.hex(minTrail));
            System.out.println("maxTrail: " + Utility.hex(maxTrail));
            System.out.println("gap3: " + Utility.hex(gap3));
            System.out.println("primaries3count: " + primaries3count);
        }
        // some simple parameter checks
        if (minPrimary < 0 || minPrimary >= maxPrimary || maxPrimary > 0xFF) {
            throw new IllegalArgumentException("bad lead bytes");
        }
        if (minTrail < 0 || minTrail >= maxTrail || maxTrail > 0xFF) {
            throw new IllegalArgumentException("bad trail bytes");
        }
        if (primaries3count < 1) {
            throw new IllegalArgumentException("bad three-byte primaries");
        }

        this.minTrail = minTrail;
        this.maxTrail = maxTrail;

        min3Primary = minPrimary;
        max4Primary = maxPrimary;
        // compute constants for use later.
        // number of values we can use in trailing bytes
        // leave room for empty values between AND above, e.g. if gap = 2
        // range 3..7 => +3 -4 -5 -6 -7: so 1 value
        // range 3..8 => +3 -4 -5 +6 -7 -8: so 2 values
        // range 3..9 => +3 -4 -5 +6 -7 -8 -9: so 2 values
        final3Multiplier = gap3 + 1;
        final3Count = (maxTrail - minTrail + 1) / final3Multiplier;
        max3Trail = minTrail + (final3Count - 1) * final3Multiplier;

        // medials can use full range
        medialCount = (maxTrail - minTrail + 1);
        // find out how many values fit in each form
        final int threeByteCount = medialCount * final3Count;
        // now determine where the 3/4 boundary is.
        // we use 3 bytes below the boundary, and 4 above
        final int primariesAvailable = maxPrimary - minPrimary + 1;
        final int primaries4count = primariesAvailable - primaries3count;

        final int min3ByteCoverage = primaries3count * threeByteCount;
        min4Primary = minPrimary + primaries3count;
        min4Boundary = min3ByteCoverage;
        // Now expand out the multiplier for the 4 bytes, and redo.

        final int totalNeeded = MAX_INPUT - min4Boundary;
        final int neededPerPrimaryByte = divideAndRoundUp(totalNeeded, primaries4count);
        if (DEBUG) {
            System.out.println("neededPerPrimaryByte: " + neededPerPrimaryByte);
        }

        final int neededPerFinalByte =
                divideAndRoundUp(neededPerPrimaryByte, medialCount * medialCount);
        if (DEBUG) {
            System.out.println("neededPerFinalByte: " + neededPerFinalByte);
        }

        final int gap4 = (maxTrail - minTrail - 1) / neededPerFinalByte;
        if (DEBUG) {
            System.out.println("expandedGap: " + gap4);
        }
        if (gap4 < 1) {
            throw new IllegalArgumentException("must have larger gap4s");
        }

        final4Multiplier = gap4 + 1;
        final4Count = neededPerFinalByte;
        max4Trail = minTrail + (final4Count - 1) * final4Multiplier;

        if (primaries4count * medialCount * medialCount * final4Count < MAX_INPUT) {
            throw new IllegalArgumentException("internal error");
        }
        if (DEBUG) {
            System.out.println("final4Count: " + final4Count);
            for (int counter = 0; counter < final4Count; ++counter) {
                final int value = minTrail + (1 + counter) * final4Multiplier;
                System.out.println(counter + "\t" + value + "\t" + Utility.hex(value));
            }
        }
    }

    public static int divideAndRoundUp(int a, int b) {
        return 1 + (a - 1) / b;
    }

    /**
     * Converts implicit CE into raw integer
     *
     * @param implicit
     * @return -1 if illegal format
     */
    public int getRawFromImplicit(int implicit) {
        int result;
        int b3 = implicit & 0xFF;
        implicit >>= 8;
        int b2 = implicit & 0xFF;
        implicit >>= 8;
        int b1 = implicit & 0xFF;
        implicit >>= 8;
        int b0 = implicit & 0xFF;

        // simple parameter checks
        if (b0 < min3Primary || b0 > max4Primary || b1 < minTrail || b1 > maxTrail) {
            return -1;
        }
        // normal offsets
        b1 -= minTrail;

        // take care of the final values, and compose
        if (b0 < min4Primary) {
            if (b2 < minTrail || b2 > max3Trail || b3 != 0) {
                return -1;
            }
            b2 -= minTrail;
            final int remainder = b2 % final3Multiplier;
            if (remainder != 0) {
                return -1;
            }
            b0 -= min3Primary;
            b2 /= final3Multiplier;
            result = ((b0 * medialCount) + b1) * final3Count + b2;
        } else {
            if (b2 < minTrail || b2 > maxTrail || b3 < minTrail || b3 > max4Trail) {
                return -1;
            }
            b2 -= minTrail;
            b3 -= minTrail;
            final int remainder = b3 % final4Multiplier;
            if (remainder != 0) {
                return -1;
            }
            b3 /= final4Multiplier;
            b0 -= min4Primary;
            result =
                    (((b0 * medialCount) + b1) * medialCount + b2) * final4Count
                            + b3
                            + min4Boundary;
        }
        // final check
        if (result < 0 || result > MAX_INPUT) {
            return -1;
        }
        return result;
    }

    /**
     * Generate the implicit CE, from raw integer. Left shifted to put the first byte at the top of
     * an int.
     *
     * @param cp code point
     * @return
     */
    public int getImplicitFromRaw(int cp) {
        if (cp < 0 || cp > MAX_INPUT) {
            throw new IllegalArgumentException("Code point out of range " + Utility.hex(cp));
        }
        int last0 = cp - min4Boundary;
        if (last0 < 0) {
            int last1 = cp / final3Count;
            last0 = cp % final3Count;

            int last2 = last1 / medialCount;
            last1 %= medialCount;

            last0 = minTrail + last0 * final3Multiplier; // spread out, leaving gap at start
            last1 = minTrail + last1; // offset
            last2 = min3Primary + last2; // offset

            if (last2 >= min4Primary) {
                throw new IllegalArgumentException(
                        "4-byte out of range: " + Utility.hex(cp) + ", " + Utility.hex(last2));
            }

            return (last2 << 24) + (last1 << 16) + (last0 << 8);
        } else {
            int last1 = last0 / final4Count;
            last0 %= final4Count;

            int last2 = last1 / medialCount;
            last1 %= medialCount;

            int last3 = last2 / medialCount;
            last2 %= medialCount;

            last0 = minTrail + last0 * final4Multiplier; // spread out, leaving gap at start
            last1 = minTrail + last1; // offset
            last2 = minTrail + last2; // offset
            last3 = min4Primary + last3; // offset

            if (last3 > max4Primary) {
                throw new IllegalArgumentException(
                        "4-byte out of range: " + Utility.hex(cp) + ", " + Utility.hex(last3));
            }

            return (last3 << 24) + (last2 << 16) + (last1 << 8) + last0;
        }
    }

    /**
     * Gets an Implicit from a code point. Internally, swaps (which produces a raw value 0..220000,
     * then converts raw to implicit.
     *
     * @param cp
     * @return
     */
    public int getSwappedImplicit(int cp) {
        if (false && DEBUG) {
            System.out.println("Incoming: " + Utility.hex(cp));
        }

        // Produce Raw value
        // note, we add 1 so that the first value is always empty!!
        cp = Implicit.swapCJK(cp) + 1;
        // we now have a range of numbers from 0 to 220000.

        if (false && DEBUG) {
            System.out.println("CJK swapped: " + Utility.hex(cp));
        }

        return getImplicitFromRaw(cp);
    }

    /**
     * Function used to: a) collapse the 2 different Han ranges from UCA into one (in the right
     * order), and b) bump any non-CJK characters by 10FFFF. The relevant blocks are: A: 4E00..9FFF;
     * CJK Unified Ideographs F900..FAFF; CJK Compatibility Ideographs B: 3400..4DBF; CJK Unified
     * Ideographs Extension A 20000..XX; CJK Unified Ideographs Extension B (and others later on) As
     * long as no new B characters are allocated between 4E00 and FAFF, and no new A characters are
     * outside of this range, (very high probability) this simple code will work. The reordered
     * blocks are: Block1 is CJK Block2 is CJK_COMPAT_USED Block3 is CJK_A (all contiguous) Any
     * other CJK gets its normal code point Any non-CJK gets +10FFFF When we reorder Block1, we make
     * sure that it is at the very start, so that it will use a 3-byte form. Warning: the we only
     * pick up the compatibility characters that are NOT decomposed, so that block is smaller!
     */
    static int NON_CJK_OFFSET = 0x110000;

    static int swapCJK(int i) {

        if (i >= CJK_BASE) {
            if (i < CJK_LIMIT) {
                return i - CJK_BASE;
            }

            if (i < CJK_COMPAT_USED_BASE) {
                return i + NON_CJK_OFFSET;
            }

            if (i < CJK_COMPAT_USED_LIMIT) {
                return i - CJK_COMPAT_USED_BASE + (CJK_LIMIT - CJK_BASE);
            }
            if (i < CJK_B_BASE) {
                return i + NON_CJK_OFFSET;
            }

            if (i < CJK_B_LIMIT) {
                return i; // non-BMP-CJK
            }

            return i + NON_CJK_OFFSET; // non-CJK
        }
        if (i < CJK_A_BASE) {
            return i + NON_CJK_OFFSET;
        }

        if (i < CJK_A_LIMIT) {
            return i
                    - CJK_A_BASE
                    + (CJK_LIMIT - CJK_BASE)
                    + (CJK_COMPAT_USED_LIMIT - CJK_COMPAT_USED_BASE);
        }
        return i + NON_CJK_OFFSET; // non-CJK
    }

    /**
     * @return
     */
    public int getMinTrail() {
        return minTrail;
    }

    /**
     * @return
     */
    public int getMaxTrail() {
        return maxTrail;
    }
}
