package org.unicode.text.tools;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.tools.emoji.Emoji;
import org.unicode.tools.emoji.EmojiData;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.EntryRange;

public class RegexBuilder {
    public enum Style {CODEPOINT_REGEX, CHAR_REGEX}
    public static final UnicodeSet NEEDS_ESCAPE = new UnicodeSet("[[:di:][:Me:][:Mn:][:c:]]")
//          .add(0x1F1E6,0x1F1FF)
            .freeze();

    public static StringBuilder showSet(UnicodeSet us, StringBuilder output) {
        if (us.size() == 1) {
            showString(us.iterator().next(), output);
        } else {
            output.append('[');
            for (EntryRange e : us.ranges()) {
                showChar(e.codepoint, output);
                int count = e.codepointEnd - e.codepoint;
                if (count > 0) {
                    if (count != 1) {
                        output.append('-');
                    };
                    showChar(e.codepointEnd, output);
                }
            }
            for (String s : us.strings()) {
                showString(s, output);
            }
            output.append(']');
        }
        return output;
    }

    public static StringBuilder showString(String s, StringBuilder output) {
        final int[] codePoints = CharSequences.codePoints(s);
        if (codePoints.length == 1) {
            showChar(codePoints[0], output);
        } else {
            output.append('{');
            for (int cp : codePoints) {
                showChar(cp, output);
            }
            output.append('}');
        }
        return output;
    }

    public static StringBuilder showChar(int cp, StringBuilder output) {
        if (NEEDS_ESCAPE.contains(cp)) {
            output.append("\\x{")
            .append(Integer.toHexString(cp).toUpperCase(Locale.ROOT))
            .append("}");
        } else {
            output.appendCodePoint(cp);
        }
        return output;
    }

    private static class Node {
        UnicodeMap<Node> continues = new UnicodeMap<>();
        UnicodeSet finals = new UnicodeSet();
    }

    private class NodeF {
        final UnicodeSet plainSet;
        final UnicodeMap<NodeF> plainMap;
        final UnicodeMap<NodeF> optionalMap;

        public String toString() {
            return "〔" + plainSet + "/" + plainMap + "/" + optionalMap + "〕";
        };

        NodeF(Node source) {
            UnicodeSet finals = source.finals;
            UnicodeMap<NodeF> continues = deepCopy(source.continues);
            UnicodeSet multiKeys = continues.keySet();

            plainSet = new UnicodeSet(finals).removeAll(multiKeys).freeze();
            optionalMap = new UnicodeMap<NodeF>().putAll(continues).retainAll(finals).freeze();
            plainMap = new UnicodeMap<NodeF>().putAll(continues).removeAll(finals).freeze();

        }
        @Override
        public boolean equals(Object obj) {
            NodeF that = (NodeF) obj;
            return that.plainSet.equals(plainSet) 
                    && that.plainMap.equals(plainMap)
                    && that.optionalMap.equals(optionalMap)
                    ;
        }
        @Override
        public int hashCode() {
            int result = plainSet.hashCode();
            for (NodeF f : plainMap.values()) {
                result *= 37;
                result ^= plainMap.getSet(f).hashCode();
                result ^= f.hashCode();
            }
            for (NodeF f : optionalMap.values()) {
                result *= 37;
                result ^= optionalMap.getSet(f).hashCode();
                result ^= f.hashCode();
            }
            return result;
        }

        UnicodeMap<NodeF> deepCopy(UnicodeMap<Node> continues) {
            UnicodeMap<NodeF> result = new UnicodeMap<>();
            for (Node n : continues.values()) {
                result.putAll(continues.getSet(n), new NodeF(n));
            }
            return result.freeze();
        }

        private StringBuilder print(int depth, boolean showLevel, boolean optional, StringBuilder output) {

            int mapItemCount = countItems(plainMap)
                    + countItems(optionalMap);
            int setCount = plainSet.isEmpty() ? 0 : 1;
            final boolean needsParen = (mapItemCount + setCount) > 1 || optional && mapItemCount != 0;

            if (needsParen) {
                output.append('(');
            }
            boolean first = true;
            if (!plainSet.isEmpty()) {
                first = false;
                showSet(plainSet, output);
            }
            if (!plainMap.isEmpty()) {
                first = print(plainMap, depth, showLevel, output, first, false);
            }
            if (!optionalMap.isEmpty()) {
                first = print(optionalMap, depth, showLevel, output, first, true);
            }
            if (needsParen) {
                output.append(')');
            }
            if (optional) {
                output.append("?");
            }
            return output;
        }

        private boolean print(UnicodeMap<NodeF> plainMap, int depth, boolean showLevel, StringBuilder output, boolean first, boolean optional) {
            for (NodeF n : plainMap.values()) {
                if (first) {
                    first = false;
                } else {
                    if (showLevel) {
                        output.append('\n' + Utility.repeat("\t", depth));
                    }
                    output.append('|');
                }
                UnicodeSet us = plainMap.getSet(n);
                showSet(us, output);
                n.print(depth+1, false, optional, output);
            }
            return first;
        }


    }


    // TODO fix hashcode
    // TODO make strings be empty not null
    private static int countItems(UnicodeMap<?> unicodeMap) {
        return unicodeMap.values().size();
    }

    Node data = new Node();
    NodeF dataF;
    Style style;

    public RegexBuilder(Style style) {
        this.style = style;
    }

    public RegexBuilder finish() {
        dataF = new NodeF(data);
        return this;
    }

    public RegexBuilder addAll(Iterable<String> source) {
        for (String s : source) {
            add(s);
        }
        return this;
    }

    public RegexBuilder add(String s) {
        int[] list;
        if (style == Style.CHAR_REGEX) {
            list = new int[s.length()];
            for (int i = 0; i < s.length(); ++i) {
                list[i] = s.charAt(i);
            }
        } else {
            list = CharSequences.codePoints(s);
        }
        add(list, 0, data);
        return this;
    }

    private void add(int[] list, int pos, Node data2) {
        int cp = list[pos];
        if (pos == list.length - 1) {
            data2.finals.add(cp);
        } else {
            Node node2 = data2.continues.get(cp);
            if (node2 == null) {
                data2.continues.put(cp, node2 = new Node());
            }
            add(list, pos+1, node2);
        }
    }

    @Override
    public String toString() {
        return dataF.print(0, false, false, new StringBuilder()).toString();
    }

    public String toString(boolean showLevel) {
        return dataF.print(0, showLevel, false, new StringBuilder()).toString();
    }
}
