package org.unicode.idna;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.text.utility.Settings;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.text.Transliterator;

public class LoadIdnaTest {
    public static final Splitter semi = Splitter.on(';').trimResults();
    public static final Transliterator fromHex = Transliterator.getInstance("hex-any/java");

    public enum Type {T, N, B};

    final static class TestLine {
        public final Type type;
        public final String source;
        public final String toUnicode;
        public final String toAscii;
        public TestLine(String test) {
            if (test.contains("\\u")) {
                int debug = 0;
            }
            List<String> parts = semi.splitToList(test);
            type = Type.valueOf(parts.get(0));
            source = fromHex.transform(parts.get(1));
            toUnicode = getWithFallback(fromHex.transform(parts.get(2)), source);
            toAscii = getWithFallback(fromHex.transform(parts.get(3)), toUnicode);
        }
        private String getWithFallback(String string, String fallback) {
            return string.isEmpty() ? fallback : string;
        }
        @Override
        public String toString() {
            return type + ";\t" + source + ";\t" + toUnicode + ";\t" + toAscii;
        }
        @Override
        public boolean equals(Object obj) {
            if (obj.getClass() != TestLine.class) {
                return false;
            }
            TestLine that = (TestLine)obj;
            return type == that.type 
                    && source == that.source; // rest should be consistent
        }
        @Override
        public int hashCode() {
            return Objects.hash(type, source);
        }
    }
    
    static public Set<TestLine> load(String directory) {
        Set<TestLine> result = new LinkedHashSet<>();

        for (String line : FileUtilities.in(directory, "IdnaTest.txt")) {
            int hash = line.indexOf('#');
            line = hash < 0 ? line : line.substring(0, hash).trim(); // strip final comment
            if (line.isEmpty()) {
                continue;
            }
            result.add(new TestLine(line));
        }
        return ImmutableSet.copyOf(result);
    }
    
    public static void main(String[] args) {
        for (TestLine testLine : load(Settings.UNICODETOOLS_DIRECTORY + "data/idna/9.0.0")) {
            System.out.println(testLine);
        }
    }
}
