package org.unicode.text.tools;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.unicode.text.tools.UnicodeSetTree.Merger;
import org.unicode.text.tools.UnicodeSetTree.Node;
import org.unicode.text.tools.UnicodeSetTree.Visitor;
import org.unicode.tools.emoji.Emoji;
import org.unicode.tools.emoji.EmojiAnnotations;

import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.UnicodeSet;

public class GenerateAnnotationTree {

    public static void main(String[] args) {
        UnicodeSetTree<Set<String>> sample = build(); 
        htmlPrint(sample);
    }

    private static void simplePrint(UnicodeSetTree<Set<String>> sample) {
        Visitor simpleVisitor = new Visitor() {
            public <T> void show(UnicodeSetTree<T> tree, Node node, int indent) {
                System.out.println(
                        Utility.repeat("\t", indent*3) 
                        + node.parent.size() 
                        + (node.children.isEmpty() ? "\t" + node.parent.toPattern(false) : "")
                        + "\t" + tree.data.get(node.parent)
                        );
            }
            public void showRemainder(UnicodeSet remainder, int indent) {
                System.out.println(Utility.repeat("\t", indent*3) 
                        + remainder.size() 
                        + "\t" + remainder.toPattern(false)
                        + "\t" + "OTHER"
                        );
            }
        };
        sample.print(simpleVisitor);
    }

    private static void htmlPrint(UnicodeSetTree<Set<String>> sample) {
        Visitor simpleVisitor = new Visitor() {
            public <T> void show(UnicodeSetTree<T> tree, Node node, int indent) {
                System.out.println(
                        "<tr>"
                        + "<td colspan='" + indent*3 + "'>&nbsp;</td>"
                        + "<td>" + node.parent.size() + "</td>"
                        + "<td>" + (node.children.isEmpty() ? node.parent.toPattern(false): "") + "</td>"
                        + "<td>" + tree.data.get(node.parent) + "<td>"
                        + "</tr>"
                        );
            }
            public void showRemainder(UnicodeSet remainder, int indent) {
                System.out.println(
                        "<tr>"
                        + "<td colspan='" + indent*3 + "'>&nbsp;</td>"
                        + "<td>" + remainder.size() + "</td>"
                        + "<td>" + remainder.toPattern(false) + "</td>"
                        + "<td><i>" + "OTHER" + "</i><td>"
                        + "</tr>"
                        );
            }
        };
        sample.print(simpleVisitor);
    }


    private static UnicodeSetTree<Set<String>> build() {
        Relation<UnicodeSet,String> data2 = Relation.of(new HashMap<UnicodeSet, Set<String>>(), HashSet.class);
        for (Entry<String, Set<String>> foo : EmojiAnnotations.ANNOTATIONS_TO_CHARS.keyValuesSet()) {
            data2.put(new UnicodeSet().addAll(foo.getValue()), foo.getKey());
        };

        Merger<Set<String>> merger = new Merger<Set<String>>() {
            @Override
            public Set<String> merge(Set<String> a, Set<String> b) {
                LinkedHashSet<String> result = new LinkedHashSet<String>();
                result.addAll(a);
                result.addAll(b);
                return result;
            }
        };

        UnicodeSetTree<Set<String>> sample = new UnicodeSetTree<>(merger);
        for (Entry<UnicodeSet, Set<String>> entry : data2.keyValuesSet()) {
            UnicodeSet set = new UnicodeSet(entry.getKey()).removeAll(Emoji.FLAGS);
            if (set.isEmpty()) {
                continue;
            }
            sample.add(entry.getValue(), set.freeze());
        }
        sample.cleanup();
        return sample;
    }

}
