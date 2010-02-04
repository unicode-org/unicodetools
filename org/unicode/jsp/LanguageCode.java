package org.unicode.jsp;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Collator;
import com.ibm.icu.util.ULocale;

public class LanguageCode { 

  static public final Pattern languageID = Pattern.compile(
          "      (?: ( [a-z A-Z]{2,8} | [a-z A-Z]{2,3} [-_] [a-z A-Z]{3} )"
          + "      (?: [-_] ( [a-z A-Z]{4} ) )? "
          + "      (?: [-_] ( [a-z A-Z]{2} | [0-9]{3} ) )?"
          + "      (?: [-_] ( (?: [0-9 a-z A-Z]{5,8} | [0-9] [0-9 a-z A-Z]{3} ) (?: [-_] (?: [0-9 a-z A-Z]{5,8} | [0-9] [0-9 a-z A-Z]{3} ) )* ) )?"
          + "      (?: [-_] ( [a-w y-z A-W Y-Z] (?: [-_] [0-9 a-z A-Z]{2,8} )+ (?: [-_] [a-w y-z A-W Y-Z] (?: [-_] [0-9 a-z A-Z]{2,8} )+ )* ) )?"
          + "      (?: [-_] ( [xX] (?: [-_] [0-9 a-z A-Z]{1,8} )+ ) )? ) "
          + "    | ( [xX] (?: [-_] [0-9 a-z A-Z]{1,8} )+ )",
          Pattern.COMMENTS);

  static final Pattern extensionID = Pattern.compile("[a-w y-z A-W Y-Z]([-_][0-9 a-z A-Z]{2,8})*");
  static final Collection<String> QUALITY_EXCLUSIONS = new HashSet<String>(Arrays.asList("ti fo so kok ps cy sw ur pa pa_Guru uz_Latn ii haw az_Cyrl bo as zu ha ha_Latn uz_Arab om pa_Arab kw kl kk kk_Cyrl gv si uz uz_Cyrl"
          .split("\\s+")));

  enum Subtag {language, script, region, 
    variants,
    extensions, privateUse, privateUse2;
  String get(Matcher m) {
    return m.group(ordinal()+1);
  }
  }

  static class MyHandler extends FileUtilities.SemiFileReader {
    TreeMap<String,String> map = new TreeMap<String,String>();
    protected boolean isCodePoint() {
      return false;
    }
    public void handleLine(int start, int end, String[] items) {
      map.put(items[0], items[1]);
    }
  }

  static final Map<String,String> names = ((MyHandler) new MyHandler().process(LanguageCode.class, "subtagNames.txt")).map;
  static final Map<String,String> toAlpha3 = ((MyHandler) new MyHandler().process(LanguageCode.class, "alpha2_3.txt")).map;
  static final Map<String,String> fixCodes = ((MyHandler)new MyHandler().process(LanguageCode.class, "fixCodes.txt")).map;

  public static String validate(String input, ULocale ulocale) {
    String oldInput = input;
    StringBuilder canonical = new StringBuilder();
    String prefix = "";

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
          prefix = "<p><i><b>Ill-Formed Language Identifier: </b></i>" + input.substring(0, posBefore)
          + "<span class='x'>" + input.substring(posBefore, i) 
          + "×" 
          + input.substring(i, posAfter)
          + "</span>" + input.substring(posAfter, input.length())
          + "<br><i>Couldn't parse past the point marked with <span class='x'>×</span>.</i></p>\n";
          if (posBefore <= 0) {
            return prefix;
          }
          input = input.substring(0, posBefore-1);
          m.reset(input);
          if (!m.matches()) {
            return prefix;
          }
          break;
        }
      }
    }
    StringBuilder builder = new StringBuilder().append("<table>\n").append(getLine("th", "Type", "2.1", "Code", "Name", "Replacement?"));

    String languageCode = Subtag.language.get(m);
    if (languageCode != null) {
      String languageAndLink = languageCode = languageCode.toLowerCase(Locale.ENGLISH);
      String originalCode = languageCode;
      String fixed;
      String languageName;
      String[] parts = languageCode.split("[-_]");
      if (parts.length == 1) {
        boolean invalidLanguageCode = !names.containsKey(languageCode);
        if (invalidLanguageCode) {
          languageName = "<i>invalid code</i>";
        } else {
          languageName = getSubtagName(languageCode, ulocale, true);
          if (languageName.startsWith("@")) {
            languageName = languageName.substring(1);
          }
          languageAndLink = getCodeAndLink(Subtag.language, languageCode, ulocale);
        }
        fixed = fixCodes.get(languageCode);
      } else { // must be 2
        // cases are the following. For the replacement, we use fix(extlang) if valid, otherwise fix(lang) if valid, otherwise fix(extlang) 
        // zh-cmn - valid => cmn
        // en-cmn - valid => cmn // but shouldn't be; by canonicalization en-cmn = cmn
        // eng-cmn - invalid => cmn
        // xxx-cmn - invalid => cmn
        // zh-xxx - invalid => zh
        // xxx-eng - invalid => en
        // xxx-yyy - invalid => null
        // That is, pick the second unless it is invald
        languageCode = parts[0];
        String extlang = parts[1];
        String extLangName = names.get(extlang);
        boolean invalidLanguageCode = !names.containsKey(languageCode);
        final boolean invalidExtlang = extLangName == null || !extLangName.startsWith("@");
        if (invalidExtlang & invalidLanguageCode) {
          if (extLangName == null) {
            languageName = "<i>invalid base and extlang codes</i>";
          } else {
            languageName = "<i>invalid base and extlang code - extlang would be valid base-lang code</i>";
          }
        } else if (invalidExtlang) {
          if (extLangName == null) {
            languageName = "<i>invalid extlang code</i>";
          } else {
            languageName = "<i>invalid extlang code - would be valid base-lang code</i>";
          }
        } else if (invalidLanguageCode) {
          languageName = "<i>invalid base-lang code</i>";
          languageCode = extlang;
        } else {
          languageName = getSubtagName(extlang, ulocale, true);
          if (languageName.startsWith("@")) {
            languageName = languageName.substring(1);
          }
          //languageAndLink = getLanguageAndLink(extlang);
          languageCode = extlang;
        }
        fixed = fixCodes.get(languageCode);
        languageAndLink = originalCode;
      }
      builder.append(getLine("td", "Language", "2.2.1", languageAndLink, languageName, getCodeAndLink(Subtag.language, fixed, ulocale)));
      addFixed(canonical, languageCode, fixed);
    }

    String script = Subtag.script.get(m);
    if (script != null) {
      String scriptCode = script = UCharacter.toTitleCase(Locale.ENGLISH, script, null);
      String scriptName;
      if (!names.containsKey(script)) {
        scriptName = "<i>invalid Code</i>";
      } else {
        scriptName = getSubtagName(script, ulocale, true);
        script = getCodeAndLink(Subtag.script, script, ulocale);
      }
      final String fixed = fixCodes.get(scriptCode);
      builder.append(getLine("td", "Script", "2.2.3", script, scriptName, getCodeAndLink(Subtag.script, fixed, ulocale)));
      addFixed(canonical, scriptCode, fixed);
    }

    String region = Subtag.region.get(m);
    if (region != null) {
      String regionCode = region = region.toUpperCase(Locale.ENGLISH);
      String regionName;
      if (!names.containsKey(region)) {
        regionName = "<i>invalid Code</i>";
      } else {
        regionName = getSubtagName(region, ulocale, true);
        region = getCodeAndLink(Subtag.region, region, ulocale);
      }
      final String fixed = fixCodes.get(regionCode);
      builder.append(getLine("td", "Region", "2.2.4", region, regionName, getCodeAndLink(Subtag.region, fixed, ulocale)));
      addFixed(canonical, regionCode, fixed);
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
          variantName = getSubtagName(variant, ulocale, true);
          variant = "<a href='http://tools.ietf.org/html/draft-ietf-ltru-4645bis' target='iso'>" + variant + "</a>";
        }
        final String fixed = fixCodes.get(variantCode);
        builder.append(getLine("td", "Variant", "2.2.5", variant, variantName, fixed));
        addFixed(canonical, variantCode, fixed);
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
        builder.append(getLine("td", "Extension", "2.2.6", extension, "", null));
        addFixed(canonical, extension, null);
      }
    }

    String privateUse = Subtag.privateUse.get(m);
    if (privateUse == null) {
      privateUse = Subtag.privateUse2.get(m);
    }
    if (privateUse != null) {
      privateUse = privateUse.toLowerCase(Locale.ENGLISH);
      builder.append(getLine("td", "Private-Use", "2.2.7", privateUse, "", null));
      addFixed(canonical, privateUse, null);
    }
    builder.append("</table>\n");
    String canonicalString = canonical.toString();
    if (!canonicalString.equals(oldInput)) {
      builder.insert(0, "<p>Suggested Canonical Form: <b><a href='languageid.jsp?a=" + canonical + "' target='languageid'>" + canonical + "</b></p>\n");
    }
    builder.insert(0, prefix);
    return builder.toString();
  }

  private static void addFixed(StringBuilder canonical, String code, String fixed) {
    if (fixed == null) {
      fixed = code;
    }
    if (fixed.startsWith("?")) {
      return;
    }
    int spacePos = fixed.indexOf(' ');
    if (spacePos >= 0) {
      fixed = fixed.substring(0, spacePos);
    }
    if (canonical.length() != 0) {
      canonical.append('-');
    }
    canonical.append(fixed);
  }

  private static String getCodeAndLink(Subtag subtag, String codes, ULocale ulocale) {
    if (codes == null) {
      return codes;
    }
    StringBuilder buffer = new StringBuilder();
    for (String code : codes.split("\\s+")) {
      String value = getCodeAndLink2(subtag, code, ulocale);
      if (buffer.length() != 0) {
        buffer.append(" ");
      }
      buffer.append(value);
    }
    return buffer.toString();
  }

  private static String getCodeAndLink2(Subtag subtag, String code, ULocale ulocale) {
    String name = getSubtagName(code, ulocale, false);
    if (name != null) {
      name = " title='" + name + "'";
    } else {
      name = "";
    }
    switch (subtag) {
    case region: {
      if (code.compareTo("A") < 0) {
        code = "<a href='http://unstats.un.org/unsd/methods/m49/m49regin.htm' target='iso'" + name + ">" + code + "</a>";
      } else {
        code = "<a href='http://www.iso.org/iso/country_codes/iso_3166_code_lists/english_country_names_and_code_elements.htm' target='iso'" + name + ">" + code + "</a>";
      }
      return code;
    }
    case script: {
      code = "<a href='http://unicode.org/iso15924/iso15924-en.html' target='iso'" + name + ">" + code + "</a>";
      return code;
    }
    case language: {
      String alpha3 = code;
      if (code.length() == 2) {
        alpha3 = toAlpha3.get(code);
        if (alpha3 == null) {
          alpha3 = code;
        }
      }
      code = "<a href='http://www.sil.org/iso639-3/documentation.asp?id=" + alpha3 + "' target='iso'" + name + ">" + code + "</a>";
      return code;
    }
    default: throw new IllegalArgumentException();
    }
  }

  private static String getSubtagName(String code, ULocale ulocale, boolean html) {
    String name = getIcuName(code, ulocale);
    if (!name.equals(code)) {
      return name;
    }
    if (!ulocale.equals(ULocale.ENGLISH)) {
      name = getIcuName(code, ULocale.ENGLISH);
      if (!name.equals(code)) {
        name = name + "*";
        if (html) {
          name = "<i>" + name + "</i>";
        }
        return name;
      }
    }
    name = names.get(code);
    if (name != null) {
      if (name.startsWith("@")) {
        name = name.substring(1);
      }
      name = name + "**";
      if (html) {
        name = "<i>" + name + "</i>";
      }
      return name;
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

  private static String getLine(String element, String type, String specSection, String subtag, String name, String replacement) {
    if (name == null) {
      name = "<i>invalid</i>";
    }
    if (replacement != null) {
      replacement = "<" + element + ">" + replacement + "</" + element + ">";
    } else {
      replacement = "";
    }
    final String typeAndLink = specSection == null ? type : "<a href='http://tools.ietf.org/html/draft-ietf-ltru-4646bis#section-" + specSection + "' target='bcp47bis'>" + type + "</a>";
    return "<tr><" + element + ">" + typeAndLink + "</" + element + "><" + element + ">" + subtag + "</" + element
    + "><" + element + ">" + name + "</" + element + ">" + replacement + "</tr>\n";
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
      if (country.length() != 0) {
        continue;
      }
      if (QUALITY_EXCLUSIONS.contains(ulocale.toString())) {
        continue;
      }

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

  public static String getLocaleName(ULocale toBeLocalized, ULocale toLocalizeInto) {
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
