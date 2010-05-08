package org.unicode.draft;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.icu.text.Collator;
import com.ibm.icu.util.ULocale;


public class CheckCollator {
  public static void main(String[] args) {
    Collator collator = Collator.getInstance(new ULocale("en-US"));
    Set<String> languages = new TreeSet();
    languages.addAll(Arrays.asList(ULocale.getISOLanguages()));
    for (String code : languages) {
      String name = ULocale.getDisplayName(code, ULocale.ENGLISH);
      if (!name.equals(code)) {
        System.out.println(code + "\t" + "Tier ?" + "\t" + name);
      }
    }

    String functionalLocale = Collator.getFunctionalEquivalent("collation", collator.getLocale(ULocale.ACTUAL_LOCALE)).toString();
    System.out.println(functionalLocale);

    String[] values = Collator.getKeywordValues("collation");
    //System.out.println("collation" + ":\t" + Arrays.asList(values));
    ULocale[] locales = Collator.getAvailableULocales();
    for (ULocale locale : locales) {
      String[] localeValues = Collator.getKeywordValuesForLocale("collation", locale, true);
      //System.out.println(locale + "\t" + "collation" + ":\t" + Arrays.asList(localeValues));
      ULocale functionalLocale2 = Collator.getFunctionalEquivalent("collation", locale);
      if (!functionalLocale2.equals(locale)) {
        System.out.println(locale + "\t=>\t" + functionalLocale2);
      }
    }
  }
}
