package org.unicode.text.tools;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.Counter;
import org.unicode.text.UCD.TestData;
import org.unicode.text.UCD.ToolUnicodePropertySource;
import org.unicode.text.UCD.UCD;
import org.unicode.text.UCD.UCD_Types;

import com.ibm.icu.dev.test.util.UnicodeProperty.Factory;

public class ShowUnicodeGrowth {
  enum Type {noncharacter, space, number, symbol, otherLetter, surrogate, privateUse, unassigned, punctuation, format, whitespace, combiningMark, hangul};
  static UCD ucd = UCD.make("");
  
  public static void main(String[] args) {
    TestData.countChars();
  } 
  void foo() {
    Map<Type,Counter<String>> map = new TreeMap();
    Set<String> ages = new TreeSet();
    //for (String s : AGE_VERSIONS)
    for (int i = 0; i <= 0x10FFFF; ++i) {
      String age = ucd.getAgeID(i);
      Type type = getType(i);
      Counter<String> counter = map.get(type);
      if (counter == null) {
        map.put(type, counter = new Counter<String>());
      }
      counter.increment(age);
      ages.add(age);
    }
    for (String age : ages) {
      System.out.print("\t" + age);
    }
    System.out.println();
    for (Type type : map.keySet()) {
      System.out.print(type);
      Counter<String> counter = map.get(type);
      for (String age : ages) {
        System.out.print("\t" + counter.getCount(age));
      }
      System.out.println();
    }
  }
  
  private static Type getType(int i) {
    if (ucd.isNoncharacter(i)) {
      return Type.noncharacter;
    } else {
      switch (ucd.getCategory(i)) {
        case UCD_Types.UNASSIGNED:
        case UCD_Types.UPPERCASE_LETTER:
        case UCD_Types.LOWERCASE_LETTER:
        case UCD_Types.TITLECASE_LETTER:
        case UCD_Types.MODIFIER_LETTER:
        case UCD_Types.OTHER_LETTER:
          byte script = ucd.getScript(i);
          if (script == ucd.HANGUL_SCRIPT) {
            return  Type.hangul;
          } else if (script == ucd.HAN_SCRIPT) {
            return  Type.hangul;
          } else {
            return  Type.otherLetter;
          }

        case UCD_Types.NON_SPACING_MARK:
        case UCD_Types.ENCLOSING_MARK:
        case UCD_Types.COMBINING_SPACING_MARK:
          return  Type.combiningMark;  
        case UCD_Types.DECIMAL_DIGIT_NUMBER:
        case UCD_Types.LETTER_NUMBER:
        case UCD_Types.OTHER_NUMBER:
          return  Type.number;  
        case UCD_Types.SPACE_SEPARATOR:
        case UCD_Types.LINE_SEPARATOR:
        case UCD_Types.PARAGRAPH_SEPARATOR:
          return  Type.whitespace;  
        case UCD_Types.CONTROL:
        case UCD_Types.FORMAT:
          return  Type.format;  
        case UCD_Types.UNUSED_CATEGORY:
        case UCD_Types.PRIVATE_USE:
          return  Type.privateUse;  
        case UCD_Types.SURROGATE:
          return  Type.surrogate;  
        case UCD_Types.DASH_PUNCTUATION:
        case UCD_Types.START_PUNCTUATION:
        case UCD_Types.END_PUNCTUATION:
        case UCD_Types.CONNECTOR_PUNCTUATION:
        case UCD_Types.OTHER_PUNCTUATION:
        case UCD_Types.INITIAL_PUNCTUATION:
        case UCD_Types.FINAL_PUNCTUATION:
          return  Type.punctuation;  
        case UCD_Types.MATH_SYMBOL:
        case UCD_Types.CURRENCY_SYMBOL:
        case UCD_Types.MODIFIER_SYMBOL:
        case UCD_Types.OTHER_SYMBOL:
          return  Type.otherLetter;  
      }
    }
    return null;
  }
}
