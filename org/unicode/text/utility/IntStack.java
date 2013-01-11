/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /home/cvsroot/unicodetools/org/unicode/text/utility/IntStack.java,v $
*
*******************************************************************************
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
            int[] temp = new int[values.length*2];
            System.arraycopy(values,0,temp,0,values.length);
            values = temp;
        }
        values[top++] = value;
        return this;
    }

    public int pop() {
        if (top > first) {
            int result = values[--top];
            if (top == first && first > 0) {
                top = first = 0;
            }
            return result;
        }
        throw new IllegalArgumentException("Stack underflow");
    }

    public int popFront() {
        if (top > first) {
            int result = values[first++];
            if (top == first) {
                top = first = 0;
            }
            return result;
        }
        throw new IllegalArgumentException("Stack underflow");
    }

    public int get(int index) {
        int i = first + index;
        if (first <= i && i < top) {
            return values[i];
        }
        throw new ArrayIndexOutOfBoundsException(index);
    }

    public void set(int index, int value) {
        int i = first + index;
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
    
    public int compareTo(IntStack other) {
        IntStack that = other;
        int myLen = top - first;
        int thatLen = that.top - that.first;
        int limit = first + ((myLen < thatLen) ? myLen : thatLen);
        int delta = that.first - first;
        for (int i = first; i < limit; ++i) {
            int result = values[i] - that.values[i + delta];
            if (result != 0) return result;
        }
        return myLen - thatLen;
    }

    public boolean equals(IntStack other) {
        return compareTo(other) == 0;
    }

    public int hashCode() {
        int result = top;
        for (int i = first; i < top; ++i) {
            result = result * 37 + values[i];
        }
        return result;
    }

    public Object clone() {
        try {
            IntStack result = (IntStack) super.clone();
            result.values = (int[]) result.values.clone();
            return result;
        } catch (CloneNotSupportedException e) {
            throw new IllegalArgumentException("Will never happen");
        }
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
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
     * @param limit 
     * @param start 
     * @param buffer
     * @param start
     * @return
     */
    public int extractInto(int start, int limit, int[] buffer, int bufferStart) {
        int len = limit-start;
        System.arraycopy(values, first + start, buffer, bufferStart, len);
        return len;
    }
}
