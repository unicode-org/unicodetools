/*
 ******************************************************************************
 * Copyright (C) 2004-2005, International Business Machines Corporation and        *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 *
 */

package org.unicode.tools;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeMap.Composer;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.XSymbolTable;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.ULocale;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.RegexUtilities;
import org.unicode.cldr.util.TransliteratorUtilities;
import org.unicode.props.UnicodeProperty;
import org.unicode.tools.Segmenter.SegmentationRule.Breaks;

/** Ordered list of rules, with variables resolved before building. Use Builder to make. */
public class Segmenter {
    public enum Target {
        FOR_UCD,
        FOR_CLDR
    }

    public static final int REGEX_FLAGS = Pattern.COMMENTS | Pattern.MULTILINE | Pattern.DOTALL;
    public static final Pattern IDENTIFIER_PATTERN =
            Pattern.compile("[$][\\p{Alnum}_]+", REGEX_FLAGS);

    /**
     * If not null, masks off the character properties so the UnicodeSets are easier to use when
     * debugging.
     */
    public static UnicodeSet DEBUG_REDUCE_SET_SIZE = null; // new

    // UnicodeSet("[\\u0000-\\u00FF\\u0300-\\u03FF\\u2000-\\u20FF]");
    // // new UnicodeSet("[\\u0000-\\u00FF\\u2000-\\u20FF]"); //
    // or null
    private static final boolean SHOW_VAR_CONTENTS = false;
    private static final boolean SHOW_SAMPLES = false;
    private static final String DEBUG_AT_STRING = "\u0009\u0308\u00A0"; // null to turn off
    private static final String DEBUG_AT_RULE_CONTAINING = "$Spec3_"; // null to turn off

    public final Target target;

    private UnicodeMap<String> samples = new UnicodeMap<String>();

    private Segmenter(Target target) {
        this.target = target;
    }

    public static interface CodePointShower {
        String show(int codePoint);
    }

    public static Builder make(UnicodeProperty.Factory propFactory, String type) {
        return make(propFactory, type, Target.FOR_UCD);
    }

    public static Builder make(UnicodeProperty.Factory propFactory, String type, Target target) {
        String sourceFileName =
                target == Target.FOR_CLDR ? "SegmenterCldr.txt" : "SegmenterDefault.txt";
        Builder b = new Builder(propFactory, target);

        // quick and dirty cache of file lines, so we don't hit file multiple times.
        Multimap<String, String> data = FILE_CACHE.get(sourceFileName);

        if (data == null) {
            data = LinkedHashMultimap.create();
            int lineCount = 0;
            String key = null;
            for (String line : FileUtilities.in(Segmenter.class, sourceFileName)) {
                ++lineCount;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("##")) {
                    continue;
                }
                if (line.startsWith("@")) {
                    key = line.substring(1).trim();
                    continue;
                }
                if (key == null) {
                    throw new IllegalArgumentException(
                            "Missing @ type in rule syntax, "
                                    + sourceFileName
                                    + " line="
                                    + lineCount);
                }
                data.put(key, line);
            }
            FILE_CACHE.put(sourceFileName, ImmutableMultimap.copyOf(data));
        }
        Collection<String> lines = data.get(type);
        if (lines == null) {
            throw new IllegalArgumentException(
                    "Missing type=" + type + " in file " + sourceFileName);
        }
        for (String line : lines) {
            b.addLine(line);
        }
        return b;
    }

    static final Map<String, Multimap<String, String>> FILE_CACHE = new ConcurrentHashMap<>();

    /** Certain rules are generated, and have artificial numbers */
    public static final double NOBREAK_SUPPLEMENTARY = 0.1,
            BREAK_SOT = 0.2,
            BREAK_EOT = 0.3,
            BREAK_ANY = 999;

    /** Convenience for formatting doubles */
    public static NumberFormat nf = NumberFormat.getInstance(ULocale.ENGLISH);

    static {
        nf.setMinimumFractionDigits(0);
    }

    /**
     * Does the rule list give a break at this point? Also sets the rule number that matches, for
     * return by getBreakRule.
     *
     * @param text
     * @param position
     * @return
     */
    public boolean breaksAt(CharSequence text, int position) {
        if (DEBUG_AT_STRING != null && DEBUG_AT_STRING.equals(text)) {
            System.out.println("!#$@541 Debug");
        }
        if (position == 0) {
            breakRule = BREAK_SOT;
            return true;
        }
        if (position == text.length()) {
            breakRule = BREAK_EOT;
            return true;
        }
        // don't break in middle of surrogate
        if (UTF16.isLeadSurrogate(text.charAt(position - 1))
                && UTF16.isTrailSurrogate(text.charAt(position))) {
            breakRule = NOBREAK_SUPPLEMENTARY;
            return false;
        }
        StringBuilder remapped = new StringBuilder(text.toString());
        Consumer<CharSequence> remap =
                (s) -> {
                    remapped.setLength(0);
                    remapped.append(s);
                };
        Integer[] indexInRemapped = new Integer[text.length() + 1];
        for (int i = 0; i < indexInRemapped.length; ++i) {
            indexInRemapped[i] = i;
        }
        for (int i = 0; i < rules.size(); ++i) {
            SegmentationRule rule = rules.get(i);
            if (DEBUG_AT_RULE_CONTAINING != null
                    && rule.toString().contains(DEBUG_AT_RULE_CONTAINING)) {
                System.out.println(" !#$@543 Debug");
            }
            Breaks result = rule.applyAt(position, remapped, indexInRemapped, remap);
            if (result != SegmentationRule.Breaks.UNKNOWN_BREAK) {
                breakRule = orders.get(i).doubleValue();
                return result == SegmentationRule.Breaks.BREAK;
            }
        }
        breakRule = BREAK_ANY;
        return true; // default
    }

    public int getRuleStatusVec(int[] ruleStatus) {
        ruleStatus[0] = 0;
        return 1;
    }

    /**
     * Add a numbered rule.
     *
     * @param order
     * @param rule
     */
    public void add(double order, SegmentationRule rule) {
        orders.add(new Double(order));
        rules.add(rule);
    }

    public SegmentationRule get(double order) {
        int loc = orders.indexOf(new Double(order));
        if (loc < 0) return null;
        return rules.get(loc);
    }

    /**
     * Gets the rule number that matched at the point. Only valid after calling breaksAt
     *
     * @return
     */
    public double getBreakRule() {
        return breakRule;
    }

    /** Debugging aid */
    public String toString() {
        return toString(false);
    }

    public String toString(boolean showResolved) {
        String result = "";
        for (int i = 0; i < rules.size(); ++i) {
            if (i != 0) result += Utility.LINE_SEPARATOR;
            result += orders.get(i) + ")\t" + rules.get(i).toString(showResolved);
        }
        return result;
    }

    public abstract static class SegmentationRule {
        /** Status of a breaking rule */
        public enum Breaks {
            UNKNOWN_BREAK,
            BREAK,
            NO_BREAK
        };

        /**
         * Applies this rule throughout the text.
         *
         * @param remappedString The text, with any preceding remappings applied.
         * @param indexInRemapped An array whose size is one greater than the original string.
         *     Associates indices in the original string to indices in remappedString.
         *     indexInRemapped[0] == 0, and indexInRemapped[indexInRemapped.size() - 1] ==
         *     remappedString.size(). Whenever indexInRemapped[i] == null, resolvedBreaks[i] ==
         *     NO_BREAK: this corresponds to positions inside a string which has been replaced by a
         *     remap rule. Remap rules may update this mapping.
         * @param resolvedBreaks An array whose size is one greater than the original string,
         *     indicating resolved breaks in the string. Values that are UNKNOWN_BREAK are updated
         *     if the rule applies to their position.
         * @param remap Called by remap rules with the value of remappedString to be passed to
         *     subsequent rules. The indices in indexInRemapped are updated consistently.
         */
        public abstract void apply(
                CharSequence remappedString,
                Integer[] indexInRemapped,
                Breaks[] resolvedBreaks,
                Consumer<CharSequence> remap);

        protected abstract String toString(boolean showResolved);

        /** Same as above, but only returns the resolution at the current position. */
        public abstract Breaks applyAt(
                int position,
                CharSequence remappedString,
                Integer[] indexInRemapped,
                Consumer<CharSequence> remap);

        public String toString() {
            return toString(false);
        }
    }

    /** A « treat as » rule. */
    public static class RemapRule extends SegmentationRule {

        public RemapRule(String leftHandSide, String replacement, String line) {
            pattern = Pattern.compile(leftHandSide, REGEX_FLAGS);
            this.replacement = replacement;
            name = line;
        }

        @Override
        public void apply(
                CharSequence remappedString,
                Integer[] indexInRemapped,
                Breaks[] resolvedBreaks,
                Consumer<CharSequence> remap) {
            final var result = new StringBuilder();
            int i = 0;
            int offset = 0;
            final var matcher = pattern.matcher(remappedString);
            while (matcher.find()) {
                for (; ; ++i) {
                    if (indexInRemapped[i] == null) {
                        continue;
                    }
                    if (indexInRemapped[i] > matcher.start()) {
                        break;
                    }
                    indexInRemapped[i] += offset;
                }
                for (; ; ++i) {
                    if (indexInRemapped[i] == null) {
                        continue;
                    }
                    if (indexInRemapped[i] == matcher.end()) {
                        break;
                    }
                    if (resolvedBreaks[i] == Breaks.BREAK) {
                        throw new IllegalArgumentException(
                                "Replacement rule at remapped indices "
                                        + matcher.start()
                                        + " sqq. spans a break: "
                                        + remappedString);
                    }
                    resolvedBreaks[i] = Breaks.NO_BREAK;
                    indexInRemapped[i] = null;
                }
                matcher.appendReplacement(result, replacement);
                offset = result.length() - indexInRemapped[i];
            }
            for (; i < indexInRemapped.length; ++i) {
                if (indexInRemapped[i] == null) {
                    continue;
                }
                indexInRemapped[i] += offset;
            }
            matcher.appendTail(result);
            if (indexInRemapped[indexInRemapped.length - 1] != result.length()) {
                StringBuilder indices = new StringBuilder();
                for (var j : indexInRemapped) {
                    indices.append(j == null ? "null" : j.toString());
                    indices.append(",");
                }
                throw new IllegalArgumentException(
                        "Inconsistent indexInRemapped "
                                + indices
                                + " for new remapped string "
                                + result);
            }
            remap.accept(result);
        }

        private Pattern pattern;
        private String replacement;
        private String name;

        @Override
        public Breaks applyAt(
                int position,
                CharSequence remappedString,
                Integer[] indexInRemapped,
                Consumer<CharSequence> remap) {
            var resolvedBreaks = new Breaks[indexInRemapped.length];
            apply(remappedString, indexInRemapped, resolvedBreaks, remap);
            return resolvedBreaks[position] == null
                    ? Breaks.UNKNOWN_BREAK
                    : resolvedBreaks[position];
        }

        @Override
        protected String toString(boolean showResolved) {
            return name;
        }
    }

    /** A rule that determines the status of an offset. */
    public static class RegexRule extends SegmentationRule {
        /**
         * @param before pattern for the text after the offset. All variables must be resolved.
         * @param result the break status to return when the rule is invoked
         * @param after pattern for the text before the offset. All variables must be resolved.
         * @param line
         */
        public RegexRule(String before, Breaks result, String after, String line) {
            breaks = result;
            before = ".*(" + before + ")";
            String parsing = null;
            try {
                this.before = Pattern.compile(parsing = before, REGEX_FLAGS);
                this.after = Pattern.compile(parsing = after, REGEX_FLAGS);
            } catch (PatternSyntaxException e) {
                // Format: Unclosed character class near index 927
                int index = e.getIndex();
                throw (RuntimeException)
                        new IllegalArgumentException(
                                        "On <"
                                                + line
                                                + ">, Can't parse: "
                                                + parsing.substring(0, index)
                                                + "<<<>>>"
                                                + parsing.substring(index))
                                .initCause(e);
            } catch (RuntimeException e) {
                // Unclosed character class near index 927
                throw (RuntimeException)
                        new IllegalArgumentException("On <" + line + ">, Can't parse: " + parsing)
                                .initCause(e);
            }
            name = line;
            resolved =
                    Utility.escape(before)
                            + (result == Breaks.NO_BREAK ? " \u00D7 " : " \u00F7 ")
                            + Utility.escape(after);
            // COMMENTS allows whitespace
        }

        @Override
        public void apply(
                CharSequence remappedString,
                Integer[] indexInRemapped,
                Breaks[] resolvedBreaks,
                Consumer<CharSequence> remap) {
            for (int i = 0; i < indexInRemapped.length; ++i) {
                if (resolvedBreaks[i] == Breaks.UNKNOWN_BREAK) {
                    resolvedBreaks[i] = applyAt(i, remappedString, indexInRemapped, remap);
                }
            }
        }

        @Override
        public Breaks applyAt(
                int position,
                CharSequence remappedString,
                Integer[] indexInRemapped,
                Consumer<CharSequence> remap) {
            if (after.matcher(remappedString)
                            .region(indexInRemapped[position], remappedString.length())
                            .lookingAt()
                    && before.matcher(remappedString)
                            .region(0, indexInRemapped[position])
                            .matches()) {
                return breaks;
            }
            return Breaks.UNKNOWN_BREAK;
        }

        @Override
        public String toString(boolean showResolved) {
            String result = name;
            if (showResolved) result += ": " + resolved;
            return result;
        }

        // ============== Internals ================
        // We cannot use a single regex of the form "(?<= before) after" because
        // (RI RI)* RI × RI would require unbounded lookbehind.
        private Pattern before;
        private Pattern after;
        private String name;

        private String resolved;
        private Breaks breaks;
    }

    /** Separate the builder for clarity */

    /** Sort the longest strings first. Used for variable lists. */
    static Comparator<String> LONGEST_STRING_FIRST =
            new Comparator<String>() {
                public int compare(String s0, String s1) {
                    int len0 = s0.length();
                    int len1 = s1.length();
                    if (len0 < len1) return 1; // longest first
                    if (len0 > len1) return -1;
                    // lengths equal, use string order
                    return s0.compareTo(s1);
                }
            };

    /**
     * Used to build RuleLists. Can be used to do inheritance, since (a) adding a variable overrides
     * any previous value, and any variables used in its value are resolved before adding, and (b)
     * adding a rule sorts/overrides according to numeric value.
     */
    public static class Builder {
        private final UnicodeProperty.Factory propFactory;
        private final Target target;
        private XSymbolTable symbolTable;
        private List<String> rawVariables = new ArrayList<String>();
        private Map<Double, String> xmlRules = new TreeMap<Double, String>();
        private Map<Double, String> htmlRules = new TreeMap<Double, String>();
        private List<String> lastComments = new ArrayList<String>();
        private UnicodeMap<String> samples = new UnicodeMap<String>();

        public Builder(UnicodeProperty.Factory factory, Target target) {
            propFactory = factory;
            this.target = target;
            symbolTable = new MyXSymbolTable(); // propFactory.getXSymbolTable();
            htmlRules.put(new Double(BREAK_SOT), "sot \u00F7");
            htmlRules.put(new Double(BREAK_EOT), "\u00F7 eot");
            htmlRules.put(new Double(BREAK_ANY), "\u00F7 Any");
        }

        // copied to make independent of ICU4J internals
        private class MyXSymbolTable extends UnicodeSet.XSymbolTable {
            public boolean applyPropertyAlias(
                    String propertyName, String propertyValue, UnicodeSet result) {
                UnicodeProperty prop = propFactory.getProperty(propertyName);
                if (prop == null) {
                    if (propertyValue.isEmpty()) {
                        prop = propFactory.getProperty("Script");
                        result.clear();
                        UnicodeSet x = prop.getSet(propertyName, result);
                        if (!x.isEmpty()) {
                            return true;
                        }
                    }
                    // If we cannot handle the property name, then we need to really fail.
                    // If we were to just print something and return false, then the UnicodeSet code
                    // would just evaluate this itself, and may succeed but give wrong results.
                    // For example, as long as we require "gc=Cn" and don't handle "Cn" here,
                    // falling back to built-in ICU data means that we get gc=Cn ranges from ICU
                    // rather than from the current Unicode beta.
                    throw new IllegalArgumentException(
                            "Segmenter.MyXSymbolTable: Unknown property " + propertyName);
                }
                // Binary properties:
                // \p{Extended_Pictographic} is equivalent with \p{Extended_Pictographic=Yes}
                if (propertyValue.isEmpty() && prop.isType(UnicodeProperty.BINARY_MASK)) {
                    propertyValue = "Yes";
                }
                result.clear();
                UnicodeSet x = prop.getSet(propertyValue, result);
                if (x.isEmpty()) {
                    // didn't find anything
                    System.out.println(
                            "Segmenter.MyXSymbolTable: !Empty! "
                                    + propertyName
                                    + "="
                                    + propertyValue);
                }
                return true; // mark that we handled it even if there are no results.
            }
        }

        public String toString(String testName, String indent) {

            StringBuffer result = new StringBuffer();
            result.append(indent + "<segmentation type=\"" + testName + "\">")
                    .append(Utility.LINE_SEPARATOR);
            result.append(indent + "\t<variables>").append(Utility.LINE_SEPARATOR);
            for (int i = 0; i < rawVariables.size(); ++i) {
                result.append(indent + "\t\t")
                        .append(rawVariables.get(i))
                        .append(Utility.LINE_SEPARATOR);
            }
            result.append(indent + "\t</variables>").append(Utility.LINE_SEPARATOR);
            result.append(indent + "\t<segmentRules>").append(Utility.LINE_SEPARATOR);
            for (Iterator<Double> it = xmlRules.keySet().iterator(); it.hasNext(); ) {
                Double key = it.next();
                result.append(indent + "\t\t")
                        .append(xmlRules.get(key))
                        .append(Utility.LINE_SEPARATOR);
            }
            for (String comment : lastComments) {
                result.append(indent + "\t\t").append(comment).append(Utility.LINE_SEPARATOR);
            }
            result.append(indent + "\t</segmentRules>").append(Utility.LINE_SEPARATOR);
            result.append(indent + "</segmentation>").append(Utility.LINE_SEPARATOR);
            return result.toString();
        }

        /**
         * Add a line. If contains a =, is a variable definition. Otherwise, is of the form nn)
         * rule, where nn is the number of the rule. For now, pretty lame parsing, because we can't
         * easily determine whether =, etc is part of the regex or not. So any 'real' =, etc in a
         * regex must be expressed with unicode escapes, \\u....
         *
         * @param line
         * @return
         */
        public boolean addLine(String line) {
            // for debugging
            if (line.startsWith("show")) {
                line = line.substring(4).trim();
                System.out.println("# " + line + ": ");
                System.out.println("\t" + expandUnicodeSets(replaceVariables(line, variables)));
                return false;
            }
            // dumb parsing for now
            if (line.startsWith("#")) {
                lastComments.add("<!-- " + line.substring(1).trim() + " -->");
                return false;
            }
            int relationPosition = line.indexOf('=');
            if (relationPosition >= 0) {
                addVariable(
                        line.substring(0, relationPosition).trim(),
                        line.substring(relationPosition + 1).trim());
                return false;
            }
            relationPosition = line.indexOf(')');
            Double order;
            try {
                order = new Double(Double.parseDouble(line.substring(0, relationPosition).trim()));
            } catch (Exception e) {
                throw new IllegalArgumentException("Rule must be of form '1)...': <" + line + ">");
            }
            line = line.substring(relationPosition + 1).trim();
            relationPosition = line.indexOf('→');
            if (relationPosition >= 0) {
                addRemapRule(
                        order,
                        line.substring(0, relationPosition).trim(),
                        line.substring(relationPosition + 1).trim(),
                        line);
                return true;
            }
            relationPosition = line.indexOf('\u00F7');
            Breaks breaks = Segmenter.RegexRule.Breaks.BREAK;
            if (relationPosition < 0) {
                relationPosition = line.indexOf('\u00D7');
                if (relationPosition < 0) {
                    throw new IllegalArgumentException(
                            "Couldn't find =, \u00F7, or \u00D7 on line: " + line);
                }
                breaks = Segmenter.RegexRule.Breaks.NO_BREAK;
            }
            addRegexRule(
                    order,
                    line.substring(0, relationPosition).trim(),
                    breaks,
                    line.substring(relationPosition + 1).trim(),
                    line);
            return true;
        }

        private transient Matcher whiteSpace = Pattern.compile("\\s+", REGEX_FLAGS).matcher("");
        private transient Matcher identifierMatcher = IDENTIFIER_PATTERN.matcher("");

        /**
         * Add a variable and value. Resolves the internal references in the value.
         *
         * @param displayName
         * @param value
         * @return
         */
        static class MyComposer extends UnicodeMap.Composer<String> {
            public String compose(int codePoint, String string, String a, String b) {
                if (a == null) return b;
                if (b == null) return a;
                if (a.equals(b)) return a;
                return a + "_" + b;
            }
        }

        static MyComposer myComposer = new MyComposer();

        Builder addVariable(String name, String value) {
            if (lastComments.size() != 0) {
                rawVariables.addAll(lastComments);
                lastComments.clear();
            }
            rawVariables.add(
                    "<variable id=\""
                            + name
                            + "\">"
                            + TransliteratorUtilities.toXML.transliterate(value)
                            + "</variable>");
            if (!identifierMatcher.reset(name).matches()) {
                String show = RegexUtilities.showMismatch(identifierMatcher, name);
                throw new IllegalArgumentException(
                        "Variable name must be $id: '" + name + "' — " + show);
            }
            value = replaceVariables(value, variables);
            if (!name.endsWith("_")) {
                try {
                    parsePosition.setIndex(0);
                    UnicodeSet valueSet = new UnicodeSet(value, parsePosition, symbolTable);
                    if (parsePosition.getIndex() != value.length()) {
                        if (SHOW_SAMPLES)
                            System.out.println(
                                    parsePosition.getIndex()
                                            + ", "
                                            + value.length()
                                            + " -- No samples for: "
                                            + name
                                            + " = "
                                            + value);
                    } else if (valueSet.size() == 0) {
                        if (SHOW_SAMPLES)
                            System.out.println("Empty -- No samples for: " + name + " = " + value);
                    } else {
                        String name2 = name;
                        if (name2.startsWith("$")) name2 = name2.substring(1);
                        composeWith(samples, valueSet, name2, myComposer);
                        if (SHOW_SAMPLES) {
                            System.out.println("Samples for: " + name + " = " + value);
                            System.out.println("\t" + valueSet);
                        }
                    }
                } catch (Exception e) {
                }
                ; // skip if can't do
            }

            if (SHOW_VAR_CONTENTS) System.out.println(name + "=" + value);
            // verify that the value is a valid REGEX
            if (value.equals("[]")) {
                value = "(?!a)[a]"; // HACK to match nothing.
            }
            Pattern.compile(expandUnicodeSets(value), REGEX_FLAGS).matcher("");
            // if (false && name.equals("$AL")) {
            // findRegexProblem(value);
            // }
            variables.put(name, value);
            expandedVariables.put(name, expandUnicodeSets(value));
            return this;
        }

        public static UnicodeMap<String> composeWith(
                UnicodeMap<String> target,
                UnicodeSet set,
                String value,
                Composer<String> composer) {
            final Set<String> complements = new HashSet<>();
            for (UnicodeSetIterator it = new UnicodeSetIterator(set); it.next(); ) {
                int i = it.codepoint;
                String v = target.getValue(i);
                // Every `v` is a `value`, no need to rename it to `v_value`.
                if (set.containsAll(target.getSet(v))) {
                    continue;
                }
                if (target.getSet(v).containsAll(set)) {
                    // Every `value` is a `v`, just call them `value` rather than `v_value`;
                    // But we want to call the other `v` `vmvalue`.
                    target.put(i, value);
                    if (v != null) {
                        complements.add(v);
                    }
                } else {
                    final String intersectionName = composer.compose(i, null, v, value);
                    target.put(i, intersectionName);
                    if (v != null) {
                        complements.add(v);
                    }
                }
            }
            // Whenever we have used `v_value` for the intersection, the remaining `v` are
            // actually `v` minus `value`.
            for (String v : complements) {
                System.out.println("Renaming " + v + " to " + v + "m" + value);
               target.putAll(target.getSet(v), v + "m" + value);
            }
            return target;
        }

        Builder addRemapRule(Double order, String before, String after, String line) {
            line = whiteSpace.reset(line).replaceAll(" ");
            if (lastComments.size() != 0) {
                double increment = 0.0001;
                double temp = order.doubleValue() - increment * lastComments.size();
                for (int i = 0; i < lastComments.size(); ++i) {
                    Double position = new Double(temp);
                    if (xmlRules.containsKey(position)) {
                        System.out.println("WARNING: Overriding rule " + position);
                    }
                    xmlRules.put(position, lastComments.get(i));
                    temp += increment;
                }
                lastComments.clear();
            }
            if (htmlRules.containsKey(order)
                    || xmlRules.containsKey(order)
                    || rules.containsKey(order)) {
                throw new IllegalArgumentException("Duplicate numbers for rules: " + order);
            }
            htmlRules.put(order, TransliteratorUtilities.toHTML.transliterate(line));
            xmlRules.put(
                    order,
                    "<rule id=\""
                            + Segmenter.nf.format(order)
                            + "\""
                            // + (flagItems.reset(line).find() ? " normative=\"true\"" : "")
                            + "> "
                            + TransliteratorUtilities.toXML.transliterate(line)
                            + " </rule>");
            rules.put(
                    order,
                    new Segmenter.RemapRule(
                            replaceVariables(before, expandedVariables), after, line));
            return this;
        }

        /**
         * Add a numbered rule, already broken into the parts before and after.
         *
         * @param order
         * @param before
         * @param breaks
         * @param after
         * @param line
         * @return
         */
        Builder addRegexRule(
                Double order, String before, Breaks breaks, String after, String line) {
            // if (brokenIdentifierMatcher.reset(line).find()) {
            // int start = brokenIdentifierMatcher.start();
            // int end = brokenIdentifierMatcher.end();
            // throw new IllegalArgumentException("Illegal identifier at:"
            // + line.substring(0,start) + "<<"
            // + line.substring(start, end) + ">>"
            // + line.substring(end)
            // );
            // }
            line = whiteSpace.reset(line).replaceAll(" ");
            // insert comments before current line, in order.
            if (lastComments.size() != 0) {
                double increment = 0.0001;
                double temp = order.doubleValue() - increment * lastComments.size();
                for (int i = 0; i < lastComments.size(); ++i) {
                    Double position = new Double(temp);
                    if (xmlRules.containsKey(position)) {
                        System.out.println("WARNING: Overriding rule " + position);
                    }
                    xmlRules.put(position, lastComments.get(i));
                    temp += increment;
                }
                lastComments.clear();
            }
            if (htmlRules.containsKey(order)
                    || xmlRules.containsKey(order)
                    || rules.containsKey(order)) {
                throw new IllegalArgumentException("Duplicate numbers for rules: " + order);
            }
            htmlRules.put(order, TransliteratorUtilities.toHTML.transliterate(line));
            xmlRules.put(
                    order,
                    "<rule id=\""
                            + Segmenter.nf.format(order)
                            + "\""
                            // + (flagItems.reset(line).find() ? " normative=\"true\"" : "")
                            + "> "
                            + TransliteratorUtilities.toXML.transliterate(line)
                            + " </rule>");
            if (after.contains("[^$OLetter")) {
                System.out.println("!@#$31 Debug");
            }
            rules.put(
                    order,
                    new Segmenter.RegexRule(
                            replaceVariables(before, expandedVariables),
                            breaks,
                            replaceVariables(after, expandedVariables),
                            line));
            return this;
        }

        /**
         * Return a RuleList from what we have currently.
         *
         * @return
         */
        public Segmenter make() {
            Segmenter result = new Segmenter(target);
            for (Double key : rules.keySet()) {
                result.add(key.doubleValue(), rules.get(key));
            }
            result.samples = samples;
            return result;
        }

        // ============== internals ===================
        private Map<String, String> expandedVariables =
                new TreeMap<String, String>(LONGEST_STRING_FIRST);
        private Map<String, String> variables =
                new TreeMap<String, String>(LONGEST_STRING_FIRST); // sorted by length,
        // longest first, to
        // make substitution
        // easy
        private Map<Double, SegmentationRule> rules = new TreeMap<Double, SegmentationRule>();

        public Map<Double, SegmentationRule> getProcessedRules() {
            return rules;
        }

        /**
         * A workhorse. Replaces all variable references: anything of the form $id. Flags an error
         * if anything of that form is not a variable.
         *
         * @param input
         * @return
         */
        private static String replaceVariables(String input, Map<String, String> variables) {
            // to do, optimize
            String result = input;
            int position = -1;
            main:
            while (true) {
                position = result.indexOf('$', position);
                if (position < 0) break;
                for (String name : variables.keySet()) {
                    if (result.regionMatches(position, name, 0, name.length())) {
                        String value = variables.get(name);
                        result =
                                result.substring(0, position)
                                        + value
                                        + result.substring(position + name.length());
                        position += value.length(); // don't allow overlap
                        continue main;
                    }
                }
                if (IDENTIFIER_PATTERN.matcher(result.substring(position)).lookingAt()) {
                    throw new IllegalArgumentException(
                            "Illegal variable at: '" + result.substring(position) + "'");
                }
            }
            return result;
        }

        /** Replaces Unicode Sets with literals. */
        public String expandUnicodeSets(String input) {
            String result = input;
            // replace properties
            // TODO really dumb parse for now, fix later
            for (int i = 0; i < result.length(); ++i) {
                if (UnicodeSet.resemblesPattern(result, i)) {
                    parsePosition.setIndex(i);
                    UnicodeSet temp = new UnicodeSet(result, parsePosition, symbolTable);
                    String insert = getInsertablePattern(temp);
                    result =
                            result.substring(0, i)
                                    + insert
                                    + result.substring(parsePosition.getIndex());
                    i +=
                            insert.length()
                                    - 1; // skip over inserted stuff; -1 since the loop will add
                }
            }
            return result;
        }

        transient ParsePosition parsePosition = new ParsePosition(0);

        /**
         * Transform a unicode pattern into stuff we can use in Java.
         *
         * @param temp
         * @return
         */
        private String getInsertablePattern(UnicodeSet temp) {
            temp.complement().complement();
            if (DEBUG_REDUCE_SET_SIZE != null) {
                UnicodeSet temp2 = new UnicodeSet(temp);
                temp2.retainAll(DEBUG_REDUCE_SET_SIZE);
                if (temp2.isEmpty()) {
                    temp = new UnicodeSet(temp.getRangeStart(0), temp.getRangeEnd(0));
                } else {
                    temp = temp2;
                }
            }

            String result = toPattern(temp, JavaRegexShower);
            // double check the pattern!!
            UnicodeSet reversal = new UnicodeSet(result);
            if (!reversal.equals(temp))
                throw new IllegalArgumentException("Failure on UnicodeSet print");
            return result;
        }

        static UnicodeSet JavaRegex_uxxx =
                new UnicodeSet(
                        "[[[:White_Space:][:defaultignorablecodepoint:]#]&[\\u0000-\\uFFFF]]"); // hack to fix # in Java
        static UnicodeSet JavaRegex_slash =
                new UnicodeSet("[[:Pattern_White_Space:]" + "\\[\\]\\-\\^\\&\\\\\\{\\}\\$\\:]");
        static CodePointShower JavaRegexShower =
                new CodePointShower() {
                    public String show(int codePoint) {
                        if (JavaRegex_uxxx.contains(codePoint)) {
                            if (codePoint > 0xFFFF) {
                                return "\\u"
                                        + Utility.hex(UTF16.getLeadSurrogate(codePoint))
                                        + "\\u"
                                        + Utility.hex(UTF16.getTrailSurrogate(codePoint));
                            }
                            return "\\u" + Utility.hex(codePoint);
                        }
                        if (JavaRegex_slash.contains(codePoint))
                            return "\\" + UTF16.valueOf(codePoint);
                        return UTF16.valueOf(codePoint);
                    }
                };

        private static String toPattern(UnicodeSet temp, CodePointShower shower) {
            StringBuffer result = new StringBuffer();
            result.append('[');
            for (UnicodeSetIterator it = new UnicodeSetIterator(temp); it.nextRange(); ) {
                // three cases: single, adjacent, range
                int first = it.codepoint;
                result.append(shower.show(first++));
                if (first > it.codepointEnd) continue;
                if (first != it.codepointEnd) result.append('-');
                result.append(shower.show(it.codepointEnd));
            }
            result.append(']');
            return result.toString();
        }

        /* The mapping of variables to their values. */
        public Map<String, String> getVariables() {
            return Collections.unmodifiableMap(variables);
        }

        public List<String> getRules() {
            List<String> result = new ArrayList<String>();
            for (Double key : htmlRules.keySet()) {
                result.add(key.toString() + ")\t" + htmlRules.get(key));
            }
            return result;
        }
    }

    // ============== Internals ================

    private List<SegmentationRule> rules = new ArrayList<SegmentationRule>(1);
    private List<Double> orders = new ArrayList<Double>(1);
    private double breakRule;

    public UnicodeMap<String> getSamples() {
        return samples;
    }
}
