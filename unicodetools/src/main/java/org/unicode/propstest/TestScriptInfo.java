package org.unicode.propstest;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import org.unicode.props.ScriptInfo;
import org.unicode.props.UcdPropertyValues.Script_Values;
import org.unicode.text.utility.Settings;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.UnicodeSet;

public class TestScriptInfo extends TestFmwk {

    public static void main(String[] args) {
        new TestScriptInfo().run(args);
    }

    public void TestMain() {
        String[][] testCases = {
            // Format is testString, expected scripts, expected alternates, expected numerics (if any)
            { "ab c", "ASCII", "Latn Common" },
            { "a12", "ASCII", "Common Latn", "", "[0]" },
            { "ー", "HIGHLY_RESTRICTIVE", "", "Kana Hira" },
            { "カー", "HIGHLY_RESTRICTIVE", "Kana" },
            { "ーカ", "HIGHLY_RESTRICTIVE", "Kana" },
            { "かー", "HIGHLY_RESTRICTIVE", "Hira" },
            { "ーか", "HIGHLY_RESTRICTIVE", "Hira" },
            { "かーカa", "HIGHLY_RESTRICTIVE", "Kana Hira Latn" },
            { "a、 〃", "HIGHLY_RESTRICTIVE", "Zyyy Latn", "Bopo Hani Hang Hira Kana" },
            { "a、 〃カ一", "HIGHLY_RESTRICTIVE", "Zyyy Latn Kana Hani" },
            { "a가一", "HIGHLY_RESTRICTIVE", "Hani Hang Latn" },
            { "aㄅ一", "HIGHLY_RESTRICTIVE", "Bopo Hani Latn" },
            { "٢", "HIGHLY_RESTRICTIVE", "", "Arab Thaa", "[٠]" },
            { "٢\u0670", "HIGHLY_RESTRICTIVE", "", "Arab Thaa; Arab Syrc", "[٠]" },
            { "٢\u0670ـ", "HIGHLY_RESTRICTIVE", "", "Arab Thaa; Arab Syrc", "[٠]" },
            { "٢\u0670ـ،", "HIGHLY_RESTRICTIVE", "", "Arab Thaa; Arab Syrc", "[٠]" },
            { "AᎪ", "MINIMALLY_RESTRICTIVE", "Cher Latn" },
            { "aА가一", "MINIMALLY_RESTRICTIVE", "Cyrl Hani Hang Latn" },
            { "AᎪА♥", "MINIMALLY_RESTRICTIVE", "Zyyy Cher Cyrl Latn" },
            { "a1२", "UNRESTRICTIVE", "Zyyy Latn", "Devanagari Kaithi Mahajani", "[0०]" },
            { "a1٢", "UNRESTRICTIVE", "Zyyy Latn", "Arab Thaa", "[0٠]" },
        };
        ScriptInfo actualInfo = new ScriptInfo(Settings.latestVersion);

        int item = 0;
        for (String[] testCase : testCases) {
            ++item;
            final String identifier = testCase[0];
            final String actualStatusString = testCase[1];
            final String scriptsString = testCase[2];
            final String multiscriptsString = testCase.length < 4 ? "" : testCase[3];
            final String numericString = testCase.length < 5 ? "[]" : testCase[4];

            actualInfo.setIdentifier(identifier);
//            IdentifierStatus actualStatus = actualInfo.getRestrictionLevel();
//            IdentifierStatus expectedStatus = IdentifierStatus.valueOf(actualStatusString);

            EnumSet<Script_Values> expectedScripts = ScriptInfo.parseScripts(scriptsString);
            Set<EnumSet<Script_Values>> expectedMultiscripts = ScriptInfo.parseAlternates(multiscriptsString);
            UnicodeSet expectedNumerics = new UnicodeSet(numericString);

            if (!actualInfo.getScripts().equals(expectedScripts)
                //|| !actualStatus.equals(expectedStatus)
                || !actualInfo.getAlternates().equals(expectedMultiscripts)
                || !actualInfo.getNumerics().equals(expectedNumerics)) {
                errln("(" + item + ") " + Arrays.asList(testCase) + " !=" + actualInfo);
            }
        }
    }

}
