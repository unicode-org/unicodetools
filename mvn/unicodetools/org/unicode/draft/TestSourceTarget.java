package org.unicode.draft;

import java.util.Collections;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;

public class TestSourceTarget extends TestFmwk {

    public static void main(String[] args) {
        new TestSourceTarget().run(args);
    }

    static final UnicodeSet TEST_SET = new UnicodeSet("[^[:c:]-[:cc:]]");

    public void TestSets() {
        for (final String id : Collections.list(Transliterator.getAvailableIDs())) {
            final UnicodeSet testSource = new UnicodeSet();
            final UnicodeSet testTarget = new UnicodeSet();
            final Transliterator t = Transliterator.getInstance(id);
            final UnicodeSet source = t.getSourceSet();
            final UnicodeSet target = t.getTargetSet();
            if (source.size() == 0 && target.size() == 0) {
                errln(id + "\tgetSourceSet & getTargetSet are empty");
            } else if (source.size() == 0) {
                errln(id + "\tgetSourceSet is empty");
            } else if (source.size() == 0) {
                errln(id + "\tgetTargetSet is empty");
            } else {
                testSource.clear();
                testTarget.clear();
                for (final String s : TEST_SET) {
                    final String u = t.transform(s);
                    if (!s.equals(u)) {
                        testSource.add(s);
                        testTarget.add(u);
                    }
                }
                boolean hasError = false;
                if (!source.containsAll(testSource)) {
                    testSource.removeAll(source);
                    errln(id + "\tgetSourceSet doesn't contain mapped characters:\t" + testSource.toPattern(false));
                    hasError = true;
                }
                if (!target.containsAll(testTarget)) {
                    testTarget.removeAll(target);
                    errln(id + "\tgetTargetSet doesn't contain mapped-to characters:\t" + testSource.toPattern(false));
                    hasError = true;
                }
                if (!hasError) {
                    logln(id + "\tok");
                }
            }
        }
    }

    //    // "Fixed" versions
    //
    //    static UnicodeSet getSourceSet(Transliterator t) {
    //        Transliterator[] subTransliterators = t.getElements();
    //        if (subTransliterators.length == 1 && subTransliterators[0] == t) {
    //            return t.getSourceSet();
    //        } else {
    //            UnicodeSet sources = new UnicodeSet();
    //            for (Transliterator s : subTransliterators) {
    //                UnicodeSet source = getSourceSet(s);
    //                sources.addAll(source);
    //            }
    //            // TODO: if s1 produces ABC, what about chaining?
    //            UnicodeFilter filter = t.getFilter();
    //            if (filter != null) {
    //                sources.retainAll((UnicodeSet)filter); // TODO fix for arbitrary filters
    //            }
    //            return sources;
    //        }
    //    }
    //
    //    static UnicodeSet getTargetSet(Transliterator t) {
    //        Transliterator[] subTransliterators = t.getElements();
    //        if (subTransliterators.length == 1 && subTransliterators[0] == t) {
    //            return t.getTargetSet();
    //        } else {
    //            UnicodeSet sources = new UnicodeSet();
    //            for (Transliterator s : subTransliterators) {
    //                UnicodeSet source = getTargetSet(s);
    //                sources.addAll(source);
    //            }
    //            return sources;
    //        }
    //    }
}
