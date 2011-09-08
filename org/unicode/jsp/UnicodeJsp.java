package org.unicode.jsp;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.idna.Idna2003;
import org.unicode.idna.Idna2008;
import org.unicode.idna.Uts46;
import org.unicode.jsp.UnicodeUtilities.CodePointShower;

import com.ibm.icu.dev.test.util.BNF;
import com.ibm.icu.dev.test.util.Quoter;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.RuleBasedBreakIterator;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class UnicodeJsp {

  public static NumberFormat nf = NumberFormat.getInstance(ULocale.ENGLISH);
  static {
    nf.setGroupingUsed(true);
    nf.setMaximumFractionDigits(0);
  }

  public static String showBidi(String str, int baseDirection, boolean asciiHack) {
    return UnicodeUtilities.showBidi(str, baseDirection, asciiHack);
  }

  public static String validateLanguageID(String input, String locale) {
    String result = LanguageCode.validate(input, new ULocale(locale));
    return result;
  }

  public static String showRegexFind(String regex, String test) {
    try {
      Matcher matcher = Pattern.compile(regex, Pattern.COMMENTS).matcher(test);
      String result = UnicodeUtilities.toHTML.transform(matcher.replaceAll("⇑⇑$0⇓⇓"));
      result = result.replaceAll("⇑⇑", "<u>").replaceAll("⇓⇓", "</u>");
      return result;
    } catch (Exception e) {
      return "Error: " + e.getMessage();
    }
  }

  /**
   * The regex doesn't have to have the UnicodeSets resolved.
   * @param regex
   * @param count
   * @param maxRepeat
   * @return
   */
  public static String getBnf(String regexSource, int count, int maxRepeat) {
    //String regex = new UnicodeRegex().compileBnf(rules);
    String regex = regexSource.replace("(?:", "(").replace("(?i)", "");

    BNF bnf = new BNF(new Random(), new Quoter.RuleQuoter());
    if (maxRepeat > 20) {
      maxRepeat = 20;
    }
    bnf.setMaxRepeat(maxRepeat)
    .addRules("$root=" + regex + ";")
    .complete();
    StringBuffer output = new StringBuffer();
    for (int i = 0; i < count; ++i) {
      String line = bnf.next();
      output.append("<p>").append(UnicodeUtilities.toHTML(line)).append("</p>");
    }
    return output.toString();
  }

  public static String showBreaks(String text, String choice) {

    RuleBasedBreakIterator b;
    if (choice.equals("Word")) b = (RuleBasedBreakIterator) BreakIterator.getWordInstance();
    else if (choice.equals("Line")) b = (RuleBasedBreakIterator) BreakIterator.getLineInstance();
    else if (choice.equals("Sentence")) b = (RuleBasedBreakIterator) BreakIterator.getSentenceInstance();
    else b = (RuleBasedBreakIterator) BreakIterator.getCharacterInstance();

    Matcher decimalEscapes = Pattern.compile("&#(x)?([0-9]+);").matcher(text);
    // quick hack, since hex-any doesn't do decimal escapes
    int start = 0;
    StringBuffer result2 = new StringBuffer();
    while (decimalEscapes.find(start)) {
      int radix = 10;
      int code = Integer.parseInt(decimalEscapes.group(2), radix);
      result2.append(text.substring(start,decimalEscapes.start()) + UTF16.valueOf(code));
      start = decimalEscapes.end();
    }
    result2.append(text.substring(start));
    text = result2.toString();

    int lastBreak = 0;
    StringBuffer result = new StringBuffer();
    b.setText(text);
    b.first();
    for (int nextBreak = b.next(); nextBreak != BreakIterator.DONE; nextBreak = b.next()) {
      int status = b.getRuleStatus();
      String piece = text.substring(lastBreak, nextBreak);
      //piece = toHTML.transliterate(piece);
      piece = UnicodeUtilities.toHTML(piece);

      piece = piece.replaceAll("&#xA;","<br>");
      result.append("<span class='break'>").append(piece).append("</span>");
      lastBreak = nextBreak;
    }

    return result.toString();  }

  public static void showProperties(int cp, Appendable out) throws IOException {
    UnicodeUtilities.showProperties(cp, out);
  }

  static String defaultIdnaInput = ""
    +"fass.de faß.de fäß.de xn--fa-hia.de"
    + "\n₹.com 𑀓.com"
    + "\n\u0080.com xn--a.com a\u200cb xn--ab-j1t"
    +"\nöbb.at ÖBB.at ÖBB.at"
    +"\nȡog.de ☕.de I♥NY.de"
    +"\nＡＢＣ・日本.co.jp 日本｡co｡jp 日本｡co．jp 日本⒈co．jp"
    +"\nx\\u0327\\u0301.de x\\u0301\\u0327.de"
    +"\nσόλος.gr Σόλος.gr ΣΌΛΟΣ.gr"
    +"\nﻋﺮﺑﻲ.de عربي.de نامهای.de نامه\\u200Cای.de".trim();

  public static String getDefaultIdnaInput() {
    return defaultIdnaInput;
  }
  public static final Transliterator UNESCAPER = Transliterator.getInstance("hex-any");
  
  public static String getLanguageOptions(String locale) {
    return LanguageCode.getLanguageOptions(new ULocale(locale));
  }

  public static String getTrace(Exception e) {
     return Arrays.asList(e.getStackTrace()).toString().replace("\n", "<\br>");
  }
  
  public static String getSimpleSet(String setA, UnicodeSet a, boolean abbreviate, boolean escape) {
    String a_out;
    a.clear();
    try {
      //setA = UnicodeSetUtilities.MyNormalize(setA, Normalizer.NFC);
      setA = setA.replace("..U+", "-\\u");
      setA = setA.replace("U+", "\\u");
      a.addAll(UnicodeSetUtilities.parseUnicodeSet(setA));
      a_out = UnicodeUtilities.getPrettySet(a, abbreviate, escape);
    } catch (Exception e) {
      a_out = e.getMessage();
    }
    return a_out;
  }

  public static void showSet(String grouping, UnicodeSet a, boolean abbreviate, boolean ucdFormat, Appendable out) throws IOException {
    CodePointShower codePointShower = new CodePointShower(abbreviate, ucdFormat, false);
    UnicodeUtilities.showSet(grouping, a, codePointShower, out);
  }
  public static void showPropsTable(Appendable out, String propForValues, String myLink) throws IOException {
    UnicodeUtilities.showPropsTable(out, propForValues, myLink);
  }
  public static String showTransform(String transform, String sample) {
    return UnicodeUtilities.showTransform(transform, sample);
  }

  public static String listTransforms() {
    return UnicodeUtilities.listTransforms();
  }

  public static void getDifferences(String setA, String setB,
          boolean abbreviate, String[] abResults, int[] abSizes, String[] abLinks) {
    UnicodeUtilities.getDifferences(setA, setB, abbreviate, abResults, abSizes, abLinks);
  }

  public static int parseCode(String text, String nextButton, String previousButton) {
    //text = fromHTML.transliterate(text);
    String trimmed = text.trim();
    if (trimmed.length() > 1) {
      try {
        text = UTF16.valueOf(Integer.parseInt(trimmed,16));
      } catch (Exception e) {}
    }
    int cp = UTF16.charAt(text, 0);
    if (nextButton != null) {
      cp += 1;
      if (cp > 0x10FFFF) {
        cp = 0;
      }
    } else if (previousButton != null) {
      cp -= 1;
      if (cp < 0) {
        cp = 0x10FFFF;
      }
    }
    return cp;
  }
  
  public static String getConfusables(String test, int choice) {
    try {

      Confusables confusables = new Confusables(test);
      switch (choice) {
      case 0: // none
        break;
      case 1: // IDNA2008
        confusables.setAllowedCharacters(Idna2003.SINGLETON.validSet_transitional);
        confusables.setNormalizationCheck(Normalizer.NFC);
        break;
      case 2: // IDNA2008
        confusables.setAllowedCharacters(Idna2008.SINGLETON.validSet_transitional);
        confusables.setNormalizationCheck(Normalizer.NFC);
        break;
      case 3: // UTS46/39
        confusables.setAllowedCharacters(new UnicodeSet(Uts46.SINGLETON.validSet_transitional).retainAll(XIDModifications.getAllowed()));
        confusables.setNormalizationCheck(Normalizer.NFC);
        confusables.setScriptCheck(Confusables.ScriptCheck.same);
        break;
      }      
      return getConfusablesCore(test, confusables);
    } catch (Exception e) {
      return returnStackTrace(e);
    }
  }

  private static String returnStackTrace(Exception e) {
    StringWriter s = new StringWriter();
    PrintWriter p = new PrintWriter(s);
    e.printStackTrace(p);
    String str = UnicodeUtilities.toHTML(s.toString());
    str = str.replace("\n", "<br>");
    return str;
  }


  public static String getConfusables(String test, boolean nfkcCheck, boolean scriptCheck, boolean idCheck, boolean xidCheck) {
    try {

      Confusables confusables = new Confusables(test);
      if (nfkcCheck) confusables.setNormalizationCheck(Normalizer.NFKC);
      if (scriptCheck) confusables.setScriptCheck(Confusables.ScriptCheck.same);
      if (idCheck) confusables.setAllowedCharacters(new UnicodeSet("[\\-[:L:][:M:][:N:]]"));
      if (xidCheck) confusables.setAllowedCharacters(XIDModifications.getAllowed());
      
      return getConfusablesCore(test, confusables);
    } catch (Exception e) {
      return returnStackTrace(e);
    }
  }

  private static String getConfusablesCore(String test, Confusables confusables) {
    test = test.replaceAll("[\r\n\t]", " ").trim();
    StringBuilder result = new StringBuilder();
    double maxSize = confusables.getMaxSize();
    List<Collection<String>> alternates = confusables.getAlternates();
    if (alternates.size() > 0) {
      int max = 0;
      for (Collection<String> items : alternates) {
        int size = items.size();
        if (size > max) {
          max = size;
        }
      }
      String topCell = "<td class='smc' align='center' width='" + (100/max) +
      "%'>";
      String underStart = " <span class='chb'>";
      String underEnd = "</span> ";
      UnicodeSet nsm = new UnicodeSet("[[:Mn:][:Me:]]");

      result.append("<table><caption style='text-align:left'><h3>Confusable Characters</h3></caption>\n");
      for (Collection<String> items : alternates) {
        result.append("<tr>");
        for (String item : items) {
          result.append(topCell);
          String htmlItem = UnicodeUtilities.toHTML(item);
          if (nsm.containsAll(item)) {
            htmlItem = "&nbsp;" + htmlItem + "&nbsp;";
          }
          result.append(underStart).append(htmlItem).append(underEnd);
          result.append("</td>");
        }
        for (int i = max - items.size(); i > 0; --i) {
          result.append("<td class='smb' rowSpan='3'>&nbsp;</td>");
        }
        result.append("</tr>\n");
        
        result.append("<tr>");
        for (String item : items) {
          result.append("<td class='smh' align='center'>");
          result.append(com.ibm.icu.impl.Utility.hex(item));
          result.append("</td>");
        }
        result.append("</tr>\n");

        result.append("<tr>");
        for (String item : items) {
          result.append("<td class='smn' align='center'>");
          result.append(UCharacter.getName(item, " + "));
          result.append("</td>");
        }
        result.append("</tr>\n");
      }
      result.append("</table>\n");
    }

    result.append("<p>Total raw values: " + nf.format(maxSize) + "</p>\n");
    if (maxSize > 1000000) {
      result.append( "<p><i>Too many raw items to process.<i></p>\n");
      return result.toString();
    }
    
    result.append("<h3>Confusable Results</h3>");   
    int count = 0;
    result.append("<div style='border: 1px solid blue'>");
    for (String item : confusables) {
      ++count;
      if (count > 1000) {
        continue;
      }
      if (count != 1) {
        result.append("\n");
      }
      result.append(UnicodeUtilities.toHTML(item));
    }
    if (count > 1000) {
      result.append(" ...\n");
    }
    result.append("</div>\n");
    result.append("<p>Total filtered values: " + nf.format(count) + "</p>\n");

    if (count > 1000) {
      result.append("<p><i>Too many filtered items to display; truncating to 1,000.<i></p>\n");
    }
    return result.toString();
  }
  public static String testIdnaLines(String lines, String filter) {
    return UnicodeUtilities.testIdnaLines(lines, filter);
  }
  
  public static String getIdentifier(String script) {
    return UnicodeUtilities.getIdentifier(script);
  }
}
