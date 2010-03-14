package org.unicode.jsp;

import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.UnicodeSet;

/**
 * Class for testing whether strings have allowed combinations of multiple scripts.
 * @author markdavis
 */
public class ScriptTester {
  private final UnicodeMap<BitSet> character_scripts;


  public enum CompatibilityLevel {Highly_Restrictive, Moderately_Restrictive}
  public enum ScriptSpecials {on, off}

  /**
   * Extended scripts; note that they do not have stable numbers, and should not be persisted.
   */
  public static final int 
  HANT = UScript.CODE_LIMIT + 1,
  HANS = HANT + 1,
  LIMIT = HANS + 1;

  private static final BitSet ALL = new BitSet(LIMIT); // be careful when using this; can't freeze it!
  static {
    ALL.set(0, LIMIT, true);
  }

  /**
   * Build a ScriptTester
   * @return
   */
  public static Builder start(CompatibilityLevel level, ScriptSpecials specials) {
    return new Builder(level, specials);
  }

  public static Builder start() {
    return new Builder(CompatibilityLevel.Highly_Restrictive, ScriptSpecials.on);
  }

  public static Builder start(CompatibilityLevel level) {
    return new Builder(level, ScriptSpecials.on);
  }
  

  /**
   * If the scripts in the string are compatible, then returns a list of them. Otherwise returns an empty bitset.
   * @param input
   * @return bitset of scripts found
   */
  public BitSet test(CharSequence input) {
    return getFoundAndCompatible(input, new BitSet(LIMIT), new BitSet(LIMIT));
  }

  private BitSet getFoundAndCompatible(CharSequence input, BitSet found, BitSet compatible) {
    found.clear();
    compatible.or(ALL);
    int cp;
    for (int i = 0; i < input.length(); i += Character.charCount(cp)) {
      cp = Character.codePointAt(input, i);
      BitSet scripts = character_scripts.get(cp); // will never fail
      compatible.and(scripts);
      // now the actual
      BitSet actualScripts = scriptSpecials.get(cp);
      if (actualScripts != null) {
        found.or(actualScripts);
      } else {
        int script = UCharacter.getIntPropertyValue(cp, UProperty.SCRIPT);
        if (script == UScript.HAN) {
          found.set(HANT);
          found.set(HANS);
        } else {
          found.set(script);
        }
      }
    }
    found.and(compatible);
    return found;
  }

  public boolean filterTable(List<Set<String>> table) {
    // this gets tricky. We go through each string, in each set. 
    // For the characters in each string, we gather the compatible (ANDing)
    // We then OR those in for the set.
    // Across the sets in the list, we AND them.
    BitSet itemFound = new BitSet();
    BitSet itemCompatible = new BitSet();

    BitSet overallCompatible = new BitSet();
    overallCompatible.or(ALL);
    for (Set<String> items : table) {
      BitSet setCompatible = new BitSet();
      for (String item : items) {
        getFoundAndCompatible(item, itemFound, itemCompatible);
        setCompatible.or(itemCompatible);
      }
      overallCompatible.and(setCompatible);
    }
    // at this point, compatible contains all the scripts that occur in every row.
    // we'll now remove items that aren't in those scripts
    
    for (Set<String> items : table) {
      for (Iterator<String> it = items.iterator(); it.hasNext();) {
        String item = it.next();
        if (!isCompatible(item, overallCompatible)) {
        //getFoundAndCompatible(item, itemFound, itemCompatible);
        //if (!contains(overallCompatible, itemFound)) {
          it.remove();
        }
      }
      if (items.size() == 0) {
        return false;
      }
    }
    return true;
  }
  
  /**
   * Each character in item has a compatible set that intersects overall.
   * @param item
   * @param overallCompatible
   * @return
   */
  private boolean isCompatible(String input, BitSet overallCompatible) {
    int cp;
    for (int i = 0; i < input.length(); i += Character.charCount(cp)) {
      cp = Character.codePointAt(input, i);
      BitSet scripts = character_scripts.get(cp); // will never fail
      if (!scripts.intersects(overallCompatible)) {
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

  private static class MyHandler extends FileUtilities.SemiFileReader {
    public final static Pattern SPACES = Pattern.compile("\\s+");

    UnicodeMap<BitSet> map = new UnicodeMap<BitSet>();

    public void handleLine(int start, int end, String[] items) {
      BitSet bitSet = new BitSet(LIMIT);
      for (String script : SPACES.split(items[1])) {
        int scriptCode = script.equalsIgnoreCase("hant") ? HANT
                : script.equalsIgnoreCase("hans") ? HANS
                        : UCharacter.getPropertyValueEnum(UProperty.SCRIPT, script);
        bitSet.set(scriptCode);
      }
      map.putAll(start, end, bitSet);
    }
  }

  private static UnicodeMap<BitSet> scriptSpecials = ((MyHandler) new MyHandler().process(Confusables.class, "scriptSpecials.txt")).map;

  public static BitSet getScriptSpecials(int codepoint) {
    BitSet actualScripts = scriptSpecials.get(codepoint);
    if (actualScripts == null) {
      int script = UCharacter.getIntPropertyValue(codepoint, UProperty.SCRIPT);
      if (script != UScript.HAN) {
        return null;
      }
      BitSet output = new BitSet(LIMIT);
      output.set(HANT);
      output.set(HANS);
      return output;
    }
    BitSet output = new BitSet(LIMIT);
    output.or(actualScripts);
    return output;
  }

  private ScriptTester(UnicodeMap<BitSet> character_scripts) {
    this.character_scripts = character_scripts;
  }

  public static class Builder {

    private final Map<Integer, BitSet> compatible = new TreeMap<Integer, BitSet>();
    private final UnicodeMap<BitSet> char2scripts = new UnicodeMap<BitSet>();

    private Builder(CompatibilityLevel level, ScriptSpecials specials) {
      // make everything compatible with itself
      for (int i = 0; i < LIMIT; ++i) {
        BitSet itself = new BitSet(LIMIT);
        itself.set(i);
        compatible.put(i, itself);
      }
      // first do levels
      switch (level) {
      case Moderately_Restrictive:
        for (int i = 0; i < LIMIT; ++i) {
          if (i == UScript.CYRILLIC || i == UScript.GREEK || i == UScript.CHEROKEE) {
            continue;
          }
          addCompatible(UScript.LATIN, i);
        }
        // FALL THRU!
      case Highly_Restrictive: 
        addCompatible(UScript.LATIN, UScript.HAN, UScript.HIRAGANA, UScript.KATAKANA);
        addCompatible(UScript.LATIN, HANT, UScript.HIRAGANA, UScript.KATAKANA);
        addCompatible(UScript.LATIN, HANS, UScript.HIRAGANA, UScript.KATAKANA);
        
        addCompatible(UScript.LATIN, UScript.HAN, UScript.HANGUL);
        addCompatible(UScript.LATIN, HANT, UScript.HANGUL);
        addCompatible(UScript.LATIN, HANS, UScript.HANGUL);
        
        addCompatible(UScript.LATIN, HANT, UScript.BOPOMOFO);
        addCompatible(UScript.LATIN, HANS);
        // ?? Asomtavruli, Nuskhuri, and Mkhedruli (georgian)
        // FALL THRU!
      default:
        addCompatible(UScript.HAN, HANT);
        addCompatible(UScript.HAN, HANS);
        // Common and Inherited are compatible with everything!
        for (int i = 0; i < LIMIT; ++i) {
          addCompatible(UScript.COMMON, i);
          addCompatible(UScript.INHERITED, i);
        }
      }
      // then specials
      // fix the char2scripts mapping

      if (specials == ScriptSpecials.on){
        char2scripts.putAll(scriptSpecials);
      }
    }

    public ScriptTester get() {
      UnicodeMap<BitSet> character_scripts = new UnicodeMap<BitSet>();
      // first set all the simple cases: character => script => scripts
      for (int script = 0; script < UScript.CODE_LIMIT; ++script) {
        UnicodeSet uset = new UnicodeSet();
        uset.applyIntPropertyValue(UProperty.SCRIPT, script);
        if (uset.size() != 0) {
          BitSet scripts = compatible.get(script);
          character_scripts.putAll(uset, scripts);
        }
      }
      // now override these (as necessary) with the charScriptMapping
      for (BitSet scripts : char2scripts.values()) {
        // The scripts need fluffing up according to the acceptableTogether sets
        // We have to create new Bitsets!
        BitSet fluffed = new BitSet(LIMIT);
        fluffed.or(scripts);
        for (int unfluffedScript = scripts.nextSetBit(0); unfluffedScript >= 0; unfluffedScript = scripts.nextSetBit(unfluffedScript+1)) {
          BitSet acceptable = compatible.get(unfluffedScript);
          fluffed.or(acceptable);
        }
        UnicodeSet uset = char2scripts.getSet(scripts);
        character_scripts.putAll(uset, fluffed);
      }
      return new ScriptTester(character_scripts);
    }
    /**
     * Add list of scripts that are acceptable in combination together.
     * <p>Example: st.addAcceptable(UScript.LATIN, USCRIPT.HANGUL);</p>
     * @param scripts
     */
    public Builder addCompatible(int... scripts) {
      // set all the scripts on each of the other scripts
      for (int script : scripts) {
        BitSet items = compatible.get(script);
        for (int script2 : scripts) {
          items.set(script2);
        }
      }
      return this;
    }

    /**
     * Add mapping from code point to scripts
     * <p>Example: st.addMapping(0x, USCRIPT.HIRAGANA, USCRIPT.KATAKANA); // U+30FC KATAKANA-HIRAGANA PROLONGED SOUND MARK</p>
     */
    public Builder addMapping(int codePoint, int... scripts) {
      BitSet newScripts = new BitSet(LIMIT);
      BitSet oldScripts = char2scripts.get(codePoint);
      if (oldScripts != null) {
        newScripts.or(oldScripts);
      }
      for (int script : scripts) {
        newScripts.set(script);
      }
      char2scripts.put(codePoint, newScripts);
      return this;
    }
  }
}
