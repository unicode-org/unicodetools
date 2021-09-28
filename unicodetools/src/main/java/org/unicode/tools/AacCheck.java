package org.unicode.tools;

import org.unicode.tools.emoji.Emoji;
import org.unicode.tools.emoji.EmojiData;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.EntryRange;
import com.ibm.icu.util.Output;

public class AacCheck {
    // Error messages
    public static final int OK = 0;
    public static final int TOO_FEW_CODEPOINTS = 1;
    public static final int NOT_REGISTRATABLE = 8;
    public static final char ZWJ = '\u200D';

    /**
     * input is a list of space-delimited lists of hex values, such as:<br>
     * AacCheck 61<br>
     * AacCheck "1F468 200D 2764" 200D 1F468 
     * 
     * @param args
     * @return error code, after printing message
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            EmojiData.EMOJI_DATA.getName("👨");
            for (EntryRange range : ALLOWED.ranges()) {
                if (range.codepoint == range.codepointEnd) {
                    System.out.println(Utility.hex(range.codepoint) 
                            + " ; " + UCharacter.getName(range.codepoint));
                } else {
                    System.out.println(Utility.hex(range.codepoint) + ".." + Utility.hex(range.codepointEnd) 
                            + " ; " + UCharacter.getName(range.codepoint) + ".." + UCharacter.getName(range.codepointEnd));
                }
            }
            for (String cps: ALLOWED.strings()) {
                System.out.println(Utility.hex(cps) 
                        + " ; " + EmojiData.EMOJI_DATA.getName(cps));
            }
        } else {
            Output<String> message = new Output<>();
            int value = process(message, args);
            System.out.println(message);
            System.exit(value);
        }
    }

    public static int process(Output<String> messageOut, String... input) {
        StringBuilder filtered = new StringBuilder();
        StringBuilder unfiltered = new StringBuilder();
        int filteredCount = 0;

        // process the input to filter out certain code points.
        // we capture both the filtered and unfiltered strings

        for (String inputItem : input) {
            String[] args = inputItem.split("\\s+"); // split at spaces
            for (String arg : args) {
                if (arg.length() == 0) {
                    messageOut.value = ";Error: need 1 argument";
                    return TOO_FEW_CODEPOINTS;
                }
                int cp = -1;
                try {
                    cp = Integer.parseInt(arg, 16);            
                } catch (Exception e) {} // fall through with -1

                if (cp < 0 || cp > 0x10FFFF) {
                    messageOut.value = ";Error: bad codepoint: " + arg;
                    return NOT_REGISTRATABLE;
                } else if (!IGNORE.contains(cp)) {
                    filtered.appendCodePoint(cp);
                    filteredCount++;
                }
                unfiltered.appendCodePoint(cp);
            }
        }
        // now strip trailing VS characters
        // we can work with 16-bit chars because there is no overlap
        for (int i = filtered.length()-1; i > 0; --i) {
            if (EMOJI_VARIATION_SELECTORS.contains(filtered.charAt(i))) {
                filtered.setLength(i);
            } else {
                break;
            }
        }

        // Now we do the real checks
        String filteredString = filtered.toString();
        final String reformattedHex = Utility.hex(filteredString, 4, " ");

        // First, whether the string was empty after removing ignored code points
        if (filteredCount == 0) {
            messageOut.value = ";Only ignored code points: <" + Utility.hex(unfiltered) + ">";
            return NOT_REGISTRATABLE;
        }
        // Next, whether there were any disallowed characters or sequences
        if (!ALLOWED.contains(filteredString)) {
            if (filteredCount > 1) {
                messageOut.value = ";Not registerable sequence: <" + reformattedHex + ">";
                return NOT_REGISTRATABLE;
            }
            messageOut.value = ";Not registerable codepoint: <" + reformattedHex + ">";
            return NOT_REGISTRATABLE;
        }

        // success!
        String name = UCharacter.getName(filteredString, " + ");
        messageOut.value = reformattedHex + ";" + name;
        return OK;
    }

    private static final UnicodeSet EMOJI_VARIATION_SELECTORS = new UnicodeSet("[\uFE0F\uFE0E]")
    .freeze();

    private static final UnicodeSet IGNORE = new UnicodeSet("[[:z:][:di:]]")
    .remove(ZWJ)
//    .removeAll(EMOJI_VARIATION_SELECTORS)
    .freeze();

    static final UnicodeSet ALLOWED = new UnicodeSet();
    static { 
        UnicodeSet temp = new UnicodeSet("[^[:c:][:z:][:di:][࿕-࿘ 卍 卐]]")
        .addAll(EmojiData.EMOJI_DATA.getChars());
        // remove the variation selectors
        for (String s : temp) {
            if (s.contains(Emoji.EMOJI_VARIANT_STRING)) {
                s = s.replace(Emoji.EMOJI_VARIANT_STRING, "");
            }
            ALLOWED.add(s);
        }
        ALLOWED.freeze();
    }
    //            "["
    //            // singletons, all but C, Z, DI, and initial exclusions
    //            + "[^[:c:][:z:][:di:][࿕-࿘ 卍 卐]]"
    //            // keycaps
    //            + "[{#⃣}{*⃣}{0⃣}{1⃣}{2⃣}{3⃣}{4⃣}{5⃣}{6⃣}{7⃣}{8⃣}{9⃣}]"
    //            // flags
    //            + "[{🇦🇨}{🇦🇩}{🇦🇪}{🇦🇫}{🇦🇬}{🇦🇮}{🇦🇱}{🇦🇲}{🇦🇴}{🇦🇶}{🇦🇷}{🇦🇸}{🇦🇹}{🇦🇺}{🇦🇼}{🇦🇽}{🇦🇿}{🇧🇦}{🇧🇧}{🇧🇩}{🇧🇪}{🇧🇫}"
    //            + "{🇧🇬}{🇧🇭}{🇧🇮}{🇧🇯}{🇧🇱}{🇧🇲}{🇧🇳}{🇧🇴}{🇧🇶}{🇧🇷}{🇧🇸}{🇧🇹}{🇧🇻}{🇧🇼}{🇧🇾}{🇧🇿}{🇨🇦}{🇨🇨}{🇨🇩}{🇨🇫}{🇨🇬}{🇨🇭}"
    //            + "{🇨🇮}{🇨🇰}{🇨🇱}{🇨🇲}{🇨🇳}{🇨🇴}{🇨🇵}{🇨🇷}{🇨🇺}{🇨🇻}{🇨🇼}{🇨🇽}{🇨🇾}{🇨🇿}{🇩🇪}{🇩🇬}{🇩🇯}{🇩🇰}{🇩🇲}{🇩🇴}{🇩🇿}{🇪🇦}"
    //            + "{🇪🇨}{🇪🇪}{🇪🇬}{🇪🇭}{🇪🇷}{🇪🇸}{🇪🇹}{🇪🇺}{🇫🇮}{🇫🇯}{🇫🇰}{🇫🇲}{🇫🇴}{🇫🇷}{🇬🇦}{🇬🇧}{🇬🇩}{🇬🇪}{🇬🇫}{🇬🇬}{🇬🇭}{🇬🇮}"
    //            + "{🇬🇱}{🇬🇲}{🇬🇳}{🇬🇵}{🇬🇶}{🇬🇷}{🇬🇸}{🇬🇹}{🇬🇺}{🇬🇼}{🇬🇾}{🇭🇰}{🇭🇲}{🇭🇳}{🇭🇷}{🇭🇹}{🇭🇺}{🇮🇨}{🇮🇩}{🇮🇪}{🇮🇱}{🇮🇲}"
    //            + "{🇮🇳}{🇮🇴}{🇮🇶}{🇮🇷}{🇮🇸}{🇮🇹}{🇯🇪}{🇯🇲}{🇯🇴}{🇯🇵}{🇰🇪}{🇰🇬}{🇰🇭}{🇰🇮}{🇰🇲}{🇰🇳}{🇰🇵}{🇰🇷}{🇰🇼}{🇰🇾}{🇰🇿}{🇱🇦}"
    //            + "{🇱🇧}{🇱🇨}{🇱🇮}{🇱🇰}{🇱🇷}{🇱🇸}{🇱🇹}{🇱🇺}{🇱🇻}{🇱🇾}{🇲🇦}{🇲🇨}{🇲🇩}{🇲🇪}{🇲🇫}{🇲🇬}{🇲🇭}{🇲🇰}{🇲🇱}{🇲🇲}{🇲🇳}{🇲🇴}"
    //            + "{🇲🇵}{🇲🇶}{🇲🇷}{🇲🇸}{🇲🇹}{🇲🇺}{🇲🇻}{🇲🇼}{🇲🇽}{🇲🇾}{🇲🇿}{🇳🇦}{🇳🇨}{🇳🇪}{🇳🇫}{🇳🇬}{🇳🇮}{🇳🇱}{🇳🇴}{🇳🇵}{🇳🇷}{🇳🇺}"
    //            + "{🇳🇿}{🇴🇲}{🇵🇦}{🇵🇪}{🇵🇫}{🇵🇬}{🇵🇭}{🇵🇰}{🇵🇱}{🇵🇲}{🇵🇳}{🇵🇷}{🇵🇸}{🇵🇹}{🇵🇼}{🇵🇾}{🇶🇦}{🇷🇪}{🇷🇴}{🇷🇸}{🇷🇺}{🇷🇼}"
    //            + "{🇸🇦}{🇸🇧}{🇸🇨}{🇸🇩}{🇸🇪}{🇸🇬}{🇸🇭}{🇸🇮}{🇸🇯}{🇸🇰}{🇸🇱}{🇸🇲}{🇸🇳}{🇸🇴}{🇸🇷}{🇸🇸}{🇸🇹}{🇸🇻}{🇸🇽}{🇸🇾}{🇸🇿}{🇹🇦}"
    //            + "{🇹🇨}{🇹🇩}{🇹🇫}{🇹🇬}{🇹🇭}{🇹🇯}{🇹🇰}{🇹🇱}{🇹🇲}{🇹🇳}{🇹🇴}{🇹🇷}{🇹🇹}{🇹🇻}{🇹🇼}{🇹🇿}{🇺🇦}{🇺🇬}{🇺🇲}{🇺🇸}{🇺🇾}{🇺🇿}"
    //            + "{🇻🇦}{🇻🇨}{🇻🇪}{🇻🇬}{🇻🇮}{🇻🇳}{🇻🇺}{🇼🇫}{🇼🇸}{🇽🇰}{🇾🇪}{🇾🇹}{🇿🇦}{🇿🇲}{🇿🇼}]"
    //            // modifier sequences
    //            + "[{☝🏻}{☝🏼}{☝🏽}{☝🏾}{☝🏿}{⛹🏻}{⛹🏼}{⛹🏽}{⛹🏾}{⛹🏿}{✊🏻}{✊🏼}{✊🏽}{✊🏾}{✊🏿}{✋🏻}{✋🏼}{✋🏽}{✋🏾}{✋🏿}"
    //            + "{✌🏻}{✌🏼}{✌🏽}{✌🏾}{✌🏿}{✍🏻}{✍🏼}{✍🏽}{✍🏾}{✍🏿}{🎅🏻}{🎅🏼}{🎅🏽}{🎅🏾}{🎅🏿}{🏃🏻}{🏃🏼}{🏃🏽}{🏃🏾}{🏃🏿}"
    //            + "{🏄🏻}{🏄🏼}{🏄🏽}{🏄🏾}{🏄🏿}{🏊🏻}{🏊🏼}{🏊🏽}{🏊🏾}{🏊🏿}{🏋🏻}{🏋🏼}{🏋🏽}{🏋🏾}{🏋🏿}{👂🏻}{👂🏼}{👂🏽}{👂🏾}{👂🏿}"
    //            + "{👃🏻}{👃🏼}{👃🏽}{👃🏾}{👃🏿}{👆🏻}{👆🏼}{👆🏽}{👆🏾}{👆🏿}{👇🏻}{👇🏼}{👇🏽}{👇🏾}{👇🏿}{👈🏻}{👈🏼}{👈🏽}{👈🏾}{👈🏿}"
    //            + "{👉🏻}{👉🏼}{👉🏽}{👉🏾}{👉🏿}{👊🏻}{👊🏼}{👊🏽}{👊🏾}{👊🏿}{👋🏻}{👋🏼}{👋🏽}{👋🏾}{👋🏿}{👌🏻}{👌🏼}{👌🏽}{👌🏾}{👌🏿}"
    //            + "{👍🏻}{👍🏼}{👍🏽}{👍🏾}{👍🏿}{👎🏻}{👎🏼}{👎🏽}{👎🏾}{👎🏿}{👏🏻}{👏🏼}{👏🏽}{👏🏾}{👏🏿}{👐🏻}{👐🏼}{👐🏽}{👐🏾}{👐🏿}"
    //            + "{👦🏻}{👦🏼}{👦🏽}{👦🏾}{👦🏿}{👧🏻}{👧🏼}{👧🏽}{👧🏾}{👧🏿}{👨🏻}{👨🏼}{👨🏽}{👨🏾}{👨🏿}{👩🏻}{👩🏼}{👩🏽}{👩🏾}{👩🏿}"
    //            + "{👮🏻}{👮🏼}{👮🏽}{👮🏾}{👮🏿}{👰🏻}{👰🏼}{👰🏽}{👰🏾}{👰🏿}{👱🏻}{👱🏼}{👱🏽}{👱🏾}{👱🏿}{👲🏻}{👲🏼}{👲🏽}{👲🏾}{👲🏿}"
    //            + "{👳🏻}{👳🏼}{👳🏽}{👳🏾}{👳🏿}{👴🏻}{👴🏼}{👴🏽}{👴🏾}{👴🏿}{👵🏻}{👵🏼}{👵🏽}{👵🏾}{👵🏿}{👶🏻}{👶🏼}{👶🏽}{👶🏾}{👶🏿}"
    //            + "{👷🏻}{👷🏼}{👷🏽}{👷🏾}{👷🏿}{👸🏻}{👸🏼}{👸🏽}{👸🏾}{👸🏿}{👼🏻}{👼🏼}{👼🏽}{👼🏾}{👼🏿}{💁🏻}{💁🏼}{💁🏽}{💁🏾}{💁🏿}"
    //            + "{💂🏻}{💂🏼}{💂🏽}{💂🏾}{💂🏿}{💃🏻}{💃🏼}{💃🏽}{💃🏾}{💃🏿}{💅🏻}{💅🏼}{💅🏽}{💅🏾}{💅🏿}{💆🏻}{💆🏼}{💆🏽}{💆🏾}{💆🏿}"
    //            + "{💇🏻}{💇🏼}{💇🏽}{💇🏾}{💇🏿}{💪🏻}{💪🏼}{💪🏽}{💪🏾}{💪🏿}{🕵🏻}{🕵🏼}{🕵🏽}{🕵🏾}{🕵🏿}{🖐🏻}{🖐🏼}{🖐🏽}{🖐🏾}{🖐🏿}"
    //            + "{🖕🏻}{🖕🏼}{🖕🏽}{🖕🏾}{🖕🏿}{🖖🏻}{🖖🏼}{🖖🏽}{🖖🏾}{🖖🏿}{🙅🏻}{🙅🏼}{🙅🏽}{🙅🏾}{🙅🏿}{🙆🏻}{🙆🏼}{🙆🏽}{🙆🏾}{🙆🏿}"
    //            + "{🙇🏻}{🙇🏼}{🙇🏽}{🙇🏾}{🙇🏿}{🙋🏻}{🙋🏼}{🙋🏽}{🙋🏾}{🙋🏿}{🙌🏻}{🙌🏼}{🙌🏽}{🙌🏾}{🙌🏿}{🙍🏻}{🙍🏼}{🙍🏽}{🙍🏾}{🙍🏿}"
    //            + "{🙎🏻}{🙎🏼}{🙎🏽}{🙎🏾}{🙎🏿}{🙏🏻}{🙏🏼}{🙏🏽}{🙏🏾}{🙏🏿}{🚣🏻}{🚣🏼}{🚣🏽}{🚣🏾}{🚣🏿}{🚴🏻}{🚴🏼}{🚴🏽}{🚴🏾}{🚴🏿}"
    //            + "{🚵🏻}{🚵🏼}{🚵🏽}{🚵🏾}{🚵🏿}{🚶🏻}{🚶🏼}{🚶🏽}{🚶🏾}{🚶🏿}{🛀🏻}{🛀🏼}{🛀🏽}{🛀🏾}{🛀🏿}{🤘🏻}{🤘🏼}{🤘🏽}{🤘🏾}{🤘🏿}]"
    //            // zwj sequences
    //            + "[{👁‍🗨}{👨‍❤️‍👨}{👨‍❤️‍💋‍👨}{👨‍👨‍👦}{👨‍👨‍👦‍👦}{👨‍👨‍👧}{👨‍👨‍👧‍👦}{👨‍👨‍👧‍👧}{👨‍👩‍👦}{👨‍👩‍👦‍👦}{👨‍👩‍👧}{👨‍👩‍👧‍👦}{👨‍👩‍👧‍👧}{👩‍❤️‍👨}{👩‍❤️‍👩}{👩‍❤️‍💋‍👨}{👩‍❤️‍💋‍👩}{👩‍👩‍👦}{👩‍👩‍👦‍👦}{👩‍👩‍👧}{👩‍👩‍👧‍👦}{👩‍👩‍👧‍👧}]"
    //            + "]"
    //            // TODO, add NamedSequences-8.0.0.txt, or at least Tamil
    //            ).freeze();
}
