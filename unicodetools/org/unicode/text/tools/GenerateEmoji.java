package org.unicode.text.tools;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
import org.unicode.cldr.util.MapComparator;
import org.unicode.cldr.util.With;
import org.unicode.jsp.Subheader;
import org.unicode.props.GenerateEnums;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.props.UcdPropertyValues.Binary;
import org.unicode.props.UcdPropertyValues.Block_Values;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.text.UCA.UCA;
import org.unicode.text.UCD.Default;
import org.unicode.text.utility.Utility;

import com.google.common.collect.ComparisonChain;
import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.CollectionUtilities.SetComparator;
import com.ibm.icu.dev.util.TransliteratorUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.MultiComparator;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.LocaleDisplayNames;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.EntryRange;
import com.ibm.icu.util.ICUUncheckedIOException;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class GenerateEmoji {
    private static final boolean           DRAFT                       = false;

    private static final boolean SAMSUNG = false;

    private static final String            DRAFT_TITLE_PREFIX          = DRAFT ? "Draft " : "";

    private static boolean                 SHOW                        = false;

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

    static final IndexUnicodeProperties    LATEST                      = IndexUnicodeProperties.make(Default.ucdVersion());
    static final UCA                       UCA_COLLATOR                = UCA.buildCollator(null);

    public static final UnicodeMap<String> STANDARDIZED_VARIANT        = LATEST.load(UcdProperty.Standardized_Variant);
    static {
        UnicodeSet HAS_EMOJI_VS = new UnicodeSet();
        for (String s : Emoji.EMOJI_CHARS_FLAT) {
            if (STANDARDIZED_VARIANT.containsKey(s + Emoji.EMOJI_VARIANT)) {
                HAS_EMOJI_VS.add(s);
            }
        }
        System.out.println("HAS_EMOJI_VS: " + HAS_EMOJI_VS.toPattern(false));
    }
    static final UnicodeMap<String>        VERSION                     = LATEST.load(UcdProperty.Age);
    static final UnicodeMap<String>        WHITESPACE                  = LATEST.load(UcdProperty.White_Space);
    static final UnicodeMap<String>        GENERAL_CATEGORY            = LATEST.load(UcdProperty.General_Category);
    static final UnicodeMap<String>        SCRIPT_EXTENSIONS           = LATEST.load(UcdProperty.Script_Extensions);
    private static final UnicodeSet        COMMON_SCRIPT               = new UnicodeSet()
    .addAll(SCRIPT_EXTENSIONS.getSet(UcdPropertyValues.Script_Values.Common
            .toString()))
            .freeze();

    static final UnicodeMap<String>        NFKCQC                      = LATEST.load(UcdProperty.NFKD_Quick_Check);
    static final UnicodeMap<String>        NAME                        = LATEST.load(UcdProperty.Name);
    static final UnicodeSet                JSOURCES                    = new UnicodeSet();
    static {
        UnicodeMap<String> dcmProp = LATEST.load(UcdProperty.Emoji_DCM);
        UnicodeMap<String> kddiProp = LATEST.load(UcdProperty.Emoji_KDDI);
        UnicodeMap<String> sbProp = LATEST.load(UcdProperty.Emoji_SB);
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
    static final LocaleDisplayNames        LOCALE_DISPLAY              = LocaleDisplayNames.getInstance(ULocale.ENGLISH);

    static final Pattern                   tab                         = Pattern.compile("\t");
    static final Pattern                   space                       = Pattern.compile(" ");
    static final String                    REPLACEMENT_CHARACTER       = "\uFFFD";

    static final MapComparator<String>     mp                          = new MapComparator<String>().setErrorOnMissing(false);

    static final Relation<String, String>  ORDERING_TO_CHAR            = new Relation(new LinkedHashMap(), LinkedHashSet.class);
    static {
        Set<String> sorted = new LinkedHashSet<>();
        Output<Set<String>> lastLabel = new Output<Set<String>>(new TreeSet<String>());
        for (String line : FileUtilities.in(GenerateEmoji.class, "emojiOrdering.txt")) {
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            line = Emoji.getLabelFromLine(lastLabel, line);
            for (int i = 0; i < line.length();) {
                line = Emoji.UNESCAPE.transform(line);
                String string = Emoji.getEmojiSequence(line, i);
                i += string.length();
                if (Emoji.skipEmojiSequence(string)) {
                    continue;
                }
                if (!sorted.contains(string)) {
                    sorted.add(string);
                    for (String item : lastLabel.value) {
                        ORDERING_TO_CHAR.put(item, string);
                    }
                }
            }
        }
        Set<String> missing = Emoji.EMOJI_CHARS.addAllTo(new LinkedHashSet());
        missing.removeAll(sorted);
        if (!missing.isEmpty()) {
            ORDERING_TO_CHAR.putAll("other", missing);
            throw new IllegalArgumentException("Missing some orderings: " + new UnicodeSet().addAll(missing));
        }
        sorted.addAll(missing);
        mp.add(sorted);
        mp.freeze();
        ORDERING_TO_CHAR.freeze();
    }

    public static final Comparator         CODEPOINT_COMPARE1           =
            new MultiComparator<String>(
                    mp,
                    UCA_COLLATOR, // don't
                    // need
                    // cldr
                    // features
                    new UTF16.StringComparator(true, false, 0));

    //    static final Comparator                CODEPOINT_COMPARE_SHORTER   =
    //            new MultiComparator<String>(
    //                    Emoji.CODEPOINT_LENGTH,
    //                    mp,
    //                    UCA_COLLATOR, // don't
    //                    // need
    //                    // cldr
    //                    // features
    //                    new UTF16.StringComparator(true, false, 0));

    static final Set<String>               SORTED_EMOJI_CHARS_SET;
    static {
        TreeSet<String> temp = new TreeSet<String>(CODEPOINT_COMPARE1);
        Emoji.EMOJI_CHARS.addAllTo(temp);
        SORTED_EMOJI_CHARS_SET = Collections.unmodifiableSortedSet(temp);
    }
    static final Comparator<String> CODEPOINT_COMPARE;
    static {
        String rules = appendCollationRules(new StringBuilder()).toString();
        try {
            CODEPOINT_COMPARE = (Comparator<String>) (Comparator) new RuleBasedCollator(rules);
        } catch (Exception e) {
            throw new ICUUncheckedIOException("Internal Error", e);
        }
    }
    static final Comparator<String> MULTI = CODEPOINT_COMPARE;

    static final EmojiAnnotations          ANNOTATIONS_TO_CHARS        = new EmojiAnnotations(CODEPOINT_COMPARE,
            "emojiAnnotations.txt",
            "emojiAnnotationsFlags.txt",
            "emojiAnnotationsGroups.txt"
            );
    static final EmojiAnnotations          ANNOTATIONS_TO_CHARS_GROUPS = new EmojiAnnotations(CODEPOINT_COMPARE,
            "emojiAnnotationsGroupsSpecial.txt"
            );
    static final UnicodeSet                DEFAULT_TEXT_STYLE          = new UnicodeSet()
    .addAll(ANNOTATIONS_TO_CHARS_GROUPS.keyToValues.get("default-text-style"))
    .freeze();

    // static final EmojiAnnotations ANNOTATIONS_TO_CHARS_NEW = new
    // EmojiAnnotations(CODEPOINT_COMPARE, "emojiAnnotationsNew.txt");

    static final Subheader                 subheader                   = new Subheader("/Users/markdavis/workspace/unicodetools/data/ucd/7.0.0-Update/");
    static final Set<String>               SKIP_BLOCKS                 = new HashSet(Arrays.asList("Miscellaneous Symbols",
            "Enclosed Alphanumeric Supplement",
            "Miscellaneous Symbols And Pictographs",
            "Miscellaneous Symbols And Arrows"));

    public static String getEmojiVariant(String browserChars, String variant) {
        int first = browserChars.codePointAt(0);
        String probe = new StringBuilder()
        .appendCodePoint(first)
        .append(variant).toString();
        if (STANDARDIZED_VARIANT.get(probe) != null) {
            browserChars = probe + browserChars.substring(Character.charCount(first));
        }
        return browserChars;
    }

    static final UnicodeSet HAS_EMOJI_VS = new UnicodeSet();
    static {
        for (String vs : STANDARDIZED_VARIANT.keySet()) {
            if (vs.contains(Emoji.TEXT_VARIANT_STRING)) {
                HAS_EMOJI_VS.add(vs.codePointAt(0));
            }
        }
        HAS_EMOJI_VS.freeze();
    }

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
        plain, text, emoji, bestImage, refImage
    }

    static final Relation<Style, String> STYLE_TO_CHARS = Relation.of(new EnumMap(Style.class), TreeSet.class, CODEPOINT_COMPARE);

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
                new TreeMap<String,Set<Label>>(CODEPOINT_COMPARE),
                new EnumMap<Label,Set<String>>(Label.class),
                TreeSet.class,
                TreeSet.class,
                LABEL_COMPARE,
                CODEPOINT_COMPARE
                );

        static {
            Output<Set<String>> lastLabel = new Output(new TreeSet<String>(CODEPOINT_COMPARE));
            String sublabel = null;
            for (String line : FileUtilities.in(GenerateEmoji.class, "emojiLabels.txt")) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                line = Emoji.getLabelFromLine(lastLabel, line);
                for (int i = 0; i < line.length();) {
                    line = Emoji.UNESCAPE.transform(line);
                    String string = Emoji.getEmojiSequence(line, i);
                    i += string.length();
                    if (Emoji.skipEmojiSequence(string)) {
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
            if (!Emoji.EMOJI_CHARS.containsAll(string)) {
                return;
            }
            CHARS_TO_LABELS.add(string, lastLabel);
        }
    }

    private static String getAnchor(String code) {
        return code.replace(" ", "_").replace("+", "").replace("U", "");
    }

    private static final String MISSING_CELL  = "<td class='miss'>missing</td>\n";

    static class Data implements Comparable<Data> {
        final String                       chars;
        final String                       code;
        final UcdPropertyValues.Age_Values age;
        final Style                        defaultPresentation;
        final Set<Label>                   labels;
        final String                       name;
        // static final Relation<Label, Data> LABELS_TO_DATA
        // = Relation.of(new EnumMap(Label.class), TreeSet.class); // , BY_LABEL

        static final UnicodeSet            DATA_CHARACTERS = new UnicodeSet();

        static final UnicodeSet            missingJSource  = new UnicodeSet(JSOURCES);
        static Map<String, Data>           STRING_TO_DATA  = new TreeMap<>();

        @Override
        public boolean equals(Object obj) {
            return chars.equals(((Data) obj).chars);
        }

        @Override
        public int hashCode() {
            return chars.hashCode();
        }

        @Override
        public int compareTo(Data o) {
            // int diff = age.compareTo(o.age);
            // if (diff != 0) {
            // return diff;
            // }
            return CODEPOINT_COMPARE.compare(chars, o.chars);
        }

        static final UnicodeSet          EMOJI_STYLE_OVERRIDE = new UnicodeSet("[🔙 🔚 🔛 🔜 🔝➕ ➖ ➗ ➰ ➿]").freeze();
        private static final Set<String> SUPPRESS_ANNOTATIONS = new HashSet<>(Arrays.asList("default-text-style"));

        public Data(String chars, String code, String age,
                String defaultPresentation, String name) {
            this.chars = chars;
            if (chars.contains(Emoji.EMOJI_VARIANT_STRING) || chars.contains(Emoji.TEXT_VARIANT_STRING)) {
                throw new IllegalArgumentException();
            }
            this.code = code;
            this.age = UcdPropertyValues.Age_Values.valueOf(age.replace('.', '_'));
            this.defaultPresentation = DEFAULT_TEXT_STYLE.contains(chars) ? Style.text : Style.emoji;
            this.labels = storeLabels();
            this.name = getName(chars, true);
            // addWords(chars, name);
            DATA_CHARACTERS.add(chars);
            // for (Label label : labels) {
            // LABELS_TO_DATA.put(label, this);
            // }
            if (!Utility.fromHex(code).equals(chars)) {
                throw new IllegalArgumentException();
            }
        }

        public Data(int codepoint) {
            this(new StringBuilder().appendCodePoint(codepoint).toString());
        }

        public Data(String s) {
            this(s,
                    "U+" + Utility.hex(s, " U+"),
                    VERSION.get(s.codePointAt(0)).replace("_", "."),
                    "text",
                    Default.ucd().getName(s));
        }

        private Set<Label> storeLabels() {
            Set<Label> labels2 = Label.CHARS_TO_LABELS.getValues(chars); // override
            if (labels2 == null) {
                if (chars.equals("🇽🇰")) {
                    labels2 = Collections.singleton(Label.flag);
                } else {
                    labels2 = Collections.singleton(Label.sign);
                    if (SHOW)
                        System.out.println("*** No specific label for " + Utility.hex(chars) + " " + NAME.get(chars.codePointAt(0)));
                }
            }
            return Collections.unmodifiableSet(labels2);
        }

        static final Data parseLine(String line) {
            String[] items = tab.split(line);
            // U+2194 V1.1 text arrows ↔ LEFT RIGHT ARROW
            String code1 = items[0];
            String age1 = items[1];
            String defaultPresentation = items[2];
            String temp = items[3];
            if (temp.isEmpty()) {
                temp = "misc";
            }
            // EnumSet labelSet = EnumSet.noneOf(Label.class);
            // for (String label : Arrays.asList(space.split(temp))) {
            // Label newLabel = Label.get(label);
            // labelSet.add(newLabel);
            // }

            String chars1 = items[4];
            if (!Emoji.EMOJI_CHARS.containsAll(chars1)) {
                if (SHOW)
                    System.out.println("Skipping " + getCodeAndName(chars1, " "));
                return null;
            }
            temp = items[5];
            if (temp.startsWith("flag")) {
                temp = "flag for" + temp.substring(4);
            }
            String name1 = temp;
            return new Data(chars1, code1, age1, defaultPresentation, name1);
        }

        static void add(String line) {
            Data data = parseLine(line);
            addNewItem(data, STRING_TO_DATA);
        }

        @Override
        public String toString() {
            return code
                    + "\t" + getVersionAndSources()
                    + "\t" + defaultPresentation
                    + "\t" + chars
                    + "\t" + name;
        }

        private String getVersionAndSources() {
            return getSources(new StringBuilder(getVersion()), true);
        }

        public String getVersion() {
            return age.toString().replace('_', '.');
        }

        public String getSources(StringBuilder suffix, boolean superscript) {
            boolean first = true;
            for (CharSource source : getCharSources(chars)) {
                suffix.append(superscript ? source.superscript
                        : first ? source.letter
                                : " " + source.letter);
                first = false;
            }
            return suffix.toString();
        }

        public static Set<CharSource> getCharSources(String s) {
            Set<CharSource> source = EnumSet.noneOf(CharSource.class);
            if (DINGBATS.contains(s)) {
                source.add(CharSource.ZDings);
            }
            if (JSOURCES.contains(s)) {
                source.add(CharSource.JCarrier);
            }
            if (DING_MAP.containsKey(s)) {
                source.add(CharSource.WDings);
            }
            if (ARIB.contains(s)) {
                source.add(CharSource.ARIB);
            }
            if (source.size() == 0) {
                source.add(CharSource.Other);
            }
            return source;
        }

        public String toHtmlString(Form form, int item, Stats stats) {
            String missingCell = MISSING_CELL;
            String symbolaCell = getCell(Emoji.Source.ref, chars, missingCell);
            // String symbolaCell =
            // Emoji.isRegionalIndicator(chars.codePointAt(0))
            // ? getCell("ref", core, missingCell)
            // : "<td class='symb'>" + chars + "</td>\n";
            // getFlag(chars)
            // if (symbolaChars == null) {
            // symbolaChars = chars;
            // }
            // int firstCodepoint = chars.codePointAt(0);
            // int firstLen = Character.charCount(firstCodepoint);
            // int secondCodepoint = firstLen >= chars.length() ? 0 :
            // chars.codePointAt(firstLen);

            // String header =
            // Default.ucd().getBlock(firstCodepoint).replace('_', ' ');
            // String subhead = subheader.getSubheader(firstCodepoint);
            // if (SKIP_BLOCKS.contains(header)) {
            // header = "<i>" + subhead + "</i>";
            // } else if (!header.equalsIgnoreCase(subhead)) {
            // header += ": <i>" + subhead + "</i>";
            // }

            String appleCell = getCell(Emoji.Source.apple, chars, missingCell);
            String androidCell = getCell(Emoji.Source.android, chars, missingCell);
            String twitterCell = getCell(Emoji.Source.twitter, chars, missingCell);
            String windowsCell = getCell(Emoji.Source.windows, chars, missingCell);
            String samsungCell = SAMSUNG ? getCell(Emoji.Source.samsung, chars, missingCell) : missingCell;
            String gmailCell = getCell(Emoji.Source.gmail, chars, missingCell);
            String sbCell = getCell(Emoji.Source.sb, chars, missingCell);
            String dcmCell = getCell(Emoji.Source.dcm, chars, missingCell);
            String kddiCell = getCell(Emoji.Source.kddi, chars, missingCell);
            if (stats != null) {
                stats.add(chars, Emoji.Source.apple, appleCell.equals(missingCell));
                stats.add(chars, Emoji.Source.android, androidCell.equals(missingCell));
                stats.add(chars, Emoji.Source.twitter, twitterCell.equals(missingCell));
                stats.add(chars, Emoji.Source.windows, windowsCell.equals(missingCell));
                stats.add(chars, Emoji.Source.samsung, samsungCell.equals(missingCell));
                stats.add(chars, Emoji.Source.gmail, gmailCell.equals(missingCell));
                stats.add(chars, Emoji.Source.dcm, dcmCell.equals(missingCell));
                stats.add(chars, Emoji.Source.kddi, kddiCell.equals(missingCell));
                stats.add(chars, Emoji.Source.sb, sbCell.equals(missingCell));
            }

            String browserCell = "<td class='chars'>" + getEmojiVariant(chars, Emoji.EMOJI_VARIANT_STRING) + "</td>\n";

            String textChars = getEmojiVariant(chars, Emoji.TEXT_VARIANT_STRING);
            Set<String> annotations = new LinkedHashSet<String>(ifNull(GenerateEmoji.ANNOTATIONS_TO_CHARS.getKeys(chars), Collections.EMPTY_SET));
            annotations.removeAll(SUPPRESS_ANNOTATIONS);
            StringBuilder annotationString = new StringBuilder();
            if (!annotations.isEmpty()) {
                for (String annotation : annotations) {
                    if (annotationString.length() != 0) {
                        annotationString.append(", ");
                    }
                    annotationString.append(getLink("emoji-annotations.html#" + annotation, annotation, "annotate"));
                }
            }
            String anchor = getAnchor(code);
            return "<tr>"
            + "<td class='rchars'>" + item + "</td>\n"
            + "<td class='code'>" + getDoubleLink(anchor, code) + "</td>\n"
            + (form != Form.fullForm && form != Form.extraForm ? symbolaCell
                    : browserCell
                    + symbolaCell
                    + appleCell
                    + androidCell
                    + twitterCell
                    + windowsCell
                    + (SAMSUNG ? samsungCell : "")
                    + gmailCell
                    + dcmCell
                    + kddiCell
                    + sbCell
                    )
                    // + browserCell
                    + (form.compareTo(Form.shortForm) <= 0 ? "" :
                        "<td class='name'>" + name + "</td>\n")
                        + "<td class='age'>" + getVersionAndSources() + "</td>\n"
                        + "<td class='default'>" + defaultPresentation + (!textChars.equals(chars) ? "*" : "") + "</td>\n"
                        + (form.compareTo(Form.shortForm) <= 0 ? "" :
                            "<td class='name'>"
                            + annotationString
                            // + CollectionUtilities.join(labels, ", ")
                            // + (annotationString.length() == 0 ? "" :
                            // ";<br>" + annotationString)
                            + "</td>\n"
                            // + "<td class='name'>" + header + "</td>"
                                )
                                + "</tr>";
        }

        enum DataStyle {
            plain, cldr
        }

        public String toSemiString(int order, UnicodeSet level1) {
            // "# Code ;\tDefault Style ;\tLevel ;\tModifier ;\tSources ;\tVersion\t# (Character) Name\n"

            String extraString = "";
            if (level1 != null) {
                ModifierStatus modifier = MODIFIER_STATUS.get(chars);
                extraString = " ;\t" + (level1.contains(chars) ? "L1" : "L2")
                        + " ;\t" + modifier;
            }
            return Utility.hex(chars, " ")
                    + " ;\t" + defaultPresentation
                    + extraString
                    + " ;\t" + getSources(new StringBuilder(), false)
                    + "\t# " + getVersion()
                    + " (" + chars
                    + ") " + getName(chars, false);
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

        public String getCell(Emoji.Source type, String core, String missingCell) {
            //String core = Emoji.buildFileName(chars, "_");

            String filename = Emoji.getImageFilenameFromChars(type, core);
            String androidCell = missingCell;
            if (filename != null) {
                // final File file = new File(IMAGES_OUTPUT_DIR, filename);
                String fullName = // type == Source.gmail ? filename :
                        getDataUrl(filename);
                if (fullName != null) {
                    String className = type.getClassAttribute(chars);
                    // String className = "imga";
                    // if (type == Source.ref && getFlagCode(chars) != null) {
                    // className = "imgf";
                    // }
                    androidCell = "<td class='andr'><img alt='" + chars + "' class='" +
                            className + "' src='" + fullName + "'></td>\n";
                }
            }
            return androidCell;
        }

        public static String toHtmlHeaderString(Form form) {
            boolean shortForm = form.compareTo(Form.shortForm) <= 0;
            return "<tr>"
            + "<th>Count</th>\n"
            + "<th class='rchars'>Code</th>\n"
            + (form != Form.fullForm && form != Form.extraForm
            ? "<th class='cchars'>B&amp;W*</th>\n" :
                "<th class='cchars'>Browser</th>\n"
                + "<th class='cchars'>B&amp;W*</th>\n"
                + "<th class='cchars'>Apple</th>\n"
                + "<th class='cchars'>Andr</th>\n"
                + "<th class='cchars'>Twit</th>\n"
                + "<th class='cchars'>Wind</th>\n"
                + (SAMSUNG ? "<th class='cchars'>Sams</th>\n" : "")
                + "<th class='cchars'>GMail</th>\n"
                + "<th class='cchars'>DCM</th>\n"
                + "<th class='cchars'>KDDI</th>\n"
                + "<th class='cchars'>SB</th>\n"
                    )
                    // + "<th class='cchars'>Browser</th>\n"
                    + (shortForm ? "" :
                        "<th>Name</th>\n"
                        + "<th>Version</th>\n"
                        + "<th>Default</th>\n"
                        + "<th>Annotations</th>\n"
                        // + "<th>Block: <i>Subhead</i></th>\n"
                            )
                            + "</tr>";
        }
    }

    enum ModifierStatus {
        none, modifier, primary, secondary
    }

    static final UnicodeMap<ModifierStatus> MODIFIER_STATUS = new UnicodeMap<ModifierStatus>()
            .putAll(Emoji.EMOJI_CHARS, ModifierStatus.none)
            .putAll(new UnicodeSet("[\\x{1F3FB}\\x{1F3FC}\\x{1F3Fd}\\x{1F3Fe}\\x{1F3Ff}]"),
                    ModifierStatus.none)
                    .putAll(new UnicodeSet("[\\x{1F3FB}\\x{1F3FC}\\x{1F3Fd}\\x{1F3Fe}\\x{1F3Ff}]"),
                            ModifierStatus.modifier)
                            .putAll(new UnicodeSet().addAll(ANNOTATIONS_TO_CHARS_GROUPS.getValues("fitz-primary")),
                                    ModifierStatus.primary)
                                    .putAll(new UnicodeSet().addAll(ANNOTATIONS_TO_CHARS_GROUPS.getValues("fitz-secondary")),
                                            ModifierStatus.secondary)
                                            .freeze();

    static final UnicodeMap<Emoji.Source> BEST_OVERRIDE = new UnicodeMap<>();
    static {
        BEST_OVERRIDE.putAll(new UnicodeSet("[🕐-🕧🚶🏃💃👪👫👬👭🙍🙎🙅🙆🙇🙋🙌🙏💮]"), Emoji.Source.android);
        BEST_OVERRIDE.putAll(new UnicodeSet("[✊-✌ 💅💪👂👃👯" +
                "👦 👰 👧  👨  👩  👮  👱  👲  👳 👴  👵  👶  👷  👸  💁  💂 👼" +
                "👈👉☝👆👇👊  👋  👌  👍👎 👏  👐]"), Emoji.Source.twitter);
        BEST_OVERRIDE.putAll(new UnicodeSet("[💆👯💏💑💇]"), Emoji.Source.windows);
        BEST_OVERRIDE.putAll(new UnicodeSet("[💇]"), Emoji.Source.ref);
        BEST_OVERRIDE.freeze();
    }

    public static String getBestImage(String s, boolean useDataURL, String extraClasses, Emoji.Source... doFirst) {
        // if (doFirst.length == 0) {
        // Source source0 = BEST_OVERRIDE.get(s);
        // if (source0 != null) {
        // doFirst = new Source[]{source0};
        // }
        // }
        for (Emoji.Source source : orderedEnum(doFirst)) {
            String cell = getImage(source, s, useDataURL, extraClasses);
            if (cell != null) {
                return cell;
            }
        }
        throw new IllegalArgumentException("Can't find image for: " + Utility.hex(s) + " " + getName(s, true) + "\t" + Emoji.buildFileName(s, "_"));
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
                    " src='" + (useDataUrl ? getDataUrl(filename) : "images/" + filename) + "'" +
                    " title='" + getCodeAndName(chars, " ") + "'" +
                    ">";
        }
        return null;
    }

    public static File getBestFile(String s, Emoji.Source... doFirst) {
        for (Emoji.Source source : orderedEnum(doFirst)) {
            File file = getImageFile(source, s);
            if (file != null) {
                return file;
            }
        }
        return null;
    }

    public static Iterable<Emoji.Source> orderedEnum(Emoji.Source... doFirst) {
        if (doFirst.length == 0) {
            return Arrays.asList(Emoji.Source.values());
        }
        LinkedHashSet<Emoji.Source> ordered = new LinkedHashSet<>(Arrays.asList(doFirst));
        ordered.addAll(Arrays.asList(Emoji.Source.values()));
        return ordered;
    }

    static public File getImageFile(Emoji.Source type, String chars) {
        String filename = Emoji.getImageFilenameFromChars(type, chars);
        if (filename != null) {
            File file = new File(Emoji.IMAGES_OUTPUT_DIR, filename);
            if (file.exists()) {
                return file;
            }
        }
        return null;
    }

    private static class Stats {
        enum Type {
            carriers(GenerateEmoji.JCARRIERS),
            commonAdditions(Emoji.COMMON_ADDITIONS),
            flags(Emoji.FLAGS),
            standardAdditions(nc7),
            standardAdditions8(nc8);
            final UnicodeSet items;

            Type(UnicodeSet _items) {
                items = _items;
            }

            static Type getType(String chars) {
                for (Type t : Type.values()) {
                    if (t.items.contains(chars)) {
                        return t;
                    }
                }
                return null;
            }
        } // cards, dominos, majong,

        // static final UnicodeSet DOMINOS = new UnicodeSet("[🀰-🂓]");
        // static final UnicodeSet CARDS = new UnicodeSet("[🂠-🃵]");
        // static final UnicodeSet MAHJONG = new UnicodeSet("[🀀-🀫]");
        final EnumMap<Type, EnumMap<Emoji.Source, UnicodeSet>> data      = new EnumMap<>(Type.class);
        final EnumMap<Emoji.Source, UnicodeSet>                totalData = new EnumMap<>(Emoji.Source.class);
        {
            for (Emoji.Source s : Emoji.Source.values()) {
                totalData.put(s, new UnicodeSet());
            }
        }

        public void add(
                String chars,
                Emoji.Source source,
                boolean isMissing) {
            if (isMissing) {

                // per type
                Type type = Type.getType(chars);
                // VERSION70.containsAll(chars) ? Type.v70
                // :
                // getFlagCode(chars) != null ? Type.countries
                // : DOMINOS.containsAll(chars) ? Type.dominos
                // : CARDS.containsAll(chars) ? Type.cards
                // : MAHJONG.containsAll(chars) ? Type.majong
                // : Type.misc;
                EnumMap<Emoji.Source, UnicodeSet> counter = data.get(type);
                if (counter == null) {
                    data.put(type, counter = new EnumMap<Emoji.Source, UnicodeSet>(Emoji.Source.class));
                }
                UnicodeSet us = counter.get(source);
                if (us == null) {
                    counter.put(source, us = new UnicodeSet());
                }
                us.add(chars);
            } else {
                // total data
                totalData.get(source).add(chars);
            }
        }

        static EnumSet<Emoji.Source> platformsToInclude = EnumSet.allOf(Emoji.Source.class);
        static {
            platformsToInclude.remove(Emoji.Source.ref);
            platformsToInclude.remove(Emoji.Source.gmail);
            platformsToInclude.remove(Emoji.Source.sb);
            platformsToInclude.remove(Emoji.Source.dcm);
            platformsToInclude.remove(Emoji.Source.kddi);
            platformsToInclude.remove(Emoji.Source.twitter);
        }

        public void write() throws IOException {
            PrintWriter out = BagFormatter.openUTF8Writer(Emoji.TR51_OUTPUT_DIR,
                    "missing-emoji-list.html");
            UnicodeSet jc = JCARRIERS;
            // new UnicodeSet()
            // .addAll(totalData.get(Source.sb))
            // .addAll(totalData.get(Source.kddi))
            // .addAll(totalData.get(Source.dcm))
            // .freeze();
            UnicodeSet textStyle = new UnicodeSet();
            for (Entry<String, Data> s : Data.STRING_TO_DATA.entrySet()) {
                if (s.getValue().defaultPresentation == Style.text) {
                    textStyle.add(s.getKey());
                }
            }
            UnicodeSet needsVS = new UnicodeSet();
            for (String s : jc) {
                int first = s.codePointAt(0);
                if (!HAS_EMOJI_VS.contains(first) && textStyle.contains(first)) {
                    needsVS.add(first);
                }
            }

            System.out.println("All Emoji\t" + Emoji.EMOJI_CHARS.toPattern(false));

            System.out.println("needs VS\t" + needsVS.toPattern(false));

            System.out.println("gmail-jc" + "\t"
                    + new UnicodeSet(totalData.get(Emoji.Source.gmail)).removeAll(jc).toPattern(false));
            System.out.println("jc-gmail" + "\t"
                    + new UnicodeSet(jc).removeAll(totalData.get(Emoji.Source.gmail)).toPattern(false));

            for (Entry<Emoji.Source, UnicodeSet> entry : totalData.entrySet()) {
                System.out.println(entry.getKey() + "\t" + entry.getValue().toPattern(false));
            }

            writeHeader(out, "Missing", null, "Missing list of emoji characters.", "border='1'");
            String headerRow = "<tr><th>" + "Type" + "</th>";
            for (Emoji.Source type : platformsToInclude) {
                headerRow += "<th class='cchars' width='" + (80.0 / platformsToInclude.size()) + "%'>" + type + " missing</th>";
            }
            headerRow += "</tr>";

            for (Entry<Type, EnumMap<Emoji.Source, UnicodeSet>> entry : data.entrySet()) {
                showDiff(out, headerRow, entry.getKey().toString(), entry.getValue());
            }

            EnumMap<Emoji.Source, UnicodeSet> familyMap = new EnumMap<>(Emoji.Source.class);
            familyMap.put(Emoji.Source.android, Emoji.APPLE_COMBOS);
            familyMap.put(Emoji.Source.windows, Emoji.APPLE_COMBOS);
            showDiff(out, headerRow, "families", familyMap);

            EnumMap<Emoji.Source, UnicodeSet> diversityMap = new EnumMap<>(Emoji.Source.class);
            diversityMap.put(Emoji.Source.android, Emoji.APPLE_MODIFIED);
            diversityMap.put(Emoji.Source.windows, Emoji.APPLE_MODIFIED);
            showDiff(out, headerRow, "skinTone", diversityMap);

            writeFooter(out);
            out.close();
        }

        private void showDiff(PrintWriter out, String headerRow, final String title, 
                final Map<Emoji.Source, UnicodeSet> values) {
            // find common
            UnicodeSet common = null;
            boolean skipSeparate = true;
            for (Emoji.Source source : platformsToInclude) {
                final UnicodeSet uset = values.get(source);
                final UnicodeSet us = ifNull(uset, UnicodeSet.EMPTY);
                if (common == null) {
                    common = new UnicodeSet(us);
                } else if (!common.equals(us)) {
                    common.retainAll(us);
                    skipSeparate = false;
                }
            }
            out.println(headerRow);
            // per source
            String sectionLink = getDoubleLink(title);
            if (!skipSeparate) {
                out.print("<tr><th>" + sectionLink + " count</th>");
                sectionLink = title;
                for (Emoji.Source source : platformsToInclude) {
                    final UnicodeSet us = ifNull(values.get(source), UnicodeSet.EMPTY);
                    out.print("<td class='cchars'>" + (us.size() - common.size()) + "</td>");
                }
                out.print("</tr>");
                out.print("<tr><th>" + title + " chars</th>");
                for (Emoji.Source source : platformsToInclude) {
                    final UnicodeSet us = ifNull(values.get(source), UnicodeSet.EMPTY);
                    displayUnicodeSet(out, new UnicodeSet(us).removeAll(common), Style.bestImage, 0, 1, 1, "../../emoji/charts/full-emoji-list.html", CODEPOINT_COMPARE);
                }
                out.print("</tr>");
            }
            // common
            out.println("<tr><th>Common</th>"
                    + "<th class='cchars' colSpan='" + platformsToInclude.size() + "'>"
                    + "common missing" + "</th></tr>");
            out.println("<tr><th>" + sectionLink + " count</th>"
                    + "<td class='cchars' colSpan='" + platformsToInclude.size() + "'>"
                    + common.size() + "</td></tr>");
            if (common.size() != 0) {
                out.println("<tr><th>" + title + "</th>");
                displayUnicodeSet(out, common, Style.bestImage, 0, platformsToInclude.size(), 1, null, CODEPOINT_COMPARE);
                out.println("</td></tr>");
            }
        }
    }

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

    static final Birelation<String, String> OLD_ANNOTATIONS_TO_CHARS = new Birelation<>(
            new TreeMap(CODEPOINT_COMPARE),
            new TreeMap(CODEPOINT_COMPARE),
            TreeSet.class,
            TreeSet.class,
            CODEPOINT_COMPARE,
            CODEPOINT_COMPARE);
    static {
        addOldAnnotations();
    }

    private static void compareOtherAnnotations() {
        for (Entry<String, Set<String>> entry : OLD_ANNOTATIONS_TO_CHARS.valueKeysSet()) {
            String chars = entry.getKey();
            Set<String> oldAnnotations = entry.getValue();

            Set<String> newAnnotations = new TreeSet(ifNull(GenerateEmoji.ANNOTATIONS_TO_CHARS.getKeys(chars), Collections.EMPTY_SET));
            Set<Label> labels = ifNull(Label.CHARS_TO_LABELS.getValues(chars), Collections.EMPTY_SET);
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

    static String getFlagCode(String chars) {
        int firstCodepoint = chars.codePointAt(0);
        if (!Emoji.isRegionalIndicator(firstCodepoint)) {
            return null;
        }
        int firstLen = Character.charCount(firstCodepoint);
        int secondCodepoint = firstLen >= chars.length() ? 0 : chars.codePointAt(firstLen);
        if (!Emoji.isRegionalIndicator(secondCodepoint)) {
            return null;
        }
        secondCodepoint = chars.codePointAt(2);
        String cc = (char) (firstCodepoint - Emoji.FIRST_REGIONAL + 'A')
                + ""
                + (char) (secondCodepoint - Emoji.FIRST_REGIONAL + 'A');
        // String remapped = REMAP_FLAGS.get(cc);
        // if (remapped != null) {
        // cc = remapped;
        // }
        // if (REPLACEMENT_CHARACTER.equals(cc)) {
        // return null;
        // }
        return cc;
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
            if (NAME.get(realChars.codePointAt(0)) == null) {
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
            if (!SKIP_WORDS.contains(word) && word.length() > 1 && getFlagCode(chars) == null) {
                OLD_ANNOTATIONS_TO_CHARS.add(word, chars);
            }
        }
    }

    static String getFlag(String chars, String extraClasses) {
        String filename = Emoji.getImageFilenameFromChars(Emoji.Source.ref, chars);
        String cc = getFlagRegionName(chars);
        return cc == null ? null : "<img"
                + " alt='" + chars + "'"
                + " class='imgf" + extraClasses + "'"
                + " title='" + getCodeAndName(chars, " ") + "'"
                + " src='" + getDataUrl(filename) + "'>";
    }

    public static void main(String[] args) throws IOException {
        for (String line : FileUtilities.in(GenerateEmoji.class, "emojiData.txt")) {
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            Data.add(line);
        }
        for (String s : Label.CHARS_TO_LABELS.keySet()) {
            if (!Data.STRING_TO_DATA.containsKey(s)) {
                addNewItem(s, Data.STRING_TO_DATA);
            }
        }
        for (String s : Emoji.EMOJI_CHARS) {
            if (!Data.STRING_TO_DATA.containsKey(s)) {
                addNewItem(s, Data.STRING_TO_DATA);
                if (SHOW)
                    System.out.println(s);
            }
        }
        LinkedHashMap<String, Data> missingMap = new LinkedHashMap<>();
        addFileCodepoints(new File(Emoji.IMAGES_OUTPUT_DIR), missingMap);
        UnicodeSet fileChars = new UnicodeSet().addAll(missingMap.keySet()).removeAll(Emoji.EMOJI_CHARS);
        System.out.println("MISSING: " + fileChars.toPattern(false));

        // show data
        System.out.println("Total Emoji:\t" + Emoji.EMOJI_CHARS.size());
        UnicodeSet newItems = new UnicodeSet();
        newItems.addAll(Data.STRING_TO_DATA.keySet());
        newItems.removeAll(JSOURCES);
        UnicodeSet newItems70 = new UnicodeSet(newItems).retainAll(VERSION70);
        UnicodeSet newItems63 = new UnicodeSet(newItems).removeAll(newItems70);
        UnicodeSet newItems63flags = getStrings(newItems63);
        newItems63.removeAll(newItems63flags);
        System.out.println("Other 6.3 Flags:\t" + newItems63flags.size() + "\t" + newItems63flags);
        System.out.println("Other 6.3:\t" + newItems63.size() + "\t" + newItems63);
        System.out.println("Other 7.0:\t" + newItems70.size() + "\t" + newItems70);
        // Data.missingJSource.removeAll(new
        // UnicodeSet("[\\u2002\\u2003\\u2005]"));
        if (Data.missingJSource.size() > 0) {
            throw new IllegalArgumentException("Missing: " + Data.missingJSource);
        }
        // print(Form.imagesOnly, columns, Data.STRING_TO_DATA);
        // print(Form.shortForm, Data.STRING_TO_DATA);
        // System.out.println(Data.LABELS_TO_DATA.keySet());

        Stats stats = new Stats();
        print(Form.fullForm, Data.STRING_TO_DATA, stats);
        stats.write();
        print(Form.noImages, Data.STRING_TO_DATA, null);
        printData(Data.STRING_TO_DATA, stats);
        // print(Form.extraForm, missingMap, null);
        showNewCharacters();
        for (String e : Emoji.EMOJI_CHARS) {
            Data data = Data.STRING_TO_DATA.get(e);
            if (data == null) {
                STYLE_TO_CHARS.put(Style.text, e);
            } else {
                STYLE_TO_CHARS.put(data.defaultPresentation, e);
            }
        }
        printCollationOrder();
        printAnnotations();
        STYLE_TO_CHARS.freeze();
        showTextStyle();
        showOrdering(Style.bestImage);
        // showOrdering(Style.refImage);
        showLabels();
        showVersions();
        showVersionsOnly();
        showDefaultStyle();
        showSequences();
        // showSubhead();
        showAnnotations(Emoji.CHARTS_DIR, "emoji-annotations.html", Emoji.EMOJI_CHARS, null, false);
        showAnnotations(Emoji.TR51_OUTPUT_DIR, "emoji-annotations-flags.html", Emoji.FLAGS, GROUP_ANNOTATIONS, true);
        showAnnotations(Emoji.TR51_OUTPUT_DIR, "emoji-annotations-groups.html", Emoji.EMOJI_CHARS, GROUP_ANNOTATIONS, false);
        showAnnotationsBySize(Emoji.TR51_OUTPUT_DIR, "emoji-annotations-size.html", Emoji.EMOJI_CHARS_WITHOUT_FLAGS);

        // showAnnotationsDiff();
        // compareOtherAnnotations();
        showOtherUnicode();
        // oldAnnotationDiff();
        // check twitter glyphs

        if (SHOW) {
            System.out.println("static final UnicodeSet EMOJI_CHARS = new UnicodeSet(\n\"" + Data.DATA_CHARACTERS.toPattern(false) + "\");");
            // getUrlCharacters("TWITTER", TWITTER_URL);
            // getUrlCharacters("APPLE", APPLE_URL);
            System.out.println(new UnicodeSet(Emoji.GITHUB_APPLE_CHARS).removeAll(APPLE_CHARS).toPattern(false));
            System.out.println(list(new UnicodeSet(APPLE_CHARS).removeAll(Emoji.GITHUB_APPLE_CHARS)));
            System.out.println("Apple: " + APPLE_CHARS);
        }
        System.out.println("DONE");
    }

    static Set<Emoji.Source>                  MAIN_SOURCES  = Collections.unmodifiableSet(EnumSet.of(Emoji.Source.apple, Emoji.Source.android, Emoji.Source.twitter, Emoji.Source.windows));

    static final IndexUnicodeProperties latest        = IndexUnicodeProperties.make(GenerateEnums.ENUM_VERSION);
    static final UnicodeMap<String>     emojiDCM      = latest.load(UcdProperty.Emoji_DCM);
    static final UnicodeMap<String>     emojiKDDI     = latest.load(UcdProperty.Emoji_KDDI);
    static final UnicodeMap<String>     emojiSB       = latest.load(UcdProperty.Emoji_SB);

    static final UnicodeSet             JCARRIERS     = new UnicodeSet()
    .addAll(emojiDCM.keySet())
    .addAll(emojiKDDI.keySet())
    .addAll(emojiSB.keySet())
    .removeAll(new UnicodeSet("[:whitespace:]"))
    .freeze();

    static final UnicodeSet             otherStandard = Emoji.COMMON_ADDITIONS;
    // new UnicodeSet(carriers);
    // static {
    // for (String s : Emoji.EMOJI_CHARS) {
    // String image = getImage(Source.apple, s, false, "");
    // if (image != null) {
    // otherStandard.add(s);
    // }
    // }
    // // HACK for now
    // otherStandard.remove("🖖");
    // otherStandard.removeAll(carriers).freeze();
    // }

    static final UnicodeSet             LEVEL1        = new UnicodeSet(JCARRIERS).addAll(otherStandard).freeze();

    static final UnicodeSet             nc            = new UnicodeSet(Emoji.EMOJI_CHARS)
    .removeAll(JCARRIERS)
    .removeAll(otherStandard)
    .removeAll(Emoji.FLAGS)
    .freeze();

    static final UnicodeSet             nc8           = new UnicodeSet(nc)
    .removeAll(new UnicodeSet("[:age=7.0:]"))
    .removeAll(nc.strings())
    .freeze();

    static final UnicodeSet             nc7           = new UnicodeSet(nc)
    .removeAll(nc8)
    .freeze();

    static final UnicodeSet             otherFlags    = new UnicodeSet(Emoji.FLAGS)
    .removeAll(JCARRIERS).freeze();

    private static void showNewCharacters() throws IOException {
        Set<String> optional = ANNOTATIONS_TO_CHARS_GROUPS.getValues("fitz-secondary");
        Set<String> minimal = ANNOTATIONS_TO_CHARS_GROUPS.getValues("fitz-primary");

        // Set<String> newChars =
        // ANNOTATIONS_TO_CHARS.getValues("fitz-minimal");
        PrintWriter out = BagFormatter.openUTF8Writer(Emoji.TR51_OUTPUT_DIR, "emoji-count.html");
        writeHeader(out, "Groupings and counts for screenshots and UnicodeJsps", null, "no message", "border='1' width='1200pt'");
        showRow(out, "Primary", minimal, true);
        showRow(out, "Secondary", optional, true);
        //        UnicodeSet modifierBase = new UnicodeSet().addAll(minimal).addAll(optional);
        //        showRow(out, "Modifier_Base", modifierBase.addAllTo(new TreeSet<String>(CODEPOINT_COMPARE)), false);
        final UnicodeSet MODIFIERS = new UnicodeSet("[\\x{1F3FB}-\\x{1F3FF}]").freeze();
        showRow(out, "Modifiers", MODIFIERS.addAllTo(new TreeSet<String>(CODEPOINT_COMPARE)), false);
        showRow(out, "JCarriers", JCARRIERS.addAllTo(new TreeSet<String>(CODEPOINT_COMPARE)), true);
        showRow(out, "Common Additions", otherStandard.addAllTo(new TreeSet<String>(CODEPOINT_COMPARE)), true);
        showRow(out, "Other Flags", otherFlags.addAllTo(new TreeSet<String>(CODEPOINT_COMPARE)), true);
        showRow(out, "Standard Additions", nc7.addAllTo(new TreeSet<String>(CODEPOINT_COMPARE)), true);
        showRow(out, "Standard Additions8", nc8.addAllTo(new TreeSet<String>(CODEPOINT_COMPARE)), true);

        // for unicodejsps
        UnicodeSet singletons = new UnicodeSet(Emoji.EMOJI_CHARS).removeAll(Emoji.EMOJI_CHARS.strings());
        showRowSet(out, "Singletons", singletons.addAllTo(new TreeSet<String>(CODEPOINT_COMPARE)));
        showRowSet(out, "Groupings", Emoji.APPLE_COMBOS.addAllTo(new TreeSet<String>(CODEPOINT_COMPARE)));
        showRowSet(out, "Diverse Primary", getDiverse(minimal, MODIFIERS).addAllTo(new TreeSet<String>(CODEPOINT_COMPARE)));
        Set<String> face = ANNOTATIONS_TO_CHARS.getValues("face");
        Set<String> optional_face = new HashSet<>(optional);
        optional_face.retainAll(face);
        Set<String> optional_other = new HashSet<>(optional);
        optional_other.removeAll(face);
        showRowSet(out, "Diverse Secondary Face", getDiverse(optional_face, MODIFIERS).addAllTo(new TreeSet<String>(CODEPOINT_COMPARE)));
        showRowSet(out, "Diverse Secondary Other", getDiverse(optional_other, MODIFIERS).addAllTo(new TreeSet<String>(CODEPOINT_COMPARE)));
        // UnicodeSet keycapBase = new UnicodeSet();
        // for (String s : Emoji.EMOJI_CHARS.strings()) {
        // if (s.indexOf(Emoji.KEYCAP_MARK) > 0) {
        // keycapBase.add(s.codePointAt(0));
        // }
        // }
        // showRow(out, "KeycapBase", keycapBase.addAllTo(new
        // TreeSet<String>(CODEPOINT_COMPARE)), true);
        //        showRow(out, "RegionalIndicators", Emoji.REGIONAL_INDICATORS.addAllTo(new TreeSet<String>(CODEPOINT_COMPARE)), true);
        writeFooter(out);
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
        // + "\t" + "http://unicode.org/Public/emoji/1.0/full-emoji-list.html#"
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
        for (Entry<String, Data> dataEntry : Data.STRING_TO_DATA.entrySet()) {
            Data data = dataEntry.getValue();
            if (data.defaultPresentation == Style.text) {
                defaultText.add(data.chars);
            }
        }
        if (!DEFAULT_TEXT_STYLE.equals(defaultText)) {
            throw new IllegalArgumentException(new UnicodeSet(defaultText).removeAll(DEFAULT_TEXT_STYLE)
                    + ", "
                    + new UnicodeSet(DEFAULT_TEXT_STYLE).removeAll(defaultText));
        }
        defaultText.freeze();
        PrintWriter out = BagFormatter.openUTF8Writer(Emoji.CHARTS_DIR, "text-style.html");
        PrintWriter out2 = BagFormatter.openUTF8Writer(Emoji.TR51_OUTPUT_DIR, "text-vs.txt");
        writeHeader(out, "Text vs Emoji", null, "This chart shows the default display style (text vs emoji) by version. "
                + "The 'Dings' include Dingbats, Webdings, and Wingdings. "
                + "The label V1.1 ⊖ Dings indicates those characters (except for Dings) that are in Unicode version 1.1. "
                + "The lable V1.1 ∩ Dings indicates those Ding characters that are in Unicode version 1.1.", "border='1'");
        out.println("<tr><th>Version</th>"
                + "<th width='25%'>Default Text Style; no VS</th>"
                + "<th width='25%'>Default Text Style; has VSs</th>"
                + "<th width='25%'>Default Emoji Style; no VS</th>"
                + "<th width='25%'>Default Emoji Style; has VSs</th>"
                + "</tr>");
        UnicodeSet dings = new UnicodeSet(DINGBATS)
        .addAll(DING_MAP.keySet())
        .retainAll(Emoji.EMOJI_CHARS).freeze();
        for (String version : new TreeSet<String>(VERSION.values())) {
            UnicodeSet current = VERSION.getSet(version).retainAll(Emoji.EMOJI_CHARS);
            if (current.size() == 0) {
                continue;
            }
            UnicodeSet currentDings = new UnicodeSet(current).retainAll(dings);
            current.removeAll(dings);
            String versionString = version.replace("_", ".");
            showTextRow(out, versionString, true, current, defaultText, out2);
            showTextRow(out, versionString, false, currentDings, defaultText, out2);
        }
        writeFooter(out);
        out.close();
        out2.close();
    }

    private static void showTextRow(PrintWriter out, String version, boolean minusDings, UnicodeSet current, UnicodeSet defaultText,
            PrintWriter out2) {
        if (current.size() == 0) {
            return;
        }
        String title = version + (minusDings ? " <span style='color:gray'>⊖ Dings</span>" : " ∩ Dings");
        UnicodeSet emojiSet = new UnicodeSet(current).removeAll(defaultText).removeAll(HAS_EMOJI_VS);
        UnicodeSet emojiSetVs = new UnicodeSet(current).removeAll(defaultText).retainAll(HAS_EMOJI_VS);
        UnicodeSet textSet = new UnicodeSet(current).retainAll(defaultText).removeAll(HAS_EMOJI_VS);
        UnicodeSet textSetVs = new UnicodeSet(current).retainAll(defaultText).retainAll(HAS_EMOJI_VS);
        out.print("<tr><th>" + title + "</th><td>");
        getImages(out, textSet);
        out.print("</td><td>");
        getImages(out, textSetVs);
        out.print("</td><td>");
        getImages(out, emojiSet);
        out.print("</td><td>");
        getImages(out, emojiSetVs);
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

    private static void getImages(PrintWriter out, UnicodeSet textSet) {
        String link = "full-emoji-list.html";
        for (String emoji : textSet.addAllTo(new TreeSet<String>(CODEPOINT_COMPARE))) {
            if (link != null) {
                out.print("<a href='" + link + "#" + Emoji.buildFileName(emoji, "_") + "' target='full'>");
            }
            out.print(getBestImage(emoji, true, "", Emoji.Source.apple));
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
            out.print(getCodeAndName2(UTF16.valueOf(r.codepoint)));
            if (r.codepoint != r.codepointEnd) {
                if (abbreviate) {
                    out.print("<br>\n…" + getCodeAndName2(UTF16.valueOf(r.codepointEnd)));
                } else {
                    for (int cp = r.codepoint + 1; cp <= r.codepointEnd; ++cp) {
                        out.print("<br>\n" + getCodeAndName2(UTF16.valueOf(cp)));
                    }
                }
            }
            out.println("<br>");
        }
        for (String s : us.strings()) {
            out.println(getCodeAndName2(s) + "<br>");
        }
    }

    private static String getCodeAndName2(String s) {
        return "U+" + Utility.hex(s, " U+") + " " + getName(s, false);
    }

    private static void showExplicitAppleImages(PrintWriter out, Set<String> minimal) {
        for (String emoji : minimal) {
            out.println(getBestImage(emoji, false, "", Emoji.Source.apple));
            // out.println("<img height=\"24\" width=\"auto\" alt=\""
            // + emoji
            // + "\" src=\"images/apple/apple_"
            // + Utility.hex(emoji, "_").toLowerCase(Locale.ENGLISH) // emoji
            // + ".png\" title=\"" + getCodeAndName(emoji, " ") + "\"> ");
        }
    }

    private static <U> U ifNull(U keys, U defaultValue) {
        return keys == null ? defaultValue : keys;
    }

    public static void addFileCodepoints(File imagesOutputDir, Map<String, Data> results) {
        for (File file : imagesOutputDir.listFiles()) {
            String fileName = file.getName();
            if (file.isDirectory()) {
                if (!fileName.equals("other") && !fileName.equals("proposed") && !fileName.equals("sample")) {
                    addFileCodepoints(file, results);
                }
                continue;
            }
            String s = fileName;
            String original = s;
            if (s.startsWith(".") || !s.endsWith(".png") || s.contains("emoji-palette")
                    || s.contains("_200d")) { // ZWJ from new combos
                continue;
            }
            String chars = Emoji.parseFileName(true, s);
            if (chars.isEmpty()) { // resulting from _x codepoints
                continue;
            }
            addNewItem(chars, results);
        }
    }

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
        for (String s : Data.DATA_CHARACTERS) {
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
        PrintWriter out = BagFormatter.openUTF8Writer(Emoji.TR51_OUTPUT_DIR, "emoji-labels.html");
        writeHeader(out, "Emoji Labels", null, "Main categories for character picking. " +
                "Characters may occur more than once. " +
                "Categories could be grouped in the UI.", "border='1'");
        for (Entry<Label, Set<String>> entry : Label.CHARS_TO_LABELS.valueKeysSet()) {
            Label label = entry.getKey();
            Set<String> set = entry.getValue();
            String word = label.toString();
            Set<String> values = entry.getValue();
            UnicodeSet uset = new UnicodeSet().addAll(values);

            displayUnicodeset(out, Collections.singleton(word), null, uset, Style.bestImage, 16, null);
        }
        writeFooter(out);
        out.close();
    }

    /** Main Chart */
    private static void showOrdering(Style style) throws IOException {
        PrintWriter out = BagFormatter.openUTF8Writer(Emoji.CHARTS_DIR,
                (style == Style.bestImage ? "" : "ref-") + "emoji-ordering.html");
        writeHeader(out, "Emoji Ordering",
                null, "This chart shows the default ordering of emoji characters from " + CLDR_DATA_LINK + ". "
                        + "This is designed to improve on the <a target='uca' href='http://unicode.org/charts/collation/'>UCA</a> " +
                        "orderings (shown at the right), by grouping similar items together." +
                        "The cell divisions are an artifact, simply to help in review. " +
                        "The left side is an emoji image (* colorful where possible), while the right is black and white.", "border='1'");

        final Set<Entry<String, Set<String>>> keyValuesSet = ORDERING_TO_CHAR.keyValuesSet();
        final int rows = keyValuesSet.size();
        out.println("<tr><th width='49%'>Emoji Ordering</th>"
                + "<th rowSpan='" + (rows + 1) + "'>&nbsp;</th>"
                //                + "<th width='33%'>With Chart Glyphs</th>"
                //                + "<th rowSpan='" + (rows + 1) + "'>&nbsp;</th>"
                + "<th width='49%'>Default Unicode Collation Order</th></tr>");
        UnicodeSet all = new UnicodeSet();
        for (Entry<String, Set<String>> entry : keyValuesSet) {
            all.addAll(entry.getValue());
        }
        boolean first = true;
        final int charsPerRow = -1;
        for (Entry<String, Set<String>> entry : keyValuesSet) {
            out.println("<tr>");
            final UnicodeSet values = new UnicodeSet().addAll(entry.getValue());
            displayUnicodeSet(out, values, Style.bestImage, charsPerRow, 1, 1, null, CODEPOINT_COMPARE);
            // displayUnicodeSet(out, values, Style.refImage, charsPerRow, 1, 1, null, CODEPOINT_COMPARE);
            if (first) {
                first = false;
                displayUnicodeSet(out, all, Style.bestImage, charsPerRow, 1, rows, null, UCA_COLLATOR);
            }
            out.println("</tr>");
        }
        writeFooter(out);
        out.close();
    }

    static final String UTR_LINK = "<em><a target='doc' href='http://www.unicode.org/reports/tr51/index.html'>UTR #51 Unicode Emoji</a></em>";

    /** Main Chart */
    private static void showDefaultStyle() throws IOException {
        PrintWriter out = BagFormatter.openUTF8Writer(Emoji.CHARTS_DIR, "emoji-style.html");
        writeHeader(out, "Emoji Default Style Values", 
                "body {\nfont-family: \"Times New Roman\", \"Segoe UI Emoji\";\n}",
                "This chart provides the default style values for display of emoji characters,"
                        + " and shows the characters that can take variation selectors with the two forms (emoji variant and text variant). "
                        + "These are only <i>default</i> styles; the environment can change the presentation. "
                        + "For example, with a color emoji font or in a chat context, "
                        + "the presentation could be emoji for all characters in <a href='#text_all'>text-all</a>. " 
                        + "</p><p>Unlike the other charts, the emoji are presented as text, to show the style supplied in your browser. "
                        + "The text is given a gray color, so as to help distinguish the emoji presentations. "
                        + "For testing, at the end there are two sets of sequences supported by some platforms: "
                        + "<i>emoji <a href='#modifier'>modifier</a> sequences</i>, and <i>emoji <a href='#zwj'>zwj</a> sequences</i>.", "border='1'");
        for (Entry<Style, Set<String>> entry : STYLE_TO_CHARS.keyValuesSet()) {
            UnicodeSet plain = new UnicodeSet().addAll(entry.getValue());
            final Set<String> singletonWord = Collections.singleton(entry.getKey().toString());
            displayUnicodeset(out, singletonWord, "all", plain, Style.plain, -1, null);
        }
        for (Entry<Style, Set<String>> entry : STYLE_TO_CHARS.keyValuesSet()) {
            Set<String> values = entry.getValue();
            UnicodeSet emoji = new UnicodeSet();
            UnicodeSet text = new UnicodeSet();
            for (String value : values) {
                String emojiStyle = getEmojiVariant(value, Emoji.EMOJI_VARIANT_STRING);
                if (!value.equals(emojiStyle)) {
                    emoji.add(value);
                }
                String textStyle = getEmojiVariant(value, Emoji.TEXT_VARIANT_STRING);
                if (!value.equals(textStyle)) {
                    text.add(value);
                }
            }
            final Set<String> singletonWord = Collections.singleton(entry.getKey().toString());
            displayUnicodeset(out, singletonWord, "with emoji variant", emoji, Style.emoji, -1, null);
            displayUnicodeset(out, singletonWord, "with text variant", text, Style.text, -1, null);
        }
        displayUnicodeset(out, Collections.singleton("modifier"), "", Emoji.APPLE_MODIFIED, Style.plain, -1, null);
        displayUnicodeset(out, Collections.singleton("zwj"), "", Emoji.APPLE_COMBOS, Style.plain, -1, null);
        writeFooter(out);
        out.close();
    }

    /** Main Chart */
    private static void showSequences() throws IOException {
        PrintWriter out = BagFormatter.openUTF8Writer(Emoji.CHARTS_DIR, "emoji-sequences.html");
        writeHeader(out, "Emoji Sequences", 
                "body {\nfont-family: \"Times New Roman\", \"Segoe UI Emoji\";\n}",
                "This chart provides a list of sequences of emoji characters, for checking in browsers. "
                + "These include modifier sequences, joiner sequences, and flags. "
                + "For variation sequences, see <a href='emoji-style.html'>Emoji Default Style Values</a>. "
                + "Unlike the other charts, the emoji are presented as text, to show the style supplied in your browser.", "border='1'");

        UnicodeSet modifiers = new UnicodeSet();
        for (String cp : Emoji.EMOJI_CHARS) {
            ModifierStatus modifier = MODIFIER_STATUS.get(cp);
            if (modifier == ModifierStatus.modifier) {
                modifiers.add(cp);
            }
        }
        modifiers.freeze();

        UnicodeSet primary = new UnicodeSet();
        UnicodeSet secondaryFaces = new UnicodeSet();
        UnicodeSet secondaryNonfaces = new UnicodeSet();

        for (String cp : Emoji.EMOJI_CHARS) {
            ModifierStatus modifier = MODIFIER_STATUS.get(cp);
            if (modifier == ModifierStatus.primary ) {
                for (String s : modifiers) {
                    primary.add(cp+s);
                }
            } else if (modifier == ModifierStatus.secondary) {
                if (Emoji.FACES.contains(cp)) {
                    for (String s : modifiers) {
                        secondaryFaces.add(cp+s);
                    }
                } else {
                    for (String s : modifiers) {
                        secondaryNonfaces.add(cp+s);
                    }
                }
            }
        }
        displayUnicodeset(out, Collections.singleton("primary modifier sequences"), "", primary, Style.plain, -1, null);
        displayUnicodeset(out, Collections.singleton("secondary modifier sequences (non-faces)"), "", secondaryNonfaces, Style.plain, -1, null);
        displayUnicodeset(out, Collections.singleton("secondary modifier sequences (faces)"), "", secondaryFaces, Style.plain, -1, null);
        displayUnicodeset(out, Collections.singleton("joiner sequences"), "", Emoji.APPLE_COMBOS, Style.plain, -1, null);
        displayUnicodeset(out, Collections.singleton("keycaps"), "", Emoji.KEYCAPS, Style.plain, -1, null);
        displayUnicodeset(out, Collections.singleton("flags"), "", Emoji.FLAGS, Style.plain, -1, null);

        writeFooter(out);
        out.close();
    }


    enum CharSource {
        ZDings("ᶻ", "z"),
        ARIB("ª", "a"),
        JCarrier("ʲ", "j"),
        WDings("ʷ", "w"),
        Other("ˣ", "x");
        final String superscript;
        final String letter;

        private CharSource(String shortString, String letter) {
            this.superscript = shortString;
            this.letter = letter;
        }
    }

    static final UnicodeSet ARIB     = new UnicodeSet(
            "[²³¼-¾࿖‼⁉ℓ№℡℻⅐-⅛Ⅰ-Ⅻ↉ ①-⑿⒈-⒓ⒹⓈ⓫⓬▶◀☀-☃☎☓☔☖☗♠ ♣♥♦♨♬⚓⚞⚟⚡⚾⚿⛄-⛿✈❶-❿➡⟐⨀ ⬅-⬇⬛⬤⬮⬯〒〖〗〶㈪-㈳㈶㈷㈹㉄-㉏㉑-㉛ ㊋㊙�㍱㍻-㍾㎏㎐㎝㎞㎠-㎢㎤㎥㏊円年日月 🄀-🄊🄐-🄭🄱🄽🄿🅂🅆🅊-🅏🅗🅟🅹🅻🅼🅿🆊-🆍 🈀🈐-🈰🉀-🉈]")
    .freeze();
    static final UnicodeSet DINGBATS = new UnicodeSet(
            "[\u2194\u2195\u260E\u261B\u261E\u2660\u2663\u2665\u2666\u2701-\u2704\u2706-\u2709\u270C-\u2712\u2714-\u2718\u2733\u2734\u2744\u2747\u2762-\u2767\u27A1]")
    .freeze();

    static class VersionData implements Comparable<VersionData> {
        final Age_Values      versionInfo;
        final Set<CharSource> setCharSource;

        public VersionData(String s) {
            Data data = Data.STRING_TO_DATA.get(s);
            if (data == null) {
                throw new IllegalArgumentException(Utility.hex(s) + " Missing data");
            }
            this.versionInfo = data.age;
            this.setCharSource = Collections.unmodifiableSet(Data.getCharSources(s));
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
            return versionInfo.toString().replace('_', '.');
        }

        public String getCharSources() {
            return CollectionUtilities.join(setCharSource, "+");
        }
    }

    /** Main Chart */
    private static void showVersions() throws IOException {
        PrintWriter out = BagFormatter.openUTF8Writer(Emoji.CHARTS_DIR, "emoji-versions-sources.html");
        writeHeader(
                out,
                "Emoji Versions & Sources",
                null, "This chart shows when each emoji character first appeared in a Unicode version, and which"
                        + "sources the character corresponds to. For example, “ZDings+ARIB+JCarrier” indicates that the character also appears in the Zapf Dingbats, the ARIB set, and the Japanese Carrier set. ", "border='1'");
        UnicodeMap<VersionData> m = new UnicodeMap<>();
        TreeSet<VersionData> sorted = getSortedVersionInfo(m);
        for (VersionData value : sorted) {
            UnicodeSet chars = m.getSet(value);
            displayUnicodeset(out, Collections.singleton(value.getCharSources()), value.getVersion(),
                    Collections.singleton(String.valueOf(chars.size())), chars, Style.bestImage, -1, null);
        }
        writeFooter(out);
        out.close();
    }

    public static TreeSet<VersionData> getSortedVersionInfo(
            UnicodeMap<VersionData> m) {
        for (String s : Emoji.EMOJI_CHARS) {
            m.put(s, new VersionData(s));
        }
        TreeSet<VersionData> sorted = new TreeSet<>(m.values());
        return sorted;
    }

    /** Main Chart */
    private static void showVersionsOnly() throws IOException {
        PrintWriter out = BagFormatter.openUTF8Writer(Emoji.CHARTS_DIR, "emoji-versions.html");
        writeHeader(out, "Emoji Versions", null, "This chart shows when each emoji character first appeared in a Unicode version. "
                + "The emoji characters are displayed with images from a chart font (except for flags).", "border='1'");
        UnicodeMap<Age_Values> m = new UnicodeMap<>();
        for (String s : Emoji.EMOJI_CHARS) {
            Data data = Data.STRING_TO_DATA.get(s);
            m.put(s, data.age);
        }
        TreeSet<Age_Values> sorted = new TreeSet<>(m.values());
        for (Age_Values value : sorted) {
            UnicodeSet chars = m.getSet(value);
            displayUnicodeset(out, Collections.singleton(value.toString().replace("_", ".")), null,
                    Collections.singleton(String.valueOf(chars.size())), chars, Style.refImage, -1, null);
        }
        writeFooter(out);
        out.close();
    }

    private static void showSubhead() throws IOException {
        Map<String, UnicodeSet> subheadToChars = new TreeMap();
        for (String s : Data.DATA_CHARACTERS) {
            int firstCodepoint = s.codePointAt(0);
            String header = Default.ucd().getBlock(firstCodepoint).replace('_', ' ');
            String subhead = subheader.getSubheader(firstCodepoint);
            if (subhead == null) {
                subhead = "UNNAMED";
            }
            header = header.contains(subhead) ? header : header + ": " + subhead;
            UnicodeSet uset = subheadToChars.get(header);
            if (uset == null) {
                subheadToChars.put(header, uset = new UnicodeSet());
            }
            uset.add(s);
        }
        PrintWriter out = BagFormatter.openUTF8Writer(Emoji.CHARTS_DIR, "emoji-subhead.html");
        writeHeader(out, "Emoji Subhead", null, "Unicode Subhead mapping.", "border='1'");
        for (Entry<String, UnicodeSet> entry : subheadToChars.entrySet()) {
            String label = entry.getKey();
            UnicodeSet uset = entry.getValue();
            if (label.equalsIgnoreCase("exclude")) {
                continue;
            }
            displayUnicodeset(out, Collections.singleton(label), null, uset, Style.emoji, 16, null);
        }
        writeFooter(out);
        out.close();
    }

    /** Main charts */
    private static void showAnnotations(String dir, String filename, UnicodeSet filterOut, Set<String> retainAnnotations, boolean removeInsteadOf)
            throws IOException {
        try (PrintWriter out = BagFormatter.openUTF8Writer(dir, filename)) {
            writeHeader(out, "Emoji Annotations",
                    null, "This chart shows the English emoji character annotations based on " + CLDR_DATA_LINK + ".", "border='1'");

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
            Relation<Set<String>, String> setOfCharsToKeys = GenerateEmoji.ANNOTATIONS_TO_CHARS.getValuesToKeys();

            for (Entry<String, Set<String>> entry : GenerateEmoji.ANNOTATIONS_TO_CHARS.keyValuesSet()) {
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
                    displayUnicodeset(out, words, null, filtered, Style.bestImage, -1, "full-emoji-list.html");
                }
            }
            writeFooter(out);
        }
    }

    private static void showAnnotationsBySize(String dir, String filename, UnicodeSet retainSet) throws IOException {
        PrintWriter out = BagFormatter.openUTF8Writer(dir, filename);
        writeHeader(out, "Emoji Annotations", null, "Finer-grained character annotations. ", "border='1'");
        TreeSet<Row.R3<Integer, UnicodeSet, String>> sorted = new TreeSet<>();
        Relation<UnicodeSet, String> usToAnnotations = Relation.of(new HashMap(), TreeSet.class, UCA_COLLATOR);
        for (Entry<String, Set<String>> entry : GenerateEmoji.ANNOTATIONS_TO_CHARS.keyValuesSet()) {
            String word = entry.getKey();
            if (GROUP_ANNOTATIONS.contains(word)) {
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
            displayUnicodeset(out, allWords, null, uset, Style.bestImage, 16, "full-emoji-list.html");
            seenAlready.addAll(allWords);
        }
        writeFooter(out);
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
        UnicodeSet symbolMath = LATEST.load(UcdProperty.Math).getSet(Binary.Yes.toString());
        UnicodeSet symbolMathAlphanum = new UnicodeSet()
        .addAll(LATEST.load(UcdProperty.Alphabetic).getSet(Binary.Yes.toString()))
        .addAll(GENERAL_CATEGORY.getSet(General_Category_Values.Decimal_Number.toString()))
        .addAll(GENERAL_CATEGORY.getSet(General_Category_Values.Letter_Number.toString()))
        .addAll(GENERAL_CATEGORY.getSet(General_Category_Values.Other_Number.toString()))
        .retainAll(symbolMath);
        symbolMath.removeAll(symbolMathAlphanum);
        addSet(labelToUnicodeSet, "Symbol-Math", symbolMath);
        addSet(labelToUnicodeSet, "Symbol-Math-Alphanum", symbolMathAlphanum);
        addSet(labelToUnicodeSet, "Symbol-Braille",
                LATEST.load(UcdProperty.Block).getSet(Block_Values.Braille_Patterns.toString()));
        addSet(labelToUnicodeSet, "Symbol-APL", new UnicodeSet("[⌶-⍺ ⎕]"));

        UnicodeSet otherSymbols = new UnicodeSet()
        .addAll(GENERAL_CATEGORY.getSet(General_Category_Values.Math_Symbol.toString()))
        .addAll(GENERAL_CATEGORY.getSet(General_Category_Values.Other_Symbol.toString()))
        .removeAll(NFKCQC.getSet(Binary.No.toString()))
        .removeAll(Data.DATA_CHARACTERS)
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
        .removeAll(Data.DATA_CHARACTERS)
        .retainAll(COMMON_SCRIPT);
        ;

        for (Entry<String, UnicodeSet> entry : labelToUnicodeSet.entrySet()) {
            UnicodeSet uset = entry.getValue();
            uset.removeAll(Emoji.EMOJI_CHARS);
            otherSymbols.removeAll(uset);
            otherPunctuation.removeAll(uset);
        }
        if (!otherPunctuation.isEmpty()) {
            addSet(labelToUnicodeSet, "Punctuation-Other", otherPunctuation);
        }
        if (!otherSymbols.isEmpty()) {
            addSet(labelToUnicodeSet, "Symbol-Other", otherSymbols);
        }

        PrintWriter out = BagFormatter.openUTF8Writer(Emoji.TR51_OUTPUT_DIR, "other-labels.html");
        writeHeader(out, "Other Labels", null, "Draft categories for other Symbols and Punctuation.", "border='1'");

        for (Entry<String, UnicodeSet> entry : labelToUnicodeSet.entrySet()) {
            String label = entry.getKey();
            UnicodeSet uset = entry.getValue();
            if (label.equalsIgnoreCase("exclude")) {
                continue;
            }
            displayUnicodeset(out, Collections.singleton(label), null, uset, Style.plain, 16, "");
        }

        writeFooter(out);
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

    public static <T extends Object> void displayUnicodeset(PrintWriter out, Set<T> labels, String sublabel,
            UnicodeSet uset, Style showEmoji, int maxPerLine, String link) {
        displayUnicodeset(out, labels, sublabel, Collections.<String> emptySet(), uset, showEmoji, maxPerLine, link);
    }

    public static <T extends Object> void displayUnicodeset(PrintWriter out, Set<T> labels, String sublabel, 
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
        displayUnicodeSet(out, uset, showEmoji, maxPerLine, 1, 1, link, MULTI);
        out.println("</tr>");
    }

    public static void displayUnicodeSet(PrintWriter out,
            UnicodeSet uset, Style showEmoji, int maxPerLine, int colSpan, int rowSpan,
            String link, Comparator comparator) {
        Set<String> sorted = uset.addAllTo(new TreeSet<String>(comparator));
        displayUnicodeSet(out, sorted, showEmoji, maxPerLine, colSpan, rowSpan, link, "");
    }

    static final String         FULL_LINK          = "<a href='full-emoji-list.html' target='full'>Full Emoji List</a>";

    private static final String HOVER_INSTRUCTIONS = "Hovering over an emoji shows the name; clicking goes to the " + FULL_LINK + " entry for that emoji.";

    public static void displayUnicodeSet(PrintWriter out,
            Collection<String> sorted, Style showEmoji, int maxPerLine, int colSpan, int rowSpan,
            String link, String extraClasses) {
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
            if (cell == null) {
                switch (showEmoji) {
                case text:
                case emoji:
                    cell = getEmojiVariant(s, showEmoji == Style.emoji ? Emoji.EMOJI_VARIANT_STRING : Emoji.TEXT_VARIANT_STRING);
                    break;
                case plain:
                    cell = s;
                    break;
                case bestImage:
                    cell = getBestImage(s, true, extraClasses);
                    gotTitle = true;
                    break;
                case refImage:
                    cell = getImage(Emoji.Source.ref, s, true, extraClasses);
                    gotTitle = true;
                    break;
                }
            }
            if (link != null) {
                cell = "<a class='plain' href='" + link + "#" + Emoji.buildFileName(s, "_") + "' target='full'>" + cell + "</a>";
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
                getHex(s) + " " + getName(s, true) + "'>"
                + cell
                + "</span>";
    }

    public static String getName(String s, boolean tolower) {
        String flag = getFlagRegionName(s);
        if (flag != null) {
            String result = LOCALE_DISPLAY.regionDisplayName(flag);
            if (result.endsWith(" SAR China")) {
                result = result.substring(0, result.length() - " SAR China".length());
            }
            return "flag for " + result;
        }
        final int firstCodePoint = s.codePointAt(0);
        String name = NAME.get(firstCodePoint);
        if (s.indexOf(Emoji.ENCLOSING_KEYCAP) >= 0) {
            return "keycap " + (tolower ? name.toLowerCase(Locale.ENGLISH) : name);
        }
        if (s.length() > Character.charCount(firstCodePoint)) {
            StringBuffer nameBuffer = new StringBuffer();
            for (int cp : CharSequences.codePoints(s)) {
                if (nameBuffer.length() != 0) {
                    nameBuffer.append(", ");
                }
                nameBuffer.append(cp == Emoji.JOINER ? "zwj" 
                        : cp == Emoji.EMOJI_VARIANT ? "emoji-vs" 
                                : NAME.get(cp));
            }
            name = nameBuffer.toString();
        }
        return name == null ? "UNNAMED" : (tolower ? name.toLowerCase(Locale.ENGLISH) : name);
    }

    public static String getFlagRegionName(String s) {
        String result = getFlagCode(s);
        if (result != null) {
            result = LOCALE_DISPLAY.regionDisplayName(result);
            if (result.endsWith(" SAR China")) {
                result = result.substring(0, result.length() - " SAR China".length());
            } else if (result.contains("(")) {
                result = result.substring(0, result.indexOf('(')) + result.substring(result.lastIndexOf(')') + 1);
            }
            result = result.replaceAll("\\s\\s+", " ").trim();
        }
        return result;
    }

    public static String getHex(String theChars) {
        return "U+" + Utility.hex(theChars, " U+");
    }

    public static String getCodeAndName(String chars1, String separator) {
        return getHex(chars1) + separator + chars1 + separator + getName(chars1, true);
    }

    public static void addNewItem(String s, Map<String, Data> missingMap) {
        if (!Data.DATA_CHARACTERS.contains(s)) {
            addNewItem(new Data(s), missingMap);
        }
    }

    public static void addNewItem(Data item, Map<String, Data> missingMap) {
        if (item == null || !Emoji.EMOJI_CHARS.contains(item.chars)) {
            return;
        }
        if (missingMap.containsKey(item.chars)) {
            throw new IllegalArgumentException(item.toString());
        }
        missingMap.put(item.chars, item);
        Data.missingJSource.remove(item.chars);
    }

    private static final String CLDR_DATA_LINK = "<a target='cldr' href='http://cldr.unicode.org/#TOC-What-is-CLDR-'>Unicode CLDR data</a>";

    enum Form {
        shortForm("short", " short form"),
        noImages("", " with single image"),
        fullForm("full", "with images from different vendors"),
        extraForm("extra", " with images; have icons but are not including");
        final String filePrefix;
        final String title;
        final String description;

        Form(String prefix, String description) {
            this.description = description
                    + ", version and source information, default style, and annotations. "
                    + "The ordering of the emoji and the annotations are based on "
                    + CLDR_DATA_LINK + ".";
            filePrefix = prefix.isEmpty() ? "" : prefix + "-";
            title = (prefix.isEmpty() ? "" : UCharacter.toTitleCase(prefix, null) + " ")
                    + "Emoji Data";
        }
    }

    /** Main charts */
    public static <T> void print(Form form, Map<String, Data> set, Stats stats) throws IOException {
        PrintWriter out = BagFormatter.openUTF8Writer(Emoji.CHARTS_DIR,
                form.filePrefix + "emoji-list.html");
        PrintWriter outText = null;
        PrintWriter outText2 = null;
        int order = 0;
        UnicodeSet level1 = null;
        writeHeader(out, form.title, null, "This chart provides a list of the Unicode emoji characters, " + form.description, "border='1'");
        out.println(Data.toHtmlHeaderString(form));
        int item = 0;
        for (Data data : new TreeSet<Data>(set.values())) {
            out.println(data.toHtmlString(form, ++item, stats));
            if (outText != null) {
                outText.println(data.toSemiString(order++, null));
                outText2.println(data.toSemiString(order++, level1));
            }
        }
        writeFooter(out);
        out.close();
    }

    public static <T> void printData(Map<String, Data> set, Stats stats) throws IOException {
        // PrintWriter outText = null;
        PrintWriter outText2 = null;
        int order = 0;
        UnicodeSet level1 = null;
        outText2 = BagFormatter.openUTF8Writer(Emoji.DATA_DIR, "emoji-data.txt");
        // String format =
        // "# Code ; Default Style ; Sources ; Version # (Character) Name\n";
        // outText.println(dataHeader("", "", ""));
        outText2.println(dataHeader());
        // level1 = new UnicodeSet()
        // .addAll(stats.totalData.get(Source.apple))
        // .retainAll(stats.totalData.get(Source.android))
        // .retainAll(stats.totalData.get(Source.windows))
        // .retainAll(stats.totalData.get(Source.twitter))
        // .freeze();
        level1 = new UnicodeSet(LEVEL1);
        for (String cp : Emoji.EMOJI_CHARS) {
            Data data = Data.STRING_TO_DATA.get(cp);
            // final TreeSet<Data> sorted = new TreeSet<Data>(set.values());
            // outText.println(data.toSemiString(order++, null));
            outText2.println(data.toSemiString(order++, level1));
        }
        // outText.close();
        outText2.close();
    }

    private static String dataHeader() {
        return "# Emoji Data for UTR #51\n"
                + "#\n"
                + "# File:    emoji-data.txt\n"
                + "# Version: 1.0\n"
                + "# Date:    " + getDate() + "\n"
                + "#\n"
                + "# Copyright (c) 2015 Unicode, Inc.\n"
                + "# For terms of use, see http://www.unicode.org/terms_of_use.html\n"
                + "# For documentation and usage, see http://www.unicode.org/reports/tr51/\n"
                + "#\n"
                + "# Format: Code ; Default_Emoji_Style ; Emoji_Level ; Emoji_Modifier_Status ; Emoji_Sources # Comment\n"
                + "#\n"
                + "#   Field 1 — Default_Emoji_Style:\n"
                + "#             text:      default text presentation\n"
                + "#             emoji:     default emoji presentation\n"
                + "#   Field 2 — Emoji_Level:\n"
                + "#             L1:        level 1 emoji\n"
                + "#             L2:        level 2 emoji\n"
                + "#             NA:        not applicable\n"
                + "#   Field 3 — Emoji_Modifier_Status:\n"
                + "#             modifier:  an emoji modifier\n"
                + "#             primary:   a primary emoji modifier base\n"
                + "#             secondary: a secondary emoji modifier base\n"
                + "#             none:      not applicable\n"
                + "#   Field 4 — Emoji_Sources:\n"
                + "#             one or more values from {z, a, j, w, x}\n"
                + "#             see the key in http://unicode.org/reports/tr51#Major_Sources\n"
                + "#             NA:        not applicable\n"
                + "#   Comment — currently contains the version where the character was first encoded,\n"
                + "#             followed by:\n"
                + "#             - a character name in uppercase (for a single character),\n"
                + "#             - a keycap name,\n"
                + "#             - an associated flag, where is associated with value unicode region code\n"
                + "#";
    }

    private static String getDate() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd", ULocale.ENGLISH);
        df.setTimeZone(TimeZone.getFrozenTimeZone("GMT"));
        return df.format(new Date());
    }

    static final String FOOTER = "</table>" + Utility.repeat("<br>", 60) + "</body></html>";

    public static void writeFooter(PrintWriter out) {
        out.println(FOOTER);
    }

    public static void writeHeader(PrintWriter out, String title, String styles, String firstLine, String tableAttrs) {
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
                + "<h1>" + DRAFT_TITLE_PREFIX + title + "</h1>\n"
                + "<p><b>" + chartIndex + "</b></p>"
                + "<p>" + firstLine + "</p>\n"
                + "<p>For details about the format and fields, see " +
                chartIndex + " and " + UTR_LINK
                + ". " + HOVER_INSTRUCTIONS + "</p>\n"
                + "<table " + tableAttrs + ">");
    }

    static boolean CHECKFACE = false;

    static void oldAnnotationDiff() throws IOException {
        PrintWriter out = BagFormatter.openUTF8Writer(Emoji.CHARTS_DIR, "emoji-diff.html");
        writeHeader(out, "Diff List", null, "Differences from other categories.", "border='1'");

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

        writeFooter(out);
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
                    "<td>" + "⊉" + "</td>\n" +
                    "<td>" + title2 + "</td>\n" +
                    "<td>" + missing.size() + "/" + containee.size() + "</td>\n" +
                    "<td class='lchars'>");
            boolean first = true;
            Set<String> sorted = new TreeSet<String>(CODEPOINT_COMPARE);
            missing.addAllTo(sorted);
            for (String s : sorted) {
                if (first) {
                    first = false;
                } else {
                    out.print("\n");
                }
                out.print("<span title='" + Default.ucd().getName(s) + "'>"
                        + getEmojiVariant(s, Emoji.EMOJI_VARIANT_STRING)
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
                final File file = new File(filename.startsWith("samsung") 
                        ? "/Users/markdavis/Google Drive/workspace/DATA/emoji/" 
                                : Emoji.IMAGES_OUTPUT_DIR, filename);
                if (!file.exists()) {
                    result = "";
                } else if (!DATAURL) {
                    result = "images/" + filename;
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

    static void printCollationOrder() throws IOException {
        try (
                PrintWriter outText = BagFormatter.openUTF8Writer(Emoji.TR51_OUTPUT_DIR, "emoji-ordering-list.txt")) {
            for (String s : SORTED_EMOJI_CHARS_SET) {
                outText.println(getCodeAndName2(s));
            }
        }
        try (
                PrintWriter outText = BagFormatter.openUTF8Writer(Emoji.TR51_OUTPUT_DIR, "emoji-ordering.txt")) {
            outText.append("<!-- DRAFT emoji-ordering.txt\n"
                    + "\tFor details about the format and other information, see " + DOC_DATA_FILES + ".\n"
                    + "\thttp://unicode.org/cldr/trac/ticket/7270 -->\n"
                    + "<collation type='emoji'>\n"
                    + "<cr><![CDATA[\n"
                    + "# START AUTOGENERATED EMOJI ORDER\n");
            appendCollationRules(outText);
            outText.write("\n]]></cr>\n</collation>");
        }
    }

    private static <T extends Appendable> T appendCollationRules(T outText) {
        try {
            boolean needRelation = true;
            boolean haveFlags = false;
            boolean isFirst = true;
            for (String s : SORTED_EMOJI_CHARS_SET) {
                boolean multiCodePoint = s.codePointCount(0, s.length()) > 1;
                if (isFirst) {
                    if (multiCodePoint) {
                        throw new IllegalArgumentException("Cannot have first item with > 1 codepoint: " + s);
                    }
                    outText.append("&").append(s);
                    isFirst = false;
                    continue;
                }
                if (multiCodePoint) { // flags and keycaps
                    if (Emoji.isRegionalIndicator(s.codePointAt(0))) {
                        if (!haveFlags) {
                            // put all the 26 regional indicators in order at
                            // this point
                            StringBuilder b = new StringBuilder("\n<*");
                            for (int i = Emoji.FIRST_REGIONAL; i <= Emoji.LAST_REGIONAL; ++i) {
                                b.appendCodePoint(i);
                            }
                            outText.append(b);
                            haveFlags = true;
                        }
                        continue;
                    }
                    // keycaps, can't use <* syntax
                    String quoted = s.contains("*") || s.contains("#")? "'" + s + "'" : s;
                    outText.append("\n<").append(quoted);
                    needRelation = true;
                } else {
                    if (needRelation) {
                        outText.append("\n<*");
                    }
                    outText.append(s);
                    // break arbitrarily (but predictably)
                    int bottomBits = s.codePointAt(0) & 0xF;
                    needRelation = bottomBits == 0;
                }
            }
        } catch (IOException e) {
            throw new ICUUncheckedIOException("Internal Error",e);
        }
        return outText;
    }

    static final String      ANNOTATION_HEADER = "<?xml version='1.0' encoding='UTF-8' ?>\n"
            + "<!DOCTYPE ldml SYSTEM '../../common/dtd/ldml.dtd'>\n"
            + "<!-- Copyright © 1991-2013 Unicode, Inc. DRAFT emoji-annotations.txt For \n"
            + " details about the format and other information, see /../../../reports/tr51/index.html#Data_Files. \n"
            + " http://unicode.org/cldr/trac/ticket/8019 CLDR data files are interpreted \n"
            + " according to the LDML specification (http://unicode.org/reports/tr35/) For \n"
            + " terms of use, see http://www.unicode.org/copyright.html \n"
            // +
            // " This is still under development, and will be refined before release. \n"
            // +
            // " In particular, the annotations like 'person-apple' are only present during development, and will be withdrawn for the release.\n"
            + " -->\n"
            + "<ldml>\n"
            + "\t<identity>\n"
            + "\t\t<version number='$Revision: 10585 $' />\n"
            + "\t\t<generation date='$Date: 2014-06-19 06:23:55 +0200 (Thu, 19 Jun 2014) $' />\n";

    static final Set<String> GROUP_ANNOTATIONS = new HashSet<>(Arrays.asList(
            "default-text-style",
            "fitz-primary",
            "fitz-secondary",
            "nature",
            "nature-android",
            "nature-apple",
            "object",
            "object-android",
            "object-apple",
            "person",
            "person-android",
            "person-apple",
            "place",
            "place-android",
            "place-apple",
            "symbol",
            "symbol-android",
            "symbol-apple",
            "other-android",
            "flag",
            "other"));

    private static void printAnnotations() throws IOException {
        try (
                PrintWriter outText = BagFormatter.openUTF8Writer(Emoji.TR51_OUTPUT_DIR, "emoji-annotations.xml")) {
            outText.append(ANNOTATION_HEADER
                    + "\t\t<language type='en'/>\n"
                    + "\t</identity>\n"
                    + "\t<annotations>\n");
            Set<Row.R2<Set<String>, UnicodeSet>> sorted = new TreeSet<>(PAIR_SORT);
            for (String s : Emoji.EMOJI_CHARS) {
                Set<String> annotations = new LinkedHashSet<>(ANNOTATIONS_TO_CHARS.getKeys(s));
                annotations.removeAll(GROUP_ANNOTATIONS);
                if (annotations.isEmpty()) {
                    throw new IllegalArgumentException("Missing annotation: " + s
                            + "\t" + ANNOTATIONS_TO_CHARS.getKeys(s));
                }
            }
            for (Entry<Set<String>, Set<String>> s : ANNOTATIONS_TO_CHARS.getValuesToKeys().keyValuesSet()) {
                UnicodeSet chars = new UnicodeSet().addAll(s.getKey());
                Set<String> annotations = new LinkedHashSet<>(s.getValue());
                annotations.removeAll(GROUP_ANNOTATIONS);
                if (annotations.isEmpty()) {
                    continue;
                }
                sorted.add(Row.of(annotations, chars));
            }
            for (R2<Set<String>, UnicodeSet> s : sorted) {
                String annotations = CollectionUtilities.join(s.get0(), "; ");
                UnicodeSet chars = s.get1();
                outText.append("\t\t<annotation cp='")
                .append(chars.toPattern(false))
                .append("' draft='provisional'>")
                .append(annotations)
                .append("</annotation>\n");
            }
            outText.write("\t</annotations>\n"
                    + "</ldml>");
        }
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
                    (Comparator<String>) UCA_COLLATOR);
            if (diff != 0) {
                return diff;
            }
            return o1.get1().compareTo(o2.get1());
        }
    };
}
