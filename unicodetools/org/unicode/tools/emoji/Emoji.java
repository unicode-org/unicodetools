package org.unicode.tools.emoji;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.With;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.EmojiData.EmojiDatum;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.LocaleDisplayNames;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.VersionInfo;

public class Emoji {

    /**
     * Change the following for a new version.
     */
    public static final boolean IS_BETA = CldrUtility.getProperty("emoji-beta", false);

    /**
     * Change the following once we release
     */
    public static final VersionInfo VERSION_LAST_RELEASED = VersionInfo.getInstance(2);
    public static final VersionInfo VERSION_LAST_RELEASED_UNICODE = VersionInfo.getInstance(8);
    
    public static final VersionInfo VERSION_BETA = VersionInfo.getInstance(3);
    public static final VersionInfo VERSION_BETA_UNICODE = VersionInfo.getInstance(9);
    
    public static final VersionInfo VERSION_FORMAT1 = VersionInfo.getInstance(1);

    /**
     * Computed
     */

    public static final VersionInfo VERSION_TO_GENERATE = IS_BETA ? VERSION_BETA : VERSION_LAST_RELEASED;
    public static final String VERSION_STRING = VERSION_TO_GENERATE.getVersionString(2, 4);

    public static final VersionInfo VERSION_TO_GENERATE_UNICODE = IS_BETA ? VERSION_BETA_UNICODE : VERSION_LAST_RELEASED_UNICODE;

    public static final String TR51_SVN_DIR = Settings.UNICODE_DRAFT_DIRECTORY + "reports/tr51/";
    //public static final String TR51_PREFIX = IS_BETA ? "internal-beta/" : "internal/";

    public static final String TR51_INTERNAL_DIR = Settings.UNICODE_DRAFT_DIRECTORY + "reports/tr51/"
            + (IS_BETA ? "internal-beta/" : "internal/");
    public static final String CHARTS_DIR = Settings.UNICODE_DRAFT_DIRECTORY + "emoji/" 
            + (IS_BETA ? "beta/" : "charts/");
    public static final String DATA_DIR = Settings.UNICODE_DRAFT_PUBLIC + "emoji/" + VERSION_STRING + "/";

    static final String IMAGES_OUTPUT_DIR = TR51_SVN_DIR + "images/";

    public enum ModifierStatus {
        none, modifier, modifier_base;
        static ModifierStatus fromString(String s) {
            if (s.equals("primary") || s.equals("secondary")) return modifier_base;
            return valueOf(s);
        }
    }

    public enum Source {
        // pngs
        color, apple, twitter("Twtr."), emojione("One"), google("Goog."), samsung("Sams."), windows("Wind."), ref, proposed, emojipedia, emojixpress, sample,
        // gifs; don't change order!
        gmail("GMail"), sb, dcm, kddi;

        private final String shortName;
        private Source(String shortName) {
            this.shortName = shortName;
        }
        private Source() {
            this.shortName = UCharacter.toTitleCase(name(), null);
        }
        
        boolean isGif() {
            return compareTo(Source.gmail) >= 0;
        }

        String getClassAttribute(String chars) {
            if (isGif()) {
                return "imgs";
            }
            String className = "imga";
            if (this == Source.ref && Emoji.getFlagCode(chars) != null) {
                className = "imgf";
            }
            return className;
        }

        public String getPrefix() {
            return this == google ? "android" : name();
        }

        public String shortName() {
            return shortName;
        }
    }


    enum CharSource {
        ZDings("ᶻ", "z"),
        ARIB("ª", "a"),
        JCarrier("ʲ", "j"),
        WDings("ʷ", "w"),
        Other("ˣ", "x");
        final String superscript;
        final String letter;
    
        private CharSource(String shortString, String letter) {
            this.superscript = shortString;
            this.letter = letter;
        }
    }


    public static final char KEYCAP_MARK = '\u20E3';
    private static final UnicodeSet Unicode8Emoji = new UnicodeSet("[\\x{1F3FB}\\x{1F3FC}\\x{1F3FD}\\x{1F3FE}\\x{1F3FF}\\x{1F4FF}\\x{1F54B}\\x{1F54C}\\x{1F54D}"
            +"\\x{1F54E}\\x{1F6D0}\\x{1F32D}\\x{1F32E}\\x{1F32F}\\x{1F37E}\\x{1F37F}\\x{1F983}\\x{1F984}\\x{1F9C0}"
            +"\\x{1F3CF}\\x{1F3D0}\\x{1F3D1}\\x{1F3D2}\\x{1F3D3}\\x{1F3F8}\\x{1F3F9}\\x{1F3FA}\\x{1F643}"
            +"\\x{1F644}\\x{1F910}\\x{1F911}\\x{1F912}\\x{1F913}\\x{1F914}\\x{1F915}\\x{1F916}\\x{1F917}"
            +"\\x{1F918}\\x{1F980}\\x{1F981}\\x{1F982}]").freeze();
    public static final UnicodeSet EMOJI_CHARS = new UnicodeSet(
            "[🕉 ✡ ☸ ☯ ✝ ☦ ⛩ ☪ ⚛ 0-9©®‼⁉℗™ℹ↔-↙↩↪⌚⌛⌨⎈⏏⏩-⏺Ⓜ▪▫▶◀●◪◻-◾☀-☄☎-☒☔☕☘-☠☢-☤☦🕉☦ ☪ ☬ ☸ ✝ 🕉☪-☬☮☯☹-☾♈-♓♠-♯♲"
                    + "♻♾♿⚐-⚜⚠⚡⚪⚫⚰⚱⚽-⚿⛄-⛈⛍-⛙⛛-⛡⛨-⛪⛰-⛵⛷-⛺⛼-✒✔-✘✝✨✳✴❄❇❌❎❓-❕❗❢-❧➕-➗"
                    + "➡➰➿⤴⤵⬅-⬇⬛⬜⭐⭕⸙〰〽㊗㊙🀄🃏🅰🅱🅾🅿🆎🆏🆑-🆚🈁🈂🈚🈯🈲-🈺🉐🉑🌀-🌬🌰-🍽🎀-🏎"
                    + "🏔-🏷🐀-📾🔀-🔿🕊🕐-🕱🕳-🕹🖁-🖣🖥-🖩🖮-🗳🗺-🙂🙅-🙏🚀-🛏🛠-🛬🛰-🛳"
                    + "{#⃣}{*⃣}{0⃣}{1⃣}{2⃣}{3⃣}{4⃣}{5⃣}{6⃣}{7⃣}{8⃣}{9⃣}]")
    .addAll(Unicode8Emoji)
    .removeAll(new UnicodeSet("[☫☬🎕⚘⸙⎈]"))
    .removeAll(new UnicodeSet("[℗⏴-⏷●◪☙☤☼-☾♩-♯♾⚐⚑⚕⚚ ⚿⛆⛍⛐⛒⛕-⛙⛛⛜⛞-⛡⛨⛼⛾-✀✆✇✑ ❢❦❧🌢🌣🎔🎘🎜🎝🏱🏲🏶📾🔾🔿🕨-🕮🕱🖁-🖆 🖈🖉🖎🖏🖒-🖔🖗-🖣🖦🖧🖩🖮-🖰🖳-🖻🖽-🗁 🗅-🗐🗔-🗛🗟🗠🗤-🗮🗰-🗲🛆-🛈🛦-🛨🛪 🛱🛲]"))
    .removeAll(new UnicodeSet("[🛉 🛊 🖑🗢☏☐☒☚-☜☞☟♲⛇✁✃✄✎✐✕✗✘  ♤  ♡  ♢ ♧❥🆏 ☻ ⛝ 0  1  2  3  4 5  6  7  8  9]"))
    .add("🗨")
    // .freeze() will freeze later
    ;
    static {
        if (IS_BETA) {
            EMOJI_CHARS.addAll("[🕺 🖤 🛑 🛒 🛴 🛵 🛶 🤙 🤚 🤛 🤜 🤝 🤞 🤠 🤡 🤢 🤣 🤤 🤥 🤦 🤧 🤰 🤳 🤴 🤵 🤶 🤷 🤸 🤹 🤺 🤻 🤼 🤽 🤾 🥀 🥁 🥂 🥃 🥄 🥅 🥆 🥇 🥈 🥉 🥊 🥋 🥐 🥑 🥒 🥓 🥔 🥕 🥖 🥗 🥘 🥙 🥚 🥛 🥜 🥝 🥞 🦅 🦆 🦇 🦈 🦉 🦊 🦋 🦌 🦍 🦎 🦏 🦐 🦑]");
        }
    }
    public static final UnicodeSet COMMON_ADDITIONS = new UnicodeSet("[➿🌍🌎🌐🌒🌖-🌘🌚🌜-🌞🌲🌳🍋🍐🍼🏇🏉🏤🐀-🐋🐏🐐🐓🐕🐖🐪👥👬👭💭💶💷📬📭📯📵🔀-🔂🔄-🔉🔕🔬🔭🕜-🕧😀😇😈😎😐😑😕😗😙😛😟😦😧😬😮😯😴😶🚁🚂🚆🚈🚊🚋🚍🚎🚐🚔🚖🚘🚛-🚡🚣🚦🚮-🚱🚳-🚵🚷🚸🚿🛁-🛅]").freeze();
    static final UnicodeSet ASCII_LETTER_HYPHEN = new UnicodeSet('-', '-', 'A', 'Z', 'a', 'z', '’', '’').freeze();
    static final UnicodeSet LATIN1_LETTER = new UnicodeSet("[[:L:]&[\\x{0}-\\x{FF}}]]").freeze();
    static final UnicodeSet KEYWORD_CHARS = new UnicodeSet(Emoji.ASCII_LETTER_HYPHEN)
    .add('0','9')
    .addAll(" +:.&")
    .addAll(LATIN1_LETTER)
    .freeze();
    static final UnicodeSet KEYCAPS = new UnicodeSet("[{#⃣}{*⃣}{0⃣}{1⃣}{2⃣}{3⃣}{4⃣}{5⃣}{6⃣}{7⃣}{8⃣}{9⃣}]").freeze();

    //public static final UnicodeSet SKIP_ANDROID = new UnicodeSet("[♨ ⚠ ▶ ◀ ✉ ✏ ✒ ✂ ⬆ ↗ ➡ ↘ ⬇ ↙ ⬅ ↖ ↕ ↔ ↩ ↪ ⤴ ⤵ ♻ ☑ ✔ ✖ 〽 ✳ ✴ ❇ ▪ ▫ ◻ ◼ ‼ ⁉ 〰 © ® 🅰 🅱 ℹ Ⓜ 🅾 🅿 ™ 🈂 🈷 ㊗ ㊙]").freeze();

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
            if (part.startsWith("x")) {
                continue;
            }
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

    public static final UnicodeSet FACES = new UnicodeSet("[☺ ☹ 🙁 🙂 😀-😆 😉-😷 😇 😈 👿 🙃 🙄 🤐-🤕 🤗]").freeze();
    public static final char EMOJI_VARIANT = '\uFE0F';
    public static final char TEXT_VARIANT = '\uFE0E';
    public static final char JOINER = '\u200D';

    static final int FIRST_REGIONAL = 0x1F1E6;
    static final int LAST_REGIONAL = 0x1F1FF;

    public static final UnicodeSet REGIONAL_INDICATORS = new UnicodeSet(FIRST_REGIONAL,LAST_REGIONAL).freeze();
    public static final UnicodeSet DEFECTIVE = new UnicodeSet("[0123456789*#]").addAll(REGIONAL_INDICATORS).freeze();

    //    static final UnicodeSet EXCLUDE = new UnicodeSet(
    //    "[🂠-🂮 🂱-🂿 🃁-🃎 🃑-🃵 🀀-🀃 🀅-🀫 〠🕲⍾☸🀰-🂓 🙬 🙭 🙮 🙯🗴🗵🗶🗷🗸🗹★☆⛫\uFFFC⛤-⛧ ⌤⌥⌦⌧⌫⌬⎆⎇⎋⎗⎘⎙⎚⏣⚝⛌⛚⛬⛭⛮⛯⛶⛻✓🆊\\U0001F544-\\U0001F549" +
    //    "☖  ☗  ⛉  ⛊  ⚀  ⚁  ⚂  ⚃  ⚄  ⚅ ♔  ♕  ♖  ♗  ♘  ♙  ♚  ♛  ♜  ♝  ♞  ♟  ⛀  ⛁  ⛂ ⛃" +
    //    "]").freeze();
    //    // 🖫🕾🕿🕻🕼🕽🕾🕿🖀🖪🖬🖭

    // TODO Remove EMOJI_CHARS and all that depend on them; change to use EmojiData.
    public static final UnicodeSet FLAGS = new UnicodeSet();
    public static final UnicodeSet EMOJI_SINGLETONS = new UnicodeSet(EMOJI_CHARS)
    .removeAll(EMOJI_CHARS.strings())
    .addAll(FIRST_REGIONAL,LAST_REGIONAL)
    .addAll('0','9')
    .add('*')
    .add('#')
    .freeze();
    static final UnicodeSet EMOJI_CHARS_FLAT = new UnicodeSet(EMOJI_CHARS)
    .addAll(FIRST_REGIONAL,LAST_REGIONAL)
    .removeAll(EMOJI_CHARS.strings())
    .addAll(new UnicodeSet("[{*⃣} {#⃣}]"))
    .add(EMOJI_VARIANT)
    .add(TEXT_VARIANT)
    .add(JOINER)
    .removeAll(new UnicodeSet("[[:L:][:M:][:^nt=none:]+_-]"))
    .freeze();

    static final UnicodeSet EMOJI_CHARS_WITHOUT_FLAGS = new UnicodeSet(EMOJI_CHARS).freeze();
    static {
        CLDRConfig config = CLDRConfig.getInstance();
        //StandardCodes sc = config.getStandardCodes();
        SupplementalDataInfo sdi = config.getSupplementalDataInfo();
        Set<String> container = new TreeSet<>();
        Set<String> contained = new TreeSet<>();
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
            FLAGS.add(getHexFromFlagCode(s));
        }
        FLAGS.freeze();
        EMOJI_CHARS.addAll(FLAGS).freeze();
    }

    public static boolean isRegionalIndicator(int firstCodepoint) {
        return FIRST_REGIONAL <= firstCodepoint && firstCodepoint <= Emoji.LAST_REGIONAL;
    }

    public static final char ENCLOSING_KEYCAP = '\u20E3';
    static final Comparator<String> CODEPOINT_LENGTH = new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            return o1.codePointCount(0, o1.length()) - o2.codePointCount(0, o2.length());
        }
    };


    public static void main(String[] args) {
        if (!EMOJI_CHARS.containsAll(Unicode8Emoji)) {
            throw new IllegalArgumentException();
        }
        if (!EMOJI_CHARS.contains("🗨")) {
            throw new IllegalArgumentException();
        }
        System.out.println("Singletons:\n" + EMOJI_SINGLETONS.toPattern(false));
        System.out.println("Without flags:\n" + EMOJI_CHARS_WITHOUT_FLAGS.toPattern(false));
        System.out.println("Flags:\n" + FLAGS.toPattern(false));
        System.out.println("With flags:\n" + EMOJI_CHARS.toPattern(false));
        System.out.println("FLAT:\n" + EMOJI_CHARS_FLAT.toPattern(false));
        System.out.println("FLAT:\n" + EMOJI_CHARS_FLAT.toPattern(true));
    }

    //    private static final UnicodeSet FITZ_OPTIONAL = new UnicodeSet("[\\u261D \\u261F \\u2639-\\u263B \\u270A-\\u270D \\U0001F3C2-\\U0001F3C4 \\U0001F3C7 \\U0001F3CA \\U0001F440-\\U0001F450 \\U0001F47F \\U0001F483 \\U0001F485 \\U0001F48B \\U0001F4AA \\U0001F58E-\\U0001F597 \\U0001F59E-\\U0001F5A3 \\U0001F5E2 \\U0001F600-\\U0001F637 \\U0001F641 \\U0001F642 \\U0001F64C \\U0001F64F \\U0001F6A3 \\U0001F6B4-\\U0001F6B6 \\U0001F6C0]");
    //    private static final UnicodeSet FITZ_MINIMAL = new UnicodeSet("[\\U0001F385 \\U0001F466- \\U0001F478 \\U0001F47C \\U0001F481 \\U0001F482 \\U0001F486 \\U0001F487 \\U0001F48F \\U0001F491 \\U0001F645- \\U0001F647 \\U0001F64B \\U0001F64D \\U0001F64E]");
    static final UnicodeSet ASCII_LETTERS = new UnicodeSet("[A-Za-z]").freeze();
    static final String EMOJI_VARIANT_STRING = String.valueOf(EMOJI_VARIANT);
    static final String TEXT_VARIANT_STRING = String.valueOf(TEXT_VARIANT);
    static final String JOINER_STRING = String.valueOf(JOINER);

    public static String getLabelFromLine(Output<Set<String>> newLabel, String line) {
        line = line.replace(EMOJI_VARIANT_STRING, "").replace(TEXT_VARIANT_STRING, "").trim();
        int tabPos = line.indexOf('\t');
        //        if (tabPos < 0 && Emoji.EMOJI_CHARS.contains(getEmojiSequence(line, 0))) {
        //            tabPos = line.length();
        //            
        //        }
        if (tabPos >= 0) {
            newLabel.value.clear();
            String[] temp = line.substring(0,tabPos).trim().split(",\\s*");
            for (String part : temp) {
                if (KEYWORD_CHARS.containsAll(part)) {
                    newLabel.value.add(part);
                } else {
                    throw new IllegalArgumentException("Bad line format: " + line);
                }
            }
            line = line.substring(tabPos + 1);
        }
        return line;
    }
    //    private static final Transform<String,String> WINDOWS_URL = new Transform<String,String>() {
    //        public String transform(String s) {
    //            String base = "images /windows/windows_";
    //            String separator = "_";
    //            return base + Emoji.buildFileName(s, separator) + ".png";
    //        }
    //
    //    };

    public static String getEmojiSequence(String line, int i) {
        // take the first character.
        int firstCodepoint = line.codePointAt(i);
        int firstLen = Character.charCount(firstCodepoint);
        if (i + firstLen == line.length()) {
            return line.substring(i, i+firstLen);
        }
        int secondCodepoint = line.codePointAt(i+firstLen);
        int secondLen = Character.charCount(secondCodepoint);
        if (secondCodepoint == ENCLOSING_KEYCAP
                || (isRegionalIndicator(firstCodepoint) && isRegionalIndicator(secondCodepoint))) {
            return line.substring(i, i+firstLen+secondLen);
        }
        // ZWJ sequence
        //        if ((secondCodepoint == EMOJI_VARIANT || secondCodepoint == TEXT_VARIANT) && i + firstLen + secondLen < line.length()) {
        //            int codePoint3 = line.codePointAt(i+firstLen+secondLen);
        //            int len3 = Character.charCount(codePoint3);
        //            if (codePoint3 == ENCLOSING_KEYCAP) {
        //                return line.substring(i, i+firstLen+secondLen+len3);
        //            }
        //        }
        return line.substring(i, i+firstLen);
    }

    static final UnicodeSet U80 = new UnicodeSet("[🌭🌮🌯🍾🍿🏏🏐🏑🏒🏓🏸🏹🏺🏻🏼🏽🏾🏿📿🕋🕌🕍🕎🙃🙄🛐🤀🤐🤑🤒🤓🤔🤕🤖🤗🤘🦀🦁🦂🦃🦄🧀]").freeze();
    static final UnicodeSet U90 = new UnicodeSet("[\\x{1F57A} \\x{1F5A4} \\x{1F6D1} \\x{1F6F4} \\x{1F6F5} \\x{1F919} \\x{1F91A} \\x{1F91B} \\x{1F91C} \\x{1F91D} \\x{1F91E} \\x{1F920} \\x{1F921} \\x{1F922} \\x{1F923} \\x{1F924} \\x{1F925} \\x{1F926} \\x{1F930} \\x{1F933} \\x{1F934} \\x{1F935} \\x{1F936} \\x{1F937} \\x{1F940} \\x{1F942} \\x{1F950} \\x{1F951} \\x{1F952} \\x{1F953} \\x{1F954} \\x{1F955} \\x{1F985} \\x{1F986} \\x{1F987} \\x{1F988} \\x{1F989} \\x{1F98A}]").freeze();
    public static final Transliterator UNESCAPE = Transliterator.getInstance("hex-any/Perl");

    static String getImageFilenameFromChars(Emoji.Source type, String chars) {
//        if (type == Emoji.Source.android && Emoji.SKIP_ANDROID.contains(chars)) { // hack to exclude certain android
//            return null;
//        }
        String core = buildFileName(chars, "_");
        String suffix = ".png";
        if (type != null && type.isGif()) {
            suffix = ".gif";
        }
        return type.getPrefix() + "/" + type.getPrefix() + "_" + core + suffix;
    }

    static String getFlagCode(String chars) {
        int firstCodepoint = chars.codePointAt(0);
        if (!isRegionalIndicator(firstCodepoint)) {
            return null;
        }
        int firstLen = Character.charCount(firstCodepoint);
        int secondCodepoint = firstLen >= chars.length() ? 0 : chars.codePointAt(firstLen);
        if (!isRegionalIndicator(secondCodepoint)) {
            return null;
        }
        secondCodepoint = chars.codePointAt(2);
        String cc = (char) (firstCodepoint - FIRST_REGIONAL + 'A')
                + ""
                + (char) (secondCodepoint - FIRST_REGIONAL + 'A');
        // String remapped = REMAP_FLAGS.get(cc);
        // if (remapped != null) {
        // cc = remapped;
        // }
        // if (REPLACEMENT_CHARACTER.equals(cc)) {
        // return null;
        // }
        return cc;
    }

    static public File getImageFile(Source type, String chars) {
        String filename = getImageFilenameFromChars(type, chars);
        if (filename != null) {
            File file = new File(IMAGES_OUTPUT_DIR, filename);
            if (file.exists()) {
                return file;
            }
        }
        return null;
    }

    public static File getBestFile(String s, Source... doFirst) {
        for (Source source : Emoji.orderedEnum(doFirst)) {
            File file = getImageFile(source, s);
            if (file != null) {
                return file;
            }
        }
        return null;
    }

    public static Iterable<Source> orderedEnum(Source... doFirst) {
        if (doFirst.length == 0) {
            return Arrays.asList(Source.values());
        }
        LinkedHashSet<Source> ordered = new LinkedHashSet<>(Arrays.asList(doFirst));
        ordered.addAll(Arrays.asList(Source.values()));
        return ordered;
    }

    static final IndexUnicodeProperties    LATEST  = IndexUnicodeProperties.make(VERSION_TO_GENERATE_UNICODE);
    static final IndexUnicodeProperties    BETA  = IndexUnicodeProperties.make(VERSION_BETA_UNICODE);

    public static String getEmojiVariant(String browserChars, String variant) {
        return getEmojiVariant(browserChars, variant, null);
    }
    
    public static String getEmojiVariant(String browserChars, String variant, UnicodeSet extraVariants) {
        int first = browserChars.codePointAt(0);
        String probe = new StringBuilder()
        .appendCodePoint(first)
        .append(variant).toString();
        if (Emoji.STANDARDIZED_VARIANT.get(probe) != null || extraVariants != null && extraVariants.contains(first)) {
            browserChars = probe + browserChars.substring(Character.charCount(first));
        }
        return browserChars;
    }

    public static final UnicodeMap<String> STANDARDIZED_VARIANT = BETA.load(UcdProperty.Standardized_Variant);
    
    public static final UnicodeSet HAS_EMOJI_VS = new UnicodeSet();
    
    static {
        for (String vs : Emoji.STANDARDIZED_VARIANT.keySet()) {
            if (vs.contains(Emoji.TEXT_VARIANT_STRING)) {
                Emoji.HAS_EMOJI_VS.add(vs.codePointAt(0));
            }
        }
        Emoji.HAS_EMOJI_VS.freeze();
    }

    static final UnicodeMap<Age_Values>        VERSION_ENUM            = BETA.loadEnum(UcdProperty.Age, Age_Values.class);

    public static String getName(String s, boolean tolower, UnicodeMap<String> extraNames) {
        String flag = Emoji.getFlagRegionName(s);
        if (flag != null) {
            String result = Emoji.LOCALE_DISPLAY.regionDisplayName(flag);
            if (result.endsWith(" SAR China")) {
                result = result.substring(0, result.length() - " SAR China".length());
            }
            return (tolower ? "flag for " : "Flag for ") + result;
        }
        final int firstCodePoint = s.codePointAt(0);
        String name = Emoji.NAME.get(firstCodePoint);
        if (name == null && extraNames != null) {
            name = extraNames.get(s);
        }
        if (s.indexOf(ENCLOSING_KEYCAP) >= 0) {
            name = "Keycap " + name;
            return (tolower ? name.toLowerCase(Locale.ENGLISH) : name);
        }
        final int firstCount = Character.charCount(firstCodePoint);
        if (s.length() > firstCount) {
            int cp2 = s.codePointAt(firstCount);
            final EmojiDatum edata = EmojiData.EMOJI_DATA.getData(cp2);
            if (edata != null && ModifierStatus.modifier == edata.modifierStatus) {
                name += ", " + Emoji.shortName(cp2);
            } else {
                StringBuffer nameBuffer = new StringBuffer();
                boolean sep = false;
                if (s.indexOf(JOINER) >= 0) {
                    String title = "";
                    if (s.indexOf(0x1F48B) >= 0) { // KISS MARK
                        title = "Kiss: ";
                    } else if (s.indexOf(0x2764) >= 0) { // HEART
                        title = "Couple with heart: ";
                    } else if (s.indexOf(0x1F441) < 0) { // !EYE
                        title = "Family: ";
                    }
                    nameBuffer.append(title);
                }
                for (int cp : CharSequences.codePoints(s)) {
                    if (cp == JOINER || cp == EMOJI_VARIANT || cp == 0x2764 || cp == 0x1F48B) { // heart, kiss
                        continue;
                    }
                    if (sep) {
                        nameBuffer.append(", ");
                    } else {
                        sep = true;
                    }
                    nameBuffer.append(Emoji.NAME.get(cp));
    
                    //                    nameBuffer.append(cp == Emoji.JOINER ? "zwj" 
                    //                            : cp == Emoji.EMOJI_VARIANT ? "emoji-vs" 
                    //                                    : NAME.get(cp));
                }
                name = nameBuffer.toString();
            }
        }
        return name == null ? "UNNAMED" : (tolower ? name.toLowerCase(Locale.ENGLISH) : name);
    }

    static String shortName(int cp2) {
        return Emoji.NAME.get(cp2).substring("emoji modifier fitzpatrick ".length());
    }

    public static String getFlagRegionName(String s) {
        String result = getFlagCode(s);
        if (result != null) {
            result = Emoji.LOCALE_DISPLAY.regionDisplayName(result);
            if (result.endsWith(" SAR China")) {
                result = result.substring(0, result.length() - " SAR China".length());
            } else if (result.contains("(")) {
                result = result.substring(0, result.indexOf('(')) + result.substring(result.lastIndexOf(')') + 1);
            }
            result = result.replaceAll("\\s\\s+", " ").trim();
        }
        return result;
    }

    // Certain resources we always load from latest.
    
    static final UnicodeMap<String>        NAME                        = BETA.load(UcdProperty.Name);

    static final LocaleDisplayNames        LOCALE_DISPLAY              = LocaleDisplayNames.getInstance(ULocale.ENGLISH);

    static final transient Collection<Age_Values> output = new TreeSet(Collections.reverseOrder());

    static Age_Values getNewest(String s) {
        synchronized (Emoji.output) {
            Emoji.output.clear();
            Emoji.getValues(s, VERSION_ENUM, Emoji.output);
            return Emoji.output.iterator().next();
        }
    }

    // should be method on UnicodeMap
    static final <T, C extends Collection<T>> C getValues(String source, UnicodeMap<T> data, C output) {
        for (int cp : CharSequences.codePoints(source)) {
            T datum = data.get(cp);
            if (datum != null) {
                output.add(datum);
            }
        }
        return output;
    }

    static final String INTERNAL_OUTPUT_DIR = Settings.OTHER_WORKSPACE_DIRECTORY + "Generated/emoji/";
}
