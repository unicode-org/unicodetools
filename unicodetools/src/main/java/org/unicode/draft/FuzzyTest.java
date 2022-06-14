package org.unicode.draft;

import java.util.Random;

public class FuzzyTest {
    public static void main(String[] args) {
        FuzzyNumber aa = FuzzyNumber.parse("1");
        FuzzyNumber expected = new FuzzyNumber(1, 0.5, 1.5);
        assertEquals("aa", expected, aa);
        final FuzzyNumber aaa = FuzzyNumber.parse("1.");
        assertEquals("aa", expected, aaa);

        aa = FuzzyNumber.parse("1.0");
        expected = new FuzzyNumber(1.0, 0.95, 1.05);
        assertEquals("aa", expected, aa);

        final Random r = new Random(0);
        for (int k = 0; k < 1000; ++k) {
            final FuzzyNumber a = new FuzzyNumber(r.nextInt(200) / 10.0D - 10, 0.5);
            final FuzzyNumber b = new FuzzyNumber(r.nextInt(200) / 10.0D - 10, 0.5);
            final FuzzyNumber added = a.add(b);
            final FuzzyNumber invert = a.invert();
            final FuzzyNumber subtracted = a.subtract(b);
            final FuzzyNumber multiplied = a.multiply(b);
            final FuzzyNumber divided = a.divide(b);
            if (false) {
                System.out.println(
                        "1/"
                                + a
                                + " = "
                                + invert
                                + "\t\t"
                                + a
                                + " + "
                                + b
                                + " = "
                                + added
                                + "\t\t"
                                + a
                                + " - "
                                + b
                                + " = "
                                + subtracted
                                + "\t\t"
                                + a
                                + " × "
                                + b
                                + " = "
                                + multiplied
                                + "\t\t"
                                + a
                                + " ÷ "
                                + b
                                + " = "
                                + divided);
            }

            for (int i = 1; i < 10; ++i) {
                final double foo = (a.lower * i + a.upper * (10 - i)) / 10;
                for (int j = 1; j < 10; ++j) {
                    final double bar = (b.lower * j + b.upper * (10 - j)) / 10;
                    if (!assertCovers("invert", invert, 1 / foo)) {
                        final FuzzyNumber invert2 = a.invert();
                        assertCovers("invert", invert, 1 / foo);
                    }
                    assertCovers("add", added, foo + bar);
                    assertCovers("subtract", subtracted, foo - bar);
                    assertCovers("multiply", multiplied, foo * bar);
                    assertCovers("divide", divided, foo / bar);
                }
            }
        }
    }

    private static void assertEquals(String string, FuzzyNumber aa, FuzzyNumber bb) {
        if (!equals(aa, bb)) {
            System.out.println("FAILURE " + string + "\t" + aa + " != " + bb);
        } else {
            System.out.println(string + "\t" + aa + " = " + bb);
        }
    }

    private static boolean equals(FuzzyNumber aa, FuzzyNumber bb) {
        // TODO Auto-generated method stub
        return aa.value == bb.value && aa.lower == bb.lower && aa.upper == bb.upper;
    }

    private static boolean assertCovers(String title, FuzzyNumber added, double d) {
        if (added.value < added.lower || added.value > added.upper) {
            System.out.println(title + " invalid:\t" + added);
            return false;
        }
        if (d < added.lower || d > added.upper) {
            System.out.println(title + " fails:\t" + added + "\t" + d);
            return false;
        }
        return true;
    }
}
