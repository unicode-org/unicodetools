package org.unicode.jsptest;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.jsp.Builder;
import org.unicode.jsp.NFM;
import org.unicode.jsp.PropertyMetadata;
import org.unicode.jsp.UnicodeJsp;
import org.unicode.jsp.UnicodeProperty;
import org.unicode.jsp.UnicodeSetUtilities;
import org.unicode.jsp.UnicodeUtilities;
import org.unicode.jsp.XPropertyFactory;

import com.google.common.base.Splitter;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Row.R4;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UProperty.NameChoice;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.UTF16;
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

    public void checkContained(final String setPattern, final String containedPattern) {
        String[] message = {""};
        UnicodeSet primary = UnicodeUtilities.parseSimpleSet(setPattern, message);
        UnicodeSet x = UnicodeUtilities.parseSimpleSet(containedPattern, message);
        if (!primary.containsAll(x)) {
            errln(primary.toPattern(false) + " doesn't contain " + x.toPattern(false));
        } else {
            logln(primary.toPattern(false) + " contains " + x.toPattern(false));
        }
    }
    
    public static final Set<String> SKIP_CJK = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            "kAccountingNumeric",
            "kCompatibilityVariant",
            "kIICore",
            "kIRG_GSource",
            "kIRG_HSource",
            "kIRG_JSource",
            "kIRG_KPSource",
            "kIRG_KSource",
            "kIRG_MSource",
            "kIRG_TSource",
            "kIRG_USource",
            "kIRG_VSource",
            "kOtherNumeric",
            "kPrimaryNumeric",
            "kRSUnicode",
            "Unicode_Radical_Stroke")));

    public static final Set<String> SKIP_FOR_NOW = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            "Unicode_Radical_Stroke")));

    public void TestScope() {
        Set<String> metaprops = new TreeSet<String>();
        for (R4<String, String, String, String> propData : PropertyMetadata.CategoryDatatypeSourceProperty) {
            String category = propData.get0();
            if (category.startsWith("X-")) {
                continue;
            }
            String propName = propData.get3();
            metaprops.add(propName);
            String scope = ScopeOfUse.getScope(propName);
            if (!category.equals(scope)) {
                warnln(propName + "\tCat != scope: " + category + "\t" + scope);
            }
        }
        for (String propName : ScopeOfUse.getProperties()) {
            if (ScopeOfUse.isContributory(propName) || ScopeOfUse.isDeprecated(propName) || SKIP_CJK.contains(propName)) {
                continue;
            }
            if (!metaprops.contains(propName)) {
                if (SKIP_FOR_NOW.contains(propName)) {
                    warnln(propName + "\tCat != scope: " + null + "\t" + ScopeOfUse.getScope(propName));
                } else {
                    warnln(propName + "\tCat != scope: " + null + "\t" + ScopeOfUse.getScope(propName));
                }
            }
        }
    }

    public void TestScopeForPropertyAliases() {
        for (String propName : PropertyAliases.names) {
            String scope = ScopeOfUse.getScope(propName);
            if (scope == null) {
                msg(propName + " in PropertyAliases, but not in http://unicode.org/reports/tr44/proposed.html#Property_Index_Table", 
                        SKIP_CJK.contains(propName) ? LOG
                                : SKIP_FOR_NOW.contains(propName) ? WARN
                                        : ERR, 
                                        true, true);
            }
        }
    }

    public enum Source {METADATA, ICU, FACTORY, PROPERTY_ALIASES}
    private static final Comparator<String> LC = new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            return o1.compareToIgnoreCase(o2);
        }
    };

    public void TestPropertySupport() {
        Relation<String,Source> source = Relation.of(new TreeMap<String,Set<Source>>(LC), TreeSet.class);
        for (R4<String, String, String, String> propData : PropertyMetadata.CategoryDatatypeSourceProperty) {
            String propName = propData.get3();
            put(source, propName, Source.METADATA);
        }
        int[][] ranges = {
                {UProperty.BINARY_START, UProperty.BINARY_LIMIT},
                {UProperty.INT_START, UProperty.INT_LIMIT},
                {UProperty.DOUBLE_START, UProperty.DOUBLE_LIMIT},
                {UProperty.STRING_START, UProperty.STRING_LIMIT},
                {UProperty.OTHER_PROPERTY_START, UProperty.OTHER_PROPERTY_LIMIT},
        };
        for (int[] range : ranges) {
            for (int property = range[0]; property < range[1]; ++property) {
                String propName = UCharacter.getPropertyName(property, NameChoice.LONG);
                put(source, propName, Source.ICU);
            }
        }
        for (String propName : (Collection<String>) factory.getAvailableNames()) {
            put(source, propName, Source.FACTORY);
        }
        for (String propName : PropertyAliases.names) {
            put(source, propName, Source.PROPERTY_ALIASES);
        }
        for (Entry<String, Set<Source>> sources : source.keyValuesSet()) {
            String propName = sources.getKey();
            if (SKIP_CJK.contains(propName)) {
                continue;
            }
            if (ScopeOfUse.isDeprecated(propName) || ScopeOfUse.isContributory(propName)) {
                continue;
            }
            Set<Source> where = sources.getValue();
            if (!where.contains(Source.FACTORY)) {
                warnln("Missing entirely: " + propName + "\t" + where);
            } else if (!where.contains(Source.METADATA)) {
                warnln("Missing metadata: " + propName + "\t" + where);
            }
        }
    }

    private Source put(Relation<String, Source> source, String propName, Source value) {
        return source.put(propName, value);
    }

    private String getGoodName(String propName) {
        // TODO Auto-generated method stub
        return null;
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
    //  public void TestDefaultEncodingValue() {
    //    UnicodeProperty prop = factory.getProperty("enc_ISO-8859-2");
    //    assertTrue("Default for Å, enc_ISO-8859-2", prop.isDefault('Å'));
    //  }

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
        System.out.println("Testing " + prop + "\t\t" + property.getTypeName());
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
                if (!TestUnicodeSet.assertEquals(this, test, expectedNormal, actual)) {
                    return;
                }
                if ("}".equals(alt)) {
                    logKnownIssue("x", "Can't parse } even with \\Q");
                    continue;
                }
                test = "\\p{" + prop + "=/^\\Q" + alt + "\\E$/}";
                actual = UnicodeSetUtilities.parseUnicodeSet(test);
                if (!TestUnicodeSet.assertEquals(this, test, expectedRegex, actual)) {
                    return;
                }
            }
        }
    }

    public void TestAllProperties() {
        UnicodeProperty foo;
        XPropertyFactory factory = XPropertyFactory.make();
        checkProperty(factory, "NFKC_Casefold");
        checkProperty(factory, "Age");

        //////    checkProperty(factory, "Lead_Canonical_Combining_Class");
        //////    checkProperty(factory, "Joining_Group");
        //    if (true) return;

        long start = System.currentTimeMillis();
        for (String prop : (Collection<String>)factory.getAvailableNames()) {
            try {
                checkProperty(factory, prop);
            } catch (Throwable e) {
                errln (prop + "\t" + maxLen(150, e.getMessage()));
                break;
            }
            long current = System.currentTimeMillis();
            logln("Time: " + prop + "\t\t" + (current-start) + "ms");
            start = current;
        }
    }

    private String maxLen(int max, String message) {
        return message.length() <= max ? message : message.substring(0,max)+"…";
    }

    static final class PropertyAliases {
        static Set<String> names = new TreeSet<String>();
        static {
            Splitter SEMI = Splitter.on(';').trimResults();
            for (String line : FileUtilities.in(PropertyAliases.class,"PropertyAliases.txt")) {
                // bc                       ; Bidi_Class
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                List<String> parts = SEMI.splitToList(line);
                names.add(parts.get(1));
            }
        }
    }

    static final class ScopeOfUse {
        public ScopeOfUse(List<String> parts) {
            scope = parts.get(1);
            contributory = parts.get(1).equals("Contributory Properties") 
                    || parts.get(0).equals("Composition_Exclusion")
                    || parts.get(0).equals("Decomposition_Mapping");
            deprecated = !parts.get(2).isEmpty();
        }
        public final String scope;
        public final boolean deprecated;
        public final boolean contributory;
        private static final Map<String,ScopeOfUse> data;

        public static ScopeOfUse get(String propName) {
            return data.get(propName);
        }
        public static boolean isDeprecated(String prop) {
            ScopeOfUse item = get(prop);
            return item == null ? false : item.contributory;
        }
        public static boolean isContributory(String prop) {
            ScopeOfUse item = get(prop);
            return item == null ? false : item.deprecated;
        }
        public static String getScope(String prop) {
            ScopeOfUse item = get(prop);
            return item == null ? null : item.scope;
        }
        public static Set<String> getProperties() {
            return data.keySet();
        }

        static {
            Splitter SEMI = Splitter.on(';').trimResults();
            TreeMap<String, ScopeOfUse> _data = new TreeMap<String,ScopeOfUse>();
            for (String line : FileUtilities.in(PropertyAliases.class,"ScopeOfUse.txt")) {
                // bc                       ; Bidi_Class
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                List<String> parts = SEMI.splitToList(line);
                _data.put(parts.get(0),new ScopeOfUse(parts));
            }
            data = Collections.unmodifiableMap(_data);
        }
    }

}
