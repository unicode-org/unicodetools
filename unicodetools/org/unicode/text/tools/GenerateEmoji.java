package org.unicode.text.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.draft.ScriptMetadata.Trinary;
import org.unicode.cldr.util.With;
import org.unicode.draft.GetNames;
import org.unicode.jsp.Subheader;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;
import org.unicode.props.UcdPropertyValues.Binary;
import org.unicode.props.UcdPropertyValues.Block_Values;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.text.UCA.UCA;
import org.unicode.text.UCA.UCA.CollatorType;
import org.unicode.text.UCA.WriteCollationData;
import org.unicode.text.UCD.Default;
import org.unicode.text.tools.GenerateEmoji.Data;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.MultiComparator;
import com.ibm.icu.impl.Row;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.LocaleDisplayNames;
import com.ibm.icu.text.LocaleDisplayNames.DialectHandling;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UTF16.StringComparator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class GenerateEmoji {
    private static final char ENCLOSING_KEYCAP = '\u20E3';
    private static final UnicodeSet ASCII_LETTER_HYPHEN = new UnicodeSet('-', '-', 'A', 'Z', 'a', 'z').freeze();
    private static final UnicodeSet EXTRAS = new UnicodeSet(
            "[\\u2714\\u2716\\u303D\\u3030 \\u00A9 \\u00AE \\u2795-\\u2797 \\u27B0 \\U0001F519-\\U0001F51C {🇽🇰}]")
    .add("*"+ENCLOSING_KEYCAP)
    .freeze();
    static final UnicodeSet EXCLUDE = new UnicodeSet(
            "[🖫🕾🕿🕻🕼🕽🕾🕿🖀🖪🖬🖭🗴🗵🗶🗷🗸🗹★☆⛫⛩\uFFFC⛤-⛧ ⌤⌥⌦⌧⌫⌬⎆⎇⎋⎗⎘⎙⎚⏣⚝⛌⛚⛬⛭⛮⛯⛶⛻✓🆊\\U0001F544-\\U0001F549]").freeze();

    private static final String OUTPUT_DIR = "/Users/markdavis/workspace/unicode-draft/reports/tr51/";
    private static final String IMAGES_OUTPUT_DIR = OUTPUT_DIR + "images/";
    static final Pattern tab = Pattern.compile("\t");
    static final Pattern space = Pattern.compile(" ");
    static final char EMOJI_VARIANT = '\uFE0F';
    static final String EMOJI_VARIANT_STRING = String.valueOf(EMOJI_VARIANT);
    static final char TEXT_VARIANT = '\uFE0F';
    static final String REPLACEMENT_CHARACTER = "\uFFFD";
    static final String TEXT_VARIANT_STRING = String.valueOf(TEXT_VARIANT);

    static final Set<String> SKIP_WORDS = new HashSet(Arrays.asList("with", "a", "in", "without", "and", "white", "symbol", "sign", "for", "of", "black"));
    static final Map<Row.R2<Integer, Integer>,Integer> ANDROID_REMAP = new HashMap<>();
    static final UnicodeMap<String> ANDROID_REMAP_VALUES = new UnicodeMap();
    static final Set<String> ANDROID_IMAGES = new TreeSet<>();
    private static final Comparator<String> CODEPOINT_LENGTH = new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            return o1.codePointCount(0, o1.length()) - o2.codePointCount(0, o2.length());
        }
    };
    static {
        addAndroidRemap("🇨🇳", 0xFE4ED); // cn
        addAndroidRemap("🇩🇪", 0xFE4E8); // de
        addAndroidRemap("🇪🇸", 0xFE4ED); // es
        addAndroidRemap("🇫🇷", 0xFE4E7); // fr
        addAndroidRemap("🇬🇧", 0xfe4eA); // gb
        addAndroidRemap("🇮🇹", 0xFE4E9); // it
        addAndroidRemap("🇯🇵", 0xFE4E5); // ja
        addAndroidRemap("🇰🇷", 0xFE4EE); // ko
        addAndroidRemap("🇷🇺", 0xFE4EC); // ru
        addAndroidRemap("🇺🇸", 0xFE4E6); // us
        addAndroidRemap("#⃣", 0xFE82C);
        for (int i = 1; i <= 9; ++i) {
            addAndroidRemap((char)('0' + i) + "" + ENCLOSING_KEYCAP, 0xFE82D + i); // 1 => U+FE82E
        }
        addAndroidRemap("0⃣", 0xFE837);
    }

    public static Integer addAndroidRemap(String real, int replacement) {
        ANDROID_REMAP_VALUES.put(replacement, real);
        int first = real.codePointAt(0);
        return ANDROID_REMAP.put(Row.of(first, real.codePointAt(Character.charCount(first))), replacement);
    }

    public static String androidPng(int firstCodepoint, int secondCodepoint, boolean first) {
        if (secondCodepoint == ENCLOSING_KEYCAP) {
            int debug = 0;
        }
        if (secondCodepoint != 0) {
            Integer remapped = ANDROID_REMAP.get(Row.of(firstCodepoint, secondCodepoint));
            if (remapped != null) {
                if (!first) {
                    return null;
                }
                firstCodepoint = remapped;
            } else {
                return null;
            }
        }
        String filename = "android/emoji_u" + Utility.hex(first ? firstCodepoint : secondCodepoint).toLowerCase(Locale.ENGLISH) + ".png";
        ANDROID_IMAGES.add(filename);
        return filename;
    }

    static final Comparator CODEPOINT_COMPARE = 
            new MultiComparator<String>(
                    UCA.buildCollator(null), // don't need cldr features
                    new UTF16.StringComparator(true,false,0));

    static final Comparator CODEPOINT_COMPARE_SHORTER = 
            new MultiComparator<String>(
                    CODEPOINT_LENGTH,
                    UCA.buildCollator(null), // don't need cldr features
                    new UTF16.StringComparator(true,false,0)); 
    //Collator.getInstance(ULocale.ENGLISH);

    static final IndexUnicodeProperties LATEST = IndexUnicodeProperties.make(Default.ucdVersion());
    static final UnicodeMap<String> STANDARDIZED_VARIANT = LATEST.load(UcdProperty.Standardized_Variant);
    static final UnicodeMap<String> VERSION = LATEST.load(UcdProperty.Age);
    static final UnicodeMap<String> WHITESPACE = LATEST.load(UcdProperty.White_Space);
    static final UnicodeMap<String> GENERAL_CATEGORY = LATEST.load(UcdProperty.General_Category);
    static final UnicodeMap<String> SCRIPT_EXTENSIONS = LATEST.load(UcdProperty.Script_Extensions);
    private static final UnicodeSet COMMON_SCRIPT = new UnicodeSet()
    .addAll(SCRIPT_EXTENSIONS.getSet(UcdPropertyValues.Script_Values.Common.toString()))
    .freeze();

    static final UnicodeMap<String> NFKCQC = LATEST.load(UcdProperty.NFKD_Quick_Check);
    static final UnicodeMap<String> NAME = LATEST.load(UcdProperty.Name);
    static final UnicodeSet JSOURCES = new UnicodeSet();
    static {
        JSOURCES
        .addAll(LATEST.load(UcdProperty.Emoji_DCM).keySet())
        .addAll(LATEST.load(UcdProperty.Emoji_KDDI).keySet())
        .addAll(LATEST.load(UcdProperty.Emoji_SB).keySet())
        .removeAll(WHITESPACE.getSet(UcdPropertyValues.Binary.Yes.toString()))
        .freeze();
        System.out.println("Core:\t" + JSOURCES.size() + "\t" + JSOURCES);
    }
    //    static final Map<String,String> REMAP_FLAGS = new HashMap();
    //    static {
    //        addFlagRemap("BL", "FR");
    //        addFlagRemap("BV", "NO");
    //        addFlagRemap("GF", "FR");
    //        addFlagRemap("HM", "AU");   
    //        addFlagRemap("MF", "FR");
    //        addFlagRemap("RE", "FR");
    //        addFlagRemap("SJ", "NO");
    //        addFlagRemap("TF", "FR");
    //        addFlagRemap("UM", "US");
    //        addFlagRemap("WF", "FR");
    //        addFlagRemap("YT", "FR");
    //    }
    //    public static void addFlagRemap(String originalCountry, String replacementCountry) {
    //        REMAP_FLAGS.put(originalCountry, replacementCountry);
    //        System.out.println(
    //                Emoji.buildFileName(Emoji.getHexFromFlagCode(originalCountry),"_") 
    //                + " => "
    //                + Emoji.buildFileName(Emoji.getHexFromFlagCode(replacementCountry),"_"));
    //    }
    static final Subheader subheader = new Subheader("/Users/markdavis/workspace/unicodetools/data/ucd/7.0.0-Update/");
    static final Set<String> SKIP_BLOCKS = new HashSet(Arrays.asList("Miscellaneous Symbols", 
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
    static class BiRelation<K,V> {
        Relation<K,V> keyToValues = Relation.of(new TreeMap(CODEPOINT_COMPARE), TreeSet.class, CODEPOINT_COMPARE);
        Relation<V,K> valueToKey = Relation.of(new TreeMap(CODEPOINT_COMPARE), TreeSet.class, CODEPOINT_COMPARE);
        BiRelation<K, V> add(K key, V value) {
            keyToValues.put(key, value);
            valueToKey.put(value, key);
            return this;
        }
        public BiRelation<K, V> removeKey(K key) {
            Set<V> values = keyToValues.get(key);
            if (values == null) {
                return this;
            }
            for (V value : values) {
                valueToKey.remove(value, key);
            }
            keyToValues.removeAll(key);
            return this;
        }
        public Set<V> getValues(K key) {
            return keyToValues.get(key);
        }
        public Set<K> getKeys(V value) {
            return valueToKey.get(value);
        }
        public Set<Entry<K, Set<V>>> keyValuesSet() {
            return keyToValues.keyValuesSet();
        }
        public Set<Entry<V, Set<K>>> valueKeysSet() {
            return valueToKey.keyValuesSet();
        }
        public void freeze() {
            keyToValues.freeze();
            valueToKey.freeze();
        }
    }

    static final BiRelation<String,String> ANNOTATIONS_TO_CHARS = new BiRelation();
    static {
        String lastLabel = null;
        for (String line : FileUtilities.in(GenerateEmoji.class, "emojiAnnotations.txt")) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            // U+00AE   Registered symbol, Registered
            if (line.startsWith("*")) {
                lastLabel = line.substring(1).trim();
                continue;
            }
            for (int i = 0; i < line.length();) {
                String string = getEmojiSequence(line, i);
                i += string.length();
                if (skipEmojiSequence(string)) {
                    continue;
                }
                ANNOTATIONS_TO_CHARS.add(lastLabel, string);
            }
        }
        ANNOTATIONS_TO_CHARS.freeze();
    }
    static final Transform<String,String> WINDOWS_URL = new Transform<String,String>() {
        public String transform(String s) {
            String base = "images/windows/windows_";
            String separator = "_";
            return base + Emoji.buildFileName(s, separator) + ".png";
        }

    };

    static final BiRelation<String,String> NAME_TO_CHARS = new BiRelation<>();
    static {
        addOldAnnotations();
    }
    enum Label {
        people, body, face, nature, clothing, emotion, 
        food, travel, place, office,
        time, weather, game, sport, activity, object,
        sound, 
        flag,    
        arrow,
        word,
        sign, 
        //unknown,
        ;

        static Label get(String string) {
            return Label.valueOf(string);
        }
        static final Relation<String, Label> CHARS_TO_LABELS 
        = Relation.of(new TreeMap(), TreeSet.class);

        static {
            Label lastLabel = null;
            String sublabel = null;
            for (String line : FileUtilities.in(GenerateEmoji.class, "emojiLabels.txt")) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                char first = line.charAt(0);
                if (first == '*') {
                    sublabel = line.substring(1).trim();
                } else if ('a' <= first && first <= 'z') {
                    if (!ASCII_LETTER_HYPHEN.containsAll(line)) {
                        throw new IllegalArgumentException();
                    }
                    lastLabel = Label.get(line);
                    sublabel = null;
                } else {
                    for (int i = 0; i < line.length();) {
                        String string = getEmojiSequence(line, i);
                        i += string.length();
                        if (skipEmojiSequence(string)) {
                            continue;
                        }
                        CHARS_TO_LABELS.put(string, lastLabel);
                        if (sublabel != null) {
                            NAME_TO_CHARS.add(sublabel, string);
                        }
                        //NAME_TO_CHARS.put(lastLabel.toString(), string);
                    }
                }
            }
            // add sublabels
            for (Label label : Label.values()) {
                Set<String> s = NAME_TO_CHARS.getValues(label.toString());
                if (s != null) {
                    for (String ss : s) {
                        CHARS_TO_LABELS.put(ss, label);
                    }
                }
                NAME_TO_CHARS.removeKey(label.toString());
            }
            for (String isoCountries : ULocale.getISOCountries()) {
                if (!ASCII_LETTER_HYPHEN.containsAll(isoCountries)) {
                    continue;
                }
                String cc = Emoji.getHexFromFlagCode(isoCountries);
                CHARS_TO_LABELS.put(cc, Label.flag);
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
            if (false) for (Entry<String, Set<Label>> entry : CHARS_TO_LABELS.keyValuesSet()) {
                System.out.println(i++ + "\t" + entry.getKey() + "\t" + entry.getValue());
            }
        }


    }

    public static boolean skipEmojiSequence(String string) {
        if (string.equals(" ") 
                || string.equals(EMOJI_VARIANT_STRING) 
                || string.equals(TEXT_VARIANT_STRING)
                || EXCLUDE.contains(string)) {
            return true;
        }
        return false;
    }

    private static String getEmojiSequence(String line, int i) {
        int firstCodepoint = line.codePointAt(i);
        int firstLen = Character.charCount(firstCodepoint);
        if (i + firstLen == line.length()) {
            return line.substring(i, i+firstLen);
        }
        int secondCodepoint = line.codePointAt(i+firstLen);
        int secondLen = Character.charCount(secondCodepoint);
        if (secondCodepoint == ENCLOSING_KEYCAP
                || (Emoji.isRegionalIndicator(firstCodepoint) && Emoji.isRegionalIndicator(secondCodepoint))) {
            return line.substring(i, i+firstLen+secondLen);
        }
        return line.substring(i, i+firstLen);
    }


    public static void addWords(String chars, String name) {
        String nameString = name.replaceAll("[^A-Za-z]+", " ").toLowerCase(Locale.ENGLISH);
        for (String word : nameString.split(" ")) {
            if (!SKIP_WORDS.contains(word) && word.length() > 1 && getFlagCode(chars) == null) {
                NAME_TO_CHARS.add(word, chars);
            }
        }
    }

    static class Data implements Comparable<Data>{
        private static final String MISSING_CELL = "<td class='miss'>missing</td>\n";
        private static final String MISSING7_CELL = "<td class='miss7'>new 7.0</td>\n";
        final String chars;
        final String code;
        final UcdPropertyValues.Age_Values age;
        final String defaultPresentation;
        final Set<Label> labels;
        final String name;
        static final Relation<Label, Data> LABELS_TO_DATA 
        = Relation.of(new EnumMap(Label.class), TreeSet.class); // , BY_LABEL

        static final UnicodeSet DATA_CHARACTERS = new UnicodeSet();

        static final UnicodeSet missingJSource = new UnicodeSet(JSOURCES);
        static Map<String, Data> STRING_TO_DATA = new TreeMap<>();
        @Override
        public boolean equals(Object obj) {
            return chars.equals(((Data)obj).chars);
        }        
        @Override
        public int hashCode() {
            return chars.hashCode();
        }
        @Override
        public int compareTo(Data o) {
            //            int diff = age.compareTo(o.age);
            //            if (diff != 0) {
            //                return diff;
            //            }
            return CODEPOINT_COMPARE_SHORTER.compare(chars, o.chars);
        }

        public Data(String chars, String code, String age,
                String defaultPresentation, String name) {
            this.chars = chars;
            if (chars.contains(EMOJI_VARIANT_STRING) || chars.equals(TEXT_VARIANT_STRING)) {
                throw new IllegalArgumentException();
            }
            this.code = code;
            this.age = UcdPropertyValues.Age_Values.valueOf(age.replace('.', '_'));
            this.defaultPresentation = defaultPresentation;
            this.labels = storeLabels();
            this.name = getName(chars);
            addWords(chars, name);
            DATA_CHARACTERS.add(chars);
            for (Label label : labels) {
                LABELS_TO_DATA.put(label, this);
            }
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
            Set<Label> labels2 = Label.CHARS_TO_LABELS.get(chars); // override
            if (labels2 == null) {
                if (chars.equals("🇽🇰")) {
                    labels2 = Collections.singleton(Label.flag);
                } else {
                    labels2 = Collections.singleton(Label.sign);
                    System.out.println("*** No specific label for " + Utility.hex(chars) + " " + NAME.get(chars.codePointAt(0)));
                }
            }
            return Collections.unmodifiableSet(labels2);
        }

        static final Data parseLine(String line) {
            String[] items = tab.split(line);
            // U+2194   V1.1    text    arrows  ↔   LEFT RIGHT ARROW
            String code1 = items[0];
            String age1 = items[1];
            String defaultPresentation = items[2];
            String temp = items[3];
            if (temp.isEmpty()) {
                temp = "misc";
            }
            //            EnumSet labelSet = EnumSet.noneOf(Label.class);
            //            for (String label : Arrays.asList(space.split(temp))) {
            //                Label newLabel = Label.get(label);
            //                labelSet.add(newLabel);
            //            }

            String chars1 = items[4];
            if (EXCLUDE.contains(chars1)) {
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
                    + "\t" + getVersion() 
                    + "\t" + defaultPresentation
                    + "\t" + chars 
                    + "\t" + name;
        }
        private String getVersion() {
            return age.toString().replace('_', '.') + (JSOURCES.contains(chars) ? "*" : "");
        }

        public String toHtmlString(Form form, int item) {
            String missingCell = VERSION70.containsSome(chars) ? MISSING7_CELL : MISSING_CELL;
            String core = Emoji.buildFileName(chars,"_");

            String symbolaCell = getCell("ref", core, missingCell);
            //                    String symbolaCell = Emoji.isRegionalIndicator(chars.codePointAt(0))
            //                            ? getCell("ref", core, missingCell)
            //                            : "<td class='symb'>" + chars + "</td>\n";
            //                    getFlag(chars)
            //            if (symbolaChars == null) {
            //                symbolaChars = chars;
            //            }
            //            int firstCodepoint = chars.codePointAt(0);
            //            int firstLen = Character.charCount(firstCodepoint);
            //            int secondCodepoint = firstLen >= chars.length() ? 0 : chars.codePointAt(firstLen);

            //            String header = Default.ucd().getBlock(firstCodepoint).replace('_', ' ');
            //            String subhead = subheader.getSubheader(firstCodepoint);
            //            if (SKIP_BLOCKS.contains(header)) {
            //                header = "<i>" + subhead + "</i>";
            //            } else if (!header.equalsIgnoreCase(subhead)) {
            //                header += ": <i>" + subhead + "</i>";
            //            }

            String androidCell = getCell("android", core, missingCell);
            String appleCell = getCell("apple", core, missingCell);
            String windowsCell = getCell("windows", core, missingCell);
            String twitterCell = getCell("twitter", core, missingCell);

            //            String android = androidPng(firstCodepoint, secondCodepoint, true);
            //            String androidCell = missingCell;
            //            if (android != null && new File(IMAGES_OUTPUT_DIR, android).exists()) {
            //                if (secondCodepoint != 0) {
            //                    String secondString = androidPng(firstCodepoint, secondCodepoint, false);
            //                    if (secondString != null) {
            //                        android = secondString;
            //                    }
            //                }
            //                androidCell = "<td class='andr'><img alt='" + chars
            //                        + "' class='imga' src='images/" + android + "'></td>\n";
            //            }


            String browserCell =  "<td class='chars'>" + getEmojiVariant(chars, EMOJI_VARIANT_STRING) + "</td>\n";

            String textChars = getEmojiVariant(chars, TEXT_VARIANT_STRING);
            Set<String> annotations = ANNOTATIONS_TO_CHARS.getKeys(chars);
            if (annotations != null) {
                annotations = new LinkedHashSet(annotations);
                for (Label label : labels) {
                    annotations.remove(label.toString());
                }
            }
            //            String twitterCell = ! TWITTER_CHARS.contains(chars) ? missingCell
            //                    : "<td class='andr'>" +
            //                    "<img class='imga' alt='" + chars
            //                    + "' src='" +
            //                    TWITTER_URL.transform(chars) +
            //                    "'>" + "</td>\n";
            //            String appleCell = !GITHUB_APPLE_CHARS.contains(chars) && !APPLE_LOCAL.contains(chars)
            //                    ? missingCell
            //                            : "<td class='andr'>" +
            //                            "<img class='imga' alt='" + chars
            //                            + "' src='" +
            //                            APPLE_URL.transform(chars) +
            //                            "'>" + "</td>\n";
            //            String windowsCell = !WINDOWS_CHARS.contains(chars) ? missingCell : "<td class='andr'>" +
            //                    "<img class='imga' alt='" + chars
            //                    + "' src='" +
            //                    WINDOWS_URL.transform(chars) +
            //                    "'>" + "</td>\n";
            String anchor = getAnchor(code);
            return "<tr>"
            + "<td class='rchars'>" + item + "</td>\n"
            + "<td class='code'>" + getDoubleLink(anchor, code) + "</td>\n"
            + (form != Form.fullForm && form != Form.missingForm ? 
                    browserCell :
                        symbolaCell
                        + appleCell
                        + androidCell
                        + twitterCell
                        + windowsCell
                    )
                    //+ browserCell
                    + (form.compareTo(Form.shortForm) <= 0 ? "" : 
                        "<td class='name'>" + name + "</td>\n")
                        + "<td class='age'>" + getVersion() + "</td>\n"
                        + "<td class='default'>" + defaultPresentation + (!textChars.equals(chars) ? "*" : "") + "</td>\n"
                        + (form.compareTo(Form.shortForm) <= 0 ? "" : 
                            "<td class='name'>" 
                            + CollectionUtilities.join(labels, ", ")
                            + (annotations == null ? "" : ";<br>" + CollectionUtilities.join(annotations, ", "))
                            + "</td>\n"
                            //                          +  "<td class='name'>" + header + "</td>"
                                )
                                + "</tr>";
        }

        public String getCell(String type, String core, String missingCell) {
            String filename = getImageFilename(type, core);
            String androidCell = missingCell;
            if (filename != null && new File(IMAGES_OUTPUT_DIR, filename).exists()) {
                androidCell = "<td class='andr'><img alt='" + chars + "' class='imga' src='images/" + filename + "'></td>\n";
            }
            return androidCell;
        }
        public static String getImageFilename(String type, String core) {
            return type + "/" + type + "_" + core + ".png";
        }

        private String getAnchor(String code2) {
            return code.replace(" ", "-").replace("+", "").replace("U", "");
        }
        public static String toHtmlHeaderString(Form form) {
            boolean shortForm = form.compareTo(Form.shortForm) <= 0;
            return "<tr>"
            + "<th>Count</th>\n"
            + "<th class='rchars'>Code</th>\n"
            + (form != Form.fullForm && form != Form.missingForm 
            ? "<th class='cchars'>Browser</th>\n" : 
                "<th class='cchars'>B&amp;W*</th>\n"
                + "<th class='cchars'>Apple*</th>\n"
                + "<th class='cchars'>Android</th>\n"
                + "<th class='cchars'>Twitter</th>\n"
                + "<th class='cchars'>Windows</th>\n"
                    )
                    //+ "<th class='cchars'>Browser</th>\n"
                    + (shortForm ? "" : 
                        "<th>Name</th>\n"
                        + "<th>Version</th>\n"
                        + "<th>Default</th>\n"
                        + "<th>Labels; Annotations</th>\n"
                        //                        + "<th>Block: <i>Subhead</i></th>\n"
                            )
                            + "</tr>";
        }
    }

    public static int getResponseCode(String urlString) {
        try {
            URL u = new URL(urlString); 
            HttpURLConnection huc =  (HttpURLConnection)  u.openConnection(); 
            huc.setRequestMethod("GET"); 
            huc.connect(); 
            return huc.getResponseCode();
        } catch (Exception e) {
            return -1;
        }
    }
    static final UnicodeSet VERSION70 = VERSION.getSet(UcdPropertyValues.Age_Values.V7_0.toString());

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
        String cc = (char)(firstCodepoint - Emoji.FIRST_REGIONAL + 'A') 
                + ""
                + (char)(secondCodepoint - Emoji.FIRST_REGIONAL + 'A');
        //        String remapped = REMAP_FLAGS.get(cc);
        //        if (remapped != null) {
        //            cc = remapped;
        //        }
        //        if (REPLACEMENT_CHARACTER.equals(cc)) {
        //            return null;
        //        }
        return cc;
    }

    private static void addOldAnnotations() {
        for (String line : FileUtilities.in(GenerateEmoji.class, "oldEmojiAnnotations.txt")) {
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            // U+00AE   Registered symbol, Registered
            String[] fields = line.split("\t");
            int codepoint = Integer.parseInt(fields[0].substring(2), 16);
            String realChars = ANDROID_REMAP_VALUES.get(codepoint);
            if (realChars == null) {
                realChars = new StringBuilder().appendCodePoint(codepoint).toString();
            }
            if (NAME.get(realChars.codePointAt(0)) == null ) {
                System.out.println("skipping private use: " + Integer.toHexString(realChars.codePointAt(0)));
                continue;
            }
            addWords(realChars, fields[1]);
        }
    }

    static String getFlag(String chars) {
        String core = Emoji.buildFileName(chars,"_");
        String filename = Data.getImageFilename("ref", core);
        String cc = getFlagCode(chars);
        return cc == null ? null : "<img alt='" + chars
                + "' class='imgf' src='images/" + filename + 
                //"http//unicode.org/draft/reports/tr51/images/" + cc + ".png'" +
                "'>";
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
        for (String s : EXTRAS) {
            if (!Data.STRING_TO_DATA.containsKey(s)) {
                addNewItem(s, Data.STRING_TO_DATA);
                System.out.println(s);
            }
        }
        LinkedHashMap missingMap = new LinkedHashMap();
        addFileCodepoints(new File(IMAGES_OUTPUT_DIR), missingMap);
        UnicodeSet fileChars = new UnicodeSet().addAll(missingMap.keySet()).removeAll(Emoji.EMOJI_CHARS);
        System.out.println("MISSING: " + fileChars.toPattern(false));

        // show data
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
        //Data.missingJSource.removeAll(new UnicodeSet("[\\u2002\\u2003\\u2005]"));
        if (Data.missingJSource.size() > 0) {
            throw new IllegalArgumentException("Missing: " + Data.missingJSource);
        }
        //print(Form.imagesOnly, columns, Data.STRING_TO_DATA);
        //print(Form.shortForm, Data.STRING_TO_DATA);
        System.out.println(Data.LABELS_TO_DATA.keySet());
        
        print(Form.noImages, Data.STRING_TO_DATA);
        print(Form.fullForm, Data.STRING_TO_DATA);
        print(Form.missingForm, missingMap);

        showLabels();
        showSubhead();
        showAnnotations();
        showOtherUnicode();
        test();
        // check twitter glyphs

        if (true) {
            System.out.println("static final UnicodeSet EMOJI_CHARS = new UnicodeSet(\n\"" + Data.DATA_CHARACTERS.toPattern(false) + "\");");
            // getUrlCharacters("TWITTER", TWITTER_URL);
            //getUrlCharacters("APPLE", APPLE_URL);
        }
        System.out.println(new UnicodeSet(Emoji.GITHUB_APPLE_CHARS).removeAll(APPLE_CHARS).toPattern(false));
        System.out.println(list(new UnicodeSet(APPLE_CHARS).removeAll(Emoji.GITHUB_APPLE_CHARS)));
    }

    public static void addFileCodepoints(File imagesOutputDir, Map<String, Data> results) {
        for (File file : imagesOutputDir.listFiles()) {
            if (file.isDirectory()) {
                addFileCodepoints(file, results);
                continue;
            }
            String s = file.getName();
            String original = s;
            if (s.startsWith(".") || !s.endsWith(".png")) {
                continue;
            }
            String chars = Emoji.parseFileName(s, "_");
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
            int codePoint = Integer.parseInt(part,16);
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
        //http://emojistatic.github.io/images/32/1f4ab.png
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

    private static void showLabels() throws IOException {
        PrintWriter out = BagFormatter.openUTF8Writer(OUTPUT_DIR, "emoji-labels.html");
        writeHeader(out, "Emoji Labels", "Main categories for character picking.");
        for (Label l : Label.values()) {
            Set<Data> set = Data.LABELS_TO_DATA.get(l);
            if (set == null) {
                System.out.println("No chars for: " + l);
                continue;
            }
            out.println("<tr><td>" + getDoubleLink(l.toString(), l.toString()) + "</td></tr>\n" +
                    "<tr><td class='lchars'>");
            boolean first = true;
            for (Data data : set) {
                if (first) {
                    first = false;
                } else {
                    out.print("\n");
                }
                String flag = getFlag(data.chars);
                out.print("<span title='U+" + Utility.hex(data.chars, " U+") + " " + data.name.toLowerCase(Locale.ENGLISH) + "'>" 
                        + (flag == null ? getEmojiVariant(data.chars, EMOJI_VARIANT_STRING) : flag)
                        + "</span>");
            }
            out.println("</td></tr>");
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
        PrintWriter out = BagFormatter.openUTF8Writer(OUTPUT_DIR, "emoji-subhead.html");
        writeHeader(out, "Emoji Subhead", "Unicode Subhead mapping.");
        for (Entry<String, UnicodeSet> entry : subheadToChars.entrySet()) {
            String label = entry.getKey();
            UnicodeSet uset = entry.getValue();
            if (label.equalsIgnoreCase("exclude")) {
                continue;
            }
            displayUnicodeset(out, label, null, uset, true);
        }
        writeFooter(out);
        out.close();
    }


    private static void showAnnotations() throws IOException {
        PrintWriter out = BagFormatter.openUTF8Writer(OUTPUT_DIR, "emoji-annotations.html");
        writeHeader(out, "Emoji Annotations", "Finer-grained character annotations. " +
                "For brevity, flags are not shown: they would have names of the associated countries.");

        Set<String> skip = new HashSet<>();
        for (Entry<String, Set<String>> entry : ANNOTATIONS_TO_CHARS.keyValuesSet()) {
            String word = entry.getKey();
            Set<String> values = entry.getValue();
            UnicodeSet uset = new UnicodeSet().addAll(values);

            displayUnicodeset(out, "*" + word, null, uset, true);
        }
        writeFooter(out);
        out.close();
    }


    static final UnicodeSet EXCLUDE_SET = new UnicodeSet()
    .addAll(GENERAL_CATEGORY.getSet(General_Category_Values.Unassigned.toString()))
    .addAll(GENERAL_CATEGORY.getSet(General_Category_Values.Private_Use.toString()))
    .addAll(GENERAL_CATEGORY.getSet(General_Category_Values.Surrogate.toString()))
    .addAll(GENERAL_CATEGORY.getSet(General_Category_Values.Control.toString()))
    .addAll(GENERAL_CATEGORY.getSet(General_Category_Values.Nonspacing_Mark.toString()))
    ;

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

        PrintWriter out = BagFormatter.openUTF8Writer(OUTPUT_DIR, "other-labels.html");
        writeHeader(out, "Other Labels", "Draft categories for other Symbols and Punctuation.");

        for (Entry<String, UnicodeSet> entry : labelToUnicodeSet.entrySet()) {
            String label = entry.getKey();
            UnicodeSet uset = entry.getValue();
            if (label.equalsIgnoreCase("exclude")) {
                continue;
            }
            displayUnicodeset(out, label, null, uset, false);
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
                if (!ASCII_LETTER_HYPHEN.containsAll(line)) {
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

    public static void displayUnicodeset(PrintWriter out, String label,
            String sublabel, UnicodeSet uset, boolean showEmoji) {
        out.println("<tr><td>" + getDoubleLink(label, label) + "</td>");
        if (sublabel != null) {
            out.println("<tr><td>" + sublabel + "</td>");
        }
        out.println("<td class='lchars'>");
        System.out.println(label + "\t" + uset.size());
        Set<String> sorted = uset.addAllTo(new TreeSet(CODEPOINT_COMPARE_SHORTER));
        boolean first = true;
        for (String s : sorted) {
            if (first) {
                first = false;
            } else {
                out.print("\n");
            }
            String chars = s;
            if (showEmoji) {
                chars = getFlag(s);
                if (chars == null) {
                    chars = getEmojiVariant(s, EMOJI_VARIANT_STRING);
                }
            }
            out.print("<span title='U+" + Utility.hex(s, " U+") + " " + getName(s) + "'>" 
                    + chars
                    + "</span>");
        }
        out.println("</td></tr>");
    }

    static final LocaleDisplayNames LOCALE_DISPLAY = LocaleDisplayNames.getInstance(ULocale.ENGLISH);

    public static String getName(String s) {
        final String name = NAME.get(s.codePointAt(0));
        if (s.indexOf(ENCLOSING_KEYCAP) >= 0) {
            return "keycap " + name.toLowerCase(Locale.ENGLISH);
        }
        String flag = getFlagCode(s);
        if (flag != null) {
            String result = LOCALE_DISPLAY.regionDisplayName(flag);
            if (result.endsWith(" SAR China")) {
                result = result.substring(0, result.length() - " SAR China".length());
            }
            return "flag for " + result;
        }
        return name == null ? "UNNAMED" : name.toLowerCase(Locale.ENGLISH);
    }


    public static void addNewItem(String s, Map<String, Data> missingMap) {
        if (!Data.DATA_CHARACTERS.contains(s)) {
            addNewItem(new Data(s), missingMap);
        }
    }

    public static void addNewItem(Data item, Map<String, Data> missingMap) {
        if (item == null || EXCLUDE.contains(item.chars)) {
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
        noImages("", " without images."), 
        fullForm("full", "with images."), 
        missingForm("missing", " with images; have icons but are not including.");
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

    public static <T> void print(Form form, Map<String, Data> set) throws IOException {
        PrintWriter out = BagFormatter.openUTF8Writer(OUTPUT_DIR, 
                form.filePrefix + "emoji-list.html");
        writeHeader(out, form.title, "List of emoji characters, " + form.description);
        out.println(Data.toHtmlHeaderString(form));
        int item = 0;
        for (Data data : new TreeSet<Data>(set.values())) {
            out.println(data.toHtmlString(form, ++item));
        }
        writeFooter(out);
        out.close();
    }

    public static void writeFooter(PrintWriter out) {
        out.println("</table></body></html>");
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
                " For more information, see <a target='_blank' href='http://unicode.org/draft/reports/tr51/tr51.html'>Unicode Emoji</a>.</p>\n"
                + "<table>");
    }

    static boolean CHECKFACE = false;

    static void test() throws IOException {
        PrintWriter out = BagFormatter.openUTF8Writer(OUTPUT_DIR, "emoji-diff.html");
        writeHeader(out, "Diff List", "Differences from other categories.");

        UnicodeSet AnimalPlantFood = new UnicodeSet("[☕ 🌰-🌵 🌷-🍼 🎂 🐀-🐾]");
        testEquals(out, "AnimalPlantFood", AnimalPlantFood, Label.nature, Label.food);

        UnicodeSet Object = new UnicodeSet("[⌚ ⌛ ⏰ ⏳ ☎ ⚓ ✂ ✉ ✏ 🎀 🎁 👑-👣 💄 💉 💊 💌-💎 💐 💠 💡 💣 💮 💰-📷 📹-📼 🔋-🔗 🔦-🔮 🕐-🕧]");
        testEquals(out, "Object", Object, Label.object, Label.office, Label.clothing);

        CHECKFACE=true;
        UnicodeSet PeopleEmotion = new UnicodeSet("[☝ ☺ ✊-✌ ❤ 👀 👂-👐 👤-💃 💅-💇 💋 💏 💑 💓-💟 💢-💭 😀-🙀 🙅-🙏]");
        testEquals(out, "PeopleEmotion", PeopleEmotion, Label.people, Label.body, Label.emotion, Label.face);
        CHECKFACE=false;

        UnicodeSet SportsCelebrationActivity = new UnicodeSet("[⛑ ⛷ ⛹ ♠-♧ ⚽ ⚾ 🀀-🀫 🂠-🂮 🂱-🂾 🃁-🃏 🃑-🃟 🎃-🎓 🎠-🏄 🏆-🏊 💒]");
        testEquals(out, "SportsCelebrationActivity", SportsCelebrationActivity, Label.game, Label.sport, Label.activity);

        UnicodeSet TransportMapSignage = new UnicodeSet("[♨ ♻ ♿ ⚠ ⚡ ⛏-⛡ ⛨-⛿ 🏠-🏰 💈 🗻-🗿 🚀-🛅]");
        testEquals(out, "TransportMapSignage", TransportMapSignage, Label.travel, Label.place);

        UnicodeSet WeatherSceneZodiacal = new UnicodeSet("[☀-☍ ☔ ♈-♓ ⛄-⛈ ⛎ ✨ 🌀-🌠 🔥]");
        testEquals(out, "WeatherSceneZodiacal", WeatherSceneZodiacal, Label.weather, Label.time);

        UnicodeSet Enclosed = new UnicodeSet("[[\u24C2\u3297\u3299][\\U0001F150-\\U0001F19A][\\U0001F200-\\U0001F202][\\U0001F210-\\U0001F23A][\\U0001F240-\\U0001F248][\\U0001F250-\\U0001F251]]");
        testEquals(out, "Enclosed", Enclosed, Label.word);

        UnicodeSet Symbols = new UnicodeSet("[[\\U0001F4AF][\\U0001F500-\\U0001F525][\\U0001F52F-\\U0001F53D][\\U0001F540-\\U0001F543[\u00A9\u00AE\u2002\u2003\u2005\u203C\u2049\u2122\u2139\u2194\u2195\u2196\u2197\u2198\u2199\u21A9\u21AA\u231B\u23E9\u23EA\u23EB\u23EC\u25AA\u25AB\u25B6\u25C0\u25FB\u25FC\u25FD\u25FE\u2611\u2660\u2663\u2665\u2666\u267B\u2693\u26AA\u26AB\u2705\u2708\u2712\u2714\u2716\u2728\u2733\u2734\u2744\u2747\u274C\u274E\u2753\u2754\u2755\u2757\u2764\u2795\u2796\u2797\u27A1\u27B0\u2934\u2935\u2B05\u2B06\u2B07\u2B1B\u2B1C\u2B50\u2B55\u3030\u303D]]]");
        testEquals(out, "Symbols", Symbols, Label.sign);

        UnicodeSet other = new UnicodeSet(get70(Label.values()))
        .removeAll(AnimalPlantFood)
        .removeAll(Object)
        .removeAll(PeopleEmotion)
        .removeAll(SportsCelebrationActivity)
        .removeAll(TransportMapSignage)
        .removeAll(WeatherSceneZodiacal)
        .removeAll(Enclosed)
        .removeAll(Symbols)
        ;

        testEquals(out, "Other", other, Label.flag, Label.sign, Label.arrow);

        UnicodeSet ApplePeople = new UnicodeSet("[☝☺✊-✌✨❤🌂🌟🎀🎩🎽🏃👀👂-👺👼👽 👿-💇💋-💏💑💓-💜💞💢💤-💭💼🔥😀-🙀🙅-🙏 🚶]");
        testEquals(out, "ApplePeople", ApplePeople, Label.people, Label.emotion, Label.face, Label.body, Label.clothing);

        UnicodeSet AppleNature = new UnicodeSet("[☀☁☔⚡⛄⛅❄⭐🌀🌁🌈🌊-🌕🌙-🌞🌠🌰-🌵 🌷-🌼🌾-🍄🐀-🐾💐💩]");
        testEquals(out, "AppleNature", AppleNature, Label.nature, Label.food, Label.weather);

        UnicodeSet ApplePlaces = new UnicodeSet("[♨⚓⚠⛪⛲⛵⛺⛽✈🇧-🇬🇮-🇰🇳🇵🇷-🇺 🌃-🌇🌉🎠-🎢🎪🎫🎭🎰🏠-🏦🏨-🏰💈💒💺📍 🔰🗻-🗿🚀-🚝🚟-🚩🚲]");
        testEquals(out, "ApplePlaces", ApplePlaces, Label.place, Label.travel);

        UnicodeSet AppleSymbols = new UnicodeSet("[©®‼⁉⃣™ℹ↔-↙↩↪⏩-⏬ Ⓜ▪▫▶◀◻-◾☑♈-♓♠♣♥♦♻♿⚪⚫⛎ ⛔✅✔✖✳✴❇❌❎❓-❕❗➕-➗➡➰➿⤴⤵ ⬅-⬇⬛⬜⭕〰〽㊗㊙🅰🅱🅾🅿🆎🆑-🆚🈁🈂🈚 🈯🈲-🈺🉐🉑🌟🎦🏧👊👌👎💙💛💟💠💢💮💯💱💲 💹📳-📶🔀-🔄🔗-🔤🔯🔱-🔽🕐-🕧🚫🚭-🚱 🚳🚷-🚼🚾🛂-🛅]");
        testEquals(out, "AppleSymbols", AppleSymbols, Label.sign, Label.game);

        UnicodeSet AppleTextOrEmoji = new UnicodeSet("[‼⁉ℹ↔-↙↩↪Ⓜ▪▫▶◀◻-◾☀☁☎ ☑☔☕☝☺♈-♓♠♣♥♦♨♻♿⚓⚠⚡⚪⚫⚰ ⚾✂✈✉✌✏✒✳✴❄❇❤➡⤴⤵⬅-⬇〽㊗㊙ 🅰🅱🅾🅿🈂🈷🔝{#⃣}{0⃣}{1⃣}{2 ⃣}{3⃣}{4⃣}{5⃣}{6⃣}{7⃣}{8 ⃣}{9⃣}{🇨🇳}{🇩🇪}{🇪🇸}{🇫🇷}{🇬🇧}{ 🇮🇹}{🇯🇵}{🇰🇷}{🇷🇺}{🇺🇸}]");
        UnicodeSet AppleOnlyEmoji = new UnicodeSet("[⌚⌛⏩-⏬⏰⏳⚽⛄⛅⛎⛔⛪⛲⛳⛵⛺⛽✅ ✊✋✨❌❎❓-❕❗➿⬛⬜⭐⭕🀄🃏🆎🆑-🆚🈁 🈚🈯🈲-🈶🈸-🈺🉐🉑🌀-🌠🌰-🌵🌷-🍼🎀-🎓 🎠-🏊🏠-🏰🐀-🐾👀👂-📷📹-📼🔀-🔘🔞-🔽 🕐-🕧🗻-🙀🙅-🙏🚀-🛅]");

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
            Set<String> sorted = new TreeSet<String>(CODEPOINT_COMPARE_SHORTER);
            missing.addAllTo(sorted);
            for (String s : sorted) {
                if (first) {
                    first = false;
                } else {
                    out.print("\n");
                }
                out.print("<span title='" + Default.ucd().getName(s) + "'>" 
                        + getEmojiVariant(s, EMOJI_VARIANT_STRING) 
                        + "</span>");
            }            
            out.println("</td></tr>");
        }
    }

    public static UnicodeSet get70(Label... labels) {
        UnicodeSet containee = new UnicodeSet();
        for (Label label : labels) {
            for (Data data : Data.LABELS_TO_DATA.get(label)) {
                containee.addAll(data.chars);
            }
        }
        containee.removeAll(VERSION70);
        //containee.retainAll(JSOURCES);
        return containee;
    }

    public static String getDoubleLink(String anchor, String code) {
        anchor = anchor.replace(' ', '_').toLowerCase(Locale.ENGLISH);
        return "<a href='#" + anchor + "' name='" + anchor + "'>" + code + "</a>";
    }
}
