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
 * @author ram
 */

/**
 * Class that implements the PunyCode algorithm for encode/decode
 * @draft
 */
final public class Punycode {

    boolean showProgress = true;

    /* Punycode parameters for Bootstring */
    private final int BASE;
    private static final int TMIN           = 1;
    private static final int TMAX           = 26;
    private static final int SKEW           = 38;
    private static final int DAMP           = 700;
    private static final int INITIAL_BIAS   = 72;
    private static final int INITIAL_N      = 0x80;

    /* "Basic" Unicode/ASCII code points */
    private final char DELIMITER;

    private static final int MAX_CP_COUNT   = 200;
    //private static final int UINT_MAGIC     = 0x80000000;
    //private static final long ULONG_MAGIC   = 0x8000000000000000L;

    public Punycode() {
        this("abcdefghijklmnopqrstuvwxyz0123456789", '-');
    }

    public Punycode(String digits, char delimiter) {
        for (int i = 0; i < 256; ++i) {
            basicToDigit[i] = -1;
            digitToBasic2[i] = -1;
        }
        final int length = digits.length();
        for (int i = 0; i < length; ++i) {
            final char c = digits.charAt(i);
            if (c > 0xFF) {
                throw new IllegalArgumentException("Illegal character, must be 0..FF: " + Integer.toHexString(c));
            }
            if (basicToDigit[c] >= 0) {
                throw new IllegalArgumentException("Illegal character, cannot repeat in string: " + Integer.toHexString(c));
            }
            basicToDigit[c] = i;
            digitToBasic2[i] = c;
        }
        BASE = length;
        DELIMITER = delimiter;
    }

    private int adaptBias(int delta, int length, boolean firstTime){
        if(firstTime){
            delta /=DAMP;
        }else{
            delta /=  2;
        }
        delta += delta/length;

        int count=0;
        for(; delta>((BASE-TMIN)*TMAX)/2; count+=BASE) {
            delta/=(BASE-TMIN);
        }

        return count+(((BASE-TMIN+1)*delta)/(delta+SKEW));
    }

    /**
     * basicToDigit[] contains the numeric value of a basic code
     * point (for use in representing integers) in the range 0 to
     * BASE-1, or -1 if b is does not represent a value.
     */
    final int[] basicToDigit = new int[256];

    /**
     * The reverse mapping, digit to basic
     */

    final int[] digitToBasic2 = new int[256];

    /**
     * Converts Unicode to Punycode.
     * The input string must not contain single, unpaired surrogates.
     * The output will be represented as an array of ASCII code points.
     * 
     * @param src
     * @param caseFlags
     * @return
     * @throws ParseException
     * @draft
     */
    public StringBuilder encode(CharSequence src, StringBuilder dest) throws StringPrepParseException{
        final int srcLength = src.length();
        final int[] cpBuffer = new int[srcLength];
        int cpBeingEncoded, delta, handledCPCount, basicLength, bias, j, nextLargerCodePoint, q, k, t, srcCPCount;
        char c, c2;

        if (showProgress) {
            System.out.println("NEW DECODE");
        }

        /*
         * Handle the basic code points and
         * convert extended ones to UTF-32 in cpBuffer (caseFlag in sign bit):
         */
        srcCPCount=0;

        for(j=0; j<srcLength; ++j) {
            c=src.charAt(j);
            if(c < 0x80
                    //&& basicToDigit[c] >= 0
                    ) {
                cpBuffer[srcCPCount++]=0;
                dest.append(c);
            } else {
                cpBeingEncoded=0;
                if(!UTF16.isSurrogate(c)) {
                    cpBeingEncoded|=c;
                } else if(UTF16.isLeadSurrogate(c) && (j+1)<srcLength && UTF16.isTrailSurrogate(c2=src.charAt(j+1))) {
                    ++j;

                    cpBeingEncoded|=UCharacter.getCodePoint(c, c2);
                } else {
                    /* error: unmatched surrogate */
                    throw new StringPrepParseException("Illegal char found",StringPrepParseException.ILLEGAL_CHAR_FOUND);
                }
                cpBuffer[srcCPCount++]=cpBeingEncoded;
            }
        }

        /* Finish the basic string - if it is not empty - with a delimiter. */
        basicLength = ((CharSequence) dest).length();
        if(basicLength>0) {
            dest.append(DELIMITER);
        }
        if (showProgress) {
            System.out.println("Base: " + dest);
            System.out.println("cpBuffer: " + show(cpBuffer));
        }


        /*
         * handledCPCount is the number of code points that have been handled
         * basicLength is the number of basic code points
         * destLength is the number of chars that have been output
         */

        /* Initialize the state: */
        cpBeingEncoded=INITIAL_N;
        delta=0;
        bias=INITIAL_BIAS;

        /* Main encoding loop: */
        for(handledCPCount=basicLength; handledCPCount<srcCPCount; /* no op */) {

            if (showProgress) {
                System.out.println("Left: " + srcCPCount + ", " + handledCPCount);
            }

            /*
             * All non-basic code points < n have been handled already.
             * Find the next larger one:
             */
            for(nextLargerCodePoint=0x7fffffff, j=0; j<srcCPCount; ++j) {
                q=cpBuffer[j];
                if(cpBeingEncoded<=q && q<nextLargerCodePoint) {
                    nextLargerCodePoint=q;
                }
            }

            if (showProgress) {
                System.out.println("Next larger: " + Integer.toString(nextLargerCodePoint, 16));
            }

            /*
             * Increase delta enough to advance the decoder's
             * <n,i> state to <m,0>, but guard against overflow:
             */
            if(nextLargerCodePoint-cpBeingEncoded>(0x7fffffff-srcLength-delta)/(handledCPCount+1)) {
                throw new IllegalStateException("Internal program error");
            }

            // delta is composed of the delta to the next character * gap (the characters so far)
            // below, we'll also add to it slowly as we find each new identical character in the input

            delta+=(nextLargerCodePoint-cpBeingEncoded)*(handledCPCount+1);
            cpBeingEncoded=nextLargerCodePoint;
            if (showProgress) {
                System.out.println("\tStart Delta: " + Integer.toString(delta, 16));
            }

            /* Encode a sequence of same code points n */
            int startWriting = 0;
            for(j=0; j<srcCPCount; ++j) {
                q=cpBuffer[j];
                if(q<cpBeingEncoded) {
                    ++delta;
                } else if(q==cpBeingEncoded) {
                    if (showProgress) {
                        System.out.println("\t\tDelta: " + Integer.toString(delta, 16));
                        startWriting = dest.length();
                    }
                    /* Represent delta as a generalized variable-length integer: */
                    for(q=delta, k=BASE; /* no condition */; k+=BASE) {

                        t=k-bias;
                        if(t<TMIN) {
                            t=TMIN;
                        } else if(k>=(bias+TMAX)) {
                            t=TMAX;
                        }

                        if(q<t) {
                            break;
                        }

                        dest.append((char) digitToBasic2[(t+(q-t)%(BASE-t))]);

                        q=(q-t)/(BASE-t);
                    }

                    dest.append((char) digitToBasic2[q]);

                    bias=adaptBias(delta, handledCPCount+1,(handledCPCount==basicLength));
                    delta=0;
                    ++handledCPCount;
                }
            }
            if (showProgress) {
                System.out.println("\tChars: " + dest.substring(startWriting));
            }

            ++delta;
            ++cpBeingEncoded;
        }

        return dest;
    }

    private static boolean isSurrogate(int ch){
        return (((ch)&0xfffff800)==0xd800);
    }

    /**
     * Converts Punycode to Unicode.
     * The Unicode string will be at most as long as the Punycode string.
     * 
     * @param src
     * @param caseFlags
     * @return
     * @throws ParseException
     * @draft
     */
    public StringBuffer decode(CharSequence src, StringBuffer dest) throws StringPrepParseException{
        final int srcLength = src.length();

        int n, i, bias, basicLength, j, in, oldi, w, k, digit, t,
        destCPCount, firstSupplementaryIndex;

        /*
         * Handle the basic code points:
         * Let basicLength be the number of input code points
         * before the last delimiter, or 0 if there is none,
         * then copy the first basicLength code points to the output.
         *
         * The two following loops iterate backward.
         */
        for(j=srcLength; j>0;) {
            if(src.charAt(--j)==DELIMITER) {
                break;
            }
        }
        basicLength=destCPCount=j;

        for (j = 0; j < basicLength; ++j) {
            final char b=src.charAt(j);
            if(b > 0x80
                    // || basicToDigit[b] < 0
                    ) {
                throw new StringPrepParseException("Illegal char found", StringPrepParseException.INVALID_CHAR_FOUND);
            }
            dest.append(b);
        }

        /* Initialize the state: */
        n=INITIAL_N;
        i=0;
        bias=INITIAL_BIAS;
        firstSupplementaryIndex=1000000000;

        /*
         * Main decoding loop:
         * Start just after the last delimiter if any
         * basic code points were copied; start at the beginning otherwise.
         */
        for(in=basicLength>0 ? basicLength+1 : 0; in<srcLength; /* no op */) {
            /*
             * in is the index of the next character to be consumed, and
             * destCPCount is the number of code points in the output array.
             *
             * Decode a generalized variable-length integer into delta,
             * which gets added to i.  The overflow checking is easier
             * if we increase i as we go, then subtract off its starting
             * value at the end to obtain delta.
             */
            for(oldi=i, w=1, k=BASE; /* no condition */; k+=BASE) {
                if(in>=srcLength) {
                    throw new StringPrepParseException("Illegal char found", StringPrepParseException.ILLEGAL_CHAR_FOUND);
                }

                digit=basicToDigit[src.charAt(in++) & 0xFF];
                if(digit<0) {
                    throw new StringPrepParseException("Invalid char found", StringPrepParseException.INVALID_CHAR_FOUND);
                }
                if(digit>(0x7fffffff-i)/w) {
                    /* integer overflow */
                    throw new StringPrepParseException("Illegal char found", StringPrepParseException.ILLEGAL_CHAR_FOUND);
                }

                i+=digit*w;
                t=k-bias;
                if(t<TMIN) {
                    t=TMIN;
                } else if(k>=(bias+TMAX)) {
                    t=TMAX;
                }
                if(digit<t) {
                    break;
                }

                if(w>0x7fffffff/(BASE-t)) {
                    /* integer overflow */
                    throw new StringPrepParseException("Illegal char found", StringPrepParseException.ILLEGAL_CHAR_FOUND);
                }
                w*=BASE-t;
            }

            /*
             * Modification from sample code:
             * Increments destCPCount here,
             * where needed instead of in for() loop tail.
             */
            ++destCPCount;
            bias=adaptBias(i-oldi, destCPCount, (oldi==0));

            /*
             * i was supposed to wrap around from (incremented) destCPCount to 0,
             * incrementing n each time, so we'll fix that now:
             */
            if(i/destCPCount>(0x7fffffff-n)) {
                /* integer overflow */
                throw new StringPrepParseException("Illegal char found", StringPrepParseException.ILLEGAL_CHAR_FOUND);
            }

            n+=i/destCPCount;
            i%=destCPCount;
            /* not needed for Punycode: */
            /* if (decode_digit(n) <= BASE) return punycode_invalid_input; */

            if(n>0x10ffff || isSurrogate(n)) {
                /* Unicode code point overflow */
                throw new StringPrepParseException("Illegal char found", StringPrepParseException.ILLEGAL_CHAR_FOUND);
            }

            /* Insert n at position i of the output: */

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
            if(i<=firstSupplementaryIndex) {
                codeUnitIndex=i;
                if(n > 0xFFFF) {
                    firstSupplementaryIndex=codeUnitIndex;
                } else {
                    ++firstSupplementaryIndex;
                }
            } else {
                codeUnitIndex=firstSupplementaryIndex;
                codeUnitIndex=UTF16.moveCodePointOffset(dest, codeUnitIndex, i-codeUnitIndex);
            }

            /* use the UChar index codeUnitIndex instead of the code point index i */
            insertCodePoint(dest, codeUnitIndex, n);

            ++i;
        }
        return dest;
    }

    String valueOfCodePoint(int codepoint) {
        return codepoint < 0x10000 ? String.valueOf((char)codepoint) : new StringBuilder(2).appendCodePoint(codepoint).toString();
    }
    StringBuilder insertCodePoint(StringBuilder target, int offset, int codepoint) {
        return codepoint < 0x10000 ? target.insert(offset, (char) codepoint) : target.insert(offset, valueOfCodePoint(codepoint));
    }
    StringBuffer insertCodePoint(StringBuffer target, int offset, int codepoint) {
        return codepoint < 0x10000 ? target.insert(offset, (char) codepoint) : target.insert(offset, valueOfCodePoint(codepoint));
    }
    static String show(int[] source) {
        String result = "";
        for (final int item : source) {
            result += item + ",";
        }
        return result;
    }
}

