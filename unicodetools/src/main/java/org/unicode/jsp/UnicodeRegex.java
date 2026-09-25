// ##header
/*
 *******************************************************************************
 * Copyright (C) 2009, Google, International Business Machines Corporation and *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package org.unicode.jsp;

import com.ibm.icu.text.StringTransform;
import com.ibm.icu.text.SymbolTable;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Freezable;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParsePosition;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Contains utilities to supplement the JDK Regex, since it doesn't handle Unicode well.
 *
 * @author markdavis
 */
public class UnicodeRegex implements Cloneable, Freezable, StringTransform {
    // Note: we don't currently have any state, but intend to in the future,
    // particularly for the regex style supported.

    SymbolTable symbolTable;
    ParsePosition parsePosition = new ParsePosition(0);

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    public UnicodeRegex setSymbolTable(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        return this;
    }

    /**
     * Adds full Unicode property support, with the latest version of Unicode, to Java Regex,
     * bringing it up to Level 1 (see http://www.unicode.org/reports/tr18/). It does this by
     * preprocessing the regex pattern string and interpreting the character classes (\p{...},
     * \P{...}, [...]) according to their syntax and meaning in UnicodeSet. With this utility, Java
     * regex expressions can be updated to work with the latest version of Unicode, and with all
     * Unicode properties. Note that the UnicodeSet syntax has not yet, however, been updated to be
     * completely consistent with Java regex, so be careful of the differences.
     *
     * <p>Not thread-safe; create a separate copy for different threads.
     *
     * <p>In the future, we may extend this to support other regex packages.
     *
     * @regex A modified Java regex pattern, as in the input to Pattern.compile(), except that all
     *     "character classes" are processed as if they were UnicodeSet patterns. Example:
     *     "abc[:bc=N:]. See UnicodeSet for the differences in syntax.
     * @return A processed Java regex pattern, suitable for input to Pattern.compile().
     */
    public String transform(String regex) {
        return transform(regex, 0);
    }

    /**
     * Like {@link #transform(String)}, using the flags that will be passed to {@link
     * Pattern#compile(String, int)}. Comments and ignored whitespace must be removed before
     * expanding UnicodeSets, while literal characters introduced by expansion must be preserved.
     */
    public String transform(String regex, int flags) {
        if ((flags & Pattern.LITERAL) != 0) {
            return regex;
        }
        regex = removeComments(escapeQuotedText(regex), flags);
        StringBuilder result = new StringBuilder();
        UnicodeSet temp = new UnicodeSet();
        ParsePosition pos = new ParsePosition(0);
        for (int i = 0; i < regex.length(); ++i) {
            char ch = regex.charAt(i);
            if ((ch == '\\' || ch == '[') && UnicodeSet.resemblesPattern(regex, i)) {
                i = processSet(regex, i, result, temp, pos);
                continue;
            }
            result.append(ch);
            if (ch == '\\' && i + 1 < regex.length()) {
                result.append(regex.charAt(++i));
            }
        }
        return result.toString();
    }

    /**
     * Convenience static function, using standard parameters.
     *
     * @param regex as in process()
     * @return processed regex pattern, as in process()
     */
    public static String fix(String regex) {
        return STANDARD.transform(regex);
    }

    /** Like {@link #fix(String)}, with the flags used to compile the result. */
    public static String fix(String regex, int flags) {
        return STANDARD.transform(regex, flags);
    }

    /**
     * Compile a regex string, after processing by fix(...).
     *
     * @param regex Raw regex pattern, as in fix(...).
     * @return Pattern
     */
    public static Pattern compile(String regex) {
        return Pattern.compile(STANDARD.transform(regex));
    }

    /**
     * Compile a composed string from a set of BNF lines; see the List version for more information.
     *
     * @param bnfLines Series of BNF lines.
     * @return Pattern
     */
    public String compileBnf(String bnfLines) {
        return compileBnf(Arrays.asList(bnfLines.split("\\r\\n?|\\n")));
    }

    /**
     * Compile a composed string from a set of BNF lines, such as for composing a regex expression.
     * The lines can be in any order, but there must not be any cycles. The result can be used as
     * input for fix().
     *
     * <p>Example:
     *
     * <pre>
     * uri = (?: (scheme) \\:)? (host) (?: \\? (query))? (?: \\u0023 (fragment))?;
     * scheme = reserved+;
     * host = // reserved+;
     * query = [\\=reserved]+;
     * fragment = reserved+;
     * reserved = [[:Block=ASCII:][:alphabetic:]];
     * </pre>
     *
     * <p>Caveats: at this point the parsing is simple; for example, # cannot be quoted (use
     * \\u0023); you can set it to null to disable. The equality sign and a few others can be reset
     * with setBnfX().
     *
     * @param bnfLines Series of lines that represent a BNF expression. The lines contain a series
     *     of statements that of the form x=y;. A statement can take multiple lines, but there can't
     *     be multiple statements on a line. A hash quotes to the end of the line.
     * @return Pattern
     */
    public String compileBnf(List<String> lines) {
        Map<String, String> variables = getVariables(lines);
        Set<String> unused = new LinkedHashSet<String>(variables.keySet());
        // brute force replacement; do twice to allow for different order
        // later on can optimize
        for (int i = 0; i < 2; ++i) {
            for (String variable : variables.keySet()) {
                String definition = variables.get(variable);
                for (String variable2 : variables.keySet()) {
                    if (variable.equals(variable2)) {
                        continue;
                    }
                    String definition2 = variables.get(variable2);
                    String altered2 = definition2.replace(variable, definition);
                    if (!altered2.equals(definition2)) {
                        unused.remove(variable);
                        variables.put(variable2, altered2);
                        if (log != null) {
                            try {
                                log.append(variable2 + "=" + altered2 + ";");
                            } catch (IOException e) {
                                throw (IllegalArgumentException)
                                        new IllegalArgumentException().initCause(e);
                            }
                        }
                    }
                }
            }
        }
        if (unused.size() != 1) {
            throw new IllegalArgumentException("Not a single root: " + unused);
        }
        return variables.get(unused.iterator().next());
    }

    /**
     * Compile a regex string, after processing by fix(...).
     *
     * @param regex Raw regex pattern, as in fix(...).
     * @return Pattern
     */
    public static Pattern compile(String regex, int options) {
        return Pattern.compile(STANDARD.transform(regex, options), options);
    }

    public String getBnfCommentString() {
        return bnfCommentString;
    }

    public void setBnfCommentString(String bnfCommentString) {
        this.bnfCommentString = bnfCommentString;
    }

    public String getBnfVariableInfix() {
        return bnfVariableInfix;
    }

    public void setBnfVariableInfix(String bnfVariableInfix) {
        this.bnfVariableInfix = bnfVariableInfix;
    }

    public String getBnfLineSeparator() {
        return bnfLineSeparator;
    }

    public void setBnfLineSeparator(String bnfLineSeparator) {
        this.bnfLineSeparator = bnfLineSeparator;
    }

    /**
     * Utility for loading lines from a UTF8 file.
     *
     * @param file
     * @param result
     * @return
     * @throws IOException
     */
    public static List<String> loadFile(String file, List<String> result) throws IOException {
        BufferedReader in =
                new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
        while (true) {
            String line = in.readLine();
            if (line == null) {
                break;
            }
            result.add(line);
        }
        return result;
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.util.Freezable#cloneAsThawed()
     */
    public Object cloneAsThawed() {
        // TODO Auto-generated method stub
        try {
            return clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalArgumentException(); // should never happen
        }
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.util.Freezable#freeze()
     */
    public Object freeze() {
        // no action needed now.
        return this;
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.util.Freezable#isFrozen()
     */
    public boolean isFrozen() {
        // at this point, always true
        return true;
    }

    // ===== PRIVATES =====

    // Java processes quoting before comments. Hex escapes also let quoted text appear in a
    // UnicodeSet, whose parser does not recognize Java's \Q...\E syntax.
    private static String escapeQuotedText(String regex) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < regex.length(); ++i) {
            char ch = regex.charAt(i);
            if (ch == '\\' && i + 1 < regex.length()) {
                char escaped = regex.charAt(++i);
                if (escaped == 'Q') {
                    int end = regex.indexOf("\\E", i + 1);
                    if (end < 0) {
                        end = regex.length();
                    }
                    for (++i; i < end; ) {
                        int cp = regex.codePointAt(i);
                        // Even a quoted line separator ends a preceding comment in Java.
                        if (cp == 0
                                || cp == '\n'
                                || cp == '\r'
                                || cp == 0x85
                                || cp == 0x2028
                                || cp == 0x2029) {
                            result.append('\\').appendCodePoint(cp);
                        } else {
                            result.append("\\x{").append(Integer.toHexString(cp)).append('}');
                        }
                        i += Character.charCount(cp);
                    }
                    i = end < regex.length() ? end + 1 : end;
                } else {
                    result.append(ch).append(escaped);
                }
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    private static String removeComments(String regex, int flags) {
        StringBuilder result = new StringBuilder();
        Deque<Integer> groupFlags = new ArrayDeque<>();
        int setDepth = 0;
        // Parentheses and brackets in UnicodeSet string literals do not affect regex scopes.
        boolean inString = false;
        for (int i = 0; (i = skipIgnored(regex, i, flags)) < regex.length(); ++i) {
            char ch = regex.charAt(i);
            if (ch == '\\' && i + 1 < regex.length()) {
                result.append(ch);
                char escaped = regex.charAt(++i);
                result.append(escaped);
                if ("pPN".indexOf(escaped) >= 0
                        && i + 1 < regex.length()
                        && regex.charAt(i + 1) == '{') {
                    // Property and character names have their own syntax (including spaces).
                    int end = regex.indexOf('}', i + 2);
                    if (end >= 0) {
                        result.append(regex, i + 1, end + 1);
                        i = end;
                    }
                }
                continue;
            }
            if (!inString && regex.startsWith("[:", i)) {
                int end = regex.indexOf(":]", i + 2);
                if (end >= 0) {
                    result.append(regex, i, end + 2);
                    i = end + 1;
                    continue;
                }
            }
            if (setDepth > 0 && ch == '{') {
                inString = true;
            } else if (inString) {
                if (ch == '}') {
                    inString = false;
                }
            } else if (ch == '[') {
                ++setDepth;
            } else if (ch == ']' && setDepth > 0) {
                --setDepth;
            } else if (setDepth == 0 && ch == '(') {
                groupFlags.push(flags);
                int j = skipIgnored(regex, i + 1, flags);
                if (j < regex.length() && regex.charAt(j) == '?') {
                    StringBuilder prefix = new StringBuilder("(?");
                    int newFlags = flags;
                    boolean enable = true;
                    boolean foundFlags = false;
                    for (++j; (j = skipIgnored(regex, j, newFlags)) < regex.length(); ++j) {
                        char flag = regex.charAt(j);
                        if (flag == '-' && enable) {
                            enable = false;
                        } else if ("cdimsuUx".indexOf(flag) >= 0) {
                            // Only these two flags affect comment parsing.
                            int mask =
                                    flag == 'x'
                                            ? Pattern.COMMENTS
                                            : flag == 'd' ? Pattern.UNIX_LINES : 0;
                            newFlags = enable ? newFlags | mask : newFlags & ~mask;
                        } else {
                            if (flag == ':' || flag == ')') {
                                result.append(prefix).append(flag);
                                flags = newFlags;
                                i = j;
                                if (flag == ')') {
                                    // An unscoped flag change lasts until the enclosing group ends.
                                    groupFlags.pop();
                                }
                                foundFlags = true;
                            }
                            break;
                        }
                        prefix.append(flag);
                    }
                    if (foundFlags) {
                        continue;
                    }
                }
            } else if (setDepth == 0 && ch == ')' && !groupFlags.isEmpty()) {
                flags = groupFlags.pop();
            }
            result.append(ch);
        }
        return result.toString();
    }

    private static int skipIgnored(String regex, int i, int flags) {
        if ((flags & Pattern.COMMENTS) == 0) {
            return i;
        }
        while (i < regex.length()) {
            char ch = regex.charAt(i);
            // Java COMMENTS uses ASCII whitespace, not UnicodeSet.IGNORE_SPACE.
            if (ch == ' ' || (ch >= '\t' && ch <= '\r')) {
                ++i;
            } else if (ch == '#') {
                while (++i < regex.length()) {
                    ch = regex.charAt(i);
                    if (ch == '\n'
                            || ch == 0
                            || ((flags & Pattern.UNIX_LINES) == 0
                                    && (ch == '\r'
                                            || ch == '\u0085'
                                            || ch == '\u2028'
                                            || ch == '\u2029'))) {
                        break;
                    }
                }
            } else {
                break;
            }
        }
        return i;
    }

    private int processSet(
            String regex, int i, StringBuilder result, UnicodeSet temp, ParsePosition pos) {
        try {
            pos.setIndex(i);
            UnicodeSet x = temp.clear().applyPattern(regex, pos, symbolTable, 0);
            x.complement().complement(); // hack to fix toPattern
            // Input comments have already been removed. Escape literal # characters introduced
            // by expansion, since they would otherwise start comments in Java's COMMENTS mode.
            // https://github.com/unicode-org/unicodetools/issues/420
            result.append(x.toPattern(false).replace("#", "\\x{23}"));
            i = pos.getIndex() - 1; // allow for the loop increment
            return i;
        } catch (Exception e) {
            throw (IllegalArgumentException)
                    new IllegalArgumentException("Error in " + regex).initCause(e);
        }
    }

    private static UnicodeRegex STANDARD = new UnicodeRegex();
    private String bnfCommentString = "#";
    private String bnfVariableInfix = "=";
    private String bnfLineSeparator = "\n";
    private Appendable log = null;

    private Comparator<String> LongestFirst =
            new Comparator<String>() {
                public int compare(String arg0, String arg1) {
                    int len0 = arg0.length();
                    int len1 = arg1.length();
                    if (len0 != len1) {
                        return len1 - len0;
                    }
                    return arg0.compareTo(arg1);
                }
            };

    private Map<String, String> getVariables(List<String> lines) {
        Map<String, String> variables = new TreeMap<String, String>(LongestFirst);
        String variable = null;
        StringBuffer definition = new StringBuffer();
        int count = 0;
        for (String line : lines) {
            ++count;
            // remove initial bom, comments
            if (line.length() == 0) {
                continue;
            }
            if (line.charAt(0) == '\uFEFF') {
                line = line.substring(1);
            }

            if (bnfCommentString != null) {
                int hashPos = line.indexOf(bnfCommentString);
                if (hashPos >= 0) {
                    line = line.substring(0, hashPos);
                }
            }
            String trimline = line.trim();
            if (trimline.length() == 0) {
                continue;
            }

            // String[] lineParts = line.split(";");
            String linePart = line; // lineParts[i]; // .trim().replace("\\s+", " ");
            if (linePart.trim().length() == 0) {
                continue;
            }
            boolean terminated = trimline.endsWith(";");
            if (terminated) {
                linePart = linePart.substring(0, linePart.lastIndexOf(';'));
            }
            int equalsPos = linePart.indexOf(bnfVariableInfix);
            if (equalsPos >= 0) {
                if (variable != null) {
                    throw new IllegalArgumentException("Missing ';' before " + count + ") " + line);
                }
                variable = linePart.substring(0, equalsPos).trim();
                if (variables.containsKey(variable)) {
                    throw new IllegalArgumentException("Duplicate variable definition in " + line);
                }
                definition.append(linePart.substring(equalsPos + 1).trim());
            } else { // no equals, so
                if (variable == null) {
                    throw new IllegalArgumentException("Missing '=' at " + count + ") " + line);
                }
                definition.append(bnfLineSeparator).append(linePart);
            }
            // we are terminated if i is not at the end, or the line ends with a ;
            if (terminated) {
                variables.put(variable, definition.toString());
                variable = null; // signal we have no variable
                definition.setLength(0);
            }
        }
        if (variable != null) {
            throw new IllegalArgumentException("Missing ';' at end");
        }
        return variables;
    }
}
