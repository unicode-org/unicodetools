package org.unicode.text.tools;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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

    final Node base = new Node(new UnicodeSet());

    final Map<UnicodeSet,T> data = new TreeMap<UnicodeSet,T>(LONGEST);

    static interface Merger<T> {
        T merge(T a, T b);
    }

    final Merger<T> merger;

    public UnicodeSetTree(Merger<T> merger2) {
        merger = merger2;
    }

    UnicodeSetTree<T> add(T name, UnicodeSet set) {
        T old = data.get(set);
        if (old != null) {
            name = merger.merge(old, name);
        };
        data.put(set, name);
        base.parent.addAll(set);
        return this;
    }

    void cleanup() {
        UnicodeSet lastEntry = null;
        base.parent.freeze();

        for (UnicodeSet entry : data.keySet()) {
            if (entry.equals(lastEntry)) {
                throw new IllegalArgumentException("Duplicate: " + entry);
            }
            base.add(entry);
            lastEntry = entry;
        }
    }

    public T get(UnicodeSet key) {
        return data.get(key);
    }

    static interface Visitor {
        public <T> void show(UnicodeSetTree<T> tree, Node node, int indent);
        public void showRemainder(UnicodeSet remainder, int indent);
    }

    void print(Visitor visitor) {
        print(base, 0, visitor);
    }

    private void print(Node node, int indent, Visitor visitor) {
        if (node.parent != null) {
            visitor.show(this, node, indent);
        }
        if (node.children.isEmpty()) {
            return;
        }
        ++indent;
        UnicodeSet remainder = new UnicodeSet(node.parent);
        for (Node child : node.children) {
            print(child, indent, visitor);
            remainder.removeAll(child.parent);
        }
        if (remainder.size() != 0) {
            visitor.showRemainder(remainder, indent);
        }
    }
}
