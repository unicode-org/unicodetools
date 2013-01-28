package org.unicode.draft;
import java.util.Iterator;

import org.unicode.cldr.util.Timer;

import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.ULocale;


public class CodePoint implements Iterator<Integer>, Iterable<Integer> {
	private static final int SUPPLEMENTAL_OFFSET =
			(Character.MIN_HIGH_SURROGATE << 10) + Character.MIN_LOW_SURROGATE
			- Character.MIN_SUPPLEMENTARY_CODE_POINT;
	private final CharSequence charSequence;
	private int position;
	private StringBuilder builder;

	public CodePoint(CharSequence charSequence) {
		this.charSequence = charSequence;
	}

	@Override
	public boolean hasNext() {
		return position < charSequence.length();
	}

	@Override
	public Integer next() {
		final int cp = Character.codePointAt(charSequence, position);
		position += Character.charCount(cp);
		return cp;
	}

	@Override
	public void remove() {
		// TODO allow this for modifiable strings
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<Integer> iterator() {
		return this;
	}

	static CodePoint with (CharSequence s) {
		return new CodePoint(s);
	}

	static int[] full (CharSequence s) {
		final int len = s.length();
		int[] result = new int[len];
		int pos = 0;
		for (int i = 0; i < len;) {
			int cp = s.charAt(i++);
			// The key to performance is that surrogate pairs are very rare.
			// Test for a trail (low) surrogate.
			if (cp >= Character.MIN_LOW_SURROGATE && cp < Character.MAX_LOW_SURROGATE && pos > 0) {
				// If we get a trail, and if the last code point was a lead (high) surrogate,
				// we need to backup and set the correct value
				final int last = result[pos-1];
				if (last >= Character.MIN_HIGH_SURROGATE && last <= Character.MAX_HIGH_SURROGATE) {
					--pos;
					cp += (last << 10) - SUPPLEMENTAL_OFFSET;
				}
			}
			result[pos++] = cp;
		}
		// In the unusual case that we hit a supplemental code point, resize
		if (pos < len) {
			final int[] result2 = new int[pos];
			System.arraycopy(result, 0, result2, 0, pos);
			result = result2;
		}
		return result;
	}

	static final class CodePointIterator {
		private final CharSequence buffer;
		private final int length;
		private int position = 0;

		public int codePoint;

		public CodePointIterator(CharSequence s) {
			buffer = s;
			length = s.length();
		}

		public void reset() {
			position = 0;
		}

		public boolean next() {
			if (position >= length) {
				return false;
			}
			int cp = buffer.charAt(position++);
			if (cp >= Character.MIN_HIGH_SURROGATE && cp <= Character.MAX_HIGH_SURROGATE && position < length) {
				final int trail = buffer.charAt(position);
				if (trail >= Character.MIN_LOW_SURROGATE && trail <= Character.MAX_LOW_SURROGATE) {
					cp = (cp << 10) + trail - SUPPLEMENTAL_OFFSET;
					++position;
				}
			}
			codePoint = cp;
			return true;
		}
	}

	public static void main(String[] args) {
		System.out.print("Warmup\t");
		timeMethods("a\uD800\uDC00", 100001); // warmup

		final String[] tests = {"In a hole in the ground there lived a hobbit.",
				"In a hole in the ground there lived a hobbit.\uD800\uDC00",
				"In a hole in the ground there lived a hobbit.\uD800",
		"\uDC00In a hole in the ground there lived a hobbit."};
		for (final String test : tests) {
			timeMethods(test, 10000001);
		}
	}

	private static  NumberFormat nf = NumberFormat.getNumberInstance(ULocale.ENGLISH);

	private static void timeMethods(CharSequence s, int ITERATIONS) {
		System.out.println("Testing <" + s + "> for " + nf.format(ITERATIONS) + " iterations");
		int doSomethingWith = 0;
		int doSomethingWith2 = 0;

		// wrong code, but clumsy
		final Timer timer = new Timer();
		timer.start();
		for (int iteration = ITERATIONS; iteration > 0; --iteration) {
			for (int i = 0; i < s.length(); ++i) {
				final char cp = s.charAt(i);
				doSomethingWith ^= cp;
			}
		}
		timer.stop();
		System.out.println("Wrong\t" + timer.toString(ITERATIONS));

		// correct code, but clumsy
		doSomethingWith = 0;
		timer.start();
		for (int iteration = ITERATIONS; iteration > 0; --iteration) {
			int cp;
			for (int i = 0; i < s.length(); i += Character.charCount(cp)) {
				cp = Character.codePointAt(s, i);
				doSomethingWith ^= cp;
			}
		}
		final long base = timer.stop();
		System.out.println("Correct\t" + timer.toString(ITERATIONS));

		// correct code, optimized
		timer.start();
		for (int iteration = ITERATIONS; iteration > 0; --iteration) {
			final int len = s.length();
			for (int i = 0; i < len;) {
				int cp = s.charAt(i++);
				if (cp >= Character.MIN_HIGH_SURROGATE && cp <= Character.MAX_HIGH_SURROGATE && i < len) {
					final int trail = s.charAt(i);
					if (trail >= Character.MIN_LOW_SURROGATE && trail < Character.MAX_LOW_SURROGATE) {
						cp = (cp << 10) + trail - SUPPLEMENTAL_OFFSET;
						++i;
					}
				}
				doSomethingWith2 ^= cp;
			}
		}
		timer.stop();
		System.out.println("Optim.\t" + timer.toString(ITERATIONS, base));
		if (doSomethingWith2 != doSomethingWith) {
			throw new IllegalArgumentException();
		}

		// easy code (but slower)
		doSomethingWith2 = 0;
		timer.start();
		for (int iteration = ITERATIONS; iteration > 0; --iteration) {
			for (final CodePointIterator it = new CodePointIterator(s); it.next();) {
				doSomethingWith2 ^= it.codePoint;
			}
		}
		timer.stop();
		System.out.println("Iter.\t" + timer.toString(ITERATIONS, base));
		if (doSomethingWith2 != doSomethingWith) {
			throw new IllegalArgumentException();
		}

		// easy code, and faster -- IF all the code points are traversed
		doSomethingWith2 = 0;
		timer.start();
		for (int iteration = ITERATIONS; iteration > 0; --iteration) {
			for (final int cp3 : CodePoint.full(s)) {
				doSomethingWith2 ^= cp3;
			}
		}
		timer.stop();
		System.out.println("int[]\t" + timer.toString(ITERATIONS, base));
		if (doSomethingWith2 != doSomethingWith) {
			throw new IllegalArgumentException();
		}
		System.out.println();
	}
}
