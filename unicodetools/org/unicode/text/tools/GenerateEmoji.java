package org.unicode.text.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
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
import org.unicode.text.UCD.Default;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.dev.util.CollectionUtilities;

public class GenerateEmoji {
    static final Pattern tab = Pattern.compile("\t");
    static final Pattern space = Pattern.compile(" ");

    static final Subheader subheader = new Subheader("/Users/markdavis/workspace/unicodetools/data/ucd/7.0.0-Update/");
    static final Set<String> SKIP_BLOCKS = new HashSet(Arrays.asList("Miscellaneous Symbols", 
            "Enclosed Alphanumeric Supplement", 
            "Miscellaneous Symbols And Pictographs",
            "Miscellaneous Symbols And Arrows"));
    
    enum Label {
        misc, arrows, time, weather, people, faces, zodiac, games, music, 
        flags, nature, sports, transport, astronomy, food, cats, hands,
        clothing, hearts, bubbles;
    
        static Label get(String string) {
            if (string.equals("hand")) {
                return hands;
            } else if (string.equals("heart")) {
                return hearts;
            } else {
                return Label.valueOf(string);
            }
        }
    }

    static class Data {
        private static final String REPLACEMENT_CHARACTER = "\uFFFD";
        final String chars;
        final String code;
        final String age;
        final String defaultPresentation;
        final Set<Label> labels;
        final String name;
        static final Set<Label> allLabels = new LinkedHashSet();
        static Map<String,Data> STRING_TO_DATA = new LinkedHashMap<>();

        Data(String line) {
            String[] items = tab.split(line);
            // U+2194   V1.1    text    arrows  ↔   LEFT RIGHT ARROW
            code = items[0];
            age = items[1];
            defaultPresentation = items[2];
            String temp = items[3];
            if (temp.isEmpty()) {
                temp = "misc";
            }
            EnumSet labelSet = EnumSet.noneOf(Label.class);
            for (String label : Arrays.asList(space.split(temp))) {
                labelSet.add(Label.get(label));
            }
            labels = Collections.unmodifiableSet(labelSet);
            allLabels.addAll(labels);
            chars = items[4];
            temp = items[5];
            if (temp.startsWith("flag")) {
                temp = "flag for" + temp.substring(4);
            }
            name = temp;
            if (!Utility.fromHex(code).equals(chars)) {
                throw new IllegalArgumentException();
            }
        }
        static void add(String line) {
            Data data = new Data(line);
            STRING_TO_DATA.put(data.chars, data);
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
        public String toHtmlString() {
            String chars2 = chars;
            int firstCodepoint = chars.codePointAt(0);
            if (0x1F1E6 <= firstCodepoint && firstCodepoint <= 0x1F1FF) {
                int secondCodepoint = chars.codePointAt(2);
                String cc = (char)(firstCodepoint - 0x1F1E6 + 'A') 
                        + ""
                        + (char)(secondCodepoint - 0x1F1E6 + 'A');
                String remapped = remap.get(cc);
                if (remapped != null) {
                    cc = remapped;
                }
                if (REPLACEMENT_CHARACTER.equals(cc)) {
                    chars2 = REPLACEMENT_CHARACTER;
                } else {
                    chars2 = "<img src='images/" + cc + ".png'>";
                }
            }

            String header = Default.ucd().getBlock(firstCodepoint).replace('_', ' ');
            String subhead = subheader.getSubheader(firstCodepoint);
            if (SKIP_BLOCKS.contains(header)) {
                header = "<i>" + subhead + "</i>";
            } else if (!header.equalsIgnoreCase(subhead)) {
                header += ": <i>" + subhead + "</i>";
            }
            return "<tr><td class='code'>" + code 
                    + "</td><td class='age'>" + age 
                    + "</td><td class='default'>" + defaultPresentation
                    + "</td><td class='chars'>" + chars2 
                    + "</td><td class='name'>" + name
                    + "</td><td class='name'>" + CollectionUtilities.join(labels, ", ")
                    + "</td><td class='name'>" + header
                    + "</td></tr>";
        }
        public static String toHtmlHeaderString() {
            return "<tr>" +
                    "<th>Code</th>" +
                    "<th>Version</th>" +
                    "<th>Default</th>" +
                    "<th>Char(s)</th>" +
                    "<th>Name</th>" +
                    "<th>Labels*</th>" +
                    "<th>Block: <i>Subhead</i></th>" +
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
        PrintWriter out = BagFormatter.openUTF8Writer("/Users/markdavis/workspace/unicode-draft/reports/tr51/","emoji-list.html");
        out.println("<html><head><meta http-equiv='Content-Type' content='text/html; charset=UTF-8'/>\n" +
                "<link rel='stylesheet' type='text/css' href='emoji-list.css'>\n" +
                "<title>Emoji List</title>\n" +
                "</head><body><table>");
        out.println(Data.toHtmlHeaderString());
        for (Entry<String, Data> entry : Data.STRING_TO_DATA.entrySet()) {
            out.println(entry.getValue().toHtmlString());
        }
        out.println("</table></body></html>");
        out.close();
        System.out.println(Data.allLabels);
    }
}
