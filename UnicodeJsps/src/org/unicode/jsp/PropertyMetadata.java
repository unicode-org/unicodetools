package org.unicode.jsp;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R4;

public class PropertyMetadata {

    // #Property ;   Source ; Datatype ;   Category

    public static class PropertyMetaDatum {
        public final String source;
        public final String datatype;
        public final String category;
        
        public PropertyMetaDatum(String[] items) {
            source = items[1];
            datatype = items[2];
            category = items[3];
        }
        @Override
        public String toString() {
            return "{source: " + source + ", datatype: " + datatype + ", category: " + category + "}";
        }
    }

    private static final Map<String, PropertyMetaDatum> propToDatum;

    private static final Set<Row.R4<String, String, String,String>> CategoryDatatypeSourceProperty;

    static {
        MyHandler myHandler = new MyHandler();
        myHandler.process(PropertyMetadata.class, "propertyMetadata.txt");
        CategoryDatatypeSourceProperty = ImmutableSet.copyOf(myHandler.set);
        propToDatum = ImmutableMap.copyOf(myHandler._propToDatum);
    }

    private static class MyHandler extends FileUtilities.SemiFileReader {

        private Set<Row.R4<String, String, String,String>> set = new TreeSet<Row.R4<String, String, String,String>>();
        private Map<String, PropertyMetaDatum> _propToDatum = new TreeMap<String, PropertyMetaDatum>();

        protected boolean isCodePoint() {
            return false;
        }
        
        public boolean handleLine(int start, int end, String[] items) {
            if (items.length != 4) {
                throw new IllegalArgumentException("Must have exactly 4 items: " + Arrays.asList(items));
            }
           PropertyMetaDatum datum = new PropertyMetaDatum(items);
           _propToDatum.put(items[0], datum);
           
            set.add((R4<String, String, String, String>) Row.of(items[3], items[2], items[1], items[0]).freeze());
            set.add((R4<String, String, String, String>) Row.of(items[3], items[2], items[1], items[0]+"β").freeze());
            return true;
        }
        
        protected void handleEnd() {
            super.handleEnd();
        }
    }

    public static Set<Row.R4<String, String, String,String>> getCategoryDatatypeSourceProperty() {
        return CategoryDatatypeSourceProperty;
    }

    public static Map<String, PropertyMetaDatum> getPropertyToData() {
        return propToDatum;
    }
    
    public static Set<String> getPropertiesWithData() {
        return propToDatum.keySet();
    }

    public static PropertyMetaDatum getData(String propName) {
        return propToDatum.get(propName.replace("β",""));
    }
}
