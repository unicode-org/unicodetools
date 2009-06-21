package jsp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.util.BNF;
import com.ibm.icu.dev.test.util.Quoter;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class TestJsp  extends TestFmwk {

  private static final String enSample = "a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x, y, z, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, Z";

  public static void main(String[] args) throws Exception {
    new TestJsp().run(args);
  }
  
  static UnicodeSet IPA = (UnicodeSet) new UnicodeSet("[a-zæçðøħŋœǀ-ǃɐ-ɨɪ-ɶ ɸ-ɻɽɾʀ-ʄʈ-ʒʔʕʘʙʛ-ʝʟʡʢ ʤʧʰ-ʲʴʷʼˈˌːˑ˞ˠˤ̀́̃̄̆̈ ̘̊̋̏-̜̚-̴̠̤̥̩̪̬̯̰̹-̽͜ ͡βθχ↑-↓↗↘]").freeze();
  static String IPA_SAMPLE = "a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x, y, z, æ, ç, ð, ø, ħ, ŋ, œ, ǀ, ǁ, ǂ, ǃ, ɐ, ɑ, ɒ, ɓ, ɔ, ɕ, ɖ, ɗ, ɘ, ə, ɚ, ɛ, ɜ, ɝ, ɞ, ɟ, ɠ, ɡ, ɢ, ɣ, ɤ, ɥ, ɦ, ɧ, ɨ, ɪ, ɫ, ɬ, ɭ, ɮ, ɯ, ɰ, ɱ, ɲ, ɳ, ɴ, ɵ, ɶ, ɸ, ɹ, ɺ, ɻ, ɽ, ɾ, ʀ, ʁ, ʂ, ʃ, ʄ, ʈ, ʉ, ʊ, ʋ, ʌ, ʍ, ʎ, ʏ, ʐ, ʑ, ʒ, ʔ, ʕ, ʘ, ʙ, ʛ, ʜ, ʝ, ʟ, ʡ, ʢ, ʤ, ʧ, ʰ, ʱ, ʲ, ʴ, ʷ, ʼ, ˈ, ˌ, ː, ˑ, ˞, ˠ, ˤ, ̀, ́, ̃, ̄, ̆, ̈, ̊, ̋, ̏, ̐, ̑, ̒, ̓, ̔, ̕, ̖, ̗, ̘, ̙, ̚, ̛, ̜, ̝, ̞, ̟, ̠, ̡, ̢, ̣, ̤, ̥, ̦, ̧, ̨, ̩, ̪, ̫, ̬, ̭, ̮, ̯, ̰, ̱, ̲, ̳, ̴, ̹, ̺, ̻, ̼, ̽, ͜, ͡, β, θ, χ, ↑, →, ↓, ↗, ↘";

  public void TestLanguageTag() {
    String ulocale = "de";
    assertNotNull("valid tag", UnicodeUtilities.validateLanguageID("gsw-Hrkt-AQ-pinyin-AbCdE-1901-b-fo-fjdklkfj-23-a-foobar-x-1", ulocale));
    assertNotNull("valid tag", UnicodeUtilities.validateLanguageID("fi-Latn-US", ulocale));
    assertNotNull("valid tag", UnicodeUtilities.validateLanguageID("fil-Latn-US", ulocale));
    assertNotNull("valid tag", UnicodeUtilities.validateLanguageID("aaa-Latn-003-FOOBAR-ALPHA-A-xyzw", ulocale));
    assertNotNull("valid tag", UnicodeUtilities.validateLanguageID("aaa-A-xyzw", ulocale));
    assertNotNull("valid tag", UnicodeUtilities.validateLanguageID("x-aaa-Latn-003-FOOBAR-ALPHA-A-xyzw", ulocale));
    assertNotNull("valid tag", UnicodeUtilities.validateLanguageID("aaa-x-Latn-003-FOOBAR-ALPHA-A-xyzw", ulocale));
    assertNull("invalid tag", UnicodeUtilities.validateLanguageID("aaa-bbb", ulocale));
  }
  public void TestATransform() {
    checkCompleteness(enSample, "en-ipa", new UnicodeSet("[a-z]"));
    checkCompleteness(IPA_SAMPLE, "ipa-en", new UnicodeSet("[a-z]"));
    String sample;
    sample = UnicodeUtilities.showTransform("en-IPA; IPA-en", enSample);
    logln(sample);
    sample = UnicodeUtilities.showTransform("en-IPA; IPA-deva", "The quick brown fox.");
    logln(sample);
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
      String sample = UnicodeUtilities.showTransform(transId, piece);
      logln(piece + " => " + sample);
      if (shouldNotBeLeftOver.containsSome(sample)) {
        final UnicodeSet missing = new UnicodeSet().addAll(sample).retainAll(shouldNotBeLeftOver);
        allProblems.addAll(missing);
        errln("Leftover from " + transId + ": " + missing.toPattern(false));
        Transliterator foo = Transliterator.getInstance(transId, Transliterator.FORWARD);
        Transliterator.DEBUG = true;
        sample = UnicodeUtilities.showTransform(transId, piece);
        Transliterator.DEBUG = false;
      }
    }
    if (allProblems.size() != 0) {
      errln("ALL Leftover from " + transId + ": " + allProblems.toPattern(false));
    }
  }

  public void TestBidi() {
    String sample;
    sample = UnicodeUtilities.showBidi("mark \u05DE\u05B7\u05E8\u05DA\nHelp", 0, true);
    logln(sample);
  }

  public void TestMapping() {
    String sample;
    sample = UnicodeUtilities.showTransform("(.) > '<' $1 '> ' &hex/perl($1) ', ';", "Hi There.");
    logln(sample);
    sample = UnicodeUtilities.showTransform("lower", "Abcd");
    logln(sample);
    sample = UnicodeUtilities.showTransform("bc > CB; X > xx;", "Abcd");
    logln(sample);
    sample = UnicodeUtilities.showTransform("lower", "[[:ascii:]{Abcd}]");
    logln(sample);
    sample = UnicodeUtilities.showTransform("bc > CB; X > xx;", "[[:ascii:]{Abcd}]");
    logln(sample);
    sample = UnicodeUtilities.showTransform("casefold", "[\\u0000-\\u00FF]");
    logln(sample);

  }

  public void TestStuff() throws IOException {
    Appendable printWriter = getLogPrintWriter();

    //if (true) return;

    UnicodeUtilities.showSet(new UnicodeSet("[\\u0080\\U0010FFFF]"), true, true, printWriter);
    UnicodeUtilities.showSet(new UnicodeSet("[\\u0080\\U0010FFFF{abc}]"), true, true, printWriter);
    UnicodeUtilities.showSet(new UnicodeSet("[\\u0080-\\U0010FFFF{abc}]"), true, true, printWriter);


    UnicodeUtilities.showProperties("a", printWriter);

    String[] abResults = new String[3];
    String[] abLinks = new String[3];
    int[] abSizes = new int[3];
    UnicodeUtilities.getDifferences("[:letter:]", "[:idna:]", false, abResults, abSizes, abLinks);
    for (int i = 0; i < abResults.length; ++i) {
      logln(abSizes[i] + "\r\n\t" + abResults[i] + "\r\n\t" + abLinks[i]);
    }

    final UnicodeSet unicodeSet = new UnicodeSet();
    logln("simple: " + UnicodeUtilities.getSimpleSet("[a-bm-p\uAc00]", unicodeSet, true, false));
    UnicodeUtilities.showSet(unicodeSet, true, true, printWriter);


    //    String archaic = "[[\u018D\u01AA\u01AB\u01B9-\u01BB\u01BE\u01BF\u021C\u021D\u025F\u0277\u027C\u029E\u0343\u03D0\u03D1\u03D5-\u03E1\u03F7-\u03FB\u0483-\u0486\u05A2\u05C5-\u05C7\u066E\u066F\u068E\u0CDE\u10F1-\u10F6\u1100-\u115E\u1161-\u11FF\u17A8\u17D1\u17DD\u1DC0-\u1DC3\u3165-\u318E\uA700-\uA707\\U00010140-\\U00010174]" +
    //    "[\u02EF-\u02FF\u0363-\u0373\u0376\u0377\u07E8-\u07EA\u1DCE-\u1DE6\u1DFE\u1DFF\u1E9C\u1E9D\u1E9F\u1EFA-\u1EFF\u2056\u2058-\u205E\u2180-\u2183\u2185-\u2188\u2C77-\u2C7D\u2E00-\u2E17\u2E2A-\u2E30\uA720\uA721\uA730-\uA778\uA7FB-\uA7FF]" +
    //    "[\u0269\u027F\u0285-\u0287\u0293\u0296\u0297\u029A\u02A0\u02A3\u02A5\u02A6\u02A8-\u02AF\u0313\u037B-\u037D\u03CF\u03FD-\u03FF]" +
    //"";
    UnicodeUtilities.showSet(UnicodeUtilities.parseUnicodeSet("[:archaic=/.+/:]"),false, false, printWriter);

    UnicodeUtilities.showPropsTable(printWriter);
  }

  public void TestProperties() {
    checkProperties("[:subhead=/Mayanist/:]");

    checkProperties("[[:script=*latin:]-[:script=latin:]]");
    checkProperties("[[:script=**latin:]-[:script=latin:]]");
    checkProperties("abc-m");

    checkProperties("[:archaic=no:]");

    checkProperties("[:toNFKC=a:]");
    checkProperties("[:isNFC=false:]");
    checkProperties("[:toNFD=A\u0300:]");
    checkProperties("[:toLowercase= /a/ :]");
    checkProperties("[:toLowercase= /a/ :]");
    checkProperties("[:ASCII:]");
    checkProperties("[:lowercase:]");
    checkProperties("[:toNFC=/\\./:]");
    checkProperties("[:toNFKC=/\\./:]");
    checkProperties("[:toNFD=/\\./:]");
    checkProperties("[:toNFKD=/\\./:]");
    checkProperties("[:toLowercase=/a/:]");
    checkProperties("[:toUppercase=/A/:]");
    checkProperties("[:toCaseFold=/a/:]");
    checkProperties("[:toTitlecase=/A/:]");
    checkProperties("[:idna:]");
    checkProperties("[:idna=ignored:]");
    checkProperties("[:idna=remapped:]");
    checkProperties("[:idna=disallowed:]");
    checkProperties("[:iscased:]");
    checkProperties("[:name=/WITH/:]");
  }

  void checkProperties(String testString) {
    UnicodeSet tc1 = UnicodeUtilities.parseUnicodeSet(testString);
    logln(tc1 + "\t=\t" + tc1.complement().complement());
  }

  public void TestParameters() {
    UtfParameters parameters = new UtfParameters("ab%61=%C3%A2%CE%94");
    assertEquals("parameters", "\u00E2\u0394", parameters.getParameter("aba"));
  }

  public void TestRegex() {
    final String fix = UnicodeRegex.fix("ab[[:ascii:]&[:Ll:]]*c");
    assertEquals("", "ab[a-z]*c", fix);
    assertEquals("", "<u>abcc</u> <u>abxyzc</u> ab$c", UnicodeUtilities.showRegexFind(fix, "abcc abxyzc ab$c"));
  }

  public void TestIdna() {
    String IDNA2008 = "ÖBB\n"
      + "O\u0308BB\n"
      + "Schäffer\n"
      + "ＡＢＣ・フ\n"
      + "I♥NY\n"
      + "faß\n"
      + "βόλος";
    String testLines = UnicodeUtilities.testIdnaLines(IDNA2008, "[]");
    logln(testLines);


    //showIDNARemapDifferences(printWriter);

    expectError("][:idna=output:][abc]");

    assertTrue("contains hyphen", UnicodeUtilities.parseUnicodeSet("[:idna=output:]").contains('-'));
  }

  public void expectError(String input) {
    try {
      UnicodeUtilities.parseUnicodeSet(input);
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
        logln(resolved);
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
        logln("Result: " + result + "\n" + checks + "\n" + test);
        String randomBnf = UnicodeUtilities.getBnf(result, 10, 10);
        logln(randomBnf);
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
      assertTrue("Max too large?", 1 <= s.length() && s.length() < 11);
    }
  }
  
  public void TestBnfGen() {
    String stuff = UnicodeUtilities.getBnf("([:Nd:]{3} 90% | abc 10%)", 100, 10);
    logln(stuff);
    stuff = UnicodeUtilities.getBnf("[0-9]+ ([[:WB=MB:][:WB=MN:]] [0-9]+)?", 100, 10);  
    logln(stuff);
    String bnf = "item = word | number;\n" +
      "word = $alpha+;\n" +
      "number = (digits (separator digits)?);\n" +
      "digits = [:Pd:]+;\n" +
      "separator = [[:WB=MB:][:WB=MN:]];\n" +
      "$alpha = [:alphabetic:];";
    String fixedbnf = new UnicodeRegex().compileBnf(bnf);
    String fixedbnf2 = UnicodeRegex.fix(fixedbnf);
    //String fixedbnfNoPercent = fixedbnf2.replaceAll("[0-9]+%", "");
    String random = UnicodeUtilities.getBnf(fixedbnf2, 100, 10);
    logln(random);
  }
}
