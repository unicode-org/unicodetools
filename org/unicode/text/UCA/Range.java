/**
 * 
 */
package org.unicode.text.UCA;


public class Range {
	private int minimum = Integer.MAX_VALUE;
	private int maximum = Integer.MIN_VALUE;

	public int getMinimum() {
		return minimum;
	}

	public int getMaximum() {
		return maximum;
	}

	public Range add(int... newValues) {
		for (final int newValue : newValues) {
			if (minimum > newValue) {
				minimum = newValue;
			}
			if (maximum < newValue) {
				maximum = newValue;
			}
		}
		return this;
	}

	public Range add(Range range) {
		return add(range.minimum, range.maximum);
	}

	public boolean hasItems() {
		return minimum <= maximum;
	}

	@Override
	public String toString() {
		return "[" + minimum + "(" + Integer.toHexString(minimum) + ").." + maximum + "(" + Integer.toHexString(maximum) + ")]";
	}
}