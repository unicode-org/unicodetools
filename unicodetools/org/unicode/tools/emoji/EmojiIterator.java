package org.unicode.tools.emoji;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.unicode.tools.emoji.Emoji.ModifierStatus;

import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.UnicodeSet;

public final class EmojiIterator implements Iterable<String>, Iterator<String> {
    private static UnicodeSet COMBINING = new UnicodeSet("[:M:]");

    private int[] line;
    private int pos;
    private final StringBuffer result = new StringBuffer();
    public final Set<String> newLabel = new LinkedHashSet<>();
    private final UnicodeSet modifierBase;
    private final UnicodeSet modifier;
    private final boolean stripTrailingStyleVariants;

    public EmojiIterator(EmojiData data, boolean stripTrailingStyleVariants) {
        this.modifierBase = data.getModifierBases();
        this.modifier = data.getModifierStatusSet(ModifierStatus.modifier);
        this.stripTrailingStyleVariants = stripTrailingStyleVariants;
    }
    /**
     * Resets newLabel if there is a label.
     * @param line
     * @return 
     */
    public EmojiIterator set(String line) {
        line = Emoji.UNESCAPE.transform(line);
        pos = 0;
        int tabPos = line.indexOf('\t');
        if (tabPos >= 0) {
            newLabel.clear();
            String[] temp = line.substring(0,tabPos).trim().split(",\\s*");
            for (String part : temp) {
                if (Emoji.KEYWORD_CHARS.containsAll(part)) {
                    newLabel.add(part);
                } else {
                    throw new IllegalArgumentException("Bad line format: " + line + " — " + new UnicodeSet().addAll(part).removeAll(Emoji.KEYWORD_CHARS));
                }
            }
            line = line.substring(tabPos + 1);
        }
        line = line.replace(" ", "").trim();
        this.line = CharSequences.codePoints(line);
        return this;
    }

    /**
     * Gets the next sequence, either single code points or emoji sequences.
     * @return
     */
    public String next() {
        result.setLength(0);
        main:
        while (pos < line.length) {
            int first = line[pos++];
            result.appendCodePoint(first);
            // we need to look at the next character
            if (pos >= line.length) {
                break;
            }
            int current = line[pos++];

            // two special cases: RI+RI and MB+MO

            if (Emoji.isRegionalIndicator(first) && Emoji.isRegionalIndicator(current)) {
                result.appendCodePoint(current);
                if (pos >= line.length) {
                    break;
                }
                current = line[pos++];
            } else if (modifier.contains(current) && modifierBase.contains(first)) {
                result.appendCodePoint(current);
                if (pos >= line.length) {
                    break;
                }
                current = line[pos++];
            }

            // add any combining marks

            while (COMBINING.contains(current)) {
                result.appendCodePoint(current);
                if (pos >= line.length) {
                    break main;
                }
                current = line[pos++];
            }

            // we're done, unless we have a joiner. For simplicity, we'll join with whatever's next
            // better would be to make sure it's emoji.

            if (current != Emoji.JOINER) {
                --pos;
                break;
            }
            // add joiner
            result.appendCodePoint(current);
        }
        // remove trailing emoji variant
        if (stripTrailingStyleVariants) {
            final char finalChar = result.charAt(result.length()-1);
            if (finalChar == Emoji.EMOJI_VARIANT || finalChar == Emoji.TEXT_VARIANT) {
                result.setLength(result.length()-1);
            }
        }
        return result.toString();
    }

    @Override
    public boolean hasNext() {
        return pos < line.length;
    }

    @Override
    public Iterator<String> iterator() {
        return this;
    }

    // quick test.
    public static void main(String[] args) {
        String[] tests = {
                " couple\t👩‍❤️‍💋‍👩",
                " flag, junk\t🇰🇷🦄👦🏻 ㊗\uFE0F㊗\uFE0F",
                " face \t  😀  😁  😂  😃  😄  😅  😆  😉   ",
                " 😊  😋  😎  😍  😘  😗 😙  😚",
                " unicorn \t  🦄",
                " cheese  \t🧀🍕",
                " no bullying, witness  \t  👁‍🗨",
        };
        EmojiIterator ei = new EmojiIterator(EmojiData.of(Emoji.VERSION_LAST_RELEASED), true);
        for (String line : tests) {
            ei.set(line);
            for (String s : ei) {
                System.out.println(ei.newLabel + "\t«" + s + "»");
            }
        }
    }
}