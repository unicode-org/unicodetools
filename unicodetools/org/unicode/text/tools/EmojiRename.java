package org.unicode.text.tools;

import java.io.File;
import java.util.List;

import com.google.common.base.Splitter;
import com.ibm.icu.text.Transform;

public class EmojiRename {
    static final boolean NO_ACTION = false;
    static final File DIR = new File("/Users/markdavis/Google Drive/workspace/DATA/emoji_images_source");
    static final Splitter DOT = Splitter.on('.');
    static final Splitter UNDERBAR = Splitter.on('_');

    public static void main(String[] args) {
        renameCountryFlags(ANDROID_TRANSFORM, "android", "android_large");
    }
    private static void renameCountryFlags(Transform<String,String> transform, String... subdirectories) {
        String base = subdirectories[0];
        for (String subdir : subdirectories) {
            File fileSubdir = new File(DIR,subdir);
            for (File file : fileSubdir.listFiles()) {
                String oldName = file.getName();
                if (oldName.startsWith(base)) {
                    continue; // skip transformed names
                }
                List<String> parts = DOT.splitToList(oldName);
                if (parts.size() != 2) {
                    throw new IllegalArgumentException(parts.toString());
                }
                String prefix = parts.get(0);
                String suffix = parts.get(1);
                String emoji = transform.transform(prefix);
                String newName = base + "_" + Emoji.buildFileName(emoji,"_") + "." + suffix;
                File target = new File(fileSubdir,newName);
                if (NO_ACTION) {
                    System.out.println(file.getName() + "\t=>\t" + target);
                } else {
                    file.renameTo(target);
                }

            }
        }
    }

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
