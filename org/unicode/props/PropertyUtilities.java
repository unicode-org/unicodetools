package org.unicode.props;

import java.util.Collection;
import java.util.Map;

import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.UnicodeMap;

public class PropertyUtilities {

	public static interface Merge<V> {
		V merge(V first, V second);
	}

	public static final class Joiner implements Merge<String> {
		static String separator;
		/**
		 * @param separator
		 */
		public Joiner(String separator) {
			Joiner.separator = separator;
		}
		@Override
		public String merge(String first, String second) {
			return first + separator + second;
		}
	}

	static final <K, V, M extends Map<K,V>> M putNew(M map, K key, V value) {
		final V oldValue = map.get(key);
		if (oldValue != null) {
			throw new IllegalArgumentException("Key already present in Map: " + key + ",\told: " + oldValue + ",\tnew: " + value);
		}
		map.put(key, value);
		return map;
	}

	static final <V> UnicodeMap<V> putNew(UnicodeMap<V> map, int key, V value, Merge<V> merger) {
		final V oldValue = map.get(key);
		if (oldValue != null) {
			if (merger == null) {
				throw new IllegalArgumentException("Key already present in UnicodeMap: " + Utility.hex(key) + ",\told: " + oldValue + ",\tnew: " + value);
			}
			value = merger.merge(oldValue, value);
		}
		map.put(key, value);
		return map;
	}

	static final <V> UnicodeMap<V> putNew(UnicodeMap<V> map, String key, V value, Merge<V> merger) {
		final V oldValue = map.get(key);
		if (oldValue != null) {
			if (merger == null) {
				throw new IllegalArgumentException("Key already present in UnicodeMap: " + Utility.hex(key) + ",\told: " + oldValue + ",\tnew: " + value);
			}
			value = merger.merge(oldValue, value);
		}
		map.put(key, value);
		return map;
	}

	static final <V, C extends Collection<V>> C addNew(C collection, V value) {
		if (collection.contains(value)) {
			throw new IllegalArgumentException("Value already present in Collection: " + value);
		}
		collection.add(value);
		return collection;
	}
}
