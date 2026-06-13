package org.unicode.text.tools;

import com.ibm.icu.impl.UnicodeMap;
import com.ibm.icu.segmenter.LocalizedSegmenter;
import com.ibm.icu.segmenter.LocalizedSegmenter.SegmentationType;
import com.ibm.icu.segmenter.Segment;
import com.ibm.icu.segmenter.Segmenter;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.VersionInfo;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.text.ParsePosition;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.text.UCD.VersionedSymbolTable;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

/**
 * A tool that determines the reason for property assignments based on UTC minutes and on all
 * versions of data files.
 *
 * <h4>Nomenclature</h4>
 *
 * <p>This tool is so named because it engages in ætiology:
 *
 * <blockquote>
 *
 * The assignment of a cause, the rendering of a reason; […].<br>
 * — OED, second edition (1989).
 *
 * </blockquote>
 *
 * <p>NOTE(egg): Ætiolog<i>er</i> is, as far as I know, a neologism; the OED has ætiological and
 * ætiologically. One could have gone with -logian or -logist (indeed the OED has all three for
 * archæo- and theo-), but -er felt more appropriate for a tool.
 *
 * <h4>Outline</h4>
 *
 * <p>The tool takes as input:
 *
 * <ol>
 *   <li>the data files under unicodetools/data/;
 *   <li>actions.txt, a plain-text version of all decisions, action items, and notes from UTC
 *       minutes, provided by Peter Constable;
 *   <li>reasons_manual.txt, a data file with manually-determined reasons for some property
 *       assignments, which supplement and override the tool’s ætiology.
 * </ol>
 *
 * <p>Whenever a UTC action appears to mention a property, code points, and a version of Unicode,
 * the ætiologer checks whether the relevant property changed for those code points in that version
 * of Unicode; if it did, it assigns that UTC action as the reason.
 *
 * <p>The reasons are output to reasons_auto.txt, and can be accessed using {@code
 * Ætiologer#getReasons()}. A flag --compute-unexplained creates a file reasons_unknown.txt listing
 * all changes to property assignments after character assignments for which a reason could not be
 * determined. This can serve as a way to analyze deficiencies of the ætiologer, or as a template to
 * add entries to reasons_manual.txt.
 */
public class Ætiologer {
    private static final Segmenter WORD_BREAK =
            LocalizedSegmenter.builder()
                    .setLocale(ULocale.ENGLISH)
                    .setSegmentationType(SegmentationType.WORD)
                    .build();
    public static final Pattern L2_REF = Pattern.compile("((\\d+)-[A-Z]\\d+[a-z]*)");
    public static final Pattern L2_DOC = Pattern.compile("L2/\\d{2}-\\d{3}(R\\d*)?");
    public static final Pattern PUBREV_DOC = Pattern.compile("L2/(\\d{2})-(\\d{3})@(.*)");
    public static final Pattern PRI = Pattern.compile("PRI-(\\d+)@(.*)");
    private static final Pattern BRACKETED_L2_REF =
            Pattern.compile("\\[" + L2_REF.pattern() + "\\]");
    private static final Pattern TARGET_VERSION =
            Pattern.compile("Unicode\\s+(?:[Vv]ersion\\s+)?(\\d+(?:\\.\\d+)*)");
    private static final Pattern CODE_POINTS =
            Pattern.compile(
                    """
                    \\b
                    (?:U\\+)?
                    # Avoid false positives from source references, e.g., UTC-1234,
                    # and feedback dates.
                    (?<!C[DS]T\\s|-)
                    # Group 1, start.
                    ([0-9A-F]{4}|(?:[1-9A-F]|10)[0-9A-F]{4})
                    # Optional name.
                    (?:\\s+[A-Z][A-Z\\s-]+[A-Z])?
                    (?:
                       (?:\\sthrough\\s|\\.\\.)
                       (?:U\\+)?
                       # Group 2, limit.
                       ([0-9A-F]{4}|(?:[1-9A-F]|10)[0-9A-F]{4})
                    )?
                    (?!-)
                    \\b""",
                    Pattern.COMMENTS);
    private static final Pattern FORMAL_ALIAS = Pattern.compile("\\bformal\\s+alias");
    private static final String RESOURCES =
            Settings.UnicodeTools.UNICODETOOLS_RSRC_DIR + "org/unicode/text/tools/";
    // TODO(egg): Eventually this should be restricted to years near the current UTC meeting.
    private static final UnicodeSet POSSIBLY_YEARS =
            new UnicodeSet("[\u2000-\u2009\u2010-\u2019\u2020-\u2029]").freeze();

    public static final void main(String[] args) throws IOException {
        final var argSet = new HashSet<>(Arrays.asList(args));
        final Set<String> aliases = new HashSet<>();
        final Map<VersionInfo, UnicodeSet> unassigned = new TreeMap<>();
        final Map<Integer, Set<VersionInfo>> utcToVersions = new TreeMap<>();
        for (final var version : Utility.UNICODE_VERSIONS) {
            if (version.equals(VersionInfo.getInstance(13, 1))) {
                continue; // https://github.com/unicode-org/unicodetools/issues/100.
            }
            unassigned.put(
                    version,
                    IndexUnicodeProperties.make(version)
                            .getProperty(UcdProperty.General_Category)
                            .getSet("Cn"));
        }
        final Map<UcdProperty, Map<VersionInfo, UnicodeMap<List<String>>>> manualReasons =
                readReasonsFile("reasons_manual.txt");
        checkReasons(manualReasons);
        // Just reread the file instead of writing a deep copy…
        final Map<UcdProperty, Map<VersionInfo, UnicodeMap<List<String>>>> reasons =
                readReasonsFile("reasons_manual.txt");
        for (final var property : UcdProperty.values()) {
            switch (property.getDerivedStatus()) {
                case Approved:
                // case Provisional:
                case NonUCDProperty:
                    for (final var name : property.getNames().getAllNames()) {
                        aliases.add(name);
                    }
                    if (!argSet.contains("--compute-unexplained")) {
                        continue;
                    }
                    IndexUnicodeProperties newerIUP = null;
                    VersionInfo newerVersion = null;
                    System.out.println("Computing changes for " + property + "...");
                    // UNICODE_VERSION is in reverse chronological order.
                    for (final var olderVersion : Utility.UNICODE_VERSIONS) {
                        if (olderVersion.equals(VersionInfo.getInstance(13, 1))) {
                            continue; // https://github.com/unicode-org/unicodetools/issues/100.
                        }
                        final var olderIUP = IndexUnicodeProperties.make(olderVersion);
                        final var olderProperty = olderIUP.getProperty(property);
                        if (olderProperty.isTrivial()) {
                            // If there are property changes, they would be as part of property
                            // creation.
                            continue;
                        }
                        if (newerIUP != null) {
                            final var newerProperty = newerIUP.getProperty(property);
                            final UnicodeSet Δ = new UnicodeSet();
                            for (int cp = 0; cp <= 0x10FFFF; ++cp) {
                                if (!Objects.equals(
                                        olderProperty.getValue(cp), newerProperty.getValue(cp))) {
                                    if (unassigned.get(olderVersion).contains(cp)
                                            && !unassigned.get(newerVersion).contains(cp)) {
                                        // Property change as part of assignment.
                                        continue;
                                    }
                                    if (!manualReasons.containsKey(property)
                                            || !manualReasons
                                                    .get(property)
                                                    .containsKey(newerVersion)
                                            || !manualReasons
                                                    .get(property)
                                                    .get(newerVersion)
                                                    .containsKey(cp)) {
                                        Δ.add(cp);
                                    }
                                }
                            }
                            reasons.computeIfAbsent(property, (p) -> new TreeMap<>())
                                    .computeIfAbsent(newerVersion, (v) -> new UnicodeMap<>())
                                    .putAll(Δ, List.of());
                        }
                        newerIUP = olderIUP;
                        newerVersion = olderVersion;
                    }
                    break;
                default:
                    break;
            }
        }
        final var actionsFile =
                new BufferedReader(new FileReader(new File(RESOURCES + "actions.txt")));
        final Map<String, String> actions = new HashMap<>();
        final List<String> actionsWithNoTarget = new ArrayList<>();
        int lastUTC = 0;
        for (String line = actionsFile.readLine(); line != null; line = actionsFile.readLine()) {
            final var l2Ref = BRACKETED_L2_REF.matcher(line);
            if (!l2Ref.find()) {
                continue;
            }
            actions.put(l2Ref.group(1), line);
            final var target = TARGET_VERSION.matcher(line);
            if (!target.find()) {
                actionsWithNoTarget.add(line);
                continue;
            }
            var version = VersionInfo.getInstance(target.group(1));
            if (version == VersionInfo.UNICODE_1_0) {
                System.err.println("Reference to 1.0 cannot be a target:" + line);
                continue;
            }
            if (!Utility.isUnicodeVersion(version)) {
                if (version.compareTo(Settings.LATEST_VERSION_INFO) > 0) {
                    continue;
                }
                version = Utility.getVersionFollowing(version);
                System.out.println(
                        "Interpreting action for " + target.group(1) + " as targeting " + version);
            }
            final boolean isReason =
                    AnalyseAction(line, version, aliases, actions, l2Ref, reasons, manualReasons);
            if (isReason) {
                lastUTC = Integer.parseInt(l2Ref.group(2));
                utcToVersions.computeIfAbsent(lastUTC, (meeting) -> new TreeSet<>()).add(version);
            }
        }
        actionsFile.close();
        Set<VersionInfo> lastTargetSet = null;
        utcToVersions.put(106, Set.of(VersionInfo.UNICODE_5_0));
        utcToVersions.put(108, Set.of(VersionInfo.UNICODE_5_1));
        utcToVersions.put(116, Set.of(VersionInfo.UNICODE_5_2));
        utcToVersions.put(119, Set.of(VersionInfo.UNICODE_5_2, VersionInfo.UNICODE_6_0));
        utcToVersions.put(136, Set.of(VersionInfo.UNICODE_6_3, VersionInfo.UNICODE_7_0));
        for (int i = utcToVersions.keySet().iterator().next(); i <= lastUTC; ++i) {
            final var targetSet = utcToVersions.get(i);
            if (targetSet != null) {
                System.out.println(
                        "UTC #"
                                + i
                                + " targets "
                                + targetSet.stream()
                                        .map(v -> v.getVersionString(2, 3))
                                        .collect(Collectors.joining(" ")));
                if (i == 183) {
                    System.out.println("Excluding 10.0 for target inference");
                    // A 10.0 with a correctly deduced reason in a UTC #183 note (183-N5), but we
                    // should not assign random 10.0 changes to untargeted decisions at UTC #183.
                    targetSet.remove(VersionInfo.UNICODE_10_0);
                }
                lastTargetSet = targetSet;
            } else {
                utcToVersions.put(i, new TreeSet<>(lastTargetSet));
                System.out.println("Inferring that UTC #" + i + " targets the same");
            }
        }
        for (final String line : actionsWithNoTarget) {
            final var l2Ref = BRACKETED_L2_REF.matcher(line);
            if (!l2Ref.find()) {
                continue;
            }
            final int utcNumber = Integer.parseInt(l2Ref.group(2));
            for (final var version : utcToVersions.get(utcNumber)) {
                AnalyseAction(line, version, aliases, actions, l2Ref, reasons, manualReasons);
            }
        }
        PrintStream reasonsFile;
        if (argSet.contains("--compute-unexplained")) {
            reasonsFile = new PrintStream(new File(RESOURCES + "reasons_unknown.txt"));
            for (final var propertyReasons : reasons.entrySet()) {
                final var property = propertyReasons.getKey();
                for (final var versionReasons : propertyReasons.getValue().entrySet()) {
                    final var version = versionReasons.getKey();
                    final var unicodeMap = versionReasons.getValue();
                    for (final var value : unicodeMap.getAvailableValues()) {
                        if (value == null) {
                            continue;
                        }
                        if (value.isEmpty()) {
                            reasonsFile.println(
                                    property
                                            + " ; "
                                            + version.getVersionString(2, 3)
                                            + " ; "
                                            + unicodeMap.getSet(value)
                                            + " ; # Yet unexplained");
                        }
                    }
                }
            }
            reasonsFile.close();
        }
        reasonsFile = new PrintStream(new File(RESOURCES + "reasons_auto.txt"));
        for (final var propertyReasons : reasons.entrySet()) {
            if (propertyReasons.getValue().values().stream()
                    .flatMap(unicodeMap -> unicodeMap.getAvailableValues().stream())
                    .allMatch(List::isEmpty)) {
                continue;
            }
            final var property = propertyReasons.getKey();
            reasonsFile.println();
            reasonsFile.println("# ==========================================================");
            reasonsFile.println("# Documentation for changes to " + property + " assignments");
            reasonsFile.println("# ==========================================================");
            for (final var versionReasons : propertyReasons.getValue().entrySet()) {
                final var version = versionReasons.getKey();
                final var unicodeMap = versionReasons.getValue();
                if (unicodeMap.getAvailableValues().stream().allMatch(List::isEmpty)) {
                    continue;
                }
                reasonsFile.println();
                reasonsFile.println(
                        "# "
                                + property
                                + " assignment changes in Unicode Version "
                                + version.getVersionString(2, 3));
                for (final var value : unicodeMap.getAvailableValues()) {
                    if (value == null) {
                        continue;
                    }
                    if (value.isEmpty()) {
                        continue;
                    }
                    for (var action : value) {
                        reasonsFile.println(
                                "# "
                                        + (L2_REF.matcher(action).matches()
                                                ? actions.get(action)
                                                : action));
                    }
                    reasonsFile.println(
                            property
                                    + " ; "
                                    + version.getVersionString(2, 3)
                                    + " ; "
                                    + unicodeMap.getSet(value)
                                    + " ; "
                                    + value.stream().collect(Collectors.joining(", ")));
                }
            }
        }
        reasonsFile.close();
        int mostDocumentedCodePoint = -1;
        int maxExplainedEvents = 0;
        int mostUniquelyDocumentedCodePoint = -1;
        int maxUniqueExplanations = 0;
        int mostUniquelyDocumentedCodePointPostAssignment = -1;
        int maxUniqueExplanationsPostAssignment = 0;
        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            int explainedEvents = 0;
            Set<List<String>> uniqueExplanations = new HashSet<>();
            Set<List<String>> uniqueExplanationsPostAssignment = new HashSet<>();
            for (final var propertyReasons : reasons.entrySet()) {
                final var age =
                        Age_Values.forName(
                                IndexUnicodeProperties.make()
                                        .getProperty(UcdProperty.Age)
                                        .getValue(cp));
                final var ageVersion =
                        age == Age_Values.Unassigned
                                ? VersionInfo.getInstance(255)
                                : VersionInfo.getInstance(age.getShortName());
                for (final var versionReasons : propertyReasons.getValue().entrySet()) {
                    final var version = versionReasons.getKey();
                    final var unicodeMap = versionReasons.getValue();
                    final var reason = unicodeMap.get(cp);
                    if (reason != null && !reason.isEmpty()) {
                        ++explainedEvents;
                        uniqueExplanations.add(reason);
                        if (version.compareTo(ageVersion) > 0) {
                            uniqueExplanationsPostAssignment.add(reason);
                        }
                    }
                }
            }
            if (explainedEvents > maxExplainedEvents) {
                mostDocumentedCodePoint = cp;
                maxExplainedEvents = explainedEvents;
            }
            if (uniqueExplanations.size() > maxUniqueExplanations) {
                mostUniquelyDocumentedCodePoint = cp;
                maxUniqueExplanations = uniqueExplanations.size();
            }
            if (uniqueExplanationsPostAssignment.size() > maxUniqueExplanationsPostAssignment) {
                mostUniquelyDocumentedCodePointPostAssignment = cp;
                maxUniqueExplanationsPostAssignment = uniqueExplanationsPostAssignment.size();
            }
        }
        System.out.println(
                "Most documented: U+"
                        + Utility.hex(mostDocumentedCodePoint)
                        + " with "
                        + maxExplainedEvents
                        + " explained events");
        System.out.println(
                "Most uniquely documented: U+"
                        + Utility.hex(mostUniquelyDocumentedCodePoint)
                        + " with "
                        + maxUniqueExplanations
                        + " unique explanations");
        System.out.println(
                "Most uniquely documented post assignment: U+"
                        + Utility.hex(mostUniquelyDocumentedCodePointPostAssignment)
                        + " with "
                        + maxUniqueExplanationsPostAssignment
                        + " unique explanations");
    }

    private static boolean AnalyseAction(
            String line,
            VersionInfo version,
            Set<String> aliases,
            Map<String, String> actions,
            Matcher l2Ref,
            Map<UcdProperty, Map<VersionInfo, UnicodeMap<List<String>>>> reasons,
            Map<UcdProperty, Map<VersionInfo, UnicodeMap<List<String>>>> manualReasons) {
        final var iup = IndexUnicodeProperties.make(version);
        // Skip 13.1.0, see https://github.com/unicode-org/unicodetools/issues/100.
        final var previous =
                version.equals(VersionInfo.UNICODE_14_0)
                        ? Utility.getVersionPreceding(Utility.getVersionPreceding(version))
                        : Utility.getVersionPreceding(version);
        final var previousIUP = IndexUnicodeProperties.make(previous);
        final String segmentedText = line.split("\\]", 2)[1];
        Iterable<Segment> words = WORD_BREAK.segment(segmentedText).segments()::iterator;
        Set<UcdProperty> candidateProperties = new TreeSet<>();
        final var codePointsMentioned = new UnicodeSet();
        for (final var segment : words) {
            if (aliases.contains(segment.getSubSequence())) {
                candidateProperties.add(UcdProperty.forString((String) segment.getSubSequence()));
            }
        }
        final var range = CODE_POINTS.matcher(segmentedText);
        while (range.find()) {
            if (range.group(2) != null) {
                codePointsMentioned.add(
                        Utility.codePointFromHex(range.group(1)),
                        Utility.codePointFromHex(range.group(2)));
            } else {
                codePointsMentioned.add(Utility.codePointFromHex(range.group(1)));
            }
        }
        // Find mentions of "general category" for General_Category, "linebreak class" or "line
        // break class" for Line_Break, etc.
        for (final var alias : aliases) {
            if (alias.contains("_")
                    && line.toLowerCase()
                            .replace(" ", "")
                            .contains(alias.toLowerCase().replace("_", ""))) {
                candidateProperties.add(UcdProperty.forString(alias));
            }
        }
        if (FORMAL_ALIAS.matcher(line).find()) {
            candidateProperties.add(UcdProperty.Name_Alias);
        }
        if (candidateProperties.isEmpty() || codePointsMentioned.isEmpty()) {
            return false;
        }
        System.out.println(line);
        boolean isReason = false;
        for (final var codePoints :
                POSSIBLY_YEARS.containsAll(codePointsMentioned)
                                || codePointsMentioned.containsNone(POSSIBLY_YEARS)
                        ? new UnicodeSet[] {codePointsMentioned}
                        : new UnicodeSet[] {
                            codePointsMentioned,
                            codePointsMentioned.cloneAsThawed().removeAll(POSSIBLY_YEARS)
                        }) {
            properties:
            for (final var property : candidateProperties) {
                // TODO(egg): Horribly load-bearing.  We must read the latest files because we use
                // the
                // new @missing lines for files that predate @missing lines, and we do not do that
                // automatically.  See https://github.com/unicode-org/unicodetools/issues/1438.
                IndexUnicodeProperties.make().load(property);
                final var newProperty = iup.getProperty(property);
                final var oldProperty = previousIUP.getProperty(property);
                for (int cp : codePoints.codePoints()) {
                    if (Objects.equals(oldProperty.getValue(cp), newProperty.getValue(cp))) {
                        System.out.println(
                                "    Not "
                                        + property
                                        + ": value did not change for "
                                        + Utility.hex(cp)
                                        + " in "
                                        + version);
                        continue properties;
                    }
                }
                System.out.println(
                        "    Could be "
                                + property
                                + ": "
                                + oldProperty.getValue(codePoints.charAt(0))
                                + " vs. "
                                + newProperty.getValue(codePoints.charAt(0)));
                for (int cp : codePoints.codePoints()) {
                    if (manualReasons.containsKey(property)
                            && manualReasons.get(property).containsKey(version)
                            && manualReasons.get(property).get(version).containsKey(cp)) {
                        continue;
                    }
                    isReason = true;
                    final var unicodeMap =
                            reasons.computeIfAbsent(property, (p) -> new TreeMap<>())
                                    .computeIfAbsent(version, (v) -> new UnicodeMap<>());
                    List<String> reasonList = unicodeMap.get(cp);
                    if (reasonList == null) {
                        reasonList = new ArrayList<>();
                    } else {
                        reasonList = new ArrayList<>(reasonList);
                    }
                    reasonList.add(l2Ref.group(1));
                    unicodeMap.put(cp, reasonList);
                }
            }
            if (isReason) {
                break;
            }
        }
        return isReason;
    }

    private static Map<UcdProperty, Map<VersionInfo, UnicodeMap<List<String>>>> reasons = null;

    public static Map<UcdProperty, Map<VersionInfo, UnicodeMap<List<String>>>> getReasons()
            throws IOException {
        if (reasons == null) {
            reasons = readReasonsFile("reasons_auto.txt");
        }
        return reasons;
    }

    private static Map<UcdProperty, Map<VersionInfo, UnicodeMap<List<String>>>> readReasonsFile(
            String name) throws IOException {
        final Map<UcdProperty, Map<VersionInfo, UnicodeMap<List<String>>>> reasons =
                new TreeMap<>();
        final var file = new BufferedReader(new FileReader(new File(RESOURCES + name)));
        for (String line = file.readLine(); line != null; line = file.readLine()) {
            line = line.split("#", 2)[0].trim();
            if (line.isEmpty()) {
                continue;
            }
            final var fields = line.split("\\s*;\\s*");
            final var property = UcdProperty.forString(fields[0]);
            final var version = VersionInfo.getInstance(fields[1]);
            final var set =
                    new UnicodeSet(
                            fields[2], new ParsePosition(0), VersionedSymbolTable.forDevelopment());
            final var actions = Arrays.asList(fields[3].split("\\s*,\\s*"));
            reasons.computeIfAbsent(property, p -> new TreeMap<>())
                    .computeIfAbsent(version, v -> new UnicodeMap<>())
                    .putAll(set, actions);
        }
        file.close();
        return reasons;
    }

    private static void checkReasons(
            Map<UcdProperty, Map<VersionInfo, UnicodeMap<List<String>>>> reasons) {
        for (final var propertyReasons : reasons.entrySet()) {
            final var property = propertyReasons.getKey();
            for (final var versionReasons : propertyReasons.getValue().entrySet()) {
                final var newVersion = versionReasons.getKey();
                final var newIUP = IndexUnicodeProperties.make(newVersion);
                // Skip 13.1.0, see https://github.com/unicode-org/unicodetools/issues/100.
                final var oldVersion =
                        newVersion.equals(VersionInfo.UNICODE_14_0)
                                ? Utility.getVersionPreceding(
                                        Utility.getVersionPreceding(newVersion))
                                : Utility.getVersionPreceding(newVersion);
                final var oldIUP = IndexUnicodeProperties.make(oldVersion);
                final var newProperty = newIUP.getProperty(property);
                final var oldProperty = oldIUP.getProperty(property);
                final UnicodeSet explainedSet = versionReasons.getValue().getSet(null).complement();
                for (int cp : explainedSet.codePoints()) {
                    if (Objects.equals(oldProperty.getValue(cp), newProperty.getValue(cp))) {
                        throw new IllegalArgumentException(
                                property
                                        + " has the same value "
                                        + oldProperty.getValue(cp)
                                        + " for U+"
                                        + Utility.hex(cp)
                                        + " in "
                                        + oldVersion
                                        + " and "
                                        + newVersion
                                        + ", so it does not require the explanation "
                                        + versionReasons.getValue().getValue(cp));
                    }
                    for (final String reason : versionReasons.getValue().getValue(cp)) {
                        linkifyReason(reason);
                    }
                }
            }
        }
    }

    public static String linkifyReason(String reason) {
        if (Ætiologer.L2_REF.matcher(reason).matches()) {
            return "<a style=white-space:nowrap href=https://www.unicode.org/cgi-bin/GetL2Ref.pl?"
                    + reason
                    + ">"
                    + reason
                    + "</a>";
        } else if (Ætiologer.L2_DOC.matcher(reason).matches()) {
            return "<a style=white-space:nowrap href=https://www.unicode.org/cgi-bin/GetMatchingDocs.pl?"
                    + reason
                    + ">"
                    + reason
                    + "</a>";
        }
        var matcher = Ætiologer.PRI.matcher(reason);
        if (matcher.matches()) {
            return "<a style=white-space:nowrap href=https://www.unicode.org/review/pri"
                    + matcher.group(1)
                    + "/feedback.html#"
                    + feedbackAnchor(matcher.group(2))
                    + ">PRI-"
                    + matcher.group(1)
                    + "<wbr>#"
                    + feedbackID(matcher.group(2))
                    + "</a>";
        }
        matcher = Ætiologer.PUBREV_DOC.matcher(reason);
        if (matcher.matches()) {
            return "<a style=white-space:nowrap href=https://www.unicode.org/L2/L20"
                    + matcher.group(1)
                    + "/"
                    + matcher.group(1)
                    + matcher.group(2)
                    + "-pubrev.html#"
                    + feedbackAnchor(matcher.group(3))
                    + ">L2/"
                    + matcher.group(1)
                    + "-"
                    + matcher.group(2)
                    + "#"
                    + feedbackID(matcher.group(3))
                    + "</a>";
        }
        throw new IllegalArgumentException(reason);
    }

    public static String feedbackAnchor(String dateOrId) {
        if (dateOrId.startsWith("ID")) {
            return dateOrId;
        } else {
            return ":~:text=" + dateOrId.replace(" ", "%20");
        }
    }

    public static String feedbackID(String dateOrId) {
        if (dateOrId.startsWith("ID")) {
            return dateOrId;
        } else {
            return "ID"
                    + DateTimeFormatter.ofPattern("EEE MMM d HH:mm:ss zzz yyyy", Locale.ENGLISH)
                            .parse(dateOrId, LocalDateTime::from)
                            .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        }
    }
}
