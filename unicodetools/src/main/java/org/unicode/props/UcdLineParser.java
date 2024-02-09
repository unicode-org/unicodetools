package org.unicode.props;

import com.ibm.icu.text.UnicodeSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.unicode.cldr.util.RegexUtilities;
import org.unicode.idna.Regexes;
import org.unicode.text.utility.Utility;

/**
 * Low-level parser for lines of Unicode data files with TAB or semicolon separators. Skips comments
 * and empty lines. Handles @missing lines, if this option is turned on. Parses a range of code
 * points from the first field, unless this option is turned off.
 */
public final class UcdLineParser implements Iterable<UcdLineParser.UcdLine> {
    private static final Pattern TAB = Pattern.compile("[ ]*\t[ ]*");
    private static final Pattern SEMICOLON = Pattern.compile("\\s*;\\s*");

    private static final class UcdFileStats {
        private int lineCount;
        private boolean containsEOF;
    }

    public static class IntRange {
        int start;
        int end;
        String string;

        public IntRange set(String source) {
            if (source.startsWith("U+")) {
                source = source.substring(2);
            }
            final int range = source.indexOf("..");
            if (range >= 0) {
                start = Integer.parseInt(source.substring(0, range), 16);
                end = Integer.parseInt(source.substring(range + 2), 16);
                string = null;
            } else if (source.contains(" ")) {
                string = Utility.fromHex(source);
                start = end = -1;
            } else if (source.isEmpty()) {
                string = "";
                start = end = -1;
            } else {
                start = end = Integer.parseInt(source, 16);
                string = null;
            }
            return this;
        }
    }

    public static final class UcdLine implements Iterator<UcdLine> {
        private enum State {
            LOOK,
            HAVE_NEXT,
            NO_NEXT
        }

        private static final Pattern MISSING =
                Pattern.compile("\\s*#\\s*@(missing|empty):?\\s*(.+)\\s*");

        // TODO: Should be renamed to Type.
        public enum Contents {
            DATA,
            MISSING,
            EMPTY
        }

        private final Matcher splitter;
        private final boolean withRange;
        private final boolean withMissing;
        private final Iterator<String> rawLines;
        /** Code points covered by @missing lines for less than all of Unicode. */
        private final UnicodeSet missingSet = new UnicodeSet();

        private State state = State.LOOK;
        private String line; // original line for logging and error messages
        private String line2; // modified line for parsing
        private final UcdFileStats stats;
        private Contents contents = Contents.DATA;
        private final ArrayList<String> partsList = new ArrayList<>();
        private String[] parts = null;
        private final IntRange intRange = new IntRange();

        UcdLine(
                Pattern splitPattern,
                boolean withRange,
                boolean withMissing,
                Iterator<String> rawLines,
                UcdFileStats stats) {
            splitter = splitPattern.matcher("");
            this.withRange = withRange;
            this.withMissing = withMissing;
            this.rawLines = rawLines;
            this.stats = stats;
        }

        @Override
        public boolean hasNext() {
            if (state == State.NO_NEXT) {
                return false;
            }
            if (state == State.LOOK) {
                contents = Contents.DATA;
                do {
                    if (!rawLines.hasNext()) {
                        state = State.NO_NEXT;
                        return false;
                    }
                    line = line2 = rawLines.next();
                    if (line.startsWith("<<<<<<<")
                            || line.startsWith("=======")
                            || line.startsWith(">>>>>>>")) {
                        line2 = "";
                    }
                    ++stats.lineCount;
                    final int hashPos = line2.indexOf('#');
                    if (hashPos >= 0) {
                        if (line2.contains("# EOF")) {
                            stats.containsEOF = true;
                        } else {
                            if (line2.contains("@missing:")) { // quick test
                                // # @missing: 0000..10FFFF; cjkIRG_KPSource; <none>
                                if (!withMissing) {
                                    throw new IllegalArgumentException(
                                            "Unhandled @missing line: " + line);
                                }
                                final Matcher missingMatcher = MISSING.matcher(line2);
                                if (!missingMatcher.matches()) {
                                    System.err.println(RegexUtilities.showMismatch(MISSING, line2));
                                    throw new UnicodePropertyException(
                                            "Bad @missing statement: " + line);
                                }
                                contents =
                                        missingMatcher.group(1).equals("empty")
                                                ? Contents.EMPTY
                                                : Contents.MISSING;
                                line2 = missingMatcher.group(2);
                            }
                        }
                        if (contents == Contents.DATA) {
                            line2 = line2.substring(0, hashPos);
                        }
                    }
                    line2 = line2.trim();
                    if (line2.startsWith("\ufeff")) {
                        line2 = line2.substring(1).trim();
                    }
                } while (line2.isEmpty());
                partsList.clear();
                parts = Regexes.split(splitter.reset(line2), line2, partsList, parts);
                // 3400;<CJK Ideograph Extension A, First>;Lo;0;L;;;;;N;;;;;
                // 4DB5;<CJK Ideograph Extension A, Last>;Lo;0;L;;;;;N;;;;;
                // U+4F70   kAccountingNumeric  100
                if (withRange || contents != Contents.DATA) {
                    try {
                        intRange.set(parts[0]);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("line: " + line, e);
                    }
                    if (contents != Contents.DATA) {
                        if (intRange.start != 0 || intRange.end != 0x10FFFF) {
                            if (contents == Contents.MISSING) {
                                // @missing line for less than all of Unicode
                                missingSet.add(intRange.start, intRange.end);
                                contents = Contents.DATA;
                            } else {
                                System.err.println("Unexpected range: " + line);
                            }
                        }
                    }
                }
                state = State.HAVE_NEXT;
            }
            return true; // HAVE_NEXT
        }

        @Override
        public UcdLine next() {
            if (state != State.HAVE_NEXT && !hasNext()) {
                throw new NoSuchElementException();
            }
            state = State.LOOK;
            return this;
        }

        public Contents getType() {
            return contents;
        }

        public String getOriginalLine() {
            return line;
        }

        public String getLineForParsing() {
            return line2;
        }

        /**
         * @return the parts of {@link #getLineForParsing()} after splitting by the file's
         *     separator.
         */
        public String[] getParts() {
            return parts;
        }

        public IntRange getRange() {
            return intRange;
        }
        /**
         * @return Code points covered by @missing lines for less than all of Unicode.
         */
        public UnicodeSet getMissingSet() {
            return missingSet;
        }
    }

    private boolean withTabs = false;
    private boolean withRange = true;
    private boolean withMissing = false;
    private final Iterable<String> rawLines;
    private final UcdFileStats stats = new UcdFileStats();

    public UcdLineParser(Iterable<String> rawLines) {
        this.rawLines = rawLines;
    }

    public UcdLineParser withTabs(boolean t) {
        withTabs = t;
        return this;
    }

    public UcdLineParser withRange(boolean r) {
        withRange = r;
        return this;
    }

    public UcdLineParser withMissing(boolean m) {
        withMissing = m;
        return this;
    }

    @Override
    public Iterator<UcdLine> iterator() {
        return new UcdLine(
                withTabs ? TAB : SEMICOLON, withRange, withMissing, rawLines.iterator(), stats);
    }

    public int getLineCount() {
        return stats.lineCount;
    }

    public boolean containsEOF() {
        return stats.containsEOF;
    }
}
