package org.unicode.draft;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.jsp.CharEncoder;
import org.unicode.jsp.FileUtilities;
import org.unicode.jsp.FileUtilities.SemiFileReader;

import sun.text.normalizer.UTF16;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.Normalizer2.Mode;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.ULocale;

public class FindHanSizes {
	static final int        SHOW_LIMIT   = 100;
	static Normalizer2      nfkd         = Normalizer2.getInstance(null, "nfkc", Mode.DECOMPOSE);
	static UnicodeSet       NONCANONICAL = new UnicodeSet("[:nfd_qc=n:]");
	static final UnicodeSet HAN;
	static {
		// make sure we include the characters that contain HAN
		HAN = new UnicodeSet("[[:sc=han:][:ideographic:]]");
		for (final String s : new UnicodeSet("[:nfkd_qc=n:]")) {
			if (HAN.containsSome(nfkd.normalize(s))) {
				HAN.add(s);
			}
		}
		HAN.removeAll(NONCANONICAL);
		HAN.freeze();
	}

	enum NamedHanSet {
		zh, zh_Hant, GB2312, GBK, Big5, Big5_HKSCS, Stroke, Pinyin, NewStroke, NewPinyin;

		public static String toString(EnumSet<NamedHanSet> set) {
			final StringBuilder result = new StringBuilder();
			for (final NamedHanSet item : values()) {
				result.append('\t');
				if (set.contains(item)) {
					result.append(item.toString());
				}
			}
			return result.toString();
		}
	}

	static final SetComparator<NamedHanSet> SINGLETON = new SetComparator<NamedHanSet>();
	static Normalizer2                      nfc       = Normalizer2.getInstance(null, "nfc", Mode.COMPOSE);

	static class EncodingInfo {
		Map<Integer, EnumSet<NamedHanSet>> status = new HashMap();

		void add(int cp, NamedHanSet e) {
			final String s = nfc.normalize(UTF16.valueOf(cp));
			final int newCp = s.codePointAt(0);
			if (cp != newCp) {
				if (s.length() != Character.charCount(newCp)) {
					throw new IllegalArgumentException("Han growing??");
				}
				cp = newCp;
			}
			EnumSet<NamedHanSet> set = status.get(cp);
			if (set == null) {
				status.put(cp, set = EnumSet.noneOf(NamedHanSet.class));
			}
			set.add(e);
		}

		public TreeSet<EnumSet<NamedHanSet>> getValues() {
			final Collection<EnumSet<NamedHanSet>> values = status.values();
			final TreeSet<EnumSet<NamedHanSet>> set = new TreeSet(SINGLETON);
			set.addAll(values);
			return set;
		}

		public UnicodeMap<EnumSet<NamedHanSet>> getUnicodeMap() {
			final UnicodeMap<EnumSet<NamedHanSet>> result = new UnicodeMap();
			for (final Entry<Integer, EnumSet<NamedHanSet>> entry : status.entrySet()) {
				result.put(entry.getKey(), entry.getValue());
			}
			return result;
		}

		public void addAll(UnicodeSet tailored, NamedHanSet e) {
			for (final UnicodeSetIterator it = new UnicodeSetIterator(tailored); it.next();) {
				add(it.codepoint, e);
			}
		}

		public UnicodeMap<EnumSet<NamedHanSet>> showContents() {
			final UnicodeMap<EnumSet<NamedHanSet>> unicodeMap = getUnicodeMap();
			final Collection<EnumSet<NamedHanSet>> values = unicodeMap.values();
			final TreeSet<EnumSet<NamedHanSet>> set = new TreeSet(SINGLETON);
			set.addAll(values);

			for (final EnumSet<NamedHanSet> value : set) {
				final UnicodeSet keys = unicodeMap.getSet(value);
				System.out.println(NamedHanSet.toString(value) + "\t" + keys.size() + "\t" + toAbbreviated(SHOW_LIMIT, keys));
			}
			System.out.println("Total:\t" + unicodeMap.size() + "\t" + toAbbreviated(SHOW_LIMIT, unicodeMap.keySet()));
			return unicodeMap;
		}
	}

	/**
	 * Shows only chars, and just to the limit
	 * 
	 * @param input
	 * @param limit
	 * @return
	 */
	static String toAbbreviated(int limit, UnicodeSet input) {
		final int ranges = input.getRangeCount();
		if (ranges * 3 < limit) {
			return input.toPattern(false);
		} else {
			final UnicodeSet smaller = new UnicodeSet();
			int count = 0;
			for (final UnicodeSetIterator it = new UnicodeSetIterator(input); it.nextRange();) {
				count += it.codepoint == it.codepointEnd ? 1 : it.codepoint + 1 == it.codepointEnd ? 2 : 3;
				if (count > limit) {
					break;
				}
				smaller.addAll(it.codepoint, it.codepointEnd);
			}
			return smaller.toPattern(false) + '…';
		}
	}

	public static void main(String[] args) {
		System.out.println("Use GenerateHanCollators for data");
		System.out.println("All Han:\t" + HAN.size() + "\t" + HAN.toPattern(false));
		final Set<String> collators = new TreeSet<String>();
		collators.addAll(Arrays.asList(Collator.getKeywordValuesForLocale("collation", ULocale.CHINESE, false)));
		System.out.println("Collators:\t" + collators);
		for (final String collatorType : collators) {
			final UnicodeSet set = getTailoredHan(collatorType);
			System.out.println(collatorType + "\t" + set.size() + "\t" + set);
		}

		final EncodingInfo info = new EncodingInfo();

		System.out.println("Most Frequent (99.9%)");
		addHanMostFrequent(info, NamedHanSet.zh, 0.999);
		addHanMostFrequent(info, NamedHanSet.zh_Hant, 0.999);
		// addHanMostFrequent(info, NamedHanSet.ja, 0.999);
		info.showContents();

		System.out.println("Current Collators");
		addTailoredHan(info, NamedHanSet.Stroke);
		addTailoredHan(info, NamedHanSet.Pinyin);
		info.showContents();

		System.out.println("New Collators");
		addNewCollator(info, NamedHanSet.NewStroke);
		addNewCollator(info, NamedHanSet.NewPinyin);
		info.showContents();

		System.out.println("Comparing Charsets");

		final SortedMap<String, Charset> charsets = Charset.availableCharsets();
		System.out.println("All charsets:\t" + charsets.keySet());

		final Matcher charsetMatcher = Pattern.compile("GB2312|GBK|Big5|Big5-HKSCS").matcher("");
		for (final String name : charsets.keySet()) {
			if (!charsetMatcher.reset(name).matches()) {
				continue;
			}
			final UnicodeSet result = getCharsetRepertoire(name);
			final NamedHanSet e = NamedHanSet.valueOf(name.replace("-", "_"));
			for (final String s: result) {
				final int cp = s.codePointAt(0);
				info.add(cp, e);
			}
		}

		info.showContents();
	}

	static UnicodeSet getCharsetRepertoire(String name) {
		final UnicodeSet result = new UnicodeSet();
		{
			final Charset charset = Charset.forName(name);
			final CharEncoder encoder = new CharEncoder(charset, true, true);
			for (final String s : HAN) {
				final int cp = s.codePointAt(0);
				if (encoder.getValue(cp, null, 0) > 0) {
					result.add(cp);
				}
			}
		}
		return result;
	}

	private static void addNewCollator(EncodingInfo info, NamedHanSet e) {
		try {
			final BufferedReader in = FileUtilities.openFile(FindHanSizes.class, e + "_repertoire.txt");
			final String contents = FileUtilities.getFileAsString(in);
			final UnicodeSet items = new UnicodeSet(contents);
			items.retainAll(HAN);
			info.addAll(items, e);
			System.out.println(e + "\t" + items.size() + "\t" + toAbbreviated(SHOW_LIMIT, items));
		} catch (final IOException e1) {
			throw new IllegalArgumentException(e1);
		}
	}

	private static void addTailoredHan(EncodingInfo info, NamedHanSet e) {
		final UnicodeSet tailored = getTailoredHan(e.toString());
		info.addAll(tailored, e);
	}

	private static UnicodeSet getTailoredHan(String type) {
		final Collator collator = Collator.getInstance(new ULocale("zh_co_" + type));
		final UnicodeSet tailored = new UnicodeSet(collator.getTailoredSet()).retainAll(HAN).removeAll(NONCANONICAL);
		return tailored;
	}

	static void addHanMostFrequent(EncodingInfo info, NamedHanSet e, double limit) {
		final UnicodeSet results = getMostFrequent(e.toString(), limit);
		info.addAll(results, e);
	}

	public static UnicodeSet getMostFrequent(String e, double limit) {
		final MyReader myReader = new MyReader(limit);
		myReader.process(MyReader.class, e + ".txt");
		final UnicodeSet results = myReader.results;
		return results;
	}

	// 1000 0.8716829002327223 [一七三-下不且世並中主久之九也了事二五
	static class MyReader extends SemiFileReader {
		UnicodeSet results = new UnicodeSet();
		double     limit;

		MyReader(double limit) {
			this.limit = limit;
		}

		public final Pattern SPLIT2 = Pattern.compile("\\s+");

		@Override
		protected String[] splitLine(String line) {
			return SPLIT2.split(line);
		}

		@Override
		protected boolean isCodePoint() {
			return false;
		}

		@Override
		protected boolean handleLine(int start, int end, String[] items) {
			final double inclusion = Double.parseDouble(items[1]);
			if (inclusion <= limit) {
				final UnicodeSet other = new UnicodeSet(items[2]);
				results.addAll(other);
			}
			return true;
		}

	};
}
