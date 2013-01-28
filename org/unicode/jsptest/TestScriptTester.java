package org.unicode.jsptest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.jsp.Builder;
import org.unicode.jsp.Confusables;
import org.unicode.jsp.Confusables.ScriptCheck;
import org.unicode.jsp.ScriptTester;
import org.unicode.jsp.XIDModifications;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.Normalizer;

public class TestScriptTester extends TestFmwk {
	public static void main(String[] args) {
		new TestScriptTester().run(args);
	}

	public void TestBasic() {
		final ScriptTester scriptTester = ScriptTester.start().get();

		final String[] bad = {
				"gー",
				"ä\u0308",
				"1٦",
				"٦۶",
				"1aᎠ",
				"aᎠ1",
				"aᎠ", // Cherokee
				"aα", // simplified and traditional
				"万丟", // simplified and traditional
		};
		for (final String testCase : bad) {
			final boolean result = scriptTester.isOk(testCase);
			assertFalse(testCase, result);
		}

		final String[] cases = {"abc", "1abc", "abc1", "ab가", "一가", "一万", "一丟", "一\u4E07"};
		for (final String testCase : cases) {
			final boolean result = scriptTester.isOk(testCase);
			assertTrue(testCase + " should be ok: ", result);
		}

	}

	public void TestFilter() {
		checkFilter("万");
		checkFilter("丟");
		checkFilter("\u4e01");
	}

	private void checkFilter(String testChar) {
		final List<Set<String>> listTrial = Builder.with(new ArrayList<Set<String>>())
				.add(Builder.with(new LinkedHashSet<String>()).addAll("\u30FC", "-", "\u4e00").get())
				.add(Builder.with(new LinkedHashSet<String>()).addAll(testChar).get())
				.get();
		final ScriptTester scriptTester = ScriptTester.start().get();
		final String before = listTrial.toString();
		scriptTester.filterTable(listTrial);
		final String after = listTrial.toString();
		assertEquals("filterTable", before, after);
	}

	public void TestConfusables() {
		final TreeSet<String> expected = Builder.with(new TreeSet<String>()).addAll("google", "goog1e", "googIe").get();
		checkScriptCheck("google", expected);
		checkScriptCheck("mark", null);
		checkScriptCheck("scope", null);
		checkScriptCheck("pop", null);

		//g໐໐g1e
	}

	private void checkScriptCheck(String string, TreeSet<String> expected) {
		final Confusables confusables = new Confusables(string)
		.setNormalizationCheck(Normalizer.NFKC)
		.setScriptCheck(ScriptCheck.same)
		.setAllowedCharacters(XIDModifications.getAllowed());

		final TreeSet<String> items = Builder.with(new TreeSet<String>()).addAll(confusables).get();
		if (expected != null) {
			assertEquals("Confusables for '" + string +
					"'", expected, items);
		}

		final Confusables confusables2 = new Confusables(string)
		.setNormalizationCheck(Normalizer.NFKC)
		.setAllowedCharacters(XIDModifications.getAllowed());

		final HashSet<String> filteredDifferently = new HashSet<String>();
		for (final String s : confusables2) {
			if (Confusables.scriptTester.isOk(s)) {
				filteredDifferently.add(s);
			}
		}
		assertEquals("Confusables for '" + string +
				"'", items, filteredDifferently);
	}
}
