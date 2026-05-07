package org.unicode.tools.emoji;

import com.google.common.collect.ImmutableSet;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUException;
import com.ibm.icu.util.VersionInfo;
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
import org.unicode.text.utility.DiffingPrintWriter;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.EmojiData.VariantFactory;
import org.unicode.tools.emoji.EmojiData.VariantStatus;
import org.unicode.tools.emoji.EmojiOrder.MajorGroup;

public class GenerateEmojiTestFile {

    private static final EmojiOrder GEN_ORDER = EmojiOrder.BETA_ORDER;

    enum Target {
        csv,
        propFile,
        summary
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

        private void show(DiffingPrintWriter out, EmojiOrder.MajorGroup lastMajorGroup)
                throws IOException {
            Totals totals = this;
            out.println("");
            out.println("# " + lastMajorGroup.toPlainString() + " subtotal:\t\t" + totals.total);
            out.println(
                    "# "
                            + lastMajorGroup.toPlainString()
                            + " subtotal:\t\t"
                            + totals.totalNoMod
                            + "\tw/o modifiers");
            System.out.println(
                    "\t"
                            + lastMajorGroup.toPlainString()
                            + "\t"
                            + totals.total
                            + "\t"
                            + totals.totalNoMod
                            + "\t"
                            + totals.totalNoModNoSign);
            totals.total = 0;
            totals.totalNoMod = 0;
            totals.totalNoModNoSign = 0;
        }
    }

    public static void showLines(
            EmojiOrder emojiOrder, UnicodeSet charactersToInclude, Target target, String directory)
            throws IOException {
        Set<String> retain =
                ImmutableSet.copyOf(charactersToInclude.addAllTo(new HashSet<String>()));

        UnicodeSet charactersNotShown = new UnicodeSet().addAll(retain);
        EmojiOrder.MajorGroup lastMajorGroup = null;
        DiffingPrintWriter out = null;
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
        Tabber tabber =
                new Tabber.MonoTabber()
                        .add(maxField1 + 1, Tabber.LEFT)
                        .add(maxField2 + 3, Tabber.LEFT);
        ;

        Counter<VariantStatus> vsCount = new Counter<>();

        for (Entry<String, Collection<String>> labelToSet :
                emojiOrder.orderingToCharacters.asMap().entrySet()) {
            final String label = labelToSet.getKey();
            final Collection<String> list = labelToSet.getValue();
            if (list.contains("🏻") || list.contains("🦲")) {
                int debug = 0;
            }
            EmojiOrder.MajorGroup majorGroup =
                    emojiOrder.getMajorGroupFromCategory(
                            label); // majorGroupings.get(list.iterator().next());
            if (majorGroup == null) {
                throw new ICUException("Major group not found for «" + label + "»");
            }
            if (lastMajorGroup != majorGroup) {
                if (out != null) {
                    totals.show(out, lastMajorGroup);
                    if (target == Target.csv) {
                        out.println("\n#EOF");
                        out.close();
                        out = null;
                    }
                }
                if (out == null) {
                    String filename =
                            target == Target.csv
                                    ? majorGroup
                                            .toPlainString()
                                            .toLowerCase(Locale.ENGLISH)
                                            .replaceAll("[^a-z]+", "_")
                                    : "emoji-test";
                    final String suffix = target == Target.csv ? ".csv" : ".txt";
                    out = new DiffingPrintWriter(directory, filename + suffix);
                    if (target == Target.csv) {
                        out.println("# " + filename);
                        out.println("\n# Format\n" + "#   Hex code points, characters, name");
                    } else {
                        out.println(
                                Utility.getBaseDataHeader(
                                        filename,
                                        51,
                                        "Emoji Keyboard/Display Test Data",
                                        Emoji.VERSION_STRING));
                        out.println(
                                "# This file provides data for testing which emoji forms should be in keyboards and which should also be displayed/processed.\n"
                                        + "# Format: code points; status # emoji name\n"
                                        + "#     Code points — list of one or more hex code points, separated by spaces\n"
                                        + "#     Status\n"
                                        + "#       component           — an Emoji_Component,\n"
                                        + "#                             excluding Regional_Indicator, ASCII, and non-Emoji\n"
                                        + "#       fully-qualified     — a fully-qualified emoji (see ED-18 in UTS #51),\n"
                                        + "#                             excluding Emoji_Component\n"
                                        + "#       minimally-qualified — a minimally-qualified emoji (see ED-18a in UTS #51)\n"
                                        + "#       unqualified         — an unqualified emoji (see ED-19 in UTS #51)\n"
                                        + "# Notes:\n"
                                        + "#   • A mapping of these status values to RGI_Emoji_Qualification property values\n"
                                        + "#     is given by ED-28 in UTS #51.\n"
                                        + "#   • This includes the emoji components that need emoji presentation (skin tone and hair)\n"
                                        + "#     when isolated, but omits the components that need not have an emoji\n"
                                        + "#     presentation when isolated. See ED-20 in UTS #51 for further information.\n"
                                        + "#   • The RGI emoji set corresponds to the RGI_Emoji property and contains the same sequences\n"
                                        + "#     as the union of the sets of component and fully-qualified sequences in this file.\n"
                                        + "#     See ED-27 in UTS #51 for further information.\n"
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
            out.println(
                    "\n# subgroup: " + label); //  + "; size: " + filtered.size() + "; list: [" +
            // CollectionUtilities.join(filtered, " ") + "]\n");

            VariantFactory vf = emojiOrder.emojiData.new VariantFactory();
            for (String cp_raw : filtered) {
                vf.set(cp_raw);
                for (String cp : vf.getCombinations()) {
                    charactersNotShown.remove(cp);
                    VariantStatus variantStatus = emojiOrder.emojiData.getVariantStatus(cp);
                    String ageDisplay = "E" + BirthInfo.getVersionInfo(cp).getVersionString(2, 2);

                    switch (target) {
                        case csv:
                            if (variantStatus == VariantStatus.full) {
                                out.println(
                                        "U+"
                                                + Utility.hex(cp, "U+")
                                                + ","
                                                + cp
                                                + ","
                                                + ageDisplay
                                                + ","
                                                + EmojiData.EMOJI_DATA.getName(cp));
                            }
                            break;
                        case propFile:
                            vsCount.add(variantStatus, 1);
                            out.println(
                                    tabber.process(
                                            Utility.hex(cp)
                                                    + "\t; "
                                                    + variantStatus.name
                                                    + "\t# "
                                                    + cp
                                                    + " "
                                                    + ageDisplay
                                                    + " "
                                                    + EmojiData.EMOJI_DATA.getName(cp)));
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
            //                      + "\t" + new
            // UnicodeSet().addAll(retain).removeAll(allCharacters)
            //                      + "\t" + new
            // UnicodeSet().addAll(allCharacters).removeAll(retain)
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
            System.err.println(
                    "Missing characters: "
                            + charactersNotShown.size()
                            + "\t"
                            + charactersNotShown.toPattern(false));
        }
    }
}
