package org.unicode.props;

import org.unicode.text.UCD.Default;

import com.ibm.icu.dev.util.UnicodeMap;

public class PropertyDemo {
    public static void main(String[] args) {
        final IndexUnicodeProperties latest = IndexUnicodeProperties.make(Default.ucdVersion());
        final UnicodeMap<String> scripts = latest.load(UcdProperty.Unified_Ideograph);
        for (final String i : scripts.values()) {
            System.out.println(i + "\t" + scripts.getSet(i).size());
        }
    }
}
