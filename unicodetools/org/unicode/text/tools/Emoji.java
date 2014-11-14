package org.unicode.text.tools;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.With;
import org.unicode.text.UCA.UCA;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.MultiComparator;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

public class Emoji {
    static final UnicodeSet GITHUB_APPLE_CHARS = new UnicodeSet(
    "[‼⁉™ℹ↔-↙↩↪⌚⌛⏩-⏬⏰⏳Ⓜ▪▫▶◀◻-◾☀☁☎☑☔☕☝☺♈-♓♠♣♥♦♨♻♿⚓⚠⚡⚪⚫⚽⚾⛄⛅⛎⛔⛪⛲⛳⛵⛺⛽✂✅✈-✌✏✒✔✖✨✳✴❄❇❌❎❓-❕❗❤➕-➗➡➰➿⤴⤵⬅-⬇⬛⬜⭐⭕〰〽㊗㊙🀄🃏🅰🅱🅾🅿🆎🆑-🆚🈁🈂🈚🈯🈲-🈺🉐🉑🌀-🌟🌰-🌵🌷-🍼🎀-🎓🎠-🏄🏆-🏊🏠-🏰🐀-🐾👀👂-📷📹-📼🔀-🔇🔉-🔽🕐-🕧🗻-🙀🙅-🙏🚀-🚊🚌-🛅{🇨🇳}{🇩🇪}{🇪🇸}{🇫🇷}{🇬🇧}{🇮🇹}{🇯🇵}{🇰🇷}{🇷🇺}{🇺🇸}]")
    .freeze();

    static public String buildFileName(String chars, String separator) {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (int cp : With.codePointArray(chars)) {
            if (first) {
                first = false;
            } else {
                result.append(separator);
            }
            result.append(Utility.hex(cp).toLowerCase(Locale.ENGLISH));
        }
        return  result.toString();
    }
    
    static Pattern DASH_OR_UNDERBAR = Pattern.compile("[-_]");
    
    static public String parseFileName(boolean hasPrefix, String chars) {
        StringBuilder result = new StringBuilder();
        int dotPos = chars.lastIndexOf('.');
        if (dotPos >= 0) {
            chars = chars.substring(0,dotPos);
        }
        String[] parts = DASH_OR_UNDERBAR.split(chars); //chars.split(separator);
        boolean first = true;
        for (String part : parts) {
            if (hasPrefix && first) {
                first = false;
                continue;
            }
            result.appendCodePoint(Integer.parseInt(part,16));
        }
        return  result.toString();
    }

    public static String getHexFromFlagCode(String isoCountries) {
        String cc = new StringBuilder()
        .appendCodePoint(isoCountries.charAt(0) + Emoji.FIRST_REGIONAL - 'A') 
        .appendCodePoint(isoCountries.charAt(1) + Emoji.FIRST_REGIONAL - 'A')
        .toString();
        return cc;
    }
    
    static String getEmojiFromRegionCode(String chars) {
        return new StringBuilder()
        .appendCodePoint(chars.codePointAt(0) + FIRST_REGIONAL - 'A')
        .appendCodePoint(chars.codePointAt(1) + FIRST_REGIONAL - 'A')
        .toString();
    }

    static String getRegionCodeFromEmoji(String chars) {
        int first = chars.codePointAt(0);
        return new StringBuilder()
        .appendCodePoint(first - FIRST_REGIONAL + 'A')
        .appendCodePoint(chars.codePointAt(Character.charCount(first)) - FIRST_REGIONAL + 'A')
        .toString();
    }

    static final int FIRST_REGIONAL = 0x1F1E6;
    static final int LAST_REGIONAL = 0x1F1FF;

    // 0-9
    public static final UnicodeSet EMOJI_CHARS = new UnicodeSet(
    "[🕉 ✡ ☸ ☯ ✝ ☦ ⛩ ☪ ⚛ 0-9©®‼⁉℗™ℹ↔-↙↩↪⌚⌛⌨⎈⏏⏩-⏺Ⓜ▪▫▶◀●◪◻-◾☀-☄☎-☒☔☕☘-☠☢-☤☦🕉☦ ☪ ☬ ☸ ✝ 🕉☪-☬☮☯☹-☾♈-♓♠-♯♲"
    + "♻♾♿⚐-⚜⚠⚡⚪⚫⚰⚱⚽-⚿⛄-⛈⛍-⛙⛛-⛡⛨-⛪⛰-⛵⛷-⛺⛼-✒✔-✘✝✨✳✴❄❇❌❎❓-❕❗❢-❧➕-➗"
    + "➡➰➿⤴⤵⬅-⬇⬛⬜⭐⭕⸙〰〽㊗㊙🀄🃏🅰🅱🅾🅿🆎🆏🆑-🆚🈁🈂🈚🈯🈲-🈺🉐🉑🌀-🌬🌰-🍽🎀-🏎"
    + "🏔-🏷🐀-📾🔀-🔿🕊🕐-🕱🕳-🕹🖁-🖣🖥-🖩🖮-🗳🗺-🙂🙅-🙏🚀-🛏🛠-🛬🛰-🛳"
    + "{#⃣}{*⃣}{0⃣}{1⃣}{2⃣}{3⃣}{4⃣}{5⃣}{6⃣}{7⃣}{8⃣}{9⃣}]")
    .removeAll(new UnicodeSet("[☫☬]"));
//    static final UnicodeSet EXCLUDE = new UnicodeSet(
//    "[🂠-🂮 🂱-🂿 🃁-🃎 🃑-🃵 🀀-🀃 🀅-🀫 〠🕲⍾☸🀰-🂓 🙬 🙭 🙮 🙯🗴🗵🗶🗷🗸🗹★☆⛫\uFFFC⛤-⛧ ⌤⌥⌦⌧⌫⌬⎆⎇⎋⎗⎘⎙⎚⏣⚝⛌⛚⛬⛭⛮⛯⛶⛻✓🆊\\U0001F544-\\U0001F549" +
//    "☖  ☗  ⛉  ⛊  ⚀  ⚁  ⚂  ⚃  ⚄  ⚅ ♔  ♕  ♖  ♗  ♘  ♙  ♚  ♛  ♜  ♝  ♞  ♟  ⛀  ⛁  ⛂ ⛃" +
//    "]").freeze();
//    // 🖫🕾🕿🕻🕼🕽🕾🕿🖀🖪🖬🖭

    static {
        System.out.println(EMOJI_CHARS.toPattern(false));
    }
    
    static {
        CLDRConfig config = CLDRConfig.getInstance();
        StandardCodes sc = config.getStandardCodes();
        SupplementalDataInfo sdi = config.getSupplementalDataInfo();
        Set<String> container = new TreeSet();
        Set<String> contained = new TreeSet();
        for (Entry<String, String> territoryToContained : sdi.getTerritoryToContained().entrySet()) {
            container.add(territoryToContained.getKey());
            contained.add(territoryToContained.getValue());
        }
        contained.removeAll(container);
        contained.add("EU"); // special case
        Map<String, R2<List<String>, String>> aliasInfo = sdi.getLocaleAliasInfo().get("territory");
        contained.removeAll(aliasInfo.keySet());
        for (String s: contained) {
            //System.out.println(s + "\t" + config.getEnglish().getName("territory", s));
            EMOJI_CHARS.add(getHexFromFlagCode(s));
        }
        EMOJI_CHARS.freeze();
    }
    
    static final Transform<String,String> APPLE_URL = new Transform<String,String>() {
        public String transform(String s) {
            StringBuilder result = 
                    new StringBuilder(
                            Emoji.APPLE_LOCAL.containsAll(s) ? "images/apple-extras/apple-" 
                                    : "http://emojistatic.github.io/images/64/");
            boolean first = true;
            for (int cp : With.codePointArray(s)) {
                if (first) {
                    first = false;
                } else {
                    result.append("-");
                }
                result.append(com.ibm.icu.impl.Utility.hex(cp).toLowerCase(Locale.ENGLISH));
            }
            return  result.append(".png").toString();
        }
    };
    
    static final Transform<String,String> TWITTER_URL = new Transform<String,String>() {
        public String transform(String s) {
            StringBuilder result = new StringBuilder("https://abs.twimg.com/emoji/v1/72x72/");
            boolean first = true;
            for (int cp : With.codePointArray(s)) {
                if (first) {
                    first = false;
                } else {
                    result.append("-");
                }
                result.append(Integer.toHexString(cp));
            }
            return  result.append(".png").toString();
        }
    };
    static final UnicodeSet APPLE_LOCAL = new UnicodeSet("[🌠 🔈 🚋{#⃣}{0⃣}{1⃣}{2⃣}{3⃣}{4⃣}{5⃣}{6⃣}{7⃣}{8⃣}{9⃣}]").freeze();

    public static boolean isRegionalIndicator(int firstCodepoint) {
        return FIRST_REGIONAL <= firstCodepoint && firstCodepoint <= Emoji.LAST_REGIONAL;
    }

    static final char ENCLOSING_KEYCAP = '\u20E3';
    static final Comparator<String> CODEPOINT_LENGTH = new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            return o1.codePointCount(0, o1.length()) - o2.codePointCount(0, o2.length());
        }
    };

    public static final String OUTPUT_DIR = "/Users/markdavis/workspace/unicode-draft/Public/emoji/1.0/";

    static final String IMAGES_OUTPUT_DIR = "/Users/markdavis/workspace/unicode-draft/reports/tr51/images";
    
    public static void main(String[] args) {
        
    }
}
