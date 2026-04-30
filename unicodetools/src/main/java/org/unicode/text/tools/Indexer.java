package org.unicode.text.tools;

import com.google.common.collect.Lists;
import com.ibm.icu.segmenter.LocalizedSegmenter;
import com.ibm.icu.segmenter.LocalizedSegmenter.SegmentationType;
import com.ibm.icu.segmenter.Segment;
import com.ibm.icu.segmenter.Segmenter;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import java.util.zip.DeflaterOutputStream;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;
import org.unicode.props.UcdPropertyValues.Block_Values;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.props.UnicodeProperty;
import org.unicode.text.UCD.Normalizer;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

public class Indexer {

    private static final char RECORD_SEPARATOR = 0x001E;

    private static Transliterator toHTML;
    private static String htmlRulesControls;

    private static final int BOOP = 0x10BE77;
    private static final int DOOD = 0x10D00D;

    private static final IndexUnicodeProperties IUP = IndexUnicodeProperties.make();
    private static final Normalizer NFKC = new Normalizer(Normalizer.NormalizationForm.NFKC, IUP);
    private static final UnicodeSet NEW_CHARACTERS =
            IndexUnicodeProperties.make(Settings.LAST_VERSION_INFO)
                    .getProperty(UcdProperty.General_Category)
                    .getSet("Unassigned")
                    .removeAll(IUP.getProperty(UcdProperty.General_Category).getSet("Unassigned"))
                    .freeze();
    private static final UnicodeProperty NAME = IUP.getProperty(UcdProperty.Name);
    private static final UnicodeProperty NAME_ALIAS = IUP.getProperty(UcdProperty.Name_Alias);
    private static final UnicodeProperty INFORMAL_ALIAS =
            IUP.getProperty(UcdProperty.Names_List_Alias);
    private static final UnicodeProperty BLOCK = IUP.getProperty(UcdProperty.Block);
    private static final UnicodeProperty PRETTY_BLOCK = IUP.getProperty(UcdProperty.Pretty_Block);
    private static final UnicodeProperty SUBHEADER =
            IUP.getProperty(UcdProperty.Names_List_Subheader);
    private static final UnicodeProperty SUBHEADER_NOTICE =
            IUP.getProperty(UcdProperty.Names_List_Subheader_Notice);
    private static final UnicodeProperty COMMENT = IUP.getProperty(UcdProperty.Names_List_Comment);
    private static final UnicodeProperty K_RS_UNICODE = IUP.getProperty(UcdProperty.kRSUnicode);
    private static final UnicodeProperty K_TGT_RS_UNICODE =
            IUP.getProperty(UcdProperty.kTGT_RSUnicode);
    private static final UnicodeProperty K_JURC_RS_UNICODE =
            IUP.getProperty(UcdProperty.kJURC_RSUnicode);
    private static final UnicodeProperty K_SEAL_RAD = IUP.getProperty(UcdProperty.kSEAL_Rad);
    private static final UnicodeProperty CJK_RADICAL = IUP.getProperty(UcdProperty.CJK_Radical);
    private static final UnicodeProperty GENERAL_CATEGORY =
            IUP.getProperty(UcdProperty.General_Category);
    private static final UnicodeSet NONCHARACTERS =
            IUP.getProperty(UcdProperty.Noncharacter_Code_Point).getSet("Yes");

    private static final Segmenter SENTENCE_BREAK =
            LocalizedSegmenter.builder().setSegmentationType(SegmentationType.SENTENCE).build();
    private static final Segmenter WORD_BREAK =
            LocalizedSegmenter.builder().setSegmentationType(SegmentationType.WORD).build();

    private static final Map<String, UnicodeSet> blockSet = new HashMap<>();

    private static int maxRSEntryCharacters = 0;

    private static class StringIndexer {
        public StringIndexer() {}

        public int getStringIndex(String s) {
            int result = stringIndices.getOrDefault(s, allTheStrings.length());
            if (result == allTheStrings.length()) {
                allTheStrings.append(s).append(RECORD_SEPARATOR);
                stringIndices.put(s, result);
            }
            return result;
        }

        @Override
        public String toString() {
            return allTheStrings.toString();
        }

        private final HashMap<String, Integer> stringIndices = new HashMap<>();
        private final StringBuilder allTheStrings = new StringBuilder();
    }

    static {
        String baseRules =
                "'<' > '&lt;' ;"
                        + "'<' < '&'[lL][Tt]';' ;"
                        + "'&' > '&amp;' ;"
                        + "'&' < '&'[aA][mM][pP]';' ;"
                        + "'>' < '&'[gG][tT]';' ;"
                        + "'\"' < '&'[qQ][uU][oO][tT]';' ; "
                        + "'' < '&'[aA][pP][oO][sS]';' ; ";

        String contentRules = "'>' > '&gt;' ;";

        String htmlRules = baseRules + contentRules + "'\"' > '&quot;' ; '' > '&apos;' ;";

        htmlRulesControls =
                htmlRules
                        + "[\\uD800-\\uDB7F] > '<span class=\"high-surrogate\"><span>'\uFFFD'</span></span>' ; "
                        + "[\\uDB80-\\uDBFF] > '<span class=\"private-surrogate\"><span>'\uFFFD'</span></span>' ; "
                        + "[\\uDC00-\\uDFFF] > '<span class=\"low-surrogate\"><span>'\uFFFD'</span></span>' ; "
                        + "([[:cn:][:co:][:cc:]-[:White_Space:]]) > '<span class=\"control\">'$1'</span>' ; "
                        + "([[:cc:]&[:White_Space:]]) > '<span class=\"control\">'\uFFFD'</span>' ; ";
        toHTML =
                Transliterator.createFromRules(
                        "any-xml", htmlRulesControls, Transliterator.FORWARD);

        for (String block : BLOCK.getAvailableValues()) {
            blockSet.put(block, BLOCK.getSet(block));
        }
    }

    private static class IndexEntry {
        IndexEntry(int snippetIndex, UnicodeProperty property) {
            this.snippetIndex = snippetIndex;
            this.property = property;
            characters = new UnicodeSet();
        }

        List<IndexSubEntry> subEntries() {
            try {
                return Indexer.subEntries(
                        /* showBlock= */ property == SUBHEADER,
                        /* showSubheader= */ property == SUBHEADER_NOTICE,
                        /* showName= */ property != NAME,
                        characters);
            } catch (Exception e) {
                System.err.println("In entry for " + property.getName() + ": " + snippetIndex);
                throw e;
            }
        }

        int snippetIndex;
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
            final String singleEntry =
                    subEntries.size() == 1 ? subEntries.get(0).toHTML("[RESULT TEXT]") : null;
            return "<tr class=entry>"
                    + (singleEntry != null
                            ? singleEntry + "</tr>"
                            : ("<td class=entry-text>[RESULT TEXT]</td></tr>"
                                    + "<tr class=subentry>"
                                    + subEntries().stream()
                                            .map(e -> e.toHTML(""))
                                            .collect(
                                                    Collectors.joining(
                                                            "</tr>" + "<tr class=subentry>"))
                                    + "</tr>"))
                    + relatedCharacters.entrySet().stream()
                            .map(
                                    entry ->
                                            "<tr class=related><td>"
                                                    + entry.getKey()
                                                    + "</td></tr>"
                                                    + "<tr class=subentry>"
                                                    + Indexer.subEntries(
                                                                    /* showBlock= */ true,
                                                                    /* showSubheader= */ false,
                                                                    /* showName= */ true,
                                                                    entry.getValue())
                                                            .stream()
                                                            .map(e -> e.toHTML(""))
                                                            .collect(
                                                                    Collectors.joining(
                                                                            "</tr>"
                                                                                    + "<tr class=subentry>"))
                                                    + "</tr>")
                            .collect(Collectors.joining());
        }
    }

    // Keyed by radical character, not radical number.
    private static Map<Integer, UnicodeSet> getRadicalSets() {
        final Map<String, UnicodeSet> fastCJKRadicals = new HashMap<>();
        for (final String r : CJK_RADICAL.getAvailableValues()) {
            fastCJKRadicals.put(r, CJK_RADICAL.getSet(r));
        }
        final Map<Integer, Integer> tangutComponents = new HashMap<>();
        for (int i = 1; i < 1000; ++i) {
            int cp = i <= 768 ? 0x18800 + i - 1 : 0x18D80 + i - 769;
            if (NAME.getValue(cp) == null) {
                break;
            }
            if (!NAME.getValue(cp).equals(String.format("TANGUT COMPONENT-%03d", i))) {
                throw new IllegalArgumentException(NAME.getValue(cp));
            }
            tangutComponents.put(i, cp);
        }
        final Map<Integer, Integer> jurchenRadicals = new HashMap<>();
        for (int i = 1; i < 100; ++i) {
            int cp = 0x191A0 + i - 1;
            if (NAME.getValue(cp) == null) {
                break;
            }
            if (!NAME.getValue(cp).equals(String.format("JURCHEN RADICAL-%02d", i))) {
                throw new IllegalArgumentException(NAME.getValue(cp));
            }
            jurchenRadicals.put(i, cp);
        }
        final Map<Integer, UnicodeSet> radicalSets = new HashMap<>();
        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            for (final String rs : K_RS_UNICODE.getValues(cp)) {
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
            for (final String rs : K_TGT_RS_UNICODE.getValues(cp)) {
                if (rs == null) {
                    continue;
                }
                final int component = Integer.parseInt(rs.split("\\.")[0]);
                final int componentCharacter = tangutComponents.get(component);
                radicalSets.computeIfAbsent(componentCharacter, c -> new UnicodeSet()).add(cp);
            }
            for (final String rs : K_JURC_RS_UNICODE.getValues(cp)) {
                if (rs == null) {
                    continue;
                }
                final int radical = Integer.parseInt(rs.split("\\.")[0]);
                final int radicalCharacter = jurchenRadicals.get(radical);
                radicalSets.computeIfAbsent(radicalCharacter, c -> new UnicodeSet()).add(cp);
            }
            for (final String r : K_SEAL_RAD.getValues(cp)) {
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
        final var allTheStrings = new StringIndexer();
        // Property to snippet based on property value (as an index in allTheStrings) to index
        // entry.
        Map<UnicodeProperty, Map<Integer, IndexEntry>> indexEntries =
                new TreeMap<>(new PropertyComparator());
        // Lemma to snippet (as an index in allTheStrings) to position of the word in the snippet.
        Map<String, Map<Integer, Integer>> wordIndex = new TreeMap<>();
        final var properties =
                List.of(
                        BLOCK,
                        SUBHEADER,
                        NAME,
                        NAME_ALIAS,
                        INFORMAL_ALIAS,
                        SUBHEADER_NOTICE,
                        COMMENT,
                        K_RS_UNICODE,
                        K_TGT_RS_UNICODE,
                        K_JURC_RS_UNICODE,
                        K_SEAL_RAD);
        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            for (var prop : properties) {
                final var propertyIndex = indexEntries.computeIfAbsent(prop, k -> new TreeMap<>());
                for (String snippet : getSnippets(prop, cp)) {
                    if (snippet == null) {
                        continue;
                    }
                    if (prop == BLOCK) {
                        if (Block_Values.forName(snippet) == Block_Values.No_Block) {
                            continue;
                        }
                        snippet = PRETTY_BLOCK.getValue(cp);
                    } else if (prop == NAME) {
                        snippet = snippet.replace(Utility.hex(cp), "#");
                    }
                    final int snippetIndex = allTheStrings.getStringIndex(snippet);
                    propertyIndex
                            .computeIfAbsent(snippetIndex, k -> new IndexEntry(k, prop))
                            .characters
                            .add(cp);
                    // Override word breaking of ' and - in appropriate contexts so that
                    // radical/stroke indices are atomic.
                    // With ICU4J we could do that with custom segmentation rules, but we need to
                    // have the same segmentation in the JavaScript where we do not have that
                    // luxury, so poor man’s tailoring by segmenting a mangled string it is.
                    final String mangledForWordBreak =
                            snippet.replaceAll("\\.-", ".0")
                                    .replaceAll("(?<=[0-9]'*)'(?='*\\.[0-9])", "0");
                    final Iterable<Segment> segments =
                            WORD_BREAK
                                            .segment(mangledForWordBreak)
                                            .segments()
                                            .filter(s -> s.ruleStatus >= BreakIterator.WORD_NUMBER)
                                    ::iterator;
                    for (final var segment : segments) {
                        String word =
                                snippet.substring(segment.start, segment.limit)
                                        .toLowerCase(Locale.ROOT);
                        String lemma = lemmatize(word);
                        wordIndex
                                .computeIfAbsent(fold(word), k -> new TreeMap<>())
                                .putIfAbsent(snippetIndex, segment.start);
                        if (!lemma.equals(fold(word))) {
                            wordIndex
                                    .computeIfAbsent(lemma, k -> new TreeMap<>())
                                    .putIfAbsent(snippetIndex, segment.start);
                        }
                    }
                }
            }
            if (cp % 0x10000 == 0xFFFF) {
                System.out.println("Indexed plane " + cp / 0x10000);
            }
        }
        final int bettyIndex = allTheStrings.getStringIndex("Betty");
        final int theIndex = allTheStrings.getStringIndex("the");
        indexEntries
                .get(BLOCK)
                .computeIfAbsent(bettyIndex, k -> new IndexEntry(k, BLOCK))
                .characters
                .add(BOOP);
        indexEntries
                .get(BLOCK)
                .computeIfAbsent(theIndex, k -> new IndexEntry(k, BLOCK))
                .characters
                .add(DOOD);
        wordIndex.computeIfAbsent("betty", k -> new TreeMap<>()).putIfAbsent(bettyIndex, 0);
        wordIndex.computeIfAbsent("the", k -> new TreeMap<>()).putIfAbsent(theIndex, 0);

        System.out.println("Radicals…");
        final var radicalSets = getRadicalSets();
        for (final var propertyIndex : indexEntries.entrySet()) {
            if (propertyIndex.getKey() == K_RS_UNICODE) {
                continue;
            }
            for (final var indexEntry : propertyIndex.getValue().values()) {
                if (indexEntry.characters.size() == 1
                        && radicalSets.containsKey(indexEntry.characters.charAt(0))) {
                    final int radical = indexEntry.characters.charAt(0);
                    final String kind =
                            NAME.getValue(radical).contains("COMPONENT") ? "component" : "radical";
                    String cjkParenthetical = CJK_RADICAL.getValue(radical);
                    cjkParenthetical =
                            cjkParenthetical == null ? "" : " (" + cjkParenthetical + ")";
                    indexEntry.relatedCharacters.put(
                            "Characters with this " + kind + cjkParenthetical + ":",
                            radicalSets.get(radical));
                }
            }
        }

        System.out.println("Writing charindex.html...");
        final String resources =
                Settings.UnicodeTools.UNICODETOOLS_RSRC_DIR + "org/unicode/text/tools/";
        var file = new PrintStream(new File(Settings.Output.GEN_DIR + "charindex.html"));
        final var htmlTemplate =
                new BufferedReader(new FileReader(new File(resources + "charindex_template.html")));
        for (String htmlLine = htmlTemplate.readLine();
                htmlLine != null;
                htmlLine = htmlTemplate.readLine()) {
            if (htmlLine.contains("CSS HERE")) {
                final var css =
                        new BufferedReader(new FileReader(new File(resources + "charindex.css")));
                for (String cssLine = css.readLine(); cssLine != null; cssLine = css.readLine()) {
                    file.println(cssLine);
                }
                css.close();
            } else if (htmlLine.contains("JS HERE")) {
                // No pretty-printing in the loops that print these two maps; each space or newline
                // here enlarges charindex.html by hundreds of kilobytes.  These are not suitable
                // for human consumption anyway, since anything readable is turned into indices in
                // allTheStrings.
                file.print("let wordIndex = new Map([");
                System.out.println("wordIndex...");
                {
                    int i = 0;
                    for (var wordAndSnippets : wordIndex.entrySet()) {
                        if (++i % 1000 == 0) {
                            System.out.println(i + "/" + wordIndex.size() + "...");
                        }
                        file.print(
                                "['"
                                        + wordAndSnippets.getKey().replace("'", "\\'")
                                        + "',new Map([");
                        // Stream and collect for the innermost map to avoid trailing commas, for
                        // size.
                        file.print(
                                wordAndSnippets.getValue().entrySet().stream()
                                        .map(
                                                snippetAndPosition ->
                                                        "["
                                                                + snippetAndPosition.getKey()
                                                                + ","
                                                                + snippetAndPosition.getValue()
                                                                + "]")
                                        .collect(Collectors.joining(",")));
                        file.print("])],");
                    }
                }
                file.println("]);");
                System.out.println("indexEntries...");
                file.print("let indexEntries = new Map([");
                for (var property : properties) {
                    System.out.println(property.getName() + "...");
                    final var propertyIndex = indexEntries.get(property);
                    file.print("['" + property.getName() + "',new Map([");
                    int i = 0;
                    for (var indexEntry : propertyIndex.values()) {
                        if (++i % 1000 == 0) {
                            System.out.println(i + "/" + propertyIndex.size() + "...");
                        }
                        final int htmlIndex = allTheStrings.getStringIndex(indexEntry.toHTML());
                        file.print("[" + indexEntry.snippetIndex + ",{");
                        file.print("html:" + htmlIndex + ",");
                        file.print("characters:[");
                        // Stream and collect for the innermost array to avoid trailing commas, for
                        // size.
                        file.print(
                                indexEntry
                                        .coveredCharacters()
                                        .rangeStream()
                                        .map(
                                                range ->
                                                        // Code points in decimal without
                                                        // zero-padding for size.
                                                        "["
                                                                + range.codepoint
                                                                + (range.codepointEnd
                                                                                != range.codepoint
                                                                        ? "," + range.codepointEnd
                                                                        : "")
                                                                + "]")
                                        .collect(Collectors.joining(",")));
                        file.print("]}],");
                    }
                    file.print("])],");
                }
                file.println("]);");
                file.println("let bettyIndex = " + bettyIndex + ";");
                file.println("let theIndex = " + theIndex + ";");
                final var compressed = new ByteArrayOutputStream();
                final var compressor = new DeflaterOutputStream(compressed);
                final var uncompressed = allTheStrings.toString().getBytes("UTF-8");
                compressor.write(uncompressed);
                compressor.close();
                final var compressedBytes = compressed.toByteArray();
                System.out.println(
                        "Strings compressed from "
                                + (uncompressed.length >> 20)
                                + " MiB to "
                                + (compressedBytes.length >> 10)
                                + " kiB ("
                                + 100 * compressedBytes.length / uncompressed.length
                                + "%)");
                System.out.println(
                        "Compressed payload is "
                                + compressedBytes.length
                                + " bytes, first byte is "
                                + Byte.toUnsignedInt(compressedBytes[0]));
                file.println(
                        "let allTheStringsCompressed = '"
                                + Base64.getEncoder().encodeToString(compressedBytes)
                                + "'");
                final var js =
                        new BufferedReader(new FileReader(new File(resources + "charindex.js")));
                for (String jsLine = js.readLine(); jsLine != null; jsLine = js.readLine()) {
                    if (jsLine.contains("GENERATED LINE")) {
                        continue;
                    }
                    file.println(jsLine);
                }
                js.close();
            } else {
                file.println(
                        htmlLine.replace(
                                "<!--VERSION HERE-->",
                                Settings.LATEST_VERSION_INFO.getVersionString(2, 2)
                                        + Settings.latestVersionPhase));
            }
        }
        htmlTemplate.close();
        file.close();

        System.out.println(wordIndex.size() + " words");
        System.out.println(
                indexEntries.values().stream().collect(Collectors.summingInt(Map::size))
                        + " index entries");
        System.out.println("Max characters in RS entries: " + maxRSEntryCharacters);
    }

    private static Iterable<String> getSnippets(UnicodeProperty prop, int cp) {
        if (prop == SUBHEADER_NOTICE || prop == COMMENT) {
            return StreamSupport.stream(prop.getValues(cp).spliterator(), false)
                            .filter(Objects::nonNull)
                            .flatMap(s -> SENTENCE_BREAK.segment(s).segments())
                            .map(Segment::getSubSequence)
                            .map(CharSequence::toString)
                            .map(String::strip)
                    ::iterator;
        } else if (prop == INFORMAL_ALIAS) {
            return StreamSupport.stream(prop.getValues(cp).spliterator(), false)
                            .filter(Objects::nonNull)
                            .flatMap(s -> Arrays.stream(s.split("[,;]")))
                            .map(String::strip)
                    ::iterator;
        } else if (prop == K_RS_UNICODE || prop == K_TGT_RS_UNICODE || prop == K_JURC_RS_UNICODE) {
            final String rsKind =
                    (prop == K_RS_UNICODE
                                    ? "CJK "
                                    : prop == K_TGT_RS_UNICODE ? "Tangut " : "Jurchen ")
                            + " radical-stroke ";
            return StreamSupport.stream(prop.getValues(cp).spliterator(), false)
                            .filter(Objects::nonNull)
                            .map(s -> rsKind + s)
                    ::iterator;
        } else if (prop == K_SEAL_RAD) {
            // The kSealRad property maps Seal characters to numbered radicals, which are
            // seal characters; the property values are <radical number>.<code point>.
            // If a seal character is a radical, it is mapped to itself; if we find such a
            // self-mapping, add an index entry "Seal radical <number>".
            for (final String value : prop.getValues(cp)) {
                if (value == null) {
                    continue;
                }
                final String[] parts = value.split("\\.");
                if (Utility.codePointFromHex(parts[1]) != cp) {
                    continue;
                }
                return List.of("Seal radical " + parts[0]);
            }
            return List.of();
        } else {
            return prop.getValues(cp);
        }
    }

    private static String fold(String word) {
        // TODO(egg): collation folding.
        // Maybe some of it before segmentation.
        String folding = NFKC.normalize(word).toLowerCase(Locale.ROOT);
        return folding.replace("š", "sh");
    }

    private static String lemmatize(String word) {
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

    private static class IndexSubEntry {
        String block;
        String subheader;
        String chartLink;
        String ranges;
        String characters;
        boolean rsEntry = false;

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

        public String toHTML(String entryText) {
            StringBuilder result = new StringBuilder();
            result.append("<td class=entry-text>");
            result.append(entryText);
            result.append("<span class=location>");
            if (subheader != null) {
                result.append(toHTML.transform(subheader) + ". ");
            }
            if (block != null) {
                result.append("In " + block + ": ");
            }
            result.append("</span>");
            result.append("</td><td class='ranges");
            if (rsEntry) {
                result.append(" rs-entry");
            }
            result.append("'><a href='" + chartLink + "'>");
            result.append(toHTML.transform(ranges));
            result.append("</a>");
            result.append("</td><td class='characters");
            if (rsEntry) {
                result.append(" rs-entry");
            }
            result.append("'>");
            if (characters != null) {
                result.append(toHTML.transform(characters));
            }
            result.append("</td>");
            return result.toString();
        }
    }

    private static List<IndexSubEntry> subEntries(
            boolean showBlock, boolean showSubheader, boolean showName, UnicodeSet characters) {
        List<IndexSubEntry> result = new ArrayList<>();
        final boolean showBlocks =
                showBlock
                        || showSubheader
                        || !blockSet.get(BLOCK.getValue(characters.charAt(0)))
                                .containsAll(characters);
        if (!showBlocks) {
            result.add(new IndexSubEntry());
        }
        IndexSubEntry previousSubEntryWithLocation = null;
        for (var range : characters.ranges()) {
            if (range.codepointEnd == range.codepoint) {
                if (showBlocks) {
                    result.add(new IndexSubEntry());
                    result.get(result.size() - 1).block = PRETTY_BLOCK.getValue(range.codepoint);
                }
                final var currentSubEntry = result.get(result.size() - 1);
                if (showSubheader) {
                    currentSubEntry.subheader = SUBHEADER.getValue(range.codepoint);
                }
                currentSubEntry.chartLink =
                        getChartLink(new UnicodeSet(range.codepoint, range.codepoint));
                currentSubEntry.ranges = Utility.hex(range.codepoint);
                if (!General_Category_Values.forName(GENERAL_CATEGORY.getValue(range.codepoint))
                        .getShortName()
                        .startsWith("C")) {
                    currentSubEntry.characters = Character.toString(range.codepoint);
                }
                if (range.codepoint == BOOP || range.codepoint == DOOD) {
                    currentSubEntry.chartLink = "https://unicode.org/charts/PDF/UBOOP.pdf";
                    currentSubEntry.ranges = range.codepoint == BOOP ? "BOOP" : "DOOD";
                }
                if (previousSubEntryWithLocation != null
                        && Objects.equals(
                                currentSubEntry.subheader, previousSubEntryWithLocation.subheader)
                        && Objects.equals(
                                currentSubEntry.block, previousSubEntryWithLocation.block)) {
                    currentSubEntry.subheader = null;
                    currentSubEntry.block = null;
                } else {
                    previousSubEntryWithLocation = currentSubEntry;
                }
            } else {
                UnicodeSet remainder = new UnicodeSet(range.codepoint, range.codepointEnd);
                while (!remainder.isEmpty()) {
                    final String blockValue = BLOCK.getValue(remainder.charAt(0));
                    final var currentBlock = blockSet.get(blockValue);
                    if (showBlocks) {
                        result.add(new IndexSubEntry());
                        result.get(result.size() - 1).block =
                                PRETTY_BLOCK.getValue(remainder.charAt(0));
                    }
                    final var subrange = remainder.cloneAsThawed().retainAll(currentBlock);
                    remainder.removeAll(currentBlock);
                    final var currentSubEntry = result.get(result.size() - 1);
                    if (showSubheader) {
                        currentSubEntry.subheader = SUBHEADER.getValue(range.codepoint);
                    }
                    currentSubEntry.chartLink = getChartLink(subrange);
                    currentSubEntry.ranges =
                            Utility.hex(subrange.getRangeStart(0))
                                    + "–"
                                    + Utility.hex(subrange.getRangeEnd(0));
                    rsProperties:
                    for (var rsProperty :
                            new UnicodeProperty[] {
                                K_RS_UNICODE, K_TGT_RS_UNICODE, K_JURC_RS_UNICODE
                            }) {
                        Set<String> commonValues = new HashSet<>();
                        commonValues.addAll(
                                Lists.newArrayList(
                                        rsProperty
                                                .getValues(subrange.getRangeStart(0))
                                                .iterator()));
                        commonValues.remove(null);
                        for (int cp : subrange.codePoints()) {
                            commonValues.retainAll(
                                    Lists.newArrayList(rsProperty.getValues(cp).iterator()));
                            if (commonValues.isEmpty()) {
                                continue rsProperties;
                            }
                        }
                        currentSubEntry.characters =
                                subrange.stream().collect(Collectors.joining());
                        currentSubEntry.rsEntry = true;
                        maxRSEntryCharacters = Math.max(maxRSEntryCharacters, subrange.size());
                    }
                    final String firstGC = GENERAL_CATEGORY.getValue(subrange.getRangeStart(0));
                    if (currentSubEntry.characters == null
                            && !General_Category_Values.forName(firstGC)
                                    .getShortName()
                                    .startsWith("C")
                            && GENERAL_CATEGORY.getSet(firstGC).containsAll(subrange)) {
                        currentSubEntry.characters =
                                Character.toString(subrange.getRangeStart(0))
                                        + "–"
                                        + Character.toString(subrange.getRangeEnd(0));
                    }
                    if (previousSubEntryWithLocation != null
                            && Objects.equals(
                                    currentSubEntry.subheader,
                                    previousSubEntryWithLocation.subheader)
                            && Objects.equals(
                                    currentSubEntry.block, previousSubEntryWithLocation.block)) {
                        currentSubEntry.subheader = null;
                        currentSubEntry.block = null;
                    } else {
                        previousSubEntryWithLocation = currentSubEntry;
                    }
                }
            }
        }
        return result;
    }

    private static String getChartLink(UnicodeSet set) {
        int chartStart;
        var blockValue = UcdPropertyValues.Block_Values.forName(BLOCK.getValue(set.charAt(0)));
        boolean blockHasNewCharacters = false;
        if (NONCHARACTERS.containsAll(set)
                && (blockValue == Block_Values.No_Block
                        || blockValue == Block_Values.Supplementary_Private_Use_Area_A
                        || blockValue == Block_Values.Supplementary_Private_Use_Area_B)) {
            chartStart = set.charAt(0) & 0xFF_FF80;
        } else {
            if (blockValue == Block_Values.No_Block) {
                throw new IllegalArgumentException("Getting chart for No_Block characters " + set);
            }
            if (blockValue == Block_Values.High_Private_Use_Surrogates) {
                blockValue = Block_Values.High_Surrogates;
            }
            final var currentBlock = blockSet.get(blockValue.toString());
            chartStart = currentBlock.getRangeStart(0);
            blockHasNewCharacters = currentBlock.containsSome(NEW_CHARACTERS);
        }
        switch (Settings.latestVersionPhase) {
            case ALPHA:
                if (blockHasNewCharacters) {
                    return "https://www.unicode.org/charts/PDF/Unicode-"
                            + Settings.LATEST_VERSION_INFO.getVersionString(2, 2)
                            + "/U"
                            + Settings.LATEST_VERSION_INFO.getVersionString(2, 2).replace(".", "")
                            + "-"
                            + Utility.hex(chartStart)
                            + ".pdf";
                } else {
                    return "https://unicode.org/charts/PDF/U" + Utility.hex(chartStart) + ".pdf";
                }
            case BETA:
                return "https://www.unicode.org/Public/draft/charts/blocks/U"
                        + Utility.hex(chartStart)
                        + ".pdf";
            default:
                return "https://unicode.org/charts/PDF/U" + Utility.hex(chartStart) + ".pdf";
        }
    }
}
