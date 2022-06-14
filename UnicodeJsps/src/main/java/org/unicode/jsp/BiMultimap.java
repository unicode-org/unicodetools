package org.unicode.jsp;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

class BiMultimap<K, V> {
    private final Multimap<K, V> keyToValues = LinkedHashMultimap.create();
    private final Multimap<V, K> valueToKeys = LinkedHashMultimap.create();
    private final Collection<K> defaultKeys;
    private final Collection<V> defaultValues;

    BiMultimap(Collection<K> defaultKeys, Collection<V> defaultValues) {
        this.defaultKeys = defaultKeys;
        this.defaultValues = defaultValues;
    }

    public void putAll(K key, Collection<V> values) {
        keyToValues.putAll(key, values);
        putAll(valueToKeys, values, key);
    }

    public static <K, V> void putAll(Multimap<K, V> kToVs, Collection<K> keys, V value) {
        for (K key : keys) {
            kToVs.put(key, value);
        }
    }

    public Collection<K> getKeys(V value) {
        Collection<K> result = valueToKeys.get(value);
        return result.isEmpty() ? defaultKeys : result;
    }

    public Collection<V> getValues(K key) {
        Collection<V> result = keyToValues.get(key);
        return result.isEmpty() ? defaultValues : result;
    }

    public Multimap<K, V> getKeyToValues() {
        return keyToValues;
    }

    @Override
    public String toString() {
        return keyToValues.toString();
    }

    public Set<K> keySet() {
        return Collections.unmodifiableSet(keyToValues.keySet());
    }

    public Set<V> valueSet() {
        return Collections.unmodifiableSet(valueToKeys.keySet());
    }
}
