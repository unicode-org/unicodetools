package org.unicode.idna;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.TransliteratorUtilities;
import org.unicode.cldr.util.props.UnicodeProperty;
import org.unicode.cldr.util.props.UnicodePropertySymbolTable;
import org.unicode.idna.Idna2008.Idna2008Type;
import org.unicode.idna.LoadIdnaTest.TestLine;
import org.unicode.idna.Uts46.Errors;
import org.unicode.idna.Uts46.IdnaChoice;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.props.UcdPropertyValues.Idn_Status_Values;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.ToolUnicodePropertySource;
import org.unicode.text.UCD.ToolUnicodeTransformFactory;
import org.unicode.text.UCD.UCD_Types;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.UnicodeTransform;
import org.unicode.text.utility.Utility;

import com.google.common.base.Objects;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.locale.XCldrStub.Splitter;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.XSymbolTable;
import com.ibm.icu.util.ULocale;

public class GenerateIdnaTest {
    private static final boolean MATCH_OLD = true;

    static {
        // MUST BE FIRST
        GenerateIdnaTest.setUnicodeVersion();
    }

    private static final String TO_ESCAPE = "[[:c:][:z:][:m:][:di:][:bc=R:][:bc=AL:][:bc=AN:]#;]";

    private static final Pattern IDNA2003_LABEL_SEPARATOR = Pattern.compile("[.\uFF0E \u3002\uFF61]");
    private static final boolean NEW_FORMAT = true;
    private static final int UNDEFINED;

    
    static {
        // find a character that is likely to remain undefined, and is if possible in the BMP.
        // so we take the highest BMP if possible, then the highest smp
        UnicodeSet unassigned = new UnicodeSet("[[:cn:]-[:NChar:]]").freeze();
        UnicodeSet unassignedBMP = new UnicodeSet("[\\u0000-\\uFFFF]").retainAll(unassigned);
        if (!unassignedBMP.isEmpty()) {
            UNDEFINED = unassignedBMP.getRangeEnd(unassignedBMP.getRangeCount()-1);
        } else {
            UNDEFINED = unassigned.getRangeEnd(unassigned.getRangeCount()-1);
        }
    }

    public static void main(String[] args) throws IOException {
        final int count = new GenerateIdnaTest().generateTests(1000);
        System.out.println("DONE " + count);
        System.out.println("NOTE: use ICU to test until TestIdna is updated."
                + "\nCopy the new data to {workspace}/unicodetools/data/idna and run TestIdna -prop:DIR=draft");
    }

    //    private static <T> void assertEquals(String string, T expected, T actual) {
    //        if (!Objects.equal(expected, actual)) {
    //            throw new IllegalArgumentException(string + ": expected: " + expected + " ≠ " + actual);
    //        }
    //    }

    public static void setUnicodeVersion() {
        Default.setUCD(Settings.latestVersion);
        UnicodeTransform.setFactory(new ToolUnicodeTransformFactory());
        final ToolUnicodePropertySource toolUPS1 = ToolUnicodePropertySource.make(Default.ucdVersion());
        final XSymbolTable toolUPS = new UnicodePropertySymbolTable(toolUPS1);
        UnicodeSet.setDefaultXSymbolTable(toolUPS);
        UnicodeProperty.ResetCacheProperties();
    }

    public static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'GMT'", ULocale.US);
    public static String internalOldName = "internal-IdnaTest.txt";
    public static String NEW_FILE_NAME = MATCH_OLD ? "IdnaTestV2.txt" : "internal-IdnaTestV2-fixed.txt";

    int generateTests(int lines) throws IOException {
        final PrintWriter out = org.unicode.cldr.draft.FileUtilities.openUTF8Writer(GenerateIdna.DIR, internalOldName);
        out.println(Utility.getDataHeader(internalOldName));

        FileUtilities.appendFile(this.getClass().getResource("IdnaTestHeader.txt").toString().substring(5), "UTF-8", out);

        final PrintWriter out2 = org.unicode.cldr.draft.FileUtilities.openUTF8Writer(GenerateIdna.DIR, NEW_FILE_NAME);
        out2.println(Utility.getDataHeader(NEW_FILE_NAME));

        FileUtilities.appendFile(this.getClass().getResource("IdnaTestHeader2.txt").toString().substring(5), "UTF-8", out2);

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

        count += generateLine("fass.de", out, out2);
        count += generateLine("faß.de", out, out2);

        out.println("\n# BIDI TESTS\n");
        out2.println("\n# BIDI TESTS\n");


        for (final String[] testCase : bidiTests) {
            count += generateLine(testCase[0], out, out2);
        }

        out.println("\n# CONTEXT TESTS\n");
        out2.println("\n# CONTEXT TESTS\n");


        for (final String[] testCase : contextTests) {
            count += generateLine(testCase[0], out, out2);
        }

        out.println("\n# SELECTED TESTS\n");
        out2.println("\n# SELECTED TESTS\n");


        count += generateLine("\u00a1", out, out2);
        for (String s : Idna2008.GRANDFATHERED_VALID) {
            count += generateLine(s, out, out2);
        }

        for (final Object[] testCaseLine : testCases) {
            final String source = testCaseLine[0].toString();
            count += generateLine(source, out, out2);
        }

        out.println("\n# RANDOMIZED TESTS\n");
        out2.println("\n# RANDOMIZED TESTS\n");

        Set<TestLine> testLines = LoadIdnaTest.load(Settings.UNICODETOOLS_DIRECTORY + "data/idna/9.0.0");

        for (TestLine testLine : testLines) {
            count += generateLine(replaceNewerThan90(testLine.source), out, out2);
        }

        //        final RandomString randomString = new RandomString();
        //        final char[] LABELSEPARATORS = {'\u002E', '\uFF0E', '\u3002', '\uFF61'};
        //        final StringBuilder sb = new StringBuilder();
        //
        //        final int labelLength = NEW_FORMAT ? 2 : 3;
        //
        //        for (int line = 0; line < lines; ++line) {
        //            sb.setLength(0);
        //            randomString.resetRandom(line); // provide predictable results based on line number
        //            // random number of labels
        //            int labels = RandomString.random.nextInt(labelLength);
        //            for (; labels >= 0; --labels) {
        //                randomString.appendNext(sb);
        //                if (labels != 0) {
        //                    sb.append(LABELSEPARATORS[RandomString.random.nextInt(4)]);
        //                }
        //            }
        //            // random length
        //            count += generateLine(sb.toString(), out);
        //        }

        out.close();
        out2.close();
        return count;
    }

    private String replaceNewerThan90(String source) {
        StringBuilder sb = new StringBuilder();
        for (int cp : CharSequences.codePoints(source)) {
            byte age = Default.ucd().getAge(cp);
            if (age > UCD_Types.AGE90) {
                cp = UNDEFINED;
            }
            sb.appendCodePoint(cp);
        }
        return sb.toString();
    }

    int generateLine(String source, PrintWriter out, PrintWriter out2) {
        if (alreadyDone(source)) {
            return 0;
        }
        if (source.equals("\u200d")) {
            int debug = 0;
        }
        int result = 0;
        final Set<Errors> toUnicodeErrors = EnumSet.noneOf(Errors.class);
        final String unicode = Uts46.SINGLETON.toUnicode(source, IdnaChoice.nontransitional, toUnicodeErrors);
        if (!Collections.disjoint(toUnicodeErrors, Errors.TO_ASCII_ERRORS)) {
            System.err.println("Should never have ASCII errors in toUnicode:\t" + source + "\ty==>\t" + toUnicodeErrors);
        }
        if (MATCH_OLD) {
            replace(toUnicodeErrors, Errors.X4_2, Errors.A4_2);
            replace(toUnicodeErrors, Errors.P4, Errors.A3);
            replace(toUnicodeErrors, Errors.X3, Errors.A3);
        }

        final Set<Errors> transitionalErrors = EnumSet.noneOf(Errors.class);
        final String transitional = Uts46.SINGLETON.toASCII(source, IdnaChoice.transitional, transitionalErrors);
        replace(transitionalErrors, Errors.X4_2, Errors.A4_2);
        if (MATCH_OLD) {
            replace(transitionalErrors, Errors.P4, Errors.A3);
            replace(transitionalErrors, Errors.X3, Errors.A3);
        }

        final Set<Errors> nonTransitionalErrors = EnumSet.noneOf(Errors.class);
        final String nontransitional = Uts46.SINGLETON.toASCII(source, IdnaChoice.nontransitional, nonTransitionalErrors);
        replace(nonTransitionalErrors, Errors.X4_2, Errors.A4_2);
        if (MATCH_OLD) {
            replace(nonTransitionalErrors, Errors.P4, Errors.A3);
            replace(nonTransitionalErrors, Errors.X3, Errors.A3);
        }

//        Set<Errors> toUnicodeErrors2 = toUnicodeErrors;
//        if (!IDNA2008Valid.containsAll(source)) {
//            toUnicodeErrors2 = EnumSet.copyOf(toUnicodeErrors2);
//            toUnicodeErrors2.add(Errors.NV8);
//        }
//        
//        // Hack to check whether problems were introduced. Needs to be deeper check in processMap
//        
//        final Set<Errors> throwAway = EnumSet.noneOf(Errors.class);
//        Set<Errors> nonTransitionalErrors2 = nonTransitionalErrors;
//        final String nontransitional2 = Uts46.SINGLETON.toASCII(unicode, IdnaChoice.nontransitional, throwAway);
//        if (!IDNA2008Valid.containsAll(nontransitional2)) {
//            nonTransitionalErrors2 = EnumSet.copyOf(nonTransitionalErrors);
//            nonTransitionalErrors2.add(Errors.NV8);
//        }
//
//        Set<Errors> transitionalErrors2 = transitionalErrors;
//        final String transitional2 = Uts46.SINGLETON.toASCII(unicode, IdnaChoice.transitional, throwAway);
//        if (!IDNA2008Valid.containsAll(transitional2)) {
//            nonTransitionalErrors2 = EnumSet.copyOf(nonTransitionalErrors);
//            nonTransitionalErrors2.add(Errors.NV8);
//        }

        out2.println(source 
                + "; " + CldrUtility.ifEqual(unicode, source, "")
                + "; " + CldrUtility.ifEqual(toUnicodeErrors, Collections.EMPTY_SET, "")
                + "; " + CldrUtility.ifEqual(nontransitional, unicode, "")
                + "; " + CldrUtility.ifEqual(nonTransitionalErrors, toUnicodeErrors, "")
                + "; " + CldrUtility.ifEqual(transitional, nontransitional, "")
                + "; " + CldrUtility.ifEqual(transitionalErrors, nonTransitionalErrors, "")
                + " # " + removeInvisible.transform(unicode)
                );

        if (!transitionalErrors.equals(nonTransitionalErrors)
                || !transitional.equals(nontransitional)
                //&& transitionalErrors.size() == 0
                ) {
            showLine(source, "T", transitional, transitionalErrors, unicode, toUnicodeErrors, out);
            showLine(source, "N", nontransitional, nonTransitionalErrors, unicode, toUnicodeErrors, out);
            result += 2;
        } else {
            showLine(source, "B", nontransitional, nonTransitionalErrors, unicode, toUnicodeErrors, out);
            result += 1;
        }
        if (NEW_FORMAT) {
            result += generateLine(Idna.NFC.transform(source), out, out2);
            result += generateLine(Idna.NFD.transform(source), out, out2);
            result += generateLine(Idna.NFKC.transform(source), out, out2);
            result += generateLine(Idna.NFKD.transform(source), out, out2);
        }
        result += generateLine(UCharacter.toLowerCase(source), out, out2);
        result += generateLine(UCharacter.toUpperCase(source), out, out2);
        result += generateLine(UCharacter.foldCase(source, true), out, out2);
        result += generateLine(UCharacter.toTitleCase(source, null), out, out2);

        //if (transitionalErrors.size() == 0) {
        result += generateLine(transitional, out, out2);
        //}
        //if (nonTransitionalErrors.size() == 0) {
        result += generateLine(nontransitional, out, out2);
        //}
        if (toUnicodeErrors.size() == 0) {
            result += generateLine(unicode, out, out2);
        }
        return result;
    }

    private void replace(final Set<Errors> transitionalErrors, Errors toReplace, Errors replacement) {
        if (transitionalErrors.contains(toReplace)) {
            transitionalErrors.remove(toReplace);
            transitionalErrors.add(replacement);
        }
    }

    Set<String> alreadySeen = new HashSet<String>();

    private boolean alreadyDone(String source) {
        final String canonical = getCanonicalLabel(source);
        if (alreadySeen.contains(canonical)) {
            return true;
        }
        alreadySeen.add(canonical);
        return false;
    }

    Matcher labelSeparator = IDNA2003_LABEL_SEPARATOR.matcher("");
    String getCanonicalString(String source) {
        labelSeparator.reset(source);
        StringBuilder result = new StringBuilder();
        int last = 0;
        while (labelSeparator.find()) {
            result.append(getCanonicalLabel(source.substring(last, labelSeparator.start())));
            result.append(source, labelSeparator.start(), labelSeparator.end());
        }
        result.append(getCanonicalLabel(source.substring(last)));
        return result.toString();

    }
    // we uppercase IF all ascii, otherwise leave it alone
    String getCanonicalLabel(String source) {
        final StringBuilder result = new StringBuilder();
        for (int cp : CharSequences.codePoints(source)) {
            if (cp > 0x7f) {
                return source;
            }
            if (cp >= 'a' && cp <= 'z') {
                cp = cp - 'a' + 'A'; // uppercase
            }
            result.appendCodePoint(cp);
        }
        //        final Matcher m = labelSeparator.reset(source);
        //        for (final String label : labelSeparator.split(source)) {
        //            if (IdnaTypes.LABEL_ASCII.containsAll(label)) {
        //                final String folded = Idna.CASEFOLD.transform(label);
        //                result.append(folded);
        //                continue;
        //            }
        //            result.append(label);
        //        }
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
                + (hasAsciiErrors ? showErrors(asciiErrors) : unicode.equals(ascii) ? "" : hexForTest.transform(ascii))
                + (Idna2008.GRANDFATHERED_VALID.containsSome(unicode) ? ";\tXV8" 
                        : hasUnicodeErrors || validIdna2008 ? "" :  ";\tNV8") // checking
                + (!NEW_FORMAT ? "" : ""
                        + (unicodeReveal.equals(unicode) ? "" : "\t#\t" + removeInvisible.transform(unicode)))
                );
    }

    static class RandomString {
        static Random random = new Random(0);
        static UnicodeSet[] sampleSets;
        static {
            final String[] samplesNew = {
                    //                    // bidi
                    //                    "[[:bc=R:][:bc=AL:]]",
                    //                    "[[:bc=L:]]",
                    //                    "[[:bc=ES:][:bc=CS:][:bc=ET:][:bc=ON:][:bc=BN:][:bc=NSM:]]",
                    //                    "[[:bc=EN:]]",
                    //                    "[[:bc=AN:]]",
                    //                    "[[:bc=NSM:]]",
                    //                    // contextj
                    //                    "[\u200C\u200D]",
                    //                    "[[:ccc=virama:]]",
                    //                    "[[:jt=T:]]",
                    //                    "[[:jt=L:][:jt=D:]]",
                    //                    "[[:jt=R:][:jt=D:]]",
                    //                    // syntax
                    //                    "[-]",
                    //                    // changed mapping from 2003
                    //                    "[\u04C0 \u10A0-\u10C5 \u2132 \u2183 \u2F868 \u2F874 \u2F91F \u2F95F \u2F9BF \u3164 \uFFA0 \u115F \u1160 \u17B4 \u17B5 \u1806]",
                    //                    // disallowed in 2003
                    //                    "[\u200E-\u200F \u202A-\u202E \u2061-\u2063 \uFFFC \uFFFD \u1D173-\u1D17A \u206A-\u206F \uE0001 \uE0020-\uE007F]",
                    //                    // Step 7
                    //                    "[\u2260 \u226E \u226F \uFE12 \u2488]",
                    //                    // disallowed
                    //                    "[:age=9.0:]",
                    //                    // deviations
                    //                    "[\\u200C\\u200D\\u00DF\\u03C2]",
                    // stable sets
                    // bidi
                    "[\\u05BE\\u05C0\\u05C3\\u05C6\\u05D0-\\u05EA\\u05F0-\\u05F4\\u0608\\u060B\\u060D\\u061B\\u061C\\U0001EE67-\\U0001EE6A\\U0001EE6C-\\U0001EE72\\U0001EE74-\\U0001EE77\\U0001EE79-\\U0001EE7C\\U0001EE7E\\U0001EE80-\\U0001EE89\\U0001EE8B-\\U0001EE9B\\U0001EEA1-\\U0001EEA3\\U0001EEA5-\\U0001EEA9\\U0001EEAB-\\U0001EEBB]",
                    "[A-Za-z\\u00AA\\u00B5\\u00BA\\u00C0-\\u00D6\\u00D8-\\u00F6\\u00F8-\\u02B8\\u02BB-\\u02C1\\u02D0\\u02D1\\U0001F210-\\U0001F23B\\U0001F240-\\U0001F248\\U0001F250\\U0001F251\\U00020000-\\U0002A6D6\\U0002A700-\\U0002B734\\U0002B740-\\U0002B81D\\U0002B820-\\U0002CEA1\\U0002F800-\\U0002FA1D\\U000F0000-\\U000FFFFD\\U00100000-\\U0010FFFD]",
                    "[\\u0000-\\u0008\\u000E-\\u001B!-/\\:-@\\[-`\\{-\\u0084\\u0086-\\u00A9\\u00AB-\\u00B1\\u00B4\\u00B6-\\u00B8\\U0001F920-\\U0001F927\\U0001F930\\U0001F933-\\U0001F93E\\U0001F940-\\U0001F94B\\U0001F950-\\U0001F95E\\U0001F980-\\U0001F991\\U0001F9C0\\U000E0001\\U000E0020-\\U000E007F\\U000E0100-\\U000E01EF]",
                    "[0-9\\u00B2\\u00B3\\u00B9\\u06F0-\\u06F9\\u2070\\u2074-\\u2079\\u2080-\\u2089\\u2488-\\u249B\\uFF10-\\uFF19\\U000102E1-\\U000102FB\\U0001D7CE-\\U0001D7FF\\U0001F100-\\U0001F10A]",
                    "[\\u0600-\\u0605\\u0660-\\u0669\\u066B\\u066C\\u06DD\\u08E2\\U00010E60-\\U00010E7E]",
                    "[\\u0300-\\u036F\\u0483-\\u0489\\u0591-\\u05BD\\u05BF\\u05C1\\u05C2\\u05C4\\u05C5\\u05C7\\u0610-\\u061A\\u064B-\\u065F\\u0670\\U0001DA9B-\\U0001DA9F\\U0001DAA1-\\U0001DAAF\\U0001E000-\\U0001E006\\U0001E008-\\U0001E018\\U0001E01B-\\U0001E021\\U0001E023\\U0001E024\\U0001E026-\\U0001E02A\\U0001E8D0-\\U0001E8D6\\U0001E944-\\U0001E94A\\U000E0100-\\U000E01EF]",
                    // contextj
                    "[\\u200C\\u200D]",
                    "[\\u094D\\u09CD\\u0A4D\\u0ACD\\u0B4D\\u0BCD\\u0C4D\\u0CCD\\u0D4D\\u0DCA\\U00011235\\U000112EA\\U0001134D\\U00011442\\U000114C2\\U000115BF\\U0001163F\\U000116B6\\U0001172B\\U00011C3F]",
                    "[\\u00AD\\u0300-\\u036F\\u0483-\\u0489\\u0591-\\u05BD\\u05BF\\u05C1\\u05C2\\u05C4\\u05C5\\u05C7\\u0610-\\u061A\\u061C\\U0001E000-\\U0001E006\\U0001E008-\\U0001E018\\U0001E01B-\\U0001E021\\U0001E023\\U0001E024\\U0001E026-\\U0001E02A\\U0001E8D0-\\U0001E8D6\\U0001E944-\\U0001E94A\\U000E0001\\U000E0020-\\U000E007F\\U000E0100-\\U000E01EF]",
                    "[\\u0620\\u0626\\u0628\\u062A-\\u062E\\u0633-\\u063F\\u0641-\\u0647\\u0649\\u064A\\u066E\\u066F\\u0678-\\u0687\\u069A-\\u06BF\\U00010ADE-\\U00010AE0\\U00010AEB-\\U00010AEE\\U00010B80\\U00010B82\\U00010B86-\\U00010B88\\U00010B8A\\U00010B8B\\U00010B8D\\U00010B90\\U00010BAD\\U00010BAE\\U0001E900-\\U0001E943]",
                    "[\\u0620\\u0622-\\u063F\\u0641-\\u064A\\u066E\\u066F\\u0671-\\u0673\\u0675-\\u06D3\\u06D5\\u06EE\\u06EF\\u06FA-\\u06FC\\u06FF\\U00010AC0-\\U00010AC5\\U00010AC7\\U00010AC9\\U00010ACA\\U00010ACE-\\U00010AD6\\U00010AD8-\\U00010AE1\\U00010AE4\\U00010AEB-\\U00010AEF\\U00010B80-\\U00010B91\\U00010BA9-\\U00010BAE\\U0001E900-\\U0001E943]",
                    // syntax
                    "[\\-]",
                    // changed mapping from 2003
                    "[48F\\u04C0\\u10A0-\\u10C5\\u115F\\u1160\\u17B4\\u17B5\\u1806\\u2132\\u2183\\u2F86\\u2F87\\u2F91\\u2F95\\u2F9B\\u3164\\uFFA0]",
                    // disallowed in 2003
                    "[\\-0-\\u0377\\u037A-\\u037F\\u0384-\\u038A\\u038C\\u038E-\\u03A1\\u03A3-\\u052F\\u0531-\\u0556\\u0559-\\u055F\\u0561-\\u0587\\uAB20-\\uAB26\\uAB28-\\uAB2E\\uAB30-\\uAB65\\uAB70-\\uABED\\uABF0-\\uABF9\\uAC00-\\uD7A3\\uD7B0-\\uD7C6\\uD7CB-\\uD7FB\\uD800-\\uE007\\uFFFC\\uFFFD]",
                    // Step 7
                    "[\\u2260\\u226E\\u226F\\u2488\\uFE12]",
                    // disallowed
                    "[^\\ 0-9A-Za-z\\u00A0\\u00AA\\u00B2\\u00B3\\u00B5\\u00B9\\u00BA\\u00BC-\\U0006FFFD\\U00070000-\\U0007FFFD\\U00080000-\\U0008FFFD\\U00090000-\\U0009FFFD\\U000A0000-\\U000AFFFD\\U000B0000-\\U000BFFFD\\U000C0000-\\U000CFFFD\\U000D0001-\\U000DFFFD\\U000E0002-\\U000E001F\\U000E0080-\\U000EFFFD]",
                    // deviations
                    "[\\u00DF\\u03C2\\u200C\\u200D]",
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

    public static Transliterator hexForTest = Transliterator.getInstance(
            "[" + TO_ESCAPE + "&[\\u0000-\\uFFFF]] any-hex;" + TO_ESCAPE + " any-hex/perl;");
    Transliterator removeInvisible = Transliterator.getInstance("[[:di:][:c:]-[:whitespace:]] remove");
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
            // special case
            { "。", "B", "。", 0 },
            // special case
            { "\uAB60", "B", "\uAB60", 0 },

            { "1234567890\u00E41234567890123456789012345678901234567890123456", "B",
                "1234567890\u00E41234567890123456789012345678901234567890123456",
                Uts46.UIDNA_ERROR_LABEL_TOO_LONG },
            // all ASCII
            { "www.eXample.cOm", "B", "www.example.com", 0 },
            // u-umlaut
            { "B\u00FCcher.de", "B", "b\u00FCcher.de", 0 },
            // O-umlaut
            { "\u00D6BB", "B", "\u00F6bb", 0 },
            { "fa\u00DF.de", "N", "fa\u00DF.de", 0 }, // sharp s
            // sharp s
            { "fa\u00DF.de", "T", "fass.de", 0 },
            // sharp s in Punycode
            { "XN--fA-hia.dE", "B", "fa\u00DF.de", 0 },
            // Greek with final sigma
            { "\u03B2\u03CC\u03BB\u03BF\u03C2.com", "N",
                "\u03B2\u03CC\u03BB\u03BF\u03C2.com", 0 },
            // Greek with final
            // sigma
            { "\u03B2\u03CC\u03BB\u03BF\u03C2.com", "T",
                    "\u03B2\u03CC\u03BB\u03BF\u03C3.com", 0 },
            { "xn--nxasmm1c", "B", // Greek with final sigma in Punycode
                        "\u03B2\u03CC\u03BB\u03BF\u03C2", 0 },
            { "www.\u0DC1\u0DCA\u200D\u0DBB\u0DD3.com", "N", // "Sri" in "Sri
                            // Lanka" has a ZWJ
                            "www.\u0DC1\u0DCA\u200D\u0DBB\u0DD3.com", 0 },
            { "www.\u0DC1\u0DCA\u200D\u0DBB\u0DD3.com", "T", // "Sri" in "Sri
                                // Lanka" has a ZWJ
                                "www.\u0DC1\u0DCA\u0DBB\u0DD3.com", 0 },
            { "www.xn--10cl1a0b660p.com", "B", // "Sri" in Punycode
                                    "www.\u0DC1\u0DCA\u200D\u0DBB\u0DD3.com", 0 },
            { "\u0646\u0627\u0645\u0647\u200C\u0627\u06CC", "N", // ZWNJ
                                        "\u0646\u0627\u0645\u0647\u200C\u0627\u06CC", 0 },
            { "\u0646\u0627\u0645\u0647\u200C\u0627\u06CC", "T", // ZWNJ
                                            "\u0646\u0627\u0645\u0647\u0627\u06CC", 0 },
            { "xn--mgba3gch31f060k.com", "B", // ZWNJ in Punycode
                                                "\u0646\u0627\u0645\u0647\u200C\u0627\u06CC.com", 0 },
            { "a.b\uFF0Ec\u3002d\uFF61", "B",
                                                    "a.b.c.d.", 0 },
            { "U\u0308.xn--tda", "B", // U+umlaut.u-umlaut
                                                        "\u00FC.\u00FC", 0 },
            { "xn--u-ccb", "B", // u+umlaut in Punycode
                                                            "xn--u-ccb\uFFFD", Uts46.UIDNA_ERROR_INVALID_ACE_LABEL },
            { "a\u2488com", "B", // contains 1-dot
                                                                "a\uFFFDcom", Uts46.UIDNA_ERROR_DISALLOWED },
            { "xn--a-ecp.ru", "B", // contains 1-dot in Punycode
                                                                    "xn--a-ecp\uFFFD.ru", Uts46.UIDNA_ERROR_INVALID_ACE_LABEL },
            { "xn--0.pt", "B", // invalid Punycode
                                                                        "xn--0\uFFFD.pt", Uts46.UIDNA_ERROR_PUNYCODE },
            { "xn--a.pt", "B", // U+0080
                                                                            "xn--a\uFFFD.pt", Uts46.UIDNA_ERROR_INVALID_ACE_LABEL },
            { "xn--a-\u00C4.pt", "B", // invalid Punycode
                                                                                "xn--a-\u00E4.pt", Uts46.UIDNA_ERROR_PUNYCODE },
            { "\u65E5\u672C\u8A9E\u3002\uFF2A\uFF30", "B", // Japanese with
                                                                                    // fullwidth ".jp"
                                                                                    "\u65E5\u672C\u8A9E.jp", 0 },
            { "\u2615", "B", "\u2615", 0 }, // Unicode 4.0 HOT BEVERAGE
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
                            Uts46.UIDNA_ERROR_LABEL_TOO_LONG | Uts46.UIDNA_ERROR_CONTEXTJ },
            { "1.a\u00DF\u200C\u200Db\u200C\u200Dc\u00DF\u00DF\u00DF\u00DFd"
                    + "\u03C2\u03C3\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DFe"
                    + "\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DFx"
                    + "\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DFy"
                    + "\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u0302\u00DFz", "T",
                    "1.assbcssssssssd"
                            + "\u03C3\u03C3sssssssssssssssse"
                            + "ssssssssssssssssssssx"
                            + "ssssssssssssssssssssy"
                            + "sssssssssssssss\u015Dssz",
                            Uts46.UIDNA_ERROR_LABEL_TOO_LONG },
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
                    // + "\\U0001D530"
                    + UTF16.valueOf(0x1D530)
                    // + "\\U000E01EF"
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
                            + "1234567890123456789012345678901234567890123456789012345678901",
                            0 },
            { "123456789012345678901234567890123456789012345678901234567890123."
                    + "123456789012345678901234567890123456789012345678901234567890123."
                    + "123456789012345678901234567890123456789012345678901234567890123."
                    + "1234567890123456789012345678901234567890123456789012345678901.", "B",
                    "123456789012345678901234567890123456789012345678901234567890123."
                            + "123456789012345678901234567890123456789012345678901234567890123."
                            + "123456789012345678901234567890123456789012345678901234567890123."
                            + "1234567890123456789012345678901234567890123456789012345678901.",
                            0 },
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
                            Uts46.UIDNA_ERROR_LABEL_TOO_LONG | Uts46.UIDNA_ERROR_DOMAIN_NAME_TOO_LONG },
            // label length 63:
            // xn--1234567890123456789012345678901234567890123456789012345-9te
            { "\u00E41234567890123456789012345678901234567890123456789012345", "B",
                                "\u00E41234567890123456789012345678901234567890123456789012345", 0 },
            { "1234567890\u00E41234567890123456789012345678901234567890123456", "B",
                                    "1234567890\u00E41234567890123456789012345678901234567890123456",
                                    Uts46.UIDNA_ERROR_LABEL_TOO_LONG },
            { "123456789012345678901234567890123456789012345678901234567890123."
                    + "1234567890\u00E4123456789012345678901234567890123456789012345."
                    + "123456789012345678901234567890123456789012345678901234567890123."
                    + "1234567890123456789012345678901234567890123456789012345678901", "B",
                    "123456789012345678901234567890123456789012345678901234567890123."
                            + "1234567890\u00E4123456789012345678901234567890123456789012345."
                            + "123456789012345678901234567890123456789012345678901234567890123."
                            + "1234567890123456789012345678901234567890123456789012345678901",
                            0 },
            { "123456789012345678901234567890123456789012345678901234567890123."
                    + "1234567890\u00E4123456789012345678901234567890123456789012345."
                    + "123456789012345678901234567890123456789012345678901234567890123."
                    + "1234567890123456789012345678901234567890123456789012345678901.", "B",
                    "123456789012345678901234567890123456789012345678901234567890123."
                            + "1234567890\u00E4123456789012345678901234567890123456789012345."
                            + "123456789012345678901234567890123456789012345678901234567890123."
                            + "1234567890123456789012345678901234567890123456789012345678901.",
                            0 },
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
                            Uts46.UIDNA_ERROR_LABEL_TOO_LONG | Uts46.UIDNA_ERROR_DOMAIN_NAME_TOO_LONG },
            // hyphen errors and empty-label errors
            // "xn---q----jra"=="-q--a-umlaut-"
            { "a.b..-q--a-.e", "B", "a.b..-q--a-.e",
                                Uts46.UIDNA_ERROR_EMPTY_LABEL | Uts46.UIDNA_ERROR_LEADING_HYPHEN | Uts46.UIDNA_ERROR_TRAILING_HYPHEN
                                |
                                Uts46.UIDNA_ERROR_HYPHEN_3_4 },
            { "a.b..-q--\u00E4-.e", "B", "a.b..-q--\u00E4-.e",
                                    Uts46.UIDNA_ERROR_EMPTY_LABEL | Uts46.UIDNA_ERROR_LEADING_HYPHEN | Uts46.UIDNA_ERROR_TRAILING_HYPHEN
                                    |
                                    Uts46.UIDNA_ERROR_HYPHEN_3_4 },
            { "a.b..xn---q----jra.e", "B", "a.b..-q--\u00E4-.e",
                                        Uts46.UIDNA_ERROR_EMPTY_LABEL | Uts46.UIDNA_ERROR_LEADING_HYPHEN | Uts46.UIDNA_ERROR_TRAILING_HYPHEN
                                        |
                                        Uts46.UIDNA_ERROR_HYPHEN_3_4 },
            { "a..c", "B", "a..c", Uts46.UIDNA_ERROR_EMPTY_LABEL },
            { "a.-b.", "B", "a.-b.", Uts46.UIDNA_ERROR_LEADING_HYPHEN },
            { "a.b-.c", "B", "a.b-.c", Uts46.UIDNA_ERROR_TRAILING_HYPHEN },
            { "a.-.c", "B", "a.-.c", Uts46.UIDNA_ERROR_LEADING_HYPHEN | Uts46.UIDNA_ERROR_TRAILING_HYPHEN },
            { "a.bc--de.f", "B", "a.bc--de.f", Uts46.UIDNA_ERROR_HYPHEN_3_4 },
            { "\u00E4.\u00AD.c", "B", "\u00E4..c", Uts46.UIDNA_ERROR_EMPTY_LABEL },
            { "\u00E4.-b.", "B", "\u00E4.-b.", Uts46.UIDNA_ERROR_LEADING_HYPHEN },
            { "\u00E4.b-.c", "B", "\u00E4.b-.c", Uts46.UIDNA_ERROR_TRAILING_HYPHEN },
            { "\u00E4.-.c", "B", "\u00E4.-.c", Uts46.UIDNA_ERROR_LEADING_HYPHEN | Uts46.UIDNA_ERROR_TRAILING_HYPHEN },
            { "\u00E4.bc--de.f", "B", "\u00E4.bc--de.f", Uts46.UIDNA_ERROR_HYPHEN_3_4 },
            { "a.b.\u0308c.d", "B", "a.b.\uFFFDc.d", Uts46.UIDNA_ERROR_LEADING_COMBINING_MARK },
            { "a.b.xn--c-bcb.d", "B", "a.b.xn--c-bcb\uFFFD.d", Uts46.UIDNA_ERROR_LEADING_COMBINING_MARK },
            // BiDi
            { "A0", "B", "a0", 0 },
            { "0A", "B", "0a", 0 }, // all-LTR is ok to start with a digit (EN)
            { "0A.\u05D0", "B", // ASCII label does not start with L/R/AL
                "0a.\u05D0", Uts46.UIDNA_ERROR_BIDI },
            { "c.xn--0-eha.xn--4db", "B", // 2nd label does not start with
                    // L/R/AL
                    "c.0\u00FC.\u05D0", Uts46.UIDNA_ERROR_BIDI },
            { "b-.\u05D0", "B", // label does not end with L/EN
                        "b-.\u05D0", Uts46.UIDNA_ERROR_TRAILING_HYPHEN | Uts46.UIDNA_ERROR_BIDI },
            { "d.xn----dha.xn--4db", "B", // 2nd label does not end with L/EN
                            "d.\u00FC-.\u05D0", Uts46.UIDNA_ERROR_TRAILING_HYPHEN | Uts46.UIDNA_ERROR_BIDI },
            { "a\u05D0", "B", "a\u05D0", Uts46.UIDNA_ERROR_BIDI }, // first dir
            // != last
            // dir
            { "\u05D0\u05C7", "B", "\u05D0\u05C7", 0 },
            { "\u05D09\u05C7", "B", "\u05D09\u05C7", 0 },
            { "\u05D0a\u05C7", "B", "\u05D0a\u05C7", Uts46.UIDNA_ERROR_BIDI }, // first
            // dir
            // !=
            // last
            // dir
            { "\u05D0\u05EA", "B", "\u05D0\u05EA", 0 },
            { "\u05D0\u05F3\u05EA", "B", "\u05D0\u05F3\u05EA", 0 },
            { "a\u05D0Tz", "B", "a\u05D0tz", Uts46.UIDNA_ERROR_BIDI }, // mixed
            // dir
            { "\u05D0T\u05EA", "B", "\u05D0t\u05EA", Uts46.UIDNA_ERROR_BIDI }, // mixed
            // dir
            { "\u05D07\u05EA", "B", "\u05D07\u05EA", 0 },
            { "\u05D0\u0667\u05EA", "B", "\u05D0\u0667\u05EA", 0 }, // Arabic 7
            // in the
            // middle
            { "a7\u0667z", "B", "a7\u0667z", Uts46.UIDNA_ERROR_BIDI }, // AN
            // digit
            // in LTR
            { "\u05D07\u0667\u05EA", "B", // mixed EN/AN digits in RTL
                "\u05D07\u0667\u05EA", Uts46.UIDNA_ERROR_BIDI },
            // ZWJ
            { "\u0BB9\u0BCD\u200D", "N", "\u0BB9\u0BCD\u200D", 0 }, // Virama+ZWJ
            { "\u0BB9\u200D", "N", "\u0BB9\u200D", Uts46.UIDNA_ERROR_CONTEXTJ }, // no
            // Virama
            { "\u200D", "N", "\u200D", Uts46.UIDNA_ERROR_CONTEXTJ }, // no
            // Virama
            // ZWNJ
            { "\u0BB9\u0BCD\u200C", "N", "\u0BB9\u0BCD\u200C", 0 }, // Virama+ZWNJ
            { "\u0BB9\u200C", "N", "\u0BB9\u200C", Uts46.UIDNA_ERROR_CONTEXTJ }, // no
            // Virama
            { "\u200C", "N", "\u200C", Uts46.UIDNA_ERROR_CONTEXTJ }, // no
            // Virama
            { "\u0644\u0670\u200C\u06ED\u06EF", "N", // Joining types D T ZWNJ T
                // R
                "\u0644\u0670\u200C\u06ED\u06EF", 0 },
            { "\u0644\u0670\u200C\u06EF", "N", // D T ZWNJ R
                    "\u0644\u0670\u200C\u06EF", 0 },
            { "\u0644\u200C\u06ED\u06EF", "N", // D ZWNJ T R
                        "\u0644\u200C\u06ED\u06EF", 0 },
            { "\u0644\u200C\u06EF", "N", // D ZWNJ R
                            "\u0644\u200C\u06EF", 0 },
            { "\u0644\u0670\u200C\u06ED", "N", // D T ZWNJ T
                                "\u0644\u0670\u200C\u06ED", Uts46.UIDNA_ERROR_BIDI | Uts46.UIDNA_ERROR_CONTEXTJ },
            { "\u06EF\u200C\u06EF", "N", // R ZWNJ R
                                    "\u06EF\u200C\u06EF", Uts46.UIDNA_ERROR_CONTEXTJ },
            { "\u0644\u200C", "N", // D ZWNJ
                                        "\u0644\u200C", Uts46.UIDNA_ERROR_BIDI | Uts46.UIDNA_ERROR_CONTEXTJ },
            // { "", "B",
            // "", 0 },
            {"0à.\u05D0"},
            {"à.\u05D00\u0660"},
            {"a。。b"},
            {"\u200D。。\u06B9\u200C"},
            {"\u05D0\u0030\u0660"},
            {"$"},
    };
}
