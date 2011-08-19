package org.unicode.jsptest;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.unicode.jsp.Subheader;
import org.unicode.jsp.Typology;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.PrettyPrinter;
import com.ibm.icu.impl.MultiComparator;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

public class TestTypology extends TestFmwk {

    private static final boolean DEBUG = false;
    private static final String VERSION = "5";
    private static final boolean SHOW_RELATED = false;

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

        if (DEBUG) System.out.println(list);

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
                ".sub {color: gray}\n" + 
                "</style></head><body>\n" +
                "<p><b>L2/10-450R" + VERSION
                 +
                "</b></p>\n" +
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
        showLabel(html, out, pp, "Label\tSize\tContents/Other-Labels/Subheads", LabelStyle.title, LabelRowStyle.normal);

        for (String label : list) {
            UnicodeSet uset = Typology.label_to_uset.get(label);
            UnicodeSet[] usets = { uset };
            showLabel(html, out, pp, label, LabelStyle.subhead, LabelRowStyle.normal, usets);
        }

        closeTable(html, out);


        //        startTable(html, out, "Label in single path");
        //        showLabel(html, out, pp, "Label\tPath\tSize\tContents", LabelStyle.title);
        //
        //        for (String label : list) {
        //            Set<String> lists = Typology.labelToParents.getAll(label);
        //            if (lists.size() == 1) {
        //                for (String path : lists) {
        //                    showLabel(html, out, pp, (label + "\t" + path), LabelStyle.normal);
        //                }
        //            }
        //        }
        //
        //        closeTable(html, out);

        startTable(html, out, "Label with multiple parents");
        showLabel(html, out, pp, "Label\tPath", LabelStyle.title, LabelRowStyle.normal);

        for (String label : list) {
            Map<String, UnicodeSet> lists = Typology.label_parent_uset.get(label);
            if (lists.size() < 2) {
                continue;
            }
            String oldLabel = "";
            for (Entry<String, UnicodeSet> parentAndSet : lists.entrySet()) {
                String parent = parentAndSet.getKey();
                UnicodeSet uset = parentAndSet.getValue();
                showLabel(html, out, pp, (label + "\t" + parent), LabelStyle.normal, 
                        !label.equals(oldLabel) ? LabelRowStyle.normal : LabelRowStyle.sub, 
                                uset);
                oldLabel = label;
            }
        }
        closeTable(html, out);

        Set<PropData> props = getProps();
        Set<R3<Double, String, TreeSet<Row.R2<Double, PropData>>>> matches = new TreeSet<R3<Double, String, TreeSet<Row.R2<Double, PropData>>>>();

        for (String label : list) {
            UnicodeSet uset = Typology.label_to_uset.get(label);
            TreeSet<Row.R2<Double, PropData>> close = new TreeSet<R2<Double, PropData>>();
            for (PropData item : props) {
                double closeness = getCloseness(item.getSet(), uset);
                if (closeness == 0d) {
                    continue;
                }
                if (close.size() > 4) {
                    R2<Double, PropData> lowest = close.iterator().next();
                    if (closeness <= lowest.get0()) {
                        continue;
                    }
                }
                close.add(Row.of(closeness, item));
                if (close.size() > 5) {
                    close.remove(close.iterator().next()); // remove first
                }
            }
            if (close.size() > 6) {
                throw new IllegalArgumentException();
            }
            double closestValue = 0;
            for (R2<Double, PropData> row : close) {
                closestValue = row.get0();
            }

            R3<Double, String, TreeSet<Row.R2<Double, PropData>>> match = Row.of(closestValue, label, close);
            matches.add(match);
        }

        startTable(html, out, "Labels compared to Properties");
        showLabel(html, out, pp, "Overlap\tLabel\tProp/Subhead\tCount Shared\tShared\tLabel-Prop\tProp-Label", LabelStyle.title, LabelRowStyle.normal);

        for (R3<Double, String, TreeSet<Row.R2<Double, PropData>>> match : matches) {
            String label = match.get1();
            UnicodeSet uset = Typology.label_to_uset.get(label);
            boolean first = true;
            for (R2<Double, PropData> matchData : InverseIterator.of(match.get2())) {
                double closeness = matchData.get0();
                if (closeness < 0.75 && !first) {
                    break;
                }
                PropData propData = matchData.get1();
                UnicodeSet propSet = propData.getSet();
                UnicodeSet label_AND_propset = new UnicodeSet(uset).retainAll(propSet);
                UnicodeSet label_propset = new UnicodeSet(uset).removeAll(propSet);
                UnicodeSet propset_label = new UnicodeSet(propSet).removeAll(uset);
                //showLabel(html, out, pp, label + "\t" + path, Typology.path_to_uset.get(path), false);
                showLabel(html, out, pp, pf.format(closeness) 
                        + "\t" + label 
                        + "\t" + propData.getName() + "=" + propData.getValue(),
                        LabelStyle.normal,
                        first ? LabelRowStyle.normal : LabelRowStyle.sub,
                                label_AND_propset,
                                label_propset, propset_label
                );
                first = false;
            }
        }

        closeTable(html, out);

        html.println("</body></html>");
        html.close();
        out.close();
    }

    static class InverseIterator<T> implements Iterator<T>, Iterable<T> {
        private ArrayList<T> items;
        private int position;
        private InverseIterator(ArrayList<T> items) {
            this.items = items;
            this.position = items.size();
        }

        public static <T> InverseIterator<T> of(Iterable<T> source) {
            return InverseIterator.of(source.iterator());            
        }

        public static <T> InverseIterator<T> of(Iterator<T> source) {
            ArrayList<T> items = new ArrayList<T>();
            while (source.hasNext()) {
                items.add(source.next());
            }
            return new InverseIterator<T>(items);
        }

        @Override
        public boolean hasNext() {
            return position > 0;
        }

        @Override
        public T next() {
            return items.get(--position);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Iterator<T> iterator() {
            return this;
        }
    }

    static class PropData extends R3<UnicodeSet, String, String> {
        public PropData(UnicodeSet values, String name, String value) {
            super(values, name, value);
        }

        public UnicodeSet getSet() {
            return get0();
        }
        public String getName() {
            return get1();
        }
        public String getValue() {
            return get2();
        }
    }

    private double getCloseness(UnicodeSet get2, UnicodeSet uset) {
        if (!get2.containsSome(uset)) {
            return 0d;
        }
        UnicodeSet intersect = new UnicodeSet(get2).retainAll(uset);
        return (2d * intersect.size())/(get2.size() + uset.size());
    }

    static final Set<String> SKIP_PROPS = new HashSet<String>(Arrays.asList("Trail_Canonical_Combining_Class Lead_Canonical_Combining_Class".split("\\s")));

    private Set<PropData> getProps() {
        Set<PropData> props = new HashSet<PropData>();
        int[][] ranges = {
                {UProperty.BINARY_START,    UProperty.BINARY_LIMIT},
                {UProperty.INT_START,       UProperty.INT_LIMIT},
                {UProperty.DOUBLE_START,    UProperty.DOUBLE_LIMIT},
                {UProperty.STRING_START,    UProperty.STRING_LIMIT},
        };
        UnicodeSet skip = new UnicodeSet("[[:cn:][:cs:][:co:]]");
        for (int[] range : ranges) {
            for (int propEnum  = range[0]; propEnum < range[1]; ++propEnum) {
                String alias = UCharacter.getPropertyName(propEnum, UProperty.NameChoice.LONG);
                if (SKIP_PROPS.contains(alias)) {
                    continue;
                }
                int max = UCharacter.getIntPropertyMaxValue(propEnum);
                for (int valueEnum = UCharacter.getIntPropertyMinValue(propEnum); valueEnum <= max; ++valueEnum) {
                    try {
                        UnicodeSet foo = new UnicodeSet().applyIntPropertyValue(propEnum, valueEnum);
                        String valueAlias = UCharacter.getPropertyValueName(propEnum, valueEnum, UProperty.NameChoice.LONG);

                        //                        foo.removeAll(skip);
                        //                        if (foo.size() == 0) {
                        //                            continue;
                        //                        }
                        //                        PropData r = Row.of(alias, valueAlias, foo);
                        //                        props.add(r);
                        //                        System.out.println(alias + "=" + valueAlias);
                        addProps(props, skip, valueAlias, foo, alias);

                    } catch (Exception e) {
                        continue; // probably mismatch in ICU version
                    }
                }
            }
        }
        for (String subhead : subheader) {
            UnicodeSet uset = subheader.getUnicodeSet(subhead);
            String alias = "subhead";

            addProps(props, skip, subhead, uset, alias);
        }
        return props;
    }

    private void addProps(Set<PropData> props, UnicodeSet skip, String subhead, UnicodeSet uset, String alias) {
        UnicodeSet foo = new UnicodeSet(uset);
        foo.removeAll(skip);
        if (foo.size() != 0) {
            PropData r = new PropData(foo, alias, subhead);
            props.add(r);
            if (DEBUG) System.out.println(alias + "=" + subhead);
        }
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

    enum LabelStyle {title, normal, subhead}
    enum LabelRowStyle {normal, sub}

    private void showLabel(PrintWriter html, PrintWriter printStream, PrettyPrinter ppx, 
            String labelName, LabelStyle subhead, LabelRowStyle labelRowStyle, 
            UnicodeSet... usets) {
        String cell;
        String setString;
        String sizeString;
        String printLabel = labelName;
        if (printLabel.isEmpty()) {
            printLabel = "\"\"";
        }
        if (subhead == LabelStyle.title) {
            cell = "th";
        } else if (subhead == LabelStyle.subhead) {
            cell = "td";
            Map<String,Double> subheads = getSubheadInfo(usets[0]);
            setString = formatUnicodeSet(usets) 
            + "<p>" + Typology.label_parent_uset.get(labelName).keySet() +
            (SHOW_RELATED ? "<p>" + join(subheads, labelName) : "");

            sizeString = usets.length == 0 ? "" : usets[0].size()+"";
            labelName = BREAK_AFTER.matcher(labelName).replaceAll("$1\u200B");
            labelName += "\t" + sizeString + "\t" + setString;
            printLabel += "\t" + sizeString + "\t" + show(usets); 
        } else {
            cell = "td";
            setString = formatUnicodeSet(usets);
            sizeString = usets.length == 0 ? "" : usets[0].size()+"";
            labelName = BREAK_AFTER.matcher(labelName).replaceAll("$1\u200B");
            labelName += "\t" + sizeString + "\t" + setString;
            printLabel += "\t" + sizeString + "\t" + show(usets); 
        }
        printStream.println(printLabel);
        html.println("<tr" + (labelRowStyle == LabelRowStyle.normal ? "" : " class='sub'") +
                "><" + cell + ">" 
                + labelName.replace("\t", "</" + cell + "><" + cell + ">") 
                + "</" + cell + "></tr>");
    }

    private String show(UnicodeSet[] usets) {
        String results = "";
        for (UnicodeSet u : usets) {
            results += u.toPattern(false);
        }
        return results;
    }

    private String formatUnicodeSet(UnicodeSet... usets) {
        String result = "";
        for (UnicodeSet uset : usets) {
            if (result.length() != 0) {
                result += "\t";
            }
            result += formatUnicodeSet(uset);
        }
        return result;
    }

    private String formatUnicodeSet(UnicodeSet uset) {
        if (uset.size() ==0) return "∅";
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
        Set<String> lists = Typology.labelToPaths.getAll(label);
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
