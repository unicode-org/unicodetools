/** */
package org.unicode.text.UCA;

import com.ibm.icu.text.UnicodeSet;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

public class UCA_Statistics {

    int[] secondaryCount = new int[0x200];
    int[] tertiaryCount = new int[0x80];
    int[][] stCounts = {null, null, secondaryCount, tertiaryCount};

    private final BitSet primarySet = new BitSet();
    private final BitSet secondarySet = new BitSet();
    private final BitSet tertiarySet = new BitSet();
    private final RoBitSet primarySetRo = new RoBitSet(primarySet);
    private final RoBitSet secondarySetRo = new RoBitSet(secondarySet);
    private final RoBitSet tertiarySetRo = new RoBitSet(tertiarySet);

    Map<Integer, StringBuilder> representativePrimary = new HashMap<Integer, StringBuilder>();
    Map<Integer, StringBuilder> representativePrimarySeconds =
            new HashMap<Integer, StringBuilder>();

    /** For recording statistics */
    int count1 = 0, count2 = 0, count3 = 0, max2 = 0, max3 = 0;

    int oldKey1 = -1, oldKey2 = -1, oldKey3 = -1;
    UnicodeSet found = new UnicodeSet();

    boolean haveUnspecified = false;
    UnicodeSet unspecified = new UnicodeSet();
    UnicodeSet variantSecondaries = new UnicodeSet(0x0153, 0x0154); // TODO, fix
    UnicodeSet digitSecondaries = new UnicodeSet(0x155, 0x017F); // TODO, fix
    UnicodeSet homelessSecondaries;
    /** Just for statistics */
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

    public RoBitSet getPrimarySet() {
        return primarySetRo;
    }

    public RoBitSet getSecondarySet() {
        return secondarySetRo;
    }

    public RoBitSet getTertiarySet() {
        return tertiarySetRo;
    }

    // HACK for CJK
    // secondarySet.set(0x0040);

    public static class RoBitSet {
        private final BitSet guts;

        public RoBitSet(BitSet bitSetToProtect) {
            guts = bitSetToProtect;
        }

        public int length() {
            return guts.length();
        }

        public boolean get(int i) {
            return guts.get(i);
        }

        public int nextSetBit(int i) {
            return guts.nextSetBit(i);
        }

        public int size() {
            return guts.size();
        }
    }

    public void setPrimary(int key1) {
        primarySet.set(key1);
    }

    public void setSecondary(int key2) {
        secondarySet.set(key2);
    }

    public void setTertiary(int key3) {
        tertiarySet.set(key3);
    }
}
