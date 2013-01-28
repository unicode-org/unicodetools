package org.unicode.draft;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;

public class SetComparator<T extends Comparable<T>> implements Comparator<Set<T>> {
	@Override
	public int compare(Set<T> o1, Set<T> o2) {
		final int size1 = o1.size();
		final int size2 = o2.size();
		int diff = size1 - size2;
		if (diff != 0) {
			return diff;
		}
		final Iterator<T> i1 = o1.iterator();
		final Iterator<T> i2 = o2.iterator();
		while (i1.hasNext() && i2.hasNext()) {
			final T item1 = i1.next();
			final T item2 = i2.next();
			diff = item1.compareTo(item2);
			if (diff != 0) {
				return diff;
			}
		}
		// we know that they are the same length at this point, so if we
		// make it through the gauntlet, we're done
		return 0;
	}
}