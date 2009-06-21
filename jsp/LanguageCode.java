package jsp;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.util.ULocale;

public class LanguageCode {

  static final Pattern languageID = Pattern.compile(
          "      (?: ( [a-z A-Z]{2,8} )"
          + "      (?: [-_] ( [a-z A-Z]{4} ) )? "
          + "      (?: [-_] ( [a-z A-Z]{2} | [0-9]{3} ) )?"
          + "      (?: [-_] ( (?: [0-9 a-z A-Z]{5,8} | [0-9] [0-9 a-z A-Z]{3} ) (?: [-_] (?: [0-9 a-z A-Z]{5,8} | [0-9] [0-9 a-z A-Z]{3} ) )* ) )?"
          + "      (?: [-_] ( [a-w y-z A-W Y-Z] (?: [-_] [0-9 a-z A-Z]{2,8} )+ (?: [-_] [a-w y-z A-W Y-Z] (?: [-_] [0-9 a-z A-Z]{2,8} )+ )* ) )?"
          + "      (?: [-_] ( [xX] (?: [-_] [0-9 a-z A-Z]{1,8} )+ ) )? ) "
          + "    | ( [xX] (?: [-_] [0-9 a-z A-Z]{1,8} )+ ) ", Pattern.COMMENTS);

  static final Pattern extensionID = Pattern.compile("[a-w y-z A-W Y-Z]([-_][0-9 a-z A-Z]{2,8})*");

  enum Subtag {language, script, region, 
    variants,
    extensions, privateUse, privateUse2;
  String get(Matcher m) {
    return m.group(ordinal()+1);
  }
  }

  static final Map<String,String> names = new TreeMap<String,String>();
  static {
    BufferedReader in;
    String line = null;
    try {
      in = UnicodeUtilities.openFile(LanguageCode.class, "subtagNames.txt");
      for (int lineCount = 1; ; ++lineCount) {
        line = in.readLine();
        if (line == null) break;
        String[] parts = line.split(";");
        names.put(parts[0], parts[1]);
      }
      in.close();
    } catch (Exception e) {
      throw (RuntimeException) new IllegalArgumentException(line).initCause(e);
    }
  }

  public static String validate(String input, ULocale ulocale) {
    Matcher m = languageID.matcher(input);
    if (!m.matches()) {
      return null;
    }
    StringBuilder builder = new StringBuilder().append("<table>\n").append(getLine("th", "Type", "Code", "Name"));

    String language = Subtag.language.get(m);
    if (language != null) {
      language = language.toLowerCase(Locale.ENGLISH);
      builder.append(getLine("td", "Language", language, getSubtagName(language, ulocale)));
    }

    String script = Subtag.script.get(m);
    if (script != null) {
      script = UCharacter.toTitleCase(Locale.ENGLISH, script, null);
      builder.append(getLine("td", "Script", script, getSubtagName(script, ulocale)));
    }

    String region = Subtag.region.get(m);
    if (region != null) {
      region = region.toUpperCase(Locale.ENGLISH);
      builder.append(getLine("td", "Region", region, getSubtagName(region, ulocale)));
    }

    String variantList = Subtag.variants.get(m);
    if (variantList != null) {
      variantList = variantList.toLowerCase(Locale.ENGLISH);
      Set<String> variants = new TreeSet<String>(Arrays.asList(variantList.split("[-_]")));
      for (String variant : variants) {
        builder.append(getLine("td", "Variant", variant, getSubtagName(variant, ulocale)));
      }
    }

    String extensionList = Subtag.extensions.get(m);
    if (extensionList != null) {
      extensionList = extensionList.toLowerCase(Locale.ENGLISH);
      Matcher m2 = extensionID.matcher(extensionList);
      Set<String> extensions = new TreeSet<String>();

      while (m2.find()) {
        String extension = m2.group();
        extensions.add(extension);
      }
      for (String extension : extensions) {
        builder.append(getLine("td", "Extension", extension, ""));
      }
    }

    String privateUse = Subtag.privateUse.get(m);
    if (privateUse == null) {
      privateUse = Subtag.privateUse2.get(m);
    }
    if (privateUse != null) {
      privateUse = privateUse.toLowerCase(Locale.ENGLISH);
      builder.append(getLine("td", "Private-Use", privateUse, ""));
    }

    return builder.append("</table>").toString();
  }

  private static String getSubtagName(String code, ULocale ulocale) {
    String name = getIcuName(code, ulocale);
    if (!name.equals(code)) {
      return name;
    }
    if (!ulocale.equals(ULocale.ENGLISH)) {
      name = getIcuName(code, ULocale.ENGLISH);
      if (!name.equals(code)) {
        return "*" + name;
      }
    }
    name = names.get(code);
    if (name != null) {
      return "**" + name;
    }
    return null;
  }

  private static String getIcuName(String code, ULocale ulocale) {
    String icuName = code;
    switch(code.length()) {
      case 2:
      case 3:
        icuName = code.compareTo("a") < 0 
        ? ULocale.getDisplayCountry("und-" + code, ulocale)
                : ULocale.getDisplayLanguage(code, ulocale);
        break;
      case 4: 
        if (code.compareTo("A") >= 0) {
          ULocale.getDisplayScript("und-" + code, ulocale); 
          break;
        } // otherwise fall through!
      default: 
        icuName = ULocale.getDisplayVariant("und-Latn-AQ-" + code, ulocale).toLowerCase(); 
      break;
    }
    return icuName;
  }

  private static String getLine(String element, String type, String subtag, String name) {
    if (name == null) {
      name = "<i>invalid</i>";
    }
    return "<tr><" + element + ">" + type + "</" + element + "><" + element + ">" + subtag + "</" + element + "><" + element + ">" + name + "</" + element + "></tr>\n";
  }

}
