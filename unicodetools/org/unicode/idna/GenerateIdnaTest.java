package org.unicode.idna;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.idna.Idna2008.Idna2008Type;
import org.unicode.idna.Uts46.Errors;
import org.unicode.idna.Uts46.IdnaChoice;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.ToolUnicodePropertySource;
import org.unicode.text.UCD.ToolUnicodeTransformFactory;
import org.unicode.text.utility.Settings;

import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.FileUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeProperty;
import com.ibm.icu.dev.util.UnicodePropertySymbolTable;
import com.ibm.icu.dev.util.UnicodeTransform;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.XSymbolTable;
import com.ibm.icu.util.ULocale;

public class GenerateIdnaTest {
    static {
        // MUST BE FIRST
        GenerateIdnaTest.setUnicodeVersion();
    }

    private static final Pattern IDNA2003_LABEL_SEPARATOR = Pattern.compile("[.\uFF0E \u3002\uFF61]");
    private static final boolean NEW_FORMAT = true;

    public static void main(String[] args) throws IOException {
        final int count = new GenerateIdnaTest().generateTests(1000);
        System.out.println("DONE " + count);
    }

    public static void setUnicodeVersion() {
        Default.setUCD(Settings.latestVersion);
        UnicodeTransform.setFactory(new ToolUnicodeTransformFactory());
        final ToolUnicodePropertySource toolUPS1 = ToolUnicodePropertySource.make(Default.ucdVersion());
        final XSymbolTable toolUPS = new UnicodePropertySymbolTable(toolUPS1);
        UnicodeSet.setDefaultXSymbolTable(toolUPS);
        UnicodeProperty.ResetCacheProperties();
    }

    public static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'GMT'", ULocale.US);

    int generateTests(int lines) throws IOException {
        final String filename = "IdnaTest.txt";
        final PrintWriter out = BagFormatter.openUTF8Writer(GenerateIdna.DIR, filename);
        out.println("# " + "IdnaTest"  + Default.ucdVersion() +  ".txt" + "\n" +
                "# Date: " + dateFormat.format(new Date()) + " [MD]\n" +
                "#\n" +
                "# Copyright (c) 1991-" + (new Date().getYear()+1900) +
                " Unicode, Inc.");

        FileUtilities.appendFile(this.getClass().getResource("IdnaTestHeader.txt").toString().substring(5), "UTF-8", out);
        //        out.println(
        //                "# Format\n" +
        //                "# source ; type ; toASCII ; toUnicode\n" +
        //                "# type: T for transitional, N for nontransitional, B for both\n" +
        //                "# In case of errors, field 3 and 4 show errors in [....] instead of a result\n" +
        //                "# The errors are based on the step numbers in UTS46.\n" +
        //                "# Pn for Section 4 Processing step n\n" +
        //                "# Vn for 4.1 Validity Criteria step n\n" +
        //                "# An for 4.2 ToASCII step n\n" +
        //                "# Bn for Bidi (in IDNA2008)\n" +
        //                "# Cn for ContextJ (in IDNA2008)\n" +
        //                "\n"
        //        );
        int count = 0;

        count += generateLine("fass.de", out);
        count += generateLine("faß.de", out);

        out.println("\n# BIDI TESTS\n");


        for (final String[] testCase : bidiTests) {
            count += generateLine(testCase[0], out);
        }

        out.println("\n# CONTEXT TESTS\n");


        for (final String[] testCase : contextTests) {
            count += generateLine(testCase[0], out);
        }

        out.println("\n# SELECTED TESTS\n");


        count += generateLine("\u00a1", out);
        for (String s : Idna2008.GRANDFATHERED_VALID) {
            count += generateLine(s, out);
        }

        for (final Object[] testCaseLine : testCases) {
            final String source = testCaseLine[0].toString();
            count += generateLine(source, out);
        }

        out.println("\n# RANDOMIZED TESTS\n");


        final RandomString randomString = new RandomString();
        final char[] LABELSEPARATORS = {'\u002E', '\uFF0E', '\u3002', '\uFF61'};
        final StringBuilder sb = new StringBuilder();

        final int labelLength = NEW_FORMAT ? 2 : 3;

        for (int line = 0; line < lines; ++line) {
            sb.setLength(0);
            randomString.resetRandom(line); // provide predictable results based on line number
            // random number of labels
            int labels = RandomString.random.nextInt(labelLength);
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

    int generateLine(String source, PrintWriter out) {
        if (source.contains("🌱")) {
            int debug = 0;
        }
        if (alreadyDone(source)) {
            return 0;
        }
        int result = 0;
        final Set<Errors> transitionalErrors = new LinkedHashSet<Errors>();
        final Set<Errors> nonTransitionalErrors = new LinkedHashSet<Errors>();
        final String transitional = Uts46.SINGLETON.toASCII(source, IdnaChoice.transitional, transitionalErrors);
        final String nontransitional = Uts46.SINGLETON.toASCII(source, IdnaChoice.nontransitional, nonTransitionalErrors);
        final Set<Errors> toUnicodeErrors = new LinkedHashSet<Errors>();
        final String unicode = Uts46.SINGLETON.toUnicode(source, IdnaChoice.nontransitional, toUnicodeErrors);

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
        if (NEW_FORMAT) {
            result += generateLine(Idna.NFC.transform(source), out);
            result += generateLine(Idna.NFD.transform(source), out);
            result += generateLine(Idna.NFKC.transform(source), out);
            result += generateLine(Idna.NFKD.transform(source), out);
        }
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

    Set<String> alreadySeen = new HashSet<String>();

    private boolean alreadyDone(String source) {
        if (alreadySeen.contains(source)) {
            return true;
        }
        final String canonical = getCanonical(source);
        if (alreadySeen.contains(canonical)) {
            return true;
        }
        alreadySeen.add(source);
        alreadySeen.add(canonical);
        return false;
    }

    Matcher labelSeparator = IDNA2003_LABEL_SEPARATOR.matcher("");
    // we lowercase all ascii labels, and otherwise leave as is.
    String getCanonical(String source) {
        final StringBuilder result = new StringBuilder();
        final Matcher m = labelSeparator.reset(source);
        for (final String label : Regexes.split(m, source, true)) {
            if (IdnaTypes.LABEL_ASCII.containsAll(label)) {
                final String folded = Idna.CASEFOLD.transform(label);
                result.append(folded);
                continue;
            }
            result.append(label);
        }
        return result.toString();
    }

    /**
     * Draws line
     */
    private void showLine(String source, String type, String ascii, Set<Errors> asciiErrors, String unicode, Set<Errors> toUnicodeErrors, PrintWriter out) {
        final String unicodeReveal = hexForTest.transform(unicode);
        final boolean hasUnicodeErrors = toUnicodeErrors.size() != 0;
        final boolean hasAsciiErrors = asciiErrors.size() != 0;
        final Set<Errors> extraErrors = EnumSet.noneOf(Errors.class);
        final boolean validIdna2008 = IDNA2008Valid.containsAll(unicode) && Uts46.hasBidiOrContextError(unicode, extraErrors ) == 0;
        out.println(type
                + ";\t"
                + hexForTest.transform(source)
                + ";\t"
                + (hasUnicodeErrors ? showErrors(toUnicodeErrors) : unicode.equals(source) ? "" : unicodeReveal)
                + ";\t"
                + (hasAsciiErrors ? showErrors(asciiErrors) : unicode.equals(ascii) ? "" :  hexForTest.transform(ascii))
                + (Idna2008.GRANDFATHERED_VALID.containsSome(unicode) ? ";\tXV8" 
                        : hasUnicodeErrors || validIdna2008 ? "" :  ";\tNV8") // checking
                        + (!NEW_FORMAT ? "" : ""
                                + (unicodeReveal.equals(unicode) ? "" : "\t#\t" + unicode))
                );
    }

    static class RandomString {
        static Random random = new Random(0);
        static UnicodeSet[] sampleSets;
        static {
            final String[] samplesNew = {
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
            final String[] samplesOld = {
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
                    "[\\u200C\\u200D\\u00DF\\u03C2]", // deviations
            };
            final String[] samples = NEW_FORMAT ? samplesNew : samplesOld;
            // OLD B;   \u063D\uD803\uDDD6\u1039;   [P1 V6];    [P1 V6]
            // NEW B;   \u063D\uFBB0\u0BCD⁰．\uDB40\uDDD6\uD803\uDE71;   [B1];   [B1]

            sampleSets = new UnicodeSet[samples.length];
            //UnicodeSet age = new UnicodeSet("[:age=6.0:]");
            for (int i = 0; i < samples.length; ++i) {
                sampleSets[i] = new UnicodeSet(samples[i])
                //                .retainAll(age)
                .freeze();
            }
        }
        void appendNext(StringBuilder sb) {
            int len = 1 + random.nextInt(NEW_FORMAT ? 4 : 7);
            // random contents, picking from random set
            for (; len > 0; --len) {
                final int setNum = random.nextInt(sampleSets.length);
                final UnicodeSet uset = sampleSets[setNum];
                final int size = uset.size();
                final int indexInSet = random.nextInt(size);
                final int cp = uset.charAt(indexInSet);
                sb.appendCodePoint(cp);
            }
        }
        void resetRandom(long seed) {
            random.setSeed(seed);
        }
    }

    private String showErrors(Set<Errors> errors) {
        return "[" + CollectionUtilities.join(errors, " ") + "]";
    }

    public static UnicodeSet getIdna2008Valid() {
        //    IdnaLabelTester tester = getIdna2008Tester();
        //    UnicodeSet valid2008 = UnicodeSetUtilities.parseUnicodeSet(tester.getVariable("$Valid"), TableStyle.simple);
        //    return valid2008;
        final UnicodeMap<Idna2008Type> typeMapping = Idna2008.getTypeMapping();
        return new UnicodeSet(typeMapping.getSet(Idna2008Type.PVALID))
        .addAll(typeMapping.getSet(Idna2008Type.CONTEXTJ))
        .addAll(typeMapping.getSet(Idna2008Type.CONTEXTO))
        ;
    }

    Transliterator hexForTest = Transliterator.getInstance("[[:c:][:z:][:m:][:di:][:bc=R:][:bc=AL:][:bc=AN:]] any-hex");
    static UnicodeSet IDNA2008Valid = new UnicodeSet(getIdna2008Valid()).add('.').freeze();

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

    public static String[][] bidiTests = {
        {"à" + SAMPLE_R_AL, "B5", "B6"},
        {"0à." + SAMPLE_R_AL,"B1"},
        {"à." + SAMPLE_R_AL + SAMPLE_NSM},
        {"à." + SAMPLE_R_AL + SAMPLE_EN + SAMPLE_AN + SAMPLE_R_AL, "B4"},
        {SAMPLE_NSM + "." + SAMPLE_R_AL + "","B3"},
        {"à." + SAMPLE_R_AL + "0" + SAMPLE_AN,"B4"},
        {"à" + SAMPLE_ES_CS_ET_ON_BN + "." + SAMPLE_R_AL + "","B6"},
        {"à" + SAMPLE_NSM + "." + SAMPLE_R_AL + ""},
    };

    public static String[][] contextTests = new String[][] {
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

    public static final Object[][] testCases = {
        { "。", "B",  // special case
            "。", 0 },
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
                                                                                                                                                                                                                                                                                                                                                                                                                                    //               "", 0 },
    };

}
