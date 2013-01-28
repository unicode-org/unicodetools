package org.unicode.text.UCA;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.unicode.text.UCA.UCA.AppendToCe;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.UCD;
import org.unicode.text.utility.Utility;

import com.ibm.icu.text.CanonicalIterator;
import com.ibm.icu.text.UTF16;

/**
 * Prepares the UCA mappings for generation of FractionalUCA.txt.
 *
 * @since 2013-jan-02 (mostly pulled out of {@link FractionalUCA})
 */
public final class MappingsForFractionalUCA {
	private static boolean DEBUG = false;
	private final UCA uca;

	/**
	 * UCA collation mapping data.
	 * Comparison is by sort key first, then by the string.
	 * CEs do not contain completely ignorable CEs.
	 */
	/* package */ static class MappingWithSortKey implements Comparable<MappingWithSortKey> {
		/**
		 * Optional prefix (context) string. null if none.
		 */
		private final String prefix;
		private final String s;
		/**
		 * Only non-zero collation elements, enforced by the constructors.
		 */
		private final CEList ces;
		/**
		 * Modified CEs, if any.
		 * If not null, then these are the CEs to be transformed into fractional CEs.
		 */
		private CEList modifiedCEs;
		/**
		 * Standard 3-level UCA sort key "string" corresponding to ces.
		 */
		private final String sortKey;

		private MappingWithSortKey(UCA uca, String s) {
			this(uca, null, s, uca.getCEList(s, true));
		}

		private MappingWithSortKey(UCA uca, String s, CEList ces) {
			this(uca, null, s, ces);
		}

		private MappingWithSortKey(UCA uca, String prefix, String s, CEList ces) {
			this(prefix, s, ces, uca.getSortKey(ces, UCA_Types.NON_IGNORABLE, AppendToCe.none));
		}

		private MappingWithSortKey(String prefix, String s, CEList ces, String sortKey) {
			this.prefix = prefix;
			this.s = s;
			this.ces = ces.onlyNonZero();
			this.sortKey = sortKey;
		}

		public boolean hasPrefix() {
			return prefix != null;
		}

		/**
		 * Returns the optional prefix (context) string. null if none.
		 */
		public String getPrefix() {
			return prefix;
		}

		public String getString() {
			return s;
		}

		public CEList getOriginalCEs() {
			return ces;
		}

		/**
		 * Returns the modified CEs, if set, or else the original CEs.
		 * @see #setModifiedCEs(CEList)
		 */
		public CEList getCEs() {
			if (modifiedCEs != null) {
				return modifiedCEs;
			}
			return ces;
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			if (prefix != null) {
				sb.append(Utility.hex(prefix)).append(" | ");
			}
			sb.append(Utility.hex(s)).append(" -> ").append(ces).toString();
			if (modifiedCEs != null) {
				sb.append(" modified: ").append(modifiedCEs);
			}
			return sb.toString();
		}

		@Override
		public int compareTo(MappingWithSortKey other) {
			int diff = sortKey.compareTo(other.sortKey);
			if (diff == 0) {
				// Simple tie-breaker.
				// Should we instead compare the NFD versions of the strings in code point order?
				// That would be consistent with the UCA identical level.
				// We could compute the normalized strings on demand and cache them.
				diff = s.compareTo(other.s);
				if (diff == 0) {
					diff = comparePrefixes(prefix, other.prefix);
				}
			}
			return diff;
		}

		private static int comparePrefixes(String p1, String p2) {
			if (p1 == null) {
				return p2 == null ? 0 : -1;
			} else if (p2 == null) {
				return 1;
			} else {
				return p1.compareTo(p2);
			}
		}
	}

	/* package */ MappingsForFractionalUCA(UCA uca) {
		this.uca = uca;
	}

	/**
	 * Returns the mappings for FractionalUCA.txt.
	 * The mappings are sorted by UCA sort keys, canonically closed,
	 * and modified for improved collation performance.
	 */
	/* package */ SortedSet<MappingWithSortKey> getMappings() {
		final SortedSet<MappingWithSortKey> ordered = getSortedUCAMappings();
		modifyMappings(ordered);
		return ordered;
	}

	/**
	 * Returns a set of UCA mappings, sorted by their nearly-UCA-type sort key strings.
	 *
	 * <p>This method also adds canonical equivalents (canonical closure),
	 * if any are missing.
	 */
	private SortedSet<MappingWithSortKey> getSortedUCAMappings() {
		final String highCompat = UTF16.valueOf(0x2F805);

		System.out.println("Sorting");
		final SortedSet<MappingWithSortKey> ordered = new TreeSet<MappingWithSortKey>();
		final Set<String> contentsForCanonicalIteration = new TreeSet<String>();
		final UCA.UCAContents ucac = uca.getContents(null);
		int ccounter = 0;
		while (true) {
			Utility.dot(ccounter++);
			final String s = ucac.next();
			if (s == null) {
				break;
			}
			if (s.equals("\uFFFF") || s.equals("\uFFFE")) {
				continue; // Suppress the FFFF and FFFE, since we are adding them artificially later.
			}
			if (s.equals("\uFA36") || s.equals("\uF900") || s.equals("\u2ADC") || s.equals(highCompat)) {
				System.out.println(" * " + Default.ucd().getCodeAndName(s));
			}
			contentsForCanonicalIteration.add(s);
			ordered.add(new MappingWithSortKey(uca, s, ucac.getCEs()));
		}

		// Add canonically equivalent characters!!
		System.out.println("Start Adding canonical Equivalents2");
		int canCount = 0;

		System.out.println("Add missing decomposibles and non-characters");
		for (int i = 0; i <= 0x10FFFF; ++i) {
			if (Default.ucd().isNoncharacter(i)) {
				continue;
			}
			if (!Default.ucd().isAllocated(i)) {
				continue;
			}
			if (Default.nfd().isNormalized(i)) {
				continue;
			}
			if (UCD.isHangulSyllable(i)) {
				continue;
			}
			final String s = UTF16.valueOf(i);
			if (!contentsForCanonicalIteration.contains(s)) {
				contentsForCanonicalIteration.add(s);
				ordered.add(new MappingWithSortKey(uca, s));
				if (DEBUG) {
					System.out.println(" + " + Default.ucd().getCodeAndName(s));
				}
				canCount++;
			}
		}

		final Set<String> additionalSet = new HashSet<String>();
		System.out.println("Loading canonical iterator");
		final CanonicalIterator canIt = new CanonicalIterator(".");
		final Iterator<String> it2 = contentsForCanonicalIteration.iterator();
		System.out.println("Adding any FCD equivalents that have different sort keys");
		while (it2.hasNext()) {
			final String key = it2.next();
			if (key == null) {
				System.out.println("Null Key");
				continue;
			}
			canIt.setSource(key);

			boolean first = true;
			while (true) {
				final String s = canIt.next();
				if (s == null) {
					break;
				}
				if (s.equals(key)) {
					continue;
				}
				if (contentsForCanonicalIteration.contains(s)) {
					continue;
				}
				if (additionalSet.contains(s)) {
					continue;
				}


				// Skip anything that is not FCD.
				if (!Default.nfd().isFCD(s)) {
					continue;
				}

				// We ONLY add if the sort key would be different
				// Than what we would get if we didn't decompose!!
				final CEList ces = uca.getCEList(s, true);
				final String sortKey = uca.getSortKey(ces, UCA_Types.NON_IGNORABLE, AppendToCe.none);
				final String nonDecompSortKey = uca.getSortKey(s, UCA_Types.NON_IGNORABLE, false, AppendToCe.none);
				if (sortKey.equals(nonDecompSortKey)) {
					continue;
				}

				if (first) {
					System.out.println(" " + Default.ucd().getCodeAndName(key));
					first = false;
				}
				System.out.println(" => " + Default.ucd().getCodeAndName(s));
				System.out.println("    old: " + UCA.toString(nonDecompSortKey));
				System.out.println("    new: " + UCA.toString(sortKey));
				canCount++;
				additionalSet.add(s);
				ordered.add(new MappingWithSortKey(null, s, ces, sortKey));
			}
		}
		System.out.println("Done Adding canonical Equivalents -- added " + canCount);
		/*

            for (int ch = 0; ch < 0x10FFFF; ++ch) {
                Utility.dot(ch);
                byte type = collator.getCEType(ch);
                if (type >= UCA.FIXED_CE && !nfd.hasDecomposition(ch))
                    continue;
                }
                String s = org.unicode.text.UTF16.valueOf(ch);
                ordered.put(collator.getSortKey(s, UCA.NON_IGNORABLE) + '\u0000' + s, s);
            }

            Hashtable multiTable = collator.getContracting();
            Enumeration enum = multiTable.keys();
            int ecount = 0;
            while (enum.hasMoreElements()) {
                Utility.dot(ecount++);
                String s = (String)enum.nextElement();
                ordered.put(collator.getSortKey(s, UCA.NON_IGNORABLE) + '\u0000' + s, s);
            }
		 */
		// JUST FOR TESTING
		final boolean TESTING = false;
		if (TESTING) {
			final String sample = "\u3400\u3401\u4DB4\u4DB5\u4E00\u4E01\u9FA4\u9FA5\uAC00\uAC01\uD7A2\uD7A3";
			for (int i = 0; i < sample.length(); ++i) {
				final String s = sample.substring(i, i+1);
				ordered.add(new MappingWithSortKey(uca, s));
			}
		}

		return ordered;
	}

	/**
	 * Modifies some of the UCA mappings before they are converted to fractional CEs.
	 * <ul>
	 * <li>Turns L+middle dot contractions into prefix rules.
	 * <li>Merges artificial secondary CEs into the preceding primary ones.
	 *     DUCET primary CEs only use the "common" secondary weight.
	 *     All secondary distinctions are made via additional secondary CEs.
	 *     In FractionalUCA we change that, to reduce the number of expansions.
	 * </ul>
	 */
	private void modifyMappings(SortedSet<MappingWithSortKey> ordered) {
		System.out.println("Modify UCA Mappings");

		// Find the highest secondary weight assigned to a character.
		// Look at the first CE of each mapping, and look at only primary ignorable CEs.
		// Higher secondary weights are DUCET-specific, for secondary distinctions
		// of primary CEs.
		int maxNormalSecondary = 0;
		int numSecondariesMerged = 0;
		// Avoid merging the precomposed L+middle dot too. Look for its CEs.
		int lMiddleDotPri;
		int lMiddleDotSec;
		{
			final CEList lMiddleDotCEs = uca.getCEList("\u0140", true);
			if (lMiddleDotCEs.length() != 2) {
				throw new IllegalArgumentException(
						"L+middle dot has unexpected CEs: " + lMiddleDotCEs);
			}
			lMiddleDotPri = CEList.getPrimary(lMiddleDotCEs.at(0));
			lMiddleDotSec = CEList.getSecondary(lMiddleDotCEs.at(1));
		}

		final List<MappingWithSortKey> newMappings = new LinkedList<MappingWithSortKey>();
		final Iterator<MappingWithSortKey> it = ordered.iterator();
		while (it.hasNext()) {
			MappingWithSortKey mapping = it.next();
			CEList ces = mapping.ces;
			if (ces.isEmpty()) {
				continue;
			}

			// Look for L+middle dot first so that the middle dot's secondary weight
			// does not get merged into the L's primary CE.
			// (That would prevent it from turning into a prefix mapping.)
			String s = mapping.s;
			if (s.length() == 2 && ces.length() == 2 && mapping.prefix == null
					&& (s.equals("l\u00B7") || s.equals("L\u00B7")
							|| s.equals("l\u0387") || s.equals("L\u0387"))) {
				it.remove();
				// Move the l/L to the prefix.
				final String prefix = s.substring(0, 1);
				s = s.substring(1);
				// Retain only the prefix-conditional CE for the middle dot.
				ces = ces.sub(1, 2);
				// Make a new mapping for the middle dot.
				mapping = new MappingWithSortKey(uca, prefix, s, ces);
				newMappings.add(mapping);
				continue;
			}

			// Check and merge secondary CEs.
			final int firstCE = ces.at(0);
			if (CEList.getPrimary(firstCE) == 0) {
				final int sec = CEList.getSecondary(firstCE);
				if (sec > maxNormalSecondary) {
					maxNormalSecondary = sec;
				}
				// Check that no primary CE follows because
				// we may not have seen all of the ignorable mappings yet
				// and may therefore not have an accurate maxNormalSecondary yet.
				for (int i = 1; i < ces.length(); ++i) {
					if (CEList.getPrimary(ces.at(i)) != 0) {
						throw new IllegalArgumentException(
								"UCA Mapping " + mapping +
								"contains a primary CE after the initial ignorable CE");
					}
				}
			} else {
				for (int i = 0; i < ces.length(); ++i) {
					final int ce = ces.at(i);
					int sec = CEList.getSecondary(ce);
					if (CEList.getPrimary(ce) != 0) {
						if (sec != UCA_Types.NEUTRAL_SECONDARY && sec != 0) {
							throw new IllegalArgumentException(
									"UCA Mapping " + mapping +
									"contains a primary CE with a non-common secondary weight");
						}
					} else if (sec > maxNormalSecondary) {
						if (ces.length() == 2 && sec == lMiddleDotSec &&
								CEList.getPrimary(firstCE) == lMiddleDotPri) {
							break;
						}
						if ((i + 1) < ces.length()) {
							final int nextCE = ces.at(i + 1);
							final int nextPri = CEList.getPrimary(nextCE);
							final int nextSec = CEList.getSecondary(nextCE);
							if (nextPri == 0 && nextSec > maxNormalSecondary) {
								throw new IllegalArgumentException(
										"UCA Mapping " + mapping +
										"contains two artificial secondary CEs in a row");
							}
						}
						// Check that the previous CE is a primary CE.
						int previous = i - 1;
						int previousCE = ces.at(previous);
						int previousPri = CEList.getPrimary(previousCE);
						if (previousPri == 0) {
							continue;
						}
						if (CEList.getSecondary(previousCE) == 0) {
							// Index i is after a continuation,
							// should be for a two-CE implicit primary.
							previousCE = ces.at(--previous);
							previousPri = CEList.getPrimary(previousCE);
							if (previousPri == 0 || CEList.getSecondary(previousCE) == 0) {
								// Something unexpected.
								continue;
							}
						}
						// Copy the CEs before the previous primary CE.
						final int[] newCEs = new int[ces.length() - 1];
						for (int j = 0; j < previous; ++j) {
							newCEs[j] = ces.at(j);
						}
						// Merge ces[i]'s secondary weight into the previous primary CE.
						// Reduce the secondary weight to just after the common weight.
						sec = UCA_Types.NEUTRAL_SECONDARY + sec - maxNormalSecondary;
						// TODO: This is broken!
						// Map secondaries of primary CEs vs. ignorable CEs to separate ranges of fractional secondaries.
						final int previousTer = CEList.getTertiary(previousCE);
						newCEs[previous] = UCA.makeKey(previousPri, sec, previousTer);
						while (++previous < i) {
							// Copy the remainder of the continuation CE.
							newCEs[previous] = ces.at(previous);
						}
						// Copy the CEs after ces[i], shifting left by one to remove ces[i].
						for (int j = i + 1; j < ces.length(); ++j) {
							newCEs[j - 1] = ces.at(j);
						}
						// Store the modified CEs and continue with looking at the next CE.
						// We do not replace the whole mapping because the modified CEs
						// are not well-formed (secondary weights of primary vs. ignorable CEs overlap now)
						// and therefore we should not use them to create a sort key.
						mapping.modifiedCEs = ces = new CEList(newCEs);
						--i;
						++numSecondariesMerged;
					}
				}
			}
		}
		ordered.addAll(newMappings);
		System.out.println(
				"Number of artificial secondary CEs merged into the preceding primary CEs: " +
						numSecondariesMerged);
	}
}
