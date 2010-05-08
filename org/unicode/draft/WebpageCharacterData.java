package org.unicode.draft;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.Counter;
import org.unicode.text.UCD.Default;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

/**
 * Here are the latest results of the code point frequencies for one
whole Base segment:

http://www.corp.google.com/~erikv/unicode-count62.txt

The 1st column is the code point.

Then there are 3 groups of 4 columns, where each group is:

pre-HTML code point count, post-HTML code point count, document count, UTF-8 document count

The 1st group includes "bad" docs (error during input conversion or
contains unassigned or high private use), 2nd group excludes "bad"
docs, 3rd group is multiplied by pagerank (and excludes "bad" docs).

Then there are up to 3 groups, where each group is:

navboost, pagerank, language, encoding, url

...Data/unicode-count62.txt
 */
public class WebpageCharacterData {

  enum Columns {
    codePoint,
    preHtmlCount1, postHtmlCount1, documentCount1, utf8DocumentCount1,
    preHtmlCount2, postHtmlCount2, documentCount2, utf8DocumentCount2,
    preHtmlCount3, postHtmlCount3, documentCount3, utf8DocumentCount3;
    static String[] parts;
    public static void set(String line) {
      parts = line.split("\t");
    }
    public String get() {
      return parts[ordinal()];
    }
  }
  static PrintWriter log;

  static NumberFormat integer = NumberFormat.getInstance();
  static NumberFormat percent = NumberFormat.getPercentInstance();
  static NumberFormat percent6 = NumberFormat.getPercentInstance();
  static {
    percent.setMinimumFractionDigits(3);
    percent6.setMinimumFractionDigits(6);
  }
  private static Map<Integer,Counter<Integer>> scriptToChars;

  public static void main(String[] args) throws IOException {
    log = BagFormatter.openUTF8Writer("", "character_frequency_data.txt");
    log.write('\uFEFF');
    doData();
    log.close();
    System.out.println("DONE");
  }

  static public void doData() throws IOException {
    BufferedReader in = BagFormatter.openUTF8Reader("/Users/markdavis/Documents/Data/", "unicode-count62.txt");
    //Counter<Integer> charsPre = new Counter<Integer>();
    Counter<String> charsPost = new Counter<String>();
    Counter<Integer> scriptsPre = new Counter<Integer>();
    Counter<Integer> scriptsPost = new Counter<Integer>();
    scriptToChars = new HashMap<Integer,Counter<Integer>>();
    while (true) {
      String line = in.readLine();
      if (line == null) break;
      Columns.set(line);

      int codePoint = Integer.parseInt(Columns.codePoint.get(), 16);
      long pre = Long.parseLong(Columns.preHtmlCount2.get());
      long post = Long.parseLong(Columns.postHtmlCount2.get());
      int script = getScript(codePoint);
      scriptsPre.add(script, pre);
      scriptsPost.add(script, post);
      String chars = codePointValueOf(codePoint);
      String foldedchars = myFold(chars);
      //charsPre.add(codePoint, pre);
      if (foldedchars.length() == 0) {
        codePointValueOf(codePoint);
        throw new IllegalArgumentException();
      }
      if (post != 0) {
        charsPost.add(chars, post);
      }
      Counter<Integer> x = scriptToChars.get(script);
      if (x == null) {
        scriptToChars.put(script, x = new Counter<Integer>());
      }
      x.add(codePoint, post);
    }
    in.close();
    System.out.println("Done reading data");
    
    PrintWriter out = BagFormatter.openUTF8Writer("", "web_character_frequency.txt");
    Set<String> problems = new LinkedHashSet();
    for (String s : charsPost.getKeysetSortedByCount(false)) {
      if (UTF16.countCodePoint(s) != 1) {
        problems.add(s);
      } else {
        int cp = UTF16.charAt(s, 0);
        out.println(com.ibm.icu.impl.Utility.hex(cp) + " ; " + charsPost.getCount(s)); //  + " # " + UCharacter.getExtendedName(cp));
      }
    }
    if (problems.size() != 0) {
      System.out.println("Problems: " + problems);
    }
    out.println("# END");
    out.close();
    
    if (true) return;
    
    final long preTotal = scriptsPre.getTotal();
    scriptsPost.add(-3, preTotal - scriptsPost.getTotal());
    //Set<Integer> foo = new LinkedHashSet(scriptsPost.getKeysetSortedByCount(false));
    //foo.addAll(scriptsPre.getKeysetSortedByCount(false));
    log.println("Script\tGood/Pre\tGood/Post");
    int limit = 16;
    long soFar = 0;
    for (int script : scriptsPost.getKeysetSortedByCount(false)) {
      final long postScriptCount = scriptsPost.getCount(script);
      showScripts(script, postScriptCount, preTotal);
      soFar += postScriptCount;
      if (--limit < 0) break;
    }
    showScripts(-4, preTotal - soFar, preTotal);

    if (true) {
      UnicodeMap mand = Default.ucd().getHanValue("kDefinition");
      Counter<Integer> han = scriptToChars.get(UScript.HAN);
      UnicodeSet unseenHan = new UnicodeSet("[:script=han:]");
      final double charsTotal = han.getTotal();
      List<String> missingDefinition = new ArrayList();
      List<String> supplemental = new ArrayList();
      int rank = 1;
      for (Integer codePoint : han.getKeysetSortedByCount(false)) {
        final double proportion = han.getCount(codePoint) / charsTotal;
        final String definition = (String) mand.getValue(codePoint);
        final String codePointString = codePointValueOf(codePoint);
        rank = showHanLine(codePointString, rank, proportion, definition);
        unseenHan.remove(codePoint);
        if (definition == null && proportion >= 0.00001) {
          missingDefinition.add(codePointString);
        }
        if (codePoint > 0xFFFF) {
          supplemental.add(codePointString);
        }
      }
      System.out.println("Missing definitions: " + missingDefinition);
      System.out.println("Supplemental: " + supplemental);
      UnicodeSet hkAndJis = new UnicodeSet("[𠮟 𠹭 𡌶 𣕚 𣜿 𣳾 𣵀 𥇍 𥻘 𦍌 𦐂 𦥑 𧄍 𧪄 𨥉 𨨩 𨸶]");
      supplemental.retainAll(hkAndJis.addAllTo(new TreeSet()));
      System.out.println("Supplemental*: " + supplemental);
      for (UnicodeSetIterator it = new UnicodeSetIterator(unseenHan); it.next();) {
        int codePoint = it.codepoint;
        rank = showHanLine(codePointValueOf(codePoint), rank, 0, (String) mand.getValue(codePoint));
      }
    }

    if (false) {
      limit = 1000;
      final double charsTotal = charsPost.getTotal();
      for (String codePoint : charsPost.getKeysetSortedByCount(false)) {
        final double proportion = charsPost.getCount(codePoint) / charsTotal;
        log.println(
                //toHex(codePoint) 
                //+ "\t" + 
                "'" + codePoint + "'"
                //+ "\t" + UCharacter.getExtendedName(codePoint)
                + "\t" + getScriptName(getScript(codePoint), false)
                + "\t" + integer.format(charsPost.getCount(codePoint))
                + "\t" + percent.format(proportion)
        );
        if (--limit < 0 || proportion < 0.00001d) break;
        if ((limit % 10) == 0) {
          System.out.println(limit);
        }
      }
    }
  }

  private static int showHanLine(final String codePointString, int rank, final double proportion,
          final String definition) {
    log.println(codePointString + "\t" + rank++ + "\t" + percent6.format(proportion) + "\t" + definition);
    return rank;
  }

  private static String myFold(String chars) {
    if (whitespace.containsAll(chars)) {
      return " ";
    }
    return UCharacter.foldCase(chars, true);
  }

  public static UnicodeSet dontshow = new UnicodeSet("[[:whitespace:][:di:][:cn:]]");
  public static UnicodeSet whitespace = new UnicodeSet("[[:whitespace:]]");

  private static String codePointValueOf(int codePoint) {
    //    if (dontshow.contains(codePoint)) {
    //      return "-";
    //    }
    return new StringBuilder().appendCodePoint(codePoint).toString();
  }

  private static String toHex(int codePoint) {
    String result = Integer.toHexString(codePoint).toUpperCase(Locale.ENGLISH);
    if (result.length() > 3) {
      return result;
    }
    return "0000".substring(result.length()) + result;
  }



  private static void showScripts(int script, final long postScriptCount, final long preTotal) {
    log.println(getScriptName(script, true) 
            //+ "\t" + integer.format(scriptsPre.getCount(script))
            //+ "\t" + percent.format((scriptsPre.getCount(script)+0.0)/scriptsPre.getTotal())
            + "\t" + integer.format(postScriptCount)
            + "\t" + percent.format((postScriptCount+0.0)/preTotal)              
    );
  }

  private static String toHex(String s) {
    StringBuilder result = new StringBuilder();
    int cp;
    for (int i = 0; i < s.length(); i += Character.charCount(cp)) {
      cp = s.codePointAt(i);
      if (i != 0) {
        result.append(",");
      }
      result.append(toHex(cp));
    }
    return result.toString();
  }

  private static int getScript(String s) {
    return getScript(s.codePointAt(0));
  }

  private static int getScript(int codePoint) {
    int script = UScript.getScript(codePoint);
    if (script == UScript.KATAKANA) {
      script = UScript.HIRAGANA;
    }
    return codePoint >= 0x80 ? script
            : script == UScript.LATIN ? -2
                    : -5;
  }

  private static String getScriptName(int script, boolean useExamples) {
    StringBuilder charList = new StringBuilder();

    Counter<Integer> counts = scriptToChars.get(script);
    if (useExamples && counts != null) {
      //charList.append("[");
      final Set<Integer> keysetSortedByCount = counts.getKeysetSortedByCount(false);
      Iterator<Integer> it = keysetSortedByCount.iterator();
      for (int i = 0; i < 5 && it.hasNext(); ++i) {
        if (i != 0) {
          charList.append(' ');
        }
        int cp = it.next();
        appendChar(charList, cp);
      }
      charList.append(" …");
      return charList.toString();
      //.append(   		"] ");
    }
    charList.append(script == -5 ? "ASCII Punct/Sym" 
            : script == -4 ? "Other" 
                    : script == -3 ? "Markup" 
                            : script == -2 ? "ASCII Latin"
                                    : script == UScript.LATIN ? "Other Latin" 
                                            : script == UScript.HIRAGANA ? "Kana" 
                                                    : script == UScript.COMMON ? "Other Punct/Sym"
                                                            : UScript.getName(script));
    return charList.toString();
  }

  private static StringBuilder appendChar(StringBuilder charList, int cp) {
    switch(cp) {
      case 0x20: return charList.append("␠");
      case 0x9: return charList.append("␉");
      case 0xA: return charList.append("␊");
      case 0xD: return charList.append("␍");
      case 0xA0: return charList.append("ɴʙ␠");
      case 0x3000: return charList.append("ɪᴅ␠");
    }
    int type = Character.getType(cp);
    if (type == Character.SPACE_SEPARATOR || type == Character.CONTROL) {
      return charList.append(toHex(cp).toUpperCase())
      .append("₁₆ ")
      .append(UCharacter.getExtendedName(cp));
    } else {
      return charList.appendCodePoint(cp);
    }
  }

}
