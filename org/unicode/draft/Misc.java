package org.unicode.draft;
import java.lang.reflect.Method;
import java.text.Collator;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.management.RuntimeErrorException;

import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;


public class Misc {

    // there are 8 possible different relations between two sets A and B,
    // based whether A-B, A&B, B-A are each empty or not
    //      condition     how to set A to
    // nnn: A = 0 = B     A.clear()
    // YYY: A ⫘ B         A.addAll(B)

    // nnY: A = 0 ≠ B     A = B.removeAll(A)
    // Ynn: B = 0 ≠ A     A.removeAll(B)

    // nYY: B ⊃ A ≠ 0     A.set(B) := A.clear(); A.addAll(B);
    // YYn: A ⊃ B ≠ 0     A

    // nYn: A = B ≠ 0     A.retainAll(B)
    // YnY: 0 ≠ B ∥ A ≠ 0 A.symmetricDiff(B) := C = B; C.removeAll(A); A.removeAll(B); A.addAll(C)

    // Java appears to always favor the source in intersections.
    // That is, for A.retainAll(B), A.addAll(B), A's version wins.
    // There are 4 other possible versions: retain/addAll with override, and replaceAll

    static Collator col = Collator.getInstance();
    static {
        col.setStrength(Collator.PRIMARY);
    }



    public static boolean isInterchangeValid(CharSequence s) {
        int length = s.length();
        int index = 0;
        char c1, c2;
        while (index < length) {
            int cp = !((c1 = s.charAt(index++)) >= Character.MIN_HIGH_SURROGATE 
                    && c1 <= Character.MAX_HIGH_SURROGATE) 
                    ? c1
                            : index < s.length() 
                            && ((c2 = s.charAt(index)) >= Character.MIN_LOW_SURROGATE 
                                    && c2 <= Character.MAX_LOW_SURROGATE) 
                                    ? Character.toCodePoint(c1, c2)
                                            : c1;
                                    if (!isInterchangeValidCodePoint(cp)) {
                                        return false;
                                    }
                                    index += cp >= Character.MIN_SUPPLEMENTARY_CODE_POINT? 2 : 1;
        }
        return true;
    }

    public static int codePointAt(CharSequence s, int index) {
        char c1, c2;
        return !Character.isHighSurrogate(c1 = s.charAt(index++)) ? c1
                : index < s.length() && Character.isLowSurrogate(c2 = s.charAt(index)) ? Character.toCodePoint(c1, c2)
                        : c1;
    }

    private static boolean isInterchangeValidCodePoint(int cp) {
        // TODO Auto-generated method stub
        return false;
    }

    private static boolean isSurrogate(int codePoint) {
        return codePoint >= Character.MIN_SURROGATE
        && codePoint <= Character.MAX_SURROGATE;
    }

    public static void main(String[] args) throws ParseException {
        new Misc().compareCalendars();

        if (true) return;
        new Misc().testDateParsing();

        Map<String,Integer> am;
        Map<String,Integer> cm;
        TreeSet<String> as;
        TreeSet<String> bs;

        as = CollectionBuilder.of(new TreeSet<String>(col)).add("a").add("A", "B").unmodifiable();
        bs = CollectionBuilder.of(new TreeSet<String>(col)).add("b").add("c").unmodifiable();

        Set<String> d;
        d = clone(as);
        d.retainAll(bs);
        System.out.println("retainAll:\t" + d);
        d = clone(as);
        d.removeAll(bs);
        System.out.println("removeAll:\t" + d);
        d = clone(as);
        d.addAll(bs);
        System.out.println("addAll:\t" + d);

        am = MapBuilder.of(new TreeMap<String,Integer>(col)).put("A", 1).put("B", 2).unmodifiable();
        cm = MapBuilder.of(new TreeMap<String,Integer>(col)).put("b", 3).put("c", 4).unmodifiable();
        addAll(am, cm, true);
        System.out.println(am);
    }

    @SuppressWarnings("unchecked")
    public static <T> T clone(T source) {
        try {
            Method method = source.getClass().getMethod("clone", (Class[]) null);
            return (T) method.invoke(source, (Object[]) null);
        } catch (Exception e) {
            throw (RuntimeException) new UnsupportedOperationException().initCause(e);
        }
    }

    static <T,U> Map<T,U> addAll(Map<T,U> toChange, Map<T,U> other, boolean override) {
        for (T key : other.keySet()) {
            if (!override && toChange.containsKey(key)) {
                continue;
            }
            toChange.put(key, other.get(key));
        }
        return toChange;
    }

    static <T,U> Map<T,U> removeAll(Map<T,U> toChange, Map<T,U> other) {
        for (T key : other.keySet()) {
            toChange.remove(key);
        }
        return toChange;
    }

    // change only the cases where A has a mapping
    static <T,U> Map<T,U> replaceAll(Map<T,U> toChange, Map<T,U> other) {
        for (T key : other.keySet()) {
            if (toChange.containsKey(key)) {
                toChange.put(key, other.get(key));
            }
        }
        return toChange;
    }


    static <T,U> Map<T,U> retainAll(Map<T,U> toChange, Map<T,U> other, boolean override) {
        Iterator<T> it = toChange.keySet().iterator();
        while (it.hasNext()) {
            T key = it.next();
            if (other.containsKey(key)) {
                it.remove();
            }
        }
        if (override) {
            return replaceAll(toChange, other);
        }
        return toChange;
    }

    static class MapBuilder<K,V,M extends Map<K,V>> {
        private M map;

        public static <K,V,M extends Map<K,V>> MapBuilder<K,V,M> of(M map) {
            return new MapBuilder<K,V,M>(map);
        }

        private MapBuilder(M map) {
            this.map = map;
        }
        public MapBuilder<K,V,M> put(K key, V value) {
            map.put(key, value);
            return this;
        }
        public M finish() {
            M result = map;
            map = null;
            return result;
        }

        @SuppressWarnings("unchecked")
        public M unmodifiable() {
            M result = map instanceof SortedMap ? (M) Collections.unmodifiableSortedMap((SortedMap<K,V>) map) :
                (M) Collections.unmodifiableMap((Map<K,V>) map);
            map = null;
            return result;
        }
    }

    public static class CollectionBuilder<E,C extends Collection<E>> {
        private C collection;

        private CollectionBuilder(C collection) {
            this.collection = collection;
        }

        public static <E,C extends Collection<E>> CollectionBuilder<E,C> of(C collection) {
            return new CollectionBuilder<E,C>(collection);
        }

        public C finish() {
            C result = collection;
            collection = null;
            return result;
        }

        @SuppressWarnings("unchecked")
        public C unmodifiable() {
            // ugly, but don't see a way around it
            C result = 
                collection instanceof SortedSet ? (C) Collections.unmodifiableSortedSet((SortedSet<E>)collection) : 
                    collection instanceof Set ? (C) Collections.unmodifiableSet((Set<E>)collection) :
                        collection instanceof List ? (C) Collections.unmodifiableList((List<E>)collection) : 
                            (C) Collections.unmodifiableCollection(collection);
                        collection = null;
                        return result;
        }

        public CollectionBuilder<E,C> add(E... elements) {
            for (E element : elements) {
                collection.add(element);
            }
            return this;
        }

        public CollectionBuilder<E,C> addAll(Iterable<E> iterable) {
            for (E element : iterable) {
                collection.add(element);
            }
            return this;
        }
    }
}
