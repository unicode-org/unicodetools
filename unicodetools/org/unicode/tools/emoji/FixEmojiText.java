package org.unicode.tools.emoji;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.With;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

public class FixEmojiText {
    static boolean SHOW_NAME = false;

    public static void main(String[] args) {
        if (args.length == 0) {
            SHOW_NAME = true;
            args = new String[] {EmojiData.EMOJI_DATA.getChars().toString()};
        }
        for (String arg : args) {
            process(arg);
        }
    }

    private static void process(String arg) {
        try (
                PrintWriter out = FileUtilities.openUTF8Writer(Settings.OTHER_WORKSPACE_DIRECTORY, "Generated/images/listing.html")
                ) {
            out.println("<html><body><p>");
            StringBuilder result = new StringBuilder();
            System.out.println(arg);
            if (arg.startsWith("[")) {
                Set<String> sorted = new TreeSet<>(GenerateEmoji.EMOJI_COMPARATOR);
                new UnicodeSet(arg).addAllTo(sorted);
                for (String cp : sorted) {
                    process2(cp, result);
                }
            } else {
                for (int cp : With.codePointArray(arg)) {
                    process2(UTF16.valueOf(cp), result);
                }
            }
            out.println(result);
            out.println("</p></body></html>");
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    static final String DATA_SOURCE = Settings.OTHER_WORKSPACE_DIRECTORY + "DATA/emoji_images/";

    private static void process2(String cp, StringBuilder result) {
        if (EmojiData.EMOJI_DATA.getChars().contains(cp)) {
            String hex = Utility.hex(cp);
            String ID = Utility.hex(cp,"_").toLowerCase(Locale.ENGLISH) + ".png";
            String fileName = DATA_SOURCE + "AppleEmoji/apple_" + ID;
            if (!new File(fileName).exists()) {
                fileName = DATA_SOURCE + "country-flags/ref_" + ID;
                if (!new File(fileName).exists()) {
                    fileName = Settings.UNICODE_DRAFT_DIRECTORY + "reports/tr51/images/ref/ref_" + ID;
                }
            }
            String uHex = "U+" + hex;
            if (SHOW_NAME) {
                result.append(uHex + "\t");
            } else {
                result.append("\t\t\t\t\t");
            }
            String name = UCharacter.getName(cp, " + ").toLowerCase(Locale.ENGLISH);
            result
            .append("<img height='24' width='auto' alt='")
            .append(cp)
            .append("' src='" + fileName + "'"
                    + " title='"
                    + uHex
                    + " ")
                    .append(cp)
                    .append(" ")
                    .append(name)
                    .append("'>");
            if (SHOW_NAME) {
                result.append("\t" +name + "<br>");
            }
        } else {
            result.append(cp);
        }
        result.append('\n');
    }
}
