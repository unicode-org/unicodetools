package org.unicode.jsptest;

import java.util.BitSet;

import org.unicode.jsp.ScriptTester;
import org.unicode.jsp.ScriptTester.CompatibilityLevel;
import org.unicode.jsp.ScriptTester.ScriptSpecials;

import com.ibm.icu.dev.test.TestFmwk;

public class TestScriptTester extends TestFmwk {
  public static void main(String[] args) {
    new TestScriptTester().run(args);
  }
  
  public void TestBasic() {
    ScriptTester tester = ScriptTester.start(CompatibilityLevel.Highly_Restrictive, ScriptSpecials.on).get();
    
    tester.test("一\u4E07");

    String[] cases = {"abc", "ab가", "一가", "一\u4E07", "一\u4E1F"};
    for (String testCase : cases) {
      BitSet result = tester.test(testCase);
      assertTrue(testCase + " should be ok: " + result, !result.isEmpty());
    }
    
    String[] bad = {"aᎠ", // Cherokee
            "aα", // simplified and traditional
            "\u4E07\u4E1F", // simplified and traditional
            };
    for (String testCase : bad) {
      BitSet result = tester.test(testCase);
      assertFalse(testCase + " should be bad: " + result, !result.isEmpty());
    }
  }
}
