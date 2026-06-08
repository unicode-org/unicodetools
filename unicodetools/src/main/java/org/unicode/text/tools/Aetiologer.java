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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.unicode.props.DerivedPropertyStatus;
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
    private static final Pattern UTC_DESIGNATION = Pattern.compile("\\[(\\d+-[A-Z]\\d+[a-z]*)\\]");
    private static final Pattern TARGET_VERSION =
            Pattern.compile("Unicode\\s+(?:[Vv]ersion\\s+)?(\\d+(?:\\.\\d+)*)");
    private static final Pattern CODE_POINTS =
            Pattern.compile(
                    "(?:U\\+)?([0-9A-F]{4}|(?:[1-9A-F]|10)[0-9A-F]{4})(?:\\.\\.(?:U\\+)?([0-9A-F]{4}|(?:[1-9A-F]|10)[0-9A-F]{4}))?");

    public static final void main(String[] args) throws IOException {
        final Set<String> aliases = new HashSet<>();
        for (final var property : UcdProperty.values()) {
            if (property.getDerivedStatus() == DerivedPropertyStatus.Approved) {
                for (final var name : property.getNames().getAllNames()) {
                    aliases.add(name);
                }
            }
        }
        final String resources =
                Settings.UnicodeTools.UNICODETOOLS_RSRC_DIR + "org/unicode/text/tools/";
        final var actionsFile =
                new BufferedReader(new FileReader(new File(resources + "actions.txt")));
        final Map<String, String> actions = new HashMap<>();
        final Map<VersionInfo, Map<UcdProperty, UnicodeMap<List<String>>>> reasons =
                new TreeMap<>();
        for (String line = actionsFile.readLine(); line != null; line = actionsFile.readLine()) {
            final var designation = UTC_DESIGNATION.matcher(line);
            if (!designation.find()) {
                continue;
            }
            final var target = TARGET_VERSION.matcher(line);
            if (!target.find()) {
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
            final var iup = IndexUnicodeProperties.make(version);
            // Skip 13.1.0, see https://github.com/unicode-org/unicodetools/issues/100.
            final var previous =
                    version.equals(VersionInfo.UNICODE_14_0)
                            ? Utility.getVersionPreceding(Utility.getVersionPreceding(version))
                            : Utility.getVersionPreceding(version);
            final var previousIUP = IndexUnicodeProperties.make(previous);
            Iterable<Segment> words =
                    WORD_BREAK.segment(line.split("\\]", 2)[1].replace("-", "_")).segments()
                            ::iterator;
            Set<UcdProperty> candidateProperties = new TreeSet<>();
            final var codePointsMentioned = new UnicodeSet();
            for (final var segment : words) {
                if (aliases.contains(segment.getSubSequence())) {
                    candidateProperties.add(
                            UcdProperty.forString((String) segment.getSubSequence()));
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
                continue;
            }
            System.out.println(line);
            properties:
            for (final var property : candidateProperties) {
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
                System.out.println("    Could be " + property);
                actions.put(designation.group(1), line);
                for (int cp : codePointsMentioned.codePoints()) {
                    final var unicodeMap =
                            reasons.computeIfAbsent(version, (v) -> new TreeMap<>())
                                    .computeIfAbsent(property, (p) -> new UnicodeMap<>());
                    List<String> reasonList = unicodeMap.get(cp);
                    if (reasonList == null) {
                        reasonList = new ArrayList<>();
                    } else {
                        reasonList = new ArrayList<>(reasonList);
                    }
                    reasonList.add(designation.group(1));
                    unicodeMap.put(cp, reasonList);
                }
            }
        }
        actionsFile.close();
        final var reasonsFile = new PrintStream(new File(resources + "reasons.txt"));
        for (final var versionReasons : reasons.entrySet()) {
            final var version = versionReasons.getKey();
            for (final var propertyReasons : versionReasons.getValue().entrySet()) {
                final var property = propertyReasons.getKey();
                final var unicodeMap = propertyReasons.getValue();
                List<String> previousActions = List.of();
                for (int i = 0; i < unicodeMap.getRangeCount(); ++i) {
                    if (unicodeMap.getRangeValue(i) == null) {
                        continue;
                    }
                    if (!previousActions.equals(unicodeMap.getRangeValue(i))) {
                        for (var action : unicodeMap.getRangeValue(i)) {
                            reasonsFile.println("# " + actions.get(action));
                        }
                    }
                    previousActions = unicodeMap.getRangeValue(i);
                    reasonsFile.println(
                            version.getVersionString(2, 3)
                                    + " ; "
                                    + property
                                    + " ; "
                                    + (unicodeMap.getRangeStart(i) == unicodeMap.getRangeEnd(i)
                                            ? Utility.hex(unicodeMap.getRangeStart(i))
                                            : (Utility.hex(unicodeMap.getRangeStart(i))
                                                    + ".."
                                                    + Utility.hex(unicodeMap.getRangeEnd(i))))
                                    + " ; "
                                    + unicodeMap.getRangeValue(i).stream()
                                            .collect(Collectors.joining(" ")));
                }
            }
        }
        reasonsFile.close();
    }
}
