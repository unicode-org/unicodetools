/**
 *******************************************************************************
 * Copyright (C) 1996-2001, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /home/cvsroot/unicodetools/org/unicode/text/utility/EnumBase.java,v $
 *
 *******************************************************************************
 */

package org.unicode.text.utility;

import java.util.ArrayList;
import java.util.List;

/**
    Used for generating fake enums. These can be compared with ==,
    used in for statements, etc.
    Subclasses will be of the form:
    <pre>
    static public class MyEnum extends EnumBase {
        public static MyEnum
            ZEROED = (MyEnum) makeNext(new MyEnum(), "ZEROED"),
            SHIFTED = (MyEnum) makeNext(new MyEnum(), "SHIFTED"),
            NON_IGNORABLE = (MyEnum) makeNext(new MyEnum(), "NON_IGNORABLE");
        public MyEnum next() { return (MyEnum) internalNext(); }
    }
    </pre>
 */

public class EnumBase implements Comparable {

    /** For use in collections
     */
    @Override
    public int compareTo(Object other) {
        final EnumBase that = (EnumBase) other;
        return value < that.value ? -1 : value > that.value ? 1 : 0;
    }

    // dont' need equals, since object identity sufficies.

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public String toString() {
        return (String)uniqueNames.get(value);
    }

    //////////////////

    private int value;
    private static List uniqueList = new ArrayList();
    private static List uniqueNames = new ArrayList();

    /** For use in for(..) statements
     */
    public Object internalNext() {
        final int temp = value + 1;
        if (temp >= uniqueList.size()) {
            return null;
        }
        final Object result = uniqueList.get(temp);
        if (getClass() != result.getClass()) {
            return null;
        }
        return result;
    }

    /**
     * For constructing the enums the first time
     */
    static protected EnumBase makeNext(EnumBase result, String name) {
        try {
            result.value = uniqueList.size();
            uniqueList.add(result);
            uniqueNames.add(name);
            return result;
        } catch (final Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Internal Error in " + result);
        }
    }

    /*
    protected final int getValue() {
        return value;
    }
     */

    protected EnumBase() {}
}

