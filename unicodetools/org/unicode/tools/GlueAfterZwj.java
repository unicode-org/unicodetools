package org.unicode.tools;

import java.util.List;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.EmojiData;

import com.ibm.icu.text.UnicodeSet;

class GlueAfterZwj {
    static final UnicodeSet GLUE_AFTER_ZWJ = new UnicodeSet();
    static final String HEADER;
    
    static {
        StringBuilder header = new StringBuilder();
        boolean inHeader = true;
        for (String line : FileUtilities.in(Settings.DATA_DIR + "cldr/","GlueAfterZwj.txt")) {
            // U+02704  ; Glue_After_Zwj #  ✄   WHITE SCISSORS
            if (line.startsWith("#") || line.isEmpty()) {
                if (inHeader) {
                    header.append(line).append("\n");
                    if (line.startsWith("# DATA")) {
                        inHeader = false;
                    }
                }
                continue;
            }
            List<String> coreList = EmojiData.hashOnly.splitToList(line);
            List<String> list = EmojiData.semi.splitToList(coreList.get(0));
            final String f0 = list.get(0);
            int codePoint, codePointEnd;
            int pos = f0.indexOf("..");
            if (pos < 0) {
                codePoint = codePointEnd = Utility.fromHex(f0).codePointAt(0);
            } else {
                codePoint = Utility.fromHex(f0.substring(0,pos)).codePointAt(0);
                codePointEnd = Utility.fromHex(f0.substring(pos+2)).codePointAt(0);
            }
            GLUE_AFTER_ZWJ.add(codePoint,codePointEnd);
        }
        GLUE_AFTER_ZWJ.freeze();
        HEADER = header.toString();
    }
}