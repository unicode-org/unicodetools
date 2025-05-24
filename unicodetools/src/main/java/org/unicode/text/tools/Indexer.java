package org.unicode.text.tools;

import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UnicodeProperty;
import org.unicode.text.utility.Utility;

public class Indexer {

    static Transliterator toHTML;
    static Transliterator toHTMLInput;
    static String HTML_RULES_CONTROLS;

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
                        + "([[:cn:][:co:][:cc:]-[:White_Space:]]) > '<span class=\"control\">'$1'</span>' ; ";
        toHTML =
                Transliterator.createFromRules(
                        "any-xml", HTML_RULES_CONTROLS, Transliterator.FORWARD);
    }

    public static void main(String[] args) throws IOException {
        class IndexKey {
            IndexKey(String key, UnicodeProperty source) {
                this.key = key;
                this.source = source;
            }

            String key;
            UnicodeProperty source;
        }
        ;
        class PropertyComparator implements Comparator<UnicodeProperty> {
            @Override
            public int compare(UnicodeProperty left, UnicodeProperty right) {
                return left.getName().compareTo(right.getName());
            }
        }
        final var iup = IndexUnicodeProperties.make();
        Map<UnicodeProperty, Map<String, UnicodeSet>> leaves =
                new TreeMap<>(new PropertyComparator());
        Map<UnicodeProperty, Map<String, Set<String>>> wordIndices =
                new TreeMap<>(new PropertyComparator());
        final var name = iup.getProperty(UcdProperty.Name);
        final var nameAlias = iup.getProperty(UcdProperty.Name_Alias);
        final var informalAlias = iup.getProperty(UcdProperty.Names_List_Alias);
        final var block = iup.getProperty(UcdProperty.Block);
        final var subheader = iup.getProperty(UcdProperty.Names_List_Subheader);
        final var subheader_notice = iup.getProperty(UcdProperty.Names_List_Subheader_Notice);
        final var comment = iup.getProperty(UcdProperty.Names_List_Comment);
        // final var kEHDesc = iup.getProperty(UcdProperty.kEH_Desc);
        final var properties =
                new UnicodeProperty[] {
                    block,
                    subheader,
                    subheader_notice,
                    name,
                    nameAlias,
                    informalAlias,
                    comment, // kEHDesc
                };
        final var wordBreak = BreakIterator.getWordInstance();
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
                    propertyLeaves.computeIfAbsent(key, k -> new UnicodeSet()).add(cp);
                    wordBreak.setText(key);
                    int start = 0;
                    for (int end = wordBreak.next();
                            end != BreakIterator.DONE;
                            start = end, end = wordBreak.next()) {
                        if (wordBreak.getRuleStatus() >= BreakIterator.WORD_LETTER) {
                            // TODO(egg): properly lemmatize, casefold, etc.
                            String word = key.substring(start, end).toLowerCase();
                            if (word.endsWith("s")) {
                                word = word.substring(0, word.length() - 1);
                            }
                            propertyWordIndex.computeIfAbsent(word, k -> new TreeSet<>()).add(key);
                        }
                    }
                }
            }
            if (cp % 0x10000 == 0xFFFF) {
                System.out.println("Indexed plane " + cp / 0x10000);
            }
        }

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

            final var covered = new UnicodeSet();
            final TreeSet<String> mutableEmpty = new TreeSet<>();
            for (var property : properties) {
                final var propertyLeaves = leaves.get(property);
                final var resultLeaves =
                        new TreeSet<>(
                                wordIndices
                                        .get(property)
                                        .getOrDefault(queryWords[0], mutableEmpty));
                for (int i = 1; i < queryWords.length; ++i) {
                    resultLeaves.retainAll(
                            wordIndices.get(property).getOrDefault(queryWords[i], mutableEmpty));
                }
                for (String leaf : resultLeaves) {
                    final int position = leaf.toLowerCase().indexOf(queryWords[0]);
                    if (position == -1) {
                        continue;
                    }
                    final UnicodeSet leafSet = propertyLeaves.get(leaf);
                    if (!covered.containsAll(leafSet)) {
                        final String tail =
                                leaf.substring(position)
                                        .substring(0, Math.min(72, leaf.length() - position));
                        final String head = leaf.substring(Math.max(position - 72, 0), position);
                        final String kwoc =
                                tail
                                        + (position > 0
                                                ? (tail.endsWith(".") ? "    " : "❟   ") + head
                                                : "");
                        System.out.println(
                                kwoc
                                        + " ("
                                        + leafSet.size()
                                        + (property == block
                                                ? "-character block"
                                                : " characters in "
                                                        + blockCount(leafSet)
                                                        + " blocks")
                                        + ")");
                        covered.addAll(leafSet);
                    }
                }
            }
        }
        var file = new PrintStream(new File("index.html"));
        file.close();
    }

    static int blockCount(UnicodeSet characters) {
        final var iup = IndexUnicodeProperties.make();
        final var block = iup.getProperty(UcdProperty.Block);
        UnicodeSet remainder = characters.cloneAsThawed();
        int count = 0;
        while (!remainder.isEmpty()) {
            ++count;
            remainder.removeAll(block.getSet(block.getValue(remainder.charAt(0))));
        }
        return count;
    }

    public static String htmlIndexValue(UnicodeSet characters) {
        final var iup = IndexUnicodeProperties.make();
        final var block = iup.getProperty(UcdProperty.Block);
        final boolean showBlocks =
                !block.getSet(block.getValue(characters.charAt(0))).containsAll(characters);
        var result = new StringBuilder();
        if (showBlocks) {
            result.append("<ul>");
        }
        for (var range : characters.ranges()) {
            if (range.codepointEnd == range.codepoint) {
                final int blockStart =
                        block.getSet(block.getValue(range.codepoint)).getRangeStart(0);
                if (showBlocks) {
                    result.append("<li>In " + block.getValue(range.codepoint) + ": ");
                }
                result.append("<a href='https://www.unicode.org/charts/PDF/U");
                result.append(Utility.hex(blockStart));
                result.append(".pdf'>U+");
                result.append(Utility.hex(range.codepoint));
                result.append("&nbsp;");
                result.append(toHTML.transform(Character.toString(range.codepoint)));
                result.append(
                        "</a>&nbsp;(<a href='https://util.unicode.org/UnicodeJsps/character.jsp?a=");
                result.append(Utility.hex(range.codepoint));
                result.append("'>properties</a>) ");
                if (showBlocks) {
                    result.append("</li>");
                }
            } else {
                UnicodeSet remainder = new UnicodeSet(range.codepoint, range.codepointEnd);
                while (!remainder.isEmpty()) {
                    final var currentBlock = block.getSet(block.getValue(remainder.charAt(0)));
                    if (showBlocks) {
                        result.append("<li>In " + block.getValue(remainder.charAt(0)) + ": ");
                    }
                    final int blockStart = currentBlock.getRangeStart(0);
                    final var subrange = remainder.cloneAsThawed().retainAll(currentBlock);
                    remainder.removeAll(currentBlock);
                    result.append("<a href='https://www.unicode.org/charts/PDF/U");
                    result.append(Utility.hex(blockStart));
                    result.append(".pdf'>U+");
                    result.append(Utility.hex(subrange.getRangeStart(0)));
                    result.append("..U+");
                    result.append(Utility.hex(subrange.getRangeEnd(0)));
                    result.append("</a> ");
                    if (showBlocks) {
                        result.append("</li>");
                    }
                }
            }
        }
        return result.toString();
    }
}
