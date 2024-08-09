package org.unicode.xml;

import com.ibm.icu.dev.util.UnicodeMap;
import java.util.*;
import org.unicode.cldr.draft.FileUtilities;
import org.unicode.props.*;

public class AttributeResolver {

    private final IndexUnicodeProperties indexUnicodeProperties;
    private final UnicodeMap<UcdPropertyValues.Age_Values> map_age;
    private final UnicodeMap<UcdPropertyValues.Bidi_Class_Values> map_bidi_class;
    private final UnicodeMap<UcdPropertyValues.Bidi_Paired_Bracket_Type_Values>
            map_bidi_paired_bracket_type;
    private final UnicodeMap<UcdPropertyValues.Block_Values> map_block;
    private final UnicodeMap<UcdPropertyValues.Canonical_Combining_Class_Values>
            map_canonical_combining_class;
    private final UnicodeMap<UcdPropertyValues.Decomposition_Type_Values> map_decomposition_type;
    private final UnicodeMap<UcdPropertyValues.Do_Not_Emit_Type_Values> map_do_not_emit_type;
    private final UnicodeMap<UcdPropertyValues.East_Asian_Width_Values> map_east_asian_width;
    private final UnicodeMap<UcdPropertyValues.General_Category_Values> map_general_category;
    private final UnicodeMap<UcdPropertyValues.Grapheme_Cluster_Break_Values>
            map_grapheme_cluster_break;
    private final UnicodeMap<UcdPropertyValues.Hangul_Syllable_Type_Values>
            map_hangul_syllable_type;
    private final UnicodeMap<UcdPropertyValues.Identifier_Status_Values> map_identifier_status;
    private final UnicodeMap<UcdPropertyValues.Identifier_Type_Values> map_identifier_type;
    private final UnicodeMap<UcdPropertyValues.Idn_2008_Values> map_idn_2008;
    private final UnicodeMap<UcdPropertyValues.Idn_Status_Values> map_idn_status;
    private final UnicodeMap<UcdPropertyValues.Indic_Conjunct_Break_Values>
            map_indic_conjunct_break;
    private final UnicodeMap<UcdPropertyValues.Indic_Positional_Category_Values>
            map_indic_positional_category;
    private final UnicodeMap<UcdPropertyValues.Indic_Syllabic_Category_Values>
            map_indic_syllabic_category;
    private final UnicodeMap<UcdPropertyValues.Jamo_Short_Name_Values> map_jamo_short_name;
    private final UnicodeMap<UcdPropertyValues.Joining_Group_Values> map_joining_group;
    private final UnicodeMap<UcdPropertyValues.Joining_Type_Values> map_joining_type;
    private final UnicodeMap<UcdPropertyValues.Line_Break_Values> map_line_break;
    private final UnicodeMap<UcdPropertyValues.NFC_Quick_Check_Values> map_nfc_quick_check;
    private final UnicodeMap<UcdPropertyValues.NFD_Quick_Check_Values> map_nfd_quick_check;
    private final UnicodeMap<UcdPropertyValues.NFKC_Quick_Check_Values> map_nfkc_quick_check;
    private final UnicodeMap<UcdPropertyValues.NFKD_Quick_Check_Values> map_nfkd_quick_check;
    private final UnicodeMap<UcdPropertyValues.Numeric_Type_Values> map_numeric_type;
    private final UnicodeMap<UcdPropertyValues.Other_Joining_Type_Values> map_other_joining_type;
    private final UnicodeMap<UcdPropertyValues.Script_Values> map_script;
    private final UnicodeMap<String> map_script_extensions;
    private final UnicodeMap<UcdPropertyValues.Sentence_Break_Values> map_sentence_break;
    private final UnicodeMap<UcdPropertyValues.Vertical_Orientation_Values>
            map_vertical_orientation;
    private final UnicodeMap<UcdPropertyValues.Word_Break_Values> map_word_break;
    private final HashMap<Integer, LinkedList<NameAlias>> map_NameAlias;

    // If there is a change in any of these properties between two adjacent characters, it will
    // result in a new range.
    private final UcdProperty[] rangeDefiningProperties = {
        UcdProperty.Age,
        UcdProperty.Bidi_Class,
        UcdProperty.Block,
        UcdProperty.Decomposition_Mapping,
        UcdProperty.Numeric_Type,
        UcdProperty.Numeric_Value,
        UcdProperty.Vertical_Orientation
    };

    public AttributeResolver(IndexUnicodeProperties iup) {
        indexUnicodeProperties = iup;
        map_age = indexUnicodeProperties.loadEnum(UcdProperty.Age);
        map_bidi_class = indexUnicodeProperties.loadEnum(UcdProperty.Bidi_Class);
        map_bidi_paired_bracket_type =
                indexUnicodeProperties.loadEnum(UcdProperty.Bidi_Paired_Bracket_Type);
        map_block = indexUnicodeProperties.loadEnum(UcdProperty.Block);
        map_canonical_combining_class =
                indexUnicodeProperties.loadEnum(UcdProperty.Canonical_Combining_Class);
        map_decomposition_type = indexUnicodeProperties.loadEnum(UcdProperty.Decomposition_Type);
        map_do_not_emit_type = indexUnicodeProperties.loadEnum(UcdProperty.Do_Not_Emit_Type);
        map_east_asian_width = indexUnicodeProperties.loadEnum(UcdProperty.East_Asian_Width);
        map_general_category = indexUnicodeProperties.loadEnum(UcdProperty.General_Category);
        map_grapheme_cluster_break =
                indexUnicodeProperties.loadEnum(UcdProperty.Grapheme_Cluster_Break);
        map_hangul_syllable_type =
                indexUnicodeProperties.loadEnum(UcdProperty.Hangul_Syllable_Type);
        map_identifier_status = indexUnicodeProperties.loadEnum(UcdProperty.Identifier_Status);
        map_identifier_type = indexUnicodeProperties.loadEnum(UcdProperty.Identifier_Type);
        map_idn_2008 = indexUnicodeProperties.loadEnum(UcdProperty.Idn_2008);
        map_idn_status = indexUnicodeProperties.loadEnum(UcdProperty.Idn_Status);
        map_indic_conjunct_break =
                indexUnicodeProperties.loadEnum(UcdProperty.Indic_Conjunct_Break);
        map_indic_positional_category =
                indexUnicodeProperties.loadEnum(UcdProperty.Indic_Positional_Category);
        map_indic_syllabic_category =
                indexUnicodeProperties.loadEnum(UcdProperty.Indic_Syllabic_Category);
        map_jamo_short_name = indexUnicodeProperties.loadEnum(UcdProperty.Jamo_Short_Name);
        map_joining_group = indexUnicodeProperties.loadEnum(UcdProperty.Joining_Group);
        map_joining_type = indexUnicodeProperties.loadEnum(UcdProperty.Joining_Type);
        map_line_break = indexUnicodeProperties.loadEnum(UcdProperty.Line_Break);
        map_nfc_quick_check = indexUnicodeProperties.loadEnum(UcdProperty.NFC_Quick_Check);
        map_nfd_quick_check = indexUnicodeProperties.loadEnum(UcdProperty.NFD_Quick_Check);
        map_nfkc_quick_check = indexUnicodeProperties.loadEnum(UcdProperty.NFKC_Quick_Check);
        map_nfkd_quick_check = indexUnicodeProperties.loadEnum(UcdProperty.NFKD_Quick_Check);
        map_numeric_type = indexUnicodeProperties.loadEnum(UcdProperty.Numeric_Type);
        map_other_joining_type = indexUnicodeProperties.loadEnum(UcdProperty.Other_Joining_Type);
        map_script = indexUnicodeProperties.loadEnum(UcdProperty.Script);
        map_script_extensions =
                indexUnicodeProperties.getProperty(UcdProperty.Script_Extensions).getUnicodeMap();
        map_sentence_break = indexUnicodeProperties.loadEnum(UcdProperty.Sentence_Break);
        map_vertical_orientation =
                indexUnicodeProperties.loadEnum(UcdProperty.Vertical_Orientation);
        map_word_break = indexUnicodeProperties.loadEnum(UcdProperty.Word_Break);

        // UCD code is only set up to read a single Alias value from NameAliases.txt
        // Instead, we'll load the Alias and the Type data as part of the constructor. We'll keep in
        // memory as it
        // NameAliases isn't too large.
        map_NameAlias = loadNameAliases();
    }

    private enum AliasType {
        ABBREVIATION("abbreviation"),
        ALTERNATE("alternate"),
        CONTROL("control"),
        CORRECTION("correction"),
        FIGMENT("figment");

        private final String aliasType;

        AliasType(String aliasType) {
            this.aliasType = aliasType;
        }

        public String toString() {
            return aliasType;
        }
    }

    private static class NameAlias {

        private String alias;
        private final AliasType type;

        private NameAlias(String alias, AliasType type) {
            this.alias = alias;
            this.type = type;
        }

        public String getAlias() {
            return alias;
        }

        public AliasType getType() {
            return type;
        }
    }

    private static class NameAliasComparator implements java.util.Comparator<NameAlias> {

        @Override
        public int compare(NameAlias o1, NameAlias o2) {
            return o1.getAlias().compareTo(o2.getAlias());
        }
    }

    private HashMap<Integer, LinkedList<NameAlias>> loadNameAliases() {
        HashMap<Integer, LinkedList<NameAlias>> nameAliasesByCodepoint = new HashMap<>();
        final PropertyParsingInfo fileInfo =
                PropertyParsingInfo.getPropertyInfo(UcdProperty.Name_Alias);
        String fullFilename = fileInfo.getFullFileName(indexUnicodeProperties.getUcdVersion());
        UcdLineParser parser = new UcdLineParser(FileUtilities.in("", fullFilename));
        NameAliasComparator nameAliasComparator = new NameAliasComparator();

        for (UcdLineParser.UcdLine line : parser) {
            String[] parts = line.getParts();
            int codepoint = Integer.parseInt(parts[0], 16);
            NameAlias nameAlias =
                    new NameAlias(parts[1], AliasType.valueOf(parts[2].toUpperCase(Locale.ROOT)));

            if (nameAliasesByCodepoint.containsKey(codepoint)) {
                LinkedList<NameAlias> nameAliases =
                        new LinkedList<>(nameAliasesByCodepoint.get(codepoint));
                nameAliases.add(nameAlias);
                nameAliases.sort(nameAliasComparator);
                nameAliasesByCodepoint.replace(codepoint, nameAliases);
            } else {
                nameAliasesByCodepoint.put(codepoint, new LinkedList<>(List.of(nameAlias)));
            }
        }
        return nameAliasesByCodepoint;
    }

    public String getAttributeValue(UcdProperty prop, int codepoint) {
        String resolvedValue = indexUnicodeProperties.getResolvedValue(prop, codepoint);
        switch (prop.getType()) {
            case Numeric:
                switch (prop) {
                    case kOtherNumeric:
                    case kPrimaryNumeric:
                    case kAccountingNumeric:
                        return (resolvedValue.equals("NaN")) ? null : resolvedValue;
                    default:
                        return Optional.ofNullable(resolvedValue).orElse("NaN");
                }
            case String:
                switch (prop) {
                    case Equivalent_Unified_Ideograph:
                        String EqUIdeo = getMappingValue(codepoint, resolvedValue, false, "");
                        return (EqUIdeo.equals("#")) ? null : EqUIdeo;
                    case kCompatibilityVariant:
                        String kCompatibilityVariant =
                                getMappingValue(codepoint, resolvedValue, false, "U+");
                        return (kCompatibilityVariant.equals("#")) ? "" : kCompatibilityVariant;
                    case kSimplifiedVariant:
                    case kTraditionalVariant:
                        String kVariant =
                                getMappingValue(
                                        codepoint,
                                        resolvedValue,
                                        isUnihanAttributeRange(codepoint),
                                        "U+");
                        return (kVariant.equals("#")) ? "" : kVariant;
                    case Bidi_Mirroring_Glyph:
                        // Returning empty string for bmg to maintain compatibility with older
                        // generated files.
                        String bmg = getMappingValue(codepoint, resolvedValue, false, "");
                        return (bmg.equals("#")) ? "" : bmg;
                    default:
                        return getMappingValue(codepoint, resolvedValue, false, "");
                }
            case Miscellaneous:
                switch (prop) {
                    case Jamo_Short_Name:
                        // return map_jamo_short_name.get(codepoint).getShortName();
                        return Optional.ofNullable(resolvedValue).orElse("");
                    case Name:
                        if (resolvedValue != null
                                && resolvedValue.startsWith("CJK UNIFIED IDEOGRAPH-")) {
                            return "CJK UNIFIED IDEOGRAPH-#";
                        }
                        if (resolvedValue != null
                                && resolvedValue.startsWith("CJK COMPATIBILITY IDEOGRAPH-")) {
                            return "CJK COMPATIBILITY IDEOGRAPH-#";
                        }
                        if (resolvedValue != null
                                && resolvedValue.startsWith("TANGUT IDEOGRAPH-")) {
                            return "TANGUT IDEOGRAPH-#";
                        }
                        if (resolvedValue != null
                                && resolvedValue.startsWith("KHITAN SMALL SCRIPT CHARACTER-")) {
                            return "KHITAN SMALL SCRIPT CHARACTER-#";
                        }
                        if (resolvedValue != null && resolvedValue.startsWith("NUSHU CHARACTER-")) {
                            return "NUSHU CHARACTER-#";
                        }
                        if (resolvedValue != null
                                && resolvedValue.startsWith("EGYPTIAN HIEROGLYPH-")) {
                            return "EGYPTIAN HIEROGLYPH-#";
                        }
                        return Optional.ofNullable(resolvedValue).orElse("");
                    case kDefinition:
                        return resolvedValue;
                    default:
                        if (resolvedValue != null) {
                            return resolvedValue.replaceAll("\\|", " ");
                        }
                        return "";
                }
            case Catalog:
                switch (prop) {
                    case Age:
                        String age = map_age.get(codepoint).getShortName();
                        return (age.equals("NA")) ? "unassigned" : age;
                    case Block:
                        return map_block.get(codepoint).getShortName();
                    case Script:
                        return map_script.get(codepoint).getShortName();
                    case Script_Extensions:
                        StringBuilder extensionBuilder = new StringBuilder();
                        String[] extensions = map_script_extensions.get(codepoint).split("\\|", 0);
                        for (String extension : extensions) {
                            extensionBuilder.append(
                                    UcdPropertyValues.Script_Values.valueOf(extension)
                                            .getShortName());
                            extensionBuilder.append(" ");
                        }
                        return extensionBuilder.toString().trim();
                    default:
                        throw new RuntimeException("Missing Catalog case");
                }
            case Enumerated:
                switch (prop) {
                    case Bidi_Class:
                        return map_bidi_class.get(codepoint).getShortName();
                    case Bidi_Paired_Bracket_Type:
                        return map_bidi_paired_bracket_type.get(codepoint).getShortName();
                    case Canonical_Combining_Class:
                        return map_canonical_combining_class.get(codepoint).getShortName();
                    case Decomposition_Type:
                        // Returning lower case to maintain compatibility with older generated
                        // files.
                        return map_decomposition_type
                                .get(codepoint)
                                .getShortName()
                                .toLowerCase(Locale.ROOT);
                    case Do_Not_Emit_Type:
                        return map_do_not_emit_type.get(codepoint).getShortName();
                    case East_Asian_Width:
                        return map_east_asian_width.get(codepoint).getShortName();
                    case General_Category:
                        return map_general_category.get(codepoint).getShortName();
                    case Grapheme_Cluster_Break:
                        return map_grapheme_cluster_break.get(codepoint).getShortName();
                    case Hangul_Syllable_Type:
                        return map_hangul_syllable_type.get(codepoint).getShortName();
                    case Identifier_Status:
                        return map_identifier_status.get(codepoint).getShortName();
                    case Identifier_Type:
                        return map_identifier_type.get(codepoint).getShortName();
                    case Idn_2008:
                        return map_idn_2008.get(codepoint).getShortName();
                    case Idn_Status:
                        return map_idn_status.get(codepoint).getShortName();
                    case Indic_Conjunct_Break:
                        return map_indic_conjunct_break.get(codepoint).getShortName();
                    case Indic_Positional_Category:
                        return map_indic_positional_category.get(codepoint).getShortName();
                    case Indic_Syllabic_Category:
                        return map_indic_syllabic_category.get(codepoint).getShortName();
                    case Joining_Group:
                        return map_joining_group.get(codepoint).getShortName();
                    case Joining_Type:
                        return map_joining_type.get(codepoint).getShortName();
                    case Line_Break:
                        return map_line_break.get(codepoint).getShortName();
                    case NFC_Quick_Check:
                        return map_nfc_quick_check.get(codepoint).getShortName();
                    case NFD_Quick_Check:
                        return map_nfd_quick_check.get(codepoint).getShortName();
                    case NFKC_Quick_Check:
                        return map_nfkc_quick_check.get(codepoint).getShortName();
                    case NFKD_Quick_Check:
                        return map_nfkd_quick_check.get(codepoint).getShortName();
                    case Numeric_Type:
                        return map_numeric_type.get(codepoint).getShortName();
                    case Other_Joining_Type:
                        return map_other_joining_type.get(codepoint).getShortName();
                    case Sentence_Break:
                        return map_sentence_break.get(codepoint).getShortName();
                    case Vertical_Orientation:
                        return map_vertical_orientation.get(codepoint).getShortName();
                    case Word_Break:
                        return map_word_break.get(codepoint).getShortName();
                    default:
                        throw new RuntimeException("Missing Enumerated case");
                }
            case Binary:
                {
                    switch (resolvedValue) {
                            // Seems overkill to get this from UcdPropertyValues.Binary
                        case "No":
                            return "N";
                        case "Yes":
                            return "Y";
                        default:
                            throw new RuntimeException("Unexpected Binary value");
                    }
                }
            default:
                throw new RuntimeException("Missing PropertyType case");
        }
    }

    public boolean isUnassignedCodepoint(int codepoint) {
        return UcdPropertyValues.General_Category_Values.Unassigned.equals(getgc(codepoint))
                || UcdPropertyValues.General_Category_Values.Private_Use.equals(getgc(codepoint))
                || UcdPropertyValues.General_Category_Values.Surrogate.equals(getgc(codepoint));
    }

    public UcdPropertyValues.General_Category_Values getgc(int codepoint) {
        return map_general_category.get(codepoint);
    }

    public String getNChar(int codepoint) {
        return getAttributeValue(UcdProperty.Noncharacter_Code_Point, codepoint);
    }

    public HashMap<String, String> getNameAliases(int codepoint) {
        HashMap<String, String> nameAliases = new LinkedHashMap<>();
        LinkedList<NameAlias> nameAliasList = map_NameAlias.get(codepoint);
        if (null != nameAliasList && !nameAliasList.isEmpty()) {
            for (NameAlias nameAlias : nameAliasList) {
                nameAliases.put(nameAlias.getAlias(), nameAlias.getType().toString());
            }
            return nameAliases;
        }
        return null;
    }

    private String getMappingValue(
            int codepoint, String resolvedValue, boolean ignoreUnihanRange, String prefix) {
        if (null == resolvedValue) {
            return "#";
        }
        int[] resolvedValueInts = resolvedValue.codePoints().toArray();
        if (resolvedValueInts.length == 1
                && resolvedValueInts[0] == codepoint
                && !ignoreUnihanRange) {
            return "#";
        }
        StringBuilder sb = new StringBuilder();
        for (int i : resolvedValueInts) {
            sb.append(prefix).append(getCPString(i)).append(" ");
        }
        return sb.toString().trim();
    }

    public boolean isDifferentRange(int codepointA, int codepointB) {
        boolean isDifference = false;
        for (UcdProperty property : rangeDefiningProperties) {
            isDifference =
                    isDifference
                            || !getAttributeValue(property, codepointA)
                                    .equals(getAttributeValue(property, codepointB));
        }
        return isDifference;
    }

    private static String getCPString(int codepoint) {
        return String.format("%4s", Integer.toHexString(codepoint))
                .replace(" ", "0")
                .toUpperCase(Locale.ROOT);
    }

    public String getHexString(int codepoint) {
        return getCPString(codepoint);
    }

    public boolean isUnihanAttributeRange(int codepoint) {
        return getAttributeValue(UcdProperty.Unified_Ideograph, codepoint).equals("Y")
                || !getAttributeValue(UcdProperty.kCompatibilityVariant, codepoint).isEmpty();
    }

    public boolean isUnifiedIdeograph(int codepoint) {
        return getAttributeValue(UcdProperty.Unified_Ideograph, codepoint).equals("Y")
                && getAttributeValue(UcdProperty.Name, codepoint).equals("CJK UNIFIED IDEOGRAPH-#");
    }
}
