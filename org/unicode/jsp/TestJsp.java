package org.unicode.jsp;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.Counter;
import org.unicode.jsp.UnicodeSetUtilities.TableStyle;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.util.BNF;
import com.ibm.icu.dev.test.util.PrettyPrinter;
import com.ibm.icu.dev.test.util.Quoter;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UProperty.NameChoice;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.LocaleData;
import com.ibm.icu.util.ULocale;

public class TestJsp  extends TestFmwk {

  private static final String enSample = "a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x, y, z, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, Z";
  static final UnicodeSet U5_2 = new UnicodeSet().applyPropertyAlias("age", "5.2").freeze();
  static final UnicodeSet U5_1 = new UnicodeSet().applyPropertyAlias("age", "5.1").freeze();
  static UnicodeSet BREAKING_WHITESPACE = new UnicodeSet("[\\p{whitespace=true}-\\p{linebreak=glue}]").freeze();

  public static void main(String[] args) throws Exception {
    int cp = ' ';
    if (BREAKING_WHITESPACE.contains(cp)) {
      System.out.println("found");
    }
    System.out.println(BREAKING_WHITESPACE);
    new TestJsp().run(args);
  }

  static UnicodeSet IPA = new UnicodeSet("[a-zæçðøħŋœǀ-ǃɐ-ɨɪ-ɶ ɸ-ɻɽɾʀ-ʄʈ-ʒʔʕʘʙʛ-ʝʟʡʢ ʤʧʰ-ʲʴʷʼˈˌːˑ˞ˠˤ̀́̃̄̆̈ ̘̊̋̏-̜̚-̴̠̤̥̩̪̬̯̰̹-̽͜ ͡βθχ↑-↓↗↘]").freeze();
  static String IPA_SAMPLE = "a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x, y, z, æ, ç, ð, ø, ħ, ŋ, œ, ǀ, ǁ, ǂ, ǃ, ɐ, ɑ, ɒ, ɓ, ɔ, ɕ, ɖ, ɗ, ɘ, ə, ɚ, ɛ, ɜ, ɝ, ɞ, ɟ, ɠ, ɡ, ɢ, ɣ, ɤ, ɥ, ɦ, ɧ, ɨ, ɪ, ɫ, ɬ, ɭ, ɮ, ɯ, ɰ, ɱ, ɲ, ɳ, ɴ, ɵ, ɶ, ɸ, ɹ, ɺ, ɻ, ɽ, ɾ, ʀ, ʁ, ʂ, ʃ, ʄ, ʈ, ʉ, ʊ, ʋ, ʌ, ʍ, ʎ, ʏ, ʐ, ʑ, ʒ, ʔ, ʕ, ʘ, ʙ, ʛ, ʜ, ʝ, ʟ, ʡ, ʢ, ʤ, ʧ, ʰ, ʱ, ʲ, ʴ, ʷ, ʼ, ˈ, ˌ, ː, ˑ, ˞, ˠ, ˤ, ̀, ́, ̃, ̄, ̆, ̈, ̊, ̋, ̏, ̐, ̑, ̒, ̓, ̔, ̕, ̖, ̗, ̘, ̙, ̚, ̛, ̜, ̝, ̞, ̟, ̠, ̡, ̢, ̣, ̤, ̥, ̦, ̧, ̨, ̩, ̪, ̫, ̬, ̭, ̮, ̯, ̰, ̱, ̲, ̳, ̴, ̹, ̺, ̻, ̼, ̽, ͜, ͡, β, θ, χ, ↑, →, ↓, ↗, ↘";

  enum Subtag {language, script, region, mixed, fail}

  static PrettyPrinter pretty = new PrettyPrinter().setOrdering(Collator.getInstance(ULocale.ENGLISH));

  static String prettyTruncate(int max, UnicodeSet set) {
    String prettySet = pretty.format(set);
    if (prettySet.length() > max) {
      prettySet = prettySet.substring(0,max) + "...";
    }
    return prettySet;
  }

  public void TestPropertyFactory() {
    //showIcuEnums();
    checkProperties("[:ccc=/3/:]", "[\u0308]");
    checkProperties("[:age=3.2:]", "[\u0308]");
    checkProperties("[:alphabetic:]", "[a]");
    checkProperties("[:greek:]", "[\u0370]");
    checkProperties("[:mn:]", "[\u0308]");
    checkProperties("[:sc!=Latn:]", "[\u0308]");
    checkProperties("[:sc≠Latn:]", "[\u0308]");
    try {
      checkProperties("[:linebreak:]", "[\u0308]");
      throw new IllegalArgumentException("Exception expected.");
    } catch (Exception e) {
      if (!e.getMessage().contains("must be in")) {
        throw new IllegalArgumentException("Exception expected with 'illegal'", e);
      } else {
        logln(e.getMessage());
      }
    }

    try {
      checkProperties("[:alphabetic=foobar:]", "[\u0308]");
      throw new IllegalArgumentException("Exception expected.");
    } catch (Exception e) {
      if (!e.getMessage().contains("must be in")) {
        throw new IllegalArgumentException("Exception expected with 'illegal'", e);
      }
    }
    
    checkProperties("[:alphabetic=no:]", "[\u0308]");
    checkProperties("[:alphabetic=false:]", "[\u0308]");
    checkProperties("[:alphabetic=f:]", "[\u0308]");
    checkProperties("[:alphabetic=n:]", "[\u0308]");

    checkProperties("[:isNFC=no:]", "[\u0308]");

    checkProperties("\\p{idna2003=disallowed}", "[\\u0001]");
    showPropValues(XPropertyFactory.make().getProperty("idna"));
    showPropValues(XPropertyFactory.make().getProperty("uts46"));
    checkProperties("\\p{idna=valid}", "[\u0308]");
    checkProperties("\\p{uts46=valid}", "[\u0308]");
    checkProperties("\\p{idna2008=disallowed}", "[A]");
  }

  private void showIcuEnums() {
    for (int prop = UProperty.BINARY_START; prop < UProperty.BINARY_LIMIT; ++prop) {
      showEnumPropValues(prop);
    }
    for (int prop = UProperty.INT_START; prop < UProperty.INT_LIMIT; ++prop) {
      showEnumPropValues(prop);
    }
  }

  private void showEnumPropValues(int prop) {
    System.out.println("Property number:\t" + prop);
    for (int nameChoice = 0; ; ++nameChoice) {
      try {
        String propertyName = UCharacter.getPropertyName(prop, nameChoice);
        if (propertyName == null && nameChoice > NameChoice.LONG) {
          break;
        }
        System.out.println("\t" + nameChoice + "\t" + propertyName);
      } catch (Exception e) {
        break;
      }
    }
    for (int i = UCharacter.getIntPropertyMinValue(prop); i <= UCharacter.getIntPropertyMaxValue(prop); ++i) {
      System.out.println("\tProperty value number:\t" + i);
      for (int nameChoice = 0; ; ++nameChoice) {
        String propertyValueName;
        try {
          propertyValueName = UCharacter.getPropertyValueName(prop, i, nameChoice);
          if (propertyValueName == null && nameChoice > NameChoice.LONG) {
            break;
          }
          System.out.println("\t\t"+ nameChoice + "\t" + propertyValueName);
        } catch (Exception e) {
          break;
        }
      }
    }
  }

  private void showPropValues(UnicodeProperty idna2003) {
    System.out.println(idna2003.getName());
    for (Object value : idna2003.getAvailableValues()) {
      System.out.println(value);
      System.out.println("\t" + idna2003.getSet(value.toString()));
    }
  }

  public void checkLanguageLocalizations() {

    Set<String> languages = new TreeSet<String>();
    Set<String> scripts = new TreeSet<String>();
    Set<String> countries = new TreeSet<String>();
    for (ULocale displayLanguage : ULocale.getAvailableLocales()) {
      addIfNotEmpty(languages, displayLanguage.getLanguage());
      addIfNotEmpty(scripts, displayLanguage.getScript());
      addIfNotEmpty(countries, displayLanguage.getCountry());
    }
    Map<ULocale,Counter<Subtag>> canDisplay = new TreeMap<ULocale,Counter<Subtag>>(new Comparator<ULocale>() {
      public int compare(ULocale o1, ULocale o2) {
        return o1.toLanguageTag().compareTo(o2.toString());
      }
    });

    for (ULocale displayLanguage : ULocale.getAvailableLocales()) {
      if (displayLanguage.getCountry().length() != 0) {
        continue;
      }
      Counter<Subtag> counter = new Counter<Subtag>();
      canDisplay.put(displayLanguage, counter);

      final LocaleData localeData = LocaleData.getInstance(displayLanguage);
      final UnicodeSet exemplarSet = new UnicodeSet()
      .addAll(localeData.getExemplarSet(UnicodeSet.CASE, LocaleData.ES_STANDARD));
      final String language = displayLanguage.getLanguage();
      final String script = displayLanguage.getScript();
      if (language.equals("zh")) {
        if (script.equals("Hant")) {
          exemplarSet.removeAll(UnicodeSetUtilities.simpOnly);
        } else {
          exemplarSet.removeAll(UnicodeSetUtilities.tradOnly);
        }
      } else {
        exemplarSet.addAll(localeData.getExemplarSet(UnicodeSet.CASE, LocaleData.ES_AUXILIARY));
        if (language.equals("ja")) {
          exemplarSet.add('ー');
        }
      }
      final UnicodeSet okChars = (UnicodeSet) new UnicodeSet("[[:P:][:S:][:Cf:][:m:][:whitespace:]]").addAll(exemplarSet).freeze();

      Set<String> mixedSamples = new TreeSet<String>();

      for (String code : languages) {
        add(displayLanguage, Subtag.language, code, counter, okChars, mixedSamples);
      }
      for (String code : scripts) {
        add(displayLanguage, Subtag.script, code, counter, okChars, mixedSamples);
      }
      for (String code : countries) {
        add(displayLanguage, Subtag.region, code, counter, okChars, mixedSamples);
      }
      UnicodeSet missing = new UnicodeSet();
      for (String mixed : mixedSamples) {
        missing.addAll(mixed);
      }
      missing.removeAll(okChars);

      final long total = counter.getTotal() - counter.getCount(Subtag.mixed) - counter.getCount(Subtag.fail);
      final String missingDisplay = mixedSamples.size() == 0 ? "" : "\t" + missing.toPattern(false) + "\t" + mixedSamples;
      System.out.println(displayLanguage + "\t" + displayLanguage.getDisplayName(ULocale.ENGLISH)
              + "\t" + (total/(double)counter.getTotal())
              + "\t" + total
              + "\t" + counter.getCount(Subtag.language)
              + "\t" + counter.getCount(Subtag.script)
              + "\t" + counter.getCount(Subtag.region)
              + "\t" + counter.getCount(Subtag.mixed)
              + "\t" + counter.getCount(Subtag.fail)
              + missingDisplay
      );
    }
  }

  private void add(ULocale displayLanguage, Subtag subtag, String code, Counter<Subtag> counter, UnicodeSet okChars, Set<String> mixedSamples) {
    switch (canDisplay(displayLanguage, subtag, code, okChars, mixedSamples)) {
    case code:
      counter.add(Subtag.fail, 1);
      break;
    case localized:
      counter.add(subtag, 1);
      break;
    case badLocalization:
      counter.add(Subtag.mixed, 1);
      break;
    }
  }

  enum Display {code, localized, badLocalization}

  private Display canDisplay(ULocale displayLanguage, Subtag subtag, String code, UnicodeSet okChars, Set<String> mixedSamples) {
    String display;
    switch (subtag) {
    case language:
      display = ULocale.getDisplayLanguage(code, displayLanguage);
      break;
    case script:
      display = ULocale.getDisplayScript("und-" + code, displayLanguage);
      break;
    case region:
      display = ULocale.getDisplayCountry("und-" + code, displayLanguage);
      break;
    default: throw new IllegalArgumentException();
    }
    if (display.equals(code)) {
      return Display.code;
    } else if (okChars.containsAll(display)) {
      return Display.localized;
    } else {
      mixedSamples.add(display);
      UnicodeSet missing = new UnicodeSet().addAll(display).removeAll(okChars);
      return Display.badLocalization;
    }
  }

  private void addIfNotEmpty(Collection<String> languages, String language) {
    if (language != null && language.length() != 0) {
      languages.add(language);
    }
  }

  public void TestLanguageTag() {
    String ulocale = "sq";
    assertNotNull("valid list", UnicodeJsp.getLanguageOptions(ulocale));
    assertNoMatch(null, "Ill-Formed", UnicodeJsp.validateLanguageID("zh-yyy", ulocale));
    assertNoMatch(null, "Ill-Formed", UnicodeJsp.validateLanguageID("arb-SU", ulocale));
    assertNoMatch(null, "Ill-Formed", UnicodeJsp.validateLanguageID("xxx-yyy", ulocale));
    assertNoMatch(null, "Ill-Formed", UnicodeJsp.validateLanguageID("zh-cmn", ulocale));
    assertNoMatch(null, "Ill-Formed", UnicodeJsp.validateLanguageID("en-cmn", ulocale));
    assertNoMatch(null, "Ill-Formed", UnicodeJsp.validateLanguageID("eng-cmn", ulocale));
    assertNoMatch(null, "Ill-Formed", UnicodeJsp.validateLanguageID("xxx-cmn", ulocale));
    assertNoMatch(null, "Ill-Formed", UnicodeJsp.validateLanguageID("zh-eng", ulocale));
    assertNoMatch(null, "Ill-Formed", UnicodeJsp.validateLanguageID("eng-eng", ulocale));
    assertNoMatch(null, "Ill-Formed", UnicodeJsp.validateLanguageID("eng-yyy", ulocale));

    assertNoMatch(null, "Ill-Formed", UnicodeJsp.validateLanguageID("gsw-Hrkt-AQ-pinyin-AbCdE-1901-b-fo-fjdklkfj-23-a-foobar-x-1", ulocale));
    assertNoMatch(null, "Ill-Formed", UnicodeJsp.validateLanguageID("fi-Latn-US", ulocale));
    assertNoMatch(null, "Ill-Formed", UnicodeJsp.validateLanguageID("fil-Latn-US", ulocale));
    assertNoMatch(null, "Ill-Formed", UnicodeJsp.validateLanguageID("aaa-Latn-003-FOOBAR-ALPHA-A-xyzw", ulocale));
    assertNoMatch(null, "Ill-Formed", UnicodeJsp.validateLanguageID("aaa-A-xyzw", ulocale));
    assertNoMatch(null, "Ill-Formed", UnicodeJsp.validateLanguageID("x-aaa-Latn-003-FOOBAR-ALPHA-A-xyzw", ulocale));
    assertNoMatch(null, "Ill-Formed", UnicodeJsp.validateLanguageID("aaa-x-Latn-003-FOOBAR-ALPHA-A-xyzw", ulocale));
    assertMatch(null, "invalid\\scode", UnicodeJsp.validateLanguageID("zho-Xxxx-248", ulocale));
    assertMatch(null, "invalid\\sextlang\\scode", UnicodeJsp.validateLanguageID("aaa-bbb", ulocale));
    assertMatch(null, "Ill-Formed", UnicodeJsp.validateLanguageID("aaa--bbb", ulocale));
    assertMatch(null, "Ill-Formed", UnicodeJsp.validateLanguageID("aaa-bbb-abcdefghihkl", ulocale));
    assertMatch(null, "Ill-Formed", UnicodeJsp.validateLanguageID("1aaa-bbb-abcdefghihkl", ulocale));
  }

  public void assertMatch(String message, String pattern, Object actual) {
    assertMatches(message, Pattern.compile(pattern, Pattern.COMMENTS | Pattern.DOTALL), true, actual);
  }

  public void assertNoMatch(String message, String pattern, Object actual) {
    assertMatches(message, Pattern.compile(pattern, Pattern.COMMENTS | Pattern.DOTALL), false, actual);
  }
  //         return handleAssert(expected == actual, message, stringFor(expected), stringFor(actual), "==", false);

  private void assertMatches(String message, Pattern pattern, boolean expected, Object actual) {
    final String actualString = actual == null ? "null" : actual.toString();
    final boolean result = pattern.matcher(actualString).find() == expected;
    handleAssert(result, 
            message,
            "/" + pattern.toString() + "/", 
            actualString,
            expected ? "matches" : "doesn't match",
                    true);
  }

  public void TestATransform() {
    checkCompleteness(enSample, "en-ipa", new UnicodeSet("[a-z]"));
    checkCompleteness(IPA_SAMPLE, "ipa-en", new UnicodeSet("[a-z]"));
    String sample;
    sample = UnicodeJsp.showTransform("en-IPA; IPA-en", enSample);
    //logln(sample);
    sample = UnicodeJsp.showTransform("en-IPA; IPA-deva", "The quick brown fox.");
    //logln(sample);
    String deva = "कँ, कं, कः, ऄ, अ, आ, इ, ई, उ, ऊ, ऋ, ऌ, ऍ, ऎ, ए, ऐ, ऑ, ऒ, ओ, औ, क, ख, ग, घ, ङ, च, छ, ज, झ, ञ, ट, ठ, ड, ढ, ण, त, थ, द, ध, न, ऩ, प, फ, ब, भ, म, य, र, ऱ, ल, ळ, ऴ, व, श, ष, स, ह, ़, ऽ, क्, का, कि, की, कु, कू, कृ, कॄ, कॅ, कॆ, के, कै, कॉ, कॊ, को, कौ, क्, क़, ख़, ग़, ज़, ड़, ढ़, फ़, य़, ॠ, ॡ, कॢ, कॣ, ०, १, २, ३, ४, ५, ६, ७, ८, ९, ।";
    checkCompleteness(IPA_SAMPLE, "ipa-deva", null);
    checkCompleteness(deva, "deva-ipa", null);
  }

  private void checkCompleteness(String testString, String transId, UnicodeSet exceptionsAllowed) {
    String pieces[] = testString.split(",\\s*");
    UnicodeSet shouldNotBeLeftOver = new UnicodeSet().addAll(testString).remove(' ').remove(',');
    if (exceptionsAllowed != null) {
      shouldNotBeLeftOver.removeAll(exceptionsAllowed);
    }
    UnicodeSet allProblems = new UnicodeSet();
    for (String piece : pieces) {
      String sample = UnicodeJsp.showTransform(transId, piece);
      //logln(piece + " => " + sample);
      if (shouldNotBeLeftOver.containsSome(sample)) {
        final UnicodeSet missing = new UnicodeSet().addAll(sample).retainAll(shouldNotBeLeftOver);
        allProblems.addAll(missing);
        errln("Leftover from " + transId + ": " + missing.toPattern(false));
        Transliterator foo = Transliterator.getInstance(transId, Transliterator.FORWARD);
        //Transliterator.DEBUG = true;
        sample = UnicodeJsp.showTransform(transId, piece);
        //Transliterator.DEBUG = false;
      }
    }
    if (allProblems.size() != 0) {
      errln("ALL Leftover from " + transId + ": " + allProblems.toPattern(false));
    }
  }

  public void TestBidi() {
    String sample;
    sample = UnicodeJsp.showBidi("mark \u05DE\u05B7\u05E8\u05DA\nHelp", 0, true);
    if (!sample.contains(">WS<")) {
      errln(sample);
    }
  }

  public void TestMapping() {
    String sample;
    sample = UnicodeJsp.showTransform("(.) > '<' $1 '> ' &hex/perl($1) ', ';", "Hi There.");
    assertContains(sample, "\\x{69}");
    sample = UnicodeJsp.showTransform("lower", "Abcd");
    assertContains(sample, "abcd");
    sample = UnicodeJsp.showTransform("bc > CB; X > xx;", "Abcd");
    assertContains(sample, "ACBd");
    sample = UnicodeJsp.showTransform("lower", "[[:ascii:]{Abcd}]");
    assertContains(sample, "\u00A0A\u00A0");
    sample = UnicodeJsp.showTransform("bc > CB; X > xx;", "[[:ascii:]{Abcd}]");
    assertContains(sample, "\u00A0ACBd\u00A0");
    sample = UnicodeJsp.showTransform("casefold", "[\\u0000-\\u00FF]");
    assertContains(sample, "\u00A0\u00E1\u00A0");

  }
  
  public void TestGrouping() throws IOException {
    StringWriter printWriter = new StringWriter();
    UnicodeJsp.showSet("sc gc", UnicodeSetUtilities.parseUnicodeSet("[:subhead=/Syllables/:]", TableStyle.extras), true, true, printWriter);
    assertContains(printWriter.toString(), "General_Category=Letter_Number");
    printWriter.getBuffer().setLength(0);
    UnicodeJsp.showSet("subhead", UnicodeSetUtilities.parseUnicodeSet("[:subhead=/Syllables/:]", TableStyle.extras), true, true, printWriter);
    assertContains(printWriter.toString(), "a=A595");
  }

  public void TestStuff() throws IOException {
    //int script = UScript.getScript(0xA6E6);
    //int script2 = UCharacter.getIntPropertyValue(0xA6E6, UProperty.SCRIPT);
    String propValue = UnicodeUtilities.getXStringPropertyValue(UnicodeUtilities.SUBHEAD, 0xA6E6, NameChoice.LONG);
    //System.out.println(propValue);

    //System.out.println("Script for A6E6: " + script + ", " + UScript.getName(script) + ", " + script2);
    checkProperties("[:subhead=/Syllables/:]", "[\u1200]");

    //System.out.println("Script for A6E6: " + script + ", " + UScript.getName(script) + ", " + script2);


    Appendable printWriter = getLogPrintWriter();


    //if (true) return;
    UnicodeJsp.showSet("sc gc", new UnicodeSet("[[:ascii:]{123}{ab}{456}]"), true, true, printWriter);

    UnicodeJsp.showSet("", new UnicodeSet("[\\u0080\\U0010FFFF]"), true, true, printWriter);
    UnicodeJsp.showSet("", new UnicodeSet("[\\u0080\\U0010FFFF{abc}]"), true, true, printWriter);
    UnicodeJsp.showSet("", new UnicodeSet("[\\u0080-\\U0010FFFF{abc}]"), true, true, printWriter);



    String[] abResults = new String[3];
    String[] abLinks = new String[3];
    int[] abSizes = new int[3];
    UnicodeJsp.getDifferences("[:letter:]", "[:idna:]", false, abResults, abSizes, abLinks);
    for (int i = 0; i < abResults.length; ++i) {
      logln(abSizes[i] + "\r\n\t" + abResults[i] + "\r\n\t" + abLinks[i]);
    }

    final UnicodeSet unicodeSet = new UnicodeSet();
    logln("simple: " + UnicodeJsp.getSimpleSet("[a-bm-p\uAc00]", unicodeSet, true, false));
    UnicodeJsp.showSet("", unicodeSet, true, true, printWriter);


    //    String archaic = "[[\u018D\u01AA\u01AB\u01B9-\u01BB\u01BE\u01BF\u021C\u021D\u025F\u0277\u027C\u029E\u0343\u03D0\u03D1\u03D5-\u03E1\u03F7-\u03FB\u0483-\u0486\u05A2\u05C5-\u05C7\u066E\u066F\u068E\u0CDE\u10F1-\u10F6\u1100-\u115E\u1161-\u11FF\u17A8\u17D1\u17DD\u1DC0-\u1DC3\u3165-\u318E\uA700-\uA707\\U00010140-\\U00010174]" +
    //    "[\u02EF-\u02FF\u0363-\u0373\u0376\u0377\u07E8-\u07EA\u1DCE-\u1DE6\u1DFE\u1DFF\u1E9C\u1E9D\u1E9F\u1EFA-\u1EFF\u2056\u2058-\u205E\u2180-\u2183\u2185-\u2188\u2C77-\u2C7D\u2E00-\u2E17\u2E2A-\u2E30\uA720\uA721\uA730-\uA778\uA7FB-\uA7FF]" +
    //    "[\u0269\u027F\u0285-\u0287\u0293\u0296\u0297\u029A\u02A0\u02A3\u02A5\u02A6\u02A8-\u02AF\u0313\u037B-\u037D\u03CF\u03FD-\u03FF]" +
    //"";
    UnicodeJsp.showSet("",UnicodeSetUtilities.parseUnicodeSet("[:usage=/.+/:]", TableStyle.extras), false, false, printWriter);
    UnicodeJsp.showSet("",UnicodeSetUtilities.parseUnicodeSet("[:hantype=/simp/:]", TableStyle.extras), false, false, printWriter);
  }
  
  public void TestShowProperties() throws IOException {
    StringWriter out = new StringWriter();
    UnicodeJsp.showProperties(0x0061, out);
    assertTrue("props for character", out.toString().contains("Line_Break"));
    //System.out.println(out);
  }
  
  public void TestPropsTable() throws IOException {
    StringWriter out = new StringWriter();
    UnicodeJsp.showPropsTable(out);
    assertTrue("props table", out.toString().contains("Line_Break"));
    //System.out.println(out);
  }

  public void TestShowSet() throws IOException {
    StringWriter out = new StringWriter();
//    UnicodeJsp.showSet("sc gc", UnicodeSetUtilities.parseUnicodeSet("[:Hangul_Syllable_Type=LVT_Syllable:]", TableStyle.extras), false, true, out);
//    assertTrue("props table", out.toString().contains("Hangul"));
//    System.out.println(out);
//    
//    out.getBuffer().setLength(0);
//    UnicodeJsp.showSet("sc gc", UnicodeSetUtilities.parseUnicodeSet("[:cn:]", TableStyle.extras), false, true, out);
//    assertTrue("props table", out.toString().contains("unassigned"));
//    System.out.println(out); 
    
    out.getBuffer().setLength(0);
    UnicodeJsp.showSet("sc", UnicodeSetUtilities.parseUnicodeSet("[:script=/Han/:]", TableStyle.extras), false, true, out);
    assertTrue("props table", out.toString().contains("unassigned"));
    //System.out.println(out);


  }
  
  public void TestProperties() {
    checkProperties("[:subhead=/Mayanist/:]", "[\uA726]");

    //checkProperties("[[:script=*latin:]-[:script=latin:]]");
    //checkProperties("[[:script=**latin:]-[:script=latin:]]");
    checkProperties("abc-m", "[d]");

    checkProperties("[:usage=common:]", "[9]");

    checkProperties("[:toNFKC=a:]", "[\u00AA]");
    checkProperties("[:isNFC=false:]", "[\u0308]");
    checkProperties("[:toNFD=A\u0300:]", "[\u00C0]");
    checkProperties("[:toLowercase= /a/ :]", "[aA]");
    checkProperties("[:ASCII:]", "[z]");
    checkProperties("[:lowercase:]", "[a]");
    checkProperties("[:toNFC=/\\./:]", "[.]");
    checkProperties("[:toNFKC=/\\./:]", "[\u2024]");
    checkProperties("[:toNFD=/\\./:]", "[.]");
    checkProperties("[:toNFKD=/\\./:]", "[\u2024]");
    checkProperties("[:toLowercase=/a/:]", "[aA]");
    checkProperties("[:toUppercase=/A/:]", "[Aa\u1E9A]");
    checkProperties("[:toCaseFold=/a/:]", "[Aa\u1E9A]");
    checkProperties("[:toTitlecase=/A/:]", "[Aa\u1E9A]");
    checkProperties("[:idna=valid:]", "[\u0308]");
    checkProperties("[:idna=ignored:]", "[\u00AD]");
    checkProperties("[:idna=mapped:]", "[\u00AA]");
    checkProperties("[:idna=disallowed:]", "[\\u0001]");
    checkProperties("[:iscased:]", "[a-zA-Z]");
    checkProperties("[:name=/WITH/:]", "[\u00C0]");
  }

  void checkProperties(String testString, String containsSet) {
      UnicodeSet tc1 = UnicodeSetUtilities.parseUnicodeSet(testString, TableStyle.extras);
      UnicodeSet contains = new UnicodeSet(containsSet);
      if (!tc1.containsAll(contains)) {
        errln(tc1 + "\t=\t" + tc1.complement().complement() + "\t\nDoesn't contain " + contains);  
      }
  }

  public void TestParameters() {
    UtfParameters parameters = new UtfParameters("ab%61=%C3%A2%CE%94");
    assertEquals("parameters", "\u00E2\u0394", parameters.getParameter("aba"));
  }

  public void TestRegex() {
    final String fix = UnicodeRegex.fix("ab[[:ascii:]&[:Ll:]]*c");
    assertEquals("", "ab[a-z]*c", fix);
    assertEquals("", "<u>abcc</u> <u>abxyzc</u> ab$c", UnicodeJsp.showRegexFind(fix, "abcc abxyzc ab$c"));
  }

  public void TestIdna() {
    String testLines = UnicodeJsp.testIdnaLines("ΣΌΛΟΣ", "[]");
    assertContains(testLines, "xn--wxaikc6b");
    testLines = UnicodeJsp.testIdnaLines(UnicodeJsp.getDefaultIdnaInput(), "[]");
    assertContains(testLines, "xn--bb-eka.at");


    //showIDNARemapDifferences(printWriter);

    expectError("][:idna=valid:][abc]");

    assertTrue("contains hyphen", UnicodeSetUtilities.parseUnicodeSet("[:idna=valid:]", TableStyle.extras).contains('-'));
  }


  public void expectError(String input) {
    try {
      UnicodeSetUtilities.parseUnicodeSet(input, TableStyle.extras);
      errln("Failure to detect syntax error.");
    } catch (IllegalArgumentException e) {
      logln("Expected error: " + e.getMessage());
    }
  }

  public void TestBnf() {
    UnicodeRegex regex = new UnicodeRegex();
    final String[][] tests = {
            {
              "c = a* wq;\n" +
              "a = xyz;\n" +
              "b = a{2} c;\n"
            },
            {
              "c = a* b;\n" +
              "a = xyz;\n" +
              "b = a{2} c;\n",
              "Exception"
            },
            {
              "uri = (?: (scheme) \\:)? (host) (?: \\? (query))? (?: \\u0023 (fragment))?;\n" +
              "scheme = reserved+;\n" +
              "host = \\/\\/ reserved+;\n" +
              "query = [\\=reserved]+;\n" +
              "fragment = reserved+;\n" +
              "reserved = [[:ascii:][:sc=grek:]&[:alphabetic:]];\n",
            "http://αβγ?huh=hi#there"},
            {
              "/Users/markdavis/Documents/workspace/cldr-code/java/org/unicode/cldr/util/data/langtagRegex.txt"
            }
    };
    for (int i = 0; i < tests.length; ++i) {
      String test = tests[i][0];
      final boolean expectException = tests[i].length < 2 ? false : tests[i][1].equals("Exception");
      try {
        String result;
        if (test.endsWith(".txt")) {
          List<String> lines = UnicodeRegex.loadFile(test, new ArrayList<String>());
          result = regex.compileBnf(lines);
        } else {
          result = regex.compileBnf(test);
        }
        if (expectException) {
          errln("Expected exception for " + test);
          continue;
        }
        String result2 = result.replaceAll("[0-9]+%", ""); // just so we can use the language subtag stuff
        String resolved = regex.transform(result2);
        //logln(resolved);
        Matcher m = Pattern.compile(resolved, Pattern.COMMENTS).matcher("");
        String checks = "";
        for (int j = 1; j < tests[i].length; ++j) {
          String check = tests[i][j];
          if (!m.reset(check).matches()) {
            checks = checks + "Fails " + check + "\n";
          } else {
            for (int k = 1; k <= m.groupCount(); ++k) {
              checks += "(" + m.group(k) + ")";
            }
            checks += "\n";
          }
        }
        //logln("Result: " + result + "\n" + checks + "\n" + test);
        String randomBnf = UnicodeJsp.getBnf(result, 10, 10);
        //logln(randomBnf);
      } catch (Exception e) {
        if (!expectException) {
          errln(e.getClass().getName() + ": " + e.getMessage());
        }
        continue;
      }
    }
  }
  
  public void TestBnfMax() {
    BNF bnf = new BNF(new Random(), new Quoter.RuleQuoter());
    bnf.setMaxRepeat(10)
    .addRules("$root=[0-9]+;")
    .complete();
    for (int i = 0; i < 100; ++i) {
      String s = bnf.next();
      assertTrue("Max too large? " + i + ", " + s.length() + ", " + s, 1 <= s.length() && s.length() < 11);
    }
  }

  public void TestBnfGen() {
    String stuff = UnicodeJsp.getBnf("([:Nd:]{3} 90% | abc 10%)", 100, 10);
    assertContains(stuff, "<p>\\U0001D7E8");
    stuff = UnicodeJsp.getBnf("[0-9]+ ([[:WB=MB:][:WB=MN:]] [0-9]+)?", 100, 10);  
    assertContains(stuff, "726283663");
    String bnf = "item = word | number;\n" +
    "word = $alpha+;\n" +
    "number = (digits (separator digits)?);\n" +
    "digits = [:Pd:]+;\n" +
    "separator = [[:WB=MB:][:WB=MN:]];\n" +
    "$alpha = [:alphabetic:];";
    String fixedbnf = new UnicodeRegex().compileBnf(bnf);
    String fixedbnf2 = UnicodeRegex.fix(fixedbnf);
    //String fixedbnfNoPercent = fixedbnf2.replaceAll("[0-9]+%", "");
    String random = UnicodeJsp.getBnf(fixedbnf2, 100, 10);
    assertContains(random, "\\U0002A089");
  }

  private void assertContains(String stuff, String string) {
    if (!stuff.contains(string)) {
      errln(string + " not contained in " + stuff);
    }
  }

  public void TestSimpleSet() {
    checkUnicodeSetParseContains("[a-z\u00e4\u03b1]", "\\p{idna2003=valid}");
    checkUnicodeSetParseContains("[a-z\u00e4\u03b1]", "\\p{idna=valid}");
    checkUnicodeSetParseContains("[a-z\u00e4\u03b1]", "\\p{uts46=valid}");
    checkUnicodeSetParseContains("[a-z\u00e4\u03b1]", "\\p{idna2008=PVALID}");
    checkUnicodeSetParse("[\\u1234\\uABCD-\\uAC00]", "U+1234 U+ABCD-U+AC00");
    checkUnicodeSetParse("[\\u1234\\uABCD-\\uAC00]", "U+1234 U+ABCD..U+AC00");
  }

  private void checkUnicodeSetParse(String expected1, String test) {
    UnicodeSet actual = new UnicodeSet();
    UnicodeSet expected = new UnicodeSet(expected1);
    UnicodeJsp.getSimpleSet(test, actual , true, false);
    TestUnicodeSet.assertEquals(this, test, expected, actual);
  }
  private void checkUnicodeSetParseContains(String expected1, String test) {
    UnicodeSet actual = new UnicodeSet();
    UnicodeSet expectedSubset = new UnicodeSet(expected1);
    UnicodeJsp.getSimpleSet(test, actual , true, false);
    TestUnicodeSet.assertContains(this, test, expectedSubset, actual);
  }
}
