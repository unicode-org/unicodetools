package org.unicode.text.UCA;

import java.util.ArrayList;
import java.util.List;

import org.unicode.text.UCD.ToolUnicodePropertySource;
import org.unicode.text.UCD.UCD;
import org.unicode.text.utility.Utility;

import com.ibm.icu.text.UnicodeSet;

/**
 * Code points that do not have explicit mappings in the DUCET
 * are mapped to collation elements with implicit primary weights
 * that sort between regular explicit weights and trailing weights.
 * See http://www.unicode.org/reports/tr10/#Implicit_Weights
 */
public class Implicit {
    /**
     * Start of implicit primaries.
     */
    static final int START = 0xFB00;
    /**
     * First primary weight available for ranges.
     * Intended for siniform ideographic scripts, starting with Tangut in UCA 9.
     */
    static final int RANGES_BASE = 0xFB00;
    /**
     * Base primary weight for the original CJK Unihan block &
     * non-decomposable CJK compatibility characters.
     */
    static final int CJK_BASE = 0xFB40;
    /**
     * Base primary weight for CJK extensions blocks.
     */
    static final int CJK_EXTENSIONS_BASE = 0xFB80;
    /**
     * Base primary weight for unassigned code points.
     * Formally, this is used for characters that have neither explicit DUCET mappings
     * nor implicit mappings according to any other rule.
     */
    static final int UNASSIGNED_BASE = 0xFBC0;
    static final int UNASSIGNED_LIMIT = 0xFC00;
    /**
     * End of implicit primaries, start of trailing ones.
     */
    static final int LIMIT = 0xFC00;

    /**
     * Range of implicit weights for siniform ideographic scripts.
     * Corresponds to allkeys.txt:
     * <pre>
     * @implicitweights 17000..18AFF; FB00 # Tangut and Tangut Components
     * </pre>
     */
    static final class Range {
        final int leadPrimary;
        final int startCP;
        final int endCP;
        /** First assigned code point in the range. */
        final int firstCP;
        final UnicodeSet set;

        private Range(int leadPrimary, int startCP, int endCP, UnicodeSet unassignedSet) {
            this.leadPrimary = leadPrimary;
            this.startCP = startCP;
            this.endCP = endCP;
            set = new UnicodeSet(startCP, endCP).removeAll(unassignedSet).freeze();
            assert !set.isEmpty();
            firstCP = set.charAt(0);
        }
    }

    private final UnicodeSet unassignedSet;
    final UnicodeSet unifiedIdeographSet;
    List<Range> ranges = new ArrayList<Range>();

    Implicit(UCD ucd) {
        String unicodeVersion = ucd.getVersion();
        ToolUnicodePropertySource propSource = ToolUnicodePropertySource.make(unicodeVersion);
        unassignedSet = propSource.getProperty("General_Category").getSet("Cn");
        unifiedIdeographSet = propSource.getProperty("Unified_Ideograph").getSet("True");
        unifiedIdeographSet.freeze();
    }

    /**
     * Adds a range of implicit weights for siniform ideographic scripts.
     * Corresponds to allkeys.txt:
     * <pre>
     * @return 
     * @implicitweights 17000..18AFF; FB00 # Tangut and Tangut Components
     * </pre>
     */
    Range addRange(int leadPrimary, int startCP, int endCP) {
        Range r = new Range(leadPrimary, startCP, endCP, unassignedSet);
        ranges.add(r);
        return r;
    }

    /**
     * @return true if primary is in the range of implicit lead primaries,
     *         although it may not actually be used as one
     */
    static boolean isImplicitLeadPrimary(int primary) {
        return START <= primary && primary < LIMIT;
    }

    /**
     * Returns a pair of implicit primary weights for c.
     * The lead primary is in bits 31..16 (use the >>> operator),
     * the trail primary in bits 15..0.
     */
    int primaryPairForCodePoint(int c) {
        assert 0 <= c && c <= 0x10FFFF;
        if (unifiedIdeographSet.contains(c)) {
            int base;
            if (0x4E00 <= c && c <= 0xFFFF) {
                // Unified_Ideograph=True AND
                // ((Block=CJK_Unified_Ideograph) OR (Block=CJK_Compatibility_Ideographs))
                base = CJK_BASE;
            } else {
                // other Unified_Ideograph
                base = CJK_EXTENSIONS_BASE;
            }
            return ((base + (c >> 15)) << 16) | 0x8000 | (c & 0x7FFF);
        }
        for (Range r : ranges) {
            if (r.set.contains(c)) {
                return (r.leadPrimary << 16) | 0x8000 | (c - r.startCP);
            }
        }
        // unassigned
        return ((UNASSIGNED_BASE + (c >> 15)) << 16) | 0x8000 | (c & 0x7FFF);
    }

    public int codePointForPrimaryPair(int lead, int trail) {
        if (lead < START || trail <= 0x7FFF) {
            // not implicit
        } else if (lead < CJK_BASE) {
            for (Range r : ranges) {
                if (lead == r.leadPrimary) {
                    int c = r.startCP + (trail & 0x7FFF);
                    if (r.set.contains(c)) {
                        return c;
                    }
                    break;  // invalid
                }
            }
        } else if (lead < UNASSIGNED_LIMIT) {
            int c = ((lead & 0x3F) << 15) | (trail & 0x7FFF);
            if (c <= 0x10FFFF) {
                int pair = primaryPairForCodePoint(c);
                if ((pair >>> 16) == lead && (pair & 0xFFFF) == trail) {
                    return c;
                }
            }
        }
        throw new IllegalArgumentException("invalid pair of implicit primaries: " +
                Utility.hex(lead) + ", " + Utility.hex(trail));
    }
}
