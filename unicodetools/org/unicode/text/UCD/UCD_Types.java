/**
 *******************************************************************************
 * Copyright (C) 1996-2001, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /home/cvsroot/unicodetools/org/unicode/text/UCD/UCD_Types.java,v $
 *
 *******************************************************************************
 */

package org.unicode.text.UCD;



public interface UCD_Types {
    static final int FIX_FOR_NEW_VERSION = 0;

    static final byte BINARY_FORMAT = 23; // bumped if binary format of UCD changes. Forces rebuild

    public static final char DOTTED_CIRCLE = '\u25CC';

    /**
     * Tangut but not Tangut Components, only assigned code points.
     * The end/limit of the range depends on the Unicode version,
     * see UCD.java mapToRepresentative().
     */
    public static final int TANGUT_BASE = 0x17000;
    // Unicode 9:
    // 17000;<Tangut Ideograph, First>;Lo;0;L;;;;;N;;;;;
    // 187EC;<Tangut Ideograph, Last>;Lo;0;L;;;;;N;;;;;

    public static final int
    // 4E00;<CJK Ideograph, First>;Lo;0;L;;;;;N;;;;;
    // 9FEA;<CJK Ideograph, Last>;Lo;0;L;;;;;N;;;;;
    CJK_BASE = 0x4E00,
    CJK_LIMIT = 0x9FEA+1,

    CJK_COMPAT_USED_BASE = 0xFA0E,
    CJK_COMPAT_USED_LIMIT = 0xFA2F+1,

    //3400;<CJK Ideograph Extension A, First>;Lo;0;L;;;;;N;;;;;
    //4DB5;<CJK Ideograph Extension A, Last>;Lo;0;L;;;;;N;;;;;

    CJK_A_BASE = 0x3400,
    CJK_A_LIMIT = 0x4DB5+1,

    //20000;<CJK Ideograph Extension B, First>;Lo;0;L;;;;;N;;;;;
    //2A6D6;<CJK Ideograph Extension B, Last>;Lo;0;L;;;;;N;;;;;

    CJK_B_BASE = 0x20000,
    CJK_B_LIMIT = 0x2A6D6+1,

    //2A700;<CJK Ideograph Extension C, First>;Lo;0;L;;;;;N;;;;;
    //2B734;<CJK Ideograph Extension C, Last>;Lo;0;L;;;;;N;;;;;

    CJK_C_BASE = 0x2A700,
    CJK_C_LIMIT = 0x2B734+1,

    //2B740;<CJK Ideograph Extension D, First>;Lo;0;L;;;;;N;;;;;
    //2B81D;<CJK Ideograph Extension D, Last>;Lo;0;L;;;;;N;;;;;

    CJK_D_BASE = 0x2B740,
    CJK_D_LIMIT = 0x2B81D+1,

    // 2B820;<CJK Ideograph Extension E, First>;Lo;0;L;;;;;N;;;;;
    // 2CEA1;<CJK Ideograph Extension E, Last>;Lo;0;L;;;;;N;;;;;
    CJK_E_BASE = 0x2B820,
    CJK_E_LIMIT = 0x2CEA1+1,

    // 2CEB0;<CJK Ideograph Extension F, First>;Lo;0;L;;;;;N;;;;;
    // 2EBE0;<CJK Ideograph Extension F, Last>;Lo;0;L;;;;;N;;;;;
    CJK_F_BASE = 0x2CEB0,
    CJK_F_LIMIT = 0x2EBE0+1

    // when adding to this list, look for all occurrences (in project) of CJK_C_BASE and CJK_C_LIMIT to check for code that needs changing.
    ;

    // Unicode Property Types
    static final byte
    NOT_DERIVED = 1,
    DERIVED_CORE = 2,
    DERIVED_NORMALIZATION = 4,
    DERIVED_ALL = 0x6,
    ALL = (byte)-1;

    static final byte
    NUMERIC_PROP = 0,
    STRING_PROP = 1,
    MISC_PROP = 2,
    CATALOG_PROP = 3,
    ENUMERATED_PROP = 4,
    BINARY_PROP = 5,
    FLATTENED_BINARY_PROP = 6,
    UNKNOWN_PROP = 7;

    /*
  0	Code value in 4-digit hexadecimal format.
  1	Unicode 2.1 Character Name. These names match exactly the
  2	General Category. This is a useful breakdown into various "character
  3	Canonical Combining Classes. The classes used for the
  4	Bidirectional Category. See the list below for an explanation of the
  5	Character Decomposition. In the Unicode Standard, not all of
  6	Decimal digit value. This is a numeric field. If the character
  7	Digit value. This is a numeric field. If the character represents a
  8	Numeric value. This is a numeric field. If the character has the
  9	If the characters has been identified as a "mirrored" character in
 10	Unicode 1.0 Name. This is the old name as published in Unicode 1.0.
 11	10646 Comment field. This field is informative.
 12	Upper case equivalent mapping. If a character is part of an
 13	Lower case equivalent mapping. Similar to 12. This field is informative.
 14	Title case equivalent mapping. Similar to 12. This field is informative.
     */


    // for IDs
    static final byte NUMBER = -2, SHORT = -1, NORMAL = 0, LONG = 1, BOTH = 2, EXTRA_ALIAS = 3;

    // Binary ENUM Grouping
    public static final int
    CATEGORY = 0,
    COMBINING_CLASS = 0x100,
    BIDI_CLASS = 0x200,
    DECOMPOSITION_TYPE = 0x300,
    NUMERIC_TYPE = 0x400,
    EAST_ASIAN_WIDTH = 0x500,
    LINE_BREAK = 0x600,
    JOINING_TYPE = 0x700,
    JOINING_GROUP = 0x800,
    BINARY_PROPERTIES = 0x900,
    SCRIPT = 0xA00,
    AGE = 0xB00,
    HANGUL_SYLLABLE_TYPE = 0xC00,
    DERIVED = 0xE00,
    LIMIT_ENUM = DERIVED + 0x100;

    public static final int LIMIT_COMBINING_CLASS = 256;

    // getCategory
    public static final byte
    UNASSIGNED		= 0,
    UPPERCASE_LETTER	= 1,
    LOWERCASE_LETTER	= 2,
    TITLECASE_LETTER	= 3,
    MODIFIER_LETTER		= 4,
    OTHER_LETTER		= 5,
    NON_SPACING_MARK	= 6,
    ENCLOSING_MARK		= 7,
    COMBINING_SPACING_MARK	= 8,
    DECIMAL_DIGIT_NUMBER	= 9,
    LETTER_NUMBER		= 10,
    OTHER_NUMBER		= 11,
    SPACE_SEPARATOR		= 12,
    LINE_SEPARATOR		= 13,
    PARAGRAPH_SEPARATOR	= 14,
    CONTROL			= 15,
    FORMAT			= 16,
    UNUSED_CATEGORY			= 17,
    PRIVATE_USE		= 18,
    SURROGATE		= 19,
    DASH_PUNCTUATION	= 20,
    START_PUNCTUATION	= 21,
    END_PUNCTUATION		= 22,
    CONNECTOR_PUNCTUATION	= 23,
    OTHER_PUNCTUATION	= 24,
    MATH_SYMBOL		= 25,
    CURRENCY_SYMBOL		= 26,
    MODIFIER_SYMBOL		= 27,
    OTHER_SYMBOL		= 28,
    INITIAL_PUNCTUATION	= 29,
    FINAL_PUNCTUATION		= 30,
    LIMIT_CATEGORY = FINAL_PUNCTUATION+1,

    // Unicode abbreviations
    Lu = UPPERCASE_LETTER,
    Ll = LOWERCASE_LETTER,
    Lt = TITLECASE_LETTER,
    Lm = MODIFIER_LETTER,
    Lo = OTHER_LETTER,
    Mn = NON_SPACING_MARK,
    Me = ENCLOSING_MARK,
    Mc = COMBINING_SPACING_MARK,
    Nd = DECIMAL_DIGIT_NUMBER,
    Nl = LETTER_NUMBER,
    No = OTHER_NUMBER,
    Zs = SPACE_SEPARATOR,
    Zl = LINE_SEPARATOR,
    Zp = PARAGRAPH_SEPARATOR,
    Cc = CONTROL,
    Cf = FORMAT,
    Cs = SURROGATE,
    Co = PRIVATE_USE,
    Cn = UNASSIGNED,
    Pc = CONNECTOR_PUNCTUATION,
    Pd = DASH_PUNCTUATION,
    Ps = START_PUNCTUATION,
    Pe = END_PUNCTUATION,
    Po = OTHER_PUNCTUATION,
    Pi = INITIAL_PUNCTUATION,
    Pf = FINAL_PUNCTUATION,
    Sm = MATH_SYMBOL,
    Sc = CURRENCY_SYMBOL,
    Sk = MODIFIER_SYMBOL,
    So = OTHER_SYMBOL;

    static final int
    LETTER_MASK = (1<<Lu) | (1<<Ll) | (1<<Lt) | (1<<Lm) | (1 << Lo),
    CASED_LETTER_MASK = (1<<Lu) | (1<<Ll) | (1<<Lt),
    MARK_MASK = (1<<Mn) | (1<<Me) | (1<<Mc),
    NUMBER_MASK = (1<<Nd) | (1<<Nl) | (1<<No),
    SEPARATOR_MASK = (1<<Zs) | (1<<Zl) | (1<<Zp),
    CONTROL_MASK = (1<<Cc) | (1<<Cf) | (1<<Cs) | (1<<Co),
    PUNCTUATION_MASK = (1<<Pc) | (1<<Pd) | (1<<Ps) | (1<<Pe) | (1<<Po) | (1<<Pi) | (1<<Pf),
    SYMBOL_MASK = (1<<Sm) | (1<<Sc) | (1<<Sk) | (1<<So),
    UNASSIGNED_MASK = (1<<Cn),
    BASE_MASK = LETTER_MASK | NUMBER_MASK | PUNCTUATION_MASK | SYMBOL_MASK | (1<<Mc),
    NONSPACING_MARK_MASK = (1<<Mn) | (1<<Me);


    // Binary Properties

    public static final byte
    BidiMirrored = 0,
    CompositionExclusion = 1,
    White_space = 2,
    Non_break = 3,
    Bidi_Control = 4,
    Join_Control = 5,
    Dash = 6,
    Hyphen = 7,
    Quotation_Mark = 8,
    Terminal_Punctuation = 9,
    Math_Property = 10,
    Hex_Digit = 11,
    ASCII_Hex_Digit = 12,
    Other_Alphabetic = 13,
    Ideographic = 14,
    Diacritic = 15,
    Extender = 16,
    Other_Lowercase = 17,
    Other_Uppercase = 18,
    Noncharacter_Code_Point = 19,
    CaseFoldTurkishI = 20,
    Other_GraphemeExtend = 21,
    GraphemeLink = 22,
    IDS_BinaryOperator = 23,
    IDS_TrinaryOperator = 24,
    Radical = 25,
    UnifiedIdeograph = 26,
    Other_Default_Ignorable_Code_Point = 27,
    Deprecated = 28,
    Soft_Dotted = 29,
    Logical_Order_Exception = 30,
    Other_ID_Start = 31,
    Sentence_Terminal = 32,
    Variation_Selector = 33,
    Other_ID_Continue = 34,
    Pattern_White_Space = 35,
    Pattern_Syntax = 36,
    Prepended_Concatenation_Mark = 37,
    Regional_Indicator = 38,
    LIMIT_BINARY_PROPERTIES = 39;

    /*
    static final int
	    BidiMirroredMask = 1<<BidiMirrored,
	    CompositionExclusionMask = 1<<CompositionExclusion,
	    AlphabeticMask = 1<<Other_Alphabetic,
	    Bidi_ControlMask = 1<<Bidi_Control,
        DashMask = 1<<Dash,
        DiacriticMask = 1<<Diacritic,
        ExtenderMask = 1<<Extender,
        Hex_DigitMask = 1<<Hex_Digit,
        HyphenMask = 1<<Hyphen,
        IdeographicMask = 1<<Ideographic,
        Join_ControlMask = 1<<Join_Control,
        Math_PropertyMask = 1<<Math_Property,
        Non_breakMask = 1<<Non_break,
        Noncharacter_Code_PointMask = 1<<Noncharacter_Code_Point,
        Other_LowercaseMask = 1<<Other_Lowercase,
        Other_UppercaseMask = 1<<Other_Uppercase,
        Quotation_MarkMask = 1<<Quotation_Mark,
        Terminal_PunctuationMask = 1<<Terminal_Punctuation,
        White_spaceMask = 1<<White_space;
     */

    // line break
    public static final byte
    LB_XX = 0, LB_OP = 1, LB_CL = 2, LB_QU = 3, LB_GL = 4, LB_NS = 5, LB_EX = 6, LB_SY = 7,
    LB_IS = 8, LB_PR = 9, LB_PO = 10, LB_NU = 11, LB_AL = 12, LB_ID = 13, LB_IN = 14, LB_HY = 15,
    LB_CM = 16, LB_BB = 17, LB_BA = 18, LB_SP = 19, LB_BK = 20, LB_CR = 21, LB_LF = 22, LB_CB = 23,
    LB_SA = 24, LB_AI = 25, LB_B2 = 26, LB_SG = 27, LB_ZW = 28,
    LB_NL = 29,
    LB_WJ = 30,
    LB_JL = 31,
    LB_JV = 32,
    LB_JT = 33,
    LB_H2 = 34,
    LB_H3 = 35,
    LB_CP = 36,
    LB_HL = 37,
    LB_CJ = 38,
    LB_RI = 39,
    LB_EB = 40,
    LB_EM = 41,
    LB_ZWJ = 42,
    LIMIT_LINE_BREAK = 43,
    LB_LIMIT = LIMIT_LINE_BREAK;

    // east asian width
    public static final byte
    EAN = 0, EAA = 1, EAH = 2, EAW = 3, EAF = 4, EANa = 5,
    LIMIT_EAST_ASIAN_WIDTH = 6;

    // bidi class
    static final byte
    BIDI_L = 0,     // Left-Right; Most alphabetic, syllabic, and logographic characters (e.g., CJK ideographs)
    BIDI_R = 1,     // Right-Left; Arabic, Hebrew, and punctuation specific to those scripts
    BIDI_EN = 2,    // European Number
    BIDI_ES = 3,    // European Number Separator
    BIDI_ET = 4,    // European Number Terminator
    BIDI_AN = 5,    // Arabic Number
    BIDI_CS = 6,    // Common Number Separator
    BIDI_B = 7,     // Block Separator
    BIDI_S = 8,     // Segment Separator
    BIDI_WS = 9,    // Whitespace
    BIDI_ON = 10,   // Other Neutrals ; All other characters: punctuation, symbols
    LIMIT_BIDI_2 = 11,
    BIDI_UNUSED = 11,
    BIDI_BN = 12,
    BIDI_NSM = 13,
    BIDI_AL = 14,
    BIDI_LRO = 15,
    BIDI_RLO = 16,
    BIDI_LRE = 17,
    BIDI_RLE = 18,
    BIDI_PDF = 19,
    BIDI_LRI = 20,
    BIDI_RLI = 21,
    BIDI_FSI = 22,
    BIDI_PDI = 23,
    LIMIT_BIDI_CLASS = 24;

    // decompositionType
    static final byte
    NONE = 0,
    CANONICAL = 1,
    COMPATIBILITY = 2,
    COMPAT_UNSPECIFIED = 2,	// Otherwise unspecified compatibility character.
    COMPAT_FONT = 3,		// A font variant (e.g. a blackletter form).
    COMPAT_NOBREAK = 4,	// A no-break version of a space or hyphen.
    COMPAT_INITIAL = 5,	// // An initial presentation form (Arabic).
    COMPAT_MEDIAL = 6,	// // A medial presentation form (Arabic).
    COMPAT_FINAL = 7,	// // 	A final presentation form (Arabic).
    COMPAT_ISOLATED = 8,	// An isolated presentation form (Arabic).
    COMPAT_CIRCLE = 9,	// An encircled form.
    COMPAT_SUPER = 10,	// 	A superscript form.
    COMPAT_SUB = 11,	// 	A subscript form.
    COMPAT_VERTICAL = 12,	// A vertical layout presentation form.
    COMPAT_WIDE = 13,	// 	A wide (or zenkaku) compatibility character.
    COMPAT_NARROW = 14,	// A narrow (or hankaku) compatibility character.
    COMPAT_SMALL = 15,	// 	A small variant form (CNS compatibility).
    COMPAT_SQUARE = 16,	// A CJK squared font variant.
    COMPAT_FRACTION = 17,	// A vulgar fraction form.
    LIMIT_DECOMPOSITION_TYPE = 18;

    // mirrored type
    static final byte NO = 0, YES = 1, LIMIT_MIRRORED = 2;

    // for QuickCheck
    static final byte QNO = 0, QMAYBE = 1, QYES = 2;

    // case type
    static final byte LOWER = 0, TITLE = 1, UPPER = 2, UNCASED = 3, FOLD = 3, LIMIT_CASE = 4;
    static final byte SIMPLE = 0, FULL = 8;

    // normalization type
    static final byte UNNORMALIZED = 0, C = 1, KC = 2, D = 3, KD = 4, FORM_LIMIT = 5;

    // numericType
    static final byte NUMERIC_NONE = 0, NUMERIC = 1, DIGIT = 2, DECIMAL = 3,
            LIMIT_NUMERIC_TYPE = 4;
    //        HAN_PRIMARY = 4, HAN_ACCOUNTING = 5, HAN_OTHER = 6,
    // WARNING, reset to 7 if all properties desired!!

    static final byte NA = 0, L = 1, V = 2, T = 3, LV = 4, LVT = 5,
            HANGUL_SYLLABLE_TYPE_LIMIT = 6;

    public static final short // SCRIPT CODE
    COMMON_SCRIPT = 0,
    LATIN_SCRIPT = 1,
    GREEK_SCRIPT = 2,
    CYRILLIC_SCRIPT = 3,
    ARMENIAN_SCRIPT = 4,
    HEBREW_SCRIPT = 5,
    ARABIC_SCRIPT = 6,
    SYRIAC_SCRIPT = 7,
    THAANA_SCRIPT = 8,
    DEVANAGARI_SCRIPT = 9,
    BENGALI_SCRIPT = 10,
    GURMUKHI_SCRIPT = 11,
    GUJARATI_SCRIPT = 12,
    ORIYA_SCRIPT = 13,
    TAMIL_SCRIPT = 14,
    TELUGU_SCRIPT = 15,
    KANNADA_SCRIPT = 16,
    MALAYALAM_SCRIPT = 17,
    SINHALA_SCRIPT = 18,
    THAI_SCRIPT = 19,
    LAO_SCRIPT = 20,
    TIBETAN_SCRIPT = 21,
    MYANMAR_SCRIPT = 22,
    GEORGIAN_SCRIPT = 23,
    UNUSED_SCRIPT = 24,
    HANGUL_SCRIPT = 25,
    ETHIOPIC_SCRIPT = 26,
    CHEROKEE_SCRIPT = 27,
    ABORIGINAL_SCRIPT = 28,
    OGHAM_SCRIPT = 29,
    RUNIC_SCRIPT = 30,
    KHMER_SCRIPT = 31,
    MONGOLIAN_SCRIPT = 32,
    HIRAGANA_SCRIPT = 33,
    KATAKANA_SCRIPT = 34,
    BOPOMOFO_SCRIPT = 35,
    HAN_SCRIPT = 36,
    YI_SCRIPT = 37,
    OLD_ITALIC_SCRIPT = 38,
    GOTHIC_SCRIPT = 39,
    DESERET_SCRIPT = 40,
    INHERITED_SCRIPT = 41,
    TAGALOG_SCRIPT = 42,
    HANUNOO_SCRIPT = 43,
    BUHID_SCRIPT = 44,
    TAGBANWA_SCRIPT = 45,
    LIMBU = 46,
    TAI_LE = 47,
    LINEAR_B = 48,
    UGARITIC = 49,
    SHAVIAN = 50,
    OSMANYA = 51,
    CYPRIOT = 52,
    BRAILLE = 53,
    KATAKANA_OR_HIRAGANA = 54,
    BUGINESE = 55,
    COPTIC = 56,
    NEW_TAI_LUE = 57,
    GLAGOLITIC = 58,
    TIFINAGH = 59,
    SYLOTI_NAGRI = 60,
    OLD_PERSIAN = 61,
    KHAROSHTHI = 62,
    Balinese = 63,
    Cuneiform = 64,
    Phoenician = 65,
    Phags_Pa = 66,
    NKo = 67,
    Unknown_Script = 68,
    Sundanese = 69,
    Lepcha = 70,
    Ol_Chiki = 71,
    Vai = 72,
    Saurashtra = 73,
    Kayah_Li = 74,
    Rejang = 75,
    Lycian = 76,
    Carian = 77,
    Lydian= 78,
    Cham = 79,
    Tai_Tham = 80,
    Tai_Viet = 81,
    Avestan = 82,
    Egyptian_Hieroglyphs = 83,
    Samaritan = 84,
    Lisu = 85,
    Bamum = 86,
    Javanese = 87,
    Meetei_Mayek = 88,
    Imperial_Aramaic = 89,
    Old_South_Arabian = 90,
    Inscriptional_Parthian = 91,
    Inscriptional_Pahlavi = 92,
    Old_Turkic = 93,
    Kaithi = 94,
    Batak = 95,
    Brahmi = 96,
    Mandaic = 97,
    Chakma = 98,
    Meroitic_Cursive = 99,
    Meroitic_Hieroglyphs = 100,
    Miao = 101,
    Sharada = 102,
    Sora_Sompeng = 103,
    Takri = 104,
    Caucasian_Albanian = 105,
    Bassa_Vah = 106,
    Duployan = 107,
    Elbasan = 108,
    Grantha = 109,
    Pahawh_Hmong = 110,
    Khojki = 111,
    Linear_A = 112,
    Mahajani = 113,
    Manichaean = 114,
    Mende_Kikakui = 115,
    Modi = 116,
    Mro = 117,
    Old_North_Arabian = 118,
    Nabataean = 119,
    Palmyrene = 120,
    Pau_Cin_Hau = 121,
    Old_Permic = 122,
    Psalter_Pahlavi = 123,
    Siddham = 124,
    Khudawadi = 125,
    Tirhuta = 126,
    Warang_Citi = 127,
    Ahom = 128,
    Anatolian_Hieroglyphs = 129,
    Hatran = 130,
    Multani = 131,
    Old_Hungarian = 132,
    SignWriting = 133,  // Sutton SignWriting, in CamelCase without underscore
    // Unicode 9
    Adlam = 134,
    Bhaiksuki = 135,
    Marchen = 136,
    Newa = 137,
    Osage = 138,
    Tangut = 139,
    // Unicode 10
    Masaram_Gondi = 140,
    Nushu = 141,
    Soyombo = 142,
    Zanabazar_Square = 143,
    // Unicode 11
    Dogra = 144,
    Gunjala_Gondi = 145,
    Makasar = 146,
    Medefaidrin = 147,
    Hanifi_Rohingya = 148,
    Sogdian = 149,
    Old_Sogdian = 150,
    // Unicode 12
    Elymaic = 151,
    Nandinagari = 152,
    Nyiakeng_Puachue_Hmong = 153,
    Wancho = 154,
    LIMIT_SCRIPT = Wancho + 1;

    // Bidi_Paired_Bracket_Type
    public static final byte
    BPT_N = 0, BPT_O = 1, BPT_C = 2, LIMIT_BPT = 3;

    // Vertical_Orientation
    public static final byte
    VO_R = 0, VO_TR = 1, VO_TU = 2, VO_U = 3, LIMIT_VO = 4;

    static final int  // TODO: change to using an enum
    UNKNOWN = 0,
    AGE11 = 1,
    AGE20 = 2,
    AGE21 = 3,
    AGE30 = 4,
    AGE31 = 5,
    AGE32 = 6,
    AGE40 = 7,
    AGE41 = 8,
    AGE50 = 9,
    AGE51 = 10,
    AGE52 = 11,
    AGE60 = 12,
    AGE61 = 13,
    AGE62 = 14,
    AGE63 = 15,
    AGE70 = 16,
    AGE80 = 17,
    AGE90 = 18,
    AGE100 = 19,
    AGE110 = 20,
    AGE120 = 21,
    LIMIT_AGE = AGE120 + 1; // + FIX_FOR_NEW_VERSION;

    static final String[] AGE_VERSIONS = {
            "?",
            "1.1.0",
            "2.0.0",
            "2.1.2",
            "3.0.0",
            "3.1.0",
            "3.2.0",
            "4.0.0",
            "4.1.0",
            "5.0.0",
            "5.1.0",
            "5.2.0",
            "6.0.0",
            "6.1.0",
            "6.2.0",
            "6.3.0",
            "7.0.0",
            "8.0.0",
            "9.0.0",
            "10.0.0",
            "11.0.0",
            "12.0.0",
    };

    public static byte
    JT_C = 0,
    JT_D = 1,
    JT_R = 2,
    JT_U = 3,
    JT_L = 4,
    JT_T = 5,
    LIMIT_JOINING_TYPE = 6;

    public static short
    NO_SHAPING = 0,
    AIN = 1,
    ALAPH = 2,
    ALEF = 3,
    BEH = 4,
    BETH = 5,
    DAL = 6,
    DALATH_RISH = 7,
    E = 8,
    FEH = 9,
    FINAL_SEMKATH = 10,
    GAF = 11,
    GAMAL = 12,
    HAH = 13,
    TEH_MARBUTA_GOAL = 14,
    HE = 15,
    HEH = 16,
    HEH_GOAL = 17,
    HETH = 18,
    KAF = 19,
    KAPH = 20,
    KNOTTED_HEH = 21,
    LAM = 22,
    LAMADH = 23,
    MEEM = 24,
    MIM = 25,
    NOON = 26,
    NUN = 27,
    PE = 28,
    QAF = 29,
    QAPH = 30,
    REH = 31,
    REVERSED_PE = 32,
    SAD = 33,
    SADHE = 34,
    SEEN = 35,
    SEMKATH = 36,
    SHIN = 37,
    SWASH_KAF = 38,
    TAH = 39,
    TAW = 40,
    TEH_MARBUTA = 41,
    TETH = 42,
    WAW = 43,
    SYRIAC_WAW = 44,
    YEH = 45,
    YEH_BARREE = 46,
    YEH_WITH_TAIL = 47,
    YUDH = 48,
    YUDH_HE = 49,
    ZAIN = 50,
    ZHAIN = 51,
    KHAPH = 52,
    FE = 53,
    BURUSHASKI_YEH_BARREE = 54,
    FARSI_YEH = 55,
    NYA = 56,
    ROHINGYA_YEH = 57,
    HAMZAH_ON_HA_GOAL = 58, 
    STRAIGHT_WAW= 59,
    MANICHAEAN_ALEPH= 60,
    MANICHAEAN_AYIN= 61,
    MANICHAEAN_BETH= 62,
    MANICHAEAN_DALETH= 63,
    MANICHAEAN_DHAMEDH= 64,
    MANICHAEAN_FIVE= 65,
    MANICHAEAN_GIMEL= 66,
    MANICHAEAN_HETH= 67,
    MANICHAEAN_HUNDRED= 68,
    MANICHAEAN_KAPH= 69,
    MANICHAEAN_LAMEDH= 70,
    MANICHAEAN_MEM= 71,
    MANICHAEAN_NUN= 72,
    MANICHAEAN_ONE= 73,
    MANICHAEAN_PE= 74,
    MANICHAEAN_QOPH= 75,
    MANICHAEAN_RESH= 76,
    MANICHAEAN_SADHE= 77,
    MANICHAEAN_SAMEKH= 78,
    MANICHAEAN_TAW= 79,
    MANICHAEAN_TEN= 80,
    MANICHAEAN_TETH= 81,
    MANICHAEAN_THAMEDH= 82,
    MANICHAEAN_TWENTY= 83,
    MANICHAEAN_WAW= 84,
    MANICHAEAN_YODH= 85,
    MANICHAEAN_ZAYIN= 86,
    // Unicode 9:
    AFRICAN_FEH = 87,
    AFRICAN_QAF = 88,
    AFRICAN_NOON = 89,
    // Unicode 10:
    MALAYALAM_NGA = 90,
    MALAYALAM_JA = 91,
    MALAYALAM_NYA = 92,
    MALAYALAM_TTA = 93,
    MALAYALAM_NNA = 94,
    MALAYALAM_NNNA = 95,
    MALAYALAM_BHA = 96,
    MALAYALAM_RA = 97,
    MALAYALAM_LLA = 98,
    MALAYALAM_LLLA = 99,
    MALAYALAM_SSA = 100,
    // Unicode 11, non-singletons:
    Hanifi_Rohingya_Pa = 101,
    Hanifi_Rohingya_Kinna_Ya = 102,
    // limit
    LIMIT_JOINING_GROUP = Hanifi_Rohingya_Kinna_Ya + 1;

    static final byte NFD = 0, NFC = 1, NFKD = 2, NFKC = 3;
    public static final int
    NF_COMPATIBILITY_MASK = 2,
    NF_COMPOSITION_MASK = 1;

    // DERIVED PROPERTY

    static final byte
    PropMath = 0,
    PropAlphabetic = 1,
    PropLowercase = 2,
    PropUppercase = 3,

    ID_Start = 4,
    ID_Continue_NO_Cf = 5,

    Mod_ID_Start = 6,
    Mod_ID_Continue_NO_Cf = 7,

    Missing_Uppercase = 8,
    Missing_Lowercase = 9,
    Missing_Mixedcase = 10,

    FC_NFKC_Closure = 11,

    FullCompExclusion = 12,
    FullCompInclusion = 13,

    QuickNFD = 14,
    QuickNFC = 15,
    QuickNFKD = 16,
    QuickNFKC = 17,

    ExpandsOnNFD = 18,
    ExpandsOnNFC = 19,
    ExpandsOnNFKD = 20,
    ExpandsOnNFKC = 21,

    GenNFD = 22,
    GenNFC = 23,
    GenNFKD = 24,
    GenNFKC = 25,

    DefaultIgnorable = 26,
    GraphemeExtend = 27,
    GraphemeBase = 28,

    FC_NFC_Closure = 29,

    Other_Case_Ignorable = 30,
    Case_Ignorable = 31,
    Type_i = 32,

    NFC_Leading = 33,
    NFC_TrailingNonZero = 34,
    NFC_TrailingZero = 35,
    NFC_Resulting = 36,

    NFD_UnsafeStart = 37,
    NFC_UnsafeStart = 38,
    NFKD_UnsafeStart = 39,
    NFKC_UnsafeStart = 40,

    NFD_Skippable = 41,
    NFC_Skippable = 42,
    NFKD_Skippable = 43,
    NFKC_Skippable = 44,

    Case_Sensitive = 45,

    DERIVED_PROPERTY_LIMIT = 46;

}