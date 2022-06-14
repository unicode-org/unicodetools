/**
 * ****************************************************************************** Copyright (C)
 * 1996-2001, International Business Machines Corporation and * others. All Rights Reserved. *
 * ******************************************************************************
 *
 * <p>$Source: /home/cvsroot/unicodetools/org/unicode/text/utility/OldEquivalenceClass.java,v $
 *
 * <p>******************************************************************************
 */
package org.unicode.text.utility;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class OldEquivalenceClass {
    static final boolean DEBUG = false;
    /**
     * Takes a many:many relation between source and value. Produces equivalence class. Two sources
     * are in the same equivalence class any time they share the same value.
     */
    // associated with each value, we keep a set of sources.
    // whenever we add a <source, value> pair, we see if any sets collide.
    // associated with each set of sources, we keep a representative Whenever we add to the set, if
    // we
    //
    Map sourceToEquiv = new TreeMap();

    Map valueToRepresentativeSource = new HashMap();
    Map forcedMerge = new HashMap();
    /**
     * @return true if made a difference
     */
    String itemSeparator;

    int places;
    boolean hex;

    public OldEquivalenceClass() {
        this(",", 4, true);
    }

    public OldEquivalenceClass(String itemSeparator, int places, boolean hex) {
        this.itemSeparator = itemSeparator;
        this.places = places;
        this.hex = hex;
    }

    public boolean add(Object source, Object value) {
        boolean result = false;
        Object repSource = valueToRepresentativeSource.get(value);
        Set equivSet = (Set) sourceToEquiv.get(source);
        Set fm = (Set) forcedMerge.get(source);
        if (fm == null) {
            fm = new TreeSet();
            forcedMerge.put(source, fm);
        }

        if (DEBUG) {
            System.out.println("+Source " + source + ", value: " + value);
        }
        if (repSource == null && equivSet == null) {
            equivSet = new TreeSet();
            equivSet.add(source);
            sourceToEquiv.put(source, equivSet);
            valueToRepresentativeSource.put(value, source);
            repSource = source; // for debugging
        } else if (equivSet == null) {
            equivSet = (Set) sourceToEquiv.get(repSource);
            equivSet.add(source);
            sourceToEquiv.put(source, equivSet);
            result = true;
        } else if (repSource == null) {
            valueToRepresentativeSource.put(value, source);
            repSource = source; // for debugging;
        } else { // both non-null
            final Set repEquiv = (Set) sourceToEquiv.get(repSource);
            if (!repEquiv.equals(equivSet)) {

                result = true;
                if (DEBUG) {
                    System.out.println(
                            "Merging ("
                                    + repSource
                                    + ") "
                                    + toString(repEquiv)
                                    + " + ("
                                    + source
                                    + ") "
                                    + toString(equivSet));
                }
                // merge!!
                // put all items from equivSet into repEquiv
                repEquiv.addAll(equivSet);

                // now add the values to the forced sets
                Iterator it = repEquiv.iterator();
                while (it.hasNext()) {
                    final Object n = it.next();
                    fm = (Set) forcedMerge.get(n);
                    fm.add(value);
                }

                // then replace all instances for equivSet by repEquiv
                // we have to do this in two steps, since iterators are invalidated by changes
                final Set toReplace = new TreeSet();
                it = sourceToEquiv.keySet().iterator();
                while (it.hasNext()) {
                    final Object otherSource = it.next();
                    final Set otherSet = (Set) sourceToEquiv.get(otherSource);
                    if (otherSet == equivSet) {
                        toReplace.add(otherSource);
                    }
                }
                it = toReplace.iterator();
                while (it.hasNext()) {
                    final Object otherSource = it.next();
                    sourceToEquiv.put(otherSource, repEquiv);
                }
                equivSet = repEquiv; // for debugging
            }
        }
        if (DEBUG) {
            System.out.println("--- repSource: " + repSource + ", equivSet: " + equivSet);
        }
        return result;
    }

    @Override
    public String toString() {
        final StringBuffer result = new StringBuffer();
        // make a set to skip duplicates
        final Iterator it = new HashSet(sourceToEquiv.values()).iterator();
        while (it.hasNext()) {
            toString((Set) it.next(), result, forcedMerge);
        }
        return result.toString();
    }

    private class MyIterator implements Iterator {
        Iterator it = sourceToEquiv.keySet().iterator();

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public Object next() {
            return sourceToEquiv.get(it.next());
        }

        @Override
        public void remove() {
            throw new IllegalArgumentException("can't remove");
        }
    }

    public Iterator getSetIterator() {
        return new MyIterator();
    }

    private String toString(Object s) {
        if (s == null) {
            return "null";
        }
        if (s instanceof Collection) {
            final StringBuffer sb = new StringBuffer();
            toString((Collection) s, sb, null);
            return sb.toString();
        }
        if (hex && s instanceof Number) {
            return Utility.hex(s, places);
        }
        return s.toString();
    }

    private void toString(Collection s, StringBuffer sb, Map valueToRep) {
        if (sb.length() != 0) {
            sb.append(itemSeparator);
        }
        if (s == null) {
            sb.append("{}");
            return;
        }
        sb.append('{');
        final Iterator it = s.iterator();
        boolean notFirst = false;
        while (it.hasNext()) {
            if (notFirst) {
                sb.append(", ");
            }
            notFirst = true;
            final Object n = it.next();
            sb.append(toString(n));
            /*if (valueToRep != null) {
                sb.append("(" + toString(valueToRep.get(n)) + ")");
            }*/
        }
        sb.append('}');
    }
}
