package org.unicode.tools;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.tool.Option;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.tool.Option.Params;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.PropertyNames.Named;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.props.UcdPropertyValues.Block_Values;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.Emoji;
import org.unicode.tools.emoji.EmojiData;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.EntryRange;

class ExtendedPictographic {
    static final EmojiData emojiData = EmojiData.of(Emoji.VERSION11);
    
    static final UnicodeSet GLUE_AFTER_ZWJ = new UnicodeSet();
    static String HEADER;
    static final IndexUnicodeProperties iup = IndexUnicodeProperties.make(Settings.latestVersion);
    static final UnicodeMap<Age_Values> age = iup.loadEnum(UcdProperty.Age);
    static final UnicodeMap<String> names = iup.load(UcdProperty.Name);
    static final UnicodeMap<General_Category_Values> gencat = iup.loadEnum(UcdProperty.General_Category, General_Category_Values.class);
    static final UnicodeSet Cn = gencat.getSet(General_Category_Values.Unassigned);
    static final UnicodeMap<Block_Values> blocks = iup.loadEnum(UcdProperty.Block, Block_Values.class);

    static PrintWriter out = null;

    static void load(File file) throws IOException {
        StringBuilder header = new StringBuilder();
        boolean inHeader = true;
        for (String line : FileUtilities.in(file.getParent(), file.getName())) { // Settings.DATA_DIR + "cldr/","ExtendedPictographic.txt")
            // U+02704  ; Glue_After_Zwj #  ✄   WHITE SCISSORS
            if (line.startsWith("#") || line.isEmpty()) {
                if (inHeader) {
                    header.append(line).append("\n");
                    if (line.startsWith("# =====")) {
                        inHeader = false;
                    }
                }
                continue;
            }
            List<String> coreList = EmojiData.hashOnly.splitToList(line);
            List<String> list = EmojiData.semi.splitToList(coreList.get(0));
            final String f0 = list.get(0);
            int codePoint, codePointEnd;
            int pos = f0.indexOf("..");
            if (pos < 0) {
                codePoint = codePointEnd = Utility.fromHex(f0).codePointAt(0);
            } else {
                codePoint = Utility.fromHex(f0.substring(0,pos)).codePointAt(0);
                codePointEnd = Utility.fromHex(f0.substring(pos+2)).codePointAt(0);
            }
            GLUE_AFTER_ZWJ.add(codePoint,codePointEnd);
        }
        GLUE_AFTER_ZWJ.freeze();
        HEADER = header.toString();
    }

    enum MyOptions {
        destination(new Params().setHelp("File to read and change.")
                .setMatch(".*")
                .setDefault(CLDRPaths.COMMON_DIRECTORY + "properties/ExtendedPictographic.txt")),
        normal(new Params().setHelp("Generate the CLDR file (default option)")),
        list(new Params().setHelp("List the extended pictographs and emoji")),
        operations(new Params().setHelp("List the operations")),
        extended(new Params().setHelp("List the extended pictographs without emoji")),
        ;

        // BOILERPLATE TO COPY
        final Option option;
        private MyOptions(Params params) {
            option = new Option(this, params);
        }
        private static Options myOptions = new Options();
        static {
            for (MyOptions option : MyOptions.values()) {
                myOptions.add(option, option.option);
            }
        }
        private static Set<String> parse(String[] args, boolean showArguments) {
            return myOptions.parse(MyOptions.values()[0], args, true);
        }
    }

    public static void main(String[] args) throws IOException {
        //        IndexUnicodeProperties iup = IndexUnicodeProperties.make();
        //        UnicodeMap<Age_Values> age = iup.loadEnum(UcdProperty.Age);

        MyOptions.parse(args, true);
        File file = new File(MyOptions.destination.option.getValue());
        load(file);
        
        try (PrintWriter _out = FileUtilities.openUTF8Writer(file.getParent(), file.getName())) {
            out = _out;
            boolean doNormal = true;

            final UnicodeSet picto = new UnicodeSet(GLUE_AFTER_ZWJ)
                    .addAll(EmojiData.EMOJI_DATA.getSingletonsWithoutDefectives())
                    .freeze();

            if (MyOptions.list.option.doesOccur()) {
                out.println(picto.toPattern(false));
                showAge(picto, iup, age, false);
                doNormal = false;
            }

            if (MyOptions.extended.option.doesOccur()) {
                out.println(GLUE_AFTER_ZWJ.toPattern(false));
                showAge(GLUE_AFTER_ZWJ, iup, age, false);
                doNormal = false;
            }


            if (MyOptions.operations.option.doesOccur()) {
                UnicodeSet ops = new UnicodeSet("[[:s:][:p:]-[:sc:]-[:xidcontinue:]-[:nfkcqc=n:]&[:scx=Common:]]")
                        .removeAll(picto);
                //            showAge(ops, iup, age, true);

                for (String cp : ops) {
                    out.println(showCodePoint(iup, null, cp.codePointAt(0))); 
                }
                doNormal = false;
            }

            if (doNormal || MyOptions.normal.option.doesOccur()) {
                Set<Block_Values> emojiBlocks = EnumSet.noneOf(Block_Values.class);
                UnicodeSet emoji = emojiData.getSingletonsWithoutDefectives();
                for (String s : new UnicodeSet(emoji).addAll(ExtendedPictographic.GLUE_AFTER_ZWJ)) {
                    Block_Values block = blocks.get(s);
                    emojiBlocks.add(block);
                }

                out.println(ExtendedPictographic.HEADER);
                for (Block_Values block : emojiBlocks) {
                    out.println("# " + block);

                    UnicodeSet blockSet = blocks.getSet(block);
                    UnicodeSet emojiInBlock = new UnicodeSet(blockSet).retainAll(emoji);
                    UnicodeSet gazInBlock = new UnicodeSet(blockSet).retainAll(ExtendedPictographic.GLUE_AFTER_ZWJ);
                    UnicodeSet gazInBlockNoCn = new UnicodeSet(gazInBlock).removeAll(Cn);
                    UnicodeSet gazInBlockCn = new UnicodeSet(gazInBlock).retainAll(Cn);
                    UnicodeSet cnInBlock = new UnicodeSet(blockSet).retainAll(Cn).removeAll(gazInBlock);
                    UnicodeSet otherInBlock = new UnicodeSet(blockSet).removeAll(emojiInBlock).removeAll(cnInBlock).removeAll(gazInBlock);

                    showNonEmpty("emoji", emojiInBlock, true);
                    showNonEmpty("EP", gazInBlock, true);
                    showNonEmpty("other", otherInBlock, true);
                    if (!gazInBlock.isEmpty()) {
                        if (block != Block_Values.No_Block) {
                            showNonEmpty("otherCn", cnInBlock, false);
                        }
                        out.println();
                        showRanges(gazInBlockNoCn, true);
                        showRanges(gazInBlockCn, false);
                    }

                    showNonEmpty("count", gazInBlock, false);
                    out.println();
                }
                showNonEmpty("total_count", ExtendedPictographic.GLUE_AFTER_ZWJ, false);
                out.println("# EOF");
            }
        }
    }

    private static void showValue(int cp, final UcdProperty prop, final Class classIn) {
        Named value = (Named) iup.loadEnum(prop, classIn).get(cp);
        out.println(Utility.hex(cp) + " " + names.get(cp) 
        + " → " + prop.getShortName() + "=" + value.getShortName() + "\t" + prop + "=" + value);
    }

    private static void showNonEmpty(String title, UnicodeSet emojiInBlock, boolean includeUS) {
        if (!emojiInBlock.isEmpty()) {
            out.println("# " + title + "=" + emojiInBlock.size() + (includeUS ? "\t: " + emojiInBlock.toPattern(false) : ""));
        }
    }

    private static void showRanges(UnicodeSet gazInBlock, boolean includeSetName) {
        for (UnicodeSet.EntryRange range : gazInBlock.ranges()) {
            out.println(
                    printRange(range.codepoint, range.codepointEnd)
                    + "\t; ExtendedPictographic"
                    + " #\t"
                    + (includeSetName ?  
                            new UnicodeSet(range.codepoint, range.codepointEnd).toPattern(false)
                            + "\t" + getNames(range.codepoint, range.codepointEnd) : "GC=Cn")
                    );
        }
    }

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

    private static void showAge(final UnicodeSet picto, IndexUnicodeProperties iup, UnicodeMap<Age_Values> age, boolean all) {
        UnicodeMap<Age_Values> mm = new UnicodeMap<>();
        for (String s : picto) {
            final Age_Values currentAge = age.get(s);
            mm.put(s, currentAge);
        }
        for (Age_Values v : mm.values()) {
            if (v == Age_Values.Unassigned) {
                continue;
            }
            UnicodeSet us = mm.getSet(v);
            out.println("# " + (v == null ? "" : v.getShortName() + "; ") + us.size());
            for (EntryRange range : us.ranges()) {
                if (range.codepoint == range.codepointEnd) {
                    out.println(showCodePoint(iup, v, range.codepoint));
                } else if (all) {
                    for (int i = range.codepoint; i <= range.codepointEnd; ++i) {
                        out.println(showCodePoint(iup, v, i)); 
                    }
                } else {
                    out.println(showRange(iup, v, range.codepoint, range.codepointEnd));
                }
            }
            out.println();
        }
    }

    private static String showRange(IndexUnicodeProperties iup, Age_Values v, int cpStart, int cpEnd) {
        return Utility.hex(cpStart) + ".." + Utility.hex(cpEnd)
        + (v == null ? "" : "; " + v.getShortName())
        + " # " 
        + UTF16.valueOf(cpStart) + ".." + UTF16.valueOf(cpEnd) + "; " 
        + iup.getName(cpStart) + ".." + iup.getName(cpEnd);
    }
    private static String showCodePoint(IndexUnicodeProperties iup, Age_Values v, int cp) {
        return Utility.hex(cp)
                + (v == null ? "" : "; " + v.getShortName())
                + " # " 
                + UTF16.valueOf(cp) + "; " 
                + iup.getName(cp);
    }
}