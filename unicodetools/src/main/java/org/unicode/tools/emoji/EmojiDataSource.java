package org.unicode.tools.emoji;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import org.unicode.tools.emoji.Emoji.Qualified;

public interface EmojiDataSource {

    public UnicodeSet getEmojiComponents();

    public UnicodeSet getSingletonsWithDefectives();

    public UnicodeSet getEmojiPresentationSet();

    public UnicodeSet getModifierBases();

    public UnicodeSet getExtendedPictographic();

    public UnicodeSet getTagSequences();

    public UnicodeSet getModifierSequences();

    public UnicodeSet getKeycapSequences();

    public UnicodeSet getFlagSequences();

    public UnicodeSet getZwjSequencesNormal();

    public UnicodeSet getEmojiWithVariants();

    public UnicodeSet getAllEmojiWithoutDefectives();

    public UnicodeSet getTextPresentationSet();

    public UnicodeSet getAllEmojiWithDefectives();

    public UnicodeSet getGenderBases();

    public UnicodeSet getTakesSign();

    public UnicodeSet getSingletonsWithoutDefectives();

    public String getName(String s);

    public default String getName(int codepoint) {
        return getName(UTF16.valueOf(codepoint));
    }

    public UnicodeMap<String> getRawNames();

    public default UnicodeSet getBasicSequences() {
        UnicodeSet result = new UnicodeSet();
        for (String s : getSingletonsWithDefectives()) {
            if (Emoji.KEYCAP_BASE.contains(s) || Emoji.REGIONAL_INDICATORS.contains(s)) {
                continue;
            }
            if (getEmojiPresentationSet().contains(s)) {
                result.add(s);
            } else {
                result.add(s + Emoji.EMOJI_VARIANT);
            }
        }
        return result.freeze();
    }

    public default UnicodeSet getEmojiForSortRules() {
        return new UnicodeSet()
                .addAll(getAllEmojiWithoutDefectives())
                .removeAll(Emoji.DEFECTIVE)
                .addAll(getZwjSequencesNormal())
                .addAll(getKeycapSequences());
    }

    public String addEmojiVariants(String s1);

    public String getVersionString();

    public String getPlainVersion();

    public UnicodeSet getExplicitGender();

    public UnicodeSet getMultiPersonGroupings();

    public UnicodeSet getModifierBasesRgi();

    public UnicodeSet getAllEmojiWithoutDefectivesOrModifiers();

    public String addEmojiVariants(String s1, Qualified qualified);
}
