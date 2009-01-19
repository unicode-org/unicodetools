package jsp;

import org.unicode.cldr.icu.PrettyPrinter;

import com.ibm.icu.text.*;
import com.ibm.icu.lang.*;
import com.ibm.icu.util.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.ParsePosition;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UnicodeUtilities {
  
  private static final List<String> REGEX_PROPS = Arrays.asList(new String[] {"xdigit", "alnum", "blank", "graph", "print", "word"});

  private static final List<String> UNICODE_PROPS = Arrays.asList(new String[] {
              "Numeric_Value", "Bidi_Mirroring_Glyph", "Case_Folding",
              "Decomposition_Mapping", "FC_NFKC_Closure",
              "Lowercase_Mapping", "Special_Case_Condition",
              "Simple_Case_Folding", "Simple_Lowercase_Mapping",
              "Simple_Titlecase_Mapping", "Simple_Uppercase_Mapping",
              "Titlecase_Mapping", "Uppercase_Mapping", "ISO_Comment",
              "Name", "Unicode_1_Name", "Unicode_Radical_Stroke", "Age",
              "Block", "Script", "Bidi_Class", "Canonical_Combining_Class",
              "Decomposition_Type", "East_Asian_Width", "General_Category",
              "Grapheme_Cluster_Break", "Hangul_Syllable_Type",
              "Joining_Group", "Joining_Type", "Line_Break",
              "NFC_Quick_Check", "NFD_Quick_Check", "NFKC_Quick_Check",
              "NFKD_Quick_Check", "Numeric_Type", "Sentence_Break",
              "Word_Break", "ASCII_Hex_Digit", "Alphabetic", "Bidi_Control",
              "Bidi_Mirrored", "Composition_Exclusion",
              "Full_Composition_Exclusion", "Dash", "Deprecated",
              "Default_Ignorable_Code_Point", "Diacritic", "Extender",
              "Grapheme_Base", "Grapheme_Extend", "Grapheme_Link",
              "Hex_Digit", "Hyphen", "ID_Continue", "Ideographic",
              "ID_Start", "IDS_Binary_Operator", "IDS_Trinary_Operator",
              "Join_Control", "Logical_Order_Exception", "Lowercase", "Math",
              "Noncharacter_Code_Point", "Other_Alphabetic",
              "Other_Default_Ignorable_Code_Point", "Other_Grapheme_Extend",
              "Other_ID_Continue", "Other_ID_Start", "Other_Lowercase",
              "Other_Math", "Other_Uppercase", "Pattern_Syntax",
              "Pattern_White_Space", "Quotation_Mark", "Radical",
              "Soft_Dotted", "STerm", "Terminal_Punctuation",
              "Unified_Ideograph", "Uppercase", "Variation_Selector",
              "White_Space", "XID_Continue", "XID_Start", "Expands_On_NFC",
              "Expands_On_NFD", "Expands_On_NFKC", "Expands_On_NFKD",
              "toNFC", "toNFD", "toNFKC", "toNFKD"});
  /*
   * Arrays.asList(new String[] {
        "Numeric_Value", "Bidi_Mirroring_Glyph", "Case_Folding",
        "Decomposition_Mapping", "FC_NFKC_Closure", "Lowercase_Mapping",
        "Special_Case_Condition", "Simple_Case_Folding",
        "Simple_Lowercase_Mapping", "Simple_Titlecase_Mapping",
        "Simple_Uppercase_Mapping", "Titlecase_Mapping", "Uppercase_Mapping",
        "ISO_Comment", "Name", "Unicode_1_Name", "Unicode_Radical_Stroke",
        "Age", "Block", "Script", "Bidi_Class", "Canonical_Combining_Class",
        "Decomposition_Type", "East_Asian_Width", "General_Category",
        "Grapheme_Cluster_Break", "Hangul_Syllable_Type", "Joining_Group",
        "Joining_Type", "Line_Break", "NFC_Quick_Check", "NFD_Quick_Check",
        "NFKC_Quick_Check", "NFKD_Quick_Check", "Numeric_Type",
        "Sentence_Break", "Word_Break", "ASCII_Hex_Digit", "Alphabetic",
        "Bidi_Control", "Bidi_Mirrored", "Composition_Exclusion",
        "Full_Composition_Exclusion", "Dash", "Deprecated",
        "Default_Ignorable_Code_Point", "Diacritic", "Extender",
        "Grapheme_Base", "Grapheme_Extend", "Grapheme_Link", "Hex_Digit",
        "Hyphen", "ID_Continue", "Ideographic", "ID_Start",
        "IDS_Binary_Operator", "IDS_Trinary_Operator", "Join_Control",
        "Logical_Order_Exception", "Lowercase", "Math",
        "Noncharacter_Code_Point", "Other_Alphabetic",
        "Other_Default_Ignorable_Code_Point", "Other_Grapheme_Extend",
        "Other_ID_Continue", "Other_ID_Start", "Other_Lowercase", "Other_Math",
        "Other_Uppercase", "Pattern_Syntax", "Pattern_White_Space",
        "Quotation_Mark", "Radical", "Soft_Dotted", "STerm",
        "Terminal_Punctuation", "Unified_Ideograph", "Uppercase",
        "Variation_Selector", "White_Space", "XID_Continue", "XID_Start",
        "Expands_On_NFC", "Expands_On_NFD", "Expands_On_NFKC",
        "Expands_On_NFKD", "toNFC", "toNFD", "toNFKC", "toNFKD", })
   */

  final static Subheader subheader;
  static {
    try {
      final String unicodeDataDirectory = "./jsp/";
      System.out.println(new File(unicodeDataDirectory).getCanonicalPath());
      subheader = new Subheader(unicodeDataDirectory);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      throw new IllegalArgumentException();
    }
  }
  
  public static void main(String[] args) throws IOException {
    final PrintWriter printWriter = new PrintWriter(System.out);
    
    
    //showIDNARemapDifferences(printWriter);
 
    expectError("][:idna=output:][abc]");
    
    if (!parseUnicodeSet("[:idna=output:]").contains('-')) {
      System.out.println("FAILURE");
    }

    test("[:subhead=/Mayanist/:]");
    test("abc-m");

    test("[:toNFKC=a:]");
    test("[:isNFC=false:]");
    test("[:toNFD=A\u0300:]");
    test("[:toLowercase= /a/ :]");
    test("[:toLowercase= /a/ :]");
    test("[:ASCII:]");
    test("[:lowercase:]");
    test("[:toNFC=/\\./:]");
    test("[:toNFKC=/\\./:]");
    test("[:toNFD=/\\./:]");
    test("[:toNFKD=/\\./:]");
    test("[:toLowercase=/a/:]");
    test("[:toUppercase=/A/:]");
    test("[:toCaseFold=/a/:]");
    test("[:toTitlecase=/A/:]");
     printWriter.flush();
    
    //if (true) return;
    
    showSet(new UnicodeSet("[\\u0080\\U0010FFFF]"), true, true, printWriter);
    printWriter.flush();
    
    
    test("[:name=/WITH/:]");
    showProperties("a", printWriter);
    printWriter.flush();
    
    String[] abResults = new String[3];
    String[] abLinks = new String[3];
    int[] abSizes = new int[3];
    UnicodeUtilities.getDifferences("[:letter:]", "[:idna:]", false, abResults, abSizes, abLinks);
    for (int i = 0; i < abResults.length; ++i) {
      System.out.println(abSizes[i] + "\r\n\t" + abResults[i] + "\r\n\t" + abLinks[i]);
    }
    
    final UnicodeSet unicodeSet = new UnicodeSet();
    System.out.println("simple: " + UnicodeUtilities.getSimpleSet("[a-bm-p\uAc00]", unicodeSet, true));
    showSet(unicodeSet, true, true, printWriter);
    printWriter.flush();
    test("[:idna:]");
    test("[:idna=ignored:]");
    test("[:idna=remapped:]");
    test("[:idna=disallowed:]");
    test("[:iscased:]");
    String archaic = "[[\u018D\u01AA\u01AB\u01B9-\u01BB\u01BE\u01BF\u021C\u021D\u025F\u0277\u027C\u029E\u0343\u03D0\u03D1\u03D5-\u03E1\u03F7-\u03FB\u0483-\u0486\u05A2\u05C5-\u05C7\u066E\u066F\u068E\u0CDE\u10F1-\u10F6\u1100-\u115E\u1161-\u11FF\u17A8\u17D1\u17DD\u1DC0-\u1DC3\u3165-\u318E\uA700-\uA707\\U00010140-\\U00010174]" +
    "[\u02EF-\u02FF\u0363-\u0373\u0376\u0377\u07E8-\u07EA\u1DCE-\u1DE6\u1DFE\u1DFF\u1E9C\u1E9D\u1E9F\u1EFA-\u1EFF\u2056\u2058-\u205E\u2180-\u2183\u2185-\u2188\u2C77-\u2C7D\u2E00-\u2E17\u2E2A-\u2E30\uA720\uA721\uA730-\uA778\uA7FB-\uA7FF]" +
    "[\u0269\u027F\u0285-\u0287\u0293\u0296\u0297\u029A\u02A0\u02A3\u02A5\u02A6\u02A8-\u02AF\u0313\u037B-\u037D\u03CF\u03FD-\u03FF]" +
"";
    showSet(parseUnicodeSet("[:archaic=/.+/:]"),false, false, printWriter);
    printWriter.flush();
    showPropsTable(printWriter);
    printWriter.flush();

  }

  private static void expectError(String input) {
    try {
      parseUnicodeSet(input);
      System.out.println("Failure to detect syntax error.");
    } catch (IllegalArgumentException e) {
      System.out.println("Expected error: " + e.getMessage());
    }
  }
  
  private static void showIDNARemapDifferences(Writer out) throws IOException {
    UnicodeSet diffs = new UnicodeSet();
    for (int cp = 0; cp <= 0x10FFFF; ++cp) {
      if (cp >= 'A' && cp <= 'Z') {
        continue;
      }
      int cat = UCharacter.getType(cp);
      if (cat == UCharacter.UNASSIGNED || cat == UCharacter.PRIVATE_USE  || cat == UCharacter.SURROGATE) {
        continue;
      }
      String idna = getIDNAValue(cp);
      if (idna == null) continue;
      if (idna.length() == 0) continue;
      String cased0 = UCharacter.foldCase(UTF16.valueOf(cp), true);
      String folded = Normalizer.normalize(cased0, Normalizer.NFKC, 0);
      String cased = UCharacter.foldCase(folded, true);
      String folded2 = Normalizer.normalize(cased, Normalizer.NFKC, 0);
      String cased2 = UCharacter.foldCase(folded2, true);
      if (!cased2.equals(folded2) && !cased2.equals(cased)) {
        out.write("ERROR:\r\n");
        showCodePoint(cp, true, out);
        out.write("folded:\r\n");
        showString(folded, "\t", true, out);
        out.write("cased:\r\n");
        showString(cased, "\t", true, out);
        out.write("folded2:\r\n");
        showString(folded2, "\t", true, out);
        out.write("cased2:\r\n");
        showString(cased2, "\t", true, out);
        out.flush();
        //throw new IllegalArgumentException("Internal error!!!" + Integer.toString(cp,16) + " " + UCharacter.getName(cp));
      }
      String casedAndNFKC = folded2;
      if (!idna.equals(casedAndNFKC)) {
        out.write("Source: ");
        showCodePoint(cp, true, out);
        out.write("IDNA2003: ");
        showString(idna, ", ", true, out);
        out.write("Case-NFKC: ");
        showString(casedAndNFKC, ", ", true, out);
        out.write("<br>\r\n");
        out.flush();
        diffs.add(cp);
      }
    }
    out.write(diffs.toString());
    out.flush();
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
    + "[[:di:]-[:cc:]-[:cs:]-[\\u200E\\u200F]] > ; " // remove, should ignore in rendering (but may not be in browser)
    + "[[:nchar:][:cn:][:cs:][:co:][:cc:]-[:whitespace:]-[\\u200E\\u200F]] > \\uFFFD ; "; // should be missing glyph (but may not be in browser)
   //     + "([[:C:][:Z:][:whitespace:][:Default_Ignorable_Code_Point:]-[\\u0020]]) > &hex/xml($1) ; "; // [\\u0080-\\U0010FFFF]

    toHTML = Transliterator.createFromRules("any-xml", HTML_RULES_CONTROLS,
        Transliterator.FORWARD);
  }

  public static String toHTML(String input) {
    return toHTML.transliterate(input);
  }
  
  static Transliterator UNICODE = Transliterator.getInstance("hex-any");

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
  
  static public String getIDNAValue(int cp) {
    if (cp == '-')
      return "-";
    inbuffer.setLength(0);
    UTF16.append(inbuffer, cp);
    try {
      intermediate = IDNA.convertToASCII(inbuffer, IDNA.USE_STD3_RULES); // USE_STD3_RULES,
      // DEFAULT
      if (intermediate.length() == 0)
        return "";
      outbuffer = IDNA.convertToUnicode(intermediate, IDNA.USE_STD3_RULES);
    } catch (StringPrepParseException e) {
      if (e.getMessage().startsWith("Found zero length")) {
        return "";
      }
      return null;
    } catch (Exception e) {
      System.out.println("Failure at: " + Integer.toString(cp, 16));
      return null;
    }
    return outbuffer.toString();
  }

  static StringBuffer inbuffer = new StringBuffer();

  static StringBuffer intermediate, outbuffer;

  static {
    for (int cp = 0; cp <= 0x10FFFF; ++cp) {

      int cat = UCharacter.getType(cp);
      if (cat == UCharacter.UNASSIGNED || cat == UCharacter.PRIVATE_USE  || cat == UCharacter.SURROGATE) {
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
  
  static Object[][] specialProperties = {
    {"isCaseFolded", isCaseFolded},
    {"isUppercase", isUppercase},
    {"isLowercase", isLowercase},
    {"isTitlecase", isTitlecase},
    {"isCased", isCased},
    {"isNFC", new UnicodeSet("[:^nfcqc=n:]")},
    {"isNFD", new UnicodeSet("[:^nfdqc=n:]")},
    {"isNFKC", new UnicodeSet("[:^nfkcqc=n:]")},
    {"isNFKD", new UnicodeSet("[:^nfkdqc=n:]")},
    };

  static UnicodeSet.XSymbolTable myXSymbolTable = new UnicodeSet.XSymbolTable() {
    public boolean applyPropertyAlias(String propertyName,
        String propertyValue, UnicodeSet result) {
      if (propertyName.equalsIgnoreCase("idna")) {
        return getIdnaProperty(propertyName, propertyValue, result);
      }
      for (int i = 0; i < specialProperties.length; ++i) {
        if (propertyName.equalsIgnoreCase((String) specialProperties[i][0])) {
          result.clear().addAll((UnicodeSet) specialProperties[i][1]);
          if (getBinaryValue(propertyValue)) {
            result.complement();
          }
          return true;
        }
      }
      int propertyEnum;
      try {
        propertyEnum = getXPropertyEnum(propertyName);
      } catch (RuntimeException e) {
        return false;
      }
      String trimmedPropertyValue = propertyValue.trim();
      if (trimmedPropertyValue.startsWith("/") && trimmedPropertyValue.endsWith("/")) {
        Matcher matcher = Pattern.compile(
            trimmedPropertyValue.substring(1, trimmedPropertyValue.length() - 1)).matcher("");
        result.clear();
        boolean onlyOnce = propertyEnum >= UProperty.STRING_START
            && propertyEnum < XSTRING_LIMIT;
        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
          int cat = UCharacter.getType(cp);
          if (cat == UCharacter.UNASSIGNED || cat == UCharacter.PRIVATE_USE
              || cat == UCharacter.SURROGATE) {
            continue;
          }
          for (int nameChoice = UProperty.NameChoice.SHORT; nameChoice <= UProperty.NameChoice.LONG; ++nameChoice) {
            String value = getXStringPropertyValue(propertyEnum, cp, nameChoice);
            if (value == null) {
              continue;
            }
            if (matcher.reset(value).find()) {
              result.add(cp);
            }
            if (onlyOnce)
              break;
          }
        }
        return true;
      } else if (propertyEnum >= UProperty.STRING_LIMIT
          && propertyEnum < XSTRING_LIMIT) {
        // support extra string routines
        String fixedPropertyValue = UNICODE.transform(propertyValue);
        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
          int cat = UCharacter.getType(cp);
          if (cat == UCharacter.UNASSIGNED || cat == UCharacter.PRIVATE_USE
              || cat == UCharacter.SURROGATE) {
            continue;
          }
          String value = getXStringPropertyValue(propertyEnum, cp,
              UProperty.NameChoice.SHORT);
          if (fixedPropertyValue.equals(value)) {
            result.add(cp);
          }
        }
        return true;
      }
      return false;
    }
  };
  
  static final int 
  TO_NFC = UProperty.STRING_LIMIT,
  TO_NFD = UProperty.STRING_LIMIT + 1,
  TO_NFKC = UProperty.STRING_LIMIT + 2,
  TO_NFKD = UProperty.STRING_LIMIT + 3,
  TO_CASEFOLD  = UProperty.STRING_LIMIT + 4,
  TO_LOWERCASE  = UProperty.STRING_LIMIT + 5,
  TO_UPPERCASE  = UProperty.STRING_LIMIT + 6,
  TO_TITLECASE  = UProperty.STRING_LIMIT + 7,
  SUBHEAD = TO_TITLECASE + 1,
  ARCHAIC = SUBHEAD + 1,
  XSTRING_LIMIT = ARCHAIC + 1;
  
  static List<String> XPROPERTY_NAMES = Arrays.asList(new String[]{"tonfc", "tonfd", "tonfkc", "tonfkd", "tocasefold", "tolowercase", "touppercase", "totitlecase", "subhead", "archaic"});
  
  static String getXStringPropertyValue(int propertyEnum, int codepoint, int nameChoice) {
    switch (propertyEnum) {
      case TO_NFC: return Normalizer.normalize(codepoint, Normalizer.NFC, 0);
      case TO_NFD: return Normalizer.normalize(codepoint, Normalizer.NFD, 0);
      case TO_NFKC: return Normalizer.normalize(codepoint, Normalizer.NFKC, 0);
      case TO_NFKD: return Normalizer.normalize(codepoint, Normalizer.NFKD, 0);
      case TO_CASEFOLD: return UCharacter.foldCase(UTF16.valueOf(codepoint), true);
      case TO_LOWERCASE: return UCharacter.toLowerCase(ULocale.ROOT, UTF16.valueOf(codepoint));
      case TO_UPPERCASE: return UCharacter.toUpperCase(ULocale.ROOT, UTF16.valueOf(codepoint));
      case TO_TITLECASE: return UCharacter.toTitleCase(ULocale.ROOT, UTF16.valueOf(codepoint), null);
      case SUBHEAD: return subheader.getSubheader(codepoint);
      case ARCHAIC: return ScriptCategories.ARCHAIC_31.contains(codepoint) ? "uax31" 
              : ScriptCategories.ARCHAIC_31.contains(codepoint) ? "utr29" 
                      : ScriptCategories.ARCHAIC_HEURISTIC.contains(codepoint) ? "heuristics" 
                              : ScriptCategories.ARCHAIC_ADDITIONS.contains(codepoint) ? "additions" 
                      : "";
    }
    return UCharacter.getStringPropertyValue(propertyEnum, codepoint, nameChoice);
  }
  
  static int getXPropertyEnum(String propertyAlias) {
    int extra = XPROPERTY_NAMES.indexOf(propertyAlias
        .toLowerCase(Locale.ENGLISH));
    if (extra != -1) {
      return UProperty.STRING_LIMIT + extra;
    }
    return UCharacter.getPropertyEnum(propertyAlias);
  }
  
  public static UnicodeSet OK_AT_END = (UnicodeSet) new UnicodeSet("[ \\]\t]").freeze();

  public static UnicodeSet parseUnicodeSet(String input) {
    input = input.trim() + "]]]]]";
    
    String parseInput = (input.startsWith("[") ? "" : "[") + input + "]]]]]";
    ParsePosition parsePosition = new ParsePosition(0);
    UnicodeSet result = new UnicodeSet(parseInput, parsePosition, myXSymbolTable);
    int parseEnd = parsePosition.getIndex();
    if (parseEnd != parseInput.length() && !OK_AT_END.containsAll(parseInput.substring(parseEnd))) {
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
  
  static final int BLOCK_ENUM = UCharacter.getPropertyEnum("block");

  public static void showSet(UnicodeSet a, boolean abbreviate, boolean ucdFormat, Writer out) throws IOException {
    if (a.size() < 20000 && !abbreviate) {
      String oldBlock = "";
      String oldSubhead = "";
      for (UnicodeSetIterator it = new UnicodeSetIterator(a); it.next();) {
        int s = it.codepoint;
        String newBlock = UCharacter.getStringPropertyValue(BLOCK_ENUM, s, UProperty.NameChoice.LONG).replace('_', ' ');
        String newSubhead = subheader.getSubheader(s);
        if (newSubhead == null) {
          newSubhead = "<i>no subhead</i>";
        }
        if (!newBlock.equals(oldBlock) || !oldSubhead.equals(newSubhead)) {
          out.write("<b>" + newBlock + " - " + newSubhead + "</b><br>\r\n");
          oldBlock = newBlock;
          oldSubhead = newSubhead;
        }
        showCodePoint(s, ucdFormat, out);
      }
    } else if (a.getRangeCount() < 10000) {
      for (UnicodeSetIterator it = new UnicodeSetIterator(a); it.nextRange();) {
        int s = it.codepoint;
        int end = it.codepointEnd;
        if (end == s) {
          showCodePoint(s, ucdFormat, out);
        } else if (end == s + 1) {
          showCodePoint(s, ucdFormat, out);
          showCodePoint(end, ucdFormat, out);
        } else {
          if (ucdFormat) {
            out.write(getHex(s, ucdFormat));
            out.write("..");
            showCodePoint(end, ucdFormat, out);
          } else {
            showCodePoint(s, ucdFormat, out);
            out.write("\u2026{" + (end-s-1) + "}\u2026");
            showCodePoint(end, ucdFormat, out);
          }
        }
      }
    } else {
      out.write("<i>Too many to list individually</i>\r\n");
    }
  }

  static private UnicodeSet RTL= new UnicodeSet("[[:bc=R:][:bc=AL:]]");
  
  private static void showCodePoint(int s, boolean ucdFormat, Writer out) throws IOException {
    String literal = toHTML.transliterate(UTF16.valueOf(s));
    if (RTL.containsSome(literal)) {
      literal = '\u200E' + literal + '\u200E';
    }
    String name = UCharacter.getExtendedName(s);
    if (name == null || name.length() == 0) {
      name = "<i>no name</i>";
    } else {
      boolean special = name.indexOf('<') >= 0;
      name = toHTML.transliterate(name);
      if (special) {
        name = "<i>" + name + "</i>";
      }
    }
    out.write(getHex(s, ucdFormat) + " " + (ucdFormat ? 	"\t;" : "(\u00A0" + literal + "\u00A0) ") + name + "<br>\r\n");
  }

  private static String getHex(int s, boolean ucdFormat) {
    String hex = com.ibm.icu.impl.Utility.hex(s, 4);
    final String string = "<code><a target='c' href='character.jsp?a=" + hex + "'>"
            + (ucdFormat ? "" : "U+")
            + hex + "</a></code>";
    return string;
  }

  private static void showString(String s, String separator, boolean ucdFormat, Writer out) throws IOException {
    int cp;
    for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
      if (i != 0) {
        out.write(separator);
      }
      showCodePoint(cp = UTF16.charAt(s, i), ucdFormat, out);
    }
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
        // add a space, but not in x-y, or \\uXXXX
        if (cp == '-' || oldCp == '-') {
          // do nothing
        } else if (oldCp == '\\' || cp < 0x80) {
          // do nothing
        } else {
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
      //setA = Normalizer.normalize(setA, Normalizer.NFC);
      return parseUnicodeSet(setA);
    } catch (Exception e) {
      exceptionMessage[0] = e.getMessage();
    }
    return null;
  }
  
  public static void getDifferences(String setA, String setB,
      boolean abbreviate, String[] abResults, int[] abSizes, String[] abLinks) {
    
    String setAr = setA.replace("&", "%26");
    String setBr = setB.replace("&", "%26");
    abLinks[0] = "http://unicode.org/cldr/utility/list-unicodeset.jsp?a=[" + setAr + '-' + setBr + "]";
    abLinks[1] = "http://unicode.org/cldr/utility/list-unicodeset.jsp?a=[" + setBr + '-' + setAr + "]";
    abLinks[2] = "http://unicode.org/cldr/utility/list-unicodeset.jsp?a=[" + setAr + "%26" + setBr + "]";
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
  
  static int[][] ranges = { { UProperty.BINARY_START, UProperty.BINARY_LIMIT },
      { UProperty.INT_START, UProperty.INT_LIMIT },
      { UProperty.DOUBLE_START, UProperty.DOUBLE_LIMIT },
      { UProperty.STRING_START, UProperty.STRING_LIMIT }, };

  static Comparator<String> col = Collator.getInstance(ULocale.ROOT);
  static {
    ((RuleBasedCollator) col).setNumericCollation(true);
  }

  public static void showProperties(String text, Writer out) throws IOException {
    text = UTF16.valueOf(text, 0);
    int cp = UTF16.charAt(text, 0);
    Set<String> showLink = new HashSet<String>();
    Map<String,String> alpha = new TreeMap<String,String>(col);

    for (int range = 0; range < ranges.length; ++range) {
      for (int propIndex = ranges[range][0]; propIndex < ranges[range][1]; ++propIndex) {
        String propName = UCharacter.getPropertyName(propIndex,
            UProperty.NameChoice.LONG);
        String propValue = null;
        int ival;
        switch (range) {
          default:
            propValue = "???";
            break;
          case 0:
            ival = UCharacter.getIntPropertyValue(cp, propIndex);
            if (ival != 0)
              propValue = "True";
            showLink.add(propName);
            break;
          case 2:
            double nval = UCharacter.getNumericValue(cp);
            if (nval != -1) {
              propValue = String.valueOf(nval);
              showLink.add(propName);
            }
            break;
          case 3:
            propValue = UCharacter.getStringPropertyValue(propIndex, cp,
                UProperty.NameChoice.LONG);
            if (text.equals(propValue))
              propValue = null;
            break;
          case 1:
            ival = UCharacter.getIntPropertyValue(cp, propIndex);
            if (ival != 0) {
              propValue = UCharacter.getPropertyValueName(propIndex, ival,
                  UProperty.NameChoice.LONG);
              if (propValue == null)
                propValue = String.valueOf(ival);
            }
            showLink.add(propName);
            break;
        }
        if (propValue != null) {
          alpha.put(propName, propValue);
        }
      }
    }
    showLink.add("Age");

    String x;
    String upper = x = UCharacter.toUpperCase(ULocale.ENGLISH, text);
    if (!text.equals(x))
      alpha.put("toUppercase", x);
    String lower = x = UCharacter.toLowerCase(ULocale.ENGLISH, text);
    if (!text.equals(x))
      alpha.put("toLowercase", x);
    String title = x = UCharacter.toTitleCase(ULocale.ENGLISH, text, null);
    if (!text.equals(x))
      alpha.put("toTitlecase", x);

    String nfc = x = Normalizer.normalize(text, Normalizer.NFC);
    if (!text.equals(x))
      alpha.put("toNFC", x);
    String nfd = x = Normalizer.normalize(text, Normalizer.NFD);
    if (!text.equals(x))
      alpha.put("toNFD", x);
    x = Normalizer.normalize(text, Normalizer.NFKD);
    if (!text.equals(x))
      alpha.put("toNFKD", x);
    x = Normalizer.normalize(text, Normalizer.NFKC);
    if (!text.equals(x))
      alpha.put("toNFKC", x);

    CanonicalIterator ci = new CanonicalIterator(text);
    int count = 0;
    for (String item = ci.next(); item != null; item = ci.next()) {
      if (item.equals(text))
        continue;
      if (item.equals(nfc))
        continue;
      if (item.equals(nfd))
        continue;
      alpha.put("toOther_Canonical_Equivalent#" + (++count), item);
    }

    /*
     * CaseIterator cai = new CaseIterator(); cai.reset(text); count = 0; for
     * (String item = cai.next(); item != null; item = cai.next()) { if
     * (item.equals(text)) continue; if (item.equals(upper)) continue; if
     * (item.equals(lower)) continue; if (item.equals(title)) continue;
     * alpha.put("toOther_Case_Equivalent#" + (++count), item); }
     */

    Set unicodeProps = new TreeSet(UNICODE_PROPS);

    Set regexProps = new TreeSet(REGEX_PROPS);
    Set icuProps = new TreeSet(alpha.keySet());
    icuProps.removeAll(unicodeProps);
    icuProps.removeAll(regexProps);

    out.write("<table>\r\n");
    String name = (String) alpha.get("Name");
    if (name != null)
      name = toHTML.transliterate(name);

    out.write("<tr><th>" + "Character" + "</th><td>"
        + toHTML.transliterate(text) + "</td></tr>\r\n");
    out.write("<tr><th>" + "Code_Point" + "</th><td>"
        + com.ibm.icu.impl.Utility.hex(cp, 4) + "</td></tr>\r\n");
    out.write("<tr><th>" + "Name" + "</th><td>" + name + "</td></tr>\r\n");
    alpha.remove("Name");
    showPropertyValue(alpha, showLink, "", unicodeProps, out); 
    showPropertyValue(alpha, showLink, "® ", regexProps, out);
    showPropertyValue(alpha, showLink, "© ", icuProps, out);
    out.write("</table>\r\n");
  }

  private static void showPropertyValue(Map alpha, Set showLink, String flag, 
      Set unicodeProps, Writer out) throws IOException {
    for (Iterator it = alpha.keySet().iterator(); it.hasNext();) {
      String propName = (String) it.next();
      if (!unicodeProps.contains(propName)) continue;
      String propValue = (String) alpha.get(propName);

      String hValue = toHTML.transliterate(propValue);
      hValue = showLink.contains(propName) ? "<a target='u' href='list-unicodeset.jsp?a=[:"
          + propName + "=" + propValue + ":]'>" + hValue + "</a>"
          : hValue;

      out.write("<tr><th><a target='c' href='properties.jsp#" + propName + "'>"
          + flag + propName + "</a></th><td>" + hValue + "</td></tr>\r\n");
    }
  }
  
  public static Set<String> showPropsTable(PrintWriter out) {
    int[][] ranges = {{UProperty.BINARY_START, UProperty.BINARY_LIMIT},
            {UProperty.INT_START, UProperty.INT_LIMIT},
            {UProperty.DOUBLE_START, UProperty.DOUBLE_LIMIT},
            {UProperty.STRING_START, UProperty.STRING_LIMIT},
        };
        Collator col = Collator.getInstance(ULocale.ROOT);
        ((RuleBasedCollator)col).setNumericCollation(true);
        Map<String, Set<String>> alpha = new TreeMap<String, Set<String>>(col);

        Set<String> showLink = new HashSet<String>();
        
        for (int range = 0; range < ranges.length; ++range) {
          for (int propIndex = ranges[range][0]; propIndex < ranges[range][1]; ++propIndex) {
            String propName = UCharacter.getPropertyName(propIndex, UProperty.NameChoice.LONG);
            //String shortPropName = UCharacter.getPropertyName(propIndex, UProperty.NameChoice.SHORT);
            //propName = getName(propIndex, propName, shortPropName);
            Set<String> valueOrder = new TreeSet<String>(col);
            alpha.put(propName, valueOrder);
            //out.println(propName + "<br>");
            switch (range) {
            default: valueOrder.add("[?]"); break;
            case 0: valueOrder.add("True"); valueOrder.add("False"); showLink.add(propName); break;
            case 2: valueOrder.add("[double]"); break;
            case 3: valueOrder.add("[string]"); break;
            case 1:
            for (int valueIndex = 0; valueIndex < 256; ++valueIndex) {
              try {
                String valueName = UCharacter.getPropertyValueName(propIndex, valueIndex, UProperty.NameChoice.LONG);
                //out.println("----" + valueName + "<br>");
                //String shortValueName = UCharacter.getPropertyValueName(propIndex, valueIndex, UProperty.NameChoice.SHORT);
                //valueName = getName(valueIndex, valueName, shortValueName);
                if (valueName != null) valueOrder.add(valueName);
                else if (propIndex == UProperty.CANONICAL_COMBINING_CLASS) {
                  String posVal = String.valueOf(valueIndex);
                  if (new UnicodeSet("[:ccc=" + posVal + ":]").size() != 0) {
                    valueOrder.add(posVal);
                  }
                }
                showLink.add(propName);
              } catch (RuntimeException e) {
                // just skip
              }
            }
            }
          }
        }
        Set<String> unicodeProps = new TreeSet<String>(UNICODE_PROPS);
        
        Set<String> regexProps = new TreeSet<String>(REGEX_PROPS);

        out.println("<table>");
        for (Iterator<String> it = alpha.keySet().iterator(); it.hasNext();) {
          String propName = (String) it.next();
          String sPropName = propName;
          Set<String> values = alpha.get(propName);
          if (unicodeProps.contains(propName)) {
            unicodeProps.remove(propName);
          } else if (regexProps.contains(propName)) {
                regexProps.remove(propName);
              sPropName = "<tt>\u00AE\u00A0" + sPropName + "</tt>";
          } else {
            sPropName = "<i>\u00A9\u00A0" + sPropName + "</i>";
          }

          out.println("<tr><th width='1%'><a name='" + propName + "'>" + sPropName + "</a></th>");
          out.println("<td>");
          boolean first = true;
          for (Iterator<String> it2 = values.iterator(); it2.hasNext();) {
            String propValue = (String) it2.next();
            if (first) first = false;
            else out.print(", ");

            
              if (showLink.contains(propName)) {
                propValue = "<a target='u' href='list-unicodeset.jsp?a=[:" + propName
                  + "=" + propValue + ":]'>" + propValue + "</a>";
              }
              
            out.print(propValue);
          }
          out.println("</td></tr>");
        }
        out.println("</table>");
        unicodeProps.addAll(regexProps);
        return unicodeProps;
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