package org.unicode.text.UCD;

import java.util.BitSet;

public interface NormalizationData {

    public static final int NOT_COMPOSITE = 0xFFFF;

    public abstract String getUCDVersion();

    public abstract short getCanonicalClass(int cp);

    public abstract boolean isTrailing(int cp);

    public abstract boolean isLeading(int cp);

    public abstract boolean normalizationDiffers(int cp, boolean composition, boolean compat);

    public abstract void getRecursiveDecomposition(int cp, StringBuffer buffer, boolean compat);

    public abstract int getPairwiseComposition(int starterCh, int ch);

    public abstract boolean hasCompatDecomposition(int i);

    public abstract boolean isNonSpacing(int cp);

    public abstract void getCompositionStatus(BitSet leading, BitSet trailing, BitSet resulting);
}
