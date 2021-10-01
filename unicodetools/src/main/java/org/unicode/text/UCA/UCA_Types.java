/**
 *******************************************************************************
 * Copyright (C) 1996-2001, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /home/cvsroot/unicodetools/org/unicode/text/UCA/UCA_Types.java,v $
 *
 *******************************************************************************
 */

package org.unicode.text.UCA;

import java.util.BitSet;

public interface UCA_Types {
    public static final char LEVEL_SEPARATOR = '\u0000';

    static final int NEUTRAL_SECONDARY = 0x20;
    // http://www.unicode.org/reports/tr10/#Tertiary_Weight_Table
    static final int NEUTRAL_TERTIARY = 0x02;
    static final int SMALL_HIRAGANA_TERTIARY = 0x0D;
    static final int NORMAL_HIRAGANA_TERTIARY = 0x0E;
    static final int MAX_TERTIARY = 0x1F;

    /**
     * Uppercase and normal-Kana UCA tertiary weights.
     * Bits/weights 08-0C, 0E, 11, 12, 1D.
     * See http://www.unicode.org/reports/tr10/#Tertiary_Weight_Table
     */
    static final BitSet uppercaseTertiaries = BitSet.valueOf(new long[] { 0x20065F00 });

    /** Enum for alternate handling */
    public static final byte SHIFTED = 0, ZEROED = 1, NON_IGNORABLE = 2, SHIFTED_TRIMMED = 3, LAST = 3;

    /**
     * Used to terminate a list of CEs
     */
    public static final int TERMINATOR = 0xFFFFFFFF;   // CE that marks end of string

    /**
     *  Special char value that means failed or terminated
     */
    static final char NOT_A_CHAR = '\uFFFF';
}
