package org.unicode.text.tools;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.props.GenerateEnums;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.text.tools.Emoji.ModifierStatus;
import org.unicode.text.tools.GenerateEmoji.CharSource;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

import com.google.common.base.Splitter;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.VersionInfo;

public class EmojiData {
    private static UnicodeSet SUPPRESS_SECONDARY = new UnicodeSet("[😀 😁 😂 😃 😄 😅 😆 😉 😊 😋 😎 😍 😘 😗 😙 😚 ☺ 🙂 🤗 😇 🤔 😐 😑 😶 🙄 😏 😣 😥 😮 🤐 😯 😪 😫 😴 😌 🤓 😛 😜 😝 ☹ 🙁 😒 😓 😔 😕 😖 🙃 😷 🤒 🤕 🤑 😲 😞 😟 😤 😢 😭 😦 😧 😨 😩 😬 😰 😱 😳 😵 😡 😠 👿 😈]").freeze();
    public enum DefaultPresentation {text, emoji}
    public enum EmojiLevel {L1, L2}

    public static class EmojiDatum {
        public final DefaultPresentation style;
        public final EmojiLevel level;
        public final ModifierStatus modifierStatus;
        public final Set<CharSource> sources;

        /**
         * Sets must be immutable.
         * @param style
         * @param order
         * @param annotations
         * @param sources
         */
        private EmojiDatum(DefaultPresentation style, EmojiLevel level, ModifierStatus modifierClass, Set<CharSource> sources) {
            this.style = style;
            this.level = level;
            this.modifierStatus = modifierClass;
            this.sources = sources;
        }

        @Override
        public String toString() {
            return "{" + style + ", " + level + ", " + modifierStatus + ", " + sources + "}";
        }
    }

    final UnicodeMap<EmojiDatum> data = new UnicodeMap<>();
    final Map<DefaultPresentation, UnicodeSet> defaultPresentationMap;
    final Map<EmojiLevel, UnicodeSet> levelMap;
    final Map<ModifierStatus, UnicodeSet> modifierClassMap;
    final Map<CharSource, UnicodeSet> charSourceMap;
    final VersionInfo version;

    static final Splitter semi = Splitter.on(";").trimResults();
    static final Splitter comma = Splitter.on(",").trimResults();

    static final ConcurrentHashMap<VersionInfo, EmojiData> cache = new ConcurrentHashMap<>();

    public static EmojiData of(VersionInfo version) {
        EmojiData result = cache.get(version);
        if (result == null) {
            result = new EmojiData(version);
            cache.put(version, result);
        }
        return result;
    }

    private EmojiData(VersionInfo version) {
        EnumMap<DefaultPresentation, UnicodeSet> _defaultPresentationMap = new EnumMap<>(DefaultPresentation.class);
        EnumMap<EmojiLevel, UnicodeSet> _levelMap = new EnumMap<>(EmojiLevel.class);
        EnumMap<ModifierStatus, UnicodeSet> _modifierClassMap = new EnumMap<>(ModifierStatus.class);
        EnumMap<CharSource, UnicodeSet> _charSourceMap = new EnumMap<>(CharSource.class);
        // /Users/markdavis/workspace/unicode-draft/Public/emoji/2.0/emoji-data.txt

        this.version = version;
        for (String line : FileUtilities.in(Settings.DATA_DIR + "emoji/" + version.getVersionString(2, 4), "emoji-data.txt")) {
            //# Code ;  Default Style ; Ordering ;  Annotations ;   Sources #Version Char Name
            // U+263A ;    text ;  0 ; face, human, outlined, relaxed, smile, smiley, smiling ;    jw  # V1.1 (☺) white smiling face
            if (line.startsWith("#")) continue;
            List<String> list = semi.splitToList(line);
            // 00A9 ;   text ;  L1 ;    none ;  j   # V1.1 (©) COPYRIGHT SIGN
            // 2639 ;   text ;  L2 ;    secondary ; w   # V1.1 (☹) WHITE FROWNING FACE
            String codePoint = Utility.fromHex(list.get(0)); // .replace("U+","")
            DefaultPresentation styleIn = DefaultPresentation.valueOf(list.get(1));

            EmojiLevel levelIn = EmojiLevel.valueOf(list.get(2));
            ModifierStatus modClass = Emoji.IS_BETA && SUPPRESS_SECONDARY.contains(codePoint) 
                    ? ModifierStatus.none 
                            : ModifierStatus.valueOf(list.get(3));
            Set<CharSource> sourcesIn = getSet(_charSourceMap, codePoint, list.get(4));
            data.put(codePoint, new EmojiDatum(styleIn, levelIn, modClass, sourcesIn));
            putUnicodeSetValue(_defaultPresentationMap, codePoint, styleIn);
            putUnicodeSetValue(_levelMap, codePoint, levelIn);
            putUnicodeSetValue(_modifierClassMap, codePoint, modClass);
            putUnicodeSetValue(_defaultPresentationMap, codePoint, styleIn);
        }
        freezeUnicodeSets(_defaultPresentationMap.values());
        freezeUnicodeSets(_charSourceMap.values());
        levelMap = Collections.unmodifiableMap(_levelMap);
        modifierClassMap = Collections.unmodifiableMap(_modifierClassMap);
        defaultPresentationMap = Collections.unmodifiableMap(_defaultPresentationMap);
        charSourceMap = Collections.unmodifiableMap(_charSourceMap);
        data.freeze();
    }

    public static void freezeUnicodeSets(Collection<UnicodeSet> collection) {
        for (UnicodeSet value : collection) {
            value.freeze();
        }
    }

    public EmojiDatum getData(String codePointString) {
        return data.get(codePointString);
    }

    public EmojiDatum getData(int codePoint) {
        return data.get(codePoint);
    }

    public Set<EmojiLevel> getLevels() {
        return levelMap.keySet();
    }

    public Set<ModifierStatus> getModifierStatuses() {
        return modifierClassMap.keySet();
    }

    public UnicodeSet getLevelSet(EmojiLevel source) {
        return CldrUtility.ifNull(levelMap.get(source), UnicodeSet.EMPTY);
    }

    public UnicodeSet getModifierStatusSet(ModifierStatus source) {
        return CldrUtility.ifNull(modifierClassMap.get(source), UnicodeSet.EMPTY);
    }

    public UnicodeSet getDefaultPresentationSet(DefaultPresentation defaultPresentation) {
        return CldrUtility.ifNull(defaultPresentationMap.get(defaultPresentation),UnicodeSet.EMPTY);
    }

    public UnicodeSet getCharSourceSet(CharSource charSource) {
        return CldrUtility.ifNull(charSourceMap.get(charSource), UnicodeSet.EMPTY);
    }

    private static Set<CharSource> getSet(EnumMap<CharSource, UnicodeSet> _defaultPresentationMap, String source, String string) {
        if (string.isEmpty()) {
            return Collections.emptySet();
        }
        EnumSet<CharSource> result = EnumSet.noneOf(CharSource.class);
        for (CharSource cs : CharSource.values()) {
            if (string.contains(cs.letter)) {
                result.add(cs);
                putUnicodeSetValue(_defaultPresentationMap, source, cs);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    public static <T> void putUnicodeSetValue(
            Map<T, UnicodeSet> map,
            String key, T value) {
        UnicodeSet us = map.get(value);
        if (us == null) {
            map.put(value, us = new UnicodeSet());
        }
        us.add(key);
    }

    //    public final Comparator<String> EMOJI_COMPARATOR = new Comparator<String>() {
    //
    //        @Override
    //        public int compare(String o1, String o2) {
    //            int i1 = 0, i2 = 0;
    //            while (true) {
    //                if (i1 == o1.length()) {
    //                    return i2 == o2.length() ? 0 : -1;
    //                } else if (i2 == o2.length()) {
    //                    return 1;
    //                }
    //                int cp1 = o1.codePointAt(i1);
    //                int cp2 = o2.codePointAt(i2);
    //                if (cp1 != cp2) {
    //                    EmojiDatum d1 = getData(cp1);
    //                    EmojiDatum d2 = getData(cp2);
    //                    if (d1 == null) {
    //                        return d2 == null ? cp1 - cp2 : 1;
    //                    } else {
    //                        return d2 == null ? -1 : d1.order - d2.order;
    //                    }
    //                }
    //                i1 += Character.charCount(cp1);
    //                i2 += Character.charCount(cp2);
    //                continue;
    //            }
    //        }       
    //    };
    //
    public static void main(String[] args) {
        final IndexUnicodeProperties latest = IndexUnicodeProperties.make(GenerateEnums.ENUM_VERSION);
        final UnicodeMap<String> names = latest.load(UcdProperty.Name);

        EmojiData emojiData = new EmojiData(VersionInfo.getInstance(1));
        show(0x26e9, names, emojiData);
        System.out.println("L1" + ", " + emojiData.getLevelSet(EmojiLevel.L1).toPattern(false));
        System.out.println("modifier" + ", " + emojiData.getModifierStatusSet(ModifierStatus.modifier).toPattern(false));
        System.out.println(CharSource.WDings  + ", " + emojiData.getCharSourceSet(CharSource.WDings).toPattern(false));
        System.out.println(DefaultPresentation.emoji + ", " + emojiData.getDefaultPresentationSet(DefaultPresentation.emoji).toPattern(false));
        EmojiData emojiData2 = new EmojiData(VersionInfo.getInstance(2));
        show(0x1F3CB, names, emojiData);
        show(0x1F3CB, names, emojiData2);
    }

    private static void show(int cp, final UnicodeMap<String> names, EmojiData emojiData) {
        System.out.println(emojiData.version + "\t" + Utility.hex(cp) + ", " + emojiData.getData(cp) + "\t" + names.get(cp));
    }
}
