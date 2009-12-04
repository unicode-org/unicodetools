package org.unicode.jsp;

import java.util.Collection;
import java.util.List;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.UnicodeSet;

public class TestUnicodeSet  extends TestFmwk {
  public static void main(String[] args) throws Exception {
    new TestUnicodeSet().run(args);
  }

  public void TestProperties() {
    UnicodeProperty foo;
    XPropertyFactory factory = XPropertyFactory.make();
    checkProperty(factory, "Age");
//////    checkProperty(factory, "Lead_Canonical_Combining_Class");
//////    checkProperty(factory, "Joining_Group");
//    if (true) return;
    
    long start = System.currentTimeMillis();
    for (String prop : (Collection<String>)factory.getAvailableNames()) {
      checkProperty(factory, prop);
      long current = System.currentTimeMillis();
      logln("Time: " + prop + "\t\t" + (current-start) + "ms");
      start = current;
    }
  }
  
  public void TestCCC() {
    XPropertyFactory factory = XPropertyFactory.make();
    checkProperty(factory, "ccc");

    String test = "[:ccc=/3/:]";
    UnicodeSet actual = UnicodeSetUtilities.parseUnicodeSet(test, UnicodeSetUtilities.TableStyle.extras);
    UnicodeSet expected = new UnicodeSet();
    for (int i = 0; i < 256; ++i) {
      String s = String.valueOf(i);
      if (s.contains("3")) {
        expected.addAll(new UnicodeSet("[:ccc=" + s + ":]"));
      }
    }
    myAssertEquals(test, expected, actual);
  }

  private void checkProperty(XPropertyFactory factory, String prop) {
    UnicodeProperty property = factory.getProperty(prop);
    logln("Testing " + prop + "\t\t" + property.getTypeName());
    List<String> values = property.getAvailableValues();
    for (String value : values) {
      UnicodeSet expectedRegex = property.getSet(value);
      UnicodeSet expectedNormal = expectedRegex;
      if (UnicodeProperty.equalNames("age", prop)) {
        expectedNormal = new UnicodeSet(expectedNormal);
        for (String other : values) {
          if (other.compareTo(value) < 0) {
            expectedNormal.addAll(property.getSet(other));
          }
        }
        expectedRegex = expectedNormal;
      }
      List<String> alts = property.getValueAliases(value);
      if (!alts.contains(value)) {
        errln(value + " not in " + alts + " for " + prop);
      }
      for (String alt : alts) {
        String test = "\\p{" + prop + "=" + alt + "}";
        UnicodeSet actual = UnicodeSetUtilities.parseUnicodeSet(test, UnicodeSetUtilities.TableStyle.extras);
        myAssertEquals(test, expectedNormal, actual);
        test = "\\p{" + prop + "=/^\\Q" + alt + "\\E$/}";
        actual = UnicodeSetUtilities.parseUnicodeSet(test, UnicodeSetUtilities.TableStyle.extras);
        myAssertEquals(test, expectedRegex, actual);
      }
    }
  }

  private void myAssertEquals(String test, UnicodeSet expected, UnicodeSet actual) {
    if (!expected.equals(actual)) {
      UnicodeSet inExpected = new UnicodeSet(expected).removeAll(actual);
      UnicodeSet inActual = new UnicodeSet(actual).removeAll(expected);
      errln(test + " - MISSING: " + inExpected + ", EXTRA: " + inActual);
    } else {
      logln("OK\t\t" + test);
    }
  }
}
