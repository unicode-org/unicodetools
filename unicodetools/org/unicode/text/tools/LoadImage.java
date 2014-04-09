package org.unicode.text.tools;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

import com.ibm.icu.text.UnicodeSet;

/**
 * This class demonstrates how to load an Image from an external file
 */
public class LoadImage extends Component {

    private static final int IMAGE_TYPE = BufferedImage.TYPE_INT_RGB;

    private static final long serialVersionUID = 1L;

    BufferedImage img;

    public void paint(Graphics g) {
        g.drawImage(img, 0, 0, null);
    }

    public static BufferedImage getImage(String filename) {
        try {
            return ImageIO.read(new File(filename));
        } catch (IOException e) {
            return null;
        }

    }

    public Dimension getPreferredSize() {
        if (img == null) {
            return new Dimension(100, 100);
        } else {
            return new Dimension(img.getWidth(null), img.getHeight(null));
        }
    }

    static final UnicodeSet TWITTER_CHARS = new UnicodeSet(
            "[©®‼⁉™ℹ↔-↙↩↪⌚⌛⏩-⏬⏰⏳Ⓜ▪▫▶◀◻-◾☀☁☎☑☔☕☝☺♈-♓♠♣♥♦♨♻♿⚓⚠⚡⚪⚫⚽⚾⛄⛅⛎⛔⛪⛲⛳⛵⛺⛽✂✅✈-✌✏✒✔✖✨✳✴❄❇❌❎❓-❕❗❤➕-➗➡➰➿⤴⤵⬅-⬇⬛⬜⭐⭕〰〽㊗㊙🀄🃏🅰🅱🅾🅿🆎🆑-🆚🇦-🇿🈁🈂🈚🈯🈲-🈺🉐🉑🌀-🌠🌰-🌵🌷-🍼🎀-🎓🎠-🏄🏆-🏊🏠-🏰🐀-🐾👀👂-📷📹-📼🔀-🔽🕐-🕧🗻-🙀🙅-🙏🚀-🛅{#⃣}{0⃣}{1⃣}{2⃣}{3⃣}{4⃣}{5⃣}{6⃣}{7⃣}{8⃣}{9⃣}{🇨🇳}{🇩🇪}{🇪🇸}{🇫🇷}{🇬🇧}{🇮🇹}{🇯🇵}{🇰🇷}{🇷🇺}{🇺🇸}]");

    public static void main(String[] args) throws IOException {

        String inputDir = "/Users/markdavis/workspace/unicode-draft/reports/tr51/images/";
        String outputDir = "/Users/markdavis/Google Drive/Backup-2012-10-09/Documents/indigo/Generated/images/";

        //        doAndroid(inputDir, outputDir);
        //        doTwitter(inputDir, outputDir);
        //        doWindows(inputDir, outputDir);
        //doRef(inputDir, outputDir);
        //doGitHub(inputDir, outputDir);
        doSymbola(inputDir, outputDir, "Symbola", SYMBOLA);
    }

    public static void doGitHub(String inputDir, String outputDir)
            throws IOException {
        for (String s : Emoji.GITHUB_APPLE_CHARS) {
            String url = Emoji.APPLE_URL.transform(s);
            String core = Emoji.buildFileName(s, "_");
            System.out.println(core);
            BufferedImage sourceImage = ImageIO.read(new URL(url));
            BufferedImage targetImage = writeResizedImage(url, sourceImage, outputDir + "/apple", "apple_" + core, 72);
        }
    }

    final static UnicodeSet NON_SYMBOLA = new UnicodeSet("[🅰🆎🅱🆑🆒🆓🆔🆕🆖🅾🆗🅿🆘🆙🆚🆏🈁🈂🈹🉑🈴🈺🉐🈯🈷🈶🈵🈚🈸🈲🈳]");

    private static final UnicodeSet SYMBOLA = new UnicodeSet(Emoji.EMOJI_CHARS).removeAll(NON_SYMBOLA).freeze();

    public static void doSymbola(String inputDir, String outputDir, String font, UnicodeSet unicodeSet)
            throws IOException { // 🌰-🌵
        UnicodeSet quicktest = unicodeSet;
        int height2 = 72;
        int width = height2;
        BufferedImage sourceImage = new BufferedImage(width, height2, IMAGE_TYPE);
        Graphics2D graphics = sourceImage.createGraphics();
        graphics.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF);
        graphics.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        FontMetrics metrics = setFont(font, height2, graphics);
        UnicodeSet firstChars = new UnicodeSet();
        String fileDirectory = outputDir + "/ref2";
        for (String s : quicktest) { // 
            if (Emoji.isRegionalIndicator(s.codePointAt(0))) {
                continue;
            }
            String core = Emoji.buildFileName(s, "_");
            String filename = "ref_" + core;
            File outputfile = new File(fileDirectory, filename + ".png");
//            if (outputfile.exists()) {
//                continue; // workaround for ugly Java problem
//            }
//            char first = s.charAt(0);
//            if (firstChars.contains(first)) {
//                continue; // workaround for ugly Java problem
//            }
//            firstChars.add(first);

            graphics.clearRect(0, 0, width, height2);
            if (false) {
//                FontRenderContext frc = graphics.getFontRenderContext();
//                GlyphVector gv = myFont.createGlyphVector(frc, s);
//                Rectangle2D bounds = gv.getVisualBounds();
//                int xStart = (int)(width - bounds.getWidth()+0.5)/2;
//                Shape shape = gv.getOutline(xStart, metrics.getAscent());
//                graphics.draw(shape);
            } else {
                Rectangle2D bounds = metrics.getStringBounds(s, graphics);
                boolean reset = false;
                if (bounds.getWidth() > width) {
                    int height3 = (int)(height2*width/bounds.getWidth()+0.5);
                    metrics = setFont(font, height3, graphics);
                    bounds = metrics.getStringBounds(s, graphics);
                    reset = true;
                }
                int xStart = (int)(width - bounds.getWidth()+0.5)/2;
                int yStart = (int)(height2 - bounds.getHeight() + 0.5)/2 + metrics.getAscent();
                graphics.drawString(s, xStart, yStart);
                if (reset) {
                    metrics = setFont(font, height2, graphics);
                }
            }
            String url = Emoji.APPLE_URL.transform(s);
            System.out.println(core);

            //BufferedImage sourceImage = ImageIO.read(new URL(url));
            BufferedImage targetImage = writeResizedImage(url, sourceImage, fileDirectory, filename, height2);
        }
    }

    public static FontMetrics setFont(String font, int height2,
            Graphics2D graphics) {
        Font myFont = new Font(font, 0, height2);
        graphics.setFont(myFont);
        graphics.setColor(Color.BLACK);
        graphics.setBackground(Color.WHITE);
        FontMetrics metrics = graphics.getFontMetrics();
        return metrics;
    }

    public static void doTwitter(String inputDir, String outputDir)
            throws IOException {
        for (String s : TWITTER_CHARS) {
            String url = Emoji.TWITTER_URL.transform(s);
            String core = Emoji.buildFileName(s, "_");
            System.out.println(core);
            BufferedImage sourceImage = ImageIO.read(new URL(url));
            BufferedImage targetImage = writeResizedImage(url, sourceImage, outputDir + "/twitter", "twitter_" + core, 72);
        }
    }

    public static void doAndroid(String inputDir, String outputDir)
            throws IOException {
        for (File file : new File(inputDir, "android").listFiles()) {
            if (file.isDirectory()) {
                continue;
            }
            String name = file.getName();
            String core = name.substring("emoji_u".length(), name.length()-".png".length());
            System.out.println(file);
            // emoji_u00a9.png
            BufferedImage sourceImage = ImageIO.read(file);
            //BufferedImage sourceImage = ImageIO.read(new URL("http://abs.twimg.com/emoji/v1/72x72/23e9.png"));
            BufferedImage targetImage = writeResizedImage(name, sourceImage, outputDir + "/android", "android_" + core, 72);
        }
    }
    public static void doWindows(String inputDir, String outputDir)
            throws IOException {
        for (File file : new File(inputDir, "windows").listFiles()) {
            if (file.isDirectory()) {
                continue;
            }
            String name = file.getName();
            String core = name.substring("windows_".length(), name.length()-".png".length());
            System.out.println(file);
            // emoji_u00a9.png
            BufferedImage sourceImage = ImageIO.read(file);
            //BufferedImage sourceImage = ImageIO.read(new URL("http://abs.twimg.com/emoji/v1/72x72/23e9.png"));
            BufferedImage targetImage = writeResizedImage(name, sourceImage, outputDir + "/windows", "windows_" + core, 72);
        }
    }
    public static void doRef(String inputDir, String outputDir)
            throws IOException {
        for (File file : new File(inputDir).listFiles()) {
            if (file.isDirectory()) {
                continue;
            }
            String name = file.getName();
            if (name.startsWith(".")) {
                continue;
            }
            String core1 = name.substring(0, name.length()-".png".length());
            String core = Emoji.buildFileName(Emoji.getHexFromFlagCode(core1), "_");
            System.out.println(file);
            // emoji_u00a9.png
            BufferedImage sourceImage = ImageIO.read(file);
            //BufferedImage sourceImage = ImageIO.read(new URL("http://abs.twimg.com/emoji/v1/72x72/23e9.png"));
            BufferedImage targetImage = writeResizedImage(name, sourceImage, outputDir + "/ref", "ref_" + core, 72);
        }
    }


    public static BufferedImage writeResizedImage(String name, BufferedImage sourceImage,
            String outputDir, String outputName, int height) throws IOException {
        int sourceHeight = sourceImage.getHeight();
        BufferedImage targetImage;
        if (height == sourceHeight) {
            targetImage = sourceImage;
        } else {
            double scale = height / (double) sourceHeight;
            int width = (int)(sourceImage.getWidth() * scale + 0.5);

            targetImage = new BufferedImage(width, height, IMAGE_TYPE);

            System.out.println(name
                    + "\t" + sourceImage.getWidth() + ", " + sourceHeight 
                    + " => " + outputName + "\t" 
                    + targetImage.getWidth() + ", " + targetImage.getHeight());

            // https://abs.twimg.com/emoji/v1/72x72/231a.png

            Graphics2D graphics = targetImage.createGraphics();

            graphics.drawImage(sourceImage, 0, 0, targetImage.getWidth(),
                    targetImage.getHeight(),
                    0, 0, sourceImage.getWidth(), sourceHeight,
                    null);
        }
        File outputfile = new File(outputDir, outputName + ".png");
        ImageIO.write(targetImage, "png", outputfile);
        //        File outputfile2 = new File(outputDir, "big-" + outputName + ".png");
        //        ImageIO.write(sourceImage, "png", outputfile2);
        //
        return targetImage;
    }
}