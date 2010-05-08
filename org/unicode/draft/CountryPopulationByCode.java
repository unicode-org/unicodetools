package org.unicode.draft;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.SupplementalDataInfo.PopulationData;

import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R4;

public class CountryPopulationByCode {
  static TestInfo testInfo = TestInfo.getInstance();

  public static void main(String[] args) {

    Set<R4<Double, Double, String, Integer>> byPopulation = new TreeSet<R4<Double, Double, String, Integer>>();

    for (String code : testInfo.getStandardCodes().getGoodAvailableCodes("territory")) {
      Set<Integer> numbers = testInfo.getSupplementalDataInfo().numericTerritoryMapping
              .getAll(code);
      if (numbers == null) {
        System.out.println("Skipping " + code);
        continue;
      }
      for (Integer regionNumber : numbers) {
        PopulationData population = testInfo.getSupplementalDataInfo()
                .getPopulationDataForTerritory(code);
        if (population == null) {
          System.out.println("Skipping " + code + ", " + regionNumber);
          continue;
        }
        R4<Double, Double, String, Integer> items = Row
                .of(population.getPopulation(), population.getGdp(), code, regionNumber);
        byPopulation.add(items);
      }
    }
    for (R4<Double, Double, String, Integer> row : byPopulation) {
      final String name = row.get2();
      System.out.println(testInfo.getEnglish().getName(CLDRFile.TERRITORY_NAME, name)
              + "\t" + row.get0()
              + "\t" + row.get1()
              + "\t" + name
              + "\t" + row.get3());
    }
  }
}
