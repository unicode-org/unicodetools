package org.unicode.propstest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.PropertyNames;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;
import org.unicode.props.UcdPropertyValues.Line_Break_Values;
import org.unicode.props.UcdPropertyValues.Word_Break_Values;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;
import org.unicode.tools.Segmenter;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.locale.XCldrStub.ImmutableMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UnicodeSet;

public class CheckGCB {
    private static final IndexUnicodeProperties IUP = IndexUnicodeProperties.make(Settings.latestVersion);

    private static final Set<PropertyNames.Named> TO_SKIP = ImmutableSet.of(
	    Line_Break_Values.Complex_Context,

	    Word_Break_Values.Regional_Indicator
	    );

    public static void main(String[] args) {
        final Segmenter gcb =
                Segmenter.make(IUP, "GraphemeClusterBreak", Segmenter.Target.FOR_UCD).make();
	final UnicodeMap<UcdPropertyValues.Grapheme_Cluster_Break_Values> gcbProps = IUP.loadEnum(UcdProperty.Grapheme_Cluster_Break);

	final Segmenter wb = Segmenter.make(IUP, "WordBreak", Segmenter.Target.FOR_UCD).make();
	final UnicodeMap<UcdPropertyValues.Word_Break_Values> wbProps = IUP.loadEnum(UcdProperty.Word_Break);
	
	check2("GCB", gcb, gcbProps, "WB", wb, wbProps, 3, false);

	final Segmenter sb = Segmenter.make(IUP, "SentenceBreak", Segmenter.Target.FOR_UCD).make();
	final UnicodeMap<UcdPropertyValues.Sentence_Break_Values> sbProps = IUP.loadEnum(UcdProperty.Sentence_Break);
	
	check2("GCB", gcb, gcbProps, "SB", sb, sbProps, 3, false);
	
	final Segmenter lb = Segmenter.make(IUP, "LineBreak", Segmenter.Target.FOR_UCD).make();
	final UnicodeMap<UcdPropertyValues.Line_Break_Values> lbProps = IUP.loadEnum(UcdProperty.Line_Break);
	
	check2("GCB", gcb, gcbProps, "LB", lb, lbProps, 2, true);
	check2("WB", wb, wbProps, "LB", lb, lbProps, 2, true);
    }

    //    private static void check(String title, 
    //	    Segmenter seg2, UnicodeMap props2) {
    //	System.out.println("\n\t\tGCB\t≠\t" + title);
    //	Samples samples = new Samples(gcbProps, props2, TO_SKIP);
    //	StringBuilder sb = new StringBuilder();
    //	int countErrors = 0;
    //	for (int cp1 : samples) {
    //
    //	    sb.setLength(0);
    //	    sb.appendCodePoint(cp1);
    //	    int testPoint = sb.length();
    //
    //	    for (int cp2 : samples) {
    //
    //		sb.setLength(testPoint); // back up to original point
    //		sb.appendCodePoint(cp2); // add next character
    //
    //		boolean b1 = gcb.breaksAt(sb, testPoint);
    //		boolean b2 = seg2.breaksAt(sb, testPoint);
    //		String xb = "\\p{" + title.toLowerCase(Locale.ROOT) + "=";
    //
    //		if (b1 == false && b2 == true) {
    //		    EPair<Enum, Enum> pair1 = samples.getPair(cp1);
    //		    EPair<Enum, Enum> pair2 = samples.getPair(cp2);
    //		    System.out.println(
    //			    "\\p{gcb=" + pair1.x + "} × \\p{gcb=" + pair2.x + "}"
    //				    + "\t but \t" 
    //				    + xb + pair1.y + "} ÷ " + xb + pair2.y + "}"
    //				    + "\t(" + samples.getSet(pair1).size()
    //				    + " vs " + samples.getSet(pair2).size()
    //				    + ")"
    //				    + "\t\\x{" + Utility.hex(sb, 1) 
    //				    + "}\t«" + sb + "»");
    //		    countErrors++;
    //		}
    //	    }
    //	}
    //	System.out.println("Errors: " + countErrors);
    //    }

    private static void check2(String title1, 
	    Segmenter seg1, UnicodeMap props1, 
	    String title2, Segmenter seg2, 
	    UnicodeMap props2, int sampleLength, boolean skipSpaceBefore) {
	System.out.println("\n\t\t" + title1 + "\t≠\t" + title2);

	Samples samples = new Samples(props1, props2, TO_SKIP);
	Set<String> exclude = new HashSet<>();
	Set<String> strings = buildStrings(samples, sampleLength, new StringBuilder(), new LinkedHashSet<String>(), exclude);
	Set<EPair> pairsFound = new TreeSet<>();
	System.out.println("Samples count:\t" + strings.size() + "\tmax-length:\t" + sampleLength);
	for (String s : strings) {
	    int cp;
	    for (int i = 0; i < s.length(); i += UCharacter.charCount(cp)) {
		cp = s.codePointAt(i); 
		if (i == 0) {
		    continue;
		}
		if (skipSpaceBefore && s.charAt(i-1) == 0x20) {
		    continue;
		}
		boolean b1 = seg1.breaksAt(s, i);
		boolean b2 = seg2.breaksAt(s, i);
		if (b1 == false && b2 == true) {
		    System.out.println(samples.show(s, i, " × ", " ÷ ", pairsFound));
		    exclude.add(s);
		}
	    }
	}
	if (!pairsFound.isEmpty()) {
	    System.out.println("\n* Pairs found in problem strings");
	    System.out.println("Pair {"
	    	+ title1.toLowerCase(Locale.ROOT)
	    	+ ", " + title2.toLowerCase(Locale.ROOT) 
	    	+ "}\tCP Count\tSample: Hex\tString\tName");
	    for (EPair pair : pairsFound) {
		UnicodeSet us = samples.getSet(pair);
		String sample = us.iterator().next();
		System.out.println(pair + "\t" + us.size() 
		+ "\t" + hexStringName(sample));
	    }
	} else {
	    System.out.println("NO PROBLEMS FOUND!");
	}
    }

    private static String hexStringName(String sample) {
	return "\\x{" + Utility.hex(sample, 1) + "}" 
		+ "\t" + "«" + sample + "»"
		+ "\t" + IUP.getName(sample, " + ");
    }

    private static Set<String> buildStrings(Samples samples, int depth, StringBuilder sb, Set<String> output, Set<String> exclude) {
	int oldLength = sb.length();
	for (int cp1 : samples) {
	    sb.appendCodePoint(cp1);
	    String candidate = sb.toString();
	    if (hasSubstring(candidate, exclude)) {
		continue;
	    }
	    output.add(candidate);

	    if (depth > 1) {
		buildStrings(samples, depth-1, sb, output, exclude);
	    }
	    sb.setLength(oldLength); // backup
	}
	return output;
    }

    private static boolean hasSubstring(String candidate, Set<String> exclude) {
	for (String s : exclude) {
	    if (candidate.contains(s)) {
		return true;
	    }
	}
	return false;
    }

    static class Samples implements Iterable<Integer> {
	final Map<EPair<Enum, Enum>, UnicodeSet>  pairToUnicodeSet;
	final Map<EPair<Enum, Enum>, Integer> pairToCodePoint;
	final UnicodeMap<EPair<Enum, Enum>> codepointToPair = new UnicodeMap<>();

	public Samples(UnicodeMap props1, UnicodeMap props2, Set<PropertyNames.Named> toSkip) {
	    pairToUnicodeSet = combine(props1, props2, toSkip);

	    Map<EPair<Enum, Enum>, Integer> _pairToCodePoint = new TreeMap<>();
	    for (Entry<EPair<Enum, Enum>, UnicodeSet> entry : pairToUnicodeSet.entrySet()) {
		UnicodeSet us = entry.getValue();
		EPair<Enum, Enum> pair = entry.getKey();
		codepointToPair.putAll(us, pair);
		int cp = us.getRangeStart(0);
		_pairToCodePoint.put(pair, cp);
	    }
	    pairToCodePoint = ImmutableBiMap.copyOf(_pairToCodePoint);
	}

	public StringBuilder show(String s, int offset, String sep1, String sep2, Set<EPair> pairsFound) {
	    StringBuilder sb = new StringBuilder();
	    List<Integer> sizes = new ArrayList();
	    int cp;
	    for (int i = 0; i < s.length(); i += UCharacter.charCount(cp)) {
		cp = s.codePointAt(i);
		if (i == offset) {
		    sb.append(sep1);
		} else if (i != 0) {
		    sb.append(" • "); 
		}
		EPair<Enum, Enum> pair = getPair(cp);
		sizes.add(pairToUnicodeSet.get(pair).size());
		pairsFound.add(pair);
		sb.append(pair.x);
	    }
	    sb.append("\t≠\t");
	    for (int i = 0; i < s.length(); i += UCharacter.charCount(cp)) {
		cp = s.codePointAt(i);
		if (i == offset) {
		    sb.append(sep2);
		} else if (i != 0) {
		    sb.append(" • "); 
		}
		sb.append(getPair(cp).y);
	    }
	    sb.append("\t" + "\\x{" + Utility.hex(s, 1) + "}"
		    + "\t" + sizes
		    + "\t" + "«" + s + "»");
	    return sb;
	}

	public UnicodeSet getSet(EPair<Enum, Enum> pair) {
	    return pairToUnicodeSet.get(pair);
	}

	private class SampleIterator implements Iterator<Integer> {
	    Iterator<Integer> iterator = pairToCodePoint.values().iterator();

	    @Override
	    public boolean hasNext() {
		return iterator.hasNext();
	    }

	    @Override
	    public Integer next() {
		return iterator.next();
	    } 
	}

	@Override
	public Iterator<Integer> iterator() {
	    // TODO Auto-generated method stub
	    return new SampleIterator();
	}

	public EPair<Enum, Enum> getPair(int cp) {
	    return codepointToPair.get(cp);
	}
    }

    public static Map<EPair<Enum, Enum>, UnicodeSet> combine(
            UnicodeMap<Enum> props1, UnicodeMap<Enum> props2, Set<PropertyNames.Named> toSkip) {
	Map<EPair<Enum, Enum>, UnicodeSet> result = new TreeMap<>();
	for (Enum v1 : props1.values()) {
	    if (TO_SKIP.contains(v1)) {
		continue;
	    }
	    UnicodeSet us1 = props1.getSet(v1);
	    for (Enum v2 : props2.values()) {
		if (TO_SKIP.contains(v2)) {
		    continue;
		}
		UnicodeSet us2 = props2.getSet(v2);
		UnicodeSet joint = new UnicodeSet(us1).retainAll(us2);
		if (joint.isEmpty()) {
		    continue;
		}
		result.put(new EPair(v1,v2), joint.freeze());
	    }
	}
	return ImmutableMap.copyOf(result);
    }

    static class EPair<T extends Enum, U extends Enum> implements Comparable<EPair<T,U>> {
	final T x;
	final U y;
	public EPair(T x, U y) {
	    this.x = x;
	    this.y = y;
	}
	@Override
	public int compareTo(EPair o) {
	    int diff = x.ordinal() - o.x.ordinal();
	    if (diff != 0) {
		return diff;
	    }
	    return y.ordinal() - o.y.ordinal();
	}
	@Override
	public boolean equals(Object obj) {
	    EPair<T,U> that = (EPair<T,U>)obj;
	    return x == that.x && y == that.y;
	}
	@Override
	public int hashCode() {
	    return x.hashCode()*7919 + y.hashCode();
	}
	@Override
	public String toString() {
	    return "{" + x + ", " + y + "}";
	}
    }

}
