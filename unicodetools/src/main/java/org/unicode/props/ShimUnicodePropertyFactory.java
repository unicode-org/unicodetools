package org.unicode.props;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.EntryRange;
import org.unicode.props.UnicodeProperty.BaseProperty;

public class ShimUnicodePropertyFactory extends UnicodeProperty.Factory {

    public ShimUnicodePropertyFactory(UnicodeProperty.Factory factory) {
        for (String propName : factory.getAvailableNames()) {
            UnicodeProperty prop = factory.getProperty(propName);
            switch (propName) {
                case "Joining_Type":
                    prop = modifyJoining_Type(prop);
                    break;
                case "Bidi_Mirroring_Glyph":
                    prop = modifyBidi_Mirroring_Glyph(prop);
                    break;
                case "Bidi_Paired_Bracket":
                    prop = modifyBidi_Paired_Bracket(prop);
                    break;
            }
            add(prop);
        }
    }

    private UnicodeProperty modifyBidi_Paired_Bracket(UnicodeProperty prop) {
        UnicodeMap<String> map = prop.getUnicodeMap();
        UnicodeMap<String> newMap = new UnicodeMap<>(map);
        UnicodeSet nullValues = map.getSet(null);
        for (EntryRange range : nullValues.ranges()) {
            for (int cp = range.codepoint; cp <= range.codepointEnd; ++cp) {
                // set all the values to NUL
                newMap.put(cp, "\u0000");
            }
        }
        return newProp(prop, newMap);
    }

    private UnicodeProperty modifyBidi_Mirroring_Glyph(UnicodeProperty prop) {
        UnicodeMap<String> map = prop.getUnicodeMap();
        UnicodeMap<String> newMap = new UnicodeMap<>(map);
        // for each null valued range
        for (EntryRange range : map.keySet().complement().ranges()) {
            for (int cp = range.codepoint; cp <= range.codepointEnd; ++cp) {
                // set all the values to identity
                newMap.put(cp, UTF16.valueOf(cp));
            }
        }
        return newProp(prop, newMap);
    }

    private UnicodeProperty modifyJoining_Type(UnicodeProperty prop) {
        UnicodeMap<String> map = new UnicodeMap<>(prop.getUnicodeMap());
        UnicodeSet defaultTransparent = new UnicodeSet("[[:Cf:][:Me:][:Mn:]]");
        for (EntryRange range : defaultTransparent.ranges()) {
            for (int cp = range.codepoint; cp <= range.codepointEnd; ++cp) {
                String oldValue = map.get(cp);
                if (oldValue.equals("Non_Joining")) {
                    map.put(cp, "Transparent");
                }
            }
        }
        return newProp(prop, map);
    }
    
    public BaseProperty newProp(UnicodeProperty prop, UnicodeMap<String> newMap) {
        return new UnicodeProperty.UnicodeMapProperty()
                .set(newMap)
                .setMain(
                        prop.getName(),
                        prop.getFirstNameAlias(),
                        prop.getType(),
                        prop.getVersion());
    }
}
