package org.unicode.text.tools;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.Locale;

import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.text.UCD.Default;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.UnicodeSet;

public class CarrierGlyphs {
    static final IndexUnicodeProperties LATEST = IndexUnicodeProperties
            .make(Default.ucdVersion());
    static final UnicodeMap<String> Emoji_SB = LATEST
            .load(UcdProperty.Emoji_SB);
    static final UnicodeMap<String> Emoji_DCM = LATEST
            .load(UcdProperty.Emoji_DCM);
    static final UnicodeMap<String> Emoji_KDDI = LATEST
            .load(UcdProperty.Emoji_KDDI);
    private static final Comparator SB_FIRST = new Comparator<String>() {
        public int compare(String o1, String o2) {
            if (o1.equals(o2)) {
                return 0;
            }
            String c1 = Emoji_SB.get(o1);
            String c2 = Emoji_SB.get(o2);
            if (c1 == null) {
                return c2 == null ? o1.compareTo(o2) : 1;
            } else if (c2 == null) {
                return -1;
            }
            return c1.compareTo(c2);
        }
    };

    public static void main(String[] args) throws IOException {
        UnicodeSet carrier = new UnicodeSet(); // new TreeSet(SB_FIRST);
        carrier.addAll(Emoji_KDDI.keySet());
        carrier.addAll(Emoji_DCM.keySet());
        carrier.addAll(Emoji_SB.keySet());
        PrintWriter out = BagFormatter.openUTF8Writer(Settings.SVN_WORKSPACE_DIRECTORY
                + "/reports/tr51/", "carrier-emoji.html");
        out.println("<html>\n" +
                "<head>\n" +
                "<meta http-equiv='Content-Type' content='text/html; charset=utf-8'>\n" +
                "<link rel='stylesheet' type='text/css' href='emoji-list.css'>\n" +
                "<title>Draft Carrier Data (Full)</title>\n" +
                "</head><body>\n" +
                "<table>\n" +
                "<tr><th>Hex</th><th>Char</th>" +
                "<th>gmail image</th>" +
                "<th>hex kddi</th><th>kddi image</th>" +
                "<th>hex dcm</th><th>dcm image</th>" +
                "<th>hex sb</th><th>sb image</th>" +
                "<th>Name</th>" +
                "</tr>\n"
                );
        for (String s : carrier) {
            final String au = AU_URL.transform(s);
            final String dcm = DCM_URL.transform(s);
            final String sb = SB_URL.transform(s);
            final String kddiCode = Emoji_KDDI.get(s);
            final String dcmCode = Emoji_DCM.get(s);
            final String sbCode = Emoji_SB.get(s);
            out.println(
                    "<tr><td>" + Utility.hex(s) + "</td>" +
                            "<td>" + s + "</td>" +
                            "\n\t<td>" + img("gmail", s, "x") + "</td>" +
                            "\n\t<td>" + replaceNull(kddiCode, "n/a") + "</td>" +
                            "\n\t<td>" + img("kddi", s, kddiCode) + "</td>" +
                            "\n\t<td>" + replaceNull(dcmCode, "n/a") + "</td>" +
                            "\n\t<td>" + img("dcm", s, dcmCode) + "</td>" +
                            "\n\t<td>" + replaceNull(sbCode, "n/a") + "</td>" +
                            "\n\t<td>" + img("sb", s, sbCode) + "</td>" +
                            "\n\t<td>" + UCharacter.getName(s, "+") + "</td>" +
                            "</tr>"
                    );
        }
        out.println("</table></body></html>");
        out.close();
    }

    private static String replaceNull(String string, String string2) {
        return string == null ? string2 : string;
    }

    public static String img(String type, final String unicode, String code) {
        if (code == null) {
            return "";
        }
        final String dir = Settings.SVN_WORKSPACE_DIRECTORY + "/reports/tr51/";
        final String filename = "images/"
                + type
                + "/" 
                + type +
                "_" + Utility.hex(unicode, "_").toLowerCase(Locale.ROOT)
                + ".gif";
        return // !new File(dir, filename).exists() ? "missing" : 
                    "<img class='imgb' src='"  + filename + "'>";
    }

    static final Transform<String, String> AU_URL = new Transform<String, String>() {
        public String transform(String s) {
            String transformed = Emoji_KDDI.get(s);
            if (transformed == null) {
                return null;
                // transformed = "fffd";
            } else {
                transformed = transformed.toLowerCase(Locale.ROOT);
            }
            return "http://trialgoods.com/images/200807au/" + transformed
                    + ".gif";
        }
    };
    static final Transform<String, String> DCM_URL = new Transform<String, String>() {
        public String transform(String s) {
            String transformed = Emoji_DCM.get(s);
            if (transformed == null) {
                return null;
                // transformed = "fffd";
            } else {
                transformed = transformed.toLowerCase(Locale.ROOT);
            }
            return "http://trialgoods.com/images/200807i/" + transformed
                    + ".gif";
        }
    };
    // http://trialgoods.com/images/200807sb/F72.gif
    static final Transform<String, String> SB_URL = new Transform<String, String>() {
        public String transform(String s) {
            //if (true) return null;

            String transformed = Emoji_SB.get(s);
            if (transformed == null) {
                return null;
                // transformed = "fffd";
            } else {
                int sjis = Integer.parseInt(transformed, 16);
                int fixed = sbFromShiftJis(sjis);
                String trail = Utility.hex((fixed & 0xFF),2).toLowerCase(Locale.ROOT);
                transformed = ((char)(fixed >> 8))+ trail;
            }
            return "http://trialgoods.com/images/200807sb/" + transformed
                    + ".gif";
        }
    };

    static int sbFromShiftJis(int b) {
        int b1 = b >> 8;
        int b2 = b & 0xFF;
        //Create a RowCell instance from a Shift-JIS byte pair.

        //    Returns:
        //      A RowCell instance with the row-cell value pair.
        //
        //    Raises:
        //      ValueError: The lead byte is not 0x81..0x9f or 0xe0..0xef, and/or
        //        the trail byte is not 0x40..0x7e or 0x80..0xfc.
        //    """

        // Can't use the following, since it blows out on F7..

        //    if (!((0x81 <= b1 && b1 <= 0x9f || 0xe0 <= b1 && b1 <= 0xef) 
        //        && 0x40 <= b2 && b2 <= 0xfc && b2 != 0x7f)) {
        //      throw new IllegalArgumentException("value out of range " + Utility.hex(b));
        //    }
        if (b1 <= 0x9f) {
          b1 = (b1 - 0x80) << 1;
        } else {
          b1 = (b1 - 0xc0) << 1;
        }
        if (b2 <= 0x9e) {
          b1 -= 1;
          if (b2 <= 0x7e) {
            b2 -= 0x3f;
          } else {
            b2 -= 0x40;
          }
        } else {
          b2 -= 0x9e;
        }
        // According to http://trialgoods.com/emoji/?career=sb&page=all,
        // F741 => $E! = 4521
        // but this returns 6D02
        // so trying (???)
        b1 = b1 + 0x45 - 0x6D;
        b2 = b2 + 0x21 - 0x02;
        // next step
        // appears to work for the E's
        // but then fails for the F's
        // 🚶 F7A1  =>  F"  4622, should be 4621
        if (b1 >= 0x46) {
          --b2;
          // 👦 F941  =>  G!  4721
          if (b1 >= 0x49) {
            b1 -= 2;
            b2 += 1;
            // 📝 F9A1  =>  H"  4822 should be O! 4F21
            if (b1 >= 'H') {
              b1 += 'O' - 'H';
              b2 -= 1;
              // 😥 FB41  =>  R   5220 should be $P! 5021
              if (b1 >= 'R') {
                b1 -= 2;
                b2 += 1;
                // 🏩 FBA1  =>  Q"  5122 should be $Q! 5121
                if (b1 >= 'Q') {
                  b2 -= 1;
                }
                // final is ™ FBD7  =>  QW  5157, so checks out.
              }
            }
          }
        }
        return (b1 << 8) & 0xFF00 | (b2 & 0xFF);
      }
}
