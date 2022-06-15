package org.unicode.draft;

import com.ibm.icu.text.Collator;
import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentHashMap;

public abstract class IcuCache<K, V> {
    private final ConcurrentHashMap<K, SoftReference<V>> cache =
            new ConcurrentHashMap<K, SoftReference<V>>();

    protected abstract V getInstance(K key);

    public V get(K key) {
        // get the value from the cache if possible;
        // otherwise, create from getInstance and add to cache
        return null;
    }

    static {
        final IcuCache<String, Collator> SINGLETON =
                new IcuCache<String, Collator>() {
                    @Override
                    protected Collator getInstance(String key) {
                        // generate the collator corresponding to the string
                        return null;
                    }
                };
    }
}
