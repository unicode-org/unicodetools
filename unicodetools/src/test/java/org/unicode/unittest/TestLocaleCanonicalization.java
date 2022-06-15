package org.unicode.unittest;

import com.ibm.icu.util.ULocale;
import java.util.Locale;
import java.util.Map.Entry;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.unicode.unittest.LocaleCanonicalizer.ErrorChoice;

public class TestLocaleCanonicalization extends TestFmwkMinusMinus {
    private static final boolean DEBUG = false;
    private static final boolean GEN_TEST_FILE = true;

    @Disabled("Broken")
    @Test
    public void TestCanonicalize() {
        int line = 0;
        AliasDataCldr aliasDataSource = new AliasDataCldr();
        LocaleCanonicalizer lc = new LocaleCanonicalizer(aliasDataSource, ErrorChoice.ignoreErrors);
        for (Entry<String, String> test : aliasDataSource.generateTests().entrySet()) {
            String sourceString = test.getKey();
            ULocale source = ULocale.forLanguageTag(sourceString);
            String expected = test.getValue().toLowerCase(Locale.ROOT);
            ULocale actual;
            try {
                actual = lc.canonicalize(source);
            } catch (Exception e) {
                try { // debugging
                    lc.canonicalize(source);
                } catch (Exception e2) {
                }
                assertEquals(++line + ") " + sourceString, null, e.getMessage());
                continue;
            }
            assertEquals(
                    ++line + ") " + sourceString,
                    expected,
                    actual.toLanguageTag().toLowerCase(Locale.ROOT));
        }
    }
}
