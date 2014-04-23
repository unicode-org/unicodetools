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

import org.unicode.text.utility.Settings;

public interface UCA_Types {
    /**
     * Version of the UCA tables to use
     */
    public static final String ALLFILES = "allkeys"; // null if not there

    public static final String BASE_UCA_GEN_DIR = Settings.GEN_DIR + "uca/"; // UCD_Types.GEN_DIR + "collation" + "/";
    public static final char LEVEL_SEPARATOR = '\u0000';

    static final int NEUTRAL_SECONDARY = 0x20;
    static final int NEUTRAL_TERTIARY = 0x02;

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
     * Any unsupported characters (those not in the UCA data tables)
     * are marked with a exception bit combination
     * so that they can be treated specially.<br>
     * There are at least 34 values, so that we can use a range for surrogates
     * However, we do add to the first weight if we have surrogate pairs!
     */
    static final int UNSUPPORTED_CJK_BASE = 0xFB40;
    static final int UNSUPPORTED_CJK_AB_BASE = 0xFB80;
    static final int UNSUPPORTED_OTHER_BASE = 0xFBC0;

    static final int UNSUPPORTED_BASE = UNSUPPORTED_CJK_BASE;
    static final int UNSUPPORTED_LIMIT = UNSUPPORTED_OTHER_BASE + 0x40;


    /**
     *  Special char value that means failed or terminated
     */
    static final char NOT_A_CHAR = '\uFFFF';
}
