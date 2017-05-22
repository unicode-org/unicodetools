package org.unicode.idna;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.idna.Uts46.Errors;
import org.unicode.text.utility.Settings;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.text.Transliterator;

public class LoadIdnaTest {
    public static final Splitter semi = Splitter.on(';').trimResults();
    public static final Splitter SPACE_SPLITTER = Splitter.on(' ').trimResults().omitEmptyStrings();
    
    public static final Transliterator fromHex = Transliterator.getInstance("hex-any/java");

    public enum Type {T, N, B};
    public enum Idna2008Status {V8, NV8, XV8};

    public final static class TestLine {
        public final Type type;
        public final String source;
        public final String toUnicode;
        public final Set<Errors> toUnicodeErrors;
        public final String toAscii;
        public final Set<Errors> toAsciiErrors;
        public final Idna2008Status idn2008Status;
        
        /**
         * Create a test line from a string. Comments are removed and resulting empty lines are ignored.
         * but it will do \\u and \\x expansion.
         * @param test
         */
        public static TestLine from(String line) {
            int hash = line.indexOf('#');
            line = hash < 0 ? line : line.substring(0, hash).trim(); // strip final comment
            if (!line.isEmpty()) {
                return new TestLine(line);
            }
            return null;
        }

        private TestLine(String test) {
            if (test.contains("\\u")) {
                int debug = 0;
            }
            List<String> parts = semi.splitToList(test);
            type = Type.valueOf(parts.get(0));
            source = fromHex.transform(parts.get(1));
            
            String toUnicodeRaw = getWithFallback(fromHex.transform(parts.get(2)), source);
            toUnicodeErrors = parseEnumSet(toUnicodeRaw);
            toUnicode = toUnicodeErrors.isEmpty() ? toUnicodeRaw : null;
            
            String toAsciiRaw = getWithFallback(fromHex.transform(parts.get(3)), toUnicode);
            toAsciiErrors = parseEnumSet(toAsciiRaw);
            toAscii = toAsciiErrors.isEmpty() ? toAsciiRaw : null;

            idn2008Status = parts.size() < 5 ? Idna2008Status.V8 : Idna2008Status.valueOf(parts.get(4));
        }

        private Set<Errors> parseEnumSet(String toUnicodeRaw) {
            if (toUnicodeRaw.startsWith("[") && toUnicodeRaw.endsWith("]")) {
                Set<Errors> toUnicodeErrorsRaw = EnumSet.noneOf(Errors.class);
                for (String item : SPACE_SPLITTER.split(toUnicodeRaw.substring(1, toUnicodeRaw.length()-1))) {
                    toUnicodeErrorsRaw.add(Errors.valueOf(item));
                }
                return ImmutableSet.copyOf(toUnicodeErrorsRaw);
            }
            return Collections.emptySet();
        }
        
        private String getWithFallback(String string, String fallback) {
            return string.isEmpty() ? fallback : string;
        }
        @Override
        public String toString() {
            return type + ";\t" + source 
                    + ";\t" + toUnicode  + ";\t" + toUnicodeErrors
                    + ";\t" + toAscii  + ";\t" + toAsciiErrors
                    + (idn2008Status == Idna2008Status.V8 ? "" : ";\t" + idn2008Status);
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
            TestLine testLine = TestLine.from(line);
            if (testLine != null) {
                result.add(testLine);
            }
        }
        return ImmutableSet.copyOf(result);
    }

    public static void main(String[] args) {
        Set<Idna2008Status> seen = EnumSet.noneOf(Idna2008Status.class);
        for (TestLine testLine : load(Settings.UNICODETOOLS_DIRECTORY + "data/idna/9.0.0")) {
            System.out.println(testLine);
            if (!seen.contains(testLine.idn2008Status)) {
                System.out.println("First " + testLine.idn2008Status);
                seen.add(testLine.idn2008Status);
            }
        }
    }
}
