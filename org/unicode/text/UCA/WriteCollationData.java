/**
 *******************************************************************************
 * Copyright (C) 1996-2001, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /home/cvsroot/unicodetools/org/unicode/text/UCA/WriteCollationData.java,v $
 *
 *******************************************************************************
 */

package org.unicode.text.UCA;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.CollationElementIterator;
import java.text.Collator;
import java.text.DateFormat;
import java.text.RuleBasedCollator;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Pattern;

import org.unicode.text.UCA.UCA.CollatorType;
import org.unicode.text.UCA.UCA.Remap;
import org.unicode.text.UCA.UCA_Statistics.RoBitSet;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.ToolUnicodePropertySource;
import org.unicode.text.UCD.UCD;
import org.unicode.text.UCD.UCD_Types;
import org.unicode.text.utility.CompactByteArray;
import org.unicode.text.utility.CompactShortArray;
import org.unicode.text.utility.IntStack;
import org.unicode.text.utility.UTF32;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.UnicodeProperty;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

public class WriteCollationData {
	private static final boolean SHOW_NON_MAPPED = false;

	private static final boolean ADD_TIBETAN = true;

	private static final String UNICODE_VERSION = UCD.latestVersion;

	private static UCA                 ducetCollator;
	private static UCA                 cldrCollator;
	private static UCA                 cldrWithoutFFFxCollator;

	private static PrintWriter              log;

	// Called by UCA.Main.
	// TODO: Remove? This code tests the Java Collator. Useful?
	static void javatest() throws Exception {
		checkJavaRules("& J , K / B & K , M", new String[] { "JA", "MA", "KA", "KC", "JC", "MC" });
		checkJavaRules("& J , K / B , M", new String[] { "JA", "MA", "KA", "KC", "JC", "MC" });
	}

	private static void checkJavaRules(String rules, String[] tests) throws Exception {
		System.out.println();
		System.out.println("Rules: " + rules);
		System.out.println();

		// duplicate the effect of ICU 1.8 by grabbing the default rules and
		// appending

		final RuleBasedCollator defaultCollator = (RuleBasedCollator) Collator.getInstance(Locale.US);
		final RuleBasedCollator col = new RuleBasedCollator(defaultCollator.getRules() + rules);

		// check to make sure each pair is in order

		int i = 1;
		for (; i < tests.length; ++i) {
			System.out.println(tests[i - 1] + "\t=> " + showJavaCollationKey(col, tests[i - 1]));
			if (col.compare(tests[i - 1], tests[i]) > 0) {
				System.out.println("Failure: " + tests[i - 1] + " > " + tests[i]);
			}
		}
		System.out.println(tests[i - 1] + "\t=> " + showJavaCollationKey(col, tests[i - 1]));
	}

	private static String showJavaCollationKey(RuleBasedCollator col, String test) {
		final CollationElementIterator it = col.getCollationElementIterator(test);
		String result = "[";
		for (int i = 0;; ++i) {
			final int ce = it.next();
			if (ce == CollationElementIterator.NULLORDER) {
				break;
			}
			if (i != 0) {
				result += ", ";
			}
			result += Utility.hex(ce, 8);
		}
		return result + "]";
	}

	// Called by UCA.Main.
	static void writeCaseExceptions() {
		System.err.println("Writing Case Exceptions");
		for (char a = 0; a < 0xFFFF; ++a) {
			if (!Default.ucd().isRepresented(a)) {
				continue;
				// if (0xA000 <= a && a <= 0xA48F) continue; // skip YI
			}

			final String b = Case.fold(a);
			final String c = Default.nfkc().normalize(b);
			final String d = Case.fold(c);
			final String e = Default.nfkc().normalize(d);
			if (!e.equals(c)) {
				System.out.println(Utility.hex(a) + "; " + Utility.hex(d, " ") + " # " + Default.ucd().getName(a));
				/*
				 * System.out.println(Utility.hex(a) + ", " + Utility.hex(b,
				 * " ") + ", " + Utility.hex(c, " ") + ", " + Utility.hex(d,
				 * " ") + ", " + Utility.hex(e, " "));
				 * 
				 * System.out.println(ucd.getName(a) + ", " + ucd.getName(b) +
				 * ", " + ucd.getName(c) + ", " + ucd.getName(d) + ", " +
				 * ucd.getName(e));
				 */
			}
			final String f = Case.fold(e);
			final String g = Default.nfkc().normalize(f);
			if (!f.equals(d) || !g.equals(e)) {
				System.out.println("!!!!!!SKY IS FALLING!!!!!!");
			}
		}
	}

	// Called by UCA.Main.
	static void writeCaseFolding() throws IOException {
		System.err.println("Writing Javascript data");
		final BufferedReader in = Utility.openUnicodeFile("CaseFolding", UNICODE_VERSION, true, Utility.LATIN1);
		// new BufferedReader(new FileReader(DIR31 +
		// "CaseFolding-3.d3.alpha.txt"), 64*1024);
		// log = new PrintWriter(new FileOutputStream("CaseFolding_data.js"));
		log = Utility.openPrintWriter(UCA.getUCA_GEN_DIR(), "CaseFolding_data.js", Utility.UTF8_WINDOWS);
		log.println("var CF = new Object();");
		int count = 0;
		while (true) {
			String line = in.readLine();
			if (line == null) {
				break;
			}
			final int comment = line.indexOf('#'); // strip comments
			if (comment != -1) {
				line = line.substring(0, comment);
			}
			if (line.length() == 0) {
				continue;
			}
			final int semi1 = line.indexOf(';');
			final int semi2 = line.indexOf(';', semi1 + 1);
			final int semi3 = line.indexOf(';', semi2 + 1);
			final char type = line.substring(semi1 + 1, semi2).trim().charAt(0);
			if (type == 'C' || type == 'F' || type == 'T') {
				final String code = line.substring(0, semi1).trim();
				String result = " " + line.substring(semi2 + 1, semi3).trim();
				result = replace(result, ' ', "\\u");
				log.println("\t CF[0x" + code + "]='" + result + "';");
				count++;
			}
		}
		log.println("// " + count + " case foldings total");

		in.close();
		log.close();
	}

	private static String replace(String source, char toBeReplaced, String toReplace) {
		final StringBuffer result = new StringBuffer();
		for (int i = 0; i < source.length(); ++i) {
			final char c = source.charAt(i);
			if (c == toBeReplaced) {
				result.append(toReplace);
			} else {
				result.append(c);
			}
		}
		return result.toString();
	}

	// Called by UCA.Main.
	static void writeJavascriptInfo() throws IOException {
		System.err.println("Writing Javascript data");
		// Normalizer normKD = new Normalizer(Normalizer.NFKD, UNICODE_VERSION);
		// Normalizer normD = new Normalizer(Normalizer.NFD, UNICODE_VERSION);
		// log = new PrintWriter(new FileOutputStream("Normalization_data.js"));
		log = Utility.openPrintWriter(UCA.getUCA_GEN_DIR(), "Normalization_data.js", Utility.LATIN1_WINDOWS);

		int count = 0;
		int datasize = 0;
		int max = 0;
		int over7 = 0;
		log.println("var KD = new Object(); // NFKD compatibility decomposition mappings");
		log.println("// NOTE: Hangul is done in code!");
		CompactShortArray csa = new CompactShortArray((short) 0);

		for (char c = 0; c < 0xFFFF; ++c) {
			if ((c & 0xFFF) == 0) {
				System.err.println(Utility.hex(c));
			}
			if (0xAC00 <= c && c <= 0xD7A3) {
				continue;
			}
			if (!Default.nfkd().isNormalized(c)) {
				++count;
				final String decomp = Default.nfkd().normalize(c);
				datasize += decomp.length();
				if (max < decomp.length()) {
					max = decomp.length();
				}
				if (decomp.length() > 7) {
					++over7;
				}
				csa.setElementAt(c, (short) count);
				log.println("\t KD[0x" + Utility.hex(c) + "]='\\u" + Utility.hex(decomp, "\\u") + "';");
			}
		}
		csa.compact();
		log.println("// " + count + " NFKD mappings total");
		log.println("// " + datasize + " total characters of results");
		log.println("// " + max + " string length, maximum");
		log.println("// " + over7 + " result strings with length > 7");
		log.println("// " + csa.storage() + " trie length (doesn't count string size)");
		log.println();

		count = 0;
		datasize = 0;
		max = 0;
		log.println("var D = new Object();  // NFD canonical decomposition mappings");
		log.println("// NOTE: Hangul is done in code!");
		csa = new CompactShortArray((short) 0);

		for (char c = 0; c < 0xFFFF; ++c) {
			if ((c & 0xFFF) == 0) {
				System.err.println(Utility.hex(c));
			}
			if (0xAC00 <= c && c <= 0xD7A3) {
				continue;
			}
			if (!Default.nfd().isNormalized(c)) {
				++count;
				final String decomp = Default.nfd().normalize(c);
				datasize += decomp.length();
				if (max < decomp.length()) {
					max = decomp.length();
				}
				csa.setElementAt(c, (short) count);
				log.println("\t D[0x" + Utility.hex(c) + "]='\\u" + Utility.hex(decomp, "\\u") + "';");
			}
		}
		csa.compact();

		log.println("// " + count + " NFD mappings total");
		log.println("// " + datasize + " total characters of results");
		log.println("// " + max + " string length, maximum");
		log.println("// " + csa.storage() + " trie length (doesn't count string size)");
		log.println();

		count = 0;
		datasize = 0;
		log.println("var CC = new Object(); // canonical class mappings");
		final CompactByteArray cba = new CompactByteArray();

		for (char c = 0; c < 0xFFFF; ++c) {
			if ((c & 0xFFF) == 0) {
				System.err.println(Utility.hex(c));
			}
			final int canClass = Default.nfkd().getCanonicalClass(c);
			if (canClass != 0) {
				++count;

				log.println("\t CC[0x" + Utility.hex(c) + "]=" + canClass + ";");
			}
		}
		cba.compact();
		log.println("// " + count + " canonical class mappings total");
		log.println("// " + cba.storage() + " trie length");
		log.println();

		count = 0;
		datasize = 0;
		log.println("var C = new Object();  // composition mappings");
		log.println("// NOTE: Hangul is done in code!");

		System.out.println("WARNING -- COMPOSITIONS UNFINISHED!!");

		/*
		 * 
		 * IntHashtable.IntEnumeration enum = Default.nfkd.getComposition();
		 * while (enum.hasNext()) { int key = enum.next(); char val = (char)
		 * enum.value(); if (0xAC00 <= val && val <= 0xD7A3) continue; ++count;
		 * log.println("\tC[0x" + Utility.hex(key) + "]=0x" + Utility.hex(val) +
		 * ";"); } log.println("// " + count + " composition mappings total");
		 * log.println();
		 */

		log.close();
		System.err.println("Done writing Javascript data");
	}

	static void writeVersionAndDate(PrintWriter log, String filename, boolean auxiliary) {
		log.println("# File:        " + filename);
		log.println("# UCA Version: " + getCollator(CollatorType.ducet).getDataVersion());
		log.println("# UCD Version: " + getCollator(CollatorType.ducet).getDataVersion());
		log.println("# Generated:   " + getNormalDate());
		log.println("# For a description of the format and usage, see Collation" +
				(auxiliary ? "Auxiliary" : "Test") +
				".html");
		log.println();
	}

	// Called by UCA.Main.
	static void writeContractions() throws IOException {
		final String fullFileName = "UCA_Contractions.txt";
		final PrintWriter diLog = Utility.openPrintWriter(UCA.getUCA_GEN_DIR(), fullFileName, Utility.UTF8_WINDOWS);

		diLog.write('\uFEFF');

		final UCA.UCAContents cc = getCollator(CollatorType.ducet).getContents(Default.nfd());

		diLog.println("# Contractions");
		writeVersionAndDate(diLog, fullFileName, true);
		while (true) {
			final String s = cc.next();
			if (s == null) {
				break;
			}
			final CEList ces = cc.getCEs();

			if (s.length() > 1) {
				diLog.println(Utility.hex(s, " ")
						+ ";\t #" + ces
						+ " ( " + s + " )"
						+ " " + Default.ucd().getName(s));
			}
		}
		diLog.close();
	}

	// Called by UCA.Main.
	static void checkDisjointIgnorables() throws IOException {
		final PrintWriter diLog = Utility.openPrintWriter(UCA.getUCA_GEN_DIR(), "DisjointIgnorables.js", Utility.UTF8_WINDOWS);

		diLog.write('\uFEFF');

		/*
		 * PrintWriter diLog = new PrintWriter( // try new one new
		 * UTF8StreamWriter(new FileOutputStream(UCA_GEN_DIR +
		 * "DisjointIgnorables.txt"), 32*1024)); diLog.write('\uFEFF');
		 */

		// diLog = new PrintWriter(new FileOutputStream(UCA_GEN_DIR +
		// "DisjointIgnorables.txt"));

		// Normalizer nfd = new Normalizer(Normalizer.NFD, UNICODE_VERSION);

		final int[] secondariesZP = new int[400];
		final Vector<String>[] secondariesZPsample = new Vector[400];
		final int[] remapZP = new int[400];

		final int[] secondariesNZP = new int[400];
		final Vector<String>[] secondariesNZPsample = new Vector[400];
		final int[] remapNZP = new int[400];

		for (int i = 0; i < secondariesZP.length; ++i) {
			secondariesZPsample[i] = new Vector<String>();
			secondariesNZPsample[i] = new Vector<String>();
		}

		int zpCount = 0;
		int nzpCount = 0;

		/*
		 * for (char ch = 0; ch < 0xFFFF; ++ch) { byte type =
		 * collator.getCEType(ch); if (type >= UCA.FIXED_CE) continue; if
		 * (SKIP_CANONICAL_DECOMPOSIBLES && nfd.hasDecomposition(ch)) continue;
		 * String s = String.valueOf(ch); int len = collator.getCEs(s, true,
		 * ces);
		 */
		final UCA.UCAContents cc = getCollator(CollatorType.ducet).getContents(Default.nfd());

		final Set<String> sortedCodes = new TreeSet<String>();
		final Set<String> mixedCEs = new TreeSet<String>();

		while (true) {
			final String s = cc.next();
			if (s == null) {
				break;
			}

			// process all CEs. Look for controls, and for mixed
			// ignorable/non-ignorables
			final CEList ces = cc.getCEs();

			int ccc;
			for (int kk = 0; kk < s.length(); kk += UTF32.count16(ccc)) {
				ccc = UTF32.char32At(s, kk);
				final byte cat = Default.ucd().getCategory(ccc);
				if (cat == UCD_Types.Cf || cat == UCD_Types.Cc || cat == UCD_Types.Zs || cat == UCD_Types.Zl || cat == UCD_Types.Zp) {
					sortedCodes.add(ces + "\t" + Default.ucd().getCodeAndName(s));
					break;
				}
			}

			final int len = ces.length();

			int haveMixture = 0;
			for (int j = 0; j < len; ++j) {
				final int ce = ces.at(j);
				final int pri = CEList.getPrimary(ce);
				final int sec = CEList.getSecondary(ce);
				if (pri == 0) {
					secondariesZPsample[sec].add(secondariesZP[sec], s);
					secondariesZP[sec]++;
				} else {
					secondariesNZPsample[sec].add(secondariesNZP[sec], s);
					secondariesNZP[sec]++;
				}
				if (haveMixture == 3) {
					continue;
				}
				if (getCollator(CollatorType.ducet).isVariable(ce)) {
					haveMixture |= 1;
				} else {
					haveMixture |= 2;
				}
				if (haveMixture == 3) {
					mixedCEs.add(ces + "\t" + Default.ucd().getCodeAndName(s));
				}
			}
		}

		for (int i = 0; i < secondariesZP.length; ++i) {
			if (secondariesZP[i] != 0) {
				remapZP[i] = zpCount;
				zpCount++;
			}
			if (secondariesNZP[i] != 0) {
				remapNZP[i] = nzpCount;
				nzpCount++;
			}
		}

		diLog.println();
		diLog.println("# Proposed Remapping (see doc about Japanese characters)");
		diLog.println();

		int bothCount = 0;
		for (int i = 0; i < secondariesZP.length; ++i) {
			if ((secondariesZP[i] != 0) || (secondariesNZP[i] != 0)) {
				char sign = ' ';
				if (secondariesZP[i] != 0 && secondariesNZP[i] != 0) {
					sign = '*';
					bothCount++;
				}
				if (secondariesZP[i] != 0) {
					showSampleOverlap(diLog, false, sign + "ZP ", secondariesZPsample[i]); // i,
					// 0x20
					// +
					// nzpCount
					// +
					// remapZP[i],
				}
				if (secondariesNZP[i] != 0) {
					if (i == 0x20) {
						diLog.println("(omitting " + secondariesNZP[i] + " NZP with values 0020 -- values don't change)");
					} else {
						showSampleOverlap(diLog, true, sign + "NZP", secondariesNZPsample[i]); // i,
						// 0x20
						// +
						// remapNZP[i],
					}
				}
				diLog.println();
			}
		}
		diLog.println("ZP Count = " + zpCount + ", NZP Count = " + nzpCount + ", Collisions = " + bothCount);

		/*
		 * diLog.println(); diLog.println("OVERLAPS"); diLog.println();
		 * 
		 * for (int i = 0; i < secondariesZP.length; ++i) { if (secondariesZP[i]
		 * != 0 && secondariesNZP[i] != 0) { diLog.println("Overlap at " +
		 * Utility.hex(i) + ": " + secondariesZP[i] + " with zero primaries" +
		 * ", " + secondariesNZP[i] + " with non-zero primaries" );
		 * 
		 * showSampleOverlap(" ZP:  ", secondariesZPsample[i], ces);
		 * showSampleOverlap(" NZP: ", secondariesNZPsample[i], ces);
		 * diLog.println(); } }
		 */

		diLog.println();
		diLog.println("# BACKGROUND INFORMATION");
		diLog.println();
		diLog.println("# All characters with 'mixed' CEs: variable and non-variable");
		diLog.println("# Note: variables are in "
				+ Utility.hex(CEList.getPrimary(getCollator(CollatorType.ducet).getVariableLowCE())) + " to "
				+ Utility.hex(CEList.getPrimary(getCollator(CollatorType.ducet).getVariableHighCE())));
		diLog.println();

		Iterator<String> it;
		it = mixedCEs.iterator();
		while (it.hasNext()) {
			final String key = it.next();
			diLog.println(key);
		}

		diLog.println();
		diLog.println("# All 'controls': Cc, Cf, Zs, Zp, Zl");
		diLog.println();

		it = sortedCodes.iterator();
		while (it.hasNext()) {
			final Object key = it.next();
			diLog.println(key);
		}

		diLog.close();
	}

	private static void showSampleOverlap(PrintWriter diLog, boolean doNew, String head, Vector<String> v) {
		for (int i = 0; i < v.size(); ++i) {
			showSampleOverlap(diLog, doNew, head, v.get(i));
		}
	}

	private static void showSampleOverlap(PrintWriter diLog, boolean doNew, String head, String src) {
		final int[] ces = new int[30];
		final int len = getCollator(CollatorType.ducet).getCEs(src, true, ces);
		int[] newCes = null;
		int newLen = 0;
		if (doNew) {
			newCes = new int[30];
			for (int i = 0; i < len; ++i) {
				final int ce = ces[i];
				final int p = CEList.getPrimary(ce);
				final int s = CEList.getSecondary(ce);
				final int t = CEList.getTertiary(ce);
				if (p != 0 && s != 0x20) {
					newCes[newLen++] = UCA.makeKey(p, 0x20, t);
					newCes[newLen++] = UCA.makeKey(0, s, 0x1F);
				} else {
					newCes[newLen++] = ce;
				}
			}
		}
		diLog.println(
				UCD.getCode(src)
				+ "\t" + head
				// + "\t" + Utility.hex(oldWeight)
				// + " => " + Utility.hex(newWeight)
				+ "\t" + CEList.toString(ces, len)
				+ (doNew ? " => " + CEList.toString(newCes, newLen) : "")
				+ "\t( " + src + " )"
				+ "\t" + Default.ucd().getName(src)
				);
	}

	// Options for writeRules(byte options, ...), used by UCA.Main.
	static final byte    WITHOUT_NAMES                = 0, WITH_NAMES = 1, IN_XML = 2;

	private static final boolean SKIP_CANONICAL_DECOMPOSIBLES = true;

	private static int getFirstCELen(CEList ces) {
		final int len = ces.length();
		if (len < 2) {
			return len;
		}
		int expansionStart = 1;
		if (UCA.isImplicitLeadCE(ces.at(0))) {
			expansionStart = 2; // move up if first is double-ce
		}
		if (len > expansionStart && getCollator(CollatorType.ducet).getHomelessSecondaries().contains(CEList.getSecondary(ces.at(expansionStart)))) {
			if (log2 != null) {
				log2.println("Homeless: " + ces);
			}
			++expansionStart; // move up if *second* is homeless ignoreable
		}
		return expansionStart;
	}

	private static PrintWriter log2 = null;

	// Called by UCA.Main.
	static void writeRules(byte option, boolean shortPrint, boolean noCE, CollatorType collatorType2) throws IOException {
		System.out.println("Sorting");
		final Map<ArrayWrapper, String> backMap = new HashMap<ArrayWrapper, String>();
		final java.util.Comparator<String> cm = new RuleComparator();
		final Map<String, String> ordered = new TreeMap<String, String>(cm);

		final UCA.UCAContents cc = getCollator(collatorType2).getContents(SKIP_CANONICAL_DECOMPOSIBLES ? Default.nfd() : null);

		final Set<String> alreadyDone = new HashSet<String>();

		log2 = Utility.openPrintWriter(UCA.getUCA_GEN_DIR() + File.separator + "log", "UCARules-log.txt", Utility.UTF8_WINDOWS);

		while (true) {
			final String s = cc.next();
			if (s == null) {
				break;
			}
			final CEList ces = cc.getCEs();

			if (s.equals("\uD800")) {
				System.out.println("Check: " + ces);
			}

			final String safeString = s.replace("\u0000", "\\u0000");
			log2.println(safeString + "\t" + bidiBracket(ces.toString()) + "\t" + Default.ucd().getCodeAndName(s));

			addToBackMap(backMap, ces, s, false);

			int ce2 = 0;
			int ce3 = 0;
			final int logicalFirstLen = getFirstCELen(ces);
			if (logicalFirstLen > 1) {
				ce2 = ces.at(1);
				if (logicalFirstLen > 2) {
					ce3 = ces.at(2);
				}
			}

			final String key = String.valueOf(CEList.getPrimary(ces.at(0))) + String.valueOf(CEList.getPrimary(ce2)) + String.valueOf(CEList.getPrimary(ce3))
					+ String.valueOf(CEList.getSecondary(ces.at(0))) + String.valueOf(CEList.getSecondary(ce2)) + String.valueOf(CEList.getSecondary(ce3))
					+ String.valueOf(CEList.getTertiary(ces.at(0))) + String.valueOf(CEList.getTertiary(ce2)) + String.valueOf(CEList.getTertiary(ce3))
					+ getCollator(collatorType2).getSortKey(s, UCA_Types.NON_IGNORABLE) + '\u0000' + UCA.codePointOrder(s);

			// String.valueOf((char)(ces.at(0]>>>16)) +
			// String.valueOf((char)(ces.at(0] & 0xFFFF))
			// + String.valueOf((char)(ce2>>>16)) + String.valueOf((char)(ce2 &
			// 0xFFFF))

			if (s.equals("\u0660") || s.equals("\u2080")) {
				System.out.println(Default.ucd().getCodeAndName(s) + "\t" + Utility.hex(key));
			}

			ordered.put(key, s);
			alreadyDone.add(s);

			final String result = ordered.get(key);
			if (result == null) {
				System.out.println("BAD SORT: " + Utility.hex(key) + ", " + Utility.hex(s));
			}
		}

		System.out.println("Checking CJK");

		// Check for characters that are ARE explicitly mapped in the CJK ranges
		final UnicodeSet CJK = new UnicodeSet(0x2E80, 0x2EFF);
		CJK.add(0x2F00, 0x2EFF);
		CJK.add(0x2F00, 0x2FDF);
		CJK.add(0x3400, 0x9FFF);
		CJK.add(0xF900, 0xFAFF);
		CJK.add(0x20000, 0x2A6DF);
		CJK.add(0x2F800, 0x2FA1F);
		CJK.removeAll(new UnicodeSet("[:Cn:]")); // remove unassigned

		// make set with canonical decomposibles
		final UnicodeSet composites = new UnicodeSet();
		for (int i = 0; i < 0x10FFFF; ++i) {
			if (!Default.ucd().isAllocated(i)) {
				continue;
			}
			if (Default.nfd().isNormalized(i)) {
				continue;
			}
			composites.add(i);
		}
		final UnicodeSet CJKcomposites = new UnicodeSet(CJK).retainAll(composites);
		System.out.println("CJK composites " + CJKcomposites.toPattern(true));
		System.out.println("CJK NONcomposites " + new UnicodeSet(CJK).removeAll(composites).toPattern(true));

		final UnicodeSet mapped = new UnicodeSet();
		Iterator<String> it = alreadyDone.iterator();
		while (it.hasNext()) {
			final String member = it.next();
			mapped.add(member);
		}
		final UnicodeSet CJKmapped = new UnicodeSet(CJK).retainAll(mapped);
		System.out.println("Mapped CJK: " + CJKmapped.toPattern(true));
		System.out.println("UNMapped CJK: " + new UnicodeSet(CJK).removeAll(mapped).toPattern(true));
		System.out.println("Neither Mapped nor Composite CJK: "
				+ new UnicodeSet(CJK).removeAll(CJKcomposites).removeAll(CJKmapped).toPattern(true));

		/*
		 * 2E80..2EFF; CJK Radicals Supplement 2F00..2FDF; Kangxi Radicals
		 * 
		 * 3400..4DBF; CJK Unified Ideographs Extension A 4E00..9FFF; CJK
		 * Unified Ideographs F900..FAFF; CJK Compatibility Ideographs
		 * 
		 * 20000..2A6DF; CJK Unified Ideographs Extension B 2F800..2FA1F; CJK
		 * Compatibility Ideographs Supplement
		 */

		System.out.println("Adding Kanji");
		for (int i = 0; i < 0x10FFFF; ++i) {
			if (!Default.ucd().isAllocated(i)) {
				continue;
			}
			if (Default.nfkd().isNormalized(i)) {
				continue;
			}
			Utility.dot(i);
			final String decomp = Default.nfkd().normalize(i);
			int cp;
			for (int j = 0; j < decomp.length(); j += UTF16.getCharCount(cp)) {
				cp = UTF16.charAt(decomp, j);
				final String s = UTF16.valueOf(cp);
				if (alreadyDone.contains(s)) {
					continue;
				}

				alreadyDone.add(s);
				final CEList ces = getCollator(collatorType2).getCEList(s, true);

				log2.println(s + "\t" + ces
						+ "\t" + Default.ucd().getCodeAndName(s) + " from " + Default.ucd().getCodeAndName(i));

				addToBackMap(backMap, ces, s, false);
			}
		}

		System.out.println("Find Exact Equivalents");

		final Set<String> removals = new HashSet<String>();
		final Map<String,String> equivalentsMap = findExactEquivalents(backMap, ordered, collatorType2, removals);
		for (final String s : removals) {
			ordered.remove(s);
		}

		System.out.println("Writing");

		String filename = "UCA_Rules";
		if (collatorType2 == CollatorType.ducet) {
			filename += "_DUCET";
		}
		if (shortPrint) {
			filename += "_SHORT";
		}
		if (noCE) {
			filename += "_NoCE";
		}
		if (option == IN_XML) {
			filename += ".xml";
		} else {
			filename += ".txt";
		}

		final String directory = UCA.getUCA_GEN_DIR() + File.separator
				+ (collatorType2==CollatorType.cldr ? "CollationAuxiliary" : "Ducet");

		log = Utility.openPrintWriter(directory, filename, Utility.UTF8_WINDOWS);

		//        String[] commentText = {
		//                filename,
		//                "This file contains the UCA tables for the given version, but transformed into rule syntax.",
		//                "Generated:   " + getNormalDate(),
		//                "NOTE: Since UCA handles canonical equivalents, no composites are necessary",
		//                "(except in extensions).",
		//                "For syntax description, see: http://oss.software.ibm.com/icu/userguide/Collate_Intro.html"
		//        };

		if (option == IN_XML) {
			log.println("<collation>");
			log.println("<!--");
			WriteCollationData.writeVersionAndDate(log, filename, collatorType2==CollatorType.cldr);
			log.println("-->");
			log.println("<base uca='" + getCollator(collatorType2).getDataVersion() + "/" + getCollator(collatorType2).getUCDVersion() + "'/>");
			log.println("<rules>");
		} else {
			log.write('\uFEFF'); // BOM
			WriteCollationData.writeVersionAndDate(log, filename, collatorType2==CollatorType.cldr);
		}

		it = ordered.keySet().iterator();

		// String lastSortKey = collator.getSortKey("\u0000");;
		// 12161004
		int lastCE = 0;
		int ce = 0;
		int nextCE = 0;

		final CEList bogusCes = new CEList(new int[] {});
		boolean firstTime = true;

		boolean done = false;

		String chr = "";
		CEList ces = bogusCes;

		String nextChr = "";
		CEList nextCes = bogusCes;  // bogusCes signals that we need to skip!!

		String lastChr = "";
		CEList lastCes = CEList.EMPTY;
		int lastExpansionStart = 0;
		int expansionStart = 0;

		// for debugging ordering
		String lastSortKey = "";
		boolean showNext = false;

		for (int loopCounter = 0; !done; loopCounter++) {
			Utility.dot(loopCounter);

			lastCE = ce;
			lastChr = chr;
			lastExpansionStart = expansionStart;
			lastCes = ces;

			// copy the current from Next

			ce = nextCE;
			chr = nextChr;
			ces = nextCes;

			// We need to look ahead one, to be able to reset properly

			if (it.hasNext()) {
				final String nextSortKey = it.next();
				nextChr = ordered.get(nextSortKey);
				final int result = cm.compare(nextSortKey, lastSortKey);
				if (result < 0) {
					System.out.println();
					System.out.println("DANGER: Sort Key Unordered!");
					System.out.println((loopCounter - 1) + " " + Utility.hex(lastSortKey)
							+ ", " + Default.ucd().getCodeAndName(lastSortKey.charAt(lastSortKey.length() - 1)));
					System.out.println(loopCounter + " " + Utility.hex(nextSortKey)
							+ ", " + Default.ucd().getCodeAndName(nextSortKey.charAt(nextSortKey.length() - 1)));
				}
				if (nextChr == null) {
					Utility.fixDot();
					if (!showNext) {
						System.out.println();
						System.out.println((loopCounter - 1) + "   Last = " + Utility.hex(lastSortKey)
								+ ", " + Default.ucd().getCodeAndName(lastSortKey.charAt(lastSortKey.length() - 1)));
					}
					System.out.println(cm.compare(lastSortKey, nextSortKey)
							+ ", " + cm.compare(nextSortKey, lastSortKey));
					System.out.println(loopCounter + " NULL AT  " + Utility.hex(nextSortKey)
							+ ", " + Default.ucd().getCodeAndName(nextSortKey.charAt(nextSortKey.length() - 1)));
					nextChr = "??";
					showNext = true;
				} else if (showNext) {
					showNext = false;
					System.out.println(cm.compare(lastSortKey, nextSortKey)
							+ ", " + cm.compare(nextSortKey, lastSortKey));
					System.out.println(loopCounter + "   Next = " + Utility.hex(nextSortKey)
							+ ", " + Default.ucd().getCodeAndName(nextChr));
				}
				lastSortKey = nextSortKey;
			} else {
				nextChr = "??";
				done = true; // make one more pass!!!
			}

			nextCes = getCollator(collatorType2).getCEList(nextChr, true);
			nextCE = nextCes.isEmpty() ? 0 : nextCes.at(0);

			// skip first (fake) element

			if (ces == bogusCes) {
				continue;
			}

			// for debugging

			if (loopCounter < 5) {
				System.out.println(loopCounter);
				System.out.println(lastCes.toString() + ", " + Default.ucd().getCodeAndName(lastChr));
				System.out.println(ces.toString() + ", " + Default.ucd().getCodeAndName(chr));
				System.out.println(nextCes.toString() + ", " + Default.ucd().getCodeAndName(nextChr));
			}

			// get relation

			/*
			 * if (chr.charAt(0) == 0xFFFB) { System.out.println("DEBUG"); }
			 */

			if (chr.equals("\u0966")) {
				System.out.println(ces.toString());
			}

			expansionStart = getFirstCELen(ces);

			int relation = getStrengthDifference(ces, expansionStart, lastCes, lastExpansionStart);

			if (relation == QUARTERNARY_DIFF) {
				final int relation2 = getStrengthDifference(ces, ces.length(), lastCes, lastCes.length());
				if (relation2 != QUARTERNARY_DIFF) {
					relation = TERTIARY_DIFF;
				}
			}

			// RESETs: do special case for relations to fixed items

			String reset = "";
			String resetComment = "";
			boolean insertVariableTop = false;
			boolean resetToParameter = false;

			final int ceLayout = getCELayout(ce, getCollator(collatorType2));
			if (ceLayout == IMPLICIT) {
				if (relation == PRIMARY_DIFF) {
					final int primary = CEList.getPrimary(ce);
					final int resetCp = UCA.ImplicitToCodePoint(primary, CEList.getPrimary(ces.at(1)));

					final CEList ces2 = getCollator(collatorType2).getCEList(UTF16.valueOf(resetCp), true);
					relation = getStrengthDifference(ces, ces.length(), ces2, ces2.length());

					reset = quoteOperand(UTF16.valueOf(resetCp));
					if (!shortPrint) {
						resetComment = Default.ucd().getCodeAndName(resetCp);
					}
					// lastCE = UCA.makeKey(primary, UCA.NEUTRAL_SECONDARY,
					// UCA.NEUTRAL_TERTIARY);
				}
				// lastCJKPrimary = primary;
			} else if (ceLayout != getCELayout(lastCE, getCollator(collatorType2)) || firstTime) {
				resetToParameter = true;
				switch (ceLayout) {
				case T_IGNORE:
					reset = "last tertiary ignorable";
					break;
				case S_IGNORE:
					reset = "last secondary ignorable";
					break;
				case P_IGNORE:
					reset = "last primary ignorable";
					break;
				case VARIABLE:
					reset = "last regular";
					break;
				case NON_IGNORE: /* reset = "top"; */
					insertVariableTop = true;
					break;
				case TRAILING:
					reset = "last trailing";
					break;
				}
			}

			// There are double-CEs, so we have to know what the length of the
			// first bit is.

			// check expansions

			String expansion = "";
			if (ces.length() > expansionStart) {
				// int tert0 = ces.at(0] & 0xFF;
				// boolean isCompat = tert0 != 2 && tert0 != 8;
				log2.println("Exp: " + Default.ucd().getCodeAndName(chr) + ", " + ces + ", start: " + expansionStart);
				final int[] rel = { relation };
				expansion = getFromBackMap(backMap, ces, expansionStart, ces.length(), chr, rel);
				// relation = rel[0];

				// The relation needs to be fixed differently. Since it is an
				// expansion, it should be compared to
				// the first CE
				// ONLY reset if the sort keys are not equal
				if (false && (relation == PRIMARY_DIFF || relation == SECONDARY_DIFF)) {
					final int relation2 = getStrengthDifference(ces, expansionStart, lastCes, lastExpansionStart);
					if (relation2 != relation) {
						System.out.println();
						System.out.println("Resetting: " + RELATION_NAMES[relation] + " to " + RELATION_NAMES[relation2]);
						System.out.println("LCes: " + lastCes + ", " + lastExpansionStart
								+ ", " + Default.ucd().getCodeAndName(lastChr));
						System.out.println("Ces:  " + ces + ", " + expansionStart
								+ ", " + Default.ucd().getCodeAndName(chr));
						relation = relation2;
					}
				}

			}

			// print results
			// skip printing if it ends with a half-surrogate
			final char lastChar = chr.charAt(chr.length() - 1);
			if (Character.isHighSurrogate(lastChar)) {
				System.out.println("Skipping trailing surrogate: " + chr + "\t" + Utility.hex(chr));
			} else {
				if (option == IN_XML) {
					if (insertVariableTop) {
						log.println(XML_RELATION_NAMES[0] + "<variableTop/>");
					}

					/*
					 * log.print("  <!--" + ucd.getCodeAndName(chr)); if (len > 1)
					 * log.print(" / " + Utility.hex(expansion));
					 * log.println("-->");
					 */

					if (reset.length() != 0) {
						log.println("<reset/>"
								+ (resetToParameter ? "<position at=\"" + reset + "\"/>" : Utility.quoteXML(reset))
								+ (resetComment.length() != 0 ? "<!-- " + resetComment + "-->" : ""));
					}
					if (expansion.length() > 0) {
						log.print("<x>");
					}
					if (!firstTime) {
						log.print("  <" + XML_RELATION_NAMES[relation] + ">");
						log.print(Utility.quoteXML(chr));
						log.print("</" + XML_RELATION_NAMES[relation] + ">");
					}

					// <x><t>&#x20A8;</t><extend>s</extend></x> <!--U+20A8 RUPEE SIGN / 0073-->

					if (expansion.length() > 0) {
						log.print("<extend>" + Utility.quoteXML(expansion) + "</extend></x>");
					}
					if (!shortPrint) {
						log.print("\t<!--");
						if (!noCE) {
							log.print(ces.toString() + " ");
						}
						log.print(Default.ucd().getCodeAndName(chr));
						if (expansion.length() > 0) {
							log.print(" / " + Utility.hex(expansion));
						}
						log.print("-->");
					}
					log.println();
				} else {
					if (insertVariableTop) {
						log.println(RELATION_NAMES[0] + " [variable top]");
					}
					if (reset.length() != 0) {
						log.println("& "
								+ (resetToParameter ? "[" : "") + reset + (resetToParameter ? "]" : "")
								+ (resetComment.length() != 0 ? "\t\t# " + resetComment : ""));
					}
					if (!firstTime) {
						log.print(RELATION_NAMES[relation] + " " + quoteOperand(chr));
					}
					if (expansion.length() > 0) {
						log.print(" / " + quoteOperand(expansion));
					}
					if (!shortPrint) {
						log.print("\t# ");
						if (false) {
							if (latestAge(chr).startsWith("5.2")) {
								log.print("† ");
							}
						}

						log.print(latestAge(chr) + " [");
						final String typeKD = ReorderingTokens.getTypesCombined(chr);
						log.print(typeKD + "] ");

						if (!noCE) {
							log.print(ces.toString() + " ");
						}
						log.print(Default.ucd().getCodeAndName(chr));
						if (expansion.length() > 0) {
							log.print(" / " + Utility.hex(expansion));
						}
					}
					log.println();
				}
			}
			firstTime = false;
		}
		for (final Entry<String, String> sourceReplacement : equivalentsMap.entrySet()) {
			// note: we set the reset to the value we want, then have
			// = X for the item whose value is to be changed
			final String valueToSetTo = sourceReplacement.getValue();
			final String stringToSet = sourceReplacement.getKey();
			if (option == IN_XML) {
				log.print("<reset/>"
						+ Utility.quoteXML(valueToSetTo)
						+ "<i>"
						+ Utility.quoteXML(stringToSet)
						+ "</i>");
				if (!shortPrint) {
					log.print("\t<!--");
					log.print(Default.ucd().getCodeAndName(stringToSet)
							+ "\t→\t"
							+ Default.ucd().getCodeAndName(valueToSetTo));
					log.print("-->");
				}
			} else {
				log.print("& "
						+ quoteOperand(valueToSetTo)
						+ " = "
						+ quoteOperand(stringToSet));
				if (!shortPrint) {
					log.print("\t# ");
					log.print(latestAge(stringToSet) + " [");
					final String typeKD = ReorderingTokens.getTypesCombined(stringToSet);
					log.print(typeKD + "] ");
					log.print(Default.ucd().getCodeAndName(stringToSet)
							+ "\t→\t"
							+ Default.ucd().getCodeAndName(valueToSetTo));
				}
			}
			log.println();
		}
		// log.println("& [top]"); // RESET
		if (option == IN_XML) {
			log.println("</rules></collation>");
		}
		log2.close();
		log.close();
		Utility.fixDot();
	}

	private static final UnicodeSet SKIP_TIBETAN_EQUIVALENTS = new UnicodeSet("[ྲཱི  ྲཱི ྲཱུ  ྲཱུ ླཱི  ླཱི ླཱུ  ླཱུ]").freeze();
	private static Map<String, String> findExactEquivalents(
			Map<ArrayWrapper, String> backMap, Map<String, String> ordered,
			CollatorType collatorType2,
			Set<String> removals) {
		final Map<String, String> equivalentsStrings = new LinkedHashMap<String, String>();
		final IntStack nextCes = new IntStack(10);
		final int[] startBuffer = new int[100];
		final int[] endBuffer = new int[100];
		final ArrayWrapper start = new ArrayWrapper(startBuffer, 0, 0);
		final ArrayWrapper end = new ArrayWrapper(endBuffer, 0, 0);
		for (final Entry<String, String> entry : ordered.entrySet()) {
			final String sortKey = entry.getKey();
			final String string = entry.getValue();
			if (Character.codePointCount(string, 0, string.length()) < 2) {
				continue;
			} else if (SKIP_TIBETAN_EQUIVALENTS.containsSome(string)) {
				continue;
			}
			nextCes.clear();
			getCollator(collatorType2).getCEs(string, true, nextCes);
			final int len = nextCes.length();
			if (len < 2) {
				continue;
			}
			// just look for pairs
			for (int i = 1; i < len; ++i) {
				start.limit = nextCes.extractInto(0, i, startBuffer, 0);
				final String string1 = backMap.get(start);
				if (string1 == null) {
					continue;
				}
				end.limit = nextCes.extractInto(i, len, endBuffer, 0);
				final String string2 = backMap.get(end);
				if (string2 == null) {
					continue;
				}
				final String replacement = string1 + string2;
				if (string.equals(replacement)) {
					continue;
				}
				equivalentsStrings.put(string, replacement);
				removals.add(sortKey);
			}
		}
		return equivalentsStrings;
	}

	private static String bidiBracket(String string) {
		if (BIDI.containsSome(string)) {
			return LRM + string + LRM;
		}
		return string;
	}

	private static ToolUnicodePropertySource ups;

	private static ToolUnicodePropertySource getToolUnicodeSource() {
		if (ups == null) {
			ups = ToolUnicodePropertySource.make(Default.ucdVersion());
		}
		return ups;
	}

	private static final UnicodeProperty bidiProp = getToolUnicodeSource().getProperty("bc");
	private static final UnicodeSet      BIDI     = new UnicodeSet(bidiProp.getSet("AL")).addAll(bidiProp.getSet("R")).freeze();
	private static final String          LRM      = "\u200E";

	private static String latestAge(String chr) {
		int cp;
		String latestAge = "";
		for (int i = 0; i < chr.length(); i += Character.charCount(cp)) {
			final String age = getAge(cp = chr.codePointAt(i));
			if (latestAge.compareTo(age) < 0) {
				latestAge = age;
			}
		}
		// if (latestAge.endsWith(".0")) {
		// latestAge = latestAge.substring(0, latestAge.length() - 2);
		// }
		return latestAge;
	}

	private static UnicodeProperty ageProp;

	private static String getAge(int cp) {
		if (ageProp == null) {
			ageProp = getToolUnicodeSource().getProperty("age");
		}
		return ageProp.getValue(cp, true);
	}

	private static final int T_IGNORE = 1, S_IGNORE = 2, P_IGNORE = 3, VARIABLE = 4, NON_IGNORE = 5, IMPLICIT = 6, TRAILING = 7;

	private static int getCELayout(int ce, UCA collator) {
		final int primary = CEList.getPrimary(ce);
		final int secondary = CEList.getSecondary(ce);
		final int tertiary = CEList.getSecondary(ce);
		if (primary == 0) {
			if (secondary == 0) {
				if (tertiary == 0) {
					return T_IGNORE;
				}
				return S_IGNORE;
			}
			return P_IGNORE;
		}
		if (collator.isVariable(ce)) {
			return VARIABLE;
		}
		if (primary < UCA_Types.UNSUPPORTED_BASE) {
			return NON_IGNORE;
		}
		if (primary < UCA_Types.UNSUPPORTED_LIMIT) {
			return IMPLICIT;
		}
		return TRAILING;
	}

	private static final int
	PRIMARY_DIFF = 0,
	SECONDARY_DIFF = 1,
	TERTIARY_DIFF = 2,
	QUARTERNARY_DIFF = 3,
	DONE = -1;

	private static class CE_Iterator {
		CEList ces;
		int   len;
		int   current;
		int   level;

		void reset(CEList ces, int len) {
			this.ces = ces;
			this.len = len;
			current = 0;
			level = PRIMARY_DIFF;
		}

		void setLevel(int level) {
			current = 0;
			this.level = level;
		}

		int next() {
			int val = DONE;
			while (current < len) {
				final int ce = ces.at(current++);
				switch (level) {
				case PRIMARY_DIFF:
					val = CEList.getPrimary(ce);
					break;
				case SECONDARY_DIFF:
					val = CEList.getSecondary(ce);
					break;
				case TERTIARY_DIFF:
					val = CEList.getTertiary(ce);
					break;
				}
				if (val != 0) {
					return val;
				}
			}
			return DONE;
		}
	}

	private static CE_Iterator ceit1 = new CE_Iterator();
	private static CE_Iterator ceit2 = new CE_Iterator();

	// WARNING, Never Recursive!

	private static int getStrengthDifference(CEList ces, int len, CEList lastCes, int lastLen) {
		if (false && lastLen > 0 && lastCes.at(0) > 0) {
			System.out.println("DeBug");
		}
		ceit1.reset(ces, len);
		ceit2.reset(lastCes, lastLen);

		for (int level = PRIMARY_DIFF; level <= TERTIARY_DIFF; ++level) {
			ceit1.setLevel(level);
			ceit2.setLevel(level);
			while (true) {
				final int weight1 = ceit1.next();
				final int weight2 = ceit2.next();
				if (weight1 != weight2) {
					return level;
				}
				if (weight1 == DONE) {
					break;
				}
			}
		}
		return QUARTERNARY_DIFF;
	}

	private static final String[] RELATION_NAMES     = { " <\t", "  <<\t", "   <<<\t", "    =\t" };
	private static final String[] XML_RELATION_NAMES = { "p", "s", "t", "i" };

	private static class ArrayWrapper {
		int[] array;
		int   start;
		int   limit;

		/*
		 * public ArrayWrapper(int[] contents) { set(contents, 0,
		 * contents.length); }
		 */

		public ArrayWrapper(int[] contents, int start, int limit) {
			set(contents, start, limit);
		}

		private void set(int[] contents, int start, int limit) {
			array = contents;
			this.start = start;
			this.limit = limit;
		}

		@Override
		public boolean equals(Object other) {
			final ArrayWrapper that = (ArrayWrapper) other;
			if (that.limit - that.start != limit - start) {
				return false;
			}
			for (int i = start; i < limit; ++i) {
				if (array[i] != that.array[i - start + that.start]) {
					return false;
				}
			}
			return true;
		}

		@Override
		public int hashCode() {
			int result = limit - start;
			for (int i = start; i < limit; ++i) {
				result = result * 37 + array[i];
			}
			return result;
		}

		@Override
		public String toString() {
			final StringBuilder result = new StringBuilder();
			for (int i = start; i < limit; ++i) {
				if (result.length() != 0) {
					result.append(",");
				}
				result.append(Utility.hex(0xFFFFFFFFL & array[i]));
			}
			return result.toString();
		}
	}

	private static int testCase[] = {
		UCA.makeKey(0x0255, 0x0020, 0x000E),
	};

	private static String testString = "\u33C2\u002E";

	private static boolean contains(int[] array, int start, int limit, int key) {
		for (int i = start; i < limit; ++i) {
			if (array[i] == key) {
				return true;
			}
		}
		return false;
	}

	private static final void addToBackMap(Map<ArrayWrapper,String> backMap, CEList ces, String s, boolean show) {
		if (show || contains(testCase, 0, testCase.length, ces.at(0)) || testString.indexOf(s) > 0) {
			System.out.println("Test case: " + Utility.hex(s) + ", " + ces);
		}
		// NOTE: we add the back map based on the string value; the smallest
		// (UTF-16 order) string wins
		final int[] cesArray = new int[ces.length()];
		final ArrayWrapper key = new ArrayWrapper(cesArray, 0, ces.appendTo(cesArray, 0));
		if (false) {
			final String value = backMap.get(key);
			if (value == null) {
				return;
			}
			if (s.compareTo(value) >= 0) {
				return;
			}
		}
		backMap.put(key, s);
	}

	private static final String getFromBackMap(Map<ArrayWrapper, String> backMap, CEList originalces, int expansionStart, int len, String chr, int[] rel) {
		final int[] ces = new int[originalces.length()];
		originalces.appendTo(ces, 0);

		String expansion = "";

		// process ces to neutralize tertiary

		for (int i = expansionStart; i < len; ++i) {
			final int probe = ces[i];
			final char primary = CEList.getPrimary(probe);
			final char secondary = CEList.getSecondary(probe);
			final char tertiary = CEList.getTertiary(probe);

			int tert = tertiary;
			switch (tert) {
			case 8:
			case 9:
			case 0xA:
			case 0xB:
			case 0xC:
			case 0x1D:
				tert = 8;
				break;
			case 0xD:
			case 0x10:
			case 0x11:
			case 0x12:
			case 0x13:
			case 0x1C:
				tert = 0xE;
				break;
			default:
				tert = 2;
				break;
			}
			ces[i] = UCA.makeKey(primary, secondary, tert);
		}

		for (int i = expansionStart; i < len;) {
			int limit;
			String s = null;
			for (limit = len; limit > i; --limit) {
				final ArrayWrapper wrapper = new ArrayWrapper(ces, i, limit);
				s = backMap.get(wrapper);
				if (s != null) {
					break;
				}
			}
			if (s == null) {
				do {
					if (getCollator(CollatorType.ducet).getHomelessSecondaries().contains(CEList.getSecondary(ces[i]))) {
						s = "";
						if (rel[0] > 1) {
							rel[0] = 1; // HACK
						}
						break;
					}

					// Try stomping the value to different tertiaries

					final int probe = ces[i];
					if (UCA.isImplicitLeadCE(probe)) {
						s = UTF16.valueOf(UCA.ImplicitToCodePoint(CEList.getPrimary(probe), CEList.getPrimary(ces[i + 1])));
						++i; // skip over next item!!
						break;
					}

					final char primary = CEList.getPrimary(probe);
					final char secondary = CEList.getSecondary(probe);

					ces[i] = UCA.makeKey(primary, secondary, 2);
					ArrayWrapper wrapper = new ArrayWrapper(ces, i, i + 1);
					s = backMap.get(wrapper);
					if (s != null) {
						break;
					}

					ces[i] = UCA.makeKey(primary, secondary, 0xE);
					wrapper = new ArrayWrapper(ces, i, i + 1);
					s = backMap.get(wrapper);
					if (s != null) {
						break;
					}

					// we failed completely. Print error message, and bail

					System.out.println("Fix Homeless! No back map for " + CEList.toString(ces[i])
							+ " from " + CEList.toString(ces, len));
					System.out.println("\t" + Default.ucd().getCodeAndName(chr)
							+ " => " + Default.ucd().getCodeAndName(Default.nfkd().normalize(chr))
							);
					s = "[" + Utility.hex(ces[i]) + "]";
				} while (false); // exactly one time, just for breaking
				limit = i + 1;
			}
			expansion += s;
			i = limit;
		}
		return expansion;
	}

	private static StringBuffer   quoteOperandBuffer           = new StringBuffer();                            // faster

	private static UnicodeSet     needsQuoting                 = null;
	private static UnicodeSet     needsUnicodeForm             = null;

	static final String quoteOperand(String s) {
		if (needsQuoting == null) {
			final ToolUnicodePropertySource ups = getToolUnicodeSource();
			final UnicodeProperty cat = ups.getProperty("gc");
			final UnicodeSet cn = cat.getSet("Cn");
			/*
			 * c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <=
			 * '9' || (c >= 0xA0 && !UCharacterProperty.isRuleWhiteSpace(c))
			 */
			needsQuoting = new UnicodeSet("[[:whitespace:][:z:][:c:][:ascii:]-[a-zA-Z0-9]-[:cn:]]").addAll(cn); //
			// "[[:ascii:]-[a-zA-Z0-9]-[:c:]-[:z:]]"); //
			// [:whitespace:][:c:][:z:]
			// for (int i = 0; i <= 0x10FFFF; ++i) {
			// if (UCharacterProperty.isRuleWhiteSpace(i)) needsQuoting.add(i);
			// }
			// needsQuoting.remove();
			needsUnicodeForm = new UnicodeSet("[\\u000d\\u000a[:zl:][:zp:][:c:][:di:]-[:cn:]]").addAll(cn);
		}
		s = Default.nfc().normalize(s);
		quoteOperandBuffer.setLength(0);
		boolean noQuotes = true;
		boolean inQuote = false;
		int cp;
		for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
			cp = UTF16.charAt(s, i);
			if (!needsQuoting.contains(cp)) {
				if (inQuote) {
					quoteOperandBuffer.append('\'');
					inQuote = false;
				}
				quoteOperandBuffer.append(UTF16.valueOf(cp));
			} else {
				noQuotes = false;
				if (cp == '\'') {
					quoteOperandBuffer.append("''");
				} else {
					if (!inQuote) {
						quoteOperandBuffer.append('\'');
						inQuote = true;
					}
					if (!needsUnicodeForm.contains(cp)) {
						quoteOperandBuffer.append(UTF16.valueOf(cp)); // cp !=
						// 0x2028
					} else if (cp > 0xFFFF) {
						quoteOperandBuffer.append("\\U").append(Utility.hex(cp, 8));
					} else if (cp <= 0x20 || cp > 0x7E) {
						quoteOperandBuffer.append("\\u").append(Utility.hex(cp));
					} else {
						quoteOperandBuffer.append(UTF16.valueOf(cp));
					}
				}
			}
			/*
			 * switch (c) { case '<': case '>': case '#': case '=': case '&':
			 * case '/': quoteOperandBuffer.append('\'').append(c).append('\'');
			 * break; case '\'': quoteOperandBuffer.append("''"); break;
			 * default: if (0 <= c && c < 0x20 || 0x7F <= c && c < 0xA0) {
			 * quoteOperandBuffer.append("\\u").append(Utility.hex(c)); break; }
			 * quoteOperandBuffer.append(c); break; }
			 */
		}
		if (inQuote) {
			quoteOperandBuffer.append('\'');
		}

		if (noQuotes) {
			return bidiBracket(s); // faster
		}
		return bidiBracket(quoteOperandBuffer.toString());
	}

	// Do not print a full date+time, to reduce gratuitous file changes.
	private static DateFormat myDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	// was "yyyy-MM-dd','HH:mm:ss' GMT'" in UCA 6.2

	static String getNormalDate() {
		// return Default.getDate() + " [MD]";
		final String noDate = System.getProperty("NODATE");
		if (noDate != null) {
			return "(date omitted)";
		}
		final String date = myDateFormat.format(new Date());
		String author = System.getProperty("AUTHOR");
		if (author == null) {
			author = " [MS]";
		} else if (author.isEmpty()) {
			// empty value in -DAUTHOR= or -DAUTHOR means add no author
		} else {
			author = " [" + author + ']';
		}
		return date + author;
	}

	private static final boolean needsXMLQuote(String source, boolean quoteApos) {
		for (int i = 0; i < source.length(); ++i) {
			final char ch = source.charAt(i);
			if (ch < ' ' || ch == '<' || ch == '&' || ch == '>') {
				return true;
			}
			if (quoteApos & ch == '\'') {
				return true;
			}
			if (ch == '\"') {
				return true;
			}
			if (ch >= '\uD800' && ch <= '\uDFFF') {
				return true;
			}
			if (ch >= '\uFFFE') {
				return true;
			}
		}
		return false;
	}

	// TODO: Unused, remove?
	public static final String XMLString(int[] cps) {
		return XMLBaseString(cps, cps.length, true);
	}

	// TODO: Unused, remove?
	public static final String XMLString(int[] cps, int len) {
		return XMLBaseString(cps, len, true);
	}

	// TODO: Unused, remove?
	public static final String XMLString(String source) {
		return XMLBaseString(source, true);
	}

	// TODO: Unused, remove?
	public static final String HTMLString(int[] cps) {
		return XMLBaseString(cps, cps.length, false);
	}

	// TODO: Unused, remove?
	public static final String HTMLString(int[] cps, int len) {
		return XMLBaseString(cps, len, false);
	}

	// TODO: Unused, remove?
	public static final String HTMLString(String source) {
		return XMLBaseString(source, false);
	}

	// TODO: Unused, remove?
	public static final String XMLBaseString(int[] cps, int len, boolean quoteApos) {
		final StringBuffer temp = new StringBuffer();
		for (int i = 0; i < len; ++i) {
			temp.append((char) cps[i]);
		}
		return XMLBaseString(temp.toString(), quoteApos);
	}

	// TODO: Unused, remove?
	public static final String XMLBaseString(String source, boolean quoteApos) {
		if (!needsXMLQuote(source, quoteApos)) {
			return source;
		}
		final StringBuffer result = new StringBuffer();
		for (int i = 0; i < source.length(); ++i) {
			final char ch = source.charAt(i);
			if (ch < ' '
					|| ch >= '\u007F' && ch <= '\u009F'
					|| ch >= '\uD800' && ch <= '\uDFFF'
					|| ch >= '\uFFFE') {
				result.append('\uFFFD');
				/*
				 * result.append("#x"); result.append(cpName(ch));
				 * result.append(";");
				 */
			} else if (quoteApos && ch == '\'') {
				result.append("&apos;");
			} else if (ch == '\"') {
				result.append("&quot;");
			} else if (ch == '<') {
				result.append("&lt;");
			} else if (ch == '&') {
				result.append("&amp;");
			} else if (ch == '>') {
				result.append("&gt;");
			} else {
				result.append(ch);
			}
		}
		return result.toString();
	}

	static UCA getCollator(CollatorType type) {
		switch(type) {
		case cldr:
			if (cldrCollator == null) {
				//                if (Default.ucdVersion().compareTo("6.1") < 0) { // only reorder if less than v6.1
				//                    cldrCollator = buildCldrCollator(true);
				//                } else
				{
					cldrCollator = buildCldrCollator(false);

					cldrCollator.overrideCE("\uFFFE", 0x1, 0x20, 0x5);
					cldrCollator.overrideCE("\uFFFF", 0xFFFE, 0x20, 0x5);
				}
			}
			return cldrCollator;
		case ducet:
			if (ducetCollator == null) {
				ducetCollator = UCA.buildCollator(null);
			}
			return ducetCollator;
		case cldrWithoutFFFx:
			if (cldrWithoutFFFxCollator == null) {
				cldrWithoutFFFxCollator = buildCldrCollator(false);
			}
			return cldrWithoutFFFxCollator;
		default:
			throw new IllegalArgumentException();
		}
	}

	private static UCA buildCldrCollator(boolean addFFFx) {
		final PrintWriter fractionalLog = Utility.openPrintWriter(UCA.getUCA_GEN_DIR() + File.separator + "log", "FractionalRemap.txt", Utility.UTF8_WINDOWS);
		// hack to reorder elements
		final UCA oldCollator = getCollator(CollatorType.ducet);
		final CEList ceListForA = oldCollator.getCEList("a", true);
		final int firstForA = ceListForA.at(0);
		final int firstScriptPrimary = CEList.getPrimary(firstForA);
		final Remap primaryRemap = new Remap();
		final RoBitSet primarySet = oldCollator.getStatistics().getPrimarySet();
		// gather the data
		final UnicodeSet spaces = new UnicodeSet();
		final UnicodeSet punctuation = new UnicodeSet();
		final UnicodeSet generalSymbols = new UnicodeSet();
		final UnicodeSet currencySymbols = new UnicodeSet();
		final UnicodeSet numbers = new UnicodeSet();

		final int oldVariableHigh = CEList.getPrimary(oldCollator.getVariableHighCE());
		int firstDucetNonVariable = -1;

		for (int i = primarySet.nextSetBit(0); i >= 0; i = primarySet.nextSetBit(i+1)) {
			if (i == 0)
			{
				continue; // skip ignorables
			}
			if (i == UCA.TEST_PRIMARY) {
				i = i; // for debugging
			}
			if (firstDucetNonVariable < 0 && i > oldVariableHigh) {
				firstDucetNonVariable = i;
			}

			final CharSequence repChar = oldCollator.getRepresentativePrimary(i);
			CharSequence rep2 = filter(repChar);
			if (rep2 == null) {
				rep2 = repChar;
				fractionalLog.println("# Warning - No NFKD primary with:\t" + Utility.hex(i)
						+ "\t" + repChar
						+ "\t" + Default.ucd().getCodeAndName(repChar.toString()));
				//continue;
			}
			rep2 = repChar;
			final int firstChar = Character.codePointAt(rep2, 0);
			final int cat = Default.ucd().getCategory(firstChar);
			switch (cat) {
			case UCD_Types.SPACE_SEPARATOR: case UCD_Types.LINE_SEPARATOR: case UCD_Types.PARAGRAPH_SEPARATOR: case UCD_Types.CONTROL:
				spaces.add(i);
				break;
			case UCD_Types.DASH_PUNCTUATION: case UCD_Types.START_PUNCTUATION: case UCD_Types.END_PUNCTUATION: case UCD_Types.CONNECTOR_PUNCTUATION:
			case UCD_Types.OTHER_PUNCTUATION: case UCD_Types.INITIAL_PUNCTUATION: case UCD_Types.FINAL_PUNCTUATION:
				punctuation.add(i);
				break;
			case UCD_Types.DECIMAL_DIGIT_NUMBER:
				numbers.add(i);
				break;
			case UCD_Types.LETTER_NUMBER: case UCD_Types.OTHER_NUMBER:
				if (i >= firstScriptPrimary) {
					break;
				}
				numbers.add(i);
				break;
			case UCD_Types.CURRENCY_SYMBOL:
				currencySymbols.add(i);
				break;
			case UCD_Types.MATH_SYMBOL: case UCD_Types.MODIFIER_SYMBOL: case UCD_Types.OTHER_SYMBOL:
				generalSymbols.add(i);
				break;
			case UCD_Types.UNASSIGNED: case UCD_Types.UPPERCASE_LETTER: case UCD_Types.LOWERCASE_LETTER: case UCD_Types.TITLECASE_LETTER: case UCD_Types.MODIFIER_LETTER:
			case UCD_Types.OTHER_LETTER: case UCD_Types.NON_SPACING_MARK: case UCD_Types.ENCLOSING_MARK: case UCD_Types.COMBINING_SPACING_MARK:
			case UCD_Types.FORMAT:
				if (i >= firstScriptPrimary) {
					break;
				}
				generalSymbols.add(i);
				break;
			default:
				throw new IllegalArgumentException();
			}
		}
		// now reorder
		primaryRemap
		.addItems(spaces)
		.addItems(punctuation)
		.setVariableHigh()
		.addItems(generalSymbols)
		.addItems(currencySymbols)
		.putRemappedCharacters(0x20A8) // U+20A8 RUPEE SIGN
		.putRemappedCharacters(0xFDFC) // U+FDFC RIAL SIGN
		.addItems(numbers);

		primaryRemap.setFirstDucetNonVariable(firstDucetNonVariable);

		final LinkedHashSet<String> s = new LinkedHashSet<String>();

		fractionalLog.println("# Remapped primaries");

		for (int i = primarySet.nextSetBit(0); i >= 0; i = primarySet.nextSetBit(i+1)) {
			if (i == UCA.TEST_PRIMARY) {
				i = i; // for debugging
			}
			final CharSequence repChar = oldCollator.getRepresentativePrimary(i);
			final CharSequence rep2 = repChar;
			//                        filter(repChar);
			//                    if (rep2 == null) {
			//                        rep2 = repChar;
			//                    }
			final String gcInfo = FractionalUCA.getStringTransform(rep2, "/", FractionalUCA.GeneralCategoryTransform, s);
			// FractionalUCA.GeneralCategoryTransform.transform(repChar);
			//String scriptInfo = FractionalUCA.ScriptTransform.transform(repChar);

			final Integer remap = primaryRemap.getRemappedPrimary(i);
			if (remap == null) {
				if (!SHOW_NON_MAPPED) {
					continue;
				}
			}
			final int remap2 = remap == null ? i : remap;
			fractionalLog.println(
					(remap == null ? "#" : "")
					+ "\t" + i
					+ "\t" + remap2
					+ "\tx" + Utility.hex(i)
					+ "\tx" + Utility.hex(remap2)
					+ "\t" + gcInfo
					+ "\t" + excelQuote(rep2)
					+ "\t" + Default.ucd().getCodeAndName(Character.codePointAt(rep2, 0))
					);
		}
		final Map<Integer, IntStack> characterRemap = primaryRemap.getCharacterRemap();
		fractionalLog.println("# Remapped characters");

		for (final Entry<Integer, IntStack> x : characterRemap.entrySet()) {
			final Integer character = x.getKey();
			fractionalLog.println("#" + Utility.hex(character)
					+ "\t" + x.getValue()
					+ "\t" + Default.ucd().getCodeAndName(character));
		}
		fractionalLog.close();

		final UCA result = UCA.buildCollator(primaryRemap);

		if (addFFFx) {
			result.overrideCE("\uFFFE", 0x1, 0x20, 0x5);
			result.overrideCE("\uFFFF", 0xFFFE, 0x20, 0x5);
		}

		if (ADD_TIBETAN) {
			final CEList fb2 = result.getCEList("\u0FB2", true);
			final CEList fb3 = result.getCEList("\u0FB3", true);
			final CEList f71_f72 = result.getCEList("\u0F71\u0F72", true);
			final CEList f71_f74 = result.getCEList("\u0F71\u0F74", true);
			final CEList fb2_f71 = result.getCEList("\u0FB2\u0F71", true);
			final CEList fb3_f71 = result.getCEList("\u0FB3\u0F71", true);

			addOverride(result, "\u0FB2\u0F71", fb2_f71);               //0FB2 0F71      ;     [.255A.0020.0002.0FB2][.2570.0020.0002.0F71] - concat 0FB2 + 0F71
			addOverride(result, "\u0FB2\u0F71\u0F72", fb2, f71_f72);    //0FB2 0F71 0F72 ;    [.255A.0020.0002.0FB2][.2572.0020.0002.0F73] - concat 0FB2 + (0F71/0F72)
			addOverride(result, "\u0FB2\u0F73", fb2, f71_f72);          //0FB2 0F73      ;        [.255A.0020.0002.0FB2][.2572.0020.0002.0F73] = prev
			addOverride(result, "\u0FB2\u0F71\u0F74", fb2, f71_f74);    //0FB2 0F71 0F74 ;    [.255A.0020.0002.0FB2][.2576.0020.0002.0F75] - concat 0FB2 + (0F71/0F74)
			addOverride(result, "\u0FB2\u0F75", fb2, f71_f74);          //0FB2 0F75      ;        [.255A.0020.0002.0FB2][.2576.0020.0002.0F75]  = prev

			// same as above, but 0FB2 => 0FB3 and fb2 => fb3

			addOverride(result, "\u0FB3\u0F71", fb3_f71);               //0FB3 0F71      ;     [.255A.0020.0002.0FB3][.2570.0020.0002.0F71] - concat 0FB3 + 0F71
			addOverride(result, "\u0FB3\u0F71\u0F72", fb3, f71_f72);    //0FB3 0F71 0F72 ;    [.255A.0020.0002.0FB3][.2572.0020.0002.0F73] - concat 0FB3 + (0F71/0F72)
			addOverride(result, "\u0FB3\u0F73", fb3, f71_f72);          //0FB3 0F73      ;        [.255A.0020.0002.0FB3][.2572.0020.0002.0F73] = prev
			addOverride(result, "\u0FB3\u0F71\u0F74", fb3, f71_f74);    //0FB3 0F71 0F74 ;    [.255A.0020.0002.0FB3][.2576.0020.0002.0F75] - concat 0FB3 + (0F71/0F74)
			addOverride(result, "\u0FB3\u0F75", fb3, f71_f74);          //0FB3 0F75      ;        [.255A.0020.0002.0FB3][.2576.0020.0002.0F75]  = prev
		}

		// verify results
		final int[] output = new int[30];
		final StringBuilder failures = new StringBuilder();
		for (int i = 0; i <= 0x10FFFF; ++i) {
			if (!result.codePointHasExplicitMappings(i)) {
				continue;
			}
			if (!Default.ucd().isAllocated(i)) {
				continue;
			}
			final int ceCount = result.getCEs(UTF16.valueOf(i), true, output);
			final int primary = ceCount < 1 ? 0 : CEList.getPrimary(output[0]);
			final int cat = Default.ucd().getCategory(i);

			switch (cat) {
			case UCD_Types.SPACE_SEPARATOR: case UCD_Types.LINE_SEPARATOR: case UCD_Types.PARAGRAPH_SEPARATOR: case UCD_Types.CONTROL:
			case UCD_Types.DASH_PUNCTUATION: case UCD_Types.START_PUNCTUATION: case UCD_Types.END_PUNCTUATION: case UCD_Types.CONNECTOR_PUNCTUATION:
			case UCD_Types.OTHER_PUNCTUATION: case UCD_Types.INITIAL_PUNCTUATION: case UCD_Types.FINAL_PUNCTUATION:
			case UCD_Types.DECIMAL_DIGIT_NUMBER: // case LETTER_NUMBER: case OTHER_NUMBER:
			case UCD_Types.CURRENCY_SYMBOL:
			case UCD_Types.MATH_SYMBOL: case UCD_Types.MODIFIER_SYMBOL:
				//case OTHER_SYMBOL:
				if (primary > firstScriptPrimary) {
					failures.append("\t" + Utility.hex(primary)
							+ "\t" + Default.ucd().getCategoryID(i)
							+ "\t" + Default.ucd().getCodeAndName(i)
							+ "\n"
							);
				}
				break;
			default:
				// no action
			}
		}
		if (failures.length() > 0) {
			throw new IllegalArgumentException("Failures:\n" + failures);
		}
		return result;
	}

	private static void addOverride(UCA result, String string, CEList... ceLists) {
		final IntStack tempStack = new IntStack(10);
		for (final CEList ceList : ceLists) {
			for (int i = 0; i < ceList.length(); ++i) {
				final int ce = ceList.at(i);
				tempStack.append(ce);
			}
		}
		result.overrideCE(string, tempStack);
	}

	private static CharSequence filter(CharSequence repChar) {
		if (Default.nfkd().isNormalized(repChar.toString())) {
			return repChar;
		}
		final StringBuilder result = new StringBuilder();
		int cp;
		for (int i = 0; i < repChar.length(); i += Character.charCount(cp)) {
			cp = Character.codePointAt(repChar, i);
			if (Default.nfkd().isNormalized(cp)) {
				result.appendCodePoint(cp);
			}
		}
		if (result.length() == 0) {
			return null;
		}
		return result.toString();
	}

	private static Pattern EXCEL_QUOTE = Pattern.compile("[\"\\p{Cntrl}\u0085\u2029\u2028]");

	private static String excelQuote(CharSequence input) {
		return EXCEL_QUOTE.matcher(input).replaceAll("\uFFFD");
	}
}
