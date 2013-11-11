/**
 *******************************************************************************
 * Copyright (C) 1996-2001, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /home/cvsroot/unicodetools/org/unicode/text/utility/PoorMansEnum.java,v $
 *
 *******************************************************************************
 */

/* Goal for enum is:
 * Easy to use
 * ID <-> int
 * ID <-> string name
 */
package org.unicode.text.utility;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PoorMansEnum {
    protected int value;
    protected String name;
    protected PoorMansEnum next;

    public int toInt() {
        return value;
    }

    @Override
    public String toString() {
        return name;
    }

    // for subclassers

    protected PoorMansEnum() {
    }

    /** Utility for subclasses
     */
    protected static class EnumStore {
        private final List int2Id = new ArrayList();
        private final Map string2Id = new HashMap();
        private PoorMansEnum last = null;

        public PoorMansEnum add(PoorMansEnum id, String name) {
            // both string and id must be new!
            if (int2Id.indexOf(id) >= 0) {
                throw new IllegalArgumentException("ID already stored for \"" + name + '"');
            } else if (string2Id.containsKey(name)) {
                throw new IllegalArgumentException('"' + name + "\" already stored for ID ");
            }
            id.value = int2Id.size();
            id.name = name;
            if (last != null) {
                last.next = id;
            }
            int2Id.add(id);
            string2Id.put(name, id);
            last = id;
            return id;
        }

        public PoorMansEnum addAlias(PoorMansEnum id, String name) {
            // id must be old, string must be new
            if (int2Id.indexOf(id) < 0) {
                throw new IllegalArgumentException("ID must already be stored for \"" + name + '"');
            } else if (string2Id.containsKey(name)) {
                throw new IllegalArgumentException('"' + name + "\" already stored for ID ");
            }
            string2Id.put(name, id);
            return id;
        }

        public Collection getAliases(PoorMansEnum id, Collection output) {
            final Iterator it = string2Id.keySet().iterator();
            while (it.hasNext()) {
                final Object s = it.next();
                if (s == id.name) {
                    continue;
                }
                if (id == string2Id.get(s)) {
                    output.add(s);
                }
            }
            return output;
        }

        public int getMax() {
            return int2Id.size();
        }

        public PoorMansEnum get(int value) {
            return (PoorMansEnum) int2Id.get(value);
        }

        public PoorMansEnum get(String name) {
            return (PoorMansEnum) string2Id.get(name);
        }
    }
}