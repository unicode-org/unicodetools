package org.unicode.draft;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.text.utility.Utility;

import com.ibm.icu.text.UnicodeSet;

public class TestUnicodeSet {

    static final UnicodeSet POSSIBLY_INVISIBLE = new UnicodeSet("[[:cc:][:di:]-[:whitespace:]]").freeze();

    static String replace(String source, UnicodeSet matcher, String replacement) {
        if (matcher.containsNone(source)) { // common case. can optimize further on later version of ICU
            return source;
        }
        final StringBuilder result = new StringBuilder();
        int cp;
        for (int i = 0; i < source.length(); i += Character.charCount(cp)) {
            cp = source.codePointAt(i);
            if (matcher.contains(cp)) {
                result.append(replacement);
            } else {
                result.appendCodePoint(cp);
            }
        }
        return result.toString();
    }

    public static void main(String[] args) {
        final String testdi = "abc\u0000def";
        final String filtered = replace(testdi, POSSIBLY_INVISIBLE, "");
        System.out.println(Utility.hex(filtered));

        final UnicodeSet foo = new UnicodeSet("[a-b d-g {ch} {zh}]");
        for (final String s : foo) {
            System.out.println("us\t" + s);
        }
        final TreeSet<String> target = new TreeSet<String>();
        foo.addAllTo(target);
        for (final String s : target) {
            System.out.println("ts\t" + s);
        }

        System.out.println("expect equal?\t" + UnicodeSet.compare(foo,target));

        final UnicodeSet fii = new UnicodeSet("[a d-g {ch} {zh}]");
        System.out.println("expect unequal?\t" + foo.compareTo(fii));
        System.out.println("expect unequal?\t" + UnicodeSet.compare(foo,fii));

        System.out.println("expect unequal?\t" + fii.compareTo(foo));
        System.out.println("expect unequal?\t" + UnicodeSet.compare(fii,foo));

        final int max = 1000;
        final Set<String> test1 = new HashSet<String>(max);
        final Set<String> test2 = new HashSet<String>(max);
        for (int i = 0; i <= max; ++i) {
            test1.add("a" + i);
            test2.add("a" + (max - i)); // add in reverse order
        }
        System.out.println("expect equal?\t" + UnicodeSet.compare(test1,test2));
        final List<String> test1a = new ArrayList<String>(test1);
        final List<String> test2a = new ArrayList<String>(test2);
        System.out.println("expect equal?\t" + UnicodeSet.compare(test1a,test2a));
    }
}
