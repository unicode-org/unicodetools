package org.unicode.text.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import org.unicode.cldr.util.XEquivalenceMap;
import org.unicode.tools.emoji.EmojiData;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeMap.EntryRange;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;


public class StringTree {
    static final Transliterator SHOW = Transliterator.createFromRules("foo", "([[:c:][:z:][:di:][:M:]-[\\ \\x0A]]) > &hex/perl($1);", Transliterator.FORWARD);

    public abstract static class CPNode<T extends CPNode<T>> implements Iterable<EntryRange<T>> {
        public static final int NO_VALUE = Integer.MIN_VALUE;
        /** Not complete comparator !! **/
        public static final Comparator<CPNode> DEPTH_FIRST = new Comparator<CPNode>() {
            @Override
            public int compare(CPNode o1, CPNode o2) {
                return o1.depth() - o2.depth();
            }
        };

        abstract public int getValue();
        public boolean hasValue() {
            return getValue() != NO_VALUE;
        }
        abstract public boolean childless();
        abstract public int childCount();
        abstract public Iterator<EntryRange<T>> iterator();
        abstract public Set<T> values();
        abstract public UnicodeSet getSet(T value);
        abstract public T get(int cp);
        abstract public int depth();

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
        public abstract UnicodeSet keySet();
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
            // slow path for now
            UnicodeSet set1 = o1.keySet();
            UnicodeSet set2 = o2.keySet();
            if (!set1.equals(set2)) {
                return false;
            }
            for (UnicodeSetIterator range1 = new UnicodeSetIterator(set1); range1.next();) {
                CPNode value1 = o1.get(range1.codepoint);
                for (int cp = range1.codepoint; cp <= range1.codepointEnd; ++cp) {
                    if (!equal(value1, o2.get(cp))) {
                        return false;
                    }
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
        public UnicodeSet getSet(ImmutableCPNode value) {
            return data.getSet(value);
        }

        @Override
        public int depth() {
            if (childless()) {
                return 1;
            }
            int childMax = 0;
            for (ImmutableCPNode value : values()) {
                int curDepth = value.depth();
                if (childMax < curDepth) {
                    childMax = curDepth;
                }
            }
            return 1 + childMax;
        }

        @Override
        public UnicodeSet keySet() {
            return data.keySet();
        }
        @Override
        public ImmutableCPNode get(int cp) {
            return data.get(cp);
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
        public UnicodeSet getSet(CPNodeBuilder value) {
            return data.getSet(value);
        }

        @Override
        public int depth() {
            if (childless()) {
                return 1;
            }
            int childMax = 0;
            for (CPNodeBuilder value : values()) {
                int curDepth = value.depth();
                if (childMax < curDepth) {
                    childMax = curDepth;
                }
            }
            return 1 + childMax;
        }
        @Override
        public UnicodeSet keySet() {
            return data.keySet();
        }
        @Override
        public CPNodeBuilder get(int cp) {
            return data.get(cp);
        }
    }

    // TODO reorder to be longest
    static class RegexBuilder {

        public static <T extends CPNode<?>> String getRegex(T baseNode) {
            StringBuilder result = new StringBuilder();
            getRegex(baseNode, result);
            return result.toString();
        }

        private static void getRegex(CPNode baseNode, StringBuilder result) {
            // find out what's there
            // we have to put longer items first, because otherwise regex | won't find them
            int countWithChildren = 0;
            UnicodeSet singles = new UnicodeSet();
            ArrayList<CPNode> sorted = new ArrayList<>(baseNode.values());
            sorted.sort(CPNode.DEPTH_FIRST);
            for (CPNode value : sorted) {
                if (value.childless()) {
                    singles.addAll(baseNode.getSet(value));
                } else {
                    ++countWithChildren;
                }
            }
            boolean paren = countWithChildren + (singles.isEmpty() ? 0 : 1) > 1;
            if (paren) {
                result.append('(');
            }
            boolean needBar = false;
            if (countWithChildren > 0) {
                for (CPNode child : sorted) {
                    if (child.childless()) {
                        continue;
                    }
                    if (needBar) {
                        result.append('|');
                    }
                    needBar = true;
                    UnicodeSet set = baseNode.getSet(child);
                    addCodePoint(result, set);
                    int childValues = child.values().size();
                    if (childValues > 1 || child.hasValue()) {
                        result.append('(');
                    }
                    getRegex(child, result);
                    if (childValues > 1 || child.hasValue()) {
                        result.append(')');
                    }
                    if (child.hasValue()) {
                        result.append("?+"); // possessive quantifier, no backup
                    }
                }
            }
            // put singles after everything else.
            if (!singles.isEmpty()) {
                if (needBar) {
                    result.append('|');
                }
                if (singles.size() == 1) {
                    int cp = singles.charAt(0);
                    addCodePoint(result, cp);
                } else {
                    addCodePoint(result, singles);
                }
                needBar = true;
            }
            if (paren) {
                result.append(')');
            }
        }

        static private void addCodePoint(StringBuilder result, UnicodeSet singles) {
            if (singles.size() == 1) {
                addCodePoint(result, singles.charAt(0));
                return;
            }
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
            case '*' : 
            case '#' :
            case '|' :
            case '\\' : 
                result.append('\\');
                break;
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
        UnicodeSet starters = new UnicodeSet("[🏴]"); // #
        //UnicodeSet starters = new UnicodeSet("[#*0-9©®‼⁉☝⛹✌-✍🕴⛹🖐👻👽✊-✋🎅🏂]");
        tests2.forEach(s -> {
            String fixed = EmojiData.EMOJI_DATA.addEmojiVariants(s);
            if (starters.matchesAt(fixed, 0) >= 0) {
                tests3.add(fixed); 
            }
            tests4.add(fixed);
        });
        check(tests3);
        ImmutableCPNode full = check(tests4);
        partition(full);
    }

    private static ImmutableCPNode check(Collection<String> tests) {
        CPNodeBuilder x = new CPNodeBuilder().addAll(tests, 1);
        ImmutableCPNode s = new CPNodeBuilder().addAll(tests, 1).build();
        System.out.println(s.toString());
        if (!CpWrapper.equal(x, s)) {
            CpWrapper.equal(x, s);
            System.out.println("Immutable fails:\t" + SHOW.transform(x.toString()));
            return null;
        }
        String pattern = RegexBuilder.getRegex(s);
        System.out.println("Regex:\t" + SHOW.transform(pattern));
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
        return s;
    }

    static void partition(ImmutableCPNode s) {
        System.out.println("Partition: ");
        UnicodeMap<String> ri = new UnicodeMap()
                .putAll(new UnicodeSet("[:regional_indicator:]"), "RI")
                .putAll(new UnicodeSet("[:block=tags:]"), "TAG")
                .freeze();
        Object fake = new Object();
        Multimap<Integer, Object> basePartition = LinkedHashMultimap.create();
        addPartitions(s, ri, fake, basePartition);
        XEquivalenceMap<Integer, Set<Object>, String> partition = new XEquivalenceMap<>();
        for ( Entry<Integer, Collection<Object>> entry : basePartition.asMap().entrySet()) {
            partition.add(entry.getKey(), (Set<Object>) entry.getValue());
        }

        UnicodeSet key = new UnicodeSet();
        int count = 0;
        for (Set<Integer> entry : partition) {
            key.clear();
            Set<Object> targets = null;
            for (Integer item : entry) {
                if (targets == null) {
                    targets = partition.getTarget(item);
                }
                key.add(item);
            }
            Set<String> targetSet = new HashSet<>();
            for (Object target : targets) {
                String targetString;
                if (target instanceof ImmutableCPNode) {
                    String pattern = RegexBuilder.getRegex((ImmutableCPNode)target);
                    targetString = SHOW.transform(pattern);
                } else {
                    targetString = target.toString();
                }
                targetSet.add(targetString);
            }

            System.out.println(key.size() 
                    + "\t" + targetSet.size()
                    + "\t" + SHOW.transform(key.toPattern(false)) 
                    + "\t" + targetSet
                    );
        }
    }

    private static void addPartitions(ImmutableCPNode s, UnicodeMap<String> ri, Object fake, Multimap<Integer, Object> basePartition) {
        for (EntryRange<ImmutableCPNode> entry : s) {
            for (int cp = entry.codepoint; cp <= entry.codepointEnd; ++cp) {
                String special = ri.get(cp);
                if (special != null) {
                    basePartition.put(cp, special);
                } else if (entry.value.childless()){
                    basePartition.put(cp, "TERM");
                } else {
                    basePartition.put(cp, entry.value);
                    addPartitions(entry.value, ri, fake, basePartition);
                }
            }
        }
    }
}
