package org.unicode.jsptest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.unicode.jsp.Typology;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.util.PrettyPrinter;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.UnicodeSet;

public class TestTypology extends TestFmwk {

    public static void main(String[] args) {
        new TestTypology().run(args);
    }

    public void TestSimple() {
        Set<String> archaicLabels = new HashSet<String>(Arrays.asList("Archaic Ancient Biblical Historic".split("\\s")));
        UnicodeSet archaic = new UnicodeSet();
        PrettyPrinter pp = new PrettyPrinter().setOrdering(Collator.getInstance());
//        System.out.println();
//        System.out.println("Label\tSet");
//
//        for (String label : Typology.getLabels()) {
//            UnicodeSet uset = Typology.getSet(label);
//            String labelName = label.length() == 0 ? "<no_label>" : label;
//            showLabel(pp, uset, labelName);
//            if (archaicLabels.contains(label)) {
//                archaic.addAll(uset);
//            }
//        }
//        showLabel(pp, archaic, "(Archaic Ancient Biblical Historic)");

        for (String label : Typology.getLabels()) {
            Set<String> lists = Typology.labelToPath.getAll(label);
            if (lists.size() == 1) {
                
                for (String path : lists) {
                    showLabel(pp, Typology.path_to_uset.get(path), label + "\t" + path);
                }
            }
        }
        for (String label : Typology.getLabels()) {
            Set<String> lists = Typology.labelToPath.getAll(label);
            if (lists.size() > 1) {
                System.out.println();
                for (String path : lists) {
                    showLabel(pp, Typology.path_to_uset.get(path), label + "\t" + path);
                }
            }
        }

    }

    private void showLabel(PrettyPrinter pp, UnicodeSet uset, String labelName) {
        String setString = pp.format(uset);
        if (setString.length() > 100) {
            setString = setString.substring(0,100) + "...";
        }
        System.out.println(labelName + "\t" + uset.size() + "\t" + setString);
    }
}
