package org.unicode.jsptest;

import com.ibm.icu.util.ULocale;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.unicode.jsp.LanguageCode;

public class TestLanguageid {

    @Test
    public void TestParse() {
        {
            final String results = LanguageCode.validate("pap-CW", new ULocale("en"));
            final String expected = "Curaçao";
            assertContains(results, expected);
        }

        {
            final String results = LanguageCode.validate("$, eng-840, fr-fr", new ULocale("en"));
            final String expected = "target='languageid'>fr-FR</a></b>";
            assertContains(results, expected);
        }
    }

    private void assertContains(final String results, final String expected) {
        assertTrue(results.contains(expected), () -> results + " did not contain " + expected);
    }
}
