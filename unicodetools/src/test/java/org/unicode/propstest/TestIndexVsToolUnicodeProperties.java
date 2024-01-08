package org.unicode.propstest;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.Sets;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Pair;
import com.ibm.icu.text.UnicodeSet;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.unicode.cldr.util.CodePointEscaper;
import org.unicode.cldr.util.SimpleUnicodeSetFormatter;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UnicodeProperty;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.ToolUnicodePropertySource;
import org.unicode.text.utility.Utility;
import org.unicode.unittest.TestFmwkMinusMinus;

public class TestIndexVsToolUnicodeProperties extends TestFmwkMinusMinus {
    private static final int MAX_USET_ITEMS = 15;

    private static final IndexUnicodeProperties iup =
            IndexUnicodeProperties.make(Default.ucdVersion());

    private static final ToolUnicodePropertySource tup =
            ToolUnicodePropertySource.make(Default.ucdVersion());

    SimpleUnicodeSetFormatter susetFormatter = new SimpleUnicodeSetFormatter();

    @Test
    public void TestProperties() {

        warnln("Comparing values for " + Default.ucdVersion());
        Set<String> iupNames = new LinkedHashSet<>(iup.getAvailableNames());
        Set<String> tupNames = new LinkedHashSet<>(tup.getAvailableNames());
        Set<String> common = Sets.intersection(iupNames, tupNames);

        Set<String> iupMissing = Sets.difference(tupNames, iupNames);
        warnln(
                "\t*TUP properties missing from IUP:\t"
                        + iupMissing.size()
                        + "\t"
                        + Joiner.on(' ').join(iupMissing));

        Set<String> tupMissing = Sets.difference(iupNames, tupNames);
        warnln(
                "\t*IUP properties missing from TUP:\t"
                        + +tupMissing.size()
                        + "\t"
                        + Joiner.on(' ').join(tupMissing));

        for (String propName : common) {
            // warnln(propName);
            UnicodeProperty iupProp = iup.getProperty(propName);
            UnicodeProperty tupProp = tup.getProperty(propName);
            UnicodeSet iupNullTupEmpty = new UnicodeSet();
            UnicodeMap<Pair<String, String>> iupDiffTup = new UnicodeMap<>();

            for (int i = 0x0; i <= 0x10ffff; ++i) {
                String iupValue = iupProp.getValue(i);
                String tupValue = tupProp.getValue(i);
                if (!Objects.equal(iupValue, tupValue)) {
                    if (iupValue == null && "".equals(tupValue)
                            || iupValue != null
                                    && "NaN".equals(iupValue.toString())
                                    && tupValue == null) {
                        iupNullTupEmpty.add(i);
                    } else {
                        iupDiffTup.put(i, Pair.of(showContents(iupValue), showContents(tupValue)));
                    }
                }
            }
            if (!iupDiffTup.isEmpty()) {
                int count = iupDiffTup.size();

                final Collection<Pair<String, String>> values = iupDiffTup.getAvailableValues();
                int valueCount = 0;
                UnicodeSet remaining = new UnicodeSet(iupDiffTup.keySet());
                for (Pair<String, String> value : values) {
                    final UnicodeSet uset = iupDiffTup.getSet(value);
                    errln("\t" + propName + showLine(uset, value.first, value.second));
                    remaining.removeAll(uset);
                    if (++valueCount > 5) {
                        errln(
                                "\t"
                                        + propName
                                        + "\t"
                                        + remaining.size()
                                        + "\t"
                                        + format(remaining, 30)
                                        + "\tothers");
                        break;
                    }
                }
            }
            if (!iupNullTupEmpty.isEmpty()) {
                warnln(
                        "\t"
                                + propName
                                + showLine(iupNullTupEmpty, showContents(null), showContents("")));
            }
        }
    }

    public String showContents(String iupValue) {
        return iupValue == null ? "{NULL}" : iupValue.isBlank() ? "{EMPTY}" : format(iupValue);
    }

    private String showLine(UnicodeSet failures, String iupValue, String tupValue) {
        return "\t"
                + failures.size()
                + "\t"
                + format(failures, MAX_USET_ITEMS)
                + "\tIUP\t"
                + iupValue
                + "\t≠\tTUP\t"
                + tupValue;
    }

    // copied from CLDR, should make public there

    public static String format(UnicodeSet uset, int maxItems) {
        return appendFormattedLimit(new StringBuilder(), uset, maxItems).toString();
    }

    public static String format(String string) {
        StringBuilder foo = appendFormatted(new StringBuilder(), NEEDS_ESCAPE_STRING, string);
        return foo.toString();
    }

    public static StringBuilder appendFormattedLimit(
            StringBuilder builder, UnicodeSet uset, int maxItems) {
        builder.append('[');
        int rangeCount = Math.min(uset.getRangeCount(), maxItems);
        for (int range = 0; range < rangeCount; ++range) {
            int start = uset.getRangeStart(range);
            int end = uset.getRangeEnd(range);
            if (builder.length() > 1) {
                builder.append(' ');
            }
            appendFormatted(builder, NEEDS_ESCAPE, start);
            if (start != end) {
                builder.append('-');
                appendFormatted(builder, NEEDS_ESCAPE, end);
            }
        }
        int remaining = rangeCount - uset.getRangeCount();
        if (remaining > 0) {
            for (String string : uset.strings()) {
                builder.append('{');
                appendFormatted(builder, NEEDS_ESCAPE_STRING, string);
                builder.append('}');
                if (--remaining <= 0) {
                    break;
                }
            }
        }

        if (remaining == 0) {
            builder.append(']');
        } else {
            builder.append("…]");
        }
        return builder;
    }

    public static StringBuilder appendFormatted(
            StringBuilder builder, UnicodeSet toEscape, String string) {
        string.codePoints().forEach(x -> appendFormatted(builder, toEscape, x));
        return builder;
    }

    public static StringBuilder appendFormatted(
            StringBuilder builder, UnicodeSet toEscape, int start) {
        if (toEscape.contains(start)) {
            builder.append("\\x{" + Utility.hex(start, 1) + "}");
        } else {
            builder.appendCodePoint(start);
        }
        return builder;
    }

    public static final UnicodeSet NEEDS_ESCAPE_STRING =
            new UnicodeSet("[[:DI:][:Pat_WS:][:WSpace:][:C:][:Z:]]")
                    .add('\\')
                    .removeAll(CodePointEscaper.EMOJI_INVISIBLES)
                    .remove(' ')
                    .freeze();

    public static final UnicodeSet NEEDS_ESCAPE =
            new UnicodeSet(NEEDS_ESCAPE_STRING).addAll("-{}[] ").freeze();
}
