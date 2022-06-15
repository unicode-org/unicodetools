package org.unicode.propstest;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeMap.EntryRange;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.UnicodeSet;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import org.unicode.cldr.tool.LikelySubtags;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CLDRFile.WinningChoice;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Iso639Data;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.SimpleFactory;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PopulationData;
import org.unicode.props.GenerateEnums;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;
import org.unicode.props.UcdPropertyValues.Binary;
import org.unicode.props.UcdPropertyValues.Identifier_Status_Values;
import org.unicode.props.UcdPropertyValues.Identifier_Type_Values;

public class CompareExemplarsToIdmod {
    static final IndexUnicodeProperties iup =
            IndexUnicodeProperties.make(GenerateEnums.ENUM_VERSION);
    static final UnicodeMap<String> nameMap = iup.load(UcdProperty.Name);

    static final CLDRConfig testInfo = CLDRConfig.getInstance();

    private static final SupplementalDataInfo SUPPLEMENTAL_DATA_INFO =
            testInfo.getSupplementalDataInfo();
    static final Factory cldrFactory;

    static {
        File[] paths = {
            new File(CLDRPaths.MAIN_DIRECTORY),
            new File(CLDRPaths.SEED_DIRECTORY),
            new File(CLDRPaths.EXEMPLARS_DIRECTORY)
        };
        cldrFactory = SimpleFactory.make(paths, ".*");
    }

    static final Set<String> defaultContents = SUPPLEMENTAL_DATA_INFO.getDefaultContentLocales();
    static final Normalizer2 nfd = Normalizer2.getNFDInstance();
    static final Normalizer2 nfkc = Normalizer2.getNFKCInstance();

    public static void main(String[] args) {
        UnicodeMap<Identifier_Status_Values> statusMap =
                iup.loadEnum(
                        UcdProperty.Identifier_Status,
                        UcdPropertyValues.Identifier_Status_Values.class);
        UnicodeMap<Identifier_Type_Values> typeMap =
                iup.loadEnum(
                        UcdProperty.Identifier_Type,
                        UcdPropertyValues.Identifier_Type_Values.class);
        UnicodeSet xidContinue =
                iup.loadEnum(UcdProperty.XID_Continue, UcdPropertyValues.Binary.class)
                        .getSet(Binary.Yes);
        UnicodeSet Unified_Ideograph =
                iup.loadEnum(UcdProperty.Unified_Ideograph, UcdPropertyValues.Binary.class)
                        .getSet(Binary.Yes);
        UnicodeMap<String> Confusables = iup.load(UcdProperty.Confusable_MA);

        UnicodeSet allowed = statusMap.getSet(Identifier_Status_Values.Allowed);

        CLDRFile english = testInfo.getEnglish();
        LanguageTagParser ltp = new LanguageTagParser();

        //        Set<String> nonapprovedLocales = new LinkedHashSet();
        //        UnicodeMap<String> restricted = new UnicodeMap();
        //        UnicodeSet allowedHangulTypes = new UnicodeSet("[ᄀ-ᄒ ᅡ-ᅵ ᆨ-ᇂ]").freeze();

        Set<String> nonLiving = new TreeSet();
        Set<String> missingPopData = new TreeSet();
        Set<String> lowPopData = new TreeSet();
        Set<String> noExemplars = new TreeSet();

        UnicodeSet flattened = new UnicodeSet();

        UnicodeSet flattenedAllowed = new UnicodeSet();
        for (String cp : allowed) {
            String nfdValue = nfd.normalize(cp);
            flattenedAllowed.addAll(nfd.normalize(cp));
        }
        System.out.println("flattenedAllowed: " + flattenedAllowed.toPattern(false));

        HashSet<String> seen = new HashSet();
        LikelySubtags likely = new LikelySubtags();
        UnicodeMap<Integer> charToPopulation = new UnicodeMap();

        for (String rawLocale : cldrFactory.getAvailable()) {
            if (defaultContents.contains(rawLocale)
                    || !ltp.set(rawLocale).getRegion().isEmpty()
                    || ltp.getScript().equals("Dsrt")) {
                continue;
            }
            Iso639Data.Type type = Iso639Data.getType(ltp.getLanguage());
            if (type != Iso639Data.Type.Living) {
                nonLiving.add(ltp.getLanguage());
                continue;
            }
            String locale = rawLocale;
            PopulationData pop =
                    SUPPLEMENTAL_DATA_INFO.getBaseLanguagePopulationData(ltp.getLanguage());
            if (pop == null) {
                missingPopData.add(locale);
                continue;
            }
            int population = (int) pop.getLiteratePopulation();
            if (population < 2) {
                continue;
            }

            CLDRFile f = cldrFactory.make(locale, false, DraftStatus.approved);
            UnicodeSet uset = f.getExemplarSet("", WinningChoice.WINNING);
            if (uset == null) {
                noExemplars.add(locale);
                continue;
            }
            uset.removeAll(Unified_Ideograph);
            for (String cp : uset) {
                String nfdValue = nfd.normalize(cp);
                add(nfdValue, population, charToPopulation);
                String upper = cp.toUpperCase();
                if (!upper.equals(cp)) {
                    nfdValue = nfd.normalize(upper);
                    add(nfdValue, population, charToPopulation);
                }
            }
        }
        for (EntryRange<Integer> entry : charToPopulation.entryRanges()) {
            if (entry.value > 100000) {
                if (entry.string != null) {
                    flattened.add(entry.string);
                } else {
                    flattened.add(entry.codepoint, entry.codepointEnd);
                }
            }
        }
        UnicodeSet allowedButNotInCLDR =
                new UnicodeSet(flattenedAllowed).removeAll(flattened).removeAll(Unified_Ideograph);

        System.out.println("allowedButNotInCLDR: " + allowedButNotInCLDR.toPattern(false));

        System.out.println("nonLiving: " + nonLiving);
        System.out.println("missingPopData: " + missingPopData);
        System.out.println("lowPopData: " + lowPopData);
        System.out.println("noExemplars: " + noExemplars);

        //            String localeName = english.getName(locale) + " (" + locale + ")";
        //            UnicodeSet suspicious = new UnicodeSet();
        //            for (String cp : flattened) {
        //                if (!nfkc.isNormalized(cp)) {
        //                    continue;
        //                }
        //                if (!"Yes".equals(xidContinue.get(cp))) {
        //                    continue;
        //                }
        //                if (allowedHangulTypes.contains(cp)) {
        //                    continue;
        //                }
        //                if (!"allowed".equals(statusMap.get(cp))) {
        //                    String s = restricted.get(cp);
        //                    String info = localeName + " " + typeMap.get(cp);
        //                    restricted.put(cp, s == null ? info : s + "; " + info);
        //                    suspicious.add(cp);
        //                }
        //            }
        //            if (!suspicious.isEmpty()) {
        //                for (String path : f){
        //                    if (path.contains("character")) {
        //                        continue;
        //                    }
        //                    String value = f.getStringValue(path);
        //                    suspicious.removeAll(nfd.normalize(value));
        //
        // suspicious.removeAll(nfd.normalize(UCharacter.toUpperCase(ULocale.ROOT, value)));
        //
        // suspicious.removeAll(nfd.normalize(UCharacter.toLowerCase(ULocale.ROOT, value)));
        //                }
        //            }
        //            if (!suspicious.isEmpty()) {
        //                System.out.println(localeName + "\tSuspicious characters; never in CLDR
        // data: ");
        //                for (String cp : suspicious) {
        //                    System.out.println("\t" + getCodeAndName(cp));
        //                }
        //            }
        //        }
        //        for (String cp : restricted) {
        //            System.out.println(Utility.hex(cp) + " (" + cp + ") " + nameMap.get(cp) + "\t"
        // + restricted.get(cp));
        //        }
        //        for (String locale :  nonapprovedLocales) {
        //            System.out.println("No approved exemplars for " + english.getName(locale) +
        // "\t" + locale);
        //        }
    }

    private static void add(String nfdValue, int population, UnicodeMap<Integer> charToPopulation) {
        for (int cp : CharSequences.codePoints(nfdValue)) {
            Integer old = charToPopulation.get(cp);
            charToPopulation.put(cp, old == null ? population : population + old);
        }
    }

    public static String getCodeAndName(String cp) {
        return Utility.hex(cp) + " (" + cp + ") " + nameMap.get(cp);
    }
}
