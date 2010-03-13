package org.unicode.jsp;

import java.util.BitSet;
import java.util.Map;
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

  /**
   * If the scripts in the string are compatible, then returns a list of them. Otherwise returns an empty bitset.
   * @param input
   * @return bitset of scripts found
   */
  public BitSet test(CharSequence input) {
    int cp;
    BitSet compatible = new BitSet(LIMIT);
    BitSet found = new BitSet(LIMIT);
    compatible.or(ALL);
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

  public static BitSet getScriptSpecials(int codepoint) {
    BitSet output = new BitSet(LIMIT);
    BitSet actualScripts = scriptSpecials.get(codepoint);
    output.or(actualScripts);
    return output;
  }

  private ScriptTester(UnicodeMap<BitSet> character_scripts) {
    this.character_scripts = character_scripts;
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

  public static class Builder {

    private final Map<Integer, BitSet> compatible = new TreeMap<Integer, BitSet>();
    private final UnicodeMap<BitSet> char2scripts = new UnicodeMap<BitSet>();

    public Builder(CompatibilityLevel level, ScriptSpecials specials) {
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
        addCompatible(UScript.LATIN, HANT, UScript.HIRAGANA, UScript.KATAKANA);
        addCompatible(UScript.LATIN, HANT, UScript.BOPOMOFO);
        addCompatible(UScript.LATIN, HANT, UScript.HANGUL);
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
