package jsp;

import org.unicode.cldr.icu.PrettyPrinter;

import com.ibm.icu.text.*;
import com.ibm.icu.lang.*;
import com.ibm.icu.util.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.ParsePosition;
import java.util.Arrays;

public class UnicodeUtilities {
  public static void main(String[] args) throws IOException {
    
    String[] abResults = new String[3];
    int[] abSizes = new int[3];
    UnicodeUtilities.getDifferences("[:letter:]", "[:idna:]", false, abResults, abSizes);
    for (int i = 0; i < abResults.length; ++i) {
      System.out.println(abSizes[i] + "\t" + abResults[i]);
    }
    
    final PrintWriter printWriter = new PrintWriter(System.out);
    final UnicodeSet unicodeSet = new UnicodeSet();
    System.out.println("simple: " + UnicodeUtilities.getSimpleSet("[a-bm-p\uAc00]", unicodeSet, true));
    showSet(unicodeSet, true, printWriter);
    printWriter.flush();
    test("[:idna:]");
    test("[:idna=ignored:]");
    test("[:idna=remapped:]");
    test("[:idna=disallowed:]");
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
        + "([[:C:][:Z:][:whitespace:][:Default_Ignorable_Code_Point:]-[\\u0020]]) > &hex/xml($1) ; "; // [\\u0080-\\U0010FFFF]

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

  static final int OUTPUT = 0, IGNORED = 1, REMAPPED = 2, DISALLOWED = 3, 
      IDNA_TYPE_LIMIT = 4;

  static final String[] IdnaNames = { "OUTPUT", "IGNORED", "REMAPPED", "DISALLOWED" };

  static UnicodeSet idnaTypeSet[] = new UnicodeSet[IDNA_TYPE_LIMIT];
  static {
    for (int i = 0; i < idnaTypeSet.length; ++i)
      idnaTypeSet[i] = new UnicodeSet();
  }

  /**
   * 
   */
  static public int getIDNAType(int cp) {
    if (cp == '-')
      return OUTPUT;
    inbuffer.setLength(0);
    UTF16.append(inbuffer, cp);
    try {
      intermediate = IDNA.convertToASCII(inbuffer, IDNA.USE_STD3_RULES); // USE_STD3_RULES,
                                                                          // DEFAULT
      if (intermediate.length() == 0)
        return IGNORED;
      outbuffer = IDNA.convertToUnicode(intermediate, IDNA.USE_STD3_RULES);
    } catch (StringPrepParseException e) {
      if (e.getMessage().startsWith("Found zero length")) {
        return IGNORED;
      }
      return DISALLOWED;
    } catch (Exception e) {
      System.out.println("Failure at: " + Integer.toString(cp, 16));
      return DISALLOWED;
    }
    if (!equals(inbuffer, outbuffer))
      return REMAPPED;
    return OUTPUT;
  }

  static StringBuffer inbuffer = new StringBuffer();

  static StringBuffer intermediate, outbuffer;

  static {
    for (int cp = 0; cp <= 0x10FFFF; ++cp) {

      int cat = UCharacter.getType(cp);
      if (cat == Character.UNASSIGNED || cat == Character.PRIVATE_USE) {
        idnaTypeSet[DISALLOWED].add(cp); // faster
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
    isCased = new UnicodeSet(isLowercase).retainAll(isUppercase).retainAll(
        isTitlecase).complement();
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
          "PropertyValue must be empty (= OUTPUT) or one of: "
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
    if (inbuffer.length() != outbuffer.length())
      return false;
    for (int i = inbuffer.length() - 1; i >= 0; --i) {
      if (inbuffer.charAt(i) != outbuffer.charAt(i))
        return false;
    }
    return true;
  }

  public static void showSet(UnicodeSet a, boolean abbreviate, Writer out) throws IOException {
    if (a.size() < 20000 && !abbreviate) {
      for (UnicodeSetIterator it = new UnicodeSetIterator(a); it.next();) {
        int s = it.codepoint;
        showCodePoint(s, out);
      }
    } else if (a.getRangeCount() < 10000) {
      for (UnicodeSetIterator it = new UnicodeSetIterator(a); it.nextRange();) {
        int s = it.codepoint;
        int end = it.codepointEnd;
        showCodePoint(s, out);
        if (end != s) {
          if (end > s + 1) {
            out.write("\u2026 ");
          }
          showCodePoint(end, out);
        }
      }
    } else {
      out.write("<i>Too many to list individually</i>\r\n");
    }
  }

  private static void showCodePoint(int s, Writer out) throws IOException {
    String literal = toHTML.transliterate(UTF16.valueOf(s));
    String hex = com.ibm.icu.impl.Utility.hex(s, 4);
    String name = UCharacter.getExtendedName(s);
    if (name == null || name.length() == 0) {
      name = "<i>no name</i>";
    }
    out.write("<code><a target='c' href='character.jsp?a=" + hex + "'>U+"
        + hex + "</a></code> ( " + literal + " ) " + name + "<br>\r\n");
  }

  public static String getSimpleSet(String setA, UnicodeSet a, boolean abbreviate) {
    String a_out;
    a.clear();
    try {
      setA = Normalizer.normalize(setA, Normalizer.NFC);
      a.addAll(parseUnicodeSet(setA));
      a_out = getPrettySet(a, abbreviate);
    } catch (Exception e) {
      a_out = e.getMessage();
    }
    return a_out;
  }

  private static String getPrettySet(UnicodeSet a, boolean abbreviate) {
    String a_out;
    if (a.size() < 10000 && !abbreviate) {
      PrettyPrinter pp = new PrettyPrinter();
      a_out = toHTML(pp.toPattern(a));
    } else {
      a.complement().complement();
      a_out = toHTML(a.toPattern(false));
    }
    // insert spaces occasionally
    int cp;
    int oldCp = 0;
    StringBuffer out = new StringBuffer();
    int charCount = 0;
    for (int i = 0; i < a_out.length(); i+= UTF16.getCharCount(cp)) {
      cp = UTF16.charAt(a_out, i);
      ++charCount;
      if (charCount > 20) {
        if (cp != '-' && oldCp != '-') {
          out.append(' ');
          charCount = 0;
        }
      }
      UTF16.append(out, cp);
      oldCp = cp;
    }
    return out.toString();
  }
  
  public static UnicodeSet  parseSimpleSet(String setA, String[] exceptionMessage) {
    try {
      exceptionMessage[0] = null;
      setA = Normalizer.normalize(setA, Normalizer.NFC);
      return parseUnicodeSet(setA);
    } catch (Exception e) {
      exceptionMessage[0] = e.getMessage();
    }
    return null;
  }
  
  public static void getDifferences(String setA, String setB,
      boolean abbreviate, String[] abResults, int[] abSizes) {
    
    String[] aMessage = new String[1];
    String[] bMessage = new String[1];
    UnicodeSet a = UnicodeUtilities.parseSimpleSet(setA, aMessage);
    UnicodeSet b = UnicodeUtilities.parseSimpleSet(setB, bMessage);

    String a_b;
    String b_a;
    String ab;

    // try {
    // setA = Normalizer.normalize(setA, Normalizer.NFC);
    // a = UnicodeUtilities.parseUnicodeSet(setA);
    // } catch (Exception e) {
    // a_b = e.getMessage();
    // }
    // UnicodeSet b = null;
    // try {
    // setB = Normalizer.normalize(setB, Normalizer.NFC);
    // b = UnicodeUtilities.parseUnicodeSet(setB);
    // } catch (Exception e) {
    // b_a = e.getMessage();
    // }
    int a_bSize = 0, b_aSize = 0, abSize = 0;
    if (a == null || b == null) {
      a_b = a == null ? aMessage[0] : "error" ;
      b_a = b == null ? bMessage[0] : "error" ;
      ab = "error";
    } else  {
      UnicodeSet temp = new UnicodeSet(a).removeAll(b);
      a_bSize = temp.size();
      a_b = getPrettySet(temp, abbreviate);

      temp = new UnicodeSet(b).removeAll(a);
      b_aSize = temp.size();
      b_a = getPrettySet(temp, abbreviate);

      temp = new UnicodeSet(a).retainAll(b);
      abSize = temp.size();
      ab = getPrettySet(temp, abbreviate);
    }
    abResults[0] = a_b;
    abSizes[0] = a_bSize;
    abResults[1] = b_a;
    abSizes[1] = b_aSize;
    abResults[2] = ab;
    abSizes[2] = abSize;
  }
}
/*
 * <% http://www.devshed.com/c/a/Java/Developing-JavaServer-Pages/ Enumeration
 * parameterNames = request.getParameterNames(); while
 * (parameterNames.hasMoreElements()){ String parameterName = (String)
 * parameterNames.nextElement(); String parameterValue =
 * request.getParameter(parameterName); %> <%= parameterName %> has value <%=
 * parameterValue %>. <br> <% } %>
 */