package org.unicode.text.tools;

import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UnicodeProperty;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

public class Indexer {

    static Transliterator toHTML;
    static Transliterator toHTMLInput;
    static String HTML_RULES_CONTROLS;

    static final IndexUnicodeProperties iup = IndexUnicodeProperties.make();
    static final UnicodeProperty name = iup.getProperty(UcdProperty.Name);
    static final UnicodeProperty nameAlias = iup.getProperty(UcdProperty.Name_Alias);
    static final UnicodeProperty informalAlias = iup.getProperty(UcdProperty.Names_List_Alias);
    static final UnicodeProperty block = iup.getProperty(UcdProperty.Block);
    static final UnicodeProperty subheader = iup.getProperty(UcdProperty.Names_List_Subheader);
    static final UnicodeProperty subheader_notice =
            iup.getProperty(UcdProperty.Names_List_Subheader_Notice);
    static final UnicodeProperty comment = iup.getProperty(UcdProperty.Names_List_Comment);
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

    static class Leaf {
        Leaf(String key, UnicodeProperty property) {
            this.key = key;
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

        String key;
        UnicodeProperty property;
        UnicodeSet characters;

        public String toHTML() {
            final var subEntries = subEntries();
            final String singleEntry = subEntries.size() == 1 ? subEntries.get(0).toHTML() : null;
            return "<li><div style='overflow:hidden'>"
                    + toHTML.transform(key)
                    + (singleEntry != null
                            ? (singleEntry.startsWith("In") ? " — " : "") + singleEntry
                            : "<ul><li><div style='overflow:hidden'>"
                                    + subEntries().stream()
                                            .map(e -> e.toHTML())
                                            .collect(Collectors.joining("</div></li><li><div style='overflow:hidden'>"))
                                    + "</div></li></ul>")
                    + "</div></li>";
        }
    }

    public static void main(String[] args) throws IOException {
        class PropertyComparator implements Comparator<UnicodeProperty> {
            @Override
            public int compare(UnicodeProperty left, UnicodeProperty right) {
                return left.getName().compareTo(right.getName());
            }
        }
        Map<UnicodeProperty, Map<String, Leaf>> leaves = new TreeMap<>(new PropertyComparator());
        Map<UnicodeProperty, Map<String, Set<String>>> wordIndices =
                new TreeMap<>(new PropertyComparator());
        // final var kEHDesc = iup.getProperty(UcdProperty.kEH_Desc);
        final var properties =
                new UnicodeProperty[] {
                    block,
                    subheader,
                    name,
                    nameAlias,
                    informalAlias,
                    subheader_notice,
                    comment, // kEHDesc
                };
        final var wordBreak = BreakIterator.getWordInstance();
        final Set<String> lemmatizations = new HashSet<>();
        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            for (var prop : properties) {
                final var propertyLeaves = leaves.computeIfAbsent(prop, k -> new TreeMap<>());
                final var propertyWordIndex =
                        wordIndices.computeIfAbsent(prop, k -> new TreeMap<>());
                for (String key : prop.getValues(cp)) {
                    if (key == null) {
                        continue;
                    }
                    if (prop == name) {
                        key = key.replace(Utility.hex(cp), "#");
                    }
                    if (prop == block) {
                        key = key.replace("_", " ");
                    }
                    final String leafKey = key;
                    propertyLeaves
                            .computeIfAbsent(key, k -> new Leaf(leafKey, prop))
                            .characters
                            .add(cp);
                    wordBreak.setText(key);
                    int start = 0;
                    for (int end = wordBreak.next();
                            end != BreakIterator.DONE;
                            start = end, end = wordBreak.next()) {
                        if (wordBreak.getRuleStatus() >= BreakIterator.WORD_LETTER) {
                            String word = key.substring(start, end).toLowerCase();
                            String lemma = lemmatize(word);
                            if (false && !lemmatizations.contains(word) && !lemma.equals(word)) {
                                System.out.println(word + " < " + lemma);
                                lemmatizations.add(word);
                            }
                            propertyWordIndex.computeIfAbsent(lemma, k -> new TreeSet<>()).add(key);
                        }
                    }
                }
            }
            if (cp % 0x10000 == 0xFFFF) {
                System.out.println("Indexed plane " + cp / 0x10000);
            }
        }

        System.out.println("Writing index.html...");
        var file = new PrintStream(new File("index.html"));
        file.println("<head>");
        file.println("<script>");
        file.println("let wordIndices = new Map([");
        System.out.println("wordIndices...");
        for (var property : properties) {
            final var propertyIndex = wordIndices.get(property);
            file.println("  ['" + property.getName() + "', new Map([");
            for (var wordIndex : propertyIndex.entrySet()) {
                file.println("    ['" + wordIndex.getKey().replace("'", "\\'") + "', new Set([");
                for (String leaf : wordIndex.getValue()) {
                    file.println("      '" + leaf.replace("'", "\\'") + "',");
                }
                file.println("])],");
            }
            file.println("  ])],");
        }
        file.println("]);");
        System.out.println("leaves...");
        file.println("let leaves = new Map([");
        for (var property : properties) {
            System.out.println(property.getName() + "...");
            final var propertyLeaves = leaves.get(property);
            file.println("  ['" + property.getName() + "', new Map([");
            int i = 0;
            for (var leaf : propertyLeaves.values()) {
                if (++i % 1000 == 0) {
                    System.out.println(i + "/" + propertyLeaves.size() + "...");
                }
                file.println("    ['" + leaf.key.replace("'", "\\'") + "', {");
                file.println("       html: \"" + leaf.toHTML().replace("\"", "\\\"") + "\",");
                file.println("       characters: [");
                for (var range : leaf.characters.ranges()) {
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
                "<input style='width:100%;max-width:60em' type='search' placeholder='Search terms, e.g., [arrow], [click], [contour integral], [cyrillic o], [queen card], [sanskrit]…' oninput='updateResults(event)'>");
        file.println("<p id='info'></p>");
        file.println(
                "<ul style='max-width:60em;list-style:none;padding-inline-start:0' id='results'></ul>");
        file.println("</body>");
        file.close();

        for (var property : properties) {
            System.out.println(
                    property.getName() + ": " + wordIndices.get(property).size() + " words");
        }

        final var input = new BufferedReader(new InputStreamReader(System.in));
        for (; ; ) {
            System.out.print("> ");
            String query = input.readLine();
            if (query.equals("quitquitquit")) {
                break;
            }
            String[] queryWords = query.split("\\s+");
            if (queryWords.length == 0) {
                return;
            }
            for (int i = 0; i < queryWords.length; ++i) {
                queryWords[i] = lemmatize(queryWords[i]);
            }

            final var covered = new UnicodeSet();
            final TreeSet<String> mutableEmpty = new TreeSet<>();
            for (var property : properties) {
                final var propertyLeaves = leaves.get(property);
                class KwocComparator implements Comparator<String> {
                    @Override
                    public int compare(String left, String right) {
                        final int lpos = findWord(queryWords[0], left);
                        final int rpos = findWord(queryWords[0], right);
                        return (left.substring(lpos) + left.substring(0, lpos))
                                .compareTo(right.substring(rpos) + right.substring(0, rpos));
                    }
                }
                final var resultLeaves = new TreeSet<>(new KwocComparator());
                resultLeaves.addAll(
                        wordIndices.get(property).getOrDefault(queryWords[0], mutableEmpty));
                for (int i = 1; i < queryWords.length; ++i) {
                    resultLeaves.retainAll(
                            wordIndices.get(property).getOrDefault(queryWords[i], mutableEmpty));
                }
                for (String leaf : resultLeaves) {
                    final int position = findWord(queryWords[0], leaf);
                    final var entry = propertyLeaves.get(leaf);
                    final UnicodeSet leafSet = entry.characters;
                    if (!covered.containsAll(leafSet)) {
                        String tail = leaf.substring(position);
                        if (tail.length() > 72) {
                            tail = tail.substring(0, 72) + "…";
                        }
                        String head = leaf.substring(0, position);
                        if (head.length() > 40) {
                            head = "…" + head.substring(head.length() - 40);
                        }
                        final String kwoc =
                                tail
                                        + (position > 0
                                                ? (tail.endsWith(".") ? " " : "❟")
                                                        + (tail.length() > 40 ? "\n    " : "   ")
                                                        + head
                                                : "");
                        System.out.println(kwoc);
                        for (var subEntry : entry.subEntries()) {
                            System.out.println(subEntry);
                        }
                        covered.addAll(leafSet);
                    }
                }
            }
        }
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

    static String lemmatize(String word) {
        // TODO(egg): collation folding, proper lemmatization.
        String lemma = word.toLowerCase();
        lemma = lemma.replace("š", "sh");
        if (lemma.endsWith("ses") && lemma.length() > 4) {
            lemma = lemma.substring(0, lemma.length() - 2);
        } else if (lemma.endsWith("s") && !lemma.endsWith("ss") && lemma.length() > 2) {
            lemma = lemma.substring(0, lemma.length() - 1);
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
            if (wordBreak.getRuleStatus() >= BreakIterator.WORD_LETTER) {
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
            result.append("<span style='float:right'><a href='" + chartLink + "'>");
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
                final int blockStart =
                        blockSet.get(block.getValue(range.codepoint)).getRangeStart(0);
                if (showBlocks) {
                    result.add(new IndexSubEntry());
                    result.get(result.size() - 1).block = block.getValue(range.codepoint);
                }
                final var currentSubEntry = result.get(result.size() - 1);
                if (showSubheader) {
                    currentSubEntry.subheader = subheader.getValue(range.codepoint);
                }
                currentSubEntry.chartLink =
                        "https://www.unicode.org/Public/draft/charts/blocks/U"
                                + Utility.hex(blockStart)
                                + ".pdf";
                final String characterName = name.getValue(range.codepoint);
                currentSubEntry.ranges =
                        "U+"
                                + Utility.hex(range.codepoint)
                                + "\u00A0"
                                + Character.toString(range.codepoint)
                                + (showName && characterName != null ? " " + characterName : "");
                currentSubEntry.propertiesLink =
                        "https://util.unicode.org/UnicodeJsps/character.jsp?a="
                                + Utility.hex(range.codepoint);
            } else {
                UnicodeSet remainder = new UnicodeSet(range.codepoint, range.codepointEnd);
                while (!remainder.isEmpty()) {
                    final var currentBlock = blockSet.get(block.getValue(remainder.charAt(0)));
                    if (showBlocks) {
                        result.add(new IndexSubEntry());
                        result.get(result.size() - 1).block = block.getValue(remainder.charAt(0));
                    }
                    final int blockStart = currentBlock.getRangeStart(0);
                    final var subrange = remainder.cloneAsThawed().retainAll(currentBlock);
                    remainder.removeAll(currentBlock);
                    final var currentSubEntry = result.get(result.size() - 1);
                    if (showSubheader) {
                        currentSubEntry.subheader = subheader.getValue(range.codepoint);
                    }
                    currentSubEntry.chartLink =
                            "https://www.unicode.org/Public/draft/charts/blocks/U"
                                    + Utility.hex(blockStart)
                                    + ".pdf";
                    currentSubEntry.ranges =
                            "U+"
                                    + Utility.hex(subrange.getRangeStart(0))
                                    + "..U+"
                                    + Utility.hex(subrange.getRangeEnd(0));
                }
            }
        }
        return result;
    }
}
