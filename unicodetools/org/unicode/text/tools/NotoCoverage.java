package org.unicode.text.tools;

import java.util.List;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.text.utility.Utility;

import com.google.common.base.Splitter;
import com.ibm.icu.dev.util.UnicodeMap;

public class NotoCoverage {
    static final UnicodeMap<String> DATA = new UnicodeMap<>();
    static {
        Splitter lineSplitter = Splitter.onPattern("\\.\\.|;").trimResults();
        for (String line : FileUtilities.in(NotoCoverage.class, "notoCoverage.txt")) {
            // 2F83B ;  NotoSansCJKjp-Black
            // 2F83F..2F840 ;  NotoSansCJKjp-Black
            List<String> items = lineSplitter.splitToList(line);
            String codePoint = Utility.fromHex(items.get(0));
            switch(items.size()) {
            case 3:
                String codePointEnd = Utility.fromHex(items.get(1));
                DATA.putAll(codePoint.codePointAt(0), codePointEnd.codePointAt(0), items.get(1));
                break;
            case 2:
                DATA.put(codePoint, items.get(1));
                break;
            default:
                throw new IllegalArgumentException();
            }

        }
        DATA.freeze();
    }
    public static UnicodeMap<String> getData() {
        return DATA;
    }
    public static boolean isCovered(int cp) {
        return DATA.containsKey(cp);
    }
}
