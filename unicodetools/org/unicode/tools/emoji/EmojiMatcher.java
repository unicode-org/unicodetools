package org.unicode.tools.emoji;

import java.util.List;

import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

public class EmojiMatcher  {
    private int start;
    private int end;

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    static final UnicodeSet KEYCAP_START = new UnicodeSet("[0-9#*]");
    static final UnicodeSet BASE = new UnicodeSet("[\\p{Emoji}-\\p{Emoji_Component}]");
    static final UnicodeSet BASE2 = new UnicodeSet("[\\p{Emoji_Modifier}\uFE0F]");
    static final UnicodeSet TAG_SPEC = new UnicodeSet("[\\x{E0020}-\\x{E007E}]");
    /**
     * Returns true if a possible emoji occurs at or after offset. If true, use getStart() to find the start of the emoji, and getEnd() to get the end.
     * <pre>
emoji_sequence := 
| [0-9#*] \x{FE0F 20E3}
| ( \p{Regional_Indicator} \p{Regional_Indicator} )
| emoji_zwj_element ( \x{200d} emoji_zwj_element )+

emoji_zwj_element := 
  [\p{Emoji}-\p{Emoji_Component}] ( \p{Emoji_Modifier} | \x{FE0F} )? ( [\x{E0020}-\x{E007E}]+ \x{E007F} )? 
  </pre>
     * @param input
     * @param offset
     * @return
     */
    public FindStatus findPossible(String input, int offset) {
        int cp = 0;
        State state = State.start;
        for ( ; offset < input.length(); offset += Character.charCount(cp)) {
            cp = input.codePointAt(offset);
            switch (state) {
            case start: {
                start = offset;
                if (KEYCAP_START.contains(cp)) {
                    state = State.haveKeycap1;
                } else if (Emoji.REGIONAL_INDICATORS.contains(cp)) {
                    start = offset;
                    state = State.haveRegionalIndicator;
                } else if (BASE.contains(cp)) {
                    start = offset;
                    state = State.haveBase;
                }
                break;
            }
            case haveKeycap1: {
                if (cp == Emoji.EMOJI_VARIANT) {
                    state = State.haveKeycap2;
                    break;
                }
                // optional, fall through
            }
            case haveKeycap2: {
                if (cp == 0x20E3) {
                    this.end = offset + 1;
                    return FindStatus.full;
                }
                this.end = offset;
                return FindStatus.partial;
            }
            case haveRegionalIndicator: {
                if (Emoji.REGIONAL_INDICATORS.contains(cp)) {
                    this.end = offset + Character.charCount(cp);
                    return FindStatus.full;
                }
                this.end = offset;
                return FindStatus.partial;
            }
            case haveBase: {
                if (BASE2.contains(cp)) {
                    state = State.haveBase2;
                    break;
                }
                // optional, fallthrough
            }
            case haveBase2: {
                if (TAG_SPEC.contains(cp)) {
                    state = State.haveTag;
                    break;
                } else if (cp == Emoji.JOINER) {
                    state = State.haveZwj;
                    break;
                }
                this.end = offset;
                return FindStatus.full;
            }
            case haveTag: {
                if (TAG_SPEC.contains(cp)) {
                    state = State.haveTag;
                    break;
                } else if (cp == 0xE007F) {
                    state = State.tagDone;
                    break;
                }
                this.end = offset;
                return FindStatus.full;
            }
            case tagDone: {
                if (cp == Emoji.JOINER) {
                    state = State.haveZwj;
                    continue;
                }
                this.end = offset;
                return FindStatus.full;
            }
            case haveZwj: {
                if (BASE.contains(cp)) {
                    start = offset;
                    state = State.haveBase;
                    continue;
                }
                this.end = offset-1; // backup to before zwj
                return FindStatus.full;
            }
            default:
                throw new IllegalArgumentException();
            }
        }
        switch (state) {
        case start: {
            start = offset;
            this.end = offset;
            return FindStatus.none;
        }
        case haveBase: case haveBase2: case tagDone: {
            this.end = offset;
            return FindStatus.full;
        }
        case haveZwj: {
            this.end = offset-1; // backup to before zwj
            return FindStatus.full;
        }
        default: {
            this.end = offset;
            return FindStatus.partial;
        }
        }
    }

    public enum FindStatus {none, partial, full}

    private enum State {start, haveKeycap1, haveKeycap2, haveRegionalIndicator, haveBase, haveBase2, haveTag, tagDone, haveZwj}


    static final UnicodeSet fixed;
    static final UnicodeSet nopres = new UnicodeSet(EmojiData.EMOJI_DATA.getSingletonsWithDefectives())
            .removeAll(EmojiData.EMOJI_DATA.getEmojiPresentationSet());
    static final UnicodeSet components = EmojiData.of(Emoji.VERSION_BETA).getEmojiComponents();

    static {
        fixed = new UnicodeSet(EmojiData.EMOJI_DATA.getAllEmojiWithoutDefectives())
                .removeAll(components)
                .removeAll(nopres);
        for (String s : nopres) {
            fixed.add(s + Emoji.EMOJI_VARIANT);
        }
        String heart = "\u2764";
        boolean m = EmojiMatcher.fixed.contains(heart);

        String heartVs = "❤️";
        boolean m2 = EmojiMatcher.fixed.contains(heartVs);

        fixed.freeze();
    }
    static UnicodeSet singletonFailures = new UnicodeSet();

    static void parse(String input, List<String> emoji, List<String> noPres, List<String> nonEmoji) {
        int emojiEnd = 0;
        for (int offset = 0; offset < input.length();) {
            int match = ListEmojiGroups.matches(fixed, input, offset);
            if (match > offset) {
                if (emojiEnd < offset) {
                    String str = input.substring(emojiEnd, offset);
                    addNonEmoji(str, nonEmoji, noPres);
                }
                emoji.add(input.substring(offset, match));
                offset = emojiEnd = match;
            } else {
                ++offset;
            }
        }
        if (emojiEnd < input.length()) {
            String str = input.substring(emojiEnd);
            addNonEmoji(str, nonEmoji, noPres);
        }
    }

    private static boolean addNonEmoji(String str, List<String> nonEmoji, List<String> noPres2) {
        StringBuilder nonEmojiBuffer = new StringBuilder();
        for (int cp : CharSequences.codePoints(str)) {
            if (nopres.contains(cp)) {
                noPres2.add(UTF16.valueOf(cp));
            } else {
                nonEmojiBuffer.appendCodePoint(cp);
            }
        }
        return nonEmoji.add(nonEmojiBuffer.toString());
    }

    public static void main(String[] args) {
        EmojiMatcher m = new EmojiMatcher();
        boolean verbose = true;
        Object[][] tests = {
                {"a", FindStatus.none, 1, 1},
                {"a👶🏿b", FindStatus.full, 1, 5, FindStatus.none, 6, 6},
                {"a👶👶🏿b", FindStatus.full, 1, 3, FindStatus.full, 3, 7, FindStatus.none, 8, 8},
        };
        for (Object[] row : tests) {
            final String input = (String)(row[0]);
            int cursor = 0;
            for (int i = 1; i < row.length; i += 3) {
                FindStatus expectedStatus = (FindStatus) row[i];
                int expectedStart = (int) row[i+1];
                int expectedEnd = (int) row[i+2];
                FindStatus status = m.findPossible(input, cursor);
                System.out.println(cursor);
                if (verbose || status != expectedStatus) {
                    System.out.println((status != expectedStatus ? "Failed " : "OK") + " Status:\t" + input + "\texpected: " + expectedStatus + "\tactual: " + status);
                    m.findPossible(input, cursor);
                }
                if (verbose || m.getStart() != expectedStart) {
                    System.out.println(( m.getStart() != expectedStart ? "Failed " : "OK") + " Start:\t" + input + "\texpected: " + expectedStart + "\tactual: " + m.getStart());
                    m.findPossible(input, cursor);
                }
                if (verbose || m.getEnd() != expectedEnd) {
                    System.out.println((m.getEnd() != expectedEnd ? "Failed " : "OK") + " end:\t" + input + "\texpected: " + expectedEnd + "\tactual: " + m.getEnd());
                    m.findPossible(input, cursor);
                }
                cursor = expectedEnd;
            }
            System.out.println();
        }


        for (String s : EmojiData.EMOJI_DATA.getAllEmojiWithDefectives()) {
            if (EmojiData.MODIFIERS.contains(s)) {
                continue;
            }
            FindStatus status = check(m, s);
        }
    }

    private static FindStatus check(EmojiMatcher m, String s) {
        FindStatus status = m.findPossible(s, 0);
        if (status != FindStatus.full) {
            System.out.println("Failed: " + s + "\t" + status);
            m.findPossible(s, 0);
        } else if (m.getEnd() != s.length()) {
            System.out.println("Failed: " + s + "\t" + m.getEnd());
            m.findPossible(s, 0);
        }
        return status;
    }
}