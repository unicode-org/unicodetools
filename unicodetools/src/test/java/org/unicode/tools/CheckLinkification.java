package org.unicode.tools;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.UnicodeMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UProperty.NameChoice;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.StringTransform;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.EntryRange;
import com.ibm.icu.text.UnicodeSet.SpanCondition;
import com.ibm.icu.util.ULocale;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Rational.MutableLong;
import org.unicode.cldr.util.TransliteratorUtilities;
import org.unicode.text.utility.Utility;

public class CheckLinkification {

    private static final boolean DEBUG = false;

    private static final String RESOURCE_DIR =
            "/Users/markdavis/github/unicodetools/unicodetools/src/main/resources/org/unicode/tools/";
    static final boolean USE_CLDR = false;
    private static final Joiner JOIN_TAB = Joiner.on('\t');
    private static final CLDRFile ENGLISH = USE_CLDR ? CLDRConfig.getInstance().getEnglish() : null;

    public static void main(String[] args) throws IOException {
        new CheckLinkification().check();
    }

    private void check() throws IOException {
        processTestFile();
        if (true) return;
        showLinkTermination();
        checkParts();
        checkUnescape();
        checkTestFile();
        checkLinkification();
        checkOverlap();
        checkMinimumEscaping();
        if (true) return;
        showLinkPairedOpeners();
    }

    public enum LinkTermination {
        Include("[\\p{ANY}]"), // overridden by following
        Hard("[\\p{whitespace}\\p{NChar}[\\p{C}-\\p{Cf}]\\p{deprecated}]"),
        Soft("[\\p{Term}\\p{lb=qu}]"), // was [‘-‛ ‹ › \"“-‟ « »'] instead of \p{lb=qu}
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
        PROTOCOL('\u0000', "[{//}]", "[]", "[]"),
        HOST('\u0000', "[/?#]", "[]", "[]"),
        PATH('/', "[?#]", "[/]", "[]"),
        QUERY('?', "[#]", "[=\\&]", "[+]"),
        FRAGMENT('#', "[]", "[]", "[]");
        final int initiator;
        final UnicodeSet terminators;
        final UnicodeSet clearStack;
        final UnicodeSet extraQuoted;

        private Part(char initiator, String terminators, String clearStack, String extraQuoted) {
            this.initiator = initiator;
            this.terminators = new UnicodeSet(terminators).freeze();
            this.clearStack = new UnicodeSet(clearStack).freeze();
            this.extraQuoted =
                    new UnicodeSet(extraQuoted)
                            .addAll(this.clearStack)
                            .addAll(this.terminators)
                            .freeze();
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
            return CheckLinkification.unescape(substring, extraQuoted);
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
     * @param atEndOfText TODO
     * @param escapedCounter TODO
     */
    public static String minimalEscape(
            NavigableMap<Part, String> parts,
            boolean atEndOfText,
            Counter<Integer> escapedCounter) {
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
            if (atEndOfText || part != lastPart) {
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
                final String actual = minimalEscape(source, false, null);
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
            String min0 = minimalEscape(parts0, false, null);
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
        long skippedAscii = 0;
        UnicodeMap<Counter<String>> allEscaped = new UnicodeMap<>();
        Counter<Boolean> allOkCounter = new Counter<>();
        Counter<String> langToPageCount = new Counter<>();
        final Matcher wikiLanguageMatcher = WIKI_LANGUAGE.matcher("");
        Set<String> unseen = new TreeSet<>(WIKI_LANGUAGES);
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
                    String wikiLanguage = host;
                    if (wikiLanguageMatcher.reset(host).find()) {
                        final String rawWikiCode = wikiLanguageMatcher.group(1);
                        wikiLanguage = fixWiki(rawWikiCode);
                        unseen.remove(rawWikiCode);
                    }
                    wikiLanguage += "\t" + getLanguageName(x);
                    final Counter<Boolean> okCounter = new Counter<>();
                    Counter<Integer> escapedCounter = new Counter<>();

                    String actual =
                            minimalEscape(new TreeMap<Part, String>(parts), false, escapedCounter);
                    okCounter.add(tempAssertEquals(wikiLanguage, expected, actual), 1);
                    try {
                        processUrls(source, wikiLanguage, okCounter, escapedCounter);
                    } catch (IOException e) {
                        // skip if failure
                    }
                    show(wikiLanguage, okCounter);
                    langToPageCount.add(wikiLanguage, okCounter.getTotal());
                    allEscaped.put(wikiLanguage, new Counter<>());
                    for (R2<Long, Integer> entry :
                            escapedCounter.getEntrySetSortedByCount(false, null)) {
                        Integer cp = entry.get1();
                        long count = escapedCounter.getCount(cp);
                        Counter<String> countByLocale = allEscaped.get(cp);
                        if (countByLocale == null) {
                            allEscaped.put(cp, countByLocale = new Counter<>());
                        }
                        countByLocale.add(wikiLanguage, count);
                    }
                    allOkCounter.addAll(okCounter);
                };
        final Path filePath = Path.of(RESOURCE_DIR + "testLinkification.txt").toRealPath();
        System.out.println(filePath);
        Files.lines(filePath).forEach(action);
        show("ALL", allOkCounter);
        if (!allEscaped.isEmpty()) {
            final UnicodeSet keySet = allEscaped.keySet();
            for (String ch : keySet) {
                LinkTermination prop2 = LinkTermination.Property.get(ch);
                Counter<String> escapedCounter = allEscaped.get(ch);
                for (R2<Long, String> countAndSource :
                        escapedCounter.getEntrySetSortedByCount(false, null)) {
                    Long count = countAndSource.get0();
                    String source = countAndSource.get1();
                    long sourceCount = langToPageCount.get(source);
                    System.out.println(
                            JOIN_TAB.join(
                                    "ESCAPED",
                                    ch,
                                    Utility.hex(ch),
                                    UCharacter.getName(ch, ", "),
                                    prop2,
                                    count,
                                    source,
                                    sourceCount));
                }
            }
        }
        unseen.stream()
                .forEach(
                        x -> {
                            System.out.println(JOIN_TAB.join("MISSING", x, getLanguageName(x)));
                        });
    }

    /**
     * Use the query https://w.wiki/CKpG to create a file. Put it in RESOURCE_DIR as
     * wikipedia1000raw.tsv, then run this method. It generates two new files, testUrls.txt and
     * testUrlsStats.txt
     *
     * @throws IOException
     */
    public void processTestFile() throws IOException {
        MutableLong skippedAscii = new MutableLong();
        MutableLong included = new MutableLong();
        final UnicodeSet skipIfOnly = new UnicodeSet("\\p{ascii}").freeze(); // [a-zA-Z0-9:/?#]
        final Path sourcePath = Path.of(RESOURCE_DIR + "wikipedia1000raw.tsv").toRealPath();
        final String toRemove = "http://www.wikidata.org/entity/Q";
        final Multimap<Long, String> output = TreeMultimap.create();
        final Counter<Integer> escaped = new Counter<>();
        final Counter<String> hostCounter = new Counter<>();
        MutableLong lines = new MutableLong();
        Consumer<? super String> action =
                line -> {
                    if (line.startsWith("item")) {
                        return;
                    }
                    lines.value++;
                    int fieldNumber = 0;
                    long qid = 0;
                    String url = null;
                    for (String item : Splitter.on('\t').split(line)) {
                        ++fieldNumber;
                        switch (fieldNumber) {
                            case 1:
                                if (!item.startsWith(toRemove)) {
                                    throw new IllegalArgumentException(line);
                                }
                                qid = Long.parseLong(item.substring(toRemove.length()));
                                break;
                            case 2:
                                NavigableMap<Part, String> parts =
                                        Part.getParts(item, false); // splits and unescapes
                                hostCounter.add(parts.get(Part.HOST), 1);
                                url = minimalEscape(parts, true, escaped);
                                break;
                        }
                    }
                    if (skipIfOnly.containsAll(url)) {
                        skippedAscii.value++;
                        return;
                    } else if (DEBUG && url.startsWith("https://en.wiki")) {
                        System.out.println("en non-ascii:\t" + url);
                    }
                    included.value++;
                    output.put(qid, url);
                    escaped.clear();
                };
        Files.lines(sourcePath).forEach(action);
        MutableLong lastQid = new MutableLong();
        System.out.println("Lines read: " + lines + "\tLines generated: " + output.size());
        lines.value = 0;
        try (PrintWriter output2 = FileUtilities.openUTF8Writer(RESOURCE_DIR, "testUrls.txt")) {
            output2.println("# Test file using the the 1000 pages every wiki should have.");
            output2.println("# Included urls: " + included);
            output2.println("# Skipping all-ASCII urls: " + skippedAscii);
            NumberFormat nf = NumberFormat.getPercentInstance();
            output.entries().stream()
                    .forEach(
                            x -> {
                                long qid = x.getKey();
                                if (qid != lastQid.value) {
                                    output2.println(
                                            JOIN_TAB.join(
                                                    "# Q" + qid,
                                                    nf.format(
                                                            lines.value / (double) output.size())));
                                    lastQid.value = qid;
                                }
                                lines.value++;
                                output2.println(x.getValue());
                            });
            output2.println("# EOF");
        }
        try (PrintWriter output2 =
                FileUtilities.openUTF8Writer(RESOURCE_DIR, "testUrlsStats.txt")) {
            hostCounter.getKeysetSortedByKey().stream()
                    .forEach(x -> output2.println(hostCounter.get(x) + "\t" + x));
        }
    }

    public String getLanguageName(String localeCode) {
        try {
            if (USE_CLDR) {
                return ENGLISH.getName(localeCode);
            } else {
                return ULocale.getDisplayCountry(localeCode, "en");
            }
        } catch (Exception e1) {
            return "BAD LANGUAGE CODE";
        }
    }

    static final Pattern HREF = Pattern.compile("href='([^']+)'|href=\"([^\"]+)\"");
    static final Pattern WIKI_LANGUAGE = Pattern.compile("([^/]+)\\.wikipedia\\.org");

    public static void processUrls(
            String requestURL,
            String wikiLanguage,
            Counter<Boolean> counter,
            Counter<Integer> escaped)
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
                                        String actual = minimalEscape(parts, false, escaped);
                                        counter.add(tempAssertEquals(wikiLanguage, raw, actual), 1);
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

    /**
     * https://meta.wikimedia.org/wiki/Special_language_codes <br>
     * https://meta.wikimedia.org/wiki/List_of_Wikipedias#Nonstandard_language_codes
     *
     * @param languageCode
     * @return
     */
    static String fixWiki(String languageCode) {
        switch (languageCode) {
            case "als":
                return "gsw";
            case "roa-rup":
                return "rup";
            case "bat-smg":
                return "sgs";
            case "simple":
                return "en";
            case "fiu-vro":
                return "vro";
            case "zh-classical":
                return "lzh";
            case "zh-min-nan":
                return "nan";
            case "zh-yue":
                return "yue";
            case "cbk-zam":
                return "cbk";
            case "map-bms":
                return "map";
            case "nrm":
                return "nrf";
            case "roa-tara":
                return "nap";
            default:
                return languageCode;
        }
    }

    static Set<String> WIKI_LANGUAGES =
            ImmutableSet.copyOf(
                    Splitter.on(",")
                            .splitToList(
                                    "en,ceb,de,fr,sv,nl,ru,es,it,pl,arz,zh,ja,uk,vi,war,ar,pt,fa,ca,id,sr,ko,no,tr,ce,fi,cs,hu,tt,ro,sh,eu,zh-min-nan,ms,he,eo,hy,da,bg,uz,cy,simple,sk,et,be,azb,el,kk,min,hr,lt,gl,ur,az,sl,lld,ka,nn,ta,th,hi,bn,mk,zh-yue,la,ast,lv,af,tg,my,te,sq,mr,mg,bs,oc,be-tarask,ku,br,sw,ml,nds,ky,lmo,jv,pnb,ckb,new,ht,vec,pms,lb,ba,su,ga,is,szl,cv,pa,fy,io,ha,tl,an,mzn,wuu,diq,vo,ig,yo,sco,kn,ne,als,gu,ia,avk,crh,bar,ban,scn,bpy,mn,qu,nv,si,xmf,frr,ps,os,or,tum,sd,bcl,bat-smg,sah,cdo,gd,bug,glk,yi,ilo,am,li,nap,gor,as,fo,mai,hsb,map-bms,shn,zh-classical,eml,ace,ie,wa,sa,hyw,sat,zu,sn,mhr,lij,hif,km,bjn,mrj,mni,dag,ary,hak,pam,rue,roa-tara,ug,zgh,bh,nso,co,tly,so,vls,nds-nl,mi,se,myv,rw,kaa,sc,bo,kw,vep,mt,tk,mdf,kab,gv,gan,fiu-vro,ff,zea,ab,skr,smn,ks,gn,frp,pcd,udm,kv,csb,ay,nrm,lo,ang,fur,olo,lfn,lez,ln,pap,nah,mwl,tw,stq,rm,ext,lad,gom,dty,av,tyv,koi,dsb,lg,cbk-zam,dv,ksh,za,bxr,blk,gag,pfl,bew,szy,haw,tay,pag,pi,awa,tcy,krc,inh,gpe,xh,kge,fon,atj,to,pdc,mnw,arc,shi,om,tn,dga,ki,nia,jam,kbp,wo,xal,nov,kbd,anp,nqo,bi,kg,roa-rup,tpi,tet,guw,jbo,mad,fj,lbe,kcg,pcm,cu,ty,trv,dtp,sm,ami,st,iba,srn,btm,alt,ltg,gcr,ny,kus,mos,ss,chr,ee,ts,got,bbc,gur,bm,pih,ve,rmy,fat,chy,rn,igl,ik,guc,ch,ady,pnt,iu,ann,rsk,pwn,dz,ti,sg,din,tdd,kl,bdr,nr,cr"));
}
