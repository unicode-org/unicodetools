package org.unicode.jsptest;

import java.util.Map.Entry;

import com.ibm.icu.text.Collator;
import com.ibm.icu.util.ULocale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.unicode.jsp.PropertyMetadata;
import org.unicode.jsp.PropertyMetadata.PropertyMetaDatum;
import org.unicode.props.UnicodeProperty;
import org.unicode.jsp.XPropertyFactory;

public class TestBasicProperties extends TestFmwk2 {

    static XPropertyFactory factory = XPropertyFactory.make();
    static Collator col = Collator.getInstance(ULocale.ROOT);
    static String sample = "क";

    @EnabledIf(value = "org.unicode.unittest.TestFmwkMinusMinus#getRunBroken", disabledReason = "Skip unless UNICODETOOLS_RUN_BROKEN_TEST=true")
    @Test
    public void TestListing() {
        for (Entry<String, PropertyMetaDatum> propInfo : PropertyMetadata.getPropertyToData().entrySet()) {
            if (isVerbose()) {
                logln(propInfo.toString());
            }
            String propName = propInfo.getKey();
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

    //        public void TestPropertyMetadata() {
    //                Set<String> hasMetadata = new TreeSet<String>();
    //                for (R4<String, String, String, String> propData : PropertyMetadata.getCategoryDatatypeSourceProperty()) {
    //                        String propName = propData.get3();
    //                        hasMetadata.add(propName);
    //                }
    //                CachedProps cp = CachedProps.getInstance(VersionInfo.getInstance(10));
    //                Set<String> propsMissingMetadata = new LinkedHashSet<String>(cp.getPropertyNames());
    //                propsMissingMetadata.removeAll(hasMetadata);
    //                assertEquals("PropertyMetadata", Collections.EMPTY_SET, propsMissingMetadata);
    //        }
}
