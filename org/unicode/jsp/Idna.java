package org.unicode.jsp;

import java.util.regex.Pattern;

import org.unicode.cldr.util.Predicate;
import org.unicode.jsp.UnicodeSetUtilities.TableStyle;

import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.StringPrepParseException;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.StringTransform;

public class Idna implements StringTransform {

  public enum IdnaType {
    valid, ignored, mapped, deviation, disallowed;
  }

  protected UnicodeMap<IdnaType> types = new UnicodeMap<IdnaType>();
  protected UnicodeMap<String> mappings = new UnicodeMap<String>();
  protected UnicodeMap<String> mappings_display = new UnicodeMap<String>();
  protected UnicodeSet validSet = new UnicodeSet();
  private final String name;
  static final UnicodeSet ASCII = UnicodeSetUtilities.parseUnicodeSet("[:ASCII:]", TableStyle.simple);
  static Pattern DOT = Pattern.compile("[.]");
  static UnicodeSet OTHER_DOT_SET = new UnicodeSet("[． 。｡]");
  static Pattern DOTS = Pattern.compile("[.． 。｡]");

  protected Idna() {
    String[] names = this.getClass().getName().split("[.]");
    name = names[names.length-1];
  }

  public IdnaType getType(int i) {
    return types.get(i);
  }
  
  public String getName() {
    return name;
  }

  public String transform(String source) {
    return transform(source, false);
  }

  public String transform(String source, boolean display) {
    String remapped = (display ? mappings_display : mappings).transform(source);
    return Normalizer.normalize(remapped, Normalizer.NFC);
  }

  public String toUnicode(String source, boolean[] error, boolean display) {
    error[0] = false;
    String remapped = transform(source, display);
    StringBuilder result = new StringBuilder();
    for (String label : remapped.split("[.]")) {
      if (result.length() != 0) {
        result.append('.');
      }
      String fixedLabel = fromPunycodeOrUnicode(label, error);
      result.append(fixedLabel);
    }
    return result.toString();
  }

  private String fromPunycodeOrUnicode(String label, boolean[] error) {
    if (!label.startsWith("xn--")) {
      return label;
    }
    try {
      StringBuffer temp = new StringBuffer();
      temp.append(label.substring(4));
      StringBuffer depuny = Punycode.decode(temp, null);
      return depuny.toString();
    } catch (StringPrepParseException e) {
      error[0] = true;
      return label;
    }
  }
  
  public String toPunyCode(String source, boolean[] error) {
    String clean = toUnicode(source, error, false);
    StringBuilder result = new StringBuilder();
    for (String label : clean.split("[.]")) {
      if (result.length() != 0) {
        result.append('.');
      }
      try {
        StringBuffer temp = new StringBuffer();
        temp.append(label);
        StringBuffer depuny = Punycode.encode(temp, null);
        result.append("xn--").append(depuny);
      } catch (StringPrepParseException e) {
        error[0] = true;
        result.append(label);
      }
    }
    return result.toString();
  }

  public boolean isValid(String string) {
    String trans = transform(string);
    return Normalizer.isNormalized(trans, Normalizer.NFC, 0) && validSet.containsAll(trans);
  }

  public static String testIdnaLines(String lines, String filter) {
      Transliterator hex = Transliterator.getInstance("any-hex");
      try {
  
        lines = UnicodeJsp.UNESCAPER.transform(lines);
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
            UnicodeUtilities.addBlank(resultLines);
          }
  
          String rawPunycode = processLabels(line, Idna.DOTS, true, new Predicate() {
            public boolean is(Object item) {
              return true;
            }});
  
          String idna2003puny = Idna2003.SINGLETON.toPunyCode(line, errorOut);
          String idna2003unic = Idna2003.SINGLETON.toUnicode(line, errorOut, true);
  
          String uts46puny = Uts46.SINGLETON.toPunyCode(line, errorOut);
          String uts46unic = Uts46.SINGLETON.toUnicode(line, errorOut, true);
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
  
          String idna2008puny = processLabels(line, Idna.DOT, true, verifier2008);
          String idna2008unic = processLabels(line, Idna.DOT, false, verifier2008);
  
          // first lines
          resultLines.append("<tr>");
          resultLines.append("<th>Display</th>");
          UnicodeUtilities.addCell(resultLines, hex, line, "class='cn ltgreen'");
          UnicodeUtilities.addCell(resultLines, hex, idna2003unic, "class='cn i2003'");
          UnicodeUtilities.addCell(resultLines, hex, uts46unic, "class='cn i46'");
          UnicodeUtilities.addCell(resultLines, hex, idna2008unic, "class='cn i2008'");
          resultLines.append("<tr></tr>");
  
          resultLines.append("<th class='mono'>Punycode</th>");
          UnicodeUtilities.addCell(resultLines, hex, rawPunycode, "class='cn ltgreen mono'");
          UnicodeUtilities.addCell(resultLines, hex, idna2003puny, "class='cn mono i2003'");
          UnicodeUtilities.addCell(resultLines, hex, uts46puny, "class='cn mono i46'");
          UnicodeUtilities.addCell(resultLines, hex, idna2008puny, "class='cn mono i2008'");
  
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
        if (!punycode || Idna.ASCII.containsAll(label)) {
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

