package org.unicode.jsptest;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.unicode.cldr.util.BNF;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Quoter;
import org.unicode.cldr.util.UnicodeSetPrettyPrinter;
import org.unicode.idna.Idna;
import org.unicode.idna.Idna.IdnaType;
import org.unicode.idna.Idna2003;
import org.unicode.idna.Idna2008;
import org.unicode.jsp.Common;
import org.unicode.jsp.UnicodeJsp;
import org.unicode.jsp.UnicodeRegex;
import org.unicode.jsp.UnicodeSetUtilities;
import org.unicode.jsp.UnicodeUtilities;
import org.unicode.jsp.UtfParameters;
import org.unicode.jsp.Uts46;
import org.unicode.jsp.XPropertyFactory;
import org.unicode.props.UnicodeProperty;
import org.unicode.unittest.TestFmwkMinusMinus;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UProperty.NameChoice;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.IDNA;
import com.ibm.icu.text.StringPrepParseException;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.LocaleData;
import com.ibm.icu.util.ULocale;

public class TestJsp  extends TestFmwkMinusMinus {

    private static final String enSample = "a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x, y, z, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, Z";
    static final UnicodeSet U5_2 = new UnicodeSet().applyPropertyAlias("age", "5.2").freeze();
    public static final UnicodeSet U5_1 = new UnicodeSet().applyPropertyAlias("age", "5.1").freeze();
    static UnicodeSet BREAKING_WHITESPACE = new UnicodeSet("[\\p{whitespace=true}-\\p{linebreak=glue}]").freeze();


    static UnicodeSet IPA = new UnicodeSet("[a-zæçðøħŋœǀ-ǃɐ-ɨɪ-ɶ ɸ-ɻɽɾʀ-ʄʈ-ʒʔʕʘʙʛ-ʝʟʡʢ ʤʧʰ-ʲʴʷʼˈˌːˑ˞ˠˤ̀́̃̄̆̈ ̘̊̋̏-̜̚-̴̠̤̥̩̪̬̯̰̹-̽͜ ͡βθχ↑-↓↗↘]").freeze();
    static String IPA_SAMPLE = "a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x, y, z, æ, ç, ð, ø, ħ, ŋ, œ, ǀ, ǁ, ǂ, ǃ, ɐ, ɑ, ɒ, ɓ, ɔ, ɕ, ɖ, ɗ, ɘ, ə, ɚ, ɛ, ɜ, ɝ, ɞ, ɟ, ɠ, ɡ, ɢ, ɣ, ɤ, ɥ, ɦ, ɧ, ɨ, ɪ, ɫ, ɬ, ɭ, ɮ, ɯ, ɰ, ɱ, ɲ, ɳ, ɴ, ɵ, ɶ, ɸ, ɹ, ɺ, ɻ, ɽ, ɾ, ʀ, ʁ, ʂ, ʃ, ʄ, ʈ, ʉ, ʊ, ʋ, ʌ, ʍ, ʎ, ʏ, ʐ, ʑ, ʒ, ʔ, ʕ, ʘ, ʙ, ʛ, ʜ, ʝ, ʟ, ʡ, ʢ, ʤ, ʧ, ʰ, ʱ, ʲ, ʴ, ʷ, ʼ, ˈ, ˌ, ː, ˑ, ˞, ˠ, ˤ, ̀, ́, ̃, ̄, ̆, ̈, ̊, ̋, ̏, ̐, ̑, ̒, ̓, ̔, ̕, ̖, ̗, ̘, ̙, ̚, ̛, ̜, ̝, ̞, ̟, ̠, ̡, ̢, ̣, ̤, ̥, ̦, ̧, ̨, ̩, ̪, ̫, ̬, ̭, ̮, ̯, ̰, ̱, ̲, ̳, ̴, ̹, ̺, ̻, ̼, ̽, ͜, ͡, β, θ, χ, ↑, →, ↓, ↗, ↘";

    enum Subtag {language, script, region, mixed, fail}

    static UnicodeSetPrettyPrinter pretty = new UnicodeSetPrettyPrinter().setOrdering(Collator.getInstance(ULocale.ENGLISH));

    static String prettyTruncate(int max, UnicodeSet set) {
        String prettySet = pretty.format(set);
        if (prettySet.length() > max) {
            prettySet = prettySet.substring(0,max) + "...";
        }
        return prettySet;
    }

    @Test
    public void TestLanguage() {
        String foo = UnicodeJsp.getLanguageOptions("de");
        assertContains(foo, "<option value='de' selected>Deutsch / German</option>");
        String fii = UnicodeJsp.validateLanguageID("en", "fr");
        assertContains(fii, "draft-ietf-ltru-4646bis");
    }

    @EnabledIf(value = "org.unicode.unittest.TestFmwkMinusMinus#getRunBroken", disabledReason = "Skip unless UNICODETOOLS_RUN_BROKEN_TEST=true")
    @Test
    public void TestJoiner() {

        checkValidity(Idna2003.SINGLETON, "a", true, true);
        checkValidity(Idna2003.SINGLETON, "ÖBB.at", true, true);
        checkValidity(Idna2003.SINGLETON, "xn--BB-nha.at", true, true);
        checkValidity(Idna2003.SINGLETON, "xn--bb-eka.at", true, true);
        checkValidity(Idna2003.SINGLETON, "a\u200cb", true, true);
        checkValidity(Idna2003.SINGLETON, "xn--ab-j1t", true, true);
        checkValidity(Idna2003.SINGLETON, "faß.de", true, true);
        checkValidity(Idna2003.SINGLETON, "xn--fa-hia.de", true, true);
        checkValidity(Idna2003.SINGLETON, "\u0080.de", false, true);
        checkValidity(Idna2003.SINGLETON, "xn--a.de", true, true);

        checkValidity(Uts46.SINGLETON, "a", true, true);
        checkValidity(Uts46.SINGLETON, "ÖBB.at", true, true);
        checkValidity(Uts46.SINGLETON, "xn--BB-nha.at", false, true);
        checkValidity(Uts46.SINGLETON, "xn--bb-eka.at", true, true);
        checkValidity(Uts46.SINGLETON, "a\u200cb", true, true);
        checkValidity(Uts46.SINGLETON, "xn--ab-j1t", true, true);
        checkValidity(Uts46.SINGLETON, "faß.de", true, true);
        checkValidity(Uts46.SINGLETON, "xn--fa-hia.de", true, true);
        checkValidity(Uts46.SINGLETON, "\u0080.de", false, true);
        checkValidity(Uts46.SINGLETON, "xn--a.de", false, true);

        checkValidity(Idna2008.SINGLETON, "a", true, true);
        checkValidity(Idna2008.SINGLETON, "ÖBB.at", false, false);
        checkValidity(Idna2008.SINGLETON, "xn--BB-nha.at", true, true);
        checkValidity(Idna2008.SINGLETON, "xn--bb-eka.at", true, true);
        checkValidity(Idna2008.SINGLETON, "a\u200cb", true, true);
        checkValidity(Idna2008.SINGLETON, "xn--ab-j1t", true, true);
        checkValidity(Idna2008.SINGLETON, "faß.de", true, true);
        checkValidity(Idna2008.SINGLETON, "xn--fa-hia.de", true, true);
        checkValidity(Idna2008.SINGLETON, "\u0080.de", false, false);
        checkValidity(Idna2008.SINGLETON, "xn--a.de", true, true);

    }

    private void checkValidity(Idna uts46, String url, boolean expectedPuny, boolean expectedUni) {
        boolean[] error = new boolean[1];
        String fii = uts46.toPunyCode(url, error);
        assertEquals(uts46.getName() + "\ttoPunyCode(" + url + ")="+fii, !expectedPuny, error[0]);
        fii = uts46.toUnicode(url, error, true);
        assertEquals(uts46.getName() + "\ttoUnicode(" + url + ")="+fii, !expectedUni, error[0]);
    }

    //  public void Test2003vsUts46() {
    //
    //    ToolUnicodePropertySource properties = ToolUnicodePropertySource.make("6.0");
    //    UnicodeMap<String> nfkc_cfMap = properties.getProperty("NFKC_CF").getUnicodeMap();
    //
    //    for (UnicodeSetIterator it = new UnicodeSetIterator(IdnaTypes.U32); it.next();) {
    //      int i = it.codepoint;
    //      String map2003 = Idna2003.SINGLETON.mappings.get(i);
    //      String map46 = Uts46.SINGLETON.mappings.get(i);
    //      IdnaType type2003 = Idna2003.SINGLETON.types.get(i);
    //      IdnaType type46 = Uts46.SINGLETON.types.get(i);
    //      if (type46 == IdnaType.ignored) {
    //        assertNotNull("tr46ignored U+" + codeAndName(i), map46);
    //      } else if (type46 == IdnaType.deviation) {
    //          type46 = map46 == null || map46.length() == 0
    //          ? IdnaType.ignored
    //                  : IdnaType.mapped;
    //      }
    //      if (type2003 == IdnaType.ignored) {
    //        assertNotNull("2003ignored", map2003);
    //      }
    //      if (type46 != type2003 || !UnicodeProperty.equals(map46, map2003)) {
    //        String map2 = map2003 == null ? UTF16.valueOf(i) : map2003;
    //        String nfcf = nfkc_cfMap.get(i);
    //        if (!map2.equals(nfcf)) continue;
    //        String typeDiff = type46 + "\tvs 2003\t" + type2003;
    //        String mapDiff = "[" + codeAndName(map46) + "\tvs 2003\t" + codeAndName(map2003);
    //        errln((codeAndName(i)) + "\tdifference:"
    //                + (type46 != type2003 ? "\ttype:\t" + typeDiff : "")
    //                + (!UnicodeProperty.equals(map46, map2003) ? "\tmap:\t" + mapDiff : "")
    //                +  "\tNFKCCF:\t" + codeAndName(nfcf));
    //      }
    //    }
    //  }

    private String codeAndName(int i) {
        return Utility.hex(i) + " ( " + UTF16.valueOf(i) + " ) " + UCharacter.getName(i);
    }

    private String codeAndName(String i) {
        return i == null ? null : (Utility.hex(i, 4, ",", true, new StringBuilder()) + " ( " + i + " ) " + UCharacter.getName(i, "+"));
    }


    static class TypeAndMap {
        IdnaType type;
        String mapping;
    }

    public void oldTestIdnaAndIcu() {
        StringBuffer inbuffer = new StringBuffer();
        TypeAndMap typeAndMapIcu = new TypeAndMap();
        UnicodeMap<String> errors = new UnicodeMap<String>();
        int count = 0;
        for (int cp = 0x80; cp < 0x10FFFF; ++cp) {
            inbuffer.setLength(0);
            inbuffer.appendCodePoint(cp);
            getIcuIdna(inbuffer, typeAndMapIcu);

            IdnaType type = Uts46.SINGLETON.getType(cp); // used to be Idna2003.
            String mapping = Uts46.SINGLETON.mappings.get(cp); // used to be Idna2003.
            if (type != typeAndMapIcu.type || !UnicodeProperty.equals(mapping, typeAndMapIcu.mapping)) {
                inbuffer.setLength(0);
                inbuffer.appendCodePoint(cp);
                getIcuIdna(inbuffer, typeAndMapIcu);
                String typeDiff = type + "\tvs ICU\t" + typeAndMapIcu.type;
                String mapDiff = "[" + mapping + "]\tvs ICU\t[" + typeAndMapIcu.mapping + "]";
                errors.put(cp, (type != typeAndMapIcu.type ? "\ttype:\t" + typeDiff : "")
                        + (!UnicodeProperty.equals(mapping, typeAndMapIcu.mapping) ? "\tmap:\t" + mapDiff : ""));
                //        errln(Utility.hex(cp) + "\t( " + UTF16.valueOf(cp) + " )\tdifference:"
                //                + (type != typeAndMapIcu.type ? "\ttype:\t" + typeDiff : "")
                //                + (!UnicodeProperty.equals(mapping, typeAndMapIcu.mapping) ? "\tmap:\t" + mapDiff : ""));
                if (++count > 50) {
                    break;
                }
            }
        }
        if (errors.size() != 0) {
            for (String value : errors.values()) {
                UnicodeSet s = errors.getSet(value);
                errln(value + "\t" + s.toPattern(false));
            }
        }
    }

    private void getIcuIdna(StringBuffer inbuffer, TypeAndMap typeAndMapIcu) {

        typeAndMapIcu.type = null;
        typeAndMapIcu.mapping = null;
        try {
            StringBuffer intermediate = convertWithHack(inbuffer);
            // DEFAULT
            if (intermediate.length() == 0) {
                typeAndMapIcu.type = IdnaType.ignored;
                typeAndMapIcu.mapping = "";
            } else {
                StringBuffer outbuffer = IDNA.convertToUnicode(intermediate, IDNA.USE_STD3_RULES);
                if (!UnicodeUtilities.equals(inbuffer, outbuffer)) {
                    typeAndMapIcu.type = IdnaType.mapped;
                    typeAndMapIcu.mapping = outbuffer.toString();
                } else {
                    typeAndMapIcu.type = IdnaType.valid;
                    typeAndMapIcu.mapping = null;
                }
            }
        } catch (StringPrepParseException e) {
            if (e.getMessage().startsWith("Found zero length")) {
                typeAndMapIcu.type = IdnaType.ignored;
                typeAndMapIcu.mapping = null;
            } else {
                typeAndMapIcu.type = IdnaType.disallowed;
                typeAndMapIcu.mapping = null;
            }
        } catch (Exception e) {
            logln("Failure at: " + Utility.hex(inbuffer));
            typeAndMapIcu.type = IdnaType.disallowed;
            typeAndMapIcu.mapping = null;
        }
    }

    private static StringBuffer convertWithHack(StringBuffer inbuffer) throws StringPrepParseException {
        StringBuffer intermediate;
        try {
            intermediate = IDNA.convertToASCII(inbuffer, IDNA.USE_STD3_RULES); // USE_STD3_RULES,
        } catch (StringPrepParseException e) {
            if (!e.getMessage().contains("BIDI")) {
                throw e;
            }
            inbuffer.append("\\u05D9");
            intermediate = IDNA.convertToASCII(inbuffer, IDNA.USE_STD3_RULES); // USE_STD3_RULES,
        }
        return intermediate;
    }


    private void getIcuIdnaUts(StringBuilder inbuffer, TypeAndMap typeAndMapIcu) {
        IDNA icuIdna = IDNA.getUTS46Instance(0);
        IDNA.Info info = new IDNA.Info();

        typeAndMapIcu.type = null;
        typeAndMapIcu.mapping = null;
        try {
            StringBuilder intermediate = convertWithHackUts(inbuffer, icuIdna);
            // DEFAULT
            if (intermediate.length() == 0) {
                typeAndMapIcu.type = IdnaType.ignored;
                typeAndMapIcu.mapping = "";
            } else {
                StringBuilder outbuffer = icuIdna.nameToUnicode(intermediate.toString(), intermediate, info);
                if (!UnicodeUtilities.equals(inbuffer, outbuffer)) {
                    typeAndMapIcu.type = IdnaType.mapped;
                    typeAndMapIcu.mapping = outbuffer.toString();
                } else {
                    typeAndMapIcu.type = IdnaType.valid;
                    typeAndMapIcu.mapping = null;
                }
            }
        } catch (StringPrepParseException e) {
            if (e.getMessage().startsWith("Found zero length")) {
                typeAndMapIcu.type = IdnaType.ignored;
                typeAndMapIcu.mapping = null;
            } else {
                typeAndMapIcu.type = IdnaType.disallowed;
                typeAndMapIcu.mapping = null;
            }
        } catch (Exception e) {
            logln("Failure at: " + Utility.hex(inbuffer));
            typeAndMapIcu.type = IdnaType.disallowed;
            typeAndMapIcu.mapping = null;
        }
    }

    private static StringBuilder convertWithHackUts(StringBuilder inbuffer, IDNA icuIdna) throws StringPrepParseException {
        StringBuilder intermediate;
        try {
            intermediate = icuIdna.nameToASCII(inbuffer.toString(), inbuffer, null); // USE_STD3_RULES,
        } catch (RuntimeException e) {
            if (!e.getMessage().contains("BIDI")) {
                throw e;
            }
            inbuffer.append("\\u05D9");
            intermediate = icuIdna.nameToASCII(inbuffer.toString(), inbuffer, null); // USE_STD3_RULES,
        }
        return intermediate;
    }



    @EnabledIf(value = "org.unicode.unittest.TestFmwkMinusMinus#getRunBroken", disabledReason = "Skip unless UNICODETOOLS_RUN_BROKEN_TEST=true")
    @Test
    public void TestIdnaProps() {
        String map = Idna2003.SINGLETON.mappings.get(0x200c);
        IdnaType type = Idna2003.SINGLETON.getType(0x200c);
        logln("Idna2003\t" + (map == null ? "null" : Utility.hex(map)) + ", " + type);
        map = Uts46.SINGLETON.mappings.get(0x200c);
        type = Uts46.SINGLETON.getType(0x200c);
        logln("Uts46\t" + (map == null ? "null" : Utility.hex(map)) + ", " + type);

        for (int i = 0; i <= 0x10FFFF; ++i) {
            // invariants are:
            // if mapped, then mapped the same
            String map2003 = Idna2003.SINGLETON.mappings.get(i);
            String map46 = Uts46.SINGLETON.mappings.get(i);
            String map2008 = Idna2008.SINGLETON.mappings.get(i);
            IdnaType type2003 = Idna2003.SINGLETON.types.get(i);
            IdnaType type46 = Uts46.SINGLETON.types.get(i);
            IdnaType type2008 = Idna2008.SINGLETON.types.get(i);
            checkNullOrEqual("2003/46", i, type2003, map2003, type46, map46);
            checkNullOrEqual("2003/2008", i, type2003, map2003, type2008, map2008);
            checkNullOrEqual("46/2008", i, type46, map46, type2008, map2008);
        }

        showPropValues(XPropertyFactory.make().getProperty("idna"));
        showPropValues(XPropertyFactory.make().getProperty("uts46"));
    }

    private void checkNullOrEqual(String title, int cp, IdnaType t1, String m1, IdnaType t2, String m2) {
        if (t1 == IdnaType.disallowed || t2 == IdnaType.disallowed) return;
        if (t1 == IdnaType.valid && t2 == IdnaType.valid) return;
        m1 = m1 == null ? UTF16.valueOf(cp) : m1;
        m2 = m2 == null ? UTF16.valueOf(cp) : m2;
        if (m1.equals(m2)) return;
        assertEquals(title + "\t" + Utility.hex(cp), Utility.hex(m1), Utility.hex(m2));
    }

    @Test
    public void TestConfusables() {
        String trial = UnicodeJsp.getConfusables("一万", true, true, true, true);
        logln("***TRIAL0 : " + trial);
        trial = UnicodeJsp.getConfusables("sox", true, true, true, true);
        logln("***TRIAL1 : " + trial);
        trial = UnicodeJsp.getConfusables("sox", 1);
        logln("***TRIAL2 : " + trial);
        //showPropValues(
        XPropertyFactory.make().getProperty("confusable");
        XPropertyFactory.make().getProperty("idr");
    }


    private void showIcuEnums() {
        for (int prop = UProperty.BINARY_START; prop < UProperty.BINARY_LIMIT; ++prop) {
            showEnumPropValues(prop);
        }
        for (int prop = UProperty.INT_START; prop < UProperty.INT_LIMIT; ++prop) {
            showEnumPropValues(prop);
        }
    }

    private void showEnumPropValues(int prop) {
        logln("Property number:\t" + prop);
        for (int nameChoice = 0; ; ++nameChoice) {
            try {
                String propertyName = UCharacter.getPropertyName(prop, nameChoice);
                if (propertyName == null && nameChoice > NameChoice.LONG) {
                    break;
                }
                logln("\t" + nameChoice + "\t" + propertyName);
            } catch (Exception e) {
                break;
            }
        }
        for (int i = UCharacter.getIntPropertyMinValue(prop); i <= UCharacter.getIntPropertyMaxValue(prop); ++i) {
            logln("\tProperty value number:\t" + i);
            for (int nameChoice = 0; ; ++nameChoice) {
                String propertyValueName;
                try {
                    propertyValueName = UCharacter.getPropertyValueName(prop, i, nameChoice);
                    if (propertyValueName == null && nameChoice > NameChoice.LONG) {
                        break;
                    }
                    logln("\t\t"+ nameChoice + "\t" + propertyValueName);
                } catch (Exception e) {
                    break;
                }
            }
        }
    }

    private void showPropValues(UnicodeProperty prop) {
        logln(prop.getName());
        for (Object value : prop.getAvailableValues()) {
            logln(value.toString());
            logln("\t" + prop.getSet(value.toString()).toPattern(false));
        }
    }

    public void checkLanguageLocalizations() {

        Set<String> languages = new TreeSet<String>();
        Set<String> scripts = new TreeSet<String>();
        Set<String> countries = new TreeSet<String>();
        for (ULocale displayLanguage : ULocale.getAvailableLocales()) {
            addIfNotEmpty(languages, displayLanguage.getLanguage());
            addIfNotEmpty(scripts, displayLanguage.getScript());
            addIfNotEmpty(countries, displayLanguage.getCountry());
        }
        Map<ULocale,Counter<Subtag>> canDisplay = new TreeMap<ULocale,Counter<Subtag>>(new Comparator<ULocale>() {
            public int compare(ULocale o1, ULocale o2) {
                return o1.toLanguageTag().compareTo(o2.toString());
            }
        });

        for (ULocale displayLanguage : ULocale.getAvailableLocales()) {
            if (displayLanguage.getCountry().length() != 0) {
                continue;
            }
            Counter<Subtag> counter = new Counter<Subtag>();
            canDisplay.put(displayLanguage, counter);

            final LocaleData localeData = LocaleData.getInstance(displayLanguage);
            final UnicodeSet exemplarSet = new UnicodeSet()
            .addAll(localeData.getExemplarSet(UnicodeSet.CASE, LocaleData.ES_STANDARD));
            final String language = displayLanguage.getLanguage();
            final String script = displayLanguage.getScript();
            if (language.equals("zh")) {
                if (script.equals("Hant")) {
                    exemplarSet.removeAll(Common.simpOnly);
                } else {
                    exemplarSet.removeAll(Common.tradOnly);
                }
            } else {
                exemplarSet.addAll(localeData.getExemplarSet(UnicodeSet.CASE, LocaleData.ES_AUXILIARY));
                if (language.equals("ja")) {
                    exemplarSet.add('ー');
                }
            }
            final UnicodeSet okChars = (UnicodeSet) new UnicodeSet("[[:P:][:S:][:Cf:][:m:][:whitespace:]]").addAll(exemplarSet).freeze();

            Set<String> mixedSamples = new TreeSet<String>();

            for (String code : languages) {
                add(displayLanguage, Subtag.language, code, counter, okChars, mixedSamples);
            }
            for (String code : scripts) {
                add(displayLanguage, Subtag.script, code, counter, okChars, mixedSamples);
            }
            for (String code : countries) {
                add(displayLanguage, Subtag.region, code, counter, okChars, mixedSamples);
            }
            UnicodeSet missing = new UnicodeSet();
            for (String mixed : mixedSamples) {
                missing.addAll(mixed);
            }
            missing.removeAll(okChars);

            final long total = counter.getTotal() - counter.getCount(Subtag.mixed) - counter.getCount(Subtag.fail);
            final String missingDisplay = mixedSamples.size() == 0 ? "" : "\t" + missing.toPattern(false) + "\t" + mixedSamples;
            logln(displayLanguage + "\t" + displayLanguage.getDisplayName(ULocale.ENGLISH)
                    + "\t" + (total/(double)counter.getTotal())
                    + "\t" + total
                    + "\t" + counter.getCount(Subtag.language)
                    + "\t" + counter.getCount(Subtag.script)
                    + "\t" + counter.getCount(Subtag.region)
                    + "\t" + counter.getCount(Subtag.mixed)
                    + "\t" + counter.getCount(Subtag.fail)
                    + missingDisplay
                    );
        }
    }

    private void add(ULocale displayLanguage, Subtag subtag, String code, Counter<Subtag> counter, UnicodeSet okChars, Set<String> mixedSamples) {
        switch (canDisplay(displayLanguage, subtag, code, okChars, mixedSamples)) {
        case code:
            counter.add(Subtag.fail, 1);
            break;
        case localized:
            counter.add(subtag, 1);
            break;
        case badLocalization:
            counter.add(Subtag.mixed, 1);
            break;
        }
    }

    enum Display {code, localized, badLocalization}

    private Display canDisplay(ULocale displayLanguage, Subtag subtag, String code, UnicodeSet okChars, Set<String> mixedSamples) {
        String display;
        switch (subtag) {
        case language:
            display = ULocale.getDisplayLanguage(code, displayLanguage);
            break;
        case script:
            display = ULocale.getDisplayScript("und-" + code, displayLanguage);
            break;
        case region:
            display = ULocale.getDisplayCountry("und-" + code, displayLanguage);
            break;
        default: throw new IllegalArgumentException();
        }
        if (display.equals(code)) {
            return Display.code;
        } else if (okChars.containsAll(display)) {
            return Display.localized;
        } else {
            mixedSamples.add(display);
            UnicodeSet missing = new UnicodeSet().addAll(display).removeAll(okChars);
            return Display.badLocalization;
        }
    }

    private void addIfNotEmpty(Collection<String> languages, String language) {
        if (language != null && language.length() != 0) {
            languages.add(language);
        }
    }

    @Test
    public void TestLanguageTag() {
        String ulocale = "sq";
        assertNotNull("valid list", UnicodeJsp.getLanguageOptions(ulocale));
        assertNoMatch(null, "Ill-Formed", UnicodeJsp.validateLanguageID("zh-yyy", ulocale));
        assertNoMatch(null, "Ill-Formed", UnicodeJsp.validateLanguageID("arb-SU", ulocale));
        assertNoMatch(null, "Ill-Formed", UnicodeJsp.validateLanguageID("xxx-yyy", ulocale));
        assertNoMatch(null, "Ill-Formed", UnicodeJsp.validateLanguageID("zh-cmn", ulocale));
        assertNoMatch(null, "Ill-Formed", UnicodeJsp.validateLanguageID("en-cmn", ulocale));
        assertNoMatch(null, "Ill-Formed", UnicodeJsp.validateLanguageID("eng-cmn", ulocale));
        assertNoMatch(null, "Ill-Formed", UnicodeJsp.validateLanguageID("xxx-cmn", ulocale));
        assertNoMatch(null, "Ill-Formed", UnicodeJsp.validateLanguageID("zh-eng", ulocale));
        assertNoMatch(null, "Ill-Formed", UnicodeJsp.validateLanguageID("eng-eng", ulocale));
        assertNoMatch(null, "Ill-Formed", UnicodeJsp.validateLanguageID("eng-yyy", ulocale));

        assertNoMatch(null, "Ill-Formed", UnicodeJsp.validateLanguageID("gsw-Hrkt-AQ-pinyin-AbCdE-1901-b-fo-fjdklkfj-23-a-foobar-x-1", ulocale));
        assertNoMatch(null, "Ill-Formed", UnicodeJsp.validateLanguageID("fi-Latn-US", ulocale));
        assertNoMatch(null, "Ill-Formed", UnicodeJsp.validateLanguageID("fil-Latn-US", ulocale));
        assertNoMatch(null, "Ill-Formed", UnicodeJsp.validateLanguageID("aaa-Latn-003-FOOBAR-ALPHA-A-xyzw", ulocale));
        assertNoMatch(null, "Ill-Formed", UnicodeJsp.validateLanguageID("aaa-A-xyzw", ulocale));
        assertNoMatch(null, "Ill-Formed", UnicodeJsp.validateLanguageID("x-aaa-Latn-003-FOOBAR-ALPHA-A-xyzw", ulocale));
        assertNoMatch(null, "Ill-Formed", UnicodeJsp.validateLanguageID("aaa-x-Latn-003-FOOBAR-ALPHA-A-xyzw", ulocale));
        assertMatch(null, "invalid\\scode", UnicodeJsp.validateLanguageID("zho-Xxxx-248", ulocale));
        assertMatch(null, "invalid\\sextlang\\scode", UnicodeJsp.validateLanguageID("aaa-bbb", ulocale));
        assertMatch(null, "Ill-Formed", UnicodeJsp.validateLanguageID("aaa--bbb", ulocale));
        assertMatch(null, "Ill-Formed", UnicodeJsp.validateLanguageID("aaa-bbb-abcdefghihkl", ulocale));
        assertMatch(null, "Ill-Formed", UnicodeJsp.validateLanguageID("1aaa-bbb-abcdefghihkl", ulocale));
    }

    public void assertMatch(String message, String pattern, Object actual) {
        assertMatches(message, Pattern.compile(pattern, Pattern.COMMENTS | Pattern.DOTALL), true, actual);
    }

    public void assertNoMatch(String message, String pattern, Object actual) {
        assertMatches(message, Pattern.compile(pattern, Pattern.COMMENTS | Pattern.DOTALL), false, actual);
    }
    //         return handleAssert(expected == actual, message, stringFor(expected), stringFor(actual), "==", false);

    private void assertMatches(String message, Pattern pattern, boolean expected, Object actual) {
        final String actualString = actual == null ? "null" : actual.toString();
        final boolean result = pattern.matcher(actualString).find() == expected;
        handleAssert(result,
                message,
                "/" + pattern.toString() + "/",
                actualString,
                expected ? "matches" : "doesn't match",
                        true);
    }

    @Test
    public void TestATransform() {
        checkCompleteness(enSample, "en-ipa", new UnicodeSet("[a-z]"));
        checkCompleteness(IPA_SAMPLE, "ipa-en", new UnicodeSet("[a-z]"));
        String sample;
        sample = UnicodeJsp.showTransform("en-IPA; IPA-en", enSample);
        //logln(sample);
        sample = UnicodeJsp.showTransform("en-IPA; IPA-deva", "The quick brown fox.");
        //logln(sample);
        String deva = "कँ, कं, कः, ऄ, अ, आ, इ, ई, उ, ऊ, ऋ, ऌ, ऍ, ऎ, ए, ऐ, ऑ, ऒ, ओ, औ, क, ख, ग, घ, ङ, च, छ, ज, झ, ञ, ट, ठ, ड, ढ, ण, त, थ, द, ध, न, ऩ, प, फ, ब, भ, म, य, र, ऱ, ल, ळ, ऴ, व, श, ष, स, ह, ़, ऽ, क्, का, कि, की, कु, कू, कृ, कॄ, कॅ, कॆ, के, कै, कॉ, कॊ, को, कौ, क्, क़, ख़, ग़, ज़, ड़, ढ़, फ़, य़, ॠ, ॡ, कॢ, कॣ, ०, १, २, ३, ४, ५, ६, ७, ८, ९, ।";
        checkCompleteness(IPA_SAMPLE, "ipa-deva", null);
        checkCompleteness(deva, "deva-ipa", null);
    }

    private void checkCompleteness(String testString, String transId, UnicodeSet exceptionsAllowed) {
        String pieces[] = testString.split(",\\s*");
        UnicodeSet shouldNotBeLeftOver = new UnicodeSet().addAll(testString).remove(' ').remove(',');
        if (exceptionsAllowed != null) {
            shouldNotBeLeftOver.removeAll(exceptionsAllowed);
        }
        UnicodeSet allProblems = new UnicodeSet();
        for (String piece : pieces) {
            String sample = UnicodeJsp.showTransform(transId, piece);
            //logln(piece + " => " + sample);
            if (shouldNotBeLeftOver.containsSome(sample)) {
                final UnicodeSet missing = new UnicodeSet().addAll(sample).retainAll(shouldNotBeLeftOver);
                allProblems.addAll(missing);
                warnln("Leftover from " + transId + ": " + missing.toPattern(false));
                Transliterator foo = Transliterator.getInstance(transId, Transliterator.FORWARD);
                //Transliterator.DEBUG = true;
                sample = UnicodeJsp.showTransform(transId, piece);
                //Transliterator.DEBUG = false;
            }
        }
        if (allProblems.size() != 0) {
            warnln("ALL Leftover from " + transId + ": " + allProblems.toPattern(false));
        }
    }

    @Test
    public void TestBidi() {
        String sample;
        sample = UnicodeJsp.showBidi("mark \u05DE\u05B7\u05E8\u05DA\nHelp", 0, true);
        if (!sample.contains(">WS<")) {
            errln(sample);
        }
    }

    @Test
    public void TestMapping() {
        String sample;
        sample = UnicodeJsp.showTransform("(.) > '<' $1 '> ' &hex/perl($1) ', ';", "Hi There.");
        assertContains(sample, "\\x{69}");
        sample = UnicodeJsp.showTransform("lower", "Abcd");
        assertContains(sample, "abcd");
        sample = UnicodeJsp.showTransform("bc > CB; X > xx;", "Abcd");
        assertContains(sample, "ACBd");
        sample = UnicodeJsp.showTransform("lower", "[[:ascii:]{Abcd}]");
        assertContains(sample, "\u00A0A\u00A0");
        sample = UnicodeJsp.showTransform("bc > CB; X > xx;", "[[:ascii:]{Abcd}]");
        assertContains(sample, "\u00A0ACBd\u00A0");
        sample = UnicodeJsp.showTransform("casefold", "[\\u0000-\\u00FF]");
        assertContains(sample, "\u00A0\u00E1\u00A0");

    }

    @Test
    public void TestGrouping() throws IOException {
        StringWriter printWriter = new StringWriter();
        UnicodeJsp.showSet("sc gc", "", UnicodeSetUtilities.parseUnicodeSet("[:subhead=/Syllables/:]"), true, true, true, printWriter);
        assertContains(printWriter.toString(), "General_Category=Letter_Number");
        printWriter.getBuffer().setLength(0);
        UnicodeJsp.showSet("subhead", "", UnicodeSetUtilities.parseUnicodeSet("[:subhead=/Syllables/:]"), true, true, true, printWriter);
        assertContains(printWriter.toString(), "a=A595");
    }

    @Test
    public void TestStuff() throws IOException {
        //int script = UScript.getScript(0xA6E6);
        //int script2 = UCharacter.getIntPropertyValue(0xA6E6, UProperty.SCRIPT);
        String propValue = Common.getXStringPropertyValue(Common.SUBHEAD, 0xA6E6, NameChoice.LONG);
        //logln(propValue);


        //logln("Script for A6E6: " + script + ", " + UScript.getName(script) + ", " + script2);

        try (final PrintWriter printWriter = new PrintWriter(System.out)) {
            //if (true) return;
            UnicodeJsp.showSet("sc gc", "", new UnicodeSet("[[:ascii:]{123}{ab}{456}]"), true, true, true, printWriter);

            UnicodeJsp.showSet("", "", new UnicodeSet("[\\u0080\\U0010FFFF]"), true, true, true, printWriter);
            UnicodeJsp.showSet("", "", new UnicodeSet("[\\u0080\\U0010FFFF{abc}]"), true, true, true, printWriter);
            UnicodeJsp.showSet("", "", new UnicodeSet("[\\u0080-\\U0010FFFF{abc}]"), true, true, true, printWriter);



            String[] abResults = new String[3];
            String[] abLinks = new String[3];
            int[] abSizes = new int[3];
            UnicodeJsp.getDifferences("[:letter:]", "[:idna:]", false, abResults, abSizes, abLinks);
            for (int i = 0; i < abResults.length; ++i) {
                logln(abSizes[i] + "\r\n\t" + abResults[i] + "\r\n\t" + abLinks[i]);
            }

            final UnicodeSet unicodeSet = new UnicodeSet();
            logln("simple: " + UnicodeJsp.getSimpleSet("[a-bm-p\uAc00]", unicodeSet, true, false));
            UnicodeJsp.showSet("", "", unicodeSet, true, true, true, printWriter);


            //    String archaic = "[[\u018D\u01AA\u01AB\u01B9-\u01BB\u01BE\u01BF\u021C\u021D\u025F\u0277\u027C\u029E\u0343\u03D0\u03D1\u03D5-\u03E1\u03F7-\u03FB\u0483-\u0486\u05A2\u05C5-\u05C7\u066E\u066F\u068E\u0CDE\u10F1-\u10F6\u1100-\u115E\u1161-\u11FF\u17A8\u17D1\u17DD\u1DC0-\u1DC3\u3165-\u318E\uA700-\uA707\\U00010140-\\U00010174]" +
            //    "[\u02EF-\u02FF\u0363-\u0373\u0376\u0377\u07E8-\u07EA\u1DCE-\u1DE6\u1DFE\u1DFF\u1E9C\u1E9D\u1E9F\u1EFA-\u1EFF\u2056\u2058-\u205E\u2180-\u2183\u2185-\u2188\u2C77-\u2C7D\u2E00-\u2E17\u2E2A-\u2E30\uA720\uA721\uA730-\uA778\uA7FB-\uA7FF]" +
            //    "[\u0269\u027F\u0285-\u0287\u0293\u0296\u0297\u029A\u02A0\u02A3\u02A5\u02A6\u02A8-\u02AF\u0313\u037B-\u037D\u03CF\u03FD-\u03FF]" +
            //"";
            //UnicodeJsp.showSet("",UnicodeSetUtilities.parseUnicodeSet("[:usage=/.+/:]"), false, false, printWriter);
            UnicodeJsp.showSet("","", UnicodeSetUtilities.parseUnicodeSet("[:hantype=/simp/:]"), false, false, true, printWriter);
        }
    }

    @Test
    public void TestShowProperties() throws IOException {
        StringWriter out = new StringWriter();
        UnicodeJsp.showProperties(0x00C5, out);
        assertTrue("props for character", out.toString().contains("Line_Break"));
        logln(out.toString());
        //logln(out);
    }

    public void TestIdentifiers() throws IOException {
        String out = UnicodeUtilities.getIdentifier("Latin");
        assertTrue("identifier info", out.toString().contains("U+016F"));
        logln(out.toString());
        //logln(out);
    }

    @Test
    public void TestShowSet() throws IOException {
        StringWriter out = new StringWriter();
        //    UnicodeJsp.showSet("sc gc", UnicodeSetUtilities.parseUnicodeSet("[:Hangul_Syllable_Type=LVT_Syllable:]", TableStyle.extras), false, true, out);
        //    assertTrue("props table", out.toString().contains("Hangul"));
        //    logln(out);
        //
        //    out.getBuffer().setLength(0);
        //    UnicodeJsp.showSet("sc gc", UnicodeSetUtilities.parseUnicodeSet("[:cn:]", TableStyle.extras), false, true, out);
        //    assertTrue("props table", out.toString().contains("unassigned"));
        //    logln(out);

        out.getBuffer().setLength(0);
        UnicodeJsp.showSet("sc", "", UnicodeSetUtilities.parseUnicodeSet("[:script=/Han/:]"), false, true,true, out);
        assertFalse("props table", out.toString().contains("unassigned"));
        logln(out.toString());


    }

    @Test
    public void TestParameters() {
        UtfParameters parameters = new UtfParameters("ab%61=%C3%A2%CE%94");
        assertEquals("parameters", "\u00E2\u0394", parameters.getParameter("aba"));
    }

    @Test
    public void TestRegex() {
        final String fix = UnicodeRegex.fix("ab[[:ascii:]&[:Ll:]]*c");
        assertEquals("", "ab[a-z]*c", fix);
        assertEquals("", "<u>abcc</u> <u>abxyzc</u> ab$c", UnicodeJsp.showRegexFind(fix, "abcc abxyzc ab$c"));
    }

    @Test
    public void TestIdna() {
        boolean[] error = new boolean[1];

        String uts46unic = Uts46.SINGLETON.toUnicode("faß.de", error, true);
        logln(uts46unic + ", " + error[0]);
        checkValues(error, Uts46.SINGLETON);
        checkValidIdna(Uts46.SINGLETON, "À｡÷");
        checkInvalidIdna(Uts46.SINGLETON, "≠");
        checkInvalidIdna(Uts46.SINGLETON, "\u0001");
        checkToUnicode(Uts46.SINGLETON, "ß｡ab", "ß.ab");
        //checkToPunyCode(Uts46.SINGLETON, "\u0002", "xn---");
        checkToPunyCode(Uts46.SINGLETON, "ß｡ab", "ss.ab");
        checkToUnicodeAndPunyCode(Uts46.SINGLETON, "faß.de", "faß.de", "fass.de");

        checkValues(error, Idna2003.SINGLETON);
        checkToUnicode(Idna2003.SINGLETON, "ß｡ab", "ss.ab");
        checkToPunyCode(Idna2003.SINGLETON, "ß｡ab", "ss.ab");
        checkValidIdna(Idna2003.SINGLETON, "À÷");
        checkValidIdna(Idna2003.SINGLETON, "≠");

        checkToUnicodeAndPunyCode(Idna2003.SINGLETON, "نامه\u200Cای.de", "نامهای.de", "xn--mgba3gch31f.de");



        checkValues(error, Idna2008.SINGLETON);
        checkToUnicode(Idna2008.SINGLETON, "ß", "ß");
        checkToPunyCode(Idna2008.SINGLETON, "ß", "xn--zca");
        checkInvalidIdna(Idna2008.SINGLETON, "À");
        checkInvalidIdna(Idna2008.SINGLETON, "÷");
        checkInvalidIdna(Idna2008.SINGLETON, "≠");
        checkInvalidIdna(Idna2008.SINGLETON, "ß｡");


        Uts46.SINGLETON.isValid("≠");
        assertTrue("uts46 a", Uts46.SINGLETON.isValid("a"));
        assertFalse("uts46 not equals", Uts46.SINGLETON.isValid("≠"));

        String testLines = UnicodeJsp.testIdnaLines("ΣΌΛΟΣ", "[]");
        assertContains(testLines, "xn--wxaikc6b");
        testLines = UnicodeJsp.testIdnaLines(UnicodeJsp.getDefaultIdnaInput(), "[]");
        assertContains(testLines, "xn--bb-eka.at");


        //showIDNARemapDifferences(printWriter);

        expectError("][:idna=valid:][abc]");

        assertTrue("contains hyphen", UnicodeSetUtilities.parseUnicodeSet("[:idna=valid:]").contains('-'));
    }

    private void checkValues(boolean[] error, Idna idna) {
        checkToUnicodeAndPunyCode(idna, "α.xn--mxa", "α.α", "xn--mxa.xn--mxa");
        checkValidIdna(idna, "a");
        checkInvalidIdna(idna, "=");
    }

    private void checkToUnicodeAndPunyCode(Idna idna, String source, String toUnicode, String toPunycode) {
        checkToUnicode(idna, source, toUnicode);
        checkToPunyCode(idna, source, toPunycode);
    }

    private void checkToUnicode(Idna idna, String source, String expected) {
        boolean[] error = new boolean[1];
        String head = idna.getName() + ".toUnicode, " + source;
        assertEquals(head, expected, idna.toUnicode(source, error, true));
        String head2 = idna.getName() + ".toUnicode error?, " + source + " = " + expected;
        assertFalse(head2, error[0]);
    }

    private void checkToPunyCode(Idna idna, String source, String expected) {
        boolean[] error = new boolean[1];
        String head = idna.getName() + ".toPunyCode, " + source;
        assertEquals(head, expected, idna.toPunyCode(source, error));
        String head2 = idna.getName() + ".toPunyCode error?, " + source + " = " + expected;
        assertFalse(head2, error[0]);
    }

    private void checkInvalidIdna(Idna idna, String value) {
        assertFalse(idna.getName() + ".isValid: " + value, idna.isValid(value));
    }

    private void checkValidIdna(Idna idna, String value) {
        assertTrue(idna.getName() + ".isValid: " + value, idna.isValid(value));
    }

    public void expectError(String input) {
        try {
            UnicodeSetUtilities.parseUnicodeSet(input);
            errln("Failure to detect syntax error.");
        } catch (IllegalArgumentException e) {
            logln("Expected error: " + e.getMessage());
        }
    }

    @Test
    public void TestBnf() {
        UnicodeRegex regex = new UnicodeRegex();
        final String[][] tests = {
                {
                    "c = a* wq;\n" +
                            "a = xyz;\n" +
                            "b = a{2} c;\n"
                },
                {
                    "c = a* b;\n" +
                            "a = xyz;\n" +
                            "b = a{2} c;\n",
                            "Exception"
                },
                {
                    "uri = (?: (scheme) \\:)? (host) (?: \\? (query))? (?: \\u0023 (fragment))?;\n" +
                            "scheme = reserved+;\n" +
                            "host = \\/\\/ reserved+;\n" +
                            "query = [\\=reserved]+;\n" +
                            "fragment = reserved+;\n" +
                            "reserved = [[:ascii:][:sc=grek:]&[:alphabetic:]];\n",
                "http://αβγ?huh=hi#there"},
//                {
//                    "/Users/markdavis/Documents/workspace/cldr/tools/java/org/unicode/cldr/util/data/langtagRegex.txt"
//                }
        };
        for (int i = 0; i < tests.length; ++i) {
            String test = tests[i][0];
            final boolean expectException = tests[i].length < 2 ? false : tests[i][1].equals("Exception");
            try {
                String result;
                if (test.endsWith(".txt")) {
                    List<String> lines = UnicodeRegex.loadFile(test, new ArrayList<String>());
                    result = regex.compileBnf(lines);
                } else {
                    result = regex.compileBnf(test);
                }
                if (expectException) {
                    errln("Expected exception for " + test);
                    continue;
                }
                String result2 = result.replaceAll("[0-9]+%", ""); // just so we can use the language subtag stuff
                String resolved = regex.transform(result2);
                //logln(resolved);
                Matcher m = Pattern.compile(resolved, Pattern.COMMENTS).matcher("");
                String checks = "";
                for (int j = 1; j < tests[i].length; ++j) {
                    String check = tests[i][j];
                    if (!m.reset(check).matches()) {
                        checks = checks + "Fails " + check + "\n";
                    } else {
                        for (int k = 1; k <= m.groupCount(); ++k) {
                            checks += "(" + m.group(k) + ")";
                        }
                        checks += "\n";
                    }
                }
                //logln("Result: " + result + "\n" + checks + "\n" + test);
                String randomBnf = UnicodeJsp.getBnf(result, 10, 10);
                //logln(randomBnf);
            } catch (Exception e) {
                if (!expectException) {
                    errln(e.getClass().getName() + ": " + e.getMessage());
                }
                continue;
            }
        }
    }

    @Test
    public void TestBnfMax() {
        BNF bnf = new BNF(new Random(), new Quoter.RuleQuoter());
        bnf.setMaxRepeat(10)
        .addRules("$root=[0-9]+;")
        .complete();
        for (int i = 0; i < 100; ++i) {
            String s = bnf.next();
            assertTrue("Max too large? " + i + ", " + s.length() + ", " + s, 1 <= s.length() && s.length() < 11);
        }
    }

    @Test
    public void TestBnfGen() {
        if (logKnownIssue("x", "old test disabling for now")) {
            return;
        }
        String stuff = UnicodeJsp.getBnf("([:Nd:]{3} 90% | abc 10%)", 100, 10);
        assertContains(stuff, "<p>\\U0001D7E8");
        stuff = UnicodeJsp.getBnf("[0-9]+ ([[:WB=MB:][:WB=MN:]] [0-9]+)?", 100, 10);
        assertContains(stuff, "726283663");
        String bnf = "item = word | number;\n" +
                "word = $alpha+;\n" +
                "number = (digits (separator digits)?);\n" +
                "digits = [:Pd:]+;\n" +
                "separator = [[:WB=MB:][:WB=MN:]];\n" +
                "$alpha = [:alphabetic:];";
        String fixedbnf = new UnicodeRegex().compileBnf(bnf);
        String fixedbnf2 = UnicodeRegex.fix(fixedbnf);
        //String fixedbnfNoPercent = fixedbnf2.replaceAll("[0-9]+%", "");
        String random = UnicodeJsp.getBnf(fixedbnf2, 100, 10);
        //assertContains(random, "\\U0002A089");
    }

    @Test
    public void TestSimpleSet() {
        checkUnicodeSetParseContains("[a-z\u00e4\u03b1]", "\\p{idna2003=valid}");
        checkUnicodeSetParseContains("[a-z\u00e4\u03b1]", "\\p{idna=valid}");
        checkUnicodeSetParseContains("[a-z\u00e4\u03b1]", "\\p{uts46=valid}");
        checkUnicodeSetParseContains("[a-z\u00e4\u03b1]", "\\p{idna2008=PVALID}");
        checkUnicodeSetParse("[\\u1234\\uABCD-\\uAC00]", "U+1234 U+ABCD-U+AC00");
        checkUnicodeSetParse("[\\u1234\\uABCD-\\uAC00]", "U+1234 U+ABCD..U+AC00");
    }

    private void checkUnicodeSetParse(String expected1, String test) {
        UnicodeSet actual = new UnicodeSet();
        UnicodeSet expected = new UnicodeSet(expected1);
        UnicodeJsp.getSimpleSet(test, actual , true, false);
        assertEquals(test, expected, actual);
    }
    private void checkUnicodeSetParseContains(String expected1, String test) {
        UnicodeSet actual = new UnicodeSet();
        UnicodeSet expectedSubset = new UnicodeSet(expected1);
        UnicodeJsp.getSimpleSet(test, actual , true, false);
        assertContains(test, expectedSubset, actual);
    }

    @Test
    public void TestConfusable() {
        String test = "l l l l o̸ ä O O v v";
        String string = UnicodeJsp.showTransform("confusable", test);
        assertEquals(null, test, string);
        string = UnicodeJsp.showTransform("confusableLower", test);
        assertEquals(null, test, string);
        String listedTransforms = UnicodeJsp.listTransforms();
        if (!listedTransforms.contains("confusable")) {
            errln("Missing 'confusable' " + listedTransforms);
        }
    }
}
