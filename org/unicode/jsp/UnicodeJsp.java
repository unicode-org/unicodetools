package org.unicode.jsp;

import java.io.IOException;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.IdnaLabelTester;
import org.unicode.cldr.util.Predicate;
import org.unicode.jsp.UnicodeSetUtilities.TableStyle;

import com.ibm.icu.dev.test.util.BNF;
import com.ibm.icu.dev.test.util.Quoter;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.RuleBasedBreakIterator;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class UnicodeJsp {
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

  static String defaultIdnaInput = "\u0001.com"
    +"\nöbb.at ÖBB.at ÖBB.at"
    +"\nȡog.de ☕.de I♥NY.de"
    +"\nfass.de faß.de fäß.de Schäffer.de"
    +"\nＡＢＣ・日本.co.jp 日本｡co｡jp 日本｡co．jp 日本⒈co．jp"
    +"\nx\\u0327\\u0301.de x\\u0301\\u0327.de"
    +"\nσόλος.gr Σόλος.gr ΣΌΛΟΣ.gr"
    +"\nﻋﺮﺑﻲ.de عربي.de نامهای.de نامه\\u200Cای.de";

  public static String getDefaultIdnaInput() {
    return defaultIdnaInput;
  }

  public static String testIdnaLines(String lines, String filter) {
    Transliterator hex = Transliterator.getInstance("any-hex");
    try {

      lines = IdnaLabelTester.UNESCAPER.transform(lines);
      StringBuilder resultLines = new StringBuilder();
      UnicodeUtilities.getIdna2008Tester();

      Predicate<String> verifier2008 = new Predicate<String>() {
        public boolean is(String item) {
          return Normalizer.isNormalized(item, Normalizer.NFC, 0) && UnicodeUtilities.tester.test(item) == null;
        }
      };

      resultLines.append("<table>\n");
      resultLines.append("<th></th><th class='cn'>Input</th><th class='cn'>IDNA2003</th><th class='cn'>UTS46</th><th class='cn'>IDNA2008</th>\n");

      boolean first = true;
      for (String line : lines.split("\\s+")) {
        if (first) {
          first = false;
        } else {
          UnicodeUtilities.addBlank(resultLines);
        }

        String rawPunycode = UnicodeUtilities.processLabels(line, UnicodeUtilities.DOTS, true, new Predicate() {
          public boolean is(Object item) {
            return true;
          }});
        R2<String, String> idna2003Pair = Idna2003.getIdna2033(line);
        String idna2003 = idna2003Pair.get0();
        String idna2003back = idna2003Pair.get1();


        String tr46back = Uts46.toUts46(line);
        String tr46 = UnicodeUtilities.processLabels(tr46back, UnicodeUtilities.DOTS, true, new Predicate<String>() {
          public boolean is(String item) {
            return Uts46.Uts46Chars.containsAll(item);
          }
        });
        String tr46display = Uts46.foldDisplay.transform(line);
        tr46display = UnicodeUtilities.processLabels(tr46display, UnicodeUtilities.DOTS, false, new Predicate<String>() {
          public boolean is(String item) {
            return Uts46.Uts46CharsDisplay.containsAll(item);
          }
        });

        String idna2008 = UnicodeUtilities.processLabels(line, UnicodeUtilities.DOT, true, verifier2008);
        String idna2008back = UnicodeUtilities.processLabels(line, UnicodeUtilities.DOT, false, verifier2008);

        // first lines
        resultLines.append("<tr>");
        resultLines.append("<th>Display</th>");
        UnicodeUtilities.addCell(resultLines, hex, line, "class='cn ltgreen'");
        UnicodeUtilities.addCell(resultLines, hex, idna2003back, "class='cn i2003'");
        UnicodeUtilities.addCell(resultLines, hex, tr46display, "class='cn i46'");
        UnicodeUtilities.addCell(resultLines, hex, idna2008back, "class='cn i2008'");
        resultLines.append("<tr></tr>");

        resultLines.append("<th class='mono'>Punycode</th>");
        UnicodeUtilities.addCell(resultLines, hex, rawPunycode, "class='cn ltgreen mono'");
        UnicodeUtilities.addCell(resultLines, hex, idna2003, "class='cn mono i2003'");
        UnicodeUtilities.addCell(resultLines, hex, tr46, "class='cn mono i46'");
        UnicodeUtilities.addCell(resultLines, hex, idna2008, "class='cn mono i2008'");

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
      return UnicodeUtilities.toHTML.transform(e.getMessage());
    }
  }

  public static String getLanguageOptions(String locale) {
    return LanguageCode.getLanguageOptions(new ULocale(locale));
  }

  public static String getSimpleSet(String setA, UnicodeSet a, boolean abbreviate, boolean escape) {
    String a_out;
    a.clear();
    try {
      //setA = UnicodeSetUtilities.MyNormalize(setA, Normalizer.NFC);
      setA = setA.replace("..U+", "-\\u");
      setA = setA.replace("U+", "\\u");
      a.addAll(UnicodeSetUtilities.parseUnicodeSet(setA, TableStyle.extras));
      a_out = UnicodeUtilities.getPrettySet(a, abbreviate, escape);
    } catch (Exception e) {
      a_out = e.getMessage();
    }
    return a_out;
  }

  public static void showSet(String grouping, UnicodeSet a, boolean abbreviate, boolean ucdFormat, Appendable out) throws IOException {
    UnicodeUtilities.showSet(grouping, a, abbreviate, ucdFormat, out);
  }
  public static void showPropsTable(Appendable out) throws IOException {
    UnicodeUtilities.showPropsTable(out);
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
        if (text.length() > 2) {
            try {
            text = UTF16.valueOf(Integer.parseInt(text,16));
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
}
