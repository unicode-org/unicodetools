package org.unicode.text.tools;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.Log;
import org.unicode.cldr.util.Segmenter;
import org.unicode.cldr.util.Segmenter.Builder;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.Normalizer;
import org.unicode.text.UCD.ToolUnicodePropertySource;
import org.unicode.text.UCD.UCD;
import org.unicode.text.UCD.UCD_Names;
import org.unicode.text.UCD.UCD_Types;

import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.dev.util.Tabber;
import com.ibm.icu.dev.util.TransliteratorUtilities;
import com.ibm.icu.dev.util.UnicodeLabel;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeProperty;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public class VerifyUCD {

	private static final boolean FULL = false;

	public static void main(String[] args) throws IOException {
		// System.out.println(new File(UCD_Types.BASE_DIR).getCanonicalPath());
		final String x = Default.ucd().getCase("\u0130", UCD_Types.FULL, UCD_Types.LOWER);
		final String y = Default.ucd().getCase(Default.nfd().normalize("\u0130"), UCD_Types.FULL, UCD_Types.LOWER);

		Log.setLog(UCD_Types.GEN_DIR + "verifyUCD.html");
		Log.logln("<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'>");
		Log.logln("<title>UCD Canonical Check</title></head><body>");
		Log.getLog().println("<h2 align='right'>L2/06-386R2</h2>");
		Log.logln("<h1>UCD Canonical Check</title></h1>");
		//Log.logln("<p>" + new java.util.Date() + "</title></p>");
		final String property = System.getProperty("method");

		try {
			if (property != null) {
				org.unicode.cldr.util.CldrUtility.callMethod(property, VerifyUCD.class);
			} else {
				checkBidiMirroredNonspacingMarks();
				checkSegmentation();
				checkCanonicalEquivalenceOfProperties();
				if (FULL) {
					checkCompatibilityEquivalenceOfProperties();
				}
			}

		} finally {
			System.out.println("Done");
			Log.logln("</body></html>");
			Log.close();
		}
	}

	static final int COMBINING_MASK = (1<<UCD.Mc) | (1<<UCD.Me) | (1<<UCD.Mn);

	public static void checkBidiMirroredNonspacingMarks() {
		final ToolUnicodePropertySource ups = ToolUnicodePropertySource.make(Default.ucdVersion());
		final UnicodeSet bidiMirrored = ups.getSet("BidiMirrored=true");
		final UnicodeSet contents = new UnicodeSet();
		final UnicodeSet marks = new UnicodeSet(ups.getSet("generalcategory=Mn"));
		marks.addAll(ups.getSet("generalcategory=Me"));
		marks.addAll(ups.getSet("generalcategory=Mc"));
		for (final UnicodeSetIterator it = new UnicodeSetIterator(bidiMirrored); it.next();) {
			final int codepoint = it.codepoint;
			addMarks(contents, marks, codepoint, Default.nfd());
			addMarks(contents, marks, codepoint, Default.nfc());
			addMarks(contents, marks, codepoint, Default.nfkd());
			addMarks(contents, marks, codepoint, Default.nfkc());
		}
		final BagFormatter bf = new BagFormatter();
		bf.setShowLiteral(TransliteratorUtilities.toHTMLControl);
		bf.setTabber(new Tabber.HTMLTabber());
		Log.logln("<h2>*Characters with Bidi_Mirrored=True containing one or more marks*</h2>");
		bf.showSetNames(Log.getLog(),contents);
	}

	private static void addMarks(UnicodeSet contents, UnicodeSet marks, int codepoint, Normalizer normalizer) {
		if (marks.containsSome(normalizer.normalize(codepoint))) {
			contents.add(codepoint);
		}
	}

	public static void checkCanonicalEquivalenceOfProperties() {
		doEquivalenceOfProperties(Equivalence.CANONICAL);
	}

	public static void checkCompatibilityEquivalenceOfProperties() {
		doEquivalenceOfProperties(Equivalence.COMPATIBLITY);
	}

	public static void doEquivalenceOfProperties(Equivalence equivalence) {
		Log.logln("<h2>" + equivalence + " Equivalence of Properties</h2>");

		final ToolUnicodePropertySource ups = ToolUnicodePropertySource.make(Default.ucdVersion());
		final UnicodeSet setToTest = ups.getSet(equivalence == Equivalence.CANONICAL ? "NFDQuickCheck=No" : "NFKDQuickCheck=No");

		final TreeSet<String> properties = new TreeSet<String>();
		final Set<String> availablePropertyNames = new TreeSet<String>(ups.getAvailableNames(UnicodeProperty.BINARY_MASK
				| UnicodeProperty.ENUMERATED_OR_CATALOG_MASK
				| (1<<UnicodeProperty.NUMERIC
						| UnicodeProperty.STRING_OR_MISC_MASK)));
		final Set<String> removals = new TreeSet<String>(Arrays.asList(new String[] { "Name",
				"Unicode_1_Name", "East_Asian_Width",
				"IdnOutput",

				"Simple_Case_Folding",
				"Simple_Titlecase_Mapping", "Simple_Lowercase_Mapping",
				"Simple_Uppercase_Mapping",
				/*
        "Titlecase_Mapping", "Lowercase_Mapping",
        "Uppercase_Mapping",
				 */
				"Case_Stable",

				"Decomposition_Mapping",
				"Age", "Composition_Exclusion", "Canonical_Combining_Class", "Pattern_Syntax", "Pattern_White_Space", "Expands_On_NFC", "Expands_On_NFD",
				"Expands_On_NFKC", "Expands_On_NFKD", "Block", "Decomposition_Type", "Deprecated", "Full_Composition_Exclusion",
				"NFC_Quick_Check", "Unified_Ideograph", "NFD_Quick_Check", "NFKC_Quick_Check", "NFKD_Quick_Check",
				"Other_Alphabetic", "Other_Default_Ignorable_Code_Point", "Other_Grapheme_Extend", "Other_ID_Continue", "Other_ID_Start", "Other_Lowercase", "Other_Math", "Other_Uppercase"
		}));
		if (equivalence == Equivalence.COMPATIBLITY) {
			removals.addAll(Arrays.asList("XID_Start", "XID_Continue", "ID_Start", "ID_Continue"));
		}
		removals.retainAll(availablePropertyNames);

		final UnicodeSet forceNFC = new UnicodeSet()
		.addAll(ups.getSet("Hangul_Syllable_Type=LV_Syllable"))
		.addAll(ups.getSet("Hangul_Syllable_Type=LVT_Syllable"))
		.addAll(ups.getSet("General_Category=Titlecase_Letter"))
		.addAll("\u1B3B\u1B3D\u1B43\u0CC0\u0CC7\u0CC8\u0CCA\u0CCB")
		;
		final Set<String> singleCharOnly = new TreeSet<String>(Arrays.asList(new String[] {
				"ASCII_Hex_Digit", "Hex_Digit", "Bidi_Mirroring_Glyph", "Soft_Dotted"}));

		//System.out.println("Other:\t" + ups.getAvailableNames(UnicodeProperty.STRING_OR_MISC_MASK));
		//removals.addAll(ups.getAvailableNames(UnicodeProperty.STRING_OR_MISC_MASK));
		availablePropertyNames.removeAll(removals);
		Log.getLog().println("<table>");
		Log.getLog().println("<tr><td><b>Testing:</b></td><td>" + availablePropertyNames + "</td><tr>");
		Log.getLog().println("<tr><td><b>Skipping:</b></td><td>" + removals + "</td><tr>");
		Log.getLog().println("</table><br>");
		Log.logln("<hr>");
		final UnicodeMap results = new UnicodeMap();
		final Map<String,UnicodeMap> sidewaysResults = new TreeMap<String,UnicodeMap>();

		// http://demo.icu-project.org/icu-bin/ubrowse?go=2224
		for (final UnicodeSetIterator it = new UnicodeSetIterator(setToTest); it.next();) {
			final int codepoint = it.codepoint;
			final String normalized = (forceNFC.contains(codepoint)
					? equivalence == Equivalence.CANONICAL ? Default.nfc() : Default.nfkc()
							: equivalence == Equivalence.CANONICAL ? Default.nfd() : Default.nfkd()).normalize(codepoint);
			properties.clear();
			for (final String propertyName : availablePropertyNames) {
				if (UTF16.hasMoreCodePointsThan(normalized,1) && singleCharOnly.contains(propertyName)) {
					continue;
				}
				final UnicodeProperty up = ups.getProperty(propertyName);
				final boolean isStringProp = ((1<<up.getType()) & UnicodeProperty.STRING_OR_MISC_MASK) != 0;

				final Object value1 = getValue(up, codepoint);
				int newCodepoint;
				String nfcStringPropertyValue = "";
				for (int i = 0; i < normalized.length(); i+=UTF16.getCharCount(newCodepoint)) {
					newCodepoint = UTF16.charAt(normalized, i);
					final int catMask = Default.ucd().getCategoryMask(newCodepoint);
					// special case strings
					if (isStringProp) {
						Object value2 = getValue(up, newCodepoint);
						if (value2 == null) {
							value2 = UTF16.valueOf(newCodepoint);
						}
						nfcStringPropertyValue += value2;
						continue;
					}

					if (i > 0 && (catMask & COMBINING_MASK) != 0) {
						continue;
					}
					final Object value2 = up.getValue(newCodepoint);
					if (!equals(value1,value2, Equivalence.PLAIN)) {
						addPropertyDifference(sidewaysResults, properties, codepoint, propertyName, value1, value2);
					}
				}
				if (propertyName.contains("case_Mapping") || propertyName.contains("Case_Folding")) {
					nfcStringPropertyValue = caseMapping(normalized, propertyName);
				}
				if (isStringProp && !equals(value1, nfcStringPropertyValue, equivalence)) {
					addPropertyDifference(sidewaysResults, properties, codepoint, propertyName, value1, nfcStringPropertyValue);
				}
			}
			if (properties.size() != 0) {
				results.put(codepoint, properties.clone());
			}
		}
		showProperties(results, sidewaysResults, equivalence);
	}

	enum Equivalence {PLAIN, CANONICAL, COMPATIBLITY};

	private static void showProperties(UnicodeMap results, Map<String, UnicodeMap> sidewaysResults, final Equivalence
			equivalence) {
		final UnicodeLabel nameLabel = new UnicodeLabel() {
			@Override
			public String getValue(int codepoint, boolean isShort) {
				final String nfd = (equivalence == Equivalence.CANONICAL ? Default.nfd() : Default.nfkd()).normalize(codepoint);
				return Default.ucd().getCodeAndName(codepoint,UCD_Types.NORMAL,TransliteratorUtilities.toHTMLControl)
						+ "\t\u2192\t"
						+ Default.ucd().getCodeAndName(nfd,UCD_Types.NORMAL,TransliteratorUtilities.toHTMLControl);
			}
		};
		final BagFormatter bf = new BagFormatter();
		bf.setNameSource(nameLabel);
		bf.setTabber(new Tabber.HTMLTabber());
		bf.setMergeRanges(false);
		final TreeSet<Set> sorted = new TreeSet<Set>(new CollectionOfComparablesComparator());
		sorted.addAll(results.getAvailableValues());
		for (final Object props : sorted) {
			Log.logln("<p><b>" + props + "</b></p>");
			Log.logln(bf.showSetNames(results.keySet(props)));
		}

		Log.logln("<hr>");
		Log.logln("<h2>" + "By Property" + "</h1>");
		for (final String propName : sidewaysResults.keySet()) {
			final UnicodeMap map = sidewaysResults.get(propName);
			bf.setValueSource((new UnicodeProperty.UnicodeMapProperty() {
			}).set(map).setMain(propName + "_diff", propName + "_diff",
					UnicodeProperty.EXTENDED_STRING, "1.0"));

			Log.logln("<p><b>" + propName + "</b></p>");
			Log.logln(bf.showSetNames(map.keySet()));
		}
	}

	private static String caseMapping(String source, String propertyName) {
		final byte operation = propertyName.contains("Uppercase") ? UCD_Types.UPPER
				: propertyName.contains("Lowercase") ? UCD_Types.LOWER
						: propertyName.contains("Titlecase") ? UCD_Types.TITLE
								: UCD_Types.FOLD;
		final byte style = propertyName.contains("Simple") ? UCD_Types.SIMPLE : UCD_Types.FULL;
		return Default.ucd().getCase(source,style, operation);
	}

	private static void addPropertyDifference(Map<String, UnicodeMap> sidewaysResults, TreeSet<String> properties, int codePoint, String propName, Object value1, Object value2) {
		properties.add(propName + "=" + value1 + "\u2260" + value2);
		UnicodeMap umap = sidewaysResults.get(propName);
		if (umap == null) {
			sidewaysResults.put(propName, umap = new UnicodeMap());
		}
		umap.put(codePoint, value1 + "\u2260" + value2);
	}

	private static Object getValue(UnicodeProperty up, int codepoint) {
		final int type = 1<<up.getType();
		Object value1 = up.getValue(codepoint);
		if (value1 == null) {
			if ((type & UnicodeProperty.STRING_OR_MISC_MASK) != 0) {
				value1 = UTF16.valueOf(codepoint);
			} else if ((type & UnicodeProperty.BINARY_MASK) != 0) {
				value1 = UCD_Names.NO;
			}
		}
		return value1;
	}

	static boolean equals(Object value1, Object value2, Equivalence equivalence) {
		if (value1 == value2) {
			return true;
		}
		if (value1 == null || value2 == null) {
			return false;
		}
		if (equivalence == Equivalence.CANONICAL) {
			return Default.nfd().normalize(value1.toString()).equals(Default.nfd().normalize(value2.toString()));
		} else if (equivalence == Equivalence.COMPATIBLITY) {
			return Default.nfkd().normalize(value1.toString()).equals(Default.nfkd().normalize(value2.toString()));
		}
		return value1.equals(value2);
	}

	static public class CollectionOfComparablesComparator implements Comparator {
		@Override
		public int compare(Object o1, Object o2) {
			if (o1 == null) {
				if (o2 == null) {
					return 0;
				}
				return -1;
			} else if (o2 == null) {
				return 1;
			}
			final Iterator i1 = ((Collection) o1).iterator();
			final Iterator i2 = ((Collection) o2).iterator();
			while (i1.hasNext() && i2.hasNext()) {
				final Comparable a = (Comparable) i1.next();
				final Comparable b = (Comparable) i2.next();
				final int result = a.compareTo(b);
				if (result != 0) {
					return result;
				}
			}
			// if we run out, the shortest one is first
			if (i1.hasNext()) {
				return 1;
			}
			if (i2.hasNext()) {
				return -1;
			}
			return 0;
		}

	}

	static public void checkSegmentation() {
		// verify that every character is a single grapheme cluster, even if canonically decomposed.
		Log.logln("<h2><b>Characters that break within their NFD form.</b></h2>");
		for (final String type : new String[] {"GraphemeClusterBreak", "WordBreak", "LineBreak", "SentenceBreak"}) {
			final Builder segBuilder = Segmenter.make(ToolUnicodePropertySource.make(Default.ucdVersion()), type);
			final Segmenter seg = segBuilder.make();

			// quick test
			Log.logln("<h3>Testing for breaks in toNFD(chars) with " + type + "</h3>");

			// U+1D15E
			final int failures = 0;
			for (int cp = 0; cp <= 0x10FFFF; ++cp) {
				final int cat = Default.ucd().getCategory(cp);
				if (cat == UCD_Types.Cs || cat == UCD_Types.Cn || cat == UCD_Types.Co) {
					continue;
				}
				final String nfd = Default.nfd().normalize(cp);
				if (!UTF16.hasMoreCodePointsThan(nfd, 1)) {
					continue;
				}
				for (int i = 1; i < nfd.length() - 1; ++i) {
					final boolean b = seg.breaksAt(nfd, i);
					if (b) {
						seg.breaksAt(nfd, i);
						Log.logln("<p>Failure with " + Default.ucd().getCodeAndName(cp)
								+ " => " + Default.ucd().getCodeAndName(nfd)
								+ " @ " + i
								+ "</p>");
					}
				}
			}
			Log.logln("<p>Failures: " + failures + "</p>");
		}
	}

}