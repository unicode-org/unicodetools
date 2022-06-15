package org.unicode.propstest;

import java.util.Objects;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.text.utility.Utility;

public class CheckEmojiProps {
    public static void main(String[] args) {
        IndexUnicodeProperties latest = IndexUnicodeProperties.make();
        final String unassigned = new StringBuilder().appendCodePoint(0x1FC00).toString();
        final String sampleEmoji = "🧔";

        System.out.println(
                "Comparing samples: " + sampleEmoji + " to U+" + Utility.hex(unassigned));
        for (UcdProperty item : UcdProperty.values()) {
            try {
                String cn = latest.getResolvedValue(item, unassigned);
                String emoji = latest.getResolvedValue(item, sampleEmoji);
                if (!Objects.equals(emoji, cn)) {
                    System.out.println(item + "\t" + emoji + "≠" + cn);
                }
            } catch (Exception e) {
            }
        }
    }
}
