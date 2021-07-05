package org.unicode.draft;

public class Alphagram {
    private final int[] letters = new int[26]; // only care about ascii

    public Alphagram(String a) {
        for (int i = 0; i < a.length(); ++i) {
            final char ch = a.charAt(i);
            if (ch < 'A' || ch > 'z' || (ch > 'Z' && ch < 'a')) {
                throw new IllegalArgumentException("Only A-Z allowed");
            }
            ++letters[(ch & 0x1F) - 1];
        }
    }

    public Alphagram() {
    }

    public Alphagram intersection(Alphagram other) {
        final Alphagram result = new Alphagram();
        for (int i = 0; i < letters.length; ++i) {
            result.letters[i] = Math.min(letters[i], other.letters[i]);
        }
        return result;
    }

    public int countShared(Alphagram other) {
        int total = 0;
        for (int i = 0; i < letters.length; ++i) {
            total += Math.min(letters[i], other.letters[i]);
        }
        return total;
    }

    @Override
    public String toString() {
        final StringBuffer result = new StringBuffer();
        for (int i = 0; i < letters.length; ++i) {
            final int count = letters[i];
            while (count > 0) {
                result.append((char)(i + 'a'));
            }
        }
        return result.toString();
    }
}
