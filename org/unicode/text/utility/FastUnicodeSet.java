package org.unicode.text.utility;

import com.ibm.icu.text.UnicodeSet;

public final class FastUnicodeSet {
	static final int index1Length = 272;
	static final int index2Length = 64;
	/**
	 * Structured as a simple trie. The last level is a long (64 bits). It is accessed by
	 * taking successive parts of the codepoint
	 */
	private final long[][] data = new long[272][64];
	private int size = 0;

	public FastUnicodeSet(UnicodeSet source) {
		// use a dumb implementation. Create a set of unique blocks of 4096 (6 bits x 6 bits)
		// This would be much faster if we traversed by ranges, and only stored unique blocks.
		// If we produce a flat structure, then an equality test can just iterate over
		// the longs.
		for (int i = 0; i < 0x110000; ++i) {
			if (source.contains(i)) {
				set(i);
				++size;
			}
		}
		// do compaction later
	}

	public int size() {
		return size;
	}

	public boolean contains(int codePoint) {
		int index1 = codePoint;
		final int index3 = index1 & 0x3F; // bottom 6 bits
		index1 >>= 6;
		final int index2 = index1 & 0x3F; // middle 6 bits
		index1 >>= 6; // top 9 bits
		return 0 != (data[index1][index2] & (1L<<index3));
	}

	public boolean containsAll(FastUnicodeSet other) {
		if (other.size > size) {
			return false; // quick check
		}
		for (int i = 0; i < index1Length; ++i) {
			final long[] data1 = data[i];
			final long[] data2 = other.data[i];
			for (int j = 0; j < index2Length; ++j) {
				// contains means that every 1 bit in data1 is on in data2
				// that is, data1 & data2 = data2
				final long data1a = data1[j];
				final long data2a = data2[j];
				if ((data1a & data2a) != data2a) {
					return false;
				}
			}
		}
		return true;
	}

	public boolean containsNone(FastUnicodeSet other) {
		for (int i = 0; i < index1Length; ++i) {
			final long[] data1 = data[i];
			final long[] data2 = other.data[i];
			for (int j = 0; j < index2Length; ++j) {
				// containsSome means that some bit is on in both
				final long data1a = data1[j];
				final long data2a = data2[j];
				if ((data1a & data2a) != 0) {
					return false;
				}
			}
		}
		return true;
	}

	public boolean equals(FastUnicodeSet other) {
		if (size != other.size) {
			return false;
		}
		// this can be optimized later, since we know they must have the same structure.
		// thus if we see two top-level items we've compared earlier, we can skip
		for (int i = 0; i < index1Length; ++i) {
			final long[] data1 = data[i];
			final long[] data2 = other.data[i];
			for (int j = 0; j < index2Length; ++j) {
				// contains means that every 1 bit in data1 is on in data2
				// that is, data1 & data2 = data2
				if (data1[j] != data2[j]) {
					return false;
				}
			}
		}
		return true;
	}

	private void set(int codePoint) {
		int index1 = codePoint;
		final int index3 = index1 & 0x3F; // bottom 6 bits
		index1 >>= 6;
			final int index2 = index1 & 0x3F; // middle 6 bits
			index1 >>= 6; // top 11 bits
			data[index1][index2] |= 1L<<index3;
	}
}