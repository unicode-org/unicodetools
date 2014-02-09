package org.unicode.props;
import org.unicode.props.PropertyNames.Named;
public class UcdPropertyValues {

    public enum Binary implements Named {
        No("N", "F", "False"),
        Yes("Y", "T", "True");
        private final PropertyNames<Binary> names;
        private Binary (String shortName, String...otherNames) {
            names = new PropertyNames(Binary.class, this, shortName, otherNames);
        }
        public PropertyNames<Binary> getNames() {
            return names;
        }
        public String getShortName() {
            return names.getShortName();
        }
    }

	public enum Age_Values implements Named {
        V1_1("1.1"),
        V2_0("2.0"),
        V2_1("2.1"),
        V3_0("3.0"),
        V3_1("3.1"),
        V3_2("3.2"),
        V4_0("4.0"),
        V4_1("4.1"),
        V5_0("5.0"),
        V5_1("5.1"),
        V5_2("5.2"),
        V6_0("6.0"),
        V6_1("6.1"),
        V6_2("6.2"),
        V6_3("6.3"),
        V7_0("7.0"),
        Unassigned("NA");
        private final PropertyNames<Age_Values> names;
        private Age_Values (String shortName, String...otherNames) {
            names = new PropertyNames(Age_Values.class, this, shortName, otherNames);
        }
        public PropertyNames<Age_Values> getNames() {
            return names;
        }
        public String getShortName() {
            return names.getShortName();
        }
    }

	public enum Bidi_Class_Values implements Named {
        Arabic_Letter("AL"),
        Arabic_Number("AN"),
        Paragraph_Separator("B"),
        Boundary_Neutral("BN"),
        Common_Separator("CS"),
        European_Number("EN"),
        European_Separator("ES"),
        European_Terminator("ET"),
        First_Strong_Isolate("FSI"),
        Left_To_Right("L"),
        Left_To_Right_Embedding("LRE"),
        Left_To_Right_Isolate("LRI"),
        Left_To_Right_Override("LRO"),
        Nonspacing_Mark("NSM"),
        Other_Neutral("ON"),
        Pop_Directional_Format("PDF"),
        Pop_Directional_Isolate("PDI"),
        Right_To_Left("R"),
        Right_To_Left_Embedding("RLE"),
        Right_To_Left_Isolate("RLI"),
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
        public String getShortName() {
            return names.getShortName();
        }
    }

		// Bidi_Mirroring_Glyph
		// Bidi_Paired_Bracket
	public enum Bidi_Paired_Bracket_Type_Values implements Named {
        Close("c"),
        None("n"),
        Open("o");
        private final PropertyNames<Bidi_Paired_Bracket_Type_Values> names;
        private Bidi_Paired_Bracket_Type_Values (String shortName, String...otherNames) {
            names = new PropertyNames(Bidi_Paired_Bracket_Type_Values.class, this, shortName, otherNames);
        }
        public PropertyNames<Bidi_Paired_Bracket_Type_Values> getNames() {
            return names;
        }
        public String getShortName() {
            return names.getShortName();
        }
    }

	public enum Block_Values implements Named {
        Aegean_Numbers("Aegean_Numbers"),
        Alchemical_Symbols("Alchemical"),
        Alphabetic_Presentation_Forms("Alphabetic_PF"),
        Ancient_Greek_Musical_Notation("Ancient_Greek_Music"),
        Ancient_Greek_Numbers("Ancient_Greek_Numbers"),
        Ancient_Symbols("Ancient_Symbols"),
        Arabic("Arabic"),
        Arabic_Extended_A("Arabic_Ext_A"),
        Arabic_Mathematical_Alphabetic_Symbols("Arabic_Math"),
        Arabic_Presentation_Forms_A("Arabic_PF_A", "Arabic_Presentation_Forms-A"),
        Arabic_Presentation_Forms_B("Arabic_PF_B"),
        Arabic_Supplement("Arabic_Sup"),
        Armenian("Armenian"),
        Arrows("Arrows"),
        Basic_Latin("ASCII"),
        Avestan("Avestan"),
        Balinese("Balinese"),
        Bamum("Bamum"),
        Bamum_Supplement("Bamum_Sup"),
        Bassa_Vah("Bassa_Vah"),
        Batak("Batak"),
        Bengali("Bengali"),
        Block_Elements("Block_Elements"),
        Bopomofo("Bopomofo"),
        Bopomofo_Extended("Bopomofo_Ext"),
        Box_Drawing("Box_Drawing"),
        Brahmi("Brahmi"),
        Braille_Patterns("Braille"),
        Buginese("Buginese"),
        Buhid("Buhid"),
        Byzantine_Musical_Symbols("Byzantine_Music"),
        Carian("Carian"),
        Caucasian_Albanian("Caucasian_Albanian"),
        Chakma("Chakma"),
        Cham("Cham"),
        Cherokee("Cherokee"),
        CJK_Unified_Ideographs("CJK"),
        CJK_Compatibility("CJK_Compat"),
        CJK_Compatibility_Forms("CJK_Compat_Forms"),
        CJK_Compatibility_Ideographs("CJK_Compat_Ideographs"),
        CJK_Compatibility_Ideographs_Supplement("CJK_Compat_Ideographs_Sup"),
        CJK_Unified_Ideographs_Extension_A("CJK_Ext_A"),
        CJK_Unified_Ideographs_Extension_B("CJK_Ext_B"),
        CJK_Unified_Ideographs_Extension_C("CJK_Ext_C"),
        CJK_Unified_Ideographs_Extension_D("CJK_Ext_D"),
        CJK_Radicals_Supplement("CJK_Radicals_Sup"),
        CJK_Strokes("CJK_Strokes"),
        CJK_Symbols_And_Punctuation("CJK_Symbols"),
        Hangul_Compatibility_Jamo("Compat_Jamo"),
        Control_Pictures("Control_Pictures"),
        Coptic("Coptic"),
        Coptic_Epact_Numbers("Coptic_Epact_Numbers"),
        Counting_Rod_Numerals("Counting_Rod"),
        Cuneiform("Cuneiform"),
        Cuneiform_Numbers_And_Punctuation("Cuneiform_Numbers"),
        Currency_Symbols("Currency_Symbols"),
        Cypriot_Syllabary("Cypriot_Syllabary"),
        Cyrillic("Cyrillic"),
        Cyrillic_Extended_A("Cyrillic_Ext_A"),
        Cyrillic_Extended_B("Cyrillic_Ext_B"),
        Cyrillic_Supplement("Cyrillic_Sup", "Cyrillic_Supplementary"),
        Deseret("Deseret"),
        Devanagari("Devanagari"),
        Devanagari_Extended("Devanagari_Ext"),
        Combining_Diacritical_Marks("Diacriticals"),
        Combining_Diacritical_Marks_Extended("Diacriticals_Ext"),
        Combining_Diacritical_Marks_For_Symbols("Diacriticals_For_Symbols", "Combining_Marks_For_Symbols"),
        Combining_Diacritical_Marks_Supplement("Diacriticals_Sup"),
        Dingbats("Dingbats"),
        Domino_Tiles("Domino"),
        Duployan("Duployan"),
        Egyptian_Hieroglyphs("Egyptian_Hieroglyphs"),
        Elbasan("Elbasan"),
        Emoticons("Emoticons"),
        Enclosed_Alphanumerics("Enclosed_Alphanum"),
        Enclosed_Alphanumeric_Supplement("Enclosed_Alphanum_Sup"),
        Enclosed_CJK_Letters_And_Months("Enclosed_CJK"),
        Enclosed_Ideographic_Supplement("Enclosed_Ideographic_Sup"),
        Ethiopic("Ethiopic"),
        Ethiopic_Extended("Ethiopic_Ext"),
        Ethiopic_Extended_A("Ethiopic_Ext_A"),
        Ethiopic_Supplement("Ethiopic_Sup"),
        Geometric_Shapes("Geometric_Shapes"),
        Geometric_Shapes_Extended("Geometric_Shapes_Ext"),
        Georgian("Georgian"),
        Georgian_Supplement("Georgian_Sup"),
        Glagolitic("Glagolitic"),
        Gothic("Gothic"),
        Grantha("Grantha"),
        Greek_And_Coptic("Greek"),
        Greek_Extended("Greek_Ext"),
        Gujarati("Gujarati"),
        Gurmukhi("Gurmukhi"),
        Halfwidth_And_Fullwidth_Forms("Half_And_Full_Forms"),
        Combining_Half_Marks("Half_Marks"),
        Hangul_Syllables("Hangul"),
        Hanunoo("Hanunoo"),
        Hebrew("Hebrew"),
        High_Private_Use_Surrogates("High_PU_Surrogates"),
        High_Surrogates("High_Surrogates"),
        Hiragana("Hiragana"),
        Ideographic_Description_Characters("IDC"),
        Imperial_Aramaic("Imperial_Aramaic"),
        Common_Indic_Number_Forms("Indic_Number_Forms"),
        Inscriptional_Pahlavi("Inscriptional_Pahlavi"),
        Inscriptional_Parthian("Inscriptional_Parthian"),
        IPA_Extensions("IPA_Ext"),
        Hangul_Jamo("Jamo"),
        Hangul_Jamo_Extended_A("Jamo_Ext_A"),
        Hangul_Jamo_Extended_B("Jamo_Ext_B"),
        Javanese("Javanese"),
        Kaithi("Kaithi"),
        Kana_Supplement("Kana_Sup"),
        Kanbun("Kanbun"),
        Kangxi_Radicals("Kangxi"),
        Kannada("Kannada"),
        Katakana("Katakana"),
        Katakana_Phonetic_Extensions("Katakana_Ext"),
        Kayah_Li("Kayah_Li"),
        Kharoshthi("Kharoshthi"),
        Khmer("Khmer"),
        Khmer_Symbols("Khmer_Symbols"),
        Khojki("Khojki"),
        Khudawadi("Khudawadi"),
        Lao("Lao"),
        Latin_1_Supplement("Latin_1_Sup", "Latin_1"),
        Latin_Extended_A("Latin_Ext_A"),
        Latin_Extended_Additional("Latin_Ext_Additional"),
        Latin_Extended_B("Latin_Ext_B"),
        Latin_Extended_C("Latin_Ext_C"),
        Latin_Extended_D("Latin_Ext_D"),
        Latin_Extended_E("Latin_Ext_E"),
        Lepcha("Lepcha"),
        Letterlike_Symbols("Letterlike_Symbols"),
        Limbu("Limbu"),
        Linear_A("Linear_A"),
        Linear_B_Ideograms("Linear_B_Ideograms"),
        Linear_B_Syllabary("Linear_B_Syllabary"),
        Lisu("Lisu"),
        Low_Surrogates("Low_Surrogates"),
        Lycian("Lycian"),
        Lydian("Lydian"),
        Mahajani("Mahajani"),
        Mahjong_Tiles("Mahjong"),
        Malayalam("Malayalam"),
        Mandaic("Mandaic"),
        Manichaean("Manichaean"),
        Mathematical_Alphanumeric_Symbols("Math_Alphanum"),
        Mathematical_Operators("Math_Operators"),
        Meetei_Mayek("Meetei_Mayek"),
        Meetei_Mayek_Extensions("Meetei_Mayek_Ext"),
        Mende_Kikakui("Mende_Kikakui"),
        Meroitic_Cursive("Meroitic_Cursive"),
        Meroitic_Hieroglyphs("Meroitic_Hieroglyphs"),
        Miao("Miao"),
        Miscellaneous_Symbols_And_Arrows("Misc_Arrows"),
        Miscellaneous_Mathematical_Symbols_A("Misc_Math_Symbols_A"),
        Miscellaneous_Mathematical_Symbols_B("Misc_Math_Symbols_B"),
        Miscellaneous_Symbols_And_Pictographs("Misc_Pictographs"),
        Miscellaneous_Symbols("Misc_Symbols"),
        Miscellaneous_Technical("Misc_Technical"),
        Modi("Modi"),
        Spacing_Modifier_Letters("Modifier_Letters"),
        Modifier_Tone_Letters("Modifier_Tone_Letters"),
        Mongolian("Mongolian"),
        Mro("Mro"),
        Musical_Symbols("Music"),
        Myanmar("Myanmar"),
        Myanmar_Extended_A("Myanmar_Ext_A"),
        Myanmar_Extended_B("Myanmar_Ext_B"),
        Nabataean("Nabataean"),
        No_Block("NB"),
        New_Tai_Lue("New_Tai_Lue"),
        NKo("NKo"),
        Number_Forms("Number_Forms"),
        Optical_Character_Recognition("OCR"),
        Ogham("Ogham"),
        Ol_Chiki("Ol_Chiki"),
        Old_Italic("Old_Italic"),
        Old_North_Arabian("Old_North_Arabian"),
        Old_Permic("Old_Permic"),
        Old_Persian("Old_Persian"),
        Old_South_Arabian("Old_South_Arabian"),
        Old_Turkic("Old_Turkic"),
        Oriya("Oriya"),
        Ornamental_Dingbats("Ornamental_Dingbats"),
        Osmanya("Osmanya"),
        Pahawh_Hmong("Pahawh_Hmong"),
        Palmyrene("Palmyrene"),
        Pau_Cin_Hau("Pau_Cin_Hau"),
        Phags_Pa("Phags_Pa"),
        Phaistos_Disc("Phaistos"),
        Phoenician("Phoenician"),
        Phonetic_Extensions("Phonetic_Ext"),
        Phonetic_Extensions_Supplement("Phonetic_Ext_Sup"),
        Playing_Cards("Playing_Cards"),
        Psalter_Pahlavi("Psalter_Pahlavi"),
        Private_Use_Area("PUA", "Private_Use"),
        General_Punctuation("Punctuation"),
        Rejang("Rejang"),
        Rumi_Numeral_Symbols("Rumi"),
        Runic("Runic"),
        Samaritan("Samaritan"),
        Saurashtra("Saurashtra"),
        Sharada("Sharada"),
        Shavian("Shavian"),
        Shorthand_Format_Controls("Shorthand_Format_Controls"),
        Siddham("Siddham"),
        Sinhala("Sinhala"),
        Sinhala_Archaic_Numbers("Sinhala_Archaic_Numbers"),
        Small_Form_Variants("Small_Forms"),
        Sora_Sompeng("Sora_Sompeng"),
        Specials("Specials"),
        Sundanese("Sundanese"),
        Sundanese_Supplement("Sundanese_Sup"),
        Supplemental_Arrows_A("Sup_Arrows_A"),
        Supplemental_Arrows_B("Sup_Arrows_B"),
        Supplemental_Arrows_C("Sup_Arrows_C"),
        Supplemental_Mathematical_Operators("Sup_Math_Operators"),
        Supplementary_Private_Use_Area_A("Sup_PUA_A"),
        Supplementary_Private_Use_Area_B("Sup_PUA_B"),
        Supplemental_Punctuation("Sup_Punctuation"),
        Superscripts_And_Subscripts("Super_And_Sub"),
        Syloti_Nagri("Syloti_Nagri"),
        Syriac("Syriac"),
        Tagalog("Tagalog"),
        Tagbanwa("Tagbanwa"),
        Tags("Tags"),
        Tai_Le("Tai_Le"),
        Tai_Tham("Tai_Tham"),
        Tai_Viet("Tai_Viet"),
        Tai_Xuan_Jing_Symbols("Tai_Xuan_Jing"),
        Takri("Takri"),
        Tamil("Tamil"),
        Telugu("Telugu"),
        Thaana("Thaana"),
        Thai("Thai"),
        Tibetan("Tibetan"),
        Tifinagh("Tifinagh"),
        Tirhuta("Tirhuta"),
        Transport_And_Map_Symbols("Transport_And_Map"),
        Unified_Canadian_Aboriginal_Syllabics("UCAS", "Canadian_Syllabics"),
        Unified_Canadian_Aboriginal_Syllabics_Extended("UCAS_Ext"),
        Ugaritic("Ugaritic"),
        Vai("Vai"),
        Vedic_Extensions("Vedic_Ext"),
        Vertical_Forms("Vertical_Forms"),
        Variation_Selectors("VS"),
        Variation_Selectors_Supplement("VS_Sup"),
        Warang_Citi("Warang_Citi"),
        Yi_Radicals("Yi_Radicals"),
        Yi_Syllables("Yi_Syllables"),
        Yijing_Hexagram_Symbols("Yijing");
        private final PropertyNames<Block_Values> names;
        private Block_Values (String shortName, String...otherNames) {
            names = new PropertyNames(Block_Values.class, this, shortName, otherNames);
        }
        public PropertyNames<Block_Values> getNames() {
            return names;
        }
        public String getShortName() {
            return names.getShortName();
        }
    }

		// CJK_Radical
	public enum Canonical_Combining_Class_Values implements Named {
        Not_Reordered("0", "NR"),
        Overlay("1", "OV"),
        Nukta("7", "NK"),
        Kana_Voicing("8", "KV"),
        Virama("9", "VR"),
        CCC10("10"),
        CCC11("11"),
        CCC12("12"),
        CCC13("13"),
        CCC14("14"),
        CCC15("15"),
        CCC16("16"),
        CCC17("17"),
        CCC18("18"),
        CCC19("19"),
        CCC20("20"),
        CCC21("21"),
        CCC22("22"),
        CCC23("23"),
        CCC24("24"),
        CCC25("25"),
        CCC26("26"),
        CCC27("27"),
        CCC28("28"),
        CCC29("29"),
        CCC30("30"),
        CCC31("31"),
        CCC32("32"),
        CCC33("33"),
        CCC34("34"),
        CCC35("35"),
        CCC36("36"),
        CCC84("84"),
        CCC91("91"),
        CCC103("103"),
        CCC107("107"),
        CCC118("118"),
        CCC122("122"),
        CCC129("129"),
        CCC130("130"),
        CCC132("132"),
        CCC133("133"),
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
        Iota_Subscript("240", "IS");
        private final PropertyNames<Canonical_Combining_Class_Values> names;
        private Canonical_Combining_Class_Values (String shortName, String...otherNames) {
            names = new PropertyNames(Canonical_Combining_Class_Values.class, this, shortName, otherNames);
        }
        public PropertyNames<Canonical_Combining_Class_Values> getNames() {
            return names;
        }
        public String getShortName() {
            return names.getShortName();
        }
    }

		// Case_Folding
		// Confusable_MA
		// Confusable_ML
		// Confusable_SA
		// Confusable_SL
		// Decomposition_Mapping
	public enum Decomposition_Type_Values implements Named {
        Canonical("Can", "can"),
        Compat("Com", "com"),
        Circle("Enc", "enc"),
        Final("Fin", "fin"),
        Font("Font", "font"),
        Fraction("Fra", "fra"),
        Initial("Init", "init"),
        Isolated("Iso", "iso"),
        Medial("Med", "med"),
        Narrow("Nar", "nar"),
        Nobreak("Nb", "nb"),
        None("None", "none"),
        Small("Sml", "sml"),
        Square("Sqr", "sqr"),
        Sub("Sub", "sub"),
        Super("Sup", "sup"),
        Vertical("Vert", "vert"),
        Wide("Wide", "wide");
        private final PropertyNames<Decomposition_Type_Values> names;
        private Decomposition_Type_Values (String shortName, String...otherNames) {
            names = new PropertyNames(Decomposition_Type_Values.class, this, shortName, otherNames);
        }
        public PropertyNames<Decomposition_Type_Values> getNames() {
            return names;
        }
        public String getShortName() {
            return names.getShortName();
        }
    }

	public enum East_Asian_Width_Values implements Named {
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
        public String getShortName() {
            return names.getShortName();
        }
    }

		// Emoji_DCM
		// Emoji_KDDI
		// Emoji_SB
		// FC_NFKC_Closure
	public enum General_Category_Values implements Named {
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
        public String getShortName() {
            return names.getShortName();
        }
    }

	public enum Grapheme_Cluster_Break_Values implements Named {
        Control("CN"),
        CR("CR"),
        Extend("EX"),
        L("L"),
        LF("LF"),
        LV("LV"),
        LVT("LVT"),
        Prepend("PP"),
        Regional_Indicator("RI"),
        SpacingMark("SM"),
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
        public String getShortName() {
            return names.getShortName();
        }
    }

	public enum Hangul_Syllable_Type_Values implements Named {
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
        public String getShortName() {
            return names.getShortName();
        }
    }

		// ISO_Comment
	public enum Id_Mod_Status_Values implements Named {
        restricted("r"),
        allowed("a");
        private final PropertyNames<Id_Mod_Status_Values> names;
        private Id_Mod_Status_Values (String shortName, String...otherNames) {
            names = new PropertyNames(Id_Mod_Status_Values.class, this, shortName, otherNames);
        }
        public PropertyNames<Id_Mod_Status_Values> getNames() {
            return names;
        }
        public String getShortName() {
            return names.getShortName();
        }
    }

	public enum Id_Mod_Type_Values implements Named {
        default_ignorable("di"),
        historic("h"),
        limited_use("lu"),
        not_chars("nc"),
        not_NFKC("nn"),
        not_xid("nx"),
        obsolete("o"),
        technical("t"),
        inclusion("inc"),
        recommended("rec");
        private final PropertyNames<Id_Mod_Type_Values> names;
        private Id_Mod_Type_Values (String shortName, String...otherNames) {
            names = new PropertyNames(Id_Mod_Type_Values.class, this, shortName, otherNames);
        }
        public PropertyNames<Id_Mod_Type_Values> getNames() {
            return names;
        }
        public String getShortName() {
            return names.getShortName();
        }
    }

	public enum Idn_2008_Values implements Named {
        NV8("nv8"),
        na("na");
        private final PropertyNames<Idn_2008_Values> names;
        private Idn_2008_Values (String shortName, String...otherNames) {
            names = new PropertyNames(Idn_2008_Values.class, this, shortName, otherNames);
        }
        public PropertyNames<Idn_2008_Values> getNames() {
            return names;
        }
        public String getShortName() {
            return names.getShortName();
        }
    }

		// Idn_Mapping
	public enum Idn_Status_Values implements Named {
        valid("v"),
        ignored("i"),
        mapped("m"),
        deviation("dv"),
        disallowed("da"),
        disallowed_STD3_valid("ds3v"),
        disallowed_STD3_mapped("ds3m");
        private final PropertyNames<Idn_Status_Values> names;
        private Idn_Status_Values (String shortName, String...otherNames) {
            names = new PropertyNames(Idn_Status_Values.class, this, shortName, otherNames);
        }
        public PropertyNames<Idn_Status_Values> getNames() {
            return names;
        }
        public String getShortName() {
            return names.getShortName();
        }
    }

	public enum Indic_Matra_Category_Values implements Named {
        Bottom("Bottom"),
        Bottom_And_Right("Bottom_And_Right"),
        Invisible("Invisible"),
        Left("Left"),
        Left_And_Right("Left_And_Right"),
        NA("NA"),
        Overstruck("Overstruck"),
        Right("Right"),
        Top("Top"),
        Top_And_Bottom("Top_And_Bottom"),
        Top_And_Bottom_And_Right("Top_And_Bottom_And_Right"),
        Top_And_Left("Top_And_Left"),
        Top_And_Left_And_Right("Top_And_Left_And_Right"),
        Top_And_Right("Top_And_Right"),
        Visual_Order_Left("Visual_Order_Left");
        private final PropertyNames<Indic_Matra_Category_Values> names;
        private Indic_Matra_Category_Values (String shortName, String...otherNames) {
            names = new PropertyNames(Indic_Matra_Category_Values.class, this, shortName, otherNames);
        }
        public PropertyNames<Indic_Matra_Category_Values> getNames() {
            return names;
        }
        public String getShortName() {
            return names.getShortName();
        }
    }

	public enum Indic_Syllabic_Category_Values implements Named {
        Avagraha("Avagraha"),
        Bindu("Bindu"),
        Consonant("Consonant"),
        Consonant_Dead("Consonant_Dead"),
        Consonant_Final("Consonant_Final"),
        Consonant_Head_Letter("Consonant_Head_Letter"),
        Consonant_Medial("Consonant_Medial"),
        Consonant_Placeholder("Consonant_Placeholder"),
        Consonant_Repha("Consonant_Repha"),
        Consonant_Subjoined("Consonant_Subjoined"),
        Modifying_Letter("Modifying_Letter"),
        Nukta("Nukta"),
        Other("Other"),
        Register_Shifter("Register_Shifter"),
        Tone_Letter("Tone_Letter"),
        Tone_Mark("Tone_Mark"),
        Virama("Virama"),
        Visarga("Visarga"),
        Vowel("Vowel"),
        Vowel_Dependent("Vowel_Dependent"),
        Vowel_Independent("Vowel_Independent");
        private final PropertyNames<Indic_Syllabic_Category_Values> names;
        private Indic_Syllabic_Category_Values (String shortName, String...otherNames) {
            names = new PropertyNames(Indic_Syllabic_Category_Values.class, this, shortName, otherNames);
        }
        public PropertyNames<Indic_Syllabic_Category_Values> getNames() {
            return names;
        }
        public String getShortName() {
            return names.getShortName();
        }
    }

	public enum Jamo_Short_Name_Values implements Named {
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
        public String getShortName() {
            return names.getShortName();
        }
    }

	public enum Joining_Group_Values implements Named {
        Ain("Ain"),
        Alaph("Alaph"),
        Alef("Alef"),
        Beh("Beh"),
        Beth("Beth"),
        Burushaski_Yeh_Barree("Burushaski_Yeh_Barree"),
        Dal("Dal"),
        Dalath_Rish("Dalath_Rish"),
        E("E"),
        Farsi_Yeh("Farsi_Yeh"),
        Fe("Fe"),
        Feh("Feh"),
        Final_Semkath("Final_Semkath"),
        Gaf("Gaf"),
        Gamal("Gamal"),
        Hah("Hah"),
        He("He"),
        Heh("Heh"),
        Heh_Goal("Heh_Goal"),
        Heth("Heth"),
        Kaf("Kaf"),
        Kaph("Kaph"),
        Khaph("Khaph"),
        Knotted_Heh("Knotted_Heh"),
        Lam("Lam"),
        Lamadh("Lamadh"),
        Manichaean_Aleph("Manichaean_Aleph"),
        Manichaean_Ayin("Manichaean_Ayin"),
        Manichaean_Beth("Manichaean_Beth"),
        Manichaean_Daleth("Manichaean_Daleth"),
        Manichaean_Dhamedh("Manichaean_Dhamedh"),
        Manichaean_Five("Manichaean_Five"),
        Manichaean_Gimel("Manichaean_Gimel"),
        Manichaean_Heth("Manichaean_Heth"),
        Manichaean_Hundred("Manichaean_Hundred"),
        Manichaean_Kaph("Manichaean_Kaph"),
        Manichaean_Lamedh("Manichaean_Lamedh"),
        Manichaean_Mem("Manichaean_Mem"),
        Manichaean_Nun("Manichaean_Nun"),
        Manichaean_One("Manichaean_One"),
        Manichaean_Pe("Manichaean_Pe"),
        Manichaean_Qoph("Manichaean_Qoph"),
        Manichaean_Resh("Manichaean_Resh"),
        Manichaean_Sadhe("Manichaean_Sadhe"),
        Manichaean_Samekh("Manichaean_Samekh"),
        Manichaean_Taw("Manichaean_Taw"),
        Manichaean_Ten("Manichaean_Ten"),
        Manichaean_Teth("Manichaean_Teth"),
        Manichaean_Thamedh("Manichaean_Thamedh"),
        Manichaean_Twenty("Manichaean_Twenty"),
        Manichaean_Waw("Manichaean_Waw"),
        Manichaean_Yodh("Manichaean_Yodh"),
        Manichaean_Zayin("Manichaean_Zayin"),
        Meem("Meem"),
        Mim("Mim"),
        No_Joining_Group("No_Joining_Group"),
        Noon("Noon"),
        Nun("Nun"),
        Nya("Nya"),
        Pe("Pe"),
        Qaf("Qaf"),
        Qaph("Qaph"),
        Reh("Reh"),
        Reversed_Pe("Reversed_Pe"),
        Rohingya_Yeh("Rohingya_Yeh"),
        Sad("Sad"),
        Sadhe("Sadhe"),
        Seen("Seen"),
        Semkath("Semkath"),
        Shin("Shin"),
        Straight_Waw("Straight_Waw"),
        Swash_Kaf("Swash_Kaf"),
        Syriac_Waw("Syriac_Waw"),
        Tah("Tah"),
        Taw("Taw"),
        Teh_Marbuta("Teh_Marbuta"),
        Hamza_On_Heh_Goal("Teh_Marbuta_Goal"),
        Teth("Teth"),
        Waw("Waw"),
        Yeh("Yeh"),
        Yeh_Barree("Yeh_Barree"),
        Yeh_With_Tail("Yeh_With_Tail"),
        Yudh("Yudh"),
        Yudh_He("Yudh_He"),
        Zain("Zain"),
        Zhain("Zhain");
        private final PropertyNames<Joining_Group_Values> names;
        private Joining_Group_Values (String shortName, String...otherNames) {
            names = new PropertyNames(Joining_Group_Values.class, this, shortName, otherNames);
        }
        public PropertyNames<Joining_Group_Values> getNames() {
            return names;
        }
        public String getShortName() {
            return names.getShortName();
        }
    }

	public enum Joining_Type_Values implements Named {
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
        public String getShortName() {
            return names.getShortName();
        }
    }

	public enum Line_Break_Values implements Named {
        Ambiguous("AI"),
        Alphabetic("AL"),
        Break_Both("B2"),
        Break_After("BA"),
        Break_Before("BB"),
        Mandatory_Break("BK"),
        Contingent_Break("CB"),
        Conditional_Japanese_Starter("CJ"),
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
        Regional_Indicator("RI"),
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
        public String getShortName() {
            return names.getShortName();
        }
    }

		// Lowercase_Mapping
	public enum NFC_Quick_Check_Values implements Named {
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
        public String getShortName() {
            return names.getShortName();
        }
    }

	public enum NFD_Quick_Check_Values implements Named {
        No("N"),
        Yes("Y");
        private final PropertyNames<NFD_Quick_Check_Values> names;
        private NFD_Quick_Check_Values (String shortName, String...otherNames) {
            names = new PropertyNames(NFD_Quick_Check_Values.class, this, shortName, otherNames);
        }
        public PropertyNames<NFD_Quick_Check_Values> getNames() {
            return names;
        }
        public String getShortName() {
            return names.getShortName();
        }
    }

		// NFKC_Casefold
	public enum NFKC_Quick_Check_Values implements Named {
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
        public String getShortName() {
            return names.getShortName();
        }
    }

	public enum NFKD_Quick_Check_Values implements Named {
        No("N"),
        Yes("Y");
        private final PropertyNames<NFKD_Quick_Check_Values> names;
        private NFKD_Quick_Check_Values (String shortName, String...otherNames) {
            names = new PropertyNames(NFKD_Quick_Check_Values.class, this, shortName, otherNames);
        }
        public PropertyNames<NFKD_Quick_Check_Values> getNames() {
            return names;
        }
        public String getShortName() {
            return names.getShortName();
        }
    }

		// Name
		// Name_Alias
		// Named_Sequences
		// Named_Sequences_Prov
	public enum Numeric_Type_Values implements Named {
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
        public String getShortName() {
            return names.getShortName();
        }
    }

		// Numeric_Value
	public enum Script_Values implements Named {
        Caucasian_Albanian("Aghb"),
        Arabic("Arab"),
        Imperial_Aramaic("Armi"),
        Armenian("Armn"),
        Avestan("Avst"),
        Balinese("Bali"),
        Bamum("Bamu"),
        Bassa_Vah("Bass"),
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
        Duployan("Dupl"),
        Egyptian_Hieroglyphs("Egyp"),
        Elbasan("Elba"),
        Ethiopic("Ethi"),
        Georgian("Geor"),
        Glagolitic("Glag"),
        Gothic("Goth"),
        Grantha("Gran"),
        Greek("Grek"),
        Gujarati("Gujr"),
        Gurmukhi("Guru"),
        Hangul("Hang"),
        Han("Hani"),
        Hanunoo("Hano"),
        Hebrew("Hebr"),
        Hiragana("Hira"),
        Pahawh_Hmong("Hmng"),
        Katakana_Or_Hiragana("Hrkt"),
        Old_Italic("Ital"),
        Javanese("Java"),
        Kayah_Li("Kali"),
        Katakana("Kana"),
        Kharoshthi("Khar"),
        Khmer("Khmr"),
        Khojki("Khoj"),
        Kannada("Knda"),
        Kaithi("Kthi"),
        Tai_Tham("Lana"),
        Lao("Laoo"),
        Latin("Latn"),
        Lepcha("Lepc"),
        Limbu("Limb"),
        Linear_A("Lina"),
        Linear_B("Linb"),
        Lisu("Lisu"),
        Lycian("Lyci"),
        Lydian("Lydi"),
        Mahajani("Mahj"),
        Mandaic("Mand"),
        Manichaean("Mani"),
        Mende_Kikakui("Mend"),
        Meroitic_Cursive("Merc"),
        Meroitic_Hieroglyphs("Mero"),
        Malayalam("Mlym"),
        Modi("Modi"),
        Mongolian("Mong"),
        Mro("Mroo"),
        Meetei_Mayek("Mtei"),
        Myanmar("Mymr"),
        Old_North_Arabian("Narb"),
        Nabataean("Nbat"),
        Nko("Nkoo"),
        Ogham("Ogam"),
        Ol_Chiki("Olck"),
        Old_Turkic("Orkh"),
        Oriya("Orya"),
        Osmanya("Osma"),
        Palmyrene("Palm"),
        Pau_Cin_Hau("Pauc"),
        Old_Permic("Perm"),
        Phags_Pa("Phag"),
        Inscriptional_Pahlavi("Phli"),
        Psalter_Pahlavi("Phlp"),
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
        Siddham("Sidd"),
        Khudawadi("Sind"),
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
        Tirhuta("Tirh"),
        Ugaritic("Ugar"),
        Vai("Vaii"),
        Warang_Citi("Wara"),
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
        public String getShortName() {
            return names.getShortName();
        }
    }

		// Script_Extensions
	public enum Sentence_Break_Values implements Named {
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
        public String getShortName() {
            return names.getShortName();
        }
    }

		// Simple_Case_Folding
		// Simple_Lowercase_Mapping
		// Simple_Titlecase_Mapping
		// Simple_Uppercase_Mapping
		// Standardized_Variant
		// Titlecase_Mapping
		// Unicode_1_Name
		// Uppercase_Mapping
	public enum Word_Break_Values implements Named {
        CR("CR"),
        Double_Quote("DQ"),
        ExtendNumLet("EX"),
        Extend("Extend"),
        Format("FO"),
        Hebrew_Letter("HL"),
        Katakana("KA"),
        ALetter("LE"),
        LF("LF"),
        MidNumLet("MB"),
        MidLetter("ML"),
        MidNum("MN"),
        Newline("NL"),
        Numeric("NU"),
        Regional_Indicator("RI"),
        Single_Quote("SQ"),
        Other("XX");
        private final PropertyNames<Word_Break_Values> names;
        private Word_Break_Values (String shortName, String...otherNames) {
            names = new PropertyNames(Word_Break_Values.class, this, shortName, otherNames);
        }
        public PropertyNames<Word_Break_Values> getNames() {
            return names;
        }
        public String getShortName() {
            return names.getShortName();
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
