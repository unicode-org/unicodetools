package org.unicode.propstest;

import java.util.Map.Entry;
import java.util.TreeMap;

import org.unicode.cldr.util.With;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.text.UCD.Default;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeMap.EntryRange;

public class ListVariants {
    public static void main(String[] args) {
        final IndexUnicodeProperties latest = IndexUnicodeProperties.make(Default.ucdVersion());
        final UnicodeMap<String> standardizedVariant = latest.load(UcdProperty.Standardized_Variant);
        UnicodeMap<TreeMap<Integer, String>> variantToUnicodeMap = new UnicodeMap();
        
        for (EntryRange entry : standardizedVariant.entryRanges()) {
            if (entry.string == null) {
                System.out.println(Utility.hex(entry.codepoint) + ".." + Utility.hex(entry.codepointEnd) + "\t" + entry.value);
            } else {
                int[] codepoints = With.codePointArray(entry.string);
                if (codepoints.length != 2) {
                    throw new IllegalArgumentException();
                }
                TreeMap<Integer, String> map = variantToUnicodeMap.get(codepoints[1]);
                if (map == null) {
                    variantToUnicodeMap.put(codepoints[1], map = new TreeMap<Integer, String>());
                }
                map.put(codepoints[0], entry.value.toString());
                //System.out.println(Utility.hex(entry.string) + "\t" + entry.value);
            }
        }
        for (Entry<String, TreeMap<Integer, String>> entry : variantToUnicodeMap.entrySet()) {
            final String variant = entry.getKey();
            System.out.println(Utility.hex(variant) + "\t" + Default.ucd().getName(variant));
            for (Entry<Integer, String> entry2 : entry.getValue().entrySet()) {
                final Integer base = entry2.getKey();
                System.out.println("\t" + Utility.hex(base) + "\t" + Default.ucd().getName(base) + "\t" + entry2.getValue());
            }
        }
    }
}
