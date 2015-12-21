package org.unicode.tools;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

import org.unicode.text.tools.Emoji;
import org.unicode.text.tools.EmojiData;
import org.unicode.text.tools.EmojiOrder;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.lang.UCharacter;

public class AacOrder {
    static final boolean showName = false;
    static final EmojiData emojiData = EmojiData.of(Emoji.VERSION_TO_GENERATE);

    static final Set<String> SORTED_ALL_EMOJI_CHARS_SET
    = EmojiOrder.sort(EmojiOrder.STD_ORDER.codepointCompare, AacCheck.ALLOWED);

    /**
     * First arg is output directory.
     * @param args
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException {
        if (SORTED_ALL_EMOJI_CHARS_SET.size() != AacCheck.ALLOWED.size()) {
            throw new IllegalArgumentException("Bad size");
        }
        String outputDir = args.length == 1 ? args[0] : Settings.OTHER_WORKSPACE_DIRECTORY + "Generated/emoji/";
        try (PrintWriter out = BagFormatter.openUTF8Writer(outputDir, "aac-order-ranges.txt");
                PrintWriter out2 = BagFormatter.openUTF8Writer(outputDir, "aac-order.txt")
                ) {
            Range range = new Range(out, true);
            Range rangeNone = new Range(out2, false);
            for (String s : SORTED_ALL_EMOJI_CHARS_SET) {
                range.add(s);
                rangeNone.add(s);
            }
            range.flush();
            rangeNone.flush();
        }
    }

    private static class Range {
        int first;
        int last = -2;
        int firstIndex;
        int currentIndex;
        private final boolean doRanges;
        private final PrintWriter out;

        public Range(PrintWriter out, boolean doRanges) {
            this.out = out;
            this.doRanges = doRanges;
        }
        public void flush() {
            if (last >= 0) {
                if (first == last) {
                    out.println(Utility.hex(first) + " ; " + firstIndex 
                            + (showName ? "\t# " + UCharacter.getName(first) : ""));
                } else {
                    out.println(Utility.hex(first) + ".." + Utility.hex(last) + " ; " + firstIndex 
                            + (showName ? "\t# " + UCharacter.getName(first) + ".." + UCharacter.getName(last) : ""));
                }
            }
            last = -2;
        }
        public void add(String s) {
            ++currentIndex;
            int current = s.codePointAt(0);
            if (UCharacter.charCount(current) != s.length()) {
                flush();
                out.println(Utility.hex(s) + " ; " + currentIndex 
                        + (showName ? "\t# " + UCharacter.getName(s,"+") : ""));
            } else {
                if (doRanges & current == last+1) {
                    last = current;
                } else {
                    flush();
                    first = last = current;
                    firstIndex = currentIndex;
                }
            }
        }
    }
}
