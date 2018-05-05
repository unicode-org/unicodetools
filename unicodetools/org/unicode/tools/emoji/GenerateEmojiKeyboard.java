package org.unicode.tools.emoji;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;

import org.unicode.cldr.util.Counter;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.props.VersionToAge;
import org.unicode.text.utility.Utility;
import org.unicode.cldr.util.Tabber;
import org.unicode.tools.emoji.EmojiData.VariantFactory;
import org.unicode.tools.emoji.EmojiOrder.MajorGroup;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.VersionInfo;

public class GenerateEmojiKeyboard {
    private static final VersionInfo V6 = VersionInfo.getInstance(6,0);
    private static final VersionInfo V61 = VersionInfo.getInstance(6,1);
    private static final VersionInfo V62 = VersionInfo.getInstance(6,2);
    private static final VersionInfo V63 = VersionInfo.getInstance(6,3);
    private static final VersionInfo V7 = VersionInfo.getInstance(7,0);
    private static final IndexUnicodeProperties IUP7 = IndexUnicodeProperties.make(V7);

    enum Target {csv, propFile, summary}

    public static void main(String[] args) throws Exception {
        System.out.println("\t\tSingletons\tCount w/o Skintones\t-Typical Dups\tCount Total\t-Typical Dups");
        if (true) {
            for (VersionInfo version : Arrays.asList(V6, V61, V7)) {
                GenerateEmojiKeyboard.getCounts(version, false);
            }

            for (VersionInfo version : Arrays.asList(Emoji.VERSION2, Emoji.VERSION3, Emoji.VERSION4, Emoji.VERSION5, Emoji.VERSION11)) {
                GenerateEmojiKeyboard.getCounts(version, true);
            }
        }
        System.out.println();
        GenerateEmojiKeyboard.showLines(EmojiOrder.STD_ORDER, EmojiOrder.STD_ORDER.emojiData.getSortingChars(), Target.propFile, Emoji.DATA_DIR_PRODUCTION);
        GenerateEmojiKeyboard.showLines(EmojiOrder.STD_ORDER, EmojiOrder.STD_ORDER.emojiData.getSortingChars(), Target.csv, Emoji.TR51_INTERNAL_DIR + "keyboard");
        //        boolean foo2 = EmojiData.EMOJI_DATA.getChars().contains(EmojiData.SAMPLE_WITHOUT_TRAILING_EVS);
        //        Set<String> foo = EmojiOrder.sort(EmojiOrder.STD_ORDER.codepointCompare, 
        //                EmojiData.EMOJI_DATA.getChars());
        //
        //        showDiff(EmojiData.EMOJI_DATA.getChars(), EmojiOrder.STD_ORDER.emojiData.getSortingChars());
    }

    private static void getCounts(VersionInfo version, boolean emojiVersion) {
        UnicodeSet setToList;
        String date;
        EmojiData emojiData;
        if (!emojiVersion) {
            emojiData = EmojiData.of(Emoji.VERSION2);
            setToList = emojiData.getAllEmojiWithoutDefectives();
            setToList = new UnicodeSet(IndexUnicodeProperties.make(version)
                    .loadEnumSet(UcdProperty.General_Category, UcdPropertyValues.General_Category_Values.Unassigned)).complement()
                    .retainAll(setToList);
            date = "U" + version.getVersionString(2, 2) + " / " + VersionToAge.ucd.getYear(Age_Values.forName(version.getVersionString(2, 2))) + "";
            System.out.println("\nUnicode Version Year:\t" + date);
        } else {
            emojiData = EmojiData.of(version);
            if (!emojiData.getVersion().equals(version)) {
                throw new IllegalAccessError();
            }
            setToList = emojiData.getAllEmojiWithoutDefectives();
            date = "E" + version.getVersionString(2, 2) + " / " + Emoji.EMOJI_TO_DATE.get(version);
            System.out.println("\nEmoji Version Date:\t" + date);
        }

        Counter<EmojiOrder.MajorGroup> totalSingletons = new Counter<>();
        Counter<EmojiOrder.MajorGroup> totalsWithoutModifiers = new Counter<>();
        Counter<EmojiOrder.MajorGroup> totalDuplicatesWithoutModifiers = new Counter<>();
        Counter<EmojiOrder.MajorGroup> totals = new Counter<>();
        Counter<EmojiOrder.MajorGroup> totalDuplicates = new Counter<>();
        for (String emoji : setToList) {
            boolean isDup = EmojiData.isTypicallyDuplicateGroup(emoji) 
                    || emojiData.isTypicallyDuplicateSign(emoji) 
                    || EmojiData.MODIFIERS.contains(emoji);

            MajorGroup majorGroup = EmojiOrder.STD_ORDER.majorGroupings.get(emoji);

            if (isSingleCodePoint(emoji)) {
                totalSingletons.add(majorGroup, 1);
            }

            if (!EmojiData.MODIFIERS.containsSome(emoji)) {
                totalsWithoutModifiers.add(majorGroup, 1);
                if (isDup) {
                    totalDuplicatesWithoutModifiers.add(majorGroup, -1);
                }
            }
            totals.add(majorGroup, 1);
            if (isDup) {
                totalDuplicates.add(majorGroup, -1);
            }
        }
        for (MajorGroup group : EmojiOrder.MajorGroup.values()) {
            System.out.println(group.toPlainString() 
                    + "\t" + date
                    + "\t" + totalSingletons.get(group)
                    //					+ "\t" + totalsWithoutModifiers.get(group) 
                    //					+ "\t" + totalDuplicatesWithoutModifiers.get(group)
                    + "\t" + totals.get(group) 
                    + "\t" + totalDuplicates.get(group)
                    + "\t" + (totals.get(group)+totalDuplicates.get(group))
                    );
        }
        System.out.println("TOTAL:"
                + "\t" + date  
                + "\t" + totalSingletons.getTotal()
                //                + "\t" + totalsWithoutModifiers.getTotal() 
                //                + "\t" + (totalsWithoutModifiers.getTotal()+totalDuplicatesWithoutModifiers.getTotal())
                + "\t" + totals.getTotal() 
                + "\t" + totalDuplicates.getTotal()
                + "\t" + (totals.getTotal()+totalDuplicates.getTotal()));
    }

    private static boolean isSingleCodePoint(String emoji) {
        int cp = emoji.codePointAt(0);
        return (UCharacter.charCount(cp) == emoji.length());
    }

    private static void showDiff(UnicodeSet chars, UnicodeSet sortingChars) {
        System.out.println(new UnicodeSet(chars).removeAll(sortingChars));
        System.out.println(new UnicodeSet(sortingChars).removeAll(chars));
    }

    static class Totals {
        int total = 0;
        int totalNoMod = 0;
        int totalNoModNoSign = 0;

        private void add(String cp) {
            Totals totals = this;
            ++totals.total;
            if (!EmojiData.MODIFIERS.containsSome(cp)) {
                ++totals.totalNoMod;
                if (!Emoji.GENDER_MARKERS.containsSome(cp)) {
                    ++totals.totalNoModNoSign;
                }
            }
        }

        private void show(TempPrintWriter out, EmojiOrder.MajorGroup lastMajorGroup) throws IOException {
            Totals totals = this;
            out.println("");
            out.println("# " + lastMajorGroup.toPlainString() + " subtotal:\t\t" + totals.total);
            out.println("# " + lastMajorGroup.toPlainString() + " subtotal:\t\t" + totals.totalNoMod + "\tw/o modifiers");
            System.out.println("\t" + lastMajorGroup.toPlainString() + "\t" + totals.total + "\t" + totals.totalNoMod + "\t" + totals.totalNoModNoSign);
            totals.total = 0;
            totals.totalNoMod = 0;
            totals.totalNoModNoSign = 0;
        }
    }

    public static void showLines(EmojiOrder emojiOrder, UnicodeSet charactersToInclude, Target target, String directory) throws IOException {
        Set<String> retain = ImmutableSet.copyOf(charactersToInclude.addAllTo(new HashSet<String>()));

        UnicodeSet charactersNotShown = new UnicodeSet().addAll(retain);
        EmojiOrder.MajorGroup lastMajorGroup = null;
        TempPrintWriter out = null;
        Totals totals = new Totals();

        int maxField1 = 0;
        int maxField2 = 10;
        for (String cp : retain) {
            String hcp1 = Utility.hex(cp, " ");
            if (hcp1.length() > maxField1) {
                maxField1 = hcp1.length();
            }
        }
        Tabber tabber = new Tabber.MonoTabber()
                .add(maxField1+1, Tabber.LEFT)
                .add("non-fully-qualified".length()+3, Tabber.LEFT);
        ;

        for (Entry<String, Set<String>> labelToSet : emojiOrder.orderingToCharacters.keyValuesSet()) {
            final String label = labelToSet.getKey();
            final Set<String> list = labelToSet.getValue();
            if (list.contains("👮")) {
                int debug = 0;
            }
            EmojiOrder.MajorGroup majorGroup = emojiOrder.getMajorGroup(list); // majorGroupings.get(list.iterator().next());
            if (lastMajorGroup != majorGroup) {
                if (out != null) {
                    totals.show(out, lastMajorGroup);
                    if (target == Target.csv){ 
                        out.println("\n#EOF");
                        out.close();
                        out = null;
                    }
                }
                if (out == null) {
                    String filename = target == Target.csv ? majorGroup.toPlainString().toLowerCase(Locale.ENGLISH).replaceAll("[^a-z]+", "_") : "emoji-test";
                    final String suffix = target == Target.csv ? ".csv" : ".txt";
                    out = new TempPrintWriter(directory, filename + suffix);
                    if (target == Target.csv) {
                        out.println("# " + filename);
                        out.println("\n# Format\n"
                                + "#   Hex code points, characters, name");
                    } else {
                        out.println(Utility.getBaseDataHeader(filename, 51, "Emoji Keyboard/Display Test Data", Emoji.VERSION_STRING));
                        out.println("# This file provides data for testing which emoji forms should be in keyboards and which should also be displayed/processed.\n"
                                + "# Format\n"
                                + "#   Code points; status # emoji name\n"
                                + "#     Status\n"
                                + "#       fully-qualified — see “Emoji Implementation Notes” in UTS #51\n"
                                + "#       non-fully-qualified — see “Emoji Implementation Notes” in UTS #51"
                                );
                        out.println("# Notes:\n"
                                + "#   • This currently omits the 12 keycap bases, the 5 modifier characters, and 26 singleton Regional Indicator characters\n"
                                + "#   • The file is in CLDR order, not codepoint order. This is recommended (but not required!) for keyboard palettes.\n"
                                + "#   • The groups and subgroups are purely illustrative. See the Emoji Order chart for more information."
                                );
                    }
                }
                if (target == Target.propFile) {
                    out.println("\n# group: " + majorGroup.toPlainString());
                }
                lastMajorGroup = majorGroup;
            }
            LinkedHashSet<String> filtered = new LinkedHashSet<>(list);
            if (retain != null) {
                filtered.retainAll(retain);
            }
            if (filtered.isEmpty()) {
                continue;
            }
            out.println("\n# subgroup: " + label); //  + "; size: " + filtered.size() + "; list: [" + CollectionUtilities.join(filtered, " ") + "]\n");

            VariantFactory vf = emojiOrder.emojiData.new VariantFactory();
            for (String cp_raw : filtered) {
                vf.set(cp_raw);
                for (String cp : vf.getCombinations()) {
                    charactersNotShown.remove(cp);
                    boolean isFull = vf.getFull().equals(cp);

                    switch(target) {
                    case csv:
                        if (isFull) {
                            out.println("U+" + Utility.hex(cp,"U+") 
                            + "," + cp 
                            + "," + EmojiData.EMOJI_DATA.getName(cp));
                        }
                        break;
                    case propFile:
                        out.println(tabber.process(Utility.hex(cp) + "\t; " 
                                + (isFull ? "fully-qualified" : "non-fully-qualified")
                                + "\t# " + cp + " " + EmojiData.EMOJI_DATA.getName(cp)));
                        break;
                    }
                    totals.add(cp);
                }
            }

            //          allCharacters.add(filtered);
            //          if (!allCharacters.equals(new UnicodeSet().addAll(retain))) {
            //              out.println(
            //                      retain.size() 
            //                      + "\t" + allCharacters.size() 
            //                      + "\t" + new UnicodeSet().addAll(retain).removeAll(allCharacters)
            //                      + "\t" + new UnicodeSet().addAll(allCharacters).removeAll(retain)
            //                      );
            //          }
        }
        if (out != null) {
            totals.show(out, lastMajorGroup);
            out.println("\n#EOF");
            out.close();
        }
        if (charactersNotShown.size() != 0) {
            System.err.println("Missing characters: " + charactersNotShown.size() + "\t" + charactersNotShown.toPattern(false));
        }
    }

    //	static Splitter vsSplitter = Splitter.on(Emoji.EMOJI_VARIANT);

    /** Show all of the combinations with VS, except for all VS characters.
     */
    //	private static void showWithoutVS(TempPrintWriter out, Tabber tabber, String cp, UnicodeSet charactersNotShown) throws IOException {
    //		if (!cp.contains(Emoji.JOINER_STRING)) {
    //			return;
    //		}
    //		int pos = cp.indexOf(Emoji.EMOJI_VARIANT);
    //		if (pos < 0) {
    //			return;
    //		}
    //		String name = EmojiData.EMOJI_DATA.getName(cp, false, CandidateData.getInstance());
    //
    //		final List<String> parts = vsSplitter.splitToList(cp);
    //		final int size = parts.size();
    //		if (size > 2) {
    //			int debug = 0;
    //		}
    //		int count = (1 << (size-1)) - 1; // 3 parts => 100 => 11
    //		for (int bitmap = 0; bitmap < count; ++bitmap) {
    //			String temp = parts.get(0);
    //			for (int rest = 0; rest < size - 1; ++rest) {
    //				if ((bitmap & (1<<rest)) != 0) {
    //					temp += Emoji.EMOJI_VARIANT_STRING;
    //				}
    //				temp += parts.get(rest+1);
    //			}
    //			out.println(tabber.process(Utility.hex(temp) + "\t; " 
    //					+ "non-fully-qualified"
    //					+ "\t# " + temp + " " + name));
    //			charactersNotShown.remove(temp);
    //		}
    //	}
}
