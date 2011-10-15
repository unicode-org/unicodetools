package org.unicode.props;

public class PropertyValuesCopy {

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
        public Binary forString(String name) {
            return names.forString(name);
        }
    }

    public enum General_Category_Values {
        Other("C", "Other"),
        Control("Cc", "Control", "cntrl"),
        Format("Cf", "Format"),
        Unassigned("Cn", "Unassigned"),
        Private_Use("Co", "Private_Use"),
        Surrogate("Cs", "Surrogate"),
        Letter("L", "Letter"),
        Cased_Letter("LC", "Cased_Letter"),
        Lowercase_Letter("Ll", "Lowercase_Letter"),
        Modifier_Letter("Lm", "Modifier_Letter"),
        Other_Letter("Lo", "Other_Letter"),
        Titlecase_Letter("Lt", "Titlecase_Letter"),
        Uppercase_Letter("Lu", "Uppercase_Letter"),
        Mark("M", "Mark", "Combining_Mark"),
        Spacing_Mark("Mc", "Spacing_Mark"),
        Enclosing_Mark("Me", "Enclosing_Mark"),
        Nonspacing_Mark("Mn", "Nonspacing_Mark"),
        Number("N", "Number"),
        Decimal_Number("Nd", "Decimal_Number", "digit"),
        Letter_Number("Nl", "Letter_Number"),
        Other_Number("No", "Other_Number"),
        Punctuation("P", "Punctuation", "punct"),
        Connector_Punctuation("Pc", "Connector_Punctuation"),
        Dash_Punctuation("Pd", "Dash_Punctuation"),
        Close_Punctuation("Pe", "Close_Punctuation"),
        Final_Punctuation("Pf", "Final_Punctuation"),
        Initial_Punctuation("Pi", "Initial_Punctuation"),
        Other_Punctuation("Po", "Other_Punctuation"),
        Open_Punctuation("Ps", "Open_Punctuation"),
        Symbol("S", "Symbol"),
        Currency_Symbol("Sc", "Currency_Symbol"),
        Modifier_Symbol("Sk", "Modifier_Symbol"),
        Math_Symbol("Sm", "Math_Symbol"),
        Other_Symbol("So", "Other_Symbol"),
        Separator("Z", "Separator"),
        Line_Separator("Zl", "Line_Separator"),
        Paragraph_Separator("Zp", "Paragraph_Separator"),
        Space_Separator("Zs", "Space_Separator");
        private final PropertyNames<General_Category_Values> names;
        private General_Category_Values (String shortName, String...otherNames) {
            names = new PropertyNames(General_Category_Values.class, this, shortName, otherNames);
        }
        public PropertyNames<General_Category_Values> getNames() {
            return names;
        }
        public General_Category_Values forString(String name) {
            return names.forString(name);
        }
    }

    //    public enum Age_Values {
    //        _1_1("1.1"),
    //        _2_0("2.0"),
    //        _2_1("2.1"),
    //        _3_0("3.0"),
    //        _3_1("3.1"),
    //        _3_2("3.2"),
    //        _4_0("4.0"),
    //        _4_1("4.1"),
    //        _5_0("5.0"),
    //        _5_1("5.1"),
    //        _5_2("5.2"),
    //        _6_0("6.0"),
    //        _6_1("6.1"),
    //        unassigned("unassigned");
    //        private Age_Values (String...otherNames) {
    //            addNames(UcdProperty.Age, otherNames, this);
    //        }
    //    }
    //    public enum Alphabetic_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Alphabetic_Values (String...otherNames) {
    //            addNames(UcdProperty.Alphabetic, otherNames, this);
    //        }
    //    }
    //    public enum Bidi_Class_Values {
    //        Arabic_Letter("AL", "Arabic_Letter"),
    //        Arabic_Number("AN", "Arabic_Number"),
    //        Paragraph_Separator("B", "Paragraph_Separator"),
    //        Boundary_Neutral("BN", "Boundary_Neutral"),
    //        Common_Separator("CS", "Common_Separator"),
    //        European_Number("EN", "European_Number"),
    //        European_Separator("ES", "European_Separator"),
    //        European_Terminator("ET", "European_Terminator"),
    //        Left_To_Right("L", "Left_To_Right"),
    //        Left_To_Right_Embedding("LRE", "Left_To_Right_Embedding"),
    //        Left_To_Right_Override("LRO", "Left_To_Right_Override"),
    //        Nonspacing_Mark("NSM", "Nonspacing_Mark"),
    //        Other_Neutral("ON", "Other_Neutral"),
    //        Pop_Directional_Format("PDF", "Pop_Directional_Format"),
    //        Right_To_Left("R", "Right_To_Left"),
    //        Right_To_Left_Embedding("RLE", "Right_To_Left_Embedding"),
    //        Right_To_Left_Override("RLO", "Right_To_Left_Override"),
    //        Segment_Separator("S", "Segment_Separator"),
    //        White_Space("WS", "White_Space");
    //        private Bidi_Class_Values (String...otherNames) {
    //            addNames(UcdProperty.Bidi_Class, otherNames, this);
    //        }
    //    }
    //    public enum Bidi_Control_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Bidi_Control_Values (String...otherNames) {
    //            addNames(UcdProperty.Bidi_Control, otherNames, this);
    //        }
    //    }
    //    public enum Bidi_Mirrored_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Bidi_Mirrored_Values (String...otherNames) {
    //            addNames(UcdProperty.Bidi_Mirrored, otherNames, this);
    //        }
    //    }
    //    // Bidi_Mirroring_Glyph
    //    public enum Block_Values {
    //        Aegean_Numbers("Aegean_Numbers"),
    //        Alchemical_Symbols("Alchemical_Symbols"),
    //        Alphabetic_Presentation_Forms("Alphabetic_Presentation_Forms"),
    //        Ancient_Greek_Musical_Notation("Ancient_Greek_Musical_Notation"),
    //        Ancient_Greek_Numbers("Ancient_Greek_Numbers"),
    //        Ancient_Symbols("Ancient_Symbols"),
    //        Arabic("Arabic"),
    //        Arabic_Extended_A("Arabic_Extended_A"),
    //        Arabic_Mathematical_Alphabetic_Symbols("Arabic_Mathematical_Alphabetic_Symbols"),
    //        Arabic_Presentation_Forms_A("Arabic_Presentation_Forms_A", "Arabic_Presentation_Forms-A"),
    //        Arabic_Presentation_Forms_B("Arabic_Presentation_Forms_B"),
    //        Arabic_Supplement("Arabic_Supplement"),
    //        Armenian("Armenian"),
    //        Arrows("Arrows"),
    //        Avestan("Avestan"),
    //        Balinese("Balinese"),
    //        Bamum("Bamum"),
    //        Bamum_Supplement("Bamum_Supplement"),
    //        Basic_Latin("Basic_Latin", "ASCII"),
    //        Batak("Batak"),
    //        Bengali("Bengali"),
    //        Block_Elements("Block_Elements"),
    //        Bopomofo("Bopomofo"),
    //        Bopomofo_Extended("Bopomofo_Extended"),
    //        Box_Drawing("Box_Drawing"),
    //        Brahmi("Brahmi"),
    //        Braille_Patterns("Braille_Patterns"),
    //        Buginese("Buginese"),
    //        Buhid("Buhid"),
    //        Byzantine_Musical_Symbols("Byzantine_Musical_Symbols"),
    //        Carian("Carian"),
    //        Chakma("Chakma"),
    //        Cham("Cham"),
    //        Cherokee("Cherokee"),
    //        CJK_Compatibility("CJK_Compatibility"),
    //        CJK_Compatibility_Forms("CJK_Compatibility_Forms"),
    //        CJK_Compatibility_Ideographs("CJK_Compatibility_Ideographs"),
    //        CJK_Compatibility_Ideographs_Supplement("CJK_Compatibility_Ideographs_Supplement"),
    //        CJK_Radicals_Supplement("CJK_Radicals_Supplement"),
    //        CJK_Strokes("CJK_Strokes"),
    //        CJK_Symbols_And_Punctuation("CJK_Symbols_And_Punctuation"),
    //        CJK_Unified_Ideographs("CJK_Unified_Ideographs"),
    //        CJK_Unified_Ideographs_Extension_A("CJK_Unified_Ideographs_Extension_A"),
    //        CJK_Unified_Ideographs_Extension_B("CJK_Unified_Ideographs_Extension_B"),
    //        CJK_Unified_Ideographs_Extension_C("CJK_Unified_Ideographs_Extension_C"),
    //        CJK_Unified_Ideographs_Extension_D("CJK_Unified_Ideographs_Extension_D"),
    //        Combining_Diacritical_Marks("Combining_Diacritical_Marks"),
    //        Combining_Diacritical_Marks_For_Symbols("Combining_Diacritical_Marks_For_Symbols", "Combining_Marks_For_Symbols"),
    //        Combining_Diacritical_Marks_Supplement("Combining_Diacritical_Marks_Supplement"),
    //        Combining_Half_Marks("Combining_Half_Marks"),
    //        Common_Indic_Number_Forms("Common_Indic_Number_Forms"),
    //        Control_Pictures("Control_Pictures"),
    //        Coptic("Coptic"),
    //        Counting_Rod_Numerals("Counting_Rod_Numerals"),
    //        Cuneiform("Cuneiform"),
    //        Cuneiform_Numbers_And_Punctuation("Cuneiform_Numbers_And_Punctuation"),
    //        Currency_Symbols("Currency_Symbols"),
    //        Cypriot_Syllabary("Cypriot_Syllabary"),
    //        Cyrillic("Cyrillic"),
    //        Cyrillic_Extended_A("Cyrillic_Extended_A"),
    //        Cyrillic_Extended_B("Cyrillic_Extended_B"),
    //        Cyrillic_Supplement("Cyrillic_Supplement", "Cyrillic_Supplementary"),
    //        Deseret("Deseret"),
    //        Devanagari("Devanagari"),
    //        Devanagari_Extended("Devanagari_Extended"),
    //        Dingbats("Dingbats"),
    //        Domino_Tiles("Domino_Tiles"),
    //        Egyptian_Hieroglyphs("Egyptian_Hieroglyphs"),
    //        Emoticons("Emoticons"),
    //        Enclosed_Alphanumeric_Supplement("Enclosed_Alphanumeric_Supplement"),
    //        Enclosed_Alphanumerics("Enclosed_Alphanumerics"),
    //        Enclosed_CJK_Letters_And_Months("Enclosed_CJK_Letters_And_Months"),
    //        Enclosed_Ideographic_Supplement("Enclosed_Ideographic_Supplement"),
    //        Ethiopic("Ethiopic"),
    //        Ethiopic_Extended("Ethiopic_Extended"),
    //        Ethiopic_Extended_A("Ethiopic_Extended_A"),
    //        Ethiopic_Supplement("Ethiopic_Supplement"),
    //        General_Punctuation("General_Punctuation"),
    //        Geometric_Shapes("Geometric_Shapes"),
    //        Georgian("Georgian"),
    //        Georgian_Supplement("Georgian_Supplement"),
    //        Glagolitic("Glagolitic"),
    //        Gothic("Gothic"),
    //        Greek_And_Coptic("Greek_And_Coptic", "Greek"),
    //        Greek_Extended("Greek_Extended"),
    //        Gujarati("Gujarati"),
    //        Gurmukhi("Gurmukhi"),
    //        Halfwidth_And_Fullwidth_Forms("Halfwidth_And_Fullwidth_Forms"),
    //        Hangul_Compatibility_Jamo("Hangul_Compatibility_Jamo"),
    //        Hangul_Jamo("Hangul_Jamo"),
    //        Hangul_Jamo_Extended_A("Hangul_Jamo_Extended_A"),
    //        Hangul_Jamo_Extended_B("Hangul_Jamo_Extended_B"),
    //        Hangul_Syllables("Hangul_Syllables"),
    //        Hanunoo("Hanunoo"),
    //        Hebrew("Hebrew"),
    //        High_Private_Use_Surrogates("High_Private_Use_Surrogates"),
    //        High_Surrogates("High_Surrogates"),
    //        Hiragana("Hiragana"),
    //        Ideographic_Description_Characters("Ideographic_Description_Characters"),
    //        Imperial_Aramaic("Imperial_Aramaic"),
    //        Inscriptional_Pahlavi("Inscriptional_Pahlavi"),
    //        Inscriptional_Parthian("Inscriptional_Parthian"),
    //        IPA_Extensions("IPA_Extensions"),
    //        Javanese("Javanese"),
    //        Kaithi("Kaithi"),
    //        Kana_Supplement("Kana_Supplement"),
    //        Kanbun("Kanbun"),
    //        Kangxi_Radicals("Kangxi_Radicals"),
    //        Kannada("Kannada"),
    //        Katakana("Katakana"),
    //        Katakana_Phonetic_Extensions("Katakana_Phonetic_Extensions"),
    //        Kayah_Li("Kayah_Li"),
    //        Kharoshthi("Kharoshthi"),
    //        Khmer("Khmer"),
    //        Khmer_Symbols("Khmer_Symbols"),
    //        Lao("Lao"),
    //        Latin_1_Supplement("Latin_1_Supplement", "Latin_1"),
    //        Latin_Extended_A("Latin_Extended_A"),
    //        Latin_Extended_Additional("Latin_Extended_Additional"),
    //        Latin_Extended_B("Latin_Extended_B"),
    //        Latin_Extended_C("Latin_Extended_C"),
    //        Latin_Extended_D("Latin_Extended_D"),
    //        Lepcha("Lepcha"),
    //        Letterlike_Symbols("Letterlike_Symbols"),
    //        Limbu("Limbu"),
    //        Linear_B_Ideograms("Linear_B_Ideograms"),
    //        Linear_B_Syllabary("Linear_B_Syllabary"),
    //        Lisu("Lisu"),
    //        Low_Surrogates("Low_Surrogates"),
    //        Lycian("Lycian"),
    //        Lydian("Lydian"),
    //        Mahjong_Tiles("Mahjong_Tiles"),
    //        Malayalam("Malayalam"),
    //        Mandaic("Mandaic"),
    //        Mathematical_Alphanumeric_Symbols("Mathematical_Alphanumeric_Symbols"),
    //        Mathematical_Operators("Mathematical_Operators"),
    //        Meetei_Mayek("Meetei_Mayek"),
    //        Meetei_Mayek_Extensions("Meetei_Mayek_Extensions"),
    //        Meroitic_Cursive("Meroitic_Cursive"),
    //        Meroitic_Hieroglyphs("Meroitic_Hieroglyphs"),
    //        Miao("Miao"),
    //        Miscellaneous_Mathematical_Symbols_A("Miscellaneous_Mathematical_Symbols_A"),
    //        Miscellaneous_Mathematical_Symbols_B("Miscellaneous_Mathematical_Symbols_B"),
    //        Miscellaneous_Symbols("Miscellaneous_Symbols"),
    //        Miscellaneous_Symbols_And_Arrows("Miscellaneous_Symbols_And_Arrows"),
    //        Miscellaneous_Symbols_And_Pictographs("Miscellaneous_Symbols_And_Pictographs"),
    //        Miscellaneous_Technical("Miscellaneous_Technical"),
    //        Modifier_Tone_Letters("Modifier_Tone_Letters"),
    //        Mongolian("Mongolian"),
    //        Musical_Symbols("Musical_Symbols"),
    //        Myanmar("Myanmar"),
    //        Myanmar_Extended_A("Myanmar_Extended_A"),
    //        New_Tai_Lue("New_Tai_Lue"),
    //        NKo("NKo"),
    //        No_Block("No_Block"),
    //        Number_Forms("Number_Forms"),
    //        Ogham("Ogham"),
    //        Ol_Chiki("Ol_Chiki"),
    //        Old_Italic("Old_Italic"),
    //        Old_Persian("Old_Persian"),
    //        Old_South_Arabian("Old_South_Arabian"),
    //        Old_Turkic("Old_Turkic"),
    //        Optical_Character_Recognition("Optical_Character_Recognition"),
    //        Oriya("Oriya"),
    //        Osmanya("Osmanya"),
    //        Phags_Pa("Phags_Pa"),
    //        Phaistos_Disc("Phaistos_Disc"),
    //        Phoenician("Phoenician"),
    //        Phonetic_Extensions("Phonetic_Extensions"),
    //        Phonetic_Extensions_Supplement("Phonetic_Extensions_Supplement"),
    //        Playing_Cards("Playing_Cards"),
    //        Private_Use_Area("Private_Use_Area", "Private_Use"),
    //        Rejang("Rejang"),
    //        Rumi_Numeral_Symbols("Rumi_Numeral_Symbols"),
    //        Runic("Runic"),
    //        Samaritan("Samaritan"),
    //        Saurashtra("Saurashtra"),
    //        Sharada("Sharada"),
    //        Shavian("Shavian"),
    //        Sinhala("Sinhala"),
    //        Small_Form_Variants("Small_Form_Variants"),
    //        Sora_Sompeng("Sora_Sompeng"),
    //        Spacing_Modifier_Letters("Spacing_Modifier_Letters"),
    //        Specials("Specials"),
    //        Sundanese("Sundanese"),
    //        Sundanese_Supplement("Sundanese_Supplement"),
    //        Superscripts_And_Subscripts("Superscripts_And_Subscripts"),
    //        Supplemental_Arrows_A("Supplemental_Arrows_A"),
    //        Supplemental_Arrows_B("Supplemental_Arrows_B"),
    //        Supplemental_Mathematical_Operators("Supplemental_Mathematical_Operators"),
    //        Supplemental_Punctuation("Supplemental_Punctuation"),
    //        Supplementary_Private_Use_Area_A("Supplementary_Private_Use_Area_A"),
    //        Supplementary_Private_Use_Area_B("Supplementary_Private_Use_Area_B"),
    //        Syloti_Nagri("Syloti_Nagri"),
    //        Syriac("Syriac"),
    //        Tagalog("Tagalog"),
    //        Tagbanwa("Tagbanwa"),
    //        Tags("Tags"),
    //        Tai_Le("Tai_Le"),
    //        Tai_Tham("Tai_Tham"),
    //        Tai_Viet("Tai_Viet"),
    //        Tai_Xuan_Jing_Symbols("Tai_Xuan_Jing_Symbols"),
    //        Takri("Takri"),
    //        Tamil("Tamil"),
    //        Telugu("Telugu"),
    //        Thaana("Thaana"),
    //        Thai("Thai"),
    //        Tibetan("Tibetan"),
    //        Tifinagh("Tifinagh"),
    //        Transport_And_Map_Symbols("Transport_And_Map_Symbols"),
    //        Ugaritic("Ugaritic"),
    //        Unified_Canadian_Aboriginal_Syllabics("Unified_Canadian_Aboriginal_Syllabics", "Canadian_Syllabics"),
    //        Unified_Canadian_Aboriginal_Syllabics_Extended("Unified_Canadian_Aboriginal_Syllabics_Extended"),
    //        Vai("Vai"),
    //        Variation_Selectors("Variation_Selectors"),
    //        Variation_Selectors_Supplement("Variation_Selectors_Supplement"),
    //        Vedic_Extensions("Vedic_Extensions"),
    //        Vertical_Forms("Vertical_Forms"),
    //        Yi_Radicals("Yi_Radicals"),
    //        Yi_Syllables("Yi_Syllables"),
    //        Yijing_Hexagram_Symbols("Yijing_Hexagram_Symbols");
    //        private Block_Values (String...otherNames) {
    //            addNames(UcdProperty.Block, otherNames, this);
    //        }
    //    }
    //    public enum Canonical_Combining_Class_Values {
    //        NR("0", "NR", "Not_Reordered"),
    //        OV("1", "OV", "Overlay"),
    //        NK("7", "NK", "Nukta"),
    //        KV("8", "KV", "Kana_Voicing"),
    //        VR("9", "VR", "Virama"),
    //        ATBL("200", "ATBL", "Attached_Below_Left"),
    //        ATB("202", "ATB", "Attached_Below"),
    //        ATA("214", "ATA", "Attached_Above"),
    //        ATAR("216", "ATAR", "Attached_Above_Right"),
    //        BL("218", "BL", "Below_Left"),
    //        B("220", "B", "Below"),
    //        BR("222", "BR", "Below_Right"),
    //        L("224", "L", "Left"),
    //        R("226", "R", "Right"),
    //        AL("228", "AL", "Above_Left"),
    //        A("230", "A", "Above"),
    //        AR("232", "AR", "Above_Right"),
    //        DB("233", "DB", "Double_Below"),
    //        DA("234", "DA", "Double_Above"),
    //        IS("240", "IS", "Iota_Subscript");
    //        private Canonical_Combining_Class_Values (String...otherNames) {
    //            addNames(UcdProperty.Canonical_Combining_Class, otherNames, this);
    //        }
    //    }
    //    // Case_Folding
    //    public enum Case_Ignorable_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Case_Ignorable_Values (String...otherNames) {
    //            addNames(UcdProperty.Case_Ignorable, otherNames, this);
    //        }
    //    }
    //    public enum Cased_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Cased_Values (String...otherNames) {
    //            addNames(UcdProperty.Cased, otherNames, this);
    //        }
    //    }
    //    public enum Changes_When_Casefolded_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Changes_When_Casefolded_Values (String...otherNames) {
    //            addNames(UcdProperty.Changes_When_Casefolded, otherNames, this);
    //        }
    //    }
    //    public enum Changes_When_Casemapped_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Changes_When_Casemapped_Values (String...otherNames) {
    //            addNames(UcdProperty.Changes_When_Casemapped, otherNames, this);
    //        }
    //    }
    //    public enum Changes_When_Lowercased_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Changes_When_Lowercased_Values (String...otherNames) {
    //            addNames(UcdProperty.Changes_When_Lowercased, otherNames, this);
    //        }
    //    }
    //    public enum Changes_When_NFKC_Casefolded_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Changes_When_NFKC_Casefolded_Values (String...otherNames) {
    //            addNames(UcdProperty.Changes_When_NFKC_Casefolded, otherNames, this);
    //        }
    //    }
    //    public enum Changes_When_Titlecased_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Changes_When_Titlecased_Values (String...otherNames) {
    //            addNames(UcdProperty.Changes_When_Titlecased, otherNames, this);
    //        }
    //    }
    //    public enum Changes_When_Uppercased_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Changes_When_Uppercased_Values (String...otherNames) {
    //            addNames(UcdProperty.Changes_When_Uppercased, otherNames, this);
    //        }
    //    }
    //    public enum Composition_Exclusion_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Composition_Exclusion_Values (String...otherNames) {
    //            addNames(UcdProperty.Composition_Exclusion, otherNames, this);
    //        }
    //    }
    //    public enum Dash_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Dash_Values (String...otherNames) {
    //            addNames(UcdProperty.Dash, otherNames, this);
    //        }
    //    }
    //    // Decomposition_Mapping
    //    public enum Decomposition_Type_Values {
    //        Canonical("Can", "Canonical", "can"),
    //        Compat("Com", "Compat", "com"),
    //        Circle("Enc", "Circle", "enc"),
    //        Final("Fin", "Final", "fin"),
    //        font("Font", "font"),
    //        Fraction("Fra", "Fraction", "fra"),
    //        Initial("Init", "Initial", "init"),
    //        Isolated("Iso", "Isolated", "iso"),
    //        Medial("Med", "Medial", "med"),
    //        Narrow("Nar", "Narrow", "nar"),
    //        Nobreak("Nb", "Nobreak", "nb"),
    //        none("None", "none"),
    //        Small("Sml", "Small", "sml"),
    //        Square("Sqr", "Square", "sqr"),
    //        sub("Sub", "sub"),
    //        Super("Sup", "Super", "sup"),
    //        Vertical("Vert", "Vertical", "vert"),
    //        wide("Wide", "wide");
    //        private Decomposition_Type_Values (String...otherNames) {
    //            addNames(UcdProperty.Decomposition_Type, otherNames, this);
    //        }
    //    }
    //    public enum Default_Ignorable_Code_Point_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Default_Ignorable_Code_Point_Values (String...otherNames) {
    //            addNames(UcdProperty.Default_Ignorable_Code_Point, otherNames, this);
    //        }
    //    }
    //    public enum Deprecated_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Deprecated_Values (String...otherNames) {
    //            addNames(UcdProperty.Deprecated, otherNames, this);
    //        }
    //    }
    //    public enum Diacritic_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Diacritic_Values (String...otherNames) {
    //            addNames(UcdProperty.Diacritic, otherNames, this);
    //        }
    //    }
    //    public enum East_Asian_Width_Values {
    //        Ambiguous("A", "Ambiguous"),
    //        Fullwidth("F", "Fullwidth"),
    //        Halfwidth("H", "Halfwidth"),
    //        Neutral("N", "Neutral"),
    //        Narrow("Na", "Narrow"),
    //        Wide("W", "Wide");
    //        private East_Asian_Width_Values (String...otherNames) {
    //            addNames(UcdProperty.East_Asian_Width, otherNames, this);
    //        }
    //    }
    //    public enum Expands_On_NFC_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Expands_On_NFC_Values (String...otherNames) {
    //            addNames(UcdProperty.Expands_On_NFC, otherNames, this);
    //        }
    //    }
    //    public enum Expands_On_NFD_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Expands_On_NFD_Values (String...otherNames) {
    //            addNames(UcdProperty.Expands_On_NFD, otherNames, this);
    //        }
    //    }
    //    public enum Expands_On_NFKC_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Expands_On_NFKC_Values (String...otherNames) {
    //            addNames(UcdProperty.Expands_On_NFKC, otherNames, this);
    //        }
    //    }
    //    public enum Expands_On_NFKD_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Expands_On_NFKD_Values (String...otherNames) {
    //            addNames(UcdProperty.Expands_On_NFKD, otherNames, this);
    //        }
    //    }
    //    public enum Extender_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Extender_Values (String...otherNames) {
    //            addNames(UcdProperty.Extender, otherNames, this);
    //        }
    //    }
    //    // FC_NFKC_Closure
    //    public enum Full_Composition_Exclusion_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Full_Composition_Exclusion_Values (String...otherNames) {
    //            addNames(UcdProperty.Full_Composition_Exclusion, otherNames, this);
    //        }
    //    }
    //    public enum Grapheme_Base_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Grapheme_Base_Values (String...otherNames) {
    //            addNames(UcdProperty.Grapheme_Base, otherNames, this);
    //        }
    //    }
    //    public enum Grapheme_Cluster_Break_Values {
    //        Control("CN", "Control"),
    //        CR("CR", "CR"),
    //        Extend("EX", "Extend"),
    //        L("L", "L"),
    //        LF("LF", "LF"),
    //        LV("LV", "LV"),
    //        LVT("LVT", "LVT"),
    //        Prepend("PP", "Prepend"),
    //        SpacingMark("SM", "SpacingMark"),
    //        T("T", "T"),
    //        V("V", "V"),
    //        Other("XX", "Other");
    //        private Grapheme_Cluster_Break_Values (String...otherNames) {
    //            addNames(UcdProperty.Grapheme_Cluster_Break, otherNames, this);
    //        }
    //    }
    //    public enum Grapheme_Extend_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Grapheme_Extend_Values (String...otherNames) {
    //            addNames(UcdProperty.Grapheme_Extend, otherNames, this);
    //        }
    //    }
    //    public enum Grapheme_Link_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Grapheme_Link_Values (String...otherNames) {
    //            addNames(UcdProperty.Grapheme_Link, otherNames, this);
    //        }
    //    }
    //    public enum Hangul_Syllable_Type_Values {
    //        Leading_Jamo("L", "Leading_Jamo"),
    //        LV_Syllable("LV", "LV_Syllable"),
    //        LVT_Syllable("LVT", "LVT_Syllable"),
    //        Not_Applicable("NA", "Not_Applicable"),
    //        Trailing_Jamo("T", "Trailing_Jamo"),
    //        Vowel_Jamo("V", "Vowel_Jamo");
    //        private Hangul_Syllable_Type_Values (String...otherNames) {
    //            addNames(UcdProperty.Hangul_Syllable_Type, otherNames, this);
    //        }
    //    }
    //    public enum Hex_Digit_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Hex_Digit_Values (String...otherNames) {
    //            addNames(UcdProperty.Hex_Digit, otherNames, this);
    //        }
    //    }
    //    public enum Hyphen_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Hyphen_Values (String...otherNames) {
    //            addNames(UcdProperty.Hyphen, otherNames, this);
    //        }
    //    }
    //    public enum IDS_Binary_Operator_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private IDS_Binary_Operator_Values (String...otherNames) {
    //            addNames(UcdProperty.IDS_Binary_Operator, otherNames, this);
    //        }
    //    }
    //    public enum IDS_Trinary_Operator_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private IDS_Trinary_Operator_Values (String...otherNames) {
    //            addNames(UcdProperty.IDS_Trinary_Operator, otherNames, this);
    //        }
    //    }
    //    public enum ID_Continue_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private ID_Continue_Values (String...otherNames) {
    //            addNames(UcdProperty.ID_Continue, otherNames, this);
    //        }
    //    }
    //    public enum ID_Start_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private ID_Start_Values (String...otherNames) {
    //            addNames(UcdProperty.ID_Start, otherNames, this);
    //        }
    //    }
    //    // ISO_Comment
    //    public enum Ideographic_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Ideographic_Values (String...otherNames) {
    //            addNames(UcdProperty.Ideographic, otherNames, this);
    //        }
    //    }
    //    // Indic_Matra_Category
    //    // Indic_Syllabic_Category
    //    public enum Jamo_Short_Name_Values {
    //        A("A", "A"),
    //        AE("AE", "AE"),
    //        B("B", "B"),
    //        BB("BB", "BB"),
    //        BS("BS", "BS"),
    //        C("C", "C"),
    //        D("D", "D"),
    //        DD("DD", "DD"),
    //        E("E", "E"),
    //        EO("EO", "EO"),
    //        EU("EU", "EU"),
    //        G("G", "G"),
    //        GG("GG", "GG"),
    //        GS("GS", "GS"),
    //        H("H", "H"),
    //        I("I", "I"),
    //        J("J", "J"),
    //        JJ("JJ", "JJ"),
    //        K("K", "K"),
    //        L("L", "L"),
    //        LB("LB", "LB"),
    //        LG("LG", "LG"),
    //        LH("LH", "LH"),
    //        LM("LM", "LM"),
    //        LP("LP", "LP"),
    //        LS("LS", "LS"),
    //        LT("LT", "LT"),
    //        M("M", "M"),
    //        N("N", "N"),
    //        NG("NG", "NG"),
    //        NH("NH", "NH"),
    //        NJ("NJ", "NJ"),
    //        O("O", "O"),
    //        OE("OE", "OE"),
    //        P("P", "P"),
    //        R("R", "R"),
    //        S("S", "S"),
    //        SS("SS", "SS"),
    //        T("T", "T"),
    //        U("U", "U"),
    //        WA("WA", "WA"),
    //        WAE("WAE", "WAE"),
    //        WE("WE", "WE"),
    //        WEO("WEO", "WEO"),
    //        WI("WI", "WI"),
    //        YA("YA", "YA"),
    //        YAE("YAE", "YAE"),
    //        YE("YE", "YE"),
    //        YEO("YEO", "YEO"),
    //        YI("YI", "YI"),
    //        YO("YO", "YO"),
    //        YU("YU", "YU");
    //        private Jamo_Short_Name_Values (String...otherNames) {
    //            addNames(UcdProperty.Jamo_Short_Name, otherNames, this);
    //        }
    //    }
    //    public enum Join_Control_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Join_Control_Values (String...otherNames) {
    //            addNames(UcdProperty.Join_Control, otherNames, this);
    //        }
    //    }
    //    public enum Joining_Group_Values {
    //        Ain("Ain"),
    //        Alaph("Alaph"),
    //        Alef("Alef"),
    //        Beh("Beh"),
    //        Beth("Beth"),
    //        Burushaski_Yeh_Barree("Burushaski_Yeh_Barree"),
    //        Dal("Dal"),
    //        Dalath_Rish("Dalath_Rish"),
    //        E("E"),
    //        Farsi_Yeh("Farsi_Yeh"),
    //        Fe("Fe"),
    //        Feh("Feh"),
    //        Final_Semkath("Final_Semkath"),
    //        Gaf("Gaf"),
    //        Gamal("Gamal"),
    //        Hah("Hah"),
    //        He("He"),
    //        Heh("Heh"),
    //        Heh_Goal("Heh_Goal"),
    //        Heth("Heth"),
    //        Kaf("Kaf"),
    //        Kaph("Kaph"),
    //        Khaph("Khaph"),
    //        Knotted_Heh("Knotted_Heh"),
    //        Lam("Lam"),
    //        Lamadh("Lamadh"),
    //        Meem("Meem"),
    //        Mim("Mim"),
    //        No_Joining_Group("No_Joining_Group"),
    //        Noon("Noon"),
    //        Nun("Nun"),
    //        Nya("Nya"),
    //        Pe("Pe"),
    //        Qaf("Qaf"),
    //        Qaph("Qaph"),
    //        Reh("Reh"),
    //        Reversed_Pe("Reversed_Pe"),
    //        Rohingya_Yeh("Rohingya_Yeh"),
    //        Sad("Sad"),
    //        Sadhe("Sadhe"),
    //        Seen("Seen"),
    //        Semkath("Semkath"),
    //        Shin("Shin"),
    //        Swash_Kaf("Swash_Kaf"),
    //        Syriac_Waw("Syriac_Waw"),
    //        Tah("Tah"),
    //        Taw("Taw"),
    //        Teh_Marbuta("Teh_Marbuta"),
    //        Teh_Marbuta_Goal("Teh_Marbuta_Goal", "Hamza_On_Heh_Goal"),
    //        Teth("Teth"),
    //        Waw("Waw"),
    //        Yeh("Yeh"),
    //        Yeh_Barree("Yeh_Barree"),
    //        Yeh_With_Tail("Yeh_With_Tail"),
    //        Yudh("Yudh"),
    //        Yudh_He("Yudh_He"),
    //        Zain("Zain"),
    //        Zhain("Zhain");
    //        private Joining_Group_Values (String...otherNames) {
    //            addNames(UcdProperty.Joining_Group, otherNames, this);
    //        }
    //    }
    //    public enum Joining_Type_Values {
    //        Join_Causing("C", "Join_Causing"),
    //        Dual_Joining("D", "Dual_Joining"),
    //        Left_Joining("L", "Left_Joining"),
    //        Right_Joining("R", "Right_Joining"),
    //        Transparent("T", "Transparent"),
    //        Non_Joining("U", "Non_Joining");
    //        private Joining_Type_Values (String...otherNames) {
    //            addNames(UcdProperty.Joining_Type, otherNames, this);
    //        }
    //    }
    //    public enum Line_Break_Values {
    //        Ambiguous("AI", "Ambiguous"),
    //        Alphabetic("AL", "Alphabetic"),
    //        Break_Both("B2", "Break_Both"),
    //        Break_After("BA", "Break_After"),
    //        Break_Before("BB", "Break_Before"),
    //        Mandatory_Break("BK", "Mandatory_Break"),
    //        Contingent_Break("CB", "Contingent_Break"),
    //        Close_Punctuation("CL", "Close_Punctuation"),
    //        Combining_Mark("CM", "Combining_Mark"),
    //        Close_Parenthesis("CP", "Close_Parenthesis"),
    //        Carriage_Return("CR", "Carriage_Return"),
    //        Exclamation("EX", "Exclamation"),
    //        Glue("GL", "Glue"),
    //        H2("H2", "H2"),
    //        H3("H3", "H3"),
    //        Hyphen("HY", "Hyphen"),
    //        Ideographic("ID", "Ideographic"),
    //        Inseparable("IN", "Inseparable", "Inseperable"),
    //        Infix_Numeric("IS", "Infix_Numeric"),
    //        JL("JL", "JL"),
    //        JT("JT", "JT"),
    //        JV("JV", "JV"),
    //        Line_Feed("LF", "Line_Feed"),
    //        Next_Line("NL", "Next_Line"),
    //        Nonstarter("NS", "Nonstarter"),
    //        Numeric("NU", "Numeric"),
    //        Open_Punctuation("OP", "Open_Punctuation"),
    //        Postfix_Numeric("PO", "Postfix_Numeric"),
    //        Prefix_Numeric("PR", "Prefix_Numeric"),
    //        Quotation("QU", "Quotation"),
    //        Complex_Context("SA", "Complex_Context"),
    //        Surrogate("SG", "Surrogate"),
    //        Space("SP", "Space"),
    //        Break_Symbols("SY", "Break_Symbols"),
    //        Word_Joiner("WJ", "Word_Joiner"),
    //        Unknown("XX", "Unknown"),
    //        ZWSpace("ZW", "ZWSpace");
    //        private Line_Break_Values (String...otherNames) {
    //            addNames(UcdProperty.Line_Break, otherNames, this);
    //        }
    //    }
    //    public enum Logical_Order_Exception_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Logical_Order_Exception_Values (String...otherNames) {
    //            addNames(UcdProperty.Logical_Order_Exception, otherNames, this);
    //        }
    //    }
    //    public enum Lowercase_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Lowercase_Values (String...otherNames) {
    //            addNames(UcdProperty.Lowercase, otherNames, this);
    //        }
    //    }
    //    // Lowercase_Mapping
    //    public enum Math_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Math_Values (String...otherNames) {
    //            addNames(UcdProperty.Math, otherNames, this);
    //        }
    //    }
    //    public enum NFC_Quick_Check_Values {
    //        Maybe("M", "Maybe"),
    //        No("N", "No"),
    //        Yes("Y", "Yes");
    //        private NFC_Quick_Check_Values (String...otherNames) {
    //            addNames(UcdProperty.NFC_Quick_Check, otherNames, this);
    //        }
    //    }
    //    public enum NFD_Quick_Check_Values {
    //        No("N", "No"),
    //        Yes("Y", "Yes");
    //        private NFD_Quick_Check_Values (String...otherNames) {
    //            addNames(UcdProperty.NFD_Quick_Check, otherNames, this);
    //        }
    //    }
    //    // NFKC_Casefold
    //    public enum NFKC_Quick_Check_Values {
    //        Maybe("M", "Maybe"),
    //        No("N", "No"),
    //        Yes("Y", "Yes");
    //        private NFKC_Quick_Check_Values (String...otherNames) {
    //            addNames(UcdProperty.NFKC_Quick_Check, otherNames, this);
    //        }
    //    }
    //    public enum NFKD_Quick_Check_Values {
    //        No("N", "No"),
    //        Yes("Y", "Yes");
    //        private NFKD_Quick_Check_Values (String...otherNames) {
    //            addNames(UcdProperty.NFKD_Quick_Check, otherNames, this);
    //        }
    //    }
    //    // Name
    //    // Name_Alias
    //    public enum Noncharacter_Code_Point_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Noncharacter_Code_Point_Values (String...otherNames) {
    //            addNames(UcdProperty.Noncharacter_Code_Point, otherNames, this);
    //        }
    //    }
    //    public enum Numeric_Type_Values {
    //        Decimal("De", "Decimal"),
    //        Digit("Di", "Digit"),
    //        None("None", "None"),
    //        Numeric("Nu", "Numeric");
    //        private Numeric_Type_Values (String...otherNames) {
    //            addNames(UcdProperty.Numeric_Type, otherNames, this);
    //        }
    //    }
    //    // Numeric_Value
    //    public enum Other_Alphabetic_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Other_Alphabetic_Values (String...otherNames) {
    //            addNames(UcdProperty.Other_Alphabetic, otherNames, this);
    //        }
    //    }
    //    public enum Other_Default_Ignorable_Code_Point_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Other_Default_Ignorable_Code_Point_Values (String...otherNames) {
    //            addNames(UcdProperty.Other_Default_Ignorable_Code_Point, otherNames, this);
    //        }
    //    }
    //    public enum Other_Grapheme_Extend_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Other_Grapheme_Extend_Values (String...otherNames) {
    //            addNames(UcdProperty.Other_Grapheme_Extend, otherNames, this);
    //        }
    //    }
    //    public enum Other_ID_Continue_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Other_ID_Continue_Values (String...otherNames) {
    //            addNames(UcdProperty.Other_ID_Continue, otherNames, this);
    //        }
    //    }
    //    public enum Other_ID_Start_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Other_ID_Start_Values (String...otherNames) {
    //            addNames(UcdProperty.Other_ID_Start, otherNames, this);
    //        }
    //    }
    //    public enum Other_Lowercase_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Other_Lowercase_Values (String...otherNames) {
    //            addNames(UcdProperty.Other_Lowercase, otherNames, this);
    //        }
    //    }
    //    public enum Other_Math_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Other_Math_Values (String...otherNames) {
    //            addNames(UcdProperty.Other_Math, otherNames, this);
    //        }
    //    }
    //    public enum Other_Uppercase_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Other_Uppercase_Values (String...otherNames) {
    //            addNames(UcdProperty.Other_Uppercase, otherNames, this);
    //        }
    //    }
    //    public enum Pattern_Syntax_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Pattern_Syntax_Values (String...otherNames) {
    //            addNames(UcdProperty.Pattern_Syntax, otherNames, this);
    //        }
    //    }
    //    public enum Pattern_White_Space_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Pattern_White_Space_Values (String...otherNames) {
    //            addNames(UcdProperty.Pattern_White_Space, otherNames, this);
    //        }
    //    }
    //    public enum Quotation_Mark_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Quotation_Mark_Values (String...otherNames) {
    //            addNames(UcdProperty.Quotation_Mark, otherNames, this);
    //        }
    //    }
    //    public enum Radical_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Radical_Values (String...otherNames) {
    //            addNames(UcdProperty.Radical, otherNames, this);
    //        }
    //    }
    //    public enum STerm_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private STerm_Values (String...otherNames) {
    //            addNames(UcdProperty.STerm, otherNames, this);
    //        }
    //    }
    //    public enum Script_Values {
    //        Arabic("Arab", "Arabic"),
    //        Imperial_Aramaic("Armi", "Imperial_Aramaic"),
    //        Armenian("Armn", "Armenian"),
    //        Avestan("Avst", "Avestan"),
    //        Balinese("Bali", "Balinese"),
    //        Bamum("Bamu", "Bamum"),
    //        Batak("Batk", "Batak"),
    //        Bengali("Beng", "Bengali"),
    //        Bopomofo("Bopo", "Bopomofo"),
    //        Brahmi("Brah", "Brahmi"),
    //        Braille("Brai", "Braille"),
    //        Buginese("Bugi", "Buginese"),
    //        Buhid("Buhd", "Buhid"),
    //        Chakma("Cakm", "Chakma"),
    //        Canadian_Aboriginal("Cans", "Canadian_Aboriginal"),
    //        Carian("Cari", "Carian"),
    //        Cham("Cham", "Cham"),
    //        Cherokee("Cher", "Cherokee"),
    //        Coptic("Copt", "Coptic", "Qaac"),
    //        Cypriot("Cprt", "Cypriot"),
    //        Cyrillic("Cyrl", "Cyrillic"),
    //        Devanagari("Deva", "Devanagari"),
    //        Deseret("Dsrt", "Deseret"),
    //        Egyptian_Hieroglyphs("Egyp", "Egyptian_Hieroglyphs"),
    //        Ethiopic("Ethi", "Ethiopic"),
    //        Georgian("Geor", "Georgian"),
    //        Glagolitic("Glag", "Glagolitic"),
    //        Gothic("Goth", "Gothic"),
    //        Greek("Grek", "Greek"),
    //        Gujarati("Gujr", "Gujarati"),
    //        Gurmukhi("Guru", "Gurmukhi"),
    //        Hangul("Hang", "Hangul"),
    //        Han("Hani", "Han"),
    //        Hanunoo("Hano", "Hanunoo"),
    //        Hebrew("Hebr", "Hebrew"),
    //        Hiragana("Hira", "Hiragana"),
    //        Katakana_Or_Hiragana("Hrkt", "Katakana_Or_Hiragana"),
    //        Old_Italic("Ital", "Old_Italic"),
    //        Javanese("Java", "Javanese"),
    //        Kayah_Li("Kali", "Kayah_Li"),
    //        Katakana("Kana", "Katakana"),
    //        Kharoshthi("Khar", "Kharoshthi"),
    //        Khmer("Khmr", "Khmer"),
    //        Kannada("Knda", "Kannada"),
    //        Kaithi("Kthi", "Kaithi"),
    //        Tai_Tham("Lana", "Tai_Tham"),
    //        Lao("Laoo", "Lao"),
    //        Latin("Latn", "Latin"),
    //        Lepcha("Lepc", "Lepcha"),
    //        Limbu("Limb", "Limbu"),
    //        Linear_B("Linb", "Linear_B"),
    //        Lisu("Lisu", "Lisu"),
    //        Lycian("Lyci", "Lycian"),
    //        Lydian("Lydi", "Lydian"),
    //        Mandaic("Mand", "Mandaic"),
    //        Meroitic_Cursive("Merc", "Meroitic_Cursive"),
    //        Meroitic_Hieroglyphs("Mero", "Meroitic_Hieroglyphs"),
    //        Malayalam("Mlym", "Malayalam"),
    //        Mongolian("Mong", "Mongolian"),
    //        Meetei_Mayek("Mtei", "Meetei_Mayek"),
    //        Myanmar("Mymr", "Myanmar"),
    //        Nko("Nkoo", "Nko"),
    //        Ogham("Ogam", "Ogham"),
    //        Ol_Chiki("Olck", "Ol_Chiki"),
    //        Old_Turkic("Orkh", "Old_Turkic"),
    //        Oriya("Orya", "Oriya"),
    //        Osmanya("Osma", "Osmanya"),
    //        Phags_Pa("Phag", "Phags_Pa"),
    //        Inscriptional_Pahlavi("Phli", "Inscriptional_Pahlavi"),
    //        Phoenician("Phnx", "Phoenician"),
    //        Miao("Plrd", "Miao"),
    //        Inscriptional_Parthian("Prti", "Inscriptional_Parthian"),
    //        Rejang("Rjng", "Rejang"),
    //        Runic("Runr", "Runic"),
    //        Samaritan("Samr", "Samaritan"),
    //        Old_South_Arabian("Sarb", "Old_South_Arabian"),
    //        Saurashtra("Saur", "Saurashtra"),
    //        Shavian("Shaw", "Shavian"),
    //        Sharada("Shrd", "Sharada"),
    //        Sinhala("Sinh", "Sinhala"),
    //        Sora_Sompeng("Sora", "Sora_Sompeng"),
    //        Sundanese("Sund", "Sundanese"),
    //        Syloti_Nagri("Sylo", "Syloti_Nagri"),
    //        Syriac("Syrc", "Syriac"),
    //        Tagbanwa("Tagb", "Tagbanwa"),
    //        Takri("Takr", "Takri"),
    //        Tai_Le("Tale", "Tai_Le"),
    //        New_Tai_Lue("Talu", "New_Tai_Lue"),
    //        Tamil("Taml", "Tamil"),
    //        Tai_Viet("Tavt", "Tai_Viet"),
    //        Telugu("Telu", "Telugu"),
    //        Tifinagh("Tfng", "Tifinagh"),
    //        Tagalog("Tglg", "Tagalog"),
    //        Thaana("Thaa", "Thaana"),
    //        Thai("Thai", "Thai"),
    //        Tibetan("Tibt", "Tibetan"),
    //        Ugaritic("Ugar", "Ugaritic"),
    //        Vai("Vaii", "Vai"),
    //        Old_Persian("Xpeo", "Old_Persian"),
    //        Cuneiform("Xsux", "Cuneiform"),
    //        Yi("Yiii", "Yi"),
    //        Inherited("Zinh", "Inherited", "Qaai"),
    //        Common("Zyyy", "Common"),
    //        Unknown("Zzzz", "Unknown");
    //        private Script_Values (String...otherNames) {
    //            addNames(UcdProperty.Script, otherNames, this);
    //        }
    //    }
    //    public enum Sentence_Break_Values {
    //        ATerm("AT", "ATerm"),
    //        Close("CL", "Close"),
    //        CR("CR", "CR"),
    //        Extend("EX", "Extend"),
    //        Format("FO", "Format"),
    //        OLetter("LE", "OLetter"),
    //        LF("LF", "LF"),
    //        Lower("LO", "Lower"),
    //        Numeric("NU", "Numeric"),
    //        SContinue("SC", "SContinue"),
    //        Sep("SE", "Sep"),
    //        Sp("SP", "Sp"),
    //        STerm("ST", "STerm"),
    //        Upper("UP", "Upper"),
    //        Other("XX", "Other");
    //        private Sentence_Break_Values (String...otherNames) {
    //            addNames(UcdProperty.Sentence_Break, otherNames, this);
    //        }
    //    }
    //    // Simple_Case_Folding
    //    // Simple_Lowercase_Mapping
    //    // Simple_Titlecase_Mapping
    //    // Simple_Uppercase_Mapping
    //    public enum Soft_Dotted_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Soft_Dotted_Values (String...otherNames) {
    //            addNames(UcdProperty.Soft_Dotted, otherNames, this);
    //        }
    //    }
    //    public enum Terminal_Punctuation_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Terminal_Punctuation_Values (String...otherNames) {
    //            addNames(UcdProperty.Terminal_Punctuation, otherNames, this);
    //        }
    //    }
    //    // Titlecase_Mapping
    //    // Unicode_1_Name
    //    public enum Unified_Ideograph_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Unified_Ideograph_Values (String...otherNames) {
    //            addNames(UcdProperty.Unified_Ideograph, otherNames, this);
    //        }
    //    }
    //    public enum Uppercase_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Uppercase_Values (String...otherNames) {
    //            addNames(UcdProperty.Uppercase, otherNames, this);
    //        }
    //    }
    //    // Uppercase_Mapping
    //    public enum Variation_Selector_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private Variation_Selector_Values (String...otherNames) {
    //            addNames(UcdProperty.Variation_Selector, otherNames, this);
    //        }
    //    }
    //    public enum White_Space_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private White_Space_Values (String...otherNames) {
    //            addNames(UcdProperty.White_Space, otherNames, this);
    //        }
    //    }
    //    public enum Word_Break_Values {
    //        CR("CR", "CR"),
    //        ExtendNumLet("EX", "ExtendNumLet"),
    //        Extend("Extend", "Extend"),
    //        Format("FO", "Format"),
    //        Katakana("KA", "Katakana"),
    //        ALetter("LE", "ALetter"),
    //        LF("LF", "LF"),
    //        MidNumLet("MB", "MidNumLet"),
    //        MidLetter("ML", "MidLetter"),
    //        MidNum("MN", "MidNum"),
    //        Newline("NL", "Newline"),
    //        Numeric("NU", "Numeric"),
    //        Other("XX", "Other");
    //        private Word_Break_Values (String...otherNames) {
    //            addNames(UcdProperty.Word_Break, otherNames, this);
    //        }
    //    }
    //    public enum XID_Continue_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private XID_Continue_Values (String...otherNames) {
    //            addNames(UcdProperty.XID_Continue, otherNames, this);
    //        }
    //    }
    //    public enum XID_Start_Values {
    //        No("N", "No", "F", "False"),
    //        Yes("Y", "Yes", "T", "True");
    //        private XID_Start_Values (String...otherNames) {
    //            addNames(UcdProperty.XID_Start, otherNames, this);
    //        }
    //    }
    //    // kAccountingNumeric
    //    // kBigFive
    //    // kCCCII
    //    // kCNS1986
    //    // kCNS1992
    //    // kCangjie
    //    // kCantonese
    //    // kCheungBauer
    //    // kCheungBauerIndex
    //    // kCihaiT
    //    // kCompatibilityVariant
    //    // kCowles
    //    // kDaeJaweon
    //    // kDefinition
    //    // kEACC
    //    // kFenn
    //    // kFennIndex
    //    // kFourCornerCode
    //    // kFrequency
    //    // kGB0
    //    // kGB1
    //    // kGB3
    //    // kGB5
    //    // kGB7
    //    // kGB8
    //    // kGSR
    //    // kGradeLevel
    //    // kHDZRadBreak
    //    // kHKGlyph
    //    // kHKSCS
    //    // kHanYu
    //    // kHangul
    //    // kHanyuPinlu
    //    // kHanyuPinyin
    //    // kIBMJapan
    //    // kIICore
    //    // kIRGDaeJaweon
    //    // kIRGDaiKanwaZiten
    //    // kIRGHanyuDaZidian
    //    // kIRGKangXi
    //    // kIRG_GSource
    //    // kIRG_HSource
    //    // kIRG_JSource
    //    // kIRG_KPSource
    //    // kIRG_KSource
    //    // kIRG_MSource
    //    // kIRG_TSource
    //    // kIRG_USource
    //    // kIRG_VSource
    //    // kJIS0213
    //    // kJapaneseKun
    //    // kJapaneseOn
    //    // kJis0
    //    // kJis1
    //    // kKPS0
    //    // kKPS1
    //    // kKSC0
    //    // kKSC1
    //    // kKangXi
    //    // kKarlgren
    //    // kKorean
    //    // kLau
    //    // kMainlandTelegraph
    //    // kMandarin
    //    // kMatthews
    //    // kMeyerWempe
    //    // kMorohashi
    //    // kNelson
    //    // kOtherNumeric
    //    // kPhonetic
    //    // kPrimaryNumeric
    //    // kPseudoGB1
    //    // kRSAdobe_Japan1_6
    //    // kRSJapanese
    //    // kRSKanWa
    //    // kRSKangXi
    //    // kRSKorean
    //    // kRSUnicode
    //    // kSBGY
    //    // kSemanticVariant
    //    // kSimplifiedVariant
    //    // kSpecializedSemanticVariant
    //    // kTaiwanTelegraph
    //    // kTang
    //    // kTotalStrokes
    //    // kTraditionalVariant
    //    // kVietnamese
    //    // kXHC1983
    //    // kXerox
    //    // kZVariant
    ////    static final int FORCE_LOAD2 = Bidi_Mirrored_Values.values().length;
    ////    static {
    ////        propertyToValues.put(UcdProperty.Age, Arrays.asList(Age_Values.values()));
    ////    }
}
