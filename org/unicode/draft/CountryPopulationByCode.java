package org.unicode.draft;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.SupplementalDataInfo.OfficialStatus;
import org.unicode.cldr.util.SupplementalDataInfo.PopulationData;
import org.unicode.jsp.FileUtilities;
import org.unicode.jsp.FileUtilities.SemiFileReader;

import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R4;
import com.ibm.icu.impl.Row.R5;
import com.ibm.icu.util.ULocale;

public class CountryPopulationByCode {
	private static final boolean SHOW_INTERNET = false;
	private static final boolean SHOW_WEIGHTS = true;
	private static final boolean SHOW_SOURCE = false;
	private static final boolean FILTER = true;
	static TestInfo testInfo = TestInfo.getInstance();

	public static void main(String[] args) {
		final Set<String> territories = testInfo.getStandardCodes().getAvailableCodes("territory");
		for (final String territory : territories) {
			final String name = testInfo.getEnglish().getName("territory", territory);
			if (name == null) {
				continue;
			}
			System.out.println(name + "\t" + territory + "\t" + name);
			final String alt = testInfo.getStandardCodes().getData("territory", territory);
			if (!alt.equals(name)) {
				System.out.println(alt + "\t" + territory + "\t" + name);
			}
		}
		//countryPopulation();
		countryLanguagePopulation();
	}

	private static void countryLanguagePopulation() {
		final Map<String, Row.R2<Integer, Integer>> country2internetUsers = new TreeMap();

		final SemiFileReader handler = new SequenceHandler(country2internetUsers).process(CountryPopulationByCode.class, "internetUsers.txt");

		final Counter<String> gdp = new Counter();
		final Counter<String> language2InternetLatest = new Counter();
		final TreeSet<R5<String, Double, Double, String, Boolean>> rowSet = new TreeSet();

		for (final String territoryCode : testInfo.getStandardCodes().getGoodAvailableCodes("territory")) {
			Set<String> languages;
			try {
				languages = testInfo.getSupplementalDataInfo().getLanguagesForTerritoryWithPopulationData(territoryCode);
				if (languages == null) {
					continue;
				}
			} catch (final Exception e) {
				continue;
			}
			final R2<Integer, Integer> internetData = country2internetUsers.get(territoryCode);
			if (internetData == null) {
				continue;
			}
			double totalWeighted = 0;
			double total = 0;

			final PopulationData territoryData = testInfo.getSupplementalDataInfo().getPopulationDataForTerritory(territoryCode);
			final double territoryGdp = territoryData.getGdp();

			double maxLiteratePopulation = 0;
			if (SHOW_SOURCE || territoryCode.equals("CN")) {
				System.out.println(
						territoryCode + "\t"
								+ testInfo.getEnglish().getName(CLDRFile.TERRITORY_NAME,territoryCode)
								+ "\t"
								+ "\t"
								+ "\t" + territoryData.getPopulation()
								+ "\t" + territoryData.getLiteratePopulation() // getWeightedLiteratePopulation(data)
								+ "\t" + territoryData.getOfficialStatus()
								+ "\t" + territoryData.getGdp()
						);

			}
			for (final String languageCode : languages) {
				final PopulationData data = testInfo.getSupplementalDataInfo().getLanguageAndTerritoryPopulationData(languageCode, territoryCode);
				if (SHOW_SOURCE || territoryCode.equals("CN")) {
					System.out.println(
							territoryCode + "\t"
									+ testInfo.getEnglish().getName(CLDRFile.TERRITORY_NAME,territoryCode)
									+ "\t" + languageCode
									+ "\t" + getBaseName(languageCode)
									+ "\t" + data.getPopulation()
									+ "\t" + data.getLiteratePopulation() // getWeightedLiteratePopulation(data)
									+ "\t" + data.getOfficialStatus());
				}
				totalWeighted += getWeightedLiteratePopulation(data, languageCode);
				final double literatePopulation = data.getLiteratePopulation();
				if (maxLiteratePopulation < literatePopulation) {
					maxLiteratePopulation = literatePopulation;
				}
				total += literatePopulation;
			}
			final double literatePopulationInTerritory = territoryData.getLiteratePopulation();
			addRow(rowSet, territoryCode, "und", 1-total/literatePopulationInTerritory, 1-total/literatePopulationInTerritory, OfficialStatus.unknown);
			addRow(rowSet, territoryCode, "mul", 1-maxLiteratePopulation/literatePopulationInTerritory, 1-maxLiteratePopulation/literatePopulationInTerritory, OfficialStatus.unknown);

			for (final String languageCode : languages) {
				final PopulationData data = testInfo.getSupplementalDataInfo().getLanguageAndTerritoryPopulationData(languageCode, territoryCode);

				final String languageName = getBaseName(languageCode);
				if (territoryCode.equals("IL")) {
					System.out.println("$$\t" + data.getLiteratePopulation() + "\t" + testInfo.getEnglish().getName(languageCode));
				}
				final double ratioWeighted = getWeightedLiteratePopulation(data, languageCode)/totalWeighted;
				final double ratio = data.getLiteratePopulation()/literatePopulationInTerritory;

				addRow(rowSet, territoryCode, languageCode, ratioWeighted, ratio, data.getOfficialStatus());

				gdp.add(languageName, (int)(ratioWeighted * territoryGdp));
				language2InternetLatest.add(languageName, (int)(ratioWeighted * internetData.get1()));
			}
		}
		if (SHOW_WEIGHTS) {
			System.out.println("*** Factors");
			System.out.println("und = 1-total/literatePopulationInTerritory, 1-total/literatePopulationInTerritory");
			System.out.println("mul = 1-maxLiteratePopulation/literatePopulationInTerritory, 1-maxLiteratePopulation/literatePopulationInTerritory");
			System.out.println("region" + "\t" + "code"
					//+ "\t" + "rank"
					+ "\t" + "ratio"
					+ "\t" + "weighted-ratio"
					+ "\t" + "language"
					+ "\t" + "code"
					+ "\t" + "status"
					//+ "\t" + "K-if-Key"
					);

			Object oldRegion = "";
			int counter = 1;
			for (final R5<String, Double, Double, String, Boolean> row : rowSet) {
				final Object region = row.get0();
				final double ratio = -row.get1();
				final double weightedRatio = -row.get2();
				final String languageCodeStatus = row.get3();
				if (FILTER && (
						weightedRatio < 0.01
						|| languageCodeStatus.contains("\tund\t")
						|| languageCodeStatus.contains("\tmul\t"))) {
					continue;
				}
				counter = region.equals(oldRegion) ? counter + 1 : 1;
				System.out.println(region
						//+ "\t" + counter
						+ "\t" + ratio
						+ "\t" + weightedRatio
						+ "\t" + languageCodeStatus
						//+ "\t" + (row.get4() ? "K" : "")
						);
				oldRegion = region;
			}
		}

		if (SHOW_INTERNET) {
			//        for (String languageName : language2Internet2000.keySet()) {
			//            System.out.println(languageName + "\t2000\t" + language2Internet2000.get(languageName));
			//        }
			System.out.println("*** internet/gdp");
			for (final String languageName : gdp.getKeysetSortedByCount(false)) {
				System.out.println(languageName + "\t" + language2InternetLatest.get(languageName) + "\t" + gdp.get(languageName));
			}
		}
	}

	private static void addRow(TreeSet<R5<String, Double, Double, String, Boolean>> rowSet, String territoryCode, String languageCode, double ratioWeighted, double ratio, OfficialStatus officialStatus) {
		final R5<String, Double, Double, String, Boolean> row = Row.of(
				testInfo.getEnglish().getName("territory", territoryCode) + "\t" + territoryCode,
				-ratio,
				-ratioWeighted,
				testInfo.getEnglish().getName(languageCode) + "\t" + languageCode + "\t" + (officialStatus == OfficialStatus.unknown ? "" : officialStatus.toString()),
				KEY_LANGUAGES.contains(languageCode));
		rowSet.add(row);
	}

	static final         Set<String> KEY_LANGUAGES = new LinkedHashSet(Arrays.asList(
			"en", "es", "de", "fr", "ja", "it", "tr", "pt", "zh", "nl",
			"pl", "ar", "ru", "zh_Hant", "ko", "th", "sv", "fi", "da",
			"he", "nb", "el", "hr", "bg", "sk", "lt", "vi", "lv", "sr",
			"pt_PT", "ro", "hu", "cs", "id", "sl", "fil", "fa", "uk",
			"ca", "hi", "et", "eu", "is", "sw", "ms", "bn", "am", "ta",
			"te", "mr", "ur", "ml", "kn", "gu", "or"));

	private static double getWeightedLiteratePopulation(PopulationData data, String languageCode) {
		return data.getLiteratePopulation() * (languageCode.equals("nn") ? OfficialStatus.official_minority : data.getOfficialStatus()).getWeight();
	}

	private static String getBaseName(String languageCode) {
		final String baseLanguage = languageCode.contains("Hant") ? languageCode : new ULocale(languageCode).getLanguage();

		final String languageName = testInfo.getEnglish().getName(baseLanguage);
		return languageName;
	}

	static class SequenceHandler extends FileUtilities.SemiFileReader {
		Map<String, Row.R2<Integer, Integer>> country2internetUsers = new TreeMap();
		public final static Pattern TABS = Pattern.compile("\\t+");
		public static final Map<String,String> name2code = new HashMap();
		public static final Map<String,String> remapName = new HashMap();
		static {
			for (final String territory : testInfo.getStandardCodes().getGoodAvailableCodes("territory")) {
				name2code.put(testInfo.getEnglish().getName(CLDRFile.TERRITORY_NAME, territory), territory);
			}
			remapName.put("Korea, South", "South Korea");
			remapName.put("Hong Kong�*", "Hong Kong SAR China");
			remapName.put("Bosnia-Herzegovina", "Bosnia and Herzegovina");
			remapName.put("Kyrgystan", "Kyrgyzstan");
			remapName.put("Cote d'Ivoire", "Côte d’Ivoire");
			remapName.put("Afganistan", "Afghanistan");
			remapName.put("Trinidad & Tobago", "Trinidad and Tobago");
			remapName.put("Kosovo", "Serbia");
			remapName.put("Palestine(West Bk.)", "Palestinian Territories");
			remapName.put("Congo, Dem. Rep.", "Congo - Kinshasa");
			remapName.put("Reunion (FR)", "Réunion");
			remapName.put("Macao�*", "Macau SAR China");
			remapName.put("Brunei Darussalem", "Brunei");
			remapName.put("Congo", "Congo - Brazzaville");
			remapName.put("Papau New Guinea", "Papua New Guinea");
			remapName.put("Myanmar", "Myanmar [Burma]");
			remapName.put("St. Vincent & Grenadines", "Saint Vincent and the Grenadines");
			remapName.put("Antigua & Barbuda", "Antigua and Barbuda");
			remapName.put("Guernsey & Alderney", "Guernsey");
			remapName.put("US Virgin Islands", "U.S. Virgin Islands");
			remapName.put("Sao Tome & Principe", "São Tomé and Príncipe");
			remapName.put("Central African Rep.", "Central African Republic");
			remapName.put("St. Kitts & Nevis", "Saint Kitts and Nevis");
			remapName.put("Northern Marianas", "Northern Mariana Islands");
			remapName.put("Monserrat", "Montserrat");
			remapName.put("Wallis & Futuna", "Wallis and Futuna");
			remapName.put("Saint Helena (UK)", "Saint Helena");
			remapName.put("Vatican City State", "Vatican City");
		}

		@Override
		protected String[] splitLine(String line) {
			return TABS.split(line);
		}

		public SequenceHandler(Map<String, R2<Integer, Integer>> rawLanguageToSequencesCounter2) {
			country2internetUsers = rawLanguageToSequencesCounter2;
		}

		@Override
		protected boolean isCodePoint() {
			return false;
		}
		@Override
		protected boolean handleLine(int start, int end, String[] items) {
			String code = name2code.get(items[0]);
			final String rename = remapName.get(items[0]);
			if (rename != null) {
				code = name2code.get(rename);
			}
			if (code == null) {
				//System.out.println("remapName.put(\"" + items[0] + "\", \"XX\");");
				code = items[0];
			}
			country2internetUsers.put(code,
					Row.of(Integer.parseInt(items[1].replace(",","")), Integer.parseInt(items[2].replace(",",""))));
			return true;
		}
		@Override
		protected void handleEnd() {
			final Set<String> missing = new TreeSet(testInfo.getStandardCodes().getGoodAvailableCodes("territory"));
			missing.removeAll(country2internetUsers.keySet());
			for (final String s : missing) {
				System.out.println("//missing\t" + s + "\t" + testInfo.getEnglish().getName(CLDRFile.TERRITORY_NAME, s));
			}
		}
	}

	private static void countryPopulation() {
		final Set<R4<Double, Double, String, Integer>> byPopulation = new TreeSet<R4<Double, Double, String, Integer>>();

		for (final String code : testInfo.getStandardCodes().getGoodAvailableCodes("territory")) {
			final Set<Integer> numbers = testInfo.getSupplementalDataInfo().numericTerritoryMapping
					.getAll(code);
			if (numbers == null) {
				//System.out.println("Skipping " + code);
				continue;
			}
			for (final Integer regionNumber : numbers) {
				final PopulationData population = testInfo.getSupplementalDataInfo()
						.getPopulationDataForTerritory(code);
				if (population == null) {
					System.out.println("Skipping " + code + ", " + regionNumber);
					continue;
				}
				final R4<Double, Double, String, Integer> items = Row
						.of(population.getPopulation(), population.getGdp(), code, regionNumber);
				byPopulation.add(items);
			}
		}
		for (final R4<Double, Double, String, Integer> row : byPopulation) {
			final String name = row.get2();
			System.out.println(testInfo.getEnglish().getName(CLDRFile.TERRITORY_NAME, name)
					+ "\t" + row.get0()
					+ "\t" + row.get1()
					+ "\t" + name
					+ "\t" + row.get3());
		}
	}
}
