/**
 *******************************************************************************
 * Copyright (C) 1996-2001, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /home/cvsroot/unicodetools/org/unicode/text/UCA/CEList.java,v $ 
 *
 *******************************************************************************
 */

package org.unicode.text.UCA;
import org.unicode.text.UCD.UCD_Types;
import org.unicode.text.utility.IntStack;
import org.unicode.text.utility.Utility;

/**
 * Immutable list of collation element integers.
 */
public final class CEList implements java.lang.Comparable<CEList>, UCD_Types {
    private int[] contents;
    private int startOffset;
    private int endOffset;
    private int count;

    /**
     * Constructs a new list by copying source[start:end].
     */
    public CEList(int[] source, int start, int end) {
        count = end-start;
        contents = new int[count];
        System.arraycopy(source, start, contents, 0, count);
        startOffset = 0;
        endOffset = count;
    }

    /**
     * Constructs a new list by copying source.
     */
    public CEList(int[] source) {
        this(source, 0, source.length);
    }

    /**
     * Constructs a new list by <i>aliasing</i> source[start:end].
     * @param spare ignored
     */
    private CEList(int[] source, int start, int end, boolean spare) {
        contents = source;
        startOffset = start;
        endOffset = end;
        count = end - start;
    }

    /**
     * Returns a new list with the concatenation of this &amp; that.
     */
    public CEList append(CEList that) {
        int[] newContents = new int[count + that.count];
        System.arraycopy(contents, startOffset, newContents, 0, count);
        System.arraycopy(that.contents, that.startOffset, newContents, count, that.count);
        return new CEList(newContents, 0, count + that.count, true);
    }

    /**
     * Returns a new list with the slice [start:end].
     */
    public CEList sub(int start, int end) {
        return new CEList(contents, startOffset + start, startOffset + end, true);
    }

    /**
     * Returns a new list with the slice [0:end].
     */
    public CEList start(int end) {
        return new CEList(contents, startOffset, startOffset + end, true);
    }

    /**
     * Returns a new list with the slice [start:length()].
     */
    public CEList end(int start) {
        return new CEList(contents, startOffset + start, contents.length, true);
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public int length() {
        return count;
    }

    /**
     * Returns the i-th collation element.
     */
    public int at(int i) {
        if (i < 0 || i >= count) throw new ArrayIndexOutOfBoundsException(i);
        return contents[startOffset + i];
    }

    public int hashCode() {
        int result = count;
        for (int i = startOffset; i < endOffset; ++i) {
            result *= 37;
            result += contents[i];
        }
        return result;
    }

    public boolean equals(Object other) {
        try {
            CEList that = (CEList)other;
            if (count != that.count) return false;
            int delta = that.startOffset - startOffset;
            for (int i = startOffset; i < endOffset; ++i) {
                if (contents[i] != that.contents[i + delta]) return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    public int compareTo(CEList other) {
        return compareTo(other, 0xFFFFFFFFL);
    }
    
    public int compareTo(CEList other, long mask) {
        CEList that = other;
        try {
            int delta = that.startOffset - startOffset;
            int min = endOffset;
            int min2 = that.endOffset - delta;
            if (min > min2) min = min2;

            for (int i = startOffset; i < min; ++i) {
                long first = contents[i] & mask;
                long second = that.contents[i + delta] & mask;
                if (first != second) {
                    return first < second ? -1 : 1;
                }
            }
            if (count < that.count) return -1;
            if (count > that.count) return 1;
            return 0;
        } catch (RuntimeException e) {
            System.out.println("This: " + this + ", that: " + other);
            System.out.println(startOffset + ", " + endOffset
                    + ", " + count + ", " + contents.length);
            System.out.println(that.startOffset + ", " + that.endOffset
                    + ", " + that.count + ", " + that.contents.length);
            throw e;
        }
    }

    public static byte remap(int ch, byte type, int t) {
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
        return (byte)t;
    }

    public String toString() {
        if (isEmpty()) return toString(0);

        StringBuilder result = new StringBuilder();
        for (int i = startOffset; i < endOffset; ++i) {
            if (i != startOffset) result.append(' ');
            result.append(toString(contents[i]));
        }
        return result.toString();
    }

    public static String toString(int[] ces, int len) {
        if (len <= 0) return toString(0);

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < len; ++i) {
            if (i != 0) result.append(' ');
            result.append(toString(ces[i]));
        }
        return result.toString();
    }

    public static String toString(IntStack ces) {
        if (ces.length() <= 0) return toString(0);

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < ces.length(); ++i) {
            if (i != 0) result.append(' ');
            result.append(toString(ces.get(i)));
        }
        return result.toString();
    }

    public static String toString(int ce) {
        return "[" + Utility.hex(getPrimary(ce)) + "." 
        + Utility.hex(getSecondary(ce)) + "."
        + Utility.hex(getTertiary(ce)) + "]"
        // + "(" + NAME3[getTertiary(ce)] + ")"
        ;
    }

    static final String[] NAME3 = {
        "IGNORE",    // 0
        "BLK",     // Unused?
        "MIN",
        "WIDE",
        "COMPAT",
        "FONT",
        "CIRCLE",
        "RES-2",
        "CAP",
        "WIDECAP",
        "COMPATCAP",
        "FONTCAP",
        "CIRCLECAP",
        "HIRA-SMALL",
        "HIRA",
        "SMALL",
        "SMALL-NARROW",
        "KATA",
        "NARROW",
        "CIRCLE-KATA",
        "SUP-MNN",
        "SUB-MNS",
        "VERT", // Missing??
        "AINI",
        "AMED",
        "AFIN",
        "AISO",
        "NOBREAK", // Missing?
        "SQUARED",
        "SQUAREDCAP",
        "FRACTION",
        "MAX"
    };

    public boolean isPrimaryIgnorable() {
        for (int i = startOffset; i < endOffset; ++i) {
            int ce = contents[i];
            if (getPrimary(ce) != 0) return false;
        }
        return true;
    }

    /**
     * Returns the primary weight from a 32-bit CE.
     * The primary is 16 bits, stored in b31..b16.
     */
    public static char getPrimary(int ce) {
        return (char)(ce >>> 16);
    }

    public static final int SECONDARY_MAX = 0x1FF;
    public static final int TERTIARY_MAX = 0x7F;

    /**
     * Returns the secondary weight from a 32-bit CE.
     * The secondary is 9 bits, stored in b15..b7.
     */
    public static char getSecondary(int ce) {
        return (char)((ce >>> 7) & SECONDARY_MAX);
    }

    /**
     * Returns the tertiary weight from a 32-bit CE.
     * The tertiary is 7 bits, stored in b6..b0.
     */
    public static char getTertiary(int ce) {
        return (char)(ce & TERTIARY_MAX);
    }

    // testing

    public static void main(String args[]) throws Exception {
        /* This: [0241.0020.0004], that: [0F6B.0020.0002]
            1, 2, 1, 2
            0, 1, 1, 1
         */
        CEList t1 = new CEList(new int[] {0, 0x02412004});
        t1 = t1.sub(1,2);
        CEList t2 = new CEList(new int[] {0x0F6B2002});
        System.out.println(t1.compareTo(t2));
        System.out.println("t1[0]=" + Integer.toHexString(t1.at(0)) +
                " t2[0]=" + Integer.toHexString(t2.at(0)));

        CEList foo = new CEList(new int[] {0, 1, 2, 3, 4});
        CEList fuu = new CEList(new int[] {});
        int cc = foo.compareTo(fuu);
        System.out.println(cc);

        System.out.println(foo);
        System.out.println(foo.start(2));
        System.out.println(foo.end(1));
        CEList fii = new CEList(new int[] {2, 3});
        CEList foo2 = foo.sub(2,4);
        System.out.println(fii.equals(foo2));
        System.out.println(fii.compareTo(foo2));
        System.out.println(fii.compareTo(foo));
        System.out.println(fii.hashCode() == foo2.hashCode());

    }
}
