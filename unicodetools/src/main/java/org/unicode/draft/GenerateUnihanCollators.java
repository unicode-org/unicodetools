package org.unicode.draft;

import com.google.common.base.Splitter;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeMap.EntryRange;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R4;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUException;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Differ;
import org.unicode.cldr.util.MultiComparator;
import org.unicode.cldr.util.XEquivalenceClass;
import org.unicode.draft.ComparePinyin.PinyinSource;
import org.unicode.jsp.FileUtilities.SemiFileReader;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.text.UCA.RadicalStroke;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.Normalizer;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

public class GenerateUnihanCollators {
    private static final boolean DEBUG = false;

    private static String version = CldrUtility.getProperty("UVERSION");

    static {
        System.out.println(
                "To make files for a different version of unicode, use -DUVERSION=x.y.z");
        if (version == null) {
            version = Settings.latestVersion;
        } else {
            System.out.println("Resetting default version to: " + version);
            Default.setUCD(version);
        }
    }

    private static final IndexUnicodeProperties IUP = IndexUnicodeProperties.make(version);
    private static final RadicalStroke radicalStroke = new RadicalStroke(version);
    private static final char INDEX_ITEM_BASE = '\u2800';

    private enum FileType {
        txt,
        xml
    }

    private enum InfoType {
        radicalStroke("\uFDD2"),
        stroke("\uFDD1"),
        pinyin("\uFDD0");
        final String base = "\uFDD0";

        InfoType(String base) {
            // this.base = base;
        }
    }

    private enum OverrideItems {
        keepOld,
        keepNew
    }

    private static final Transform<String, String> fromNumericPinyin =
            Transliterator.getInstance("NumericPinyin-Latin;nfc");
    private static final Transliterator toNumericPinyin =
            Transliterator.getInstance("Latin-NumericPinyin;nfc");

    private static final Normalizer nfkd = Default.nfkd();
    private static final Normalizer nfd = Default.nfd();
    private static final Normalizer nfc = Default.nfc();

    private static final UnicodeSet PINYIN_LETTERS =
            new UnicodeSet("['a-uw-zàáèéìíòóùúüāēěīōūǎǐǒǔǖǘǚǜ]").freeze();

    // these should be ok, eve if we are not on an old version

    private static final UnicodeSet NOT_NFC = new UnicodeSet("[:nfc_qc=no:]").freeze();
    private static final UnicodeSet NOT_NFD = new UnicodeSet("[:nfd_qc=no:]").freeze();
    private static final UnicodeSet NOT_NFKD = new UnicodeSet("[:nfkd_qc=no:]").freeze();

    // specifically restrict this to the set version. Theoretically there could be some variance in
    // ideographic, but it isn't worth worrying about

    private static final UnicodeSet UNIHAN_LATEST =
            new UnicodeSet("[[:ideographic:][:script=han:]]").removeAll(NOT_NFC).freeze();
    private static final UnicodeSet UNIHAN =
            version == null
                    ? UNIHAN_LATEST
                    : new UnicodeSet("[:age=" + version + ":]").retainAll(UNIHAN_LATEST).freeze();

    static {
        if (!UNIHAN.contains(0x2B820)) {
            throw new ICUException(Utility.hex(0x2B820) + " not supported");
        }
    }

    private static Matcher unicodeCp = Pattern.compile("^U\\+(2?[0-9A-F]{4})$").matcher("");
    private static final HashMap<String, Boolean> validPinyin = new HashMap<String, Boolean>();
    private static final Collator pinyinSort =
            Collator.getInstance(new ULocale("zh@collator=pinyin"));
    private static final Collator strokeSort =
            Collator.getInstance(new ULocale("zh@collator=stroke"));

    private static final Comparator<String> codepointComparator =
            new UTF16.StringComparator(true, false, 0);
    private static final Comparator<String> nfkdComparator =
            new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    if (!nfkd.isNormalized(o1) || !nfkd.isNormalized(o2)) {
                        final String s1 = nfkd.normalize(o1);
                        final String s2 = nfkd.normalize(o2);
                        final int result = codepointComparator.compare(s1, s2);
                        if (result != 0) {
                            return result; // otherwise fall through to codepoint comparator
                        }
                    }
                    return codepointComparator.compare(o1, o2);
                }
            };

    private static final UnicodeMap<Integer> bestStrokesS = new UnicodeMap<Integer>();
    private static final UnicodeMap<Integer> bestStrokesT = new UnicodeMap<Integer>();
    private static final UnicodeMap<String> kTotalStrokes = IUP.load(UcdProperty.kTotalStrokes);
    private static final Splitter ONBAR = Splitter.on('|').trimResults();
    private static final Splitter ONCOMMA = Splitter.on(',').trimResults();

    static {
        for (EntryRange<String> s : kTotalStrokes.entryRanges()) {
            List<String> parts = ONBAR.splitToList(s.value);
            Integer sValue = Integer.parseInt(parts.get(0));
            Integer tValue = parts.size() == 1 ? sValue : Integer.parseInt(parts.get(1));
            if (s.string != null) {
                bestStrokesS.put(s.string, sValue);
                bestStrokesT.put(s.string, tValue);
            } else {
                bestStrokesS.putAll(s.codepoint, s.codepointEnd, sValue);
                bestStrokesT.putAll(s.codepoint, s.codepointEnd, tValue);
            }
        }
    }

    private static UnicodeMap<Row.R2<String, String>> bihuaData =
            new UnicodeMap<Row.R2<String, String>>();

    private static final UnicodeMap<String> kRSUnicode =
            IUP.load(UcdProperty.kRSUnicode).cloneAsThawed();
    private static final UnicodeMap<String> kSimplifiedVariant =
            IUP.load(UcdProperty.kSimplifiedVariant);
    private static final UnicodeMap<String> kTraditionalVariant =
            IUP.load(UcdProperty.kTraditionalVariant);

    private static final UnicodeMap<Set<String>> mergedPinyin = new UnicodeMap<Set<String>>();
    private static final UnicodeSet originalPinyin;

    private static final boolean only19 = System.getProperty("only19") != null;
    private static UnicodeMap<String> radicalMap = new UnicodeMap<String>();

    // kHanyuPinyin, space, 10297.260: qīn,qìn,qǐn,
    // [a-z\x{FC}\x{300}-\x{302}\x{304}\x{308}\x{30C}]+(,[a-z\x{FC}\x{300}-\x{302}\x{304}\x{308}\x{30C}]
    // kMandarin, space, [A-Z\x{308}]+[1-5] // 3475=HAN4 JI2 JIE2 ZHA3 ZI2
    // kHanyuPinlu, space, [a-z\x{308}]+[1-5]\([0-9]+\) 4E0A=shang4(12308)
    // shang5(392)
    private static UnicodeMap<String> bestPinyin = new UnicodeMap<>();

    // while these use NFKD, for the repertoire they apply to it should work.
    private static Transform<String, String> noaccents =
            Transliterator.getInstance("nfkd; [[:m:]-[\u0308]] remove; nfc");

    private static UnicodeSet INITIALS =
            new UnicodeSet("[b c {ch} d f g h j k l m n p q r s {sh} t w x y z {zh}]").freeze();
    private static UnicodeSet FINALS =
            new UnicodeSet(
                            "[a {ai} {an} {ang} {ao} e {ei} {en} {eng} {er} i {ia} {ian} {iang} {iao} {ie} {in} {ing} {iong} {iu} o {ong} {ou} u {ua} {uai} {uan} {uang} {ue} {ui} {un} {uo} ü {üe}]")
                    .freeze();
    private static final int NO_STROKE_INFO = Integer.MAX_VALUE;

    // We need to quote at least the collation syntax characters, see
    // http://www.unicode.org/reports/tr35/tr35-collation.html#Rules
    private static UnicodeSet NEEDSQUOTE =
            new UnicodeSet("[_[:pattern_syntax:][:pattern_whitespace:]]").freeze();

    private static final XEquivalenceClass<Integer, Integer> variantEquivalents =
            new XEquivalenceClass<Integer, Integer>();
    private static final String INDENT = "               ";
    private static final UnicodeMap<String> kMandarin = IUP.load(UcdProperty.kMandarin);

    static {
        System.out.println("kRSUnicode " + kRSUnicode.size());

        new BihuaReader().process(GenerateUnihanCollators.class, "bihua-chinese-sorting.txt");
        getBestStrokes();
        new PatchStrokeReader(bestStrokesS, Pattern.compile("\\t\\s*"))
                .process(GenerateUnihanCollators.class, "ucs-strokes-ext-e.txt");
        RsInfo.addToStrokeInfo(bestStrokesS, true);

        bestStrokesT.putAll(bestStrokesS);
        // patch the values for the T strokes
        new PatchStrokeReader(bestStrokesT, SemiFileReader.SPLIT)
                .process(GenerateUnihanCollators.class, "patchStrokeT.txt");
        RsInfo.addToStrokeInfo(bestStrokesT, false);

        new MyFileReader().process(GenerateUnihanCollators.class, "CJK_Radicals.csv");
        if (false) {
            for (final String s : radicalMap.values()) {
                System.out.println(s + "\t" + radicalMap.getSet(s).toPattern(false));
            }
        }
        addRadicals(kRSUnicode);
        closeUnderNFKD("Unihan", kRSUnicode);

        System.out.println("UcdProperty.kMandarin " + kMandarin.size());
        for (final String s : kMandarin.keySet()) {
            final String original = kMandarin.get(s);
            String source = original.toLowerCase();
            source = fromNumericPinyin.transform(source);
            addAllKeepingOld(s, original, PinyinSource.m, ONBAR.split(source));
        }

        // all kHanyuPinlu readings first; then take all kXHC1983; then
        // kHanyuPinyin.

        final UnicodeMap<String> kHanyuPinlu = IUP.load(UcdProperty.kHanyuPinlu);
        for (final String s : kHanyuPinlu.keySet()) {
            final String original = kHanyuPinlu.get(s);
            String source = original.replaceAll("\\([0-9]+\\)", "");
            source = fromNumericPinyin.transform(source);
            addAllKeepingOld(s, original, PinyinSource.l, ONBAR.split(source));
        }

        // kXHC1983
        // ^[0-9,.*]+:*[a-zx{FC}x{300}x{301}x{304}x{308}x{30C}]+$
        final UnicodeMap<String> kXHC1983 = IUP.load(UcdProperty.kXHC1983);
        System.out.println("UcdProperty.kXHC1983 " + kXHC1983.size());
        for (final String s : kXHC1983.keySet()) {
            final String original = kXHC1983.get(s);
            String source = nfc.normalize(original);
            source = source.replaceAll("([0-9,.*]+:)*", "");
            addAllKeepingOld(s, original, PinyinSource.x, ONBAR.split(source));
        }

        Pattern stuffToRemove = Pattern.compile("(\\d{5}\\.\\d{2}0,)*\\d{5}\\.\\d{2}0:");
        final UnicodeMap<String> kHanyuPinyin = IUP.load(UcdProperty.kHanyuPinyin);
        System.out.println("UcdProperty.kHanyuPinyin " + kHanyuPinyin.size());
        for (final String s : kHanyuPinyin.keySet()) {
            final String original2 = kHanyuPinyin.get(s);
            for (String original : ONBAR.split(original2)) {
                String source = nfc.normalize(original);
                source = stuffToRemove.matcher(source).replaceAll("");
                // only
                // for
                // medial
                // source = source.replaceAll("\\s*(\\d{5}\\.\\d{2}0,)*\\d{5}\\.\\d{2}0:", ",");
                addAllKeepingOld(s, original, PinyinSource.p, ONCOMMA.split(source));
            }
        }

        originalPinyin = mergedPinyin.keySet().freeze();
        int count = mergedPinyin.size();
        System.out.println("unihanPinyin original size " + count);

        addBihua();
        count += showAdded("bihua", count);

        addRadicals();
        count += showAdded("radicals", count);

        addEquivalents(kTraditionalVariant);
        addEquivalents(kSimplifiedVariant);

        count += addPinyinFromVariants("STVariants", count);
        // count += showAdded("kTraditionalVariant", count);

        // addVariants("kSimplifiedVariant", kSimplifiedVariant);
        // count += showAdded("kSimplifiedVariant", count);

        new PatchPinyinReader().process(GenerateUnihanCollators.class, "patchPinyin.txt");

        closeUnderNFKD("Pinyin", bestPinyin);

        printExtraPinyinForUnihan();
        printExtraStrokesForUnihan();
    }

    public static void main(String[] args) throws Exception {
        showOldData(pinyinSort, "pinyin1.8.txt", false);
        showOldData(strokeSort, "stroke1.8.txt", false);
        showOldData(Collator.getInstance(new ULocale("ja")), "japanese1.8.txt", true);

        final UnicodeSet zh = FindHanSizes.getMostFrequent("zh", 0.999);
        final UnicodeSet zh_Hant = FindHanSizes.getMostFrequent("zh_Hant", 0.999);

        // Matcher charsetMatcher = Pattern.compile("GB2312|GBK|Big5|Big5-HKSCS").matcher("");
        final UnicodeSet GB2312 = FindHanSizes.getCharsetRepertoire("GB2312");
        final UnicodeSet GBK = FindHanSizes.getCharsetRepertoire("GBK");
        final UnicodeSet Big5 = FindHanSizes.getCharsetRepertoire("Big5");
        final UnicodeSet Big5_HKSCS = FindHanSizes.getCharsetRepertoire("Big5-HKSCS");

        // TGH 2013 set of characters.
        // From CLDR-16571:
        // GB18030-2022 has 3 implementation levels ... Level 2 ... requires that
        // collations support the 8,105 characters in
        // 通用规范汉字表 (Tōngyòng Guīfàn Hànzìbiǎo; aka TGH 2013),
        // “General Standard Chinese Character List”.
        String tghPattern =
                "[㑇㑊㕮㘎㙍㙘㙦㛃㛚㛹㟃㠇㠓㤘㥄㧐㧑㧟㫰㬊㬎㬚㭎㭕㮾㰀㳇㳘㳚㴔㵐㶲㸆㸌㺄㻬㽏㿠䁖䂮䃅䃎䅟䌹䎃䎖䏝䏡䏲䐃䓖䓛䓨䓫䓬䗖䗛䗪䗴"
                        + "䜣䝙䢺䢼䣘䥽䦃䲟䲠䲢䴓-䴙䶮一丁七万-下不-丑专-世丘-丞丢两严丧个丫中丰串临丸-主丽举乂乃久么义之-乐乒-乔乖乘乙"
                        + "乜-乡书乩买乱乳乸乾了予争事-亏云-井亘亚些亟亡亢交-亩享-亮亲亳亵亶亸-人亿-仇仉-介仍从仑仓-仙仝-仟仡代-以仨"
                        + "仪-们仰仲仳仵-价任份仿企伈-伋伍-休众-伛伞-传伢-伧伪伫伭伯估伲伴伶伸伺似-伾佁佃但位-佑体何-你佣-佥佩佬佯佰佳佴"
                        + "佶佸佺-使侁-侄侈侉例侍侏侑侔侗侘供依侠侣侥-侪侬侮侯侴侵侹便促-俅俊俍-俑俗-俚俜-俟信俣俦俨-俫俭-俯俱俳俵俶俸俺俾"
                        + "倌倍倏倒-倕倘-倚倜倞借倡倥-倪倬-倮倴债-值倾偁偃假偈偌偎偏偓偕做停偡健偬偭偰偲偶偷偻偾-傀傃傅傈傉傍傒傕傣傥傧-傩催"
                        + "傲傺傻僇僎像僔僖僚僦僧僬-僮僰僳僵僻儆儇儋儒儡儦儳儴儿-允元-兆先光克免兑兔-兖党兜兢入全八-兮兰共关-兹养-兽冀冁内冈"
                        + "冉册再冏冒冔冕冗写军农冠冢冤冥冬冮-决况-冷冻-冽净凄准凇凉凋凌减凑凓凘凛凝几凡凤凫凭凯凰凳凶凸-函凿-刁刃分-刈刊刍刎"
                        + "刑划刖-创初删判刨利别-刮到刳制-刻刽刿-剃剅削-前剐剑剔-剖剜剞剟剡剥剧剩剪副割剽剿劁劂劄劈劐劓力劝-劣动-劭励-劳劼"
                        + "劾势勃勇勉勋勍勐勒勔勖勘勚募勠勤勰勺勾-匀包匆匈匍匏匐匕-北匙匜匝匠匡匣匦匪匮匹-匼匾匿十千卅升-半华协卑-卓单-南博卜"
                        + "卞-卤卦卧卫卬卮-危即-卵卷卸卺卿厂厄-历厉压-厍厕厖厘厚厝原厢厣厥厦厨厩厮去厾县叁参叆-反发叔-叛叟叠口-另叨-右"
                        + "叵-叹叻-叽吁吃各吆合-吊同-吓吕-吗君吝-吡吣否-吩含-启吱吲吴吵吸吹吻-吾呀呃呆-呈告呋呐呒-呙呛呜呢-呤呦周呱-味"
                        + "呵-呸呻-命咀咂咄咆咇咉咋-咐咒咔-咖咙-咛咝咡咣-咬咯咱咳咴咸咺咻咽咿-哄哆-哉哌-哕哗哙哚哝-哟哢哥-哪哭哮哱-哳哺"
                        + "哼哽哿唁唆唇唉唏-唑唔唛唝唠唢-唤唧唪唬售-唱唳唵唷唼唾唿啁啃啄商啉啊啐啕啖啜啡啤-啧啪-啮啰啴-啸啻啼啾喀-善喆-喋喏"
                        + "喑喔喘喙喜喝喟喤喧喱喳喵喷喹喻喽喾嗄嗅嗉嗌嗍嗐-嗔嗖嗜-嗟嗡嗣-嗦嗨嗪-嗬嗯嗲嗳嗵嗷嗽嗾嘀嘁嘈嘉嘌嘎嘏嘘嘚嘛嘞嘟嘡嘣嘤嘧"
                        + "嘬嘭嘱嘲嘴嘶嘹嘻嘿噀噂噇噌-噎噔噗-噙噜噢噤器-噬噱噶噻噼嚄-嚆嚎嚏嚓嚚嚣嚭嚯嚷嚼囊囔囚四回-团囤囫园困囱围囵囷囹固"
                        + "国-囿圃圄圆圈-圊圌圐圙圜土圢圣在-地圲圳圹-圻圾址坂均坉-坒块坚-坡坤-坦坨-坭坯坰坳坷坻-坽垂-垄垆垈型-垏垒垓垕"
                        + "垙-垛垞-垤垦垧垩垫垭-垯垱垲垴垵垸垺垾垿埂埃埆埇埋埌城埏埒埔埕埗-埚埝域埠埤埪埫埭埯埴埵埸-基埼埽堂堃堆堇堉堋-堎堐堑"
                        + "堕堙堞堠堡堤堧堨堪堰堲堵堼-堾塄-塆塌塍塑塔塘塝塞塥填塬塱塾墀墁境墅墈墉墐墒墓墕墘-墚增墟墡墣墦墨墩墼壁壅壑壕壤士壬壮声"
                        + "壳壶壸壹处备复夏夐夔-外夙多夜够夤夥大天-夯失头夷-夺夼奁奂奄奇-奉奋奎奏契奓-奘奚奠-奢奥奭女奴奶奸她好妁-妄妆-妈妊"
                        + "妍妒妓妖-妙妞妣-妥妧-妫妭-妯妲妹妻妾姆姈姊始姐-委姗姘姚姜-姞姣-姥姨姬姮姱姶姹姻姽姿-威娃-娉娌娑娓娘娜娟娠娣娥娩"
                        + "娱娲娴-娶娼婀婆婉婊婌婍婕婘婚婞婠婢婤婧婪婫婳-婷婺-婼婿媂媄媆媒媓媖媚媛媞媪媭媱-媳媵媸媾嫁嫂嫄嫉嫌嫒嫔-嫖嫘嫚嫜嫠嫡"
                        + "嫣嫦嫩-嫫嫭嫱嫽嬉嬖嬗嬛嬥嬬嬴嬷嬿孀孅子孑孓-孝孟孢-学孩孪孬孰孱孳孵孺孽宁它-宅宇-安宋完宏宓宕宗-实宠-宧宪-宬宰"
                        + "害-家宸容宽-宿寁寂寄-寇富寐寒寓寝-察寡寤寥寨寮寰寸-导寿封射将尉尊小少尔-尖尘尚尜尝尢尤尥尧尨尪尬就尴尸-屃居屈-屋"
                        + "屎-屑展屙属屠屡屣履屦屯山屹屺屼屾屿岁岂岈岊岌岍岐岑岔岖-岜岞岠岢岣岨岩岫-岭岱岳岵岷岸岽岿峁-峄峋峒峗-峙峛峡峣-峨峪"
                        + "峭峰峱峻峿-崄崆崇崌崎崒崔崖崚崛崞崟崡崤崦崧崩崭崮崴崶崽-崿嵁嵅嵇嵊-嵌嵎嵖嵘嵚嵛嵝嵩嵫嵬嵯嵲嵴嶂嶅嶍嶒嶓嶙嶝嶟嶦嶲嶷巅"
                        + "巇巉巍川州巡巢工-巩巫差巯己-巴巷巽巾币-布帅帆师希帏-帑帔-帖帘-帝帡带-帨席帮帱帷常帻-帽幂幄幅幌幔-幖幛幞幡幢幪"
                        + "干-年并幸幺-幽广庄庆庇床庋序-庑库-店庙庚府庞-庠庤-座庭庱庳庵-庹庼庾廆廉-廋廑-廓廖廙廛廨廪延廷建廿-弄弆-弈弊弋"
                        + "式弑弓引弗弘弛弟张弢弥-弩弭弯弱弶弸-强弼彀归当录-彘彝彟形彤彦彧彩彪彬彭彰影彳彷役彻彼往-徂径待徇-律徐徒徕得-徙徛徜"
                        + "御徨循徭微徵德徼徽心必忆忉忌忍忏-忒忖-忙忝忞忠忡忤忧忪快忭忮忱忳念忸忺忻忽-怆怊怍-怏怒怔-怖怙怛-思怠怡急-怫怯怵总"
                        + "怼怿恁-恃恋恍恐恒-恕恙恚恝恢-恤恧-恭息恰恳恶恸-恽恿悃悄悆悈悉悌悍悒悔悖悚悛悝悟悠悢患悦您悫-悭悯-悲悴悸悻悼情-惇"
                        + "惊惋惎惑惔惕惘-惝惟惠惦-惩惫-惰想惴惶惹惺愀愁愃愆愈愉愍-愐愔愕愚感愠愣愤愦愧愫愭愿慆慈慊慌慎慑慕慝慢慥慧慨慬慭慰慵慷"
                        + "憋憎憔憕憙憧-憩憬憭憷憺憾懂懈懊懋懑懒懔懦懵懿戆戈戊-戒戕-战戚戛戟戡-戥截戬-戮戳戴户戽-扃扅-手才扎扑-扔托扛扞扣扦"
                        + "执扩-扰扳扶批扺扼-技抃抄抉把抑-折抚抛抟-抢护报抨披抬抱抵抹抻-抽抿拂-拊拌-拎拐拒-拔拖-拙招拜拟拢-择括-拯拱拳拴"
                        + "拶拷拼-拿持挂指-按挎挑挓挖挚挛挝-挡挣-挦挨挪挫振挲挹挺挽捂捃捅捆捉捋-捐捕捞损捡-捣捧捩捭-捯捶捷捺捻捽掀掂掇-掊掌"
                        + "掎-掐排掖掘掞掠探掣接控-措掬-掮掰掳掴掷掸掺掼掾揄揆揉揍描提插揕揖揠握揣揩揪揭揳援揶揸揽揿-搂搅搋搌搏搐搒-搔搛搜搞搠"
                        + "搡搦搪搬搭搴携搽摁摄-摈摊摏摒摔摘摛摞摧摩摭摴摸摹摽撂撄撅撇撑撒撕撖撙撞撤撩撬-撮撰撵撷撸撺撼擀擂擅操擎擐擒擘擞擢擤擦擿"
                        + "攀攉攒攘攥攫攮支收攸改攻攽-政故效敉敌敏救敔-敖教敛敝敞敢散敦敩敫敬数敲整敷文斋斌斐斑斓斗料斛-斝斟-斡斤斥斧斩斫断斯新"
                        + "斶方於施旁旃-旆旋旌旎-旐旒旖旗旞无既日-早旬-旱旴-旸旺旻旿昀昂-昄昆-昊昌明昏昒-昕昙昝星-昡昣-春昧昨昪昫昭是昱"
                        + "昳-昶昺昼-显晁晃晅晊-晌晏晐晒-晗晙晚晞晟晡晢晤晦晨晪晫普-晱晴晶晷智晾暂暄暅暇暌暑暕-暗暝暧暨暮暲暴-暶暹暾暿曈曌曙"
                        + "曛-曝曦曩曰曲-更曷曹曼曾-最月有朋服朏朐朓-朕朗望朝期朦木未-札术朱朳-朵朸机朽杀杂-杄杆杈杉杌李-村杓杕杖杙杜杞-条"
                        + "来杧-杪杭杯杰杲杳杵杷杻杼松板极构枅枇枉枋枍析枕林枘枚果-枞枢枣枥枧枨枪枫枭枯枰枲枳枵-枹柁柃柄柈柊柏-柔柖柘-柚柜-柞"
                        + "柠柢查柩柬柯-柱柳柴柷柽柿栀栅标-栌栎-栓栖栗栝栟校栩株栲-栴样-根栻-栾桀-框案-桊桌桎桐桑桓-桕桠-桩桫桯桲桴桶桷桹"
                        + "梁梃梅梆梌梏梓梗梠梢梣梦-梨梭梯械梳-梵梼-棂棉棋棍棐棒棓棕棘棚棠棣棤棨棪-棬森棰棱棵棹-棽椀椁椅椆椋植椎椐-椓椟椠椤椪"
                        + "椭椰椴椸椹椽椿楂楒楔楗楙楚楝楞楠楣楦楩-楫楮楯楷-楹楼概-榉榍榑榔-榖榛榜榧榨榫榭榰榱榴榷榻槁槃槊槌槎槐槔槚-槜槟槠槭槱"
                        + "槲槽槿樊樗樘樟模樨横樯樱樵樽樾橄橇橐橑橘橙橛橞橡橥橦橱橹橼檀檄檎檐檑檗檞檠檩檫檬櫆欂欠-欤欧欲欸-欻款歃歅-歇歉歌歙"
                        + "止-歧歪歹死歼殁-殄殆殇殉-残殍殒殓殖殚殛殡殣殪殳-段殷殿毁毂毅毋-母每毐毒-毗毙毛毡毪毫毯毳毵毹毽氅-氇氍氏-民氓-氖"
                        + "氘-氛氟氡氢氤氦-氪氮-氰氲水永氾-求汆-汋汐汔汕汗汛-污汤汧-汫汭汰汲汴汶汹汽汾沁-沉沌沏沐沓沔沘-沛沟没沣-沫沭沮沱"
                        + "河沸-沿泂-泅泇泉泊泌泐泓-泗泙-泜泞泠-泣泥注泪泫泮-泱泳泵泷泸泺-泾洁洄洇洈洋洌洎洑-洓洗-洛洞洢洣津洧洨洪洫洭洮"
                        + "洱-洵洸-洿流浃浅-测浍-浕浙-浜浞-浡浣浥浦浩浪浬-浰浲浴海浸浼涂涄涅消涉涌-涎涐涑涓-涕涘涛涝-涤润-涫涮涯液涴涵涸"
                        + "涿淀淄-淇淋淌淏淑淖淘淙淜-淡淤淦淫淬淮淯深淳淴混淹添淼清渊渌-渎渐渑渔渗渚渝渟-渡渣-渥温渫渭港渰渲渴游渺渼湃湄湉湍湎"
                        + "湑湓湔湖湘湛-湝湟湣湫湮湲湴湾湿溁溃溅-溇溉溍溏源溘溚溜溞-溠溢溥-溧溪溯溱溲溴-溷溹-溻溽滁-滃滆滇滉滋滍滏滑滓-滕滗"
                        + "滘滚滞-滢滤-滫滴滹漂漆漈漉漋漏漓-漖漠漤漦漩-漫漭漯漱漳漴漶漷漹漻漼漾潆潇潋潍潏潖潘潜潞潟潢潦潩潭潮潲潴潵潸潺潼-潾澂"
                        + "澄澈澉澌-澎澛澜澡澥澧澪澭澳澴澶澹澼澽激濂濉濋濑濒濞濠濡濩濮濯瀌瀍瀑瀔瀚瀛瀣瀱瀵瀹瀼灈灌灏灞火灭灯灰灵灶灸灼灾-炀炅炆炉"
                        + "炊炌炎炒炔-炖炘炙炜炝炟炣炫-炯炱炳炷-点炻-炽烀-烃烈烊烔烘烙烛-烝烟烠烤烦-烩烫-热烯烶烷烹-烻烽焆焉焊焌焐焓焕-焚"
                        + "焜焞焦焯-焱然煁煃煅煊-煌煎煓煜煞煟煤煦-煨煮煲-煴煸煺煽熄熇熊熏熔熘熙熛熜熟熠熥熨熬熵熹熻燃燊燋燎燏燔燕燚燠燥燧燮燹爆"
                        + "爇爔爚爝爟爨爪爬爰爱爵-爹爻爽爿牁牂片版牌牍牒牖牙-牛牝牟牡牢牤-牧物牮牯牲牵特-牻牾-犁犄犇犊犋犍犏犒犟犨犬犯犰犴"
                        + "状-犹狁-狄狈狉狍狎狐狒狗狙狝狞狠狡狨狩独-狴狷狸狺-狼猁猃猄猇猊猎猕-猗猛-猞猡猢猥猩-猬献-猱猴猷猹猺猾猿獍獐獒獗獠"
                        + "獬獭獯獴獾玃玄率玉王玎玑-玓玕玖玘-玛玞-玢玤-玦玩玫玭-玳玶玷玹-玼玿珀珂珅珇-珍珏-珒珕珖珙珛珝珞珠珢珣珥-珧珩-珫"
                        + "班珰珲珵珷-珺珽琀球-琊琎-琐琔琚琛琟琡琢琤-琦琨琪-琰琲-琶琼瑀-瑆瑑瑓-瑗瑙-瑟瑢瑧瑨瑬瑭瑰瑱瑳瑶瑷瑾璀璁璃璆-璈璋"
                        + "璎璐璒璘璜璞-璠璥璧-璪璬璮璱璲璺瓀瓒瓖瓘瓜瓞瓠瓢-瓤瓦瓮瓯瓴瓶瓷瓻瓿甄甍甏甑甓甗甘甚甜生甡甥甦用-甭甯-申电男甸町画甾"
                        + "畀畅畈畋界畎畏畔畖留-畜畤-畦番畬畯畲畴畸畹畿疁疃疆疍疏-疑疔疖疗疙疚疝疟-疥疫-疵疸疹疼-疾痂-病症-痊痍痒-痕痘痛痞"
                        + "痢-痤痦-痨痪痫痰痱痴痹痼痿-瘁瘃瘅瘆瘊瘌瘐瘕瘗-瘙瘛瘟瘠瘢瘤-瘦瘩-瘫瘭瘰瘳-瘵瘸瘼瘾-癀癃癌癍癔癖癗癜癞癣癫癯癸登"
                        + "白-癿皂的皆-皈皋皎皑皓皕皖皙皛皞皤皦皭皮皱皲皴皿盂盅盆盈-益盍-盒盔盖-盘盛盟盥盦目盯盱盲直盷-盹盼盾省眄眇-看眍眙眚"
                        + "真眠眢眦眨眩眬眭眯眵-眸眺眼着睁睃睄睇睎睐睑睚睛睡-督睥睦睨睫睬睹睽-瞀瞄瞅瞋-瞎瞑瞒瞟瞠瞢瞥瞧瞩-瞭瞰瞳瞵瞻瞽瞿矍矗矛"
                        + "矜矞矢矣知矧矩矫-矮矰石矶矸矻矼矾-砂砄砆砉砌砍砑砒研砖-砘砚砜砝砟砠砣砥砧砫-砮砰破砵砷-砼砾础硁硅硇硊硌-硎硐硒"
                        + "硔-硗硙硚硝硪-确硼硿碃碇-碉碌-碏碑碓碗碘碚-碜碟碡碣碥碧碨碰-碴碶碹碾磁磅磉-磋磏磐磔磕磙磜磡磨磬磲磴磷磹磻礁礅礌礓"
                        + "礞礴礵示礼社祀祁祃祆-祋祎-祐祓祕-祗祚-祠祢祥祧票祭祯祲祷祸祺祼祾禀禁禄禅禊禋福禒禔禘禚禛禤禧禳禹-离禽禾秀私秃秆秉秋"
                        + "种科秒秕秘租秣秤秦秧秩秫-秭积称秸移秽秾稀稂稃稆程-税稑稔稗稙稚稞稠稣稳稷稹稻-稽稿穄穆穑穗穙穜穟穰穴究-空穿-突窃-窅"
                        + "窈窊窍窎窑窒窕-窘窜窝窟窠窣窥窦窨窬窭窳窸窿立竑竖竘站竞-章竣童竦竫竭端竹竺竽竿笃笄笆笈笊笋笏笑笔笕笙笛笞笠笤-符笨"
                        + "笪-第笮笯笱笳笸笺笼笾筀筅筇等筋筌筏-筒答策筘筚-筝筠筢筤-筦筮筱筲筵-筷筹筻筼签简箅箍箐箓-算箜管箢箦-箭箱箴箸篁篆篇"
                        + "篌篑篓篙篚篝篡篥篦篪篮篯篱篷篼篾簃簇簉簋簌簏簕簖簝簟簠簧簪簰簸簿-籁籍籥米籴类-籽粉粑粒粕粗粘粜-粟粢粤粥粪粮粱-粳粹"
                        + "粼-粿糁糅糇糈糊糌糍糒糕-糗糙糜糟糠糨糯糵系紊素索紧紫累絜絮絷綦綮縠縢縻繁繄繇纂纛纠-绫续-缊缌缎缐-缶缸缺罂罄罅罍罐网"
                        + "罔罕罗罘罚罟罡罢罨-罪置罱署罴罶罹罽罾羁羊羌美羑羓-羖羚羝-羟羡群羧羯-羲羸羹羼羽羿-翃翅翈翊翌翎翔翕翘-翛翟-翡翥翦翩"
                        + "翮-翱翳翷翻翼翾耀老考-耇耋-耍耏-耒耔-耙耜耠耢耤-耪耰耱耳耵-耸耻耽耿聂聃聆聊-聍聒联聘聚聩聪聱聿肃肄肆肇肉肋肌肓肖"
                        + "肘肚肛肝肟-肢肤肥肩-肫肭-肯肱育肴肷肸肺肼-胄胆胈背-胎胖胗胙-胞胠胡胣-胥胧-胭胯-胴胶胸胺胼能脂脆脉脊脍-脔脖脘脚"
                        + "脞脟脩脬脯脱脲脶脸脾脿腆腈腊-腌腐-腕腘-腚腠腥腧-腩腭-腱腴腹-膀膂膈膊膏膑膘膙膛-膝膦膨膳膺膻臀臂臃臆臊臌臑臜臣臧自"
                        + "臬臭至致臻臼臾舀-舂舄-舆舌舍舐舒舔舛舜舞-舠舢舣舥航-舭舯-船舻舾艄艅艇艉艋艎艏艘艚艟艨艮-艰色-艴艺艽-艿节-芄芈芊"
                        + "芋芍-芏芑芒芗-芙芜芝芟-芡芣-芦芨-花芳芴芷-芹芼-芾苁苄苇-苏苑-苕苗苘苛苜苞-苡苣-苧苫苯英苴苷苹苻苾茀-茆茈茉茋"
                        + "茌茎茏茑茓-茕茗茚-茝茧茨茫-茭茯茱茳-茶茸-茺茼茽荀荁荃荄荆荇草荏-荔荖荙-荜荞-荡荣-药荷荸荻-荽莅莆莉莎莒莓莘莙"
                        + "莛-莞莠莨-莫莰-莴莶-莺莼莽莿-菂菅菇菉菊菌菍菏菔菖菘菜菝菟-菡菥菩菪菰-菲菹菼菽萁萃萄萆萋-萏萑萘萚萜萝萣-萩萱萳萸"
                        + "萹萼落葆葎葑葖著葙-葜葡董葩葫-葭葰葱葳-葶葸葺蒂蒄蒇-蒉蒋蒌蒎蒐蒗蒙蒜蒟蒡蒨蒯蒱蒲蒴蒸-蒻蒽蒿蓁蓂蓄蓇蓉蓊蓍蓏-蓑蓓蓖"
                        + "蓝蓟蓠蓢蓣蓥蓦蓬蓰蓼蓿蔀蔃蔈蔊蔌蔑蔓蔗蔚蔟蔡蔫蔬蔷-蔽蕃蕈-蕊蕖蕗蕙蕞蕤蕨蕰蕲蕴蕹-蕻蕾薁薄薅薇薏薛薜薢薤薨薪薮-薰薳"
                        + "薷-薹薿藁藉藏藐藓藕藜藟藠藤藦藨藩藻藿蘅蘑蘖蘘蘧蘩蘸蘼虎-虔虚虞虢虤虫虬虮虱虷-蚂蚄蚆蚊-蚍蚓蚕蚜蚝蚣蚤蚧-蚪蚬蚯-蚲蚴"
                        + "蚶蚺蛀蛃蛄蛆蛇蛉-蛋蛎-蛑蛔蛘蛙蛛蛞蛟蛤蛩蛭蛮蛰-蛴蛸蛹蛾蜀蜂蜃蜇-蜊蜍蜎蜐蜒蜓蜕蜗蜘蜚蜜蜞蜡-蜣蜥蜩蜮蜱蜴蜷蜻蜾蜿"
                        + "蝇-蝉蝌蝎蝓蝗-蝙蝠蝣-蝥蝮蝰蝲蝴蝶蝻-蝾螂螃螅螈螋融螗螟螠螣螨螫-螭螯螱螳螵螺螽蟀蟆蟊蟋蟏蟑蟒蟛蟠蟥蟪蟫蟮蟹蟾蠃蠊蠋蠓"
                        + "蠕蠖蠡蠢蠲蠹蠼血衃-衅行-衎衒衔街衙衠-衣补表衩衫衬衮衰衲衷衽-衿袁袂袄-袆袈袋袍袒袖袗袜袢袤袪被袭袯袱袷袼裁裂装裆裈裉"
                        + "裎裒裔裕裘裙裛裟裢-裥裨裰裱裳裴裸裹裼裾褂褊褐褒褓褕褙-褛褟褡褥褪褫褯褰褴褶襁襄襕襚襜襞襟襦襫襻西要覃覆见-觌觎-角觖觚"
                        + "觜觞觟解觥触觫觭觯觱觳觿言訄訇訚訾詈詟詹誉誊誓謇警譬计-讫训-诩诫-诵请-谈谊-谗谙-谷谼谿豁豆豇豉豌豕豚象豢豨豪豫豮豳"
                        + "豸-豺貂貅貆貉貊貌貔貘贝-负贡-赤赦赧赪赫赭走赳-起趁趄超越趋趑趔趟趣趯趱足-趵趸趺趼趾趿跂-跄跆跋跌跎-跑跖跗跚跛"
                        + "距-跟跣跤跨跪跬路跱跳践-跻跽踅踉踊踌踏踒踔踝-踟踢踣踦踩踪踬踮踯踱踵踶踹踺踽蹀-蹂蹄蹅蹇-蹋蹐-蹒蹙蹚蹜蹢蹦蹩蹬蹭蹯蹰"
                        + "蹲蹴蹶蹼-蹿躁躅躇躏躐躔躜躞身躬躯躲躺车-辜辞辟辣辨辩辫辰辱边辽-辿迁迂迄迅过迈迎运近迓-迕还这进-迟迢迤-迦迨-迫迭迮"
                        + "述迳迷-迺追退-逆选-逋逍透-递途逖逗通逛逝-逢逦逭-逯逴-逶逸逻逼逾遁遂遄遆遇遍遏-道遗遘遛遢遣遥遨遭遮遴遵遹遽避邀邂"
                        + "邃邈邋邑邓邕邗-邙邛邝邠-那邦邨邪邬邮-邶邸-邻邽-邿郁郃-郅郇郈郊郎-郑郓郗郚-郝郡郢郤郦-部郪郫郭郯郴郸都-鄀鄂-鄅"
                        + "鄌鄑鄗-鄚鄜鄞鄠鄢鄣鄫鄯鄱鄹酂酃酅酆酉-酐酒酗酚酝酞酡-酦酩酪酬酮-酲酴-酺酽-酿醅醇醉醋-醍醐-醒醚醛醢醨醪醭-醯醴醵"
                        + "醺醾采釉释里-金釜鉴銎銮鋆鋈錾鍪鎏鏊鏖鐾鑫钆-钐钒-钵钷钹-铆铈-铒铕-铥铧-锟锡-镘镚-镞镠-镶长门-闫闭-阒阔-阚阜"
                        + "队阡阪阮阱-阶阻-阽阿陀陂附-陉陋-陎限陑陔陕陛陞陟陡院除陧-陪陬陲陴-陷隃隅隆隈隋隍随隐隔隗-隙障隧隩隰隳隶隹隺隼-难"
                        + "雀雁雄-雇雉雊雌-雏雒雕雠雨-雪雯雱雳零雷雹雾需霁霄-霉霍-霏霓霖霜霞霨霪霭霰露霸霹霾青靓靖静靛非靠-面靥革靬靰靳靴靶靸"
                        + "靺靼靽靿鞁鞅鞋鞍鞑鞒鞔鞘鞠鞡鞣鞧鞨鞫-鞯鞲-鞴韂韦-韭音韵韶页-频颓颔颖-颢颤-颧风-飕飗-飙飞食飧飨餍餐餮饔饕饥饧-饽"
                        + "饿馁馃-馍馏-香馝馞馥馧馨马-骓骕-骨骰骱骶-骸骺骼髀-髃髅髋髌髎髑髓高髡髢髦髫髭髯髹髻髽鬃鬈鬏鬒鬓鬘鬟鬣鬯鬲鬶鬷鬻鬼"
                        + "魁-魉魋魍魏魑魔鱼-鲃鲅-鲒鲔-鲵鲷-鳅鳇-鳊鳌-鳤鸟-鸳鸵-鹒鹔-鹤鹦-鹴鹾-麀麂麇麈麋麑-麓麖麝麟麦麸麹麻麽麾黄黇黉"
                        + "黍-黏黑黔默黛-黝黟-黢黥黧黩黪黯黹黻黼黾鼋鼍鼎鼐鼒鼓鼗鼙鼠鼢鼩鼫鼬鼯鼱鼷鼹鼻鼽鼾齁齇齉齐齑齿-龌龙-龛龟龠龢鿍-鿏𠅤𠙶"
                        + "𠳐𡎚𡐓𣗋𣲗𣲘𣸣𤧛𤩽𤫉𥔲𥕢𥖨𥻗𦈡𦒍𦙶𦝼𦭜𦰡𧿹𨐈𨙸𨚕𨟠𨭉𨱇𨱏𨱑𨱔𨺙𩽾𩾃𩾌𪟝𪣻𪤗𪨰𪨶𪩘𪾢𫄧𫄨𫄷𫄸𫇭𫌀𫍣𫍯𫍲𫍽𫐄𫐐𫐓𫑡𫓧𫓯𫓶𫓹𫔍"
                        + "𫔎𫔶𫖮𫖯𫖳𫗧𫗴𫘜𫘝𫘦-𫘨𫘪𫘬𫚕𫚖𫚭𫛭𫞩𫟅𫟦𫟹𫟼𫠆𫠊𫠜𫢸𫫇𫭟𫭢𫭼𫮃𫰛𫵷𫶇𫷷𫸩𬀩𬀪𬂩𬃊𬇕𬇙𬇹𬉼𬊈𬊤𬌗𬍛𬍡𬍤𬒈𬒔𬒗𬕂𬘓𬘘𬘡𬘩"
                        + "𬘫-𬘭𬘯𬙂𬙊𬙋𬜬𬜯𬞟𬟁𬟽𬣙𬣞𬣡𬣳𬤇𬤊𬤝𬨂𬨎𬩽𬪩𬬩𬬭𬬮𬬱𬬸𬬹𬬻𬬿𬭁𬭊𬭎𬭚𬭛𬭤𬭩𬭬𬭯𬭳𬭶𬭸𬭼𬮱𬮿𬯀𬯎𬱖𬱟𬳵𬳶𬳽𬳿𬴂𬴃𬴊𬶋𬶍𬶏"
                        + "𬶐𬶟𬶠𬶨𬶭𬶮𬷕𬸘𬸚𬸣𬸦𬸪𬹼𬺈𬺓]";
        UnicodeSet tghSet = new UnicodeSet(tghPattern);

        final UnicodeSet shortPinyin = new UnicodeSet(zh).addAll(GB2312).addAll(GBK).addAll(tghSet);
        final UnicodeSet shortStroke =
                new UnicodeSet(shortPinyin)
                        .addAll(zh_Hant)
                        .addAll(Big5)
                        .addAll(Big5_HKSCS)
                        .addAll(tghSet);

        showSorting(RSComparator, kRSUnicode, "unihan", InfoType.radicalStroke);
        testSorting(RSComparator, kRSUnicode, "unihan");

        writeAndTest(shortPinyin, PinyinComparator, bestPinyin, "pinyin", InfoType.pinyin);
        // writeAndTest(shortStroke, SStrokeComparator, bestStrokesS, "stroke", InfoType.stroke);
        writeAndTest(shortStroke, TStrokeComparator, bestStrokesT, "strokeT", InfoType.stroke);

        for (final Entry<InfoType, Set<String>> entry : indexValues.keyValuesSet()) {
            final InfoType infoType = entry.getKey();
            final UnicodeSet sorted = new UnicodeSet();
            sorted.addAll(entry.getValue());
            System.out.println(infoType + "\t" + sorted);
        }

        writeUnihanFields(bestPinyin, bestPinyin, mergedPinyin, PinyinComparator, "kMandarin");
        writeUnihanFields(bestStrokesS, bestStrokesT, null, SStrokeComparator, "kTotalStrokes");

        //            showSorting(PinyinComparator, bestPinyin, "pinyin");
        //            UnicodeMap<String> shortPinyinMap = new
        // UnicodeMap<String>().putAllFiltered(bestPinyin, shortPinyin);
        //            System.out.println("stroke_pinyin base size:\t" + shortPinyinMap.size());
        //            showSorting(PinyinComparator, shortPinyinMap, "pinyin_short");
        //            testSorting(PinyinComparator, bestPinyin, "pinyin");
        //            testSorting(PinyinComparator, shortPinyinMap, "pinyin_short");

        //            showSorting(TStrokeComparator, bestStrokesT, "strokeT");
        //            UnicodeMap<Integer> shortStrokeMapT = new
        // UnicodeMap<Integer>().putAllFiltered(bestStrokesT, shortStroke);
        //            System.out.println("Tstroke_stroke base size:\t" + shortStrokeMapT.size());
        //            showSorting(TStrokeComparator, shortStrokeMapT, "stroke_shortT");
        //            testSorting(TStrokeComparator, bestStrokesT, "strokeT");
        //            testSorting(TStrokeComparator, shortStrokeMapT, "stroke_shortT");

        // showSorting(PinyinComparator, bestPinyin, "pinyinCollationInterleaved", true,
        // FileType.TXT);

        showTranslit("Han-Latin");

        showBackgroundData();

        System.out.println("TODO: test the translit");

        getIndexChars();
    }

    /**
     * U+3400 kMandarin QIU1 U+3400 kTotalStrokes 5
     *
     * @param <U>
     * @param <T>
     * @param simplified
     * @param traditional
     * @param other
     * @param comp
     * @param filename
     */
    private static <U, T> void writeUnihanFields(
            UnicodeMap<U> simplified,
            UnicodeMap<U> traditional,
            UnicodeMap<T> other,
            Comparator<String> comp,
            String filename) {
        final PrintWriter out =
                Utility.openPrintWriter(
                        GenerateUnihanCollatorFiles.OUTPUT_DIRECTORY, filename + ".txt", null);
        final UnicodeSet keys = new UnicodeSet(simplified.keySet()).addAll(traditional.keySet());
        final Set<String> sorted = new TreeSet<String>(comp);
        UnicodeSet.addAllTo(keys, sorted);
        for (final String s : sorted) {
            final U simp = simplified.get(s);
            final U trad = traditional.get(s);
            String item;
            if (simp == null) {
                item = trad.toString();
            } else if (trad != null && !simp.equals(trad)) {
                item = simp + " " + trad;
            } else {
                item = simp.toString();
            }
            final T commentSource = other == null ? null : other.get(s);
            String comments = "";
            if (commentSource == null) {
                // do nothing
            } else if (commentSource instanceof Set) {
                @SuppressWarnings("unchecked")
                final LinkedHashSet<String> temp =
                        new LinkedHashSet<String>((Set<String>) commentSource);
                temp.remove(simp);
                temp.remove(trad);
                comments = CollectionUtilities.join(temp, " ");
            } else {
                comments = commentSource.toString();
                if (comments.equals(item)) {
                    comments = "";
                }
            }
            out.println(
                    "U+"
                            + Utility.hex(s)
                            + "\t"
                            + filename
                            + "\t"
                            + item
                            + "\t# "
                            + s
                            + (comments.isEmpty() ? "" : "\t" + comments));
        }
        out.close();
    }

    private static <T> void writeAndTest(
            UnicodeSet shortStroke,
            Comparator<String> comparator2,
            UnicodeMap<T> unicodeMap2,
            String title2,
            InfoType infoType)
            throws Exception {
        showSorting(comparator2, unicodeMap2, title2, infoType);
        testSorting(comparator2, unicodeMap2, title2);
        final UnicodeMap<T> shortMap = new UnicodeMap<T>().putAllFiltered(unicodeMap2, shortStroke);
        System.out.println(title2 + " base size:\t" + shortMap.size());
        showSorting(comparator2, shortMap, title2 + "_short", infoType);
        testSorting(comparator2, shortMap, title2 + "_short");
    }

    private static void showOldData(Collator collator, String name, boolean japanese) {
        final PrintWriter out =
                Utility.openPrintWriter(GenerateUnihanCollatorFiles.OUTPUT_DIRECTORY, name, null);
        final UnicodeSet tailored = collator.getTailoredSet();
        final TreeSet<String> sorted = new TreeSet<String>(collator);
        for (final String s : tailored) {
            sorted.add(nfc.normalize(s));
        }
        final UnicodeMap<String> kJapaneseKun = IUP.load(UcdProperty.kJapaneseKun);
        final UnicodeMap<String> kJapaneseOn = IUP.load(UcdProperty.kJapaneseOn);

        final StringBuilder buffer = new StringBuilder();
        out.println("#char; strokes; radical; rem-strokes; reading");
        for (final String item : sorted) {
            buffer.append("<").append(item).append("\t#");
            final String code = Utility.hex(item);
            buffer.append(pad(code, 6)).append(";\t");

            int strokes = CldrUtility.ifNull(bestStrokesS.get(item), 0);
            buffer.append(pad(String.valueOf(strokes), 3)).append(";\t");

            int data = getRSShortData(item.codePointAt(0));
            String radical = null;
            String remainingStrokes = null;
            if (data != 0) {
                radical = radicalStroke.getRadicalStringFromShortData(data);
                remainingStrokes = RadicalStroke.getResidualStrokesFromShortData(data) + "";
            }
            buffer.append(pad(radical, 4)).append(";\t");
            buffer.append(pad(remainingStrokes, 2)).append(";\t");

            if (japanese) {
                final String reading = kJapaneseKun.get(item);
                final String reading2 = kJapaneseOn.get(item);
                buffer.append(pad(reading, 1)).append(";\t");
                buffer.append(pad(reading2, 1)).append(";\t");
            } else {
                final Set<String> pinyins = mergedPinyin.get(item);
                if (pinyins != null) {
                    boolean first = true;
                    for (final String pinyin : pinyins) {
                        if (first) {
                            first = false;
                        } else {
                            buffer.append(", ");
                        }
                        buffer.append(pinyin);
                    }
                } else {
                    buffer.append("?");
                }
            }
            out.println(buffer);
            buffer.setLength(0);
        }
        out.close();
    }

    private static String pad(String strokes, int padSize) {
        if (strokes == null) {
            strokes = "?";
        } else {
            strokes = strokes.toLowerCase(Locale.ENGLISH).replace(" ", ", ");
        }
        return Utility.repeat(" ", padSize - strokes.length()) + strokes;
    }

    private static void getIndexChars() {
        // TODO Auto-generated method stub
        final UnicodeSet tailored = pinyinSort.getTailoredSet();
        final TreeMap<String, String> sorted = new TreeMap<String, String>(pinyinSort);
        final Counter<String> counter = new Counter<String>();
        for (final String s : tailored) {
            String pinyin = bestPinyin.get(s);
            if (pinyin == null) {
                pinyin = "?";
            } else {
                pinyin = nfd.normalize(pinyin);
                pinyin = pinyin.substring(0, 1).toUpperCase().intern();
            }
            counter.add(pinyin, 1);
            sorted.put(s, pinyin);
        }
        System.out.println(counter);
        String lastPinyin = "";
        int count = 0;
        final Counter<String> progressive = new Counter<String>();

        for (final String s : sorted.keySet()) {
            final String pinyin = sorted.get(s);
            progressive.add(pinyin, 1);
            if (pinyin.equals(lastPinyin)) {
                count++;
            } else {
                if (DEBUG)
                    System.out.println(
                            "\t"
                                    + count
                                    + "\t"
                                    + (progressive.get(lastPinyin)
                                            / (double) counter.get(lastPinyin)));
                count = 1;
                lastPinyin = pinyin;
                System.out.print(s + "\t" + pinyin + "\t");
            }
        }
        if (DEBUG)
            System.out.println(
                    "\t"
                            + count
                            + "\t"
                            + (progressive.get(lastPinyin) / (double) counter.get(lastPinyin)));
    }

    private static void getBestStrokes() {
        final PrintWriter out =
                Utility.openPrintWriter(
                        GenerateUnihanCollatorFiles.OUTPUT_DIRECTORY,
                        "kTotalStrokesReplacements.txt",
                        null);

        out.println("#Code\tkTotalStrokes\tValue\t#\tChar\tUnihan");

        for (final String s : new UnicodeSet(bihuaData.keySet()).addAll(bestStrokesS.keySet())) {
            final int unihanStrokes = CldrUtility.ifNull(bestStrokesS.get(s), NO_STROKE_INFO);
            final R2<String, String> bihua = bihuaData.get(s);
            final int bihuaStrokes = bihua == null ? NO_STROKE_INFO : bihua.get1().length();
            if (bihuaStrokes != NO_STROKE_INFO) {
                bestStrokesS.put(s, bihuaStrokes);
            } else if (unihanStrokes != NO_STROKE_INFO) {
                bestStrokesS.put(s, unihanStrokes);
            }
            if (bihuaStrokes != NO_STROKE_INFO && bihuaStrokes != unihanStrokes) {
                out.println(
                        "U+"
                                + Utility.hex(s)
                                + "\tkTotalStrokes\t"
                                + bihuaStrokes
                                + "\t#\t"
                                + s
                                + "\t"
                                + unihanStrokes);
            }
        }
        out.close();

        new PatchStrokeReader(bestStrokesS, SemiFileReader.SPLIT)
                .process(GenerateUnihanCollators.class, "patchStroke.txt");
    }

    private static <T> void closeUnderNFKD(String title, UnicodeMap<T> mapping) {
        //        UnicodeSet possibles = new
        // UnicodeSet(NOT_NFKD).removeAll(NOT_NFD).removeAll(mapping.keySet());
        //        if (!possibles.contains("㊀")) {
        //            System.out.println("??");
        //        }
        //
        //        for (String s : possibles) {
        //            if (s.equals("㊀")) {
        //                System.out.println("??");
        //            }
        //            String kd = nfkd.normalize(s);
        //            T value = mapping.get(kd);
        //            if (value == null) {
        //                continue;
        //            }
        //            mapping.put(s, value);
        //            System.out.println("*** " + title + " Closing " + s + " => " + kd + "; " +
        // value);
        //        }
        final UnicodeSet extras = new UnicodeSet(NOT_NFKD).retainAll(mapping.keySet());
        if (extras.size() != 0) {
            System.out.println("*** " + title + " Removing " + extras.toPattern(false));
            mapping.putAll(extras, null);
        }
        mapping.freeze();
    }

    private static void showBackgroundData() throws IOException {
        final PrintWriter out =
                Utility.openPrintWriter(
                        GenerateUnihanCollatorFiles.OUTPUT_DIRECTORY,
                        "backgroundCjkData.txt",
                        null);
        final UnicodeSet all =
                new UnicodeSet(
                        bihuaData
                                .keySet()); // .addAll(allPinyin.keySet()).addAll(kRSUnicode.keySet());
        final Comparator<Row.R4<String, String, Integer, String>> comparator =
                new Comparator<Row.R4<String, String, Integer, String>>() {
                    @Override
                    public int compare(
                            R4<String, String, Integer, String> o1,
                            R4<String, String, Integer, String> o2) {
                        int result = o1.get0().compareTo(o2.get0());
                        if (result != 0) {
                            return result;
                        }
                        result = pinyinSort.compare(o1.get1(), o2.get1());
                        if (result != 0) {
                            return result;
                        }
                        result = o1.get2().compareTo(o2.get2());
                        if (result != 0) {
                            return result;
                        }
                        result = o1.get3().compareTo(o2.get3());
                        return result;
                    }
                };
        final Set<Row.R4<String, String, Integer, String>> items =
                new TreeSet<Row.R4<String, String, Integer, String>>(comparator);
        for (final String s : all) {
            final R2<String, String> bihua = bihuaData.get(s);
            final int bihuaStrokes = bihua == null ? 0 : bihua.get1().length();
            final String bihuaPinyin = bihua == null ? "?" : bihua.get0();
            final Set<String> allPinyins = mergedPinyin.get(s);
            final String firstPinyin = allPinyins == null ? "?" : allPinyins.iterator().next();
            final String rs = kRSUnicode.get(s);

            final int totalStrokes =
                    org.unicode.cldr.util.CldrUtility.ifNull(bestStrokesS.get(s), 0);
            // for (String rsItem : rs.split(" ")) {
            // RsInfo rsInfo = RsInfo.from(rsItem);
            // int totalStrokes = rsInfo.totalStrokes;
            // totals.set(totalStrokes);
            // if (firstTotal != -1) firstTotal = totalStrokes;
            // int radicalsStokes = bihuaStrokes - rsInfo.remainingStrokes;
            // counter.add(Row.of(rsInfo.radical + (rsInfo.alternate == 1 ? "'"
            // : ""), radicalsStokes), 1);
            // }
            final String status =
                    (bihuaPinyin.equals(firstPinyin) ? "-" : "P")
                            + (bihuaStrokes == totalStrokes ? "-" : "S");
            items.add(
                    Row.of(
                            status,
                            firstPinyin,
                            totalStrokes,
                            status
                                    + "\t"
                                    + s
                                    + "\t"
                                    + rs
                                    + "\t"
                                    + totalStrokes
                                    + "\t"
                                    + bihuaStrokes
                                    + "\t"
                                    + bihua
                                    + "\t"
                                    + mergedPinyin.get(s)));
        }
        for (final R4<String, String, Integer, String> item : items) {
            out.println(item.get3());
        }
        out.close();
    }

    // Markus: Could not figure out how to avoid type safety warnings with
    // Comparator collator = new MultiComparator(coll, codepointComparator);
    // Note that Collator is a Comparator<Object> and it cannot also be a Comparator<something
    // else>.
    private static final class CollatorWithTieBreaker implements Comparator<String> {
        private final Collator coll;
        private final Comparator<String> tieBreaker;

        CollatorWithTieBreaker(Collator c, Comparator<String> tb) {
            coll = c;
            tieBreaker = tb;
        }

        public int compare(String left, String right) {
            int result = coll.compare(left, right);
            if (result != 0) {
                return result;
            }
            return tieBreaker.compare(left, right);
        }
    }

    private static <S> void testSorting(
            Comparator<String> oldComparator, UnicodeMap<S> krsunicode2, String filename)
            throws Exception {
        final List<String> temp = krsunicode2.keySet().addAllTo(new ArrayList<String>());
        final String rules =
                getFileAsString(
                        GenerateUnihanCollatorFiles.OUTPUT_DIRECTORY
                                + File.separatorChar
                                + filename
                                + ".txt");

        // The rules contain \uFDD0 and such and must be unescaped for the RuleBasedCollator.
        final Collator coll = new RuleBasedCollator(com.ibm.icu.impl.Utility.unescape(rules));
        final Comparator<String> collator = new CollatorWithTieBreaker(coll, codepointComparator);
        final List<String> ruleSorted = sortList(collator, temp);

        @SuppressWarnings("unchecked")
        final Comparator<String> oldCollator =
                new MultiComparator<String>(oldComparator, codepointComparator);
        final List<String> originalSorted = sortList(oldCollator, temp);
        int badItems = 0;
        final int min = Math.min(originalSorted.size(), ruleSorted.size());
        final Differ<String> differ = new Differ<String>(100, 2);
        for (int k = 0; k < min; ++k) {
            final String ruleItem = ruleSorted.get(k);
            final String originalItem = originalSorted.get(k);
            if (ruleItem == null || originalItem == null) {
                throw new IllegalArgumentException();
            }
            differ.add(originalItem, ruleItem);

            differ.checkMatch(k == min - 1);

            final int aCount = differ.getACount();
            final int bCount = differ.getBCount();
            if (aCount != 0 || bCount != 0) {
                badItems += aCount + bCount;
                System.out.println(
                        aline(krsunicode2, differ, -1) + "\t" + bline(krsunicode2, differ, -1));

                if (aCount != 0) {
                    for (int i = 0; i < aCount; ++i) {
                        System.out.println(aline(krsunicode2, differ, i));
                    }
                }
                if (bCount != 0) {
                    for (int i = 0; i < bCount; ++i) {
                        System.out.println("\t\t\t\t\t\t" + bline(krsunicode2, differ, i));
                    }
                }
                System.out.println(
                        aline(krsunicode2, differ, aCount)
                                + "\t "
                                + bline(krsunicode2, differ, bCount));
                System.out.println("-----");
            }

            // if (!ruleItem.equals(originalItem)) {
            // badItems += 1;
            // if (badItems < 50) {
            // System.out.println(i + ", " + ruleItem + ", " + originalItem);
            // }
            // }
        }
        System.out.println(badItems + " differences");
    }

    private static <S> String aline(UnicodeMap<S> krsunicode2, Differ<String> differ, int i) {
        final String item = differ.getA(i);
        try {
            return "unihan: "
                    + differ.getALine(i)
                    + " "
                    + item
                    + " ["
                    + Utility.hex(item)
                    + "/"
                    + krsunicode2.get(item)
                    + "]";
        } catch (final RuntimeException e) {
            throw e;
        }
    }

    private static <S> String bline(UnicodeMap<S> krsunicode2, Differ<String> differ, int i) {
        final String item = differ.getB(i);
        return "rules: "
                + differ.getBLine(i)
                + " "
                + item
                + " ["
                + Utility.hex(item)
                + "/"
                + krsunicode2.get(item)
                + "]";
    }

    private static List<String> sortList(Comparator<String> collator, List<String> temp) {
        final String[] ruleSorted = temp.toArray(new String[temp.size()]);
        Arrays.sort(ruleSorted, collator);
        return Arrays.asList(ruleSorted);
    }

    // private static String getFileAsString(Class<GenerateUnihanCollators> relativeToClass, String
    // filename) throws IOException {
    //     final BufferedReader in = FileUtilities.openFile(relativeToClass, filename);
    //     ... same as the version below
    // }

    private static String getFileAsString(String filename) throws IOException {
        final InputStreamReader reader =
                new InputStreamReader(new FileInputStream(filename), StandardCharsets.UTF_8);
        final BufferedReader in = new BufferedReader(reader, 1024 * 64);
        final StringBuilder builder = new StringBuilder();
        while (true) {
            final String line = in.readLine();
            if (line == null) {
                break;
            }
            builder.append(line).append('\n');
        }
        in.close();
        return builder.toString();
    }

    private static void showTranslit(String filename) {
        final PrintWriter out =
                Utility.openPrintWriter(
                        GenerateUnihanCollatorFiles.OUTPUT_DIRECTORY, filename + ".txt", null);
        final PrintWriter out2 =
                Utility.openPrintWriter(
                        GenerateUnihanCollatorFiles.OUTPUT_DIRECTORY, filename + ".xml", null);
        final TreeSet<String> s = new TreeSet<String>(pinyinSort);
        s.addAll(bestPinyin.getAvailableValues());

        for (final String value : s) {
            final UnicodeSet uset = bestPinyin.getSet(value);
            // [专叀塼嫥専專瑼甎砖磚篿耑膞蟤跧鄟顓颛鱄鷒]→zhuān;
            out.println(uset.toPattern(false) + "→" + value + ";");
            out2.println(uset.toPattern(false) + "→" + value + ";");
        }
        out.close();
        out2.close();
    }

    private static class RsInfo {
        public static void addToStrokeInfo(UnicodeMap<Integer> bestStrokesIn, boolean simplified) {
            final int[] mainStrokes = new int[256];
            final int[] alternateStrokes = new int[256];

            final Counter<Integer> mainStrokesTotal = new Counter<Integer>();
            final Counter<Integer> mainCount = new Counter<Integer>();
            final Counter<Integer> alternateStrokesTotal = new Counter<Integer>();
            final Counter<Integer> alternateCount = new Counter<Integer>();
            for (final String s : bestStrokesIn) {
                final int c = s.codePointAt(0);
                final Integer bestStrokeInfo = bestStrokesIn.get(c);
                int data = getRSShortData(c);
                if (data == 0) {
                    continue;
                }
                int radical = RadicalStroke.getRadicalNumberFromShortData(data);
                final int radicalsStrokes =
                        bestStrokeInfo - RadicalStroke.getResidualStrokesFromShortData(data);
                if (!RadicalStroke.isSimplifiedFromShortData(data)) {
                    mainStrokesTotal.add(radical, radicalsStrokes);
                    mainCount.add(radical, 1);
                } else {
                    // TODO: Starting with Unicode 15.1, "simplified" can have a value of 2
                    // (UAX #38 "non-Chinese simplified form of the radical").
                    // Should we do something different than for simplified==1?
                    alternateStrokesTotal.add(radical, radicalsStrokes);
                    alternateCount.add(radical, 1);
                }
            }
            // compute averages. Lame, but the best we have for now.
            for (final int key : mainStrokesTotal.keySet()) {
                mainStrokes[key] =
                        (int) Math.round(mainStrokesTotal.get(key) / (double) mainCount.get(key));
                if (DEBUG) System.out.println("radical " + key + "\t" + mainStrokes[key]);
            }
            for (final int key : alternateStrokesTotal.keySet()) {
                alternateStrokes[key] =
                        (int)
                                Math.round(
                                        alternateStrokesTotal.get(key)
                                                / (double) alternateCount.get(key));
                if (DEBUG) System.out.println("radical' " + key + "\t" + alternateStrokes[key]);
            }
            final PrintWriter out =
                    Utility.openPrintWriter(
                            GenerateUnihanCollatorFiles.OUTPUT_DIRECTORY,
                            "imputedStrokes" + (simplified ? "" : "T") + ".txt",
                            null);
            for (final String s :
                    new UnicodeSet(kRSUnicode.keySet()).removeAll(bestStrokesIn.keySet())) {
                int c = s.codePointAt(0);
                int data = getRSShortData(c);
                int radical = RadicalStroke.getRadicalNumberFromShortData(data);
                final int computedStrokes =
                        RadicalStroke.getResidualStrokesFromShortData(data)
                                + (RadicalStroke.isSimplifiedFromShortData(data)
                                        ? alternateStrokes[radical]
                                        : mainStrokes[radical]);
                bestStrokesIn.put(s, computedStrokes);
                out.println(
                        "U+"
                                + Utility.hex(s)
                                + "\tkImputedStrokes\t"
                                + computedStrokes
                                + "\t#\t"
                                + s);
            }
            closeUnderNFKD("Strokes", bestStrokesIn);
            bestStrokesIn.freeze();
            out.close();
        }
    }

    private static int getRSShortData(int c) {
        int data = radicalStroke.getShortDataForCodePoint(c);
        if (data != 0) {
            return data;
        }
        if (c < 0x3000) {
            String radical = radicalMap.get(c);
            if (radical == null) {
                return 0;
            }
            c = radical.codePointAt(0);
            assert radical.length() == Character.charCount(c); // single code point
            data = radicalStroke.getShortDataForCodePoint(c);
            assert data != 0;
            return data;
        }
        String decomp = nfd.normalize(c);
        c = decomp.codePointAt(0);
        data = radicalStroke.getShortDataForCodePoint(c);
        return data;
    }

    private static long getRSLongOrder(int c) {
        long order = radicalStroke.getLongOrder(c);
        if (order != 0) {
            return order;
        }
        if (c < 0x3000) {
            String radical = radicalMap.get(c);
            if (radical == null) {
                // Not an ideograph, sort higher than any of them.
                return ((long) Integer.MAX_VALUE << 32) | c;
            }
            c = radical.codePointAt(0);
            assert radical.length() == Character.charCount(c); // single code point
            order = radicalStroke.getLongOrder(c);
            assert order != 0;
            return order;
        }
        String decomp = nfd.normalize(c);
        c = decomp.codePointAt(0);
        order = radicalStroke.getLongOrder(c);
        if (order == 0) {
            // Not an ideograph, sort higher than any of them.
            order = ((long) Integer.MAX_VALUE << 32) | c;
        }
        return order;
    }

    private static <S> void showSorting(
            Comparator<String> comparator,
            UnicodeMap<S> unicodeMap,
            String filename,
            InfoType infoType) {
        showSorting(comparator, unicodeMap, filename, FileType.txt, infoType);
        showSorting(comparator, unicodeMap, filename, FileType.xml, infoType);
    }

    @SuppressWarnings("resource")
    private static <S> void showSorting(
            Comparator<String> comparator,
            UnicodeMap<S> unicodeMap,
            String filename,
            FileType fileType,
            InfoType infoType) {
        // special capture for Pinyin buckets
        final boolean isPinyin = filename.startsWith("pinyin") && fileType == FileType.xml;
        int alpha = 'a';
        final StringBuilder pinyinBuffer = new StringBuilder("\"\", // A\n");
        final StringBuilder pinyinIndexBuffer = new StringBuilder("\"\u0101");

        final UnicodeSet accumulated = new UnicodeSet();
        PrintWriter out =
                Utility.openPrintWriter(
                        GenerateUnihanCollatorFiles.OUTPUT_DIRECTORY,
                        filename + "." + fileType,
                        null);
        final TreeSet<String> rsSorted = new TreeSet<String>(comparator);
        final StringBuilder buffer = new StringBuilder();
        for (final String s : unicodeMap) {
            //            S newValue = unicodeMap.get(s);
            //            if (newValue == null) continue;
            if (UTF16.countCodePoint(s) != 1) {
                throw new IllegalArgumentException("Wrong length!!");
            }
            rsSorted.add(s);
        }
        if (fileType == FileType.txt) {
            out.println(INDENT + "&[last regular]");
        } else {
            //            final String typeAlt = filename.replace("_", "' alt='");
            //            out.print(
            //                    "<?xml version='1.0' encoding='UTF-8' ?>\n"
            //                    +"<!DOCTYPE ldml SYSTEM '.../cldr/common/dtd/ldml.dtd'>\n"
            //                    +"<ldml>\n"
            //                    +"    <identity>\n"
            //                    +"        <version number='$Revision: 1.8 $' />\n"
            //                    +"        <generation date='$Date: 2010/12/14 07:57:17 $' />\n"
            //                    +"        <language type='zh' />\n"
            //                    +"    </identity>\n"
            //                    +"    <collations>\n"
            //                    +"        <collation type='" + typeAlt + "'>\n"
            //            );
            //            FileUtilities.appendFile(GenerateUnihanCollators.class,
            // "pinyinHeader.txt", out);
            out.println("\t\t\t\t<reset><last_non_ignorable /></reset>");
        }
        S oldValue = null;
        String oldIndexValue = null;
        final Output<String> comment = new Output<String>();
        for (final String s : rsSorted) {
            final S newValue = unicodeMap.get(s);
            if (!equals(newValue, oldValue)) {
                if (oldValue == null) {
                    final String indexValue = getIndexValue(infoType, s, comment);
                    showIndexValue(fileType, out, comment, indexValue);
                    oldIndexValue = indexValue;
                } else {
                    // show other characters
                    if (buffer.codePointCount(0, buffer.length()) < 128) {
                        if (fileType == FileType.txt) {
                            out.println(
                                    INDENT
                                            + "<*"
                                            + sortingQuote(buffer.toString(), accumulated)
                                            + " # "
                                            + sortingQuote(oldValue, accumulated));
                        } else {
                            out.println(
                                    "               <pc>"
                                            + buffer
                                            + "</pc><!-- "
                                            + oldValue
                                            + " -->");
                        }
                    } else {
                        int count = 1;
                        while (buffer.length() > 0) {
                            final String temp = extractFirst(buffer, 128);
                            if (fileType == FileType.txt) {
                                out.println(
                                        INDENT
                                                + "<*"
                                                + sortingQuote(temp.toString(), accumulated)
                                                + " # "
                                                + sortingQuote(oldValue, accumulated)
                                                + " (p"
                                                + count++
                                                + ")");
                            } else {
                                out.println(
                                        "               <pc>"
                                                + temp
                                                + "</pc><!-- "
                                                + oldValue
                                                + " (p"
                                                + count++
                                                + ") -->");
                            }
                        }
                    }

                    // insert index character
                    final String indexValue = getIndexValue(infoType, s, comment);
                    if (!equals(indexValue, oldIndexValue)) {
                        showIndexValue(fileType, out, comment, indexValue);
                        oldIndexValue = indexValue;
                    }
                }
                buffer.setLength(0);
                oldValue = newValue;
                if (isPinyin) {
                    // OK to just use codepoint order.
                    final String pinyinValue = newValue.toString();
                    final int pinyinFirst = nfd.normalize(pinyinValue).charAt(0);
                    if (alpha < pinyinFirst) {
                        while (alpha < pinyinFirst) {
                            alpha++;
                        }
                        // "\u516B", // B
                        pinyinBuffer.append(
                                "\""
                                        + hexConstant(s)
                                        + "\", "
                                        + "// "
                                        + UTF16.valueOf(alpha)
                                        + " : "
                                        + s
                                        + " ["
                                        + pinyinValue
                                        + "]\n");
                        pinyinIndexBuffer.append(hexConstant(pinyinValue.substring(0, 1)));
                    }
                }
            }
            buffer.append(s);
        }

        if (oldValue != null) {
            if (fileType == FileType.txt) {
                out.println(
                        INDENT
                                + "<*"
                                + sortingQuote(buffer.toString(), accumulated)
                                + " # "
                                + sortingQuote(oldValue, accumulated));
            } else {
                out.println("               <pc>" + buffer + "</pc><!-- " + oldValue + " -->");
            }
            buffer.setLength(0);
        }

        final UnicodeSet missing = new UnicodeSet().addAll(buffer.toString());
        if (missing.size() != 0) {
            System.out.println("MISSING" + "\t" + missing.toPattern(false));
        }
        final UnicodeSet tailored = new UnicodeSet(unicodeMap.keySet()).removeAll(missing);

        final TreeSet<String> sorted = new TreeSet<String>(nfkdComparator);
        new UnicodeSet(NOT_NFKD).removeAll(NOT_NFD).removeAll(tailored).addAllTo(sorted);

        for (final String s : sorted) {
            // decomposable, but not tailored
            final String kd = nfkd.normalize(s.codePointAt(0));
            if (!tailored.containsSome(kd)) {
                continue; // the decomp has to contain at least one tailored
            }
            //            if (tailored.containsAll(kd))
            //                continue; //already have it
            // character
            if (fileType == FileType.txt) {
                out.println(
                        INDENT
                                + "&"
                                + sortingQuote(kd, accumulated)
                                + "<<<"
                                + sortingQuote(s, accumulated));
            } else {
                out.println("               <reset>" + kd + "</reset>");
                out.println("               <t>" + s + "</t>");
            }
        }
        //        if (fileType == FileType.xml) {
        //            out.println(
        //                    "           </rules>\n" +
        //                    "        </collation>\n" +
        //                    "    </collations>\n" +
        //                    "</ldml>"
        //            );
        //        }

        out.close();
        out =
                Utility.openPrintWriter(
                        GenerateUnihanCollatorFiles.OUTPUT_DIRECTORY,
                        filename + "_repertoire.txt",
                        null);
        out.println(accumulated.toPattern(false));
        out.close();
        if (isPinyin) {
            out =
                    Utility.openPrintWriter(
                            GenerateUnihanCollatorFiles.OUTPUT_DIRECTORY,
                            filename + "_buckets.txt",
                            null);
            pinyinIndexBuffer.append('"');
            out.println(pinyinIndexBuffer);
            out.println(pinyinBuffer);
            out.close();
        }
    }

    private static <T> void showIndexValue(
            FileType fileType, PrintWriter out, Output<T> comment, String indexValue) {
        if (fileType == FileType.txt) {
            out.println(INDENT + "<'" + hexConstant(indexValue) + "' # INDEX " + comment);
        } else {
            out.println("               <p>" + indexValue + "</p><!-- INDEX " + comment + " -->");
        }
    }

    /**
     * Hex format by code unit.
     *
     * @param s
     * @return
     */
    private static CharSequence hexConstant(String s) {
        final StringBuilder result = new StringBuilder();
        for (int i = 0; i < s.length(); ++i) {
            final char ch = s.charAt(i);
            if (0x20 <= ch && ch < 0x80) {
                result.append(ch);
            } else {
                result.append("\\u").append(Utility.hex(ch, 4));
            }
        }
        return result;
    }

    private static String extractFirst(StringBuilder buffer, int i) {
        String result;
        try {
            final int charEnd = buffer.offsetByCodePoints(0, i);
            result = buffer.substring(0, charEnd);
            buffer.delete(0, charEnd);
        } catch (final Exception e) {
            result = buffer.toString();
            buffer.setLength(0);
        }
        return result;
    }

    private static <T> String sortingQuote(T input, UnicodeSet accumulated) {
        String s = input.toString();
        accumulated.addAll(s);
        s = s.replace("'", "''");
        if (NEEDSQUOTE.containsSome(s)) {
            s = '\'' + s + '\'';
        }
        return s;
    }

    private static boolean equals(Object newValue, Object oldValue) {
        return newValue == null
                ? oldValue == null
                : oldValue == null ? false : newValue.equals(oldValue);
    }

    private static int showAdded(String title, int difference) {
        difference = mergedPinyin.size() - difference;
        System.out.println("added " + title + " " + difference);
        return difference;
    }

    private static void addBihua() {
        for (final String s : bihuaData.keySet()) {
            final R2<String, String> bihuaRow = bihuaData.get(s);
            final String value = bihuaRow.get0();
            addPinyin("bihua", s, value, OverrideItems.keepOld);
        }
    }

    private static void printExtraPinyinForUnihan() {
        try (PrintWriter out =
                        Utility.openPrintWriter(
                                GenerateUnihanCollatorFiles.OUTPUT_DIRECTORY,
                                "kMandarinAdditions.txt",
                                null);
                PrintWriter overrideOut =
                        Utility.openPrintWriter(
                                GenerateUnihanCollatorFiles.OUTPUT_DIRECTORY,
                                "kMandarinOverride.txt",
                                null); ) {
            final String header = "#Code\t“Best”\tValue\t#\tChar";
            String description =
                    "# the format is like Unihan, with “kMandarin” being the field, and the value being a possible replacement for what is there.";
            out.println(header + "\n" + description);
            overrideOut.println(header + "\tkMandarin\n" + description);

            for (final String s : new UnicodeSet(bestPinyin.keySet())) {
                final String bestValue = bestPinyin.get(s);
                final String kMandarinString = kMandarin.get(s);
                if (kMandarinString == null) {
                    final String bestValueNumeric =
                            toNumericPinyin.transform(bestValue).toUpperCase();
                    out.println(
                            "U+"
                                    + Utility.hex(s)
                                    + "\tkMandarin\t"
                                    + bestValueNumeric
                                    + "\t#\t"
                                    + s);
                    continue;
                }

                String firstMandarin = ONBAR.split(kMandarinString).iterator().next();
                if (bestValue.equals(firstMandarin)) {
                    continue;
                }
                final String bestValueNumeric = toNumericPinyin.transform(bestValue).toUpperCase();
                overrideOut.println(
                        "U+"
                                + Utility.hex(s)
                                + "\tkMandarin\t"
                                + bestValueNumeric
                                + "\t#\t"
                                + s
                                + "\t"
                                + kMandarinString);
            }
        }
    }

    private static void printExtraStrokesForUnihan() {
        try (PrintWriter out =
                        Utility.openPrintWriter(
                                GenerateUnihanCollatorFiles.OUTPUT_DIRECTORY,
                                "kTotalStrokesAdditions.txt",
                                null);
                PrintWriter overrideOut =
                        Utility.openPrintWriter(
                                GenerateUnihanCollatorFiles.OUTPUT_DIRECTORY,
                                "kTotalStrokesOverride.txt",
                                null); ) {
            final String header = "#Code\t“Best”\tValue\t#\tChar";
            String description =
                    "# the format is like Unihan, with “kTotalStrokes” being the field, and the value being a possible replacement for what is there.";
            out.println(header + "\n" + description);
            overrideOut.println(header + "\tkTotalStrokes\n" + description);

            UnicodeSet keys =
                    new UnicodeSet(bestStrokesS.keySet()).addAll(bestStrokesT.keySet()).freeze();
            for (final String s : keys) {
                Integer bestS = bestStrokesS.get(s);
                Integer bestT = bestStrokesT.get(s);
                String replacement =
                        bestS == null
                                ? bestT.toString()
                                : bestT == null
                                        ? bestS.toString()
                                        : bestS.equals(bestT)
                                                ? bestS.toString()
                                                : bestS + "|" + bestT;
                final String kTotalStrokesString = kTotalStrokes.get(s);
                if (kTotalStrokesString == null) {
                    out.println(
                            "U+"
                                    + Utility.hex(s)
                                    + "\tkTotalStrokes\t"
                                    + replacement.replace('|', ' ')
                                    + "\t#\t"
                                    + s);
                    continue;
                }
                if (kTotalStrokesString.equals(replacement)) {
                    continue;
                }
                overrideOut.println(
                        "U+"
                                + Utility.hex(s)
                                + "\tkTotalStrokes\t"
                                + replacement.replace('|', ' ')
                                + "\t#\t"
                                + s
                                + "\t"
                                + kTotalStrokesString);
            }
        }
    }

    private static int addPinyinFromVariants(String title, int count) {
        for (final Set<Integer> s : variantEquivalents.getEquivalenceSets()) {
            String hasPinyin = null;
            int countHasPinyin = 0;
            for (final Integer cp : s) {
                final String existing = bestPinyin.get(cp);
                if (existing != null) {
                    hasPinyin =
                            existing; // take last one. Might be better algorithm, but for now...
                    countHasPinyin++;
                }
            }
            // see if at least one has a pinyin, and at least one doesn't.
            if (countHasPinyin != s.size() && hasPinyin != null) {
                for (final Integer cp : s) {
                    if (!bestPinyin.containsKey(cp)) {
                        addPinyin(title, UTF16.valueOf(cp), hasPinyin, OverrideItems.keepOld);
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static void addEquivalents(UnicodeMap<String> variantMap) {
        for (final String s : variantMap) {
            final String value = variantMap.get(s);
            if (value == null) {
                continue;
            }
            final int baseCp = s.codePointAt(0);
            for (final String part : ONBAR.split(value)) {
                int cp;
                if (unicodeCp.reset(part).matches()) {
                    cp = Integer.parseInt(unicodeCp.group(1), 16);
                } else {
                    // New in Unicode 10: Characters are given as themselves
                    // rather than as code point numbers.
                    cp = part.codePointAt(0);
                    if (cp < 0x3400) {
                        throw new IllegalArgumentException();
                    }
                }
                variantEquivalents.add(baseCp, cp);
            }
        }
    }

    private static void addRadicals() {
        for (final String s : radicalMap.keySet()) {
            final String main = radicalMap.get(s);
            final Set<String> newValues = mergedPinyin.get(main);
            if (newValues == null) {
                continue;
            }
            for (final String newValue : newValues) {
                addPinyin("radicals", s, newValue, OverrideItems.keepOld);
            }
        }
    }

    private static void addRadicals(UnicodeMap<String> source) {
        for (final String s : radicalMap.keySet()) {
            final String main = radicalMap.get(s);
            final String newValue = source.get(main);
            if (newValue == null) {
                continue;
            }
            source.put(s, newValue);
        }
    }

    private static boolean validPinyin(String pinyin) {
        final Boolean cacheResult = validPinyin.get(pinyin);
        if (cacheResult != null) {
            return cacheResult;
        }
        boolean result;
        final String base = noaccents.transform(pinyin);
        final int initialEnd = INITIALS.findIn(base, 0, true);
        if (initialEnd == 0 && (pinyin.startsWith("i") || pinyin.startsWith("u"))) {
            result = false;
        } else {
            final String finalSegment = base.substring(initialEnd);
            result = finalSegment.length() == 0 ? true : FINALS.contains(finalSegment);
        }
        validPinyin.put(pinyin, result);
        return result;
    }

    private static void addAllKeepingOld(
            String han, String original, PinyinSource pinyin, Iterable<String> pinyinList) {
        int count = 0;
        for (final String source : pinyinList) {
            if (source.length() == 0) {
                throw new IllegalArgumentException();
            }
            addPinyin(pinyin.toString(), han, source, OverrideItems.keepOld);
            ++count;
        }
        if (count == 0) {
            throw new IllegalArgumentException();
        }
    }

    private static void addPinyin(String title, String han, String source, OverrideItems override) {
        if (han.equals("賈")) {
            int debug = 0;
        }
        if (!validPinyin(source)) {
            System.out.println(
                    "***Invalid Pinyin - "
                            + title
                            + ": "
                            + han
                            + "\t"
                            + source
                            + "\t"
                            + Utility.hex(han));
            return;
        }
        source = source.intern();
        final String item = bestPinyin.get(han);
        if (item == null || override == OverrideItems.keepNew) {
            if (!source.equals(item)) {
                if (item != null) {
                    System.out.println(
                            "Overriding Pinyin " + han + "\told: " + item + "\tnew: " + source);
                }
                bestPinyin.put(han, source);
            }
        }
        Set<String> set = mergedPinyin.get(han);
        if (set == null) {
            mergedPinyin.put(han, set = new LinkedHashSet<String>());
        }
        set.add(source);
    }

    private static final class MyFileReader extends SemiFileReader {
        public final Pattern SPLIT = Pattern.compile("\\s*,\\s*");
        String last = "";

        @Override
        protected String[] splitLine(String line) {
            return SPLIT.split(line);
        }

        @Override
        protected boolean isCodePoint() {
            return false;
        }
        ;

        /**
         *
         *
         * <pre>
         * ;Radical Number,Status,Unified_Ideo,Hex,Radical,Hex,Name,Conf.Char,Hex,Unified Ideo. has NORemainingStrokes in Unihan
         * <br>1,Main,一,U+4E00,⼀,U+2F00,ONE
         * </pre>
         */
        @Override
        protected boolean handleLine(int start, int end, String[] items) {
            if (items[0].startsWith(";")) {
                return true;
            }
            if (items[2].length() != 0) {
                last = items[2];
            }

            final String radical = items[4];
            if (NOT_NFC.contains(radical)) {
                return true;
            }
            radicalMap.put(radical, last);
            return true;
        }
    }
    ;

    // 吖 ; a ; 1 ; 251432 ; 0x5416
    private static final class BihuaReader extends SemiFileReader {
        @Override
        protected boolean isCodePoint() {
            return false;
        }
        ;

        Set<String> seen = new HashSet<String>();

        @Override
        protected boolean handleLine(int start, int end, String[] items) {
            final String character = items[0];
            String pinyinBase = items[1];
            final String other = pinyinBase.replace("v", "ü");
            if (!other.equals(pinyinBase)) {
                if (!seen.contains(other)) {
                    System.out.println("bihua: " + pinyinBase + " => " + other);
                    seen.add(other);
                }
                pinyinBase = other;
            }
            final String pinyinTone = items[2];
            final String charSequence = items[3];
            final String hex = items[4];
            if (!hex.startsWith("0x")) {
                throw new RuntimeException(hex);
            } else {
                final int hexValue = Integer.parseInt(hex.substring(2), 16);
                if (!character.equals(UTF16.valueOf(hexValue))) {
                    throw new RuntimeException(hex + "!=" + character);
                }
            }
            final String source = fromNumericPinyin.transform(pinyinBase + pinyinTone);
            bihuaData.put(character, Row.of(source, charSequence));
            return true;
        }
    }
    ;

    private static final class PatchPinyinReader extends SemiFileReader {
        boolean skip = false;

        @Override
        protected boolean isCodePoint() {
            return false;
        }
        ;

        @Override
        protected void processComment(String line, int comment) {
            if (only19 && line.substring(comment).contains("1.9.1")) {
                skip = true;
            }
        }

        @Override
        protected boolean handleLine(int start, int end, String[] items) {
            if (skip) {
                return true;
            }
            if (items.length > 1) {
                if (!UNIHAN.contains(items[0])) {
                    throw new IllegalArgumentException("Non-Unihan character: " + items[0]);
                }
                if (!PINYIN_LETTERS.containsAll(items[1])) {
                    throw new IllegalArgumentException(
                            "Non-Pinyin character: "
                                    + items[1]
                                    + "; "
                                    + new UnicodeSet().addAll(items[1]).removeAll(PINYIN_LETTERS));
                }
                addPinyin("patchPinyin", items[0], items[1], OverrideItems.keepNew);
            }
            return true;
        }
    }

    private static final class PatchStrokeReader extends SemiFileReader {
        final UnicodeMap<Integer> target;
        boolean skip = false;
        private Pattern splitter;

        PatchStrokeReader(UnicodeMap<Integer> target, Pattern splitter) {
            this.target = target;
            this.splitter = splitter;
        }

        protected String[] splitLine(String line) {
            return splitter.split(line);
        }

        @Override
        protected boolean isCodePoint() {
            return false;
        }
        ;

        @Override
        protected void processComment(String line, int comment) {
            if (only19 && line.substring(comment).contains("1.9.1")) {
                skip = true;
            }
        }

        @Override
        protected boolean handleLine(int start, int end, String[] items) {
            if (skip) {
                return true;
            }
            String codepoint = items[0];
            if (codepoint.startsWith("U+")) {
                codepoint = UTF16.valueOf(Integer.parseInt(codepoint.substring(2), 16));
            }
            if (!UNIHAN.contains(codepoint)) {
                throw new IllegalArgumentException(
                        "Non-Unihan character: " + codepoint + ", " + Utility.hex(codepoint));
            }
            if (items.length > 1) {
                String strokeCount = items[1];
                int comma = strokeCount.indexOf(',');
                if (comma >= 0) {
                    strokeCount = strokeCount.substring(0, comma);
                }
                target.put(codepoint, Integer.parseInt(strokeCount));
            }
            return true;
        }
    }

    private static Comparator<String> RSComparator =
            new Comparator<String>() {
                @Override
                public int compare(String s1, String s2) {
                    int c1 = s1.codePointAt(0);
                    assert Character.charCount(c1) == s1.length();
                    int c2 = s2.codePointAt(0);
                    assert Character.charCount(c2) == s2.length();
                    long order1 = getRSLongOrder(c1);
                    long order2 = getRSLongOrder(c2);
                    if (order1 != order2) {
                        return order1 < order2 ? -1 : 1;
                    }
                    return codepointComparator.compare(s1, s2);
                }
            };

    private static class StrokeComparator implements Comparator<String> {
        final UnicodeMap<Integer> baseMap;

        public StrokeComparator(UnicodeMap<Integer> baseMap) {
            this.baseMap = baseMap;
        }

        @Override
        public int compare(String o1, String o2) {
            final Integer n1 = getStrokeValue(o1, baseMap);
            final Integer n2 = getStrokeValue(o2, baseMap);
            if (n1 == null) {
                if (n2 != null) {
                    return 1;
                }
                // both null, fall through
            } else if (n2 == null) {
                return -1;
            } else { // both not null
                final int result = n1 - n2;
                if (result != 0) {
                    return result;
                }
            }
            return RSComparator.compare(o1, o2);
        }
    }

    private static Integer getStrokeValue(String o1, UnicodeMap<Integer> baseMap) {
        final int cp1 = o1.codePointAt(0);
        return baseMap.get(cp1);
    }

    private static Comparator<String> SStrokeComparator = new StrokeComparator(bestStrokesS);
    private static Comparator<String> TStrokeComparator = new StrokeComparator(bestStrokesT);

    private static Comparator<String> PinyinComparator =
            new Comparator<String>() {

                @Override
                public int compare(String o1, String o2) {
                    final String s1 = getPinyin(o1);
                    final String s2 = getPinyin(o2);
                    if (s1 == null) {
                        if (s2 != null) {
                            return 1;
                        }
                    } else if (s2 == null) {
                        return -1;
                    }
                    final int result = pinyinSort.compare(s1, s2);
                    if (result != 0) {
                        return result;
                    }
                    return SStrokeComparator.compare(o1, o2);
                }
            };

    public static String getPinyin(String o1) {
        final int cp1 = o1.codePointAt(0);
        return bestPinyin.get(cp1);
    }

    private static final Relation<InfoType, String> indexValues =
            Relation.of(new EnumMap<InfoType, Set<String>>(InfoType.class), HashSet.class);

    private static String getIndexValue(InfoType infoType, String s, Output<String> comment) {
        String rest;
        switch (infoType) {
            case pinyin:
                final String str = getPinyin(s).toUpperCase(Locale.ENGLISH); // TODO drop accents
                final int first = str.charAt(0);
                if (first < 0x7F) {
                    rest = str.substring(0, 1);
                } else {
                    rest = nfd.normalize(first).substring(0, 1);
                }
                comment.value = rest;
                break;
            case radicalStroke:
                final int codepoint = s.codePointAt(0);
                int data = getRSShortData(codepoint);
                if (data == 0) {
                    throw new IllegalArgumentException(
                            "Missing R-S data for U+" + Utility.hex(codepoint));
                }
                rest = radicalStroke.getRadicalCharFromShortData(data);
                comment.value = radicalStroke.getRadicalStringFromShortData(data);
                break;
            case stroke:
                final Integer strokeCount = getStrokeValue(s, bestStrokesT);
                rest = String.valueOf((char) (INDEX_ITEM_BASE + strokeCount));
                comment.value = String.valueOf(strokeCount);
                break;
            default:
                throw new IllegalArgumentException();
        }
        final String result = infoType.base + rest;
        indexValues.put(infoType, result);
        return result;
    }
}
