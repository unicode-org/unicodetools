package org.unicode.unittest;

import com.google.common.base.Splitter;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.IDNA;
import com.ibm.icu.text.IDNA.Info;
import com.ibm.icu.text.UnicodeSet;
import java.io.File;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.unicode.idna.GenerateIdna;
import org.unicode.idna.LoadIdnaTest;
import org.unicode.idna.LoadIdnaTest.TestLine;
import org.unicode.idna.Uts46;
import org.unicode.idna.Uts46.Errors;
import org.unicode.idna.Uts46.IdnaChoice;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Bidi_Class_Values;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.props.UcdPropertyValues.Idn_Status_Values;
import org.unicode.text.utility.Settings;

public class TestIdnaTest extends TestFmwkMinusMinus {
    public static String TEST_DIR;
    public static Set<TestLine> loadedTests;

    static final IDNA icuUts46T =
            IDNA.getUTS46Instance(IDNA.USE_STD3_RULES | IDNA.CHECK_BIDI | IDNA.CHECK_CONTEXTJ);
    static final IDNA icuUts46N =
            IDNA.getUTS46Instance(
                    IDNA.USE_STD3_RULES
                            | IDNA.CHECK_BIDI
                            | IDNA.CHECK_CONTEXTJ
                            | IDNA.NONTRANSITIONAL_TO_ASCII);

    @BeforeAll
    public static void init() {
        TEST_DIR = System.getProperty("DIR");
        if (TEST_DIR == null) {
            TEST_DIR = Settings.UnicodeTools.getDataPathStringForLatestVersion("idna");
        } else if (TEST_DIR.equalsIgnoreCase("DRAFT")) {
            TEST_DIR = GenerateIdna.GEN_IDNA_DIR;
        }
        loadedTests = LoadIdnaTest.load(TEST_DIR);

        iup = IndexUnicodeProperties.make(Settings.latestVersion);
        BIDI_CLASS = iup.loadEnum(UcdProperty.Bidi_Class, Bidi_Class_Values.class);
    }

    @Test
    public void testLoadedTests() {
        Assertions.assertNotNull(loadedTests, "make sure loadedTests is OK");
        Assertions.assertTrue(new File(TEST_DIR).isDirectory());
    }

    static IndexUnicodeProperties iup;
    static UnicodeMap<Bidi_Class_Values> BIDI_CLASS;

    @Test
    public void testBackwardsCompatibility() {
        UnicodeMap<String> idnaMapping = iup.load(UcdProperty.Idn_Mapping);
        UnicodeMap<Idn_Status_Values> idnaStatus =
                iup.loadEnum(UcdProperty.Idn_Status, Idn_Status_Values.class);

        IndexUnicodeProperties iupLast = IndexUnicodeProperties.make(Settings.lastVersion);
        UnicodeMap<String> idnaMappingLast = iupLast.load(UcdProperty.Idn_Mapping);
        UnicodeMap<Idn_Status_Values> idnaStatusLast =
                iupLast.loadEnum(UcdProperty.Idn_Status, Idn_Status_Values.class);

        UnicodeMap<General_Category_Values> gcOld =
                iupLast.loadEnum(UcdProperty.General_Category, General_Category_Values.class);
        UnicodeSet oldAssigned =
                new UnicodeSet(gcOld.getSet(General_Category_Values.Unassigned))
                        .complement()
                        .freeze();

        String versionString =
                " expected=v" + Settings.latestVersion + " actual=v" + Settings.lastVersion;
        for (String x : oldAssigned) {
            char c0 = x.charAt(0);
            {
                // Unicode 15.1 changes the mapping of capital sharp s:
                // Used to map to ss, now to lowercase sharp s.
                boolean skip = c0 == 0x1E9E;
                if (!skip) {
                    assertEquals(
                            "mapping" + versionString, idnaMappingLast.get(x), idnaMapping.get(x));
                }
            }
            {
                Idn_Status_Values lastStatus = idnaStatusLast.get(x);
                Idn_Status_Values currentStatus = idnaStatus.get(x);
                // Unicode 15.1 changes these from disallowed_STD3_valid to valid.
                boolean skip =
                        (c0 == 0x2260 || c0 == 0x226E || c0 == 0x226F)
                                && currentStatus == Idn_Status_Values.valid;
                if (!skip) {
                    assertEquals("status" + versionString, lastStatus, currentStatus);
                }
            }
        }
    }

    public static final Splitter semi = Splitter.on(';').trimResults();

    static final String[] tests = {
        "B; xn--fa-hia.de;  faß.de; xn--fa-hia.de",
        "B; 0à.\u05D0;  [B1];   [B1]    #   0à.א",
        "B; à.\u05D00\u0660\u05D0;  [B4];   [B4]    #   à.א0٠א",
        "T; \u200D。。\u06B9\u200C;   [B1 B3 C1 C2 A4_2]; [A4_2]  #   ..ڹ",
        "N;  \u200D。。\u06B9\u200C;   [B1 B3 C1 C2 A4_2]; [B1 B3 C1 C2 A4_2]  #   ..ڹ"
    };

    @Disabled("Broken")
    @Test
    public void testBroken() {
        for (String test : tests) {
            TestLine tl = TestLine.from(test);
            checkTestLine(tl, Choice.GEN);
        }
    }

    @Disabled("Broken")
    @Test
    public void testBrokenICU() {
        for (String test : tests) {
            TestLine tl = TestLine.from(test);
            checkTestLine(tl, Choice.ICU);
        }
    }

    /*
    #  Column 1: type - T for transitional, N for nontransitional, B for both
    #  Column 2: source - the source string to be tested
    #  Column 3: toUnicode - the result of applying toUnicode to the source, using nontransitional. A blank value means the same as the source value.
    #  Column 4: toASCII - the result of applying toASCII to the source, using the specified type. A blank value means the same as the toUnicode value.
    #  Column 5: NV8 - present if the toUnicode value would not be a valid domain name under IDNA2008. Not a normative field.
         */

    enum Choice {
        ICU,
        GEN
    }

    private void checkTestLine(TestLine tl, Choice choice) {
        String detailedSource = showBidi(tl.source);
        if (tl.source.equals("0à.\u05D0")) {
            int debug = 0;
        }
        final Set<Errors> toUnicodeErrors = EnumSet.noneOf(Errors.class);
        final String toUnicode =
                Uts46.SINGLETON.toUnicode(tl.source, IdnaChoice.nontransitional, toUnicodeErrors);
        Info info = new Info();
        StringBuilder buffer = new StringBuilder();
        icuUts46N.nameToASCII(tl.source, buffer, info);

        String lead = "toUnicode(";
        if (tl.toUnicodeErrors.isEmpty()) {
            if (!assertEquals(lead + detailedSource + ")", tl.toUnicode, toUnicode)) {
                int debug = 0;
            }
        } else {
            if (!assertEquals(lead + detailedSource + ")", tl.toUnicodeErrors, toUnicodeErrors)) {
                int debug = 0;
            }
        }

        checkAscii(tl, IdnaChoice.transitional, detailedSource);
        checkAscii(tl, IdnaChoice.nontransitional, detailedSource);
    }

    private void checkAscii(TestLine tl, IdnaChoice idnaChoice, String detailedSource) {
        final Set<Errors> toAsciiErrors = EnumSet.noneOf(Errors.class);
        final String toAscii = Uts46.SINGLETON.toASCII(tl.source, idnaChoice, toAsciiErrors);
        String f;
        switch (idnaChoice) {
            case transitional:
                f = " toAsciiT(";
                break;
            case nontransitional:
                f = " toAsciiN(";
                break;
        }
        //        if (tl.toAsciiErrors.isEmpty()) {
        //            if (!assertEquals(f + detailedSource + ")", tl.toAscii, toAscii))  {
        //                int debug = 0;
        //            }
        //        } else {
        //            if (!assertEquals(f + detailedSource + ")", tl.toAsciiErrors, toAsciiErrors))
        // {
        //                int debug = 0;
        //            }
        //        }
    }

    static final char LRM = '\u200E';

    private String showBidi(String source) {
        StringBuilder b = new StringBuilder();
        for (int cp : CharSequences.codePoints(source)) {
            if (b.length() != 0) {
                b.append(' ');
            }
            b.append(LRM)
                    .appendCodePoint(cp)
                    .append(LRM)
                    .append('[')
                    .append(Utility.hex(cp))
                    .append('/')
                    .append(BIDI_CLASS.get(cp).getShortName())
                    .append(']');
        }
        return b.toString();
    }

    /**
     * An RTL label is a label that contains at least one character of type R, AL, or AN.
     *
     * <p>An LTR label is any label that is not an RTL label.
     *
     * <p>The following rule, consisting of six conditions, applies to labels in Bidi domain names.
     * The requirements that this rule satisfies are described in Section 3. All of the conditions
     * must be satisfied for the rule to be satisfied.
     *
     * <p>1. The first character must be a character with Bidi property L, R, or AL. If it has the R
     * or AL property, it is an RTL label; if it has the L property, it is an LTR label.
     *
     * <p>2. In an RTL label, only characters with the Bidi properties R, AL, AN, EN, ES, CS, ET,
     * ON, BN, or NSM are allowed.
     *
     * <p>3. In an RTL label, the end of the label must be a character with Bidi property R, AL, EN,
     * or AN, followed by zero or more characters with Bidi property NSM.
     *
     * <p>4. In an RTL label, if an EN is present, no AN may be present, and vice versa.
     *
     * <p>5. In an LTR label, only characters with the Bidi properties L, EN, ES, CS, ET, ON, BN, or
     * NSM are allowed.
     *
     * <p>6. In an LTR label, the end of the label must be a character with Bidi property L or EN,
     * followed by zero or more characters with Bidi property NSM.
     */
    @Test
    public void testFinalDot() {
        String source = "a.b．c。d｡";
        String expected = "a.b.c.d.";
        final Set<Errors> toUnicodeErrors = new LinkedHashSet<Errors>();
        final String actual =
                Uts46.SINGLETON.toUnicode(source, IdnaChoice.nontransitional, toUnicodeErrors);
        assertEquals("toUnicode(" + source + "):", expected, actual);
    }

    @Disabled("Broken")
    @Test
    public void testFile() {
        for (TestLine testLine : loadedTests) {
            checkTestLine(testLine, Choice.GEN);
        }
    }

    @Disabled("Broken")
    @Test
    public void testFileICU() {
        for (TestLine testLine : loadedTests) {
            checkTestLine(testLine, Choice.ICU);
        }
    }
}
