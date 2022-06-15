package org.unicode.text.UCA;

import java.util.Map;
import java.util.TreeMap;

/** Maps per-one-primary secondary and tertiary UCA weights to fractional weights. */
public final class SecTerToFractional {
    // There is one instance of this class per primary weight
    // that has non-trivial secondary and/or tertiary weights.
    // We try not to allocate sub-structures for trivial data.
    //
    // If the primary weight is 00, then the secondary weights
    // may include 00 (for all tertiary CEs)
    // and above-common weights (for all secondary CEs).
    //
    // For tertiary weights we use simple int arrays.
    // While collecting the set of possible tertiary weights per primary+secondary,
    // we set simple flags into such an array.
    // Item 0 in each array counts the number of unique tertiary weights used
    // (= the number of flags set).
    // When we assign fractional weights, we replace the flags with those weights.
    //
    // A separate variable remembers the lowest tertiary UCA weight seen.
    // This is normally the NEUTRAL_TERTIARY.
    // If it is not, then the lowest actual tertiary
    // will be mapped to the common fractional weight.
    // This ensures that users of the fractional data
    // always have a common tertiary weight for every secondary weight.
    //
    // For secondary weights greater than the common weight,
    // we use a Map from the UCA secondary to a struct that stores
    // the fractional secondary (after it has been assigned)
    // and the associated tertiaries (if there are non-trivial ones).

    private static final int UCA_TERTIARY_LIMIT = UCA_Types.MAX_TERTIARY + 1;
    /**
     * If true, then we store the secondary and tertiary weights for CEs like [00, 00, tt] and [00,
     * ss, tt]. ss cannot be the common weight. {@link #commonSecTs2f} stores the tertiary weights
     * of tertiary CEs [00, 00, tt], if there are any besides 00.
     *
     * <p>If false, then we store the weights for CEs like [pp, ss, tt] (where pp!=0). ss cannot be
     * 00. {@link #commonSecTs2f} stores the tertiary weights of primary CEs like [pp, ss, tt] where
     * ss=common, if there are any besides the common tertiary.
     */
    private final boolean isPrimaryIgnorable;

    private int commonSecLowestUCATer;
    /**
     * Tertiaries for the 00 or common secondary weight. null if only 00 and common tertiary
     * weights.
     *
     * @see #isPrimaryIgnorable
     */
    private int[] commonSecTs2f;
    /** Secondaries-to-fractional. */
    private Map<Integer, SecondaryToFractional> ss2f;

    private static final class SecondaryToFractional {
        private int fractionalSecondary;
        private int lowestUCATer;
        /** Tertiaries-to-fractional. */
        private int[] ts2f;
    }

    public SecTerToFractional(boolean isPrimaryIgnorable) {
        this.isPrimaryIgnorable = isPrimaryIgnorable;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        if (commonSecTs2f != null) {
            sb.append(isPrimaryIgnorable ? "[00, 00, tt]: " : "[pp, 20:05, tt]: ");
            appendTertiaries(commonSecTs2f, sb);
            sb.append('\n');
        }
        if (ss2f != null) {
            for (final Map.Entry<Integer, SecondaryToFractional> entry : ss2f.entrySet()) {
                final SecondaryToFractional s2f = entry.getValue();
                sb.append(isPrimaryIgnorable ? "[00, " : "[pp, ");
                sb.append(Integer.toHexString(entry.getKey())).append(':');
                sb.append(Fractional.hexBytes(s2f.fractionalSecondary));
                sb.append(", tt]");
                if (s2f.ts2f != null) {
                    sb.append(": ");
                    appendTertiaries(s2f.ts2f, sb);
                }
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    private void appendTertiaries(int[] secTs2f, StringBuilder sb) {
        sb.append('[').append(secTs2f[0]).append(']');
        for (int ter = UCA_Types.NEUTRAL_TERTIARY + 1; ter < secTs2f.length; ++ter) {
            if (secTs2f[ter] != 0) {
                sb.append(' ').append(Integer.toHexString(ter)).append(':');
                sb.append(Fractional.hexBytes(secTs2f[ter]));
            }
        }
    }

    /**
     * After adding all of the secondary and tertiary weights for a primary, this method must be
     * called before using {@link #getFractionalSecAndTer(int, int)}.
     */
    public void assignFractionalWeights() {
        if (commonSecTs2f != null) {
            int numWeights = commonSecTs2f[0];
            int lowTer = -1;
            int terForcedToCommon;
            if (commonSecLowestUCATer <= UCA_Types.NEUTRAL_TERTIARY) {
                terForcedToCommon = -1;
            } else {
                int secondLowestTer = commonSecLowestUCATer + 1;
                while (secondLowestTer <= UCA_Types.MAX_TERTIARY
                        && commonSecTs2f[secondLowestTer] == 0) {
                    ++secondLowestTer;
                }
                if (secondLowestTer == UCA_Types.NORMAL_HIRAGANA_TERTIARY) {
                    // Small or compat Hiragana -> fractional below-common weight
                    // Normal Hiragana -> fractional common weight
                    numWeights -= 2;
                    lowTer = commonSecLowestUCATer;
                    commonSecTs2f[lowTer] = 0;
                    terForcedToCommon = UCA_Types.NORMAL_HIRAGANA_TERTIARY;
                } else {
                    --numWeights;
                    terForcedToCommon = commonSecLowestUCATer;
                }
            }
            Fractional.WeightIterator iter;
            if (isPrimaryIgnorable) {
                iter = Fractional.assignTertiaryWeightsForTertiaryCEs(numWeights);
            } else {
                iter = Fractional.assignTertiaryWeightsForSecondaryCEs(numWeights);
            }
            setFractionalTertiaries(commonSecTs2f, terForcedToCommon, iter);
            if (lowTer >= 0) {
                commonSecTs2f[lowTer] = Fractional.COMMON_TER - 2;
            }
        }
        if (ss2f != null) {
            int numWeights = ss2f.size();
            Fractional.WeightIterator iter;
            if (isPrimaryIgnorable) {
                iter = Fractional.assignSecondaryWeightsForSecondaryCEs(numWeights);
            } else {
                iter = Fractional.assignSecondaryWeightsForPrimaryCEs(numWeights);
            }
            for (final SecondaryToFractional s2f : ss2f.values()) {
                s2f.fractionalSecondary = iter.nextWeight();
                if (s2f.ts2f != null) {
                    numWeights = s2f.ts2f[0];
                    int terForcedToCommon;
                    if (s2f.lowestUCATer <= UCA_Types.NEUTRAL_TERTIARY) {
                        terForcedToCommon = -1;
                    } else {
                        // If this assertion fails, then add code like above
                        // for commonSecLowestUCATer & lowTer.
                        assert s2f.lowestUCATer != UCA_Types.SMALL_HIRAGANA_TERTIARY;
                        --numWeights;
                        terForcedToCommon = s2f.lowestUCATer;
                    }
                    Fractional.WeightIterator terIter;
                    terIter = Fractional.assignTertiaryWeightsForSecondaryCEs(numWeights);
                    setFractionalTertiaries(s2f.ts2f, terForcedToCommon, terIter);
                }
            }
        }
    }

    private void setFractionalTertiaries(
            int[] secTs2f, int terForcedToCommon, Fractional.WeightIterator iter) {
        for (int ter = UCA_Types.NEUTRAL_TERTIARY + 1; ter < secTs2f.length; ++ter) {
            if (ter == terForcedToCommon) {
                secTs2f[ter] = Fractional.COMMON_TER;
            } else if (secTs2f[ter] != 0) {
                secTs2f[ter] = iter.nextWeight();
            }
        }
    }

    public void addUCASecondaryAndTertiary(int sec, int ter) {
        checkUCAWeights(sec, ter);
        if (sec == 0 || sec == UCA_Types.NEUTRAL_SECONDARY) {
            if (ter != 0 && ter != UCA_Types.NEUTRAL_TERTIARY) {
                if (commonSecTs2f == null) {
                    commonSecTs2f = new int[UCA_TERTIARY_LIMIT];
                }
                if (commonSecTs2f[ter] == 0) {
                    commonSecTs2f[ter] = 1;
                    ++commonSecTs2f[0];
                }
            }
            if (commonSecLowestUCATer == 0 || ter < commonSecLowestUCATer) {
                commonSecLowestUCATer = ter;
            }
        } else {
            SecondaryToFractional s2f;
            if (ss2f == null) {
                ss2f = new TreeMap<Integer, SecondaryToFractional>();
                s2f = null;
            } else {
                s2f = ss2f.get(sec);
            }
            if (s2f == null) {
                ss2f.put(sec, s2f = new SecondaryToFractional());
            }
            if (ter != UCA_Types.NEUTRAL_TERTIARY) {
                if (s2f.ts2f == null) {
                    s2f.ts2f = new int[UCA_TERTIARY_LIMIT];
                }
                if (s2f.ts2f[ter] == 0) {
                    s2f.ts2f[ter] = 1;
                    ++s2f.ts2f[0];
                }
            }
            if (s2f.lowestUCATer == 0 || ter < s2f.lowestUCATer) {
                s2f.lowestUCATer = ter;
            }
        }
    }

    /**
     * Converts the UCA secondary & tertiary weights to fractional weights. Returns an int with the
     * fractional secondary in the upper 16 bits and the fractional tertiary in the lower 16 bits.
     */
    public int getFractionalSecAndTer(int sec, int ter) {
        checkUCAWeights(sec, ter);
        int[] secTs2f;
        if (sec == 0) {
            secTs2f = commonSecTs2f;
        } else if (sec == UCA_Types.NEUTRAL_SECONDARY) {
            sec = Fractional.COMMON_SEC;
            secTs2f = commonSecTs2f;
        } else {
            final SecondaryToFractional s2f = ss2f.get(sec);
            sec = s2f.fractionalSecondary;
            secTs2f = s2f.ts2f;
        }
        if (ter == 0) {
            // pass
        } else if (ter == UCA_Types.NEUTRAL_TERTIARY) {
            ter = Fractional.COMMON_TER;
        } else {
            ter = secTs2f[ter];
        }
        return (sec << 16) | ter;
    }

    private void checkUCAWeights(int sec, int ter) {
        if (isPrimaryIgnorable) {
            if (sec == 0) {
                // [00, 00, 00] or [00, 00, tt]
                assert ter == 0 || ter > UCA_Types.NEUTRAL_TERTIARY;
            } else {
                // [00, ss, tt]
                assert sec > UCA_Types.NEUTRAL_SECONDARY;
                assert ter >= UCA_Types.NEUTRAL_TERTIARY;
            }
        } else {
            // [pp, ss, tt]
            assert sec >= UCA_Types.NEUTRAL_SECONDARY;
            assert ter >= UCA_Types.NEUTRAL_TERTIARY;
        }
    }
}
