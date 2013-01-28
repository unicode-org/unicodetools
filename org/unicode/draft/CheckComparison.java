package org.unicode.draft;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.Timer;

import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.RawCollationKey;
import com.ibm.icu.util.ULocale;

public class CheckComparison {
	static final Timer        t         = new Timer();
	static ArrayList<String>  indata    = new ArrayList<String>();
	static ArrayList<String>  mixedData = new ArrayList<String>();
	static final DecimalFormat percent   = (DecimalFormat) NumberFormat.getPercentInstance();
	static {
		percent.setPositivePrefix("+");
	}

	public static void main(String[] args) throws ParseException {
		final NumberFormat foo = NumberFormat.getCurrencyInstance();
		final ParsePosition parsePosition = new ParsePosition(0);
		System.out.println(foo.parse("$124", parsePosition) + ", " + parsePosition);
		parsePosition.setIndex(0);
		System.out.println(foo.parse("$124 ", parsePosition) + ", " + parsePosition);
		parsePosition.setIndex(0);
		System.out.println(foo.parse("$ 124 ", parsePosition) + ", " + parsePosition);
		parsePosition.setIndex(0);
		System.out.println(foo.parse("12 3", parsePosition) + ", " + parsePosition);
		parsePosition.setIndex(0);
		System.out.println(foo.parse("$\u00A0124 ", parsePosition) + ", " + parsePosition);
		parsePosition.setIndex(0);
		System.out.println(foo.parse(" $ 124 ", parsePosition) + ", " + parsePosition);
		parsePosition.setIndex(0);
		System.out.println(foo.parse("124$", parsePosition) + ", " + parsePosition);
		final int dataSize = 100;
		showTimes(ULocale.ENGLISH, dataSize);
		showTimes(ULocale.FRENCH, dataSize);
		showTimes(new ULocale("da"), dataSize);
		showTimes(new ULocale("th"), dataSize);
	}

	private static void showTimes(final ULocale myLocale, final int dataSize) {
		System.out.println();
		System.out.println("Locale:\t" + myLocale.getDisplayName());
		final int constructionIterations = 100000 / dataSize;
		final int queryIterations = 100000 / dataSize;

		final Collator collator = prepareData(myLocale, dataSize);
		final java.text.Collator collator2 = java.text.Collator.getInstance(myLocale.toLocale());

		final long[] directTimes = new long[2];
		System.gc();
		System.out.println("Direct Comparison");
		timeDirectComparison(collator, 1, 1, directTimes); // warm up
		timeDirectComparison(collator, constructionIterations, queryIterations, directTimes);
		System.out.println("Construction Time: " + directTimes[0]);
		System.out.println("Query Time: " + directTimes[1]);

		final long[] directTimes2 = new long[2];
		System.gc();
		System.out.println("Direct Comparison (JDK)");
		timeDirectComparison2(collator2, 1, 1, directTimes2); // warm up
		timeDirectComparison2(collator2, constructionIterations, queryIterations, directTimes2);
		System.out.println("Construction Time: " + directTimes2[0] + showPercent(directTimes2[0], directTimes[0]));
		System.out.println("Query Time: " + directTimes2[1] + showPercent(directTimes2[1], directTimes[1]));

		final long[] sortKeyTimes = new long[2];
		System.gc();
		System.out.println("Sortkey Comparison");
		timeSortkeyComparison(collator, 1, 1, sortKeyTimes); // warm up
		timeSortkeyComparison(collator, constructionIterations, queryIterations, sortKeyTimes);
		System.out.println("Construction Time: " + sortKeyTimes[0] + showPercent(sortKeyTimes[0], directTimes[0]));
		System.out.println("Query Time: " + sortKeyTimes[1] + showPercent(sortKeyTimes[1], directTimes[1]));
	}

	private static String showPercent(long l, long m) {
		// TODO Auto-generated method stub
		return ",\t" + percent.format((l / (double) m) - 1.0d);
	}

	private static void timeSortkeyComparison(Collator collator, final int constructionIterations,
			final int queryIterations, long[] times) {
		final Map<RawCollationKey, String> sortKeyComparison = new TreeMap<RawCollationKey, String>();

		t.start();
		for (int i = constructionIterations; i >= 0; --i) {
			sortKeyComparison.clear();
			for (final String s : indata) {
				sortKeyComparison.put(collator.getRawCollationKey(s, new RawCollationKey()), s);
			}
		}
		t.stop();
		times[0] = t.getDuration();

		int count2 = 0;
		t.start();
		final RawCollationKey reusedKey = new RawCollationKey();
		for (int i = queryIterations; i >= 0; --i) {
			for (final String s : mixedData) {
				if (sortKeyComparison.containsKey(collator.getRawCollationKey(s, reusedKey))) {
					++count2;
				}
			}
		}
		t.stop();
		times[1] = t.getDuration();
	}

	private static void timeDirectComparison(Collator collator, final int constructionIterations,
			final int queryIterations, long[] times) {
		final Set<String> plainComparison = new TreeSet<String>(collator);

		t.start();
		for (int i = constructionIterations; i >= 0; --i) {
			plainComparison.clear();
			plainComparison.addAll(indata);
		}
		t.stop();
		times[0] = t.getDuration();

		int count = 0;
		t.start();
		for (int i = queryIterations; i >= 0; --i) {
			for (final String s : mixedData) {
				if (plainComparison.contains(s)) {
					++count;
				}
			}
		}
		t.stop();
		times[1] = t.getDuration();
	}

	private static void timeDirectComparison2(java.text.Collator collator, final int constructionIterations,
			final int queryIterations, long[] times) {
		final Set<String> plainComparison = new TreeSet<String>(collator);

		t.start();
		for (int i = constructionIterations; i >= 0; --i) {
			plainComparison.clear();
			plainComparison.addAll(indata);
		}
		t.stop();
		times[0] = t.getDuration();

		int count = 0;
		t.start();
		for (int i = queryIterations; i >= 0; --i) {
			for (final String s : mixedData) {
				if (plainComparison.contains(s)) {
					++count;
				}
			}
		}
		t.stop();
		times[1] = t.getDuration();
	}


	private static Collator prepareData(final ULocale myLocale, int maxCount) {
		final Collator collator = Collator.getInstance(myLocale);
		int count = 0;

		indata = new ArrayList<String>(maxCount);
		mixedData = new ArrayList<String>(maxCount);

		main: for (final String languageCode : ULocale.getISOLanguages()) {
			final String languageName = ULocale.getDisplayLanguage(languageCode, myLocale);
			for (int i = UScript.COMMON; i < UScript.CODE_LIMIT; ++i) {
				final String scriptCode = UScript.getShortName(i);
				final String scriptName = ULocale.getDisplayScript("und-" + scriptCode, myLocale);
				for (final String countryCode : ULocale.getISOCountries()) {
					final String countryName = ULocale.getDisplayCountry("und-" + countryCode, myLocale);
					final String someString = languageName + ", " + scriptName + ", " + countryName;
					final String someString2 = scriptName + ", " + countryName + ", " + languageName;
					indata.add(someString2);
					mixedData.add(someString2);
					mixedData.add(someString);
					if (++count >= maxCount) {
						break main;
					}
				}
			}
		}

		System.out.println("Item count: " + indata.size());
		System.out.println("Query item count: " + mixedData.size());
		return collator;
	}
}
