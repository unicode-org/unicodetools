/**
 *******************************************************************************
 * Copyright (C) 1996-2001, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /home/cvsroot/unicodetools/org/unicode/text/UCD/IntMap.java,v $
 * $Date: 2007-02-11 08:15:09 $
 * $Revision: 1.2 $
 *
 *******************************************************************************
 */

package org.unicode.text.UCD;
import java.util.HashMap;

public class IntMap {
    int lowest = Integer.MAX_VALUE;
    int highest = Integer.MIN_VALUE;
    HashMap store = new HashMap();

    public Object get(int key) {
        if (key < lowest || key > highest) {
            return null;
        }
        return store.get(new Integer(key));
    }

    public void put(int key, Object value) {
        if (key < lowest) {
            lowest = key;
        }
        if (key > highest) {
            highest = key;
        }
        store.put(new Integer(key), value);
    }

    public int size() {
        return store.size();
    }
}
