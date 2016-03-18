package org.unicode.jsp;

import java.text.ParsePosition;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import org.unicode.jsp.UnicodeProperty.PatternMatcher;
import org.unicode.jsp.UnicodeSetUtilities.ComparisonMatcher.Relation;

import com.ibm.icu.impl.MultiComparator;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UTF16.StringComparator;
import com.ibm.icu.util.ULocale;

public class UnicodeSetUtilities {

    public static final UnicodeSet EMOJI = new UnicodeSet("[:emoji:]");
    public static final UnicodeSet Emoji_Presentation = new UnicodeSet("[:Emoji_Presentation:]");
    public static final UnicodeSet Emoji_Modifier = new UnicodeSet("[:Emoji_Modifier:]");
    public static final UnicodeSet Emoji_Modifier_Base = new UnicodeSet("[:Emoji_Modifier_Base:]");

    public static final UnicodeSet SINGLETONS = new UnicodeSet("[©®‼⁉™ℹ↔-↙↩↪⌚⌛⌨⏏⏩-⏳⏸-⏺Ⓜ▪▫▶◀◻-◾☀-☄☎☑☔☕☘☝☠☢☣☦☪☮☯☸-☺♈-♓♠♣♥♦♨♻♿⚒-⚔⚖⚗⚙⚛⚜⚠⚡"
            + "⚪⚫⚰⚱⚽⚾⛄⛅⛈⛎⛏⛑⛓⛔⛩⛪⛰-⛵⛷-⛺⛽✂✅✈-✍✏✒✔✖✝✡✨✳✴❄❇❌❎❓-❕❗❣❤➕-➗➡➰➿⤴⤵⬅-⬇⬛⬜⭐⭕〰〽㊗㊙🀄🃏🅰🅱🅾🅿🆎🆑-🆚🈁🈂🈚🈯🈲-🈺"
            + "🉐🉑🌀-🌡🌤-🎓🎖🎗🎙-🎛🎞-🏰🏳-🏵🏷-📽📿-🔽🕉-🕎🕐-🕧🕯🕰🕳-🕹🖇🖊-🖍🖐🖕🖖🖥🖨🖱🖲🖼🗂-🗄🗑-🗓🗜-🗞🗡🗣🗯🗳🗺-🙏🚀-🛅🛋-🛐🛠-🛥🛩🛫🛬🛰🛳🤐-🤘🦀-🦄🧀]").freeze();    
    public static final UnicodeSet KEYCAPS = new UnicodeSet("[{#⃣}{*⃣}{0⃣}{1⃣}{2⃣}{3⃣}{4⃣}{5⃣}{6⃣}{7⃣}{8⃣}{9⃣}]").freeze();
    public static final UnicodeSet FLAGS = new UnicodeSet("[{🇦🇨}"
            + "{🇦🇩}{🇦🇪}{🇦🇫}{🇦🇬}{🇦🇮}{🇦🇱}{🇦🇲}{🇦🇴}{🇦🇶}{🇦🇷}{🇦🇸}{🇦🇹}{🇦🇺}{🇦🇼}{🇦🇽}{🇦🇿}{🇧🇦}{🇧🇧}{🇧🇩}{🇧🇪}{🇧🇫}{🇧🇬}{🇧🇭}{🇧🇮}{🇧🇯}{🇧🇱}{🇧🇲}{🇧🇳}{🇧🇴}{🇧🇶}{🇧🇷}{🇧🇸}"
            + "{🇧🇹}{🇧🇻}{🇧🇼}{🇧🇾}{🇧🇿}{🇨🇦}{🇨🇨}{🇨🇩}{🇨🇫}{🇨🇬}{🇨🇭}{🇨🇮}{🇨🇰}{🇨🇱}{🇨🇲}{🇨🇳}{🇨🇴}{🇨🇵}{🇨🇷}{🇨🇺}{🇨🇻}{🇨🇼}{🇨🇽}{🇨🇾}{🇨🇿}{🇩🇪}{🇩🇬}{🇩🇯}{🇩🇰}{🇩🇲}{🇩🇴}"
            + "{🇩🇿}{🇪🇦}{🇪🇨}{🇪🇪}{🇪🇬}{🇪🇭}{🇪🇷}{🇪🇸}{🇪🇹}{🇪🇺}{🇫🇮}{🇫🇯}{🇫🇰}{🇫🇲}{🇫🇴}{🇫🇷}{🇬🇦}{🇬🇧}{🇬🇩}{🇬🇪}{🇬🇫}{🇬🇬}{🇬🇭}{🇬🇮}{🇬🇱}{🇬🇲}{🇬🇳}{🇬🇵}{🇬🇶}{🇬🇷}"
            + "{🇬🇸}{🇬🇹}{🇬🇺}{🇬🇼}{🇬🇾}{🇭🇰}{🇭🇲}{🇭🇳}{🇭🇷}{🇭🇹}{🇭🇺}{🇮🇨}{🇮🇩}{🇮🇪}{🇮🇱}{🇮🇲}{🇮🇳}{🇮🇴}{🇮🇶}{🇮🇷}{🇮🇸}{🇮🇹}{🇯🇪}{🇯🇲}{🇯🇴}{🇯🇵}{🇰🇪}{🇰🇬}{🇰🇭}{🇰🇮}{🇰🇲}"
            + "{🇰🇳}{🇰🇵}{🇰🇷}{🇰🇼}{🇰🇾}{🇰🇿}{🇱🇦}{🇱🇧}{🇱🇨}{🇱🇮}{🇱🇰}{🇱🇷}{🇱🇸}{🇱🇹}{🇱🇺}{🇱🇻}{🇱🇾}{🇲🇦}{🇲🇨}{🇲🇩}{🇲🇪}{🇲🇫}{🇲🇬}{🇲🇭}{🇲🇰}{🇲🇱}{🇲🇲}{🇲🇳}{🇲🇴}{🇲🇵}{🇲🇶}{🇲🇷}{🇲🇸}"
            + "{🇲🇹}{🇲🇺}{🇲🇻}{🇲🇼}{🇲🇽}{🇲🇾}{🇲🇿}{🇳🇦}{🇳🇨}{🇳🇪}{🇳🇫}{🇳🇬}{🇳🇮}{🇳🇱}{🇳🇴}{🇳🇵}{🇳🇷}{🇳🇺}{🇳🇿}{🇴🇲}{🇵🇦}{🇵🇪}{🇵🇫}{🇵🇬}{🇵🇭}{🇵🇰}{🇵🇱}{🇵🇲}{🇵🇳}{🇵🇷}{🇵🇸}"
            + "{🇵🇹}{🇵🇼}{🇵🇾}{🇶🇦}{🇷🇪}{🇷🇴}{🇷🇸}{🇷🇺}{🇷🇼}{🇸🇦}{🇸🇧}{🇸🇨}{🇸🇩}{🇸🇪}{🇸🇬}{🇸🇭}{🇸🇮}{🇸🇯}{🇸🇰}{🇸🇱}{🇸🇲}{🇸🇳}{🇸🇴}{🇸🇷}{🇸🇸}{🇸🇹}{🇸🇻}{🇸🇽}{🇸🇾}{🇸🇿}{🇹🇦}{🇹🇨}"
            + "{🇹🇩}{🇹🇫}{🇹🇬}{🇹🇭}{🇹🇯}{🇹🇰}{🇹🇱}{🇹🇲}{🇹🇳}{🇹🇴}{🇹🇷}{🇹🇹}{🇹🇻}{🇹🇼}{🇹🇿}{🇺🇦}{🇺🇬}{🇺🇲}{🇺🇸}{🇺🇾}{🇺🇿}{🇻🇦}{🇻🇨}{🇻🇪}{🇻🇬}{🇻🇮}{🇻🇳}{🇻🇺}{🇼🇫}"
            + "{🇼🇸}{🇽🇰}{🇾🇪}{🇾🇹}{🇿🇦}{🇿🇲}{🇿🇼}]").freeze();
    public static final UnicodeSet GROUPS = new UnicodeSet("[💏 💑 👪 {👨‍❤️‍👨}{👨‍❤️‍💋‍👨}{👨‍👨‍👦}{👨‍👨‍👦‍👦}{👨‍👨‍👧}{👨‍👨‍👧‍👦}{👨‍👨‍👧‍👧}{👨‍👩‍👦}{👨‍👩‍👦‍👦}{👨‍👩‍👧}{👨‍👩‍👧‍👦}{👨‍👩‍👧‍👧}{👩‍❤️‍👩}{👩‍❤️‍💋‍👩}{👩‍👩‍👦}{👩‍👩‍👦‍👦}{👩‍👩‍👧}{👩‍👩‍👧‍👦}{👩‍👩‍👧‍👧}]").freeze();
    public static final UnicodeSet PRIMARY = new UnicodeSet("[🎅👦-👩👮👰-👸👼💁💂💆💇🙅-🙇🙋🙍🙎"
            + "{🎅🏻}{🎅🏼}{🎅🏽}{🎅🏾}{🎅🏿}{👦🏻}{👦🏼}{👦🏽}{👦🏾}{👦🏿}{👧🏻}{👧🏼}{👧🏽}{👧🏾}{👧🏿}{👨🏻}{👨🏼}{👨🏽}{👨🏾}{👨🏿}{👩🏻}{👩🏼}{👩🏽}{👩🏾}{👩🏿}{👮🏻}{👮🏼}{👮🏽}{👮🏾}{👮🏿}{👰🏻}{👰🏼}{👰🏽}{👰🏾}{👰🏿}{👱🏻}{👱🏼}{👱🏽}{👱🏾}{👱🏿}{👲🏻}{👲🏼}{👲🏽}{👲🏾}{👲🏿}{👳🏻}{👳🏼}{👳🏽}{👳🏾}{👳🏿}{👴🏻}{👴🏼}{👴🏽}{👴🏾}{👴🏿}{👵🏻}{👵🏼}{👵🏽}{👵🏾}{👵🏿}{👶🏻}{👶🏼}{👶🏽}{👶🏾}{👶🏿}{👷🏻}{👷🏼}{👷🏽}{👷🏾}{👷🏿}{👸🏻}{👸🏼}{👸🏽}{👸🏾}{👸🏿}{👼🏻}{👼🏼}{👼🏽}{👼🏾}{👼🏿}{💁🏻}{💁🏼}{💁🏽}{💁🏾}{💁🏿}{💂🏻}{💂🏼}{💂🏽}{💂🏾}{💂🏿}{💆🏻}{💆🏼}{💆🏽}{💆🏾}{💆🏿}{💇🏻}{💇🏼}{💇🏽}{💇🏾}{💇🏿}{🙅🏻}{🙅🏼}{🙅🏽}{🙅🏾}{🙅🏿}{🙆🏻}{🙆🏼}{🙆🏽}{🙆🏾}{🙆🏿}{🙇🏻}{🙇🏼}{🙇🏽}{🙇🏾}{🙇🏿}{🙋🏻}{🙋🏼}{🙋🏽}{🙋🏾}{🙋🏿}{🙍🏻}{🙍🏼}{🙍🏽}{🙍🏾}{🙍🏿}{🙎🏻}{🙎🏼}{🙎🏽}{🙎🏾}{🙎🏿}]").freeze();
    public static final UnicodeSet FACE = new UnicodeSet("[☺ ☹ 🙁 🙂 😀-😆 😉-😷 😇 😈 👿 🙃 🙄 🤐-🤕 🤗]").freeze();
           // + "{☹🏻}{☹🏼}{☹🏽}{☹🏾}{☹🏿}{☺🏻}{☺🏼}{☺🏽}{☺🏾}{☺🏿}{👿🏻}{👿🏼}{👿🏽}{👿🏾}{👿🏿}{😀🏻}{😀🏼}{😀🏽}{😀🏾}{😀🏿}{😁🏻}{😁🏼}{😁🏽}{😁🏾}{😁🏿}{😂🏻}{😂🏼}{😂🏽}{😂🏾}{😂🏿}{😃🏻}{😃🏼}{😃🏽}{😃🏾}{😃🏿}{😄🏻}{😄🏼}{😄🏽}{😄🏾}{😄🏿}{😅🏻}{😅🏼}{😅🏽}{😅🏾}{😅🏿}{😆🏻}{😆🏼}{😆🏽}{😆🏾}{😆🏿}{😇🏻}{😇🏼}{😇🏽}{😇🏾}{😇🏿}{😈🏻}{😈🏼}{😈🏽}{😈🏾}{😈🏿}{😉🏻}{😉🏼}{😉🏽}{😉🏾}{😉🏿}{😊🏻}{😊🏼}{😊🏽}{😊🏾}{😊🏿}{😋🏻}{😋🏼}{😋🏽}{😋🏾}{😋🏿}{😌🏻}{😌🏼}{😌🏽}{😌🏾}{😌🏿}{😍🏻}{😍🏼}{😍🏽}{😍🏾}{😍🏿}{😎🏻}{😎🏼}{😎🏽}{😎🏾}{😎🏿}{😏🏻}{😏🏼}{😏🏽}{😏🏾}{😏🏿}{😐🏻}{😐🏼}{😐🏽}{😐🏾}{😐🏿}{😑🏻}{😑🏼}{😑🏽}{😑🏾}{😑🏿}{😒🏻}{😒🏼}{😒🏽}{😒🏾}{😒🏿}{😓🏻}{😓🏼}{😓🏽}{😓🏾}{😓🏿}{😔🏻}{😔🏼}{😔🏽}{😔🏾}{😔🏿}{😕🏻}{😕🏼}{😕🏽}{😕🏾}{😕🏿}{😖🏻}{😖🏼}{😖🏽}{😖🏾}{😖🏿}{😗🏻}{😗🏼}{😗🏽}{😗🏾}{😗🏿}{😘🏻}{😘🏼}{😘🏽}{😘🏾}{😘🏿}{😙🏻}{😙🏼}{😙🏽}{😙🏾}{😙🏿}{😚🏻}{😚🏼}{😚🏽}{😚🏾}{😚🏿}{😛🏻}{😛🏼}{😛🏽}{😛🏾}{😛🏿}{😜🏻}{😜🏼}{😜🏽}{😜🏾}{😜🏿}{😝🏻}{😝🏼}{😝🏽}{😝🏾}{😝🏿}{😞🏻}{😞🏼}{😞🏽}{😞🏾}{😞🏿}{😟🏻}{😟🏼}{😟🏽}{😟🏾}{😟🏿}{😠🏻}{😠🏼}{😠🏽}{😠🏾}{😠🏿}{😡🏻}{😡🏼}{😡🏽}{😡🏾}{😡🏿}{😢🏻}{😢🏼}{😢🏽}{😢🏾}{😢🏿}{😣🏻}{😣🏼}{😣🏽}{😣🏾}{😣🏿}{😤🏻}{😤🏼}{😤🏽}{😤🏾}{😤🏿}{😥🏻}{😥🏼}{😥🏽}{😥🏾}{😥🏿}{😦🏻}{😦🏼}{😦🏽}{😦🏾}{😦🏿}{😧🏻}{😧🏼}{😧🏽}{😧🏾}{😧🏿}{😨🏻}{😨🏼}{😨🏽}{😨🏾}{😨🏿}{😩🏻}{😩🏼}{😩🏽}{😩🏾}{😩🏿}{😪🏻}{😪🏼}{😪🏽}{😪🏾}{😪🏿}{😫🏻}{😫🏼}{😫🏽}{😫🏾}{😫🏿}{😬🏻}{😬🏼}{😬🏽}{😬🏾}{😬🏿}{😭🏻}{😭🏼}{😭🏽}{😭🏾}{😭🏿}{😮🏻}{😮🏼}{😮🏽}{😮🏾}{😮🏿}{😯🏻}{😯🏼}{😯🏽}{😯🏾}{😯🏿}{😰🏻}{😰🏼}{😰🏽}{😰🏾}{😰🏿}{😱🏻}{😱🏼}{😱🏽}{😱🏾}{😱🏿}{😲🏻}{😲🏼}{😲🏽}{😲🏾}{😲🏿}{😳🏻}{😳🏼}{😳🏽}{😳🏾}{😳🏿}{😴🏻}{😴🏼}{😴🏽}{😴🏾}{😴🏿}{😵🏻}{😵🏼}{😵🏽}{😵🏾}{😵🏿}{😶🏻}{😶🏼}{😶🏽}{😶🏾}{😶🏿}{😷🏻}{😷🏼}{😷🏽}{😷🏾}{😷🏿}{🙁🏻}{🙁🏼}{🙁🏽}{🙁🏾}{🙁🏿}{🙂🏻}{🙂🏼}{🙂🏽}{🙂🏾}{🙂🏿}{🙃🏻}{🙃🏼}{🙃🏽}{🙃🏾}{🙃🏿}{🙄🏻}{🙄🏼}{🙄🏽}{🙄🏾}{🙄🏿}{🤐🏻}{🤐🏼}{🤐🏽}{🤐🏾}{🤐🏿}{🤑🏻}{🤑🏼}{🤑🏽}{🤑🏾}{🤑🏿}{🤒🏻}{🤒🏼}{🤒🏽}{🤒🏾}{🤒🏿}{🤓🏻}{🤓🏼}{🤓🏽}{🤓🏾}{🤓🏿}{🤔🏻}{🤔🏼}{🤔🏽}{🤔🏾}{🤔🏿}{🤕🏻}{🤕🏼}{🤕🏽}{🤕🏾}{🤕🏿}{🤗🏻}{🤗🏼}{🤗🏽}{🤗🏾}{🤗🏿}]").freeze();
    public static final UnicodeSet SECONDARY = new UnicodeSet("[☝✊-✍🏂-🏄🏇🏊👂👃👆-👐💃💅💪🖐🖕 🖖🙌🙏🚣🚴-🚶🛀🤘"
            + "{☝🏻}{☝🏼}{☝🏽}{☝🏾}{☝🏿}{✊🏻}{✊🏼}{✊🏽}{✊🏾}{✊🏿}{✋🏻}{✋🏼}{✋🏽}{✋🏾}{✋🏿}{✌🏻}{✌🏼}{✌🏽}{✌🏾}{✌🏿}{✍🏻}{✍🏼}{✍🏽}{✍🏾}{✍🏿}{🏂🏻}{🏂🏼}{🏂🏽}{🏂🏾}{🏂🏿}{🏃🏻}{🏃🏼}{🏃🏽}{🏃🏾}{🏃🏿}{🏄🏻}{🏄🏼}{🏄🏽}{🏄🏾}{🏄🏿}{🏇🏻}{🏇🏼}{🏇🏽}{🏇🏾}{🏇🏿}{🏊🏻}{🏊🏼}{🏊🏽}{🏊🏾}{🏊🏿}{👂🏻}{👂🏼}{👂🏽}{👂🏾}{👂🏿}{👃🏻}{👃🏼}{👃🏽}{👃🏾}{👃🏿}{👆🏻}{👆🏼}{👆🏽}{👆🏾}{👆🏿}{👇🏻}{👇🏼}{👇🏽}{👇🏾}{👇🏿}{👈🏻}{👈🏼}{👈🏽}{👈🏾}{👈🏿}{👉🏻}{👉🏼}{👉🏽}{👉🏾}{👉🏿}{👊🏻}{👊🏼}{👊🏽}{👊🏾}{👊🏿}{👋🏻}{👋🏼}{👋🏽}{👋🏾}{👋🏿}{👌🏻}{👌🏼}{👌🏽}{👌🏾}{👌🏿}{👍🏻}{👍🏼}{👍🏽}{👍🏾}{👍🏿}{👎🏻}{👎🏼}{👎🏽}{👎🏾}{👎🏿}{👏🏻}{👏🏼}{👏🏽}{👏🏾}{👏🏿}{👐🏻}{👐🏼}{👐🏽}{👐🏾}{👐🏿}{💃🏻}{💃🏼}{💃🏽}{💃🏾}{💃🏿}{💅🏻}{💅🏼}{💅🏽}{💅🏾}{💅🏿}{💪🏻}{💪🏼}{💪🏽}{💪🏾}{💪🏿}{🖐🏻}{🖐🏼}{🖐🏽}{🖐🏾}{🖐🏿}{🖕🏻}{🖕🏼}{🖕🏽}{🖕🏾}{🖕🏿}{🖖🏻}{🖖🏼}{🖖🏽}{🖖🏾}{🖖🏿}{🙌🏻}{🙌🏼}{🙌🏽}{🙌🏾}{🙌🏿}{🙏🏻}{🙏🏼}{🙏🏽}{🙏🏾}{🙏🏿}{🚣🏻}{🚣🏼}{🚣🏽}{🚣🏾}{🚣🏿}{🚴🏻}{🚴🏼}{🚴🏽}{🚴🏾}{🚴🏿}{🚵🏻}{🚵🏼}{🚵🏽}{🚵🏾}{🚵🏿}{🚶🏻}{🚶🏼}{🚶🏽}{🚶🏾}{🚶🏿}{🛀🏻}{🛀🏼}{🛀🏽}{🛀🏾}{🛀🏿}{🤘🏻}{🤘🏼}{🤘🏽}{🤘🏾}{🤘🏿}]").freeze();
    static final UnicodeSet MODIFIERS = new UnicodeSet(0x1F3FB,0x1F3FF).freeze();
    static final UnicodeSet REGIONALS = new UnicodeSet(0x1F1E6,0x1F1FF).freeze();

    public static final UnicodeSet TAKES_EMOJI_VS = new UnicodeSet("[©®‼⁉™↔-↙↩↪⌚⌛Ⓜ▪▫▶◀◻-◾☀☁☎☑☔☕☝☺♈-♓♠♣♥♦♨♻♿⚓⚠⚡⚪⚫⚽⚾⛄⛅⛔⛪⛲⛳⛵⛺⛽✂✈✉✌✏✒✔✖✳✴❄❇❗❤➡⤴⤵⬅-⬇⬛⬜⭐⭕〰〽㊗㊙🀄🅰🅱🅾🅿🈂🈚🈯🈷]").freeze();

    public static final StringComparator CODEPOINT_ORDER = new UTF16.StringComparator(true, false,0);
    public static final RuleBasedCollator RAW_COLLATOR = (RuleBasedCollator) Collator.getInstance(new ULocale("en-u-co-emoji"));
    static {
        RAW_COLLATOR.setNumericCollation(true);
        RAW_COLLATOR.setCaseLevel(true);
        RAW_COLLATOR.freeze();
    }
    public static final Comparator<String> MAIN_COLLATOR = new MultiComparator(RAW_COLLATOR, CODEPOINT_ORDER);

    public static String addEmojiVariation(String s) {
        StringBuilder b = new StringBuilder();
        for (int cp : CharSequences.codePoints(s)) {
            b.appendCodePoint(cp);
            if (TAKES_EMOJI_VS.contains(cp)) {
                b.append('\uFE0F');
            }
        }
        return b.toString();
    }
    
    private static UnicodeSet OK_AT_END = new UnicodeSet("[ \\]\t]").freeze();
    private static Pattern UPLUS = Pattern.compile("U\\+(1?[A-Za-z0-9]{3,5})");
    private static Pattern DOTDOT = Pattern.compile("\\.\\.");

    public static UnicodeSet parseUnicodeSet(String input) {
        input = UPLUS.matcher(input).replaceAll("\\\\x{$1}");
        input = DOTDOT.matcher(input).replaceAll("-");

//        setA = setA.replace("..U+", "-\\u");
//        setA = setA.replace("U+", "\\u");

        input = input.trim() + "]]]]]";
        String parseInput = "[" + input + "]]]]]";
        ParsePosition parsePosition = new ParsePosition(0);
        UnicodeSet result = new UnicodeSet(parseInput, parsePosition, fullSymbolTable);
        int parseEnd = parsePosition.getIndex();
        if (parseEnd != parseInput.length() && !UnicodeSetUtilities.OK_AT_END.containsAll(parseInput.substring(parseEnd))) {
            parseEnd--; // get input offset
            throw new IllegalArgumentException("Additional characters past the end of the set, at " 
                    + parseEnd + ", ..." 
                    + input.substring(Math.max(0, parseEnd - 10), parseEnd)
                    + "|"
                    + input.substring(parseEnd, Math.min(input.length(), parseEnd + 10))
                    );
        }
        return result;
    }


    static UnicodeSet.XSymbolTable fullSymbolTable = new MySymbolTable();

    private static class MySymbolTable extends UnicodeSet.XSymbolTable {
        UnicodeRegex unicodeRegex;
        XPropertyFactory factory;

        public MySymbolTable() {
            unicodeRegex = new UnicodeRegex().setSymbolTable(this);
        }


        //    public boolean applyPropertyAlias0(String propertyName,
        //            String propertyValue, UnicodeSet result) {
        //      if (!propertyName.contains("*")) {
        //        return applyPropertyAlias(propertyName, propertyValue, result);
        //      }
        //      String[] propertyNames = propertyName.split("[*]");
        //      for (int i = propertyNames.length - 1; i >= 0; ++i) {
        //        String pname = propertyNames[i];
        //        
        //      }
        //      return null;
        //    }

        public boolean applyPropertyAlias(String propertyName,
                String propertyValue, UnicodeSet result) {
            boolean status = false;
            boolean invert = false;
            int posNotEqual = propertyName.indexOf('\u2260');
            int posColon = propertyName.indexOf(':');
            if (posNotEqual >= 0 || posColon >= 0) {
                if (posNotEqual < 0) posNotEqual = propertyName.length();
                if (posColon < 0) posColon = propertyName.length();
                int opPos = posNotEqual < posColon ? posNotEqual : posColon;
                propertyValue = propertyValue.length() == 0 ? propertyName.substring(opPos+1) 
                        : propertyName.substring(opPos+1) + "=" + propertyValue;
                propertyName = propertyName.substring(0,opPos);
                if (posNotEqual < posColon) {
                    invert = true;
                }
            }
            if (propertyName.endsWith("!")) {
                propertyName = propertyName.substring(0, propertyName.length() - 1);
                invert = !invert;
            }
            propertyValue = propertyValue.trim();
            if (propertyValue.length() != 0) {
                status = applyPropertyAlias0(propertyName, propertyValue, result, invert);
            } else {
                try {
                    status = applyPropertyAlias0("gc", propertyName, result, invert);
                } catch (Exception e) {};
                if (!status) {
                    try {
                        status = applyPropertyAlias0("sc", propertyName, result, invert);
                    } catch (Exception e) {};
                    if (!status) {
                        try {
                            status = applyPropertyAlias0(propertyName, "No", result, !invert);
                        } catch (Exception e) {};
                        if (!status) {
                            status = applyPropertyAlias0(propertyName, "", result, invert);
                        }
                    }
                }
            }
            return status;
        }


        public boolean applyPropertyAlias0(String propertyName,
                String propertyValue, UnicodeSet result, boolean invert) {
            result.clear();
            PatternMatcher patternMatcher = null;
            if (propertyValue.length() > 1 && propertyValue.startsWith("/") && propertyValue.endsWith("/")) {
                String fixedRegex = unicodeRegex.transform(propertyValue.substring(1, propertyValue.length() - 1));
                patternMatcher = new UnicodeProperty.RegexMatcher().set(fixedRegex);
            }
            if (factory == null) {
                factory = XPropertyFactory.make();
            }
            UnicodeProperty otherProperty = null;
            boolean testCp = false;
            if (propertyValue.length() > 1 && propertyValue.startsWith("@") && propertyValue.endsWith("@")) {
                String otherPropName = propertyValue.substring(1, propertyValue.length() - 1).trim();
                if ("cp".equalsIgnoreCase(otherPropName)) {
                    testCp = true;
                } else {
                    otherProperty = factory.getProperty(otherPropName);
                }
            }
            boolean isAge = UnicodeProperty.equalNames("age", propertyName);
            UnicodeProperty prop = factory.getProperty(propertyName);
            if (prop != null) {
                UnicodeSet set;
                if (testCp) {
                    set = new UnicodeSet();
                    for (int i = 0; i <= 0x10FFFF; ++i) {
                        if (invert != UnicodeProperty.equals(i, prop.getValue(i))) {
                            set.add(i);
                        }
                    }
                } else if (otherProperty != null) {
                    set = new UnicodeSet();
                    for (int i = 0; i <= 0x10FFFF; ++i) {
                        String v1 = prop.getValue(i);
                        String v2 = otherProperty.getValue(i);
                        if (invert != UnicodeProperty.equals(v1, v2)) {
                            set.add(i);
                        }
                    }
                } else if (patternMatcher == null) {
                    if (!isValid(prop, propertyValue)) {
                        throw new IllegalArgumentException("The value '" + propertyValue + "' is illegal. Values for " + propertyName
                                + " must be in "
                                + prop.getAvailableValues() + " or in " + prop.getValueAliases());
                    }
                    if (isAge) {
                        set = prop.getSet(new ComparisonMatcher(propertyValue, Relation.geq));
                    } else {
                        set = prop.getSet(propertyValue);
                    }
                } else if (isAge) {
                    set = new UnicodeSet();
                    List<String> values = prop.getAvailableValues();
                    for (String value : values) {
                        if (patternMatcher.matches(value)) {
                            for (String other : values) {
                                if (other.compareTo(value) <= 0) {
                                    set.addAll(prop.getSet(other));
                                }
                            }
                        }
                    }
                } else {
                    set = prop.getSet(patternMatcher);
                }
                if (invert) {
                    if (isAge) {
                        set.complement();
                    } else {
                        set = prop.getUnicodeMap().keySet().removeAll(set);
                    }
                }
                result.addAll(set);
                return true;
            }
            throw new IllegalArgumentException("Illegal property: " + propertyName);
        }



        private boolean isValid(UnicodeProperty prop, String propertyValue) {
            //      if (prop.getName().equals("General_Category")) {
            //        if (propertyValue)
            //      }
            return prop.isValidValue(propertyValue);
        }

    };

    public static class ComparisonMatcher implements PatternMatcher {
        Relation relation;
        enum Relation {less, leq, equal, geq, greater}
        static Comparator comparator = new UTF16.StringComparator(true, false,0);

        String pattern;

        public ComparisonMatcher(String pattern, Relation comparator) {
            this.relation = comparator;
            this.pattern = pattern;
        }

        public boolean matches(Object value) {
            int comp = comparator.compare(pattern, value.toString());
            switch (relation) {
            case less: return comp < 0;
            case leq: return comp <= 0;
            default: return comp == 0;
            case geq: return comp >= 0;
            case greater: return comp > 0;
            }
        }

        public PatternMatcher set(String pattern) {
            this.pattern = pattern;
            return this;
        }
    }



}
