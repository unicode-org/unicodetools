/*
 **********************************************************************
 *   Copyright (C) 2001-2010, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 **********************************************************************
 *   Date        Name        Description
 *   06/08/01    aliu        Creation.
 **********************************************************************
 */

package org.unicode.jsp;
import com.ibm.icu.text.Replaceable;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.Transliterator;

final class SimpleTransliterator extends Transliterator {
    private final Transform<String,String> stringTransform;

    /**
     * Constructs a transliterator.
     */
    public SimpleTransliterator(String id, Transform<String,String> stringTransform) {
        super(id, null);
        this.stringTransform = stringTransform;
    }

    /**
     * Implements {@link Transliterator#handleTransliterate}.
     */
    protected void handleTransliterate(Replaceable text,
            Position offsets, boolean isIncremental) {
        // start and limit of the input range
        int start = offsets.start;
        int limit = offsets.limit;
        if(start >= limit) {
            return;
        }
        // should convert in small chunks, but for now...
        StringBuilder b = new StringBuilder();
        for (int i = start; i < limit; ++i) {
            int cp = text.char32At(i);
            b.appendCodePoint(cp);
            if (cp > 0xFFFF) {
                ++i;
            }
        }
        String toSubstitute = stringTransform.transform(b.toString());
        text.replace(start, limit, toSubstitute);
        limit = start + toSubstitute.length();

        offsets.start = limit;
        offsets.contextLimit += limit - offsets.limit;
        offsets.limit = limit;
    }
}
