package org.unicode.bidi;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class BidiConformanceTestBuilder {

	public static int MAX_SIZE = 4;

	private static BitSet SKIPS = new BitSet();
	static {
		// skip RLE, LRE, RLO, LRO, PDF, and BN
		SKIPS.set(BidiReference.RLE);
		SKIPS.set(BidiReference.LRE);
		SKIPS.set(BidiReference.RLO);
		SKIPS.set(BidiReference.LRO);
		SKIPS.set(BidiReference.PDF);
		SKIPS.set(BidiReference.BN);   
	}

	// have an iterator to get all possible variations less than a given size
	static class Sample {
		private byte[] byte_array = new byte[0];
		private final List<Byte> items = new ArrayList<Byte>();
		private int maxSize;

		public Sample(int maxSize) {
			this.maxSize = maxSize;
		}

		boolean next() {
			for (int i = items.size()-1; i >= 0; --i) {
				Byte oldValue = items.get(i);
				if (oldValue < BidiReference.TYPE_MAX) {
					items.set(i, (byte) (oldValue + 1));
					return true;
				}
				items.set(i, BidiReference.TYPE_MIN); // first value
			}
			if (items.size() < maxSize) {
				items.add(0, BidiReference.TYPE_MIN);
				return true;
			}
			return false;
		}

		public String toString() {
			StringBuilder result = new StringBuilder();
			for (int i = 0; i < items.size(); ++i) {
				if (i != 0) {
					result.append(" ");
				}
				result.append(BidiReference.typenames[items.get(i)]);
			}
			return result.toString();
		}

		public byte[] getArray() {
			if (byte_array.length != items.size()) {
				byte_array = new byte[items.size()];
			}
			for (int i = 0; i < items.size(); ++i) {
				byte_array[i] = items.get(i);
			}
			return byte_array;
		}
	}

	public static void write(PrintWriter out) throws FileNotFoundException {
		int[] linebreaks = new int[1];

		Map<String, Set<String>> resultToSource = new TreeMap<String, Set<String>>(SHORTEST_FIRST);
		Map<String, Integer> condensed = new HashMap<String, Integer>();
		Sample sample = new Sample(MAX_SIZE);

		main:
			while (sample.next()) {
				// make sure B doesn't occur in any but the last
				for (int i = 0; i < sample.items.size() - 1; ++i) {
					if (sample.items.get(i) == BidiReference.B) {
						continue main;
					}
				}

				String typeString = sample.toString();
				byte[] TYPELIST = sample.getArray();
				linebreaks[0] = TYPELIST.length;
				condensed.clear();
				for (byte paragraphEmbeddingLevel = -2; paragraphEmbeddingLevel <= 1; ++paragraphEmbeddingLevel) {

					final String reorderedIndexes = reorderedIndexes(TYPELIST, paragraphEmbeddingLevel, linebreaks);
					Integer bitmask = condensed.get(reorderedIndexes);
					if (bitmask == null) {
						bitmask = 0;
					}
					int reordered = paragraphEmbeddingLevel == -2 ? 3 : paragraphEmbeddingLevel+1;
					bitmask |= 1<<(reordered);
					condensed.put(reorderedIndexes, bitmask);
				}
				for (String reorderedIndexes : condensed.keySet()) {
					Integer bitset = condensed.get(reorderedIndexes);
					addResult(resultToSource, typeString + "; " + Integer.toHexString(bitset).toUpperCase(Locale.ENGLISH), reorderedIndexes);
				}
			}

		//    for (int i = BidiReference.TYPE_MIN; i < BidiReference.TYPE_MAX; ++i) {
		//      UnicodeSet data = new UnicodeSet("[:bidi_class=" + BidiReference.typenames[i] + ":]");
		//      data.complement().complement();
		//      out.println("@Type:\t" + BidiReference.typenames[i] + ":\t" + data);
		//    }
		int totalCount = 0;
		for (String reorderedIndexes : resultToSource.keySet()) {
			out.println();
			String[] parts = reorderedIndexes.split(";");
			out.println("@Levels:\t" + parts[0].trim());
			out.println("@Reorder:\t" + (parts.length < 2 ? "" : parts[1].trim()));
			int count = 0;
			for (String sources : resultToSource.get(reorderedIndexes)) {
				out.println(sources);
				++totalCount;
				++count;
			}
			out.println();
			out.println("#Count:\t" + count);
		}
		out.println();
		out.println("#Total Count:\t" + totalCount);
		System.out.println("#Total Count:\t" + totalCount);
		System.out.println("#Max Length:\t" + MAX_SIZE);
		out.close();
		System.out.println("Done");
	}

	private static void addResult(Map<String, Set<String>> resultToSource, final String source,
			final String reorderedIndexes) {
		Set<String> sources = resultToSource.get(reorderedIndexes);
		if (sources == null) {
			resultToSource.put(reorderedIndexes, sources = new LinkedHashSet());
		}
		sources.add(source);
	}

	private static String reorderedIndexes(byte[] types, byte paragraphEmbeddingLevel, int[] linebreaks) {

		StringBuilder result = new StringBuilder();
		BidiReference bidi = new BidiReference(types, paragraphEmbeddingLevel);

		byte[] levels = bidi.getLevels(linebreaks);
		for (int i = 0; i < levels.length; ++i) {
			if (SKIPS.get(types[i])) {
				result.append(" x");
			} else {
				result.append(' ').append(levels[i]);
			}
		}
		result.append(";");

		int[] reordering = bidi.getReordering(linebreaks);

		int lastItem = -1;
		boolean LTR = true;

		for (int i = 0; i < reordering.length; ++i) {
			final int item = reordering[i];
			if (item < lastItem) {
				LTR = false;
			}
			lastItem = item;
			if (SKIPS.get(types[item])) {
				continue;
			}
			if (result.length() != 0) {
				result.append(" ");
			}
			result.append(item);
		}
		return result.toString();
	}

	static Comparator<String> SHORTEST_FIRST = new Comparator<String>() {

		public int compare(String o1, String o2) {
			int result = o1.length() - o2.length();
			if (result != 0) {
				return result;
			}
			return o1.compareTo(o2);
		}

	};
}
