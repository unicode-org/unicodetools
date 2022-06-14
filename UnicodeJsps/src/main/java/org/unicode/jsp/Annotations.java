package org.unicode.jsp;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.UnicodeSet;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.unicode.cldr.draft.FileUtilities;

public class Annotations {
    private static final UnicodeMap<Set<String>> data = new UnicodeMap();

    public static Set<String> get(int codepoint) {
        return data.get(codepoint);
    }

    public static Set<String> get(String codepoints) {
        return data.get(codepoints);
    }

    public static UnicodeSet keys() {
        return data.keySet();
    }

    static {
        Map<String, Integer> idToCodepoint = new TreeMap();
        for (String line : FileUtilities.in(Annotations.class, "annotations.txt")) {
            String[] parts = line.split("\t");
            int codepoint = parts[1].codePointAt(0);
            idToCodepoint.put(parts[2], codepoint);
            String annotation = parts[3];
            data.put(codepoint, Collections.singleton(annotation));
        }
        // resolve references
        UnicodeSet copy = new UnicodeSet(keys());
        Matcher m = Pattern.compile("RES\\s([A-Za-z]{1,2}\\d+[A-Za-z]?)").matcher("");
        StringBuilder b = new StringBuilder();

        boolean changed;
        do {
            changed = false;
            for (final String s : copy) {
                Set<String> an = get(s);
                String aString = an.iterator().next();
                if (!aString.contains("RES")) {
                    continue;
                }
                int count = 0;
                int lastEnd = 0;
                b.setLength(0);
                m.reset(aString);
                while (m.find()) {
                    String original = m.group(1);
                    Integer codepoint = idToCodepoint.get(original.toUpperCase(Locale.ROOT));
                    Set<String> replacement = data.get(codepoint);
                    if (replacement == null) {
                        throw new IllegalArgumentException("Can't replace " + original);
                    } else {
                        b.append(aString, lastEnd, m.start())
                                .append("{")
                                .append(replacement.iterator().next())
                                .append("}");
                        lastEnd = m.end();
                        ++count;
                        if (count > 10) {
                            throw new IllegalArgumentException("Recursion");
                        }
                    }
                }
                if (count > 0) {
                    b.append(aString, lastEnd, aString.length());
                    data.put(s, Collections.singleton(b.toString()));
                }
            }
        } while (changed);
        data.freeze();
    }

    public static void main(String[] args) {
        for (String s : keys()) {
            System.out.println("U+" + Utility.hex(s) + "\t" + s + "\t" + get(s).iterator().next());
        }
    }
}
