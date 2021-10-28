package org.unicode.text.UCD;
import com.ibm.icu.text.UnicodeSet;

public class ScriptExceptions {

    public static UnicodeSet getExceptions() {
        final UnicodeSet contents = new UnicodeSet();
        //     "FAIL: " => "contents.add(0x"
        //      ";" => ");//"
        //      ".." => ", 0x"



        contents.add(0x005E);//           COMMON     # (Sk) CIRCUMFLEX ACCENT
        contents.add(0x0060);//           COMMON     # (Sk) GRAVE ACCENT
        contents.add(0x00A8);//           COMMON     # (Sk) DIAERESIS
        contents.add(0x00AF);//           COMMON     # (Sk) MACRON
        contents.add(0x00B4);//           COMMON     # (Sk) ACUTE ACCENT
        contents.add(0x00B8);//           COMMON     # (Sk) CEDILLA
        contents.add(0x02B9, 0x02BA);//     COMMON     # (Sk) MODIFIER LETTER PRIME, 0xMODIFIER LETTER DOUBLE PRIME
        contents.add(0x02C2, 0x02CF);//     COMMON     # (Sk) MODIFIER LETTER LEFT ARROWHEAD, 0xMODIFIER LETTER LOW ACUTE ACCENT
        contents.add(0x02D2, 0x02DF);//     COMMON     # (Sk) MODIFIER LETTER CENTRED RIGHT HALF RING, 0xMODIFIER LETTER CROSS ACCENT
        contents.add(0x02E5, 0x02ED);//     COMMON     # (Sk) MODIFIER LETTER EXTRA-HIGH TONE BAR, 0xMODIFIER LETTER UNASPIRATED
        contents.add(0x0374, 0x0375);//     COMMON     # (Sk) GREEK NUMERAL SIGN, 0xGREEK LOWER NUMERAL SIGN
        contents.add(0x0384, 0x0385);//     COMMON     # (Sk) GREEK TONOS, 0xGREEK DIALYTIKA TONOS
        contents.add(0x1FBD);//           COMMON     # (Sk) GREEK KORONIS
        contents.add(0x1FBF, 0x1FC0);//     COMMON     # (Sk) GREEK PSILI, 0xGREEK DIALYTIKA AND PERISPOMENI
        contents.add(0x1FCD, 0x1FCF);//     COMMON     # (Sk) GREEK PSILI AND VARIA, 0xGREEK PSILI AND PERISPOMENI
        contents.add(0x1FDD, 0x1FDF);//     COMMON     # (Sk) GREEK DASIA AND VARIA, 0xGREEK DASIA AND PERISPOMENI
        //contents.add(0x1FED, 0x1FEF);//     COMMON     # (Sk) GREEK DIALYTIKA AND VARIA, 0xGREEK VARIA
        contents.add(0x1FFE, 0x1FFE);//     COMMON     # (Sk) GREEK OXIA, 0xGREEK DASIA
        contents.add(0x309B, 0x309C);//     COMMON     # (Sk) KATAKANA-HIRAGANA VOICED SOUND MARK, 0xKATAKANA-HIRAGANA SEMI-VOICED SOUND MARK
        contents.add(0xFF3E);//           COMMON     # (Sk) FULLWIDTH CIRCUMFLEX ACCENT
        contents.add(0xFF40);//           COMMON     # (Sk) FULLWIDTH GRAVE ACCENT
        contents.add(0xFFE3);//           COMMON     # (Sk) FULLWIDTH MACRON

        contents.add(0x0640);//           COMMON     # (Lm) ARABIC TATWEEL

        contents.add(0x3006);//           COMMON     # (Lo) IDEOGRAPHIC CLOSING MARK
        contents.add(0x303C);//           COMMON     # (Lo) MASU MARK

        contents.add(0x2135, 0x2138);//     COMMON     # (Lo) ALEF SYMBOL..DALET SYMBOL
        contents.add(0x1714);//           TAGALOG    # (Mn) TAGALOG SIGN VIRAMA

        contents.add(0x1734);//           HANUNOO    # (Mn) HANUNOO SIGN PAMUDPOD
        //contents.add(0x0F3E, 0x0F3F);//     COMMON     # (Mc) TIBETAN SIGN YAR TSHES, 0xTIBETAN SIGN MAR TSHES

        contents.add(0x2071);//           COMMON     # (LC) SUPERSCRIPT LATIN SMALL LETTER I
        contents.add(0x2102);//           COMMON     # (LC) DOUBLE-STRUCK CAPITAL C
        contents.add(0x2107);//           COMMON     # (LC) EULER CONSTANT
        contents.add(0x210A, 0x2113);//     COMMON     # (LC) SCRIPT SMALL G, 0xSCRIPT SMALL L
        contents.add(0x2115);//           COMMON     # (LC) DOUBLE-STRUCK CAPITAL N
        contents.add(0x2119, 0x211D);//     COMMON     # (LC) DOUBLE-STRUCK CAPITAL P, 0xDOUBLE-STRUCK CAPITAL R
        contents.add(0x2124);//           COMMON     # (LC) DOUBLE-STRUCK CAPITAL Z
        contents.add(0x2128);//           COMMON     # (LC) BLACK-LETTER CAPITAL Z
        contents.add(0x212C, 0x212D);//     COMMON     # (LC) SCRIPT CAPITAL B, 0xBLACK-LETTER CAPITAL C
        contents.add(0x212F, 0x2131);//     COMMON     # (LC) SCRIPT SMALL E, 0xSCRIPT CAPITAL F
        contents.add(0x2133, 0x2134);//     COMMON     # (LC) SCRIPT CAPITAL M, 0xSCRIPT SMALL O
        contents.add(0x2139);//           COMMON     # (LC) INFORMATION SOURCE
        contents.add(0x213D, 0x213F);//     COMMON     # (LC) DOUBLE-STRUCK SMALL GAMMA, 0xDOUBLE-STRUCK CAPITAL PI
        contents.add(0x2145, 0x2149);//     COMMON     # (LC) DOUBLE-STRUCK ITALIC CAPITAL D, 0xDOUBLE-STRUCK ITALIC SMALL J
        contents.add(0x1D400, 0x1D454);//   COMMON     # (LC) MATHEMATICAL BOLD CAPITAL A, 0xMATHEMATICAL ITALIC SMALL G
        contents.add(0x1D456, 0x1D49C);//   COMMON     # (LC) MATHEMATICAL ITALIC SMALL I, 0xMATHEMATICAL SCRIPT CAPITAL A
        contents.add(0x1D49E, 0x1D49F);//   COMMON     # (LC) MATHEMATICAL SCRIPT CAPITAL C, 0xMATHEMATICAL SCRIPT CAPITAL D
        contents.add(0x1D4A2);//          COMMON     # (LC) MATHEMATICAL SCRIPT CAPITAL G
        contents.add(0x1D4A5, 0x1D4A6);//   COMMON     # (LC) MATHEMATICAL SCRIPT CAPITAL J, 0xMATHEMATICAL SCRIPT CAPITAL K
        contents.add(0x1D4A9, 0x1D4AC);//   COMMON     # (LC) MATHEMATICAL SCRIPT CAPITAL N, 0xMATHEMATICAL SCRIPT CAPITAL Q
        contents.add(0x1D4AE, 0x1D4B9);//   COMMON     # (LC) MATHEMATICAL SCRIPT CAPITAL S, 0xMATHEMATICAL SCRIPT SMALL D
        contents.add(0x1D4BB);//          COMMON     # (LC) MATHEMATICAL SCRIPT SMALL F
        contents.add(0x1D4BD, 0x1D4C0);//   COMMON     # (LC) MATHEMATICAL SCRIPT SMALL H, 0xMATHEMATICAL SCRIPT SMALL K
        contents.add(0x1D4C2, 0x1D4C3);//   COMMON     # (LC) MATHEMATICAL SCRIPT SMALL M, 0xMATHEMATICAL SCRIPT SMALL N
        contents.add(0x1D4C5, 0x1D505);//   COMMON     # (LC) MATHEMATICAL SCRIPT SMALL P, 0xMATHEMATICAL FRAKTUR CAPITAL B
        contents.add(0x1D507, 0x1D50A);//   COMMON     # (LC) MATHEMATICAL FRAKTUR CAPITAL D, 0xMATHEMATICAL FRAKTUR CAPITAL G
        contents.add(0x1D50D, 0x1D514);//   COMMON     # (LC) MATHEMATICAL FRAKTUR CAPITAL J, 0xMATHEMATICAL FRAKTUR CAPITAL Q
        contents.add(0x1D516, 0x1D51C);//   COMMON     # (LC) MATHEMATICAL FRAKTUR CAPITAL S, 0xMATHEMATICAL FRAKTUR CAPITAL Y
        contents.add(0x1D51E, 0x1D539);//   COMMON     # (LC) MATHEMATICAL FRAKTUR SMALL A, 0xMATHEMATICAL DOUBLE-STRUCK CAPITAL B
        contents.add(0x1D53B, 0x1D53E);//   COMMON     # (LC) MATHEMATICAL DOUBLE-STRUCK CAPITAL D, 0xMATHEMATICAL DOUBLE-STRUCK CAPITAL G
        contents.add(0x1D540, 0x1D544);//   COMMON     # (LC) MATHEMATICAL DOUBLE-STRUCK CAPITAL I, 0xMATHEMATICAL DOUBLE-STRUCK CAPITAL M
        contents.add(0x1D546);//          COMMON     # (LC) MATHEMATICAL DOUBLE-STRUCK CAPITAL O
        contents.add(0x1D54A, 0x1D550);//   COMMON     # (LC) MATHEMATICAL DOUBLE-STRUCK CAPITAL S, 0xMATHEMATICAL DOUBLE-STRUCK CAPITAL Y
        contents.add(0x1D552, 0x1D6A3);//   COMMON     # (LC) MATHEMATICAL DOUBLE-STRUCK SMALL A, 0xMATHEMATICAL MONOSPACE SMALL Z
        contents.add(0x1D6A8, 0x1D6C0);//   COMMON     # (LC) MATHEMATICAL BOLD CAPITAL ALPHA, 0xMATHEMATICAL BOLD CAPITAL OMEGA
        contents.add(0x1D6C2, 0x1D6DA);//   COMMON     # (LC) MATHEMATICAL BOLD SMALL ALPHA, 0xMATHEMATICAL BOLD SMALL OMEGA
        contents.add(0x1D6DC, 0x1D6FA);//   COMMON     # (LC) MATHEMATICAL BOLD EPSILON SYMBOL, 0xMATHEMATICAL ITALIC CAPITAL OMEGA
        contents.add(0x1D6FC, 0x1D714);//   COMMON     # (LC) MATHEMATICAL ITALIC SMALL ALPHA, 0xMATHEMATICAL ITALIC SMALL OMEGA
        contents.add(0x1D716, 0x1D734);//   COMMON     # (LC) MATHEMATICAL ITALIC EPSILON SYMBOL, 0xMATHEMATICAL BOLD ITALIC CAPITAL OMEGA
        contents.add(0x1D736, 0x1D74E);//   COMMON     # (LC) MATHEMATICAL BOLD ITALIC SMALL ALPHA, 0xMATHEMATICAL BOLD ITALIC SMALL OMEGA
        contents.add(0x1D750, 0x1D76E);//   COMMON     # (LC) MATHEMATICAL BOLD ITALIC EPSILON SYMBOL, 0xMATHEMATICAL SANS-SERIF BOLD CAPITAL OMEGA
        contents.add(0x1D770, 0x1D788);//   COMMON     # (LC) MATHEMATICAL SANS-SERIF BOLD SMALL ALPHA, 0xMATHEMATICAL SANS-SERIF BOLD SMALL OMEGA
        contents.add(0x1D78A, 0x1D7A8);//   COMMON     # (LC) MATHEMATICAL SANS-SERIF BOLD EPSILON SYMBOL, 0xMATHEMATICAL SANS-SERIF BOLD ITALIC CAPITAL OMEGA
        contents.add(0x1D7AA, 0x1D7C2);//   COMMON     # (LC) MATHEMATICAL SANS-SERIF BOLD ITALIC SMALL ALPHA, 0xMATHEMATICAL SANS-SERIF BOLD IT    ALIC SMALL OMEGA
        contents.add(0x1D7C4, 0x1D7C9);//   COMMON     # (LC) MATHEMATICAL SANS-SERIF BOLD ITALIC EPSILON SYMBOL, 0xMATHEMATICAL SANS-SERIF BOLD    ITALIC PI SYMBOL




        contents.add(0x02BB, 0x02C1);//     COMMON     # (0xLm) MODIFIER LETTER TURNED COMMA, 0xMODIFIER LETTER REVERSED GLOTTAL STOP
        contents.add(0x02D0, 0x02D1);//     COMMON     # (0xLm) MODIFIER LETTER TRIANGULAR COLON, 0xMODIFIER LETTER HALF TRIANGULAR COLON
        contents.add(0x02EE);//           COMMON     # (0xLm) MODIFIER LETTER DOUBLE APOSTROPHE
        contents.add(0x3031, 0x3035);//     COMMON     # (0xLm) VERTICAL KANA REPEAT MARK, 0xVERTICAL KANA REPEAT MARK LOWER HALF
        contents.add(0x30FC);//           COMMON     # (0xLm) KATAKANA-HIRAGANA PROLONGED SOUND MARK
        contents.add(0xFF70);//           COMMON     # (0xLm) HALFWIDTH KATAKANA-HIRAGANA PROLONGED SOUND MARK
        contents.add(0xFF9E, 0xFF9F);//     COMMON     # (0xLm) HALFWIDTH KATAKANA VOICED SOUND MARK, 0xHALFWIDTH KATAKANA SEMI-VOICED SOUND MARK

        contents.add(0x0483, 0x0486);//     CYRILLIC   # (0xMn) COMBINING CYRILLIC TITLO, 0xCOMBINING CYRILLIC PSILI PNEUMATA

        contents.add(0x0711);//           SYRIAC     # (0xMn) SYRIAC LETTER SUPERSCRIPT ALAPH
        contents.add(0x0730, 0x074A);//     SYRIAC     # (0xMn) SYRIAC PTHAHA ABOVE, 0xSYRIAC BARREKH

        contents.add(0x07A6, 0x07B0);//     THAANA     # (0xMn) THAANA ABAFILI, 0xTHAANA SUKUN

        contents.add(0x0901, 0x0902);//     DEVANAGARI # (0xMn) DEVANAGARI SIGN CANDRABINDU, 0xDEVANAGARI SIGN ANUSVARA
        contents.add(0x093C);//           DEVANAGARI # (0xMn) DEVANAGARI SIGN NUKTA
        contents.add(0x0941, 0x0948);//     DEVANAGARI # (0xMn) DEVANAGARI VOWEL SIGN U, 0xDEVANAGARI VOWEL SIGN AI
        contents.add(0x094D);//           DEVANAGARI # (0xMn) DEVANAGARI SIGN VIRAMA
        contents.add(0x0951, 0x0954);//     DEVANAGARI # (0xMn) DEVANAGARI STRESS SIGN UDATTA, 0xDEVANAGARI ACUTE ACCENT
        contents.add(0x0962, 0x0963);//     DEVANAGARI # (0xMn) DEVANAGARI VOWEL SIGN VOCALIC L, 0xDEVANAGARI VOWEL SIGN VOCALIC LL

        contents.add(0x0981);//           BENGALI    # (0xMn) BENGALI SIGN CANDRABINDU
        contents.add(0x09BC);//           BENGALI    # (0xMn) BENGALI SIGN NUKTA
        contents.add(0x09C1, 0x09C4);//     BENGALI    # (0xMn) BENGALI VOWEL SIGN U, 0xBENGALI VOWEL SIGN VOCALIC RR
        contents.add(0x09CD);//           BENGALI    # (0xMn) BENGALI SIGN VIRAMA
        contents.add(0x09E2, 0x09E3);//     BENGALI    # (0xMn) BENGALI VOWEL SIGN VOCALIC L, 0xBENGALI VOWEL SIGN VOCALIC LL

        contents.add(0x0A02);//           GURMUKHI   # (0xMn) GURMUKHI SIGN BINDI
        contents.add(0x0A3C);//           GURMUKHI   # (0xMn) GURMUKHI SIGN NUKTA
        contents.add(0x0A41, 0x0A42);//     GURMUKHI   # (0xMn) GURMUKHI VOWEL SIGN U, 0xGURMUKHI VOWEL SIGN UU
        contents.add(0x0A47, 0x0A48);//     GURMUKHI   # (0xMn) GURMUKHI VOWEL SIGN EE, 0xGURMUKHI VOWEL SIGN AI
        contents.add(0x0A4B, 0x0A4D);//     GURMUKHI   # (0xMn) GURMUKHI VOWEL SIGN OO, 0xGURMUKHI SIGN VIRAMA
        contents.add(0x0A70, 0x0A71);//     GURMUKHI   # (0xMn) GURMUKHI TIPPI, 0xGURMUKHI ADDAK

        contents.add(0x0A81, 0x0A82);//     GUJARATI   # (0xMn) GUJARATI SIGN CANDRABINDU, 0xGUJARATI SIGN ANUSVARA
        contents.add(0x0ABC);//           GUJARATI   # (0xMn) GUJARATI SIGN NUKTA
        contents.add(0x0AC1, 0x0AC5);//     GUJARATI   # (0xMn) GUJARATI VOWEL SIGN U, 0xGUJARATI VOWEL SIGN CANDRA E
        contents.add(0x0AC7, 0x0AC8);//     GUJARATI   # (0xMn) GUJARATI VOWEL SIGN E, 0xGUJARATI VOWEL SIGN AI
        contents.add(0x0ACD);//           GUJARATI   # (0xMn) GUJARATI SIGN VIRAMA

        contents.add(0x0B01);//           ORIYA      # (0xMn) ORIYA SIGN CANDRABINDU
        contents.add(0x0B3C);//           ORIYA      # (0xMn) ORIYA SIGN NUKTA
        contents.add(0x0B3F);//           ORIYA      # (0xMn) ORIYA VOWEL SIGN I
        contents.add(0x0B41, 0x0B43);//     ORIYA      # (0xMn) ORIYA VOWEL SIGN U, 0xORIYA VOWEL SIGN VOCALIC R
        contents.add(0x0B4D);//           ORIYA      # (0xMn) ORIYA SIGN VIRAMA
        contents.add(0x0B56);//           ORIYA      # (0xMn) ORIYA AI LENGTH MARK

        contents.add(0x0B82);//           TAMIL      # (0xMn) TAMIL SIGN ANUSVARA
        contents.add(0x0BC0);//           TAMIL      # (0xMn) TAMIL VOWEL SIGN II
        contents.add(0x0BCD);//           TAMIL      # (0xMn) TAMIL SIGN VIRAMA

        contents.add(0x0C3E, 0x0C40);//     TELUGU     # (0xMn) TELUGU VOWEL SIGN AA, 0xTELUGU VOWEL SIGN II
        contents.add(0x0C46, 0x0C48);//     TELUGU     # (0xMn) TELUGU VOWEL SIGN E, 0xTELUGU VOWEL SIGN AI
        contents.add(0x0C4A, 0x0C4D);//     TELUGU     # (0xMn) TELUGU VOWEL SIGN O, 0xTELUGU SIGN VIRAMA
        contents.add(0x0C55, 0x0C56);//     TELUGU     # (0xMn) TELUGU LENGTH MARK, 0xTELUGU AI LENGTH MARK

        contents.add(0x0CBF);//           KANNADA    # (0xMn) KANNADA VOWEL SIGN I
        contents.add(0x0CC6);//           KANNADA    # (0xMn) KANNADA VOWEL SIGN E
        contents.add(0x0CCC, 0x0CCD);//     KANNADA    # (0xMn) KANNADA VOWEL SIGN AU, 0xKANNADA SIGN VIRAMA

        contents.add(0x0D41, 0x0D43);//     MALAYALAM  # (0xMn) MALAYALAM VOWEL SIGN U, 0xMALAYALAM VOWEL SIGN VOCALIC R
        contents.add(0x0D4D);//           MALAYALAM  # (0xMn) MALAYALAM SIGN VIRAMA

        contents.add(0x0DCA);//           SINHALA    # (0xMn) SINHALA SIGN AL-LAKUNA
        contents.add(0x0DD2, 0x0DD4);//     SINHALA    # (0xMn) SINHALA VOWEL SIGN KETTI IS-PILLA, 0xSINHALA VOWEL SIGN KETTI PAA-PILLA
        contents.add(0x0DD6);//           SINHALA    # (0xMn) SINHALA VOWEL SIGN DIGA PAA-PILLA

        contents.add(0x0E31);//           THAI       # (0xMn) THAI CHARACTER MAI HAN-AKAT
        contents.add(0x0E34, 0x0E3A);//     THAI       # (0xMn) THAI CHARACTER SARA I, 0xTHAI CHARACTER PHINTHU
        contents.add(0x0E47, 0x0E4E);//     THAI       # (0xMn) THAI CHARACTER MAITAIKHU, 0xTHAI CHARACTER YAMAKKAN

        contents.add(0x0EB1);//           LAO        # (0xMn) LAO VOWEL SIGN MAI KAN
        contents.add(0x0EB4, 0x0EB9);//     LAO        # (0xMn) LAO VOWEL SIGN I, 0xLAO VOWEL SIGN UU
        contents.add(0x0EBB, 0x0EBC);//     LAO        # (0xMn) LAO VOWEL SIGN MAI KON, 0xLAO SEMIVOWEL SIGN LO
        contents.add(0x0EC8, 0x0ECD);//     LAO        # (0xMn) LAO TONE MAI EK, 0xLAO NIGGAHITA

        contents.add(0x0F18, 0x0F19);//     TIBETAN    # (0xMn) TIBETAN ASTROLOGICAL SIGN -KHYUD PA, 0xTIBETAN ASTROLOGICAL SIGN SDONG TSHUGS
        contents.add(0x0F35);//           TIBETAN    # (0xMn) TIBETAN MARK NGAS BZUNG NYI ZLA
        contents.add(0x0F37);//           TIBETAN    # (0xMn) TIBETAN MARK NGAS BZUNG SGOR RTAGS
        contents.add(0x0F39);//           TIBETAN    # (0xMn) TIBETAN MARK TSA -PHRU
        contents.add(0x0F71, 0x0F7E);//     TIBETAN    # (0xMn) TIBETAN VOWEL SIGN AA, 0xTIBETAN SIGN RJES SU NGA RO
        contents.add(0x0F80, 0x0F84);//     TIBETAN    # (0xMn) TIBETAN VOWEL SIGN REVERSED I, 0xTIBETAN MARK HALANTA
        contents.add(0x0F86, 0x0F87);//     TIBETAN    # (0xMn) TIBETAN SIGN LCI RTAGS, 0xTIBETAN SIGN YANG RTAGS
        contents.add(0x0F90, 0x0F97);//     TIBETAN    # (0xMn) TIBETAN SUBJOINED LETTER KA, 0xTIBETAN SUBJOINED LETTER JA
        contents.add(0x0F99, 0x0FBC);//     TIBETAN    # (0xMn) TIBETAN SUBJOINED LETTER NYA, 0xTIBETAN SUBJOINED LETTER FIXED-FORM RA
        contents.add(0x0FC6);//           TIBETAN    # (0xMn) TIBETAN SYMBOL PADMA GDAN

        contents.add(0x102D, 0x1030);//     MYANMAR    # (0xMn) MYANMAR VOWEL SIGN I, 0xMYANMAR VOWEL SIGN UU
        contents.add(0x1032);//           MYANMAR    # (0xMn) MYANMAR VOWEL SIGN AI
        contents.add(0x1036, 0x1037);//     MYANMAR    # (0xMn) MYANMAR SIGN ANUSVARA, 0xMYANMAR SIGN DOT BELOW
        contents.add(0x1039);//           MYANMAR    # (0xMn) MYANMAR SIGN VIRAMA
        contents.add(0x1058, 0x1059);//     MYANMAR    # (0xMn) MYANMAR VOWEL SIGN VOCALIC L, 0xMYANMAR VOWEL SIGN VOCALIC LL

        contents.add(0x17B7, 0x17BD);//     KHMER      # (0xMn) KHMER VOWEL SIGN I, 0xKHMER VOWEL SIGN UA
        contents.add(0x17C6);//           KHMER      # (0xMn) KHMER SIGN NIKAHIT
        contents.add(0x17C9, 0x17D3);//     KHMER      # (0xMn) KHMER SIGN MUUSIKATOAN, 0xKHMER SIGN BATHAMASAT

        contents.add(0x18A9);//           MONGOLIAN  # (0xMn) MONGOLIAN LETTER ALI GALI DAGALGA

        contents.add(0x1712, 0x1713);//     TAGALOG    # (0xMn) TAGALOG VOWEL SIGN I, 0xTAGALOG VOWEL SIGN U

        contents.add(0x1732, 0x1733);//     HANUNOO    # (0xMn) HANUNOO VOWEL SIGN I, 0xHANUNOO VOWEL SIGN U

        contents.add(0x1752, 0x1753);//     BUHID      # (0xMn) BUHID VOWEL SIGN I, 0xBUHID VOWEL SIGN U

        contents.add(0x1772, 0x1773);//     TAGBANWA   # (0xMn) TAGBANWA VOWEL SIGN I, 0xTAGBANWA VOWEL SIGN U

        //contents.add(0x1D165, 0x1D166);//   COMMON     # (0xMc) MUSICAL SYMBOL COMBINING STEM, 0xMUSICAL SYMBOL COMBINING SPRECHGESANG STEM
        //contents.add(0x1D16D, 0x1D172);//   COMMON     # (0xMc) MUSICAL SYMBOL COMBINING AUGMENTATION DOT, 0xMUSICAL SYMBOL COMBINING FLAG-5
        contents.add(0x0966, 0x096F);//     DEVANAGARI # (0xNd) DEVANAGARI DIGIT ZERO, 0xDEVANAGARI DIGIT NINE

        contents.add(0x09E6, 0x09EF);//     BENGALI    # (0xNd) BENGALI DIGIT ZERO, 0xBENGALI DIGIT NINE

        contents.add(0x0A66, 0x0A6F);//     GURMUKHI   # (0xNd) GURMUKHI DIGIT ZERO, 0xGURMUKHI DIGIT NINE

        contents.add(0x0AE6, 0x0AEF);//     GUJARATI   # (0xNd) GUJARATI DIGIT ZERO, 0xGUJARATI DIGIT NINE

        contents.add(0x0B66, 0x0B6F);//     ORIYA      # (0xNd) ORIYA DIGIT ZERO, 0xORIYA DIGIT NINE

        contents.add(0x0BE7, 0x0BEF);//     TAMIL      # (0xNd) TAMIL DIGIT ONE, 0xTAMIL DIGIT NINE

        contents.add(0x0C66, 0x0C6F);//     TELUGU     # (0xNd) TELUGU DIGIT ZERO, 0xTELUGU DIGIT NINE

        contents.add(0x0CE6, 0x0CEF);//     KANNADA    # (0xNd) KANNADA DIGIT ZERO, 0xKANNADA DIGIT NINE

        contents.add(0x0D66, 0x0D6F);//     MALAYALAM  # (0xNd) MALAYALAM DIGIT ZERO, 0xMALAYALAM DIGIT NINE

        contents.add(0x0E50, 0x0E59);//     THAI       # (0xNd) THAI DIGIT ZERO, 0xTHAI DIGIT NINE

        contents.add(0x0ED0, 0x0ED9);//     LAO        # (0xNd) LAO DIGIT ZERO, 0xLAO DIGIT NINE

        contents.add(0x0F20, 0x0F29);//     TIBETAN    # (0xNd) TIBETAN DIGIT ZERO, 0xTIBETAN DIGIT NINE

        contents.add(0x1040, 0x1049);//     MYANMAR    # (0xNd) MYANMAR DIGIT ZERO, 0xMYANMAR DIGIT NINE

        contents.add(0x1369, 0x1371);//     ETHIOPIC   # (0xNd) ETHIOPIC DIGIT ONE, 0xETHIOPIC DIGIT NINE

        contents.add(0x17E0, 0x17E9);//     KHMER      # (0xNd) KHMER DIGIT ZERO, 0xKHMER DIGIT NINE

        contents.add(0x1810, 0x1819);//     MONGOLIAN  # (0xNd) MONGOLIAN DIGIT ZERO, 0xMONGOLIAN DIGIT NINE

        contents.add(0x16EE, 0x16F0);//     RUNIC      # (0xNl) RUNIC ARLAUG SYMBOL, 0xRUNIC BELGTHOR SYMBOL

        contents.add(0x3007);//           HAN        # (0xNl) IDEOGRAPHIC NUMBER ZERO
        contents.add(0x3021, 0x3029);//     HAN        # (0xNl) HANGZHOU NUMERAL ONE, 0xHANGZHOU NUMERAL NINE
        contents.add(0x3038, 0x303A);//     HAN        # (0xNl) HANGZHOU NUMERAL TEN, 0xHANGZHOU NUMERAL THIRTY

        contents.add(0x1034A);//          GOTHIC     # (0xNl) GOTHIC LETTER NINE HUNDRED

        contents.add(0x0BF0, 0x0BF2);//     TAMIL      # (0xNo) TAMIL NUMBER TEN, 0xTAMIL NUMBER ONE THOUSAND

        contents.add(0x0F2A, 0x0F33);//     TIBETAN    # (0xNo) TIBETAN DIGIT HALF ONE, 0xTIBETAN DIGIT HALF ZERO

        contents.add(0x1372, 0x137C);//     ETHIOPIC   # (0xNo) ETHIOPIC NUMBER TEN, 0xETHIOPIC NUMBER TEN THOUSAND

        contents.add(0x2E80, 0x2E99);//     HAN        # (0xSo) CJK RADICAL REPEAT, 0xCJK RADICAL RAP
        contents.add(0x2E9B, 0x2EF3);//     HAN        # (0xSo) CJK RADICAL CHOKE, 0xCJK RADICAL C-SIMPLIFIED TURTLE
        contents.add(0x2F00, 0x2FD5);//     HAN        # (0xSo) KANGXI RADICAL ONE, 0xKANGXI RADICAL FLUTE

        contents.add(0xA490, 0xA4A1);//     YI         # (0xSo) YI RADICAL QOT, 0xYI RADICAL GA
        contents.add(0xA4A4, 0xA4B3);//     YI         # (0xSo) YI RADICAL DDUR, 0xYI RADICAL JO
        contents.add(0xA4B5, 0xA4C0);//     YI         # (0xSo) YI RADICAL JJY, 0xYI RADICAL SHAT
        contents.add(0xA4C2, 0xA4C4);//     YI         # (0xSo) YI RADICAL SHOP, 0xYI RADICAL ZZIET
        contents.add(0xA4C6);//           YI         # (0xSo) YI RADICAL KE
        return contents;
    }
}