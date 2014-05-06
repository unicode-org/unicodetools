package org.unicode.jsptest;

import org.unicode.jsp.LanguageCode;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.util.ULocale;

public class TestLanguageid extends TestFmwk {
    public static void main(String[] args) {
        new TestLanguageid().run(args);
    }
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
