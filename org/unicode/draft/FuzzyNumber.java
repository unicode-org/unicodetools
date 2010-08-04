package org.unicode.draft;


import java.text.NumberFormat;

public class FuzzyNumber {
  final double value;
  final double lower;
  final double upper;

  public FuzzyNumber(double value, double variance) {
    this.value = value;
    if (variance < 0.0D) {
      variance = -variance;
    }
    lower = value - variance;
    upper = value + variance;
  }

  FuzzyNumber(double value, double lower, double upper) {
    this.value = value;
    this.lower = lower;
    this.upper = upper;
  }

  public static FuzzyNumber parse(String in) {
    in = in.trim();
    double value = Double.parseDouble(in);
    int decimalPos = in.indexOf('.');
    double increment = decimalPos < 0 || decimalPos == in.length() - 1 ? 0.5 : 0.5 / Math.pow(10, in.length() - decimalPos - 1);
    return new FuzzyNumber(value, increment);
  }

  public FuzzyNumber add(FuzzyNumber other) {
    return new FuzzyNumber(value + other.value, lower + other.lower, upper + other.upper);
  }

  public FuzzyNumber negate() {
    return new FuzzyNumber(-value, -upper, -lower);
  }

  public FuzzyNumber subtract(FuzzyNumber other) {
    return new FuzzyNumber(value - other.value, lower - other.upper, upper - other.lower);
  }

  public FuzzyNumber multiply(FuzzyNumber other) {
    // optimize later
    double a = lower * other.lower;
    double b = lower * other.upper;
    double c = upper * other.lower;
    double d = upper * other.upper;
    double min = Math.min(a, Math.min(b, Math.min(c, d)));
    double max = Math.max(a, Math.max(b, Math.max(c, d)));
    return new FuzzyNumber(value * other.value, min, max);
  }

  public FuzzyNumber invert() {
    // TODO test NAN
    if (lower > 0) {
      return new FuzzyNumber(1.0D / value, 1.0D / upper, 1.0D / lower);
    } else if (lower < 0) {
      if (upper < 0) {
        return new FuzzyNumber(1.0D / value, 1.0D / upper, 1.0D / lower);
      } else {
        return new FuzzyNumber(1.0D / value, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
      }
    } else { // == 0 or NaN
      return new FuzzyNumber(1.0D / value, 1.0D / upper, Double.POSITIVE_INFINITY);
    }
  }

  public FuzzyNumber divide(FuzzyNumber other) {
    return multiply(other.invert());
  }

  static NumberFormat nf = NumberFormat.getInstance();
  static NumberFormat pf = NumberFormat.getPercentInstance();

  public String toString() {
    return nf.format(value) + " {-" + pf.format(Math.abs((value - lower) / value)) + "+" + pf.format(Math.abs((upper - value) / value)) + "}";
  }

}
