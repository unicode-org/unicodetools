package org.unicode.xml;

import com.ibm.icu.util.VersionInfo;
import org.unicode.props.UcdProperty;

/**
 * Helper class that defines an object that stores information about a section of the UCDXML file.
 * Information includes the section name, the type of elements that the section contains, and the
 * version range of the section.
 */
public class UCDSectionDetail {

    public enum UcdSection {
        BLOCKS(
                "blocks",
                "block",
                VersionInfo.getInstance(1, 1, 0),
                null,
                Blocks_Detail,
                true,
                true),
        CJKRADICALS(
                "cjk-radicals",
                "cjk-radical",
                VersionInfo.getInstance(1, 1, 0),
                null,
                CJKRadicals_Detail,
                false,
                false),
        DONOTEMIT(
                "do-not-emit",
                "instead",
                VersionInfo.getInstance(16, 0, 0),
                null,
                DoNotEmit_Detail,
                false,
                false),
        EMOJISOURCES(
                "emoji-sources",
                "emoji-source",
                VersionInfo.getInstance(1, 1, 0),
                null,
                EmojiSources_Detail,
                true,
                false),
        NAMEDSEQUENCES(
                "named-sequences",
                "named-sequence",
                VersionInfo.getInstance(1, 1, 0),
                null,
                NamedSequences_Detail,
                false,
                false),
        PROVISIONALNAMEDSEQUENCES(
                "provisional-named-sequences",
                "named-sequence",
                VersionInfo.getInstance(5, 0, 0),
                VersionInfo.getInstance(13, 0, 0),
                ProvisionalNamedSequences_Detail,
                false,
                false),
        NORMALIZATIONCORRECTIONS(
                "normalization-corrections",
                "normalization-correction",
                VersionInfo.getInstance(1, 1, 0),
                null,
                NormalizationCorrections_Detail,
                true,
                false),
        STANDARDIZEDVARIANTS(
                "standardized-variants",
                "standardized-variant",
                VersionInfo.getInstance(1, 1, 0),
                null,
                StandardizedVariants_Detail,
                true,
                false);
        private final String tag;
        private final String childTag;
        private final VersionInfo minVersion;
        private final VersionInfo maxVersion;
        private final UCDSectionDetail ucdSectionDetail;
        private final boolean parserWithRange;
        private final boolean parserWithMissing;

        UcdSection(
                String tag,
                String childTag,
                VersionInfo minVersion,
                VersionInfo maxVersion,
                UCDSectionDetail ucdSectionDetail,
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

        public UCDSectionDetail getUcdSectionDetail() {
            return ucdSectionDetail;
        }

        public boolean getParserWithRange() {
            return parserWithRange;
        }

        public boolean getParserWithMissing() {
            return parserWithMissing;
        }
    }

    public static UCDSectionDetail Blocks_Detail =
            new UCDSectionDetail(
                    UcdSection.BLOCKS,
                    new UCDSectionComponent[] {
                        new UCDSectionComponent(
                                VersionInfo.getInstance(1, 1, 0), null, UcdProperty.Block)
                    },
                    0);
    public static UCDSectionDetail NamedSequences_Detail =
            new UCDSectionDetail(
                    UcdSection.NAMEDSEQUENCES,
                    new UCDSectionComponent[] {
                        new UCDSectionComponent(
                                VersionInfo.getInstance(1, 1, 0), null, UcdProperty.Named_Sequences)
                    },
                    1);
    public static UCDSectionDetail ProvisionalNamedSequences_Detail =
            new UCDSectionDetail(
                    UcdSection.PROVISIONALNAMEDSEQUENCES,
                    new UCDSectionComponent[] {
                        new UCDSectionComponent(
                                VersionInfo.getInstance(5, 0, 0),
                                VersionInfo.getInstance(13, 0, 0),
                                UcdProperty.Named_Sequences_Prov)
                    },
                    1);
    public static UCDSectionDetail NormalizationCorrections_Detail =
            new UCDSectionDetail(
                    UcdSection.NORMALIZATIONCORRECTIONS,
                    new UCDSectionComponent[] {
                        new UCDSectionComponent(
                                VersionInfo.getInstance(1, 1, 0), null, UcdProperty.NC_Original)
                    },
                    2);
    public static UCDSectionDetail StandardizedVariants_Detail =
            new UCDSectionDetail(
                    UcdSection.STANDARDIZEDVARIANTS,
                    new UCDSectionComponent[] {
                        new UCDSectionComponent(
                                VersionInfo.getInstance(1, 1, 0),
                                null,
                                UcdProperty.Standardized_Variant),
                        new UCDSectionComponent(
                                VersionInfo.getInstance(13, 0, 0),
                                null,
                                UcdProperty.emoji_variation_sequence)
                    },
                    3);
    public static UCDSectionDetail CJKRadicals_Detail =
            new UCDSectionDetail(
                    UcdSection.CJKRADICALS,
                    new UCDSectionComponent[] {
                        new UCDSectionComponent(
                                VersionInfo.getInstance(1, 1, 0), null, UcdProperty.CJK_Radical)
                    },
                    4);
    public static UCDSectionDetail EmojiSources_Detail =
            new UCDSectionDetail(
                    UcdSection.EMOJISOURCES,
                    new UCDSectionComponent[] {
                        new UCDSectionComponent(
                                VersionInfo.getInstance(1, 1, 0), null, UcdProperty.Emoji_DCM)
                    },
                    5);
    public static UCDSectionDetail DoNotEmit_Detail =
            new UCDSectionDetail(
                    UcdSection.DONOTEMIT,
                    new UCDSectionComponent[] {
                        new UCDSectionComponent(
                                VersionInfo.getInstance(1, 1, 0),
                                null,
                                UcdProperty.Do_Not_Emit_Type)
                    },
                    6);

    private final UcdSection ucdSection;
    private final UCDSectionComponent[] ucdSectionComponents;
    private final int sortOrder;

    private UCDSectionDetail(
            UcdSection ucdSection, UCDSectionComponent[] ucdSectionComponents, int sortOrder) {
        this.ucdSection = ucdSection;
        this.ucdSectionComponents = ucdSectionComponents;
        this.sortOrder = sortOrder;
    }

    public UcdSection getSection() {
        return this.ucdSection;
    }

    public UCDSectionComponent[] getUcdSectionComponents() {
        return this.ucdSectionComponents;
    }

    public int getSortOrder() {
        return this.sortOrder;
    }
}
