package org.unicode.tools;

import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.text.utility.Utility;

public class CheckSimpTrad {
    public static void main(String[] args) {
        IndexUnicodeProperties iup = IndexUnicodeProperties.make("6.2");
        String test = "內衣网";
        int cp;
        for (int i = 0; i < test.length(); i += Character.charCount(cp)) {
            cp = Character.codePointAt(test, i);
            String sVar = iup.getResolvedValue(UcdProperty.kSimplifiedVariant, cp);
            String tVar = iup.getResolvedValue(UcdProperty.kTraditionalVariant, cp);
            System.out.println(Utility.hex(cp) + "\tsVar:\t" + sVar + "\ttVar:\t" + tVar);
        }
        ;
    }
}
