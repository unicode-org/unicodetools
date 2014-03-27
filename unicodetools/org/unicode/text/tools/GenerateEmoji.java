package org.unicode.text.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.jsp.Subheader;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;
import org.unicode.text.UCD.Default;
import org.unicode.text.tools.GenerateEmoji.Data;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Row;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UTF16.StringComparator;
import com.ibm.icu.text.UnicodeSet;

public class GenerateEmoji {
    private static final String OUTPUT_DIR = "/Users/markdavis/workspace/unicode-draft/reports/tr51/";
    private static final String IMAGES_OUTPUT_DIR = OUTPUT_DIR + "images/";
    static final Pattern tab = Pattern.compile("\t");
    static final Pattern space = Pattern.compile(" ");
    static final char EMOJI_VARIANT = '\uFE0F';
    static final char TEXT_VARIANT = '\uFE0F';
    static final IndexUnicodeProperties LATEST = IndexUnicodeProperties.make(Default.ucdVersion());
    static final UnicodeMap<String> STANDARDIZED_VARIANT = LATEST.load(UcdProperty.Standardized_Variant);
    static final UnicodeMap<String> VERSION = LATEST.load(UcdProperty.Age);
    static final UnicodeMap<String> WHITESPACE = LATEST.load(UcdProperty.White_Space);
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
    enum CharColumn {some, all}

    static final Subheader subheader = new Subheader("/Users/markdavis/workspace/unicodetools/data/ucd/7.0.0-Update/");
    static final Set<String> SKIP_BLOCKS = new HashSet(Arrays.asList("Miscellaneous Symbols", 
            "Enclosed Alphanumeric Supplement", 
            "Miscellaneous Symbols And Pictographs",
            "Miscellaneous Symbols And Arrows"));

    enum Label {
        people, body, face, nature, clothing, emotion, 
        food, travel, place, office,
        time, weather, game, sport, object,
        sound, 
        flag,    
        arrow,
        letter,
        misc, 
        //unknown,
        ;

        static Label get(String string) {
            return Label.valueOf(string);
        }
        static final Relation<String, Label> CHARS_TO_LABELS 
        = Relation.of(new TreeMap(), TreeSet.class);

        static {
            Label lastLabel = null;
            for (String line : FileUtilities.in(GenerateEmoji.class, "emojiLabels.txt")) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                char first = line.charAt(0);
                if ('a' <= first && first <= 'z') {
                    lastLabel = Label.get(line);
                } else {
                    for (int i = 0; i < line.length();) {
                        String string = getEmojiSequence(line, i);
                        i += string.length();
                        if (string.equals(" ")) {
                            continue;
                        }
                        CHARS_TO_LABELS.put(string, lastLabel);
                    }
                }
            }
            // remove misc
            for (Entry<String, Set<Label>> entry : CHARS_TO_LABELS.keyValuesSet()) {
                Set<Label> set = entry.getValue();
                if (set.contains(Label.misc) && set.size() > 1) {
                    CHARS_TO_LABELS.remove(entry.getKey(), Label.misc);
                }
            }
            CHARS_TO_LABELS.freeze();
            int i = 0;
            for (Entry<String, Set<Label>> entry : CHARS_TO_LABELS.keyValuesSet()) {
                System.out.println(i++ + "\t" + entry.getKey() + "\t" + entry.getValue());
            }
        }

        private static String getEmojiSequence(String line, int i) {
            int firstCodepoint = line.codePointAt(i);
            int firstLen = Character.charCount(firstCodepoint);
            if (i + firstLen == line.length()) {
                return line.substring(i, i+firstLen);
            }
            int secondCodepoint = line.codePointAt(i+firstLen);
            int secondLen = Character.charCount(secondCodepoint);
            if (secondCodepoint == 0x20E3 // special case
                    || (0x1F1E6 <= firstCodepoint && firstCodepoint <= 0x1F1FF
                    && 0x1F1E6 <= secondCodepoint && secondCodepoint <= 0x1F1FF)) {
                return line.substring(i, i+firstLen+secondLen);
            }
            return line.substring(i, i+firstLen);
        }
    }
    static final StringComparator CODEPOINT_COMPARE = new UTF16.StringComparator(true,false,0);

    static class Data implements Comparable<Data>{
        private static final String REPLACEMENT_CHARACTER = "\uFFFD";
        final String chars;
        final String code;
        final UcdPropertyValues.Age_Values age;
        final String defaultPresentation;
        final Set<Label> labels;
        final String name;
        static final Relation<Label, Data> LABELS_TO_DATA 
        = Relation.of(new EnumMap(Label.class), TreeSet.class); // , BY_LABEL

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
            int diff = age.compareTo(o.age);
            if (diff != 0) {
                return diff;
            }
            return CODEPOINT_COMPARE.compare(chars, o.chars);
        }

        public Data(String chars, String code, String age,
                String defaultPresentation, String name) {
            this.chars = chars;
            this.code = code;
            this.age = UcdPropertyValues.Age_Values.valueOf(age.replace('.', '_'));
            this.defaultPresentation = defaultPresentation;
            this.labels = storeLabels();
            this.name = name;
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
            if (labels2 == null && chars.equals("🇽🇰")) {
                labels2 = Collections.singleton(Label.flag);
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
            missingJSource.remove(data.chars);
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
        static final Map<String,String> remap = new HashMap();
        static {
            remap.put("BL", "FR");
            remap.put("BV", "NO");
            remap.put("GF", "FR");
            remap.put("HM", "AU");
            remap.put("MF", "FR");
            remap.put("RE", "FR");
            remap.put("SJ", "NO");
            remap.put("TF", "FR");
            remap.put("UM", "US");
            remap.put("WF", "FR");
            remap.put("YT", "FR");
            //            remap.put("AQ", REPLACEMENT_CHARACTER);
            //            remap.put("AX", REPLACEMENT_CHARACTER);
            //            remap.put("CC", REPLACEMENT_CHARACTER);
            //            remap.put("CX", REPLACEMENT_CHARACTER);
            //            remap.put("EH", REPLACEMENT_CHARACTER);
            //            remap.put("GG", REPLACEMENT_CHARACTER);
            //            remap.put("GS", REPLACEMENT_CHARACTER);
            //            remap.put("IM", REPLACEMENT_CHARACTER);
            //            remap.put("IO", REPLACEMENT_CHARACTER);
            //            remap.put("JE", REPLACEMENT_CHARACTER);
            //            remap.put("PN", REPLACEMENT_CHARACTER);
        }

        public String toHtmlString(Form form, Set<CharColumn> columns) {
            String symbolaChars = chars;
            int firstCodepoint = chars.codePointAt(0);
            int firstLen = Character.charCount(firstCodepoint);
            int secondCodepoint = firstLen >= chars.length() ? 0 : chars.codePointAt(firstLen);
            if (0x1F1E6 <= firstCodepoint && firstCodepoint <= 0x1F1FF && secondCodepoint != 0) {
                secondCodepoint = chars.codePointAt(2);
                String cc = (char)(firstCodepoint - 0x1F1E6 + 'A') 
                        + ""
                        + (char)(secondCodepoint - 0x1F1E6 + 'A');
                String remapped = remap.get(cc);
                if (remapped != null) {
                    cc = remapped;
                }
                if (REPLACEMENT_CHARACTER.equals(cc)) {
                    symbolaChars = REPLACEMENT_CHARACTER;
                } else {
                    symbolaChars = "<img src='images/" + cc + ".png'>";
                }
            }

            String header = Default.ucd().getBlock(firstCodepoint).replace('_', ' ');
            String subhead = subheader.getSubheader(firstCodepoint);
            if (SKIP_BLOCKS.contains(header)) {
                header = "<i>" + subhead + "</i>";
            } else if (!header.equalsIgnoreCase(subhead)) {
                header += ": <i>" + subhead + "</i>";
            }
            String android = androidPng(firstCodepoint, secondCodepoint, true);
            String androidChars = "<i>n/a</i>";
            if (new File(IMAGES_OUTPUT_DIR, android).exists()) {
                androidChars = "<img class='imga' src='images/" + android + "'>";
                if (secondCodepoint != 0) {
                    String secondString = androidPng(firstCodepoint, secondCodepoint, false);
                    if (secondString != null) {
                        androidChars += "<img class='imga' src='images/" + secondString + "'>";
                    }
                }
            }
            String browserChars = getEmojiVariant(chars);
            String textChars = chars;
            boolean hasTextVariant = STANDARDIZED_VARIANT.get(chars + TEXT_VARIANT) != null;
            if (hasTextVariant) {
                textChars = chars + TEXT_VARIANT;
            }

            return "<tr>" +
            (form == Form.imagesOnly ? "" : 
                "</td><td class='age'>" + getVersion()
                + "<td class='code'>" + code
                    )
                    + "</td><td class='symb'>" + symbolaChars 
                    + "</td><td class='chars'>" + browserChars 
                    + (columns.contains(CharColumn.some) ? "" : 
                        "</td><td class='segoe'>" + textChars 
                        + "</td><td class='andr'>" + androidChars )
                        + (form.compareTo(Form.shortForm) <= 0 ? "" : "</td><td class='name'>" + name)
                        + (form.compareTo(Form.shortForm) <= 0 ? "" : "</td><td class='default'>" + defaultPresentation
                                + (hasTextVariant ? "*" : ""))
                        + (form.compareTo(Form.shortForm) <= 0 ? "" : "</td><td class='name'>" 
                                + CollectionUtilities.join(labels, ", ")
                                + "</td><td class='name'>" + header)
                                + "</td></tr>";
        }
        public static String toHtmlHeaderString(Form form, Set<CharColumn> columns) {
            boolean shortForm = form.compareTo(Form.shortForm) <= 0;
            return "<tr>" +
            (form == Form.imagesOnly ? "" : 
                "<th>Version</th>"
                + "<th>Code</th>"
                    ) +
                    "<th>Symbola*</th>" +
                    "<th>Browser</th>" +
                    (columns.contains(CharColumn.some) ? "" : "<th>Segoe</th>" +
                            "<th>Android</th>") +
                            (shortForm ? "" : "<th>Name</th>") +
                            (shortForm ? "" : "<th>Default</th>") +
                            (shortForm ? "" : "<th>Labels*</th>" +
                                    "<th>Block: <i>Subhead</i></th>") +
                                    "</tr>";
        }
        public static String getEmojiVariant(String browserChars) {
            if (STANDARDIZED_VARIANT.get(browserChars + EMOJI_VARIANT) != null) {
                browserChars = browserChars + EMOJI_VARIANT;
            }
            return browserChars;
        }

        static final Set<String> ANDROID_IMAGES = new TreeSet<>();

        static final Map<Row.R2<Integer, Integer>,Integer> ANDROID_REMAP = new HashMap<>();
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
                addAndroidRemap((char)('0' + i) + "\u20E3", 0xFE82D + i); // 1 => U+FE82E
            }
            addAndroidRemap("0⃣", 0xFE837);
        }
        public static Integer addAndroidRemap(String real, int replacement) {
            int first = real.codePointAt(0);
            return ANDROID_REMAP.put(Row.of(first, real.codePointAt(Character.charCount(first))), replacement);
        }

        public String androidPng(int firstCodepoint, int secondCodepoint, boolean first) {
            if (secondCodepoint == 0x20e3) {
                int debug = 0;
            }
            if (secondCodepoint != 0) {
                Integer remapped = ANDROID_REMAP.get(Row.of(firstCodepoint, secondCodepoint));
                if (remapped != null) {
                    if (!first) {
                        return null;
                    }
                    firstCodepoint = remapped;
                }
            }
            String filename = "emoji_u" + Utility.hex(first ? firstCodepoint : secondCodepoint).toLowerCase() + ".png";
            ANDROID_IMAGES.add(filename);
            return filename;
        }

    }

    //    static final Comparator<Data> BY_LABEL = new Comparator<Data> () {
    //        public int compare(Data o1, Data o2) {
    //            Iterator<Label> i1 = o1.labels.iterator();
    //            Iterator<Label> i2 = o2.labels.iterator();
    //            while (true) {
    //                Label l1 = i1.hasNext() ? i1.next() : null;
    //                Label l2 = i2.hasNext() ? i2.next() : null;
    //                if (l1 == null) {
    //                    if (l2 != null) {
    //                        return -1;
    //                    }
    //                    int diff = 0; // o1.name.compareTo(o2.name);
    //                    return diff != 0 ? diff : o1.compareTo(o2);
    //                } else if (l2 == null) {
    //                    return 1;
    //                }
    //            }
    //        }
    //    };
    static final UnicodeSet VERSION70 = VERSION.getSet(UcdPropertyValues.Age_Values.V7_0.toString());

    public static void main(String[] args) throws IOException {
        for (String line : FileUtilities.in(GenerateEmoji.class, "emojiData.txt")) {
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            Data.add(line);
        }
        for (String s : new UnicodeSet("[\\u2714\\u2716\\u303D\\u3030 \\u00A9 \\u00AE \\u2795-\\u2797 \\u27B0 \\U0001F519-\\U0001F51C {🇽🇰}]")) {
            addNewItem(new Data(s), Data.STRING_TO_DATA);
            Data.missingJSource.remove(s);
            System.out.println(s);
        }
        test();

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
        Set<CharColumn> columns = EnumSet.of(CharColumn.some);
        //print(Form.imagesOnly, columns, Data.STRING_TO_DATA);
        print(Form.shortForm, columns, Data.STRING_TO_DATA);
        System.out.println(Data.LABELS_TO_DATA.keySet());
        print(Form.longForm, columns, Data.STRING_TO_DATA);

        LinkedHashMap missingMap = new LinkedHashMap();

        for (String s : new File(IMAGES_OUTPUT_DIR).list()) {
            if (Data.ANDROID_IMAGES.contains(s) || s.startsWith(".") || s.length() == 6) {
                continue;
            }
            // emoji_u1f5c3.png
            if (!s.startsWith("emoji_u") || !s.endsWith(".png")) {
                throw new IllegalArgumentException(s);
            }
            String code1 = s.substring(7,s.length()-4);
            int codepoint = Integer.parseInt(code1,16);
            if (codepoint == 0x20e3 || codepoint >= 0xF0000) {
                continue;
            }
            addNewItem(new Data(codepoint), missingMap);
        }
        print(Form.missingForm, columns, missingMap);

        showLabels();
    }

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
        writeHeader(out, "Draft Emoji Labels");
        for (Label l : Label.values()) {
            Set<Data> set = Data.LABELS_TO_DATA.get(l);
            if (set == null) {
                System.out.println("No chars for: " + l);
                continue;
            }
            out.println("<tr><td>" + l + "</td><td class='chars'>");
            boolean first = true;
            for (Data data : set) {
                if (first) {
                    first = false;
                } else {
                    out.println(" ");
                }
                out.print("<span title='" + data.name.toLowerCase() + "'>" 
                        + Data.getEmojiVariant(data.chars) 
                        + "</span>");
            }
            out.println("</td></tr>");
        }
        writeFooter(out);
        out.close();
    }

    public static void addNewItem(Data item, Map<String, Data> missingMap) {
        if (missingMap.containsKey(item.chars)) {
            throw new IllegalArgumentException(item.toString());
        }
        missingMap.put(item.chars, item);
    }

    enum Form {
        imagesOnly("images-"), 
        shortForm("short-"), 
        longForm(""), 
        missingForm("missing-");
        final String filePrefix;
        Form(String prefix) {
            filePrefix = prefix;
        }
    }

    public static <T> void print(Form form, Set<CharColumn> columns, Map<String, Data> set) throws IOException {
        PrintWriter out = BagFormatter.openUTF8Writer(OUTPUT_DIR, 
                form.filePrefix + "emoji-list.html");
        writeHeader(out, "Draft Emoji List");
        out.println(Data.toHtmlHeaderString(form, columns));
        for (Data data : new TreeSet<Data>(set.values())) {
            out.println(data.toHtmlString(form, columns));
        }
        writeFooter(out);
        out.close();
    }

    public static void writeFooter(PrintWriter out) {
        out.println("</table></body></html>");
    }

    public static void writeHeader(PrintWriter out, String title) {
        out.println("<html><head><meta http-equiv='Content-Type' content='text/html; charset=UTF-8'/>\n" +
                "<link rel='stylesheet' type='text/css' href='emoji-list.css'>\n" +
                "<title>" +
                title +
                "</title>\n" +
                "</head><body><table>");
    }

    static void test() {
        UnicodeSet AnimalPlantFood = new UnicodeSet("[☕ 🌰-🌵 🌷-🍼 🎂 🐀-🐾]");
        UnicodeSet Object = new UnicodeSet("[⌚ ⌛ ⏰ ⏳ ☎ ⚓ ✂ ✉ ✏ 🎀 🎁 👑-👣 💄 💉 💊 💌-💎 💐 💠 💡 💣 💮 💰-📷 📹-📼 🔋-🔗 🔦-🔮 🕐-🕧]");
        UnicodeSet PeopleEmotion = new UnicodeSet("[☝ ☺ ✊-✌ ❤ 👀 👂-👐 👤-💃 💅-💇 💋 💏 💑 💓-💟 💢-💭 😀-🙀 🙅-🙏]");
        UnicodeSet SportsCelebrationActivity = new UnicodeSet("[♠-♧ ⚽ ⚾ 🀀-🀫 🂠-🂮 🂱-🂾 🃁-🃏 🃑-🃟 🎃-🎓 🎠-🏄 🏆-🏊 💒]");
        UnicodeSet TransportMapSignage = new UnicodeSet("[♨ ♻ ♿ ⚠ ⚡ ⛏-⛡ ⛨-⛿ 🏠-🏰 💈 🗻-🗿 🚀-🛅]");
        UnicodeSet WeatherSceneZodiacal = new UnicodeSet("[☀-☍ ☔ ♈-♓ ⛄-⛈ ⛎ ✨ 🌀-🌠 🔥]");
        UnicodeSet Enclosed = new UnicodeSet("[[\u24C2\u3297\u3299][\\U0001F150-\\U0001F19A][\\U0001F200-\\U0001F202][\\U0001F210-\\U0001F23A][\\U0001F240-\\U0001F248][\\U0001F250-\\U0001F251]]");
        UnicodeSet labelNatureFood = get70(Label.nature, Label.food);
        testEquals("AnimalPlantFood", AnimalPlantFood, "labelNatureFood", labelNatureFood);
        testEquals("Object", AnimalPlantFood, "object", get70(Label.object));
    }

    public static void testEquals(String title1, UnicodeSet AnimalPlantFood, 
            String title2, UnicodeSet labelNatureFood) {
        testContains(title1, AnimalPlantFood, title2, labelNatureFood);
        testContains(title2, labelNatureFood, title1, AnimalPlantFood);
    }

    private static void testContains(String title, UnicodeSet container, String title2, UnicodeSet containee) {
        if (!container.containsAll(containee)) {
            System.out.println(title + " doesn't contain " + title2 + 
                    ":\t" + new UnicodeSet(containee).removeAll(container).toPattern(false));
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
        return containee;
    }
}
