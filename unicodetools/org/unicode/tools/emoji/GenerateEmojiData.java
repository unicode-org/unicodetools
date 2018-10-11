package org.unicode.tools.emoji;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.Tabber;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.GenerateEmojiKeyboard.Target;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.EntryRange;
import com.ibm.icu.util.ICUUncheckedIOException;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.VersionInfo;

public class GenerateEmojiData {
    public static final String OUTPUT_DIR = Settings.UNICODE_DRAFT_DIRECTORY + "Public/emoji/" + Emoji.VERSION_BETA_STRING;

    private static final String ORDERING_NOTE = "#\n"
            + "# Characters and sequences are listed in code point order. Users should be shown a more natural order.\n"
            + "# See the CLDR collation order for Emoji.\n";

    //	public static final String SV_WARNING = "Three characters used in emoji zwj sequences with the emoji presentation selector (VS16 = FE0F) do not yet appear in StandardizedVariants.txt.\n"
    //			+ "Implementations fully supporting these three characters as emoji will need to allow for FE0F, by the following addition to that file:";
    //
    //	public static final String SV_CHARS = "2640 FE0E; text style; # FEMALE SIGN\n"
    //			+ "2640 FE0F; emoji style; # FEMALE SIGN\n" + "2642 FE0E; text style;  # MALE SIGN\n"
    //			+ "2642 FE0F; emoji style; # MALE SIGN\n" + "2695 FE0E; text style;  # STAFF OF AESCULAPIUS\n"
    //			+ "2695 FE0F; emoji style; # STAFF OF AESCULAPIUS\n";

    private static final boolean DO_TAGS = false;

    // get this from the data file in the future

    public static final UnicodeSet flagBase = new UnicodeSet("[\\U0001F3F3]").freeze();
    public static final UnicodeSet genderBase = new UnicodeSet(
            "[👲 👳  💂  👯  🕴   👱 👮  🕵 💆" + " 💇 👰 🙍 🙎 🙅 🙆 💁 🙋 🗣 👤 👥 🙇 🚶 🏃 🚴 🚵 🚣 🛀 🏄 🏊 ⛹ 🏋"
                    + " \\U0001F935 \\U0001F926 \\U0001F937 \\U0001F938 \\U0001F93B \\U0001F93C \\U0001F93D \\U0001F93E]")
            .freeze();

    public static final String EXPLICIT_GENDER_LIST = new StringBuffer()
            .appendCodePoint(0x1F466)
            .appendCodePoint(0x1F467)
            .appendCodePoint(0x1F468)
            .appendCodePoint(0x1F469)
            .appendCodePoint(0x1F474)
            .appendCodePoint(0x1F475)


            .appendCodePoint(0x1F46B)
            .appendCodePoint(0x1F46C)
            .appendCodePoint(0x1F46D)


            .appendCodePoint(0x1F385)
            .appendCodePoint(0x1F936)
            .appendCodePoint(0x1F478)
            .appendCodePoint(0x1F934)
            .appendCodePoint(0x1F483)
            .appendCodePoint(0x1F57A)

            .appendCodePoint(0x1F470)
            .appendCodePoint(0x1F935)

            .appendCodePoint(0x1F930)
            .appendCodePoint(0x1F931)
            .appendCodePoint(0x1F9D5)


            .appendCodePoint(0x1F574)
            .appendCodePoint(0x1F472)
            .toString();

    public static final UnicodeSet hairBase = new UnicodeSet(
            "[👶 👮 👲 👳 👸 🕵 👼 💆 💇 👰 🙍 🙎 🙅 🙆 💁 🙋 🙇 " + "🚶 🏃 💃 🚣  🏄 🏊 ⛹ 🏋 " + "👦 👧  👩 👴 👵  "
                    + "\\U0001F935 \\U0001F926 \\U0001F937 \\U0001F938 \\U0001F93B \\U0001F93C \\U0001F93D \\U0001F93E"
                    + "\\U0001F934 \\U0001F936 \\U0001F57A \\U0001F930]").freeze();
    public static final UnicodeSet directionBase = new UnicodeSet(
            "[😘 🚶 🏃 ✌ ✋ 👋-👍 👏 💪 👀 🤘 💨 ✈ 🎷 🎺 🔨 ⛏ 🗡 🔫 🚬 "
                    + "\\U0001F93A \\U0001F93D \\U0001F93E \\U0001F946]").freeze();
    private static final boolean SHOW = false;

    /**@deprecated Replace by the CountEmoji.Category*/
    public enum ZwjType {
        roleWithHair, roleWithObject, roleWithSign, gestures, activity, family, other, na;
        public static ZwjType getType(String s) {
            if (!s.contains(Emoji.JOINER_STRING)) {
                return na;
            }
            int[] cps = CharSequences.codePoints(s);
            ZwjType zwjType = ZwjType.other;
            if (Emoji.HAIR_PIECES.containsSome(s)) {
                zwjType = roleWithHair;
            } else if (Emoji.FAMILY_MARKERS.contains(cps[cps.length - 1])) { // last character is in boy..woman
                zwjType = family;
            } else if (Emoji.ACTIVITY_MARKER.containsSome(s)) {
                zwjType = activity;
            } else if (Emoji.ROLE_MARKER.containsSome(s)) { //  || Emoji.FAMILY_MARKERS.containsSome(s)
                zwjType = Emoji.GENDER_MARKERS.containsSome(s) ? roleWithSign : roleWithObject;
            } else if (Emoji.GENDER_MARKERS.containsSome(s)) {
                zwjType = gestures;
            }
            return zwjType;
        }
    }

    public static <T> void printData(UnicodeMap<String> extraNames) throws IOException {

        PropPrinter printer = new PropPrinter().set(extraNames);
        
        try (TempPrintWriter outText2 = new TempPrintWriter(OUTPUT_DIR, "internal/emoji-internal.txt")) {
            UnicodeSet emojiGenderBase = EmojiDataSourceCombined.EMOJI_DATA.getGenderBases();
            UnicodeSet emojiExplicitGender = EmojiDataSourceCombined.EMOJI_DATA.getExplicitGender();
            UnicodeSet emojiMultiPersonGroupings = EmojiDataSourceCombined.EMOJI_DATA.getMultiPersonGroupings();
            outText2.println(Utility.getBaseDataHeader("emoji-internal", 51, "Emoji Data Internal", Emoji.VERSION_STRING));
            
            
            int width = maxLength("Emoji_Gender_Base", 
                    "Emoji_Explicit_Gender",
                    "Multi_Person_Groupings"
                    );

            //            outText2.println("# Warning: the format has changed from Version 1.0");
            outText2.println("# Format: ");
            outText2.println("# <codepoint(s)> ; <property> # <comments> ");
            outText2.println("# Note: there is no guarantee as to the structure of whitespace or comments");
            outText2.println(ORDERING_NOTE);
            printer.show(outText2, "Emoji_Gender_Base", null, width, 14, emojiGenderBase, true, true, false);
            printer.show(outText2, "Emoji_Explicit_Gender", null, width, 14, emojiExplicitGender, true, true, false);
            printer.show(outText2, "Multi_Person_Groupings", null, width, 14, emojiMultiPersonGroupings, true, true, false);
            outText2.println("\n#EOF");
        }

        try (TempPrintWriter outText2 = new TempPrintWriter(OUTPUT_DIR, "emoji-data.txt")) {
            UnicodeSet emoji = EmojiDataSourceCombined.EMOJI_DATA.getSingletonsWithDefectives();
            UnicodeSet emoji_presentation = EmojiDataSourceCombined.EMOJI_DATA.getEmojiPresentationSet();
            UnicodeSet emoji_modifiers = EmojiData.MODIFIERS;
            UnicodeSet emoji_modifier_bases = EmojiDataSourceCombined.EMOJI_DATA.getModifierBases();
            //UnicodeSet emoji_regional_indicators = EmojiDataSourceCombined.EMOJI_DATA.getRegionalIndicators();
            UnicodeSet emoji_components = EmojiDataSourceCombined.EMOJI_DATA.getEmojiComponents();
            UnicodeSet emoji_pict = EmojiDataSourceCombined.EMOJI_DATA.getExtendedPictographic();
            outText2.println(Utility.getBaseDataHeader("emoji-data", 51, "Emoji Data", Emoji.VERSION_STRING));
            int width = Math.max("Emoji".length(),
                    Math.max("Emoji_Presentation".length(),
                            Math.max("Emoji_Modifier".length(),
                                    // Math.max("Emoji_Regional_Indicator".length(),
                                    Math.max("Emoji_Component".length(), "Emoji_Modifier_Base".length()))));

            //            outText2.println("# Warning: the format has changed from Version 1.0");
            outText2.println("# Format: ");
            outText2.println("# <codepoint(s)> ; <property> # <comments> ");
            outText2.println("# Note: there is no guarantee as to the structure of whitespace or comments");
            outText2.println(ORDERING_NOTE);
            printer.show(outText2, "Emoji", null, width, 14, emoji, true, true, false);
            printer.show(outText2, "Emoji_Presentation", null, width, 14, emoji_presentation, true, true, false);
            printer.show(outText2, "Emoji_Modifier", null, width, 14, emoji_modifiers, true, true, false);
            printer.show(outText2, "Emoji_Modifier_Base", null, width, 14, emoji_modifier_bases, true, true, false);
            //			printer.show(outText2, "Emoji_Regional_Indicator", null, width, 14, emoji_regional_indicators, true, true,
            //					false);
            printer.show(outText2, "Emoji_Component", null, width, 14, emoji_components, true, true, false);
            if (Emoji.VERSION5.compareTo(EmojiData.EMOJI_DATA.getVersion()) < 0) {
                printer.show(outText2, "Extended_Pictographic", null, width, 14, emoji_pict, true, true, false);
            }
            outText2.println("\n#EOF");
        }

        //        if (EmojiDataSourceCombined.EMOJI_DATA.getVersion().compareTo(Emoji.VERSION6) >= 0) {
        //            try (TempPrintWriter outText2 = new TempPrintWriter(OUTPUT_DIR, "emoji-extended-data.txt")) {
        //                UnicodeSet emoji_pict = EmojiDataSourceCombined.EMOJI_DATA.getExtendedPictographic();
        //                outText2.println(Utility.getBaseDataHeader("emoji-extended-data", 51, "Emoji Data", Emoji.VERSION_STRING));
        //                int width = "Extended_Pictographic".length();
        //
        //                outText2.println("# Format: ");
        //                outText2.println("# codepoint(s) ; property(=Yes) # comments ");
        //                outText2.println(ORDERING_NOTE);
        //                printer.show(outText2, "Extended_Pictographic", null, width, 14, emoji_pict, true, true, false);
        //                outText2.println("\n#EOF");
        //            }
        //        }

        try (Writer out = new TempPrintWriter(OUTPUT_DIR, "emoji-sequences.txt")) {
            out.write(Utility.getBaseDataHeader("emoji-sequences", 51, "Emoji Sequence Data", Emoji.VERSION_STRING)
                    + "\n");
            List<String> type_fields = Arrays.asList(
                    "Basic_Emoji", 
                    "Emoji_Keycap_Sequence", 
                    "Emoji_Flag_Sequence",
                    "Emoji_Tag_Sequence",
                    "Emoji_Modifier_Sequence");
            int width = maxLength(type_fields);
            showTypeFieldsMessage(out, type_fields);

            printer.show(out, "Basic_Emoji", null, width, 14, 
                    EmojiDataSourceCombined.EMOJI_DATA.getBasicSequences(), true, false, true);
            printer.show(out, "Emoji_Keycap_Sequence", null, width, 14, Emoji.KEYCAPS, true, false, true);
            printer.show(out, "Emoji_Flag_Sequence",
                    "This list does not include deprecated or macroregion flags, except for UN and EU.\n"
                            + "# See Annex B of TR51 for more information.",
                            width, 14, EmojiDataSourceCombined.EMOJI_DATA.getFlagSequences(), true, false, true);
            printer.show(out, "Emoji_Tag_Sequence",
                    "See Annex C of TR51 for more information.",  width, 14, 
                    EmojiDataSourceCombined.EMOJI_DATA.getTagSequences(), true, false, true);
            printer.show(out, "Emoji_Modifier_Sequence", null, width, 14, 
                    EmojiDataSourceCombined.EMOJI_DATA.getModifierSequences(),
                    false, false, true);
            out.write("\n#EOF\n");
        }

        try (Writer out = new TempPrintWriter(OUTPUT_DIR, "emoji-zwj-sequences.txt")) {
            out.write(Utility.getBaseDataHeader("emoji-zwj-sequences", 51, "Emoji ZWJ Sequences",
                    Emoji.VERSION_STRING) + "\n");
            List<String> type_fields = Arrays.asList("Emoji_ZWJ_Sequence");
            int width = maxLength(type_fields);

            showTypeFieldsMessage(out, type_fields);
            UnicodeMap<ZwjType> types = new UnicodeMap<>();
            for (String s : EmojiDataSourceCombined.EMOJI_DATA.getZwjSequencesNormal()) {
                if (s.startsWith(new StringBuilder().appendCodePoint(0x1F3F4).toString())) {
                    int debug = 0;
                }
                ZwjType zwjType = ZwjType.getType(s);
                types.put(s, zwjType);
            }
            printer.show(out, "Emoji_ZWJ_Sequence", "Family", width, 44, types.getSet(ZwjType.family), false, false,
                    true);
            printer.show(out, "Emoji_ZWJ_Sequence", "Gendered Role, with object", width, 44, types.getSet(ZwjType.roleWithObject),
                    false, false, true);
            printer.show(out, "Emoji_ZWJ_Sequence", "Gendered Role", width, 44, types.getSet(ZwjType.roleWithSign), false,
                    false, true);
            printer.show(out, "Emoji_ZWJ_Sequence", "Gendered Activity", width, 44, types.getSet(ZwjType.activity),
                    false, false, true);
            printer.show(out, "Emoji_ZWJ_Sequence", "Gendered Gestures", width, 44, types.getSet(ZwjType.gestures),
                    false, false, true);
            printer.show(out, "Emoji_ZWJ_Sequence", "Other", width, 44, types.getSet(ZwjType.other), false, false,
                    true);
            out.write("\n#EOF\n");
        }

        if (EmojiData.EMOJI_DATA.getVersion().compareTo(Emoji.VERSION5) >= 0) {
            try (Writer out = new TempPrintWriter(OUTPUT_DIR, "emoji-variation-sequences.txt")) {
                out.write(Utility.getBaseDataHeader("emoji-variation-sequences", 51, "Emoji Variation Sequences",
                        Emoji.VERSION_STRING) + "\n");
                final UnicodeSet withVariants = EmojiDataSourceCombined.EMOJI_DATA.getEmojiWithVariants();
                for (String s : withVariants) {
                    // 0023 FE0E; text style; # NUMBER SIGN
                    // 0023 FE0F; emoji style; # NUMBER SIGN
                    final String code = Utility.hex(s);
                    String gap = code.length() == 4 ? " " : "";
                    out.write(code + " FE0E " + gap + "; text style;  # " + getVariationComment(s) + "\n" + code
                            + " FE0F " + gap + "; emoji style; # " + getVariationComment(s) + "\n");
                }
                out.write("\n#Total sequences: " + withVariants.size() + "\n");
                out.write("\n#EOF\n");
            }
        }

        if (DO_TAGS) {
            printer.setFlat(true);
            try (PrintWriter out = FileUtilities.openUTF8Writer(Settings.UNICODE_DRAFT_DIRECTORY + "reports/tr52/",
                    "emoji-tags.txt")) {
                out.println(Utility.getBaseDataHeader("emoji-tags", 52, "Emoji Data", Emoji.VERSION_STRING));
                List<String> type_fields = Arrays.asList("Emoji_Flag_Base", "Emoji_Gender_Base", "Emoji_Hair_Base",
                        "Emoji_Direction_Base");
                int width = maxLength(type_fields);
                showTypeFieldsMessage(out, type_fields);

                printer.show(out, "Emoji_Flag_Base", null, width, 6, flagBase, false, true, false);
                printer.show(out, "Emoji_Gender_Base", null, width, 6, genderBase, false, true, false);
                printer.show(out, "Emoji_Hair_Base", null, width, 6, hairBase, false, true, false);
                printer.show(out, "Emoji_Direction_Base", null, width, 6, directionBase, false, true, false);
                out.println("\n#EOF");
            }
        }

        if (SHOW)
            System.out.println("Regional_Indicators ; " + Emoji.REGIONAL_INDICATORS.toPattern(false));
        if (SHOW)
            System.out.println("Emoji Combining Bases ; " + EmojiData.EMOJI_DATA.getKeycapBases().toPattern(false));
        if (SHOW)
            System.out.println("Emoji All ; " + EmojiDataSourceCombined.EMOJI_DATA.getAllEmojiWithoutDefectives().toPattern(false));

        // generate emoji-test
        GenerateEmojiKeyboard.showLines(EmojiOrder.STD_ORDER, EmojiOrder.STD_ORDER.emojiData.getSortingChars(), Target.propFile, OUTPUT_DIR);

        try (TempPrintWriter reformatted = new TempPrintWriter(OUTPUT_DIR, "internal/emojiOrdering.txt")) {
            reformatted.write(EmojiOrder.BETA_ORDER.getReformatted());
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    private static int maxLength(String... items) {
        int result = 0;
        for (String item : items) {
            result = Math.max(result, item.length());
        }
        return result;
    }

    static final UnicodeMap<String> NAMES = IndexUnicodeProperties.make(Settings.latestVersion).load(UcdProperty.Name);
    static final UnicodeMap<Age_Values> AGES = IndexUnicodeProperties.make(Settings.latestVersion)
            .loadEnum(UcdProperty.Age, UcdPropertyValues.Age_Values.class);

    private static String getVariationComment(String s) {
        Age_Values age = AGES.get(s);
        String name = NAMES.get(s);
        if (name == null) {
            name = CandidateData.getInstance().getUnicodeName(s);
        }
        return "(" + (age == null ? Emoji.VERSION_BETA.getVersionString(2, 2) : age.getShortName()) + ") " + name;
    }

    private static void showTypeFieldsMessage(Writer out, Collection<String> type_fields) throws IOException {
        out.write("# Format:\n");
        out.write("#   code_point(s) ; type_field ; description # comments \n");
        out.write("# Fields:\n");
        out.write("#   code_point(s): one or more code points in hex format, separated by spaces\n");
        out.write("#   type_field"
                + (type_fields.size() != 1 ? ", one of the following: \n#       " 
                        + CollectionUtilities.join(type_fields, "\n#       ")
                        : " :" + type_fields.iterator().next())
                + "\n"
                + "#     The type_field is a convenience for parsing the emoji sequence files, "
                + "and is not intended to be maintained as a property.\n");
        out.write("#   short name: CLDR short name of sequence; characters may be escaped with \\x{hex}.\n");
        out.write(ORDERING_NOTE);
        //		if (type_fields.contains("Emoji_ZWJ_Sequence")) {
        //			out.write("#\n" + "# In display and processing, sequences should be supported both with and without FE0F."
        //					+ "\n# " + SV_WARNING.replace("\n", "\n# ") + "\n#" + "\n# " + SV_CHARS.replace("\n", "\n# "));
        //		}
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

    static final String EXCEPTION_ZWJ = new StringBuilder().appendCodePoint(0x1F441).appendCodePoint(0x200D)
            .appendCodePoint(0x1F5E8).toString();

    static class PropPrinter {
        private UnicodeMap<String> extraNames;
        private boolean flat;

        void show(Writer out, String title, String comments, int maxTitleWidth, int maxCodepointWidth,
                UnicodeSet emojiChars, boolean addVariants, boolean showMissingLine, boolean showName) {
            try {
                Tabber tabber = new Tabber.MonoTabber().add(maxCodepointWidth, Tabber.LEFT).add(maxTitleWidth + 4,
                        Tabber.LEFT);
                if (showName) {
                    tabber.add(65, Tabber.LEFT);
                }
                tabber.add(2, Tabber.LEFT) // hash
                .add(4, Tabber.RIGHT) // version
                .add(6, Tabber.RIGHT) // count
                .add(10, Tabber.LEFT) // character
                ;

                // # @missing: 0000..10FFFF; Bidi_Mirroring_Glyph; <none>
                // 0009..000D ; White_Space # Cc [5]
                // <control-0009>..<control-000D>
                out.write("\n# ================================================\n\n");
                int totalCount = 0;
                if (!showMissingLine) {
                    out.write("# " + title);
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
                UnicodeMap<Age_Values> emojiCharsWithAge = new UnicodeMap<>();
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

                    if (s.contains(UTF16.valueOf(0x1F9D6))) {
                        int debug = 0;
                    }

                    totalCount += rangeCount;
                    String ageDisplay = getAgeDisplay(range.value);
                    if (flat) {
                        if (range.string != null) {
                            throw new IllegalArgumentException("internal error");
                        }
                        for (int cp = range.codepoint; cp <= range.codepointEnd; ++cp) {
                            s = UTF16.valueOf(cp);
                            out.write(
                                    tabber.process(Utility.hex(s) + "\t" + titleField
                                            + (showName ? "\t;" + getName(s) + " " : "")
                                            + "\t#" + "\t" + ageDisplay + "\t" + "\t("
                                            + addEmojiVariant(s, addVariants) + ")"
                                            + (showName ? ""
                                                    : "\t" + getName(s)))
                                    + "\n");
                        }
                    } else if (rangeCount == 1) {
                        final boolean isException = !s.equals(EXCEPTION_ZWJ);
                        out.write(tabber.process(Utility.hex(addEmojiVariant(s, isException && range.string != null))
                                + "\t" + titleField
                                + (showName ? "\t; "
                                        + getName(s) + " "
                                        : "")
                                + "\t#" + "\t" + ageDisplay + "\t[1] " + "\t("
                                + addEmojiVariant(s, isException && (addVariants || range.string != null)) + ")"
                                + (showName ? ""
                                        : "\t" + getName(s)))
                                + "\n");
                    } else {
                        final String e = UTF16.valueOf(range.codepointEnd);
                        out.write(tabber.process(Utility.hex(range.codepoint) + ".." + Utility.hex(range.codepointEnd)
                        + "\t" + titleField
                        + (showName ? "\t; "
                                + getName(s) + " "
                                : "")
                        + "\t#" + "\t" + ageDisplay + "\t["
                        + (range.codepointEnd - range.codepoint + 1) + "] " + "\t("
                        + addEmojiVariant(s, addVariants) + ".." + addEmojiVariant(e, addVariants) + ")"
                        + (showName ? ""
                                : "\t" + getName(s)
                                + ".."
                                + getName(e)))
                                + "\n");
                    }
                }
                out.write("\n");
                // out.println("# UnicodeSet: " + emojiChars.toPattern(false));
                out.write("# Total elements: " + totalCount + "\n");
                UnicodeSet needsEvs = EmojiDataSourceCombined.EMOJI_DATA.getTextPresentationSet();
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
                        if (SHOW)
                            System.out.println(title + "\t" + Utility.hex(s) + "\t"
                                    + (sMinus.equals(s) ? "≣" : Utility.hex(sMinus)) + "\t" + (sPlus.equals(s) ? "≣"
                                            : Utility.hex(sPlus)));
                    }
                }
            } catch (IOException e) {
                throw new ICUUncheckedIOException(e);
            }
        }

        private String getAgeDisplay(Age_Values age) {
            return Emoji.getShortName(age);
        }

        private String getAgeString(VersionInfo value) {
            return Emoji.getShortName(value); // value != Age_Values.Unassigned ? value.getShortName() : CandidateData.CANDIDATE_VERSION;
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
        // return Emoji.getEmojiVariant(s, Emoji.EMOJI_VARIANT_STRING,
        // EmojiDataSourceCombined.EMOJI_DATA.getDefaultPresentationSet(DefaultPresentation.text));
    }

    // ##############################3
    public static void main(String[] args) throws IOException {
        //showExtraInfo();
        printData(EmojiDataSourceCombined.EMOJI_DATA.getRawNames());
    }

    private static void showExtraInfo() {
        IndexUnicodeProperties iup = IndexUnicodeProperties.make();
        System.out.println("Gender Base");
        for (EntryRange s : EmojiDataSourceCombined.EMOJI_DATA.getGenderBases().ranges()) {
            if (s.codepoint == s.codepointEnd) {
                System.out.println(Utility.hex(s.codepoint) + " ;\tEmoji_Gender_Base ;\t" + iup.getName(s.codepoint));
            } else {
                System.out.println(Utility.hex(s.codepoint) + ".." + Utility.hex(s.codepointEnd) 
                + " ;\tEmoji_Gender_Base ;\t" + iup.getName(s.codepoint) + ".." + iup.getName(s.codepointEnd));
            }
        }
        System.out.println("END Gender Base");

        UnicodeSet us2 = EmojiDataSourceCombined.EMOJI_DATA.getAllEmojiWithDefectives();
        UnicodeSet all = new UnicodeSet();
        for (String s : us2) {
            all.addAll(s);
        }
        all.removeAll(EmojiDataSourceCombined.EMOJI_DATA.getSingletonsWithoutDefectives()).removeAll(EmojiDataSourceCombined.EMOJI_DATA.getEmojiComponents());
        for (String s : all) {
            System.out.println(Utility.hex(s)+ "\t" + iup.getName(s, " + "));
        }

        UnicodeSet us = EmojiDataSourceCombined.EMOJI_DATA.getExtendedPictographic();
        System.out.println(us.toPattern(false));

        //        String result = EmojiDataSourceCombined.EMOJI_DATA.getName("🧙‍♀️");
        //
        //        for (int cp : CharSequences.codePoints(EXPLICIT_GENDER_LIST)) {
        //            System.out.println("U+" + Utility.hex(cp) + " " + getName(UTF16.valueOf(cp)));
        //        }
        UnicodeMap<ZwjType> types = new UnicodeMap<>();
        for (String s : EmojiDataSourceCombined.EMOJI_DATA.getZwjSequencesNormal()) {
            types.put(s, ZwjType.getType(s));
        }


        for (ZwjType value : ZwjType.values()) {
            UnicodeSet chars = types.getSet(value);
            System.out.println(value + "\t\t" + chars.toPattern(false));
            for (String s : chars) {
                System.out.println(value + ";\t" + Utility.hex(s) + ";\t" + s + ";\t" + getName(s));
            }
        }
    }

    static final IndexUnicodeProperties iup = IndexUnicodeProperties.make(Settings.latestVersion);
    static final UnicodeMap<General_Category_Values> gcMap = iup.loadEnum(UcdProperty.General_Category, General_Category_Values.class);

    private static String getName(String s) {
        String result = null;
        try {
            result = EmojiDataSourceCombined.EMOJI_DATA.getName(s);
        } catch (Exception e) {
            result = iup.getName(s," + ");
        }
        if (result == null && gcMap.get(s) == General_Category_Values.Unassigned) {
            result = iup.getName(s," + ");
        }
        if (result.startsWith("null")) {
            EmojiDataSourceCombined.EMOJI_DATA.getName(s); // for debugging
            throw new IllegalAccessError();
        }
        result = result.replace("#", "\\x{23}");
        return result;
    }
}
