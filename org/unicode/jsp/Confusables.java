package org.unicode.jsp;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.jsp.AlternateIterator.Builder;

import com.ibm.icu.dev.test.util.CollectionUtilities;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.dev.test.util.XEquivalenceClass;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.Normalizer.Mode;

public class Confusables implements Iterable<String>{
  public enum ScriptCheck {same, none};

  private static final XEquivalenceClass<String, Integer> equivalents = new XEquivalenceClass<String, Integer>();
  private String source;
  private Mode normalizationCheck;
  private ScriptCheck scriptCheck = ScriptCheck.none;
  private UnicodeSet allowedCharacters = null;

  public UnicodeSet getAllowedCharacters() {
    return allowedCharacters;
  }
  
  public static UnicodeMap<String> getMap() {
    UnicodeMap<String> result = new UnicodeMap<String>();
    for (String s : equivalents) {
      Set<String> others = new TreeSet(equivalents.getEquivalences(s));
      String list = "\u2051" + CollectionUtilities.join(others, "\u2051") + "\u2051";
      for (String other : others) {
        result.put(other, list);
      }
    }
    result.freeze();
    return result;
  }

  public Confusables setAllowedCharacters(UnicodeSet allowedCharacters) {
    this.allowedCharacters = allowedCharacters;
    return this;
  }

  public Mode getNormalizationCheck() {
    return normalizationCheck;
  }

  public Confusables setNormalizationCheck(Mode normalizationCheck) {
    this.normalizationCheck = normalizationCheck;
    return this;
  }

  static class MyReader extends FileUtilities.SemiFileReader {
    @Override
    protected void handleLine(int start, int end, String[] items) {
      String type = items[2];
      if (!type.equals("MA")) return;
      String result = Utility.fromHex(items[1], 4, " ");
      for (int i = start; i <= end; ++i) {
        equivalents.add(UTF16.valueOf(i), result);
      }
    }
  }

  static {
    new MyReader().process(Confusables.class, "confusables.txt");
  }

  public Confusables(String source) {
    this.source = Normalizer.normalize(source,Normalizer.NFD);
  }
  
  public double getMaxSize() {
    AlternateIterator build = buildIterator();
    if (build == null) {
      return 0;
    }
    return build.getMaxSize();
  }

  public Iterator<String> iterator() {
    AlternateIterator build = buildIterator();
    if (build == null) {
      return ((Set<String>)Collections.EMPTY_SET).iterator();
    }
    return new MyFilteredIterator(build);
  }

  private AlternateIterator buildIterator() {
    Builder builder = AlternateIterator.start();
    List<Set<String>> table = new ArrayList<Set<String>>();
    int cp;
    for (int i = 0; i < source.length(); i += Character.charCount(cp)) {
      cp = source.codePointAt(i);
      String cps = UTF16.valueOf(cp);
      Set<String> confusables = equivalents.getEquivalences(cps);
      Set<String> items = new HashSet<String>();
      for (String confusable : confusables) {
        if (normalizationCheck != null && !Normalizer.isNormalized(confusable, normalizationCheck, 0)) {
          continue;
        }
        if (allowedCharacters != null && !allowedCharacters.containsAll(confusable)) {
          continue;
        }
        items.add(confusable);
      }
      if (items.size() == 0) {
        return null;
      }
      table.add(items);
    }
    
    // now filter for multiple scripts, if set
    if (scriptCheck != ScriptCheck.none) {
      BitSet scripts = new BitSet();
      scripts.set(0, UScript.CODE_LIMIT);
      for (Set<String> items : table) {
        BitSet itemScripts = new BitSet();
        for (String item : items) {
          addStringScripts(item, itemScripts);
        }
        if (itemScripts.get(UScript.COMMON) || itemScripts.get(UScript.INHERITED) || itemScripts.get(UScript.UNKNOWN)) {
          continue; // item works with everything
        }
        scripts.and(itemScripts);
      }
      // at this point, scripts contains all the scripts that occur in every row.
      scripts.set(UScript.COMMON);
      scripts.set(UScript.INHERITED);
      scripts.set(UScript.UNKNOWN);
      // we'll now remove items that aren't in those scripts
      
      for (Set<String> items : table) {
        BitSet itemScripts = new BitSet();
        for (Iterator<String> it = items.iterator(); it.hasNext();) {
          String item = it.next();
          itemScripts.clear();
          if (!containsScripts(scripts, item)) {
            it.remove();
          }
        }
        if (items.size() == 0) {
          return null;
        }
      }
    }
    for (Set<String> items : table) {
      builder.add(items);
    }
    AlternateIterator build = builder.build();
    return build;
  }
  
  private boolean containsScripts(BitSet scripts, String string) {
    int cp;
    for (int i = 0; i < string.length(); i += Character.charCount(cp)) {
      cp = string.codePointAt(i);
      int script = UScript.getScript(cp);
      if (!scripts.get(script)) {
        return false;
      }
    }
    return true;
  }

  // Ugly hack, because BitSet doesn't have the method.
  private boolean contains(BitSet set1, BitSet set2) {
    // quick check to verify intersecting
    if (!set1.intersects(set2)) {
      return false;
    }
    BitSet temp = new BitSet();
    temp.or(set2);
    temp.and(set1);
    // we now have the intersection. It must be equal to set2
    return temp.equals(set2);
  }

  private BitSet addStringScripts(String source, BitSet result) {
    int cp;
    for (int i = 0; i < source.length(); i += Character.charCount(cp)) {
      cp = source.codePointAt(i);
      int script = UScript.getScript(cp);
      result.set(script);
    }
    return result;
  }

  public List<Collection<String>> getAlternates() {
    AlternateIterator build = buildIterator();
    if (build == null) {
      return ((List<Collection<String>>)Collections.EMPTY_LIST);
    }
    return build.getAlternates();
  }

  public ScriptCheck getScriptCheck() {
    return scriptCheck;
  }

  public void setScriptCheck(ScriptCheck scriptCheck) {
    this.scriptCheck = scriptCheck;
  }

  public static boolean scriptOk(String confusable, ScriptCheck scriptCheck) {
    if (scriptCheck == ScriptCheck.none) {
      return true;
    }
    int lastScript = UScript.UNKNOWN;
    int cp;
    for (int i = 0; i < confusable.length(); i += Character.charCount(cp)) {
      cp = confusable.codePointAt(i);
      int script = UScript.getScript(cp);
      if (i == 0) {
        lastScript = script;
        continue;
      }
      if (script == lastScript || script == UScript.INHERITED) {
        continue;
      }
      if (script == UScript.COMMON) {
        continue;
      }
      if (lastScript == UScript.COMMON) {
        lastScript = script;
        continue;
      }
      return false;
    }
    return true;
  }

  class MyFilteredIterator extends FilteredIterator<String>{
    Set<String> alreadySeen;;

    public MyFilteredIterator(Iterator<String> base) {
      super(base);
    }

    @Override
    public String allow(String confusable) {
      if (alreadySeen == null) {
        alreadySeen = new HashSet<String>();
      }
      if (alreadySeen.contains(confusable)) {
        return null;
      }
      alreadySeen.add(confusable);

      String nfcConfusable = Normalizer.normalize(confusable, Normalizer.NFC);
      if (!nfcConfusable.equals(confusable)) {
        if (alreadySeen.contains(nfcConfusable)) {
          return null;
        }
        alreadySeen.add(nfcConfusable);
      }

      if (allowedCharacters != null && !allowedCharacters.containsAll(nfcConfusable)) {
        return null;
      }
      if (!scriptOk(nfcConfusable, scriptCheck)) {
        return null;
      }
      if (normalizationCheck != null && !Normalizer.isNormalized(nfcConfusable, normalizationCheck, 0)) {
        return null;
      }
      return nfcConfusable;
    }

  }

  public static class FilteredIterator<T> implements Iterator<T> {
    Iterator<T> base;
    T nextItem = null;

    public FilteredIterator(Iterator<T> base) {
      this.base = base;
      load();
    }

    public boolean hasNext() {
      return nextItem != null;
    }

    public T next() {
      T temp = nextItem;
      load();
      return temp;
    }

    private void load() {
      while (base.hasNext()) {
        nextItem = allow(base.next());
        if (nextItem != null) {
          return;
        }
      }
      nextItem = null;
    }

    public T allow(T item) {
      return item;
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }

  }

  public String getOriginal() {
    return source;
  }
}
