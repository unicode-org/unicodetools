//Machine generated: GenerateEnums.java
package org.unicode.props;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.props.PropertyValues.General_Category_Values;


public enum UcdPropertyCopy {

    // Numeric
    Numeric_Value(PropertyNames.PropertyType.Numeric, "nv"),
    kAccountingNumeric(PropertyNames.PropertyType.Numeric, "cjkAccountingNumeric"),
    kOtherNumeric(PropertyNames.PropertyType.Numeric, "cjkOtherNumeric"),
    kPrimaryNumeric(PropertyNames.PropertyType.Numeric, "cjkPrimaryNumeric"),

    // String
    Bidi_Mirroring_Glyph(PropertyNames.PropertyType.String, "bmg"),
    Case_Folding(PropertyNames.PropertyType.String, "cf"),
    Decomposition_Mapping(PropertyNames.PropertyType.String, "dm"),
    FC_NFKC_Closure(PropertyNames.PropertyType.String, "FC_NFKC"),
    Lowercase_Mapping(PropertyNames.PropertyType.String, "lc"),
    NFKC_Casefold(PropertyNames.PropertyType.String, "NFKC_CF"),
    Simple_Case_Folding(PropertyNames.PropertyType.String, "scf"),
    Simple_Lowercase_Mapping(PropertyNames.PropertyType.String, "slc"),
    Simple_Titlecase_Mapping(PropertyNames.PropertyType.String, "stc"),
    Simple_Uppercase_Mapping(PropertyNames.PropertyType.String, "suc"),
    Titlecase_Mapping(PropertyNames.PropertyType.String, "tc"),
    Uppercase_Mapping(PropertyNames.PropertyType.String, "uc"),
    kCompatibilityVariant(PropertyNames.PropertyType.String, "cjkCompatibilityVariant"),

    // Miscellaneous
    ISO_Comment(PropertyNames.PropertyType.Miscellaneous, "isc"),
    Indic_Matra_Category(PropertyNames.PropertyType.Miscellaneous, "IMC"),
    Indic_Syllabic_Category(PropertyNames.PropertyType.Miscellaneous, "ISC"),
    Jamo_Short_Name(PropertyNames.PropertyType.Miscellaneous, "JSN"),
    Name(PropertyNames.PropertyType.Miscellaneous, "na"),
    Name_Alias(PropertyNames.PropertyType.Miscellaneous, null),
    Unicode_1_Name(PropertyNames.PropertyType.Miscellaneous, "na1"),
    kBigFive(PropertyNames.PropertyType.Miscellaneous, "cjkBigFive"),
    kCCCII(PropertyNames.PropertyType.Miscellaneous, "cjkCCCII"),
    kCNS1986(PropertyNames.PropertyType.Miscellaneous, "cjkCNS1986"),
    kCNS1992(PropertyNames.PropertyType.Miscellaneous, "cjkCNS1992"),
    kCangjie(PropertyNames.PropertyType.Miscellaneous, "cjkCangjie"),
    kCantonese(PropertyNames.PropertyType.Miscellaneous, "cjkCantonese"),
    kCheungBauer(PropertyNames.PropertyType.Miscellaneous, "cjkCheungBauer"),
    kCheungBauerIndex(PropertyNames.PropertyType.Miscellaneous, "cjkCheungBauerIndex"),
    kCihaiT(PropertyNames.PropertyType.Miscellaneous, "cjkCihaiT"),
    kCowles(PropertyNames.PropertyType.Miscellaneous, "cjkCowles"),
    kDaeJaweon(PropertyNames.PropertyType.Miscellaneous, "cjkDaeJaweon"),
    kDefinition(PropertyNames.PropertyType.Miscellaneous, "cjkDefinition"),
    kEACC(PropertyNames.PropertyType.Miscellaneous, "cjkEACC"),
    kFenn(PropertyNames.PropertyType.Miscellaneous, "cjkFenn"),
    kFennIndex(PropertyNames.PropertyType.Miscellaneous, "cjkFennIndex"),
    kFourCornerCode(PropertyNames.PropertyType.Miscellaneous, "cjkFourCornerCode"),
    kFrequency(PropertyNames.PropertyType.Miscellaneous, "cjkFrequency"),
    kGB0(PropertyNames.PropertyType.Miscellaneous, "cjkGB0"),
    kGB1(PropertyNames.PropertyType.Miscellaneous, "cjkGB1"),
    kGB3(PropertyNames.PropertyType.Miscellaneous, "cjkGB3"),
    kGB5(PropertyNames.PropertyType.Miscellaneous, "cjkGB5"),
    kGB7(PropertyNames.PropertyType.Miscellaneous, "cjkGB7"),
    kGB8(PropertyNames.PropertyType.Miscellaneous, "cjkGB8"),
    kGSR(PropertyNames.PropertyType.Miscellaneous, "cjkGSR"),
    kGradeLevel(PropertyNames.PropertyType.Miscellaneous, "cjkGradeLevel"),
    kHDZRadBreak(PropertyNames.PropertyType.Miscellaneous, "cjkHDZRadBreak"),
    kHKGlyph(PropertyNames.PropertyType.Miscellaneous, "cjkHKGlyph"),
    kHKSCS(PropertyNames.PropertyType.Miscellaneous, "cjkHKSCS"),
    kHanYu(PropertyNames.PropertyType.Miscellaneous, "cjkHanYu"),
    kHangul(PropertyNames.PropertyType.Miscellaneous, "cjkHangul"),
    kHanyuPinlu(PropertyNames.PropertyType.Miscellaneous, "cjkHanyuPinlu"),
    kHanyuPinyin(PropertyNames.PropertyType.Miscellaneous, "cjkHanyuPinyin"),
    kIBMJapan(PropertyNames.PropertyType.Miscellaneous, "cjkIBMJapan"),
    kIICore(PropertyNames.PropertyType.Miscellaneous, "cjkIICore"),
    kIRGDaeJaweon(PropertyNames.PropertyType.Miscellaneous, "cjkIRGDaeJaweon"),
    kIRGDaiKanwaZiten(PropertyNames.PropertyType.Miscellaneous, "cjkIRGDaiKanwaZiten"),
    kIRGHanyuDaZidian(PropertyNames.PropertyType.Miscellaneous, "cjkIRGHanyuDaZidian"),
    kIRGKangXi(PropertyNames.PropertyType.Miscellaneous, "cjkIRGKangXi"),
    kIRG_GSource(PropertyNames.PropertyType.Miscellaneous, "cjkIRG_GSource"),
    kIRG_HSource(PropertyNames.PropertyType.Miscellaneous, "cjkIRG_HSource"),
    kIRG_JSource(PropertyNames.PropertyType.Miscellaneous, "cjkIRG_JSource"),
    kIRG_KPSource(PropertyNames.PropertyType.Miscellaneous, "cjkIRG_KPSource"),
    kIRG_KSource(PropertyNames.PropertyType.Miscellaneous, "cjkIRG_KSource"),
    kIRG_MSource(PropertyNames.PropertyType.Miscellaneous, "cjkIRG_MSource"),
    kIRG_TSource(PropertyNames.PropertyType.Miscellaneous, "cjkIRG_TSource"),
    kIRG_USource(PropertyNames.PropertyType.Miscellaneous, "cjkIRG_USource"),
    kIRG_VSource(PropertyNames.PropertyType.Miscellaneous, "cjkIRG_VSource"),
    kJIS0213(PropertyNames.PropertyType.Miscellaneous, "cjkJIS0213"),
    kJapaneseKun(PropertyNames.PropertyType.Miscellaneous, "cjkJapaneseKun"),
    kJapaneseOn(PropertyNames.PropertyType.Miscellaneous, "cjkJapaneseOn"),
    kJis0(PropertyNames.PropertyType.Miscellaneous, "cjkJis0"),
    kJis1(PropertyNames.PropertyType.Miscellaneous, "cjkJis1"),
    kKPS0(PropertyNames.PropertyType.Miscellaneous, "cjkKPS0"),
    kKPS1(PropertyNames.PropertyType.Miscellaneous, "cjkKPS1"),
    kKSC0(PropertyNames.PropertyType.Miscellaneous, "cjkKSC0"),
    kKSC1(PropertyNames.PropertyType.Miscellaneous, "cjkKSC1"),
    kKangXi(PropertyNames.PropertyType.Miscellaneous, "cjkKangXi"),
    kKarlgren(PropertyNames.PropertyType.Miscellaneous, "cjkKarlgren"),
    kKorean(PropertyNames.PropertyType.Miscellaneous, "cjkKorean"),
    kLau(PropertyNames.PropertyType.Miscellaneous, "cjkLau"),
    kMainlandTelegraph(PropertyNames.PropertyType.Miscellaneous, "cjkMainlandTelegraph"),
    kMandarin(PropertyNames.PropertyType.Miscellaneous, "cjkMandarin"),
    kMatthews(PropertyNames.PropertyType.Miscellaneous, "cjkMatthews"),
    kMeyerWempe(PropertyNames.PropertyType.Miscellaneous, "cjkMeyerWempe"),
    kMorohashi(PropertyNames.PropertyType.Miscellaneous, "cjkMorohashi"),
    kNelson(PropertyNames.PropertyType.Miscellaneous, "cjkNelson"),
    kPhonetic(PropertyNames.PropertyType.Miscellaneous, "cjkPhonetic"),
    kPseudoGB1(PropertyNames.PropertyType.Miscellaneous, "cjkPseudoGB1"),
    kRSAdobe_Japan1_6(PropertyNames.PropertyType.Miscellaneous, "cjkRSAdobe_Japan1_6"),
    kRSJapanese(PropertyNames.PropertyType.Miscellaneous, "cjkRSJapanese"),
    kRSKanWa(PropertyNames.PropertyType.Miscellaneous, "cjkRSKanWa"),
    kRSKangXi(PropertyNames.PropertyType.Miscellaneous, "cjkRSKangXi"),
    kRSKorean(PropertyNames.PropertyType.Miscellaneous, "cjkRSKorean"),
    kRSUnicode(PropertyNames.PropertyType.Miscellaneous, "cjkRSUnicode"),
    kSBGY(PropertyNames.PropertyType.Miscellaneous, "cjkSBGY"),
    kSemanticVariant(PropertyNames.PropertyType.Miscellaneous, "cjkSemanticVariant"),
    kSimplifiedVariant(PropertyNames.PropertyType.Miscellaneous, "cjkSimplifiedVariant"),
    kSpecializedSemanticVariant(PropertyNames.PropertyType.Miscellaneous, "cjkSpecializedSemanticVariant"),
    kTaiwanTelegraph(PropertyNames.PropertyType.Miscellaneous, "cjkTaiwanTelegraph"),
    kTang(PropertyNames.PropertyType.Miscellaneous, "cjkTang"),
    kTotalStrokes(PropertyNames.PropertyType.Miscellaneous, "cjkTotalStrokes"),
    kTraditionalVariant(PropertyNames.PropertyType.Miscellaneous, "cjkTraditionalVariant"),
    kVietnamese(PropertyNames.PropertyType.Miscellaneous, "cjkVietnamese"),
    kXHC1983(PropertyNames.PropertyType.Miscellaneous, "cjkXHC1983"),
    kXerox(PropertyNames.PropertyType.Miscellaneous, "cjkXerox"),
    kZVariant(PropertyNames.PropertyType.Miscellaneous, "cjkZVariant"),

    // Catalog
    Age(PropertyNames.PropertyType.Catalog, "age"),
    Block(PropertyNames.PropertyType.Catalog, "blk"),
    Script(PropertyNames.PropertyType.Catalog, "sc"),

    // Enumerated
    Bidi_Class(PropertyNames.PropertyType.Enumerated, "bc"),
    Canonical_Combining_Class(PropertyNames.PropertyType.Enumerated, "ccc"),
    Decomposition_Type(PropertyNames.PropertyType.Enumerated, "dt"),
    East_Asian_Width(PropertyNames.PropertyType.Enumerated, "ea"),
    General_Category(PropertyValues.General_Category_Values.class, "gc"),
    Grapheme_Cluster_Break(PropertyNames.PropertyType.Enumerated, "GCB"),
    Hangul_Syllable_Type(PropertyNames.PropertyType.Enumerated, "hst"),
    Joining_Group(PropertyNames.PropertyType.Enumerated, "jg"),
    Joining_Type(PropertyNames.PropertyType.Enumerated, "jt"),
    Line_Break(PropertyNames.PropertyType.Enumerated, "lb"),
    NFC_Quick_Check(PropertyNames.PropertyType.Enumerated, "NFC_QC"),
    NFD_Quick_Check(PropertyNames.PropertyType.Enumerated, "NFD_QC"),
    NFKC_Quick_Check(PropertyNames.PropertyType.Enumerated, "NFKC_QC"),
    NFKD_Quick_Check(PropertyNames.PropertyType.Enumerated, "NFKD_QC"),
    Numeric_Type(PropertyNames.PropertyType.Enumerated, "nt"),
    Sentence_Break(PropertyNames.PropertyType.Enumerated, "SB"),
    Word_Break(PropertyNames.PropertyType.Enumerated, "WB"),

    // Binary
    ASCII_Hex_Digit(PropertyValues.Binary.class, "AHex"),
    Alphabetic(PropertyValues.Binary.class, "Alpha"),
    Bidi_Control(PropertyValues.Binary.class, "Bidi_C"),
    Bidi_Mirrored(PropertyValues.Binary.class, "Bidi_M"),
    Case_Ignorable(PropertyValues.Binary.class, "CI"),
    Cased(PropertyValues.Binary.class, null),
    Changes_When_Casefolded(PropertyValues.Binary.class, "CWCF"),
    Changes_When_Casemapped(PropertyValues.Binary.class, "CWCM"),
    Changes_When_Lowercased(PropertyValues.Binary.class, "CWL"),
    Changes_When_NFKC_Casefolded(PropertyValues.Binary.class, "CWKCF"),
    Changes_When_Titlecased(PropertyValues.Binary.class, "CWT"),
    Changes_When_Uppercased(PropertyValues.Binary.class, "CWU"),
    Composition_Exclusion(PropertyValues.Binary.class, "CE"),
    Dash(PropertyValues.Binary.class, null),
    Default_Ignorable_Code_Point(PropertyValues.Binary.class, "DI"),
    Deprecated(PropertyValues.Binary.class, "Dep"),
    Diacritic(PropertyValues.Binary.class, "Dia"),
    Expands_On_NFC(PropertyValues.Binary.class, "XO_NFC"),
    Expands_On_NFD(PropertyValues.Binary.class, "XO_NFD"),
    Expands_On_NFKC(PropertyValues.Binary.class, "XO_NFKC"),
    Expands_On_NFKD(PropertyValues.Binary.class, "XO_NFKD"),
    Extender(PropertyValues.Binary.class, "Ext"),
    Full_Composition_Exclusion(PropertyValues.Binary.class, "Comp_Ex"),
    Grapheme_Base(PropertyValues.Binary.class, "Gr_Base"),
    Grapheme_Extend(PropertyValues.Binary.class, "Gr_Ext"),
    Grapheme_Link(PropertyValues.Binary.class, "Gr_Link"),
    Hex_Digit(PropertyValues.Binary.class, "Hex"),
    Hyphen(PropertyValues.Binary.class, null),
    IDS_Binary_Operator(PropertyValues.Binary.class, "IDSB"),
    IDS_Trinary_Operator(PropertyValues.Binary.class, "IDST"),
    ID_Continue(PropertyValues.Binary.class, "IDC"),
    ID_Start(PropertyValues.Binary.class, "IDS"),
    Ideographic(PropertyValues.Binary.class, "Ideo"),
    Join_Control(PropertyValues.Binary.class, "Join_C"),
    Logical_Order_Exception(PropertyValues.Binary.class, "LOE"),
    Lowercase(PropertyValues.Binary.class, "Lower"),
    Math(PropertyValues.Binary.class, null),
    Noncharacter_Code_Point(PropertyValues.Binary.class, "NChar"),
    Other_Alphabetic(PropertyValues.Binary.class, "OAlpha"),
    Other_Default_Ignorable_Code_Point(PropertyValues.Binary.class, "ODI"),
    Other_Grapheme_Extend(PropertyValues.Binary.class, "OGr_Ext"),
    Other_ID_Continue(PropertyValues.Binary.class, "OIDC"),
    Other_ID_Start(PropertyValues.Binary.class, "OIDS"),
    Other_Lowercase(PropertyValues.Binary.class, "OLower"),
    Other_Math(PropertyValues.Binary.class, "OMath"),
    Other_Uppercase(PropertyValues.Binary.class, "OUpper"),
    Pattern_Syntax(PropertyValues.Binary.class, "Pat_Syn"),
    Pattern_White_Space(PropertyValues.Binary.class, "Pat_WS"),
    Quotation_Mark(PropertyValues.Binary.class, "QMark"),
    Radical(PropertyValues.Binary.class, null),
    STerm(PropertyValues.Binary.class, null),
    Soft_Dotted(PropertyValues.Binary.class, "SD"),
    Terminal_Punctuation(PropertyValues.Binary.class, "Term"),
    Unified_Ideograph(PropertyValues.Binary.class, "UIdeo"),
    Uppercase(PropertyValues.Binary.class, "Upper"),
    Variation_Selector(PropertyValues.Binary.class, "VS"),
    White_Space(PropertyValues.Binary.class, "WSpace"),
    XID_Continue(PropertyValues.Binary.class, "XIDC"),
    XID_Start(PropertyValues.Binary.class, "XIDS"),
    // EXTRAS
    Script_Extensions(PropertyNames.PropertyType.Enumerated, "SCX"),
    CJK_Radical(PropertyNames.PropertyType.Enumerated, "CJKR"),
    Emoji_DoCoMo(PropertyNames.PropertyType.Miscellaneous, null),
    Emoji_KDDI(PropertyNames.PropertyType.Miscellaneous, null),
    Emoji_SoftBank(PropertyNames.PropertyType.Miscellaneous, null),
    Name_Alias_Prov(PropertyNames.PropertyType.Miscellaneous, null),
    Named_Sequences(PropertyNames.PropertyType.Miscellaneous, null),
    Named_Sequences_Prov(PropertyNames.PropertyType.Miscellaneous, null),
    ;
    private final PropertyNames.PropertyType type;
    private final PropertyNames<UcdProperty> names;
    // for enums
    private final Map<String, Enum> name2enum;
    private final EnumSet enums;

    private UcdPropertyCopy(PropertyNames.PropertyType type, String shortName, String...otherNames) {
        this.type = type;
        names = new PropertyNames(UcdProperty.class, this, shortName, otherNames);
        name2enum = null;
        enums = null;
    }
    private UcdPropertyCopy(Class classItem, String shortName, String...otherNames) {
        Object[] x = classItem.getEnumConstants();
        type = classItem == PropertyValues.Binary.class ? PropertyNames.PropertyType.Binary : PropertyNames.PropertyType.Enumerated;
        names = new PropertyNames(UcdProperty.class, this, shortName, otherNames);
        enums = EnumSet.allOf(classItem);
        name2enum = PropertyNames.getNameToEnums(classItem);
    }

    public PropertyNames.PropertyType getType() {
        return type;
    }
    public PropertyNames<UcdProperty> getNames() {
        return names;
    }
    public static UcdProperty forString(String name) {
        return Numeric_Value.names.forString(name);
    }
    public Enum getEnum(String name) {
        return name2enum == null ? null : name2enum.get(name);
    }
    public Set<Enum> getEnums() {
        return enums;
    }
}

