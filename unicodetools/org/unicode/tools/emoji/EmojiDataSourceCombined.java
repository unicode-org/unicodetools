package org.unicode.tools.emoji;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;

public class EmojiDataSourceCombined implements EmojiDataSource {

    private static final EmojiData emojiData = EmojiData.EMOJI_DATA;
    private static final CandidateData candidates = CandidateData.getInstance();
    static final EmojiDataSource EMOJI_DATA = new EmojiDataSourceCombined();

    static final UnicodeSet add(UnicodeSet base, UnicodeSet candidates) {
        return !Emoji.IS_BETA || candidates.isEmpty() ? base 
                : new UnicodeSet(candidates).addAll(base).freeze();
    }

    static final <T> UnicodeMap<T> add(UnicodeMap<T> base, UnicodeMap<T> candidates) {
        return !Emoji.IS_BETA || candidates.isEmpty() ? base 
                : new UnicodeMap(candidates).putAll(base).freeze();
    }

    @Override
    public UnicodeSet getEmojiComponents() {
        return add(emojiData.getEmojiComponents(),
                candidates.getEmojiComponents());
    }

    @Override
    public UnicodeSet getSingletonsWithDefectives() {
        return add(emojiData.getSingletonsWithDefectives(),
                candidates.getSingletonsWithDefectives());
    }

    @Override
    public UnicodeSet getEmojiPresentationSet() {
        return add(emojiData.getEmojiPresentationSet(),
                candidates.getEmojiPresentationSet());
    }

    @Override
    public UnicodeSet getModifierBases() {
        return add(emojiData.getModifierBases(),
                candidates.getModifierBases());
    }

    @Override
    public UnicodeSet getExtendedPictographic() {
        return add(emojiData.getExtendedPictographic(),
                candidates.getExtendedPictographic());
    }

    @Override
    public UnicodeSet getTagSequences() {
        return add(emojiData.getTagSequences(),
                candidates.getTagSequences());
    }

    @Override
    public UnicodeSet getModifierSequences() {
        return add(emojiData.getModifierSequences(),
                candidates.getModifierSequences());
    }

    @Override
    public UnicodeSet getFlagSequences() {
        return add(emojiData.getFlagSequences(),
                candidates.getFlagSequences());
    }

    @Override
    public UnicodeSet getZwjSequencesNormal() {
        return add(emojiData.getZwjSequencesNormal(),
                candidates.getZwjSequencesNormal());
    }

    @Override
    public UnicodeSet getEmojiWithVariants() {
        return add(emojiData.getEmojiWithVariants(),
                candidates.getEmojiWithVariants());
    }

    @Override
    public UnicodeSet getAllEmojiWithoutDefectives() {
        return add(emojiData.getAllEmojiWithoutDefectives(),
                candidates.getAllEmojiWithoutDefectives());
    }

    @Override
    public UnicodeSet getTextPresentationSet() {
        return add(emojiData.getTextPresentationSet(),
                candidates.getTextPresentationSet());
    }

    @Override
    public UnicodeSet getAllEmojiWithDefectives() {
        return add(emojiData.getAllEmojiWithDefectives(),
                candidates.getAllEmojiWithDefectives());
    }

    @Override
    public UnicodeSet getGenderBases() {
        return add(emojiData.getGenderBases(),
                candidates.getGenderBases());
    }

    @Override
    public UnicodeSet getSingletonsWithoutDefectives() {
        return add(emojiData.getSingletonsWithoutDefectives(),
                candidates.getSingletonsWithoutDefectives());
    }

    @Override
    public String getName(String s) {
        String name = Emoji.IS_BETA ? candidates.getName(s) : null;
        return name != null ? name : emojiData.getName(s);
    }

    @Override
    public UnicodeMap<String> getRawNames() {
        return add(emojiData.getRawNames(), candidates.getRawNames());
    }
    
    public static void main(String[] args) {
        UnicodeSet allChars = EMOJI_DATA.getAllEmojiWithDefectives();
        
    }
}