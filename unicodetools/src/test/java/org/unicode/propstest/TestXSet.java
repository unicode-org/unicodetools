package org.unicode.propstest;

import java.util.Map.Entry;


import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableBiMap.Builder;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

import org.junit.jupiter.api.Test;
import org.unicode.unittest.TestFmwkMinusMinus;

public class TestXSet extends TestFmwkMinusMinus {
    private static final boolean SKIP_VALUES = false;

    public enum Operation {containsAll, containsNone, addAll, retainAll, removeAll}

	@Test
    public void TestCombos() {

	String[] tests = {
		"[]", "[a]", "[b]", "[c]", "[ab]", "[bc]", "[ca]", "[abc]",
	};

	// populate
	Builder<XSet<String>, UnicodeSet> builder = ImmutableBiMap.builder();

	for (String a1 : tests) {
	    final UnicodeSet u = new UnicodeSet(a1).freeze();
	    final XSet<String> x = of(a1).freeze();
	    builder.put(x, u);
	    //System.out.println(x + " " + u);

	    final UnicodeSet nu = new UnicodeSet(u).complement().freeze();
	    final XSet<String> nx= new XSet<String>().set(x).negate().freeze();
	    builder.put(nx, nu);
	    //System.out.println(nx + " " + nu);
	}
	ImmutableBiMap<XSet<String>, UnicodeSet> xuTests = builder.build();
	ImmutableBiMap<UnicodeSet, XSet<String>> uxTests = xuTests.inverse();

	System.out.println(xuTests);

	XSet<String> xs = new XSet<>();
	UnicodeSet us = new UnicodeSet();
	boolean xb, ub;

	// check against one another
	for (Entry<XSet<String>, UnicodeSet> pair1 : xuTests.entrySet()) {
	    final XSet<String> x1 = pair1.getKey();
	    final boolean x1pos = x1.isPositive();
	    final UnicodeSet u1 = pair1.getValue();

	    System.out.println(x1);

	    for (Entry<XSet<String>, UnicodeSet> pair2 : xuTests.entrySet()) {
		final XSet<String> x2 = pair2.getKey();
		final boolean x2Pos = x2.isPositive();
		final UnicodeSet u2 = pair2.getValue();
		//System.out.println(x1 + " — " + u1);

//		if (SKIP_VALUES || !x1pos && x2Pos) {
//		    int x = 0; // for debugging
//		} else {
//		    continue;
//		}

		for (Operation op : Operation.values()) {
		    switch (op) {
		    case containsAll:
			xb = x1.containsAll(x2);
			ub = u1.containsAll(u2);
			if (!assertEquals("containsAll:\t" + x1 + " ⊇ " + x2 + "\t IFF  " + u1 + " ⊇ " + u2, ub, xb)) {
			    xb = x1.containsAll(x2); // for debugging
			}
			break;
		    case containsNone:
			xb = x1.containsNone(x2);
			ub = u1.containsNone(u2);
			if (!assertEquals("containsNone:\t" + x1 + " ∩ " + x2 + " = ∅\t IFF " + u1 + " ∩ " + u2 +  " = ∅", ub, xb)) {
			    xb = x1.containsNone(x2); // for debugging
			}
			break;
		    case addAll:
			xs = XSet.of(x1).addAll(x2);
			us = new UnicodeSet(u1).addAll(u2);
			if (!assertEquals("add:\t" + x1 + " ∪= " + x2 + "\t IFF " + u1 + " ∪= " + u2, uxTests.get(us), xs)) {
			    xb = x1.containsNone(x2); // for debugging
			}
			break;
		    case removeAll:
			xs = XSet.of(x1).removeAll(x2);
			us = new UnicodeSet(u1).removeAll(u2);
			if (!assertEquals("remove:\t" + x1 + " ⊖= " + x2 + "\t IFF " + u1 + " ⊖= " + u2, uxTests.get(us), xs)) {
			    xb = x1.containsNone(x2); // for debugging
			}
			break;
		    case retainAll:
			xs = XSet.of(x1).retainAll(x2);
			us = new UnicodeSet(u1).retainAll(u2);
			if (!assertEquals("retain:\t" + x1 + " ∩= " + x2 + "\t IFF " + u1 + " ∩= " + u2, uxTests.get(us), xs)) {
			    xb = x1.containsNone(x2); // for debugging
			}
			break;
		    }
		    // ∪ ∩
		}
	    }
	}
    }

    // hack for testing
    private static XSet<String> of(String a1) {
	XSet<String> result = new XSet<>();
	for (int ch : CharSequences.codePoints(a1)) {
	    if (ch != '[' && ch != ']') {
		result.add(UTF16.valueOf(ch));
	    }
	}
	return result;
    }
}
