package org.unicode.text.UCA;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.ToolUnicodePropertySource;
import org.unicode.text.UCD.UCD;
import org.unicode.text.UCD.UCD_Types;

import com.ibm.icu.text.UnicodeSet;

public class CaseBit {
    public enum Casing {
        UNCASED, LOWER, MIXED, UPPER;
        public Casing composeCasing(Casing other) {
            if (this == other) {
                return this;
            } else if (this == Casing.UNCASED) {
                return other;
            } else if (other == Casing.UNCASED) {
                return this;
            } else {
                return Casing.MIXED;
            }
        }
    }

    public static class CasingList implements Comparable<CasingList> {
        private final Casing[] data;
        CasingList(List<Casing> source) {
            data = source.toArray(new Casing[source.size()]);
        }
        public int compareTo(CasingList other) {
            for (int i = 0; i < data.length && i < other.data.length; ++i) {
                int diff = data[i].compareTo(other.data[i]);
                if (diff != 0) {
                    return diff;
                }
            }
            return data.length - other.data.length;
        }
        public String toString() {
            return Arrays.asList(data).toString();
        }
    }

    static final Casing[] isUpperTertiary = new Casing[32];
    static {
        isUpperTertiary[0x8] = Casing.UPPER;
        isUpperTertiary[0x9] = Casing.UPPER;
        isUpperTertiary[0xa] = Casing.UPPER;
        isUpperTertiary[0xb] = Casing.UPPER;
        isUpperTertiary[0xc] = Casing.UPPER;
        isUpperTertiary[0xd] = Casing.LOWER;
        isUpperTertiary[0xe] = Casing.UPPER;
        isUpperTertiary[0xf] = Casing.LOWER;
        isUpperTertiary[0x10] = Casing.LOWER;
        isUpperTertiary[0x11] = Casing.UPPER;
        isUpperTertiary[0x12] = Casing.UPPER;
        isUpperTertiary[0x1D] = Casing.UPPER;
    }

    public static Casing getCaseFromTertiary(int x) {
        return isUpperTertiary[x] != null ? isUpperTertiary[x] : Casing.UNCASED;
    }

    static ToolUnicodePropertySource propertySource = ToolUnicodePropertySource.make(Default.ucdVersion());
    static UnicodeSet UPPER_EXCEPTIONS = new UnicodeSet("[ᴯᴻ℘℺⅁-⅄あいうえお-ぢつ-もやゆ よ-ろわ-ゔゝゞアイウエオ-ヂツ-モヤユヨ-ロ ワ-ヴヷ-ヺヽヾ𛀀𛀁🅐-🅩🅰-🆏🆑-🆚]").freeze();
    static UnicodeSet LOWER_EXCEPTIONS = new UnicodeSet("[\u0363-\u036F\u1DCA\u1DD3-\u1DDA\u1DDC\u1DDD\u1DE0\u1DE3-\u1DE6 ℩ぁぃぅぇぉっゃゅょゎゕゖァィゥェォッ ャュョヮヵヶㇰ-ㇿ]").freeze();

    // not multithreaded!!
    static List<Casing> data = new ArrayList<Casing>();

    public static CasingList getPropertyCasing(String s, boolean merge) {
        data.clear();
        int cp;
        for (int i = 0; i < s.length(); i += Character.charCount(cp)) {
            cp = s.codePointAt(i);
            Casing propCasing = getPropertyCasingNfd(cp);
            if (merge && propCasing == Casing.LOWER) {
                propCasing = Casing.UNCASED;
            }
            data.add(propCasing);
        }
        return new CasingList(data);
    }

    public static Casing getPropertyCasing(int codePoint) {
        String nfkd = Default.nfkd().normalize(codePoint);
        Casing propertyCasing = Casing.UNCASED;
        int cp;
        for (int i = 0; i < nfkd.length(); i += Character.charCount(cp)) {
            cp = nfkd.codePointAt(i);
            propertyCasing = propertyCasing.composeCasing(getPropertyCasingNfd(cp));
        }
        return propertyCasing;
    }

    static UnicodeSet EXTRA_UPPER = new UnicodeSet("[\\U0001F150-\\U0001F18F\\U0001F191-\\U0001F19A]");

    public static Casing getPropertyCasingNfd(int cp) {
        byte cat = Default.ucd().getCategory(cp);
        // Check the category
        switch(cat) {
        case UCD_Types.Lu:
            return Casing.UPPER;
        case UCD_Types.Ll:
            return Casing.LOWER;
        case UCD_Types.Lt:
            return Casing.MIXED;
        }

        // Check the extended properties
        if (Default.ucd().getBinaryProperty(cp, UCD_Types.Other_Lowercase)) {
            return Casing.LOWER;
        }
        if (Default.ucd().getBinaryProperty(cp, UCD_Types.Other_Uppercase)) {
            return Casing.UPPER;
        }

        // Check the functionally cased
        if (!equals(cp, Default.ucd().getCase(cp, UCD.FULL, UCD.LOWER))) {
            return Casing.UPPER;
        }
        if (!equals(cp, Default.ucd().getCase(cp, UCD.FULL, UCD.UPPER))) {
            return Casing.LOWER;
        }

        // Check for kana (treated as case for collation)
        if (LOWER_EXCEPTIONS.contains(cp)) {
            return Casing.LOWER;
        }
        if (UPPER_EXCEPTIONS.contains(cp)) {
            return Casing.UPPER;
        }

        // through the gauntlet
        return Casing.UNCASED;
    }

    private static boolean equals(int cp, String lower) {
        return !lower.isEmpty() 
        && cp == lower.codePointAt(0) 
        && lower.length() == Character.charCount(cp);
    }

    static Map<Character,Character> bigToSmallKana = new HashMap();
    static Map<Character,Character> smallToBigKana = new HashMap();
    static {
        String[][] bigToSmall = {
                {"あ", "ぁ"},
                {"い", "ぃ"},
                {"う", "ぅ"},
                {"え", "ぇ"},
                {"お", "ぉ"},
                {"つ", "っ"},
                {"や", "ゃ"},
                {"ゆ", "ゅ"},
                {"よ", "ょ"},
                {"わ", "ゎ"},
                {"か", "ゕ"},
                {"け", "ゖ"},
                {"ア", "ァ"},
                {"イ", "ィ"},
                {"ウ", "ゥ"},
                {"エ", "ェ"},
                {"オ", "ォ"},
                {"ツ", "ッ"},
                {"ヤ", "ャ"},
                {"ユ", "ュ"},
                {"ヨ", "ョ"},
                {"ワ", "ヮ"},
                {"カ", "ヵ"},
                {"ケ", "ヶ"},
                {"ク", "ㇰ"},
                {"シ", "ㇱ"},
                {"ス", "ㇲ"},
                {"ト", "ㇳ"},
                {"ヌ", "ㇴ"},
                {"ハ", "ㇵ"},
                {"ヒ", "ㇶ"},
                {"フ", "ㇷ"},
                {"ヘ", "ㇸ"},
                {"ホ", "ㇹ"},
                {"ム", "ㇺ"},
                {"ラ", "ㇻ"},
                {"リ", "ㇼ"},
                {"ル", "ㇽ"},
                {"レ", "ㇾ"},
                {"ロ", "ㇿ"},
                {"ｱ", "ｧ"},
                {"ｲ", "ｨ"},
                {"ｳ", "ｩ"},
                {"ｴ", "ｪ"},
                {"ｵ", "ｫ"},
                {"ﾔ", "ｬ"},
                {"ﾕ", "ｭ"},
                {"ﾖ", "ｮ"},
                {"ﾂ", "ｯ"},
        };
        for (String[] pair : bigToSmall) {
            if (pair[0].length() > 1 || pair[1].length() > 1) {
                throw new IllegalArgumentException("Surrogate; need to rework code");
            }
            bigToSmallKana.put(pair[0].charAt(0), pair[1].charAt(0));
            smallToBigKana.put(pair[1].charAt(0), pair[0].charAt(0));
        }
    }

    private static String toSmallKana(String s) {
        boolean gotOne = false;
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            Character cNew = bigToSmallKana.get(c);
            if (cNew != null) {
                gotOne = true;
                c = cNew;
            }
            result.append(c);
        }
        if (gotOne) {
            return result.toString();
        }
        return s;
    }

    private static String toBigKana(String s) {
        boolean gotOne = false;
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            Character cNew = smallToBigKana.get(c);
            if (cNew != null) {
                gotOne = true;
                c = cNew;
            }
            result.append(c);
        }
        if (gotOne) {
            return result.toString();
        }
        return s;
    }

    //    public static String toSmallKanaOld(String s) {
    //        // note: don't need to do surrogates; none exist
    //        boolean gotOne = false;
    //        StringBuilder toSmallKanaBuffer = new StringBuilder();
    //        for (int i = 0; i < s.length(); ++i) {
    //            char c = s.charAt(i);
    //            if ('\u3042' <= c && c <= '\u30EF') {
    //                switch(c - 0x3000) {
    //                case 0x42: case 0x44: case 0x46: case 0x48: case 0x4A: case 0x64: case 0x84: case 0x86: case 0x8F:
    //                case 0xA2: case 0xA4: case 0xA6: case 0xA8: case 0xAA: case 0xC4: case 0xE4: case 0xE6: case 0xEF:
    //                    --c; // maps to previous char
    //                    gotOne = true;
    //                    break;
    //                case 0xAB:
    //                    c = '\u30F5'; 
    //                    gotOne = true;
    //                    break;
    //                case 0xB1:
    //                    c = '\u30F6'; 
    //                    gotOne = true;
    //                    break;
    //                }
    //            }
    //            toSmallKanaBuffer.append(c);
    //        }
    //        if (gotOne) {
    //            return toSmallKanaBuffer.toString();
    //        }
    //        return s;
    //    }

}
