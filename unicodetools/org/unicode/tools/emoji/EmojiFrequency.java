package org.unicode.tools.emoji;

import java.util.Locale;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.Counter;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.EmojiData.EmojiDatum;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.VersionInfo;

public class EmojiFrequency {
    /* <a href="/details/2665" title="BLACK HEART SUIT" data-id="2665"> 
    <li class="emoji_char" id="2665" data-title="BLACK HEART SUIT"> 
    <span class="char emojifont"><span class="emoji emoji-2665"></span></span> 
    <span class="score" id="score-2665">375235298</span> </li> </a>
     */
    static UnicodeMap<Long> data = new UnicodeMap<>();
    static long total;
    static {
        Matcher m = Pattern.compile("id=\"score-(\\p{XDigit}+)(-\\p{XDigit}+)?\">(\\d+)</span>").matcher("");
        for (String line : FileUtilities.in(Settings.OTHER_WORKSPACE_DIRECTORY + "data/frequency/emoji/", "emoji-tracker.txt")) {
            if (line.startsWith("<section")) {
                continue;
            }
            if (m.reset(line).find()) {
                String cpString = m.group(1);
                String cpString2 = m.group(2);
                String scoreString = m.group(3);
                int cpFirst = Integer.parseInt(cpString, 16);
                String cp = UTF16.valueOf(cpFirst);
                if (m.group(2) != null) {
                    int cp2 = Integer.parseInt(m.group(2).substring(1), 16);
                    cp += UTF16.valueOf(cp2);
                }
                
                if (!Emoji.EMOJI_CHARS.contains(cp)) {
                    throw new IllegalArgumentException(Utility.hex(cp) + UCharacter.getName(cp, ", "));
                }
                long score = Long.parseLong(scoreString);

                data.put(cp, score);
                total += score;
                if (total < 0) {
                    throw new IllegalArgumentException();
                }
            } else {
                throw new IllegalArgumentException();
            }
        }
        data.freeze();
    }
    public static Long getFrequency(String word) {
        return data.get(word);
    }
    
    static final EmojiData EMOJIDATA = EmojiData.of(VersionInfo.getInstance(1));
    
    public static void main(String[] args) {
        System.out.println("Emoji\tTw. Count\tName\tAnnotations");
        NumberFormat nf = NumberFormat.getInstance(Locale.ENGLISH);
        for (Entry<String, Long> entry : data.entrySet()) {
            final String cp = entry.getKey();
            EmojiDatum data = EMOJIDATA.getData(cp);
            System.out.println(cp
                    + "\t" + nf.format(entry.getValue())
                    + "\t" + UCharacter.getName(cp, ", ")
                    + "\t" + EmojiAnnotations.ANNOTATIONS_TO_CHARS.getKeys(cp)
                    );
        }
        
        System.out.println("\n\n\n");
        System.out.println("Annotation\tTw. Count\tTw. Count Ave.\tSet");
        Counter<String> annotationToFrequency = new Counter<String>();
        Counter<String> annotationToCount = new Counter<String>();
        for (String cp : Emoji.EMOJI_CHARS) {
            Long freq = getFrequency(cp);
            if (freq == null) continue;
            for (String annotation : EmojiAnnotations.ANNOTATIONS_TO_CHARS.keySet()) {
                annotationToFrequency.add(annotation, freq);
                annotationToCount.add(annotation, 1);
            }
        }
        for (R2<Long, String> entry : annotationToFrequency.getEntrySetSortedByCount(false, null)) {
            final String annotation = entry.get1();
            final UnicodeSet us = EmojiAnnotations.ANNOTATIONS_TO_CHARS.getUnicodeSet(annotation);
            final Long count = entry.get0();
            System.out.println(
                    annotation
                    + "\t" + nf.format(count)
                    + "\t" + nf.format(count/annotationToCount.get(annotation))
                    + "\t" + us.toPattern(false)
                    );
        }
    }
}
