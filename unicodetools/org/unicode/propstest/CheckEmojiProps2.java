package org.unicode.propstest;

import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Binary;

import com.google.common.collect.Sets;
import com.ibm.icu.text.UnicodeSet;

/**
 * PR #44 added this class, but it was obviously added under the same name with
 * different code at another time. Re-adding under a different name.
 */
public class CheckEmojiProps2 {
    static IndexUnicodeProperties IUP = IndexUnicodeProperties.make();

    static UnicodeSet EMOJI = IUP.loadEnum(UcdProperty.Emoji, Binary.class).getSet(Binary.Yes);
    static UnicodeSet BASIC_EMOJI = IUP.loadEnum(UcdProperty.Basic_Emoji, Binary.class).getSet(Binary.Yes);
    static UnicodeSet EMOJI_COMPONENT = IUP.loadEnum(UcdProperty.Emoji_Component, Binary.class).getSet(Binary.Yes);
    static UnicodeSet ZWJ_EMOJI = IUP.loadEnum(UcdProperty.RGI_Emoji_Zwj_Sequence, Binary.class).getSet(Binary.Yes);

    public static void main(String[] arg) {
        UnicodeSet b_c = new UnicodeSet(EMOJI_COMPONENT).retainAll(BASIC_EMOJI);
        UnicodeSet bAndc = new UnicodeSet(EMOJI_COMPONENT).removeAll(BASIC_EMOJI);
        System.out.println("new UnicodeSet(EMOJI_COMPONENT).retainAll(BASIC_EMOJI)" + ":\t" + b_c.toPattern(false));
        System.out.println("new UnicodeSet(EMOJI_COMPONENT).removeAll(BASIC_EMOJI)" + ":\t" + bAndc.toPattern(false));

    }

}
