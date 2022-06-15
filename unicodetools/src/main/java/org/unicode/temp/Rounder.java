package org.unicode.temp;

import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.ULocale;

/**
 * Simple program to try out different rounding schemes to preserve significant digits for unit
 * conversion. No attempt has been made to make the code efficient.
 *
 * @author markdavis
 */
public class Rounder {
    private static final double cmPerInch = 2.54d;
    private static final double litersPerGallon = 0.946352946d * 4;
    private static final double kmPerMile = cmPerInch * 12 * 5280 / 100000d;
    private static final double lpkmToMpg = litersPerGallon / kmPerMile;

    public static class BoundedDouble extends Number {
        private double low;
        private double high;

        @Override
        public int intValue() {
            return (int) ((low + high) / 2);
        }

        @Override
        public long longValue() {
            return (long) ((low + high) / 2);
        }

        @Override
        public float floatValue() {
            return (float) ((low + high) / 2);
        }

        @Override
        public double doubleValue() {
            return (low + high) / 2;
        }
    }

    public static void main(String[] args) {
        tryRounderlpkm();
        tryRounder();
        tryRounder60();
    }

    private double finalLow;
    private double finalHigh;
    private double finalMedian;

    private String getFinalDigits() {
        return finalLow + " / " + finalHigh + " / " + finalMedian;
    }

    public StringBuilder round(double lowPositive, double highPositive) {
        lowPositive = Math.nextUp(lowPositive);
        highPositive = Math.nextDown(highPositive);
        StringBuilder digits = new StringBuilder();
        int count = 1;
        while (lowPositive >= 10d || highPositive >= 10d) {
            lowPositive /= 10d;
            highPositive /= 10d;
            ++count;
        }
        while (true) {
            if (count == 0) {
                digits.append('.');
            }
            --count;
            double lowDigit = Math.floor(lowPositive);
            double highDigit = Math.floor(highPositive);
            if (lowDigit != highDigit) {
                // for the last digit, we take our best shot
                // if we don't do this last digit, we don't have enough digits
                // to get within the bounds of low/high
                double medianDouble = (lowPositive + highPositive) / 2;
                long median = Math.round(medianDouble);
                finalLow = lowDigit;
                finalHigh = highDigit;
                finalMedian = medianDouble;
                digits.append((char) ('0' + (int) median));
                break;
            }
            digits.append((char) ('0' + (int) lowDigit));
            lowPositive -= lowDigit;
            lowPositive *= 10d;
            highPositive -= highDigit;
            highPositive *= 10d;
        }
        for (int i = 0; i < count; ++i) {
            digits.append('0');
        }
        return digits;
    }

    public StringBuilder round60(double lowPositive, double highPositive) {
        lowPositive = Math.nextUp(lowPositive);
        highPositive = Math.nextDown(highPositive);
        Position position = Position.DEGREES;
        StringBuilder digits = new StringBuilder();
        int count = 1;
        while (lowPositive >= 10d || highPositive >= 10d) {
            lowPositive /= 10d;
            highPositive /= 10d;
            ++count;
        }
        while (true) {
            if (count == 0) {
                boolean isSeconds = position == Position.SECONDS;
                digits.append(isSeconds ? "." : position.symbol);
                if (!isSeconds) {
                    position = Position.values()[position.ordinal() + 1];
                    lowPositive *= 60;
                    highPositive *= 60;
                    while (lowPositive >= 10d || highPositive >= 10d) {
                        lowPositive /= 10d;
                        highPositive /= 10d;
                        ++count;
                    }
                }
            }
            --count;
            double lowDigit = Math.floor(lowPositive);
            double highDigit = Math.floor(highPositive);
            if (lowDigit != highDigit) {
                // for the last digit, we take our best shot
                // if we don't do this last digit, we don't have enough digits
                // to get within the bounds of low/high
                double medianDouble = (lowPositive + highPositive) / 2;
                long median = Math.round(medianDouble);
                finalLow = lowPositive;
                finalHigh = highPositive;
                finalMedian = medianDouble;
                digits.append((char) ('0' + (int) median));
                break;
            }
            digits.append((char) ('0' + (int) lowDigit));
            lowPositive -= lowDigit;
            lowPositive *= 10d;
            highPositive -= highDigit;
            highPositive *= 10d;
        }
        for (int i = 0; i < count; ++i) {
            digits.append('0');
        }
        digits.append(position.symbol);
        return digits;
    }

    public enum Position {
        DEGREES("°", 1),
        MINUTES("′", 60),
        SECONDS("″", 60 * 60);
        public final String symbol;
        public final double divisor;

        private Position(String symbol, int divisor) {
            this.symbol = symbol;
            this.divisor = divisor;
        }

        public static Position fromSymbol(char ch) {
            for (Position p : Position.values()) {
                if (p.symbol.charAt(0) == ch) {
                    return p;
                }
            }
            return null;
        }
    }

    static double[] parseLowHigh(CharSequence ss) {
        String s = ss.toString();
        double base = Double.parseDouble(s);
        double low;
        double high;
        int pos = s.indexOf('.');
        if (pos >= 0) {
            double offset = Math.pow(10d, -(double) (s.length() - pos - 1));
            low = base - offset / 2;
            high = base + offset / 2;
        } else {
            low = base - 0.5;
            high = base + 0.5;
        }
        double[] result = {low, high};
        return result;
    }

    static double[] parseLowHigh60(CharSequence s) {
        double value = 0;
        int start = 0;
        int lastIndex = s.length() - 1;
        for (int i = 0; i <= lastIndex; ++i) {
            char ch = s.charAt(i);
            if ('0' <= ch && ch <= '9' || ch == '.') {
                continue;
            }
            Position position = Position.fromSymbol(ch);
            CharSequence toParse = s.subSequence(start, i);
            start = i + 1;
            if (i != lastIndex) { // not final field
                int item = Integer.parseInt(toParse.toString());
                value += item / position.divisor;
            } else {
                double[] result = parseLowHigh(toParse);
                result[0] = result[0] / position.divisor + value;
                result[1] = result[1] / position.divisor + value;
                return result;
            }
        }
        throw new IllegalArgumentException("Bad degree format for " + s);
    }

    static final DecimalFormat nf = (DecimalFormat) NumberFormat.getInstance(ULocale.ENGLISH);
    static final DecimalFormat nfPlusMinus =
            (DecimalFormat) NumberFormat.getInstance(ULocale.ENGLISH);

    static {
        nf.setMinimumSignificantDigits(1);
        nf.setMaximumSignificantDigits(9);
        nfPlusMinus.setMinimumSignificantDigits(1);
        nfPlusMinus.setMaximumSignificantDigits(3);
    }

    private static void tryRounder60() {
        System.out.println(
                "\nRounding when convering double degrees to degrees-minutes-seconds\nSource\tdegrees interpreted\tlast digit internals\t rounded & converted to dms\tdms interpreted\t rounded & converted back\tSame?");
        String[] tests = {
            "12",
            "12.3",
            "12.30",
            "12.34",
            "12.340",
            "12.345",
            "12.3456",
            "12.34567",
            "12.3456789",
            "12.34567890"
        };
        Rounder rounder = new Rounder();
        for (String test : tests) {
            double[] start = parseLowHigh(test);

            double low = start[0];
            double high = start[1];
            StringBuilder rounded = rounder.round60(low, high);
            String finalDigits = rounder.getFinalDigits();
            double[] back = rounder.parseLowHigh60(rounded);
            StringBuilder rounded3 = rounder.round(back[0], back[1]);

            System.out.println(
                    '“'
                            + test
                            + "°"
                            + '”'
                            + "\t"
                            + showPlusMinus(start)
                            + "°"
                            + "\t"
                            + finalDigits
                            + "\t“"
                            + rounded
                            + "”"
                            + "\t"
                            + showPlusMinus(back)
                            + "°"
                            + "\t“"
                            + rounded3
                            + "°”"
                            + (test.equals(rounded3.toString()) ? "" : "\tNO"));
        }
    }

    private static void tryRounder() {
        System.out.println(
                "Rounding when convering centimeter values to inches\nSource\tcm interpreted\tmapped to inches\t rounded & converted \tinches interpreted\tmapped to cm\t rounded & converted \tSame?\t");
        String[] tests = {
            "0.49", "0.5", "0.50", "0.51", "0.510", "0.52", "4.8", "4.9", "5", "5.0", "5.1", "5.10",
            "5.2", "49", "50", "51", "51.0", "52", "0.98", "0.99", "0.100"
        };
        Rounder rounder = new Rounder();
        for (String test : tests) {
            double[] start = parseLowHigh(test);

            double low = start[0];
            double high = start[1];
            // StringBuilder rounded = rounder.round(low, high);
            double[] start2 = {low / cmPerInch, high / cmPerInch};
            StringBuilder rounded2 = rounder.round(start2[0], start2[1]);
            double[] back = parseLowHigh(rounded2);
            double[] back2 = {back[0] * cmPerInch, back[1] * cmPerInch};
            StringBuilder rounded3 = rounder.round(back2[0], back2[1]);
            //            System.out.println('“' + test + '”'
            //                    + "\t" + showPlusMinus(start)
            //                    //+ "\t" + rounded + " cm"
            //                    + "\t" + showPlusMinus(start2) + " in"
            //                    + "\t" + rounded2 + " in"
            //                    + "\t" + showPlusMinus(back) + " in"
            //                    + "\t" + showPlusMinus(back2) + " cm"
            //                    + "\t" + rounded3 + " cm"
            //                    + (test.equals(rounded3.toString()) ? "" : "\tNO")
            //                    );
            showConversion("cm", "in", test, start, start2, rounded2, back, back2, rounded3);
        }
    }

    private static void tryRounderlpkm() {
        System.out.println(
                "Rounding when convering liters/km values to mpg\nSource\tlpkm interpreted\tmapped to mpg\t rounded & converted \tmpg interpreted\tmapped to lpkm\t rounded & converted \tSame?\t");
        String[] tests = {
            "0.49", "0.5", "0.50", "0.51", "0.510", "0.52",
            "4.9", "5", "5.0", "5.1", "5.10", "5.2",
            "49", "50", "51", "51.0", "52"
        };
        Rounder rounder = new Rounder();
        for (String test : tests) {
            double[] start = parseLowHigh(test);

            double low = start[0];
            double high = start[1];
            // 235 / (L/100 km) = mpgUS

            // StringBuilder rounded = rounder.round(low, high);
            double[] start2 = {lpkmToMpg / low, lpkmToMpg / high};
            StringBuilder rounded2 = rounder.round(start2[0], start2[1]);
            double[] back = parseLowHigh(rounded2);
            double[] back2 = {lpkmToMpg / back[0], lpkmToMpg / back[1]};
            StringBuilder rounded3 = rounder.round(back2[0], back2[1]);
            showConversion("lpkm", "mpg", test, start, start2, rounded2, back, back2, rounded3);
        }
    }

    private static void showConversion(
            String sourceUnit,
            String targetUnit,
            String test,
            double[] start,
            double[] start2,
            StringBuilder rounded2,
            double[] back,
            double[] back2,
            StringBuilder rounded3) {
        System.out.println(
                '“'
                        + test
                        + " "
                        + sourceUnit
                        + '”'
                        + "\t"
                        + showPlusMinus(start)
                        + "\t"
                        + showPlusMinus(start2)
                        + " "
                        + targetUnit
                        + "\t“"
                        + rounded2
                        + " "
                        + targetUnit
                        + "”"
                        + "\t"
                        + showPlusMinus(back)
                        + " "
                        + targetUnit
                        + "\t"
                        + showPlusMinus(back2)
                        + " "
                        + sourceUnit
                        + "\t“"
                        + rounded3
                        + " "
                        + sourceUnit
                        + "”"
                        + (test.equals(rounded3.toString()) ? "" : "\tNO"));
    }

    private static String showPlusMinus(double[] start) {
        double average = (start[0] + start[1]) / 2;
        return nf.format(average) + " ± " + nfPlusMinus.format(Math.abs(start[1] - average));
    }
}
