package org.unicode.props;

import org.unicode.text.UCD.Default;

import com.ibm.icu.dev.test.util.UnicodeMap;

public class PropertyDemo {
public static void main(String[] args) {
    IndexUnicodeProperties latest = IndexUnicodeProperties.make(Default.ucdVersion());
    UnicodeMap<String> scripts = latest.load(UcdProperty.Unified_Ideograph);
    for (String i : scripts.values()) {
        System.out.println(i + "\t" + scripts.getSet(i).size());
    }
}
}
