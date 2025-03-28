package org.unicode.text.UCD;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ibm.icu.text.UnicodeSet;
import java.text.ParsePosition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestVersionedSymbolTable {
    @BeforeEach
    void setUp() {
        UnicodeSet.setDefaultXSymbolTable(VersionedSymbolTable.forDevelopment());
    }

    @AfterEach
    void tearDown() {
        UnicodeSet.setDefaultXSymbolTable(NO_PROPS);
    }

    @Test
    void testIntroductionExamples() {
        assertThatUnicodeSet("\\p{XID_Continue}")
                .contains("a")
                .contains("α")
                .contains("𒀀")
                .doesNotContain("'")
                .doesNotContain(",");
        assertThatUnicodeSet("[\\p{lb=OP}-[\\p{ea=F}\\p{ea=W}\\p{ea=H}]]")
                .contains("(")
                .doesNotContain("【");
        assertThatUnicodeSet(
                        "[\\p{Other_ID_Start}\\p{Other_ID_Continue}"
                                + "\\p{L}\\p{Nl}\\p{Mn}\\p{Mc}\\p{Nd}\\p{Pc}"
                                + "-\\p{Pattern_Syntax}"
                                + "-\\p{Pattern_White_Space}]")
                .contains("A")
                .contains("_")
                .contains("᧚")
                .doesNotContain("\u2E2F")
                .doesNotContain("$");
        assertThatUnicodeSet("[\\p{L}\\p{Nl}\\p{Mn}\\p{Mc}\\p{Nd}\\p{Pc}-[\\u2E2F]]")
                .contains("A")
                .contains("_")
                .doesNotContain("᧚")
                .doesNotContain("\u2E2F")
                .doesNotContain("$");
    }

    /** Helper class for testing multiple properties of the same UnicodeSet. */
    private static class UnicodeSetTestFluent {
        UnicodeSetTestFluent(String expression) {
            this.expression = expression;
            ParsePosition parsePosition = new ParsePosition(0);
            set = new UnicodeSet(expression, parsePosition, VersionedSymbolTable.forDevelopment());
            set.complement().complement();
        }

        public <T extends CharSequence> UnicodeSetTestFluent isEqualTo(UnicodeSet collection) {
            assertTrue(set.equals(collection));
            return this;
        }

        public <T extends CharSequence> UnicodeSetTestFluent doesNotContain(CharSequence element) {
            assertFalse(
                    set.contains(element),
                    element + " ∉ " + expression + " = " + set.toPattern(true));
            return this;
        }

        public UnicodeSetTestFluent contains(CharSequence element) {
            assertTrue(
                    set.contains(element),
                    element + " ∈ " + expression + " = " + set.toPattern(true));
            return this;
        }

        private UnicodeSet set;
        private String expression;
    }

    private UnicodeSetTestFluent assertThatUnicodeSet(String expression) {
        return new UnicodeSetTestFluent(expression);
    }

    static UnicodeSet.XSymbolTable NO_PROPS =
            new UnicodeSet.XSymbolTable() {
                @Override
                public boolean applyPropertyAlias(
                        String propertyName, String propertyValue, UnicodeSet result) {
                    throw new IllegalArgumentException(
                            "Don't use any ICU Unicode Properties! "
                                    + propertyName
                                    + "="
                                    + propertyValue);
                }
                ;
            };
}
