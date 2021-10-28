package org.unicode.jsp;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * Convenience class for building collections and maps. Allows them to be built by chaining, making it simpler to
 * set as parameters and fields. Also supplies some operations that are missing on the JDK maps and collections,
 * and provides finer control for what happens with equal elements.
 * <pre>
 * Operations: A is current contents, B is new collection, x indicates the results
 * A-B   A&B    B-A   Name
 *                    clear()
 * x                  removeAll(B)
 *        x           retainAll(B) -- option 1: keep A, option 2: substitute B
 *               x    keepNew(B)
 * x      x           <no operation>
 *        x      x    clear().addAll(B)
 * x             x    xor(B)
 * x      x      x    addAll(B)
 * </pre>
 * @author markdavis
 */
// TODO add other Iterable<? extends E>

public final class Builder {
    public enum EqualAction {NATIVE, REPLACE, RETAIN, THROW}

    public static <E, C extends Collection<E>> CBuilder<E,C> with(C collection, EqualAction ea) {
        return new CBuilder<E,C>(collection, ea);
    }

    public static <E, C extends Collection<E>> CBuilder<E,C> with(C collection) {
        return new CBuilder<E,C>(collection, EqualAction.NATIVE);
    }

    public static <K, V, M extends Map<K,V>> MBuilder<K,V,M> with(M map, EqualAction ea) {
        return new MBuilder<K,V,M>(map, ea);
    }

    public static <K, V, M extends Map<K,V>> MBuilder<K,V,M> with(M map) {
        return new MBuilder<K,V,M>(map, EqualAction.NATIVE);
    }

    // ===== Collections ======

    public static final class CBuilder<E, U extends Collection<E>> {
        public EqualAction getEqualAction() {
            return equalAction;
        }
        public CBuilder<E, U> setEqualAction(EqualAction equalAction) {
            this.equalAction = equalAction;
            return this;
        }

        public CBuilder<E, U> clear() {
            collection.clear();
            return this;
        }

        public CBuilder<E,U> add(E e) {
            switch (equalAction) {
            case NATIVE:
                break;
            case REPLACE:
                collection.remove(e);
                break;
            case RETAIN:
                if (collection.contains(e)) {
                    return this;
                }
                break;
            case THROW:
                if (collection.contains(e)) {
                    throw new IllegalArgumentException("Map already contains " + e);
                }
            }
            collection.add(e);
            return this;
        }

        public CBuilder<E,U> addAll(Collection<? extends E> c) {
            if (equalAction == EqualAction.REPLACE) {
                collection.addAll(c);
            } else {
                for (final E item : c) {
                    add(item);
                }
            }
            return this;
        }

        public CBuilder<E, U> addAll(E... items) {
            for (final E item : items) {
                collection.add(item);
            }
            return this;
        }

        public CBuilder<E, U> addAll(Iterable<? extends E> items) {
            for (final E item : items) {
                collection.add(item);
            }
            return this;
        }

        public CBuilder<E, U> remove(E o) {
            collection.remove(o);
            return this;
        }

        public CBuilder<E,U> removeAll(Collection<? extends E> c) {
            collection.removeAll(c);
            return this;
        }

        public CBuilder<E,U> removeAll(E... items) {
            for (final E item : items) {
                collection.remove(item);
            }
            return this;
        }

        public CBuilder<E,U> removeAll(Iterable<? extends E> items) {
            for (final E item : items) {
                collection.remove(item);
            }
            return this;
        }

        public CBuilder<E,U> retainAll(Collection<? extends E> c) {
            collection.retainAll(c);
            return this;
        }

        public CBuilder<E,U> retainAll(E... items) {
            collection.retainAll(Arrays.asList(items));
            return this;
        }

        public CBuilder<E,U> xor(Collection<? extends E> c) {
            for (final E item : c) {
                final boolean changed = collection.remove(item);
                if (!changed) {
                    collection.add(item);
                }
            }
            return this;
        }

        public CBuilder<E,U> xor(E... items) {
            return xor(Arrays.asList(items));
        }

        public CBuilder<E,U> keepNew(Collection<? extends E> c) {
            final HashSet<E> extras = new HashSet<E>(c);
            extras.removeAll(collection);
            collection.clear();
            collection.addAll(extras);
            return this;
        }

        public CBuilder<E,U> keepNew(E... items) {
            return keepNew(Arrays.asList(items));
        }

        public U get() {
            final U temp = collection;
            collection = null;
            return temp;
        }

        @SuppressWarnings("unchecked")
        public U freeze() {
            U temp;
            if (collection instanceof SortedSet) {
                temp = (U)Collections.unmodifiableSortedSet((SortedSet<E>) collection);
            } else if (collection instanceof Set) {
                temp = (U)Collections.unmodifiableSet((Set<E>) collection);
            } else if (collection instanceof List) {
                temp = (U)Collections.unmodifiableList((List<E>) collection);
            } else {
                temp = (U)Collections.unmodifiableCollection(collection);
            }
            collection = null;
            return temp;
        }

        @Override
        public String toString() {
            return collection.toString();
        }

        // ===== PRIVATES ======

        private CBuilder(U set2, EqualAction ea) {
            this.collection = set2;
            equalAction = ea;
        }
        private U collection;
        private EqualAction equalAction;
    }

    // ===== Maps ======

    public static final class MBuilder<K, V, M extends Map<K,V>> {

        public EqualAction getEqualAction() {
            return equalAction;
        }
        public MBuilder<K, V, M> setEqualAction(EqualAction equalAction) {
            this.equalAction = equalAction;
            return this;
        }

        public MBuilder<K, V, M> clear() {
            map.clear();
            return this;
        }
        public MBuilder<K, V, M> put(K key, V value) {
            switch (equalAction) {
            case NATIVE:
                break;
            case REPLACE:
                map.remove(key);
                break;
            case RETAIN:
                if (map.containsKey(key)) {
                    return this;
                }
                break;
            case THROW:
                if (map.containsKey(key)) {
                    throw new IllegalArgumentException("Map already contains " + key);
                }
            }
            map.put(key, value);
            return this;
        }

        public MBuilder<K, V, M> on(K... keys) {
            this.keys = Arrays.asList(keys);
            return this;
        }

        public MBuilder<K, V, M> on(Collection<? extends K> keys) {
            this.keys = keys;
            return this;
        }

        public MBuilder<K, V, M> put(V value) {
            for (final K key : keys) {
                put(key, value);
            }
            keys = null;
            return this;
        }

        public MBuilder<K, V, M> put(V... values) {
            int v = 0;
            for (final K key : keys) {
                put(key, values[v++]);
                if (v >= values.length) {
                    v = 0;
                }
            }
            keys = null;
            return this;
        }

        public MBuilder<K, V, M> put(Collection<? extends V> values) {
            Iterator<? extends V> vi = null;
            for (final K key : keys) {
                if (vi == null || !vi.hasNext()) {
                    vi = values.iterator();
                }
                put(key, vi.next());
            }
            return this;
        }

        public MBuilder<K, V, M> putAll(Map<? extends K, ? extends V> m) {
            if (equalAction == EqualAction.NATIVE) {
                map.putAll(m);
            } else {
                for (final K key : m.keySet()) {
                    put(key, m.get(key));
                }
            }
            keys = null;
            return this;
        }

        public MBuilder<K, V, M> putAll(Object[][] data) {
            for (final Object[] key : data) {
                put((K)key[0], (V)key[1]);
            }
            keys = null;
            return this;
        }

        public MBuilder<K, V, M> remove(K key) {
            map.remove(key);
            return this;
        }

        public MBuilder<K, V, M> removeAll(Collection<? extends K> keys) {
            map.keySet().removeAll(keys);
            return this;
        }
        public MBuilder<K, V, M> removeAll(K... keys) {
            return removeAll(Arrays.asList(keys));
        }

        public MBuilder<K, V, M> retainAll(Collection<? extends K> keys) {
            map.keySet().retainAll(keys);
            return this;
        }
        public MBuilder<K, V, M> retainAll(K... keys) {
            return retainAll(Arrays.asList(keys));
        }

        public <N extends Map<K,V>> MBuilder<K, V, M> xor(N c) {
            for (final K item : c.keySet()) {
                if (map.containsKey(item)) {
                    map.remove(item);
                } else {
                    put(item, c.get(item));
                }
            }
            return this;
        }

        public <N extends Map<K,V>> MBuilder<K, V, M> keepNew(N c) {
            final HashSet<K> extras = new HashSet<K>(c.keySet());
            extras.removeAll(map.keySet());
            map.clear();
            for (final K key : extras) {
                map.put(key, c.get(key));
            }
            return this;
        }

        public M get() {
            final M temp = map;
            map = null;
            return temp;
        }

        @SuppressWarnings("unchecked")
        public M freeze() {
            M temp;
            if (map instanceof SortedMap<?,?>) {
                temp = (M)Collections.unmodifiableSortedMap((SortedMap<K,V>) map);
            } else {
                temp = (M)Collections.unmodifiableMap(map);
            }
            map = null;
            return temp;
        }

        @Override
        public String toString() {
            return map.toString();
        }

        // ===== PRIVATES ======

        private Collection<? extends K> keys;
        private M map;
        private EqualAction equalAction;

        private MBuilder(M map, EqualAction ea) {
            this.map = map;
            equalAction = ea;
        }
    }
}
