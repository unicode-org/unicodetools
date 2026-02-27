package org.unicode.text.tools;

import com.ibm.icu.segmenter.LocalizedSegmenter;
import com.ibm.icu.segmenter.LocalizedSegmenter.SegmentationType;
import com.ibm.icu.segmenter.Segment;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UnicodeProperty;
import org.unicode.text.UCD.Normalizer;
import org.unicode.text.tools.Indexer.IndexEntry;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

public class Indexer {

    static Transliterator toHTML;
    static Transliterator toHTMLInput;
    static String HTML_RULES_CONTROLS;

    static int BOOP = 0x10BE77;
    static int DOOD = 0x10D00D;

    static final IndexUnicodeProperties iup = IndexUnicodeProperties.make();
    static Normalizer nfkc = new Normalizer(Normalizer.NormalizationForm.NFKC, iup);
    static final UnicodeSet newCharacters =
            IndexUnicodeProperties.make(Settings.LAST_VERSION_INFO)
                    .getProperty(UcdProperty.General_Category)
                    .getSet("Unassigned")
                    .removeAll(
                            IndexUnicodeProperties.make(Settings.LATEST_VERSION_INFO)
                                    .getProperty(UcdProperty.General_Category)
                                    .getSet("Unassigned"))
                    .freeze();
    static final UnicodeProperty name = iup.getProperty(UcdProperty.Name);
    static final UnicodeProperty nameAlias = iup.getProperty(UcdProperty.Name_Alias);
    static final UnicodeProperty informalAlias = iup.getProperty(UcdProperty.Names_List_Alias);
    static final UnicodeProperty block = iup.getProperty(UcdProperty.Block);
    static final UnicodeProperty pretty_block = iup.getProperty(UcdProperty.Pretty_Block);
    static final UnicodeProperty subheader = iup.getProperty(UcdProperty.Names_List_Subheader);
    static final UnicodeProperty subheader_notice =
            iup.getProperty(UcdProperty.Names_List_Subheader_Notice);
    static final UnicodeProperty comment = iup.getProperty(UcdProperty.Names_List_Comment);
    static final UnicodeProperty kRSUnicode = iup.getProperty(UcdProperty.kRSUnicode);
    static final UnicodeProperty kTGT_RSUnicode = iup.getProperty(UcdProperty.kTGT_RSUnicode);
    static final UnicodeProperty kJURC_RSUnicode = iup.getProperty(UcdProperty.kJURC_RSUnicode);
    static final UnicodeProperty kSeal_Rad = iup.getProperty(UcdProperty.kSEAL_Rad);
    static final UnicodeProperty cjkRadical = iup.getProperty(UcdProperty.CJK_Radical);
    static final Map<String, UnicodeSet> blockSet = new HashMap<>();

    static {
        String BASE_RULES =
                "'<' > '&lt;' ;"
                        + "'<' < '&'[lL][Tt]';' ;"
                        + "'&' > '&amp;' ;"
                        + "'&' < '&'[aA][mM][pP]';' ;"
                        + "'>' < '&'[gG][tT]';' ;"
                        + "'\"' < '&'[qQ][uU][oO][tT]';' ; "
                        + "'' < '&'[aA][pP][oO][sS]';' ; ";

        String CONTENT_RULES = "'>' > '&gt;' ;";

        String HTML_RULES = BASE_RULES + CONTENT_RULES + "'\"' > '&quot;' ; '' > '&apos;' ;";

        toHTMLInput = Transliterator.createFromRules("any-xml", HTML_RULES, Transliterator.FORWARD);

        HTML_RULES_CONTROLS =
                HTML_RULES
                        // + "\\u0000 > \uFFFD ; "
                        + "[\\uD800-\\uDB7F] > '<span class=\"high-surrogate\"><span>'\uFFFD'</span></span>' ; "
                        + "[\\uDB80-\\uDBFF] > '<span class=\"private-surrogate\"><span>'\uFFFD'</span></span>' ; "
                        + "[\\uDC00-\\uDFFF] > '<span class=\"low-surrogate\"><span>'\uFFFD'</span></span>' ; "
                        + "([[:cn:][:co:][:cc:]-[:White_Space:]]) > '<span class=\"control\">'$1'</span>' ; "
                        + "([[:cc:]&[:White_Space:]]) > '<span class=\"control\">'\uFFFD'</span>' ; ";
        toHTML =
                Transliterator.createFromRules(
                        "any-xml", HTML_RULES_CONTROLS, Transliterator.FORWARD);

        for (String blockName : block.getAvailableValues()) {
            blockSet.put(blockName, block.getSet(blockName));
        }
    }

    static class IndexEntry {
        IndexEntry(String snippet, UnicodeProperty property) {
            this.snippet = snippet;
            this.property = property;
            characters = new UnicodeSet();
        }

        List<IndexSubEntry> subEntries() {
            return Indexer.subEntries(
                    property == subheader,
                    property == subheader_notice,
                    property != name,
                    characters);
        }

        String snippet;
        UnicodeProperty property;
        UnicodeSet characters;
        Map<String, UnicodeSet> relatedCharacters = new TreeMap<>();

        public UnicodeSet coveredCharacters() {
            UnicodeSet result = characters.cloneAsThawed();
            for (UnicodeSet related : relatedCharacters.values()) {
                result.addAll(related);
            }
            return result;
        }

        public String toHTML() {
            final var subEntries = subEntries();
            final String singleEntry = subEntries.size() == 1 ? subEntries.get(0).toHTML() : null;
            return "<li><div style='overflow:hidden'>"
                    + "[RESULT TEXT]"
                    + (singleEntry != null
                            ? "<span class=ranges>" + singleEntry.replace("<span class=ranges>", "")
                            : "<ul><li><div style='overflow:hidden'>"
                                    + subEntries().stream()
                                            .map(e -> e.toHTML())
                                            .collect(
                                                    Collectors.joining(
                                                            "</div></li><li><div style='overflow:hidden'>"))
                                    + "</div></li></ul>")
                    + "</div>"
                    + relatedCharacters.entrySet().stream()
                            .map(
                                    entry ->
                                            "<div style='overflow:hidden'>"
                                                    + entry.getKey()
                                                    + "<ul><li><div style='overflow:hidden'>"
                                                    + Indexer.subEntries(
                                                                    /* showBlock= */ true,
                                                                    /* showSubheader= */ false,
                                                                    /* showName= */ true,
                                                                    entry.getValue())
                                                            .stream()
                                                            .map(e -> e.toHTML())
                                                            .collect(
                                                                    Collectors.joining(
                                                                            "</div></li><li><div style='overflow:hidden'>"))
                                                    + "</div></li></ul></div>")
                            .collect(Collectors.joining())
                    + "</li>";
        }
    }

    // Keyed by radical character, not radical number.
    static Map<Integer, UnicodeSet> getRadicalSets() {
        final Map<String, UnicodeSet> fastCJKRadicals = new HashMap<>();
        for (final String r : cjkRadical.getAvailableValues()) {
            fastCJKRadicals.put(r, cjkRadical.getSet(r));
        }
        final Map<Integer, Integer> tangutComponents = new HashMap<>();
        for (int i = 1; i < 1000; ++i) {
            int cp = i <= 768 ? 0x18800 + i - 1 : 0x18D80 + i - 769;
            if (name.getValue(cp) == null) {
                break;
            }
            if (!name.getValue(cp).equals(String.format("TANGUT COMPONENT-%03d", i))) {
                throw new IllegalArgumentException(name.getValue(cp));
            }
            tangutComponents.put(i, cp);
        }
        final Map<Integer, Integer> jurchenRadicals = new HashMap<>();
        for (int i = 1; i < 100; ++i) {
            int cp = 0x191A0 + i - 1;
            if (name.getValue(cp) == null) {
                break;
            }
            if (!name.getValue(cp).equals(String.format("JURCHEN RADICAL-%02d", i))) {
                throw new IllegalArgumentException(name.getValue(cp));
            }
            jurchenRadicals.put(i, cp);
        }
        final Map<Integer, UnicodeSet> radicalSets = new HashMap<>();
        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            for (final String rs : kRSUnicode.getValues(cp)) {
                if (rs == null) {
                    continue;
                }
                final String radical = rs.split("\\.")[0];
                for (String radicalCharacter : fastCJKRadicals.get(radical)) {
                    radicalSets
                            .computeIfAbsent(radicalCharacter.codePointAt(0), c -> new UnicodeSet())
                            .add(cp);
                }
            }
            for (final String rs : kTGT_RSUnicode.getValues(cp)) {
                if (rs == null) {
                    continue;
                }
                final int component = Integer.parseInt(rs.split("\\.")[0]);
                final int componentCharacter = tangutComponents.get(component);
                radicalSets.computeIfAbsent(componentCharacter, c -> new UnicodeSet()).add(cp);
            }
            for (final String rs : kJURC_RSUnicode.getValues(cp)) {
                if (rs == null) {
                    continue;
                }
                final int radical = Integer.parseInt(rs.split("\\.")[0]);
                final int radicalCharacter = jurchenRadicals.get(radical);
                radicalSets.computeIfAbsent(radicalCharacter, c -> new UnicodeSet()).add(cp);
            }
            for (final String r : kSeal_Rad.getValues(cp)) {
                if (r == null) {
                    continue;
                }
                final int radicalCharacter = Utility.codePointFromHex(r.split("\\.")[1]);
                radicalSets.computeIfAbsent(radicalCharacter, c -> new UnicodeSet()).add(cp);
            }
        }
        return radicalSets;
    }

    public static void main(String[] args) throws IOException {
        class PropertyComparator implements Comparator<UnicodeProperty> {
            @Override
            public int compare(UnicodeProperty left, UnicodeProperty right) {
                return left.getName().compareTo(right.getName());
            }
        }
        // Property to property value to index entry.
        Map<UnicodeProperty, Map<String, IndexEntry>> indexEntries =
                new TreeMap<>(new PropertyComparator());
        // Lemma to snippet to position of the word in the snippet.
        Map<String, Map<String, Integer>> wordIndex = new TreeMap<>();
        final var properties =
                new UnicodeProperty[] {
                    block,
                    subheader,
                    name,
                    nameAlias,
                    informalAlias,
                    subheader_notice,
                    comment,
                    kRSUnicode,
                    kTGT_RSUnicode,
                    kJURC_RSUnicode,
                };
        final var wordBreak = BreakIterator.getWordInstance();
        final var sentenceBreak =
                LocalizedSegmenter.builder().setSegmentationType(SegmentationType.SENTENCE).build();
        final Set<String> lemmatizations = new HashSet<>();
        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            for (var prop : properties) {
                final var propertyIndex = indexEntries.computeIfAbsent(prop, k -> new TreeMap<>());
                Iterable<String> snippets;
                if (prop == subheader_notice || prop == comment) {
                    snippets =
                            StreamSupport.stream(prop.getValues(cp).spliterator(), false)
                                            .filter(Objects::nonNull)
                                            .flatMap(s -> sentenceBreak.segment(s).segments())
                                            .map(Segment::getSubSequence)
                                            .map(CharSequence::toString)
                                            .map(String::strip)
                                    ::iterator;
                } else if (prop == informalAlias) {
                    snippets =
                            StreamSupport.stream(prop.getValues(cp).spliterator(), false)
                                            .filter(Objects::nonNull)
                                            .flatMap(s -> Arrays.stream(s.split("[,;]")))
                                            .map(String::strip)
                                    ::iterator;
                } else if (prop == kRSUnicode
                        || prop == kTGT_RSUnicode
                        || prop == kJURC_RSUnicode) {
                    snippets =
                            StreamSupport.stream(prop.getValues(cp).spliterator(), false)
                                            .filter(Objects::nonNull)
                                            .map(
                                                    s ->
                                                            (prop == kRSUnicode
                                                                            ? "CJK "
                                                                            : prop == kTGT_RSUnicode
                                                                                    ? "Tangut "
                                                                                    : "Jurchen ")
                                                                    + " radical/stroke "
                                                                    + s)
                                    ::iterator;
                } else {
                    snippets = prop.getValues(cp);
                }
                for (String snippet : snippets) {
                    if (snippet == null) {
                        continue;
                    }
                    if (prop == block) {
                        snippet = pretty_block.getValue(cp);
                    } else if (prop == name) {
                        snippet = snippet.replace(Utility.hex(cp), "#");
                    }
                    // Copy the snippet to a final variable for use in the λ.
                    final String indexSnippet = snippet;
                    propertyIndex
                            .computeIfAbsent(snippet, k -> new IndexEntry(indexSnippet, prop))
                            .characters
                            .add(cp);
                    // Override word breaking of ', ., and .- so that radical/stroke indices are
                    // atomic.
                    wordBreak.setText(
                            snippet.replaceAll(".-", "pm")
                                    .replace('\'', 'p')
                                    .replaceAll("\\.(?=[0-9])", "p"));
                    int start = 0;
                    for (int end = wordBreak.next();
                            end != BreakIterator.DONE;
                            start = end, end = wordBreak.next()) {
                        if (wordBreak.getRuleStatus() >= BreakIterator.WORD_NUMBER) {
                            String word = snippet.substring(start, end).toLowerCase();
                            String lemma = lemmatize(word);
                            if (false && !lemmatizations.contains(word) && !lemma.equals(word)) {
                                System.out.println(word + " < " + lemma);
                                lemmatizations.add(word);
                            }
                            wordIndex
                                    .computeIfAbsent(fold(word), k -> new TreeMap<>())
                                    .putIfAbsent(snippet, start);
                            if (!lemma.equals(fold(word))) {
                                wordIndex
                                        .computeIfAbsent(lemma, k -> new TreeMap<>())
                                        .putIfAbsent(snippet, start);
                            }
                        }
                    }
                }
            }
            if (cp % 0x10000 == 0xFFFF) {
                System.out.println("Indexed plane " + cp / 0x10000);
            }
        }
        indexEntries
                .get(block)
                .computeIfAbsent("Betty", k -> new IndexEntry(k, block))
                .characters
                .add(BOOP);
        indexEntries
                .get(block)
                .computeIfAbsent("the", k -> new IndexEntry(k, block))
                .characters
                .add(DOOD);
        wordIndex.computeIfAbsent("betty", k -> new TreeMap<>()).putIfAbsent("Betty", 0);
        wordIndex.computeIfAbsent("the", k -> new TreeMap<>()).putIfAbsent("the", 0);

        System.out.println("Radicals…");
        final var radicalSets = getRadicalSets();
        for (final int cp : radicalSets.keySet()) {
            if (!block.getValue(cp).equals("Seal")) {
                continue;
            }
            String snippet = "Seal radical " + kSeal_Rad.getValue(cp).split("\\.")[0];
            indexEntries
                    .get(comment)
                    .computeIfAbsent(snippet, k -> new IndexEntry(k, comment))
                    .characters
                    .add(cp);
            wordIndex.computeIfAbsent("seal", k -> new TreeMap<>()).putIfAbsent(snippet, 0);
            wordIndex.computeIfAbsent("radical", k -> new TreeMap<>()).putIfAbsent(snippet, 5);
            wordIndex
                    .computeIfAbsent(kSeal_Rad.getValue(cp).split("\\.")[0], k -> new TreeMap<>())
                    .putIfAbsent(snippet, 13);
        }
        for (final var propertyIndex : indexEntries.entrySet()) {
            if (propertyIndex.getKey() == kRSUnicode) {
                continue;
            }
            for (final var indexEntry : propertyIndex.getValue().values()) {
                if (indexEntry.characters.size() == 1
                        && radicalSets.containsKey(indexEntry.characters.charAt(0))) {
                    final int radical = indexEntry.characters.charAt(0);
                    final String kind =
                            name.getValue(radical).contains("COMPONENT") ? "component" : "radical";
                    String cjkParenthetical = cjkRadical.getValue(radical);
                    cjkParenthetical =
                            cjkParenthetical == null ? "" : " (" + cjkParenthetical + ")";
                    indexEntry.relatedCharacters.put(
                            "Characters with this " + kind + cjkParenthetical + ":",
                            radicalSets.get(radical));
                }
            }
        }

        System.out.println("Writing charindex.html...");
        var file = new PrintStream(new File("charindex.html"));
        file.println("<head>");
        file.println("<meta charset=\"utf-8\">");
        file.println("<title>Character Name Index</title>");
        file.println("<style>");
        final var css = new BufferedReader(new FileReader(new File("charindex.css")));
        for (String line = css.readLine(); line != null; line = css.readLine()) {
            file.println(line);
        }
        css.close();
        file.println("</style>");
        file.println("<script>");
        file.println("let wordIndex = new Map([");
        System.out.println("wordIndex...");
        {
            int i = 0;
            for (var wordAndSnippets : wordIndex.entrySet()) {
                if (++i % 1000 == 0) {
                    System.out.println(i + "/" + wordIndex.size() + "...");
                }
                file.println(
                        "    ['" + wordAndSnippets.getKey().replace("'", "\\'") + "', new Map([");
                for (var snippetAndPosition : wordAndSnippets.getValue().entrySet()) {
                    file.println(
                            "      ['"
                                    + snippetAndPosition.getKey().replace("'", "\\'")
                                    + "', "
                                    + snippetAndPosition.getValue()
                                    + "],");
                }
                file.println("])],");
            }
        }
        file.println("]);");
        System.out.println("indexEntries...");
        file.println("let indexEntries = new Map([");
        for (var property : properties) {
            System.out.println(property.getName() + "...");
            final var propertyIndex = indexEntries.get(property);
            file.println("  ['" + property.getName() + "', new Map([");
            int i = 0;
            for (var indexEntry : propertyIndex.values()) {
                if (++i % 1000 == 0) {
                    System.out.println(i + "/" + propertyIndex.size() + "...");
                }
                file.println("    ['" + indexEntry.snippet.replace("'", "\\'") + "', {");
                file.println("       html: \"" + indexEntry.toHTML().replace("\"", "\\\"") + "\",");
                file.println("       characters: [");
                for (var range : indexEntry.coveredCharacters().ranges()) {
                    file.println(
                            "         [0x"
                                    + Utility.hex(range.codepoint)
                                    + ", 0x"
                                    + Utility.hex(range.codepointEnd)
                                    + "],");
                }
                file.println("      ],");
                file.println("    }],");
            }
            file.println("  ])],");
        }
        file.println("]);");
        final var js = new BufferedReader(new FileReader(new File("index_search.js")));
        for (String line = js.readLine(); line != null; line = js.readLine()) {
            if (line.contains("GENERATED LINE")) {
                continue;
            }
            file.println(line);
        }
        js.close();
        file.println("</script>");
        file.println("</head>");
        file.println("<body>");
        file.println(
                "<h1>Character names list index "
                        + Settings.latestVersion
                        + Settings.latestVersionPhase
                        + "</h1>");
        file.println(
                "<input type='search' placeholder='Search terms, e.g., [arrow], [click], [cyrillic o], [letter with ring], [queen card], [sanskrit]…' oninput='updateResults(event)'>");
        file.println("<p id='info'></p>");
        file.println("<ul id='results'></ul>");
        file.println("</body>");
        file.close();

        System.out.println(wordIndex.size() + " words");
    }

    static int blockCount(UnicodeSet characters) {
        UnicodeSet remainder = characters.cloneAsThawed();
        int count = 0;
        while (!remainder.isEmpty()) {
            ++count;
            remainder.removeAll(blockSet.get(block.getValue(remainder.charAt(0))));
        }
        return count;
    }

    static String fold(String word) {
        // TODO(egg): collation folding.
        // Maybe some of it before segmentation.
        String folding = nfkc.normalize(word).toLowerCase();
        return folding.replace("š", "sh");
    }

    static String lemmatize(String word) {
        // TODO(egg): proper lemmatization.
        String lemma = fold(word);
        if (lemma.endsWith("ses") && lemma.length() > 4) {
            lemma = lemma.substring(0, lemma.length() - 2);
        } else if (lemma.endsWith("s") && !lemma.endsWith("ss") && lemma.length() > 2) {
            lemma = lemma.substring(0, lemma.length() - 1);
        }
        if (lemma.matches("^[0-9]*[1-9][0-9]*$")) {
            lemma = lemma.replaceAll("^0+", "");
        }
        return lemma;
    }

    static int findWord(String needle, String haystack) {
        final var wordBreak = BreakIterator.getWordInstance();
        wordBreak.setText(haystack);
        int start = 0;
        int position = -1;
        for (int end = wordBreak.next();
                end != BreakIterator.DONE;
                start = end, end = wordBreak.next()) {
            if (wordBreak.getRuleStatus() >= BreakIterator.WORD_NUMBER) {
                String word = lemmatize(haystack.substring(start, end));
                if (word.equals(needle)) {
                    position = start;
                }
            }
        }
        return position;
    }

    static class IndexSubEntry {
        String block;
        String subheader;
        String chartLink;
        String ranges;
        String propertiesLink;

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder("        ");
            if (subheader != null) {
                result.append(subheader + ". ");
            }
            if (block != null) {
                result.append("In " + block + ": ");
            }
            result.append(ranges);
            return result.toString();
        }

        public String toHTML() {
            StringBuilder result = new StringBuilder();
            if (subheader != null) {
                result.append(toHTML.transform(subheader) + ". ");
            }
            if (block != null) {
                result.append("In " + block + ": ");
            }
            result.append("<span class=ranges><a href='" + chartLink + "'>");
            result.append(toHTML.transform(ranges));
            result.append("</a>");
            if (propertiesLink != null) {
                result.append(" (<a href='" + propertiesLink + "'>properties</a>)");
            }
            result.append("</span>");
            return result.toString();
        }
    }

    static List<IndexSubEntry> subEntries(
            boolean showBlock, boolean showSubheader, boolean showName, UnicodeSet characters) {
        List<IndexSubEntry> result = new ArrayList<>();
        final boolean showBlocks =
                showBlock
                        || showSubheader
                        || !blockSet.get(block.getValue(characters.charAt(0)))
                                .containsAll(characters);
        if (!showBlocks) {
            result.add(new IndexSubEntry());
        }
        for (var range : characters.ranges()) {
            if (range.codepointEnd == range.codepoint) {
                final UnicodeSet currentBlock = blockSet.get(block.getValue(range.codepoint));
                final int blockStart = currentBlock.getRangeStart(0);
                final boolean blockHasNewCharacters =
                        !newCharacters.cloneAsThawed().retainAll(currentBlock).isEmpty();
                if (showBlocks) {
                    result.add(new IndexSubEntry());
                    result.get(result.size() - 1).block = pretty_block.getValue(range.codepoint);
                }
                final var currentSubEntry = result.get(result.size() - 1);
                if (showSubheader) {
                    currentSubEntry.subheader = subheader.getValue(range.codepoint);
                }
                switch (Settings.latestVersionPhase) {
                    case ALPHA:
                        if (blockHasNewCharacters) {
                            currentSubEntry.chartLink =
                                    "https://www.unicode.org/charts/PDF/Unicode-"
                                            + Settings.LATEST_VERSION_INFO.getVersionString(2, 2)
                                            + "/U"
                                            + Settings.LATEST_VERSION_INFO
                                                    .getVersionString(2, 2)
                                                    .replace(".", "")
                                            + "-"
                                            + Utility.hex(blockStart)
                                            + ".pdf";
                        } else {
                            currentSubEntry.chartLink =
                                    "https://unicode.org/charts/PDF/U"
                                            + Utility.hex(blockStart)
                                            + ".pdf";
                        }
                        break;
                    case BETA:
                        currentSubEntry.chartLink =
                                "https://www.unicode.org/Public/draft/charts/blocks/U"
                                        + Utility.hex(blockStart)
                                        + ".pdf";
                        break;
                    default:
                        currentSubEntry.chartLink =
                                "https://unicode.org/charts/PDF/U"
                                        + Utility.hex(blockStart)
                                        + ".pdf";
                        break;
                }
                final String characterName = name.getValue(range.codepoint);
                currentSubEntry.ranges =
                        Utility.hex(range.codepoint)
                                + "\u00A0"
                                + Character.toString(range.codepoint)
                                + (showName
                                                && characterName != null
                                                && !characterName.contains(
                                                        Utility.hex(range.codepoint))
                                        ? " " + characterName
                                        : "");
                currentSubEntry.propertiesLink =
                        "https://util.unicode.org/UnicodeJsps/character.jsp?a="
                                + Utility.hex(range.codepoint);
                if (newCharacters.contains(range.codepoint)
                        && Settings.latestVersionPhase.compareTo(Settings.ReleasePhase.BETA) < 0) {
                    currentSubEntry.propertiesLink += "&showDevProperties=1";
                }
                if (range.codepoint == BOOP || range.codepoint == DOOD) {
                    currentSubEntry.chartLink = "https://unicode.org/charts/PDF/UBOOP.pdf";
                    currentSubEntry.ranges = range.codepoint == BOOP ? "BOOP" : "DOOD";
                    currentSubEntry.propertiesLink = null;
                }
            } else {
                UnicodeSet remainder = new UnicodeSet(range.codepoint, range.codepointEnd);
                while (!remainder.isEmpty()) {
                    final var currentBlock = blockSet.get(block.getValue(remainder.charAt(0)));
                    if (showBlocks) {
                        result.add(new IndexSubEntry());
                        result.get(result.size() - 1).block =
                                pretty_block.getValue(remainder.charAt(0));
                    }
                    final int blockStart = currentBlock.getRangeStart(0);
                    final boolean blockHasNewCharacters =
                            !newCharacters.cloneAsThawed().retainAll(currentBlock).isEmpty();
                    final var subrange = remainder.cloneAsThawed().retainAll(currentBlock);
                    remainder.removeAll(currentBlock);
                    final var currentSubEntry = result.get(result.size() - 1);
                    if (showSubheader) {
                        currentSubEntry.subheader = subheader.getValue(range.codepoint);
                    }
                    switch (Settings.latestVersionPhase) {
                        case ALPHA:
                            if (blockHasNewCharacters) {
                                currentSubEntry.chartLink =
                                        "https://www.unicode.org/charts/PDF/Unicode-"
                                                + Settings.LATEST_VERSION_INFO.getVersionString(
                                                        2, 2)
                                                + "/U"
                                                + Settings.LATEST_VERSION_INFO
                                                        .getVersionString(2, 2)
                                                        .replace(".", "")
                                                + "-"
                                                + Utility.hex(blockStart)
                                                + ".pdf";
                            } else {
                                currentSubEntry.chartLink =
                                        "https://unicode.org/charts/PDF/U"
                                                + Utility.hex(blockStart)
                                                + ".pdf";
                            }
                            break;
                        case BETA:
                            currentSubEntry.chartLink =
                                    "https://www.unicode.org/Public/draft/charts/blocks/U"
                                            + Utility.hex(blockStart)
                                            + ".pdf";
                            break;
                        default:
                            currentSubEntry.chartLink =
                                    "https://unicode.org/charts/PDF/U"
                                            + Utility.hex(blockStart)
                                            + ".pdf";
                            break;
                    }
                    currentSubEntry.ranges =
                            Utility.hex(subrange.getRangeStart(0))
                                    + "–"
                                    + Utility.hex(subrange.getRangeEnd(0));
                }
            }
        }
        return result;
    }
}
