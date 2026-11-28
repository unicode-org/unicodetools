package org.unicode.text.UCD;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ibm.icu.text.UnicodeSet;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UnicodeProperty;
import org.unicode.text.utility.Settings;

public class TestCaseFolding {
    @ParameterizedTest
    @ValueSource(strings = {"15.0.0", "15.1.0", Settings.lastVersion, Settings.latestVersion})
    void testCaseFoldingMatchesData(String version) {
        // In particular, the simple mappings added in 15.1 must not leak into 15.0.
        UCD ucd = UCD.make(version);
        IndexUnicodeProperties properties = IndexUnicodeProperties.make(version);
        UnicodeProperty simple = properties.getProperty(UcdProperty.Simple_Case_Folding);
        UnicodeProperty full = properties.getProperty(UcdProperty.Case_Folding);
        UnicodeSet simpleDifferences = new UnicodeSet();
        UnicodeSet fullDifferences = new UnicodeSet();
        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            if (!simple.getValue(cp).equals(ucd.getCase(cp, UCD_Types.SIMPLE, UCD_Types.FOLD))) {
                simpleDifferences.add(cp);
            }
            if (!full.getValue(cp).equals(ucd.getCase(cp, UCD_Types.FULL, UCD_Types.FOLD))) {
                fullDifferences.add(cp);
            }
        }
        assertEquals(new UnicodeSet(), simpleDifferences, version + " Simple_Case_Folding");
        assertEquals(new UnicodeSet(), fullDifferences, version + " Case_Folding");
    }
}
