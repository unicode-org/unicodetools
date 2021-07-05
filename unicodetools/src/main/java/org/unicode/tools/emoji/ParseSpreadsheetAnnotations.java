package org.unicode.tools.emoji;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.WinningChoice;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.SimpleHtmlParser;
import org.unicode.cldr.util.SimpleHtmlParser.Type;
import org.unicode.cldr.util.TransliteratorUtilities;
import org.unicode.text.utility.Utility;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;

/**
 * Current Process is:
 * <ul>
 * <li>Generate annotation csv's with GenerateMissingAnnotations
 * <li>Add to spreadsheet
 * <li>Periodically download with File>Download As...>Web Page (html, zipped)
 * <li>Run this program
 * <li>Update spreadsheet
 * </ul>
 * Needs to be automated, integrated in ST.
 * @author markdavis
 *
 */
public class ParseSpreadsheetAnnotations {
    enum Target {annotations, main};

    static Target TARGET = Target.main;

    private static final CLDRConfig CONFIG = CLDRConfig.getInstance();
    private static final String SOURCE_DIR = CLDRPaths.DATA_DIRECTORY + "emoji/annotations_import";
    private static final String TARGET_DIR = CLDRPaths.GEN_DIRECTORY + "emoji/annotations/";
    private static final CLDRFile ENGLISH = CONFIG.getEnglish();
    private static final Set<String> skipNames = ImmutableSet.of(
            "Internal", "Counts", "BadChars", "Collisions", "CollisionsInternal", "MissingInternal", "Temp", "Template",  
            "Sheet", "Summary", "Duplicates");

    enum TableState {
        NEED_CHARACTER_CODE, NEED_CODE
    }

    static final Splitter BAR = Splitter.onPattern("[।|၊]").trimResults().omitEmptyStrings();
    static final Joiner BAR_JOINER = Joiner.on(" | ");
    private static final String DEBUG_CODEPOINT = new StringBuilder().appendCodePoint(0x1f951).toString();
    //private static UnicodeSet OK_CODEPOINTS = new UnicodeSet("[\\-[:L:][:M:][:Nd:]\\u0020/|“”„’,.\u200C\u200D\u05F4\u104D\u2018]").freeze();
    private static Matcher SPACES = Pattern.compile("[\\s\u00A0\u00B7]+").matcher("");
    private static UnicodeSet OK_IN_JOINER_SEQUENCES = new UnicodeSet("[\\:,\u060C()]").freeze();

    enum ItemType {short_name, keywords, label, label_pattern, region, region_short};

    static Normalizer2 NFC = Normalizer2.getNFCInstance();

    private static boolean DEBUG_CHECK = false;
    
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
        final UnicodeMap<Annotations> existingAnnotations;
        final Map<String, String> existingLabels;
        final Map<String, String> existingLabelPatterns;
        final Transform<String,String> fixQuotes;

        public LocaleInfo(String localeName) {
            locale = localeName;
            final CLDRFile cldrFile = CONFIG.getCldrFactory().make(localeName, true);
            existingAnnotations = Annotations.getData(localeName);
            existingLabels = GenerateMissingAnnotations.loadLabels(cldrFile, "//ldml/characterLabels/characterLabel[");
            existingLabelPatterns = GenerateMissingAnnotations.loadLabels(cldrFile, "//ldml/characterLabels/characterLabelP");
            okNameCharacters = new UnicodeSet("[[:Nd:]\\u0020+]")
            .addAll(cldrFile.getExemplarSet("", WinningChoice.WINNING))
            .addAll(cldrFile.getExemplarSet("auxiliary", WinningChoice.WINNING))
            .addAll(cldrFile.getExemplarSet("punctuation", WinningChoice.WINNING));
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
            } else if (locale.equals("bs")) {
                okNameCharacters.addAll("-");        
            } else if (locale.equals("be")) {
                okNameCharacters.addAll("’ґ");        
            } else if (locale.equals("ky")) {
                okNameCharacters.addAll("ң");        
            }

            okNameCharacters.remove('|').remove(';').remove("'").remove('"').remove('}').remove('{').freeze();
            okKeywordCharacters = new UnicodeSet(okNameCharacters).add('|').freeze();
            okPatternChars = new UnicodeSet(okNameCharacters).add('{').add('}').freeze();

            String qStart = cldrFile.getStringValue("//ldml/delimiters/quotationStart");
            String qEnd = cldrFile.getStringValue("//ldml/delimiters/quotationEnd");

            fixQuotes = Transliterator.createFromRules("fixQuotes", 
                    "$single = \\' ;"
                            + "$double = \\\" ;"
                            + "$makeRight = [[:Z:][:Ps:][:Pi:]$] ;"
                            + "$makeRight {$double} ↔ " + qStart + " ;"
                            + "$double ↔ " + qEnd + " ;"
                            + "“ > " + qStart + " ;"
                            + "” > " + qEnd + " ;"
                            + "$makeRight {$single} ↔ ‘ ;"
                            + "$single ↔ ’;",
                            Transliterator.FORWARD);
        }

        UnicodeSet getCheckingSet(ItemType itemType) {
            switch(itemType) {
            case keywords: return okKeywordCharacters;
            case label_pattern: return okPatternChars;
            default: return okNameCharacters;
            }
        }

        private String check(String codePoint, ItemType itemType, String item, Output<Boolean> isOk) {
            isOk.value = Boolean.TRUE;
            item = SPACES.reset(TransliteratorUtilities.fromHTML.transform(item.trim())).replaceAll(" ").trim();
            String oldItem = item;
            item = fixQuotes.transform(item);
            //            if (!item.equals(oldItem)) {
            //                System.out.println(locale + " FIXED QUOTES: " + oldItem + " => " + item);
            //            }
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
                if (!badCodePoints.isEmpty() && !haveAlready(itemType, codePoint)) {
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
            if (item.contains("\"") || item.contains("'")) {
                int debug = 0;
            }
            return item;
        }

        private boolean haveAlready(ItemType itemType, String codePoint) {
            switch(itemType) {
            case short_name:
                Annotations info2 = existingAnnotations.get(codePoint);
                return info2 != null && info2.getShortName() != null && !info2.getShortName().isEmpty();
            case keywords:
                Annotations info = existingAnnotations.get(codePoint);
                return info != null && info.getKeywords() != null && !info.getKeywords().isEmpty();
            case label:
                return existingLabels.containsKey(codePoint);
            case label_pattern:
                return existingLabelPatterns.containsKey(codePoint);
            case region:
            case region_short:
            }
            return false;
        }
    }

    enum Problem {missing, badchar, duplicate, duplicateWithin}

    static class NewAnnotation {
        final String shortName;
        final String keywords;
        Set<Problem> shortNameProblems = EnumSet.noneOf(Problem.class);
        Set<Problem> annotationProblems = EnumSet.noneOf(Problem.class);
        public NewAnnotation(LocaleInfo localeInfo, String codePoint, String shortName, String keywords) {
            if (localeInfo.locale.equals("da") && codePoint.equals("☢")) {
                int debug = 0;
            }
            Output<Boolean> isOkOut = new Output<>();
            if (shortName.trim().equals("n/a")  || shortName.trim().equals("n / a") || shortName.trim().equals("???")) {
                shortName = "";
            }
            if (keywords.trim().equals("n/a")  || keywords.trim().equals("n / a") || keywords.trim().equals("???")) {
                shortName = "";
            }
            shortName = localeInfo.check(codePoint, ItemType.short_name, shortName, isOkOut);
            if (!isOkOut.value) {
                shortNameProblems.add(Problem.badchar);
            }
            if (shortName.isEmpty()) {
                shortNameProblems.add(Problem.missing);
            }
            this.shortName = shortName.isEmpty() ? null : shortName;

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
                annotationProblems.add(Problem.badchar);
            }
            if (keywords.isEmpty()) {
                annotationProblems.add(Problem.missing);
            }
            this.keywords = keywords.isEmpty() ? null : keywords;
        }

        @Override
        public String toString() {
            return shortName + " | " + keywords;
        }
    }

    static final Multimap<ItemType,String> ALL;
    static {
        UnicodeSet EXCLUDE = new UnicodeSet().add(0x2764).add(0x1F466).add(0x1F467);
        Multimap<ItemType,String> all = TreeMultimap.create();
        for (String s : GenerateMissingAnnotations.SORTED_EMOJI) {
            s = s.replace(Emoji.EMOJI_VARIANT_STRING,"");
            if (s.equals("🇺🇳")) {
                all.put(ItemType.region, "UN");
                all.put(ItemType.region_short, "UN");
                continue;
            }
            if (s.contains(Emoji.JOINER_STR) && EXCLUDE.containsSome(s)) {
                System.out.println("Skipping other Joiners: " + s + ", " + Utility.hex(s));
                continue;
            }
            all.put(ItemType.short_name, s);
            all.put(ItemType.keywords, s);
        }        
        for (String s : GenerateMissingAnnotations.CHARACTER_LABELS_AND_PATHS.keySet()) {
            getEnglishName(ItemType.label, s);
            all.put(ItemType.label, s);
        }
        for (String s : GenerateMissingAnnotations.CHARACTER_LABEL_PATTERNS_AND_PATHS.keySet()) {
            getEnglishName(ItemType.label_pattern, s);
            all.put(ItemType.label_pattern, s);
        }
        ALL = ImmutableMultimap.copyOf(all);
    }

    public static void main(String[] args) throws IOException {

        Map<String,UnicodeMap<NewAnnotation>> localeToNewAnnotations = new TreeMap<>();
        Map<String,Map<ItemType,Map<String,String>>> localeToLabelCodeToTrans = new TreeMap<>();
        Multimap<String,String> duplicates = TreeMultimap.create();
        Set<String> badLocales = new LinkedHashSet<>();


        // specials
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
        Map<String, LocaleInfo> localeInfoMap = new LinkedHashMap<>();

        Map<String,Multimap<ItemType,String>> missing = new TreeMap<>();
        Map<String,Set<String>> localeToLinesFound = new HashMap<>();

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
            Map<ItemType, Map<String,String>> itemTypeTolabelCodeToTrans = new LinkedHashMap<>();
            LocaleInfo localeInfo = new LocaleInfo(localeName);
            localeInfoMap.put(localeInfo.locale, localeInfo);

            Set<String> linesFound = new HashSet<>();
            UnicodeMap<NewAnnotation> newAnnotations = readFile(file.getParent(), name, localeInfo, itemTypeTolabelCodeToTrans, linesFound);
            localeToLinesFound.put(localeName, linesFound);

            TreeMultimap<ItemType, String> missingFromLocale = TreeMultimap.create();
            missingFromLocale.putAll(ALL);
            DEBUG_CHECK  = localeName.equals("is");
            if (DEBUG_CHECK) {
                System.out.println(missingFromLocale.get(ItemType.label));
            }
            missing.put(localeName, missingFromLocale);

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
                localeToLabelCodeToTrans.put(localeName, itemTypeTolabelCodeToTrans);

                UnicodeMap<Annotations> oldData = Annotations.getData(localeName);
                Multimap<String,String> oldNameToCode = TreeMultimap.create();
                Multimap<String,String> newNameToCode = TreeMultimap.create();

                for (Entry<String, Annotations> item : oldData.entrySet()) {
                    String codepoint = item.getKey();
                    NewAnnotation newItem = newAnnotations.get(codepoint);
                    final String shortName = item.getValue().getShortName();
                    if (shortName != null) {
                        oldNameToCode.put(shortName, item.getKey());
                        missingFromLocale.remove(ItemType.short_name, codepoint);
                    }
                    if (item.getValue().getKeywords() != null) {
                        missingFromLocale.remove(ItemType.keywords, codepoint);
                    }
                }
                for (Entry<String, String> entry : localeInfo.existingLabels.entrySet()) {
                    if (DEBUG_CHECK) {
                        System.out.println(missingFromLocale.get(ItemType.label));
                    }
                    missingFromLocale.remove(ItemType.label, entry.getKey());
                }
                for (Entry<String, String> entry : localeInfo.existingLabelPatterns.entrySet()) {
                    missingFromLocale.remove(ItemType.label_pattern, entry.getKey());
                }


                // find duplicate names

                for (Entry<String, NewAnnotation> item : newAnnotations.entrySet()) {
                    final NewAnnotation newAnnotation = item.getValue();
                    String newCode = item.getKey();
                    final String tts = newAnnotation.shortName;
                    if (tts == null) {
                        continue;
                    }
                    newNameToCode.put(tts, item.getKey());
                    Collection<String> oldCodes = oldNameToCode.get(tts);
                    if (oldCodes != null) {
                        for (String old : oldCodes) {
                            if (old.equals(newCode)) {
                                continue;
                            }
                            addDuplicates2(localeInfo, ItemType.short_name, tts, old, Collections.singleton(newCode), duplicates);
                            if (localeInfo.locale.equals("am") && newCode.equals("🐀")) {
                                for (String oldCode : oldCodes) {
                                    System.out.println(Utility.hex(oldCode));
                                }
                                int debug = 0;
                            }
                            newAnnotation.shortNameProblems.add(Problem.duplicate);
                        }
                    }
                }

                // now pick up the items that are duplicates within the sheet.
                for (Entry<String, NewAnnotation> item : newAnnotations.entrySet()) {
                    String newCode = item.getKey();
                    final NewAnnotation newAnnotation = item.getValue();
                    final String key = newAnnotation.shortName;
                    if (key == null) continue;
                    Collection<String> newCodes = newNameToCode.get(key);
                    if (newCodes.size() != 1) {
                        addDuplicates2(localeInfo, ItemType.short_name, key, null, newCodes, duplicates);
                        if (localeInfo.locale.equals("am") && newCode.equals("🐀")) {
                            for (String oldCode : newCodes) {
                                System.out.println(Utility.hex(oldCode));
                            }
                            int debug = 0;
                        }
                        newAnnotation.shortNameProblems.add(Problem.duplicateWithin);
                    }
                }

                //                for (Entry<String, Collection<String>> entry : nameToCode.asMap().entrySet()) {
                //                    if (entry.getValue().size() != 1) {
                //                        System.out.println("\t" + localeName + "\tduplicate: " + entry.getKey() + "\t" + entry.getValue());
                //                    }
                //                }
            }
        }

        for (Entry<String, Map<ItemType, Map<String, String>>> localeAndItemTypeToLabelCodeToTrans : localeToLabelCodeToTrans.entrySet()) {
            String locale = localeAndItemTypeToLabelCodeToTrans.getKey();
            final LocaleInfo localeInfo = localeInfoMap.get(locale);

            for ( Entry<ItemType, Map<String, String>> itemTypeAndLabelCodeToTrans : localeAndItemTypeToLabelCodeToTrans.getValue().entrySet()) {
                ItemType itemType = itemTypeAndLabelCodeToTrans.getKey();
                Map<String, String> labelCodeToTrans = itemTypeAndLabelCodeToTrans.getValue();
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
                        addDuplicates2(localeInfo, itemType, trans, null, labelCodes, duplicates);
                        labelCodesToRemove.addAll(labelCodes);
                    }
                }
                // remove them
                for (String label : labelCodesToRemove) {
                    labelCodeToTrans.remove(label);
                }
            }
        }

        try (PrintWriter out = FileUtilities.openUTF8Writer(TARGET_DIR, "spreadsheetSuspectChars.txt")) {
            out.println("#Locale\tType\tEmoji or Label\tEnglish Version\tNative Version\tSuspect characters\tGoogle translate (just for comparison)\tComments");
            for (LocaleInfo localeInfo : localeInfoMap.values()) {
                for (String value : localeInfo.badChars) {
                    out.println(localeInfo.locale + "\t" + value);
                }
            }
        }

        try (PrintWriter out = FileUtilities.openUTF8Writer(TARGET_DIR, "spreadsheetBadLocales.txt")) {
            for (String s : badLocales) {
                out.println(s + "\t" + ULocale.getDisplayName(s, "en"));
            }
        }

        try (PrintWriter out = FileUtilities.openUTF8Writer(TARGET_DIR, "spreadsheetDuplicates.txt")) {
            out.println("#Locale\tType\tCLDR Emoji\tEnglish\tSheet Emoji\tEnglish2\tNative Collision\tGoogle translate (just for comparison)\tFix for CLDR (in cell, not comment). Put fix for sheet in sheet.");
            for (Entry<String, Collection<String>> s : duplicates.asMap().entrySet()) {
                for (String value : s.getValue()) {
                    out.println(value);
                }
            }
        }

        Set<String> localesFound = new LinkedHashSet<>();
        try (PrintWriter out = FileUtilities.openUTF8Writer(TARGET_DIR, "modify_config_annotations.txt")) {
            for (Entry<String, UnicodeMap<NewAnnotation>> entry : localeToNewAnnotations.entrySet()) {
                String locale = entry.getKey();
                Multimap<ItemType, String> missingFromLocale = missing.get(locale);

                UnicodeMap<NewAnnotation> map = entry.getValue();
                UnicodeMap<Annotations> existingAnnotations = Annotations.getData(locale);
                for (Entry<String, NewAnnotation> entry2 : map.entrySet()) {
                    String codepoints = entry2.getKey();
                    if (codepoints.equals("⛹️‍♀")) {
                        int debug = 0;
                    }
                    NewAnnotation emoji = entry2.getValue();

                    if (emoji.annotationProblems.isEmpty()) {
                        writeConfigLine(out, locale, ItemType.keywords, codepoints, emoji.keywords, missingFromLocale, localesFound);
                    }
                    if (emoji.shortNameProblems.isEmpty()) {
                        writeConfigLine(out, locale, ItemType.short_name, codepoints, emoji.shortName, missingFromLocale, localesFound);
                    }
                }
            }
            out.println("# -m^(" + CollectionUtilities.join(localesFound, "|") + ")$");
        }
        localesFound.clear();
        try (PrintWriter out = FileUtilities.openUTF8Writer(TARGET_DIR, "modify_config_main.txt")) {
            for (Entry<String, Map<ItemType, Map<String, String>>> localeAndItemTypeToLabelCodeToTrans : localeToLabelCodeToTrans.entrySet()) {
                String locale = localeAndItemTypeToLabelCodeToTrans.getKey();
                Multimap<ItemType, String> missingFromLocale = missing.get(locale);
                for (Entry<ItemType, Map<String, String>> itemTypeAndLabelCodeToTrans : localeAndItemTypeToLabelCodeToTrans.getValue().entrySet()) {
                    ItemType itemType = itemTypeAndLabelCodeToTrans.getKey();
                    for (Entry<String, String> labelToTrans : itemTypeAndLabelCodeToTrans.getValue().entrySet()) {
                        String labelCode = labelToTrans.getKey();
                        String charLabel = labelToTrans.getValue();
                        if (DEBUG_CHECK && labelCode.startsWith("mi")) {
                            System.out.println(locale + "\t" + itemType + "\t" + labelCode + "\t" + charLabel);
                            int debug = 0;
                        }
                        writeConfigLine(out, locale, itemType, labelCode, charLabel, missingFromLocale, localesFound);
                    }
                }
            }
            out.println("# -m^(" + CollectionUtilities.join(localesFound, "|") + ")$");
        }

        try (PrintWriter out = FileUtilities.openUTF8Writer(TARGET_DIR, "spreadsheetMissing.txt")) {
            for (Entry<String, Multimap<ItemType, String>> entry : missing.entrySet()) {
                String locale = entry.getKey();
                if (locale.equals("pt_PT")) {
                    continue;
                }
                for ( Entry<ItemType, Collection<String>> entry2 : entry.getValue().asMap().entrySet()) {
                    final ItemType itemType = entry2.getKey();
                    for (String key : entry2.getValue()) {
                        out.println(locale + "\t" + itemType + "\t" + key + "\t" + getKey(itemType, key));
                    }
                }
            }
        }
        try (PrintWriter out = FileUtilities.openUTF8Writer(TARGET_DIR, "spreadsheetMissingSheet.txt")) {
            for (Entry<String, Multimap<ItemType, String>> entry : missing.entrySet()) {
                String locale = entry.getKey();
                if (locale.equals("pt_PT")) {
                    continue;
                }
                Set<String> linesFound = localeToLinesFound.get(locale);
                int count = linesFound.size()+1;
                Set<String> missingLines = new LinkedHashSet<>();
                Set<String> seenAnnotation = new HashSet<>();

                for ( Entry<ItemType, Collection<String>> entry2 : entry.getValue().asMap().entrySet()) {
                    final ItemType itemType = entry2.getKey();
                    for (String key : entry2.getValue()) {
                        if (linesFound.contains(key)) {
                            continue;
                        }
                        if (itemType == ItemType.short_name || itemType == ItemType.keywords) {
                            if (seenAnnotation.contains(key)) {
                                continue;
                            }
                            seenAnnotation.add(key);
                            final String engShortName = GenerateMissingAnnotations.ENGLISH_ANNOTATIONS.getShortName(key);
                            final Set<String> engKeys = GenerateMissingAnnotations.ENGLISH_ANNOTATIONS.getKeywords(key);
                            GenerateMissingAnnotations.showMissingLine(missingLines, ++count, GenerateMissingAnnotations.getKey(key), 
                                    engShortName, engKeys, "", "", "n/a", Collections.singleton("n/a"));
                        } else {
                            String engShortName = getEnglishName(itemType, key);
                            if (engShortName == null) {
                                throw new IllegalArgumentException();
                            }
                            GenerateMissingAnnotations.showMissingLine(missingLines, ++count, key, 
                                    engShortName, Collections.singleton("n/a"), "", "", "n/a", Collections.singleton("n/a"));
                        }
                    }
                }
                if (!missingLines.isEmpty()) {
                    out.println("\n" + locale + "\n");
                    for (String s : missingLines) {
                        out.println(s);
                    }
                }
            }
        }
    }

    private static String getKey(ItemType itemType, String key) {
        return itemType == ItemType.keywords || itemType == ItemType.short_name 
                ? GenerateMissingAnnotations.getKey(key)
                        : key;
    }

    private static void addDuplicates2(LocaleInfo localeInfo, ItemType itemType, final String translation, String CLDR, Collection<String> keys,
            Multimap<String, String> duplicates) {
        ArrayList<String> keysList = new ArrayList<>(keys);
        for (int i = 0; i < keysList.size(); ++i) {
            final String cldrItem = CLDR == null ? "n/a" : CLDR;
            final String englishName = CLDR == null ? "n/a" : getEnglishName(itemType, CLDR);
            StringBuilder ss = new StringBuilder(localeInfo.locale)
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
            duplicates.put(localeInfo.locale, ss.toString());
        }
    }

    private static String getEnglishName(ItemType itemType, String code1) {
        String englishName;
        switch (itemType) {
        case keywords:
            final Set<String> keywords = GenerateMissingAnnotations.ENGLISH_ANNOTATIONS.getKeywords(code1);
            englishName = keywords == null ? null : CollectionUtilities.join(keywords, " | ");
            break;
        case short_name:
            englishName = GenerateMissingAnnotations.ENGLISH_ANNOTATIONS.getShortName(code1);
            break;
        case label: 
        case label_pattern: 
        case region:
        case region_short:
            englishName = ENGLISH.getStringValue(getLabelPath(itemType, code1));
            //englishName = "United Nations";
            //englishName = "UN";
            break;
        default: 
            throw new IllegalArgumentException();
        }
        if (englishName == null) {
            throw new IllegalArgumentException(); 
        }
        return englishName;
    }


    private static String getLabelPath(ItemType itemType, String code1) {
        switch (itemType) {
        case label:
            return "//ldml/characterLabels/characterLabel[@type=\"" + code1 + "\"]";
        case label_pattern:
            return "//ldml/characterLabels/characterLabelPattern[@type=\"" + code1 + "\"]" 
            + (code1.equals("strokes") ? "[@count=\"other\"]" : "");
        case region:
            return "//ldml/localeDisplayNames/territories/territory[@type=\"" + code1 + "\"]";
        case region_short:
            return "//ldml/localeDisplayNames/territories/territory[@type=\"" + code1 + "\"][@alt=\"short\"]";
        case keywords:
            return "//ldml/annotations/annotation[@cp=\"" + code1 + "\"]";
        case short_name:
            return "//ldml/annotations/annotation[@cp=\"" + code1 + "\"][@type=\"tts\"]";
        default:
            throw new IllegalArgumentException();
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
    NKEYWORDS = 7,
    STOP = 8;
    ;

    static final UnicodeSet DIGITS = new UnicodeSet('0','9').freeze();
    static final boolean debug = false;
    private static final boolean CHECK_MISSING = false;
    private static UnicodeSet ALPHA = new UnicodeSet("[[:L:][:M:][:N:]]").freeze();

    private static UnicodeMap<NewAnnotation> readFile(String parent, String name, LocaleInfo localeInfo,
            Map<ItemType, Map<String, String>> itemTypeTolabelCodeToTrans,
            Set<String> linesFound) {
        UnicodeMap<NewAnnotation> newAnnotations = new UnicodeMap<>();

        try (BufferedReader in = FileUtilities.openUTF8Reader(parent, name)) {
            SimpleHtmlParser simple = new SimpleHtmlParser().setReader(in);
            StringBuilder result = new StringBuilder();
            String codePoint = null;
            String ename = "";
            String shortName = "";
            String annotations = "";
            boolean isFirst = true;
            int tdCount = 0;
            boolean inPop = true;
            boolean isLabel = false;
            main:
                while (true) {
                    Type type = simple.next(result);
                    String resultString = result.toString();
                    if (localeInfo.locale.equals("af") && "UN".equals(codePoint)) {
                        System.out.println(inPop + "\t" + tdCount + "\t" + codePoint + "\t" + type + "\t«" + resultString + "»");
                    }
                    switch (type) {
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
                        switch (tdCount) {
                        case CODE:
                            isLabel = false;
                            if (resultString.length() < 2) { // hack, since there are a few special rows inserted.
                                break;
                            } else if (isFirst) {
                                isFirst = false;
                            } else if (resultString.equals("_1f1fa_1f1f3")) {
                                codePoint = "UN";
                                isLabel = true;
                            } else if (resultString.charAt(0) == '_') {
                                codePoint = Utility.fromHex(resultString.substring(1).replace('_', ' '));
                            } else {
                                codePoint = resultString;
                                isLabel = true;
                            }
                            linesFound.add(codePoint);
                            break;
                        case ENAME:
                            ename = addWithBrHack(ename, resultString);
                            break;
                        case NNAME:
                            if (codePoint != null) {
                                shortName = addWithBrHack(shortName, resultString);
                            }
                            break;
                        case NKEYWORDS:
                            if (codePoint != null) {
                                annotations = addWithBrHack(annotations, resultString);
                            }
                            break;
                        case STOP:
                            if (codePoint == null) {
                                break;
                            }
                            NewAnnotation old = newAnnotations.get(codePoint);
                            if (old != null) {
                                throw new IllegalArgumentException("Duplicate code point: " + codePoint);
                            }
                            if (isLabel) {
                                if (DEBUG_CHECK) { 
                                    if (codePoint.startsWith("mi")) {
                                        int debug = 0;
                                    }
                                }
                                if (annotations.equals("n/a")) {
                                    annotations = "";
                                }
                                // for certain labels, the pattern is in the short name
                                if (!annotations.isEmpty() 
                                        && (codePoint.equals("UN") || codePoint.equals("keycap"))) { 
                                    // only keycaps and UN, special cases
                                    final ItemType itemType = codePoint.equals("keycap") 
                                            ? ItemType.label 
                                                    : ItemType.region_short;
                                    if (annotations.contains("|")) {
                                        annotations = getShortest(annotations);
                                    }
                                    Output<Boolean> isOkOut = new Output<>();
                                    annotations = localeInfo.check(codePoint, itemType, annotations, isOkOut);
                                    if (isOkOut.value) { // skipping bad value.
                                        addItem(itemTypeTolabelCodeToTrans, itemType, codePoint, annotations);
                                    } else {
                                        annotations = ""; // signal for hack below
                                    }
                                }
                                if (!shortName.isEmpty()) {
                                    final ItemType itemType = getItemType(codePoint, ename);
                                    Output<Boolean> isOkOut = new Output<>();
                                    if ((codePoint.equals("keycap") || codePoint.equals("emoji"))) {
                                        shortName = shortName.replace("{0}","").trim();
                                        while (!shortName.isEmpty()) {
                                            int last = shortName.codePointBefore(shortName.length());
                                            if (ALPHA.contains(last)) {
                                                addItem(itemTypeTolabelCodeToTrans, ItemType.label, codePoint, annotations);
                                                break;
                                            }
                                            shortName = shortName.substring(0,shortName.length()-Character.charCount(last));
                                        }
                                    }
                                    shortName = localeInfo.check(codePoint, itemType, shortName, isOkOut);
                                    if (isOkOut.value) { // skipping bad value.
                                        // hack for keycap, emoji
                                        addItem(itemTypeTolabelCodeToTrans, itemType, codePoint, shortName);
                                    }
                                }
                            } else if (shortName.isEmpty() && annotations.isEmpty()) {
                                if (CHECK_MISSING) {
                                    System.out.println("Missing value. Code point: " + codePoint + "\tname:" + shortName + "\tkeywords:" + annotations);
                                }
                            } else {
                                final NewAnnotation newAnnotation = new NewAnnotation(localeInfo, codePoint, shortName, annotations);
                                if (newAnnotation.shortNameProblems.isEmpty() 
                                        || newAnnotation.annotationProblems.isEmpty()) {
                                    newAnnotations.put(codePoint, newAnnotation);
                                }
                            }
                            codePoint = null;
                            ename = shortName = annotations = "";

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

    private static ItemType getItemType(String codePoint, String ename2) {
        return codePoint.equals("UN") 
                ? ItemType.region 
                        : ename2.contains("{0}") // GenerateMissingAnnotations.CHARACTER_LABEL_PATTERNS_AND_PATHS.containsKey(codePoint)
                        ? ItemType.label_pattern 
                                : ItemType.label;
    }

    private static String getShortest(String annotations) {
        String shortest = null;
        for (String s : BAR.splitToList(annotations)) {
            if (shortest == null || s.length() < shortest.length()) {
                shortest = s;
            }
        }
        return shortest;
    }

    private static void addItem(Map<ItemType, Map<String, String>> itemTypeTolabelCodeToTrans, final ItemType itemType, String codePoint, String translation) {
        Map<String, String> labelCodeToTrans = itemTypeTolabelCodeToTrans.get(itemType);
        if (labelCodeToTrans == null) {
            itemTypeTolabelCodeToTrans.put(itemType, labelCodeToTrans = new TreeMap<String, String>());
        }
        labelCodeToTrans.put(codePoint, translation);
    }

    private static String addWithBrHack(String annotations, String resultString) {
        if (annotations.isEmpty()) {
            annotations = resultString;
        } else {
            annotations += " " + resultString;
        }
        return annotations;
    }

    private static void writeConfigLine(PrintWriter out, String locale, ItemType itemType, String codepoints, String value,
            Multimap<ItemType, String> missingFromLocale, Set<String> localesFound) {
        if (DEBUG_CHECK && codepoints.equals("downwards_arrows")) {
            int debug = 0;
        }
        if (!missingFromLocale.containsEntry(itemType, codepoints)) {
            return;
        }
        out.println("locale=" + locale
                + " ; action=addNew ; new_path=" + getLabelPath(itemType, codepoints)
                + " ; new_value=" + TransliteratorUtilities.toXML.transform(value)
                );
        missingFromLocale.remove(itemType, codepoints);
        localesFound.add(locale);
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