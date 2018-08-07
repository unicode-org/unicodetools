package org.unicode.tools.emoji;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.xerces.impl.dv.util.Base64;
import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.UnicodeSetPrettyPrinter;
import org.unicode.tools.emoji.Emoji.Source;
import org.unicode.tools.emoji.GenerateEmoji.Style;
import org.unicode.tools.emoji.GenerateEmoji.Visibility;
import org.unicode.utilities.UnicodeSetFormatter;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.VersionInfo;

public class EmojiImageData {
    static final Map<Emoji.Source, UnicodeMap<EmojiImageData>> DATA = new ConcurrentHashMap<>();
    static final Map<String, String> IMAGE_CACHE = new ConcurrentHashMap<>();

    private final String fileName;

    private EmojiImageData(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public String toString() {
        return fileName;
    }

    public static UnicodeSet getSupported(Source source) {
        return load(source).keySet();
    }

    public static String getFileName(Source source, String cp) {
        final EmojiImageData data = load(source).get(cp);
        return data == null ? null : data.fileName;
    }

    public static String getFileName(Source source, int cp) {
        final EmojiImageData data = load(source).get(cp);
        return data == null ? null : data.fileName;
    }

    public static String getDataUrl(Source source, String cp) {
        final EmojiImageData data = load(source).get(cp);
        if (data == null) {
            return null;
        }
        return getDataUrlFromFilename(source, data.fileName);
    }

    public static String getDataUrl(Source source, int cp) {
        final EmojiImageData data = load(source).get(cp);
        if (data == null) {
            return null;
        }
        return getDataUrlFromFilename(source, data.fileName);
    }

    private static UnicodeMap<EmojiImageData> load(Source source) {
        UnicodeMap<EmojiImageData> haveData = DATA.get(source);
        if (haveData != null) {
            return haveData;
        }
        haveData = new UnicodeMap<EmojiImageData>();
        for (String cp : EmojiData.EMOJI_DATA.getAllEmojiWithoutDefectives()) {
            final String filename = source.getImageFileName(cp);
            final File file = new File(source.getImageDirectory(), filename);
            if (file.exists()) {
                haveData.put(cp, new EmojiImageData(filename));
            }
        }
        DATA.put(source, haveData.freeze());
        return haveData;
    }

    static String getDataUrlFromFilename(Source source, String filename) {
        if (source == Source.google && filename.contains("1f468_1f3fb_200d_1f9b0")) {
            int debug = 0;
        }
        try {
            String result = EmojiImageData.IMAGE_CACHE.get(filename);
            if (result == null) {
                final File file = new File(source.getImageDirectory(), filename);
                if (!file.exists()) {
                    result = "";
                } else if (!GenerateEmoji.DATAURL) {
                    result = "data:image/gif;base64,R0lGODlhAQABAPAAABEA3v///yH5BAAAAAAALAAAAAABAAEAAAICRAEAOw=="; 
                    // "../images/" + filename;
                } else if (source == Source.svg) {
                    result = CollectionUtilities.join(Files.readAllLines(file.toPath()), "\n");
                } else {
                    byte[] bytes = GenerateEmoji.RESIZE_IMAGE <= 0 ? Files.readAllBytes(file.toPath())
                            : LoadImage.resizeImage(file, GenerateEmoji.RESIZE_IMAGE, GenerateEmoji.RESIZE_IMAGE);
                    result = "data:image/png;base64," + Base64.encode(bytes);
                }
                EmojiImageData.IMAGE_CACHE.put(filename, result);
            }
            return result.isEmpty() ? null : result;
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static void write(Set<Source> platforms2) throws IOException {
        try (PrintWriter outText = FileUtilities.openUTF8Writer(Emoji.TR51_INTERNAL_DIR, "missing-emoji-list.txt")) {
            showText(outText, 100);
        }

        final String outFileName = "missing-emoji-list.html";
        try (PrintWriter out = FileUtilities.openUTF8Writer(Emoji.TR51_INTERNAL_DIR, outFileName)) {
            GenerateEmoji.writeHeader(outFileName, out, "Missing", null, false, "<p>Missing list of emoji characters.</p>\n", Emoji.DATA_DIR_PRODUCTION, Emoji.TR51_HTML);
            out.println("<table " + "border='1'" + ">");
            String headerRow = "<tr><th>Type</th>";
            for (Emoji.Source type : platforms2) {
                headerRow += "<th class='centerTop' width='" + (90.0 / platforms2.size()) + "%'>" + type + " missing</th>";
            }
            headerRow += "</tr>";

            for (Breakdown breakdown : getBreakdown()) {
                showDiff(out, headerRow, platforms2, breakdown);
            }

            out.println("</table>");
            GenerateEmoji.writeFooter(out);
        } catch (java.lang.IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    private static void showDiff(PrintWriter out, String headerRow, Set<Source> platforms2, Breakdown breakdown) {
        // find common
        UnicodeSet common = null;
        boolean skipSeparate = true;
        for (Emoji.Source source : platforms2) {
            final UnicodeSet us = breakdown.getMissing(source);
            if (common == null) {
                common = new UnicodeSet(us);
            } else if (!common.equals(us)) {
                common.retainAll(us);
                skipSeparate = false;
            }
        }
        // per source
        String sectionLink = GenerateEmoji.getDoubleLink(breakdown.title);
        final GenerateEmojiData.PropPrinter propPrinter = new GenerateEmojiData.PropPrinter().set(GenerateEmoji.EXTRA_NAMES);
        String title = breakdown.title;

        if (!skipSeparate) {
            out.println(headerRow);
            out.print("<tr><th>" + sectionLink + " count</th>");
            for (Emoji.Source source : platforms2) {
                final UnicodeSet us = breakdown.getMissing(source);
                out.print("<td class='centerTop'>" + (us.size() - common.size()) + "</td>");
            }
            out.print("</tr>");
            out.print("<tr><th>" + title + " chars</th>");
            for (Emoji.Source source : platforms2) {
                final UnicodeSet us = breakdown.getMissing(source);
                final UnicodeSet missing = new UnicodeSet(us).removeAll(common);
                GenerateEmoji.displayUnicodeSet(out, missing.addAllTo(new TreeSet<String>(GenerateEmoji.EMOJI_COMPARATOR)), Style.bestImage, 0, 1, 1, "../../emoji/charts/full-emoji-list.html", "", "lchars", Visibility.external);
            }
            out.print("</tr>");
        }
        // common
        if (common.size() != 0) {
            out.println("<tr><th>Common</th>"
                    + "<th class='cchars' colSpan='" + platforms2.size() + "'>"
                    + "common missing</th></tr>");
            out.println("<tr><th>" + sectionLink + " count</th>"
                    + "<td class='cchars' colSpan='" + platforms2.size() + "'>"
                    + common.size() + "</td></tr>");
            out.println("<tr><th>" + title + "</th>");
            GenerateEmoji.displayUnicodeSet(out, common.addAllTo(new TreeSet<String>(GenerateEmoji.EMOJI_COMPARATOR)), Style.bestImage, 0, platforms2.size(), 1, null, "", "lchars", Visibility.external);
            out.println("</td></tr>");
        }
    }

    private static final class Breakdown {
        final String title;
        final UnicodeSet uset;

        public Breakdown(String title, UnicodeSet uset) {
            this.title = title;
            this.uset = uset;
        }

        static void add(List<Breakdown> result, String title, VersionInfo version, UnicodeSet v3, UnicodeSet v4) {
            final UnicodeSet old = new UnicodeSet(v3).retainAll(v4).freeze();
            title += "\tv" + version.getVersionString(2, 2);
            if (version.equals(Emoji.VERSION_LAST_RELEASED)) {
                result.add(new Breakdown(title, old));
            } else {
                result.add(new Breakdown(title, new UnicodeSet(v4).removeAll(old).freeze()));
            }
        }

        public UnicodeSet getSupported(Source source) {
            return new UnicodeSet(uset).retainAll(EmojiImageData.getSupported(source)).freeze();
        }
        public UnicodeSet getMissing(Source source) {
            return new UnicodeSet(uset).removeAll(EmojiImageData.getSupported(source)).freeze();
        }
    }

    private static List<Breakdown> getBreakdown() {
        EmojiData last = EmojiData.of(Emoji.VERSION_LAST_RELEASED);
        EmojiData current = EmojiData.of(Emoji.VERSION_BETA);

        List<Breakdown> result = new ArrayList<>();
        for (VersionInfo version : Arrays.asList(Emoji.VERSION_LAST_RELEASED, Emoji.VERSION_BETA)) {
            Breakdown.add(result, "singletons", version, last.getSingletonsWithoutDefectives(), current.getSingletonsWithoutDefectives());
            Breakdown.add(result, "keycaps", version, last.getKeycapSequences(), current.getKeycapSequences());
            Breakdown.add(result, "flags", version, last.getFlagSequences(), current.getFlagSequences());
            Breakdown.add(result, "tags", version, last.getTagSequences(), current.getTagSequences());
            Breakdown.add(result, "modifiers", version, last.getModifierSequences(), current.getModifierSequences());
            Breakdown.add(result, "zwj", version, last.getZwjSequencesNormal(), current.getZwjSequencesNormal());
        }
        return result;
    }

    public static void main(String[] args) throws IOException {
        write(Emoji.Source.VENDOR_SOURCES);
        //        try (PrintWriter out = new PrintWriter(System.out)) {
        //            showText(out, 50);
        //        }
    }

    private static void showText(PrintWriter out, int MAX) {
        for (Source source : Source.values()) {
            final UnicodeSet supported = getSupported(source);
            //System.out.println(source + "\t" + supported.size() + "\t" + max(supported.toPattern(false), MAX));
            if (supported.isEmpty() || !Source.VENDOR_SOURCES.contains(source)) {
                continue;
            }
            EmojiData last = EmojiData.of(Emoji.VERSION_LAST_RELEASED);
            UnicodeSet missing = new UnicodeSet(supported);
            Output<Boolean> printed = new Output<>(false);
            List<Breakdown> breakdowns = getBreakdown();
            for (Breakdown breakdown : breakdowns) {
                UnicodeSet foundItems = getCounts(out, source, breakdown, MAX, printed);
                missing.removeAll(foundItems);
            }
            if (!missing.isEmpty()) {
                out.println("\tOthers: " + PRETTY_HEX.format(missing));
                printed.value = true;
            }

            if (printed.value) {
                out.println();
            }
            for (Breakdown breakdown : breakdowns) {
                UnicodeSet foundItems = getCounts(out, source, breakdown, -1, printed);
                missing.removeAll(foundItems);
            }
            if (!missing.isEmpty()) {
                out.println("\tOthers: " + PRETTY_HEX.format(missing));
            }

            if (printed.value) {
                out.println();
            }
            for (Breakdown breakdown : breakdowns) {
                UnicodeSet foundItems = getCounts(out, source, breakdown, -2, printed);
                missing.removeAll(foundItems);
            }
            if (!missing.isEmpty()) {
                out.println("\tOthers: " + formatFiles(source, missing));
            }

            if (printed.value) {
                out.println();
                out.println();
            }

            out.flush();
        }
    }

    static final UnicodeSetFormatter PRETTY_HEX = new UnicodeSetFormatter()
            .setRawToQuote(UnicodeSet.ALL_CODE_POINTS)
            .setQuoter(new Hexer('x').setDoEscape(x -> UnicodeSet.ALL_CODE_POINTS.contains(x)));

    static final UnicodeSetFormatter PRETTY_PLAIN = new UnicodeSetFormatter()
            .setRawToQuote(UnicodeSet.EMPTY)
            .setQuoter(new Hexer('x'));

    private static UnicodeSet getCounts(PrintWriter out, Source source, Breakdown breakdown, int MAX, Output<Boolean> printed) {
        String title = breakdown.title;
        UnicodeSet lastMissingSingletons = breakdown.getMissing(source); // new UnicodeSet(breakdown.uset).removeAll(getSupported(source));
        if (!lastMissingSingletons.isEmpty()) {
            out.println(source + "\t"
                    + title
                    + "\t" + lastMissingSingletons.size()
                    + "\t" + (MAX == -1 ? "hex\t" + PRETTY_HEX.format(lastMissingSingletons) 
                    : MAX == -2 ? "file\t" + formatFiles(source, lastMissingSingletons)
                    : "plain\t" + max(PRETTY_PLAIN.format(lastMissingSingletons), MAX)
                            )
                    );
            printed.value = true;
        }
        return breakdown.getSupported(source); // new UnicodeSet(breakdown.uset).retainAll(getSupported(source)).freeze();
    }

    private static String formatFiles(Source type, UnicodeSet lastMissingSingletons) {
        StringBuilder result = new StringBuilder();
        for (String s : lastMissingSingletons) {
            if (result.length() != 0) {
                result.append(", ");
            }
            String fixed = s.replace(Emoji.EMOJI_VARIANT_STRING,"");
            String core = Emoji.buildFileName(fixed, "_");
            String suffix = type.getSuffix();
            String filename = type.getPrefix() + "_" + core + suffix;
            result.append(filename);
        }
        return result.toString();
    }

    private static String max(String pattern, int maxLen) {
        if (pattern.codePointCount(0, pattern.length()) <= maxLen) {
            return pattern;
        }
        int maxOffset = pattern.offsetByCodePoints(0, maxLen);
        return pattern.substring(0,maxOffset) + "…";
    }
}
