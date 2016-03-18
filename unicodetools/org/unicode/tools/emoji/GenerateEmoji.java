package org.unicode.tools.emoji;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.xerces.impl.dv.util.Base64;
import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.tool.GenerateTransformCharts.CollectionOfComparablesComparator;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.With;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.props.UcdPropertyValues.Binary;
import org.unicode.props.UcdPropertyValues.Block_Values;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.props.VersionToAge;
import org.unicode.text.UCD.NamesList;
import org.unicode.text.utility.Birelation;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.Emoji.ModifierStatus;
import org.unicode.tools.emoji.Emoji.Source;
import org.unicode.tools.emoji.EmojiData.DefaultPresentation;
import org.unicode.tools.emoji.EmojiData.EmojiDatum;
import org.unicode.tools.emoji.EmojiOrder.MajorGroup;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.CollectionUtilities.SetComparator;
import com.ibm.icu.dev.util.TransliteratorUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.EntryRange;
import com.ibm.icu.util.ICUUncheckedIOException;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class GenerateEmoji {
    private static final boolean SHOW_NAMES_LIST = false;

    private static final boolean           DRAFT                       = false;

    private static final String            DRAFT_TITLE_PREFIX          = DRAFT ? "Draft " : "";

    static boolean                 SHOW                        = false;

    private static final boolean           DATAURL                     = true;
    private static final int               RESIZE_IMAGE                = -1;

    private static final String            BREAK                       = "<br>";
    private static final String            DOC_DATA_FILES              = "/../../../reports/tr51/index.html#Data_Files";

    // private static final UnicodeSet EXTRAS = new UnicodeSet(
    // "[☦ ☪-☬ ☸ ✝ 🕉0-9\\u2714\\u2716\\u303D\\u3030 \\u00A9 \\u00AE \\u2795-\\u2797 \\u27B0 \\U0001F519-\\U0001F51C {🇽🇰}]")
    // .add("*"+Emoji.ENCLOSING_KEYCAP)
    // .freeze();
    static final Set<String>               SKIP_WORDS                  = new HashSet<String>(Arrays.asList("with", "a", "in", "without", "and", "white",
            "symbol",
            "sign", "for", "of", "black"));

    static int MODIFIER_STATUS;

    public static final UnicodeMap<String> TO_FIRST_VERSION_FOR_VARIANT = new UnicodeMap<>();
    static {
        //        UnicodeSet HAS_EMOJI_VS = new UnicodeSet();
        //        final UnicodeSet flatChars = new UnicodeSet(EmojiData.EMOJI_DATA.getSingletonsWithDefectives()).removeAll(Emoji.REGIONAL_INDICATORS).freeze();
        //        for (String s : flatChars) {
        //            if (Emoji.STANDARDIZED_VARIANT.containsKey(s + Emoji.EMOJI_VARIANT)) {
        //                HAS_EMOJI_VS.add(s);
        //            }
        //        }
        //        if (SHOW) System.out.println("HAS_EMOJI_VS: " + HAS_EMOJI_VS.toPattern(false));
        // Default is 9.0
        EmojiData betaData = EmojiData.of(Emoji.VERSION_BETA);
        List<Age_Values> reversed = new ArrayList<>(Arrays.asList(UcdPropertyValues.Age_Values.values()));
        Collections.reverse(reversed);
        for (Age_Values v : reversed) {
            if (v == Age_Values.Unassigned) {
                continue;
            } else if (v.compareTo(Age_Values.V6_1) < 0) {
                continue;
            }
            IndexUnicodeProperties iup = IndexUnicodeProperties.make(v);
            UnicodeMap<String> sv = iup.load(UcdProperty.Standardized_Variant);
            String version = v.getShortName();
            for (String key : sv.keySet()) {
                int first = key.codePointAt(0);
                if (first == 0x1F170) {
                    int debug = 0;
                }
                // filter to only emoji in beta version
                if (!betaData.getSingletonsWithDefectives().contains(first)) {
                    continue;
                }
                int second = key.codePointAt(Character.charCount(first));
                if (Emoji.TEXT_VARIANT == second || Emoji.EMOJI_VARIANT == second) {
                    TO_FIRST_VERSION_FOR_VARIANT.put(first, version);
                }
            }
        }
        TO_FIRST_VERSION_FOR_VARIANT.freeze();
        // verify that all are present
        for (String key : new UnicodeSet(EmojiData.EMOJI_DATA.getDefaultPresentationSet(DefaultPresentation.text))
        .removeAll(Emoji.REGIONAL_INDICATORS)
        .removeAll(Emoji.FLAGS)
                ) {
            final int first = key.codePointAt(0);
            String version = TO_FIRST_VERSION_FOR_VARIANT.get(first);
            if (version == null) {
                System.err.println("Missing: " + Utility.hex(key) + "\t" + key);
            }
        }
    }
    static final UnicodeMap<String>        VERSION                     = Emoji.LATEST.load(UcdProperty.Age);
    static {
        System.out.println("🤧, " + Emoji.VERSION_ENUM.get("🤧"));
    }
    static final UnicodeMap<String>        WHITESPACE                  = Emoji.LATEST.load(UcdProperty.White_Space);
    static final UnicodeMap<String>        GENERAL_CATEGORY            = Emoji.LATEST.load(UcdProperty.General_Category);
    static final UnicodeMap<General_Category_Values>        GENERAL_CATEGORY_E            = Emoji.LATEST.loadEnum(UcdProperty.General_Category, UcdPropertyValues.General_Category_Values.class);
    static final UnicodeMap<String>        SCRIPT_EXTENSIONS           = Emoji.LATEST.load(UcdProperty.Script_Extensions);
    private static final UnicodeSet        COMMON_SCRIPT               = new UnicodeSet()
    .addAll(SCRIPT_EXTENSIONS.getSet(UcdPropertyValues.Script_Values.Common
            .toString()))
            .freeze();

    static final UnicodeMap<String>        NFKCQC                      = Emoji.LATEST.load(UcdProperty.NFKD_Quick_Check);
    static final UnicodeSet                JSOURCES                    = new UnicodeSet();
    static {
        UnicodeMap<String> dcmProp = Emoji.LATEST.load(UcdProperty.Emoji_DCM);
        UnicodeMap<String> kddiProp = Emoji.LATEST.load(UcdProperty.Emoji_KDDI);
        UnicodeMap<String> sbProp = Emoji.LATEST.load(UcdProperty.Emoji_SB);
        checkDuplicates(dcmProp, kddiProp, sbProp);
        JSOURCES
        .addAll(dcmProp.keySet())
        .addAll(kddiProp.keySet())
        .addAll(sbProp.keySet())
        .removeAll(WHITESPACE.getSet(UcdPropertyValues.Binary.Yes.toString()))
        .freeze();
        // if (true)
        // System.out.println("Core:\t" + JSOURCES.size() + "\t" + JSOURCES);
    }
    static final Pattern                   tab                         = Pattern.compile("\t");
    static final Pattern                   space                       = Pattern.compile(" ");
    static final String                    REPLACEMENT_CHARACTER       = "\uFFFD";




    //    static final Comparator                CODEPOINT_COMPARE_SHORTER   =
    //            new MultiComparator<String>(
    //                    Emoji.CODEPOINT_LENGTH,
    //                    mp,
    //                    UCA_COLLATOR, // don't
    //                    // need
    //                    // cldr
    //                    // features
    //                    new UTF16.StringComparator(true, false, 0));

    static final Set<String> SORTED_EMOJI_CHARS_SET
    = EmojiOrder.sort(EmojiOrder.STD_ORDER.codepointCompare, 
            EmojiData.EMOJI_DATA.getChars());

    static final Set<String> SORTED_ALL_EMOJI_CHARS_SET
    = EmojiOrder.sort(EmojiOrder.STD_ORDER.codepointCompare, 
            EmojiData.EMOJI_DATA.getAllEmojiWithoutDefectives());

    public static final UnicodeSet APPLE_COMBOS_WITHOUT_VS = EmojiData.EMOJI_DATA.getZwjSequencesAll();

    static final Comparator<String> EMOJI_COMPARATOR;
    static {
        try {
            String rules = EmojiOrder.STD_ORDER.appendCollationRules(new StringBuilder(), 
                    new UnicodeSet(EmojiData.EMOJI_DATA.getChars()).removeAll(Emoji.DEFECTIVE), 
                    EmojiData.EMOJI_DATA.getZwjSequencesNormal(), GenerateEmoji.APPLE_COMBOS_WITHOUT_VS)
                    .toString();
            final RuleBasedCollator ruleBasedCollator = new RuleBasedCollator(rules);
            ruleBasedCollator.setStrength(Collator.IDENTICAL);
            ruleBasedCollator.freeze();
            EMOJI_COMPARATOR = (Comparator<String>) (Comparator) ruleBasedCollator;
            int x = EMOJI_COMPARATOR.compare("#️⃣","☺️");
        } catch (Exception e) {
            throw new ICUUncheckedIOException("Internal Error", e);
        }
    }

    static final EmojiAnnotations          ANNOTATIONS_TO_CHARS_GROUPS = new EmojiAnnotations(EMOJI_COMPARATOR,
            "emojiAnnotationsGroupsSpecial.txt"
            );
    static final UnicodeSet                DEFAULT_TEXT_STYLE          = new UnicodeSet()
    .addAll(ANNOTATIONS_TO_CHARS_GROUPS.getValues("default-text-style"))
    .freeze();

    // static final EmojiAnnotations ANNOTATIONS_TO_CHARS_NEW = new
    // EmojiAnnotations(CODEPOINT_COMPARE, "emojiAnnotationsNew.txt");

    //private static final Subheader                 subheader                   = new Subheader(Settings.SVN_WORKSPACE_DIRECTORY + "unicodetools/data/ucd/7.0.0-Update/");
    static final Set<String>               SKIP_BLOCKS                 = new HashSet(Arrays.asList("Miscellaneous Symbols",
            "Enclosed Alphanumeric Supplement",
            "Miscellaneous Symbols And Pictographs",
            "Miscellaneous Symbols And Arrows"));


    private static void checkDuplicates(UnicodeMap<String> dcmProp, UnicodeMap<String> kddiProp, UnicodeMap<String> sbProp) {
        Relation<String, String> carrierToUnicode = Relation.of(new TreeMap(), TreeSet.class);
        for (Entry<String, String> unicodeToCarrier : dcmProp.entrySet()) {
            carrierToUnicode.put(unicodeToCarrier.getValue(), unicodeToCarrier.getKey());
        }
        for (Entry<String, String> unicodeToCarrier : kddiProp.entrySet()) {
            carrierToUnicode.put(unicodeToCarrier.getValue(), unicodeToCarrier.getKey());
        }
        for (Entry<String, String> unicodeToCarrier : sbProp.entrySet()) {
            carrierToUnicode.put(unicodeToCarrier.getValue(), unicodeToCarrier.getKey());
        }
        int count = 0;
        for (Entry<String, Set<String>> carrierAndUnicodes : carrierToUnicode.keyValuesSet()) {
            Set<String> unicodes = carrierAndUnicodes.getValue();
            if (unicodes.size() > 1) {
                if (SHOW)
                    System.out.println(++count);
                for (String s : unicodes) {
                    if (SHOW)
                        System.out.println(carrierAndUnicodes.getKey() + "\tU+" + Utility.hex(s, " U+") + "\t" + UCharacter.getName(s, " + "));
                }
            }
        }
    }

    enum Style {
        plain, text, emoji, emojiFont, bestImage, refImage;

        public static Style valueOf(DefaultPresentation style) {
            return style == DefaultPresentation.emoji ? Style.emoji 
                    : style == DefaultPresentation.text ? Style.text : null;
        }
    }

    static final Relation<Style, String> STYLE_TO_CHARS = Relation.of(new EnumMap(Style.class), TreeSet.class, EMOJI_COMPARATOR);

    static final UnicodeMap<Integer>     DING_MAP       = new UnicodeMap<>();
    static {
        for (String line : FileUtilities.in(GenerateEmoji.class, "dings.txt")) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\\s*;\\s*");
            DING_MAP.put(Integer.parseInt(parts[0], 16), Integer.parseInt(parts[1], 16));
        }
        DING_MAP.freeze();
    }

    enum Label {
        person, body, face, nature, animal, plant, clothing, emotion,
        food, travel, vehicle, place, office,
        time, weather, game, sport, activity, object,
        sound,
        flag,
        arrow,
        word,
        sign,
        // unknown,
        ;

        static Label get(String string) {
            return Label.valueOf(string);
        }

        static final Comparator<Label>         LABEL_COMPARE   = new Comparator<Label>() {
            public int compare(Label o1, Label o2) {
                return o1.compareTo(o2);
            }
        };

        static final Birelation<String, Label> CHARS_TO_LABELS = Birelation.of(
                new TreeMap<String,Set<Label>>(EMOJI_COMPARATOR),
                new EnumMap<Label,Set<String>>(Label.class),
                TreeSet.class,
                TreeSet.class,
                LABEL_COMPARE,
                EMOJI_COMPARATOR
                );

        static {
            Output<Set<String>> lastLabel = new Output(new TreeSet<String>(EMOJI_COMPARATOR));
            String sublabel = null;
            for (String line : FileUtilities.in(GenerateEmoji.class, "emojiLabels.txt")) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                line = Emoji.getLabelFromLine(lastLabel, line);
                line = Emoji.UNESCAPE.transform(line);
                for (int i = 0; i < line.length();) {
                    String string = Emoji.getEmojiSequence(line, i);
                    i += string.length();
                    if (EmojiData.EMOJI_DATA.skipEmojiSequence(string)) {
                        continue;
                    }
                    for (String item : lastLabel.value) {
                        addLabel(string, Label.valueOf(item));
                    }
                }
            }
            for (String isoCountries : ULocale.getISOCountries()) {
                if (!Emoji.ASCII_LETTER_HYPHEN.containsAll(isoCountries)) {
                    continue;
                }
                String cc = Emoji.getHexFromFlagCode(isoCountries);
                addLabel(cc, Label.flag);
            }
            // remove misc
            for (Entry<String, Set<Label>> entry : CHARS_TO_LABELS.keyValuesSet()) {
                String chars = entry.getKey();
                Set<Label> set = entry.getValue();
                if (set.contains(Label.sign) && set.size() > 1) {
                    CHARS_TO_LABELS.remove(chars, Label.sign);
                }
            }
            CHARS_TO_LABELS.freeze();
            int i = 0;
            if (SHOW)
                for (Entry<Label, Set<String>> entry : CHARS_TO_LABELS.valueKeysSet()) {
                    System.out.println(i++ + "\t" + entry.getKey() + "\t" + entry.getValue());
                }
        }

        public static void addLabel(String string, Label lastLabel) {
            if (string.contains("Ⓕ")) {
                int x = 0; // for debugging
            }
            if (!EmojiData.EMOJI_DATA.getChars().containsAll(string)) {
                return;
            }
            CHARS_TO_LABELS.add(string, lastLabel);
        }
    }

    private static String getAnchor(String code) {
        return code.replace(" ", "_").replace("+", "").replace("U", "");
    }

    static final UnicodeMap<Emoji.Source> BEST_OVERRIDE = new UnicodeMap<>();
    static {
        BEST_OVERRIDE.putAll(new UnicodeSet("[🕐-🕧🚶🏃💃👪👫👬👭🙍🙎🙅🙆🙇🙋🙌🙏💮]"), Emoji.Source.google);
        BEST_OVERRIDE.putAll(new UnicodeSet("[✊-✌ 💅💪👂👃👯" +
                "👦 👰 👧  👨  👩  👮  👱  👲  👳 👴  👵  👶  👷  👸  💁  💂 👼" +
                "👈👉☝👆👇👊  👋  👌  👍👎 👏  👐]"), Emoji.Source.twitter);
        BEST_OVERRIDE.putAll(new UnicodeSet("[💆👯💏💑💇]"), Emoji.Source.windows);
        BEST_OVERRIDE.putAll(new UnicodeSet("[💇]"), Emoji.Source.ref);
        BEST_OVERRIDE.freeze();
    }

    public static String getBestImage(String s, boolean useDataURL, String extraClasses, Emoji.Source... doFirst) {
        String result = getBestImageNothrow(s, useDataURL, extraClasses, doFirst);
        if (result != null) {
            return result;
        }
        throw new IllegalArgumentException("Can't find image for: " + Utility.hex(s) + " " + Emoji.getName(s, true, GenerateEmoji.EXTRA_NAMES) + "\t" + Emoji.buildFileName(s, "_"));
    }

    private static String getBestImageNothrow(String s, boolean useDataURL, String extraClasses, Emoji.Source... doFirst) {
        for (Emoji.Source source : Emoji.orderedEnum(doFirst)) {
            String cell = getImage(source, s, useDataURL, extraClasses);
            if (cell != null) {
                return cell;
            }
        }

        if (EmojiData.MODIFIERS.containsSome(s) && !EmojiData.MODIFIERS.contains(s) && s.length() > 2) {
            int[] points = CharSequences.codePoints(s);
            String s2 = fromIntArray(points,0,points.length-1);
            for (Emoji.Source source : Emoji.orderedEnum(doFirst)) {
                String cell = getImage(source, s2, useDataURL, extraClasses);
                if (cell != null) {
                    return cell;
                }
            }
        }
        return null;
    }

    private static String fromIntArray(int[] points, int start, int limit) {
        StringBuilder result = new StringBuilder();
        for (int i = start; i < limit; ++i) {
            result.appendCodePoint(points[i]);
        }
        return result.toString();
    }

    static public String getImage(Emoji.Source type, String chars, boolean useDataUrl, String extraClasses) {
        String filename = Emoji.getImageFilenameFromChars(type, chars);
        if (filename != null && new File(Emoji.IMAGES_OUTPUT_DIR, filename).exists()) {
            String className = type.getClassAttribute(chars);
            // String className = "imga";
            // if (type == Source.ref && getFlagCode(chars) != null) {
            // className = "imgf";
            // }
            return "<img alt='" + chars + "'" +
            (useDataUrl ? " class='" + className + extraClasses + "'" 
                    //: " height=\"24\" width=\"auto\""
                    : " class='imga'"
                    ) +
                    " src='" + (useDataUrl ? getDataUrl(filename) : "../images/" + filename) + "'" +
                    " title='" + getCodeAndName(chars, " ") + "'" +
                    ">";
        }
        return null;
    }

    private static final EnumSet<Emoji.Source> platformsToIncludeNormal = EnumSet.of(
            Source.apple, Source.google, Source.windows, Source.twitter, Source.emojione, Source.samsung,
            Source.gmail, Source.dcm, Source.kddi, Source.sb
            );
    private static final EnumSet<Emoji.Source> platformsToIncludeExtra = EnumSet.noneOf(Source.class);
    static {
        platformsToIncludeExtra.addAll(platformsToIncludeNormal);
    }

    /**
     * @param extraPlatforms TODO
     * @return the platformsToInclude
     */

    public static int getResponseCode(String urlString) {
        try {
            URL u = new URL(urlString);
            HttpURLConnection huc = (HttpURLConnection) u.openConnection();
            huc.setRequestMethod("GET");
            huc.connect();
            return huc.getResponseCode();
        } catch (Exception e) {
            return -1;
        }
    }

    static final UnicodeSet                 VERSION70                = VERSION.getSet(UcdPropertyValues.Age_Values.V7_0.toString());

    static final Birelation<String, String> OLD_ANNOTATIONS_TO_CHARS = new Birelation(
            new TreeMap(EMOJI_COMPARATOR),
            new TreeMap(EMOJI_COMPARATOR),
            TreeSet.class,
            TreeSet.class,
            EMOJI_COMPARATOR,
            EMOJI_COMPARATOR);
    static {
        addOldAnnotations();
    }

    private static void compareOtherAnnotations() {
        for (Entry<String, Set<String>> entry : OLD_ANNOTATIONS_TO_CHARS.valueKeysSet()) {
            String chars = entry.getKey();
            Set<String> oldAnnotations = entry.getValue();

            Set<String> newAnnotations = new TreeSet(Utility.ifNull(EmojiAnnotations.ANNOTATIONS_TO_CHARS.getKeys(chars), Collections.EMPTY_SET));
            Set<Label> labels = Utility.ifNull(Label.CHARS_TO_LABELS.getValues(chars), Collections.EMPTY_SET);
            for (Label label : labels) {
                newAnnotations.add(label.toString());
            }

            if (!Objects.equals(newAnnotations, oldAnnotations)) {
                TreeSet oldNotNew = new TreeSet(oldAnnotations);
                oldNotNew.removeAll(newAnnotations);
                TreeSet newNotOld = new TreeSet(newAnnotations);
                newNotOld.removeAll(oldAnnotations);
                TreeSet both = new TreeSet(newAnnotations);
                both.retainAll(oldAnnotations);
                System.out.println(getCodeAndName(chars, "\t")
                        + "\t" + CollectionUtilities.join(oldNotNew, ", ")
                        + "\t" + CollectionUtilities.join(newNotOld, ", ")
                        + "\t" + CollectionUtilities.join(both, ", ")
                        );
            }
        }
    }

    private static void addOldAnnotations() {
        for (String line : FileUtilities.in(GenerateEmoji.class, "oldEmojiAnnotations.txt")) {
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            // U+00AE Registered symbol, Registered
            String[] fields = line.split("\t");
            String[] codes = fields[0].split("U+");
            StringBuilder realChars = new StringBuilder();
            for (String code : codes) {
                if (code.isEmpty()) {
                    continue;
                }
                int codePoint = Integer.parseInt(code, 16);
                realChars.appendCodePoint(codePoint);
            }
            // String realChars = ANDROID_REMAP_VALUES.get(codepoint);
            // if (realChars == null) {
            // realChars = new
            // StringBuilder().appendCodePoint(codepoint).toString();
            // }
            if (Emoji.NAME.get(realChars.codePointAt(0)) == null) {
                if (SHOW)
                    System.out.println("skipping private use: " + Integer.toHexString(realChars.codePointAt(0)));
                continue;
            }
            addWords(realChars.toString(), fields[1]);
        }
    }

    public static void addWords(String chars, String name) {
        if (OLD_ANNOTATIONS_TO_CHARS.getKeys(chars) != null) {
            throw new IllegalArgumentException("duplicate");
        }
        String nameString = name.replaceAll("[^-A-Za-z:]+", " ").toLowerCase(Locale.ENGLISH);
        for (String word : nameString.split(" ")) {
            if (!SKIP_WORDS.contains(word) && word.length() > 1 && Emoji.getFlagCode(chars) == null) {
                OLD_ANNOTATIONS_TO_CHARS.add(word, chars);
            }
        }
    }

    static String getFlag(String chars, String extraClasses) {
        String filename = Emoji.getImageFilenameFromChars(Emoji.Source.ref, chars);
        String cc = Emoji.getFlagRegionName(chars);
        return cc == null ? null : "<img"
                + " alt='" + chars + "'"
                + " class='imgf" + extraClasses + "'"
                + " title='" + getCodeAndName(chars, " ") + "'"
                + " src='" + getDataUrl(filename) + "'>";
    }

    public static void main(String[] args) throws IOException {
        FileUtilities.copyFile(GenerateEmoji.class, "emoji-list.css", Emoji.CHARTS_DIR);
        FileUtilities.copyFile(GenerateEmoji.class, "emoji-list.css", Emoji.TR51_INTERNAL_DIR);
        showOrdering(Style.bestImage, true, false);
        showCandidates();
        // show data
        System.out.println("Total Emoji:\t" + EmojiData.EMOJI_DATA.getChars().size());
        UnicodeSet newItems = new UnicodeSet();
        newItems.addAll(EmojiData.EMOJI_DATA.getChars());
        newItems.removeAll(JSOURCES);
        UnicodeSet newItems70 = new UnicodeSet(newItems).retainAll(VERSION70);
        UnicodeSet newItems63 = new UnicodeSet(newItems).removeAll(newItems70);
        UnicodeSet newItems63flags = getStrings(newItems63);
        newItems63.removeAll(newItems63flags);
        if (SHOW) System.out.println("Other 6.3 Flags:\t" + newItems63flags.size() + "\t" + newItems63flags);
        if (SHOW) System.out.println("Other 6.3:\t" + newItems63.size() + "\t" + newItems63);
        if (SHOW) System.out.println("Other 7.0:\t" + newItems70.size() + "\t" + newItems70);

        for (boolean extraPlatforms : Arrays.asList(true, false)) {
            EmojiStats stats = new EmojiStats();
            print(Form.fullForm, stats, extraPlatforms);
            stats.write(Source.VENDOR_SOURCES);
            if (Emoji.IS_BETA) {
                GenerateEmojiData.printData(GenerateEmoji.EXTRA_NAMES);
            }
        }
        print(Form.noImages, null, false);
        // print(Form.extraForm, missingMap, null);
        showNewCharacters();
        for (String e : EmojiData.EMOJI_DATA.getChars()) {
            STYLE_TO_CHARS.put(Style.valueOf(EmojiData.EMOJI_DATA.getData(e).style), e);
        }
        printCollationOrder();
        //printAnnotations();
        STYLE_TO_CHARS.freeze();
        showTextStyle();
        showOrdering(Style.bestImage, false, false);
        // showOrdering(Style.refImage);
        showLabels();
        showVersions();
        showVersionsOnly();
        showDefaultStyle();
        showConstructedNames();
        showVariationSequences();
        showSequences();
        // showSubhead();
        showAnnotations(Emoji.CHARTS_DIR, "emoji-annotations.html", EmojiData.EMOJI_DATA.getChars(), null, false);
        showAnnotations(Emoji.TR51_INTERNAL_DIR
                , "emoji-annotations-flags.html", Emoji.FLAGS, EmojiAnnotations.GROUP_ANNOTATIONS, true);
        showAnnotations(Emoji.TR51_INTERNAL_DIR
                , "emoji-annotations-groups.html", EmojiData.EMOJI_DATA.getChars(), EmojiAnnotations.GROUP_ANNOTATIONS, false);
        showAnnotationsBySize(Emoji.TR51_INTERNAL_DIR
                , "emoji-annotations-size.html", new UnicodeSet(EmojiData.EMOJI_DATA.getChars()).removeAll(Emoji.FLAGS));

        // showAnnotationsDiff();
        // compareOtherAnnotations();
        showOtherUnicode();
        // oldAnnotationDiff();
        // check twitter glyphs

        //        if (SHOW) {
        //            System.out.println("static final UnicodeSet EMOJI_CHARS = new UnicodeSet(\n\"" + Data.DATA_CHARACTERS.toPattern(false) + "\");");
        //            // getUrlCharacters("TWITTER", TWITTER_URL);
        //            // getUrlCharacters("APPLE", APPLE_URL);
        //            System.out.println(new UnicodeSet(Emoji.GITHUB_APPLE_CHARS).removeAll(APPLE_CHARS).toPattern(false));
        //            System.out.println(list(new UnicodeSet(APPLE_CHARS).removeAll(Emoji.GITHUB_APPLE_CHARS)));
        //            System.out.println("Apple: " + APPLE_CHARS);
        //        }
        System.out.println("DONE");
    }

    private static void showConstructedNames() throws IOException {
        try (PrintWriter outText = BagFormatter.openUTF8Writer(Emoji.TR51_INTERNAL_DIR,
                "constructedNames.txt")) {
            for (String s : Iterables.concat(
                    EmojiData.EMOJI_DATA.getChars(), 
                    EmojiData.EMOJI_DATA.getModifierSequences(),
                    EmojiData.EMOJI_DATA.getZwjSequencesNormal()
                    )) {
                int len = s.codePointCount(0, s.length());
                if (len > 1) {
                    outText.println(Utility.hex(s) + " ; " + Emoji.getName(s, false, GenerateEmoji.EXTRA_NAMES));
                }
            }
        }
    }

    //    static Set<Emoji.Source> MAIN_SOURCES  = Collections.unmodifiableSet(EnumSet.of(
    //            Emoji.Source.apple, Emoji.Source.google, Emoji.Source.twitter, Emoji.Source.windows));

    static final UnicodeSet             otherStandard = Emoji.COMMON_ADDITIONS;
    // new UnicodeSet(carriers);
    // static {
    // for (String s : emojiData.getChars()) {
    // String image = getImage(Source.apple, s, false, "");
    // if (image != null) {
    // otherStandard.add(s);
    // }
    // }
    // // HACK for now
    // otherStandard.remove("🖖");
    // otherStandard.removeAll(carriers).freeze();
    // }

    static final UnicodeSet             LEVEL1        = new UnicodeSet(EmojiData.JCARRIERS).addAll(otherStandard).freeze();

    static final UnicodeSet             nc            = new UnicodeSet(EmojiData.EMOJI_DATA.getChars())
    .removeAll(EmojiData.JCARRIERS)
    .removeAll(otherStandard)
    .removeAll(Emoji.FLAGS)
    .freeze();

    //    static final UnicodeSet             nc8           = new UnicodeSet(nc)
    //    .removeAll(new UnicodeSet("[:age=7.0:]"))
    //    .removeAll(nc.strings())
    //    .freeze();
    //
    //    static final UnicodeSet             nc7           = new UnicodeSet(nc)
    //    .removeAll(nc8)
    //    .freeze();

    static final UnicodeSet             otherFlags    = new UnicodeSet(Emoji.FLAGS)
    .removeAll(EmojiData.JCARRIERS).freeze();

    private static void showNewCharacters() throws IOException {
        Set<String> minimal = EmojiData.EMOJI_DATA.getModifierStatusSet(ModifierStatus.modifier_base)
                //                .addAll(emojiData.getModifierStatusSet(ModifierStatus.primary))
                .addAllTo(new TreeSet<String>(EMOJI_COMPARATOR)); // ANNOTATIONS_TO_CHARS_GROUPS.getValues("fitz-secondary");

        // Set<String> newChars =
        // ANNOTATIONS_TO_CHARS.getValues("fitz-minimal");
        PrintWriter out = BagFormatter.openUTF8Writer(Emoji.TR51_INTERNAL_DIR
                , "emoji-count.html");
        writeHeader(out, "Groupings and counts for screenshots and UnicodeJsps", null, "<p>no message</p>\n", "border='1' width='1200pt'", true);
        showRow(out, "Modifier Base", minimal, true);
        //        UnicodeSet modifierBase = new UnicodeSet().addAll(minimal).addAll(optional);
        //        showRow(out, "Modifier_Base", modifierBase.addAllTo(new TreeSet<String>(CODEPOINT_COMPARE)), false);
        final UnicodeSet MODIFIERS = new UnicodeSet("[\\x{1F3FB}-\\x{1F3FF}]").freeze();
        showRow(out, "Modifiers", MODIFIERS.addAllTo(new TreeSet<String>(EMOJI_COMPARATOR)), false);
        showRow(out, "JCarriers", EmojiData.JCARRIERS.addAllTo(new TreeSet<String>(EMOJI_COMPARATOR)), true);
        showRow(out, "Common Additions", otherStandard.addAllTo(new TreeSet<String>(EMOJI_COMPARATOR)), true);
        showRow(out, "Other Flags", otherFlags.addAllTo(new TreeSet<String>(EMOJI_COMPARATOR)), true);
        showRow(out, "Standard Additions", nc.addAllTo(new TreeSet<String>(EMOJI_COMPARATOR)), true);
        //        showRow(out, "Standard Additions8", nc8.addAllTo(new TreeSet<String>(CODEPOINT_COMPARE)), true);

        // for unicodejsps
        UnicodeSet singletons = new UnicodeSet(EmojiData.EMOJI_DATA.getChars()).removeAll(EmojiData.EMOJI_DATA.getChars().strings());
        showRowSet(out, "Singletons", singletons.addAllTo(new TreeSet<String>(EMOJI_COMPARATOR)));
        showRowSet(out, "ZWJ Sequences", EmojiData.EMOJI_DATA.getZwjSequencesNormal().addAllTo(new TreeSet<String>(EMOJI_COMPARATOR)));
        showRowSet(out, "Modifier Sequences", getDiverse(minimal, MODIFIERS).addAllTo(new TreeSet<String>(EMOJI_COMPARATOR)));

        UnicodeSet directional = new UnicodeSet("[☄☝⚰⛏⛴⛵⛷-⛹✈✊-✍✏✒🌀🌂🌊🌬 🍳🍼🍾🎈🎉🎏🎒🎙🎠🎣-🎥🎧🎬🎯🎷🎸🎺🎻🎿🏁-🏄 🏇🏊-🏏🏑-🏓🏳🏴🏷-🏹🐀-🐒🐕-🐘🐛-🐝 🐟-🐣🐦🐧🐩-🐬🐲-🐴🐺🐿-👂👆👇👊-👏👞-👢 👺💃💅💉💘💦💨💪💬💭💺📈📉📌📝📞📡-📣📪-📭 📯📲📹📽🔇-🔊🔌🔦🔨🔪-🔭🕊🖊-🖍🖐🖕🖖🗜 🗡🗣🗯🗿😾🚀-🚅🚈🚋🚌🚎🚐-🚗🚙-🚤🚩🚬🚲 🚴-🚶🚽🚿-🛁🛋🛌🛏🛥🛩🛫🛬🛳🤘🦂-🦄]");
        showRowSet(out, "Directional", directional.addAllTo(new TreeSet<String>(EMOJI_COMPARATOR)));
        //        Set<String> face = EmojiAnnotations.ANNOTATIONS_TO_CHARS.getValues("face");
        //        Set<String> optional_face = new HashSet<>(optional);
        //        optional_face.retainAll(face);
        //        Set<String> optional_other = new HashSet<>(optional);
        //        optional_other.removeAll(face);
        //        showRowSet(out, "Diverse Secondary Face", getDiverse(optional_face, MODIFIERS).addAllTo(new TreeSet<String>(CODEPOINT_COMPARE)));
        //        showRowSet(out, "Diverse Secondary Other", getDiverse(optional_other, MODIFIERS).addAllTo(new TreeSet<String>(CODEPOINT_COMPARE)));
        // UnicodeSet keycapBase = new UnicodeSet();
        // for (String s : Emoji.EMOJI_CHARS.strings()) {
        // if (s.indexOf(Emoji.KEYCAP_MARK) > 0) {
        // keycapBase.add(s.codePointAt(0));
        // }
        // }
        // showRow(out, "KeycapBase", keycapBase.addAllTo(new
        // TreeSet<String>(CODEPOINT_COMPARE)), true);
        //        showRow(out, "RegionalIndicators", Emoji.REGIONAL_INDICATORS.addAllTo(new TreeSet<String>(CODEPOINT_COMPARE)), true);
        writeFooter(out, "");
        out.close();
        // main:
        //
        // for (String emoji : SORTED_EMOJI_CHARS_SET) {
        // for (Source s : MAIN_SOURCES) {
        // File f = getImageFile(s, emoji);
        // if (f != null) {
        // continue main;
        // }
        // }
        // System.out.println(
        // "U+" + Utility.hex(emoji,"&U+")
        // + "\t" + emoji
        // + "\t" + getName(emoji)
        // + "\thttp://unicode.org/Public/emoji/1.0/full-emoji-list.html#"
        // + Utility.hex(emoji,"_")
        // );
        // }
    }

    private static UnicodeSet getDiverse(Set<String> minimal, final UnicodeSet MODIFIERS) {
        UnicodeSet primaryDiverse = new UnicodeSet();
        for (String item : minimal) {
            for (String tone : MODIFIERS) {
                primaryDiverse.add(item + tone);
            }
        }
        return primaryDiverse.freeze();
    }

    /** Main Chart */
    private static void showTextStyle() throws IOException {
        UnicodeSet defaultText = new UnicodeSet();
        for (String chars2 : EmojiData.EMOJI_DATA.getChars()) {
            if (Style.valueOf(EmojiData.EMOJI_DATA.getData(chars2).style) == Style.text) {
                defaultText.add(chars2);
            }
        }
        //        if (!DEFAULT_TEXT_STYLE.equals(defaultText)) {
        //            throw new IllegalArgumentException(new UnicodeSet(defaultText).removeAll(DEFAULT_TEXT_STYLE)
        //                    + ", "
        //                    + new UnicodeSet(DEFAULT_TEXT_STYLE).removeAll(defaultText));
        //        }
        defaultText.freeze();
        PrintWriter out = BagFormatter.openUTF8Writer(Emoji.CHARTS_DIR, "text-style.html");
        PrintWriter out2 = BagFormatter.openUTF8Writer(Emoji.TR51_INTERNAL_DIR
                , "text-vs.txt");
        writeHeader(out, "Text vs Emoji", null, "<p>This chart shows the default display style (text vs emoji) by version. "
                + "It does not include emoji sequences. "
                + "The 'Dings' include Dingbats, Webdings, and Wingdings. "
                + "The label V1.1 ⊖ Dings indicates those characters (except for Dings) that are in Unicode version 1.1. "
                + "The lable V1.1 ∩ Dings indicates those Ding characters that are in Unicode version 1.1.</p>\n", "border='1'", true);
        out.println("<tr><th>Version</th>"
                + "<th width='25%'>Default Text Style; no VS in U8.0</th>"
                + "<th width='25%'>Default Text Style; has VSs</th>"
                + "<th width='25%'>Default Emoji Style; no VS in U8.0</th>"
                + "<th width='25%'>Default Emoji Style; has VSs</th>"
                + "</tr>");
        UnicodeSet dings = new UnicodeSet(DINGBATS)
        .addAll(DING_MAP.keySet())
        .retainAll(EmojiData.EMOJI_DATA.getSingletonsWithoutDefectives()).freeze();
        for (String version : new TreeSet<String>(VERSION.values())) {
            UnicodeSet current = VERSION.getSet(version).retainAll(EmojiData.EMOJI_DATA.getSingletonsWithoutDefectives());
            if (current.size() == 0) {
                continue;
            }
            UnicodeSet currentDings = new UnicodeSet(current).retainAll(dings);
            current.removeAll(dings);
            String versionString = version.replace("_", ".");
            showTextRow(out, versionString, true, current, defaultText, out2);
            showTextRow(out, versionString, false, currentDings, defaultText, out2);
        }
        writeFooter(out, "");
        out.close();
        out2.close();
    }

    private static void showTextRow(PrintWriter out, String version, boolean minusDings, UnicodeSet current, UnicodeSet defaultText,
            PrintWriter out2) {
        if (current.size() == 0) {
            return;
        }
        String title = version + (minusDings ? " <span style='color:gray'>⊖ Dings</span>" : " ∩ Dings");
        UnicodeSet emojiSet = new UnicodeSet(current).removeAll(defaultText).removeAll(Emoji.HAS_EMOJI_VS);
        UnicodeSet emojiSetVs = new UnicodeSet(current).removeAll(defaultText).retainAll(Emoji.HAS_EMOJI_VS);
        UnicodeSet textSet = new UnicodeSet(current).retainAll(defaultText).removeAll(Emoji.HAS_EMOJI_VS);
        UnicodeSet textSetVs = new UnicodeSet(current).retainAll(defaultText).retainAll(Emoji.HAS_EMOJI_VS);
        out.print("<tr><th>" + title + "</th><td>");
        getImages(out, textSet, true);
        out.print("</td><td>");
        getImages(out, textSetVs, true);
        out.print("</td><td>");
        getImages(out, emojiSet, true);
        out.print("</td><td>");
        getImages(out, emojiSetVs, true);
        out.print("</td></tr>");
        if (textSet.isEmpty()) {
            return;
        }
        out2.println("\n#\t" + version + (minusDings ? " ⊖ Dings" : " ∩ Dings") + "\n");
        for (String s : textSet) {
            // 2764 FE0E; text style; # HEAVY BLACK HEART
            // 2764 FE0F; emoji style; # HEAVY BLACK HEART
            out2.println(Utility.hex(s, " ") + " FE0E; text style;   # " + UCharacter.getName(s, "+"));
            out2.println(Utility.hex(s, " ") + " FE0F; emoji style;  # " + UCharacter.getName(s, "+"));
        }
    }

    private static void getImages(PrintWriter out, UnicodeSet textSet, boolean useDataUrl) {
        String link = "full-emoji-list.html";
        for (String emoji : textSet.addAllTo(new TreeSet<String>(EMOJI_COMPARATOR))) {
            if (link != null) {
                out.print("<a href='" + link + "#" + Emoji.buildFileName(emoji, "_") + "' target='full'>");
            }
            out.print(getBestImage(emoji, useDataUrl, ""));
            if (link != null) {
                out.print("</a>");
            }
            out.print(" ");
        }
    }

    private static void showRow(PrintWriter out, String title, Set<String> minimal, boolean abbreviate) {
        showRowSet(out, title, minimal);
        out.print("<tr><td colSpan='2'>");
        showExplicitAppleImages(out, minimal);
        //        out.print("</td>\n<td width='10%'>");
        //        showNames(out, minimal, abbreviate);
        out.println("</td><tr>");
    }

    private static void showRowSet(PrintWriter out, String title, Set<String> minimal) {
        out.println("<tr><th width='10em'>" + minimal.size() + "</th><th>" + title + "</th></tr>");
        out.println("<tr><td colSpan='2'>" + CollectionUtilities.join(minimal, " ") + "</td></tr>");
        out.println("<tr><td colSpan='2'>" + new UnicodeSet().addAll(minimal).toPattern(false) + "</td></tr>");
    }

    private static void showNames(PrintWriter out, Set<String> minimal, boolean abbreviate) {
        UnicodeSet us = new UnicodeSet().addAll(minimal);
        for (EntryRange r : us.ranges()) {
            out.print(getCodeAndName2(UTF16.valueOf(r.codepoint), false));
            if (r.codepoint != r.codepointEnd) {
                if (abbreviate) {
                    out.print("<br>\n…" + getCodeAndName2(UTF16.valueOf(r.codepointEnd), false));
                } else {
                    for (int cp = r.codepoint + 1; cp <= r.codepointEnd; ++cp) {
                        out.print("<br>\n" + getCodeAndName2(UTF16.valueOf(cp), false));
                    }
                }
            }
            out.println("<br>");
        }
        for (String s : us.strings()) {
            out.println(getCodeAndName2(s, false) + "<br>");
        }
    }

    private static String getCodeAndName2(String s, boolean toLower) {
        return toUHex(s) + " " + Emoji.getName(s, toLower, GenerateEmoji.EXTRA_NAMES);
    }

    private static String toUHex(String s) {
        return "U+" + Utility.hex(s, " U+");
    }

    private static void showExplicitAppleImages(PrintWriter out, Set<String> minimal) {
        for (String emoji : minimal) {
            out.println(getBestImage(emoji, false, ""));
            // out.println("<img height=\"24\" width=\"auto\" alt=\""
            // + emoji
            // + "\" src=\"images/apple/apple_"
            // + Utility.hex(emoji, "_").toLowerCase(Locale.ENGLISH) // emoji
            // + ".png\" title=\"" + getCodeAndName(emoji, " ") + "\"> ");
        }
    }



    //    public static void addFileCodepoints(File imagesOutputDir, Map<String, Data> results) {
    //        for (File file : imagesOutputDir.listFiles()) {
    //            String fileName = file.getName();
    //            if (file.isDirectory()) {
    //                if (!fileName.equals("other") && !fileName.equals("proposed") && !fileName.equals("sample")) {
    //                    addFileCodepoints(file, results);
    //                }
    //                continue;
    //            }
    //            String s = fileName;
    //            String original = s;
    //            if (s.startsWith(".") || !s.endsWith(".png") || s.contains("emoji-palette")
    //                    || s.contains("_200d")) { // ZWJ from new combos
    //                continue;
    //            }
    //            String chars = Emoji.parseFileName(true, s);
    //            if (chars.isEmpty()) { // resulting from _x codepoints
    //                continue;
    //            }
    //            //checkNewItems(chars, fileName);
    //        }
    //    }

    //this.age = UcdPropertyValues.Age_Values.valueOf(age.replace('.', '_'));

    static final UnicodeSet WINDOWS_CHARS = new UnicodeSet();

    private static String extractCodes(String s, String prefix, UnicodeSet chars) {
        if (!s.startsWith(prefix)) {
            return null;
        }
        String[] parts = s.substring(prefix.length()).split("[-_]");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            int codePoint = Integer.parseInt(part, 16);
            if (codePoint >= 0xF0000) {
                return "";
            }
            result.appendCodePoint(codePoint);
        }
        String stringResult = result.toString();
        if (chars != null) {
            chars.add(stringResult);
        }
        return stringResult;
    }

    private static String list(UnicodeSet uset) {
        return CollectionUtilities.join(With.in(uset).toList(), " ");
    }

    public static void getUrlCharacters(String title, Transform<String, String> transform) {
        // http://emojistatic.github.io/images/32/1f4ab.png
        UnicodeSet twitterChars = new UnicodeSet();
        int limit = 0;
        for (String s : EmojiData.EMOJI_DATA.getChars()) {
            String twitterUrl = transform.transform(s);
            if (getResponseCode(twitterUrl) == 200) {
                twitterChars.add(s);
            }
            if ((limit++ % 50) == 0) {
                System.out.println(limit + "\t" + s);
            }
        }
        System.out.println("static final UnicodeSet " +
                title +
                "_CHARS = new UnicodeSet(\n\""
                + twitterChars.toPattern(false) + "\");");
    }

    static final UnicodeSet APPLE_CHARS   = new UnicodeSet(
            "[©®‼⁉™ℹ↔-↙↩↪⌚⌛⏩-⏬⏰⏳Ⓜ▪▫▶◀◻-◾☀☁☎☑☔☕☝☺♈-♓♠♣♥♦♨♻♿⚓⚠⚡⚪⚫⚽⚾⛄⛅⛎⛔⛪⛲⛳⛵⛺⛽✂✅✈-✌✏✒✔✖✨✳✴❄❇❌❎❓-❕❗❤➕-➗➡➰➿⤴⤵⬅-⬇⬛⬜⭐⭕〰〽㊗㊙🀀-🀫🀰-🂓🂠-🂮🂱-🂿🃁-🃏🃑-🃵🅰🅱🅾🅿🆊🆎🆏🆑-🆚🇦-🇿🈁🈂🈚🈯🈲-🈺🉐🉑🌀-🌬🌰-🍽🎀-🏎🏔-🏷🐀-📾🔀-🔿🕊🕐-🕹🕻-🖣🖥-🙂🙅-🙏🙬-🙯🚀-🛏🛠-🛬🛰-🛳{#⃣}{0⃣}{1⃣}{2⃣}{3⃣}{4⃣}{5⃣}{6⃣}{7⃣}{8⃣}{9⃣}{🇨🇳}{🇩🇪}{🇪🇸}{🇫🇷}{🇬🇧}{🇮🇹}{🇯🇵}{🇰🇷}{🇷🇺}{🇺🇸}]");

    static final UnicodeSet TWITTER_CHARS = new UnicodeSet(
            "[©®‼⁉™ℹ↔-↙↩↪⌚⌛⏩-⏬⏰⏳Ⓜ▪▫▶◀◻-◾☀☁☎☑☔☕☝☺♈-♓♠♣♥♦♨♻♿⚓⚠⚡⚪⚫⚽⚾⛄⛅⛎⛔⛪⛲⛳⛵⛺⛽✂✅✈-✌✏✒✔✖✨✳✴❄❇❌❎❓-❕❗❤➕-➗➡➰➿⤴⤵⬅-⬇⬛⬜⭐⭕〰〽㊗㊙🀄🃏🅰🅱🅾🅿🆎🆑-🆚🇦-🇿🈁🈂🈚🈯🈲-🈺🉐🉑🌀-🌠🌰-🌵🌷-🍼🎀-🎓🎠-🏄🏆-🏊🏠-🏰🐀-🐾👀👂-📷📹-📼🔀-🔽🕐-🕧🗻-🙀🙅-🙏🚀-🛅{#⃣}{0⃣}{1⃣}{2⃣}{3⃣}{4⃣}{5⃣}{6⃣}{7⃣}{8⃣}{9⃣}{🇨🇳}{🇩🇪}{🇪🇸}{🇫🇷}{🇬🇧}{🇮🇹}{🇯🇵}{🇰🇷}{🇷🇺}{🇺🇸}]");

    private static UnicodeSet getStrings(UnicodeSet us) {
        UnicodeSet result = new UnicodeSet();
        for (String s : us) {
            if (Character.charCount(s.codePointAt(0)) != s.length()) {
                result.add(s);
            }
        }
        return result;
    }

    private static void showLabels() throws IOException {
        PrintWriter out = BagFormatter.openUTF8Writer(Emoji.TR51_INTERNAL_DIR
                , "emoji-labels.html");
        writeHeader(out, "Emoji Labels", null, "<p>Main categories for character picking. " +
                "Characters may occur more than once. " +
                "Categories could be grouped in the UI.</p>\n", "border='1'", true);
        for (Entry<Label, Set<String>> entry : Label.CHARS_TO_LABELS.valueKeysSet()) {
            Label label = entry.getKey();
            Set<String> set = entry.getValue();
            String word = label.toString();
            Set<String> values = entry.getValue();
            UnicodeSet uset = new UnicodeSet().addAll(values);

            displayUnicodesetTD(out, Collections.singleton(word), null, Collections.<String> emptySet(), uset, Style.bestImage, 16, null);
        }
        writeFooter(out, "");
        out.close();
    }

    /** Main Chart 
     * @param internalForCopy TODO
     * @param showUca TODO*/
    private static void showOrdering(Style style, boolean internalForCopy, boolean showUca) throws IOException {
        PrintWriter out = BagFormatter.openUTF8Writer(internalForCopy ? Emoji.TR51_INTERNAL_DIR : Emoji.CHARTS_DIR,
                (style == Style.bestImage ? "" : "ref-") + "emoji-ordering.html");
        writeHeader(out, "Emoji Ordering", null, "<p>This chart shows the default ordering of emoji characters from " + CLDR_DATA_LINK + ". "
                + "This is designed to improve on the <a target='uca' href='http://unicode.org/charts/collation/'>UCA</a> orderings"
                + (showUca ? " (shown at the right)" : "")
                + ", by grouping similar items together. " +
                "The cell divisions "
                + (showUca ? "for Emoji Ordering " : "")
                + "indicate the rough categories that are used to organize related characters together. "
                + "The categories are rough, and any character can fit in multiple categories: "
                + "<i>they may change at any time, and should not be used in production.</i> "
                + "The <a target='style' href='emoji-sequences.html#modifier_sequences'>320 modifier sequences</a> are omitted, "
                + "because they are simply ordered after their emoji modifier bases. "
                + (showUca ? "The cell divisions for the Default Unicode Collation Order are Unicode code-chart blocks. " : "")
                + "To make suggestions for improvements, please file a " + getCldrTicket("collation", "Emoji ordering suggestions") + ".</p>\n", "border='1'", true);

        final Set<Entry<String, Set<String>>> keyValuesSet = EmojiOrder.STD_ORDER.orderingToCharacters.keyValuesSet();
        final int rows = keyValuesSet.size();
        UnicodeSet all = new UnicodeSet();
        boolean first = true;
        final int charsPerRow = -1;
        int totalSorted = 0;
        if (showUca) {
            out.println("<tr>"
                    + "<th width='49%'>Emoji Ordering</th>"
                    + "<th rowSpan='" + (rows + 1)*2 + "'>&nbsp;</th>"
                    //                + "<th width='33%'>With Chart Glyphs</th>"
                    //                + "<th rowSpan='" + (rows + 1) + "'>&nbsp;</th>"
                    + "<th width='49%'>Default Unicode Collation Order</th>"
                    + "</tr>");
            out.println("<tr><td><table>");
        }
        MajorGroup lastMajorGroup = null;
        for (Entry<String, Set<String>> entry : keyValuesSet) {
            final UnicodeSet values = new UnicodeSet().addAll(entry.getValue());
            values.removeAll(EmojiData.EMOJI_DATA.getModifierSequences())
            .retainAll(EmojiData.EMOJI_DATA.getAllEmojiWithoutDefectives())
            .freeze();
            if (values.isEmpty()) {
                continue;
            }
            MajorGroup majorGroup = EmojiOrder.STD_ORDER.getMajorGroup(values);
            if (majorGroup != lastMajorGroup) {
                lastMajorGroup = majorGroup;
                out.println("<tr><th class='bighead'>" + getDoubleLink(majorGroup.toString()) + "</th></tr>");
            }
            out.println("<tr><th>" + getDoubleLink(entry.getKey()) + "</th></tr>");
            out.println("<tr>");
            all.addAll(values);
            totalSorted += values.size();
            displayUnicodeSet(out, values, Style.bestImage, charsPerRow, 1, 1, null, EMOJI_COMPARATOR, !internalForCopy);
            // displayUnicodeSet(out, values, Style.refImage, charsPerRow, 1, 1, null, CODEPOINT_COMPARE);
            if (first) {
                first = false;
            }
            out.println("</tr>");
        }
        System.out.println(totalSorted);
        if (showUca) {
            out.println("</table></td>");
            final UnicodeMap<Block_Values>        blocks           = Emoji.LATEST.loadEnum(UcdProperty.Block, UcdPropertyValues.Block_Values.class);
            TreeSet<String> ucaOrder = new TreeSet<>(EmojiOrder.UCA_COLLATOR);
            all.addAllTo(ucaOrder);
            Block_Values lastBlock = null;
            UnicodeSet current = null;
            LinkedHashSet<Pair<Block_Values,UnicodeSet>> pairs = new LinkedHashSet<>();
            for (String s : all) {
                Block_Values thisBlock = blocks.get(s.codePointAt(0));
                if (thisBlock != lastBlock) {
                    if (current != null) {
                        current.freeze();
                    }
                    lastBlock = thisBlock;
                    pairs.add(Pair.of(thisBlock, current = new UnicodeSet()));
                }
                current.add(s);
            }

            out.println("<td><table>");
            for (Pair<Block_Values, UnicodeSet>  pair : pairs) {
                out.println("<tr><th>" + TransliteratorUtilities.toHTML.transform(pair.getFirst().toString()) + "</th></tr>");
                out.println("<tr>");
                displayUnicodeSet(out, pair.getSecond(), Style.bestImage, charsPerRow, 1, 1, null, EmojiOrder.UCA_COLLATOR, true);
                out.println("</tr>");
            }
            out.println("</table></td></tr>");
        }
        writeFooter(out, "");
        out.close();
    }

    static final String UTR_LINK = "<em><a target='doc' href='http://www.unicode.org/reports/tr51/index.html'>UTR #51 Unicode Emoji</a></em>";

    /** Main Chart */
    private static void showDefaultStyle() throws IOException {
        PrintWriter out = BagFormatter.openUTF8Writer(Emoji.CHARTS_DIR, "emoji-style.html");
        PrintWriter outText = BagFormatter.openUTF8Writer(Emoji.CHARTS_DIR, "emoji-style.txt");
        writeHeader(out, "Emoji Default Style Values", null, "<p>This chart provides a listing of characters for testing the display of emoji characters. "
                + "Unlike the other charts, the emoji are presented as text rather than images, to show the style supplied in your browser. "
                + "The text is given a gray color, so as to help distinguish the emoji presentation from a plain text presentation. "
                + "</p><ul><li>"
                + "The Plain subtable shows a listing of all emoji characters and sequences with neither an emoji font nor variation selectors. "
                + "The characters with a default “text” display are separated out. "
                + "</li><li>"
                + "The Emoji subtable lists the same characters but with common color fonts on the text, to show a colorful display if possible. "
                + "</li><li>"
                + "The Variation subtables adds the corresponding variation selectors. "
                + "It does not yet, however, add the variation sequences included in Unicode 9.0. "
                + "See <a target='variants' href='emoji-variants.html'>Emoji Variation Sequences</a>."
                + "</ul>\n"
                + "<p>The default presentation choice (colorful vs gray) is discussed in "
                + "<a href='http://www.unicode.org/reports/tr51/index.html#Presentation_Style'>Presentation Style</a>.</p>\n", "border='1'", true);

        out.println("<tr><th colSpan='3'>Plain  (“text” should be all gray; others should all be colorful)</th></tr>");
        outText.println("\uFEFFThis text file provides a listing of characters for testing the display of emoji characters. ");
        outText.println("\nPlain  (“text” should be all gray; others should all be colorful)");
        for (Entry<Style, Set<String>> entry : STYLE_TO_CHARS.keyValuesSet()) {
            UnicodeSet plain = new UnicodeSet().addAll(entry.getValue());
            final Set<String> singletonWord = Collections.singleton(entry.getKey().toString());
            displayUnicodesetTD(out, singletonWord, null, Collections.<String> emptySet(), plain, Style.plain, -1, null);
            outText.println("\n" + entry.getKey());
            outText.println(textList(plain));
        }
        displayUnicodesetTD(out, Collections.singleton("modifier"), null, Collections.<String> emptySet(), EmojiData.EMOJI_DATA.getModifierSequences(), Style.plain, -1, null);
        outText.println("\nmodifier");
        outText.println(textList(EmojiData.EMOJI_DATA.getModifierSequences()));
        displayUnicodesetTD(out, Collections.singleton("zwj"), null, Collections.<String> emptySet(), EmojiData.EMOJI_DATA.getZwjSequencesNormal(), Style.plain, -1, null);
        outText.println("\nzwj");
        outText.println(textList(EmojiData.EMOJI_DATA.getZwjSequencesNormal()));

        out.println("<tr><th colSpan='3'>Emoji Font (should all be colorful)</th></tr>");
        for (Entry<Style, Set<String>> entry : STYLE_TO_CHARS.keyValuesSet()) {
            Set<String> values = entry.getValue();
            final Set<String> singletonWord = Collections.singleton(entry.getKey().toString());
            displayUnicodesetTD(out, singletonWord, null, Collections.<String> emptySet(), new UnicodeSet().addAll(values), Style.emojiFont, -1, null);
        }
        displayUnicodesetTD(out, Collections.singleton("modifier"), null, Collections.<String> emptySet(), EmojiData.EMOJI_DATA.getModifierSequences(), Style.emojiFont, -1, null);
        displayUnicodesetTD(out, Collections.singleton("zwj"), null, Collections.<String> emptySet(), EmojiData.EMOJI_DATA.getZwjSequencesNormal(), Style.emojiFont, -1, null);

        out.println("<tr><th colSpan='3'>Emoji Variation Sequences (should all be colorful)</th></tr>");
        outText.println("\nEmoji Variation Sequences (should all be colorful)");
        for (Entry<Style, Set<String>> entry : STYLE_TO_CHARS.keyValuesSet()) {
            UnicodeSet emoji = new UnicodeSet();
            for (String value : entry.getValue()) {
                String emojiStyle = Emoji.getEmojiVariant(value, Emoji.EMOJI_VARIANT_STRING);
                if (!value.equals(emojiStyle)) {
                    emoji.add(emojiStyle);
                }
            }
            final Set<String> singletonWord = Collections.singleton(entry.getKey().toString());
            displayUnicodesetTD(out, singletonWord, null, Collections.<String> emptySet(), emoji, Style.emoji, -1, null);
            outText.println("\n" + entry.getKey());
            outText.println(textList(emoji));
        }
        out.println("<tr><th colSpan='3'>Text Variation Sequences (should all be gray)</th></tr>");
        outText.println("\nText Variation Sequences (should all be black)");
        for (Entry<Style, Set<String>> entry : STYLE_TO_CHARS.keyValuesSet()) {
            UnicodeSet text = new UnicodeSet();
            for (String value : entry.getValue()) {
                String textStyle = Emoji.getEmojiVariant(value, Emoji.TEXT_VARIANT_STRING);
                if (!value.equals(textStyle)) {
                    text.add(textStyle);
                }
            }
            final Set<String> singletonWord = Collections.singleton(entry.getKey().toString());
            displayUnicodesetTD(out, singletonWord, null, Collections.<String> emptySet(), text, Style.text, -1, null);
            outText.println("\n" + entry.getKey());
            outText.println(textList(text));
        }

        writeFooter(out, "");
        out.close();
        outText.close();
    }

    private static String textList(UnicodeSet plain) {
        StringBuilder result = new StringBuilder();
        Set<String> sorted = plain.addAllTo(new TreeSet<String>(EMOJI_COMPARATOR));
        int count = -1;
        for (String s : sorted) {
            ++count;
            if (count > 30) {
                result.append('\n');
                count = 0;
            } else if (count > 0) {
                result.append(' ');
            }
            result.append(s);
        }
        return result.toString();
    }

    private static void showVariationSequences() throws IOException {
        PrintWriter out = BagFormatter.openUTF8Writer(Emoji.CHARTS_DIR, "emoji-variants.html");
        writeHeader(out, "Emoji Variation Sequences", "", "<p>This chart specifies the exact list of Unicode standardized variation sequences known "
                + "as as <i>emoji variation sequences</i>. "
                + "These sequences consist of an <i>emoji base</i> followed either by the "
                + "variation selector U+FE0E or the variation selector U+FE0F. "
                + "Sample glyphs are provided to illustrate the contrast between "
                + "the desired appearances for each of these selectors. "
                + "For more information, see "
                + "<em><a target='doc' href='http://www.unicode.org/reports/tr51#Definitions'>Definitions</a></em> "
                + "in <em><a target='doc' href='http://www.unicode.org/reports/tr51'>UTR #51 Unicode Emoji</a></em>. "
                + "</p><p>"
                + "The Version column shows the first version of Unicode containing "
                + "the standard variation sequence. "
                + "It is not the version in which the <i>emoji base</i> was added to Unicode, "
                + "but rather the one in which the <i>emoji variation sequence</i> was first defined. "
                + "Note that a version identified as “9.0” marks those sequences that "
                + "will be included in Unicode 9.0, which is to be scheduled for release in June 2016. "
                + "Keycap images (those with a * on the Name) are for sequences "
                + "followed by U+20E3 COMBINING ENCLOSING KEYCAP. </p>\n", "border='1'", true);

        UnicodeSet x = Emoji.HAS_EMOJI_VS; 
        out.println("<tr><th class='cchars'>Code</th>"
                + "<th class='narrow cchars'>Sample Text (+FE0E)</th>"
                + "<th class='narrow cchars'>Sample Emoji (+FE0F)</th>"
                + "<th>Version</th>"
                + "<th>Name</th></tr>");
        final String keycapIndicator = "*";
        TreeSet<String> sorted = new TreeSet<>(EmojiOrder.PLAIN_STRING_COMPARATOR);
        for (String cp : TO_FIRST_VERSION_FOR_VARIANT.keySet().addAllTo(sorted)) {
            String version = TO_FIRST_VERSION_FOR_VARIANT.get(cp);
            out.println("<tr>");
            out.println("<td class='code cchars'>" + getDoubleLink(Utility.hex(cp)) + "</td>");
            if (EmojiData.EMOJI_DATA.getKeycapBases().contains(cp)) {
                // keycaps, treat specially
                String cp2 = cp + Emoji.KEYCAP_MARK;
                out.println(GenerateEmoji.getCell(Emoji.Source.ref, cp2, "andr"));
                out.println(GenerateEmoji.getCell(null, cp2, "andr"));
                out.println("<td>" + version + "</td>");
                out.println("<td>" + Emoji.getName(cp, false, GenerateEmoji.EXTRA_NAMES) + keycapIndicator + "</td>");
            } else {
                out.println(GenerateEmoji.getCell(Emoji.Source.ref, cp, "andr"));
                out.println(GenerateEmoji.getCell(null, cp, "andr"));
                out.println("<td>" + version + "</td>");
                out.println("<td>" + Emoji.getName(cp, false, GenerateEmoji.EXTRA_NAMES) + "</td>");
            }
            out.println("</tr>");
        }
        writeFooter(out, "");
        out.close();
    }

    /** Main Chart */
    private static void showSequences() throws IOException {
        try (PrintWriter out = BagFormatter.openUTF8Writer(Emoji.CHARTS_DIR, "emoji-sequences.html")) {
            writeHeader(out, "Emoji Sequences", null, "<p>This chart provides a list of sequences of emoji characters, "
                    + "including <a href='#keycaps'>keycap sequences</a>, "
                    + "<a href='#flags'>flags</a>, and "
                    + "<a href='#modifier_sequences'>modifier sequences</a>. "
                    + "For variation sequences, see <a href='emoji-style.html'>Emoji Default Style Values</a>. "
                    + "For a catalog of emoji zwj sequences, see <a href='emoji-zwj-sequences.html'>Emoji ZWJ Sequences Catalog</a>. </p>\n", "border='1'", true);

            displayUnicodesetTD(out, Collections.singleton("keycaps"), null, Collections.singleton(""+Emoji.KEYCAPS.size()), Emoji.KEYCAPS, Style.bestImage, -1, null);
            displayUnicodesetTD(out, Collections.singleton("flags"), null, Collections.singleton(""+Emoji.FLAGS.size()), Emoji.FLAGS, Style.bestImage, -1, null);
            displayUnicodesetTD(out, Collections.singleton("modifier sequences"), null, Collections.singleton(""+EmojiData.EMOJI_DATA.getModifierSequences().size()), EmojiData.EMOJI_DATA.getModifierSequences(), Style.bestImage, -1, null);

            writeFooter(out, "");
        }

        try (PrintWriter out = BagFormatter.openUTF8Writer(Emoji.CHARTS_DIR, "emoji-zwj-sequences.html")) {
            writeHeader(out, "Emoji ZWJ Sequences Catalog", null, "<p>For interoperability, this page catalogs emoji zwj sequences that are supported on at least one commonly available platform, "
                    + "so that other vendors can choose whether or not to support them as well. "
                    + "</p><p>The U+200D ZERO WIDTH JOINER (ZWJ) can be used between the elements of a sequence of characters to indicate that a single glyph should be presented if available. "
                    + "An implementation may use this mechanism to handle such an emoji zwj sequence as a single glyph, "
                    + "with a palette or keyboard that generates the appropriate sequences for the glyphs shown. "
                    + "So to the user, these would behave like single emoji characters, even though internally they are sequences. "
                    + "</p><p>When an emoji zwj sequence is sent to a system that does not have a corresponding single glyph, "
                    + "the ZWJ characters would be ignored and a fallback sequence of separate emoji would be displayed. "
                    + "Thus an emoji zwj sequence should only be supported where the fallback sequence would also make sense to a recipient. "
                    + "Note that the emoji variation selector (U+FE0F) is not required in the sequence, per UTR #51, Unicode Emoji, "
                    + "when there is at least one character in the sequence with a default emoji presentation. </p>\n", "border='1'", true);
            displayZwjTD(out, "ZWJ sequences", new UnicodeSet("[💏💑👪]").addAll(EmojiData.EMOJI_DATA.getZwjSequencesNormal()));
            //displayZwjTD(out, "ZWJ sequences: No VS", Emoji.APPLE_COMBOS_WITHOUT_VS);

            writeFooter(out, "");
        }
    }

    private static void displayZwjTD(PrintWriter out, String title, UnicodeSet items) {
        out.println("<tr><th colSpan='6'>" + TransliteratorUtilities.toHTML.transform(title) + "</th></tr>");
        out.println("<tr><th>Count</th><th style='width:10em'>Code Points</th><th>Browser</th><th>Sample Image</th><th>Sample Fallback Images</th><th>Description</th></tr>");
        //        StringBuffer name = new StringBuffer();
        //        String prefix = "";
        int i = 0;
        StringBuilder buffer = new StringBuilder();
        for (String s : EmojiOrder.sort(EMOJI_COMPARATOR, items)) {
            boolean isSingle = s.codePointCount(0, s.length()) == 1;
            out.println("<tr>");
            out.println("<td class='rchars'>" + (isSingle ? "<i>neutral</i>" : ""+(++i)) + "</td>");
            out.println("<td>U+" + Utility.hex(s, " U+") + "</td>");
            out.println("<td class='chars'>‍" + s + "</td>");
            final String bestImage = getBestImage(s, true, "");
            out.println("<td class='andr'>" + bestImage + "</td>");
            if (buffer.length() != 0) {
                buffer.append(isSingle || s.startsWith("👁")? "<br>" : " ");
            }
            buffer.append(bestImage);
            out.println("<td class='andr'>");
            //            name.setLength(0);
            for (int item : CharSequences.codePoints(s)) {
                if (Emoji.EMOJI_CHARS.contains(item)) {
                    String s2 = UTF16.valueOf(item);
                    out.println(getBestImage(s2, true, ""));
                    //                    if (name.length() != 0) {
                    //                        name.append();
                    //                    }
                    //                    name.append(getName(s2, true));
                }
            }
            out.println("</td>");
            //            if (isSingle) {
            //                prefix = name + ": <i>";
            //            } else {
            //                name.insert(0, prefix);
            //                name.append("</i>");
            //            }
            out.println("<td>" + Emoji.getName(s, true, GenerateEmoji.EXTRA_NAMES) + "</td>");
            out.println("</tr>");
        }
        out.println("<tr><th colSpan='6'>Character list for copying</th></tr>");
        out.println("<tr><td colSpan='6'>" + buffer
                + "</td></tr>");
    }

    static final UnicodeSet ARIB     = new UnicodeSet(
            "[²³¼-¾࿖‼⁉ℓ№℡℻⅐-⅛Ⅰ-Ⅻ↉ ①-⑿⒈-⒓ⒹⓈ⓫⓬▶◀☀-☃☎☓☔☖☗♠ ♣♥♦♨♬⚓⚞⚟⚡⚾⚿⛄-⛿✈❶-❿➡⟐⨀ ⬅-⬇⬛⬤⬮⬯〒〖〗〶㈪-㈳㈶㈷㈹㉄-㉏㉑-㉛ ㊋㊙�㍱㍻-㍾㎏㎐㎝㎞㎠-㎢㎤㎥㏊円年日月 🄀-🄊🄐-🄭🄱🄽🄿🅂🅆🅊-🅏🅗🅟🅹🅻🅼🅿🆊-🆍 🈀🈐-🈰🉀-🉈]")
    .freeze();
    static final UnicodeSet DINGBATS = new UnicodeSet(
            "[\u2194\u2195\u260E\u261B\u261E\u2660\u2663\u2665\u2666\u2701-\u2704\u2706-\u2709\u270C-\u2712\u2714-\u2718\u2733\u2734\u2744\u2747\u2762-\u2767\u27A1]")
    .freeze();

    static class VersionData implements Comparable<VersionData> {
        final Age_Values      versionInfo;
        final Set<Emoji.CharSource> setCharSource;

        public VersionData(String s) {
            this.versionInfo = Emoji.getNewest(s);
            this.setCharSource = Collections.unmodifiableSet(GenerateEmoji.getCharSources(s));
        }

        static final CollectionOfComparablesComparator ccc = new CollectionOfComparablesComparator();

        @Override
        public int hashCode() {
            return versionInfo.hashCode() * 37 ^ setCharSource.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return compareTo((VersionData) obj) == 0;
        }

        @Override
        public int compareTo(VersionData o) {
            Comparator foo;
            return ComparisonChain.start()
                    .compare(setCharSource, o.setCharSource, ccc)
                    .compare(versionInfo, o.versionInfo)
                    .result();
        }

        @Override
        public String toString() {
            return getCharSources() + "/" + getVersion();
        }

        public String getVersion() {
            return showVersion(versionInfo);
        }

        public String getCharSources() {
            return CollectionUtilities.join(setCharSource, "+");
        }
    }

    /** Main Chart */
    private static void showVersions() throws IOException {
        PrintWriter out = BagFormatter.openUTF8Writer(Emoji.CHARTS_DIR, "emoji-versions-sources.html");
        writeHeader(out, "Emoji Versions &amp; Sources", null, "<p>This chart shows when each emoji code point first appeared in a Unicode version, "
                + "and which "
                + "sources the character corresponds to. It does not include the emoji sequences. "
                + "For example, “ZDings+ARIB+JCarrier” indicates that the character also appears in the Zapf Dingbats, "
                + "the ARIB set, and the Japanese Carrier set. </p>\n", "border='1'", true);
        UnicodeMap<VersionData> m = new UnicodeMap<>();
        TreeSet<VersionData> sorted = getSortedVersionInfo(m);
        for (VersionData value : sorted) {
            UnicodeSet chars = m.getSet(value);
            displayUnicodesetTD(out, 
                    Collections.singleton(value.getCharSources()), 
                    null,
                    ImmutableSet.of(showVersionOnly(value.versionInfo), String.valueOf(VersionToAge.getYear(value.versionInfo)), String.valueOf(chars.size())), 
                    chars, Style.bestImage, -1, null);
        }
        writeFooter(out, "");
        out.close();
    }

    public static TreeSet<VersionData> getSortedVersionInfo(
            UnicodeMap<VersionData> m) {
        for (String s : EmojiData.EMOJI_DATA.getSingletonsWithoutDefectives()) {
            m.put(s, new VersionData(s));
        }
        TreeSet<VersionData> sorted = new TreeSet<>(m.values());
        return sorted;
    }

    /** Main Chart */
    private static void showVersionsOnly() throws IOException {
        PrintWriter out = BagFormatter.openUTF8Writer(Emoji.CHARTS_DIR, "emoji-versions.html");
        writeHeader(out, "Emoji Versions", null, "<p>This chart shows when each emoji character first appeared in a Unicode version.</p>\n", "border='1'", true);
        UnicodeMap<Age_Values> m = new UnicodeMap<>();
        for (String s : EmojiData.EMOJI_DATA.getAllEmojiWithoutDefectives()) {
            m.put(s, Emoji.getNewest(s));
        }
        TreeSet<Age_Values> sorted = new TreeSet<>(m.values());
        for (Age_Values value : sorted) {
            UnicodeSet chars = m.getSet(value);
            displayUnicodesetTD(out, Collections.singleton(showVersionOnly(value)), 
                    VersionToAge.getYear(value)+"",
                    Collections.singleton(String.valueOf(chars.size())), chars, Style.bestImage, -1, null);
        }
        writeFooter(out, "");
        out.close();
    }

    //    private static void showSubhead() throws IOException {
    //        Map<String, UnicodeSet> subheadToChars = new TreeMap();
    //        for (String s : GenerateEmoji.emojiData.getChars()) {
    //            int firstCodepoint = s.codePointAt(0);
    //            String header = Default.ucd().getBlock(firstCodepoint).replace('_', ' ');
    //            String subhead = subheader.getSubheader(firstCodepoint);
    //            if (subhead == null) {
    //                subhead = "UNNAMED";
    //            }
    //            header = header.contains(subhead) ? header : header + ": " + subhead;
    //            UnicodeSet uset = subheadToChars.get(header);
    //            if (uset == null) {
    //                subheadToChars.put(header, uset = new UnicodeSet());
    //            }
    //            uset.add(s);
    //        }
    //        PrintWriter out = BagFormatter.openUTF8Writer(Emoji.CHARTS_DIR, "emoji-subhead.html");
    //        writeHeader(out, "Emoji Subhead", null, "Unicode Subhead mapping.", "border='1'", true);
    //        for (Entry<String, UnicodeSet> entry : subheadToChars.entrySet()) {
    //            String label = entry.getKey();
    //            UnicodeSet uset = entry.getValue();
    //            if (label.equalsIgnoreCase("exclude")) {
    //                continue;
    //            }
    //            displayUnicodesetTD(out, Collections.singleton(label), null, Collections.<String> emptySet(), uset, Style.emoji, 16, null);
    //        }
    //        writeFooter(out, "");
    //        out.close();
    //    }

    /** Main charts */
    private static void showAnnotations(String dir, String filename, UnicodeSet filterOut, Set<String> retainAnnotations, boolean removeInsteadOf)
            throws IOException {
        try (PrintWriter out = BagFormatter.openUTF8Writer(dir, filename)) {
            writeHeader(out, "Emoji Annotations", null, "<p>This chart shows the English emoji character annotations based on " + CLDR_DATA_LINK + ". "
                    + "To make suggestions for improvements, "
                    + "please file a " + getCldrTicket("annotations", "Emoji annotation suggestions") + ".</p>\n", "border='1'", true);

            // Relation<UnicodeSet, String> seen = Relation.of(new HashMap(),
            // TreeSet.class, CODEPOINT_COMPARE);
            // for (Entry<String, Set<String>> entry :
            // GenerateEmoji.ANNOTATIONS_TO_CHARS.keyValuesSet()) {
            // String word = entry.getKey();
            // Set<String> values = entry.getValue();
            // UnicodeSet uset = new UnicodeSet().addAll(values);
            // try {
            // Label label = Label.valueOf(word);
            // continue;
            // } catch (Exception e) {
            // }
            // seen.put(uset, word);
            // }
            out.println("<tr><th style='max-width:25%;min-width:15em'>Annotations</th><th>Emoji</th></tr>");
            Set<String> labelSeen = new HashSet<>();
            Relation<Set<String>, String> setOfCharsToKeys = EmojiAnnotations.ANNOTATIONS_TO_CHARS.getValuesToKeys();

            for (Entry<String, Set<String>> entry : EmojiAnnotations.ANNOTATIONS_TO_CHARS.keyValuesSet()) {
                String word = entry.getKey();
                if (labelSeen.contains(word)) {
                    continue;
                }
                Set<String> values = entry.getValue();
                Set<String> words = setOfCharsToKeys.get(values);
                labelSeen.addAll(words);
                if (retainAnnotations != null) {
                    words = new LinkedHashSet<>(words);
                    if (removeInsteadOf) {
                        words.removeAll(retainAnnotations);
                    } else {
                        words.retainAll(retainAnnotations);
                    }
                }
                if (words.isEmpty()) {
                    continue;
                }
                UnicodeSet uset = new UnicodeSet().addAll(values);
                // Set<String> words = seen.getAll(uset);
                // if (words == null || labelSeen.contains(words)) {
                // continue;
                // }
                // labelSeen.add(words);
                UnicodeSet filtered = new UnicodeSet(uset).retainAll(filterOut);
                if (!filtered.isEmpty()) {
                    displayUnicodesetTD(out, words, null, Collections.<String> emptySet(), filtered, Style.bestImage, -1, "full-emoji-list.html");
                }
            }
            writeFooter(out, "");
        }
    }

    private static void showAnnotationsBySize(String dir, String filename, UnicodeSet retainSet) throws IOException {
        PrintWriter out = BagFormatter.openUTF8Writer(dir, filename);
        writeHeader(out, "Emoji Annotations", null, "<p>Finer-grained character annotations. </p>\n", "border='1'", true);
        TreeSet<Row.R3<Integer, UnicodeSet, String>> sorted = new TreeSet<>();
        Relation<UnicodeSet, String> usToAnnotations = Relation.of(new HashMap(), TreeSet.class, EmojiOrder.UCA_COLLATOR);
        for (Entry<String, Set<String>> entry : EmojiAnnotations.ANNOTATIONS_TO_CHARS.keyValuesSet()) {
            String word = entry.getKey();
            if (EmojiAnnotations.GROUP_ANNOTATIONS.contains(word)) {
                continue;
            }
            Set<String> values = entry.getValue();
            UnicodeSet uset = new UnicodeSet().addAll(values);
            UnicodeSet filtered = new UnicodeSet(uset).retainAll(retainSet);
            if (filtered.isEmpty()) {
                continue;
            }
            sorted.add(Row.of(-filtered.size(), filtered, word));
            usToAnnotations.put(filtered, word);
        }
        Set<String> seenAlready = new HashSet<>();
        for (R3<Integer, UnicodeSet, String> entry : sorted) {
            String word = entry.get2();
            if (seenAlready.contains(word)) {
                continue;
            }
            UnicodeSet uset = entry.get1();
            Set<String> allWords = usToAnnotations.get(uset);
            displayUnicodesetTD(out, allWords, null, Collections.<String> emptySet(), uset, Style.bestImage, 16, "full-emoji-list.html");
            seenAlready.addAll(allWords);
        }
        writeFooter(out, "");
        out.close();
    }

    // private static void showAnnotationsDiff() throws IOException {
    // PrintWriter out = BagFormatter.openUTF8Writer(Emoji.OUTPUT_DIR,
    // "emoji-annotations-diff.html");
    // writeHeader(out, "Emoji Annotations Diff",
    // "Finer-grained character annotations. " +
    // "For brevity, flags are not shown: they would have names of the associated countries.");
    // out.println("<tr><th>Code</th><th>Image</th><th>Name</th><th>Old-Only</th><th>New-Only</th><th>Same Annotation</th></tr>");
    //
    // for (String emoji : SORTED_EMOJI_CHARS_SET) {
    // Set<String> values = ANNOTATIONS_TO_CHARS.getKeys(emoji);
    // Set<String> valuesNew = ANNOTATIONS_TO_CHARS_NEW.getKeys(emoji);
    // boolean sameValues = Objects.equals(values, valuesNew);
    // Set<String> same = new LinkedHashSet(values);
    // same.retainAll(valuesNew);
    // Set<String> oldOnly = new LinkedHashSet(values);
    // oldOnly.removeAll(valuesNew);
    // Set<String> newOnly = new LinkedHashSet(valuesNew);
    // newOnly.removeAll(values);
    // UnicodeSet uset = new UnicodeSet().add(emoji);
    // out.print("<tr>");
    // out.println("<td class='code'>" + getDoubleLink(Utility.hex(emoji, " "))
    // + "</td>\n");
    //
    // displayUnicodeSet(out, uset, Style.bestImage, 16, 1, 1,
    // "full-emoji-list.html", CODEPOINT_COMPARE);
    // out.println("<td>" + getName(emoji, true) + "</td>\n");
    // if (sameValues) {
    // out.println("<td colSpan='3' bgcolor='#EEE'>" +
    // CollectionUtilities.join(same, ", ") + "</td>\n");
    // } else {
    // out.println("<td bgcolor='#DFD'>" + CollectionUtilities.join(oldOnly,
    // ", ") + "</td>\n");
    // out.println("<td bgcolor='#DDF'>" + CollectionUtilities.join(newOnly,
    // ", ") + "</td>\n");
    // out.println("<td>" + CollectionUtilities.join(same, ", ") + "</td>\n");
    // }
    // out.println("</tr>");
    // }
    // writeFooter(out);
    // out.close();
    // }

    static final UnicodeSet EXCLUDE_SET = new UnicodeSet()
    .addAll(GENERAL_CATEGORY.getSet(General_Category_Values.Unassigned.toString()))
    .addAll(GENERAL_CATEGORY.getSet(General_Category_Values.Private_Use.toString()))
    .addAll(GENERAL_CATEGORY.getSet(General_Category_Values.Surrogate.toString()))
    .addAll(GENERAL_CATEGORY.getSet(General_Category_Values.Control.toString()))
    .addAll(GENERAL_CATEGORY.getSet(General_Category_Values.Nonspacing_Mark.toString()));

    private static void showOtherUnicode() throws IOException {
        Map<String, UnicodeSet> labelToUnicodeSet = new TreeMap();

        getLabels("otherLabels.txt", labelToUnicodeSet);
        getLabels("otherLabelsComputed.txt", labelToUnicodeSet);
        UnicodeSet symbolMath = Emoji.LATEST.load(UcdProperty.Math).getSet(Binary.Yes.toString());
        UnicodeSet symbolMathAlphanum = new UnicodeSet()
        .addAll(Emoji.LATEST.load(UcdProperty.Alphabetic).getSet(Binary.Yes.toString()))
        .addAll(GENERAL_CATEGORY.getSet(General_Category_Values.Decimal_Number.toString()))
        .addAll(GENERAL_CATEGORY.getSet(General_Category_Values.Letter_Number.toString()))
        .addAll(GENERAL_CATEGORY.getSet(General_Category_Values.Other_Number.toString()))
        .retainAll(symbolMath);
        symbolMath.removeAll(symbolMathAlphanum);
        addSet(labelToUnicodeSet, "Symbol-Math", symbolMath);
        addSet(labelToUnicodeSet, "Symbol-Math-Alphanum", symbolMathAlphanum);
        addSet(labelToUnicodeSet, "Symbol-Braille",
                Emoji.LATEST.load(UcdProperty.Block).getSet(Block_Values.Braille_Patterns.toString()));
        addSet(labelToUnicodeSet, "Symbol-APL", new UnicodeSet("[⌶-⍺ ⎕]"));

        UnicodeSet otherSymbols = new UnicodeSet()
        .addAll(GENERAL_CATEGORY.getSet(General_Category_Values.Math_Symbol.toString()))
        .addAll(GENERAL_CATEGORY.getSet(General_Category_Values.Other_Symbol.toString()))
        .removeAll(NFKCQC.getSet(Binary.No.toString()))
        .removeAll(EmojiData.EMOJI_DATA.getChars())
        .retainAll(COMMON_SCRIPT);
        ;
        UnicodeSet otherPunctuation = new UnicodeSet()
        .addAll(GENERAL_CATEGORY.getSet(General_Category_Values.Close_Punctuation.toString()))
        .addAll(GENERAL_CATEGORY.getSet(General_Category_Values.Connector_Punctuation.toString()))
        .addAll(GENERAL_CATEGORY.getSet(General_Category_Values.Dash_Punctuation.toString()))
        .addAll(GENERAL_CATEGORY.getSet(General_Category_Values.Final_Punctuation.toString()))
        .addAll(GENERAL_CATEGORY.getSet(General_Category_Values.Initial_Punctuation.toString()))
        .addAll(GENERAL_CATEGORY.getSet(General_Category_Values.Math_Symbol.toString()))
        .addAll(GENERAL_CATEGORY.getSet(General_Category_Values.Open_Punctuation.toString()))
        .addAll(GENERAL_CATEGORY.getSet(General_Category_Values.Other_Punctuation.toString()))
        .removeAll(NFKCQC.getSet(Binary.No.toString()))
        .removeAll(EmojiData.EMOJI_DATA.getChars())
        .retainAll(COMMON_SCRIPT);
        ;

        for (Entry<String, UnicodeSet> entry : labelToUnicodeSet.entrySet()) {
            UnicodeSet uset = entry.getValue();
            uset.removeAll(EmojiData.EMOJI_DATA.getChars());
            otherSymbols.removeAll(uset);
            otherPunctuation.removeAll(uset);
        }
        if (!otherPunctuation.isEmpty()) {
            addSet(labelToUnicodeSet, "Punctuation-Other", otherPunctuation);
        }
        if (!otherSymbols.isEmpty()) {
            addSet(labelToUnicodeSet, "Symbol-Other", otherSymbols);
        }

        PrintWriter out = BagFormatter.openUTF8Writer(Emoji.TR51_INTERNAL_DIR
                , "other-labels.html");
        writeHeader(out, "Other Labels", null, "<p>Draft categories for other Symbols and Punctuation.</p>\n", "border='1'", true);

        for (Entry<String, UnicodeSet> entry : labelToUnicodeSet.entrySet()) {
            String label = entry.getKey();
            UnicodeSet uset = entry.getValue();
            if (label.equalsIgnoreCase("exclude")) {
                continue;
            }
            displayUnicodesetTD(out, Collections.singleton(label), null, Collections.singleton(""+uset.size()), uset, Style.plain, 16, "");
        }

        writeFooter(out, "");
        out.close();
    }

    public static void getLabels(String string, Map<String, UnicodeSet> labelToUnicodeSet) {
        String lastLabel = null;
        for (String line : FileUtilities.in(GenerateEmoji.class, string)) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            char first = line.charAt(0);
            if ('a' <= first && first <= 'z' || 'A' <= first && first <= 'Z') {
                if (!Emoji.ASCII_LETTER_HYPHEN.containsAll(line)) {
                    throw new IllegalArgumentException();
                }
                lastLabel = line;
            } else {
                UnicodeSet set = new UnicodeSet("[" + line
                        .replace("&", "\\&")
                        .replace("\\", "\\\\")
                        .replace("[", "\\[")
                        .replace("]", "\\]")
                        .replace("^", "\\^")
                        .replace("{", "\\{")
                        .replace("-", "\\-")
                        + "]");
                addSet(labelToUnicodeSet, lastLabel, set);
            }
        }
    }

    public static <T> void addSet(Map<T, UnicodeSet> labelToUnicodeSet, T key, UnicodeSet set) {
        UnicodeSet s = labelToUnicodeSet.get(key);
        if (s == null) {
            labelToUnicodeSet.put(key, s = new UnicodeSet());
        }
        s.addAll(set).removeAll(EXCLUDE_SET);
    }

    public static <T extends Object> void displayUnicodesetTD(PrintWriter out, Set<T> labels, String sublabel, 
            Set<String> otherCols,
            UnicodeSet uset, Style showEmoji, int maxPerLine, String link) {
        out.print("<tr><td>");
        boolean first = true;
        for (Object labelRaw : labels) {
            String label = labelRaw.toString();
            if (!first) {
                out.print(", ");
            }
            first = false;
            String anchor = (sublabel == null || sublabel.isEmpty() ? label : label + "_" + sublabel);
            getDoubleLink(anchor, label);
            out.print(getDoubleLink(anchor, label));
        }
        out.println("</td>");
        if (sublabel != null) {
            out.println("<td>" + sublabel + "</td>");
        }
        for (String s : otherCols) {
            out.println("<td>" + TransliteratorUtilities.toHTML.transform(s) + "</td>");
        }
        if (SHOW)
            System.out.println(labels + "\t" + uset.size());
        displayUnicodeSet(out, uset, showEmoji, maxPerLine, 1, 1, link, EMOJI_COMPARATOR, true);
        out.println("</tr>");
    }

    public static void displayUnicodeSet(PrintWriter out,
            UnicodeSet uset, Style showEmoji, int maxPerLine, int colSpan, int rowSpan,
            String link, Comparator comparator, boolean useDataUrl) {
        Set<String> sorted = uset.addAllTo(new TreeSet<String>(comparator));
        displayUnicodeSet(out, sorted, showEmoji, maxPerLine, colSpan, rowSpan, link, "", useDataUrl);
    }

    static final String         FULL_LINK          = "<a href='full-emoji-list.html' target='full'>Full Emoji List</a>";

    private static final String HOVER_INSTRUCTIONS = "Hovering over an emoji shows the name; clicking goes to the " + FULL_LINK + " entry for that emoji.";

    public static void displayUnicodeSet(PrintWriter out,
            Collection<String> sorted, Style showEmoji, int maxPerLine, int colSpan, int rowSpan,
            String link, String extraClasses, boolean useDataUrl) {
        if (link == null) {
            link = "full-emoji-list.html";
        } else if (link.isEmpty()) {
            link = null;
        }
        out.println("<td class='lchars'"
                + (rowSpan <= 1 ? "" : " rowSpan='" + rowSpan + "'")
                + (colSpan <= 1 ? "" : " colSpan='" + colSpan + "'")
                + ">");
        int count = 0;
        for (String s : sorted) {
            if (count == 0) {
                out.print("\n");
            } else if (maxPerLine > 0 && (count % maxPerLine) == 0) {
                out.print(BREAK);
            } else {
                out.print(" ");
            }
            ++count;
            boolean gotTitle = false;
            String cell = null; // getFlag(s, extraClasses);
            String classString = " class='plain'";
            if (cell == null) {
                switch (showEmoji) {
                case text:
                case emoji:
                    cell = Emoji.getEmojiVariant(s, showEmoji == Style.emoji ? Emoji.EMOJI_VARIANT_STRING : Emoji.TEXT_VARIANT_STRING);
                    break;
                case emojiFont:
                    classString = " class='charsSmall'";
                    // fall through
                case plain:
                    cell = s;
                    break;
                case bestImage:
                    cell = getBestImage(s, useDataUrl, extraClasses);
                    gotTitle = true;
                    break;
                case refImage:
                    cell = getImage(Emoji.Source.ref, s, true, extraClasses);
                    gotTitle = true;
                    break;
                }
            }
            if (link != null) {
                cell = "<a" + classString + " href='" + link + "#" + Emoji.buildFileName(s, "_") + "' target='full'>" + cell + "</a>";
            }
            if (!gotTitle) {
                cell = addTitle(s, cell);
            }
            out.print(cell);
        }
        out.println("</td>");
    }

    private static String addTitle(String s, String cell) {
        return "<span title='" +
                getHex(s) + " " + Emoji.getName(s, true, GenerateEmoji.EXTRA_NAMES) + "'>"
                + cell
                + "</span>";
    }

    public static String getHex(String theChars) {
        return toUHex(theChars);
    }

    public static String getCodeAndName(String chars1, String separator) {
        return getHex(chars1) + separator + chars1 + separator + Emoji.getName(chars1, true, GenerateEmoji.EXTRA_NAMES);
    }

    static final UnicodeSet SPECIAL_INCLUSIONS = new UnicodeSet("[#*0-9 ⃣]").addAll(Emoji.REGIONAL_INDICATORS).freeze();

    private static final String CLDR_DATA_LINK = "<a target='cldr' href='http://cldr.unicode.org/#TOC-What-is-CLDR-'>Unicode CLDR data</a>";

    enum Form {
        shortForm("short", " short form"),
        noImages("", " with single image and annotations. (See also the <a target='full' href='full-emoji-list.html'>full list</a>.) "
                + "The ordering of the emoji and the annotations are based on "
                + CLDR_DATA_LINK + ". This list does not include the <a target='style' href='emoji-sequences.html#modifier_sequences'>320 modifier sequences</a>,"
                + " or the <a target='zwj' href='emoji-zwj-sequences.html'>23 ZWJ sequences</a>."),
                fullForm("full", "with images from different vendors, version and source information, default style, and annotations. "
                        + "The ordering of the emoji and the annotations are based on "
                        + CLDR_DATA_LINK + ". This list does  include the <a target='style' href='emoji-sequences.html#modifier_sequences'>320 modifier sequences</a>,"
                        + " and the <a target='zwj' href='emoji-zwj-sequences.html'>23 ZWJ sequences</a>."),
                        extraForm("extra", " with images; have icons but are not including");

        final String filePrefix;
        final String title;
        final String description;

        Form(String prefix, String description) {
            this.description = description
                    ;
            filePrefix = prefix.isEmpty() ? "" : prefix + "-";
            title = (prefix.isEmpty() ? "" : UCharacter.toTitleCase(prefix, null) + " ")
                    + "Emoji Data";
        }
    }

    /** Main charts 
     * @param extraPlatforms TODO*/
    public static <T> void print(Form form, EmojiStats stats, boolean extraPlatforms) throws IOException {
        PrintWriter out = BagFormatter.openUTF8Writer(extraPlatforms ? Emoji.INTERNAL_OUTPUT_DIR : Emoji.CHARTS_DIR,
                form.filePrefix + "emoji-list" + (form != Form.fullForm ? "" : extraPlatforms ? "-extra" : "")
                + ".html");
        PrintWriter outText = null;
        PrintWriter outText2 = null;
        int order = 0;
        UnicodeSet level1 = null;
        writeHeader(out, form.title, null, "<p>This chart provides a list of the Unicode emoji characters, " + form.description + "</p>\n", "border='1'", false);
        final String htmlHeaderString = GenerateEmoji.toHtmlHeaderString(form, extraPlatforms);
        int item = 0;
        for (String s : SORTED_ALL_EMOJI_CHARS_SET // form == Form.fullForm ? SORTED_ALL_EMOJI_CHARS_SET : SORTED_EMOJI_CHARS_SET
                ) {
            if ((item % 25) == 0) {
                out.println(htmlHeaderString);
            }
            out.println(toHtmlString(s, form, ++item, stats, extraPlatforms));
            if (outText != null) {
                outText.println(toSemiString(s, order++, null));
                outText2.println(toSemiString(s, order++, level1));
            }
        }
        writeFooter(out, "");
        out.close();
    }

    enum Properties {Emoji, Emoji_Presentation, Emoji_Modifier, Emoji_Modifier_Base}

    public static void writeFooter(PrintWriter out, String htmlAfterTable) {
        out.println("</table>"); 
        out.println(htmlAfterTable);
        out.println("</div>"
                + "<div class='copyright'>"
                //+ "<hr width='50%'>"
                + "<br><a href='http://www.unicode.org/unicode/copyright.html'>"
                + "<img src='http://www.unicode.org/img/hb_notice.gif' style='border-style: none; width: 216px; height=50px;' alt='Access to Copyright and terms of use'>"
                + "</a><br><script language='Javascript' type='text/javascript' src='http://www.unicode.org/webscripts/lastModified.js'></script>"
                + "</div><script>\n"
                + "(function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){"
                + "(i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),"
                + "m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)"
                + "})(window,document,'script','//www.google-analytics.com/analytics.js','ga');"
                + "  ga('create', 'UA-19876713-1', 'auto');"
                + "  ga('send', 'pageview');"
                + "</script>"
                + "</body></html>");
    }

    public static void writeHeader(PrintWriter out, String title, String styles, String firstLine, String tableAttrs, boolean showGeneralComments) {
        final String chartIndex = "<a target='text' href='index.html'>Emoji Chart Index</a>";
        out.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n" +
                "<html>\n" +
                "<head>\n" +
                "<meta http-equiv='Content-Type' content='text/html; charset=UTF-8'>\n" +
                "<link rel='stylesheet' type='text/css' href='emoji-list.css'>\n" +
                "<title>" + DRAFT_TITLE_PREFIX + title + "</title>\n" +
                (styles == null ? "" : "<style>\n" + styles + "\n</style>\n") +
                "</head>\n" +
                "<body>\n"
                + UNICODE_HEADER
                + getButton()
                + "<h1>" + DRAFT_TITLE_PREFIX + title + "</h1>\n"
                //+ "<p><b>" + chartIndex + "</b></p>\n"
                + firstLine
                + "<p>For information about the images used in these charts, see <a href='../images.html'>Emoji Images and Rights</a>. "
                + "For details about the format and fields, see " + chartIndex + " and " + UTR_LINK + ". " 
                + (showGeneralComments ? HOVER_INSTRUCTIONS : "")
                + " See also <a target='submitting-emoji' href='../../emoji/selection.html'>Submitting Emoji Character Proposals</a>."
                + "</p>\n"
                + "<table " + tableAttrs + ">");
    }

    static boolean CHECKFACE = false;

    static void oldAnnotationDiff() throws IOException {
        PrintWriter out = BagFormatter.openUTF8Writer(Emoji.CHARTS_DIR, "emoji-diff.html");
        writeHeader(out, "Diff List", null, "<p>Differences from other categories.</p>\n", "border='1'", true);

        UnicodeSet AnimalPlantFood = new UnicodeSet("[☕ 🌰-🌵 🌷-🍼 🎂 🐀-🐾]");
        testEquals(out, "AnimalPlantFood", AnimalPlantFood, Label.nature, Label.food);

        UnicodeSet Object = new UnicodeSet("[⌚ ⌛ ⏰ ⏳ ☎ ⚓ ✂ ✉ ✏ 🎀 🎁 👑-👣 💄 💉 💊 💌-💎 💐 💠 💡 💣 💮 💰-📷 📹-📼 🔋-🔗 🔦-🔮 🕐-🕧]");
        testEquals(out, "Object", Object, Label.object, Label.office, Label.clothing);

        CHECKFACE = true;
        UnicodeSet PeopleEmotion = new UnicodeSet("[☝ ☺ ✊-✌ ❤ 👀 👂-👐 👤-💃 💅-💇 💋 💏 💑 💓-💟 💢-💭 😀-🙀 🙅-🙏]");
        testEquals(out, "PeopleEmotion", PeopleEmotion, Label.person, Label.body, Label.emotion, Label.face);
        CHECKFACE = false;

        UnicodeSet SportsCelebrationActivity = new UnicodeSet("[⛑ ⛷ ⛹ ♠-♧ ⚽ ⚾ 🀀-🀫 🂠-🂮 🂱-🂾 🃁-🃏 🃑-🃟 🎃-🎓 🎠-🏄 🏆-🏊 💒]");
        testEquals(out, "SportsCelebrationActivity", SportsCelebrationActivity, Label.game, Label.sport, Label.activity);

        UnicodeSet TransportMapSignage = new UnicodeSet("[♨ ♻ ♿ ⚠ ⚡ ⛏-⛡ ⛨-⛿ 🏠-🏰 💈 🗻-🗿 🚀-🛅]");
        testEquals(out, "TransportMapSignage", TransportMapSignage, Label.travel, Label.place);

        UnicodeSet WeatherSceneZodiacal = new UnicodeSet("[☀-☍ ☔ ♈-♓ ⛄-⛈ ⛎ ✨ 🌀-🌠 🔥]");
        testEquals(out, "WeatherSceneZodiacal", WeatherSceneZodiacal, Label.weather, Label.time);

        UnicodeSet Enclosed = new UnicodeSet(
                "[[\u24C2\u3297\u3299][\\U0001F150-\\U0001F19A][\\U0001F200-\\U0001F202][\\U0001F210-\\U0001F23A][\\U0001F240-\\U0001F248][\\U0001F250-\\U0001F251]]");
        testEquals(out, "Enclosed", Enclosed, Label.word);

        UnicodeSet Symbols = new UnicodeSet(
                "[[\\U0001F4AF][\\U0001F500-\\U0001F525][\\U0001F52F-\\U0001F53D][\\U0001F540-\\U0001F543[\u00A9\u00AE\u2002\u2003\u2005\u203C\u2049\u2122\u2139\u2194\u2195\u2196\u2197\u2198\u2199\u21A9\u21AA\u231B\u23E9\u23EA\u23EB\u23EC\u25AA\u25AB\u25B6\u25C0\u25FB\u25FC\u25FD\u25FE\u2611\u2660\u2663\u2665\u2666\u267B\u2693\u26AA\u26AB\u2705\u2708\u2712\u2714\u2716\u2728\u2733\u2734\u2744\u2747\u274C\u274E\u2753\u2754\u2755\u2757\u2764\u2795\u2796\u2797\u27A1\u27B0\u2934\u2935\u2B05\u2B06\u2B07\u2B1B\u2B1C\u2B50\u2B55\u3030\u303D]]]");
        testEquals(out, "Symbols", Symbols, Label.sign);

        UnicodeSet other = new UnicodeSet(get70(Label.values()))
        .removeAll(AnimalPlantFood)
        .removeAll(Object)
        .removeAll(PeopleEmotion)
        .removeAll(SportsCelebrationActivity)
        .removeAll(TransportMapSignage)
        .removeAll(WeatherSceneZodiacal)
        .removeAll(Enclosed)
        .removeAll(Symbols);

        testEquals(out, "Other", other, Label.flag, Label.sign, Label.arrow);

        UnicodeSet ApplePeople = new UnicodeSet("[☝☺✊-✌✨❤🌂🌟🎀🎩🎽🏃👀👂-👺👼👽 👿-💇💋-💏💑💓-💜💞💢💤-💭💼🔥😀-🙀🙅-🙏 🚶]");
        testEquals(out, "ApplePeople", ApplePeople, Label.person, Label.emotion, Label.face, Label.body, Label.clothing);

        UnicodeSet AppleNature = new UnicodeSet("[☀☁☔⚡⛄⛅❄⭐🌀🌁🌈🌊-🌕🌙-🌞🌠🌰-🌵 🌷-🌼🌾-🍄🐀-🐾💐💩]");
        testEquals(out, "AppleNature", AppleNature, Label.nature, Label.food, Label.weather);

        UnicodeSet ApplePlaces = new UnicodeSet("[♨⚓⚠⛪⛲⛵⛺⛽✈🇧-🇬🇮-🇰🇳🇵🇷-🇺 🌃-🌇🌉🎠-🎢🎪🎫🎭🎰🏠-🏦🏨-🏰💈💒💺📍 🔰🗻-🗿🚀-🚝🚟-🚩🚲]");
        testEquals(out, "ApplePlaces", ApplePlaces, Label.place, Label.travel);

        UnicodeSet AppleSymbols = new UnicodeSet(
                "[©®‼⁉⃣™ℹ↔-↙↩↪⏩-⏬ Ⓜ▪▫▶◀◻-◾☑♈-♓♠♣♥♦♻♿⚪⚫⛎ ⛔✅✔✖✳✴❇❌❎❓-❕❗➕-➗➡➰➿⤴⤵ ⬅-⬇⬛⬜⭕〰〽㊗㊙🅰🅱🅾🅿🆎🆑-🆚🈁🈂🈚 🈯🈲-🈺🉐🉑🌟🎦🏧👊👌👎💙💛💟💠💢💮💯💱💲 💹📳-📶🔀-🔄🔗-🔤🔯🔱-🔽🕐-🕧🚫🚭-🚱 🚳🚷-🚼🚾🛂-🛅]");
        testEquals(out, "AppleSymbols", AppleSymbols, Label.sign, Label.game);

        UnicodeSet AppleTextOrEmoji = new UnicodeSet(
                "[‼⁉ℹ↔-↙↩↪Ⓜ▪▫▶◀◻-◾☀☁☎ ☑☔☕☝☺♈-♓♠♣♥♦♨♻♿⚓⚠⚡⚪⚫⚰ ⚾✂✈✉✌✏✒✳✴❄❇❤➡⤴⤵⬅-⬇〽㊗㊙ 🅰🅱🅾🅿🈂🈷🔝{#⃣}{0⃣}{1⃣}{2 ⃣}{3⃣}{4⃣}{5⃣}{6⃣}{7⃣}{8 ⃣}{9⃣}{🇨🇳}{🇩🇪}{🇪🇸}{🇫🇷}{🇬🇧}{ 🇮🇹}{🇯🇵}{🇰🇷}{🇷🇺}{🇺🇸}]");
        UnicodeSet AppleOnlyEmoji = new UnicodeSet(
                "[⌚⌛⏩-⏬⏰⏳⚽⛄⛅⛎⛔⛪⛲⛳⛵⛺⛽✅ ✊✋✨❌❎❓-❕❗➿⬛⬜⭐⭕🀄🃏🆎🆑-🆚🈁 🈚🈯🈲-🈶🈸-🈺🉐🉑🌀-🌠🌰-🌵🌷-🍼🎀-🎓 🎠-🏊🏠-🏰🐀-🐾👀👂-📷📹-📼🔀-🔘🔞-🔽 🕐-🕧🗻-🙀🙅-🙏🚀-🛅]");

        UnicodeSet AppleAll = new UnicodeSet(AppleTextOrEmoji).addAll(AppleOnlyEmoji);
        UnicodeSet AppleObjects = new UnicodeSet(AppleAll)
        .removeAll(ApplePeople)
        .removeAll(AppleNature)
        .removeAll(ApplePlaces)
        .removeAll(AppleSymbols);

        testEquals(out, "AppleObjects", AppleObjects, Label.flag, Label.sign, Label.arrow);

        writeFooter(out, "");
        out.close();
    }

    public static void testEquals(PrintWriter out, String title1, UnicodeSet AnimalPlantFood,
            String title2, UnicodeSet labelNatureFood) {
        testContains(out, title1, AnimalPlantFood, title2, labelNatureFood);
        testContains(out, title2, labelNatureFood, title1, AnimalPlantFood);
    }

    public static void testEquals(PrintWriter out, String title1, UnicodeSet AnimalPlantFood,
            Label... labels) {
        title1 = "<b>" + title1 + "</b>";
        for (Label label : labels) {
            testContains(out, title1, AnimalPlantFood, label.toString(), get70(label));
        }
        String title2 = CollectionUtilities.join(labels, "+");
        UnicodeSet labelNatureFood = get70(labels);
        testContains(out, title2, labelNatureFood, title1, AnimalPlantFood);
    }

    private static void testContains(PrintWriter out, String title, UnicodeSet container, String title2, UnicodeSet containee) {
        if (!container.containsAll(containee)) {
            UnicodeSet missing = new UnicodeSet(containee).removeAll(container);
            out.println("<tr><td>" + title + "</td>\n" +
                    "<td>⊉</td>\n" +
                    "<td>" + title2 + "</td>\n" +
                    "<td>" + missing.size() + "/" + containee.size() + "</td>\n" +
                    "<td class='lchars'>");
            boolean first = true;
            Set<String> sorted = new TreeSet<String>(EMOJI_COMPARATOR);
            missing.addAllTo(sorted);
            for (String s : sorted) {
                if (first) {
                    first = false;
                } else {
                    out.print("\n");
                }
                out.print("<span title='" + Emoji.getName(s, false, null) + "'>"
                        + Emoji.getEmojiVariant(s, Emoji.EMOJI_VARIANT_STRING)
                        + "</span>");
            }
            out.println("</td></tr>");
        }
    }

    public static UnicodeSet get70(Label... labels) {
        UnicodeSet containee = new UnicodeSet();
        for (Label label : labels) {
            containee.addAll(Label.CHARS_TO_LABELS.getKeys(label));
        }
        containee.removeAll(VERSION70);
        // containee.retainAll(JSOURCES);
        return containee;
    }

    public static String getDoubleLink(String href, String anchorText) {
        href = href.replace(' ', '_').toLowerCase(Locale.ENGLISH);
        return "<a href='#" + href + "' name='" + href + "'>" + anchorText + "</a>";
    }

    public static String getLink(String href, String anchorText, String target) {
        href = href.replace(' ', '_').toLowerCase(Locale.ENGLISH);
        return "<a" +
        " href='" + href + "'" +
        (target == null ? "" : " target='" + target + "'") +
        ">" + anchorText + "</a>";
    }

    public static String getDoubleLink(String anchor) {
        return getDoubleLink(anchor, anchor);
    }

    static Map<String, String> IMAGE_CACHE = new HashMap<>();

    static String getDataUrl(String filename) {
        try {
            String result = IMAGE_CACHE.get(filename);
            if (result == null) {
                final File file = new File(getImageDirectory(filename), filename);
                if (!file.exists()) {
                    result = "";
                } else if (!DATAURL) {
                    result = "../images/" + filename;
                } else {
                    byte[] bytes = RESIZE_IMAGE <= 0 ? Files.readAllBytes(file.toPath())
                            : LoadImage.resizeImage(file, RESIZE_IMAGE, RESIZE_IMAGE);
                    result = "data:image/png;base64," + Base64.encode(bytes);
                }
                IMAGE_CACHE.put(filename, result);
            }
            return result.isEmpty() ? null : result;
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static String getImageDirectory(String filename) {
        return 
                //                filename.startsWith("samsung") || filename.startsWith("google")
                //                ? Settings.OTHER_WORKSPACE_DIRECTORY + "DATA/emoji/" 
                //                        : 
                Emoji.IMAGES_OUTPUT_DIR;
    }

    static void printCollationOrder() throws IOException {
        try (
                PrintWriter outText = BagFormatter.openUTF8Writer(Emoji.TR51_INTERNAL_DIR
                        , "emoji-ordering-list.txt")) {
            for (String s : SORTED_ALL_EMOJI_CHARS_SET) {
                outText.println(toUHex(s) 
                        + " ; " + Emoji.getNewest(s).getShortName() 
                        + " # " + s 
                        + " " + Emoji.getName(s, false, GenerateEmoji.EXTRA_NAMES));
            }
        }
        try (
                PrintWriter outText = BagFormatter.openUTF8Writer(Emoji.TR51_INTERNAL_DIR
                        , "emoji-ordering.txt")) {
            outText.append("<!-- emoji-ordering.txt\n"
                    + "\tFor details about the format and other information, see " + DOC_DATA_FILES + ".\n"
                    + "\thttp://unicode.org/cldr/trac/ticket/7270 -->\n"
                    + "<collation type='emoji'>\n"
                    + "<cr><![CDATA[\n"
                    + "# START AUTOGENERATED EMOJI ORDER\n");
            EmojiOrder.STD_ORDER.appendCollationRules(outText, 
                    EmojiData.EMOJI_DATA.getChars(), 
                    EmojiData.EMOJI_DATA.getZwjSequencesNormal()); // those without VS are added
            outText.write("\n]]></cr>\n</collation>");
        }
    }

    //    private static void printAnnotations() throws IOException {
    //        try (
    //                PrintWriter outText = BagFormatter.openUTF8Writer(Emoji.TR51_INTERNAL_DIR
    //                        , "emoji-annotations.xml")) {
    //            outText.append(ANNOTATION_HEADER
    //                    + "\t\t<language type='en'/>\n"
    //                    + "\t</identity>\n"
    //                    + "\t<annotations>\n");
    //            Set<Row.R2<Set<String>, UnicodeSet>> sorted = new TreeSet<>(PAIR_SORT);
    //            for (String s : emojiData.getChars()) {
    //                Set<String> annotations = getAnnotations(s);
    //                annotations.removeAll(EmojiAnnotations.GROUP_ANNOTATIONS);
    //                if (annotations.isEmpty()) {
    //                    throw new IllegalArgumentException("Missing annotation: " + s
    //                            + "\t" + EmojiAnnotations.ANNOTATIONS_TO_CHARS.getKeys(s));
    //                }
    //            }
    //            //            for (Entry<Set<String>, Set<String>> s : EmojiAnnotations.ANNOTATIONS_TO_CHARS.getValuesToKeys().keyValuesSet()) {
    //            //                UnicodeSet chars = new UnicodeSet().addAll(s.getKey());
    //            //                Set<String> annotations = new LinkedHashSet<>(s.getValue());
    //            //                annotations.removeAll(GROUP_ANNOTATIONS);
    //            //                if (annotations.isEmpty()) {
    //            //                    continue;
    //            //                }
    //            //                sorted.add(Row.of(annotations, chars));
    //            //            }
    //            //for (R2<Set<String>, UnicodeSet> s : sorted) {
    //            UnicodeSet chars = new UnicodeSet();
    //            for (String cp : EmojiOrder.STD_ORDER.orderingToCharacters.values()) {
    //                Set<String> annotationSet = getAnnotations(cp);
    //                annotationSet.removeAll(EmojiAnnotations.GROUP_ANNOTATIONS);
    //                String annotations = CollectionUtilities.join(annotationSet, "; ");
    //                chars.clear().add(cp);
    //                outText.append("\t\t<annotation cp='")
    //                .append(chars.toPattern(false))
    //                .append("'")
    //                //.append(" draft='provisional'")
    //                .append(">")
    //                .append(annotations)
    //                .append("</annotation>\n");
    //            }
    //            outText.write("\t</annotations>\n"
    //                    + "</ldml>");
    //        }
    //    }

    private static Set<String> getAnnotations(String string) {
        if (!string.contains("\u200D")) {
            return new LinkedHashSet<>(EmojiAnnotations.ANNOTATIONS_TO_CHARS.getKeys(string));
        }
        Set<String> result = new LinkedHashSet<>();
        for (int cp : CharSequences.codePoints(string)) {
            if (cp != 0x200D && cp != 0xFE0F) {
                result.addAll(EmojiAnnotations.ANNOTATIONS_TO_CHARS.getKeys(UTF16.valueOf(cp)));
            }
        }
        return result;
    }

    public static <T> int compareX(Iterator<T> iterator1, Iterator<T> iterator2, Comparator<T> comparator) {
        int diff;
        while (true) {
            if (!iterator1.hasNext()) {
                return iterator2.hasNext() ? -1 : 0;
            } else if (!iterator2.hasNext()) {
                return 1;
            }
            diff = comparator.compare(iterator1.next(), iterator2.next());
            if (diff != 0) {
                return diff;
            }
        }
    }

    static final Comparator<Row.R2<Set<String>, UnicodeSet>> PAIR_SORT = new Comparator<Row.R2<Set<String>, UnicodeSet>>() {
        SetComparator<Comparable> setComp;

        public int compare(R2<Set<String>, UnicodeSet> o1, R2<Set<String>, UnicodeSet> o2) {
            int diff = compareX(o1.get0().iterator(), o2.get0().iterator(),
                    (Comparator<String>) EmojiOrder.UCA_COLLATOR);
            if (diff != 0) {
                return diff;
            }
            return o1.get1().compareTo(o2.get1());
        }
    };

    public static String showVersion(Age_Values age_Values) {
        return showVersionOnly(age_Values) + " " + VersionToAge.getYear(age_Values);
    }

    private static String showVersionOnly(Age_Values age_Values) {
        return age_Values.toString().replace('_', '.');
    }

    public static String getSources(String chars2, StringBuilder suffix, boolean superscript) {
        boolean first = true;
        for (Emoji.CharSource source : GenerateEmoji.getCharSources(chars2)) {
            suffix.append(superscript ? source.superscript
                    : first ? source.letter
                            : " " + source.letter);
            first = false;
        }
        return suffix.toString();
    }

    public static Set<Emoji.CharSource> getCharSources(String s) {
        Set<Emoji.CharSource> source = EnumSet.noneOf(Emoji.CharSource.class);
        if (DINGBATS.contains(s)) {
            source.add(Emoji.CharSource.ZDings);
        }
        if (JSOURCES.contains(s)) {
            source.add(Emoji.CharSource.JCarrier);
        }
        if (DING_MAP.containsKey(s)) {
            source.add(Emoji.CharSource.WDings);
        }
        if (ARIB.contains(s)) {
            source.add(Emoji.CharSource.ARIB);
        }
        if (source.size() == 0) {
            source.add(Emoji.CharSource.Other);
        }
        return source;
    }

    public static String toSemiString(String chars2, int order, UnicodeSet level1) {
        // "# Code ;\tDefault Style ;\tLevel ;\tModifier ;\tSources ;\tVersion\t# (Character) Name\n"

        String extraString = "";
        final EmojiDatum data = EmojiData.EMOJI_DATA.getData(chars2);
        ModifierStatus modifier = data.modifierStatus;
        extraString = " ;\t" + modifier;

        return Utility.hex(chars2, " ")
                + " ;\t" + Style.valueOf(EmojiData.EMOJI_DATA.getData(chars2).style)
                + extraString
                + " ;\t" + getSources(chars2, new StringBuilder(), false)
                + "\t# " + showVersion(Emoji.getNewest(chars2))
                + " (" + chars2
                + ") " + Emoji.getName(chars2, false, GenerateEmoji.EXTRA_NAMES);
        // Set<String> annotations = new
        // LinkedHashSet<>(ifNull(GenerateEmoji.ANNOTATIONS_TO_CHARS.getKeys(chars),
        // Collections.EMPTY_SET));
        // annotations.removeAll(SUPPRESS_ANNOTATIONS);
        // if (annotations != null) {
        // annotations = new LinkedHashSet(annotations);
        // for (Label label : labels) {
        // annotations.remove(label.toString());
        // }
        // }
        // String flagRegion = getFlagRegionName(chars);
        // if (flagRegion != null) {
        // annotations.add(flagRegion);
        // }
        // if (annotations.isEmpty()) {
        // throw new IllegalArgumentException("No annotations for:\t" +
        // getName(chars, true) + "\t" + chars);
        // }
        // return Utility.hex(chars, " ")
        // + " ;\t" + order
        // + " ;\t" + CollectionUtilities.join(annotations, ", ")
        // + " \t# " + getVersion()
        // + " (" + chars
        // + ") " + getName(chars, true);
    }

    public static String getCell(Emoji.Source type, String core, String cellClass) {
        if (type == null) {
            return "<td class='andr'>"
                    + getBestImage(core, true, "", Emoji.Source.color)
                    + "</td>\n";
        }

        String filename = Emoji.getImageFilenameFromChars(type, core);
        String androidCell = "<td class='"+ cellClass
                + " miss'>missing</td>\n";
        if (filename != null) {
            String fullName = getDataUrl(filename);
            if (fullName == null && Emoji.IS_BETA && type == Source.ref) {
                filename = Emoji.getImageFilenameFromChars(Source.proposed, core);
                fullName = getDataUrl(filename);
            }
            if (fullName != null) {
                String className = type.getClassAttribute(core);
                androidCell = "<td class='"
                        + cellClass
                        + "'><img alt='" + core + "' class='" +
                        className + "' src='" + fullName + "'></td>\n";
            }
        }
        return androidCell;
    }
    static NamesList NAMESLIST = new NamesList("NamesList", Emoji.VERSION_TO_GENERATE_UNICODE.getVersionString(3,3));
    static final Joiner JOIN_PLUS = Joiner.on(" ⊕ ");


    static String getNamesListInfo(String s) {
        int cp = CharSequences.getSingleCodePoint(s);
        if (cp == Integer.MAX_VALUE) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        addNameslistInfo(NAMESLIST.formalAliases.get(cp), NamesList.Comment.formalAlias.displaySymbol, result);
        addNameslistInfo(NAMESLIST.informalAliases.get(cp), NamesList.Comment.alias.displaySymbol, result);
        addNameslistInfo(NAMESLIST.informalComments.get(cp), NamesList.Comment.comment.displaySymbol, result);
        addNameslistInfo(NAMESLIST.informalXrefs.get(cp), NamesList.Comment.xref.displaySymbol, result);
        return result.toString();
    }

    private static void addNameslistInfo(Set<String> formalAliases, String symbol, StringBuilder result) {
        if (formalAliases != null) {
            for (String x : formalAliases) {
                String image = EmojiData.EMOJI_DATA.getAllEmojiWithDefectives().contains(x) ? getBestImageNothrow(x, true, " imgb") : null;
                if (image == null) {
                    String fixed = TransliteratorUtilities.toHTML.transform(x);
                    image = UTF16.hasMoreCodePointsThan(x, 1) ? fixed : toUHex(x) + " " + fixed + " " + Emoji.getName(x, false, GenerateEmoji.EXTRA_NAMES);
                } else {
                    image = toUHex(x) + " " + image + " " + Emoji.getName(x, false, GenerateEmoji.EXTRA_NAMES);
                }
                result.append("<br>" + symbol + " " + image);
            }
        }
    }

    static boolean altAppearance = false;
    static String altClass(String cell) {
        return cell.replace(ALT_COLUMN, (altAppearance = !altAppearance) ? "andr" : "andr alt");
    }

    static final String ALT_COLUMN = "%%%";

    public static String toHtmlString(String chars2, Form form, int item, EmojiStats stats, boolean extraPlatforms) {
        String bestCell = getCell(null, chars2, ALT_COLUMN);
        String symbolaCell = getCell(Emoji.Source.ref, chars2, ALT_COLUMN);

        StringBuilder otherCells = new StringBuilder();

        for (Source s : platformsToIncludeNormal) {
            otherCells.append(altClass(getCell(s, chars2, ALT_COLUMN)));
        }

        String browserCell = "<td class='chars'>" + Emoji.getEmojiVariant(chars2, Emoji.EMOJI_VARIANT_STRING) + "</td>\n";
        String name2 = Emoji.getName(chars2, false, GenerateEmoji.EXTRA_NAMES);
        String tts = EmojiAnnotations.TTS.get(chars2);
        if (tts != null && !tts.equalsIgnoreCase(name2)) {
            name2 += "<br>≊ " + TransliteratorUtilities.toHTML.transform(tts);
        }
        if (SHOW_NAMES_LIST) {
            name2 += getNamesListInfo(chars2);
        }

        String textChars = Emoji.getEmojiVariant(chars2, Emoji.TEXT_VARIANT_STRING);
        Set<String> annotations = new LinkedHashSet<String>(Utility.ifNull(EmojiAnnotations.ANNOTATIONS_TO_CHARS.getKeys(chars2), Collections.EMPTY_SET));
        annotations.removeAll(GenerateEmoji.SUPPRESS_ANNOTATIONS);
        StringBuilder annotationString = new StringBuilder();
        if (!annotations.isEmpty()) {
            for (String annotation : annotations) {
                if (annotationString.length() != 0) {
                    annotationString.append(", ");
                }
                annotationString.append(getLink("emoji-annotations.html#" + annotation, annotation, "annotate"));
            }
        }
        String anchor = getAnchor(toUHex(chars2));
        final boolean shortForm = form != Form.fullForm && form != Form.extraForm;
        EmojiDatum emojiDatum = EmojiData.EMOJI_DATA.getData(chars2);
        return "<tr>"
        + "<td class='rchars'>" + item + "</td>\n"
        + "<td class='code'>" + getDoubleLink(anchor, toUHex(chars2)) + "</td>\n"
        + altClass(browserCell)
        + (shortForm ?  altClass(bestCell) : 
            altClass(symbolaCell)
            + otherCells)
            + "<td class='name'>" + name2 + "</td>\n"
            + (shortForm ? "" : 
                "<td class='age'>" + VersionToAge.getYear(Emoji.getNewest(chars2)) + getSources(chars2, new StringBuilder(), true) + "</td>\n"
                + "<td class='default'>" + (emojiDatum == null ? "n/a" : emojiDatum.style) + (!textChars.equals(chars2) ? "*" : "") + "</td>\n")
                + "<td class='name'>" + annotationString + "</td>\n"
                + "</tr>";
    }

    public static String toHtmlHeaderString(Form form, boolean extraPlatforms) {
        boolean shortForm = form.compareTo(Form.shortForm) <= 0;
        final boolean shortForm2 = form != Form.fullForm && form != Form.extraForm;
        StringBuilder otherCells = new StringBuilder();
        for (Source s : platformsToIncludeNormal) {
            otherCells.append("<th class='cchars'>" + s.shortName() + "</th>\n");
        }

        return "<tr>"
        + "<th class='rchars'>№</th>\n"
        + "<th class='rchars'>Code</th>\n"
        + (shortForm2
                ? "<th class='cchars'>Browser</th>\n"
                + "<th class='cchars'>Sample</th>\n"
                :
                    "<th class='cchars'>Brow.</th>\n"
                    + "<th class='cchars'>Chart</th>\n"
                    + otherCells
                )
                // + "<th class='cchars'>Browser</th>\n"
                + (shortForm ? "" :
                    "<th>Name</th>\n"
                    + (shortForm2 ? "" : "<th>Year</th>\n"
                            + "<th>Default</th>\n")
                            + "<th>Annotations</th>\n"
                            // + "<th>Block: <i>Subhead</i></th>\n"
                        )
                        + "</tr>";
    }

    private static final Set<String> SUPPRESS_ANNOTATIONS = new HashSet<>(Arrays.asList("default-text-style"));

    static final Splitter TAB = Splitter.on('\t');
    public static final UnicodeMap<String> EXTRA_NAMES = new UnicodeMap<>();

    static final Joiner SPACE_JOINER = Joiner.on(' ').skipNulls();

    static void showCandidates() throws IOException {
        // gather data
        EmojiData betaEmojiData = EmojiData.of(Emoji.VERSION_BETA);
        UnicodeSet modBase = betaEmojiData.getModifierStatusSet(ModifierStatus.modifier_base);
        UnicodeSet pres = betaEmojiData.getDefaultPresentationSet(DefaultPresentation.emoji);
        UnicodeMap<CandidateData.Quarter> quartersForChars = new UnicodeMap<>();
        // The data file is designed to take the contents of the table, when pasted as plain text, and format it.
        List<String> output = new ArrayList<>();
        CandidateData cd = CandidateData.getInstance();        
        Set<String> sorted = cd.getCharacters().addAllTo(new TreeSet<String>(cd.comparator));
        String lastCategory = null;
        MajorGroup lastMajorGroup = null;
        String header = "<tr>"
                + "<th>№</th>"
                + "<th width='7em'>Code Point</th>"
                + "<th width='5em'>Draft Chart Glyph</th>"
                + "<th>Sample Colored Glyphs</th>"
                + "<th>Date</th>"
                + "<th>EMP</th>"
                + "<th>EMB</th>"
                + "<th>Name</th>"
                + "</tr>";

        int count = 0;
        boolean isCurrent = true;
        String futureSuffix = "";
        output.add("<tr><th colspan='8' class='bighead2'>" + getDoubleLink("U9.0 Candidates") + "</th></tr>");
        output.add(header);
        for (String source : sorted) {
            String category = cd.getCategory(source);
            MajorGroup majorGroup = cd.getMajorGroup(source);
            if (majorGroup == null) {
                cd.getMajorGroup(source);  
            }
            if (isCurrent && CandidateData.Quarter.FUTURE.contains(cd.getQuarter(source))) {
                output.add("<tr><th colspan='8' class='bighead2'>" + getDoubleLink("Post U9.0 Candidates*") + "</th></tr>");
                output.add(header);
                isCurrent = false;
                futureSuffix = "*";
            }
            if (majorGroup != lastMajorGroup) {
                output.add("<tr><th colspan='8' class='bighead'>" + getDoubleLink(majorGroup.toString() + futureSuffix) + "</th></tr>");
                lastMajorGroup = majorGroup; 
            }
            if (!Objects.equals(category,lastCategory)) {
                output.add("<tr><th colspan='8'>" + getDoubleLink(category + futureSuffix) + "</th></tr>");
                lastCategory = category; 
            }
            String blackAndWhite = getImage(Source.proposed, source, true, "");
            String color1 = getImage(Source.emojixpress, source, true, "");
            String color2 = getImage(Source.emojipedia, source, true, "");
            String sample = getImage(Source.sample, source, true, "");
            String color = SPACE_JOINER.join(color1, color2, sample);
            String currentRow = "<tr>"
                    + "<td class='rchars'>" + ++count + "</td>"
                    + "<td class='code'>" + getDoubleLink(Utility.hex(source).replace(" ", "_"), toUHex(source)) + "</td>"
                    + "<td class='andr'>" + blackAndWhite + "</td>"
                    + "<td class='default'>" + color + "</td>"
                    + "<td class='default'>" + cd.getQuarter(source) + "</td>"
                    + "<td class='default'>" + (pres.contains(source) ? "P" : "")
                    + "<td class='default'>" + (modBase.contains(source) ? "B" : "")
                    + "<td class='name'>" + cd.getName(source)
                    ;
            for (String annotation :  cd.getAnnotations(source)) {
                currentRow += "<br> • " + annotation;
            }
            output.add(currentRow + "</tr>");
        }
        UnicodeSet items = cd.getCharacters();

        for (CandidateData.Quarter q : quartersForChars.values()) {
            System.out.println(q + "\t" + quartersForChars.getSet(q));
        }
        System.out.println(items.toString().replace("\\", "\\\\"));
        // now print
        try (PrintWriter out = BagFormatter.openUTF8Writer(Emoji.CHARTS_DIR, "emoji-candidates.html");) {
            writeHeader(out, "Emoji Candidates", null, "<p>The Unicode Technical Committee (UTC) has accepted the following "
                    + items.size()
                    + " characters as candidates for emoji. "
                    + "At the 2016Q2 UTC meeting, a final determination will be made for ones to be added to Unicode 9.0, "
                    + "for release in June, 2016. "
                    + "The candidates for 9.0 are limited to those accepted in 2015.</p>\n"
                    + "<p>The ones accepted in 2016 are candidates for Unicode 10.0, for release in June, 2017. "
                    + "For more information on the timeline, see "
                    + "<a href='http://unicode.org/emoji/selection.html#timeline'>Process and Timeline</a>. "
                    + "For information on the format of this chart, see the <a href='#notes'>notes</a>.</p>\n"
                    + "<ul style='color:#CC0000'>\n"
                    + "<li>Do <em>not</em> deploy any of these yet: they are not yet final!</li>\n"
                    + "<li>Candidates may still be removed or their code point, glyph, or name changed. </li>\n"
                    + "</ul>\n", "border='1'", false);
            for (String outputLine : output) {
                out.println(outputLine);
            }
            writeFooter(out, "<h3><a href='#notes' name='notes'>Notes</a></h3>"
                    + "<p>These candidates were based on proposals received by the Emoji "
                    + "Subcommittee and Unicode members, and selected on the basis of the "
                    + "Emoji <i>Selection Factors</i> in "
                    + "<a target='_blank' href='../../emoji/selection.html'>Submitting Emoji Character Proposals</a>. "
                    + "Anyone can file a proposal for a new emoji: see the instructions there.</p>\n"
                    + "<ul>"
                    + "<li>The cell-divisions (like <a href='#face-happy'>face-happy</a>) are as "
                    + "in <a href='emoji-ordering.html' target='order'>Emoji Ordering</a>.</li>\n"
                    + "<li>All of the images are <em>only</em> for illustration.</li>\n"
                    + "<li>The draft black and white <strong>Chart Glyphs</strong> are drafts for the Unicode charts. "
                    + "They may change. "
                    + "These drafts are courtesy of Adobe, Microsoft, Apple, and proposers "
                    + "(2016Q1 images from Yiying Lu and Maximilian Merz).</li>\n"
                    + "<li>The <strong>Sample Colored Glyphs</strong> column use a "
                    + "variety of different styles to illustrate some possible "
                    + "presentations. However, the actual presentations on phones and "
                    + "other devices are up to vendors, subject to the considerations in " + UTR_LINK + ". "
                    + "These samples are courtesy of EmojiXpress, Emojipedia.org, and proposers "
                    + "(2016Q1 images from Yiying Lu and Maximilian Merz).</li>\n"
                    + "<li>The <b>Date</b> column indicates the UTC meeting where each was accepted as a candidate.</li>\n"
                    + "<li>The <b>EMP</b> column has P when the draft Emoji_Presentation property specifies "
                    + "that the presentation should be colorful by default.</li>\n"
                    + "<li>The <b>EMB</b> column has B when the draft Emoji_Modifier_Base property specifies "
                    + "that the emoji can be used with a skin-tone modifier.</li>\n"
                    + "<li>The <b>Name</b> column has the draft Unicode name. "
                    + "The bulleted items below the name are clarifications or comments. "
                    + "Some of these may end "
                    + "up being reflected in Unicode chart annotations.</li>\n"
                    + "<li>The <b>Category</b>, such as "
                    + "<b><a href='http://www.unicode.org/emoji/charts/emoji-candidates.html#faces'>Faces</a></b>, "
                    + "is used as a basic grouping for later generation of the emoji sort order in "
                    + "<a href='http://cldr.unicode.org/'>CLDR</a>. "
                    + "There is a superscript ¹ on the characters that are not candidates for the upcoming release.</li>\n"
                    + "</ul>\n"
                    + "<p>For more information, see <a target='_blank' href='index.html'>Unicode Emoji</a> "
                    + "and the <a target='_blank' href='http://www.unicode.org/faq/emoji_dingbats.html'>Emoji FAQ</a>.</p>\n"
                    );
        }
    }
    //    static void showCopyingList() throws IOException {
    //        try (PrintWriter out = BagFormatter.openUTF8Writer(Emoji.TR51_INTERNAL_DIR, "emoji-copying.html")) {
    //            PrintWriter outText = null;
    //            int order = 0;
    //            UnicodeSet level1 = null;
    //            writeHeader(out, "Emoji for copying", null, "This chart provides a list of the Unicode emoji characters, ", "border='1'", true);
    //            int item = 0;
    //            out.println("<tr><td>");
    //            for (String s : SORTED_ALL_EMOJI_CHARS_SET) {
    //                out.println(getBestImage(s, false, "", Emoji.Source.color) + " ");
    //                if (outText != null) {
    //                    outText.println(toSemiString(s, order++, null));
    //                }
    //            }
    //            out.println("</td></tr>");
    //            writeFooter(out, "");
    //        }
    //    }
    public static String getButton() {
        return "<div class='aacButton' title='Show your support of Unicode'>"
                + "<a target='adopt' href='../../consortium/adopt-a-character.html'>"
                + "<img src='../../consortium/images/aac-button.png'></a>"
                + "</div>";
    }
    // <a href='http://www.unicode.org/cldr/trac/newticket?component=survey&amp;summary=Feedback+on+BOOTING+%3F'>Report Problem in Tool</a> 

    private static String getCldrTicket(String component, String summary) {
        return "<a target='cldr-ticket' href='http://unicode.org/cldr/trac/newticket"
                + "?component=" + fixUrl(component)
                + "&amp;summary=" + fixUrl(summary) 
                + "'>CLDR ticket</a>";
    }

    private static String fixUrl(String summary) {
        return summary.replace(' ', '+'); // TODO make more robust with % encoding
    }

    static final String UNICODE_HEADER = ""
            + "<div class='icon'>"
            + "<a href='http://www.unicode.org/'><img class='logo' alt='[Unicode]' src='http://www.unicode.org/webscripts/logo60s2.gif'></a>"
            + "<a class='bar' target='text' href='index.html'>Emoji Charts</a>"
            + "</div>"
            + "<div class='gray'>&nbsp;</div>"
            + "<div class='main'>";
}
