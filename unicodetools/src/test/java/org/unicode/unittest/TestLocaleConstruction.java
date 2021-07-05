package org.unicode.unittest;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.util.ICUException;
import com.ibm.icu.util.ULocale;

public class TestLocaleConstruction extends TestFmwk {
    private static final boolean DEBUG = false;

    public static final Splitter SEP_SPLITTER = Splitter.on(CharMatcher.anyOf("-_"));
    public static final Joiner SEP_JOIN = Joiner.on('-');
    public static final Joiner SPACE_JOINER = Joiner.on(' ');
    public static final Splitter SPACE_SPLITTER = Splitter.on(' ').trimResults().omitEmptyStrings();
    public static final Splitter EQUAL_SPLITTER = Splitter.on('=').trimResults().omitEmptyStrings();
    public static final Joiner SEMI_JOINER = Joiner.on("; ");

    public static void main(String[] args) {
        new TestLocaleConstruction().run(args);
    }
    static final String[] tests = {
            "title=checkAttr setLanguage=en addUnicodeLocaleAttribute=attr setUnicodeLocaleKeyword=nu,thai",
            "store=plain setLanguageTag=en-u-attr-nu-thai equals=plain",

            "setLanguageTag=en-a-bc-t-en-u-ca-foo-nu-bcd-xx-abc",
            "toLanguageTag=en-a-bc-t-en-u-ca-foo-nu-bcd-xx-abc",

            "arg=en_latn arg=DE",
            "toString=en_Latn_DE",

            "title=checkVariantOrder setLanguage=en setVariant=defgh-abcde",
            "toLanguageTag=en-abcde-defgh",
    };

//    public void oldTestLocaleMatcherSpeed() {
//        Set<ULocale> supportedLocales = localeListStringToULocales("en, de, en-GB");
//        Set<ULocale> desiredLocales = localeListStringToULocales("en-IN, fr");
//        Output<ULocale> bestDesired = new Output<ULocale>();
//        XLocaleMatcher.Builder builder = new XLocaleMatcher.Builder();
//        XLocaleMatcher matcher;
//        ULocale bestSupported = null;
//
//        for (int i = 0; i < 1000; ++i) {
//            matcher = builder.setSupportedLocales(supportedLocales).build();
//            bestSupported = matcher.getBestMatch(desiredLocales, bestDesired);
//        }
//        System.out.println("\nsupported: " + localeListStringToULocales(supportedLocales)
//        + "\ndesired: " + localeListStringToULocales(desiredLocales)
//        + "\nbest-supported: " + bestSupported.toLanguageTag()
//        + "\nbest-desired: " + bestDesired.value.toLanguageTag());
//
//
//        Timer t = new Timer();
//        matcher = builder.setSupportedLocales(supportedLocales).build();
//        bestSupported = matcher.getBestMatch(desiredLocales, bestDesired);
//        long setup = t.stop();
//
//        int iterations = 1000000;
//        t.start();
//        for (int i = 0; i < iterations; ++i) {
//            matcher = builder.setSupportedLocales(supportedLocales).build();
//            bestSupported = matcher.getBestMatch(desiredLocales, bestDesired);
//        }
//        long time = t.stop();
//        System.out.println("\ntime:" + (time/iterations) + "ns" 
//                + ", setup: " + setup + "ns");
//    }


    private Set<ULocale> localeListStringToULocales(String localeListString) {
        String localeList = localeListString;
        Set<ULocale> temp = new LinkedHashSet<>();
        for (String locale : localeList.split(",\\s*|\\s+")) {
            temp.add(ULocale.forLanguageTag(locale));
        }
        return ImmutableSet.copyOf(temp);
    }

    private String localeListStringToULocales(Set<ULocale> locales) {
        StringBuilder result = new StringBuilder();
        for (ULocale locale : locales) {
            if (result.length() != 0) {
                result.append(", ");
            }
            result.append(locale.toLanguageTag());
        }
        return result.toString();
    }


    public void TestLocalesJava() {
        CheckLocaleState jLocaleTester = new CheckLocaleState(new JLocaleShim(Locale.class, Locale.Builder.class));
        int lineCount = 0;
        for (String line : tests) {
            jLocaleTester.handleLine(++lineCount, line);
        }
        //new Locale.Builder().setUnicodeLocaleKeyword("a", "b");
    }

    public void TestLocalesICU() {
        CheckLocaleState uLocaleTester = new CheckLocaleState(new JLocaleShim(ULocale.class, ULocale.Builder.class));
        int lineCount = 0;
        for (String line : tests) {
            uLocaleTester.handleLine(++lineCount, line);
        }
    }

    public class CheckLocaleState<L,S extends LocaleShim<L>> {
        S shim;
        boolean tested = true;
        String title = "";
        Multimap<String, String> commandArgs = LinkedListMultimap.create();

        CheckLocaleState(S shim) {
            this.shim = shim;
        }

        public void handleLine(int lineCount, String line) {
            List<String> commands = SPACE_SPLITTER.splitToList(line);
            for (String commandString : commands) {
                handleCommand(lineCount, commandString);
            }            
        }

        String getBuildArgs() {
            StringBuilder result = new StringBuilder();
            for (Entry<String, String> entry : commandArgs.entries()) {
                if (result.length() != 0) {
                    result.append(' ');
                }
                result.append(entry.getKey() + "=" + entry.getValue());
            }
            return result.toString();
        }

        public void handleCommand(int lineCount, String commandString) {
            List<String> parts = EQUAL_SPLITTER.splitToList(commandString);
            String operator = parts.get(0);
            String parameter = parts.get(1);
            switch (OperatorType.from(operator)) {
            case title:
                title = parameter + ": ";
                break;
            case build:
                if (tested) {
                    tested = false;
                    commandArgs.clear();
                }
                commandArgs.put(operator, parameter);
                break;
            case store: 
                if (!tested) {
                    tested = true;
                    shim.buildLocale(commandArgs);
                }
                shim.setStored(parameter);
                break;
            case test:
                if (!tested) {
                    tested = true;
                    shim.buildLocale(commandArgs);
                }
                assertEquals(lineCount + ") " + getBuildArgs(), parameter, shim.getValue(operator));
                break;
            case compare:
                if (!tested) {
                    tested = true;
                    shim.buildLocale(commandArgs);
                }
                assertEquals(lineCount + ") " + getBuildArgs(), shim.getStored(parameter), shim.getStored(""));
                break;
            }
        }
    }

    enum OperatorType {
        build, test, store, compare, title;
        static OperatorType from(String operator) {
            return "arg".equals(operator) || operator.startsWith("set") || operator.startsWith("add") ? OperatorType.build
                    : operator.equals("store") ? OperatorType.store
                            : operator.equals("equals")  ? OperatorType.compare
                                    : operator.equals("title") ? OperatorType.title
                                            : OperatorType.test;
        }
    }

    abstract class LocaleShim<L> {
        L locale = null;
        Map<String,L> stored = new HashMap<>();
        Multimap<String, String> buildArgs;
        abstract String getValue(String testCommand);
        abstract void buildLocale(Multimap<String,String> args);
        public void setStored(String name) {
            stored.put(name, locale);
        }
        public L getStored(String name) {
            return name.isEmpty() ? locale : stored.get(name);
        }
    }

    public class JLocaleShim<L, B, LC extends Class<L>, BC extends Class<B>> extends LocaleShim<L> {
        private final Class<?> localeClass;
        private Class<?> builderClass;

        JLocaleShim(LC localeClass, BC builderClass) {
            this.localeClass = localeClass;
            this.builderClass = builderClass;
        }

        @Override
        public String getValue(String testCommand) {
            try {
                Method method = localeClass.getMethod(testCommand);
                return (String) method.invoke(locale);
            } catch (Exception e) {
                throw new ICUException(e);
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        void buildLocale(Multimap<String, String> args) {
            try {
                buildArgs = args;
                Map<String, Collection<String>> map = args.asMap();
                Collection<String> argValues = args.get("arg");
                if (!argValues.isEmpty()) {
                    List<String> argList = new ArrayList<>(argValues);
                    try {
                        Constructor<?> method;
                        switch (argList.size()) {
                        case 1: 
                            locale = (L) localeClass.getConstructor(String.class)
                            .newInstance(argList.get(0));
                            break;
                        case 2: 
                            locale = (L) localeClass.getConstructor(String.class, String.class)
                            .newInstance(argList.get(0), argList.get(1));
                            break;
                        case 3: 
                            locale = (L) localeClass.getConstructor(String.class, String.class, String.class)
                            .newInstance(argList.get(0), argList.get(1), argList.get(1));
                            break;
                        }
                    } catch (Exception e) {
                        throw new ICUException(e);
                    }
                } else {
                    B builder = (B) builderClass.getConstructor().newInstance();
                    for (Entry<String, Collection<String>> entry : map.entrySet()) {
                        List<String> argList = new ArrayList<>(entry.getValue());
                        String key = entry.getKey();
                        switch(key) {
                        case "setExtension":
                            builderClass.getMethod(key, Character.class, String.class)
                            .invoke(builder, argList.get(0).charAt(0), argList.get(1));
                            break;
                        case "setUnicodeLocaleKeyword":
                            String[] keyValue = argList.get(0).split(",");
                            builderClass.getMethod(key, String.class, String.class)
                            .invoke(builder, keyValue[0], keyValue[1]);
                            break;
                        default:            
                            builderClass.getMethod(key, String.class)
                            .invoke(builder, argList.get(0));
                        }
                    }
                    locale = (L) builderClass.getMethod("build").invoke(builder);
                }
            } catch (Exception e) {
                throw new ICUException(e);
            }
        }
    }

//    static final LocaleValidityChecker validity = new LocaleValidityChecker(EnumSet.allOf(Datasubtype.class));
//
//    public void TestLocales () {
//        // examples: aay, bcc => bal; Abcd, none; AB, BU => MM; HEPLOC => ALALC97
//        // example special cases: context and/or reset other fields
//        //  sgn_BE_NL => vgt // replaces language if regions are BE or NL
//        //  cnr => sr_ME
//        //  SU => RU AM AZ BY EE GE KZ KG LV LT MD TJ TM UA UZ // 
//        //  aaland => AX // removes variant, sets region if exists
//        // special cases
//        check(validity, "a", false);
//        check(validity, "$", false);
//        check(validity, "еѕ", false);
//
//        check(validity, "he-IL", true);
//        check(validity, "aay-IL", false);
//        check(validity, "iw-Latn-IL", true);
//        check(validity, "iw-Abcd-IL", false);
//        check(validity, "iw-Latn-AB", false);
//        check(validity, "en-fonipa-heploc", true);
//    }
//
//    public static final Map<Datatype, Map<Datasubtype, ValiditySet>> validityInfo = ValidIdentifiers.getData();
//
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
//
////    private static void checkCodes(Datatype dataType) {
////        if (DEBUG) System.out.println("\n" + dataType);
////        AliasesFull aliases = new AliasesFull(dataType);
////        Output<Collection<ExceptionInfo>> exception = new Output<>();
////
////        for (Entry<Datasubtype, ValiditySet> entry : validityInfo.get(dataType).entrySet()) {
////            for (String code : entry.getValue().regularData) {
////                String replacement = aliases.getCanonical(
////                        dataType == Datatype.region || dataType == Datatype.variant? code.toUpperCase(Locale.ROOT) 
////                                : code,
////                                exception);
////                if (replacement != null) {
////                    if (DEBUG) System.out.println(code + " ==> " + replacement);
////                } else if (exception.value != null){
////                    if (DEBUG) System.out.println(code + " ==> " + exception.toString());
////                }
////            }
////        }
////    }
//
//    private void check(LocaleValidityChecker validity, String localeString, 
//            boolean expectedValidity) {
//        ULocale locale;
//        try {
//            locale = new ULocale.Builder().setLanguageTag(localeString).build();
//        } catch (Exception e) {
//            assertEquals("«" + localeString + "»"
//                    + " Syntax error: " + e.getMessage(), expectedValidity, false);
//            return;
//        }
//        Where where = new Where();
//        boolean isValid = validity.isValid(locale, where);
//        assertEquals("«" + localeString + "»" + where, expectedValidity, isValid);
//    }
}
