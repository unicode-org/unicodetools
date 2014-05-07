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
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;

import org.unicode.text.tools.GenerateEmoji.Source;

import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

/**
 * This class demonstrates how to load an Image from an external file
 */
public class LoadImage extends Component {

    private static final int IMAGE_TYPE = BufferedImage.TYPE_INT_ARGB;

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

    static String inputDir = "/Users/markdavis/Google Drive/Backup-2012-10-09/Documents/indigo/DATA/images/";
    static String outputDir = "/Users/markdavis/Google Drive/Backup-2012-10-09/Documents/indigo/Generated/images/";
    public static void main(String[] args) throws IOException {
        doAnimatedGif();

        if (false) {
        UnicodeSet result = checkCanDisplay(new Font("Symbola",0,24));
        System.out.println(result.toPattern(false));
            List<BufferedImage> list;
            doAnimatedGif();
            UnicodeSet s = new UnicodeSet("[▫◻◼◽◾☀⚪⚫❗⤴⤵⬅⬆⬇⬛⬜⭐⭕〽]");
            list = doSymbola(inputDir, outputDir, "android", "AndroidEmoji", s, 144); // "Symbola"
            list = doAndroid(inputDir, outputDir);
            doWindows(inputDir, outputDir);
            doRef(inputDir, outputDir);
            doTwitter(inputDir, outputDir);
            doGitHub(inputDir, outputDir);
            //List<BufferedImage> list = doSymbola(inputDir, outputDir, "Apple Emoji", SYMBOLA, 144); // "Symbola"
            createAnimatedImage(new File(outputDir, "animated-symbola.gif"), list, 1, false);
        }
    }

    private static UnicodeSet checkCanDisplay(Font f) {
        UnicodeSet result = new UnicodeSet();
        UnicodeSet glyphCodes = new UnicodeSet();
        FontRenderContext frc = new FontRenderContext(null, true, true);
        for (UnicodeSetIterator it = new UnicodeSetIterator(new UnicodeSet("[^[:c:]]")); 
                it.next(); ) {
            int i = it.codepoint;
            if (f.canDisplay(i)) {
                GlyphVector gv = f.createGlyphVector(frc, UTF16.valueOf(i));
                final int glyphCode = gv.getGlyphCode(0);
                if (glyphCode >= 0) {
                    result.add(i);
                    glyphCodes.add(glyphCode);
                }
            }
        }
        return result;
    }

    public static void doAnimatedGif(Source... ordering) throws IOException {
        List<BufferedImage> list = new ArrayList();
        for (Entry<String, Set<String>> entry : GenerateEmoji.ORDERING_TO_CHAR.keyValuesSet()) {
            final String key = entry.getKey();
            System.out.println("\t" + key);
            //if (!key.contains("time")) continue;
            for (String chars : entry.getValue()) {
                System.out.println("\t" + chars);
                File file = GenerateEmoji.getBestFile(chars, ordering);
                BufferedImage sourceImage = ImageIO.read(file);
                BufferedImage targetImage = resizeImage(sourceImage, sourceImage.getHeight(), 72, true);
                list.add(targetImage);
            }
        }
        createAnimatedImage(new File(outputDir, "animated-emoji.gif"), list, 100, true);
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

    public static List<BufferedImage> doSymbola(String inputDir, String outputDir, 
            String prefix, String font, UnicodeSet unicodeSet, int height)
                    throws IOException { // 🌰-🌵
        List<BufferedImage> result = new ArrayList<>();
        Set<String> sorted = unicodeSet.addAllTo(new TreeSet());
        int width = height;
        BufferedImage sourceImage = new BufferedImage(width, height, IMAGE_TYPE);
        Graphics2D graphics = sourceImage.createGraphics();
        graphics.setColor(Color.BLACK);
        graphics.setBackground(Color.WHITE);
        graphics.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF);
        graphics.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        FontMetrics metrics = setFont(font, height, graphics);
        UnicodeSet firstChars = new UnicodeSet();
        String fileDirectory = outputDir + "/" +
                prefix;
        for (String s : sorted) { // 
            if (Emoji.isRegionalIndicator(s.codePointAt(0))) {
                continue;
            }
            if (graphics.getFont().canDisplayUpTo(s) != -1) {
                continue;
            }
            String core = Emoji.buildFileName(s, "_");
            String filename = prefix +
                    "_" + core;
            File outputfile = new File(fileDirectory, filename + ".png");
            graphics.clearRect(0, 0, width, height);
            Rectangle2D bounds = metrics.getStringBounds(s, graphics);
            boolean reset = false;
            if (bounds.getWidth() > width) {
                int height3 = (int)(height*width/bounds.getWidth()+0.5);
                metrics = setFont(font, height3, graphics);
                bounds = metrics.getStringBounds(s, graphics);
                reset = true;
            }
            int xStart = (int)(width - bounds.getWidth()+0.5)/2;
            int yStart = (int)(height - bounds.getHeight() + 0.5)/2 + metrics.getAscent();
            graphics.drawString(s, xStart, yStart);
            if (reset) {
                metrics = setFont(font, height, graphics);
            }
            String url = Emoji.APPLE_URL.transform(s);
            BufferedImage targetImage = writeResizedImage(url, sourceImage, fileDirectory, filename, height);
            result.add(deepCopy(sourceImage));
            System.out.println(core + "\t" + s);
        }
        return result;
    }

    public static FontMetrics setFont(String font, int height2,
            Graphics2D graphics) {
        Font myFont;
        if (font != null) {
            myFont = new Font(font, 0, height2);
        } else {
            myFont = graphics.getFont().deriveFont(height2);
        }
        graphics.setFont(myFont);
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

    public static List<BufferedImage> doAndroid(String inputDir, String outputDir)
            throws IOException {
        List<BufferedImage> result = new ArrayList<>();
        for (File file : new File(inputDir, "android").listFiles()) {
            if (file.isDirectory()) {
                continue;
            }
            String name = file.getName();
            String core = name.substring("emoji_u".length(), name.length()-".png".length());
            System.out.println(file);
            // emoji_u00a9.png
            BufferedImage sourceImage = ImageIO.read(file);
            BufferedImage targetImage = writeResizedImage(name, sourceImage, outputDir + "/android", "android_" + core, 72);
            result.add(targetImage);
        }
        return result;
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
        for (File file : new File(inputDir, "ref").listFiles()) {
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
            //            System.out.println(name
            //                    + "\t" + sourceImage.getWidth() + ", " + sourceHeight 
            //                    + " => " + outputName + "\t" 
            //                    + targetImage.getWidth() + ", " + targetImage.getHeight());

            targetImage = resizeImage(sourceImage, sourceHeight, height, false);
        }
        File outputfile = new File(outputDir, outputName + ".png");
        ImageIO.write(targetImage, "png", outputfile);
        //        File outputfile2 = new File(outputDir, "big-" + outputName + ".png");
        //        ImageIO.write(sourceImage, "png", outputfile2);
        //
        return targetImage;
    }

    public static BufferedImage resizeImage(BufferedImage sourceImage,
            int sourceHeight, int height, boolean square) {
        BufferedImage targetImage;
        double scale = height / (double) sourceHeight;
        int width = (int)(sourceImage.getWidth() * scale);
        int targetWidth = width;
        int targetHeight = height;
        int x = 0, y = 0;
        if (square) {
            if (width > height) {
                targetWidth = width = height;
                targetHeight = height * sourceImage.getHeight() / sourceImage.getWidth();
                y = (height - targetHeight)/2;
            }
        }
        targetImage = new BufferedImage(width, height, IMAGE_TYPE);


        // https://abs.twimg.com/emoji/v1/72x72/231a.png

        Graphics2D graphics = targetImage.createGraphics();
        graphics.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setBackground(Color.WHITE);
        graphics.clearRect(0, 0, width, height);

        graphics.drawImage(sourceImage, x, y, targetWidth, targetHeight,
                0, 0, sourceImage.getWidth(), sourceHeight,
                null);
        return targetImage;
    }

    public static void createAnimatedImage(File file, List<BufferedImage> input, 
            int timeBetweenImages, boolean loopContinuously) throws IOException {
        BufferedImage firstImage = input.get(0);

        // create a new BufferedOutputStream with the last argument
        ImageOutputStream output =
                new FileImageOutputStream(file);

        // create a gif sequence with the type of the first image, 1 second
        // between frames, which loops continuously
        GifSequenceWriter writer =
                new GifSequenceWriter(output, firstImage.getType(), timeBetweenImages,
                        loopContinuously);

        // write out the first image to our sequence...
        writer.writeToSequence(firstImage);
        for (int i = 1; i < input.size(); i++) {
            writer.writeToSequence(input.get(i));
        }

        writer.close();
        output.close();
    }
    static BufferedImage deepCopy(BufferedImage bi) {
        ColorModel cm = bi.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }
}