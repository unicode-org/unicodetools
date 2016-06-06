package org.unicode.tools;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Set;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.PropertyValueSets;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Binary;
import org.unicode.props.UcdPropertyValues.Block_Values;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.Emoji;
import org.unicode.tools.emoji.EmojiData;
import org.unicode.tools.emoji.EmojiOrder;

import com.google.common.collect.ImmutableSet;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.VersionInfo;

public class AacOrder {
    static final boolean showName = false;
    static final IndexUnicodeProperties iup = IndexUnicodeProperties.make(Settings.latestVersion);
    static final UnicodeMap<String> names = iup.load(UcdProperty.Name);
    static final UnicodeMap<General_Category_Values> gencat = iup.loadEnum(UcdProperty.General_Category, General_Category_Values.class);
    static final UnicodeSet DI = iup.loadEnum(UcdProperty.Default_Ignorable_Code_Point, Binary.class).getSet(Binary.Yes);
    static final EmojiData emojiData = EmojiData.of(Emoji.VERSION_TO_GENERATE);
    static final UnicodeSet ALLOWED = new UnicodeSet(0,0x10FFFF);
    static {
//      + "[^[:c:][:z:][:di:][࿕-࿘ 卍 卐]]" + emoji_sequences
        for (General_Category_Values v : ImmutableSet.<General_Category_Values>builder()
                .addAll(PropertyValueSets.CONTROL)
                .addAll(PropertyValueSets.SEPARATOR)
                .build()) {
            ALLOWED.removeAll(gencat.getSet(v));
        }
        ALLOWED
        .removeAll(DI)
        .removeAll(new UnicodeSet("[࿕-࿘ 卍 卐]")) // special exceptions
        .addAll(emojiData.getZwjSequencesNormal())
        .addAll(emojiData.getFlagSequences())
        .addAll(emojiData.getModifierSequences())
        .addAll(emojiData.getKeycapSequences())
        .freeze();
    }

    static final Set<String> SORTED_ALL_EMOJI_CHARS_SET
    = EmojiOrder.sort(EmojiOrder.STD_ORDER.codepointCompare, ALLOWED);

    /**
     * First arg is output directory.
     * @param args
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException {
        if (SORTED_ALL_EMOJI_CHARS_SET.size() != ALLOWED.size()) {
            throw new IllegalArgumentException("Bad size");
        }
        String outputDir = args.length == 1 ? args[0] : Settings.UNICODE_DRAFT_DIRECTORY + "consortium/";
        try (PrintWriter out = FileUtilities.openUTF8Writer(outputDir, "aac-order-ranges.txt");
                PrintWriter out2 = FileUtilities.openUTF8Writer(outputDir, "aac-order.txt")
                ) {
            out.println("# Format: codepoint/range/string ; index");
            out2.println("# Format: codepoint/string ; index # char\n"
                    + "# Compute the index while reading the file:\n"
                    + "#  For each single codepoint or string, add one");
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
                    out.println(Utility.hex(first) 
                            + (doRanges ? " ; " + firstIndex : "\t# " + UTF16.valueOf(first))
                            + (showName ? "\t" + UCharacter.getName(first) : ""));
                } else {
                    out.println(Utility.hex(first) + ".." + Utility.hex(last) 
                            + (doRanges ? " ; " + firstIndex : "\t# " + UTF16.valueOf(first) + ".." + UTF16.valueOf(last))
                            + (showName ? "\t" + UCharacter.getName(first) + ".." + UCharacter.getName(last) : ""));
                }
            }
            last = -2;
        }
        public void add(String s) {
            ++currentIndex;
            int current = s.codePointAt(0);
            if (UCharacter.charCount(current) != s.length()) {
                flush();
                out.println(Utility.hex(s) 
                        + (doRanges ? " ; " + currentIndex : "\t# " + s)
                        + (showName ? "\t" + UCharacter.getName(s,"+") : ""));
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
