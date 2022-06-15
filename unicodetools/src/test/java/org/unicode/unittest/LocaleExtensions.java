package org.unicode.unittest;

import com.google.common.collect.ImmutableMap;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.ULocale.Builder;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.unicode.unittest.LocaleCanonicalizer.ErrorChoice;

/**
 * Parse the t extension, because that isn't yet supported by ICU. Results are immutable. Internally
 * it is a single map of key-values, with the attributes represented by <u,attributes> and the tlang
 * by <t,tLang> (both attributes and tLang may be "" as may be the values of other keys like kc.
 */
public class LocaleExtensions {
    public static final Pattern TKEY = Pattern.compile("[a-zA-Z][0-9]");
    public static final Pattern TKEY_SPLITTER =
            Pattern.compile("(?:^|[-])(?=[a-zA-Z][0-9](?:$|[-]))");

    /** Puts keys in the order <a, ... s, t, a0, a1 ... z9, u, aa, ab, .. zz, v, w, y, z, x> */
    public static final Comparator<? super String> EXTENSION_ORDER =
            new Comparator<String>() {

                @Override
                public int compare(String o1, String o2) {
                    int diff = getBucket(o1) - getBucket(o2);
                    if (diff != 0) {
                        return diff;
                    }
                    return o1.compareTo(o2);
                }
            };

    final Map<String, String> tKeyValues;

    static final int a_sBucket = 0,
            tBucket = 1,
            tkeyBucket = 2,
            uBucket = 3,
            ukeyBucket = 4,
            vwyzBucket = 5,
            xBucket = 6;
    // TODO maybe optimize to make complete mapping to integers.
    public static int getBucket(String o1) {
        char first;
        switch (o1.length()) {
            case 1:
                first = o1.charAt(0);
                switch (first) {
                    case 'a':
                    case 'b':
                    case 'c':
                    case 'd':
                    case 'e':
                    case 'f':
                    case 'g':
                    case 'h':
                    case 'i':
                    case 'j':
                    case 'k':
                    case 'l':
                    case 'm':
                    case 'n':
                    case 'o':
                    case 'p':
                    case 'q':
                    case 'r':
                    case 's':
                        return a_sBucket;
                    case 't':
                        return tBucket;
                    case 'u':
                        return uBucket;
                    case 'v':
                    case 'w':
                    case 'y':
                    case 'z':
                        return vwyzBucket;
                    case 'x':
                        return xBucket;
                    default:
                        throw new IllegalArgumentException("Illegal key: " + o1);
                }
            case 2:
                first = o1.charAt(0);
                char second = o1.charAt(1);
                return second <= '9' ? tkeyBucket : ukeyBucket;
            default:
                throw new IllegalArgumentException("Illegal key: " + o1);
        }
    }

    /**
     * parse out the extension
     *
     * @param source
     */
    public static LocaleExtensions create(ULocale source, ErrorChoice throwChoice) {
        Map<String, String> _tKeyValues = new TreeMap<String, String>(EXTENSION_ORDER);

        Set<String> attr = source.getUnicodeLocaleAttributes();
        if (!attr.isEmpty()) {
            _tKeyValues.put("u", LocaleCanonicalizer.SEP_JOIN.join(attr));
        }

        for (String key : source.getUnicodeLocaleAttributes()) {
            String value = source.getUnicodeLocaleType(key);
            _tKeyValues.put(key, value);
        }

        String extensions = source.getExtension('t');
        if (extensions != null) {
            String[] tExtensions = TKEY_SPLITTER.split(extensions);
            boolean first = true;
            for (String part : tExtensions) {
                if (first & !TKEY.matcher(part).lookingAt()) {
                    _tKeyValues.put("t", part);
                    first = false;
                } else {
                    // TODO optimize
                    List<String> keyValue = LocaleCanonicalizer.SEP_SPLITTER.splitToList(part);
                    _tKeyValues.put(
                            keyValue.get(0),
                            LocaleCanonicalizer.SEP_JOIN.join(
                                    keyValue.subList(1, keyValue.size())));
                }
            }
        }
        return new LocaleExtensions(_tKeyValues);
    }

    public static LocaleExtensions create(Map<String, String> _tKeyValues) {
        return new LocaleExtensions(_tKeyValues);
    }

    /**
     * Creates new immutable T_Extension; does NOT check for correct syntax.
     *
     * @param _tLocale
     * @param _tKeyValues
     */
    private LocaleExtensions(Map<String, String> keyValues) {
        Map<String, String> _tKeyValues = new TreeMap<String, String>(EXTENSION_ORDER);
        _tKeyValues.putAll(keyValues);
        tKeyValues = ImmutableMap.copyOf(_tKeyValues);
    }

    public String toString() {
        if (tKeyValues.isEmpty()) {
            return "";
        }
        StringBuilder b = new StringBuilder();

        boolean haveT = false;
        boolean haveU = false;
        for (Entry<String, String> entry : tKeyValues.entrySet()) {
            String key = entry.getKey();
            switch (getBucket(key)) {
                case tBucket:
                    haveT = true;
                    break;
                case tkeyBucket:
                    if (!haveT) {
                        b.append("-t");
                    }
                    haveT = true;
                    break;
                case uBucket:
                    haveU = true;
                    break;
                case ukeyBucket:
                    if (!haveU) {
                        b.append("-u");
                    }
                    haveU = true;
                    break;
            }
            b.append('-').append(key);
            String value = entry.getValue();
            if (!value.isEmpty()) {
                b.append('-').append(value);
            }
        }
        return b.toString();
    }

    @Override
    public boolean equals(Object obj) {
        LocaleExtensions other = (LocaleExtensions) obj;
        if (!tKeyValues.equals(other.tKeyValues)) return false;
        // Important; make sure order is the same!
        Iterator<String> it = tKeyValues.keySet().iterator();
        Iterator<String> it2 = other.tKeyValues.keySet().iterator();
        while (it.hasNext()) {
            if (!it.next().equals(it2.next())) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return tKeyValues.hashCode();
    }

    /**
     * Set the builder fields
     *
     * @param builder
     */
    public void set(Builder builder) {
        boolean haveT = false;
        StringBuilder tValue = null; // allocate when needed
        for (Entry<String, String> entry : tKeyValues.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            switch (getBucket(key)) {
                case tBucket:
                    if (tValue == null) {
                        tValue = new StringBuilder();
                    }
                    tValue.append(value);
                    break;
                case tkeyBucket:
                    if (tValue == null) {
                        tValue = new StringBuilder();
                    } else {
                        tValue.append('-');
                    }
                    tValue.append(key).append('-').append(value);
                    break;
                case uBucket:
                    if (tValue.length() != 0) {
                        builder.setExtension('t', tValue.toString());
                        tValue = null;
                    }
                    throw new UnsupportedOperationException("no API to set attribute");
                case ukeyBucket:
                    if (tValue.length() != 0) {
                        builder.setExtension('t', tValue.toString());
                        tValue = null;
                    }
                    builder.setUnicodeLocaleKeyword(key, value);
                    break;
                case vwyzBucket:
                case xBucket:
                    if (tValue.length() != 0) {
                        builder.setExtension('t', tValue.toString());
                        tValue = null;
                    }
                    // fall through
                default:
                    builder.setExtension(key.charAt(0), value);
                    break;
            }
        }
    }
}
