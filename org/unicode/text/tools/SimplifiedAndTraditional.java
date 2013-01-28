package org.unicode.text.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.text.UCD.Default;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.PrettyPrinter;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.XEquivalenceClass;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.ULocale;

public class SimplifiedAndTraditional {
	public static void main(String[] args) {
		new SimplifiedAndTraditional().showSimpVsTrad(args);
	}

	private void showSupplementalIntersection(String[] args) {
		final UnicodeSet x = showKeyset("kHKSCS", new UnicodeSet("[\\U00010000-\\U0010FFFF]"));
		final UnicodeSet y = showKeyset("kJIS0213", new UnicodeSet("[\\U00010000-\\U0010FFFF]"));
		System.out.println("intersection" + ":\t" + PRETTY.format(x.retainAll(y)));
	}

	private UnicodeSet showKeyset(String propertyName, UnicodeSet filter) {
		final UnicodeMap mand = Default.ucd().getHanValue(propertyName);
		final UnicodeSet supplementals = new UnicodeSet(filter).retainAll(mand.keySet());
		System.out.println(propertyName + ":\t" + PRETTY.format(supplementals));
		return supplementals;
	}

	private void run3(String[] args) {
		final Map<String,UnicodeSet> mandarin = new TreeMap();
		final UnicodeMap mand = Default.ucd().getHanValue("kMandarin");
		for (final String value : (Collection<String>) mand.getAvailableValues()) {
			final UnicodeSet sources = mand.keySet(value);
			final String[] pieces = value.split("\\s");
			for (String piece : pieces) {
				piece = piece.trim();
				UnicodeSet set = mandarin.get(piece);
				if (set == null) {
					mandarin.put(piece, set = new UnicodeSet());
				}
				set.addAll(sources);
			}
		}
		UnicodeSet biggest = new UnicodeSet();
		for (final String value : mandarin.keySet()) {
			final UnicodeSet set = mandarin.get(value);
			if (set.size() > biggest.size()) {
				biggest = set;
			}
		}
		System.out.println(PRETTY.format(biggest));
	}

	static     PrettyPrinter PRETTY = new PrettyPrinter().setOrdering(Collator.getInstance(ULocale.ROOT)).setSpaceComparator(Collator.getInstance(ULocale.ROOT).setStrength2(Collator.PRIMARY));

	private void showSimpVsTrad(String[] args) {


		final UnicodeMap simp2trad = Default.ucd().getHanValue("kTraditionalVariant");
		final UnicodeMap trad2simp = Default.ucd().getHanValue("kSimplifiedVariant");

		final UnicodeSet simpOnly = simp2trad.keySet();
		final UnicodeSet tradOnly = trad2simp.keySet();
		final UnicodeSet overlap = new UnicodeSet(simpOnly).retainAll(tradOnly);
		simpOnly.removeAll(overlap);
		tradOnly.removeAll(overlap);
		System.out.println("UnicodeSet simpOnly = new UnicodeSet(\"" + simpOnly.toPattern(false) + "\");");
		System.out.println("UnicodeSet tradOnly = new UnicodeSet(\"" + tradOnly.toPattern(false) + "\");");

		final XEquivalenceClass equivalences = new XEquivalenceClass("?");

		System.out.println("*** Data Problems ***");
		System.out.println();

		for (final UnicodeSetIterator it = new UnicodeSetIterator(simp2trad.keySet()); it.next();) {
			final String source = it.getString();
			final String targetOptions = getVariant(simp2trad, it.codepoint);
			int cp;
			for (int i = 0; i < targetOptions.length(); i += UTF16.getCharCount(cp)) {
				String target = new StringBuilder().appendCodePoint(cp = UTF16.charAt(targetOptions,i)).toString();
				if (source.equals(target)) {
					target = target + "*";
				}
				equivalences.add(source, target, "→T", "T←");
			}
		}
		for (final UnicodeSetIterator it = new UnicodeSetIterator(trad2simp.keySet()); it.next();) {
			final String source = it.getString();
			final String targetOptions = getVariant(trad2simp, it.codepoint);
			int cp;
			for (int i = 0; i < targetOptions.length(); i += UTF16.getCharCount(cp)) {
				final String target = new StringBuilder().appendCodePoint(cp = UTF16.charAt(targetOptions,i)).toString();
				String source2 = source;
				if (source.equals(target)) {
					source2 = source2 + "*";
				}
				equivalences.add(source2, target, "→S", "S←");
			}
			//equivalences.add(it.getString(), getVariant(trad2simp, it.codepoint), "→S", "S←");
		}

		System.out.println("*** Simple Pairs ***");
		System.out.println();
		int count = 0;
		final Set<Set<String>> equivalenceSets = equivalences.getEquivalenceSets();

		final Set<Set<String>> seenEquivalences = new HashSet();
		for (final Set<String> equivSet : equivalenceSets) {
			if (equivSet.size() != 2) {
				continue;
			}
			final ArrayList<String> list = new ArrayList(equivSet);
			final String reasonString = equivalences.getReasons(list.get(0), list.get(1)).toString();
			// S↔T
			if (reasonString.equals("[[[S←, →T]]]")) {
				System.out.println(list.get(0) + "\tS↔T\t" + list.get(1));
				seenEquivalences.add(equivSet);
				count++;
			} else if (reasonString.equals("[[[→S, T←]]]")) {
				System.out.println(list.get(1) + "\tS↔T\t" + list.get(0));
				seenEquivalences.add(equivSet);
				count++;
			}
		}
		System.out.println("Count:\t" + count);
		count = 0;

		System.out.println();
		System.out.println("*** Complicated Relations ***");
		System.out.println();

		final UnicodeSet simp = new UnicodeSet();
		final UnicodeSet trad = new UnicodeSet();
		final UnicodeSet dual = new UnicodeSet();

		for (final Set<String> equivSet : equivalenceSets) {
			if (seenEquivalences.contains(equivSet)) {
				continue;
			}
			System.out.println("Equivalence Class:\t" + subtractStar(equivSet));
			final Set<String> lines = new TreeSet();

			for (final String item : equivSet) {
				if (item.endsWith("*")) {
					continue;
				}
				for (String item2 : equivSet) {
					if (item.equals(item2)) {
						continue;
					}
					final List reason = equivalences.getReasons(item, item2);
					if (reason == null) {
						continue;
					}
					String reasonString = reason.toString();
					reasonString = reasonString.substring(1,reasonString.length()-1);
					if (item2.endsWith("*")) {
						item2 = item2.substring(0,item2.length()-1);
					}
					String line;
					if (reasonString.equals("S←, →T")) {
						line = showLine(item, "\tS↔T\t", item2, simp, trad);
					} else if (reasonString.equals("S←")) {
						line = showLine(item, "\tS←\t", item2, simp, trad);
					} else if (reasonString.equals("→T")) {
						line = showLine(item, "\t→T\t", item2, simp, trad);
						// reverse the following
					} else if (reasonString.equals("→S, T←")) {
						line = showLine(item2, "\tS↔T\t", item, simp, trad);
					} else if (reasonString.equals("T←")) {
						line = showLine(item2, "\t→T\t", item, simp, trad);
					} else if (reasonString.equals("→S")) {
						line = showLine(item2, "\tS←\t", item, simp, trad);
					} else {
						line = (item + "\t" + reasonString + "\t" + item2 + "\t!DATA ERROR?!");
					}
					if (item.contains(item2) || item2.contains(item)) {
						line += "\tDUAL";
						dual.add(item2);
						dual.add(item);
					}

					lines.add(line);
				}
			}
			for (final String line : lines) {
				System.out.println(line);
				count++;
			}
			System.out.println();
		}

		System.out.println("Count:\t" + count);
		System.out.println();
		final UnicodeSet simpAndTrad = new UnicodeSet(simp).retainAll(trad);
		System.out.println("Characters that are both Simp & Trad: " + PRETTY.format(simpAndTrad));
		System.out.println();
		System.out.println("Characters that are both Simp & Trad - Dual: " + PRETTY.format(simpAndTrad.removeAll(dual)));

		if (true)
		{
			return;
			// ==============================
		}

		System.out.println("x →T y & x →S z");
		final UnicodeSet both = new UnicodeSet(simp2trad.keySet()).retainAll(trad2simp.keySet());
		for (final UnicodeSetIterator it = new UnicodeSetIterator(both); it.next();) {
			System.out.println(it.getString() + "\t→T\t" + getVariant(simp2trad, it.codepoint));
			System.out.println(it.getString() + "\t→S\t" + getVariant(trad2simp, it.codepoint));
			System.out.println();
		}

		System.out.println("y →T x & z →S x");
		final Set<String> bothValues = new TreeSet<String>(simp2trad.getAvailableValues());
		bothValues.retainAll(trad2simp.getAvailableValues());
		for (final String value : bothValues) {
			final UnicodeSet simpSource = simp2trad.keySet(value);
			final UnicodeSet tradSource = trad2simp.keySet(value);
			System.out.println(simpSource.toPattern(false) + "\t→T\t" + hexToString(value));
			System.out.println(tradSource.toPattern(false) + "\t→S\t" + hexToString(value));
			System.out.println();
		}

		System.out.println("\tS↔T\t");
		final List<String> output = new ArrayList();
		final Set<String> seen = new HashSet();
		final Set<String> buffered = new LinkedHashSet();
		addItems(simp2trad, trad2simp, output, seen, false, buffered);
		System.out.println();

		System.out.println("x\t→S\ty\t...");
		for (final String line : buffered) {
			System.out.println(line);
		}
		System.out.println();
		buffered.clear();

		addItems(trad2simp, simp2trad, output, seen, true, buffered);
		System.out.println();

		System.out.println("x\t→T\ty\t...");
		for (final String line : buffered) {
			System.out.println(line);
		}
		System.out.println();
		System.out.println("Count:\t" + buffered.size());
	}

	private Set<String> subtractStar(Set<String> equivSet) {
		final Set<String> result = new TreeSet();
		for (final String item : equivSet) {
			if (!item.endsWith("*")) {
				result.add(item);
			}
		}
		return result;
	}

	private String showLine(String item, String relation, String item2, UnicodeSet simp, UnicodeSet trad) {
		String line;
		line = (item + relation + item2);
		simp.addAll(item);
		trad.addAll(item2);
		return line;
	}

	private void addItems(UnicodeMap simp2trad, UnicodeMap trad2simp, List<String> output,
			Set<String> seen, boolean isTrad2Simp, Set<String> buffered) {
		for (final UnicodeSetIterator it = new UnicodeSetIterator(simp2trad.keySet()); it.next();) {
			final String string = it.getString();
			if (seen.contains(string)) {
				continue;
			}
			output.clear();
			if (isTrad2Simp) {
				output.add("");
			}
			output.add(string);
			final int circular = getVariants(simp2trad, trad2simp, it.codepoint, output);
			seen.addAll(output);

			boolean first = true;
			if (circular == 0 && output.size() == 2) {
				System.out.println(output.get(0) + "\tS↔T\t" + output.get(1));
				continue;
			}
			boolean toTrad = true;
			final StringBuffer line = new StringBuffer();
			for (final String code : output) {
				if (first) {
					first = false;
				} else if (toTrad) {
					line.append("\t→T\t");
				} else {
					line.append("\t→S\t");
				}
				line.append(code);
				toTrad = !toTrad;
			}
			if (circular >= 0) {
				line.append("\t→\t" + circular);
			}
			buffered.add(line.toString());
		}
	}

	private int getVariants(UnicodeMap v1, UnicodeMap v2, int codepoint, List<String> output) {
		final String x = getVariant(v1, codepoint);
		if (x == null) {
			return -1;
		}
		final int found = output.indexOf(x);
		if (found >= 0) {
			return found;
		}
		output.add(x);
		if (UTF16.countCodePoint(x) != 1) {
			return -1;
		}
		return getVariants(v2, v1, UTF16.charAt(x, 0), output);
	}

	private String getVariant(UnicodeMap v1, int codepoint) {
		final String trad = (String) v1.getValue(codepoint);
		String result;
		if (trad == null) {
			result = null;
		} else {
			result = hexToString(trad);
			if (result.length() == 0) {
				System.out.println("Problem at " + Utility.hex(codepoint) + " => " + trad);
				return null;
			}
		}
		return result;
	}

	private String hexToString(String trad) {
		//    if (trad.indexOf(' ') >= 0) {
		//      System.out.println("Multiples: " + trad);
		//    }
		trad = trad.replace("U+", "");
		return Utility.fromHex(trad);
	}
}
