package org.unicode.text.tools;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.text.tools.GenerateEmoji.Source;
import org.unicode.text.tools.GmailEmoji.Data;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

import com.google.common.base.Objects;
import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.lang.UScript.ScriptUsage;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.ULocale;

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

    static final UnicodeSet OLD_TWITTER_CHARS = new UnicodeSet(
            "[©®‼⁉™ℹ↔-↙↩↪⌚⌛⏩-⏬⏰⏳Ⓜ▪▫▶◀◻-◾☀☁☎☑☔☕☝☺♈-♓♠♣♥♦♨♻♿⚓⚠⚡⚪⚫⚽⚾⛄⛅⛎⛔⛪⛲⛳⛵⛺⛽✂✅✈-✌✏✒✔✖✨✳✴❄❇❌❎❓-❕❗❤➕-➗➡➰➿⤴⤵⬅-⬇⬛⬜⭐⭕〰〽㊗㊙🀄🃏🅰🅱🅾🅿🆎🆑-🆚🇦-🇿🈁🈂🈚🈯🈲-🈺🉐🉑🌀-🌠🌰-🌵🌷-🍼🎀-🎓🎠-🏄🏆-🏊🏠-🏰🐀-🐾👀👂-📷📹-📼🔀-🔽🕐-🕧🗻-🙀🙅-🙏🚀-🛅{#⃣}{0⃣}{1⃣}{2⃣}{3⃣}{4⃣}{5⃣}{6⃣}{7⃣}{8⃣}{9⃣}{🇨🇳}{🇩🇪}{🇪🇸}{🇫🇷}{🇬🇧}{🇮🇹}{🇯🇵}{🇰🇷}{🇷🇺}{🇺🇸}]");

    static String inputDir = Settings.OTHER_WORKSPACE_DIRECTORY + "DATA/emoji_images/";
    static String outputDir = Settings.OTHER_WORKSPACE_DIRECTORY + "Generated/images/";

    public static void main(String[] args) throws IOException {
        doRef(inputDir, outputDir);
        if (true) return;
        doTwitter(inputDir, outputDir);
        doRef(inputDir, outputDir);
        writeCharSamples(true);
        doAnimatedGif(false, 72);
        EmojiFlagOrder.getFlagOrder();
        TarotSuits.makeTest();
        doAnimatedGif(false, 72);
        doGmail(outputDir);
        doKddi(outputDir);
        doDoCoMo(outputDir);
        doSb(outputDir);

        getAppleNumbers();
        UnicodeSet dingbats = canDisplay("Zapf Dingbats");
        System.out.println(dingbats);

        UnicodeSet missing = new UnicodeSet("[\u00A9\u00AE\u2934\u2935\u2B05-\u2B07\u2B1B\u2B1C\u2B50\u2B55\u2E19\u3297\u3299]");
        UnicodeSet missing2 = new UnicodeSet("[✊ ✋ ✨  ✅ ❌ ❎ ➕ ➖ ➗ ➰ ➿  ❓ ❔ ❕]");
        doSymbola(inputDir, outputDir, "ref", "ref", "Symbola", missing2, 144, true); // "Symbola"
        // doAnimatedGif();

        if (false) {
            UnicodeSet result = checkCanDisplay(new Font("Symbola",0,24));
            System.out.println(result.toPattern(false));
            List<BufferedImage> list;
            UnicodeSet s = new UnicodeSet("[▫◻◼◽◾☀⚪⚫❗⤴⤵⬅⬆⬇⬛⬜⭐⭕〽]");
            list = doSymbola(inputDir, outputDir, null, "android", "AndroidEmoji", s, 144, false); // "Symbola"
            list = doAndroid(inputDir, outputDir);
            doWindows(inputDir, outputDir);
            doRef(inputDir, outputDir);
            doTwitter(inputDir, outputDir);
            doGitHub(inputDir, outputDir);
            //List<BufferedImage> list = doSymbola(inputDir, outputDir, "Apple Emoji", SYMBOLA, 144); // "Symbola"
            createAnimatedImage(new File(outputDir, "animated-symbola.gif"), list, 1, false);
        }
    }

    private static void renameCountryFlags() {
        File dir = new File("/Users/markdavis/Google Drive/workspace/DATA/emoji_images/newCountryFlags");
        for (File file : dir.listFiles()) {
            String oldName = file.getName();
            String[] parts = oldName.split("\\.");
            if (parts.length != 2) throw new IllegalArgumentException();
            if (parts[0].length() != 2) {
                System.out.println("SKIPPING: " + file.getName());
                continue;
            }
            String emoji = GenerateEmoji.getEmojiFromRegionCode(parts[0]);
            String newName = "ref_" + Emoji.buildFileName(emoji,"_") + "." + parts[1];
            File target = new File(dir + "/" + newName);
            //System.out.println(file.getName() + "\t=>\t" + target);
            file.renameTo(target);
        }
    }

    static final BitSet SKIP_SCRIPTS = new BitSet();
    static {
        SKIP_SCRIPTS.set(UScript.BASSA_VAH);
        SKIP_SCRIPTS.set(UScript.DUPLOYAN);
        SKIP_SCRIPTS.set(UScript.GRANTHA);
        SKIP_SCRIPTS.set(UScript.KHOJKI);
        SKIP_SCRIPTS.set(UScript.KHUDAWADI);
        SKIP_SCRIPTS.set(UScript.MAHAJANI);
        SKIP_SCRIPTS.set(UScript.MENDE);
        SKIP_SCRIPTS.set(UScript.MODI);
        SKIP_SCRIPTS.set(UScript.MRO);
        SKIP_SCRIPTS.set(UScript.NABATAEAN);
        SKIP_SCRIPTS.set(UScript.OLD_PERMIC);
        SKIP_SCRIPTS.set(UScript.PALMYRENE);
        SKIP_SCRIPTS.set(UScript.PAU_CIN_HAU);
        SKIP_SCRIPTS.set(UScript.TIRHUTA);
        SKIP_SCRIPTS.set(UScript.WARANG_CITI);
        SKIP_SCRIPTS.set(UScript.MANICHAEAN);
    }
    public static void writeCharSamples(boolean all) throws IOException {
        Collection<String> samples = new TreeSet<>(Collator.getInstance(ULocale.ROOT)); // Arrays.asList("a", "☕")
        final CLDRConfig config = CLDRConfig.getInstance();
        final Factory cldrFactory = config.getCldrFactory();
        LocaleIDParser lidp = new LocaleIDParser();
        final BreakIterator bi = BreakIterator.getSentenceInstance();

        UnicodeSet rawSamples = new UnicodeSet();
        if (all) {
            for (int script = 0; script < UScript.CODE_LIMIT; ++script) {
                ScriptUsage usage = UScript.getUsage(script);
                if (usage == ScriptUsage.NOT_ENCODED || SKIP_SCRIPTS.get(script)) continue;
                String sample = getSample(script);
                rawSamples.add(sample);
            }
        } else {
            for (String cldrFile : cldrFactory.getAvailableLanguages()) {
                if (cldrFile.equals("root")) continue;
                lidp.set(cldrFile);
                String lang = lidp.getLanguage();
                //            Level level = config.getStandardCodes().getLocaleCoverageLevel("cldr", lang);
                //            if (Level.MODERN.compareTo(level) > 0) {
                //                continue;
                //            }
                CLDRFile cldrFileObj = cldrFactory.make(cldrFile, true);
                UnicodeSet exemplars = cldrFileObj.getExemplarSet("", CLDRFile.WinningChoice.WINNING);
                int script = 0;
                for (String s : exemplars) {
                    script = UScript.getScript(s.codePointAt(0));
                    if (script != UScript.COMMON && script != UScript.INHERITED) break;
                }
                String sample = getSample(script);
                //String sample = cldrFileObj.getName(CLDRFile.LANGUAGE_NAME, lang);
                sample = UCharacter.toTitleCase(ULocale.ENGLISH, sample, bi, UCharacter.TITLECASE_NO_LOWERCASE);
                rawSamples.add(sample);
            }
        }

        for (String sample : rawSamples) {
            samples.add(" " + sample + " ");
        }
        System.out.println(samples);
        //samples = new LinkedHashSet<>(samples);

        //        for (int script = 0; script < UScript.CODE_LIMIT; ++script) {
        //            String sample = UScript.getSampleString(script);
        //            if (sample != null) {
        //                scripts.add(sample);
        //            }
        //        }
        writeTextAnimatedImage(new File(outputDir, "animated-text.gif"), 288, 288, 14, samples, 1000);
    }

    private static String getSample(int script) {
        String sample = UScript.getSampleString(script);
        switch (sample) {
        case "\u2800": sample = "⠎"; break;
        case "\u07CA": sample = "‎ߘ‎"; break;
        case "𐊠": sample = "𐊷"; break;
        case "ꓐ": sample = "ꓨ"; break;
        case "‎\uD802\uDED8": sample = "‎𐫁‎"; break;
        case "ⴰ": sample = "ⵞ"; break;
        }
        return sample;
    }

    private static void getAppleNumbers() throws IOException {
        Set<String> codepointOrder = new TreeSet(new UTF16.StringComparator());
        UnicodeSet found = new UnicodeSet("[©®🌠🔈🚃🚋🔊🔉{#⃣}{0⃣}{1⃣}{2⃣}{3⃣}{4⃣}{5⃣}{6⃣}{7⃣}{8⃣}{9⃣}]")
        .addAll(Emoji.GITHUB_APPLE_CHARS);
        for (String s : found) {
            codepointOrder.add(s);
        }
        Set<Integer> skipSet = new HashSet();
        skipSet.add(129);

        int i = 1;
        String newDir = Settings.OTHER_WORKSPACE_DIRECTORY + "DATA/AppleEmoji/";
        final int maxNew = 846;
        String oldDir = "/Users/markdavis/workspace/unicode-draft/reports/tr51/images/apple/";
        PrintWriter out = BagFormatter.openUTF8Writer(Settings.OTHER_WORKSPACE_DIRECTORY + "Generated/images", "checkApple.html");
        out.println("<html><body><table>");
        for (String s : codepointOrder) {
            while (skipSet.contains(i)) {
                ++i;
            }
            writeAppleLine(out, newDir, i, oldDir, s);
            ++i;
        }
        for (;i <= maxNew; ++i) {
            writeAppleLine(out, newDir, i, oldDir, "");
        }
        out.println("</table></body></html>");
        out.close();
    }

    static FileSystem dfs = FileSystems.getDefault();

    public static void writeAppleLine(PrintWriter out, String newDir, int i,
            String oldDir, String s) throws IOException {
        final String gitName = newDir + i + ".png";
        Path oldPath = dfs.getPath(gitName);     
        final String fixedName = "apple_" + Emoji.buildFileName(s, "_") + ".png";

        Path foo = Files.move(oldPath, oldPath.resolveSibling(fixedName), StandardCopyOption.ATOMIC_MOVE);

        if (false) {
            out.println("<tr>" +
                    "<td>" + i + "</td>" +
                    "<td>" + "<img src='" + gitName + "' >" + "</td>" +
                    "<td>" + Utility.hex(s) + "</td>" +
                    "<td>" + (s.isEmpty() ? "" : "<img src='" + oldDir + fixedName +
                            "' >") + "</td>" +
                            "</tr>" 
                    );
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

    public static void doAnimatedGif(boolean onlySize, int size, Source... ordering) throws IOException {
        final File output = new File(outputDir, "animated-emoji.gif");
        System.out.println(output.getCanonicalPath());
        List<BufferedImage> list = new ArrayList();
        for (Entry<String, Set<String>> entry : GenerateEmoji.ORDERING_TO_CHAR.keyValuesSet()) {
            final String key = entry.getKey();
            System.out.println("\t" + key);
            //if (!key.contains("time")) continue;
            for (String chars : entry.getValue()) {
                File file = GenerateEmoji.getBestFile(chars, ordering);
                BufferedImage sourceImage = ImageIO.read(file);
                System.out.println(chars + "\t" + sourceImage.getWidth() + "\t" + sourceImage.getHeight());
                if (onlySize) continue;
                BufferedImage targetImage;
                if (size == sourceImage.getHeight()) {
                    targetImage = sourceImage;
                } else {
                    targetImage = resizeImage(sourceImage, size, size, Resizing.DEFAULT);
                }
                list.add(targetImage);
            }
        }
        if (onlySize) return;
        createAnimatedImage(output, list, 100, true);
        System.out.println("Image created");
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

    private static void writeTextAnimatedImage(File output, int height, int width, int margin, Iterable<String> textList, int millisBetween) throws IOException {
        //final File output = new File(outputDir, "animated-emoji.gif");
        System.out.println(output.getCanonicalPath());
        List<BufferedImage> list = new ArrayList<>();
        for (String s : textList) {
            BufferedImage bi = drawString(s, "Symbola", width, height, margin, false, true);
            list.add(bi);
        }
        createAnimatedImage(output, list, millisBetween, true);
    }

    static public BufferedImage drawString(String text, String font, int width, int height, int margin, boolean scale, boolean center) {
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
        String originalFont = font;
        FontMetrics metrics = setFont(font, height - 2*margin, graphics);
        graphics.clearRect(0, 0, width, height);
        Rectangle2D bounds = metrics.getStringBounds(text, graphics);
        boolean reset = false;
        if (scale && bounds.getWidth() > width - 2*margin) {
            int height3 = (int)(height*width/bounds.getWidth()+0.5);
            metrics = setFont(font, height3, graphics);
            bounds = metrics.getStringBounds(text, graphics);
            reset = true;
        }
        int xStart = (center ? (int)(width - bounds.getWidth()+0.5)/2 : margin);
        int yStart = (int)(height - bounds.getHeight() + 0.5)/2 + metrics.getAscent();
        graphics.drawString(text, xStart, yStart);
        return sourceImage;
    }
    public static List<BufferedImage> doSymbola(String inputDir, String outputDir, 
            String dir, String prefix, String font, UnicodeSet unicodeSet, int height, boolean useFonts)
                    throws IOException { // 🌰-🌵
        if (dir == null) {
            dir = prefix;
        }
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
        String originalFont = font;
        FontMetrics metrics = setFont(font, height, graphics);
        UnicodeSet firstChars = new UnicodeSet();
        String fileDirectory = outputDir + "/" + prefix;
        for (String s : sorted) { //
            if (Emoji.isRegionalIndicator(s.codePointAt(0))) {
                continue;
            }
            if (useFonts) {
                FontData font1 = FontData.getFont(s);
                if (font1 == null) {
                    font = originalFont;
                    //System.err.println("No UCS font for " + Utility.hex(s));
                } else {
                    font = font1.fontName;
                }
                metrics = setFont(font, height, graphics);
            }
            if (graphics.getFont().canDisplayUpTo(s) != -1) {
                continue;
            }
            String core = Emoji.buildFileName(s, "_");
            String filename = prefix + "_" + core;
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

    //    public static void doTwitterOld(String inputDir, String outputDir)
    //            throws IOException {
    //        for (String s : TWITTER_CHARS) {
    //            String url = Emoji.TWITTER_URL.transform(s);
    //            String core = Emoji.buildFileName(s, "_");
    //            System.out.println(core);
    //            BufferedImage sourceImage = ImageIO.read(new URL(url));
    //            BufferedImage targetImage = writeResizedImage(url, sourceImage, outputDir + "/twitter", "twitter_" + core, 72);
    //        }
    //    }

    public static void doTwitter(String inputDir, String outputDir)
            throws IOException {
        String inputDirectory = "/Users/markdavis/twemoji/72x72/";
        UnicodeSet twitterChars = new UnicodeSet();
        for (File file : new File(inputDirectory).listFiles()) {
            if (file.isDirectory()) {
                continue;
            }
            String name = file.getName();
            if (name.startsWith(".")) {
                continue;
            }
            String[] parts = name.split("\\.");
            if (parts.length != 2) throw new IllegalArgumentException();
            String core1 = Emoji.parseFileName(false, parts[0]);
            twitterChars.add(core1);
            String core = Emoji.buildFileName(core1, "_");
            System.out.println(file + "\t" + core + (core.equals(parts[0]) ? "" 
                    : "\tCHANGED"));
            if (true) continue;
            // emoji_u00a9.png
            BufferedImage sourceImage = ImageIO.read(file);
            //BufferedImage sourceImage = ImageIO.read(new URL("http://abs.twimg.com/emoji/v1/72x72/23e9.png"));
            BufferedImage targetImage = writeResizedImage(name, sourceImage, outputDir + "/twitter", "twitter_" + core, 72);
        }
        twitterChars.freeze();
        System.out.println("TwitterChars: " + twitterChars.toPattern(false));
        System.out.println("Added: " + new UnicodeSet(twitterChars).removeAll(OLD_TWITTER_CHARS));
        System.out.println("Removed: " + new UnicodeSet(OLD_TWITTER_CHARS).removeAll(twitterChars));
    }

    public static void doDoCoMo(String outputDir)
            throws IOException {
        for (String s : CarrierGlyphs.Emoji_DCM.keySet()) {
            String url = CarrierGlyphs.DCM_URL.transform(s);
            String core = Emoji.buildFileName(s, "_");
            System.out.println(core);
            copy(new URL(url), new File(outputDir + "/dcm","dcm_" + core + ".gif"));
            //            BufferedImage sourceImage = ImageIO.read(new URL(url));
            //            writeImage(sourceImage,outputDir + "/dcm","dcm_" + core, "gif");
            //BufferedImage targetImage = writeResizedImage(url, sourceImage, outputDir + "/twitter", "twitter_" + core, 72);
        }
    }

    public static void doKddi(String outputDir)
            throws IOException {
        for (String s : CarrierGlyphs.Emoji_KDDI.keySet()) {
            String url = CarrierGlyphs.AU_URL.transform(s);
            String core = Emoji.buildFileName(s, "_");
            System.out.println(core);
            copy(new URL(url), new File(outputDir + "/kddi","kddi_" + core + ".gif"));
            //            BufferedImage sourceImage = ImageIO.read(new URL(url));
            //            writeImage(sourceImage,outputDir + "/kddi","kddi_" + core, "gif");
            //BufferedImage targetImage = writeResizedImage(url, sourceImage, outputDir + "/twitter", "twitter_" + core, 72);
        }
    }

    public static void doSb(String outputDir)
            throws IOException {
        for (String s : CarrierGlyphs.Emoji_SB.keySet()) {
            String url = CarrierGlyphs.SB_URL.transform(s);
            String core = Emoji.buildFileName(s, "_");
            System.out.println(core);
            copy(new URL(url), new File(outputDir + "/sb","sb_" + core + ".gif"));
        }
        //        for (String s : Arrays.asList("G", "E", "F", "O", "P", "Q")) {
        //            for (int i = 0x21; i <= 0x7E; ++i) {
        //                final String code = s + Integer.toHexString(i);
        //                String url = "http://trialgoods.com/images/200807sb/" + code + ".gif";
        //                //String core = Emoji.buildFileName(s, "_");
        //                try {
        //                    copy(new URL(url), new File(outputDir + "/sb","sb_" + code + ".gif"));
        ////                    BufferedImage sourceImage = ImageIO.read(new URL(url));
        ////                    writeImage(sourceImage,outputDir + "/sb","sb_" + code, "gif");
        //                    System.out.println(code);
        //                } catch (Exception e) {
        //                    System.out.println("Skipping " + code);
        //                    continue;
        //                }
        //                //BufferedImage targetImage = writeResizedImage(url, sourceImage, outputDir + "/twitter", "twitter_" + core, 72);
        //            }
        //        }
    }

    public static void doGmail(String outputDir)
            throws IOException {
        for (Entry<String, Data> entry : GmailEmoji.unicode2data.entrySet()) {
            String s = entry.getKey();
            String url = GmailEmoji.getURL(s);
            if (url == null) continue;
            String core = Emoji.buildFileName(s, "_");
            System.out.println(core);

            final URL url2 = new URL(url);
            File output = new File(outputDir + "/gmail","gmail_" + core + ".gif");
            copy(url2, output);
            //            BufferedImage sourceImage = ImageIO.read(url2);
            //            writeImage(sourceImage,outputDir + "/gmail","gmail_" + core, "gif");
            //BufferedImage targetImage = writeResizedImage(url, sourceImage, outputDir + "/twitter", "twitter_" + core, 72);
        }
    }
    static void copy(URL url, File file) {
        byte[] buffer = new byte[1024*16];
        try (InputStream in = url.openStream();
                OutputStream out = new FileOutputStream(file)){
            while (true) {
                int lengthRead = in.read(buffer, 0, buffer.length);
                if (lengthRead == -1) {
                    break;
                }
                out.write(buffer, 0, lengthRead);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
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
        String nepal = "NP";
        for (File file : new File(inputDir, "ref").listFiles()) {
            if (file.isDirectory()) {
                continue;
            }
            String name = file.getName();
            if (name.startsWith(".")) {
                continue;
            }
            String core1 = name.substring(0, name.length()-".png".length());
            Resizing addBorder = !core1.equals(nepal) ? Resizing.FLAG : Resizing.FLAG_NOBORDER;
            String core = Emoji.buildFileName(Emoji.getHexFromFlagCode(core1), "_");
            System.out.println(file);
            // emoji_u00a9.png
            BufferedImage sourceImage = ImageIO.read(file);
            //BufferedImage sourceImage = ImageIO.read(new URL("http://abs.twimg.com/emoji/v1/72x72/23e9.png"));
            BufferedImage targetImage = writeResizedImage(name, sourceImage, outputDir + "/ref", "ref_" + core, 108, 108, addBorder);
        }
    }

    static class FontData {
        final String fontName;
        final int fontSize;
        public FontData(String fontName, int fontSize) {
            this.fontName = fontName;
            this.fontSize = fontSize;
        }
        @Override
        public String toString() {
            return fontName + "," + fontSize;
        }
        @Override
        public int hashCode() {
            return fontName.hashCode() ^ fontSize;
        }
        @Override
        public boolean equals(Object obj) {
            FontData other = (FontData)obj;
            return fontName.equals(other.fontName) && fontSize == other.fontSize;
        }
        static Map<FontData, FontData> KEY_TO_FONT = new HashMap<>();
        static private FontData getFont(FontData key) {
            FontData result = KEY_TO_FONT.get(key);
            if (result == null) {
                KEY_TO_FONT.put(key, key);
                result = key;
            }
            return result;
        }
        static UnicodeMap<FontData> FONT_DATA = new UnicodeMap();
        static public FontData getFont(int codepoint) {
            return FONT_DATA.get(codepoint);
        }
        static public FontData getFont(String codepoint) {
            return FONT_DATA.get(codepoint);
        }
        enum DataType {Q, X, R, I}
        static {
            Map<DataType,UnicodeSet> tempData = new LinkedHashMap();
            Matcher lineMatch = Pattern.compile("([^,]*),\\s*(\\d+)(?:,\\s*(\\d+))?(.*)").matcher("");
            Matcher m = Pattern.compile("/([QXRI])=(.*)").matcher("");
            for (String line : FileUtilities.in(FontData.class, "CONFIG-FILE-SECTIONS.txt")) {
                if (line.isEmpty() || line.startsWith(";")) {
                    continue;
                }
                // TmsMathPak7bttPF,22 /Q=2047 /R=2047-2047
                if (line.contains("2300")) {
                    int debug = 0;
                }
                if (!lineMatch.reset(line).matches()) {
                    throw new IllegalArgumentException("Couldn't match font line: " + line);
                }
                FontData key = getFont(new FontData(lineMatch.group(1).trim(), Integer.parseInt(lineMatch.group(2))));
                tempData.clear();
                String[] parts = lineMatch.group(4).trim().split("\\s*[ ,]\\s*");
                for (String part : parts) {
                    if (part.startsWith(";")) {
                        break;
                    } else if (part.isEmpty()) {
                        continue;
                    }
                    if (!m.reset(part).matches()) {
                        throw new IllegalArgumentException("Couldn't match font line: " + line);
                    }
                    // /X=0000-10FFFF /I=2000-206F
                    String[] ranges = m.group(2).split("-");
                    int r1 = Integer.parseInt(ranges[0],16);
                    int r2 = ranges.length == 1 
                            ? r1
                                    : Integer.parseInt(ranges[1],16);
                    tempData.put(DataType.valueOf(m.group(1)), new UnicodeSet(r1, r2));
                }
                ///I=2070-209F code points
                UnicodeSet i = tempData.get(DataType.I);
                if (i == null) {
                    UnicodeSet q = tempData.get(DataType.Q);
                    UnicodeSet r = tempData.get(DataType.R);
                    if (q.getRangeStart(0) != r.getRangeStart(0)) {
                        UnicodeSet missing = new UnicodeSet(q.getRangeStart(0),
                                q.getRangeStart(0) + (r.getRangeEnd(0)-r.getRangeStart(0)));
                        if (Emoji.EMOJI_CHARS.containsSome(missing)) {
                            System.err.println("Couldn't get code points from: " + line);
                        }
                        continue;
                        //throw new IllegalArgumentException("Couldn't get code points from: " + line);
                    }
                    i = r;
                }
                for (String s : i) {
                    if (!Emoji.EMOJI_CHARS.contains(s)) {
                        continue;
                    }
                    if (s.equals("\u231A")) {
                        int debug = 0;
                    }
                    FontData old = FONT_DATA.get(s);
                    if (old == null) {
                        FONT_DATA.put(s, key); // only add new values
                    }
                }
            }
            FontData x = FONT_DATA.get("\u231A");
            for (FontData value : FONT_DATA.values()) {
                UnicodeSet keys = FONT_DATA.getSet(value);
                System.out.println(keys.size() + "\t" + value + "\t" + keys);
            }
            UnicodeSet keys = new UnicodeSet(Emoji.EMOJI_CHARS).removeAll(FONT_DATA.keySet());
            System.out.println(keys.size() + "\t" + "Missing" + "\t" + keys);
        }
    }

    public static class Resizing {
        final boolean addBorder;
        final boolean isFlag;
        public Resizing(boolean isFlag, boolean addBorder) {
            this.addBorder = addBorder;
            this.isFlag = isFlag;
        }
        public static final Resizing 
        DEFAULT = new Resizing(false, false),
        FLAG = new Resizing(true, true),
        FLAG_NOBORDER = new Resizing(true, false)
        ;
        @Override
        public boolean equals(Object obj) {
            Resizing other = (Resizing) obj; 
            return addBorder == other.addBorder && isFlag == other.isFlag;
        }
        @Override
        public int hashCode() {
            // TODO Auto-generated method stub
            return Objects.hashCode(addBorder, isFlag);
        }
    }
    
    public static BufferedImage writeResizedImage(String name, BufferedImage sourceImage,
            String outputDir, String outputName, int heightAndWidth) throws IOException {
        return writeResizedImage(name, sourceImage, outputDir, outputName, heightAndWidth, heightAndWidth, Resizing.DEFAULT);
    }
    
    public static BufferedImage writeResizedImage(String name, BufferedImage sourceImage,
            String outputDir, String outputName, int height, int width, Resizing resizing) throws IOException {
        int sourceHeight = sourceImage.getHeight();
        int sourceWidth = sourceImage.getHeight();
        BufferedImage targetImage;
        if (height == sourceHeight && width == sourceWidth && !resizing.equals(Resizing.DEFAULT)) {
            targetImage = sourceImage;
        } else {
            //            System.out.println(name
            //                    + "\t" + sourceImage.getWidth() + ", " + sourceHeight 
            //                    + " => " + outputName + "\t" 
            //                    + targetImage.getWidth() + ", " + targetImage.getHeight());

            targetImage = resizeImage(sourceImage, height, width, resizing);
        }
        writeImage(targetImage, outputDir, outputName, "png");
        //        File outputfile2 = new File(outputDir, "big-" + outputName + ".png");
        //        ImageIO.write(sourceImage, "png", outputfile2);
        //
        return targetImage;
    }

    public static void writeImage(BufferedImage sourceImage, String outputDir,
            String outputName, String fileSuffix) throws IOException {
        File outputfile = new File(outputDir, outputName + "." + fileSuffix);
        ImageIO.write(sourceImage, "png", outputfile);
    }

    public static BufferedImage resizeImage(BufferedImage sourceImage,
            int height, int width, Resizing resizing) {
        int sourceHeight = sourceImage.getHeight();
        int sourceWidth = sourceImage.getWidth();
        BufferedImage targetImage;
        int targetHeight = height - (resizing.addBorder ? 2 : 0);
        int targetWidth = (2 * sourceWidth * targetHeight + 1) / (2 * sourceHeight);
        int x = 0, y = 0;
        if (targetWidth > targetHeight) {
            targetWidth = width - (resizing.addBorder ? 2 : 0);
            targetHeight = (2 * sourceHeight * targetWidth + 1) / (2 * sourceWidth);
            if (targetHeight < 10 * height / 16) {
                targetHeight = 10 * height / 16;
            }
            y = (height - targetHeight + 1) / 2;
//        } else if (targetHeight > 2 * height / 3) {
//            
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

        if (resizing.addBorder) {
            graphics.setPaint(Color.LIGHT_GRAY);
            graphics.fillRect(x, y, targetWidth+2, targetHeight+2);
            x += 1;
            y += 1;
        }
        graphics.drawImage(sourceImage, x, y, x+targetWidth, y+targetHeight,
                0, 0, sourceWidth, sourceHeight,
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

    static UnicodeSet canDisplay(String fontName) {
        Font f = new Font("Zapf Dingbats", 0, 24);
        UnicodeSet result = new UnicodeSet();
        for (String s : Emoji.EMOJI_CHARS) {
            if (canDisplay(f,s)) {
                result.add(s);
            }
        }
        return result;
    }
    static boolean canDisplay(Font f, String codepoint) {
        final FontRenderContext fontRenderContext = new FontRenderContext(null, false, false);
        GlyphVector glyphVector = f.createGlyphVector(fontRenderContext, codepoint);
        int glyphCode = glyphVector.getGlyphCode(0);
        return (glyphCode > 0);
    }

    static byte[] resizeImage(File file, int targetHeight, int targetWidth) {
        try {
            BufferedImage sourceImage = ImageIO.read(file);
            BufferedImage targetImage = sourceImage;
            if (sourceImage.getHeight() != targetHeight || sourceImage.getWidth() != targetWidth) {
                targetImage = resizeImage(sourceImage, targetHeight, targetHeight, Resizing.DEFAULT);
            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageOutputStream ios = new MemoryCacheImageOutputStream(outputStream);
            ImageIO.write(targetImage, "png", ios);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
    static final String[] TAROT = {

    };

    enum TarotSuits {
        SWORDS(
                0x1F0A1, 0x1F0AE,
                "1/1a/Swords01.jpg",
                "9/9e/Swords02.jpg",
                "0/02/Swords03.jpg",
                "b/bf/Swords04.jpg",
                "2/23/Swords05.jpg",
                "2/29/Swords06.jpg",
                "3/34/Swords07.jpg",
                "a/a7/Swords08.jpg",
                "2/2f/Swords09.jpg",
                "d/d4/Swords10.jpg",
                "4/4c/Swords11.jpg",
                "b/b0/Swords12.jpg",
                "d/d4/Swords13.jpg",
                "3/33/Swords14.jpg"
                ),
                CUPS(
                        0x1F0B1, 0x1F0BE,
                        "3/36/Cups01.jpg",
                        "f/f8/Cups02.jpg",
                        "7/7a/Cups03.jpg",
                        "3/35/Cups04.jpg",
                        "d/d7/Cups05.jpg",
                        "1/17/Cups06.jpg",
                        "a/ae/Cups07.jpg",
                        "6/60/Cups08.jpg",
                        "2/24/Cups09.jpg",
                        "8/84/Cups10.jpg",
                        "a/ad/Cups11.jpg",
                        "f/fa/Cups12.jpg",
                        "6/62/Cups13.jpg",
                        "0/04/Cups14.jpg"
                        ),
                        PENTACLES(
                                0x1F0C1, 0x1F0CE,
                                "f/fd/Pents01.jpg",
                                "9/9f/Pents02.jpg",
                                "4/42/Pents03.jpg",
                                "3/35/Pents04.jpg",
                                "9/96/Pents05.jpg",
                                "a/a6/Pents06.jpg",
                                "6/6a/Pents07.jpg",
                                "4/49/Pents08.jpg",
                                "f/f0/Pents09.jpg",
                                "4/42/Pents10.jpg",
                                "e/ec/Pents11.jpg",
                                "d/d5/Pents12.jpg",
                                "8/88/Pents13.jpg",
                                "1/1c/Pents14.jpg"
                                ),
                                WANDS(
                                        0x1F0D1,
                                        0x1F0DE, 
                                        "1/11/Wands01.jpg",
                                        "0/0f/Wands02.jpg",
                                        "f/ff/Wands03.jpg",
                                        "a/a4/Wands04.jpg",
                                        "9/9d/Wands05.jpg",
                                        "3/3b/Wands06.jpg",
                                        "e/e4/Wands07.jpg",
                                        "6/6b/Wands08.jpg",
                                        "e/e7/Wands09.jpg",
                                        "0/0b/Wands10.jpg",
                                        "6/6a/Wands11.jpg",
                                        "1/16/Wands12.jpg",
                                        "0/0d/Wands13.jpg",
                                        "c/ce/Wands14.jpg"),
                                        MAJOR(
                                                0x1F0E0,
                                                0x1F0F5,
                                                "9/90/RWS_Tarot_00_Fool.jpg",
                                                "d/de/RWS_Tarot_01_Magician.jpg",
                                                "8/88/RWS_Tarot_02_High_Priestess.jpg",
                                                "d/d2/RWS_Tarot_03_Empress.jpg",
                                                "c/c3/RWS_Tarot_04_Emperor.jpg",
                                                "8/8d/RWS_Tarot_05_Hierophant.jpg",
                                                "d/db/RWS_Tarot_06_Lovers.jpg",
                                                "9/9b/RWS_Tarot_07_Chariot.jpg",
                                                "f/f5/RWS_Tarot_08_Strength.jpg",
                                                "4/4d/RWS_Tarot_09_Hermit.jpg",
                                                "3/3c/RWS_Tarot_10_Wheel_of_Fortune.jpg",
                                                "e/e0/RWS_Tarot_11_Justice.jpg",
                                                "2/2b/RWS_Tarot_12_Hanged_Man.jpg",
                                                "d/d7/RWS_Tarot_13_Death.jpg",
                                                "f/f8/RWS_Tarot_14_Temperance.jpg",
                                                "5/55/RWS_Tarot_15_Devil.jpg",
                                                "5/53/RWS_Tarot_16_Tower.jpg",
                                                "d/db/RWS_Tarot_17_Star.jpg",
                                                "7/7f/RWS_Tarot_18_Moon.jpg",
                                                "1/17/RWS_Tarot_19_Sun.jpg",
                                                "d/dd/RWS_Tarot_20_Judgement.jpg",
                                                "f/ff/RWS_Tarot_21_World.jpg"
                                                );
        private static final String PREFIX = "http://upload.wikimedia.org/wikipedia/en/";
        private final int min;
        private final int max;
        private final int start;
        private final String[] urls;

        TarotSuits(int min, int max, String... names) {
            this.min = min;
            this.max = max;
            urls = names;
            start = min == 0x1F0E0 ? 0 : 1;
        }
        public String getUrl(int cp) {
            int offset = cp - min;
            if (offset >= 0 && cp <= max){
                return PREFIX + urls[offset];
            }
            return null;
        }
        public static void makeTest() throws IOException {
            PrintWriter out = BagFormatter.openUTF8Writer("/Users/markdavis/workspace/temp/", "tarot.html");
            out.println("<html><body><table>");
            for (TarotSuits x : values()) {
                int counter = x.start;
                for (int i = x.min; i <= x.max; ++i) {
                    out.println("<tr>" +
                            "<td>" + x + "</td>" +
                            "<td>" + counter++ + "</td>" +
                            "<td>" + Utility.hex(i) + "</td>" +
                            "<td><img width='72' height='72' src='" + x.getUrl(i) + "'></td>" +
                            "</tr>"
                            );
                }
            }
            out.println("</table></body></html>");
            out.close();
        }
    }

}