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
    private static final int MAX_STROKES = 255;

    /**
     * The Unicode 1.1 Unihan block was U+4E00..U+9FA5. The ideographs there were allocated in
     * radical-stroke order, but some of the radical-stroke data was changed later.
     */
    private static final int LAST_UNIHAN_11 = 0x9fa5;

    private static final boolean DEBUG = false;

    // Bit field shift values (low-order bit numbers) in the combined "order".
    private static final int RADICAL_SHIFT = 36;
    private static final int STROKE_SHIFT = 28;
    private static final int SIMPLIFIED_SHIFT = 24;
    private static final int EXTENSION_SHIFT = 20;
    private static final int MAX_EXTENSION = 0xf;
    private static final int CODE_POINT_MASK = 0xfffff;

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

    private final UnicodeSet simplifiedRadicals = new UnicodeSet();

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
            int extension = getExtension(c);
            String rs = rsUnicode.get(c);
            if (rs == null) {
                // Sort characters with missing radical-stroke data last.
                // Maximum radical numbers, simplified, and residual strokes.
                rs = "214'''.254";
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
            int radicalNumberAndSimplified = parseRadicalNumberAndSimplified(rs, 0, dot);
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
        // TODO: Before changing the sort order to conform to UAX #38, demoting the simplified-ness
        // of radicals to below the number of residual strokes,
        // this successfully asserted numOutOfOrder <= 320.
        // Find out if this is a known issue.
        assert numOutOfOrder <= 1500;
        // Exclude simplifiedRadicals so that WriteConformanceTest omits those.
        // The test data should work with both implicit-han and radical-stroke orders.
        // CLDR 46 changes radical-stroke order to match UAX #38,
        // which intermingles characters with traditional and simplified radicals,
        // different from CLDR 26..45 where
        // simplified radicals strongly sorted after traditional ones.
        hanNotInCPOrder =
                new UnicodeSet(hanSet).removeAll(hanInCPOrder).addAll(simplifiedRadicals).freeze();
    }

    // Triples of (start, end, extension) for coalesced UAX #38 order blocks.
    // Read in order, so ranges can overlap.
    private static final int[] EXTENSION_TRIPLES = {
        // The original Unihan block sorts before extension A.
        // CJK Unified Ideographs block
        0x4E00, 0x9FFF, 0,
        // Compatibility ideographs sort last.
        // CJK Compatibility Ideographs
        0xF900, 0xFAFF, 0xf,
        // CJK Compatibility Ideographs Supplement
        0x2F800, 0x2FA1F, 0xf,
        // Extension I pokes a hole in the following range and sorts between H & J.
        // CJK Unified Ideographs Extension I
        0x2EBF0, 0x2EE5F, 2,
        // Extensions A..H sort after the original Unihan block.
        // CJK Unified Ideographs Extension A, B, C, D, E, F, G, H
        0x3400, 0x323AF, 1,
        // J+ after I.
        // CJK Unified Ideographs Extension J+
        // TODO: This needs adjustments when another extension block is encoded out of letter order.
        // https://www.unicode.org/roadmaps/tip/
        0x323B0, 0x37FFF, 3,
    };

    private static final int getExtension(int c) {
        // https://www.unicode.org/reports/tr38/#SortingAlgorithm
        // at a low level sorts by Block, then by code point.
        // This function determines an extension value so that extension|code point sorts
        // in the same order as UAX #38 block|code point.
        // We simply coalesce adjacent blocks that sort in code point order.
        for (int i = 0; i < EXTENSION_TRIPLES.length; i += 3) {
            int start = EXTENSION_TRIPLES[i];
            int end = EXTENSION_TRIPLES[i + 1];
            if (start <= c && c <= end) {
                return EXTENSION_TRIPLES[i + 2];
            }
        }
        throw new IllegalArgumentException("cannot find extension value for U+" + Utility.hex(c));
    }

    private static final int makeRadicalNumberAndSimplified(int radical, int simplified) {
        assert 1 <= radical && radical <= MAX_RADICAL_NUMBER;
        assert 0 <= simplified && simplified <= 3;
        return (radical << SIMPLIFIED_NUM_BITS) | simplified;
    }

    private static final long makeOrder(
            int radicalNumberAndSimplified, int residualStrokeCount, int extension, int c) {
        assert residualStrokeCount <= MAX_STROKES;
        assert 0 <= extension && extension <= MAX_EXTENSION;
        assert 0 <= c && c <= CODE_POINT_MASK;
        int radical = radicalNumberAndSimplified >> SIMPLIFIED_NUM_BITS;
        int simplified = radicalNumberAndSimplified & 3;
        return ((long) radical << RADICAL_SHIFT)
                | ((long) residualStrokeCount << STROKE_SHIFT)
                | (simplified << SIMPLIFIED_SHIFT)
                | (extension << EXTENSION_SHIFT)
                | c;
    }

    private static final int getCodePoint(long order) {
        return (int) (order & CODE_POINT_MASK);
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
        sb.append(getRadicalString(order)).append('=');
        int radicalNumber = getRadicalNumber(order);
        // Append the radicals and ideographs for each of the traditional and simplified forms.
        for (int i = radicalNumber << SIMPLIFIED_NUM_BITS, limit = i + (1 << SIMPLIFIED_NUM_BITS);
                i < limit;
                ++i) {
            String radicalChars = radToChars[i];
            if (radicalChars != null) {
                sb.append(radicalChars);
            }
        }
        sb.append(':');
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
        } while (getRadicalNumber(order) == radicalNumber);
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
            int radicalNumber = getRadicalNumber(order);
            // For the representative radical character,
            // use the unified ideograph which is almost always in the original Unihan block
            // which has good font support,
            // rather than the character in the radicals block (if there is such a character).
            int ideograph = getUnifiedIdeograph(order);
            sb.replace(0, sb.length(), "&")
                    .appendCodePoint(c)
                    .append("=\\uFDD0")
                    .appendCodePoint(ideograph)
                    .append(" # radical ")
                    .append(getRadicalString(order))
                    .append('\n');
            writer.append(sb);
            do {
                if (++pos == orderedHan.length) {
                    return;
                }
                order = orderedHan[pos];
            } while (getRadicalNumber(order) == radicalNumber);
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
    public long getOrderForCodePoint(int cp) {
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

    public static int getRadicalNumber(long order) {
        assert order != 0;
        return (int) (order >> RADICAL_SHIFT);
    }

    public static boolean isSimplified(long order) {
        assert order != 0;
        return ((order >> SIMPLIFIED_SHIFT) & 3) != 0;
    }

    public static int getResidualStrokes(long order) {
        assert order != 0;
        return (int) (order >> STROKE_SHIFT) & 0xff;
    }

    /**
     * Returns the radical character (traditional form) for its number. Returns the unified
     * ideograph if there is no radical character for this number.
     */
    public String getRadicalChar(long order) {
        int radicalNumber = getRadicalNumber(order);
        if (radicalNumber >= radToChar.length) {
            return null;
        }
        String s = radToChar[radicalNumber];
        if (s != null) {
            return s;
        }
        // For some radicals there is no character in the radicals block.
        // Return the unified ideograph.
        // Since there is no radical character, the ideograph is the only
        // character in this string.
        return radToChars[radicalNumber];
    }

    public String getRadicalString(long order) {
        int radicalNumberAndSimplified = getRadicalNumber(order) << SIMPLIFIED_NUM_BITS;
        return radicalNumberAndSimplified < radicalStrings.length
                ? radicalStrings[radicalNumberAndSimplified]
                : null;
    }

    private int getUnifiedIdeograph(long order) {
        int radicalNumber = getRadicalNumber(order);
        String radicalChars = radToChars[radicalNumber << SIMPLIFIED_NUM_BITS];
        if (radicalChars == null) {
            if (radicalNumber == MAX_RADICAL_NUMBER && getResidualStrokes(order) == 254) {
                // Special entry for missing radical-stroke data.
                return '?';
            }
            throw new IllegalArgumentException("no radToChars for " + radicalNumber);
        }
        int length = radicalChars.length();
        // All radical characters should be BMP characters.
        // Unicode 15.1 exception: 182'' --> U+322C4
        assert length == Character.codePointCount(radicalChars, 0, length);
        // Also in Unicode 15.1, there are two radicals for which there are no characters in the
        // radicals blocks.
        // In these cases, radicalChars contains only one code point, the unified ideograph.
        // Return the unified ideograph, which is always the last code point.
        return radicalChars.codePointBefore(length);
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
            int radicalNumberAndSimplified =
                    parseRadicalNumberAndSimplified(radicalString, 0, radicalString.length());
            radicalStrings[radicalNumberAndSimplified] = radicalString;

            String radicalCharString = "";
            if (!parts[1].isEmpty()) {
                int radicalChar = Integer.parseInt(parts[1], 16);
                assert 0 < radicalChar;
                assert radicalChar < 0x3000; // should be a radical code point
                if ((radicalNumberAndSimplified & 3) != 0) {
                    simplifiedRadicals.add(radicalChar);
                }
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

    private static int parseRadicalNumberAndSimplified(String s, int start, int limit) {
        // simplified (2 bits):
        // https://www.unicode.org/reports/tr38/#SortingAlgorithm
        // Quoted from Unicode 16.0:
        // - 0 = traditional form for the radical (for example, U+9F8D 龍)
        // - 1 = Chinese simplified form of the radical (for example, U+9F99 龙)
        // - 2 = non-Chinese simplified form of the radical (for example, U+7ADC 竜)
        //       [new in Unicode 15.1]
        // - 3 = second non-Chinese simplified form of the radical (for example, U+31DE5 𱷥).
        //       [new in Unicode 16.0]
        int simplified = 0;
        if (s.charAt(limit - 1) == '\'') {
            simplified = 1;
            --limit;
            if (s.charAt(limit - 1) == '\'') {
                simplified = 2;
                --limit;
                if (s.charAt(limit - 1) == '\'') {
                    simplified = 3;
                    --limit;
                }
            }
        }
        int radicalNumber = parseInt(s, start, limit);
        return makeRadicalNumberAndSimplified(radicalNumber, simplified);
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
