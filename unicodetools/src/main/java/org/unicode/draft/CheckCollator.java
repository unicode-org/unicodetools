package org.unicode.draft;

import com.ibm.icu.text.Collator;
import com.ibm.icu.util.ULocale;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

public class CheckCollator {
    public static void main(String[] args) {
        final Collator collator = Collator.getInstance(new ULocale("en-US"));
        final Set<String> languages = new TreeSet();
        languages.addAll(Arrays.asList(ULocale.getISOLanguages()));
        for (final String code : languages) {
            final String name = ULocale.getDisplayName(code, ULocale.ENGLISH);
            if (!name.equals(code)) {
                System.out.println(code + "\t" + "Tier ?" + "\t" + name);
            }
        }

        final String functionalLocale =
                Collator.getFunctionalEquivalent(
                                "collation", collator.getLocale(ULocale.ACTUAL_LOCALE))
                        .toString();
        System.out.println(functionalLocale);

        final String[] values = Collator.getKeywordValues("collation");
        // System.out.println("collation" + ":\t" + Arrays.asList(values));
        final ULocale[] locales = Collator.getAvailableULocales();
        for (final ULocale locale : locales) {
            final String[] localeValues =
                    Collator.getKeywordValuesForLocale("collation", locale, true);
            // System.out.println(locale + "\t" + "collation" + ":\t" +
            // Arrays.asList(localeValues));
            final ULocale functionalLocale2 = Collator.getFunctionalEquivalent("collation", locale);
            if (!functionalLocale2.equals(locale)) {
                System.out.println(locale + "\t=>\t" + functionalLocale2);
            }
        }
    }
}
