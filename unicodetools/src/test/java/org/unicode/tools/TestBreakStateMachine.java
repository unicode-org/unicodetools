package org.unicode.tools;

import com.ibm.icu.impl.UnicodeMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.VersionInfo;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.unicode.text.UCD.VersionedSymbolTable;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;
import org.unicode.unittest.TestFmwkMinusMinus;

public class TestBreakStateMachine extends TestFmwkMinusMinus {

    @Test
    public void testLine() throws IOException {
        final VersionInfo version = UCharacter.getUnicodeVersion();
        final var symbolTable = VersionedSymbolTable.frozenAt(version);
        UnicodeMap<String> classes = new UnicodeMap<>();
        class State {
            boolean unconditionallyAccepting = false;
            String acceptingForLookahead = null;
            String lookahead = null;
            Map<String, State> transitions = new HashMap<>();
        }
        Map<String, State> states = new HashMap<>();
        try (var file =
                new BufferedReader(
                        new FileReader(
                                "C:\\Users\\robin\\Projects\\Unicode\\unicodetools\\LineBreakClasses.txt"))) {
            for (; ; ) {
                String line = file.readLine();
                if (line == null) {
                    break;
                }
                line = line.split(" *#", 2)[0];
                if (line.isEmpty()) {
                    continue;
                }
                final String[] parts = line.split(" *; *", -1);
                classes.putAll(
                        new UnicodeSet(parts[1], new ParsePosition(0), symbolTable), parts[0]);
            }
        }
        try (var file =
                new BufferedReader(
                        new FileReader(
                                "C:\\Users\\robin\\Projects\\Unicode\\unicodetools\\LineBreakStates.txt"))) {
            for (; ; ) {
                String line = file.readLine();
                if (line == null) {
                    break;
                }
                line = line.split("#", 2)[0].stripTrailing();
                if (line.isEmpty()) {
                    continue;
                }
                final String[] parts = line.split(" *; *", -1);
                final var state = new State();
                final String accepting = parts[1];
                if (accepting.equals("Yes")) {
                    state.unconditionallyAccepting = true;
                } else if (!accepting.equals("No")) {
                    state.acceptingForLookahead = accepting;
                }
                final String lookahead = parts[2];
                if (!lookahead.isEmpty()) {
                    state.lookahead = lookahead;
                }
                states.put(parts[0], state);
            }
        }
        try (var file =
                new BufferedReader(
                        new FileReader(
                                "C:\\Users\\robin\\Projects\\Unicode\\unicodetools\\LineBreakTransitions.txt"))) {
            for (; ; ) {
                String line = file.readLine();
                if (line == null) {
                    break;
                }
                line = line.split("#", 2)[0].stripTrailing();
                if (line.isEmpty()) {
                    continue;
                }
                final String[] parts = line.split(" *; *", -1);
                states.get(parts[0]).transitions.put(parts[1], states.get(parts[2]));
            }
        }
        try (var file =
                new BufferedReader(
                        new FileReader(
                                Settings.UnicodeTools.getDataPath(
                                                "ucd", version.getVersionString(3, 3))
                                        .resolve("auxiliary/LineBreakTest.txt")
                                        .toFile()))) {
            int errors = 0;
            int testCases = 0;
            for (; ; ) {
                String line = file.readLine();
                if (line == null) {
                    break;
                }
                line = line.split("#", 2)[0].stripTrailing();
                if (line.isEmpty()) {
                    continue;
                }
                ++testCases;
                final String[] parts = line.split(" +");
                final StringBuilder testString = new StringBuilder();
                final Set<Integer> expectedBreaks = new TreeSet<>();
                for (int i = 0; i < (parts.length + 1) / 2; ++i) {
                    if (parts[2 * i].equals("÷")) {
                        expectedBreaks.add(testString.length());
                    }
                    if (2 * i + 1 != parts.length) {
                        testString.append(Utility.fromHex(parts[2 * i + 1]));
                    }
                }
                final Set<Integer> computedBreaks = new TreeSet<>();
                int lastBreak = 0;
                while (lastBreak != testString.length()) {
                    State state = states.get("START");
                    Integer lastAccepting = null;
                    Map<String, Integer> lookaheadPositions = new HashMap<>();
                    for (int i = lastBreak;
                            ;
                            i =
                                    i == testString.length()
                                            ? i
                                            : testString.offsetByCodePoints(i, 1)) {
                        if (state.unconditionallyAccepting) {
                            lastAccepting = i;
                        }
                        if (state.acceptingForLookahead != null
                                && lookaheadPositions.containsKey(state.acceptingForLookahead)) {
                            // A lookahead matches.  Break where the lookahead started.
                            lastBreak = lookaheadPositions.get(state.acceptingForLookahead);
                            break;
                        }
                        if (state.lookahead != null) {
                            lookaheadPositions.put(state.lookahead, i);
                        }

                        final String classAhead =
                                i == testString.length()
                                        ? "eot"
                                        : classes.get(testString.codePointAt(i));
                        state = state.transitions.get(classAhead);
                        if (state == null) {
                            // Normal DFA termination: no transition out of this state, the break is
                            // the
                            // last accepting state encoutered.
                            lastBreak = lastAccepting;
                            break;
                        }
                    }
                    computedBreaks.add(lastBreak);
                }
                if (!computedBreaks.equals(expectedBreaks)) {
                    ++errors;
                    System.err.println("Error on test case " + line);
                    for (var i : computedBreaks) {
                        if (!expectedBreaks.contains(i)) {
                            System.err.println("Unexpected break at position " + i);
                        }
                    }
                    for (var i : expectedBreaks) {
                        if (!computedBreaks.contains(i)) {
                            System.err.println("Missing expected break at position " + i);
                        }
                    }
                }
            }
            System.out.println("Ran " + testCases + " line breaking test cases");
            assertEquals("LineBreakTest.txt errors", errors, 0);
        }
    }
}
