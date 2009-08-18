package org.unicode.text.UCD;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.PrettyPrinter;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;

public class GetTypology {
    public static void main(String[] args) throws IOException {
        UnicodeMap<String> data2 = new UnicodeMap();
        Map<String,UnicodeSet> data = new TreeMap();
        Map<String,Set<String>> tagToPrefixes = new TreeMap();
        BufferedReader br = BagFormatter.openUTF8Reader("/Users/markdavis/Documents/workspace35/DATA/UCD/", "U52M09XXXX.txt");
        StringBuilder name = new StringBuilder();

        while (true) {
            String line = Utility.readDataLine(br);
            if (line == null) break;
            String[] parts = line.split("\t");
            int cp = Integer.parseInt(parts[0],16);
            name.setLength(0);

            for (int i = 2; i < parts.length - 1; ++i) {
                String part = parts[i];
                if (part.equals("[X]")) continue;

                if (!part.startsWith("[") || !part.endsWith("]")) {
                    throw new IllegalArgumentException(line);
                }
                part = part.replace(";", "");
                part = part.replace(" ", "_");
                part = part.substring(1, part.length()-1);
                if (part.length() > 0) {
                    UnicodeSet s = data.get(part);
                    if (s == null) {
                        data.put(part, s = new UnicodeSet());
                    }
                    s.add(cp);
                    String prefix = name.toString();
                    Set<String> prefixes = tagToPrefixes.get(part);
                    if (prefixes == null) {
                        tagToPrefixes.put(part, prefixes = new TreeSet<String>());
                    }
                    prefixes.add(prefix);
                }
                if (name.length() != 0) {
                    name.append("|");
                }
                name.append(part);
            }
            data2.put(cp, name.toString());
        }
        br.close();
        final UnicodeSet TO_QUOTE = new UnicodeSet("[[:z:][:me:][:mn:][:di:][:c:]-[\u0020]]").freeze();

        PrettyPrinter pp = new PrettyPrinter().setToQuote(TO_QUOTE);
        System.out.println("*** Tags with multiple prefixes");
        for (String tag : tagToPrefixes.keySet()) {
            Set<String> prefixes = tagToPrefixes.get(tag);
            if (prefixes.size() == 1) continue;
            System.out.println(tag + "\t" + prefixes);
        }
        System.out.println("*** Tags to Unicode Set");
        for (String tag : data.keySet()) {
            System.out.println(tag + "\t" + pp.format(data.get(tag)));
        }
        System.out.println("*** Tag Hierarchy to Unicode Set");
        TreeSet<String> values = new TreeSet();
        values.addAll(data2.values());
        for (String tag : values) {
            System.out.println(tag + "\t" + pp.format(data2.getSet(tag)));
        }
    }
}
