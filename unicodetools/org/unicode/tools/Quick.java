package org.unicode.tools;

import java.util.EnumSet;
import java.util.Set;

import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Block_Values;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.EmojiData;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.VersionInfo;

public class Quick {
    static final IndexUnicodeProperties iup = IndexUnicodeProperties.make(Settings.latestVersion);
    static final UnicodeMap<String> names = iup.load(UcdProperty.Name);
    static final UnicodeMap<Block_Values> blocks = iup.loadEnum(UcdProperty.Block, Block_Values.class);
    static final IndexUnicodeProperties iupOld = IndexUnicodeProperties.make(Settings.lastVersion);
    static final EmojiData emojiData = EmojiData.forUcd(VersionInfo.getInstance(9));
    static final UnicodeMap<General_Category_Values> gencat = iup.loadEnum(UcdProperty.General_Category, General_Category_Values.class);
    static final UnicodeSet Cn = gencat.getSet(General_Category_Values.Unassigned);
    
    public static void main(String[] args) {
        Set<Block_Values> emojiBlocks = EnumSet.noneOf(Block_Values.class);
        UnicodeSet emoji = emojiData.getSingletonsWithoutDefectives();
        for (String s : new UnicodeSet(emoji).addAll(ExtendedPictographic.GLUE_AFTER_ZWJ)) {
            Block_Values block = blocks.get(s);
            emojiBlocks.add(block);
        }

        System.out.println(ExtendedPictographic.HEADER);
        for (Block_Values block : emojiBlocks) {
            System.out.println("# " + block);
            
            UnicodeSet blockSet = blocks.getSet(block);
            UnicodeSet emojiInBlock = new UnicodeSet(blockSet).retainAll(emoji);
            UnicodeSet gazInBlock = new UnicodeSet(blockSet).retainAll(ExtendedPictographic.GLUE_AFTER_ZWJ);
            UnicodeSet gazInBlockNoCn = new UnicodeSet(gazInBlock).removeAll(Cn);
            UnicodeSet gazInBlockCn = new UnicodeSet(gazInBlock).retainAll(Cn);
            UnicodeSet cnInBlock = new UnicodeSet(blockSet).retainAll(Cn).removeAll(gazInBlock);
            UnicodeSet otherInBlock = new UnicodeSet(blockSet).removeAll(emojiInBlock).removeAll(cnInBlock).removeAll(gazInBlock);

            showNonEmpty("emoji", emojiInBlock, true);
            showNonEmpty("gaz", gazInBlock, true);
            showNonEmpty("other", otherInBlock, true);
            if (!gazInBlock.isEmpty()) {
                if (block != Block_Values.No_Block) {
                    showNonEmpty("otherCn", cnInBlock, false);
                }
                System.out.println();
                showRanges(gazInBlockNoCn, true);
                showRanges(gazInBlockCn, false);
            }

            showNonEmpty("count", gazInBlock, false);
            System.out.println();
        }
        showNonEmpty("total_count", ExtendedPictographic.GLUE_AFTER_ZWJ, false);
        System.out.println("# EOF");
    }

    private static void showRanges(UnicodeSet gazInBlock, boolean includeSetName) {
        for (UnicodeSet.EntryRange range : gazInBlock.ranges()) {
            System.out.println(
                    printRange(range.codepoint, range.codepointEnd)
                    + "\t; Glue_After_Zwj"
                    + " #\t"
                    + (includeSetName ?  
                    new UnicodeSet(range.codepoint, range.codepointEnd).toPattern(false)
                    + "\t" + getNames(range.codepoint, range.codepointEnd) : "GC=Cn")
                    );
        }
    }

    private static void showNonEmpty(String title, UnicodeSet emojiInBlock, boolean includeUS) {
        if (!emojiInBlock.isEmpty()) {
            System.out.println("# " + title + "=" + emojiInBlock.size() + (includeUS ? "\t: " + emojiInBlock.toPattern(false) : ""));
        }
    }

    //        UnicodeMap<Line_Break_Values> linebreak = iup.loadEnum(UcdProperty.Line_Break, Line_Break_Values.class);
    //        UnicodeMap<Line_Break_Values> linebreakOld = iupOld.loadEnum(UcdProperty.Line_Break, Line_Break_Values.class);
    //        UnicodeSet ID = linebreak.getSet(Line_Break_Values.Ideographic);
    //        UnicodeSet IdOld = linebreakOld.getSet(Line_Break_Values.Ideographic);
    //        UnicodeSet IdCn = new UnicodeSet(ID)
    //        .removeAll(IdOld)
    //        .retainAll(Cn)
    //        .freeze();
    //        UnicodeMap<Block_Values> blockIdCn = new UnicodeMap<>(blocks);
    //        blockIdCn.retainAll(IdCn);
    //        lastBlock = null;
    //        for (EntryRange<Block_Values> range : blockIdCn.entryRanges()) {
    //            if (range.value != lastBlock) {
    //                System.out.println();
    //                lastBlock = range.value;
    //            }
    //            System.out.println(
    //                    printRange(range.codepoint, range.codepointEnd)
    //                    + "\t; Glue_After_Zwj #\t" + range.value
    //                    );
    //        }

    private static String getNames(int codepoint, int codepointEnd) {
        String result = getName(codepoint);
        String result2 = getName(codepointEnd);
        if (!result.equals(result2)) {
            result += " .. " + result2;
        }
        return result;
    }

    private static String getName(int codepoint) {
        String result = names.get(codepoint);
        return result == null ? "∈" + blocks.get(codepoint).toString() : result;
    }

    private static String printRange(int start, int end) {
        return "U+" + Utility.hex(start)
                + (start == end ? "" : "..U+" + Utility.hex(end));
    }
}
