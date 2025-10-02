package org.unicode.unittest;

import com.google.common.base.Joiner;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.UnicodeMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.StringTransform;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Rational.MutableLong;
import org.unicode.text.utility.Utility;
import org.unicode.utilities.UrlUtilities;
import org.unicode.utilities.UrlUtilities.LinkTermination;
import org.unicode.utilities.UrlUtilities.Part;
import org.unicode.utilities.UrlUtilities.StringRange;

/** The following is very temporary, just during the spec development. */
public class TestUrl extends TestFmwk {

    private static final Joiner JOIN_SEMI_TAB = Joiner.on(";\t");
    public static final char LINKIFY_START = '⸠';
    public static final char LINKIFY_END = '⸡';

    public static void main(String[] args) {
        new TestUrl().run(args);
    }

    public void testLinkification() {
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
                int parseResult = UrlUtilities.parsePathQueryFragment(source, 0);
                String actual =
                        parseResult == 0
                                ? source
                                : source.substring(0, parseResult)
                                        + LINKIFY_END
                                        + source.substring(parseResult);
                counter.add(assertEquals(source.toString(), expected, actual), 1);
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

    static final StringTransform[] ALTS = {
        null, Transliterator.getInstance("[a-z] Latin-Greek/UNGEGN")
    };

    private void show(String functionName, Counter<Boolean> counter) {
        if (isVerbose())
            System.out.println(
                    UrlUtilities.JOIN_TAB.join(
                            "Summary",
                            functionName,
                            "error:",
                            counter.get(Boolean.FALSE),
                            "ok:",
                            counter.get(Boolean.TRUE)));
    }

    public void testkMinimumEscaping() {
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
                final String actual = UrlUtilities.minimalEscape(source, false, null);
                counter.add(assertEquals(line + ") " + source.toString(), expected, actual), 1);
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
                            + JOIN_SEMI_TAB.join(testLine.subList(0, 3))
                            + ";\thttps://example.com"
                            + testLine.get(3)
                            + (testLine.size() < 5 ? "" : "\t# " + testLine.get(4)));
        }
    }

    public void testParts() {
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
            assertEquals("checkParts", expected, parts0.toString());
            String min0 = UrlUtilities.minimalEscape(parts0, false, null);
        }
    }

    static class Bail extends RuntimeException {}

    /**
     * Check some examples of translated links of
     * https://en.wikipedia.org/wiki/Wikipedia:List_of_articles_every_Wikipedia_should_have <br>
     * TODO <br>
     * Open up each of these files and also check the links (href="[^"]*" | href='[^"]*'). <br>
     * Or use a query in wikidata to get them. <br>
     * Will have to add unescaping to the Part.getParts method.
     */
    public void testWikipediaUrls() throws IOException {
        warnln("Use -e10 to check all the examples");
        final boolean shortTest = getInclusion() < 10;
        MutableLong items = new MutableLong();
        UnicodeMap<Counter<String>> allEscaped = new UnicodeMap<>();
        Counter<Boolean> allOkCounter = new Counter<>();
        Counter<String> langToPageCount = new Counter<>();
        final Matcher wikiLanguageMatcher = WIKI_LANGUAGE.matcher("");
        Set<String> unseen = new TreeSet<>(UrlUtilities.WIKI_LANGUAGES);
        final Consumer<? super String> action =
                x -> {
                    if (x.startsWith("#")) {
                        return;
                    }
                    if (shortTest && items.value++ > 1000) {
                        throw new Bail();
                    }

                    List<String> urls = UrlUtilities.SPLIT_TAB.splitToList(x);
                    String source = urls.get(0);
                    String expected = urls.size() == 2 ? urls.get(1) : source;
                    Map<Part, String> parts = Part.getParts(source, false);
                    String host = parts.get(Part.HOST);
                    String wikiLanguage = host;
                    if (wikiLanguageMatcher.reset(host).find()) {
                        final String rawWikiCode = wikiLanguageMatcher.group(1);
                        wikiLanguage = UrlUtilities.fixWiki(rawWikiCode);
                        unseen.remove(rawWikiCode);
                    }
                    wikiLanguage += "\t" + getLanguageName(x);
                    final Counter<Boolean> okCounter = new Counter<>();
                    Counter<Integer> escapedCounter = new Counter<>();

                    String actual =
                            UrlUtilities.minimalEscape(
                                    new TreeMap<Part, String>(parts), false, escapedCounter);
                    okCounter.add(assertEquals(wikiLanguage, expected, actual), 1);
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
        final Path filePath = Path.of(UrlUtilities.RESOURCE_DIR + "testUrls.txt").toRealPath();
        System.out.println(filePath);
        try {
            Files.lines(filePath).forEach(action);
        } catch (Bail e) {
            // we stopped part way
        }
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
                            UrlUtilities.JOIN_TAB.join(
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
        if (!unseen.isEmpty()) {
            System.out.println(
                    "No URL available: " + unseen.stream().collect(Collectors.joining(", ")));
        }
    }

    public String getLanguageCodeAndName(String localeId) {
        String langName = getLanguageName(localeId);
        return langName == null ? localeId : langName + " (" + localeId + ")";
    }

    public String getLanguageName(String localeId) {
        try {
            if (UrlUtilities.USE_CLDR) {
                return UrlUtilities.ENGLISH.getName(localeId);
            } else {
                return ULocale.getDisplayName(localeId, "en");
            }
        } catch (Exception e1) {
            return null;
        }
    }

    static final Pattern HREF = Pattern.compile("href='([^']+)'|href=\"([^\"]+)\"");
    static final Pattern WIKI_LANGUAGE = Pattern.compile("([^/]+)\\.wikipedia\\.org");

    public void processUrls(
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
                                        String raw = UrlUtilities.JOIN_EMPTY.join(parts.values());
                                        String actual =
                                                UrlUtilities.minimalEscape(parts, false, escaped);
                                        counter.add(assertEquals(wikiLanguage, raw, actual), 1);
                                    }
                                }
                            });
        }
    }

    public void testUnescape() {
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
            counter.add(assertEquals(part + " " + source, expected, actual), 1);
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

    public void testOverlap() {
        for (LinkTermination lt : LinkTermination.values()) {
            if (lt == lt.Include) {
                continue;
            }
            UnicodeSet propValue = lt.Property.getSet(lt);
            if (!propValue.equals(lt.base)) {
                errln("Overlap");
            }
        }
    }

    public void testParseUrl() {
    	String source = "See http://example.com/foobar and http://a.us/foobar!";
		StringRange position = UrlUtilities.parseURL(source, 0);
		assertEquals(source, "http://example.com/foobar", position.substring(source));
		
		position = UrlUtilities.parseURL(source, position.limit);
		assertEquals(source, "http://a.us/foobar", position.substring(source));

    }
    //    private static final String SPLIT1 = "\t"; // for debugging, "\n";
    //
    //    private static final boolean VERBOSE_ASSERT = false;

    //    public static <T> boolean tempAssertEquals(String message, T expected, T actual) {
    //        final boolean areEqual = Objects.equal(expected, actual);
    //        if (!areEqual || VERBOSE_ASSERT) {
    //            System.out.println(
    //                    CheckLinkification.JOIN_TAB.join(
    //                            (areEqual ? "OK" : "ERROR"),
    //                            message,
    //                            "expected:",
    //                            expected,
    //                            "actual:",
    //                            actual));
    //        }
    //        return areEqual;
    //    }
}
