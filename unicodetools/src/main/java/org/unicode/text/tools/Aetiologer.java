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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

public class Aetiologer {
    private static final Segmenter WORD_BREAK =
            LocalizedSegmenter.builder()
                    .setLocale(ULocale.ENGLISH)
                    .setSegmentationType(SegmentationType.WORD)
                    .build();
    public static final Pattern L2_REF = Pattern.compile("((\\d+)-[A-Z]\\d+[a-z]*)");
    private static final Pattern BRACKETED_L2_REF =
            Pattern.compile("\\[" + L2_REF.pattern() + "\\]");
    private static final Pattern TARGET_VERSION =
            Pattern.compile("Unicode\\s+(?:[Vv]ersion\\s+)?(\\d+(?:\\.\\d+)*)");
    private static final Pattern CODE_POINTS =
            Pattern.compile(
                    "(?:U\\+)?([0-9A-F]{4}|(?:[1-9A-F]|10)[0-9A-F]{4})(?:\\.\\.(?:U\\+)?([0-9A-F]{4}|(?:[1-9A-F]|10)[0-9A-F]{4}))?");
    private static final String RESOURCES =
            Settings.UnicodeTools.UNICODETOOLS_RSRC_DIR + "org/unicode/text/tools/";

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
        final Map<VersionInfo, Map<UcdProperty, UnicodeMap<List<String>>>> manualReasons =
                readReasonsFile("reasons_manual.txt");
        checkReasons(manualReasons);
        // Just reread the file instead of writing a deep copy…
        final Map<VersionInfo, Map<UcdProperty, UnicodeMap<List<String>>>> reasons =
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
                                    if (!manualReasons.containsKey(newerVersion)
                                            || !manualReasons
                                                    .get(newerVersion)
                                                    .containsKey(property)
                                            || manualReasons
                                                    .get(newerVersion)
                                                    .get(property)
                                                    .containsKey(cp)) {
                                        Δ.add(cp);
                                    }
                                }
                            }
                            reasons.computeIfAbsent(newerVersion, (v) -> new TreeMap<>())
                                    .computeIfAbsent(property, (p) -> new UnicodeMap<>())
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
            for (final var versionReasons : reasons.entrySet()) {
                final var version = versionReasons.getKey();
                for (final var propertyReasons : versionReasons.getValue().entrySet()) {
                    final var property = propertyReasons.getKey();
                    final var unicodeMap = propertyReasons.getValue();
                    for (final var value : unicodeMap.getAvailableValues()) {
                        if (value == null) {
                            continue;
                        }
                        if (value.isEmpty()) {
                            reasonsFile.println(
                                    version.getVersionString(2, 3)
                                            + " ; "
                                            + property
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
        for (final var versionReasons : reasons.entrySet()) {
            final var version = versionReasons.getKey();
            for (final var propertyReasons : versionReasons.getValue().entrySet()) {
                final var property = propertyReasons.getKey();
                final var unicodeMap = propertyReasons.getValue();
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
                            version.getVersionString(2, 3)
                                    + " ; "
                                    + property
                                    + " ; "
                                    + unicodeMap.getSet(value)
                                    + " ; "
                                    + value.stream().collect(Collectors.joining(", ")));
                }
            }
        }
        reasonsFile.close();
    }

    private static boolean AnalyseAction(
            String line,
            VersionInfo version,
            Set<String> aliases,
            Map<String, String> actions,
            Matcher l2Ref,
            Map<VersionInfo, Map<UcdProperty, UnicodeMap<List<String>>>> reasons,
            Map<VersionInfo, Map<UcdProperty, UnicodeMap<List<String>>>> manualReasons) {
        final var iup = IndexUnicodeProperties.make(version);
        // Skip 13.1.0, see https://github.com/unicode-org/unicodetools/issues/100.
        final var previous =
                version.equals(VersionInfo.UNICODE_14_0)
                        ? Utility.getVersionPreceding(Utility.getVersionPreceding(version))
                        : Utility.getVersionPreceding(version);
        final var previousIUP = IndexUnicodeProperties.make(previous);
        Iterable<Segment> words =
                WORD_BREAK.segment(line.split("\\]", 2)[1].replace("-", "_")).segments()::iterator;
        Set<UcdProperty> candidateProperties = new TreeSet<>();
        final var codePointsMentioned = new UnicodeSet();
        for (final var segment : words) {
            if (aliases.contains(segment.getSubSequence())) {
                candidateProperties.add(UcdProperty.forString((String) segment.getSubSequence()));
            }
            final var range = CODE_POINTS.matcher(segment.getSubSequence());
            if (range.matches()) {
                if (range.group(2) != null) {
                    codePointsMentioned.add(
                            Utility.codePointFromHex(range.group(1)),
                            Utility.codePointFromHex(range.group(2)));
                } else {
                    codePointsMentioned.add(Utility.codePointFromHex(range.group(1)));
                }
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
        if (candidateProperties.isEmpty() || codePointsMentioned.isEmpty()) {
            return false;
        }
        System.out.println(line);
        boolean isReason = false;
        properties:
        for (final var property : candidateProperties) {
            // TODO(egg): Horribly load-bearing.  We must read the latest files, because we use the new @missing lines for files that predate @missing lines, but we rely on them having already been loaded.
            IndexUnicodeProperties.make().load(property);
            final var newProperty = iup.getProperty(property);
            final var oldProperty = previousIUP.getProperty(property);
            for (int cp : codePointsMentioned.codePoints()) {
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
                            + oldProperty.getValue(codePointsMentioned.charAt(0))
                            + " vs. "
                            + newProperty.getValue(codePointsMentioned.charAt(0)));
            for (int cp : codePointsMentioned.codePoints()) {
                if (manualReasons.containsKey(version)
                        && manualReasons.get(version).containsKey(property)
                        && manualReasons.get(version).get(property).containsKey(cp)) {
                    continue;
                }
                isReason = true;
                final var unicodeMap =
                        reasons.computeIfAbsent(version, (v) -> new TreeMap<>())
                                .computeIfAbsent(property, (p) -> new UnicodeMap<>());
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
        return isReason;
    }

    private static Map<VersionInfo, Map<UcdProperty, UnicodeMap<List<String>>>> reasons = null;

    public static Map<VersionInfo, Map<UcdProperty, UnicodeMap<List<String>>>> getReasons()
            throws IOException {
        if (reasons == null) {
            reasons = readReasonsFile("reasons_auto.txt");
        }
        return reasons;
    }

    private static Map<VersionInfo, Map<UcdProperty, UnicodeMap<List<String>>>> readReasonsFile(
            String name) throws IOException {
        final Map<VersionInfo, Map<UcdProperty, UnicodeMap<List<String>>>> reasons =
                new TreeMap<>();
        final var file = new BufferedReader(new FileReader(new File(RESOURCES + name)));
        for (String line = file.readLine(); line != null; line = file.readLine()) {
            line = line.split("#", 2)[0].trim();
            if (line.isEmpty()) {
                continue;
            }
            final var fields = line.split("\\s*;\\s*");
            final var version = VersionInfo.getInstance(fields[0]);
            final var property = UcdProperty.forString(fields[1]);
            final var set = new UnicodeSet(fields[2]);
            final var actions = Arrays.asList(fields[3].split("\\s*,\\s*"));
            reasons.computeIfAbsent(version, v -> new TreeMap<>())
                    .computeIfAbsent(property, p -> new UnicodeMap<>())
                    .putAll(set, actions);
        }
        file.close();
        return reasons;
    }

    private static void checkReasons(
            Map<VersionInfo, Map<UcdProperty, UnicodeMap<List<String>>>> reasons) {
        for (final var versionReasons : reasons.entrySet()) {
            final var newVersion = versionReasons.getKey();
            final var newIUP = IndexUnicodeProperties.make(newVersion);
            // Skip 13.1.0, see https://github.com/unicode-org/unicodetools/issues/100.
            final var oldVersion =
                    newVersion.equals(VersionInfo.UNICODE_14_0)
                            ? Utility.getVersionPreceding(Utility.getVersionPreceding(newVersion))
                            : Utility.getVersionPreceding(newVersion);
            final var oldIUP = IndexUnicodeProperties.make(oldVersion);
            for (final var propertyReasons : versionReasons.getValue().entrySet()) {
                final var property = propertyReasons.getKey();
                final var newProperty = newIUP.getProperty(property);
                final var oldProperty = oldIUP.getProperty(property);
                final UnicodeSet explainedSet =
                        propertyReasons.getValue().getSet(null).complement();
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
                                        + propertyReasons.getValue().getValue(cp));
                    }
                }
            }
        }
    }
}
