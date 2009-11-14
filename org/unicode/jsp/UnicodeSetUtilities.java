package org.unicode.jsp;

import java.text.ParsePosition;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.StringTransform;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.text.Normalizer.Mode;

public class UnicodeSetUtilities {
  private static UnicodeSet OK_AT_END = new UnicodeSet("[ \\]\t]").freeze();
  static Object[][] specialProperties = {
    {"isCaseFolded", UnicodeUtilities.isCaseFolded},
    {"isUppercase", UnicodeUtilities.isUppercase},
    {"isLowercase", UnicodeUtilities.isLowercase},
    {"isTitlecase", UnicodeUtilities.isTitlecase},
    {"isCased", UnicodeUtilities.isCased},
    {"isNFC", parseUnicodeSet("[:^nfcqc=n:]")},
    {"isNFD", parseUnicodeSet("[:^nfdqc=n:]")},
    {"isNFKC", parseUnicodeSet("[:^nfkcqc=n:]")},
    {"isNFKD", parseUnicodeSet("[:^nfkdqc=n:]")},
  };
  
  static final UnicodeSet UNASSIGNED = (UnicodeSet) UnicodeSetUtilities.parseUnicodeSet("[:gc=unassigned:]").freeze();
  static final UnicodeSet PRIVATE_USE = (UnicodeSet) UnicodeSetUtilities.parseUnicodeSet("[:gc=privateuse:]").freeze();
  static final UnicodeSet SURROGATE = (UnicodeSet) UnicodeSetUtilities.parseUnicodeSet("[:gc=surrogate:]").freeze();

  static final int SAMPLE_UNASSIGNED = 0xFFF0;
  static final int SAMPLE_PRIVATE_USE = 0xE000;
  static final int SAMPLE_SURROGATE = 0xD800;
  static {
    if (!UNASSIGNED.contains(SAMPLE_UNASSIGNED)) {
      throw new IllegalArgumentException("Internal error");
    }
  }
  static final UnicodeSet STUFF_TO_TEST = new UnicodeSet(UNASSIGNED)
  .addAll(PRIVATE_USE).addAll(SURROGATE).complement()
  .add(SAMPLE_UNASSIGNED).add(SAMPLE_PRIVATE_USE).add(SAMPLE_SURROGATE);

  static class NFKC_CF implements StringTransform {
    //static Matcher DI = Pattern.compile(UnicodeRegex.fix("[:di:]")).matcher("");
    UnicodeMap<String> DI2 = new UnicodeMap<String>().putAll(parseUnicodeSet("[:di:]"), "");
    public String transform(String source) {
      String s0 = myFoldCase(source);
      String s1 = MyNormalize(s0, Normalizer.NFKC);
      String s2 = myFoldCase(s1);
      //String s3 = DI.reset(s2).replaceAll("");
      String s3 = DI2.transform(s2);
      String s4 = MyNormalize(s3,Normalizer.NFKC);
      return s4;
    }
  }  
  
  private static String myFoldCase(String source) {
    return UCharacter.foldCase(source, true);
  }

  public static UnicodeSet parseUnicodeSet(String input) {
    input = input.trim() + "]]]]]";
  
    String parseInput = (input.startsWith("[") ? "" : "[") + input + "]]]]]";
    ParsePosition parsePosition = new ParsePosition(0);
    UnicodeSet result = new UnicodeSet(parseInput, parsePosition, UnicodeSetUtilities.myXSymbolTable);
    int parseEnd = parsePosition.getIndex();
    if (parseEnd != parseInput.length() && !UnicodeSetUtilities.OK_AT_END.containsAll(parseInput.substring(parseEnd))) {
      parseEnd--; // get input offset
      throw new IllegalArgumentException("Additional characters past the end of the set, at " 
              + parseEnd + ", ..." 
              + input.substring(Math.max(0, parseEnd - 10), parseEnd)
              + "|"
              + input.substring(parseEnd, Math.min(input.length(), parseEnd + 10))
      );
    }
    return result;
  }
  
  private static UnicodeSet.XSymbolTable myXSymbolTable = new UnicodeSet.XSymbolTable() {
    public boolean applyPropertyAlias(String propertyName,
            String propertyValue, UnicodeSet result) {
      if (propertyName.equalsIgnoreCase("idna")) {
        return UnicodeUtilities.getIdnaProperty(propertyValue, result);
      }
      for (int i = 0; i < UnicodeSetUtilities.specialProperties.length; ++i) {
        if (propertyName.equalsIgnoreCase((String) UnicodeSetUtilities.specialProperties[i][0])) {
          result.clear().addAll((UnicodeSet) UnicodeSetUtilities.specialProperties[i][1]);
          if (UnicodeUtilities.getBinaryValue(propertyValue)) {
            result.complement();
          }
          return true;
        }
      }
      int propertyEnum;
      try {
        propertyEnum = UnicodeUtilities.getXPropertyEnum(propertyName);
      } catch (RuntimeException e) {
        return false;
      }
      String trimmedPropertyValue = propertyValue.trim();
      Normalizer.Mode compat = null;
      if (trimmedPropertyValue.startsWith("*")) {
        compat = Normalizer.NFC;
        trimmedPropertyValue = trimmedPropertyValue.substring(1);
        if (trimmedPropertyValue.startsWith("*")) {
          compat = Normalizer.NFKC;
          trimmedPropertyValue = trimmedPropertyValue.substring(1);
        }
      }
      if (trimmedPropertyValue.startsWith("/") && trimmedPropertyValue.endsWith("/")) {
        Matcher matcher = Pattern.compile(
                trimmedPropertyValue.substring(1, trimmedPropertyValue.length() - 1)).matcher("");
        result.clear();
        boolean onlyOnce = propertyEnum >= UProperty.STRING_START
        && propertyEnum < UnicodeUtilities.XSTRING_LIMIT;
        for (UnicodeSetIterator it = new UnicodeSetIterator(STUFF_TO_TEST); it.next();) {
          int cp = it.codepoint;
          for (int nameChoice = UProperty.NameChoice.SHORT; nameChoice <= UProperty.NameChoice.LONG; ++nameChoice) {
            String value = UnicodeUtilities.getXStringPropertyValue(propertyEnum, cp, nameChoice, compat);
            if (value == null) {
              continue;
            }
            if (matcher.reset(value).find()) {
              result.add(cp);
            }
            if (onlyOnce) {
              break;
            }
          }
        }
      } else if (propertyEnum >= UProperty.STRING_LIMIT
              && propertyEnum < UnicodeUtilities.XSTRING_LIMIT) {
        // support extra string routines
        String fixedPropertyValue = UnicodeUtilities.UNICODE.transform(trimmedPropertyValue);
        for (UnicodeSetIterator it = new UnicodeSetIterator(STUFF_TO_TEST); it.next();) {
          int cp = it.codepoint;
          String value = UnicodeUtilities.getXStringPropertyValue(propertyEnum, cp,
                  UProperty.NameChoice.SHORT, compat);
          if (fixedPropertyValue.equals(value)) {
            result.add(cp);
          }
        }
      } else if (compat != null) {
        int valueEnum = UCharacter.getPropertyValueEnum(propertyEnum, trimmedPropertyValue);
        String fixedValue = UCharacter.getPropertyValueName(propertyEnum, valueEnum, UProperty.NameChoice.LONG);
        for (UnicodeSetIterator it = new UnicodeSetIterator(STUFF_TO_TEST); it.next();) {
          int cp = it.codepoint;
          String value = UnicodeUtilities.getXStringPropertyValue(propertyEnum, cp, UProperty.NameChoice.LONG, compat);
          if (fixedValue.equals(value)) {
            result.add(cp);
          }
        }
      } else {
        return false;
      }
      if (result.contains(SAMPLE_UNASSIGNED)) {
        result.addAll(UNASSIGNED);
      }
      if (result.contains(SAMPLE_PRIVATE_USE)) {
        result.addAll(PRIVATE_USE);
      }
      if (result.contains(SAMPLE_SURROGATE)) {
        result.addAll(SURROGATE);
      }
      return true;
    }
  };

  static String MyNormalize(int codepoint, Mode mode) {
    return Normalizer.normalize(codepoint, mode);
  }

  static String MyNormalize(String string, Mode mode) {
    return Normalizer.normalize(string, mode);
  }



}
