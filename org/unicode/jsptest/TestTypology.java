package org.unicode.jsptest;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.unicode.jsp.Subheader;
import org.unicode.jsp.Typology;
import org.unicode.jsp.UnicodeProperty;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.PrettyPrinter;
import com.ibm.icu.impl.MultiComparator;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

public class TestTypology extends TestFmwk {

    public static void main(String[] args) {
        new TestTypology().run(args);
    }

    MultiComparator col = new MultiComparator(
            Collator.getInstance(), 
            new UTF16.StringComparator(true, false, 0));
    Collator primaryOnly = Collator.getInstance();
    PrettyPrinter pp = new PrettyPrinter().setOrdering(Collator.getInstance()).setSpaceComparator(primaryOnly);

    public void TestSimple() throws IOException {
        Set<String> archaicLabels = new HashSet<String>(Arrays.asList("Archaic Ancient Biblical Historic".split("\\s")));
        UnicodeSet archaic = new UnicodeSet();
        //        System.out.println();
        //        System.out.println("Label\tSet");
        //
        //        for (String label : Typology.getLabels()) {
        //            UnicodeSet uset = Typology.getSet(label);
        //            String labelName = label.length() == 0 ? "<no_label>" : label;
        //            showLabel(pp, uset, labelName);
        //            if (archaicLabels.contains(label)) {
        //                archaic.addAll(uset);
        //            }
        //        }
        //        showLabel(pp, archaic, "(Archaic Ancient Biblical Historic)");
        Set<String> list = new TreeSet<String>();
        list.addAll(Typology.getLabels());
        list.removeAll(Arrays.asList("S L M N C Z P".split(" ")));
        
        System.out.println(list);

        PrintWriter out = BagFormatter.openUTF8Writer(Utility.GEN_DIR + "/categories", "CategoryLabels.txt");
        PrintWriter html = BagFormatter.openUTF8Writer(Utility.GEN_DIR + "/categories", "CategoryLabels.html");
        String fontList = "Georgia, 'Times New Roman', Times, Symbola, Aegyptus, Aegean, Akkadian, Analecta, Musica, Code2000,  Code2001,  Code2002, serif";
        html.println(
                "<!DOCTYPE html PUBLIC '-//W3C//DTD HTML 4.0 Transitional//EN'>\n" +
                "<html><head>\n" +
        		"<base target='_blank'/>\n" +
        		"<meta http-equiv='Content-Type' content='text/html; charset=UTF-8'/> " +
                "<style>\n" +
                "body {font-family: " + fontList + "}\n" +
                "td,th,table { padding:1px; border:1px solid #eeeeFF; vertical-align:top; text-align: left; }\n" +
                "table { border-collapse: collapse; width 100% }\n" +
                "caption {text-align: left; font-size: 150%;}\n"+
                "ul,li {padding: 0px; margin: 0px;}\n" +
                "ul {padding-left: 1.5em;}\n" + 
                ".b {font-size: 133%}\n" + 
                ".p100 {color:green}\n" + 
                ".p66 {color:blue}\n" + 
                ".p33 {color:purple}\n" + 
                ".p0 {color:Maroon}\n" + 
                "</style></head><body>\n" +
                "<p><b>L2/10-449R2</b></p>\n" +
                "<p>Subject: Labels and UTR#49</p>\n" +
                "<p>From: Mark Davis</p>\n" +
                "<p>Date: " + new Date() + "</p>\n" +
                "<p>The following provides a breakdown of the data file for UTR#49. I'll explain more about the" +
                " format during the meeting. Body fonts include: " + fontList + ".</p>"
        );

//        startTable(html, out, "Labels");
//        showLabel(html, out, pp, "Label", null, false);
//
//        for (String label : list) {
//            UnicodeSet uset = Typology.label_to_uset.get(label);
//            showLabel(html, out, pp, label, uset, false);
//        }
//
//        closeTable(html, out);


        startTable(html, out, "Labels, Other Labels in Path, and Subheads");
        showLabel(html, out, pp, "Label", null, true);

        for (String label : list) {
            UnicodeSet uset = Typology.label_to_uset.get(label);
            showLabel(html, out, pp, label, uset, true);
        }

        closeTable(html, out);


        startTable(html, out, "Label in single path");
        showLabel(html, out, pp, "Label\tPath", null, false);

        for (String label : list) {
            Set<String> lists = Typology.labelToPath.getAll(label);
            if (lists.size() == 1) {

                for (String path : lists) {
                    showLabel(html, out, pp, label + "\t" + path, Typology.path_to_uset.get(path), false);
                }
            }
        }

        closeTable(html, out);
        startTable(html, out, "Label in multiple paths");
        showLabel(html, out, pp, "Label\tPath", null, false);

        for (String label : list) {
            Set<String> lists = Typology.labelToPath.getAll(label);
            if (lists.size() > 1) {
                System.out.println();
                for (String path : lists) {
                    showLabel(html, out, pp, label + "\t" + path, Typology.path_to_uset.get(path), false);
                }
            }
        }
        closeTable(html, out);
        
        Set<R3<String, String, UnicodeSet>> props = getProps();
        Set<R3<Double, String, R3<String, String, UnicodeSet>>> matches = new TreeSet<R3<Double, String, R3<String, String, UnicodeSet>>>();
        
        for (String label : list) {
            UnicodeSet uset = Typology.label_to_uset.get(label);
            R3<String, String, UnicodeSet> bestRow = null;
            double closestValue = 0d;
            for (R3<String, String, UnicodeSet> item : props) {
                double closeness = getCloseness(item.get2(), uset);
                if (closeness > closestValue) {
                    closestValue = closeness;
                    bestRow = item;
                }
            }
            if (bestRow == null) {
                System.out.println(label + "\t" + "No Match");
            } else {
                R3<Double, String, R3<String, String, UnicodeSet>> match = Row.of(closestValue, label, bestRow);
                matches.add(match);
            }
        }
        
        for (R3<Double, String, R3<String, String, UnicodeSet>> match : matches) {
            System.out.println(pf.format(match.get0()) + "\t" + match.get1() + "\t" + match.get2().get0() + "=" + match.get2().get1());
        }

        
        html.println("</body></html>");
        html.close();
        out.close();
    }

    private double getCloseness(UnicodeSet get2, UnicodeSet uset) {
        if (!get2.containsSome(uset)) {
            return 0d;
        }
        UnicodeSet intersect = new UnicodeSet(get2).retainAll(uset);
        return (2d * intersect.size())/(get2.size() + uset.size());
    }

    private Set<R3<String, String, UnicodeSet>> getProps() {
        Set<R3<String, String, UnicodeSet>> props = new HashSet<R3<String, String, UnicodeSet>>();
        int[][] ranges = {
                {UProperty.BINARY_START,    UProperty.BINARY_LIMIT},
                {UProperty.INT_START,       UProperty.INT_LIMIT},
                {UProperty.DOUBLE_START,    UProperty.DOUBLE_LIMIT},
                {UProperty.STRING_START,    UProperty.STRING_LIMIT},
        };
        UnicodeSet skip = new UnicodeSet("[[:cn:][:cs:][:co:]]");
        for (int[] range : ranges) {
            for (int propEnum  = range[0]; propEnum < range[1]; ++propEnum) {
                int max = UCharacter.getIntPropertyMaxValue(propEnum);
                for (int valueEnum = UCharacter.getIntPropertyMinValue(propEnum); valueEnum <= max; ++valueEnum) {
                    try {
                        UnicodeSet foo = new UnicodeSet().applyIntPropertyValue(propEnum, valueEnum);
                        foo.removeAll(skip);
                        if (foo.size() == 0) {
                            continue;
                        }
                        String alias = UCharacter.getPropertyName(propEnum, UProperty.NameChoice.LONG);
                        String valueAlias = UCharacter.getPropertyValueName(propEnum, valueEnum, UProperty.NameChoice.LONG);
                        R3<String, String, UnicodeSet> r = Row.of(alias, valueAlias, foo);
                        props.add(r);
                        System.out.println(alias + "=" + valueAlias);
                    } catch (Exception e) {
                        continue; // probably mismatch in ICU version
                    }
                }
            }
        }
        return props;
    }

    private void closeTable(PrintWriter html, PrintWriter out) {
        out.println();
        html.println("</table><p>&nbsp;</p>");
    }

    private void startTable(PrintWriter html, PrintWriter out, String title) {
        out.println(title);
        out.println();
        html.println("<table><caption>" + title + "</caption>");
    }

    static final int LIMIT = 80;
    static final Pattern BREAK_AFTER = Pattern.compile("([_/])");
    final String unicodeDataDirectory = "../jsp/";
    Subheader subheader = new Subheader(Typology.class.getResourceAsStream("NamesList.txt"));


    private void showLabel(PrintWriter html, PrintWriter printStream, PrettyPrinter ppx, String labelName, UnicodeSet uset, boolean subhead) {
        String cell;
        String setString;
        String sizeString = uset == null ? "Size" : uset.size()+"";
        if (uset == null) {
            cell = "th";
            setString = subhead ? "Subheadings" : "Code Points";
            sizeString = "Size";
        } else if (subhead) {
            cell = "td";
            Map<String,Double> subheads = getSubheadInfo(uset);
            setString = formatUnicodeSet(uset) + "<p>" + join(subheads, labelName);
            sizeString = uset.size()+"";
        } else {
            cell = "td";
            setString = formatUnicodeSet(uset);
            sizeString = uset.size()+"";
        }
        labelName = BREAK_AFTER.matcher(labelName).replaceAll("$1\u200B");
        String string = labelName + "\t" + sizeString + "\t" + setString;
        printStream.println(string);
        html.println("<tr><" + cell + ">" 
                + string.replace("\t", "</" + cell + "><" + cell + ">") 
                + "</" + cell + "></tr>");
    }

    private String formatUnicodeSet(UnicodeSet uset) {
        String setString;
        setString = pp.format(uset);
        if (setString.length() > LIMIT) {
            int limit = LIMIT;
            if (UCharacter.isLowSurrogate(setString.charAt(limit))) {
                limit--;
            }
            if (setString.charAt(limit) == '-') {
                limit--;
            }
            setString = setString.substring(0, limit) + "…";
        }
        String uset2 = uset.toPattern(false);
        String href = "<a href='http://unicode.org/cldr/utility/list-unicodeset.jsp?a=" + uset2 + "'>";
        return "<span class='b'>" + href + setString + "</a></span>";
    }


    private Map<String,Double> getSubheadInfo(UnicodeSet uset) {
        Map<String,Double> subheads = new TreeMap<String,Double>();
        for (String s : uset) {
            String subheadString = subheader.getSubheader(s.codePointAt(0));
            double percent;
            if (subheadString == null) {
                subheadString = "?";
                percent = 0d;
            } else {
                UnicodeSet other = subheader.getUnicodeSet(subheadString);
                UnicodeSet overlap = new UnicodeSet(other).retainAll(uset);
                percent = overlap.size()/(double)other.size();
            }
            subheads.put(subheadString, percent);
        }
        return subheads;
    }

    public static String join(Map<String,Double> map, String label) {
        StringBuffer result = new StringBuffer("<b>OL:</b> ");
        Set<String> lists = Typology.labelToPath.getAll(label);
        TreeSet<String> otherLabels = new TreeSet<String>();
        for (String path : lists) {
            String[] nodes = path.split("/");
            for (String node : nodes) {
                otherLabels.add(node);
            }
            otherLabels.remove(label);
        }
        joinItems(result, label, otherLabels);

        result.append("<br><b>SH:</b> ");

        if (map.size() < 3) {
            boolean first = true;
            for (Entry<String, Double> entry : map.entrySet()) {
                String item = entry.getKey();
                Double coverage = entry.getValue();
                if (first) first = false;
                else result.append(", ");
                appendWithCoverage(result, item, coverage);
            }
        } else {
            result.append("<ul><li>");
            String separator = ", ";
            boolean first = true;
            char firstChar = '\u0000';
            for (Entry<String, Double> entry : map.entrySet()) {
                String item = entry.getKey();
                Double coverage = entry.getValue();
                if (first) first = false;
                else {
                    char newFirstChar = Character.toLowerCase(item.charAt(0));
                    if (firstChar != newFirstChar) {
                        result.append("</li><li>");
                        firstChar = newFirstChar;
                    } else {
                        result.append(separator);
                    }
                }
                appendWithCoverage(result, item, coverage);
            }
            result.append("</li></ul>");
        }
        return result.toString();
    }

    private static void joinItems(StringBuffer result, String label, TreeSet<String> otherLabels) {
        UnicodeSet uset = Typology.label_to_uset.get(label);
        if (uset == null) {
            throw new IllegalArgumentException();
        }
        boolean first = true;
        for (String otherLabel : otherLabels) {
            if (first) first = false;
            else result.append(", ");
            UnicodeSet other = Typology.label_to_uset.get(otherLabel);
            if (other == null) {
                throw new IllegalArgumentException();
            }
            UnicodeSet overlap;
            try {
                overlap = new UnicodeSet(other).retainAll(uset);
            } catch (Exception e) {
                throw new IllegalArgumentException();
            }
            appendWithCoverage(result, otherLabel, overlap.size()/(double)other.size());
        }
    }

    private static void appendWithCoverage(StringBuffer result, String item, Double coverage) {
        String pc = coverageToClass(coverage);               
        result.append("<span class='").append(pc).append("'>");
        result.append(item).append(" [").append(pf.format(coverage)).append("]</span>");
    }

    private static String coverageToClass(double coverage) {
        String pc = coverage >= 0.999d ? "p100"
                : coverage > 0.666d ? "p66"
                        : coverage > 0.333 ? "p33"
                                : "p0";
                        return pc;
    }

    static NumberFormat pf = NumberFormat.getPercentInstance();
}
