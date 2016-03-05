package org.unicode.text.tools;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.text.tools.EmojiData.DefaultPresentation;
import org.unicode.text.utility.Utility;
import org.unicode.tools.Tabber;

import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

public class GenerateEmojiData {

    static final boolean TURN_OFF_SEQUENCE_TITLE = true;
    
    // get this from the data file in the future

    public static final UnicodeSet flagBase = new UnicodeSet("[\\U0001F3F3]").freeze();
    public static final UnicodeSet genderBase = new UnicodeSet("[👲 👳  💂  👯  🕴   👱 👮  🕵 💆"
            + " 💇 👰 🙍 🙎 🙅 🙆 💁 🙋 🗣 👤 👥 🙇 🚶 🏃 🚴 🚵 🚣 🛀 🏄 🏊 ⛹ 🏋"
            + " \\U0001F935 \\U0001F926 \\U0001F937 \\U0001F938 \\U0001F93B \\U0001F93C \\U0001F93D \\U0001F93E]").freeze();
    public static final UnicodeSet hairBase = new UnicodeSet("[👶 👮 👲 👳 👸 🕵 👼 💆 💇 👰 🙍 🙎 🙅 🙆 💁 🙋 🙇 "
            + "🚶 🏃 💃 🚣  🏄 🏊 ⛹ 🏋 "
            + "👦 👧  👩 👴 👵  "
            + "\\U0001F935 \\U0001F926 \\U0001F937 \\U0001F938 \\U0001F93B \\U0001F93C \\U0001F93D \\U0001F93E"
            + "\\U0001F934 \\U0001F936 \\U0001F57A \\U0001F930]").freeze();
    public static final UnicodeSet directionBase = new UnicodeSet(
            "[😘 🚶 🏃 ✌ ✋ 👋-👍 👏 💪 👀 🤘 💨 ✈ 🎷 🎺 🔨 ⛏ 🗡 🔫 🚬 "
                    + "\\U0001F93A \\U0001F93D \\U0001F93E \\U0001F946]").freeze();

    public static void main(String[] args) throws IOException {
        printData(null);
    }

    public static <T> void printData(UnicodeMap<String> extraNames) throws IOException {
        
        PropPrinter printer = new PropPrinter().set(extraNames);

        try (PrintWriter outText2 = BagFormatter.openUTF8Writer(Emoji.DATA_DIR, "emoji-data.txt")) {
            UnicodeSet emoji = EmojiData.EMOJI_DATA.getSingletonsWithDefectives();
            UnicodeSet emoji_presentation = new UnicodeSet(EmojiData.EMOJI_DATA.getDefaultPresentationSet(DefaultPresentation.emoji));
            UnicodeSet emoji_modifiers = EmojiData.MODIFIERS;
            UnicodeSet emoji_modifier_bases = EmojiData.EMOJI_DATA.getModifierBases();
            outText2.println(getBaseDataHeader(51, "Emoji Data", "emoji-data"));
            int width = Math.max("Emoji".length(),
                    Math.max("Emoji_Presentation".length(), 
                            Math.max("Emoji_Modifier".length(),
                                    "Emoji_Modifier_Base".length())));

            outText2.println("# Warning: the format has changed from Version 1.0");
            outText2.println("# Format: ");
            outText2.println("# codepoint(s) ; property(=Yes) # version [count] name(s) ");
            printer.show(outText2, "Emoji", width, 14, emoji, true );
            printer.show(outText2, "Emoji_Presentation", width, 14, emoji_presentation, true);
            printer.show(outText2, "Emoji_Modifier", width, 14, emoji_modifiers, true);
            printer.show(outText2, "Emoji_Modifier_Base", width, 14, emoji_modifier_bases, true);
            outText2.println("\n#EOF");
        }

        try (PrintWriter out = BagFormatter.openUTF8Writer(Emoji.DATA_DIR, "emoji-sequences.txt")) {
            out.println(getBaseDataHeader(51, "Emoji Sequence Data", "emoji-sequences"));
            int width = TURN_OFF_SEQUENCE_TITLE ? 0 : Math.max("Combining_Sequences".length(), 
                    Math.max("Flag_Sequences".length(),
                            "Modifier_Sequences".length()));
            out.println("# Format: ");
            out.println(width == 0 ? "# codepoint(s) # version [count] name(s) " : "# codepoint(s) ; property(=Yes) # version [count] name(s) ");

            printer.show(out, "Combining_Sequences", width, 14, Emoji.KEYCAPS, true);
            printer.show(out, "Flag_Sequences", width, 14, Emoji.FLAGS, true);
            printer.show(out, "Modifier_Sequences", width, 14, EmojiData.EMOJI_DATA.getModifierSequences(), false);
            out.println("\n#EOF");
        }

        try (PrintWriter out = BagFormatter.openUTF8Writer(Emoji.DATA_DIR, "emoji-zwj-sequences.txt")) {
            out.println(getBaseDataHeader(51, "Emoji ZWJ Sequence Catalog", "emoji-zwj-sequences"));
            int width = TURN_OFF_SEQUENCE_TITLE ? 0 : "ZWJ_Sequences".length(); 

            out.println("# Format: ");
            out.println(width == 0 ? "# codepoint(s) # version [count] name(s) " : "# codepoint(s) ; property(=Yes) # version [count] name(s) ");
            printer.show(out, "ZWJ_Sequences", width, 44, EmojiData.EMOJI_DATA.getZwjSequencesNormal(), false);
            out.println("\n#EOF");
        }

        printer.setFlat(true);
        try (PrintWriter out = BagFormatter.openUTF8Writer(Emoji.DATA_DIR, "emoji-tags.txt")) {
            out.println(getBaseDataHeader(52, "Emoji Data", "emoji-tags"));
            int width = 
                    Math.max("Emoji_Flag_Base".length(), 
                            Math.max("Emoji_Gender_Base".length(), 
                                    Math.max("Emoji_Hair_Base".length(),"Emoji_Direction_Base".length())));

            out.println("# Format: ");
            out.println("# codepoint(s) ; property(=Yes) # version [count] name(s)");

            printer.show(out, "Emoji_Flag_Base", width, 6, flagBase, false);
            printer.show(out, "Emoji_Gender_Base", width, 6, genderBase, false);
            printer.show(out, "Emoji_Hair_Base", width, 6, hairBase, false);
            printer.show(out, "Emoji_Direction_Base", width, 6, directionBase, false);
            out.println("\n#EOF");
        }
    }

    static class PropPrinter {
        private UnicodeMap<String> extraNames;
        private boolean flat;

        void show(PrintWriter out, String title, int maxTitleWidth, int maxCodepointWidth, UnicodeSet emojiChars, 
                boolean addVariants) {
            Tabber tabber = new Tabber.MonoTabber()
            .add(maxCodepointWidth, Tabber.LEFT)
            .add(maxTitleWidth + 4, Tabber.LEFT)
            .add(2, Tabber.LEFT) // hash
            .add(3, Tabber.RIGHT) // version
            .add(6, Tabber.RIGHT) // count
            .add(10, Tabber.LEFT) // character
            ;

            // # @missing: 0000..10FFFF; Bidi_Mirroring_Glyph; <none>
            // 0009..000D    ; White_Space # Cc   [5] <control-0009>..<control-000D>
            out.println("\n# ================================================\n");
            int totalCount = 0;
            if (maxTitleWidth == 0) {
                out.println("# " + title);
            } else {
                out.println("# All omitted code points have " + title + "=No ");
                out.println("# @missing: 0000..10FFFF  ; " + title + " ; No\n");
            }

            String titleField = maxTitleWidth == 0 ? "" : "; " + title;

            // associated ages (newest for sequence)
            UnicodeMap<Age_Values> emojiCharsWithAge = new UnicodeMap<Age_Values>();
            for (String s : emojiChars) {
                emojiCharsWithAge.put(s, Emoji.getNewest(s));
            }

            for (UnicodeMap.EntryRange<Age_Values> range : emojiCharsWithAge.entryRanges()) {
                String s;
                final int rangeCount;
                if (range.string != null) {
                    s = range.string;
                    rangeCount = 1;
                } else {
                    s = UTF16.valueOf(range.codepoint);
                    rangeCount = range.codepointEnd - range.codepoint + 1;
                }
                totalCount += rangeCount;
                if (flat) {
                    for (int cp = range.codepoint; cp <= range.codepointEnd; ++cp) {
                        s = UTF16.valueOf(cp);
                        out.println(tabber.process(
                                Utility.hex(s) 
                                + "\t" + titleField 
                                + "\t#"
                                + "\t" + range.value.getShortName()
                                + "\t"
                                + "\t(" + addEmojiVariant(s, addVariants) + ")"
                                + "\t" + Emoji.getName(s, false, extraNames)));
                    }
                } else if (rangeCount == 1) {
                    out.println(tabber.process(
                            Utility.hex(s) 
                            + "\t" + titleField 
                            + "\t#"
                            + "\t" + range.value.getShortName()
                            + "\t[1] "
                            + "\t(" + addEmojiVariant(s, addVariants) + ")"
                            + "\t" + Emoji.getName(s, false, extraNames)));
                } else  {
                    final String e = UTF16.valueOf(range.codepointEnd);
                    out.println(tabber.process(
                            Utility.hex(range.codepoint) + ".." + Utility.hex(range.codepointEnd)
                            + "\t" + titleField 
                            + "\t#"
                            + "\t" + range.value.getShortName()
                            + "\t["+ (range.codepointEnd - range.codepoint + 1) + "] "
                            + "\t(" + addEmojiVariant(s, addVariants) + ".." + addEmojiVariant(e, addVariants) + ")"
                            + "\t" + Emoji.getName(s, false, extraNames) + ".." + Emoji.getName(e, false, extraNames)));
                }
            }
            out.println();
            out.println("# UnicodeSet: " + emojiChars.toPattern(false));
            out.println("# Total code points: " + totalCount);
        }

        
        public PropPrinter set(UnicodeMap<String> extraNames) {
            this.extraNames = extraNames;
            return this;
        }
        public PropPrinter setFlat(boolean flat) {
            this.flat = flat;
            return this;
        }
    }
    /**
     * @param trNumber TODO
     * @param title TODO
     * @param filename TODO
     * @return the baseDataHeader
     */
    static String getBaseDataHeader(int trNumber, String title, String filename) {
        return Utility.getDataHeader(filename + ".txt") 
                + "\n#"
                + "\n# " + title + " for UTR #" + trNumber
                + "\n# Version: "  + Emoji.VERSION_STRING
                + "\n#"
                + "\n# For documentation and usage, see http://www.unicode.org/reports/tr" + trNumber
                + "\n#";
    }

//    static String dataHeader() {
//        return getBaseDataHeader(51, "Emoji Data", "emoji-data")
//                + "# Format: Code ; Default_Emoji_Style ; Emoji_Level ; Emoji_Modifier_Status ; Emoji_Sources # Comment\n"
//                + "#\n"
//                + "#   Field 1 — Default_Emoji_Style:\n"
//                + "#             text:      default text presentation\n"
//                + "#             emoji:     default emoji presentation\n"
//                + "#   Field 2 — Emoji_Level:\n"
//                + "#             L1:        level 1 emoji\n"
//                + "#             L2:        level 2 emoji\n"
//                + "#             NA:        not applicable\n"
//                + "#   Field 3 — Emoji_Modifier_Status:\n"
//                + "#             modifier:  an emoji modifier\n"
//                + "#             primary:   a primary emoji modifier base\n"
//                + "#             secondary: a secondary emoji modifier base\n"
//                + "#             none:      not applicable\n"
//                + "#   Field 4 — Emoji_Sources:\n"
//                + "#             one or more values from {z, a, j, w, x}\n"
//                + "#             see the key in http://unicode.org/reports/tr51#Major_Sources\n"
//                + "#             NA:        not applicable\n"
//                + "#   Comment — currently contains the version where the character was first encoded,\n"
//                + "#             followed by:\n"
//                + "#             - a character name in uppercase (for a single character),\n"
//                + "#             - a keycap name,\n"
//                + "#             - an associated flag, where is associated with value unicode region code\n"
//                + "#";
//    }

    static String getDate() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd", ULocale.ENGLISH);
        df.setTimeZone(TimeZone.getFrozenTimeZone("GMT"));
        return df.format(new Date());
    }

    static String addEmojiVariant(String s, boolean addVariants) {
        if (!addVariants) {
            return s;
        }
        // hack to add VS to v2.0 to make comparison easier.
        return Emoji.getEmojiVariant(s, Emoji.EMOJI_VARIANT_STRING, EmojiData.EMOJI_DATA.getDefaultPresentationSet(DefaultPresentation.text));
    }
}
