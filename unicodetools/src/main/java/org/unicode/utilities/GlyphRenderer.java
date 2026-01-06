package org.unicode.utilities;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UProperty.NameChoice;
import com.ibm.icu.util.Output;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import org.unicode.text.utility.Utility;

public class GlyphRenderer {
    private static final boolean SHOW_METRICS = false;

    public static BufferedImage createGlyphBitmap(
            Font font, String text, Output<Rectangle2D> boundsOutput, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Setup Font and Rendering
        g2d.setFont(font);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Measure the Glyph
        // We need a temporary Graphics context to get the FontRenderContext
        FontRenderContext frc = g2d.getFontRenderContext();

        // createGlyphVector allows us to get the precise visual bounds of the character
        GlyphVector gv = font.createGlyphVector(frc, text);
        Rectangle2D bounds = gv.getVisualBounds();
        boundsOutput.value = bounds;

        // Get Global Font Metrics
        FontMetrics fm = g2d.getFontMetrics();

        // --- HORIZONTAL CENTERING ---
        // Calculate the width of the specific character
        int textWidth = fm.stringWidth(text);
        // Calculate X to center it in the canvas
        int x = (width - textWidth) / 2;

        // --- VERTICAL BASELINE ALIGNMENT ---
        // We want the "body" of the font to be centered vertically.
        // The body height is Ascent + Descent.
        int fontBodyHeight = fm.getMaxAscent() + fm.getMaxDescent();

        // Calculate how much empty space is left vertically
        int emptySpaceY = height - fontBodyHeight;

        // Split the empty space (top margin)
        int topMargin = emptySpaceY / 2;

        // The baseline is drawn at: Top Margin + Ascent
        int y = topMargin + fm.getMaxAscent();

        g2d.setColor(Color.BLACK);
        g2d.setFont(font);
        g2d.drawString(text, x, y);

        // (Optional) draw marks to show metrics (ascent, descent, advance-width
        if (SHOW_METRICS) {
            g2d.setColor(Color.BLUE);
            int ya = y - fm.getAscent();
            int x2 = x + textWidth;
            int yd = y + fm.getDescent();

            // draw horizontals, then verticals
            for (int yp : new int[] {ya, y, yd}) {
                g2d.drawLine(x, yp, x + 5, yp);
                g2d.drawLine(x2 - 5, yp, x2, yp);
            }
            // draw verticals
            g2d.drawLine(x, ya, x, ya + 5);
            g2d.drawLine(x2, ya, x2, ya + 5);
            g2d.drawLine(x, y - 5, x, y + 5);
            g2d.drawLine(x2, y - 5, x2, y + 5);
            g2d.drawLine(x, yd, x, yd - 5);
            g2d.drawLine(x2, yd, x2, yd - 5);
        }

        g2d.dispose();
        return image;
    }

    /**
     * Compares two BufferedImages pixel by pixel. Returns true if they are identical in dimensions
     * and content.
     */
    public static boolean compareImages(BufferedImage imgA, BufferedImage imgB) {
        // 1. Check dimensions
        if (imgA.getWidth() != imgB.getWidth() || imgA.getHeight() != imgB.getHeight()) {
            return false;
        }

        // 2. Check every pixel
        int width = imgA.getWidth();
        int height = imgA.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (imgA.getRGB(x, y) != imgB.getRGB(x, y)) {
                    return false;
                }
            }
        }

        return true;
    }

    /** Checks if the image is entirely white (opaque white). */
    public static boolean isAllWhite(BufferedImage img) {
        return isImageSingleColor(img, Color.WHITE.getRGB());
    }

    /** Checks if the image is entirely transparent (useful for the previous ARGB script). */
    public static boolean isAllTransparent(BufferedImage img) {
        // 0x00000000 is the integer value for fully transparent
        return isImageSingleColor(img, 0);
    }

    /** Helper: Checks if every pixel in the image matches a specific color. */
    public static boolean isImageSingleColor(BufferedImage img, int colorCode) {
        int width = img.getWidth();
        int height = img.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (img.getRGB(x, y) != colorCode) {
                    return false; // Found a pixel that isn't the target color
                }
            }
        }
        return true;
    }

	public static String getPropValueName(int uPropertyNumber, int nameChoice, int cp) {
	    return UCharacter.getPropertyValueName(
	            uPropertyNumber,
	            UCharacter.getIntPropertyValue(cp, uPropertyNumber),
	            nameChoice);
	}
	
	public static String charInfo(int cp) {
		return Character.toString(cp)
        + "\t"
        + Utility.hex(cp)
        + "\t"
        + UCharacter.getExtendedName(cp)
        + "\t"
        + GlyphRenderer.getPropValueName(UProperty.GENERAL_CATEGORY, NameChoice.SHORT, cp);
	}
}
