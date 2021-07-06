package org.unicode.draft;
import java.util.Date;
import java.util.EnumSet;

import com.ibm.icu.text.CanonicalIterator;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.util.ULocale;

import org.junit.jupiter.api.Test;


public class TestDateGenerator {
    enum Foo {
        A, B, C;
        static final EnumSet<Foo> ALL = EnumSet.allOf(Foo.class);
    }
    CanonicalIterator a;
    static Date testDate = new Date();
    public static void main(String[] args) {
        System.out.println(Foo.ALL.contains(Foo.A));
        for (final ULocale locale : new ULocale[]{ULocale.KOREAN, ULocale.CHINESE, ULocale.TRADITIONAL_CHINESE, ULocale.JAPANESE}) {
            System.out.println(locale + "\t" + locale.getDisplayName());
            test("MMMd", locale);
            test("dMMM", locale);
        }
    }

    private static void test(String test, ULocale locale) {
        final DateTimePatternGenerator foo = DateTimePatternGenerator.getInstance(locale);
        final String pattern = foo.getBestPattern(test);
        final DateFormat df = DateFormat.getPatternInstance(test, locale);
        final String formatted = df.format(testDate);
        System.out.println("\t" + test + " => " + pattern + " => " + formatted);
    }
}
