package org.unicode.tools.emoji;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.awt.image.RenderedImage;
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
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.With;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Binary;
import org.unicode.text.tools.GifSequenceWriter;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.GmailEmoji.Data;

import com.google.common.base.Objects;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.lang.UScript.ScriptUsage;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.Transform;
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

    static final Transform<String,String> TWITTER_URL = new Transform<String,String>() {
        public String transform(String s) {
            StringBuilder result = new StringBuilder("https://abs.twimg.com/emoji/v1/72x72/");
            boolean first = true;
            for (int cp : With.codePointArray(s)) {
                if (first) {
                    first = false;
                } else {
                    result.append("-");
                }
                result.append(Integer.toHexString(cp));
            }
            return  result.append(".png").toString();
        }
    };

    static final UnicodeSet APPLE_LOCAL = new UnicodeSet("[🌠 🔈 🚋{#⃣}{0⃣}{1⃣}{2⃣}{3⃣}{4⃣}{5⃣}{6⃣}{7⃣}{8⃣}{9⃣}]").freeze();

    static final Transform<String,String> APPLE_URL = new Transform<String,String>() {
        public String transform(String s) {
            StringBuilder result = 
                    new StringBuilder(
                            LoadImage.APPLE_LOCAL.containsAll(s) ? "images/apple-extras/apple-" 
                                    : "http://emojistatic.github.io/images/64/");
            boolean first = true;
            for (int cp : With.codePointArray(s)) {
                if (first) {
                    first = false;
                } else {
                    result.append("-");
                }
                result.append(com.ibm.icu.impl.Utility.hex(cp).toLowerCase(Locale.ENGLISH));
            }
            return  result.append(".png").toString();
        }
    };

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

    static String inputDir = Settings.Output.GEN_DIR + "emoji_images/";
    static String outputDir = Settings.Output.GEN_DIR+ "images/";

    public static void main(String[] args) throws IOException {
        
        doAnimatedGif(false, 72, 50);
        if (true) return;

        IndexUnicodeProperties iup = IndexUnicodeProperties.make();
        UnicodeSet extpict = iup.loadEnumSet(UcdProperty.Emoji, Binary.Yes);
        generatePngsFromFont(outputDir, "plain", "plain", "Apple Color Emoji",
                extpict, 72, false);

        if (true) return;
        UnicodeSet hiero = new UnicodeSet("[:Script=Egyptian_Hieroglyphs:]").freeze();
        generatePngsFromFont(outputDir, "noto", "noto", "Noto Sans Egyptian Hieroglyphs", 
                hiero, 72, false);

//        doAnimatedGif(false, 72, 50);
        final CandidateData candidateData = CandidateData.getInstance();
        UnicodeSet u9 = candidateData.getCharacters();
//        UnicodeSet u9 = new UnicodeSet(
//                "[[\\U0001F6D2\\U0001F6F6\\U0001F927\\U0001F938-\\U0001F93E\\U0001F941\\U0001F943-\\U0001F94B\\U0001F956-\\U0001F959\\U0001F98B-\\U0001F98F]"
//                        + "[\\U0001F95A-\\U0001F95E\\U0001F990\\U0001F991]]");
        Map<String, Image> generated = generatePngsFromFont(outputDir, "proposed", "proposed", "Source Emoji", u9, 72, false); // "Symbola"
        System.out.println("Characters");
        for (String u : u9) {
            System.out.println(Utility.hex(u) 
                    + "\t" + u 
                    + "\t" + (generated.containsKey(u) ? "✔" :  "❌")
                    + "\t" + EmojiData.EMOJI_DATA.getName(u));
        }
        if (true) return;


        //UnicodeSet missing = new UnicodeSet("[✊ ✋ ✨  ✅ ❌ ❎ ➕ ➖ ➗ ➰ ➿  ❓ ❔ ❕]");
        //generatePngsFromFont(outputDir, outputDir, "ref", "ref", "Symbola", Emoji.U80, 72, true); // "Symbola"
        //generatePngsFromFont(outputDir, "apple", "apple", "Apple Color Emoji", Emoji.APPLE_MODIFIED, 72, false);
        //        for (String s : Emoji.APPLE_COMBOS) {
        //            System.out.print(s + " ");
        //        }
        //        System.out.println();
        //        System.out.println(Emoji.APPLE_COMBOS.size());
        //        for (String s : EmojiData.APPLE_MODIFIED) {
        //            System.out.print(s + " ");
        //        }
        //        System.out.println();
        //        System.out.println(EmojiData.APPLE_MODIFIED.size());

        //doAnimatedGif(false, 72);


        doRef(inputDir, outputDir);
        doTwitter(inputDir, outputDir);
        doRef(inputDir, outputDir);
        writeCharSamples(true);
        EmojiFlagOrder.getFlagOrder();
        TarotSuits.makeTest();
        doGmail(outputDir);
        doKddi(outputDir);
        doDoCoMo(outputDir);
        doSb(outputDir);

        //getAppleNumbers();
        UnicodeSet dingbats = canDisplay("Zapf Dingbats");
        System.out.println(dingbats);

        //        UnicodeSet missing = new UnicodeSet("[🌭🌮🌯🍾🍿🏏🏐🏑🏒🏓🏸🏹🏺🏻🏼🏽🏾🏿📿🕋🕌🕍🕎🙃🙄🛐🤀🤐🤑🤒🤓🤔🤕🤖🤗🤘🦀🦁🦂🦃🦄🧀]");
        //UnicodeSet missing = new UnicodeSet("[✊ ✋ ✨  ✅ ❌ ❎ ➕ ➖ ➗ ➰ ➿  ❓ ❔ ❕]");
        generatePngsFromFont(outputDir, "ref", "ref", "Symbola", Emoji.U80, 144, false); // "Symbola"
        // doAnimatedGif();

        if (false) {
            UnicodeSet result = checkCanDisplay(new Font("Symbola",0,24));
            System.out.println(result.toPattern(false));
            List<BufferedImage> list;
            UnicodeSet s = new UnicodeSet("[▫◻◼◽◾☀⚪⚫❗⤴⤵⬅⬆⬇⬛⬜⭐⭕〽]");
            Map<String, Image> map = generatePngsFromFont(outputDir, null, "android", "AndroidEmoji", s, 144, false); // "Symbola"
            list = doAndroid(inputDir, outputDir);
            doWindows(inputDir, outputDir);
            doRef(inputDir, outputDir);
            doTwitter(inputDir, outputDir);
            //doGitHub(inputDir, outputDir);
            //List<BufferedImage> list = doSymbola(inputDir, outputDir, "Apple Emoji", SYMBOLA, 144); // "Symbola"
            createAnimatedImage(new File(outputDir, "animated-symbola.gif"), list, 1, false);
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

    //    private static void getAppleNumbers() throws IOException {
    //        Set<String> codepointOrder = new TreeSet(new UTF16.StringComparator());
    //        UnicodeSet found = new UnicodeSet("[©®🌠🔈🚃🚋🔊🔉{#⃣}{0⃣}{1⃣}{2⃣}{3⃣}{4⃣}{5⃣}{6⃣}{7⃣}{8⃣}{9⃣}]")
    //        .addAll(Emoji.GITHUB_APPLE_CHARS);
    //        for (String s : found) {
    //            codepointOrder.add(s);
    //        }
    //        Set<Integer> skipSet = new HashSet();
    //        skipSet.add(129);
    //
    //        int i = 1;
    //        String newDir = Settings.OTHER_WORKSPACE_DIRECTORY + "DATA/AppleEmoji/";
    //        final int maxNew = 846;
    //        String oldDir = Settings.SVN_WORKSPACE_DIRECTORY + "/reports/tr51/images/apple/";
    //        PrintWriter out = FileUtilities.openUTF8Writer(Settings.OTHER_WORKSPACE_DIRECTORY + "Generated/images", "checkApple.html");
    //        out.println("<html><body><table>");
    //        for (String s : codepointOrder) {
    //            while (skipSet.contains(i)) {
    //                ++i;
    //            }
    //            writeAppleLine(out, newDir, i, oldDir, s);
    //            ++i;
    //        }
    //        for (;i <= maxNew; ++i) {
    //            writeAppleLine(out, newDir, i, oldDir, "");
    //        }
    //        out.println("</table></body></html>");
    //        out.close();
    //    }

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

    public static UnicodeSet checkCanDisplay(Font f) {
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

    public static void doAnimatedGif(boolean onlySize, int size, int milliGap, Emoji.Source... ordering) throws IOException {
        final File output = new File(outputDir, "animated-emoji.gif");
        System.out.println(output.getCanonicalPath());
        List<BufferedImage> list = new ArrayList<>();
        BufferedImage lastTargetImage = null;
        BufferedImage firstTargetImage = null;
        UnicodeSet skipping = new UnicodeSet();
        for (Entry<String, Collection<String>> entry : EmojiOrder.STD_ORDER.orderingToCharacters.asMap().entrySet()) {
            final String key = entry.getKey();
            System.out.println("\t" + key);
            //if (!key.contains("time")) continue;
            for (String chars : entry.getValue()) {
                File file = Emoji.getBestFile(chars, ordering);
                if (file == null) {
                    skipping.add(chars);
                    continue;
                }
                BufferedImage sourceImage = ImageIO.read(file);
                System.out.println(chars + "\t" + sourceImage.getWidth() + "\t" + sourceImage.getHeight());
                if (onlySize) continue;
                BufferedImage targetImage;
                if (false && size == sourceImage.getHeight()) {
                    targetImage = sourceImage;
                } else {
                    targetImage = resizeImage(sourceImage, size, size, Resizing.DEFAULT);
                }
                if (lastTargetImage != null) {
                    mix(lastTargetImage, targetImage, list);
                }
                lastTargetImage = targetImage;
                if (firstTargetImage == null) {
                    firstTargetImage = targetImage;
                }
                list.add(targetImage);
            }
        }
        mix(lastTargetImage, firstTargetImage, list);
        if (onlySize) return;
        createAnimatedImage(output, list, milliGap, true);
        System.out.println("Image created");
        System.out.println("Skipped " + skipping.toPattern(false));

    }

    private static void mix(BufferedImage image, BufferedImage overlay, List<BufferedImage> list) {
        // create the new image, canvas size is the max. of both image sizes
        int w = Math.max(image.getWidth(), overlay.getWidth());
        int h = Math.max(image.getHeight(), overlay.getHeight());
        int rule = AlphaComposite.SRC_OVER;
        for (int i = 2; i > 0; --i) {
            BufferedImage combined = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            //        float[] scales = { 1f, 1f, 1f, 0.5f };
            //        float[] offsets = new float[4];
            //        RescaleOp rop = new RescaleOp(scales, offsets, null);

            // paint both images, preserving the alpha channels
            Graphics2D g = (Graphics2D) combined.getGraphics();
            //g.drawImage(foo , rop, 0, 0);
            g.drawImage(image, 0, 0, null);
            Composite comp2 = AlphaComposite.getInstance(rule , (float)((3-i)/3.0) );
            g.setComposite(comp2);
            g.drawImage(overlay, 0, 0, null);
            list.add(combined);
        }
    }

    //    public static void doGitHub(String inputDir, String outputDir)
    //            throws IOException {
    //        for (String s : Emoji.GITHUB_APPLE_CHARS) {
    //            String url = LoadImage.APPLE_URL.transform(s);
    //            String core = Emoji.buildFileName(s, "_");
    //            System.out.println(core);
    //            BufferedImage sourceImage = ImageIO.read(new URL(url));
    //            BufferedImage targetImage = writeResizedImage(sourceImage, outputDir + "/apple", "apple_" + core, 72);
    //        }
    //    }

    final static UnicodeSet NON_SYMBOLA = new UnicodeSet("[🅰🆎🅱🆑🆒🆓🆔🆕🆖🅾🆗🅿🆘🆙🆚🆏🈁🈂🈹🉑🈴🈺🉐🈯🈷🈶🈵🈚🈸🈲🈳]");

    private static final boolean DEBUG = false;

    //private static final UnicodeSet SYMBOLA = new UnicodeSet(Emoji.EMOJI_CHARS).removeAll(NON_SYMBOLA).freeze();

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

    public static void generatePngsFromFonts(String outputDir, String dir, 
            String prefix, UnicodeSet requested, int height, boolean useFonts) throws IOException {
        GraphicsEnvironment e = GraphicsEnvironment.getLocalGraphicsEnvironment();
        System.out.println("\n\tRemaining:\t" + requested.size());
        Set<String> families = new TreeSet<>(Arrays.asList(e.getAvailableFontFamilyNames()));
        for (Iterator<String> it = families.iterator(); it.hasNext();) {
            String font = it.next();
            if (font.contains("Color")) {
                it.remove();
            }
        }
        Set<String> orderedFamilies = new LinkedHashSet<>();
        if (families.contains("Noto Sans Symbols")) {
            orderedFamilies.add("Noto Sans Symbols");
        }
        addIfContains("Dingbats", families, orderedFamilies);
        addIfContains("Noto Sans Symbols", families, orderedFamilies);
        addIfContains("Symbol", families, orderedFamilies);
        addIfContains("Noto", families, orderedFamilies);
        addIfContains("Unicode", families, orderedFamilies);
        addIfContains("Menlo", families, orderedFamilies);
        orderedFamilies.addAll(families);
        for (String font : orderedFamilies) {
                Map<String, Image> results = generatePngsFromFont(outputDir, dir, prefix, font, requested, height, useFonts);
                UnicodeSet found = new UnicodeSet().addAll(results.keySet());
                System.out.println(font + ":\t" + found.size() + "\t" + found.toPattern(false));
               if (found.isEmpty()) {
                    continue;
                }
                requested.removeAll(found);
                System.out.println("\tRemaining:\t" + requested.size());
                if (requested.isEmpty()) {
                    break;
                }
        }
        System.out.println("Missing: " + requested.toPattern(false));
    }

    private static void addIfContains(String string, Set<String> families, Set<String> orderedFamilies) {
        for (String font : families) {
            if (font.contains(string)) {
                orderedFamilies.add(font);
            }
        }
    }
    public static Map<String, Image> generatePngsFromFont(String outputDir, String dir, 
            String prefix, String font, UnicodeSet requested, int height, boolean useFonts)
                    throws IOException { // 🌰-🌵
        if (dir == null) {
            dir = prefix;
        }
        HashMap<String, Image> result = new LinkedHashMap<>();
        Set<String> sorted = requested.addAllTo(new TreeSet<String>());
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
        FontMetrics metrics = font == null ? graphics.getFontMetrics() : setFont(font, height, graphics);
        //UnicodeSet firstChars = new UnicodeSet();
        String fileDirectory = outputDir + "/" + prefix;
        boolean foundOne = false;
        for (final String s : sorted) { //
            if (useFonts) {
                UnicodeFontData font1 = UnicodeFontData.getFont(s);
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
            if (!foundOne) {
                System.out.println("Writing from «" + font + "» in: " + fileDirectory);
                foundOne = true;
            }
            String core = Emoji.buildFileName(s, "_");
            String filename = prefix + "_" + core;
            //File outputfile = new File(fileDirectory, filename + ".png");
            graphics.clearRect(0, 0, width, height);
            Rectangle2D bounds = metrics.getStringBounds(s, graphics);
            boolean reset = false;
            if (bounds.getWidth() > width || metrics.getAscent() + metrics.getDescent() > height) {
                int height3 = (int)(height*width/bounds.getWidth()+0.5);
                int height4 = height*height/(metrics.getAscent() + metrics.getDescent());
                height3 = Math.min(height3, height4);
                metrics = setFont(font, height3, graphics);
                bounds = metrics.getStringBounds(s, graphics);
                reset = true;
            }
            FontRenderContext frc = graphics.getFontRenderContext();
            GlyphVector gv = graphics.getFont().createGlyphVector(frc, s);
            Rectangle pixelBounds = gv.getPixelBounds(frc, 0, 0);
            {
                final int width2 = (int) bounds.getWidth();
                if (DEBUG) System.out.println("ascent: " + metrics.getAscent() 
                        + "; descent: " + metrics.getDescent() 
                        + "; width: " + width2
                        + "; lsb: " + pixelBounds.x
                        + "; rsb: " + (width2 - pixelBounds.width - pixelBounds.x)
                        + "; pixel ascent: " + -pixelBounds.y
                        + "; pixel descent: " + (pixelBounds.height + pixelBounds.y)
                        );
            }
            int xStart = (int)(width - bounds.getWidth()+0.5)/2;
            int yStart = (int)(height - bounds.getHeight() + 0.5)/2 + metrics.getAscent();
            graphics.setComposite(AlphaComposite.Src);
            graphics.drawString(s, xStart, yStart);
            if (reset) {
                metrics = setFont(font, height, graphics);
            }
            String url = LoadImage.APPLE_URL.transform(s);
            Resizing resizing = Resizing.DEFAULT;
            int sourceHeight = sourceImage.getHeight();
            int sourceWidth = sourceImage.getHeight();
            BufferedImage targetImage;
            if (height == sourceHeight && height == sourceWidth && !resizing.equals(Resizing.DEFAULT)) {
                targetImage = sourceImage;
            } else {
                targetImage = resizeImage(sourceImage, height, height, resizing);
            }
            targetImage = TransformGrayToTransparency(targetImage);
            writeImage(targetImage, fileDirectory, filename, "png");
            result.put(s, deepCopy(sourceImage));
            //System.out.println(core + "\t" + s);
        }
        return result;
    }

    static final ImageFilter filter = new RGBImageFilter(){
      public final int filterRGB(int x, int y, int rgb) {
          // white (FF) => 0, 
          // (rgb << 8) & 0xFF000000
        int result = (rgb & 0xFFFFFF) | ((~rgb << 8) & 0xFF000000);
        return result;
      }
    };

    private static BufferedImage TransformGrayToTransparency(BufferedImage image) {
      ImageProducer ip = new FilteredImageSource(image.getSource(), filter);
      return toBufferedImage(Toolkit.getDefaultToolkit().createImage(ip));
    }
    
    public static BufferedImage toBufferedImage(Image img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        }

        // Create a buffered image with transparency
        BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

        // Draw the image on to the buffered image
        Graphics2D bGr = bimage.createGraphics();
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();

        // Return the buffered image
        return bimage;
    }

    public static FontMetrics setFont(String font, int height2,
            Graphics2D graphics) {
        Font myFont;
        if (font != null) {
            myFont = new Font(font, 0, height2);
        } else {
            myFont = graphics.getFont().deriveFont(height2);
        }
        if (!font.equals(myFont.getName())) {
            System.out.println("Accessing " + myFont.getName() + " when " + font + " was requested.");
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
        String inputDirectory = Settings.Output.GEN_DIR + "twemoji/72x72/";
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
            BufferedImage targetImage = writeResizedImage(sourceImage, outputDir + "/twitter", "twitter_" + core, 72);
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
            BufferedImage targetImage = writeResizedImage(sourceImage, outputDir + "/android", "android_" + core, 72);
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
            BufferedImage targetImage = writeResizedImage(sourceImage, outputDir + "/windows", "windows_" + core, 72);
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
            Set<String> set = FLAG_REAL_TO_ALIASES.get(core1);
            doFlags2(file.getParentFile(), core1, core1, outputDir);
            if (set != null) {
                for (String alias : set) {
                    doFlags2(file.getParentFile(), core1, alias, outputDir);
                }
            }
        }
    }

    private static void doFlags2(File parent, String inputRegionCode, String alias, String outputDir) throws IOException {
        Resizing addBorder = !inputRegionCode.equals("NP") ? Resizing.FLAG : Resizing.FLAG_NOBORDER;
        String outputName = Emoji.buildFileName(Emoji.getHexFromFlagCode(alias), "_");
        // emoji_u00a9.png
        File input = new File(parent,inputRegionCode + ".png");
        File output = new File(outputDir + "/ref", "ref_" + outputName);
        if (true) {
            System.out.println(input + " => " + output);
        }
        BufferedImage sourceImage;
        try {
            sourceImage = ImageIO.read(input);
        } catch (Exception e) {
            throw new IllegalArgumentException(input.toString());
        }
        //BufferedImage sourceImage = ImageIO.read(new URL("http://abs.twimg.com/emoji/v1/72x72/23e9.png"));
        BufferedImage targetImage = writeResizedImage(sourceImage, outputDir + "/ref", "ref_" + outputName, 108, 108, addBorder);
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

    public static BufferedImage writeResizedImage(BufferedImage sourceImage, String outputDir,
            String outputName, int heightAndWidth) throws IOException {
        return writeResizedImage(sourceImage, outputDir, outputName, heightAndWidth, heightAndWidth, Resizing.DEFAULT);
    }

    public static BufferedImage writeResizedImage(BufferedImage sourceImage, String outputDir,
            String outputName, int height, int width, Resizing resizing) throws IOException {
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

    public static File writeImage(RenderedImage sourceImage, String outputDir,
            String outputName, String fileSuffix) throws IOException {
        File outputfile = new File(outputDir, outputName + "." + fileSuffix);
        ImageIO.write(sourceImage, "png", outputfile);
        return outputfile;
    }

    public static BufferedImage resizeImage(BufferedImage sourceImage,
            int imageHeight, int imageWidth, Resizing resizing) {
        int sourceHeight = sourceImage.getHeight();
        int sourceWidth = sourceImage.getWidth();
        BufferedImage targetImage;
        int borderWidth = imageWidth - (resizing.addBorder ? 2 : 0);
        int borderHeight = imageHeight - (resizing.addBorder ? 2 : 0);
        int targetHeight = borderHeight;
        int targetWidth = (2 * sourceWidth * targetHeight + 1) / (2 * sourceHeight);
        if (resizing.isFlag) {
            if (targetWidth > targetHeight) {
                targetWidth = borderWidth;
                targetHeight = (2 * sourceHeight * targetWidth + 1) / (2 * sourceWidth);
                if (targetHeight * 3 < 2 * borderHeight) {
                    targetHeight = (4 * borderHeight + 3) / 6;
                }
            } else if (targetHeight * 4 > 3 * borderHeight) {
                targetHeight = (3 * borderHeight + 2) / 4  - (resizing.addBorder ? 2 : 0);
                targetWidth = (2 * sourceWidth * targetHeight + 1) / (2 * sourceHeight);
            }
        }

        targetImage = new BufferedImage(imageWidth, imageHeight, IMAGE_TYPE);

        // https://abs.twimg.com/emoji/v1/72x72/231a.png

        Graphics2D graphics = targetImage.createGraphics();
        graphics.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setBackground(Color.WHITE);
        graphics.clearRect(0, 0, imageWidth, imageHeight);

        int x = (imageWidth - targetWidth + 1) / 2;
        int y = (imageHeight - targetHeight + 1) / 2;
        if (resizing.addBorder) {
            graphics.setPaint(Color.LIGHT_GRAY);
            graphics.fillRect(x-1, y-1, targetWidth+2, targetHeight+2);
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
        for (String s : EmojiData.EMOJI_DATA.getChars()) {
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
            PrintWriter out = FileUtilities.openUTF8Writer(Settings.Output.GEN_DIR + "temp/", "tarot.html");
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
    static Relation<String,String> FLAG_REAL_TO_ALIASES = Relation.of(new HashMap(), HashSet.class);
    static {
        addAlias("BL", "FR");
        addAlias("BV", "NO");
        addAlias("GF", "FR");
        addAlias("HM", "AU");
        addAlias("MF", "FR");
        addAlias("RE", "FR");
        addAlias("SJ", "NO");
        addAlias("TF", "FR");
        addAlias("UM", "US");
        addAlias("WF", "FR");
        addAlias("YT", "FR");    
        FLAG_REAL_TO_ALIASES.freeze();
    }
    private static void addAlias(String alias, String real) {
        FLAG_REAL_TO_ALIASES.put(real, alias);
    }
}
