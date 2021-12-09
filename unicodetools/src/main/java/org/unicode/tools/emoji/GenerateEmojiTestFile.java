package org.unicode.tools.emoji;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;

import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Tabber;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.props.VersionToAge;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.EmojiData.VariantFactory;
import org.unicode.tools.emoji.EmojiData.VariantStatus;
import org.unicode.tools.emoji.EmojiOrder.MajorGroup;

import com.google.common.collect.ImmutableSet;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUException;
import com.ibm.icu.util.VersionInfo;

public class GenerateEmojiTestFile {

    private static final EmojiOrder GEN_ORDER = EmojiOrder.BETA_ORDER;

    enum Target {csv, propFile, summary}

    public static void main(String[] args) throws Exception {
        System.out.println("\t\tSingletons\tCount w/o Skintones\t-Typical Dups\tCount Total\t-Typical Dups");
        if (true) {
            for (VersionInfo version : Arrays.asList(Emoji.VERSION5, Emoji.VERSION11)) {
                GenerateEmojiTestFile.getCounts(version, false);
            }

            for (VersionInfo version : Arrays.asList(Emoji.VERSION2, Emoji.VERSION3, Emoji.VERSION4, Emoji.VERSION5, Emoji.VERSION11)) {
                GenerateEmojiTestFile.getCounts(version, true);
            }
        }
        System.out.println();
        UnicodeSet sortingChars = GEN_ORDER.emojiData.getSortingChars();
        if (sortingChars.contains(EmojiData.MODIFIERS.getRangeStart(0))) {
            System.out.println("has mods");
        }
        if (sortingChars.contains(Emoji.HAIR_PIECES.getRangeStart(0))) {
            System.out.println("has hair");
        }

        GenerateEmojiTestFile.showLines(GEN_ORDER, sortingChars, Target.propFile, GenerateEmojiData.OUTPUT_DIR);
        GenerateEmojiTestFile.showLines(GEN_ORDER, sortingChars, Target.csv, Emoji.TR51_INTERNAL_DIR + "keyboard");
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
        String versionString = version.getVersionString(2, 2);
        if (!emojiVersion) {
            emojiData = EmojiData.of(Emoji.VERSION2);
            setToList = emojiData.getAllEmojiWithoutDefectives();
            setToList = new UnicodeSet(IndexUnicodeProperties.make(version)
                    .loadEnumSet(UcdProperty.General_Category, UcdPropertyValues.General_Category_Values.Unassigned)).complement()
                    .retainAll(setToList);
            date = "U" + versionString + " / " + VersionToAge.ucd.getYear(Age_Values.forName(versionString)) + "";
            System.out.println("\nUnicode Version Year:\t" + date);
        } else {
            emojiData = EmojiData.of(version);
            if (!emojiData.getVersion().equals(version)) {
                throw new IllegalAccessError();
            }
            setToList = emojiData.getAllEmojiWithoutDefectives();
            date = "E" + versionString + " / " + Emoji.EMOJI_TO_DATE.get(version);
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

            MajorGroup majorGroup = GEN_ORDER.majorGroupings.get(emoji);

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
                    //                    + "\t" + totalsWithoutModifiers.get(group) 
                    //                    + "\t" + totalDuplicatesWithoutModifiers.get(group)
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
        for (String cp : retain) {
            String hcp1 = Utility.hex(cp, " ");
            if (hcp1.length() > maxField1) {
                maxField1 = hcp1.length();
            }
        }
        int maxField2 = 0;
        for (VariantStatus s : VariantStatus.values()) {
            if (maxField2 < s.name.length()) {
                maxField2 = s.name.length();
            }
        }
        Tabber tabber = new Tabber.MonoTabber()
                .add(maxField1+1, Tabber.LEFT)
                .add(maxField2+3, Tabber.LEFT);
        ;

        Counter<VariantStatus> vsCount = new Counter<>();

        for (Entry<String, Collection<String>> labelToSet : emojiOrder.orderingToCharacters.asMap().entrySet()) {
            final String label = labelToSet.getKey();
            final Collection<String> list = labelToSet.getValue();
            if (list.contains("🏻") 
                    || list.contains("🦲")) {
                int debug = 0;
            }
            EmojiOrder.MajorGroup majorGroup = emojiOrder.getMajorGroupFromCategory(label); // majorGroupings.get(list.iterator().next());
            if (majorGroup == null) {
                throw new ICUException("Major group not found for «" + label + "»");
            }
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
                        out.println(
                                "# This file provides data for testing which emoji forms should be in keyboards and which should also be displayed/processed.\n"
                                        + "# Format: code points; status # emoji name\n"
                                        + "#     Code points — list of one or more hex code points, separated by spaces\n"
                                        + "#     Status\n"
                                        + "#       component           — an Emoji_Component,\n"
                                        + "#                             excluding Regional_Indicators, ASCII, and non-Emoji.\n"
                                        + "#       fully-qualified     — a fully-qualified emoji (see ED-18 in UTS #51),\n"
                                        + "#                             excluding Emoji_Component\n"
                                        + "#       minimally-qualified — a minimally-qualified emoji (see ED-18a in UTS #51)\n"
                                        + "#       unqualified         — a unqualified emoji (See ED-19 in UTS #51)\n"
                                        + "# Notes:\n"
                                        + "#   • This includes the emoji components that need emoji presentation (skin tone and hair)\n"
                                        + "#     when isolated, but omits the components that need not have an emoji\n"
                                        + "#     presentation when isolated.\n"
                                        + "#   • The RGI set is covered by the listed fully-qualified emoji. \n"
                                        + "#   • The listed minimally-qualified and unqualified cover all cases where an\n"
                                        + "#     element of the RGI set is missing one or more emoji presentation selectors.\n"
                                        + "#   • The file is in CLDR order, not codepoint order. This is recommended (but not required!) for keyboard palettes.\n"
                                        + "#   • The groups and subgroups are illustrative. See the Emoji Order chart for more information.\n");
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
                    VariantStatus variantStatus = emojiOrder.emojiData.getVariantStatus(cp);
            String ageDisplay = "E" + BirthInfo.getVersionInfo(cp).getVersionString(2, 2);

                    switch(target) {
                    case csv:
                        if (variantStatus == VariantStatus.full) {
                            out.println("U+" + Utility.hex(cp,"U+") 
                            + "," + cp 
                            + "," + ageDisplay
                            + "," + EmojiData.EMOJI_DATA.getName(cp)
                            );
                        }
                        break;
                    case propFile:
                        vsCount.add(variantStatus, 1);
                        out.println(tabber.process(Utility.hex(cp) 
                                + "\t; " + variantStatus.name
                                + "\t# " + cp + " " + ageDisplay + " " + EmojiData.EMOJI_DATA.getName(cp)));
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
            if (vsCount.size() != 0) {
                out.println("\n# Status Counts");
                for (VariantStatus vs : VariantStatus.values()) {
                    out.println("# " + vs.name + " : " + vsCount.get(vs));
                }
            }
            out.println("\n#EOF");
            out.close();
        }
        if (charactersNotShown.size() != 0) {
            System.err.println("Missing characters: " + charactersNotShown.size() + "\t" + charactersNotShown.toPattern(false));
        }
    }

    //    static Splitter vsSplitter = Splitter.on(Emoji.EMOJI_VARIANT);

    /** Show all of the combinations with VS, except for all VS characters.
     */
    //    private static void showWithoutVS(TempPrintWriter out, Tabber tabber, String cp, UnicodeSet charactersNotShown) throws IOException {
    //        if (!cp.contains(Emoji.JOINER_STRING)) {
    //            return;
    //        }
    //        int pos = cp.indexOf(Emoji.EMOJI_VARIANT);
    //        if (pos < 0) {
    //            return;
    //        }
    //        String name = EmojiData.EMOJI_DATA.getName(cp, false, CandidateData.getInstance());
    //
    //        final List<String> parts = vsSplitter.splitToList(cp);
    //        final int size = parts.size();
    //        if (size > 2) {
    //            int debug = 0;
    //        }
    //        int count = (1 << (size-1)) - 1; // 3 parts => 100 => 11
    //        for (int bitmap = 0; bitmap < count; ++bitmap) {
    //            String temp = parts.get(0);
    //            for (int rest = 0; rest < size - 1; ++rest) {
    //                if ((bitmap & (1<<rest)) != 0) {
    //                    temp += Emoji.EMOJI_VARIANT_STRING;
    //                }
    //                temp += parts.get(rest+1);
    //            }
    //            out.println(tabber.process(Utility.hex(temp) + "\t; " 
    //                    + "non-fully-qualified"
    //                    + "\t# " + temp + " " + name));
    //            charactersNotShown.remove(temp);
    //        }
    //    }
}
