package org.unicode.jsp;

import org.unicode.jsp.UnicodeUtilities.FilteredStringTransform;
import org.unicode.jsp.UnicodeUtilities.NFKC_CF;

import com.ibm.icu.text.StringTransform;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

public class Uts46 {

  static UnicodeSet Uts46Chars = new UnicodeSet("[[:any:]" +
          " - [:c:] - [:z:]" +
          " - [:Block=Ideographic_Description_Characters:]" +
          "- [:ascii:] - [\uFFFC \uFFFD]" +
          " [\u002Da-zA-Z0-9]" +
          "]").freeze();
  static UnicodeSet Uts46CharsDisplay = new UnicodeSet("[\u00DF\u03C2\u200D\u200C]").addAll(Uts46.Uts46Chars).freeze();

  static StringTransform nfkcCasefold = new UnicodeUtilities.NFKC_CF(); // Transliterator.getInstance("nfkc; casefold; [:di:] remove; nfkc;");
  static StringTransform foldDisplay = new UnicodeUtilities.FilteredStringTransform(new UnicodeSet("[ßς\u200C\u200D]"), nfkcCasefold); // Transliterator.getInstance("nfkc; casefold; [:di:] remove; nfkc;");
  static StringTransform fixIgnorables = Transliterator.createFromRules("foo",
          "[\\u200E\\u200F\\u202A-\\u202E\\u2061-\\u2063\\u206A-\\u206F\\U0001D173-\\U0001D17A\\U000E0001\\U000E0020-\\U000E007F] > \\uFFFF;" +
          "[᠆] > ;" +
          "[\\u17B4\\u17B5\\u115F\\u1160\\u3164\\uFFA0] > \\uFFFF",
          Transliterator.FORWARD);

  static String toUts46(String line) {
    String line2 = fixIgnorables.transform(line);
    return nfkcCasefold.transform(line2);
  }
  
  static int getUts46Type(int i) {
    String strChar = UTF16.valueOf(i);
    String remapped = Uts46.toUts46(strChar);
    if (remapped.length() == 0) {
      return UnicodeUtilities.IGNORED;
    }
    if (!Uts46.Uts46Chars.containsAll(remapped)) {
      return UnicodeUtilities.DISALLOWED;
    }
    if (remapped.equals(strChar)) {
      return UnicodeUtilities.OUTPUT;
    }
    return UnicodeUtilities.REMAPPED;
  }
}
