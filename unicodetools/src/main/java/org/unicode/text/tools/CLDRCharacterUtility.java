package org.unicode.text.tools;

class CLDRCharacterUtility {
    public static UnicodeMap<Set<String>> getCLDRCharacters() {
        UnicodeMap<Set<String>> result = new UnicodeMap<>();
        Factory factory = CLDRConfig.getInstance().getCldrFactory();
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
}