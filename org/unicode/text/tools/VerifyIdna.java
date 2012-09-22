package org.unicode.text.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParsePosition;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.ToolUnicodePropertySource;
import org.unicode.text.UCD.UCD_Types;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.dev.util.ICUPropertyFactory;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeProperty;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public class VerifyIdna {
  
  static final boolean USE_ICU = true;
  
  // 011B ; PVALID # LATIN SMALL LETTER E WITH CARON
  static final Matcher DATALINE = Pattern.compile(
          "([0-9a-fA-F]{4,6})" +
          "(?:\\.\\.([0-9a-fA-F]{4,6}))?" +
          "\\s*;\\s*" +
          "(PVALID|DISALLOWED|UNASSIGNED|CONTEXTJ|CONTEXTO)" +
          "\\s*#\\s*" +
          "(.*)").matcher("");

  public enum IdnaType {
    PVALID, DISALLOWED, UNASSIGNED, CONTEXTJ, CONTEXTO
  }

  public static void main(String[] args) throws IOException {
    try {
      UnicodeMap patrik = getPatriksMapping();
      UnicodeMap alternate = getAlternativeDerivation();
      
      final Set<IdnaType> patrickValues = new TreeSet<IdnaType>(patrik.getAvailableValues());
      final Set<IdnaType> altValues = new TreeSet<IdnaType>(alternate.getAvailableValues());
      if (!patrickValues.equals(altValues)) {
        System.out.println("Difference in Values");
        System.out.println("\tpat:\t" + patrickValues);
        System.out.println("\talt:\t" + altValues);
        System.out.println();
      }
      
      BagFormatter bf = new BagFormatter();

      for (IdnaType idnaType : patrickValues) {
        System.out.println(idnaType);
        UnicodeSet patItems = patrik.keySet(idnaType);
        UnicodeSet altItems = alternate.keySet(idnaType);
        if (patItems.equals(altItems)) {
          System.out.println("\tequal");
        } else {
          System.out.println("Difference in Contents");
          //System.out.println(bf.showSetDifferences("pat", patItems, "alt", altItems));
          final UnicodeSet patDiffAlt = new UnicodeSet(patItems).removeAll(altItems);
          System.out.println("\tpat-alt:\r\n" + bf.showSetNames(patDiffAlt));
          final UnicodeSet altDiffPat = new UnicodeSet(altItems).removeAll(patItems);
          System.out.println("\talt-pat:\r\n"  + bf.showSetNames(altDiffPat));
          //System.out.println("\tpat:\t" + patItems);
          //System.out.println("\talt:\t" + altItems);
        }
        System.out.println();
      }
    } finally {
      System.out.println("DONE");
    }
  }

  private static UnicodeMap getAlternativeDerivation() {
    UnicodeMap result = new UnicodeMap();
    UnicodeSet foo = parseUnicodeSet("[[:gc=cn:]-[:NChar:]]");
    
    System.out.println("A\t" + parseUnicodeSet("[" +
            "[[:gc=Ll:][:gc=Lt:][:gc=Lu:][:gc=Lo:][:gc=Lm:][:gc=Mn:][:gc=Mc:][:gc=Nd:]]" + // A - restrict to only letters, marks, numbers
           "]").complement().complement());
    
    result.putAll(parseUnicodeSet("[\\u0000-\\U0010FFFF]"), IdnaType.DISALLOWED); // Assume disallowed unless we set otherwise
    result.putAll(parseUnicodeSet("[[:gc=cn:]-[:NChar:]]"), IdnaType.UNASSIGNED); // J - unassigned code points // -[:NChar:]
    //parseUnicodeSet("[[:gc=cn:]]");
    result.putAll(parseUnicodeSet("[" +
            "[[:gc=Ll:][:gc=Lt:][:gc=Lu:][:gc=Lo:][:gc=Lm:][:gc=Mn:][:gc=Mc:][:gc=Nd:]]" + // A - restrict to only letters, marks, numbers
            "-[[:^isCaseFolded:]]" + // B - minus characters unstable under NFKC & casefolding
            "-[:di:]" + // C - minus default-ignorables
            "-[[:block=Combining_Diacritical_Marks_for_Symbols:]" + // D minus exceptional block exclusions
              "[:block=Musical_Symbols:]" +
              "[:block=Ancient_Greek_Musical_Notation:]" +
              "[:block=Phaistos_Disc:]]" + // x 
            "[\\u3007]" + // x 
            "]"), IdnaType.PVALID);
    result.putAll(parseUnicodeSet("[" +
            "[\u002D\u00B7\u02B9\u0375\u0483\u05F3\u05F4\u3005\u303B\u30FB]" + // F.2 - exceptional contextual characters
            "[:gc=cf:]" + // I - other Cf characters (should be omitted)
            "]"), IdnaType.CONTEXTO);
    result.putAll(parseUnicodeSet("[:join_control:]"), IdnaType.CONTEXTJ);  // H - join controls
    result.freeze();
    return result;
  }

  private static UnicodeMap getPatriksMapping() throws IOException {
    BufferedReader in = BagFormatter.openReader(Utility.DATA_DIRECTORY + "/IDN/",
            "draft-faltstrom-idnabis-tables-05.txt", "ascii");
    boolean inTable = false;
    UnicodeMap patrik = new UnicodeMap();
    int count = 0;
    while (true) {
      String line = in.readLine();
      if (line == null)
        break;
      if ((count++ % 100) == 0) {
        System.out.println(count + " " + line);
      }
      if (line.startsWith("A.1.")) {
        inTable = true;
        continue;
      }
      if (line.startsWith("Author's Address")) {
        break;
      }
      if (!inTable)
        continue;
      line = line.trim();
      if (line.length() == 0 || line.startsWith("Faltstrom") || line.startsWith("Internet-Draft"))
        continue;
      // we now have real data
      if (!DATALINE.reset(line).matches()) {
        System.out.println("Error: line doesn't match: " + line);
        continue;
      }
      final int startChar = Integer.parseInt(DATALINE.group(1), 16);
      final int endChar = DATALINE.group(2) == null ? startChar : Integer.parseInt(DATALINE
              .group(2), 16);
      final IdnaType idnaType = IdnaType.valueOf(DATALINE.group(3));
      patrik.putAll(startChar, endChar, idnaType);
    }
    in.close();
    patrik.freeze();
    return patrik;
  }
    
  static class PropertySymbolTable extends UnicodeSet.XSymbolTable {
    UnicodeProperty.Factory unicodePropertyFactory;
    UnicodeSet isCaseFolded;
    
    public PropertySymbolTable(UnicodeProperty.Factory unicodePropertyFactory) {
      this.unicodePropertyFactory = unicodePropertyFactory;
      UnicodeProperty gc = unicodePropertyFactory.getProperty("gc");
      UnicodeProperty ideo = unicodePropertyFactory.getProperty("ideographic");
      UnicodeSet invariant = new UnicodeSet()
      .addAll(gc.getSet("cc"))
      .addAll(gc.getSet("cn"))
      .addAll(gc.getSet("co"))
      .addAll(gc.getSet("cs"))
      //.addAll(ideo.getSet("t"))
      ;
      isCaseFolded = new UnicodeSet(invariant);

      for (UnicodeSetIterator it = new UnicodeSetIterator(new UnicodeSet(invariant).complement()); it.nextRange();) {
        for (int cp = it.codepoint; cp <= it.codepointEnd; ++cp) {
          String s = UTF16.valueOf(cp);
          if (getNormalizedCaseFolded(getNormalizedCaseFolded(s)).equals(s)) {
            isCaseFolded.add(cp);
          }
        }
      }
    }

    private String getNormalizedCaseFolded(String s) {
      if (USE_ICU) {
        return Normalizer.normalize(UCharacter.foldCase(s,true), Normalizer.COMPOSE_COMPAT);
      } else {
        return Default.nfkc().normalize(Default.ucd().getCase(s, UCD_Types.FULL, UCD_Types.FOLD));
      }
    }
    
    public boolean applyPropertyAlias(String propertyName,
            String propertyValue, UnicodeSet result) {
//    String trimmedPropertyValue = propertyValue.trim();
//    if (trimmedPropertyValue.startsWith("/") && trimmedPropertyValue.endsWith("/")) {
//    Matcher matcher = Pattern.compile(
//    trimmedPropertyValue.substring(1, trimmedPropertyValue.length() - 1)).matcher("");
//        UnicodeSet temp = unicodePropertyFactory.getSet(propertyName,);
//      }
      result.clear();
      if (propertyName.equals("isCaseFolded")) {
        result.addAll(isCaseFolded);
        return true;
      }
      UnicodeSet temp;
      try {
        if (propertyValue.length() != 0) {
          temp = getSet(propertyName, propertyValue);
        } else {
          temp = getSet("gc", propertyValue);
          if (temp != null) {
            temp = getSet("sc", propertyValue);
            if (temp != null) {
              temp = getSet(propertyName, "t");
            }
          }
        }
      } catch (RuntimeException e) {
        System.out.println("Failed on " + propertyName + "=" + propertyValue);
        e.printStackTrace();
        throw e;
      }
      if (temp.size() == 0) {
        System.out.println("NULL on " + propertyName + "=" + propertyValue);
      }
      result.addAll(temp);
      return true;
    }

    private UnicodeSet getSet(String propertyName, String propertyValue) {
      return unicodePropertyFactory.getSet(propertyName + "=" + propertyValue);
    }
  };
  
  static PropertySymbolTable myXSymbolTable = null;
  
  public static UnicodeSet parseUnicodeSet(String input) {
    if (myXSymbolTable == null) {
      if (USE_ICU) {
        myXSymbolTable = new PropertySymbolTable(ICUPropertyFactory.make());
      } else {
        myXSymbolTable = new PropertySymbolTable(ToolUnicodePropertySource.make(""));
      }
    }
    input = input.trim();
    ParsePosition parsePosition = new ParsePosition(0);
    UnicodeSet result = new UnicodeSet(input, parsePosition, myXSymbolTable);
    if (parsePosition.getIndex() != input.length()) {
      throw new IllegalArgumentException("Additional characters past the end of the set, at " 
          + parsePosition.getIndex() + ", ..." 
          + input.substring(Math.max(0, parsePosition.getIndex() - 10), parsePosition.getIndex())
          + "|"
          + input.substring(parsePosition.getIndex(), Math.min(input.length(), parsePosition.getIndex() + 10))
          );
    }
    return result;
  }

}
