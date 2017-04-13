package org.unicode.jsptest;

import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.jsp.PropertyMetadata;
import org.unicode.jsp.UnicodeProperty;
import org.unicode.jsp.XPropertyFactory;

import com.ibm.icu.impl.Row.R4;
import com.ibm.icu.text.Collator;
import com.ibm.icu.util.ULocale;

public class TestBasicProperties extends TestFmwk2 {
	public static void main(String[] args) {
		new TestBasicProperties().run(args);
	}
	static XPropertyFactory factory = XPropertyFactory.make();
	static Collator col = Collator.getInstance(ULocale.ROOT);
	static String sample = "🤩";

	public void TestListing() {
		Set<String> withPropData = new TreeSet<String>();
        for (R4<String, String, String, String> propData : PropertyMetadata.CategoryDatatypeSourceProperty) {
            String propName = propData.get3();
            UnicodeProperty prop = factory.getProperty(propName);
            assertNotNull("PropertyMetadata:" + propName, prop);
            String realName = prop.getName();
			assertEquals("PropertyMetadata: canonicalName" + propName, realName, propName);
            if (prop != null) {
            	withPropData.add(realName.toUpperCase(Locale.ENGLISH));
            }
        }
		for (String name : factory.getAvailableNames()) {
			UnicodeProperty prop = factory.getProperty(name);
    		String value = prop.getValue(sample.codePointAt(0));
			System.out.println(name + "\tvalue('a') = " + value);
			assertTrue("has PropertyMetadata: " + name, withPropData.contains(name.toUpperCase(Locale.ENGLISH)));
		}
	}
	
}
