package org.unicode.props;

public class Properties {
    enum UcdProperty {
        // Numeric
        Numeric_Value,  kAccountingNumeric, kOtherNumeric,  kPrimaryNumeric,    
        // String
        Bidi_Mirroring_Glyph,   Case_Folding,   Decomposition_Mapping,  FC_NFKC_Closure,    Lowercase_Mapping,  NFKC_Casefold,  Simple_Case_Folding,    Simple_Lowercase_Mapping,   
        Simple_Titlecase_Mapping,   Simple_Uppercase_Mapping,   Titlecase_Mapping,  Uppercase_Mapping,  kCompatibilityVariant,  
        // Miscellaneous
        ISO_Comment,    Jamo_Short_Name,    Name,   Name_Alias, Unicode_1_Name, kIICore,    kIRG_GSource,   kIRG_HSource,   
        kIRG_JSource,   kIRG_KPSource,  kIRG_KSource,   kIRG_MSource,   kIRG_TSource,   kIRG_USource,   kIRG_VSource,   kRSUnicode, 
        // Catalog
        Age,    Block,  Script, 
        // Enumerated
        Bidi_Class, Canonical_Combining_Class,  Decomposition_Type, East_Asian_Width,   General_Category,   Grapheme_Cluster_Break, Hangul_Syllable_Type,   Joining_Group,  
        Joining_Type,   Line_Break, NFC_Quick_Check,    NFD_Quick_Check,    NFKC_Quick_Check,   NFKD_Quick_Check,   Numeric_Type,   Sentence_Break, Word_Break, 
        // Binary
        ASCII_Hex_Digit,    Alphabetic, Bidi_Control,   Bidi_Mirrored,  Case_Ignorable, Cased,  Changes_When_Casefolded,    Changes_When_Casemapped,    
        Changes_When_Lowercased,    Changes_When_NFKC_Casefolded,   Changes_When_Titlecased,    Changes_When_Uppercased,    Composition_Exclusion,  Dash,   Default_Ignorable_Code_Point,   Deprecated, Diacritic,  
        Expands_On_NFC, Expands_On_NFD, Expands_On_NFKC,    Expands_On_NFKD,    Extender,   Full_Composition_Exclusion, Grapheme_Base,  Grapheme_Extend,    Grapheme_Link,  
        Hex_Digit,  Hyphen, IDS_Binary_Operator,    IDS_Trinary_Operator,   ID_Continue,    ID_Start,   Ideographic,    Join_Control,   Logical_Order_Exception,    
        Lowercase,  Math,   Noncharacter_Code_Point,    Other_Alphabetic,   Other_Default_Ignorable_Code_Point, Other_Grapheme_Extend,  Other_ID_Continue,  Other_ID_Start, Other_Lowercase,    
        Other_Math, Other_Uppercase,    Pattern_Syntax, Pattern_White_Space,    Quotation_Mark, Radical,    STerm,  Soft_Dotted,    Terminal_Punctuation,   
        Unified_Ideograph,  Uppercase,  Variation_Selector, White_Space,    XID_Continue,   XID_Start,  
        ;
        static final UcdProperty
        AHex = ASCII_Hex_Digit,
        age = Age,
        Alpha = Alphabetic,
        bc = Bidi_Class,
        Bidi_C = Bidi_Control,
        Bidi_M = Bidi_Mirrored,
        bmg = Bidi_Mirroring_Glyph,
        blk = Block,
        ccc = Canonical_Combining_Class,
        cf = Case_Folding,
        CI = Case_Ignorable,
        CWCF = Changes_When_Casefolded,
        CWCM = Changes_When_Casemapped,
        CWL = Changes_When_Lowercased,
        CWKCF = Changes_When_NFKC_Casefolded,
        CWT = Changes_When_Titlecased,
        CWU = Changes_When_Uppercased,
        CE = Composition_Exclusion,
        dm = Decomposition_Mapping,
        dt = Decomposition_Type,
        DI = Default_Ignorable_Code_Point,
        Dep = Deprecated,
        Dia = Diacritic,
        ea = East_Asian_Width,
        XO_NFC = Expands_On_NFC,
        XO_NFD = Expands_On_NFD,
        XO_NFKC = Expands_On_NFKC,
        XO_NFKD = Expands_On_NFKD,
        Ext = Extender,
        FC_NFKC = FC_NFKC_Closure,
        Comp_Ex = Full_Composition_Exclusion,
        gc = General_Category,
        Gr_Base = Grapheme_Base,
        GCB = Grapheme_Cluster_Break,
        Gr_Ext = Grapheme_Extend,
        Gr_Link = Grapheme_Link,
        hst = Hangul_Syllable_Type,
        Hex = Hex_Digit,
        IDSB = IDS_Binary_Operator,
        IDST = IDS_Trinary_Operator,
        IDC = ID_Continue,
        IDS = ID_Start,
        isc = ISO_Comment,
        Ideo = Ideographic,
        JSN = Jamo_Short_Name,
        Join_C = Join_Control,
        jg = Joining_Group,
        jt = Joining_Type,
        lb = Line_Break,
        LOE = Logical_Order_Exception,
        Lower = Lowercase,
        lc = Lowercase_Mapping,
        NFC_QC = NFC_Quick_Check,
        NFD_QC = NFD_Quick_Check,
        NFKC_CF = NFKC_Casefold,
        NFKC_QC = NFKC_Quick_Check,
        NFKD_QC = NFKD_Quick_Check,
        na = Name,
        NChar = Noncharacter_Code_Point,
        nt = Numeric_Type,
        nv = Numeric_Value,
        OAlpha = Other_Alphabetic,
        ODI = Other_Default_Ignorable_Code_Point,
        OGr_Ext = Other_Grapheme_Extend,
        OIDC = Other_ID_Continue,
        OIDS = Other_ID_Start,
        OLower = Other_Lowercase,
        OMath = Other_Math,
        OUpper = Other_Uppercase,
        Pat_Syn = Pattern_Syntax,
        Pat_WS = Pattern_White_Space,
        QMark = Quotation_Mark,
        sc = Script,
        SB = Sentence_Break,
        scf = Simple_Case_Folding,
        sfc = Simple_Case_Folding,
        slc = Simple_Lowercase_Mapping,
        stc = Simple_Titlecase_Mapping,
        suc = Simple_Uppercase_Mapping,
        SD = Soft_Dotted,
        Term = Terminal_Punctuation,
        tc = Titlecase_Mapping,
        na1 = Unicode_1_Name,
        UIdeo = Unified_Ideograph,
        Upper = Uppercase,
        uc = Uppercase_Mapping,
        VS = Variation_Selector,
        WSpace = White_Space,
        space = White_Space,
        WB = Word_Break,
        XIDC = XID_Continue,
        XIDS = XID_Start,
        cjkAccountingNumeric = kAccountingNumeric,
        cjkCompatibilityVariant = kCompatibilityVariant,
        cjkIICore = kIICore,
        cjkIRG_GSource = kIRG_GSource,
        cjkIRG_HSource = kIRG_HSource,
        cjkIRG_JSource = kIRG_JSource,
        cjkIRG_KPSource = kIRG_KPSource,
        cjkIRG_KSource = kIRG_KSource,
        cjkIRG_MSource = kIRG_MSource,
        cjkIRG_TSource = kIRG_TSource,
        cjkIRG_USource = kIRG_USource,
        cjkIRG_VSource = kIRG_VSource,
        cjkOtherNumeric = kOtherNumeric,
        cjkPrimaryNumeric = kPrimaryNumeric,
        cjkRSUnicode = kRSUnicode,
        Unicode_Radical_Stroke = kRSUnicode,
        URS = kRSUnicode;
    }
    enum ASCII_Hex_Digit {
        No, Yes;
        static final ASCII_Hex_Digit
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum Age {
        _1_1, _2_0, _2_1, _3_0, _3_1, _3_2, _4_0, _4_1, _5_0, _5_1, _5_2, _6_0, _6_1, unassigned
    }
    enum Alphabetic {
        No, Yes;
        static final Alphabetic
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum Bidi_Class {
        Arabic_Letter, Arabic_Number, Paragraph_Separator, Boundary_Neutral, Common_Separator, European_Number, European_Separator, European_Terminator, Left_To_Right, Left_To_Right_Embedding, Left_To_Right_Override, Nonspacing_Mark, Other_Neutral, Pop_Directional_Format, Right_To_Left, Right_To_Left_Embedding, Right_To_Left_Override, Segment_Separator, White_Space;
        static final Bidi_Class
        AL=Arabic_Letter,
        AN=Arabic_Number,
        B=Paragraph_Separator,
        BN=Boundary_Neutral,
        CS=Common_Separator,
        EN=European_Number,
        ES=European_Separator,
        ET=European_Terminator,
        L=Left_To_Right,
        LRE=Left_To_Right_Embedding,
        LRO=Left_To_Right_Override,
        NSM=Nonspacing_Mark,
        ON=Other_Neutral,
        PDF=Pop_Directional_Format,
        R=Right_To_Left,
        RLE=Right_To_Left_Embedding,
        RLO=Right_To_Left_Override,
        S=Segment_Separator,
        WS=White_Space;
    }
    enum Bidi_Control {
        No, Yes;
        static final Bidi_Control
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum Bidi_Mirrored {
        No, Yes;
        static final Bidi_Mirrored
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    // Bidi_Mirroring_Glyph
    enum Block {
        Aegean_Numbers, Alchemical_Symbols, Alphabetic_Presentation_Forms, Ancient_Greek_Musical_Notation, Ancient_Greek_Numbers, Ancient_Symbols, Arabic, Arabic_Extended_A, Arabic_Mathematical_Alphabetic_Symbols, Arabic_Presentation_Forms_A, Arabic_Presentation_Forms_B, Arabic_Supplement, Armenian, Arrows, Avestan, Balinese, Bamum, Bamum_Supplement, Basic_Latin, Batak, Bengali, Block_Elements, Bopomofo, Bopomofo_Extended, Box_Drawing, Brahmi, Braille_Patterns, Buginese, Buhid, Byzantine_Musical_Symbols, Carian, Chakma, Cham, Cherokee, CJK_Compatibility, CJK_Compatibility_Forms, CJK_Compatibility_Ideographs, CJK_Compatibility_Ideographs_Supplement, CJK_Radicals_Supplement, CJK_Strokes, CJK_Symbols_And_Punctuation, CJK_Unified_Ideographs, CJK_Unified_Ideographs_Extension_A, CJK_Unified_Ideographs_Extension_B, CJK_Unified_Ideographs_Extension_C, CJK_Unified_Ideographs_Extension_D, Combining_Diacritical_Marks, Combining_Diacritical_Marks_For_Symbols, Combining_Diacritical_Marks_Supplement, Combining_Half_Marks, Common_Indic_Number_Forms, Control_Pictures, Coptic, Counting_Rod_Numerals, Cuneiform, Cuneiform_Numbers_And_Punctuation, Currency_Symbols, Cypriot_Syllabary, Cyrillic, Cyrillic_Extended_A, Cyrillic_Extended_B, Cyrillic_Supplement, Deseret, Devanagari, Devanagari_Extended, Dingbats, Domino_Tiles, Egyptian_Hieroglyphs, Emoticons, Enclosed_Alphanumeric_Supplement, Enclosed_Alphanumerics, Enclosed_CJK_Letters_And_Months, Enclosed_Ideographic_Supplement, Ethiopic, Ethiopic_Extended, Ethiopic_Extended_A, Ethiopic_Supplement, General_Punctuation, Geometric_Shapes, Georgian, Georgian_Supplement, Glagolitic, Gothic, Greek_And_Coptic, Greek_Extended, Gujarati, Gurmukhi, Halfwidth_And_Fullwidth_Forms, Hangul_Compatibility_Jamo, Hangul_Jamo, Hangul_Jamo_Extended_A, Hangul_Jamo_Extended_B, Hangul_Syllables, Hanunoo, Hebrew, High_Private_Use_Surrogates, High_Surrogates, Hiragana, Ideographic_Description_Characters, Imperial_Aramaic, Inscriptional_Pahlavi, Inscriptional_Parthian, IPA_Extensions, Javanese, Kaithi, Kana_Supplement, Kanbun, Kangxi_Radicals, Kannada, Katakana, Katakana_Phonetic_Extensions, Kayah_Li, Kharoshthi, Khmer, Khmer_Symbols, Lao, Latin_1_Supplement, Latin_Extended_A, Latin_Extended_Additional, Latin_Extended_B, Latin_Extended_C, Latin_Extended_D, Lepcha, Letterlike_Symbols, Limbu, Linear_B_Ideograms, Linear_B_Syllabary, Lisu, Low_Surrogates, Lycian, Lydian, Mahjong_Tiles, Malayalam, Mandaic, Mathematical_Alphanumeric_Symbols, Mathematical_Operators, Meetei_Mayek, Meetei_Mayek_Extensions, Meroitic_Cursive, Meroitic_Hieroglyphs, Miao, Miscellaneous_Mathematical_Symbols_A, Miscellaneous_Mathematical_Symbols_B, Miscellaneous_Symbols, Miscellaneous_Symbols_And_Arrows, Miscellaneous_Symbols_And_Pictographs, Miscellaneous_Technical, Modifier_Tone_Letters, Mongolian, Musical_Symbols, Myanmar, Myanmar_Extended_A, New_Tai_Lue, NKo, No_Block, Number_Forms, Ogham, Ol_Chiki, Old_Italic, Old_Persian, Old_South_Arabian, Old_Turkic, Optical_Character_Recognition, Oriya, Osmanya, Phags_Pa, Phaistos_Disc, Phoenician, Phonetic_Extensions, Phonetic_Extensions_Supplement, Playing_Cards, Private_Use_Area, Rejang, Rumi_Numeral_Symbols, Runic, Samaritan, Saurashtra, Sharada, Shavian, Sinhala, Small_Form_Variants, Sora_Sompeng, Spacing_Modifier_Letters, Specials, Sundanese, Sundanese_Supplement, Superscripts_And_Subscripts, Supplemental_Arrows_A, Supplemental_Arrows_B, Supplemental_Mathematical_Operators, Supplemental_Punctuation, Supplementary_Private_Use_Area_A, Supplementary_Private_Use_Area_B, Syloti_Nagri, Syriac, Tagalog, Tagbanwa, Tags, Tai_Le, Tai_Tham, Tai_Viet, Tai_Xuan_Jing_Symbols, Takri, Tamil, Telugu, Thaana, Thai, Tibetan, Tifinagh, Transport_And_Map_Symbols, Ugaritic, Unified_Canadian_Aboriginal_Syllabics, Unified_Canadian_Aboriginal_Syllabics_Extended, Vai, Variation_Selectors, Variation_Selectors_Supplement, Vedic_Extensions, Vertical_Forms, Yi_Radicals, Yi_Syllables, Yijing_Hexagram_Symbols;
        static final Block
        ASCII=Basic_Latin,
        Combining_Marks_For_Symbols=Combining_Diacritical_Marks_For_Symbols,
        Cyrillic_Supplementary=Cyrillic_Supplement,
        Greek=Greek_And_Coptic,
        Latin_1=Latin_1_Supplement,
        Private_Use=Private_Use_Area,
        Canadian_Syllabics=Unified_Canadian_Aboriginal_Syllabics;
    }
    enum Canonical_Combining_Class {
        NR, OV, NK, KV, VR, ATBL, ATB, ATA, ATAR, BL, B, BR, L, R, AL, A, AR, DB, DA, IS;
        static final Canonical_Combining_Class
        Not_Reordered=NR,
        Overlay=OV,
        Nukta=NK,
        Kana_Voicing=KV,
        Virama=VR,
        Attached_Below_Left=ATBL,
        Attached_Below=ATB,
        Attached_Above=ATA,
        Attached_Above_Right=ATAR,
        Below_Left=BL,
        Below=B,
        Below_Right=BR,
        Left=L,
        Right=R,
        Above_Left=AL,
        Above=A,
        Above_Right=AR,
        Double_Below=DB,
        Double_Above=DA,
        Iota_Subscript=IS;
    }
    // Case_Folding
    enum Case_Ignorable {
        No, Yes;
        static final Case_Ignorable
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum Cased {
        No, Yes;
        static final Cased
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum Changes_When_Casefolded {
        No, Yes;
        static final Changes_When_Casefolded
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum Changes_When_Casemapped {
        No, Yes;
        static final Changes_When_Casemapped
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum Changes_When_Lowercased {
        No, Yes;
        static final Changes_When_Lowercased
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum Changes_When_NFKC_Casefolded {
        No, Yes;
        static final Changes_When_NFKC_Casefolded
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum Changes_When_Titlecased {
        No, Yes;
        static final Changes_When_Titlecased
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum Changes_When_Uppercased {
        No, Yes;
        static final Changes_When_Uppercased
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum Composition_Exclusion {
        No, Yes;
        static final Composition_Exclusion
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum Dash {
        No, Yes;
        static final Dash
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    // Decomposition_Mapping
    enum Decomposition_Type {
        Canonical, Compat, Circle, Final, font, Fraction, Initial, Isolated, Medial, Narrow, Nobreak, none, Small, Square, sub, Super, Vertical, wide;
        static final Decomposition_Type
        Can=Canonical,
        can=Canonical,
        Com=Compat,
        com=Compat,
        Enc=Circle,
        enc=Circle,
        Fin=Final,
        fin=Final,
        Font=font,
        Fra=Fraction,
        fra=Fraction,
        Init=Initial,
        init=Initial,
        Iso=Isolated,
        iso=Isolated,
        Med=Medial,
        med=Medial,
        Nar=Narrow,
        nar=Narrow,
        Nb=Nobreak,
        nb=Nobreak,
        None=none,
        Sml=Small,
        sml=Small,
        Sqr=Square,
        sqr=Square,
        Sub=sub,
        Sup=Super,
        sup=Super,
        Vert=Vertical,
        vert=Vertical,
        Wide=wide;
    }
    enum Default_Ignorable_Code_Point {
        No, Yes;
        static final Default_Ignorable_Code_Point
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum Deprecated {
        No, Yes;
        static final Deprecated
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum Diacritic {
        No, Yes;
        static final Diacritic
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum East_Asian_Width {
        Ambiguous, Fullwidth, Halfwidth, Neutral, Narrow, Wide;
        static final East_Asian_Width
        A=Ambiguous,
        F=Fullwidth,
        H=Halfwidth,
        N=Neutral,
        Na=Narrow,
        W=Wide;
    }
    enum Expands_On_NFC {
        No, Yes;
        static final Expands_On_NFC
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum Expands_On_NFD {
        No, Yes;
        static final Expands_On_NFD
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum Expands_On_NFKC {
        No, Yes;
        static final Expands_On_NFKC
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum Expands_On_NFKD {
        No, Yes;
        static final Expands_On_NFKD
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum Extender {
        No, Yes;
        static final Extender
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    // FC_NFKC_Closure
    enum Full_Composition_Exclusion {
        No, Yes;
        static final Full_Composition_Exclusion
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum General_Category {
        Other, Control, Format, Unassigned, Private_Use, Surrogate, Letter, Cased_Letter, Lowercase_Letter, Modifier_Letter, Other_Letter, Titlecase_Letter, Uppercase_Letter, Mark, Spacing_Mark, Enclosing_Mark, Nonspacing_Mark, Number, Decimal_Number, Letter_Number, Other_Number, Punctuation, Connector_Punctuation, Dash_Punctuation, Close_Punctuation, Final_Punctuation, Initial_Punctuation, Other_Punctuation, Open_Punctuation, Symbol, Currency_Symbol, Modifier_Symbol, Math_Symbol, Other_Symbol, Separator, Line_Separator, Paragraph_Separator, Space_Separator;
        static final General_Category
        C=Other,
        Cc=Control,
        cntrl=Control,
        Cf=Format,
        Cn=Unassigned,
        Co=Private_Use,
        Cs=Surrogate,
        L=Letter,
        LC=Cased_Letter,
        Ll=Lowercase_Letter,
        Lm=Modifier_Letter,
        Lo=Other_Letter,
        Lt=Titlecase_Letter,
        Lu=Uppercase_Letter,
        M=Mark,
        Combining_Mark=Mark,
        Mc=Spacing_Mark,
        Me=Enclosing_Mark,
        Mn=Nonspacing_Mark,
        N=Number,
        Nd=Decimal_Number,
        digit=Decimal_Number,
        Nl=Letter_Number,
        No=Other_Number,
        P=Punctuation,
        punct=Punctuation,
        Pc=Connector_Punctuation,
        Pd=Dash_Punctuation,
        Pe=Close_Punctuation,
        Pf=Final_Punctuation,
        Pi=Initial_Punctuation,
        Po=Other_Punctuation,
        Ps=Open_Punctuation,
        S=Symbol,
        Sc=Currency_Symbol,
        Sk=Modifier_Symbol,
        Sm=Math_Symbol,
        So=Other_Symbol,
        Z=Separator,
        Zl=Line_Separator,
        Zp=Paragraph_Separator,
        Zs=Space_Separator;
    }
    enum Grapheme_Base {
        No, Yes;
        static final Grapheme_Base
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum Grapheme_Cluster_Break {
        Control, CR, Extend, L, LF, LV, LVT, Prepend, SpacingMark, T, V, Other;
        static final Grapheme_Cluster_Break
        CN=Control,
        EX=Extend,
        PP=Prepend,
        SM=SpacingMark,
        XX=Other;
    }
    enum Grapheme_Extend {
        No, Yes;
        static final Grapheme_Extend
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum Grapheme_Link {
        No, Yes;
        static final Grapheme_Link
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum Hangul_Syllable_Type {
        Leading_Jamo, LV_Syllable, LVT_Syllable, Not_Applicable, Trailing_Jamo, Vowel_Jamo;
        static final Hangul_Syllable_Type
        L=Leading_Jamo,
        LV=LV_Syllable,
        LVT=LVT_Syllable,
        NA=Not_Applicable,
        T=Trailing_Jamo,
        V=Vowel_Jamo;
    }
    enum Hex_Digit {
        No, Yes;
        static final Hex_Digit
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum Hyphen {
        No, Yes;
        static final Hyphen
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum IDS_Binary_Operator {
        No, Yes;
        static final IDS_Binary_Operator
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum IDS_Trinary_Operator {
        No, Yes;
        static final IDS_Trinary_Operator
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum ID_Continue {
        No, Yes;
        static final ID_Continue
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum ID_Start {
        No, Yes;
        static final ID_Start
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    // ISO_Comment
    enum Ideographic {
        No, Yes;
        static final Ideographic
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum Jamo_Short_Name {
        A, AE, B, BB, BS, C, D, DD, E, EO, EU, G, GG, GS, H, I, J, JJ, K, L, LB, LG, LH, LM, LP, LS, LT, M, N, NG, NH, NJ, O, OE, P, R, S, SS, T, U, WA, WAE, WE, WEO, WI, YA, YAE, YE, YEO, YI, YO, YU
    }
    enum Join_Control {
        No, Yes;
        static final Join_Control
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum Joining_Group {
        Ain, Alaph, Alef, Beh, Beth, Burushaski_Yeh_Barree, Dal, Dalath_Rish, E, Farsi_Yeh, Fe, Feh, Final_Semkath, Gaf, Gamal, Hah, He, Heh, Heh_Goal, Heth, Kaf, Kaph, Khaph, Knotted_Heh, Lam, Lamadh, Meem, Mim, No_Joining_Group, Noon, Nun, Nya, Pe, Qaf, Qaph, Reh, Reversed_Pe, Rohingya_Yeh, Sad, Sadhe, Seen, Semkath, Shin, Swash_Kaf, Syriac_Waw, Tah, Taw, Teh_Marbuta, Teh_Marbuta_Goal, Teth, Waw, Yeh, Yeh_Barree, Yeh_With_Tail, Yudh, Yudh_He, Zain, Zhain;
        static final Joining_Group
        Hamza_On_Heh_Goal=Teh_Marbuta_Goal;
    }
    enum Joining_Type {
        Join_Causing, Dual_Joining, Left_Joining, Right_Joining, Transparent, Non_Joining;
        static final Joining_Type
        C=Join_Causing,
        D=Dual_Joining,
        L=Left_Joining,
        R=Right_Joining,
        T=Transparent,
        U=Non_Joining;
    }
    enum Line_Break {
        Ambiguous, Alphabetic, Break_Both, Break_After, Break_Before, Mandatory_Break, Contingent_Break, Close_Punctuation, Combining_Mark, Close_Parenthesis, Carriage_Return, Exclamation, Glue, H2, H3, Hyphen, Ideographic, Inseparable, Infix_Numeric, JL, JT, JV, Line_Feed, Next_Line, Nonstarter, Numeric, Open_Punctuation, Postfix_Numeric, Prefix_Numeric, Quotation, Complex_Context, Surrogate, Space, Break_Symbols, Word_Joiner, Unknown, ZWSpace;
        static final Line_Break
        AI=Ambiguous,
        AL=Alphabetic,
        B2=Break_Both,
        BA=Break_After,
        BB=Break_Before,
        BK=Mandatory_Break,
        CB=Contingent_Break,
        CL=Close_Punctuation,
        CM=Combining_Mark,
        CP=Close_Parenthesis,
        CR=Carriage_Return,
        EX=Exclamation,
        GL=Glue,
        HY=Hyphen,
        ID=Ideographic,
        IN=Inseparable,
        Inseperable=Inseparable,
        IS=Infix_Numeric,
        LF=Line_Feed,
        NL=Next_Line,
        NS=Nonstarter,
        NU=Numeric,
        OP=Open_Punctuation,
        PO=Postfix_Numeric,
        PR=Prefix_Numeric,
        QU=Quotation,
        SA=Complex_Context,
        SG=Surrogate,
        SP=Space,
        SY=Break_Symbols,
        WJ=Word_Joiner,
        XX=Unknown,
        ZW=ZWSpace;
    }
    enum Logical_Order_Exception {
        No, Yes;
        static final Logical_Order_Exception
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum Lowercase {
        No, Yes;
        static final Lowercase
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    // Lowercase_Mapping
    enum Math {
        No, Yes;
        static final Math
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum NFC_Quick_Check {
        Maybe, No, Yes;
        static final NFC_Quick_Check
        M=Maybe,
        N=No,
        Y=Yes;
    }
    enum NFD_Quick_Check {
        No, Yes;
        static final NFD_Quick_Check
        N=No,
        Y=Yes;
    }
    // NFKC_Casefold
    enum NFKC_Quick_Check {
        Maybe, No, Yes;
        static final NFKC_Quick_Check
        M=Maybe,
        N=No,
        Y=Yes;
    }
    enum NFKD_Quick_Check {
        No, Yes;
        static final NFKD_Quick_Check
        N=No,
        Y=Yes;
    }
    // Name
    // Name_Alias
    enum Noncharacter_Code_Point {
        No, Yes;
        static final Noncharacter_Code_Point
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum Numeric_Type {
        Decimal, Digit, None, Numeric;
        static final Numeric_Type
        De=Decimal,
        Di=Digit,
        Nu=Numeric;
    }
    // Numeric_Value
    enum Other_Alphabetic {
        No, Yes;
        static final Other_Alphabetic
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum Other_Default_Ignorable_Code_Point {
        No, Yes;
        static final Other_Default_Ignorable_Code_Point
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum Other_Grapheme_Extend {
        No, Yes;
        static final Other_Grapheme_Extend
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum Other_ID_Continue {
        No, Yes;
        static final Other_ID_Continue
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum Other_ID_Start {
        No, Yes;
        static final Other_ID_Start
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum Other_Lowercase {
        No, Yes;
        static final Other_Lowercase
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum Other_Math {
        No, Yes;
        static final Other_Math
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum Other_Uppercase {
        No, Yes;
        static final Other_Uppercase
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum Pattern_Syntax {
        No, Yes;
        static final Pattern_Syntax
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum Pattern_White_Space {
        No, Yes;
        static final Pattern_White_Space
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum Quotation_Mark {
        No, Yes;
        static final Quotation_Mark
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum Radical {
        No, Yes;
        static final Radical
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum STerm {
        No, Yes;
        static final STerm
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum Script {
        Arabic, Imperial_Aramaic, Armenian, Avestan, Balinese, Bamum, Batak, Bengali, Bopomofo, Brahmi, Braille, Buginese, Buhid, Chakma, Canadian_Aboriginal, Carian, Cham, Cherokee, Coptic, Cypriot, Cyrillic, Devanagari, Deseret, Egyptian_Hieroglyphs, Ethiopic, Georgian, Glagolitic, Gothic, Greek, Gujarati, Gurmukhi, Hangul, Han, Hanunoo, Hebrew, Hiragana, Katakana_Or_Hiragana, Old_Italic, Javanese, Kayah_Li, Katakana, Kharoshthi, Khmer, Kannada, Kaithi, Tai_Tham, Lao, Latin, Lepcha, Limbu, Linear_B, Lisu, Lycian, Lydian, Mandaic, Meroitic_Cursive, Meroitic_Hieroglyphs, Malayalam, Mongolian, Meetei_Mayek, Myanmar, Nko, Ogham, Ol_Chiki, Old_Turkic, Oriya, Osmanya, Phags_Pa, Inscriptional_Pahlavi, Phoenician, Miao, Inscriptional_Parthian, Rejang, Runic, Samaritan, Old_South_Arabian, Saurashtra, Shavian, Sharada, Sinhala, Sora_Sompeng, Sundanese, Syloti_Nagri, Syriac, Tagbanwa, Takri, Tai_Le, New_Tai_Lue, Tamil, Tai_Viet, Telugu, Tifinagh, Tagalog, Thaana, Thai, Tibetan, Ugaritic, Vai, Old_Persian, Cuneiform, Yi, Inherited, Common, Unknown;
        static final Script
        Arab=Arabic,
        Armi=Imperial_Aramaic,
        Armn=Armenian,
        Avst=Avestan,
        Bali=Balinese,
        Bamu=Bamum,
        Batk=Batak,
        Beng=Bengali,
        Bopo=Bopomofo,
        Brah=Brahmi,
        Brai=Braille,
        Bugi=Buginese,
        Buhd=Buhid,
        Cakm=Chakma,
        Cans=Canadian_Aboriginal,
        Cari=Carian,
        Cher=Cherokee,
        Copt=Coptic,
        Qaac=Coptic,
        Cprt=Cypriot,
        Cyrl=Cyrillic,
        Deva=Devanagari,
        Dsrt=Deseret,
        Egyp=Egyptian_Hieroglyphs,
        Ethi=Ethiopic,
        Geor=Georgian,
        Glag=Glagolitic,
        Goth=Gothic,
        Grek=Greek,
        Gujr=Gujarati,
        Guru=Gurmukhi,
        Hang=Hangul,
        Hani=Han,
        Hano=Hanunoo,
        Hebr=Hebrew,
        Hira=Hiragana,
        Hrkt=Katakana_Or_Hiragana,
        Ital=Old_Italic,
        Java=Javanese,
        Kali=Kayah_Li,
        Kana=Katakana,
        Khar=Kharoshthi,
        Khmr=Khmer,
        Knda=Kannada,
        Kthi=Kaithi,
        Lana=Tai_Tham,
        Laoo=Lao,
        Latn=Latin,
        Lepc=Lepcha,
        Limb=Limbu,
        Linb=Linear_B,
        Lyci=Lycian,
        Lydi=Lydian,
        Mand=Mandaic,
        Merc=Meroitic_Cursive,
        Mero=Meroitic_Hieroglyphs,
        Mlym=Malayalam,
        Mong=Mongolian,
        Mtei=Meetei_Mayek,
        Mymr=Myanmar,
        Nkoo=Nko,
        Ogam=Ogham,
        Olck=Ol_Chiki,
        Orkh=Old_Turkic,
        Orya=Oriya,
        Osma=Osmanya,
        Phag=Phags_Pa,
        Phli=Inscriptional_Pahlavi,
        Phnx=Phoenician,
        Plrd=Miao,
        Prti=Inscriptional_Parthian,
        Rjng=Rejang,
        Runr=Runic,
        Samr=Samaritan,
        Sarb=Old_South_Arabian,
        Saur=Saurashtra,
        Shaw=Shavian,
        Shrd=Sharada,
        Sinh=Sinhala,
        Sora=Sora_Sompeng,
        Sund=Sundanese,
        Sylo=Syloti_Nagri,
        Syrc=Syriac,
        Tagb=Tagbanwa,
        Takr=Takri,
        Tale=Tai_Le,
        Talu=New_Tai_Lue,
        Taml=Tamil,
        Tavt=Tai_Viet,
        Telu=Telugu,
        Tfng=Tifinagh,
        Tglg=Tagalog,
        Thaa=Thaana,
        Tibt=Tibetan,
        Ugar=Ugaritic,
        Vaii=Vai,
        Xpeo=Old_Persian,
        Xsux=Cuneiform,
        Yiii=Yi,
        Zinh=Inherited,
        Qaai=Inherited,
        Zyyy=Common,
        Zzzz=Unknown;
    }
    enum Sentence_Break {
        ATerm, Close, CR, Extend, Format, OLetter, LF, Lower, Numeric, SContinue, Sep, Sp, STerm, Upper, Other;
        static final Sentence_Break
        AT=ATerm,
        CL=Close,
        EX=Extend,
        FO=Format,
        LE=OLetter,
        LO=Lower,
        NU=Numeric,
        SC=SContinue,
        SE=Sep,
        SP=Sp,
        ST=STerm,
        UP=Upper,
        XX=Other;
    }
    // Simple_Case_Folding
    // Simple_Lowercase_Mapping
    // Simple_Titlecase_Mapping
    // Simple_Uppercase_Mapping
    enum Soft_Dotted {
        No, Yes;
        static final Soft_Dotted
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum Terminal_Punctuation {
        No, Yes;
        static final Terminal_Punctuation
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    // Titlecase_Mapping
    // Unicode_1_Name
    enum Unified_Ideograph {
        No, Yes;
        static final Unified_Ideograph
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum Uppercase {
        No, Yes;
        static final Uppercase
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    // Uppercase_Mapping
    enum Variation_Selector {
        No, Yes;
        static final Variation_Selector
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum White_Space {
        No, Yes;
        static final White_Space
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum Word_Break {
        CR, ExtendNumLet, Extend, Format, Katakana, ALetter, LF, MidNumLet, MidLetter, MidNum, Newline, Numeric, Other;
        static final Word_Break
        EX=ExtendNumLet,
        FO=Format,
        KA=Katakana,
        LE=ALetter,
        MB=MidNumLet,
        ML=MidLetter,
        MN=MidNum,
        NL=Newline,
        NU=Numeric,
        XX=Other;
    }
    enum XID_Continue {
        No, Yes;
        static final XID_Continue
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    enum XID_Start {
        No, Yes;
        static final XID_Start
        N=No,
        F=No,
        False=No,
        Y=Yes,
        T=Yes,
        True=Yes;
    }
    // kAccountingNumeric
    // kCompatibilityVariant
    // kIICore
    // kIRG_GSource
    // kIRG_HSource
    // kIRG_JSource
    // kIRG_KPSource
    // kIRG_KSource
    // kIRG_MSource
    // kIRG_TSource
    // kIRG_USource
    // kIRG_VSource
    // kOtherNumeric
    // kPrimaryNumeric
    // kRSUnicode
}