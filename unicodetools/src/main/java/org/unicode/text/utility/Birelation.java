package org.unicode.text.utility;

import com.ibm.icu.impl.Relation;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class Birelation<K, V> {
    private final Relation<K, V> keyToValues;
    private final Relation<V, K> valueToKeys;

    public Birelation(
            Map<K, Set<V>> map1,
            Map<V, Set<K>> map2,
            Class valueSetCreator,
            Class keySetCreator,
            Comparator<V> valueSetComparator,
            Comparator<K> keySetComparator) {
        keyToValues = Relation.of(map1, valueSetCreator, valueSetComparator);
        valueToKeys = Relation.of(map2, keySetCreator, keySetComparator);
    }

    public static <K, V> Birelation<K, V> of(
            Map<K, Set<V>> map1,
            Map<V, Set<K>> map2,
            Class setCreator1,
            Class setCreator2,
            Comparator<V> setComparator1,
            Comparator<K> setComparator2) {
        return new Birelation<K, V>(
                map1, map2, setCreator1, setCreator2, setComparator1, setComparator2);
    }

    public Birelation<K, V> add(K key, V value) {
        keyToValues.put(key, value);
        valueToKeys.put(value, key);
        return this;
    }

    public Birelation<K, V> remove(K key, V value) {
        keyToValues.remove(key, value);
        valueToKeys.remove(value, key);
        return this;
    }

    public Birelation<K, V> removeKey(K key) {
        Set<V> values = keyToValues.get(key);
        if (values == null) {
            return this;
        }
        for (V value : values) {
            valueToKeys.remove(value, key);
        }
        keyToValues.removeAll(key);
        return this;
    }

    public Set<V> getValues(K key) {
        return keyToValues.get(key);
    }

    public Set<K> getKeys(V value) {
        return valueToKeys.get(value);
    }

    public Set<Entry<K, Set<V>>> keyValuesSet() {
        return keyToValues.keyValuesSet();
    }

    public Set<Entry<V, Set<K>>> valueKeysSet() {
        return valueToKeys.keyValuesSet();
    }

    public void freeze() {
        keyToValues.freeze();
        valueToKeys.freeze();
    }

    public Set<K> keySet() {
        return keyToValues.keySet();
    }

    public Set<V> valuesSet() {
        return valueToKeys.keySet();
    }

    public Relation<Set<V>, K> getValuesToKeys() {
        Relation<Set<V>, K> result =
                Relation.of(new HashMap<Set<V>, Set<K>>(), LinkedHashSet.class);
        for (Entry<K, Set<V>> entry : keyToValues.keyValuesSet()) {
            K key = entry.getKey();
            Set<V> values = entry.getValue();
            result.put(values, key);
        }
        return result;
    }
}
