package org.unicode.jsptest;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.jsp.Builder;
import org.unicode.jsp.NFM;
import org.unicode.jsp.PropertyMetadata;
import org.unicode.jsp.UnicodeJsp;
import org.unicode.jsp.UnicodeProperty;
import org.unicode.jsp.UnicodeSetUtilities;
import org.unicode.jsp.XPropertyFactory;

import sun.text.normalizer.UTF16;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.impl.Row.R4;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class TestProperties extends TestFmwk {
  static XPropertyFactory factory = XPropertyFactory.make();
  static Collator col = Collator.getInstance(ULocale.ROOT);
  static {
    ((RuleBasedCollator) col).setNumericCollation(true);
  }

  public static void main(String[] args) {
    new TestProperties().run(args);
  }

  public void TestNFM() {
      UnicodeMap<String> map = NFM.nfm;
      assertEquals("A", "a", map.transform("A"));
      assertEquals("0640", "", map.transform("\u0640"));
      UnicodeSet actual = UnicodeSetUtilities.parseUnicodeSet("[:isNFM:]");
      assertTrue("isNFM", actual.contains("a"));
      assertTrue("isNFM", !actual.contains("A"));
      assertTrue("isNFM", !actual.contains("\u0640"));
      UnicodeSet actual2 = UnicodeSetUtilities.parseUnicodeSet("[:toNFM=a:]");
      assertTrue("toNFM=a", actual2.contains("A"));
      assertTrue("toNFM=a", !actual2.contains("B"));
  }
  public void TestDefaultEncodingValue() {
    UnicodeProperty prop = factory.getProperty("enc_ISO-8859-2");
    assertTrue("Default for Å, enc_ISO-8859-2", prop.isDefault('Å'));
  }

  public void TestInstantiateProps() {
    Set<R4<String, String, String, String>> propInfo = new TreeSet<R4<String, String, String, String>>();
    //Relation<Integer,String> typeToProp = new Relation(new TreeMap(), TreeSet.class, col);
    List<String> availableNames = (List<String>)factory.getAvailableNames();
    TreeSet<String> sortedProps = Builder
    .with(new TreeSet<String>(col))
    .addAll(availableNames)
    .remove("Name")
    .get();

    int cp = 'a';
    logln("Properties for " + UTF16.valueOf(cp));
    for (String propName : sortedProps) {
      UnicodeProperty prop;
      boolean isDefault;
      try {
        prop = factory.getProperty(propName);
        //int type = prop.getType();
        //typeToProp.put(type, propName);
        isDefault = prop.isDefault(cp);
      } catch (Exception e) {
        errln(propName + "\t" + Arrays.asList(e.getStackTrace()).toString());
        continue;
      }
      if (isDefault) continue;
      String propValue = prop.getValue(cp);
      logln(propName + "\t" + propValue);
    }
//    for (Integer type : typeToProp.keySet()) {
//      for (String name : typeToProp.getAll(type)) {
//        logln(UnicodeProperty.getTypeName(type) + "\t" + name);
//      }
//    }

    Set<String> notCovered = new HashSet<String>(availableNames);
    for (R4<String, String, String, String> propData : PropertyMetadata.CategoryDatatypeSourceProperty) {
      logln(propData.toString());
      notCovered.remove(propData.get3());
    }
    if (notCovered.size() == 0) {
      errln("Properties not covered:\t" + notCovered);
    }
  }
  
  public void TestPropsTable() throws IOException {
    StringWriter out = new StringWriter();
    UnicodeJsp.showPropsTable(out, "Block", "properties.jsp");
    assertTrue("props table", out.toString().contains("Cherokee"));
    logln(out.toString());
    //System.out.println(out);
  }
  
  public void TestCCC() {
      XPropertyFactory factory = XPropertyFactory.make();
      checkProperty(factory, "ccc");

      String test = "[:ccc=/3/:]";
      UnicodeSet actual = UnicodeSetUtilities.parseUnicodeSet(test);
      UnicodeSet expected = new UnicodeSet();
      for (int i = 0; i < 256; ++i) {
          String s = String.valueOf(i);
          if (s.contains("3")) {
              expected.addAll(new UnicodeSet("[:ccc=" + s + ":]"));
          }
      }
      TestUnicodeSet.assertEquals(this, test, expected, actual);
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
              UnicodeSet actual = UnicodeSetUtilities.parseUnicodeSet(test);
              TestUnicodeSet.assertEquals(this, test, expectedNormal, actual);
              test = "\\p{" + prop + "=/^\\Q" + alt + "\\E$/}";
              actual = UnicodeSetUtilities.parseUnicodeSet(test);
              TestUnicodeSet.assertEquals(this, test, expectedRegex, actual);
          }
      }
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



}
