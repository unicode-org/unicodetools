package org.unicode.jsptest;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.jsp.Builder;
import org.unicode.jsp.ScriptTester;
import org.unicode.jsp.ScriptTester.CompatibilityLevel;
import org.unicode.jsp.ScriptTester.ScriptSpecials;

import com.ibm.icu.dev.test.TestFmwk;

public class TestScriptTester extends TestFmwk {
  public static void main(String[] args) {
    new TestScriptTester().run(args);
  }
  
  public void TestBasic() {
    ScriptTester scriptTester = ScriptTester.start().get();
    
    scriptTester.test("一\u4E07");

    String[] cases = {"abc", "ab가", "一가", "一万", "一丟"};
    for (String testCase : cases) {
      BitSet result = scriptTester.test(testCase);
      assertTrue(testCase + " should be ok: " + result, !result.isEmpty());
    }
    
    String[] bad = {"aᎠ", // Cherokee
            "aα", // simplified and traditional
            "万丟", // simplified and traditional
            };
    for (String testCase : bad) {
      BitSet result = scriptTester.test(testCase);
      assertFalse(testCase + " should be bad: " + result, !result.isEmpty());
    }
  }
  
  public void TestFilter() {
    checkFilter("万");
    checkFilter("丟");
    checkFilter("\u4e01");
  }

  private void checkFilter(String testChar) {
    List<Set<String>> listTrial = Builder.with(new ArrayList<Set<String>>())
    .add(Builder.with(new LinkedHashSet<String>()).addAll("\u30FC", "-", "\u4e00").get())
    .add(Builder.with(new LinkedHashSet<String>()).addAll(testChar).get())
    .get();
    ScriptTester scriptTester = ScriptTester.start().get();
    String before = listTrial.toString();
    scriptTester.filterTable(listTrial);
    String after = listTrial.toString();
    assertEquals("filterTable", before, after);
  }
}
