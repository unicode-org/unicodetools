package org.unicode.text.tools;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
import org.unicode.cldr.util.Pair;
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
import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.dev.util.TransliteratorUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.MultiComparator;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.LocaleDisplayNames;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.EntryRange;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;

public class GenerateEmoji {
    private static boolean                SHOW                      = false;

    private static final boolean          DATAURL                   = true;
    private static final int              RESIZE_IMAGE              = -1;

    private static final String           BREAK                     = "<br>";
    private static final String           DOC_DATA_FILES            = "/../../../reports/tr51/index.html#Data_Files";

    // private static final UnicodeSet EXTRAS = new UnicodeSet(
    // "[☦ ☪-☬ ☸ ✝ 🕉0-9\\u2714\\u2716\\u303D\\u3030 \\u00A9 \\u00AE \\u2795-\\u2797 \\u27B0 \\U0001F519-\\U0001F51C {🇽🇰}]")
    // .add("*"+Emoji.ENCLOSING_KEYCAP)
    // .freeze();
    static final Set<String>              SKIP_WORDS                = new HashSet<String>(Arrays.asList("with", "a", "in", "without", "and", "white", "symbol",
            "sign", "for", "of", "black"));

    static final IndexUnicodeProperties   LATEST                    = IndexUnicodeProperties.make(Default.ucdVersion());
    static final UCA                      UCA_COLLATOR              = UCA.buildCollator(null);

    static final UnicodeMap<String>       STANDARDIZED_VARIANT      = LATEST.load(UcdProperty.Standardized_Variant);
    static final UnicodeMap<String>       VERSION                   = LATEST.load(UcdProperty.Age);
    static final UnicodeMap<String>       WHITESPACE                = LATEST.load(UcdProperty.White_Space);
    static final UnicodeMap<String>       GENERAL_CATEGORY          = LATEST.load(UcdProperty.General_Category);
    static final UnicodeMap<String>       SCRIPT_EXTENSIONS         = LATEST.load(UcdProperty.Script_Extensions);
    private static final UnicodeSet       COMMON_SCRIPT             = new UnicodeSet()
    .addAll(SCRIPT_EXTENSIONS.getSet(UcdPropertyValues.Script_Values.Common.toString()))
    .freeze();

    static final UnicodeMap<String>       NFKCQC                    = LATEST.load(UcdProperty.NFKD_Quick_Check);
    static final UnicodeMap<String>       NAME                      = LATEST.load(UcdProperty.Name);
    static final UnicodeSet               JSOURCES                  = new UnicodeSet();
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
        if (SHOW)
            System.out.println("Core:\t" + JSOURCES.size() + "\t" + JSOURCES);
    }
    static final LocaleDisplayNames       LOCALE_DISPLAY            = LocaleDisplayNames.getInstance(ULocale.ENGLISH);

    static final Pattern                  tab                       = Pattern.compile("\t");
    static final Pattern                  space                     = Pattern.compile(" ");
    static final String                   REPLACEMENT_CHARACTER     = "\uFFFD";

    static final MapComparator<String>    mp                        = new MapComparator<String>().setErrorOnMissing(false);

    static final Relation<String, String> ORDERING_TO_CHAR          = new Relation(new LinkedHashMap(), LinkedHashSet.class);
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

    static final Comparator               CODEPOINT_COMPARE         =
            new MultiComparator<String>(
                    mp,
                    UCA_COLLATOR, // don't
                    // need
                    // cldr
                    // features
                    new UTF16.StringComparator(true, false, 0));

    static final Comparator               CODEPOINT_COMPARE_SHORTER =
            new MultiComparator<String>(
                    Emoji.CODEPOINT_LENGTH,
                    mp,
                    UCA_COLLATOR, // don't
                    // need
                    // cldr
                    // features
                    new UTF16.StringComparator(true, false, 0));

    static final Set<String>              SORTED_EMOJI_CHARS_SET;
    static {
        TreeSet<String> temp = new TreeSet<String>(CODEPOINT_COMPARE);
        Emoji.EMOJI_CHARS.addAllTo(temp);
        SORTED_EMOJI_CHARS_SET = Collections.unmodifiableSortedSet(temp);
    }

    static final EmojiAnnotations         ANNOTATIONS_TO_CHARS      = new EmojiAnnotations(CODEPOINT_COMPARE, "emojiAnnotations.txt");
    static final UnicodeSet DEFAULT_TEXT_STYLE = new UnicodeSet()
    .addAll(ANNOTATIONS_TO_CHARS.keyToValues.get("default-text-style")).freeze();

    //    static final EmojiAnnotations         ANNOTATIONS_TO_CHARS_NEW  = new EmojiAnnotations(CODEPOINT_COMPARE, "emojiAnnotationsNew.txt");

    static final Subheader                subheader                 = new Subheader("/Users/markdavis/workspace/unicodetools/data/ucd/7.0.0-Update/");
    static final Set<String>              SKIP_BLOCKS               = new HashSet(Arrays.asList("Miscellaneous Symbols",
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
                new TreeMap(CODEPOINT_COMPARE),
                new EnumMap(Label.class),
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
    private static final String        MISSING_CELL    = "<td class='miss'>missing</td>\n";
    private static final String        MISSING7_CELL   = "<td class='miss7'>new 7.0</td>\n";

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

        static final UnicodeSet EMOJI_STYLE_OVERRIDE = new UnicodeSet("[🔙 🔚 🔛 🔜 🔝➕ ➖ ➗ ➰ ➿]").freeze();
        private static final Set<String> SUPPRESS_ANNOTATIONS = new HashSet<>(Arrays.asList("default-text-style"));

        public Data(String chars, String code, String age,
                String defaultPresentation, String name) {
            this.chars = chars;
            if (chars.contains(Emoji.EMOJI_VARIANT_STRING) || chars.equals(Emoji.TEXT_VARIANT_STRING)) {
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
            String missingCell = VERSION70.containsSome(chars) ? MISSING7_CELL : MISSING_CELL;
            String core = Emoji.buildFileName(chars, "_");
            String symbolaCell = getCell(Source.ref, core, missingCell);
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

            String appleCell = getCell(Source.apple, core, missingCell);
            String androidCell = getCell(Source.android, core, missingCell);
            String twitterCell = getCell(Source.twitter, core, missingCell);
            String windowsCell = getCell(Source.windows, core, missingCell);
            String gmailCell = getCell(Source.gmail, core, missingCell);
            String sbCell = getCell(Source.sb, core, missingCell);
            String dcmCell = getCell(Source.dcm, core, missingCell);
            String kddiCell = getCell(Source.kddi, core, missingCell);
            if (stats != null) {
                stats.add(chars, Source.apple, appleCell.equals(missingCell));
                stats.add(chars, Source.android, androidCell.equals(missingCell));
                stats.add(chars, Source.twitter, twitterCell.equals(missingCell));
                stats.add(chars, Source.windows, windowsCell.equals(missingCell));
                stats.add(chars, Source.gmail, gmailCell.equals(missingCell));
                stats.add(chars, Source.dcm, dcmCell.equals(missingCell));
                stats.add(chars, Source.kddi, kddiCell.equals(missingCell));
                stats.add(chars, Source.sb, sbCell.equals(missingCell));
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

        enum DataStyle {plain, cldr}

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
                    + " ;\t" + getVersion()
                    + "\t# (" + chars
                    + ") " + getName(chars, true);
            //            Set<String> annotations = new LinkedHashSet<>(ifNull(GenerateEmoji.ANNOTATIONS_TO_CHARS.getKeys(chars), Collections.EMPTY_SET));
            //            annotations.removeAll(SUPPRESS_ANNOTATIONS);
            // if (annotations != null) {
            // annotations = new LinkedHashSet(annotations);
            // for (Label label : labels) {
            // annotations.remove(label.toString());
            // }
            // }
            //            String flagRegion = getFlagRegionName(chars);
            //            if (flagRegion != null) {
            //                annotations.add(flagRegion);
            //            }
            //            if (annotations.isEmpty()) {
            //                throw new IllegalArgumentException("No annotations for:\t" + getName(chars, true) + "\t" + chars);
            //            }
            //            return Utility.hex(chars, " ")
            //                    + " ;\t" + order
            //                    + " ;\t" + CollectionUtilities.join(annotations, ", ")
            //                    + " \t# " + getVersion()
            //                    + " (" + chars
            //                    + ") " + getName(chars, true);
        }

        public String getCell(Source type, String core, String missingCell) {
            String filename = getImageFilename(type, core);
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

    enum ModifierStatus {none, modifier, minimal, optional}

    static final UnicodeMap<ModifierStatus> MODIFIER_STATUS = new UnicodeMap<ModifierStatus>()
            .putAll(Emoji.EMOJI_CHARS, ModifierStatus.none)
            .putAll(new UnicodeSet("[\\x{1F3FB}\\x{1F3FC}\\x{1F3Fd}\\x{1F3Fe}\\x{1F3Ff}]"), ModifierStatus.none)
            .putAll(new UnicodeSet("[\\x{1F3FB}\\x{1F3FC}\\x{1F3Fd}\\x{1F3Fe}\\x{1F3Ff}]"), ModifierStatus.modifier)
            .putAll(new UnicodeSet().addAll(ANNOTATIONS_TO_CHARS.getValues("fitz-minimal")), ModifierStatus.minimal)
            .putAll(new UnicodeSet().addAll(ANNOTATIONS_TO_CHARS.getValues("fitz-optional")), ModifierStatus.optional)
            .freeze();

    public static String getImageFilename(Source type, String core) {
        // if (type == Source.gmail) {
        // return GmailEmoji.getURL(core);
        // }
        String suffix = ".png";
        if (type != null && type.isGif()) {
            suffix = ".gif";
        }
        return type + "/" + type + "_" + core + suffix;
    }

    static final UnicodeMap<Source> BEST_OVERRIDE = new UnicodeMap<>();
    static {
        BEST_OVERRIDE.putAll(new UnicodeSet("[🕐-🕧🚶🏃💃👪👫👬👭🙍🙎🙅🙆🙇🙋🙌🙏💮]"), Source.android);
        BEST_OVERRIDE.putAll(new UnicodeSet("[✊-✌ 💅💪👂👃👯" +
                "👦 👰 👧  👨  👩  👮  👱  👲  👳 👴  👵  👶  👷  👸  💁  💂 👼" +
                "👈👉☝👆👇👊  👋  👌  👍👎 👏  👐]"), Source.twitter);
        BEST_OVERRIDE.putAll(new UnicodeSet("[💆👯💏💑💇]"), Source.windows);
        BEST_OVERRIDE.putAll(new UnicodeSet("[💇]"), Source.ref);
        BEST_OVERRIDE.freeze();
    }

    public static String getBestImage(String s, boolean useDataURL, Source... doFirst) {
        if (doFirst.length == 0) {
            Source source0 = BEST_OVERRIDE.get(s);
            if (source0 != null) {
                doFirst = new Source[]{source0};
            }
        }
        for (Source source : orderedEnum(doFirst)) {
            String cell = getImage(source, s, useDataURL);
            if (cell != null) {
                return cell;
            }
        }
        throw new IllegalArgumentException("Can't find image for: " + Utility.hex(s) + " " + getName(s, true) + "\t" + Emoji.buildFileName(s, "_"));
    }

    static public String getImage(Source type, String chars, boolean useDataUrl) {
        String core = Emoji.buildFileName(chars, "_");
        String filename = getImageFilename(type, core);
        if (filename != null && new File(Emoji.IMAGES_OUTPUT_DIR, filename).exists()) {
            String className = type.getClassAttribute(chars);
            // String className = "imga";
            // if (type == Source.ref && getFlagCode(chars) != null) {
            // className = "imgf";
            // }
            return "<img alt='" + chars + "'" +
            (useDataUrl ? " class='" + className + "'" : " height=\"24\" width=\"auto\"") +
            " src='" + (useDataUrl ? getDataUrl(filename) : "images/" + filename) + "'" +
            " title='" + getCodeAndName(chars, " ") + "'" +
            ">";
        }
        return null;
    }

    public static File getBestFile(String s, Source... doFirst) {
        for (Source source : orderedEnum(doFirst)) {
            File file = getImageFile(source, s);
            if (file != null) {
                return file;
            }
        }
        return null;
    }

    public static Iterable<Source> orderedEnum(Source... doFirst) {
        if (doFirst.length == 0) {
            return Arrays.asList(Source.values());
        }
        LinkedHashSet<Source> ordered = new LinkedHashSet<>(Arrays.asList(doFirst));
        ordered.addAll(Arrays.asList(Source.values()));
        return ordered;
    }

    static public File getImageFile(Source type, String chars) {
        String core = Emoji.buildFileName(chars, "_");
        String filename = getImageFilename(type, core);
        if (filename != null) {
            File file = new File(Emoji.IMAGES_OUTPUT_DIR, filename);
            if (file.exists()) {
                return file;
            }
        }
        return null;
    }

    enum Source {
        apple, android, twitter, windows, ref, gmail, sb, dcm, kddi;
        boolean isGif() {
            return compareTo(Source.gmail) >= 0;
        }

        String getClassAttribute(String chars) {
            if (isGif()) {
                return "imgs";
            }
            String className = "imga";
            if (this == Source.ref && getFlagCode(chars) != null) {
                className = "imgf";
            }
            return className;
        }
    }

    private static class Stats {
        enum Type {
            countries, misc, v70
        } // cards, dominos, majong,

        // static final UnicodeSet DOMINOS = new UnicodeSet("[🀰-🂓]");
        // static final UnicodeSet CARDS = new UnicodeSet("[🂠-🃵]");
        // static final UnicodeSet MAHJONG = new UnicodeSet("[🀀-🀫]");
        final EnumMap<Type, EnumMap<Source, UnicodeSet>> data      = new EnumMap<>(Type.class);
        final EnumMap<Source, UnicodeSet>                totalData = new EnumMap<>(Source.class);
        {
            for (Source s : Source.values()) {
                totalData.put(s, new UnicodeSet());
            }
        }

        public void add(
                String chars,
                Source source,
                boolean isMissing) {
            if (isMissing) {

                // per type
                Type type =
                        // VERSION70.containsAll(chars) ? Type.v70
                        // :
                        getFlagCode(chars) != null ? Type.countries
                                // : DOMINOS.containsAll(chars) ? Type.dominos
                                // : CARDS.containsAll(chars) ? Type.cards
                                // : MAHJONG.containsAll(chars) ? Type.majong
                                : Type.misc;
                EnumMap<Source, UnicodeSet> counter = data.get(type);
                if (counter == null) {
                    data.put(type, counter = new EnumMap<Source, UnicodeSet>(Source.class));
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

        public void write() throws IOException {
            PrintWriter out = BagFormatter.openUTF8Writer(Emoji.OUTPUT_DIR,
                    "missing-emoji-list.html");
            UnicodeSet jc = new UnicodeSet()
            .addAll(totalData.get(Source.sb))
            .addAll(totalData.get(Source.kddi))
            .addAll(totalData.get(Source.dcm))
            .freeze();
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
                    + new UnicodeSet(totalData.get(Source.gmail)).removeAll(jc).toPattern(false));
            System.out.println("jc-gmail" + "\t"
                    + new UnicodeSet(jc).removeAll(totalData.get(Source.gmail)).toPattern(false));

            for (Entry<Source, UnicodeSet> entry : totalData.entrySet()) {
                System.out.println(entry.getKey() + "\t" + entry.getValue().toPattern(false));
            }

            EnumSet<Source> skipRef = EnumSet.allOf(Source.class);
            skipRef.remove(Source.ref);
            skipRef.remove(Source.gmail);
            skipRef.remove(Source.sb);
            skipRef.remove(Source.dcm);
            skipRef.remove(Source.kddi);
            writeHeader(out, "Missing", "Missing list of emoji characters.");
            String headerRow = "<tr><th>" + "Type" + "</th>";
            for (Source type : skipRef) {
                headerRow += "<th width='" + (80.0 / skipRef.size()) + "%'>" + type + "</th>";
            }
            headerRow += "</tr>";

            for (Entry<Type, EnumMap<Source, UnicodeSet>> entry : data.entrySet()) {
                EnumMap<Source, UnicodeSet> values = entry.getValue();

                // find common
                UnicodeSet common = null;
                boolean skipSeparate = true;
                for (Source source : skipRef) {
                    final UnicodeSet us = ifNull(values.get(source), UnicodeSet.EMPTY);
                    if (common == null) {
                        common = new UnicodeSet(us);
                    } else if (!common.equals(us)) {
                        common.retainAll(us);
                        skipSeparate = false;
                    }
                }
                out.println(headerRow);
                // per source
                String sectionLink = getDoubleLink(entry.getKey().toString());
                if (!skipSeparate) {
                    out.print("<tr><th>" + sectionLink + " count</th>");
                    sectionLink = entry.getKey().toString();
                    for (Source source : skipRef) {
                        final UnicodeSet us = ifNull(values.get(source), UnicodeSet.EMPTY);
                        out.print("<td class='cchars'>" + (us.size() - common.size()) + "</td>");
                    }
                    out.print("</tr>");
                    out.print("<tr><th>" + entry.getKey() + " chars</th>");
                    for (Source source : skipRef) {
                        final UnicodeSet us = ifNull(values.get(source), UnicodeSet.EMPTY);
                        displayUnicodeSet(out, new UnicodeSet(us).removeAll(common), Style.bestImage, 0, 1, 1, null, CODEPOINT_COMPARE);
                    }
                    out.print("</tr>");
                }
                // common
                out.println("<tr><th>" + sectionLink + " count (common)</th>"
                        + "<td class='cchars' colSpan='" + skipRef.size() + "'>"
                        + common.size() + "</td></tr>");
                out.println("<tr><th>" + entry.getKey() + " (common)</th>");
                displayUnicodeSet(out, common, Style.bestImage, 0, skipRef.size(), 1, null, CODEPOINT_COMPARE);
                out.println("</td></tr>");
            }
            writeFooter(out);
            out.close();
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

    static String getFlag(String chars) {
        String core = Emoji.buildFileName(chars, "_");
        String filename = getImageFilename(Source.ref, core);
        String cc = getFlagRegionName(chars);
        return cc == null ? null : "<img"
                + " alt='" + chars + "'"
                + " class='imgf'"
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
        //print(Form.extraForm, missingMap, null);
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
        //showOrdering(Style.refImage);
        showLabels();
        showVersions();
        showVersionsOnly();
        showDefaultStyle();
        //showSubhead();
        showAnnotations();
        //        showAnnotationsDiff();
        // compareOtherAnnotations();
        showOtherUnicode();
        //oldAnnotationDiff();
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

    static Set<Source> MAIN_SOURCES = Collections.unmodifiableSet(EnumSet.of(Source.apple, Source.android, Source.twitter, Source.windows));

    private static void showNewCharacters() throws IOException {
        Set<String> optional = ANNOTATIONS_TO_CHARS.getValues("fitz-optional");
        Set<String> minimal = ANNOTATIONS_TO_CHARS.getValues("fitz-minimal");

        final IndexUnicodeProperties latest = IndexUnicodeProperties.make(GenerateEnums.ENUM_VERSION);
        final UnicodeMap<String> emojiDCM = latest.load(UcdProperty.Emoji_DCM);
        final UnicodeMap<String> emojiKDDI = latest.load(UcdProperty.Emoji_KDDI);
        final UnicodeMap<String> emojiSB = latest.load(UcdProperty.Emoji_SB);
        UnicodeSet carriers = new UnicodeSet()
        .addAll(emojiDCM.keySet())
        .addAll(emojiKDDI.keySet())
        .addAll(emojiSB.keySet())
        .freeze();
        UnicodeSet otherStandard = new UnicodeSet(carriers);
        for (String s : Emoji.EMOJI_CHARS) {
            String image = getImage(Source.apple, s, false);
            if (image != null) {
                otherStandard.add(s);
            }
        }
        otherStandard.removeAll(carriers).freeze();
        UnicodeSet nc = new UnicodeSet(Emoji.EMOJI_CHARS)
        .removeAll(carriers)
        .removeAll(otherStandard)
        .removeAll(Emoji.FLAGS).freeze();
        UnicodeSet otherFlags = new UnicodeSet(Emoji.FLAGS)
        .removeAll(carriers).freeze();

        // Set<String> newChars =
        // ANNOTATIONS_TO_CHARS.getValues("fitz-minimal");
        PrintWriter out = BagFormatter.openUTF8Writer(Emoji.TR51_OUTPUT_DIR, "emoji-count.html");
        writeHeader(out, "Temp Items", "no message");
        showRow(out, "Minimal", minimal);
        showRow(out, "Optional", optional);
        showRow(out, "JCarriers", carriers.addAllTo(new TreeSet<String>(CODEPOINT_COMPARE)));
        showRow(out, "Other Std", otherStandard.addAllTo(new TreeSet<String>(CODEPOINT_COMPARE)));
        showRow(out, "Other Flags", otherFlags.addAllTo(new TreeSet<String>(CODEPOINT_COMPARE)));
        showRow(out, "New7", nc.addAllTo(new TreeSet<String>(CODEPOINT_COMPARE)));
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
        PrintWriter out = BagFormatter.openUTF8Writer(Emoji.OUTPUT_DIR, "text-style.html");
        PrintWriter out2 = BagFormatter.openUTF8Writer(Emoji.OUTPUT_DIR, "text-vs.txt");
        writeHeader(out, "Text vs Emoji", "Default style (text vs emoji) by version. The 'Dings' include Dingbats, Webdings, and Wingdings.");
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
            // 2764 FE0E; text style;  # HEAVY BLACK HEART
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
            out.print(getBestImage(emoji, true, Source.apple));
            if (link != null) {
                out.print("</a>");
            }
            out.print(" ");
        }
    }

    private static void showRow(PrintWriter out, String title, Set<String> minimal) {
        out.print("<tr><td>" + title + "</td>\n<td>");
        out.print("<tr><td>" + minimal.size() + "</td>\n<td>");
        showExplicitAppleImages(out, minimal);
        out.print("</td>\n<td width='45%'>");
        showNames(out, minimal);
        out.println("</td><tr>");
    }

    private static void showNames(PrintWriter out, Set<String> minimal) {
        UnicodeSet us = new UnicodeSet().addAll(minimal);
        for (EntryRange r : us.ranges()) {
            out.print(getCodeAndName2(UTF16.valueOf(r.codepoint)));
            if (r.codepoint != r.codepointEnd) {
                out.print("<br>\n…" + getCodeAndName2(UTF16.valueOf(r.codepointEnd)));
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
            out.println(getBestImage(emoji, false, Source.apple));
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
            if (file.isDirectory()) {
                if (!file.getName().equals("other")) {
                    addFileCodepoints(file, results);
                }
                continue;
            }
            String s = file.getName();
            String original = s;
            if (s.startsWith(".") || !s.endsWith(".png") || s.contains("emoji-palette")) {
                continue;
            }
            String chars = Emoji.parseFileName(true, s);
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
        PrintWriter out = BagFormatter.openUTF8Writer(Emoji.OUTPUT_DIR, "emoji-labels.html");
        writeHeader(out, "Emoji Labels", "Main categories for character picking. " +
                "Characters may occur more than once. " +
                "Categories could be grouped in the UI.");
        for (Entry<Label, Set<String>> entry : Label.CHARS_TO_LABELS.valueKeysSet()) {
            Label label = entry.getKey();
            Set<String> set = entry.getValue();
            String word = label.toString();
            Set<String> values = entry.getValue();
            UnicodeSet uset = new UnicodeSet().addAll(values);

            displayUnicodeset(out, Collections.singleton(word), null, uset, Style.bestImage, null);
        }
        writeFooter(out);
        out.close();
    }

    private static void showOrdering(Style style) throws IOException {
        PrintWriter out = BagFormatter.openUTF8Writer(Emoji.OUTPUT_DIR,
                (style == Style.bestImage ? "" : "ref-") + "emoji-ordering.html");
        writeHeader(out, "Emoji Ordering",
                "Proposed default ordering, designed to improve on the <a href='http://www.unicode.org/charts/collation/'>UCA</a> " +
                        "orderings (shown at the bottom), by grouping similar items together, such as ." +
                        "The cell divisions are an artifact, simply to help in review. " +
                "The left side is an emoji image (* colorful where possible), while the right is black and white.");

        final Set<Entry<String, Set<String>>> keyValuesSet = ORDERING_TO_CHAR.keyValuesSet();
        final int rows = keyValuesSet.size();
        out.println("<tr><th>Colored*</th>"
                + "<th rowSpan='" + (rows + 1) + "'>&nbsp;</th>"
                + "<th>B&W</th>"
                + "<th rowSpan='" + (rows + 1) + "'>&nbsp;</th>"
                + "<th>Unicode Collation Order (UCA - DUCET)</th></tr>");
        UnicodeSet all = new UnicodeSet();
        for (Entry<String, Set<String>> entry : keyValuesSet) {
            all.addAll(entry.getValue());
        }
        boolean first = true;
        final int charsPerRow = 8;
        for (Entry<String, Set<String>> entry : keyValuesSet) {
            out.println("<tr>");
            final UnicodeSet values = new UnicodeSet().addAll(entry.getValue());
            displayUnicodeSet(out, values, Style.bestImage, charsPerRow, 1, 1, null, CODEPOINT_COMPARE);
            displayUnicodeSet(out, values, Style.refImage, charsPerRow, 1, 1, null, CODEPOINT_COMPARE);
            if (first) {
                first = false;
                displayUnicodeSet(out, all, Style.bestImage, charsPerRow, 1, rows, null, UCA_COLLATOR);
            }
            out.println("</tr>");
        }
        writeFooter(out);
        out.close();
    }

    private static void showDefaultStyle() throws IOException {
        PrintWriter out = BagFormatter.openUTF8Writer(Emoji.OUTPUT_DIR, "emoji-style.html");
        writeHeader(out, "Emoji Default Style Values", "Default Style Values for display.");
        for (Entry<Style, Set<String>> entry : STYLE_TO_CHARS.keyValuesSet()) {
            Style label = entry.getKey();
            Set<String> set = entry.getValue();
            String word = label.toString();
            Set<String> values = entry.getValue();
            UnicodeSet plain = new UnicodeSet();
            UnicodeSet emoji = new UnicodeSet();
            UnicodeSet text = new UnicodeSet();
            for (String value : values) {
                plain.add(value);
                String emojiStyle = getEmojiVariant(value, Emoji.EMOJI_VARIANT_STRING);
                if (!value.equals(emojiStyle)) {
                    emoji.add(value);
                }
                String textStyle = getEmojiVariant(value, Emoji.TEXT_VARIANT_STRING);
                if (!value.equals(textStyle)) {
                    text.add(value);
                }
            }
            final Set<String> singletonWord = Collections.singleton(word);
            // xxx
            displayUnicodeset(out, singletonWord, "", plain, Style.plain, null);
            displayUnicodeset(out, singletonWord, "with emoji variant", emoji, Style.emoji, null);
            displayUnicodeset(out, singletonWord, "with text variant", text, Style.text, null);
        }
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

    private static void showVersions() throws IOException {
        PrintWriter out = BagFormatter.openUTF8Writer(Emoji.OUTPUT_DIR, "emoji-versions-sources.html");
        writeHeader(out, "Emoji Versions & Sources", "Versions and sources for Emoji.");
        UnicodeMap<VersionData> m = new UnicodeMap<>();
        TreeSet<VersionData> sorted = getSortedVersionInfo(m);
        for (VersionData value : sorted) {
            UnicodeSet chars = m.getSet(value);
            displayUnicodeset(out, Collections.singleton(value.getCharSources()), value.getVersion(),
                    Collections.singleton(String.valueOf(chars.size())), chars, Style.bestImage, null);
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

    private static void showVersionsOnly() throws IOException {
        PrintWriter out = BagFormatter.openUTF8Writer(Emoji.OUTPUT_DIR, "emoji-versions.html");
        writeHeader(out, "Emoji Versions", "Versions for Emoji.");
        UnicodeMap<Age_Values> m = new UnicodeMap<>();
        for (String s : Emoji.EMOJI_CHARS) {
            Data data = Data.STRING_TO_DATA.get(s);
            m.put(s, data.age);
        }
        TreeSet<Age_Values> sorted = new TreeSet<>(m.values());
        for (Age_Values value : sorted) {
            UnicodeSet chars = m.getSet(value);
            displayUnicodeset(out, Collections.singleton(value.toString().replace("_", ".")), null,
                    Collections.singleton(String.valueOf(chars.size())), chars, Style.refImage, null);
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
        PrintWriter out = BagFormatter.openUTF8Writer(Emoji.OUTPUT_DIR, "emoji-subhead.html");
        writeHeader(out, "Emoji Subhead", "Unicode Subhead mapping.");
        for (Entry<String, UnicodeSet> entry : subheadToChars.entrySet()) {
            String label = entry.getKey();
            UnicodeSet uset = entry.getValue();
            if (label.equalsIgnoreCase("exclude")) {
                continue;
            }
            displayUnicodeset(out, Collections.singleton(label), null, uset, Style.emoji, null);
        }
        writeFooter(out);
        out.close();
    }

    private static void showAnnotations() throws IOException {
        PrintWriter out = BagFormatter.openUTF8Writer(Emoji.OUTPUT_DIR, "emoji-annotations.html");
        writeHeader(out, "Emoji Annotations", "Finer-grained character annotations. ");

        Relation<UnicodeSet, String> seen = Relation.of(new HashMap(), TreeSet.class, CODEPOINT_COMPARE);
        for (Entry<String, Set<String>> entry : GenerateEmoji.ANNOTATIONS_TO_CHARS.keyValuesSet()) {
            String word = entry.getKey();
            Set<String> values = entry.getValue();
            UnicodeSet uset = new UnicodeSet().addAll(values);
            try {
                Label label = Label.valueOf(word);
                continue;
            } catch (Exception e) {
            }
            seen.put(uset, word);
        }
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
            UnicodeSet uset = new UnicodeSet().addAll(values);
            // Set<String> words = seen.getAll(uset);
            // if (words == null || labelSeen.contains(words)) {
            // continue;
            // }
            // labelSeen.add(words);
            displayUnicodeset(out, words, null, uset, Style.bestImage, "full-emoji-list.html");
        }
        writeFooter(out);
        out.close();
    }

    //    private static void showAnnotationsDiff() throws IOException {
    //        PrintWriter out = BagFormatter.openUTF8Writer(Emoji.OUTPUT_DIR, "emoji-annotations-diff.html");
    //        writeHeader(out, "Emoji Annotations Diff", "Finer-grained character annotations. " +
    //                "For brevity, flags are not shown: they would have names of the associated countries.");
    //        out.println("<tr><th>Code</th><th>Image</th><th>Name</th><th>Old-Only</th><th>New-Only</th><th>Same Annotation</th></tr>");
    //
    //        for (String emoji : SORTED_EMOJI_CHARS_SET) {
    //            Set<String> values = ANNOTATIONS_TO_CHARS.getKeys(emoji);
    //            Set<String> valuesNew = ANNOTATIONS_TO_CHARS_NEW.getKeys(emoji);
    //            boolean sameValues = Objects.equals(values, valuesNew);
    //            Set<String> same = new LinkedHashSet(values);
    //            same.retainAll(valuesNew);
    //            Set<String> oldOnly = new LinkedHashSet(values);
    //            oldOnly.removeAll(valuesNew);
    //            Set<String> newOnly = new LinkedHashSet(valuesNew);
    //            newOnly.removeAll(values);
    //            UnicodeSet uset = new UnicodeSet().add(emoji);
    //            out.print("<tr>");
    //            out.println("<td class='code'>" + getDoubleLink(Utility.hex(emoji, " ")) + "</td>\n");
    //
    //            displayUnicodeSet(out, uset, Style.bestImage, 16, 1, 1, "full-emoji-list.html", CODEPOINT_COMPARE);
    //            out.println("<td>" + getName(emoji, true) + "</td>\n");
    //            if (sameValues) {
    //                out.println("<td colSpan='3' bgcolor='#EEE'>" + CollectionUtilities.join(same, ", ") + "</td>\n");
    //            } else {
    //                out.println("<td bgcolor='#DFD'>" + CollectionUtilities.join(oldOnly, ", ") + "</td>\n");
    //                out.println("<td bgcolor='#DDF'>" + CollectionUtilities.join(newOnly, ", ") + "</td>\n");
    //                out.println("<td>" + CollectionUtilities.join(same, ", ") + "</td>\n");
    //            }
    //            out.println("</tr>");
    //        }
    //        writeFooter(out);
    //        out.close();
    //    }

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

        PrintWriter out = BagFormatter.openUTF8Writer(Emoji.OUTPUT_DIR, "other-labels.html");
        writeHeader(out, "Other Labels", "Draft categories for other Symbols and Punctuation.");

        for (Entry<String, UnicodeSet> entry : labelToUnicodeSet.entrySet()) {
            String label = entry.getKey();
            UnicodeSet uset = entry.getValue();
            if (label.equalsIgnoreCase("exclude")) {
                continue;
            }
            displayUnicodeset(out, Collections.singleton(label), null, uset, Style.plain, "");
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
            UnicodeSet uset, Style showEmoji, String link) {
        displayUnicodeset(out, labels, sublabel, Collections.EMPTY_SET, uset, showEmoji, link);
    }

    public static <T extends Object> void displayUnicodeset(PrintWriter out, Set<T> labels, String sublabel, Set<String> otherCols,
            UnicodeSet uset, Style showEmoji, String link) {
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
        displayUnicodeSet(out, uset, showEmoji, 16, 1, 1, link, CODEPOINT_COMPARE);
        out.println("</tr>");
    }

    public static void displayUnicodeSet(PrintWriter out,
            UnicodeSet uset, Style showEmoji, int maxPerLine, int colSpan, int rowSpan,
            String link, Comparator comparator) {
        if (link == null) {
            link = "full-emoji-list.html";
        } else if (link.isEmpty()) {
            link = null;
        }
        out.println("<td class='lchars'"
                + (rowSpan <= 1 ? "" : " rowSpan='" + rowSpan + "'")
                + (colSpan <= 1 ? "" : " colSpan='" + colSpan + "'")
                + ">");
        Set<String> sorted = uset.addAllTo(new TreeSet(comparator));
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
            if (link != null) {
                out.print("<a href='" + link + "#" + Emoji.buildFileName(s, "_") + "' target='full'>");
            }
            String cell = getFlag(s);
            if (cell == null) {
                switch (showEmoji) {
                case text:
                case emoji:
                    cell = getEmojiVariant(s, showEmoji == Style.emoji ? Emoji.EMOJI_VARIANT_STRING : Emoji.TEXT_VARIANT_STRING);
                    cell = "<span title='" +
                            getHex(s) + " " + getName(s, true) + "'>"
                            + cell
                            + "</span>";
                    break;
                case plain:
                    cell = "<span title='" +
                            getHex(s) + " " + getName(s, true) + "'>"
                            + s
                            + "</span>";
                    break;
                case bestImage:
                    cell = getBestImage(s, true);
                    break;
                case refImage:
                    cell = getImage(Source.ref, s, true);
                    break;
                }
            }
            out.print(cell);
            if (link != null) {
                out.println("</a>");
            }
        }
        out.println("</td>");
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
        final String name = NAME.get(s.codePointAt(0));
        if (s.indexOf(Emoji.ENCLOSING_KEYCAP) >= 0) {
            return "keycap " + (tolower ? name.toLowerCase(Locale.ENGLISH) : name);
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
            result.replaceAll("\\s\\s+", " ").trim();
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

    enum Form {
        shortForm("short", " short form."),
        noImages("", " with single image."),
        fullForm("full", "with images."),
        extraForm("extra", " with images; have icons but are not including.");
        final String filePrefix;
        final String title;
        final String description;

        Form(String prefix, String description) {
            this.description = description;
            filePrefix = prefix.isEmpty() ? "" : prefix + "-";
            title = "Emoji Data"
                    + (prefix.isEmpty() ? "" : " (" + UCharacter.toTitleCase(prefix, null) + ")");
        }
    }

    public static <T> void print(Form form, Map<String, Data> set, Stats stats) throws IOException {
        PrintWriter out = BagFormatter.openUTF8Writer(Emoji.OUTPUT_DIR,
                form.filePrefix + "emoji-list.html");
        PrintWriter outText = null;
        PrintWriter outText2 = null;
        int order = 0;
        UnicodeSet level1 = null;
        writeHeader(out, form.title, "List of emoji characters, " + form.description);
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
        //PrintWriter outText = null;
        PrintWriter outText2 = null;
        int order = 0;
        UnicodeSet level1 = null;
        //outText = BagFormatter.openUTF8Writer(Emoji.OUTPUT_DIR, "emoji-data-old.txt");
        outText2 = BagFormatter.openUTF8Writer(Emoji.OUTPUT_DIR, "emoji-data.txt");
        String format = "# Code ; Default Style ; Sources ; Version # (Character) Name\n";
        //outText.println(dataHeader("", "", ""));
        outText2.println(dataHeader());
        level1 = new UnicodeSet()
        .addAll(stats.totalData.get(Source.apple))
        .retainAll(stats.totalData.get(Source.android))
        .retainAll(stats.totalData.get(Source.windows))
        .retainAll(stats.totalData.get(Source.twitter))
        .freeze();
        for (Data data : new TreeSet<Data>(set.values())) {
            //outText.println(data.toSemiString(order++, null));
            outText2.println(data.toSemiString(order++, level1));
        }
        //outText.close();
        outText2.close();
    }

    private static String dataHeader() {
        return "# DRAFT emoji-data.txt\n" 
                + "# For details about the format and other information, see " + DOC_DATA_FILES + ".\n" 
                + "#\n" 
                + "# Format: Code ; Default_Emoji_Style ; Emoji_Level ; Emoji_Modifier_Status ; Emoji_Sources ; Version # (Character) Name\n" 
                + "#\n"
                + "#   Field 1 — Default_Emoji_Style:\n"
                + "#             text:      default text presentation\n"
                + "#             emoji:     default emoji presentation\n"
                + "#   Field 2 — Emoji_Level:\n"
                + "#             L1:        level 1 emoji\n"
                + "#             L2:        level 2 emoji\n"
                + "#   Field 3 — Emoji_Modifier_Status:\n"
                + "#             modifier:  an emoji modifier\n"
                + "#             minimal:   a minimal emoji modifier base\n"
                + "#             optional:  an optional emoji modifier base\n"
                + "#             none:      none of the above\n"
                + "#   Field 4 — Emoji_Sources (Informative):\n"
                + "#             one or more values from {z, a, j, w, x}\n"
                + "#             see the key in http://www.unicode.org/draft/reports/tr51/tr51.html#Major_Sources\n"
                + "#   Field 5 — Version (Informative):\n"
                + "#             version the character was first encoded, from DerivedAge\n#";
    }

    static final String FOOTER = "</table>" + Utility.repeat("<br>", 60) + "</body></html>";

    public static void writeFooter(PrintWriter out) {
        out.println(FOOTER);
    }

    public static void writeHeader(PrintWriter out, String title, String firstLine) {
        out.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n" +
                "<html>\n" +
                "<head>\n" +
                "<meta http-equiv='Content-Type' content='text/html; charset=UTF-8'>\n" +
                "<link rel='stylesheet' type='text/css' href='emoji-list.css'>\n" +
                "<title>Draft " +
                title +
                "</title>\n" +
                "</head>\n" +
                "<body>\n"
                + "<h1>Draft " + title + "</h1>\n"
                + "<p>" + firstLine +
                " For details about the format and other information, see " +
                "<a target='text' href='" +
                DOC_DATA_FILES +
                "'>Unicode Emoji</a>.</p>\n"
                + "<table border='1'>");
    }

    static boolean CHECKFACE = false;

    static void oldAnnotationDiff() throws IOException {
        PrintWriter out = BagFormatter.openUTF8Writer(Emoji.OUTPUT_DIR, "emoji-diff.html");
        writeHeader(out, "Diff List", "Differences from other categories.");

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
                final File file = new File(Emoji.IMAGES_OUTPUT_DIR, filename);
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
                PrintWriter outText = BagFormatter.openUTF8Writer(Emoji.OUTPUT_DIR, "emoji-ordering.txt")) {
            outText.append("<!-- DRAFT emoji-ordering.txt\n"
                    + "\tFor details about the format and other information, see " + DOC_DATA_FILES + ".\n"
                    + "\thttp://unicode.org/cldr/trac/ticket/7270 -->\n"
                    + "<collation type='emoji'>\n"
                    + "<cr><![CDATA[\n"
                    + "# START AUTOGENERATED EMOJI ORDER\n");
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
                            // put all the 26 regional indicators in order at this point
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
                    outText.append("\n<").append(s);
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
            outText.write("\n]]></cr>\n</collation>");
        }
    }

    static final String ANNOTATION_HEADER = "<?xml version='1.0' encoding='UTF-8' ?>\n"
            + "<!DOCTYPE ldml SYSTEM '../../common/dtd/ldml.dtd'>\n"
            + "<!-- Copyright © 1991-2013 Unicode, Inc. DRAFT emoji-annotations.txt For \n"
            + " details about the format and other information, see /../../../reports/tr51/index.html#Data_Files. \n"
            + " http://unicode.org/cldr/trac/ticket/8019 CLDR data files are interpreted \n"
            + " according to the LDML specification (http://unicode.org/reports/tr35/) For \n"
            + " terms of use, see http://www.unicode.org/copyright.html \n"
            + " This is still under development, and will be refined before release. \n"
            + " In particular, the annotations like 'people-apple' are only present during development, and will be withdrawn for the release.\n"
            + " -->\n"
            + "<ldml>\n"
            + "\t<identity>\n"
            + "\t\t<version number='$Revision: 10585 $' />\n"
            + "\t\t<generation date='$Date: 2014-06-19 06:23:55 +0200 (Thu, 19 Jun 2014) $' />\n";

    private static void printAnnotations() throws IOException {
        try (
                PrintWriter outText = BagFormatter.openUTF8Writer(Emoji.OUTPUT_DIR, "emoji-annotations.xml")) {
            outText.append(ANNOTATION_HEADER
                    + "\t\t<language type='en'/>\n"
                    + "\t</identity>\n"
                    + "\t<annotations>\n");
            Set<Row.R2<Set<String>,UnicodeSet>> sorted = new TreeSet<>(PAIR_SORT);
            for (Entry<Set<String>, Set<String>> s : ANNOTATIONS_TO_CHARS.getValuesToKeys().keyValuesSet()) {
                UnicodeSet chars = new UnicodeSet().addAll(s.getKey());
                Set<String> annotations = s.getValue();
                sorted.add(Row.of(annotations, chars));
            }
            for (R2<Set<String>, UnicodeSet> s : sorted) {
                String annotations = CollectionUtilities.join(s.get0(), "; ");
                UnicodeSet chars = s.get1();
                outText.append("\t\t<annotation cp='")
                .append(chars.toPattern(false))
                .append("'>")
                .append(annotations)
                .append("</annotation>\n")
                ;
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

    static final Comparator<Row.R2<Set<String>,UnicodeSet>> PAIR_SORT = new Comparator<Row.R2<Set<String>,UnicodeSet>>() {
        SetComparator<Comparable> setComp;
        public int compare(R2<Set<String>, UnicodeSet> o1, R2<Set<String>, UnicodeSet> o2) {
            int diff = compareX(o1.get0().iterator(), o2.get0().iterator(), (Comparator<String>) UCA_COLLATOR);
            if (diff != 0) {
                return diff;
            }
            return o1.get1().compareTo(o2.get1());
        }
    };
}
