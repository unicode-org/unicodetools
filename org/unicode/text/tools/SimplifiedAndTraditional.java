package org.unicode.text.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.PrettyPrinter;
import org.unicode.text.UCD.Default;
import org.unicode.text.utility.EquivalenceClass;
import org.unicode.text.utility.Utility;
import org.unicode.text.utility.XEquivalenceClass;

import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public class SimplifiedAndTraditional {
  public static void main(String[] args) {
    new SimplifiedAndTraditional().run(args);
  }

  private void run(String[] args) {
    UnicodeSet x = showKeyset("kHKSCS", new UnicodeSet("[\\U00010000-\\U0010FFFF]"));
    UnicodeSet y = showKeyset("kJIS0213", new UnicodeSet("[\\U00010000-\\U0010FFFF]"));
    System.out.println("intersection" + ":\t" + PRETTY.toPattern(x.retainAll(y)));
  }

  private UnicodeSet showKeyset(String propertyName, UnicodeSet filter) {
    UnicodeMap mand = Default.ucd().getHanValue(propertyName);
    UnicodeSet supplementals = new UnicodeSet(filter).retainAll(mand.keySet());
    System.out.println(propertyName + ":\t" + PRETTY.toPattern(supplementals));
    return supplementals;
  }

  private void run3(String[] args) {
    Map<String,UnicodeSet> mandarin = new TreeMap();
    UnicodeMap mand = Default.ucd().getHanValue("kMandarin");
    for (String value : (Collection<String>) mand.getAvailableValues()) {
      final UnicodeSet sources = mand.getSet(value);
      String[] pieces = value.split("\\s");
      for (String piece : pieces) {
        piece = piece.trim();
        UnicodeSet set = mandarin.get(piece);
        if (set == null) mandarin.put(piece, set = new UnicodeSet());
        set.addAll(sources);
      }
    }
    UnicodeSet biggest = new UnicodeSet();
    for (String value : mandarin.keySet()) {
      UnicodeSet set = mandarin.get(value);
      if (set.size() > biggest.size()) {
        biggest = set;
      }
    }
    System.out.println(PRETTY.toPattern(biggest));
  }

  static     PrettyPrinter PRETTY = new PrettyPrinter();
  private void run2(String[] args) {
    
    
    UnicodeMap simp2trad = Default.ucd().getHanValue("kTraditionalVariant");
    UnicodeMap trad2simp = Default.ucd().getHanValue("kSimplifiedVariant");
    
    XEquivalenceClass equivalences = new XEquivalenceClass("?");

    System.out.println("*** Data Problems ***");
    System.out.println();

    for (UnicodeSetIterator it = new UnicodeSetIterator(simp2trad.keySet()); it.next();) {
      final String source = it.getString();
      final String target = getVariant(simp2trad, it.codepoint);
      if (source.equals(target)) {
        System.out.println("Source = Target: " + source + "\t→T\t" + target + "\t!DATA ERROR?!");
        continue;
      }
      equivalences.add(source, target, "→T", "T←");
    }
    for (UnicodeSetIterator it = new UnicodeSetIterator(trad2simp.keySet()); it.next();) {
      final String source = it.getString();
      final String target = getVariant(trad2simp, it.codepoint);
      if (source.equals(target)) {
        System.out.println("Source = Target: " + source + "\t→S\t" + target + "\t!DATA ERROR?!");
        continue;
      }
      equivalences.add(source, target, "→S", "S←");
      //equivalences.add(it.getString(), getVariant(trad2simp, it.codepoint), "→S", "S←");
    }
    
    System.out.println("*** Simple Pairs ***");
    System.out.println();
    final Set<Set<String>> equivalenceSets = (Set<Set<String>>)equivalences.getEquivalenceSets();
    Set<Set<String>> seenEquivalences = new HashSet();
    for (Set<String> equivSet : equivalenceSets) {
      if (equivSet.size() != 2) continue;
      ArrayList<String> list = new ArrayList(equivSet);
      String reasonString = equivalences.getReasons(list.get(0), list.get(1)).toString();
      // S↔T
      if (reasonString.equals("[[[S←, →T]]]")) {
        System.out.println(list.get(0) + "\tS↔T\t" + list.get(1));
        seenEquivalences.add(equivSet);
      } else if (reasonString.equals("[[[→S, T←]]]")) {
        System.out.println(list.get(1) + "\tS↔T\t" + list.get(0));
        seenEquivalences.add(equivSet);
      }
    }
    
    System.out.println();
    System.out.println("*** Complicated Relations ***");
    System.out.println();
    
    for (Set<String> equivSet : equivalenceSets) {
      if (seenEquivalences.contains(equivSet)) continue;
      System.out.println("Equivalence Class:\t" + equivSet);
      Set<String> lines = new TreeSet();

      for (String item : equivSet) {
        for (String item2 : equivSet) {
          if (item.equals(item2)) continue;
          Set reason = equivalences.getReason(item, item2);
          if (reason == null) continue;
          String reasonString = reason.toString();
          reasonString = reasonString.substring(1,reasonString.length()-1);
          String line;
          if (reasonString.equals("S←, →T")) {
            line = (item + "\tS↔T\t" + item2);
          } else if (reasonString.equals("S←")) {
            line = (item + "\tS←\t" + item2);
          } else if (reasonString.equals("→T")) {
            line = (item + "\t→T\t" + item2);
          // reverse the following
          } else if (reasonString.equals("→S, T←")) {
            line = (item2 + "\tS↔T\t" + item);
          } else if (reasonString.equals("T←")) {
            line = (item2 + "\t→T\t" + item);
          } else if (reasonString.equals("→S")) {
            line = (item2 + "\tS←\t" + item);
          } else {    
            line = (item + "\t" + reasonString + "\t" + item2 + "\t!DATA ERROR?!");
          }
          if (item.contains(item2) || item2.contains(item)) line += "\t!CONTAINS?!";
          lines.add(line);
        }
     }
      for (String line : lines) {
        System.out.println(line);
      }
      System.out.println();
    }
    
    if (true) return;
    // ==============================
    
    System.out.println("x →T y & x →S z");
    UnicodeSet both = new UnicodeSet(simp2trad.keySet()).retainAll(trad2simp.keySet());
    for (UnicodeSetIterator it = new UnicodeSetIterator(both); it.next();) {
      System.out.println(it.getString() + "\t→T\t" + getVariant(simp2trad, it.codepoint));
      System.out.println(it.getString() + "\t→S\t" + getVariant(trad2simp, it.codepoint));
      System.out.println();
    }
    
    System.out.println("y →T x & z →S x");
    Set<String> bothValues = new TreeSet<String>(simp2trad.getAvailableValues());
    bothValues.retainAll(trad2simp.getAvailableValues());
    for (String value : bothValues) {
      UnicodeSet simpSource = simp2trad.getSet(value);
      UnicodeSet tradSource = trad2simp.getSet(value);
      System.out.println(simpSource.toPattern(false) + "\t→T\t" + hexToString(value));
      System.out.println(tradSource.toPattern(false) + "\t→S\t" + hexToString(value));
      System.out.println();
    }
    
    System.out.println("\tS↔T\t");
    List<String> output = new ArrayList();
    Set<String> seen = new HashSet();
    Set<String> buffered = new LinkedHashSet();
    addItems(simp2trad, trad2simp, output, seen, false, buffered);
    System.out.println();
    
    System.out.println("x\t→S\ty\t...");
    for (String line : buffered) {
      System.out.println(line);
    }
    System.out.println();
    buffered.clear();
    
    addItems(trad2simp, simp2trad, output, seen, true, buffered);
    System.out.println();
    
    System.out.println("x\t→T\ty\t...");
    for (String line : buffered) {
      System.out.println(line);
    }
    System.out.println();
  }

  private void addItems(UnicodeMap simp2trad, UnicodeMap trad2simp, List<String> output,
          Set<String> seen, boolean isTrad2Simp, Set<String> buffered) {
    for (UnicodeSetIterator it = new UnicodeSetIterator(simp2trad.keySet()); it.next();) {
      final String string = it.getString();
      if (seen.contains(string)) {
        continue;
      }
      output.clear();
      if (isTrad2Simp) {
        output.add("");
      }
      output.add(string);
      int circular = getVariants(simp2trad, trad2simp, it.codepoint, output);
      seen.addAll(output);

      boolean first = true;
      if (circular == 0 && output.size() == 2) {
        System.out.println(output.get(0) + "\tS↔T\t" + output.get(1));
        continue;
      }
      boolean toTrad = true;
      StringBuffer line = new StringBuffer();
      for (String code : output) {
        if (first) {
          first = false;
        } else if (toTrad) {
          line.append("\t→T\t");
        } else {
          line.append("\t→S\t");
        }
        line.append(code);
        toTrad = !toTrad;
      }
      if (circular >= 0) {
        line.append("\t→\t" + circular);
      }
      buffered.add(line.toString());
    }
  }

  private int getVariants(UnicodeMap v1, UnicodeMap v2, int codepoint, List<String> output) {
    String x = getVariant(v1, codepoint);
    if (x == null) {
      return -1;
    }
    int found = output.indexOf(x);
    if (found >= 0) {
      return found;
    }
    output.add(x);
    if (UTF16.countCodePoint(x) != 1) {
      return -1;
    }
    return getVariants(v2, v1, UTF16.charAt(x, 0), output);
  }

  private String getVariant(UnicodeMap v1, int codepoint) {
    String trad = (String) v1.getValue(codepoint);
    String result;
    if (trad == null) {
      result = null;
    } else {
      result = hexToString(trad);
      if (result.length() == 0) {
        System.out.println("Problem at " + Utility.hex(codepoint) + " => " + trad);
        return null;
      }
    }
    return result;
  }

  private String hexToString(String trad) {
    trad = trad.replace("U+", "");
    return Utility.fromHex(trad);
  }
}
