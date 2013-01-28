package org.unicode.text.UCA;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.test.TestMetadata;
import org.unicode.cldr.util.With;
import org.unicode.text.UCD.UCD_Types;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UProperty.NameChoice;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.UnicodeSet;

public class CompareDucetToCldr {
	private static final Date DATE = new Date();
	private static final String BASE_DIR = Utility.DATA_DIRECTORY + "/UCA/6.1.0/";  // TODO: parameterize
	static class Birelation<K,V> {
		private final Relation<K,V> keyValues;
		private final Relation<V,K> valueKeys;
		public Birelation(Relation<K, V> keyValues, Relation<V, K> valueKeys) {
			this.keyValues = keyValues;
			this.valueKeys = valueKeys;
		}
		public Set<V> getValues(K key) {
			return keyValues.getAll(key);
		}
		public Set<K> getKeys(V value) {
			return valueKeys.getAll(value);
		}
		public static <K,V> Birelation<K,V> of(
				Map<K, Set<V>> map1, Class class1,
				Map<V, Set<K>> map2, Class class2) {
			return new Birelation<K,V>(Relation.of(map1, class1), Relation.of(map2, class2));
		}
		public void put(K key, V value) {
			keyValues.put(key, value);
			valueKeys.put(value, key);
		}
		public <C extends Collection<V>> C values(C arrayList) {
			return keyValues.values(arrayList);
		}
		public Set<Entry<K, Set<V>>> keyValuesSet() {
			return keyValues.keyValuesSet();
		}
	}

	static class HexAndName implements Transform<String,String> {
		final Birelation<WeightList, String> keyValue;
		public HexAndName(Birelation<WeightList, String> ducet) {
			keyValue = ducet;
		}

		@Override
		public String transform(String source) {
			return
					Utility.hex(source, ",")
					+ " ( " + source + " ) "
					+ "[" + UCharacter.getPropertyValueName(UProperty.GENERAL_CATEGORY, UCharacter.getType(source.codePointAt(0)), NameChoice.SHORT) + "] "
					+ UCharacter.getName(source, ", ")
					+ "\t" + keyValue.getKeys(source);
		}
	}

	public static void main(String[] args) throws IOException {
		final Birelation<WeightList, String> cldr = getData(BASE_DIR + "CollationAuxiliary/", "allkeys_CLDR.txt");
		writeValues(cldr, "cldr.txt", false);
		writeValues(cldr, "cldr_weights.txt", true);
		final Birelation<WeightList, String> ducet = getData(BASE_DIR, "allkeys.txt");
		writeValues(ducet, "allkeys.txt", false);
		writeValues(ducet, "allkeys_weights.txt", true);

		System.out.println("(-)ducet to cldr(+)");
		System.out.println(
				TestMetadata.showDifference(
						ducet.values(new ArrayList<String>()),
						cldr.values(new ArrayList<String>()),
						"\n",
						new HexAndName(ducet),
						new HexAndName(cldr))
				);
		//        int diffCount = 0;
		//        Differ<String> differ = new Differ(100, 10);
		//
		//        for (Entry<WeightList, Set<String>> key : cldr.keyValuesSet()) {
		//            WeightList cldrWeights = cldr.get(key);
		//            WeightList ducetWeights = ducet.get
		//            if (!UnicodeProperty.equals(cldrWeights, ducetWeights)) {
		//                System.out.println(Utility.hex(key) +
		//                        " ;\t" + (ducetWeights == null ? "null" : ducetWeights.toString()) +
		//                        " ;\t" + (cldrWeights == null ? "null" : cldrWeights.toString()));
		//                ++diffCount;
		//            }
		//        }
		//        System.out.println(diffCount);
		System.out.println("Skipping " + SKIPPED);

		System.out.println("Done");
	}

	public static void writeValues(Birelation<WeightList, String> cldr, String filename, boolean showWeights) throws IOException {
		final PrintWriter out = BagFormatter.openUTF8Writer(UCD_Types.GEN_DIR + "uca_COMP/", filename);
		out.println("#Date: " + DATE);
		for (final Entry<WeightList, Set<String>> kvs : cldr.keyValuesSet()) {
			final WeightList weights = kvs.getKey();
			for (final String source : kvs.getValue()) {
				out.println (
						"U+" + Utility.hex(source, " U+")
						+ " ( " + source + " )"
						+ " [" + UCharacter.getPropertyValueName(UProperty.GENERAL_CATEGORY, UCharacter.getType(source.codePointAt(0)), NameChoice.SHORT) + "]"
						+ " " + UCharacter.getName(source, ", ")
						+ (showWeights ? " " + weights : ""));
			}
		}
		out.close();
	}

	static UnicodeSet SKIPPED = new UnicodeSet();

	public static Birelation<WeightList, String> getData(String directory, String filename) {
		final Birelation<WeightList, String> cldr = Birelation.of(
				new TreeMap<WeightList, Set<String>>(), TreeSet.class,
				new TreeMap<String, Set<WeightList>>(), TreeSet.class);
		for (final String line : FileUtilities.in(directory, filename)) {
			if (line.startsWith("@version")) {
				continue;
			}
			if (line.contains("Z NOTATION LEFT IMAGE BRACKET")) {
				final int x = 0; // for debugging
			}
			final String[] parts = FileUtilities.cleanSemiFields(line);
			if (parts != null) {
				final String string = Utility.fromHex(parts[0]);
				if (!isCanonical(string)) {
					SKIPPED.add(string);
					continue;
				}
				cldr.put(Weight.parse(parts[1]), string);
			}
		}
		return cldr;
	}

	private static boolean isCanonical(String string) {
		// TODO Make this use UcdProperty
		for (final int cp : With.codePointArray(string)) {
			final int dt = UCharacter.getIntPropertyValue(cp, UProperty.DECOMPOSITION_TYPE);
			if (dt == UCharacter.DecompositionType.CANONICAL) {
				return false;
			}
		}
		return true;
	}

	private static class WeightList implements Comparable<WeightList> {
		final List<Weight> list;
		public WeightList(List<Weight> result) {
			list = result;
		}
		@Override
		public int compareTo(WeightList other) {
			for (int i = 0; i < list.size() && i < other.list.size(); ++i) {
				final int result = list.get(i).compareTo(other.list.get(i));
				if (result != 0) {
					return result;
				}
			}
			return list.size() - other.list.size();
		}
		@Override
		public boolean equals(Object other) {
			return list.equals(((WeightList)other).list);
		}
		@Override
		public String toString() {
			return CollectionUtilities.join(list,"");
		}
	}

	private static class Weight implements Comparable<Weight> {
		static Matcher WEIGHT = Pattern.compile("\\[([.*])([0-9A-F]{4})\\.([0-9A-F]{4})\\.([0-9A-F]{4})\\.([0-9A-F]{4,6})\\]").matcher("");
		final private boolean variable;
		final private int primary;
		final private int secondary;
		final private int tertiary;
		public Weight(boolean variable, int parseInt, int parseInt2, int parseInt3) {
			this.variable = variable;
			primary = parseInt;
			secondary = parseInt2;
			tertiary = parseInt2;
		}
		// 0EC4 0E9C ;  [.24BE.0020.0002.0E9C][.24DC.0020.001F.0EC4]
		static WeightList parse(String input) {
			final List<Weight> result = new ArrayList<Weight>();
			WEIGHT.reset(input);
			int last = 0;
			while (WEIGHT.find()) {
				if (WEIGHT.start() != last) {
					throw new IllegalArgumentException();
				}
				final boolean variable = WEIGHT.group(1).equals("*");
				result.add(new Weight(
						variable,
						Integer.parseInt(WEIGHT.group(2),16),
						Integer.parseInt(WEIGHT.group(3),16),
						Integer.parseInt(WEIGHT.group(4),16)));
				last = WEIGHT.end();
			}
			if (last != input.length()) {
				throw new IllegalArgumentException();
			}
			return new WeightList(result);
		}
		@Override
		public boolean equals(Object object) {
			final Weight other = (Weight)object;
			return variable == other.variable && primary == other.primary && secondary == other.secondary && tertiary == other.tertiary;
		}
		@Override
		public String toString() {
			return "[" +
					(variable ? "*" : ".") +
					Utility.hex(primary) + "." + Utility.hex(secondary) + "." + Utility.hex(tertiary) + "]";
		}
		@Override
		public int compareTo(Weight other) {
			int result;
			if (0 != (result = primary - other.primary)) {
				return result;
			}
			if (0 != (result = secondary - other.secondary)) {
				return result;
			}
			if (0 != (result = tertiary - other.tertiary)) {
				return result;
			}
			return variable == other.variable ? 0 : variable ? -1 : 1;
		}
	}
}
