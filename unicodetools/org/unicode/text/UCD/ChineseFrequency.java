package org.unicode.text.UCD;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Pair;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UTF16;

public class ChineseFrequency {
    static NumberFormat percent = new DecimalFormat("0.000000%");
    static NumberFormat percent3 = new DecimalFormat("000.000000%");
    static NumberFormat number = new DecimalFormat("#,##0");

    static class InverseCompareTo implements Comparator {
        @Override
        public int compare(Object o1, Object o2) {
            return -((Comparable)o1).compareTo(o2);
        }
    }

    public static void test() throws IOException{
        final Set freq_char = new TreeSet(new InverseCompareTo());
        final BufferedReader br = BagFormatter.openUTF8Reader(Settings.DICT_DIR, "kHYPLCDPF.txt");
        double grandTotal = 0.0;
        while (true) {
            final String line = br.readLine();
            if (line == null) {
                break;
            }
            final String[] pieces = Utility.split(line,'\t');
            final int cp = Integer.parseInt(pieces[0],16);
            final String[] says = Utility.split(pieces[1],',');
            long total = 0;
            for (final String say : says) {
                final int start = say.indexOf('(');
                final int end = say.indexOf(')');
                final long count = Long.parseLong(say.substring(start+1, end));
                total += count;
            }
            grandTotal += total;
            freq_char.add(new Pair(new Long(total), new Integer(cp)));
        }
        br.close();
        final PrintWriter pw = BagFormatter.openUTF8Writer(Settings.DICT_DIR,"kHYPLCDPF_frequency.txt");
        pw.write("\uFEFF");
        pw.println("No.\tPercentage\tAccummulated\tHex\tChar");

        final Iterator it = freq_char.iterator();
        int counter = 0;
        double cummulative = 0;
        double cummulativePercentage = 0;
        while (it.hasNext()) {
            final Pair item = (Pair)it.next();
            final Long total = (Long) item.first;
            final Integer cp = (Integer) item.second;
            final double current = total.longValue();
            cummulative += current;
            final double percentage = current / grandTotal;
            cummulativePercentage += percentage;
            pw.println(
                    ++counter
                    //+ "\t" + number.format(current)
                    //+ "\t" + number.format(cummulative)
                    + "\t" + percent.format(percentage)
                    + "\t" + percent3.format(cummulativePercentage)
                    + "\t" + Integer.toHexString(cp.intValue()).toUpperCase()
                    + "\t" + UTF16.valueOf(cp.intValue()));
        }
        //pw.println("Grand total: " + (long)grandTotal);
        pw.close();
    }
}