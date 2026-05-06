package org.unicode.text.tools;

import com.ibm.icu.impl.RBBIDataWrapper;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.RuleBasedBreakIterator;
import com.ibm.icu.text.UnicodeSet;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.text.UCD.VersionedSymbolTable;
import org.unicode.tools.Segmenter;
import org.unicode.tools.Segmenter.Builder.NamedRefinedSet;
import org.unicode.tools.Segmenter.Builder.NamedSet;

public class GenerateBreakStateTables {
    public static void main(String[] args) throws IOException {
        Generate("Line", "line");
        Generate("GraphemeCluster", "char");
        Generate("Word", "word");
        Generate("Sentence", "sent");
    }

    private static void Generate(String name, String icuName) throws IOException {
        RuleBasedBreakIterator rbbi;
        try (var f =
                new FileInputStream(
                        new File(
                                "..\\icu\\icu4c\\source\\data\\out\\build\\icudt79l\\brkitr\\"
                                        + icuName
                                        + ".brk"))) {
            rbbi = RuleBasedBreakIterator.getInstanceFromCompiledRules(f);
        }
        final var iup = IndexUnicodeProperties.make(UCharacter.getUnicodeVersion());
        final var unassigned = iup.getProperty("gc").getSet("Unassigned");
        final var pua = iup.getProperty("gc").getSet("Private Use");
        var segmenter =
                Segmenter.make(
                                VersionedSymbolTable.frozenAt(UCharacter.getUnicodeVersion()),
                                name + "Break")
                        .make();
        List<NamedRefinedSet> namedPartition = segmenter.getPartitionDefinition();
        Map<Integer, UnicodeSet> rbbiPartition = new HashMap<>();
        Map<Integer, List<NamedRefinedSet>> rbbiNames = new HashMap<>();
        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            int partIndex = rbbi.fRData.fTrie.get(cp);
            if (!rbbiPartition.containsKey(partIndex)) {
                rbbiPartition.put(partIndex, new UnicodeSet());
            }
            rbbiPartition.get(partIndex).add(cp);
        }
        loopOverRbbiPartition:
        for (var entry : rbbiPartition.entrySet()) {
            UnicodeSet rbbiPart = entry.getValue();
            UnicodeSet rbbiPartRemaining = rbbiPart.cloneAsThawed();
            List<NamedRefinedSet> refinement = new ArrayList<>();
            for (var part : namedPartition) {
                if (part.getSet().equals(rbbiPart)) {
                    rbbiNames.put(entry.getKey(), List.of(part));
                    System.out.println("Part " + entry.getKey() + " is " + part.getName());
                    continue loopOverRbbiPartition;
                } else if (rbbiPart.containsAll(part.getSet())) {
                    refinement.add(part);
                    rbbiPartRemaining.removeAll(part.getSet());
                    if (rbbiPartRemaining.isEmpty()) {
                        System.out.println(
                                "Part "
                                        + entry.getKey()
                                        + " is a union of: "
                                        + refinement.stream()
                                                .map(NamedRefinedSet::getName)
                                                .collect(Collectors.joining(", ")));
                        if (refinement.stream()
                                .map(NamedRefinedSet::getName)
                                .collect(Collectors.joining(", "))
                                .equals("CMorig_EastAsian, CMorigmEastAsian")) {
                            rbbiNames.put(
                                    entry.getKey(),
                                    List.of(
                                            new NamedRefinedSet()
                                                    .intersect(
                                                            refinement
                                                                    .get(0)
                                                                    .intersectionTerms
                                                                    .get(0))));
                        } else {
                            rbbiNames.put(entry.getKey(), refinement);
                        }
                        continue loopOverRbbiPartition;
                    }
                }
            }
            System.out.println(
                    "Part " + entry.getKey() + " is nameless: " + entry.getValue().toString());
            if (!refinement.isEmpty()) {
                System.out.println(
                        "Partially covered by "
                                + refinement.stream()
                                        .map(NamedRefinedSet::getName)
                                        .collect(Collectors.joining(", ")));
                System.out.println("Remaining: " + rbbiPartRemaining.toString());
            }
            rbbiNames.put(
                    entry.getKey(),
                    List.of(
                            new NamedRefinedSet()
                                    .intersect(
                                            new NamedSet(
                                                    "UNIDENTIFIED-" + entry.getKey(),
                                                    entry.getValue().toString(),
                                                    entry.getValue()))));
        }
        System.out.println("Named categories:");
        for (int col = 0; col < rbbi.fRData.fHeader.fCatCount; ++col) {
            System.out.print(col + " : ");
            if (!rbbiNames.containsKey(col)) {
                System.out.println("NAMELESS");
            } else 
                System.out.println(
            rbbiNames.get(col).stream()
                                                .sorted(
                                                        Comparator.<NamedRefinedSet>comparingInt(
                                                                s ->
                                                                        -s.getSet()
                                                                                .removeAll(
                                                                                        unassigned)
                                                                                .removeAll(pua)
                                                                                .size()))
                                                .map(NamedRefinedSet::getName)
                                                .map(s -> s.replace("orig", ""))
                                                .findFirst()
                                                .orElse(""));
        }
        var table = rbbi.fRData.fFTable;
        System.out.println(rbbiPartition.size() + " classes");
        System.out.println(table.fNumStates + " states");
        Map<Integer, String> stateNames = new HashMap<>();
        Map<Integer, String> lookaheadNames = new HashMap<>();
        stateNames.put(0, "STOP");
        stateNames.put(1, "START");
        Queue<Integer> neighbourhoodsToName = new LinkedList<>();
        neighbourhoodsToName.add(1);
        do {
            final int state = neighbourhoodsToName.poll();
            final int row = rbbi.fRData.getRowIndex(state);
            final String stateName = stateNames.get(state);
            {
                final int lookahead = table.fTable[row + RBBIDataWrapper.LOOKAHEAD];
                if (lookahead != 0 && !lookaheadNames.containsKey(lookahead)) {
                    lookaheadNames.put(lookahead, stateName);
                }
            }
            {
                final int accepting = table.fTable[row + RBBIDataWrapper.ACCEPTING];
                if (accepting > RBBIDataWrapper.ACCEPTING_UNCONDITIONAL) {
                    final String prefix = lookaheadNames.get(accepting);
                    if (!prefix.contains("/")) {
                        if (!stateName.startsWith(prefix)) {
                            throw new IllegalArgumentException(
                                    stateName + " does not start with " + prefix);
                        }
                        lookaheadNames.put(
                                accepting,
                                prefix
                                        + " /"
                                        + stateName.subSequence(
                                                prefix.length(), stateName.length()));
                    }
                }
            }
            for (int col = 0; col < rbbi.fRData.fHeader.fCatCount; ++col) {
                final int next = table.fTable[row + RBBIDataWrapper.NEXTSTATES + col];
                if (stateNames.containsKey(next)) {
                    continue;
                }
                stateNames.put(
                        next,
                        (state == 1 ? "" : stateName + " ")
                                + (col == 1
                                        ? "eot" : col == 2 ? "sot"
                                        : rbbiNames.get(col).stream()
                                                .sorted(
                                                        Comparator.<NamedRefinedSet>comparingInt(
                                                                s ->
                                                                        -s.getSet()
                                                                                .removeAll(
                                                                                        unassigned)
                                                                                .removeAll(pua)
                                                                                .size()))
                                                .map(NamedRefinedSet::getName)
                                                .map(s -> s.replace("orig", ""))
                                                .findFirst().get()));
                neighbourhoodsToName.add(next);
            }
        } while (!neighbourhoodsToName.isEmpty());
        Map<String, Integer> nameToState = new HashMap<>();
        for (var entry : stateNames.entrySet()) {
            if (nameToState.containsKey(entry.getValue())) {
                throw new IllegalArgumentException(
                        "Duplicate state name "
                                + entry.getValue()
                                + " for states "
                                + nameToState.get(entry.getValue())
                                + " and "
                                + entry.getKey());
            }
            nameToState.put(entry.getValue(), entry.getKey());
        }
        for (var entry : lookaheadNames.entrySet()) {
            if (!entry.getValue().contains("/")) {
                throw new IllegalArgumentException(
                        "lookahead name "
                                + entry.getValue()
                                + " for "
                                + entry.getKey()
                                + " has no / (accepting state was not found by the BFS)");
            }
            nameToState.put(entry.getValue(), entry.getKey());
        }
        try (var file = new PrintStream(new File(name + "BreakClasses.txt"))) {
            file.println("# Class name ; Class definition in UnicodeSet notation");
            for (final var characterClass : rbbiNames.values()) {
                file.print(
                        characterClass.stream()
                                        .map(NamedRefinedSet::getName)
                                        .map(s -> s.replace("orig", ""))
                                        .collect(Collectors.joining("|"))
                                + " ; ");
                if (characterClass.size() > 1) {
                    file.print("[");
                }
                file.print(
                        characterClass.stream()
                                .map(NamedRefinedSet::getDefinition)
                                .collect(Collectors.joining(" ")));
                if (characterClass.size() > 1) {
                    file.print("]");
                }
                file.println();
            }
        }
        try (var file = new PrintStream(new File(name + "BreakStates.txt"))) {
            file.println(
                    "# State name ; Accepting (Yes, No, or lookahead name); lookahead name or empty.");
            for (int state = 1; state < table.fNumStates; ++state) {
                final int row = rbbi.fRData.getRowIndex(state);
                file.print(stateNames.get(state) + " ; ");
                final int accepting = table.fTable[row + RBBIDataWrapper.ACCEPTING];
                if (accepting == RBBIDataWrapper.ACCEPTING_UNCONDITIONAL) {
                    file.print("Yes");
                } else if (accepting > RBBIDataWrapper.ACCEPTING_UNCONDITIONAL) {
                    file.print(lookaheadNames.get(accepting));
                } else {
                    file.print("No");
                }
                file.print(" ; ");
                final int lookahead = table.fTable[row + RBBIDataWrapper.LOOKAHEAD];
                if (lookahead != 0) {
                    file.print(lookaheadNames.get(lookahead));
                }
                file.println();
            }
        }
        try (var file = new PrintStream(new File(name + "BreakTransitions.txt"))) {
            file.println("# From state ; class name or eot ; to state");
            for (int state = 1; state < table.fNumStates; ++state) {
                final int row = rbbi.fRData.getRowIndex(state);

                for (int col = 0; col < rbbi.fRData.fHeader.fCatCount; ++col) {
                    int next = table.fTable[row + RBBIDataWrapper.NEXTSTATES + col];
                    if (rbbiNames.get(col) == null && next == 0) {
                        continue;
                    }
                    String ahead =
                            col == 1
                                    ? "eot" : col == 2 ? "sot"
                                    : rbbiNames.get(col).stream()
                                            .map(NamedRefinedSet::getName)
                                            .map(s -> s.replace("orig", ""))
                                            .collect(Collectors.joining("|"));
                    if (next != 0 && !ahead.isEmpty()) {
                        file.print(stateNames.get(state) + " ; ");
                        file.println(ahead + " ; " + stateNames.get(next));
                    }
                }
            }
        }
    }
}
