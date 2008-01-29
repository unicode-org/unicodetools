package org.unicode.text.tools;

import org.unicode.cldr.util.Log;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.Tabber;
import com.ibm.icu.dev.test.util.TransliteratorUtilities;
import com.ibm.icu.dev.test.util.UnicodeLabel;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.dev.test.util.UnicodeProperty;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.Normalizer;
import org.unicode.text.UCD.ToolUnicodePropertySource;
import org.unicode.text.UCD.UCD;
import org.unicode.text.UCD.UCD_Names;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class VerifyUCD {
  
  public static void main(String[] args) throws IOException {
    String x = Default.ucd().getCase("\u0130", UCD.FULL, UCD.LOWER);
    String y = Default.ucd().getCase(Default.nfd().normalize("\u0130"), UCD.FULL, UCD.LOWER);
    
    Log.setLog("C:\\DATA\\GEN\\verifyUCD.html");
    Log.logln("<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'>");
    Log.logln("<title>UCD Canonical Check</title></head><body>");
    Log.getLog().println("<h2 align='right'>L2/06-386R2</h2>");
    Log.logln("<h1>UCD Canonical Check</title></h1>");
    //Log.logln("<p>" + new java.util.Date() + "</title></p>");
    try {
      org.unicode.cldr.util.Utility.callMethod(System.getProperty("method"), VerifyUCD.class);
    } finally {
      System.out.println("Done");
      Log.logln("</body></html>");
      Log.close();
    }
  }

  static final int COMBINING_MASK = (1<<UCD.Mc) | (1<<UCD.Me) | (1<<UCD.Mn);
  
  public static void checkBidiMirroredNonspacingMarks() {
    ToolUnicodePropertySource ups = ToolUnicodePropertySource.make(Default.ucdVersion());
    UnicodeSet bidiMirrored = ups.getSet("BidiMirrored=true");
    UnicodeSet contents = new UnicodeSet();
    UnicodeSet marks = new UnicodeSet(ups.getSet("generalcategory=Mn"));
    marks.addAll(ups.getSet("generalcategory=Me"));
    marks.addAll(ups.getSet("generalcategory=Mc"));
    for (UnicodeSetIterator it = new UnicodeSetIterator(bidiMirrored); it.next();) {
      int codepoint = it.codepoint;
      addMarks(contents, marks, codepoint, Default.nfd());
      addMarks(contents, marks, codepoint, Default.nfc());
      addMarks(contents, marks, codepoint, Default.nfkd());
      addMarks(contents, marks, codepoint, Default.nfkc());
    }
    BagFormatter bf = new BagFormatter();
    bf.setShowLiteral(TransliteratorUtilities.toHTMLControl);
    bf.setTabber(new Tabber.HTMLTabber());
    Log.logln("<p><b>*Characters with Bidi_Mirrored=True containing one or more marks*</b></p>");
    bf.showSetNames(Log.getLog(),contents);
  }

  private static void addMarks(UnicodeSet contents, UnicodeSet marks, int codepoint, Normalizer normalizer) {
    if (marks.containsSome(normalizer.normalize(codepoint))) {
      contents.add(codepoint);
    }
  }
  
  public static void checkCanonicalEquivalenceOfProperties() {
    ToolUnicodePropertySource ups = ToolUnicodePropertySource.make(Default.ucdVersion());
    UnicodeSet nonNFD = ups.getSet("NFDQuickCheck=No");
    TreeSet<String> properties = new TreeSet<String>();
    Set<String> availablePropertyNames = new TreeSet<String>(ups.getAvailableNames(UnicodeProperty.BINARY_MASK 
        | UnicodeProperty.ENUMERATED_OR_CATALOG_MASK 
        | (1<<UnicodeProperty.NUMERIC
            | UnicodeProperty.STRING_OR_MISC_MASK)));
    Set<String> removals = new TreeSet<String>(Arrays.asList(new String[] { "Name", 
        "Unicode_1_Name", "East_Asian_Width",
        "IdnOutput",

        "Simple_Case_Folding", 
        "Simple_Titlecase_Mapping", "Simple_Lowercase_Mapping",
        "Simple_Uppercase_Mapping", 
        /*
        "Titlecase_Mapping", "Lowercase_Mapping",
        "Uppercase_Mapping", 
        */
        "Case_Stable",

        "Decomposition_Mapping",
        "Age", "Composition_Exclusion", "Canonical_Combining_Class", "Pattern_Syntax", "Pattern_White_Space", "Expands_On_NFC", "Expands_On_NFD",
        "Expands_On_NFKC", "Expands_On_NFKD", "Block", "Decomposition_Type", "Deprecated", "Full_Composition_Exclusion", 
        "NFC_Quick_Check", "Unified_Ideograph", "NFD_Quick_Check", "NFKC_Quick_Check", "NFKD_Quick_Check",
        "Other_Alphabetic", "Other_Default_Ignorable_Code_Point", "Other_Grapheme_Extend", "Other_ID_Continue", "Other_ID_Start", "Other_Lowercase", "Other_Math", "Other_Uppercase"
        }));
    removals.retainAll(availablePropertyNames);
    UnicodeSet forceNFC = new UnicodeSet()
    .addAll(ups.getSet("Hangul_Syllable_Type=LV_Syllable"))
    .addAll(ups.getSet("Hangul_Syllable_Type=LVT_Syllable"))
    .addAll(ups.getSet("General_Category=Titlecase_Letter"))
    .addAll("\u1B3B\u1B3D\u1B43\u0CC0\u0CC7\u0CC8\u0CCA\u0CCB")
    ;
    Set<String> singleCharOnly = new TreeSet<String>(Arrays.asList(new String[] { 
        "ASCII_Hex_Digit", "Hex_Digit", "Bidi_Mirroring_Glyph", "Soft_Dotted"}));
    
    //System.out.println("Other:\t" + ups.getAvailableNames(UnicodeProperty.STRING_OR_MISC_MASK));
    //removals.addAll(ups.getAvailableNames(UnicodeProperty.STRING_OR_MISC_MASK));
    availablePropertyNames.removeAll(removals);
    Log.getLog().println("<table>");
    Log.getLog().println("<tr><td><b>Testing:</b></td><td>" + availablePropertyNames + "</td><tr>");
    Log.getLog().println("<tr><td><b>Skipping:</b></td><td>" + removals + "</td><tr>");
    Log.getLog().println("</table><br>");
    Log.logln("<hr>");
   UnicodeMap results = new UnicodeMap();
    Map<String,UnicodeMap> sidewaysResults = new TreeMap<String,UnicodeMap>();
    
    // http://demo.icu-project.org/icu-bin/ubrowse?go=2224
    for (UnicodeSetIterator it = new UnicodeSetIterator(nonNFD); it.next();) {
      int codepoint = it.codepoint;
      String nfdOrNfc = (forceNFC.contains(codepoint) ? Default.nfc() : Default.nfd()).normalize(codepoint);
      properties.clear();
      for (String propertyName : availablePropertyNames) {
        if (UTF16.hasMoreCodePointsThan(nfdOrNfc,1) && singleCharOnly.contains(propertyName)) {
          continue;
        }
        UnicodeProperty up = ups.getProperty(propertyName);
        boolean isStringProp = ((1<<up.getType()) & UnicodeProperty.STRING_OR_MISC_MASK) != 0;

        Object value1 = getValue(up, codepoint);
        int newCodepoint;
        String nfcStringPropertyValue = "";
        for (int i = 0; i < nfdOrNfc.length(); i+=UTF16.getCharCount(newCodepoint)) {
          newCodepoint = UTF16.charAt(nfdOrNfc, i);
          int catMask = Default.ucd().getCategoryMask(newCodepoint);
          // special case strings
          if (isStringProp) {
            Object value2 = getValue(up, newCodepoint);
            if (value2 == null) value2 = UTF16.valueOf(newCodepoint);
            nfcStringPropertyValue += value2;
            continue;
          }

          if (i > 0 && (catMask & COMBINING_MASK) != 0) {
            continue;
          }
          Object value2 = up.getValue(newCodepoint);
          if (!equals(value1,value2, false)) {
            addPropertyDifference(sidewaysResults, properties, codepoint, propertyName, value1, value2);
          }
        }
        if (propertyName.contains("case_Mapping") || propertyName.contains("Case_Folding")) {
          nfcStringPropertyValue = caseMapping(nfdOrNfc, propertyName);
        }
        if (isStringProp && !equals(value1, nfcStringPropertyValue, true)) {
          addPropertyDifference(sidewaysResults, properties, codepoint, propertyName, value1, nfcStringPropertyValue);
        }
      }
      if (properties.size() != 0) {
        results.put(codepoint, properties.clone());
      }
    }
    UnicodeLabel nameLabel = new UnicodeLabel() {
      public String getValue(int codepoint, boolean isShort) {
        String nfd = Default.nfd().normalize(codepoint);
        return Default.ucd().getCodeAndName(codepoint,UCD.NORMAL,TransliteratorUtilities.toHTMLControl)
        + "\t\u2192\t"
        + Default.ucd().getCodeAndName(nfd,UCD.NORMAL,TransliteratorUtilities.toHTMLControl);
      }       
    };
    BagFormatter bf = new BagFormatter();
    bf.setNameSource(nameLabel);
    bf.setTabber(new Tabber.HTMLTabber());
    bf.setMergeRanges(false);
    TreeSet<Set> sorted = new TreeSet<Set>(new CollectionOfComparablesComparator());
    sorted.addAll(results.getAvailableValues());
    for (Object props : sorted) {
      Log.logln("<p><b>" + props + "</b></p>");
      Log.logln(bf.showSetNames(results.getSet(props)));
    }
    
    Log.logln("<hr>");
    Log.logln("<h2>" + "By Property" + "</h1>");
    for (String propName : sidewaysResults.keySet()) {
      UnicodeMap map = sidewaysResults.get(propName);
      bf.setValueSource((new UnicodeProperty.UnicodeMapProperty() {
      }).set(map).setMain(propName + "_diff", propName + "_diff",
          UnicodeProperty.EXTENDED_STRING, "1.0"));

      Log.logln("<p><b>" + propName + "</b></p>");
      Log.logln(bf.showSetNames(map.keySet()));
    }
  }
  
  private static String caseMapping(String source, String propertyName) {
    byte operation = propertyName.contains("Uppercase") ? UCD.UPPER
        : propertyName.contains("Lowercase") ? UCD.LOWER
            : propertyName.contains("Titlecase") ? UCD.TITLE           
                : UCD.FOLD;
    byte style = propertyName.contains("Simple") ? UCD.SIMPLE : UCD.FULL;
    return Default.ucd().getCase(source,style, operation);
  }

  private static void addPropertyDifference(Map<String, UnicodeMap> sidewaysResults, TreeSet<String> properties, int codePoint, String propName, Object value1, Object value2) {
    properties.add(propName + "=" + value1 + "\u2260" + value2);
    UnicodeMap umap = sidewaysResults.get(propName);
    if (umap == null) sidewaysResults.put(propName, umap = new UnicodeMap());
    umap.put(codePoint, value1 + "\u2260" + value2);
  }

  private static Object getValue(UnicodeProperty up, int codepoint) {
    int type = 1<<up.getType();
    Object value1 = up.getValue(codepoint);
    if (value1 == null) {
      if ((type & UnicodeProperty.STRING_OR_MISC_MASK) != 0) {
        value1 = UTF16.valueOf(codepoint);
      } else if ((type & UnicodeProperty.BINARY_MASK) != 0) {
        value1 = UCD_Names.NO;
      }
    }
    return value1;
  }
  
  static boolean equals(Object value1, Object value2, boolean canonical) {
    if (value1 == value2) {
      return true;
    }
    if (value1 == null || value2 == null) {
      return false;
    }
    if (canonical) {
      return Default.nfd().normalize(value1.toString()).equals(Default.nfd().normalize(value2.toString()));
    }
    return value1.equals(value2);
  }
  
  static public class CollectionOfComparablesComparator implements Comparator {
    public int compare(Object o1, Object o2) {
        if (o1 == null) {
            if (o2 == null) return 0;
            return -1;
        } else if (o2 == null) {
            return 1;
        }
        Iterator i1 = ((Collection) o1).iterator();
        Iterator i2 = ((Collection) o2).iterator();
        while (i1.hasNext() && i2.hasNext()) {
            Comparable a = (Comparable) i1.next();
            Comparable b = (Comparable) i2.next();
            int result = a.compareTo(b);
            if (result != 0) {
                return result;
            }
        }
        // if we run out, the shortest one is first
        if (i1.hasNext())
            return 1;
        if (i2.hasNext())
            return -1;
        return 0;
    }
    
}

}