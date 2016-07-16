package org.unicode.tools.emoji;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;

import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.EmojiData.VariantHandling;

import com.google.common.collect.ImmutableSet;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUUncheckedIOException;

public class GenerateEmojiKeyboard {
    public static void main(String[] args) throws Exception {
        GenerateEmojiKeyboard.showLines(EmojiOrder.STD_ORDER, true);
        //        boolean foo2 = EmojiData.EMOJI_DATA.getChars().contains(EmojiData.SAMPLE_WITHOUT_TRAILING_EVS);
        //        Set<String> foo = EmojiOrder.sort(EmojiOrder.STD_ORDER.codepointCompare, 
        //                EmojiData.EMOJI_DATA.getChars());
        //
        //        showDiff(EmojiData.EMOJI_DATA.getChars(), EmojiOrder.STD_ORDER.emojiData.getSortingChars());
    }

    private static void showDiff(UnicodeSet chars, UnicodeSet sortingChars) {
        System.out.println(new UnicodeSet(chars).removeAll(sortingChars));
        System.out.println(new UnicodeSet(sortingChars).removeAll(chars));
    }

    public static void showLines(EmojiOrder emojiOrder, boolean spreadsheet) throws IOException {
        Set<String> retain = ImmutableSet.copyOf(emojiOrder.emojiData.getSortingChars().addAllTo(new HashSet<String>()));
        UnicodeSet allCharacters = new UnicodeSet();
        EmojiOrder.MajorGroup lastMajorGroup = null;
        TempPrintWriter out = null;
        int i = 0;
        int total = 0;
        int totalNoMod = 0;
        int totalNoModNoSign = 0;
        for (Entry<String, Set<String>> labelToSet : emojiOrder.orderingToCharacters.keyValuesSet()) {
            boolean isFirst = true;
            final String label = labelToSet.getKey();
            final Set<String> list = labelToSet.getValue();
            EmojiOrder.MajorGroup majorGroup = emojiOrder.getMajorGroup(list); // majorGroupings.get(list.iterator().next());
            if (lastMajorGroup != majorGroup) {
                if (out != null) {
                    out.println("# total:\t\t" + total);
                    out.println("# total base:\t\t" + totalNoMod + "\tw/o modifiers");
                    System.out.println("\t" + lastMajorGroup + "\t" + total + "\t" + totalNoMod + "\t" + totalNoModNoSign);
                    total = 0;
                    totalNoMod = 0;
                    totalNoModNoSign = 0;
                    out.close();
                }
                String filename = majorGroup.toString().toLowerCase(Locale.ENGLISH).replaceAll("[^a-z]+", "_");
                out = new TempPrintWriter(Emoji.TR51_INTERNAL_DIR + "keyboard", filename + ".csv");
                out.println("# Hex code points, characters, name");
                lastMajorGroup = majorGroup;
            }
            if (list.contains("👮")) {
                int debug = 0;
            }
            if (spreadsheet) {
                LinkedHashSet<String> filtered = new LinkedHashSet<>(list);
                if (retain != null) {
                    filtered.retainAll(retain);
                }
                if (!filtered.isEmpty()) {
                    out.println("# sublabel=" + label + ", size=" + filtered.size() + ", list=[" + CollectionUtilities.join(filtered, " ") + "]");
                    for (String cp_raw : filtered) {
                        String cp = emojiOrder.emojiData.addEmojiVariants(cp_raw, Emoji.EMOJI_VARIANT, VariantHandling.all);
                        out.println("U+" + Utility.hex(cp,"U+") 
                                + "," + cp 
//                                + "," + Emoji.getNewest(cp).getShortName() 
                                + "," + Emoji.getName(cp, false, null));
                        ++total;
                        if (!EmojiData.MODIFIERS.containsSome(cp)) {
                            ++totalNoMod;
                            if (!Emoji.GENDER_MARKERS.containsSome(cp)) {
                                ++totalNoModNoSign;
                            }
                        }
                    }
                }
                allCharacters.add(filtered);
                continue;
            }
            //                for (String cp : list) {
            //                    if (emojiOrder.emojiData.getModifierSequences().contains(cp)) {
            //                        continue;
            //                    }
            //                    ++i;
            //                    if (majorGroup != lastMajorGroup) {
            //                        if (lastMajorGroup != null) {
            //                            out.println("# " + lastMajorGroup + " count:\t" + i);
            //                            i = 0;
            //                        }
            //                        out.println("####################");
            //                        out.println("# " + majorGroup);
            //                        out.println("####################");
            //                        lastMajorGroup = majorGroup;
            //                    }
            //                    if (isFirst) {
            //                        out.println("# " + label);
            //                        isFirst = false;
            //                    }
            //                    out.println(Utility.hex(cp) 
            //                            + "," + cp 
            //                            + "," + Emoji.getNewest(cp).getShortName() 
            //                            + "," + Emoji.getName(cp, false, null));
            //                }
            if (spreadsheet) {
                if (!allCharacters.equals(new UnicodeSet().addAll(retain))) {
                    out.println(
                            retain.size() 
                            + "\t" + allCharacters.size() 
                            + "\t" + new UnicodeSet().addAll(retain).removeAll(allCharacters)
                            + "\t" + new UnicodeSet().addAll(allCharacters).removeAll(retain)
                            );
                }
                //                } else {
                //                    out.println("# " + lastMajorGroup + " count:\t" + i);
            }
        }
        if (out != null) {
            out.println("# total:\t\t" + total);
            out.println("# total base:\t\t" + totalNoMod + "\tw/o modifiers");
            System.out.println("\t" + lastMajorGroup + "\t" + total + "\t" + totalNoMod + "\t" + totalNoModNoSign);
            out.close();
        }
    }
}
