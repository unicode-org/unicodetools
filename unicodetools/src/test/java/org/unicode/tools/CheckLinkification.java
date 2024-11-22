package org.unicode.tools;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSortedMap;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.UnicodeMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UProperty.NameChoice;
import com.ibm.icu.text.StringTransform;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.EntryRange;
import com.ibm.icu.text.UnicodeSet.SpanCondition;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Stack;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.TransliteratorUtilities;
import org.unicode.text.utility.Utility;

public class CheckLinkification {
    private static final Joiner JOIN_TAB = Joiner.on('\t');

    public static void main(String[] args) throws IOException {
        new CheckLinkification().check();
    }

    private void check() throws IOException {
        checkParts();
        checkUnescape();
        checkTestFile();
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

    public static int getOpening(int cp) {
        return cp == '>' ? '<' : UCharacter.getBidiPairedBracket(cp);
    }

    public enum Part {
        PROTOCOL('\u0000', "[{//}]", "[]"),
        HOST('\u0000', "[/?#]", "[]"),
        PATH('/', "[?#]", "[/]"),
        QUERY('?', "[#]", "[=\\&]"),
        FRAGMENT('#', "[]", "[]");
        final int initiator;
        final UnicodeSet terminators;
        final UnicodeSet interior;
        final UnicodeSet interiorAndTerminators;

        private Part(char initiator, String terminators, String interior) {
            this.initiator = initiator;
            this.terminators = new UnicodeSet(terminators).freeze();
            this.interior = new UnicodeSet(interior).freeze();
            this.interiorAndTerminators =
                    new UnicodeSet(this.interior).addAll(this.terminators).freeze();
        }

        static Part fromInitiator(int cp) {
            for (Part part : Part.values()) {
                if (part.initiator == cp) {
                    return part;
                }
            }
            return null;
        }

        /**
         * Pull apart a URL string into Parts. <br>
         * TODO: unescape the %escapes.
         *
         * @param source
         * @param unescape TODO
         * @return
         */
        static NavigableMap<Part, String> getParts(String source, boolean unescape) {
            Map<Part, String> result = new HashMap<>();
            // quick and dirty
            int partStart = 0;
            int partEnd;
            main:
            for (Part part : Part.values()) {
                switch (part) {
                    case PROTOCOL:
                        partEnd = source.indexOf("://");
                        if (partEnd > 0) {
                            partEnd += 3;
                            result.put(Part.PROTOCOL, source.substring(0, partEnd));
                            partStart = partEnd;
                        }
                        break;
                    default:
                        partEnd =
                                part.terminators.span(
                                        source, partStart, SpanCondition.NOT_CONTAINED);
                        if (partStart != partEnd) {
                            result.put(part, part.unescape(source.substring(partStart, partEnd)));
                        }
                        if (partEnd == source.length()) {
                            break main;
                        }
                        partStart = partEnd;
                        break;
                }
            }
            return ImmutableSortedMap.copyOf(result);
        }

        /**
         * Unescape a part. But don't unescape interior characters or terminators because they are
         * content! For example "a/b%2Fc" as a path should not be turned into a/b/c, because that
         * b/c is a path-part.
         *
         * @param substring
         * @return
         */
        String unescape(String substring) {
            return CheckLinkification.unescape(substring, interiorAndTerminators);
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
     *
     * @param escapedCounter TODO
     */
    public static String minimalEscape(
            NavigableMap<Part, String> parts, Counter<Integer> escapedCounter) {
        StringBuilder output = new StringBuilder();
        // get the last part
        List<Entry<Part, String>> ordered = List.copyOf(parts.entrySet());
        Part lastPart = ordered.get(ordered.size() - 1).getKey();
        // process all parts
        for (Entry<Part, String> partEntry : ordered) {
            Part part = partEntry.getKey();
            final String string = partEntry.getValue();
            if (string.isEmpty()) {
                throw new IllegalArgumentException();
            }
            if (part == Part.HOST || part == Part.PROTOCOL) {
                output.append(string);
                continue;
            }
            int[] cps = string.codePoints().toArray();
            int n = cps.length;
            if (cps[0] != part.initiator) {
                output.appendCodePoint(part.initiator);
            }
            ;
            int copiedAlready = 0;
            Stack<Integer> openingStack = new Stack<>();
            for (int i = 0; i < n; ++i) {
                final int cp = cps[i];
                LinkTermination lt =
                        part.terminators.contains(cp)
                                ? LinkTermination.Hard
                                : LinkTermination.Property.get(cp);
                switch (lt) {
                    case Include:
                        appendCodePointsBetween(output, cps, copiedAlready, i);
                        output.appendCodePoint(cp);
                        copiedAlready = i + 1;
                        break;
                    case Hard:
                        appendCodePointsBetween(output, cps, copiedAlready, i);
                        appendPercentEscaped(output, cp, escapedCounter);
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
                            appendPercentEscaped(output, cp, escapedCounter);
                        } else {
                            Integer topOfStack = openingStack.pop();
                            int matchingOpening = getOpening(cp);
                            if (matchingOpening == topOfStack) {
                                appendCodePointsBetween(output, cps, copiedAlready, i);
                                output.appendCodePoint(cp);
                            } else { // failed to match
                                appendCodePointsBetween(output, cps, copiedAlready, i);
                                appendPercentEscaped(output, cp, escapedCounter);
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
                appendPercentEscaped(output, cps[n - 1], escapedCounter);
            }
        }
        return output.toString();
    }

    private static void appendCodePointsBetween(
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
            {
                "/avg/dez?d. ef#lmn",
                "/avg/dez?d⸡. ef#lmn",
                "Break on soft ('.') followed by hard (' ')"
            },
            // ordering
            {"/a/vg?d/e?z#l/m?n#p", "/a/vg?d/e?z#l/m?n#p⸡"},
            // opening/closing
            {"/av)", "/av⸡)", "Break on unmatched bracket"},
            {"/a(v)", "/a(v)⸡", "Include matched bracket"},
            {
                "/av(g/d)rs?thik#lmn",
                "/av(g/d)rs?thik#lmn⸡",
                "Includes matching across interior syntax — consider changing"
            },
        };
        List<List<String>> testLines = new ArrayList<>();
        Counter<Boolean> counter = new Counter<>();
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
                counter.add(tempAssertEquals(source.toString(), expected, actual), 1);
            }
        }
        show("checkLinkification", counter);
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
            System.out.println(
                    "See example.com"
                            + testLine.get(0)
                            + " on…;\tSee "
                            + LINKIFY_START
                            + "example.com"
                            + testLine.get(1)
                            + " on…"
                            + (testLine.size() == 2 ? "" : "\t# " + testLine.get(2)));
        }
    }

    private static void show(String functionName, Counter<Boolean> counter) {
        System.out.println(
                JOIN_TAB.join(
                        "Summary",
                        functionName,
                        "error:",
                        counter.get(Boolean.FALSE),
                        "ok:",
                        counter.get(Boolean.TRUE)));
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
            {
                "av g/dez",
                "th=ik l&=nxo",
                "pr s",
                "/av%20g/dez?th=ik%20l&=nxo#pr%20s",
                "Escape hard (' ')"
            },
            {
                "avg./dez.",
                "th=ik.l&=nxo.",
                "prs.",
                "/avg./dez.?th=ik.l&=nxo.#prs%2E",
                "Escape soft ('.') unless followed by include"
            },
            {"a(v))", "g(d))", "e(z))", "/a(v)%29?g(d)%29#e(z)%29", "Escape unmatched brackets"},
            {"", "a%3D%26%=%3D%26%", "", "?a%3D%26%=%3D%26%", "Query with escapes"},
            {"a/v%2Fg", "", "", "/a/v%2Fg", "Path with escapes"},
            {"", "a%3D%26%=%3D%26%", "", "?a%3D%26%=%3D%26%", "Query with escapes"},
        };
        List<List<String>> testLines = new ArrayList<>();
        int line = 0;
        Counter<Boolean> counter = new Counter<>();
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
                final String actual = minimalEscape(source, null);
                counter.add(tempAssertEquals(line + ") " + source.toString(), expected, actual), 1);
            }
        }
        show("checkMinimumEscaping", counter);

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

    public void checkParts() {
        String[][] tests = {
            {
                "https://ja.wikipedia.org/wiki/Special:EntityPage/Q5582#sitelinks-wikipedia",
                "{PROTOCOL=https://, HOST=ja.wikipedia.org, PATH=/wiki/Special:EntityPage/Q5582, FRAGMENT=#sitelinks-wikipedia}"
            },
            {
                "https://docs.foobar.com/knowledge/area/?name=article&topic=seo#top",
                "{PROTOCOL=https://, HOST=docs.foobar.com, PATH=/knowledge/area/, QUERY=?name=article&topic=seo, FRAGMENT=#top}"
            },
        };
        for (String[] test : tests) {
            String test0 = test[0];
            String expected = test[1];
            NavigableMap<Part, String> parts0 = Part.getParts(test0, false);
            tempAssertEquals("checkParts", expected, parts0.toString());
            String min0 = minimalEscape(parts0, null);
        }
    }

    /**
     * Check some examples of translated links of
     * https://en.wikipedia.org/wiki/Wikipedia:List_of_articles_every_Wikipedia_should_have <br>
     * TODO <br>
     * Open up each of these files and also check the links (href="[^"]*" | href='[^"]*'). <br>
     * Or use a query in wikidata to get them. <br>
     * Will have to add unescaping to the Part.getParts method.
     */
    public void checkTestFile() throws IOException {
        UnicodeMap<Counter<String>> allEscaped = new UnicodeMap<>();
        Counter<Boolean> allOkCounter = new Counter<>();
        final Consumer<? super String> action =
                x -> {
                    if (x.startsWith("#")) {
                        return;
                    }
                    String[] urls = x.split("\t");
                    String source = urls[0];
                    String expected = urls.length == 2 ? urls[1] : source;
                    Map<Part, String> parts = Part.getParts(source, false);
                    String host = parts.get(Part.HOST);
                    final Counter<Boolean> okCounter = new Counter<>();
                    Counter<Integer> escapedCounter = new Counter<>();

                    String actual = minimalEscape(new TreeMap<Part, String>(parts), escapedCounter);
                    okCounter.add(tempAssertEquals(source, expected, actual), 1);
                    try {
                        processUrls(source, okCounter, escapedCounter);
                    } catch (IOException e) {
                        // skip if failure
                    }
                    show(host, okCounter);
                    allEscaped.put(host, new Counter<>());
                    for (R2<Long, Integer> entry :
                            escapedCounter.getEntrySetSortedByCount(false, null)) {
                        Integer cp = entry.get1();
                        long count = escapedCounter.getCount(cp);
                        Counter<String> countByLocale = allEscaped.get(cp);
                        if (countByLocale == null) {
                            allEscaped.put(cp, countByLocale = new Counter<>());
                        }
                        countByLocale.add(host, count);
                    }
                    allOkCounter.addAll(okCounter);
                };
        final Path filePath =
                Path.of(
                                "/Users/markdavis/github/unicodetools/unicodetools/src/main/resources/org/unicode/tools/testLinkification.txt")
                        .toRealPath();
        System.out.println(filePath);
        Files.lines(filePath).forEach(action);
        show("ALL", allOkCounter);
        if (!allEscaped.isEmpty()) {
            final UnicodeSet keySet = allEscaped.keySet();
            for (String ch : keySet) {
                LinkTermination prop2 = LinkTermination.Property.get(ch);
                Counter<String> escapedCounter = allEscaped.get(ch);
                for (R2<Long, String> host : escapedCounter.getEntrySetSortedByCount(false, null)) {
                    Long count = host.get0();
                    String source = host.get1();
                    System.out.println(
                            JOIN_TAB.join(
                                    "ESCAPED",
                                    ch,
                                    Utility.hex(ch),
                                    UCharacter.getName(ch, ", "),
                                    prop2,
                                    count,
                                    source));
                }
            }
        }
    }

    static final Pattern HREF = Pattern.compile("href='([^']+)'|href=\"([^\"]+)\"");

    public static void processUrls(
            String requestURL, Counter<Boolean> counter, Counter<Integer> escaped)
            throws IOException {
        int wikindex = requestURL.indexOf("/wiki/");
        String prefix = requestURL.substring(0, wikindex + 6);
        final Matcher href = HREF.matcher("");
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(new URL(requestURL).openStream()))) {
            reader.lines()
                    .forEach(
                            line -> {
                                if (href.reset(line).find()) {
                                    String url = href.group(1);
                                    if (url == null) {
                                        url = href.group(2);
                                    }
                                    int wikipos = url.indexOf("/wiki/");
                                    if (wikipos >= 0) {
                                        url = prefix + url.substring(wikipos + 6);
                                        NavigableMap<Part, String> parts =
                                                Part.getParts(url, false); // splits and unescapes
                                        String raw = Joiner.on("").join(parts.values());
                                        String actual = minimalEscape(parts, escaped);
                                        counter.add(tempAssertEquals(prefix, raw, actual), 1);
                                    }
                                }
                            });
        }
    }

    public void checkUnescape() {
        String[][] tests = {
            {"PATH", "%3F%23%2F%3D%26av%C3%A7%C4%8B", "%3F%23%2F=&avçċ"},
            {"QUERY", "%3F%23%2F%3D%26av%C3%A7%C4%8B", "?%23/%3D%26avçċ"},
            {"FRAGMENT", "%3F%23%2F%3D%26av%C3%A7%C4%8B", "?#/=&avçċ"},
            {"PATH", "%61/v%2Fc", "a/v%2Fc"},
            {"QUERY", "%3D%26%23", "%3D%26%23"},
        };
        Counter<Boolean> counter = new Counter<>();
        for (String[] test : tests) {
            Part part = Part.valueOf(test[0]);
            String source = test[1];
            String expected = test[2];
            final String actual = part.unescape(source);
            counter.add(tempAssertEquals(part + " " + source, expected, actual), 1);
        }
    }

    private String show(UnicodeSet escaped) {
        StringBuilder result = new StringBuilder();
        for (String s : escaped) {
            if (result.length() == 0) {
                result.append(";\t");
            }
            result.append(Utility.hex(s, ",") + "\t" + UCharacter.getName(s, ","));
        }
        return result.toString();
    }

    private static final String SPLIT1 = "\t"; // for debugging, "\n";

    private static final boolean VERBOSE_ASSERT = false;

    public static <T> boolean tempAssertEquals(String message, T expected, T actual) {
        final boolean areEqual = Objects.equal(expected, actual);
        if (!areEqual || VERBOSE_ASSERT) {
            System.out.println(
                    JOIN_TAB.join(
                            (areEqual ? "OK" : "ERROR"),
                            message,
                            "expected:",
                            expected,
                            "actual:",
                            actual));
        }
        return areEqual;
    }

    static final Pattern escapedSequence = Pattern.compile("(%[a-fA-F0-9][a-fA-F0-9])+");

    public static String unescape(String stringWithEscapes, UnicodeSet toEscape) {
        StringBuilder result = new StringBuilder();
        Matcher matcher = escapedSequence.matcher(stringWithEscapes);
        int current = 0;
        while (matcher.find(current)) {
            result.append(
                    stringWithEscapes.substring(
                            current, matcher.start())); // append intervening text
            String unescaped = percentUnescape(matcher.group());
            unescaped
                    .chars()
                    .forEach(
                            x -> {
                                if (toEscape.contains(x)) {
                                    // quote it
                                    appendPercentEscaped(result, x, null);
                                } else {
                                    result.appendCodePoint(x);
                                }
                            });
            current = matcher.end();
        }
        result.append(stringWithEscapes.substring(current, stringWithEscapes.length()));
        return result.toString();
    }

    private static void appendPercentEscaped(
            StringBuilder output, int cp, Counter<Integer> escaped) {
        if (escaped != null) {
            escaped.add(cp, 1);
        }
        byte[] bytes = Character.toString(cp).getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < bytes.length; ++i) {
            output.append('%');
            output.append(Utility.hex(bytes[i]));
        }
    }

    /** We are guaranteed that string is all percent escaped utf8, %a3%c0 ... */
    private static String percentUnescape(String escapedSource) {
        byte[] temp = new byte[escapedSource.length() / 3];
        int tempOffset = 0;
        for (int i = 0; i < escapedSource.length(); i += 3) {
            if (escapedSource.charAt(i) != '%') {
                throw new IllegalArgumentException();
            }
            byte b = (byte) Integer.parseInt(escapedSource.substring(i + 1, i + 3), 16);
            temp[tempOffset++] = b;
        }
        return new String(temp, StandardCharsets.UTF_8);
    }
}
