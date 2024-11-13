package org.unicode.draft;

/*
 *******************************************************************************
 * Copyright (C) 2003-2008, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.StringPrepParseException;
import com.ibm.icu.text.UTF16;

/**
 * Ported code from ICU punycode.c
 *
 * @author ram
 */

/**
 * Class that implements the PunyCode algorithm for encode/decode
 *
 * @draft
 */
public final class OldPunycode {
    static boolean showProgress = false;
    /* Punycode parameters for Bootstring */
    private static final int BASE = 36;
    private static final int TMIN = 1;
    private static final int TMAX = 26;
    private static final int SKEW = 38;
    private static final int DAMP = 700;
    private static final int INITIAL_BIAS = 72;
    private static final int INITIAL_N = 0x80;

    /* "Basic" Unicode/ASCII code points */
    private static final int HYPHEN = 0x2d;
    private static final int DELIMITER = HYPHEN;

    private static final int ZERO = 0x30;
    // private static final int NINE           = 0x39;

    private static final int SMALL_A = 0x61;
    private static final int SMALL_Z = 0x7a;

    private static final int CAPITAL_A = 0x41;
    private static final int CAPITAL_Z = 0x5a;
    private static final int MAX_CP_COUNT = 200;

    // private static final int UINT_MAGIC     = 0x80000000;
    // private static final long ULONG_MAGIC   = 0x8000000000000000L;

    private static int adaptBias(int delta, int length, boolean firstTime) {
        if (firstTime) {
            delta /= DAMP;
        } else {
            delta /= 2;
        }
        delta += delta / length;

        int count = 0;
        for (; delta > ((BASE - TMIN) * TMAX) / 2; count += BASE) {
            delta /= (BASE - TMIN);
        }

        return count + (((BASE - TMIN + 1) * delta) / (delta + SKEW));
    }

    /**
     * basicToDigit[] contains the numeric value of a basic code point (for use in representing
     * integers) in the range 0 to BASE-1, or -1 if b is does not represent a value.
     */
    static final int[] basicToDigit =
            new int[] {
                -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                26, 27, 28, 29, 30, 31, 32, 33, 34, 35, -1, -1, -1, -1, -1, -1,
                -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14,
                15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1,
                -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14,
                15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1,
                -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
            };

    /// CLOVER:OFF
    private static char asciiCaseMap(char b, boolean uppercase) {
        if (uppercase) {
            if (SMALL_A <= b && b <= SMALL_Z) {
                b -= (SMALL_A - CAPITAL_A);
            }
        } else {
            if (CAPITAL_A <= b && b <= CAPITAL_Z) {
                b += (SMALL_A - CAPITAL_A);
            }
        }
        return b;
    }

    /// CLOVER:ON
    /**
     * digitToBasic() returns the basic code point whose value (when used for representing integers)
     * is d, which must be in the range 0 to BASE-1. The lowercase form is used unless the uppercase
     * flag is nonzero, in which case the uppercase form is used.
     */
    private static char digitToBasic(int digit, boolean uppercase) {
        /*  0..25 map to ASCII a..z or A..Z */
        /* 26..35 map to ASCII 0..9         */
        if (digit < 26) {
            if (uppercase) {
                return (char) (CAPITAL_A + digit);
            } else {
                return (char) (SMALL_A + digit);
            }
        } else {
            return (char) ((ZERO - 26) + digit);
        }
    }

    /**
     * Converts Unicode to Punycode. The input string must not contain single, unpaired surrogates.
     * The output will be represented as an array of ASCII code points.
     *
     * @param src
     * @param caseFlags
     * @return
     * @throws ParseException
     * @draft
     */
    public static StringBuffer encode(StringBuffer src, boolean[] caseFlags)
            throws StringPrepParseException {
        if (showProgress) {
            System.out.println("OLD DECODE");
        }
        final int[] cpBuffer = new int[src.length()];
        int n, delta, handledCPCount, basicLength, destLength, bias, j, m, q, k, t, srcCPCount;
        char c, c2;
        final int srcLength = src.length();
        final int destCapacity = MAX_CP_COUNT;
        final char[] dest = new char[destCapacity];
        final StringBuffer result = new StringBuffer();
        /*
         * Handle the basic code points and
         * convert extended ones to UTF-32 in cpBuffer (caseFlag in sign bit):
         */
        srcCPCount = destLength = 0;

        for (j = 0; j < srcLength; ++j) {
            if (srcCPCount == MAX_CP_COUNT) {
                /* too many input code points */
                throw new IndexOutOfBoundsException();
            }
            c = src.charAt(j);
            if (isBasic(c)) {
                if (destLength < destCapacity) {
                    cpBuffer[srcCPCount++] = 0;
                    dest[destLength] = caseFlags != null ? asciiCaseMap(c, caseFlags[j]) : (char) c;
                }
                ++destLength;
            } else {
                n = ((caseFlags != null && caseFlags[j]) ? 1 : 0) << 31L;
                if (!UTF16.isSurrogate(c)) {
                    n |= c;
                } else if (UTF16.isLeadSurrogate(c)
                        && (j + 1) < srcLength
                        && UTF16.isTrailSurrogate(c2 = src.charAt(j + 1))) {
                    ++j;

                    n |= UCharacter.getCodePoint(c, c2);
                } else {
                    /* error: unmatched surrogate */
                    throw new StringPrepParseException(
                            "Illegal char found", StringPrepParseException.ILLEGAL_CHAR_FOUND);
                }
                cpBuffer[srcCPCount++] = n;
            }
        }

        /* Finish the basic string - if it is not empty - with a delimiter. */
        basicLength = destLength;
        if (basicLength > 0) {
            if (destLength < destCapacity) {
                dest[destLength] = DELIMITER;
            }
            ++destLength;
        }

        if (showProgress) {
            System.out.println("Base: " + String.valueOf(dest, 0, destLength));
            System.out.println("cpBuffer: " + Punycode.show(cpBuffer));
        }
        /*
         * handledCPCount is the number of code points that have been handled
         * basicLength is the number of basic code points
         * destLength is the number of chars that have been output
         */

        /* Initialize the state: */
        n = INITIAL_N;
        delta = 0;
        bias = INITIAL_BIAS;

        /* Main encoding loop: */
        for (handledCPCount = basicLength; handledCPCount < srcCPCount; /* no op */ ) {
            if (showProgress) {
                System.out.println("Left: " + srcCPCount + ", " + handledCPCount);
            }
            /*
             * All non-basic code points < n have been handled already.
             * Find the next larger one:
             */
            for (m = 0x7fffffff, j = 0; j < srcCPCount; ++j) {
                q = cpBuffer[j] & 0x7fffffff; /* remove case flag from the sign bit */
                if (n <= q && q < m) {
                    m = q;
                }
            }

            if (showProgress) {
                System.out.println("Next larger: " + Integer.toString(m, 16));
            }

            /*
             * Increase delta enough to advance the decoder's
             * <n,i> state to <m,0>, but guard against overflow:
             */
            if (m - n > (0x7fffffff - MAX_CP_COUNT - delta) / (handledCPCount + 1)) {
                throw new IllegalStateException("Internal program error");
            }
            delta += (m - n) * (handledCPCount + 1);
            n = m;

            if (showProgress) {
                System.out.println("\tStart Delta: " + Integer.toString(delta, 16));
            }
            int startWriting = 0;

            /* Encode a sequence of same code points n */
            for (j = 0; j < srcCPCount; ++j) {
                q = cpBuffer[j] & 0x7fffffff; /* remove case flag from the sign bit */
                if (q < n) {
                    ++delta;
                } else if (q == n) {
                    if (showProgress) {
                        System.out.println("\t\tDelta: " + Integer.toString(delta, 16));
                        startWriting = destLength;
                    }
                    /* Represent delta as a generalized variable-length integer: */
                    for (q = delta, k = BASE; /* no condition */ ; k += BASE) {

                        /**
                         * RAM: comment out the old code for conformance with
                         * draft-ietf-idn-punycode-03.txt
                         *
                         * <p>t=k-bias; if(t<TMIN) { t=TMIN; } else if(t>TMAX) { t=TMAX; }
                         */
                        t = k - bias;
                        if (t < TMIN) {
                            t = TMIN;
                        } else if (k >= (bias + TMAX)) {
                            t = TMAX;
                        }

                        if (q < t) {
                            break;
                        }

                        if (destLength < destCapacity) {
                            dest[destLength++] = digitToBasic(t + (q - t) % (BASE - t), false);
                        }
                        q = (q - t) / (BASE - t);
                    }

                    if (destLength < destCapacity) {
                        dest[destLength++] = digitToBasic(q, (cpBuffer[j] < 0));
                    }
                    bias = adaptBias(delta, handledCPCount + 1, (handledCPCount == basicLength));
                    delta = 0;
                    ++handledCPCount;
                }
            }
            if (showProgress) {
                System.out.println("\tChars: " + String.valueOf(dest, startWriting, destLength));
            }

            ++delta;
            ++n;
        }

        return result.append(dest, 0, destLength);
    }

    private static boolean isBasic(int ch) {
        return (ch < INITIAL_N);
    }

    /// CLOVER:OFF
    private static boolean isBasicUpperCase(int ch) {
        return (CAPITAL_A <= ch && ch >= CAPITAL_Z);
    }

    /// CLOVER:ON
    private static boolean isSurrogate(int ch) {
        return (((ch) & 0xfffff800) == 0xd800);
    }

    /**
     * Converts Punycode to Unicode. The Unicode string will be at most as long as the Punycode
     * string.
     *
     * @param src
     * @param caseFlags
     * @return
     * @throws ParseException
     * @draft
     */
    public static StringBuffer decode(StringBuffer src, boolean[] caseFlags)
            throws StringPrepParseException {
        final int srcLength = src.length();
        final StringBuffer result = new StringBuffer();
        int n,
                destLength,
                i,
                bias,
                basicLength,
                j,
                in,
                oldi,
                w,
                k,
                digit,
                t,
                destCPCount,
                firstSupplementaryIndex,
                cpLength;
        char b;
        final int destCapacity = MAX_CP_COUNT;
        final char[] dest = new char[destCapacity];

        /*
         * Handle the basic code points:
         * Let basicLength be the number of input code points
         * before the last delimiter, or 0 if there is none,
         * then copy the first basicLength code points to the output.
         *
         * The two following loops iterate backward.
         */
        for (j = srcLength; j > 0; ) {
            if (src.charAt(--j) == DELIMITER) {
                break;
            }
        }
        destLength = basicLength = destCPCount = j;

        while (j > 0) {
            b = src.charAt(--j);
            if (!isBasic(b)) {
                throw new StringPrepParseException(
                        "Illegal char found", StringPrepParseException.INVALID_CHAR_FOUND);
            }

            if (j < destCapacity) {
                dest[j] = b;

                if (caseFlags != null) {
                    caseFlags[j] = isBasicUpperCase(b);
                }
            }
        }

        /* Initialize the state: */
        n = INITIAL_N;
        i = 0;
        bias = INITIAL_BIAS;
        firstSupplementaryIndex = 1000000000;

        /*
         * Main decoding loop:
         * Start just after the last delimiter if any
         * basic code points were copied; start at the beginning otherwise.
         */
        for (in = basicLength > 0 ? basicLength + 1 : 0; in < srcLength; /* no op */ ) {
            /*
             * in is the index of the next character to be consumed, and
             * destCPCount is the number of code points in the output array.
             *
             * Decode a generalized variable-length integer into delta,
             * which gets added to i.  The overflow checking is easier
             * if we increase i as we go, then subtract off its starting
             * value at the end to obtain delta.
             */
            for (oldi = i, w = 1, k = BASE; /* no condition */ ; k += BASE) {
                if (in >= srcLength) {
                    throw new StringPrepParseException(
                            "Illegal char found", StringPrepParseException.ILLEGAL_CHAR_FOUND);
                }

                digit = basicToDigit[src.charAt(in++) & 0xFF];
                if (digit < 0) {
                    throw new StringPrepParseException(
                            "Invalid char found", StringPrepParseException.INVALID_CHAR_FOUND);
                }
                if (digit > (0x7fffffff - i) / w) {
                    /* integer overflow */
                    throw new StringPrepParseException(
                            "Illegal char found", StringPrepParseException.ILLEGAL_CHAR_FOUND);
                }

                i += digit * w;
                t = k - bias;
                if (t < TMIN) {
                    t = TMIN;
                } else if (k >= (bias + TMAX)) {
                    t = TMAX;
                }
                if (digit < t) {
                    break;
                }

                if (w > 0x7fffffff / (BASE - t)) {
                    /* integer overflow */
                    throw new StringPrepParseException(
                            "Illegal char found", StringPrepParseException.ILLEGAL_CHAR_FOUND);
                }
                w *= BASE - t;
            }

            /*
             * Modification from sample code:
             * Increments destCPCount here,
             * where needed instead of in for() loop tail.
             */
            ++destCPCount;
            bias = adaptBias(i - oldi, destCPCount, (oldi == 0));

            /*
             * i was supposed to wrap around from (incremented) destCPCount to 0,
             * incrementing n each time, so we'll fix that now:
             */
            if (i / destCPCount > (0x7fffffff - n)) {
                /* integer overflow */
                throw new StringPrepParseException(
                        "Illegal char found", StringPrepParseException.ILLEGAL_CHAR_FOUND);
            }

            n += i / destCPCount;
            i %= destCPCount;
            /* not needed for Punycode: */
            /* if (decode_digit(n) <= BASE) return punycode_invalid_input; */

            if (n > 0x10ffff || isSurrogate(n)) {
                /* Unicode code point overflow */
                throw new StringPrepParseException(
                        "Illegal char found", StringPrepParseException.ILLEGAL_CHAR_FOUND);
            }

            /* Insert n at position i of the output: */
            cpLength = UTF16.getCharCount(n);
            if ((destLength + cpLength) < destCapacity) {
                int codeUnitIndex;

                /*
                 * Handle indexes when supplementary code points are present.
                 *
                 * In almost all cases, there will be only BMP code points before i
                 * and even in the entire string.
                 * This is handled with the same efficiency as with UTF-32.
                 *
                 * Only the rare cases with supplementary code points are handled
                 * more slowly - but not too bad since this is an insertion anyway.
                 */
                if (i <= firstSupplementaryIndex) {
                    codeUnitIndex = i;
                    if (cpLength > 1) {
                        firstSupplementaryIndex = codeUnitIndex;
                    } else {
                        ++firstSupplementaryIndex;
                    }
                } else {
                    codeUnitIndex = firstSupplementaryIndex;
                    codeUnitIndex =
                            UTF16.moveCodePointOffset(
                                    dest, 0, destLength, codeUnitIndex, i - codeUnitIndex);
                }

                /* use the UChar index codeUnitIndex instead of the code point index i */
                if (codeUnitIndex < destLength) {
                    System.arraycopy(
                            dest,
                            codeUnitIndex,
                            dest,
                            codeUnitIndex + cpLength,
                            (destLength - codeUnitIndex));
                    if (caseFlags != null) {
                        System.arraycopy(
                                caseFlags,
                                codeUnitIndex,
                                caseFlags,
                                codeUnitIndex + cpLength,
                                destLength - codeUnitIndex);
                    }
                }
                if (cpLength == 1) {
                    /* BMP, insert one code unit */
                    dest[codeUnitIndex] = (char) n;
                } else {
                    /* supplementary character, insert two code units */
                    dest[codeUnitIndex] = UTF16.getLeadSurrogate(n);
                    dest[codeUnitIndex + 1] = UTF16.getTrailSurrogate(n);
                }
                if (caseFlags != null) {
                    /* Case of last character determines uppercase flag: */
                    caseFlags[codeUnitIndex] = isBasicUpperCase(src.charAt(in - 1));
                    if (cpLength == 2) {
                        caseFlags[codeUnitIndex + 1] = false;
                    }
                }
            }
            destLength += cpLength;
            ++i;
        }
        result.append(dest, 0, destLength);
        return result;
    }
}
