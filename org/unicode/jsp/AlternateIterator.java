package org.unicode.jsp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

public class AlternateIterator implements Iterator<String>, Iterable<String> {
    final String[][] sources;
    final int[] position;
    // optimize later
    final int length;
    boolean notDone = true;
    StringBuilder result = new StringBuilder();

    public static class Builder {
        List<List<String>> sources = new ArrayList<List<String>>();

        Builder add(Collection<String> items) {
            if (items.size() == 0) {
                throw new IllegalArgumentException();
            }
            final ArrayList<String> copy = new ArrayList<String>(items);
            sources.add(copy);
            return this;
        }

        public Builder add(String... items) {
            return add(Arrays.asList(items));
        }

        public AlternateIterator build() {
            return new AlternateIterator(sources);
        }
    }

    public static Builder start() {
        return new Builder();
    }

    private AlternateIterator(List<List<String>> inSources) {
        length = inSources.size();
        sources = new String[length][];
        for (int i = 0; i < length; ++i) {
            final List<String> list = inSources.get(i);
            sources[i] = list.toArray(new String[list.size()]);
        }
        position = new int[length];
    }

    @Override
    public boolean hasNext() {
        return notDone;
    }

    @Override
    public String next() {
        result.setLength(0);
        for (int i = 0; i < length; ++i) {
            result.append(sources[i][position[i]]);
        }
        int i;
        for (i = length-1; i >= 0; --i) {
            ++position[i];
            if (position[i] < sources[i].length) {
                break;
            }
            position[i] = 0;
        }
        if (i < 0) {
            notDone = false;
        }
        return result.toString();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<String> iterator() {
        return this;
    }

    public double getMaxSize() {
        double result = 1;
        for (int i = 0; i < length; ++i) {
            result *= sources[i].length;
        }
        return result;
    }

    public List<Collection<String>> getAlternates() {
        final List<Collection<String>> result = new ArrayList<Collection<String>>();
        for (int i = 0; i < length; ++i) {
            result.add(new TreeSet<String>(Arrays.asList(sources[i])));
        }
        return result;
    }
}
