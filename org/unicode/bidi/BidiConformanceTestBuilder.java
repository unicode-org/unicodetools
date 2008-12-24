package org.unicode.bidi;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.ibm.icu.text.UnicodeSet;

public class BidiConformanceTestBuilder {

  private static BitSet SKIPS = new BitSet();
  private static byte[] TYPELIST = new byte[5];
  static {
    // skip RLE, LRE, RLO, LRO, PDF, and BN
    SKIPS.set(BidiReference.RLE);
    SKIPS.set(BidiReference.LRE);
    SKIPS.set(BidiReference.RLO);
    SKIPS.set(BidiReference.LRO);
    SKIPS.set(BidiReference.PDF);
    SKIPS.set(BidiReference.BN);   
  }

  public static void main(String[] args) throws FileNotFoundException {
    int[] linebreaks = new int[1];
    linebreaks[0] = TYPELIST.length;

    int maxCount = 10000;

    Map<String, Set<String>> resultToSource = new TreeMap<String, Set<String>>(SHORTEST_FIRST);
    Map<String, Integer> condensed = new HashMap<String, Integer>();

    main:
      for (byte i0 = BidiReference.TYPE_MIN; i0 <= BidiReference.TYPE_MAX; ++i0) {
        if (i0 == BidiReference.B) {
          continue;
        }
        System.out.println(BidiReference.typenames[i0]);
        TYPELIST[0] = i0;
        for (byte i1 = BidiReference.TYPE_MIN; i1 <= BidiReference.TYPE_MAX; ++i1) {
          if (i1 == BidiReference.B) {
            continue;
          }
          TYPELIST[1] = i1;
          for (byte i2 = BidiReference.TYPE_MIN; i2 <= BidiReference.TYPE_MAX; ++i2) {
            if (i2 == BidiReference.B) {
              continue;
            }
            TYPELIST[2] = i2;
            for (byte i3 = BidiReference.TYPE_MIN; i3 <= BidiReference.TYPE_MAX; ++i3) {
              if (i3 == BidiReference.B) {
                continue;
              }
              TYPELIST[3] = i3;
              for (byte i4 = BidiReference.TYPE_MIN; i4 <= BidiReference.TYPE_MAX; ++i4) {
                TYPELIST[4] = i4;

                String typeString = typesToString(TYPELIST);
                condensed.clear();
                for (byte paragraphEmbeddingLevel = -1; paragraphEmbeddingLevel <= 1; ++paragraphEmbeddingLevel) {

                  final String reorderedIndexes = reorderedIndexes(TYPELIST, paragraphEmbeddingLevel, linebreaks);
                  Integer bitmask = condensed.get(reorderedIndexes);
                  if (bitmask == null) {
                    bitmask = 0;
                  }
                  bitmask |= 1<<(paragraphEmbeddingLevel+1);
                  condensed.put(reorderedIndexes, bitmask);
                }
                for (String reorderedIndexes : condensed.keySet()) {
                  addResult(resultToSource, typeString + "; " + condensed.get(reorderedIndexes), reorderedIndexes);
                }
                maxCount--;
                if (maxCount < 0) {
                  break main;
                }
              }
            }
          }
        }
      }
    final String file = "/Users/markdavis/Desktop/BidiConformance.txt";
    PrintWriter out = new PrintWriter(new FileOutputStream(file));
    System.out.println("Writing: " + file);
    for (int i = BidiReference.TYPE_MIN; i < BidiReference.TYPE_MAX; ++i) {
      UnicodeSet data = new UnicodeSet("[:bidi_class=" + BidiReference.typenames[i] + ":]");
      data.complement().complement();
      out.println("@Type: " + BidiReference.typenames[i] + data);
    }
    int totalCount = 0;
    for (String reorderedIndexes : resultToSource.keySet()) {
      out.println();
      out.println("@Result: " + reorderedIndexes);
      int count = 0;
      for (String sources : resultToSource.get(reorderedIndexes)) {
        out.println(sources);
        ++totalCount;
        ++count;
      }
      out.println("#Count: " + count);
    }
    out.println("#Total Count: " + totalCount);
    out.close();
    System.out.println("Done");
  }

  private static void addResult(Map<String, Set<String>> resultToSource, final String source,
          final String reorderedIndexes) {
    Set<String> sources = resultToSource.get(reorderedIndexes);
    if (sources == null) {
      resultToSource.put(reorderedIndexes, sources = new LinkedHashSet());
    }
    sources.add(source);
  }

  private static String reorderedIndexes(byte[] types, byte paragraphEmbeddingLevel, int[] linebreaks) {
    BidiReference bidi = new BidiReference(types, paragraphEmbeddingLevel);
    int[] reordering = bidi.getReordering(linebreaks);

    StringBuilder result = new StringBuilder();
    int lastItem = -1;
    boolean LTR = true;

    for (int i = 0; i < reordering.length; ++i) {
      final int item = reordering[i];
      if (item < lastItem) {
        LTR = false;
      }
      lastItem = item;
      if (SKIPS.get(types[item])) {
        continue;
      }
      if (result.length() != 0) {
        result.append(" ");
      }
      result.append(item);
    }
    return result.toString();
  }

  private static String typesToString(byte[] types) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < types.length; ++i) {
      if (i != 0) {
        result.append(" ");
      }
      result.append(BidiReference.typenames[types[i]]);
    }
    return result.toString();
  }

  static Comparator<String> SHORTEST_FIRST = new Comparator<String>() {

    public int compare(String o1, String o2) {
      int result = o1.length() - o2.length();
      if (result != 0) {
        return result;
      }
      return o1.compareTo(o2);
    }

  };
}
