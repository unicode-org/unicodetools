// Don't worry about the package, etc. This is just a snippet for your incorporation.
package org.unicode.text.UCD;

import com.ibm.icu.text.Transliterator;

import junit.framework.TestCase;

public class TestAsciify extends TestCase {

  /**
   * The following isn't really part of the test; it is a just a snippet with the rules
   * that would be incorporated into the code.
   * Warning: transliterators need to be synchronized if they are shared across threads.
   */
  Transliterator buildTransliterator(String delimiter) {
    // The rule strings are obscured by the need to use only ASCII in source files instead of UTF-8
    String rules = "$delimiter = '" + delimiter + "';\n"
    + ":: latin; # convert other scripts to Latin\n"
    + ":: nfkd; # separate base letters from remaining accents\n"
    + "# add special rules\n"
    + "# map ring (like a-ring) to 'a'\n"
    + "\\u030A}[:Ll:]   \u2192   a;\n"
    + "[:Lu:]{\\u030A   \u2192   A;\n"
    + "\\u030A   \u2192   a;\n"
    + "# map umlaut (like a-umlaut) to 'e'\n"
    + "\\u0308}[:Ll:]   \u2192   e;\n"
    + "[:Lu:]{\\u0308   \u2192   E;\n"
    + "\\u0308   \u2192   e;\n"
    + "# individual cases\n"
    + "\u00E6   \u2192   ae ;\n"
    + "\u00C6}[:Ll:]   \u2192   Ae ;\n"
    + "\u00C6   \u2192   AE ;\n"
    + "\u0253   \u2192   b ;\n"
    + "\u0181   \u2192   B ;\n"
    + "\u0110}[:Ll:]   \u2192   Dh ;\n"
    + "\u0110   \u2192   DH ;\n"
    + "\u00F0   \u2192   dh ;\n"
    + "\u00D0   \u2192   D ;\n"
    + "\u018A   \u2192  D ;\n"
    + "\u0111   \u2192   d\u0335 ;\n"
    + "\u0257   \u2192   d\u0314 ;\n"
    + "\u0259   \u2192   e ;\n"
    + "\u018F   \u2192   E ;\n"
    + "\u0127   \u2192   h\u0335 ;\n"
    + "\u0126   \u2192   H\u0335 ;\n"
    + "\u0131   \u2192   i ;\n"
    + "\u0198   \u2192   K ;\n"
    + "\u0199   \u2192   k\u0314 ;\n"
    + "\u0138   \u2192   k ;\n"
    + "\u0142   \u2192   l\u0337 ;\n"
    + "\u0141   \u2192   L\u0337 ;\n"
    + "\u00F8   \u2192   oe ;\n"
    + "\u00D8}[:Ll:]   \u2192   Oe ;\n"
    + "\u00D8   \u2192   OE ;\n"
    + "\u0153   \u2192   oe ;\n"
    + "\u0152}[:Ll:]   \u2192   Oe ;\n"
    + "\u0152   \u2192   OE ;\n"
    + "\u00DF   \u2192   ss ;\n"
    + "\u1E9E}[:Ll:]   \u2192   Ss ; # won't ever occur, but just in case\n"
    + "\u1E9E   \u2192   SS ;\n"
    + "\u021B   \u2192   \u0163 ;\n"
    + "\u021A   \u2192   \u0162 ;\n"
    + "\u00FE   \u2192   th ;\n"
    + "\u00DE}[:Ll:]   \u2192   Th ;\n"
    + "\u00DE   \u2192   TH ;\n"
    + ":: [[:m:][:lm:]] remove; # remove accents, 'modifiers'\n"
    + "[^-a-zA-Z0-9\\r\\n]+ > $delimiter; # substituted delimiter for everything else\n"
    + "# note that I have \\r\\n allowed here, for testing\n"
   ;
    return Transliterator.createFromRules("asciify", rules, Transliterator.FORWARD);
  }

  /**
   * These test cases are obscured by the need to use only ASCII in source files instead of UTF-8.
   * These are not real words, just a sampling that were checked to make sure of the expected values.
   * The Japanese one will need to be changed if a different transliteration is used for that.
   * Note that transliterators may change over time as improvements are made, requiring some changes
   * here.
   * I could do a more comprehensive test if needed.
   */
  static String[][] translitTestCases = {
    //{"SAMPLE", "EXPECTED TRANSFORM"},
    {"besk\u00E6ftiger", "beskaeftiger"},
    {"s\u00E5som", "saasom"},
    {"underst\u00F8ttes", "understoettes"},
    {"sprachunabh\u00E4ngig", "sprachunabhaengig"},
    {"herk\u00F6mmlichen", "herkoemmlichen"},
    {"gemeinn\u00FCtzige", "gemeinnuetzige"},
    {"\u00C7'\u00EBsht\u00EB", "C-eeshtee"},
    {"\u0645\u0647\u064A\u0627\u0644\u0634\u0641\u0631\u0629\u0627\u0644\u0645\u0648\u062D\u062F\u0629\u064A\u0648\u0646\u0650\u0643\u0648\u062F\u061F", "mhyalshfrtealmwhdteywnikwd-"},
    {"\u4EC0\u9EBD\u662F", "shen-mo-shi"},
    {"\u4EC0\u4E48\u662F", "shen-me-shi"},
    {"\u0160toje", "Stoje"},
    {"Mik\u00E4on", "Mikaeon"},
    {"Qu'estcequ'", "Qu-estcequ-"},
    {"\u10E0\u10D0\u10D0\u10E0\u10D8\u10E1\u10E3\u10DC\u10D8\u10D9\u10DD\u10D3\u10D8", "raarisunikodi"},
    {"\u03A4\u03B9\u03B5\u03AF\u03BD\u03B1\u03B9\u03C4\u03BF;", "Tieinaito-"},
    {"\u05DE\u05D4\u05D6\u05D4\u05D9\u05D5\u05E0\u05D9\u05E7\u05D5\u05D3", "mhzhywnyqwd"},
    {"\u092F\u0942\u0928\u093F\u0915\u094B\u0921\u0915\u094D\u092F\u093E\u0939\u0948", "yunikodakyahai"},
    {"Hva\u00F0er", "Hvadher"},
    {"Cos'\u00E8", "Cos-e"},
    {"\u30E6\u30CB\u30B3\u30FC\u30C9\u3068\u306F\u4F55\u304B\uFF1F", "yunikodotoha-heka-"},
    {"\uC720\uB2C8\uCF54\uB4DC\uC5D0\uB300\uD574", "yunikodeuedaehae"},
    {"\u0428\u0442\u043E\u0435", "Stoe"},
    {"X'inhul-", "X-inhul-"},
    {"\u064A\u0648\u0646\u06CC\u06A9\u064F\u062F\u0686\u064A\u0633\u062A\u061F", "ywny-udchyst-"},
    {"Oque\u00E9", "Oquee"},
    {"\u0427\u0442\u043E\u0442\u0430\u043A\u043E\u0435", "Ctotakoe"},
    {"\u0160taje", "Staje"},
    {"\u0428\u0442\u0430je", "Staje"},
    {"\u00BFQu\u00E9es", "-Quees"},
    {"Vad\u00E4r", "Vadaer"},
    {"\u0BAF\u0BC2\u0BA9\u0BBF\u0B95\u0BCD\u0B95\u0BCB\u0B9F\u0BC1\u0B8E\u0BA9\u0BCD\u0BB1\u0BBE\u0BB2\u0BCD\u0B8E\u0BA9\u0BCD\u0BA9", "yunikkotu-enralenna"},
    {"\u0E04\u0E37\u0E2D\u0E2D\u0E30\u0E44\u0E23", "khux-xari"},
    {"\u0160toje", "Stoje"},
    {"\uFEF3\uFBD8\uFEE7\uFBE9\uFEDC\uFEEE\uFEA9\uFEA9\uFBE8\uFB95\uFEEA\uFEE5\uFEE7\uFBE9\uFEE4\uFEEA\u061F", "y-nykwddyghnnymh-"},
    {"d\u00E9genn\u00E9me", "degenneme"},
    {"l\u00E0g\u00EC", "lagi"},
  };

  /**
   * Check that all of the test cases work.
   */
  public void testBasic() {
    Transliterator asciify = buildTransliterator("-");
    for (String[] pair : translitTestCases) {
      String actual = asciify.transform(pair[0]);
      String expected = pair[1];
      assertEquals(actual, expected);
    }
  }
}
