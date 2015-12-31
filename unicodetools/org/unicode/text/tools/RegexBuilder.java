package org.unicode.text.tools;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.UnicodeSet;

public class RegexBuilder {
    public enum Style {CODEPOINT_REGEX, CHAR_REGEX}

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
                output.append(showSet(plainSet));
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
                output.append(showSet(us));
                n.print(depth+1, false, optional, output);
            }
            return first;
        }

        private String showSet(UnicodeSet us) {
            return us.size() != 1 ? us.toPattern(style == Style.CHAR_REGEX) 
                    : style != Style.CHAR_REGEX ? us.iterator().next() 
                            : "\\u" + Utility.hex(us.iterator().next()); // fix
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

    public static void main(String[] args) {
        EmojiData edata = EmojiData.of(Emoji.VERSION_TO_GENERATE);
        UnicodeSet testSet = new UnicodeSet()
        .addAll(edata.getChars())
        .addAll(edata.getModifierSequences())
        .addAll(edata.getZwjSequencesNormal())
        .freeze();
        RegexBuilder b = new RegexBuilder(RegexBuilder.Style.CHAR_REGEX)
        .addAll(testSet)
        .finish();
        String result = b.toString(true) + "\\u20E0?";
        System.out.println(result);
        result = b.toString(false) + "\\u20E0?";
        Pattern check = Pattern.compile(result);
        Matcher checkMatcher = check.matcher("");
        UnicodeSet others = new UnicodeSet(0,0x10FFFF);
        for (String s : testSet) {
            if (!checkMatcher.reset(s).matches()) {
                System.out.println("FAILS: " + s);
            }
            if (!checkMatcher.reset(s+'\u20E0').matches()) {
                System.out.println("FAILS: " + s);
            }
            others.remove(s);
        }
        for (String s : others) {
            if (checkMatcher.reset(s).matches()) {
                System.out.println("Doesn't FAIL (but should): " + s);
            }
        }

        //        b = new RegexBuilder(RegexBuilder.Style.CHAR_REGEX)
        //        .addAll(edata.getChars())
        //        .addAll(edata.getModifierSequences())
        //        .addAll(edata.getZwjSequencesNormal())
        //        .finish();
        //        System.out.println(b.toString(true));

    }
}
