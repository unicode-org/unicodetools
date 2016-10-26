package org.unicode.tools;

import java.util.List;
import java.util.Set;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.tool.Option;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.tool.Option.Params;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.EmojiData;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.EntryRange;

class ExtendedPictographic {
    static final UnicodeSet GLUE_AFTER_ZWJ = new UnicodeSet();
    static final String HEADER;

    static {
        StringBuilder header = new StringBuilder();
        boolean inHeader = true;
        for (String line : FileUtilities.in(Settings.DATA_DIR + "cldr/","ExtendedPictographic.txt")) {
            // U+02704  ; Glue_After_Zwj #  ✄   WHITE SCISSORS
            if (line.startsWith("#") || line.isEmpty()) {
                if (inHeader) {
                    header.append(line).append("\n");
                    if (line.startsWith("# DATA")) {
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

    public static void main(String[] args) {
        IndexUnicodeProperties iup = IndexUnicodeProperties.make();
        UnicodeMap<Age_Values> age = iup.loadEnum(UcdProperty.Age);
        
        final UnicodeSet picto = new UnicodeSet(GLUE_AFTER_ZWJ)
        .addAll(EmojiData.EMOJI_DATA.getSingletonsWithoutDefectives())
        .freeze();
        
        MyOptions.parse(args, true);
       
        if (MyOptions.list.option.doesOccur()) {
            System.out.println(picto.toPattern(false));
            showAge(picto, iup, age, false);
        }

        if (MyOptions.extended.option.doesOccur()) {
            System.out.println(GLUE_AFTER_ZWJ.toPattern(false));
            showAge(GLUE_AFTER_ZWJ, iup, age, false);
        }


        if (MyOptions.operations.option.doesOccur()) {
            UnicodeSet ops = new UnicodeSet("[[:s:][:p:]-[:sc:]-[:xidcontinue:]-[:nfkcqc=n:]&[:scx=Common:]]")
            .removeAll(picto);
//            showAge(ops, iup, age, true);
            
            for (String cp : ops) {
                System.out.println(showCodePoint(iup, null, cp.codePointAt(0))); 
            }
        }
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
            System.out.println("# " + (v == null ? "" : v.getShortName() + "; ") + us.size());
            for (EntryRange range : us.ranges()) {
                if (range.codepoint == range.codepointEnd) {
                    System.out.println(showCodePoint(iup, v, range.codepoint));
                } else if (all) {
                    for (int i = range.codepoint; i <= range.codepointEnd; ++i) {
                        System.out.println(showCodePoint(iup, v, i)); 
                    }
                } else {
                    System.out.println(showRange(iup, v, range.codepoint, range.codepointEnd));
                }
            }
            System.out.println();
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