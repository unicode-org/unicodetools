package org.unicode.text.UCD;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ibm.icu.impl.UnicodeMap;
import com.ibm.icu.util.VersionInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.unicode.props.UnicodeProperty;
import org.unicode.text.utility.Settings;

public class TestAge {
    @ParameterizedTest
    @ValueSource(strings = {Settings.lastVersion, Settings.latestVersion})
    void testHistoricalAllocation(String unicodeVersion) {
        UCD current = UCD.make(unicodeVersion);
        UCD[] history = new UCD[UCD_Types.LIMIT_AGE];
        for (int i = UCD_Types.AGE11; i < history.length; ++i) {
            String version = UCD_Types.AGE_VERSIONS[i];
            if (VersionInfo.getInstance(version).compareTo(current.getVersionInfo()) > 0) {
                break;
            }
            history[i] = UCD.make(version);
        }

        // Keep the former derivation as an independent check of every code point.
        UnicodeMap<String> expected = new UnicodeMap<>();
        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            expected.put(cp, historicalAge(cp, history));
        }
        UnicodeProperty age =
                ToolUnicodePropertySource.make(current.getVersion()).getProperty("Age");
        assertEquals(expected, age.getUnicodeMap());
    }

    @Test
    void testAdditionsAndRemovals() {
        VersionInfo currentVersion = VersionInfo.getInstance(19, 0);
        String currentAge = "V19_0";
        int added = 0x2FA20;
        int removed = 0x16D82;

        UnicodeMap<String> input =
                new UnicodeMap<String>()
                        .putAll(0, 0x10FFFF, "Unassigned")
                        .put(removed, currentAge)
                        .freeze();
        UnicodeMap<String> result =
                ToolUnicodePropertySource.deriveAge(
                        currentVersion,
                        cp -> cp == added,
                        new UnicodeProperty.UnicodeMapProperty().set(input));

        assertEquals(currentAge, result.get(added));
        assertEquals("Unassigned", result.get(removed));
        // The input Age map must remain unchanged.
        assertEquals("Unassigned", input.get(added));
        assertEquals(currentAge, input.get(removed));
    }

    private static String historicalAge(int codePoint, UCD[] history) {
        for (int i = UCD_Types.AGE11; i < history.length && history[i] != null; ++i) {
            if (history[i].isAllocated(codePoint)) {
                if (i == UCD_Types.AGE11 && !history[i + 1].isAllocated(codePoint)) {
                    // Deallocations in Unicode 2.
                    continue;
                }
                return UCD_Names.LONG_AGE[i];
            } else if (i == UCD_Types.AGE11
                    && ((codePoint >= 0xE000 && codePoint <= 0xF8FF)
                            || (codePoint >= 0xF900 && codePoint <= 0xFA2D))) {
                // Private use and CJK compatibility ideographs, not overt in UnicodeData 1.1.5.
                return UCD_Names.LONG_AGE[i];
            }
        }
        return UCD_Names.LONG_AGE[UCD_Types.UNKNOWN];
    }
}
