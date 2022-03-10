package org.unicode.jsptest;

import com.ibm.icu.util.ULocale;

import org.junit.jupiter.api.Test;
import org.unicode.jsp.LanguageCode;
import org.unicode.unittest.TestFmwkMinusMinus;

public class TestLanguageid extends TestFmwkMinusMinus {

    @Test
    public void TestParse() {
        String results;
        results = LanguageCode.validate("pap-CW",new ULocale("en"));
        if (!assertTrue("", results.contains("Curaçao"))) {
            errln(results);
        }

        results = LanguageCode.validate("$, eng-840, fr-fr",new ULocale("en"));
        assertTrue("", results.contains("target='languageid'>fr-FR</b>"));
    }
}
