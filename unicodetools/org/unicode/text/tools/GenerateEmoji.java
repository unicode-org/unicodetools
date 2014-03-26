package org.unicode.text.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.jsp.Subheader;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
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
    static final UnicodeSet JSOURCES = new UnicodeSet();
    static {
        JSOURCES
        .addAll(LATEST.load(UcdProperty.Emoji_DCM).keySet())
        .addAll(LATEST.load(UcdProperty.Emoji_KDDI).keySet())
        .addAll(LATEST.load(UcdProperty.Emoji_SB).keySet())
        .freeze();
    }

    static final Subheader subheader = new Subheader("/Users/markdavis/workspace/unicodetools/data/ucd/7.0.0-Update/");
    static final Set<String> SKIP_BLOCKS = new HashSet(Arrays.asList("Miscellaneous Symbols", 
            "Enclosed Alphanumeric Supplement", 
            "Miscellaneous Symbols And Pictographs",
            "Miscellaneous Symbols And Arrows"));

    enum Label {
        unknown,
        misc, 
        people, faces, hands, nature, cats, clothing, hearts, bubbles, 
        food, transport, places, office,
        time, weather, zodiac, games, sports, 
        sound, 
        flags,    
        arrows;

        static Label get(String string) {
            if (string.equals("hand")) {
                return hands;
            } else if (string.equals("heart")) {
                return hearts;
            } else {
                return Label.valueOf(string);
            }
        }
        static final Relation<String, Label> CHARS_TO_LABELS 
        = Relation.of(new HashMap(), HashSet.class);

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
            CHARS_TO_LABELS.freeze();
            for (Entry<String, Set<Label>> entry : CHARS_TO_LABELS.keyValuesSet()) {
                System.out.println(entry.getKey() + "\t" + entry.getValue());
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
        final String age;
        final String defaultPresentation;
        final Set<Label> labels;
        final String name;
        static final Relation<Label, Data> LABELS_TO_DATA 
        = Relation.of(new EnumMap(Label.class), TreeSet.class);

        static final UnicodeSet missingJSource = new UnicodeSet(JSOURCES);
        static Set<Data> STRING_TO_DATA = new TreeSet<>();
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
            if (diff != 0) return diff;
            return CODEPOINT_COMPARE.compare(chars, o.chars);
        }

        public Data(String chars, String code, String age,
                String defaultPresentation, Set<Label> labels, String name) {
            this.chars = chars;
            this.code = code;
            this.age = age;
            this.defaultPresentation = defaultPresentation;
            this.labels = storeLabels(labels);
            this.name = name;
            if (!Utility.fromHex(code).equals(chars)) {
                throw new IllegalArgumentException();
            }
        }

        public Data(int codepoint) {
            this(new StringBuilder().appendCodePoint(codepoint).toString(), 
                    "U+" + Utility.hex(codepoint), 
                    VERSION.get(codepoint).replace("_", "."), 
                    "text", 
                    Collections.EMPTY_SET, 
                    Default.ucd().getName(codepoint));
        }

        private Set<Label> storeLabels(Set<Label> labels2) {
            labels2 = Label.CHARS_TO_LABELS.get(chars); // override
            if (labels2 == null) {
                labels2 = Collections.singleton(Label.unknown);
            }
            for (Label label : labels2) {
                LABELS_TO_DATA.put(label, this);
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
            return new Data(chars1, code1, age1, defaultPresentation, Collections.EMPTY_SET, name1);
        }

        static void add(String line) {
            Data data = parseLine(line);
            addNewItem(data, STRING_TO_DATA);
            missingJSource.remove(data.chars);
        }

        @Override
        public String toString() {
            return code 
                    + "\t" + age 
                    + "\t" + defaultPresentation
                    + "\t" + chars 
                    + "\t" + name;
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
            if (STANDARDIZED_VARIANT.get(chars + TEXT_VARIANT) != null) {
                textChars = chars + TEXT_VARIANT;
            }

            return "<tr><td class='code'>" + code 
                    + "</td><td class='age'>" + age 
                    + "</td><td class='symb'>" + symbolaChars 
                    + "</td><td class='segoe'>" + textChars 
                    + "</td><td class='chars'>" + browserChars 
                    + "</td><td class='andr'>" + androidChars 
                    + "</td><td class='name'>" + name
                    + (form == Form.shortForm ? "" : "</td><td class='default'>" + defaultPresentation)
                    + (form == Form.shortForm ? "" : "</td><td class='name'>" 
                            + CollectionUtilities.join(labels, ", ")
                            + "</td><td class='name'>" + header)
                            + "</td></tr>";
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

        public static String toHtmlHeaderString(boolean shortForm) {
            return "<tr>" +
                    "<th>Code</th>" +
                    "<th>Version</th>" +
                    "<th>Symbola*</th>" +
                    "<th>Segoe</th>" +
                    "<th>Browser</th>" +
                    "<th>Android</th>" +
                    "<th>Name</th>" +
                    (shortForm ? "" : "<th>Default</th>") +
                    (shortForm ? "" : "<th>Labels*</th>" +
                            "<th>Block: <i>Subhead</i></th>") +
                            "</tr>";
        }
    }

    public static void main(String[] args) throws IOException {
        for (String line : FileUtilities.in(GenerateEmoji.class, "emojiData.txt")) {
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            Data.add(line);
        }
        for (String s : new UnicodeSet("[\\u2714\\u2716\\u303D\\u3030 \\u00A9 \\u00AE \\u2795-\\u2797 \\u27B0 \\U0001F519-\\U0001F51C]")) {
            addNewItem(new Data(s.codePointAt(0)), Data.STRING_TO_DATA);
            Data.missingJSource.remove(s);
            System.out.println(s);
        }
        Data.missingJSource.removeAll(new UnicodeSet("[\\u2002\\u2003\\u2005]"));
        if (Data.missingJSource.size() > 0) {
            throw new IllegalArgumentException("Missing: " + Data.missingJSource);
        }
        print(Form.shortForm, Data.STRING_TO_DATA);
        System.out.println(Data.LABELS_TO_DATA.keySet());
        print(Form.longForm, Data.STRING_TO_DATA);

        LinkedHashSet missingMap = new LinkedHashSet();

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
            addNewItem(new Data(codepoint), missingMap);
        }
        print(Form.missingForm, missingMap);

        showLabels();
    }

    private static void showLabels() throws IOException {
        PrintWriter out = BagFormatter.openUTF8Writer(OUTPUT_DIR, "emoji-labels.html");
        writeHeader(out, "Draft Emoji Labels");
        for (Label l : Label.values()) {
            Set<Data> set = Data.LABELS_TO_DATA.get(l);
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

    public static void addNewItem(Data item, Set<Data> missingMap) {
        if (missingMap.contains(item)) {
            throw new IllegalArgumentException(item.toString());
        }
        missingMap.add(item);
    }

    enum Form {longForm, shortForm, missingForm}

    public static <T> void print(Form form, Set<Data> set) throws IOException {
        PrintWriter out = BagFormatter.openUTF8Writer(OUTPUT_DIR, 
                (form == Form.shortForm ? "short-" : form == Form.missingForm ? "missing-" : "")
                + "emoji-list.html");
        writeHeader(out, "Draft Emoji List");
        out.println(Data.toHtmlHeaderString(form == Form.shortForm));
        for (Data data : set) {
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
}
