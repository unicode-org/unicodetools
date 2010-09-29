/**
 * 
 */
package org.unicode.text.UCA;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import com.ibm.icu.text.UnicodeSet;

public class UCA_Statistics {

    int[] secondaryCount = new int[0x200];
    int[] tertiaryCount = new int[0x80];
    int[][] stCounts = {null, null, secondaryCount, tertiaryCount};
    BitSet primarySet = new BitSet();
    BitSet secondarySet = new BitSet();
    BitSet tertiarySet = new BitSet();
    Map<Integer,StringBuilder> representativePrimary = new HashMap<Integer,StringBuilder>();
    Map<Integer,StringBuilder> representativePrimarySeconds = new HashMap<Integer,StringBuilder>();

    /**
     * For recording statistics
     */
    int count1 = 0, count2 = 0, count3 = 0, max2 = 0, max3 = 0;
    int oldKey1 = -1, oldKey2 = -1, oldKey3 = -1;
    UnicodeSet found = new UnicodeSet();

    boolean haveUnspecified = false;
    UnicodeSet unspecified = new UnicodeSet();
    UnicodeSet variantSecondaries = new UnicodeSet(0x0153,0x0154); // TODO, fix
    UnicodeSet digitSecondaries = new UnicodeSet(0x155,0x017F); // TODO, fix
    UnicodeSet homelessSecondaries;
    /**
     * Just for statistics
     */
    int lastUniqueVariable = 0;
    int renumberedVariable = 50;
    char MIN1 = '\uFFFF'; // start large; will be reset as table is built
    char MIN2 = '\uFFFF'; // start large; will be reset as table is built
    char MIN3 = '\uFFFF'; // start large; will be reset as table is built
    char MAX1 = '\u0000'; // start small; will be reset as table is built
    char MAX2 = '\u0000'; // start small; will be reset as table is built
    char MAX3 = '\u0000'; // start small; will be reset as table is built

    public int firstDucetNonVariable = -1;
    public int firstScript = -1;
}