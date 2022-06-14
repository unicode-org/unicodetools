package org.unicode.text.tools;

import com.ibm.icu.text.CollationElementIterator;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class GenerateCeMapping {

    static Map<Integer, Set<String>> ceToCharsContaining = new TreeMap<Integer, Set<String>>();
    static Map<String, CeList> charsToCeList = new TreeMap<String, CeList>();
    static Map<Integer, Set<String>> ceToCharsStartingWith = new TreeMap<Integer, Set<String>>();

    public static void main(String[] args) throws Exception {

        System.out.println("Gathering data");

        final RuleBasedCollator rbc = (RuleBasedCollator) Collator.getInstance();
        final UnicodeSet contractions = new UnicodeSet();
        final UnicodeSet expansions = new UnicodeSet();
        rbc.getContractionsAndExpansions(contractions, expansions, true);
        final UnicodeSet charsToTest =
                new UnicodeSet("[[:assigned:]-[:ideographic:]-[:hangul:]-[:c:]]")
                        .addAll(contractions)
                        .addAll(expansions)
                        .addAll(rbc.getTailoredSet());

        final CollationElementIterator cei = rbc.getCollationElementIterator("");

        System.out.println("Items to check: " + charsToTest.size());
        // not the most efficient way, but for testing...

        for (final UnicodeSetIterator it = new UnicodeSetIterator(charsToTest); it.next(); ) {
            final String item = it.getString();
            cei.setText(item);
            final CeList ceList = new CeList(cei, item);
            charsToCeList.put(item, ceList);

            // add to mapping from ce to chars containing, starting
            for (int i = 0; i < ceList.size(); ++i) {
                final int ce = ceList.get(i);
                if (i == 0) {
                    addCeToString(ce, ceToCharsStartingWith, item);
                }
                addCeToString(ce, ceToCharsContaining, item);
            }
        }

        System.out.println("Removing Singletons");

        for (final Iterator<Integer> it = ceToCharsContaining.keySet().iterator(); it.hasNext(); ) {
            final Integer item = it.next();
            final Set<String> set = ceToCharsContaining.get(item);
            int maxSize = 0;
            int minSize = Integer.MAX_VALUE;
            for (final String string : set) {
                final CeList ceList = charsToCeList.get(string);
                final int size = ceList.size();
                if (maxSize < size) {
                    maxSize = size;
                }
                if (minSize > size) {
                    minSize = size;
                }
            }
            if (minSize == maxSize) {
                it.remove();
            }
        }

        // print stats
        if (true) {
            for (final int item : ceToCharsContaining.keySet()) {
                final Set<String> set = ceToCharsContaining.get(item);
                System.out.println(Integer.toString(item, 16));
                for (final String string : set) {
                    System.out.println("\t" + string + "\t" + charsToCeList.get(string));
                }
            }
        }

        System.out.println("Get Shortest Distance to End");
        final String[] examples = {"fuss", "fiss", "affliss"};
        final StringBuilder debug = new StringBuilder();
        for (final String example : examples) {
            final CeList exampleCeList = new CeList(cei, example);
            System.out.println("Example: " + example + "\t" + exampleCeList);
            for (int i = 0; i < example.length(); ++i) {
                System.out.println("\t" + minLengthInChars(exampleCeList, i, debug) + "\t" + debug);
            }
        }
        // very quick and dirty
        // for each character that could match in the string, figure out the shortest distance to
        // the end of the string.

    }

    /**
     * return the minimum distance in chars to the end of the string. For now, dumb algorithm
     *
     * @param ceList
     * @param offset
     */
    public static int minLengthInChars(CeList ceList, int offset, StringBuilder debug) {
        // find out shortest string for the longest sequence of ces.
        // needs to be refined to use dynamic programming, but will be roughly right
        debug.setLength(0);
        int totalStringLength = 0;
        while (offset < ceList.size()) {
            final int ce = ceList.get(offset);
            int bestLength = Integer.MIN_VALUE;
            String bestString = null;
            int bestCeLength = 0;
            for (final String s : ceToCharsStartingWith.get(ce)) {
                final CeList ceList2 = charsToCeList.get(s);
                if (ceList.matchesAt(offset, ceList2)) {
                    final int length = ceList2.size() - s.length();
                    if (bestLength < length) {
                        bestLength = length;
                        bestCeLength = ceList2.size();
                        bestString = s;
                    }
                }
            }
            totalStringLength += bestString.length();
            debug.append(bestString).append("/");
            offset += bestCeLength;
        }
        return totalStringLength;
    }

    private static void addCeToString(
            final int ce, Map<Integer, Set<String>> ceToCharsStartingWith, final String item) {
        Set<String> possibleStrings = ceToCharsStartingWith.get(ce);
        if (possibleStrings == null) {
            ceToCharsStartingWith.put(ce, possibleStrings = new TreeSet<String>());
        }
        possibleStrings.add(item);
    }

    static class CeList {
        List<Integer> ces = new ArrayList<Integer>(0);

        public CeList(CollationElementIterator cei, String item) {
            cei.setText(item);
            while (true) {
                int ce = cei.next();
                if (ce == CollationElementIterator.NULLORDER) {
                    break;
                }
                ce = CollationElementIterator.primaryOrder(ce);
                if (ce == 0) {
                    continue;
                }
                ces.add(ce);
            }
        }

        /**
         * This contains the other starting at offset
         *
         * @param offset
         * @param ceList2
         * @return
         */
        public boolean matchesAt(int offset, CeList ceList2) {
            if (ces.size() - offset < ceList2.size()) {
                return false;
            }
            for (int i = offset, j = 0; j < ceList2.size(); ++i, j++) {
                if (ces.get(i) != ceList2.get(j)) {
                    return false;
                }
            }
            return true;
        }

        public void add(int ce) {
            ces.add(ce);
        }

        public int get(int i) {
            return ces.get(i);
        }

        public int size() {
            return ces.size();
        }

        @Override
        public String toString() {
            final StringBuilder b = new StringBuilder("[");
            for (int i = 0; i < ces.size(); ++i) {
                if (i != 0) {
                    b.append(", ");
                }
                b.append(Integer.toString(ces.get(i), 16));
            }
            return b.append("]").toString();
        }
    }
}
