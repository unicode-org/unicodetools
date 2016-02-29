package org.unicode.text.tools;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.text.tools.Emoji.Source;
import org.unicode.text.tools.GenerateEmoji.Style;

import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.UnicodeSet;

class EmojiStats {
    enum Type {
        carriers(GenerateEmoji.JCARRIERS),
        commonAdditions(Emoji.COMMON_ADDITIONS),
        flags(Emoji.FLAGS),
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
    static final Map<Emoji.Source, UnicodeSet> totalData;
    static final Map<Emoji.Source, UnicodeSet> extraData;
    private static final boolean SHOW = false;

    static {
        Map<Source, UnicodeSet> _totalData = new EnumMap<>(Emoji.Source.class);
        Map<Source, UnicodeSet> _extraData = new EnumMap<>(Emoji.Source.class);
        Map<Type, UnicodeSet> _allTypes = new EnumMap<>(Type.class);
        Map<EmojiStats.Type, Map<Emoji.Source, UnicodeSet>> _data = new EnumMap<>(EmojiStats.Type.class);
        for (Emoji.Source s : Emoji.Source.values()) {
            _totalData.put(s, new UnicodeSet());
            _extraData.put(s, new UnicodeSet());
        }
        totalData = Collections.unmodifiableMap(_totalData);
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
            UnicodeSet us = _totalData.get(source);
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

    //    public void add(
    //            String chars,
    //            Emoji.Source source,
    //            String missingCell) {
    //        if (missingCell.contains("miss")) {
    //
    //            // per type
    //            EmojiStats.Type type = Type.getType(chars);
    //            // VERSION70.containsAll(chars) ? Type.v70
    //            // :
    //            // getFlagCode(chars) != null ? Type.countries
    //            // : DOMINOS.containsAll(chars) ? Type.dominos
    //            // : CARDS.containsAll(chars) ? Type.cards
    //            // : MAHJONG.containsAll(chars) ? Type.majong
    //            // : Type.misc;
    //            EnumMap<Emoji.Source, UnicodeSet> counter = data.get(type);
    //            if (counter == null) {
    //                data.put(type, counter = new EnumMap<Emoji.Source, UnicodeSet>(Emoji.Source.class));
    //            }
    //            UnicodeSet us = counter.get(source);
    //            if (us == null) {
    //                counter.put(source, us = new UnicodeSet());
    //            }
    //            us.add(chars);
    //        } else {
    //            // total data
    //            totalData.get(source).add(chars);
    //        }
    //    }

    public void write(Set<Source> platforms2) throws IOException {
        final boolean extraPlatforms = false;
        PrintWriter out = BagFormatter.openUTF8Writer(extraPlatforms ? GenerateEmoji.INTERNAL_OUTPUT_DIR : Emoji.TR51_INTERNAL_DIR,
                "missing-emoji-list.html");
        PrintWriter outText = BagFormatter.openUTF8Writer(extraPlatforms ? GenerateEmoji.INTERNAL_OUTPUT_DIR : Emoji.TR51_INTERNAL_DIR,
                "missing-emoji-list.txt");
        UnicodeSet jc = GenerateEmoji.JCARRIERS;
        // new UnicodeSet()
        // .addAll(totalData.get(Source.sb))
        // .addAll(totalData.get(Source.kddi))
        // .addAll(totalData.get(Source.dcm))
        // .freeze();
        UnicodeSet textStyle = new UnicodeSet();
        for (String s : EmojiData.EMOJI_DATA.getChars()) {
            if (Style.valueOf(EmojiData.EMOJI_DATA.getData(s).style) == Style.text) {
                textStyle.add(s);
            }
        }
        UnicodeSet needsVS = new UnicodeSet();
        for (String s : jc) {
            int first = s.codePointAt(0);
            if (!Emoji.HAS_EMOJI_VS.contains(first) && textStyle.contains(first)) {
                needsVS.add(first);
            }
        }

        if (SHOW) System.out.println("All Emoji\t" + EmojiData.EMOJI_DATA.getChars().toPattern(false));

        if (SHOW) System.out.println("needs VS\t" + needsVS.toPattern(false));

        if (SHOW) System.out.println("gmail-jc\t"
                + new UnicodeSet(totalData.get(Emoji.Source.gmail)).removeAll(jc).toPattern(false));
        if (SHOW) System.out.println("jc-gmail\t"
                + new UnicodeSet(jc).removeAll(totalData.get(Emoji.Source.gmail)).toPattern(false));

        for (Entry<Emoji.Source, UnicodeSet> entry : totalData.entrySet()) {
            if (SHOW) System.out.println(entry.getKey() + "\t" + entry.getValue().toPattern(false));
        }

        GenerateEmoji.writeHeader(out, "Missing", null, "<p>Missing list of emoji characters.</p>\n", "border='1'", true);
        String headerRow = "<tr><th>Type</th>";
        for (Emoji.Source type : platforms2) {
            headerRow += "<th class='centerTop' width='" + (80.0 / platforms2.size()) + "%'>" + type + " missing</th>";
        }
        headerRow += "</tr>";

        for (Entry<EmojiStats.Type, Map<Emoji.Source, UnicodeSet>> entry : data.entrySet()) {
            showDiff(out, outText, headerRow, entry.getKey().toString(), entry.getValue(), platforms2);
        }

        //            if (!extraPlatforms) {
        //                EnumMap<Emoji.Source, UnicodeSet> familyMap = addItems(GenerateEmoji.APPLE_COMBOS, platforms2);
        //                showDiff(out, outText, headerRow, "families", familyMap, platforms2);
        //
        //                EnumMap<Emoji.Source, UnicodeSet> diversityMap = addItems(GenerateEmoji.emojiData.getModifierSequences(), platforms2);
        //                showDiff(out, outText, headerRow, "skinTone", diversityMap, platforms2);
        //            }
        GenerateEmoji.writeFooter(out, "");
        out.close();
        outText.close();
    }

    //    private EnumMap<Emoji.Source, UnicodeSet> addItems(UnicodeSet unicodeSet, Set<Source> platforms2) {
    //        EnumMap<Emoji.Source, UnicodeSet> familyMap = new EnumMap<>(Emoji.Source.class);
    //        for (String s : unicodeSet) {
    //            for (Source platform : platforms2) {
    //                if (GenerateEmoji.getImage(platform,s,false,null) == null) {
    //                    UnicodeSet set = familyMap.get(platform);
    //                    if (set == null) {
    //                        familyMap.put(platform, set = new UnicodeSet());
    //                    }
    //                    set.add(s);
    //                }
    //            }
    //        }
    //        return familyMap;
    //    }

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
        String sectionLink = GenerateEmoji.getDoubleLink(title);
        final GenerateEmojiData.PropPrinter propPrinter = new GenerateEmojiData.PropPrinter().set(GenerateEmoji.EXTRA_NAMES);

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
                GenerateEmoji.displayUnicodeSet(out, missing, Style.bestImage, 0, 1, 1, "../../emoji/charts/full-emoji-list.html", GenerateEmoji.EMOJI_COMPARATOR, true);
                outText.println(source + "\t" + missing.size());
                propPrinter.show(outText, source+"", 14, 14, us, true);
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
            GenerateEmoji.displayUnicodeSet(out, common, Style.bestImage, 0, platforms2.size(), 1, null, GenerateEmoji.EMOJI_COMPARATOR, true);
            out.println("</td></tr>");
            outText.println("common \t" + common.size());
            propPrinter.show(outText, "common", 14, 14, common, true);
        }
    }
    public static void main(String[] args) {
//        for (Entry<Source, UnicodeSet> entry : totalData.entrySet()) {
//            System.out.println(entry.getKey() + "\t" + entry.getValue());
//        }
//        for (Entry<Type, Map<Source, UnicodeSet>> entry : data.entrySet()) {
//            for (Entry<Source, UnicodeSet> entry2 : entry.getValue().entrySet()) {
//                System.out.println(entry.getKey() + "\t" + entry2.getKey() + "\t" + entry2.getValue());
//            }
//        }
        
        final UnicodeSet missingSamsung = new UnicodeSet(EmojiData.EMOJI_DATA.getAllEmojiWithoutDefectives())
        .removeAll(EmojiData.EMOJI_DATA.getModifierSequences())
        .removeAll(totalData.get(Source.samsung));
        System.out.println("\nSamsung missing: " + missingSamsung.size() + "\t" + missingSamsung.toPattern(false) + "\n");
        for (String cp : missingSamsung) {
            show(Source.samsung, cp);
        }

        UnicodeSet samsungExtras = extraData.get(Source.samsung);
        System.out.println("\nSamsung extras: " + samsungExtras.size() + "\t" + samsungExtras.toPattern(false) + "\n");
        for (String cp : samsungExtras) {
            show(Source.samsung, cp);
        }
    }

    private static void show(Source source, String cp) {
        System.out.println(
                source.getPrefix() + "_" + Emoji.buildFileName(cp, "_") + ".png"
                + " ;\t" + cp 
                + " ;\tv" + Emoji.getNewest(cp).getShortName() 
                + " ;\t" + Emoji.getName(cp, false, null));
    }
}