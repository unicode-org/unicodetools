package org.unicode.tools;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.ibm.icu.impl.UnicodeMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UProperty.NameChoice;
import com.ibm.icu.text.StringTransform;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.EntryRange;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Stack;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.unicode.cldr.util.TransliteratorUtilities;
import org.unicode.text.utility.Utility;

public class CheckLinkification {
    public static void main(String[] args) {
        new CheckLinkification().check();
    }

    private void check() {
        checkLinkification();
        checkOverlap();
        checkMinimumEscaping();
        if (true) return;
        showLinkTermination();
        showLinkPairedOpeners();
    }

    public enum LinkTermination {
        Include("[\\p{ANY}]"), // overridden by following
        Hard("[\\p{whitespace}\\p{NChar}\\p{C}]"),
        Soft("[\\p{Term}[‘-‛ ‹ › \"“-‟ « »']]"),
        Close("[\\p{Bidi_Paired_Bracket_Type=Close}[>]]"),
        Open("[\\p{Bidi_Paired_Bracket_Type=Open}[<]]"),
        ;

        final UnicodeSet base;

        private LinkTermination(String uset) {
            this.base = new UnicodeSet(uset).freeze();
        }

        static final UnicodeMap<LinkTermination> Property = new UnicodeMap<>();

        static {
            for (LinkTermination lt : values()) {
                Property.putAll(lt.base, lt);
            }
            Property.freeze();
        }
    }

    void checkOverlap() {
        for (LinkTermination lt : LinkTermination.values()) {
            if (lt == lt.Include) {
                continue;
            }
            UnicodeSet propValue = lt.Property.getSet(lt);
            if (!propValue.equals(lt.base)) {
                System.out.print("Overlap");
            }
        }
    }

    void showLinkTermination() {
        for (LinkTermination lt : LinkTermination.values()) {
            UnicodeSet value = LinkTermination.Property.getSet(lt);
            String name = lt.toString();
            System.out.println("\n#\tLink_Termination=" + name);
            if (lt == lt.Include) {
                System.out.println("#   " + "(All code points without other values)");
                continue;
            } else {
                System.out.println("#   draft = " + lt.base);
            }
            if (lt == LinkTermination.Hard) {
                value.removeAll(new UnicodeSet("[\\p{Cn}\\p{Cs}]"));
                System.out.println("#   (not listing Unassigned or Surrogates)");
            }
            System.out.println();
            for (EntryRange range : value.ranges()) {
                final String rangeString =
                        Utility.hex(range.codepoint)
                                + (range.codepoint == range.codepointEnd
                                        ? ""
                                        : ".." + Utility.hex(range.codepointEnd));
                System.out.println(
                        rangeString
                                + ";"
                                + " ".repeat(15 - rangeString.length())
                                + lt
                                + "\t# "
                                + "("
                                + getGeneralCategory(
                                        UProperty.GENERAL_CATEGORY,
                                        range.codepoint,
                                        NameChoice.SHORT)
                                + ") "
                                + quote(UCharacter.getExtendedName(range.codepoint))
                                + (range.codepoint == range.codepointEnd
                                        ? ""
                                        : ".."
                                                + "("
                                                + getGeneralCategory(
                                                        UProperty.GENERAL_CATEGORY,
                                                        range.codepointEnd,
                                                        NameChoice.SHORT)
                                                + ") "
                                                + quote(
                                                        UCharacter.getExtendedName(
                                                                range.codepointEnd))));
            }
            System.out.println();
        }
    }

    public String getGeneralCategory(int property, int codePoint, int nameChoice) {
        //        return UCharacter.getStringPropertyValue(
        //                UProperty.GENERAL_CATEGORY, codePoint, NameChoice.SHORT);
        return UCharacter.getPropertyValueName(
                property, UCharacter.getIntPropertyValue(codePoint, property), nameChoice);
    }

    static String quote(String s) {
        return TransliteratorUtilities.toHTML.transform(s);
    }

    void showLinkPairedOpeners() {
        UnicodeSet value = LinkTermination.Property.getSet(LinkTermination.Close);

        System.out.println("\n#\tLink_Paired_Opener");
        System.out.println(
                "#   draft = BidiPairedBracket + (“&gt;” GREATER-THAN SIGN 🡆  “&lt;” LESS-THAN SIGN)");
        System.out.println();

        for (String cpString : value) {
            int cp = cpString.codePointAt(0);
            String hex = Utility.hex(cp);
            final int value2 = getOpening(cp);
            System.out.println(
                    hex
                            + ";"
                            + " ".repeat(7 - hex.length())
                            + Utility.hex(value2)
                            + "\t#"
                            + " “"
                            + quote(UTF16.valueOf(cp))
                            + "” "
                            + UCharacter.getExtendedName(cp)
                            + " 🡆 "
                            + " “"
                            + quote(UTF16.valueOf(value2))
                            + "” "
                            + UCharacter.getExtendedName(value2));
        }
    }

    public int getOpening(int cp) {
        return cp == '>' ? '<' : UCharacter.getBidiPairedBracket(cp);
    }

    public enum Part {
        PATH('/', "[?#]"),
        QUERY('?', "[#]"),
        FRAGMENT('#', "[]");
        final int initiator;
        final UnicodeSet terminators;

        private Part(char initiator, String terminators) {
            this.initiator = initiator;
            this.terminators = new UnicodeSet(terminators);
        }

        static Part fromInitiator(int cp) {
            for (Part part : Part.values()) {
                if (part.initiator == cp) {
                    return part;
                }
            }
            return null;
        }
    }

    /**
     * Set lastSafe to 0 — this marks the last code point that is definitely included in the
     * linkification.<br>
     * Set closingStack to empty<br>
     * Set the current code point position i to 0<br>
     * Loop from i = 0 to n<br>
     * Set LT to LinkTermination(cp[i])<br>
     * If LT == none, set lastSafe to be i+1, continue loop<br>
     * If LT == soft, continue loop<br>
     * If LT == hard, stop linkification and return lastSafe<br>
     * If LT == opening, push cp[i] onto closingStack<br>
     * If LT == closing, set open to the pop of closingStack, or 0 if the closingStack is empty<br>
     * If LinkPairedOpeners(cp[i]) == open, set lastSafe to be i+1, continue loop.<br>
     * Otherwise, stop linkification and return lastSafe<br>
     * If lastSafe == n+1, then the entire part is safe; continue to the next part<br>
     * Otherwise, stop linkification and return lastSafe<br>
     */
    int parsePathQueryFragment(String source, int codePointOffset) {
        int[] codePoints = source.codePoints().toArray();
        int lastSafe = codePointOffset;
        Part part = null;
        Stack<Integer> openingStack = new Stack<>();
        for (int i = codePointOffset; i < codePoints.length; ++i) {
            int cp = codePoints[i];
            if (part == null) {
                part = Part.fromInitiator(cp);
                if (part == null) {
                    return i; // failed, don't move cursor
                }
                lastSafe = i + 1;
                continue;
            }
            
            LinkTermination lt = LinkTermination.Property.get(cp);
            switch (lt) {
                case Include:
                    if (part.terminators.contains(cp)) {
                        lastSafe = i;
                        part = Part.fromInitiator(cp);
                        if (part == null) {
                            return lastSafe;
                        }
                    }
                    lastSafe = i + 1;
                    break;
                case Soft: // no action
                    break;
                case Hard:
                    return lastSafe;
                case Open:
                    openingStack.push(cp);
                    lastSafe = i + 1;
                    break;
                case Close:
                    if (openingStack.empty()) {
                        return lastSafe;
                    }
                    int matchingOpening = getOpening(cp);
                    Integer topOfStack = openingStack.pop();
                    if (matchingOpening == topOfStack) {
                        lastSafe = i + 1;
                        break;
                    } // else failed to match
                    return lastSafe;
            }
        }
        return codePoints.length;
    }

    /**
     * Minimally escape. Presumes that the parts have had necessary interior quoting.<br>
     * For example, a path
     */
    public String minimalEscape(NavigableMap<Part, String> parts) {
        StringBuilder output = new StringBuilder();
        // get the last part
        List<Entry<Part, String>> ordered = List.copyOf(parts.entrySet());
        Part lastPart = ordered.get(ordered.size() - 1).getKey();
        // process all parts
        for (Entry<Part, String> partEntry : ordered) {
            Part part = partEntry.getKey();
            int[] cps = partEntry.getValue().codePoints().toArray();
            int n = cps.length;
            output.appendCodePoint(part.initiator);
            int copiedAlready = 0;
            Stack<Integer> openingStack = new Stack<>();
            for (int i = 0; i < n; ++i) {
                final int cp = cps[i];
                LinkTermination lt = part.terminators.contains(cp) ? LinkTermination.Hard :
                        LinkTermination.Property.get(cp);
                switch (lt) {
                    case Include:
                        appendCodePointsBetween(output, cps, copiedAlready, i);
                        output.appendCodePoint(cp);
                        copiedAlready = i + 1;
                        break;
                    case Hard:
                        appendCodePointsBetween(output, cps, copiedAlready, i);
                        appendPercentEscaped(output, cp);
                        copiedAlready = i + 1;
                        continue;
                    case Soft: // fix
                        continue;
                    case Open:
                        openingStack.push(cp);
                        appendCodePointsBetween(output, cps, copiedAlready, i);
                        output.appendCodePoint(cp);
                        copiedAlready = i + 1;
                        continue; // fix
                    case Close: // fix
                        if (openingStack.empty()) {
                            appendCodePointsBetween(output, cps, copiedAlready, i);
                            appendPercentEscaped(output, cp);
                        } else {
                            Integer topOfStack = openingStack.pop();
                            int matchingOpening = getOpening(cp);
                            if (matchingOpening == topOfStack) {
                                appendCodePointsBetween(output, cps, copiedAlready, i);
                                output.appendCodePoint(cp);
                            } else { // failed to match
                                appendCodePointsBetween(output, cps, copiedAlready, i);
                                appendPercentEscaped(output, cp);
                            }
                        }
                        copiedAlready = i + 1;
                        continue;
                    default:
                        throw new IllegalArgumentException();
                }
            } // fix
            if (part != lastPart) {
                appendCodePointsBetween(output, cps, copiedAlready, n);
            } else if (copiedAlready < n) {
                appendCodePointsBetween(output, cps, copiedAlready, n - 1);
                appendPercentEscaped(output, cps[n - 1]);
            }
        }
        return output.toString();
    }

    private void appendPercentEscaped(StringBuilder output, int cp) {
        output.append('%');
        byte[] bytes = Character.toString(cp).getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < bytes.length; ++i) {
            output.append(Utility.hex(bytes[i]));
        }
    }

    private void appendCodePointsBetween(
            StringBuilder output, int[] cp, int copyEnd, int notToCopy) {
        for (int i = copyEnd; i < notToCopy; ++i) {
            output.appendCodePoint(cp[i]);
        }
    }

    static final char LINKIFY_START = '⸠';
    static final char LINKIFY_END = '⸡';

    /* The following is very temporary, just during the spec development. */

    /** TODO: extract test later */
    public void checkLinkification() {
        String[][] tests = {
            {"!", "!"},
            {"/avg", "/avg⸡"},
            {"?avg", "?avg⸡"},
            {"#avg", "#avg⸡"},
            // complex
            {"/avg/dez?thik#lmn", "/avg/dez?thik#lmn⸡"},
            // soft vs hard
            {"/avg/dez?d.ef#lmn", "/avg/dez?d.ef#lmn⸡"},
            {"/avg/dez?d ef#lmn", "/avg/dez?d⸡ ef#lmn", "Break on hard (' ')"},
            {"/avg/dez?d. ef#lmn", "/avg/dez?d⸡. ef#lmn", "Break on soft ('.') followed by hard (' ')"},
            // ordering
            {"/a/vg?d/e?z#l/m?n#p", "/a/vg?d/e?z#l/m?n#p⸡"},
            // opening/closing
            {"/av)", "/av⸡)", "Break on unmatched bracket"},
            {"/a(v)", "/a(v)⸡", "Include matched bracket"},
            {"/av(g/d)rs?thik#lmn", "/av(g/d)rs?thik#lmn⸡", "Includes matching across interior syntax — consider changing"},
        };
        List<List<String>> testLines = new ArrayList<>();
        for (String[] test : tests) {
            for (StringTransform alt : ALTS) {
                if (alt != null) { // generate alt version
                    String comment = test.length < 3 ? null : test[2]; // save
                  test =
                            Arrays.asList(test).stream()
                                    .map(x -> alt.transform(x))
                                    .collect(Collectors.toList())
                                    .toArray(new String[test.length]);
                  if (comment != null) {
                      test[2] = comment;
                  }
                 testLines.add(Arrays.asList(test));
                }

                String source = test[0];
                String expected = test[1];
                int parseResult = parsePathQueryFragment(source, 0);
                String actual =
                        parseResult == 0
                                ? source
                                : source.substring(0, parseResult)
                                        + LINKIFY_END
                                        + source.substring(parseResult);
                tempAssertEquals(source.toString(), expected, actual);
            }
        }
        System.out.println(
                "\n@Linkification\n"
                        + "# Field 0: Source\n" //
                        + "# Field 1: Expected Linkification, where:\n\t"
                        + LINKIFY_START
                        + " is at the start, and \n\t"
                        + LINKIFY_END
                        + " is at the end" //
                        + "\n");
        for (List<String> testLine : testLines) {
            System.out.println("See example.com" + testLine.get(0) + " on…;\tSee " + LINKIFY_START + "example.com" + testLine.get(1) + " on…" + (testLine.size() == 2 ? "" : "\t# " + testLine.get(2)));
        }
    }

    static final StringTransform[] ALTS = {
        null, Transliterator.getInstance("[a-z] Latin-Greek/UNGEGN")
    };

    /** TODO: extract test later */
    public void checkMinimumEscaping() {
        System.out.println();
        String[][] tests = {
                {"a", "", "", "/a", "Path only"},
                {"", "a", "", "?a", "Query only"},
                {"", "", "a", "#a", "Fragment only"},
                {"avg/dez", "th=ikl&m=nxo", "prs", "/avg/dez?th=ikl&m=nxo#prs", "All parts"},
           {"a?b", "", "", "/a%3Fb", "Escape ? in Path"},
            {"a#v", "g=d#e", "", "/a%23v?g=d%23e", "Escape # in Path/Query"},
            {"av g/dez", "th=ik l&=nxo", "pr s", "/av%20g/dez?th=ik%20l&=nxo#pr%20s", "Escape hard (' ')"},
            {"avg./dez.", "th=ik.l&=nxo.", "prs.", "/avg./dez.?th=ik.l&=nxo.#prs%2E", "Escape soft ('.') unless followed by include"},
            {"a(v))", "g(d))", "e(z))", "/a(v)%29?g(d)%29#e(z)%29", "Escape unmatched brackets"},
        };
        List<List<String>> testLines = new ArrayList<>();
        int line = 0;
        for (String[] test : tests) {
            ++line;
            for (StringTransform alt : ALTS) {
                if (alt != null) { // generate alt version
                    String comment = test.length < 5 ? null : test[4]; // save
                    test =
                            Arrays.asList(test).stream()
                                    .map(x -> alt.transform(x))
                                    .collect(Collectors.toList())
                                    .toArray(new String[test.length]);
                    if (comment != null) {
                        test[4] = comment;
                    }
                    testLines.add(Arrays.asList(test));
                }
                // produce a map, ignoring null values
                int j = 0;
                TreeMap<Part, String> source = new TreeMap<>();
                for (Part part : List.of(Part.PATH, Part.QUERY, Part.FRAGMENT)) {
                    if (!test[j].isEmpty()) {
                        source.put(part, test[j]);
                    }
                    j++;
                }
                // check
                final String expected = test[3];
                final String actual = minimalEscape(source);
                tempAssertEquals(line + ") " + source.toString() , expected, actual);
            }
        }
        System.out.println(
                "\n@Minimal-Escaping\n"
                        + "# Field 0: Domain\n"
                        + "# Field 1: Path\n"
                        + "# Field 2: Query\n"
                        + "# Field 3: Fragment\n"
                        + "# Field 4: Expected result\n");
        for (List<String> testLine : testLines) {
            System.out.println(
                    "https://example.com;\t"
                            + Joiner.on(";\t").join(testLine.subList(0, 3))
                            + ";\thttps://example.com"
                            + testLine.get(3)
                            + (testLine.size() < 5 ? "" : "\t# " + testLine.get(4)));
        }
    }

    private static final String SPLIT1 = "\t"; // for debugging, "\n";

    public <T> void tempAssertEquals(String message, T expected, T actual) {
        System.out.println(
                (Objects.equal(expected, actual) ? "OK" : "ERROR")
                        + " "
                        + message
                        + SPLIT1
                        + "expected:\t"
                        + expected
                        + SPLIT1
                        + "actual:  \t"
                        + actual);
    }
}
