package org.unicode.props;

import java.util.EnumSet;
import java.util.Set;
import org.unicode.props.PropertyNames.NameMatcher;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.props.UcdPropertyValues.Bidi_Class_Values;
import org.unicode.props.UcdPropertyValues.Bidi_Paired_Bracket_Type_Values;
import org.unicode.props.UcdPropertyValues.Binary;
import org.unicode.props.UcdPropertyValues.Block_Values;
import org.unicode.props.UcdPropertyValues.Canonical_Combining_Class_Values;
import org.unicode.props.UcdPropertyValues.Decomposition_Type_Values;
import org.unicode.props.UcdPropertyValues.Do_Not_Emit_Type_Values;
import org.unicode.props.UcdPropertyValues.East_Asian_Width_Values;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.props.UcdPropertyValues.Grapheme_Cluster_Break_Values;
import org.unicode.props.UcdPropertyValues.Hangul_Syllable_Type_Values;
import org.unicode.props.UcdPropertyValues.Identifier_Status_Values;
import org.unicode.props.UcdPropertyValues.Identifier_Type_Values;
import org.unicode.props.UcdPropertyValues.Idn_2008_Values;
import org.unicode.props.UcdPropertyValues.Idn_Status_Values;
import org.unicode.props.UcdPropertyValues.Indic_Conjunct_Break_Values;
import org.unicode.props.UcdPropertyValues.Indic_Positional_Category_Values;
import org.unicode.props.UcdPropertyValues.Indic_Syllabic_Category_Values;
import org.unicode.props.UcdPropertyValues.Joining_Group_Values;
import org.unicode.props.UcdPropertyValues.Joining_Type_Values;
import org.unicode.props.UcdPropertyValues.Line_Break_Values;
import org.unicode.props.UcdPropertyValues.NFC_Quick_Check_Values;
import org.unicode.props.UcdPropertyValues.NFD_Quick_Check_Values;
import org.unicode.props.UcdPropertyValues.NFKC_Quick_Check_Values;
import org.unicode.props.UcdPropertyValues.NFKD_Quick_Check_Values;
import org.unicode.props.UcdPropertyValues.Numeric_Type_Values;
import org.unicode.props.UcdPropertyValues.Other_Joining_Type_Values;
import org.unicode.props.UcdPropertyValues.Script_Values;
import org.unicode.props.UcdPropertyValues.Sentence_Break_Values;
import org.unicode.props.UcdPropertyValues.Vertical_Orientation_Values;
import org.unicode.props.UcdPropertyValues.Word_Break_Values;

/**
 * Machine-generated file for properties, produced by GenerateEnums.java from PropertyAliases.txt
 * and ExtraPropertyAliases.txt. The ordering of properties is first by category, then alphabetical
 * (ASCII order).
 */
public enum UcdProperty {

    // Numeric
    Numeric_Value(PropertyType.Numeric, "nv"),
    kAccountingNumeric(PropertyType.Numeric, "cjkAccountingNumeric"),
    kOtherNumeric(PropertyType.Numeric, "cjkOtherNumeric"),
    kPrimaryNumeric(PropertyType.Numeric, "cjkPrimaryNumeric"),

    // String
    Bidi_Mirroring_Glyph(PropertyType.String, "bmg"),
    Bidi_Paired_Bracket(PropertyType.String, "bpb"),
    Case_Folding(PropertyType.String, "cf"),
    Confusable_MA(PropertyType.String, "ConfMA", "Confusable"),
    Confusable_ML(PropertyType.String, "ConfML"),
    Confusable_SA(PropertyType.String, "ConfSA"),
    Confusable_SL(PropertyType.String, "ConfSL"),
    Decomposition_Mapping(PropertyType.String, "dm"),
    Do_Not_Emit_Preferred(PropertyType.String, "Do_Not_Emit_Preferred"),
    Equivalent_Unified_Ideograph(PropertyType.String, "EqUIdeo"),
    FC_NFKC_Closure(PropertyType.String, "FC_NFKC"),
    Idn_Mapping(PropertyType.String, "idnm"),
    Lowercase_Mapping(PropertyType.String, "lc"),
    NFKC_Casefold(PropertyType.String, "NFKC_CF"),
    NFKC_Simple_Casefold(PropertyType.String, "NFKC_SCF"),
    Simple_Case_Folding(PropertyType.String, "scf", "sfc"),
    Simple_Lowercase_Mapping(PropertyType.String, "slc"),
    Simple_Titlecase_Mapping(PropertyType.String, "stc"),
    Simple_Uppercase_Mapping(PropertyType.String, "suc"),
    Titlecase_Mapping(PropertyType.String, "tc"),
    Uppercase_Mapping(PropertyType.String, "uc"),
    kCompatibilityVariant(PropertyType.String, "cjkCompatibilityVariant"),
    kSimplifiedVariant(
            PropertyType.String, null, ValueCardinality.Unordered, "cjkSimplifiedVariant"),
    kTraditionalVariant(
            PropertyType.String, null, ValueCardinality.Unordered, "cjkTraditionalVariant"),

    // Miscellaneous
    CJK_Radical(PropertyType.Miscellaneous, null, ValueCardinality.Ordered, "CJKR"),
    Emoji_DCM(PropertyType.Miscellaneous, "EDCM"),
    Emoji_KDDI(PropertyType.Miscellaneous, "EKDDI"),
    Emoji_SB(PropertyType.Miscellaneous, "ESB"),
    ISO_Comment(PropertyType.Miscellaneous, "isc"),
    Jamo_Short_Name(PropertyType.Miscellaneous, "JSN"),
    Name(PropertyType.Miscellaneous, "na"),
    Name_Alias(PropertyType.Miscellaneous, null, ValueCardinality.Unordered, "Name_Alias"),
    Named_Sequences(PropertyType.Miscellaneous, "NS"),
    Named_Sequences_Prov(PropertyType.Miscellaneous, "NSP"),
    Standardized_Variant(PropertyType.Miscellaneous, null, ValueCardinality.Unordered, "SV"),
    Unicode_1_Name(PropertyType.Miscellaneous, "na1"),
    kAlternateTotalStrokes(PropertyType.Miscellaneous, "cjkAlternateTotalStrokes"),
    kBigFive(PropertyType.Miscellaneous, "cjkBigFive"),
    kCCCII(PropertyType.Miscellaneous, "cjkCCCII"),
    kCNS1986(PropertyType.Miscellaneous, "cjkCNS1986"),
    kCNS1992(PropertyType.Miscellaneous, "cjkCNS1992"),
    kCangjie(PropertyType.Miscellaneous, "cjkCangjie"),
    kCantonese(PropertyType.Miscellaneous, null, ValueCardinality.Unordered, "cjkCantonese"),
    kCheungBauer(PropertyType.Miscellaneous, "cjkCheungBauer"),
    kCheungBauerIndex(
            PropertyType.Miscellaneous, null, ValueCardinality.Unordered, "cjkCheungBauerIndex"),
    kCihaiT(PropertyType.Miscellaneous, null, ValueCardinality.Unordered, "cjkCihaiT"),
    kCowles(PropertyType.Miscellaneous, null, ValueCardinality.Unordered, "cjkCowles"),
    kDaeJaweon(PropertyType.Miscellaneous, "cjkDaeJaweon"),
    kDefinition(PropertyType.Miscellaneous, "cjkDefinition"),
    kEACC(PropertyType.Miscellaneous, "cjkEACC"),
    kFanqie(PropertyType.Miscellaneous, "cjkFanqie"),
    kFenn(PropertyType.Miscellaneous, null, ValueCardinality.Unordered, "cjkFenn"),
    kFennIndex(PropertyType.Miscellaneous, null, ValueCardinality.Unordered, "cjkFennIndex"),
    kFourCornerCode(
            PropertyType.Miscellaneous, null, ValueCardinality.Unordered, "cjkFourCornerCode"),
    kFrequency(PropertyType.Miscellaneous, "cjkFrequency"),
    kGB0(PropertyType.Miscellaneous, "cjkGB0"),
    kGB1(PropertyType.Miscellaneous, "cjkGB1"),
    kGB3(PropertyType.Miscellaneous, "cjkGB3"),
    kGB5(PropertyType.Miscellaneous, "cjkGB5"),
    kGB7(PropertyType.Miscellaneous, "cjkGB7"),
    kGB8(PropertyType.Miscellaneous, "cjkGB8"),
    kGSR(PropertyType.Miscellaneous, null, ValueCardinality.Unordered, "cjkGSR"),
    kGradeLevel(PropertyType.Miscellaneous, "cjkGradeLevel"),
    kHDZRadBreak(PropertyType.Miscellaneous, "cjkHDZRadBreak"),
    kHKGlyph(PropertyType.Miscellaneous, null, ValueCardinality.Unordered, "cjkHKGlyph"),
    kHKSCS(PropertyType.Miscellaneous, "cjkHKSCS"),
    kHanYu(PropertyType.Miscellaneous, null, ValueCardinality.Unordered, "cjkHanYu"),
    kHangul(PropertyType.Miscellaneous, null, ValueCardinality.Unordered, "cjkHangul"),
    kHanyuPinlu(PropertyType.Miscellaneous, null, ValueCardinality.Unordered, "cjkHanyuPinlu"),
    kHanyuPinyin(PropertyType.Miscellaneous, null, ValueCardinality.Unordered, "cjkHanyuPinyin"),
    kIBMJapan(PropertyType.Miscellaneous, "cjkIBMJapan"),
    kIICore(PropertyType.Miscellaneous, "cjkIICore"),
    kIRGDaeJaweon(PropertyType.Miscellaneous, "cjkIRGDaeJaweon"),
    kIRGDaiKanwaZiten(PropertyType.Miscellaneous, "cjkIRGDaiKanwaZiten"),
    kIRGHanyuDaZidian(PropertyType.Miscellaneous, "cjkIRGHanyuDaZidian"),
    kIRGKangXi(PropertyType.Miscellaneous, "cjkIRGKangXi"),
    kIRG_GSource(PropertyType.Miscellaneous, "cjkIRG_GSource"),
    kIRG_HSource(PropertyType.Miscellaneous, "cjkIRG_HSource"),
    kIRG_JSource(PropertyType.Miscellaneous, "cjkIRG_JSource"),
    kIRG_KPSource(PropertyType.Miscellaneous, "cjkIRG_KPSource"),
    kIRG_KSource(PropertyType.Miscellaneous, "cjkIRG_KSource"),
    kIRG_MSource(PropertyType.Miscellaneous, "cjkIRG_MSource"),
    kIRG_SSource(PropertyType.Miscellaneous, "cjkIRG_SSource"),
    kIRG_TSource(PropertyType.Miscellaneous, "cjkIRG_TSource"),
    kIRG_UKSource(PropertyType.Miscellaneous, "cjkIRG_UKSource"),
    kIRG_USource(PropertyType.Miscellaneous, "cjkIRG_USource"),
    kIRG_VSource(PropertyType.Miscellaneous, "cjkIRG_VSource"),
    kJIS0213(PropertyType.Miscellaneous, "cjkJIS0213"),
    kJa(PropertyType.Miscellaneous, "cjkJa"),
    kJapanese(PropertyType.Miscellaneous, "cjkJapanese"),
    kJapaneseKun(PropertyType.Miscellaneous, null, ValueCardinality.Unordered, "cjkJapaneseKun"),
    kJapaneseOn(PropertyType.Miscellaneous, null, ValueCardinality.Unordered, "cjkJapaneseOn"),
    kJinmeiyoKanji(
            PropertyType.Miscellaneous, null, ValueCardinality.Unordered, "cjkJinmeiyoKanji"),
    kJis0(PropertyType.Miscellaneous, "cjkJis0"),
    kJis1(PropertyType.Miscellaneous, "cjkJis1"),
    kJoyoKanji(PropertyType.Miscellaneous, null, ValueCardinality.Unordered, "cjkJoyoKanji"),
    kKPS0(PropertyType.Miscellaneous, "cjkKPS0"),
    kKPS1(PropertyType.Miscellaneous, "cjkKPS1"),
    kKSC0(PropertyType.Miscellaneous, "cjkKSC0"),
    kKSC1(PropertyType.Miscellaneous, "cjkKSC1"),
    kKangXi(PropertyType.Miscellaneous, "cjkKangXi"),
    kKarlgren(PropertyType.Miscellaneous, "cjkKarlgren"),
    kKorean(PropertyType.Miscellaneous, null, ValueCardinality.Unordered, "cjkKorean"),
    kKoreanEducationHanja(
            PropertyType.Miscellaneous,
            null,
            ValueCardinality.Unordered,
            "cjkKoreanEducationHanja"),
    kKoreanName(PropertyType.Miscellaneous, null, ValueCardinality.Unordered, "cjkKoreanName"),
    kLau(PropertyType.Miscellaneous, null, ValueCardinality.Unordered, "cjkLau"),
    kMainlandTelegraph(PropertyType.Miscellaneous, "cjkMainlandTelegraph"),
    kMandarin(PropertyType.Miscellaneous, null, ValueCardinality.Ordered, "cjkMandarin"),
    kMatthews(PropertyType.Miscellaneous, "cjkMatthews"),
    kMeyerWempe(PropertyType.Miscellaneous, null, ValueCardinality.Unordered, "cjkMeyerWempe"),
    kMojiJoho(PropertyType.Miscellaneous, "cjkMojiJoho"),
    kMorohashi(PropertyType.Miscellaneous, "cjkMorohashi"),
    kNelson(PropertyType.Miscellaneous, null, ValueCardinality.Unordered, "cjkNelson"),
    kPhonetic(PropertyType.Miscellaneous, null, ValueCardinality.Unordered, "cjkPhonetic"),
    kPseudoGB1(PropertyType.Miscellaneous, "cjkPseudoGB1"),
    kRSAdobe_Japan1_6(
            PropertyType.Miscellaneous, null, ValueCardinality.Unordered, "cjkRSAdobe_Japan1_6"),
    kRSJapanese(PropertyType.Miscellaneous, "cjkRSJapanese"),
    kRSKanWa(PropertyType.Miscellaneous, "cjkRSKanWa"),
    kRSKangXi(PropertyType.Miscellaneous, "cjkRSKangXi"),
    kRSKorean(PropertyType.Miscellaneous, "cjkRSKorean"),
    kRSUnicode(
            PropertyType.Miscellaneous,
            null,
            ValueCardinality.Ordered,
            "cjkRSUnicode",
            "Unicode_Radical_Stroke",
            "URS"),
    kSBGY(PropertyType.Miscellaneous, null, ValueCardinality.Unordered, "cjkSBGY"),
    kSMSZD2003Index(PropertyType.Miscellaneous, "cjkSMSZD2003Index"),
    kSMSZD2003Readings(PropertyType.Miscellaneous, "cjkSMSZD2003Readings"),
    kSemanticVariant(
            PropertyType.Miscellaneous, null, ValueCardinality.Unordered, "cjkSemanticVariant"),
    kSpecializedSemanticVariant(
            PropertyType.Miscellaneous,
            null,
            ValueCardinality.Unordered,
            "cjkSpecializedSemanticVariant"),
    kSpoofingVariant(PropertyType.Miscellaneous, "cjkSpoofingVariant"),
    kStrange(PropertyType.Miscellaneous, "cjkStrange"),
    kTGH(PropertyType.Miscellaneous, null, ValueCardinality.Unordered, "cjkTGH"),
    kTGHZ2013(PropertyType.Miscellaneous, "cjkTGHZ2013"),
    kTaiwanTelegraph(PropertyType.Miscellaneous, "cjkTaiwanTelegraph"),
    kTang(PropertyType.Miscellaneous, null, ValueCardinality.Unordered, "cjkTang"),
    kTotalStrokes(PropertyType.Miscellaneous, null, ValueCardinality.Ordered, "cjkTotalStrokes"),
    kUnihanCore2020(PropertyType.Miscellaneous, "cjkUnihanCore2020"),
    kVietnamese(PropertyType.Miscellaneous, null, ValueCardinality.Unordered, "cjkVietnamese"),
    kVietnameseNumeric(PropertyType.Miscellaneous, "cjkVietnameseNumeric"),
    kXHC1983(PropertyType.Miscellaneous, null, ValueCardinality.Unordered, "cjkXHC1983"),
    kXerox(PropertyType.Miscellaneous, "cjkXerox"),
    kZVariant(PropertyType.Miscellaneous, "cjkZVariant"),
    kZhuangNumeric(PropertyType.Miscellaneous, "cjkZhuangNumeric"),

    // Catalog
    Age(PropertyType.Catalog, Age_Values.class, null, "age"),
    Block(PropertyType.Catalog, Block_Values.class, null, "blk"),
    Script(PropertyType.Catalog, Script_Values.class, null, "sc"),
    Script_Extensions(PropertyType.Catalog, Script_Values.class, ValueCardinality.Unordered, "scx"),

    // Enumerated
    Bidi_Class(PropertyType.Enumerated, Bidi_Class_Values.class, null, "bc"),
    Bidi_Paired_Bracket_Type(
            PropertyType.Enumerated, Bidi_Paired_Bracket_Type_Values.class, null, "bpt"),
    Canonical_Combining_Class(
            PropertyType.Enumerated, Canonical_Combining_Class_Values.class, null, "ccc"),
    Decomposition_Type(PropertyType.Enumerated, Decomposition_Type_Values.class, null, "dt"),
    Do_Not_Emit_Type(
            PropertyType.Enumerated, Do_Not_Emit_Type_Values.class, null, "Do_Not_Emit_Type"),
    East_Asian_Width(PropertyType.Enumerated, East_Asian_Width_Values.class, null, "ea"),
    General_Category(PropertyType.Enumerated, General_Category_Values.class, null, "gc"),
    Grapheme_Cluster_Break(
            PropertyType.Enumerated, Grapheme_Cluster_Break_Values.class, null, "GCB"),
    Hangul_Syllable_Type(PropertyType.Enumerated, Hangul_Syllable_Type_Values.class, null, "hst"),
    Identifier_Status(PropertyType.Enumerated, Identifier_Status_Values.class, null, "idstatus"),
    Identifier_Type(
            PropertyType.Enumerated,
            Identifier_Type_Values.class,
            ValueCardinality.Unordered,
            "idtype"),
    Idn_2008(PropertyType.Enumerated, Idn_2008_Values.class, null, "idn8"),
    Idn_Status(PropertyType.Enumerated, Idn_Status_Values.class, null, "idns"),
    Indic_Conjunct_Break(PropertyType.Enumerated, Indic_Conjunct_Break_Values.class, null, "InCB"),
    Indic_Positional_Category(
            PropertyType.Enumerated, Indic_Positional_Category_Values.class, null, "InPC"),
    Indic_Syllabic_Category(
            PropertyType.Enumerated, Indic_Syllabic_Category_Values.class, null, "InSC"),
    Joining_Group(PropertyType.Enumerated, Joining_Group_Values.class, null, "jg"),
    Joining_Type(PropertyType.Enumerated, Joining_Type_Values.class, null, "jt"),
    Line_Break(PropertyType.Enumerated, Line_Break_Values.class, null, "lb"),
    NFC_Quick_Check(PropertyType.Enumerated, NFC_Quick_Check_Values.class, null, "NFC_QC"),
    NFD_Quick_Check(PropertyType.Enumerated, NFD_Quick_Check_Values.class, null, "NFD_QC"),
    NFKC_Quick_Check(PropertyType.Enumerated, NFKC_Quick_Check_Values.class, null, "NFKC_QC"),
    NFKD_Quick_Check(PropertyType.Enumerated, NFKD_Quick_Check_Values.class, null, "NFKD_QC"),
    Numeric_Type(PropertyType.Enumerated, Numeric_Type_Values.class, null, "nt"),
    Other_Joining_Type(
            PropertyType.Enumerated, Other_Joining_Type_Values.class, null, "Other_Joining_Type"),
    Sentence_Break(PropertyType.Enumerated, Sentence_Break_Values.class, null, "SB"),
    Vertical_Orientation(PropertyType.Enumerated, Vertical_Orientation_Values.class, null, "vo"),
    Word_Break(PropertyType.Enumerated, Word_Break_Values.class, null, "WB"),

    // Binary
    ASCII_Hex_Digit(PropertyType.Binary, Binary.class, null, "AHex"),
    Alphabetic(PropertyType.Binary, Binary.class, null, "Alpha"),
    Basic_Emoji(PropertyType.Binary, Binary.class, null, "BE"),
    Bidi_Control(PropertyType.Binary, Binary.class, null, "Bidi_C"),
    Bidi_Mirrored(PropertyType.Binary, Binary.class, null, "Bidi_M"),
    Case_Ignorable(PropertyType.Binary, Binary.class, null, "CI"),
    Cased(PropertyType.Binary, Binary.class, null, "Cased"),
    Changes_When_Casefolded(PropertyType.Binary, Binary.class, null, "CWCF"),
    Changes_When_Casemapped(PropertyType.Binary, Binary.class, null, "CWCM"),
    Changes_When_Lowercased(PropertyType.Binary, Binary.class, null, "CWL"),
    Changes_When_NFKC_Casefolded(PropertyType.Binary, Binary.class, null, "CWKCF"),
    Changes_When_Titlecased(PropertyType.Binary, Binary.class, null, "CWT"),
    Changes_When_Uppercased(PropertyType.Binary, Binary.class, null, "CWU"),
    Composition_Exclusion(PropertyType.Binary, Binary.class, null, "CE"),
    Dash(PropertyType.Binary, Binary.class, null, "Dash"),
    Default_Ignorable_Code_Point(PropertyType.Binary, Binary.class, null, "DI"),
    Deprecated(PropertyType.Binary, Binary.class, null, "Dep"),
    Diacritic(PropertyType.Binary, Binary.class, null, "Dia"),
    Emoji(PropertyType.Binary, Binary.class, null, "Emoji"),
    Emoji_Component(PropertyType.Binary, Binary.class, null, "EComp"),
    Emoji_Modifier(PropertyType.Binary, Binary.class, null, "EMod"),
    Emoji_Modifier_Base(PropertyType.Binary, Binary.class, null, "EBase"),
    Emoji_Presentation(PropertyType.Binary, Binary.class, null, "EPres"),
    Expands_On_NFC(PropertyType.Binary, Binary.class, null, "XO_NFC"),
    Expands_On_NFD(PropertyType.Binary, Binary.class, null, "XO_NFD"),
    Expands_On_NFKC(PropertyType.Binary, Binary.class, null, "XO_NFKC"),
    Expands_On_NFKD(PropertyType.Binary, Binary.class, null, "XO_NFKD"),
    Extended_Pictographic(PropertyType.Binary, Binary.class, null, "ExtPict"),
    Extender(PropertyType.Binary, Binary.class, null, "Ext"),
    Full_Composition_Exclusion(PropertyType.Binary, Binary.class, null, "Comp_Ex"),
    Grapheme_Base(PropertyType.Binary, Binary.class, null, "Gr_Base"),
    Grapheme_Extend(PropertyType.Binary, Binary.class, null, "Gr_Ext"),
    Grapheme_Link(PropertyType.Binary, Binary.class, null, "Gr_Link"),
    Hex_Digit(PropertyType.Binary, Binary.class, null, "Hex"),
    Hyphen(PropertyType.Binary, Binary.class, null, "Hyphen"),
    IDS_Binary_Operator(PropertyType.Binary, Binary.class, null, "IDSB"),
    IDS_Trinary_Operator(PropertyType.Binary, Binary.class, null, "IDST"),
    IDS_Unary_Operator(PropertyType.Binary, Binary.class, null, "IDSU"),
    ID_Compat_Math_Continue(PropertyType.Binary, Binary.class, null, "ID_Compat_Math_Continue"),
    ID_Compat_Math_Start(PropertyType.Binary, Binary.class, null, "ID_Compat_Math_Start"),
    ID_Continue(PropertyType.Binary, Binary.class, null, "IDC"),
    ID_Start(PropertyType.Binary, Binary.class, null, "IDS"),
    Ideographic(PropertyType.Binary, Binary.class, null, "Ideo"),
    Join_Control(PropertyType.Binary, Binary.class, null, "Join_C"),
    Logical_Order_Exception(PropertyType.Binary, Binary.class, null, "LOE"),
    Lowercase(PropertyType.Binary, Binary.class, null, "Lower"),
    Math(PropertyType.Binary, Binary.class, null, "Math"),
    Modifier_Combining_Mark(PropertyType.Binary, Binary.class, null, "MCM"),
    Noncharacter_Code_Point(PropertyType.Binary, Binary.class, null, "NChar"),
    Other_Alphabetic(PropertyType.Binary, Binary.class, null, "OAlpha"),
    Other_Default_Ignorable_Code_Point(PropertyType.Binary, Binary.class, null, "ODI"),
    Other_Grapheme_Extend(PropertyType.Binary, Binary.class, null, "OGr_Ext"),
    Other_ID_Continue(PropertyType.Binary, Binary.class, null, "OIDC"),
    Other_ID_Start(PropertyType.Binary, Binary.class, null, "OIDS"),
    Other_Lowercase(PropertyType.Binary, Binary.class, null, "OLower"),
    Other_Math(PropertyType.Binary, Binary.class, null, "OMath"),
    Other_Uppercase(PropertyType.Binary, Binary.class, null, "OUpper"),
    Pattern_Syntax(PropertyType.Binary, Binary.class, null, "Pat_Syn"),
    Pattern_White_Space(PropertyType.Binary, Binary.class, null, "Pat_WS"),
    Prepended_Concatenation_Mark(PropertyType.Binary, Binary.class, null, "PCM"),
    Quotation_Mark(PropertyType.Binary, Binary.class, null, "QMark"),
    RGI_Emoji_Flag_Sequence(PropertyType.Binary, Binary.class, null, "REFS", "Emoji_Flag_Sequence"),
    RGI_Emoji_Keycap_Sequence(
            PropertyType.Binary, Binary.class, null, "REKS", "Emoji_Keycap_Sequence"),
    RGI_Emoji_Modifier_Sequence(
            PropertyType.Binary, Binary.class, null, "REMS", "Emoji_Modifier_Sequence"),
    RGI_Emoji_Tag_Sequence(PropertyType.Binary, Binary.class, null, "RETS", "Emoji_Tag_Sequence"),
    RGI_Emoji_Zwj_Sequence(PropertyType.Binary, Binary.class, null, "REZS", "Emoji_Zwj_Sequence"),
    Radical(PropertyType.Binary, Binary.class, null, "Radical"),
    Regional_Indicator(PropertyType.Binary, Binary.class, null, "RI"),
    Sentence_Terminal(PropertyType.Binary, Binary.class, null, "STerm"),
    Soft_Dotted(PropertyType.Binary, Binary.class, null, "SD"),
    Terminal_Punctuation(PropertyType.Binary, Binary.class, null, "Term"),
    Unified_Ideograph(PropertyType.Binary, Binary.class, null, "UIdeo"),
    Uppercase(PropertyType.Binary, Binary.class, null, "Upper"),
    Variation_Selector(PropertyType.Binary, Binary.class, null, "VS"),
    White_Space(PropertyType.Binary, Binary.class, null, "WSpace", "space"),
    XID_Continue(PropertyType.Binary, Binary.class, null, "XIDC"),
    XID_Start(PropertyType.Binary, Binary.class, null, "XIDS"),

// Unknown
;

    private final PropertyType type;
    private final PropertyNames<UcdProperty> names;
    // for enums
    private final NameMatcher name2enum;
    private final EnumSet enums;
    private final Class enumClass;
    private final ValueCardinality cardinality;

    private UcdProperty(PropertyType type, String shortName, String... otherNames) {
        this.type = type;
        names = new PropertyNames<UcdProperty>(UcdProperty.class, this, shortName, otherNames);
        name2enum = null;
        enums = null;
        enumClass = null;
        cardinality = ValueCardinality.Singleton;
    }

    private UcdProperty(
            PropertyType type,
            Class classItem,
            ValueCardinality _cardinality,
            String shortName,
            String... otherNames) {
        this.type = type;
        names = new PropertyNames<UcdProperty>(UcdProperty.class, this, shortName, otherNames);
        cardinality = _cardinality == null ? ValueCardinality.Singleton : _cardinality;
        if (classItem == null) {
            name2enum = null;
            enums = null;
            enumClass = null;
        } else {
            enums = EnumSet.allOf(classItem);
            name2enum = PropertyNames.getNameToEnums(classItem);
            enumClass = classItem;
        }
    }

    public ValueCardinality getCardinality() {
        return cardinality;
    }

    public Class<Enum> getEnumClass() {
        return enumClass;
    }

    public PropertyType getType() {
        return type;
    }

    public PropertyNames<UcdProperty> getNames() {
        return names;
    }

    public String getShortName() {
        return names.getShortName();
    }

    public static UcdProperty forString(String name) {
        return Numeric_Value.names.forString(name);
    }

    public Enum getEnum(String name) {
        return name2enum == null ? null : name2enum.get(name);
    }

    public PropertyNames getEnumNames() {
        return name2enum == null ? null : name2enum.getNames();
    }

    public Set<Enum> getEnums() {
        return enums;
    }
}
