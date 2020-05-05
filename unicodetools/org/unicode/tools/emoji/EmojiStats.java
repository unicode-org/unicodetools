package org.unicode.tools.emoji;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.tools.emoji.Emoji.Source;
import org.unicode.tools.emoji.GenerateEmoji.Style;
import org.unicode.tools.emoji.GenerateEmoji.Visibility;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.UnicodeSet;

class EmojiStats {
    enum Type {
        carriers(EmojiData.JCARRIERS),
        commonAdditions(Emoji.COMMON_ADDITIONS),
        flags(EmojiData.EMOJI_DATA.getFlagSequences()),
        other(UnicodeSet.EMPTY),
        modifierSequences(EmojiData.EMOJI_DATA.getModifierSequences()),
        zwjSequences(EmojiData.EMOJI_DATA.getZwjSequencesNormal()),
        //            standardAdditions8(nc8)
        ;
        final UnicodeSet items;

        Type(UnicodeSet _items) {
            items = _items;
        }

        static EmojiStats.Type getType(String chars) {
            for (EmojiStats.Type t : Type.values()) {
                if (t.items.contains(chars)) {
                    return t;
                }
            }
            return other;
        }
    }

    // static final UnicodeSet DOMINOS = new UnicodeSet("[🀰-🂓]");
    // static final UnicodeSet CARDS = new UnicodeSet("[🂠-🃵]");
    // static final UnicodeSet MAHJONG = new UnicodeSet("[🀀-🀫]");
    static final Map<EmojiStats.Type, Map<Emoji.Source, UnicodeSet>> data;
    static final Map<Emoji.Source, UnicodeSet> totalMissingData;
    static final Map<Emoji.Source, UnicodeSet> extraData;
    private static final boolean SHOW = false;

    static {
        Map<Source, UnicodeSet> _totalMissingData = new EnumMap<>(Emoji.Source.class);
        Map<Source, UnicodeSet> _extraData = new EnumMap<>(Emoji.Source.class);
        Map<Type, UnicodeSet> _allTypes = new EnumMap<>(Type.class);
        Map<EmojiStats.Type, Map<Emoji.Source, UnicodeSet>> _data = new EnumMap<>(EmojiStats.Type.class);
        for (Emoji.Source s : Emoji.Source.values()) {
            _totalMissingData.put(s, new UnicodeSet());
            _extraData.put(s, new UnicodeSet());
        }
        totalMissingData = Collections.unmodifiableMap(_totalMissingData);
        extraData = Collections.unmodifiableMap(_extraData);
        for (Type t : Type.values()) {
            _allTypes.put(t, new UnicodeSet());
            EnumMap<Source, UnicodeSet> subdata = new EnumMap<Source, UnicodeSet>(Source.class);
            for (Emoji.Source s : Emoji.Source.values()) {
                subdata.put(s, new UnicodeSet());
            }
            _data.put(t, Collections.unmodifiableMap(subdata));
        }
        data = Collections.unmodifiableMap(_data);

        for (File platform : new File(Emoji.IMAGES_OUTPUT_DIR).listFiles()) {
            final String platformString = platform.getName();
            if (platformString.startsWith(".")) {
                continue;
            }
            Source source;
            try {
                source = Source.valueOf(platformString.equals("android") ? "google" : platformString);
            } catch (Exception e) {
                if (SHOW) System.out.println("Skipping directory file " + platformString);
                continue;
            }
            UnicodeSet us = _totalMissingData.get(source);
            UnicodeSet extras = _extraData.get(source);

            Matcher matcher = Pattern.compile(platformString + "_(.*).(png|gif)").matcher("");
            for (File image : platform.listFiles()) {
                String imageString = image.getName();
                if (imageString.startsWith(".")) {
                    continue;
                }
                if (!matcher.reset(imageString).matches()) {
                    if (SHOW) System.out.println("No match — Skipping: " + image);
                    continue;
                }
                final String hexString = matcher.group(1);
                if (hexString.startsWith("x")) {
                    if (SHOW) System.out.println("X hex match — Skipping: " + image);
                    continue;
                }
                String cp;
                try {
                    cp = Utility.fromHex(hexString, 4, "_");
                } catch (Exception e) {
                    if (SHOW) System.out.println("Bad hex — Skipping: " + image);
                    continue;
                }
                if (!EmojiData.EMOJI_DATA.getAllEmojiWithoutDefectives().contains(cp)) {
                    extras.add(cp);
                    continue;
                }
                us.add(cp);
                EmojiStats.Type type = Type.getType(cp);
                _allTypes.get(type).add(cp);
                UnicodeSet us2 = _data.get(type).get(source);
                us2.add(cp);
            }
            us.freeze();
            extras.freeze();
        }

        for (Type t : Type.values()) {
            Map<Source, UnicodeSet> subdata = _data.get(t);
            UnicodeSet allTypes = _allTypes.get(t);
            allTypes.freeze();
            for (Emoji.Source s : Emoji.Source.values()) {
                UnicodeSet us = subdata.get(s);
                us.set(new UnicodeSet(allTypes).removeAll(us));
                us.freeze();
            }
        }
    }

    public void write(Set<Source> platforms2) throws IOException {
        final boolean extraPlatforms = false;
        final String outFileName = "missing-emoji-list.html";
        PrintWriter out = FileUtilities.openUTF8Writer(extraPlatforms ? Emoji.INTERNAL_OUTPUT_DIR : Emoji.TR51_INTERNAL_DIR, outFileName);
        PrintWriter outText = FileUtilities.openUTF8Writer(extraPlatforms ? Emoji.INTERNAL_OUTPUT_DIR : Emoji.TR51_INTERNAL_DIR, "missing-emoji-list.txt");
        UnicodeSet jc = EmojiData.JCARRIERS;
        // new UnicodeSet()
        // .addAll(totalData.get(Source.sb))
        // .addAll(totalData.get(Source.kddi))
        // .addAll(totalData.get(Source.dcm))
        // .freeze();
        UnicodeSet textStyle = EmojiData.EMOJI_DATA.getTextPresentationSet();
        UnicodeSet needsVS = new UnicodeSet();
        for (String s : jc) {
            int first = s.codePointAt(0);
            if (!EmojiData.EMOJI_DATA.getEmojiWithVariants().contains(first) && textStyle.contains(first)) {
                needsVS.add(first);
            }
        }

        if (SHOW) System.out.println("All Emoji\t" + EmojiData.EMOJI_DATA.getChars().toPattern(false));

        if (SHOW) System.out.println("needs VS\t" + needsVS.toPattern(false));

        if (SHOW) System.out.println("gmail-jc\t"
                + new UnicodeSet(totalMissingData.get(Emoji.Source.gmail)).removeAll(jc).toPattern(false));
        if (SHOW) System.out.println("jc-gmail\t"
                + new UnicodeSet(jc).removeAll(totalMissingData.get(Emoji.Source.gmail)).toPattern(false));

        for (Entry<Emoji.Source, UnicodeSet> entry : totalMissingData.entrySet()) {
            if (SHOW) System.out.println(entry.getKey() + "\t" + entry.getValue().toPattern(false));
        }

        ChartUtilities.writeHeader(outFileName, out, "Missing", null, false, "<p>Missing list of emoji characters.</p>\n", Emoji.DATA_DIR_PRODUCTION, Emoji.TR51_HTML);
        out.println("<table " + "border='1'" + ">");
        String headerRow = "<tr><th>Type</th>";
        for (Emoji.Source type : platforms2) {
            headerRow += "<th class='centerTop' width='" + (80.0 / platforms2.size()) + "%'>" + type + " missing</th>";
        }
        headerRow += "</tr>";

        for (Entry<EmojiStats.Type, Map<Emoji.Source, UnicodeSet>> entry : data.entrySet()) {
            showDiff(out, outText, headerRow, entry.getKey().toString(), entry.getValue(), platforms2);
        }

        out.println("</table>");
        ChartUtilities.writeFooter(out);
        out.close();
        outText.close();
    }

    private void showDiff(PrintWriter out, PrintWriter outText, String headerRow, final String title, 
            final Map<Emoji.Source, UnicodeSet> values, Set<Source> platforms2) {
        // find common
        UnicodeSet common = null;
        boolean skipSeparate = true;
        for (Emoji.Source source : platforms2) {
            final UnicodeSet uset = values.get(source);
            final UnicodeSet us = org.unicode.text.utility.Utility.ifNull(uset, UnicodeSet.EMPTY);
            if (common == null) {
                common = new UnicodeSet(us);
            } else if (!common.equals(us)) {
                common.retainAll(us);
                skipSeparate = false;
            }
        }

        // per source
        String sectionLink = ChartUtilities.getDoubleLink(title);
        final GenerateEmojiData.PropPrinter propPrinter = new GenerateEmojiData.PropPrinter().set(EmojiDataSourceCombined.EMOJI_DATA);

        if (!skipSeparate) {
            out.println(headerRow);
            outText.println(title);
            out.print("<tr><th>" + sectionLink + " count</th>");
            sectionLink = title;
            for (Emoji.Source source : platforms2) {
                final UnicodeSet us = org.unicode.text.utility.Utility.ifNull(values.get(source), UnicodeSet.EMPTY);
                out.print("<td class='centerTop'>" + (us.size() - common.size()) + "</td>");
            }
            out.print("</tr>");
            out.print("<tr><th>" + title + " chars</th>");
            for (Emoji.Source source : platforms2) {
                final UnicodeSet us = org.unicode.text.utility.Utility.ifNull(values.get(source), UnicodeSet.EMPTY);
                final UnicodeSet missing = new UnicodeSet(us).removeAll(common);
                GenerateEmoji.displayUnicodeSet(out, missing.addAllTo(new TreeSet<String>(GenerateEmoji.EMOJI_COMPARATOR)), Style.bestImage, 0, 1, 1, "../../emoji/charts/full-emoji-list.html", "", "lchars", Visibility.external);
                outText.println(source + "\t" + missing.size());
                propPrinter.show(outText, source+"", null, 14, 14, us, true, false, false);
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
            outText.println("common \t" + common.size());
            propPrinter.show(outText, "common", null, 14, 14, common, true, false, false);
        }
    }
    public static void main(String[] args) {
        
        final UnicodeSet missingSamsung = new UnicodeSet(EmojiData.EMOJI_DATA.getAllEmojiWithoutDefectives())
        .removeAll(EmojiData.EMOJI_DATA.getModifierSequences())
        .removeAll(totalMissingData.get(Source.samsung));
        System.out.println("\nSamsung missing: " + missingSamsung.size() + "\t" + missingSamsung.toPattern(false) + "\n");
        for (String cp : missingSamsung) {
            show(Source.samsung, cp);
        }

        UnicodeSet samsungExtras = extraData.get(Source.samsung);
        System.out.println("\nSamsung extras: " + samsungExtras.size() + "\t" + samsungExtras.toPattern(false) + "\n");
        for (String cp : samsungExtras) {
            show(Source.samsung, cp);
        }
        
        for (String cp : totalMissingData.get(Source.google)) {
            show(Source.google, cp);
        }
    }

    private static void show(Source source, String cp) {
        System.out.println(
                source.getPrefix() + "_" + Emoji.buildFileName(cp, "_") + ".png"
                + " ;\t" + cp 
                + " ;\tv" + BirthInfo.getYear(cp)
                + " ;\t" + EmojiData.EMOJI_DATA.getName(cp));
    }
}