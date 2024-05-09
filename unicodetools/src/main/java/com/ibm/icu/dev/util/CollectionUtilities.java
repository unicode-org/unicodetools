// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1996-2015, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.util;

// This file was migrated from the ICU4J repo,
// path icu4j/main/framework/src/test/java/com/ibm/icu/dev/util/CollectionUtilities.java

import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Utilities that ought to be on collections, but aren't
 *
 * @internal CLDR
 */
public final class CollectionUtilities {

    /**
     * Join an array of items.
     *
     * @param <T>
     * @param array
     * @param separator
     * @return string
     */
    public static <T> String join(T[] array, String separator) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < array.length; ++i) {
            if (i != 0) result.append(separator);
            result.append(array[i]);
        }
        return result.toString();
    }

    /**
     * Join a collection of items.
     *
     * @param <T>
     * @param collection
     * @param <U>
     * @param array
     * @param separator
     * @return string
     */
    public static <T, U extends Iterable<T>> String join(U collection, String separator) {
        StringBuffer result = new StringBuffer();
        boolean first = true;
        for (Iterator<T> it = collection.iterator(); it.hasNext(); ) {
            if (first) first = false;
            else result.append(separator);
            result.append(it.next());
        }
        return result.toString();
    }

    /**
     * Utility like Arrays.asList()
     *
     * @param source
     * @param target
     * @param reverse
     * @param <T>
     * @return
     */
    public static <T> Map<T, T> asMap(T[][] source, Map<T, T> target, boolean reverse) {
        int from = 0, to = 1;
        if (reverse) {
            from = 1;
            to = 0;
        }
        for (int i = 0; i < source.length; ++i) {
            target.put(source[i][from], source[i][to]);
        }
        return target;
    }

    /**
     * @param <T>
     * @param source
     * @return
     */
    public static <T> Map<T, T> asMap(T[][] source) {
        return asMap(source, new HashMap<T, T>(), false);
    }

    /**
     * Get the "best" in collection. That is the least if direction is < 0, otherwise the greatest.
     * The first is chosen if there are multiples.
     *
     * @param <T>
     * @param <U>
     * @param c
     * @param comp
     * @param direction
     * @return
     */
    public static <T, U extends Collection<T>> T getBest(U c, Comparator<T> comp, int direction) {
        Iterator<T> it = c.iterator();
        if (!it.hasNext()) return null;
        T bestSoFar = it.next();
        if (direction < 0) {
            while (it.hasNext()) {
                T item = it.next();
                int compValue = comp.compare(item, bestSoFar);
                if (compValue < 0) {
                    bestSoFar = item;
                }
            }
        } else {
            while (it.hasNext()) {
                T item = it.next();
                int compValue = comp.compare(item, bestSoFar);
                if (compValue > 0) {
                    bestSoFar = item;
                }
            }
        }
        return bestSoFar;
    }

    public static String remove(String source, UnicodeSet removals) {
        StringBuffer result = new StringBuffer();
        int cp;
        for (int i = 0; i < source.length(); i += UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(source, i);
            if (!removals.contains(cp)) UTF16.append(result, cp);
        }
        return result.toString();
    }

    /**
     * Compare, allowing nulls and putting them first
     *
     * @param a
     * @param b
     * @return
     */
    public static <T extends Comparable<T>> int compare(T a, T b) {
        return a == null ? b == null ? 0 : -1 : b == null ? 1 : a.compareTo(b);
    }

    /**
     * Compare iterators
     *
     * @param iterator1
     * @param iterator2
     * @return
     */
    public static <T extends Comparable<T>> int compare(
            Iterator<T> iterator1, Iterator<T> iterator2) {
        int diff;
        while (true) {
            if (!iterator1.hasNext()) {
                return iterator2.hasNext() ? -1 : 0;
            } else if (!iterator2.hasNext()) {
                return 1;
            }
            diff = CollectionUtilities.compare(iterator1.next(), iterator2.next());
            if (diff != 0) {
                return diff;
            }
        }
    }

    /**
     * Compare, with shortest first, and otherwise lexicographically
     *
     * @param a
     * @param b
     * @return
     */
    public static <T extends Comparable<T>, U extends Collection<T>> int compare(U o1, U o2) {
        int diff = o1.size() - o2.size();
        if (diff != 0) {
            return diff;
        }
        Iterator<T> iterator1 = o1.iterator();
        Iterator<T> iterator2 = o2.iterator();
        return compare(iterator1, iterator2);
    }

    public static class SetComparator<T extends Comparable<T>> implements Comparator<Set<T>> {
        public int compare(Set<T> o1, Set<T> o2) {
            return CollectionUtilities.compare(o1, o2);
        }
    }

    public static class CollectionComparator<T extends Comparable<T>>
            implements Comparator<Collection<T>> {
        public int compare(Collection<T> o1, Collection<T> o2) {
            return CollectionUtilities.compare(o1, o2);
        }
    }

    /**
     * Compare, allowing nulls and putting them first
     *
     * @param a
     * @param b
     * @return
     */
    public static <K extends Comparable<K>, V extends Comparable<V>, T extends Entry<K, V>>
            int compare(T a, T b) {
        if (a == null) {
            return b == null ? 0 : -1;
        } else if (b == null) {
            return 1;
        }
        int diff = compare(a.getKey(), b.getKey());
        if (diff != 0) {
            return diff;
        }
        return compare(a.getValue(), b.getValue());
    }
}
