package org.unicode.props;

import org.unicode.props.UcdPropertyValues.Bidi_Class_Values;
import org.unicode.props.UcdPropertyValues.Block_Values;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.VersionInfo;

/**
 * Default property values for some properties and certain ranges
 * other than all of Unicode.
 */
public final class DefaultValues {
    public static final class BidiClass {
        private static final Bidi_Class_Values L = Bidi_Class_Values.Left_To_Right;
        private static final Bidi_Class_Values R = Bidi_Class_Values.Right_To_Left;
        private static final Bidi_Class_Values AL = Bidi_Class_Values.Arabic_Letter;
        private static final Bidi_Class_Values BN = Bidi_Class_Values.Boundary_Neutral;
        private static final Bidi_Class_Values ET = Bidi_Class_Values.European_Terminator;

        public static enum Option { ALL, OMIT_BN };

        private static final class Builder {
            int compositeVersion;
            IndexUnicodeProperties props;
            UnicodeMap<Block_Values> blocks;
            UnicodeMap<Bidi_Class_Values> bidi = new UnicodeMap<>();

            Builder(VersionInfo version) {
                compositeVersion =
                        (version.getMajor() << 16) | (version.getMinor() << 8) | version.getMilli();
                props = IndexUnicodeProperties.make(version);
                blocks = props.loadEnum(UcdProperty.Block);
            }

            UnicodeMap<Bidi_Class_Values> build(Option option) {
                // Overall default
                bidi.setMissing(L);

                // Set defaults in ascending order of Unicode versions,
                // at least if there are overlaps, so that a later change
                // can override parts of an earlier, larger range.
                // Adding a block before it existed in the given version is a no-op.
                // If a block has had its default value since it was allocated,
                // then we could simply use minVersion=0x30000
                // (but it would be less obvious which block got its default when).

                // Unicode 3.0 was the first version to publish UAX #9, effectively create
                // the Bidi_Class property, and assign default Bidi_Class values.
                addBlockValueIfAtLeast(Block_Values.Hebrew, 0x30000, R);
                addBlockValueIfAtLeast(Block_Values.Arabic, 0x30000, AL);
                addBlockValueIfAtLeast(Block_Values.Syriac, 0x30000, AL);
                addRangeValueIfAtLeast(0x0750, 0x077F, 0x30000, AL);
                addBlockValueIfAtLeast(Block_Values.Thaana, 0x30000, AL);
                addRangeValueIfAtLeast(0xFB1D, 0xFB4F, 0x30000, R);
                addBlockValueIfAtLeast(Block_Values.Arabic_Presentation_Forms_A, 0x30000, AL);
                addBlockValueIfAtLeast(Block_Values.Arabic_Presentation_Forms_B, 0x30000, AL);

                addRangeValueIfAtLeast(0x07C0, 0x8FF, 0x40000, R);
                addRangeValueIfAtLeast(0x10800, 0x10FFF, 0x40000, R);

                // In order to be precise, exclude U+FEFF ("BOM") for Unicode 4.0 & 4.0.1.
                // See https://www.unicode.org/Public/4.0-Update1/UCD-4.0.1.html
                // This had no real effect, since U+FEFF was already
                // an *assigned* character with bc=BN.
                if (0x40000 <= compositeVersion && compositeVersion < 0x40100) {
                    bidi.put(0xFEFF, L);
                }

                // The noncharacter code points FDD0..FDEF were designated in Unicode 3.1, but the
                // whole enclosing block FB50..FDFF Arabic Presentation Forms-A kept default bc=AL.
                // Unicode 4.0 then excluded these noncharacters from bc=AL.
                addRangeValueIfAtLeast(0xFDD0, 0xFDEF, 0x40000, L);

                // Unicode 4.0.1 changed all noncharacter code points and
                // default ignorables to default bc=BN.
                // Since many of these ranges are not aligned with block boundaries,
                // we may omit them when presenting defaults.
                if (compositeVersion >= 0x40001 && option != Option.OMIT_BN) {
                    UnicodeSet nonchars = props.loadBinary(UcdProperty.Noncharacter_Code_Point);
                    bidi.putAll(nonchars, BN);
                    UnicodeSet defaultIgnorable =
                            props.loadBinary(UcdProperty.Default_Ignorable_Code_Point);
                    bidi.putAll(defaultIgnorable, BN);
                }

                addBlockValueIfAtLeast(Block_Values.Arabic_Supplement, 0x40100, AL);
                addRangeValueIfAtLeast(0x1E800, 0x1EFFF, 0x50200, R);
                addBlockValueIfAtLeast(Block_Values.Arabic_Extended_A, 0x60100, AL);
                addBlockValueIfAtLeast(
                        Block_Values.Arabic_Mathematical_Alphabetic_Symbols, 0x60100, AL);
                addBlockValueIfAtLeast(
                        Block_Values.Currency_Symbols, 0x60300, ET); // default ET since 6.3

                addBlockValueIfAtLeast(Block_Values.Syriac_Supplement, 0xA0000, AL);
                addBlockValueIfAtLeast(Block_Values.Hanifi_Rohingya, 0xB0000, AL);
                addBlockValueIfAtLeast(Block_Values.Sogdian, 0xB0000, AL);
                addBlockValueIfAtLeast(Block_Values.Indic_Siyaq_Numbers, 0xB0000, AL);
                addBlockValueIfAtLeast(Block_Values.Ottoman_Siyaq_Numbers, 0xC0000, AL);
                addBlockValueIfAtLeast(Block_Values.Arabic_Extended_B, 0xE0000, AL);
                addBlockValueIfAtLeast(Block_Values.Arabic_Extended_C, 0xF0000, AL);

                return bidi;
            }

            private void addRangeValueIfAtLeast(
                    int start, int end, int minVersion, Bidi_Class_Values bidiValue) {
                if (compositeVersion >= minVersion) {
                    bidi.putAll(start, end, bidiValue);
                }
            }

            private void addBlockValueIfAtLeast(
                    Block_Values blockValue, int minVersion, Bidi_Class_Values bidiValue) {
                if (compositeVersion >= minVersion) {
                    UnicodeSet block = blocks.keySet(blockValue);
                    bidi.putAll(block, bidiValue);
                }
            }
        }

        public static UnicodeMap<Bidi_Class_Values> forVersion(
                VersionInfo version, Option option) {
            return new Builder(version).build(option);
        }
    }
}
