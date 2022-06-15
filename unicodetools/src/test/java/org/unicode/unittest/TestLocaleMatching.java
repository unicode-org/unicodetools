package org.unicode.unittest;

import com.google.common.base.Splitter;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.ULocale.Builder;
import com.ibm.icu.util.ULocale.Minimize;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

public class TestLocaleMatching extends TestFmwkMinusMinus {

    static final Splitter COMMA_SPACE_SPLITTER = Splitter.on(Pattern.compile(",\\s*|\\s+"));

    @Test
    public void TestBasic() {
        String[][] tests = {
            //                //{"preferred locales", "supported locales", "best supported (ui)",
            // "best preferred", "best services"},                {"de-CH-u-, sv-u-co-reformed",
            // "sv, de", "sv", "sv-u-co-reformed", "="},
            //                {"de-CH-u-co-phonebk, sv-u-co-reformed", "en, sv", "sv",
            // "sv-u-co-reformed", "="},
            //                {"de-CH-u-co-phonebk, sv-u-cf-account", "sv, de", "sv",
            // "sv-u-cf-account", "="},
            //                {"en-IN-u-cu-eur, sv", "en, en-GB, fr", "en-GB", "en-IN-u-cu-eur",
            // "="},
            //                //                {"en-GB-u-cu-eur, sv", "en, en-IN", "en",
            // "en-GB-u-cu-eur", "="},
            //                {"es-MX, sv", "es-ES, es-419, fr", "es-419", "es-MX", "="},
            //                {"sr-ME-u-cu-eur, sv", "sr, sr-Latn, fr", "sr-Latn", "sr-ME-u-cu-eur",
            // "="},
            //                {"sr-ME-u-cu-eur, sv", "sr, fr", "sr", "sr-ME-u-cu-eur",
            // "sr-Cyrl-ME-u-cu-eur"},
            // {"preferred locales", "supported locales", "best supported (ui)", "best preferred",
            // "best services"},
            {"de-CH-u-, sv-u-co-reformed", "sv, de", "sv", "sv-u-co-reformed", " ="},
            {"de-CH-u-co-phonebk, sv-u-cf-account", "en, sv", "sv", "sv-u-cf-account", " ="},
            {"de-CH-u-co-phonebk, sv-u-cf-account", "sv, de", "sv", "sv-u-cf-account", " ="},
            {"en-IN-u-hc-h23, sv", "en, en-GB", "en-GB", "en-IN-u-hc-h23", " ="},
            {"sr-ME-u-hc-h23, sv", "sr, sr-Latn, fr", "sr-Latn", "sr-ME-u-hc-h23", " ="},
            {"sr-ME-u-hc-h23, sv", "sr, fr", "sr", "sr-ME-u-hc-h23", "sr-Cyrl-ME-u-hc-h23"},
        };
        int item = 0;
        for (String[] test : tests) {
            ++item;
            Set<ULocale> preferred_locales = new LinkedHashSet<>();
            COMMA_SPACE_SPLITTER
                    .splitToList(test[0])
                    .forEach(x -> preferred_locales.add(ULocale.forLanguageTag(x)));

            Set<ULocale> supported_locales = new LinkedHashSet<>();
            COMMA_SPACE_SPLITTER
                    .splitToList(test[1])
                    .forEach(x -> supported_locales.add(ULocale.forLanguageTag(x)));

            ULocale expected_best_supported = ULocale.forLanguageTag(test[2]);
            ULocale expected_best_preferred = ULocale.forLanguageTag(test[3]);
            ULocale expected_best_services =
                    ULocale.forLanguageTag(!test[4].trim().equals("=") ? test[4] : test[3]);

            //            XLocaleMatcher matcher = new XLocaleMatcher.Builder()
            //                    .setSupportedLocales(supported_locales)
            //                    .setDemotionPerAdditionalDesiredLocale(0)
            //                    .build();
            //            Output<ULocale> actual_best_preferred = new Output<>();
            //            ULocale actual_best_supported = matcher.getBestMatch(preferred_locales,
            // actual_best_preferred);
            //
            //            String message = item + ") pref: " + preferred_locales + ", supp: " +
            // supported_locales;
            //            assertEquals(message + " => best-supp", expected_best_supported,
            // actual_best_supported);
            //
            //            assertEquals(message + " => best-pref", expected_best_preferred,
            // actual_best_preferred.value);
            //
            //            ULocale actual_best_services = combine(expected_best_supported,
            // expected_best_preferred);
            //            assertEquals(item + ") pref: " + expected_best_preferred + ", supp: " +
            // expected_best_supported + " => best-serv", expected_best_services,
            // actual_best_services);
        }
    }

    /**
     * Combine features of the desired locale and supported locale for the best services locale, and
     * return result.
     */
    public static ULocale combine(ULocale supported, ULocale desired) {
        // for examples of extensions, variants, see
        //  http://unicode.org/repos/cldr/tags/latest/common/bcp47/
        //  http://unicode.org/repos/cldr/tags/latest/common/validity/variant.xml
        if (desired == null) {
            return supported;
        }
        String desiredLanguage = desired.getLanguage();
        String supportedLanguage = supported.getLanguage();
        String desiredScript = desired.getScript();
        String supportedScript = supported.getScript();
        String desiredCountry = desired.getCountry();
        String supportedCountry = supported.getCountry();

        if (desiredLanguage.equals(supportedLanguage)
                && desiredScript.equals(supportedScript)
                && desiredCountry.equals(supportedCountry)) {
            return desired;
        }

        // we know now that the LSRs are different.
        // if the regions are the same, just combine the other features and return

        Builder builder =
                new ULocale.Builder().setLocale(desired); // gets region, variants, extensions, etc.

        if (desiredCountry.equals(supportedCountry)) {
            return builder.setLanguage(supportedLanguage)
                    .setScript(supportedScript)
                    .setRegion(supportedCountry)
                    .build();
        }

        // otherwise, use the desired region, but adjust the language/script
        // need to do this in case the default script for the country is different

        ULocale maxSupp = ULocale.addLikelySubtags(supported);
        ULocale maxService =
                new ULocale.Builder().setLocale(maxSupp).setRegion(desiredCountry).build();
        ULocale minService = ULocale.minimizeSubtags(maxService, Minimize.FAVOR_REGION);

        // now set the language / script
        return builder // gets variants, extensions
                .setLanguage(minService.getLanguage())
                .setScript(minService.getScript())
                .build();
    }
}
