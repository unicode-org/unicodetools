package org.unicode.text.tools;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.props.UnicodeRelation;
import org.unicode.text.utility.Utility;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;

public class CandidateData {
    public enum Quarter {Q1, Q2, Q3, Q4}

    private final List<Integer> order;
    private final UnicodeMap<String> categories = new UnicodeMap<>();
    private final UnicodeMap<String> names = new UnicodeMap<>();
    private final UnicodeRelation<String> annotations = new UnicodeRelation<>();
    private final UnicodeMap<Quarter> quarters = new UnicodeMap<>();

    static final Splitter TAB = Splitter.on('\t');
    static final CandidateData SINGLE = new CandidateData();

    private CandidateData() {
        UnicodeMap<Quarter> quartersForChars = quarters;
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
                quartersForChars.put(source, Quarter.valueOf(quarter));
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
    public Quarter getQuarter(String source) {
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
    public Quarter getQuarter(int source) {
        return quarters.get(source);
    }
    
    public List<Integer> getOrder() {
        return order;
    }
    
    public static void main(String[] args) {
        CandidateData cd = CandidateData.getInstance();
        String oldCat = "";
        for (int s : cd.getOrder()) {
            String cat = cd.getCategory(s);
            if (!cat.equals(oldCat)) {
                System.out.println("\n" + cat + "\n");
                oldCat = cat;
            }
            System.out.println(Utility.hex(s) + "\t" + cd.getQuarter(s) + "\t" + cd.getName(s) + "\t" + cd.getAnnotations(s));
        }
    }
}
