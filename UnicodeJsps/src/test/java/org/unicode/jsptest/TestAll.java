// ##header J2SE15

package org.unicode.jsptest;

import com.ibm.icu.dev.test.TestFmwk.TestGroup;

/** Top level test used to run all other tests as a batch. */
public class TestAll extends TestGroup {

    public static void main(String[] args) {
        new TestAll().run(args);
    }

    public TestAll() {
        super(
                new String[] {
                    "org.unicode.jsptest.TestAlternateIterator",
                    "org.unicode.jsptest.TestBasicProperties",
                    // "org.unicode.jsptest.TestBuilder", // not really a test, move
                    "org.unicode.jsptest.TestEmoji",
                    // "org.unicode.jsptest.TestGenerate", // not really a test, move
                    // "org.unicode.jsptest.TestIcuProperties", // not really a test, move
                    "org.unicode.jsptest.TestIdna",
                    "org.unicode.jsptest.TestJsp",
                    "org.unicode.jsptest.TestLanguageid",
                    "org.unicode.jsptest.TestProperties",
                    "org.unicode.jsptest.TestScriptTester",
                    // "org.unicode.jsptest.TestTypology",
                    "org.unicode.jsptest.TestUnicodeSet",
                    "org.unicode.jsptest.TestUts46",
                },
                "All tests in jsptest");
    }
}
