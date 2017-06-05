package org.unicode.jsptest;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.jsp.CachedProps;
import org.unicode.jsp.PropertyMetadata;
import org.unicode.jsp.PropertyMetadata.PropertyMetaDatum;
import org.unicode.jsp.UnicodeProperty;
import org.unicode.jsp.XPropertyFactory;

import com.ibm.icu.impl.Row.R4;
import com.ibm.icu.text.Collator;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.VersionInfo;

public class TestBasicProperties extends TestFmwk2 {
	public static void main(String[] args) {
		new TestBasicProperties().run(args);
	}
	static XPropertyFactory factory = XPropertyFactory.make();
	static Collator col = Collator.getInstance(ULocale.ROOT);
	static String sample = "क";

	public void TestListing() {
		for (String propName : PropertyMetadata.getPropertiesWithData()) {
			UnicodeProperty prop = factory.getProperty(propName);
			if (prop == null) {
			    prop = factory.getProperty(propName+"β");
			}
			if (!assertNotNull("PropertyMetadata has property: " + propName, prop)) {
				String realName = prop.getName();
				assertEquals("PropertyMetadata: canonicalName" + propName, realName, propName);
			}
		}
		for (String name : factory.getAvailableNames()) {
			UnicodeProperty prop = factory.getProperty(name);
			String value = prop.getValue(sample.codePointAt(0));
			logln(name + "\tvalue('क') = " + value);
			assertNotNull("Property " + name + " has metadata", PropertyMetadata.getData(name));
		}
	}

//	public void TestPropertyMetadata() {
//		Set<String> hasMetadata = new TreeSet<String>();
//		for (R4<String, String, String, String> propData : PropertyMetadata.getCategoryDatatypeSourceProperty()) {
//			String propName = propData.get3();
//			hasMetadata.add(propName);
//		}
//		CachedProps cp = CachedProps.getInstance(VersionInfo.getInstance(10));
//		Set<String> propsMissingMetadata = new LinkedHashSet<String>(cp.getPropertyNames());
//		propsMissingMetadata.removeAll(hasMetadata);
//		assertEquals("PropertyMetadata", Collections.EMPTY_SET, propsMissingMetadata);
//	}
}
