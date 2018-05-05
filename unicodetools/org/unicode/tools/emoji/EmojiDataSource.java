package org.unicode.tools.emoji;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UTF16;

public interface EmojiDataSource {

    public UnicodeSet getEmojiComponents();
    public UnicodeSet getSingletonsWithDefectives();
    public UnicodeSet getEmojiPresentationSet();
    public UnicodeSet getModifierBases();
    public UnicodeSet getExtendedPictographic();
    public UnicodeSet getTagSequences();
    public UnicodeSet getModifierSequences();
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
}

