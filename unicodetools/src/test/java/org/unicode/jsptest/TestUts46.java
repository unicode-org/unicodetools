package org.unicode.jsptest;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.unicode.idna.GenerateIdnaTest;
import org.unicode.idna.Idna;
import org.unicode.idna.Regexes;
import org.unicode.idna.Uts46;
import org.unicode.idna.Uts46.Errors;
import org.unicode.idna.Uts46.IdnaChoice;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.Normalizer2;

public class TestUts46 extends TestFmwk{

    boolean generate;

    public static void main(String[] args) throws IOException {
        checkSplit("..a..b..");
        checkSplit(".");

        //    TestUts46 testUts46 = new TestUts46();
        //    if (Arrays.asList(args).contains("generate")) {
        //      int count = testUts46.generateTests(1000);
        //      System.out.println("DONE " + count);
        //      return;
        //    }
        //    testUts46.run(args);
    }

    private static void checkSplit(String testcase) {
        String[] labels = Idna.FULL_STOP.split(testcase);
        showSplit("Old", testcase, labels);
        labels = Regexes.split(Idna.FULL_STOP, testcase);
        showSplit("New", testcase, labels);
    }

    private static void showSplit(String title, String testcase, String[] labels) {
        System.out.print(title + ":Splitting: \"" + testcase + "\"\t=>\t");
        boolean first = true;
        for (final String label : labels) {
            if (first) {
                first = false;
            } else {
                System.out.print(", ");
            }
            System.out.print("\"" + label + "\"");
        }
        System.out.println("");
    }




    private int errorNum;

    // CS, ET, ON, BN and NSM
    public void TestBidi() {
        final Set<Errors> errors = new LinkedHashSet<Errors>();

        for (final String[] test : GenerateIdnaTest.bidiTests) {
            final String domain = test[0];
            final Collection<String> rules = Arrays.asList(test).subList(1, test.length);
            //String rule = test[1];
            errors.clear();
            final boolean error = Uts46.hasBidiError(domain, errors);
            checkErrors(domain, rules, error, errors);
        }
    }

    public void TestContextJ() {
        final Set<Errors> errors = new LinkedHashSet<Errors>();

        for (final String[] test : GenerateIdnaTest.contextTests) {
            final String domain = test[0];
            final Collection<String> rules = Arrays.asList(test).subList(1, test.length);
            errors.clear();
            final boolean error = Uts46.hasContextJError(domain, errors);
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
            for (final String rule : rules) {
                for (final Errors error : errors) {
                    if (error.toString().contains(rule)) {
                        continue main;
                    }
                }
                return false;
            }
    return true;
    }


    static final Normalizer2 nfd = Normalizer2.getInstance(null, "nfc", Normalizer2.Mode.COMPOSE);
    static final Normalizer2 nfc = Normalizer2.getInstance(null, "nfc", Normalizer2.Mode.COMPOSE);
    static final Normalizer2 nfkc = Normalizer2.getInstance(null, "nfc", Normalizer2.Mode.COMPOSE);
    static final Normalizer2 nfkd = Normalizer2.getInstance(null, "nfc", Normalizer2.Mode.COMPOSE);



    enum TestType {B, N, T}

    public void TestIcuCases() {
        final Set<Errors> toUnicodeErrors = new LinkedHashSet<Errors>();
        for (final Object[] testCaseLine : GenerateIdnaTest.testCases) {
            final String source = testCaseLine[0].toString();
            final TestType type = TestType.valueOf(testCaseLine[1].toString());
            final String target = testCaseLine[2].toString();
            final String targetError = testCaseLine[3].toString();
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
        final String unicode = Uts46.SINGLETON.toUnicode(source, idnaChoice, toUnicodeErrors);
        final String punycode = Uts46.SINGLETON.toASCII(source, idnaChoice, toUnicodeErrors);
        final boolean expectedError = !targetError.equals("0");
        final boolean actualError = toUnicodeErrors.size() != 0;
        if (expectedError != actualError) {
            errln("Error code for: " + source + "\texpected:\t" + (expectedError ? targetError : "NO_ERROR")
                    + "\tactual:\t" + (actualError ? toUnicodeErrors.toString() : "NO_ERROR"));
        } else if (!actualError) {
            logln("Error code for: " + source + "\texpected:\t" + (expectedError ? targetError : "NO_ERROR")
                    + "\tactual:\t" + (actualError ? toUnicodeErrors.toString() : "NO_ERROR"));
            assertEquals("Result for: " + source, target, unicode);
        }
    }

}
