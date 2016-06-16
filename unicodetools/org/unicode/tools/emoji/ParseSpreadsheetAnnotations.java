package org.unicode.tools.emoji;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.tool.LanguageCodeConverter;
import org.unicode.cldr.util.Annotations;
import org.unicode.cldr.util.SimpleHtmlParser;
import org.unicode.cldr.util.SimpleHtmlParser.Type;
import org.unicode.cldr.util.TransliteratorUtilities;
import org.unicode.text.utility.Utility;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class ParseSpreadsheetAnnotations {

    enum TableState {
        NEED_CHARACTER_CODE, NEED_CODE
    }

    static final Splitter BAR = Splitter.on('|').trimResults().omitEmptyStrings();
    static final Joiner BAR_JOINER = Joiner.on(" | ");
    private static final String DEBUG_CODEPOINT = new StringBuilder().appendCodePoint(0x1f951).toString();
    private static UnicodeSet OK_CODEPOINTS = new UnicodeSet("[\\-[:L:][:M:][:Nd:]\\u0020/|“”„’,.\u200C\u200D\u05F4\u104D\u2018]");
    private static Matcher SPACES = Pattern.compile("[\\s\u00A0\u00B7]+").matcher("");

    static class NewAnnotation {
        final String shortName;
        final String keywords;
        public NewAnnotation(String shortName, String keywords) {
            shortName = check(shortName);
            keywords = check(BAR_JOINER.join(BAR.splitToList(keywords.replace("/", "|"))));
            this.shortName = shortName.isEmpty() ? null : shortName;
            this.keywords = keywords.isEmpty() ? null : keywords;
        }
        private String check(String item) {
            item = SPACES.reset(TransliteratorUtilities.fromHTML.transform(item.trim())).replaceAll(" ");
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
            if (!OK_CODEPOINTS.containsAll(item)) {
                throw new IllegalArgumentException("Bad char in «" + item + "»: " + new UnicodeSet().addAll(item).removeAll(OK_CODEPOINTS));
            }
            return item;
        }
        @Override
        public String toString() {
            return shortName + " | " + keywords;
        }
    }

    public static void main(String[] args) throws IOException {

        UnicodeMap<Annotations> englishAnnotations = Annotations.getData("en");
        Map<String,UnicodeMap<NewAnnotation>> localeToNewAnnotations = new TreeMap<>();
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
        Set<String> skipNames = ImmutableSet.of("Internal", "Sheet", "Summary", "Template", "Duplicates");
        Set<String> currentAnnotations = Annotations.getAvailable();
        boolean debug = false;

        final String dir = "/Users/markdavis/Google Drive/workspace/DATA/emoji/";
        fileLoop: for (File file : new File(dir
                + "2016 Emoji Annotations").listFiles()) {
            String name = file.getName();
            if (!name.endsWith(".html")) {
                continue;
            }
            String localeName = LanguageCodeConverter.fromGoogleLocaleId(name.substring(0,name.length()-5));

            for (String skipName : skipNames) {
                if (name.startsWith(skipName)) {
                    continue fileLoop;
                }
            }

            UnicodeMap<NewAnnotation> newAnnotations = new UnicodeMap<>();

            try (BufferedReader in = BagFormatter.openUTF8Reader(file.getParent(), name)) {
                SimpleHtmlParser simple = new SimpleHtmlParser().setReader(in);
                StringBuilder result = new StringBuilder();
                String codePoint = "";
                String shortName = "";
                String annotations = "";
                int tdCount = 0;
                TableState tableState = TableState.NEED_CHARACTER_CODE;
                main:
                    while (true) {
                        Type x = simple.next(result);
                        String resultString = result.toString();
                        switch (x) {
                        case ELEMENT: // with each row store the info.
                            switch (resultString) {
                            case "tr":
                                if (!codePoint.isEmpty()
                                        && (!shortName.isEmpty() || !annotations.isEmpty())
                                        ) {
                                    NewAnnotation old = newAnnotations.get(codePoint);
                                    if (old != null) {
                                        throw new IllegalArgumentException("Duplicate code point: " + codePoint);
                                    }
                                    if (shortName.isEmpty() || annotations.isEmpty()) {
                                        System.out.println("Missing value. Code point: " + codePoint + "\tname:" + shortName + "\tkeywords:" + annotations);
                                    }
                                    newAnnotations.put(codePoint, new NewAnnotation(shortName, annotations));
                                }
                                codePoint = shortName = annotations = "";
                                tdCount = 0;
                                break;
                            case "td":
                                tdCount += 1;
                                break;
                            }
                            break;
                        case ELEMENT_POP:
                            break;
                        case ELEMENT_END:
                            break;
                        case DONE:
                            break main;
                        case ELEMENT_CONTENT:
                            switch (tdCount) {
                            case 1:
                                TableState newTableState = checkStructure(tableState, resultString);
                                if (tableState == TableState.NEED_CODE) {
                                    codePoint = resultString.isEmpty() ? "" : Utility.fromHex(resultString.substring(2));
                                    debug = codePoint.equals(DEBUG_CODEPOINT) && localeName.equals("bg");
                                } else {
                                    codePoint = "";
                                }
                                tableState = newTableState;
                                break;
                            case 7:
                                if (debug) {
                                    System.out.println(tdCount + "\t" + codePoint + "\t" + resultString);
                                }
                                shortName = addWithBrHack(shortName, resultString);
                                break;
                            case 11:
                                if (debug) {
                                    System.out.println(tdCount + "\t" + codePoint + "\t" + resultString);
                                }
                                // HACK because of <br>
                                annotations = addWithBrHack(annotations, resultString);
                                break;
                            default:
                                if (debug) {
                                    System.out.println(tdCount + "\t" + codePoint + "\t" + resultString);
                                }
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
            if (newAnnotations.isEmpty()) {
                System.out.println("\t" + localeName + "\tempty, skipping");
                continue;
            } 
            System.out.println("\t" + localeName + "\tsize:\t" + newAnnotations.size());
            if (!currentAnnotations.contains(localeName)) {
                System.out.println("\t" + localeName + "\tbad locale name");
                badLocales.add(localeName);
            } else {
                newAnnotations.freeze();
                localeToNewAnnotations.put(localeName, newAnnotations);

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
                            ss.append('\t').append(code)
                            .append('\t').append(englishAnnotations.get(code).tts)
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
        System.out.println("Bad locales: " + badLocales);
        for (String s : badLocales) {
            System.out.println(s + "\t" + ULocale.getDisplayName(s, "en"));
        }
        for (String s : status) {
            System.out.println(s);
        }
        try (PrintWriter out = BagFormatter.openUTF8Writer(dir, "modify_config.txt")) {
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
        }
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
                throw new IllegalArgumentException("Bad TableState: " + tableState);
            }
            tableState = TableState.NEED_CODE;
            break;
        default:
            if (tableState != TableState.NEED_CODE) {
                throw new IllegalArgumentException("Bad TableState: " + tableState);
            }
            if (!resultString.isEmpty() && !resultString.startsWith("U+")) {
                throw new IllegalArgumentException("Bad hex: " + resultString);
            }
        }
        return tableState;
    }
}