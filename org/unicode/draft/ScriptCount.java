package org.unicode.draft;

import org.unicode.cldr.util.Counter;

import com.ibm.icu.lang.UCharacter;

import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.Normalizer2.Mode;

public class ScriptCount {
public static void main(String[] args) {
  for (int script = 0; script < UScript.CODE_LIMIT; ++script) {
    UnicodeSet samples = new UnicodeSet().applyIntPropertyValue(UProperty.SCRIPT, script).retainAll(new UnicodeSet("[:l:]"));
    if (samples.size() == 0) continue;
    String first = samples.iterator().next();
    System.out.println(UScript.getName(script) + "\t" + UScript.getShortName(script) + "\t" + first);
  }
  //if (true) return;
  Counter<Integer> scriptCounter = new Counter<Integer>();
  Counter<String> mulCounter = CharacterFrequency.getCharCounter("mul");
  Normalizer2 nfkd = Normalizer2.getInstance(null, "nfkc", Mode.DECOMPOSE);
  for (String s : mulCounter) {
    long count = mulCounter.getCount(s);
    String norm = nfkd.normalize(s);
    int script = getScript(norm);
    scriptCounter.add(script, count);
  }
  for (Integer script : scriptCounter) {
    System.out.println(UScript.getShortName(script) + "\t" + UScript.getName(script) + "\t" + scriptCounter.get(script));
  }
}

private static int getScript(String norm) {
  int cp;
  int result = UScript.INHERITED;
  for (int i = 0; i < norm.length(); i += Character.charCount(cp)) {
    cp = norm.codePointAt(i);
    int script = UScript.getScript(cp);
    if (script == UScript.UNKNOWN) {
      int type = UCharacter.getType(cp);
      if (type == UCharacter.PRIVATE_USE) {
        script = UScript.BLISSYMBOLS;
      }
    }
    if (script == UScript.INHERITED || script == result) continue;
    if (script == UScript.COMMON) {
      if (result == UScript.INHERITED) {
        result = script;
      }
      continue;
    }
    if (result == UScript.COMMON || result == UScript.INHERITED) {
      result = script;
      continue;
    }
    // at this point both are different explicit scripts
    return UScript.COMMON;
  }
  return result;
}
}
