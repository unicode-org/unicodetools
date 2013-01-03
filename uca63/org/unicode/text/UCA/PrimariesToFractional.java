package org.unicode.text.UCA;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.unicode.text.UCA.UCA_Statistics.RoBitSet;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.UCD;
import org.unicode.text.UCD.UCD_Types;
import org.unicode.text.utility.Utility;

import com.ibm.icu.impl.Row;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

/**
 * Maps UCA-style primary weights to byte-fractional primaries.
 *
 * @since 2013-jan-02 (mostly pulled out of {@link FractionalUCA})
 */
public final class PrimariesToFractional {
    private static final BitSet MAJOR_SCRIPTS = new BitSet();
    static {
        for (byte i : new Byte[]{
                UCD_Types.ARABIC_SCRIPT,
                UCD_Types.ARMENIAN_SCRIPT,
                UCD_Types.BENGALI_SCRIPT,
                UCD_Types.BOPOMOFO_SCRIPT,
                UCD_Types.CYRILLIC_SCRIPT,
                UCD_Types.DEVANAGARI_SCRIPT,
                UCD_Types.ETHIOPIC_SCRIPT,
                UCD_Types.GEORGIAN_SCRIPT,
                UCD_Types.GREEK_SCRIPT,
                UCD_Types.GUJARATI_SCRIPT,
                UCD_Types.GURMUKHI_SCRIPT,
                UCD_Types.HAN_SCRIPT,
                UCD_Types.HANGUL_SCRIPT,
                UCD_Types.HEBREW_SCRIPT,
                UCD_Types.HIRAGANA_SCRIPT,
                UCD_Types.KANNADA_SCRIPT,
                UCD_Types.KATAKANA_SCRIPT,
                UCD_Types.KHMER_SCRIPT,
                UCD_Types.LAO_SCRIPT,
                UCD_Types.LATIN_SCRIPT,
                UCD_Types.MALAYALAM_SCRIPT,
                UCD_Types.MYANMAR_SCRIPT,
                UCD_Types.ORIYA_SCRIPT,
                UCD_Types.SINHALA_SCRIPT,
                UCD_Types.TAMIL_SCRIPT,
                UCD_Types.TELUGU_SCRIPT,
                UCD_Types.THAANA_SCRIPT,
                UCD_Types.THAI_SCRIPT,
                UCD_Types.TIBETAN_SCRIPT
        }) {
            MAJOR_SCRIPTS.set(i & 0xFF);
        }
    }

    private UCA uca;

    /**
     * The first fractional primary weight for each reorder code.
     */
    private int[] firstFractionalPrimary = new int[ReorderCodes.LIMIT];

    /**
     * Fractional primary weight for numeric sorting (CODAN).
     * Single-byte weight, lead byte for all computed whole-number CEs.
     *
     * <p>This must be a "homeless" weight.
     * If any character or string mapped to a weight with this same lead byte,
     * then we would get an illegal prefix overlap.
     */
    private int numericFractionalPrimary;

    /**
     * Reordering groups with sort-key-compressible fractional primary weights.
     * Only the first script in the group needs to be marked.
     * These are a subset of the {@link #MAJOR_SCRIPTS}.
     *
     * <p>Whether a group is compressible should be determined by whether all of its primaries
     * fit into one lead byte, but we need to know this before assigning
     * fractional primary weights so that we can assign them optimally.
     */
    private boolean[] groupIsCompressible = new boolean[ReorderCodes.LIMIT];

    /**
     * One flag per fractional primary lead byte for whether
     * the fractional weights that start with that byte are sort-key-compressible.
     */
    private BitSet compressibleBytes = new BitSet(256);

    private final Map<Integer, PrimaryToFractional> map = new HashMap<Integer, PrimaryToFractional>();

    /**
     * FractionalUCA properties for a UCA primary weight.
     */
    public static class PrimaryToFractional {
        /**
         * true if this primary is at the start of a reordering group.
         */
        private boolean startsGroup;
        /**
         * The reorder code of the group or script for which this is the first primary.
         * -1 if none.
         */
        private int reorderCodeIfFirst = -1;
        /**
         * The script-first fractional primary inserted before the normal fractional primary.
         * 0 if not the first primary in a script or group.
         * Will be reset to 0 again after first output.
         */
        private int scriptFirstPrimary;

        private boolean useSingleBytePrimary;
        private boolean useTwoBytePrimary;

        private int fractionalPrimary;

        private PrimaryToFractional() {}

        /**
         * Returns the planned number of fractional primary weight bytes.
         */
        private int getFractionalLength() {
            if (useSingleBytePrimary) {
                return 1;
            }
            if (useTwoBytePrimary) {
                return 2;
            }
            return 3;
        }

        /**
         * Returns the reorder code of the group or script for which this is the first primary.
         * -1 if none.
         */
        public int getReorderCodeIfFirst() {
            return reorderCodeIfFirst;
        }

        /**
         * Returns the script-first fractional primary that precedes this UCA primary's
         * own fractional primary, if this is the first primary of a group or script,
         * otherwise returns 0.
         * The script-first primary is reset, so that the next call with the same UCA primary returns 0.
         */
        public int getAndResetScriptFirstPrimary() {
            int firstFractional = scriptFirstPrimary;
            scriptFirstPrimary = 0;
            return firstFractional;
        }

        public boolean isFirstInReorderingGroup() {
            return startsGroup;
        }

        /**
         * Returns the fractional primary weight for the UCA primary.
         */
        public int getFractionalPrimary() {
            return fractionalPrimary;
        }
    }

    /**
     * Computes valid FractionalUCA primary weights of desired byte lengths.
     * Always starts with the first primary weight after 02.
     * {@link PrimaryWeight#next(int, boolean)} increments
     * one 1/2/3-byte weight to another 1/2/3-byte weight.
     */
    private static class PrimaryWeight {
        /**
         * For most bytes except a weight's lead byte, 02 is ok.
         * It just needs to be greater than the level separator 01.
         */
        private static final int MIN_BYTE = 2;

        private static final int MIN2_UNCOMPRESSED = MIN_BYTE;
        private static final int MAX2_UNCOMPRESSED = 0xff;

        /**
         * Primary compression for sort keys uses bytes 03 and FF as compression terminators.
         * The low terminator must be greater than the end-of-merged-string separator 02.
         */
        private static final int MIN2_COMPRESSED = MIN_BYTE + 2;
        private static final int MAX2_COMPRESSED = 0xfe;

        /**
         * Increment byte2 a little more at script boundaries
         * and around single-byte primaries,
         * for tailoring of at least 4 two-byte primaries or more than 1000 three-byte primaries.
         */
        private static final int GAP2_PLUS = 4;

        private static final int GAP3 = 7;

        private int minByte2 = MIN2_UNCOMPRESSED;
        private int maxByte2 = MAX2_UNCOMPRESSED;

        // We start the first reordering group at byte1 = 3.
        // The simplest is to initialize byte1 = 1 so that the tailoring gap naturally
        // gives us what we want.
        private int byte1 = 1;
        private int byte2;
        private int byte3;
        private int lastByteLength = 1;
        private boolean compressibleLeadByte;

        public int getIntValue() {
            return (byte1 << 16) + (byte2 << 8) + byte3;
        }

        public int startNewGroup(int newByteLength, boolean compress) {
            int oByte1 = byte1;
            int oByte2 = byte2;

            int inc1;
            if (lastByteLength == 1) {
                // Single-byte gap of 1 from a single-byte weight to the new reordering group.
                inc1 = 2;
            } else if ((byte2 + GAP2_PLUS) <= maxByte2) {
                // End-of-script two-byte-weight gap.
                inc1 = 1;
            } else {
                // The two-byte-weight gap would be too small.
                inc1 = 2;
            }
            addTo1(inc1);

            int newMinByte2 = compress ? MIN2_COMPRESSED : MIN2_UNCOMPRESSED;
            switch (newByteLength) {
            case 1:
                byte2 = byte3 = 0;
                break;
            case 2:
                byte2 = newMinByte2;
                byte3 = 0;
                break;
            case 3:
                byte2 = newMinByte2;
                byte3 = MIN_BYTE;
                break;
            }

            check(oByte1, oByte2, newByteLength, true);

            compressibleLeadByte = compress;
            minByte2 = newMinByte2;
            maxByte2 = compressibleLeadByte ? MAX2_COMPRESSED : MAX2_UNCOMPRESSED;;
            lastByteLength = newByteLength;
            return getIntValue();
        }

        public int next(int newByteLength, boolean scriptChange) {
            int oByte1 = byte1;
            int oByte2 = byte2;

            switch (lastByteLength) {
            case 1:
                switch (newByteLength) {
                case 1:
                    // Gap of 1 lead byte between singles.
                    addTo1(2);
                    break;
                case 2:
                    // Larger two-byte gap after a single.
                    addTo1(1);
                    byte2 = minByte2 + GAP2_PLUS;
                    break;
                case 3:
                    // Larger two-byte gap after a single.
                    addTo1(1);
                    byte2 = minByte2 + GAP2_PLUS;
                    byte3 = MIN_BYTE;
                    break;
                }
                break;
            case 2:
                switch (newByteLength) {
                case 1:
                    // At least a larger two-byte gap before a single.
                    addTo1((byte2 + GAP2_PLUS) <= maxByte2 ? 1 : 2);
                    byte2 = 0;
                    break;
                case 2:
                    // Normal two-byte gap.
                    addTo2(scriptChange ? GAP2_PLUS + 1: 2);
                    break;
                case 3:
                    // At least a two-byte gap after a double.
                    addTo2(scriptChange ? GAP2_PLUS + 1: 2);
                    byte3 = MIN_BYTE;
                    break;
                }
                break;
            case 3:
                switch (newByteLength) {
                case 1:
                    // At least a larger two-byte gap before a single.
                    addTo1((byte2 + GAP2_PLUS) <= maxByte2 ? 1 : 2);
                    byte2 = byte3 = 0;
                    break;
                case 2:
                    // At least a two-byte gap before a double.
                    addTo2(scriptChange ? GAP2_PLUS + 1: 2);
                    byte3 = 0;
                    break;
                case 3:
                    if (scriptChange) {
                        // TODO: At least a small two-byte gap between minor scripts.
                        //       Issue: At least one of the miscellaneous-scripts reordering groups
                        //       overflows with this.
                        // addTo2(2);
                        // byte3 = MIN_BYTE;

                        // TODO: The following is a smaller gap in the meantime.
                        // At least a large three-byte gap between minor scripts.
                        addTo3(40 + 1);
                    } else {
                        // Normal three-byte gap.
                        addTo3(GAP3 + 1);
                    }
                    break;
                }
                break;
            }

            check(oByte1, oByte2, newByteLength, false);

            lastByteLength = newByteLength;
            return getIntValue();
        }

        /**
         * Checks that we made a good transition.
         */
        private void check(int oByte1, int oByte2, int newByteLength, boolean newFirstByte) {
            // verify results
            // right bytes are filled in, as requested
            switch (newByteLength) {
            case 1:
                assertTrue(byte1 != 0 && byte2 == 0 &&  byte3 == 0);
                break;
            case 2:
                assertTrue(byte1 != 0 && byte2 != 0 &&  byte3 == 0);
                break;
            case 3:
                assertTrue(byte1 != 0 && byte2 != 0 &&  byte3 != 0);
                break;
            }

            // neither is prefix of the other
            if (lastByteLength != newByteLength) {
                int minLength = lastByteLength < newByteLength ? lastByteLength : newByteLength;
                switch (minLength) {
                case 1:
                    assertTrue(byte1 != oByte1);
                    break;
                case 2:
                    assertTrue(byte1 != oByte1 || byte2 != oByte2);
                    break;
                }
            }

            if (newFirstByte) {
                assertTrue(byte1 != oByte1);
            } else {
                // Do not leave a compressible lead byte
                // without starting a new reordering group.
                // If there are too many primaries, then we need to
                // either turn more of them into three-byters
                // or split their group or make it not compressible.
                assertTrue(!compressibleLeadByte || byte1 == oByte1);
            }
        }

        private void assertTrue(boolean b) {
            if (!b) {
                throw new IllegalArgumentException();
            }
        }

        private void addTo3(int increment) {
            byte3 += increment;
            if (byte3 > 0xff) {
                byte3 = MIN_BYTE + (byte3 - 0x100);
                addTo2(1);
            }
        }

        private void addTo2(int increment) {
            byte2 += increment;
            if (byte2 > maxByte2) {
                byte2 = minByte2 + (byte2 - (maxByte2 + 1));
                addTo1(1);
            }
        }

        private void addTo1(int increment) {
            byte1 += increment;
        }

        public String toString() {
            return Fractional.hexBytes(getIntValue());
        }
    }

    /**
     * This constructor just performs basic initialization.
     * You must call {@link PrimariesToFractional#assignFractionalPrimaries(StringBuilder)}
     * to build the data for the rest of the API.
     */
    public PrimariesToFractional(UCA uca) {
        this.uca = uca;
        
        groupIsCompressible[UCD_Types.GREEK_SCRIPT] = true;
        groupIsCompressible[UCD_Types.CYRILLIC_SCRIPT] = true;
        groupIsCompressible[UCD_Types.ARABIC_SCRIPT] = true;
        groupIsCompressible[UCD_Types.GEORGIAN_SCRIPT] = true;
        groupIsCompressible[UCD_Types.ARMENIAN_SCRIPT] = true;
        groupIsCompressible[UCD_Types.HEBREW_SCRIPT] = true;
        groupIsCompressible[UCD_Types.THAANA_SCRIPT] = true;
        groupIsCompressible[UCD_Types.ETHIOPIC_SCRIPT] = true;
        groupIsCompressible[UCD_Types.DEVANAGARI_SCRIPT] = true;
        groupIsCompressible[UCD_Types.BENGALI_SCRIPT] = true;
        groupIsCompressible[UCD_Types.GURMUKHI_SCRIPT] = true;
        groupIsCompressible[UCD_Types.GUJARATI_SCRIPT] = true;
        groupIsCompressible[UCD_Types.ORIYA_SCRIPT] = true;
        groupIsCompressible[UCD_Types.TAMIL_SCRIPT] = true;
        groupIsCompressible[UCD_Types.TELUGU_SCRIPT] = true;
        groupIsCompressible[UCD_Types.KANNADA_SCRIPT] = true;
        groupIsCompressible[UCD_Types.MALAYALAM_SCRIPT] = true;
        groupIsCompressible[UCD_Types.SINHALA_SCRIPT] = true;
        groupIsCompressible[UCD_Types.THAI_SCRIPT] = true;
        groupIsCompressible[UCD_Types.LAO_SCRIPT] = true;
        groupIsCompressible[UCD_Types.TIBETAN_SCRIPT] = true;
        groupIsCompressible[UCD_Types.MYANMAR_SCRIPT] = true;
        groupIsCompressible[UCD_Types.KHMER_SCRIPT] = true;
        groupIsCompressible[UCD_Types.HANGUL_SCRIPT] = true;
        groupIsCompressible[UCD_Types.HIRAGANA_SCRIPT] = true;
        groupIsCompressible[UCD_Types.BOPOMOFO_SCRIPT] = true;
    }

    /**
     * Loads the set of UCA primary weights, finds reordering group boundaries,
     * assigns a fractional primary weight for every UCA primary,
     * and writes the [top_byte] information.
     */
    public PrimariesToFractional assignFractionalPrimaries(StringBuilder topByteInfo) {
        System.out.println("Finding Bumps");        
        findBumps();

        System.out.println("Fixing Primaries");
        RoBitSet primarySet = uca.getStatistics().getPrimarySet();        

        PrimaryWeight fractionalPrimary = new PrimaryWeight();

        // Map UCA 0 to fractional 0.
        getOrCreateProps(0);

        topByteInfo.append("[top_byte\t00\tTERMINATOR ]\n");
        topByteInfo.append("[top_byte\t01\tLEVEL-SEPARATOR ]\n");
        topByteInfo.append("[top_byte\t02\tFIELD-SEPARATOR ]\n");

        StringBuilder groupInfo = new StringBuilder();
        int previousGroupLeadByte = 3;
        boolean previousGroupIsCompressible = false;
        int numPrimaries = 0;

        // start at 1 so zero stays zero.
        for (int primary = primarySet.nextSetBit(1);
                0 <= primary && primary < UCA_Types.UNSUPPORTED_BASE;
                primary = primarySet.nextSetBit(primary+1)) {

            // we know that we need a primary weight for this item.
            // we base it on the last value, and whether we have a 1, 2, or 3-byte weight
            // if we change lengths, then we have to change the initial segment
            // if 'bumps' are set, then we change the first byte

            String old = fractionalPrimary.toString();

            PrimaryToFractional props = getOrCreateProps(primary);
            int currentByteLength = props.getFractionalLength();
            boolean scriptChange = false;

            int reorderCode = props.reorderCodeIfFirst;
            if (reorderCode >= 0) {
                int firstFractional;
                if (props.startsGroup) {
                    boolean compress = groupIsCompressible[reorderCode];
                    firstFractional = fractionalPrimary.startNewGroup(3, compress);
                    int leadByte = Fractional.getLeadByte(firstFractional);

                    // Finish the previous reordering group.
                    appendTopByteInfo(topByteInfo, previousGroupIsCompressible,
                            previousGroupLeadByte, leadByte, groupInfo, numPrimaries);
                    previousGroupLeadByte = leadByte;
                    previousGroupIsCompressible = compress;
                    numPrimaries = 0;
                    groupInfo.setLength(0);
                    groupInfo.append(ReorderCodes.getShortName(reorderCode));
                    if (reorderCode == UCD_Types.HIRAGANA_SCRIPT) {
                        groupInfo.append(" Hrkt Kana");  // script aliases
                    }

                    // Now record the new group.
                    String groupComment = " starts reordering group";
                    if (compress) {
                        compressibleBytes.set(leadByte);
                        groupComment = groupComment + " (compressible)";
                    }
                    System.out.println(String.format(
                            "[%s]  # %s first primary%s",
                            Fractional.hexBytes(firstFractional),
                            ReorderCodes.getName(reorderCode),
                            groupComment));

                    if (reorderCode == ReorderCodes.DIGIT) {
                        numericFractionalPrimary = fractionalPrimary.next(1, true);
                        ++numPrimaries;
                    }
                } else {
                    // New script in current reordering group.
                    firstFractional = fractionalPrimary.next(3, true);
                    if (groupInfo.length() != 0) {
                        groupInfo.append(' ');
                    }
                    groupInfo.append(ReorderCodes.getShortName(reorderCode));
                    if (reorderCode == UCD_Types.Meroitic_Cursive) {
                        groupInfo.append(" Mero");  // script aliases
                    }
                    System.out.println(String.format(
                            "[%s]  # %s first primary",
                            Fractional.hexBytes(firstFractional),
                            ReorderCodes.getName(reorderCode)));
                }
                props.scriptFirstPrimary = firstFractional;
                firstFractionalPrimary[reorderCode] = firstFractional;
                ++numPrimaries;
                scriptChange = true;
            }

            if (currentByteLength == 3 && fractionalPrimary.lastByteLength <= 2) {
                // We slightly optimize the assignment of primary weights:
                // If a 3-byte primary is surrounded by one-or-two-byte primaries,
                // then we can shorten the middle one to two bytes as well,
                // because we would generate at least a two-byte gap before and after anyway.
                //
                // We lose a little tailoring space for further 3-byte weights
                // with the same two-byte prefix, but the sort key will be shorter,
                // and if the CE has non-common secondary or tertiary weights,
                // then it will take less space in the binary data.
                //
                // This requires a lookahead to the desired length of the next primary weight.
                // (But not to the next weight itself which we have not computed yet.)
                //
                // We do not want to auto-shorten the first-script primary weights
                // because they are essentially never used,
                // and we know they fit into long-primary CEs.
                // (They are generated with the next() call above,
                // so we need not handle them specially here.)
                int nextPrimary = primarySet.nextSetBit(primary + 1);
                PrimaryToFractional nextProps = getProps(nextPrimary);
                if (0 <= nextPrimary && nextPrimary < UCA_Types.UNSUPPORTED_BASE &&
                        nextProps != null &&
                        nextProps.reorderCodeIfFirst < 0 &&
                        nextProps.getFractionalLength() <= 2) {
                    currentByteLength = 2;
                }
            }
            props.fractionalPrimary = fractionalPrimary.next(currentByteLength, scriptChange);
            ++numPrimaries;

            String newWeight = fractionalPrimary.toString();
            final boolean DEBUG_FW = false;
            if (DEBUG_FW) {
                System.out.println(
                        currentByteLength
                        + ", " + old
                        + " => " + newWeight
                        + "\t" + Utility.hex(Character.codePointAt(
                                uca.getRepresentativePrimary(primary),0)));
            }
        }

        // Create an entry for the first primary in the Hani script.
        int firstFractional = fractionalPrimary.startNewGroup(3, false);
        int leadByte = Fractional.getLeadByte(firstFractional);

        // Finish the previous reordering group.
        appendTopByteInfo(topByteInfo, previousGroupIsCompressible,
                previousGroupLeadByte, leadByte, groupInfo, numPrimaries);
        previousGroupLeadByte = leadByte;

        // Record Hani.
        PrimaryToFractional props = getOrCreateProps(UCA_Types.UNSUPPORTED_CJK_BASE);
        props.startsGroup = true;
        props.reorderCodeIfFirst = UCD_Types.HAN_SCRIPT;
        props.scriptFirstPrimary = firstFractional;
        firstFractionalPrimary[UCD_Types.HAN_SCRIPT] = firstFractional;
        System.out.println(String.format(
                "[%s]  # %s first primary%s",
                Fractional.hexBytes(firstFractional),
                ReorderCodes.getName(UCD_Types.HAN_SCRIPT),
                " starts reordering group"));

        leadByte = Fractional.IMPLICIT_BASE_BYTE;
        props.fractionalPrimary = leadByte << 16;

        appendTopByteInfo(topByteInfo, false, previousGroupLeadByte, leadByte, "Hani Hans Hant", 0);
        previousGroupLeadByte = leadByte;

        // Record the remaining fixed ranges.
        leadByte = Fractional.IMPLICIT_MAX_BYTE + 1;
        appendTopByteInfo(topByteInfo, false, previousGroupLeadByte, leadByte, "IMPLICIT", 0);
        previousGroupLeadByte = leadByte;

        // Map reserved high UCA primary weights in the trailing-primary range.
        getOrCreateProps(0xfffd).fractionalPrimary = 0xeffd;
        getOrCreateProps(0xfffe).fractionalPrimary = 0xeffe;
        getOrCreateProps(0xffff).fractionalPrimary = 0xefff;

        leadByte = Fractional.SPECIAL_BASE;
        appendTopByteInfo(topByteInfo, false, previousGroupLeadByte, leadByte, "TRAILING", 0);

        appendTopByteInfo(topByteInfo, false, leadByte, 0x100, "SPECIAL", 0);
        return this;
    }

    private static void appendTopByteInfo(StringBuilder topByteInfo, boolean compress,
            int b, int limit, CharSequence groupInfo, int count) {
        boolean canCompress = (limit - b) == 1;
        if (compress) {
            if (!canCompress) {
                throw new IllegalArgumentException(
                        "reordering group {" + groupInfo +
                        "} marked for compression but uses more than one lead byte " +
                        Utility.hex(b, 2) + ".." +
                        Utility.hex(limit, 2));
            }
        } else if (canCompress) {
            System.out.println(
                    "# Note: reordering group {" + groupInfo + "} " +
                    "not marked for compression but uses only one lead byte " +
                    Utility.hex(b, 2));
        }

        while (b < limit) {
            topByteInfo.append("[top_byte\t").
                    append(Utility.hex(b, 2)).
                    append("\t").append(groupInfo);
            if (compress) {
                topByteInfo.append("\tCOMPRESS");
            }
            topByteInfo.append(" ]");
            if (count != 0) {
                // Write the number of primaries on the group's first line.
                topByteInfo.append("  # ").append(count).append(" primary weights");
                count = 0;
            }
            topByteInfo.append('\n');
            ++b;
        }
    }

    private static boolean isHanUCAPrimary(int ucaPrimary) {
        return UCA_Types.UNSUPPORTED_CJK_BASE <= ucaPrimary &&
                ucaPrimary < UCA_Types.UNSUPPORTED_OTHER_BASE;
    }

    /**
     * Returns the properties for the UCA primary weight.
     * Returns null if there are no data for this primary.
     */
    public PrimaryToFractional getProps(int ucaPrimary) {
        return map.get(ucaPrimary);
    }

    /**
     * Same as {@link #getProps(int)} but returns the same object for all
     * Han UCA primary lead weights.
     */
    public PrimaryToFractional getPropsPinHan(int ucaPrimary) {
        if (isHanUCAPrimary(ucaPrimary)) {
            ucaPrimary = UCA_Types.UNSUPPORTED_CJK_BASE;
        }
        return map.get(ucaPrimary);
    }

    /**
     * Returns the properties for the UCA primary weight.
     * Creates and caches a new one if there are no data for this primary yet.
     */
    public PrimaryToFractional getOrCreateProps(int ucaPrimary) {
        PrimaryToFractional props = map.get(ucaPrimary);
        if (props == null) {
            map.put(ucaPrimary, props = new PrimaryToFractional());
        }
        return props;
    }

    public boolean isCompressibleLeadByte(int leadByte) {
        return compressibleBytes.get(leadByte);
    }

    public boolean isCompressibleFractionalPrimary(int primary) {
        return isCompressibleLeadByte(Fractional.getLeadByte(primary));
    }

    public int getFirstFractionalPrimary(int reorderCode) {
        return firstFractionalPrimary[reorderCode];
    }

    public int getNumericFractionalPrimary() {
        return numericFractionalPrimary;
    }

    /**
     * Analyzes the UCA primary weights.
     * <ul>
     * <li>Determines the lengths of the corresponding fractional weights.
     * <li>Sets a flag for the first UCA primary in each reordering group.
     * </ul>
     */
    private void findBumps() {
        int[] groupChar = new int[ReorderCodes.LIMIT];

        BitSet threeByteSymbolPrimaries = new BitSet();
        UnicodeSet threeByteChars = new UnicodeSet();


        RoBitSet primarySet = uca.getStatistics().getPrimarySet();
        int firstScriptPrimary = uca.getStatistics().firstScript;

        // First UCA primary weight per reordering group.
        int[] groupFirstPrimary = new int[ReorderCodes.LIMIT];

        for (int primary = primarySet.nextSetBit(0); primary >= 0; primary = primarySet.nextSetBit(primary+1)) {
            CharSequence ch2 = uca.getRepresentativePrimary(primary);
            int ch = Character.codePointAt(ch2,0);
            byte cat = Fractional.getFixedCategory(ch);
            byte script = Fractional.getFixedScript(ch);

            // see if we have an "infrequent" character: make it a 3 byte if so.
            // also collect data on primaries

            if (primary < firstScriptPrimary) {
                int reorderCode = ReorderCodes.getSpecialReorderCode(ch);
                if (primary > 0) {
                    if (groupFirstPrimary[reorderCode] == 0 || primary < groupFirstPrimary[reorderCode]) {
                        groupFirstPrimary[reorderCode] = primary;
                        groupChar[script] = ch;
                    }
                }
                if (ch < 0xFF || reorderCode == ReorderCodes.SPACE) {
                    // do nothing, assume Latin 1 is "frequent"
                } else if (cat == UCD_Types.OTHER_SYMBOL ||
                        cat == UCD_Types.MATH_SYMBOL ||
                        cat == UCD_Types.MODIFIER_SYMBOL) {
                    // Note: We do not test reorderCode == ReorderCodes.SYMBOL
                    // because that includes Lm etc.
                    threeByteSymbolPrimaries.set(primary);
                    threeByteChars.addAll(ch2.toString());
                } else {
                    if (script != UCD_Types.COMMON_SCRIPT &&
                            script != UCD_Types.INHERITED_SCRIPT &&
                            !MAJOR_SCRIPTS.get(script)) {
                        threeByteSymbolPrimaries.set(primary);
                        threeByteChars.addAll(ch2.toString());
                    }
                }
                continue;
            }

            if (script == UCD_Types.COMMON_SCRIPT || script == UCD_Types.INHERITED_SCRIPT || script == UCD_Types.Unknown_Script) {
                continue;
            }
            // Script aliases: Make sure we get only one script boundary.
            if (script == UCD_Types.Meroitic_Hieroglyphs) {
                script = UCD_Types.Meroitic_Cursive;
            } else if (script == UCD_Types.KATAKANA_SCRIPT) {
                script = UCD_Types.HIRAGANA_SCRIPT;
            }
            boolean TESTING = false;
            if (TESTING && script == UCD_Types.SYRIAC_SCRIPT) {
                System.out.println(Default.ucd().getName(ch));
            }

            // get least primary for script
            if (groupFirstPrimary[script] == 0 || groupFirstPrimary[script] > primary) {
                if (cat <= UCD_Types.OTHER_LETTER && cat != UCD_Types.Lm) {
                    groupFirstPrimary[script] = primary;
                    groupChar[script] = ch;
                    if (TESTING && script == UCD_Types.GREEK_SCRIPT) {
                        System.out.println("*" + Utility.hex(primary) + Default.ucd().getName(ch));
                    }
                }
            }
        }

        System.out.println("3-byte primaries" + threeByteChars.toPattern(false));

        // capture in order the ranges that are major vs minor
        TreeMap<Integer, Row.R2<Boolean, Integer>> majorPrimary =
                new TreeMap<Integer, Row.R2<Boolean, Integer>>();

        // set bumps
        for (int reorderCode = 0; reorderCode < ReorderCodes.LIMIT; ++reorderCode) {
            int primary = groupFirstPrimary[reorderCode];
            if (primary > 0) {
                boolean isMajor = reorderCode >= ReorderCodes.FIRST || MAJOR_SCRIPTS.get(reorderCode);
                majorPrimary.put(primary, Row.of(isMajor, reorderCode));
                PrimaryToFractional props = getOrCreateProps(primary);
                props.startsGroup = isMajor;
                props.reorderCodeIfFirst = reorderCode;
                if (isMajor) {
                    System.out.println("Bumps:\t" + Utility.hex(primary) + " "
                            + ReorderCodes.getName(reorderCode) + " "
                            + Utility.hex(groupChar[reorderCode]) + " "
                            + Default.ucd().getName(groupChar[reorderCode]));
                }
            }
        }

        // now add ranges of primaries that are major, for selecting 2 byte vs 3 byte forms.
        int lastPrimary = 1;
        boolean lastMajor = true;
        int lastScript = UCD_Types.COMMON_SCRIPT;
        for (Entry<Integer, Row.R2<Boolean, Integer>> majorPrimaryEntry : majorPrimary.entrySet()) {
            int primary = majorPrimaryEntry.getKey();
            boolean major = majorPrimaryEntry.getValue().get0();
            int script = majorPrimaryEntry.getValue().get1();
            addMajorPrimaries(lastPrimary, primary-1, lastMajor, lastScript);
            lastPrimary = primary;
            lastMajor = major;
            lastScript = script;
        }
        int veryLastUCAPrimary = primarySet.size() - 1;
        addMajorPrimaries(lastPrimary, veryLastUCAPrimary, lastMajor, lastScript);
        for (int i = threeByteSymbolPrimaries.nextSetBit(0);
                i >= 0;
                i = threeByteSymbolPrimaries.nextSetBit(i + 1)) {
            PrimaryToFractional props = getProps(i);
            if (props != null) {
                props.useTwoBytePrimary = false;
            }
        }

        char[][] singlePairs = {
            {'a','z'}, {' ', ' '},
            {'0', '9'}, {'.', '.'},  {',', ','}
        };
        for (int j = 0; j < singlePairs.length; ++j) {
            char start = singlePairs[j][0];
            char end = singlePairs[j][1];
            for (char k = start; k <= end; ++k) {
                setSingleBytePrimaryFor(k);
            }
        }

        UnicodeSet twoByteChars = new UnicodeSet(
                "[" +
                // Cyrillic main exemplar characters from CLDR 22,
                // for common locales plus Mongolian.
                // TODO: We could make this dynamic, using CLDR's tools to fetch this data.
                // TODO: Consider adding Cyrillic auxiliary exemplar characters.
                "\u0430-\u044F\u0451-\u045C\u045E-\u045F\u0491\u0493\u0495\u049B\u049D" +
                "\u04A3\u04A5\u04AF\u04B1\u04B3\u04B7\u04B9\u04BB\u04CA" +
                "\u04D5\u04D9\u04E3\u04E9\u04EF" +
                // Arabic main exemplar characters from CLDR 22,
                // except for primary ignorable characters.
                "\u0621-\u063A\u0641-\u064A\u066E\u0672\u0679\u067C\u067E" +
                "\u0681\u0685\u0686\u0688\u0689\u0691\u0693\u0696\u0698\u069A" +
                "\u06A9\u06AB\u06AF\u06BA\u06BC\u06BE\u06C1\u06C2\u06C4\u06C7\u06C9\u06CC\u06CD" +
                "\u06D0\u06D2" +
                // Jamo L, V, T
                "\u1100-\u1112\u1161-\u1175\u11A8-\u11C2" +
                "]");
        UnicodeSetIterator twoByteIter = new UnicodeSetIterator(twoByteChars);
        while (twoByteIter.next()) {
            setTwoBytePrimaryFor(twoByteIter.codepoint);
        }
    }

    private static boolean isThreeByteMajorScript(int script) {
        // Some scripts are "major" (and start reordering groups)
        // but have too many primaries for two bytes with a single lead byte.
        // If they are uncased, that is, they have mostly common secondary/tertiary weights,
        // then they lend themselves to using 3-byte primaries because
        // their CEs can be stored compactly as long-primary CEs,
        // and the then-possible primary sort key compression makes sort keys hardly longer.
        return
                // We cherry-pick the main Cyrillic letters for two-byte primaries.
                script == UCD_Types.CYRILLIC_SCRIPT ||
                // We cherry-pick the main Arabic letters for two-byte primaries.
                script == UCD_Types.ARABIC_SCRIPT ||
                // We cherry-pick the conjoining Jamo L/V/T for two-byte primaries.
                script == UCD_Types.HANGUL_SCRIPT ||
                script == UCD_Types.ETHIOPIC_SCRIPT ||
                script == UCD_Types.MYANMAR_SCRIPT;
    }

    private static boolean isTwoByteMinorScript(int script) {
        // Coptic is not a "major" script,
        // but it fits into the Greek lead byte even with 2-byte primaries.
        // This is desirable because Coptic is a cased script,
        // and the CEs for the uppercase characters cannot be stored as "long primary" CEs.
        // (They would have to use less efficient storage.)
        //
        // Note: We could also do this for Deseret:
        // It is also cased and has relatively few primaries,
        // but making them two-byte primaries would take up too much space in its reordering group
        // and would push the group to two lead bytes and to not being compressible any more.
        // Not worth it.
        // At least *lowercase* Deseret sorts in code point order
        // and can therefore be stored as a compact range.
        return
                script == UCD_Types.COPTIC;
    }

    private void addMajorPrimaries(int startPrimary, int endPrimary, boolean isMajor, int script) {
        if (isMajor ? !isThreeByteMajorScript(script) : isTwoByteMinorScript(script)) {
            for (int p = startPrimary; p <= endPrimary; ++p) {
                getOrCreateProps(p).useTwoBytePrimary = true;
            }
        }
        System.out.println("Major:\t" + isMajor + "\t" + UCD.getScriptID_fromIndex((byte)script)
                + "\t" + Utility.hex(startPrimary) + ".." + Utility.hex(endPrimary));
    }

    private void setSingleBytePrimaryFor(char ch) {
        CEList ces = uca.getCEList(String.valueOf(ch), true);
        int firstPrimary = CEList.getPrimary(ces.at(0));
        getOrCreateProps(firstPrimary).useSingleBytePrimary = true;
    }

    private void setTwoBytePrimaryFor(int ch) {
        CEList ces = uca.getCEList(UTF16.valueOf(ch), true);
        int firstPrimary = CEList.getPrimary(ces.at(0));
        getOrCreateProps(firstPrimary).useTwoBytePrimary = true;
    }
}
