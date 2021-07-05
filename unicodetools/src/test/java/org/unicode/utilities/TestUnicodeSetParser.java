package org.unicode.utilities;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.RegexUtilities;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.UnicodeRegex;
import com.ibm.icu.text.UnicodeSet;

public class TestUnicodeSetParser extends TestFmwk {
    public static void main(String[] args) {
	new TestUnicodeSetParser().run(args);
    }

    public void TestSimple() {
	UnicodeSetParser parser = new UnicodeSetParser();
	
	String[] tests = {
		"[a-b]",
		"\\p{sc=Runic}",
		"[\\p{script=runic}&\\p{gc=Nl}]",
		"[\\p{script=runic}-\\p{gc=Nl}]",
		"[[a-b]&[^[^[c-d]]]]",
		};
	for (String test : tests) {
	    UnicodeSet expected = new UnicodeSet(test);
	    UnicodeSet actual = parser.applyPattern(new UnicodeSet(), test); 
	    assertEquals(test, expected, actual);
	}
	
    }
    
    public void TestRegex() {
	String[][] tests = {
		{"[a-c&&b[d]&&b]", "[[a-c]&[bd]&[ab]]"}
	};
	for (String[] row : tests) {
		Matcher matcher = Pattern.compile(row[0]).matcher("");
		UnicodeSet actual = new UnicodeSet();
		StringBuilder buf = new StringBuilder("a");
		for (char cp = 'a'; cp <= 'z'; ++cp) {
		    buf.setCharAt(0, cp);
		    if (matcher.reset(buf).matches()) {
			actual.add(cp);
		    }
		}
		UnicodeSet expected = new UnicodeSet(row[1]);
		assertEquals(row[0], expected, actual);
	}
    }
}
