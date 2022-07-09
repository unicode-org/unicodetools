package org.unicode.props;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.VersionInfo;
import org.unicode.props.UcdPropertyValues.Bidi_Class_Values;
import org.unicode.props.UcdPropertyValues.Block_Values;
import org.unicode.props.UcdPropertyValues.East_Asian_Width_Values;
import org.unicode.props.UcdPropertyValues.Line_Break_Values;

/**
 * Default property values for some properties and certain ranges other than all of Unicode. See
 * https://www.unicode.org/reports/tr44/#Complex_Default_Values
 */
public final class DefaultValues {
    private static final Block_Values CJK_Ext_A = Block_Values.CJK_Unified_Ideographs_Extension_A;

    private static final UnicodeSet IDEO_PLANES =
            new UnicodeSet(0x20000, 0x2FFFD, 0x30000, 0x3FFFD).freeze();

    private static class BuilderBase<T> {
        int compositeVersion;
        IndexUnicodeProperties props;
        UnicodeMap<Block_Values> blocks;

        BuilderBase(VersionInfo version) {
            compositeVersion =
                    (version.getMajor() << 16) | (version.getMinor() << 8) | version.getMilli();
            props = IndexUnicodeProperties.make(version);
            blocks = props.loadEnum(UcdProperty.Block);
        }

        protected void addRangeValueIfAtLeast(
                UnicodeMap<T> map, int start, int end, int minVersion, T value) {
            if (compositeVersion >= minVersion) {
                map.putAll(start, end, value);
            }
        }

        protected void addSetValueIfAtLeast(
                UnicodeMap<T> map, UnicodeSet set, int minVersion, T value) {
            if (compositeVersion >= minVersion) {
                map.putAll(set, value);
            }
        }

        protected void addBlockValueIfAtLeast(
                UnicodeMap<T> map, Block_Values blockValue, int minVersion, T value) {
            if (compositeVersion >= minVersion) {
                UnicodeSet block = blocks.keySet(blockValue);
                map.putAll(block, value);
            }
        }
    }

    public static final class BidiClass {
        private static final Bidi_Class_Values L = Bidi_Class_Values.Left_To_Right;
        private static final Bidi_Class_Values R = Bidi_Class_Values.Right_To_Left;
        private static final Bidi_Class_Values AL = Bidi_Class_Values.Arabic_Letter;
        private static final Bidi_Class_Values BN = Bidi_Class_Values.Boundary_Neutral;
        private static final Bidi_Class_Values ET = Bidi_Class_Values.European_Terminator;

        public static enum Option {
            ALL,
            OMIT_BN
        };

        private static final class Builder extends BuilderBase<Bidi_Class_Values> {
            UnicodeMap<Bidi_Class_Values> bidi = new UnicodeMap<>();

            Builder(VersionInfo version) {
                super(version);
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
                    int start, int end, int minVersion, Bidi_Class_Values value) {
                addRangeValueIfAtLeast(bidi, start, end, minVersion, value);
            }

            private void addBlockValueIfAtLeast(
                    Block_Values blockValue, int minVersion, Bidi_Class_Values value) {
                addBlockValueIfAtLeast(bidi, blockValue, minVersion, value);
            }
        }

        public static UnicodeMap<Bidi_Class_Values> forVersion(VersionInfo version, Option option) {
            return new Builder(version).build(option);
        }
    }

    public static final class EastAsianWidth {
        // private static final East_Asian_Width_Values A = East_Asian_Width_Values.Ambiguous;
        private static final East_Asian_Width_Values N = East_Asian_Width_Values.Neutral;
        private static final East_Asian_Width_Values W = East_Asian_Width_Values.Wide;

        private static final class Builder extends BuilderBase<East_Asian_Width_Values> {
            UnicodeMap<East_Asian_Width_Values> ea = new UnicodeMap<>();

            Builder(VersionInfo version) {
                super(version);
            }

            UnicodeMap<East_Asian_Width_Values> build() {
                // Unicode 3.0 was the first version to publish UAX #11, effectively create
                // the East_Asian_Width property, and assign default East_Asian_Width values.

                // https://www.unicode.org/reports/tr44/#Complex_Default_Values
                // This property defaults to Neutral for most code points, but
                // defaults to Wide for unassigned code points in blocks associated with CJK
                // ideographs.
                ea.setMissing(N);

                // https://www.unicode.org/reports/tr11/#Unassigned
                // All private-use characters are by default classified as Ambiguous...
                // (except not the noncharacters)
                //
                // Omit these, so that private use areas (which are not unassigned gc=Cn)
                // are listed in data files explicitly.
                //
                // addBlockValueIfAtLeast(Block_Values.Private_Use_Area, 0x30000, A);
                // addRangeValueIfAtLeast(0xF0000, 0xFFFFD, 0x30100, A);
                // addRangeValueIfAtLeast(0x100000, 0x10FFFD, 0x30100, A);

                // Unassigned code points in ranges intended for CJK ideographs
                // are classified as Wide. Those ranges are:
                // - the CJK Unified Ideographs block, 4E00..9FFF
                // - the CJK Unified Ideographs Extension A block, 3400..4DBF
                // - the CJK Compatibility Ideographs block, F900..FAFF
                // - the Supplementary Ideographic Plane, 20000..2FFFF
                // - the Tertiary Ideographic Plane, 30000..3FFFF
                // (except not the noncharacters)
                addBlockValueIfAtLeast(ea, Block_Values.CJK_Unified_Ideographs, 0x50200, W);
                addBlockValueIfAtLeast(ea, CJK_Ext_A, 0x50200, W);
                addBlockValueIfAtLeast(ea, Block_Values.CJK_Compatibility_Ideographs, 0x50200, W);
                addSetValueIfAtLeast(ea, IDEO_PLANES, 0x40000, W);

                return ea;
            }
        }

        public static UnicodeMap<East_Asian_Width_Values> forVersion(VersionInfo version) {
            return new Builder(version).build();
        }
    }

    public static final class LineBreak {
        private static final Line_Break_Values XX = Line_Break_Values.Unknown;
        private static final Line_Break_Values ID = Line_Break_Values.Ideographic;
        private static final Line_Break_Values PR = Line_Break_Values.Prefix_Numeric;

        private static final class Builder extends BuilderBase<Line_Break_Values> {
            UnicodeMap<Line_Break_Values> lb = new UnicodeMap<>();

            Builder(VersionInfo version) {
                super(version);
            }

            UnicodeMap<Line_Break_Values> build() {
                // https://www.unicode.org/reports/tr44/#Complex_Default_Values
                // This property defaults to Unknown for most code points, but defaults to
                // ID for unassigned code points in blocks associated with CJK ideographs,
                // and in blocks in the ranges U+1F000..U+1FAFF and U+1FC00..U+1FFFD.
                // The property defaults to PR for unassigned code points in the
                // Currency Symbols block.
                // (except not the noncharacters)
                lb.setMissing(XX);
                addBlockValueIfAtLeast(lb, Block_Values.CJK_Unified_Ideographs, 0x50200, ID);
                addBlockValueIfAtLeast(lb, CJK_Ext_A, 0x50200, ID);
                addBlockValueIfAtLeast(lb, Block_Values.CJK_Compatibility_Ideographs, 0x50200, ID);
                addSetValueIfAtLeast(lb, IDEO_PLANES, 0x50200, ID);

                addBlockValueIfAtLeast(lb, Block_Values.Currency_Symbols, 0x60300, PR);

                addRangeValueIfAtLeast(lb, 0x1F000, 0x1FFFD, 0x90000, ID);
                // Unicode 13+: punch a hole
                addRangeValueIfAtLeast(lb, 0x1FB00, 0x1FBFF, 0xD0000, XX);

                return lb;
            }
        }

        public static UnicodeMap<Line_Break_Values> forVersion(VersionInfo version) {
            return new Builder(version).build();
        }
    }
}
