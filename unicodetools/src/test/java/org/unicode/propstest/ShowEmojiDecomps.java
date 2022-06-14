package org.unicode.propstest;

import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.UnicodeSet;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.text.utility.Utility;

public class ShowEmojiDecomps {
    public static void main(String[] args) {
        Normalizer2 nfkd = Normalizer2.getNFKDInstance();
        IndexUnicodeProperties iup = IndexUnicodeProperties.make();
        UnicodeSet emoji = iup.loadBinary(UcdProperty.Emoji);
        UnicodeSet ASCII = new UnicodeSet("[[:ascii:]\\uFE0F]").freeze();
        for (String s : emoji) {
            String norm = nfkd.normalize(s);
            if (norm.contains("!")) {
                int i = 0;
            }
            if (!norm.equals(s) || ASCII.containsAll(norm)) {
                System.out.println(Utility.hex(s) + "\t" + s + "\t" + norm);
            }
        }
    }
}
