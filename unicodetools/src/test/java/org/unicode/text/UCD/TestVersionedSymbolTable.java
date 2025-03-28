package org.unicode.text.UCD;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    void testIntroductionBasicExamples() {
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

    @Test
    void testIntroductionQueryLanguageExamples() {
        assertThatUnicodeSet("\\p{Uppercase_Mapping≠@Simple_Uppercase_Mapping@}")
                .contains("ß")
                .doesNotContain("ſ");
        assertThatUnicodeSet("\\p{U15.1:Simple_Case_Folding≠@U15.0:Simple_Case_Folding@}")
                .consistsOf("ﬅ", "ΐ", "ΰ");
        assertThatUnicodeSet("[\\p{cjkDefinition=/\\bcat\\b/} \\p{kEH_Desc=/\\bcat\\b/}]")
                .contains("貓")
                .contains("𓃠")
                .doesNotContain("犬")
                .doesNotContain("𓃡");
        assertThatUnicodeSet("[\\p{Case_Folding≠@code point@}-\\p{Changes_When_Casefolded}]")
                .contains("ǰ")
                .doesNotContain("š")
                .doesNotContain("ß");
    }

    @Test
    void testNegations() {
        assertThatUnicodeSet("\\P{Cn}").contains("a").doesNotContain("\uFFFF");
        assertThatUnicodeSet("[:^Cn:]").contains("a").doesNotContain("\uFFFF");
        assertThatUnicodeSet("\\P{General_Category=Cn}").contains("a").doesNotContain("\uFFFF");
        assertThatUnicodeSet("[:^General_Category=Cn:]").contains("a").doesNotContain("\uFFFF");
        assertThatUnicodeSet("\\p{General_Category≠Cn}").contains("a").doesNotContain("\uFFFF");
        assertThatUnicodeSet("[:General_Category≠Cn:]").contains("a").doesNotContain("\uFFFF");
        assertThatUnicodeSet("[:^General_Category≠Cn:]").doesNotContain("a").contains("\uFFFF");
        assertThatUnicodeSet("[:^General_Category≠Cn:]").doesNotContain("a").contains("\uFFFF");
        assertThatUnicodeSet("[:^General_Category≠Cn:]").doesNotContain("a").contains("\uFFFF");

        assertThatUnicodeSet("\\P{Decomposition_Type≠compat}")
                .contains("∯")
                .doesNotContain("∮")
                .isEqualToUnicodeSet("\\p{Decomposition_Type=compat}");
    }

    @Test
    void testNamedSingleton() {
        assertThatUnicodeSet("\\N{SPACE}").consistsOf(" ");
        assertThatUnicodeSet("\\N{THIS IS NOT A CHARACTER}")
                .isIllFormed("No character name nor name alias matches");
        assertThatUnicodeSet("\\N{PRESENTATION FORM FOR VERTICAL RIGHT WHITE LENTICULAR BRAKCET}")
                .isEqualToUnicodeSet(
                        "\\N{PRESENTATION FORM FOR VERTICAL RIGHT WHITE LENTICULAR BRACKET}")
                .consistsOf("︘");
        assertThatUnicodeSet("\\N{Latin small ligature o-e}").consistsOf("œ");
        assertThatUnicodeSet("\\N{Hangul jungseong O-E}").consistsOf("ᆀ");
        assertThatUnicodeSet("\\N{Hangul jungseong OE}").consistsOf("ᅬ");
    }

    @Test
    void testAge() {
        assertThatUnicodeSet("\\p{Age=6.0}")
                .contains("U")
                .contains("𒌋")
                .doesNotContain("𒎙")
                .isEqualToUnicodeSet("[ \\P{U6:Cn} \\p{U6:Noncharacter_Code_Point} ]");
        assertThatUnicodeSet("\\p{Age=@U6:Age@}").isIllFormed("property-comparison for Age");
        assertThatUnicodeSet("\\p{Age=/1/}").isIllFormed("regular-expression-match for Age");
    }

    @Test
    void testPropertyComparisons() {
        assertThatUnicodeSet("\\p{scf=@lc@}").contains("Σ").contains("σ").doesNotContain("ς");
        assertThatUnicodeSet("\\p{U15.1:scf=@U15.1:lc@}")
                .contains("Σ")
                .contains("σ")
                .doesNotContain("ς");
        assertThatUnicodeSet("\\p{U15.0:Line_Break≠@U15.1:Line_Break@}")
                .contains("ᯤ")
                .doesNotContain("i");
        assertThatUnicodeSet("\\p{kIRG_GSource=@none@}").contains("𒇽").doesNotContain("人");
        assertThatUnicodeSet("\\p{case folding=@code point@}")
                .contains("s")
                .doesNotContain("S")
                .doesNotContain("ſ")
                .doesNotContain("ß");
        assertThatUnicodeSet("\\p{kIRG_GSource=@U16:none@}")
                .isIllFormed("comparison version on null query");
        assertThatUnicodeSet("\\p{case folding=@U16:code point@}")
                .isIllFormed("comparison version on identity query");

        assertThatUnicodeSet("\\p{Decomposition_Mapping=@Ideographic@}")
                .isIllFormed(
                        "comparison between String property Decomposition_Mapping and"
                                + " Binary property Ideographic");
        assertThatUnicodeSet("\\p{Uppercase≠@Changes_When_Lowercased@}")
                .isEqualToUnicodeSet(
                        "[[\\p{Uppercase}\\p{Changes_When_Lowercased}]"
                                + "-[\\p{Uppercase}&\\p{Changes_When_Lowercased}]]")
                .contains("𝔄")
                .doesNotContain("A");
    }

    /** Helper class for testing multiple properties of the same UnicodeSet. */
    private static class UnicodeSetTestFluent {
        UnicodeSetTestFluent(String expression) {
            this.expression = expression;
            ParsePosition parsePosition = new ParsePosition(0);
            try {
                set = new UnicodeSet(expression);
                set.complement().complement();
            } catch (Exception e) {
                exception = e;
            }
        }

        public void isIllFormed(String messageSubstring) {
            assertNotNull(exception, expression + " is ill-formed");
            assertTrue(
                    exception.getMessage().contains(messageSubstring),
                    "Error message '"
                            + exception.getMessage()
                            + "' for "
                            + expression
                            + " contains '"
                            + messageSubstring
                            + "'");
        }

        public <T extends CharSequence> UnicodeSetTestFluent isEqualToUnicodeSet(
                String expectedExpression) {
            final var expected = new UnicodeSet(expectedExpression);
            assertTrue(
                    set.containsAll(expected),
                    expected + " ⊆ " + expression + " = " + set.toPattern(true));
            assertTrue(
                    expected.containsAll(set),
                    expected + " ⊇ " + expression + " = " + set.toPattern(true));
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

        public UnicodeSetTestFluent consistsOf(CharSequence... elements) {
            for (CharSequence element : elements) {
                contains(element);
            }
            final var expectedElements = new UnicodeSet().addAll(elements);
            assertTrue(
                    expectedElements.containsAll(set),
                    expectedElements + " ⊇ " + expression + " = " + set.toPattern(true));
            return this;
        }

        private UnicodeSet set;
        private String expression;
        private Exception exception;
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
