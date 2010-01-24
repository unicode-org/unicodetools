package org.unicode.jsp;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
    for (String label : Typology.getLabels()) {
      UnicodeSet uset = Typology.getSet(label);
      System.out.println(label + "\t" + pp.format(uset));
      if (archaicLabels.contains(label)) {
        archaic.addAll(uset);
      }
    }
    System.out.println("*all-archaic\t" + pp.format(archaic));
  }
}
