package org.unicode.tools.emoji;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.unicode.cldr.tool.LanguageCodeConverter;
import org.unicode.cldr.util.Annotations;
import org.unicode.cldr.util.SimpleHtmlParser;
import org.unicode.cldr.util.SimpleHtmlParser.Type;
import org.unicode.text.utility.Utility;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.dev.util.UnicodeMap;

public class ParseSpreadsheetAnnotations {

    enum TableState {
        NEED_CHARACTER_CODE, NEED_CODE
    }

    static final Splitter BAR = Splitter.on('|').trimResults().omitEmptyStrings();
    static final Joiner BAR_JOINER = Joiner.on(" | ");
    
    static class NewAnnotation {
        final String shortName;
        final String keywords;
        public NewAnnotation(String shortName, String keywords) {
            this.shortName = shortName.isEmpty() ? null : shortName;
            this.keywords = keywords.isEmpty() ? null : BAR_JOINER.join(BAR.split(keywords));
        }
        @Override
        public String toString() {
            return shortName + " | " + keywords;
        }
    }

    public static void main(String[] args) {

        UnicodeMap<Annotations> englishAnnotations = Annotations.getData("en");

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
        Set<String> skipNames = ImmutableSet.of("Internal", "Sheet", "Summary", "Template");
        Set<String> currentAnnotations = Annotations.getAvailable();

        fileLoop: for (File file : new File("/Users/markdavis/Google Drive/workspace/DATA/emoji/2016 Emoji Annotations").listFiles()) {
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
                String codePoint = null;
                String shortName = null;
                String annotations = null;
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
                                if (codePoint != null && !codePoint.isEmpty()) {
                                    NewAnnotation old = newAnnotations.get(codePoint);
                                    if (old != null) {
                                        throw new IllegalArgumentException("Duplicate code point: " + codePoint);
                                    }
                                    newAnnotations.put(codePoint, new NewAnnotation(shortName, annotations));
                                    codePoint = shortName = annotations = null;
                                }
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
                                }
                                tableState = newTableState;
                                break;
                            case 7:
                                shortName = resultString;
                                break;
                            case 11:
                                annotations = resultString;
                                break;
                            default:
                                int debug = 0;
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
            System.out.println("\t" + localeName + "\tsize:\t" + newAnnotations.size());
            if (!currentAnnotations.contains(localeName)) {
                System.out.println("\t" + localeName + "\tbad locale name");
            } else {
                UnicodeMap<Annotations> oldData = Annotations.getData(localeName);
                Multimap<String,String> nameToCode = TreeMultimap.create();
                for (Entry<String, Annotations> item : oldData.entrySet()) {
                    final String key = item.getValue().tts;
                    if (key == null) continue;
                    nameToCode.put(key, item.getKey());
                }
                for (Entry<String, NewAnnotation> item : newAnnotations.entrySet()) {
                    final String key = item.getValue().shortName;
                    if (key == null) continue;
                    nameToCode.put(key, item.getKey());
                }
                for (Entry<String, NewAnnotation> item : newAnnotations.entrySet()) {
                    final String key = item.getValue().shortName;
                    if (key == null) continue;
                    Collection<String> codes = nameToCode.get(key);
                    if (codes.size() != 1) {
                        LinkedHashSet<String> englishNames = new LinkedHashSet<String>();
                        for (String code : codes) {
                            Annotations eName = englishAnnotations.get(code);
                            englishNames.add(eName.tts);
                        }
                        System.out.println("\t" + localeName + "\tduplicate:\t" + key + "\t" + codes + "\t" + englishNames);
                    }
                }

//                for (Entry<String, Collection<String>> entry : nameToCode.asMap().entrySet()) {
//                    if (entry.getValue().size() != 1) {
//                        System.out.println("\t" + localeName + "\tduplicate: " + entry.getKey() + "\t" + entry.getValue());
//                    }
//                }
            }
        }
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