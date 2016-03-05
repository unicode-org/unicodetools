package org.unicode.text.UCA;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.text.UCD.ToolUnicodePropertySource;
import org.unicode.text.UCD.UCD;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

final class RadicalStroke {
    /**
     * The Unicode 1.1 Unihan block was U+4E00..U+9FA5.
     * The ideographs there were allocated in radical-stroke order,
     * but some of the radical-stroke data was changed later.
     */
    private static final int LAST_UNIHAN_11 = 0x9fa5;

    private static final boolean DEBUG = false;

    private String unicodeVersion;
    private long[] orderedHan;
    private Map<String, String> radToChars;
    /**
     * Han characters for which code point order == radical-stroke order.
     * Hand-picked exceptions that are hard to detect optimally
     * (because there are 2 or 3 in a row out of order) are removed here,
     * while other characters are removed automatically.
     */
    private UnicodeSet hanInCPOrder = new UnicodeSet(0x4e00, LAST_UNIHAN_11)
            .remove(0x561f).remove(0x5620)
            .remove(0x7adf).remove(0x7ae0)
            .remove(0x9824).remove(0x9825);
    private UnicodeSet hanNotInCPOrder;

    RadicalStroke(String unicodeVersion) {
        this.unicodeVersion = unicodeVersion;
        UCD ucd = UCD.make(unicodeVersion);
        UnicodeMap<String> rsUnicode = ucd.getHanValue("kRSUnicode");
        IndexUnicodeProperties iup = IndexUnicodeProperties.make(unicodeVersion);
        radToChars = getCJKRadicals(iup);
        ToolUnicodePropertySource propSource = ToolUnicodePropertySource.make(unicodeVersion);
        final UnicodeSet hanSet = propSource.getProperty("Unified_Ideograph").getSet("True");
        final UnicodeSetIterator hanIter = new UnicodeSetIterator(hanSet);
        orderedHan = new long[hanSet.size()];
        int i = 0;
        long prevPrevOrder = 0;
        long prevOrder = 0;
        while (hanIter.next()) {
            int c = hanIter.codepoint;
            assert c >= 0;
            int extension = (0x4E00 <= c && c <= 0xFFFF) ? 0 : 1;  // see UCA implicit weights BASE FB40 vs. FB80
            String rs = rsUnicode.get(c);
            if (rs == null) {
                rs = "999.0";
            }
            // Use only the first radical-stroke value if there are multiple.
            int delim = rs.indexOf(' ');
            if (delim < 0) {
                delim = rs.length();
            }
            int dot = rs.indexOf('.');
            assert 0 <= dot && dot < delim;
            int simplified = 0;
            int radicalNumberLimit = dot;
            if (rs.charAt(radicalNumberLimit - 1) == '\'') {
                simplified = 1;
                --radicalNumberLimit;
            }
            int radicalNumber = parseInt(rs, 0, radicalNumberLimit);
            int residualStrokeCount = parseInt(rs, dot + 1, delim);
            long order =
                    ((long)radicalNumber << 32) |
                    (simplified << 30) |
                    (residualStrokeCount << 24) |
                    (extension << 23) |
                    c;
            if (DEBUG) {
                String radical = rs.substring(0, dot);
                System.out.println("U+" + Utility.hex(c) + " -> " + Long.toHexString(order) +
                        "  rad " + radical +
                        " chars " + radToChars.get(radical) +
                        "  residual " + residualStrokeCount);
            }
            if (extension == 0 && 0x4E00 <= c && c <= LAST_UNIHAN_11) {
                if (!hanInCPOrder.contains(c)) {
                    if (DEBUG) {
                        System.out.println("*** out of order: " +
                                Long.toHexString(prevPrevOrder) + ' ' +
                                Long.toHexString(prevOrder) + " (" +
                                Long.toHexString(order) + ") (manually removed)");
                    }
                } else if (prevPrevOrder <= order && prevOrder > order) {
                    // The previous character sorts higher than the surrounding ones.
                    if (DEBUG) {
                        System.out.println("*** out of order: " +
                                Long.toHexString(prevPrevOrder) + " (" +
                                Long.toHexString(prevOrder) + ") " +
                                Long.toHexString(order));
                    }
                    int prevCodePoint = ((int)prevOrder) & 0x1fffff;
                    hanInCPOrder.remove(prevCodePoint);
                    prevOrder = order;
                } else if (prevOrder > order) {
                    // The current character sorts lower than the previous one.
                    if (DEBUG) {
                        System.out.println("*** out of order: " +
                                Long.toHexString(prevPrevOrder) + ' ' +
                                Long.toHexString(prevOrder) + " (" +
                                Long.toHexString(order) + ')');
                    }
                    hanInCPOrder.remove(c);
                } else {
                    assert prevPrevOrder <= prevOrder && prevOrder <= order;
                    prevPrevOrder = prevOrder;
                    prevOrder = order;
                }
            }
            orderedHan[i++] = order;
        }
        assert i == orderedHan.length;
        Arrays.sort(orderedHan);
        hanInCPOrder.freeze();
        System.out.println("hanInCPOrder = " + hanInCPOrder.toPattern(false));
        int numOutOfOrder = LAST_UNIHAN_11 + 1 - 0x4e00 - hanInCPOrder.size();
        System.out.println("number of original-Unihan characters out of order: " + numOutOfOrder);
        // In Unicode 7.0, there are 313 original-Unihan characters out of order.
        // If this number is much higher, then either the data has changed a lot,
        // or there is a bug in the code.
        // Turn on the DEBUG flag and see if we can manually remove some characters from the set
        // so that a sequence of following ones does not get removed.
        assert numOutOfOrder <= 320;
        hanNotInCPOrder = new UnicodeSet(hanSet).removeAll(hanInCPOrder).freeze();
    }

    public void printRadicalStrokeOrder(Writer writer) throws IOException {
        for (int pos = 0; pos >= 0; pos = printNextRadical(pos, writer)) {}
    }

    /**
     * Prints the next radical and the Han ideographs that sort with it.
     *
     * @param pos The data position for the next radical;
     *        initially use 0, then pass in the return value from the previous call.
     * @return The next radical's position, or -1 if there are none more.
     * @throws IOException 
     */
    private int printNextRadical(int pos, Writer writer) throws IOException {
        if (pos == orderedHan.length) {
            return -1;
        }
        StringBuilder sb = new StringBuilder("[radical ");
        long order = orderedHan[pos];
        int radicalNumberAndSimplified = (int)(order >> 30);
        String radicalString = Integer.toString(radicalNumberAndSimplified >> 2);
        if ((radicalNumberAndSimplified & 1) != 0) {
            radicalString += '\'';
        }
        String radicalChars = radToChars.get(radicalString);
        sb.append(radicalString).append('=').append(radicalChars).append(':');
        int start = 0;
        int prev = 0;
        do {
            int c = (int)order & 0x1fffff;
            if (c != (prev + 1)) {
                // c does not continue a range.
                if (start < prev) {
                    // Finish the previous range.
                    if ((start + 2) <= prev) {  // at least 3 code points
                        sb.append('-');
                    }
                    sb.appendCodePoint(prev);
                }
                sb.appendCodePoint(c);
                start = c;
            }
            prev = c;
            if (++pos == orderedHan.length) {
                pos = -1;
                break;
            }
            order = orderedHan[pos];
        } while ((int)(order >> 30) == radicalNumberAndSimplified);
        if (start < prev) {
            // Finish the last range.
            if ((start + 2) <= prev) {  // at least 3 code points
                sb.append('-');
            }
            sb.appendCodePoint(prev);
        }
        writer.append(sb.append(']').append('\n'));
        if (pos < 0) {
            writer.append("[radical end]\n");
        }
        return pos;
    }

    public void printUnihanIndex(Writer writer) throws IOException {
        writer.append("# Index characters for the unihan sort order in root.\n").
                append("# Each index character is an ideograph representing a radical,\n").
                append("# and sorts like the first ideograph in the radical-stroke order.\n");
        writer.append("# Unicode ").append(unicodeVersion).append('\n');
        StringBuilder sb = new StringBuilder();
        for (int pos = 0;;) {
            long order = orderedHan[pos];
            int c = (int)order & 0x1fffff;  // First code point for the radical.
            int radicalNumberAndSimplified = (int)(order >> 30);
            String radicalString = Integer.toString(radicalNumberAndSimplified >> 2);
            if ((radicalNumberAndSimplified & 1) != 0) {
                radicalString += '\'';
            }
            String radicalChars = radToChars.get(radicalString);
            // All radicals should be BMP characters.
            assert radicalChars.length() == UTF16.countCodePoint(radicalChars);
            // For the representative radical character,
            // use the one at index 1 which is in the original Unihan block
            // which has good font support,
            // rather than the one at index 0 which is in the radicals block.
            sb.replace(0, sb.length(), "&").appendCodePoint(c).
                    append("=\\uFDD0").append(radicalChars.charAt(1)).
                    append(" # radical ").append(radicalString).append('\n');
            writer.append(sb);
            do {
                if (++pos == orderedHan.length) {
                    return;
                }
                order = orderedHan[pos];
            } while ((int)(order >> 30) == radicalNumberAndSimplified);
        }
    }

    /**
     * Returns a set of Han characters for which code point order == radical-stroke order.
     */
    public UnicodeSet getHanInCPOrder() {
        return hanInCPOrder;
    }

    /**
     * Returns a set of Han characters for which code point order != radical-stroke order.
     */
    public UnicodeSet getHanNotInCPOrder() {
        return hanNotInCPOrder;
    }

    private static Map<String, String> getCJKRadicals(IndexUnicodeProperties iup) {
        UnicodeMap<String> cjkr = iup.load(UcdProperty.CJK_Radical);
        // cjkr maps from each radical code point to the radical string.
        // We invert it and return a map from radical to the two code points.
        Map<String, String> map = new TreeMap<String, String>();
        map.put("999", "??");  // dummy entry for characters without RS data
        for (UnicodeMap.EntryRange<String> range : cjkr.entryRanges()) {
            String radicalString = range.value;
            if (radicalString == null) {
                continue;
            }
            int c = range.codepoint;
            assert c >= 0;
            String oldValue = map.get(radicalString);
            if (oldValue == null) {
                assert c < 0x3000;  // should be a radical code point
                map.put(radicalString, Character.toString((char)c));
            } else {
                assert 0x4e00 <= c && c <= LAST_UNIHAN_11;
                int oldCodePoint = oldValue.codePointAt(0);
                assert oldCodePoint < 0x3000;
                assert oldValue.length() == 1;
                map.put(radicalString, oldValue + (char)c);
            }
        }
        return Collections.unmodifiableMap(map);
    }

    /**
     * Parses a small (max 3 digits) integer from a subsequence.
     * Avoids creation of a subsequence object.
     */
    private static int parseInt(String s, int start, int limit) {
        assert start < limit;
        if (s.charAt(start) == '-') {
            // Unicode 9 UAX #38 kDefaultSortKey:
            // If the number of residual strokes is negative, 0 is used instead.
            return 0;
        }
        int result = 0;
        int i = start;
        do {
            int digit = s.charAt(i++) - '0';
            if (digit < 0 || 9 < digit) {
                throw new NumberFormatException(s.substring(start, limit));
            }
            result = result * 10 + digit;
        } while (i < limit);
        return result;
    }
}
