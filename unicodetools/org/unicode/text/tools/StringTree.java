package org.unicode.text.tools;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.unicode.tools.emoji.EmojiData;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeMap.EntryRange;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;


public class StringTree {
    static final Transliterator SHOW = Transliterator.createFromRules("foo", "([[:c:][:z:][:di:]-[\\ \\x0A]]) > &hex($1);", Transliterator.FORWARD);

    abstract static class CPNode<T extends CPNode<T>> implements Iterable<EntryRange<T>> {
        static final int NO_VALUE = Integer.MIN_VALUE;
        abstract public int getValue();
        public boolean hasValue() {
            return getValue() != NO_VALUE;
        }
        abstract public boolean childless();
        abstract public int childCount();
        abstract public Iterator<EntryRange<T>> iterator();
        abstract public Set<T> values();
        abstract public UnicodeSet set(T value);
        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            toString(result, "\n");
            return SHOW.transform(result.toString());
        }

        void toString(StringBuilder result, String indent) {
            for (EntryRange<T> entry : this) {
                result.append(indent);
                if (entry.codepoint == entry.codepointEnd) {
                    result.appendCodePoint(entry.codepoint);
                } else {
                    result.append('[');
                    result.appendCodePoint(entry.codepoint);
                    result.append('-');
                    result.appendCodePoint(entry.codepointEnd);
                    result.append(']');
                }
                if (entry.value.getValue() != NO_VALUE) {
                    result.append('«').append(entry.value.getValue()).append('»');
                }
                if (!entry.value.childless()) {
                    entry.value.toString(result, indent + "  ");
                }
            }        
        }
    }

    static final Comparator<CPNode> CPNODE_COMPARATOR = new Comparator<CPNode>() {
        @Override
        public int compare(CPNode o1, CPNode o2) {
            if (o1 == o2) {
                return 0;
            }
            int diff;
            
            if (0 != (diff = o1.getValue() - o2.getValue())) {
                return diff;
            }
            int childCount1 = o1.childCount();
            if (0 != (diff = childCount1 - o2.childCount())) {
                return diff;
            }
            if (childCount1 == 0) {
                return 0;
            }
            for (Iterator<EntryRange<CPNode>> it1 = o1.iterator(), it2 = o2.iterator();
                    it1.hasNext();) {
                EntryRange<CPNode> range1 = it1.next(), range2 = it2.next();
                // This is more complicated. 
                // We treat null values as less than everything else.
                // so the key is that when we find the lowest range
                if (0 != (range1.codepoint - range2.codepoint)) {
                    return diff;
                }
                if (0 != (this.compare(range1.value, range2.value))) {
                    return diff;
                }
                // this is more complicated. So that it works properly we have to
                // compare the value at minEnd+1
                if (0 != (range2.codepointEnd - range1.codepointEnd)) {
                    return diff;
                }
            }
            return 0;
        }
    };
    
    static class CpWrapper {
        final public CPNode item;
        public CpWrapper(CPNode item) {
            this.item = item;
        }
        @Override
        public boolean equals(Object obj) {
            return equal(item, ((CpWrapper)obj).item);
        }
        
        @Override
        public int hashCode() {
            return item.childCount() * 37 ^ item.getValue();
        }

        public static boolean equal(CPNode o1, CPNode o2) {
            if (o1 == o2) {
                return true;
            }
            if (o1.getValue() != o2.getValue()) {
                return false;
            }
            int childCount1 = o1.childCount();
            if (childCount1 != o2.childCount()) {
                return false;
            }
            if (childCount1 == 0) {
                return true;
            }
            for (Iterator<EntryRange<CPNode>> it1 = o1.iterator(), it2 = o2.iterator();
                    it1.hasNext();) {
                EntryRange<CPNode> range1 = it1.next(), range2 = it2.next();
                if (range1.codepoint != range2.codepoint 
                        || range2.codepointEnd != range1.codepointEnd 
                        || !equal(range1.value, range2.value)) {
                    return false;
                }
            }
            return true;
        }
    }
    
    static class ImmutableCPNode extends CPNode<ImmutableCPNode> {
        static final UnicodeMap<ImmutableCPNode> EMPTY = new UnicodeMap<ImmutableCPNode>().freeze();
        final int stringValue;
        final private UnicodeMap<ImmutableCPNode> data;

        private ImmutableCPNode(int stringValue, UnicodeMap<ImmutableCPNode> data) {
            super();
            this.stringValue = stringValue;
            this.data = data;
        }

        public static ImmutableCPNode copy(CPNodeBuilder source) {
            return copy(source, new HashMap<CpWrapper,ImmutableCPNode>());
        }
        
        public static ImmutableCPNode copy(CPNodeBuilder source, Map<CpWrapper, ImmutableCPNode> cache) {
            // need special map
            CpWrapper wrapped = new CpWrapper(source);
            ImmutableCPNode result = cache.get(wrapped);
            if (result == null) {
                int _stringValue = source.getValue();
                UnicodeMap<ImmutableCPNode> _data;
                if (source.childless()) {
                    _data = EMPTY;
                } else {
                    _data = new UnicodeMap<>();
                    for (EntryRange<CPNodeBuilder> range : source) {
                        _data.putAll(range.codepoint, range.codepointEnd, copy(range.value, cache));
                    }
                    _data.freeze();
                }
                result = new ImmutableCPNode(_stringValue, _data);
                cache.put(wrapped, result);
            }
            return result;
        }

        @Override
        public int getValue() {
            return stringValue;
        }

        @Override
        public boolean childless() {
            return data.isEmpty();
        }

        @Override
        public Iterator<EntryRange<ImmutableCPNode>> iterator() {
            return data.entryRanges().iterator();
        }

        @Override
        public int childCount() {
            return data.size();
        }

        @Override
        public Set<ImmutableCPNode> values() {
            return data.values();
        }

        @Override
        public UnicodeSet set(ImmutableCPNode value) {
            return data.getSet(value);
        }

    }

    static class CPNodeBuilder extends CPNode<CPNodeBuilder> {
        int stringValue = NO_VALUE;
        private UnicodeMap<CPNodeBuilder> data = new UnicodeMap<>();

        public CPNodeBuilder() {}

        private CPNodeBuilder setValue(int value) {
            this.stringValue = value;
            return this;
        }

        public CPNodeBuilder addAll(Collection<String> sources, int value) {
            for (String s : sources) {
                add(s, value);
            }
            return this;
        }

        public CPNodeBuilder add(String s, int value) {
            int base = s.codePointAt(0);
            int endBase = Character.charCount(base);
            CPNodeBuilder old = data.get(base);
            boolean noRemainder = endBase == s.length();
            if (old == null) {
                old = new CPNodeBuilder();
                if (noRemainder) {
                    old.setValue(value);
                }
                data.put(base, old);
            }
            if (noRemainder) {
                old.stringValue = value;
            } else {
                old.add(s.substring(endBase), value);
            }
            return this;
        }

        @Override
        public Iterator<EntryRange<CPNodeBuilder>> iterator() {
            return data.entryRanges().iterator();
        }

        @Override
        public int getValue() {
            return stringValue;
        }

        @Override
        public boolean childless() {
            return data.isEmpty();
        }

        ImmutableCPNode build() {
            return ImmutableCPNode.copy(this);
        }

        @Override
        public int childCount() {
            return data.size();
        }

        @Override
        public Set<CPNodeBuilder> values() {
            return data.values();
        }

        @Override
        public UnicodeSet set(CPNodeBuilder value) {
            return data.getSet(value);
        }
    }

    // TODO reorder to be longest
    static class RegexBuilder {
        public static String getRegex(CPNode baseNode) {
            StringBuilder result = new StringBuilder();
            getRegex(baseNode, result);
            return result.toString();
        }

        private static void getRegex(CPNode baseNode, StringBuilder result) {
            // find out what's there
            int countWithChildren = 0;
            UnicodeSet singles = new UnicodeSet();
            for (EntryRange<CPNode> entry : (Iterable<EntryRange<CPNode>>) baseNode) {
                if (entry.value.childless()) {
                    singles.add(entry.codepoint, entry.codepointEnd);
                } else {
                    ++countWithChildren;
                }
            }
            boolean paren = countWithChildren + (singles.isEmpty() ? 0 : 1) > 1;
            if (paren) {
                result.append('(');
            }
            boolean needBar = false;
            if (!singles.isEmpty()) {
                if (singles.size() == 1) {
                    int cp = singles.charAt(0);
                    addCodePoint(result, cp);
                } else {
                    addCodePoint(result, singles);
                }
                --countWithChildren;
                needBar = true;
            }
            if (countWithChildren >= 0) {
                for (EntryRange<CPNode> entry : (Iterable<EntryRange<CPNode>>) baseNode) {
                    CPNode child = entry.value;
                    if (child.childless()) {
                        continue;
                    }
                    if (needBar) {
                        result.append('|');
                    }
                    needBar = true;
                    if (entry.codepoint == entry.codepointEnd) {
                        addCodePoint(result, entry.codepoint);
                    } else {
                        result.append('[');
                        addCodePoint(result, entry.codepoint);
                        result.append('-');
                        addCodePoint(result, entry.codepointEnd);
                        result.append(']');
                    }
                    if (child.hasValue()) {
                        result.append('(');
                    }
                    getRegex(child, result);
                    if (child.hasValue()) {
                        result.append(")?");
                    }
                }
            }
            if (paren) {
                result.append(')');
            }
        }

        static private void addCodePoint(StringBuilder result, UnicodeSet singles) {
            result.append('[');
            for (UnicodeSetIterator entry = new UnicodeSetIterator(singles); entry.nextRange();) {
                addCodePoint(result, entry.codepoint);
                if (entry.codepoint != entry.codepointEnd) {
                    result.append('-');
                    addCodePoint(result, entry.codepointEnd);
                }
            }
            result.append(']');
        }

        static private StringBuilder addCodePoint(StringBuilder result, int cp) {
            switch (cp) {
            case '*' : result.append('\\');
            }
            return result.appendCodePoint(cp);
        }
    }

    public static void main(String[] args) {
        Collection<String> tests = Arrays.asList("a", "xyz", "bc", "bce", "bcd", "p", "q", "r");
        check(tests);
        HashSet<String> tests2 = EmojiData.EMOJI_DATA.getAllEmojiWithoutDefectives().addAllTo(new HashSet<String>());
        LinkedHashSet<String> tests3 = new LinkedHashSet<>();
        LinkedHashSet<String> tests4 = new LinkedHashSet<>();
        tests2.forEach(s -> {
            if (s.startsWith("👯")) tests3.add(s); 
            tests4.add(EmojiData.EMOJI_DATA.addEmojiVariants(s));
        });
        check(tests3);
        check(tests4);
    }

    private static void check(Collection<String> tests) {
        CPNodeBuilder x = new CPNodeBuilder().addAll(tests, 1);
        System.out.println(x.toString());
        CPNode s = new CPNodeBuilder().addAll(tests, 1).build();
        System.out.println(s.toString());
        String pattern = RegexBuilder.getRegex(s);
        System.out.println(pattern);
        Pattern p = Pattern.compile(pattern);
        UnicodeSet failures = new UnicodeSet();
        for (String test : tests) {
            if (!p.matcher(test).matches()) {
                failures.add(test);
            }
        }
        if (failures.size() > 0) {
            System.out.println("Fails: " + SHOW.transform(failures.toPattern(false)));
        }
    }
}
