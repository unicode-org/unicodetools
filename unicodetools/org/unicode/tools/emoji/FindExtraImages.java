package org.unicode.tools.emoji;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.Emoji.Source;

import com.ibm.icu.lang.UCharacter;

public class FindExtraImages {
    public static void main(String[] args) {
        Set<String> emojiFileSuffixes = new LinkedHashSet<>();
        for (String emoji : EmojiData.EMOJI_DATA.getAllEmojiWithoutDefectives()) {
            emojiFileSuffixes.add(Emoji.buildFileName(emoji, "_"));
        }
        //Map<Source,UnicodeSet> extraChars = 
        for (Emoji.Source source : Source.values()) {
            if (source == Source.charOverride || source.compareTo(Source.gmail) >= 0) {
                continue;
            }
            System.out.println(source);
            String dir = source.getImageDirectory();
            Set<String> missing = new LinkedHashSet<>();
            for (File file : new File(dir + "/" + source.getPrefix()).listFiles()) {
                String name = file.getName();
                if (name.startsWith(".")) {
                    continue;
                }
                String otherChars = "";
                String otherName = "";
                if (name.startsWith(source.getPrefix() + "_") && name.endsWith(".png")) {
                    String remainder = name.substring(source.getPrefix().length()+1, name.length()-4);
                    if (emojiFileSuffixes.contains(remainder)) {
                        continue;
                    } else {
                        if (remainder.startsWith("x")) {
                            remainder = remainder.substring(1);
                        }
                        try {
                            otherChars = Utility.fromHex(remainder.replace("_", " "), false, 2);
                            otherName = UCharacter.getName(otherChars, ", ");
                            otherChars = "\t[" + otherChars + "]\t" + otherName;
                        } catch (Exception e) {
                            otherChars = "\t<bad hex>";
                        }
                    }
                }
                missing.add(name);
                System.out.println(source + "\t" +name + otherChars);
            }
        }
    }
}
