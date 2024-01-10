package org.unicode.propstest;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.Sets;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Pair;
import com.ibm.icu.text.UnicodeSet;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import org.unicode.cldr.util.CodePointEscaper;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Rational;
import org.unicode.cldr.util.SimpleUnicodeSetFormatter;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.ShimUnicodePropertyFactory;
import org.unicode.props.UnicodeProperty;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.ToolUnicodePropertySource;
import org.unicode.text.utility.Utility;

public class CheckIndexVsToolUnicodeProperties {
    final int MAX_USET_ITEMS = 15;

    final ShimUnicodePropertyFactory iup =
            new ShimUnicodePropertyFactory(IndexUnicodeProperties.make(Default.ucdVersion()));

    final ToolUnicodePropertySource tup = ToolUnicodePropertySource.make(Default.ucdVersion());

    SimpleUnicodeSetFormatter susetFormatter = new SimpleUnicodeSetFormatter();

    // null to skip
    final Set<String> debugLimited = null;
    //final Set<String> debugLimited = ImmutableSet.of("Bidi_Paired_Bracket");
    final UnicodeSet debugItems = new UnicodeSet("[\\x{0}]");

    enum Shim {
        equals,
        diffDefault,
        diffNumberFormat,
        different,
    }

    public static void main(String[] args) {
        new CheckIndexVsToolUnicodeProperties().TestProperties();
    }

    public void TestProperties() {

        warnln("\tComparing values for " + Default.ucdVersion());
        Set<String> iupNames = new LinkedHashSet<>(iup.getAvailableNames());
        Set<String> tupNames = new LinkedHashSet<>(tup.getAvailableNames());
        Set<String> common = Sets.intersection(iupNames, tupNames);
        if (debugLimited != null) {
            common = debugLimited;
        }

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
            UnicodeProperty iupProp = iup.getProperty(propName);
            UnicodeProperty tupProp = tup.getProperty(propName);
            UnicodeSet iupNullTupEmpty = new UnicodeSet();
            UnicodeMap<Pair<String, String>> iupDiffTup = new UnicodeMap<>();
            Counter<Shim> shims = new Counter<>();

            for (int i = 0x0; i <= 0x10ffff; ++i) {
                if (debugItems.contains(i)) {
                    int debug = 0; // stop if debugging
                }
                String iupValue = iupProp.getValue(i);
                String tupValue = tupProp.getValue(i);
                final Shim shim = equalsShim(propName, iupValue, tupValue);
                if (shim != Shim.equals) {
                    shims.add(shim, 1);

                    switch (shim) {
                        case equals:
                            break;
                        case diffDefault:
                            iupNullTupEmpty.add(i);
                            break;
                        case diffNumberFormat:
                            iupNullTupEmpty.add(i);
                            break;
                        case different:
                            equalsShim(propName, iupValue, tupValue);
                            iupDiffTup.put(
                                    i, Pair.of(showContents(iupValue), showContents(tupValue)));
                            break;
                    }
                }
            }
            if (!iupDiffTup.isEmpty()) {
                final Collection<Pair<String, String>> values = iupDiffTup.getAvailableValues();
                int valueCount = 0;
                UnicodeSet remaining = new UnicodeSet(iupDiffTup.keySet());
                for (Pair<String, String> value : values) {
                    final UnicodeSet uset = iupDiffTup.getSet(value);
                    errln("\t" + propName + showLine(uset, value.first, value.second, null));
                    remaining.removeAll(uset);
                    if (++valueCount > 5) {
                        errln(
                                "\t"
                                        + propName
                                        + "\t"
                                        + remaining.size()
                                        + "\t"
                                        + format(remaining, 30)
                                        + "\t"
                                        + shims
                                        + "\tothers");
                        break;
                    }
                }
            }
            if (!iupNullTupEmpty.isEmpty()) {
                warnln(
                        "\t"
                                + propName
                                + showLine(
                                        iupNullTupEmpty,
                                        showContents(null),
                                        showContents(""),
                                        shims));
            }
        }
    }

    private void errln(String string) {
        System.out.println("SEVERE" + string);
    }

    private void warnln(String string) {
        System.out.println("WARNING" + string);
    }

    private Shim equalsShim(String propName, String iupValue, String tupValue) {
        if (Objects.equal(iupValue, tupValue)) {
            return Shim.equals;
        } else if (iupValue == null && "".equals(tupValue)
                || iupValue != null && "NaN".equals(iupValue.toString()) && tupValue == null) {
            return Shim.diffDefault;
        } else if (numericValueEquals(propName, iupValue, tupValue)) {
            return Shim.diffNumberFormat;
        } else {
            return Shim.different;
        }
    }

    private boolean numericValueEquals(String propName, String iupValue, String tupValue) {
        if (!propName.equals("Numeric_Value")) {
            return false;
        }
        Rational iupRational = Rational.of(iupValue);
        Rational tupRational = Rational.of(BigDecimal.valueOf(Double.parseDouble(tupValue)));
        return iupRational.approximatelyEquals(tupRational);
    }

    public String showContents(String iupValue) {
        return iupValue == null ? "{NULL}" : iupValue.isBlank() ? "{EMPTY}" : format(iupValue);
    }

    private String showLine(
            UnicodeSet failures, String iupValue, String tupValue, Counter<Shim> shims) {
        return "\t"
                + failures.size()
                + "\t"
                + format(failures, MAX_USET_ITEMS)
                + "\t"
                + "\tIUP\t"
                + iupValue
                + "\t≠\tTUP\t"
                + tupValue
                + (shims == null ? "" : "\t" + shims);
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
