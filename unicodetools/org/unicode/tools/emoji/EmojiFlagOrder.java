package org.unicode.tools.emoji;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.imageio.ImageIO;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Pair;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

import com.google.common.collect.ComparisonChain;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.LocaleDisplayNames;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.ULocale;

public class EmojiFlagOrder {

    static class HSB implements Comparable<HSB> {
        short red;
        short green;
        short blue;

        private HSB(int red, int green, int blue) {
            this.red = (short)red;
            this.green = (short)green;
            this.blue = (short)blue;
        }
        @Override
        public String toString() {
            return Utility.hex(red,2) + Utility.hex(green,2) + Utility.hex(blue,2);
        }
        @Override
        public int compareTo(HSB o) {
            int diff;
            if (0 != (diff = red - o.red)) return diff;
            if (0 != (diff = green - o.green)) return diff;
            return blue - o.blue;
        }

        static Map<Integer,HSB> cache = new HashMap<>();

        public static HSB make(int red, int green, int blue) {
            red = fix(red);
            green = fix(green);
            blue =fix(blue);
            final int key = (red<<16)|(green<<8)|blue;
            HSB result = cache.get(key);
            if (result == null) {
                result = new HSB(red, green, blue);
                cache.put(key, result);
            }
            return result;
        }

        private static int fix(int red) {
            return red & 0xFF;
        }

        double getDLuminance() {
            return Math.sqrt(0.299d * red * red + 0.587d * green * green + 0.114d * blue * blue);
        }

        public int distanceTo(HSB other) {
            if (other == null || other == this) {
                return 0;
            }
            final int s0 = RED_FACTOR * (red - other.red);
            final int s1 = GREEN_FACTOR * (green - other.green);
            final int s2 = BLUE_FACTOR * (blue - other.blue);
            int result = s0*s0 + s1*s1 + s2*s2;
            return result;
        }

        private static final int BLUE_FACTOR = 19;
        private static final int GREEN_FACTOR = 183;
        private static final int RED_FACTOR = 54;

        int getLuminance() {
            // red, green, blue are 0..255
            return (RED_FACTOR * red + GREEN_FACTOR * green + BLUE_FACTOR * blue) >> 8;
        }

        public HSB getContrast() {
            int lum = getLuminance();
            int contrast = lum < 0x6F ? lum + 0x40 : lum - 0x40;
            return make(contrast, contrast, contrast);
        }
    }

    static class ImageInfo implements Comparable<ImageInfo> {
        private String name;
        private long overall;
        private long count;
        private Set<R2<Long, HSB>> colorDistribution;
        static int order = 0;
        private int currentOrder = ++order;
        /**
         * returns hue, saturation, brightness
         * @param image
         * @return
         */
        ImageInfo (String name, BufferedImage image) {
            this.name = name;
            if (image == null) {
                return;
            }
            final WritableRaster raster = image.getRaster();
            //        final SampleModel sampleModel = raster.getSampleModel();
            //        System.out.println(sampleModel);
            final ColorModel colorModel = image.getColorModel();
            //        final DataBuffer dataBuffer = raster.getDataBuffer();
            //        final byte[][] pixels = ((DataBufferByte) dataBuffer).getBankData();
            //        final boolean hasAlphaChannel = image.getAlphaRaster() != null;
            //        System.out.println(hasAlphaChannel + ", "
            //                + image.getWidth() + ", "
            //                + image.getHeight() + ", "
            //                + image.getWidth() * image.getHeight() + ", "
            //                + pixels.length);
            count = 0;
            final int yCount = image.getHeight();
            final int xCount = image.getWidth();
            HSB[][] colors = new HSB[xCount][];
            Counter<HSB> _colorDistribution = new Counter();
            long lumTotal = 0;
            for (int x = 0; x < xCount; ++x) {
                colors[x] = new HSB[yCount];
                for (int y = 0; y < yCount; ++y) {
                    //int rgb = image.getRGB(x, y);
                    HSB hsbvals = getHSB(x, y, raster, colorModel);
                    if (hsbvals == null) {
                        continue;
                    }
                    _colorDistribution.add(hsbvals, 1);
                    colors[x][y] = hsbvals;
                    lumTotal += hsbvals.getLuminance();
                    ++count;
                }
            }
            colorDistribution = _colorDistribution.getEntrySetSortedByCount(false, null);
            // get diffs
            long distance = 0;
            for (int x = 1; x < xCount; ++x) {
                for (int y = 1; y < yCount; ++y) {
                    HSB base = colors[x][y];
                    if (base == null) {
                        continue;
                    }
                    //                if (base.distanceTo(colors[x-1][y-1]) > MIN
                    //                        || base.distanceTo(colors[x-1][y]) > MIN
                    //                        || base.distanceTo(colors[x][y-1]) > MIN
                    //                        || x < xCount - 1 && base.distanceTo(colors[x+1][y]) > MIN 
                    //                        ) {
                    //                    distance++;
                    //                }
                    int core = Math.max(base.distanceTo(colors[x-1][y-1]),
                            Math.max(base.distanceTo(colors[x-1][y]),
                                    Math.max(base.distanceTo(colors[x][y-1]),
                                            + (x < xCount - 1 ? base.distanceTo(colors[x+1][y]) : 0))));
                    distance += core;
                    //                if (core > MIN) {
                    //                    distance += 1;
                    //                }
                }
            }
            //            overall = Math.round(distance/(double)count);
            overall = -lumTotal*1000/count;

        }
        @Override
        public int compareTo(ImageInfo o) {
            return ComparisonChain.start()
                    .compare(name,o.name, EmojiOrder.UCA_COLLATOR)
                    .compare(overall, o.overall)
                    .compare(currentOrder, o.currentOrder)
                    .result();
        }
    }
    public static HSB getHSB(int x, int y, final WritableRaster raster, final ColorModel colorModel) {
        Object inData = raster.getDataElements(x, y, null);
        int alpha = colorModel.getAlpha(inData);
        if (alpha < 32) {
            return null; // skip
        } else if (alpha != 255) {
            //throw new IllegalArgumentException();
        }
        int blue = colorModel.getBlue(inData);
        int green = colorModel.getGreen(inData);
        int red = colorModel.getRed(inData);
        HSB hsbvals = HSB.make(red, green, blue);
        return hsbvals;
    }


    static void getFlagOrder() throws IOException {
        Set<Pair<ImageInfo,String>> sorted = new TreeSet<>();
        LocaleDisplayNames localeDisplayNames = LocaleDisplayNames.getInstance(ULocale.ENGLISH);
        for (String s : EmojiData.EMOJI_DATA.getChars()) {
            if (!Emoji.isRegionalIndicator(s.codePointAt(0))) {
                continue;
            }
            if (s.equals("🇩🇪")) {
                int debug = 0;
            }
            File file = new File(getFile(s));
            BufferedImage sourceImage;
            try {
                sourceImage = ImageIO.read(file);
            } catch (Exception e) {
                System.out.println("Can't read: " + file);
                sourceImage = null;
            }
            String sortString = localeDisplayNames.regionDisplayName(Emoji.getRegionCodeFromEmoji(s));
            ImageInfo info = new ImageInfo(sortString, sourceImage);
            sorted.add(new Pair<>(info,s));
        }
        NumberFormat percent = NumberFormat.getPercentInstance();
        percent.setMaximumFractionDigits(2);
        percent.setMinimumFractionDigits(2);
        NumberFormat percent2 = NumberFormat.getPercentInstance();
        double base = -1;
        StringBuilder emojiList = new StringBuilder();
        final String outFileName = "flag-emoji-list.html";
        try (PrintWriter out = FileUtilities.openUTF8Writer(Emoji.CHARTS_DIR, outFileName)) {
            ChartUtilities.writeHeader(outFileName, out, "Emoji Flags", null, false, "<p>" + "Flag list. " + "</p>\n", Emoji.DATA_DIR_PRODUCTION, Emoji.TR51_HTML);
            out.println("<table " + "border='1'" + ">");
            out.println("<html><body><table border='1px'>");
            for (Pair<ImageInfo, String> colorChar : sorted) {
                final ImageInfo info = colorChar.getFirst();
                final String codePoints = colorChar.getSecond();
                emojiList.append(codePoints).append(' ');
                if (base < 0) {
                    base = info.overall;
                }
                //                File file = getFile(codePoints);
                out.print("<tr><td>" + codePoints 
                        + "</td><td>" + Emoji.getFlagRegionName(codePoints)
                        + "</td><td padding='2px'>" + getFlag(codePoints)
                        + "</td><td>" + percent2.format(info.overall/base)
                        + "</td>");
                int limit = 15;
                if (info.colorDistribution != null) {
                    for (R2<Long, HSB> item : info.colorDistribution) {
                        HSB color = item.get1();
                        HSB inverse = color.getContrast();
                        out.println("<td style='background-color:#" + color.toString() +
                                ";color:#" + inverse.toString() +
                                "'>" + percent.format(item.get0()/(double)info.count) + " " + item.get1() + "</td>");
                        if (--limit < 0) break;
                    }
                }
                out.println("</tr>");
            }
            out.println("</table>");
            out.println("<p>country-flag\t" + emojiList + "<p>");
            out.println("</body></html>");
            out.println("</table>");
            ChartUtilities.writeFooter(out);
        }
    }

    static String getFlag(String chars) {
        //String core = Emoji.buildFileName(chars,"_");
        return "<img"
        + " alt='" + chars + "'"
        + " class='big'"
        + " src='" + getFile(chars) + "'>";
    }


    public static String getFile(String s) {
        String core = Emoji.buildFileName(s,"_");
        return Settings.OTHER_WORKSPACE_DIRECTORY + "DATA/emoji_images/country-flags/ref_" +
        core +
        ".png";
    }

    public static void main(String[] args) throws IOException {
        getFlagOrder();
    }
}
