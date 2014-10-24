package org.unicode.draft;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.unicode.cldr.util.Counter;
import org.unicode.jsp.FileUtilities;
import org.unicode.jsp.FileUtilities.SemiFileReader;
import org.unicode.text.utility.Settings;


public class CharacterFrequency {
    private static final String DATA_DIR = Settings.OTHER_WORKSPACE_DIRECTORY +
    		"DATA/frequency/languages/";
    private static final String DATA_DIR_RANK = Settings.OTHER_WORKSPACE_DIRECTORY +
    		"DATA/frequency/languages-rank/";
    public static final boolean DEBUG = false;
    //	static final int MAX_LINE_COUNT = Integer.MAX_VALUE; // 10000;
    //	static final int MAX_SEQUENCE_CHARS = 15;
    //	static final int MIN_COUNT = 0;
    //	static final boolean CHARS_ONLY = false;
    //	private static Map<String,Double> languageToPopulation = new HashMap<String,Double>();
    //	private static Map<String,String> languagesFound = new TreeMap<String,String>();
    ////	private static Map<String,String> languageNameToTag = new HashMap<String,String>();
    //	private static Map<String, Counter<String>> languageToCharsCounter = new TreeMap<String, Counter<String>>();
    private static Map<String, Counter<Integer>> languageToCodePointCounter = new TreeMap<String, Counter<Integer>>();


    //	public static SupplementalDataInfo supplementalInfo = SupplementalDataInfo.getInstance(CldrUtility.SUPPLEMENTAL_DIRECTORY);
    //
    //	static {
    //		languageToPopulation.put("eo", 100000d);
    //
    //		Transform<String,ULocale> transform = new Transform<String,ULocale>() {
    //			public ULocale transform(String source) {
    //				return new ULocale(source);
    //			}
    //		};
    //
    ////		for (ULocale locale : new Iterables<ULocale>()
    ////				.and(transform, ULocale.getISOLanguages())
    ////				.and(ULocale.getAvailableLocales())) {
    ////			if (!locale.getCountry().isEmpty()) continue;
    ////			String name = locale.getDisplayName(ULocale.ENGLISH).toUpperCase(Locale.ENGLISH);
    ////			String languageTag = locale.toLanguageTag();
    ////			languageNameToTag.put(name, languageTag);
    ////		}
    ////		languageNameToTag.put("CHINESET", "zh-Hant");
    ////		languageNameToTag.put("DHIVEHI", "dv");
    ////		languageNameToTag.put("FRISIAN", "fy");
    ////		languageNameToTag.put("HAITIAN_CREOLE", "ht");
    ////		languageNameToTag.put("KYRGYZ", "ky");
    ////		languageNameToTag.put("LAOTHIAN", "lo");
    ////		languageNameToTag.put("SCOTS_GAELIC", "gd");
    ////		languageNameToTag.put("SINHALESE", "si");
    ////		languageNameToTag.put("TAGALOG", "fil");
    ////		languageNameToTag.put("BIHARI", "bho");
    ////		languageNameToTag.put("UNKNOWN", "und");
    //
    //		Map<String, Counter<String>> rawLanguageToSequencesCounter = new TreeMap<String, Counter<String>>();
    //		//Map<String, Counter<String>> rawLanguageToCharsCounter = new TreeMap<String, Counter<String>>();
    //
    //		//      System.out.println("loading stats.characters.txt");
    //		//      SemiFileReader handler = new SequenceHandler(rawLanguageToCharsCounter).process(Utility.DATA_DIRECTORY + "/frequency/", "stats.short_sequences.txt");
    //		//      System.out.println("read lines:\t" + handler.getLineCount());
    //
    //		Counter<String> mulValue = new Counter<String>();
    //
    //		System.out.println("loading stats.lang_sequences.txt");
    //		SemiFileReader handler = new SequenceHandler(rawLanguageToSequencesCounter).process(DATA_DIR,
    //				"mul.txt");
    //		System.out.println("read lines:\t" + handler.getLineCount());
    //
    //		System.out.println("fixing counts");
    //
    //		// make sure we have the same set of languages for both
    //		Set<String> languages = new TreeSet<String>();
    //		//languages.addAll(rawLanguageToCharsCounter.keySet());
    //		languages.addAll(rawLanguageToSequencesCounter.keySet());
    //
    //		// first add subtract the sequence counts from the character counts
    //
    //		for (String language : languages) {
    //			ULocale locale = new ULocale(language);
    //			Counter<String> combinedCounter = new Counter<String>();
    //			languageToCharsCounter.put(language, combinedCounter);
    //			// make sure we have non null data for both
    //
    //			if (!rawLanguageToSequencesCounter.containsKey(language)) {
    //				rawLanguageToSequencesCounter.put(language, combinedCounter);
    //			}
    //			//        if (!rawLanguageToCharsCounter.containsKey(language)) {
    //			//          rawLanguageToCharsCounter.put(language, combinedCounter);
    //			//        }
    //
    //			// get the counters
    //			Counter<String> sequenceCounter = rawLanguageToSequencesCounter.get(language);
    //			//Counter<String> charCounter = rawLanguageToCharsCounter.get(language);
    //
    //
    //			// Subtract all of the characters from the char counter
    //			//        for (String sequence : sequenceCounter.keySet()) {
    //			//          long sequenceCount = sequenceCounter.get(sequence);
    //			//          int cp;
    //			//          for (int i = 0; i < sequence.length(); i+=Character.charCount(cp)) {
    //			//            cp = sequence.codePointAt(i);
    //			//            String cpStr = UTF16.valueOf(cp);
    //			//            long charCount = charCounter.get(cpStr);
    //			//            if (sequenceCount > charCount) { // debug
    //			//              System.out.println(language + "\tsequence:\t" + sequenceCount + "\tchar:\t" + charCount);
    //			//            }
    //			//            charCounter.add(cpStr, -sequenceCount);
    //			//          }
    //			//        }
    //
    //			// the sets are now orthogonal.
    //			// put all of the normalized marks into a combined list
    //
    //			for (String sequence : sequenceCounter.keySet()) {
    //				addNormalizedCount(sequence, sequenceCounter.get(sequence), locale, combinedCounter);
    //			}
    //			//        for (String sequence : charCounter.keySet()) {
    //			//          addNormalizedCount(sequence, charCounter.get(sequence), locale, combinedCounter);
    //			//        }
    //
    //
    //			// at this point, the chars contain all the NFC'd characters, and the sequences contain all the sequences
    //			// sequenceCounter.freeze();
    //			// charCounter.freeze();
    //
    //			// now get the mul values
    //			mulValue.addAll(combinedCounter);
    //		}
    //
    //		languageToCharsCounter.put("mul", mulValue);
    //		Counter<String> supValue = new Counter<String>();
    //		for (String x : mulValue.keySet()) {
    //			int cp = x.codePointAt(0);
    //			if (cp > 0xFFFF) {
    //				supValue.add(x, mulValue.get(x));
    //			}
    //		}
    //		languageToCharsCounter.put("qsu", supValue);
    //		languageToPopulation.put("mul", 7000000000d * 0.82d);
    //		languageToPopulation.put("qsu", 7000000000d * 0.82d);
    //	}

    //	private static void addNormalizedCount(String sequence, long countValue, ULocale locale, Counter<String> combinedCounter) {
    //		String nfcSequence = ExemplarInfo.specialNormalize(sequence, locale);
    //		int cp;
    //		for (int i = 0; i < nfcSequence.length(); i+=Character.charCount(cp)) {
    //			cp = nfcSequence.codePointAt(i);
    //			combinedCounter.add(UTF16.valueOf(cp), countValue);
    //		}
    //	}



    //	public static String getLanguageCode(String string) {
    //		String result = LanguageCodeConverter.getCodeForName(string);
    //		//    string = string.toUpperCase(Locale.ENGLISH);
    //		//    String result = CharacterFrequency.languageNameToTag.get(string);
    //		if (result == null) {
    //			throw new IllegalArgumentException();
    //			//      result = "?"+string;
    //			//      languageNameToTag.put(string, result);
    //		}
    //		CharacterFrequency.languagesFound.put(string, result);
    //		return result;
    //	}

    //	private static void checkPopulation(String language) {
    //		String original = language;
    //		Double pop = languageToPopulation.get(language);
    //		if (pop == null) {
    //			if (language.equals("zh")) {
    //				language = "zh-Hans"; // special case
    //			}
    //			double pop2;
    //			String cldrLanguage = ExemplarInfo.getCldrLanguage(language);
    //			PopulationData popData = CharacterFrequency.supplementalInfo.getLanguagePopulationData(cldrLanguage);
    //			if (popData != null) {
    //				pop2 = popData.getLiteratePopulation();
    //			} else {
    //				pop2 = 0;
    //				for (String child : CharacterFrequency.supplementalInfo.getLanguagesForTerritoriesPopulationData()) {
    //					if (child.startsWith(cldrLanguage + "_")) {
    //						popData = CharacterFrequency.supplementalInfo.getLanguagePopulationData(child);
    //						if (popData != null) {
    //							pop2 += popData.getLiteratePopulation();
    //						}
    //					}
    //				}
    //				if (pop2 == 0) {
    //					System.out.println("Still failed for:" + language);
    //				}
    //			}
    //			languageToPopulation.put(original, pop2);
    //		}
    //		/*
    //		 * Still failed for:bh
    //  Still failed for:eo
    //  Still failed for:ky
    //  Still failed for:sd
    //
    //		 */
    //	}

    //	public static Double getLanguageToPopulation(String language) {
    //		return languageToPopulation.get(language);
    //	}

    //  public static Counter<String> getClusterCounter(String language) {
    //    return languageToSequencesCounter.get(language);
    //  }

    //	public static Counter<String> getCharCounter(String language) {
    //		Counter<String> result = languageToCharsCounter.get(language);
    //		if (result == null) {
    //			result = new Counter<String>();
    //			SemiFileReader handler = new SequenceHandler(result).process(DATA_DIR, "mul.txt");
    //
    //		}
    //	}

    public static Counter<Integer> getCodePointCounter(String language, boolean ranked) {
        Counter<Integer> result = languageToCodePointCounter.get(language);
        if (result == null) {
            result = new Counter<Integer>();
            final SemiFileReader handler = new SequenceHandler(result).process(ranked ? DATA_DIR_RANK : DATA_DIR, language + ".txt");
            languageToCodePointCounter.put(language, result);
        }
        return result;
    }

    static class SequenceHandler extends FileUtilities.SemiFileReader {
        Counter<Integer> counter;
        long lineCounter = 0;

        public SequenceHandler(Counter<Integer> counter) {
            this.counter = counter;
        }
        @Override
        public boolean handleLine(int start, int end, String[] items) {
            //			if (getLineCount() > CharacterFrequency.MAX_LINE_COUNT) {
            //				return false;
            //			}
            if (DEBUG && (++lineCounter % 1000) == 0) {
                System.out.println(lineCounter + "\t" + Arrays.asList(items) + "\t" + counter.getItemCount());
            }

            if (items.length != 2) {
                throw new IllegalArgumentException(Arrays.asList(items).toString());
            }

            final int cp = Integer.parseInt(items[0],16);
            final long count = Long.parseLong(items[1]);
            //			if (count < CharacterFrequency.MIN_COUNT) {
            //				return true;
            //			}
            counter.add(cp, count);
            return true;
        }
    }

    static final Set<String> LANGUAGES;
    static {
        final HashSet<String> result = new HashSet<String>();
        final File dir = new File(DATA_DIR);
        for (final String file : dir.list()) {
            if (!file.endsWith(".txt")) {
                continue;
            }
            result.add(file.substring(0,file.length()-4));
        }
        LANGUAGES = Collections.unmodifiableSet(result);
    }

    public static Set<String> getLanguagesWithCounter() {
        return LANGUAGES;
    }
}
