package org.unicode.tools.emoji;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.tool.LanguageCodeConverter;
import org.unicode.cldr.util.Annotations;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.WinningChoice;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.SimpleHtmlParser;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SimpleHtmlParser.Type;
import org.unicode.cldr.util.TransliteratorUtilities;
import org.unicode.props.UnicodeRelation;
import org.unicode.text.utility.Utility;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class ParseSpreadsheetAnnotations {

    private static final String SOURCE_DIR = CLDRPaths.DATA_DIRECTORY + "emoji/annotations_import";
    private static final String TARGET_DIR = CLDRPaths.GEN_DIRECTORY + "emoji/annotations/";

    enum TableState {
        NEED_CHARACTER_CODE, NEED_CODE
    }

    static final Splitter BAR = Splitter.onPattern("[।|၊/]").trimResults().omitEmptyStrings();
    static final Joiner BAR_JOINER = Joiner.on(" | ");
    private static final String DEBUG_CODEPOINT = new StringBuilder().appendCodePoint(0x1f951).toString();
    //private static UnicodeSet OK_CODEPOINTS = new UnicodeSet("[\\-[:L:][:M:][:Nd:]\\u0020/|“”„’,.\u200C\u200D\u05F4\u104D\u2018]").freeze();
    private static Matcher SPACES = Pattern.compile("[\\s\u00A0\u00B7]+").matcher("");
    private static UnicodeSet OK_IN_JOINER_SEQUENCES = new UnicodeSet("[\\:,\u060C()]").freeze();

    enum ItemType {short_name, keywords};
    static Normalizer2 NFC = Normalizer2.getNFCInstance();

    static class LocaleInfo {
        final String locale;
        final UnicodeSet okNameCharacters;
        final UnicodeSet okKeywordCharacters;
        final UnicodeRelation<String> errors = new UnicodeRelation<>();

        public LocaleInfo(String localeName) {
            locale = localeName;
            final CLDRFile cldrFile = CLDRConfig.getInstance().getCldrFactory().make(localeName, true);
            okNameCharacters = new UnicodeSet("[[:Nd:]\\u0020+-]")
            .addAll(cldrFile.getExemplarSet("", WinningChoice.WINNING))
            .addAll(cldrFile.getExemplarSet("auxiliary", WinningChoice.WINNING))
            .addAll(cldrFile.getExemplarSet("punctuation", WinningChoice.WINNING))
            .freeze();
            okKeywordCharacters = new UnicodeSet(okNameCharacters).add('|').freeze();
        }

        UnicodeSet getCheckingSet(ItemType itemType) {
            return itemType == ItemType.keywords ? okKeywordCharacters : okNameCharacters;
        }

        private String check(String codePoint, ItemType itemType, String item) {
            item = SPACES.reset(TransliteratorUtilities.fromHTML.transform(item.trim())).replaceAll(" ");
            item = NFC.normalize(item);
            // hacks
            item = item.replace("'", "’")
                    .replace("סכו\"ם", "סכו\u05F4ם")
                    .replace(", |", " |")
                    .replace("\"сөз", "сөз")
                    .replace("жок\"", "жок")
                    .replace("\"мага чал\"", "“мага чал”")
                    .replace("\"нет слов\"", "“нет слов”")
                    .replace("\"më telefono\"", "“më telefono”")
                    .replace("\"ndalo\"", "“ndalo”")
                    .replace("\"nipigie simu\"", "“nipigie simu”")
                    .replace("\"стоп\"", "“стоп”")
                    ;
            final UnicodeSet okCharacters = getCheckingSet(itemType);
            if (!okCharacters.containsAll(item)) {
                final UnicodeSet badCodePoints = new UnicodeSet().addAll(item).removeAll(okCharacters);
                if (codePoint.contains(Emoji.JOINER_STRING)) {
                    badCodePoints.removeAll(OK_IN_JOINER_SEQUENCES);
                }
                if (!badCodePoints.isEmpty()) {
//                    Set<String> chars = new TreeSet<>();
//                    Set<String> names = new LinkedHashSet<>();
//                    for (String s : badCodePoints) {
//                        chars.add(s);
//                        names.add(Utility.hex(s, "+") + " ( " + s + " ) " + UCharacter.getName(s, " + "));
//                    }
                    errors.addAll(badCodePoints, locale + "\t" + itemType + "\t«" + item);
                }
            }
            return item;
        }
    }

    static class NewAnnotation {
        final String shortName;
        final String keywords;
        public NewAnnotation(LocaleInfo localeInfo, String codePoint, String shortName, String keywords) {
            shortName = localeInfo.check(codePoint, ItemType.short_name, shortName);
            keywords = localeInfo.check(codePoint, ItemType.keywords, BAR_JOINER.join(BAR.splitToList(keywords)));
            this.shortName = shortName.isEmpty() ? null : shortName;
            this.keywords = keywords.isEmpty() ? null : keywords;
        }
        @Override
        public String toString() {
            return shortName + " | " + keywords;
        }
    }

    public static void main(String[] args) throws IOException {

        UnicodeMap<Annotations> englishAnnotations = Annotations.getData("en");
        Map<String,UnicodeMap<NewAnnotation>> localeToNewAnnotations = new TreeMap<>();
        Map<String,Map<String,String>> localeToLabels = new TreeMap<>();
        Set<String> status = new LinkedHashSet<>();
        Set<String> badLocales = new LinkedHashSet<>();

        /*
         * <tr style='height: 20px;'> <th id="515027226R2" style="height: 20px;"
         * class="row-headers-background"><div class="row-header-wrapper"
         * style="line-height: 20px;">3</div></th> #1<td class="s4">U+1f920</td>
         * <td class="s5"><div style='width: 100px; height: 20px; background:
         * url
         * (//images-docs-opensocial.googleusercontent.com/gadgets/proxy?url=http
         * :
         * //unicode.org/draft/reports/tr51/images/android/android_1f920.png&amp
         * ;
         * container=docs&amp;gadget=docs&amp;rewriteMime=image/*&amp;resize_h=36
         * &amp;resize_w=36) no-repeat center top' /></td> <td class="s6">cowboy
         * hat face</td> #4<td class="s7" dir="ltr">Smiley mit Cowboyhut</td>
         * <td class="s6">cowboy | face | hat | cowgirl</td> #6<td class="s7"
         * dir="ltr">Cowboy | Gesicht | Hut</td> <td class="s6"></td> <td
         * class="s7"></td> </tr>
         */
        Set<String> skipNames = ImmutableSet.of("Internal", "Sheet", "Summary", "Template", "Duplicates", "Counts");
        Set<String> currentAnnotations = Annotations.getAvailable();

        final Set<String> inclusions = null; // ImmutableSet.of("be", "bs", "cy", "eu", "ga", "gl", "zu"); // en-GB, es-419, fr-ca,  zh-HK (yue), 
        Set<LocaleInfo> errors = new LinkedHashSet<>();

        fileLoop: for (File file : new File(SOURCE_DIR).listFiles()) {
            String name = file.getName();
            if (!name.endsWith(".html")) {
                continue;
            }
            final String coreName = name.substring(0,name.length()-5);
            if (inclusions != null && !inclusions.contains(coreName)) {
                continue;
            }
            String localeName = LanguageCodeConverter.fromGoogleLocaleId(coreName);

            for (String skipName : skipNames) {
                if (name.startsWith(skipName)) {
                    continue fileLoop;
                }
            }
            Map<String,String> labels = new LinkedHashMap<>();
            LocaleInfo localeInfo = new LocaleInfo(localeName);
            errors.add(localeInfo);

            UnicodeMap<NewAnnotation> newAnnotations = readFile(file.getParent(), name, localeInfo, labels);

            if (newAnnotations.isEmpty()) {
                System.out.println("\t" + localeName + "\tempty, skipping");
                continue;
            } 
            System.out.println("\t" + localeName + "\tsize:\t" + newAnnotations.size());
            if (!currentAnnotations.contains(localeName)) {
                System.out.println("\t" + localeName + "\tbad locale name");
                badLocales.add(localeName);
            } else {
                localeToNewAnnotations.put(localeName, newAnnotations.freeze());
                localeToLabels.put(localeName, ImmutableMap.copyOf(labels));

                UnicodeMap<Annotations> oldData = Annotations.getData(localeName);
                Multimap<String,String> nameToCode = TreeMultimap.create();
                for (Entry<String, Annotations> item : oldData.entrySet()) {
                    final String key = item.getValue().tts;
                    if (key == null) continue;
                    nameToCode.put(key, item.getKey());
                }
                for (Entry<String, NewAnnotation> item : newAnnotations.entrySet()) {
                    String code = item.getKey();
                    final String key = item.getValue().shortName;
                    final String keywords = item.getValue().keywords;
                    if (key == null || keywords == null) {
                        status.add(localeName + "\tmissing:\t" + code + "\tenglish:" + englishAnnotations.get(code));
                        continue;
                    }
                    nameToCode.put(key, item.getKey());
                }
                for (Entry<String, NewAnnotation> item : newAnnotations.entrySet()) {
                    final String key = item.getValue().shortName;
                    if (key == null) continue;
                    Collection<String> codes = nameToCode.get(key);
                    if (codes.size() != 1) {
                        StringBuilder ss = new StringBuilder(localeName);
                        for (String code : codes) {
                            final Annotations englishVersion = englishAnnotations.get(code);
                            if (englishVersion == null) {
                                ss.append("\tBad code: «" + code + "»");
                                break;
                            }
                            ss.append('\t').append(code)
                            .append('\t').append(englishVersion.tts)
                            .append('\t').append("")
                            ;
                        }
                        ss.append('\t').append(key);

                        // Locale (CLDR)    Char1   English Name1   Fix to Native Name1 Char2   English Name2   Fix to Native Name2 Duplicate Native Name   Google Translate (just for reference)   Fixed?  Notes for translators — don't edit. Char2 Source
                        // af  🐐  goat             deer       bok goat    No      🦌

                        status.add(ss.toString());
                    }
                }

                //                for (Entry<String, Collection<String>> entry : nameToCode.asMap().entrySet()) {
                //                    if (entry.getValue().size() != 1) {
                //                        System.out.println("\t" + localeName + "\tduplicate: " + entry.getKey() + "\t" + entry.getValue());
                //                    }
                //                }
            }
        }

        try (PrintWriter out = BagFormatter.openUTF8Writer(TARGET_DIR, "spreadsheetErrors.txt")) {
            for (LocaleInfo error : errors) {
                Set<String> valuesSeen = new HashSet<>();
                for (Entry<String, Set<String>> entry : error.errors.entrySet()) {
                    System.out.println(entry.getKey());
                    for (String value : entry.getValue()) {
                        if (valuesSeen.contains(value)) {
                            continue;
                        }
                        out.println("\t" + entry.getKey() + "\t" + value);
                        valuesSeen.add(value);
                    }
                }
            }
        }

        System.out.println("Bad locales: " + badLocales);
        for (String s : badLocales) {
            System.out.println(s + "\t" + ULocale.getDisplayName(s, "en"));
        }
        for (String s : status) {
            System.out.println(s);
        }
        try (PrintWriter out = BagFormatter.openUTF8Writer(TARGET_DIR, "modify_config.txt")) {
            for (Entry<String, UnicodeMap<NewAnnotation>> entry : localeToNewAnnotations.entrySet()) {
                String locale = entry.getKey();
                UnicodeMap<NewAnnotation> map = entry.getValue();
                for (Entry<String, NewAnnotation> entry2 : map.entrySet()) {
                    String codepoints = entry2.getKey();
                    NewAnnotation emoji = entry2.getValue();
                    // locale=  af     ; action=add ; new_path=        //ldml/dates/fields/field[@type="second"]/relative[@type="0"]    ; new_value=    nou          
                    /*
                     * <ldml> <annotations> <!-- 1F600 grinning face; face; grin -->
                     * <annotation cp="😀">gesig | grinnik</annotation> <annotation
                     * cp="😀" type="tts">grinnikende gesig</annotation>
                     */
                    if (emoji.keywords != null) {
                        showConfigLine(out, locale, codepoints, "", emoji.keywords);
                    }
                    if (emoji.shortName != null) {
                        showConfigLine(out, locale, codepoints, "[@type=\"tts\"]", emoji.shortName);
                    }
                }
            }
            for (Entry<String, Map<String, String>> entry : localeToLabels.entrySet()) {
                String locale = entry.getKey();
                Map<String, String> labelToTrans = entry.getValue();
                for (Entry<String, String> entry2 : labelToTrans.entrySet()) {
                    System.out.println("TODO: " + locale + "\t" + entry2.getKey() + "\t" + entry2.getValue());
                }
            }
        }
    }

    /*
<tr style='height:20px;'>
<th id="155761641R3" style="height: 20px;" class="row-headers-background"><div class="row-header-wrapper" style="line-height: 20px;">4</div></th>
<td class="s5" dir="ltr">_1f468_200d_2695</td>
<td class="s6" dir="ltr"><div style='width:71px;height:20px;background:url(//images-docs-opensocial.googleusercontent.com/gadgets/proxy?url=http://unicode.org/draft/reports/tr51/images/android/android_1f468_200d_2695.png&container=docs&gadget=docs&rewriteMime=image/*&resize_h=36&resize_w=36) no-repeat center top'/></td>
<td class="s7" dir="ltr">male health worker</td>
<td class="s7" dir="ltr">doctor | healthcare | male | man | nurse | therapist</td>
<td class="s8"></td>
<td class="s8"></td>
<td class="s5" dir="ltr">mand og ???</td>
<td class="s5" dir="ltr">??? | menneske | person</td>
<td class="s9" dir="ltr">-</td>
<td class="s10" dir="ltr">#VALUE!</td>
<td class="s10" dir="ltr">#VALUE!</td>
</tr>
     */

    static final int 
    COUNT = 1,
    CODE = 2,
    IMAGE = 3,
    ENAME = 4,
    EKEYWORDS = 5,
    NNAME = 6,
    NKEYWORDS = 7;
    ;

    static final UnicodeSet DIGITS = new UnicodeSet('0','9').freeze();
    static final boolean debug = false;
    private static final boolean CHECK_MISSING = false;

    private static UnicodeMap<NewAnnotation> readFile(String parent, String name, LocaleInfo localeInfo,
            Map<String,String> labels) {
        UnicodeMap<NewAnnotation> newAnnotations = new UnicodeMap<>();

        try (BufferedReader in = BagFormatter.openUTF8Reader(parent, name)) {
            SimpleHtmlParser simple = new SimpleHtmlParser().setReader(in);
            StringBuilder result = new StringBuilder();
            String codePoint = null;
            String shortName = "";
            String annotations = "";
            boolean isFirst = true;
            int tdCount = 0;
            boolean inPop = true;
            boolean isLabel = false;
            main:
                while (true) {
                    Type x = simple.next(result);
                    String resultString = result.toString();
                    switch (x) {
                    case ELEMENT:
                        if (inPop) {
                            break;
                        }
                        switch (resultString) {
                        case "tr":
                            tdCount = 0;
                            break;
                        case "td":
                        case "th":
                            tdCount += 1;
                            break;
                        }
                        break;
                    case ELEMENT_START:
                        inPop = false;
                        break;
                    case ELEMENT_POP:
                        inPop = true;
                        break;
                    case ELEMENT_END:
                        break;
                    case DONE:
                        break main;
                    case ELEMENT_CONTENT:
                        if (inPop) {
                            break;
                        }
                        if (debug) {
                            System.out.println(inPop + "\t" + tdCount + "\t" + codePoint + "\t" + resultString);
                        }
                        switch (tdCount) {
                        case CODE:
                            if (resultString.length() < 2) { // hack, since there are a few special rows inserted.
                                break;
                            } else if (isFirst) {
                                isFirst = false;
                            } else if (resultString.charAt(0) == '_') {
                                codePoint = Utility.fromHex(resultString.substring(1).replace('_', ' '));
                            } else {
                                codePoint = resultString;
                                isLabel = true;
                            }
                            break;
                        case NNAME:
                            if (codePoint != null) {
                                shortName = addWithBrHack(shortName, resultString);
                            }
                            break;
                        case NKEYWORDS:
                            if (codePoint != null) {
                                annotations = addWithBrHack(annotations, resultString);
                                NewAnnotation old = newAnnotations.get(codePoint);
                                if (old != null) {
                                    throw new IllegalArgumentException("Duplicate code point: " + codePoint);
                                }
                                if (isLabel) {
                                    if (!shortName.isEmpty()) {
                                        labels.put(codePoint, shortName);
                                    }
                                } else if (shortName.isEmpty() || annotations.isEmpty()) {
                                    if (CHECK_MISSING) {
                                        System.out.println("Missing value. Code point: " + codePoint + "\tname:" + shortName + "\tkeywords:" + annotations);
                                    }
                                } else {
                                    newAnnotations.put(codePoint, new NewAnnotation(localeInfo, codePoint, shortName, annotations));
                                }
                                codePoint = null;
                                shortName = annotations = "";
                            }
                            break;
                        default:
                            break;
                        }
                        break;
                    default:
                        break;
                    }
                }
        } catch (IOException e) {
            System.err.println("Can't read file: " + name);
        }
        return newAnnotations;
    }

    private static String addWithBrHack(String annotations, String resultString) {
        if (annotations.isEmpty()) {
            annotations = resultString;
        } else {
            annotations += " " + resultString;
        }
        return annotations;
    }

    private static void showConfigLine(PrintWriter out, String locale, String codepoints, String type, String value) {
        out.println("locale=" + locale
                + " ; action=add ; new_path=//ldml/annotations/annotation[@cp=\"" + codepoints + "\"]"
                + type
                + " ; new_value=" + TransliteratorUtilities.toXML.transform(value)
                );
    }

    private static TableState checkStructure(TableState tableState, String resultString) {
        switch (resultString) {
        case "Character Code":
        case "e":
            if (tableState != TableState.NEED_CHARACTER_CODE) {
                throw new IllegalArgumentException("Bad TableState: " + tableState + "\t" + resultString);
            }
            tableState = TableState.NEED_CODE;
            break;
        default:
            if (tableState != TableState.NEED_CODE) {
                throw new IllegalArgumentException("Bad TableState: " + tableState + "\t" + resultString);
            }
            if (!resultString.isEmpty() && !resultString.startsWith("U+")) {
                throw new IllegalArgumentException("Bad hex: " + resultString);
            }
        }
        return tableState;
    }
}