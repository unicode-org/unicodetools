package org.unicode.jsp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.Predicate;
import org.unicode.jsp.Idna.IdnaType;
import org.unicode.jsp.Idna2008.Idna2008Type;
import org.unicode.jsp.UnicodeSetUtilities.TableStyle;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.test.util.PrettyPrinter;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.StringTransform;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.VersionInfo;

public class UnicodeUtilities {


  static final UnicodeSet OFF_LIMITS = new UnicodeSet(UnicodeProperty.UNASSIGNED).addAll(UnicodeProperty.PRIVATE_USE).addAll(UnicodeProperty.SURROGATE).freeze();

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
          "toNFC", "toNFD", "toNFKC", "toNFKD",
          "NFKC_Casefold", "Case_Ignorable", "Cased", 
          "Changes_When_Lowercased", "Changes_When_Uppercased", "Changes_When_Titlecased", "Changes_When_Casemapped", "Changes_When_Casefolded",
  "Changes_When_NFKC_Casefolded"});
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

  private static Subheader subheader = null;

  static Transliterator toHTML;
  static String HTML_RULES_CONTROLS;
  static {

    String BASE_RULES = "'<' > '&lt;' ;" + "'<' < '&'[lL][Tt]';' ;"
    + "'&' > '&amp;' ;" + "'&' < '&'[aA][mM][pP]';' ;"
    + "'>' < '&'[gG][tT]';' ;" + "'\"' < '&'[qQ][uU][oO][tT]';' ; "
    + "'' < '&'[aA][pP][oO][sS]';' ; ";

    String CONTENT_RULES = "'>' > '&gt;' ;";

    String HTML_RULES = BASE_RULES + CONTENT_RULES + "'\"' > '&quot;' ; ";

    HTML_RULES_CONTROLS = HTML_RULES
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

  static final int IDNA_TYPE_LIMIT = 4;

  //  static final Map<IdnaType,UnicodeSet> idnaTypeSet = new TreeMap<IdnaType,UnicodeSet>();
  //  static {
  //    for (IdnaType i : IdnaType.values()) {
  //      idnaTypeSet.put(i, new UnicodeSet());
  //    }
  //  }

  public static UnicodeSet IGNORE_IN_IDNA_DIFF = UnicodeSetUtilities.parseUnicodeSet("[[\\u0000-\\u007F][:Cc:][:Cn:][:Co:][:Cs:]]", TableStyle.simple).freeze();

  public static UnicodeMap<String> getIdnaDifferences(UnicodeSet remapped, UnicodeSet overallAllowed) {
    UnicodeMap<String> result = new UnicodeMap<String>();
    UnicodeSet valid2008 = getIdna2008Valid();

    for (int i = 0; i <= 0x10FFFF; ++i) {
      if ((i & 0xFFF) == 0) System.out.println(Utility.hex(i));
      if (i == 0x20000) {
        System.out.println("debug");
      }
      if (IGNORE_IN_IDNA_DIFF.contains(i)) continue;
      boolean isNew = UCharacter.getAge(i).compareTo(VersionInfo.UNICODE_3_2) > 0;
      String age = isNew ? "v4.0-5.2" : "v3.2";
      IdnaType idna2003 = Idna2003.getIDNA2003Type(i);
      IdnaType tr46 = Uts46.SINGLETON.getType(i);
      if (isNew) {// skip
      } else if ((tr46 == IdnaType.mapped || idna2003 == IdnaType.mapped) && tr46 != IdnaType.disallowed && idna2003 != IdnaType.disallowed) {
        remapped.add(i);
      }
      //TestStatus testResult = valid2008.contains(i);
      IdnaType idna2008 = valid2008.contains(i) ? IdnaType.valid : IdnaType.disallowed;
      String iClass = age
      + "\t" + getShortName(idna2003) 
      + "\t" + getShortName(tr46)
      + "\t" + getShortName(idna2008)
      ;
      result.put(i, iClass);
    }
    return result.freeze();
  }

  public static UnicodeSet getIdna2008Valid() {
    //    IdnaLabelTester tester = getIdna2008Tester();
    //    UnicodeSet valid2008 = UnicodeSetUtilities.parseUnicodeSet(tester.getVariable("$Valid"), TableStyle.simple);
    //    return valid2008;
    UnicodeMap<Idna2008Type> typeMapping = Idna2008.getTypeMapping();
    return new UnicodeSet(typeMapping.getSet(Idna2008Type.PVALID))
    .addAll(typeMapping.getSet(Idna2008Type.CONTEXTJ))
    .addAll(typeMapping.getSet(Idna2008Type.CONTEXTO))
    ;
  }

  static String getShortName(IdnaType tr46) {
    // TODO Auto-generated method stub
    return UCharacter.toTitleCase(
            tr46==IdnaType.valid ? "Valid" 
                    : tr46==IdnaType.ignored || tr46==IdnaType.mapped ? "Mapped/Ignored" 
                            : tr46.toString()
                            , null);
  }
  static {
    for (int cp = 0; cp <= 0x10FFFF; ++cp) {

      int cat = UCharacter.getType(cp);
      if (cat == UCharacter.UNASSIGNED || cat == UCharacter.PRIVATE_USE  || cat == UCharacter.SURROGATE) {
        //        idnaTypeSet.get(IdnaType.disallowed).add(cp); // faster
        isCaseFolded.add(cp);
        isLowercase.add(cp);
        isTitlecase.add(cp);
        isUppercase.add(cp);
        continue;
      }

      //      IdnaType idnaType = Idna2003.getIDNA2003Type(cp);
      //      idnaTypeSet.get(idnaType).add(cp);

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



  static final int 
  XSTRING_START = UProperty.STRING_LIMIT,
  TO_NFC = UProperty.STRING_LIMIT,
  TO_NFD = UProperty.STRING_LIMIT + 1,
  TO_NFKC = UProperty.STRING_LIMIT + 2,
  TO_NFKD = UProperty.STRING_LIMIT + 3,
  TO_CASEFOLD  = UProperty.STRING_LIMIT + 4,
  TO_LOWERCASE  = UProperty.STRING_LIMIT + 5,
  TO_UPPERCASE  = UProperty.STRING_LIMIT + 6,
  TO_TITLECASE  = UProperty.STRING_LIMIT + 7;

  public static final int SUBHEAD = TO_TITLECASE + 1;

  static final int XSTRING_LIMIT = SUBHEAD + 1; 

  static List<String> XPROPERTY_NAMES = Arrays.asList(new String[]{"toNfc", "toNfd", "toNfkc", "toNfkd", "toCasefold", "toLowercase", "toUppercase", "toTitlecase",
  "subhead"});
  static final UnicodeSet MARK = (UnicodeSet) UnicodeSetUtilities.parseUnicodeSet("[:M:]", TableStyle.simple).freeze();

  static String getXStringPropertyValue(int propertyEnum, int codepoint, int nameChoice, Normalizer.Mode compat) {
    if (compat == null || Normalizer.isNormalized(codepoint, compat, 0)) {
      return getXStringPropertyValue(propertyEnum, codepoint, nameChoice);
    }
    String s = UnicodeSetUtilities.MyNormalize(codepoint, compat);
    int cp;
    String lastPart = null;
    for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
      cp = UTF16.charAt(s, i);
      String part = getXStringPropertyValue(propertyEnum, cp, nameChoice);
      if (lastPart == null) {
        lastPart = part;
      } else if (!lastPart.equals(part)) {
        if (propertyEnum == UProperty.SCRIPT && MARK.contains(cp)) {
          continue;
        }
        return "Mixed";
      }
    }
    return lastPart;
  }

  static UnicodeSet COMMON_USE_SCRIPTS = new UnicodeSet("[[:script=Zyyy:] [:script=Zinh:] [:script=Arab:] [:script=Armn:]" +
          " [:script=Beng:] [:script=Bopo:] [:script=Cans:] [:script=Cyrl:] [:script=Deva:] [:script=Ethi:]" +
          " [:script=Geor:] [:script=Grek:] [:script=Gujr:] [:script=Guru:] [:script=Hani:] [:script=Hang:]" +
          " [:script=Hebr:] [:script=Hira:] [:script=Knda:] [:script=Kana:] [:script=Khmr:] [:script=Laoo:]" +
          " [:script=Latn:] [:script=Mlym:] [:script=Mong:] [:script=Mymr:] [:script=Orya:] [:script=Sinh:] " +
  "[:script=Taml:] [:script=Telu:] [:script=Tfng:] [:script=Thaa:] [:script=Thai:] [:script=Tibt:] [:script=Yiii:]]").freeze();

  static UnicodeSet LITURGICAL = new UnicodeSet("[\u0615\u0617-\u061A\u0671\u06D6-\u06ED\u08F0-\u08F3[:sc=coptic:]" +
  "\u1CD0-\u1CF2\u214F]");
  static UnicodeSet DEPRECATED = new UnicodeSet("[:deprecated:]").freeze();

  public static String getXStringPropertyValue(int propertyEnum, int codepoint, int nameChoice) {

    switch (propertyEnum) {
    case TO_NFC: return UnicodeSetUtilities.MyNormalize(codepoint, Normalizer.NFC);
    case TO_NFD: return UnicodeSetUtilities.MyNormalize(codepoint, Normalizer.NFD);
    case TO_NFKC: return UnicodeSetUtilities.MyNormalize(codepoint, Normalizer.NFKC);
    case TO_NFKD: return UnicodeSetUtilities.MyNormalize(codepoint, Normalizer.NFKD);
    case TO_CASEFOLD: return UCharacter.foldCase(UTF16.valueOf(codepoint), true);
    case TO_LOWERCASE: return UCharacter.toLowerCase(ULocale.ROOT, UTF16.valueOf(codepoint));
    case TO_UPPERCASE: return UCharacter.toUpperCase(ULocale.ROOT, UTF16.valueOf(codepoint));
    case TO_TITLECASE: return UCharacter.toTitleCase(ULocale.ROOT, UTF16.valueOf(codepoint), null);
    case SUBHEAD: return getSubheader().getSubheader(codepoint);
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

  //  protected static boolean getIdnaProperty(String propertyValue,
  //          UnicodeSet result) {
  //    try {
  //      String lowercase = propertyValue.toLowerCase(Locale.ENGLISH);
  //      IdnaType i = lowercase.equals("output") ? IdnaType.valid 
  //              : lowercase.equals("remapped") ? IdnaType.mapped
  //                      : IdnaType.valueOf(lowercase);
  //      result.clear().addAll(idnaTypeSet.get(i));
  //      return true;
  //    } catch (Exception e) {
  //      throw new IllegalArgumentException("Error with <" + propertyValue + ">", e);
  //    }
  //  }

  static boolean getBinaryValue(String propertyValue) {
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
    if (inbuffer.length() != outbuffer.length()) {
      return false;
    }
    for (int i = inbuffer.length() - 1; i >= 0; --i) {
      if (inbuffer.charAt(i) != outbuffer.charAt(i)) {
        return false;
      }
    }
    return true;
  }

  static final int BLOCK_ENUM = UCharacter.getPropertyEnum("block");

  static XPropertyFactory factory = XPropertyFactory.make();

  static NumberFormat numberFormat = NumberFormat.getInstance(ULocale.ENGLISH, NumberFormat.NUMBERSTYLE);
  static {
    numberFormat.setGroupingUsed(true);
  }

  public static void showSet(String grouping, UnicodeSet a, boolean abbreviate, boolean ucdFormat, Appendable out) throws IOException {
    grouping = grouping.trim();
    if (grouping.length() == 0) {
      showSet(a, abbreviate, ucdFormat, out);
      return;
    }
    String[] props = grouping.split("[;,\\s]\\s*");
    int length = props.length;

    if (length > 5) {
      out.append("<p>Too many groups: " + Arrays.asList(props) + "</p>");
      return;
    }
    boolean getShortest = false;
    UnicodeProperty[] properties = new UnicodeProperty[length];
    String[] names = new String[length];
    for (int i = 0; i < length; ++i) {
      try {
        properties[i] = factory.getProperty(props[i]);
        names[i] = properties[i].getName();
        names[i].charAt(0); // trigger exception
        properties[i].getValue(0, getShortest);
      } catch (Exception e) {
        out.append("<p>Unknown 'Group by' property name: '" + props[i] + "'</p>");
        return;
      }
    }
    UnicodeMap<String> map = new UnicodeMap<String>();
    StringBuilder builder = new StringBuilder();
    for (UnicodeSetIterator it = new UnicodeSetIterator(a); it.next();) {
      int s = it.codepoint;
      if (s == UnicodeSetIterator.IS_STRING) {
        String ss = it.string;
        builder.setLength(0);
        for (int i = 0; i < length; ++i) {
          if (i != 0) {
            builder.append("; ");
          }
          builder.append(names[i]).append("=");
          builder.append(getStringProperties(properties[i], ss, ",", getShortest));
        }
        map.put(ss, builder.toString());
      } else {
        builder.setLength(0);
        for (int i = 0; i < length; ++i) {
          if (i != 0) {
            builder.append("; ");
          }
          try {
            builder.append(names[i]).append("=");
            builder.append(properties[i].getValue(s, getShortest));
          } catch (Exception e) {
            builder.append("Internal error: " + names[i] + ", " + properties[i] + ", " + getHex(i,true));
          }
        }
        map.put(s, builder.toString());
      }
    }
    TreeSet<String> sorted = new TreeSet<String>(Collator.getInstance(ULocale.ENGLISH));
    sorted.addAll(map.values());
    String[] propsOld = new String[length];
    for (int i = 0; i < propsOld.length; ++i) {
      propsOld[i] = "";
    }
    int lastLevel = -1;
    for (String s : sorted) {
      String[] props2 = s.split("; ");
      int level = getFirstDiff(propsOld, props2);
      //out.append("// level: " + level + ", lastLevel: " + lastLevel + "\n");
      // if higher, back off
      if (lastLevel >= 0) {
        for (int i = level; i < length; ++i) {
          out.append("</blockquote>\n");
        }
      }
      lastLevel = level;
      UnicodeSet items = map.getSet(s);

      for (int i = lastLevel; i < length; ++i) {
        out.append("<h2 class='L" + (i + 5 - length) + "'>" + props2[i] + 
                (i == length - 1 ? " <div class='ri'>items: " + numberFormat.format(items.size()) : "</div>") +
        "</h2><blockquote>\n");
      }
      showSet(items, abbreviate, ucdFormat, out);
      for (int i = 0; i < propsOld.length; ++i) {
        propsOld[i] = props2[i];
      }
    }
    for (int i = 0; i <= lastLevel; ++i) {
      out.append("</blockquote>\n");
    }
  }

  static int getFirstDiff(String[] a, String[] b) {
    for (int i = 0; i < a.length; ++i) {
      if (!a[i].equals(b[i])) {
        return i;
      }
    }
    return a.length;
  }

  //  static getPropNames() {
  //    return factory.getAvailableNames();
  //  }

  public static String getStringProperties(UnicodeProperty prop, String s, String separator, boolean getShortest) {
    StringBuilder builder = new StringBuilder();
    int cp;
    for (int i = 0; i < s.length(); i += Character.charCount(cp)) {
      cp = s.codePointAt(i);
      if (i != 0) {
        builder.append(separator);
      }
      builder.append(prop.getValue(cp, getShortest));
    }
    return builder.toString();
  }

  /*jsp*/
  public static void showSet(UnicodeSet inputSetRaw, boolean abbreviate, boolean ucdFormat, Appendable out) throws IOException {
    if (inputSetRaw.getRangeCount() > 10000) {
      out.append("<i>Too many to list individually</i>\n");
    } else if (abbreviate) {
      showAbbreviated(inputSetRaw, ucdFormat, out);
    } else {
      LinkedHashMap<String,UnicodeSet> items = new LinkedHashMap();
      String specials = "Unassigned, Private use, or Surrogates";

      UnicodeSet specialSet = new UnicodeSet(inputSetRaw).retainAll(UnicodeProperty.SPECIALS);
      UnicodeSet inputSet = specialSet.size() == 0 ? inputSetRaw : new UnicodeSet(inputSetRaw).removeAll(UnicodeProperty.SPECIALS);
      if (specialSet.size() != 0) {
        items.put(specials, specialSet);
      }

      String oldBlock = "";
      String oldSubhead = "";
      for (UnicodeSetIterator it = new UnicodeSetIterator(inputSet); it.next();) {
        int s = it.codepoint;
        if (s == UnicodeSetIterator.IS_STRING) {
          String newBlock = "Strings";
          UnicodeSet set = items.get(newBlock);
          if (set == null) items.put(newBlock, set = new UnicodeSet());
          set.add(it.string);
        } else {
          String newBlock = UCharacter.getStringPropertyValue(BLOCK_ENUM, s, UProperty.NameChoice.LONG).replace('_', ' ');
          String newSubhead = getSubheader().getSubheader(s);
          if (newSubhead == null) {
            newSubhead = "<u>no subhead</u>";
          }
          newBlock = newBlock + " - <i>" + newSubhead + "</i>";
          UnicodeSet set = items.get(newBlock);
          if (set == null) items.put(newBlock, set = new UnicodeSet());
          set.add(s);
        }
      }

      for (String newBlock : items.keySet()) {
        UnicodeSet set = items.get(newBlock);
        out.append("<h3>" + newBlock + "</b>: " + numberFormat.format(set.size()) + "</h3>\n");
        if (set.size() > 500 || newBlock == specials) {
          showAbbreviated(set, ucdFormat, out);
        } else {
          for (UnicodeSetIterator it = new UnicodeSetIterator(set); it.next();) {
            int s = it.codepoint;
            if (s == UnicodeSetIterator.IS_STRING) {
              out.append(showCodePoint(it.string)).append("<br>\n");
            } else {
              showCodePoint(s, ucdFormat, out);
            }
          }
        }
      }
    }
  }

  private static void showAbbreviated(UnicodeSet a, boolean ucdFormat, Appendable out) throws IOException {
    for (UnicodeSetIterator it = new UnicodeSetIterator(a); it.nextRange();) {
      int s = it.codepoint;
      if (s == UnicodeSetIterator.IS_STRING) {
        out.append(showCodePoint(it.string)).append("<br>\n");
      } else {        
        int end = it.codepointEnd;
        if (end == s) {
          showCodePoint(s, ucdFormat, out);
        } else if (end == s + 1) {
          showCodePoint(s, ucdFormat, out);
          showCodePoint(end, ucdFormat, out);
        } else {
          if (ucdFormat) {
            out.append(getHex(s, ucdFormat));
            out.append("..");
            showCodePoint(end, ucdFormat, out);
          } else {
            showCodePoint(s, ucdFormat, out);
            out.append("\u2026{" + (end-s-1) + "}\u2026");
            showCodePoint(end, ucdFormat, out);
          }
        }
      }
    }
  }

  static private UnicodeSet RTL= UnicodeSetUtilities.parseUnicodeSet("[[:bc=R:][:bc=AL:]]", TableStyle.simple);

  private static String showCodePoint(int codepoint) {
    return showCodePoint(UTF16.valueOf(codepoint));
  }

  private static String showCodePoint(String s) {
    String literal = getLiteral(s);
    return "<a target='c' href='list-unicodeset.jsp?a=" + toHTML.transliterate(UtfParameters.fixQuery(s)) + "'>\u00a0" + literal + "\u00a0</a>";
  }

  private static String getLiteral(int codepoint) {
    return getLiteral(UTF16.valueOf(codepoint));
  }

  private static String getLiteral(String s) {
    String literal = toHTML.transliterate(s);
    if (RTL.containsSome(literal)) {
      literal = '\u200E' + literal + '\u200E';
    }
    return literal;
  }

  private static void showCodePoint(int codePoint, boolean ucdFormat, Appendable out) throws IOException {
    final String string = UTF16.valueOf(codePoint);
    String separator = ", ";
    showString(string, ucdFormat, separator, out);
  }

  private static void showString(final String string, boolean ucdFormat, String separator,
          Appendable out) throws IOException {
    String literal = toHTML.transliterate(string);
    if (RTL.containsSome(literal)) {
      literal = '\u200E' + literal + '\u200E';
    }
    String name = getName(string, separator, false);
    if (name == null || name.length() == 0) {
      name = "<i>no name</i>";
    } else {
      boolean special = name.indexOf('<') >= 0;
      name = toHTML.transliterate(name);
      if (special) {
        name = "<i>" + name + "</i>";
      }
    }
    out.append(getHex(string, separator, ucdFormat) + " " + (ucdFormat ? 	"\t;" : "(\u00A0" + literal + "\u00A0) ") + name + "<br>\n");
  }

  private static String getName(String string, String separator, boolean andCode) {
    StringBuilder result = new StringBuilder();
    int cp;
    for (int i = 0; i < string.length(); i += UTF16.getCharCount(cp)) {
      cp = UTF16.charAt(string, i);
      if (i != 0) {
        result.append(separator);
      }
      if (andCode) {
        result.append("U+").append(com.ibm.icu.impl.Utility.hex(cp, 4)).append(' ');
      }
      result.append(UCharacter.getExtendedName(cp));
    }
    return result.toString();
  }

  private static String getHex(int codePoint, boolean ucdFormat) {
    String hex = com.ibm.icu.impl.Utility.hex(codePoint, 4);
    final String string = "<code><a target='c' href='character.jsp?a=" + hex + "'>"
    + (ucdFormat ? "" : "U+")
    + hex + "</a></code>";
    return string;
  }

  private static String getHex(String string, String separator, boolean ucdFormat) {
    StringBuilder result = new StringBuilder();
    int cp;
    for (int i = 0; i < string.length(); i += UTF16.getCharCount(cp)) {
      if (i != 0) {
        result.append(separator);
      }
      result.append(getHex(cp = UTF16.charAt(string, i), ucdFormat));
    }
    return result.toString();
  }

  //  private static void showString(String s, String separator, boolean ucdFormat, Writer out) throws IOException {
  //    int cp;
  //    for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
  //      if (i != 0) {
  //        out.write(separator);
  //      }
  //      showCodePoint(cp = UTF16.charAt(s, i), ucdFormat, out);
  //    }
  //  }

  static final UnicodeSet MAPPING_SET = UnicodeSetUtilities.parseUnicodeSet("[:^c:]", TableStyle.simple);

  static {
    Transliterator.registerInstance(getTransliteratorFromFile("en-IPA", "en-IPA.txt", Transliterator.FORWARD));
    Transliterator.registerInstance(getTransliteratorFromFile("IPA-en", "en-IPA.txt", Transliterator.REVERSE));

    Transliterator.registerInstance(getTransliteratorFromFile("deva-ipa", "Deva-IPA.txt", Transliterator.FORWARD));
    Transliterator.registerInstance(getTransliteratorFromFile("ipa-deva", "Deva-IPA.txt", Transliterator.REVERSE));
  }

  public static Transliterator getTransliteratorFromFile(String ID, String file, int direction) {
    try {
      BufferedReader br = FileUtilities.openFile(UnicodeUtilities.class, file);
      StringBuffer input = new StringBuffer();
      while (true) {
        String line = br.readLine();
        if (line == null) {
          break;
        }
        if (line.startsWith("\uFEFF")) {
          line = line.substring(1); // remove BOM
        }
        input.append(line);
        input.append('\n');
      }
      return Transliterator.createFromRules(ID, input.toString(), direction);
    } catch (IOException e) {
      throw (IllegalArgumentException) new IllegalArgumentException("Can't open transliterator file " + file).initCause(e);
    }
  }

  public static final Transliterator UNESCAPER = Transliterator.getInstance("hex-any");


  /*jsp*/
  public static String showTransform(String transform, String sample) {
    //    if (!haveCaseFold) {
    //      registerCaseFold();
    //    }
    Transliterator trans;
    try {
      trans = Transliterator.createFromRules("foo", transform, Transliterator.FORWARD);
    } catch (Exception e) {
      try {
        trans = Transliterator.getInstance(transform);
      } catch (Exception e2) {
        return "Error: " + toHTML.transform(e.getMessage() + "; " + e2.getMessage());
      }
    }

    UnicodeSet set = null;
    // see if sample is a UnicodeSet
    if (UnicodeSet.resemblesPattern(sample, 0)) {
      try {
        set = UnicodeSetUtilities.parseUnicodeSet(sample, TableStyle.extras);
      } catch (Exception e) {}
    }
    if (set == null) {
      sample = UNESCAPER.transform(sample);
      return getLiteral(trans.transform(sample)).replace("\n", "<br>");
    }

    PrettyPrinter pp = new PrettyPrinter().setOrdering(Collator.getInstance(ULocale.ROOT)).setSpaceComparator(Collator.getInstance(ULocale.ROOT).setStrength2(RuleBasedCollator.PRIMARY)).setSpaceComparator(new Comparator<String>() {
      public int compare(String o1, String o2) {
        return 1;
      }
    });

    Map<String, UnicodeSet> mapping = new TreeMap<String,UnicodeSet>(pp.getOrdering());

    for (UnicodeSetIterator it = new UnicodeSetIterator(set); it.next();) {
      String s = it.getString();
      String mapped = trans.transform(s);
      if (!mapped.equals(s)) {
        UnicodeSet x = mapping.get(mapped);
        if (x == null) {
          mapping.put(mapped, x = new UnicodeSet());
        }
        x.add(s);
      }
    }
    StringBuilder result = new StringBuilder();
    for (String mapped : mapping.keySet()) {
      UnicodeSet source = mapping.get(mapped);
      result.append(showCodePoint(mapped));
      result.append("\t←\t");
      if (source.size() == 1) {
        UnicodeSetIterator it = new UnicodeSetIterator(source);
        it.next();
        result.append(showCodePoint(it.getString()));
      } else {
        result.append(showCodePoint(pp.format(source)));
      }
      result.append("</br>\n");
    }
    return result.toString();
  }

  public static class StringPair implements Comparable<StringPair> {
    String first;
    String second;
    public StringPair(String first, String second) {
      this.first = first;
      this.second = second;
    }
    public int compareTo(StringPair o) {
      int result = first.compareTo(o.first);
      if (result != 0) {
        return result;
      }
      return second.compareTo(o.second);
    }
  }

  static String TRANSFORMLIST = null;

  public static String listTransforms() {
    if (TRANSFORMLIST == null) {
      StringBuilder result = new StringBuilder();
      Set<StringPair> pairs = new TreeSet<StringPair>();
      Set<String> sources = append(new TreeSet<String>(col), (Enumeration<String>) Transliterator.getAvailableSources());
      for (String source : sources) {
        Set<String> targets = append(new TreeSet<String>(col), (Enumeration<String>) Transliterator.getAvailableTargets(source));
        for (String target : targets) {
          Set<String> variants = append(new TreeSet<String>(col), (Enumeration<String>) Transliterator.getAvailableVariants(source, target));
          for (String variant : variants) {
            final String id = toHTML.transform(source + "-" + target + (variant.length() == 0 ? "" : "/" + variant));
            pairs.add(new StringPair(target, id));
          }
        }
      }
      result.append("<hr><table><tr><th>Result</th><th>IDs</th></tr>\n");
      String last = "";
      boolean first = true;
      for (StringPair pair : pairs) {
        if (!last.equals(pair.first)) {
          if (first) {
            first = false;
          } else {
            result.append("</td></tr>\n");
          }
          result.append("<tr><th>" + pair.first + "</th><td>");
        }
        result.append("<a href='transform.jsp?a=" + pair.second + "'>" + pair.second + "</a>\n");
        last = pair.first;
      }
      result.append("\t\t</ul>\n\t</li>\n");
      result.append("</table>");
      TRANSFORMLIST = result.toString();
    }
    return TRANSFORMLIST;
  }

  private static <T, U extends Collection<T>> U append(U result, Enumeration<T> sources) {
    while (sources.hasMoreElements()) {
      result.add(sources.nextElement());
    }
    return result;
  }

  //  private static void registerCaseFold() {
  //    StringBuilder rules = new StringBuilder();
  //    for (UnicodeSetIterator it = new UnicodeSetIterator(MAPPING_SET); it.nextRange();) {
  //      for (int i = it.codepoint; i <= it.codepointEnd; ++i) {
  //        String s = UTF16.valueOf(i);
  //        String caseFold = UCharacter.foldCase(s, true);
  //        String lower = UCharacter.toLowerCase(Locale.ENGLISH, s);
  //        if (!caseFold.equals(lower) || i == 'Σ') {
  //          rules.append(s + ">" + caseFold + " ;\n");
  //        }
  //      }
  //    }
  //    rules.append("::Lower;");
  //    Transliterator.registerInstance(Transliterator.createFromRules("Any-CaseFold", rules.toString(), Transliterator.FORWARD));
  //    haveCaseFold = true;
  //  }

  static class FilteredStringTransform implements StringTransform {
    final UnicodeSet toExclude;
    final StringTransform trans;
    public FilteredStringTransform(UnicodeSet toExclude, StringTransform trans) {
      this.toExclude = toExclude;
      this.trans = trans;
    }
    public String transform(String source) {
      StringBuilder result = new StringBuilder();
      int start = 0;
      while (start < source.length()) {
        int end = toExclude.findIn(source, start, false);
        result.append(trans.transform(source.substring(start,end)));
        if (end == source.length()) break;
        start = toExclude.findIn(source, end, true);
        result.append(source.substring(end,start));
      }
      return result.toString();
    }
  }

  static String getPrettySet(UnicodeSet a, boolean abbreviate, boolean escape) {
    String a_out;
    if (a.size() < 10000 && !abbreviate) {
      PrettyPrinter pp = new PrettyPrinter().setOrdering(Collator.getInstance(ULocale.ROOT)).setSpaceComparator(Collator.getInstance(ULocale.ROOT).setStrength2(RuleBasedCollator.PRIMARY));
      if (escape) {
        pp.setToQuote(new UnicodeSet("[^\\u0021-\\u007E]"));
      }
      a_out = toHTML(pp.format(a));
    } else {
      a.complement().complement();
      a_out = toHTML(a.toPattern(escape));
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
      setA = setA.replace("..U+", "-\\u");
      setA = setA.replace("U+", "\\u");
      return UnicodeSetUtilities.parseUnicodeSet(setA, TableStyle.extras);
    } catch (Exception e) {
      exceptionMessage[0] = e.getMessage();
    }
    return null;
  }

  public static void getDifferences(String setA, String setB,
          boolean abbreviate, String[] abResults, int[] abSizes, String[] abLinks) {
    boolean escape = false;

    String setAr = toHTML.transliterate(UtfParameters.fixQuery(setA));
    String setBr = toHTML.transliterate(UtfParameters.fixQuery(setB));
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
    // setA = MyNormalize(setA, Normalizer.NFC);
    // a = UnicodeUtilities.parseUnicodeSet(setA);
    // } catch (Exception e) {
    // a_b = e.getMessage();
    // }
    // UnicodeSet b = null;
    // try {
    // setB = MyNormalize(setB, Normalizer.NFC);
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
      a_b = getPrettySet(temp, abbreviate, escape);

      temp = new UnicodeSet(b).removeAll(a);
      b_aSize = temp.size();
      b_a = getPrettySet(temp, abbreviate, escape);

      temp = new UnicodeSet(a).retainAll(b);
      abSize = temp.size();
      ab = getPrettySet(temp, abbreviate, escape);
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

  static Collator col = Collator.getInstance(ULocale.ROOT);
  static {
    ((RuleBasedCollator) col).setNumericCollation(true);
  }

  public static void showProperties(int cp, Appendable out) throws IOException {
    String text = UTF16.valueOf(cp);
    String name = factory.getProperty("Name").getValue(cp);
    if (name != null) {
      name = toHTML.transliterate(name);
    } else {
      name = "<i>Unknown</i>";
    }
    String scriptCat = factory.getProperty("script").getValue(cp).replace("_", " ");
    if (scriptCat.equals("Common") || scriptCat.equals("Inherited")) {
      scriptCat = factory.getProperty("gc").getValue(cp).replace("_", " ");
    } else {
      scriptCat += " Script";
    }
    
    String hex = com.ibm.icu.impl.Utility.hex(cp, 4);

    out.append("<div class='bigDiv'><table class='bigTable'>\n");
    out.append("<tr><td class='bigChar'>\u00A0" + toHTML.transliterate(text) + "\u00A0</td></tr>\n");
    out.append("<tr><td class='bigCode'>" + hex + "</td></tr>\n");
    out.append("<tr><td class='bigName'>" + name + "</td></tr>\n");
    out.append("<tr><td class='bigName'>" + scriptCat + "</td></tr>\n");
    out.append("</table></div>\n");

    List<String> availableNames = (List<String>)factory.getAvailableNames();
    TreeSet<String> sortedProps = Builder
    .with(new TreeSet<String>(col))
    .addAll(availableNames)
    .remove("Name")
    .get();

    out.append("<table class='propTable'>" 
            + "<caption>Properties for U+" + hex + "</caption>"
            + "<tr><th>With Non-Default Values</th><th>With Default Values</th></tr>" + 
    		"<tr><td width='50%'>\n");
    out.append("<table width='100%'>\n");

    for (String propName : sortedProps) {
      UnicodeProperty prop = factory.getProperty(propName);
      boolean isDefault = prop.isDefault(cp);
      if (isDefault) continue;
      String propValue = prop.getValue(cp);
      showPropertyValue(propName, propValue, isDefault, out); 
    }
    out.append("</table>\n");

    out.append("</td><td width='50%'>\n");

    out.append("<table width='100%'>\n");
    for (String propName : sortedProps) {
      UnicodeProperty prop = factory.getProperty(propName);
      boolean isDefault = prop.isDefault(cp);
      if (!isDefault) continue;
      String propValue = prop.getValue(cp);
      showPropertyValue(propName, propValue, isDefault, out); 
    }
    out.append("</table>\n");

    out.append("</td></tr></table>\n");
  }

  private static void showPropertyValue(String propName, String propValue, boolean isDefault, Appendable out) throws IOException {
    String defaultClass = isDefault ? " class='default'" : "";
    if (propValue == null) {
      out.append("<tr><th><a target='c' href='properties.jsp#" + propName + "'>" + propName + "</a></th><td"  +defaultClass+
      "><i>null</i></td></tr>\n");
      return;
    }
    String hValue = toHTML.transliterate(propValue);
    hValue = "<a target='u' href='list-unicodeset.jsp?a=[:"
      + propName + "=" + propValue + ":]'>" + hValue + "</a>";

    out.append("<tr><th><a target='c' href='properties.jsp#" + propName + "'>" + propName + "</a></th><td"  +defaultClass+
            ">" + hValue + "</td></tr>\n");
  }

  /*jsp*/
  public static void showPropsTable(Appendable out, String propForValues, String myLink) throws IOException {
    Collator col = Collator.getInstance(ULocale.ROOT);
    ((RuleBasedCollator)col).setNumericCollation(true);
    Map<String, Map<String, String>> alpha = new TreeMap<String, Map<String, String>>(col);
    Map<String, String> longToShort = new HashMap<String, String>();

    Set<String> showLink = new HashSet<String>();

    out.append("<table>\n");
    for (String propName : Builder.with(new TreeSet<String>(col)).addAll((List<String>)factory.getAvailableNames()).get()) {
      UnicodeProperty prop = factory.getProperty(propName);
      String propHtml = toHTML.transform(propName);
      String shortName = prop.getFirstNameAlias();
      String title = shortName == null ? "" : " title='" + toHTML(shortName) + "'";
      out.append("<tr><th width='1%'" + title + "><a name='" + propHtml + "'>" + propHtml + "</a></th>\n");
      out.append("<td>\n");
      if (propName.equals(propForValues)) {
        List<String> availableValues = (List<String>)prop.getAvailableValues();
        TreeSet<String> sortedList = Builder.with(new TreeSet<String>(col)).addAll(availableValues).get();
        int count = 500;
        for (String valueName : sortedList) {
          if (--count < 0) {
            out.append("\n<i>too many values to show</i>");
            break;
          }
          String valueHtml = toHTML.transform(valueName);
          String shortValue = prop.getFirstValueAlias(valueName);
          if (valueName.startsWith("<") && valueName.endsWith(">")) {
            out.append(valueHtml);
          } else {
            out.append(getPropLink(propHtml, valueHtml, valueHtml, shortValue));
          }
          out.append("\n");
        }
      } else {
        out.append("<a href='" + myLink + "?a=" + propName + "#" + propName + "'>Show Values</a>");
      }
      out.append("</td></tr>\n");
    }
    out.append("</table>\n");
  }

  private static String getPropLink(String propName, String propValue, String linkText, String shortName) {
    final String propExp = 
      propValue == "T" ? propName
              : propValue == "F" ? "^" + propName
                      : propName + "=" + propValue;
    String title = shortName == null ? "" : " title='" + toHTML(shortName) + "'";
    return "<a target='u' href='list-unicodeset.jsp?a=[:" + propExp + ":]'" + title + 
    ">" + linkText + "</a>";
  }

  static Subheader getSubheader() {
    if (subheader == null) {
      // /home/users/jakarta/apache-tomcat-6.0.14/bin
      // /home/users/jakarta/apache-tomcat-6.0.14/webapps/cldr/utility
      subheader = new Subheader(UnicodeUtilities.class.getResourceAsStream("NamesList.txt"));
      //      try {
      //        final String unicodeDataDirectory = "../webapps/cldr/utility/";
      //        //System.out.println(canonicalPath);
      //        subheader = new Subheader(unicodeDataDirectory);
      //      } catch (IOException e) {
      //        try {
      //          final String unicodeDataDirectory = "./jsp/";
      //          subheader = new Subheader(unicodeDataDirectory);
      //        } catch (IOException e2) {
      //          final String[] list = new File("home").list();
      //          String currentDirectory = list == null ? null : new TreeSet<String>(Arrays.asList(list)).toString();
      //          throw (RuntimeException) new IllegalArgumentException("Can't find file starting from: <" + currentDirectory + ">").initCause(e);
      //        }
      //      }
    }
    return subheader;
  }

  //static IdnaLabelTester tester = null;
  static String removals = UnicodeSetUtilities.parseUnicodeSet("[\u1806[:di:]-[:cn:]]", TableStyle.simple).complement().complement().toPattern(false);
  static Matcher rem = Pattern.compile(removals).matcher("");


  //  static IdnaLabelTester getIdna2008Tester() {
  //    if (tester == null) {
  //      try {
  //        URL path = UnicodeUtilities.class.getResource("idnaContextRules.txt");
  //        String externalForm = path.toExternalForm();
  //        if (externalForm.startsWith("file:")) {
  //          externalForm = externalForm.substring(5);
  //        }
  //        tester = new IdnaLabelTester(externalForm);
  //      } catch (IOException e) {
  //        throw new IllegalArgumentException(e);
  //      }
  //    }
  //    return tester;
  //  }

  static void addBlank(StringBuilder resultLines) {
    resultLines.append("<tr><td colSpan='5'>&nbsp;</td></tr>\n");
  }

  static void addCell(StringBuilder resultLines, Transliterator hex, String tr46, String attributes, String confusableChoice) {
    if (tr46 == null) {
      resultLines.append("<td " +
              attributes +
      "><i>fails</i></td>\n");
    } else {
      String escaped = showEscaped(tr46);
      String linkStart = "", linkEnd = "";
      if (confusableChoice != null) {
        linkStart = "<a target='confusables' href='confusables.jsp?&r=" +
        confusableChoice +
        "&a=" + toHTML.transform(tr46) + "'>";
        linkEnd = "</a>";
      }
      resultLines.append("<td " +
              attributes +
              (" title='" + hex.transform(tr46) + "'") +
      ">")
      .append(linkStart)
      .append(escaped)
      .append(linkEnd)
      .append("</td>\n");
    }
  }

  public static final UnicodeSet TO_QUOTE = new UnicodeSet("[[:z:][:me:][:mn:][:di:][:c:]-[\u0020]]");

  static final Transliterator ESCAPER = Transliterator.createFromRules("escaper", 
          "(" + TO_QUOTE + ") > '<span class=\"q\">'&any-hex($1)'</span>';"
          + HTML_RULES_CONTROLS, Transliterator.FORWARD);

  public static final UnicodeSet SYMBOL = new UnicodeSet("[:s:]").freeze();
  public static final UnicodeSet PUNCTUATION = new UnicodeSet("[:p:]").freeze();

  private static String showEscaped(String line) {
    String toShow = toHTML.transform(line);
    String escaped = ESCAPER.transform(line);
    if (!escaped.equals(toShow)) {
      toShow += "<br><span class='esc'>" + escaped + "</span>";
    }
    return toShow;
  }

  public static String showBidi(String str, int baseDirection, boolean asciiHack) {
    // warning, only BMP for now
    final StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);

    BidiCharMap bidiCharMap = new BidiCharMap(asciiHack);

    String[] parts = str.split("\\r\\n?|\\n");
    for (int i = 0; i < parts.length; ++i) {
      writer.println("<h3>Paragraph " + (i+1) + "</h3>");
      if (parts[i] == null || parts[i].length() == 0) {
        continue;
      }
      showBidiLine(parts[i], baseDirection, writer, bidiCharMap);
    }

    if (asciiHack) {
      writer.println("<h3>ASCII Hack</h3>");
      writer.println("<p>For testing the UBA with only ASCII characters, the following property values are used (<,> are RLM and LRM):</p>");
      writer.println("<table>");
      for (byte i = 0; i < BidiReference.typenames.length; ++i) {
        final UnicodeSet modifiedClass = bidiCharMap.getAsciiHack(i);
        writer.println("<tr><th>" + BidiReference.getHtmlTypename(i) + "</th><td>" + getList(modifiedClass) + "</td></tr>"); 
      }
      writer.println("</table>");
    }

    writer.flush();
    return stringWriter.toString();
  }

  private static String getList(final UnicodeSet uset) {
    StringBuffer codePointString = new StringBuffer();
    for (UnicodeSetIterator it = new UnicodeSetIterator(uset); it.next();) {
      if (codePointString.length() != 0) {
        codePointString.append(" ");
      }
      final String literal = it.codepoint <= 0x20 ? "\u00AB" + getLiteral(UCharacter.getExtendedName(it.codepoint)) + "\u00BB" : getLiteral(it.codepoint);
      codePointString.append(literal);
    }
    return codePointString.toString();
  }

  private static void showBidiLine(String str, int baseDirection, PrintWriter writer, BidiCharMap bidiCharMap) {
    byte[] codes = new byte[str.length()];
    for (int i = 0; i < str.length(); ++i) {
      codes[i] = bidiCharMap.getBidiClass(str.charAt(i));
    }
    int[] linebreaks = new int[1];
    linebreaks[0] = str.length();

    BidiReference bidi = new BidiReference(codes, (byte)baseDirection);
    int[] reorder = bidi.getReordering(new int[] { codes.length });
    byte[] levels = bidi.getLevels(linebreaks);

    writer.println("<table><tr><th>Base Level</th>");
    final byte baseLevel = bidi.getBaseLevel();
    writer.println("<td>" + baseLevel + " = " + (baseLevel == 0 ? "LTR" : "RTL") + "</td><td>" + (baseDirection >= 0 ? "explicit" : "heuristic") + "</td>");
    writer.println("</tr></table>");

    // output original text
    writer.println("<h3>Source</h3>");
    writer.println("<table><tr><th>Memory Position</th>");
    for (int i = 0; i < str.length(); ++i) {
      writer.println("<td class='bcell'>" + i + "</td>");
    }
    writer.println("</tr><tr><th>Character</th>");
    for (int i = 0; i < str.length(); ++i) {
      final String s = str.substring(i,i+1);
      String title = toHTML.transform(getName(s, "", true));
      writer.println("<td class='bccell' title='" + title + "'> " + getLiteral(getBidiChar(str, i, codes[i])) + " </td>");
    }
    writer.println("</tr><tr><th>Bidi Class</th>");
    for (int i = 0; i < str.length(); ++i) {
      writer.println("<td class='bcell'><tt>" + BidiReference.getHtmlTypename(codes[i]) + "</tt></td>");
    }
    writer.println("</tr><tr><th>Rules Applied</th>");
    for (int i = 0; i < str.length(); ++i) {
      writer.println("<td class='bcell'><tt>" + bidi.getChanges(i).replace("\n", "<br>") + "</tt></td>");
    }
    writer.println("</tr><tr><th>Resulting Level</th>");
    for (int i = 0; i < str.length(); ++i) {
      writer.println("<td class='bcell'><tt>" + showLevel(levels[i]) + "</tt></td>");
    }
    writer.println("</tr></table>");

    // output visually ordered text
    writer.println("<h3>Reordered</h3>");
    writer.println("<table><th>Display Position</th>");
    for (int k = 0; k < str.length(); ++k) {
      final int i = reorder[k];
      final String bidiChar = getBidiChar(str, i, codes[i]);
      String td = bidiChar.length() == 0 ? "<td class='bxcell'>" : "<td class='bcell'>";
      writer.println(td + k + "</td>");
    }
    writer.println("</tr><tr><th>Memory Position</th>");
    for (int k = 0; k < str.length(); ++k) {
      final int i = reorder[k];
      final String bidiChar = getBidiChar(str, i, codes[i]);
      String td = bidiChar.length() == 0 ? "<td class='bxcell'>" : "<td class='bcell'>";
      writer.println(td + i + "</td>");
    }
    writer.println("</tr><tr><th>Character</th>");
    for (int k = 0; k < str.length(); ++k) {
      final int i = reorder[k];
      final String bidiChar = getBidiChar(str, i, codes[i]);
      String title = bidiChar.length() == 0 ? "deleted" : toHTML.transform(getName(bidiChar, "", true));
      String td = bidiChar.length() == 0 ? "bxcell" : "bccell";
      writer.println("<td class='" + td + "' title='" + title + "'>" + " " + getLiteral(bidiChar) +"</td>");
    }
    writer.println("</tr></table>");

  }

  private static String getBidiChar(String str, int i, byte b) {
    if (b == BidiReference.PDF || b == BidiReference.RLE || b == BidiReference.LRE || b == BidiReference.LRO || b == BidiReference.RLO || b == BidiReference.BN) {
      return "";
    }
    String substring = str.substring(i,i+1);
    if ((substring.equals("<") || substring.equals(">")) && (b == BidiReference.L || b == BidiReference.R)) {
      return "";
    }
    return substring;
  }

  private static String showLevel(int level) {
    StringBuffer result = new StringBuffer();
    for (int i = 0; i < level; ++i) {
      result.append("<br>");
    }
    result.append("L").append(level);
    return result.toString();
  }

  public static String testIdnaLines(String lines, String filter) {
    Transliterator hex = Transliterator.getInstance("any-hex");
    try {

      lines = UnicodeJsp.UNESCAPER.transform(lines.trim());
      StringBuilder resultLines = new StringBuilder();
      //UnicodeUtilities.getIdna2008Tester();

      Predicate<String> verifier2008 = new Predicate<String>() {
        public boolean is(String item) {
          return Idna2008.SINGLETON.isValid(item);
        }
      };

      resultLines.append("<table>\n");
      resultLines.append("<th></th><th class='cn'>Input</th><th class='cn'>IDNA2003</th><th class='cn'>UTS46</th><th class='cn'>IDNA2008</th>\n");

      boolean first = true;
      boolean[] errorOut = new boolean[1];

      for (String line : lines.split("\\s+")) {
        if (first) {
          first = false;
        } else {
          addBlank(resultLines);
        }

        String rawPunycode = UnicodeUtilities.processLabels(line, IdnaTypes.DOTS, true, new Predicate() {
          public boolean is(Object item) {
            return true;
          }});


        //        String tr46 = UnicodeUtilities.processLabels(tr46back, UnicodeUtilities.DOTS, true, new Predicate<String>() {
        //          public boolean is(String item) {
        //            return Uts46.SINGLETON.transform(item).indexOf('\uFFFD') < 0; // Uts46.SINGLETON.Uts46Chars.containsAll(item);
        //          }
        //        });
        //        String tr46display = Uts46.SINGLETON.toUnicode(line, errorOut);
        //        tr46display = UnicodeUtilities.processLabels(tr46display, UnicodeUtilities.DOTS, false, new Predicate<String>() {
        //          public boolean is(String item) {
        //            return Uts46.SINGLETON.toUnicode(item).indexOf('\uFFFD') < 0; // Uts46.SINGLETON.Uts46Chars.containsAll(item);
        //            //return Uts46.SINGLETON.Uts46CharsDisplay.containsAll(item);
        //          }
        //        });


        // first lines
        resultLines.append("<tr>");
        resultLines.append("<th>Display</th>");
        addCell(resultLines, hex, line, "class='cn ltgreen'", "None");
        String idna2003unic = Idna2003.SINGLETON.toUnicode(line, errorOut, true);
        addCell(resultLines, hex, idna2003unic, getIdnaClass("cn i2003", errorOut[0]), "IDNA2003");

        String uts46unic = Uts46.SINGLETON.toUnicode(line, errorOut, true);
        addCell(resultLines, hex, uts46unic, getIdnaClass("cn i46", errorOut[0]), "UTS46%2BUTS39");

        String idna2008unic = UnicodeUtilities.processLabels(line, IdnaTypes.DOT, false, verifier2008);
        addCell(resultLines, hex, idna2008unic, getIdnaClass("cn i2008", idna2008unic.contains("\uFFFD")), "IDNA2003");
        resultLines.append("<tr></tr>");

        resultLines.append("<th class='mono'>Punycode</th>");
        addCell(resultLines, hex, rawPunycode, "class='cn ltgreen mono'", null);
        String idna2003puny = Idna2003.SINGLETON.toPunyCode(line, errorOut);
        addCell(resultLines, hex, idna2003puny, getIdnaClass("cn mono i2003", errorOut[0]), null);

        String uts46puny = Uts46.SINGLETON.toPunyCode(line, errorOut);
        addCell(resultLines, hex, uts46puny, getIdnaClass("cn mono i46", errorOut[0]), null);

        String idna2008puny = UnicodeUtilities.processLabels(line, IdnaTypes.DOT, true, verifier2008);
        addCell(resultLines, hex, idna2008puny, getIdnaClass("cn mono i2008", idna2008puny.contains("\uFFFD")), null);

        //        if (result == null) {
        //          resultLines.append("<td class='c'>\u00A0</td><td class='c'>\u00A0</td>");
        //        } else {
        //          resultLines.append("<td class='c'>")
        //          .append(toHTML.transform(IdnaLabelTester.ESCAPER.transform(normalized.substring(0, result.position))) 
        //                  + "<span class='x'>\u2639</span>" + toHTML.transform(IdnaLabelTester.ESCAPER.transform(normalized.substring(result.position))) 
        //                  + "</td><td>" + result.title
        //                  //+ "</td><td class='c'>" + result.ruleLine
        //                  + "</td>");
        //        }
        resultLines.append("</tr>\n");
      }

      resultLines.append("</table>\n");
      return resultLines.toString();
    } catch (Exception e) {
      return toHTML.transform(e.getMessage());
    }
  }

  private static String getIdnaClass(String classItems, boolean error) {
    return "class='" +
    classItems + (error ? " error" : "") + "'";
  }

  static String processLabels(String inputLabels, Pattern dotPattern, boolean punycode, Predicate<String> verifier) {
    StringBuilder result = new StringBuilder();
    for (String label : dotPattern.split(inputLabels)) {
      if (result.length() != 0) {
        result.append('.');
      }
      try {
        if (!verifier.is(label)) {
          throw new IllegalArgumentException();
        }
        if (!punycode || IdnaTypes.ASCII.containsAll(label)) {
          result.append(label);
        } else {
          StringBuffer puny = Punycode.encode(new StringBuffer(label), null);
          if (puny.length() == 0) {
            throw new IllegalArgumentException();
          }
          result.append("xn--").append(puny);
        }
      } catch (Exception e) {
        result.append('\uFFFD');
      }
    }
    return result.toString();
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