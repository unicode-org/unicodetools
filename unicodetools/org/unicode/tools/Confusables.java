package org.unicode.tools;

import java.util.EnumMap;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.UnicodeMap;

public class Confusables {
    
    public enum Style {SL, SA, ML, MA}

    public static class Data {
        final Style style;
        final String result;
        public Data(Style style, String result) {
            this.style = style;
            this.result = result;
        }
    }

    /**
     * @return the style2map
     */
    public EnumMap<Style, UnicodeMap<String>> getStyle2map() {
        return style2map;
    }
    /**
     * @return the char2data
     */
    public UnicodeMap<EnumMap<Style, String>> getChar2data() {
        return char2data;
    }
    
    final private EnumMap<Style, UnicodeMap<String>> style2map;
    final private UnicodeMap<EnumMap<Style,String>> char2data = new UnicodeMap<EnumMap<Style,String>>();
    
    public Confusables (String directory) {
        EnumMap<Style, UnicodeMap<String>> _style2map = new EnumMap<Style,UnicodeMap<String>>(Style.class);
        try {
            for (String line : FileUtilities.in(directory, "confusables.txt")) {
                if (line.startsWith("\uFEFF")) {
                    line = line.substring(1);
                }
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                int hashPos = line.indexOf('#');
                line = line.substring(0,hashPos).trim();
                if (line.isEmpty()) {
                    continue;
                }
                String[] parts = line.split("\\s*;\\s*");
                String source = Utility.fromHex(parts[0]);
                String target = Utility.fromHex(parts[1]);
                Style style = Style.valueOf(parts[2]);
                UnicodeMap<String> map = _style2map.get(style);
                if (map == null) {
                    _style2map.put(style, map = new UnicodeMap<>());
                }
                map.put(source, target);
                EnumMap<Style, String> map2 = char2data.get(source);
                if (map2 == null) {
                    char2data.put(source, map2 = new EnumMap<>(Style.class));
                }
                map2.put(style, target);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        style2map = CldrUtility.protectCollection(_style2map);
        char2data.freeze();
    }
}