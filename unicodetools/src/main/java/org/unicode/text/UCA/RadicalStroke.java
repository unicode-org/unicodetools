package org.unicode.text.UCA;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import org.unicode.cldr.draft.FileUtilities;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.PropertyParsingInfo;
import org.unicode.props.UcdLineParser;
import org.unicode.props.UcdProperty;
import org.unicode.text.UCD.ToolUnicodePropertySource;
import org.unicode.text.utility.Utility;

public final class RadicalStroke {
    private static final int MAX_RADICAL_NUMBER = 214;
    /**
     * The Unicode 1.1 Unihan block was U+4E00..U+9FA5. The ideographs there were allocated in
     * radical-stroke order, but some of the radical-stroke data was changed later.
     */
    private static final int LAST_UNIHAN_11 = 0x9fa5;

    private static final boolean DEBUG = false;

    private static final int SIMPLIFIED_NUM_BITS = 2;

    private String unicodeVersion;
    /** Han character data in code point order. */
    private long[] rawHan;
    /** Han character data in UCA radical-stroke order. */
    private long[] orderedHan;
    /** Maps radicalNumberAndSimplified to the radical character. */
    private String[] radToChar = new String[(MAX_RADICAL_NUMBER + 1) << SIMPLIFIED_NUM_BITS];
    /** Maps radicalNumberAndSimplified to the radical character and its ideograph sibling. */
    private String[] radToChars = new String[(MAX_RADICAL_NUMBER + 1) << SIMPLIFIED_NUM_BITS];
    /** Radical strings. Avoid constructing them over and over. */
    private String[] radicalStrings = new String[(MAX_RADICAL_NUMBER + 1) << SIMPLIFIED_NUM_BITS];
    /**
     * Han characters for which code point order == radical-stroke order. Hand-picked exceptions
     * that are hard to detect optimally (because there are 2 or 3 in a row out of order) are
     * removed here, while other characters are removed automatically.
     */
    private UnicodeSet hanInCPOrder =
            new UnicodeSet(0x4e00, LAST_UNIHAN_11)
                    .remove(0x561f)
                    .remove(0x5620)
                    .remove(0x7adf)
                    .remove(0x7ae0)
                    .remove(0x9824)
                    .remove(0x9825);

    private UnicodeSet hanNotInCPOrder;

    public RadicalStroke(String unicodeVersion) {
        this.unicodeVersion = unicodeVersion;
        IndexUnicodeProperties iup = IndexUnicodeProperties.make(unicodeVersion);
        UnicodeMap<String> rsUnicode = iup.load(UcdProperty.kRSUnicode);
        getCJKRadicals(iup);
        ToolUnicodePropertySource propSource = ToolUnicodePropertySource.make(unicodeVersion);
        final UnicodeSet hanSet = propSource.getProperty("Unified_Ideograph").getSet("True");
        final UnicodeSetIterator hanIter = new UnicodeSetIterator(hanSet);
        rawHan = new long[hanSet.size()];
        int i = 0;
        long prevPrevOrder = 0;
        long prevOrder = 0;
        while (hanIter.next()) {
            int c = hanIter.codepoint;
            assert c >= 0;
            // Create an "order" collation key similar to the one in
            // https://www.unicode.org/reports/tr38/#SortingAlgorithm
            // Sorting Algorithm Used by the Radical-Stroke Charts.
            //
            // extension (1 bit):
            // Sort the original Unihan block first, then all extension blocks.
            // We also include the code point in the "order".
            // No need to determine the extension block number since they sort in code point order.
            // We don't use special values for compatibility ideographs.
            int extension =
                    (0x4E00 <= c && c <= 0xFFFF)
                            ? 0
                            : 1; // see UCA implicit weights BASE FB40 vs. FB80
            String rs = rsUnicode.get(c);
            if (rs == null) {
                // Sort characters with missing radical-stroke data last.
                // Maximum radical numbers, simplified, and residual strokes.
                rs = "214''.63";
            }
            // Use only the first radical-stroke value if there are multiple.
            int delim = rs.indexOf(' '); // value separator in Unihan data files
            if (delim < 0) {
                delim = rs.indexOf('|'); // The new parser rewrites multi-values with a | separator.
                if (delim < 0) {
                    delim = rs.length();
                }
            }
            int dot = rs.indexOf('.');
            assert 0 <= dot && dot < delim;
            // simplified (2 bits):
            // - 0 = traditional form for the radical (for example, 齒)
            // - 1 = Chinese simplified form of the radical (for example, 齿)
            // - 2 = non-Chinese simplified form of the radical (for example, 歯)
            //       [new in Unicode 15.1]
            int simplified = 0;
            int radicalNumberLimit = dot;
            if (rs.charAt(radicalNumberLimit - 1) == '\'') {
                simplified = 1;
                --radicalNumberLimit;
                if (rs.charAt(radicalNumberLimit - 1) == '\'') {
                    simplified = 2;
                    --radicalNumberLimit;
                }
            }
            int radicalNumber = parseInt(rs, 0, radicalNumberLimit);
            int radicalNumberAndSimplified =
                    makeRadicalNumberAndSimplified(radicalNumber, simplified);
            int residualStrokeCount = parseInt(rs, dot + 1, delim);
            long order = makeOrder(radicalNumberAndSimplified, residualStrokeCount, extension, c);
            if (DEBUG) {
                String radical = rs.substring(0, dot);
                System.out.println(
                        "U+"
                                + Utility.hex(c)
                                + " -> "
                                + Long.toHexString(order)
                                + "  rad "
                                + radical
                                + " chars "
                                + radToChars[radicalNumberAndSimplified]
                                + "  residual "
                                + residualStrokeCount);
            }
            if (extension == 0 && 0x4E00 <= c && c <= LAST_UNIHAN_11) {
                if (!hanInCPOrder.contains(c)) {
                    if (DEBUG) {
                        System.out.println(
                                "*** out of order: "
                                        + Long.toHexString(prevPrevOrder)
                                        + ' '
                                        + Long.toHexString(prevOrder)
                                        + " ("
                                        + Long.toHexString(order)
                                        + ") (manually removed)");
                    }
                } else if (prevPrevOrder <= order && prevOrder > order) {
                    // The previous character sorts higher than the surrounding ones.
                    if (DEBUG) {
                        System.out.println(
                                "*** out of order: "
                                        + Long.toHexString(prevPrevOrder)
                                        + " ("
                                        + Long.toHexString(prevOrder)
                                        + ") "
                                        + Long.toHexString(order));
                    }
                    int prevCodePoint = getCodePoint(prevOrder);
                    hanInCPOrder.remove(prevCodePoint);
                    prevOrder = order;
                } else if (prevOrder > order) {
                    // The current character sorts lower than the previous one.
                    if (DEBUG) {
                        System.out.println(
                                "*** out of order: "
                                        + Long.toHexString(prevPrevOrder)
                                        + ' '
                                        + Long.toHexString(prevOrder)
                                        + " ("
                                        + Long.toHexString(order)
                                        + ')');
                    }
                    hanInCPOrder.remove(c);
                } else {
                    assert prevPrevOrder <= prevOrder && prevOrder <= order;
                    prevPrevOrder = prevOrder;
                    prevOrder = order;
                }
            }
            rawHan[i++] = order;
        }
        assert i == rawHan.length;
        orderedHan = rawHan.clone();
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

    private static final int makeRadicalNumberAndSimplified(int radical, int simplified) {
        assert 1 <= radical && radical <= MAX_RADICAL_NUMBER;
        assert 0 <= simplified && simplified <= 3;
        return (radical << SIMPLIFIED_NUM_BITS) | simplified;
    }

    private static final long makeOrder(
            int radicalNumberAndSimplified, int residualStrokeCount, int extension, int c) {
        assert residualStrokeCount <= 255;
        assert extension == 0 || extension == 1;
        return ((long) radicalNumberAndSimplified << 34)
                | (residualStrokeCount << 24)
                | (extension << 23)
                | c;
    }

    private static final int getRadicalNumberAndSimplified(long order) {
        return (int) (order >> 34);
    }

    private static final int getCodePoint(long order) {
        return (int) (order & 0x1fffff);
    }

    public void printRadicalStrokeOrder(Writer writer) throws IOException {
        for (int pos = 0; pos >= 0; pos = printNextRadical(pos, writer)) {}
    }

    /**
     * Prints the next radical and the Han ideographs that sort with it.
     *
     * @param pos The data position for the next radical; initially use 0, then pass in the return
     *     value from the previous call.
     * @return The next radical's position, or -1 if there are none more.
     * @throws IOException
     */
    private int printNextRadical(int pos, Writer writer) throws IOException {
        if (pos == orderedHan.length) {
            return -1;
        }
        StringBuilder sb = new StringBuilder("[radical ");
        long order = orderedHan[pos];
        int radicalNumberAndSimplified = getRadicalNumberAndSimplified(order);
        String radicalChars = radToChars[radicalNumberAndSimplified];
        sb.append(getRadicalStringFromShortData(getShortData(order)))
                .append('=')
                .append(radicalChars)
                .append(':');
        int start = 0;
        int prev = 0;
        do {
            int c = getCodePoint(order);
            if (c != (prev + 1)) {
                // c does not continue a range.
                if (start < prev) {
                    // Finish the previous range.
                    if ((start + 2) <= prev) { // at least 3 code points
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
        } while (getRadicalNumberAndSimplified(order) == radicalNumberAndSimplified);
        if (start < prev) {
            // Finish the last range.
            if ((start + 2) <= prev) { // at least 3 code points
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
        writer.append("# Index characters for the unihan sort order in root.\n")
                .append("# Each index character is an ideograph representing a radical,\n")
                .append("# and sorts like the first ideograph in the radical-stroke order.\n");
        writer.append("# Unicode ").append(unicodeVersion).append('\n');
        StringBuilder sb = new StringBuilder();
        for (int pos = 0; ; ) {
            long order = orderedHan[pos];
            int c = getCodePoint(order); // First code point for the radical.
            int radicalNumberAndSimplified = getRadicalNumberAndSimplified(order);
            // For the representative radical character,
            // use the unified ideograph which is almost always in the original Unihan block
            // which has good font support,
            // rather than the character in the radicals block (if there is such a character).
            int ideograph =
                    getUnifiedIdeographForRadicalNumberAndSimplified(radicalNumberAndSimplified);
            sb.replace(0, sb.length(), "&")
                    .appendCodePoint(c)
                    .append("=\\uFDD0")
                    .appendCodePoint(ideograph)
                    .append(" # radical ")
                    .append(getRadicalStringFromShortData(getShortData(order)))
                    .append('\n');
            writer.append(sb);
            do {
                if (++pos == orderedHan.length) {
                    return;
                }
                order = orderedHan[pos];
            } while (getRadicalNumberAndSimplified(order) == radicalNumberAndSimplified);
        }
    }

    /** Returns a set of Han characters for which code point order == radical-stroke order. */
    public UnicodeSet getHanInCPOrder() {
        return hanInCPOrder;
    }

    /** Returns a set of Han characters for which code point order != radical-stroke order. */
    public UnicodeSet getHanNotInCPOrder() {
        return hanNotInCPOrder;
    }

    /**
     * Returns a long for the UCA order of ideographs, including the code point tie-breaker. Returns
     * 0 for non-ideographs.
     */
    public long getLongOrder(int cp) {
        return getDataForCodePoint(cp);
    }

    /**
     * Returns data in bit sets: 19..12=radicalNumber, 11..10=simplified, 7..0=residualStrokes.
     * Returns 0 for non-ideographs.
     */
    public int getShortDataForCodePoint(int cp) {
        return getShortData(getDataForCodePoint(cp));
    }

    // TODO: Why not always work with long order?
    // If we always get the "short data" from the long order, then these just seem like
    // unnecessarily duplicate APIs.
    private static int getShortData(long order) {
        return (int) (order >> 24);
    }

    private static int getRadicalNumberAndSimplifiedFromShortData(int data) {
        assert data != 0;
        int radicalNumberAndSimplified = data >> 10;
        assert radicalNumberAndSimplified >= makeRadicalNumberAndSimplified(1, 0); // radical >= 1
        return radicalNumberAndSimplified;
    }

    public static int getRadicalNumberFromShortData(int data) {
        assert data != 0;
        return data >> 12;
    }

    public static int getSimplifiedFromShortData(int data) {
        assert data != 0;
        return (data >> 10) & 3;
    }

    public static boolean isSimplifiedFromShortData(int data) {
        assert data != 0;
        return (data & 0xc00) != 0;
    }

    public static int getResidualStrokesFromShortData(int data) {
        assert data != 0;
        return data & 0xff;
    }

    /** Returns the radical character for its number and simplified-ness. */
    public String getRadicalCharFromShortData(int data) {
        int radicalNumberAndSimplified = getRadicalNumberAndSimplifiedFromShortData(data);
        if (radicalNumberAndSimplified >= radToChar.length) {
            return null;
        }
        String s = radToChar[radicalNumberAndSimplified];
        if (s != null) {
            return s;
        }
        // For some radicals there is no character in the radicals block. Return the unified
        // ideograph.
        return radToChars[radicalNumberAndSimplified];
    }

    /** Returns a string like "90" or "90'". */
    public String getRadicalStringFromShortData(int data) {
        int radicalNumberAndSimplified = getRadicalNumberAndSimplifiedFromShortData(data);
        return radicalNumberAndSimplified < radicalStrings.length
                ? radicalStrings[radicalNumberAndSimplified]
                : null;
    }

    private int getUnifiedIdeographForRadicalNumberAndSimplified(int radicalNumberAndSimplified) {
        String radicalChars = radToChars[radicalNumberAndSimplified];
        if (radicalChars == null) {
            int radical = radicalNumberAndSimplified >> SIMPLIFIED_NUM_BITS;
            int simplified = radicalNumberAndSimplified & ((1 << SIMPLIFIED_NUM_BITS) - 1);
            if (radical == MAX_RADICAL_NUMBER && simplified == 2) {
                // Special entry for missing radical-stroke data.
                return '?';
            }
            throw new IllegalArgumentException(
                    "no radToChars for " + radical + "'''".substring(0, simplified));
        }
        int length = radicalChars.length();
        // All radical characters should be BMP characters.
        // Unicode 15.1 exception: 182'' --> U+322C4
        assert length == Character.codePointCount(radicalChars, 0, length)
                || radicalNumberAndSimplified == makeRadicalNumberAndSimplified(182, 2);
        // Also in Unicode 15.1, there are two radicals for which there are no characters in the
        // radicals blocks.
        // In these cases, radicalChars contains only one code point, the unified ideograph.
        // Return the unified ideograph, which is always the last code point.
        return radicalChars.codePointBefore(length);
    }

    private long getDataForCodePoint(int cp) {
        // There is no Arrays.binarySearch(long[], ...) that takes a Comparator.
        int start = 0;
        int limit = rawHan.length;
        while (start < limit) {
            int i = (start + limit) / 2;
            int midCP = getCodePoint(rawHan[i]);
            if (cp < midCP) {
                limit = i;
            } else if (cp > midCP) {
                start = i + 1;
            } else /* == */ {
                return rawHan[i];
            }
        }
        return 0; // not found
    }

    // TODO: Consider moving this into a new class/file CJKRadicals which could also be called from
    // other code
    // that currently uses iup.load(UcdProperty.CJK_Radical) even though that cannot represent all
    // of the data.
    private void getCJKRadicals(IndexUnicodeProperties iup) {
        // Here we don't use
        //   UnicodeMap<String> cjkr = iup.load(UcdProperty.CJK_Radical)
        // because CJKRadicals.txt is a mapping from radical to one or two characters, not the other
        // way around.
        // In particular, starting with Unicode 15.1 some radicals do not have a character in a
        // radicals block,
        // so we cannot represent all of the data via a UnicodeMap, which would require an inversion
        // to
        // a character->radical map.
        // Instead, we parse the file directly.
        String fullFilename =
                PropertyParsingInfo.getFullFileName(UcdProperty.CJK_Radical, iup.getUcdVersion());
        Iterable<String> rawLines = FileUtilities.in("", fullFilename);
        // CJKRadicals.txt
        // # There is one line per CJK radical number. Each line contains three
        // # fields, separated by a semicolon (';'). The first field is the
        // # CJK radical number. The second field is the CJK radical character,
        // # which may be empty if the CJK radical character is not included in
        // # the Kangxi Radicals block or the CJK Radicals Supplement block.
        // # The third field is the CJK unified ideograph.
        // 1; 2F00; 4E00
        // 182''; ; 322C4
        //
        // (Unicode 15.1 is the first version where the second field may be empty
        // and where the third field may be outside of the original Unihan block,
        // and even a supplementary code point. See UTC #174 minutes.)
        UcdLineParser parser = new UcdLineParser(rawLines).withRange(false);
        for (UcdLineParser.UcdLine line : parser) {
            String[] parts = line.getParts();
            String radicalString = parts[0];
            int simplified = 0;
            int radicalNumberLimit = radicalString.length();
            if (radicalString.charAt(radicalNumberLimit - 1) == '\'') {
                simplified = 1;
                --radicalNumberLimit;
                if (radicalString.charAt(radicalNumberLimit - 1) == '\'') {
                    // Unicode 15.1 UAX #38:
                    // Two apostrophes (") after the radical indicates a
                    // non-Chinese simplified version of the given radical.
                    simplified = 2;
                    --radicalNumberLimit;
                }
            }
            int radicalNumber = parseInt(radicalString, 0, radicalNumberLimit);
            int radicalNumberAndSimplified =
                    makeRadicalNumberAndSimplified(radicalNumber, simplified);
            radicalStrings[radicalNumberAndSimplified] = radicalString;

            int radicalChar = -1;
            String radicalCharString = "";
            if (!parts[1].isEmpty()) {
                radicalChar = Integer.parseInt(parts[1], 16);
                assert 0 < radicalChar;
                assert radicalChar < 0x3000; // should be a radical code point
                radToChar[radicalNumberAndSimplified] =
                        radicalCharString = Character.toString((char) radicalChar);
                // radToChar[] remains null if there is no radical character.
            }

            int ideograph = Integer.parseInt(parts[2], 16);
            assert 0x3000 < ideograph;
            String ideographString = new String(Character.toChars(ideograph));
            radToChars[radicalNumberAndSimplified] = radicalCharString + ideographString;
            // radToChars[] contains only one character if there is no radical character.
        }
    }

    /**
     * Parses a small (max 3 digits) integer from a subsequence. Avoids creation of a subsequence
     * object.
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
