package org.unicode.text.tools;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.UTF16;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;
import org.unicode.cldr.draft.FileUtilities;
import org.unicode.tools.emoji.ChartUtilities;
import org.unicode.tools.emoji.Emoji;
import org.unicode.tools.emoji.EmojiData;

public class MakeEmojiTable {
    public static void main(String[] args) throws IOException {
        final String outFileName = "emoji-glyphs.html";
        PrintWriter out = FileUtilities.openUTF8Writer(Emoji.TR51_INTERNAL_DIR, outFileName);
        ChartUtilities.writeHeader(
                outFileName,
                out,
                "recommended glyphs",
                null,
                false,
                "<p>" + "" + "</p>\n",
                Emoji.DATA_DIR_PRODUCTION,
                Emoji.TR51_HTML);
        out.println("<table " + "border='1'" + ">");
        boolean first = true;
        boolean firstText = true;
        out.println(
                "<tr><th>Code</th><th>Ref</th><th>Apple</th><th>Andr.</th><th>Name</th><th>Remarks</th></tr>");
        for (String line : FileUtilities.in(MakeEmojiTable.class, "emojiGlyphs.txt")) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            } else if (line.startsWith("U+")) {
                int hex = Integer.parseInt(line.substring(2), 16);
                if (first) {
                    first = false;
                    out.println("</td></tr>");
                }
                String chars = UTF16.valueOf(hex);
                String hexUpper = Utility.hex(hex);
                String hexLower = hexUpper.toLowerCase(Locale.ROOT);
                out.println(
                        "<tr>"
                                + "<td>"
                                + hexUpper
                                + "</td>"
                                + "<td><img class='imgb' alt='"
                                + chars
                                + "' src='images/ref/ref_"
                                + hexLower
                                + ".png'></td>"
                                + "<td><img class='imgb' alt='"
                                + chars
                                + "' src='images/apple/apple_"
                                + hexLower
                                + ".png'></td>"
                                + "<td><img class='imgb' alt='"
                                + chars
                                + "' src='images/android/android_"
                                + hexLower
                                + ".png'></td>"
                                + "<td>"
                                + EmojiData.EMOJI_DATA.getName(chars)
                                + "</td>"
                                + "<td>");
                firstText = true;
            } else {
                if (firstText) {
                    out.println("<div>");
                    firstText = false;
                } else {
                    out.println("<div style='margin-top:6pt'>");
                }
                out.println(line + "</div>");
            }
        }
        out.println("</table>");
        ChartUtilities.writeFooter(out);
        out.close();
        System.out.println("DONE");
    }
}
