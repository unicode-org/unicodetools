package org.unicode.utilities;

import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Output;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import org.unicode.text.utility.Utility;

public class FastSymbolMatcher {

    // --- Tuning Parameters ---
    private static final double MAX_ASPECT_RATIO_DIFF =
            0.5; // Fail if width/height is too different
    private static final double MAX_DENSITY_DIFF = 0.25; // Fail if ink thickness is too different

    public static void main(String[] args) {
        UnicodeSet chars = new UnicodeSet(args[0]);
        System.out.println(chars.size() + "\t" + chars);

        List<SymbolProfile> profiles = new ArrayList<>();
        Output<Rectangle2D> visualBounds = new Output<>();
        String fontName = "Noto Sans";
        int fontSize = 144;
        Font font = new Font(fontName, Font.PLAIN, fontSize);

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
            profiles.add(new SymbolProfile(cp, image));
        }
        compare(profiles);
    }

    public static void compare(List<SymbolProfile> profiles) {

        // 2. PRE-PROCESSING (The "N" Step)
        // Convert heavy images into lightweight Profiles ONCE.
        // This is O(N) complexity.

        System.out.println("Generating comparisons");
        // 3. COMPARISON (The "N^2" Step)
        // Compare every symbol against every other symbol
        for (int i = 0; i < profiles.size(); i++) {
            System.out.print("\t" + Character.toString(profiles.get(i).cp));
        }
        System.out.println();
        for (int i = 0; i < profiles.size(); i++) {
            SymbolProfile p1 = profiles.get(i);
            System.out.print(Character.toString(p1.cp));
            for (int j = 0; j < i; j++) {
                SymbolProfile p2 = profiles.get(j);

                // --- THE QUICK TEST ---
                if (shouldFailFast(p1, p2)) {
                    // Skip expensive logic completely
                    // System.out.println("Quick Mismatch: " + GlyphRenderer.charInfo(p2.cp));
                    System.out.print("\t");
                    continue;
                }

                // --- EXPENSIVE TEST ---
                // Only runs if the symbols are roughly similar shapes
                double score = detailedCompare(p1, p2);
                System.out.print("\t" + (int) (score * 100));
            }
            System.out.println();
        }
    }

    /**
     * The "Gatekeeper" method. Returns TRUE if the symbols are so different we shouldn't bother
     * comparing pixels.
     */
    private static boolean shouldFailFast(SymbolProfile p1, SymbolProfile p2) {
        // Test 1: Aspect Ratio (Is one tall and thin, and the other short and wide?)
        // e.g., prevents comparing "l" with "w"
        if (Math.abs(p1.aspectRatio - p2.aspectRatio) > MAX_ASPECT_RATIO_DIFF) {
            return true;
        }

        // Test 2: Ink Density (Is one heavy/filled and the other light/empty?)
        // e.g., prevents comparing "." with "M"
        if (Math.abs(p1.inkDensity - p2.inkDensity) > MAX_DENSITY_DIFF) {
            return true;
        }

        return false;
    }

    /** The detailed pixel comparison (same logic as before, but using cached thumbnails). */
    private static double detailedCompare(SymbolProfile p1, SymbolProfile p2) {
        // 1. Calculate Weights
        double visualSim = getVisualSimilarity(p1.thumbnail, p2.thumbnail);

        // Size Penalty (using cached bounds)
        double areaA = p1.bounds.width * p1.bounds.height;
        double areaB = p2.bounds.width * p2.bounds.height;
        double sizeSim = 1.0 - (Math.abs(areaA - areaB) / Math.max(areaA, areaB));

        // Position Penalty (using cached centers)
        double dist = p1.center.distance(p2.center);
        double maxDist = Math.sqrt(Math.pow(1000, 2) + Math.pow(1000, 2)); // Mock max canvas size
        double posSim = 1.0 - (dist / maxDist);
        if (posSim < 0) posSim = 0;

        return (visualSim * 0.6) + (sizeSim * 0.2) + (posSim * 0.2);
    }

    private static double getVisualSimilarity(BufferedImage imgA, BufferedImage imgB) {
        long diff = 0;
        int w = imgA.getWidth();
        int h = imgA.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                // Quick grayscale diff
                int rgbA = imgA.getRGB(x, y) & 0xFF; // Blue channel proxy for gray
                int rgbB = imgB.getRGB(x, y) & 0xFF;
                diff += Math.abs(rgbA - rgbB);
            }
        }
        return 1.0 - (diff / (w * h * 255.0));
    }

    // --- INNER CLASS FOR PRE-CALCULATED DATA ---
    static class SymbolProfile {
        int cp;
        Rectangle bounds;
        Point center;
        double aspectRatio;
        double inkDensity;
        BufferedImage thumbnail; // Small 32x32 cached version

        public SymbolProfile(int cp, BufferedImage raw) {
            this.cp = cp;
            // 1. Expensive Scan (Done ONLY ONCE per image)
            this.bounds = getBounds(raw);

            if (this.bounds == null) {
                // Handle empty image
                this.bounds = new Rectangle(0, 0, 1, 1);
                this.aspectRatio = 0;
                this.inkDensity = 0;
                this.thumbnail = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
                return;
            }

            this.center = new Point((int) bounds.getCenterX(), (int) bounds.getCenterY());

            // 2. Pre-calculate Heuristics
            this.aspectRatio = (double) bounds.width / bounds.height;

            // 3. Create Cached Thumbnail
            BufferedImage cropped =
                    raw.getSubimage(bounds.x, bounds.y, bounds.width, bounds.height);
            this.thumbnail = resize(cropped, 32, 32);

            // 4. Calculate Density (on the small thumbnail to save time)
            this.inkDensity = calculateDensity(this.thumbnail);
        }

        private double calculateDensity(BufferedImage img) {
            long totalPixels = img.getWidth() * img.getHeight();
            long filledPixels = 0;
            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    int alpha = (img.getRGB(x, y) >> 24) & 0xff;
                    if (alpha > 0) filledPixels++; // Assuming transparent background
                    // If white background, check brightness < 200
                }
            }
            return (double) filledPixels / totalPixels;
        }

        private Rectangle getBounds(BufferedImage img) {
            int minX = img.getWidth(), minY = img.getHeight(), maxX = -1, maxY = -1;
            boolean found = false;
            for (int y = 0; y < img.getHeight(); y++) {
                int xSum = 0;
                for (int x = 0; x < img.getWidth(); x++) {
                    int alpha = (img.getRGB(x, y) >> 24) & 0xff;
                    xSum += alpha;
                    if (alpha != 0) { // Assuming transparent background
                        if (x < minX) minX = x;
                        if (x > maxX) maxX = x;
                        if (y < minY) minY = y;
                        if (y > maxY) maxY = y;
                        found = true;
                    }
                }
            }
            return found ? new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1) : null;
        }

        private BufferedImage resize(BufferedImage img, int w, int h) {
            Image tmp = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
            BufferedImage dimg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = dimg.createGraphics();
            g2d.drawImage(tmp, 0, 0, null);
            g2d.dispose();
            return dimg;
        }
    }
}
