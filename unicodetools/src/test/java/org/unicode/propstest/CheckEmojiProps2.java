package org.unicode.propstest;

import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Binary;

import com.google.common.collect.Sets;
import com.ibm.icu.text.UnicodeSet;

/*
Note:
- in Feb 2021  org.unicode.propstest.CheckEmojiProps was added in 968c94c (PR#44) but not merged.  To reduce confusion, this class has been renamed to CheckEmojiProps2.
- in June 2021 added org.unicode.propstest.CheckEmojiProps was added in 163def1 in CLDR 14.0 keyword fixes (PR#65) with different content.
- Both of these are not to be confused with org.unicode.tools.CheckEmojiProps 
 */
public class CheckEmojiProps2 {
    static IndexUnicodeProperties IUP = IndexUnicodeProperties.make();

    static UnicodeSet EMOJI = IUP.loadEnum(UcdProperty.Emoji, Binary.class).getSet(Binary.Yes);
    static UnicodeSet BASIC_EMOJI = IUP.loadEnum(UcdProperty.Basic_Emoji, Binary.class).getSet(Binary.Yes);
    static UnicodeSet EMOJI_COMPONENT = IUP.loadEnum(UcdProperty.Emoji_Component, Binary.class).getSet(Binary.Yes);
    static UnicodeSet ZWJ_EMOJI = IUP.loadEnum(UcdProperty.RGI_Emoji_Zwj_Sequence, Binary.class).getSet(Binary.Yes);

    public static void main(String[] arg) {
        UnicodeSet bAndc = new UnicodeSet(EMOJI_COMPONENT).retainAll(BASIC_EMOJI);
        UnicodeSet bMinusc = new UnicodeSet(EMOJI_COMPONENT).removeAll(BASIC_EMOJI);
        System.out.println("new UnicodeSet(EMOJI_COMPONENT).retainAll(BASIC_EMOJI)" + ":\t" + bAndc.toPattern(false));
        System.out.println("new UnicodeSet(EMOJI_COMPONENT).removeAll(BASIC_EMOJI)" + ":\t" + bMinusc.toPattern(false));
    }

}
