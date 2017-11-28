package org.unicode.tools.emoji;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Counter2;
import org.unicode.draft.CharacterFrequency;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.GenerateEmoji.Style;
import org.unicode.tools.emoji.GenerateEmoji.Visibility;

import com.google.common.base.Splitter;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUUncheckedIOException;
import com.ibm.icu.util.ULocale;

public class GenerateEmojiFrequency {
    public static void main(String[] args) {
        Matcher m = Pattern.compile("id=\"score-([A-F0-9]+)\">\\s*(\\d+)\\s*</span>").matcher("");
        EmojiOrder order = EmojiOrder.of(Emoji.VERSION_LAST_RELEASED);
        // <span class="score" id="score-1F602">1872748264</span>
        try (BufferedReader in = FileUtilities.openFile(GenerateEmojiFrequency.class, "emojitracker.txt")) {
            String lastBuffer = "";
            double factor = 0;

            while (true) {
                String line = in.readLine();
                if (line == null) break;
                line = lastBuffer+line;
                m.reset(line);
                int pos = 0;
                
                while (true) {
                    boolean found = m.find(pos);
                    if (!found) break;
                    int cp = Integer.parseInt(m.group(1),16);
                    String str = UTF16.valueOf(cp);
                    String category = order.getCategory(str);
                    long count = Long.parseLong(m.group(2));
                    if (factor == 0) {
                        factor = 1_000_000_000.0/count;
                    }
                    System.out.println(str
                            + "\tU+" + m.group(1)
                            + "\t" + EmojiData.EMOJI_DATA.getName(str)
                            + "\t" + (long)Math.round(count*factor)
                            + "\t" + order.getMajorGroupFromCategory(category).toPlainString()
                            + "\t" + category
                            + "\t" + order.getGroupOrder(category)
                            );
                    pos = m.end();
                }
                lastBuffer = line.substring(pos);
            }
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }
}
