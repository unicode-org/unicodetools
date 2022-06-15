/**
 * ****************************************************************************** Copyright (C)
 * 1996-2001, International Business Machines Corporation and * others. All Rights Reserved. *
 * ******************************************************************************
 *
 * <p>$Source: /home/cvsroot/unicodetools/org/unicode/text/utility/Pair.java,v $
 *
 * <p>******************************************************************************
 */
package org.unicode.text.utility;

public final class Pair implements java.lang.Comparable, Cloneable {

    public Comparable first, second;

    public Pair(Comparable first, Comparable second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public int hashCode() {
        return first.hashCode() * 37 + second.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        try {
            final Pair that = (Pair) other;
            return first.equals(that.first) && second.equals(that.second);
        } catch (final Exception e) {
            return false;
        }
    }

    @Override
    public int compareTo(Object other) {
        final Pair that = (Pair) other;
        final int trial = first.compareTo(that.first);
        if (trial != 0) {
            return trial;
        }
        return second.compareTo(that.second);
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (final CloneNotSupportedException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return '('
                + (first == null ? "null" : first.toString())
                + ','
                + (second == null ? "null" : second.toString())
                + ')';
    }
}
