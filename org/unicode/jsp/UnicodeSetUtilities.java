package org.unicode.jsp;

import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.unicode.jsp.UnicodeProperty.PatternMatcher;
import org.unicode.jsp.UnicodeSetUtilities.ComparisonMatcher.Relation;

import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.StringTransform;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.Normalizer.Mode;

public class UnicodeSetUtilities {
  private static UnicodeSet OK_AT_END = new UnicodeSet("[ \\]\t]").freeze();
  static Object[][] specialProperties = {
    {"isCaseFolded", UnicodeUtilities.isCaseFolded},
    {"isUppercase", UnicodeUtilities.isUppercase},
    {"isLowercase", UnicodeUtilities.isLowercase},
    {"isTitlecase", UnicodeUtilities.isTitlecase},
    {"isCased", UnicodeUtilities.isCased},
    {"isNFC", parseUnicodeSet("[:^nfcqc=n:]", TableStyle.simple)},
    {"isNFD", parseUnicodeSet("[:^nfdqc=n:]", TableStyle.simple)},
    {"isNFKC", parseUnicodeSet("[:^nfkcqc=n:]", TableStyle.simple)},
    {"isNFKD", parseUnicodeSet("[:^nfkdqc=n:]", TableStyle.simple)},
    {"ASCII", parseUnicodeSet("[\\u0000-\\u007F]", TableStyle.simple)},
    {"ANY", parseUnicodeSet("[\\u0000-\\U0010FFFF]", TableStyle.simple)},
  };


  static class NFKC_CF implements StringTransform {
    //static Matcher DI = Pattern.compile(UnicodeRegex.fix("[:di:]")).matcher("");
    UnicodeMap<String> DI2 = new UnicodeMap<String>().putAll(parseUnicodeSet("[:di:]", TableStyle.simple), "");
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

  enum TableStyle {simple, extras}

  public static UnicodeSet parseUnicodeSet(String input, TableStyle style) {
    input = input.trim() + "]]]]]";

    String parseInput = "[" + input + "]]]]]";
    ParsePosition parsePosition = new ParsePosition(0);
    UnicodeSet result = new UnicodeSet(parseInput, parsePosition, style == TableStyle.simple ? myXSymbolTable : fullSymbolTable);
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


  static UnicodeSet.XSymbolTable myXSymbolTable = new MySymbolTable(TableStyle.simple);
  static UnicodeSet.XSymbolTable fullSymbolTable = new MySymbolTable(TableStyle.extras);

  private static class MySymbolTable extends UnicodeSet.XSymbolTable {
    XPropertyFactory factory;
    boolean skipFactory;

    public MySymbolTable(TableStyle style) {
      skipFactory = style == TableStyle.simple;
    }


    //    public boolean applyPropertyAlias0(String propertyName,
    //            String propertyValue, UnicodeSet result) {
    //      if (!propertyName.contains("*")) {
    //        return applyPropertyAlias(propertyName, propertyValue, result);
    //      }
    //      String[] propertyNames = propertyName.split("[*]");
    //      for (int i = propertyNames.length - 1; i >= 0; ++i) {
    //        String pname = propertyNames[i];
    //        
    //      }
    //      return null;
    //    }

    public boolean applyPropertyAlias(String propertyName,
            String propertyValue, UnicodeSet result) {
      boolean status = false;
      boolean invert = false;
      int opPos = propertyName.indexOf('\u2260');
      if (opPos != -1) {
        propertyValue = propertyValue.length() == 0 
        ? propertyName.substring(opPos+1) 
                : propertyName.substring(opPos+1) + "=" + propertyValue;
        propertyName = propertyName.substring(0,opPos);
        invert = true;
      } else if (propertyName.endsWith("!")) {
        propertyName = propertyName.substring(0, propertyName.length() - 1);
        invert = true;
      }
      propertyValue = propertyValue.trim();
      if (propertyValue.length() != 0) {
        status = applyPropertyAlias0(propertyName, propertyValue, result);
      } else {
        try {
          status = applyPropertyAlias0("gc", propertyName, result);
        } catch (Exception e) {};
        if (!status) {
          try {
            status = applyPropertyAlias0("sc", propertyName, result);
          } catch (Exception e) {};
          if (!status) {
            try {
              status = applyPropertyAlias0(propertyName, "t", result);
            } catch (Exception e) {};
            if (!status) {
              status = applyPropertyAlias0(propertyName, "", result);
            }
          }
        }
      }
      if (status && invert) {
        result.complement();
      }
      return status;
    }

    public boolean applyPropertyAlias0(String propertyName,
            String propertyValue, UnicodeSet result) {
      if (skipFactory) {
        return false;
      }
      result.clear();
      PatternMatcher patternMatcher = null;
      if (propertyValue.startsWith("/") && propertyValue.endsWith("/")) {
        patternMatcher = new UnicodeProperty.RegexMatcher().set(propertyValue.substring(1, propertyValue.length() - 1));
      }
      if (factory == null) {
        factory = XPropertyFactory.make();
      }
      boolean isAge = UnicodeProperty.equalNames("age", propertyName);
      UnicodeProperty prop = factory.getProperty(propertyName);
      if (prop != null) {
        UnicodeSet set;
        if (patternMatcher == null) {
          if (!prop.isValidValue(propertyValue)) {
            throw new IllegalArgumentException("The value '" + propertyValue + "' is illegal. Values for " + propertyName
                    + " must be in "
                    + prop.getAvailableValues() + " or in " + prop.getValueAliases());
          }
          if (isAge) {
            set = prop.getSet(new ComparisonMatcher(propertyValue, Relation.geq));
          } else {
            set = prop.getSet(propertyValue);
          }
        } else if (isAge) {
          set = new UnicodeSet();
          List<String> values = prop.getAvailableValues();
          for (String value : values) {
            if (patternMatcher.matches(value)) {
              for (String other : values) {
                if (other.compareTo(value) <= 0) {
                  set.addAll(prop.getSet(other));
                }
              }
            }
          }
        } else {
          set = prop.getSet(patternMatcher);
        }
        result.addAll(set);
        return true;
      }
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
      throw new IllegalArgumentException("Illegal property: " + propertyName);
      //      int propertyEnum;
      //      try {
      //        propertyEnum = UnicodeUtilities.getXPropertyEnum(propertyName);
      //      } catch (RuntimeException e) {
      //        return false;
      //      }
      //      Normalizer.Mode compat = null;
      //      //      if (trimmedPropertyValue.startsWith("*")) {
      //      //        compat = Normalizer.NFC;
      //      //        trimmedPropertyValue = trimmedPropertyValue.substring(1);
      //      //        if (trimmedPropertyValue.startsWith("*")) {
      //      //          compat = Normalizer.NFKC;
      //      //          trimmedPropertyValue = trimmedPropertyValue.substring(1);
      //      //        }
      //      //      }
      //      if (propertyValue.startsWith("/") && propertyValue.endsWith("/")) {
      //        Matcher matcher = Pattern.compile(
      //                propertyValue.substring(1, propertyValue.length() - 1)).matcher("");
      //        result.clear();
      //        boolean onlyOnce = propertyEnum >= UProperty.STRING_START
      //        && propertyEnum < UnicodeUtilities.XSTRING_LIMIT;
      //        for (UnicodeSetIterator it = new UnicodeSetIterator(UnicodeProperty.STUFF_TO_TEST); it.next();) {
      //          int cp = it.codepoint;
      //          for (int nameChoice = UProperty.NameChoice.SHORT; nameChoice <= UProperty.NameChoice.LONG; ++nameChoice) {
      //            String value = UnicodeUtilities.getXStringPropertyValue(propertyEnum, cp, nameChoice, compat);
      //            if (value == null) {
      //              continue;
      //            }
      //            if (matcher.reset(value).find()) {
      //              result.add(cp);
      //            }
      //            if (onlyOnce) {
      //              break;
      //            }
      //          }
      //        }
      //      } else if (propertyEnum >= UProperty.STRING_LIMIT
      //              && propertyEnum < UnicodeUtilities.XSTRING_LIMIT) {
      //        // support extra string routines
      //        String fixedPropertyValue = UnicodeUtilities.UNICODE.transform(propertyValue);
      //        for (UnicodeSetIterator it = new UnicodeSetIterator(UnicodeProperty.STUFF_TO_TEST); it.next();) {
      //          int cp = it.codepoint;
      //          String value = UnicodeUtilities.getXStringPropertyValue(propertyEnum, cp,
      //                  UProperty.NameChoice.SHORT, compat);
      //          if (fixedPropertyValue.equals(value)) {
      //            result.add(cp);
      //          }
      //        }
      //      } else if (compat != null) {
      //        int valueEnum = UCharacter.getPropertyValueEnum(propertyEnum, propertyValue);
      //        String fixedValue = UCharacter.getPropertyValueName(propertyEnum, valueEnum, UProperty.NameChoice.LONG);
      //        for (UnicodeSetIterator it = new UnicodeSetIterator(UnicodeProperty.STUFF_TO_TEST); it.next();) {
      //          int cp = it.codepoint;
      //          String value = UnicodeUtilities.getXStringPropertyValue(propertyEnum, cp, UProperty.NameChoice.LONG, compat);
      //          if (fixedValue.equals(value)) {
      //            result.add(cp);
      //          }
      //        }
      //      } else {
      //        return false;
      //      }
//      UnicodeProperty.addUntested(result);
//      return true;
    }

  };

  static String MyNormalize(int codepoint, Mode mode) {
    return Normalizer.normalize(codepoint, mode);
  }

  static String MyNormalize(String string, Mode mode) {
    return Normalizer.normalize(string, mode);
  }

  public static class ComparisonMatcher implements PatternMatcher {
    Relation relation;
    enum Relation {less, leq, equal, geq, greater}
    static Comparator comparator = new UTF16.StringComparator(true, false,0);

    String pattern;

    public ComparisonMatcher(String pattern, Relation comparator) {
      this.relation = comparator;
      this.pattern = pattern;
    }

    public boolean matches(Object value) {
      int comp = comparator.compare(pattern, value.toString());
      switch (relation) {
      case less: return comp < 0;
      case leq: return comp <= 0;
      default: return comp == 0;
      case geq: return comp >= 0;
      case greater: return comp > 0;
      }
    }

    public PatternMatcher set(String pattern) {
      this.pattern = pattern;
      return this;
    }
  }


}
