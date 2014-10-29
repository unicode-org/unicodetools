package org.unicode.text.tools;

import java.io.File;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.With;
import org.unicode.text.utility.Utility;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UnicodeSet;

public class FixEmojiText {
    public static void main(String[] args) {
        for (String arg : args) {
            process(arg);
        }
    }

    private static void process(String arg) {
        StringBuilder result = new StringBuilder();
        System.out.println(arg);
        if (arg.startsWith("[")) {
            Set<String> sorted = new TreeSet<>(EmojiData.EMOJI_COMPARATOR);
            new UnicodeSet(arg).addAllTo(sorted);
            for (String cp : sorted) {
                process2(cp.codePointAt(0), result);
            }
        } else {
            for (int cp : With.codePointArray(arg)) {
                process2(cp, result);
            }
        }
        System.out.println(result);
    }

    private static void process2(int cp, StringBuilder result) {
        if (Emoji.EMOJI_CHARS.contains(cp)) {
            String hex = Utility.hex(cp);
            String fileName = "apple/apple_"
                    + hex.toLowerCase(Locale.ENGLISH)
                    + ".png";
            if (!new File(Emoji.IMAGES_OUTPUT_DIR
                    + fileName).exists()) {
                fileName = "ref/ref_"
                        + hex.toLowerCase(Locale.ENGLISH)
                        + ".png";
            }
            result
            .append("\t\t\t\t\t<img height='24' width='auto' alt='")
            .appendCodePoint(cp)
            .append("' src='images/" + fileName + "'"
                    + " title='U+" + hex
                    + " ")
                    .appendCodePoint(cp)
                    .append(" ")
                    .append(UCharacter.getName(cp).toLowerCase(Locale.ENGLISH))
                    .append("'>\n");
        } else {
            result.appendCodePoint(cp);
        }
    }
}
