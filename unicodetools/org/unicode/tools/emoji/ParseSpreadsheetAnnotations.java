package org.unicode.tools.emoji;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
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
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;

public class ParseSpreadsheetAnnotations {

    private static final CLDRConfig CONFIG = CLDRConfig.getInstance();
    private static final String SOURCE_DIR = CLDRPaths.DATA_DIRECTORY + "emoji/annotations_import";
    private static final String TARGET_DIR = CLDRPaths.GEN_DIRECTORY + "emoji/annotations/";
    private static final CLDRFile ENGLISH = CONFIG.getEnglish();
    private static final Set<String> skipNames = ImmutableSet.of(
            "Internal", "Counts", "BadChars", "Collisions", "Temp", "Template",  
            "Sheet", "Summary", "Duplicates");

    enum TableState {
        NEED_CHARACTER_CODE, NEED_CODE
    }

    static final Splitter BAR = Splitter.onPattern("[।|၊/]").trimResults().omitEmptyStrings();
    static final Joiner BAR_JOINER = Joiner.on(" | ");
    private static final String DEBUG_CODEPOINT = new StringBuilder().appendCodePoint(0x1f951).toString();
    //private static UnicodeSet OK_CODEPOINTS = new UnicodeSet("[\\-[:L:][:M:][:Nd:]\\u0020/|“”„’,.\u200C\u200D\u05F4\u104D\u2018]").freeze();
    private static Matcher SPACES = Pattern.compile("[\\s\u00A0\u00B7]+").matcher("");
    private static UnicodeSet OK_IN_JOINER_SEQUENCES = new UnicodeSet("[\\:,\u060C()]").freeze();

    enum ItemType {short_name, keywords, label, label_pattern};
    static Normalizer2 NFC = Normalizer2.getNFCInstance();
    static final UnicodeMap<UnicodeSet> EXCEPTIONS = new UnicodeMap<UnicodeSet>()
            .put("💿", new UnicodeSet("[cdCD]").freeze())
            .put("📀", new UnicodeSet("[DVdv]").freeze())
            .put("㊗", new UnicodeSet("[祝]").freeze())
            .put("㊙", new UnicodeSet("[秘]").freeze())
            .put("🔠", new UnicodeSet("[ABCDabcd]").freeze())
            .put("🔡", new UnicodeSet("[abcd]").freeze())
            .put("🔤", new UnicodeSet("[abc]").freeze())
            .put("🚻", new UnicodeSet("[wcWC]").freeze())
            .put("🅰", new UnicodeSet("[Aa]").freeze())
            .put("🆎", new UnicodeSet("[ABab]").freeze())
            .put("🅱", new UnicodeSet("[Bb]").freeze())
            .put("🆑", new UnicodeSet("[CLcl]").freeze())
            .put("🆒", new UnicodeSet("[COLcol]").freeze())
            .put("🆓", new UnicodeSet("[FREfre]").freeze())
            .put("🆔", new UnicodeSet("[IDid]").freeze())
            .put("Ⓜ", new UnicodeSet("[Mm]").freeze())
            .put("🆕", new UnicodeSet("[NEWnew]").freeze())
            .put("🆖", new UnicodeSet("[NGng]").freeze())
            .put("🅾", new UnicodeSet("[Oo]").freeze())
            .put("🆗", new UnicodeSet("[OKok]").freeze())
            .put("🅿", new UnicodeSet("[Pp]").freeze())
            .put("🆘", new UnicodeSet("[SOSsos]").freeze())
            .put("™", new UnicodeSet("[TMtm]").freeze())
            .put("🆙", new UnicodeSet("[UP!up!]").freeze())
            .put("🆚", new UnicodeSet("[VSvs]").freeze())
            .put("🈁", new UnicodeSet("[ココKOko]").freeze())
            .put("🈂", new UnicodeSet("[サSAsa]").freeze())
            .put("🈹", new UnicodeSet("[割]").freeze())
            .put("🉑", new UnicodeSet("[可]").freeze())
            .put("🈴", new UnicodeSet("[合]").freeze())
            .put("🈺", new UnicodeSet("[営]").freeze())
            .put("🉐", new UnicodeSet("[得]").freeze())
            .put("🈯", new UnicodeSet("[指]").freeze())
            .put("🈷", new UnicodeSet("[月]").freeze())
            .put("🈶", new UnicodeSet("[有]").freeze())
            .put("🈵", new UnicodeSet("[満]").freeze())
            .put("🈚", new UnicodeSet("[無]").freeze())
            .put("🈸", new UnicodeSet("[申]").freeze())
            .put("🈲", new UnicodeSet("[禁]").freeze())
            .put("🈳", new UnicodeSet("[空]").freeze())
            .put("🈲", new UnicodeSet("[禁]").freeze())
            .put("🈳", new UnicodeSet("[空]").freeze())

            .put("🙅‍♂", new UnicodeSet("[ＮＧGN]").freeze())
            .put("🙅‍♀", new UnicodeSet("[ＮＧGN]").freeze())
            .put("🙅", new UnicodeSet("[ＮＧGN]").freeze())
            .put("🙆‍♂", new UnicodeSet("[OKokＫＯ]").freeze())
            .put("🙆‍♀", new UnicodeSet("[OKokＫＯ]").freeze())
            .put("🙆", new UnicodeSet("[OKokＫＯ]").freeze())
            .put("👌", new UnicodeSet("[OKokＫＯ]").freeze())
            .put("🔛", new UnicodeSet("[onON!]").freeze())
            .put("📺", new UnicodeSet("[tvTV]").freeze())
            .put("📻", new UnicodeSet("[FM]").freeze())
            .put("👨‍💻", new UnicodeSet("[IT]").freeze())
            .put("👩‍💻", new UnicodeSet("[IT]").freeze())
            .freeze();

    static class LocaleInfo {
        final String locale;
        final UnicodeSet okNameCharacters;
        final UnicodeSet okKeywordCharacters;
        final UnicodeSet okPatternChars;
        final Set<String> badChars = new LinkedHashSet<>();

        public LocaleInfo(String localeName) {
            locale = localeName;
            final CLDRFile cldrFile = CONFIG.getCldrFactory().make(localeName, true);
            okNameCharacters = new UnicodeSet("[[:Nd:]\\u0020+]")
            .addAll(cldrFile.getExemplarSet("", WinningChoice.WINNING))
            .addAll(cldrFile.getExemplarSet("auxiliary", WinningChoice.WINNING))
            .addAll(cldrFile.getExemplarSet("punctuation", WinningChoice.WINNING))
            .remove("'")
            .remove('"');
            if (locale.equals("zh")) {
                okNameCharacters.addAll(new UnicodeSet("[乒 乓 仓 伞 冥 凉 刨 匕 厦 厨 呣 唇 啤 啮 喱 嗅 噘 噢 墟 妆 婴 媚 宅 寺 尬 尴 屑 巾 弓 彗 惊 戟 扔 扰 扳 抛 挂 捂 摇 撅 杆 杖 柜 柱 栗 栽 桶 棍 棕 棺 榈 槟 橙 洒 浆 涌 淇 滚 滩 灾 烛 烟 焰 煎 犬 猫 瓢 皱 盆 盔 眨 眯 瞌 矿 祈 祭 祷 稻 竿 笼 筒 篷 粮 纠 纬 缆 缎 耸 舔 舵 艇 芽 苜 苞 菇 菱 葫 葵 蒸 蓿 蔽 薯 蘑 蚂 蛛 蜗 蜘 蜡 蝎 蝴 螃 裹 谍 豚 账 跤 踪 躬 轴 辐 迹 郁 鄙 酢 钉 钥 钮 铅 铛 锄 锚 锤 闺 阱 隧 雕 霾 靴 靶 鞠 颠 馏 驼 骆 髦 鲤 鲸 鳄 鸽]"));
            } else if (locale.equals("zh_Hant")) {
                okNameCharacters.addAll(new UnicodeSet("[乳 划 匕 匙 匣 叉 吻 嘟 噘 妖 巾 帆 廁 廚 弋 弓 懸 戟 扳 捂 摔 暈 框 桶 桿 櫃 煎 燭 牡 皺 盒 眨 眩 筒 簍 糰 紋 紗 纏 纜 羯 聳 肖 艇 虹 蛛 蜘 蝴 蝸 蠟 裙 豚 躬 釘 鈔 鈕 鉛 鎚 鎬 鐺 鑰 鑽 霄 鞠 骰 骷 髏 鯉 鳶]"));
            } else if (locale.equals("uk")) {
                okNameCharacters.add('’');
            } else if (locale.equals("id")) {
                okNameCharacters.add('-');
            } else if (locale.equals("zu")) {
                okNameCharacters.add('’');
            } else if (locale.equals("fi")) {
                okNameCharacters.add('-');
            } else if (locale.equals("my")) {
                okNameCharacters.addAll("၏−");
            } else if (locale.equals("am")) {
                okNameCharacters.addAll(":");
            } else if (locale.equals("ka")) {
                okNameCharacters.addAll(":");
            } else if (locale.equals("mr")) {
                okNameCharacters.addAll("ऱ");
            } else if (locale.equals("sw")) {
                okNameCharacters.addAll("’");
            } else if (locale.equals("km")) {
                okNameCharacters.addAll(":");
            } else if (locale.equals("hi")) {
                okNameCharacters.addAll("–");                
            }
            okNameCharacters.remove('|').remove(';').freeze();
            okKeywordCharacters = new UnicodeSet(okNameCharacters).add('|').freeze();
            okPatternChars = new UnicodeSet(okNameCharacters).add('{').add('}').freeze();
        }

        UnicodeSet getCheckingSet(ItemType itemType) {
            switch(itemType) {
            case keywords: return okKeywordCharacters;
            case label_pattern: return okPatternChars;
            case label:
            case short_name: return okNameCharacters;
            default: throw new IllegalArgumentException("Internal error");
            }
        }

        private String check(String codePoint, ItemType itemType, String item, Output<Boolean> isOk) {
            isOk.value = Boolean.TRUE;
            item = SPACES.reset(TransliteratorUtilities.fromHTML.transform(item.trim())).replaceAll(" ").trim();
            item = item.replace("'", "’"); // HACK
            item = NFC.normalize(item);
            // hacks
            //            item = item.replace("'", "’")
            //                    .replace("סכו\"ם", "סכו\u05F4ם")
            //                    .replace(", |", " |")
            //                    .replace("\"сөз", "сөз")
            //                    .replace("жок\"", "жок")
            //                    .replace("\"мага чал\"", "“мага чал”")
            //                    .replace("\"нет слов\"", "“нет слов”")
            //                    .replace("\"më telefono\"", "“më telefono”")
            //                    .replace("\"ndalo\"", "“ndalo”")
            //                    .replace("\"nipigie simu\"", "“nipigie simu”")
            //                    .replace("\"стоп\"", "“стоп”")
            //                    ;
            final UnicodeSet okCharacters = getCheckingSet(itemType);
            if (!okCharacters.containsAll(item)) {
                final UnicodeSet badCodePoints = new UnicodeSet().addAll(item).removeAll(okCharacters);
                if (codePoint.contains(Emoji.JOINER_STRING)) {
                    badCodePoints.removeAll(OK_IN_JOINER_SEQUENCES);
                }
                UnicodeSet exceptions = EXCEPTIONS.get(codePoint);
                if (exceptions != null) {
                    badCodePoints.removeAll(exceptions);
                }
                if (!badCodePoints.isEmpty()) {
                    //                    Set<String> chars = new TreeSet<>();
                    //                    Set<String> names = new LinkedHashSet<>();
                    //                    for (String s : badCodePoints) {
                    //                        chars.add(s);
                    //                        names.add(Utility.hex(s, "+") + " ( " + s + " ) " + UCharacter.getName(s, " + "));
                    //                    }
                    badChars.add(itemType 
                            + "\t" + codePoint 
                            + "\t«" + getEnglishName(itemType, codePoint)
                            + "»\t«" +  item
                            + "»\t" + badCodePoints.toPattern(false)
                            + "\t" + "=googletranslate(index($A$1:$G$999,row(),5),index($A$1:$G$999,row(),1),\"en\")");
                    isOk.value = Boolean.FALSE;
                }
            }
            return item;
        }
    }

    enum Problem {missing, badchar, duplicate, duplicateWithin}

    static class NewAnnotation {
        final String shortName;
        final String keywords;
        Set<Problem> problems = EnumSet.noneOf(Problem.class);
        public NewAnnotation(LocaleInfo localeInfo, String codePoint, String shortName, String keywords) {
            if (localeInfo.locale.equals("am") && codePoint.equals("🐀")) {
                int debug = 0;
            }
            Output<Boolean> isOkOut = new Output<>();
            if (shortName.trim().equals("n/a")  || shortName.trim().equals("n / a")) {
                shortName = "";
            }
            if (keywords.trim().equals("n/a")  || keywords.trim().equals("n / a")) {
                shortName = "";
            }
            shortName = localeInfo.check(codePoint, ItemType.short_name, shortName, isOkOut);
            if (!isOkOut.value) {
                addProblem(Problem.badchar);
            }
            List<String> keywordsRaw= BAR.splitToList(keywords); 
            Set<String> keywordsOk = new LinkedHashSet<>();
            Set<String> keywordsBad = new LinkedHashSet<>();
            for (String keyword : keywordsRaw) {
                String fixedKeyword = localeInfo.check(codePoint, ItemType.keywords, keyword, isOkOut);
                if (isOkOut.value) {
                    keywordsOk.add(fixedKeyword);
                } else {
                    keywordsBad.add(fixedKeyword);
                }
            }
            if (!keywordsBad.isEmpty()) {
                System.out.println("# Skipping keywords: " + keywordsBad + "; keeping " + keywordsOk);
            }
            if (!keywordsOk.isEmpty()) {
                keywords = BAR_JOINER.join(keywordsOk);
            } else {
                keywords = BAR_JOINER.join(keywordsBad);
                addProblem(Problem.badchar);
            }
            if (shortName.isEmpty() || keywords.isEmpty()) {
                addProblem(Problem.missing);
            }
            this.shortName = shortName;
            this.keywords = keywords;
        }

        private boolean addProblem(Problem problem) {
            return problems.add(problem);
        }

        @Override
        public String toString() {
            return shortName + " | " + keywords;
        }
    }

    static final EmojiAnnotations englishAnnotations = EmojiAnnotations.ANNOTATIONS_TO_CHARS;

    public static void main(String[] args) throws IOException {

        Map<String,UnicodeMap<NewAnnotation>> localeToNewAnnotations = new TreeMap<>();
        Map<String,Map<String,String>> localeToLabelCodeToTrans = new TreeMap<>();
        Multimap<String,String> duplicates = TreeMultimap.create();
        Set<String> missing = new LinkedHashSet<>();
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
        Set<String> currentAnnotations = Annotations.getAvailable();

        final Set<String> inclusions = null; // ImmutableSet.of("be", "bs", "cy", "eu", "ga", "gl", "zu"); // en-GB, es-419, fr-ca,  zh-HK (yue), 
        Set<LocaleInfo> localeInfoSet = new LinkedHashSet<>();

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
            Map<String,String> labelCodeToTrans = new LinkedHashMap<>();
            LocaleInfo localeInfo = new LocaleInfo(localeName);
            localeInfoSet.add(localeInfo);

            UnicodeMap<NewAnnotation> newAnnotations = readFile(file.getParent(), name, localeInfo, labelCodeToTrans);

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
                localeToLabelCodeToTrans.put(localeName, labelCodeToTrans);

                UnicodeMap<Annotations> oldData = Annotations.getData(localeName);
                Multimap<String,String> oldNameToCode = TreeMultimap.create();
                Multimap<String,String> newNameToCode = TreeMultimap.create();

                for (Entry<String, Annotations> item : oldData.entrySet()) {
                    String codepoint = item.getKey();
                    NewAnnotation newItem = newAnnotations.get(codepoint);
                    final String shortName = item.getValue().tts;
                    if (shortName == null) continue;
                    oldNameToCode.put(shortName, item.getKey());
                }

                for (Entry<String, NewAnnotation> item : newAnnotations.entrySet()) {
                    final NewAnnotation newAnnotation = item.getValue();
                    String newCode = item.getKey();
                    final String tts = newAnnotation.shortName;
                    final String keywords = newAnnotation.keywords;
                    if (tts == null || keywords == null) {
                        missing.add(localeName + "\tmissing:\t" + newCode + "\tenglish:" + getEnglishName(ItemType.short_name, newCode));
                        continue;
                    }
                    newNameToCode.put(tts, item.getKey());
                    Collection<String> oldCodes = oldNameToCode.get(tts);
                    if (oldCodes != null) {
                        for (String old : oldCodes) {
                            if (old.equals(newCode)) {
                                continue;
                            }
                            addDuplicates2(localeName, ItemType.short_name, tts, old, Collections.singleton(newCode), duplicates);
                            if (localeInfo.locale.equals("am") && newCode.equals("🐀")) {
                                for (String oldCode : oldCodes) {
                                    System.out.println(Utility.hex(oldCode));
                                }
                                int debug = 0;
                            }
                            newAnnotation.addProblem(Problem.duplicate);
                        }
                    }
                }
                // now pick up the items that are duplicates within the sheet.
                for (Entry<String, NewAnnotation> item : newAnnotations.entrySet()) {
                    String newCode = item.getKey();
                    final NewAnnotation newAnnotation = item.getValue();
                    final String key = newAnnotation.shortName;
                    //if (key == null) continue;
                    Collection<String> newCodes = newNameToCode.get(key);
                    if (newCodes.size() != 1) {
                        addDuplicates2(localeName, ItemType.short_name, key, null, newCodes, duplicates);
                        if (localeInfo.locale.equals("am") && newCode.equals("🐀")) {
                            for (String oldCode : newCodes) {
                                System.out.println(Utility.hex(oldCode));
                            }
                            int debug = 0;
                        }
                        newAnnotation.addProblem(Problem.duplicateWithin);
                    }
                }

                //                for (Entry<String, Collection<String>> entry : nameToCode.asMap().entrySet()) {
                //                    if (entry.getValue().size() != 1) {
                //                        System.out.println("\t" + localeName + "\tduplicate: " + entry.getKey() + "\t" + entry.getValue());
                //                    }
                //                }
            }
        }

        for (Entry<String, Map<String, String>> localeAndLabelCodeToTrans : localeToLabelCodeToTrans.entrySet()) {
            String locale = localeAndLabelCodeToTrans.getKey();
            Map<String, String> labelCodeToTrans = localeAndLabelCodeToTrans.getValue();
            // invert
            Multimap<String,String> transToLabelCodes = TreeMultimap.create();
            for (Entry<String, String> entry2 : labelCodeToTrans.entrySet()) {
                transToLabelCodes.put(entry2.getValue(), entry2.getKey());
            }
            // record the labels we have to remove because they collide
            Set<String> labelCodesToRemove = new HashSet<>();
            for (Entry<String, Collection<String>> transAndLabelCodes : transToLabelCodes.asMap().entrySet()) {
                final String trans = transAndLabelCodes.getKey();
                final Collection<String> labelCodes = transAndLabelCodes.getValue();
                if (labelCodes.size() > 1) {
                    addDuplicates2(locale, ItemType.label, trans, null, labelCodes, duplicates);
                    labelCodesToRemove.addAll(labelCodes);
                }
            }
            // remove them
            for (String label : labelCodesToRemove) {
                labelCodeToTrans.remove(label);
            }
        }

        try (PrintWriter out = BagFormatter.openUTF8Writer(TARGET_DIR, "spreadsheetSuspectChars.txt")) {
            out.println("#Locale\tType\tEmoji or Label\tEnglish Version\tNative Version\tSuspect characters\tGoogle translate (just for comparison)\tComments");
            for (LocaleInfo localeInfo : localeInfoSet) {
                for (String value : localeInfo.badChars) {
                    out.println(localeInfo.locale + "\t" + value);
                }
            }
        }

        try (PrintWriter out = BagFormatter.openUTF8Writer(TARGET_DIR, "spreadsheetBadLocales.txt")) {
            for (String s : badLocales) {
                out.println(s + "\t" + ULocale.getDisplayName(s, "en"));
            }
        }

        try (PrintWriter out = BagFormatter.openUTF8Writer(TARGET_DIR, "spreadsheetDuplicates.txt")) {
            out.println("#Locale\tType\tCLDR Emoji\tEnglish\tSheet Emoji\tEnglish2\tNative Collision\tGoogle translate (just for comparison)\tFix for CLDR (in cell, not comment). Put fix for sheet in sheet.");
            for (Entry<String, Collection<String>> s : duplicates.asMap().entrySet()) {
                for (String value : s.getValue()) {
                    out.println(value);
                }
            }
        }

        try (PrintWriter out = BagFormatter.openUTF8Writer(TARGET_DIR, "spreadsheetMissing.txt")) {
            for (String s : missing) {
                out.println(s);
            }
        }

        try (PrintWriter out = BagFormatter.openUTF8Writer(TARGET_DIR, "modify_config.txt")) {
            for (Entry<String, UnicodeMap<NewAnnotation>> entry : localeToNewAnnotations.entrySet()) {
                String locale = entry.getKey();
                UnicodeMap<NewAnnotation> map = entry.getValue();
                for (Entry<String, NewAnnotation> entry2 : map.entrySet()) {
                    String codepoints = entry2.getKey();
                    NewAnnotation emoji = entry2.getValue();
                    if (!emoji.problems.isEmpty()) {
                        showConfigDeleteLine(out, locale, codepoints, "");
                        System.out.println("#" + locale + "\t" + codepoints + "\t" + emoji + "\t" + emoji.problems);
                        continue;
                    }
                    /*
                     * locale=  af     ; action=add ; new_path=        //ldml/dates/fields/field[@type="second"]/relative[@type="0"]    ; new_value=    nou          
                     * <ldml> <annotations>
                     * <annotation cp="😀">gesig | grinnik</annotation>
                     * <annotation cp="😀" type="tts">grinnikende gesig</annotation>
                     */
                    if (emoji.keywords != null) {
                        showConfigLine(out, locale, codepoints, "", emoji.keywords);
                    }
                    if (emoji.shortName != null) {
                        showConfigLine(out, locale, codepoints, "[@type=\"tts\"]", emoji.shortName);
                    }
                }
            }
            //            for (Entry<String, Map<String, String>> entry : localeToLabels.entrySet()) {
            //                String locale = entry.getKey();
            //                Map<String, String> labelToTrans = entry.getValue();
            //                for (Entry<String, String> entry2 : labelToTrans.entrySet()) {
            //                    System.out.println("TODO: " + locale + "\t" + entry2.getKey() + "\t" + entry2.getValue());
            //                }
            //            }
        }
    }

    private static void addDuplicates2(String localeName, ItemType itemType, final String translation, String CLDR, Collection<String> keys,
            Multimap<String, String> duplicates) {
        ArrayList<String> keysList = new ArrayList<>(keys);
        for (int i = 0; i < keysList.size(); ++i) {
            final String cldrItem = CLDR == null ? "n/a" : CLDR;
            final String englishName = CLDR == null ? "n/a" : getEnglishName(itemType, CLDR);
            StringBuilder ss = new StringBuilder(localeName)
            .append('\t').append(itemType)
            .append('\t').append(cldrItem)
            .append('\t').append(englishName)
            .append('\t').append(keysList.get(i))
            .append('\t').append(getEnglishName(itemType, keysList.get(i)))
            .append('\t').append(translation)
            .append('\t').append("=googletranslate(index($A$1:$G$999,row(),7),index($A$1:$G$999,row(),1),\"en\")");
            if (CLDR == null) {
                ss.append("\tn/a: no value in CLDR, make change in sheet");
            }
            duplicates.put(localeName, ss.toString());
        }
    }

    private static String getEnglishName(ItemType itemType, String code1) {
        String englishName;
        switch (itemType) {
        case keywords:
            final Set<String> keywords = englishAnnotations.getKeys(code1);
            englishName = keywords == null ? null : CollectionUtilities.join(keywords, " | ");
            break;
        case short_name:
            englishName = englishAnnotations.getShortName(code1);
            break;
        case label: 
            //      <characterLabel type="activities">activity</characterLabel>
            englishName = ENGLISH.getStringValue("//ldml/characterLabels/characterLabel[@type=\"" + code1 + "\"]");
            break;
        case label_pattern: 
            //      <characterLabelPattern type="all">{0} — all</characterLabelPattern>
            englishName = ENGLISH.getStringValue("//ldml/characterLabels/characterLabelPattern[@type=\"" + code1 + "\"]");
            break;
        default: 
            throw new IllegalArgumentException();
        }
        return englishName == null 
                ? "???" 
                        : englishName;
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
            Map<String,String> labelCodeToTrans) {
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
                                        final ItemType itemType = LABEL_PATTERNS.contains(codePoint) 
                                                ? ItemType.label_pattern 
                                                        : ItemType.label;
                                        Output<Boolean> isOkOut = new Output<>();
                                        shortName = localeInfo.check(codePoint, itemType, shortName, isOkOut);
                                        if (isOkOut.value) { // skipping bad value.
                                            labelCodeToTrans.put(codePoint, shortName);
                                        }
                                    }
                                } else if (shortName.isEmpty() || annotations.isEmpty()) {
                                    if (CHECK_MISSING) {
                                        System.out.println("Missing value. Code point: " + codePoint + "\tname:" + shortName + "\tkeywords:" + annotations);
                                    }
                                } else {
                                    final NewAnnotation newAnnotation = new NewAnnotation(localeInfo, codePoint, shortName, annotations);
                                    if (newAnnotation.problems.isEmpty()) {
                                        newAnnotations.put(codePoint, newAnnotation);
                                    }
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

    static Set<String> LABEL_PATTERNS = ImmutableSet.of("category-list", "emoji", "keycap");

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

    private static void showConfigDeleteLine(PrintWriter out, String locale, String codepoints, String type) {
        out.println("locale=" + locale
                + " ; action=delete ; new_path=//ldml/annotations/annotation[@cp=\"" + codepoints + "\"]"
                );
        out.println("locale=" + locale
                + " ; action=delete ; new_path=//ldml/annotations/annotation[@cp=\"" + codepoints + "\"]"
                + type
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