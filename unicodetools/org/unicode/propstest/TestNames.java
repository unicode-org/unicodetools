package org.unicode.propstest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.props.GenerateEnums;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeMap.EntryRange;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

public class TestNames {
    static final IndexUnicodeProperties latest = IndexUnicodeProperties.make(GenerateEnums.ENUM_VERSION);
    static final UnicodeMap<String> names = latest.load(UcdProperty.Name);
    static final UnicodeMap<String> nameAliases = latest.load(UcdProperty.Name_Alias);
    static final UnicodeMap<String> namedSequences = latest.load(UcdProperty.Named_Sequences);

    static HashMap<String,Data> skeletonToCodepoint = new HashMap();

    static class Data {
        public Data(String name, String codepoints2) {
            names.add(name);
            codePoints.add(codepoints2);
        }
        Set<String> names = new LinkedHashSet();
        UnicodeSet codePoints = new UnicodeSet();
        @Override
        public String toString() {
            // TODO Auto-generated method stub
            return codePoints + " => " + names;
        }
    }

    public static void main(String[] args) {
        add(names);
        add(nameAliases);
        add(namedSequences);
        for (Entry<String, Data> entry : skeletonToCodepoint.entrySet()) {
            String key = entry.getKey();
            Data value = entry.getValue();
            if (value.names.size() > 1 || value.codePoints.size() > 1) {
                System.out.println(key + "\t" + value);
            }
        }
    }

    static Pattern SEMI = Pattern.compile("\\s*;\\s*");

    private static void add(UnicodeMap<String> values) {
        for (EntryRange<String> name : values.entryRanges()) {
            String value = name.value;
            if (value == null) {
                continue;
            }
            if (name.string != null) {
                add2(name.string, value);
            } else {
                add2(UTF16.valueOf(name.codepoint), value);
                add2(UTF16.valueOf(name.codepointEnd), value);
                // we don't care about intervening; all CJK...
            }
        }
    }

    public static void add2(String codepoints, String value) {
        if (value.contains(";")) {
            for (String part : SEMI.split(value)) {
                add(codepoints, part);
            }
        } else {
            add(codepoints, value);
        }
    }

    static HashSet<String> HYPHENS = new HashSet();
    
    private static void add(String codepoints, String name) {
        if (name.contains("-") && !HYPHENS.contains(codepoints)) {
            System.out.println(codepoints + "\t" + name);
            HYPHENS.add(codepoints);
        }
        String skeleton = getSkeleton(name);
        Data oldCodePoint = skeletonToCodepoint.get(skeleton);
        if (oldCodePoint == null) {
            skeletonToCodepoint.put(skeleton, new Data(name, codepoints));
        } else {
            oldCodePoint.names.add(name);
            oldCodePoint.codePoints.add(codepoints);
        }
    }

    static Matcher TO_REMOVE = Pattern.compile(
            "\\s" // and space character
            // + "|-"  // any  hyphen
            + "|(?<=[a-zA-Z0-9])-(?=[a-zA-Z0-9])" // any medial hyphen
            + "|CHARACTER" // any one of these sequences of letters
            + "|LETTER"
            + "|DIGIT")
            .matcher("");

    public static synchronized String getSkeleton(String name) {
        // Ignore (i.e., fold away) any casing distinctions
        name = name.toUpperCase(Locale.ROOT);
        // Ignore (i.e., fold away) any medial hyphens; remove "CHARACTER", "LETTER", or "DIGIT"
        name = TO_REMOVE.reset(name).replaceAll("");
        return name;
    }
}
