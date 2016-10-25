package org.unicode.jsptest;

import org.unicode.jsp.AlternateIterator;
import org.unicode.jsp.Confusables;
import org.unicode.jsp.UnicodeJsp;
import org.unicode.jsp.XIDModifications;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.UnicodeSet;

public class TestAlternateIterator extends TestFmwk {
  public static void main(String[] args) {
    new TestAlternateIterator().run(args);
  }
  
  public void TestUI() {
    logln(XIDModifications.getAllowed().toPattern(false));
    logln(UnicodeJsp.getConfusables("\u00c5w", true, false, false, false));
    logln(UnicodeJsp.getConfusables("mark-davis", false, false, false, false));
    logln(UnicodeJsp.getConfusables("mark-davis", true, false, false, false));
    logln(UnicodeJsp.getConfusables("mark-davis", false, true, true, true));
    logln(UnicodeJsp.getConfusables("mark-davis", true, true, true, true));
    logln(UnicodeJsp.getConfusables("mark davis", false, true, true, true));
  }
  
  public void TestBasic() {
    AlternateIterator foo = AlternateIterator.start().add("a", "b", "c").add("d", "e").build();
    int count = 0;
    for (String items : foo) {
      logln(++count + "\t" + items);
    }
  }
  
  public void TestConfusables() {
    String test = "mark-davis";
    Confusables confusables = new Confusables(test).setNormalizationCheck(Normalizer.NFKC);
    confusables.setAllowedCharacters(new UnicodeSet("[\\-[:L:][:M:][:N:]]"));
    confusables.setScriptCheck(Confusables.ScriptCheck.same);
    check(confusables);
    confusables.setAllowedCharacters(null);
    check(confusables);
    //confusables.setScriptCheck(Confusables.ScriptCheck.none);
    //check(confusables);
  }

  private void check(Confusables confusables) {
      if (isVerbose()) {
          logln("Confusables for: " + confusables.getOriginal());
          logln("\tNormalizationCheck:\t" + confusables.getNormalizationCheck());
          logln("\tScriptCheck:\t" + confusables.getScriptCheck());
          logln("\tAllowedCharacters:\t" + confusables.getAllowedCharacters());
          int count = 0;
//          for (String item : confusables) {
//              logln(++count + "\t" + item + "\t" + Utility.hex(item));
//          }
      } else {
          assertNotEquals("Confusable count", 0, confusables.iterator().hasNext());
      }
  }
}
