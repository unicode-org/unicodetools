package org.unicode.jsptest;

import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UProperty.NameChoice;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.LocaleData;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.VersionInfo;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentest4j.TestAbortedException;
import org.unicode.jsp.CharEncoder;
import org.unicode.jsp.Common;
import org.unicode.jsp.UcdLoader;
import org.unicode.jsp.UnicodeJsp;
import org.unicode.jsp.UnicodeSetUtilities;
import org.unicode.jsp.UnicodeUtilities;
import org.unicode.jsp.XPropertyFactory;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UnicodeProperty;
import org.unicode.text.utility.Settings;

public class TestUnicodeSet extends TestFmwk2 {

    @Test
    public void TestInput() {
        String[][] tests = {
            // loose, strict
            {"U+00A0", "[\u00A0]"},
            {"U+10FFFE..U+10FFFF", "[\\U0010FFFE\\U0010FFFF]"},
            {"a..z", "[a-z]"},
        };
        for (String[] test : tests) {
            UnicodeSet source = UnicodeSetUtilities.parseUnicodeSet(test[0]);
            assertEquals("input unicode set " + test[0], new UnicodeSet(test[1]), source);
        }
    }

    @Test
    public void TestOutput() {
        String[][] tests = {
            // loose, strict
            {"[\u00A0]", "[\\u00A0]", "abb", "esc"},
            {"[{👨\u200D👨\u200D👦}]", "[{👨‍👨‍👦}]"},
            {"[{👨\u200D❤\uFE0F\u200D👨}]", "[{👨‍❤️‍👨}]"},
        };
        assertFalse("", UnicodeUtilities.WHITESPACE_IGNORABLES_C.contains(UnicodeUtilities.JOINER));

        for (String[] test : tests) {
            boolean abbreviate = false, escape = false;
            if (test.length > 3) {
                escape = test[3].startsWith("esc");
            }
            if (test.length > 3) {
                abbreviate = test[2].startsWith("abb");
            }
            String a_out =
                    UnicodeUtilities.getPrettySet(new UnicodeSet(test[0]), abbreviate, escape);

            assertEquals(
                    "input unicode set " + test[0] + ", " + abbreviate + ", " + escape,
                    test[1],
                    a_out);
        }
    }

    @Test
    public void TestEmoji() throws IOException {
        StringBuilder b = new StringBuilder();
        UnicodeJsp.showSet(
                "scx", "", UnicodeSetUtilities.TAKES_EMOJI_VS, false, false, false, false, b);
        String bs = UnicodeUtilities.getPrettySet(UnicodeSetUtilities.FACE, false, false);
        if (bs.contains(" \uFE0F") || bs.contains(" \u200D")) {
            errln("Fails extra-space insert" + bs);
        }
    }

    @EnabledIf(
            value = "org.unicode.unittest.TestFmwkMinusMinus#getRunBroken",
            disabledReason = "Skip unless UNICODETOOLS_RUN_BROKEN_TEST=true")
    @Test
    public void TestPretty() {
        String[] tests = {
            "00A0", "0000",
        };
        for (String test : tests) {
            UnicodeSet source = UnicodeSetUtilities.parseUnicodeSet("[\\u" + test + "]");
            for (boolean abbreviate : new boolean[] {false}) {
                for (boolean escape : new boolean[] {false, true}) {
                    String derived = UnicodeUtilities.getPrettySet(source, abbreviate, escape);
                    UnicodeSet reparsed = new UnicodeSet(derived);
                    if (!assertEquals(
                            "UnicodeSet " + source + ", " + abbreviate + ", " + escape,
                            source,
                            reparsed)) {
                        logln(derived);
                    } else if (!assertTrue("Contains", derived.contains(test))) {
                        logln(derived);
                    }
                }
            }
        }
        String test =
                "[[[:age=4.1:]&"
                        + "[:toNFM!=@toNFKC_CF@:]]-[[:age=4.1:]&"
                        + "[[:dt=circle:]"
                        + "[:dt=sub:]"
                        + "[:dt=super:]"
                        + "[:dt=small:]"
                        + "[:dt=square:]"
                        + "[:dt=vertical:]"
                        + "[[:block=Kangxi_Radicals:]-[:cn:]]"
                        + "[[:toNFKC=/[ ().0-9/°]/:]-[:toNFKC=/^.$/:]]"
                        + "[[:defaultignorablecodepoint:]&[:cn:]]"
                        + "[:block=Hangul Compatibility Jamo:]"
                        + "[[:block=Halfwidth_And_Fullwidth_Forms:]&[:sc=Hang:]]"
                        + "[:block=tags:]"
                        + "]]]";
        UnicodeSet source = UnicodeSetUtilities.parseUnicodeSet(test);
        String derived = UnicodeUtilities.getPrettySet(source, false, false);
        assertTrue("contains 00A0", derived.contains("00A0"));
        logln(derived);
    }

    @Test
    @EnabledIfSystemProperty(
            named = "UNICODETOOLS_TEST_WITH_INCREMENTAL_PROPERTIES",
            matches = ".*",
            disabledReason = "Tests with incremental properties must be run separately")
    public void TestGeneralCategoryGroupingsWithIncrementalProperties() {
        IndexUnicodeProperties.useIncrementalProperties();
        UcdLoader.setOldestLoadedUcd(VersionInfo.UNICODE_10_0);
        checkSetsEqual("[\\p{U10:Lu}\\p{U10:Ll}\\p{U10:Lm}\\p{U10:Lt}\\p{U10:Lo}]", "\\p{U10:L}");
        UcdLoader.setOldestLoadedUcd(Settings.LAST_VERSION_INFO);
    }

    @Test
    public void TestGeneralCategoryGroupings() {
        checkSetsEqual("[\\p{Lu}\\p{Ll}\\p{Lm}\\p{Lt}\\p{Lo}]", "\\p{L}");
        checkSetsEqual("[\\p{Mc}\\p{Me}\\p{Mn}]", "\\p{gc=Combining_Mark}");
    }

    @Test
    public void TestInteriorlyNegatedComparison() {
        checkProperties("\\p{Uppercase≠@Changes_When_Lowercased@}", "[𝕬-𝖅]");
        checkSetsEqual(
                "\\p{Uppercase≠@Changes_When_Lowercased@}",
                "\\P{Uppercase=@Changes_When_Lowercased@}");

        checkSetsEqual(
                "\\p{Is_Uppercase≠@Changes_When_Lowercased@}",
                "[[\\p{Uppercase}\\p{Changes_When_Lowercased}]-[\\p{Uppercase}&\\p{Changes_When_Lowercased}]]");
    }

    @Test
    public void TestNameMatching() {
        // UAX44-LM2 for both Name and Name_Alias.
        checkSetsEqual("\\p{Name=NO-BREAK SPACE}", "[\\xA0]");
        checkSetsEqual("\\p{Name=no break space}", "[\\xA0]");
        checkSetsEqual("\\p{Name=HANGUL JUNGSEONG O-E}", "[\\u1180]");
        checkSetsEqual("\\p{Name=HANGUL JUNGSEONG OE}", "[\\u116C]");
        checkSetsEqual("\\p{Name=Hangul jungseong o-e}", "[\\u1180]");
        checkSetsEqual("\\p{Name=Hangul jungseong oe}", "[\\u116C]");
        checkSetsEqual("\\p{Name=MARCHEN LETTER -A}", "[\\x{11C88}]");
        checkSetsEqual("\\p{Name=MARCHEN LETTER A}", "[\\x{11C8F}]");
        checkSetsEqual("\\p{Name=TIBETAN MARK TSA -PHRU}", "[\\u0F39]");
        checkSetsEqual("\\p{Name=TIBETAN MARK TSA PHRU}", "[]");
        checkSetsEqual("\\p{Name=TIBETAN MARK BKA- SHOG YIG MGO}", "[\\u0F0A]");
        checkSetsEqual("\\p{Name=TIBETAN MARK BKA SHOG YIG MGO}", "[]");
        checkSetsEqual("\\p{Name_Alias=newline}", "[\\x0A]");
        checkSetsEqual("\\p{Name_Alias=NEW LINE}", "[\\x0A]");
        // The medial hyphen is only significant in HANGUL JUNGSEONG O-E, not in arbitrary O-E/OE.
        checkSetsEqual("\\p{Name=twoemdash}", "⸺");
        checkSetsEqual("\\p{Name=SeeNoEvil_Monkey}", "🙈");
        checkSetsEqual("\\p{Name=BALLET S-H-O-E-S}", "🩰");
        checkSetsEqual("[\\p{Name=LATIN SMALL LIGATURE O-E}uf]", "[œuf]");
    }

    @Test
    public void TestNameAliases() {
        // Name_Alias values behave as aliases for Name, but not vice-versa.
        checkSetsEqual(
                "\\p{Name=PRESENTATION FORM FOR VERTICAL RIGHT WHITE LENTICULAR BRAKCET}", "[︘]");
        checkSetsEqual(
                "\\p{Name=PRESENTATION FORM FOR VERTICAL RIGHT WHITE LENTICULAR BRACKET}", "[︘]");
        checkSetsEqual(
                "\\p{Name_Alias=PRESENTATION FORM FOR VERTICAL RIGHT WHITE LENTICULAR BRAKCET}",
                "[]");
        checkSetsEqual(
                "\\p{Name_Alias=PRESENTATION FORM FOR VERTICAL RIGHT WHITE LENTICULAR BRACKET}",
                "[︘]");
        checkProperties("\\p{Name_Alias=@none@}", "[a-z]");
    }

    @Test
    public void TestIdentityQuery() {
        checkSetsEqual("\\p{NFKC_Casefold=@code point@}", "\\P{Changes_When_NFKC_Casefolded}");
        checkSetsEqual("\\p{NFKC_Casefold≠@Code_Point@}", "\\p{Changes_When_NFKC_Casefolded}");
    }

    @Test
    public void TestNullQuery() {
        // Check that we are not falling into the trap described in
        // https://www.unicode.org/reports/tr44/#UAX44-LM3.
        checkProperties("\\p{lb=IS}", "[,.:;]");
        // TODO(egg): This should perhaps be an error. But if it is not an error, it
        // should be empty.
        checkSetsEqual("\\p{lb=@none@}", "[]");
        checkSetsEqual("\\p{Bidi_Paired_Bracket=@none@}", "\\p{Bidi_Paired_Bracket_Type=Is_None}");
        checkSetsEqual("\\p{Bidi_Paired_Bracket≠@None@}", "\\p{Bidi_Paired_Bracket_Type≠None}");
    }

    //    public void TestAExemplars() {
    //        checkProperties("[:exemplars_en:]", "[a]", "[\u0350]");
    //    }

    //    public void TestAEncodings() {
    //        checkProperties("[:isEncSJIS:]", "[\\u00B0]", "[\u0350]");
    //        checkProperties("[:isEncEUCKR:]", "[\\u00B0]", "[\u0350]");
    //    }

    @EnabledIf(
            value = "org.unicode.unittest.TestFmwkMinusMinus#getRunBroken",
            disabledReason = "Skip unless UNICODETOOLS_RUN_BROKEN_TEST=true")
    @Test
    public void TestU60() {
        logln("ICU Version: " + VersionInfo.ICU_VERSION.toString());
        logln("Unicode Data Version:   " + UCharacter.getUnicodeVersion().toString());
        logln("Java Version:   " + System.getProperty("java.version"));
        logln("CLDR Data Version:      " + LocaleData.getCLDRVersion().toString());
        logln("Time Zone Data Version: " + TimeZone.getTZDataVersion());

        UnicodeSet age60 = UnicodeSetUtilities.parseUnicodeSet("[:age=6.0:]");
        UnicodeSet age52 = UnicodeSetUtilities.parseUnicodeSet("[:age=5.2:]");
        assertTrue("6.0 characters", age60.contains(0x20B9));
        logln("New Characters: " + new UnicodeSet(age60).removeAll(age52).toPattern(false));
        assertTrue("6.0 characters", age60.contains(0x20B9));

        UnicodeSet emoji = UnicodeSetUtilities.parseUnicodeSet("[:emoji:]");
        assertEquals("6.0 emoji", 1051, emoji.size()); // really 749, but we flatten the set

        emoji.add(0);
        emoji.remove(0);
        logln(emoji.toString());
    }

    @Test
    public void TestUCA() {
        checkUca("[:uca=0304:]", "[\t]");
        checkUca("[:uca2=05 9E:]", "[Øø]");
        checkUca("[:uca2.5=81 81 01:]", "[ǄǢ]");
        checkUca("[:uca3=05:]", "[a]");
    }

    private void checkUca(String ucaPropValue, String containedItemsString) {
        try {
            UnicodeSet containedItems = new UnicodeSet(containedItemsString);
            UnicodeSet uca = UnicodeSetUtilities.parseUnicodeSet(ucaPropValue);
            assertContains(ucaPropValue, containedItems, uca);
        } catch (Exception e) {
            errln("Can't parse: " + ucaPropValue + "\t" + e.getMessage());
        }
    }

    @EnabledIf(
            value = "org.unicode.unittest.TestFmwkMinusMinus#getRunBroken",
            disabledReason = "Skip unless UNICODETOOLS_RUN_BROKEN_TEST=true")
    @Test
    public void TestICUEnums() {
        UnicodeSet nonchars = UnicodeSetUtilities.parseUnicodeSet("\\p{noncharactercodepoint}");
        assertEquals(
                "Nonchars",
                new UnicodeSet("[:noncharactercodepoint:]").complement().complement(),
                nonchars.complement().complement());

        XPropertyFactory factory = XPropertyFactory.make();
        for (int propEnum = UProperty.INT_START; propEnum < UProperty.INT_LIMIT; ++propEnum) {
            checkProperty(factory, propEnum);
        }
        for (int propEnum = UProperty.BINARY_START; propEnum < UProperty.BINARY_LIMIT; ++propEnum) {
            checkProperty(factory, propEnum);
        }
    }

    private void checkProperty(XPropertyFactory factory, int propEnum) {
        try {
            int min = UCharacter.getIntPropertyMinValue(propEnum);
            int max = UCharacter.getIntPropertyMaxValue(propEnum);
            String propName = UCharacter.getPropertyName(propEnum, NameChoice.SHORT);
            if (propName == null) {
                logln("Skipping name=null for prop number: " + propEnum);
                return;
            }
            UnicodeProperty prop3 = factory.getProperty(propName);
            Set<String> toolValues = new TreeSet<String>(prop3.getAvailableValues());
            logln(propName);
            for (int value = min; value <= max; ++value) {
                UnicodeSet icuSet = new UnicodeSet().applyIntPropertyValue(propEnum, value);
                String valueName =
                        UCharacter.getPropertyValueName(propEnum, value, NameChoice.SHORT);
                if (valueName == null) {
                    valueName = UCharacter.getPropertyValueName(propEnum, value, NameChoice.LONG);
                }
                if (valueName == null) {
                    valueName = String.valueOf(value); // for ccc
                }
                UnicodeSet toolSet = prop3.getSet(valueName);
                try {
                    if (valueName.equals("Sutton_SignWriting")) {
                        int debug = 0;
                    }
                    List<String> namesFound = prop3.getValueAliases(valueName);
                    toolValues.removeAll(namesFound);
                } catch (Exception e) {
                    errln(propName + "=" + valueName + " problem: " + e);
                }
                assertEquals(propName + "=" + valueName, icuSet, toolSet);
            }
            if (propName.equals("gc")) {
                toolValues.removeAll(
                        Arrays.asList(
                                "Cased_Letter, Letter, Mark, Number, Other, Punctuation, Separator, Symbol"
                                        .split(", ")));
            }
            if (!assertEquals(
                    propName + " should have no extra values: ",
                    Collections.EMPTY_SET,
                    toolValues)) {
                int debug = 0;
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("PropEnum: " + propEnum, e);
        }
    }

    //    public void TestEncodingProp() {
    //
    //        XPropertyFactory factory = XPropertyFactory.make();
    //        UnicodeProperty prop = factory.getProperty("enc_Latin1");
    //        UnicodeProperty prop2 = factory.getProperty("enc_Latin2");
    //        UnicodeMap<String> map = prop.getUnicodeMap();
    //        UnicodeMap<String> map2 = prop2.getUnicodeMap();
    //        for (String value : Builder.with(new
    // TreeSet<String>()).addAll(map.values()).addAll(map2.values()).get()) {
    //            logln(value + "\t" + map.getSet(value) + "\t" + map2.getSet(value));
    //        }
    //        UnicodeSet set = UnicodeSetUtilities.parseUnicodeSet("[:enc_Latin1=/61/:]");
    //        assertNotEquals("Latin1", 0, set.size());
    //    }

    public static Stream<Arguments> charsetProvider() {
        final SortedMap<String, Charset> charsets = Charset.availableCharsets();
        final List<Arguments> args = new ArrayList<Arguments>(charsets.size());
        int count = (int) (5 + charsets.size() * getInclusion() / 10.0);
        for (final Map.Entry<String, Charset> e : Charset.availableCharsets().entrySet()) {
            if (--count < 0) break;
            args.add(arguments(e.getKey(), e.getValue()));
        }
        return args.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("charsetProvider")
    public void TestPerMill(final String name, final Charset charset) {
        byte[] dest = new byte[50];
        UnicodeSet values = new UnicodeSet();
        CharEncoder encoder;
        try {
            encoder = new CharEncoder(charset, false, false);
        } catch (UnsupportedOperationException e) {
            // skip charsets that aren't supported
            throw new TestAbortedException("Skipping charset " + charset.name(), e);
        } catch (Exception e) {
            e.printStackTrace();
            assumeTrue(e == null, "Caught exception " + e);
            return; /*NOTREACHED*/
        }

        // first check that we are an ASCII-based encoding, and skip if not
        int len = encoder.getValue(0x61, dest, 0);
        assumeFalse(len != 1 || dest[0] != 0x61, "not ASCII based");

        values.clear();
        byte checkByte = (byte) 0x89;
        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            len = encoder.getValue(cp, dest, 0);
            if (len > 0) {
                for (int j = 0; j < len; ++j) {
                    if (dest[j] == checkByte) {
                        values.add(cp);
                        break;
                    }
                }
            }
        }
        values.remove(0x2030);
        if (values.size() != 0) {
            logln(name + "\tvalues:\t" + values + "\taliases:\t" + charset.aliases());
        }
    }

    @Test
    public void TestGC() {
        Map<String, R2<String, UnicodeSet>> SPECIAL_GC =
                new LinkedHashMap<String, R2<String, UnicodeSet>>();

        String[][] extras = {
            {"Other", "C", "[[:Cc:][:Cf:][:Cn:][:Co:][:Cs:]]"},
            {"Letter", "L", "[[:Ll:][:Lm:][:Lo:][:Lt:][:Lu:]]"},
            {"Cased_Letter", "LC", "[[:Ll:][:Lt:][:Lu:]]"},
            {"Mark", "M", "[[:Mc:][:Me:][:Mn:]]"},
            {"Number", "N", "[[:Nd:][:Nl:][:No:]]"},
            {"Punctuation", "P", "[[:Pc:][:Pd:][:Pe:][:Pf:][:Pi:][:Po:][:Ps:]]"},
            {"Symbol", "S", "[[:Sc:][:Sk:][:Sm:][:So:]]"},
            {"Separator", "Z", "[[:Zl:][:Zp:][:Zs:]]"},
        };

        String[] gcs = {"General_Category=", "", "gc="};
        /*
        gc ; C         ; Other                            # Cc | Cf | Cn | Co | Cs
        gc ; Cc        ; Control                          ; cntrl
        gc ; L         ; Letter                           # Ll | Lm | Lo | Lt | Lu
        gc ; LC        ; Cased_Letter                     # Ll | Lt | Lu
        gc ; M         ; Mark                             # Mc | Me | Mn
        gc ; N         ; Number                           # Nd | Nl | No
        gc ; Nd        ; Decimal_Number                   ; digit
        gc ; P         ; Punctuation                      ; punct                            # Pc | Pd | Pe | Pf | Pi | Po | Ps
        gc ; S         ; Symbol                           # Sc | Sk | Sm | So
        gc ; Z         ; Separator                        # Zl | Zp | Zs
                 */
        for (String[] extra : extras) {
            UnicodeSet expected = UnicodeSetUtilities.parseUnicodeSet(extra[2]).freeze();
            for (String test : extra) {
                if (test.startsWith("[")) continue;
                for (String gc : gcs) {
                    UnicodeSet set = UnicodeSetUtilities.parseUnicodeSet("[:" + gc + test + ":]");
                    assertEquals("Multiprop:\t" + gc + test, expected, set);
                }
            }
        }
        assertEquals(
                "Coverage:\t",
                new UnicodeSet("[:any:]"),
                UnicodeSetUtilities.parseUnicodeSet("[[:C:][:L:][:M:][:N:][:P:][:S:][:Z:]]"));
    }

    @Test
    @Disabled("Stop using ICU for properties: version skew")
    public void TestNF() {
        for (String nf : new String[] {"d", "c", "kd", "kc"}) {
            checkSetsEqual("[:isnf" + nf + ":]", "[:nf" + nf + "qc!=N:]");
            checkSetsEqual("[:isnf" + nf + ":]", "[:tonf" + nf + "=@code point@:]");
        }
    }

    @EnabledIf(
            value = "org.unicode.unittest.TestFmwkMinusMinus#getRunBroken",
            disabledReason = "Skip unless UNICODETOOLS_RUN_BROKEN_TEST=true")
    @Test
    public void TestSets() {

        checkProperties("\\p{tonfkd=/[:alphabetic:]/}", "[:alphabetic:]", "[b]");
        checkProperties("[:toLowercase=a:]", "[Aa]", "[b]");
        checkProperties("[:subhead=/Mayanist/:]", "[\uA726]");

        // checkProperties("[[:script=*latin:]-[:script=latin:]]");
        // checkProperties("[[:script=**latin:]-[:script=latin:]]");
        checkProperties("abc-m", "[d]");

        //        checkProperties("[:usage=common:]", "[9]");

        checkProperties("[:toNFKC=a:]", "[\u00AA]");
        checkProperties("[:isNFC=false:]", "[\u212B]", "[a]");
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
        checkProperties(testString, containsSet, null);
    }

    void checkProperties(String testString, String containsSet, String doesntContainSet) {
        UnicodeSet tc1 = UnicodeSetUtilities.parseUnicodeSet(testString);
        if (containsSet != null) {
            UnicodeSet contains = new UnicodeSet(containsSet);
            if (!tc1.containsAll(contains)) {
                UnicodeSet missing = new UnicodeSet(contains).removeAll(tc1);
                errln(
                        tc1
                                + "\t=\t"
                                + tc1.complement().complement()
                                + "\t\nDoesn't contain "
                                + missing);
            }
        }
        if (doesntContainSet != null) {
            UnicodeSet doesntContain = new UnicodeSet(doesntContainSet);
            if (!tc1.containsNone(doesntContain)) {
                UnicodeSet extra = new UnicodeSet(doesntContain).retainAll(tc1);
                errln(
                        tc1
                                + "\t=\t"
                                + tc1.complement().complement()
                                + "\t\nContains some of"
                                + extra);
            }
        }
    }

    private void checkSetsEqual(String... unicodeSetPatterns) {
        UnicodeSet base = null;
        for (String pattern : unicodeSetPatterns) {
            UnicodeSet current = UnicodeSetUtilities.parseUnicodeSet(pattern);
            if (base == null) {
                base = current;
            } else {
                assertEquals(unicodeSetPatterns[0] + " == " + pattern, base, current);
            }
        }
    }

    @EnabledIf(
            value = "org.unicode.unittest.TestFmwkMinusMinus#getRunBroken",
            disabledReason = "Skip unless UNICODETOOLS_RUN_BROKEN_TEST=true")
    @Test
    public void TestSetSyntax() {
        // System.out.println("Script for A6E6: " + script + ", " + UScript.getName(script) + ", " +
        // script2);
        checkProperties("[:subhead=/Syllables/:]", "[\u1200]");
        // showIcuEnums();
        checkProperties("\\p{ccc:0}", "\\p{ccc=0}", "[\u0308]");
        checkProperties("\\p{isNFC}", "[:ASCII:]", "[\u212B]");
        checkProperties("[:isNFC=no:]", "[\u212B]", "[:ASCII:]");
        checkProperties("[:dt!=none:]&[:toNFD=/^\\p{ccc:0}/:]", "[\u00A0]", "[\u0340]");
        checkProperties("[:toLowercase!=@code point@:]", "[A-Z\u00C0]", "[abc]");
        checkProperties("[:toNfkc!=@toNfc@:]", "[\\u00A0]", "[abc]");

        String trans1 = Common.NFKC_CF.transform("\u2065");
        XPropertyFactory factory = XPropertyFactory.make();
        UnicodeProperty prop = factory.getProperty("tonfkccf");
        String trans2 = prop.getValue('\u2065');
        if (!trans1.equals(trans2)) {
            errln(
                    "mapping of \u2065 "
                            + UCharacter.getName('\u2065')
                            + ","
                            + trans1
                            + ","
                            + trans2);
        }
        checkProperties("[:tonfkccf=/^$/:]", "[:di:]", "[abc]");
        checkProperties("[:ccc=/3/:]", "[\u0308]");
        checkProperties("[:age=3.2:]", "[\u0308]");
        checkProperties("[:alphabetic:]", "[a]");
        checkProperties("[:greek:]", "[\u0370]");
        checkProperties("[:mn:]", "[\u0308]");
        checkProperties("[:sc!=Latn:]", "[\u0308]");
        checkProperties("[:^sc:Latn:]", "[\u0308]");
        checkProperties("[:sc≠Latn:]", "[\u0308]");
        checkSetsEqual("[:sc≠Latn:]", "[:^sc:Latn:]", "[:^sc=Latn:]", "[:sc!=Latn:]");
        checkSetsEqual(
                "[:sc=Latn:]", "[:sc:Latn:]", "[:^sc≠Latn:]", "[:^sc!=Latn:]", "[:^sc!:Latn:]");

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

        checkProperties("\\p{idna2003=disallowed}", "[\\u0001]");
        checkProperties("\\p{idna=valid}", "[\u0308]");
        checkProperties("\\p{uts46=valid}", "[\u0308]");
        checkProperties("\\p{idna2008=disallowed}", "[A]");
    }
}
