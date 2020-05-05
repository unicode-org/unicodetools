package org.unicode.draft;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.text.utility.Settings;

import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;


public class LanguageQuadgrams {

    public static void main(String[] args) throws IOException {
        final BufferedReader in = FileUtilities.openUTF8Reader(Settings.BASE_DIRECTORY + "Downloads/", "languageQuadgrams.txt");
        while (true) {
            final String line = in.readLine();
            if (line == null) {
                break;
            }
            final String[] parts = line.split("\\s+");
            add(parts[0], Integer.parseInt(parts[1],16), Byte.parseByte(parts[2]));
        }
        in.close();

        // normalize to sum to 1.0

        for (final String lang : languageToLanguageInfo.keySet()) {
            final Map<Integer, Blur> x = languageToLanguageInfo.get(lang);

            // get the sum
            Blur sum = new Blur(0,0);
            for (final Integer quad : x.keySet()) {
                sum = sum.add(x.get(quad));
            }
            System.out.println(lang + "\t" + sum);

            // add the zero value
            add(lang, 0, (byte)0);

            // normalize to 1.0
            for (final Integer quad : x.keySet()) {
                x.put(quad, x.get(quad).divideBy(sum.getMax()));
            }
            //      sum = 0;
            //      for (Integer quad : x.keySet()) {
            //        sum += x.get(quad);
            //      }
            //      System.out.println("\t" + lang + "\t" + sum);
        }

        final Relation<Blur, Row.R3<String,String,String>> distanceToPair = new Relation(new TreeMap(), TreeSet.class);

        // compare languages
        final Set<String> languagesDone = new HashSet<String>();

        for (final String lang : languageToLanguageInfo.keySet()) {
            System.out.println(lang);
            final Map<Integer, Blur> quad1 = languageToLanguageInfo.get(lang);
            for (final String lang2 : languageToLanguageInfo.keySet()) {
                if (languagesDone.contains(lang2)) {
                    continue;
                }
                final Map<Integer, Blur> quad2 = languageToLanguageInfo.get(lang2);
                final boolean show = lang.equals("DANISH") && lang2.equals("NORWEGIAN");

                final Set<Integer> common = new HashSet<Integer>();
                common.addAll(quad1.keySet());
                common.retainAll(quad2.keySet());

                final Set<Integer> x1only = new HashSet<Integer>();
                x1only.addAll(quad1.keySet());
                x1only.removeAll(quad2.keySet());

                final Set<Integer> x2only = new HashSet<Integer>();
                x2only.addAll(quad2.keySet());
                x2only.removeAll(quad1.keySet());

                final Blur distance1 = getDistance(x1only, quad1, quad2, show);
                final Blur distanceCommon = getDistance(common, quad1, quad2, show);
                final Blur distance2 = getDistance(x2only, quad1, quad2, show);

                final String message = "\tcommon:\6" + common.size() + "\t;\t" + distanceCommon
                        + "\tunique1:\t" + x1only.size()  + "\t;\t" + distance1
                        + "\tunique2:\t" + x2only.size() + "\t;\t" + distance2;
                //message = ""; // remove for now.

                final Blur distance = distanceCommon.add(distance1).add(distance2).divideBy(2);
                distanceToPair.put(distance, new Row.R3<String,String,String>(lang,lang2,message));
            }
            languagesDone.add(lang);
        }

        for (final Blur distance : distanceToPair.keySet()) {
            for (final Row.R3<String, String, String> value : distanceToPair.getAll(distance)) {
                System.out.println(distance + "\t" + value.get0() + "-" + value.get1() + "\t" + value.get2());
            }
        }
    }

    private static Blur getDistance(Set<Integer> quads, Map<Integer, Blur> quad1,
            Map<Integer, Blur> quad2, boolean show) {
        Blur distance = new Blur(0,0);
        final Blur aZero = quad1.get(0);
        final Blur bZero = quad2.get(0);
        for (final Integer quad : quads) {
            distance = distance.addDelta(quad1.get(quad), quad2.get(quad), aZero, bZero, show);
        }
        return distance;
    }

    static Map<String, Map<Integer, Blur>> languageToLanguageInfo = new TreeMap<String, Map<Integer, Blur>>();

    //static Blur one = Math.pow(2,12);

    private static void add(String string, int quad, byte parseByte) {
        Map<Integer, Blur> x = languageToLanguageInfo.get(string);
        if (x == null) {
            languageToLanguageInfo.put(string, x = new HashMap<Integer, Blur>());
        }
        x.put(quad, new Blur(Math.pow(2,parseByte-0.5), Math.pow(2,parseByte+0.5)));
    }

    public static class Blur implements Comparable<Blur> {
        public static Blur ZERO = new Blur(0,0);
        private final double max;
        private final double min;
        public Blur (double min, double max) {
            this.min = min;
            this.max = max;
        }
        public Blur add(Blur other) {
            return new Blur(min + other.min, max + other.max);
        }
        public Blur divideBy(Blur other) {
            // TODO fix for negatives
            return new Blur(min/other.max, max/other.min);
        }
        public Blur divideBy(double other) {
            // TODO fix for negatives
            return new Blur(min/other, max/other);
        }
        public Blur addDelta(Blur a, Blur b, Blur aZero, Blur bZero, boolean show) {
            if (a == null) {
                a = aZero;
            }
            if (b == null) {
                b = bZero;
            }
            if (show) {
                System.out.println("\t" + a.min + "\t" + a.max + "\t" + b.min + "\t" + b.max);
            }
            double min2 = Math.abs(a.min - b.min), max2 = min2;
            final double m2 = Math.abs(a.min - b.max);
            if (min2 > m2) {
                min2 = m2;
            }
            if (max2 < m2) {
                max2 = m2;
            }
            final double m3 = Math.abs(a.max - b.min);
            if (min2 > m3) {
                min2 = m3;
            }
            if (max2 < m3) {
                max2 = m3;
            }
            final double m4 = Math.abs(a.max - b.max);
            if (min2 > m4) {
                min2 = m4;
            }
            if (max2 < m4) {
                max2 = m4;
            }
            return new Blur(min + min2, max + max2);
        }

        @Override
        public String toString() {
            return min + "\t" + max;
        }
        @Override
        public int compareTo(Blur o) {
            if (min < o.min) {
                return -1;
            }
            if (min > o.min) {
                return 1;
            }
            if (max < o.max) {
                return -1;
            }
            if (max > o.max) {
                return 1;
            }
            return 0;
        }
        protected double getMax() {
            return max;
        }
        protected double getMin() {
            return min;
        }
    }
}
