package org.unicode.tools.emoji;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.tool.Option;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.tool.Option.Params;
import org.unicode.cldr.util.Tabber;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.CountEmoji.Category;
import org.unicode.tools.emoji.GenerateEmojiTestFile.Target;

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
    private static final VersionInfo DATA_VERSION_TO_GENERATE = EmojiData.EMOJI_DATA.getVersion();
    private static final String VERSION_STRING = DATA_VERSION_TO_GENERATE.getVersionString(2, 2);
    
    public static final String OUTPUT_DIR_BASE = Settings.UNICODE_DRAFT_DIRECTORY + "Public/emoji/";
    public static String OUTPUT_DIR = OUTPUT_DIR_BASE + VERSION_STRING;

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

    public static final UnicodeSet HOLDING_HANDS = EmojiData.HOLDING_HANDS_COMPOSITES;

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

    public static <T> void printData() throws IOException {
	printData(EmojiDataSourceCombined.EMOJI_DATA);
    }

    public static <T> void printData(EmojiDataSource emojiDataSource) throws IOException {
	OUTPUT_DIR = OUTPUT_DIR_BASE + emojiDataSource.getPlainVersion();
	UnicodeSet emojiMultiPersonGroupings = emojiDataSource.getMultiPersonGroupings();

	PropPrinter printer = new PropPrinter().set(emojiDataSource);

	try (TempPrintWriter outText2 = new TempPrintWriter(OUTPUT_DIR, "internal/emoji-internal.txt")) {
	    UnicodeSet emojiGenderBase = emojiDataSource.getGenderBases();
	    UnicodeSet emojiExplicitGender = emojiDataSource.getExplicitGender();
	    outText2.println(Utility.getBaseDataHeader("emoji-internal", 51, "Emoji Data Internal", VERSION_STRING));


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

	try (TempPrintWriter outText2 = new TempPrintWriter(OUTPUT_DIR, "internal/emoji-proposals.txt")) {
	    outText2.println("# Mapping from emoji to proposals.");
	    outText2.println("# Format: ");
	    outText2.println("#     <emoji> ; <property> # <comments> ");
	    outText2.println("# To reduce the number of lines, the strings are transformed to skeletons:\n"
		    + "#  • gender signs to 'woman'\n"
		    + "#  • skin tone to 'dark'\n"
		    + "#  • emoji presentation characters removed\n"
		    + "# (No proposal splits two strings with the same skeleton.)");
	    outText2.write(ProposalData.getInstance().toString());
	    outText2.println("\n#EOF");
	}

	try (TempPrintWriter outText2 = new TempPrintWriter(OUTPUT_DIR, "emoji-data.txt")) {
	    UnicodeSet emoji = emojiDataSource.getSingletonsWithDefectives();
	    UnicodeSet emoji_presentation = emojiDataSource.getEmojiPresentationSet();
	    UnicodeSet emoji_modifiers = EmojiData.MODIFIERS;
	    UnicodeSet emoji_modifier_bases = emojiDataSource.getModifierBases();
	    //UnicodeSet emoji_regional_indicators = emojiDataSource.getRegionalIndicators();
	    UnicodeSet emoji_components = emojiDataSource.getEmojiComponents();
	    if (SHOW) System.out.println(EmojiData.EMOJI_DATA_BETA.getExtendedPictographic().contains("🦰"));
	    if (SHOW) System.out.println(CandidateData.getInstance().getExtendedPictographic().contains("🦰"));
	    UnicodeSet emoji_pict = emojiDataSource.getExtendedPictographic();
	    outText2.println(Utility.getBaseDataHeader("emoji-data", 51, "Emoji Data", VERSION_STRING));
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
	    if (Emoji.VERSION5.compareTo(DATA_VERSION_TO_GENERATE) < 0) {
		printer.show(outText2, "Extended_Pictographic", null, width, 14, emoji_pict, true, true, false);
	    }
	    outText2.println("\n#EOF");
	}

	//        if (emojiDataSource.getVersion().compareTo(Emoji.VERSION6) >= 0) {
	//            try (TempPrintWriter outText2 = new TempPrintWriter(OUTPUT_DIR, "emoji-extended-data.txt")) {
	//                UnicodeSet emoji_pict = emojiDataSource.getExtendedPictographic();
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

	UnicodeSet nonRgiSequences = new UnicodeSet();
	String prefix13 = emojiDataSource.getVersionString().compareTo("13") >= 0 ? "RGI_" : "";

	try (Writer out = new TempPrintWriter(OUTPUT_DIR, "emoji-sequences.txt");
		Writer outNonRgi = new TempPrintWriter(OUTPUT_DIR, "internal/emoji-sequences-nonrgi.txt")) {
	    out.write(Utility.getBaseDataHeader("emoji-sequences", 51, "Emoji Sequence Data", VERSION_STRING)
		    + "\n");
	    outNonRgi.write(Utility.getBaseDataHeader("emoji-sequences-nonrgi", 51, "Emoji Sequence Data — Non-RGI", VERSION_STRING)
		    + "\n");
	    List<String> type_fields = Arrays.asList(
		    "Basic_Emoji", 
		    "Emoji_Keycap_Sequence", 
		    prefix13 + "Emoji_Flag_Sequence",
		    prefix13 + "Emoji_Tag_Sequence",
		    prefix13 + "Emoji_Modifier_Sequence");
	    int width = maxLength(type_fields);
	    showTypeFieldsMessage(out, type_fields, "#\n"
		    + "# For the purpose of regular expressions, the property RGI_Emoji is defined as\n"
		    + "# a binary property of strings corresponding to ED-27 in UTS #51 Unicode Emoji.\n"
		    + "# That is, it is the union of the above properties plus RGI_Emoji_ZWJ_Sequence,\n"
		    + "# whose data is in emoji-zwj-sequences.txt.\n"
		    + "# The short name of RGI_Emoji is the same as the long name.\n");

	    printer.show(out, "Basic_Emoji", null, width, 14, 
		    emojiDataSource.getBasicSequences(), true, false, true);
	    printer.show(out, "Emoji_Keycap_Sequence", null, width, 14, Emoji.KEYCAPS, true, false, true);
	    printer.show(out, prefix13 + "Emoji_Flag_Sequence",
		    "This list does not include deprecated or macroregion flags, except for UN and EU.\n"
			    + "# See Annex B of TR51 for more information.",
			    width, 14, emojiDataSource.getFlagSequences(), true, false, true);
	    printer.show(out, prefix13 + "Emoji_Tag_Sequence",
		    "See Annex C of TR51 for more information.",  width, 14, 
		    emojiDataSource.getTagSequences(), true, false, true);

//	    // START separate out nonRGI for L2/19-340
	    // No longer necessary, since building from data directly.
	    UnicodeSet rgiModifierSequences = emojiDataSource.getModifierSequences();
//	    // separate out the sequences
//	    UnicodeSet rgiModifierSequences = new UnicodeSet();
//
//	    rawModifierSequences.forEach(x -> {
//		if (emojiMultiPersonGroupings.containsSome(x) && !HOLDING_HANDS.containsSome(x)) {
//		    nonRgiSequences.add(x);
//		} else {
//		    rgiModifierSequences.add(x);
//		}
//	    });
//	    rgiModifierSequences.freeze();
//	    nonRgiSequences.freeze();
//	    
//	    printer.show(outNonRgi, "Emoji_Modifier_Sequence_Non_RGI", null, width, 14, 
//		    nonRgiSequences,
//		    false, false, true);
	    // END separate out specials for L2/19-340

	    printer.show(out, prefix13 + "Emoji_Modifier_Sequence", null, width, 14, 
		    rgiModifierSequences,
		    false, false, true);
	    out.write("\n#EOF\n");
	}

	try (Writer out = new TempPrintWriter(OUTPUT_DIR, "emoji-zwj-sequences.txt");
		Writer outNonRgi = new TempPrintWriter(OUTPUT_DIR, "internal/emoji-zwj-sequences-nonrgi.txt")) {
	    out.write(Utility.getBaseDataHeader("emoji-zwj-sequences", 51, "Emoji ZWJ Sequences",
		    VERSION_STRING) + "\n");
	    outNonRgi.write(Utility.getBaseDataHeader("emoji-zwj-sequences-nonrgi", 51, "Emoji ZWJ Sequences — Non-RGI",
		    VERSION_STRING) + "\n");
	    final String REZS = prefix13 + "Emoji_ZWJ_Sequence";
	    List<String> type_fields = Arrays.asList(REZS);
	    int width = maxLength(type_fields);
	    UnicodeSet nonRgiZwjSequences = new UnicodeSet();

	    showTypeFieldsMessage(out, type_fields, null);
	    UnicodeMap<String> types = new UnicodeMap<>();
	    for (String s : emojiDataSource.getZwjSequencesNormal()) {
		if (s.contains("👨‍🤝‍👨")) {
		    boolean foo2 = CandidateData.getInstance().getZwjSequencesNormal().contains("👨‍🤝‍👨");
		    int debug = 0;
		}
		if (nonRgiSequences.containsSome(s)) {
		    nonRgiZwjSequences.add(s);
		    continue;
		}
		Category zwjType = CountEmoji.Category.getType(s);
		switch (zwjType) {
		case zwj_seq_fam: 
		case zwj_seq_fam_mod:
		case zwj_seq_mod:
		    types.put(s, "family");
		    break;

		case zwj_seq_gender:
		case zwj_seq_gender_mod:
		    types.put(s, "gendered");
		    break;

		case zwj_seq_role:
		case zwj_seq_role_mod:
		    types.put(s, "role");
		    break;

		case zwj_seq_hair:
		case zwj_seq_mod_hair:
		    types.put(s, "hair");
		    break;

		default:
		    types.put(s, s.equals(EmojiData.NEUTRAL_HOLDING) ? "family" : "other");
		    break;
		}
	    }
	    printer.show(out, REZS, "Family", width, 44, types.getSet("family"), false, false,
		    true);
	    printer.show(out, REZS, "Role", width, 44, types.getSet("role"), false,
		    false, true);
	    printer.show(out, REZS, "Gendered", width, 44, types.getSet("gendered"),
		    false, false, true);
	    printer.show(out, REZS, "Hair", width, 44, types.getSet("hair"), false, false,
		    true);
	    printer.show(out, REZS, "Other", width, 44, types.getSet("other"), false, false,
		    true);

	    printer.show(outNonRgi, REZS, "Non-RGI", width, 44, nonRgiZwjSequences, false, false,
		    true);

	    out.write("\n#EOF\n");
	}

	if (DATA_VERSION_TO_GENERATE.compareTo(Emoji.VERSION5) >= 0) {
	    try (Writer out = new TempPrintWriter(OUTPUT_DIR, "emoji-variation-sequences.txt")) {
		out.write(Utility.getBaseDataHeader("emoji-variation-sequences", 51, "Emoji Variation Sequences",
			VERSION_STRING) + "\n");
		final UnicodeSet withVariants = emojiDataSource.getEmojiWithVariants();
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
		out.println(Utility.getBaseDataHeader("emoji-tags", 52, "Emoji Data", VERSION_STRING));
		List<String> type_fields = Arrays.asList("Emoji_Flag_Base", "Emoji_Gender_Base", "Emoji_Hair_Base",
			"Emoji_Direction_Base");
		int width = maxLength(type_fields);
		showTypeFieldsMessage(out, type_fields, null);

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
	    System.out.println("Emoji All ; " + emojiDataSource.getAllEmojiWithoutDefectives().toPattern(false));


	try (TempPrintWriter reformatted = new TempPrintWriter(OUTPUT_DIR, "internal/emojiOrdering.txt")) {
	    new EmojiDataSourceCombined().showOrderingInterleaved(reformatted);
	}

	// generate emoji-test

	// HACK Exclude the emojiMultiPersonGroupings
	UnicodeSet temp = new UnicodeSet();
	for (String s : EmojiData.EMOJI_DATA.getSortingChars()) {
//	    if (emojiMultiPersonGroupings.containsSome(s) 
//		    && EmojiData.MODIFIERS.containsSome(s)
//		    && !HOLDING_HANDS.containsSome(s)
//		    && !s.contains(EmojiData.ZWJ_HANDSHAKE_ZWJ)
//		    ) {
//		System.out.println("Skipping: " + s);
//		continue;
//	    }
	    temp.add(s);
	}
	temp.freeze();

	System.out.println("Unlike the other data files, the test file doesn't use CandidateData.\n"
		+ "Instead, it needs to have the other data files built for the version, including the emoji ordering.");
	GenerateEmojiTestFile.showLines(EmojiOrder.STD_ORDER, temp, Target.propFile, OUTPUT_DIR);

	//        try (TempPrintWriter reformatted = new TempPrintWriter(OUTPUT_DIR, "internal/emojiOrdering.txt")) {
	//            reformatted.write(EmojiOrder.BETA_ORDER.getReformatted());
	//        } catch (IOException e) {
	//            throw new ICUUncheckedIOException(e);
	//        }
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
	return "(" + (age == null ? DATA_VERSION_TO_GENERATE.getVersionString(2, 2) : age.getShortName()) + ") " + name;
    }

    private static void showTypeFieldsMessage(Writer out, Collection<String> type_fields, String extraMessage) throws IOException {
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

	out.write("#\n"
		+ "# For the purpose of regular expressions, " + 
		(type_fields.size() != 1 ? "each of the type fields" : "the above type field")
		+ " defines the name of\n"
		+ "# a binary property of strings. The short name of "
		+ (type_fields.size() != 1 ? "each" : "the")
		+ " property is the same as the long name.\n");
	if (extraMessage != null) {
	    out.write(extraMessage);
	}
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
	private EmojiDataSource emojiDataSource;

	void show(Writer out, String title, String comments, int maxTitleWidth, int maxCodepointWidth,
		UnicodeSet emojiChars, boolean addVariants, boolean showMissingLine, boolean showName) {
	    try {
		Tabber tabber = new Tabber.MonoTabber().add(maxCodepointWidth, Tabber.LEFT).add(maxTitleWidth + 4,
			Tabber.LEFT);
		if (showName) {
		    tabber.add(65, Tabber.LEFT);
		}
		tabber.add(2, Tabber.LEFT) // hash
		.add(5, Tabber.LEFT) // version
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
		UnicodeMap<VersionInfo> emojiCharsWithAge = new UnicodeMap<>();
		for (String s : emojiChars) {
		    emojiCharsWithAge.put(s, BirthInfo.getVersionInfo(s));
		}

		for (UnicodeMap.EntryRange<VersionInfo> range : emojiCharsWithAge.entryRanges()) {
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
		    String ageDisplay = "E" + range.value.getVersionString(2, 2);
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
		UnicodeSet needsEvs = emojiDataSource.getTextPresentationSet();
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

	public PropPrinter set(EmojiDataSource emojiDataSource) {
	    this.extraNames = emojiDataSource.getRawNames();
	    this.emojiDataSource = emojiDataSource;
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
	// emojiDataSource.getDefaultPresentationSet(DefaultPresentation.text));
    }

    // ##############################3
    enum MyOptions {
	Version(new Params().setMatch("\\d+(\\.\\d++)")), // .setHelp("version number")
	show(new Params().setHelp("showExtraInfo")),
	;

	// BOILERPLATE TO COPY
	final Option option;

	private MyOptions(Params params) {
	    option = new Option(this, params);
	}

	private static Options myOptions = new Options();
	static {
	    for (MyOptions option : MyOptions.values()) {
		myOptions.add(option, option.option);
	    }
	}

	private static Set<String> parse(String[] args, boolean showArguments) {
	    return myOptions.parse(MyOptions.values()[0], args, true);
	}
    }

    public static void main(String[] args) throws IOException {
	MyOptions.parse(args, true);
	if (MyOptions.show.option.doesOccur()) {
	    showExtraInfo();
	    return;
	}
	EmojiDataSource emojiDataSource = MyOptions.Version.option.doesOccur() 
		? EmojiData.of(VersionInfo.getInstance(MyOptions.Version.option.getValue()))
			: EmojiDataSourceCombined.EMOJI_DATA_BETA
			;
		printData(emojiDataSource);
    }

    private static void showExtraInfo() {
	EmojiDataSource emojiDataSource = EmojiDataSourceCombined.EMOJI_DATA;
	IndexUnicodeProperties iup = IndexUnicodeProperties.make();
	System.out.println("Gender Base");
	for (EntryRange s : emojiDataSource.getGenderBases().ranges()) {
	    if (s.codepoint == s.codepointEnd) {
		System.out.println(Utility.hex(s.codepoint) + " ;\tEmoji_Gender_Base ;\t" + iup.getName(s.codepoint));
	    } else {
		System.out.println(Utility.hex(s.codepoint) + ".." + Utility.hex(s.codepointEnd) 
		+ " ;\tEmoji_Gender_Base ;\t" + iup.getName(s.codepoint) + ".." + iup.getName(s.codepointEnd));
	    }
	}
	System.out.println("END Gender Base");

	UnicodeSet us2 = emojiDataSource.getAllEmojiWithDefectives();
	UnicodeSet all = new UnicodeSet();
	for (String s : us2) {
	    all.addAll(s);
	}
	all.removeAll(emojiDataSource.getSingletonsWithoutDefectives()).removeAll(emojiDataSource.getEmojiComponents());
	for (String s : all) {
	    System.out.println(Utility.hex(s)+ "\t" + iup.getName(s, " + "));
	}

	UnicodeSet us = emojiDataSource.getExtendedPictographic();
	System.out.println(us.toPattern(false));

	//        String result = emojiDataSource.getName("🧙‍♀️");
	//
	//        for (int cp : CharSequences.codePoints(EXPLICIT_GENDER_LIST)) {
	//            System.out.println("U+" + Utility.hex(cp) + " " + getName(UTF16.valueOf(cp)));
	//        }
	UnicodeMap<ZwjType> types = new UnicodeMap<>();
	for (String s : emojiDataSource.getZwjSequencesNormal()) {
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
	return getName(s, EmojiDataSourceCombined.EMOJI_DATA);
    }

    private static String getName(String s, EmojiDataSource emojiDataSource) {
	//EmojiDataSource emojiDataSource = EmojiDataSourceCombined.EMOJI_DATA;
	if (EmojiData.SAMPLE_NEUTRAL_HOLDING_WITH_SKIN.equals(s)) {
	    int debug = 0;
	}
	String result = null;
	try {
	    result = emojiDataSource.getName(s);
	} catch (Exception e) {
	    result = iup.getName(s," + ");
	}
	if (result == null && gcMap.get(s) == General_Category_Values.Unassigned) {
	    result = iup.getName(s," + ");
	}
	if (result.startsWith("null")) {
	    emojiDataSource.getName(s); // for debugging
	    throw new IllegalAccessError();
	}
	result = result.replace("#", "\\x{23}");
	return result;
    }
}
