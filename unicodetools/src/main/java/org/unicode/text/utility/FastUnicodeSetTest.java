package org.unicode.text.utility;

import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UnicodeSet;

public class FastUnicodeSetTest {

    static final UnicodeSet[] testList = {
        new UnicodeSet("[:whitespace:]"),
        new UnicodeSet("[a-f]"),
        new UnicodeSet("[:alphabetic:]"),
        new UnicodeSet("[[:alphabetic:][\\uFFFD]]"),
        new UnicodeSet("[:cn:]")
    };

    public static void main(String[] args) {
        new FastUnicodeSetTest().TestContains();
        // [[:alphabetic:][\uFFFD]]  containsAll [:cn:]
        UnicodeSet lastSet = testList[testList.length - 1];

        if (System.getProperty("DO_QUICK") != null) {
            for (final UnicodeSet set : testList) {
                timeContainsNone(set, lastSet, 10000);
                timeContainsNone(lastSet, set, 10000);
                timeContainsNone(set, set, 10000);
                lastSet = set;
            }
            if (true) {
                return;
            }
        }

        lastSet = testList[testList.length - 1];
        FastUnicodeSet lastAlt = new FastUnicodeSet(lastSet);
        for (final UnicodeSet set : testList) {
            System.out.println();
            System.out.println(
                    "Set:\t"
                            + set
                            + "\tsize:\t"
                            + set.size()
                            + "\tranges:\t"
                            + set.getRangeCount());
            final FastUnicodeSet alt = new FastUnicodeSet(set);
            verify(set, lastSet, alt, lastAlt);
            verify(set, set, alt, alt);
            timeContains(set, alt, 100);

            timeContainsAll(set, set, alt, alt, 10000);
            timeContainsAll(lastSet, set, lastAlt, alt, 10000);
            timeContainsAll(set, lastSet, alt, lastAlt, 10000);

            timeContainsNone(set, set, alt, alt, 10000);
            timeContainsNone(lastSet, set, lastAlt, alt, 10000);
            timeContainsNone(set, lastSet, alt, lastAlt, 10000);

            timeEquals(set, set, alt, alt, 10000);
            timeEquals(lastSet, set, lastAlt, alt, 10000);
            timeEquals(set, lastSet, alt, lastAlt, 10000);

            lastSet = set;
            lastAlt = alt;
        }
        System.out.println("DONE");
    }

    public void TestContains() {
        final int limit = 256; // combinations to test
        for (int i = 0; i < limit; ++i) {
            logln("Trying: " + i);
            final FastUnicodeSet x = bitsToSet(i);
            for (int j = 0; j < limit; ++j) {
                final FastUnicodeSet y = bitsToSet(j);
                final boolean containsNone = (i & j) == 0;
                final boolean containsAll = (i & j) == j;
                final boolean equals = i == j;
                if (containsNone != x.containsNone(y)) {
                    x.containsNone(y); // repeat for debugging
                    errln("FAILED: " + x + " containsSome " + y);
                }
                if (containsAll != x.containsAll(y)) {
                    x.containsAll(y); // repeat for debugging
                    errln("FAILED: " + x + " containsAll " + y);
                }
                if (equals != x.equals(y)) {
                    x.equals(y); // repeat for debugging
                    errln("FAILED: " + x + " equals " + y);
                }
            }
        }
    }

    /** Convert a bitmask to a UnicodeSet. */
    FastUnicodeSet bitsToSet(int a) {
        final UnicodeSet result = new UnicodeSet();
        for (int i = 0; i < 32; ++i) {
            if ((a & (1 << i)) != 0) {
                result.add((char) i, (char) i);
            }
        }
        return new FastUnicodeSet(result);
    }

    private void errln(String string) {
        System.out.println("Error: " + string);
    }

    private void logln(String string) {
        System.out.println("Log: " + string);
    }

    private static void verify(
            UnicodeSet set, UnicodeSet set2, FastUnicodeSet alt, FastUnicodeSet alt2) {
        verifyEquals(set, alt);
        verifyEquals(set2, alt2);

        if (set.containsAll(set2) != alt.containsAll(alt2)) {
            alt.containsAll(alt2);
            throw new IllegalArgumentException("Failure at: " + set + " containsAll " + set2);
        }
        if (set2.containsAll(set) != alt2.containsAll(alt)) {
            alt2.containsAll(alt);
            throw new IllegalArgumentException("Failure at: " + set2 + " containsAll " + set);
        }
        if (set.containsNone(set2) != alt.containsNone(alt2)) {
            alt.containsNone(alt2);
            throw new IllegalArgumentException("Failure at: " + set + " containsNone " + set2);
        }
        if (set2.containsNone(set) != alt2.containsNone(alt)) {
            alt2.containsNone(alt);
            throw new IllegalArgumentException("Failure at: " + set2 + " containsNone " + set);
        }
    }

    private static void verifyEquals(UnicodeSet set, FastUnicodeSet alt) {
        if (set.size() != alt.size()) {
            alt.size();
            throw new IllegalArgumentException("Bad size at");
        }
        for (int j = 0; j < 0x10FFFF; ++j) {
            if (set.contains(j) != alt.contains(j)) {
                alt.contains(j);
                throw new IllegalArgumentException("Failure at: " + j);
            }
        }
    }

    public static class Timer {
        long start = Long.MAX_VALUE;
        long delta = 0;
        long timeLimit = 1000; // 1 seconds
        long iterations;

        public void reset() {
            iterations = 0;
            start = Long.MAX_VALUE;
        }

        public void reset(long timeLimitInMillis) {
            timeLimit = timeLimitInMillis;
            iterations = 0;
            start = Long.MAX_VALUE;
        }

        public boolean insufficient() {
            delta = System.currentTimeMillis() - start;
            if (delta > timeLimit && iterations > 1) {
                return false;
            }
            iterations += iterations;
            ++iterations;
            System.gc();
            start = System.currentTimeMillis();
            return true;
        }

        public long iterations() {
            return iterations;
        }

        public double getDelta() {
            return delta / (double) iterations;
        }
    }

    static Timer timer = new Timer();

    private static boolean timeContains(UnicodeSet set, FastUnicodeSet alt, int iterations) {
        boolean result = false;

        for (timer.reset(); timer.insufficient(); ) {
            for (long i = timer.iterations(); i >= 0; --i) {
                for (int j = 0; j < 0x10FFFF; ++j) {
                    result ^= set.contains(j);
                }
            }
        }
        final double lastDelta = timer.getDelta();

        for (timer.reset(); timer.insufficient(); ) {
            for (long i = timer.iterations(); i >= 0; --i) {
                for (int j = 0; j < 0x10FFFF; ++j) {
                    result ^= alt.contains(j);
                }
            }
        }
        final double delta = timer.getDelta();

        show(set.toString(), "contains", "ch", "n/a", lastDelta, delta);
        return result;
    }

    private static void show(
            String set, String relation, String x, String value, double lastDelta, double delta) {
        System.out.println(
                set
                        + "\t"
                        + relation
                        + "\t"
                        + x
                        + "\t"
                        + value
                        + "\told:\t"
                        + lastDelta
                        + "\tnew:\t"
                        + delta
                        + "\t"
                        + percent.format(delta / lastDelta));
    }

    private static final NumberFormat percent = NumberFormat.getPercentInstance();

    private static boolean timeContainsAll(
            UnicodeSet set,
            UnicodeSet set2,
            FastUnicodeSet alt,
            FastUnicodeSet alt2,
            int iterations) {
        boolean result = false;

        for (timer.reset(); timer.insufficient(); ) {
            for (long i = timer.iterations(); i >= 0; --i) {
                result ^= set.containsAll(set2);
            }
        }
        final double lastDelta = timer.getDelta();

        for (timer.reset(); timer.insufficient(); ) {
            for (long i = timer.iterations(); i >= 0; --i) {
                result ^= alt.containsAll(alt2);
            }
        }
        final double delta = timer.getDelta();
        show(
                set.toString(),
                "containsAll",
                set2.toString(),
                String.valueOf(set.containsAll(set2)),
                lastDelta,
                delta);
        return result;
    }

    private static boolean timeContainsAll(UnicodeSet set, UnicodeSet set2, int iterations) {
        final boolean result = false;

        //    for (timer.reset(); timer.insufficient();) {
        //      for (long i = timer.iterations(); i >= 0; --i) {
        //        result ^= set.containsAllOld(set2);
        //      }
        //    }
        //    double lastDelta = timer.getDelta();
        //
        //    for (timer.reset(); timer.insufficient();) {
        //      for (long i = timer.iterations(); i >= 0; --i) {
        //        result ^= set.containsAll(set2);
        //      }
        //    }
        //    double delta = timer.getDelta();
        //    show(set.toString(), "containsAll", set2.toString(),
        // String.valueOf(set.containsAll(set2)), lastDelta, delta);
        return result;
    }

    private static boolean timeContainsNone(UnicodeSet set, UnicodeSet set2, int iterations) {
        final boolean result = false;

        //    for (timer.reset(); timer.insufficient();) {
        //      for (long i = timer.iterations(); i >= 0; --i) {
        //        result ^= set.containsNoneOld(set2);
        //      }
        //    }
        //    double lastDelta = timer.getDelta();
        //
        //    for (timer.reset(); timer.insufficient();) {
        //      for (long i = timer.iterations(); i >= 0; --i) {
        //        result ^= set.containsNone(set2);
        //      }
        //    }
        //    double delta = timer.getDelta();
        //    show(set.toString(), "containsNone", set2.toString(),
        // String.valueOf(set.containsNone(set2)), lastDelta, delta);
        return result;
    }

    private static boolean timeContainsNone(
            UnicodeSet set,
            UnicodeSet set2,
            FastUnicodeSet alt,
            FastUnicodeSet alt2,
            int iterations) {
        boolean result = false;

        for (timer.reset(); timer.insufficient(); ) {
            for (long i = timer.iterations(); i >= 0; --i) {
                result ^= set.containsNone(set2);
            }
        }
        final double lastDelta = timer.getDelta();

        for (timer.reset(); timer.insufficient(); ) {
            for (long i = timer.iterations(); i >= 0; --i) {
                result ^= alt.containsNone(alt2);
            }
        }
        final double delta = timer.getDelta();
        show(
                set.toString(),
                "containsNone",
                set2.toString(),
                String.valueOf(set.containsNone(set2)),
                lastDelta,
                delta);
        return result;
    }

    private static boolean timeEquals(
            UnicodeSet set,
            UnicodeSet set2,
            FastUnicodeSet alt,
            FastUnicodeSet alt2,
            int iterations) {
        boolean result = false;

        for (timer.reset(); timer.insufficient(); ) {
            for (long i = timer.iterations(); i >= 0; --i) {
                result ^= set.equals(set2);
            }
        }
        final double lastDelta = timer.getDelta();

        for (timer.reset(); timer.insufficient(); ) {
            for (long i = timer.iterations(); i >= 0; --i) {
                result ^= alt.equals(alt2);
            }
        }
        final double delta = timer.getDelta();
        show(
                set.toString(),
                "equals",
                set2.toString(),
                String.valueOf(set.equals(set2)),
                lastDelta,
                delta);
        return result;
    }
}
