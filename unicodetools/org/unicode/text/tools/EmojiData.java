package org.unicode.text.tools;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.With;
import org.unicode.text.tools.GenerateEmoji.CharSource;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

import com.google.common.base.Splitter;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;

public class EmojiData {
    public enum DefaultPresentation {text, emoji}
    
    final DefaultPresentation style;
    final int order;
    final Set<String> annotations;
    final Set<CharSource> sources;
    
    private EmojiData(DefaultPresentation style, int order, Set<String> annotations, Set<CharSource> sources) {
        this.style = style;
        this.order = order;
        this.annotations = annotations;
        this.sources = sources;
    }
    
    @Override
    public String toString() {
        return style + "; " + order + "; " + annotations + "; " + sources;
    }
    
    static final UnicodeMap<EmojiData> data = new UnicodeMap<>();
    static final Map<String, UnicodeSet> annotationMap;
    static final Map<DefaultPresentation, UnicodeSet> defaultPresentationMap;
    static final Map<CharSource, UnicodeSet> charSourceMap;
    
    static final Splitter semi = Splitter.on(";").trimResults();
    static final Splitter comma = Splitter.on(",").trimResults();

    static {
        HashMap<String, UnicodeSet> _annotationMap = new HashMap<>();
        EnumMap<DefaultPresentation, UnicodeSet> _defaultPresentationMap = new EnumMap<>(DefaultPresentation.class);
        EnumMap<CharSource, UnicodeSet> _charSourceMap = new EnumMap<>(CharSource.class);
        
        for (String line : FileUtilities.in(Settings.UNICODE_DRAFT_DIRECTORY + "reports/tr51/", "emoji-data.txt")) {
            //# Code ;  Default Style ; Ordering ;  Annotations ;   Sources #Version Char Name
            // U+263A ;    text ;  0 ; face, human, outlined, relaxed, smile, smiley, smiling ;    jw  # V1.1 (☺) white smiling face
            if (line.startsWith("#")) continue;
            List<String> list = semi.splitToList(line);
            String codePoint = Utility.fromHex(list.get(0).replace("U+",""));
            DefaultPresentation styleIn = DefaultPresentation.valueOf(list.get(1));
            putUnicodeSetValue(_defaultPresentationMap, codePoint, styleIn);
            int orderIn = Integer.parseInt(list.get(2));
            Set<String> annotationsIn = getAnnotations(_annotationMap, codePoint, list);
            Set<CharSource> sourcesIn = getSet(_charSourceMap, codePoint, list.get(4));
            data.put(codePoint, new EmojiData(styleIn, orderIn, annotationsIn, sourcesIn));
        }
        freezeUnicodeSets(_annotationMap.values());
        freezeUnicodeSets(_defaultPresentationMap.values());
        freezeUnicodeSets(_charSourceMap.values());
        annotationMap = Collections.unmodifiableMap(_annotationMap);
        defaultPresentationMap = Collections.unmodifiableMap(_defaultPresentationMap);
        charSourceMap = Collections.unmodifiableMap(_charSourceMap);
        data.freeze();
    }

    public static void freezeUnicodeSets(Collection<UnicodeSet> collection) {
        for (UnicodeSet value : collection) {
            value.freeze();
        }
    }

    private static Set<String> getAnnotations(HashMap<String, UnicodeSet> _annotationMap, String source, List<String> list) {
        Set<String> result = new HashSet();
        for (String s : comma.split(list.get(3))) {
            result.add(s);
            putUnicodeSetValue(_annotationMap, source, s);
        }
        return Collections.unmodifiableSet(result);
    }
    
    public static EmojiData getData(String codePointString) {
        return data.get(codePointString);
    }
    
    public static Set<String> getAnnotations() {
        return annotationMap.keySet();
    }

    public static UnicodeSet getAnnotationSet(String annotationString) {
        UnicodeSet result = annotationMap.get(annotationString);
        return result == null ? UnicodeSet.EMPTY : result;
    }

    public static UnicodeSet getDefaultPresentationSet(DefaultPresentation defaultPresentation) {
        UnicodeSet result = defaultPresentationMap.get(defaultPresentation);
        return result == null ? UnicodeSet.EMPTY : result;
    }

    public static UnicodeSet getCharSourceSet(CharSource charSource) {
        UnicodeSet result = charSourceMap.get(charSource);
        return result == null ? UnicodeSet.EMPTY : result;
    }

    
    private static Set<CharSource> getSet(EnumMap<CharSource, UnicodeSet> _defaultPresentationMap, String source, String string) {
        if (string.isEmpty()) return Collections.EMPTY_SET;
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

    public static void main(String[] args) {
        System.out.println("\u26e9" + ", " + getData("\u26e9"));
        System.out.println("human" + ", " + getAnnotationSet("human").toPattern(false));
        System.out.println(CharSource.WDings  + ", " + getCharSourceSet(CharSource.WDings).toPattern(false));
        System.out.println(DefaultPresentation.emoji + ", " + getDefaultPresentationSet(DefaultPresentation.emoji).toPattern(false));
    }
}
