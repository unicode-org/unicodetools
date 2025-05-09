package org.unicode.xml;

import com.ibm.icu.util.VersionInfo;
import java.util.LinkedHashSet;
import java.util.Set;
import org.unicode.props.UcdProperty;

/**
 * Helper class for determining how and when UCD properties should be shown in UCDXML. Also includes
 * information about when a UCDProperty was added to Unicode.
 */
public class UCDPropertyDetail {

    private static LinkedHashSet<UCDPropertyDetail> basePropertyDetails =
            new LinkedHashSet<UCDPropertyDetail>();
    private static LinkedHashSet<UCDPropertyDetail> cjkPropertyDetails =
            new LinkedHashSet<UCDPropertyDetail>();
    private static LinkedHashSet<UCDPropertyDetail> unikemetPropertyDetails =
            new LinkedHashSet<UCDPropertyDetail>();
    private static LinkedHashSet<UCDPropertyDetail> ucdxmlPropertyDetails =
            new LinkedHashSet<UCDPropertyDetail>();
    private static LinkedHashSet<UCDPropertyDetail> allPropertyDetails =
            new LinkedHashSet<UCDPropertyDetail>();

    public static UCDPropertyDetail Age_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Age,
                    VersionInfo.getInstance(3, 2, 0),
                    1,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Name_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Name,
                    VersionInfo.getInstance(1, 1, 0),
                    2,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Jamo_Short_Name_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Jamo_Short_Name,
                    VersionInfo.getInstance(5, 1, 0),
                    3,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail General_Category_Detail =
            new UCDPropertyDetail(
                    UcdProperty.General_Category,
                    VersionInfo.getInstance(1, 1, 0),
                    4,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Canonical_Combining_Class_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Canonical_Combining_Class,
                    VersionInfo.getInstance(1, 1, 0),
                    5,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Decomposition_Type_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Decomposition_Type,
                    VersionInfo.getInstance(1, 1, 0),
                    6,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Decomposition_Mapping_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Decomposition_Mapping,
                    VersionInfo.getInstance(1, 1, 0),
                    7,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Numeric_Type_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Numeric_Type,
                    VersionInfo.getInstance(1, 1, 0),
                    8,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Numeric_Value_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Numeric_Value,
                    VersionInfo.getInstance(1, 1, 0),
                    9,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Bidi_Class_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Bidi_Class,
                    VersionInfo.getInstance(1, 1, 0),
                    10,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Bidi_Paired_Bracket_Type_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Bidi_Paired_Bracket_Type,
                    VersionInfo.getInstance(6, 3, 0),
                    11,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Bidi_Paired_Bracket_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Bidi_Paired_Bracket,
                    VersionInfo.getInstance(6, 3, 0),
                    12,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Bidi_Mirrored_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Bidi_Mirrored,
                    VersionInfo.getInstance(1, 1, 0),
                    13,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Bidi_Mirroring_Glyph_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Bidi_Mirroring_Glyph,
                    VersionInfo.getInstance(3, 0, 1),
                    14,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Simple_Uppercase_Mapping_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Simple_Uppercase_Mapping,
                    VersionInfo.getInstance(1, 1, 0),
                    15,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Simple_Lowercase_Mapping_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Simple_Lowercase_Mapping,
                    VersionInfo.getInstance(1, 1, 0),
                    16,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Simple_Titlecase_Mapping_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Simple_Titlecase_Mapping,
                    VersionInfo.getInstance(1, 1, 0),
                    17,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Uppercase_Mapping_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Uppercase_Mapping,
                    VersionInfo.getInstance(2, 1, 8),
                    18,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Lowercase_Mapping_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Lowercase_Mapping,
                    VersionInfo.getInstance(2, 1, 8),
                    19,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Titlecase_Mapping_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Titlecase_Mapping,
                    VersionInfo.getInstance(2, 1, 8),
                    20,
                    true,
                    false,
                    false,
                    true,
                    false);
    //        public static UCDPropertyDetail Special_Case_Condition_Detail = new UCDPropertyDetail
    // (
    //            UcdProperty.Special_Case_Condition, VersionInfo.getInstance(1,1,0), 21,
    //            true, false, false, true, false);
    public static UCDPropertyDetail Simple_Case_Folding_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Simple_Case_Folding,
                    VersionInfo.getInstance(3, 0, 1),
                    22,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Case_Folding_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Case_Folding,
                    VersionInfo.getInstance(3, 0, 1),
                    23,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Joining_Type_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Joining_Type,
                    VersionInfo.getInstance(2, 0, 0),
                    24,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Joining_Group_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Joining_Group,
                    VersionInfo.getInstance(2, 0, 0),
                    25,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail East_Asian_Width_Detail =
            new UCDPropertyDetail(
                    UcdProperty.East_Asian_Width,
                    VersionInfo.getInstance(3, 0, 0),
                    26,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Line_Break_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Line_Break,
                    VersionInfo.getInstance(3, 0, 0),
                    27,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Script_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Script,
                    VersionInfo.getInstance(3, 1, 0),
                    28,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Script_Extensions_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Script_Extensions,
                    VersionInfo.getInstance(6, 1, 0),
                    29,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Dash_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Dash,
                    VersionInfo.getInstance(2, 0, 0),
                    30,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail White_Space_Detail =
            new UCDPropertyDetail(
                    UcdProperty.White_Space,
                    VersionInfo.getInstance(2, 0, 0),
                    31,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Hyphen_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Hyphen,
                    VersionInfo.getInstance(2, 0, 0),
                    VersionInfo.getInstance(16, 0, 0),
                    32,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Quotation_Mark_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Quotation_Mark,
                    VersionInfo.getInstance(2, 0, 0),
                    33,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Radical_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Radical,
                    VersionInfo.getInstance(3, 2, 0),
                    34,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Ideographic_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Ideographic,
                    VersionInfo.getInstance(2, 0, 0),
                    35,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Unified_Ideograph_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Unified_Ideograph,
                    VersionInfo.getInstance(3, 2, 0),
                    36,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail IDS_Binary_Operator_Detail =
            new UCDPropertyDetail(
                    UcdProperty.IDS_Binary_Operator,
                    VersionInfo.getInstance(3, 2, 0),
                    37,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail IDS_Trinary_Operator_Detail =
            new UCDPropertyDetail(
                    UcdProperty.IDS_Trinary_Operator,
                    VersionInfo.getInstance(3, 2, 0),
                    38,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Hangul_Syllable_Type_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Hangul_Syllable_Type,
                    VersionInfo.getInstance(4, 0, 0),
                    39,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Default_Ignorable_Code_Point_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Default_Ignorable_Code_Point,
                    VersionInfo.getInstance(3, 2, 0),
                    40,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Other_Default_Ignorable_Code_Point_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Other_Default_Ignorable_Code_Point,
                    VersionInfo.getInstance(3, 2, 0),
                    41,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Alphabetic_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Alphabetic,
                    VersionInfo.getInstance(1, 1, 0),
                    42,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Other_Alphabetic_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Other_Alphabetic,
                    VersionInfo.getInstance(3, 1, 0),
                    43,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Uppercase_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Uppercase,
                    VersionInfo.getInstance(3, 1, 0),
                    44,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Other_Uppercase_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Other_Uppercase,
                    VersionInfo.getInstance(3, 1, 0),
                    45,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Lowercase_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Lowercase,
                    VersionInfo.getInstance(3, 1, 0),
                    46,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Other_Lowercase_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Other_Lowercase,
                    VersionInfo.getInstance(3, 1, 0),
                    47,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Math_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Math,
                    VersionInfo.getInstance(2, 0, 0),
                    48,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Other_Math_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Other_Math,
                    VersionInfo.getInstance(3, 1, 0),
                    49,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Hex_Digit_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Hex_Digit,
                    VersionInfo.getInstance(2, 0, 0),
                    50,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail ASCII_Hex_Digit_Detail =
            new UCDPropertyDetail(
                    UcdProperty.ASCII_Hex_Digit,
                    VersionInfo.getInstance(3, 1, 1),
                    51,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Noncharacter_Code_Point_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Noncharacter_Code_Point,
                    VersionInfo.getInstance(3, 0, 1),
                    52,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Variation_Selector_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Variation_Selector,
                    VersionInfo.getInstance(4, 0, 1),
                    53,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Bidi_Control_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Bidi_Control,
                    VersionInfo.getInstance(2, 0, 0),
                    54,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Join_Control_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Join_Control,
                    VersionInfo.getInstance(2, 0, 0),
                    55,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Grapheme_Base_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Grapheme_Base,
                    VersionInfo.getInstance(3, 2, 0),
                    56,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Grapheme_Extend_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Grapheme_Extend,
                    VersionInfo.getInstance(3, 2, 0),
                    57,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Other_Grapheme_Extend_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Other_Grapheme_Extend,
                    VersionInfo.getInstance(3, 2, 0),
                    58,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Grapheme_Link_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Grapheme_Link,
                    VersionInfo.getInstance(3, 2, 0),
                    VersionInfo.getInstance(16, 0, 0),
                    59,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Sentence_Terminal_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Sentence_Terminal,
                    VersionInfo.getInstance(9, 0, 0),
                    60,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Extender_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Extender,
                    VersionInfo.getInstance(2, 0, 0),
                    61,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Terminal_Punctuation_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Terminal_Punctuation,
                    VersionInfo.getInstance(2, 0, 0),
                    62,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Diacritic_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Diacritic,
                    VersionInfo.getInstance(2, 0, 0),
                    63,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Deprecated_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Deprecated,
                    VersionInfo.getInstance(3, 2, 0),
                    64,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail ID_Start_Detail =
            new UCDPropertyDetail(
                    UcdProperty.ID_Start,
                    VersionInfo.getInstance(3, 1, 0),
                    65,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Other_ID_Start_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Other_ID_Start,
                    VersionInfo.getInstance(4, 0, 0),
                    66,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail XID_Start_Detail =
            new UCDPropertyDetail(
                    UcdProperty.XID_Start,
                    VersionInfo.getInstance(3, 1, 0),
                    67,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail ID_Continue_Detail =
            new UCDPropertyDetail(
                    UcdProperty.ID_Continue,
                    VersionInfo.getInstance(3, 1, 0),
                    68,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Other_ID_Continue_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Other_ID_Continue,
                    VersionInfo.getInstance(4, 1, 0),
                    69,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail XID_Continue_Detail =
            new UCDPropertyDetail(
                    UcdProperty.XID_Continue,
                    VersionInfo.getInstance(3, 1, 0),
                    70,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Soft_Dotted_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Soft_Dotted,
                    VersionInfo.getInstance(3, 2, 0),
                    71,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Logical_Order_Exception_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Logical_Order_Exception,
                    VersionInfo.getInstance(3, 2, 0),
                    72,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Pattern_White_Space_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Pattern_White_Space,
                    VersionInfo.getInstance(4, 1, 0),
                    73,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Pattern_Syntax_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Pattern_Syntax,
                    VersionInfo.getInstance(4, 1, 0),
                    74,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Grapheme_Cluster_Break_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Grapheme_Cluster_Break,
                    VersionInfo.getInstance(4, 1, 0),
                    75,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Word_Break_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Word_Break,
                    VersionInfo.getInstance(4, 1, 0),
                    76,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Sentence_Break_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Sentence_Break,
                    VersionInfo.getInstance(4, 1, 0),
                    77,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Composition_Exclusion_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Composition_Exclusion,
                    VersionInfo.getInstance(3, 0, 0),
                    78,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Full_Composition_Exclusion_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Full_Composition_Exclusion,
                    VersionInfo.getInstance(3, 1, 0),
                    79,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail NFC_Quick_Check_Detail =
            new UCDPropertyDetail(
                    UcdProperty.NFC_Quick_Check,
                    VersionInfo.getInstance(3, 2, 0),
                    80,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail NFD_Quick_Check_Detail =
            new UCDPropertyDetail(
                    UcdProperty.NFD_Quick_Check,
                    VersionInfo.getInstance(3, 2, 0),
                    81,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail NFKC_Quick_Check_Detail =
            new UCDPropertyDetail(
                    UcdProperty.NFKC_Quick_Check,
                    VersionInfo.getInstance(5, 2, 0),
                    82,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail NFKD_Quick_Check_Detail =
            new UCDPropertyDetail(
                    UcdProperty.NFKD_Quick_Check,
                    VersionInfo.getInstance(3, 2, 0),
                    83,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Expands_On_NFC_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Expands_On_NFC,
                    VersionInfo.getInstance(3, 2, 0),
                    VersionInfo.getInstance(16, 0, 0),
                    84,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Expands_On_NFD_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Expands_On_NFD,
                    VersionInfo.getInstance(3, 2, 0),
                    VersionInfo.getInstance(16, 0, 0),
                    85,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Expands_On_NFKC_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Expands_On_NFKC,
                    VersionInfo.getInstance(3, 2, 0),
                    VersionInfo.getInstance(16, 0, 0),
                    86,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Expands_On_NFKD_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Expands_On_NFKD,
                    VersionInfo.getInstance(3, 2, 0),
                    VersionInfo.getInstance(16, 0, 0),
                    87,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail FC_NFC_Closure_Detail =
            new UCDPropertyDetail(
                    UcdProperty.FC_NFKC_Closure,
                    VersionInfo.getInstance(3, 1, 0),
                    VersionInfo.getInstance(16, 0, 0),
                    88,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Case_Ignorable_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Case_Ignorable,
                    VersionInfo.getInstance(5, 2, 0),
                    89,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Cased_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Cased,
                    VersionInfo.getInstance(5, 2, 0),
                    90,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Changes_When_CaseFolded_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Changes_When_Casefolded,
                    VersionInfo.getInstance(5, 2, 0),
                    91,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Changes_When_CaseMapped_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Changes_When_Casemapped,
                    VersionInfo.getInstance(5, 2, 0),
                    92,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Changes_When_NFKC_Casefolded_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Changes_When_NFKC_Casefolded,
                    VersionInfo.getInstance(5, 2, 0),
                    93,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Changes_When_Lowercased_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Changes_When_Lowercased,
                    VersionInfo.getInstance(5, 2, 0),
                    94,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Changes_When_Titlecased_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Changes_When_Titlecased,
                    VersionInfo.getInstance(5, 2, 0),
                    95,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Changes_When_Uppercased_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Changes_When_Uppercased,
                    VersionInfo.getInstance(5, 2, 0),
                    96,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail NFKC_Casefold_Detail =
            new UCDPropertyDetail(
                    UcdProperty.NFKC_Casefold,
                    VersionInfo.getInstance(5, 2, 0),
                    97,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Indic_Syllabic_Category_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Indic_Syllabic_Category,
                    VersionInfo.getInstance(6, 1, 0),
                    98,
                    true,
                    false,
                    false,
                    true,
                    false);
    // Indic_Matra_Category was renamed to Indic_Positional_Category in Unicode 8.0
    //        public static UCDPropertyDetail Indic_Matra_Category_Detail = new UCDPropertyDetail (
    //            UcdProperty.Indic_Matra_Category, VersionInfo.getInstance(6,1,0),
    // VersionInfo.getInstance(7,0,0), 99,
    //            true, false, false, true, false);
    public static UCDPropertyDetail Indic_Positional_Category_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Indic_Positional_Category,
                    VersionInfo.getInstance(8, 0, 0),
                    100,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kJa_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kJa,
                    VersionInfo.getInstance(8, 0, 0),
                    VersionInfo.getInstance(16, 0, 0),
                    101,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Prepended_Concatenation_Mark_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Prepended_Concatenation_Mark,
                    VersionInfo.getInstance(9, 0, 0),
                    102,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Vertical_Orientation_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Vertical_Orientation,
                    VersionInfo.getInstance(10, 0, 0),
                    103,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Regional_Indicator_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Regional_Indicator,
                    VersionInfo.getInstance(10, 0, 0),
                    104,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Block_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Block,
                    VersionInfo.getInstance(2, 0, 0),
                    105,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Equivalent_Unified_Ideograph_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Equivalent_Unified_Ideograph,
                    VersionInfo.getInstance(11, 0, 0),
                    106,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kCompatibilityVariant_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kCompatibilityVariant,
                    VersionInfo.getInstance(3, 2, 0),
                    107,
                    false,
                    true,
                    true,
                    true,
                    false);
    public static UCDPropertyDetail kRSUnicode_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kRSUnicode,
                    VersionInfo.getInstance(2, 0, 0),
                    108,
                    false,
                    true,
                    false,
                    true,
                    false);
    //        public static UCDPropertyDetail kIRG_RSIndex_Detail = new UCDPropertyDetail (
    //            UcdProperty.kIRG_RSIndex, VersionInfo.getInstance(11,0,0), 109,
    //            false, true, false, true, false);
    public static UCDPropertyDetail kIRG_GSource_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kIRG_GSource,
                    VersionInfo.getInstance(3, 0, 0),
                    110,
                    false,
                    true,
                    true,
                    true,
                    false);
    public static UCDPropertyDetail kIRG_TSource_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kIRG_TSource,
                    VersionInfo.getInstance(3, 0, 0),
                    111,
                    false,
                    true,
                    true,
                    true,
                    false);
    public static UCDPropertyDetail kIRG_JSource_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kIRG_JSource,
                    VersionInfo.getInstance(3, 0, 0),
                    112,
                    false,
                    true,
                    true,
                    true,
                    false);
    public static UCDPropertyDetail kIRG_KSource_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kIRG_KSource,
                    VersionInfo.getInstance(3, 0, 0),
                    113,
                    false,
                    true,
                    true,
                    true,
                    false);
    public static UCDPropertyDetail kIRG_KPSource_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kIRG_KPSource,
                    VersionInfo.getInstance(3, 1, 1),
                    114,
                    false,
                    true,
                    true,
                    true,
                    false);
    public static UCDPropertyDetail kIRG_VSource_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kIRG_VSource,
                    VersionInfo.getInstance(3, 0, 0),
                    115,
                    false,
                    true,
                    true,
                    true,
                    false);
    public static UCDPropertyDetail kIRG_HSource_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kIRG_HSource,
                    VersionInfo.getInstance(3, 1, 0),
                    116,
                    false,
                    true,
                    true,
                    true,
                    false);
    public static UCDPropertyDetail kIRG_USource_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kIRG_USource,
                    VersionInfo.getInstance(4, 0, 1),
                    117,
                    false,
                    true,
                    true,
                    true,
                    false);
    public static UCDPropertyDetail kIRG_MSource_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kIRG_MSource,
                    VersionInfo.getInstance(5, 2, 0),
                    118,
                    false,
                    true,
                    true,
                    true,
                    false);
    public static UCDPropertyDetail kIRG_UKSource_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kIRG_UKSource,
                    VersionInfo.getInstance(13, 0, 0),
                    119,
                    false,
                    true,
                    true,
                    true,
                    false);
    public static UCDPropertyDetail kIRG_SSource_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kIRG_SSource,
                    VersionInfo.getInstance(13, 0, 0),
                    120,
                    false,
                    true,
                    true,
                    true,
                    false);
    public static UCDPropertyDetail kIICore_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kIICore,
                    VersionInfo.getInstance(4, 1, 0),
                    121,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kUnihanCore2020_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kUnihanCore2020,
                    VersionInfo.getInstance(13, 0, 0),
                    122,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kGB0_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kGB0,
                    VersionInfo.getInstance(2, 0, 0),
                    123,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kGB1_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kGB1,
                    VersionInfo.getInstance(2, 0, 0),
                    124,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kGB3_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kGB3,
                    VersionInfo.getInstance(2, 0, 0),
                    125,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kGB5_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kGB5,
                    VersionInfo.getInstance(2, 0, 0),
                    126,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kGB7_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kGB7,
                    VersionInfo.getInstance(2, 0, 0),
                    VersionInfo.getInstance(16, 0, 0),
                    127,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kGB8_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kGB8,
                    VersionInfo.getInstance(2, 0, 0),
                    128,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kCNS1986_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kCNS1986,
                    VersionInfo.getInstance(2, 0, 0),
                    129,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kCNS1992_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kCNS1992,
                    VersionInfo.getInstance(2, 0, 0),
                    130,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kJis0_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kJis0,
                    VersionInfo.getInstance(2, 0, 0),
                    131,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kJis1_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kJis1,
                    VersionInfo.getInstance(2, 0, 0),
                    132,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kJIS0213_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kJIS0213,
                    VersionInfo.getInstance(3, 1, 1),
                    133,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kKSC0_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kKSC0,
                    VersionInfo.getInstance(2, 0, 0),
                    VersionInfo.getInstance(15, 1, 0),
                    134,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kKSC1_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kKSC1,
                    VersionInfo.getInstance(2, 0, 0),
                    VersionInfo.getInstance(15, 1, 0),
                    135,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kKPS0_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kKPS0,
                    VersionInfo.getInstance(3, 1, 1),
                    VersionInfo.getInstance(15, 1, 0),
                    136,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kKPS1_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kKPS1,
                    VersionInfo.getInstance(3, 1, 1),
                    VersionInfo.getInstance(15, 1, 0),
                    137,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kHKSCS_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kHKSCS,
                    VersionInfo.getInstance(3, 1, 1),
                    VersionInfo.getInstance(15, 1, 0),
                    138,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kCantonese_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kCantonese,
                    VersionInfo.getInstance(2, 0, 0),
                    139,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kHangul_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kHangul,
                    VersionInfo.getInstance(5, 0, 0),
                    140,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kDefinition_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kDefinition,
                    VersionInfo.getInstance(2, 0, 0),
                    141,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kHanYu_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kHanYu,
                    VersionInfo.getInstance(2, 0, 0),
                    142,
                    false,
                    true,
                    false,
                    true,
                    false);
    //        public static UCDPropertyDetail kAlternateHanYu_Detail = new UCDPropertyDetail (
    //            UcdProperty.kAlternateHanYu, VersionInfo.getInstance(2,0,0),
    // VersionInfo.getInstance(3,1,1), 143,
    //            false, true, false, true, false);
    //
    //        The following UcdProperty values were added via PR1026 to support generating pre-5.2
    // Unihan; however,
    //        they weren't supported in previous versions of UCDXML.
    //        kAlternateJEF
    //        kJHJ
    //        kRSMerged
    public static UCDPropertyDetail kMandarin_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kMandarin,
                    VersionInfo.getInstance(2, 0, 0),
                    144,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kCihaiT_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kCihaiT,
                    VersionInfo.getInstance(3, 2, 0),
                    145,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kSBGY_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kSBGY,
                    VersionInfo.getInstance(3, 2, 0),
                    146,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kNelson_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kNelson,
                    VersionInfo.getInstance(2, 0, 0),
                    147,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kCowles_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kCowles,
                    VersionInfo.getInstance(3, 1, 1),
                    148,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kMatthews_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kMatthews,
                    VersionInfo.getInstance(2, 0, 0),
                    149,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kOtherNumeric_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kOtherNumeric,
                    VersionInfo.getInstance(3, 2, 0),
                    150,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kPhonetic_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kPhonetic,
                    VersionInfo.getInstance(3, 1, 0),
                    151,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kGSR_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kGSR,
                    VersionInfo.getInstance(4, 0, 1),
                    152,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kFenn_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kFenn,
                    VersionInfo.getInstance(3, 1, 1),
                    153,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kFennIndex_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kFennIndex,
                    VersionInfo.getInstance(4, 1, 0),
                    154,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kKarlgren_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kKarlgren,
                    VersionInfo.getInstance(3, 1, 1),
                    155,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kCangjie_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kCangjie,
                    VersionInfo.getInstance(3, 1, 1),
                    156,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kMeyerWempe_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kMeyerWempe,
                    VersionInfo.getInstance(3, 1, 0),
                    157,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kSimplifiedVariant_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kSimplifiedVariant,
                    VersionInfo.getInstance(2, 0, 0),
                    158,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kTraditionalVariant_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kTraditionalVariant,
                    VersionInfo.getInstance(2, 0, 0),
                    159,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kSpecializedSemanticVariant_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kSpecializedSemanticVariant,
                    VersionInfo.getInstance(2, 0, 0),
                    160,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kSemanticVariant_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kSemanticVariant,
                    VersionInfo.getInstance(2, 0, 0),
                    161,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kVietnamese_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kVietnamese,
                    VersionInfo.getInstance(3, 1, 1),
                    162,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kLau_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kLau,
                    VersionInfo.getInstance(3, 1, 1),
                    163,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kTang_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kTang,
                    VersionInfo.getInstance(2, 0, 0),
                    164,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kZVariant_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kZVariant,
                    VersionInfo.getInstance(2, 0, 0),
                    165,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kJapaneseKun_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kJapaneseKun,
                    VersionInfo.getInstance(2, 0, 0),
                    166,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kJapaneseOn_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kJapaneseOn,
                    VersionInfo.getInstance(2, 0, 0),
                    167,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kKangXi_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kKangXi,
                    VersionInfo.getInstance(2, 0, 0),
                    168,
                    false,
                    true,
                    false,
                    true,
                    false);
    //    public static UCDPropertyDetail kAlternateKangXi_Detail = new UCDPropertyDetail (
    //            UcdProperty.kAlternateKangXi, VersionInfo.getInstance(2,0,0),
    // VersionInfo.getInstance(4,0,1), 169,
    //            false, true, false, true, false);
    public static UCDPropertyDetail kBigFive_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kBigFive,
                    VersionInfo.getInstance(2, 0, 0),
                    170,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kCCCII_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kCCCII,
                    VersionInfo.getInstance(2, 0, 0),
                    171,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kDaeJaweon_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kDaeJaweon,
                    VersionInfo.getInstance(2, 0, 0),
                    172,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kEACC_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kEACC,
                    VersionInfo.getInstance(2, 0, 0),
                    173,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kFrequency_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kFrequency,
                    VersionInfo.getInstance(3, 2, 0),
                    VersionInfo.getInstance(16, 0, 0),
                    174,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kGradeLevel_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kGradeLevel,
                    VersionInfo.getInstance(3, 2, 0),
                    175,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kHDZRadBreak_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kHDZRadBreak,
                    VersionInfo.getInstance(4, 1, 0),
                    176,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kHKGlyph_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kHKGlyph,
                    VersionInfo.getInstance(3, 1, 1),
                    177,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kHanyuPinlu_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kHanyuPinlu,
                    VersionInfo.getInstance(4, 0, 1),
                    178,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kHanyuPinyin_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kHanyuPinyin,
                    VersionInfo.getInstance(5, 2, 0),
                    179,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kIRGHanyuDaZidian_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kIRGHanyuDaZidian,
                    VersionInfo.getInstance(3, 0, 0),
                    180,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kIRGKangXi_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kIRGKangXi,
                    VersionInfo.getInstance(3, 0, 0),
                    181,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kIRGDaeJaweon_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kIRGDaeJaweon,
                    VersionInfo.getInstance(3, 0, 0),
                    182,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kIRGDaiKanwaZiten_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kIRGDaiKanwaZiten,
                    VersionInfo.getInstance(3, 0, 0),
                    VersionInfo.getInstance(15, 1, 0),
                    183,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kKorean_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kKorean,
                    VersionInfo.getInstance(2, 0, 0),
                    184,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kMainlandTelegraph_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kMainlandTelegraph,
                    VersionInfo.getInstance(2, 0, 0),
                    185,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kMorohashi_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kMorohashi,
                    VersionInfo.getInstance(2, 0, 0),
                    186,
                    false,
                    true,
                    false,
                    true,
                    false);
    //    public static UCDPropertyDetail kAlternateMorohashi_Detail = new UCDPropertyDetail (
    //            UcdProperty.kAlternateMorohashi, VersionInfo.getInstance(2,0,0),
    // VersionInfo.getInstance(4,0,1), 187,
    //            false, true, false, true, false);
    public static UCDPropertyDetail kPrimaryNumeric_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kPrimaryNumeric,
                    VersionInfo.getInstance(3, 2, 0),
                    188,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kTaiwanTelegraph_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kTaiwanTelegraph,
                    VersionInfo.getInstance(2, 0, 0),
                    189,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kXerox_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kXerox,
                    VersionInfo.getInstance(2, 0, 0),
                    190,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kPseudoGB1_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kPseudoGB1,
                    VersionInfo.getInstance(2, 0, 0),
                    191,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kIBMJapan_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kIBMJapan,
                    VersionInfo.getInstance(2, 0, 0),
                    192,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kAccountingNumeric_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kAccountingNumeric,
                    VersionInfo.getInstance(3, 2, 0),
                    193,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kCheungBauer_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kCheungBauer,
                    VersionInfo.getInstance(5, 0, 0),
                    194,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kCheungBauerIndex_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kCheungBauerIndex,
                    VersionInfo.getInstance(5, 0, 0),
                    195,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kFourCornerCode_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kFourCornerCode,
                    VersionInfo.getInstance(5, 0, 0),
                    196,
                    false,
                    true,
                    false,
                    true,
                    false);
    //    public static UCDPropertyDetail kWubi_Detail = new UCDPropertyDetail (
    //            UcdProperty.kWubi, VersionInfo.getInstance(11,0,0), 197,
    //            false, true, false, true, false);
    public static UCDPropertyDetail kXHC1983_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kXHC1983,
                    VersionInfo.getInstance(5, 1, 0),
                    198,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kJinmeiyoKanji_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kJinmeiyoKanji,
                    VersionInfo.getInstance(11, 0, 0),
                    199,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kJoyoKanji_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kJoyoKanji,
                    VersionInfo.getInstance(11, 0, 0),
                    200,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kKoreanEducationHanja_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kKoreanEducationHanja,
                    VersionInfo.getInstance(11, 0, 0),
                    201,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kKoreanName_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kKoreanName,
                    VersionInfo.getInstance(11, 0, 0),
                    202,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kTGH_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kTGH,
                    VersionInfo.getInstance(11, 0, 0),
                    203,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kTGHZ2013_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kTGHZ2013,
                    VersionInfo.getInstance(13, 0, 0),
                    204,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kSpoofingVariant_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kSpoofingVariant,
                    VersionInfo.getInstance(13, 0, 0),
                    205,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kRSKanWa_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kRSKanWa,
                    VersionInfo.getInstance(2, 0, 0),
                    206,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kRSJapanese_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kRSJapanese,
                    VersionInfo.getInstance(2, 0, 0),
                    207,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kRSKorean_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kRSKorean,
                    VersionInfo.getInstance(2, 0, 0),
                    208,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kRSKangXi_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kRSKangXi,
                    VersionInfo.getInstance(2, 0, 0),
                    VersionInfo.getInstance(15, 1, 0),
                    209,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kRSAdobe_Japan1_6_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kRSAdobe_Japan1_6,
                    VersionInfo.getInstance(4, 1, 0),
                    210,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kTotalStrokes_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kTotalStrokes,
                    VersionInfo.getInstance(3, 1, 0),
                    211,
                    false,
                    true,
                    false,
                    true,
                    false);
    // kRSTUnicode_Detail was renamed to kTGT_RSUnicode_Detail in Unicode 17
    //    public static UCDPropertyDetail kRSTUnicode_Detail =
    //            new UCDPropertyDetail(
    //                    UcdProperty.kTGT_RSUnicode,
    //                    VersionInfo.getInstance(9, 0, 0),
    //                    VersionInfo.getInstance(16, 0, 0),
    //                    212,
    //                    false,
    //                    true,
    //                    false,
    //                    true,
    //                    false);
    public static UCDPropertyDetail kTGT_RSUnicode_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kTGT_RSUnicode,
                    VersionInfo.getInstance(17, 0, 0),
                    212,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kTGT_MergedSrc_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kTGT_MergedSrc,
                    VersionInfo.getInstance(9, 0, 0),
                    213,
                    false,
                    true,
                    false,
                    true,
                    false);
    // kSrc_NushuDuben_Detail was renamed to kNSHU_DubenSrc_Detail in Unicode 17
    //    public static UCDPropertyDetail kSrc_NushuDuben_Detail =
    //            new UCDPropertyDetail(
    //                    UcdProperty.kNSHU_DubenSrc,
    //                    VersionInfo.getInstance(10, 0, 0),
    //                    VersionInfo.getInstance(16, 0, 0),
    //                    214,
    //                    false,
    //                    true,
    //                    false,
    //                    true,
    //                    false);
    public static UCDPropertyDetail kNSHU_DubenSrc_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kNSHU_DubenSrc,
                    VersionInfo.getInstance(17, 0, 0),
                    214,
                    false,
                    true,
                    false,
                    true,
                    false);
    // kReading_Detail was renamed to kNSHU_Reading_Detail in Unicode 17
    //    public static UCDPropertyDetail kNSHU_Reading_Detail =
    //            new UCDPropertyDetail(
    //                    UcdProperty.kNSHU_Reading,
    //                    VersionInfo.getInstance(10, 0, 0),
    //                    VersionInfo.getInstance(16, 0, 0),
    //                    215,
    //                    false,
    //                    true,
    //                    false,
    //                    true,
    //                    false);
    public static UCDPropertyDetail kNSHU_Reading_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kNSHU_Reading,
                    VersionInfo.getInstance(17, 0, 0),
                    215,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail ISO_Comment_Detail =
            new UCDPropertyDetail(
                    UcdProperty.ISO_Comment,
                    VersionInfo.getInstance(11, 0, 0),
                    VersionInfo.getInstance(16, 0, 0),
                    216,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Unicode_1_Name_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Unicode_1_Name,
                    VersionInfo.getInstance(2, 0, 0),
                    217,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Name_Alias_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Name_Alias,
                    VersionInfo.getInstance(5, 0, 0),
                    218,
                    false,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Emoji_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Emoji,
                    VersionInfo.getInstance(13, 0, 0),
                    219,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Emoji_Presentation_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Emoji_Presentation,
                    VersionInfo.getInstance(13, 0, 0),
                    220,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Emoji_Modifier_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Emoji_Modifier,
                    VersionInfo.getInstance(13, 0, 0),
                    221,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Emoji_Modifier_Base_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Emoji_Modifier_Base,
                    VersionInfo.getInstance(13, 0, 0),
                    222,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Emoji_Component_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Emoji_Component,
                    VersionInfo.getInstance(13, 0, 0),
                    223,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Extended_Pictographic_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Extended_Pictographic,
                    VersionInfo.getInstance(13, 0, 0),
                    224,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kStrange_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kStrange,
                    VersionInfo.getInstance(14, 0, 0),
                    225,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kAlternateTotalStrokes_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kAlternateTotalStrokes,
                    VersionInfo.getInstance(15, 0, 0),
                    226,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail NFKC_Simple_Casefold_Detail =
            new UCDPropertyDetail(
                    UcdProperty.NFKC_Simple_Casefold,
                    VersionInfo.getInstance(15, 1, 0),
                    227,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail ID_Compat_Math_Start_Detail =
            new UCDPropertyDetail(
                    UcdProperty.ID_Compat_Math_Start,
                    VersionInfo.getInstance(15, 1, 0),
                    228,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail ID_Compat_Math_Continue_Detail =
            new UCDPropertyDetail(
                    UcdProperty.ID_Compat_Math_Continue,
                    VersionInfo.getInstance(15, 1, 0),
                    229,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail IDS_Unary_Operator_Detail =
            new UCDPropertyDetail(
                    UcdProperty.IDS_Unary_Operator,
                    VersionInfo.getInstance(15, 1, 0),
                    230,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kJapanese_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kJapanese,
                    VersionInfo.getInstance(15, 1, 0),
                    231,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kMojiJoho_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kMojiJoho,
                    VersionInfo.getInstance(15, 1, 0),
                    232,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kSMSZD2003Index_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kSMSZD2003Index,
                    VersionInfo.getInstance(15, 1, 0),
                    233,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kSMSZD2003Readings_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kSMSZD2003Readings,
                    VersionInfo.getInstance(15, 1, 0),
                    234,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kVietnameseNumeric_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kVietnameseNumeric,
                    VersionInfo.getInstance(15, 1, 0),
                    235,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kZhuang_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kZhuang,
                    VersionInfo.getInstance(16, 0, 0),
                    236,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kZhuangNumeric_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kZhuangNumeric,
                    VersionInfo.getInstance(15, 1, 0),
                    237,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Indic_Conjunct_Break_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Indic_Conjunct_Break,
                    VersionInfo.getInstance(15, 1, 0),
                    238,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail Modifier_Combining_Mark_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Modifier_Combining_Mark,
                    VersionInfo.getInstance(16, 0, 0),
                    239,
                    true,
                    false,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kFanqie_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kFanqie,
                    VersionInfo.getInstance(16, 0, 0),
                    240,
                    false,
                    true,
                    false,
                    true,
                    false);
    public static UCDPropertyDetail kEH_Cat_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kEH_Cat,
                    VersionInfo.getInstance(16, 0, 0),
                    241,
                    false,
                    false,
                    false,
                    false,
                    true);
    public static UCDPropertyDetail kEH_Core_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kEH_Core,
                    VersionInfo.getInstance(16, 0, 0),
                    242,
                    false,
                    false,
                    false,
                    false,
                    true);
    public static UCDPropertyDetail kEH_Desc_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kEH_Desc,
                    VersionInfo.getInstance(16, 0, 0),
                    243,
                    false,
                    false,
                    false,
                    false,
                    true);
    public static UCDPropertyDetail kEH_Func_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kEH_Func,
                    VersionInfo.getInstance(16, 0, 0),
                    244,
                    false,
                    false,
                    false,
                    false,
                    true);
    public static UCDPropertyDetail kEH_FVal_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kEH_FVal,
                    VersionInfo.getInstance(16, 0, 0),
                    245,
                    false,
                    false,
                    false,
                    false,
                    true);
    public static UCDPropertyDetail kEH_UniK_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kEH_UniK,
                    VersionInfo.getInstance(16, 0, 0),
                    246,
                    false,
                    false,
                    false,
                    false,
                    true);
    public static UCDPropertyDetail kEH_JSesh_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kEH_JSesh,
                    VersionInfo.getInstance(16, 0, 0),
                    247,
                    false,
                    false,
                    false,
                    false,
                    true);
    public static UCDPropertyDetail kEH_HG_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kEH_HG,
                    VersionInfo.getInstance(16, 0, 0),
                    248,
                    false,
                    false,
                    false,
                    false,
                    true);
    public static UCDPropertyDetail kEH_IFAO_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kEH_IFAO,
                    VersionInfo.getInstance(16, 0, 0),
                    249,
                    false,
                    false,
                    false,
                    false,
                    true);
    public static UCDPropertyDetail kEH_NoMirror_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kEH_NoMirror,
                    VersionInfo.getInstance(16, 0, 0),
                    250,
                    false,
                    false,
                    false,
                    false,
                    true);
    public static UCDPropertyDetail kEH_NoRotate_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kEH_NoRotate,
                    VersionInfo.getInstance(16, 0, 0),
                    251,
                    false,
                    false,
                    false,
                    false,
                    true);
    public static UCDPropertyDetail kEH_AltSeq_Detail =
            new UCDPropertyDetail(
                    UcdProperty.kEH_AltSeq,
                    VersionInfo.getInstance(17, 0, 0),
                    252,
                    false,
                    false,
                    false,
                    false,
                    true);
    public static UCDPropertyDetail kTayNumeric =
            new UCDPropertyDetail(
                    UcdProperty.kTayNumeric,
                    VersionInfo.getInstance(17, 0, 0),
                    253,
                    false,
                    true,
                    false,
                    false,
                    false);
    public static UCDPropertyDetail Basic_Emoji_Detail =
            new UCDPropertyDetail(UcdProperty.Basic_Emoji, -1, false, false, false, false, false);
    public static UCDPropertyDetail CJK_Radical_Detail =
            new UCDPropertyDetail(UcdProperty.CJK_Radical, -2, false, false, false, false, false);
    public static UCDPropertyDetail Confusable_MA_Detail =
            new UCDPropertyDetail(UcdProperty.Confusable_MA, -3, false, false, false, false, false);
    public static UCDPropertyDetail Confusable_ML_Detail =
            new UCDPropertyDetail(UcdProperty.Confusable_ML, -4, false, false, false, false, false);
    public static UCDPropertyDetail Confusable_SA_Detail =
            new UCDPropertyDetail(UcdProperty.Confusable_SA, -5, false, false, false, false, false);
    public static UCDPropertyDetail Confusable_SL_Detail =
            new UCDPropertyDetail(UcdProperty.Confusable_SL, -6, false, false, false, false, false);
    public static UCDPropertyDetail Do_Not_Emit_Preferred_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Do_Not_Emit_Preferred, -7, false, false, false, false, false);
    public static UCDPropertyDetail Do_Not_Emit_Type_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Do_Not_Emit_Type, -8, false, false, false, false, false);
    public static UCDPropertyDetail Emoji_DCM_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Emoji_DCM,
                    VersionInfo.getInstance(6, 0, 0),
                    -9,
                    false,
                    false,
                    false,
                    false,
                    false);
    public static UCDPropertyDetail Emoji_KDDI_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Emoji_KDDI,
                    VersionInfo.getInstance(6, 0, 0),
                    -10,
                    false,
                    false,
                    false,
                    false,
                    false);
    public static UCDPropertyDetail Emoji_SB_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Emoji_SB,
                    VersionInfo.getInstance(6, 0, 0),
                    -11,
                    false,
                    false,
                    false,
                    false,
                    false);
    public static UCDPropertyDetail Identifier_Status_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Identifier_Status,
                    VersionInfo.getInstance(9, 0, 0),
                    -12,
                    false,
                    false,
                    false,
                    false,
                    false);
    public static UCDPropertyDetail Identifier_Type_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Identifier_Type,
                    VersionInfo.getInstance(9, 0, 0),
                    -13,
                    false,
                    false,
                    false,
                    false,
                    false);
    public static UCDPropertyDetail Idn_2008_Detail =
            new UCDPropertyDetail(UcdProperty.Idn_2008, -14, false, false, false, false, false);
    public static UCDPropertyDetail Idn_Mapping_Detail =
            new UCDPropertyDetail(UcdProperty.Idn_Mapping, -15, false, false, false, false, false);
    public static UCDPropertyDetail Idn_Status_Detail =
            new UCDPropertyDetail(UcdProperty.Idn_Status, -16, false, false, false, false, false);
    public static UCDPropertyDetail Named_Sequences_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Named_Sequences, -17, false, false, false, false, false);
    public static UCDPropertyDetail Named_Sequences_Prov_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Named_Sequences_Prov, -18, false, false, false, false, false);
    public static UCDPropertyDetail Other_Joining_Type_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Other_Joining_Type, -19, false, false, false, false, false);
    public static UCDPropertyDetail RGI_Emoji_Flag_Sequence_Detail =
            new UCDPropertyDetail(
                    UcdProperty.RGI_Emoji_Flag_Sequence, -20, false, false, false, false, false);
    public static UCDPropertyDetail RGI_Emoji_Keycap_Sequence_Detail =
            new UCDPropertyDetail(
                    UcdProperty.RGI_Emoji_Keycap_Sequence, -21, false, false, false, false, false);
    public static UCDPropertyDetail RGI_Emoji_Modifier_Sequence_Detail =
            new UCDPropertyDetail(
                    UcdProperty.RGI_Emoji_Modifier_Sequence,
                    -22,
                    false,
                    false,
                    false,
                    false,
                    false);
    public static UCDPropertyDetail RGI_Emoji_Tag_Sequence_Detail =
            new UCDPropertyDetail(
                    UcdProperty.RGI_Emoji_Tag_Sequence, -23, false, false, false, false, false);
    public static UCDPropertyDetail RGI_Emoji_Zwj_Sequence_Detail =
            new UCDPropertyDetail(
                    UcdProperty.RGI_Emoji_Zwj_Sequence, -24, false, false, false, false, false);
    public static UCDPropertyDetail Standardized_Variant_Detail =
            new UCDPropertyDetail(
                    UcdProperty.Standardized_Variant, -25, false, false, false, false, false);

    private UcdProperty ucdProperty;
    private VersionInfo minVersion;
    private VersionInfo maxVersion;
    private int sortOrder;
    private boolean isBaseAttribute;
    private boolean isCJKAttribute;
    private boolean isCJKShowIfEmpty;
    private boolean isOrgUCDXMLAttribute;
    private boolean isUnikemetAttribute;

    private UCDPropertyDetail(
            UcdProperty ucdProperty,
            VersionInfo minVersion,
            int sortOrder,
            boolean isBaseAttribute,
            boolean isCJKAttribute,
            boolean isCJKShowIfEmpty,
            boolean isOrgUCDXMLAttribute,
            boolean isUnikemetAttribute) {
        this(
                ucdProperty,
                minVersion,
                null,
                sortOrder,
                isBaseAttribute,
                isCJKAttribute,
                isCJKShowIfEmpty,
                isOrgUCDXMLAttribute,
                isUnikemetAttribute);
    }

    private UCDPropertyDetail(
            UcdProperty ucdProperty,
            int sortOrder,
            boolean isBaseAttribute,
            boolean isCJKAttribute,
            boolean isCJKShowIfEmpty,
            boolean isOrgUCDXMLAttribute,
            boolean isUnikemetAttribute) {
        this(
                ucdProperty,
                null,
                null,
                sortOrder,
                isBaseAttribute,
                isCJKAttribute,
                isCJKShowIfEmpty,
                isOrgUCDXMLAttribute,
                isUnikemetAttribute);
    }

    private UCDPropertyDetail(
            UcdProperty ucdProperty,
            VersionInfo minVersion,
            VersionInfo maxVersion,
            int sortOrder,
            boolean isBaseAttribute,
            boolean isCJKAttribute,
            boolean isCJKShowIfEmpty,
            boolean isOrgUCDXMLAttribute,
            boolean isUnikemetAttribute) {
        this.ucdProperty = ucdProperty;
        this.minVersion = minVersion;
        this.maxVersion = maxVersion;
        this.sortOrder = sortOrder;
        this.isBaseAttribute = isBaseAttribute;
        this.isCJKAttribute = isCJKAttribute;
        this.isCJKShowIfEmpty = isCJKShowIfEmpty;
        this.isOrgUCDXMLAttribute = isOrgUCDXMLAttribute;
        this.isUnikemetAttribute = isUnikemetAttribute;

        allPropertyDetails.add(this);
        if (isBaseAttribute) {
            basePropertyDetails.add(this);
            ucdxmlPropertyDetails.add(this);
        }
        if (isCJKAttribute) {
            cjkPropertyDetails.add(this);
            ucdxmlPropertyDetails.add(this);
        }
        if (isUnikemetAttribute) {
            unikemetPropertyDetails.add(this);
            ucdxmlPropertyDetails.add(this);
        }
    }

    public static Set<UCDPropertyDetail> values() {
        return allPropertyDetails;
    }

    public static Set<UCDPropertyDetail> baseValues() {
        return basePropertyDetails;
    }

    public static Set<UCDPropertyDetail> cjkValues() {
        return cjkPropertyDetails;
    }

    public static Set<UCDPropertyDetail> ucdxmlValues() {
        return ucdxmlPropertyDetails;
    }

    public static Set<UCDPropertyDetail> unikemetValues() {
        return unikemetPropertyDetails;
    }

    public UcdProperty getUcdProperty() {
        return this.ucdProperty;
    }

    public VersionInfo getMinVersion() {
        return this.minVersion;
    }

    public VersionInfo getMaxVersion() {
        return this.maxVersion;
    }

    public boolean isBaseAttribute() {
        return this.isBaseAttribute;
    }

    public boolean isCJKAttribute() {
        return this.isCJKAttribute;
    }

    public boolean isCJKShowIfEmpty() {
        return this.isCJKShowIfEmpty;
    }

    public boolean isOrgUCDXMLAttribute() {
        return this.isOrgUCDXMLAttribute;
    }

    public boolean isUnikemetAttribute() {
        return this.isUnikemetAttribute;
    }
}
