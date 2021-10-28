package org.unicode.text.tools;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Iso639Data;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.CLDRFile.WinningChoice;

public class CLDRCharacterUtility {
    public static UnicodeMap<Set<String>> getCLDRCharacters() {
        UnicodeMap<Set<String>> result = new UnicodeMap<>();
        org.unicode.cldr.util.Factory factory = CLDRConfig.getInstance().getCldrFactory();
        //        File[] paths = { new File(CLDRPaths.MAIN_DIRECTORY)
        //        //, new File(CLDRPaths.SEED_DIRECTORY), new File(CLDRPaths.EXEMPLARS_DIRECTORY)
        //        };
        //        Factory factory = SimpleFactory.make(paths, ".*");
        Set<String> localeCoverage = StandardCodes.make().getLocaleCoverageLocales("cldr");
        Set<String> skipped = new LinkedHashSet<>();
        LanguageTagParser ltp = new LanguageTagParser();
        for (String localeId : localeCoverage) { //  factory.getAvailableLanguages()
            ltp.set(localeId);
            if (!ltp.getRegion().isEmpty()) {
                continue;
            }
            Iso639Data.Type type = Iso639Data.getType(ltp.getLanguage());
            if (type != Iso639Data.Type.Living) {
                skipped.add(localeId);
                continue;
            }
            CLDRFile cldrFile;
            try {
                cldrFile = factory.make(localeId, false);
            } catch (Exception e) {
                if (!localeId.equals("jv")) { // temporary hack
                    throw e;
                }
                System.err.println("Couldn't open: " + localeId);
                continue;
            }
            UnicodeSet exemplars = cldrFile
                    .getExemplarSet("", WinningChoice.WINNING);
            if (exemplars != null) {
                exemplars.closeOver(UnicodeSet.CASE);
                for (String s : flatten(exemplars)) { // flatten
                    Set<String> old = result.get(s);
                    if (old == null) {
                        result.put(s, Collections.singleton(localeId));
                    } else {
                        old = new TreeSet<String>(old);
                        old.add(localeId);
                        result.put(s, Collections.unmodifiableSet(old));
                    }
                }
            }
        }
        System.out.println("Skipped non-living languages " + skipped);
        return result;
    }

    private static UnicodeSet flatten(UnicodeSet result) {
        UnicodeSet result2 = new UnicodeSet();
        for (String s : result) { // flatten
            result2.addAll(s);
        }
        return result2;
    }
}