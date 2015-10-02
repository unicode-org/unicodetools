package org.unicode.text.UCD;

import com.ibm.icu.text.UnicodeSet;

public class PartOfTrailingNFD {
    public static void main(String[] args) {
        Normalizer nfd = Default.nfd();
        UnicodeSet result = new UnicodeSet();
        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            String s = nfd.normalize(cp);
            int first = s.codePointAt(0);
            String rest = s.substring(Character.charCount(first));
            result.addAll(rest);
        }
        System.out.println(result.size() + "\t" + result.toPattern(false));
    }
}
