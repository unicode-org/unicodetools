package org.unicode.tools.emoji;

import com.ibm.icu.util.ULocale;

/**
 * Provide access to the CLDR short name and annotations for emoji characters.
 */
public class EmojiLocaleData {
    private final ULocale locale;

    public EmojiLocaleData(ULocale locale) {
        this.locale = locale;
    }
    
    public ULocale getLocale() {
        return locale;
    }
    
    public static final ULocale[] getAvailableULocales() {
        return null;
    }

    public String getName(String emojiOrSequence) {
        return null;
    }
    
    public String getKeywords(String emojiOrSequence) {
        return null;
    }
}
