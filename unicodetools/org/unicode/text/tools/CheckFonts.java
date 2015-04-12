package org.unicode.text.tools;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeMap.EntryRange;
import com.ibm.icu.text.UnicodeSet;

public class CheckFonts {
    public static void main(String[] args) {

        if (true) return;
        GraphicsEnvironment e = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Font[] fonts = e.getAllFonts(); // Get the fonts

        // order the fonts to put big ones first, avoiding costly glyph check
        String[] bigFonts = { "NotoSansCJKjp-Black", "NotoSans", "NotoSansSymbols", "NotoSansYi", "NotoSansEgyptianHieroglyphs", "NotoSansCuneiform",
                "NotoSansCanadianAboriginal", "NotoSansBamum", "NotoNaskhArabic", "NotoSansEthiopic", "NotoKufiArabic", "NotoSansVai", "NotoSansLinearB" };
        Set<Font> allFonts = new LinkedHashSet<>();
        for (String s : bigFonts) {
            for (Font f : fonts) {
                if (f.getName().equals(s)) {
                    allFonts.add(f);
                    break;
                }
            }
        }
        allFonts.addAll(Arrays.asList(fonts));

        UnicodeMap<String> ur = new UnicodeMap<>();
        UnicodeSet TESTSET = new UnicodeSet("[[:L:][:M:][:N:][:P:][:S:]]");

        int oldKeysSize = 0;
        Set<String> seen = new HashSet<>();
        for (Font f : allFonts) {
            String fontName = f.getFontName();
            if (!fontName.startsWith("Noto")
                    && !fontName.startsWith("Roboto")
                    && !fontName.startsWith("Arimo")
                    && !fontName.startsWith("Cousine")
                    && !fontName.startsWith("Tinos")
                    ) {
                continue;
            }
            // skip -Medium, etc.
            int pos = fontName.indexOf('-');
            String baseName = pos < 0 ? fontName : fontName.substring(0,pos);
            if (seen.contains(baseName)) continue;
            seen.add(baseName);

            //System.out.println("*" + fontName);
            for (String s : TESTSET) {
                if (ur.containsKey(s)) {
                    continue;
                }
                if (!f.canDisplay(s.codePointAt(0))) {
                    continue;
                }
                if (canDisplay(f,s)) {
                    ur.put(s, fontName);
                }
            }
            int keysSize = ur.keySet().size();
            System.out.println("@" + fontName + "\t" + (keysSize - oldKeysSize));
            oldKeysSize = keysSize;
        }
        for (EntryRange<String> entry : ur.entryRanges()) {
            if (entry.string != null) {
                System.out.println(Utility.hex(entry.string) + " ;\t" + entry.value);
            } else if (entry.codepoint == entry.codepointEnd) {
                System.out.println(Utility.hex(entry.codepoint) + " ;\t" + entry.value); 
            } else {
                System.out.println(Utility.hex(entry.codepoint) + ".." + Utility.hex(entry.codepointEnd) + " ;\t" + entry.value); 
            }
        }
    }

    static final FontRenderContext fontRenderContext = new FontRenderContext(null, false, false);

    static boolean canDisplay(Font f, String codepoint) {
        GlyphVector glyphVector = f.createGlyphVector(fontRenderContext, codepoint);
        int glyphCode = glyphVector.getGlyphCode(0);
        return (glyphCode > 0);
    }
}
