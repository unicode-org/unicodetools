package org.unicode.tools.emoji;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.unicode.cldr.draft.FileUtilities;
import org.unicode.text.utility.Settings;

public class EmojiLinkAdder {
    static Pattern TO_FIX = Pattern.compile("(\\s*<td>)([0-9A-Fa-f]{4,5})(</td>\\s*)");

    public static void main(String[] args) {
        if (args.length == 0) {
            args =
                    new String[] {
                        Settings.UnicodeTools.UNICODETOOLS_REPO_DIR
                                + "/reports/tr51/pri294-emoji-image-backgroundA.html"
                    };
        }
        Matcher m = TO_FIX.matcher("");

        for (String file : args) {
            for (String line : FileUtilities.in("", file)) {
                if (m.reset(line).matches()) {
                    String hex = m.group(2);
                    String mid =
                            "<a href='https://www.unicode.org/emoji/charts/full-emoji-list.html#"
                                    + hex.toLowerCase(Locale.ROOT)
                                    + "'>"
                                    + hex
                                    + "</a>";
                    line = m.group(1) + mid + m.group(3);
                }
                System.out.println(line);
            }
        }
    }
}
