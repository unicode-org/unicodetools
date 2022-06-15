/**
 * ****************************************************************************** Copyright (C)
 * 1996-2001, International Business Machines Corporation and * others. All Rights Reserved. *
 * ******************************************************************************
 *
 * <p>$Source: /home/cvsroot/unicodetools/org/unicode/text/utility/IntStack.java,v $
 *
 * <p>******************************************************************************
 */
package org.unicode.text.utility;

// =============================================================
// Simple stack mechanism, with push, pop and access
// =============================================================

public final class IntStack implements Comparable<IntStack>, Cloneable {
    private int[] values;
    private int top = 0;
    private int first = 0;

    public IntStack(int initialSize) {
        values = new int[initialSize];
    }

    public IntStack append(IntStack other) {
        // TODO speed up by copying arrays
        for (int i = other.first; i < other.top; ++i) {
            push(other.values[i]);
        }
        return this;
    }

    public IntStack append(int value) {
        return push(value);
    }

    public int length() {
        return top - first;
    }

    public IntStack push(int value) {
        if (top >= values.length) { // must grow?
            final int[] temp = new int[values.length * 2];
            System.arraycopy(values, 0, temp, 0, values.length);
            values = temp;
        }
        values[top++] = value;
        return this;
    }

    public int pop() {
        if (top > first) {
            final int result = values[--top];
            if (top == first && first > 0) {
                top = first = 0;
            }
            return result;
        }
        throw new IllegalArgumentException("Stack underflow");
    }

    public int popFront() {
        if (top > first) {
            final int result = values[first++];
            if (top == first) {
                top = first = 0;
            }
            return result;
        }
        throw new IllegalArgumentException("Stack underflow");
    }

    public int get(int index) {
        final int i = first + index;
        if (first <= i && i < top) {
            return values[i];
        }
        throw new ArrayIndexOutOfBoundsException(index);
    }

    public void set(int index, int value) {
        final int i = first + index;
        if (first <= i && i < top) {
            values[i] = value;
            return;
        }
        throw new ArrayIndexOutOfBoundsException(index);
    }

    public boolean isEmpty() {
        return top == first;
    }

    public void clear() {
        top = first = 0;
    }

    @Override
    public int compareTo(IntStack other) {
        final IntStack that = other;
        final int myLen = top - first;
        final int thatLen = that.top - that.first;
        final int limit = first + ((myLen < thatLen) ? myLen : thatLen);
        final int delta = that.first - first;
        for (int i = first; i < limit; ++i) {
            final int result = values[i] - that.values[i + delta];
            if (result != 0) {
                return result;
            }
        }
        return myLen - thatLen;
    }

    public boolean equals(IntStack other) {
        return compareTo(other) == 0;
    }

    @Override
    public int hashCode() {
        int result = top;
        for (int i = first; i < top; ++i) {
            result = result * 37 + values[i];
        }
        return result;
    }

    @Override
    public Object clone() {
        try {
            final IntStack result = (IntStack) super.clone();
            result.values = result.values.clone();
            return result;
        } catch (final CloneNotSupportedException e) {
            throw new IllegalArgumentException("Will never happen");
        }
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        for (int i = first; i < top; ++i) {
            if (result.length() != 0) {
                result.append(",");
            }
            result.append(Utility.hex(0xFFFFFFFFL & values[i]));
        }
        return result.toString();
    }

    /**
     * Copy items from the stack into a buffer, and return number of items copied.
     *
     * @param limit
     * @param start
     * @param buffer
     * @param start
     * @return
     */
    public int extractInto(int start, int limit, int[] buffer, int bufferStart) {
        final int len = limit - start;
        System.arraycopy(values, first + start, buffer, bufferStart, len);
        return len;
    }
}
