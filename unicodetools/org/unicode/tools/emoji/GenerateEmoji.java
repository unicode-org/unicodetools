package org.unicode.tools.emoji;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.tool.GenerateTransformCharts.CollectionOfComparablesComparator;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.TransliteratorUtilities;
import org.unicode.cldr.util.With;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.props.UcdPropertyValues.Block_Values;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.props.VersionToAge;
import org.unicode.text.UCD.NamesList;
import org.unicode.text.utility.Birelation;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.CandidateData.Quarter;
import org.unicode.tools.emoji.Emoji.Source;
import org.unicode.tools.emoji.EmojiData.DefaultPresentation;
import org.unicode.tools.emoji.EmojiOrder.MajorGroup;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.CollectionUtilities.SetComparator;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.EntryRange;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class GenerateEmoji {
    private static final String SINGLETONS_KEYCAPS_FLAGS = "It does not include emoji sequences, except for keycaps and flags. ";
    static boolean SHOW = false;
    static boolean DO_SPECIALS = false;
    private static final boolean DEBUG = false;
    private static final boolean SHOW_NAMES_LIST = false;

    static final boolean DATAURL = true;
    static final int RESIZE_IMAGE = -1;

    private static final String HEADER_NUM = "<th class='rchars'><a target='text' href='index.html#col-num'>№</a></th>\n";
    private static final String HEADER_COUNT = "<th class='rchars'><a target='text' href='index.html#col-count'>Count</a></th>\n";
    private static final String HEADER_CODE = "<th class='rchars'><a target='text' href='index.html#col-code'>Code</a></th>\n";
    private static final String HEADER_SAMPLE_IMAGE = "<th class='center'><a target='text' href='index.html#col-vendor'>Sample</a></th>\n";
    private static final String HEADER_SAMPLE_FALLBACK_IMAGES = "<th class='center'><a target='text' href='index.html#col-fallback'>Sample Fallback</a></th>\n";
    private static final String HEADER_SAMPLE_EMOJI = "<th class='narrow center'><a target='text' href='index.html#col-vendor'>Sample Emoji (+FE0F)</a</th>";
    private static final String HEADER_SAMPLE_TEXT = "<th class='narrow center'><a target='text' href='index.html#col-vendor'>Sample Text (+FE0E)</a></th>";
    private static final String HEADER_BROWSER = "<th class='cchars'><a target='text' href='index.html#col-browser'>Browser</a></th>"; 
    private static final String HEADER_NAME = "<th><a target='text' href='index.html#col-name'>CLDR Short Name</a></th>\n";
    private static final String HEADER_DATE = "<th><a target='text' href='index.html#col-date'>Date</a></th>\n";
    private static final String HEADER_KEYWORDS = "<th><a target='text' href='index.html#col-annotations'>Other Keywords</a></th>\n";
    private static final String HEADER_EMOJI = "<th><a target='text' href='index.html#col-emoji'>Emoji</a></th>";
    private static final String HEADER_SOURCES = "<th><a target='text' href='index.html#col-sources'>Sources</a></th>";
    private static final String TABLE_TOTALS = "</table>\n"
            + "<h2><a href='#totals' name='totals'>Totals</a></h2>\n"
            + "<p>Totals for the above emoji. For more information, see <a href='index.html#col-totals'>Totals</a>.</p>\n"
            + "<table>\n";

    private static final String BREAK = "<br>";
    private static final String TR51_HTML = "../../reports/tr51/" + (Emoji.USE_PROPOSED ? "proposed.html" : "index.html");

    static final Set<String> SKIP_WORDS = new HashSet<String>(
            Arrays.asList("with", "a", "in", "without", "and", "white", "symbol", "sign", "for", "of", "black"));

    static int MODIFIER_STATUS;

    private static boolean VERSION5PLUS = Emoji.VERSION_TO_GENERATE.compareTo(Emoji.VERSION5) >= 0;
    private static final String EMOJI_ZWJ_SEQUENCES_TITLE = VERSION5PLUS ? "Recommended Emoji ZWJ Sequences"
            : "Emoji ZWJ Sequences Catalog";
    private static final String CATALOG = VERSION5PLUS ? "list of recommended" : "catalog of";

    public static final EmojiData EMOJI_DATA_PREVIOUS = EmojiData.of(Emoji.VERSION_TO_GENERATE_PREVIOUS);
    public static UnicodeSet ARE_NEW = new UnicodeSet(EmojiData.EMOJI_DATA.getAllEmojiWithoutDefectives())
            .removeAll(EMOJI_DATA_PREVIOUS.getAllEmojiWithoutDefectives())
            .freeze();

    public static final UnicodeMap<String> TO_FIRST_VERSION_FOR_VARIANT = new UnicodeMap<>();
    static IndexUnicodeProperties IUP_LATEST = IndexUnicodeProperties.make();

    static {
        TO_FIRST_VERSION_FOR_VARIANT.putAll(EmojiData.EMOJI_DATA.getEmojiWithVariants(), "E");
        // override with versions from Unicode, where available.
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
                if (!EmojiData.EMOJI_DATA.getSingletonsWithDefectives().contains(first)) {
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
        for (String key : new UnicodeSet(EmojiData.EMOJI_DATA.getTextPresentationSet())
                .removeAll(Emoji.REGIONAL_INDICATORS).removeAll(EmojiData.EMOJI_DATA.getFlagSequences())) {
            final int first = key.codePointAt(0);
            String version = TO_FIRST_VERSION_FOR_VARIANT.get(first);
            if (version == null) {
                System.err.println("Missing from Standardized Variant: " + Utility.hex(key) + "\t" + key);
            }
        }
    }

    static {
        if (SHOW)
            System.out.println("🤧, " + Emoji.VERSION_ENUM.get("🤧"));
    }
    static final UnicodeMap<String> WHITESPACE = Emoji.LATEST.load(UcdProperty.White_Space);
    static final UnicodeMap<String> GENERAL_CATEGORY = Emoji.LATEST.load(UcdProperty.General_Category);
    static final UnicodeMap<General_Category_Values> GENERAL_CATEGORY_E = Emoji.LATEST
            .loadEnum(UcdProperty.General_Category, UcdPropertyValues.General_Category_Values.class);
    static final UnicodeMap<String> SCRIPT_EXTENSIONS = Emoji.LATEST.load(UcdProperty.Script_Extensions);
    private static final UnicodeSet COMMON_SCRIPT = new UnicodeSet()
            .addAll(SCRIPT_EXTENSIONS.getSet(UcdPropertyValues.Script_Values.Common.toString())).freeze();

    static final UnicodeMap<String> NFKCQC = Emoji.LATEST.load(UcdProperty.NFKD_Quick_Check);
    static final UnicodeSet JSOURCES = new UnicodeSet();
    static {
        UnicodeMap<String> dcmProp = Emoji.LATEST.load(UcdProperty.Emoji_DCM);
        UnicodeMap<String> kddiProp = Emoji.LATEST.load(UcdProperty.Emoji_KDDI);
        UnicodeMap<String> sbProp = Emoji.LATEST.load(UcdProperty.Emoji_SB);
        checkDuplicates(dcmProp, kddiProp, sbProp);
        JSOURCES.addAll(dcmProp.keySet())
        .addAll(kddiProp.keySet())
        .addAll(sbProp.keySet())
        .removeAll(WHITESPACE.getSet(UcdPropertyValues.Binary.Yes.toString()))
        // HACK
        .addAll(new UnicodeSet("[{0️⃣} {1️⃣} {2️⃣} {3️⃣} {4️⃣} {5️⃣} {6️⃣} {7️⃣} {8️⃣} {9️⃣} {#️⃣} {🇨🇳} {🇩🇪} {🇪🇸} {🇫🇷} {🇬🇧} {🇮🇹} {🇯🇵} {🇰🇷} {🇷🇺} {🇺🇸}]"))
        .freeze();
        // if (true)
        // System.out.println("Core:\t" + JSOURCES.size() + "\t" + JSOURCES);
    }
    static final Pattern tab = Pattern.compile("\t");
    static final Pattern space = Pattern.compile(" ");
    static final String REPLACEMENT_CHARACTER = "\uFFFD";

    // static final Comparator CODEPOINT_COMPARE_SHORTER =
    // new MultiComparator<String>(
    // Emoji.CODEPOINT_LENGTH,
    // mp,
    // UCA_COLLATOR, // don't
    // // need
    // // cldr
    // // features
    // new UTF16.StringComparator(true, false, 0));

    static final Set<String> SORTED_EMOJI_CHARS_SET = EmojiOrder.sort(EmojiOrder.STD_ORDER.codepointCompare,
            EmojiData.EMOJI_DATA.getChars());

    static final Set<String> SORTED_ALL_EMOJI_CHARS_SET = EmojiOrder.sort(EmojiOrder.STD_ORDER.codepointCompare,
            EmojiData.EMOJI_DATA.getAllEmojiWithoutDefectives());

    static {
        if (DEBUG) {
            System.out.println(
                    "{" + CollectionUtilities.join(EmojiData.EMOJI_DATA.getAllEmojiWithoutDefectives().strings(), "}{")
                    + "}");
            System.out.println(CollectionUtilities.join(SORTED_ALL_EMOJI_CHARS_SET, ""));
        }
    }

    // public static final UnicodeSet APPLE_COMBOS_WITHOUT_VS =
    // EmojiData.EMOJI_DATA.getZwjSequencesAll();

    static final Comparator<String> EMOJI_COMPARATOR = EmojiOrder.STD_ORDER.codepointCompare;
    // static {
    // try {
    // // EmojiOrder.sort(, new UnicodeSet(EmojiData.EMOJI_DATA.getChars());
    // // STD_ORDER.appendCollationRules(new StringBuilder(),
    // // new UnicodeSet(EmojiData.EMOJI_DATA.getChars())
    // //// EmojiData.EMOJI_DATA.getChars()).removeAll(Emoji.DEFECTIVE),
    // //// EmojiData.EMOJI_DATA.getZwjSequencesAll(),
    // //// EmojiData.EMOJI_DATA.getKeycapSequencesAll())
    // // ).toString();
    // // System.out.println(rules);
    // // final RuleBasedCollator ruleBasedCollator = new
    // RuleBasedCollator(rules);
    // // ruleBasedCollator.setStrength(Collator.IDENTICAL);
    // // ruleBasedCollator.freeze();
    // // EMOJI_COMPARATOR = (Comparator) ruleBasedCollator;
    // int x = EMOJI_COMPARATOR.compare("#️⃣","☺️");
    // } catch (Exception e) {
    // throw new ICUUncheckedIOException("Internal Error", e);
    // }
    // }

    // private static final EmojiAnnotations ANNOTATIONS_TO_CHARS_GROUPS = new
    // EmojiAnnotations(EMOJI_COMPARATOR,
    // "emojiAnnotationsGroupsSpecial.txt"
    // );
    // private static final UnicodeSet DEFAULT_TEXT_STYLE = new UnicodeSet()
    // .addAll(ANNOTATIONS_TO_CHARS_GROUPS.getValues("default-text-style"))
    // .freeze();

    // static final EmojiAnnotations ANNOTATIONS_TO_CHARS_NEW = new
    // EmojiAnnotations(CODEPOINT_COMPARE, "emojiAnnotationsNew.txt");

    // private static final Subheader subheader = new
    // Subheader(Settings.SVN_WORKSPACE_DIRECTORY +
    // "unicodetools/data/ucd/7.0.0-Update/");
    static final Set<String> SKIP_BLOCKS = new HashSet(
            Arrays.asList("Miscellaneous Symbols", "Enclosed Alphanumeric Supplement",
                    "Miscellaneous Symbols And Pictographs", "Miscellaneous Symbols And Arrows"));

    private static void checkDuplicates(UnicodeMap<String> dcmProp, UnicodeMap<String> kddiProp,
            UnicodeMap<String> sbProp) {
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
                        System.out.println(carrierAndUnicodes.getKey() + "\tU+" + Utility.hex(s, " U+") + "\t"
                                + UCharacter.getName(s, " + "));
                }
            }
        }
    }

    enum Style {
        plain, text, emoji, emojiFont, bestImage, refImage;

        public static Style fromString(String cp) {
            return EmojiData.EMOJI_DATA.getEmojiPresentationSet().contains(cp) ? Style.emoji : Style.text;
        }
    }

    static final Relation<Style, String> STYLE_TO_CHARS = Relation.of(new EnumMap(Style.class), TreeSet.class,
            EMOJI_COMPARATOR);

    static final UnicodeMap<Integer> DING_MAP = new UnicodeMap<>();
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
        person, body, face, nature, animal, plant, clothing, emotion, food, travel, vehicle, place, office, time, weather, game, sport, activity, object, sound, flag, arrow, word, sign,
        // unknown,
        ;

        static Label get(String string) {
            return Label.valueOf(string);
        }

        static final Comparator<Label> LABEL_COMPARE = new Comparator<Label>() {
            public int compare(Label o1, Label o2) {
                return o1.compareTo(o2);
            }
        };

        static final Birelation<String, Label> CHARS_TO_LABELS = Birelation.of(
                new TreeMap<String, Set<Label>>(EMOJI_COMPARATOR), new EnumMap<Label, Set<String>>(Label.class),
                TreeSet.class, TreeSet.class, LABEL_COMPARE, EMOJI_COMPARATOR);

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

    public static String getBestImage(String s, boolean useDataURL, String extraClasses, Emoji.Source... doFirst) {
        String result = getBestImageNothrow(s, s, useDataURL, extraClasses, doFirst);
        if (result == null) {
            result = getBestImageNothrow("\u2718", s, useDataURL, extraClasses, Source.ref);
            // throw new IllegalArgumentException("Can't find image for: " +
            // Utility.hex(s) + " " + Emoji.getName(s, true,
            // GenerateEmoji.EXTRA_NAMES) + "\t" + Emoji.buildFileName(s, "_"));
        }
        return result;
    }

    private static String getBestImageNothrow(String stringForFile, String s, boolean useDataURL, String extraClasses,
            Emoji.Source... doFirst) {
        if (s.equals("👦🏻")) {
            int debug = 0;
        }
        s = s.replace(Emoji.TEXT_VARIANT_STRING, "").replace(Emoji.TEXT_VARIANT_STRING, "");
        for (Emoji.Source source : Emoji.orderedEnum(doFirst)) {
            String cell = getImage(source, stringForFile, s, useDataURL, extraClasses);
            if (cell != null) {
                return cell;
            }
        }

        if (EmojiData.MODIFIERS.containsSome(s) && !EmojiData.MODIFIERS.containsAll(s)) {
            List<String> extracted = new ArrayList();
            String s2 = UnicodeSets.removeAll(EmojiData.MODIFIERS, s, extracted);
            for (Emoji.Source source : Emoji.orderedEnum(doFirst)) {
                String cell = getImage(source, s2, s, useDataURL, extraClasses);
                if (cell != null) {
                    for (String mod : extracted) {
                        String cell2 = getImage(Source.apple, mod, mod, useDataURL, " imgs");
                        cell += cell2;
                    }
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
        return getImage(type, chars, chars, useDataUrl, extraClasses);
    }

    /*
     * Note that Emoji.BESTOVERRIDE can override the source type for specific
     * characters.
     */
    static public String getImage(Emoji.Source type, String charsForFile, String chars, boolean useDataUrl,
            String extraClasses) {
        String filename = Emoji.getImageFilenameFromChars(type, charsForFile);
        if (filename != null && new File(Emoji.IMAGES_OUTPUT_DIR, filename).exists()) {
            String className = type.getClassAttribute(chars);
            // String className = "imga";
            // if (type == Source.ref && getFlagCode(chars) != null) {
            // className = "imgf";
            // }
            return "<img alt='" + chars + "'"
            + (useDataUrl
                    ? " class='" + className + extraClasses + (ARE_NEW.contains(chars) ? " new" : "") + "'"
                            // : " height=\"24\" width=\"auto\""
                            : " class='imga" + (ARE_NEW.contains(chars) ? " new" : "") + "'")
            + " src='" + (useDataUrl ? EmojiImageData.getDataUrl(filename) : "../images/" + filename) + "'"
            + " title='" + getCodeCharsAndName(chars, " ") + "'" + ">";
        }
        return null;
    }

    /**
     * @param extraPlatforms
     *            TODO
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

    // static final UnicodeSet VERSION70 =
    // VERSION.getSet(UcdPropertyValues.Age_Values.V7_0.toString());

    private static final Birelation<String, String> OLD_ANNOTATIONS_TO_CHARS = new Birelation(
            new TreeMap(EmojiOrder.FULL_COMPARATOR), new TreeMap(EMOJI_COMPARATOR), TreeSet.class, TreeSet.class,
            EMOJI_COMPARATOR, EmojiOrder.FULL_COMPARATOR);
    static {
        addOldAnnotations();
    }

    // private static void compareOtherAnnotations() {
    // for (Entry<String, Set<String>> entry :
    // OLD_ANNOTATIONS_TO_CHARS.valueKeysSet()) {
    // String chars = entry.getKey();
    // Set<String> oldAnnotations = entry.getValue();
    //
    // Set<String> newAnnotations = new
    // TreeSet(Utility.ifNull(EmojiAnnotations.ANNOTATIONS_TO_CHARS.getKeys(chars),
    // Collections.EMPTY_SET));
    // Set<Label> labels =
    // Utility.ifNull(Label.CHARS_TO_LABELS.getValues(chars),
    // Collections.EMPTY_SET);
    // for (Label label : labels) {
    // newAnnotations.add(label.toString());
    // }
    //
    // if (!Objects.equals(newAnnotations, oldAnnotations)) {
    // TreeSet oldNotNew = new TreeSet(oldAnnotations);
    // oldNotNew.removeAll(newAnnotations);
    // TreeSet newNotOld = new TreeSet(newAnnotations);
    // newNotOld.removeAll(oldAnnotations);
    // TreeSet both = new TreeSet(newAnnotations);
    // both.retainAll(oldAnnotations);
    // System.out.println(getCodeCharsAndName(chars, "\t")
    // + "\t" + CollectionUtilities.join(oldNotNew, ", ")
    // + "\t" + CollectionUtilities.join(newNotOld, ", ")
    // + "\t" + CollectionUtilities.join(both, ", ")
    // );
    // }
    // }
    // }

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
            final String emoji = realChars.toString();
            if (!EmojiData.EMOJI_DATA.getChars().contains(emoji)) {
                continue;
            }
            if (Emoji.NAME.get(realChars.codePointAt(0)) == null) {
                if (SHOW)
                    System.out.println("skipping private use: " + Integer.toHexString(realChars.codePointAt(0)));
                continue;
            }
            addWords(emoji, fields[1]);
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
        return cc == null ? null
                : "<img" + " alt='" + chars + "'" + " class='imgf" + extraClasses + "'" + " title='"
                + getCodeCharsAndName(chars, " ") + "'" + " src='" + EmojiImageData.getDataUrl(filename) + "'>";
    }

    public static void main(String[] args) throws IOException {
        FileUtilities.copyFile(GenerateEmoji.class, "emoji-list.css", Emoji.CHARTS_DIR);
        if (Emoji.IS_BETA) {
            GenerateEmojiData.printData(GenerateEmoji.EXTRA_NAMES);
        }
        String pointToOther = getPointToOther("index.html", "Unicode® Emoji Charts");
        String[] replacementList = new String[] {
                "%%PLAIN_VERSION%%", Emoji.VERSION_STRING + " " + Emoji.BETA_TITLE_AFFIX, // "v4.0 — Beta",
                "%%VERSION%%", Emoji.VERSION_STRING + Emoji.BETA_HEADER_AFFIX,
                "%%LINK_OTHER%%", pointToOther,
                "%%TR51_DATA%%", TR51_HTML + "#emoji_data", // "../../reports/tr51/proposed.html#emoji_data"
                "%%TR51_HTML%%", TR51_HTML, // "../../reports/tr51/proposed.html#emoji_data"
        };
        FileUtilities.copyFile(GenerateEmoji.class, "main-index.html", Emoji.CHARTS_DIR, "index.html", replacementList);

        showCandidateStyle(CandidateStyle.candidate, "emoji-candidates.html", UnicodeSet.EMPTY);

        UnicodeSet onlyNew = new UnicodeSet(EmojiData.EMOJI_DATA.getAllEmojiWithoutDefectives())
                .removeAll(EMOJI_DATA_PREVIOUS.getAllEmojiWithoutDefectives())
                // .removeAll(previousEmoji.getZwjSequencesAll()) // catch the eye
                // witness
                ;
        showCandidateStyle(CandidateStyle.released, "emoji-released.html", onlyNew);
        // once glyphs are available from vendors
        //		} else {
        //			print(Emoji.CHARTS_DIR, Form.onlyNew, "Emoji Recently Added", "emoji-released.html", onlyNew);
        //		}

        // show data
        if (SHOW)
            System.out.println("Total Emoji:\t" + EmojiData.EMOJI_DATA.getChars().size());
        // UnicodeSet newItems = new UnicodeSet();
        // newItems.addAll(EmojiData.EMOJI_DATA.getChars());
        // newItems.removeAll(JSOURCES);
        // UnicodeSet newItems70 = new
        // UnicodeSet(newItems).retainAll(VERSION70);
        // UnicodeSet newItems63 = new
        // UnicodeSet(newItems).removeAll(newItems70);
        // UnicodeSet newItems63flags = getStrings(newItems63);
        // newItems63.removeAll(newItems63flags);
        // if (SHOW) System.out.println("Other 6.3 Flags:\t" +
        // newItems63flags.size() + "\t" + newItems63flags);
        // if (SHOW) System.out.println("Other 6.3:\t" + newItems63.size() +
        // "\t" + newItems63);
        // if (SHOW) System.out.println("Other 7.0:\t" + newItems70.size() +
        // "\t" + newItems70);

        print(Emoji.CHARTS_DIR, Form.noImages, "Emoji Data", "emoji-list.html", null);
        // stats.write(Source.VENDOR_SOURCES);

        if (DO_SPECIALS) {
            final UnicodeSet male = new UnicodeSet("[👮  🕵 💂 👷 👳 👱 🙇 🚶 🏃🏌🏄🚣🏊⛹🏋🚴🚵🤼🤽🤾🤹]");
            print(Emoji.INTERNAL_OUTPUT_DIR, Form.fullForm, "Male Emoji Data", "male-emoji-list.html", male);
            final UnicodeSet female = new UnicodeSet("[🙍 🙎  🙅 🙆  💁  🙋  🤦 🤷 💆  💇  👯🤸]");
            print(Emoji.INTERNAL_OUTPUT_DIR, Form.fullForm, "Female Emoji Data", "female-emoji-list.html", female);
            final UnicodeSet otherGenderBases = new UnicodeSet(EmojiData.EMOJI_DATA.getGenderBases()).removeAll(male)
                    .removeAll(female);
            print(Emoji.INTERNAL_OUTPUT_DIR, Form.fullForm, "Missing Emoji Data", "missing-emoji-list.html",
                    otherGenderBases);
        }

        // print(Form.extraForm, missingMap, null);
        // showNewCharacters();
        for (String e : EmojiData.EMOJI_DATA.getChars()) {
            STYLE_TO_CHARS.put(Style.fromString(e), e);
        }
        STYLE_TO_CHARS.freeze();
        // printAnnotations();
        showTextStyle(Visibility.external);
        showOrdering(Style.bestImage, Visibility.external, false);
        // showOrdering(Style.refImage);
        // showLabels();
        showVersions();
        showVersionsOnly();
        showDefaultStyle();
        showVariationSequences();
        showSequences();
        //showAnnotations(Emoji.CHARTS_DIR, "emoji-annotations.html", EmojiData.EMOJI_DATA.getChars(), null, false);

        print(Emoji.CHARTS_DIR, Form.fullForm, "Full Emoji Data", "full-emoji-list.html", null);

        System.out.println("internal stuff");
        FileUtilities.copyFile(GenerateEmoji.class, "emoji-list.css", Emoji.TR51_INTERNAL_DIR);
        showOrdering(Style.bestImage, Visibility.internal, false);
        EmojiImageData.write(Source.VENDOR_SOURCES);
        printCollationOrder();
        showConstructedNames();

        // showAnnotationsDiff();
        // compareOtherAnnotations();
        // showOtherUnicode();
        // oldAnnotationDiff();
        // check twitter glyphs

        // if (SHOW) {
        // System.out.println("static final UnicodeSet EMOJI_CHARS = new
        // UnicodeSet(\n\"" + Data.DATA_CHARACTERS.toPattern(false) + "\");");
        // // getUrlCharacters("TWITTER", TWITTER_URL);
        // // getUrlCharacters("APPLE", APPLE_URL);
        // System.out.println(new
        // UnicodeSet(Emoji.GITHUB_APPLE_CHARS).removeAll(APPLE_CHARS).toPattern(false));
        // System.out.println(list(new
        // UnicodeSet(APPLE_CHARS).removeAll(Emoji.GITHUB_APPLE_CHARS)));
        // System.out.println("Apple: " + APPLE_CHARS);
        // }
        System.out.println("DONE");
    }

    private static void showConstructedNames() throws IOException {
        try (PrintWriter outText = FileUtilities.openUTF8Writer(Emoji.TR51_INTERNAL_DIR, "constructedNames.txt")) {
            for (String s : Iterables.concat(EmojiData.EMOJI_DATA.getChars(),
                    EmojiData.EMOJI_DATA.getModifierSequences(), EmojiData.EMOJI_DATA.getZwjSequencesNormal())) {
                int len = s.codePointCount(0, s.length());
                if (len > 1) {
                    outText.println(Utility.hex(s) + " ; " + EmojiData.EMOJI_DATA.getName(s));
                }
            }
        }
    }

    // static Set<Emoji.Source> MAIN_SOURCES =
    // Collections.unmodifiableSet(EnumSet.of(
    // Emoji.Source.apple, Emoji.Source.google, Emoji.Source.twitter,
    // Emoji.Source.windows));

    static final UnicodeSet otherStandard = Emoji.COMMON_ADDITIONS;
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

    static final UnicodeSet LEVEL1 = new UnicodeSet(EmojiData.JCARRIERS).addAll(otherStandard).freeze();

    static final UnicodeSet nc = new UnicodeSet(EmojiData.EMOJI_DATA.getChars()).removeAll(EmojiData.JCARRIERS)
            .removeAll(otherStandard).removeAll(EmojiData.EMOJI_DATA.getFlagSequences()).freeze();

    // static final UnicodeSet nc8 = new UnicodeSet(nc)
    // .removeAll(new UnicodeSet("[:age=7.0:]"))
    // .removeAll(nc.strings())
    // .freeze();
    //
    // static final UnicodeSet nc7 = new UnicodeSet(nc)
    // .removeAll(nc8)
    // .freeze();

    static final UnicodeSet otherFlags = new UnicodeSet(EmojiData.EMOJI_DATA.getFlagSequences())
            .removeAll(EmojiData.JCARRIERS).freeze();

    // private static void showNewCharacters() throws IOException {
    // Set<String> minimal =
    // EmojiData.EMOJI_DATA.getModifierStatusSet(ModifierStatus.modifier_base)
    // // .addAll(emojiData.getModifierStatusSet(ModifierStatus.primary))
    // .addAllTo(new TreeSet<String>(EMOJI_COMPARATOR)); //
    // ANNOTATIONS_TO_CHARS_GROUPS.getValues("fitz-secondary");
    //
    // // Set<String> newChars =
    // // ANNOTATIONS_TO_CHARS.getValues("fitz-minimal");
    // PrintWriter out = FileUtilities.openUTF8Writer(Emoji.TR51_INTERNAL_DIR,
    // "emoji-count.html");
    // writeHeader(out, "Groupings and counts for screenshots and UnicodeJsps",
    // null, "<p>no message</p>\n", "border='1' width='1200pt'", true);
    // showRow(out, "Modifier Base", minimal, true);
    // // UnicodeSet modifierBase = new
    // UnicodeSet().addAll(minimal).addAll(optional);
    // // showRow(out, "Modifier_Base", modifierBase.addAllTo(new
    // TreeSet<String>(CODEPOINT_COMPARE)), false);
    // final UnicodeSet MODIFIERS = new
    // UnicodeSet("[\\x{1F3FB}-\\x{1F3FF}]").freeze();
    // showRow(out, "Modifiers", MODIFIERS.addAllTo(new
    // TreeSet<String>(EMOJI_COMPARATOR)), false);
    // showRow(out, "JCarriers", EmojiData.JCARRIERS.addAllTo(new
    // TreeSet<String>(EMOJI_COMPARATOR)), true);
    // showRow(out, "Common Additions", otherStandard.addAllTo(new
    // TreeSet<String>(EMOJI_COMPARATOR)), true);
    // showRow(out, "Other Flags", otherFlags.addAllTo(new
    // TreeSet<String>(EMOJI_COMPARATOR)), true);
    // //showRow(out, "Standard Additions", nc.addAllTo(new
    // TreeSet<String>(EMOJI_COMPARATOR)), true);
    // // showRow(out, "Standard Additions8", nc8.addAllTo(new
    // TreeSet<String>(CODEPOINT_COMPARE)), true);
    //
    // // for unicodejsps
    // UnicodeSet singletons = new
    // UnicodeSet(EmojiData.EMOJI_DATA.getChars()).removeAll(EmojiData.EMOJI_DATA.getChars().strings());
    // showRowSet(out, "Singletons", singletons.addAllTo(new
    // TreeSet<String>(EMOJI_COMPARATOR)));
    // showRowSet(out, "ZWJ Sequences",
    // EmojiData.EMOJI_DATA.getZwjSequencesNormal().addAllTo(new
    // TreeSet<String>(EMOJI_COMPARATOR)));
    // showRowSet(out, "Modifier Sequences", getDiverse(minimal,
    // MODIFIERS).addAllTo(new TreeSet<String>(EMOJI_COMPARATOR)));
    //
    // UnicodeSet directional = new UnicodeSet("[☄☝⚰⛏⛴⛵⛷-⛹✈✊-✍✏✒🌀🌂🌊🌬
    // 🍳🍼🍾🎈🎉🎏🎒🎙🎠🎣-🎥🎧🎬🎯🎷🎸🎺🎻🎿🏁-🏄
    // 🏇🏊-🏏🏑-🏓🏳🏴🏷-🏹🐀-🐒🐕-🐘🐛-🐝
    // 🐟-🐣🐦🐧🐩-🐬🐲-🐴🐺🐿-👂👆👇👊-👏👞-👢
    // 👺💃💅💉💘💦💨💪💬💭💺📈📉📌📝📞📡-📣📪-📭
    // 📯📲📹📽🔇-🔊🔌🔦🔨🔪-🔭🕊🖊-🖍🖐🖕🖖🗜
    // 🗡🗣🗯🗿😾🚀-🚅🚈🚋🚌🚎🚐-🚗🚙-🚤🚩🚬🚲
    // 🚴-🚶🚽🚿-🛁🛋🛌🛏🛥🛩🛫🛬🛳🤘🦂-🦄]");
    // showRowSet(out, "Directional", directional.addAllTo(new
    // TreeSet<String>(EMOJI_COMPARATOR)));
    // // Set<String> face =
    // EmojiAnnotations.ANNOTATIONS_TO_CHARS.getValues("face");
    // // Set<String> optional_face = new HashSet<>(optional);
    // // optional_face.retainAll(face);
    // // Set<String> optional_other = new HashSet<>(optional);
    // // optional_other.removeAll(face);
    // // showRowSet(out, "Diverse Secondary Face", getDiverse(optional_face,
    // MODIFIERS).addAllTo(new TreeSet<String>(CODEPOINT_COMPARE)));
    // // showRowSet(out, "Diverse Secondary Other", getDiverse(optional_other,
    // MODIFIERS).addAllTo(new TreeSet<String>(CODEPOINT_COMPARE)));
    // // UnicodeSet keycapBase = new UnicodeSet();
    // // for (String s : Emoji.EMOJI_CHARS.strings()) {
    // // if (s.indexOf(Emoji.KEYCAP_MARK) > 0) {
    // // keycapBase.add(s.codePointAt(0));
    // // }
    // // }
    // // showRow(out, "KeycapBase", keycapBase.addAllTo(new
    // // TreeSet<String>(CODEPOINT_COMPARE)), true);
    // // showRow(out, "RegionalIndicators",
    // Emoji.REGIONAL_INDICATORS.addAllTo(new
    // TreeSet<String>(CODEPOINT_COMPARE)), true);
    // writeFooter(out, "");
    // out.close();
    // // main:
    // //
    // // for (String emoji : SORTED_EMOJI_CHARS_SET) {
    // // for (Source s : MAIN_SOURCES) {
    // // File f = getImageFile(s, emoji);
    // // if (f != null) {
    // // continue main;
    // // }
    // // }
    // // System.out.println(
    // // "U+" + Utility.hex(emoji,"&U+")
    // // + "\t" + emoji
    // // + "\t" + getName(emoji)
    // // + "\thttp://unicode.org/Public/emoji/1.0/full-emoji-list.html#"
    // // + Utility.hex(emoji,"_")
    // // );
    // // }
    // }

    private static UnicodeSet getDiverse(Set<String> minimal, final UnicodeSet MODIFIERS) {
        UnicodeSet primaryDiverse = new UnicodeSet();
        for (String item : minimal) {
            for (String tone : MODIFIERS) {
                primaryDiverse.add(item + tone);
            }
        }
        return primaryDiverse.freeze();
    }

    enum Visibility {
        internal, external
    }

    /**
     * Main Chart
     * 
     * @param visibility
     *            TODO
     */
    private static void showTextStyle(Visibility visibility) throws IOException {
        UnicodeSet defaultText = EmojiData.EMOJI_DATA.getTextPresentationSet();
        final String outFileName = "text-style.html";
        final PrintWriter out = visibility == Visibility.external ? FileUtilities.openUTF8Writer(Emoji.CHARTS_DIR, outFileName) : null;
        final PrintWriter out2 = visibility == Visibility.internal ? FileUtilities.openUTF8Writer(Emoji.TR51_INTERNAL_DIR, "text-vs.txt") : null;
        if (out != null)
            writeHeader(outFileName, out, "Text vs Emoji", null, "border='1'", true, false, 
                    "<p>This chart lists the emoji code points by date, "
                            + "showing for each character whether it has an emoji presentation (EP) by default "
                            + "and whether or it has an emoji presentation sequence (EPSq). "
                            + "There are no characters with an EP without an EPS (no “-EP -EPSq“).</p>\n"
                            + "<p>The notation “<i>year</i> ∩ Dings” indicates the Dingbats, Webdings, and Wingdings characters in <i>year</i>, "
                            + "since the Dings were a principle source of emoji with text presentation (-EP). "
                            + "The label “<i>year</i> ⊖ Dings” indicates other characters in <i>year</i>. "
                            + "</p>\n");
        UnicodeSet dings = new UnicodeSet(DINGBATS).addAll(DING_MAP.keySet())
                .retainAll(EmojiData.EMOJI_DATA.getSingletonsWithoutDefectives()).freeze();
        final UnicodeMap<Age_Values> VERSION = Emoji.LATEST.loadEnum(UcdProperty.Age,
                UcdPropertyValues.Age_Values.class);

        ArrayList<Age_Values> ordered = new ArrayList(Arrays.asList(Age_Values.values()));
        Collections.reverse(ordered);
        int max = 999; // force first header
        for (Age_Values version : ordered) {
            UnicodeSet current = VERSION.getSet(version)
                    .retainAll(EmojiData.EMOJI_DATA.getSingletonsWithoutDefectives());
            if (current.size() == 0) {
                continue;
            }
            UnicodeSet currentDings = new UnicodeSet(current).retainAll(dings);
            current.removeAll(dings);
            max = showTextRow(out, out2, version, true, current, defaultText, max);
            max = showTextRow(out, out2, version, false, currentDings, defaultText, max);
        }
        if (out != null) {
            writeFooter(out, "");
            out.close();
        }
        if (out2 != null) {
            out2.close();
        }
    }

    private static int showTextRow(PrintWriter outExternal, PrintWriter outInternal, Age_Values version,
            boolean minusDings, UnicodeSet current, UnicodeSet defaultText, int max) {
        if (current.size() == 0) {
            return max;
        }
        String versionString = VersionToAge.ucd.getYear(version)+"";
        //				Age_Values.Unassigned ? "v10.0"
        //				: version.toString().toLowerCase(Locale.ENGLISH).replace("_", ".");
        String title = versionString + (minusDings ? " <span style='color:gray'>⊖ Dings</span>" : " ∩ Dings");
        UnicodeSet emojiSet = new UnicodeSet(current).removeAll(defaultText)
                .removeAll(EmojiData.EMOJI_DATA.getEmojiWithVariants());
        UnicodeSet emojiSetVs = new UnicodeSet(current).removeAll(defaultText)
                .retainAll(EmojiData.EMOJI_DATA.getEmojiWithVariants());
        UnicodeSet textSet = new UnicodeSet(current).retainAll(defaultText)
                .removeAll(EmojiData.EMOJI_DATA.getEmojiWithVariants());
        UnicodeSet textSetVs = new UnicodeSet(current).retainAll(defaultText)
                .retainAll(EmojiData.EMOJI_DATA.getEmojiWithVariants());
        if (outExternal != null) {
            if (max > 50 && minusDings) {
                outExternal.println("<tr><th></th>"
                        + "<th width='30%' style='text-align: center'><i>+EP -EPSq</i></th>"
                        + "<th width='30%' style='text-align: center'><i>+EP +EPSq</i></th>"
                        + "<th width='30%' style='text-align: center'><i>-EP +EPSq</i></th>" + "</tr>");
                max = 0;
            }
            max += Math.max(emojiSet.size(), 
                    Math.max(emojiSetVs.size(), 
                            Math.max(textSet.size(), textSetVs.size())));
            outExternal.print("<tr><th>" + title + "</th><td>");
            // getImages(outExternal, textSet, true);
            // outExternal.print("</td><td>");
            getImages(outExternal, emojiSet, true);
            outExternal.print("</td><td>");
            getImages(outExternal, emojiSetVs, true);
            outExternal.print("</td><td>");
            getImages(outExternal, textSetVs, true);
            outExternal.print("</td></tr>");
        }
        if (outInternal != null) {
            if (textSet.isEmpty()) {
                return max;
            }
            outInternal.println("\n#\t" + versionString + (minusDings ? " ⊖ Dings" : " ∩ Dings") + "\n");
            for (String s : textSet) {
                // 2764 FE0E; text style; # HEAVY BLACK HEART
                // 2764 FE0F; emoji style; # HEAVY BLACK HEART
                outInternal.println(Utility.hex(s, " ") + " FE0E; text style;   # " + UCharacter.getName(s, "+"));
                outInternal.println(Utility.hex(s, " ") + " FE0F; emoji style;  # " + UCharacter.getName(s, "+"));
            }
        }
        return max;
    }

    private static void getImages(PrintWriter out, UnicodeSet textSet, boolean useDataUrl) {
        String link = IMAGE_MORE_INFO;
        for (String emoji : textSet.addAllTo(new TreeSet<String>(EMOJI_COMPARATOR))) {
            if (link != null) {
                out.print(getMoreInfoLink(link, emoji));
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
        // out.print("</td>\n<td width='10%'>");
        // showNames(out, minimal, abbreviate);
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
        return Emoji.toUHex(s) + " " + EmojiData.EMOJI_DATA.getName(s);
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

    // public static void addFileCodepoints(File imagesOutputDir, Map<String,
    // Data> results) {
    // for (File file : imagesOutputDir.listFiles()) {
    // String fileName = file.getName();
    // if (file.isDirectory()) {
    // if (!fileName.equals("other") && !fileName.equals("proposed") &&
    // !fileName.equals("sample")) {
    // addFileCodepoints(file, results);
    // }
    // continue;
    // }
    // String s = fileName;
    // String original = s;
    // if (s.startsWith(".") || !s.endsWith(".png") ||
    // s.contains("emoji-palette")
    // || s.contains("_200d")) { // ZWJ from new combos
    // continue;
    // }
    // String chars = Emoji.parseFileName(true, s);
    // if (chars.isEmpty()) { // resulting from _x codepoints
    // continue;
    // }
    // //checkNewItems(chars, fileName);
    // }
    // }

    // this.age = UcdPropertyValues.Age_Values.valueOf(age.replace('.', '_'));

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
        System.out.println("static final UnicodeSet " + title + "_CHARS = new UnicodeSet(\n\""
                + twitterChars.toPattern(false) + "\");");
    }

    static final UnicodeSet APPLE_CHARS = new UnicodeSet(
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

    // private static void showLabels() throws IOException {
    // PrintWriter out = FileUtilities.openUTF8Writer(Emoji.TR51_INTERNAL_DIR,
    // "emoji-labels.html");
    // writeHeader(out, "Emoji Labels", null, "<p>Main categories for character
    // picking. " +
    // "Characters may occur more than once. " +
    // "Categories could be grouped in the UI.</p>\n", "border='1'", true);
    // for (Entry<Label, Set<String>> entry :
    // Label.CHARS_TO_LABELS.valueKeysSet()) {
    // Label label = entry.getKey();
    // Set<String> set = entry.getValue();
    // String word = label.toString();
    // Set<String> values = entry.getValue();
    // UnicodeSet uset = new UnicodeSet().addAll(values);
    //
    // displayUnicodesetTD(out, Collections.singleton(word), null,
    // Collections.<String> emptySet(), uset, Style.bestImage, 16, null);
    // }
    // writeFooter(out, "");
    // out.close();
    // }

    /**
     * Main Chart
     * 
     * @param internalForCopy
     *            TODO
     * @param showUca
     *            TODO
     */
    private static void showOrdering(Style style, Visibility visibility, boolean showUca) throws IOException {
        final String outFileName = (style == Style.bestImage ? "" : "ref-") + "emoji-ordering.html";
        PrintWriter out = FileUtilities.openUTF8Writer(
                visibility == Visibility.internal ? Emoji.TR51_INTERNAL_DIR : Emoji.CHARTS_DIR, outFileName);
        writeHeader(outFileName, out, "Emoji Ordering", null,"border='1'", true, false, 
                "<p>This chart shows the default ordering of emoji characters from " + CLDR_DATA_LINK + ". "
                        + "The cell divisions " + (showUca ? "for Emoji Ordering " : "")
                        + "indicate the rough categories that are used to organize related characters together. "
                        + "The categories are broad and not exclusive: and any character will match multiple categories.</p>"
                        + "<p>The emoji modifier sequences are omitted for brevity, "
                        + "because they are simply ordered after their emoji modifier bases. "
                        + "In the CLDR collation rules, the emoji modifiers cause a secondary difference. See also the machine-readable files: "
                        + "<a target='text' href='emoji-ordering.txt'>emoji-ordering.txt</a>" + " and "
                        + "<a target='text' href='emoji-ordering-rules.txt'>emoji-ordering-rules.txt</a>. "
                        + "To make suggestions for improvements, please file a "
                        + getCldrTicket("collation", "Emoji ordering suggestions") + ".</p>\n");

        final Set<Entry<String, Set<String>>> keyValuesSet = EmojiOrder.STD_ORDER.orderingToCharacters.keyValuesSet();
        final int rows = keyValuesSet.size();
        UnicodeSet all = new UnicodeSet();
        boolean first = true;
        final int charsPerRow = -1;
        int totalSorted = 0;
        if (showUca) {
            out.println(
                    "<tr>" + "<th width='49%'>Emoji Ordering</th>" + "<th rowSpan='" + (rows + 1) * 2 + "'>&nbsp;</th>"
                    // + "<th width='33%'>With Chart Glyphs</th>"
                    // + "<th rowSpan='" + (rows + 1) + "'>&nbsp;</th>"
                    + "<th width='49%'>Default Unicode Collation Order</th>" + "</tr>");
            out.println("<tr><td><table>");
        }
        MajorGroup lastMajorGroup = null;
        for (Entry<String, Set<String>> entry : keyValuesSet) {

            final UnicodeSet values = new UnicodeSet().addAll(entry.getValue())
                    .retainAll(EmojiData.EMOJI_DATA.getAllEmojiWithoutDefectivesOrModifiers()).freeze();
            if (values.isEmpty()) {
                continue;
            }
            MajorGroup majorGroup = EmojiOrder.STD_ORDER.getMajorGroup(values);
            if (majorGroup != lastMajorGroup) {
                lastMajorGroup = majorGroup;
                out.println("<tr><th class='bighead'>" + getDoubleLink(majorGroup.toHTMLString()) + "</th></tr>");
            }
            out.println("<tr><th class='mediumhead'>" + getDoubleLink(entry.getKey()) + "</th></tr>");
            out.println("<tr>");
            all.addAll(values);
            totalSorted += values.size();
            displayUnicodeSet(out, values, Style.bestImage, charsPerRow, 1, 1, null, EMOJI_COMPARATOR, visibility);
            // displayUnicodeSet(out, values, Style.refImage, charsPerRow, 1, 1,
            // null, CODEPOINT_COMPARE);
            if (first) {
                first = false;
            }
            out.println("</tr>");
        }
        if (SHOW)
            System.out.println(totalSorted);
        if (showUca) {
            out.println("</table></td>");
            final UnicodeMap<Block_Values> blocks = Emoji.LATEST.loadEnum(UcdProperty.Block,
                    UcdPropertyValues.Block_Values.class);
            TreeSet<String> ucaOrder = new TreeSet<>(EmojiOrder.UCA_COLLATOR);
            all.addAllTo(ucaOrder);
            Block_Values lastBlock = null;
            UnicodeSet current = null;
            LinkedHashSet<Pair<Block_Values, UnicodeSet>> pairs = new LinkedHashSet<>();
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
            for (Pair<Block_Values, UnicodeSet> pair : pairs) {
                out.println("<tr><th>" + TransliteratorUtilities.toHTML.transform(pair.getFirst().toString())
                + "</th></tr>");
                out.println("<tr>");
                displayUnicodeSet(out, pair.getSecond(), Style.bestImage, charsPerRow, 1, 1, null,
                        EmojiOrder.UCA_COLLATOR, Visibility.external);
                out.println("</tr>");
            }
            out.println("</table></td></tr>");
        }
        writeFooter(out, "");
        out.close();
    }

    /** Main Chart */
    private static void showDefaultStyle() throws IOException {
        final String outFileName = "emoji-style.html";
        PrintWriter out = FileUtilities.openUTF8Writer(Emoji.CHARTS_DIR, outFileName);
        PrintWriter outText = FileUtilities.openUTF8Writer(Emoji.CHARTS_DIR, "emoji-style.txt");
        writeHeader(outFileName, out, "Emoji Default Style Values", null, "border='1'", true, false, 
                "<p>This chart provides a listing of characters for testing the display of emoji characters. "
                        + "Unlike the other charts, the emoji are presented as text rather than images, to show the style supplied in your browser. "
                        + "The text is given a gray color, so as to help distinguish the emoji presentation from a plain text presentation. "
                        + "</p>\n" + "<ul><li>"
                        + "The Plain subtable shows a listing of all emoji characters and sequences without an emoji font. "
                        + "The characters with a default “text” display are separated out, and shown with and without presentation selectors. "
                        + "</li>\n<li>"
                        + "The Emoji subtable lists the same characters but with common color fonts on the text, to show a colorful display if possible. "
                        + "</li></ul>\n" + "<ul><li>"
                        + "“-vs” indicates that emoji varation selectors <i>are not</i> present, and a character has Emoji_Presentation=False."
                        + "</li>\n<li>"
                        + "“+vs” indicates that emoji varation selectors <i>are</i> present, and a character has Emoji_Presentation=False."
                        + "</ul>\n" + "<p>The default presentation choice (colorful vs gray) is discussed in "
                        + "<a href='" + TR51_HTML + "#Presentation_Style'>Presentation Style</a>. "
                        + "See also <a target='variants' href='emoji-variants.html'>Emoji Presentation Sequences</a>.</p>\n");
        outText.println(
                "\uFEFF" + "Emoji Default Style Values, v" + Emoji.VERSION_STRING + Emoji.BETA_TITLE_AFFIX + "\n"
                        + "This text file provides a listing of characters for testing the display of emoji characters.\n"
                        + "• “-vs” indicates that emoji presentation selectors are not present, and a character has Emoji_Presentation=False.\n"
                        + "• “+vs” indicates that emoji presentation selectors are present, and a character has Emoji_Presentation=False.\n"
                        + "For more information on presentation style, see UTS #51.");

        final String mainMessage = "Should all be colorful, except where marked with “text-vs”.";
        out.println("<tr><th colSpan='3'>Plain: " + mainMessage + "</th></tr>");
        outText.println("\n" + mainMessage);

        showText(out, outText, Style.plain);

        out.println("<tr><th colSpan='3'>Emoji Font: should all be colorful, even “text-vs”.</th></tr>");
        showText(out, null, Style.emojiFont);

        // out.println("<tr><th colSpan='3'>Emoji Locale (should all be
        // colorful)</th></tr>");
        // showText(out, null, Style.plain);

        // sr-Latn-u-em-emoji

        writeFooter(out, "");
        out.close();
        outText.close();
    }

    private static void showText(PrintWriter out, PrintWriter outText, Style style) {
        UnicodeSet dull = new UnicodeSet();
        UnicodeSet colorful = new UnicodeSet();
        UnicodeSet colorfulWithVs = new UnicodeSet();
        UnicodeSet modsWoVs = new UnicodeSet();
        UnicodeSet modsWithVs = new UnicodeSet();
        UnicodeSet mods = new UnicodeSet();
        UnicodeSet zwjWoVs = new UnicodeSet();
        UnicodeSet zwjWithVs = new UnicodeSet();
        UnicodeSet zwj = new UnicodeSet();

        for (String s : EmojiData.EMOJI_DATA.getChars()) {
            if (s.equals("👮‍♂️")) {
                int debug = 0;
            }
            String nvs = s.replace(Emoji.EMOJI_VARIANT_STRING, "");
            String vs = EmojiData.EMOJI_DATA.addEmojiVariants(nvs);
            if (nvs.contains(Emoji.JOINER_STRING)) {
                if (nvs.equals(vs)) {
                    zwj.add(nvs);
                } else {
                    zwjWithVs.add(vs);
                    zwjWoVs.add(nvs);
                }
            } else if (EmojiData.MODIFIERS.containsSome(nvs)) {
                mods.add(nvs);
            } else if (nvs.equals(vs)) {
                colorful.add(nvs);
            } else {
                dull.add(nvs);
                colorfulWithVs.add(vs);
            }
        }
        // otherwise text

        showText("text-vs", dull, out, outText, style);
        showText("text+vs", colorfulWithVs, out, outText, style);
        showText("emoji", colorful, out, outText, style);
        showText("modifier", mods, out, outText, style);
        showText("zwj-vs", zwjWoVs, out, outText, style);
        showText("zwj+vs", zwjWithVs, out, outText, style);
        showText("zwj emoji", zwj, out, outText, style);
    }

    private static void showText(String title, UnicodeSet dull, PrintWriter out, PrintWriter outText, Style style) {
        displayUnicodesetTD(out, Collections.singleton(title), style.toString(), Collections.<String>emptySet(), dull,
                style, -1, null, Visibility.external);
        if (outText != null) {
            outText.println("\n" + title);
            outText.println(textList(dull));
        }
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
        boolean hasExtra = Emoji.VERSION_TO_GENERATE.compareTo(Emoji.VERSION4) >= 0;
        final String outFileName = "emoji-variants.html";
        PrintWriter out = FileUtilities.openUTF8Writer(Emoji.CHARTS_DIR, outFileName);
        writeHeader(outFileName, out, "Emoji Presentation Sequences", "", "border='1'", true, false, 
                "<p>This chart specifies the exact list of <i>emoji presentation sequences</i> and <i>text presentation sequences</i>.\n"
                        + "Sample glyphs are provided to illustrate the contrast between "
                        + "the desired appearances for each of these sequences.\n"
                        + "For more information, see "
                        + "<i><a target='doc' href='" + TR51_HTML + "#Definitions'>Definitions</a></i> "
                        + "in <i><a target='doc' href='" + TR51_HTML + "'>UTS #51: Unicode Emoji</a></i>. " + "</p>\n"
                        + "<p>"
                        + "The Version column shows the first version of Unicode containing "
                        + "the standard presentation sequence.\n"
                        + "It is not the version in which the <i>emoji base</i> was added to Unicode, "
                        + "but rather the one in which the <i>emoji presentation sequence</i> was first defined.\n"
                        + "Unlike the other emoji charts, the names are the standard Unicode character names.\n"
                        + (hasExtra
                                ? "The version “E” indicates a sequence that is added as a part of Unicode Emoji 4.0 or later. "
                                        : "")
                        + "Keycap images (those with a * on the Name) are for sequences "
                        + "followed by U+20E3 COMBINING ENCLOSING KEYCAP. </p>\n");

        out.println("<tr>"
                + HEADER_CODE // + "<th class='cchars'>Code</th>" 
                + HEADER_SAMPLE_TEXT
                + HEADER_SAMPLE_EMOJI
                + HEADER_DATE // + "<th>Version</th>" 
                + HEADER_NAME
                + "</tr>");
        final String keycapIndicator = "*";
        TreeSet<String> sorted = new TreeSet<>(EmojiOrder.PLAIN_STRING_COMPARATOR);
        EmojiData.EMOJI_DATA.getEmojiWithVariants().addAllTo(sorted);
        for (String cp : sorted) {
            String version = TO_FIRST_VERSION_FOR_VARIANT.get(cp);
            // if (version == null) {
            // version = "E4.0";
            // }
            out.println("<tr>");
            out.println("<td class='code cchars'>" + getDoubleLink(Utility.hex(cp)) + "</td>");
            if (EmojiData.EMOJI_DATA.getKeycapBases().contains(cp)) {
                // keycaps, treat specially
                String cp2 = cp + Emoji.KEYCAP_MARK;
                out.println(GenerateEmoji.getCell(Emoji.Source.ref, cp2, "andr", false, null));
                out.println(GenerateEmoji.getCell(null, cp2, "andr", false, null));
                out.println("<td>" + version + "</td>");
                out.println("<td>" + UCharacter.getName(cp.codePointAt(0)) + keycapIndicator + "</td>");
            } else {
                out.println(GenerateEmoji.getCell(Emoji.Source.ref, cp, "andr", false, null));
                out.println(GenerateEmoji.getCell(null, cp, "andr", false, null));
                out.println("<td>" + version + "</td>");
                out.println("<td>" + UCharacter.getName(cp.codePointAt(0)) + "</td>");
            }
            out.println("</tr>");
        }
        writeFooter(out, "");
        out.close();
    }

    /** Main Chart */
    private static void showSequences() throws IOException {
        String outFileName = "emoji-sequences.html";
        try (PrintWriter out = FileUtilities.openUTF8Writer(Emoji.CHARTS_DIR, outFileName)) {
            writeHeader(outFileName, out, "Emoji Sequences", null, "border='1'", true, false, 
                    "<p>This chart provides a list of sequences of emoji characters, "
                            + "including <a href='#keycaps'>keycap sequences</a>, " + "<a href='#flags'>flags</a>, and "
                            + "<a href='#modifier_sequences'>modifier sequences</a>. "
                            + "For presentation sequences, see <a href='emoji-style.html'>Emoji Default Style Values</a>. "
                            + "For a " + CATALOG + " emoji zwj sequences, see <a href='emoji-zwj-sequences.html'>"
                            + EMOJI_ZWJ_SEQUENCES_TITLE + "</a>. </p>\n");

            displayUnicodesetTD(out, Collections.singleton("keycaps"), null,
                    Collections.singleton("" + Emoji.KEYCAPS.size()), Emoji.KEYCAPS, Style.bestImage, -1, null,
                    Visibility.external);
            displayUnicodesetTD(out, Collections.singleton("flags"), null,
                    Collections.singleton("" + EmojiData.EMOJI_DATA.getFlagSequences().size()),
                    EmojiData.EMOJI_DATA.getFlagSequences(), Style.bestImage, -1, null, Visibility.external);
            displayUnicodesetTD(out, Collections.singleton("modifier sequences"), null,
                    Collections.singleton("" + EmojiData.EMOJI_DATA.getModifierSequences().size()),
                    EmojiData.EMOJI_DATA.getModifierSequences(), Style.bestImage, -1, null, Visibility.external);

            writeFooter(out, "");
        }

        outFileName = "emoji-zwj-sequences.html";
        try (PrintWriter out = FileUtilities.openUTF8Writer(Emoji.CHARTS_DIR, outFileName)) {
            writeHeader(outFileName, out, EMOJI_ZWJ_SEQUENCES_TITLE, null, "border='1'", true, false, 
                    "<p>The following are the recommended <i>emoji zwj sequences</i>, "
                            + "which use a U+200D ZERO WIDTH JOINER (ZWJ) to join the characters into a single glyph if available. "
                            + "When not available, the ZWJ characters are ignored and "
                            + "a fallback sequence of separate emoji is displayed. "
                            + "Thus an emoji zwj sequence should only be supported where the fallback sequence "
                            + "would also make sense to a viewer."
                            + "</p>\n");
            displayZwjTD(out, "ZWJ sequences",
                    new UnicodeSet("[💏💑👪]").addAll(EmojiData.EMOJI_DATA.getZwjSequencesNormal()));
            // displayZwjTD(out, "ZWJ sequences: No VS",
            // Emoji.APPLE_COMBOS_WITHOUT_VS);

            writeFooter(out, "");
        }
    }

    private static void displayZwjTD(PrintWriter out, String title, UnicodeSet items) {
        out.println("<tr><th colSpan='6'>" + TransliteratorUtilities.toHTML.transform(title) + "</th></tr>");
        out.println(
                "<tr>"
                        + HEADER_NUM // "<th>№</th>"
                        + HEADER_CODE // "<th style='width:10em'>Code Points</th>"
                        + HEADER_BROWSER
                        + HEADER_SAMPLE_IMAGE // "<th>Sample Image</th>"
                        + HEADER_SAMPLE_FALLBACK_IMAGES
                        + HEADER_NAME
                        + "</tr>");
        // StringBuffer name = new StringBuffer();
        // String prefix = "";
        int i = 0;
        StringBuilder buffer = new StringBuilder();
        for (String s : EmojiOrder.sort(EMOJI_COMPARATOR, items)) {
            if (s.startsWith("\u200d")) {
                int debug = 0;
            }
            boolean isSingle = s.codePointCount(0, s.length()) == 1;
            out.println("<tr>");
            out.println(getCellNum(++i));
            out.println(getCellCode(s));
            out.println("<td class='chars'>" + s + "</td>");
            final String bestImage = getBestImage(s, true, "");
            out.println("<td class='andr'>" + bestImage + "</td>");
            if (buffer.length() != 0) {
                buffer.append(isSingle || s.startsWith("👁") ? "<br>" : " ");
            }
            buffer.append(bestImage);
            out.println("<td class='andr'>");
            // name.setLength(0);
            for (String part : s.replace(Emoji.EMOJI_VARIANT_STRING, "").split(Emoji.JOINER_STRING)) {
                out.println(getBestImage(part, true, ""));
            }
            // for (int item : CharSequences.codePoints(s)) {
            // if (Emoji.EMOJI_VARIANTS_JOINER.contains(item)) {
            // continue;
            // }
            // //if (Emoji.EMOJI_CHARS.contains(item)) {
            // String s2 = UTF16.valueOf(item);
            // out.println(getBestImage(s2, true, ""));
            // // if (name.length() != 0) {
            // // name.append();
            // // }
            // // name.append(getName(s2, true));
            // //}
            // }
            out.println("</td>");
            // if (isSingle) {
            // prefix = name + ": <i>";
            // } else {
            // name.insert(0, prefix);
            // name.append("</i>");
            // }
            out.println("<td>" + EmojiData.EMOJI_DATA.getName(s) + "</td>");
            out.println("</tr>");
        }
        out.println("<tr><th colSpan='6'>Character list for copying</th></tr>");
        out.println("<tr><td colSpan='6'>" + buffer + "</td></tr>");
    }

    static final UnicodeSet ARIB = new UnicodeSet(
            "[²³¼-¾࿖‼⁉ℓ№℡℻⅐-⅛Ⅰ-Ⅻ↉ ①-⑿⒈-⒓ⒹⓈ⓫⓬▶◀☀-☃☎☓☔☖☗♠ ♣♥♦♨♬⚓⚞⚟⚡⚾⚿⛄-⛿✈❶-❿➡⟐⨀ ⬅-⬇⬛⬤⬮⬯〒〖〗〶㈪-㈳㈶㈷㈹㉄-㉏㉑-㉛ ㊋㊙�㍱㍻-㍾㎏㎐㎝㎞㎠-㎢㎤㎥㏊円年日月 🄀-🄊🄐-🄭🄱🄽🄿🅂🅆🅊-🅏🅗🅟🅹🅻🅼🅿🆊-🆍 🈀🈐-🈰🉀-🉈]")
            .freeze();
    static final UnicodeSet DINGBATS = new UnicodeSet(
            "[\u2194\u2195\u260E\u261B\u261E\u2660\u2663\u2665\u2666\u2701-\u2704\u2706-\u2709\u270C-\u2712\u2714-\u2718\u2733\u2734\u2744\u2747\u2762-\u2767\u27A1]")
            .freeze();

    static class VersionData implements Comparable<VersionData> {
        final Age_Values versionInfo;
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
                    .compare(o.versionInfo, versionInfo)
                    .compare(setCharSource, o.setCharSource, ccc)
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
        final String outFileName = "emoji-versions-sources.html";
        PrintWriter out = FileUtilities.openUTF8Writer(Emoji.CHARTS_DIR, outFileName);
        writeHeader(outFileName, out, "Emoji Versions &amp; Sources", null, "border='1'", true, false, 
                "<p>This chart shows when each emoji <i>code point</i> first appeared in a Unicode version, "
                        + "and which "
                        + "sources the character corresponds to. "
                        + "For example, “ZDings+ARIB+JCarrier” indicates that the character also appears in the Zapf Dingbats, "
                        + "the ARIB set, and the Japanese Carrier set. "
                        + SINGLETONS_KEYCAPS_FLAGS
                        + "</p>\n");
        out.println("<tr>"
                + HEADER_DATE
                + HEADER_SOURCES
                + HEADER_COUNT
                + HEADER_EMOJI
                + "</tr>");
        UnicodeMap<VersionData> m = new UnicodeMap<>();
        TreeSet<VersionData> sorted = getSortedVersionInfo(m);
        for (VersionData value : sorted) {
            UnicodeSet chars = m.getSet(value);
            chars = new UnicodeSet(chars).retainAll(getCharsForVersion());
            if (chars.isEmpty()) {
                continue;
            }
            displayUnicodesetTD(out, 
                    ImmutableSet.of(VersionToAge.ucd.getYear(value.versionInfo)), // 
                    null,
                    ImmutableSet.of(
                            value.getCharSources(),
                            String.valueOf(chars.size())),
                    chars, Style.bestImage, -1, null, Visibility.external);
        }
        writeFooter(out, "");
        out.close();
    }

    private static UnicodeSet getCharsForVersion() {
        return new UnicodeSet()
                .addAll(EmojiData.EMOJI_DATA.getSingletonsWithDefectives())
                .addAll(EmojiData.EMOJI_DATA.getFlagSequences())
                .addAll(EmojiData.EMOJI_DATA.getKeycapSequences())
                .removeAll(EmojiData.EMOJI_DATA.getEmojiComponents())
                .freeze();
    }

    public static TreeSet<VersionData> getSortedVersionInfo(UnicodeMap<VersionData> m) {
        for (String s : EmojiData.EMOJI_DATA.getAllEmojiWithoutDefectivesOrModifiers()) {
            m.put(s, new VersionData(s));
        }
        TreeSet<VersionData> sorted = new TreeSet<>(m.values());
        return sorted;
    }

    /** Main Chart */
    private static void showVersionsOnly() throws IOException {
        final String outFileName = "emoji-versions.html";
        PrintWriter out = FileUtilities.openUTF8Writer(Emoji.CHARTS_DIR, outFileName);
        writeHeader(outFileName, out, "Emoji Versions", null, "border='1'", true, false, 
                "<p>This chart shows when each emoji character first appeared in a Unicode version. "
                        + SINGLETONS_KEYCAPS_FLAGS
                        + "</p>\n");
        UnicodeMap<Integer> m = new UnicodeMap<>();

        for (String s : getCharsForVersion()) {
            m.put(s, VersionToAge.ucd.getYear(Emoji.getNewest(s)));
        }
        TreeSet<Integer> sorted = new TreeSet<>(Collections.reverseOrder());
        sorted.addAll(m.values());
        out.println("<tr>"
                + HEADER_DATE
                + HEADER_COUNT
                + HEADER_EMOJI
                + "</tr>");
        for (Integer value : sorted) {
            UnicodeSet chars = m.getSet(value);
            displayUnicodesetTD(out, Collections.singleton(value), null,
                    Collections.singleton(String.valueOf(chars.size())), chars, Style.bestImage, -1, null,
                    Visibility.external);
        }
        writeFooter(out, "");
        out.close();
    }

    // private static void showSubhead() throws IOException {
    // Map<String, UnicodeSet> subheadToChars = new TreeMap();
    // for (String s : GenerateEmoji.emojiData.getChars()) {
    // int firstCodepoint = s.codePointAt(0);
    // String header = Default.ucd().getBlock(firstCodepoint).replace('_', ' ');
    // String subhead = subheader.getSubheader(firstCodepoint);
    // if (subhead == null) {
    // subhead = "UNNAMED";
    // }
    // header = header.contains(subhead) ? header : header + ": " + subhead;
    // UnicodeSet uset = subheadToChars.get(header);
    // if (uset == null) {
    // subheadToChars.put(header, uset = new UnicodeSet());
    // }
    // uset.add(s);
    // }
    // PrintWriter out = FileUtilities.openUTF8Writer(Emoji.CHARTS_DIR,
    // "emoji-subhead.html");
    // writeHeader(out, "Emoji Subhead", null, "Unicode Subhead mapping.",
    // "border='1'", true);
    // for (Entry<String, UnicodeSet> entry : subheadToChars.entrySet()) {
    // String label = entry.getKey();
    // UnicodeSet uset = entry.getValue();
    // if (label.equalsIgnoreCase("exclude")) {
    // continue;
    // }
    // displayUnicodesetTD(out, Collections.singleton(label), null,
    // Collections.<String> emptySet(), uset, Style.emoji, 16, null);
    // }
    // writeFooter(out, "");
    // out.close();
    // }

    /** Main charts */
    private static void showAnnotations(String dir, String outFileName, UnicodeSet filterOut,
            Set<String> retainAnnotations, boolean removeInsteadOf) throws IOException {
        try (PrintWriter out = FileUtilities.openUTF8Writer(dir, outFileName)) {
            writeHeader(outFileName, out, "Emoji Annotations", null, "border='1'", true, false, 
                    "<p>This chart shows the English emoji character annotations based on " + CLDR_ANNOTATIONS_LINK
                    + ". "
                    + "It does not include the annotations or short names that are algorithmically generated for sequences, such as flags. "
                    + "To make suggestions for improvements, " + "please file a "
                    + getCldrTicket("annotations", "Emoji annotation suggestions") + ".</p>\n");

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
                    displayUnicodesetTD(out, words, null, Collections.<String>emptySet(), filtered, Style.bestImage, -1,
                            IMAGE_MORE_INFO, Visibility.external);
                }
            }
            writeFooter(out, "");
        }
    }

    // private static void showAnnotationsBySize(String dir, String filename,
    // UnicodeSet retainSet) throws IOException {
    // PrintWriter out = FileUtilities.openUTF8Writer(dir, filename);
    // writeHeader(out, "Emoji Annotations", null, "<p>Finer-grained character
    // annotations. </p>\n", "border='1'", true);
    // TreeSet<Row.R3<Integer, UnicodeSet, String>> sorted = new TreeSet<>();
    // Relation<UnicodeSet, String> usToAnnotations = Relation.of(new HashMap(),
    // TreeSet.class, EmojiOrder.UCA_COLLATOR);
    // for (Entry<String, Set<String>> entry :
    // EmojiAnnotations.ANNOTATIONS_TO_CHARS.keyValuesSet()) {
    // String word = entry.getKey();
    // if (EmojiAnnotations.GROUP_ANNOTATIONS.contains(word)) {
    // continue;
    // }
    // Set<String> values = entry.getValue();
    // UnicodeSet uset = new UnicodeSet().addAll(values);
    // UnicodeSet filtered = new UnicodeSet(uset).retainAll(retainSet);
    // if (filtered.isEmpty()) {
    // continue;
    // }
    // sorted.add(Row.of(-filtered.size(), filtered, word));
    // usToAnnotations.put(filtered, word);
    // }
    // Set<String> seenAlready = new HashSet<>();
    // for (R3<Integer, UnicodeSet, String> entry : sorted) {
    // String word = entry.get2();
    // if (seenAlready.contains(word)) {
    // continue;
    // }
    // UnicodeSet uset = entry.get1();
    // Set<String> allWords = usToAnnotations.get(uset);
    // displayUnicodesetTD(out, allWords, null, Collections.<String> emptySet(),
    // uset, Style.bestImage, 16, "full-emoji-list.html");
    // seenAlready.addAll(allWords);
    // }
    // writeFooter(out, "");
    // out.close();
    // }

    // private static void showAnnotationsDiff() throws IOException {
    // PrintWriter out = FileUtilities.openUTF8Writer(Emoji.OUTPUT_DIR,
    // "emoji-annotations-diff.html");
    // writeHeader(out, "Emoji Annotations Diff",
    // "Finer-grained character annotations. " +
    // "For brevity, flags are not shown: they would have names of the
    // associated countries.");
    // out.println("<tr><th>Code</th><th>Image</th><th>Name</th><th>Old-Only</th><th>New-Only</th><th>Same
    // Annotation</th></tr>");
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

    // private static void showOtherUnicode() throws IOException {
    // Map<String, UnicodeSet> labelToUnicodeSet = new TreeMap();
    //
    // getLabels("otherLabels.txt", labelToUnicodeSet);
    // getLabels("otherLabelsComputed.txt", labelToUnicodeSet);
    // UnicodeSet symbolMath =
    // Emoji.LATEST.load(UcdProperty.Math).getSet(Binary.Yes.toString());
    // UnicodeSet symbolMathAlphanum = new UnicodeSet()
    // .addAll(Emoji.LATEST.load(UcdProperty.Alphabetic).getSet(Binary.Yes.toString()))
    // .addAll(GENERAL_CATEGORY.getSet(General_Category_Values.Decimal_Number.toString()))
    // .addAll(GENERAL_CATEGORY.getSet(General_Category_Values.Letter_Number.toString()))
    // .addAll(GENERAL_CATEGORY.getSet(General_Category_Values.Other_Number.toString()))
    // .retainAll(symbolMath);
    // symbolMath.removeAll(symbolMathAlphanum);
    // addSet(labelToUnicodeSet, "Symbol-Math", symbolMath);
    // addSet(labelToUnicodeSet, "Symbol-Math-Alphanum", symbolMathAlphanum);
    // addSet(labelToUnicodeSet, "Symbol-Braille",
    // Emoji.LATEST.load(UcdProperty.Block).getSet(Block_Values.Braille_Patterns.toString()));
    // addSet(labelToUnicodeSet, "Symbol-APL", new UnicodeSet("[⌶-⍺ ⎕]"));
    //
    // UnicodeSet otherSymbols = new UnicodeSet()
    // .addAll(GENERAL_CATEGORY.getSet(General_Category_Values.Math_Symbol.toString()))
    // .addAll(GENERAL_CATEGORY.getSet(General_Category_Values.Other_Symbol.toString()))
    // .removeAll(NFKCQC.getSet(Binary.No.toString()))
    // .removeAll(EmojiData.EMOJI_DATA.getChars())
    // .retainAll(COMMON_SCRIPT);
    // ;
    // UnicodeSet otherPunctuation = new UnicodeSet()
    // .addAll(GENERAL_CATEGORY.getSet(General_Category_Values.Close_Punctuation.toString()))
    // .addAll(GENERAL_CATEGORY.getSet(General_Category_Values.Connector_Punctuation.toString()))
    // .addAll(GENERAL_CATEGORY.getSet(General_Category_Values.Dash_Punctuation.toString()))
    // .addAll(GENERAL_CATEGORY.getSet(General_Category_Values.Final_Punctuation.toString()))
    // .addAll(GENERAL_CATEGORY.getSet(General_Category_Values.Initial_Punctuation.toString()))
    // .addAll(GENERAL_CATEGORY.getSet(General_Category_Values.Math_Symbol.toString()))
    // .addAll(GENERAL_CATEGORY.getSet(General_Category_Values.Open_Punctuation.toString()))
    // .addAll(GENERAL_CATEGORY.getSet(General_Category_Values.Other_Punctuation.toString()))
    // .removeAll(NFKCQC.getSet(Binary.No.toString()))
    // .removeAll(EmojiData.EMOJI_DATA.getChars())
    // .retainAll(COMMON_SCRIPT);
    // ;
    //
    // for (Entry<String, UnicodeSet> entry : labelToUnicodeSet.entrySet()) {
    // UnicodeSet uset = entry.getValue();
    // uset.removeAll(EmojiData.EMOJI_DATA.getChars());
    // otherSymbols.removeAll(uset);
    // otherPunctuation.removeAll(uset);
    // }
    // if (!otherPunctuation.isEmpty()) {
    // addSet(labelToUnicodeSet, "Punctuation-Other", otherPunctuation);
    // }
    // if (!otherSymbols.isEmpty()) {
    // addSet(labelToUnicodeSet, "Symbol-Other", otherSymbols);
    // }
    //
    // PrintWriter out = FileUtilities.openUTF8Writer(Emoji.TR51_INTERNAL_DIR,
    // "other-labels.html");
    // writeHeader(out, "Other Labels", null, "<p>Draft categories for other
    // Symbols and Punctuation.</p>\n", "border='1'", true);
    //
    // for (Entry<String, UnicodeSet> entry : labelToUnicodeSet.entrySet()) {
    // String label = entry.getKey();
    // UnicodeSet uset = entry.getValue();
    // if (label.equalsIgnoreCase("exclude")) {
    // continue;
    // }
    // displayUnicodesetTD(out, Collections.singleton(label), null,
    // Collections.singleton(""+uset.size()), uset, Style.plain, 16, "");
    // }
    //
    // writeFooter(out, "");
    // out.close();
    // }

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
                UnicodeSet set = new UnicodeSet("[" + line.replace("&", "\\&").replace("\\", "\\\\").replace("[", "\\[")
                        .replace("]", "\\]").replace("^", "\\^").replace("{", "\\{").replace("-", "\\-") + "]");
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

    /**
     * @deprecated Use
     *             {@link #displayUnicodesetTD(PrintWriter,Set<T>,String,Set<String>,UnicodeSet,Style,int,String,Visibility)}
     *             instead
     */
    public static <T extends Object> void displayUnicodesetTD(PrintWriter out, Set<T> labels, String sublabel,
            Set<String> otherCols, UnicodeSet uset, Style showEmoji, int maxPerLine, String link) {
        displayUnicodesetTD(out, labels, sublabel, otherCols, uset, showEmoji, maxPerLine, link, Visibility.external);
    }

    public static <T extends Object> void displayUnicodesetTD(PrintWriter out, Set<T> labels, String sublabel,
            Set<String> otherCols, UnicodeSet uset, Style showEmoji, int maxPerLine, String link,
            Visibility visibility) {
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
        displayUnicodeSet(out, uset, showEmoji, maxPerLine, 1, 1, link, EMOJI_COMPARATOR, visibility);
        out.println("</tr>");
    }

    public static void displayUnicodeSet(PrintWriter out, UnicodeSet uset, Style showEmoji, int maxPerLine, int colSpan,
            int rowSpan, String link, Comparator comparator, Visibility visibility) {
        Set<String> sorted = uset.addAllTo(new TreeSet<String>(comparator));
        displayUnicodeSet(out, sorted, showEmoji, maxPerLine, colSpan, rowSpan, link, "", visibility);
    }

    static final Pattern PREDICTABLE_TARGET = Pattern.compile("emoji-([a-z]+).html");

    public static String getTargetForFile(String file) {
        Matcher m = PREDICTABLE_TARGET.matcher(file);
        if (m.matches()) {
            return m.group(1);
        }
        switch (file) {
        case "full-emoji-list.html":
            return "full";
        default:
            throw new IllegalArgumentException("Unknown target for file: " + file);
        }
    }

    static final String IMAGE_MORE_INFO = "emoji-list.html";
    private static final String IMAGE_MORE_INFO_TITLE = "Emoji List";
    static final String IMAGE_MORE_INFO_LINK = "<a href='" + IMAGE_MORE_INFO + "' target='"
            + getTargetForFile(IMAGE_MORE_INFO) + "'>" + IMAGE_MORE_INFO_TITLE + "</a>";

    private static String getMoreInfoLink(String link, String emoji) {
        if (link == null) {
            link = IMAGE_MORE_INFO;
        }
        return "<a href='" + link + "#" + Emoji.buildFileName(emoji, "_") + "' target='" + getTargetForFile(link)
        + "'>"
        ;
    }

    //	private static final String HOVER_INSTRUCTIONS = "Emoji without available skintone show a small swatch afterwards;"
    //			+ " those without available images are shown as ✘. "
    //			+ "Hovering over an emoji shows the name; clicking goes to the " + IMAGE_MORE_INFO_LINK
    //			+ " entry for that emoji.";

    public static void displayUnicodeSet(PrintWriter out, Collection<String> sorted, Style showEmoji, int maxPerLine,
            int colSpan, int rowSpan, String link, String extraClasses, Visibility visibility) {
        String target = null;
        if (link == null) {
            link = IMAGE_MORE_INFO;
            target = getTargetForFile(link);
        } else if (link.isEmpty()) {
            link = null;
        }
        out.println("<td class='lchars'" + (rowSpan <= 1 ? "" : " rowSpan='" + rowSpan + "'")
                + (colSpan <= 1 ? "" : " colSpan='" + colSpan + "'") + ">");
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
                    cell = EmojiData.EMOJI_DATA.getVariant(s, Emoji.Qualified.all,
                            showEmoji == Style.emoji ? Emoji.EMOJI_VARIANT : Emoji.TEXT_VARIANT);
                    // Emoji.getEmojiVariant(s, showEmoji == Style.emoji ?
                    // Emoji.EMOJI_VARIANT_STRING : Emoji.TEXT_VARIANT_STRING);
                    break;
                case emojiFont:
                    classString = " class='charsSmall'";
                    // fall through
                case plain:
                    cell = s;
                    break;
                case bestImage:
                    cell = getBestImage(s, visibility == Visibility.external, extraClasses);
                    gotTitle = true;
                    break;
                case refImage:
                    cell = getImage(Emoji.Source.ref, s, true, extraClasses);
                    gotTitle = true;
                    break;
                }
            }
            if (link != null) {
                cell = "<a" + classString + " href='" + link + "#" + Emoji.buildFileName(s, "_") + "' target='" + target
                        + "'>" + cell + "</a>";
            }
            if (!gotTitle) {
                cell = addTitle(s, cell);
            }
            out.print(cell);
        }
        out.println("</td>");
    }

    private static String addTitle(String s, String cell) {
        String chars2WithVS = s; // EmojiData.EMOJI_DATA.addEmojiVariants(s);
        return "<span title='" + getCodeAndName2(chars2WithVS, true)
        // Emoji.toUHex(s) + " " + Emoji.getName(s, true,
        // GenerateEmoji.EXTRA_NAMES)
        + "'>" + cell + "</span>";
    }

    public static String getCodeCharsAndName(String chars1, String separator) {
        return Emoji.toUHex(chars1) + separator + chars1 + separator + EmojiData.EMOJI_DATA.getName(chars1);
    }

    static final UnicodeSet SPECIAL_INCLUSIONS = new UnicodeSet("[#*0-9 ⃣]").addAll(Emoji.REGIONAL_INDICATORS).freeze();

    private static final String CLDR_DATA_LINK = "<a target='cldr' href='http://cldr.unicode.org/#TOC-What-is-CLDR-'>Unicode CLDR data</a>";
    private static final String CLDR_ANNOTATIONS_LINK = "<a target='cldr_annotations' href='http://unicode.org/cldr/charts/"
            + (Emoji.IS_BETA ? "dev" : "latest") + "/annotations/index.html'>Unicode CLDR Annotations</a>";

    enum Form {
        noImages("This chart provides a list of the Unicode emoji characters and sequences, with single image and annotations."
                + " Clicking on a Sample goes to the emoji in the <a target='full' href='full-emoji-list.html'>full list</a>.) "
                + ("The ordering of the emoji and the annotations are based on " + GenerateEmoji.CLDR_DATA_LINK + ". "
                        + "Emoji sequences have more than one code point in the <b>Code</b> column.</p>")), 
        fullForm("This chart provides a list of the Unicode emoji characters and sequences, with images from different vendors, "
                + "CLDR name, date, source, and keywords. "
                + ("The ordering of the emoji and the annotations are based on " + GenerateEmoji.CLDR_DATA_LINK + ". "
                        + "Emoji sequences have more than one code point in the <b>Code</b> column. "
                        + "New characters show as a group with “…” before and after.</p>")), 
        onlyNew("This chart provides a list of the Unicode emoji characters and sequences that have been added to the most recent version of Unicode Emoji. "
                + "See also the <a target='candidates' href='emoji-candidates.html'>Emoji Candidates</a>.");

        final String description;

        Form(String description) {
            this.description = description;
        }
    }

    /**
     * Main charts
     * 
     * @param outputDir
     *            TODO
     * @param filter
     *            TODO
     * @param filteredName
     *            TODO
     * @param extraPlatforms
     *            TODO
     * @deprecated Use {@link #print(String,Form,String,String,UnicodeSet)}
     *             instead
     */
    //    public static <T> void print(String chartsDir, Form form, UnicodeSet filter, String title, String filename)
    //            throws IOException {
    //        print(chartsDir, form, title, filename, filter);
    //    }

    /**
     * Main charts
     * 
     * @param filter
     *            TODO
     * @param outputDir
     *            TODO
     * @param filteredName
     *            TODO
     * @param extraPlatforms
     *            TODO
     */
    public static <T> void print(String chartsDir, Form form, String title, String outFileName, UnicodeSet filter)
            throws IOException {
        try (PrintWriter out = FileUtilities.openUTF8Writer(chartsDir, outFileName);
                PrintWriter outPlain = FileUtilities.openUTF8Writer(Emoji.INTERNAL_OUTPUT_DIR,
                        outFileName.replace(".html", ".txt"));) {
            CountEmoji ce = new CountEmoji();
            int order = 0;
            UnicodeSet level1 = null;
            writeHeader(outFileName, out, title, null, "border='1'", false, false, "<p>" + form.description + "</p>\n");
            final String htmlHeaderString = GenerateEmoji.toHtmlHeaderString(form);
            int rows = count("<th", htmlHeaderString);
            int item = 0;
            String lastOrderingGroup = "";
            int headerGroupCount = 0;
            MajorGroup lastMajorGroup = null;
            String bigHead = "<tr><th colspan='" + rows + "' class='bighead'>";
            String smallHead = "<tr><th colspan='" + rows + "' class='mediumhead'>";
            for (String s : SORTED_ALL_EMOJI_CHARS_SET) {
                if (EmojiData.MODIFIERS.contains(s)) {
                    continue;
                }
                if (filter != null && !filter.contains(s)) {
                    continue;
                }
                String orderingGroup = EmojiOrder.STD_ORDER.charactersToOrdering.get(s);
                MajorGroup majorGroup = EmojiOrder.STD_ORDER.getMajorGroupFromCategory(orderingGroup);
                if (majorGroup != lastMajorGroup) {
                    out.println(bigHead + getDoubleLink(majorGroup.toHTMLString()) + "</th></tr>");
                    outPlain.println("@@" + majorGroup.toPlainString());
                    lastMajorGroup = majorGroup;
                }
                if (!orderingGroup.equals(lastOrderingGroup)) {
                    out.println(smallHead + getDoubleLink(orderingGroup) + "</th></tr>");
                    out.println(htmlHeaderString);
                    outPlain.println("@" + orderingGroup);
                    headerGroupCount = 0;
                    lastOrderingGroup = orderingGroup;
                } else if (headerGroupCount > 19) {
                    out.println(htmlHeaderString);
                    headerGroupCount = 0;
                }
                String toAdd = toHtmlString(s, form, ++item);
                out.println(toAdd);
                ce.add(s);
                outPlain.println(Utility.hex(s, 4, " ") + "\t" + EmojiData.ANNOTATION_SET.getShortName(s));
                ++headerGroupCount;
            }
            out.println(TABLE_TOTALS);
            ce.showCounts(out, false);

            writeFooter(out, "");
        }
    }

    private static int count(String string, String target) {
        int pos = 0;
        int count = 0;
        while (true) {
            pos = target.indexOf(string, pos) + 1;
            if (pos <= 0) {
                return count;
            }
            ++count;
        }
    }

    enum Properties {
        Emoji, Emoji_Presentation, Emoji_Modifier, Emoji_Modifier_Base
    }

    public static void writeFooter(PrintWriter out, String htmlAfterTable) {
        out.println("</table>");
        out.println(htmlAfterTable);
        out.println("</div>" + "<div class='copyright'>"
                // + "<hr width='50%'>"
                + "<br><a href='http://www.unicode.org/unicode/copyright.html'>"
                + "<img src='http://www.unicode.org/img/hb_notice.gif' style='border-style: none; width: 216px; height=50px;' alt='Access to Copyright and terms of use'>"
                + "</a><br><script language='Javascript' type='text/javascript' src='http://www.unicode.org/webscripts/lastModified.js'></script>"
                + "</div><script>\n" + "(function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){"
                + "(i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),"
                + "m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)"
                + "})(window,document,'script','//www.google-analytics.com/analytics.js','ga');"
                + "  ga('create', 'UA-19876713-1', 'auto');" + "  ga('send', 'pageview');" + "</script>"
                + "</body></html>");
    }

    public static void writeHeader(String outFileName, PrintWriter out, String title, String styles, String tableAttrs,
            boolean showGeneralComments, boolean skipVersion, String firstLine) {
        final String fullTitle = title + (skipVersion ? "" : ", v" + Emoji.VERSION_STRING);
        String headerLine = "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n"
                + "<html>\n" + "<head>\n"
                + "<meta http-equiv='Content-Type' content='text/html; charset=UTF-8'>\n"
                + "<link rel='stylesheet' type='text/css' href='emoji-list.css'>\n" 
                + "<title>" + fullTitle
                + (skipVersion ? "" : Emoji.BETA_TITLE_AFFIX) + "</title>\n"
                + (styles == null ? "" : "<style>\n" + styles + "\n</style>\n") + "</head>\n" + "<body>\n"
                + UNICODE_HEADER + getButton() + "\n"
                + "<h1>" + fullTitle
                + (skipVersion ? "" : Emoji.BETA_HEADER_AFFIX) + "</h1>\n"
                + (skipVersion ? "" : getPointToOther(outFileName, title))
                + "<p style='font-size:80%'>" 
                + "<a target='text' href='index.html'>Index &amp; Help</a>\n" 
                + " | <a target='rights' href='../images.html'>Images &amp; Rights</a>\n"
                + " | <a target='doc' href='" + TR51_HTML + "'>Spec</a>\n" 
                + " | <a target='submitting-emoji' href='../../emoji/selection.html'>Proposing Additions</a>"
                + "</p>\n"
                + firstLine
                + "<p>For more information, see <a target='text' href='index.html'>Index &amp; Help</a>.</p>\n"
                + "<table " + tableAttrs + ">";
        out.println(headerLine);
    }

    private static String getPointToOther(String outFileName, String title) {
        return !Emoji.BETA_IS_OPEN && !Emoji.IS_BETA ? ""
                : "<blockquote><i>For the " + (Emoji.IS_BETA
                        ? "current released version, see <b><a href='../charts/" + outFileName + "'>v"
                        + Emoji.VERSION_LAST_RELEASED_STRING
                        : "new beta version, see <b><a href='../charts-beta/" + outFileName + "'>v"
                        + Emoji.VERSION_BETA_STRING)
                + "</a></b>.</i></blockquote>";
    }

    static boolean CHECKFACE = false;

    // static void oldAnnotationDiff() throws IOException {
    // final String outFileName = "emoji-diff.html";
    // PrintWriter out = FileUtilities.openUTF8Writer(Emoji.CHARTS_DIR,
    // outFileName);
    // writeHeader(outFileName, out, "Diff List", null, "<p>Differences from
    // other categories.</p>\n", "border='1'", true, false);
    //
    // UnicodeSet AnimalPlantFood = new UnicodeSet("[☕ 🌰-🌵 🌷-🍼 🎂 🐀-🐾]");
    // testEquals(out, "AnimalPlantFood", AnimalPlantFood, Label.nature,
    // Label.food);
    //
    // UnicodeSet Object = new UnicodeSet("[⌚ ⌛ ⏰ ⏳ ☎ ⚓ ✂ ✉ ✏ 🎀 🎁 👑-👣 💄 💉
    // 💊 💌-💎 💐 💠 💡 💣 💮 💰-📷 📹-📼 🔋-🔗 🔦-🔮 🕐-🕧]");
    // testEquals(out, "Object", Object, Label.object, Label.office,
    // Label.clothing);
    //
    // CHECKFACE = true;
    // UnicodeSet PeopleEmotion = new UnicodeSet("[☝ ☺ ✊-✌ ❤ 👀 👂-👐 👤-💃
    // 💅-💇 💋 💏 💑 💓-💟 💢-💭 😀-🙀 🙅-🙏]");
    // testEquals(out, "PeopleEmotion", PeopleEmotion, Label.person, Label.body,
    // Label.emotion, Label.face);
    // CHECKFACE = false;
    //
    // UnicodeSet SportsCelebrationActivity = new UnicodeSet("[⛑ ⛷ ⛹ ♠-♧ ⚽ ⚾
    // 🀀-🀫 🂠-🂮 🂱-🂾 🃁-🃏 🃑-🃟 🎃-🎓 🎠-🏄 🏆-🏊 💒]");
    // testEquals(out, "SportsCelebrationActivity", SportsCelebrationActivity,
    // Label.game, Label.sport, Label.activity);
    //
    // UnicodeSet TransportMapSignage = new UnicodeSet("[♨ ♻ ♿ ⚠ ⚡ ⛏-⛡ ⛨-⛿ 🏠-🏰
    // 💈 🗻-🗿 🚀-🛅]");
    // testEquals(out, "TransportMapSignage", TransportMapSignage, Label.travel,
    // Label.place);
    //
    // UnicodeSet WeatherSceneZodiacal = new UnicodeSet("[☀-☍ ☔ ♈-♓ ⛄-⛈ ⛎ ✨
    // 🌀-🌠 🔥]");
    // testEquals(out, "WeatherSceneZodiacal", WeatherSceneZodiacal,
    // Label.weather, Label.time);
    //
    // UnicodeSet Enclosed = new UnicodeSet(
    // "[[\u24C2\u3297\u3299][\\U0001F150-\\U0001F19A][\\U0001F200-\\U0001F202][\\U0001F210-\\U0001F23A][\\U0001F240-\\U0001F248][\\U0001F250-\\U0001F251]]");
    // testEquals(out, "Enclosed", Enclosed, Label.word);
    //
    // UnicodeSet Symbols = new UnicodeSet(
    // "[[\\U0001F4AF][\\U0001F500-\\U0001F525][\\U0001F52F-\\U0001F53D][\\U0001F540-\\U0001F543[\u00A9\u00AE\u2002\u2003\u2005\u203C\u2049\u2122\u2139\u2194\u2195\u2196\u2197\u2198\u2199\u21A9\u21AA\u231B\u23E9\u23EA\u23EB\u23EC\u25AA\u25AB\u25B6\u25C0\u25FB\u25FC\u25FD\u25FE\u2611\u2660\u2663\u2665\u2666\u267B\u2693\u26AA\u26AB\u2705\u2708\u2712\u2714\u2716\u2728\u2733\u2734\u2744\u2747\u274C\u274E\u2753\u2754\u2755\u2757\u2764\u2795\u2796\u2797\u27A1\u27B0\u2934\u2935\u2B05\u2B06\u2B07\u2B1B\u2B1C\u2B50\u2B55\u3030\u303D]]]");
    // testEquals(out, "Symbols", Symbols, Label.sign);
    //
    // UnicodeSet other = new UnicodeSet(get70(Label.values()))
    // .removeAll(AnimalPlantFood)
    // .removeAll(Object)
    // .removeAll(PeopleEmotion)
    // .removeAll(SportsCelebrationActivity)
    // .removeAll(TransportMapSignage)
    // .removeAll(WeatherSceneZodiacal)
    // .removeAll(Enclosed)
    // .removeAll(Symbols);
    //
    // testEquals(out, "Other", other, Label.flag, Label.sign, Label.arrow);
    //
    // UnicodeSet ApplePeople = new UnicodeSet("[☝☺✊-✌✨❤🌂🌟🎀🎩🎽🏃👀👂-👺👼👽
    // 👿-💇💋-💏💑💓-💜💞💢💤-💭💼🔥😀-🙀🙅-🙏 🚶]");
    // testEquals(out, "ApplePeople", ApplePeople, Label.person, Label.emotion,
    // Label.face, Label.body, Label.clothing);
    //
    // UnicodeSet AppleNature = new UnicodeSet("[☀☁☔⚡⛄⛅❄⭐🌀🌁🌈🌊-🌕🌙-🌞🌠🌰-🌵
    // 🌷-🌼🌾-🍄🐀-🐾💐💩]");
    // testEquals(out, "AppleNature", AppleNature, Label.nature, Label.food,
    // Label.weather);
    //
    // UnicodeSet ApplePlaces = new UnicodeSet("[♨⚓⚠⛪⛲⛵⛺⛽✈🇧-🇬🇮-🇰🇳🇵🇷-🇺
    // 🌃-🌇🌉🎠-🎢🎪🎫🎭🎰🏠-🏦🏨-🏰💈💒💺📍 🔰🗻-🗿🚀-🚝🚟-🚩🚲]");
    // testEquals(out, "ApplePlaces", ApplePlaces, Label.place, Label.travel);
    //
    // UnicodeSet AppleSymbols = new UnicodeSet(
    // "[©®‼⁉⃣™ℹ↔-↙↩↪⏩-⏬ Ⓜ▪▫▶◀◻-◾☑♈-♓♠♣♥♦♻♿⚪⚫⛎ ⛔✅✔✖✳✴❇❌❎❓-❕❗➕-➗➡➰➿⤴⤵
    // ⬅-⬇⬛⬜⭕〰〽㊗㊙🅰🅱🅾🅿🆎🆑-🆚🈁🈂🈚 🈯🈲-🈺🉐🉑🌟🎦🏧👊👌👎💙💛💟💠💢💮💯💱💲
    // 💹📳-📶🔀-🔄🔗-🔤🔯🔱-🔽🕐-🕧🚫🚭-🚱 🚳🚷-🚼🚾🛂-🛅]");
    // testEquals(out, "AppleSymbols", AppleSymbols, Label.sign, Label.game);
    //
    // UnicodeSet AppleTextOrEmoji = new UnicodeSet(
    // "[‼⁉ℹ↔-↙↩↪Ⓜ▪▫▶◀◻-◾☀☁☎ ☑☔☕☝☺♈-♓♠♣♥♦♨♻♿⚓⚠⚡⚪⚫⚰ ⚾✂✈✉✌✏✒✳✴❄❇❤➡⤴⤵⬅-⬇〽㊗㊙
    // 🅰🅱🅾🅿🈂🈷🔝{#⃣}{0⃣}{1⃣}{2 ⃣}{3⃣}{4⃣}{5⃣}{6⃣}{7⃣}{8
    // ⃣}{9⃣}{🇨🇳}{🇩🇪}{🇪🇸}{🇫🇷}{🇬🇧}{ 🇮🇹}{🇯🇵}{🇰🇷}{🇷🇺}{🇺🇸}]");
    // UnicodeSet AppleOnlyEmoji = new UnicodeSet(
    // "[⌚⌛⏩-⏬⏰⏳⚽⛄⛅⛎⛔⛪⛲⛳⛵⛺⛽✅ ✊✋✨❌❎❓-❕❗➿⬛⬜⭐⭕🀄🃏🆎🆑-🆚🈁
    // 🈚🈯🈲-🈶🈸-🈺🉐🉑🌀-🌠🌰-🌵🌷-🍼🎀-🎓
    // 🎠-🏊🏠-🏰🐀-🐾👀👂-📷📹-📼🔀-🔘🔞-🔽 🕐-🕧🗻-🙀🙅-🙏🚀-🛅]");
    //
    // UnicodeSet AppleAll = new
    // UnicodeSet(AppleTextOrEmoji).addAll(AppleOnlyEmoji);
    // UnicodeSet AppleObjects = new UnicodeSet(AppleAll)
    // .removeAll(ApplePeople)
    // .removeAll(AppleNature)
    // .removeAll(ApplePlaces)
    // .removeAll(AppleSymbols);
    //
    // testEquals(out, "AppleObjects", AppleObjects, Label.flag, Label.sign,
    // Label.arrow);
    //
    // writeFooter(out, "");
    // out.close();
    // }

    // private static void testEquals(PrintWriter out, String title1, UnicodeSet
    // AnimalPlantFood,
    // String title2, UnicodeSet labelNatureFood) {
    // testContains(out, title1, AnimalPlantFood, title2, labelNatureFood);
    // testContains(out, title2, labelNatureFood, title1, AnimalPlantFood);
    // }

    // public static void testEquals(PrintWriter out, String title1, UnicodeSet
    // AnimalPlantFood,
    // Label... labels) {
    // title1 = "<b>" + title1 + "</b>";
    // for (Label label : labels) {
    // testContains(out, title1, AnimalPlantFood, label.toString(),
    // get70(label));
    // }
    // String title2 = CollectionUtilities.join(labels, "+");
    // UnicodeSet labelNatureFood = get70(labels);
    // testContains(out, title2, labelNatureFood, title1, AnimalPlantFood);
    // }

    private static void testContains(PrintWriter out, String title, UnicodeSet container, String title2,
            UnicodeSet containee) {
        if (!container.containsAll(containee)) {
            UnicodeSet missing = new UnicodeSet(containee).removeAll(container);
            out.println("<tr><td>" + title + "</td>\n" + "<td>⊉</td>\n" + "<td>" + title2 + "</td>\n" + "<td>"
                    + missing.size() + "/" + containee.size() + "</td>\n" + "<td class='lchars'>");
            boolean first = true;
            Set<String> sorted = new TreeSet<String>(EMOJI_COMPARATOR);
            missing.addAllTo(sorted);
            for (String s : sorted) {
                if (first) {
                    first = false;
                } else {
                    out.print("\n");
                }
                out.print("<span title='" + EmojiData.EMOJI_DATA.getName(s) + "'>"
                        + EmojiData.EMOJI_DATA.addEmojiVariants(s)
                        // + Emoji.getEmojiVariant(s,
                        // Emoji.EMOJI_VARIANT_STRING)
                        + "</span>");
            }
            out.println("</td></tr>");
        }
    }

    // public static UnicodeSet get70(Label... labels) {
    // UnicodeSet containee = new UnicodeSet();
    // for (Label label : labels) {
    // containee.addAll(Label.CHARS_TO_LABELS.getKeys(label));
    // }
    // containee.removeAll(VERSION70);
    // // containee.retainAll(JSOURCES);
    // return containee;
    // }

    public static String getDoubleLink(String href, String anchorText) {
        href = href.replace(' ', '_').toLowerCase(Locale.ENGLISH);
        return "<a href='#" + href + "' name='" + href + "'>" + anchorText + "</a>";
    }

    public static String getLink(String href, String anchorText, String target) {
        href = href.replace(' ', '_').toLowerCase(Locale.ENGLISH);
        return "<a" + " href='" + href + "'" + (target == null ? "" : " target='" + target + "'") + ">" + anchorText
                + "</a>";
    }

    public static String getExtraLink(String href) {
        return "<a" + " href='" + href + "'" + "></a>";
    }


    public static String getDoubleLink(String anchor) {
        return getDoubleLink(anchor, anchor);
    }

    /**
     * @deprecated Use {@link EmojiImageData#getDataUrl(String)} instead
     */
    static String getDataUrl(String filename) {
        return EmojiImageData.getDataUrl(filename);
    }

    static void printCollationOrder() throws IOException {
        try (PrintWriter outText = FileUtilities.openUTF8Writer(Emoji.CHARTS_DIR, "emoji-ordering.txt")) {
            outText.append("# Machine-readable version of the emoji ordering"
                    + " for v"+ Emoji.VERSION_STRING + Emoji.BETA_TITLE_AFFIX_SHORT + " (corresponding to CLDR).\n"
                    + "# Note that the skin-tone modifiers are primary-ignorable in the CLDR collation rules.\n"
                    + "# For a listing with the groups and subgroups, and the non-qualified sequences,\n"
                    + "# see http://unicode.org/Public/emoji/" + Emoji.VERSION_STRING + "/emoji-test.txt"
                    + "\n");
            for (String s : SORTED_ALL_EMOJI_CHARS_SET) {
                outText.println(Emoji.toUHex(s) + " ; " + Emoji.getNewest(s).getShortName() + " # " + s + " "
                        + EmojiData.EMOJI_DATA.getName(s));
            }
        }
        try (PrintWriter outText = FileUtilities.openUTF8Writer(Emoji.CHARTS_DIR, "emoji-ordering-rules.txt")) {
            outText.append("<!-- Machine-readable version of the emoji ordering rules for v"
                    + Emoji.VERSION_STRING + Emoji.BETA_TITLE_AFFIX_SHORT
                    + " (corresponding to CLDR). -->\n"
                    + "<collation type='emoji'>\n" + "<cr><![CDATA[\n" + "# START AUTOGENERATED EMOJI ORDER\n");
            EmojiOrder.STD_ORDER.appendCollationRules(outText, EmojiData.EMOJI_DATA.getAllEmojiWithDefectives(),
                    EmojiOrder.GENDER_NEUTRALS); // those without VS are added
            outText.write("\n]]></cr>\n</collation>");
        }
        /**
         *            final String fullTitle = title + (skipVersion ? "" : ", v" + Emoji.VERSION_STRING);
                String headerLine = "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n"
                                + "<html>\n" + "<head>\n"
                                + "<meta http-equiv='Content-Type' content='text/html; charset=UTF-8'>\n"
                                + "<link rel='stylesheet' type='text/css' href='emoji-list.css'>\n" + "<title>" + fullTitle
                                + (skipVersion ? "" : Emoji.BETA_TITLE_AFFIX) + "</title>\n"

         */
    }

    // private static void printAnnotations() throws IOException {
    // try (
    // PrintWriter outText =
    // FileUtilities.openUTF8Writer(Emoji.TR51_INTERNAL_DIR
    // , "emoji-annotations.xml")) {
    // outText.append(ANNOTATION_HEADER
    // + "\t\t<language type='en'/>\n"
    // + "\t</identity>\n"
    // + "\t<annotations>\n");
    // Set<Row.R2<Set<String>, UnicodeSet>> sorted = new TreeSet<>(PAIR_SORT);
    // for (String s : emojiData.getChars()) {
    // Set<String> annotations = getAnnotations(s);
    // annotations.removeAll(EmojiAnnotations.GROUP_ANNOTATIONS);
    // if (annotations.isEmpty()) {
    // throw new IllegalArgumentException("Missing annotation: " + s
    // + "\t" + EmojiAnnotations.ANNOTATIONS_TO_CHARS.getKeys(s));
    // }
    // }
    // // for (Entry<Set<String>, Set<String>> s :
    // EmojiAnnotations.ANNOTATIONS_TO_CHARS.getValuesToKeys().keyValuesSet()) {
    // // UnicodeSet chars = new UnicodeSet().addAll(s.getKey());
    // // Set<String> annotations = new LinkedHashSet<>(s.getValue());
    // // annotations.removeAll(GROUP_ANNOTATIONS);
    // // if (annotations.isEmpty()) {
    // // continue;
    // // }
    // // sorted.add(Row.of(annotations, chars));
    // // }
    // //for (R2<Set<String>, UnicodeSet> s : sorted) {
    // UnicodeSet chars = new UnicodeSet();
    // for (String cp : EmojiOrder.STD_ORDER.orderingToCharacters.values()) {
    // Set<String> annotationSet = getAnnotations(cp);
    // annotationSet.removeAll(EmojiAnnotations.GROUP_ANNOTATIONS);
    // String annotations = CollectionUtilities.join(annotationSet, "; ");
    // chars.clear().add(cp);
    // outText.append("\t\t<annotation cp='")
    // .append(chars.toPattern(false))
    // .append("'")
    // //.append(" draft='provisional'")
    // .append(">")
    // .append(annotations)
    // .append("</annotation>\n");
    // }
    // outText.write("\t</annotations>\n"
    // + "</ldml>");
    // }
    // }

    // private static Set<String> getAnnotations(String string) {
    // if (!string.contains("\u200D")) {
    // return new
    // LinkedHashSet<>(EmojiAnnotations.ANNOTATIONS_TO_CHARS.getKeys(string));
    // }
    // Set<String> result = new LinkedHashSet<>();
    // for (int cp : CharSequences.codePoints(string)) {
    // if (cp != 0x200D && cp != 0xFE0F) {
    // result.addAll(EmojiAnnotations.ANNOTATIONS_TO_CHARS.getKeys(UTF16.valueOf(cp)));
    // }
    // }
    // return result;
    // }

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
            int diff = compareX(o1.get0().iterator(), o2.get0().iterator(), EmojiOrder.UCA_COLLATOR);
            if (diff != 0) {
                return diff;
            }
            return o1.get1().compareTo(o2.get1());
        }
    };

    public static String showVersion(Age_Values age_Values) {
        return showVersionOnly(age_Values) + " " + VersionToAge.ucd.getYear(age_Values);
    }

    private static String showVersionOnly(Age_Values age_Values) {
        return age_Values.toString().replace('_', '.');
    }

    public static String getSources(String chars2, StringBuilder suffix, boolean superscript) {
        boolean first = true;
        for (Emoji.CharSource source : GenerateEmoji.getCharSources(chars2)) {
            suffix.append(superscript ? source.superscript : first ? source.letter : " " + source.letter);
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
        // "# Code ;\tDefault Style ;\tLevel ;\tModifier ;\tSources
        // ;\tVersion\t# (Character) Name\n"

        String extraString = "";
        extraString = " ;\t" + (EmojiData.MODIFIERS.contains(chars2) ? "modifier"
                : EmojiData.EMOJI_DATA.getModifierBases().contains(chars2) ? "modifierBase" : "non-modifier");

        return Utility.hex(chars2, " ") + " ;\t" + Style.fromString(chars2) // (EmojiData.EMOJI_DATA.getEmojiPresentationSet(chars2)
        + extraString + " ;\t" + getSources(chars2, new StringBuilder(), false) + "\t# "
        + showVersion(Emoji.getNewest(chars2)) + " (" + chars2 + ") "
        + EmojiData.EMOJI_DATA.getName(chars2);
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

    public static String getCell(Emoji.Source type, String core, String cellClass, boolean addLink, Output<Boolean> found) {
        String linkPre = addLink ? getMoreInfoLink("full-emoji-list.html", core) : "";
        String linkPost = addLink ? "</a>" : "";
        if (type == null) {
            return "<td class='andr'>" + linkPre + getBestImage(core, true, "", Emoji.Source.color) + linkPost
                    + "</td>\n";
        }

        String filename = Emoji.getImageFilenameFromChars(type, core);
        String androidCell = "<td class='" + cellClass + " miss'>—</td>\n";
        boolean isFound = false;
        if (filename != null) {
            String fullName = EmojiImageData.getDataUrl(filename);
            if (fullName == null && Emoji.IS_BETA && type == Source.ref) {
                filename = Emoji.getImageFilenameFromChars(Source.proposed, core);
                fullName = EmojiImageData.getDataUrl(filename);
            }
            if (fullName != null) {
                String className = type.getClassAttribute(core);
                androidCell = "<td class='" + cellClass + "'>" + linkPre + "<img alt='" + core + "' class='" + className
                        + "' src='" + fullName + "'>" + linkPost + "</td>\n";
                isFound = true;
            }
        }
        if (found != null) {
            found.value = isFound;
        }
        return androidCell;
    }

    static NamesList NAMESLIST = new NamesList("NamesList", Emoji.VERSION_TO_GENERATE_UNICODE.getVersionString(3, 3));
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
                Source[] doFirst = {};
                String image = EmojiData.EMOJI_DATA.getAllEmojiWithDefectives().contains(x)
                        ? getBestImageNothrow(x, x, true, " imgb", doFirst) : null;
                        if (image == null) {
                            String fixed = TransliteratorUtilities.toHTML.transform(x);
                            image = UTF16.hasMoreCodePointsThan(x, 1) ? fixed
                                    : Emoji.toUHex(x) + " " + fixed + " " + EmojiData.EMOJI_DATA.getName(x);
                        } else {
                            image = Emoji.toUHex(x) + " " + image + " " + EmojiData.EMOJI_DATA.getName(x);
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

    public static String toHtmlHeaderString(Form form) {
        StringBuilder otherCells = new StringBuilder();
        for (Source s : Emoji.Source.platformsToIncludeNormal) {
            otherCells.append("<th class='cchars'><a target='text' href='index.html#col-vendor'>" + s.shortName()
            + "</a></th>\n");
        }

        return "<tr>" + HEADER_NUM
                + HEADER_CODE
                + (form != Form.fullForm
                ? HEADER_SAMPLE_IMAGE
                        : HEADER_BROWSER +
                        //                                  "<th class='cchars'><a target='text' href='index.html#col-browser'>Brow.</a></th>\n"
                        //                          + "<th class='cchars'><a target='text' href='index.html#col-chart'>Chart</a></th>\n"
                        otherCells)
                // + "<th class='cchars'>Browser</th>\n"
                + HEADER_NAME
                + (form == Form.fullForm ? ""
                        : //HEADER_DATE + 
                            HEADER_KEYWORDS
                        )
                // + "<th>Block: <i>Subhead</i></th>\n"
                + "</tr>";
    }

    public static String toHtmlString(String chars2, Form form, int item) {
        String bestCell = getCell(null, chars2, ALT_COLUMN, form == Form.noImages, null);
        //String symbolaCell = getCell(Emoji.Source.ref, chars2, ALT_COLUMN, false);

        StringBuilder otherCells = new StringBuilder();
        Output<Boolean> isFound = new Output<>();
        int countFound = 0;
        for (Source s : Emoji.Source.platformsToIncludeNormal) {
            String cell = getCell(s, chars2, ALT_COLUMN, false, isFound);
            otherCells.append(altClass(cell));
            if (isFound.value) {
                countFound++;
            }
        }
        if (countFound < 3 || Emoji.IS_BETA && ARE_NEW.contains(chars2)) {
            otherCells.setLength(0);
            otherCells.append("<td class='andr' colSpan='" + Emoji.Source.platformsToIncludeNormal.size() + "'>… "
                    + getSamples(chars2) + " …</td>");
        }

        if (chars2.contains("\uD83D\uDC68\u200D\u2695") && form != Form.noImages) {
            int debug = 0;
        }
        String browserCell = "<td class='chars'>" + chars2
                // EmojiData.EMOJI_DATA.addEmojiVariants(chars2, Emoji.EMOJI_VARIANT,
                // null)
                // Emoji.getEmojiVariant(chars2, Emoji.EMOJI_VARIANT_STRING)
                + "</td>\n";
        if (chars2.equals("👩🏼‍⚖")) {
            int debug = 0;
        }
        String name2 = EmojiData.ANNOTATION_SET.getShortName(chars2);
        String vanilla = EmojiData.EMOJI_DATA.getName(chars2);
        if (name2 == null) {
            name2 = vanilla.toLowerCase(Locale.ENGLISH);
        }
        if (vanilla != null) {
            String noVs = chars2.replace(Emoji.EMOJI_VARIANT_STRING, "");
            if (noVs.codePointCount(0, noVs.length()) == 1 && !vanilla.equalsIgnoreCase(name2)) {
                name2 += "<br>≊ " + TransliteratorUtilities.toHTML.transform(vanilla);
            }
        }
        if (SHOW_NAMES_LIST) {
            name2 += getNamesListInfo(chars2);
        }

        String textChars = EmojiData.EMOJI_DATA.addEmojiVariants(chars2);
        // Emoji.getEmojiVariant(chars2, Emoji.TEXT_VARIANT_STRING);

        String chars2WithVS = chars2; // EmojiData.EMOJI_DATA.addEmojiVariants(chars2,
        // Emoji.EMOJI_VARIANT,
        // VariantHandling.sequencesOnly);
        DefaultPresentation style = EmojiData.EMOJI_DATA.getStyle(chars2);
        return "<tr>" 
        + getCellNum(item)
        + getCellCode(chars2WithVS)
        + (form == Form.noImages ? altClass(bestCell)
                : altClass(browserCell) + 
                //altClass(browserCell) + altClass(symbolaCell) +
                otherCells)
        + "<td class='name'>" + name2 + "</td>\n"
        + (form == Form.fullForm ? ""
                : //"<td class='age'>" + VersionToAge.ucd.getYear(Emoji.getNewest(chars2)) + getSources(chars2, new StringBuilder(), true) + "</td>\n" + 
                    "<td class='name'>" + getAnnotationsString(chars2) + "</td>\n"
                    // + "<td class='default'>" + (style == null ? "n/a" : style) +
                    // (!textChars.equals(chars2) ? "*" : "") + "</td>\n"
                ) + "</tr>";
    }

    private static String getCellCode(String chars2WithVS) {
        String uHex = Emoji.toUHex(chars2WithVS);
        String chars2WOVS = EmojiData.removeEmojiVariants(chars2WithVS);
        return "<td class='code'>" 
        + getDoubleLink(getAnchor(uHex), uHex)
//        + getExtraLink(chars2WithVS)
//        + (chars2WOVS.equals(chars2WithVS) ? "" : getExtraLink(chars2WOVS))
        + "</td>\n";
    }

    private static String getCellNum(int item) {
        return "<td class='rchars'>" + item + "</td>\n";
    }

    private static String getAnnotationsString(String chars2) {
        if (chars2.equals("👹")) {
            int debug = 0;
        }
        Set<String> annotationsPlain = new TreeSet<>();
        try { // HACK
            Set<String> plainAnnotations2 = EmojiAnnotations.ANNOTATIONS_TO_CHARS.getKeys(chars2);
            if (plainAnnotations2 != null) {
                annotationsPlain.addAll(plainAnnotations2);
            }
        } catch (Exception e) {
        }

        Set<String> annotationsExtra = new TreeSet<>();

        Set<String> plainAnnotations2 = CandidateData.getInstance().getAnnotations(chars2);
        if (plainAnnotations2 != null) {
            annotationsExtra.addAll(plainAnnotations2);
        }

        Collection<String> plainAnnotations3 = Keywords.get(chars2);
        if (plainAnnotations3 != null) {
            annotationsExtra.addAll(plainAnnotations3);
        }
        annotationsExtra.removeAll(annotationsPlain);

        Set<String> annotations = new LinkedHashSet<>();

        for (String a : annotationsPlain) {
            annotations.add(TransliteratorUtilities.toHTML.transform(a));
        }
        for (String a : annotationsExtra) {
            annotations.add("<span class='keye'>"
                    + TransliteratorUtilities.toHTML.transform(a)
                    + "</span>");
        }

        return BAR_JOIN.join(annotations);
    }

    private static final Set<String> SUPPRESS_ANNOTATIONS = ImmutableSet.of("default-text-style", "other", "nature",
            "object", "person", "place", "symbol", "travel", "office", "animal", "sign", "word", "time", "food",
            "restaurant", "sound", "sport", "emotion", "education");

    static final Splitter TAB = Splitter.on('\t');
    public static final UnicodeMap<String> EXTRA_NAMES = new UnicodeMap<>();

    static final Joiner SPACE_JOINER = Joiner.on(' ').skipNulls();

    enum CandidateStyle {
        candidate, released
    }

    static final Joiner BAR_JOIN = Joiner.on(" | ");
    static final Joiner BAR_I_JOIN = Joiner.on("</i> | <i>");

    static void showCandidateStyle(CandidateStyle candidateStyle, String outFileName, UnicodeSet emoji)
            throws IOException {
        if (candidateStyle == CandidateStyle.candidate && Emoji.IS_BETA) {
            FileUtilities.copyFile(Emoji.class, "redirect-candidates.html", Emoji.CHARTS_DIR, outFileName);
            return;
        }
        // gather data
        Comparator<String> comparator = EMOJI_COMPARATOR;
        boolean future = false;
        CandidateData cd = null;

        if (candidateStyle == CandidateStyle.candidate) {
            UnicodeMap<CandidateData.Quarter> quartersForChars = new UnicodeMap<>();
            // The data file is designed to take the contents of the table, when
            // pasted as plain text, and format it.
            cd = CandidateData.getInstance();
            comparator = cd.comparator;
            emoji = cd.getCharacters();
            future = true;
        }
        Set<String> sorted = emoji.addAllTo(new TreeSet<String>(comparator));
        String lastCategory = null;
        MajorGroup lastMajorGroup = null;
        String header = "<tr>"
                + HEADER_NUM // "<th><a target='text' href='index.html#col-num'>№</a></th>"
                + HEADER_CODE // + "<th width='7em'><a target='text' href='index.html#col-code'>Code</a></th>"
                //+ "<th width='5em'><a target='text' href='index.html#col-chart'>Chart Glyph</a></th>"
                + HEADER_SAMPLE_IMAGE // + "<th><a target='text' href='index.html#col-vendor'>Sample Colored Glyphs</a></th>"
                + HEADER_NAME
                + HEADER_KEYWORDS
                + "</tr>";

        List<String> output = new ArrayList<>();
        List<String> outputPlain = new ArrayList<>();
        outputPlain.add("Sort Order\tCode(s)\tChar(s)\tCLDR Name\tUnicode Name(s)\tAuthor(s)\tLink(s) to proposal(s) in doc registry");
        int count = 0;
        int countPlain = 0;
        boolean noHeader = true;
        CountEmoji ce = new CountEmoji();
        for (String source : sorted) {
            ce.add(source);
            // if (future != cd.getQuarter(source).isFuture()) {
            // continue;
            // }
            String category;
            MajorGroup majorGroup;
            Quarter quarter = null;
            if (candidateStyle == CandidateStyle.candidate) {
                category = cd.getCategory(source);
                majorGroup = cd.getMajorGroup(source);
                quarter = cd.getQuarter(source);
            } else {
                category = EmojiOrder.STD_ORDER.getCategory(source);
                majorGroup = EmojiOrder.STD_ORDER.getMajorGroupFromCategory(category);
            }

            if (majorGroup != lastMajorGroup) {
                output.add("<tr><th colspan='8' class='bighead'>" + getDoubleLink(majorGroup.toHTMLString() + "")
                + "</th></tr>");
                //outputPlain.add("@@" + majorGroup.toPlainString());
                noHeader = true;
                lastMajorGroup = majorGroup;
            }
            if (!Objects.equals(category, lastCategory)) {
                output.add("<tr><th colspan='8' class='mediumhead'>" + getDoubleLink(category + "") + "</th></tr>");
                //outputPlain.add("@@" + category);
                lastCategory = category;
            }
            if (noHeader) {
                output.add(header);
                noHeader = false;
            }
            //			String blackAndWhite = getImage(Source.proposed, source, true, "");
            //			if (blackAndWhite == null) {
            //				blackAndWhite = "<i>n/a</i>";
            //			}
            String color = getSamples(source);
            String href = Utility.hex(source).replace(" ", "_");
            String anchor = Emoji.toUHex(source);
            int special = source.codePointAt(0) - 0x100000;
            if (special >= 0) {
                href = anchor = "X" + Utility.hex(special, 5);
            }
            ++count;
            String currentRow = "<tr>\n" + " <td class='rchars'>" + count 
                    + "</td>\n" + " <td class='code'>"
                    + getDoubleLink(href, anchor) + "</td>\n" 
                    // + " <td class='andr'>" + blackAndWhite + "</td>\n"
                    + " <td class='default'>" + color + "</td>\n" + " <td class='name'>"
                    + EmojiData.EMOJI_DATA.getName(source);
            currentRow += "</td>\n";
            currentRow += " <td class='name'>" + getAnnotationsString(source) + "</td>\n";
            currentRow += "</tr>\n";
            //(future ? " <td class='default'>" + quarter + "</td>\n"
            // + "<td class='default'>" + (modBase.contains(source) ?
            // "Yes" : "")
            //					: "") + "</tr>\n";
            output.add(currentRow);
            if (EmojiData.EMOJI_DATA.MODIFIERS.containsSome(source)
                    || source.contains("\u200D\u2640")
                    || source.contains("\u200D\u2642")
                    ) {
                continue;
            }
            outputPlain.add(++countPlain
                    + "\t" + anchor 
                    + "\t" + source 
                    + "\t" + EmojiData.EMOJI_DATA.getName(source)
                    + "\t" + getShortUnicodeName(source, ", "));
        }
        UnicodeSet items = emoji;

        if (SHOW)
            System.out.println(items.toString().replace("\\", "\\\\"));
        // now print
        //		String topHeader = "<p>" + "At the 2016Q4 Unicode Technical Committee meeting (UTC #149), " + count
        //				+ " new emoji characters were provisionally approved for Unicode 10.0, "
        //				+ "for release in June, 2017. These are listed in the table below."
        //				+ "<blockquote style='color:#CC0000'>"
        //				+ "NOTE: While the 56 emoji will be in the Unicode 10.0 release, "
        //				+ "their code points, character names, representative chart glyphs, "
        //				+ "emoji properties, and annotations are <i>not</i> yet final. "
        //				+ "The colored glyphs are merely illustrative samples, and vary considerably from what vendors actually deploy. "
        //				+ "These sample glyphs may change at any time, and others may be added.</blockquote>\n"
        //				+ "<blockquote style='color:#CC0000'><i>Do not deploy any of these characters until the Unicode 10.0 release is available.</i>\n"
        //				+ "</blockquote>\n"
        //				+ "<p style='top-margin:3em'>These characters were based on proposals received by the Unicode "
        //				+ "Consortium, reviewed by the Unicode Emoji Subcommittee, and selected on the basis of the "
        //				+ "<i>Emoji Selection Factors</i> in "
        //				+ "<a target='_blank' href='../../emoji/selection.html'>Submitting Emoji Character Proposals</a>. "
        //				+ "That page also describes the <a href='http://unicode.org/emoji/selection.html#timeline'>Process and Timeline</a> for proposals.</p>\n"
        //				+ "<p>Thanks to EmojiXpress, Emojipedia, Emojination, and Adobe for the color sample glyphs. "
        //				+ "The representative chart glyphs (black and white) are not yet available for most of these emoji.</p>";

        String topHeader = "<p>The following emoji characters are candidates for inclusion in a future version of Unicode."
                + " These characters were based on proposals received by the Unicode "
                + "Consortium, reviewed by the Unicode Emoji Subcommittee, and selected on the basis of the "
                + "<i>Emoji Selection Factors</i> in "
                + "<a target='_blank' href='../../emoji/selection.html'>Submitting Emoji Character Proposals</a>. "
                + "That page also describes the <a href='http://unicode.org/emoji/selection.html#timeline'>Process and Timeline</a> for proposals.</p>";
        String footer = "<p><i>Thanks to submitters for the color sample glyphs.</i></p>";
        String showCandidatesTitle = "Emoji Candidates";

        if (!future) {
            topHeader = "<p>The following emoji characters and sequences have been added to this version of Unicode Emoji. "
                    // Comment out this text once Unicode 10. is released.
                    + " The list includes characters that will be in Unicode v10.0, scheduled for June 2017, "
                    + "and are being made available ahead of time so that vendors can begin working on their emoji fonts and code. "

//					+ "See also the <a target='candidates' href='emoji-candidates.html'>Emoji Candidates</a>."
+ (Emoji.IS_BETA ? footer : 
        " Thanks to EmojiXpress, Emojipedia, Emojination, Adobe, and submitters for the color sample glyphs.")
+ "</p>";
            footer = "";
            showCandidatesTitle = "Emoji Recently Added";
        }

        try (PrintWriter out = FileUtilities.openUTF8Writer(Emoji.CHARTS_DIR, outFileName);) {
            writeHeader(outFileName, out, showCandidatesTitle, null, "border='1'", true, future, topHeader);
            // out.println("<h2>" + getDoubleLink("U9.0 Candidates") + "</h2>");
            for (String outputLine : output) {
                out.println(outputLine);
            }
            out.println(TABLE_TOTALS);
            ce.showCounts(out, false);
            writeFooter(out, footer);
        }
        try (PrintWriter out = FileUtilities.openUTF8Writer(Emoji.INTERNAL_OUTPUT_DIR, outFileName.replace(".html", ".txt"))) {
            for (String outputLine : outputPlain) {
                out.println(outputLine);
            }
        }
    }

    private static String showNoModCount(int countNoMod) {
        return "<br><span class='nomod'>" + countNoMod + "</span>";
    }

    private static String getShortUnicodeName(String cps, String separator) {
        StringBuffer result = new StringBuffer();
        for (int cp : CharSequences.codePoints(cps)) {
            if (result.length() != 0) {
                result.append(separator);
            }
            if (Emoji.EMOJI_VARIANTS.contains(cp)) {
                continue;
            }
            String name;
            if (Emoji.TAG_BASE + ' ' <= cp && cp < Emoji.TAG_TERM_CHAR) {
                name = "tag_" + UTF16.valueOf(cp - Emoji.TAG_BASE);
            } else if (cp == Emoji.TAG_TERM_CHAR) {
                name = "tag_term";
            } else {
                name = cp == Emoji.JOINER ? "ZWJ" : IUP_LATEST.getName(cp);
            }
            result.append(name);
        }
        return result.toString();
    }

    private static String getSamples(String source) {
        // apple, google, twitter, emojione, samsung, fb, windows
        String color = SPACE_JOINER.join(
                getImage(Source.emojixpress, source, true, ""),
                getImage(Source.emojipedia, source, true, ""), 
                getImage(Source.apple, source, true, ""),
                getImage(Source.google, source, true, ""),
                getImage(Source.twitter, source, true, ""),
                getImage(Source.emojione, source, true, ""), 
                getImage(Source.samsung, source, true, ""),
                getImage(Source.fb, source, true, ""),
                getImage(Source.windows, source, true, ""),
                getImage(Source.emojination, source, true, ""),
                getImage(Source.adobe, source, true, ""),
                getImage(Source.sample, source, true, ""),
                getImage(Source.proposed, source, true, "")
                );
        if (color.isEmpty()) {
            color = getBestImage(source, true, "");
        }
        return color;
    }

    public static String getButton() {
        return "\n<div class='aacButton' title='Show your support of Unicode'>"
                + "<a target='sponsors' href='../../consortium/adopt-a-character.html'><b>Adopt a Character</b><br>"
                + "<img class='aacImage' alt='AAC Animation' src='../../consortium/images/aac-some-sponsors.gif'></a>"
                + "</div>";
    }
    // <a
    // href='http://www.unicode.org/cldr/trac/newticket?component=survey&amp;summary=Feedback+on+BOOTING+%3F'>Report
    // Problem in Tool</a>

    private static String getCldrTicket(String component, String summary) {
        return "<a target='cldr-ticket' href='http://unicode.org/cldr/trac/newticket" + "?component="
                + fixUrl(component) + "&amp;summary=" + fixUrl(summary) + "'>CLDR ticket</a>";
    }

    private static String fixUrl(String summary) {
        return summary.replace(' ', '+'); // TODO make more robust with %
        // encoding
    }

    static final String UNICODE_HEADER = "" + "<div class='icon'>"
            + "<a href='http://www.unicode.org/'><img class='logo' alt='[Unicode]' src='http://www.unicode.org/webscripts/logo60s2.gif'></a>"
            + "<a class='bar' target='text' href='index.html'>Emoji Charts</a>" + "</div>"
            + "<div class='gray'>&nbsp;</div>" + "<div class='main'>";
}
