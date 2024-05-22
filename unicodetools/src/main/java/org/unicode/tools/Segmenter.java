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
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.RegexUtilities;
import org.unicode.cldr.util.TransliteratorUtilities;
import org.unicode.props.UnicodeProperty;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.ToolUnicodePropertySource;
import org.unicode.text.utility.Settings;
import org.unicode.tools.Segmenter.Rule.Breaks;

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
        for (int i = 0; i < rules.size(); ++i) {
            Rule rule = rules.get(i);
            if (DEBUG_AT_RULE_CONTAINING != null
                    && rule.toString().contains(DEBUG_AT_RULE_CONTAINING)) {
                System.out.println(" !#$@543 Debug");
            }
            Breaks result = rule.matches(text, position);
            if (result != Rule.Breaks.UNKNOWN_BREAK) {
                breakRule = orders.get(i).doubleValue();
                return result == Rule.Breaks.BREAK;
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
    public void add(double order, Rule rule) {
        orders.add(new Double(order));
        rules.add(rule);
    }

    public Rule get(double order) {
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

    /** A rule that determines the status of an offset. */
    public static class Rule {
        /** Status of a breaking rule */
        public enum Breaks {
            UNKNOWN_BREAK,
            BREAK,
            NO_BREAK
        };

        /**
         * @param before pattern for the text after the offset. All variables must be resolved.
         * @param result the break status to return when the rule is invoked
         * @param after pattern for the text before the offset. All variables must be resolved.
         * @param line
         */
        public Rule(String before, Breaks result, String after, String line) {
            breaks = result;
            before = ".*(" + before + ")";
            String parsing = null;
            try {
                matchPrevious = Pattern.compile(parsing = before, REGEX_FLAGS).matcher("");
                matchSucceeding = Pattern.compile(parsing = after, REGEX_FLAGS).matcher("");
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

        // Matcher numberMatcher = PatternCache.get("[0-9]+").matcher("");

        /**
         * Match the rule against text, at a position
         *
         * @param text
         * @param position
         * @return break status
         */
        public Breaks matches(CharSequence text, int position) {
            if (!matchAfter(matchSucceeding, text, position)) return Breaks.UNKNOWN_BREAK;
            if (!matchBefore(matchPrevious, text, position)) return Breaks.UNKNOWN_BREAK;
            return breaks;
        }

        /** Debugging aid */
        public String toString() {
            return toString(false);
        }

        public String toString(boolean showResolved) {
            String result = name;
            if (showResolved) result += ": " + resolved;
            return result;
        }

        // ============== Internals ================
        // in Java 5, this can be more efficient, and use a single regex
        // of the form "(?<= before) after". MUST then have transparent bounds
        private Matcher matchPrevious;
        private Matcher matchSucceeding;
        private String name;

        private String resolved;
        private Breaks breaks;
    }

    /** utility, since we are using Java 1.4 */
    static boolean matchAfter(Matcher matcher, CharSequence text, int position) {
        return matcher.reset(text.subSequence(position, text.length())).lookingAt();
    }

    /**
     * utility, since we are using Java 1.4 depends on the pattern having been built with .* not
     * very efficient, works for testing and the best we can do.
     */
    static boolean matchBefore(Matcher matcher, CharSequence text, int position) {
        return matcher.reset(text.subSequence(0, position)).matches();
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
                System.out.println("\t" + replaceVariables(line));
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
            relationPosition = line.indexOf('\u00F7');
            Breaks breaks = Segmenter.Rule.Breaks.BREAK;
            if (relationPosition < 0) {
                relationPosition = line.indexOf('\u00D7');
                if (relationPosition < 0) {
                    throw new IllegalArgumentException(
                            "Couldn't find =, \u00F7, or \u00D7 on line: " + line);
                }
                breaks = Segmenter.Rule.Breaks.NO_BREAK;
            }
            addRule(
                    order,
                    line.substring(0, relationPosition).trim(),
                    breaks,
                    line.substring(relationPosition + 1).trim(),
                    line);
            return true;
        }

        private transient Matcher whiteSpace = Pattern.compile("\\s+", REGEX_FLAGS).matcher("");
        private transient Matcher identifierMatcher = IDENTIFIER_PATTERN.matcher("");
        private Map<String, String> originalVariables = new TreeMap<String, String>();

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
            originalVariables.put(name, value);
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
            value = replaceVariables(value);
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
            Pattern.compile(value, REGEX_FLAGS).matcher("");
            // if (false && name.equals("$AL")) {
            // findRegexProblem(value);
            // }
            variables.put(name, value);
            return this;
        }

        public static UnicodeMap<String> composeWith(
                UnicodeMap<String> target,
                UnicodeSet set,
                String value,
                Composer<String> composer) {
            for (UnicodeSetIterator it = new UnicodeSetIterator(set); it.next(); ) {
                int i = it.codepoint;
                String v1 = target.getValue(i);
                String v3 = composer.compose(i, null, v1, value);
                if (v1 != v3 && (v1 == null || !v1.equals(v3))) target.put(i, v3);
            }
            return target;
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
        Builder addRule(Double order, String before, Breaks breaks, String after, String line) {
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
                    new Segmenter.Rule(
                            replaceVariables(before), breaks, replaceVariables(after), line));
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
        private Map<String, String> variables =
                new TreeMap<String, String>(LONGEST_STRING_FIRST); // sorted by length,
        // longest first, to
        // make substitution
        // easy
        private Map<Double, Rule> rules = new TreeMap<Double, Rule>();

        public Map<Double, Rule> getProcessedRules() {
            return rules;
        }

        /**
         * A workhorse. Replaces all variable references: anything of the form $id. Flags an error
         * if anything of that form is not a variable. Since we are using Java regex, the properties
         * support is extremely weak. So replace them by literals.
         *
         * @param input
         * @return
         */
        private String replaceVariables(String input) {
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
                if (identifierMatcher.reset(result.substring(position)).lookingAt()) {
                    throw new IllegalArgumentException(
                            "Illegal variable at: '" + result.substring(position) + "'");
                }
            }
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

        public Map<String, String> getOriginalVariables() {
            return Collections.unmodifiableMap(originalVariables);
        }
    }

    // ============== Internals ================

    private List<Rule> rules = new ArrayList<Rule>(1);
    private List<Double> orders = new ArrayList<Double>(1);
    private double breakRule;

    public UnicodeMap<String> getSamples() {
        return samples;
    }

    // TODO: delete? move elsewhere?
    // Only used in main() to write to some files. Out of sync with SegmenterDefault.txt.
    private static final String[][] cannedRules = {
        {
            "GraphemeClusterBreak",
            "$CR=\\p{Grapheme_Cluster_Break=CR}",
            "$LF=\\p{Grapheme_Cluster_Break=LF}",
            "$Control=\\p{Grapheme_Cluster_Break=Control}",
            "$Extend=\\p{Grapheme_Cluster_Break=Extend}",
            "$ZWJ=\\p{Grapheme_Cluster_Break=ZWJ}",
            "$RI=\\p{Grapheme_Cluster_Break=Regional_Indicator}",
            "$Prepend=\\p{Grapheme_Cluster_Break=Prepend}",
            "$SpacingMark=\\p{Grapheme_Cluster_Break=SpacingMark}",
            "$L=\\p{Grapheme_Cluster_Break=L}",
            "$V=\\p{Grapheme_Cluster_Break=V}",
            "$T=\\p{Grapheme_Cluster_Break=T}",
            "$LV=\\p{Grapheme_Cluster_Break=LV}",
            "$LVT=\\p{Grapheme_Cluster_Break=LVT}",
            "$Virama=[\\p{Gujr}\\p{sc=Telu}\\p{sc=Mlym}\\p{sc=Orya}\\p{sc=Beng}\\p{sc=Deva}&\\p{Indic_Syllabic_Category=Virama}]",
            "$LinkingConsonant=[\\p{Gujr}\\p{sc=Telu}\\p{sc=Mlym}\\p{sc=Orya}\\p{sc=Beng}\\p{sc=Deva}&\\p{Indic_Syllabic_Category=Consonant}]",

            //                "$E_Base=\\p{Grapheme_Cluster_Break=E_Base}",
            //                "$E_Modifier=\\p{Grapheme_Cluster_Break=E_Modifier}",

            "$ExtPict=\\p{Extended_Pictographic}",
            "$ExtCccZwj=[[$Extend-\\p{ccc=0}] $ZWJ]",
            // "$EBG=\\p{Grapheme_Cluster_Break=E_Base_GAZ}",
            // "$Glue_After_Zwj=\\p{Grapheme_Cluster_Break=Glue_After_Zwj}",

            "# Rules",
            "# Break at the start and end of text, unless the text is empty.",
            "# Do not break between a CR and LF. Otherwise, break before and after controls.",
            "3) $CR          \u00D7          $LF",
            "4) ( $Control | $CR | $LF )         \u00F7",
            "5) \u00F7         ( $Control | $CR | $LF )",
            "# Do not break Hangul syllable sequences.",
            "6) $L         \u00D7         ( $L | $V | $LV | $LVT )",
            "7) ( $LV | $V )         \u00D7         ( $V | $T )",
            "8) ( $LVT | $T)    \u00D7  $T",
            "# Do not break before extending characters or ZWJ.",
            // "9) \u00D7         ($Extend | $ZWJ | $Virama)",
            "9) \u00D7         ($Extend | $ZWJ)",
            "# Only for extended grapheme clusters: Do not break before SpacingMarks, or after Prepend characters.",
            "9.1) \u00D7         $SpacingMark",
            "9.2) $Prepend  \u00D7",
            "9.3) $LinkingConsonant $ExtCccZwj* $Virama $ExtCccZwj*  \u00D7 $LinkingConsonant",
            "# Do not break within emoji modifier sequences or emoji zwj sequences.",
            // "10) $E_Base $Extend* × $E_Modifier",
            "11) $ExtPict $Extend* $ZWJ × $ExtPict",
            "# Do not break within emoji flag sequences. That is, do not break between regional indicator (RI) symbols if there is an odd number of RI characters before the break point.",
            "12) ^ ($RI $RI)* $RI × $RI",
            "13) [^$RI] ($RI $RI)* $RI × $RI",
            "# Otherwise, break everywhere.",
        },
        {
            "LineBreak",
            "# Variables",
            "$AI=\\p{Line_Break=Ambiguous}",
            "$AL=\\p{Line_Break=Alphabetic}",
            "$B2=\\p{Line_Break=Break_Both}",
            "$BA=\\p{Line_Break=Break_After}",
            "$BB=\\p{Line_Break=Break_Before}",
            "$BK=\\p{Line_Break=Mandatory_Break}",
            "$CB=\\p{Line_Break=Contingent_Break}",
            "$CL=\\p{Line_Break=Close_Punctuation}",
            "$CP=\\p{Line_Break=CP}",
            "$CM1=\\p{Line_Break=Combining_Mark}",
            "$CR=\\p{Line_Break=Carriage_Return}",
            "$EX=\\p{Line_Break=Exclamation}",
            "$GL=\\p{Line_Break=Glue}",
            "$H2=\\p{Line_Break=H2}",
            "$H3=\\p{Line_Break=H3}",
            "$HL=\\p{Line_Break=HL}",
            "$HY=\\p{Line_Break=Hyphen}",
            "$ID=\\p{Line_Break=Ideographic}",
            "$IN=\\p{Line_Break=Inseparable}",
            "$IS=\\p{Line_Break=Infix_Numeric}",
            "$JL=\\p{Line_Break=JL}",
            "$JT=\\p{Line_Break=JT}",
            "$JV=\\p{Line_Break=JV}",
            "$LF=\\p{Line_Break=Line_Feed}",
            "$NL=\\p{Line_Break=Next_Line}",
            "$NS=\\p{Line_Break=Nonstarter}",
            "$NU=\\p{Line_Break=Numeric}",
            "$OP=\\p{Line_Break=Open_Punctuation}",
            "$PO=\\p{Line_Break=Postfix_Numeric}",
            "$PR=\\p{Line_Break=Prefix_Numeric}",
            "$QU=\\p{Line_Break=Quotation}",
            "$SA=\\p{Line_Break=Complex_Context}",
            "$SG=\\p{Line_Break=Surrogate}",
            "$SP=\\p{Line_Break=Space}",
            "$SY=\\p{Line_Break=Break_Symbols}",
            "$WJ=\\p{Line_Break=Word_Joiner}",
            "$XX=\\p{Line_Break=Unknown}",
            "$ZW=\\p{Line_Break=ZWSpace}",
            "$CJ=\\p{Line_Break=Conditional_Japanese_Starter}",
            "$RI=\\p{Line_Break=Regional_Indicator}",
            "$EB=\\p{Line_Break=E_Base}",
            "$EM=\\p{Line_Break=E_Modifier}",
            "$ZWJ_O=\\p{Line_Break=ZWJ}",
            "$ZWJ=\\p{Line_Break=ZWJ}",
            "# Macros",
            "$CM=[$CM1 $ZWJ]",
            "# LB 1  Assign a line breaking class to each code point of the input. ",
            "# Resolve AI, CB, SA, SG, and XX into other line breaking classes depending on criteria outside the scope of this algorithm.",
            "# NOTE: CB is ok to fall through, but must handle others here.",
            // "show $AL",
            "$AL=[$AI $AL $SG $XX $SA]",
            "$NS=[$NS $CJ]",
            // "show $AL",
            // "$oldAL=$AL", // for debugging
            "# WARNING: Fixes for Rule 9",
            "# Treat X (CM|ZWJ* as if it were X.",
            "# Where X is any line break class except SP, BK, CR, LF, NL or ZW.",
            "$X=$CM*",
            "# Macros",
            "$Spec1_=[$SP $BK $CR $LF $NL $ZW]",
            "$Spec2_=[^ $SP $BK $CR $LF $NL $ZW]",
            "$Spec3a_=[^ $SP $BA $HY $CM]",
            "$Spec3b_=[^ $BA $HY $CM]",
            "$Spec4_=[^ $NU $CM]",
            "$AI=($AI $X)",
            "$AL=($AL $X)",
            "$B2=($B2 $X)",
            "$BA=($BA $X)",
            "$BB=($BB $X)",
            "$CB=($CB $X)",
            "$CL=($CL $X)",
            "$CP=($CP $X)",
            "$CM=($CM $X)",
            // "$CM=($CM $X)",
            "$EX=($EX $X)",
            "$GL=($GL $X)",
            "$H2=($H2 $X)",
            "$H3=($H3 $X)",
            "$HL=($HL $X)",
            "$HY=($HY $X)",
            "$ID=($ID $X)",
            "$IN=($IN $X)",
            "$IS=($IS $X)",
            "$JL=($JL $X)",
            "$JT=($JT $X)",
            "$JV=($JV $X)",
            "$NS=($NS $X)",
            "$NU=($NU $X)",
            "$OP=($OP $X)",
            "$PO=($PO $X)",
            "$PR=($PR $X)",
            "$QU=($QU $X)",
            "$SA=($SA $X)",
            "$SG=($SG $X)",
            "$SY=($SY $X)",
            "$WJ=($WJ $X)",
            "$XX=($XX $X)",
            "$RI=($RI $X)",
            "$EB=($EB $X)",
            "$EM=($EM $X)",
            "$ZWJ=($ZWJ $X)",
            "# OUT OF ORDER ON PURPOSE",
            "# LB 10  Treat any remaining combining mark as AL.",
            "$AL=($AL | ^ $CM | (?<=$Spec1_) $CM)",
            "# Rules",
            "# LB 4  Always break after hard line breaks (but never between CR and LF).",
            "4) $BK \u00F7",
            "# LB 5  Treat CR followed by LF, as well as CR, LF and NL as hard line breaks.",
            "5.01) $CR \u00D7 $LF",
            "5.02) $CR \u00F7",
            "5.03) $LF \u00F7",
            "5.04) $NL \u00F7",
            "# LB 6  Do not break before hard line breaks.",
            "6) \u00D7 ( $BK | $CR | $LF | $NL )",
            "# LB 7  Do not break before spaces or zero-width space.",
            "7.01) \u00D7 $SP",
            "7.02) \u00D7 $ZW",
            "# LB 8  Break before any character following a zero-width space, even if one or more spaces intervene.",
            "8) $ZW $SP* \u00F7",
            "# LB 8a  Don't break between ZWJ and IDs (for use in Emoji ZWJ sequences)",
            "8.1) $ZWJ_O \u00D7",
            "# LB 9  Do not break a combining character sequence; treat it as if it has the LB class of the base character",
            "# in all of the following rules. (Where X is any line break class except SP, BK, CR, LF, NL or ZW.)",
            "9) $Spec2_ \u00D7 $CM",
            "#WARNING: this is done by modifying the variable values for all but SP.... That is, $AL is really ($AI $CM*)!",
            "# LB 11  Do not break before or after WORD JOINER and related characters.",
            "11.01) \u00D7 $WJ",
            "11.02) $WJ \u00D7",
            "# LB 12  Do not break after NBSP and related characters.",
            // "12.01) [^$SP] \u00D7 $GL",
            "12) $GL \u00D7",
            "12.1) $Spec3a_ \u00D7 $GL",
            "12.2) $Spec3b_ $CM+ \u00D7 $GL",
            "12.3) ^ $CM+ \u00D7 $GL",
            "# LB 13  Do not break before \u2018]\u2019 or \u2018!\u2019 or \u2018;\u2019 or \u2018/\u2019, even after spaces.",
            "# Using customization 7.",
            "13.01) \u00D7 $EX",
            "13.02) $Spec4_ \u00D7 ($CL | $CP | $IS | $SY)",
            "13.03) $Spec4_ $CM+ \u00D7  ($CL | $CP | $IS | $SY)",
            "13.04) ^ $CM+ \u00D7  ($CL | $CP | $IS | $SY)",
            // "13.03) $Spec4_ \u00D7 $IS",
            // "13.04) $Spec4_ \u00D7 $SY",
            "#LB 14  Do not break after \u2018[\u2019, even after spaces.",
            "14) $OP $SP* \u00D7",
            "# LB 15  Do not break within \u2018\"[\u2019, even with intervening spaces.",
            "15) $QU $SP* \u00D7 $OP",
            "# LB 16  Do not break between closing punctuation and a nonstarter (lb=NS), even with intervening spaces.",
            "16) ($CL | $CP) $SP* \u00D7 $NS",
            "# LB 17  Do not break within \u2018\u2014\u2014\u2019, even with intervening spaces.",
            "17) $B2 $SP* \u00D7 $B2",
            "# LB 18  Break after spaces.",
            "18) $SP \u00F7",
            "# LB 19  Do not break before or after \u2018\"\u2019.",
            "19.01)  \u00D7 $QU",
            "19.02) $QU \u00D7",
            "# LB 20  Break before and after unresolved CB.",
            "20.01)  \u00F7 $CB",
            "20.02) $CB \u00F7",
            "# LB 21  Do not break before hyphen-minus, other hyphens, fixed-width spaces, small kana and other non-starters, or after acute accents.",
            "21.01) \u00D7 $BA",
            "21.02) \u00D7 $HY",
            "21.03) \u00D7 $NS",
            "21.04) $BB \u00D7",
            "# LB 21a  Don't break after Hebrew + Hyphen.",
            "21.1) $HL ($HY | $BA) \u00D7",
            "# LB 21b Don’t break between Solidus and Hebrew letters.",
            "21.2) $SY × $HL",
            "# LB 22  Do not break between two ellipses, or between letters, numbers or exclamations and ellipsis.",
            // "show $AL",
            "22.01) ($AL | $HL) \u00D7 $IN",
            "22.02) $EX \u00D7 $IN",
            "22.03) ($ID | $EB | $EM) \u00D7 $IN",
            "22.04) $IN \u00D7 $IN",
            "22.05) $NU \u00D7 $IN",
            "# LB 23  Do not break between digits and letters.",
            // "23.01) ($ID | $EB | $EM) \u00D7 $PO",
            "23.02) ($AL | $HL) \u00D7 $NU",
            "23.03) $NU \u00D7 ($AL | $HL)",
            "# LB 24  Do not break between prefix and letters or ideographs.",
            "23.12) $PR \u00D7 ($ID | $EB | $EM)",
            "23.13) ($ID | $EB | $EM) \u00D7 $PO",
            "# LB24 Do not break between numeric prefix/postfix and letters, or between letters and prefix/postfix.",
            "24.02) ($PR | $PO) \u00D7 ($AL | $HL)",
            "24.03) ($AL | $HL) \u00D7 ($PR | $PO)",
            "# Using customization 7",
            "# LB Alternative: ( PR | PO) ? ( OP | HY ) ? NU (NU | SY | IS) * (CL | CP) ? ( PR | PO) ?",
            "# Insert \u00D7 every place it could go. However, make sure that at least one thing is concrete, otherwise would cause $NU to not break before or after ",
            "25.01) ($PR | $PO) \u00D7 ( $OP | $HY )? $NU",
            "25.02) ( $OP | $HY ) \u00D7 $NU",
            "25.03) $NU \u00D7 ($NU | $SY | $IS)",
            "25.04) $NU ($NU | $SY | $IS)* \u00D7 ($NU | $SY | $IS | $CL | $CP)",
            "25.05) $NU ($NU | $SY | $IS)* ($CL | $CP)? \u00D7 ($PO | $PR)",
            "#LB 26 Do not break a Korean syllable.",
            "26.01) $JL  \u00D7 $JL | $JV | $H2 | $H3",
            "26.02) $JV | $H2 \u00D7 $JV | $JT",
            "26.03) $JT | $H3 \u00D7 $JT",
            "# LB 27 Treat a Korean Syllable Block the same as ID.",
            "27.01) $JL | $JV | $JT | $H2 | $H3  \u00D7 $PO",
            "27.02) $PR \u00D7 $JL | $JV | $JT | $H2 | $H3",
            "# LB 28  Do not break between alphabetics (\"at\").",
            "28) ($AL | $HL) \u00D7 ($AL | $HL)",
            "# LB 29  Do not break between numeric punctuation and alphabetics (\"e.g.\").",
            "29) $IS \u00D7 ($AL | $HL)",
            "# LB 30  Do not break between letters, numbers or ordinary symbols and opening or closing punctuation.",
            "30.01) ($AL | $HL | $NU) \u00D7 $OP",
            "30.02) $CP \u00D7 ($AL | $HL | $NU)",
            "# LB 30a  Break between two Regional Indicators if and only if there is an even number of them before the point being considered.",
            "30.11) ^ ($RI $RI)* $RI × $RI",
            "30.12) [^$RI] ($RI $RI)* $RI × $RI",
            "30.13) $RI ÷ $RI",
            "30.2) $EB × $EM",
        },
        {
            "SentenceBreak",
            "$CR=\\p{Sentence_Break=CR}",
            "$LF=\\p{Sentence_Break=LF}",
            "$Extend=\\p{Sentence_Break=Extend}",
            "$Format=\\p{Sentence_Break=Format}",
            "$Sep=\\p{Sentence_Break=Sep}",
            "$Sp=\\p{Sentence_Break=Sp}",
            "$Lower=\\p{Sentence_Break=Lower}",
            "$Upper=\\p{Sentence_Break=Upper}",
            "$OLetter=\\p{Sentence_Break=OLetter}",
            "$Numeric=\\p{Sentence_Break=Numeric}",
            "$ATerm=\\p{Sentence_Break=ATerm}",
            "$STerm=\\p{Sentence_Break=STerm}",
            "$Close=\\p{Sentence_Break=Close}",
            "$SContinue=\\p{Sentence_Break=SContinue}",
            "$Any=.",
            // "# subtract Format from Control, since we don't want to break before/after",
            // "$Control=[$Control-$Format]",
            "# Expresses the negation in rule 8; can't do this with normal regex, but works with UnicodeSet, which is all we need.",
            // "$NotStuff=[^$OLetter $Upper $Lower $Sep]",
            // "# $ATerm and $Sterm are temporary, to match ICU until UTC decides.",

            "# WARNING: For Rule 5, now add format and extend to everything but Sep, Format, and Extend",
            "$FE=[$Format $Extend]",
            "# Special rules",
            "$NotPreLower_=[^ $OLetter $Upper $Lower $Sep $CR $LF $STerm $ATerm]",
            // "$NotSep_=[^ $Sep $CR $LF]",

            // "$FE=$Extend* $Format*",
            "$Sp=($Sp $FE*)",
            "$Lower=($Lower $FE*)",
            "$Upper=($Upper $FE*)",
            "$OLetter=($OLetter $FE*)",
            "$Numeric=($Numeric $FE*)",
            "$ATerm=($ATerm $FE*)",
            "$STerm=($STerm $FE*)",
            "$Close=($Close $FE*)",
            "$SContinue=($SContinue $FE*)",
            "# Macros",
            "$ParaSep = ($Sep | $CR | $LF)",
            "$SATerm = ($STerm | $ATerm)",
            "# Rules",
            "# Break at the start and end of text, unless the text is empty.",
            "# Do not break within CRLF.",
            "3) $CR          \u00D7          $LF",
            "# Break after paragraph separators.",
            "4) $ParaSep          \u00F7",
            // "3.4) ( $Control | $CR | $LF )         \u00F7",
            // "3.5) \u00F7         ( $Control | $CR | $LF )",
            "# Ignore Format and Extend characters, except after sot, ParaSep, and within CRLF. (See Section 6.2, Replacing Ignore Rules.) This also has the effect of: Any × (Format | Extend)",
            "# WARNING: Implemented as don't break before format (except after linebreaks),",
            "# AND add format and extend in all variables definitions that appear after this point!",
            // "3.91) [^$Control | $CR | $LF] \u00D7         $Extend",
            "5) \u00D7 [$Format $Extend]",
            "# Do not break after full stop in certain contexts. [See note below.]",
            "# Do not break after ambiguous terminators like period, if immediately followed by a number or lowercase letter,",
            "# is between uppercase letters, or if the first following letter (optionally after certain punctuation) is lowercase.",
            "# For example, a period may be an abbreviation or numeric period, and not mark the end of a sentence.",
            "6) $ATerm         \u00D7         $Numeric",
            "7) ($Upper | $Lower) $ATerm         \u00D7         $Upper",
            "8) $ATerm $Close* $Sp*         \u00D7         $NotPreLower_* $Lower",
            "8.1) $SATerm $Close* $Sp*         \u00D7         ($SContinue | $SATerm)",
            "# Break after sentence terminators, but include closing punctuation, trailing spaces, and any paragraph separator. [See note below.] Include closing punctuation, trailing spaces, and (optionally) a paragraph separator.",
            "9) $SATerm $Close*         \u00D7         ( $Close | $Sp | $ParaSep )",
            "# Note the fix to $Sp*, $Sep?",
            "10) $SATerm $Close* $Sp*         \u00D7         ( $Sp | $ParaSep )",
            "11) $SATerm $Close* $Sp* $ParaSep? \u00F7",
            "#Otherwise, do not break",
            "998) \u00D7         $Any",
        },
        {
            "WordBreak",
            "$CR=\\p{Word_Break=CR}",
            "$LF=\\p{Word_Break=LF}",
            "$Newline=\\p{Word_Break=Newline}",
            // "$Control=\\p{Word_Break=Control}",
            "$Extend=\\p{Word_Break=Extend}",
            // "$NEWLINE=[$CR $LF \\u0085 \\u000B \\u000C \\u2028 \\u2029]",
            // "$Sep=\\p{Sentence_Break=Sep}",
            "# Now normal variables",
            "$Format=\\p{Word_Break=Format}",
            "$Katakana=\\p{Word_Break=Katakana}",
            "$ALetter=\\p{Word_Break=ALetter}",
            "$MidLetter=\\p{Word_Break=MidLetter}",
            "$MidNum=\\p{Word_Break=MidNum}",
            "$MidNumLet=\\p{Word_Break=MidNumLet}",
            "$Numeric=\\p{Word_Break=Numeric}",
            "$ExtendNumLet=\\p{Word_Break=ExtendNumLet}",
            "$RI=\\p{Word_Break=Regional_Indicator}",
            "$Hebrew_Letter=\\p{Word_Break=Hebrew_Letter}",
            "$Double_Quote=\\p{Word_Break=Double_Quote}",
            "$Single_Quote=\\p{Word_Break=Single_Quote}",

            //                "$E_Base=\\p{Word_Break=E_Base}",
            //                "$E_Modifier=\\p{Word_Break=E_Modifier}",
            "$ZWJ=\\p{Word_Break=ZWJ}",
            "$ExtPict=\\p{Extended_Pictographic}",

            // "$EBG=\\p{Word_Break=E_Base_GAZ}",
            // "$Glue_After_Zwj=\\p{Word_Break=Glue_After_Zwj}",

            "$WSegSpace=\\p{Word_Break=WSegSpace}",
            "# Macros",
            "$AHLetter=($ALetter | $Hebrew_Letter)",
            "$MidNumLetQ=($MidNumLet | $Single_Quote)",
            "# WARNING: For Rule 4: Fixes for GC, Format",
            // "# Subtract Format from Control, since we don't want to break before/after",
            // "$Control=[$Control-$Format]",
            "# Add format and extend to everything",
            "$FE=[$Format $Extend $ZWJ]",
            "# Special rules",
            "$NotBreak_=[^ $Newline $CR $LF ]",
            // "$FE= ($Extend | $Format)*",
            "$Katakana=($Katakana $FE*)",
            "$ALetter=($ALetter $FE*)",
            "$MidLetter=($MidLetter $FE*)",
            "$MidNum=($MidNum $FE*)",
            "$MidNumLet=($MidNumLet $FE*)",
            "$Numeric=($Numeric $FE*)",
            "$ExtendNumLet=($ExtendNumLet $FE*)",
            "$RI=($RI $FE*)",
            "$Hebrew_Letter=($Hebrew_Letter $FE*)",
            "$Double_Quote=($Double_Quote $FE*)",
            "$Single_Quote=($Single_Quote $FE*)",

            //                "$E_Base=($E_Base $FE*)",
            //                "$E_Modifier=($E_Modifier $FE*)",
            // "$ZWJ=($ZWJ $FE*)", don't do this one!
            // "$Glue_After_Zwj=($Glue_After_Zwj $FE*)",
            // "$EBG=($EBG $FE*)",

            "$AHLetter=($AHLetter $FE*)",
            "$MidNumLetQ=($MidNumLetQ $FE*)",
            "# Rules",
            "# Break at the start and end of text, unless the text is empty.",
            "# Do not break within CRLF.",
            "3) $CR          \u00D7          $LF",
            "# Otherwise break before and after Newlines (including CR and LF)",
            "3.1) ($Newline | $CR | $LF)        \u00F7",
            "3.2) \u00F7    ($Newline | $CR | $LF)",
            "# Do not break within emoji zwj sequences.",
            "3.3) $ZWJ × $ExtPict",
            "3.4) $WSegSpace × $WSegSpace",

            // "3.4) ( $Control | $CR | $LF )         \u00F7",
            // "3.5) \u00F7         ( $Control | $CR | $LF )",
            // "3.9) \u00D7         $Extend",
            // "3.91) [^$Control | $CR | $LF] \u00D7         $Extend",
            "# Ignore Format and Extend characters, except after sot, CR, LF, and Newline. (See Section 6.2, Replacing Ignore Rules.) This also has the effect of: Any × (Format | Extend)",
            "# WARNING: Implemented as don't break before format (except after linebreaks),",
            "# AND add format and extend in all variables definitions that appear after this point!",
            // "4) \u00D7 [$Format $Extend]",
            "4) $NotBreak_ \u00D7 [$Format $Extend $ZWJ]",
            "# Vanilla rules",
            "# Do not break between most letters.",
            "5) $AHLetter          \u00D7          $AHLetter",
            "# Do not break letters across certain punctuation.",
            "6) $AHLetter         \u00D7         ($MidLetter | $MidNumLetQ) $AHLetter",
            "7) $AHLetter ($MidLetter | $MidNumLetQ)         \u00D7         $AHLetter",
            "7.1) $Hebrew_Letter × $Single_Quote",
            "7.2) $Hebrew_Letter × $Double_Quote $Hebrew_Letter",
            "7.3) $Hebrew_Letter $Double_Quote × $Hebrew_Letter",
            "# Do not break within sequences of digits, or digits adjacent to letters (“3a”, or “A3”).",
            "8) $Numeric         \u00D7         $Numeric",
            "9) $AHLetter         \u00D7         $Numeric",
            "10) $Numeric         \u00D7         $AHLetter",
            "# Do not break within sequences, such as “3.2” or “3,456.789”.",
            "11) $Numeric ($MidNum | $MidNumLetQ)         \u00D7         $Numeric",
            "12) $Numeric         \u00D7         ($MidNum | $MidNumLetQ) $Numeric",
            "# Do not break between Katakana.",
            "13) $Katakana         \u00D7         $Katakana",
            "# Do not break from extenders.",
            "13.1) ($AHLetter | $Numeric | $Katakana | $ExtendNumLet)         \u00D7         $ExtendNumLet",
            "13.2) $ExtendNumLet         \u00D7         ($AHLetter | $Numeric | $Katakana)",

            // "# Do not break within emoji modifier sequences.",
            // "14) $E_Base × $E_Modifier",

            "# Do not break within emoji flag sequences. That is, do not break between regional indicator (RI) symbols if there is an odd number of RI characters before the break point.",
            "15) ^ ($RI $RI)* $RI × $RI",
            "16) [^$RI] ($RI $RI)* $RI × $RI",
            "# Otherwise, break everywhere (including around ideographs).",
        }
    };

    public static void main(String[] args) throws IOException {
        for (int i = 0; i < cannedRules.length; ++i) {
            String type = cannedRules[i][0];
            boolean hadHash = false;
            try (PrintWriter out =
                    FileUtilities.openUTF8Writer(
                            Settings.Output.GEN_DIR + "segmentation/", type + "Rules.txt")) {
                out.println("# Segmentation rules for " + type);
                out.println("#");
                out.println("# Character Classes");
                out.println("#");
                for (int j = 1; j < cannedRules[i].length; ++j) {
                    String cannedRule = cannedRules[i][j].trim();
                    if (cannedRule.equals("#")) {
                        continue;
                    }
                    boolean hasHash = cannedRule.startsWith("#");
                    if (hasHash && !hadHash) {
                        out.println("#");
                    }
                    out.println(cannedRule);
                    if (hasHash) {
                        out.println("#");
                    }
                    hadHash = hasHash;
                }
            }
        }

        try (PrintWriter out =
                FileUtilities.openUTF8Writer(
                        Settings.Output.GEN_DIR + "cldr/segmentation/", "rootAddon.xml")) {
            out.println(
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                            + "<!DOCTYPE ldml SYSTEM \"../../common/dtd/ldml.dtd\">\n"
                            + "<!--\n"
                            + "Copyright © 1991-"
                            + Default.getYear()
                            + " Unicode, Inc.\n"
                            + "CLDR data files are interpreted according to the LDML specification (https://unicode.org/reports/tr35/)\n"
                            + "For terms of use and license, see https://www.unicode.org/terms_of_use.html\n"
                            + "-->\n"
                            + "<ldml>\n"
                            + "\t<identity>\n"
                            + "\t\t<version number=\"$Revision: 13690 $\"/>\n"
                            + "\t\t<language type=\"root\"/>\n"
                            + "\t</identity>\n"
                            + "\t<segmentations>");
            for (final String type :
                    new String[] {
                        "GraphemeClusterBreak", "LineBreak", "SentenceBreak", "WordBreak"
                    }) {
                final Builder segBuilder =
                        Segmenter.make(ToolUnicodePropertySource.make(Default.ucdVersion()), type);
                out.print(segBuilder.toString(type, "\t\t"));
                if (type.equals("")) {
                    out.print(
                            "\t\t\t<suppressions type=\"standard\">\n"
                                    + "\t\t\t\t<!-- root suppression is empty. -->\n"
                                    + "\t\t\t</suppressions>\n");
                }
            }
            out.println("\t</segmentations>\n" + "</ldml>");
        }
    }
}
