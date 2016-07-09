package org.unicode.tools.emoji;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

import com.google.common.base.Splitter;
import com.ibm.icu.text.Transform;

public class EmojiRename {
    static final boolean NO_ACTION = false;
    static final File DIR = new File(Settings.OTHER_WORKSPACE_DIRECTORY + "DATA/emoji_images_source");
    static final Splitter DOT = Splitter.on('.');
    static final Splitter UNDERBAR = Splitter.on('_');
    static final Splitter DASH = Splitter.on('-');

    public static void main(String[] args) {
//        renameCountryFlags(ANDROID_TRANSFORM, "android", "android_large");
//        renameCountryFlags(TWITTER_TRANSFORM, "twitter");
//        renameCountryFlags(TWITTER_TRANSFORM, "apple", "apple_large");
//        renameCountryFlags(WINDOWS_TRANSFORM, "windows", "windows_large");
        rename("windows10", "glyph-(.*).png", "windows_$1.png");
    }

    private static void rename(String subdir, String sourcePattern, String targetPattern) {
        File fileSubdir = new File(DIR,subdir);
        if (!fileSubdir.exists()) {
            System.out.println("Skipping missing subdirectory: " + fileSubdir);
            return;
        }
        Matcher m = Pattern.compile(sourcePattern).matcher("");
        for (File file : fileSubdir.listFiles()) {
            String name = file.getName();
            if (!m.reset(name).matches()) {
                System.out.println("Mismatch: " + fileSubdir);
                return;
            }
            int cp = Integer.parseInt(m.group(1),16);
            if (!EmojiData.EMOJI_DATA.getChars().contains(cp)) {
                continue;
            }
            String newName = targetPattern.replace("$1", Utility.hex(cp,4).toLowerCase());
            File target = new File(fileSubdir, newName);
            System.out.println(file.getName() + "\t=>\t" + newName);
            file.renameTo(target);
        }        
    }

    private static void renameCountryFlags(Transform<String,String> transform, String... subdirectories) {
        String base = subdirectories[0];
        for (String subdir : subdirectories) {
            File fileSubdir = new File(DIR,subdir);
            if (!fileSubdir.exists()) {
                System.out.println("Skipping missing subdirectory: " + fileSubdir);
                continue;
            }
            for (File file : fileSubdir.listFiles()) {
                String oldName = file.getName();
                if (oldName.startsWith(base) || oldName.startsWith(".")) {
                    continue; // skip transformed names
                }
                List<String> parts = DOT.splitToList(oldName);
                if (parts.size() != 2) {
                    throw new IllegalArgumentException(parts.toString());
                }
                String prefix = parts.get(0);
                String suffix = parts.get(1);
                String emoji = transform.transform(prefix);
                if (emoji == null) {
                    continue;
                }
                String newName = base + "_" + Emoji.buildFileName(emoji,"_") + "." + suffix;
                File target = new File(fileSubdir,newName);
                System.out.println(file.getName() + "\t=>\t" + target);
                if (!NO_ACTION) {
                    file.renameTo(target);
                }

            }
        }
    }



    private static final Transform<String, String> TWITTER_TRANSFORM = new Transform<String, String>() {
        @Override
        public String transform(String prefix) {
            String emoji = null;
            // 1f1e8-1f1f3.png
            StringBuilder b = new StringBuilder();
            for (String hexes : DASH.split(prefix)) {
                b.appendCodePoint(Integer.parseInt(hexes,16));
            }
            emoji = b.toString();

            if (emoji == null) {
                throw new IllegalArgumentException(prefix);
            }
            return emoji;
        }
    };

    private static final Transform<String, String> WINDOWS_TRANSFORM = new Transform<String, String>() {
        static final String WINDOWS_PREFIX = "glyph_0x";
        @Override
        public String transform(String prefix) {
            String emoji = null;
            // glyph_0x1f6a0.png
            if (!prefix.startsWith(WINDOWS_PREFIX)) {
                throw new IllegalArgumentException("«" + prefix + "»");
            }
            StringBuilder b = new StringBuilder();
            String hexes = prefix.substring(WINDOWS_PREFIX.length());
            int cp = Integer.parseInt(hexes,16);
            if (!EmojiData.EMOJI_DATA.getChars().contains(cp)) {
                return null; // don't change
            }
            b.appendCodePoint(cp);
            emoji = b.toString();
            return emoji;
        }
    };

    private static final Transform<String, String> ANDROID_TRANSFORM = new Transform<String, String>() {
        @Override
        public String transform(String prefix) {
            String emoji = null;
            if (prefix.length() == 2) {
                emoji = Emoji.getEmojiFromRegionCode(prefix);
            } else {
                // emoji_u2b55
                // emoji_u1f1e8_1f1f3
                if (prefix.startsWith("emoji_u")) {
                    StringBuilder b = new StringBuilder();
                    for (String hexes : UNDERBAR.split(prefix.substring("emoji_u".length()))) {
                        b.appendCodePoint(Integer.parseInt(hexes,16));
                    }
                    emoji = b.toString();
                }
            }
            if (emoji == null) {
                throw new IllegalArgumentException(prefix);
            }
            return emoji;
        }
    };
}
