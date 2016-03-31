package org.unicode.tools.emoji;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.text.utility.Utility;
import org.unicode.tools.Tabber;
import org.unicode.tools.emoji.EmojiData.DefaultPresentation;
import org.unicode.tools.emoji.EmojiData.VariantHandling;

import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

public class GenerateEmojiData {

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
            printer.show(outText2, "Emoji", width, 14, emoji, true, true );
            printer.show(outText2, "Emoji_Presentation", width, 14, emoji_presentation, true, true);
            printer.show(outText2, "Emoji_Modifier", width, 14, emoji_modifiers, true, true);
            printer.show(outText2, "Emoji_Modifier_Base", width, 14, emoji_modifier_bases, true, true);
            outText2.println("\n#EOF");
        }

        try (PrintWriter out = BagFormatter.openUTF8Writer(Emoji.DATA_DIR, "emoji-sequences.txt")) {
            out.println(getBaseDataHeader(51, "Emoji Sequence Data", "emoji-sequences"));
            List<String> type_fields = Arrays.asList(
                    "Emoji_Combining_Sequence", 
                    "Emoji_Flag_Sequence", 
                    "Emoji_Modifier_Sequences");
            int width = maxLength(type_fields);
            showTypeFieldsMessage(out, type_fields);

            printer.show(out, "Emoji_Combining_Sequence", width, 14, Emoji.KEYCAPS, true, false);
            printer.show(out, "Emoji_Flag_Sequence", width, 14, Emoji.FLAGS, true, false);
            printer.show(out, "Emoji_Modifier_Sequence", width, 14, EmojiData.EMOJI_DATA.getModifierSequences(), false, false);
            out.println("\n#EOF");
        }

        try (PrintWriter out = BagFormatter.openUTF8Writer(Emoji.DATA_DIR, "emoji-zwj-sequences.txt")) {
            out.println(getBaseDataHeader(51, "Emoji ZWJ Sequence Catalog", "emoji-zwj-sequences"));
            List<String> type_fields = Arrays.asList(
                    "Emoji_ZWJ_Sequence");
            int width = maxLength(type_fields);

            showTypeFieldsMessage(out, type_fields);
            printer.show(out, "Emoji_ZWJ_Sequence", width, 44, EmojiData.EMOJI_DATA.getZwjSequencesNormal(), false, false);
            out.println("\n#EOF");
        }

        printer.setFlat(true);
        try (PrintWriter out = BagFormatter.openUTF8Writer(Emoji.DATA_DIR, "emoji-tags.txt")) {
            out.println(getBaseDataHeader(52, "Emoji Data", "emoji-tags"));
            List<String> type_fields = Arrays.asList("Emoji_Flag_Base", "Emoji_Gender_Base", 
                    "Emoji_Hair_Base", "Emoji_Direction_Base");
            int width = maxLength(type_fields);
            showTypeFieldsMessage(out, type_fields);

            printer.show(out, "Emoji_Flag_Base", width, 6, flagBase, false, true);
            printer.show(out, "Emoji_Gender_Base", width, 6, genderBase, false, true);
            printer.show(out, "Emoji_Hair_Base", width, 6, hairBase, false, true);
            printer.show(out, "Emoji_Direction_Base", width, 6, directionBase, false, true);
            out.println("\n#EOF");
        }
        System.out.println("Regional_Indicators ; " + Emoji.REGIONAL_INDICATORS.toPattern(false));
        System.out.println("Emoji Combining Bases ; " + EmojiData.EMOJI_DATA.getKeycapBases().toPattern(false));
        System.out.println("Emoji All ; " + EmojiData.EMOJI_DATA.getAllEmojiWithoutDefectives().toPattern(false));
    }

    private static void showTypeFieldsMessage(PrintWriter out, Collection<String> type_fields) {
        out.println("# Format: ");
        out.println("# code_point(s) ; type_field # version [count] name(s) ");
        out.println("#   code_point(s): one or more code points in hex format, separated by spaces");
                out.println("#   type_field: "
                        + (type_fields.size() != 1 ? "any of {" + CollectionUtilities.join(type_fields, ", ") + "}"
                               : type_fields.iterator().next())
                               + ".\n"
                + "#     The type_field is a convenience for parsing the emoji sequence files, "
                + "and is not intended to be maintained as a property.");
    }

    private static int maxLength(Iterable<String> type_fields) {
        int max = 0;
        for (String item : type_fields) {
            if (item.length() > max) {
                max = item.length();
            }
        }
        return max;
    }

    static class PropPrinter {
        private UnicodeMap<String> extraNames;
        private boolean flat;

        void show(PrintWriter out, String title, int maxTitleWidth, int maxCodepointWidth, UnicodeSet emojiChars, 
                boolean addVariants, boolean showMissingLine) {
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
            if (!showMissingLine) {
                out.println("# " + title.replace('_', ' ') + "\n");
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
                            Utility.hex(addEmojiVariant(s, range.string != null))
                            + "\t" + titleField 
                            + "\t#"
                            + "\t" + range.value.getShortName()
                            + "\t[1] "
                            + "\t(" + addEmojiVariant(s, addVariants || range.string != null) + ")"
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
            out.println("# Total elements: " + totalCount);
            UnicodeSet needsEvs = EmojiData.EMOJI_DATA.getDefaultPresentationSet(DefaultPresentation.text);
            for (String s : emojiChars) {
                String sMinus = s.replace(Emoji.EMOJI_VARIANT_STRING, "");
                final int[] codePoints = CharSequences.codePoints(sMinus);
                if (codePoints.length == 1) {
                    continue;
                }
                StringBuilder b = new StringBuilder();
                for (int i : codePoints) {
                    b.appendCodePoint(i);
                    if (needsEvs.contains(i)) {
                        b.append(Emoji.EMOJI_VARIANT);
                    }
                }
                String sPlus = b.toString();
                if (!sPlus.equals(s) || !sMinus.equals(s)) {
                    System.out.println(title 
                            + "\t" + Utility.hex(s) 
                            + "\t" + (sMinus.equals(s) ? "≣" : Utility.hex(sMinus))
                            + "\t" + (sPlus.equals(s) ? "≣" : Utility.hex(sPlus))
                            );
                }
            }
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

    static String getDate() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd", ULocale.ENGLISH);
        df.setTimeZone(TimeZone.getFrozenTimeZone("GMT"));
        return df.format(new Date());
    }

    static String addEmojiVariant(String s, boolean addVariants) {
        if (!addVariants) {
            return s;
        }
        return EmojiData.EMOJI_DATA.addEmojiVariants(s, Emoji.EMOJI_VARIANT, VariantHandling.sequencesOnly);
        // hack to add VS to v2.0 to make comparison easier.
        //return Emoji.getEmojiVariant(s, Emoji.EMOJI_VARIANT_STRING, EmojiData.EMOJI_DATA.getDefaultPresentationSet(DefaultPresentation.text));
    }
}
