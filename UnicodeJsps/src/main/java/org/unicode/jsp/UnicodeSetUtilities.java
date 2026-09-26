package org.unicode.jsp;

import com.ibm.icu.impl.UnicodeRegex;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UTF16.StringComparator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;
import java.text.ParsePosition;
import java.util.Comparator;
import java.util.regex.Pattern;
import org.unicode.cldr.util.MultiComparator;
import org.unicode.text.UCD.VersionedSymbolTable;

public class UnicodeSetUtilities {

    public static final UnicodeSet EMOJI = new UnicodeSet("[:emoji:]");
    public static final UnicodeSet Emoji_Presentation = new UnicodeSet("[:Emoji_Presentation:]");
    public static final UnicodeSet Emoji_Modifier = new UnicodeSet("[:Emoji_Modifier:]");
    public static final UnicodeSet Emoji_Modifier_Base = new UnicodeSet("[:Emoji_Modifier_Base:]");

    public static final UnicodeSet SINGLETONS =
            new UnicodeSet(
                            "[©®‼⁉™ℹ↔-↙↩↪⌚⌛⌨⏏⏩-⏳⏸-⏺Ⓜ▪▫▶◀◻-◾☀-☄☎☑☔☕☘☝☠☢☣☦☪☮☯☸-☺♈-♓♠♣♥♦♨♻♿⚒-⚔⚖⚗⚙⚛⚜⚠⚡"
                                    + "⚪⚫⚰⚱⚽⚾⛄⛅⛈⛎⛏⛑⛓⛔⛩⛪⛰-⛵⛷-⛺⛽✂✅✈-✍✏✒✔✖✝✡✨✳✴❄❇❌❎❓-❕❗❣❤➕-➗➡➰➿⤴⤵⬅-⬇⬛⬜⭐⭕〰〽㊗㊙🀄🃏🅰🅱🅾🅿🆎🆑-🆚🈁🈂🈚🈯🈲-🈺"
                                    + "🉐🉑🌀-🌡🌤-🎓🎖🎗🎙-🎛🎞-🏰🏳-🏵🏷-📽📿-🔽🕉-🕎🕐-🕧🕯🕰🕳-🕹🖇🖊-🖍🖐🖕🖖🖥🖨🖱🖲🖼🗂-🗄🗑-🗓🗜-🗞🗡🗣🗯🗳🗺-🙏🚀-🛅🛋-🛐🛠-🛥🛩🛫🛬🛰🛳🤐-🤘🦀-🦄🧀]")
                    .freeze();
    public static final UnicodeSet KEYCAPS =
            new UnicodeSet("[{#⃣}{*⃣}{0⃣}{1⃣}{2⃣}{3⃣}{4⃣}{5⃣}{6⃣}{7⃣}{8⃣}{9⃣}]").freeze();
    public static final UnicodeSet FLAGS =
            new UnicodeSet(
                            "[{🇦🇨}"
                                    + "{🇦🇩}{🇦🇪}{🇦🇫}{🇦🇬}{🇦🇮}{🇦🇱}{🇦🇲}{🇦🇴}{🇦🇶}{🇦🇷}{🇦🇸}{🇦🇹}{🇦🇺}{🇦🇼}{🇦🇽}{🇦🇿}{🇧🇦}{🇧🇧}{🇧🇩}{🇧🇪}{🇧🇫}{🇧🇬}{🇧🇭}{🇧🇮}{🇧🇯}{🇧🇱}{🇧🇲}{🇧🇳}{🇧🇴}{🇧🇶}{🇧🇷}{🇧🇸}"
                                    + "{🇧🇹}{🇧🇻}{🇧🇼}{🇧🇾}{🇧🇿}{🇨🇦}{🇨🇨}{🇨🇩}{🇨🇫}{🇨🇬}{🇨🇭}{🇨🇮}{🇨🇰}{🇨🇱}{🇨🇲}{🇨🇳}{🇨🇴}{🇨🇵}{🇨🇷}{🇨🇺}{🇨🇻}{🇨🇼}{🇨🇽}{🇨🇾}{🇨🇿}{🇩🇪}{🇩🇬}{🇩🇯}{🇩🇰}{🇩🇲}{🇩🇴}"
                                    + "{🇩🇿}{🇪🇦}{🇪🇨}{🇪🇪}{🇪🇬}{🇪🇭}{🇪🇷}{🇪🇸}{🇪🇹}{🇪🇺}{🇫🇮}{🇫🇯}{🇫🇰}{🇫🇲}{🇫🇴}{🇫🇷}{🇬🇦}{🇬🇧}{🇬🇩}{🇬🇪}{🇬🇫}{🇬🇬}{🇬🇭}{🇬🇮}{🇬🇱}{🇬🇲}{🇬🇳}{🇬🇵}{🇬🇶}{🇬🇷}"
                                    + "{🇬🇸}{🇬🇹}{🇬🇺}{🇬🇼}{🇬🇾}{🇭🇰}{🇭🇲}{🇭🇳}{🇭🇷}{🇭🇹}{🇭🇺}{🇮🇨}{🇮🇩}{🇮🇪}{🇮🇱}{🇮🇲}{🇮🇳}{🇮🇴}{🇮🇶}{🇮🇷}{🇮🇸}{🇮🇹}{🇯🇪}{🇯🇲}{🇯🇴}{🇯🇵}{🇰🇪}{🇰🇬}{🇰🇭}{🇰🇮}{🇰🇲}"
                                    + "{🇰🇳}{🇰🇵}{🇰🇷}{🇰🇼}{🇰🇾}{🇰🇿}{🇱🇦}{🇱🇧}{🇱🇨}{🇱🇮}{🇱🇰}{🇱🇷}{🇱🇸}{🇱🇹}{🇱🇺}{🇱🇻}{🇱🇾}{🇲🇦}{🇲🇨}{🇲🇩}{🇲🇪}{🇲🇫}{🇲🇬}{🇲🇭}{🇲🇰}{🇲🇱}{🇲🇲}{🇲🇳}{🇲🇴}{🇲🇵}{🇲🇶}{🇲🇷}{🇲🇸}"
                                    + "{🇲🇹}{🇲🇺}{🇲🇻}{🇲🇼}{🇲🇽}{🇲🇾}{🇲🇿}{🇳🇦}{🇳🇨}{🇳🇪}{🇳🇫}{🇳🇬}{🇳🇮}{🇳🇱}{🇳🇴}{🇳🇵}{🇳🇷}{🇳🇺}{🇳🇿}{🇴🇲}{🇵🇦}{🇵🇪}{🇵🇫}{🇵🇬}{🇵🇭}{🇵🇰}{🇵🇱}{🇵🇲}{🇵🇳}{🇵🇷}{🇵🇸}"
                                    + "{🇵🇹}{🇵🇼}{🇵🇾}{🇶🇦}{🇷🇪}{🇷🇴}{🇷🇸}{🇷🇺}{🇷🇼}{🇸🇦}{🇸🇧}{🇸🇨}{🇸🇩}{🇸🇪}{🇸🇬}{🇸🇭}{🇸🇮}{🇸🇯}{🇸🇰}{🇸🇱}{🇸🇲}{🇸🇳}{🇸🇴}{🇸🇷}{🇸🇸}{🇸🇹}{🇸🇻}{🇸🇽}{🇸🇾}{🇸🇿}{🇹🇦}{🇹🇨}"
                                    + "{🇹🇩}{🇹🇫}{🇹🇬}{🇹🇭}{🇹🇯}{🇹🇰}{🇹🇱}{🇹🇲}{🇹🇳}{🇹🇴}{🇹🇷}{🇹🇹}{🇹🇻}{🇹🇼}{🇹🇿}{🇺🇦}{🇺🇬}{🇺🇲}{🇺🇸}{🇺🇾}{🇺🇿}{🇻🇦}{🇻🇨}{🇻🇪}{🇻🇬}{🇻🇮}{🇻🇳}{🇻🇺}{🇼🇫}"
                                    + "{🇼🇸}{🇽🇰}{🇾🇪}{🇾🇹}{🇿🇦}{🇿🇲}{🇿🇼}]")
                    .freeze();
    public static final UnicodeSet GROUPS =
            new UnicodeSet(
                            "[💏 💑 👪 {👨‍❤️‍👨}{👨‍❤️‍💋‍👨}{👨‍👨‍👦}{👨‍👨‍👦‍👦}{👨‍👨‍👧}{👨‍👨‍👧‍👦}{👨‍👨‍👧‍👧}{👨‍👩‍👦}{👨‍👩‍👦‍👦}{👨‍👩‍👧}{👨‍👩‍👧‍👦}{👨‍👩‍👧‍👧}{👩‍❤️‍👩}{👩‍❤️‍💋‍👩}{👩‍👩‍👦}{👩‍👩‍👦‍👦}{👩‍👩‍👧}{👩‍👩‍👧‍👦}{👩‍👩‍👧‍👧}]")
                    .freeze();
    public static final UnicodeSet PRIMARY =
            new UnicodeSet(
                            "[🎅👦-👩👮👰-👸👼💁💂💆💇🙅-🙇🙋🙍🙎"
                                    + "{🎅🏻}{🎅🏼}{🎅🏽}{🎅🏾}{🎅🏿}{👦🏻}{👦🏼}{👦🏽}{👦🏾}{👦🏿}{👧🏻}{👧🏼}{👧🏽}{👧🏾}{👧🏿}{👨🏻}{👨🏼}{👨🏽}{👨🏾}{👨🏿}{👩🏻}{👩🏼}{👩🏽}{👩🏾}{👩🏿}{👮🏻}{👮🏼}{👮🏽}{👮🏾}{👮🏿}{👰🏻}{👰🏼}{👰🏽}{👰🏾}{👰🏿}{👱🏻}{👱🏼}{👱🏽}{👱🏾}{👱🏿}{👲🏻}{👲🏼}{👲🏽}{👲🏾}{👲🏿}{👳🏻}{👳🏼}{👳🏽}{👳🏾}{👳🏿}{👴🏻}{👴🏼}{👴🏽}{👴🏾}{👴🏿}{👵🏻}{👵🏼}{👵🏽}{👵🏾}{👵🏿}{👶🏻}{👶🏼}{👶🏽}{👶🏾}{👶🏿}{👷🏻}{👷🏼}{👷🏽}{👷🏾}{👷🏿}{👸🏻}{👸🏼}{👸🏽}{👸🏾}{👸🏿}{👼🏻}{👼🏼}{👼🏽}{👼🏾}{👼🏿}{💁🏻}{💁🏼}{💁🏽}{💁🏾}{💁🏿}{💂🏻}{💂🏼}{💂🏽}{💂🏾}{💂🏿}{💆🏻}{💆🏼}{💆🏽}{💆🏾}{💆🏿}{💇🏻}{💇🏼}{💇🏽}{💇🏾}{💇🏿}{🙅🏻}{🙅🏼}{🙅🏽}{🙅🏾}{🙅🏿}{🙆🏻}{🙆🏼}{🙆🏽}{🙆🏾}{🙆🏿}{🙇🏻}{🙇🏼}{🙇🏽}{🙇🏾}{🙇🏿}{🙋🏻}{🙋🏼}{🙋🏽}{🙋🏾}{🙋🏿}{🙍🏻}{🙍🏼}{🙍🏽}{🙍🏾}{🙍🏿}{🙎🏻}{🙎🏼}{🙎🏽}{🙎🏾}{🙎🏿}]")
                    .freeze();
    public static final UnicodeSet FACE =
            new UnicodeSet("[☺ ☹ 🙁 🙂 😀-😆 😉-😷 😇 😈 👿 🙃 🙄 🤐-🤕 🤗]").freeze();
    // +
    // "{☹🏻}{☹🏼}{☹🏽}{☹🏾}{☹🏿}{☺🏻}{☺🏼}{☺🏽}{☺🏾}{☺🏿}{👿🏻}{👿🏼}{👿🏽}{👿🏾}{👿🏿}{😀🏻}{😀🏼}{😀🏽}{😀🏾}{😀🏿}{😁🏻}{😁🏼}{😁🏽}{😁🏾}{😁🏿}{😂🏻}{😂🏼}{😂🏽}{😂🏾}{😂🏿}{😃🏻}{😃🏼}{😃🏽}{😃🏾}{😃🏿}{😄🏻}{😄🏼}{😄🏽}{😄🏾}{😄🏿}{😅🏻}{😅🏼}{😅🏽}{😅🏾}{😅🏿}{😆🏻}{😆🏼}{😆🏽}{😆🏾}{😆🏿}{😇🏻}{😇🏼}{😇🏽}{😇🏾}{😇🏿}{😈🏻}{😈🏼}{😈🏽}{😈🏾}{😈🏿}{😉🏻}{😉🏼}{😉🏽}{😉🏾}{😉🏿}{😊🏻}{😊🏼}{😊🏽}{😊🏾}{😊🏿}{😋🏻}{😋🏼}{😋🏽}{😋🏾}{😋🏿}{😌🏻}{😌🏼}{😌🏽}{😌🏾}{😌🏿}{😍🏻}{😍🏼}{😍🏽}{😍🏾}{😍🏿}{😎🏻}{😎🏼}{😎🏽}{😎🏾}{😎🏿}{😏🏻}{😏🏼}{😏🏽}{😏🏾}{😏🏿}{😐🏻}{😐🏼}{😐🏽}{😐🏾}{😐🏿}{😑🏻}{😑🏼}{😑🏽}{😑🏾}{😑🏿}{😒🏻}{😒🏼}{😒🏽}{😒🏾}{😒🏿}{😓🏻}{😓🏼}{😓🏽}{😓🏾}{😓🏿}{😔🏻}{😔🏼}{😔🏽}{😔🏾}{😔🏿}{😕🏻}{😕🏼}{😕🏽}{😕🏾}{😕🏿}{😖🏻}{😖🏼}{😖🏽}{😖🏾}{😖🏿}{😗🏻}{😗🏼}{😗🏽}{😗🏾}{😗🏿}{😘🏻}{😘🏼}{😘🏽}{😘🏾}{😘🏿}{😙🏻}{😙🏼}{😙🏽}{😙🏾}{😙🏿}{😚🏻}{😚🏼}{😚🏽}{😚🏾}{😚🏿}{😛🏻}{😛🏼}{😛🏽}{😛🏾}{😛🏿}{😜🏻}{😜🏼}{😜🏽}{😜🏾}{😜🏿}{😝🏻}{😝🏼}{😝🏽}{😝🏾}{😝🏿}{😞🏻}{😞🏼}{😞🏽}{😞🏾}{😞🏿}{😟🏻}{😟🏼}{😟🏽}{😟🏾}{😟🏿}{😠🏻}{😠🏼}{😠🏽}{😠🏾}{😠🏿}{😡🏻}{😡🏼}{😡🏽}{😡🏾}{😡🏿}{😢🏻}{😢🏼}{😢🏽}{😢🏾}{😢🏿}{😣🏻}{😣🏼}{😣🏽}{😣🏾}{😣🏿}{😤🏻}{😤🏼}{😤🏽}{😤🏾}{😤🏿}{😥🏻}{😥🏼}{😥🏽}{😥🏾}{😥🏿}{😦🏻}{😦🏼}{😦🏽}{😦🏾}{😦🏿}{😧🏻}{😧🏼}{😧🏽}{😧🏾}{😧🏿}{😨🏻}{😨🏼}{😨🏽}{😨🏾}{😨🏿}{😩🏻}{😩🏼}{😩🏽}{😩🏾}{😩🏿}{😪🏻}{😪🏼}{😪🏽}{😪🏾}{😪🏿}{😫🏻}{😫🏼}{😫🏽}{😫🏾}{😫🏿}{😬🏻}{😬🏼}{😬🏽}{😬🏾}{😬🏿}{😭🏻}{😭🏼}{😭🏽}{😭🏾}{😭🏿}{😮🏻}{😮🏼}{😮🏽}{😮🏾}{😮🏿}{😯🏻}{😯🏼}{😯🏽}{😯🏾}{😯🏿}{😰🏻}{😰🏼}{😰🏽}{😰🏾}{😰🏿}{😱🏻}{😱🏼}{😱🏽}{😱🏾}{😱🏿}{😲🏻}{😲🏼}{😲🏽}{😲🏾}{😲🏿}{😳🏻}{😳🏼}{😳🏽}{😳🏾}{😳🏿}{😴🏻}{😴🏼}{😴🏽}{😴🏾}{😴🏿}{😵🏻}{😵🏼}{😵🏽}{😵🏾}{😵🏿}{😶🏻}{😶🏼}{😶🏽}{😶🏾}{😶🏿}{😷🏻}{😷🏼}{😷🏽}{😷🏾}{😷🏿}{🙁🏻}{🙁🏼}{🙁🏽}{🙁🏾}{🙁🏿}{🙂🏻}{🙂🏼}{🙂🏽}{🙂🏾}{🙂🏿}{🙃🏻}{🙃🏼}{🙃🏽}{🙃🏾}{🙃🏿}{🙄🏻}{🙄🏼}{🙄🏽}{🙄🏾}{🙄🏿}{🤐🏻}{🤐🏼}{🤐🏽}{🤐🏾}{🤐🏿}{🤑🏻}{🤑🏼}{🤑🏽}{🤑🏾}{🤑🏿}{🤒🏻}{🤒🏼}{🤒🏽}{🤒🏾}{🤒🏿}{🤓🏻}{🤓🏼}{🤓🏽}{🤓🏾}{🤓🏿}{🤔🏻}{🤔🏼}{🤔🏽}{🤔🏾}{🤔🏿}{🤕🏻}{🤕🏼}{🤕🏽}{🤕🏾}{🤕🏿}{🤗🏻}{🤗🏼}{🤗🏽}{🤗🏾}{🤗🏿}]").freeze();
    public static final UnicodeSet SECONDARY =
            new UnicodeSet(
                            "[☝✊-✍🏂-🏄🏇🏊👂👃👆-👐💃💅💪🖐🖕 🖖🙌🙏🚣🚴-🚶🛀🤘"
                                    + "{☝🏻}{☝🏼}{☝🏽}{☝🏾}{☝🏿}{✊🏻}{✊🏼}{✊🏽}{✊🏾}{✊🏿}{✋🏻}{✋🏼}{✋🏽}{✋🏾}{✋🏿}{✌🏻}{✌🏼}{✌🏽}{✌🏾}{✌🏿}{✍🏻}{✍🏼}{✍🏽}{✍🏾}{✍🏿}{🏂🏻}{🏂🏼}{🏂🏽}{🏂🏾}{🏂🏿}{🏃🏻}{🏃🏼}{🏃🏽}{🏃🏾}{🏃🏿}{🏄🏻}{🏄🏼}{🏄🏽}{🏄🏾}{🏄🏿}{🏇🏻}{🏇🏼}{🏇🏽}{🏇🏾}{🏇🏿}{🏊🏻}{🏊🏼}{🏊🏽}{🏊🏾}{🏊🏿}{👂🏻}{👂🏼}{👂🏽}{👂🏾}{👂🏿}{👃🏻}{👃🏼}{👃🏽}{👃🏾}{👃🏿}{👆🏻}{👆🏼}{👆🏽}{👆🏾}{👆🏿}{👇🏻}{👇🏼}{👇🏽}{👇🏾}{👇🏿}{👈🏻}{👈🏼}{👈🏽}{👈🏾}{👈🏿}{👉🏻}{👉🏼}{👉🏽}{👉🏾}{👉🏿}{👊🏻}{👊🏼}{👊🏽}{👊🏾}{👊🏿}{👋🏻}{👋🏼}{👋🏽}{👋🏾}{👋🏿}{👌🏻}{👌🏼}{👌🏽}{👌🏾}{👌🏿}{👍🏻}{👍🏼}{👍🏽}{👍🏾}{👍🏿}{👎🏻}{👎🏼}{👎🏽}{👎🏾}{👎🏿}{👏🏻}{👏🏼}{👏🏽}{👏🏾}{👏🏿}{👐🏻}{👐🏼}{👐🏽}{👐🏾}{👐🏿}{💃🏻}{💃🏼}{💃🏽}{💃🏾}{💃🏿}{💅🏻}{💅🏼}{💅🏽}{💅🏾}{💅🏿}{💪🏻}{💪🏼}{💪🏽}{💪🏾}{💪🏿}{🖐🏻}{🖐🏼}{🖐🏽}{🖐🏾}{🖐🏿}{🖕🏻}{🖕🏼}{🖕🏽}{🖕🏾}{🖕🏿}{🖖🏻}{🖖🏼}{🖖🏽}{🖖🏾}{🖖🏿}{🙌🏻}{🙌🏼}{🙌🏽}{🙌🏾}{🙌🏿}{🙏🏻}{🙏🏼}{🙏🏽}{🙏🏾}{🙏🏿}{🚣🏻}{🚣🏼}{🚣🏽}{🚣🏾}{🚣🏿}{🚴🏻}{🚴🏼}{🚴🏽}{🚴🏾}{🚴🏿}{🚵🏻}{🚵🏼}{🚵🏽}{🚵🏾}{🚵🏿}{🚶🏻}{🚶🏼}{🚶🏽}{🚶🏾}{🚶🏿}{🛀🏻}{🛀🏼}{🛀🏽}{🛀🏾}{🛀🏿}{🤘🏻}{🤘🏼}{🤘🏽}{🤘🏾}{🤘🏿}]")
                    .freeze();
    static final UnicodeSet MODIFIERS = new UnicodeSet(0x1F3FB, 0x1F3FF).freeze();
    static final UnicodeSet REGIONALS = new UnicodeSet(0x1F1E6, 0x1F1FF).freeze();

    public static final UnicodeSet TAKES_EMOJI_VS =
            new UnicodeSet(
                            "[©®‼⁉™↔-↙↩↪⌚⌛Ⓜ▪▫▶◀◻-◾☀☁☎☑☔☕☝☺♈-♓♠♣♥♦♨♻♿⚓⚠⚡⚪⚫⚽⚾⛄⛅⛔⛪⛲⛳⛵⛺⛽✂✈✉✌✏✒✔✖✳✴❄❇❗❤➡⤴⤵⬅-⬇⬛⬜⭐⭕〰〽㊗㊙🀄🅰🅱🅾🅿🈂🈚🈯🈷]")
                    .freeze();

    public static final StringComparator CODEPOINT_ORDER =
            new UTF16.StringComparator(true, false, 0);
    public static final RuleBasedCollator RAW_COLLATOR =
            (RuleBasedCollator) Collator.getInstance(new ULocale("en-u-co-emoji"));

    static {
        RAW_COLLATOR.setNumericCollation(true);
        RAW_COLLATOR.setCaseLevel(true);
        RAW_COLLATOR.freeze();
    }

    public static final Comparator<String> MAIN_COLLATOR =
            new MultiComparator(RAW_COLLATOR, CODEPOINT_ORDER);

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

    private static VersionedSymbolTable getSymbolTable() {
        return VersionedSymbolTable.forReview(UcdLoader::getOldestLoadedUcd)
                .setUnversionedExtensions(XPropertyFactory.make());
    }

    /**
     * Returns a new converter using the same property data and version policy as parseUnicodeSet.
     * Use its transform method; ICU's static fix and compile methods do not use this symbol table.
     */
    public static UnicodeRegex getUnicodeRegex() {
        return new UnicodeRegex().setSymbolTable(getSymbolTable());
    }

    public static UnicodeSet parseUnicodeSet(String input) {
        input = UPLUS.matcher(input).replaceAll("\\\\x{$1}");
        input = DOTDOT.matcher(input).replaceAll("-");

        //        setA = setA.replace("..U+", "-\\u");
        //        setA = setA.replace("U+", "\\u");

        input = input.trim() + "]]]]]";
        String parseInput = "[" + input + "]]]]]";
        ParsePosition parsePosition = new ParsePosition(0);
        UnicodeSet result = new UnicodeSet(parseInput, parsePosition, getSymbolTable());
        int parseEnd = parsePosition.getIndex();
        if (parseEnd != parseInput.length()
                && !UnicodeSetUtilities.OK_AT_END.containsAll(parseInput.substring(parseEnd))) {
            parseEnd--; // get input offset
            throw new IllegalArgumentException(
                    "Additional characters past the end of the set, at "
                            + parseEnd
                            + ", ..."
                            + input.substring(Math.max(0, parseEnd - 10), parseEnd)
                            + "|"
                            + input.substring(parseEnd, Math.min(input.length(), parseEnd + 10)));
        }
        return result;
    }
}
