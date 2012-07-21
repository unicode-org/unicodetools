package org.unicode.text.UCD;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.CollectionUtilities;
import com.ibm.icu.dev.test.util.PrettyPrinter;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;

public class GetTypology {
  public static void main(String[] args) throws IOException {
    UnicodeMap<String> data2 = new UnicodeMap();
    Map<String,UnicodeSet> data = new TreeMap();
    Map<String,Set<String>> uniqueUnordered = new HashMap();

    Map<String,Map<String,UnicodeSet>> labelToPrefixes = new TreeMap();
    Map<String,Set<String>> toOriginals = new TreeMap();

    String filename = "U52M09XXXX.lst";
    BufferedReader br = BagFormatter.openUTF8Reader(Utility.WORKSPACE_DIRECTORY + "DATA/UCD/", filename);
    StringBuilder name = new StringBuilder();
    String nameString = null;

    while (true) {
      String line = Utility.readDataLine(br);
      if (line == null) break;
      String[] parts = line.split("\t");
      int cp = Integer.parseInt(parts[0],16);
      name.setLength(0);

      for (int i = 0; i < parts.length; ++i) {
        String part = parts[i];
        
        // verify the syntax

        boolean isBracketed = part.startsWith("[") && part.endsWith("]");
        boolean notSupposedToBeBracketed = i == 0 || i == 1 || i == parts.length - 1;

        if (isBracketed == notSupposedToBeBracketed) {
          throw new IllegalArgumentException("Bad syntax on " + line);
        }

        if (notSupposedToBeBracketed || part.equals("[X]") || part.equals("[]")) continue;

        part = part.substring(1, part.length()-1);
        String original = part;
        part = part.replaceAll("[^\\-0-9A-Za-z]", "_");
        Set<String> canonicalized = toOriginals.get(part);
        if (canonicalized == null) {
          toOriginals.put(part, canonicalized = new TreeSet());
        }
        canonicalized.add(original);

        // add the data
        
        UnicodeSet s = data.get(part);
        if (s == null) {
          data.put(part, s = new UnicodeSet());
        }
        s.add(cp);
        String prefix = name.toString();
        Map<String, UnicodeSet> prefixes = labelToPrefixes.get(part);
        if (prefixes == null) {
          labelToPrefixes.put(part, prefixes = new TreeMap<String, UnicodeSet>());
        }
        UnicodeSet samples = prefixes.get(prefix);
        if (samples == null) {
          prefixes.put(prefix, samples = new UnicodeSet());
        }
        samples.add(cp);

        if (name.length() != 0) {
          name.append("|");
        }
        name.append(part);
        nameString = name.toString();
        TreeSet nameSet = new TreeSet(Arrays.asList(nameString.toLowerCase().split("\\|")));
        String unorderedName = CollectionUtilities.join(nameSet, "|").toLowerCase();
        Set<String> names = uniqueUnordered.get(unorderedName);
        if (names == null) {
          uniqueUnordered.put(unorderedName, names = new TreeSet());
        }
        names.add(nameString);
      }
      data2.put(cp, nameString);
    }
    br.close();

    PrintWriter out = BagFormatter.openUTF8Writer(Utility.WORKSPACE_DIRECTORY + "Generated/classification", "classification_analysis.txt");
    out.println("# Source:\t" + filename);
    int count;
    out.println();
    
    count = 0;
    out.println("@ Problems with unique ordering/casing of labels");
    out.println("# Format:");
    out.println("# unordered_uncased_label ; label(s)");
    out.println();
    for (String unorderedLabels : uniqueUnordered.keySet()) {
      Set<String> labels = uniqueUnordered.get(unorderedLabels);
      if (labels.size() != 1) {
        out.println(unorderedLabels + " ;\t" + labels);
        ++count;
      }
    }
    out.println();
    out.println("# Total:\t" + count);

    
    count = 0;
    out.println();
    out.println("@ Problems with label format");
    out.println("# Format:");
    out.println("# label ; unnormalized label(s)");
    out.println();
    for (String label : toOriginals.keySet()) {
      Set<String> originals = toOriginals.get(label);
      if (label.contains("__") || originals.size() != 1) {
        out.println(label + " ;\t" + originals);
        ++count;
      }
    }
    out.println();
    out.println("# Total:\t" + count);
    
    out.println();
    out.println("@ Labels with multiple prefixes");
    out.println("# These need to be examined to ensure consistent semantics of labels with the same prefix.");
    out.println("# Format:");
    out.println("# label ; count; characters");
    out.println();
    showPrefixes(labelToPrefixes, out, false);

    out.println();
    out.println("@ Labels with single prefixes");
    out.println("# Format:");
    out.println("# label ; prefix ; count; characters");
    out.println();
    showPrefixes(labelToPrefixes, out, true);

    out.println();
    out.println("@ Labels to characters");
    out.println("# Format:");
    out.println("# label ; prefix ; count; characters");
    out.println();
    for (String label : data.keySet()) {
      UnicodeSet samples = data.get(label);
      out.println(label + " ;\t" + samples.size() + " ;\t" + showUnicodeSet(samples));
    }
    out.println();
    out.println("# Total:\t" + data.size());

    out.println();
    out.println("@ Label Hierarchy to characters");
    out.println("# Format:");
    out.println("# label ; count; characters");
    out.println();
    TreeSet<String> values = new TreeSet();
    values.addAll(data2.values());
    for (String label : values) {
      UnicodeSet samples = data2.getSet(label);
      out.println(label + " ;\t" + samples.size() + " ;\t" + showUnicodeSet(samples));
    }
    out.println();
    out.println("# Total:\t" + values.size());

    out.close();
  }

  static final UnicodeSet TO_QUOTE = new UnicodeSet("[[:z:][:me:][:mn:][:di:][:c:]-[\u0020]]").freeze();
  static final PrettyPrinter pp = new PrettyPrinter().setToQuote(TO_QUOTE);

  private static void showPrefixes(Map<String, Map<String, UnicodeSet>> labelToPrefixes, PrintWriter out, boolean singles) {
    int count = 0;
    for (String label : labelToPrefixes.keySet()) {
      Map<String, UnicodeSet> prefixes = labelToPrefixes.get(label);
      if (singles != (prefixes.size() == 1)) continue;
      for (String prefix : prefixes.keySet()) {
        UnicodeSet samples = prefixes.get(prefix);
        out.println(label + " ; \t" + prefix + " ;\t" + samples.size() + " ;\t" + showUnicodeSet(samples));
        count++;
      }
    }
    out.println();
    out.println("# Total:\t" + count);
  }

  private static String showUnicodeSet(UnicodeSet samples) {
    String result = pp.format(samples);
    if (result.length() > 120) result = result.substring(0,120) + "…";
    return result;
  }
}
