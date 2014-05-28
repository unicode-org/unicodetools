//Machine generated: GenerateEnums.java
package org.unicode.props;
import java.util.EnumSet;
import java.util.Set;
import org.unicode.props.PropertyNames.NameMatcher;
public enum UcdProperty {

		// Numeric
    Numeric_Value(PropertyType.Numeric, "nv"),
    kAccountingNumeric(PropertyType.Numeric, "cjkAccountingNumeric"),
    kOtherNumeric(PropertyType.Numeric, "cjkOtherNumeric"),
    kPrimaryNumeric(PropertyType.Numeric, "cjkPrimaryNumeric"),

		// String
    Case_Folding(PropertyType.String, "cf"),
    Decomposition_Mapping(PropertyType.String, "dm"),
    FC_NFKC_Closure(PropertyType.String, "FC_NFKC"),
    Lowercase_Mapping(PropertyType.String, "lc"),
    NFKC_Casefold(PropertyType.String, "NFKC_CF"),
    Simple_Case_Folding(PropertyType.String, "scf"),
    Simple_Lowercase_Mapping(PropertyType.String, "slc"),
    Simple_Titlecase_Mapping(PropertyType.String, "stc"),
    Simple_Uppercase_Mapping(PropertyType.String, "suc"),
    Titlecase_Mapping(PropertyType.String, "tc"),
    Uppercase_Mapping(PropertyType.String, "uc"),
    kCompatibilityVariant(PropertyType.String, "cjkCompatibilityVariant"),

		// Miscellaneous
    Bidi_Mirroring_Glyph(PropertyType.Miscellaneous, "bmg"),
    Bidi_Paired_Bracket(PropertyType.Miscellaneous, "bpb"),
    CJK_Radical(PropertyType.Miscellaneous, "CJKR"),
    Confusable_MA(PropertyType.Miscellaneous, "ConfMA"),
    Confusable_ML(PropertyType.Miscellaneous, "ConfML"),
    Confusable_SA(PropertyType.Miscellaneous, "ConfSA"),
    Confusable_SL(PropertyType.Miscellaneous, "ConfSL"),
    Emoji_DCM(PropertyType.Miscellaneous, "EDCM"),
    Emoji_KDDI(PropertyType.Miscellaneous, "EKDDI"),
    Emoji_SB(PropertyType.Miscellaneous, "ESB"),
    ISO_Comment(PropertyType.Miscellaneous, "isc"),
    Id_Mod_Status(PropertyType.Miscellaneous, "idmods"),
    Id_Mod_Type(PropertyType.Miscellaneous, "idmodt"),
    Idn_2008(PropertyType.Miscellaneous, "idn8"),
    Idn_Mapping(PropertyType.Miscellaneous, "idnm"),
    Idn_Status(PropertyType.Miscellaneous, "idns"),
    Jamo_Short_Name(PropertyType.Miscellaneous, "JSN"),
    Name(PropertyType.Miscellaneous, "na"),
    Name_Alias(PropertyType.Miscellaneous, "Name_Alias"),
    Named_Sequences(PropertyType.Miscellaneous, "NS"),
    Named_Sequences_Prov(PropertyType.Miscellaneous, "NSP"),
    Script_Extensions(PropertyType.Miscellaneous, "scx"),
    Standardized_Variant(PropertyType.Miscellaneous, "SV"),
    Unicode_1_Name(PropertyType.Miscellaneous, "na1"),
    kBigFive(PropertyType.Miscellaneous, "cjkBigFive"),
    kCCCII(PropertyType.Miscellaneous, "cjkCCCII"),
    kCNS1986(PropertyType.Miscellaneous, "cjkCNS1986"),
    kCNS1992(PropertyType.Miscellaneous, "cjkCNS1992"),
    kCangjie(PropertyType.Miscellaneous, "cjkCangjie"),
    kCantonese(PropertyType.Miscellaneous, "cjkCantonese"),
    kCheungBauer(PropertyType.Miscellaneous, "cjkCheungBauer"),
    kCheungBauerIndex(PropertyType.Miscellaneous, "cjkCheungBauerIndex"),
    kCihaiT(PropertyType.Miscellaneous, "cjkCihaiT"),
    kCowles(PropertyType.Miscellaneous, "cjkCowles"),
    kDaeJaweon(PropertyType.Miscellaneous, "cjkDaeJaweon"),
    kDefinition(PropertyType.Miscellaneous, "cjkDefinition"),
    kEACC(PropertyType.Miscellaneous, "cjkEACC"),
    kFenn(PropertyType.Miscellaneous, "cjkFenn"),
    kFennIndex(PropertyType.Miscellaneous, "cjkFennIndex"),
    kFourCornerCode(PropertyType.Miscellaneous, "cjkFourCornerCode"),
    kFrequency(PropertyType.Miscellaneous, "cjkFrequency"),
    kGB0(PropertyType.Miscellaneous, "cjkGB0"),
    kGB1(PropertyType.Miscellaneous, "cjkGB1"),
    kGB3(PropertyType.Miscellaneous, "cjkGB3"),
    kGB5(PropertyType.Miscellaneous, "cjkGB5"),
    kGB7(PropertyType.Miscellaneous, "cjkGB7"),
    kGB8(PropertyType.Miscellaneous, "cjkGB8"),
    kGSR(PropertyType.Miscellaneous, "cjkGSR"),
    kGradeLevel(PropertyType.Miscellaneous, "cjkGradeLevel"),
    kHDZRadBreak(PropertyType.Miscellaneous, "cjkHDZRadBreak"),
    kHKGlyph(PropertyType.Miscellaneous, "cjkHKGlyph"),
    kHKSCS(PropertyType.Miscellaneous, "cjkHKSCS"),
    kHanYu(PropertyType.Miscellaneous, "cjkHanYu"),
    kHangul(PropertyType.Miscellaneous, "cjkHangul"),
    kHanyuPinlu(PropertyType.Miscellaneous, "cjkHanyuPinlu"),
    kHanyuPinyin(PropertyType.Miscellaneous, "cjkHanyuPinyin"),
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
    kIRG_TSource(PropertyType.Miscellaneous, "cjkIRG_TSource"),
    kIRG_USource(PropertyType.Miscellaneous, "cjkIRG_USource"),
    kIRG_VSource(PropertyType.Miscellaneous, "cjkIRG_VSource"),
    kJIS0213(PropertyType.Miscellaneous, "cjkJIS0213"),
    kJapaneseKun(PropertyType.Miscellaneous, "cjkJapaneseKun"),
    kJapaneseOn(PropertyType.Miscellaneous, "cjkJapaneseOn"),
    kJis0(PropertyType.Miscellaneous, "cjkJis0"),
    kJis1(PropertyType.Miscellaneous, "cjkJis1"),
    kKPS0(PropertyType.Miscellaneous, "cjkKPS0"),
    kKPS1(PropertyType.Miscellaneous, "cjkKPS1"),
    kKSC0(PropertyType.Miscellaneous, "cjkKSC0"),
    kKSC1(PropertyType.Miscellaneous, "cjkKSC1"),
    kKangXi(PropertyType.Miscellaneous, "cjkKangXi"),
    kKarlgren(PropertyType.Miscellaneous, "cjkKarlgren"),
    kKorean(PropertyType.Miscellaneous, "cjkKorean"),
    kLau(PropertyType.Miscellaneous, "cjkLau"),
    kMainlandTelegraph(PropertyType.Miscellaneous, "cjkMainlandTelegraph"),
    kMandarin(PropertyType.Miscellaneous, "cjkMandarin"),
    kMatthews(PropertyType.Miscellaneous, "cjkMatthews"),
    kMeyerWempe(PropertyType.Miscellaneous, "cjkMeyerWempe"),
    kMorohashi(PropertyType.Miscellaneous, "cjkMorohashi"),
    kNelson(PropertyType.Miscellaneous, "cjkNelson"),
    kPhonetic(PropertyType.Miscellaneous, "cjkPhonetic"),
    kPseudoGB1(PropertyType.Miscellaneous, "cjkPseudoGB1"),
    kRSAdobe_Japan1_6(PropertyType.Miscellaneous, "cjkRSAdobe_Japan1_6"),
    kRSJapanese(PropertyType.Miscellaneous, "cjkRSJapanese"),
    kRSKanWa(PropertyType.Miscellaneous, "cjkRSKanWa"),
    kRSKangXi(PropertyType.Miscellaneous, "cjkRSKangXi"),
    kRSKorean(PropertyType.Miscellaneous, "cjkRSKorean"),
    kRSUnicode(PropertyType.Miscellaneous, "cjkRSUnicode"),
    kSBGY(PropertyType.Miscellaneous, "cjkSBGY"),
    kSemanticVariant(PropertyType.Miscellaneous, "cjkSemanticVariant"),
    kSimplifiedVariant(PropertyType.Miscellaneous, "cjkSimplifiedVariant"),
    kSpecializedSemanticVariant(PropertyType.Miscellaneous, "cjkSpecializedSemanticVariant"),
    kTaiwanTelegraph(PropertyType.Miscellaneous, "cjkTaiwanTelegraph"),
    kTang(PropertyType.Miscellaneous, "cjkTang"),
    kTotalStrokes(PropertyType.Miscellaneous, "cjkTotalStrokes"),
    kTraditionalVariant(PropertyType.Miscellaneous, "cjkTraditionalVariant"),
    kVietnamese(PropertyType.Miscellaneous, "cjkVietnamese"),
    kXHC1983(PropertyType.Miscellaneous, "cjkXHC1983"),
    kXerox(PropertyType.Miscellaneous, "cjkXerox"),
    kZVariant(PropertyType.Miscellaneous, "cjkZVariant"),

		// Catalog
    Age(PropertyType.Catalog, UcdPropertyValues.Age_Values.class, "age"),
    Block(PropertyType.Catalog, UcdPropertyValues.Block_Values.class, "blk"),
    Script(PropertyType.Catalog, UcdPropertyValues.Script_Values.class, "sc"),

		// Enumerated
    Bidi_Class(PropertyType.Enumerated, UcdPropertyValues.Bidi_Class_Values.class, "bc"),
    Bidi_Paired_Bracket_Type(PropertyType.Enumerated, UcdPropertyValues.Bidi_Paired_Bracket_Type_Values.class, "bpt"),
    Canonical_Combining_Class(PropertyType.Enumerated, UcdPropertyValues.Canonical_Combining_Class_Values.class, "ccc"),
    Decomposition_Type(PropertyType.Enumerated, UcdPropertyValues.Decomposition_Type_Values.class, "dt"),
    East_Asian_Width(PropertyType.Enumerated, UcdPropertyValues.East_Asian_Width_Values.class, "ea"),
    General_Category(PropertyType.Enumerated, UcdPropertyValues.General_Category_Values.class, "gc"),
    Grapheme_Cluster_Break(PropertyType.Enumerated, UcdPropertyValues.Grapheme_Cluster_Break_Values.class, "GCB"),
    Hangul_Syllable_Type(PropertyType.Enumerated, UcdPropertyValues.Hangul_Syllable_Type_Values.class, "hst"),
    Indic_Matra_Category(PropertyType.Enumerated, UcdPropertyValues.Indic_Matra_Category_Values.class, "InMC"),
    Indic_Syllabic_Category(PropertyType.Enumerated, UcdPropertyValues.Indic_Syllabic_Category_Values.class, "InSC"),
    Joining_Group(PropertyType.Enumerated, UcdPropertyValues.Joining_Group_Values.class, "jg"),
    Joining_Type(PropertyType.Enumerated, UcdPropertyValues.Joining_Type_Values.class, "jt"),
    Line_Break(PropertyType.Enumerated, UcdPropertyValues.Line_Break_Values.class, "lb"),
    NFC_Quick_Check(PropertyType.Enumerated, UcdPropertyValues.NFC_Quick_Check_Values.class, "NFC_QC"),
    NFD_Quick_Check(PropertyType.Enumerated, UcdPropertyValues.NFD_Quick_Check_Values.class, "NFD_QC"),
    NFKC_Quick_Check(PropertyType.Enumerated, UcdPropertyValues.NFKC_Quick_Check_Values.class, "NFKC_QC"),
    NFKD_Quick_Check(PropertyType.Enumerated, UcdPropertyValues.NFKD_Quick_Check_Values.class, "NFKD_QC"),
    Numeric_Type(PropertyType.Enumerated, UcdPropertyValues.Numeric_Type_Values.class, "nt"),
    Sentence_Break(PropertyType.Enumerated, UcdPropertyValues.Sentence_Break_Values.class, "SB"),
    Word_Break(PropertyType.Enumerated, UcdPropertyValues.Word_Break_Values.class, "WB"),

		// Binary
    ASCII_Hex_Digit(PropertyType.Binary, UcdPropertyValues.Binary.class, "AHex"),
    Alphabetic(PropertyType.Binary, UcdPropertyValues.Binary.class, "Alpha"),
    Bidi_Control(PropertyType.Binary, UcdPropertyValues.Binary.class, "Bidi_C"),
    Bidi_Mirrored(PropertyType.Binary, UcdPropertyValues.Binary.class, "Bidi_M"),
    Case_Ignorable(PropertyType.Binary, UcdPropertyValues.Binary.class, "CI"),
    Cased(PropertyType.Binary, UcdPropertyValues.Binary.class, "Cased"),
    Changes_When_Casefolded(PropertyType.Binary, UcdPropertyValues.Binary.class, "CWCF"),
    Changes_When_Casemapped(PropertyType.Binary, UcdPropertyValues.Binary.class, "CWCM"),
    Changes_When_Lowercased(PropertyType.Binary, UcdPropertyValues.Binary.class, "CWL"),
    Changes_When_NFKC_Casefolded(PropertyType.Binary, UcdPropertyValues.Binary.class, "CWKCF"),
    Changes_When_Titlecased(PropertyType.Binary, UcdPropertyValues.Binary.class, "CWT"),
    Changes_When_Uppercased(PropertyType.Binary, UcdPropertyValues.Binary.class, "CWU"),
    Composition_Exclusion(PropertyType.Binary, UcdPropertyValues.Binary.class, "CE"),
    Dash(PropertyType.Binary, UcdPropertyValues.Binary.class, "Dash"),
    Default_Ignorable_Code_Point(PropertyType.Binary, UcdPropertyValues.Binary.class, "DI"),
    Deprecated(PropertyType.Binary, UcdPropertyValues.Binary.class, "Dep"),
    Diacritic(PropertyType.Binary, UcdPropertyValues.Binary.class, "Dia"),
    Expands_On_NFC(PropertyType.Binary, UcdPropertyValues.Binary.class, "XO_NFC"),
    Expands_On_NFD(PropertyType.Binary, UcdPropertyValues.Binary.class, "XO_NFD"),
    Expands_On_NFKC(PropertyType.Binary, UcdPropertyValues.Binary.class, "XO_NFKC"),
    Expands_On_NFKD(PropertyType.Binary, UcdPropertyValues.Binary.class, "XO_NFKD"),
    Extender(PropertyType.Binary, UcdPropertyValues.Binary.class, "Ext"),
    Full_Composition_Exclusion(PropertyType.Binary, UcdPropertyValues.Binary.class, "Comp_Ex"),
    Grapheme_Base(PropertyType.Binary, UcdPropertyValues.Binary.class, "Gr_Base"),
    Grapheme_Extend(PropertyType.Binary, UcdPropertyValues.Binary.class, "Gr_Ext"),
    Grapheme_Link(PropertyType.Binary, UcdPropertyValues.Binary.class, "Gr_Link"),
    Hex_Digit(PropertyType.Binary, UcdPropertyValues.Binary.class, "Hex"),
    Hyphen(PropertyType.Binary, UcdPropertyValues.Binary.class, "Hyphen"),
    IDS_Binary_Operator(PropertyType.Binary, UcdPropertyValues.Binary.class, "IDSB"),
    IDS_Trinary_Operator(PropertyType.Binary, UcdPropertyValues.Binary.class, "IDST"),
    ID_Continue(PropertyType.Binary, UcdPropertyValues.Binary.class, "IDC"),
    ID_Start(PropertyType.Binary, UcdPropertyValues.Binary.class, "IDS"),
    Ideographic(PropertyType.Binary, UcdPropertyValues.Binary.class, "Ideo"),
    Join_Control(PropertyType.Binary, UcdPropertyValues.Binary.class, "Join_C"),
    Logical_Order_Exception(PropertyType.Binary, UcdPropertyValues.Binary.class, "LOE"),
    Lowercase(PropertyType.Binary, UcdPropertyValues.Binary.class, "Lower"),
    Math(PropertyType.Binary, UcdPropertyValues.Binary.class, "Math"),
    Noncharacter_Code_Point(PropertyType.Binary, UcdPropertyValues.Binary.class, "NChar"),
    Other_Alphabetic(PropertyType.Binary, UcdPropertyValues.Binary.class, "OAlpha"),
    Other_Default_Ignorable_Code_Point(PropertyType.Binary, UcdPropertyValues.Binary.class, "ODI"),
    Other_Grapheme_Extend(PropertyType.Binary, UcdPropertyValues.Binary.class, "OGr_Ext"),
    Other_ID_Continue(PropertyType.Binary, UcdPropertyValues.Binary.class, "OIDC"),
    Other_ID_Start(PropertyType.Binary, UcdPropertyValues.Binary.class, "OIDS"),
    Other_Lowercase(PropertyType.Binary, UcdPropertyValues.Binary.class, "OLower"),
    Other_Math(PropertyType.Binary, UcdPropertyValues.Binary.class, "OMath"),
    Other_Uppercase(PropertyType.Binary, UcdPropertyValues.Binary.class, "OUpper"),
    Pattern_Syntax(PropertyType.Binary, UcdPropertyValues.Binary.class, "Pat_Syn"),
    Pattern_White_Space(PropertyType.Binary, UcdPropertyValues.Binary.class, "Pat_WS"),
    Quotation_Mark(PropertyType.Binary, UcdPropertyValues.Binary.class, "QMark"),
    Radical(PropertyType.Binary, UcdPropertyValues.Binary.class, "Radical"),
    STerm(PropertyType.Binary, UcdPropertyValues.Binary.class, "STerm"),
    Soft_Dotted(PropertyType.Binary, UcdPropertyValues.Binary.class, "SD"),
    Terminal_Punctuation(PropertyType.Binary, UcdPropertyValues.Binary.class, "Term"),
    Unified_Ideograph(PropertyType.Binary, UcdPropertyValues.Binary.class, "UIdeo"),
    Uppercase(PropertyType.Binary, UcdPropertyValues.Binary.class, "Upper"),
    Variation_Selector(PropertyType.Binary, UcdPropertyValues.Binary.class, "VS"),
    White_Space(PropertyType.Binary, UcdPropertyValues.Binary.class, "WSpace"),
    XID_Continue(PropertyType.Binary, UcdPropertyValues.Binary.class, "XIDC"),
    XID_Start(PropertyType.Binary, UcdPropertyValues.Binary.class, "XIDS"),
		;

private final PropertyType type;
	private final PropertyNames<UcdProperty> names;
	// for enums
	private final NameMatcher name2enum;
	private final EnumSet enums;
	
	private UcdProperty(PropertyType type, String shortName, String...otherNames) {
		this.type = type;
		names = new PropertyNames(UcdProperty.class, this, shortName, otherNames);
		name2enum = null;
		enums = null;
	}
	private UcdProperty(PropertyType type, Class classItem, String shortName, String...otherNames) {
		this.type = type;
		names = new PropertyNames(UcdProperty.class, this, shortName, otherNames);
		enums = EnumSet.allOf(classItem);
		name2enum = PropertyNames.getNameToEnums(classItem);
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
