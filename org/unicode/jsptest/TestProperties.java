package org.unicode.jsptest;

import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import org.unicode.jsp.Builder;
import org.unicode.jsp.UnicodeProperty;
import org.unicode.jsp.XPropertyFactory;

import sun.text.normalizer.UTF16;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
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
  
  public void TestDefaultEncodingValue() {
    UnicodeProperty prop = factory.getProperty("enc_ISO-8859-2");
    assertTrue("Default for Å, enc_ISO-8859-2", prop.isDefault('Å'));
  }
  
  public void TestInstantiateProps() {
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
        isDefault = prop.isDefault(cp);
      } catch (Exception e) {
        errln(propName + "\t" + Arrays.asList(e.getStackTrace()).toString());
        continue;
      }
      if (isDefault) continue;
      String propValue = prop.getValue(cp);
      logln(propName + "\t" + propValue);
    }
  }
}
