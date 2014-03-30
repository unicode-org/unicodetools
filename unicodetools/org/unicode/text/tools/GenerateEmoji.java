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
import org.unicode.props.UcdPropertyValues.Binary;
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
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UTF16.StringComparator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class GenerateEmoji {
    private static final String OUTPUT_DIR = "/Users/markdavis/workspace/unicode-draft/reports/tr51/";
    private static final String IMAGES_OUTPUT_DIR = OUTPUT_DIR + "images/";
    static final Pattern tab = Pattern.compile("\t");
    static final Pattern space = Pattern.compile(" ");
    static final char EMOJI_VARIANT = '\uFE0F';
    static final String EMOJI_VARIANT_STRING = String.valueOf(EMOJI_VARIANT);
    static final char TEXT_VARIANT = '\uFE0F';
    static final String REPLACEMENT_CHARACTER = "\uFFFD";
    static final String TEXT_VARIANT_STRING = String.valueOf(TEXT_VARIANT);
    static final UnicodeSet EXCLUDE = new UnicodeSet("[⛤-⛧ \\U0001F544-\\U0001F549]");

    static final IndexUnicodeProperties LATEST = IndexUnicodeProperties.make(Default.ucdVersion());
    static final UnicodeMap<String> STANDARDIZED_VARIANT = LATEST.load(UcdProperty.Standardized_Variant);
    static final UnicodeMap<String> VERSION = LATEST.load(UcdProperty.Age);
    static final UnicodeMap<String> WHITESPACE = LATEST.load(UcdProperty.White_Space);
    static final UnicodeMap<String> GENERAL_CATEGORY = LATEST.load(UcdProperty.General_Category);
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
    static final Map<String,String> REMAP_FLAGS = new HashMap();
    static {
        REMAP_FLAGS.put("BL", "FR");
        REMAP_FLAGS.put("BV", "NO");
        REMAP_FLAGS.put("GF", "FR");
        REMAP_FLAGS.put("HM", "AU");
        REMAP_FLAGS.put("MF", "FR");
        REMAP_FLAGS.put("RE", "FR");
        REMAP_FLAGS.put("SJ", "NO");
        REMAP_FLAGS.put("TF", "FR");
        REMAP_FLAGS.put("UM", "US");
        REMAP_FLAGS.put("WF", "FR");
        REMAP_FLAGS.put("YT", "FR");
    }
    static final Subheader subheader = new Subheader("/Users/markdavis/workspace/unicodetools/data/ucd/7.0.0-Update/");
    static final Set<String> SKIP_BLOCKS = new HashSet(Arrays.asList("Miscellaneous Symbols", 
            "Enclosed Alphanumeric Supplement", 
            "Miscellaneous Symbols And Pictographs",
            "Miscellaneous Symbols And Arrows"));

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
                        if (string.equals(" ") 
                                || string.equals(EMOJI_VARIANT_STRING) 
                                || string.equals(TEXT_VARIANT_STRING)
                                || EXCLUDE.contains(string)) {
                            continue;
                        }
                        CHARS_TO_LABELS.put(string, lastLabel);
                    }
                }
            }
            // remove misc
            for (Entry<String, Set<Label>> entry : CHARS_TO_LABELS.keyValuesSet()) {
                Set<Label> set = entry.getValue();
                if (set.contains(Label.sign) && set.size() > 1) {
                    CHARS_TO_LABELS.remove(entry.getKey(), Label.sign);
                }
            }
            CHARS_TO_LABELS.freeze();
            int i = 0;
            if (false) for (Entry<String, Set<Label>> entry : CHARS_TO_LABELS.keyValuesSet()) {
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
    static final Comparator CODEPOINT_COMPARE = 
            new MultiComparator<String>(
                    UCA.buildCollator(null), // don't need cldr features
                    new UTF16.StringComparator(true,false,0)); 
    //Collator.getInstance(ULocale.ENGLISH);

    static class Data implements Comparable<Data>{
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
            //            int diff = age.compareTo(o.age);
            //            if (diff != 0) {
            //                return diff;
            //            }
            return CODEPOINT_COMPARE.compare(chars, o.chars);
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

        public String toHtmlString(Form form) {
            String symbolaChars = chars;
            int firstCodepoint = chars.codePointAt(0);
            int firstLen = Character.charCount(firstCodepoint);
            int secondCodepoint = firstLen >= chars.length() ? 0 : chars.codePointAt(firstLen);
            if (0x1F1E6 <= firstCodepoint && firstCodepoint <= 0x1F1FF && secondCodepoint != 0) {
                secondCodepoint = chars.codePointAt(2);
                String cc = (char)(firstCodepoint - 0x1F1E6 + 'A') 
                        + ""
                        + (char)(secondCodepoint - 0x1F1E6 + 'A');
                String remapped = REMAP_FLAGS.get(cc);
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
                "</td><td class='age'>" + getVersion() + "</td>\n" +
                "<td class='code'>" + code
                    )
                    + "</td>\n" +
                    "<td class='symb'>" + symbolaChars + "</td>\n" +
                    "<td class='chars'>" + browserChars 
                    + (form != Form.fullForm ? "" :  "</td>\n" +
                            "<td class='segoe'>" + textChars + "</td>\n" +
                            "<td class='andr'>" + androidChars )
                            + (form.compareTo(Form.shortForm) <= 0 ? "" : "</td>\n" +
                                    "<td class='name'>" + name)
                                    + (form.compareTo(Form.shortForm) <= 0 ? "" : "</td>\n" +
                                            "<td class='default'>" + defaultPresentation
                                            + (hasTextVariant ? "*" : ""))
                                            + (form.compareTo(Form.shortForm) <= 0 ? "" : "</td>\n" +
                                                    "<td class='name'>" 
                                                    + CollectionUtilities.join(labels, ", ") + "</td>\n" +
                                                    "<td class='name'>" + header) + "</td>" +
                                                    "</tr>";
        }
        public static String toHtmlHeaderString(Form form) {
            boolean shortForm = form.compareTo(Form.shortForm) <= 0;
            return "<tr>" +
            (form == Form.imagesOnly ? "" : 
                "<th>Version</th>\n"
                + "<th>Code</th>\n"
                    ) +
                    "<th>Symbola*</th>\n" +
                    "<th>Browser</th>\n" +
                    (form != Form.fullForm ? "" : "<th>Segoe</th>\n" +
                            "<th>Android</th>\n") +
                            (shortForm ? "" : "<th>Name</th>\n") +
                            (shortForm ? "" : "<th>Default</th>\n") +
                            (shortForm ? "" : "<th>Labels*</th>\n" +
                                    "<th>Block: <i>Subhead</i></th>\n") +
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

    static String getFlag(String chars) {
        String symbolaChars = chars;
        int firstCodepoint = chars.codePointAt(0);
        int firstLen = Character.charCount(firstCodepoint);
        int secondCodepoint = firstLen >= chars.length() ? 0 : chars.codePointAt(firstLen);
        if (0x1F1E6 <= firstCodepoint && firstCodepoint <= 0x1F1FF && secondCodepoint != 0) {
            secondCodepoint = chars.codePointAt(2);
            String cc = (char)(firstCodepoint - 0x1F1E6 + 'A') 
                    + ""
                    + (char)(secondCodepoint - 0x1F1E6 + 'A');
            String remapped = REMAP_FLAGS.get(cc);
            if (remapped != null) {
                cc = remapped;
            }
            if (!REPLACEMENT_CHARACTER.equals(cc)) {
                return "<img src='images/" + cc + ".png'>";
            }
        }
        return null;
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
        for (String s : new UnicodeSet("[\\u2714\\u2716\\u303D\\u3030 \\u00A9 \\u00AE \\u2795-\\u2797 \\u27B0 \\U0001F519-\\U0001F51C {🇽🇰}]")) {
            if (!Data.STRING_TO_DATA.containsKey(s)) {
                addNewItem(s, Data.STRING_TO_DATA);
                System.out.println(s);
            }
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
        //print(Form.imagesOnly, columns, Data.STRING_TO_DATA);
        //print(Form.shortForm, Data.STRING_TO_DATA);
        System.out.println(Data.LABELS_TO_DATA.keySet());
        print(Form.regularForm, Data.STRING_TO_DATA);
        print(Form.fullForm, Data.STRING_TO_DATA);

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
        print(Form.missingForm, missingMap);

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
        UnicodeSet otherSymbols = new UnicodeSet()
        .addAll(GENERAL_CATEGORY.getSet(General_Category_Values.Math_Symbol.toString()))
        .addAll(GENERAL_CATEGORY.getSet(General_Category_Values.Other_Symbol.toString()))
        .removeAll(NFKCQC.getSet(Binary.No.toString()))
        ;
        for (Label l : Label.values()) {
            Set<Data> set = Data.LABELS_TO_DATA.get(l);
            if (set == null) {
                System.out.println("No chars for: " + l);
                continue;
            }
            out.println("<tr><td>" + l + "</td>\n" +
            		"<td class='chars'>");
            boolean first = true;
            for (Data data : set) {
                if (first) {
                    first = false;
                } else {
                    out.print("\n");
                }
                otherSymbols.remove(data.chars);
                String flag = getFlag(data.chars);
                out.print("<span title='U+" + Utility.hex(data.chars, "U+") + " " + data.name.toLowerCase() + "'>" 
                        + (flag == null ? Data.getEmojiVariant(data.chars) : flag)
                        + "</span>");
            }
            out.println("</td></tr>");
        }
        out.println("<tr><td><i>Other Unicode symbols<br>for comparison only</i></td>\n" +
        		"<td class='chars'>");
        Set<String> sorted = new TreeSet(CODEPOINT_COMPARE);
        otherSymbols.addAllTo(sorted);
        boolean first = true;
        for (String s : sorted) {
            if (first) {
                first = false;
            } else {
                out.print("\n");
            }
            out.print("<span title='U+" + Utility.hex(s, "U+") + " " + NAME.get(s).toLowerCase() + "'>" 
                    + s
                    + "</span>");
        }
        out.println("</td></tr>");

        writeFooter(out);
        out.close();
    }

    public static void addNewItem(String s, Map<String, Data> missingMap) {
        addNewItem(new Data(s), missingMap);
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
        imagesOnly("images"), 
        shortForm("short"), 
        regularForm(""), 
        fullForm("full"), 
        missingForm("missing");
        final String filePrefix;
        final String title;
        Form(String prefix) {
            filePrefix = prefix.isEmpty() ? "" : prefix + "-";
            title = "Draft Emoji Data"
                    + (prefix.isEmpty() ? "" : " (" + UCharacter.toTitleCase(prefix, null) + ")");
        }
    }

    public static <T> void print(Form form, Map<String, Data> set) throws IOException {
        PrintWriter out = BagFormatter.openUTF8Writer(OUTPUT_DIR, 
                form.filePrefix + "emoji-list.html");
        writeHeader(out, form.title);
        out.println(Data.toHtmlHeaderString(form));
        for (Data data : new TreeSet<Data>(set.values())) {
            out.println(data.toHtmlString(form));
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

    static boolean CHECKFACE = false;

    static void test() throws IOException {
        PrintWriter out = BagFormatter.openUTF8Writer(OUTPUT_DIR, "emoji-diff.html");
        writeHeader(out, "Diff List");

        UnicodeSet AnimalPlantFood = new UnicodeSet("[☕ 🌰-🌵 🌷-🍼 🎂 🐀-🐾]");
        testEquals(out, "AnimalPlantFood", AnimalPlantFood, Label.nature, Label.food);

        UnicodeSet Object = new UnicodeSet("[⌚ ⌛ ⏰ ⏳ ☎ ⚓ ✂ ✉ ✏ 🎀 🎁 👑-👣 💄 💉 💊 💌-💎 💐 💠 💡 💣 💮 💰-📷 📹-📼 🔋-🔗 🔦-🔮 🕐-🕧]");
        testEquals(out, "Object", Object, Label.object, Label.office, Label.clothing);

        CHECKFACE=true;
        UnicodeSet PeopleEmotion = new UnicodeSet("[☝ ☺ ✊-✌ ❤ 👀 👂-👐 👤-💃 💅-💇 💋 💏 💑 💓-💟 💢-💭 😀-🙀 🙅-🙏]");
        testEquals(out, "PeopleEmotion", PeopleEmotion, Label.people, Label.body, Label.emotion, Label.face);
        CHECKFACE=false;

        UnicodeSet SportsCelebrationActivity = new UnicodeSet("[♠-♧ ⚽ ⚾ 🀀-🀫 🂠-🂮 🂱-🂾 🃁-🃏 🃑-🃟 🎃-🎓 🎠-🏄 🏆-🏊 💒]");
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
                    "<td class='chars'>"); 
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
                        + Data.getEmojiVariant(s) 
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
}
