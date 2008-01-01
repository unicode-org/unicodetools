package jsp;

import com.ibm.icu.text.*;
import com.ibm.icu.lang.*;
import com.ibm.icu.util.*;

import java.text.ParsePosition;
import java.util.Arrays;

public class UnicodeUtilities {
  public static void main(String[] args) {
    test("[:idna:]");
    test("[:idna=deleted:]");
    test("[:idna=remapped:]");
    test("[:idna=illegal:]");
    test("[:iscased:]");
  }

  private static void test(String testString) {
    UnicodeSet tc1 = parseUnicodeSet(testString);
    System.out.println(tc1 + "\t=\t" + tc1.complement().complement());
  }

  static Transliterator toHTML;
  static {

    String BASE_RULES = "'<' > '&lt;' ;" + "'<' < '&'[lL][Tt]';' ;"
        + "'&' > '&amp;' ;" + "'&' < '&'[aA][mM][pP]';' ;"
        + "'>' < '&'[gG][tT]';' ;" + "'\"' < '&'[qQ][uU][oO][tT]';' ; "
        + "'' < '&'[aA][pP][oO][sS]';' ; ";

    String CONTENT_RULES = "'>' > '&gt;' ;";

    String HTML_RULES = BASE_RULES + CONTENT_RULES + "'\"' > '&quot;' ; ";

    String HTML_RULES_CONTROLS = HTML_RULES
        + "([[:C:][:Z:][:whitespace:][:Default_Ignorable_Code_Point:][\\u0080-\\U0010FFFF]]) > &hex/xml($1) ; ";

    toHTML = Transliterator.createFromRules("any-xml", HTML_RULES_CONTROLS,
        Transliterator.FORWARD);
  }

  public static String toHTML(String input) {
    return toHTML.transliterate(input);
  }

  static UnicodeSet isCaseFolded = new UnicodeSet();

  static UnicodeSet isLowercase = new UnicodeSet();

  static UnicodeSet isUppercase = new UnicodeSet();

  static UnicodeSet isTitlecase = new UnicodeSet();
  
  static UnicodeSet isCased = new UnicodeSet();

  static final int OK = 0, DELETED = 1, ILLEGAL = 2, REMAPPED = 3,
      IDNA_TYPE_LIMIT = 4;

  static final String[] IdnaNames = { "OK ", "DELETED", "ILLEGAL", "REMAPPED" };

  static UnicodeSet idnaTypeSet[] = new UnicodeSet[IDNA_TYPE_LIMIT];
  static {
    for (int i = 0; i < idnaTypeSet.length; ++i)
      idnaTypeSet[i] = new UnicodeSet();
  }

  /**
   * 
   */
  static public int getIDNAType(int cp) {
    if (cp == '-') return OK;
    inbuffer.setLength(0);
    UTF16.append(inbuffer, cp);
    try {
      intermediate = IDNA.convertToASCII(inbuffer, IDNA.USE_STD3_RULES); // USE_STD3_RULES, DEFAULT
      if (intermediate.length() == 0)
        return DELETED;
      outbuffer = IDNA.convertToUnicode(intermediate, IDNA.USE_STD3_RULES);
    } catch (StringPrepParseException e) {
      if (e.getMessage().startsWith("Found zero length")) {
        return DELETED;
      }
      return ILLEGAL;
    } catch (Exception e) {
      System.out.println("Failure at: " + Integer.toString(cp, 16));
      return ILLEGAL;
    }
    if (!equals(inbuffer, outbuffer))
      return REMAPPED;
    return OK;
  }

  static StringBuffer inbuffer = new StringBuffer();

  static StringBuffer intermediate, outbuffer;

  static {
    for (int cp = 0; cp <= 0x10FFFF; ++cp) {

      int cat = UCharacter.getType(cp);
      if (cat == Character.UNASSIGNED || cat == Character.PRIVATE_USE) {
        idnaTypeSet[ILLEGAL].add(cp); // faster
        isCaseFolded.add(cp);
        isLowercase.add(cp);
        isTitlecase.add(cp);
        isUppercase.add(cp);
        continue;
      }
      
      int idnaType = getIDNAType(cp);
      idnaTypeSet[idnaType].add(cp);

      String s = UTF16.valueOf(cp);
      if (UCharacter.foldCase(s, true).equals(s)) {
        isCaseFolded.add(cp);
      }
      if (UCharacter.toLowerCase(ULocale.ROOT, s).equals(s)) {
        isLowercase.add(cp);
      }
      if (UCharacter.toUpperCase(ULocale.ROOT, s).equals(s)) {
        isUppercase.add(cp);
      }
      if (UCharacter.toTitleCase(ULocale.ROOT, s, null).equals(s)) {
        isTitlecase.add(cp);
      }
    }
    // isCased if isLowercase=false OR isUppercase=false OR isTitlecase=false
    // or := ! (isLowercase && isUppercase && isTitlecase)
    isCased = new UnicodeSet(isLowercase).retainAll(isUppercase).retainAll(isTitlecase).complement();
  }

  static UnicodeSet.XSymbolTable myXSymbolTable = new UnicodeSet.XSymbolTable() {
    public boolean applyPropertyAlias(String propertyName,
        String propertyValue, UnicodeSet result) {
      if (propertyName.equalsIgnoreCase("idna")) {
        return getIdnaProperty(propertyName, propertyValue, result);
      }
      if (propertyName.equalsIgnoreCase("isCaseFolded")) {
        result.clear().addAll(isCaseFolded);
        if (getBinaryValue(propertyValue)) {
          result.complement();
        }
        return true;
      }
      if (propertyName.equalsIgnoreCase("isLowercase")) {
        result.clear().addAll(isLowercase);
        if (getBinaryValue(propertyValue)) {
          result.complement();
        }
        return true;
      }
      if (propertyName.equalsIgnoreCase("isUppercase")) {
        result.clear().addAll(isUppercase);
        if (getBinaryValue(propertyValue)) {
          result.complement();
        }
        return true;
      }
      if (propertyName.equalsIgnoreCase("isTitlecase")) {
        result.clear().addAll(isTitlecase);
        if (getBinaryValue(propertyValue)) {
          result.complement();
        }
        return true;
      }
      if (propertyName.equalsIgnoreCase("isCased")) {
        result.clear().addAll(isCased);
        if (getBinaryValue(propertyValue)) {
          result.complement();
        }
        return true;
      }
      return false;
    }
  };

  public static UnicodeSet parseUnicodeSet(String input) {
    ParsePosition parsePosition = new ParsePosition(0);
    if (!input.startsWith("[")) {
      input = "[" + input + "]";
    }
    return new UnicodeSet(input, parsePosition, myXSymbolTable);
  }

  protected static boolean getIdnaProperty(String propertyName,
      String propertyValue, UnicodeSet result) {
    int i = 0;
    if (propertyValue.length() != 0) {
      for (; i < IdnaNames.length; ++i) {
        if (propertyValue.equalsIgnoreCase(IdnaNames[i])) {
          break;
        }
      }
    }
    if (i >= IdnaNames.length) {
      throw new IllegalArgumentException(
          "PropertyValue must be empty (= OK) or one of: "
              + Arrays.asList(IdnaNames));
    }
    result.clear().addAll(idnaTypeSet[i]);
    return true;
  }

  private static boolean getBinaryValue(String propertyValue) {
    boolean invert;
    if (propertyValue.length() == 0 || propertyValue.equalsIgnoreCase("true")
        || propertyValue.equalsIgnoreCase("t")
        || propertyValue.equalsIgnoreCase("yes")
        || propertyValue.equalsIgnoreCase("y")) {
      invert = false;
    } else if (propertyValue.equalsIgnoreCase("false")
        || propertyValue.equalsIgnoreCase("f")
        || propertyValue.equalsIgnoreCase("no")
        || propertyValue.equalsIgnoreCase("n")) {
      invert = true;
    } else {
      throw new IllegalArgumentException(
          "PropertyValue must be empty (= T) or one of: True, T, False, F");
    }
    return invert;
  }
  
  public static boolean equals(CharSequence inbuffer, CharSequence outbuffer) {
    if (inbuffer.length() != outbuffer.length()) return false;
    for (int i = inbuffer.length() - 1; i >= 0; --i) {
      if (inbuffer.charAt(i) != outbuffer.charAt(i)) return false;
    }
    return true;
  }
}