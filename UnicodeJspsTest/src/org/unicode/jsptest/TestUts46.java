package org.unicode.jsptest;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.jsp.UnicodeUtilities;
import org.unicode.jsp.Uts46;
import org.unicode.jsp.Uts46.Errors;
import org.unicode.jsp.Uts46.IdnaChoice;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

public class TestUts46 extends TestFmwk{

    public static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'GMT'", ULocale.US);
    static {
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

  boolean generate;

  public static void main(String[] args) throws IOException {
    checkSplit(".a..b.");
    checkSplit(".");
    
    TestUts46 testUts46 = new TestUts46();
    if (Arrays.asList(args).contains("generate")) {
      int count = testUts46.generateTests(1000);
      System.out.println("DONE " + count);
      return;
    }
    testUts46.run(args);
  }

  private static void checkSplit(String testcase) {
    String[] labels = Uts46.nonbrokenSplit(Uts46.FULL_STOP, testcase);
    System.out.print("Splitting: \"" + testcase + "\"\t=>\t");
    boolean first = true;
    for (String label : labels) {
      if (first) first = false;
      else System.out.print(", ");
      System.out.print("\"" + label + "\"");
    }
    System.out.println("");
  }


  //  1.  The first character must be a character with BIDI property L, R
  //  or AL.  If it has the R or AL property, it is an RTL label; if it
  //  has the L property, it is an LTR label.
  //
  //2.  In an RTL label, only characters with the BIDI properties R, AL,
  //  AN, EN, ES, CS, ET, ON, BN and NSM are allowed.
  // in practice, this excludes L
  //
  //3.  In an RTL label, the end of the label must be a character with
  //  BIDI property R, AL, EN or AN, followed by zero or more
  //  characters with BIDI property NSM.
  //
  //4.  In an RTL label, if an EN is present, no AN may be present, and
  //  vice versa.
  //
  //5.  In an LTR label, only characters with the BIDI properties L, EN,
  //  ES, CS.  ET, ON, BN and NSM are allowed.
  // in practice, this excludes R, AL
  //
  // 6.  In an LTR label, the end of the label must be a character with
  //  BIDI property L or EN, followed by zero or more characters with
  //  BIDI property NSM.

  static final char SAMPLE_L = 'à'; // U+00E0 ( à ) LATIN SMALL LETTER A WITH GRAVE
  static final char SAMPLE_R_AL = 'א'; // U+05D0 ( ‎א‎ ) HEBREW LETTER ALEF
  static final char SAMPLE_AN = '\u0660'; // U+0660 ( ٠ ) ARABIC-INDIC DIGIT ZERO
  static final char SAMPLE_EN = '0'; // U+0030 ( 0 ) DIGIT ZERO
  static final char SAMPLE_ES = '-'; // U+002D ( - ) HYPHEN-MINUS
  static final char SAMPLE_ES_CS_ET_ON_BN = '\u02C7'; // U+02C7 ( ˇ ) CARON
  static final char SAMPLE_NSM = '\u0308'; // U+02C7 ( ˇ ) CARON
  private static final String DIR = "/Users/markdavis/Documents/workspace/draft/reports/tr46/data";


  private int errorNum;

  String[][] bidiTests = { 
          {"à" + SAMPLE_R_AL, "B5", "B6"},
          {"0à." + SAMPLE_R_AL,"B1"},
          {"à." + SAMPLE_R_AL + SAMPLE_NSM},
          {"à." + SAMPLE_R_AL + SAMPLE_EN + SAMPLE_AN + SAMPLE_R_AL, "B4"},
          {SAMPLE_NSM + "." + SAMPLE_R_AL + "","B3"},
          {"à." + SAMPLE_R_AL + "0" + SAMPLE_AN,"B4"},
          {"à" + SAMPLE_ES_CS_ET_ON_BN + "." + SAMPLE_R_AL + "","B6"},
          {"à" + SAMPLE_NSM + "." + SAMPLE_R_AL + ""},
  };

  // CS, ET, ON, BN and NSM
  public void TestBidi() {
    Set<Errors> errors = new LinkedHashSet<Errors>();

    for (String[] test : bidiTests) {
      String domain = test[0];
      Collection<String> rules = Arrays.asList(test).subList(1, test.length);
      //String rule = test[1];
      errors.clear();
      boolean error = Uts46.hasBidiError(domain, errors);
      checkErrors(domain, rules, error, errors);
    }
  }

  private String[][] contextTests = new String[][] { 
          {"a\u200Cb","C1"},
          {"a\u094D\u200Cb"},
          {"\u0308\u200C\u0308بb","C1"},
          {"aب\u0308\u200C\u0308","C1"},
          {"aب\u0308\u200C\u0308بb"},

          {"a\u200Db","C2"},
          {"a\u094D\u200Db"},
          {"\u0308\u200D\u0308بb","C2"},
          {"aب\u0308\u200D\u0308","C2"},
          {"aب\u0308\u200D\u0308بb","C2"},
  };

  public void TestContextJ() {
    Set<Errors> errors = new LinkedHashSet<Errors>();

    for (String[] test : contextTests) {
      String domain = test[0];
      Collection<String> rules = Arrays.asList(test).subList(1, test.length);
      errors.clear();
      boolean error = Uts46.hasContextJError(domain, errors);
      checkErrors(domain, rules, error, errors);
    }
  }

  private void checkErrors(String domain, Collection<String> rules, boolean error, Set<Errors> errors) {
    if (rules.size() == 0) { // no error expected
      if (error) {
        errln("Domain " + domain + " should NOT fail, got:\t" + errors);
      } else {
        logln("Domain " + " should NOT fail, got:\t" + errors);
      }
    } else {
      if (!error || !containsAllOf(errors,rules)) {
        errln("Domain " + domain + " should fail with " + rules + ", got:\t" + errors);
      } else {
        logln("Domain " + " should fail with " + rules + ", got:\t" + errors);
      }
    }
  }
  /**
   * Return true if each rule is contained in at least one error.
   * @param errors
   * @param rules
   * @return
   */
  private boolean containsAllOf(Set<Errors> errors, Collection<String> rules) {
    main:
      for (String rule : rules) {
        for (Errors error : errors) {
          if (error.toString().contains(rule)) {
            continue main;
          }
        }
        return false;
      }
    return true;
  }

  private int generateTests(int lines) throws IOException {
    String filename = "IdnaTest.txt";
    PrintWriter out = FileUtilities.openUTF8Writer(DIR, filename);
    out.println("# " + filename + "\n" +
            "# Date: " + dateFormat.format(new Date()) + " [MD]\n" +
            "#");

    FileUtilities.appendFile(this.getClass().getResource("IdnaTestHeader.txt").toString().substring(5), "UTF-8", out);
//    out.println(
//            "# Format\n" +
//            "# source ; type ; toASCII ; toUnicode\n" +
//            "# type: T for transitional, N for nontransitional, B for both\n" +
//            "# In case of errors, field 3 and 4 show errors in [....] instead of a result\n" +
//            "# The errors are based on the step numbers in UTS46.\n" +
//            "# Pn for Section 4 Processing step n\n" +
//            "# Vn for 4.1 Validity Criteria step n\n" +
//            "# An for 4.2 ToASCII step n\n" +
//            "# Bn for Bidi (in IDNA2008)\n" +
//            "# Cn for ContextJ (in IDNA2008)\n" +
//            "\n"
//    );
    int count = 0;

    count += generateLine("fass.de", out);
    count += generateLine("faß.de", out);
    
    out.println("\n# BIDI TESTS\n");
    
    for (String[] testCase : bidiTests) {
      count += generateLine(testCase[0], out);
    }

    out.println("\n# CONTEXT TESTS\n");

    for (String[] testCase : contextTests) {
      count += generateLine(testCase[0], out);
    }

    out.println("\n# SELECTED TESTS\n");

    count += generateLine("\u00a1", out);

    for (Object[] testCaseLine : testCases) {
      String source = testCaseLine[0].toString();
      count += generateLine(source, out);
    }
    
    out.println("\n# RANDOMIZED TESTS\n");

    RandomString randomString = new RandomString();
    char[] LABELSEPARATORS = {'\u002E', '\uFF0E', '\u3002', '\uFF61'};
    StringBuilder sb = new StringBuilder();

    for (; lines > 0; --lines) {
      sb.setLength(0);
      // random number of labels
      int labels = RandomString.random.nextInt(2);
      for (; labels >= 0; --labels) {
        randomString.appendNext(sb);
        if (labels != 0) {
          sb.append(LABELSEPARATORS[RandomString.random.nextInt(4)]);
        }
      }
      // random length
      count += generateLine(sb.toString(), out);
    }
    out.close();
    return count;
  }

  static class RandomString {
    static Random random = new Random(0);
    static UnicodeSet[] sampleSets;
    static {
      String[] samples = {
              // bidi
              "[[:bc=R:][:bc=AL:]]",
              "[[:bc=L:]]",
              "[[:bc=ES:][:bc=CS:][:bc=ET:][:bc=ON:][:bc=BN:][:bc=NSM:]]",
              "[[:bc=EN:]]",
              "[[:bc=AN:]]",
              "[[:bc=NSM:]]",
              // contextj
              "[\u200C\u200D]",
              "[:ccc=virama:]",
              "[:jt=T:]",
              "[[:jt=L:][:jt=D:]]",
              "[[:jt=R:][:jt=D:]]",
              // syntax
              "[-]",
              // changed mapping from 2003
              "[\u04C0 \u10A0-\u10C5 \u2132 \u2183 \u2F868 \u2F874 \u2F91F \u2F95F \u2F9BF \u3164 \uFFA0 \u115F \u1160 \u17B4 \u17B5 \u1806]",
              // disallowed in 2003
              "[\u200E-\u200F \u202A-\u202E \u2061-\u2063 \uFFFC \uFFFD \u1D173-\u1D17A \u206A-\u206F \uE0001 \uE0020-\uE007F]",
              // Step 7
              "[\u2260 \u226E \u226F \uFE12 \u2488]",
              // disallowed
              "[:cn:]",
              // deviations
              "[\\u200C\\u200D\\u00DF\\u03C2]",
      };
      sampleSets = new UnicodeSet[samples.length];
      for (int i = 0; i < samples.length; ++i) {
        sampleSets[i] = new UnicodeSet(samples[i]).freeze();
      }
    }
    void appendNext(StringBuilder sb) {
      int len = 1 + random.nextInt(4);
      // random contents, picking from random set
      for (; len > 0; --len) {
        int setNum = random.nextInt(sampleSets.length);
        UnicodeSet uset = sampleSets[setNum];
        int size = uset.size();
        int indexInSet = random.nextInt(size);
        int cp = uset.charAt(indexInSet);
        sb.appendCodePoint(cp);
      }
    }
  }

  Set<String> alreadySeen = new HashSet<String>();
  
  static final Normalizer2 nfd = Normalizer2.getInstance(null, "nfc", Normalizer2.Mode.COMPOSE);
  static final Normalizer2 nfc = Normalizer2.getInstance(null, "nfc", Normalizer2.Mode.COMPOSE);
  static final Normalizer2 nfkc = Normalizer2.getInstance(null, "nfc", Normalizer2.Mode.COMPOSE);
  static final Normalizer2 nfkd = Normalizer2.getInstance(null, "nfc", Normalizer2.Mode.COMPOSE);

  
  int generateLine(String source, PrintWriter out) {
    if (alreadySeen.contains(source)) {
      return 0;
    }
    alreadySeen.add(source);
    int result = 0;
    Set<Errors> transitionalErrors = new LinkedHashSet<Errors>();
    Set<Errors> nonTransitionalErrors = new LinkedHashSet<Errors>();
    String transitional = Uts46.SINGLETON.toASCII(source, IdnaChoice.transitional, transitionalErrors);
    String nontransitional = Uts46.SINGLETON.toASCII(source, IdnaChoice.nontransitional, nonTransitionalErrors);
    Set<Errors> toUnicodeErrors = new LinkedHashSet<Errors>();
    String unicode = Uts46.SINGLETON.toUnicode(source, IdnaChoice.nontransitional, toUnicodeErrors);

    if (!transitionalErrors.equals(nonTransitionalErrors) 
            || !transitional.equals(nontransitional)
            && transitionalErrors.size() == 0) {
      showLine(source, "T", transitional, transitionalErrors, unicode, toUnicodeErrors, out);
      showLine(source, "N", nontransitional, nonTransitionalErrors, unicode, toUnicodeErrors, out);
      result += 2;
    } else {
      showLine(source, "B", nontransitional, nonTransitionalErrors, unicode, toUnicodeErrors, out);
      result += 1;
    }
    result += generateLine(nfc.normalize(source), out);
    result += generateLine(nfd.normalize(source), out);
    result += generateLine(nfkc.normalize(source), out);
    result += generateLine(nfkd.normalize(source), out);
    result += generateLine(UCharacter.toLowerCase(source), out);
    result += generateLine(UCharacter.toUpperCase(source), out);
    result += generateLine(UCharacter.foldCase(source, true), out);
    result += generateLine(UCharacter.toTitleCase(source, null), out);
    if (transitionalErrors.size() == 0) {
      result += generateLine(transitional, out);
    }
    if (nonTransitionalErrors.size() == 0) {
      result += generateLine(nontransitional, out);
    }
    if (toUnicodeErrors.size() == 0) {
      result += generateLine(unicode, out);
    }
    return result;
  }

  Transliterator hexForTest = Transliterator.getInstance("[[:c:][:z:][:m:][:di:][:bc=R:][:bc=AL:][:bc=AN:]] any-hex");
  static UnicodeSet IDNA2008Valid = new UnicodeSet(UnicodeUtilities.getIdna2008Valid()).add('.').freeze();

  /**
   * Draws line
   */
  private void showLine(String source, String type, String ascii, Set<Errors> asciiErrors, String unicode, Set<Errors> toUnicodeErrors, PrintWriter out) {
      String unicodeReveal = hexForTest.transform(unicode);
      boolean hasUnicodeErrors = toUnicodeErrors.size() != 0;
      boolean hasAsciiErrors = asciiErrors.size() != 0;
      boolean allCharsIdna2008 = IDNA2008Valid.containsAll(unicode);
      out.println(type
              + ";\t" 
              + hexForTest.transform(source)
              + ";\t"
              + (hasUnicodeErrors ? showErrors(toUnicodeErrors) : unicode.equals(source) ? "" : unicodeReveal)
              + ";\t"
              + (hasAsciiErrors ? showErrors(asciiErrors) : unicode.equals(ascii) ? "" :  hexForTest.transform(ascii))
              + ";\t"
              + (hasUnicodeErrors || allCharsIdna2008 ? "" : "NV8")
              + (unicodeReveal.equals(unicode) ? "" : "\t#\t" + unicode)
      );
  }

  private String showErrors(Set<Errors> errors) {
    return "[" + CollectionUtilities.join(errors, " ") + "]";
  }
  
  enum TestType {B, N, T}
  
  public void TestIcuCases() {
    Set<Errors> toUnicodeErrors = new LinkedHashSet<Errors>();
    for (Object[] testCaseLine : testCases) {
      String source = testCaseLine[0].toString();
      TestType type = TestType.valueOf(testCaseLine[1].toString());
      String target = testCaseLine[2].toString();
      String targetError = testCaseLine[3].toString();
      // do test
      if (type.equals(TestType.B) || type.equals(TestType.N)) {
        checkIcuTestCases(IdnaChoice.nontransitional, source, toUnicodeErrors, target, targetError);
      }
      if (type.equals(TestType.B) || type.equals(TestType.T)) {
        checkIcuTestCases(IdnaChoice.transitional, source, toUnicodeErrors, target, targetError);        
      }
    }
  }

  private void checkIcuTestCases(IdnaChoice idnaChoice, String source, Set<Errors> toUnicodeErrors, String target, String targetError) {
    toUnicodeErrors.clear();
    String unicode = Uts46.SINGLETON.toUnicode(source, idnaChoice, toUnicodeErrors);
    String punycode = Uts46.SINGLETON.toASCII(source, idnaChoice, toUnicodeErrors);
    boolean expectedError = !targetError.equals("0");
    boolean actualError = toUnicodeErrors.size() != 0;
    if (expectedError != actualError) {
      errln("Error code for: " + source + "\texpected:\t" + (expectedError ? targetError : "NO_ERROR")
              + "\tactual:\t" + (actualError ? toUnicodeErrors.toString() : "NO_ERROR"));
    } else if (!actualError) {
      logln("Error code for: " + source + "\texpected:\t" + (expectedError ? targetError : "NO_ERROR")
              + "\tactual:\t" + (actualError ? toUnicodeErrors.toString() : "NO_ERROR"));
      assertEquals("Result for: " + source, target, unicode);
    }
  }

  static final Object[][] testCases = {
    { "1234567890\u00E41234567890123456789012345678901234567890123456", "B",
      "1234567890\u00E41234567890123456789012345678901234567890123456", Uts46.UIDNA_ERROR_LABEL_TOO_LONG },

    { "www.eXample.cOm", "B",  // all ASCII
      "www.example.com", 0 },
      { "B\u00FCcher.de", "B",  // u-umlaut
      "b\u00FCcher.de", 0 },
      { "\u00D6BB", "B",  // O-umlaut
      "\u00F6bb", 0 },
      { "fa\u00DF.de", "N",  // sharp s
      "fa\u00DF.de", 0 },
      { "fa\u00DF.de", "T",  // sharp s
      "fass.de", 0 },
      { "XN--fA-hia.dE", "B",  // sharp s in Punycode
      "fa\u00DF.de", 0 },
      { "\u03B2\u03CC\u03BB\u03BF\u03C2.com", "N",  // Greek with final sigma
      "\u03B2\u03CC\u03BB\u03BF\u03C2.com", 0 },
      { "\u03B2\u03CC\u03BB\u03BF\u03C2.com", "T",  // Greek with final sigma
      "\u03B2\u03CC\u03BB\u03BF\u03C3.com", 0 },
      { "xn--nxasmm1c", "B",  // Greek with final sigma in Punycode
      "\u03B2\u03CC\u03BB\u03BF\u03C2", 0 },
      { "www.\u0DC1\u0DCA\u200D\u0DBB\u0DD3.com", "N",  // "Sri" in "Sri Lanka" has a ZWJ
      "www.\u0DC1\u0DCA\u200D\u0DBB\u0DD3.com", 0 },
      { "www.\u0DC1\u0DCA\u200D\u0DBB\u0DD3.com", "T",  // "Sri" in "Sri Lanka" has a ZWJ
      "www.\u0DC1\u0DCA\u0DBB\u0DD3.com", 0 },
      { "www.xn--10cl1a0b660p.com", "B",  // "Sri" in Punycode
      "www.\u0DC1\u0DCA\u200D\u0DBB\u0DD3.com", 0 },
      { "\u0646\u0627\u0645\u0647\u200C\u0627\u06CC", "N",  // ZWNJ
      "\u0646\u0627\u0645\u0647\u200C\u0627\u06CC", 0 },
      { "\u0646\u0627\u0645\u0647\u200C\u0627\u06CC", "T",  // ZWNJ
      "\u0646\u0627\u0645\u0647\u0627\u06CC", 0 },
      { "xn--mgba3gch31f060k.com", "B",  // ZWNJ in Punycode
      "\u0646\u0627\u0645\u0647\u200C\u0627\u06CC.com", 0 },
      { "a.b\uFF0Ec\u3002d\uFF61", "B",
      "a.b.c.d.", 0 },
      { "U\u0308.xn--tda", "B",  // U+umlaut.u-umlaut
      "\u00FC.\u00FC", 0 },
      { "xn--u-ccb", "B",  // u+umlaut in Punycode
      "xn--u-ccb\uFFFD", Uts46.UIDNA_ERROR_INVALID_ACE_LABEL },
      { "a\u2488com", "B",  // contains 1-dot
      "a\uFFFDcom", Uts46.UIDNA_ERROR_DISALLOWED },
      { "xn--a-ecp.ru", "B",  // contains 1-dot in Punycode
      "xn--a-ecp\uFFFD.ru", Uts46.UIDNA_ERROR_INVALID_ACE_LABEL },
      { "xn--0.pt", "B",  // invalid Punycode
      "xn--0\uFFFD.pt", Uts46.UIDNA_ERROR_PUNYCODE },
      { "xn--a.pt", "B",  // U+0080
      "xn--a\uFFFD.pt", Uts46.UIDNA_ERROR_INVALID_ACE_LABEL },
      { "xn--a-\u00C4.pt", "B",  // invalid Punycode
      "xn--a-\u00E4.pt", Uts46.UIDNA_ERROR_PUNYCODE },
      { "\u65E5\u672C\u8A9E\u3002\uFF2A\uFF30", "B",  // Japanese with fullwidth ".jp"
      "\u65E5\u672C\u8A9E.jp", 0 },
      { "\u2615", "B", "\u2615", 0 },  // Unicode 4.0 HOT BEVERAGE
      // many deviation characters, test the special mapping code
      { "1.a\u00DF\u200C\u200Db\u200C\u200Dc\u00DF\u00DF\u00DF\u00DFd"
      + "\u03C2\u03C3\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DFe"
      + "\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DFx"
      + "\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DFy"
      + "\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u0302\u00DFz", "N",
      "1.a\u00DF\u200C\u200Db\u200C\u200Dc\u00DF\u00DF\u00DF\u00DFd"
      + "\u03C2\u03C3\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DFe"
      + "\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DFx"
      + "\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DFy"
      + "\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u0302\u00DFz",
      Uts46.UIDNA_ERROR_LABEL_TOO_LONG|Uts46.UIDNA_ERROR_CONTEXTJ },
      { "1.a\u00DF\u200C\u200Db\u200C\u200Dc\u00DF\u00DF\u00DF\u00DFd"
      + "\u03C2\u03C3\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DFe"
      + "\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DFx"
      + "\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DFy"
      + "\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u0302\u00DFz", "T",
      "1.assbcssssssssd"
      + "\u03C3\u03C3sssssssssssssssse"
      + "ssssssssssssssssssssx"
      + "ssssssssssssssssssssy"
      + "sssssssssssssss\u015Dssz", Uts46.UIDNA_ERROR_LABEL_TOO_LONG },
      // "xn--bss" with deviation characters
      { "\u200Cx\u200Dn\u200C-\u200D-b\u00DF", "N",
      "\u200Cx\u200Dn\u200C-\u200D-b\u00DF", Uts46.UIDNA_ERROR_CONTEXTJ },
      { "\u200Cx\u200Dn\u200C-\u200D-b\u00DF", "T",
      "\u5919", 0 },
      // "xn--bssffl" written as:
      // 02E3 MODIFIER LETTER SMALL X
      // 034F COMBINING GRAPHEME JOINER (ignored)
      // 2115 DOUBLE-STRUCK CAPITAL N
      // 200B ZERO WIDTH SPACE (ignored)
      // FE63 SMALL HYPHEN-MINUS
      // 00AD SOFT HYPHEN (ignored)
      // FF0D FULLWIDTH HYPHEN-MINUS
      // 180C MONGOLIAN FREE VARIATION SELECTOR TWO (ignored)
      // 212C SCRIPT CAPITAL B
      // FE00 VARIATION SELECTOR-1 (ignored)
      // 017F LATIN SMALL LETTER LONG S
      // 2064 INVISIBLE PLUS (ignored)
      // 1D530 MATHEMATICAL FRAKTUR SMALL S
      // E01EF VARIATION SELECTOR-256 (ignored)
      // FB04 LATIN SMALL LIGATURE FFL
      { "\u02E3\u034F\u2115\u200B\uFE63\u00AD\uFF0D\u180C"
        + "\u212C\uFE00\u017F\u2064"
        //+ "\\U0001D530" 
        + UTF16.valueOf(0x1D530)
        //+ "\\U000E01EF"
        + UTF16.valueOf(0xE01EF)
        + "\uFB04", "B",
        "\u5921\u591E\u591C\u5919", 0 },
      { "123456789012345678901234567890123456789012345678901234567890123."
      + "123456789012345678901234567890123456789012345678901234567890123."
      + "123456789012345678901234567890123456789012345678901234567890123."
      + "1234567890123456789012345678901234567890123456789012345678901", "B",
      "123456789012345678901234567890123456789012345678901234567890123."
      + "123456789012345678901234567890123456789012345678901234567890123."
      + "123456789012345678901234567890123456789012345678901234567890123."
      + "1234567890123456789012345678901234567890123456789012345678901", 0 },
      { "123456789012345678901234567890123456789012345678901234567890123."
      + "123456789012345678901234567890123456789012345678901234567890123."
      + "123456789012345678901234567890123456789012345678901234567890123."
      + "1234567890123456789012345678901234567890123456789012345678901.", "B",
      "123456789012345678901234567890123456789012345678901234567890123."
      + "123456789012345678901234567890123456789012345678901234567890123."
      + "123456789012345678901234567890123456789012345678901234567890123."
      + "1234567890123456789012345678901234567890123456789012345678901.", 0 },
      { "123456789012345678901234567890123456789012345678901234567890123."
      + "123456789012345678901234567890123456789012345678901234567890123."
      + "123456789012345678901234567890123456789012345678901234567890123."
      + "12345678901234567890123456789012345678901234567890123456789012", "B",
      "123456789012345678901234567890123456789012345678901234567890123."
      + "123456789012345678901234567890123456789012345678901234567890123."
      + "123456789012345678901234567890123456789012345678901234567890123."
      + "12345678901234567890123456789012345678901234567890123456789012",
      Uts46.UIDNA_ERROR_DOMAIN_NAME_TOO_LONG },
      { "123456789012345678901234567890123456789012345678901234567890123."
      + "1234567890123456789012345678901234567890123456789012345678901234."
      + "123456789012345678901234567890123456789012345678901234567890123."
      + "123456789012345678901234567890123456789012345678901234567890", "B",
      "123456789012345678901234567890123456789012345678901234567890123."
      + "1234567890123456789012345678901234567890123456789012345678901234."
      + "123456789012345678901234567890123456789012345678901234567890123."
      + "123456789012345678901234567890123456789012345678901234567890",
      Uts46.UIDNA_ERROR_LABEL_TOO_LONG },
      { "123456789012345678901234567890123456789012345678901234567890123."
      + "1234567890123456789012345678901234567890123456789012345678901234."
      + "123456789012345678901234567890123456789012345678901234567890123."
      + "123456789012345678901234567890123456789012345678901234567890.", "B",
      "123456789012345678901234567890123456789012345678901234567890123."
      + "1234567890123456789012345678901234567890123456789012345678901234."
      + "123456789012345678901234567890123456789012345678901234567890123."
      + "123456789012345678901234567890123456789012345678901234567890.",
      Uts46.UIDNA_ERROR_LABEL_TOO_LONG },
      { "123456789012345678901234567890123456789012345678901234567890123."
      + "1234567890123456789012345678901234567890123456789012345678901234."
      + "123456789012345678901234567890123456789012345678901234567890123."
      + "1234567890123456789012345678901234567890123456789012345678901", "B",
      "123456789012345678901234567890123456789012345678901234567890123."
      + "1234567890123456789012345678901234567890123456789012345678901234."
      + "123456789012345678901234567890123456789012345678901234567890123."
      + "1234567890123456789012345678901234567890123456789012345678901",
      Uts46.UIDNA_ERROR_LABEL_TOO_LONG|Uts46.UIDNA_ERROR_DOMAIN_NAME_TOO_LONG },
      // label length 63: xn--1234567890123456789012345678901234567890123456789012345-9te
      { "\u00E41234567890123456789012345678901234567890123456789012345", "B",
      "\u00E41234567890123456789012345678901234567890123456789012345", 0 },
      { "1234567890\u00E41234567890123456789012345678901234567890123456", "B",
      "1234567890\u00E41234567890123456789012345678901234567890123456", Uts46.UIDNA_ERROR_LABEL_TOO_LONG },
      { "123456789012345678901234567890123456789012345678901234567890123."
      + "1234567890\u00E4123456789012345678901234567890123456789012345."
      + "123456789012345678901234567890123456789012345678901234567890123."
      + "1234567890123456789012345678901234567890123456789012345678901", "B",
      "123456789012345678901234567890123456789012345678901234567890123."
      + "1234567890\u00E4123456789012345678901234567890123456789012345."
      + "123456789012345678901234567890123456789012345678901234567890123."
      + "1234567890123456789012345678901234567890123456789012345678901", 0 },
      { "123456789012345678901234567890123456789012345678901234567890123."
      + "1234567890\u00E4123456789012345678901234567890123456789012345."
      + "123456789012345678901234567890123456789012345678901234567890123."
      + "1234567890123456789012345678901234567890123456789012345678901.", "B",
      "123456789012345678901234567890123456789012345678901234567890123."
      + "1234567890\u00E4123456789012345678901234567890123456789012345."
      + "123456789012345678901234567890123456789012345678901234567890123."
      + "1234567890123456789012345678901234567890123456789012345678901.", 0 },
      { "123456789012345678901234567890123456789012345678901234567890123."
      + "1234567890\u00E4123456789012345678901234567890123456789012345."
      + "123456789012345678901234567890123456789012345678901234567890123."
      + "12345678901234567890123456789012345678901234567890123456789012", "B",
      "123456789012345678901234567890123456789012345678901234567890123."
      + "1234567890\u00E4123456789012345678901234567890123456789012345."
      + "123456789012345678901234567890123456789012345678901234567890123."
      + "12345678901234567890123456789012345678901234567890123456789012",
      Uts46.UIDNA_ERROR_DOMAIN_NAME_TOO_LONG },
      { "123456789012345678901234567890123456789012345678901234567890123."
      + "1234567890\u00E41234567890123456789012345678901234567890123456."
      + "123456789012345678901234567890123456789012345678901234567890123."
      + "123456789012345678901234567890123456789012345678901234567890", "B",
      "123456789012345678901234567890123456789012345678901234567890123."
      + "1234567890\u00E41234567890123456789012345678901234567890123456."
      + "123456789012345678901234567890123456789012345678901234567890123."
      + "123456789012345678901234567890123456789012345678901234567890",
      Uts46.UIDNA_ERROR_LABEL_TOO_LONG },
      { "123456789012345678901234567890123456789012345678901234567890123."
      + "1234567890\u00E41234567890123456789012345678901234567890123456."
      + "123456789012345678901234567890123456789012345678901234567890123."
      + "123456789012345678901234567890123456789012345678901234567890.", "B",
      "123456789012345678901234567890123456789012345678901234567890123."
      + "1234567890\u00E41234567890123456789012345678901234567890123456."
      + "123456789012345678901234567890123456789012345678901234567890123."
      + "123456789012345678901234567890123456789012345678901234567890.",
      Uts46.UIDNA_ERROR_LABEL_TOO_LONG },
      { "123456789012345678901234567890123456789012345678901234567890123."
      + "1234567890\u00E41234567890123456789012345678901234567890123456."
      + "123456789012345678901234567890123456789012345678901234567890123."
      + "1234567890123456789012345678901234567890123456789012345678901", "B",
      "123456789012345678901234567890123456789012345678901234567890123."
      + "1234567890\u00E41234567890123456789012345678901234567890123456."
      + "123456789012345678901234567890123456789012345678901234567890123."
      + "1234567890123456789012345678901234567890123456789012345678901",
      Uts46.UIDNA_ERROR_LABEL_TOO_LONG|Uts46.UIDNA_ERROR_DOMAIN_NAME_TOO_LONG },
      // hyphen errors and empty-label errors
      // "xn---q----jra"=="-q--a-umlaut-"
      { "a.b..-q--a-.e", "B", "a.b..-q--a-.e",
      Uts46.UIDNA_ERROR_EMPTY_LABEL|Uts46.UIDNA_ERROR_LEADING_HYPHEN|Uts46.UIDNA_ERROR_TRAILING_HYPHEN|
      Uts46.UIDNA_ERROR_HYPHEN_3_4 },
      { "a.b..-q--\u00E4-.e", "B", "a.b..-q--\u00E4-.e",
      Uts46.UIDNA_ERROR_EMPTY_LABEL|Uts46.UIDNA_ERROR_LEADING_HYPHEN|Uts46.UIDNA_ERROR_TRAILING_HYPHEN|
      Uts46.UIDNA_ERROR_HYPHEN_3_4 },
      { "a.b..xn---q----jra.e", "B", "a.b..-q--\u00E4-.e",
      Uts46.UIDNA_ERROR_EMPTY_LABEL|Uts46.UIDNA_ERROR_LEADING_HYPHEN|Uts46.UIDNA_ERROR_TRAILING_HYPHEN|
      Uts46.UIDNA_ERROR_HYPHEN_3_4 },
      { "a..c", "B", "a..c", Uts46.UIDNA_ERROR_EMPTY_LABEL },
      { "a.-b.", "B", "a.-b.", Uts46.UIDNA_ERROR_LEADING_HYPHEN },
      { "a.b-.c", "B", "a.b-.c", Uts46.UIDNA_ERROR_TRAILING_HYPHEN },
      { "a.-.c", "B", "a.-.c", Uts46.UIDNA_ERROR_LEADING_HYPHEN|Uts46.UIDNA_ERROR_TRAILING_HYPHEN },
      { "a.bc--de.f", "B", "a.bc--de.f", Uts46.UIDNA_ERROR_HYPHEN_3_4 },
      { "\u00E4.\u00AD.c", "B", "\u00E4..c", Uts46.UIDNA_ERROR_EMPTY_LABEL },
      { "\u00E4.-b.", "B", "\u00E4.-b.", Uts46.UIDNA_ERROR_LEADING_HYPHEN },
      { "\u00E4.b-.c", "B", "\u00E4.b-.c", Uts46.UIDNA_ERROR_TRAILING_HYPHEN },
      { "\u00E4.-.c", "B", "\u00E4.-.c", Uts46.UIDNA_ERROR_LEADING_HYPHEN|Uts46.UIDNA_ERROR_TRAILING_HYPHEN },
      { "\u00E4.bc--de.f", "B", "\u00E4.bc--de.f", Uts46.UIDNA_ERROR_HYPHEN_3_4 },
      { "a.b.\u0308c.d", "B", "a.b.\uFFFDc.d", Uts46.UIDNA_ERROR_LEADING_COMBINING_MARK },
      { "a.b.xn--c-bcb.d", "B", "a.b.xn--c-bcb\uFFFD.d", Uts46.UIDNA_ERROR_LEADING_COMBINING_MARK },
      // BiDi
      { "A0", "B", "a0", 0 },
      { "0A", "B", "0a", 0 },  // all-LTR is ok to start with a digit (EN)
      { "0A.\u05D0", "B",  // ASCII label does not start with L/R/AL
      "0a.\u05D0", Uts46.UIDNA_ERROR_BIDI },
      { "c.xn--0-eha.xn--4db", "B",  // 2nd label does not start with L/R/AL
      "c.0\u00FC.\u05D0", Uts46.UIDNA_ERROR_BIDI },
      { "b-.\u05D0", "B",  // label does not end with L/EN
      "b-.\u05D0", Uts46.UIDNA_ERROR_TRAILING_HYPHEN|Uts46.UIDNA_ERROR_BIDI },
      { "d.xn----dha.xn--4db", "B",  // 2nd label does not end with L/EN
      "d.\u00FC-.\u05D0", Uts46.UIDNA_ERROR_TRAILING_HYPHEN|Uts46.UIDNA_ERROR_BIDI },
      { "a\u05D0", "B", "a\u05D0", Uts46.UIDNA_ERROR_BIDI },  // first dir != last dir
      { "\u05D0\u05C7", "B", "\u05D0\u05C7", 0 },
      { "\u05D09\u05C7", "B", "\u05D09\u05C7", 0 },
      { "\u05D0a\u05C7", "B", "\u05D0a\u05C7", Uts46.UIDNA_ERROR_BIDI },  // first dir != last dir
      { "\u05D0\u05EA", "B", "\u05D0\u05EA", 0 },
      { "\u05D0\u05F3\u05EA", "B", "\u05D0\u05F3\u05EA", 0 },
      { "a\u05D0Tz", "B", "a\u05D0tz", Uts46.UIDNA_ERROR_BIDI },  // mixed dir
      { "\u05D0T\u05EA", "B", "\u05D0t\u05EA", Uts46.UIDNA_ERROR_BIDI },  // mixed dir
      { "\u05D07\u05EA", "B", "\u05D07\u05EA", 0 },
      { "\u05D0\u0667\u05EA", "B", "\u05D0\u0667\u05EA", 0 },  // Arabic 7 in the middle
      { "a7\u0667z", "B", "a7\u0667z", Uts46.UIDNA_ERROR_BIDI },  // AN digit in LTR
      { "\u05D07\u0667\u05EA", "B",  // mixed EN/AN digits in RTL
      "\u05D07\u0667\u05EA", Uts46.UIDNA_ERROR_BIDI },
      // ZWJ
      { "\u0BB9\u0BCD\u200D", "N", "\u0BB9\u0BCD\u200D", 0 },  // Virama+ZWJ
      { "\u0BB9\u200D", "N", "\u0BB9\u200D", Uts46.UIDNA_ERROR_CONTEXTJ },  // no Virama
      { "\u200D", "N", "\u200D", Uts46.UIDNA_ERROR_CONTEXTJ },  // no Virama
      // ZWNJ
      { "\u0BB9\u0BCD\u200C", "N", "\u0BB9\u0BCD\u200C", 0 },  // Virama+ZWNJ
      { "\u0BB9\u200C", "N", "\u0BB9\u200C", Uts46.UIDNA_ERROR_CONTEXTJ },  // no Virama
      { "\u200C", "N", "\u200C", Uts46.UIDNA_ERROR_CONTEXTJ },  // no Virama
      { "\u0644\u0670\u200C\u06ED\u06EF", "N",  // Joining types D T ZWNJ T R
      "\u0644\u0670\u200C\u06ED\u06EF", 0 },
      { "\u0644\u0670\u200C\u06EF", "N",  // D T ZWNJ R
      "\u0644\u0670\u200C\u06EF", 0 },
      { "\u0644\u200C\u06ED\u06EF", "N",  // D ZWNJ T R
      "\u0644\u200C\u06ED\u06EF", 0 },
      { "\u0644\u200C\u06EF", "N",  // D ZWNJ R
      "\u0644\u200C\u06EF", 0 },
      { "\u0644\u0670\u200C\u06ED", "N",  // D T ZWNJ T
      "\u0644\u0670\u200C\u06ED", Uts46.UIDNA_ERROR_BIDI|Uts46.UIDNA_ERROR_CONTEXTJ },
      { "\u06EF\u200C\u06EF", "N",  // R ZWNJ R
      "\u06EF\u200C\u06EF", Uts46.UIDNA_ERROR_CONTEXTJ },
      { "\u0644\u200C", "N",  // D ZWNJ
          "\u0644\u200C", Uts46.UIDNA_ERROR_BIDI|Uts46.UIDNA_ERROR_CONTEXTJ },
      // { "", "B",
//         "", 0 },
};
}
