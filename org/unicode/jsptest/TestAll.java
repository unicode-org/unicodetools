//##header J2SE15

package org.unicode.jsptest;

import com.ibm.icu.dev.test.TestFmwk.TestGroup;

/**
 * Top level test used to run all other tests as a batch.
 */
public class TestAll extends TestGroup {

	public static void main(String[] args) {
		new TestAll().run(args);
	}

	public TestAll() {
		super(
				new String[] {
						"org.unicode.jsptest.TestAlternateIterator",
						"org.unicode.jsptest.TestBuilder",
						//"org.unicode.jsptest.TestGenerate", // not really a test
						"org.unicode.jsptest.TestIdna",
						"org.unicode.jsptest.TestJsp",
						"org.unicode.jsptest.TestProperties",
						"org.unicode.jsptest.TestScriptTester",
						"org.unicode.jsptest.TestTypology",
						"org.unicode.jsptest.TestUnicodeSet",
				},
				"All tests in jsptest");
	}
}