package org.unicode.text.tools;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.text.UnicodeSet;

public class UnicodeSetTree<T> {
    static boolean SHOW = false;

    static class Node {
        final UnicodeSet parent;
        final Set<Node> children = new LinkedHashSet<>();
        Node(UnicodeSet set) {
            parent = set;
        }
        public boolean add(UnicodeSet entry) {
            return add(new Node(entry));
        }
        // returns false if we don't need addition as a child.
        public boolean add(Node entry) {
            if (parent == null || parent.containsAll(entry.parent)) {
                // if already in children, we've done enough
                if (children.contains(entry)) {
                    return true; // don't need higher addition
                }
                // see if it is in child
                boolean didAdd = false;
                for (Node child : children) {
                    didAdd |= child.add(entry);
                }
                if (!didAdd) { // only add as child if not in any children already
                    if (SHOW) {
                        System.out.println(
                                (parent == null ? "null" : parent.toPattern(false)) 
                                + "\t" + show(children) 
                                + "\t" + entry.parent.toPattern(false));
                    }
                    children.add(entry);
                }
                return true;
            }
            return false;
        }

        private String show(Set<Node> children2) {
            StringBuffer b = new StringBuffer();
            for (Node child : children2) {
                if (b.length() != 0) {
                    b.append(", ");
                }
                b.append(child.parent.toPattern(false));
            }
            return b.toString();
        }
        @Override
        public String toString() {
            return parent + " =>\n" + children;
        }
    }

    private static final Comparator<UnicodeSet> LONGEST = new Comparator<UnicodeSet>() {
        @Override
        public int compare(UnicodeSet o1, UnicodeSet o2) {
            return o1.compareTo(o2,UnicodeSet.ComparisonStyle.LONGER_FIRST);
        }
    };
    Node base = new Node(new UnicodeSet());

    Map<UnicodeSet,T> data = new TreeMap<UnicodeSet,T>(LONGEST);

    UnicodeSetTree<T> add(T name, UnicodeSet set) {
        if (data.put(set, name) != null) {
            throw new IllegalArgumentException("duplicate");
        };
        base.parent.addAll(set);
        return this;
    }

    void cleanup() {
        int i = 0;
        UnicodeSet lastEntry = null;
        base.parent.freeze();
        
        for (UnicodeSet entry : data.keySet()) {
            if (entry.equals(lastEntry)) {
                throw new IllegalArgumentException("Duplicate: " + entry);
            }
//            System.out.println(entry.size() + "\t" + entry.toPattern(false));
//            if (++i > 0 || (++i % 10) == 0) {
//                System.out.println(i);
//                if (i == 72) {
//                    SHOW = true;
//                }
//            }
            base.add(entry);
            lastEntry = entry;
        }
    }

    void print() {
        print(base, "");
    }

    private void print(Node node, String indent) {
        if (node.parent != null) {
            System.out.println(indent 
                    + node.parent.size() 
                    + (node.children.isEmpty() ? "\t" + node.parent.toPattern(false) : "")
                    + "\t" + data.get(node.parent)
                    );
        }
        if (node.children.isEmpty()) {
            return;
        }
        indent = "\t" + indent;
        UnicodeSet remainder = new UnicodeSet(node.parent);
        for (Node child : node.children) {
            print(child, indent);
            remainder.removeAll(child.parent);
        }
        if (remainder.size() != 0) {
            System.out.println(indent 
                    + remainder.size() 
                    + "\t" + remainder.toPattern(false)
                    + "\t" + "OTHER"
                    );
        }
    }

    public static void main(String[] args) {
        Relation<UnicodeSet,String> data2 = Relation.of(new HashMap(), HashSet.class);
        for (Entry<String, Set<String>> foo : GenerateEmoji.ANNOTATIONS_TO_CHARS.keyToValues.keyValuesSet()) {
            data2.put(new UnicodeSet().addAll(foo.getValue()), foo.getKey());
        };
        UnicodeSetTree<Set<String>> sample = new UnicodeSetTree<>();
        for (Entry<UnicodeSet, Set<String>> entry : data2.keyValuesSet()) {
            sample.add(entry.getValue(), entry.getKey());
        }
//        for (Entry<UnicodeSet, Set<String>> entry : sample.data.entrySet()) {
//            System.out.println(entry.getValue() + "\t" + entry.getKey());
//        }
        sample.cleanup();
        sample.print();
    }
}
