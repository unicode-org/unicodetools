package org.unicode.idna;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.idna.Uts46.Errors;
import org.unicode.text.utility.Settings;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.text.Transliterator;

public class LoadIdnaTest {
    public static final Splitter semi = Splitter.on(';').trimResults();
    public static final Splitter SPACE_SPLITTER = Splitter.on(Pattern.compile(",? ")).trimResults().omitEmptyStrings();
    
    public static final Transliterator fromHex = Transliterator.getInstance("hex-any/java");

    public enum Type {T, N, B};
    public enum Idna2008Status {V8, NV8, XV8};

    public final static class TestLine {
        public final String source;
        public final String toUnicode;
        public final Set<Errors> toUnicodeErrors;
        public final String toAsciiN;
        public final Set<Errors> toAsciiNErrors;
        public final String toAsciiT;
        public final Set<Errors> toAsciiTErrors;

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
            /* 
OLD
#  Column 1: type -       T for transitional, N for nontransitional, B for both
#  Column 2: source -     The source string to be tested
#  Column 3: toUnicode -  The result of applying toUnicode to the source, using nontransitional. 
#                         A blank value means the same as the source value; a value in [...] is a set of error codes.
#  Column 4: toASCII -    The result of applying toASCII to the source, using the specified type: T, N, or B.
#                         A blank value means the same as the toUnicode value; a value in [...] is a set of error codes.
#  Column 5: idna2008 -   NV8 is only present if the status is valid but the character is excluded by IDNA2008
#                         from all domain names for all versions of Unicode.
#                         XV8 is present when the character is excluded by IDNA2008 for the current version of Unicode. 
#                         These are informative values only.

V2
# Column 1: source -          The source string to be tested
# Column 2: toUnicode -       The result of applying toUnicode to the source,
#                             with Transitional_Processing=false.
#                             A blank value means the same as the source value.
# Column 3: toUnicodeStatus - A set of status codes, each corresponding to a particular test.
#                             A blank value means [] (no errors).
# Column 4: toAsciiN -        The result of applying toASCII to the source,
#                             with Transitional_Processing=false.
#                             A blank value means the same as the toUnicode value.
# Column 5: toAsciiNStatus -  A set of status codes, each corresponding to a particular test.
#                             A blank value means the same as the toUnicodeStatus value.
#                             An explicit [] means no errors.
# Column 6: toAsciiT -        The result of applying toASCII to the source,
#                             with Transitional_Processing=true.
#                             A blank value means the same as the toAsciiN value.
# Column 7: toAsciiTStatus -  A set of status codes, each corresponding to a particular test.
#                             A blank value means the same as the toAsciiNStatus value.
#                             An explicit [] means no errors.

             */
            List<String> parts = semi.splitToList(test);
            int col = 0;
            
            // TODO (maybe) enable for old format also
            
            // type = Type.valueOf(parts.get(0));
            try {
		source = fromHex.transform(parts.get(col++));
		
		toUnicode = getWithFallback(fromHex.transform(parts.get(col++)), source);
		toUnicodeErrors = parseEnumSet(parts.get(col++));
		            
		toAsciiN = getWithFallback(fromHex.transform(parts.get(col++)), toUnicode);
		toAsciiNErrors = parseEnumSet(parts.get(col++));
		
		toAsciiT = getWithFallback(fromHex.transform(parts.get(col++)), toUnicode);
		toAsciiTErrors = parseEnumSet(parts.get(col++));
	    } catch (Exception e) {
		throw e; // pause for debugging
	    }

        }

        private Set<Errors> parseEnumSet(String toUnicodeRaw) {
            if (toUnicodeRaw.startsWith("[") && toUnicodeRaw.endsWith("]")) {
                Set<Errors> toUnicodeErrorsRaw = EnumSet.noneOf(Errors.class);
                for (String item : SPACE_SPLITTER.split(toUnicodeRaw.substring(1, toUnicodeRaw.length()-1))) {
                    try {
			toUnicodeErrorsRaw.add(Errors.valueOf(item));
		    } catch (Exception e) {
			throw e; // pause for debugging
		    }
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
            return source 
                    + ";\t" + toUnicode  + ";\t" + toUnicodeErrors
                    + ";\t" + toAsciiN  + ";\t" + toAsciiNErrors
                    + ";\t" + toAsciiT  + ";\t" + toAsciiTErrors
                    ;
        }
        @Override
        public boolean equals(Object obj) {
            if (obj.getClass() != TestLine.class) {
                return false;
            }
            TestLine that = (TestLine)obj;
            return source == that.source; // rest should be consistent
        }
        @Override
        public int hashCode() {
            return Objects.hash(source);
        }
    }
    
    static public Set<TestLine> load(String directory) {
        Set<TestLine> result = new LinkedHashSet<>();

        for (String line : FileUtilities.in(directory, "IdnaTestV2.txt")) {
            TestLine testLine = TestLine.from(line);
            if (testLine != null) {
                result.add(testLine);
            }
        }
        return ImmutableSet.copyOf(result);
    }

    public static void main(String[] args) {
        Set<Idna2008Status> seen = EnumSet.noneOf(Idna2008Status.class);
        for (TestLine testLine : load(Settings.UnicodeTools.UNICODETOOLS_DIR + "data/idna/13.0.0")) {
            System.out.println(testLine);
        }
    }
}
