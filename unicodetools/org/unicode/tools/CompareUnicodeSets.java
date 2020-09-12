package org.unicode.tools;

import java.util.Objects;

import com.ibm.icu.text.UnicodeSet;

public class CompareUnicodeSets {
public static void main(String[] args) {
    UnicodeSet a = new UnicodeSet("[ا أ آ ب پ ت ٹ ث ج چ ح خ د ڈ ذ ر ڑ ز ژ س ش ص ض ط ظ ع غ ف ق ک گ ل م ن ں و ؤ ہ ۂ ھ ء ی ئ ے ة ه]");
    UnicodeSet b = new UnicodeSet("[ا ب پ ت ٹ ث ج چ ح خ د ڈ ذ ر ڑ ز ژ س ش ص ض ط ظ ع غ ف ق ک گ ل م ن و ہ ھ ء ی ے]");
    System.out.println("old:\t" + a.toPattern(false));
    System.out.println("new:\t" + b.toPattern(false));
    System.out.println("old-only:\t" + new UnicodeSet(a).removeAll(b).toPattern(false));
    System.out.println("new-only:\t" + new UnicodeSet(b).removeAll(a).toPattern(false));
    Objects.equals(3, 3);
}
}
