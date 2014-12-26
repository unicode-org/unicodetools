package org.unicode.propstest;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.With;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

import com.google.common.base.Splitter;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;


public class Ids {
    static final Pattern NCR_PATTERN = Pattern.compile("\\&[^;]+;");
    static final String BASE = Settings.OTHER_WORKSPACE_DIRECTORY + "DATA/ids-b6bb70e/";

    static class NcrToPua {
        int puaCounter = 0xE000;
        Map<String, Integer> data = new TreeMap();

        public String clean(String string) {
            if (!string.contains("&")) {
                return string;
            }
            Matcher m = NCR_PATTERN.matcher(string);
            StringBuilder b = new StringBuilder();
            int start = 0;
            while (m.find()) {
                b.append(string.substring(start, m.start()));
                Integer pua = data.get(m.group());
                if (pua == null) {
                    pua = puaCounter;
                    data.put(m.group(), puaCounter++);
                }
                b.append(UTF16.valueOf(pua));
                start = m.end();
            }
            b.append(string.substring(start));
            return b.toString();
        }
    }

    static final Map<Integer, String> DecompIds = new TreeMap();
    static final Map<Integer, String> Ids = new TreeMap();
    static final Splitter tabs = Splitter.on('\t');
    static final Splitter semi = Splitter.on(';').trimResults();
    static final UnicodeSet ID = new UnicodeSet("[[:IDS_Binary_Operator:][:IDS_Trinary_Operator:]]").freeze();
    static final UnicodeSet RADICAL_ID = new UnicodeSet("[:radical:]").addAll(ID).freeze();

    static {
        for (String line : FileUtilities.in("/Users/markdavis/workspace/unicodetools/data/ucd/7.0.0-Update", "CJKRadicals.txt")) {
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            // 1; 2F00; 4E00
            List<String> parts = semi.splitToList(line);
            int radical = Integer.parseInt(parts.get(1), 16);
            int ideo = Integer.parseInt(parts.get(2), 16);
            DecompIds.put(ideo, UTF16.valueOf(radical));
        }
    }


    public static void main(String[] args) {
        NcrToPua cleanup = new NcrToPua();
        for (String f : new File(BASE).list()) {
            if (!f.startsWith("IDS-")) {
                continue;
            }
            for (String line : FileUtilities.in(BASE, f)) {
                if (line.startsWith(";")) {
                    continue;
                }
                List<String> parts = tabs.splitToList(line);
                final String codePointString = parts.get(0);
                int codePoint = Integer.parseInt(codePointString.substring(2), 16);
                String chars = cleanup.clean(parts.get(1));
                if (chars.codePointAt(0) != codePoint) {
                    System.out.println("Error on line:\t" + Utility.hex(codePoint)
                            + "\t" + Utility.hex(chars.codePointAt(0))
                            + "\t" + line
                            );
                }
                String decomp = cleanup.clean(parts.get(2));
                Ids.put(codePoint, decomp);
//                if (codePoint == decomp.codePointAt(0)) {
//                    DecompIds.put(codePoint, decomp);
//                }
            }
        }
        // decompose
        UnicodeSet allMissing = new UnicodeSet();
        
        for (Entry<Integer, String> entry : Ids.entrySet()) {
            int key = entry.getKey();
            String value = entry.getValue();
            String fixed = addDecompose(key, value);
            if (!value.equals(fixed)) {
                String notice = "";
                if (!RADICAL_ID.containsAll(fixed)) {
                    UnicodeSet extra = new UnicodeSet().addAll(fixed).removeAll(RADICAL_ID);
                    allMissing.addAll(extra);
                    notice = "\t # " + extra.toPattern(false);
                }

                System.out.println("U+" + Utility.hex(key) 
                        + "\t" + UTF16.valueOf(key) 
                        + "\t" + value 
                        + "\t=>\t" + fixed
                        + notice);
            }
        }
        
//        for (Entry<Integer, String> entry : Ids.entrySet()) {
//            int key = entry.getKey();
//            String value = entry.getValue();
//            System.out.println("U+" + Utility.hex(key) + "\t" + UTF16.valueOf(key) + "\t" + value);
//        }
        for (Entry<String, Integer> entry : cleanup.data.entrySet()) {
            String key = entry.getKey();
            int value = entry.getValue();
            System.out.println(key + "\t" + "U+" + Utility.hex(value));
        }
        System.out.println("Undecomposed:\t" + allMissing.toPattern(false));
    }


    private static String addDecompose(int key, String value) {
        String value2 = DecompIds.get(key);
        if (value2 != null) {
            if (!value.equals(value2)) {
                System.out.println("?\t" + "U+" + Utility.hex(value) + value);
            }
            return value;
        }
        StringBuilder b = new StringBuilder();
        for (int codePoint : With.codePointArray(value)) {
            if (ID.contains(codePoint)) {
                b.appendCodePoint(codePoint);
            } else {
                value = DecompIds.get(codePoint);
                if (value != null) {
                    b.append(value);
                } else {
                    value = Ids.get(codePoint);
                    if (value == null) { // PUA
                        value = UTF16.valueOf(codePoint);
                        DecompIds.put(codePoint, value);
                    } else if (codePoint == value.codePointAt(0)){
                        DecompIds.put(codePoint, value);
                    } else {
                        value = addDecompose(codePoint, value);
                    }
                    b.append(value);
                }
            }
        }
        String result = b.toString();
        DecompIds.put(key, result);
        return result;
    }
}
