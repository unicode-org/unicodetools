package org.unicode.tools.emoji;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.props.UnicodeRelation;
import org.unicode.text.utility.Utility;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;

public class CandidateData {

    public enum Quarter {
        _2015Q1, _2015Q2, _2015Q3, _2015Q4,
        _2016Q1;
        static Quarter fromString(String item) {
            return valueOf('_'+item);
        }
        public String toString() {
            return name().substring(1);
        }
    }

    private final List<Integer> order;
    private final UnicodeMap<String> categories = new UnicodeMap<>();
    private final UnicodeMap<String> names = new UnicodeMap<>();
    private final UnicodeRelation<String> annotations = new UnicodeRelation<>();
    private final UnicodeMap<CandidateData.Quarter> quarters = new UnicodeMap<>();

    static final Splitter TAB = Splitter.on('\t');
    static final CandidateData SINGLE = new CandidateData();

    private CandidateData() {
        UnicodeMap<CandidateData.Quarter> quartersForChars = quarters;
        String category = null;
        String source = null;
        Builder<Integer> _order = ImmutableList.builder();
        for (String line : FileUtilities.in(CandidateData.class, "candidateData.txt")) {
            if (line.startsWith("•")) { // annotation
                annotations.add(source,line.substring(1).trim());
            } else if (line.startsWith("U+")) { // data
                List<String> parts = TAB.splitToList(line);
                source = Utility.fromHex(parts.get(0));
                _order.add(source.codePointAt(0));
                final String quarter = parts.get(3).trim();
                quartersForChars.put(source, CandidateData.Quarter.fromString(quarter));
                final String name = parts.get(4).trim();
                names.put(source, name);
                categories.put(source, category);
            } else { // must be category
                category = line.trim();
                line= line.trim();
            }
        }
        order = _order.build();
        categories.freeze();
        names.freeze();
        annotations.freeze();
        quarters.freeze();
    }

    public static CandidateData getInstance() {
        return SINGLE;
    }

    public UnicodeSet keySet() {
        return names.keySet();
    }
    public String getName(String source) {
        return names.get(source);
    }
    public String getCategory(String source) {
        return categories.get(source);
    }
    public Set<String> getAnnotations(String source) {
        return CldrUtility.ifNull(annotations.get(source), Collections.<String>emptySet());
    }
    public CandidateData.Quarter getQuarter(String source) {
        return quarters.get(source);
    }

    public String getName(int source) {
        return names.get(source);
    }
    public String getCategory(int source) {
        return categories.get(source);
    }
    public Set<String> getAnnotations(int source) {
        return CldrUtility.ifNull(annotations.get(source), Collections.<String>emptySet());
    }
    public CandidateData.Quarter getQuarter(int source) {
        return quarters.get(source);
    }

    public List<Integer> getOrder() {
        return order;
    }

    public static void main(String[] args) {
        CandidateData cd = CandidateData.getInstance();
        String oldCat = "";
        UnicodeSet last = new UnicodeSet();
        UnicodeSet total = new UnicodeSet();
        for (int s : cd.getOrder()) {
            String cat = cd.getCategory(s);
            if (!cat.equals(oldCat)) {
                if (!last.isEmpty()) {
                    showLast(last);
                }
                System.out.println("\n" + cat + "\n");
                oldCat = cat;
            }
            last.add(s);
            total.add(s);
            System.out.println(Utility.hex(s) + "\t" + cd.getQuarter(s) + "\t" + cd.getName(s) + "\t" + cd.getAnnotations(s));
        }
        showLast(last);
        showLast(total);
    }

    private static void showLast(UnicodeSet last) {
        System.out.println("# Total: " + last.size());
        System.out.println("# USet: " + CollectionUtilities.join(
                last.addAllTo(new LinkedHashSet<>())," ") + "\n");
        last.clear();
    }
}
