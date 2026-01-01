package org.unicode.tools;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UProperty.NameChoice;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Output;
import java.awt.Font;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import javax.imageio.ImageIO;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;
import org.unicode.utilities.GlyphRenderer;

public class FindBlankGlyphs {
    public static final String DATA_DIR =
            Settings.UnicodeTools.UNICODETOOLS_REPO_DIR + "/unicodetools/data/temp/";

    public static void main(String[] args) {
        // Configuration
        String fontName = "Noto Sans";
        int fontSize = 144;
        Font font = new Font(fontName, Font.PLAIN, fontSize);

        String specialCases = "[\\N{HANGUL CHOSEONG FILLER}"
		                                        + "\\N{HANGUL JUNGSEONG FILLER}"
		                                        + "\\N{HANGUL FILLER}"
		                                        + "\\N{HALFWIDTH HANGUL FILLER}"
		                                        + "\\N{COMBINING GRAPHEME JOINER}"
		                                        + "\\N{KHMER VOWEL INHERENT AQ}"
		                                        + "\\N{KHMER VOWEL INHERENT AA}"
		                                        + "\\N{BRAILLE PATTERN BLANK}"
		                                        + "\\p{variation_selector}]"
		                                        + "]";
		UnicodeSet exclusions =
                new UnicodeSet(
                                "["
                                        + "\\p{C}"
                                        + "\\p{Z}"
                                        + "\\p{rgi_emoji}"
                                        // + "\\p{RGI_Emoji_Qualification=Minimally_Qualified}"
                                        // ICU doesn't support this yet!
                                        + "[\\p{emoji}-\\p{emoji_component}]"
                                        + "\\p{whitespace}"
                                        + "\\p{deprecated}"

                                        // special cases
                                        + specialCases)
                        .freeze();

        UnicodeSet showAnyway = new UnicodeSet("[]"); //  \\u034F
        UnicodeSet chars =
                new UnicodeSet(0, 0x10FFFF).removeAll(exclusions).addAll(showAnyway).freeze();
        UnicodeSet show = new UnicodeSet();
        Output<Rectangle2D> visualBounds = new Output<>();

        int count = 0;
        for (int cp : chars.codePoints()) {
            if ((count % 10000) == 0) {
                System.out.println(count + "\t" + Utility.hex(cp));
            }
            ++count;
            String character = Character.toString(cp);

            BufferedImage image =
                    GlyphRenderer.createGlyphBitmap(
                            font, character, visualBounds, fontSize * 2, fontSize * 3 / 2);
            if (!GlyphRenderer.isImageSingleColor(image, 0) && !showAnyway.contains(cp)) {
                continue;
            }
            show.add(cp);
            System.out.println(
                    Utility.hex(cp)
                            + "\t"
                            + UCharacter.getExtendedName(cp)
                            + "\t"
                            + getPropValueName(cp)
                            + "\t"
                            + visualBounds);

            // Save each image to file
            File file = new File(DATA_DIR, "glyph_" + Utility.hex(character) + ".png");
            try {
                ImageIO.write(image, "png", file);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        // write HTML file
        File file = new File(DATA_DIR, "list.html");
        try (PrintWriter writer = new PrintWriter(file)) {
            writer.println(
                    "<html><body><style>"
                            + "table, th, td {\n"
                            + "  border: 1px solid black; /* Sets a 1px solid black border on all elements */\n"
                            + "}\n"
                            + "\n"
                            + "table {\n"
                            + "  border-collapse: collapse; /* Merges adjacent borders into a single line */\n"
                            + "}\n"
                            + "\n"
                            + "th, td {\n"
                            + "  padding: 4px; /* Optional: Adds space between border and content */\n"
                            + "}\n"
                            + "body {\n"
                            + "    font-family: 'Noto Sans', 'Noto Sans Symbols', sans-serif;\n"
                            + "}\n"
                            + ""
                            + "</style><table>");
            for (int cp : show.codePoints()) {
                writer.println(
                        "<tr>"
                                + "<td>"
                                + "<img src='"
                                + "glyph_"
                                + Utility.hex(cp)
                                + ".png"
                                + "' alt='"
                                + UCharacter.getExtendedName(cp)
                                + "' width='auto' height='32'>"
                                + "</td><td style='font-size: 24px; text-align: center'>"
                                + Character.toString(cp)
                                + "</td><td>"
                                + Utility.hex(cp)
                                + "</td><td>"
                                + UCharacter.getExtendedName(cp)
                                + "</td><td>"
                                + getPropValueName(cp)
                                + "</td>"
                                + "<tr>");
            }
            writer.println("</table></body></html>");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        System.out.println("Checked: " + count);
        System.out.println(specialCases);
    }

    private static String getPropValueName(int cp) {
        return UCharacter.getPropertyValueName(
                UProperty.GENERAL_CATEGORY,
                UCharacter.getIntPropertyValue(cp, UProperty.GENERAL_CATEGORY),
                NameChoice.SHORT);
    }
}
