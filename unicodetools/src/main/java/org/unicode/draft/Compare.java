package org.unicode.draft;

import java.util.Comparator;

public class Compare {

    public static Compare START = new Compare();

    private static Compare GREATER = new CompareDone(1), LESS = new CompareDone(-1);

    public <T extends Comparable<T>> Compare compare(T a, T b) {
        if (a == null) {
            return b == null ? this : LESS;
        } else if (b == null) {
            return GREATER;
        }
        final int temp = a.compareTo(b);
        if (temp == 0) {
            return this;
        }
        if (temp < 0) {
            return LESS;
        }
        return GREATER;
    }

    public <T> Compare compare(T a, T b, Comparator<T> comparator) {
        if (a == null) {
            return b == null ? this : LESS;
        } else if (b == null) {
            return GREATER;
        }
        final int temp = comparator.compare(a, b);
        if (temp == 0) {
            return this;
        }
        if (temp < 0) {
            return LESS;
        }
        return GREATER;
    }

    public Compare compare(int a, int b) {
        if (a == b) {
            return this;
        }
        if (a < b) {
            return LESS;
        }
        return GREATER;
    }

    public int done() {
        return 0;
    }

    private Compare() {} // hide constructor

    private static class CompareDone extends Compare {
        private final int result;

        private CompareDone(int result) {
            this.result = result;
        }

        @Override
        public int done() {
            return result;
        }

        @Override
        public Compare compare(int a, int b) {
            return this;
        }

        @Override
        public <T extends Comparable<T>> Compare compare(T a, T b) {
            return this;
        }

        public <T extends Comparable<T>> Compare compare(T a, T b, Comparator<T> comparator) {
            return this;
        }
    }

    static class Foo implements Comparable<Foo> {
        int a, b;
        String c;

        public Foo(int a, int b, String c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }

        @Override
        public int compareTo(Foo other) {
            return Compare.START.compare(a, other.a).compare(b, other.b).compare(c, other.c).done();
        }
    }

    public static void main(String[] args) {
        final Foo a = new Foo(1, 2, "ab");
        final Foo b = new Foo(0, 2, "ab");
        final Foo c = new Foo(1, 2, "ab");
        System.out.println(a.compareTo(b));
        System.out.println(a.compareTo(c));
    }
}
