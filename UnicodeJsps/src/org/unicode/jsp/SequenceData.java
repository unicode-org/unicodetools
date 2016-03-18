package org.unicode.jsp;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;



// import com.google.common.base.Splitter;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.UnicodeSet;

public class SequenceData {
    static final UnicodeSet REGIONAL_INDICATORS = new UnicodeSet(0x1F1E6,0x1F1FF).freeze();
    static final UnicodeSet MARK = new UnicodeSet("[:M:]").freeze();
    static final UnicodeSet MODIFIER_BASE = new UnicodeSet("[:emoji_modifier_base:]").freeze();

//    static final Splitter SEMI = Splitter.onPattern("[;#]").trimResults();
//    static final Splitter COMMA = Splitter.on(",").trimResults();
    static final Pattern SEMI = Pattern.compile("\\s*[;#]\\s*");
    static final Pattern COMMA = Pattern.compile("\\s*,\\s");

    static final UnicodeSet EMOJI_MODIFIER_SEQUENCES = new UnicodeSet();
    static final UnicodeSet EMOJI_ZWJ_SEQUENCES = new UnicodeSet();
    static final UnicodeSet EMOJI_FLAG_SEQUENCES = new UnicodeSet();
    static final UnicodeSet EMOJI_KEYCAP_SEQUENCES = new UnicodeSet();
    static final UnicodeSet EMOJI_DEFECTIVES = new UnicodeSet(REGIONAL_INDICATORS);

    static final UnicodeMap<UnicodeSet> VARIATION_BASE_TO_SELECTORS = new UnicodeMap<UnicodeSet>();

    static {
        for (String file : Arrays.asList("emoji-sequences.txt", "emoji-zwj-sequences.txt")) {
            boolean zwj = file.contains("zwj");
            for (String line : FileUtilities.in(SequenceData.class, file)) {
                if (line.startsWith("#") || line.isEmpty()) {
                    continue;
                }
                List<String> list = Arrays.asList(SEMI.split(line));
                String source = Utility.fromHex(list.get(0), 4, " ");
                int first = source.codePointAt(0);
                if (zwj) {
                    if (!source.contains("\u2764") || source.contains("\uFE0F")) {
                        EMOJI_ZWJ_SEQUENCES.add(source);
                    }
                } else {
                    if (REGIONAL_INDICATORS.contains(first)) {
                        EMOJI_FLAG_SEQUENCES.add(source);
                    } else if (MODIFIER_BASE.contains(first)) {
                        EMOJI_MODIFIER_SEQUENCES.add(source);
                    } else if (MARK.containsSome(source)) {
                        EMOJI_KEYCAP_SEQUENCES.add(source);
                        EMOJI_DEFECTIVES.add(source.codePointAt(0));
                        // TODO fix data
                        final StringBuilder sourceWithVs = new StringBuilder()
                        .appendCodePoint(first)
                        .append('\uFE0F')
                        .append(source, Character.charCount(first), source.length());
                        EMOJI_KEYCAP_SEQUENCES.add(sourceWithVs);
                    } else {
                        throw new IllegalArgumentException("internal error");
                    }
                }
            }
        }
        for (String line : FileUtilities.in(SequenceData.class, "StandardizedVariants.txt")) {
            if (line.startsWith("#") || line.isEmpty()) continue;
            List<String> list = Arrays.asList(SEMI.split(line));
            String source = Utility.fromHex(list.get(0), 4, " ");
            int first = source.codePointAt(0);
            int second = source.codePointAt(Character.charCount(first));
            UnicodeSet selectors = VARIATION_BASE_TO_SELECTORS.get(first);
            // warning, only add immutable items as values to UnicodeMap
            if (selectors == null) {
                VARIATION_BASE_TO_SELECTORS.put(first, selectors = new UnicodeSet(second, second));
            } else {
                VARIATION_BASE_TO_SELECTORS.put(first, selectors = new UnicodeSet(selectors).add(second));
            }
        }
        EMOJI_ZWJ_SEQUENCES.freeze();
        EMOJI_FLAG_SEQUENCES.freeze();
        EMOJI_KEYCAP_SEQUENCES.freeze();
        EMOJI_MODIFIER_SEQUENCES.freeze();
        EMOJI_DEFECTIVES.freeze();
        
        VARIATION_BASE_TO_SELECTORS.freeze();
    }
    public static void main(String[] args) {
        System.out.println("EMOJI_ZWJ_SEQUENCES: " + EMOJI_ZWJ_SEQUENCES.toPattern(false));
        System.out.println("EMOJI_FLAG_SEQUENCES: " + EMOJI_FLAG_SEQUENCES.toPattern(false));
        System.out.println("EMOJI_KEYCAP_SEQUENCES: " + EMOJI_KEYCAP_SEQUENCES.toPattern(false));
        System.out.println("EMOJI_MODIFIER_SEQUENCES: " + EMOJI_MODIFIER_SEQUENCES.toPattern(false));
        System.out.println("EMOJI_DEFECTIVES: " + EMOJI_DEFECTIVES.toPattern(false));
        System.out.println("VARIATION_BASE_TO_SELECTORS: " + VARIATION_BASE_TO_SELECTORS);
    }
}
