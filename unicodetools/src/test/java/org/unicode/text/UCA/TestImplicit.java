package org.unicode.text.UCA;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Block_Values;
import org.unicode.props.UnicodeProperty;

public class TestImplicit {
    @Test
    void testImplicitWeightRanges() {
        // https://github.com/unicode-org/unicodetools/issues/717
        UCA uca = UCA.getDucetCollator();
        UnicodeProperty blocks =
                IndexUnicodeProperties.make(uca.getUCDVersion()).getProperty(UcdProperty.Block);
        assertFalse(
                uca.implicit.declaredRanges.isEmpty(),
                "No @implicitweights directives found in allkeys.txt");
        for (Implicit.DeclaredRange range : uca.implicit.declaredRanges) {
            String description = String.format("%04X..%04X", range.start(), range.end());
            // Named blocks are contiguous and are checked by TestCodeInvariants.testBlockRanges().
            // A directive may span several adjacent blocks, such as Jurchen and Jurchen Radicals.
            if (range.start() > 0) {
                assertNotEquals(
                        blocks.getValue(range.start() - 1),
                        blocks.getValue(range.start()),
                        "Implicit-weight range starts inside a block: " + description);
            }
            if (range.end() < Character.MAX_CODE_POINT) {
                assertNotEquals(
                        blocks.getValue(range.end()),
                        blocks.getValue(range.end() + 1),
                        "Implicit-weight range ends inside a block: " + description);
            }
            // Include unassigned code points when checking for gaps between blocks.
            for (int cp = range.start(); cp <= range.end(); ++cp) {
                assertNotEquals(
                        Block_Values.No_Block.toString(),
                        blocks.getValue(cp),
                        "Implicit-weight range includes No_Block: " + description);
            }
        }
    }
}
