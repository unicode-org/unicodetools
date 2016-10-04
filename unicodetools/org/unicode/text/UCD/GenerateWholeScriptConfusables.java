package org.unicode.text.UCD;

import com.ibm.icu.text.SpoofChecker;
import com.ibm.icu.text.UnicodeSet;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.IntStream;

/**
 * A class to detect and generate strings that are whole-script confusable with input strings.
 * 
 * The "public static void main" shows an example of this class's usage. The method
 * {@link #getWholeScriptConfusables} is the primary public endpoint.
 * 
 * This class uses reflection to access some internal machinery from SpoofChecker, so it is not
 * recommended for use in production-ready code.
 * 
 * This script can be run stand-alone by passing a whitespace-separated list of words as the
 * argument list.
 * 
 * @author Shane Carr
 * @see com.ibm.icu.text.SpoofChecker
 */
public class GenerateWholeScriptConfusables {
  final ConfusableGraphNode base;
  final SpoofChecker sc;
  final SpoofCheckerWrapper scw;

  // as of Unicode 9, the longest confusable skeleton is 18 characters
  final static int MAX_CONFUSABLE_STRING_LENGTH = 18;

  // --------------------------
  // Start demo code
  // --------------------------

  public static void main(String[] args) {

    // Ignore these four code points that have low-quality confusable skeletons and produce
    // poor whole-script confusables
    UnicodeSet ignorables = new UnicodeSet();
    ignorables.add('т');
    ignorables.add('ц');
    ignorables.add('τ');
    ignorables.add('к');

    @SuppressWarnings("deprecation")
    SpoofChecker sc = new SpoofChecker.Builder().setAllowedChars(SpoofChecker.RECOMMENDED)
        .setChecks(SpoofChecker.CONFUSABLE).build();
    GenerateWholeScriptConfusables wsc = new GenerateWholeScriptConfusables(sc, ignorables);
    // Loop over each argument as a word
    List<String> resultStrings = new ArrayList<String>();
    List<BitSet> resultScripts = new ArrayList<BitSet>();
    for (String word : args) {
      resultStrings.clear();
      resultScripts.clear();

      // Compute the whole script confusables
      wsc.getWholeScriptConfusables(word, resultStrings, resultScripts);

      // Print out the matches to the console
      String matches = String.join(", ",
          IntStream.range(0, resultStrings.size())
              .mapToObj(i -> (resultStrings.get(i) + " (" + resultScripts.get(i) + ")"))
              .toArray(String[]::new));
      System.out.println(word + ": " + matches);
    }
  }

  // --------------------------
  // Start class definition
  // --------------------------

  /**
   * Create a new WholeScriptConfusables instance and build the internal data structure.
   * 
   * @param sc The SpoofChecker containing the data to use when building the data structure.
   */
  public GenerateWholeScriptConfusables(SpoofChecker sc) {
    this.sc = sc;
    this.scw = new SpoofCheckerWrapper(sc);
    this.base = build(null);
  }

  /**
   * Create a new WholeScriptConfusables instance and build the internal data structure.
   * 
   * @param sc The SpoofChecker containing the data to use when building the data structure.
   * @param ignorables A set of characters to omit from the internal data structure. Useful for
   *        ignoring specific entries from the confusables table.
   */
  public GenerateWholeScriptConfusables(SpoofChecker sc, UnicodeSet ignorables) {
    this.sc = sc;
    this.scw = new SpoofCheckerWrapper(sc);
    this.base = build(ignorables);
  }

  /**
   * Builds a confusable graph based on the provided SpoofData and allowed chars.
   */
  private ConfusableGraphNode build(UnicodeSet ignorables) {
    ConfusableGraphNode base = new ConfusableGraphNode();
    StringBuilder sb = new StringBuilder();
    UnicodeSet allowedChars = sc.getAllowedChars();

    // Loop over all entries in the SpoofData
    for (int i = 0; i < scw.spoofData.length(); i++) {
      int codePoint = scw.spoofData.codePointAt(i);

      // Ignore entries that are not in the allowedChars set.
      if (!allowedChars.contains(codePoint)) {
        continue;
      }

      // Ignore certain code points by the user's request
      if (ignorables != null && ignorables.contains(codePoint)) {
        continue;
      }

      // Add this entry to the data structure.
      sb.setLength(0);
      scw.spoofData.appendValueTo(i, sb);
      ConfusableGraphNode node = base;
      for (int offset = 0; offset < sb.length();) {
        int skeletonCodePoint = sb.codePointAt(offset);
        offset += Character.charCount(skeletonCodePoint);
        node = node.addAndGetTransition(skeletonCodePoint);
      }
      node.addCompletion(codePoint);
    }
    return base;
  }

  /**
   * Computes examples of whole script confusables for the given input string. Appends the example
   * strings to the first output collection, and the corresponding set of scripts to the second
   * output collection.
   *
   * @param input The string for which to compute whole script confusables.
   * @param resultStrings The collection to which to append the whole script confusables.
   * @param resultScripts The collection to which to append the sets of scripts corresponding to the
   *        whole script confusables.
   */
  public void getWholeScriptConfusables(CharSequence input, Collection<String> resultStrings,
      Collection<BitSet> resultScripts) {
    // Compute the skeleton string.
    String skeleton = sc.getSkeleton(input);

    // Allocate space used during traversal. scriptsByIndex might allocate more space than
    // needed since we need to allocate only for the number of code points.
    ConfusableGraphNode[] currentNodes = new ConfusableGraphNode[MAX_CONFUSABLE_STRING_LENGTH];
    WholeScriptIterationSet[] scriptsByIndex = new WholeScriptIterationSet[skeleton.length() + 1];
    BitSet temp = SpoofCheckerWrapper.ScriptSet_new();

    // For each code point in the skeleton string:
    int index = 0;
    for (int utf16Offset = 0; utf16Offset < skeleton.length(); index++) {
      int skeletonCodePoint = skeleton.codePointAt(utf16Offset);
      utf16Offset += Character.charCount(skeletonCodePoint);

      // Push the graph nodes forward.
      // Will forget the node at the end of the array, if such a node exists.
      for (int i = currentNodes.length - 1; i > 0; i--) {
        ConfusableGraphNode prev = currentNodes[i - 1];
        currentNodes[i] = (prev == null ? null : prev.getTransition(skeletonCodePoint));
      }

      // Grab the new node for this skeleton char.
      currentNodes[0] = base.getTransition(skeletonCodePoint);

      WholeScriptIterationSet next = new WholeScriptIterationSet();

      // Perform transition using the skeleton character.
      temp.clear();
      scw.getAugmentedScriptSet(skeletonCodePoint, temp);
      next.addIntersectionsOf(scriptsByIndex[index], temp, skeletonCodePoint);

      // Perform transition using entries from the confusables table.
      for (int i = 0; i < currentNodes.length; i++) {
        ConfusableGraphNode node = currentNodes[i];
        if (node == null) {
          continue;
        }
        UnicodeSet completions = node.getCompletions();
        if (completions == null) {
          continue;
        }
        for (int j = 0; j < completions.size(); j++) {
          int completionCodePoint = completions.charAt(j);
          temp.clear();
          scw.getAugmentedScriptSet(completionCodePoint, temp);
          next.addIntersectionsOf(scriptsByIndex[index - i], temp, completionCodePoint);
        }
      }

      scriptsByIndex[index + 1] = next;
    }

    // Compute the scripts of the input string.
    temp.clear();
    scw.getResolvedScriptSet(input, temp);

    // Add the possible whole scripts to the destination and return.
    scriptsByIndex[index].extractNoOverlap(temp, resultScripts, resultStrings);
  }

  /**
   * Data structure used when generating whole script confusables. Contains a set of script sets,
   * where each script set is mapped to a corresponding string.
   */
  private static class WholeScriptIterationSet {
    private Map<BitSet, String> map = new HashMap<BitSet, String>();

    /**
     * Adds all BitSets that result from intersecting the entries in other1 with other2. If there
     * are N entries in other1, this method will add between 0 and N entries to this instance. For
     * each new entry, records the code point sequence consisting of the previous code point
     * sequence from other1 (if available) and the new codePoint passed to this function. If other1
     * is null, this method behaves the same as add(other2, "", codePoint).
     *
     * For example, if: other1 = { { A }, { A, B }, { C } } other2 = { B, C }
     *
     * then calling this method will add the following sets: { { B }, { C } }
     */
    public void addIntersectionsOf(WholeScriptIterationSet other1, BitSet other2, int codePoint) {
      // Trivial case
      if (other1 == null) {
        add(other2, "", codePoint);
        return;
      }

      // Compute and add intersections
      BitSet temp = SpoofCheckerWrapper.ScriptSet_new();
      for (Entry<BitSet, String> entry : other1.map.entrySet()) {
        temp.clear();
        temp.or(entry.getKey());
        temp.and(other2);
        add(temp, entry.getValue(), codePoint);
      }
    }

    /**
     * Adds the given BitSet to this set if it doesn't already exist. Never keeps a reference to the
     * given BitSet. If the BitSet is added, constructs a new string consisting of the baseString
     * plus the given codePoint, and associates it with the BitSet.
     */
    public void add(BitSet ss, String baseString, int codePoint) {
      if (!map.containsKey(ss)) {
        BitSet copy = (BitSet) ss.clone();
        StringBuilder sb = new StringBuilder(baseString);
        sb.appendCodePoint(codePoint);
        map.put(copy, sb.toString());
      }
    }

    /**
     * Identifies entries in this instance that have no overlap with the query, and unions all of
     * those entries into the destination.
     *
     * For example, if this set contained: { { A }, { A, B }, { B, C }, { D } } and query was { A, B
     * } then { D } would be unioned into destination, as D is the only script contained in a BitSet
     * that has an empty intersection with the query.
     *
     * For all entries added to destination, adds an example string to the given Collection.
     */
    public void extractNoOverlap(BitSet query, Collection<BitSet> resultScripts,
        Collection<String> samples) {
      BitSet temp = SpoofCheckerWrapper.ScriptSet_new();
      for (Entry<BitSet, String> entry : map.entrySet()) {
        BitSet ss = entry.getKey();
        if (ss.isEmpty()) {
          continue;
        }
        temp.clear();
        temp.or(ss);
        temp.and(query);
        if (temp.isEmpty()) {
          resultScripts.add((BitSet) ss.clone());
          samples.add(entry.getValue());
        }
      }
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("<SetOfBitSets { ");
      for (Entry<BitSet, String> ss : map.entrySet()) {
        SpoofCheckerWrapper.ScriptSet_appendStringTo(ss.getKey(), sb);
        sb.append(" ");
      }
      sb.append("}>");
      return sb.toString();
    }
  }

  /**
   * Data structure used for efficient lookup of confusable skeletons. Each node is associated with
   * an entry from a confusable skeleton. Each node has some set of "transitions" (edges from this
   * node to the next node in a skeleton) and "completions" (edges from this node back to the base
   * node). The "transitions" edges each have a prototype character associated with them, and the
   * "completions" edges each have a codepoint associated with them.
   */
  private static class ConfusableGraphNode {
    private Map<Integer, ConfusableGraphNode> edges;
    private UnicodeSet completions;

    public void addCompletion(int confusableCodePoint) {
      if (completions == null) {
        completions = new UnicodeSet();
      }
      completions.add(confusableCodePoint);
    }

    public UnicodeSet getCompletions() {
      return completions;
    }

    public ConfusableGraphNode addAndGetTransition(int skeletonCodePoint) {
      if (edges == null) {
        edges = new HashMap<Integer, ConfusableGraphNode>();
      }
      ConfusableGraphNode destination = edges.get(skeletonCodePoint);
      if (destination == null) {
        destination = new ConfusableGraphNode();
        edges.put(skeletonCodePoint, destination);
      }
      return destination;
    }

    public ConfusableGraphNode getTransition(int skeletonCodePoint) {
      if (edges == null) {
        return null;
      }
      return edges.get(skeletonCodePoint);
    }
  }

  /**
   * A wrapper around SpoofChecker enabling access to various private methods inside. Exposes a
   * public API similar to that of the original SpoofChecker and SpoofData.
   * 
   * Uses reflection to access private members, meaning that this code might break from an ICU
   * update and need to be updated.
   */
  static class SpoofCheckerWrapper {
    SpoofChecker sc;
    SpoofData spoofData;

    // Initialize some useful constants
    static Class<?> SpoofChecker, SpoofData, ScriptSet;
    static {
      try {
        SpoofChecker = Class.forName("com.ibm.icu.text.SpoofChecker");
        SpoofData = Class.forName("com.ibm.icu.text.SpoofChecker$SpoofData");
        ScriptSet = Class.forName("com.ibm.icu.text.SpoofChecker$ScriptSet");
      } catch (ClassNotFoundException | SecurityException e) {
        e.printStackTrace();
      }
    }

    public SpoofCheckerWrapper(SpoofChecker sc) {
      this.sc = sc;
      this.spoofData = new SpoofData();
    }

    private Object invokeMethod(String methodName, Class<?>[] parameterTypes, Object[] parameters) {
      try {
        // Get the method
        Method method = SpoofChecker.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);

        // Call the method on the SpoofChecker object
        return method.invoke(sc, parameters);

      } catch (Exception e) {
        e.printStackTrace();
        return null;
      }
    }

    private Object invokeSpoofDataMethod(String methodName, Class<?>[] parameterTypes,
        Object[] parameters) {
      try {
        // Get the method
        Method method = SpoofData.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);

        // Get the SpoofData object
        Field field = SpoofChecker.class.getDeclaredField("fSpoofData");
        field.setAccessible(true);
        Object spoofData = field.get(sc);

        // Call the method on the SpoofData object
        return method.invoke(spoofData, parameters);

      } catch (Exception e) {
        e.printStackTrace();
        return null;
      }
    }

    private static Object invokeScriptSetMethod(BitSet ss, String methodName,
        Class<?>[] parameterTypes, Object[] parameters) {
      try {
        // Get the method
        Method method = ScriptSet.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);

        // Call the method on the ScriptSet object
        return method.invoke(ss, parameters);

      } catch (Exception e) {
        e.printStackTrace();
        return null;
      }
    }

    // -----------------------------------------------------
    // Simulated interface to SpoofChecker private methods
    // -----------------------------------------------------

    public static BitSet ScriptSet_new() {
      try {
        Constructor<?> constructor = ScriptSet.getDeclaredConstructor(new Class<?>[] {});
        constructor.setAccessible(true);
        return (BitSet) constructor.newInstance();
      } catch (Exception e) {
        e.printStackTrace();
        return null;
      }
    }

    public static void ScriptSet_appendStringTo(BitSet ss, StringBuilder sb) {
      invokeScriptSetMethod(ss, "appendStringTo", new Class<?>[] {StringBuilder.class},
          new Object[] {sb});
    }

    public void getAugmentedScriptSet(int codePoint, BitSet result) {
      invokeMethod("getAugmentedScriptSet", new Class<?>[] {Integer.TYPE, ScriptSet},
          new Object[] {codePoint, result});
    }

    public void getResolvedScriptSet(CharSequence input, BitSet result) {
      // getAugmentedScriptSet/getResolvedScriptSet take a ScriptSet instead of a BitSet
      invokeMethod("getResolvedScriptSet", new Class<?>[] {CharSequence.class, ScriptSet},
          new Object[] {input, result});
    }

    class SpoofData {
      public int length() {
        return (int) invokeSpoofDataMethod("length", new Class<?>[] {}, new Object[] {});
      }

      public int codePointAt(int index) {
        return (int) invokeSpoofDataMethod("codePointAt", new Class<?>[] {Integer.TYPE},
            new Object[] {index});
      }

      public void appendValueTo(int index, StringBuilder sb) {
        invokeSpoofDataMethod("appendValueTo", new Class<?>[] {Integer.TYPE, StringBuilder.class},
            new Object[] {index, sb});
      }
    }
  }
}
