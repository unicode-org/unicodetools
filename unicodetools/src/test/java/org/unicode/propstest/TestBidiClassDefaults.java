package org.unicode.propstest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Bidi_Class_Values;

public class TestBidiClassDefaults {
    @ParameterizedTest
    @CsvSource({
        "3.1.1, Arabic_Letter, Left_To_Right",
        "3.2.0, Arabic_Letter, Left_To_Right",
        "4.0.0, Left_To_Right, Right_To_Left"
    })
    void testHistoricalBidiClass(
            String version, Bidi_Class_Values expectedFdd0, Bidi_Class_Values expected07c0) {
        final var bidi = IndexUnicodeProperties.make(version).getProperty(UcdProperty.Bidi_Class);

        // Unassigned code points with regional defaults, omitted from the pre-4.0 files.
        assertEquals("Right_To_Left", bidi.getValue(0x0590));
        assertEquals("Right_To_Left", bidi.getValue(0xFB37));
        assertEquals("Arabic_Letter", bidi.getValue(0x070E));
        assertEquals("Arabic_Letter", bidi.getValue(0x0750));
        assertEquals("Arabic_Letter", bidi.getValue(0x07B2));
        assertEquals("Arabic_Letter", bidi.getValue(0xFBC3));
        assertEquals("Arabic_Letter", bidi.getValue(0xFE75));
        assertTrue(bidi.getSet(Bidi_Class_Values.Right_To_Left).contains(0x0590));

        // An ordinary unassigned code point still has the overall default, L.
        assertEquals("Left_To_Right", bidi.getValue(0x0378));

        // The regional defaults must not overwrite explicit assignments in these ranges.
        assertEquals("Nonspacing_Mark", bidi.getValue(0x0591));
        assertEquals("Arabic_Number", bidi.getValue(0x0660));
        assertEquals("Boundary_Neutral", bidi.getValue(0xFEFF));

        // These defaults did change in 4.0; do not apply modern defaults to older versions.
        assertEquals(expectedFdd0.toString(), bidi.getValue(0xFDD0));
        assertEquals(expected07c0.toString(), bidi.getValue(0x07C0));
    }
}
