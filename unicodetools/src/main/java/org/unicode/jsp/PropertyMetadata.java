package org.unicode.jsp;

import java.util.Arrays;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R4;

public class PropertyMetadata {
    // #Property ;   Source ; Datatype ;   Category

    private static class MyHandler extends FileUtilities.SemiFileReader {
        private SortedSet<Row.R4<String, String, String,String>> set = new TreeSet<Row.R4<String, String, String,String>>();
        @Override
        protected boolean isCodePoint() {
            return false;
        }
        @Override
        public boolean handleLine(int start, int end, String[] items) {
            if (items.length != 4) {
                throw new IllegalArgumentException("Must have exactly 4 items: " + Arrays.asList(items));
            }
            set.add((R4<String, String, String, String>) Row.of(items[3], items[2], items[1], items[0]).freeze());
            return true;
        }
        @Override
        protected void handleEnd() {
            super.handleEnd();
            set = (Collections.unmodifiableSortedSet(set));
        }
        SortedSet<Row.R4<String, String, String,String>> getSet() {
            if (set == null) {
                throw new IllegalArgumentException("initialization failed");
            }
            return set;
        }
    }
    public static SortedSet<Row.R4<String, String, String,String>> CategoryDatatypeSourceProperty = ((MyHandler) new MyHandler()
    .process(PropertyMetadata.class, "propertyMetadata.txt")).getSet();
}
