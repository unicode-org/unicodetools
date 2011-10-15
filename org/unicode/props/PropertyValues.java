package org.unicode.props;
public class PropertyValues {
    public enum Binary {
        No("N", "F", "False"),
        Yes("Y", "T", "True");
        private final PropertyNames<Binary> names;
        private Binary (String shortName, String...otherNames) {
            names = new PropertyNames(Binary.class, this, shortName, otherNames);
        }
        public PropertyNames<Binary> getNames() {
            return names;
        }
    }

	public enum Age_Values {
        _1_1(null),
        _2_0(null),
        _2_1(null),
        _3_0(null),
        _3_1(null),
        _3_2(null),
        _4_0(null),
        _4_1(null),
        _5_0(null),
        _5_1(null),
        _5_2(null),
        _6_0(null),
        _6_1(null),
        unassigned(null);
        private final PropertyNames<Age_Values> names;
        private Age_Values (String shortName, String...otherNames) {
            names = new PropertyNames(Age_Values.class, this, shortName, otherNames);
        }
        public PropertyNames<Age_Values> getNames() {
            return names;
        }
    }

	public enum Bidi_Class_Values {
        Arabic_Letter("AL"),
        Arabic_Number("AN"),
        Paragraph_Separator("B"),
        Boundary_Neutral("BN"),
        Common_Separator("CS"),
        European_Number("EN"),
        European_Separator("ES"),
        European_Terminator("ET"),
        Left_To_Right("L"),
        Left_To_Right_Embedding("LRE"),
        Left_To_Right_Override("LRO"),
        Nonspacing_Mark("NSM"),
        Other_Neutral("ON"),
        Pop_Directional_Format("PDF"),
        Right_To_Left("R"),
        Right_To_Left_Embedding("RLE"),
        Right_To_Left_Override("RLO"),
        Segment_Separator("S"),
        White_Space("WS");
        private final PropertyNames<Bidi_Class_Values> names;
        private Bidi_Class_Values (String shortName, String...otherNames) {
            names = new PropertyNames(Bidi_Class_Values.class, this, shortName, otherNames);
        }
        public PropertyNames<Bidi_Class_Values> getNames() {
            return names;
        }
    }

		// Bidi_Mirroring_Glyph
	public enum Block_Values {
        Aegean_Numbers(null),
        Alchemical_Symbols(null),
        Alphabetic_Presentation_Forms(null),
        Ancient_Greek_Musical_Notation(null),
        Ancient_Greek_Numbers(null),
        Ancient_Symbols(null),
        Arabic(null),
        Arabic_Extended_A(null),
        Arabic_Mathematical_Alphabetic_Symbols(null),
        Arabic_Presentation_Forms_A(null, "Arabic_Presentation_Forms-A"),
        Arabic_Presentation_Forms_B(null),
        Arabic_Supplement(null),
        Armenian(null),
        Arrows(null),
        Avestan(null),
        Balinese(null),
        Bamum(null),
        Bamum_Supplement(null),
        Basic_Latin(null, "ASCII"),
        Batak(null),
        Bengali(null),
        Block_Elements(null),
        Bopomofo(null),
        Bopomofo_Extended(null),
        Box_Drawing(null),
        Brahmi(null),
        Braille_Patterns(null),
        Buginese(null),
        Buhid(null),
        Byzantine_Musical_Symbols(null),
        Carian(null),
        Chakma(null),
        Cham(null),
        Cherokee(null),
        CJK_Compatibility(null),
        CJK_Compatibility_Forms(null),
        CJK_Compatibility_Ideographs(null),
        CJK_Compatibility_Ideographs_Supplement(null),
        CJK_Radicals_Supplement(null),
        CJK_Strokes(null),
        CJK_Symbols_And_Punctuation(null),
        CJK_Unified_Ideographs(null),
        CJK_Unified_Ideographs_Extension_A(null),
        CJK_Unified_Ideographs_Extension_B(null),
        CJK_Unified_Ideographs_Extension_C(null),
        CJK_Unified_Ideographs_Extension_D(null),
        Combining_Diacritical_Marks(null),
        Combining_Diacritical_Marks_For_Symbols(null, "Combining_Marks_For_Symbols"),
        Combining_Diacritical_Marks_Supplement(null),
        Combining_Half_Marks(null),
        Common_Indic_Number_Forms(null),
        Control_Pictures(null),
        Coptic(null),
        Counting_Rod_Numerals(null),
        Cuneiform(null),
        Cuneiform_Numbers_And_Punctuation(null),
        Currency_Symbols(null),
        Cypriot_Syllabary(null),
        Cyrillic(null),
        Cyrillic_Extended_A(null),
        Cyrillic_Extended_B(null),
        Cyrillic_Supplement(null, "Cyrillic_Supplementary"),
        Deseret(null),
        Devanagari(null),
        Devanagari_Extended(null),
        Dingbats(null),
        Domino_Tiles(null),
        Egyptian_Hieroglyphs(null),
        Emoticons(null),
        Enclosed_Alphanumeric_Supplement(null),
        Enclosed_Alphanumerics(null),
        Enclosed_CJK_Letters_And_Months(null),
        Enclosed_Ideographic_Supplement(null),
        Ethiopic(null),
        Ethiopic_Extended(null),
        Ethiopic_Extended_A(null),
        Ethiopic_Supplement(null),
        General_Punctuation(null),
        Geometric_Shapes(null),
        Georgian(null),
        Georgian_Supplement(null),
        Glagolitic(null),
        Gothic(null),
        Greek_And_Coptic(null, "Greek"),
        Greek_Extended(null),
        Gujarati(null),
        Gurmukhi(null),
        Halfwidth_And_Fullwidth_Forms(null),
        Hangul_Compatibility_Jamo(null),
        Hangul_Jamo(null),
        Hangul_Jamo_Extended_A(null),
        Hangul_Jamo_Extended_B(null),
        Hangul_Syllables(null),
        Hanunoo(null),
        Hebrew(null),
        High_Private_Use_Surrogates(null),
        High_Surrogates(null),
        Hiragana(null),
        Ideographic_Description_Characters(null),
        Imperial_Aramaic(null),
        Inscriptional_Pahlavi(null),
        Inscriptional_Parthian(null),
        IPA_Extensions(null),
        Javanese(null),
        Kaithi(null),
        Kana_Supplement(null),
        Kanbun(null),
        Kangxi_Radicals(null),
        Kannada(null),
        Katakana(null),
        Katakana_Phonetic_Extensions(null),
        Kayah_Li(null),
        Kharoshthi(null),
        Khmer(null),
        Khmer_Symbols(null),
        Lao(null),
        Latin_1_Supplement(null, "Latin_1"),
        Latin_Extended_A(null),
        Latin_Extended_Additional(null),
        Latin_Extended_B(null),
        Latin_Extended_C(null),
        Latin_Extended_D(null),
        Lepcha(null),
        Letterlike_Symbols(null),
        Limbu(null),
        Linear_B_Ideograms(null),
        Linear_B_Syllabary(null),
        Lisu(null),
        Low_Surrogates(null),
        Lycian(null),
        Lydian(null),
        Mahjong_Tiles(null),
        Malayalam(null),
        Mandaic(null),
        Mathematical_Alphanumeric_Symbols(null),
        Mathematical_Operators(null),
        Meetei_Mayek(null),
        Meetei_Mayek_Extensions(null),
        Meroitic_Cursive(null),
        Meroitic_Hieroglyphs(null),
        Miao(null),
        Miscellaneous_Mathematical_Symbols_A(null),
        Miscellaneous_Mathematical_Symbols_B(null),
        Miscellaneous_Symbols(null),
        Miscellaneous_Symbols_And_Arrows(null),
        Miscellaneous_Symbols_And_Pictographs(null),
        Miscellaneous_Technical(null),
        Modifier_Tone_Letters(null),
        Mongolian(null),
        Musical_Symbols(null),
        Myanmar(null),
        Myanmar_Extended_A(null),
        New_Tai_Lue(null),
        NKo(null),
        No_Block(null),
        Number_Forms(null),
        Ogham(null),
        Ol_Chiki(null),
        Old_Italic(null),
        Old_Persian(null),
        Old_South_Arabian(null),
        Old_Turkic(null),
        Optical_Character_Recognition(null),
        Oriya(null),
        Osmanya(null),
        Phags_Pa(null),
        Phaistos_Disc(null),
        Phoenician(null),
        Phonetic_Extensions(null),
        Phonetic_Extensions_Supplement(null),
        Playing_Cards(null),
        Private_Use_Area(null, "Private_Use"),
        Rejang(null),
        Rumi_Numeral_Symbols(null),
        Runic(null),
        Samaritan(null),
        Saurashtra(null),
        Sharada(null),
        Shavian(null),
        Sinhala(null),
        Small_Form_Variants(null),
        Sora_Sompeng(null),
        Spacing_Modifier_Letters(null),
        Specials(null),
        Sundanese(null),
        Sundanese_Supplement(null),
        Superscripts_And_Subscripts(null),
        Supplemental_Arrows_A(null),
        Supplemental_Arrows_B(null),
        Supplemental_Mathematical_Operators(null),
        Supplemental_Punctuation(null),
        Supplementary_Private_Use_Area_A(null),
        Supplementary_Private_Use_Area_B(null),
        Syloti_Nagri(null),
        Syriac(null),
        Tagalog(null),
        Tagbanwa(null),
        Tags(null),
        Tai_Le(null),
        Tai_Tham(null),
        Tai_Viet(null),
        Tai_Xuan_Jing_Symbols(null),
        Takri(null),
        Tamil(null),
        Telugu(null),
        Thaana(null),
        Thai(null),
        Tibetan(null),
        Tifinagh(null),
        Transport_And_Map_Symbols(null),
        Ugaritic(null),
        Unified_Canadian_Aboriginal_Syllabics(null, "Canadian_Syllabics"),
        Unified_Canadian_Aboriginal_Syllabics_Extended(null),
        Vai(null),
        Variation_Selectors(null),
        Variation_Selectors_Supplement(null),
        Vedic_Extensions(null),
        Vertical_Forms(null),
        Yi_Radicals(null),
        Yi_Syllables(null),
        Yijing_Hexagram_Symbols(null);
        private final PropertyNames<Block_Values> names;
        private Block_Values (String shortName, String...otherNames) {
            names = new PropertyNames(Block_Values.class, this, shortName, otherNames);
        }
        public PropertyNames<Block_Values> getNames() {
            return names;
        }
    }

		// CJK_Radical
	public enum Canonical_Combining_Class_Values {
        Not_Reordered("0", "NR"),
        Overlay("1", "OV"),
        Nukta("7", "NK"),
        Kana_Voicing("8", "KV"),
        Virama("9", "VR"),
        Attached_Below_Left("200", "ATBL"),
        Attached_Below("202", "ATB"),
        Attached_Above("214", "ATA"),
        Attached_Above_Right("216", "ATAR"),
        Below_Left("218", "BL"),
        Below("220", "B"),
        Below_Right("222", "BR"),
        Left("224", "L"),
        Right("226", "R"),
        Above_Left("228", "AL"),
        Above("230", "A"),
        Above_Right("232", "AR"),
        Double_Below("233", "DB"),
        Double_Above("234", "DA"),
        Iota_Subscript("240", "IS"),
        Fixed10("10", "F10"),
        Fixed11("11", "F11"),
        Fixed12("12", "F12"),
        Fixed13("13", "F13"),
        Fixed14("14", "F14"),
        Fixed15("15", "F15"),
        Fixed16("16", "F16"),
        Fixed17("17", "F17"),
        Fixed18("18", "F18"),
        Fixed19("19", "F19"),
        Fixed20("20", "F20"),
        Fixed21("21", "F21"),
        Fixed22("22", "F22"),
        Fixed23("23", "F23"),
        Fixed24("24", "F24"),
        Fixed25("25", "F25"),
        Fixed26("26", "F26"),
        Fixed27("27", "F27"),
        Fixed28("28", "F28"),
        Fixed29("29", "F29"),
        Fixed30("30", "F30"),
        Fixed31("31", "F31"),
        Fixed32("32", "F32"),
        Fixed33("33", "F33"),
        Fixed34("34", "F34"),
        Fixed35("35", "F35"),
        Fixed36("36", "F36"),
        Fixed84("84", "F84"),
        Fixed91("91", "F91"),
        Fixed103("103", "F103"),
        Fixed107("107", "F107"),
        Fixed118("118", "F118"),
        Fixed122("122", "F122"),
        Fixed129("129", "F129"),
        Fixed130("130", "F130"),
        Fixed132("132", "F132");
        private final PropertyNames<Canonical_Combining_Class_Values> names;
        private Canonical_Combining_Class_Values (String shortName, String...otherNames) {
            names = new PropertyNames(Canonical_Combining_Class_Values.class, this, shortName, otherNames);
        }
        public PropertyNames<Canonical_Combining_Class_Values> getNames() {
            return names;
        }
    }

		// Case_Folding
		// Decomposition_Mapping
	public enum Decomposition_Type_Values {
        Canonical("Can", "can"),
        Compat("Com", "com"),
        Circle("Enc", "enc"),
        Final("Fin", "fin"),
        font("Font"),
        Fraction("Fra", "fra"),
        Initial("Init", "init"),
        Isolated("Iso", "iso"),
        Medial("Med", "med"),
        Narrow("Nar", "nar"),
        Nobreak("Nb", "nb"),
        none("None"),
        Small("Sml", "sml"),
        Square("Sqr", "sqr"),
        sub("Sub"),
        Super("Sup", "sup"),
        Vertical("Vert", "vert"),
        wide("Wide");
        private final PropertyNames<Decomposition_Type_Values> names;
        private Decomposition_Type_Values (String shortName, String...otherNames) {
            names = new PropertyNames(Decomposition_Type_Values.class, this, shortName, otherNames);
        }
        public PropertyNames<Decomposition_Type_Values> getNames() {
            return names;
        }
    }

	public enum East_Asian_Width_Values {
        Ambiguous("A"),
        Fullwidth("F"),
        Halfwidth("H"),
        Neutral("N"),
        Narrow("Na"),
        Wide("W");
        private final PropertyNames<East_Asian_Width_Values> names;
        private East_Asian_Width_Values (String shortName, String...otherNames) {
            names = new PropertyNames(East_Asian_Width_Values.class, this, shortName, otherNames);
        }
        public PropertyNames<East_Asian_Width_Values> getNames() {
            return names;
        }
    }

		// Emoji_DCM
		// Emoji_KDDI
		// Emoji_SB
		// FC_NFKC_Closure
	public enum General_Category_Values {
        Other("C"),
        Control("Cc", "cntrl"),
        Format("Cf"),
        Unassigned("Cn"),
        Private_Use("Co"),
        Surrogate("Cs"),
        Letter("L"),
        Cased_Letter("LC"),
        Lowercase_Letter("Ll"),
        Modifier_Letter("Lm"),
        Other_Letter("Lo"),
        Titlecase_Letter("Lt"),
        Uppercase_Letter("Lu"),
        Mark("M", "Combining_Mark"),
        Spacing_Mark("Mc"),
        Enclosing_Mark("Me"),
        Nonspacing_Mark("Mn"),
        Number("N"),
        Decimal_Number("Nd", "digit"),
        Letter_Number("Nl"),
        Other_Number("No"),
        Punctuation("P", "punct"),
        Connector_Punctuation("Pc"),
        Dash_Punctuation("Pd"),
        Close_Punctuation("Pe"),
        Final_Punctuation("Pf"),
        Initial_Punctuation("Pi"),
        Other_Punctuation("Po"),
        Open_Punctuation("Ps"),
        Symbol("S"),
        Currency_Symbol("Sc"),
        Modifier_Symbol("Sk"),
        Math_Symbol("Sm"),
        Other_Symbol("So"),
        Separator("Z"),
        Line_Separator("Zl"),
        Paragraph_Separator("Zp"),
        Space_Separator("Zs");
        private final PropertyNames<General_Category_Values> names;
        private General_Category_Values (String shortName, String...otherNames) {
            names = new PropertyNames(General_Category_Values.class, this, shortName, otherNames);
        }
        public PropertyNames<General_Category_Values> getNames() {
            return names;
        }
    }

	public enum Grapheme_Cluster_Break_Values {
        Control("CN"),
        CR("CR"),
        Extend("EX"),
        L("L"),
        LF("LF"),
        LV("LV"),
        LVT("LVT"),
        Prepend("PP"),
        Spacing_Mark("SM", "SpacingMark"),
        T("T"),
        V("V"),
        Other("XX");
        private final PropertyNames<Grapheme_Cluster_Break_Values> names;
        private Grapheme_Cluster_Break_Values (String shortName, String...otherNames) {
            names = new PropertyNames(Grapheme_Cluster_Break_Values.class, this, shortName, otherNames);
        }
        public PropertyNames<Grapheme_Cluster_Break_Values> getNames() {
            return names;
        }
    }

	public enum Hangul_Syllable_Type_Values {
        Leading_Jamo("L"),
        LV_Syllable("LV"),
        LVT_Syllable("LVT"),
        Not_Applicable("NA"),
        Trailing_Jamo("T"),
        Vowel_Jamo("V");
        private final PropertyNames<Hangul_Syllable_Type_Values> names;
        private Hangul_Syllable_Type_Values (String shortName, String...otherNames) {
            names = new PropertyNames(Hangul_Syllable_Type_Values.class, this, shortName, otherNames);
        }
        public PropertyNames<Hangul_Syllable_Type_Values> getNames() {
            return names;
        }
    }

		// ISO_Comment
	public enum Indic_Matra_Category_Values {
        Right(null),
        Left(null),
        Visual_Order_Left(null),
        Left_And_Right(null),
        Top(null),
        Bottom(null),
        Top_And_Bottom(null),
        Top_And_Right(null),
        Top_And_Left(null),
        Top_And_Left_And_Right(null),
        Bottom_And_Right(null),
        Top_And_Bottom_And_Right(null),
        Overstruck(null),
        Invisible(null),
        NA(null);
        private final PropertyNames<Indic_Matra_Category_Values> names;
        private Indic_Matra_Category_Values (String shortName, String...otherNames) {
            names = new PropertyNames(Indic_Matra_Category_Values.class, this, shortName, otherNames);
        }
        public PropertyNames<Indic_Matra_Category_Values> getNames() {
            return names;
        }
    }

	public enum Indic_Syllabic_Category_Values {
        Bindu(null),
        Visarga(null),
        Avagraha(null),
        Nukta(null),
        Virama(null),
        Vowel_Independent(null),
        Vowel_Dependent(null),
        Vowel(null),
        Consonant_Placeholder(null),
        Consonant(null),
        Consonant_Dead(null),
        Consonant_Repha(null),
        Consonant_Subjoined(null),
        Consonant_Medial(null),
        Consonant_Final(null),
        Consonant_Head_Letter(null),
        Modifying_Letter(null),
        Tone_Letter(null),
        Tone_Mark(null),
        Register_Shifter(null),
        Other(null);
        private final PropertyNames<Indic_Syllabic_Category_Values> names;
        private Indic_Syllabic_Category_Values (String shortName, String...otherNames) {
            names = new PropertyNames(Indic_Syllabic_Category_Values.class, this, shortName, otherNames);
        }
        public PropertyNames<Indic_Syllabic_Category_Values> getNames() {
            return names;
        }
    }

	public enum Jamo_Short_Name_Values {
        A("A"),
        AE("AE"),
        B("B"),
        BB("BB"),
        BS("BS"),
        C("C"),
        D("D"),
        DD("DD"),
        E("E"),
        EO("EO"),
        EU("EU"),
        G("G"),
        GG("GG"),
        GS("GS"),
        H("H"),
        I("I"),
        J("J"),
        JJ("JJ"),
        K("K"),
        L("L"),
        LB("LB"),
        LG("LG"),
        LH("LH"),
        LM("LM"),
        LP("LP"),
        LS("LS"),
        LT("LT"),
        M("M"),
        N("N"),
        NG("NG"),
        NH("NH"),
        NJ("NJ"),
        O("O"),
        OE("OE"),
        P("P"),
        R("R"),
        S("S"),
        SS("SS"),
        T("T"),
        U("U"),
        WA("WA"),
        WAE("WAE"),
        WE("WE"),
        WEO("WEO"),
        WI("WI"),
        YA("YA"),
        YAE("YAE"),
        YE("YE"),
        YEO("YEO"),
        YI("YI"),
        YO("YO"),
        YU("YU");
        private final PropertyNames<Jamo_Short_Name_Values> names;
        private Jamo_Short_Name_Values (String shortName, String...otherNames) {
            names = new PropertyNames(Jamo_Short_Name_Values.class, this, shortName, otherNames);
        }
        public PropertyNames<Jamo_Short_Name_Values> getNames() {
            return names;
        }
    }

	public enum Joining_Group_Values {
        Ain(null),
        Alaph(null),
        Alef(null),
        Beh(null),
        Beth(null),
        Burushaski_Yeh_Barree(null),
        Dal(null),
        Dalath_Rish(null),
        E(null),
        Farsi_Yeh(null),
        Fe(null),
        Feh(null),
        Final_Semkath(null),
        Gaf(null),
        Gamal(null),
        Hah(null),
        He(null),
        Heh(null),
        Heh_Goal(null),
        Heth(null),
        Kaf(null),
        Kaph(null),
        Khaph(null),
        Knotted_Heh(null),
        Lam(null),
        Lamadh(null),
        Meem(null),
        Mim(null),
        No_Joining_Group(null),
        Noon(null),
        Nun(null),
        Nya(null),
        Pe(null),
        Qaf(null),
        Qaph(null),
        Reh(null),
        Reversed_Pe(null),
        Rohingya_Yeh(null),
        Sad(null),
        Sadhe(null),
        Seen(null),
        Semkath(null),
        Shin(null),
        Swash_Kaf(null),
        Syriac_Waw(null),
        Tah(null),
        Taw(null),
        Teh_Marbuta(null),
        Teh_Marbuta_Goal(null, "Hamza_On_Heh_Goal"),
        Teth(null),
        Waw(null),
        Yeh(null),
        Yeh_Barree(null),
        Yeh_With_Tail(null),
        Yudh(null),
        Yudh_He(null),
        Zain(null),
        Zhain(null);
        private final PropertyNames<Joining_Group_Values> names;
        private Joining_Group_Values (String shortName, String...otherNames) {
            names = new PropertyNames(Joining_Group_Values.class, this, shortName, otherNames);
        }
        public PropertyNames<Joining_Group_Values> getNames() {
            return names;
        }
    }

	public enum Joining_Type_Values {
        Join_Causing("C"),
        Dual_Joining("D"),
        Left_Joining("L"),
        Right_Joining("R"),
        Transparent("T"),
        Non_Joining("U");
        private final PropertyNames<Joining_Type_Values> names;
        private Joining_Type_Values (String shortName, String...otherNames) {
            names = new PropertyNames(Joining_Type_Values.class, this, shortName, otherNames);
        }
        public PropertyNames<Joining_Type_Values> getNames() {
            return names;
        }
    }

	public enum Line_Break_Values {
        Ambiguous("AI"),
        Alphabetic("AL"),
        Break_Both("B2"),
        Break_After("BA"),
        Break_Before("BB"),
        Mandatory_Break("BK"),
        Contingent_Break("CB"),
        Close_Punctuation("CL"),
        Combining_Mark("CM"),
        Close_Parenthesis("CP"),
        Carriage_Return("CR"),
        Exclamation("EX"),
        Glue("GL"),
        H2("H2"),
        H3("H3"),
        Hebrew_Letter("HL"),
        Hyphen("HY"),
        Ideographic("ID"),
        Inseparable("IN", "Inseperable"),
        Infix_Numeric("IS"),
        JL("JL"),
        JT("JT"),
        JV("JV"),
        Line_Feed("LF"),
        Next_Line("NL"),
        Nonstarter("NS"),
        Numeric("NU"),
        Open_Punctuation("OP"),
        Postfix_Numeric("PO"),
        Prefix_Numeric("PR"),
        Quotation("QU"),
        Complex_Context("SA"),
        Surrogate("SG"),
        Space("SP"),
        Break_Symbols("SY"),
        Word_Joiner("WJ"),
        Unknown("XX"),
        ZWSpace("ZW");
        private final PropertyNames<Line_Break_Values> names;
        private Line_Break_Values (String shortName, String...otherNames) {
            names = new PropertyNames(Line_Break_Values.class, this, shortName, otherNames);
        }
        public PropertyNames<Line_Break_Values> getNames() {
            return names;
        }
    }

		// Lowercase_Mapping
	public enum NFC_Quick_Check_Values {
        Maybe("M"),
        No("N"),
        Yes("Y");
        private final PropertyNames<NFC_Quick_Check_Values> names;
        private NFC_Quick_Check_Values (String shortName, String...otherNames) {
            names = new PropertyNames(NFC_Quick_Check_Values.class, this, shortName, otherNames);
        }
        public PropertyNames<NFC_Quick_Check_Values> getNames() {
            return names;
        }
    }

	public enum NFD_Quick_Check_Values {
        No("N"),
        Yes("Y");
        private final PropertyNames<NFD_Quick_Check_Values> names;
        private NFD_Quick_Check_Values (String shortName, String...otherNames) {
            names = new PropertyNames(NFD_Quick_Check_Values.class, this, shortName, otherNames);
        }
        public PropertyNames<NFD_Quick_Check_Values> getNames() {
            return names;
        }
    }

		// NFKC_Casefold
	public enum NFKC_Quick_Check_Values {
        Maybe("M"),
        No("N"),
        Yes("Y");
        private final PropertyNames<NFKC_Quick_Check_Values> names;
        private NFKC_Quick_Check_Values (String shortName, String...otherNames) {
            names = new PropertyNames(NFKC_Quick_Check_Values.class, this, shortName, otherNames);
        }
        public PropertyNames<NFKC_Quick_Check_Values> getNames() {
            return names;
        }
    }

	public enum NFKD_Quick_Check_Values {
        No("N"),
        Yes("Y");
        private final PropertyNames<NFKD_Quick_Check_Values> names;
        private NFKD_Quick_Check_Values (String shortName, String...otherNames) {
            names = new PropertyNames(NFKD_Quick_Check_Values.class, this, shortName, otherNames);
        }
        public PropertyNames<NFKD_Quick_Check_Values> getNames() {
            return names;
        }
    }

		// Name
		// Name_Alias
		// Name_Alias_Prov
		// Named_Sequences
		// Named_Sequences_Prov
	public enum Numeric_Type_Values {
        Decimal("De"),
        Digit("Di"),
        None("None"),
        Numeric("Nu");
        private final PropertyNames<Numeric_Type_Values> names;
        private Numeric_Type_Values (String shortName, String...otherNames) {
            names = new PropertyNames(Numeric_Type_Values.class, this, shortName, otherNames);
        }
        public PropertyNames<Numeric_Type_Values> getNames() {
            return names;
        }
    }

		// Numeric_Value
	public enum Script_Values {
        Arabic("Arab"),
        Imperial_Aramaic("Armi"),
        Armenian("Armn"),
        Avestan("Avst"),
        Balinese("Bali"),
        Bamum("Bamu"),
        Batak("Batk"),
        Bengali("Beng"),
        Bopomofo("Bopo"),
        Brahmi("Brah"),
        Braille("Brai"),
        Buginese("Bugi"),
        Buhid("Buhd"),
        Chakma("Cakm"),
        Canadian_Aboriginal("Cans"),
        Carian("Cari"),
        Cham("Cham"),
        Cherokee("Cher"),
        Coptic("Copt", "Qaac"),
        Cypriot("Cprt"),
        Cyrillic("Cyrl"),
        Devanagari("Deva"),
        Deseret("Dsrt"),
        Egyptian_Hieroglyphs("Egyp"),
        Ethiopic("Ethi"),
        Georgian("Geor"),
        Glagolitic("Glag"),
        Gothic("Goth"),
        Greek("Grek"),
        Gujarati("Gujr"),
        Gurmukhi("Guru"),
        Hangul("Hang"),
        Han("Hani"),
        Hanunoo("Hano"),
        Hebrew("Hebr"),
        Hiragana("Hira"),
        Katakana_Or_Hiragana("Hrkt"),
        Old_Italic("Ital"),
        Javanese("Java"),
        Kayah_Li("Kali"),
        Katakana("Kana"),
        Kharoshthi("Khar"),
        Khmer("Khmr"),
        Kannada("Knda"),
        Kaithi("Kthi"),
        Tai_Tham("Lana"),
        Lao("Laoo"),
        Latin("Latn"),
        Lepcha("Lepc"),
        Limbu("Limb"),
        Linear_B("Linb"),
        Lisu("Lisu"),
        Lycian("Lyci"),
        Lydian("Lydi"),
        Mandaic("Mand"),
        Meroitic_Cursive("Merc"),
        Meroitic_Hieroglyphs("Mero"),
        Malayalam("Mlym"),
        Mongolian("Mong"),
        Meetei_Mayek("Mtei"),
        Myanmar("Mymr"),
        Nko("Nkoo"),
        Ogham("Ogam"),
        Ol_Chiki("Olck"),
        Old_Turkic("Orkh"),
        Oriya("Orya"),
        Osmanya("Osma"),
        Phags_Pa("Phag"),
        Inscriptional_Pahlavi("Phli"),
        Phoenician("Phnx"),
        Miao("Plrd"),
        Inscriptional_Parthian("Prti"),
        Rejang("Rjng"),
        Runic("Runr"),
        Samaritan("Samr"),
        Old_South_Arabian("Sarb"),
        Saurashtra("Saur"),
        Shavian("Shaw"),
        Sharada("Shrd"),
        Sinhala("Sinh"),
        Sora_Sompeng("Sora"),
        Sundanese("Sund"),
        Syloti_Nagri("Sylo"),
        Syriac("Syrc"),
        Tagbanwa("Tagb"),
        Takri("Takr"),
        Tai_Le("Tale"),
        New_Tai_Lue("Talu"),
        Tamil("Taml"),
        Tai_Viet("Tavt"),
        Telugu("Telu"),
        Tifinagh("Tfng"),
        Tagalog("Tglg"),
        Thaana("Thaa"),
        Thai("Thai"),
        Tibetan("Tibt"),
        Ugaritic("Ugar"),
        Vai("Vaii"),
        Old_Persian("Xpeo"),
        Cuneiform("Xsux"),
        Yi("Yiii"),
        Inherited("Zinh", "Qaai"),
        Common("Zyyy"),
        Unknown("Zzzz");
        private final PropertyNames<Script_Values> names;
        private Script_Values (String shortName, String...otherNames) {
            names = new PropertyNames(Script_Values.class, this, shortName, otherNames);
        }
        public PropertyNames<Script_Values> getNames() {
            return names;
        }
    }

		// Script_Extensions
	public enum Sentence_Break_Values {
        ATerm("AT"),
        Close("CL"),
        CR("CR"),
        Extend("EX"),
        Format("FO"),
        OLetter("LE"),
        LF("LF"),
        Lower("LO"),
        Numeric("NU"),
        SContinue("SC"),
        Sep("SE"),
        Sp("SP"),
        STerm("ST"),
        Upper("UP"),
        Other("XX");
        private final PropertyNames<Sentence_Break_Values> names;
        private Sentence_Break_Values (String shortName, String...otherNames) {
            names = new PropertyNames(Sentence_Break_Values.class, this, shortName, otherNames);
        }
        public PropertyNames<Sentence_Break_Values> getNames() {
            return names;
        }
    }

		// Simple_Case_Folding
		// Simple_Lowercase_Mapping
		// Simple_Titlecase_Mapping
		// Simple_Uppercase_Mapping
		// Titlecase_Mapping
		// Unicode_1_Name
		// Uppercase_Mapping
	public enum Word_Break_Values {
        CR("CR"),
        ExtendNumLet("EX"),
        Extend("Extend"),
        Format("FO"),
        Katakana("KA"),
        ALetter("LE"),
        LF("LF"),
        MidNumLet("MB"),
        MidLetter("ML"),
        MidNum("MN"),
        Newline("NL"),
        Numeric("NU"),
        Other("XX");
        private final PropertyNames<Word_Break_Values> names;
        private Word_Break_Values (String shortName, String...otherNames) {
            names = new PropertyNames(Word_Break_Values.class, this, shortName, otherNames);
        }
        public PropertyNames<Word_Break_Values> getNames() {
            return names;
        }
    }

		// kAccountingNumeric
		// kBigFive
		// kCCCII
		// kCNS1986
		// kCNS1992
		// kCangjie
		// kCantonese
		// kCheungBauer
		// kCheungBauerIndex
		// kCihaiT
		// kCompatibilityVariant
		// kCowles
		// kDaeJaweon
		// kDefinition
		// kEACC
		// kFenn
		// kFennIndex
		// kFourCornerCode
		// kFrequency
		// kGB0
		// kGB1
		// kGB3
		// kGB5
		// kGB7
		// kGB8
		// kGSR
		// kGradeLevel
		// kHDZRadBreak
		// kHKGlyph
		// kHKSCS
		// kHanYu
		// kHangul
		// kHanyuPinlu
		// kHanyuPinyin
		// kIBMJapan
		// kIICore
		// kIRGDaeJaweon
		// kIRGDaiKanwaZiten
		// kIRGHanyuDaZidian
		// kIRGKangXi
		// kIRG_GSource
		// kIRG_HSource
		// kIRG_JSource
		// kIRG_KPSource
		// kIRG_KSource
		// kIRG_MSource
		// kIRG_TSource
		// kIRG_USource
		// kIRG_VSource
		// kJIS0213
		// kJapaneseKun
		// kJapaneseOn
		// kJis0
		// kJis1
		// kKPS0
		// kKPS1
		// kKSC0
		// kKSC1
		// kKangXi
		// kKarlgren
		// kKorean
		// kLau
		// kMainlandTelegraph
		// kMandarin
		// kMatthews
		// kMeyerWempe
		// kMorohashi
		// kNelson
		// kOtherNumeric
		// kPhonetic
		// kPrimaryNumeric
		// kPseudoGB1
		// kRSAdobe_Japan1_6
		// kRSJapanese
		// kRSKanWa
		// kRSKangXi
		// kRSKorean
		// kRSUnicode
		// kSBGY
		// kSemanticVariant
		// kSimplifiedVariant
		// kSpecializedSemanticVariant
		// kTaiwanTelegraph
		// kTang
		// kTotalStrokes
		// kTraditionalVariant
		// kVietnamese
		// kXHC1983
		// kXerox
		// kZVariant

}
