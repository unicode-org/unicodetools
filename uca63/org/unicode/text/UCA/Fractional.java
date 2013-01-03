package org.unicode.text.UCA;

import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.UCD_Types;
import org.unicode.text.utility.Utility;

/**
 * Constants and helper functions for fractional UCA.
 * We use this small class for shared definitions so that
 * large classes need not depend on each other.
 *
 * @since 2013-jan-02 (some of these pulled out of {@link FractionalUCA})
 */
public final class Fractional {
    public static final int  SPECIAL_BASE       = 0xF0;
    public static final int  IMPLICIT_BASE_BYTE = 0xE0;
    public static final int  IMPLICIT_MAX_BYTE  = IMPLICIT_BASE_BYTE + 4;

    /**
     * Common (default) secondary weight.
     */
    public static final int COMMON_SEC = 5;
    public static final int COMMON_TER = 5;

    public static int getLeadByte(long weight) {
        int w = (int)weight;
        for (int shift = 24; shift >= 0; shift -= 8) {
            int b = w >>> shift;
            if (b != 0) {
                return b;
            }
        }
        return 0;
    }

    public static void hexBytes(long x, StringBuffer result) {
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

    public static String hexBytes(long x) {
        StringBuffer temp = new StringBuffer();
        hexBytes(x, temp);
        return temp.toString();
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
}
