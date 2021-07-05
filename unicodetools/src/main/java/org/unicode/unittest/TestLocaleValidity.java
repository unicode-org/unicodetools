package org.unicode.unittest;

import java.util.EnumSet;
import java.util.Map;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.ValidIdentifiers;
import com.ibm.icu.impl.ValidIdentifiers.Datasubtype;
import com.ibm.icu.impl.ValidIdentifiers.Datatype;
import com.ibm.icu.impl.ValidIdentifiers.ValiditySet;
import com.ibm.icu.impl.locale.LocaleValidityChecker;
import com.ibm.icu.impl.locale.LocaleValidityChecker.Where;
import com.ibm.icu.util.ULocale;

public class TestLocaleValidity extends TestFmwk {
    private static final boolean DEBUG = false;

    static final LocaleValidityChecker validityAny = new LocaleValidityChecker(EnumSet.allOf(Datasubtype.class));
    static final LocaleValidityChecker validityNoDeprecated = new LocaleValidityChecker(
            EnumSet.of(Datasubtype.macroregion, Datasubtype.private_use, Datasubtype.regular, Datasubtype.special)
            );

    public static void main(String[] args) {
        new TestLocaleValidity().run(args);
    }

    enum Validity {ill_formed, invalid, valid_w_deprecated, valid_regular}
    
    public void TestLocales () {

        // examples: aay, bcc => bal; Abcd, none; AB, BU => MM; HEPLOC => ALALC97
        // example special cases: context and/or reset other fields
        check("a", Validity.ill_formed);
        check("$", Validity.ill_formed);
        check("еѕ", Validity.ill_formed);

        check("he-IL", Validity.valid_regular);
        check("aay-IL", Validity.invalid);
        check("iw-Latn-IL", Validity.valid_w_deprecated);
        check("iw-Abcd-IL", Validity.invalid);
        check("iw-Abcde-IL", Validity.ill_formed);
        check("iw-Latn-AB", Validity.invalid);
        check("en-fonipa-heploc", Validity.valid_regular);
        check("en-heploc-fonipa", Validity.valid_regular);
        
        check("en-u-nu-thai", Validity.valid_regular);
        check("en-u-abc-nu-thai", Validity.invalid); // TODO fix LocaleValidity.
        check("en-u-nu-foobar", Validity.invalid);
        
        // non u / t extensions
        check("en-a-nu-foobar", Validity.invalid); 
        check("en-x-u-nu-foobar", Validity.valid_regular);
    }

    public static final Map<Datatype, Map<Datasubtype, ValiditySet>> validityInfo = ValidIdentifiers.getData();

//    public static void TestAliases() {
//        LocaleDisplayNames eng = LocaleDisplayNames.getInstance(ULocale.ENGLISH);
//        Set<String> isoLanguages = new LinkedHashSet<>(Arrays.asList(ULocale.getISOLanguages()));
//        Where where = new Where();
//        for (String lang : isoLanguages) {
//            String name = eng.languageDisplayName(lang);
//            String dummy2 = name.substring(0,2).toLowerCase(Locale.ROOT);
//            String dummy3 = name.length() > 2 ? name.substring(0,3).toLowerCase(Locale.ROOT) : "und";
//            if (!validity.isValid(new ULocale(dummy2), where)
//                    && !validity.isValid(new ULocale(dummy3), where)) {
//                if (DEBUG) System.out.println(lang + " " + name);
//            }
//        }
//        if (DEBUG) System.out.println();
////        checkCodes(Datatype.language);
////        checkCodes(Datatype.region);
////        checkCodes(Datatype.variant);
//    }

//    private static void checkCodes(Datatype dataType) {
//        if (DEBUG) System.out.println("\n" + dataType);
//        AliasesFull aliases = new AliasesFull(dataType);
//        Output<Collection<ExceptionInfo>> exception = new Output<>();
//
//        for (Entry<Datasubtype, ValiditySet> entry : validityInfo.get(dataType).entrySet()) {
//            for (String code : entry.getValue().regularData) {
//                String replacement = aliases.getCanonical(
//                        dataType == Datatype.region || dataType == Datatype.variant? code.toUpperCase(Locale.ROOT) 
//                                : code,
//                                exception);
//                if (replacement != null) {
//                    if (DEBUG) System.out.println(code + " ==> " + replacement);
//                } else if (exception.value != null){
//                    if (DEBUG) System.out.println(code + " ==> " + exception.toString());
//                }
//            }
//        }
//    }

    // TODO change code for isValid so that it optionally returns a list of the 
    // item(s) found with anything less than "no_deprecated"
    // Could be something like: Multimap<Validity, Where>
    private void check(String localeString, Validity expectedValidity) {
        Validity actualValidity = null;
        ULocale locale;
        Where where = new Where();
        try {
            locale = new ULocale.Builder().setLanguageTag(localeString).build();
            if (validityNoDeprecated.isValid(locale, where)) {
                actualValidity = Validity.valid_regular;
            } else if (validityAny.isValid(locale, where)) {
                actualValidity = Validity.valid_w_deprecated;
            } else {
                actualValidity = Validity.invalid; 
            }
        } catch (Exception e) {
            actualValidity = Validity.ill_formed;
//            assertEquals("«" + localeString + "»"
//                    + " Syntax error: " + e.getMessage(), expectedValidity, Validity.ill_formed);
//            return;
        }
        assertEquals("«" + localeString + "» " + where, expectedValidity, actualValidity);
    }
}
