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
import org.unicode.props.UcdPropertyValues.kEH_Core_Values;

/**
 * Machine-generated file for properties, produced by GenerateEnums.java from PropertyAliases.txt
 * and ExtraPropertyAliases.txt. The ordering of properties is first by category, then alphabetical
 * (ASCII order).
 */
public enum UcdProperty {

    // Numeric
    Numeric_Value(PropertyType.Numeric, DerivedPropertyStatus.Approved, "nv"),
    kAccountingNumeric(
            PropertyType.Numeric, DerivedPropertyStatus.Approved, "cjkAccountingNumeric"),
    kOtherNumeric(PropertyType.Numeric, DerivedPropertyStatus.Approved, "cjkOtherNumeric"),
    kPrimaryNumeric(
            PropertyType.Numeric,
            DerivedPropertyStatus.Approved,
            null,
            ValueCardinality.Ordered,
            "cjkPrimaryNumeric"),

    // String
    Bidi_Mirroring_Glyph(PropertyType.String, DerivedPropertyStatus.Approved, "bmg"),
    Bidi_Paired_Bracket(PropertyType.String, DerivedPropertyStatus.Approved, "bpb"),
    Case_Folding(PropertyType.String, DerivedPropertyStatus.Approved, "cf"),
    Confusable_MA(
            PropertyType.String, DerivedPropertyStatus.NonUCDNonProperty, "ConfMA", "Confusable"),
    Confusable_ML(PropertyType.String, DerivedPropertyStatus.NonUCDNonProperty, "ConfML"),
    Confusable_SA(PropertyType.String, DerivedPropertyStatus.NonUCDNonProperty, "ConfSA"),
    Confusable_SL(PropertyType.String, DerivedPropertyStatus.NonUCDNonProperty, "ConfSL"),
    Decomposition_Mapping(PropertyType.String, DerivedPropertyStatus.Approved, "dm"),
    Do_Not_Emit_Preferred(
            PropertyType.String, DerivedPropertyStatus.UCDNonProperty, "Do_Not_Emit_Preferred"),
    Equivalent_Unified_Ideograph(PropertyType.String, DerivedPropertyStatus.Approved, "EqUIdeo"),
    FC_NFKC_Closure(PropertyType.String, DerivedPropertyStatus.Approved, "FC_NFKC"),
    Idn_Mapping(PropertyType.String, DerivedPropertyStatus.NonUCDNonProperty, "idnm"),
    Lowercase_Mapping(PropertyType.String, DerivedPropertyStatus.Approved, "lc"),
    NFKC_Casefold(PropertyType.String, DerivedPropertyStatus.Approved, "NFKC_CF"),
    NFKC_Simple_Casefold(PropertyType.String, DerivedPropertyStatus.Approved, "NFKC_SCF"),
    Simple_Case_Folding(PropertyType.String, DerivedPropertyStatus.Approved, "scf", "sfc"),
    Simple_Lowercase_Mapping(PropertyType.String, DerivedPropertyStatus.Approved, "slc"),
    Simple_Titlecase_Mapping(PropertyType.String, DerivedPropertyStatus.Approved, "stc"),
    Simple_Uppercase_Mapping(PropertyType.String, DerivedPropertyStatus.Approved, "suc"),
    Titlecase_Mapping(PropertyType.String, DerivedPropertyStatus.Approved, "tc"),
    Uppercase_Mapping(PropertyType.String, DerivedPropertyStatus.Approved, "uc"),
    kCompatibilityVariant(
            PropertyType.String, DerivedPropertyStatus.Approved, "cjkCompatibilityVariant"),
    kEH_AltSeq(PropertyType.String, DerivedPropertyStatus.Provisional, "kEH_AltSeq"),
    kSimplifiedVariant(
            PropertyType.String,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Unordered,
            "cjkSimplifiedVariant"),
    kTraditionalVariant(
            PropertyType.String,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Unordered,
            "cjkTraditionalVariant"),
    normalization_correction_corrected(
            PropertyType.String,
            DerivedPropertyStatus.UCDNonProperty,
            "normalization_correction_corrected"),
    normalization_correction_original(
            PropertyType.String,
            DerivedPropertyStatus.UCDNonProperty,
            "normalization_correction_original"),

    // Miscellaneous
    CJK_Radical(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.UCDNonProperty,
            null,
            ValueCardinality.Ordered,
            "CJKR"),
    Emoji_DCM(PropertyType.Miscellaneous, DerivedPropertyStatus.UCDNonProperty, "EDCM"),
    Emoji_KDDI(PropertyType.Miscellaneous, DerivedPropertyStatus.UCDNonProperty, "EKDDI"),
    Emoji_SB(PropertyType.Miscellaneous, DerivedPropertyStatus.UCDNonProperty, "ESB"),
    ISO_Comment(PropertyType.Miscellaneous, DerivedPropertyStatus.Approved, "isc"),
    Jamo_Short_Name(PropertyType.Miscellaneous, DerivedPropertyStatus.Approved, "JSN"),
    Name(PropertyType.Miscellaneous, DerivedPropertyStatus.Approved, "na"),
    Name_Alias(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Approved,
            null,
            ValueCardinality.Unordered,
            "Name_Alias"),
    Named_Sequences(PropertyType.Miscellaneous, DerivedPropertyStatus.UCDNonProperty, "NS"),
    Named_Sequences_Prov(PropertyType.Miscellaneous, DerivedPropertyStatus.UCDNonProperty, "NSP"),
    Standardized_Variant(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.UCDNonProperty,
            null,
            ValueCardinality.Unordered,
            "SV"),
    Unicode_1_Name(PropertyType.Miscellaneous, DerivedPropertyStatus.Approved, "na1"),
    emoji_variation_sequence(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.UCDNonProperty,
            "emoji_variation_sequence"),
    kAlternateHanYu(
            PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkAlternateHanYu"),
    kAlternateJEF(PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkAlternateJEF"),
    kAlternateKangXi(
            PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkAlternateKangXi"),
    kAlternateMorohashi(
            PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkAlternateMorohashi"),
    kAlternateTotalStrokes(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Unordered,
            "cjkAlternateTotalStrokes"),
    kBigFive(PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkBigFive"),
    kCCCII(PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkCCCII"),
    kCNS1986(PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkCNS1986"),
    kCNS1992(PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkCNS1992"),
    kCangjie(PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkCangjie"),
    kCantonese(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Unordered,
            "cjkCantonese"),
    kCheungBauer(PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkCheungBauer"),
    kCheungBauerIndex(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Unordered,
            "cjkCheungBauerIndex"),
    kCihaiT(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Unordered,
            "cjkCihaiT"),
    kCowles(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Unordered,
            "cjkCowles"),
    kDaeJaweon(PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkDaeJaweon"),
    kDefinition(PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkDefinition"),
    kEACC(PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkEACC"),
    kEH_Cat(PropertyType.Miscellaneous, DerivedPropertyStatus.Approved, "kEH_Cat"),
    kEH_Desc(PropertyType.Miscellaneous, DerivedPropertyStatus.Approved, "kEH_Desc"),
    kEH_FVal(PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "kEH_FVal"),
    kEH_Func(PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "kEH_Func"),
    kEH_HG(PropertyType.Miscellaneous, DerivedPropertyStatus.Approved, "kEH_HG"),
    kEH_IFAO(PropertyType.Miscellaneous, DerivedPropertyStatus.Approved, "kEH_IFAO"),
    kEH_JSesh(PropertyType.Miscellaneous, DerivedPropertyStatus.Approved, "kEH_JSesh"),
    kEH_UniK(PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "kEH_UniK"),
    kFanqie(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Unordered,
            "cjkFanqie"),
    kFenn(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Unordered,
            "cjkFenn"),
    kFennIndex(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Unordered,
            "cjkFennIndex"),
    kFourCornerCode(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Unordered,
            "cjkFourCornerCode"),
    kFrequency(PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkFrequency"),
    kGB0(PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkGB0"),
    kGB1(PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkGB1"),
    kGB3(PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkGB3"),
    kGB5(PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkGB5"),
    kGB7(PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkGB7"),
    kGB8(PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkGB8"),
    kGSR(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Unordered,
            "cjkGSR"),
    kGradeLevel(PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkGradeLevel"),
    kHDZRadBreak(PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkHDZRadBreak"),
    kHKGlyph(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Unordered,
            "cjkHKGlyph"),
    kHKSCS(PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkHKSCS"),
    kHanYu(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Unordered,
            "cjkHanYu"),
    kHangul(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Unordered,
            "cjkHangul"),
    kHanyuPinlu(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Unordered,
            "cjkHanyuPinlu"),
    kHanyuPinyin(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Unordered,
            "cjkHanyuPinyin"),
    kIBMJapan(PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkIBMJapan"),
    kIICore(PropertyType.Miscellaneous, DerivedPropertyStatus.Approved, "cjkIICore"),
    kIRGDaeJaweon(PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkIRGDaeJaweon"),
    kIRGDaiKanwaZiten(
            PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkIRGDaiKanwaZiten"),
    kIRGHanyuDaZidian(
            PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkIRGHanyuDaZidian"),
    kIRGKangXi(PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkIRGKangXi"),
    kIRG_GSource(PropertyType.Miscellaneous, DerivedPropertyStatus.Approved, "cjkIRG_GSource"),
    kIRG_HSource(PropertyType.Miscellaneous, DerivedPropertyStatus.Approved, "cjkIRG_HSource"),
    kIRG_JSource(PropertyType.Miscellaneous, DerivedPropertyStatus.Approved, "cjkIRG_JSource"),
    kIRG_KPSource(PropertyType.Miscellaneous, DerivedPropertyStatus.Approved, "cjkIRG_KPSource"),
    kIRG_KSource(PropertyType.Miscellaneous, DerivedPropertyStatus.Approved, "cjkIRG_KSource"),
    kIRG_MSource(PropertyType.Miscellaneous, DerivedPropertyStatus.Approved, "cjkIRG_MSource"),
    kIRG_SSource(PropertyType.Miscellaneous, DerivedPropertyStatus.Approved, "cjkIRG_SSource"),
    kIRG_TSource(PropertyType.Miscellaneous, DerivedPropertyStatus.Approved, "cjkIRG_TSource"),
    kIRG_UKSource(PropertyType.Miscellaneous, DerivedPropertyStatus.Approved, "cjkIRG_UKSource"),
    kIRG_USource(PropertyType.Miscellaneous, DerivedPropertyStatus.Approved, "cjkIRG_USource"),
    kIRG_VSource(PropertyType.Miscellaneous, DerivedPropertyStatus.Approved, "cjkIRG_VSource"),
    kJHJ(PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkJHJ"),
    kJIS0213(PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkJIS0213"),
    kJa(PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkJa"),
    kJapanese(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Unordered,
            "cjkJapanese"),
    kJapaneseKun(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Unordered,
            "cjkJapaneseKun"),
    kJapaneseOn(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Unordered,
            "cjkJapaneseOn"),
    kJinmeiyoKanji(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Unordered,
            "cjkJinmeiyoKanji"),
    kJis0(PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkJis0"),
    kJis1(PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkJis1"),
    kJoyoKanji(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Unordered,
            "cjkJoyoKanji"),
    kKPS0(PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkKPS0"),
    kKPS1(PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkKPS1"),
    kKSC0(PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkKSC0"),
    kKSC1(PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkKSC1"),
    kKangXi(PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkKangXi"),
    kKarlgren(PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkKarlgren"),
    kKorean(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Unordered,
            "cjkKorean"),
    kKoreanEducationHanja(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Unordered,
            "cjkKoreanEducationHanja"),
    kKoreanName(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Unordered,
            "cjkKoreanName"),
    kLau(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Unordered,
            "cjkLau"),
    kMainlandTelegraph(
            PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkMainlandTelegraph"),
    kMandarin(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Ordered,
            "cjkMandarin"),
    kMatthews(PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkMatthews"),
    kMeyerWempe(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Unordered,
            "cjkMeyerWempe"),
    kMojiJoho(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Unordered,
            "cjkMojiJoho"),
    kMorohashi(PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkMorohashi"),
    kNelson(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Unordered,
            "cjkNelson"),
    kPhonetic(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Unordered,
            "cjkPhonetic"),
    kPseudoGB1(PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkPseudoGB1"),
    kRSAdobe_Japan1_6(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Unordered,
            "cjkRSAdobe_Japan1_6"),
    kRSJapanese(PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkRSJapanese"),
    kRSKanWa(PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkRSKanWa"),
    kRSKangXi(PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkRSKangXi"),
    kRSKorean(PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkRSKorean"),
    kRSMerged(PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkRSMerged"),
    kRSTUnicode(PropertyType.Miscellaneous, DerivedPropertyStatus.UCDNonProperty, "kRSTUnicode"),
    kRSUnicode(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Approved,
            null,
            ValueCardinality.Ordered,
            "cjkRSUnicode",
            "Unicode_Radical_Stroke",
            "URS"),
    kReading(PropertyType.Miscellaneous, DerivedPropertyStatus.UCDNonProperty, "kReading"),
    kSBGY(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Unordered,
            "cjkSBGY"),
    kSMSZD2003Index(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Unordered,
            "cjkSMSZD2003Index"),
    kSMSZD2003Readings(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Unordered,
            "cjkSMSZD2003Readings"),
    kSemanticVariant(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Unordered,
            "cjkSemanticVariant"),
    kSpecializedSemanticVariant(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Unordered,
            "cjkSpecializedSemanticVariant"),
    kSpoofingVariant(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Unordered,
            "cjkSpoofingVariant"),
    kSrc_NushuDuben(
            PropertyType.Miscellaneous, DerivedPropertyStatus.UCDNonProperty, "kSrc_NushuDuben"),
    kStrange(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Unordered,
            "cjkStrange"),
    kTGH(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Unordered,
            "cjkTGH"),
    kTGHZ2013(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Unordered,
            "cjkTGHZ2013"),
    kTGT_MergedSrc(
            PropertyType.Miscellaneous, DerivedPropertyStatus.UCDNonProperty, "kTGT_MergedSrc"),
    kTaiwanTelegraph(
            PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkTaiwanTelegraph"),
    kTang(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Unordered,
            "cjkTang"),
    kTotalStrokes(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Ordered,
            "cjkTotalStrokes"),
    kUnihanCore2020(
            PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkUnihanCore2020"),
    kVietnamese(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Unordered,
            "cjkVietnamese"),
    kVietnameseNumeric(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Unordered,
            "cjkVietnameseNumeric"),
    kXHC1983(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Unordered,
            "cjkXHC1983"),
    kXerox(PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkXerox"),
    kZVariant(PropertyType.Miscellaneous, DerivedPropertyStatus.Provisional, "cjkZVariant"),
    kZhuang(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Unordered,
            "cjkZhuang"),
    kZhuangNumeric(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.Provisional,
            null,
            ValueCardinality.Unordered,
            "cjkZhuangNumeric"),
    normalization_correction_version(
            PropertyType.Miscellaneous,
            DerivedPropertyStatus.UCDNonProperty,
            "normalization_correction_version"),

    // Catalog
    Age(PropertyType.Catalog, DerivedPropertyStatus.Approved, Age_Values.class, null, "age"),
    Block(PropertyType.Catalog, DerivedPropertyStatus.Approved, Block_Values.class, null, "blk"),
    Script(PropertyType.Catalog, DerivedPropertyStatus.Approved, Script_Values.class, null, "sc"),
    Script_Extensions(
            PropertyType.Catalog,
            DerivedPropertyStatus.Approved,
            Script_Values.class,
            ValueCardinality.Unordered,
            "scx"),

    // Enumerated
    Bidi_Class(
            PropertyType.Enumerated,
            DerivedPropertyStatus.Approved,
            Bidi_Class_Values.class,
            null,
            "bc"),
    Bidi_Paired_Bracket_Type(
            PropertyType.Enumerated,
            DerivedPropertyStatus.Approved,
            Bidi_Paired_Bracket_Type_Values.class,
            null,
            "bpt"),
    Canonical_Combining_Class(
            PropertyType.Enumerated,
            DerivedPropertyStatus.Approved,
            Canonical_Combining_Class_Values.class,
            null,
            "ccc"),
    Decomposition_Type(
            PropertyType.Enumerated,
            DerivedPropertyStatus.Approved,
            Decomposition_Type_Values.class,
            null,
            "dt"),
    Do_Not_Emit_Type(
            PropertyType.Enumerated,
            DerivedPropertyStatus.UCDNonProperty,
            Do_Not_Emit_Type_Values.class,
            null,
            "Do_Not_Emit_Type"),
    East_Asian_Width(
            PropertyType.Enumerated,
            DerivedPropertyStatus.Approved,
            East_Asian_Width_Values.class,
            null,
            "ea"),
    General_Category(
            PropertyType.Enumerated,
            DerivedPropertyStatus.Approved,
            General_Category_Values.class,
            null,
            "gc"),
    Grapheme_Cluster_Break(
            PropertyType.Enumerated,
            DerivedPropertyStatus.Approved,
            Grapheme_Cluster_Break_Values.class,
            null,
            "GCB"),
    Hangul_Syllable_Type(
            PropertyType.Enumerated,
            DerivedPropertyStatus.Approved,
            Hangul_Syllable_Type_Values.class,
            null,
            "hst"),
    Identifier_Status(
            PropertyType.Enumerated,
            DerivedPropertyStatus.NonUCDProperty,
            Identifier_Status_Values.class,
            null,
            "ID_Status"),
    Identifier_Type(
            PropertyType.Enumerated,
            DerivedPropertyStatus.NonUCDProperty,
            Identifier_Type_Values.class,
            ValueCardinality.Unordered,
            "ID_Type"),
    Idn_2008(
            PropertyType.Enumerated,
            DerivedPropertyStatus.NonUCDNonProperty,
            Idn_2008_Values.class,
            null,
            "idn8"),
    Idn_Status(
            PropertyType.Enumerated,
            DerivedPropertyStatus.NonUCDNonProperty,
            Idn_Status_Values.class,
            null,
            "idns"),
    Indic_Conjunct_Break(
            PropertyType.Enumerated,
            DerivedPropertyStatus.Approved,
            Indic_Conjunct_Break_Values.class,
            null,
            "InCB"),
    Indic_Positional_Category(
            PropertyType.Enumerated,
            DerivedPropertyStatus.Approved,
            Indic_Positional_Category_Values.class,
            null,
            "InPC"),
    Indic_Syllabic_Category(
            PropertyType.Enumerated,
            DerivedPropertyStatus.Approved,
            Indic_Syllabic_Category_Values.class,
            null,
            "InSC"),
    Joining_Group(
            PropertyType.Enumerated,
            DerivedPropertyStatus.Approved,
            Joining_Group_Values.class,
            null,
            "jg"),
    Joining_Type(
            PropertyType.Enumerated,
            DerivedPropertyStatus.Approved,
            Joining_Type_Values.class,
            null,
            "jt"),
    Line_Break(
            PropertyType.Enumerated,
            DerivedPropertyStatus.Approved,
            Line_Break_Values.class,
            null,
            "lb"),
    NFC_Quick_Check(
            PropertyType.Enumerated,
            DerivedPropertyStatus.Approved,
            NFC_Quick_Check_Values.class,
            null,
            "NFC_QC"),
    NFD_Quick_Check(
            PropertyType.Enumerated,
            DerivedPropertyStatus.Approved,
            NFD_Quick_Check_Values.class,
            null,
            "NFD_QC"),
    NFKC_Quick_Check(
            PropertyType.Enumerated,
            DerivedPropertyStatus.Approved,
            NFKC_Quick_Check_Values.class,
            null,
            "NFKC_QC"),
    NFKD_Quick_Check(
            PropertyType.Enumerated,
            DerivedPropertyStatus.Approved,
            NFKD_Quick_Check_Values.class,
            null,
            "NFKD_QC"),
    Numeric_Type(
            PropertyType.Enumerated,
            DerivedPropertyStatus.Approved,
            Numeric_Type_Values.class,
            null,
            "nt"),
    Other_Joining_Type(
            PropertyType.Enumerated,
            DerivedPropertyStatus.UCDNonProperty,
            Other_Joining_Type_Values.class,
            null,
            "Other_Joining_Type"),
    Sentence_Break(
            PropertyType.Enumerated,
            DerivedPropertyStatus.Approved,
            Sentence_Break_Values.class,
            null,
            "SB"),
    Vertical_Orientation(
            PropertyType.Enumerated,
            DerivedPropertyStatus.Approved,
            Vertical_Orientation_Values.class,
            null,
            "vo"),
    Word_Break(
            PropertyType.Enumerated,
            DerivedPropertyStatus.Approved,
            Word_Break_Values.class,
            null,
            "WB"),
    kEH_Core(
            PropertyType.Enumerated,
            DerivedPropertyStatus.Provisional,
            kEH_Core_Values.class,
            null,
            "kEH_Core"),

    // Binary
    ASCII_Hex_Digit(
            PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "AHex"),
    Alphabetic(PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "Alpha"),
    Basic_Emoji(
            PropertyType.Binary, DerivedPropertyStatus.NonUCDProperty, Binary.class, null, "BE"),
    Bidi_Control(PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "Bidi_C"),
    Bidi_Mirrored(
            PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "Bidi_M"),
    Case_Ignorable(PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "CI"),
    Cased(PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "Cased"),
    Changes_When_Casefolded(
            PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "CWCF"),
    Changes_When_Casemapped(
            PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "CWCM"),
    Changes_When_Lowercased(
            PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "CWL"),
    Changes_When_NFKC_Casefolded(
            PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "CWKCF"),
    Changes_When_Titlecased(
            PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "CWT"),
    Changes_When_Uppercased(
            PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "CWU"),
    Composition_Exclusion(
            PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "CE"),
    Dash(PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "Dash"),
    Default_Ignorable_Code_Point(
            PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "DI"),
    Deprecated(PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "Dep"),
    Diacritic(PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "Dia"),
    Emoji(PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "Emoji"),
    Emoji_Component(
            PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "EComp"),
    Emoji_Modifier(PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "EMod"),
    Emoji_Modifier_Base(
            PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "EBase"),
    Emoji_Presentation(
            PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "EPres"),
    Expands_On_NFC(
            PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "XO_NFC"),
    Expands_On_NFD(
            PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "XO_NFD"),
    Expands_On_NFKC(
            PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "XO_NFKC"),
    Expands_On_NFKD(
            PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "XO_NFKD"),
    Extended_Pictographic(
            PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "ExtPict"),
    Extender(PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "Ext"),
    Full_Composition_Exclusion(
            PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "Comp_Ex"),
    Grapheme_Base(
            PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "Gr_Base"),
    Grapheme_Extend(
            PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "Gr_Ext"),
    Grapheme_Link(
            PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "Gr_Link"),
    Hex_Digit(PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "Hex"),
    Hyphen(PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "Hyphen"),
    IDS_Binary_Operator(
            PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "IDSB"),
    IDS_Trinary_Operator(
            PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "IDST"),
    IDS_Unary_Operator(
            PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "IDSU"),
    ID_Compat_Math_Continue(
            PropertyType.Binary,
            DerivedPropertyStatus.Approved,
            Binary.class,
            null,
            "ID_Compat_Math_Continue"),
    ID_Compat_Math_Start(
            PropertyType.Binary,
            DerivedPropertyStatus.Approved,
            Binary.class,
            null,
            "ID_Compat_Math_Start"),
    ID_Continue(PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "IDC"),
    ID_Start(PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "IDS"),
    Ideographic(PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "Ideo"),
    Join_Control(PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "Join_C"),
    Logical_Order_Exception(
            PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "LOE"),
    Lowercase(PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "Lower"),
    Math(PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "Math"),
    Modifier_Combining_Mark(
            PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "MCM"),
    Noncharacter_Code_Point(
            PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "NChar"),
    Other_Alphabetic(
            PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "OAlpha"),
    Other_Default_Ignorable_Code_Point(
            PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "ODI"),
    Other_Grapheme_Extend(
            PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "OGr_Ext"),
    Other_ID_Continue(
            PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "OIDC"),
    Other_ID_Start(PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "OIDS"),
    Other_Lowercase(
            PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "OLower"),
    Other_Math(PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "OMath"),
    Other_Uppercase(
            PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "OUpper"),
    Pattern_Syntax(
            PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "Pat_Syn"),
    Pattern_White_Space(
            PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "Pat_WS"),
    Prepended_Concatenation_Mark(
            PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "PCM"),
    Quotation_Mark(
            PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "QMark"),
    RGI_Emoji_Flag_Sequence(
            PropertyType.Binary,
            DerivedPropertyStatus.NonUCDProperty,
            Binary.class,
            null,
            "REFS",
            "Emoji_Flag_Sequence"),
    RGI_Emoji_Keycap_Sequence(
            PropertyType.Binary,
            DerivedPropertyStatus.NonUCDProperty,
            Binary.class,
            null,
            "REKS",
            "Emoji_Keycap_Sequence",
            "Emoji_Combining_Sequence"),
    RGI_Emoji_Modifier_Sequence(
            PropertyType.Binary,
            DerivedPropertyStatus.NonUCDProperty,
            Binary.class,
            null,
            "REMS",
            "Emoji_Modifier_Sequence"),
    RGI_Emoji_Tag_Sequence(
            PropertyType.Binary,
            DerivedPropertyStatus.NonUCDProperty,
            Binary.class,
            null,
            "RETS",
            "Emoji_Tag_Sequence"),
    RGI_Emoji_Zwj_Sequence(
            PropertyType.Binary,
            DerivedPropertyStatus.NonUCDProperty,
            Binary.class,
            null,
            "REZS",
            "Emoji_Zwj_Sequence"),
    Radical(PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "Radical"),
    Regional_Indicator(
            PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "RI"),
    Sentence_Terminal(
            PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "STerm"),
    Soft_Dotted(PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "SD"),
    Terminal_Punctuation(
            PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "Term"),
    Unified_Ideograph(
            PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "UIdeo"),
    Uppercase(PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "Upper"),
    Variation_Selector(
            PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "VS"),
    White_Space(
            PropertyType.Binary,
            DerivedPropertyStatus.Approved,
            Binary.class,
            null,
            "WSpace",
            "space"),
    XID_Continue(PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "XIDC"),
    XID_Start(PropertyType.Binary, DerivedPropertyStatus.Approved, Binary.class, null, "XIDS"),
    kEH_NoMirror(
            PropertyType.Binary,
            DerivedPropertyStatus.Approved,
            Binary.class,
            null,
            "kEH_NoMirror"),
    kEH_NoRotate(
            PropertyType.Binary,
            DerivedPropertyStatus.Approved,
            Binary.class,
            null,
            "kEH_NoRotate"),

// Unknown
;

    private final PropertyType type;
    private final DerivedPropertyStatus status;
    private final PropertyNames<UcdProperty> names;
    // for enums
    private final NameMatcher name2enum;
    private final EnumSet enums;
    private final Class enumClass;
    private final ValueCardinality cardinality;

    private UcdProperty(
            PropertyType type,
            DerivedPropertyStatus status,
            String shortName,
            String... otherNames) {
        this.type = type;
        this.status = status;
        names = new PropertyNames<UcdProperty>(UcdProperty.class, this, shortName, otherNames);
        name2enum = null;
        enums = null;
        enumClass = null;
        cardinality = ValueCardinality.Singleton;
    }

    private UcdProperty(
            PropertyType type,
            DerivedPropertyStatus status,
            Class classItem,
            ValueCardinality _cardinality,
            String shortName,
            String... otherNames) {
        this.type = type;
        this.status = status;
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

    public DerivedPropertyStatus getDerivedStatus() {
        return status;
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
