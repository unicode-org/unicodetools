package org.unicode.xml;

import com.ibm.icu.util.VersionInfo;
import org.unicode.props.UcdProperty;

import java.util.LinkedHashSet;
import java.util.Set;

public class UcdPropertyDetail {

    static private LinkedHashSet<UcdPropertyDetail> basePropertyDetails = new LinkedHashSet<UcdPropertyDetail> ();
    static private LinkedHashSet<UcdPropertyDetail> cjkPropertyDetails = new LinkedHashSet<UcdPropertyDetail> ();
    static private LinkedHashSet<UcdPropertyDetail> ucdxmlPropertyDetails = new LinkedHashSet<UcdPropertyDetail> ();
    static private LinkedHashSet<UcdPropertyDetail> allPropertyDetails = new LinkedHashSet<UcdPropertyDetail> ();

    public static UcdPropertyDetail Age_Detail = new UcdPropertyDetail (
            UcdProperty.Age, VersionInfo.getInstance(1,1,0), 1,
            true, false, false, true);
    public static UcdPropertyDetail Name_Detail = new UcdPropertyDetail (
            UcdProperty.Name, VersionInfo.getInstance(1,1,0), 2,
            true, false, false, true);
    public static UcdPropertyDetail Jamo_Short_Name_Detail = new UcdPropertyDetail (
            UcdProperty.Jamo_Short_Name, VersionInfo.getInstance(1,1,0), 3,
            true, false, false, true);
    public static UcdPropertyDetail General_Category_Detail = new UcdPropertyDetail (
            UcdProperty.General_Category, VersionInfo.getInstance(1,1,0), 4,
            true, false, false, true);
    public static UcdPropertyDetail Canonical_Combining_Class_Detail = new UcdPropertyDetail (
            UcdProperty.Canonical_Combining_Class, VersionInfo.getInstance(1,1,0), 5,
            true, false, false, true);
    public static UcdPropertyDetail Decomposition_Type_Detail = new UcdPropertyDetail (
            UcdProperty.Decomposition_Type, VersionInfo.getInstance(1,1,0), 6,
            true, false, false, true);
    public static UcdPropertyDetail Decomposition_Mapping_Detail = new UcdPropertyDetail (
            UcdProperty.Decomposition_Mapping, VersionInfo.getInstance(1,1,0), 7,
            true, false, false, true);
    public static UcdPropertyDetail Numeric_Type_Detail = new UcdPropertyDetail (
            UcdProperty.Numeric_Type, VersionInfo.getInstance(1,1,0), 8,
            true, false, false, true);
    public static UcdPropertyDetail Numeric_Value_Detail = new UcdPropertyDetail (
            UcdProperty.Numeric_Value, VersionInfo.getInstance(1,1,0), 9,
            true, false, false, true);
    public static UcdPropertyDetail Bidi_Class_Detail = new UcdPropertyDetail (
            UcdProperty.Bidi_Class, VersionInfo.getInstance(1,1,0), 10,
            true, false, false, true);
    public static UcdPropertyDetail Bidi_Paired_Bracket_Type_Detail = new UcdPropertyDetail (
            UcdProperty.Bidi_Paired_Bracket_Type, VersionInfo.getInstance(6,3,0), 11,
            true, false, false, true);
    public static UcdPropertyDetail Bidi_Paired_Bracket_Detail = new UcdPropertyDetail (
            UcdProperty.Bidi_Paired_Bracket, VersionInfo.getInstance(6,3,0), 12,
            true, false, false, true);
    public static UcdPropertyDetail Bidi_Mirrored_Detail = new UcdPropertyDetail (
            UcdProperty.Bidi_Mirrored, VersionInfo.getInstance(1,1,0), 13,
            true, false, false, true);
    public static UcdPropertyDetail Bidi_Mirroring_Glyph_Detail = new UcdPropertyDetail (
            UcdProperty.Bidi_Mirroring_Glyph, VersionInfo.getInstance(1,1,0), 14,
            true, false, false, true);
    public static UcdPropertyDetail Simple_Uppercase_Mapping_Detail = new UcdPropertyDetail (
            UcdProperty.Simple_Uppercase_Mapping, VersionInfo.getInstance(1,1,0), 15,
            true, false, false, true);
    public static UcdPropertyDetail Simple_Lowercase_Mapping_Detail = new UcdPropertyDetail (
            UcdProperty.Simple_Lowercase_Mapping, VersionInfo.getInstance(1,1,0), 16,
            true, false, false, true);
    public static UcdPropertyDetail Simple_Titlecase_Mapping_Detail = new UcdPropertyDetail (
            UcdProperty.Simple_Titlecase_Mapping, VersionInfo.getInstance(1,1,0), 17,
            true, false, false, true);
    public static UcdPropertyDetail Uppercase_Mapping_Detail = new UcdPropertyDetail (
            UcdProperty.Uppercase_Mapping, VersionInfo.getInstance(1,1,0), 18,
            true, false, false, true);
    public static UcdPropertyDetail Lowercase_Mapping_Detail = new UcdPropertyDetail (
            UcdProperty.Lowercase_Mapping, VersionInfo.getInstance(1,1,0), 19,
            true, false, false, true);
    public static UcdPropertyDetail Titlecase_Mapping_Detail = new UcdPropertyDetail (
            UcdProperty.Titlecase_Mapping, VersionInfo.getInstance(1,1,0), 20,
            true, false, false, true);
//        public static UcdPropertyDetail Special_Case_Condition_Detail = new UcdPropertyDetail (
//            UcdProperty.Special_Case_Condition, VersionInfo.getInstance(1,1,0), 21,
//            true, false, false, true);
    public static UcdPropertyDetail Simple_Case_Folding_Detail = new UcdPropertyDetail (
            UcdProperty.Simple_Case_Folding, VersionInfo.getInstance(1,1,0), 22,
            true, false, false, true);
    public static UcdPropertyDetail Case_Folding_Detail = new UcdPropertyDetail (
            UcdProperty.Case_Folding, VersionInfo.getInstance(1,1,0), 23,
            true, false, false, true);
    public static UcdPropertyDetail Joining_Type_Detail = new UcdPropertyDetail (
            UcdProperty.Joining_Type, VersionInfo.getInstance(1,1,0), 24,
            true, false, false, true);
    public static UcdPropertyDetail Joining_Group_Detail = new UcdPropertyDetail (
            UcdProperty.Joining_Group, VersionInfo.getInstance(1,1,0), 25,
            true, false, false, true);
    public static UcdPropertyDetail East_Asian_Width_Detail = new UcdPropertyDetail (
            UcdProperty.East_Asian_Width, VersionInfo.getInstance(1,1,0), 26,
            true, false, false, true);
    public static UcdPropertyDetail Line_Break_Detail = new UcdPropertyDetail (
            UcdProperty.Line_Break, VersionInfo.getInstance(1,1,0), 27,
            true, false, false, true);
    public static UcdPropertyDetail Script_Detail = new UcdPropertyDetail (
            UcdProperty.Script, VersionInfo.getInstance(1,1,0), 28,
            true, false, false, true);
    public static UcdPropertyDetail Script_Extensions_Detail = new UcdPropertyDetail (
            UcdProperty.Script_Extensions, VersionInfo.getInstance(6,1,0), 29,
            true, false, false, true);
    public static UcdPropertyDetail Dash_Detail = new UcdPropertyDetail (
            UcdProperty.Dash, VersionInfo.getInstance(1,1,0), 30,
            true, false, false, true);
    public static UcdPropertyDetail White_Space_Detail = new UcdPropertyDetail (
            UcdProperty.White_Space, VersionInfo.getInstance(1,1,0), 31,
            true, false, false, true);
    public static UcdPropertyDetail Hyphen_Detail = new UcdPropertyDetail (
            UcdProperty.Hyphen, VersionInfo.getInstance(1,1,0), 32,
            true, false, false, true);
    public static UcdPropertyDetail Quotation_Mark_Detail = new UcdPropertyDetail (
            UcdProperty.Quotation_Mark, VersionInfo.getInstance(1,1,0), 33,
            true, false, false, true);
    public static UcdPropertyDetail Radical_Detail = new UcdPropertyDetail (
            UcdProperty.Radical, VersionInfo.getInstance(1,1,0), 34,
            true, false, false, true);
    public static UcdPropertyDetail Ideographic_Detail = new UcdPropertyDetail (
            UcdProperty.Ideographic, VersionInfo.getInstance(1,1,0), 35,
            true, false, false, true);
    public static UcdPropertyDetail Unified_Ideograph_Detail = new UcdPropertyDetail (
            UcdProperty.Unified_Ideograph, VersionInfo.getInstance(1,1,0), 36,
            true, false, false, true);
    public static UcdPropertyDetail IDS_Binary_Operator_Detail = new UcdPropertyDetail (
            UcdProperty.IDS_Binary_Operator, VersionInfo.getInstance(1,1,0), 37,
            true, false, false, true);
    public static UcdPropertyDetail IDS_Trinary_Operator_Detail = new UcdPropertyDetail (
            UcdProperty.IDS_Trinary_Operator, VersionInfo.getInstance(1,1,0), 38,
            true, false, false, true);
    public static UcdPropertyDetail Hangul_Syllable_Type_Detail = new UcdPropertyDetail (
            UcdProperty.Hangul_Syllable_Type, VersionInfo.getInstance(1,1,0), 39,
            true, false, false, true);
    public static UcdPropertyDetail Default_Ignorable_Code_Point_Detail = new UcdPropertyDetail (
            UcdProperty.Default_Ignorable_Code_Point, VersionInfo.getInstance(1,1,0), 40,
            true, false, false, true);
    public static UcdPropertyDetail Other_Default_Ignorable_Code_Point_Detail = new UcdPropertyDetail (
            UcdProperty.Other_Default_Ignorable_Code_Point, VersionInfo.getInstance(1,1,0), 41,
            true, false, false, true);
    public static UcdPropertyDetail Alphabetic_Detail = new UcdPropertyDetail (
            UcdProperty.Alphabetic, VersionInfo.getInstance(1,1,0), 42,
            true, false, false, true);
    public static UcdPropertyDetail Other_Alphabetic_Detail = new UcdPropertyDetail (
            UcdProperty.Other_Alphabetic, VersionInfo.getInstance(1,1,0), 43,
            true, false, false, true);
    public static UcdPropertyDetail Uppercase_Detail = new UcdPropertyDetail (
            UcdProperty.Uppercase, VersionInfo.getInstance(1,1,0), 44,
            true, false, false, true);
    public static UcdPropertyDetail Other_Uppercase_Detail = new UcdPropertyDetail (
            UcdProperty.Other_Uppercase, VersionInfo.getInstance(1,1,0), 45,
            true, false, false, true);
    public static UcdPropertyDetail Lowercase_Detail = new UcdPropertyDetail (
            UcdProperty.Lowercase, VersionInfo.getInstance(1,1,0), 46,
            true, false, false, true);
    public static UcdPropertyDetail Other_Lowercase_Detail = new UcdPropertyDetail (
            UcdProperty.Other_Lowercase, VersionInfo.getInstance(1,1,0), 47,
            true, false, false, true);
    public static UcdPropertyDetail Math_Detail = new UcdPropertyDetail (
            UcdProperty.Math, VersionInfo.getInstance(1,1,0), 48,
            true, false, false, true);
    public static UcdPropertyDetail Other_Math_Detail = new UcdPropertyDetail (
            UcdProperty.Other_Math, VersionInfo.getInstance(1,1,0), 49,
            true, false, false, true);
    public static UcdPropertyDetail Hex_Digit_Detail = new UcdPropertyDetail (
            UcdProperty.Hex_Digit, VersionInfo.getInstance(1,1,0), 50,
            true, false, false, true);
    public static UcdPropertyDetail ASCII_Hex_Digit_Detail = new UcdPropertyDetail (
            UcdProperty.ASCII_Hex_Digit, VersionInfo.getInstance(1,1,0), 51,
            true, false, false, true);
    public static UcdPropertyDetail Noncharacter_Code_Point_Detail = new UcdPropertyDetail (
            UcdProperty.Noncharacter_Code_Point, VersionInfo.getInstance(1,1,0), 52,
            true, false, false, true);
    public static UcdPropertyDetail Variation_Selector_Detail = new UcdPropertyDetail (
            UcdProperty.Variation_Selector, VersionInfo.getInstance(1,1,0), 53,
            true, false, false, true);
    public static UcdPropertyDetail Bidi_Control_Detail = new UcdPropertyDetail (
            UcdProperty.Bidi_Control, VersionInfo.getInstance(1,1,0), 54,
            true, false, false, true);
    public static UcdPropertyDetail Join_Control_Detail = new UcdPropertyDetail (
            UcdProperty.Join_Control, VersionInfo.getInstance(1,1,0), 55,
            true, false, false, true);
    public static UcdPropertyDetail Grapheme_Base_Detail = new UcdPropertyDetail (
            UcdProperty.Grapheme_Base, VersionInfo.getInstance(1,1,0), 56,
            true, false, false, true);
    public static UcdPropertyDetail Grapheme_Extend_Detail = new UcdPropertyDetail (
            UcdProperty.Grapheme_Extend, VersionInfo.getInstance(1,1,0), 57,
            true, false, false, true);
    public static UcdPropertyDetail Other_Grapheme_Extend_Detail = new UcdPropertyDetail (
            UcdProperty.Other_Grapheme_Extend, VersionInfo.getInstance(1,1,0), 58,
            true, false, false, true);
    public static UcdPropertyDetail Grapheme_Link_Detail = new UcdPropertyDetail (
            UcdProperty.Grapheme_Link, VersionInfo.getInstance(1,1,0), 59,
            true, false, false, true);
    public static UcdPropertyDetail Sentence_Terminal_Detail = new UcdPropertyDetail (
            UcdProperty.Sentence_Terminal, VersionInfo.getInstance(1,1,0), 60,
            true, false, false, true);
    public static UcdPropertyDetail Extender_Detail = new UcdPropertyDetail (
            UcdProperty.Extender, VersionInfo.getInstance(1,1,0), 61,
            true, false, false, true);
    public static UcdPropertyDetail Terminal_Punctuation_Detail = new UcdPropertyDetail (
            UcdProperty.Terminal_Punctuation, VersionInfo.getInstance(1,1,0), 62,
            true, false, false, true);
    public static UcdPropertyDetail Diacritic_Detail = new UcdPropertyDetail (
            UcdProperty.Diacritic, VersionInfo.getInstance(1,1,0), 63,
            true, false, false, true);
    public static UcdPropertyDetail Deprecated_Detail = new UcdPropertyDetail (
            UcdProperty.Deprecated, VersionInfo.getInstance(1,1,0), 64,
            true, false, false, true);
    public static UcdPropertyDetail ID_Start_Detail = new UcdPropertyDetail (
            UcdProperty.ID_Start, VersionInfo.getInstance(1,1,0), 65,
            true, false, false, true);
    public static UcdPropertyDetail Other_ID_Start_Detail = new UcdPropertyDetail (
            UcdProperty.Other_ID_Start, VersionInfo.getInstance(1,1,0), 66,
            true, false, false, true);
    public static UcdPropertyDetail XID_Start_Detail = new UcdPropertyDetail (
            UcdProperty.XID_Start, VersionInfo.getInstance(1,1,0), 67,
            true, false, false, true);
    public static UcdPropertyDetail ID_Continue_Detail = new UcdPropertyDetail (
            UcdProperty.ID_Continue, VersionInfo.getInstance(1,1,0), 68,
            true, false, false, true);
    public static UcdPropertyDetail Other_ID_Continue_Detail = new UcdPropertyDetail (
            UcdProperty.Other_ID_Continue, VersionInfo.getInstance(1,1,0), 69,
            true, false, false, true);
    public static UcdPropertyDetail XID_Continue_Detail = new UcdPropertyDetail (
            UcdProperty.XID_Continue, VersionInfo.getInstance(1,1,0), 70,
            true, false, false, true);
    public static UcdPropertyDetail Soft_Dotted_Detail = new UcdPropertyDetail (
            UcdProperty.Soft_Dotted, VersionInfo.getInstance(1,1,0), 71,
            true, false, false, true);
    public static UcdPropertyDetail Logical_Order_Exception_Detail = new UcdPropertyDetail (
            UcdProperty.Logical_Order_Exception, VersionInfo.getInstance(1,1,0), 72,
            true, false, false, true);
    public static UcdPropertyDetail Pattern_White_Space_Detail = new UcdPropertyDetail (
            UcdProperty.Pattern_White_Space, VersionInfo.getInstance(1,1,0), 73,
            true, false, false, true);
    public static UcdPropertyDetail Pattern_Syntax_Detail = new UcdPropertyDetail (
            UcdProperty.Pattern_Syntax, VersionInfo.getInstance(1,1,0), 74,
            true, false, false, true);
    public static UcdPropertyDetail Grapheme_Cluster_Break_Detail = new UcdPropertyDetail (
            UcdProperty.Grapheme_Cluster_Break, VersionInfo.getInstance(1,1,0), 75,
            true, false, false, true);
    public static UcdPropertyDetail Word_Break_Detail = new UcdPropertyDetail (
            UcdProperty.Word_Break, VersionInfo.getInstance(1,1,0), 76,
            true, false, false, true);
    public static UcdPropertyDetail Sentence_Break_Detail = new UcdPropertyDetail (
            UcdProperty.Sentence_Break, VersionInfo.getInstance(1,1,0), 77,
            true, false, false, true);
    public static UcdPropertyDetail Composition_Exclusion_Detail = new UcdPropertyDetail (
            UcdProperty.Composition_Exclusion, VersionInfo.getInstance(1,1,0), 78,
            true, false, false, true);
    public static UcdPropertyDetail Full_Composition_Exclusion_Detail = new UcdPropertyDetail (
            UcdProperty.Full_Composition_Exclusion, VersionInfo.getInstance(1,1,0), 79,
            true, false, false, true);
    public static UcdPropertyDetail NFC_Quick_Check_Detail = new UcdPropertyDetail (
            UcdProperty.NFC_Quick_Check, VersionInfo.getInstance(1,1,0), 80,
            true, false, false, true);
    public static UcdPropertyDetail NFD_Quick_Check_Detail = new UcdPropertyDetail (
            UcdProperty.NFD_Quick_Check, VersionInfo.getInstance(1,1,0), 81,
            true, false, false, true);
    public static UcdPropertyDetail NFKC_Quick_Check_Detail = new UcdPropertyDetail (
            UcdProperty.NFKC_Quick_Check, VersionInfo.getInstance(1,1,0), 82,
            true, false, false, true);
    public static UcdPropertyDetail NFKD_Quick_Check_Detail = new UcdPropertyDetail (
            UcdProperty.NFKD_Quick_Check, VersionInfo.getInstance(1,1,0), 83,
            true, false, false, true);
    public static UcdPropertyDetail Expands_On_NFC_Detail = new UcdPropertyDetail (
            UcdProperty.Expands_On_NFC, VersionInfo.getInstance(1,1,0), 84,
            true, false, false, true);
    public static UcdPropertyDetail Expands_On_NFD_Detail = new UcdPropertyDetail (
            UcdProperty.Expands_On_NFD, VersionInfo.getInstance(1,1,0), 85,
            true, false, false, true);
    public static UcdPropertyDetail Expands_On_NFKC_Detail = new UcdPropertyDetail (
            UcdProperty.Expands_On_NFKC, VersionInfo.getInstance(1,1,0), 86,
            true, false, false, true);
    public static UcdPropertyDetail Expands_On_NFKD_Detail = new UcdPropertyDetail (
            UcdProperty.Expands_On_NFKD, VersionInfo.getInstance(1,1,0), 87,
            true, false, false, true);
    public static UcdPropertyDetail FC_NFC_Closure_Detail = new UcdPropertyDetail (
            UcdProperty.FC_NFKC_Closure, VersionInfo.getInstance(1,1,0), 88,
            true, false, false, true);
    public static UcdPropertyDetail Case_Ignorable_Detail = new UcdPropertyDetail (
            UcdProperty.Case_Ignorable, VersionInfo.getInstance(5,2,0), 89,
            true, false, false, true);
    public static UcdPropertyDetail Cased_Detail = new UcdPropertyDetail (
            UcdProperty.Cased, VersionInfo.getInstance(5,2,0), 90,
            true, false, false, true);
    public static UcdPropertyDetail Changes_When_CaseFolded_Detail = new UcdPropertyDetail (
            UcdProperty.Changes_When_Casefolded, VersionInfo.getInstance(5,2,0), 91,
            true, false, false, true);
    public static UcdPropertyDetail Changes_When_CaseMapped_Detail = new UcdPropertyDetail (
            UcdProperty.Changes_When_Casemapped, VersionInfo.getInstance(5,2,0), 92,
            true, false, false, true);
    public static UcdPropertyDetail Changes_When_NFKC_Casefolded_Detail = new UcdPropertyDetail (
            UcdProperty.Changes_When_NFKC_Casefolded, VersionInfo.getInstance(5,2,0), 93,
            true, false, false, true);
    public static UcdPropertyDetail Changes_When_Lowercased_Detail = new UcdPropertyDetail (
            UcdProperty.Changes_When_Lowercased, VersionInfo.getInstance(5,2,0), 94,
            true, false, false, true);
    public static UcdPropertyDetail Changes_When_Titlecased_Detail = new UcdPropertyDetail (
            UcdProperty.Changes_When_Titlecased, VersionInfo.getInstance(5,2,0), 95,
            true, false, false, true);
    public static UcdPropertyDetail Changes_When_Uppercased_Detail = new UcdPropertyDetail (
            UcdProperty.Changes_When_Uppercased, VersionInfo.getInstance(5,2,0), 96,
            true, false, false, true);
    public static UcdPropertyDetail NFKC_Casefold_Detail = new UcdPropertyDetail (
            UcdProperty.NFKC_Casefold, VersionInfo.getInstance(5,2,0), 97,
            true, false, false, true);
    public static UcdPropertyDetail Indic_Syllabic_Category_Detail = new UcdPropertyDetail (
            UcdProperty.Indic_Syllabic_Category, VersionInfo.getInstance(6,0,0), 98,
            true, false, false, true);
//        public static UcdPropertyDetail Indic_Matra_Category_Detail = new UcdPropertyDetail (
//            UcdProperty.Indic_Matra_Category, VersionInfo.getInstance(6,0,0), VersionInfo.getInstance(7,0,0), 99,
//            true, false, false, true);
    public static UcdPropertyDetail Indic_Positional_Category_Detail = new UcdPropertyDetail (
            UcdProperty.Indic_Positional_Category, VersionInfo.getInstance(8,0,0), 100,
            true, false, false, true);
    public static UcdPropertyDetail kJa_Detail = new UcdPropertyDetail (
            UcdProperty.kJa, VersionInfo.getInstance(8,0,0), 101,
            false, true, false, true);
    public static UcdPropertyDetail Prepended_Concatenation_Mark_Detail = new UcdPropertyDetail (
            UcdProperty.Prepended_Concatenation_Mark, VersionInfo.getInstance(9,0,0), 102,
            true, false, false, true);
    public static UcdPropertyDetail Vertical_Orientation_Detail = new UcdPropertyDetail (
            UcdProperty.Vertical_Orientation, VersionInfo.getInstance(10,0,0), 103,
            true, false, false, true);
    public static UcdPropertyDetail Regional_Indicator_Detail = new UcdPropertyDetail (
            UcdProperty.Regional_Indicator, VersionInfo.getInstance(10,0,0), 104,
            true, false, false, true);
    public static UcdPropertyDetail Block_Detail = new UcdPropertyDetail (
            UcdProperty.Block, VersionInfo.getInstance(10,0,0), 105,
            true, false, false, true);
    public static UcdPropertyDetail Equivalent_Unified_Ideograph_Detail = new UcdPropertyDetail (
            UcdProperty.Equivalent_Unified_Ideograph, VersionInfo.getInstance(11,0,0), 106,
            false, true, false, true);
    public static UcdPropertyDetail kCompatibilityVariant_Detail = new UcdPropertyDetail (
            UcdProperty.kCompatibilityVariant, VersionInfo.getInstance(11,0,0), 107,
            false, true, true, true);
    public static UcdPropertyDetail kRSUnicode_Detail = new UcdPropertyDetail (
            UcdProperty.kRSUnicode, VersionInfo.getInstance(11,0,0), 108,
            false, true, false, true);
//        public static UcdPropertyDetail kIRG_RSIndex_Detail = new UcdPropertyDetail (
//            UcdProperty.kIRG_RSIndex, VersionInfo.getInstance(11,0,0), 109,
//            false, true, false, true);
    public static UcdPropertyDetail kIRG_GSource_Detail = new UcdPropertyDetail (
            UcdProperty.kIRG_GSource, VersionInfo.getInstance(11,0,0), 110,
            false, true, true, true);
    public static UcdPropertyDetail kIRG_TSource_Detail = new UcdPropertyDetail (
            UcdProperty.kIRG_TSource, VersionInfo.getInstance(11,0,0), 111,
            false, true, true, true);
    public static UcdPropertyDetail kIRG_JSource_Detail = new UcdPropertyDetail (
            UcdProperty.kIRG_JSource, VersionInfo.getInstance(11,0,0), 112,
            false, true, true, true);
    public static UcdPropertyDetail kIRG_KSource_Detail = new UcdPropertyDetail (
            UcdProperty.kIRG_KSource, VersionInfo.getInstance(11,0,0), 113,
            false, true, true, true);
    public static UcdPropertyDetail kIRG_KPSource_Detail = new UcdPropertyDetail (
            UcdProperty.kIRG_KPSource, VersionInfo.getInstance(11,0,0), 114,
            false, true, true, true);
    public static UcdPropertyDetail kIRG_VSource_Detail = new UcdPropertyDetail (
            UcdProperty.kIRG_VSource, VersionInfo.getInstance(11,0,0), 115,
            false, true, true, true);
    public static UcdPropertyDetail kIRG_HSource_Detail = new UcdPropertyDetail (
            UcdProperty.kIRG_HSource, VersionInfo.getInstance(11,0,0), 116,
            false, true, true, true);
    public static UcdPropertyDetail kIRG_USource_Detail = new UcdPropertyDetail (
            UcdProperty.kIRG_USource, VersionInfo.getInstance(11,0,0), 117,
            false, true, true, true);
    public static UcdPropertyDetail kIRG_MSource_Detail = new UcdPropertyDetail (
            UcdProperty.kIRG_MSource, VersionInfo.getInstance(11,0,0), 118,
            false, true, true, true);
    public static UcdPropertyDetail kIRG_UKSource_Detail = new UcdPropertyDetail (
            UcdProperty.kIRG_UKSource, VersionInfo.getInstance(13,0,0), 119,
            false, true, true, true);
    public static UcdPropertyDetail kIRG_SSource_Detail = new UcdPropertyDetail (
            UcdProperty.kIRG_SSource, VersionInfo.getInstance(13,0,0), 120,
            false, true, true, true);
    public static UcdPropertyDetail kIICore_Detail = new UcdPropertyDetail (
            UcdProperty.kIICore, VersionInfo.getInstance(11,0,0), 121,
            false, true, false, true);
    public static UcdPropertyDetail kUnihanCore2020_Detail = new UcdPropertyDetail (
            UcdProperty.kUnihanCore2020, VersionInfo.getInstance(11,0,0), 122,
            false, true, false, true);
    public static UcdPropertyDetail kGB0_Detail = new UcdPropertyDetail (
            UcdProperty.kGB0, VersionInfo.getInstance(11,0,0), 123,
            false, true, false, true);
    public static UcdPropertyDetail kGB1_Detail = new UcdPropertyDetail (
            UcdProperty.kGB1, VersionInfo.getInstance(11,0,0), 124,
            false, true, false, true);
    public static UcdPropertyDetail kGB3_Detail = new UcdPropertyDetail (
            UcdProperty.kGB3, VersionInfo.getInstance(11,0,0), 125,
            false, true, false, true);
    public static UcdPropertyDetail kGB5_Detail = new UcdPropertyDetail (
            UcdProperty.kGB5, VersionInfo.getInstance(11,0,0), 126,
            false, true, false, true);
    public static UcdPropertyDetail kGB7_Detail = new UcdPropertyDetail (
            UcdProperty.kGB7, VersionInfo.getInstance(11,0,0), 127,
            false, true, false, true);
    public static UcdPropertyDetail kGB8_Detail = new UcdPropertyDetail (
            UcdProperty.kGB8, VersionInfo.getInstance(11,0,0), 128,
            false, true, false, true);
    public static UcdPropertyDetail kCNS1986_Detail = new UcdPropertyDetail (
            UcdProperty.kCNS1986, VersionInfo.getInstance(11,0,0), 129,
            false, true, false, true);
    public static UcdPropertyDetail kCNS1992_Detail = new UcdPropertyDetail (
            UcdProperty.kCNS1992, VersionInfo.getInstance(11,0,0), 130,
            false, true, false, true);
    public static UcdPropertyDetail kJis0_Detail = new UcdPropertyDetail (
            UcdProperty.kJis0, VersionInfo.getInstance(11,0,0), 131,
            false, true, false, true);
    public static UcdPropertyDetail kJis1_Detail = new UcdPropertyDetail (
            UcdProperty.kJis1, VersionInfo.getInstance(11,0,0), 132,
            false, true, false, true);
    public static UcdPropertyDetail kJIS0213_Detail = new UcdPropertyDetail (
            UcdProperty.kJIS0213, VersionInfo.getInstance(11,0,0), 133,
            false, true, false, true);
    public static UcdPropertyDetail kKSC0_Detail = new UcdPropertyDetail (
            UcdProperty.kKSC0, VersionInfo.getInstance(11,0,0),
            VersionInfo.getInstance(15,1,0), 134,
            false, true, false, true);
    public static UcdPropertyDetail kKSC1_Detail = new UcdPropertyDetail (
            UcdProperty.kKSC1, VersionInfo.getInstance(11,0,0),
            VersionInfo.getInstance(15,1,0), 135,
            false, true, false, true);
    public static UcdPropertyDetail kKPS0_Detail = new UcdPropertyDetail (
            UcdProperty.kKPS0, VersionInfo.getInstance(11,0,0),
            VersionInfo.getInstance(15,1,0), 136,
            false, true, false, true);
    public static UcdPropertyDetail kKPS1_Detail = new UcdPropertyDetail (
            UcdProperty.kKPS1, VersionInfo.getInstance(11,0,0),
            VersionInfo.getInstance(15,1,0), 137,
            false, true, false, true);
    public static UcdPropertyDetail kHKSCS_Detail = new UcdPropertyDetail (
            UcdProperty.kHKSCS, VersionInfo.getInstance(11,0,0),
            VersionInfo.getInstance(15,1,0), 138,
            false, true, false, true);
    public static UcdPropertyDetail kCantonese_Detail = new UcdPropertyDetail (
            UcdProperty.kCantonese, VersionInfo.getInstance(11,0,0), 139,
            false, true, false, true);
    public static UcdPropertyDetail kHangul_Detail = new UcdPropertyDetail (
            UcdProperty.kHangul, VersionInfo.getInstance(11,0,0), 140,
            false, true, false, true);
    public static UcdPropertyDetail kDefinition_Detail = new UcdPropertyDetail (
            UcdProperty.kDefinition, VersionInfo.getInstance(11,0,0), 141,
            false, true, false, true);
    public static UcdPropertyDetail kHanYu_Detail = new UcdPropertyDetail (
            UcdProperty.kHanYu, VersionInfo.getInstance(11,0,0), 142,
            false, true, false, true);
//        public static UcdPropertyDetail kAlternateHanYu_Detail = new UcdPropertyDetail (
//            UcdProperty.kAlternateHanYu, VersionInfo.getInstance(11,0,0), 143,
//            false, true, false, true);
    public static UcdPropertyDetail kMandarin_Detail = new UcdPropertyDetail (
            UcdProperty.kMandarin, VersionInfo.getInstance(11,0,0), 144,
            false, true, false, true);
    public static UcdPropertyDetail kCihaiT_Detail = new UcdPropertyDetail (
            UcdProperty.kCihaiT, VersionInfo.getInstance(11,0,0), 145,
            false, true, false, true);
    public static UcdPropertyDetail kSBGY_Detail = new UcdPropertyDetail (
            UcdProperty.kSBGY, VersionInfo.getInstance(11,0,0), 146,
            false, true, false, true);
    public static UcdPropertyDetail kNelson_Detail = new UcdPropertyDetail (
            UcdProperty.kNelson, VersionInfo.getInstance(11,0,0), 147,
            false, true, false, true);
    public static UcdPropertyDetail kCowles_Detail = new UcdPropertyDetail (
            UcdProperty.kCowles, VersionInfo.getInstance(11,0,0), 148,
            false, true, false, true);
    public static UcdPropertyDetail kMatthews_Detail = new UcdPropertyDetail (
            UcdProperty.kMatthews, VersionInfo.getInstance(11,0,0), 149,
            false, true, false, true);
    public static UcdPropertyDetail kOtherNumeric_Detail = new UcdPropertyDetail (
            UcdProperty.kOtherNumeric, VersionInfo.getInstance(11,0,0), 150,
            false, true, false, true);
    public static UcdPropertyDetail kPhonetic_Detail = new UcdPropertyDetail (
            UcdProperty.kPhonetic, VersionInfo.getInstance(11,0,0), 151,
            false, true, false, true);
    public static UcdPropertyDetail kGSR_Detail = new UcdPropertyDetail (
            UcdProperty.kGSR, VersionInfo.getInstance(11,0,0), 152,
            false, true, false, true);
    public static UcdPropertyDetail kFenn_Detail = new UcdPropertyDetail (
            UcdProperty.kFenn, VersionInfo.getInstance(11,0,0), 153,
            false, true, false, true);
    public static UcdPropertyDetail kFennIndex_Detail = new UcdPropertyDetail (
            UcdProperty.kFennIndex, VersionInfo.getInstance(11,0,0), 154,
            false, true, false, true);
    public static UcdPropertyDetail kKarlgren_Detail = new UcdPropertyDetail (
            UcdProperty.kKarlgren, VersionInfo.getInstance(11,0,0), 155,
            false, true, false, true);
    public static UcdPropertyDetail kCangjie_Detail = new UcdPropertyDetail (
            UcdProperty.kCangjie, VersionInfo.getInstance(11,0,0), 156,
            false, true, false, true);
    public static UcdPropertyDetail kMeyerWempe_Detail = new UcdPropertyDetail (
            UcdProperty.kMeyerWempe, VersionInfo.getInstance(11,0,0), 157,
            false, true, false, true);
    public static UcdPropertyDetail kSimplifiedVariant_Detail = new UcdPropertyDetail (
            UcdProperty.kSimplifiedVariant, VersionInfo.getInstance(11,0,0), 158,
            false, true, false, true);
    public static UcdPropertyDetail kTraditionalVariant_Detail = new UcdPropertyDetail (
            UcdProperty.kTraditionalVariant, VersionInfo.getInstance(11,0,0), 159,
            false, true, false, true);
    public static UcdPropertyDetail kSpecializedSemanticVariant_Detail = new UcdPropertyDetail (
            UcdProperty.kSpecializedSemanticVariant, VersionInfo.getInstance(11,0,0), 160,
            false, true, false, true);
    public static UcdPropertyDetail kSemanticVariant_Detail = new UcdPropertyDetail (
            UcdProperty.kSemanticVariant, VersionInfo.getInstance(11,0,0), 161,
            false, true, false, true);
    public static UcdPropertyDetail kVietnamese_Detail = new UcdPropertyDetail (
            UcdProperty.kVietnamese, VersionInfo.getInstance(11,0,0), 162,
            false, true, false, true);
    public static UcdPropertyDetail kLau_Detail = new UcdPropertyDetail (
            UcdProperty.kLau, VersionInfo.getInstance(11,0,0), 163,
            false, true, false, true);
    public static UcdPropertyDetail kTang_Detail = new UcdPropertyDetail (
            UcdProperty.kTang, VersionInfo.getInstance(11,0,0), 164,
            false, true, false, true);
    public static UcdPropertyDetail kZVariant_Detail = new UcdPropertyDetail (
            UcdProperty.kZVariant, VersionInfo.getInstance(11,0,0), 165,
            false, true, false, true);
    public static UcdPropertyDetail kJapaneseKun_Detail = new UcdPropertyDetail (
            UcdProperty.kJapaneseKun, VersionInfo.getInstance(11,0,0), 166,
            false, true, false, true);
    public static UcdPropertyDetail kJapaneseOn_Detail = new UcdPropertyDetail (
            UcdProperty.kJapaneseOn, VersionInfo.getInstance(11,0,0), 167,
            false, true, false, true);
    public static UcdPropertyDetail kKangXi_Detail = new UcdPropertyDetail (
            UcdProperty.kKangXi, VersionInfo.getInstance(11,0,0), 168,
            false, true, false, true);
//    public static UcdPropertyDetail kAlternateKangXi_Detail = new UcdPropertyDetail (
//            UcdProperty.kAlternateKangXi, VersionInfo.getInstance(11,0,0), 169,
//            false, true, false, true);
    public static UcdPropertyDetail kBigFive_Detail = new UcdPropertyDetail (
            UcdProperty.kBigFive, VersionInfo.getInstance(11,0,0), 170,
            false, true, false, true);
    public static UcdPropertyDetail kCCCII_Detail = new UcdPropertyDetail (
            UcdProperty.kCCCII, VersionInfo.getInstance(11,0,0), 171,
            false, true, false, true);
    public static UcdPropertyDetail kDaeJaweon_Detail = new UcdPropertyDetail (
            UcdProperty.kDaeJaweon, VersionInfo.getInstance(11,0,0), 172,
            false, true, false, true);
    public static UcdPropertyDetail kEACC_Detail = new UcdPropertyDetail (
            UcdProperty.kEACC, VersionInfo.getInstance(11,0,0), 173,
            false, true, false, true);
    public static UcdPropertyDetail kFrequency_Detail = new UcdPropertyDetail (
            UcdProperty.kFrequency, VersionInfo.getInstance(11,0,0),
            VersionInfo.getInstance(16,0,0), 174,
            false, true, false, true);
    public static UcdPropertyDetail kGradeLevel_Detail = new UcdPropertyDetail (
            UcdProperty.kGradeLevel, VersionInfo.getInstance(11,0,0), 175,
            false, true, false, true);
    public static UcdPropertyDetail kHDZRadBreak_Detail = new UcdPropertyDetail (
            UcdProperty.kHDZRadBreak, VersionInfo.getInstance(11,0,0), 176,
            false, true, false, true);
    public static UcdPropertyDetail kHKGlyph_Detail = new UcdPropertyDetail (
            UcdProperty.kHKGlyph, VersionInfo.getInstance(11,0,0), 177,
            false, true, false, true);
    public static UcdPropertyDetail kHanyuPinlu_Detail = new UcdPropertyDetail (
            UcdProperty.kHanyuPinlu, VersionInfo.getInstance(11,0,0), 178,
            false, true, false, true);
    public static UcdPropertyDetail kHanyuPinyin_Detail = new UcdPropertyDetail (
            UcdProperty.kHanyuPinyin, VersionInfo.getInstance(11,0,0), 179,
            false, true, false, true);
    public static UcdPropertyDetail kIRGHanyuDaZidian_Detail = new UcdPropertyDetail (
            UcdProperty.kIRGHanyuDaZidian, VersionInfo.getInstance(11,0,0), 180,
            false, true, false, true);
    public static UcdPropertyDetail kIRGKangXi_Detail = new UcdPropertyDetail (
            UcdProperty.kIRGKangXi, VersionInfo.getInstance(11,0,0), 181,
            false, true, false, true);
    public static UcdPropertyDetail kIRGDaeJaweon_Detail = new UcdPropertyDetail (
            UcdProperty.kIRGDaeJaweon, VersionInfo.getInstance(11,0,0), 182,
            false, true, false, true);
    public static UcdPropertyDetail kIRGDaiKanwaZiten_Detail = new UcdPropertyDetail (
            UcdProperty.kIRGDaiKanwaZiten, VersionInfo.getInstance(11,0,0),
            VersionInfo.getInstance(15,1,0), 183,
            false, true, false, true);
    public static UcdPropertyDetail kKorean_Detail = new UcdPropertyDetail (
            UcdProperty.kKorean, VersionInfo.getInstance(11,0,0), 184,
            false, true, false, true);
    public static UcdPropertyDetail kMainlandTelegraph_Detail = new UcdPropertyDetail (
            UcdProperty.kMainlandTelegraph, VersionInfo.getInstance(11,0,0), 185,
            false, true, false, true);
    public static UcdPropertyDetail kMorohashi_Detail = new UcdPropertyDetail (
            UcdProperty.kMorohashi, VersionInfo.getInstance(11,0,0), 186,
            false, true, false, true);
//    public static UcdPropertyDetail kAlternateMorohashi_Detail = new UcdPropertyDetail (
//            UcdProperty.kAlternateMorohashi, VersionInfo.getInstance(11,0,0), 187,
//            false, true, false, true);
    public static UcdPropertyDetail kPrimaryNumeric_Detail = new UcdPropertyDetail (
            UcdProperty.kPrimaryNumeric, VersionInfo.getInstance(11,0,0), 188,
            false, true, false, true);
    public static UcdPropertyDetail kTaiwanTelegraph_Detail = new UcdPropertyDetail (
            UcdProperty.kTaiwanTelegraph, VersionInfo.getInstance(11,0,0), 189,
            false, true, false, true);
    public static UcdPropertyDetail kXerox_Detail = new UcdPropertyDetail (
            UcdProperty.kXerox, VersionInfo.getInstance(11,0,0), 190,
            false, true, false, true);
    public static UcdPropertyDetail kPseudoGB1_Detail = new UcdPropertyDetail (
            UcdProperty.kPseudoGB1, VersionInfo.getInstance(11,0,0), 191,
            false, true, false, true);
    public static UcdPropertyDetail kIBMJapan_Detail = new UcdPropertyDetail (
            UcdProperty.kIBMJapan, VersionInfo.getInstance(11,0,0), 192,
            false, true, false, true);
    public static UcdPropertyDetail kAccountingNumeric_Detail = new UcdPropertyDetail (
            UcdProperty.kAccountingNumeric, VersionInfo.getInstance(11,0,0), 193,
            false, true, false, true);
    public static UcdPropertyDetail kCheungBauer_Detail = new UcdPropertyDetail (
            UcdProperty.kCheungBauer, VersionInfo.getInstance(11,0,0), 194,
            false, true, false, true);
    public static UcdPropertyDetail kCheungBauerIndex_Detail = new UcdPropertyDetail (
            UcdProperty.kCheungBauerIndex, VersionInfo.getInstance(11,0,0), 195,
            false, true, false, true);
    public static UcdPropertyDetail kFourCornerCode_Detail = new UcdPropertyDetail (
            UcdProperty.kFourCornerCode, VersionInfo.getInstance(11,0,0), 196,
            false, true, false, true);
//    public static UcdPropertyDetail kWubi_Detail = new UcdPropertyDetail (
//            UcdProperty.kWubi, VersionInfo.getInstance(11,0,0), 197,
//            false, true, false, true);
    public static UcdPropertyDetail kXHC1983_Detail = new UcdPropertyDetail (
            UcdProperty.kXHC1983, VersionInfo.getInstance(11,0,0), 198,
            false, true, false, true);
    public static UcdPropertyDetail kJinmeiyoKanji_Detail = new UcdPropertyDetail (
            UcdProperty.kJinmeiyoKanji, VersionInfo.getInstance(11,0,0), 199,
            false, true, false, true);
    public static UcdPropertyDetail kJoyoKanji_Detail = new UcdPropertyDetail (
            UcdProperty.kJoyoKanji, VersionInfo.getInstance(11,0,0), 200,
            false, true, false, true);
    public static UcdPropertyDetail kKoreanEducationHanja_Detail = new UcdPropertyDetail (
            UcdProperty.kKoreanEducationHanja, VersionInfo.getInstance(11,0,0), 201,
            false, true, false, true);
    public static UcdPropertyDetail kKoreanName_Detail = new UcdPropertyDetail (
            UcdProperty.kKoreanName, VersionInfo.getInstance(11,0,0), 202,
            false, true, false, true);
    public static UcdPropertyDetail kTGH_Detail = new UcdPropertyDetail (
            UcdProperty.kTGH, VersionInfo.getInstance(11,0,0), 203,
            false, true, false, true);
    public static UcdPropertyDetail kTGHZ2013_Detail = new UcdPropertyDetail (
            UcdProperty.kTGHZ2013, VersionInfo.getInstance(11,0,0), 204,
            false, true, false, true);
    public static UcdPropertyDetail kSpoofingVariant_Detail = new UcdPropertyDetail (
            UcdProperty.kSpoofingVariant, VersionInfo.getInstance(11,0,0), 205,
            false, true, false, true);
    public static UcdPropertyDetail kRSKanWa_Detail = new UcdPropertyDetail (
            UcdProperty.kRSKanWa, VersionInfo.getInstance(11,0,0), 206,
            false, true, false, true);
    public static UcdPropertyDetail kRSJapanese_Detail = new UcdPropertyDetail (
            UcdProperty.kRSJapanese, VersionInfo.getInstance(11,0,0), 207,
            false, true, false, true);
    public static UcdPropertyDetail kRSKorean_Detail = new UcdPropertyDetail (
            UcdProperty.kRSKorean, VersionInfo.getInstance(11,0,0), 208,
            false, true, false, true);
    public static UcdPropertyDetail kRSKangXi_Detail = new UcdPropertyDetail (
            UcdProperty.kRSKangXi, VersionInfo.getInstance(11,0,0),
            VersionInfo.getInstance(15,1,0), 209,
            false, true, false, true);
    public static UcdPropertyDetail kRSAdobe_Japan1_6_Detail = new UcdPropertyDetail (
            UcdProperty.kRSAdobe_Japan1_6, VersionInfo.getInstance(11,0,0), 210,
            false, true, false, true);
    public static UcdPropertyDetail kTotalStrokes_Detail = new UcdPropertyDetail (
            UcdProperty.kTotalStrokes, VersionInfo.getInstance(11,0,0), 211,
            false, true, false, true);
    public static UcdPropertyDetail kRSTUnicode_Detail = new UcdPropertyDetail (
            UcdProperty.kRSTUnicode, VersionInfo.getInstance(9,0,0), 212,
            false, true, false, true);
    public static UcdPropertyDetail kTGT_MergedSrc_Detail = new UcdPropertyDetail (
            UcdProperty.kTGT_MergedSrc, VersionInfo.getInstance(9,0,0), 213,
            false, true, false, true);
    public static UcdPropertyDetail kSrc_NushuDuben_Detail = new UcdPropertyDetail (
            UcdProperty.kSrc_NushuDuben, VersionInfo.getInstance(10,0,0), 214,
            false, true, false, true);
    public static UcdPropertyDetail kReading_Detail = new UcdPropertyDetail (
            UcdProperty.kReading, VersionInfo.getInstance(10,0,0), 215,
            false, true, false, true);
    public static UcdPropertyDetail ISO_Comment_Detail = new UcdPropertyDetail (
            UcdProperty.ISO_Comment, VersionInfo.getInstance(11,0,0), 216,
            true, false, false, true);
    public static UcdPropertyDetail Unicode_1_Name_Detail = new UcdPropertyDetail (
            UcdProperty.Unicode_1_Name, VersionInfo.getInstance(11,0,0), 217,
            true, false, false, true);
    public static UcdPropertyDetail Name_Alias_Detail = new UcdPropertyDetail (
            UcdProperty.Name_Alias, VersionInfo.getInstance(11,0,0), 218,
            false, false, false, true);
    public static UcdPropertyDetail Emoji_Detail = new UcdPropertyDetail (
            UcdProperty.Emoji, VersionInfo.getInstance(13,0,0), 219,
            true, false, false, true);
    public static UcdPropertyDetail Emoji_Presentation_Detail = new UcdPropertyDetail (
            UcdProperty.Emoji_Presentation, VersionInfo.getInstance(13,0,0), 220,
            true, false, false, true);
    public static UcdPropertyDetail Emoji_Modifier_Detail = new UcdPropertyDetail (
            UcdProperty.Emoji_Modifier, VersionInfo.getInstance(13,0,0), 221,
            true, false, false, true);
    public static UcdPropertyDetail Emoji_Modifier_Base_Detail = new UcdPropertyDetail (
            UcdProperty.Emoji_Modifier_Base, VersionInfo.getInstance(13,0,0), 222,
            true, false, false, true);
    public static UcdPropertyDetail Emoji_Component_Detail = new UcdPropertyDetail (
            UcdProperty.Emoji_Component, VersionInfo.getInstance(13,0,0), 223,
            true, false, false, true);
    public static UcdPropertyDetail Extended_Pictographic_Detail = new UcdPropertyDetail (
            UcdProperty.Extended_Pictographic, VersionInfo.getInstance(13,0,0), 224,
            true, false, false, true);
    public static UcdPropertyDetail kStrange_Detail = new UcdPropertyDetail (
            UcdProperty.kStrange, VersionInfo.getInstance(14,0,0), 225,
            false, true, false, true);
    public static UcdPropertyDetail kAlternateTotalStrokes_Detail = new UcdPropertyDetail (
            UcdProperty.kAlternateTotalStrokes, VersionInfo.getInstance(15,0,0), 226,
            false, true, false, true);
    public static UcdPropertyDetail NFKC_Simple_Casefold_Detail = new UcdPropertyDetail (
            UcdProperty.NFKC_Simple_Casefold, VersionInfo.getInstance(15,1,0), 227,
            true, false, false, true);
    public static UcdPropertyDetail ID_Compat_Math_Start_Detail = new UcdPropertyDetail (
            UcdProperty.ID_Compat_Math_Start, VersionInfo.getInstance(15,1,0), 228,
            true, false, false, true);
    public static UcdPropertyDetail ID_Compat_Math_Continue_Detail = new UcdPropertyDetail (
            UcdProperty.ID_Compat_Math_Continue, VersionInfo.getInstance(15,1,0), 229,
            true, false, false, true);
    public static UcdPropertyDetail IDS_Unary_Operator_Detail = new UcdPropertyDetail (
            UcdProperty.IDS_Unary_Operator, VersionInfo.getInstance(15,1,0), 230,
            true, false, false, true);
    public static UcdPropertyDetail kJapanese_Detail = new UcdPropertyDetail (
            UcdProperty.kJapanese, VersionInfo.getInstance(15,1,0), 231,
            false, true, false, true);
    public static UcdPropertyDetail kMojiJoho_Detail = new UcdPropertyDetail (
            UcdProperty.kMojiJoho, VersionInfo.getInstance(15,1,0), 232,
            false, true, false, true);
    public static UcdPropertyDetail kSMSZD2003Index_Detail = new UcdPropertyDetail (
            UcdProperty.kSMSZD2003Index, VersionInfo.getInstance(15,1,0), 233,
            false, true, false, true);
    public static UcdPropertyDetail kSMSZD2003Readings_Detail = new UcdPropertyDetail (
            UcdProperty.kSMSZD2003Readings, VersionInfo.getInstance(15,1,0), 234,
            false, true, false, true);
    public static UcdPropertyDetail kVietnameseNumeric_Detail = new UcdPropertyDetail (
            UcdProperty.kVietnameseNumeric, VersionInfo.getInstance(15,1,0), 235,
            false, true, false, true);
    public static UcdPropertyDetail kZhuangNumeric_Detail = new UcdPropertyDetail (
            UcdProperty.kZhuangNumeric, VersionInfo.getInstance(15,1,0), 236,
            false, true, false, true);
    public static UcdPropertyDetail Indic_Conjunct_Break_Detail = new UcdPropertyDetail (
            UcdProperty.Indic_Conjunct_Break, VersionInfo.getInstance(15,1,0), 237,
            true, false, false, true);
    public static UcdPropertyDetail Modifier_Combining_Mark_Detail = new UcdPropertyDetail (
            UcdProperty.Modifier_Combining_Mark, VersionInfo.getInstance(16,0,0), 238,
            true, false, false, true);
    public static UcdPropertyDetail kFanqie_Detail = new UcdPropertyDetail (
            UcdProperty.kFanqie, VersionInfo.getInstance(16,0,0), 239,
            false, true, false, true);
    public static UcdPropertyDetail kZhuang_Detail = new UcdPropertyDetail (
            UcdProperty.kZhuang, VersionInfo.getInstance(16,0,0), 240,
            false, true, false, true);
    public static UcdPropertyDetail Basic_Emoji_Detail = new UcdPropertyDetail (
            UcdProperty.Basic_Emoji, -1,
            false, false, false, false);
    public static UcdPropertyDetail CJK_Radical_Detail = new UcdPropertyDetail (
            UcdProperty.CJK_Radical, -2,
            false, false, false, false);
    public static UcdPropertyDetail Confusable_MA_Detail = new UcdPropertyDetail (
            UcdProperty.Confusable_MA, -3,
            false, false, false, false);
    public static UcdPropertyDetail Confusable_ML_Detail = new UcdPropertyDetail (
            UcdProperty.Confusable_ML, -4,
            false, false, false, false);
    public static UcdPropertyDetail Confusable_SA_Detail = new UcdPropertyDetail (
            UcdProperty.Confusable_SA, -5,
            false, false, false, false);
    public static UcdPropertyDetail Confusable_SL_Detail = new UcdPropertyDetail (
            UcdProperty.Confusable_SL, -6,
            false, false, false, false);
    public static UcdPropertyDetail Do_Not_Emit_Preferred_Detail = new UcdPropertyDetail (
            UcdProperty.Do_Not_Emit_Preferred, -7,
            false, false, false, false);
    public static UcdPropertyDetail Do_Not_Emit_Type_Detail = new UcdPropertyDetail (
            UcdProperty.Do_Not_Emit_Type, -8,
            false, false, false, false);
    public static UcdPropertyDetail Emoji_DCM_Detail = new UcdPropertyDetail (
            UcdProperty.Emoji_DCM, VersionInfo.getInstance(6,0,0), -9,
            false, false, false, false);
    public static UcdPropertyDetail Emoji_KDDI_Detail = new UcdPropertyDetail (
            UcdProperty.Emoji_KDDI, VersionInfo.getInstance(6,0,0), -10,
            false, false, false, false);
    public static UcdPropertyDetail Emoji_SB_Detail = new UcdPropertyDetail (
            UcdProperty.Emoji_SB, VersionInfo.getInstance(6,0,0), -11,
            false, false, false, false);
    public static UcdPropertyDetail Identifier_Status_Detail = new UcdPropertyDetail (
            UcdProperty.Identifier_Status, VersionInfo.getInstance(9,0,0), -12,
            false, false, false, false);
    public static UcdPropertyDetail Identifier_Type_Detail = new UcdPropertyDetail (
            UcdProperty.Identifier_Type, VersionInfo.getInstance(9,0,0), -13,
            false, false, false, false);
    public static UcdPropertyDetail Idn_2008_Detail = new UcdPropertyDetail (
            UcdProperty.Idn_2008, -14,
            false, false, false, false);
    public static UcdPropertyDetail Idn_Mapping_Detail = new UcdPropertyDetail (
            UcdProperty.Idn_Mapping, -15,
            false, false, false, false);
    public static UcdPropertyDetail Idn_Status_Detail = new UcdPropertyDetail (
            UcdProperty.Idn_Status, -16,
            false, false, false, false);
    public static UcdPropertyDetail Named_Sequences_Detail = new UcdPropertyDetail (
            UcdProperty.Named_Sequences, -17,
            false, false, false, false);
    public static UcdPropertyDetail Named_Sequences_Prov_Detail = new UcdPropertyDetail (
            UcdProperty.Named_Sequences_Prov, -18,
            false, false, false, false);
    public static UcdPropertyDetail Other_Joining_Type_Detail = new UcdPropertyDetail (
            UcdProperty.Other_Joining_Type, -19,
            false, false, false, false);
    public static UcdPropertyDetail RGI_Emoji_Flag_Sequence_Detail = new UcdPropertyDetail (
            UcdProperty.RGI_Emoji_Flag_Sequence, -20,
            false, false, false, false);
    public static UcdPropertyDetail RGI_Emoji_Keycap_Sequence_Detail = new UcdPropertyDetail (
            UcdProperty.RGI_Emoji_Keycap_Sequence, -21,
            false, false, false, false);
    public static UcdPropertyDetail RGI_Emoji_Modifier_Sequence_Detail = new UcdPropertyDetail (
            UcdProperty.RGI_Emoji_Modifier_Sequence, -22,
            false, false, false, false);
    public static UcdPropertyDetail RGI_Emoji_Tag_Sequence_Detail = new UcdPropertyDetail (
            UcdProperty.RGI_Emoji_Tag_Sequence, -23,
            false, false, false, false);
    public static UcdPropertyDetail RGI_Emoji_Zwj_Sequence_Detail = new UcdPropertyDetail (
            UcdProperty.RGI_Emoji_Zwj_Sequence, -24,
            false, false, false, false);
    public static UcdPropertyDetail Standardized_Variant_Detail = new UcdPropertyDetail (
            UcdProperty.Standardized_Variant, -25,
            false, false, false, false);

    private UcdProperty ucdProperty;
    private VersionInfo minVersion;
    private VersionInfo maxVersion;
    private int sortOrder;
    private boolean isBaseAttribute;
    private boolean isCJKAttribute;
    private boolean isCJKShowIfEmpty;
    private boolean isOrgUCDXMLAttribute;

    private UcdPropertyDetail(
            UcdProperty ucdProperty,
            VersionInfo minVersion,
            int sortOrder,
            boolean isBaseAttribute,
            boolean isCJKAttribute,
            boolean isCJKShowIfEmpty,
            boolean isOrgUCDXMLAttribute) {
        this (
                ucdProperty, minVersion, null,
                sortOrder, isBaseAttribute, isCJKAttribute, isCJKShowIfEmpty, isOrgUCDXMLAttribute);
    }

    private UcdPropertyDetail(
            UcdProperty ucdProperty,
            int sortOrder,
            boolean isBaseAttribute,
            boolean isCJKAttribute,
            boolean isCJKShowIfEmpty,
            boolean isOrgUCDXMLAttribute) {
        this (
                ucdProperty, null, null,
                sortOrder, isBaseAttribute, isCJKAttribute, isCJKShowIfEmpty, isOrgUCDXMLAttribute);
    }

    private UcdPropertyDetail(
            UcdProperty ucdProperty,
            VersionInfo minVersion,
            VersionInfo maxVersion,
            int sortOrder,
            boolean isBaseAttribute,
            boolean isCJKAttribute,
            boolean isCJKShowIfEmpty,
            boolean isOrgUCDXMLAttribute) {
        this.ucdProperty = ucdProperty;
        this.minVersion = minVersion;
        this.maxVersion = maxVersion;
        this.sortOrder = sortOrder;
        this.isBaseAttribute = isBaseAttribute;
        this.isCJKAttribute = isCJKAttribute;
        this.isCJKShowIfEmpty = isCJKShowIfEmpty;
        this.isOrgUCDXMLAttribute = isOrgUCDXMLAttribute;

        allPropertyDetails.add(this);
        if(isBaseAttribute) {
            basePropertyDetails.add(this);
            ucdxmlPropertyDetails.add(this);
        }
        if(isCJKAttribute) {
            cjkPropertyDetails.add(this);
            ucdxmlPropertyDetails.add(this);
        }
    }

    public static Set<UcdPropertyDetail> values () {
        return allPropertyDetails;
    }
    public static Set<UcdPropertyDetail> baseValues () {
        return basePropertyDetails;
    }
    public static Set<UcdPropertyDetail> cjkValues () {
        return cjkPropertyDetails;
    }
    public static Set<UcdPropertyDetail> ucdxmlValues () {
        return ucdxmlPropertyDetails;
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
}