package org.unicode.tools;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.ChainedMap;
import org.unicode.cldr.util.ChainedMap.M3;
import org.unicode.cldr.util.Counter;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.text.utility.Settings;

import com.google.common.base.Splitter;
import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.MultiComparator;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UTF16.StringComparator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;


public class Ids {

    private static final boolean DEBUG = false;

    private static final Comparator UNIHAN_RAW = Collator.getInstance(ULocale.forLanguageTag("zh-u-co-unihan")).freeze();
    private static final Comparator<String> UNIHAN = new MultiComparator<String>(UNIHAN_RAW, new StringComparator(true,false,0));
    private static final IndexUnicodeProperties iup = IndexUnicodeProperties.make(Settings.latestVersion);
    private static final UnicodeMap<String> cjkRadical = iup.load(UcdProperty.CJK_Radical);
    private static final UnicodeMap<String> totalStrokes = iup.load(UcdProperty.kTotalStrokes);
    private static final UnicodeMap<String> radicalStroke = iup.load(UcdProperty.kRSUnicode);
    
    static final UnicodeSet IDS = new UnicodeSet("[[:IDS_Binary_Operator:][:IDS_Trinary_Operator:]]").freeze();
    static final UnicodeSet STROKES = new UnicodeSet("[㇀-㇣]").freeze();
    static final UnicodeSet EXT = new UnicodeSet(0xE000,0xE07F).freeze();
    static final UnicodeSet EXT_E = new UnicodeSet(0x2B820,0x2CEA1).freeze();
    static final UnicodeSet IDEOGRAPHIC = new UnicodeSet("[:Ideographic:]").freeze();
    static final UnicodeSet RADICAL = new UnicodeSet("[:Radical:]").freeze();

    static final class Positioning {
        static final Positioning BASE = new Positioning(0,0,1,1);
        final double x1;
        final double y1;
        final double x2;
        final double y2;

        public Positioning(double x, double y, double x2, double y2) {
            this.x1 = x;
            this.y1 = y;
            this.x2 = x2;
            this.y2 = y2;
        }

        public Positioning times(Positioning other) {
            final double x = x1 + other.x1 - x1*other.x1;
            final double y = y1 + other.y1 - y1*other.y1;
            final double width = (x2-x1) * (other.x2-other.x1);
            final double height = (y2-y1) * (other.y2-other.y1);
            return new Positioning(
                    x,
                    y,
                    x + width,
                    y + height);
        }

        @Override
        public String toString() {
            return "{" + (int)(100*x1) 
                    + ", " + (int)(100*y1) 
                    + "; " + (int)(100*x2) 
                    + ", " + (int)(100*y2)
                    + "}";
        }
    }

    static final class IdsData {
        private static final IdsData IDEO = new IdsData("IDEO", "", null);

        final String sample;
        final String sampleDecomp;
        final List<Positioning> part;

        public IdsData(String sample, String sampleDecomp, List<Positioning> part) {
            this.sample = sample;
            this.sampleDecomp = sampleDecomp;
            this.part = part;
        }

        @Override
        public String toString() {
            return part.toString();
        }
    }

    private static void add(int codepoint, String sample, String sampleDecomp, Positioning... part) {
        IDS_INFO.put(codepoint, new IdsData(sample, sampleDecomp, Arrays.asList(part)));
    }

    static final UnicodeMap<IdsData> IDS_INFO = new UnicodeMap<>();
    static {

        IDS_INFO.putAll(IDEOGRAPHIC, IdsData.IDEO);
        IDS_INFO.putAll(RADICAL, IdsData.IDEO);
        IDS_INFO.putAll(STROKES, IdsData.IDEO);
        IDS_INFO.putAll(EXT, IdsData.IDEO);
        IDS_INFO.putAll(EXT_E, IdsData.IDEO);

        //  ⿰  U+2FF0 IDEOGRAPHIC DESCRIPTION CHARACTER LEFT TO RIGHT
        add(0x2ff0, "㐖", "⿰吉乚",
                new Positioning(0,0,0.5,1), 
                new Positioning(0.5,0,1,1));
        //  ⿱  U+2FF1 IDEOGRAPHIC DESCRIPTION CHARACTER ABOVE TO BELOW
        add(0x2ff1, "㐀", "⿱卝一",
                new Positioning(0,0,1,0.5), 
                new Positioning(0,0.5,1,1));
        //  ⿲  U+2FF2 IDEOGRAPHIC DESCRIPTION CHARACTER LEFT TO MIDDLE AND RIGHT
        add(0x2ff2, "㣠", "⿲彳丨冬",
                new Positioning(0,0,0.3,1), 
                new Positioning(0.3,0,0.7,1), 
                new Positioning(0.7,0,1,1));
        //  ⿳  U+2FF3 IDEOGRAPHIC DESCRIPTION CHARACTER ABOVE TO MIDDLE AND BELOW
        add(0x2ff3, "㞿", "⿳山土乂", 
                new Positioning(0,0,1,0.3), 
                new Positioning(0,0.3,1,0.7), 
                new Positioning(0,0.7,1,1));
        //  ⿴  U+2FF4 IDEOGRAPHIC DESCRIPTION CHARACTER FULL SURROUND
        add(0x2ff4, "囝", "⿴囗子",
                new Positioning(0,0,1,1), 
                new Positioning(0.25,0.25,0.75,0.75));
        //  ⿵  U+2FF5 IDEOGRAPHIC DESCRIPTION CHARACTER SURROUND FROM ABOVE
        add(0x2ff5, "悶", "⿵門心",
                new Positioning(0,0,1,1), 
                new Positioning(0.3,0.3,0.7,1));
        //  ⿶  U+2FF6 IDEOGRAPHIC DESCRIPTION CHARACTER SURROUND FROM BELOW
        add(0x2ff6, "𠙶", "⿶凵了",
                new Positioning(0,0,1,1), 
                new Positioning(0.3,0,0.7,0.7));
        //  ⿷  U+2FF7 IDEOGRAPHIC DESCRIPTION CHARACTER SURROUND FROM LEFT
        add(0x2ff7, "𠤭", "⿷匚人",
                new Positioning(0,0,1,1), 
                new Positioning(0.3,0.3,1,0.7));
        //  ⿸  U+2FF8 IDEOGRAPHIC DESCRIPTION CHARACTER SURROUND FROM UPPER LEFT
        add(0x2ff8, "産", "⿸产生",
                new Positioning(0,0,0.9,0.9), 
                new Positioning(0.5,0.5,1,1));
        //  ⿹  U+2FF9 IDEOGRAPHIC DESCRIPTION CHARACTER SURROUND FROM UPPER RIGHT
        add(0x2ff9, "甸", "⿹勹田",
                new Positioning(0,0,1,1), 
                new Positioning(0,0.5,0.5,1));
        //  ⿺  U+2FFA IDEOGRAPHIC DESCRIPTION CHARACTER SURROUND FROM LOWER LEFT
        add(0x2ffa, "䆪", "⿺光空",
                new Positioning(0,0.2,0.8,1), 
                new Positioning(0.5,0,1,0.5));
        //  ⿻  U+2FFB IDEOGRAPHIC DESCRIPTION CHARACTER OVERLAID
        add(0x2ffb, "𠆥", "⿻人丿",
                new Positioning(0,0,0.9,0.9), 
                new Positioning(0.1,0.1,1,1));
    }

    //㿂 ⿸疒⿰⿱山王攵

    static final class CpPart {
        final int codepoint;
        final Positioning part;
        final float color;

        public CpPart(int codepoint, Positioning part, float color) {
            this.codepoint = codepoint;
            this.part = part;
            this.color = color;
        }

        public String getColor() {
            float c = color * 0x3;
            final int dr, dg, db;
            if (c <= 1) {
                dr = Math.round(c*0xFF);
                dg = Math.round((1-c)*0xFF);
                db = 0;
            } else if (c <= 2) {
                dr = 0;
                dg = Math.round((c-1)*0xFF);
                db = Math.round((2-c)*0xFF);
            } else {
                dg = 0;
                db = Math.round((c-2)*0xFF);
                dr = Math.round((3-c)*0xFF);
            }
            long v = 0x1000000L | (dr<<16) | (dg <<8) | db;
            String rgb = Long.toHexString(v);
            return "#" + rgb.substring(rgb.length()-6);
        }

        public static List<CpPart> parse(String source) {
            if (DEBUG) System.out.println(source);
            ArrayList<CpPart> result = new ArrayList<CpPart>();
            final int[] codePoints = CharSequences.codePoints(source);
            int reached;
            try {
                reached = parse(1, Positioning.BASE, 0, codePoints, result);
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new IllegalArgumentException("Too few characters");
            }
            if (reached != codePoints.length) {
                throw new IllegalArgumentException("Too many characters: over " + reached);
            }
            return result;
        }

        private static int parse(int depth, Positioning position, int pos, int[] codePoints, ArrayList<CpPart> result) {
            final int lead = codePoints[pos++];
            final IdsData ids = IDS_INFO.get(lead);
            if (ids == null || ids == IdsData.IDEO) {
                String radical = cjkRadical.get(lead);
                if (radical != null) {
                    result.add(new CpPart(lead, position, pos/(float)codePoints.length));
                    return pos;
                }
                throw new IllegalArgumentException("Didn't find IDS, Radical, or Stroke at " + (pos-1));
            }
            if (DEBUG) System.out.println(Utility.repeat("\t",depth) + UTF16.valueOf(lead) + " => " + ids.part);
            for (final Positioning subpart : ids.part) {
                final Positioning combo = position.times(subpart);
                int codePoint = codePoints[pos++];
                if (DEBUG) System.out.println(Utility.repeat("\t",depth) + UTF16.valueOf(codePoint) + " & " + combo);

                IdsData partData = IDS_INFO.get(codePoint);
                if (partData == null) {
                    if (codePoint == '{') {
                        int first = codePoints[pos++] - 0x30;
                        if (first < 0 || first > 9) {
                            throw new IllegalArgumentException("Unexpected character " + Utility.hex(codePoint) + " at " + (pos-1));
                        }
                        int second = codePoints[pos++] - 0x30;
                        if (second < 0 || second > 9) {
                            throw new IllegalArgumentException("Unexpected character " + Utility.hex(codePoint) + " at " + (pos-1));
                        }
                        int third = codePoints[pos++];
                        if (third != '}') {
                            throw new IllegalArgumentException("Unexpected character " + Utility.hex(codePoint) + " at " + (pos-1));
                        }
                        codePoint = 0xE000 + first * 10 + second;
                        partData = IDS_INFO.get(codePoint);
                    } else if (codePoint == '？') {
                        codePoint = 0xE07F;
                        partData = IDS_INFO.get(codePoint);
                    } else if (codePoint == '↔') {
                        int arg = codePoints[pos++];
                        int mirrored = "𦣞正止臣".indexOf(arg);
                        if (mirrored >= 0) {
                            partData = IDS_INFO.get(0xE040 + mirrored);
                        }
                    } else if (codePoint == '↷') {
                        int arg = codePoints[pos++];
                        int mirrored = "或止虎".indexOf(arg);
                        if (mirrored >= 0) {
                            partData = IDS_INFO.get(0xE050 + mirrored);
                        }
                    }
                }
                if (partData == null) {
                    throw new IllegalArgumentException("Unexpected character " + Utility.hex(codePoint) + " at " + (pos-1));
                } else if (partData == IdsData.IDEO) {
                    result.add(new CpPart(codePoint, combo, pos/(float)codePoints.length));
                } else {
                    pos = parse(depth + 1, combo, pos-1, codePoints, result);
                }
            }
            return pos;
        }

        @Override
        public String toString() {
            return UTF16.valueOf(codepoint) + part;
        }

        public String svgRect(String color, boolean showRect) {
            return "<g>"
                    + (showRect ?
                            "<rect x='" + (144*part.x1)
                            + "' y='" + (144*part.y1)
                            + "' width='" + (144*(part.x2-part.x1))
                            + "' height='" + (144*(part.y2-part.y1))
                            + "' style='fill:none; stroke:black;stroke-width:1'/>\n" : "")
                            + "<text x='" + (144*part.x1)/(part.x2-part.x1)
                            + "' y='" + (144*part.y2)/(part.y2-part.y1)
                            + "' fill='" + color + "'"
                            + " font-size='144'"
                            + " dominant-baseline='ideographic'"
                            + " transform='scale(" + (part.x2-part.x1) + " " + (part.y2-part.y1) + ")'" 
                            + ">" + UTF16.valueOf(codepoint) + "</text></g>\n";
        }
    }

    public static void main(String[] args) throws IOException {
        load();
        try (PrintWriter out = BagFormatter.openUTF8Writer(Settings.GEN_DIR + "ids/",
                "ids.html");
                ) {
            out.println("<!DOCTYPE html><html><head><style>\n"
                    + ".move { "
                    + "width: 144px; "
                    + "height: 144px; "
                    + "border: 1px solid black; "
                    + "position: absolute;"
                    + "font-size: 144px;"
                    + "}\n"
                    + "</style></head><body>");
            out.println("<table border='1'>");
//            show(out, "㿂".codePointAt(0), "⿸疒⿰⿱山王攵");

//            for (String cp : IDS) {
//                final IdsData idsData = IDS_INFO.get(cp);
//                show(out, idsData.sample.codePointAt(0), idsData.sampleDecomp);
//            }
            TreeSet<String> sorted = new TreeSet<>(UNIHAN);
            IDS_DATA.keySet().addAllTo(sorted);
            for (String s : sorted) {
                CharacterIds entry = IDS_DATA.get(s);
                    show(out, s.codePointAt(0), entry.idsSource, entry.parts);
            }
            out.println("</table></body></html>");
        }
    }

    static final class CharacterIds {
        final String idsSource;
        final List<CpPart> parts;
        public CharacterIds(String idsSource) {
            String x = clean(idsSource);
            this.idsSource = x; 
            this.parts = CpPart.parse(this.idsSource);
        }
        @Override
        public String toString() {
            return idsSource + "\t" + parts;
        }
    }

    static final UnicodeMap<CharacterIds> IDS_DATA = new UnicodeMap();
    static final UnicodeMap<CharacterIds> IDS2_DATA = new UnicodeMap();

    private static void load() {
        M3<String,String,String> failures = ChainedMap.of(new TreeMap(), new TreeMap(), String.class);
        UnicodeSet identicals = new UnicodeSet();
        String [] biggestCp = new String[50];
        CharacterIds [] biggest = new CharacterIds[50];
        Counter<Integer> counter = new Counter<>();

        for (String line : FileUtilities.in(Settings.OTHER_WORKSPACE_DIRECTORY + "DATA/ids/", "babelstoneIds.txt")) {
            // U+3FCD 㿍 ⿸疒解
            if (line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\\s+");
            if (parts.length != 3) {
                continue;
            }
            final String ids = clean(parts[2]);
            final String source = parts[1];
            try {
                if (ids.equals(source)) {
                    if (cjkRadical.get(ids) == null) {
                        identicals.add(source);
                    }
                    continue;
                }
                final CharacterIds chIds = new CharacterIds(ids);
                IDS_DATA.put(source, chIds);
                final int size = chIds.parts.size();
                counter.add(size, 1);
                if (biggestCp[size] == null) {
                    biggestCp[size] = source;
                    biggest[size] = chIds;
                    System.out.println(size + "\t" + source + "\t" + chIds);
                }
            } catch (Exception e) {
                failures.put(e.getMessage(), source, ids);
            }
        }
        IDS_DATA.freeze();
        System.out.println(counter);
        UnicodeSet missing = new UnicodeSet(IDEOGRAPHIC).removeAll(identicals).removeAll(IDS_DATA.keySet()).removeAll(cjkRadical.keySet());
        
        System.out.println("Identicals:\t" + identicals.size());
        System.out.println("Other missing:\t" + missing.size());

        showMissing("identicals", identicals);
        showMissing("other-missing", missing);
        
        System.out.println("Failed to parse: ");
        for (Entry<String, Map<String, String>> entry : failures) {
            for (Entry<String, String> entry2 : entry.getValue().entrySet()) {
                System.out.println(entry.getKey() + "\t" + entry2.getKey() + "\t" + entry2.getValue());
            }
        }
        for (Entry<String, String> entry : cjkRadical.entrySet()) {
            String key = entry.getKey();
            String totalStrokeCount = totalStrokes.get(key);
            double value = cleanRadical(entry.getValue());
            System.out.println(entry.getKey() + "\t" + value + "\t" + UCharacter.getName(entry.getKey(), "+") + "\t" + totalStrokeCount);
        }
        for (String s : STROKES) {
            String totalStrokeCount = totalStrokes.get(s);
            System.out.println(s + "\t" + "?" + "\t" + UCharacter.getName(s, "+") + "\t" + totalStrokeCount);
        }
    }

    private static double cleanRadical(String value) {
        if (value.endsWith("'")) {
            return Integer.parseInt(value.substring(0,value.length()-1)) + 0.1d;
        }
        return Integer.parseInt(value);
    }

    static final Splitter DOT_SPLITTER = Splitter.on('.');
    static final Splitter VBAR_SPLITTER = Splitter.on('|');
    
    private static void showMissing(String title, UnicodeSet missing) {
        Set<String> missingSorted = missing.addAllTo(new TreeSet<>(UNIHAN));
        for (String s : missingSorted) {
            final String rs = radicalStroke.get(s);
            if (rs == null) {
                System.out.println(title + "\t" + s + "\t" + "null" + "\t" + "null" + "\t" + totalStrokes.get(s));
                continue;
            }
//            if (rs.contains("|")) {
//                System.out.println(s + "\t" + rs);
//            }
            List<String> rsAlts = VBAR_SPLITTER.splitToList(rs);
            List<String> rsArray = DOT_SPLITTER.splitToList(rsAlts.get(0));
            System.out.println(title + "\t" + s + "\t" + cleanRadical(rsArray.get(0)) + "\t" + rsArray.get(1) + "\t" + totalStrokes.get(s));
        }
    }

    private static String clean(String idsSource) {
        int start = 0; 
        int end = idsSource.length();
        if (idsSource.startsWith("^")) {
            start = 1;
        }
        if (idsSource.endsWith("$")) {
            end = end-1;
        }
        String x = idsSource.substring(start,end);
        return x;
    }

    private static void show(PrintWriter out, int codepoint, String source) {
        show(out, codepoint,  source, CpPart.parse(source));
    }

    private static void show(PrintWriter out, int codepoint, String source, List<CpPart> breakdown) {
        if (DEBUG) System.out.println(UTF16.valueOf(codepoint) + "\t" + source);
        out.println("<tr>"
                //+ "<td>" + entry.getKey() + "</td>\n"
                + "<td style='font-size:144px'>" + UTF16.valueOf(codepoint) + "</td>\n"
                + "<td height='144px' width='144px' style='position:relative'>\n"
                + show(breakdown) 
                + "</td>\n"
                + "<td style='font-size:24px'>" + source + "</td>\n"
                + "<td>" + CollectionUtilities.join(breakdown, "<br>") + "</td>"
                + "</tr>"
                );
    }

    private static String show(List<CpPart> data) {
        StringBuilder b = new StringBuilder("<svg width='144' height='144'>");
        int count = 0;
        for (CpPart part : data) {
            b.append(part.svgRect(part.getColor(), false));
            //      b.append("<div class='move' "
            //          + "style='" + part.html() + "'>" 
            //          + codePoint + ":" + ++count + "</div>\n");
            ++count;
        }
        return b.append("</svg>").toString();
    }
}
