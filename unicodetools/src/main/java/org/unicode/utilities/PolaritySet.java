package org.unicode.utilities;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

public class PolaritySet<T> {
    public enum Operation {
        NEGATION("¬"),
        UNION("∪"),
        INTERSECT("∩"),
        SUBTRACT("∖"),
        XOR("⊕"),
        ;
        public final String display;

        private Operation(String display) {
            this.display = display;
        }

        public static Operation fromDisplay(String op) {
            for (Operation item : values()) {
                if (item.display.equals(op)) {
                    return item;
                }
            }
            return null;
        }
    }

    private static final Joiner SPACE_JOINER = Joiner.on(' ');
    private boolean isPositive;
    private Set<T> source;

    public static <T> PolaritySet<T> of() {
        return new PolaritySet<T>(Collections.emptySet(), true);
    }

    public static <T> PolaritySet<T> of(PolaritySet<T> left) {
        return new PolaritySet<T>(left.source, left.isPositive);
    }

    public static <T> PolaritySet<T> of(Set<T> other, boolean isPositive) {
        return new PolaritySet<T>(other, isPositive);
    }

    private PolaritySet(Set<T> source, boolean isPositive) {
        this.source = new LinkedHashSet<T>(source);
        this.isPositive = isPositive;
    }

    public int size() {
        return source.size();
    }

    public boolean isEmpty() {
        return isPositive && source.isEmpty();
    }

    public boolean isFull() {
        return !isPositive && source.isEmpty();
    }

    public boolean contains(Object o) {
        return source.contains(o) == isPositive;
    }

    public boolean add(T e) {
        return isPositive ? source.add(e) : source.remove(e);
    }

    public boolean remove(Object o) {
        return isPositive ? source.remove(o) : source.add((T) o);
    }

    public boolean containsAll(Collection<?> c) {
        return isPositive ? source.containsAll(c) : Collections.disjoint(source, c);
    }

    public boolean containsNone(Collection<?> c) {
        return isPositive ? Collections.disjoint(source, c) : source.containsAll(c);
    }

    // see https://unicode.org/reports/tr18/#Resolving_Character_Ranges_with_Strings

    public boolean addAll(PolaritySet<T> other) {
        if (isPositive && other.isPositive) {
            return source.addAll(other.source); // A ∪ B
        } else if (isPositive && !other.isPositive) {
            isPositive = false;
            return otherRemoveAllMe(other.source); // B ∖ A
        } else if (!isPositive && other.isPositive) {
            return source.removeAll(other.source); // A ∖ B
        } else /* if (!isPositive && !other.isPositive) */ {
            return source.retainAll(other.source); // A ∩ B
        }
    }

    public boolean retainAll(PolaritySet<T> other) {
        if (isPositive && other.isPositive) {
            return source.retainAll(other.source); // A ∩ B
        } else if (isPositive && !other.isPositive) {
            return source.removeAll(other.source); // A ∖ B
        } else if (!isPositive && other.isPositive) {
            isPositive = true;
            return otherRemoveAllMe(other.source); // B ∖ A
        } else /* if (!isPositive && !other.isPositive) */ {
            return source.addAll(other.source); // A ∪ B
        }
    }

    public boolean removeAll(PolaritySet<T> other) {
        if (isPositive && other.isPositive) {
            return source.removeAll(other.source); // A ∖ B
        } else if (isPositive && !other.isPositive) {
            return source.retainAll(other.source); // A ∩ B
        } else if (!isPositive && other.isPositive) {
            return source.addAll(other.source); // A ∪ B
        } else /* if (!isPositive && !other.isPositive) */ {
            isPositive = true;
            return otherRemoveAllMe(other.source); // B ∖ A
        }
    }

    public void retainDifferences(PolaritySet<T> other) {
        PolaritySet<T> temp = PolaritySet.of(other);
        temp.removeAll(this);
        removeAll(other);
        addAll(temp);
    }

    private boolean otherRemoveAllMe(Set<T> other) {
        HashSet<T> temp = new HashSet<>(source);
        source.clear();
        source.addAll(other);
        return source.removeAll(temp);
    }

    public PolaritySet<T> negate() {
        isPositive = !isPositive;
        return this;
    }

    public void clear() {
        source.clear();
        isPositive = true;
    }

    public boolean equals(Object o) {
        PolaritySet<T> other = (PolaritySet<T>) o;
        return source.equals(o) && (isPositive == other.isPositive);
    }

    public int hashCode() {
        return isPositive ? source.hashCode() : source.hashCode() ^ 1;
    }

    @Override
    public String toString() {
        if (source.isEmpty()) {
            return (isPositive ? "∅" : "Ω");
        }
        T item = source.iterator().next();
        Set<?> temp = source;
        if (item instanceof Comparable) {
            temp = new TreeSet(); // put in defined order for debugging
            temp.addAll((Set) source);
        }
        return (isPositive ? "" : Operation.NEGATION.display) + "{" + SPACE_JOINER.join(temp) + "}";
    }

    private static final Splitter SPACE_SPLITTER = Splitter.on(' ');

    public static PolaritySet<String> fromTestString(String source) {
        source = source.trim();
        boolean isNegated = source.startsWith(Operation.NEGATION.display);
        if (isNegated) {
            source = source.substring(1);
        }
        TreeSet<String> set = new TreeSet<>();
        switch (source) {
            case "∅":
                break;
            case "Ω":
                isNegated = !isNegated;
                break;
            default:
                if (!source.startsWith("{") || !source.endsWith("}")) {
                    return null;
                }
                set.addAll(SPACE_SPLITTER.splitToList(source.substring(1, source.length() - 1)));
                break;
        }
        return PolaritySet.of(set, !isNegated);
    }
}
