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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.text.UCD.VersionedSymbolTable;
import org.unicode.tools.Segmenter;
import org.unicode.tools.Segmenter.Builder.NamedRefinedSet;
import org.unicode.tools.Segmenter.Builder.NamedSet;

public class GenerateBreakStateTables {
    public static void main(String[] args) throws IOException {
        Generate("Line", "uline", Map.of(100, "Mandatory"));
        Generate("GraphemeCluster", "char", Map.of());
        // Generate("Word", "word", Map.of(100, "Number", 200, "Letter", 400, "Letter"));
        // Generate("Sentence", "sent", Map.of(100, "EOL"));
    }

    private static final Map<Integer, String> LINE_TAILORING_HOOKS =
            Map.ofEntries(
                    Map.entry(0x100003, "NON_EAST_ASIAN_UNCLASSIFIED"),
                    Map.entry(0x100000, "EAST_ASIAN_UNCLASSIFIED"),
                    Map.entry(0x100001, "NON_EAST_ASIAN_LOOSE_IN"),
                    Map.entry(0x100002, "EAST_ASIAN_LOOSE_IN"),
                    Map.entry(0x100004, "NON_EAST_ASIAN_POX"),
                    Map.entry(0x100005, "EAST_ASIAN_POX"),
                    Map.entry(0x100006, "NON_EAST_ASIAN_PRX"),
                    Map.entry(0x100007, "EAST_ASIAN_PRX"),
                    Map.entry(0x100008, "LOOSE_DASH"),
                    Map.entry(0x100009, "EAST_ASIAN_PHRASE_ID"),
                    Map.entry(0x10000A, "EAST_ASIAN_PHRASE_AL"),
                    Map.entry(0x10000B, "EAST_ASIAN_PHRASE_CM"),
                    Map.entry(0x10000C, "EAST_ASIAN_PHRASE_NS"),
                    Map.entry(0x10000D, "EAST_ASIAN_PHRASE_H2"),
                    Map.entry(0x10000E, "EAST_ASIAN_PHRASE_H3"),
                    Map.entry(0x10000F, "EAST_ASIAN_ID_AL"));

    private static void Generate(
            final String name, final String icuName, final Map<Integer, String> tagNames)
            throws IOException {
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
        final int privateUseClass = rbbi.fRData.fTrie.get(0xF0000);
        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            int partIndex = rbbi.fRData.fTrie.get(cp);
            if (LINE_TAILORING_HOOKS.containsKey(cp) && partIndex != privateUseClass) {
                if (!rbbiPartition.containsKey(partIndex)) {
                    final String hookName = LINE_TAILORING_HOOKS.get(cp);
                    rbbiPartition.put(partIndex, new UnicodeSet().add(hookName));
                    namedPartition.add(
                            new NamedRefinedSet()
                                    .intersect(
                                            new NamedSet(
                                                    hookName,
                                                    "[{" + hookName + "}]",
                                                    new UnicodeSet("[{" + hookName + "}]"))));
                }
                partIndex = privateUseClass;
            }
            if (!rbbiPartition.containsKey(partIndex)) {
                rbbiPartition.put(partIndex, new UnicodeSet());
            }
            rbbiPartition.get(partIndex).add(cp);
        }
        final var table = rbbi.fRData.fFTable;
        boolean usesEOT = false;
        for (int state = 1; state < table.fNumStates; ++state) {
            final int row = rbbi.fRData.getRowIndex(state);
            for (int col = 0; col < rbbi.fRData.fHeader.fCatCount; ++col) {
                int next = table.fTable[row + RBBIDataWrapper.NEXTSTATES + col];
                if (next != 0) {
                    if (col == 1) {
                        usesEOT = true;
                    }
                }
            }
        }
        if (usesEOT) {
            if (!rbbiPartition.containsKey(1)) {
                rbbiPartition.put(1, new UnicodeSet());
            }
            rbbiPartition.get(1).add("eot");
            namedPartition.add(
                    new NamedRefinedSet()
                            .intersect(new NamedSet("eot", "[{eot}]", new UnicodeSet("[{eot}]"))));
        }
        loopOverRbbiPartition:
        for (var entry : rbbiPartition.entrySet()) {
            // UnicodeSet strings = new UnicodeSet().addAll(entry.getValue().strings());
            UnicodeSet rbbiPart = entry.getValue(); // .cloneAsThawed().removeAll(strings);
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
                                                                .removeAll(unassigned)
                                                                .removeAll(pua)
                                                                .size()))
                                .map(NamedRefinedSet::getName)
                                .map(s -> s.replace("orig", ""))
                                .findFirst()
                                .orElse(""));
        }
        System.out.println(rbbiPartition.size() + " symbols");
        System.out.println(table.fNumStates + " states");
        Map<Integer, String> stateNames = new HashMap<>();
        Map<Integer, List<Integer>> lookaheadPrefixes = new HashMap<>();
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
                if (lookahead != 0) {
                    lookaheadPrefixes.computeIfAbsent(lookahead, l -> new ArrayList<>()).add(state);
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
                                + (rbbiNames.get(col).stream()
                                        .sorted(
                                                Comparator.<NamedRefinedSet>comparingInt(
                                                        s ->
                                                                -s.getSet()
                                                                        .removeAll(unassigned)
                                                                        .removeAll(pua)
                                                                        .size()))
                                        .map(NamedRefinedSet::getName)
                                        .map(s -> s.replace("orig", ""))
                                        .findFirst()
                                        .get()));
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
        final Map<Integer, List<String>> lookaheadNameParts = new HashMap<>();
        for (final var prefixEntry : lookaheadPrefixes.entrySet()) {
            final int lookahead = prefixEntry.getKey();
            System.out.println(
                    "Lookahead "
                            + lookahead
                            + " is set by "
                            + prefixEntry.getValue().size()
                            + " states");
            for (int startingState : prefixEntry.getValue()) {
                Set<Integer> visited = new HashSet<>();
                // We do not start with a 1-element boundary of set to startingState, because if
                // startingState accepts the lookahead it sets, it should do so when we come back to
                // it, not as it sets it.
                class StateAndPath {
                    public StateAndPath(int state, List<Integer> path) {
                        this.state = state;
                        this.path = path;
                    }

                    final int state;
                    final List<Integer> path;
                }
                Queue<StateAndPath> boundary = new LinkedList<>();
                final int startingRow = rbbi.fRData.getRowIndex(startingState);
                for (int col = 0; col < rbbi.fRData.fHeader.fCatCount; ++col) {
                    final int next = table.fTable[startingRow + RBBIDataWrapper.NEXTSTATES + col];
                    boundary.add(new StateAndPath(next, List.of(col)));
                }
                for (; ; ) {
                    final var stateAndPath = boundary.poll();
                    final var state = stateAndPath.state;
                    final int row = rbbi.fRData.getRowIndex(state);
                    final int accepting = table.fTable[row + RBBIDataWrapper.ACCEPTING];
                    if (accepting == lookahead) {
                        lookaheadNameParts
                                .computeIfAbsent(lookahead, l -> new ArrayList<>())
                                .add(
                                        stateNames.get(startingState)
                                                + " / "
                                                + stateAndPath.path.stream()
                                                        .map(
                                                                symbol ->
                                                                        rbbiNames
                                                                                .get(symbol)
                                                                                .stream()
                                                                                .sorted(
                                                                                        Comparator
                                                                                                .<NamedRefinedSet>
                                                                                                        comparingInt(
                                                                                                                s ->
                                                                                                                        -s.getSet()
                                                                                                                                .removeAll(
                                                                                                                                        unassigned)
                                                                                                                                .removeAll(
                                                                                                                                        pua)
                                                                                                                                .size()))
                                                                                .map(
                                                                                        NamedRefinedSet
                                                                                                ::getName)
                                                                                .map(
                                                                                        s ->
                                                                                                s
                                                                                                        .replace(
                                                                                                                "orig",
                                                                                                                ""))
                                                                                .findFirst()
                                                                                .get())
                                                        .collect(Collectors.joining(" ")));
                        break;
                    }
                    visited.add(state);
                    for (int col = 0; col < rbbi.fRData.fHeader.fCatCount; ++col) {
                        final int next = table.fTable[row + RBBIDataWrapper.NEXTSTATES + col];
                        if (visited.contains(next)) {
                            continue;
                        }
                        final var path = new ArrayList<>(stateAndPath.path);
                        path.add(col);
                        boundary.add(new StateAndPath(next, path));
                    }
                }
            }
        }
        final var lookaheadNames =
                lookaheadNameParts.entrySet().stream()
                        .collect(
                                Collectors.toMap(
                                        kv -> kv.getKey(),
                                        kv ->
                                                kv.getValue().stream()
                                                        .sorted(
                                                                Comparator.<String>comparingInt(
                                                                                s -> s.length())
                                                                        .thenComparing(s -> s))
                                                        .findFirst()
                                                        .get()));
        Map<String, Integer> nameToLookahead = new HashMap<>();
        for (var entry : lookaheadNames.entrySet()) {
            if (nameToLookahead.containsKey(entry.getValue())) {
                throw new IllegalArgumentException(
                        "Duplicate lookahead name "
                                + entry.getValue()
                                + " for lookaheads "
                                + nameToLookahead.get(entry.getValue())
                                + " and "
                                + entry.getKey());
            }
            nameToLookahead.put(entry.getValue(), entry.getKey());
        }
        try (var file = new PrintStream(new File(name + "BreakSymbols.txt"))) {
            file.println("# Symbol name ; Symbol definition in UnicodeSet notation");
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
                    "# State name ; Accepting (Yes, No, or lookahead name); lookahead name or empty; Break type.");
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
                file.print(" ; ");
                final int tagIndex = table.fTable[row + RBBIDataWrapper.TAGSIDX];
                final int tag =
                        rbbi.fRData.fStatusTable[tagIndex + rbbi.fRData.fStatusTable[tagIndex]];
                if (tag != 0) {
                    file.print(tagNames.get(tag));
                }
                file.println();
            }
        }
        try (var file = new PrintStream(new File(name + "BreakTransitions.txt"))) {
            file.println("# From state ; symbol ; to state");
            for (int state = 1; state < table.fNumStates; ++state) {
                final int row = rbbi.fRData.getRowIndex(state);

                for (int col = 0; col < rbbi.fRData.fHeader.fCatCount; ++col) {
                    int next = table.fTable[row + RBBIDataWrapper.NEXTSTATES + col];
                    if (rbbiNames.get(col) == null && next == 0) {
                        continue;
                    }
                    String ahead =
                            rbbiNames.get(col).stream()
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
