package org.unicode.tools.emoji;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.io.IOException;
import java.io.PrintWriter;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.text.utility.Settings;

import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.EntryRange;

public class ListFonts {
    private static final UnicodeSet NON_C = new UnicodeSet("[^[:c:]]");
    private static final GraphicsEnvironment LOCAL_GRAPHICS_ENVIRONMENT = GraphicsEnvironment.getLocalGraphicsEnvironment();

    public static void main(String[] args) throws IOException{
        String fonts[] = LOCAL_GRAPHICS_ENVIRONMENT.getAvailableFontFamilyNames();
        try (PrintWriter out = FileUtilities.openUTF8Writer(Settings.GEN_DIR + "fonts", "fontContents.txt")) {
            for ( int i = 0; i < fonts.length; i++ ) {
                Font font = new Font(fonts[i],0,24);
                UnicodeSet uset = checkCanDisplay(font);
                System.out.println(fonts[i] + "\t" + uset.size());
                out.println(fonts[i] + "\t" + uset.size() + "\t" + uset.toPattern(false));
                out.flush();
            }
        }
    }

    public static UnicodeSet checkCanDisplay(Font f) {
        UnicodeSet result = new UnicodeSet();
        FontRenderContext frc = new FontRenderContext(null, true, true);
        for (EntryRange range : NON_C.ranges()) {
            for (int cp = range.codepoint; cp <= range.codepointEnd; ++cp) {
                if (f.canDisplay(cp)) {
                    GlyphVector gv = f.createGlyphVector(frc, UTF16.valueOf(cp));
                    final int glyphCode = gv.getGlyphCode(0);
                    if (glyphCode >= 0) {
                        result.add(cp);
                    }
                }
            }
        }
        return result;
    }
}
