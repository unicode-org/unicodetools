package org.unicode.tools.emoji;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.unicode.cldr.draft.FileUtilities;

class UnicodeFontData {
    final String fontName;
    final int fontSize;

    public UnicodeFontData(String fontName, int fontSize) {
        this.fontName = fontName;
        this.fontSize = fontSize;
    }

    @Override
    public String toString() {
        return fontName + "," + fontSize;
    }

    @Override
    public int hashCode() {
        return fontName.hashCode() ^ fontSize;
    }

    @Override
    public boolean equals(Object obj) {
        UnicodeFontData other = (UnicodeFontData) obj;
        return fontName.equals(other.fontName) && fontSize == other.fontSize;
    }

    static Map<UnicodeFontData, UnicodeFontData> KEY_TO_FONT = new HashMap<>();

    private static UnicodeFontData getFont(UnicodeFontData key) {
        UnicodeFontData result = KEY_TO_FONT.get(key);
        if (result == null) {
            KEY_TO_FONT.put(key, key);
            result = key;
        }
        return result;
    }

    static UnicodeMap<UnicodeFontData> FONT_DATA = new UnicodeMap<>();

    public static UnicodeFontData getFont(int codepoint) {
        return FONT_DATA.get(codepoint);
    }

    public static UnicodeFontData getFont(String codepoint) {
        return FONT_DATA.get(codepoint);
    }

    enum DataType {
        Q,
        X,
        R,
        I
    }

    static {
        Map<UnicodeFontData.DataType, UnicodeSet> tempData = new LinkedHashMap<>();
        Matcher lineMatch = Pattern.compile("([^,]*),\\s*(\\d+)(?:,\\s*(\\d+))?(.*)").matcher("");
        Matcher m = Pattern.compile("/([QXRI])=(.*)").matcher("");
        for (String line : FileUtilities.in(UnicodeFontData.class, "CONFIG-FILE-SECTIONS.txt")) {
            if (line.isEmpty() || line.startsWith(";")) {
                continue;
            }
            // TmsMathPak7bttPF,22 /Q=2047 /R=2047-2047
            if (line.contains("2300")) {
                int debug = 0;
            }
            if (!lineMatch.reset(line).matches()) {
                throw new IllegalArgumentException("Couldn't match font line: " + line);
            }
            UnicodeFontData key =
                    getFont(
                            new UnicodeFontData(
                                    lineMatch.group(1).trim(),
                                    Integer.parseInt(lineMatch.group(2))));
            tempData.clear();
            String[] parts = lineMatch.group(4).trim().split("\\s*[ ,]\\s*");
            for (String part : parts) {
                if (part.startsWith(";")) {
                    break;
                } else if (part.isEmpty()) {
                    continue;
                }
                if (!m.reset(part).matches()) {
                    throw new IllegalArgumentException("Couldn't match font line: " + line);
                }
                // /X=0000-10FFFF /I=2000-206F
                String[] ranges = m.group(2).split("-");
                int r1 = Integer.parseInt(ranges[0], 16);
                int r2 = ranges.length == 1 ? r1 : Integer.parseInt(ranges[1], 16);
                tempData.put(DataType.valueOf(m.group(1)), new UnicodeSet(r1, r2));
            }
            /// I=2070-209F code points
            UnicodeSet i = tempData.get(DataType.I);
            if (i == null) {
                UnicodeSet q = tempData.get(DataType.Q);
                UnicodeSet r = tempData.get(DataType.R);
                if (q.getRangeStart(0) != r.getRangeStart(0)) {
                    UnicodeSet missing =
                            new UnicodeSet(
                                    q.getRangeStart(0),
                                    q.getRangeStart(0) + (r.getRangeEnd(0) - r.getRangeStart(0)));
                    if (EmojiData.EMOJI_DATA.getChars().containsSome(missing)
                            || Emoji.U80.containsSome(missing)) {
                        System.err.println("Couldn't get code points from: " + line);
                    }
                    continue;
                    // throw new IllegalArgumentException("Couldn't get code points from: " + line);
                }
                i = r;
            }
            for (String s : i) {
                if (!EmojiData.EMOJI_DATA.getChars().contains(s) && !Emoji.U80.contains(s)) {
                    continue;
                }
                if (s.equals("\u231A")) {
                    int debug = 0;
                }
                UnicodeFontData old = FONT_DATA.get(s);
                if (old == null) {
                    FONT_DATA.put(s, key); // only add new values
                }
            }
        }
        UnicodeFontData x = FONT_DATA.get("ߌ­");
        for (UnicodeFontData value : FONT_DATA.values()) {
            UnicodeSet keys = FONT_DATA.getSet(value);
            System.out.println(keys.size() + "\t" + value + "\t" + keys);
        }
        UnicodeSet keys =
                new UnicodeSet(EmojiData.EMOJI_DATA.getChars()).removeAll(FONT_DATA.keySet());
        System.out.println(keys.size() + "\t" + "Missing" + "\t" + keys);
        keys = new UnicodeSet(Emoji.U80).removeAll(FONT_DATA.keySet());
        System.out.println(keys.size() + "\t" + "Missing U80" + "\t" + keys);
    }

    public static void main(String[] args) {}
}
