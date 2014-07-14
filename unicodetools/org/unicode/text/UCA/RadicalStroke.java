package org.unicode.text.UCA;

import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.text.UCD.ToolUnicodePropertySource;
import org.unicode.text.UCD.UCD;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

class RadicalStroke {
    RadicalStroke(String unicodeVersion) {
        UCD ucd = UCD.make(unicodeVersion);
        @SuppressWarnings("unchecked")
        UnicodeMap<String> rsUnicode = ucd.getHanValue("kRSUnicode");
        IndexUnicodeProperties iup = IndexUnicodeProperties.make(unicodeVersion);
        UnicodeMap<String> cjkr = iup.load(UcdProperty.CJK_Radical);
        ToolUnicodePropertySource propSource = ToolUnicodePropertySource.make(unicodeVersion);
        final UnicodeSet hanSet = propSource.getProperty("Unified_Ideograph").getSet("True");
        final UnicodeSetIterator hanIter = new UnicodeSetIterator(hanSet);
        long prevOrder = 0;
        while (hanIter.next()) {
            int c = hanIter.codepoint;
            assert c >= 0;
            int extension = (0x4E00 <= c && c <= 0xFFFF) ? 0 : 1;  // see UCA implicit weights BASE FB40 vs. FB80
            String rs = rsUnicode.get(c);
            assert rs != null;
            int delim = rs.indexOf(' ');
            if (delim >= 0) {
                // Use only the first radical-stroke value if there are multiple.
                System.out.println("U+" + Utility.hex(c) + " -> " + rs);
            } else {
                delim = rs.length();
            }
            int dot = rs.indexOf('.');
            assert 0 <= dot && dot < delim;
            String radical = rs.substring(0, dot);
            // TODO: wrong: cjkr maps from each radical code point to the radical string;
            // we need to map from the radical string to the two code points
            // String radicalChars = cjkr.get(radical);
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
            System.out.println("U+" + Utility.hex(c) + " -> " + Long.toHexString(order) +
                    "  rad " + radical +
                    // TODO: wrong " chars " + radicalChars +
                    "  residual " + residualStrokeCount);
            if (extension == 0) {
                // Original Unihan block, starting from the second character.
                if (0x4E01 <= c && c <= 0x9FA5) {
                    if (prevOrder >= order) {
                        System.out.println("*** out of order!");
                    }
                }
                prevOrder = order;
            }
        }
    }

    /**
     * Parses a small (max 3 digits) integer from a subsequence.
     * Avoids creation of a subsequence object.
     */
    private static final int parseInt(String s, int start, int limit) {
        assert start < limit;
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
