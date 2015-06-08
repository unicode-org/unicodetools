package org.unicode.tools;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.text.utility.Settings;

import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeMap.EntryRange;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;


public class Ids {

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

  static final UnicodeSet IDS = new UnicodeSet("[[:IDS_Binary_Operator:][:IDS_Trinary_Operator:]]").freeze();

  static final UnicodeMap<IdsData> IDS_INFO = new UnicodeMap<>();
  static {

    IDS_INFO.putAll(new UnicodeSet("[[:Ideographic:][:Radical:]]"), IdsData.IDEO);

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
    private static final boolean DEBUG = false;
    final int codepoint;
    final Positioning part;
    final float color;

    public CpPart(int codepoint, Positioning part, float color) {
      this.codepoint = codepoint;
      this.part = part;
      this.color = color;
    }

    public String getColor() {
      long v = 0x10000FFL << (int)(16 * color);
      String rgb = Long.toHexString(v);
      return "#" + rgb.substring(rgb.length()-6);
    }

    public static List<CpPart> parse(String source) {
      if (DEBUG) System.out.println(source);
      ArrayList<CpPart> result = new ArrayList<CpPart>();
      final int[] codePoints = CharSequences.codePoints(source);
      final int reached = parse(1, Positioning.BASE, 0, codePoints, result);
      if (reached != codePoints.length) {
        throw new IllegalArgumentException("Only reached up through " + reached + " in " + codePoints);
      }
      return result;
    }

    private static int parse(int depth, Positioning position, int pos, int[] codePoints, ArrayList<CpPart> result) {
      final int lead = codePoints[pos++];
      final IdsData ids = IDS_INFO.get(lead);
      if (ids == null || ids == IdsData.IDEO) {
        throw new IllegalArgumentException("Didn't find IDS at " + (pos-1) + " in " + codePoints);
      }
      if (DEBUG) System.out.println(Utility.repeat("\t",depth) + UTF16.valueOf(lead) + " => " + ids.part);
      for (final Positioning subpart : ids.part) {
        final Positioning combo = position.times(subpart);
        final int codePoint = codePoints[pos++];
        if (DEBUG) System.out.println(Utility.repeat("\t",depth) + UTF16.valueOf(codePoint) + " & " + combo);

        final IdsData partData = IDS_INFO.get(codePoint);
        if (partData == null) {
          throw new IllegalArgumentException("Didn't find IDS at " + (pos-1) + " in " + codePoints);
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
      show(out, "㿂".codePointAt(0), "⿸疒⿰⿱山王攵");

      for (String cp : IDS) {
        final IdsData idsData = IDS_INFO.get(cp);
        show(out, idsData.sample.codePointAt(0), idsData.sampleDecomp);
      }
      for (EntryRange<CharacterIds> entry : ISD_DATA.entryRanges()) {
        for (int cp = entry.codepoint; cp <= entry.codepointEnd; ++cp) {
          show(out, cp, entry.value.idsSource, entry.value.parts);
        }
      }
      out.println("</table></body></html>");
    }
  }

  static final class CharacterIds {
    final String idsSource;
    final List<CpPart> parts;
    public CharacterIds(String idsSource) {
      this.idsSource = idsSource;
      this.parts = CpPart.parse(idsSource);
    }
    @Override
    public String toString() {
      return idsSource + "\t" + parts;
    }
  }

  static final UnicodeMap<CharacterIds> ISD_DATA = new UnicodeMap();

  private static void load() {
    UnicodeMap<String> failures = new UnicodeMap<String>();

    String [] biggestCp = new String[50];
    CharacterIds [] biggest = new CharacterIds[50];

    for (String line : FileUtilities.in(Settings.OTHER_WORKSPACE_DIRECTORY + "DATA/ids/", "IDS-PresumablyCorrect.txt")) {
      // U+3FCD 㿍 ⿸疒解
      String[] parts = line.split("\\s+");
      try {
        final CharacterIds chIds = new CharacterIds(parts[2]);
        ISD_DATA.put(parts[1], chIds);
        final int size = chIds.parts.size();
        if (biggestCp[size] == null) {
          biggestCp[size] = parts[1];
          biggest[size] = chIds;
          System.out.println(size + "\t" + parts[1] + "\t" + chIds);
        }
      } catch (Exception e) {
        failures.put(parts[1], parts[2]);
      }
    }
    ISD_DATA.freeze();
    System.out.println("Failed to load: " + failures.size() + "\n" + failures);
  }

  private static void show(PrintWriter out, int codepoint, String source) {
    show(out, codepoint,  source, CpPart.parse(source));
  }

  private static void show(PrintWriter out, int codepoint, String source, List<CpPart> breakdown) {
    System.out.println(UTF16.valueOf(codepoint) + "\t" + source);
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
