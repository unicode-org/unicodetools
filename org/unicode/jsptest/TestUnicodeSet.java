package org.unicode.jsptest;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeSet;

import org.unicode.jsp.Builder;
import org.unicode.jsp.CharEncoder;
import org.unicode.jsp.UnicodeProperty;
import org.unicode.jsp.UnicodeSetUtilities;
import org.unicode.jsp.XPropertyFactory;
import org.unicode.jsp.UnicodeSetUtilities.TableStyle;

import com.ibm.icu.dev.test.AbstractTestLog;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.UnicodeSet;

public class TestUnicodeSet  extends TestFmwk {
  public static void main(String[] args) throws Exception {
    new TestUnicodeSet().run(args);
  }

  public void TestEncodingProp() {

    XPropertyFactory factory = XPropertyFactory.make();
    UnicodeProperty prop = factory.getProperty("enc_Latin1");
    UnicodeProperty prop2 = factory.getProperty("enc_Latin2");
    UnicodeMap<String> map = prop.getUnicodeMap();
    UnicodeMap<String> map2 = prop2.getUnicodeMap();
    for (String value : Builder.with(new TreeSet<String>()).addAll(map.values()).addAll(map2.values()).get()) {
      System.out.println(value + "\t" + map.getSet(value) + "\t" + map2.getSet(value));
    }
    UnicodeSet set = UnicodeSetUtilities.parseUnicodeSet("[:enc_Latin1=/61/:]", TableStyle.extras);
    assertNotEquals("Latin1", 0, set.size());
  }

  public void TestPerMill() {
    SortedMap<String, Charset> charsets = Charset.availableCharsets();
    byte[] dest = new byte[50];
    UnicodeSet values = new UnicodeSet();

    for (String s : charsets.keySet()) {
      Charset charset = charsets.get(s);
      CharEncoder encoder;
      try {
        encoder = new CharEncoder(charset, false, false);
      } catch (Exception e) {
        e.printStackTrace();
        continue;
      }

      // first check that we are an ASCII-based encoding, and skip if not
      int len = encoder.getValue(0x61, dest, 0);
      if (len != 1 || dest[0] != 0x61) {
        continue;
      }

      values.clear();
      byte checkByte = (byte) 0x89;
      for (int cp = 0; cp <= 0x10FFFF; ++cp) {
        len = encoder.getValue(cp, dest, 0);
        if (len > 0) {
          for (int j = 0; j < len; ++j) {
            if (dest[j] == checkByte) {
              values.add(cp);
              break;
            }
          }
        }
      }
      values.remove(0x2030);
      if (values.size() != 0) {
        System.out.println(s + "\tvalues:\t" + values + "\taliases:\t" + charset.aliases());
      }
    }
  }

  public void TestScriptSpecials() {
    UnicodeSet set = UnicodeSetUtilities.parseUnicodeSet("[:scs=Hant:]", TableStyle.extras);
    assertNotEquals("Hant", 0, set.size());
    UnicodeSet set2 = UnicodeSetUtilities.parseUnicodeSet("[:scs=Arab,Syrc:]", TableStyle.extras);
    assertNotEquals("Arab Syrc", 0, set2.size());

  }

  public void TestGC() {
    Map<String,R2<String,UnicodeSet>> SPECIAL_GC = new LinkedHashMap<String,R2<String,UnicodeSet>>();

    String[][] extras = {
            {"Other", "C", "[[:Cc:][:Cf:][:Cn:][:Co:][:Cs:]]"},
            {"Letter", "L", "[[:Ll:][:Lm:][:Lo:][:Lt:][:Lu:]]"},
            {"Cased_Letter", "LC", "[[:Ll:][:Lt:][:Lu:]]"},
            {"Mark", "M", "[[:Mc:][:Me:][:Mn:]]"},
            {"Number", "N", "[[:Nd:][:Nl:][:No:]]"},
            {"Punctuation", "P", "[[:Pc:][:Pd:][:Pe:][:Pf:][:Pi:][:Po:][:Ps:]]"},
            {"Symbol", "S", "[[:Sc:][:Sk:][:Sm:][:So:]]"},
            {"Separator", "Z", "[[:Zl:][:Zp:][:Zs:]]"},
    };

    String[] gcs = {"General_Category=", "", "gc="};
    /*
gc ; C         ; Other                            # Cc | Cf | Cn | Co | Cs
gc ; Cc        ; Control                          ; cntrl
gc ; L         ; Letter                           # Ll | Lm | Lo | Lt | Lu
gc ; LC        ; Cased_Letter                     # Ll | Lt | Lu
gc ; M         ; Mark                             # Mc | Me | Mn
gc ; N         ; Number                           # Nd | Nl | No
gc ; Nd        ; Decimal_Number                   ; digit
gc ; P         ; Punctuation                      ; punct                            # Pc | Pd | Pe | Pf | Pi | Po | Ps
gc ; S         ; Symbol                           # Sc | Sk | Sm | So
gc ; Z         ; Separator                        # Zl | Zp | Zs
     */
    for (String[] extra : extras) {
      UnicodeSet expected = new UnicodeSet(extra[2]).freeze();
      for (String test : extra) {
        if (test.startsWith("[")) continue;
        for (String gc : gcs) {
          UnicodeSet set = UnicodeSetUtilities.parseUnicodeSet("[:" + gc + test + ":]", TableStyle.extras);
          assertEquals("Multiprop:\t" + gc + test, expected, set);
        }
      }
    }
    assertEquals("Coverage:\t", new UnicodeSet("[:any:]"), UnicodeSetUtilities.parseUnicodeSet("[[:C:][:L:][:M:][:N:][:P:][:S:][:Z:]]", TableStyle.extras));
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
    assertEquals(this, test, expected, actual);
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
        assertEquals(this, test, expectedNormal, actual);
        test = "\\p{" + prop + "=/^\\Q" + alt + "\\E$/}";
        actual = UnicodeSetUtilities.parseUnicodeSet(test, UnicodeSetUtilities.TableStyle.extras);
        assertEquals(this, test, expectedRegex, actual);
      }
    }
  }

  public static void assertEquals(AbstractTestLog testFmwk, String test, UnicodeSet expected, UnicodeSet actual) {
    if (!expected.equals(actual)) {
      UnicodeSet inExpected = new UnicodeSet(expected).removeAll(actual);
      UnicodeSet inActual = new UnicodeSet(actual).removeAll(expected);
      testFmwk.errln(test + " - MISSING: " + inExpected + ", EXTRA: " + inActual);
    } else {
      testFmwk.logln("OK\t\t" + test);
    }
  }

  public static void assertContains(AbstractTestLog testFmwk, String test, UnicodeSet expectedSubset, UnicodeSet actual) {
    if (!actual.containsAll(expectedSubset)) {
      UnicodeSet inExpected = new UnicodeSet(expectedSubset).removeAll(actual);
      testFmwk.errln(test + " - MISSING: " + inExpected);
    } else {
      testFmwk.logln("OK\t\t" + test);
    }
  }
}
