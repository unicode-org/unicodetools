//Machine generated: GenerateEnums.java
package org.unicode.props;
import java.util.EnumSet;
import java.util.Set;
import org.unicode.props.PropertyNames.NameMatcher;
public enum UcdProperty {

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
    CJK_Radical(PropertyNames.PropertyType.Miscellaneous, "CJKR"),
    Emoji_DCM(PropertyNames.PropertyType.Miscellaneous, "EDCM"),
    Emoji_KDDI(PropertyNames.PropertyType.Miscellaneous, "EKDDI"),
    Emoji_SB(PropertyNames.PropertyType.Miscellaneous, "ESB"),
    ISO_Comment(PropertyNames.PropertyType.Miscellaneous, "isc"),
    Jamo_Short_Name(PropertyNames.PropertyType.Miscellaneous, "JSN"),
    Name(PropertyNames.PropertyType.Miscellaneous, "na"),
    Name_Alias(PropertyNames.PropertyType.Miscellaneous, "Name_Alias"),
    Name_Alias_Prov(PropertyNames.PropertyType.Miscellaneous, "NAP"),
    Named_Sequences(PropertyNames.PropertyType.Miscellaneous, "NS"),
    Named_Sequences_Prov(PropertyNames.PropertyType.Miscellaneous, "NSP"),
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
    Age(PropertyNames.PropertyType.Catalog, PropertyValues.Age_Values.class, "age"),
    Block(PropertyNames.PropertyType.Catalog, PropertyValues.Block_Values.class, "blk"),
    Script(PropertyNames.PropertyType.Catalog, PropertyValues.Script_Values.class, "sc"),

		// Enumerated
    Bidi_Class(PropertyNames.PropertyType.Enumerated, PropertyValues.Bidi_Class_Values.class, "bc"),
    Canonical_Combining_Class(PropertyNames.PropertyType.Enumerated, PropertyValues.Canonical_Combining_Class_Values.class, "ccc"),
    Decomposition_Type(PropertyNames.PropertyType.Enumerated, PropertyValues.Decomposition_Type_Values.class, "dt"),
    East_Asian_Width(PropertyNames.PropertyType.Enumerated, PropertyValues.East_Asian_Width_Values.class, "ea"),
    General_Category(PropertyNames.PropertyType.Enumerated, PropertyValues.General_Category_Values.class, "gc"),
    Grapheme_Cluster_Break(PropertyNames.PropertyType.Enumerated, PropertyValues.Grapheme_Cluster_Break_Values.class, "GCB"),
    Hangul_Syllable_Type(PropertyNames.PropertyType.Enumerated, PropertyValues.Hangul_Syllable_Type_Values.class, "hst"),
    Indic_Matra_Category(PropertyNames.PropertyType.Enumerated, PropertyValues.Indic_Matra_Category_Values.class, "IMC"),
    Indic_Syllabic_Category(PropertyNames.PropertyType.Enumerated, PropertyValues.Indic_Syllabic_Category_Values.class, "ISC"),
    Joining_Group(PropertyNames.PropertyType.Enumerated, PropertyValues.Joining_Group_Values.class, "jg"),
    Joining_Type(PropertyNames.PropertyType.Enumerated, PropertyValues.Joining_Type_Values.class, "jt"),
    Line_Break(PropertyNames.PropertyType.Enumerated, PropertyValues.Line_Break_Values.class, "lb"),
    NFC_Quick_Check(PropertyNames.PropertyType.Enumerated, PropertyValues.NFC_Quick_Check_Values.class, "NFC_QC"),
    NFD_Quick_Check(PropertyNames.PropertyType.Enumerated, PropertyValues.NFD_Quick_Check_Values.class, "NFD_QC"),
    NFKC_Quick_Check(PropertyNames.PropertyType.Enumerated, PropertyValues.NFKC_Quick_Check_Values.class, "NFKC_QC"),
    NFKD_Quick_Check(PropertyNames.PropertyType.Enumerated, PropertyValues.NFKD_Quick_Check_Values.class, "NFKD_QC"),
    Numeric_Type(PropertyNames.PropertyType.Enumerated, PropertyValues.Numeric_Type_Values.class, "nt"),
    Script_Extensions(PropertyNames.PropertyType.Enumerated, PropertyValues.Script_Values.class, "SCX"),
    Sentence_Break(PropertyNames.PropertyType.Enumerated, PropertyValues.Sentence_Break_Values.class, "SB"),
    Word_Break(PropertyNames.PropertyType.Enumerated, PropertyValues.Word_Break_Values.class, "WB"),

		// Binary
    ASCII_Hex_Digit(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "AHex"),
    Alphabetic(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "Alpha"),
    Bidi_Control(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "Bidi_C"),
    Bidi_Mirrored(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "Bidi_M"),
    Case_Ignorable(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "CI"),
    Cased(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "Cased"),
    Changes_When_Casefolded(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "CWCF"),
    Changes_When_Casemapped(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "CWCM"),
    Changes_When_Lowercased(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "CWL"),
    Changes_When_NFKC_Casefolded(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "CWKCF"),
    Changes_When_Titlecased(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "CWT"),
    Changes_When_Uppercased(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "CWU"),
    Composition_Exclusion(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "CE"),
    Dash(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "Dash"),
    Default_Ignorable_Code_Point(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "DI"),
    Deprecated(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "Dep"),
    Diacritic(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "Dia"),
    Expands_On_NFC(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "XO_NFC"),
    Expands_On_NFD(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "XO_NFD"),
    Expands_On_NFKC(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "XO_NFKC"),
    Expands_On_NFKD(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "XO_NFKD"),
    Extender(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "Ext"),
    Full_Composition_Exclusion(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "Comp_Ex"),
    Grapheme_Base(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "Gr_Base"),
    Grapheme_Extend(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "Gr_Ext"),
    Grapheme_Link(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "Gr_Link"),
    Hex_Digit(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "Hex"),
    Hyphen(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "Hyphen"),
    IDS_Binary_Operator(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "IDSB"),
    IDS_Trinary_Operator(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "IDST"),
    ID_Continue(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "IDC"),
    ID_Start(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "IDS"),
    Ideographic(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "Ideo"),
    Join_Control(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "Join_C"),
    Logical_Order_Exception(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "LOE"),
    Lowercase(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "Lower"),
    Math(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "Math"),
    Noncharacter_Code_Point(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "NChar"),
    Other_Alphabetic(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "OAlpha"),
    Other_Default_Ignorable_Code_Point(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "ODI"),
    Other_Grapheme_Extend(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "OGr_Ext"),
    Other_ID_Continue(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "OIDC"),
    Other_ID_Start(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "OIDS"),
    Other_Lowercase(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "OLower"),
    Other_Math(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "OMath"),
    Other_Uppercase(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "OUpper"),
    Pattern_Syntax(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "Pat_Syn"),
    Pattern_White_Space(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "Pat_WS"),
    Quotation_Mark(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "QMark"),
    Radical(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "Radical"),
    STerm(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "STerm"),
    Soft_Dotted(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "SD"),
    Terminal_Punctuation(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "Term"),
    Unified_Ideograph(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "UIdeo"),
    Uppercase(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "Upper"),
    Variation_Selector(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "VS"),
    White_Space(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "WSpace"),
    XID_Continue(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "XIDC"),
    XID_Start(PropertyNames.PropertyType.Binary, PropertyValues.Binary.class, "XIDS"),
		;
;
private final PropertyNames.PropertyType type;
	private final PropertyNames<UcdProperty> names;
	// for enums
	private final NameMatcher name2enum;
	private final EnumSet enums;
	
	private UcdProperty(PropertyNames.PropertyType type, String shortName, String...otherNames) {
		this.type = type;
		names = new PropertyNames(UcdProperty.class, this, shortName, otherNames);
		name2enum = null;
		enums = null;
	}
	private UcdProperty(PropertyNames.PropertyType type, Class classItem, String shortName, String...otherNames) {
		this.type = type;
		Object[] x = classItem.getEnumConstants();
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
