package org.unicode.text.tools;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.Emoji;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Row;

public class OldEmojiProcessing {

    static final Set<String> ANDROID_IMAGES = new TreeSet<>();
    static final Map<Row.R2<Integer, Integer>,Integer> ANDROID_REMAP = new HashMap<>();
    static final UnicodeMap<String> ANDROID_REMAP_VALUES = new UnicodeMap();
    static {
        addAndroidRemap("🇨🇳", 0xFE4ED); // cn
        addAndroidRemap("🇩🇪", 0xFE4E8); // de
        addAndroidRemap("🇪🇸", 0xFE4ED); // es
        addAndroidRemap("🇫🇷", 0xFE4E7); // fr
        addAndroidRemap("🇬🇧", 0xfe4eA); // gb
        addAndroidRemap("🇮🇹", 0xFE4E9); // it
        addAndroidRemap("🇯🇵", 0xFE4E5); // ja
        addAndroidRemap("🇰🇷", 0xFE4EE); // ko
        addAndroidRemap("🇷🇺", 0xFE4EC); // ru
        addAndroidRemap("🇺🇸", 0xFE4E6); // us
        addAndroidRemap("#⃣", 0xFE82C);
        for (int i = 1; i <= 9; ++i) {
            addAndroidRemap((char)('0' + i) + "" + Emoji.ENCLOSING_KEYCAP, 0xFE82D + i); // 1 => U+FE82E
        }
        addAndroidRemap("0⃣", 0xFE837);
    }

    public static Integer addAndroidRemap(String real, int replacement) {
        ANDROID_REMAP_VALUES.put(replacement, real);
        int first = real.codePointAt(0);
        return ANDROID_REMAP.put(Row.of(first, real.codePointAt(Character.charCount(first))), replacement);
    }
    
    public static String androidPng(int firstCodepoint, int secondCodepoint, boolean first) {
        if (secondCodepoint == Emoji.ENCLOSING_KEYCAP) {
            int debug = 0;
        }
        if (secondCodepoint != 0) {
            Integer remapped = ANDROID_REMAP.get(Row.of(firstCodepoint, secondCodepoint));
            if (remapped != null) {
                if (!first) {
                    return null;
                }
                firstCodepoint = remapped;
            } else {
                return null;
            }
        }
        String filename = "android/emoji_u" + Utility.hex(first ? firstCodepoint : secondCodepoint).toLowerCase(Locale.ENGLISH) + ".png";
        ANDROID_IMAGES.add(filename);
        return filename;
    }
    //Collator.getInstance(ULocale.ENGLISH);

    //    static final Map<String,String> REMAP_FLAGS = new HashMap();
    //    static {
    //        addFlagRemap("BL", "FR");
    //        addFlagRemap("BV", "NO");
    //        addFlagRemap("GF", "FR");
    //        addFlagRemap("HM", "AU");   
    //        addFlagRemap("MF", "FR");
    //        addFlagRemap("RE", "FR");
    //        addFlagRemap("SJ", "NO");
    //        addFlagRemap("TF", "FR");
    //        addFlagRemap("UM", "US");
    //        addFlagRemap("WF", "FR");
    //        addFlagRemap("YT", "FR");
    //    }
    //    public static void addFlagRemap(String originalCountry, String replacementCountry) {
    //        REMAP_FLAGS.put(originalCountry, replacementCountry);
    //        System.out.println(
    //                Emoji.buildFileName(Emoji.getHexFromFlagCode(originalCountry),"_") 
    //                + " => "
    //                + Emoji.buildFileName(Emoji.getHexFromFlagCode(replacementCountry),"_"));
    //    }

    

}
