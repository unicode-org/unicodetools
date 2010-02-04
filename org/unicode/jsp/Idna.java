package org.unicode.jsp;

import java.util.Arrays;
import java.util.regex.Pattern;

import org.unicode.jsp.UnicodeSetUtilities.TableStyle;

import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.StringPrepParseException;
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
  static final UnicodeSet LABEL_ASCII = new UnicodeSet("[\\-0-9a-zA-Z]").freeze();
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
      if (!error[0]) {
        remapped = transform(fixedLabel, display);
        if (!fixedLabel.equals(remapped) || remapped.contains(".")) {
          error[0] = true;
        }
      }
    }
    String resultString = result.toString();
    if (!error[0] && !validSet.containsAll(resultString)) {
      error[0] = true;
    }
    return resultString;
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
      if (LABEL_ASCII.containsAll(label)) {
        result.append(label);
      } else {
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
    }
    return result.toString();
  }

  public boolean isValid(String string) {
    String trans = transform(string);
    return Normalizer.isNormalized(trans, Normalizer.NFC, 0) && validSet.containsAll(trans);
  }
}

