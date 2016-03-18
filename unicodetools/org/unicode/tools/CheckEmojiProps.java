package org.unicode.tools;

import org.unicode.cldr.util.Counter;
import org.unicode.props.GenerateEnums;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.Emoji;
import org.unicode.tools.emoji.Emoji.ModifierStatus;
import org.unicode.tools.emoji.EmojiData;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.EntryRange;

public class CheckEmojiProps {
    static final IndexUnicodeProperties iup = IndexUnicodeProperties.make(GenerateEnums.ENUM_VERSION);
    static final UnicodeMap<String> General_Category = iup.load(UcdProperty.General_Category);
    static final UnicodeMap<String> Name = iup.load(UcdProperty.Name);
    static final UnicodeMap<String> Grapheme_Cluster_Break = iup.load(UcdProperty.Grapheme_Cluster_Break);
    static final UnicodeMap<String> Word_Break = iup.load(UcdProperty.Word_Break);
    static final UnicodeMap<String> Line_Break = iup.load(UcdProperty.Line_Break);
    static final EmojiData data = EmojiData.of(Emoji.VERSION_LAST_RELEASED);

    public static void main(String[] args) {
        
        final UnicodeSet specials = new UnicodeSet("[[:block=tags:]-[:cn:]-[:deprecated:]]")
        .add(0x200D)
        .add(0x20E3)
        .add(0x20E0)
        .add(0xFE0E)
        .add(0xFE0F)
        ;

        showSet("Specials", specials);
        
        final UnicodeSet flags = new UnicodeSet(0x1F1E6, 0x1F1FF);
        showSet("Flags", flags);


        final UnicodeSet zwjs = new UnicodeSet();
        for (String s : data.getZwjSequencesAll()) {
            boolean haveZwj = false;
            for (int cp : CharSequences.codePoints(s)) {
                if (cp == 0x200d) {
                    haveZwj = true;
                    continue;
                } else if (haveZwj) {
                    zwjs.add(cp);
                    haveZwj = false;
                }
            }
        }
        showSet("Modifiers", data.getModifierStatusSet(ModifierStatus.modifier));
        showSet("Modifier_Bases", data.getModifierBases());
        showSet("After_Joiners", zwjs);
        
        final UnicodeSet others = new UnicodeSet(data.getSingletonsWithDefectives())
        .removeAll(specials)
        .removeAll(flags)
        .removeAll(data.getModifierStatusSet(ModifierStatus.modifier))
        .removeAll(data.getModifierBases())
        .removeAll(zwjs)
        ;

        showSet("Others", others);
    }

    private static void showSet(String string, final UnicodeSet all) {
        System.out.println("\n" + string);
        UnicodeMap<String> m = new UnicodeMap<String>();
        for (String s : all) {
            m.put(s, "GCB=" + Grapheme_Cluster_Break.get(s) 
                    + "; WB=" + Word_Break.get(s) 
                    + "; LB=" + Line_Break.get(s));
        }
        Counter<String> c = new Counter<String>();
        for (String item : m.getAvailableValues()) {
            c.add(item, m.getSet(item).size());
        }
        for (String item : c.getKeysetSortedByCount(false)) {
            UnicodeSet us = m.getSet(item);
            System.out.println("\n" + us.size() + "\t" + item);
            System.out.println("\t" + us.toPattern(false));
            int max = 1;
            boolean firstMax = true;
            for (EntryRange s : us.ranges()) {
                if (max++ > 4) {
                    if (firstMax) {
                        firstMax = false;
                        System.out.print("\t");
                    } else {
                        System.out.print(", ");
                    }
                    System.out.print(Utility.hex(s.codepoint));
                    if (s.codepoint!=s.codepointEnd) {
                        System.out.print(".." + Utility.hex(s.codepointEnd));
                    }
                    continue;
                }
                boolean single = s.codepointEnd == s.codepoint;
                System.out.println("\t" + Utility.hex(s.codepoint) 
                        + (single ? "" : ".." + Utility.hex(s.codepointEnd))
                        + "\t" + Name.get(s.codepoint) 
                        + (single ? "" : ".." + Name.get(s.codepointEnd))
                        );
            }
            if (!firstMax) {
                System.out.println();
            }
        }
    }

    private static String show(UnicodeSet us) {
        StringBuilder result = new StringBuilder();
        for (EntryRange range : us.ranges()) {
            showRange(result, range);
        }
        return result.toString();
    }

    private static void showRange(StringBuilder result, EntryRange range) {
        if (result.length() != 0) {
            result.append(", ");
        }
        result.append(Utility.hex(range.codepoint));
        if (range.codepoint!=range.codepointEnd) {
            result.append(".." + Utility.hex(range.codepointEnd));
        }
    }
}
