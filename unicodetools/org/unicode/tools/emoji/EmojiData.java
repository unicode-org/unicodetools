package org.unicode.tools.emoji;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.Annotations;
import org.unicode.cldr.util.Annotations.AnnotationSet;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.With;
import org.unicode.props.GenerateEnums;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.props.UcdPropertyValues.Binary;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.props.UnicodeRelation;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.CountEmoji.Category;
import org.unicode.tools.emoji.Emoji.CharSource;
import org.unicode.tools.emoji.Emoji.Qualified;
import org.unicode.tools.emoji.EmojiOrder.MajorGroup;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetSpanner;
import com.ibm.icu.util.ICUException;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.VersionInfo;

public class EmojiData implements EmojiDataSource {
    // should be properties
    private static final UnicodeSet MULTIPERSON = new UnicodeSet("[👯 🤼 👫-👭 💏 💑 👪 🤝]").freeze();
    private static final UnicodeSet EXPLICIT_HAIR = new UnicodeSet("[👱]").freeze();

    static final int HANDSHAKE = 0x1f91d;
    static final String HANDSHAKE_STRING = UTF16.valueOf(0x1f91d);
    public static final String NEUTRAL_HOLDING = "🧑‍🤝‍🧑";
    public static final String SAMPLE_NEUTRAL_HOLDING_WITH_SKIN = "🧑🏼‍🤝‍🧑🏻";
    public static final String MAN_WITH_RED_HAIR = "👨‍🦰";
    public static final UnicodeSet HOLDING_HANDS_COMPOSITES = new UnicodeSet().add(0x1F46B).add(0x1F46C).add(0x1F46D)
	    .freeze();
    public static final UnicodeSet OTHER_GROUP = new UnicodeSet("[💏 💑]").freeze();

    static final String ZWJ_HANDSHAKE_ZWJ = Emoji.JOINER_STR + "🤝" + Emoji.JOINER_STR;
    private static final String BAD_HANDSHAKE = "👨🏻‍🤝‍👨🏼";
    static final String RIGHTWARDS_HAND = UTF16.valueOf(0x1faf1);
    static final String LEFTWARDS_HAND = UTF16.valueOf(0x1faf2);
    static final String SHAKING_HANDS = RIGHTWARDS_HAND + Emoji.JOINER_STR + LEFTWARDS_HAND;

    public static boolean ALLOW_UNICODE_NAME = System.getProperty("ALLOW_UNICODE_NAME") != null;
    public static final UnicodeSet TAKES_NO_VARIANT = new UnicodeSet(Emoji.EMOJI_VARIANTS_JOINER)
	    .addAll(new UnicodeSet("[[:M:][:Variation_Selector:][:Block=Tags:]]")) // TODO fix to use indexed props
	    .freeze();

    public static final String SAMPLE_WITHOUT_TRAILING_EVS = "👮🏻‍♀";
    public static final AnnotationSet ANNOTATION_SET = Annotations.getDataSet("en");

    public static final UnicodeSet MODIFIERS = new UnicodeSet(0x1F3FB, 0x1F3FF).freeze();

    public enum DefaultPresentation {
	text, emoji
    }

    private final UnicodeSet singletonsWithDefectives = new UnicodeSet();
    private final UnicodeSet singletonsWithoutDefectives = new UnicodeSet();

    private final UnicodeSet allEmojiWithoutDefectives;
    private final UnicodeSet allEmojiWithDefectives;
    private final UnicodeSet allEmojiWithoutDefectivesOrModifiers;

    private final UnicodeSet emojiPresentationSet = new UnicodeSet();
    private final UnicodeSet textPresentationSet = new UnicodeSet();
    private final UnicodeSet emojiRegionalIndicators = new UnicodeSet();
    private final UnicodeSet emojiComponents = new UnicodeSet();
    private final UnicodeSet emojiTagSequences = new UnicodeSet();

    private final UnicodeSet modifierBases;
    private final UnicodeSet modifierBasesRgi;
    private final VersionInfo version;
    private final UnicodeSet modifierSequences;
    private final UnicodeSet zwjSequencesNormal = new UnicodeSet();
    private final UnicodeSet zwjSequencesAll = new UnicodeSet();
    private final UnicodeSet afterZwj = new UnicodeSet();
    private final UnicodeSet flagSequences = new UnicodeSet();
    private final UnicodeSet keycapSequences = new UnicodeSet();
    private final UnicodeSet keycapSequenceAll = new UnicodeSet();
    private final UnicodeSet keycapBases = new UnicodeSet();
    private final UnicodeSet genderBases = new UnicodeSet();
    private final UnicodeSet takesSign = new UnicodeSet();
    private final UnicodeSet hairBases = new UnicodeSet();

    private final UnicodeSet emojiDefectives = new UnicodeSet();
    private final UnicodeMap<String> toNormalizedVariant = new UnicodeMap<String>();
    private final UnicodeMap<String> fromNormalizedVariant = new UnicodeMap<String>();

    private final UnicodeMap<String> names = new UnicodeMap<>();
    private final UnicodeSet emojiWithVariants = new UnicodeSet();
    private final UnicodeSet extendedPictographic = new UnicodeSet();
    // private final UnicodeSet extendedPictographic1 = new UnicodeSet();
    private Multimap<String, String> maleToOther;
    private Multimap<String, String> femaleToOther;
    private UnicodeSet otherHuman;
    private UnicodeSet genderBase;
    private UnicodeMap<String> toNeutral;
    static final UnicodeSetSpanner MODS_SPANNER = new UnicodeSetSpanner(
	    new UnicodeSet(MODIFIERS).addAll(Emoji.ZWJ_GENDER_MARKERS).addAll(Emoji.FULL_ZWJ_GENDER_MARKERS).freeze());
    public static final UnicodeSetSpanner SKIN_SPANNER = new UnicodeSetSpanner(new UnicodeSet(MODIFIERS).freeze());

    public static final Splitter semi = Splitter.onPattern("[;#]").trimResults();
    public static final Splitter semiOnly = Splitter.onPattern(";").trimResults();
    public static final Splitter hashOnly = Splitter.onPattern("#").trimResults();
    public static final Splitter comma = Splitter.on(",").trimResults();

    static final ConcurrentHashMap<VersionInfo, EmojiData> cache = new ConcurrentHashMap<>();
    private static final boolean DEBUG = false;

    public static EmojiData of(VersionInfo version) {
	EmojiData result = cache.get(version);
	if (result == null) {
	    result = new EmojiData(version);
	    cache.put(version, result);
	}
	return result;
    }

    private enum EmojiProp {
	Emoji, Emoji_Presentation, Emoji_Modifier, Emoji_Modifier_Base, Emoji_Component, Extended_Pictographic
    }
    // 0023 ; Emoji # [1] (#️) NUMBER SIGN
    // 231A..231B ; Emoji_Presentation # [2] (⌚️..⌛️) WATCH..HOURGLASS
    // 1F3FB..1F3FF ; Emoji_Modifier
    // 261D ; Emoji_Modifier_Base # [1] (☝️) WHITE UP POINTING INDEX

    private EmojiData(VersionInfo version) {
	final UnicodeMap<General_Category_Values> gc = Emoji.BETA.loadEnum(UcdProperty.General_Category,
		UcdPropertyValues.General_Category_Values.class);
	UnicodeSet NSM = gc.getSet(UcdPropertyValues.General_Category_Values.Nonspacing_Mark);
	UnicodeSet EM = gc.getSet(UcdPropertyValues.General_Category_Values.Enclosing_Mark);
	EnumMap<Emoji.ModifierStatus, UnicodeSet> _modifierClassMap = new EnumMap<>(Emoji.ModifierStatus.class);

	String[] ADD_VARIANT_KEYCAPS = { Emoji.KEYCAP_MARK_STRING,
		// Emoji.TEXT_VARIANT_STRING + Emoji.KEYCAP_MARK_STRING,
		Emoji.EMOJI_VARIANT_STRING + Emoji.KEYCAP_MARK_STRING, };

	this.version = version;
	final String directory = Settings.DATA_DIR + "emoji/" + version.getVersionString(2, 4);
	UnicodeRelation<EmojiProp> emojiData = new UnicodeRelation<>();

	boolean oldFormat = version.compareTo(Emoji.VERSION2) < 0;
	int lineCount = 0;

	for (String line : FileUtilities.in(directory, "emoji-data.txt")) {
	    // # Code ; Default Style ; Ordering ; Annotations ; Sources #Version Char Name
	    // U+263A ; text ; 0 ; face, human, outlined, relaxed, smile, smiley, smiling ;
	    // jw # V1.1 (☺) white smiling face
	    ++lineCount;
	    line = line.trim();
	    if (line.startsWith("#") || line.isEmpty())
		continue;
	    if (line.startsWith("2388")) {
		int debug = 0;
	    }
	    List<String> list;
	    String f0;
	    String f1;
	    try {
		List<String> coreList = hashOnly.splitToList(line);
		list = semi.splitToList(coreList.get(0));
		f0 = list.get(0);
		f1 = list.get(1);
	    } catch (Exception e) {
		throw new ICUException("Malformed line at (" + lineCount + "): «" + line + "»");
	    }

	    if (oldFormat) {
		int pos = f0.indexOf(' ');
		if (pos >= 0) {
		    f0 = f0.substring(0, pos);
		}
	    }

	    int codePoint, codePointEnd;
	    int pos = f0.indexOf("..");
	    if (pos < 0) {
		codePoint = codePointEnd = Integer.parseInt(f0, 16);
	    } else {
		codePoint = Integer.parseInt(f0.substring(0, pos), 16);
		codePointEnd = Integer.parseInt(f0.substring(pos + 2), 16);
	    }

	    if (oldFormat) {
		// # Field 1 — Default_Emoji_Style:
		// # text: default text presentation
		// # emoji: default emoji presentation
		boolean emojiPresentation = false;
		switch (f1) {
		case "text":
		    break;
		case "emoji":
		    emojiPresentation = true;
		    break;
		default:
		    throw new IllegalArgumentException(line);
		}
		// # Field 3 — Emoji_Modifier_Status:
		// # modifier: an emoji modifier
		// # primary: a primary emoji modifier base
		// # secondary: a secondary emoji modifier base
		// # none: not applicable
		final String f3 = list.get(3);
		boolean emojiModifier = false;
		boolean emojiModifierBase = false;

		switch (f3) {
		case "modifier":
		    emojiModifier = true;
		    break;
		case "primary":
		case "secondary":
		    emojiModifierBase = true;
		    break;
		case "none":
		    break;
		default:
		    throw new IllegalArgumentException(line);
		}
		for (int cp = codePoint; cp <= codePointEnd; ++cp) {
		    emojiData.add(cp, EmojiProp.Emoji);
		    if (emojiPresentation) {
			emojiData.add(cp, EmojiProp.Emoji_Presentation);
		    }
		    if (emojiModifier) {
			emojiData.add(cp, EmojiProp.Emoji_Modifier);
		    }
		    if (emojiModifierBase) {
			emojiData.add(cp, EmojiProp.Emoji_Modifier_Base);
		    }
		}
	    } else {
		final EmojiProp prop = EmojiProp.valueOf(f1);
		Binary propValue = Binary.Yes;
		if (list.size() > 2) {
		    propValue = Binary.valueOf(list.get(2));
		}
		for (int cp = codePoint; cp <= codePointEnd; ++cp) {
		    if (cp == 0x1FAD7) {
			int debug = 0;
		    }
		    if (propValue == Binary.No) {
			emojiData.remove(cp, prop);
		    } else {
			emojiData.add(cp, prop);
		    }
		}
	    }
	}

	// check consistency, fix "with defectives"
	for (Entry<String, Set<EmojiProp>> entry : emojiData.entrySet()) {
	    final String cp = entry.getKey();
	    if (cp.equals("🦰")) {
		int debug = 0;
	    }
	    final Set<EmojiProp> set = entry.getValue();
	    if (set.contains(EmojiProp.Extended_Pictographic)) {
		extendedPictographic.add(cp);
		if (set.size() == 1) {
		    continue;
		}
	    }

	    if (!set.contains(EmojiProp.Emoji) && !set.contains(EmojiProp.Emoji_Component)) {
		throw new IllegalArgumentException("**\t" + cp + "\t" + set);
	    }
	    if (cp.contentEquals("🧑‍❤️‍💋‍🧑")) {
		int debug = 0;
	    }

	    if (!Emoji.DEFECTIVE_COMPONENTS.contains(cp)) {
		singletonsWithDefectives.add(cp);
	    }
	    if (!Emoji.DEFECTIVE.contains(cp)) {
		singletonsWithoutDefectives.add(cp);
	    }

	    EmojiData.DefaultPresentation styleIn = set.contains(EmojiProp.Emoji_Presentation)
		    ? EmojiData.DefaultPresentation.emoji
			    : EmojiData.DefaultPresentation.text;
	    if (styleIn == EmojiData.DefaultPresentation.emoji) {
		emojiPresentationSet.add(cp);
	    } else if (!Emoji.DEFECTIVE_COMPONENTS.contains(cp)) {
		textPresentationSet.add(cp);
	    }

	    Emoji.ModifierStatus modClass = set.contains(EmojiProp.Emoji_Modifier) ? Emoji.ModifierStatus.modifier
		    : set.contains(EmojiProp.Emoji_Modifier_Base) ? Emoji.ModifierStatus.modifier_base
			    : Emoji.ModifierStatus.none;
	    putUnicodeSetValue(_modifierClassMap, cp, modClass);

	}
	singletonsWithDefectives.freeze();
	singletonsWithoutDefectives.freeze();

	emojiPresentationSet.freeze();
	textPresentationSet.freeze();
	modifierBases = new UnicodeSet().addAll(_modifierClassMap.get(Emoji.ModifierStatus.modifier_base))
		// .addAll(modifierClassMap.get(ModifierStatus.secondary))
		.freeze();
	if (!modifierBases.contains(0x1F90C)) {
	    if (version.compareTo(Emoji.VERSION13) >= 0) {
		int debug = 0;
	    }
	}
	if (oldFormat) {
	    // Modified this for new format to be driven just from the data files.
	    modifierBasesRgi = new UnicodeSet(modifierBases).removeAll(MULTIPERSON);
	    if (version.compareTo(Emoji.VERSION12) >= 0) {
		modifierBasesRgi.addAll(HOLDING_HANDS_COMPOSITES);
		if (version.compareTo(Emoji.VERSION13_1) >= 0) {
		    modifierBasesRgi.addAll(OTHER_GROUP);
		}
	    }
	    if (!modifierBases.contains(0x1F90C)) {
		if (version.compareTo(Emoji.VERSION13) >= 0) {
		    int debug = 0;
		}
	    }
	    modifierBasesRgi.freeze();
	    modifierSequences = new UnicodeSet();
	    for (String base : modifierBasesRgi) {
		if (UnicodeSet.getSingleCodePoint(base) == 0x1F46B) {
		    int debug = 0;
		}
		for (String mod : MODIFIERS) {
		    final String seq = base + mod;
		    modifierSequences.add(seq);
		    // names.put(seq, UCharacter.toTitleFirst(ULocale.ENGLISH, getName(base, true))
		    // + ", " + shortName(mod.codePointAt(0)).toLowerCase(Locale.ENGLISH));
		}
	    }
	    modifierSequences.freeze();
	} else {
	    modifierBasesRgi = new UnicodeSet();
	    modifierSequences = new UnicodeSet();
	    // HACK 1F441 200D 1F5E8
	    zwjSequencesAll.add(new StringBuilder().appendCodePoint(0x1F441).appendCodePoint(0xFE0F)
		    .appendCodePoint(0x200D).appendCodePoint(0x1F5E8).appendCodePoint(0xFE0F).toString());

	    // VariantFactory vf = new VariantFactory();
	    UnicodeSet debugSet = new UnicodeSet("[\\x{1F48F}\\x{1F491}]").freeze();
	    for (String file : Arrays.asList("emoji-sequences.txt", "emoji-zwj-sequences.txt")) {
		boolean zwj = file.contains("zwj");
		for (String line : FileUtilities.in(directory, file)) {
		    int hashPos = line.indexOf('#');
		    if (hashPos >= 0) {
			line = line.substring(0, hashPos);
		    }
		    if (line.isEmpty()) {
			continue;
		    }
		    if (line.contains("1F48F")) {
			int i = 0;
		    }

		    List<String> list = semi.splitToList(line);
		    String f0 = list.get(0);
		    int codePointStart = -1, codePointEnd = -1;
		    String source = null;
		    int pos = f0.indexOf("..");
		    if (pos < 0) {
			source = Utility.fromHex(f0);
			int last = -1;
			for (int cp1 : With.codePointArray(source)) {
			    if (debugSet.contains(cp1)) {
				int debug = 0;
			    }
			    if (EmojiData.MODIFIERS.contains(cp1)) {
				if (last < 0) {
				    throw new IllegalArgumentException("In " + file + ", modifier " + Utility.hex(last) + " " + UTF16.valueOf(cp1) + "not following base ");
				}
				modifierBasesRgi.add(last);
				modifierSequences.add(With.fromCodePoint(last, cp1));
			    }
			    last = cp1;
			}
		    } else {
			codePointStart = Integer.parseInt(f0.substring(0, pos), 16);
			codePointEnd = Integer.parseInt(f0.substring(pos + 2), 16);
		    }

		    for (int codePoint = codePointStart; codePoint <= codePointEnd; ++codePoint) {
			int first;
			boolean isSingleton = codePoint != -1;
			if (isSingleton) {
			    first = codePoint;
			    source = UTF16.valueOf(codePoint);
			} else {
			    first = source.codePointAt(0);
			}

			final String noVariant = source.replace(Emoji.EMOJI_VARIANT_STRING, "");
			if (!noVariant.equals(source)) {
			    toNormalizedVariant.put(noVariant, source);
			    fromNormalizedVariant.put(source, noVariant);
			}
			emojiDefectives.addAll(source);
			if (zwj) {
			    addToZwjSequencesAll(source);
			    addToSequencesNormal(source);
			    for (String modSeq : addModifiers(source, false)) {
				addToZwjSequencesAll(modSeq);
				addToSequencesNormal(modSeq);
			    }
			    if (zwjSequencesNormal.contains("🧑🏼‍🦯")) {
				int debug = 0;
			    }
			    addToZwjSequencesAll(noVariant); // get non-variant
			    final Set<String> noVariantPlusModifiers = addModifiers(noVariant, false);
			    for (String modSeq : noVariantPlusModifiers) {
				addToZwjSequencesAll(modSeq);
				addName(modSeq, list);
			    }

			    // if (!source.contains("\u2764") ||
			    // source.contains(Emoji.EMOJI_VARIANT_STRING)) {
			    // zwjSequencesNormal.add(source);
			    // }

			    boolean isAfterZwj = false;
			    for (int cp : CharSequences.codePoints(source)) {
				if (isAfterZwj) {
				    afterZwj.add(cp);
				}
				isAfterZwj = cp == 0x200D;
			    }
			} else {
			    if (source.endsWith(Emoji.TAG_TERM)) {
				emojiTagSequences.add(source);
			    } else if (Emoji.isRegionalIndicator(first)) {
				if (!isSingleton) {
				    flagSequences.add(source); // only add pairs
				}
			    } else if (EM.containsSome(noVariant) || NSM.containsSome(noVariant)) {
				final String firstString = source.substring(0, 1);
				keycapSequences
				.add(firstString + Emoji.EMOJI_VARIANT_STRING + Emoji.KEYCAP_MARK_STRING);
				for (String s : ADD_VARIANT_KEYCAPS) {
				    keycapSequenceAll.add(firstString + s);
				}
				keycapBases.add(firstString);
			    } else if (Emoji.DEFECTIVE.contains(first) // if it starts with a defective
				    && !Emoji.KEYCAP_BASE.contains(noVariant) // and is not just one of the keycap
				    // starts in Basic_Emoji
				    ) {
				throw new IllegalArgumentException("Unexpected Defective");
			    }
			}

			addName(noVariant, list);

			if (!Emoji.DEFECTIVE.contains(first)) { // HACK
			    continue;
			}
			emojiData.add(source, EmojiProp.Emoji);
			// if (Emoji.REGIONAL_INDICATORS.contains(first)) {
			// emojiData.add(source, EmojiProp.Emoji_Presentation);
			// }
		    }
		}
	    }
	    modifierBasesRgi.freeze();
	    modifierSequences.freeze();
	}

	if (version.compareTo(Emoji.VERSION4) <= 0) {
	    UnicodeMap<String> sv = IndexUnicodeProperties.make(Emoji.VERSION_TO_GENERATE_UNICODE)
		    .load(UcdProperty.Standardized_Variant);
	    for (String s : sv.keySet()) {
		if (s.contains(Emoji.EMOJI_VARIANT_STRING)) {
		    emojiWithVariants.add(s.codePointAt(0));
		}
	    }
	    if (version.compareTo(Emoji.VERSION4) == 0) {
		emojiWithVariants.add(0x2640).add(0x2642).add(0x2695);
	    }
	} else {
	    for (String line : FileUtilities.in(directory, "emoji-variation-sequences.txt")) {
		int hashPos = line.indexOf('#');
		if (hashPos >= 0) {
		    line = line.substring(0, hashPos);
		}
		if (line.isEmpty())
		    continue;

		List<String> list = semi.splitToList(line);
		String source = Utility.fromHex(list.get(0));
		int cp = source.codePointAt(0);
		emojiWithVariants.add(cp);
	    }
	}
	emojiWithVariants.freeze();

	if (version.compareTo(Emoji.VERSION4) >= 0 && version.compareTo(Emoji.VERSION11) <= 0) {
	    String dir = directory + "/source";
	    String name = "ExtendedPictographic.txt";
	    for (String line : FileUtilities.in(dir, name)) {
		// # Code ; Default Style ; Ordering ; Annotations ; Sources #Version Char Name
		// U+263A ; text ; 0 ; face, human, outlined, relaxed, smile, smiley, smiling ;
		// jw # V1.1 (☺) white smiling face
		if (line.startsWith("#") || line.isEmpty())
		    continue;
		List<String> coreList = hashOnly.splitToList(line);
		List<String> list = semi.splitToList(coreList.get(0));
		final String f0 = list.get(0);
		int codePoint, codePointEnd;
		int pos = f0.indexOf("..");
		if (pos < 0) {
		    codePoint = codePointEnd = Utility.fromHex(f0).codePointAt(0);
		} else {
		    codePoint = Utility.fromHex(f0.substring(0, pos)).codePointAt(0);
		    codePointEnd = Utility.fromHex(f0.substring(pos + 2)).codePointAt(0);
		}

		String prop = list.get(1);
		if (!"ExtendedPictographic".equals(prop.replace("_", ""))) {
		    throw new IllegalArgumentException();
		}
		boolean negative = list.size() > 2 && "NO".equals(list.get(2).toUpperCase(Locale.ENGLISH));
		if (negative) {
		    extendedPictographic.remove(codePoint, codePointEnd);
		} else {
		    extendedPictographic.add(codePoint, codePointEnd);
		}
	    }
	}
	// if (!extendedPictographic1.equals(extendedPictographic)) {
	// showAminusB("pictographic", "file", extendedPictographic, "prop",
	// extendedPictographic1);
	// }

	for (String s : modifierSequences) {
	    s = s.replace(Emoji.EMOJI_VARIANT_STRING, "");
	    if (s.startsWith("🏂")) {
		int debug = 0;
	    }
	    if (names.get(s) == null) { // catch missing names during development
		addName(s, null);
	    }
	}
	emojiData.freeze();
	names.freeze();
	emojiDefectives.removeAll(emojiData.keySet()).freeze();

	// TODO make it cleaner to add new properties
	// emojiRegionalIndicators.addAll(emojiData.getKeys(EmojiProp.Emoji_Regional_Indicator)).freeze();
	emojiComponents.addAll(emojiData.getKeys(EmojiProp.Emoji_Component)).freeze();

	if (version.compareTo(Emoji.VERSION11) >= 0 && !new UnicodeSet(emojiComponents).removeAll(MODIFIERS)
		.removeAll(Emoji.HAIR_STYLES).equals(Emoji.DEFECTIVE)) {
	    throw new IllegalArgumentException(
		    "Bad components or defectives\n" + emojiComponents + "\n" + Emoji.DEFECTIVE);
	}

	zwjSequencesNormal.freeze();
	zwjSequencesAll.removeAll(zwjSequencesNormal).freeze();
	afterZwj.freeze();
	flagSequences.freeze();
	emojiTagSequences.freeze();
	keycapSequences.freeze();
	keycapSequenceAll.removeAll(keycapSequences).freeze();
	keycapBases.freeze();
	toNormalizedVariant.freeze();
	fromNormalizedVariant.freeze();
	UnicodeSet rawHairBases = new UnicodeSet().addAll(
		"🧒 👦 👧 🧑 👨 👩 🧓 👴 👵 👮 🕵 💂 👷 🤴 👸 👳 👲 🧔 🤵 👰 🤰 🤱 🎅 🤶 🧙-🧝 🙍 🙎 🙅 🙆 💁 🙋 🙇 🤦 🤷 💆 💇 🚶 🏃 💃 🕺 👯 🧖-🧘 🛀 🛌 🤺 🏇 ⛷ 🏂 🏌 🏄 🚣 🏊 ⛹ 🏋 🚴 🚵 🏎 🏍 🤸 🤼-🤾 🤹 🎓 🌾 🍳 🏫 🏭 🎨 🚒 ✈ 🚀 🎤 💻 🔬 💼 🔧 ⚖ ♀ ♂ ⚕ ")
		.add(0x1F9B8).add(0x1F9B9).freeze();

	if (DEBUG)
	    System.out.println("rawHairBases: " + rawHairBases.toPattern(false));

	hairBases.addAll(rawHairBases).retainAll(modifierBases).freeze();
	if (DEBUG)
	    System.out.println(version + "Hairbases: " + hairBases.toPattern(false));

	for (String s : zwjSequencesNormal) {
	    if (s.contains("♀️") && !MODIFIERS.containsSome(s)) {
		genderBases.add(s.codePointAt(0));
	    }
	    //	    String s2 = null;
	    //	    if (s.contains(Emoji.JOINER_STR + Emoji.FEMALE)) {
	    //		s2 = s.replace(Emoji.JOINER_STR + Emoji.FEMALE,"");
	    //	    }
	    //	    if (s.contains(Emoji.JOINER_STR + Emoji.MALE)) {
	    //		s2 = s.replace(Emoji.JOINER_STR + Emoji.MALE,"");
	    //	    }
	    //	    if (s2 != null) {
	    //		takesSign.add(EmojiData.removeEmojiVariants(s2));
	    //		takesSign.add(addEmojiVariants(s2));
	    //	    }
	}
	genderBases.freeze();
	takesSign.freeze();

	// data.freeze();
	// charsWithData.addAll(data.keySet());
	// charsWithData.freeze();
	// flatChars.addAll(singletonsWithDefectives)
	// .removeAll(charsWithData.strings())
	// .addAll(Emoji.FIRST_REGIONAL,Emoji.LAST_REGIONAL)
	// .addAll(new UnicodeSet("[0-9*#]"))
	//// .add(Emoji.EMOJI_VARIANT)
	//// .add(Emoji.TEXT_VARIANT)
	//// .add(Emoji.JOINER)
	// //.removeAll(new UnicodeSet("[[:L:][:M:][:^nt=none:]+_-]"))
	// .freeze();

	allEmojiWithoutDefectives = new UnicodeSet(singletonsWithDefectives).addAll(flagSequences)
		.addAll(emojiTagSequences).addAll(modifierSequences).addAll(keycapSequences).addAll(zwjSequencesNormal)
		.removeAll(Emoji.DEFECTIVE).addAll(MODIFIERS).freeze();
	// if (allEmojiWithoutDefectives.contains("👨🏻‍🤝‍👨🏼")) {
	// throw new ICUException("??? 👨🏻‍🤝‍👨🏼");
	// }
	if (allEmojiWithoutDefectives.contains("🧑‍❤️‍💋‍🧑")) {
	    throw new ICUException("??? 👨🏻‍🤝‍👨🏼");
	}
	fixMaleFemale();

	allEmojiWithoutDefectivesOrModifiers = new UnicodeSet();
	for (String s : allEmojiWithoutDefectives) {
	    if (MODIFIERS.contains(s) || !MODIFIERS.containsSome(s)) {
		allEmojiWithoutDefectivesOrModifiers.add(s);
	    }
	}
	allEmojiWithoutDefectivesOrModifiers.freeze();

	if (allEmojiWithoutDefectives.contains(SAMPLE_WITHOUT_TRAILING_EVS)) {
	    int debug = 0;
	}

	allEmojiWithDefectives = new UnicodeSet(allEmojiWithoutDefectives).addAll(zwjSequencesAll)
		.addAll(keycapSequenceAll).freeze();

	// make sure we are a superset (except for modifiers)
	extendedPictographic.addAll(singletonsWithoutDefectives).removeAll(EmojiData.MODIFIERS).freeze();
    }

    private UnicodeSet addToSequencesNormal(String modSeq) {
	// if (modSeq.equals("👨🏻‍🤝‍👨🏼")) {
	// throw new ICUException("??? 👨🏻‍🤝‍👨🏼");
	// }
	return zwjSequencesNormal.add(modSeq);
    }

    private UnicodeSet addToZwjSequencesAll(String source) {
	if (source.contains("👯") && MODIFIERS.containsSome(source)) {
	    int debug = 0;
	}
	return zwjSequencesAll.add(source);
    }

    private void fixMaleFemale() {
	maleToOther = TreeMultimap.create();
	femaleToOther = TreeMultimap.create();
	otherHuman = new UnicodeSet();
	// UnicodeMap<String> fromMan = new UnicodeMap<String>()
	// .put(0x2642, UTF16.valueOf(0x2640)) // MALE SIGN→FEMALE SIGN
	// .put(0x1F466, UTF16.valueOf(0x1F467)) // boy→girl
	// .put(0x1F468, UTF16.valueOf(0x1F469)) // man→woman
	// .freeze();
	//
	// UnicodeMap<String> fromWoman = new UnicodeMap<String>()
	// .put(0x2640,UTF16.valueOf(0x2642)) // FEMALE SIGN→MALE SIGN
	// .put(0x1F467, UTF16.valueOf(0x1F466)) // girl→boy
	// .put(0x1F469, UTF16.valueOf(0x1F468)) // woman→man
	// .freeze();

	for (String emoji : allEmojiWithoutDefectives) {
	    if (emoji.contains("\u26F7")) {
		int debug = 0;
	    }
	    if (Emoji.NEUTRAL.containsSome(emoji)) {
		otherHuman.add(emoji);
	    }
	    String other = Emoji.MALE_TO_OTHER.transform(emoji);
	    if (!emoji.equals(other) && allEmojiWithoutDefectives.contains(other)) {
		maleToOther.put(emoji, other);
	    }
	    other = Emoji.FEMALE_TO_OTHER.transform(emoji);
	    if (!emoji.equals(other) && allEmojiWithoutDefectives.contains(other)) {
		femaleToOther.put(emoji, other);
	    }
	}
	maleToOther = ImmutableMultimap.copyOf(maleToOther);
	femaleToOther = ImmutableMultimap.copyOf(femaleToOther);
	otherHuman.removeAll(maleToOther.keySet()).removeAll(femaleToOther.keySet()).freeze();

	// UnicodeSetSpanner otherHumanSpanner = new UnicodeSetSpanner(otherHuman);
	this.genderBase = new UnicodeSet();
	toNeutral = new UnicodeMap<>();
	for (String emoji : allEmojiWithoutDefectives) {
	    if (emoji.contains("\u26F9")) {
		int debug = 0;
	    }
	    if (Emoji.GENDER_MARKERS.containsSome(emoji)) {
		int first = emoji.codePointAt(0);
		if (!Emoji.GENDER_MARKERS.contains(first)) {
		    genderBase.add(first);
		}
	    }
	    String neutered = Emoji.TO_NEUTRAL.transform(emoji)
		    .replace(Emoji.JOINER + Emoji.FEMALE + Emoji.EMOJI_VARIANT, "")
		    .replace(Emoji.JOINER + Emoji.MALE + Emoji.EMOJI_VARIANT, "");

	    if (!neutered.equals(emoji)) {
		toNeutral.put(emoji, neutered);
	    }
	}
	toNeutral.freeze();
	genderBase.freeze();
    }

    private String typeName(String modSeq) {
	int first = new UnicodeSet().addAll(modSeq).retainAll(MODIFIERS).getRangeStart(0);
	return shortModName(first).toLowerCase(Locale.ENGLISH);
    }

    private void addName(final String source, List<String> lineParts) {
	StringBuilder filtered = new StringBuilder();
	StringBuilder noVariant = new StringBuilder();
	StringBuilder modifierNames = new StringBuilder();
	for (int cp : CharSequences.codePoints(source)) {
	    if (Emoji.EMOJI_VARIANTS.contains(cp)) {
		continue;
	    }
	    if (MODIFIERS.contains(cp)) {
		final boolean empty = modifierNames.length() == 0;
		modifierNames.append(", ");
		if (empty) {
		    modifierNames.append("type");
		}
		modifierNames.append(shortModNameX(cp));
	    } else {
		filtered.appendCodePoint(cp);
	    }
	    noVariant.appendCodePoint(cp);
	}
	String filteredSource = filtered.toString();
	String noVariantSource = noVariant.toString();

	String name;
	if (lineParts != null && lineParts.size() > 2) {
	    name = UCharacter.toTitleCase(lineParts.get(2), BreakIterator.getSentenceInstance(ULocale.ENGLISH));
	} else {
	    name = getFallbackName(filteredSource);
	}
	if (modifierNames.length() != 0) {
	    name += modifierNames;
	}
	String old = names.get(noVariantSource);
	if (false && old != null && !name.equals(old)) {
	    System.out.println(noVariantSource + ";\told: " + old + ";\t" + name);
	}
	names.put(noVariantSource, name);
    }

    public UnicodeSet getSingletonsWithDefectives() {
	return singletonsWithDefectives;
    }

    public UnicodeSet getAllEmojiWithDefectives() {
	return allEmojiWithDefectives;
    }

    public UnicodeSet getEmojiForSortRules() {
	return new UnicodeSet().addAll(getAllEmojiWithoutDefectives()).removeAll(Emoji.DEFECTIVE)
		.addAll(getZwjSequencesNormal()).addAll(getKeycapSequences());
    }

    public UnicodeSet getAllEmojiWithoutDefectives() {
	return allEmojiWithoutDefectives;
    }

    public UnicodeSet getAllEmojiWithoutDefectivesOrModifiers() {
	return allEmojiWithoutDefectivesOrModifiers;
    }

    public UnicodeSet getSingletonsWithoutDefectives() {
	return singletonsWithoutDefectives;
    }

    public UnicodeSet getChars() {
	return getSortingChars();
    }

    public static void freezeUnicodeSets(Collection<UnicodeSet> collection) {
	for (UnicodeSet value : collection) {
	    value.freeze();
	}
    }

    public DefaultPresentation getStyle(String ch) {
	return textPresentationSet.contains(ch) ? DefaultPresentation.text
		: emojiPresentationSet.contains(ch) ? DefaultPresentation.emoji : null;
    }

    public DefaultPresentation getStyle(int ch) {
	return textPresentationSet.contains(ch) ? DefaultPresentation.text
		: emojiPresentationSet.contains(ch) ? DefaultPresentation.emoji : null;
    }

    @Deprecated
    public UnicodeSet getModifierStatusSet(Emoji.ModifierStatus source) {
	return source == Emoji.ModifierStatus.modifier ? getModifiers()
		: source == Emoji.ModifierStatus.modifier_base ? modifierBases
			: throwBad(new IllegalArgumentException());
    }

    public UnicodeSet getModifierBases() {
	return modifierBases;
    }

    public UnicodeSet getModifierBasesRgi() {
	return modifierBasesRgi;
    }

    public UnicodeSet getModifierSequences() {
	return modifierSequences;
    }

    public static UnicodeSet getModifiers() {
	return MODIFIERS;
    }

    public UnicodeSet getZwjSequencesNormal() {
	return zwjSequencesNormal;
    }

    /** Extra variant sequences */
    public UnicodeSet getZwjSequencesAll() {
	return zwjSequencesAll;
    }

    public UnicodeSet getAfterZwj() {
	return afterZwj;
    }

    /**
     * @return the toNormalizedVariant
     */
    public UnicodeMap<String> getToNormalizedVariant() {
	return toNormalizedVariant;
    }

    public UnicodeSet getEmojiPresentationSet() {
	return emojiPresentationSet;
    }

    public UnicodeSet getTextPresentationSet() {
	return textPresentationSet;
    }

    public <T> T throwBad(RuntimeException e) {
	throw e;
    }


    // public Iterable<Entry<String, EmojiDatum>> entrySet() {
    // return data.entrySet();
    // }

    private static Set<Emoji.CharSource> getSet(EnumMap<Emoji.CharSource, UnicodeSet> _defaultPresentationMap,
	    String source, String string) {
	if (string.isEmpty()) {
	    return Collections.emptySet();
	}
	EnumSet<Emoji.CharSource> result = EnumSet.noneOf(Emoji.CharSource.class);
	for (Emoji.CharSource cs : Emoji.CharSource.values()) {
	    if (string.contains(cs.letter)) {
		result.add(cs);
		putUnicodeSetValue(_defaultPresentationMap, source, cs);
	    }
	}
	return Collections.unmodifiableSet(result);
    }

    private static Set<Emoji.CharSource> getSet(String list) {
	if (list.isEmpty()) {
	    return Collections.emptySet();
	}
	EnumSet<Emoji.CharSource> result = EnumSet.noneOf(Emoji.CharSource.class);
	for (Emoji.CharSource cs : Emoji.CharSource.values()) {
	    if (list.contains(cs.letter)) {
		result.add(cs);
	    }
	}
	return Collections.unmodifiableSet(result);
    }

    public static <T> void putUnicodeSetValue(Map<T, UnicodeSet> map, String key, T value) {
	UnicodeSet us = map.get(value);
	if (us == null) {
	    map.put(value, us = new UnicodeSet());
	}
	us.add(key);
    }

    // public final Comparator<String> EMOJI_COMPARATOR = new Comparator<String>() {
    //
    // @Override
    // public int compare(String o1, String o2) {
    // int i1 = 0, i2 = 0;
    // while (true) {
    // if (i1 == o1.length()) {
    // return i2 == o2.length() ? 0 : -1;
    // } else if (i2 == o2.length()) {
    // return 1;
    // }
    // int cp1 = o1.codePointAt(i1);
    // int cp2 = o2.codePointAt(i2);
    // if (cp1 != cp2) {
    // EmojiDatum d1 = getData(cp1);
    // EmojiDatum d2 = getData(cp2);
    // if (d1 == null) {
    // return d2 == null ? cp1 - cp2 : 1;
    // } else {
    // return d2 == null ? -1 : d1.order - d2.order;
    // }
    // }
    // i1 += Character.charCount(cp1);
    // i2 += Character.charCount(cp2);
    // continue;
    // }
    // }
    // };
    //

    private static void show(int cp, final UnicodeMap<String> names, EmojiData emojiData) {
	System.out.println(emojiData.version + "\t" + Utility.hex(cp) + ", " + emojiData.getStyle(cp)
	+ (emojiData.modifierBases.contains(cp) ? ", modifierBase" : "") + "\t" + names.get(cp));
    }

    private static void show(String cp, UnicodeMap<Age_Values> ages, final UnicodeMap<String> names,
	    EmojiData emojiData) {
	System.out.println(BirthInfo.getBirthInfoMap().get(cp) + ";\temojiVersion="
		+ Emoji.getShortName(emojiData.version) + ";\t" + Utility.hex(cp) + ";\t" + cp + ";\t" + names.get(cp)
		+ ";\t" + emojiData.getStyle(cp) + (emojiData.modifierBases.contains(cp) ? ", modifierBase" : ""));
    }

    public UnicodeSet getSortingChars() {
	return getAllEmojiWithoutDefectives();
    }

    public static final EmojiData EMOJI_DATA = of(Emoji.VERSION_TO_GENERATE);
    public static final EmojiData EMOJI_DATA_PREVIOUS = of(Emoji.VERSION_TO_GENERATE_PREVIOUS);
    public static final EmojiData EMOJI_DATA_RELEASED = of(Emoji.VERSION_LAST_RELEASED);
    public static final EmojiData EMOJI_DATA_BETA = Emoji.IS_BETA ? of(Emoji.VERSION_BETA) : EMOJI_DATA;

    public UnicodeSet getFlagSequences() {
	return flagSequences;
    }

    public UnicodeSet getKeycapSequences() {
	return keycapSequences;
    }

    /** Include variant VS sequences **/
    public UnicodeSet getKeycapSequencesAll() {
	return keycapSequenceAll;
    }

    public UnicodeSet getKeycapBases() {
	return keycapBases;
    }

    public boolean skipEmojiSequence(String string) {
	EmojiData emojiData = this;
	if (string.equals(" ") || string.equals("\t") || string.equals(Emoji.EMOJI_VARIANT_STRING)
		|| string.equals(Emoji.TEXT_VARIANT_STRING) || string.equals(Emoji.JOINER_STRING)) {
	    return true;
	}
	if (!emojiData.getSortingChars().contains(string) && !emojiData.getZwjSequencesNormal().contains(string)) {
	    return true;
	}
	return false;
    }

    static final UnicodeSet JCARRIERS = new UnicodeSet().addAll(Emoji.BETA.load(UcdProperty.Emoji_DCM).keySet())
	    .addAll(Emoji.BETA.load(UcdProperty.Emoji_KDDI).keySet())
	    .addAll(Emoji.BETA.load(UcdProperty.Emoji_SB).keySet()).removeAll(new UnicodeSet("[:whitespace:]"))
	    .freeze();

    private static Pattern EMOJI_VARIANTs = Pattern.compile("[" + Emoji.EMOJI_VARIANT + Emoji.TEXT_VARIANT + "]");

    public enum VariantStatus {
	/** All characters that need them have emoji-variants */
	full("fully-qualified"),
	/** The first character has an emoji-variant, if needed */
	initial("minimally-qualified"),
	/** Neither full nor partial */
	other("unqualified"),
	/** Neither full nor partial */
	component("component");
	public final String name;

	private VariantStatus(String name) {
	    this.name = name;
	}

	public static final VariantStatus forString(String name) {
	    for (VariantStatus item : values()) {
		if (name.equals(item.name)) {
		    return item;
		}
	    }
	    return valueOf(name);
	}
    }

    public VariantStatus getVariantStatus(String emoji) {
	if (getEmojiComponents().contains(emoji)) {
	    if (MODIFIERS.contains(emoji) || Emoji.HAIR_STYLES.contains(emoji)) {
		return VariantStatus.component;
	    }
	}
	if (emoji.equals(addEmojiVariants(emoji))) {
	    return VariantStatus.full;
	}
	int first = emoji.codePointAt(0);
	if (emojiPresentationSet.contains(first)) {
	    return VariantStatus.initial;
	}
	int firstCount = Character.charCount(first);
	if (emoji.length() > firstCount) {
	    int second = emoji.codePointAt(firstCount);
	    if (MODIFIERS.contains(second)) {
		return VariantStatus.initial;
	    }
	}
	return VariantStatus.other;
    }

    public class VariantFactory {
	private List<String> parts;
	private String full;

	public VariantFactory set(String source) {
	    if (source.contains("👨") && source.contains("❤")) {
		int debug = 0;
	    }
	    ImmutableList.Builder<String> _parts = ImmutableList.builder();
	    StringBuilder result = new StringBuilder();
	    int[] sequences = CharSequences.codePoints(EMOJI_VARIANTs.matcher(source).replaceAll(""));
	    for (int i = 0; i < sequences.length; ++i) {
		int cp = sequences[i];
		result.appendCodePoint(cp);
		if (!TAKES_NO_VARIANT.contains(cp) && !emojiPresentationSet.contains(cp)
			&& (i == sequences.length - 1 || !MODIFIERS.contains(sequences[i + 1]))) {
		    _parts.add(result.toString());
		    result.setLength(0);
		}
	    }
	    _parts.add(result.toString()); // can be ""
	    parts = _parts.build();
	    full = null;
	    return this;
	}

	public Set<String> getCombinations() { // TODO put in code point order??
	    int size = parts.size();
	    if (size == 1) {
		full = parts.get(0);
		return Collections.singleton(full);
	    }
	    ImmutableSet.Builder<String> combo = ImmutableSet.builder();
	    int max = 1 << (size - 1);
	    StringBuilder result = new StringBuilder();
	    for (int mask = max - 1; mask >= 0; --mask) {
		int item = 0;
		result.setLength(0);
		for (; item < size - 1; ++item) {
		    result.append(parts.get(item));
		    int itemMask = 1 << item;
		    if ((mask & itemMask) != 0) {
			result.append(Emoji.EMOJI_VARIANT);
		    }
		}
		result.append(parts.get(item));
		String itemString = result.toString();
		if (full == null) { // the first one has all 1's, ie, all possible cases with emoji variants
		    full = itemString;
		}
		combo.add(itemString);
	    }
	    return combo.build();
	}

	public boolean hasCombinations() {
	    return parts.size() > 1;
	}

	public String getFull() {
	    return full;
	}
    }

    /**
     * Add EVS to sequences where needed (and remove where not)
     * 
     * @param source
     * @return
     */
    public String addEmojiVariants(String source) {
	return getVariant(source, Emoji.Qualified.all, Emoji.EMOJI_VARIANT);
    }

    public String addEmojiVariants(String source, Emoji.Qualified qualified) {
	return getVariant(source, qualified, Emoji.EMOJI_VARIANT);
    }

    /**
     * Add EVS or TVS to sequences where needed (and remove where not)
     * 
     * @param source
     * @param qualified TODO
     * @return
     */
    public String getVariant(String source, Emoji.Qualified qualified, char variant) {
	// if (variantHandling == VariantHandling.sequencesOnly) {
	// if (!UTF16.hasMoreCodePointsThan(source, 1)) {
	// return source;
	// }
	// }
	StringBuilder result = new StringBuilder();
	int[] sequences = CharSequences.codePoints(source);
	boolean skip = qualified == Qualified.none;
	for (int i = 0; i < sequences.length; ++i) {
	    int cp = sequences[i];
	    // remove VS so we start with a clean slate
	    if (Emoji.EMOJI_VARIANTS.contains(cp)) {
		continue;
	    }
	    result.appendCodePoint(cp);
	    // TODO fix so that this works with string of characters containing emoji and
	    // others.
	    if (!skip && needsVariant(cp)) {
		// items followed by skin-tone modifiers don't use variation selector.
		if (i == sequences.length - 1 || !MODIFIERS.contains(sequences[i + 1])) {
		    result.appendCodePoint(variant);
		}
	    }
	    skip = qualified != Qualified.all;
	}
	return result.toString();
    }

    private boolean needsVariant(int cp) {
	return cp == Emoji.TRANSGENDER_CP // hack
		|| (getSingletonsWithDefectives().contains(cp) && !getEmojiPresentationSet().contains(cp)
			&& !TAKES_NO_VARIANT.contains(cp));
    }

    public UnicodeMap<String> getRawNames() {
	return names;
    }

    public String getName(String source) {
	return _getName(source, false, CandidateData.getInstance());
    }

    static final String DEBUG_STRING = UTF16.valueOf(0x1F3F4);

    private String _getName(String source, boolean toLower, Transform<String, String> otherNameSource) {
	if (source.contains(DEBUG_STRING)) {
	    int debug = 0;
	}
	String name = ANNOTATION_SET.getShortName(source, otherNameSource);
	if (name != null) {
	    return name;
	}
	String tToV = source.replace(Emoji.TEXT_VARIANT, Emoji.EMOJI_VARIANT);
	name = ANNOTATION_SET.getShortName(tToV, otherNameSource);
	if (name != null) {
	    return name;
	}
	name = otherNameSource.transform(source);
	if (name != null) {
	    return name;
	}

	// System.out.println("*** not using name for " + code + "\t" +
	// Utility.hex(code));
	//
	// name = CandidateData.getInstance().getName(source);
	// if (name != null) {
	// return name.toLowerCase(Locale.ENGLISH); // (toLower ? : name);
	// }
	if (!Emoji.DEFECTIVE.contains(source)) { // && !Emoji.EXCLUSIONS.contains(source)
	    // for debugging
	    ANNOTATION_SET.getShortName(source, otherNameSource);
	    ANNOTATION_SET.getShortName(tToV, otherNameSource);
	    if (!ALLOW_UNICODE_NAME) {
		name = otherNameSource.transform(source);
		throw new IllegalArgumentException("no name for " + source + " " + Utility.hex(source)
		+ (source.equals(tToV) ? "" : " or " + Utility.hex(tToV)));
	    }
	}

	source = source.replace(Emoji.EMOJI_VARIANT_STRING, "").replace(Emoji.TEXT_VARIANT_STRING, "");
	name = latest.getName(source, ", ");
	if (name != null) {
	    return name.toLowerCase(Locale.ENGLISH);
	}
	throw new IllegalArgumentException("no name for " + source + " " + Utility.hex(source));
    }

    /**
     * Get all characters that are in emoji sequences, but are not singleton emoji.
     * 
     * @return
     */
    public UnicodeSet getEmojiDefectives() {
	return emojiDefectives;
    }

    public String getFallbackName(String s) {
	final int firstCodePoint = s.codePointAt(0);
	String name = Emoji.NAME.get(firstCodePoint);
	if (s.indexOf(Emoji.ENCLOSING_KEYCAP) >= 0) {
	    return "Keycap " + name.toLowerCase(Locale.ENGLISH);
	}
	final int firstCount = Character.charCount(firstCodePoint);
	main: if (s.length() > firstCount) {
	    int cp2 = s.codePointAt(firstCount);
	    // final EmojiDatum edata = getData(cp2);
	    if (MODIFIERS.contains(cp2)) {
		name += ", " + shortModName(cp2);
	    } else if (Emoji.REGIONAL_INDICATORS.contains(firstCodePoint)) {
		name = "Flag for " + Emoji.getFlagRegionName(s);
	    } else {
		StringBuffer nameBuffer = new StringBuffer();
		boolean sep = false;
		if (s.indexOf(Emoji.JOINER) >= 0) {
		    String title = "";
		    if (s.indexOf(0x1F48B) >= 0) { // KISS MARK
			title = "Kiss: ";
		    } else if (s.indexOf(0x2764) >= 0) { // HEART
			title = "Couple with heart: ";
		    } else if (s.indexOf(EmojiData.HANDSHAKE) >= 0) { // HEART
			title = "Couple holding hands: ";
		    } else if (s.indexOf(0x2640) >= 0) {
			name = nameBuffer.append("FEMALE: ").append(Emoji.NAME.get(s.codePointAt(0))).toString();
			break main;
		    } else if (s.indexOf(0x2642) >= 0) {
			name = nameBuffer.append("MALE: ").append(Emoji.NAME.get(s.codePointAt(0))).toString();
			break main;
		    } else if (Emoji.PROFESSION_OBJECT.containsSome(s)) {
			title = "Role: ";
		    } else if (s.indexOf(0x1F441) < 0) { // !EYE
			title = "Family: ";
		    }
		    nameBuffer.append(title);
		}
		for (int cp : CharSequences.codePoints(s)) {
		    if (cp == Emoji.JOINER || cp == Emoji.EMOJI_VARIANT || cp == 0x2764 || cp == 0x1F48B
			    || cp == HANDSHAKE) { // heart, kiss
			continue;
		    }
		    if (sep) {
			nameBuffer.append(", ");
		    } else {
			sep = true;
		    }
		    nameBuffer.append(Emoji.NAME.get(cp));

		    // nameBuffer.append(cp == Emoji.JOINER ? "zwj"
		    // : cp == Emoji.EMOJI_VARIANT ? "emoji-vs"
		    // : NAME.get(cp));
		}
		name = nameBuffer.toString(); // handle first character
	    }
	}
	return name == null ? "UNNAMED" : name;
    }

    static String shortModName(int cp2) {
	return Emoji.NAME.get(cp2).substring("emoji modifier fitzpatrick ".length());
    }

    static String shortModNameX(int cp2) {
	return Emoji.NAME.get(cp2).substring("EMOJI MODIFIER FITZPATRICK TYPE".length());
    }

    static String shortModNameZ(int cp2) {
	switch (cp2) {
	case 0x1F3FB:
	    return "t1/2";
	case 0x1F3FC:
	case 0x1F3FD:
	case 0x1F3FE:
	case 0x1F3FF:
	    return "t" + (cp2 - 0x1F3F9);
	default:
	    throw new IllegalArgumentException("Illegal Modifier Name");
	}
    }

    public String normalizeVariant(String emojiSequence) {
	String result = toNormalizedVariant.get(emojiSequence);
	if (result != null) {
	    return result;
	}
	if (emojiSequence.contains(Emoji.EMOJI_VARIANT_STRING)) {
	    String trial = emojiSequence.replace(Emoji.EMOJI_VARIANT_STRING, "");
	    result = toNormalizedVariant.get(trial);
	    if (result != null) {
		return result;
	    } else {
		return trial;
	    }
	}
	return emojiSequence;
    }

    // static UnicodeSetSpanner COUPLEPARTS = new UnicodeSetSpanner(new
    // UnicodeSet(Emoji.MAN_STR).add(Emoji.WOMAN_STR).add(HANDSHAKE));
    // public Set<String> addModifiers(String singletonOrSequence) {
    // if (!getModifierBases().containsSome(singletonOrSequence)) {
    // return Collections.emptySet();
    // }
    // Builder<String> builder = ImmutableSet.builder();
    // for (String mod : MODIFIERS) {
    // final String addedModifier = addModifier(singletonOrSequence, mod);
    // if (addedModifier == null) {
    // return Collections.emptySet();
    // }
    // builder.add(addedModifier);
    // }
    // return builder.build();
    // }

    static final Set<String> MULTIPLE_SKINS = ImmutableSet.of("👨‍❤‍👨");

    public Set<String> addModifiers(String singletonOrSequence, boolean addMultiples) {
	if (singletonOrSequence.contains("👨‍❤‍👨")) {
	    int debug = 0;
	}
	if (NEUTRAL_HOLDING.equals(singletonOrSequence) && addMultiples) {
	    int debug = 0;
	}
	if (singletonOrSequence == null || getModifiers().containsSome(singletonOrSequence) // skip if has modifiers
		// already
		|| !getModifierBasesRgi().containsSome(singletonOrSequence) // skip if has no modifier bases
		) {
	    return Collections.emptySet();
	}
	LinkedHashSet<String> output = new LinkedHashSet<>();
	boolean didMultiples = false;
	if (addMultiples) {
	    int handshakePos = singletonOrSequence.indexOf(ZWJ_HANDSHAKE_ZWJ);
	    if (handshakePos > 0) {
		if (version.compareTo(Emoji.VERSION11) > 0) {
		    boolean afterVersion12 = version.compareTo(Emoji.VERSION12) > 0;
		    didMultiples = true;
		    // TODO HACK for now. If we add other groupings with skintones, generalize
		    String prefix = singletonOrSequence.substring(0, handshakePos);
		    String postfix = singletonOrSequence.substring(handshakePos + ZWJ_HANDSHAKE_ZWJ.length());
		    boolean sameAffix = prefix.equals(postfix);
		    final boolean skipIfFirstLighterThanSecond = !afterVersion12 && sameAffix;
		    addMultiples(prefix, ZWJ_HANDSHAKE_ZWJ, postfix, skipIfFirstLighterThanSecond, output);
		}
	    } else if (isHandshake(singletonOrSequence)) {
		    addMultiples(RIGHTWARDS_HAND, Emoji.JOINER_STR, LEFTWARDS_HAND, false, output);
	    } else if (false && MULTIPLE_SKINS.contains(singletonOrSequence)) {
		if (version.compareTo(Emoji.VERSION13) > 0) {
		    String infix = "\u200D\u2764\u200D";
		    int pos = singletonOrSequence.indexOf(infix);
		    if (pos < 0) {
			infix = "\u200D\u2764\u200D\uD83D\uDC8B\u200D";
			pos = singletonOrSequence.indexOf(infix);
		    }
		    if (pos > 0) {
			String prefix = singletonOrSequence.substring(0,pos);
			String postfix = singletonOrSequence.substring(pos + infix.length());
			addMultiples(prefix, ZWJ_HANDSHAKE_ZWJ, postfix, false, output);
		    }
		}
	    }
	}
	if (!didMultiples) {
	    for (String mod : EmojiData.MODIFIERS) {
		String result = addModifierPart(singletonOrSequence, mod);
		if (result != null) {
		    if (result.contains("👯")) {
			int debug = 0;
		    }
		    output.add(result);
		}
	    }
	}
	return output.isEmpty() ? Collections.emptySet() : ImmutableSet.copyOf(output);
    }

    public void addMultiples(String prefix, String infix, String postfix,
	    final boolean skipIfFirstLighterThanSecond, LinkedHashSet<String> output) {
	for (String mod : EmojiData.MODIFIERS) {
	    String prefixMod = addModifierPart(prefix, mod);
	    if (prefixMod == null) {
		throw new IllegalArgumentException("internal error");
	    }
	    for (String mod2 : EmojiData.MODIFIERS) {
		if (skipIfFirstLighterThanSecond && mod.compareTo(mod2) < 0) { // skip if first mod is lighter than second
		    continue;
		}
		String postfixMod = addModifierPart(postfix, mod2);
		if (prefixMod == null) {
		    throw new IllegalArgumentException("internal error");
		}
		String result = prefixMod + infix + postfixMod;
		if (result.equals(BAD_HANDSHAKE)) {
		    int x = 0;
		}
		if (result.contains("👯")) {
		    int debug = 0;
		}
		output.add(result);
	    }
	}
    }

    private String addModifierPart(String singletonOrSequence, String modifier) {
	StringBuilder b = new StringBuilder();
	int countMod = 0;
	boolean justSetMod = false;
	final int[] codePoints = CharSequences.codePoints(singletonOrSequence);
	for (int cp : codePoints) {
	    // handle special condition; we don't want emoji variant or modifier after
	    // modifier!
	    if (justSetMod && (cp == Emoji.EMOJI_VARIANT || MODIFIERS.contains(cp))) {
		continue;
	    }
	    justSetMod = false;

	    b.appendCodePoint(cp);
	    if (getModifierBasesRgi().contains(cp)) {
		if (countMod == 1) {
		    return null; // don't add for 2 or more people.
		}
		++countMod;
		b.append(modifier);
		justSetMod = true;
	    }
	}
	return b.toString();
    }

    static final IndexUnicodeProperties latest = IndexUnicodeProperties.make(GenerateEnums.ENUM_VERSION);
    static final IndexUnicodeProperties beta = IndexUnicodeProperties.make();
    static boolean SKIP = true;

    private static String getSpecialAge(String s) {
	return CandidateData.getInstance().getCharacters().containsAll(s) ? "Candidate"
		: EmojiData.of(Emoji.VERSION3).getAllEmojiWithDefectives().contains(s) ? "Emoji v3.0"
			: EmojiData.of(Emoji.VERSION4).getAllEmojiWithDefectives().contains(s) ? "Emoji v4.0"
				: "Emoji v5.0";
	//
	// return version.compareTo(VersionInfo.UNICODE_10_0) == 0 ? "Candidate"
	// : "Emoji v" + emojiVersion + ", Unicode v" + version.getVersionString(2, 2);
    }

    public static void main(String[] args) {
	final VersionInfo v1_0 = VersionInfo.getInstance(1);
	EmojiData emoji10 = EmojiData.of(v1_0);
	UnicodeSet all = emoji10.getAllEmojiWithoutDefectives();
	System.out
	.println(emoji10.getAllEmojiWithoutDefectives().size() 
		+ "\t" + all);
	for (String s : all) {
	    BirthInfo bi = BirthInfo.getBirthInfo(s);
	    if (bi.emojiVersionInfo != v1_0) {
		throw new IllegalArgumentException(s + "\t" + bi.emojiVersionInfo);
	    }
	}

	EmojiData emojiReleased = EmojiData.of(Emoji.VERSION_LAST_RELEASED);
	EmojiData emojiBeta = EmojiData.of(Emoji.VERSION_BETA);
	for (String s : emojiBeta.allEmojiWithoutDefectives) {
	    if (emojiReleased.allEmojiWithoutDefectives.contains(s)) {
		continue;
	    }
	    System.out.println(Utility.hex(s) + "; \t" + s + "; \t" + emojiReleased.getName(s));
	}
	if (true)
	    return;

	EmojiData e11a = null;
	for (Entry<String, Collection<String>> entry : e11a.maleToOther.asMap().entrySet()) {
	    System.out.println("M2F\t" + entry.getKey() + "\t" + entry.getValue());
	}
	for (Entry<String, Collection<String>> entry : e11a.femaleToOther.asMap().entrySet()) {
	    System.out.println("F2M\t" + entry.getKey() + "\t" + entry.getValue());
	}
	System.out.println("otherHuman:\t" + e11a.otherHuman.toPattern(false));
	System.out.println("otherHuman-mods:\t" + new UnicodeSet(e11a.allEmojiWithoutDefectivesOrModifiers)
		.retainAll(e11a.otherHuman).toPattern(false));

	UnicodeSet explicitGendered = new UnicodeSet().addAll(e11a.maleToOther.keySet())
		.addAll(e11a.femaleToOther.keySet()).add(new UnicodeSet("[🧔]")).freeze();

	UnicodeSet gendered = new UnicodeSet().addAll(e11a.maleToOther.keySet()).addAll(e11a.femaleToOther.keySet())
		.addAll(e11a.otherHuman).freeze();

	UnicodeSet people = new UnicodeSet().addAll(EmojiOrder.BETA_ORDER.majorGroupings.getSet(MajorGroup.People))
		.removeAll(EmojiOrder.BETA_ORDER.charactersToOrdering.getSet("body"))
		.removeAll(EmojiOrder.BETA_ORDER.charactersToOrdering.getSet("emotion"))
		.removeAll(EmojiOrder.BETA_ORDER.charactersToOrdering.getSet("clothing"))
		.retainAll(e11a.allEmojiWithoutDefectives).freeze();

	diff2("gendered", gendered, "people", people);

	System.out
	.println("genderBase:\t" + e11a.getGenderBase().size() + "\t" + e11a.getGenderBase().toPattern(false));

	diff2("otherHuman", new UnicodeSet(e11a.otherHuman).removeAll(e11a.otherHuman.strings()), "genderBase",
		e11a.getGenderBase());

	UnicodeSet explicitNeutral = new UnicodeSet("[👶🧒🧑🧓👼🧔🗣👤👥]").freeze();
	UnicodeSet group = new UnicodeSet("[👫👬👭💏💑👪]").freeze();
	UnicodeSet explicitGender = new UnicodeSet(explicitGendered).removeAll(explicitGendered.strings())
		.removeAll(group);

	show("genderBase", e11a.getGenderBase());
	show("explicitGender", explicitGender);
	show("explicitNeutral", explicitNeutral);
	show("group", group);
	show("otherHuman", new UnicodeSet(e11a.otherHuman).removeAll(e11a.otherHuman.strings())
		.removeAll(e11a.getGenderBase()).removeAll(group).removeAll(explicitNeutral).removeAll(explicitGender));
	// diff2("genderBase", e11a.getGenderBase(), "Emoji.GENDER_BASE",
	// Emoji.GENDER_BASE);

	show("neutrals", new UnicodeSet().addAll(e11a.toNeutral.values()));

	if (true) {
	    return;
	}
	{
	    EmojiData e11 = EmojiData.of(Emoji.VERSION11);
	    EmojiData e5 = EmojiData.of(Emoji.VERSION5);
	    UnicodeSet us11 = new UnicodeSet(e11.getAllEmojiWithoutDefectives())
		    .removeAll(e5.getAllEmojiWithoutDefectives());
	    Set<String> sorted = us11.addAllTo(new TreeSet<>(EmojiOrder.of(Emoji.VERSION11).codepointCompare));
	    int count = 0;
	    for (String s : sorted) {
		String v = e11.addEmojiVariants(s);
		System.out.print(v);
		System.out.println(" " + e11.getName(v));
		// if (count++ > 30) {
		// System.out.println();
		// count = 0;
		// } else {
		// System.out.print(' ');
		// }
	    }
	}
	if (true)
	    return;

	EmojiData one = EmojiData.of(Emoji.VERSION1);
	UnicodeSet e1 = one.getAllEmojiWithDefectives();
	System.out.println("E1.0: " + e1.toPattern(false));
	EmojiData two = EmojiData.of(Emoji.VERSION2);
	UnicodeSet e2 = two.getAllEmojiWithDefectives();
	System.out.println("E2.0 – E1.0: " + new UnicodeSet(e2).removeAll(e1).toPattern(false));

	EmojiData last = EmojiData.of(Emoji.VERSION_BETA);
	Multimap<Category, String> items = TreeMultimap.create();
	for (String item : last.getAllEmojiWithoutDefectives()) {
	    Category cat = Category.getBucket(item);
	    items.put(cat, item);
	}
	for (Entry<Category, Collection<String>> entry : items.asMap().entrySet()) {
	    System.out.println(entry.getKey() + "\t" + entry.getKey().getAttributes() + entry.getValue());
	}
	if (SKIP)
	    return;
	BirthInfo.checkYears();

	EmojiData v6 = EmojiData.of(Emoji.VERSION11);
	EmojiOrder order6 = EmojiOrder.of(Emoji.VERSION11);
	UnicodeSet Uv7 = new UnicodeSet("[:age=7.0:]");
	UnicodeSet newItems6 = new UnicodeSet(v6.allEmojiWithoutDefectivesOrModifiers)
		.addAll(CandidateData.getInstance().getCharacters());
	for (String s : newItems6) {
	    if (Uv7.containsAll(s)) {
		continue;
	    }
	    String category = order6.getCategory(s);
	    if (category == null) {
		category = CandidateData.getInstance().getCategory(s);
	    }
	    System.out.println(UCharacter.toTitleCase(v6.getName(s), null) + "\t"
		    + UCharacter.toTitleCase(category, null) + "\t" + getSpecialAge(s));
	}

	EmojiData v4 = EmojiData.of(Emoji.VERSION4);
	UnicodeSet newItems = new UnicodeSet(last.getSingletonsWithoutDefectives())
		.removeAll(v4.getSingletonsWithoutDefectives());
	Set<String> sorted2 = new TreeSet<>(EmojiOrder.STD_ORDER.codepointCompare);
	for (String s : newItems.addAllTo(sorted2)) {
	    System.out.println("U+" + Utility.hex(s));
	    System.out.println("Name=" + last.getName(s));
	    System.out.println();
	}
	// if (SKIP) return;

	System.out.println(EmojiData.EMOJI_DATA.getDerivableNames().toPattern(false));

	EmojiData betaData = EmojiData.of(Emoji.VERSION_BETA);
	String name3 = betaData.getName("🧙");
	String name4 = betaData.getName("🧙‍♀️");

	VariantFactory vf = betaData.new VariantFactory();
	Multimap<Integer, String> mm = TreeMultimap.create();
	// 1F575 FE0F 200D 2640 FE0F ; Emoji_ZWJ_Sequence ; woman detective # 7.0 [1]
	// (🕵️‍♀️)
	// String testString = new
	// StringBuilder().appendCodePoint(0x0023).appendCodePoint(0x20E3).toString();
	// vf.set(testString);
	// for (String combo : vf.getCombinations()) {
	// System.out.println(Utility.hex(combo, " ")
	// + "\t" + betaData.getName(combo, false, CandidateData.getInstance()));
	// }
	for (String s : betaData.getEmojiForSortRules()) {
	    vf.set(s);
	    Set<String> combos = vf.getCombinations();
	    mm.putAll(combos.size(), combos);
	}

	for (Entry<Integer, Collection<String>> combos : mm.asMap().entrySet()) {
	    int max = 9999;
	    for (String combo : combos.getValue()) {
		Integer count = combos.getKey();
		System.out.println(
			count + "\t" + Utility.hex(combo, " ") + "\t(" + combo + ")" + "\t" + betaData.getName(combo));
		if (--max < 0)
		    break;
	    }
	    System.out.println();
	}

	if (true)
	    return;

	EmojiData lastReleasedData = EmojiData.of(Emoji.VERSION_LAST_RELEASED);
	showDiff("Emoji", Emoji.VERSION_LAST_RELEASED_STRING, lastReleasedData.getSingletonsWithoutDefectives(),
		Emoji.VERSION_BETA_STRING_WITH_COLOR, betaData.getSingletonsWithoutDefectives());
	showDiff("Emoji_Presentation", Emoji.VERSION_LAST_RELEASED_STRING, lastReleasedData.getEmojiPresentationSet(),
		Emoji.VERSION_BETA_STRING_WITH_COLOR, betaData.getEmojiPresentationSet());
	showDiff("Emoji_Modifier_Base", Emoji.VERSION_LAST_RELEASED_STRING, lastReleasedData.getModifierBases(),
		Emoji.VERSION_BETA_STRING_WITH_COLOR, betaData.getModifierBases());

	String name = betaData.getName("🏂🏻");

	for (String s : betaData.getModifierBases()) {
	    String comp = betaData.getVariant(s, Emoji.Qualified.all, Emoji.EMOJI_VARIANT) + "\u200D\u2642\uFE0F";
	    System.out.println(Utility.hex(comp, " ") + "\t" + s + "\t" + betaData.getName(s));
	}
	if (true)
	    return;
	for (String s : betaData.allEmojiWithDefectives) {
	    System.out.println(Emoji.show(s));
	}
	String test = Utility.fromHex("1F482 200D 2640");
	betaData.getVariant(test, Emoji.Qualified.all, Emoji.EMOJI_VARIANT);
	// for (String s : betaData.getZwjSequencesNormal()) {
	// if ("👁‍🗨".equals(s)) {
	// continue;
	// }
	// String newS = betaData.addEmojiVariants(s, Emoji.EMOJI_VARIANT,
	// VariantHandling.sequencesOnly); // 👁‍🗨
	// if (!newS.equals(s)) {
	// throw new IllegalArgumentException(Utility.hex(s) + "\t" +
	// Utility.hex(newS));
	// } else {
	// //System.out.println(Utility.hex(s));
	// }
	// }

	System.out.println("Version " + GenerateEnums.ENUM_VERSION);
	final UnicodeMap<String> betaNames = beta.load(UcdProperty.Name);
	final UnicodeMap<String> names = latest.load(UcdProperty.Name);
	final UnicodeMap<Age_Values> ages = beta.loadEnum(UcdProperty.Age, UcdPropertyValues.Age_Values.class);

	show(0x1f946, names, betaData);
	show(0x1f93b, names, betaData);

	UnicodeSet overlap = new UnicodeSet(betaData.getModifierBases())
		.retainAll(EmojiData.DefaultPresentation.text == EmojiData.DefaultPresentation.emoji
		? betaData.getEmojiPresentationSet()
			: betaData.getTextPresentationSet());
	System.out.println("ModifierBase + TextPresentation: " + overlap.size() + "\t" + overlap.toPattern(false));
	for (String s : overlap) {
	    System.out.println(Utility.hex(s) + "\t" + s + "\t" + ages.get(s) + "\t" + names.get(s));
	}

	System.out.println("v2 SingletonsWithDefectives " + lastReleasedData.getSingletonsWithDefectives().size() + "\t"
		+ lastReleasedData.getSingletonsWithDefectives());

	System.out.println("SingletonsWithDefectives " + betaData.getSingletonsWithDefectives().size() + "\t"
		+ betaData.getSingletonsWithDefectives());
	System.out.println("Defectives "
		+ -(betaData.getSingletonsWithDefectives().size() - betaData.getSingletonsWithoutDefectives().size()));
	System.out.println("Flag Sequences " + betaData.getFlagSequences().size());
	System.out.println("ModiferSequences " + betaData.getModifierSequences().size());
	System.out.println("Keycap Sequences " + betaData.getKeycapSequences().size() + "\t"
		+ betaData.getKeycapSequences().toPattern(false));
	System.out.println("Zwj Sequences " + betaData.getZwjSequencesNormal().size() + "\t"
		+ betaData.getZwjSequencesNormal().toPattern(false));

	System.out.println("modifier" + ", " + betaData.MODIFIERS.toPattern(false));
	System.out.println(
		Emoji.CharSource.WDings + ", " + betaData.getCharSourceSet(Emoji.CharSource.WDings).toPattern(false));
	System.out.println(EmojiData.DefaultPresentation.emoji + ", "
		+ (EmojiData.DefaultPresentation.emoji == EmojiData.DefaultPresentation.emoji
		? betaData.getEmojiPresentationSet()
			: betaData.getTextPresentationSet()).toPattern(false));
	show(0x1F3CB, names, betaData);
	show(0x1F3CB, names, lastReleasedData);
	UnicodeSet keys = new UnicodeSet(betaData.getSingletonsWithDefectives())
		.addAll(lastReleasedData.getSingletonsWithDefectives());
	System.out.println("Diffs");
	for (String key : keys) {
	    // EmojiDatum datum = lastReleasedData.data.get(key);
	    // EmojiDatum other = betaData.data.get(key);
	    if (!dataEquals(lastReleasedData, betaData, key)) {
		// System.out.println("\n" + key + "\t" + Utility.hex(key) + "\t" +
		// names.get(key));
		show(key, ages, betaNames, betaData);
		// show(key, ages, betaNames, emojiData2);
	    }
	}
	System.out.println("Keycap0 " + betaData.getSortingChars().contains("0" + Emoji.KEYCAP_MARK_STRING));
	System.out.println("KeycapE "
		+ betaData.getSortingChars().contains("0" + Emoji.EMOJI_VARIANT_STRING + Emoji.KEYCAP_MARK_STRING));
	System.out.println("KeycapT "
		+ betaData.getSortingChars().contains("0" + Emoji.TEXT_VARIANT_STRING + Emoji.KEYCAP_MARK_STRING));
    }

    private static void show(String title, UnicodeSet uset) {
	for (String emoji : uset.addAllTo(new TreeSet<>(EmojiOrder.BETA_ORDER.codepointCompare))) {
	    String name;
	    try {
		name = EmojiData.EMOJI_DATA.getName(emoji);
	    } catch (Exception e) {
		StringBuilder nameBuffer = new StringBuilder();
		for (int cp : With.codePointArray(emoji)) {
		    if (!MODIFIERS.contains(cp) && EmojiData.EMOJI_DATA.emojiComponents.contains(cp)) {
			continue;
		    }
		    if (nameBuffer.length() != 0) {
			nameBuffer.append(" & ");
		    }
		    nameBuffer.append(EmojiData.EMOJI_DATA.getName(cp));
		}
		name = nameBuffer.toString();
	    }
	    System.out.println(title + "\t\\x{" + Utility.hex(emoji, 1, " ") + "}\t" + emoji + "\t" + name);
	}
    }

    private static void diff(String title1, UnicodeSet set1, String title2, UnicodeSet set2) {
	UnicodeSet uset = new UnicodeSet(set1).removeAll(set2);
	System.out.println(title1 + " - " + title2 + "\t" + uset.size() + "\t" + uset.toPattern(false));
    }

    private static void diff2(String title1, UnicodeSet set1, String title2, UnicodeSet set2) {
	diff(title1, set1, title2, set2);
	diff(title2, set2, title1, set1);
    }

    private UnicodeSet getDerivableNames() {
	UnicodeSet allRaw = getAllEmojiWithoutDefectives();
	UnicodeSet derived = new UnicodeSet();
	for (String s : allRaw) {
	    derived.add(s.replace(Emoji.EMOJI_VARIANT_STRING, ""));
	}
	UnicodeMap<Annotations> explicits = ANNOTATION_SET.getExplicitValues();
	derived.removeAll(explicits.keySet());
	return derived;
    }

    public String getOnlyFirstVariant(String item) {
	item = item.replace(Emoji.EMOJI_VARIANT_STRING, "");
	int first = item.codePointAt(0);
	if (emojiPresentationSet.contains(first)) {
	    return item;
	}
	int firstLen = Character.charCount(first);
	if (item.length() == firstLen) {
	    return item;
	}
	int second = item.codePointAt(firstLen);
	if (MODIFIERS.contains(second)) {
	    return item;
	}
	return UTF16.valueOf(first) + Emoji.EMOJI_VARIANT_STRING + item.substring(firstLen);
    }

    private static void showDiff(String title, String string1, UnicodeSet set1, String string2, UnicodeSet set2) {
	int count = showAminusB(title, string1, set1, string2, set2);
	count += showAminusB(title, string2, set2, string1, set1);
	if (count == 0) {
	    System.out.println("Diff " + title + ": <none>");
	}
    }

    private static int showAminusB(String title, String string1, UnicodeSet set1, String string2, UnicodeSet set2) {
	UnicodeSet firstMinusSecond = new UnicodeSet(set1).removeAll(set2);
	if (!firstMinusSecond.isEmpty()) {
	    System.out.println("Diff " + title + ": " + string1 + " - " + string2 + ": " + firstMinusSecond);
	}
	return firstMinusSecond.size();
    }

    /**
     * private final EmojiData.DefaultPresentation style; private final
     * Emoji.ModifierStatus modifierStatus; private final Set<Emoji.CharSource>
     * sources;
     * 
     * @param lastReleasedData
     * @param betaData
     * @param key
     * @return
     */
    private static boolean dataEquals(EmojiData lastReleasedData, EmojiData betaData, String key) {
	return lastReleasedData.singletonsWithDefectives.contains(key) == betaData.singletonsWithDefectives
		.contains(key)
		&& lastReleasedData.emojiPresentationSet.contains(key) == betaData.emojiPresentationSet.contains(key)
		&& lastReleasedData.modifierBases.contains(key) == betaData.modifierBases.contains(key);
    }

    @Override
    public UnicodeSet getGenderBases() {
	return genderBases;
    }

    @Override
    public UnicodeSet getTakesSign() {
	return takesSign;
    }

    public UnicodeSet getEmojiWithVariants() {
	return emojiWithVariants;
    }

    public VersionInfo getVersion() {
	return version;
    }

    public String getVersionString() {
	return getPlainVersion();
    }

    public String getPlainVersion() {
	return version.getVersionString(2, 2);
    }

    public UnicodeSet getRegionalIndicators() {
	// TODO Auto-generated method stub
	return emojiRegionalIndicators;
    }

    public UnicodeSet getEmojiComponents() {
	return emojiComponents;
    }

    public UnicodeSet getTagSequences() {
	return emojiTagSequences;
    }

    static final UnicodeSet TYPICAL_DUP_GROUP = new UnicodeSet("[{👨‍👩‍👧‍👦}]").freeze(); // "[{👩‍❤️‍💋‍👨}
    // {👨‍👩‍👧‍👦}
    // {👩‍❤️‍👨}]"
    static final UnicodeSet TYPICAL_DUP_SIGN = new UnicodeSet("[{\u200D\u2642}]").freeze();

    public boolean isTypicallyDuplicateSign(String emoji) {
	boolean result = getTakesSign().contains(emoji);
	return result;
    }

    public static boolean isTypicallyDuplicateGroup(String emoji) {
	boolean result = TYPICAL_DUP_GROUP.containsSome(emoji);
	return result;
    }

    public static String removeEmojiVariants(String s) {
	return s.replace(Emoji.EMOJI_VARIANT_STRING, "").replace(Emoji.TEXT_VARIANT_STRING, "");
    }

    public UnicodeSet getExtendedPictographic() {
	return extendedPictographic;
    }

    public UnicodeSet getHairBases() {
	return hairBases;
    }
    
    private static final UnicodeSet EXPLICIT_GENDER_13 = new UnicodeSet(
	    "[[👦-👩 👴 👵 🤴 👸 👲 🧕 🤵 👰 🤰 🤱 🎅 🤶 💃 🕺 🧔 🕴 👫-👭]]")
	    .freeze();

    private static final UnicodeSet EXPLICIT_GENDER_13_1 = new UnicodeSet(EXPLICIT_GENDER_13)
	    .remove("🧔")
	    .freeze();

//    private static final UnicodeSet EXPLICIT_GENDER_13 = new UnicodeSet(
//	    "[[👦-👩 👴 👵 🤴 👸 👲 🧕 🤵 👰 🤰 🤱 🎅 🤶 💃 🕺 🧔 🕴 👫-👭]]")
//	    .freeze();
//
//    private static final UnicodeSet EXPLICIT_GENDER_13_1 = new UnicodeSet(EXPLICIT_GENDER_13)
//	    .remove("🧔")
//	    .freeze();

    public UnicodeSet getExplicitGender() {
	return version.compareTo(Emoji.VERSION13) <= 0 ? EXPLICIT_GENDER_13 : EXPLICIT_GENDER_13_1;
    }

    public UnicodeSet getExplicitHair() {
	return EXPLICIT_HAIR;
    }

    static UnicodeSet getWithoutMods(UnicodeSet chars) {
	UnicodeSet noMods = new UnicodeSet();
	for (String s : chars) {
	    if (MODIFIERS.containsSome(s) && !MODIFIERS.contains(s)) {
		continue;
	    }
	    noMods.add(EMOJI_DATA.addEmojiVariants(s));
	}
	return noMods;
    }

    public UnicodeSet getGenderBase() {
	return genderBase;
    }

    @Override
    public UnicodeSet getMultiPersonGroupings() {
	return MULTIPERSON;
    }

    /**
     * This contains the mapping to the "shortest form" form for certain combinations.
     */
    public static final Map<String, String> MAP_TO_COUPLES = ImmutableMap.<String, String>builder()
	    .put("👨‍🤝‍👨", "👬") 
	    .put("👩‍🤝‍👨", "👫")
	    .put("👩‍🤝‍👩", "👭")
	    .put("🧑‍❤️‍💋‍🧑", "💏")
	    .put("🧑‍❤️‍🧑", "💑")
	    .build();

    public static final Map<String, String> COUPLES_TO_HANDSHAKE_VERSION = ImmutableMap.<String, String>builder()
	    .put("👬", "👨‍🤝‍👨")
	    .put("👫", "👩‍🤝‍👨")
	    .put("👭", "👩‍🤝‍👩")
	    .build();

    public static final UnicodeSet COUPLES = new UnicodeSet().addAll(COUPLES_TO_HANDSHAKE_VERSION.keySet()).freeze();

    /**
     * Remove the skin tone modifiers and the gender signs, and remap the couples.
     * 
     * @param s
     * @return
     */
    public String getBaseRemovingModsGender(String s) {
	String result = addEmojiVariants(EmojiData.MODS_SPANNER.deleteFrom(s));
	String temp = MAP_TO_COUPLES.get(result);
	if (temp != null) {
	    result = temp;
	    if (DEBUG)
		System.out.println("couple: " + s + " => " + result);
	}
	return result;
    }

    public boolean isHandshake(String s) {
	    if (version.compareTo(Emoji.VERSION14) >= 0) {
		    return getBaseRemovingModsGender(s).equals(EmojiData.SHAKING_HANDS);
	    }
	    return false;
    }

    private static final Map<Emoji.CharSource, UnicodeSet> charSourcesToUnicodeSet;
    private static final UnicodeMap<Set<Emoji.CharSource>> codepointToCharSource = new UnicodeMap<>();
    static {
	EnumMap<Emoji.CharSource, UnicodeSet> _charSourceMap = new EnumMap<>(Emoji.CharSource.class);
	for (String line : FileUtilities.in(EmojiData.class, "emojiSources.txt")) {
	    if (line.startsWith("#") || line.isEmpty())
		continue;
	    List<String> list = semi.splitToList(line);
	    String source = Utility.fromHex(list.get(0));
	    Set<Emoji.CharSource> sourcesIn = getSet(list.get(1));
	    codepointToCharSource.put(source, sourcesIn);
	    for (CharSource source2 : sourcesIn) {
		UnicodeSet key = _charSourceMap.get(source2);
		if (key == null) {
		    _charSourceMap.put(source2, key = new UnicodeSet());
		}
		key.add(source);
	    }
	}
	codepointToCharSource.freeze();
	charSourcesToUnicodeSet = Collections.unmodifiableMap(_charSourceMap);
	freezeUnicodeSets(_charSourceMap.values());
    }
    public static UnicodeSet getCharSourceSet(Emoji.CharSource charSource) {
	return CldrUtility.ifNull(charSourcesToUnicodeSet.get(charSource), UnicodeSet.EMPTY);
    }
    public static Set<CharSource> getCharSources(int codepoint) {
	return codepointToCharSource.get(codepoint);
    }
    public static Set<CharSource> getCharSources(String codepoints) {
	return CldrUtility.ifNull(codepointToCharSource.get(codepoints),Collections.emptySet());
    }
}