package org.unicode.tools.emoji;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUException;
import com.ibm.icu.util.ICUUncheckedIOException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.EmojiOrder.MajorGroup;

public class EmojiDataSourceCombined implements EmojiDataSource {

    public static final EmojiDataSource EMOJI_DATA = new EmojiDataSourceCombined();
    public static final EmojiDataSource EMOJI_DATA_BETA =
            new EmojiDataSourceCombined(EmojiData.EMOJI_DATA_BETA);

    private static final boolean DEBUG = false;

    private final EmojiData emojiData;
    private final CandidateData candidates;

    public EmojiDataSourceCombined(EmojiData emojiData, CandidateData candidates) {
        this.emojiData = emojiData;
        this.candidates = candidates;
    }

    public EmojiDataSourceCombined(EmojiData emojiData) {
        this(emojiData, CandidateData.getInstance());
    }

    public EmojiDataSourceCombined() {
        this(EmojiData.EMOJI_DATA, CandidateData.getInstance());
    }

    static final UnicodeSet add(UnicodeSet base, UnicodeSet candidates) {
        return candidates.isEmpty()
                ? base
                : base.isEmpty() ? candidates : new UnicodeSet(candidates).addAll(base).freeze();
    }

    static final <T> UnicodeMap<T> add(UnicodeMap<T> base, UnicodeMap<T> candidates) {
        return !Emoji.IS_BETA || candidates.isEmpty()
                ? base
                : new UnicodeMap(candidates).putAll(base).freeze();
    }

    @Override
    public UnicodeSet getEmojiComponents() {
        return add(emojiData.getEmojiComponents(), candidates.getEmojiComponents());
    }

    @Override
    public UnicodeSet getSingletonsWithDefectives() {
        return add(
                emojiData.getSingletonsWithDefectives(), candidates.getSingletonsWithDefectives());
    }

    @Override
    public UnicodeSet getEmojiPresentationSet() {
        return add(emojiData.getEmojiPresentationSet(), candidates.getEmojiPresentationSet());
    }

    @Override
    public UnicodeSet getModifierBases() {
        return add(emojiData.getModifierBases(), candidates.getModifierBases());
    }

    @Override
    public UnicodeSet getModifierBasesRgi() {
        return add(emojiData.getModifierBasesRgi(), candidates.getModifierBasesRgi());
    }

    @Override
    public UnicodeSet getExtendedPictographic() {
        return add(emojiData.getExtendedPictographic(), candidates.getExtendedPictographic());
    }

    @Override
    public UnicodeSet getTagSequences() {
        return add(emojiData.getTagSequences(), candidates.getTagSequences());
    }

    @Override
    public UnicodeSet getModifierSequences() {
        return add(emojiData.getModifierSequences(), candidates.getModifierSequences());
    }

    @Override
    public UnicodeSet getFlagSequences() {
        return add(emojiData.getFlagSequences(), candidates.getFlagSequences());
    }

    @Override
    public UnicodeSet getZwjSequencesNormal() {
        return add(emojiData.getZwjSequencesNormal(), candidates.getZwjSequencesNormal());
    }

    @Override
    public UnicodeSet getEmojiWithVariants() {
        return add(emojiData.getEmojiWithVariants(), candidates.getEmojiWithVariants());
    }

    @Override
    public UnicodeSet getAllEmojiWithoutDefectives() {
        return add(
                emojiData.getAllEmojiWithoutDefectives(),
                candidates.getAllEmojiWithoutDefectives());
    }

    @Override
    public UnicodeSet getAllEmojiWithoutDefectivesOrModifiers() {
        return add(
                emojiData.getAllEmojiWithoutDefectivesOrModifiers(),
                candidates.getAllEmojiWithoutDefectivesOrModifiers());
    }

    @Override
    public UnicodeSet getTextPresentationSet() {
        return add(emojiData.getTextPresentationSet(), candidates.getTextPresentationSet());
    }

    @Override
    public UnicodeSet getAllEmojiWithDefectives() {
        return add(emojiData.getAllEmojiWithDefectives(), candidates.getAllEmojiWithDefectives());
    }

    @Override
    public UnicodeSet getGenderBases() {
        return add(emojiData.getGenderBases(), candidates.getGenderBases());
    }

    @Override
    public UnicodeSet getSingletonsWithoutDefectives() {
        return add(
                emojiData.getSingletonsWithoutDefectives(),
                candidates.getSingletonsWithoutDefectives());
    }

    @Override
    public String getName(String s) {
        String name = emojiData.getName(s);
        if (name == null) {
            name = candidates.getName(s);
        }
        return name;
    }

    @Override
    public UnicodeMap<String> getRawNames() {
        return add(emojiData.getRawNames(), candidates.getRawNames());
    }

    @Override
    public UnicodeSet getTakesSign() {
        return add(emojiData.getTakesSign(), candidates.getTakesSign());
    }

    @Override
    public UnicodeSet getKeycapSequences() {
        return emojiData.getKeycapSequences();
    }

    @Override
    public String addEmojiVariants(String s1) {
        return emojiData.addEmojiVariants(s1);
    }

    @Override
    public String addEmojiVariants(String s1, Emoji.Qualified qualified) {
        return emojiData.addEmojiVariants(s1, qualified);
    }

    @Override
    public String getVersionString() {
        return getPlainVersion() + " + " + candidates.getVersionString();
    }

    @Override
    public String getPlainVersion() {
        return emojiData.getVersionString();
    }

    @Override
    public UnicodeSet getExplicitGender() {
        return add(emojiData.getExplicitGender(), candidates.getExplicitGender());
    }

    @Override
    public UnicodeSet getMultiPersonGroupings() {
        return add(emojiData.getMultiPersonGroupings(), candidates.getMultiPersonGroupings());
    }

    //    public static void main(String[] args) {
    //        UnicodeSet allChars = EMOJI_DATA.getAllEmojiWithDefectives();
    //
    //    }
    /**
     * Created a copy of the input emojiOrdering.txt file but merging in the candidate data
     *
     * @param reformatted
     */
    public void showOrderingInterleaved(TempPrintWriter reformatted) {
        showOrderingInterleaved(30, reformatted);
    }

    public void showOrderingInterleaved(int MAX_PER_LINE, Appendable out) {
        try {
            EmojiOrder emojiOrder = EmojiOrder.STD_ORDER;

            String lastSubgroup = null;
            MajorGroup lastMajor = null;
            int countOnLine = 0;
            final Set<Entry<String, Collection<String>>> keyValuesSet =
                    EmojiOrder.STD_ORDER.orderingToCharacters.asMap().entrySet();

            Set<String> seen = new HashSet<>();

            for (Entry<String, Collection<String>> entry : keyValuesSet) {
                for (String orig : entry.getValue()) {
                    String s = trimMods(orig);
                    if (seen.contains(s)) {
                        continue;
                    }
                    String subgroup = emojiOrder.getCategory(s);
                    if (subgroup == null) {
                        subgroup = candidates.getCategory(s);
                        if (subgroup == null) {
                            throw new ICUException(
                                    "Can't get subgroup for «" + orig + "» " + Utility.hex(orig));
                        }
                    }
                    if (!subgroup.equals(lastSubgroup)) {
                        if (DEBUG)
                            System.out.println(
                                    lastSubgroup
                                            + ";\t"
                                            + Utility.hex(lastSubgroup)
                                            + ";\t«"
                                            + orig
                                            + "»\t"
                                            + Utility.hex(orig));
                        if (countOnLine != 0) {
                            out.append('\n');
                            countOnLine = 0;
                        }
                        MajorGroup major = emojiOrder.getMajorGroupFromCategory(subgroup);
                        if (major == null) {
                            throw new ICUException("Can't get MajorGroup for «" + subgroup + "»");
                        }
                        if (major != lastMajor) {
                            out.append("@@ " + major.toSourceString()).append('\n');
                            lastMajor = major;
                        }
                        out.append("@ " + subgroup).append('\n');
                        lastSubgroup = subgroup;
                    }
                    if (countOnLine != 0) {
                        if (countOnLine >= MAX_PER_LINE || emojiOrder.isFirstInLine(s)) {
                            out.append('\n');
                            countOnLine = 0;
                        } else {
                            out.append(' ');
                        }
                    }
                    out.append(s);
                    seen.add(s);
                    ++countOnLine;
                    Set<String> afters = candidates.getCandidatesAfter(s);
                    if (afters != null) {
                        // TODO sort
                        for (String s2 : afters) {
                            s2 = trimMods(s2);
                            if (seen.contains(s2)) {
                                continue;
                            }
                            if (countOnLine != 0) {
                                if (countOnLine >= MAX_PER_LINE) {
                                    out.append('\n');
                                    countOnLine = 0;
                                } else {
                                    out.append(' ');
                                }
                            }
                            out.append(s2);
                            seen.add(s2);
                            ++countOnLine;
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    private String trimMods(String s) {
        if (EmojiData.MODIFIERS.contains(s)) {
            return s;
        }
        if (s.contains("♂")) {
            int debug = 0;
        }
        String result = EmojiData.EMOJI_DATA_BETA.getBaseRemovingModsGender(s);
        return result;
    }
}
