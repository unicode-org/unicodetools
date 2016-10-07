package org.unicode.tools.emoji;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;
import org.unicode.tools.Tabber;
import org.unicode.tools.emoji.GenerateEmojiKeyboard.Target;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUUncheckedIOException;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

public class GenerateEmojiData {
    private static final String ORDERING_NOTE 
    = "#\n"
            + "# Characters and sequences are listed in code point order. Users should be shown a more natural order.\n"
            + "# See the CLDR collation order for Emoji.\n";

    public static final String SV_WARNING
    = "Three characters used in emoji zwj sequences with the emoji variation selector (VS16 = FE0F) do not yet appear in StandardizedVariants.txt.\n"
            + "Implementations fully supporting these three characters as emoji will need to allow for FE0F, by the following addition to that file:";

    public static final String SV_CHARS
    = "2640 FE0E; text style; # FEMALE SIGN\n"
            + "2640 FE0F; emoji style; # FEMALE SIGN\n"
            + "2642 FE0E; text style;  # MALE SIGN\n"
            + "2642 FE0F; emoji style; # MALE SIGN\n"
            + "2695 FE0E; text style;  # STAFF OF AESCULAPIUS\n"
            + "2695 FE0F; emoji style; # STAFF OF AESCULAPIUS\n";

    private static final boolean DO_TAGS = false;

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
    private static final boolean SHOW = false;

    public static void main(String[] args) throws IOException {
        UnicodeSet bases = new UnicodeSet();
        for (String s : EmojiData.EMOJI_DATA.getZwjSequencesNormal()) {
            if (ZwjType.getType(s) != ZwjType.family && ZwjType.getType(s) != ZwjType.other) {
                bases.add(s.codePointAt(0));
            }
        }
        System.out.println(bases.toPattern(false));
        printData(EmojiData.EMOJI_DATA.getRawNames());
    }

    public enum ZwjType {
        family, role, genderRole, activity, gestures, other, na;
        public static ZwjType getType(String s) {
            if (!s.contains(Emoji.JOINER_STRING)) {
                return na;
            }
            int[] cps = CharSequences.codePoints(s);
            ZwjType zwjType = ZwjType.other;
            if (Emoji.FAMILY_MARKERS.contains(cps[cps.length-1])) {
                zwjType = family;
            } else if (Emoji.ACTIVITY_MARKER.containsSome(s)) {
                zwjType = activity;
            } else if (Emoji.ROLE_MARKER.containsSome(s) || Emoji.FAMILY_MARKERS.containsSome(s)) {
                zwjType = Emoji.GENDER_MARKERS.containsSome(s) ? genderRole: role;
            } else if (Emoji.GENDER_MARKERS.containsSome(s)) {
                zwjType = gestures;
            }
            return zwjType;
        }
    }

    public static <T> void printData(UnicodeMap<String> extraNames) throws IOException {

        PropPrinter printer = new PropPrinter().set(extraNames);

        try (TempPrintWriter outText2 = new TempPrintWriter(Emoji.DATA_DIR, "emoji-data.txt")) {
            UnicodeSet emoji = EmojiData.EMOJI_DATA.getSingletonsWithDefectives();
            UnicodeSet emoji_presentation = EmojiData.EMOJI_DATA.getEmojiPresentationSet();
            UnicodeSet emoji_modifiers = EmojiData.MODIFIERS;
            UnicodeSet emoji_modifier_bases = EmojiData.EMOJI_DATA.getModifierBases();
            outText2.println(Utility.getBaseDataHeader("emoji-data", 51, "Emoji Data", Emoji.VERSION_STRING));
            int width = Math.max("Emoji".length(),
                    Math.max("Emoji_Presentation".length(), 
                            Math.max("Emoji_Modifier".length(),
                                    "Emoji_Modifier_Base".length())));

            outText2.println("# Warning: the format has changed from Version 1.0");
            outText2.println("# Format: ");
            outText2.println("# codepoint(s) ; property(=Yes) # comments ");
            outText2.println(ORDERING_NOTE);
            printer.show(outText2, "Emoji", null, width, 14, emoji, true, true, false );
            printer.show(outText2, "Emoji_Presentation", null, width, 14, emoji_presentation, true, true, false);
            printer.show(outText2, "Emoji_Modifier", null, width, 14, emoji_modifiers, true, true, false);
            printer.show(outText2, "Emoji_Modifier_Base", null, width, 14, emoji_modifier_bases, true, true, false);
            outText2.println("\n#EOF");
        }

        try (Writer out = new TempPrintWriter(Emoji.DATA_DIR, "emoji-sequences.txt")) {
            out.write(Utility.getBaseDataHeader("emoji-sequences", 51, "Emoji Sequence Data", Emoji.VERSION_STRING) + "\n");
            List<String> type_fields = Arrays.asList(
                    "Emoji_Combining_Sequence", 
                    "Emoji_Flag_Sequence", 
                    "Emoji_Modifier_Sequences");
            int width = maxLength(type_fields);
            showTypeFieldsMessage(out, type_fields);

            printer.show(out, "Emoji_Combining_Sequence", null, width, 14, Emoji.KEYCAPS, true, false, true);
            printer.show(out, "Emoji_Flag_Sequence", "This list does not include deprecated or macroregion flags, except for UN and EU.\n"
                    + "# See Annex B of TR51 for more information.", width, 14, EmojiData.EMOJI_DATA.getFlagSequences(), true, false, true);
            printer.show(out, "Emoji_Modifier_Sequence", null, width, 14, EmojiData.EMOJI_DATA.getModifierSequences(), false, false, true);
            out.write("\n#EOF\n");
        }

        try (Writer out = new TempPrintWriter(Emoji.DATA_DIR, "emoji-zwj-sequences.txt")) {
            out.write(Utility.getBaseDataHeader("emoji-zwj-sequences", 51, "Emoji ZWJ Sequence Catalog", Emoji.VERSION_STRING) + "\n");
            List<String> type_fields = Arrays.asList(
                    "Emoji_ZWJ_Sequence");
            int width = maxLength(type_fields);

            showTypeFieldsMessage(out, type_fields);
            UnicodeMap<ZwjType> types = new UnicodeMap<>();
            for (String s : EmojiData.EMOJI_DATA.getZwjSequencesNormal()) {
                ZwjType zwjType = ZwjType.getType(s);
                types.put(s, zwjType);
            }
            printer.show(out, "Emoji_ZWJ_Sequence", "Family", width, 44, types.getSet(ZwjType.family), false, false, true);
            printer.show(out, "Emoji_ZWJ_Sequence", "Gendered Role, with object", width, 44, types.getSet(ZwjType.role), false, false, true);
            printer.show(out, "Emoji_ZWJ_Sequence", "Gendered Role", width, 44, types.getSet(ZwjType.genderRole), false, false, true);
            printer.show(out, "Emoji_ZWJ_Sequence", "Gendered Activity", width, 44, types.getSet(ZwjType.activity), false, false, true);
            printer.show(out, "Emoji_ZWJ_Sequence", "Gendered Gestures", width, 44, types.getSet(ZwjType.gestures), false, false, true);
            printer.show(out, "Emoji_ZWJ_Sequence", "Other", width, 44, types.getSet(ZwjType.other), false, false, true);
            out.write("\n#EOF\n");
        }

        GenerateEmojiKeyboard.showLines(EmojiOrder.STD_ORDER, Target.propFile, Emoji.DATA_DIR);

        if (DO_TAGS) {
            printer.setFlat(true);
            try (PrintWriter out = FileUtilities.openUTF8Writer(Settings.UNICODE_DRAFT_DIRECTORY + "reports/tr52/", "emoji-tags.txt")) {
                out.println(Utility.getBaseDataHeader("emoji-tags", 52, "Emoji Data", Emoji.VERSION_STRING));
                List<String> type_fields = Arrays.asList(
                        "Emoji_Flag_Base", "Emoji_Gender_Base", 
                        "Emoji_Hair_Base", "Emoji_Direction_Base");
                int width = maxLength(type_fields);
                showTypeFieldsMessage(out, type_fields);

                printer.show(out, "Emoji_Flag_Base", null, width, 6, flagBase, false, true, false);
                printer.show(out, "Emoji_Gender_Base", null, width, 6, genderBase, false, true, false);
                printer.show(out, "Emoji_Hair_Base", null, width, 6, hairBase, false, true, false);
                printer.show(out, "Emoji_Direction_Base", null, width, 6, directionBase, false, true, false);
                out.println("\n#EOF");
            }
        }

        if (SHOW) System.out.println("Regional_Indicators ; " + Emoji.REGIONAL_INDICATORS.toPattern(false));
        if (SHOW) System.out.println("Emoji Combining Bases ; " + EmojiData.EMOJI_DATA.getKeycapBases().toPattern(false));
        if (SHOW) System.out.println("Emoji All ; " + EmojiData.EMOJI_DATA.getAllEmojiWithoutDefectives().toPattern(false));
    }

    private static void showTypeFieldsMessage(Writer out, Collection<String> type_fields) throws IOException {
        out.write("# Format:\n");
        out.write("#   code_point(s) ; type_field ; description # comments \n");
        out.write("# Fields:\n");
        out.write("#   code_point(s): one or more code points in hex format, separated by spaces\n");
        out.write("#   type_field: "
                + (type_fields.size() != 1 ? "any of {" + CollectionUtilities.join(type_fields, ", ") + "}"
                        : type_fields.iterator().next())
                        + "\n"
                        + "#     The type_field is a convenience for parsing the emoji sequence files, "
                        + "and is not intended to be maintained as a property.\n");
        out.write("#   description: (optional) short description of sequence.\n");
        out.write(ORDERING_NOTE);
        if (type_fields.contains("Emoji_ZWJ_Sequence")) {
            out.write("#\n"
                    + "# In display and processing, sequences should be supported both with and without FE0F."
                    + "\n# " + SV_WARNING.replace("\n", "\n# ")
                    + "\n#"
                    + "\n# " +  SV_CHARS.replace("\n", "\n# ")
                    );
        }
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

    static final String EXCEPTION_ZWJ = new StringBuilder().appendCodePoint(0x1F441).appendCodePoint(0x200D).appendCodePoint(0x1F5E8).toString();

    static class PropPrinter {
        private UnicodeMap<String> extraNames;
        private boolean flat;

        void show(Writer out, String title, String comments, int maxTitleWidth, int maxCodepointWidth, 
                UnicodeSet emojiChars, boolean addVariants, boolean showMissingLine, boolean showName) {
            try {
                Tabber tabber = new Tabber.MonoTabber()
                .add(maxCodepointWidth, Tabber.LEFT)
                .add(maxTitleWidth + 4, Tabber.LEFT);
                if (showName) {
                    tabber.add(65, Tabber.LEFT);
                }
                tabber.add(2, Tabber.LEFT) // hash
                .add(3, Tabber.RIGHT) // version
                .add(6, Tabber.RIGHT) // count
                .add(10, Tabber.LEFT) // character
                ;

                // # @missing: 0000..10FFFF; Bidi_Mirroring_Glyph; <none>
                // 0009..000D    ; White_Space # Cc   [5] <control-0009>..<control-000D>
                out.write("\n# ================================================\n\n");
                int totalCount = 0;
                if (!showMissingLine) {
                    out.write("# " + title.replace('_', ' '));
                    if (comments != null) {
                        out.write(": " + comments);
                    }
                    out.write("\n\n");
                } else {
                    out.write("# All omitted code points have " + title + "=No \n");
                    out.write("# @missing: 0000..10FFFF  ; " + title + " ; No\n\n");
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
                        if (range.string != null) {
                            throw new IllegalArgumentException("internal error");
                        }
                        for (int cp = range.codepoint; cp <= range.codepointEnd; ++cp) {
                            s = UTF16.valueOf(cp);
                            out.write(tabber.process(
                                    Utility.hex(s) 
                                    + "\t" + titleField 
                                    + (showName ? "\t;" + EmojiData.EMOJI_DATA.getName(s, false) + " " : "")
                                    + "\t#"
                                    + "\t" + range.value.getShortName()
                                    + "\t"
                                    + "\t(" + addEmojiVariant(s, addVariants) + ")"
                                    + (showName ? "" : "\t" + EmojiData.EMOJI_DATA.getName(s, false)))
                                    + "\n");
                        }
                    } else if (rangeCount == 1) {
                        final boolean isException = !s.equals(EXCEPTION_ZWJ);
                        out.write(tabber.process(
                                Utility.hex(addEmojiVariant(s, isException && range.string != null))
                                + "\t" + titleField 
                                + (showName ? "\t; " + EmojiData.EMOJI_DATA.getName(s, false) + " " : "")
                                + "\t#"
                                + "\t" + range.value.getShortName()
                                + "\t[1] "
                                + "\t(" + addEmojiVariant(s, isException && (addVariants || range.string != null)) + ")"
                                + (showName ? "" : "\t" + EmojiData.EMOJI_DATA.getName(s, false)))
                                + "\n");
                    } else  {
                        final String e = UTF16.valueOf(range.codepointEnd);
                        out.write(tabber.process(
                                Utility.hex(range.codepoint) + ".." + Utility.hex(range.codepointEnd)
                                + "\t" + titleField 
                                + (showName ? "\t; " + EmojiData.EMOJI_DATA.getName(s, false) + " " : "")
                                + "\t#"
                                + "\t" + range.value.getShortName()
                                + "\t["+ (range.codepointEnd - range.codepoint + 1) + "] "
                                + "\t(" + addEmojiVariant(s, addVariants) + ".." + addEmojiVariant(e, addVariants) + ")"
                                + (showName ? "" : "\t" + EmojiData.EMOJI_DATA.getName(s, false) + ".." + EmojiData.EMOJI_DATA.getName(e, false)))
                                + "\n");
                    }
                }
                out.write("\n");
                //out.println("# UnicodeSet: " + emojiChars.toPattern(false));
                out.write("# Total elements: " + totalCount
                        + "\n");
                UnicodeSet needsEvs = EmojiData.EMOJI_DATA.getTextPresentationSet();
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
                        if (SHOW) System.out.println(title 
                                + "\t" + Utility.hex(s) 
                                + "\t" + (sMinus.equals(s) ? "≣" : Utility.hex(sMinus))
                                + "\t" + (sPlus.equals(s) ? "≣" : Utility.hex(sPlus))
                                );
                    }
                }
            } catch (IOException e) {
                throw new ICUUncheckedIOException(e);
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

    static String getDate() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd", ULocale.ENGLISH);
        df.setTimeZone(TimeZone.getFrozenTimeZone("GMT"));
        return df.format(new Date());
    }

    static String addEmojiVariant(String s, boolean addVariants) {
        if (!addVariants) {
            return s;
        }
        return EmojiData.EMOJI_DATA.addEmojiVariants(s);
        // hack to add VS to v2.0 to make comparison easier.
        //return Emoji.getEmojiVariant(s, Emoji.EMOJI_VARIANT_STRING, EmojiData.EMOJI_DATA.getDefaultPresentationSet(DefaultPresentation.text));
    }
}
