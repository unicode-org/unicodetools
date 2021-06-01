package org.unicode.tools.emoji;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.BitSet;
import java.util.Map.Entry;
import java.util.Set;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.UnicodeRelation;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.EntryRange;

public class ListFonts {
    private static final UnicodeSet NON_C = new UnicodeSet("[^[:c:]]");
    private static final GraphicsEnvironment LOCAL_GRAPHICS_ENVIRONMENT = GraphicsEnvironment.getLocalGraphicsEnvironment();

    public static void main(String[] args) throws IOException{
        String fonts[] = LOCAL_GRAPHICS_ENVIRONMENT.getAvailableFontFamilyNames();
        System.out.print("#fonts:\t" + fonts.length);
        UnicodeRelation<String> fontsForChars = new UnicodeRelation<>();
        UnicodeSet ascii = new UnicodeSet("[:ascii:]").freeze();
        try (PrintWriter out = FileUtilities.openUTF8Writer(Settings.Output.GEN_DIR + "fonts", "fontContents.txt")) {
            for ( int i = 0; i < fonts.length; i++ ) {
                Font font = new Font(fonts[i],0,24);
                UnicodeSet uset = checkCanDisplay(font);
                uset.removeAll(ascii);
                fontsForChars.addAll(uset, fonts[i]);
                System.out.println(fonts[i] + "\t" + uset.size());
                out.println(fonts[i] + "\t" + uset.size() + "\t" + uset.toPattern(false));
                out.flush();
            }
        }
        UnicodeSet found = fontsForChars.keySet();
        UnicodeSet missing = new UnicodeSet("[^[:cn:][:co:][:cs:][:cc:]]").removeAll(found).removeAll(ascii);
        UnicodeRelation<String> missingForScripts = new UnicodeRelation<>();

        for (String cp : missing) {
            BitSet bs = new BitSet();
            bs.clear();
            int script = UScript.getScriptExtensions(cp.codePointAt(0), bs);
            if (script >= 0) {
                missingForScripts.add(cp, UScript.getName(script));
            } else {
                for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i+1)) {
                    missingForScripts.add(cp, UScript.getName(i));
                }
            }
        }

        try (PrintWriter out = FileUtilities.openUTF8Writer(Settings.Output.GEN_DIR + "fonts", "fontContentsMissing.txt")) {
            for (String value : missingForScripts.values()) {
                UnicodeSet us = missingForScripts.getKeys(value);
                out.println(value + "\t" + us.toPattern(false));
            }
        }

        System.out.println("#Missing:\t" + missing.size());
        try (PrintWriter out = FileUtilities.openUTF8Writer(Settings.Output.GEN_DIR + "fonts", "fontContentsMissingByChar.txt")) {
            for (Entry<String, Set<String>> entry : missingForScripts.keyValues()) {
                String cp = entry.getKey();
                out.println("U+" + Utility.hex(cp, ", U+") + "\t" + UCharacter.getName(cp,", ") + "\t" + CollectionUtilities.join(entry.getValue(), "; "));
            }
        }
        

        System.out.println("#found:\t" + found.size());
        try (PrintWriter out = FileUtilities.openUTF8Writer(Settings.Output.GEN_DIR + "fonts", "fontContentsByChar.txt")) {
            for (Entry<String, Set<String>> entry : fontsForChars.keyValues()) {
                String cp = entry.getKey();
                out.println("U+" + Utility.hex(cp, ", U+") + "\t" + UCharacter.getName(cp,", ") + "\t" + CollectionUtilities.join(entry.getValue(), "; "));
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
