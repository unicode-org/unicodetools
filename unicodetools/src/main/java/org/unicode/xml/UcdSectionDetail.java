package org.unicode.xml;

import com.ibm.icu.util.VersionInfo;
import org.unicode.props.UcdProperty;

import java.util.LinkedHashSet;
import java.util.Set;

public class UcdSectionDetail {

    public enum UcdSection {
        BLOCKS ("blocks", "block", VersionInfo.getInstance(1, 1, 0), null, Blocks_Detail, true, true),
        CJKRADICALS ("cjk-radicals", "cjk-radical", VersionInfo.getInstance(1, 1, 0), null, CJKRadicals_Detail, false, false),
        DONOTEMIT ("do-not-emit", "instead", VersionInfo.getInstance(16, 0, 0), null, DoNotEmit_Detail, false, false),
        EMOJISOURCES ("emoji-sources", "emoji-source", VersionInfo.getInstance(1, 1, 0), null, EmojiSources_Detail, true, false),
        NAMEDSEQUENCES ("named-sequences", "named-sequence", VersionInfo.getInstance(1, 1, 0), null, NamedSequences_Detail, false, false),
        NORMALIZATIONCORRECTIONS ("normalization-corrections", "normalization-correction", VersionInfo.getInstance(1, 1, 0), null, NormalizationCorrections_Detail, true, false),
        STANDARDIZEDVARIANTS ("standardized-variants", "standardized-variant", VersionInfo.getInstance(1, 1, 0), null, StandardizedVariants_Detail, true, false);
        private final String tag;
        private final String childTag;
        private final VersionInfo minVersion;
        private final VersionInfo maxVersion;
        private final UcdSectionDetail ucdSectionDetail;
        private final boolean parserWithRange;
        private final boolean parserWithMissing;

        UcdSection(
                String tag,
                String childTag,
                VersionInfo minVersion,
                VersionInfo maxVersion,
                UcdSectionDetail ucdSectionDetail,
                boolean parserWithRange,
                boolean parserWithMissing) {
            this.tag = tag;
            this.childTag = childTag;
            this.minVersion = minVersion;
            this.maxVersion = maxVersion;
            this.ucdSectionDetail = ucdSectionDetail;
            this.parserWithRange = parserWithRange;
            this.parserWithMissing = parserWithMissing;
        }

        public String toString() {
            return tag;
        }
        public String getChildTag() {
            return childTag;
        }
        public VersionInfo getMinVersion() {
            return minVersion;
        }
        public VersionInfo getMaxVersion() {
            return maxVersion;
        }
        public UcdSectionDetail getUcdSectionDetail() {
            return ucdSectionDetail;
        }
        public boolean getParserWithRange() { return parserWithRange; }
        public boolean getParserWithMissing() { return parserWithMissing; }
    }

    public static UcdSectionDetail Blocks_Detail = new UcdSectionDetail(
            UcdSection.BLOCKS,
            new UcdSectionComponent[]{
                    new UcdSectionComponent(
                            VersionInfo.getInstance(1, 1, 0),
                            null,
                            UcdProperty.Block)
            },
            0);
    public static UcdSectionDetail NamedSequences_Detail = new UcdSectionDetail(
            UcdSection.NAMEDSEQUENCES,
            new UcdSectionComponent[]{
                    new UcdSectionComponent(
                            VersionInfo.getInstance(1, 1, 0),
                            null,
                            UcdProperty.Named_Sequences)
            },
            1);
    public static UcdSectionDetail NormalizationCorrections_Detail = new UcdSectionDetail(
            UcdSection.NORMALIZATIONCORRECTIONS,
            new UcdSectionComponent[]{
                    new UcdSectionComponent(
                            VersionInfo.getInstance(1, 1, 0),
                            null,
                            UcdProperty.NC_Original)
            },
            2);
    public static UcdSectionDetail StandardizedVariants_Detail = new UcdSectionDetail(
            UcdSection.STANDARDIZEDVARIANTS,
            new UcdSectionComponent[]{
                    new UcdSectionComponent(
                            VersionInfo.getInstance(1, 1, 0),
                            null,
                            UcdProperty.Standardized_Variant),
                    new UcdSectionComponent(
                            VersionInfo.getInstance(13, 1, 0),
                            null,
                            UcdProperty.emoji_variation_sequence)
            },
            3);
    public static UcdSectionDetail CJKRadicals_Detail = new UcdSectionDetail(
            UcdSection.CJKRADICALS,
            new UcdSectionComponent[]{
                    new UcdSectionComponent(
                            VersionInfo.getInstance(1, 1, 0),
                            null,
                            UcdProperty.CJK_Radical)
            },
            4);
    public static UcdSectionDetail EmojiSources_Detail = new UcdSectionDetail(
            UcdSection.EMOJISOURCES,
            new UcdSectionComponent[]{
                    new UcdSectionComponent(
                            VersionInfo.getInstance(1, 1, 0),
                            null,
                            UcdProperty.Emoji_DCM)
            },
            5);
    public static UcdSectionDetail DoNotEmit_Detail = new UcdSectionDetail(
            UcdSection.DONOTEMIT,
            new UcdSectionComponent[]{
                    new UcdSectionComponent(
                            VersionInfo.getInstance(1, 1, 0),
                            null,
                            UcdProperty.Do_Not_Emit_Type)
            },
            6);

    private final UcdSection ucdSection;
    private final UcdSectionComponent[] ucdSectionComponents;
    private final int sortOrder;

    private UcdSectionDetail(
            UcdSection ucdSection,
            UcdSectionComponent[] ucdSectionComponents,
            int sortOrder) {
        this.ucdSection = ucdSection;
        this.ucdSectionComponents = ucdSectionComponents;
        this.sortOrder = sortOrder;
    }

    public UcdSection getSection() {
        return this.ucdSection;
    }
    public UcdSectionComponent[] getUcdSectionComponents() {
        return this.ucdSectionComponents;
    }
    public int getSortOrder() {
        return this.sortOrder;
    }
}