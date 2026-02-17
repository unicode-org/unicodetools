package org.unicode.text.tools;

import com.ibm.icu.impl.RBBIDataWrapper;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.RuleBasedBreakIterator;
import com.ibm.icu.text.UnicodeSet;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;
import org.unicode.text.UCD.VersionedSymbolTable;
import org.unicode.tools.Segmenter;
import org.unicode.tools.Segmenter.Builder.NamedRefinedSet;

public class GenerateBreakStateTables {
    public static void main(String[] args) throws IOException {
        var rbbi = (RuleBasedBreakIterator) BreakIterator.getLineInstance();
        var segmenter =
                Segmenter.make(
                                VersionedSymbolTable.frozenAt(UCharacter.getUnicodeVersion()),
                                "LineBreak")
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
                        rbbiNames.put(entry.getKey(), refinement);
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
        }
        var table = rbbi.fRData.fFTable;
        var file = new PrintStream(new File("LineBreakTable.txt"));
        Map<Integer, String> stateNames = new HashMap<>();
        stateNames.put(0, "STOP");
        stateNames.put(1, "START");
        Queue<Integer> neighbourhoodsToName = new LinkedList<>();
        neighbourhoodsToName.add(1);
        for (int state = 1; !neighbourhoodsToName.isEmpty(); state = neighbourhoodsToName.poll()) {
            int row = rbbi.fRData.getRowIndex(state);
            for (int col = 0; col < rbbi.fRData.fHeader.fCatCount; ++col) {
                int next = table.fTable[row + RBBIDataWrapper.NEXTSTATES + col];
                if (stateNames.containsKey(next)) {
                    continue;
                }
                stateNames.put(
                        next,
                        (state == 1 ? "" : stateNames.get(state) + ">")
                                + rbbiNames.getOrDefault(col, List.of()).stream()
                                        .map(NamedRefinedSet::getName)
                                        .collect(Collectors.joining("|")));
                neighbourhoodsToName.add(next);
            }
        }
        for (int state = 1; state < table.fNumStates; ++state) {
            int row = rbbi.fRData.getRowIndex(state);
            if (table.fTable[row + RBBIDataWrapper.ACCEPTING] != 0) {
                file.println("State " + stateNames.get(state) + ":");
            } else {
                file.println("State ||" + stateNames.get(state) + "||:");
            }

            for (int col = 0; col < rbbi.fRData.fHeader.fCatCount; ++col) {
                int next = table.fTable[row + RBBIDataWrapper.NEXTSTATES + col];
                if (rbbiNames.get(col) == null && next == 0) {
                    continue;
                }
                // TODO(egg): Weird default, check the output for (). Seems to have to do with
                // Qu_Pf. Lookahead?
                String ahead =
                        rbbiNames.getOrDefault(col, List.of()).stream()
                                .map(NamedRefinedSet::getName)
                                .collect(Collectors.joining("+"));
                if (stateNames.get(next).equals(ahead)) {
                    file.println("-(" + ahead + ")-> SELF");
                } else {
                    file.println("-(" + ahead + ")-> " + stateNames.get(next));
                }
            }
        }
    }
}
