package org.unicode.tools.emoji;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.unicode.cldr.util.With;
import org.unicode.text.utility.Utility;

import com.google.common.collect.ImmutableList;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.SpanCondition;
import com.ibm.icu.text.UnicodeSetSpanner;
import com.ibm.icu.text.UnicodeSetSpanner.CountMethod;

public class GenerateSpecImage {

    private static final String WOMAN_STR = "👩";
    private static final String MAN_STR = "👨";

    private static final UnicodeSet emojiSet = EmojiData.EMOJI_DATA.getAllEmojiWithDefectives();
    public static void main(String[] args) {
        //show("👶 🧒 👦 👧 🧑 👱 👨 👱‍♂️");
        for (Target target : Target.values()) {
            for (ModifierUse useModifiers : ModifierUse.values()) {
                System.out.println("\n" + target + ": " + useModifiers);
                combinations("👭", WOMAN_STR, "🤝", WOMAN_STR, useModifiers, target);
                combinations("👫", WOMAN_STR, "🤝", MAN_STR, useModifiers, target);
                combinations("👬", MAN_STR, "🤝", MAN_STR, useModifiers, target);
                System.out.println();
            }
        }
    }

    static List<String> modifiers;
    static {
        List<String> temp = new ArrayList<>();
        EmojiData.MODIFIERS.addAllTo(temp);
        modifiers = ImmutableList.copyOf(temp);
    }

    enum ModifierUse {withModifiers, noModifiers, collation}
    enum Target {candidates, doc}

    private static void combinations(String single, String a, String b, String c, ModifierUse useModifiers, Target target) {
        int i = 0;
        switch (useModifiers) {
        case noModifiers: {
            String result1 = a + Emoji.JOINER_STR + b + Emoji.JOINER_STR + c;
            showItem("no-tone", i, result1, target);
            break;
        }
        case withModifiers: {
            for (String s : modifiers) {
                for (String e : modifiers) {
                    if (a.equals(c)) {
                        int indexA = modifiers.indexOf(s);
                        int indexB = modifiers.indexOf(e);
                        if (indexA < indexB) {
                            continue;
                        }
                    }
                    String result = a + s + Emoji.JOINER_STR + b + Emoji.JOINER_STR + c + e;
                    // 1F9B9 1F3FF 200D 2642 FE0F  ; Emoji_ZWJ_Sequence  ; man supervillain: dark skin tone   
                    i = showItem(single, i, result, target);
                }
            }  
            break;
        }
        case collation: {
            String result1 = a + Emoji.JOINER_STR + b + Emoji.JOINER_STR + c;
            System.out.println(single + "<< " + result1);
            for (String s : modifiers) {
                for (String e : modifiers) {
                    if (a.equals(c)) {
                        int indexA = modifiers.indexOf(s);
                        int indexB = modifiers.indexOf(e);
                        if (indexA < indexB) {
                            continue;
                        }
                    }
                String result = a + s + Emoji.JOINER_STR + b + Emoji.JOINER_STR + c + e;
                System.out.println("<< " + result);
                }
            }  
            break;
        }
        }

    }

    static UnicodeSetSpanner MOD_SCAN = new UnicodeSetSpanner(EmojiData.MODIFIERS);
    static UnicodeSetSpanner MAN_WOMAN_SCAN = new UnicodeSetSpanner(Emoji.MAN_OR_WOMAN);

    private static int showItem(String single, int i, String result, Target target) {
        String name = getName(result);
        switch (target) {
        case candidates: {
            System.out.println("U+" + Utility.hex(result, " U+") + "\n" 
                    + "Name=" + name);
        }
        case doc: {
            System.out.println(Utility.hex(result, " ") + " ; Emoji_ZWJ_Sequence # " 
                    + "\t (" + result + ")\t" + name);
        }
        }
        return i;
    }

    private static String getName(String result) {
        String genders = MAN_WOMAN_SCAN.replaceFrom(result, "", CountMethod.MIN_ELEMENTS, SpanCondition.NOT_CONTAINED);
        String name;
        switch (genders) {
        case MAN_STR+MAN_STR: name = "men holding hands"; break;
        case WOMAN_STR+WOMAN_STR: name = "women holding hands"; break;
        case WOMAN_STR+MAN_STR: name = "woman and man holding hands"; break;
        default: throw new IllegalArgumentException();
        }
        String mods = MOD_SCAN.replaceFrom(result, "", CountMethod.MIN_ELEMENTS, SpanCondition.NOT_CONTAINED);
        if (!mods.isEmpty()) {
            String modString = "";
            int last = -1;
            for (int mod : With.codePointArray(mods)) {
                if (mod == last) {
                    continue;
                }
                if (!modString.isEmpty()) {
                    modString += ", ";
                }
                switch (UTF16.valueOf(mod)) {
                case "🏻": modString += "light skin tone"; break;
                case "🏼": modString += "medium-light skin tone"; break;
                case "🏽": modString += "medium skin tone"; break;
                case "🏾": modString += "medium-dark skin tone"; break;
                case "🏿": modString += "dark skin tone"; break;
                }
                last = mod;
            }
            name += ": " + modString;
        }
        return name;
    }

    private static void show(String string) {
        for (int cp : With.codePointArray(string)) {
            if (!emojiSet.contains(cp)) {
                continue;
            }
            String cell = GenerateEmoji.getBestImage(UTF16.valueOf(cp), false, null);
            System.out.println(cell);
        }
    }
}
