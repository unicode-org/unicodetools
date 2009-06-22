package jsp;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Collator;
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

  static final Map<String,String> names = fillMapFromSemi(LanguageCode.class, "subtagNames.txt", new TreeMap<String,String>());
  static final Map<String,String> toAlpha3 = fillMapFromSemi(LanguageCode.class, "alpha2_3.txt", new TreeMap<String,String>());
  static final Map<String,String> fixCodes = fillMapFromSemi(LanguageCode.class, "fixCodes.txt", new TreeMap<String,String>());

  private static Map<String, String> fillMapFromSemi(Class<LanguageCode> classLocation, String fileName, Map<String, String> map) {
    BufferedReader in;
    String line = null;
    try {
      in = UnicodeUtilities.openFile(classLocation, fileName);
      for (int lineCount = 1; ; ++lineCount) {
        line = in.readLine();
        if (line == null) break;
        String[] parts = line.split(";");
        map.put(parts[0], parts[1]);
      }
      in.close();
      return map;
    } catch (Exception e) {
      throw (RuntimeException) new IllegalArgumentException(line).initCause(e);
    }
  }

  public static String validate(String input, ULocale ulocale) {
    input = input.trim();
    input = input.replace("_", "-");
    Matcher m = languageID.matcher(input);
    if (!m.matches()) {
      int i = input.length();
      for (; ; --i) {
        final String fragment = input.substring(0,i);
        m.reset(fragment).matches();
        if(i == 0 || m.hitEnd()) {
          int posBefore = input.lastIndexOf('-', i-1) + 1;
          int posAfter = input.indexOf('-', i);
          if (posAfter < 0) {
            posAfter = input.length();
          }
          return "<i><b>Invalid ID: </b></i>" + input.substring(0, posBefore)
          + "<span class='x'>" + input.substring(posBefore, i) 
          + "×" 
          + input.substring(i, posAfter)
          + "</span>" + input.substring(posAfter, input.length())
          + "<br><i>Couldn't parse past the point marked with <span class='x'>×</span>.</i>";
        }
      }
    }
    StringBuilder builder = new StringBuilder().append("<table>\n").append(getLine("th", "Type", "", "Code", "Name", "Replacement?"));

    String language = Subtag.language.get(m);
    if (language != null) {
      String languageCode = language = language.toLowerCase(Locale.ENGLISH);
      String languageName;
      if (!names.containsKey(language)) {
        languageName = "<i>invalid Code</i>";
      } else {
        languageName = getSubtagName(language, ulocale);
        language = getLanguageAndLink(language);
      }
      builder.append(getLine("td", "Language", languageCode, language, languageName, getLanguageAndLink(fixCodes.get(languageCode))));
    }

    String script = Subtag.script.get(m);
    if (script != null) {
      String scriptCode = script = UCharacter.toTitleCase(Locale.ENGLISH, script, null);
      String scriptName;
      if (!names.containsKey(script)) {
        scriptName = "<i>invalid Code</i>";
      } else {
        scriptName = getSubtagName(script, ulocale);
        script = getScriptAndLink(script);
      }
      builder.append(getLine("td", "Script", scriptCode, script, scriptName, getScriptAndLink(fixCodes.get(scriptCode))));
    }

    String region = Subtag.region.get(m);
    if (region != null) {
      String regionCode = region = region.toUpperCase(Locale.ENGLISH);
      String regionName;
      if (!names.containsKey(region)) {
        regionName = "<i>invalid Code</i>";
      } else {
        regionName = getSubtagName(region, ulocale);
        region = getRegionAndLink(region);
      }
      builder.append(getLine("td", "Region", regionCode, region, regionName, getRegionAndLink(fixCodes.get(regionCode))));
    }

    String variantList = Subtag.variants.get(m);
    if (variantList != null) {
      variantList = variantList.toLowerCase(Locale.ENGLISH);
      Set<String> variants = new TreeSet<String>(Arrays.asList(variantList.split("[-_]")));
      for (String variant : variants) {
        String variantCode = variant;
        String variantName;
        if (!names.containsKey(variant)) {
          variantName = "<i>invalid Code</i>";
        } else {
          variantName = getSubtagName(variant, ulocale);
          variant = "<a href='http://tools.ietf.org/html/draft-ietf-ltru-4645bis' target='iso'>" + variant + "</a>";
        }
        builder.append(getLine("td", "Variant", variantCode, variant, variantName, fixCodes.get(variantCode)));
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
        builder.append(getLine("td", "Extension", "", extension, "", null));
      }
    }

    String privateUse = Subtag.privateUse.get(m);
    if (privateUse == null) {
      privateUse = Subtag.privateUse2.get(m);
    }
    if (privateUse != null) {
      privateUse = privateUse.toLowerCase(Locale.ENGLISH);
      builder.append(getLine("td", "Private-Use", "", privateUse, "", null));
    }

    return builder.append("</table>").toString();
  }

  private static String getRegionAndLink(String region) {
    if (region == null) return region;
    if (region.compareTo("A") < 0) {
      region = "<a href='http://unstats.un.org/unsd/methods/m49/m49regin.htm' target='iso'>" + region + "</a>";
    } else {
      region = "<a href='http://www.iso.org/iso/country_codes/iso_3166_code_lists/english_country_names_and_code_elements.htm' target='iso'>" + region + "</a>";
    }
    return region;
  }

  private static String getScriptAndLink(String script) {
    if (script == null) return script;
    script = "<a href='http://unicode.org/iso15924/iso15924-en.html' target='iso'>" + script + "</a>";
    return script;
  }

  private static String getLanguageAndLink(String language) {
    if (language == null) return language;
    String alpha3 = language;
    if (language.length() == 2) {
      alpha3 = toAlpha3.get(language);
      if (alpha3 == null) {
        alpha3 = language;
      }
    }
    language = "<a href='http://www.sil.org/iso639-3/documentation.asp?id=" + alpha3 + "' target='iso'>" + language + "</a>";
    return language;
  }

  private static String getSubtagName(String code, ULocale ulocale) {
    String name = getIcuName(code, ulocale);
    if (!name.equals(code)) {
      return name;
    }
    if (!ulocale.equals(ULocale.ENGLISH)) {
      name = getIcuName(code, ULocale.ENGLISH);
      if (!name.equals(code)) {
        return name + "*";
      }
    }
    name = names.get(code);
    if (name != null) {
      return name + "**";
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
          icuName = ULocale.getDisplayScript("und-" + code, ulocale); 
          break;
        } // otherwise fall through!
      default: 
        icuName = ULocale.getDisplayVariant("und-Latn-AQ-" + code, ulocale).toLowerCase(); 
      break;
    }
    return icuName;
  }

  private static String getLine(String element, String type, String code, String subtag, String name, String replacement) {
    if (name == null) {
      name = "<i>invalid</i>";
    }
    if (replacement != null) {
      replacement = "<" + element + ">" + replacement + "</" + element + ">";
    } else {
      replacement = "";
    }
    return "<tr><" + element + ">" + type + "</" + element + "><" + element + ">" + subtag + "</" + element + "><" + element + ">" + name + "</" + element + ">" + replacement + "</tr>\n";
  }

  public static String getLanguageOptions(ULocale toLocalizeInto) {
    StringBuilder result = new StringBuilder();
    if (toLocalizeInto.getLanguage().equals("en")) {
      toLocalizeInto = ULocale.ENGLISH;
    }
    ULocale[] list = ULocale.getAvailableLocales();
    Map<String, String> sorted = new TreeMap<String, String>(Collator.getInstance(toLocalizeInto));
    for (ULocale ulocale : list) {
      String country = ulocale.getCountry();
      if (country.length() != 0) continue;
      String name = getLocaleName(ulocale, toLocalizeInto);
      sorted.put(name, ulocale.toString());
    }
    for (String name : sorted.keySet()) {
      String code = sorted.get(name).toString();
      String selected = code.equals(toLocalizeInto.toString()) ? " selected" : "";
      result.append("<option value='" + code + "'" + selected + ">" + name + "</option>\n");
    }
    return result.toString();
    /*
    <option value='en' <%= (choice.equals("en") ? "selected" : "")%>>English</option>
    <option value='de' <%= (choice.equals("de") ? "selected" : "")%>>German</option>
    <option <%= (choice.equals("fr") ? "selected" : "")%>>fr</option>
    <option <%= (choice.equals("it") ? "selected" : "")%>>it</option>
    <option <%= (choice.equals("es") ? "selected" : "")%>>es</option>
    <option value='gsw' <%= (choice.equals("gsw") ? "selected" : "")%>>Swiss German</option>
    <option <%= (choice.equals("pt") ? "selected" : "")%>>pt</option>
    <option <%= (choice.equals("zh") ? "selected" : "")%>>zh</option>
    <option <%= (choice.equals("ja") ? "selected" : "")%>>ja</option>
    <option <%= (choice.equals("hi") ? "selected" : "")%>>hi</option>
     */
  }

  private static String getLocaleName(ULocale toBeLocalized, ULocale toLocalizeInto) {
    String result = toBeLocalized.getDisplayName(toLocalizeInto);
    String test = toBeLocalized.getDisplayName(ULocale.ROOT);
    String englishName = toBeLocalized.getDisplayName(ULocale.ENGLISH);

    if (test.equals(result)) {
      result = englishName + "*";
    } else if (!result.equalsIgnoreCase(englishName)) {
      result += " / " + englishName;
    }

    return result;
  }
}
