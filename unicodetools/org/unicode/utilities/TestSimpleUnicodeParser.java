package org.unicode.utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.BNF;
import org.unicode.cldr.util.Quoter;
import org.unicode.cldr.util.VariableReplacer;
import org.unicode.utilities.StringSetBuilder.UnicodeSetBuilderFactory;

import com.google.common.base.Joiner;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UnicodeSet;

public class TestSimpleUnicodeParser extends TestFmwk {
    public static void main(String[] args) {
	new TestSimpleUnicodeParser().run(args);
    }
    public void testSamples() {
	String[] tests = {
		"[^\\q{f\\t}--\\p{name=FISHEYE}️--\\P{name=FISHEYE}]",
		"[abc\\u{3b1 3b3}-ζ]",
		"[a-\\u{3b1}\\u{3b3}-ζ]",
		"[[ab] && [bc]]",
		"[[ab] -- [bc] && [de]]",
		"[[abc] -- [[bcd] && [cde]]]",
		"[^abc]",
		"[abc \\q{xy} d]",
		"[a-\\u{3b1 3b3}-ζ]",
		"[abc\\u{3b1 3b3}-ζ]",
		"\\p{Word_Break = Mid_Letter}",
		"[\\p{script = cyrillic} && \\p{gc = Letter}]",
	};
	UnicodeSetBuilderFactory uss = new UnicodeSetBuilderFactory();
	final SimpleUnicodeParser sup = new SimpleUnicodeParser(uss).setLogOn(true);
	int count = 0;
	for (String test : tests) {
	    StringSetBuilder<UnicodeSet> actualus = sup.parse(test);
	    UnicodeSet actual = actualus.buildSet();
	}
    }

    public void testSpaces() {
	
	String[] tests = {
		"[ [ a b ] && [ b c ] ]",
		"[^ a b c ]",
		"[ a b c \\q{ x y } d]",
		"[ a b c \\q{ x \\u{20} y } d]",
		"[ a - \\u{ 3b1 20 3b3 } - ζ]", // hack to check actual against the UnicodeSet; it has \\u{A0} in place of space in \\u{...}
		"\\p{ Word_Break = Mid_Letter }",
	};
	UnicodeSetBuilderFactory uss = new UnicodeSetBuilderFactory();
	final SimpleUnicodeParser sup = new SimpleUnicodeParser(uss).setIgnoreSpaces(true);
	int count = 0;
	for (String test : tests) {
	    StringSetBuilder<UnicodeSet> actualus = sup.parse(test);
	    UnicodeSet actual = actualus.buildSet();

	    StringSetBuilder<UnicodeSet> expectedUs = sup.parse(test.replace(" ", ""));
	    UnicodeSet expected = expectedUs.buildSet();
	    assertEquals(test, expected, actual);
	}
    }


    public void testBasic() {
	String[] tests = {
		"[abc]",
		"[abc\\q{xy}]==[abc{xy}]",
		"[abc\\q{x\\u{71 72}y}]==[a-c{xqry}]",
		"[a-ce]",
		"[a-cb-d]",
		"[a&b]==[a\\&b]",
		"[a~b]==[a\\~b]",
		"[a&c-e~g]==[\\&ac-eg~]",
		"[[ab]&&[bc]]==[[ab]&[bc]]",
		"[[ab]--[bc]]==[[ab]-[bc]]",
		"[[ab]~~[bc]]==[[ab]-[bc][[bc]-[ab]]]",
		"[^[ab] && [bc]]==[^[ab]&[bc]]",
		"[^[ab] -- [bc]]==[^[ab]-[bc]]",
		"[^[ab] [bc]]",
		"[^[ab] [bc]]",
		"[abc]",
		"\\p{dash=true}",
		"\\P{dash=true}",
		"[\\p{dash=true}]",
		"[^\\p{dash=true}]",
		"[\\u{61 62}]==[ab]",

		// expected exceptions
		"[\\u{ab-c}]==EXCEPTION",
		"[a-c-e]",
		"[e-a]",
		"[a-",
		"a",
		"[a-]==EXCEPTION",
		"[-a]",
		"[[a]-b]",
		"[a-[b]]",
	};
	UnicodeSetBuilderFactory uss = new UnicodeSetBuilderFactory();
	final SimpleUnicodeParser sup = new SimpleUnicodeParser(uss);
	int line = 0;
	for (String row : tests) {
	    ++line;
	    String[] row2 = row.split("==");
	    String test = row2[0];
	    String expectedString = row2.length < 2 ? test : row2[1];
	    RuntimeException expectedException = null;
	    UnicodeSet expected = null;
	    if (expectedString.contentEquals("EXCEPTION")) {
		expectedException = new IllegalArgumentException();
	    } else {
		try {
		    expected = new UnicodeSet(expectedString);
		    expected.complement().complement();
		} catch (RuntimeException e) {
		    expectedException = e;
		}
	    }
	    RuntimeException actualException = null;
	    UnicodeSet actual = null;
	    try {
		StringSetBuilder<UnicodeSet> actualus = sup.parse(test);
		actual = actualus.buildSet();
	    } catch (RuntimeException e) {
		actualException = e;
	    }
	    if (expectedException != null) {
		if (actualException != null) {
		    logln(line + ") expected≅actual: " + expectedException + "\n\t\t\t\t\t; " + actualException);
		} else {
		    errln(line + ") expected: " + expectedException + "\n\t\t\t\t\t; actual: " + actual.toPattern(false));
		}
	    } else if (actualException != null) {
		errln(line + ") expected: " + expected.toPattern(false) + "\n\t\t\t\t\t; actual: " + actualException);
	    } else if (!expected.equals(actual)) {
		errln(line + ") expected: " + expected.toPattern(false) + "; actual: " + actual.toPattern(false));
	    } else {
		logln(line + ") expected=actual: " + expected.toPattern(false));
	    }
	}
    }


    public void test0Bnf() {
	String uts18Rules = getRulesFrom(TestSimpleUnicodeParser.class, "regexBnf.txt");
	System.out.println(uts18Rules);

	BNF bnf = new BNF(new Random(0), new Quoter.RuleQuoter())
		//.addSet("$chars", new UnicodeSet("[a-e]"))
		.setMaxRepeat(2)
		.addRules(uts18Rules)
		.complete();
	System.out.println(bnf.getInternal());


	UnicodeSetBuilderFactory uss = new UnicodeSetBuilderFactory();
	uss.setSkipValidity(true);
	final SimpleUnicodeParser sup = new SimpleUnicodeParser(uss);

	int count = 100;
	for (int i = 0; i < count; ++i) {
	    String test = bnf.next();
	    try {
		StringSetBuilder<UnicodeSet> expectedBuilder = sup.parse(test);
		logln((i+1) + ") " + test + " => " + expectedBuilder.buildSet().toPattern(false));
	    } catch (Exception e) {
		errln(test + " => " + e.getMessage());
		e.printStackTrace();
	    }
	}
    }

    public String getRulesFrom(Class<TestSimpleUnicodeParser> class1, String fileName) {
	Multimap<String, String> ruleMap = LinkedHashMultimap.create();
	String head = null;
	String root = null;
	for (String rule : FileUtilities.in(class1, fileName)) {
	    if (rule.startsWith("#")) {
		continue;
	    }
	    String[] parts = rule.split(":=");
	    String head0 = parts[0].trim();
	    if (parts.length != 2 && head0.isEmpty()) {
		continue;
	    }
	    head = head0.isEmpty() ? head : head0;
	    String tail = parts[1].trim();
	    ruleMap.put(head, tail);
	    if (root == null) {
		root = head;
	    }
	}
	// fix variables
	VariableReplacer variableReplace = new VariableReplacer();
	for (Entry<String, Collection<String>> entry : ruleMap.asMap().entrySet()) {
	    String variable = entry.getKey();
	    String replacement = fixVariable(variable);
	    if (variable.equals(root)) {
		replacement = "$root";
	    }
	    variableReplace.add(variable, replacement);
	}
	List<String> rules = new ArrayList<>();
	for ( Entry<String, Collection<String>> entry : ruleMap.asMap().entrySet()) {
	    final String key = variableReplace.replace(entry.getKey());
	    final String value = Joiner.on(" | ").join(entry.getValue());
	    rules.add("\n" + key + " = " + variableReplace.replace(value) + " ;");
	}
	String uts18Rules = Joiner.on("").join(rules);
	return uts18Rules;
    }

    private String fixVariable(String _variable) {
	String result = "$" + _variable.toLowerCase(Locale.ENGLISH).replace("_", "");
	return result;
    }
}
