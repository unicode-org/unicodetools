package org.unicode.tools.emoji;

public class GenerateLongAnnotations {
    public static void main(String[] args) {
        EmojiData ed = EmojiData.of(Emoji.VERSION5);
        for (String s : ed.getAllEmojiWithoutDefectivesOrModifiers()) {
            System.out.println(s + "\t" + ed.getName(s));
        }
    }
}
