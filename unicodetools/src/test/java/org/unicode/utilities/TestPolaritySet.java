package org.unicode.utilities;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.unicode.unittest.TestFmwkMinusMinus;
import org.unicode.utilities.PolaritySet.Operation;

public class TestPolaritySet extends TestFmwkMinusMinus {
    private static final Splitter SPACE_SPLITTER = Splitter.on(' ');

    @Test
    public void testBasic() {
        Matcher mainOp = Pattern.compile("[∪∩∖⊕]").matcher("");
        System.out.println(ImmutableSet.of("a", "b"));

        String[][] tests = {
            {"¬{a b}", "¬{a b}"},
            {"¬Ω", "∅"},
            {"¬∅", "Ω"},
            {"{a b} ∪ Ω", "Ω"},
            {"{a b} ∪ ∅", "{a b}"},
            {"{a b} ∩ Ω", "{a b}"},
            {"{a b} ∩ ∅", "∅"},
            {"{a b} ∪ {b c}", "{a b c}"},
            {"{a b} ∪ ¬{b c}", "¬{c}"},
            {"¬{a b} ∪ {b c}", "¬{a}"},
            {"¬{a b} ∪ ¬{b c}", "¬{b}"},
            {"{a b} ∩ {b c}", "{b}"},
            {"{a b} ∩ ¬{b c}", "{a}"},
            {"¬{a b} ∩ {b c}", "{c}"},
            {"¬{a b} ∩ ¬{b c}", "¬{a b c}"},
            {"{a b} ∖ {b c}", "{a}"},
            {"{a b} ∖ ¬{b c}", "{b}"},
            {"¬{a b} ∖ {b c}", "¬{a b c}"},
            {"¬{a b} ∖ ¬{b c}", "{c}"},
            {"{a b} ⊕ {b c}", "{a c}"},
            {"{a b} ⊕ ¬{b c}", "¬{a c}"},
            {"¬{a b} ⊕ {b c}", "¬{a c}"},
            {"¬{a b} ⊕ ¬{b c}", "{a c}"},
        };
        for (String[] row : tests) {
            String source = row[0];
            String expected = row[1];
            PolaritySet<String> result;
            String modSource;
            if (!mainOp.reset(source).find()) {
                // no main op, check !
                result = PolaritySet.fromTestString(source);
                modSource = result.toString();
            } else {
                PolaritySet<String> left =
                        PolaritySet.fromTestString(source.substring(0, mainOp.start()));
                PolaritySet<String> right =
                        PolaritySet.fromTestString(source.substring(mainOp.end()));
                modSource = left + " " + mainOp.group() + " " + right;
                result = PolaritySet.of(left);
                Operation operation = PolaritySet.Operation.fromDisplay(mainOp.group());
                switch (operation) {
                    case UNION:
                        result.addAll(right);
                        break;
                    case INTERSECT:
                        result.retainAll(right);
                        break;
                    case SUBTRACT:
                        result.removeAll(right);
                        break;
                    case XOR:
                        result.retainDifferences(right);
                        break;
                }
            }
            String actual = result == null ? null : result.toString();
            assertEquals(
                    source + (modSource.equals(source) ? "" : "\t" + "(" + modSource + ")"),
                    expected,
                    actual);
        }
    }
}
