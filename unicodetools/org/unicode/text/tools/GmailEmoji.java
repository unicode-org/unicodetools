package org.unicode.text.tools;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.omg.CORBA.UNKNOWN;
import org.unicode.cldr.draft.FileUtilities;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.text.UCD.Default;
import org.unicode.text.tools.EmojiData.DefaultPresentation;
import org.unicode.text.tools.GenerateEmoji.CharSource;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

import com.google.common.base.Splitter;
import com.google.common.collect.ComparisonChain;
import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

public class GmailEmoji {
    static final IndexUnicodeProperties LATEST = IndexUnicodeProperties.make(Default.ucdVersion());
    static final UnicodeMap<String> Emoji_SB = LATEST.load(UcdProperty.Emoji_SB);
    static final UnicodeMap<String> Emoji_DCM = LATEST.load(UcdProperty.Emoji_DCM);
    static final UnicodeMap<String> Emoji_KDDI = LATEST.load(UcdProperty.Emoji_KDDI);

    static class Data implements Comparable<Data> {
        private static final String URL_PREFIX = "https://mail.google.com/mail/u/0/e/";
        final int pua;
        final String unicode;
        final String name;
        final String url;
        final String url2;
        final boolean hasOverride;

        static final String UNKNOWN = "\u0000";
        static final String BOGUS = "\uFFFD";

        static final UnicodeMap<String> OVERRIDES = new UnicodeMap<String>()
                .put(0xFE1E3, UNKNOWN)
                .put(0xFEE1C, UNKNOWN)
                .put(0xFEE31, "●")
                .put(0xFEE26, "◪")
                .put(0xFEE27, "⯀")
                .put(0xFEE29, "††††")
                .put(0xFEE2A, "†††")
                .put(0xFEE2B, "††")
                .put(0xFEE2C, "†")
                
                .put(0xFE362, "😕")
                .put(0xFE366, "😟")
                .put(0xFE360, "😀")
                .put(0xFE35C, "😎")
                .put(0xFE336, "🙂")
                
                .put(0xFEE28, "⛛")
                .put(0xFEB46, "☒")
                .put(0xFEE33, "❎")
                .put(0xFEE32, "💳\u20E0")
                
                // corporate stuff, no mapping
                .putAll(new UnicodeSet("[\\U000FE4C5\\U000FE82D\\U000FE83C\\U000FEB89\\U000FEE10-\\U000FEE1B" +
                        "\\U000FEE1D-\\U000FEE25\\U000FEE2D-\\U000FEE30\\U000FEE40-\\U000FEE4A" +
                        "\\U000FEE70-\\U000FEE7D\\U000FEEA0]"), BOGUS)
                        ;
        static final Matcher URL_MATCH = Pattern.compile(".*/(.*)\\.[a-z]{3}").matcher("");
        
        public Data(List<String> list) {
            pua = Integer.parseInt(list.get(0), 16);
            name = list.get(3);
            String temp = list.get(2);
            url = temp.endsWith(".png") || temp.endsWith(".gif") ? temp.substring(0,temp.length()-4)
                    : temp.contains(".") ? temp
                            : "softbank_ne_jp."+ list.get(0).substring(2);
            
            // https://mail.google.com/mail/u/0/e/_default/360
            if (!URL_MATCH.reset(temp).matches()) {
                System.out.println("Match fails:\t" + temp);
                url2 = url;
            } else {
                url2 = "_default/" + URL_MATCH.group(1);
            }
            

            String unicodeTemp = UNKNOWN;
            String override = OVERRIDES.get(pua);
            boolean overrideTemp = false;
            if (override != null) {
                unicodeTemp = override;
                overrideTemp = true;
            } else if (!list.get(22).isEmpty()) {
                unicodeTemp = new StringBuilder().appendCodePoint(Integer.parseInt(list.get(22), 16)).toString();
            } else {
                //# (18) SoftBank Shift_JIS emoji point(s)
                unicodeTemp = getCarrier(list, 18, unicodeTemp, Emoji_SB);
                //# (13) DoCoMo Shift_JIS emoji point(s)
                unicodeTemp = getCarrier(list, 13, unicodeTemp, Emoji_DCM);
                //#  (7) KDDI Shift_JIS emoji point(s) corresponding to (6)
                unicodeTemp = getCarrier(list, 7, unicodeTemp, Emoji_KDDI);
            }
            unicode = unicodeTemp;
            hasOverride = overrideTemp;
        }

        public String getCarrier(List<String> list, int item, String unicodeTemp, UnicodeMap<String> unicodeMap) {
            if (unicodeTemp.equals(UNKNOWN) && !list.get(item).isEmpty()) {
                UnicodeSet set = unicodeMap.getSet(list.get(item));
                if (set != null && !set.isEmpty()) {
                    unicodeTemp = set.iterator().next();
                }
            }
            return unicodeTemp;
        }

        @Override
        public int compareTo(Data o) {
            return ComparisonChain.start()
                    .compare(unicode, o.unicode, EmojiData.EMOJI_COMPARATOR)
                    .compare(pua, o.pua)
                    .result();
        }
        @Override
        public String toString() {
            return Utility.hex(unicode) + "; " + Utility.hex(pua) + "; " + name;
        }
        
        public String toHtml() {
            String uName = UCharacter.getName(unicode, " + ");
            return new StringBuilder("<tr><td>")
            .append("<img src='" +
            		URL_PREFIX + url2 + "'>")
            .append("</td><td>")
            //.append(data.unicode)
            .append("&#x" + Utility.hex(unicode, ";&#x") + ";")
            .append("</td><td>")
            .append("U+" + Utility.hex(unicode, " U+"))
            .append("</td><td>")
            .append("U+" + Integer.toHexString(pua).toUpperCase(Locale.ENGLISH))
            .append("</td><td>")
            .append(name)
            .append("</td><td>")
            .append(hasOverride ? "CHANGED" : "")
            .append("</td><td>")
            .append(uName == null || unicode.equals(BOGUS) ? "n/a" : name.equals(uName) ? "" : uName)
            .append("</td></tr>")
            .toString();
        }
    }

    static final Set<Data> pua2data;
    static final UnicodeMap<Data> unicode2data = new UnicodeMap<Data>();

    static {
        Set<Data> _pua2data = new TreeSet<Data>();
        final Splitter semi = Splitter.on("\t").trimResults();
        Data google = null;
        Data crab = null;
        //Users/markdavis/Google Drive/Backup-2012-10-09/Documents/indigo/DATA/emoji_images/gmail_emoji.txt
        for (String line : FileUtilities.in(Settings.WORKSPACE_DIRECTORY + "/data/emoji_images", "gmail_emoji.txt")) {
            if (line.startsWith("#")) continue;
            List<String> list = semi.splitToList(line);
            Data data = new Data(list);
            Data oldData = unicode2data.get(data.unicode);
            if (oldData != null && !data.unicode.equals(Data.UNKNOWN) && !data.unicode.equals(Data.BOGUS)) {
                System.out.println("Collision!\t" + oldData + " with " + data);
            } else {
                unicode2data.put(data.unicode, data);
            }
            _pua2data.add(data);
            if (data.pua == 0xFEEA0) {
                google = data;
            } else if (data.pua == 0xFE1E3) {
                crab = data;
            }
        }
        pua2data = Collections.unmodifiableSet(_pua2data);
        unicode2data.freeze();
        //google.compareTo(crab);
    }

    public static void main(String[] args) throws IOException {
        PrintWriter out = BagFormatter.openUTF8Writer(GenerateEmoji.OUTPUT_DIR, "gmail-emoji.html");
        out.println("<html><head>\n" +
        		"<style>\n" +
        		"table, th, td {border: 1px solid silver;}</style>\n"
        		+"</head><body>\n" +
        		"<h1>Unmapped</h1>\n" +
        		"<table style='border-collapse: collapse; border: 1px solid silver;'>");
        for (Data data : pua2data) {
            if (!data.unicode.equals(Data.UNKNOWN)) {
                continue;
            }
            out.println(data.toHtml());
        }
        for (Data data : pua2data) {
            if (!data.unicode.equals(Data.BOGUS)) {
                continue;
            }
            out.println(data.toHtml());
        }
        for (Data data : pua2data) {
            if (data.unicode.equals(Data.UNKNOWN) || data.unicode.equals(Data.BOGUS)) {
                continue;
            }
            out.println(data.toHtml());
        }
        out.println("</table></body></html>");
        out.close();
    }

    public static String getURL(String string) {
        Data data = unicode2data.get(string);
        return data == null || data.unicode.equals(Data.BOGUS) || data.unicode.equals(Data.UNKNOWN) ? null 
                : Data.URL_PREFIX + data.url2;
    }
}
