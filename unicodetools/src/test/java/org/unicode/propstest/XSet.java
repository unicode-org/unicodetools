package org.unicode.propstest;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.util.Freezable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public class XSet<U extends Comparable<U>> implements Freezable<XSet<U>> {
    private boolean pos = true;
    private final TreeSet<U> set = new TreeSet<U>();
    boolean frozen;

    public XSet() {}

    public static <U extends Comparable<U>> XSet<U> of(XSet<U> a) {
        return new XSet<U>().set(a);
    }

    public XSet<U> set(XSet<U> a) {
        set.clear();
        set.addAll(a.set);
        pos = a.pos;
        return this;
    }

    public void clear() {
        set.clear();
        pos = true;
    }

    public XSet<U> negate() {
        pos = !pos;
        return this;
    }

    public int size() {
        return pos ? set.size() : Integer.MAX_VALUE;
    }

    public boolean contains(U o) {
        return pos ? set.contains(o) : !set.contains(o);
    }

    public XSet<U> add(U e) {
        boolean altered = pos ? set.add(e) : set.remove(e);
        return this;
    }

    public XSet<U> remove(U e) {
        boolean altered = pos ? set.remove(e) : set.add(e);
        return this;
    }

    public boolean containsAll(XSet<U> c) {
        if (pos == c.pos) {
            if (pos) { // pos && c.pos
                return set.containsAll(c.set);
            } else { // !pos && !c.pos
                return c.set.containsAll(set);
            }
        } else {
            if (pos) { // pos && !c.pos
                return false;
            } else { // !pos && c.pos
                return Collections.disjoint(set, c.set);
            }
        }
    }

    public boolean containsNone(XSet<U> c) {
        if (pos == c.pos) {
            if (pos) { // pos && c.pos
                return Collections.disjoint(set, c.set);
            } else { // !pos && !c.pos
                return false;
            }
        } else {
            if (pos) { // pos && !c.pos
                return c.set.containsAll(set);
            } else { // !pos && c.pos
                return set.containsAll(c.set);
            }
        }
    }

    public XSet<U> addAll(XSet<U> c) {
        if (frozen) {
            throw new UnsupportedOperationException();
        }
        if (pos == c.pos) {
            if (pos) { // pos && c.pos
                set.addAll(c.set);
            } else { // !pos && !c.pos
                set.retainAll(c.set);
            }
        } else {
            if (pos) { // pos && !c.pos
                otherRemoveAll(set, c.set);
                pos = false;
            } else { // !pos && c.pos
                set.removeAll(c.set);
            }
        }
        return this;
    }

    public XSet<U> retainAll(XSet<U> c) {
        if (frozen) {
            throw new UnsupportedOperationException();
        }
        if (pos == c.pos) {
            if (pos) { // pos && c.pos
                set.retainAll(c.set);
            } else { // !pos && !c.pos
                set.addAll(c.set);
            }
        } else {
            if (pos) { // pos && !c.pos
                set.removeAll(c.set);
            } else { // !pos && c.pos
                otherRemoveAll(set, c.set);
                pos = true;
            }
        }
        return this;
    }

    public XSet<U> removeAll(XSet<U> c) {
        if (frozen) {
            throw new UnsupportedOperationException();
        }
        if (pos == c.pos) {
            if (pos) { // pos && c.pos
                set.removeAll(c.set);
            } else { // !pos && !c.pos
                otherRemoveAll(set, c.set);
                pos = true;
            }
        } else {
            if (pos) { // pos && !c.pos
                set.retainAll(c.set);
            } else { // !pos && c.pos
                set.addAll(c.set);
            }
        }
        return this;
    }

    private static <U> void otherRemoveAll(Set<U> set, Set<U> other) {
        HashSet<U> temp = new HashSet<>(other);
        temp.removeAll(set);
        set.clear();
        set.addAll(temp);
    }

    public String toString() {
        return "[" + (pos ? "" : "^") + CollectionUtilities.join(set, " ") + "]";
    }

    @Override
    public boolean isFrozen() {
        return frozen;
    }

    @Override
    public XSet<U> freeze() {
        frozen = true;
        return this;
    }

    @Override
    public XSet<U> cloneAsThawed() {
        return new XSet<U>().set(this);
    }

    public boolean isPositive() {
        return pos;
    }

    @Override
    public boolean equals(Object obj) {
        XSet<U> other = (XSet<U>) obj;
        return pos == other.pos && set.equals(other.set);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pos, set);
    }
}
