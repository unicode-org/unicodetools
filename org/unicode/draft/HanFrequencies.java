package org.unicode.draft;

import java.io.PrintWriter;
import java.util.Set;

import org.unicode.cldr.util.Counter;
import org.unicode.text.UCD.UCD_Types;

import com.ibm.icu.text.UnicodeSet;

public class HanFrequencies {
    
    public static final UnicodeSet HAN = new UnicodeSet("[[:ideographic:][:sc=han:]]");
    
    public static void main(String[] args) {
        Set<String> languages = CharacterFrequency.getLanguagesWithCounter();
        //System.out.println(languages);
        show("zh");
        show("zh-Hant");
        show("ja");
        show("ko");
        show("mul");
    }

    private static void show(String locale) {
        System.out.println("Writing:\t" + locale);
        PrintWriter out = org.unicode.text.utility.Utility.openPrintWriter(UCD_Types.GEN_DIR + "/hanfrequency", 
                locale + ".txt", org.unicode.text.utility.Utility.UTF8_WINDOWS);
        Counter<String> counter = CharacterFrequency.getCharCounter(locale);
        long total = 0;
        for (String item : counter) {
            if (!HAN.containsAll(item)) {
                continue;
            }
            total += counter.get(item);
        }
        long countLimit = (long)(total * 0.999995d);
        UnicodeSet currentSet = new UnicodeSet();
        int setCount = 0;
        long runningTotal = 0;
        int chunkLimit = 1000;
        for (String item : counter.getKeysetSortedByCount(false)) {
            if (!HAN.containsAll(item)) {
                continue;
            }
            long count = counter.get(item);
            runningTotal += count;
            currentSet.add(item);
            if (currentSet.size() >= chunkLimit) {
                setCount += currentSet.size();
                out.println(setCount + "\t" + (runningTotal/(double)total) + "\t" + currentSet.toPattern(false));
                out.flush();
                System.out.print(".");
                currentSet.clear();
            }
            if (runningTotal > countLimit) break;
        }
        System.out.println();
        out.close();
    }
}
