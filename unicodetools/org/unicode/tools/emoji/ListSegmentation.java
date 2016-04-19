package org.unicode.tools.emoji;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.props.UcdPropertyValues.Grapheme_Cluster_Break_Values;
import org.unicode.props.UcdPropertyValues.Line_Break_Values;
import org.unicode.props.UcdPropertyValues.Word_Break_Values;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UnicodeSet;

public class ListSegmentation {
    private static final UnicodeSet SINGLETONS_WITHOUT_DEFECTIVES = EmojiData.EMOJI_DATA.getSingletonsWithoutDefectives();
    static final IndexUnicodeProperties iup = IndexUnicodeProperties.make("9.0.0");
    static final UnicodeMap<Line_Break_Values> lb = iup.loadEnum(UcdProperty.Line_Break, Line_Break_Values.class);
    static final UnicodeMap<Word_Break_Values> wb = iup.loadEnum(UcdProperty.Word_Break, Word_Break_Values.class);
    static final UnicodeMap<Grapheme_Cluster_Break_Values> gcb = iup.loadEnum(UcdProperty.Grapheme_Cluster_Break, Grapheme_Cluster_Break_Values.class);
    static final UnicodeMap<General_Category_Values> gc = iup.loadEnum(UcdProperty.General_Category, General_Category_Values.class);
    static final UnicodeSet WNC = new UnicodeSet("[👤  🐉 ☕ 🚀 🏍 💻]");
    public static void main(String[] args) {
        UnicodeMap<List<String>> items = new UnicodeMap<>();
        for (String s : SINGLETONS_WITHOUT_DEFECTIVES) {
            items.put(s, Arrays.asList(
                    gcb.get(s).getShortName(),
                    wb.get(s).getShortName(),
                    lb.get(s).getShortName(),
                    gc.get(s).getShortName(),
                    (WNC.contains(s) ? "WNC2" : "")
                    ));
        }
        for (List<String> value : items.values()) {
            UnicodeSet us = items.getSet(value);
            System.out.println(CollectionUtilities.join(value, "\t")  + "\t" +  us.size() + "\t" + show(us));
        }

        UnicodeSet picto1 = new UnicodeSet("[★ ☇-☍ ☏ ☐ ☒ ☖ ☗ ☙-☜ ☞ ☟ ☡ ☤ ☥ ☧-☩ ☫-☭ ☻-♇ ♔-♟ ♡ ♢ ♤ ♧ ♩-♬ ♰-♺ ♼-♾ ⚀-⚉ ⚐ ⚑ ⚕ ⚘ ⚚ ⚝-⚟ ⚢-⚩ ⚬-⚯ ⚲-⚼ ⚿-⛃ ⛆ ⛇ ⛉-⛍ ⛐ ⛒ ⛕-⛨ ⛫-⛯ ⛶ ⛻ ⛼ ⛾ ⛿ ✎ ✐ ❥ ⚊-⚏ ☰-☷ ♭-♯ 🀀-🀃 🀅-🀫 🌢 🌣 🎔 🎕 🎘 🎜 🎝 🏱 🏲 🏶 📾 🔾-🕈 🕨-🕮 🕱 🕲 🕻-🖆 🖈 🖉 🖎 🖏 🖑-🖔 🖗-🖣 🖦 🖧 🖩-🖰 🖳-🖻 🖽-🗁 🗅-🗐 🗔-🗛 🗟 🗠 🗢 🗤-🗧 🗩-🗮 🗰-🗲 🗴-🗹 🛆-🛊 🛦-🛨 🛪 🛱 🛲]");
        System.out.println("Picto");
        showList(picto1);
        UnicodeSet rest = new UnicodeSet("["
                + "[:block=Miscellaneous Symbols:]"
                +"[:block=Dingbats:]"
                +"[:block=Mahjong Tiles:]"
                +"[:block=Domino Tiles:]"
                +"[:block=Playing Cards:]"
                +"[:block=Dingbats:]"
                +"[:block=Miscellaneous Symbols And Pictographs:]"
                +"[:Block=Arrows:]"
                +"[:Block=Emoticons:]"
                +"[:Block=Transport_And_Map_Symbols:]"
                +"[:Block=Miscellaneous_Symbols_And_Arrows:]"
                +"[:Block=Miscellaneous_Technical:]"
                +"[:Block=Playing_Cards:]"
                +"[:Block=Supplemental_Arrows_B:]"
                +"[:Block=Supplemental_Symbols_And_Pictographs:]"
                +"-[:c:]]")
        .removeAll(SINGLETONS_WITHOUT_DEFECTIVES)
        .removeAll(picto1)
        ; 
        rest = new UnicodeSet("[⎈ ✀ ✁ ✃ ✄ ✑ ❦ ❧ 🀰-🂓 🂠-🂮 🂱-🂿 🃁-🃎 🃑-🃵 🕏]");
        System.out.println("\nOther possible pictographs"  + "\t" +  rest.size()); //  + "\t" + rest.toPattern(false));
        showList(rest);
    }
    
    private static void showList(UnicodeSet rest) {
        Set<String> sorted = rest.addAllTo(new TreeSet<String>(EmojiOrder.FULL_COMPARATOR));
        for (String s : sorted) {
            System.out.println("U+" + Utility.hex(s) + "\t" + UCharacter.getName(s, ", ") + "\t" + s);
        }
    }
    private static String show(UnicodeSet us) {
        Set<String> sorted = us.addAllTo(new TreeSet<String>(EmojiOrder.FULL_COMPARATOR));
        return CollectionUtilities.join(sorted, " ");
    }
}
