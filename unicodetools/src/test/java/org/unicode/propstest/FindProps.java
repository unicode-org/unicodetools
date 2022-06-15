package org.unicode.propstest;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.ULocale;
import java.util.EnumSet;
import java.util.Map.Entry;
import java.util.Set;
import org.unicode.cldr.util.With;
import org.unicode.props.GenerateEnums;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;

public class FindProps {
    static final IndexUnicodeProperties latest =
            IndexUnicodeProperties.make(GenerateEnums.ENUM_VERSION);
    static final UnicodeMap<String> names = latest.load(UcdProperty.Name);

    public interface SetMaker<T> {
        Set<T> make();
    }

    public static class UnicodeRelation<T> {
        final UnicodeMap<Set<T>> data;
        final SetMaker<T> setMaker;

        public UnicodeRelation(SetMaker<T> setMaker) {
            this.setMaker = setMaker;
            data = new UnicodeMap<Set<T>>();
        }

        public void addAll(UnicodeSet keySet, T value) {
            for (UnicodeSetIterator it = new UnicodeSetIterator(keySet); it.next(); ) {
                if (it.codepoint == UnicodeSetIterator.IS_STRING) {
                    add(it.string, value);
                } else {
                    add(it.codepoint, value);
                }
            }
        }

        public void add(String codePoint, T value) {
            // make sure we don't just mutate what is there, since it is shared
            Set<T> s = data.get(codePoint);
            Set<T> s2 = setMaker.make();
            s2.add(value);
            if (s != null) {
                s2.addAll(s);
            }
            data.put(codePoint, s2);
        }

        public void add(int codePoint, T value) {
            // make sure we don't just mutate what is there, since it is shared
            Set<T> s = data.get(codePoint);
            Set<T> s2 = setMaker.make();
            s2.add(value);
            if (s != null) {
                s2.addAll(s);
            }
            data.put(codePoint, s2);
        }

        public String toString(Transform<String, String> getName) {
            StringBuilder result = new StringBuilder();
            for (Entry<String, Set<T>> unicodeData : data.entrySet()) {
                String s = unicodeData.getKey();
                Set<T> withS = unicodeData.getValue();
                result.append(
                        "U+"
                                + Utility.hex(s, 4, " U+")
                                + "\t"
                                + CollectionUtilities.join(withS, " ")
                                + "\t# (\t"
                                + s
                                + "\t)"
                                + "\t"
                                + getName.transform(s)
                                + "\n");
            }
            return result.toString();
        }

        public UnicodeSet keySet() {
            return data.keySet();
        }
    }

    static Transform<String, String> GET_NAME =
            new Transform<String, String>() {
                public String transform(String s) {
                    int len = UTF16.countCodePoint(s);
                    if (len == 1) {
                        return names.get(s);
                    }
                    StringBuilder result = new StringBuilder();
                    StringBuilder cc = new StringBuilder();
                    for (int cp : With.codePointArray(s)) {
                        if (result.length() > 0) {
                            result.append(" + ");
                        }
                        result.append(names.get(cp));
                        if (cp >= 0x1F1E6 && cp <= 0x1F1FF) {
                            cc.appendCodePoint(cp - 0x1F1E6 + 'A');
                        }
                    }
                    if (cc.length() > 0) {
                        result.append("\t[\t")
                                .append(
                                        ULocale.getDisplayCountry("und_" + cc, ULocale.ENGLISH)
                                                + "\t]");
                    }
                    return result.toString();
                }
            };

    enum Source {
        dcm,
        kddi,
        sb,
        apple,
        sv,
        poss
    }

    public static void main(String[] args) {
        final UnicodeMap<String> variants = latest.load(UcdProperty.Standardized_Variant);
        final UnicodeMap<String> Emoji_DCM = latest.load(UcdProperty.Emoji_DCM);
        final UnicodeMap<String> Emoji_KDDI = latest.load(UcdProperty.Emoji_KDDI);
        final UnicodeMap<String> Emoji_SB = latest.load(UcdProperty.Emoji_SB);

        final UnicodeRelation<Source> emoji =
                new UnicodeRelation<Source>(
                        new SetMaker<Source>() {
                            public Set<Source> make() {
                                return EnumSet.noneOf(Source.class);
                            }
                        });
        emoji.addAll(Emoji_DCM.keySet(), Source.dcm);
        emoji.addAll(Emoji_KDDI.keySet(), Source.kddi);
        emoji.addAll(Emoji_SB.keySet(), Source.sb);
        for (String s : variants.getSet("text-style")) {
            emoji.add(s.codePointAt(0), Source.sv);
        }
        UnicodeSet apples =
                new UnicodeSet(
                        "[🌍🌎🌐🌒🌖-🌘🌚🌜-🌞🌲🌳🍋🍐🍼🏇🏉🏤 🐀-🐋🐏🐐🐓🐕🐖🐪👥👬👭💭💶💷📬📭📯📵🔀-🔂 🔄-🔇🔉🔕🔬🔭🕜-🕧😀😇😈😎😐😑😕😗😙😛😟 😦😧😬😮😯😴😶🚁🚂🚆🚈🚊🚍🚎🚐🚔🚖🚘🚛-🚡 🚣🚦🚮-🚱🚳-🚵🚷🚸🚿🛁-🛅]");
        emoji.addAll(apples, Source.apple);
        UnicodeSet other =
                new UnicodeSet(
                        "[⏭-⏯⏱⏲☂-☍☏☐☒☓☘-☜☞-☣☦-☯ ☸☹☻☼♔-♟♡♢♤♧♩-♯♾⚀-⚅⚐-⚒ ⚔-⚝⛀-⛃⛆-⛈⛌⛍⛏-⛓⛕-⛡⛣-⛩⛫-⛱ ⛴⛶-⛹⛻⛼⛾⛿✁✃✄✆✇✍✎✐✑✓✕✗✘ ❍❏-❒❖❘-❚🂠-🂮🂱-🂾🃁-🃎🃑-🃟🔈 🕀-🕃🚋]");
        for (String s : ULocale.getISOCountries()) {
            int first = s.codePointAt(0) - 'A' + 0x1F1E6;
            int second = s.codePointAt(1) - 'A' + 0x1F1E6;
            other.add(
                    new StringBuilder().appendCodePoint(first).appendCodePoint(second).toString());
        }
        other.removeAll(emoji.keySet());
        emoji.addAll(other, Source.poss);
        System.out.println("# Code Point(s)\tSources\t\tChar\t\tNames\t\t*Countries");
        System.out.println(emoji.toString(GET_NAME));
    }
}
