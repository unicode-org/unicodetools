package org.unicode.bidi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

public class GenerateN1Tests {
    enum Sample {AL("\u0627\u062F\u0630\u0631"), R("\u05D0\u05D1\u05d2\u05d3"), L("abcd"), AN("\u0660\u0661\u0662\u0663"), EN("6789"), N("!&?@");
    static EnumSet<Sample> STRONG = EnumSet.of(Sample.R, Sample.AL, Sample.L);
    static EnumSet<Sample> NUMERIC = EnumSet.of(Sample.AN, Sample.EN);
    private final String[] str = new String[2];
    Sample(String instr) {
        str[0] = instr.substring(0,2);
        str[1] = instr.substring(2);
    }
    }
    static class SampleEnumerator {
        private final List<Sample> items = new ArrayList<Sample>();
        private final List<Sample> readOnlyItems = Collections.unmodifiableList(items);
        final int maxSize;

        public SampleEnumerator(int count) {
            maxSize = count;
        }

        boolean next() {
            for (int i = items.size()-1; i >= 0; --i) {
                final Sample oldValue = items.get(i);
                final Sample newValue = oldValue.ordinal() < Sample.values().length - 1 ? Sample.values()[oldValue.ordinal()+1] : null; // next value
                if (newValue != null) {
                    items.set(i, newValue);
                    return true;
                }
                items.set(i, Sample.values()[0]); // first value
            }
            if (items.size() < maxSize) {
                items.add(0, Sample.values()[0]);
                return true;
            }
            return false;
        }

        public String toString(boolean codes) {
            final EnumSet<Sample> seen = EnumSet.noneOf(Sample.class);
            Sample lastItem = null;
            final StringBuilder result = new StringBuilder();
            for (final Sample item : items) {
                if (item == lastItem) {
                    continue;
                }
                lastItem = item;
                if (codes) {
                    if (!seen.isEmpty()) {
                        result.append(" ");
                    }
                    result.append(item);
                } else {
                    result.append(item.str[seen.contains(item) ? 1 : 0]);
                }
                seen.add(item);
            }
            return result.toString();
        }

        public List<Sample> getItems() {
            return readOnlyItems;
        }
    }

    public static void main(String[] args) {
        final SampleEnumerator samples = new SampleEnumerator(5);
        System.out.println("\uFEFF");
        int count = 1;
        while (samples.next()) {
            if (samples.getItems().size() != 5) {
                continue;
            }
            if (samples.getItems().get(2) != Sample.N) {
                continue;
            }
            if (samples.getItems().get(1) == Sample.N
                    || samples.getItems().get(3) == Sample.N
                    ) {
                continue;
            }
            if (!Sample.STRONG.contains(samples.getItems().get(0))) {
                continue;
            }
            if (!Sample.STRONG.contains(samples.getItems().get(4))) {
                continue;
            }
            if (!(Sample.NUMERIC.contains(samples.getItems().get(1))
                    || Sample.NUMERIC.contains(samples.getItems().get(3)))) {
                continue;
            }
            if (Sample.STRONG.contains(samples.getItems().get(1))
                    && samples.getItems().get(1) != samples.getItems().get(0)) {
                continue;
            }
            if (Sample.STRONG.contains(samples.getItems().get(3))
                    && samples.getItems().get(3) != samples.getItems().get(4)) {
                continue;
            }

            System.out.println((count++) + ": " + samples.toString(true));
            System.out.println(samples.toString(false));
        }
    }
}
