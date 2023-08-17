package org.unicode.tools.emoji;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUException;
import com.ibm.icu.util.ICUUncheckedIOException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.EmojiConstants;
import org.unicode.cldr.util.RegexUtilities;
import org.unicode.cldr.util.TransliteratorUtilities;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.DocRegistry.DocRegistryEntry;
import org.unicode.tools.emoji.Emoji.CharSource;
import org.unicode.tools.emoji.EmojiOrder.MajorGroup;

/**
 * Constructs proposals from proposalData.txt and the CandidateData. At the end of a release, before
 * the Draft Candidates are retired, run CandidateData.java to get the proposals for those
 * candidates, and add to the end of proposalData.txt
 */
public class ProposalData {

    private static final String DEBUG_STRING = EmojiConstants.fromCodePoints(0x1F9AC).toString();

    private static final String MISSING_PROPOSAL = "MISSING";
    private static final String GENDER_REPRESENTATIVE = Emoji.FEMALE;
    private static final String SKIN_REPRESENTATIVE = "🏿";
    static final Splitter SPLITTER_SEMI = Splitter.on(';').trimResults();
    static final Splitter SPLITTER_SEMI_HASH = Splitter.on(Pattern.compile("[#;]")).trimResults();
    static final Splitter SPLITTER_COMMA = Splitter.on(',').trimResults().omitEmptyStrings();
    static final Splitter SPLITTER_DOTDOT = Splitter.on("..").trimResults();

    static Pattern VALID_PROPOSAL =
            Pattern.compile(
                    "L2/\\d{2}[-‑]\\d{3}(R(\\d)?)?|Unicode\\d\\.\\d\\.\\d*|"
                            + CollectionUtilities.join(Emoji.CharSource.values(), "|"));

    final UnicodeMap<Set<String>> proposal;
    final Multimap<UnicodeSet, String> proposalToUnicodeSet;
    final String header;

    private ProposalData() {
        StringBuffer _header = new StringBuffer();
        proposal = load(_header);
        header = _header.toString();
        proposalToUnicodeSet = getProposalToUnicodeSet(proposal);
    }

    private static ImmutableMultimap<UnicodeSet, String> getProposalToUnicodeSet(
            UnicodeMap<Set<String>> source) {
        Map<String, UnicodeSet> _proposalToUnicodeSet = new TreeMap<>(Collections.reverseOrder());
        for (Entry<String, Set<String>> entry : source.entrySet()) {
            String codepoint = entry.getKey();
            for (String proposal : entry.getValue()) {
                UnicodeSet uset = _proposalToUnicodeSet.get(proposal);
                if (uset == null) {
                    _proposalToUnicodeSet.put(proposal, uset = new UnicodeSet());
                }
                uset.add(codepoint);
            }
        }
        ImmutableMultimap.Builder<UnicodeSet, String> result = ImmutableMultimap.builder();
        for (Entry<String, UnicodeSet> entry : _proposalToUnicodeSet.entrySet()) {
            result.put(entry.getValue().freeze(), entry.getKey());
        }
        return result.build();
    }

    public Set<String> getProposals(String source) {
        if (source.contains(DEBUG_STRING)) {
            int debug = 0;
        }
        Set<String> output = new TreeSet<>(Collections.reverseOrder());
        source = getSkeleton(source);
        String tempDebug = Utility.hex(source);
        output.addAll(CldrUtility.ifNull(proposal.get(source), Collections.emptySet()));
        if (output.isEmpty()) { // get provisional candidates
            output.addAll(
                    CldrUtility.ifNull(
                            CandidateData.getInstance().getProposal(source),
                            Collections.emptySet()));
        }
        if (output.isEmpty()) {
            // hack skin color
            if (source.contains(SKIN_REPRESENTATIVE)) {
                source = source.replaceAll(SKIN_REPRESENTATIVE, "");
                Set<String> other = getProposals(source);
                if (!other.isEmpty()) {
                    output.addAll(other);
                    output.add("L2/14‑173");
                }
            }
            if (output.isEmpty()) { // for debugging
                Set<String> foo = CandidateData.getInstance().getProposal(source);
            }
        }
        return output;
    }

    public String formatProposalsForHtml(Collection<String> proposals) {
        if (proposals == null || proposals.isEmpty()) {
            return "MISSING";
        }
        StringBuilder result = new StringBuilder();
        if (proposals.isEmpty()) {
            return "n/a";
        }
        for (String proposalItem : proposals) {
            if (result.length() != 0) {
                result.append(", ");
            }
            proposalItem = proposalItem.replace('-', '\u2011');
            if (!proposalItem.startsWith("L2")) {
                if (proposalItem.startsWith("Unicode")) {
                    result.append(
                            "<a target='e-prop' href='https://www.unicode.org/versions/"
                                    + proposalItem
                                    + "'>"
                                    + proposalItem.replace("Unicode", "Unicode ")
                                    + "</a>");
                } else {
                    CharSource indirectProposals;
                    try {
                        indirectProposals = Emoji.CharSource.valueOf(proposalItem);
                    } catch (IllegalArgumentException e) {
                        throw e;
                    }
                    result.append(formatProposalsForHtml(indirectProposals.proposals));
                }
            } else {
                result.append(DocRegistry.getProposalForHtml(proposalItem));
            }
        }
        return result.toString();
    }

    //    private Collection<String> getProposal(String source) {
    //        Collection<String> result = proposal.get(source);
    //        if (result.isEmpty()) {
    //            for (int cp : CharSequences.codePoints(source)) {
    //                result = proposal.get(UTF16.valueOf(cp));
    //                if (!result.isEmpty()) {
    //                    break;
    //                }
    //            }
    //        }
    //        return result;
    //    }

    static UnicodeMap<Set<String>> load(StringBuffer header) {
        UnicodeMap<Set<String>> builder = new UnicodeMap<>();
        Set<String> skinProposals = ImmutableSet.<String>builder().add("L2/14-173").build();
        Set<String> genProposals = ImmutableSet.<String>builder().add("L2/16‑160").build();

        boolean haveData = false;
        for (String line : FileUtilities.in(ProposalData.class, "proposalData.txt")) {
            if (line.startsWith("#")) {
                if (!haveData) {
                    header.append(line).append('\n');
                }
                continue;
            }
            haveData = true;
            if (line.trim().isEmpty()) {
                continue;
            }
            List<String> parts = SPLITTER_SEMI_HASH.splitToList(line);
            String codeString = parts.get(0);
            if (line.contains("1F468 1F3FF 200D 1F91D 200D 1F468 1F3FF")) {
                int debug = 0;
            }
            String proposalString = parts.get(1);
            if (proposalString.equals(MISSING_PROPOSAL)) {
                continue;
            }
            UnicodeSet codes = parseDotDot(codeString);
            Set<String> proposals = cleanProposalString(proposalString);

            addCodes(codes, proposals, skinProposals, genProposals, builder);
        }
        //        final CandidateData candidateData = CandidateData.getInstance();
        //        String debug = With.fromCodePoint(0x1F9D4, 0x200D, 0x2642, 0xFE0F);
        //        for (String candidate : candidateData.getAllCharacters(Status.Draft_Candidate)) {
        //            System.out.println(Utility.hex(candidate));
        //            if (candidate.contentEquals(debug)) {
        //                int debug2 = 0;
        //            }
        //            final String skeleton = getSkeleton(candidate);
        //            Set<String> proposalString = candidateData.getProposal(candidate);
        //            addCodes(new UnicodeSet().add(skeleton), proposalString, skinProposals,
        // genProposals, builder);
        //        }

        //        if (output.isEmpty()) {
        //
        // output.addAll(CldrUtility.ifNull(CandidateData.getInstance().getProposal(source),
        // Collections.emptySet()));
        //        }

        return builder.freeze();
    }

    public static void addCodes(
            UnicodeSet codes,
            Set<String> proposals,
            Set<String> skinProposals,
            Set<String> genProposals,
            UnicodeMap<Set<String>> builder) {
        // validate

        UnicodeSet cleaned = new UnicodeSet();
        UnicodeSet cleanedAndGen = new UnicodeSet();
        UnicodeSet cleanedAndSkin = new UnicodeSet();
        UnicodeSet cleanedAndGenAndSkin = new UnicodeSet();
        for (String code : codes) {
            String fixedCode = getSkeleton(code);
            if (fixedCode.contains(SKIN_REPRESENTATIVE)) {
                if (fixedCode.contains(GENDER_REPRESENTATIVE)) {
                    cleanedAndGenAndSkin.add(code);
                } else {
                    cleanedAndSkin.add(code);
                }
            } else {
                if (fixedCode.contains(GENDER_REPRESENTATIVE)) {
                    cleanedAndGen.add(code);
                } else {
                    cleaned.add(code);
                }
            }
        }
        if (!cleaned.isEmpty()) {
            addAll(builder, cleaned, proposals);
        }
        if (!cleanedAndSkin.isEmpty()) {
            proposals =
                    ImmutableSet.<String>builder().addAll(proposals).addAll(skinProposals).build();
            addAll(builder, cleanedAndSkin, proposals);
        }
        if (!cleanedAndGen.isEmpty()) {
            proposals =
                    ImmutableSet.<String>builder().addAll(proposals).addAll(genProposals).build();
            addAll(builder, cleanedAndGen, proposals);
        }
        if (!cleanedAndGenAndSkin.isEmpty()) {
            proposals =
                    ImmutableSet.<String>builder()
                            .addAll(proposals)
                            .addAll(genProposals)
                            .addAll(skinProposals)
                            .build();
            addAll(builder, cleanedAndGenAndSkin, proposals);
        }
    }

    public static void addAll(
            UnicodeMap<Set<String>> builder, UnicodeSet cleaned, Set<String> proposals) {
        for (String s : cleaned) {
            if (!EmojiData.EMOJI_DATA_BETA.getAllEmojiWithDefectives().contains(s)) {
                System.err.println(s + " not in EmojiData!! " + Utility.hex(s));
            }
            Set<String> old = builder.get(s);
            if (old != null) {
                proposals = ImmutableSet.<String>builder().addAll(proposals).addAll(old).build();
            }
            builder.putAll(cleaned, proposals);
        }
    }

    public static Set<String> cleanProposalString(String proposalString) {
        LinkedHashSet<String> result =
                new LinkedHashSet<>(
                        SPLITTER_COMMA.splitToList(
                                proposalString
                                        .replace('\u2011', '-')
                                        .replace("\u00AD", "")
                                        .replace("\u200B", "")
                                        .replace("'", "’")));
        Matcher matcher = VALID_PROPOSAL.matcher("");
        for (String s : result) {
            if (!matcher.reset(s).matches()) {
                throw new ICUException(
                        "Bad format for «"
                                + s
                                + "» in: "
                                + proposalString
                                + "\t"
                                + RegexUtilities.showMismatch(matcher, s));
            }
        }
        return ImmutableSet.copyOf(result);
    }

    static final Map<String, String> SHORTEST_SKELETON =
            ImmutableMap.<String, String>builder()
                    .put("🧑🏿‍❤️‍💋‍🧑🏿", "💏🏿")
                    .put("🧑🏿‍❤️‍🧑🏿", "💑🏿")
                    .build();
    //    static {
    //        for (Entry<String, String> entry : SHORTEST_SKELETON.entrySet()) {
    //            System.out.println(".put(\"" + entry.getKey() + "\",\"" + entry.getValue() + "\")"
    //                    + "\t// " + Utility.hex(entry.getKey()) + " => " +
    // Utility.hex(entry.getValue()));
    //        }
    //    }

    private static String shortestForm(String s) {
        String result = SHORTEST_SKELETON.get(s);
        if (result == null) {
            return s;
        }
        return result;
    }

    public static String removeEmojiVariant(String s) {
        return s.replace(Emoji.EMOJI_VARIANT_STRING, "");
    }

    /**
     * Normalize to woman, darkskin, no FE0F
     *
     * @param code
     * @return
     */
    public static String getSkeleton(String code) {
        code = removeEmojiVariant(code);
        if (CharSequences.getSingleCodePoint(code) != Integer.MAX_VALUE) {
            return code;
        }
        String result =
                EmojiData.SKIN_SPANNER.replaceFrom(
                        EmojiData.GENDER_SPANNER.replaceFrom(code, GENDER_REPRESENTATIVE),
                        SKIN_REPRESENTATIVE);
        String shorter = shortestForm(result);
        return shorter == null ? result : shorter;
    }
    // parse a .. construction.
    // syntax =  item ("," item)
    // item = cp (".." cp)? | string
    // cp = hex{2..5} || "10" hex{4} | . // do the dot later
    // string = cp{2,}

    public static UnicodeSet parseDotDot(String source) {
        UnicodeSet result = new UnicodeSet();
        for (String item : SPLITTER_COMMA.split(source)) {
            List<String> parts = SPLITTER_DOTDOT.splitToList(item);
            switch (parts.size()) {
                default:
                    throw new ICUException("too many .. parts in " + source);
                case 1:
                    String code1 = Utility.fromHex(parts.get(0));
                    result.add(code1);
                    break;
                case 2:
                    int cp1 = UnicodeSet.getSingleCodePoint(Utility.fromHex(parts.get(0)));
                    int cp2 = UnicodeSet.getSingleCodePoint(Utility.fromHex(parts.get(1)));
                    if (cp1 == Integer.MAX_VALUE || cp2 == Integer.MAX_VALUE) {
                        throw new ICUException("A .. must only be between 2 code points " + source);
                    }
                    result.add(cp1, cp2);
            }
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        out.append(header).append('\n');
        showProposals(out, proposal);
        UnicodeSet already = new UnicodeSet(proposal.keySet());

        UnicodeMap<BirthInfo> charsToYears = new UnicodeMap<>();
        for (String s : EmojiData.EMOJI_DATA_BETA.getAllEmojiWithoutDefectives()) {
            Set<String> proposal = getProposals(s);
            if (proposal == null || proposal.isEmpty()) {
                charsToYears.put(s, BirthInfo.getBirthInfo(s));
            }
        }
        for (BirthInfo birthInfo : new TreeSet<>(charsToYears.getAvailableValues())) {
            int year = birthInfo.year;
            out.append("\n# MISSING-" + year + "\n");
            TreeSet<String> sorted =
                    charsToYears
                            .getSet(birthInfo)
                            .addAllTo(new TreeSet<>(EmojiOrder.BETA_ORDER.codepointCompare));
            for (String skeleton : sorted) {
                out.append(showLine(skeleton, MISSING_PROPOSAL)).append('\n');
            }
        }
        out.append("\n#EOF\n");
        return out.toString();
    }

    private static void showProposals(StringBuilder out, UnicodeMap<Set<String>> proposals2) {
        for (Set<String> proposals : proposals2.values()) {
            UnicodeSet sources = proposals2.getSet(proposals);
            String proposalString = CollectionUtilities.join(proposals, ", ");
            String proposalTitle = proposalString;
            try {
                proposalTitle += " " + Emoji.CharSource.valueOf(proposalTitle).proposals;
            } catch (Exception e) {
                // no change
            }
            out.append("\n# " + proposalTitle + "\n");
            for (UnicodeSet.EntryRange range : sources.ranges()) {
                out.append(showLine(range.codepoint, range.codepointEnd, proposalString))
                        .append('\n');
            }
            for (String string : sources.strings()) {
                out.append(showLine(string, proposalString)).append('\n');
            }
        }
    }

    public static void main(String[] args) throws IOException {
        ProposalData instance = getInstance();
        if (args.length == 0) {
            args = new String[] {"data"};
        }
        int count = 0;
        for (String arg : args) {
            switch (arg) {
                case "chart":
                    ProposalData.chart(
                            Emoji.EMOJI_DIR + "charts-" + Emoji.VERSION_BETA_STRING + "/",
                            "emoji-proposals.html");
                    ++count;
                    break;
                case "data":
                    System.out.println(instance);
                    ++count;
                    break;
                default:
                    throw new IllegalArgumentException("Bad argument: " + arg);
            }
        }
        if (count == 0) {
            throw new IllegalArgumentException("No arguments found");
        }
    }

    public static <T> String showLine(int codepoint, int codepointEnd, T value) {
        String chars = UTF16.valueOf(codepoint);
        String charsWV = EmojiDataSourceCombined.EMOJI_DATA.addEmojiVariants(chars);
        String source = Utility.hex(chars);
        String names = getName(charsWV);
        String years = BirthInfo.getYear(charsWV) + "";
        if (codepoint != codepointEnd) {
            String chars2 = UTF16.valueOf(codepointEnd);
            String charsWV2 = EmojiDataSourceCombined.EMOJI_DATA.addEmojiVariants(chars2);
            source += ".." + Utility.hex(chars2);
            charsWV += ".." + charsWV2;
            names += ".." + getName(charsWV2);
            years += ".." + BirthInfo.getYear(charsWV2);
        }
        return showLine(source, value, charsWV, years, names);
    }

    public static <T> String showLine(String emoji, T proposalString) {
        String chars = EmojiDataSourceCombined.EMOJI_DATA.addEmojiVariants(emoji);
        return showLine(
                Utility.hex(emoji),
                proposalString,
                chars,
                BirthInfo.getYear(emoji) + "",
                getName(chars));
    }

    private static <T> String showLine(
            String source, T value, String chars, String year, String names) {
        return source + "; " + value + " # " + year + " (" + chars + ") " + names;
    }

    private static String getName(int codepoint) {
        return EmojiDataSourceCombined.EMOJI_DATA.getName(codepoint);
    }

    private static String getName(String emoji) {
        try {
            return EmojiDataSourceCombined.EMOJI_DATA.getName(emoji);
        } catch (Exception e) {
            return "name missing";
        }
    }

    private static final class Helper {
        static ProposalData SINGLETON = new ProposalData();
    }

    public static ProposalData getInstance() {
        return Helper.SINGLETON;
    }

    public static void chart(String dir, String filename) {
        ProposalData instance = getInstance();

        try (PrintWriter pw = FileUtilities.openUTF8Writer(dir, "emoji-proposals.txt")) {
            pw.println(instance);
        } catch (IOException e2) {
            throw new ICUUncheckedIOException(e2);
        }

        UnicodeMap<Integer> charsToYears = new UnicodeMap<>();
        for (String s : EmojiData.EMOJI_DATA_BETA.getAllEmojiWithoutDefectives()) {
            charsToYears.put(s, BirthInfo.getYear(s));
        }
        TreeSet<String> sorted = new TreeSet<>(EmojiOrder.BETA_ORDER.codepointCompare);

        try (TempPrintWriter out = new TempPrintWriter(dir, filename)) {
            ChartUtilities.writeHeader(
                    filename,
                    out,
                    "Emoji Proposals",
                    null,
                    false,
                    "<p>This chart shows proposals that provide historical background for each accepted emoji (character or sequence). "
                            + "This usually includes the first time that the emoji was considered for encoding, "
                            + "which often predates the development of the <a target='proposal-form' href='https://www.unicode.org/emoji/proposals.html'>standard form for proposals</a>.</p>"
                            + "<p>For a brief history of emoji in Unicode, "
                            + "see the <a target='doc-registry' href='https://www.unicode.org/reports/tr51/#Introduction'>Introduction</a> of <b>UTS #51, Unicode Emoji</b>. That includes "
                            + "documents that led to addition of emoji:</p>\n"
                            + "<ul>\n<li>"
                            + "the <a target='doc-registry' href='https://www.unicode.org/L2/L2000/00152-pictographs.txt'>first proposal for adding emoji</a> to Unicode</li><li>\n"
                            + "the <a target='doc-registry' href='https://www.unicode.org/L2/L2006/06369-symbols.txt'>initial proposal for extending the scope</a> of Unicode</li><li>\n"
                            + "the <a target='doc-registry' href='https://www.unicode.org/L2/L2007/07118.htm'>agreement to form a new Symbols Subcommittee</a> (use Find on page… » C.7.3)</li></ul>\n"
                            + "\n"
                            + "<p>This file is abbreviated by replacing certain characters that are always included in the same proposal:</p>"
                            + "<ul><li>skintones ("
                            + ChartUtilities.htmlSpanForSkintone(UTF16.valueOf(0x1F3FB))
                            + " "
                            + ChartUtilities.htmlSpanForSkintone(UTF16.valueOf(0x1F3FC))
                            + " "
                            + ChartUtilities.htmlSpanForSkintone(UTF16.valueOf(0x1F3FD))
                            + " "
                            + ChartUtilities.htmlSpanForSkintone(UTF16.valueOf(0x1F3FE))
                            + " "
                            + ChartUtilities.htmlSpanForSkintone(UTF16.valueOf(0x1F3FF))
                            + ") by "
                            + ChartUtilities.htmlSpanForSkintone(SKIN_REPRESENTATIVE)
                            + "</li>\n"
                            + "<li>gender signs (♀️ ♂️) by "
                            + GENDER_REPRESENTATIVE
                            + "</li></ul>\n"
                            + "<p><b>Caveats:</b> the proposal data is draft: "
                            + "suggestions for corrections and other improvements can be submitted at <a target='emoji-proposal-corrections' href='https://docs.google.com/forms/d/e/1FAIpQLSf5RtIkkBP_eUmRFSZfBGm9W6vj2n31_qUzWcv9FXdtyb81kQ/viewform?usp=pp_url&entry.565091389=Add&entry.1700236179=%F0%9F%91%BD&entry.1390707358=L2/99-999'>Emoji Proposal Corrections</a>.</p>",
                    Emoji.DATA_DIR_PRODUCTION,
                    Emoji.TR51_HTML);
            out.println(
                    "<hr><p><b>Contents</b></p>\n"
                            + "<p>There are two tables, one organized by proposal and one by emoji:<ul><li>"
                            + ChartUtilities.getLink("By Proposal", "By Proposal", null)
                            + " (the same emoji may occur multiple times)</li><li>\n"
                            + ChartUtilities.getLink("By Emoji", "By Emoji", null)
                            + " (the same proposal may occur multiple times)</li></ul>\n");
            out.println("<hr><h1>" + ChartUtilities.getDoubleLink("By Proposal") + "</h1>\n");
            out.append("<table>\n");
            for (Entry<UnicodeSet, Collection<String>> proposalAndUset :
                    instance.proposalToUnicodeSet.asMap().entrySet()) {
                UnicodeSet uset = proposalAndUset.getKey();
                Collection<String> proposals = proposalAndUset.getValue();
                boolean first = true;
                for (String proposal : proposals) {
                    DocRegistryEntry docRegistryEntry = DocRegistry.get(proposal);
                    String title =
                            docRegistryEntry == null ? "MISSING-TITLE" : docRegistryEntry.title;
                    String source =
                            docRegistryEntry == null ? "MISSING-TITLE" : docRegistryEntry.source;

                    TreeSet<String> ordered =
                            new TreeSet<String>(EmojiOrder.STD_ORDER.codepointCompare);
                    for (String item : uset) {
                        ordered.add(EmojiData.EMOJI_DATA.addEmojiVariants(item));
                    }
                    out.append(
                            "<tr><td>"
                                    + instance.formatProposalsForHtml(
                                            Collections.singleton(proposal))
                                    + "</td><td width='25%'>"
                                    + TransliteratorUtilities.toHTML.transform(title)
                                    + "</td><td width='15%'>"
                                    + TransliteratorUtilities.toHTML.transform(source));
                    if (first) {
                        String count =
                                proposals.size() == 1 ? "" : " rowSpan='" + proposals.size() + "'";
                        out.append(
                                "</td><td" + count + ">" + uset.size() + "</td><td" + count + ">");
                        for (String emoji : ordered) {
                            out.append(GenerateEmoji.getBestImage(emoji, true, "")).append('\n');
                        }
                        //                        + TransliteratorUtilities.toHTML.transform(
                        //                                    CollectionUtilities.join(ordered, " ")
                        //                                    ));
                        first = false;
                    }
                    out.append("</td></tr>\n");
                }
            }
            out.append("</table>\n");
            out.println("<br><br><h1>" + ChartUtilities.getDoubleLink("By Emoji") + "</h1>\n");
            out.append("<table>\n");
            ArrayList<Integer> years = new ArrayList<>(new TreeSet<>(charsToYears.values()));
            Collections.reverse(years);
            for (int year : years) {
                sorted.clear();
                TreeSet<String> yearSet = charsToYears.getSet(year).addAllTo(sorted);
                MajorGroup lastMajor = null;
                for (String s : yearSet) {
                    String skeleton = getSkeleton(s);
                    if (!removeEmojiVariant(s).equals(skeleton)) {
                        continue;
                    }
                    // handle special case for multi-skintones
                    String cat = EmojiOrder.BETA_ORDER.getCategory(s);
                    MajorGroup major = EmojiOrder.BETA_ORDER.getMajorGroupFromCategory(cat);
                    if (!major.equals(lastMajor)) {
                        out.append(
                                "<tr><th colSpan='4'>"
                                        + ChartUtilities.getDoubleLink(
                                                year + "_" + major,
                                                year + " — " + major.toHTMLString())
                                        + "</th></tr>\n");
                        lastMajor = major;
                    }
                    Set<String> proposals = instance.getProposals(s);
                    try {
                        out.append(
                                "<tr><td>"
                                        + ChartUtilities.getDoubleLink(Utility.hex(s))
                                        + "</td><td>"
                                        + GenerateEmoji.getBestImage(s, true, "")
                                        + "</td><td>"
                                        + TransliteratorUtilities.toHTML.transform(
                                                EmojiDataSourceCombined.EMOJI_DATA.getName(s))
                                        + "</td><td>"
                                        + instance.formatProposalsForHtml(proposals)
                                        + "</td></tr>\n");
                    } catch (IllegalArgumentException e) {
                        System.out.println("Failure with " + proposals);
                        throw e;
                    }
                }
            }
            out.append("</table>\n");
            ChartUtilities.writeFooter(out);
        } catch (IOException e1) {
            throw new ICUException();
        }
    }
}
