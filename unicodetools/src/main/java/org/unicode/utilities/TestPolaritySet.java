package org.unicode.utilities;

import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.utilities.PolaritySet.Operation;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.dev.test.TestFmwk;

public class TestPolaritySet extends TestFmwk {
    private static final Splitter SPACE_SPLITTER = Splitter.on(' ');

    public static void main(String[] args) {
	new TestPolaritySet().run(args);
    }

    public void testBasic() {
	Matcher mainOp = Pattern.compile("[∪∩∖⊕]").matcher("");
	System.out.println(ImmutableSet.of("a", "b"));

	String[][] tests = {
		{"¬{a b}", "¬{a b}"},

		{"¬Ω", "∅"},
		{"¬∅", "Ω"},

		{"{a b} ∪ Ω", "Ω"},
		{"{a b} ∪ ∅", "{a b}"},

		{"{a b} ∩ Ω", "{a b}"},
		{"{a b} ∩ ∅", "∅"},

		{"{a b} ∪ {b c}", "{a b c}"},
		{"{a b} ∪ ¬{b c}", "¬{c}"},
		{"¬{a b} ∪ {b c}", "¬{a}"},
		{"¬{a b} ∪ ¬{b c}", "¬{b}"},

		{"{a b} ∩ {b c}", "{b}"},
		{"{a b} ∩ ¬{b c}", "{a}"},
		{"¬{a b} ∩ {b c}", "{c}"},
		{"¬{a b} ∩ ¬{b c}", "¬{a b c}"},

		{"{a b} ∖ {b c}", "{a}"},
		{"{a b} ∖ ¬{b c}", "{b}"},
		{"¬{a b} ∖ {b c}", "¬{a b c}"},
		{"¬{a b} ∖ ¬{b c}", "{c}"},

		{"{a b} ⊕ {b c}", "{a c}"},
		{"{a b} ⊕ ¬{b c}", "¬{a c}"},
		{"¬{a b} ⊕ {b c}", "¬{a c}"},
		{"¬{a b} ⊕ ¬{b c}", "{a c}"},
	};
	for (String[] row : tests) {
	    String source = row[0];
	    String expected = row[1];
	    PolaritySet<String> result;
	    String modSource;
	    if (!mainOp.reset(source).find()) {
		// no main op, check !
		result = PolaritySet.fromTestString(source);
		modSource = result.toString();
	    } else {
		PolaritySet<String> left = PolaritySet.fromTestString(source.substring(0, mainOp.start()));
		PolaritySet<String> right = PolaritySet.fromTestString(source.substring(mainOp.end()));
		modSource = left + " " + mainOp.group() + " " + right;
		result = PolaritySet.of(left);
		Operation operation = PolaritySet.Operation.fromDisplay(mainOp.group());
		switch(operation) {
		case UNION: 
		    result.addAll(right);
		    break;
		case INTERSECT: 
		    result.retainAll(right);
		    break;
		case SUBTRACT: 
		    result.removeAll(right);
		    break;
		case XOR: 
		    result.retainDifferences(right);
		    break;
		}
	    }
	    String actual = result == null ? null : result.toString();
	    assertEquals(source + (modSource.equals(source) ? "" : "\t" + "(" + modSource + ")"), expected, actual);
	}
    }
}
