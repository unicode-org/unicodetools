package org.unicode.jsp;

import org.unicode.jsp.UnicodeUtilities.IdnaType;

import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.StringTransform;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

public class Uts46 {

  private static final String LABEL_SEPARATOR_STRING = "[\u002E\uFF0E\u3002\uFF61]";
  static UnicodeSet LABEL_SEPARATORS = new UnicodeSet(LABEL_SEPARATOR_STRING);
  static UnicodeSet DEVIATIONS = new UnicodeSet("[\\u200C \\u200D \u00DF \u03C2]");
  //static final String[] IdnaNames = { "valid", "ignored", "mapped", "disallowed" };

  static UnicodeSet Uts46Chars = UnicodeSetUtilities.parseUnicodeSet("[[:any:]" +
          " - [:c:] - [:z:]" +
          " - [:Block=Ideographic_Description_Characters:]" +
          "- [:ascii:] - [\uFFFC \uFFFD]" +
          " [\\u002D\\u002Ea-zA-Z0-9]" +
          "]")
          .freeze();
  static UnicodeSet UtsExclusions = UnicodeSetUtilities.parseUnicodeSet("[" +
          "[\u04C0\u10A0-\u10C5\u2132\u2183]" +
          "[\\U0002F868 \\U0002F874 \\U0002F91F \\U0002F95F \\U0002F9BF]" +
          "[\\u1806 \\uFFFC \\uFFFD \\u17B4 \\u17B5 \\u115F \\u1160\\u3164\\uFFA0]" +
  		"]");
  static {
    for (int i = 0; i <= 0x10FFFF; ++i) {
      if (LABEL_SEPARATORS.contains(i)) continue;
      String temp = UnicodeSetUtilities.MyNormalize(i, Normalizer.NFKC);
      if (LABEL_SEPARATORS.containsSome(temp)) {
        UtsExclusions.add(i);
      }
    }
    UtsExclusions.freeze();
  }
  static UnicodeSet Uts46CharsDisplay = UnicodeSetUtilities.parseUnicodeSet("[\u00DF\u03C2\u200D\u200C]").addAll(Uts46.Uts46Chars).freeze();

  static StringTransform nfkcCasefold = new UnicodeSetUtilities.NFKC_CF(); // Transliterator.getInstance("nfkc; casefold; [:di:] remove; nfkc;");
  static StringTransform foldDisplay = new UnicodeUtilities.FilteredStringTransform(DEVIATIONS, nfkcCasefold); 
  static StringTransform fixIgnorables = Transliterator.createFromRules("foo",
          LABEL_SEPARATOR_STRING + "> \\u002E ;" +
          "[\\u200E\\u200F\\u202A-\\u202E\\u2061-\\u2063\\u206A-\\u206F\\U0001D173-\\U0001D17A\\U000E0001\\U000E0020-\\U000E007F] > \\uFFFF;" +
          "[᠆] > ;" +
          "[\\u17B4\\u17B5\\u115F\\u1160\\u3164\\uFFA0] > \\uFFFF",
          Transliterator.FORWARD);

  static String toUts46(String line) {
    String line2 = fixIgnorables.transform(line);
    return nfkcCasefold.transform(line2);
  }
  
  static IdnaType getUts46Type(int i, UnicodeSet overallAllowed) {
    String strChar = UTF16.valueOf(i);
    String remapped = Uts46.toUts46(strChar);
    if (remapped.length() == 0) {
      return UnicodeUtilities.IGNORED;
    }
    if (!overallAllowed.contains(i)) {
      return UnicodeUtilities.DISALLOWED;
    }
    if (UtsExclusions.contains(i)) {
      return UnicodeUtilities.DISALLOWED;
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
