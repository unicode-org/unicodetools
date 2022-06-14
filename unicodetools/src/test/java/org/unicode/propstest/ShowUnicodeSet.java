package org.unicode.propstest;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.locale.XCldrStub.Splitter;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CLDRTransforms;
import org.unicode.cldr.util.CLDRTransforms.ParsedTransformID;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Factory;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Binary;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.props.UcdPropertyValues.Script_Values;
import org.unicode.text.utility.Utility;

public class ShowUnicodeSet {
    private static final String START = "⌜";
    private static final String END = "⌝";
    private static UnicodeSet BLANKS =
            new UnicodeSet(
                            "[\\p{defaultignorablecodepoint}\\p{whitespace}\\p{Z}\\p{Cc}\\p{Cf}-\\p{Cn}-\\p{block=Tags}-[\\x{20}\\x{600}-\\x{603}]]")
                    .freeze();

    public static void main(String[] args) {
        // genData(args);
        System.out.println("Blanks: " + BLANKS);

        Factory factory = CLDRConfig.getInstance().getMainAndAnnotationsFactory();
        Collection<String> locales = factory.getAvailable(); // Arrays.asList("fr", "he");
        locales.parallelStream()
                .forEach(
                        localeStr -> {
                            CLDRFile locale = factory.make(localeStr, false);
                            Counter<Integer> blankCount = new Counter<>();
                            for (String path : locale) {
                                String value = locale.getStringValue(path);
                                if (!BLANKS.containsSome(value)) {
                                    continue;
                                }
                                value.codePoints()
                                        .forEach(
                                                x -> {
                                                    if (BLANKS.contains(x)) {
                                                        blankCount.add(x, 1);
                                                    }
                                                });
                            }
                            if (blankCount.size() != 0) {
                                for (Integer cp : blankCount) {
                                    System.out.println(
                                            new StringBuffer()
                                                    .append(localeStr)
                                                    .append('\t')
                                                    .append(blankCount.get(cp))
                                                    .append("\t\\x{")
                                                    .append(Utility.hex(cp))
                                                    .append('}')
                                                    .append("\t ")
                                                    .appendCodePoint(cp)
                                                    .append(' ')
                                                    .append('\t')
                                                    .append(iup.getName(cp)));
                                }
                            }
                        });
    }

    static Normalizer2 nfc = Normalizer2.getNFCInstance();
    static IndexUnicodeProperties iup = IndexUnicodeProperties.make();

    public static void genData(String[] args) {

        String rules =
                CLDRTransforms.getIcuRulesFromXmlFile(
                        CLDRPaths.COMMON_DIRECTORY + "transforms/",
                        "Any-Superscript.xml",
                        new ParsedTransformID());
        Transliterator toSuperscript =
                Transliterator.createFromRules("any-superscript", rules, Transliterator.FORWARD);
        //        for (Transliterator element : toSuperscript.getElements()) {
        //            System.out.println(element.getID());
        //            if (element instanceof RuleBasedTransliterator) {
        //            }
        //        }
        StringBuilder buffer = new StringBuilder();
        toSuperscript
                .getSourceSet()
                .forEach(
                        x -> {
                            if (nfc.isNormalized(x) && !toSuperscript.transform(x).equals(x)) {
                                buffer.append(x);
                            }
                        });
        System.out.println(buffer);
        System.out.println(toSuperscript.transform(buffer.toString()));

        System.out.println(Arrays.asList(args));
        Normalizer2 nfkc = Normalizer2.getNFKCInstance();

        //        for (String s : new UnicodeSet("[[:Lm:][:dt=super:]]")) {
        //
        //            final String norm = nfkc.normalize(s);
        //            if (!norm.equals(s)) {
        //                System.out.println(norm + "\t" + s + "\t" + iup.getName(s, " + "));
        //            }
        //        }

        UnicodeSet us;
        try {
            us = new UnicodeSet(args[0]);
        } catch (Exception e) {
            //            e.printStackTrace();
            //            for (UcdProperty prop : iup.getAvailableUcdProperties()) {
            //                System.out.println(prop);
            //            }
            //            for (  Block_Values pv : UcdPropertyValues.Block_Values.values()) {
            //                System.out.println(pv);
            //            }
            us =
                    new UnicodeSet(
                            "[\\p{defaultignorablecodepoint}\\p{Z}\\p{whitespace}\\p{Cc}\\p{Cf}-[\\u0020]-\\p{Cn}-\\p{block=Tags}-\\p{Deprecated}]");
        }
        UnicodeMap<String> nameAlias = iup.load(UcdProperty.Name_Alias);
        UnicodeMap<General_Category_Values> genCat = iup.loadEnum(UcdProperty.General_Category);
        UnicodeMap<Binary> dep = iup.loadEnum(UcdProperty.Deprecated);
        UnicodeMap<Set<Script_Values>> script =
                iup.loadEnumSet(UcdProperty.Script_Extensions, Script_Values.class);

        Ordering<String> byLength =
                new Ordering<String>() {
                    public int compare(String left, String right) {
                        return ComparisonChain.start()
                                .compare(left.length(), right.length())
                                .compare(left, right)
                                .result();
                    }
                };

        //         final Transliterator SUPERSCRIPT = Transliterator.createFromRules(
        //                "any-show", "[\\p{dicp}\\p{Z}\\p{whitespace}]-\\p{ascii}] > &hex($0)",
        // Transliterator.FORWARD);

        String[][] fixes = {
            {"EN SPACE", "ensp"},
            {"EM SPACE", "emsp"},
            {"THIN SPACE", "thsp"},
            {"HAIR SPACE", "hrsp"},
            {"FIGURE SPACE", "figsp"},
            {"INVISIBLE PLUS", "iplus"},
            {"SIX-PER-EM SPACE", "em6sp"},
            {"INVISIBLE TIMES", "itimes"},
            {"FOUR-PER-EM SPACE", "em4sp"},
            {"THREE-PER-EM SPACE", "em3sp"},
            {"PUNCTUATION SPACE", "pncsp"},
            {"IDEOGRAPHIC SPACE", "idsp"},
            {"INVISIBLE SEPARATOR", "isep"},
            {"FUNCTION APPLICATION", "fncap"},
        };
        Map<String, String> fixMap = new HashMap<>();
        for (String[] row : fixes) {
            fixMap.put(row[0], row[1]);
        }

        for (String s : us) {
            String aliases = nameAlias.get(s);
            String bestName = null;
            if (aliases == null) {
                bestName = iup.getName(s, " + ");
                if (bestName == null) {
                    bestName = "u" + Utility.hex(s, 1);
                }
            } else {
                Set<String> sorted = new TreeSet<>(byLength);
                sorted.addAll(Splitter.on(';').trimResults().splitToList(aliases));
                bestName = sorted.iterator().next();
            }
            String fix = fixMap.get(bestName);
            if (fix != null) {
                bestName = fix;
            }
            bestName = START + bestName + END;
            final Binary isDep = dep.get(s);
            if (isDep == Binary.Yes) {
                continue;
            }
            System.out.println(
                    "\\u{"
                            + Utility.hex(s, 1)
                            + "} ↔︎ "
                            + "\t"
                            + toSuperscript.transform(bestName.toLowerCase(Locale.ENGLISH))
                            + " ;"
                            + "\t#"
                            + "\t"
                            + genCat.get(s)
                            + "\t"
                            + isDep
                            + "\t"
                            + script.get(s)
                            + "\t"
                            + iup.getName(s, " + "));
        }
    }
}
