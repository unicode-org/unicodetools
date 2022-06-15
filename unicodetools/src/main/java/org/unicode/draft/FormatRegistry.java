package org.unicode.draft;

import com.ibm.icu.util.ULocale;
import java.text.Format;

public interface FormatRegistry {

    /**
     * Return a default Format for a given type of object.
     *
     * @param obj the input Object. For example, for an object of type Number, a NumberFormat would
     *     be appropriate to return.
     * @param ulocale
     * @return default format, or null if none available for that type of object.
     */
    public abstract Format getFormatForObject(Class classType, ULocale ulocale);

    /**
     * Return a key, like "number", or "number,currency", or "number,#0,0#". If that key were passed
     * into getFormat (with the same uLocale), then a format would be generated that would be equal
     * to this one.
     *
     * @param format The format to generate a key for.
     * @param ulocale
     * @return
     */
    public abstract String getKey(Format format, ULocale ulocale);

    /**
     * From a key of the form mainType, subType, return a format. Either one may be a pattern.
     *
     * @param mainType Guaranteed to be non-empty.
     * @param subType May be empty or not. An empty subtype always works (if the mainType is valid).
     * @param ulocale
     * @exception IllegalArgumentException thrown if the mainType is not valid, or or the subType
     *     invalid for the mainType.
     * @return
     */
    public abstract Format getFormat(String mainType, String subType, ULocale ulocale);
}
