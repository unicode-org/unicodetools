package org.unicode.text.UCA;

import java.util.BitSet;

import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.UCD_Types;
import org.unicode.text.utility.Utility;

import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

/**
 * Maps UCA-style primary weights to byte-fractional primaries.
 *
 * @since 2013-jan-02 (mostly pulled out of {@link FractionalUCA})
 */
public final class PrimariesToFractional {
    private final ScriptOptions[] scriptOptions = new ScriptOptions[ReorderCodes.FULL_LIMIT];
    private final ScriptOptions ignorableOptions = new ScriptOptions(-1);

    private final UCA uca;

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
     * One flag per fractional primary lead byte for whether
     * the fractional weights that start with that byte are sort-key-compressible.
     */
    private final BitSet compressibleBytes = new BitSet(256);

    /** Maps UCA primaries to PrimaryToFractional objects. */
    private PrimaryToFractional[] primaryProps;
    // Put special properties into slots not used for UCA primaries.
    private static final int HAN_INDEX = 1;
    private static final int FFFD_INDEX = 2;
    // vate static final int FFFE_INDEX = 3;
    private static final int FFFF_INDEX = 4;

    private static final class ScriptOptions {
        final int reorderCode;

        boolean beginsByte;
        boolean endsByte;
        /**
         * If true, then primary weights of this group/script all have the same lead byte
         * and are therefore compressible when writing sort keys.
         * We need to know this before assigning
         * fractional primary weights so that we can assign them optimally.
         */
        boolean compressible = true;
        boolean defaultTwoBytePrimaries;
        boolean defaultTwoBytePunctuation;
        boolean twoBytesIfVariants;

        /** First UCA primary weight for this script. */
        int firstPrimary;
        /** The script-first fractional primary inserted before the normal fractional primary. */
        int scriptFirstFractional;
        /** true until the script-first fractional primary has been written. */
        boolean needToWriteScriptFirstFractional = true;

        ScriptOptions reservedBefore;

        ScriptOptions(int reorderCode) {
            this.reorderCode = reorderCode;
        }

        /** Use one or more whole primary lead byte. */
        ScriptOptions wholeByte() {
            beginsByte = endsByte = true;
            return this;
        }
        /** Start with a new primary lead byte. */
        ScriptOptions newByte() {
            beginsByte = true;
            return this;
        }
        /** End with the top of a primary lead byte. */
        ScriptOptions finishByte() {
            endsByte = true;
            return this;
        }
        ScriptOptions notCompressible() {
            if (!beginsByte && !endsByte) {
                throw new IllegalArgumentException(
                        "non-compressible script must begin or end with a lead byte boundary, " +
                        "or both; see LDML collation spec");
            }
            compressible = false;
            return this;
        }
        ScriptOptions twoBytePrimaries() {
            defaultTwoBytePrimaries = defaultTwoBytePunctuation = true;
            return this;
        }
        ScriptOptions twoBytePrimariesIfVariants() {
            twoBytesIfVariants = true;
            return this;
        }
        ScriptOptions twoBytePunctuation() {
            defaultTwoBytePunctuation = true;
            return this;
        }
        ScriptOptions threeBytePunctuation() {
            defaultTwoBytePunctuation = false;
            return this;
        }
    }

    /**
     * FractionalUCA properties for a UCA primary weight.
     */
    public static class PrimaryToFractional {
        private ScriptOptions options;
        /**
         * true if this primary is at the start of a group or script
         * that begins with a new primary lead byte.
         */
        private boolean newByte;

        private boolean useSingleBytePrimary;
        private boolean useTwoBytePrimary;
        private boolean useThreeBytePrimary;

        private int fractionalPrimary;
        /**
         * FractionalUCA sets neutralSec and neutralTer to the sec/ter values
         * when secTerToFractional==null and both values are either 0 or neutral.
         * These values are then added to secTerToFractional when it is allocated.
         */
        int neutralSec = -1;
        int neutralTer = -1;
        /**
         * {@link PrimaryToFractional} serves as a container for {@link SecTerToFractional}.
         * {@link PrimaryToFractional} does not set or use this reference at all.
         * We just avoid yet another map from primary weights to values,
         * and another map lookup for the same primary.
         *
         * <p>This is null until there is a non-neutral secondary or tertiary weight
         * for this primary.
         */
        public SecTerToFractional secTerToFractional;

        private PrimaryToFractional() {}

        /**
         * Returns the planned number of fractional primary weight bytes.
         */
        private int getFractionalLength() {
            if (useSingleBytePrimary) {
                return 1;
            }
            if (useTwoBytePrimary ||
                    (options.defaultTwoBytePrimaries && !useThreeBytePrimary) ||
                    (options.twoBytesIfVariants && secTerToFractional != null)) {
                return 2;
            }
            return 3;
        }

        private boolean isFirstForScript(int primary) {
            return primary == options.firstPrimary;
        }

        int getReorderCode() {
            return options.reorderCode;
        }

        /**
         * Returns the script-first fractional primary that precedes this UCA primary's
         * own fractional primary, if this is the first primary of a group or script,
         * otherwise returns 0.
         * The script-first primary is reset, so that the next call with the same UCA primary returns 0.
         */
        public int getAndResetScriptFirstFractionalPrimary() {
            if (options == null || !options.needToWriteScriptFirstFractional) {
                return 0;
            } else {
                options.needToWriteScriptFirstFractional = false;
                return options.scriptFirstFractional;
            }
        }

        public boolean beginsByte() {
            return newByte;
        }

        /**
         * Returns the fractional primary weight for the UCA primary.
         */
        public int getFractionalPrimary() {
            return fractionalPrimary;
        }

        /**
         * @return the script-first fractional primary of the reserved range before
         * this primary's script, or 0 if there is none
         */
        public int getReservedBeforeFractionalPrimary() {
            return options != null && options.reservedBefore != null ?
                    options.reservedBefore.scriptFirstFractional : 0;
        }

        /**
         * @return the special reorder code of the reserved range before
         * this primary's script, or -1 if there is none
         */
        public int getReservedBeforeReorderCode() {
            return options != null && options.reservedBefore != null ?
                    options.reservedBefore.reorderCode : -1;
        }
    }

    /**
     * Computes valid FractionalUCA primary weights of desired byte lengths.
     * Always starts with the first primary weight after 02.
     * {@link PrimaryWeight#next(int)} increments
     * one 1/2/3-byte weight to another 1/2/3-byte weight.
     */
    private static class PrimaryWeight {
        /**
         * For most bytes except a primary weight's lead byte, 02 is ok.
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
         * Increment byte2 a little more around single-byte primaries,
         * for tailoring of at least 4 two-byte primaries or more than 1000 three-byte primaries.
         */
        private static final int GAP2_FOR_SINGLE = 4;

        /**
         * Increment byte3 with a tailoring gap.
         *
         * <p>When the gap is too large, then we allocate too many primary weights for
         * a minor script and might overflow the single lead byte of a compressible reordering group.
         *
         * <p>When the gap is too small, then only a small number of characters can be tailored
         * (efficiently or at all) between root collation weights.
         *
         * <p>We can adjust in the {@link PrimariesToFractional#PrimariesToFractional(UCA)}
         * constructor which script starts a new lead byte.
         * See the comments there for criteria.
         */
        private static final int GAP3 = 6;

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

        private int numErrors = 0;

        public int getIntValue() {
            return (byte1 << 16) + (byte2 << 8) + byte3;
        }

        /**
         * @return a three-byte weight at the start of a new primary lead byte
         */
        public int startNewByte(ScriptOptions options) {
            final int oByte1 = byte1;
            final int oByte2 = byte2;

            int inc1;
            if (lastByteLength == 1) {
                // Single-byte gap of 1 from a single-byte weight to the new lead byte.
                inc1 = 2;
            } else if (lastByteLength == 2) {
                // At least a two-byte gap after a double.
                inc1 = byte2 < maxByte2 ? 1 : 2;
            } else /* lastByteLength == 3 */ {
                // At least a normal three-byte gap.
                inc1 = byte2 < maxByte2 || (byte3 + GAP3) <= 0xff ? 1 : 2;
            }
            if(inc1 != 1 && compressibleLeadByte) {
                ++numErrors;
                System.err.println(String.format(
                        "error in class PrimaryWeight: overflow of compressible lead byte %02X",
                        oByte1 & 0xff));
            }
            addTo1(inc1);

            final int newMinByte2 = options.compressible ? MIN2_COMPRESSED : MIN2_UNCOMPRESSED;
            byte2 = newMinByte2;
            byte3 = MIN_BYTE;

            check(oByte1, oByte2, 3, true);

            compressibleLeadByte = options.compressible;
            minByte2 = newMinByte2;
            maxByte2 = compressibleLeadByte ? MAX2_COMPRESSED : MAX2_UNCOMPRESSED;;
            lastByteLength = 3;
            return options.scriptFirstFractional = getIntValue();
        }

        /**
         * @return a three-byte weight at the start of a two-byte prefix
         */
        public int startNewScript(ScriptOptions options) {
            assertTrue(compressibleLeadByte == options.compressible);

            final int oByte1 = byte1;
            final int oByte2 = byte2;

            if (lastByteLength == 1) {
                // Larger two-byte gap after a single.
                addTo1(1);
                byte2 = minByte2 + GAP2_FOR_SINGLE;
            } else if (lastByteLength == 2) {
                // At least a two-byte gap after a double.
                addTo2(2);
            } else /* lastByteLength == 3 */ {
                // At least a normal three-byte gap.
                addTo2((byte3 + GAP3) <= 0xff ? 1 : 2);
            }
            // FractionalUCA has groups and scripts starting at least on two-byte boundaries.
            byte3 = MIN_BYTE;

            check(oByte1, oByte2, 3, false);

            lastByteLength = 3;
            return options.scriptFirstFractional = getIntValue();
        }

        public int next(int newByteLength) {
            final int oByte1 = byte1;
            final int oByte2 = byte2;

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
                    byte2 = minByte2 + GAP2_FOR_SINGLE;
                    break;
                case 3:
                    // Larger two-byte gap after a single.
                    addTo1(1);
                    byte2 = minByte2 + GAP2_FOR_SINGLE;
                    byte3 = MIN_BYTE;
                    break;
                }
                break;
            case 2:
                switch (newByteLength) {
                case 1:
                    // At least a larger two-byte gap before a single.
                    addTo1((byte2 + GAP2_FOR_SINGLE) <= maxByte2 ? 1 : 2);
                    byte2 = 0;
                    break;
                case 2:
                    // Normal two-byte gap.
                    addTo2(2);
                    break;
                case 3:
                    // At least a two-byte gap after a double.
                    addTo2(2);
                    byte3 = MIN_BYTE;
                    break;
                }
                break;
            case 3:
                switch (newByteLength) {
                case 1:
                    // At least a larger two-byte gap before a single.
                    addTo1((byte2 + GAP2_FOR_SINGLE) <= maxByte2 ? 1 : 2);
                    byte2 = byte3 = 0;
                    break;
                case 2:
                    // At least a two-byte gap before a double.
                    addTo2(2);
                    byte3 = 0;
                    break;
                case 3:
                    // Normal three-byte gap.
                    addTo3(GAP3 + 1);
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
                final int minLength = lastByteLength < newByteLength ? lastByteLength : newByteLength;
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
                if (compressibleLeadByte && byte1 != oByte1) {
                    ++numErrors;
                    System.err.println(String.format(
                            "error in class PrimaryWeight: overflow of compressible lead byte %02X",
                            oByte1 & 0xff));
                }
            }
        }

        private void assertTrue(boolean b) {
            if (!b) {
                throw new IllegalArgumentException();
            }
        }

        private void assertNoErrors() {
            if (numErrors > 0) {
                throw new IllegalArgumentException(numErrors + " errors");
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

        @Override
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
        primaryProps = new PrimaryToFractional[uca.getLastRegularPrimary() + 1];

        ignorableOptions.wholeByte().notCompressible();

        // Special reorder groups.

        // Some spaces and punctuation share a lead byte.
        setOptionsForScript(ReorderCodes.SPACE).newByte().notCompressible().twoBytePrimaries();
        setOptionsForScript(ReorderCodes.PUNCTUATION).finishByte().notCompressible().twoBytePrimaries();

        // Some general and currency symbols share a lead byte.
        setOptionsForScript(ReorderCodes.SYMBOL).newByte().notCompressible().twoBytePrimaries();
        setOptionsForScript(ReorderCodes.CURRENCY).finishByte().notCompressible().twoBytePrimaries();

        setOptionsForScript(ReorderCodes.DIGIT).wholeByte().notCompressible().twoBytePrimaries();

        // Set options for scripts in collation order,
        // to keep byte boundaries understandable.
        // Scripts that are not mentioned get default options.

        // We consider several factors for how to allocate primary weights.
        // - Collation elements are easy to store in 32-bit integers if
        //   primary weights are one or two bytes,
        //   or three bytes (long-primary CEs) if there are no secondary/tertiary variants.
        // - Short weights for short sort keys.
        // - Primary compression for short sort keys, requires all primaries
        //   for a compressible script to use the same lead byte.
        //   With compression, consecutive n-byte primaries approach (n-1) bytes each
        //   in sort keys.
        // - ICU stores a whole range of mappings with very little root collation data when
        //   consecutive characters have consecutive three-byte primary weights,
        //   with a consistent delta from each to the next.
        // - For script reordering, giving a script one or more whole lead bytes
        //   yields best performance, but we can do so only for a small number of scripts.
        //   Second best is for a script to be the first in a lead byte,
        //   see ICU ticket #11449.
        //
        // The more widely used a script or character, the more we want to optimize
        // its lookup performance (via encoding simplicity/regularity)
        // and its sort key length (with or without primary compression).
        // For rare and historic characters, we optimize for minimal storage.

        // "Recommended Scripts" (http://www.unicode.org/reports/tr31/#Table_Recommended_Scripts)
        // are in "widespread modern customary use" but to widely varying degrees.
        // http://en.wikipedia.org/wiki/List_of_writing_systems shows scripts by number of users.
        // Neither list ranks by usage in computers or on the internet.

        // Mark reserved ranges as not compressible, to avoid confusion,
        // and to avoid tools code issues with using multiple lead bytes.
        setOptionsForReservedRangeBeforeScript(
                ReorderCodes.REORDER_RESERVED_BEFORE_LATIN, UCD_Types.LATIN_SCRIPT)
                .wholeByte().notCompressible();
        // Latin uses multiple bytes, with single-byte primaries for A-Z.
        setOptionsForScript(UCD_Types.LATIN_SCRIPT).wholeByte().notCompressible().twoBytePrimaries();
        setOptionsForReservedRangeBeforeScript(
                ReorderCodes.REORDER_RESERVED_AFTER_LATIN, UCD_Types.GREEK_SCRIPT)
                .wholeByte().notCompressible();
        // Recommended Script, and cased.
        setOptionsForScript(UCD_Types.GREEK_SCRIPT).newByte().twoBytePrimaries();
        // Not a Recommended Script but cased, and easily fits into the same lead byte as Greek.
        setOptionsForScript(UCD_Types.COPTIC).twoBytePrimaries().threeBytePunctuation();
        // Cyrillic uses one byte, with two-byte primaries for common characters.
        setOptionsForScript(UCD_Types.CYRILLIC_SCRIPT).wholeByte().twoBytePunctuation();
        // Recommended Script, and cased.
        setOptionsForScript(UCD_Types.GEORGIAN_SCRIPT).twoBytePrimaries();
        // Recommended Script, and cased. Does not fit in a lead byte with Georgian.
        setOptionsForScript(UCD_Types.ARMENIAN_SCRIPT).newByte().twoBytePrimaries();
        // Recommended Script, few primaries, with active computer/internet usage.
        setOptionsForScript(UCD_Types.HEBREW_SCRIPT).newByte().twoBytePrimaries();
        // Arabic uses one byte, with two-byte primaries for common characters.
        setOptionsForScript(UCD_Types.ARABIC_SCRIPT).wholeByte().twoBytePunctuation();
        // Recommended Script, few primaries.
        setOptionsForScript(UCD_Types.THAANA_SCRIPT).twoBytePrimaries();
        // Ethiopic is a Recommended Script but needs three-byte primaries so that
        // they fit into one compressible lead byte.
        setOptionsForScript(UCD_Types.ETHIOPIC_SCRIPT).twoBytePunctuation();
        // Indic Recommended Scripts.
        // With two-byte primaries, nearly each of these scripts uses more than half of a lead byte.
        // Also, script reordering often reorders multiple of them,
        // so using whole bytes preserves reorder-reserved bytes.
        setOptionsForScript(UCD_Types.DEVANAGARI_SCRIPT).wholeByte().twoBytePrimaries();
        setOptionsForScript(UCD_Types.BENGALI_SCRIPT).wholeByte().twoBytePrimaries();
        setOptionsForScript(UCD_Types.GURMUKHI_SCRIPT).wholeByte().twoBytePrimaries();
        setOptionsForScript(UCD_Types.GUJARATI_SCRIPT).wholeByte().twoBytePrimaries();
        setOptionsForScript(UCD_Types.ORIYA_SCRIPT).wholeByte().twoBytePrimaries();
        setOptionsForScript(UCD_Types.TAMIL_SCRIPT).wholeByte().twoBytePrimaries();
        setOptionsForScript(UCD_Types.TELUGU_SCRIPT).wholeByte().twoBytePrimaries();
        setOptionsForScript(UCD_Types.KANNADA_SCRIPT).wholeByte().twoBytePrimaries();
        setOptionsForScript(UCD_Types.MALAYALAM_SCRIPT).wholeByte().twoBytePrimaries();
        // Sinhala shares its lead byte with minor scripts.
        setOptionsForScript(UCD_Types.SINHALA_SCRIPT).newByte().twoBytePrimaries();
        // Recommended Script.
        setOptionsForScript(UCD_Types.THAI_SCRIPT).wholeByte().twoBytePrimaries();
        // Recommended Script.
        setOptionsForScript(UCD_Types.LAO_SCRIPT).newByte().twoBytePrimaries();
        // Recommended Script.
        setOptionsForScript(UCD_Types.TIBETAN_SCRIPT).newByte().twoBytePrimaries();
        // Myanmar is a Recommended Script but needs three-byte primaries so that
        // they fit into one compressible lead byte.
        setOptionsForScript(UCD_Types.MYANMAR_SCRIPT).newByte().twoBytePunctuation();
        // Recommended Script.
        setOptionsForScript(UCD_Types.KHMER_SCRIPT).twoBytePrimaries();
        // Limited Use Script, but the previous lead byte is full.
        // TODO: revisit boundary; Cher is cased in Unicode 8.
        setOptionsForScript(UCD_Types.CHEROKEE_SCRIPT).newByte();
        // Hangul uses one byte, with two-byte primaries for conjoining Jamo L/V/T.
        setOptionsForScript(UCD_Types.HANGUL_SCRIPT).wholeByte().twoBytePunctuation();
        // Kana uses one byte.
        setOptionsForScripts(
                UCD_Types.HIRAGANA_SCRIPT, UCD_Types.KATAKANA_SCRIPT, UCD_Types.KATAKANA_OR_HIRAGANA)
                .wholeByte().twoBytePrimaries();
        // Recommended Script, uses two-byte primaries for characters with variants.
        // TODO: maybe just .twoBytePrimaries() and another new byte for following scripts
        setOptionsForScript(UCD_Types.BOPOMOFO_SCRIPT).newByte()
                .twoBytePrimariesIfVariants().twoBytePunctuation();
        // Just register the scripts as aliases.
        setOptionsForScripts(UCD_Types.Meroitic_Cursive, UCD_Types.Meroitic_Hieroglyphs);
        // Han uses many bytes, so that tailoring tens of thousands of characters
        // can use many two-byte primaries.
        setOptionsForScript(UCD_Types.HAN_SCRIPT).wholeByte().notCompressible().twoBytePunctuation();

        // All other scripts get default options.
    }
 
    private ScriptOptions setOptionsForScript(int script) {
        return scriptOptions[script] = getOrCreateOptionsForScript(script);
    }

    private ScriptOptions setOptionsForScripts(int ...scripts) {
        ScriptOptions o = getOrCreateOptionsForScript(scripts[0]);  // The other scripts are aliases.
        for (int script : scripts) {
            scriptOptions[script] = o;
        }
        return o;
    }

    private ScriptOptions setOptionsForReservedRangeBeforeScript(int beforeCode, int script) {
        ScriptOptions r = getOrCreateOptionsForScript(beforeCode);
        ScriptOptions o = getOrCreateOptionsForScript(script);
        assert o.reservedBefore == null;
        return o.reservedBefore = r;
    }

    private ScriptOptions getOptionsForScript(int script) {
        return scriptOptions[script];
    }

    private ScriptOptions getOrCreateOptionsForScript(int script) {
        ScriptOptions o = scriptOptions[script];
        if (o == null) {
            scriptOptions[script] = o = new ScriptOptions(script);
        }
        return o;
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
        final PrimaryWeight fractionalPrimary = new PrimaryWeight();

        // Map UCA 0 to fractional 0.
        getOrCreateProps(0);

        topByteInfo.append("[top_byte\t00\tTERMINATOR ]\n");
        topByteInfo.append("[top_byte\t01\tLEVEL-SEPARATOR ]\n");
        topByteInfo.append("[top_byte\t02\tFIELD-SEPARATOR ]\n");

        final StringBuilder groupInfo = new StringBuilder();
        int previousGroupLeadByte = 3;
        boolean previousGroupIsCompressible = false;
        int numPrimaries = 0;

        // Start at 1 so zero stays zero.
        for (final UCA.Primary up : uca.getRegularPrimaries()) {
            final int primary = up.primary;

            // We need a fractional primary weight for this item.
            // We base it on the last value, and whether we have a 1, 2, or 3-byte weight.
            // If we change lengths, then we have to change the initial segment.
            // We change the first byte as determined before.

            final PrimaryToFractional props = getOrCreateProps(primary);
            if (props.isFirstForScript(primary)) {
                System.out.println("last weight: " + fractionalPrimary.toString());

                // Before and after Latin, one or more lead bytes are reserved
                // (not used by FractionalUCA primaries) for script reordering.
                //
                // Some lead bytes must be reserved because a script that does not use
                // one or more whole lead bytes must still be moved by a whole-byte offset
                // (to keep sort key bytes valid and compressible),
                // and so different parts of the original lead byte
                // map to different target bytes.
                // Each time a lead byte is split a reserved byte must be used up.
                //
                // We reserve bytes before and after Latin so that typical reorderings
                // can move small scripts there without moving any other scripts.
                ScriptOptions options = props.options.reservedBefore;
                if (options != null) {
                    // Duplicate some of the code below.
                    // Since the DUCET does not reserve such ranges,
                    // there is no UCA primary to which we can assign a special object.
                    assert options.beginsByte;
                    final int firstFractional = fractionalPrimary.startNewByte(options);
                    final int leadByte = Fractional.getLeadByte(firstFractional);
                    appendTopByteInfo(topByteInfo, previousGroupIsCompressible,
                            previousGroupLeadByte, leadByte, groupInfo, numPrimaries);
                    previousGroupLeadByte = leadByte;
                    previousGroupIsCompressible = options.compressible;

                    numPrimaries = 0;
                    final String name = ReorderCodes.getName(options.reorderCode);
                    groupInfo.setLength(0);
                    groupInfo.append(name);
                    System.out.println(String.format(
                            "[%s]  # %s first primary",
                            Fractional.hexBytes(firstFractional), name));
                    // Create a one-byte gap, to reserve two bytes total for this range.
                    fractionalPrimary.byte1 += 1;
                }

                options = props.options;
                final int reorderCode = options.reorderCode;
                if (props.newByte) {
                    final int firstFractional = fractionalPrimary.startNewByte(options);
                    final int leadByte = Fractional.getLeadByte(firstFractional);

                    // Finish the previous reordering group.
                    appendTopByteInfo(topByteInfo, previousGroupIsCompressible,
                            previousGroupLeadByte, leadByte, groupInfo, numPrimaries);
                    previousGroupLeadByte = leadByte;
                    previousGroupIsCompressible = options.compressible;

                    // Now record the new group.
                    numPrimaries = 0;
                    groupInfo.setLength(0);
                    groupInfo.append(ReorderCodes.getShortName(reorderCode));
                    if (reorderCode == UCD_Types.HIRAGANA_SCRIPT) {
                        groupInfo.append(" Hrkt Kana");  // script aliases
                    }
                    String groupComment = " starts new lead byte";
                    if (options.compressible) {
                        compressibleBytes.set(leadByte);
                        groupComment = groupComment + " (compressible)";
                    }
                    System.out.println(String.format(
                            "[%s]  # %s first primary%s",
                            Fractional.hexBytes(firstFractional),
                            ReorderCodes.getName(reorderCode),
                            groupComment));

                    if (reorderCode == ReorderCodes.DIGIT) {
                        numericFractionalPrimary = fractionalPrimary.next(1);
                        ++numPrimaries;
                    }
                } else {
                    // New script but not a new lead byte.
                    final int firstFractional = fractionalPrimary.startNewScript(options);
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
                ++numPrimaries;
            }

            int currentByteLength = props.getFractionalLength();
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
                // (They are generated with special calls above,
                // so we need not handle them specially here.)
                final int nextPrimary = up.nextPrimary;
                final PrimaryToFractional nextProps = getProps(nextPrimary);
                if (0 <= nextPrimary && nextPrimary < UCA_Types.UNSUPPORTED_BASE &&
                        nextProps != null && nextProps.getFractionalLength() <= 2) {
                    currentByteLength = 2;
                }
            }
            String old;
            final boolean DEBUG_FW = false;
            if (DEBUG_FW) {
                old = fractionalPrimary.toString();
            }
            props.fractionalPrimary = fractionalPrimary.next(currentByteLength);
            ++numPrimaries;

            if (DEBUG_FW) {
                System.out.println(
                        currentByteLength
                        + ", " + old
                        + " => " + fractionalPrimary.toString()
                        + "\t" + Utility.hex(Character.codePointAt(up.getRepresentative(), 0)));
            }
        }
        System.out.println("last fractional primary weight: " + fractionalPrimary.toString());

        // Create an entry for the first primary in the Hani script.
        PrimaryToFractional hanProps = primaryProps[HAN_INDEX];
        final int firstFractional = fractionalPrimary.startNewByte(hanProps.options);
        int leadByte = Fractional.getLeadByte(firstFractional);

        // Finish the previous reordering group.
        appendTopByteInfo(topByteInfo, previousGroupIsCompressible,
                previousGroupLeadByte, leadByte, groupInfo, numPrimaries);
        previousGroupLeadByte = leadByte;

        // Record Hani.
        hanProps.options.scriptFirstFractional = firstFractional;
        System.out.println(String.format(
                "[%s]  # %s first primary%s",
                Fractional.hexBytes(firstFractional),
                ReorderCodes.getName(UCD_Types.HAN_SCRIPT),
                " starts reordering group"));

        leadByte = Fractional.IMPLICIT_BASE_BYTE;
        hanProps.fractionalPrimary = leadByte << 16;

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
        fractionalPrimary.assertNoErrors();
        return this;
    }

    private static void appendTopByteInfo(StringBuilder topByteInfo, boolean compress,
            int b, int limit, CharSequence groupInfo, int count) {
        final boolean canCompress = (limit - b) == 1;
        if (compress) {
            if (!canCompress) {
                // Was throw new IllegalArgumentException(...)
                // but PrimaryWeight also records an error and will throw an exception at the end,
                // after printing error messages.
                System.err.println(
                        "reordering group {" + groupInfo +
                        "} marked for compression but uses more than one lead byte " +
                        Utility.hex(b, 2) + ".." +
                        Utility.hex(limit - 1, 2));
                compress = false;
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

    private static int pinPrimary(int ucaPrimary) {
        if (HAN_INDEX <= ucaPrimary && ucaPrimary <= FFFF_INDEX) {
            throw new IllegalArgumentException(
                    "no properties for UCA primary used internally: " + ucaPrimary);
        }
        if (ucaPrimary < UCA_Types.UNSUPPORTED_BASE) {
            return ucaPrimary;
        }
        if (ucaPrimary < UCA_Types.UNSUPPORTED_OTHER_BASE) {
            return HAN_INDEX;
        }
        if (0xfffd <= ucaPrimary && ucaPrimary <= 0xffff) {
            return FFFD_INDEX + ucaPrimary - 0xfffd;
        }
        throw new IllegalArgumentException("no properties for unassigned UCA primary " + ucaPrimary);
    }

    /**
     * Returns the properties for the UCA primary weight.
     * Returns the same object for all Han UCA primary lead weights.
     * Returns null if there are no data for this primary.
     */
    public PrimaryToFractional getProps(int ucaPrimary) {
        int index = pinPrimary(ucaPrimary);
        return primaryProps[index];
    }

    /**
     * Returns the properties for the UCA primary weight.
     * Creates and caches a new one if there are no data for this primary yet.
     */
    public PrimaryToFractional getOrCreateProps(int ucaPrimary) {
        int index = pinPrimary(ucaPrimary);
        PrimaryToFractional props = primaryProps[index];
        if (props == null) {
            primaryProps[index] = props = new PrimaryToFractional();
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
        ScriptOptions options = getOptionsForScript(reorderCode);
        return options != null ? options.scriptFirstFractional : 0;
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
        final int firstScriptPrimary = uca.getStatistics().firstScript;

        ScriptOptions options = ignorableOptions;
        // Start after ignorable primary 0.
        for (final UCA.Primary up : uca.getRegularPrimaries()) {
            final int primary = up.primary;
            final PrimaryToFractional props = getOrCreateProps(primary);
            final int ch = up.getRepresentative().codePointAt(0);
            short script = Fractional.getFixedScript(ch);

            // see if we have an "infrequent" character: make it a 3 byte if so.
            // also collect data on primaries

            if (primary < firstScriptPrimary) {
                final int reorderCode = ReorderCodes.getSpecialReorderCode(ch);
                if (ch < 0xFF || reorderCode == ReorderCodes.SPACE) {
                    // do nothing, assume Latin 1 is "frequent"
                } else {
                    final byte cat = Fractional.getFixedCategory(ch);
                    if (cat == UCD_Types.OTHER_SYMBOL ||
                            cat == UCD_Types.MATH_SYMBOL ||
                            cat == UCD_Types.MODIFIER_SYMBOL ||
                            (script != UCD_Types.COMMON_SCRIPT &&
                                    script != UCD_Types.INHERITED_SCRIPT &&
                                    !getOrCreateOptionsForScript(script).defaultTwoBytePunctuation)) {
                        // Note: We do not test reorderCode == ReorderCodes.SYMBOL
                        // because that includes Lm etc.
                        props.useThreeBytePrimary = true;
                    }
                }
                script = (short) reorderCode;
            }

            if (script == UCD_Types.COMMON_SCRIPT || script == UCD_Types.INHERITED_SCRIPT ||
                    script == UCD_Types.Unknown_Script) {
                // Not a real script, keep current options.
            } else if (script != options.reorderCode) {
                ScriptOptions newOptions = getOrCreateOptionsForScript(script);
                if (newOptions.firstPrimary == 0) {
                    newOptions.firstPrimary = primary;
                    // Start a new lead byte according to the options.
                    // Also, compressible and uncompressible groups/scripts must not share
                    // a lead byte:
                    // - We emit (and implementations test) compressibility by primary lead bytes.
                    // - The set of bytes usable for primary second bytes depends on
                    //   whether the lead byte is compressible.
                    props.newByte = options.endsByte || newOptions.beginsByte ||
                            options.compressible != newOptions.compressible;
                    System.out.println(
                            (props.newByte ? "New primary lead byte:\t" : "Continue lead byte:   \t")
                            + Utility.hex(primary) + " "
                            + ReorderCodes.getName(script) + " "
                            + Utility.hex(ch) + " "
                            + Default.ucd().getName(ch));
                    options = newOptions;
                }
            }
            props.options = options;
        }
        System.out.println("Last UCA primary:     \t" + Utility.hex(uca.getLastRegularPrimary()));

        PrimaryToFractional hanProps = getOrCreateProps(UCA_Types.UNSUPPORTED_CJK_BASE);
        hanProps.options = getOptionsForScript(UCD_Types.HAN_SCRIPT);
        hanProps.options.firstPrimary = UCA_Types.UNSUPPORTED_CJK_BASE;
        hanProps.newByte = true;

        final char[][] singlePairs = {
                {'a','z'}, {' ', ' '},
                {'0', '9'}, {'.', '.'},  {',', ','}
        };
        for (final char[] singlePair : singlePairs) {
            final char start = singlePair[0];
            final char end = singlePair[1];
            for (char k = start; k <= end; ++k) {
                setSingleBytePrimaryFor(k);
            }
        }

        final UnicodeSet twoByteChars = new UnicodeSet(
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
        final UnicodeSetIterator twoByteIter = new UnicodeSetIterator(twoByteChars);
        while (twoByteIter.next()) {
            setTwoBytePrimaryFor(twoByteIter.codepoint);
        }
    }

    private void setSingleBytePrimaryFor(char ch) {
        final CEList ces = uca.getCEList(String.valueOf(ch), true);
        final int firstPrimary = CEList.getPrimary(ces.at(0));
        getOrCreateProps(firstPrimary).useSingleBytePrimary = true;
    }

    private void setTwoBytePrimaryFor(int ch) {
        final CEList ces = uca.getCEList(UTF16.valueOf(ch), true);
        final int firstPrimary = CEList.getPrimary(ces.at(0));
        getOrCreateProps(firstPrimary).useTwoBytePrimary = true;
    }
}
