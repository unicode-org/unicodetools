package org.unicode.jsp;

/*
 * (C) Copyright IBM Corp. 1999, All Rights Reserved
 *
 * version 1.1
 */

/**
 * Reference implementation of the Unicode 3.0 Bidi algorithm.
 *
 * <p>
 * This implementation is not optimized for performance.  It is intended
 * as a reference implementation that closely follows the specification
 * of the Bidirectional Algorithm in The Unicode Standard version 3.0.
 * <p>
 * <b>Input:</b><br>
 * There are two levels of input to the algorithm, since clients may prefer
 * to supply some information from out-of-band sources rather than relying on
 * the default behavior.
 * <ol>
 * <li>unicode type array
 * <li>unicode type array, with externally supplied base line direction
 * </ol>
 * <p><b>Output:</b><br>
 * Output is separated into several stages as well, to better enable clients
 * to evaluate various aspects of implementation conformance.
 * <ol>
 * <li>levels array over entire paragraph
 * <li>reordering array over entire paragraph
 * <li>levels array over line
 * <li>reordering array over line
 * </ol>
 * Note that for conformance, algorithms are only required to generate correct
 * reordering and character directionality (odd or even levels) over a line.
 * Generating identical level arrays over a line is not required.  Bidi
 * explicit format codes (LRE, RLE, LRO, RLO, PDF) and BN can be assigned
 * arbitrary levels and positions as long as the other text matches.
 * <p>
 * As the algorithm is defined to operate on a single paragraph at a time,
 * this implementation is written to handle single paragraphs.  Thus
 * rule P1 is presumed by this implementation-- the data provided to the
 * implementation is assumed to be a single paragraph, and either contains no
 * 'B' codes, or a single 'B' code at the end of the input.  'B' is allowed
 * as input to illustrate how the algorithm assigns it a level.
 * <p>
 * Also note that rules L3 and L4 depend on the rendering engine that uses
 * the result of the bidi algorithm.  This implementation assumes that the
 * rendering engine expects combining marks in visual order (e.g. to the
 * left of their base character in RTL runs) and that it adjust the glyphs
 * used to render mirrored characters that are in RTL runs so that they
 * render appropriately.
 *
 * @author Doug Felt
 */

public final class BidiReference {
    private final byte[] initialTypes;
    private byte[] embeddings; // generated from processing format codes
    private byte paragraphEmbeddingLevel = -1; // undefined

    private int textLength; // for convenience
    private byte[] resultTypes; // for paragraph, not lines
    private byte[] resultLevels; // for paragraph, not lines
    private StringBuffer[] record;
    private String rule;
    private int[] mapToOriginal;

    // The bidi types

    /** Left-to-right*/
    public static final byte L = 0;

    /** Left-to-Right Embedding */
    public static final byte LRE = 1;

    /** Left-to-Right Override */
    public static final byte LRO = 2;

    /** Right-to-Left */
    public static final byte R = 3;

    /** Right-to-Left Arabic */
    public static final byte AL = 4;

    /** Right-to-Left Embedding */
    public static final byte RLE = 5;

    /** Right-to-Left Override */
    public static final byte RLO = 6;

    /** Pop Directional Format */
    public static final byte PDF = 7;

    /** European Number */
    public static final byte EN = 8;

    /** European Number Separator */
    public static final byte ES = 9;

    /** European Number Terminator */
    public static final byte ET = 10;

    /** Arabic Number */
    public static final byte AN = 11;

    /** Common Number Separator */
    public static final byte CS = 12;

    /** Non-Spacing Mark */
    public static final byte NSM = 13;

    /** Boundary Neutral */
    public static final byte BN = 14;

    /** Paragraph Separator */
    public static final byte B = 15;

    /** Segment Separator */
    public static final byte S = 16;

    /** Whitespace */
    public static final byte WS = 17;

    /** Other Neutrals */
    public static final byte ON = 18;

    /** Minimum bidi type value. */
    public static final byte TYPE_MIN = 0;

    /** Maximum bidi type value. */
    public static final byte TYPE_MAX = 18;

    /** Shorthand names of bidi type values, for error reporting. */
    public static final String[] typenames = {
        "L",
        "LRE",
        "LRO",
        "R",
        "AL",
        "RLE",
        "RLO",
        "PDF",
        "EN",
        "ES",
        "ET",
        "AN",
        "CS",
        "NSM",
        "BN",
        "B",
        "S",
        "WS",
        "ON",
    };

    //
    // Input
    //

    /**
     * Initialize using an array of direction types.  Types range from TYPE_MIN to TYPE_MAX inclusive
     * and represent the direction codes of the characters in the text.
     *
     * @param types the types array
     */
    public BidiReference(byte[] types) {
        validateTypes(types);

        initialTypes = types.clone(); // client type array remains unchanged

        runAlgorithm();
    }

    /**
     * Initialize using an array of direction types and an externally supplied paragraph embedding level.
     * The embedding level may be -1, 0, or 1.  -1 means to apply the default algorithm (rules P2 and P3),
     * 0 is for LTR paragraphs, and 1 is for RTL paragraphs.
     *
     * @param types the types array
     * @param paragraphEmbeddingLevel the externally supplied paragraph embedding level.
     */
    public BidiReference(byte[] types, byte paragraphEmbeddingLevel) {
        validateTypes(types);
        validateParagraphEmbeddingLevel(paragraphEmbeddingLevel);

        initialTypes = types.clone(); // client type array remains unchanged
        this.paragraphEmbeddingLevel = paragraphEmbeddingLevel;

        runAlgorithm();
    }

    /**
     * The algorithm.
     * Does not include line-based processing (Rules L1, L2).
     * These are applied later in the line-based phase of the algorithm.
     */
    private void runAlgorithm() {
        // Ensure trace hook does not change while running algorithm.
        // Trace hook is a shared class resource.
        synchronized (BidiReference.class) {
            textLength = initialTypes.length;

            // Initialize output types.
            // Result types initialized to input types.
            resultTypes = initialTypes.clone();
            record = new StringBuffer[resultTypes.length];
            for (int i = 0; i < resultTypes.length; ++i) {
                record[i] = new StringBuffer();
            }

            trace(BidiTraceHook.PHASE_INIT, 0, textLength);

            // 1) determining the paragraph level
            // Rule P1 is the requirement for entering this algorithm.
            // Rules P2, P3.
            // If no externally supplied paragraph embedding level, use default.
            setRule("P1");
            if (paragraphEmbeddingLevel == -1) {
                determineParagraphEmbeddingLevel();
            }

            // Initialize result levels to paragraph embedding level.
            setRule("P1");
            resultLevels = new byte[textLength];
            setLevels(0, textLength, paragraphEmbeddingLevel);
            trace(BidiTraceHook.PHASE_BASELEVEL, 0, textLength);

            // 2) Explicit levels and directions
            // Rules X1-X8.\
            setRule("X1-8");
            determineExplicitEmbeddingLevels();
            trace(BidiTraceHook.PHASE_EXPLICIT, 0, textLength);

            // Rule X9.
            setRule("X9");
            textLength = removeExplicitCodes();
            trace(BidiTraceHook.PHASE_EXPLICIT_REMOVED, 0, textLength);

            // Rule X10.
            // Run remainder of algorithm one level run at a time
            setRule("X10");
            byte prevLevel = paragraphEmbeddingLevel;
            int start = 0;
            while (start < textLength) {
                final byte level = resultLevels[start];
                final byte prevType = typeForLevel(Math.max(prevLevel, level));

                int limit = start + 1;
                while (limit < textLength && resultLevels[limit] == level) {
                    ++limit;
                }

                final byte succLevel = limit < textLength ? resultLevels[limit] : paragraphEmbeddingLevel;
                final byte succType = typeForLevel(Math.max(succLevel, level));

                // 3) resolving weak types
                // Rules W1-W7.
                setRule("W1-7");
                resolveWeakTypes(start, limit, level, prevType, succType);
                trace(BidiTraceHook.PHASE_WEAK, start, limit);

                // 4) resolving neutral types
                // Rules N1-N3.
                setRule("N1-2");
                resolveNeutralTypes(start, limit, level, prevType, succType);
                trace(BidiTraceHook.PHASE_NEUTRAL, start, limit);

                // 5) resolving implicit embedding levels
                // Rules I1, I2.
                setRule("I1-2");
                resolveImplicitLevels(start, limit, level, prevType, succType);
                trace(BidiTraceHook.PHASE_IMPLICIT, start, limit);

                prevLevel = level;
                start = limit;
            }
        }

        // Reinsert explicit codes and assign appropriate levels to 'hide' them.
        // This is for convenience, so the resulting level array maps 1-1
        // with the initial array.
        // See the implementation suggestions section of TR#9 for guidelines on
        // how to implement the algorithm without removing and reinserting the codes.
        textLength = reinsertExplicitCodes(textLength);
    }

    /**
     * 1) determining the paragraph level.
     * <p>
     * Rules P2, P3.
     * <p>
     * At the end of this function, the member variable paragraphEmbeddingLevel is set to either 0 or 1.
     */
    private void determineParagraphEmbeddingLevel() {
        byte strongType = -1; // unknown

        // Rule P2.
        for (int i = 0; i < textLength; ++i) {
            final byte t = resultTypes[i];
            if (t == L || t == AL || t == R) {
                strongType = t;
                break;
            }
        }

        // Rule P3.
        if (strongType == -1) { // none found
            // default embedding level when no strong types found is 0.
            paragraphEmbeddingLevel = 0;
        } else if (strongType == L) {
            paragraphEmbeddingLevel = 0;
        } else { // AL, R
            paragraphEmbeddingLevel = 1;
        }
    }

    /**
     * Process embedding format codes.
     * <p>
     * Calls processEmbeddings to generate an embedding array from the explicit format codes.  The
     * embedding overrides in the array are then applied to the result types, and the result levels are
     * initialized.
     * @see #processEmbeddings
     */
    private void determineExplicitEmbeddingLevels() {
        embeddings = processEmbeddings(resultTypes, paragraphEmbeddingLevel);

        for (int i = 0; i < textLength; ++i) {
            byte level = embeddings[i];
            if ((level & 0x80) != 0) {
                level &= 0x7f;
                setType(i, typeForLevel(level));
            }
            resultLevels[i] = level;
        }
    }

    private void setType(int i, byte value) {
        if (value != resultTypes[i]) {
            record[i].append(getRule() + "\u2192"+getHtmlTypename(value) + "\n");
        }
        resultTypes[i] = value;
    }

    public String getChanges(int i) {
        return record[i].toString();
    }

    /**
     * Rules X9.
     * Remove explicit codes so that they may be ignored during the remainder
     * of the main portion of the algorithm.  The length of the resulting text
     * is returned.
     * @return the length of the data excluding explicit codes and BN.
     */
    private int removeExplicitCodes() {
        int w = 0;
        mapToOriginal = new int[initialTypes.length];
        for (int i = 0; i < textLength; ++i) {
            final byte t = initialTypes[i];
            if (!(t == LRE || t == RLE || t == LRO || t == RLO || t == PDF || t == BN)) {
                mapToOriginal[w] = i;
                embeddings[w] = embeddings[i];
                resultTypes[w] = resultTypes[i];
                resultLevels[w] = resultLevels[i];
                w++;
            }
        }
        return w; // new textLength while explicit levels are removed
    }

    /**
     * Reinsert levels information for explicit codes.
     * This is for ease of relating the level information
     * to the original input data.  Note that the levels
     * assigned to these codes are arbitrary, they're
     * chosen so as to avoid breaking level runs.
     * @param textLength the length of the data after compression
     * @return the length of the data (original length of
     * types array supplied to constructor)
     */
    private int reinsertExplicitCodes(int textLength) {
        for (int i = initialTypes.length; --i >= 0;) {
            final byte t = initialTypes[i];
            if (t == LRE || t == RLE || t == LRO || t == RLO || t == PDF || t == BN) {
                embeddings[i] = 0;
                setType(i, t);
                resultLevels[i] = -1;
            } else {
                --textLength;
                embeddings[i] = embeddings[textLength];
                setType(i, resultTypes[textLength]);
                resultLevels[i] = resultLevels[textLength];
            }
        }
        mapToOriginal = null;

        // now propagate forward the levels information (could have
        // propagated backward, the main thing is not to introduce a level
        // break where one doesn't already exist).

        if (resultLevels[0] == -1) {
            resultLevels[0] = paragraphEmbeddingLevel;
        }
        for (int i = 1; i < initialTypes.length; ++i) {
            if (resultLevels[i] == -1) {
                resultLevels[i] = resultLevels[i-1];
            }
        }

        // Embedding information is for informational purposes only
        // so need not be adjusted.

        return initialTypes.length;
    }

    /**
     * 2) determining explicit levels
     * Rules X1 - X8
     *
     * The interaction of these rules makes handling them a bit complex.
     * This examines resultTypes but does not modify it.  It returns embedding and
     * override information in the result array.  The low 7 bits are the level, the high
     * bit is set if the level is an override, and clear if it is an embedding.
     */
    private static byte[] processEmbeddings(byte[] resultTypes, byte paragraphEmbeddingLevel) {
        final int EXPLICIT_LEVEL_LIMIT = 62;

        final int textLength = resultTypes.length;
        final byte[] embeddings = new byte[textLength];

        // This stack will store the embedding levels and override status in a single byte
        // as described above.
        final byte[] embeddingValueStack = new byte[EXPLICIT_LEVEL_LIMIT];
        int stackCounter = 0;

        // An LRE or LRO at level 60 is invalid, since the new level 62 is invalid.  But
        // an RLE at level 60 is valid, since the new level 61 is valid.  The current wording
        // of the rules requires that the RLE remain valid even if a previous LRE is invalid.
        // This keeps track of ignored LRE or LRO codes at level 60, so that the matching PDFs
        // will not try to pop the stack.
        int overflowAlmostCounter = 0;

        // This keeps track of ignored pushes at level 61 or higher, so that matching PDFs will
        // not try to pop the stack.
        int overflowCounter = 0;

        // Rule X1.

        // Keep the level separate from the value (level | override status flag) for ease of access.
        byte currentEmbeddingLevel = paragraphEmbeddingLevel;
        byte currentEmbeddingValue = paragraphEmbeddingLevel;

        // Loop through types, handling all remaining rules
        for (int i = 0; i < textLength; ++i) {

            embeddings[i] = currentEmbeddingValue;

            final byte t = resultTypes[i];

            // Rules X2, X3, X4, X5
            switch (t) {
            case RLE:
            case LRE:
            case RLO:
            case LRO:
                // Only need to compute new level if current level is valid
                if (overflowCounter == 0) {
                    byte newLevel;
                    if (t == RLE || t == RLO) {
                        newLevel = (byte)((currentEmbeddingLevel + 1) | 1); // least greater odd
                    } else { // t == LRE || t == LRO
                        newLevel = (byte)((currentEmbeddingLevel + 2) & ~1); // least greater even
                    }

                    // If the new level is valid, push old embedding level and override status
                    // No check for valid stack counter, since the level check suffices.
                    if (newLevel < EXPLICIT_LEVEL_LIMIT) {
                        embeddingValueStack[stackCounter] = currentEmbeddingValue;
                        stackCounter++;

                        currentEmbeddingLevel = newLevel;
                        if (t == LRO || t == RLO) { // override
                            currentEmbeddingValue = (byte)(newLevel | 0x80);
                        } else {
                            currentEmbeddingValue = newLevel;
                        }

                        // Adjust level of format mark (for expositional purposes only, this gets
                        // removed later).
                        embeddings[i] = currentEmbeddingValue;
                        break;
                    }

                    // Otherwise new level is invalid, but a valid level can still be achieved if this
                    // level is 60 and we encounter an RLE or RLO further on.  So record that we
                    // 'almost' overflowed.
                    if (currentEmbeddingLevel == 60) {
                        overflowAlmostCounter++;
                        break;
                    }
                }

                // Otherwise old or new level is invalid.
                overflowCounter++;
                break;

            case PDF:
                // The only case where this did not actually overflow but may have almost overflowed
                // is when there was an RLE or RLO on level 60, which would result in level 61.  So we
                // only test the almost overflow condition in that case.
                //
                // Also note that there may be a PDF without any pushes at all.

                if (overflowCounter > 0) {
                    --overflowCounter;
                } else if (overflowAlmostCounter > 0 && currentEmbeddingLevel != 61) {
                    --overflowAlmostCounter;
                } else if (stackCounter > 0) {
                    --stackCounter;
                    currentEmbeddingValue = embeddingValueStack[stackCounter];
                    currentEmbeddingLevel = (byte)(currentEmbeddingValue & 0x7f);
                }
                break;

            case B:
                // Rule X8.

                // These values are reset for clarity, in this implementation B can only
                // occur as the last code in the array.
                stackCounter = 0;
                overflowCounter = 0;
                overflowAlmostCounter = 0;
                currentEmbeddingLevel = paragraphEmbeddingLevel;
                currentEmbeddingValue = paragraphEmbeddingLevel;

                embeddings[i] = paragraphEmbeddingLevel;
                break;

            default:
                break;
            }
        }

        return embeddings;
    }


    /**
     * 3) resolving weak types
     * Rules W1-W7.
     *
     * Note that some weak types (EN, AN) remain after this processing is complete.
     */
    private void resolveWeakTypes(int start, int limit, byte level, byte sor, byte eor) {

        // on entry, only these types remain
        assertOnly(start, limit, new byte[] {L, R, AL, EN, ES, ET, AN, CS, B, S, WS, ON, NSM });

        // Rule W1.
        // Changes all NSMs.
        setRule("W1");
        byte preceedingCharacterType = sor;
        for (int i = start; i < limit; ++i) {
            final byte t = resultTypes[i];
            if (t == NSM) {
                setType(i, preceedingCharacterType);
            } else {
                preceedingCharacterType = t;
            }
        }

        // Rule W2.
        // EN does not change at the start of the run, because sor != AL.
        setRule("W2");
        for (int i = start; i < limit; ++i) {
            if (resultTypes[i] == EN) {
                for (int j = i - 1; j >= start; --j) {
                    final byte t = resultTypes[j];
                    if (t == L || t == R || t == AL) {
                        if (t == AL) {
                            setType(i, AN);
                        }
                        break;
                    }
                }
            }
        }

        // Rule W3.
        setRule("W3");
        for (int i = start; i < limit; ++i) {
            if (resultTypes[i] == AL) {
                setType(i, R);
            }
        }

        // Rule W4.
        // Since there must be values on both sides for this rule to have an
        // effect, the scan skips the first and last value.
        //
        // Although the scan proceeds left to right, and changes the type values
        // in a way that would appear to affect the computations later in the scan,
        // there is actually no problem.  A change in the current value can only
        // affect the value to its immediate right, and only affect it if it is
        // ES or CS.  But the current value can only change if the value to its
        // right is not ES or CS.  Thus either the current value will not change,
        // or its change will have no effect on the remainder of the analysis.

        setRule("W4");
        for (int i = start + 1; i < limit - 1; ++i) {
            if (resultTypes[i] == ES || resultTypes[i] == CS) {
                final byte prevSepType = resultTypes[i-1];
                final byte succSepType = resultTypes[i+1];
                if (prevSepType == EN && succSepType == EN) {
                    setType(i, EN);
                } else if (resultTypes[i] == CS && prevSepType == AN && succSepType == AN) {
                    setType(i, AN);
                }
            }
        }

        // Rule W5.
        setRule("W5");
        for (int i = start; i < limit; ++i) {
            if (resultTypes[i] == ET) {
                // locate end of sequence
                final int runstart = i;
                final int runlimit = findRunLimit(runstart, limit, new byte[] { ET });

                // check values at ends of sequence
                byte t = runstart == start ? sor : resultTypes[runstart - 1];

                if (t != EN) {
                    t = runlimit == limit ? eor : resultTypes[runlimit];
                }

                if (t == EN) {
                    setTypes(runstart, runlimit, EN);
                }

                // continue at end of sequence
                i = runlimit;
            }
        }

        // Rule W6.
        setRule("W6");
        for (int i = start; i < limit; ++i) {
            final byte t = resultTypes[i];
            if (t == ES || t == ET || t == CS) {
                setType(i, ON);
            }
        }

        // Rule W7.
        setRule("W7");
        for (int i = start; i < limit; ++i) {
            if (resultTypes[i] == EN) {
                // set default if we reach start of run
                byte prevStrongType = sor;
                for (int j = i - 1; j >= start; --j) {
                    final byte t = resultTypes[j];
                    if (t == L || t == R) { // AL's have been removed
                        prevStrongType = t;
                        break;
                    }
                }
                if (prevStrongType == L) {
                    setType(i, L);
                }
            }
        }
    }

    /**
     * 6) resolving neutral types
     * Rules N1-N2.
     */
    private void resolveNeutralTypes(int start, int limit, byte level, byte sor, byte eor) {

        // on entry, only these types can be in resultTypes
        assertOnly(start, limit, new byte[] {L, R, EN, AN, B, S, WS, ON});

        for (int i = start; i < limit; ++i) {
            final byte t = resultTypes[i];
            if (t == WS || t == ON || t == B || t == S) {
                // find bounds of run of neutrals
                final int runstart = i;
                final int runlimit = findRunLimit(runstart, limit, new byte[] {B, S, WS, ON});

                // determine effective types at ends of run
                byte leadingType;
                byte trailingType;

                if (runstart == start) {
                    leadingType = sor;
                } else {
                    leadingType = resultTypes[runstart - 1];
                    if (leadingType == L || leadingType == R) {
                        // found the strong type
                    } else if (leadingType == AN) {
                        leadingType = R;
                    } else if (leadingType == EN) {
                        // Since EN's with previous strong L types have been changed
                        // to L in W7, the leadingType must be R.
                        leadingType = R;
                    }
                }

                if (runlimit == limit) {
                    trailingType = eor;
                } else {
                    trailingType = resultTypes[runlimit];
                    if (trailingType == L || trailingType == R) {
                        // found the strong type
                    } else if (trailingType == AN) {
                        trailingType = R;
                    } else if (trailingType == EN) {
                        trailingType = R;
                    }
                }

                byte resolvedType;
                if (leadingType == trailingType) {
                    // Rule N1.
                    setRule("N1");
                    resolvedType = leadingType;
                } else {
                    // Rule N2.
                    // Notice the embedding level of the run is used, not
                    // the paragraph embedding level.
                    setRule("N2");
                    resolvedType = typeForLevel(level);
                }

                setTypes(runstart, runlimit, resolvedType);

                // skip over run of (former) neutrals
                i = runlimit;
            }
        }
    }

    /**
     * 7) resolving implicit embedding levels
     * Rules I1, I2.
     */
    private void resolveImplicitLevels(int start, int limit, byte level, byte sor, byte eor) {

        // on entry, only these types can be in resultTypes
        assertOnly(start, limit, new byte[] {L, R, EN, AN});

        if ((level & 1) == 0) { // even level
            for (int i = start; i < limit; ++i) {
                final byte t = resultTypes[i];
                // Rule I1.
                setRule("I1");
                if (t == L ) {
                    // no change
                } else if (t == R) {
                    resultLevels[i] += 1;
                } else { // t == AN || t == EN
                    resultLevels[i] += 2;
                }
            }
        } else { // odd level
            for (int i = start; i < limit; ++i) {
                final byte t = resultTypes[i];
                // Rule I2.
                setRule("I2");
                if (t == R) {
                    // no change
                } else { // t == L || t == AN || t == EN
                    resultLevels[i] += 1;
                }
            }
        }
    }

    //
    // Output
    //

    /**
     * Return levels array breaking lines at offsets in linebreaks. <br>
     * Rule L1.
     * <p>
     * The returned levels array contains the resolved level for each
     * bidi code passed to the constructor.
     * <p>
     * The linebreaks array must include at least one value.
     * The values must be in strictly increasing order (no duplicates)
     * between 1 and the length of the text, inclusive.  The last value
     * must be the length of the text.
     *
     * @param linebreaks the offsets at which to break the paragraph
     * @return the resolved levels of the text
     */
    public byte[] getLevels(int[] linebreaks) {

        // Note that since the previous processing has removed all
        // P, S, and WS values from resultTypes, the values referred to
        // in these rules are the initial types, before any processing
        // has been applied (including processing of overrides).
        //
        // This example implementation has reinserted explicit format codes
        // and BN, in order that the levels array correspond to the
        // initial text.  Their final placement is not normative.
        // These codes are treated like WS in this implementation,
        // so they don't interrupt sequences of WS.

        validateLineBreaks(linebreaks, textLength);

        final byte[] result = resultLevels.clone(); // will be returned to caller

        // don't worry about linebreaks since if there is a break within
        // a series of WS values preceeding S, the linebreak itself
        // causes the reset.
        for (int i = 0; i < result.length; ++i) {
            final byte t = initialTypes[i];
            if (t == B || t == S) {
                // Rule L1, clauses one and two.
                result[i] = paragraphEmbeddingLevel;

                // Rule L1, clause three.
                for (int j = i - 1; j >= 0; --j) {
                    if (isWhitespace(initialTypes[j])) { // including format codes
                        result[j] = paragraphEmbeddingLevel;
                    } else {
                        break;
                    }
                }
            }
        }

        // Rule L1, clause four.
        int start = 0;
        for (final int limit : linebreaks) {
            for (int j = limit - 1; j >= start; --j) {
                if (isWhitespace(initialTypes[j])) { // including format codes
                    result[j] = paragraphEmbeddingLevel;
                } else {
                    break;
                }
            }

            start = limit;
        }

        traceLineLevels(linebreaks, result);

        return result;
    }

    /**
     * Return reordering array breaking lines at offsets in linebreaks.
     * <p>
     * The reordering array maps from a visual index to a logical index.
     * Lines are concatenated from left to right.  So for example, the
     * fifth character from the left on the third line is
     * <pre> getReordering(linebreaks)[linebreaks[1] + 4]</pre>
     * (linebreaks[1] is the position after the last character of the
     * second line, which is also the index of the first character on the
     * third line, and adding four gets the fifth character from the left).
     * <p>
     * The linebreaks array must include at least one value.
     * The values must be in strictly increasing order (no duplicates)
     * between 1 and the length of the text, inclusive.  The last value
     * must be the length of the text.
     *
     * @param linebreaks the offsets at which to break the paragraph.
     */
    public int[] getReordering(int[] linebreaks) {
        validateLineBreaks(linebreaks, textLength);

        final byte[] levels = getLevels(linebreaks);

        return computeMultilineReordering(levels, linebreaks);
    }

    /**
     * Return multiline reordering array for a given level array.
     * Reordering does not occur across a line break.
     */
    private static int[] computeMultilineReordering(byte[] levels, int[] linebreaks) {
        final int[] result = new int[levels.length];

        int start = 0;
        for (final int limit : linebreaks) {
            final byte[] templevels = new byte[limit - start];
            System.arraycopy(levels, start, templevels, 0, templevels.length);

            final int[] temporder = computeReordering(templevels);
            for (int j = 0; j < temporder.length; ++j) {
                result[start + j] = temporder[j] + start;
            }

            start = limit;
        }

        return result;
    }

    /**
     * Return reordering array for a given level array.  This reorders a single line.
     * The reordering is a visual to logical map.  For example,
     * the leftmost char is string.charAt(order[0]).
     * Rule L2.
     */
    private static int[] computeReordering(byte[] levels) {
        final int lineLength = levels.length;

        final int[] result = new int[lineLength];

        // initialize order
        for (int i = 0; i < lineLength; ++i) {
            result[i] = i;
        }

        // locate highest level found on line.
        // Note the rules say text, but no reordering across line bounds is performed,
        // so this is sufficient.
        byte highestLevel = 0;
        byte lowestOddLevel = 63;
        for (int i = 0; i < lineLength; ++i) {
            final byte level = levels[i];
            if (level > highestLevel) {
                highestLevel = level;
            }
            if (((level & 1) != 0) && level < lowestOddLevel) {
                lowestOddLevel = level;
            }
        }

        for (int level = highestLevel; level >= lowestOddLevel; --level) {
            for (int i = 0; i < lineLength; ++i) {
                if (levels[i] >= level) {
                    // find range of text at or above this level
                    final int start = i;
                    int limit = i + 1;
                    while (limit < lineLength && levels[limit] >= level) {
                        ++limit;
                    }

                    // reverse run
                    for (int j = start, k = limit - 1; j < k; ++j, --k) {
                        final int temp = result[j];
                        result[j] = result[k];
                        result[k] = temp;
                    }

                    // skip to end of level run
                    i = limit;
                }
            }
        }

        return result;
    }

    /**
     * Return the base level of the paragraph.
     */
    public byte getBaseLevel() {
        return paragraphEmbeddingLevel;
    }

    // --- internal utilities -------------------------------------------------

    /**
     * Return true if the type is considered a whitespace type for the line break rules.
     */
    private static boolean isWhitespace(byte biditype) {
        switch (biditype) {
        case LRE:
        case RLE:
        case LRO:
        case RLO:
        case PDF:
        case BN:
        case WS:
            return true;
        default:
            return false;
        }
    }

    /**
     * Return the strong type (L or R) corresponding to the level.
     */
    private static byte typeForLevel(int level) {
        return ((level & 0x1) == 0) ? L : R;
    }

    /**
     * Return the limit of the run starting at index that includes only resultTypes in validSet.
     * This checks the value at index, and will return index if that value is not in validSet.
     */
    private int findRunLimit(int index, int limit, byte[] validSet) {
        --index;
        loop:
            while (++index < limit) {
                final byte t = resultTypes[index];
                for (int i = 0; i < validSet.length; ++i) {
                    if (t == validSet[i]) {
                        continue loop;
                    }
                }
                // didn't find a match in validSet
                return index;
            }
        return limit;
    }

    /**
     * Return the start of the run including index that includes only resultTypes in validSet.
     * This assumes the value at index is valid, and does not check it.
     */
    private int findRunStart(int index, byte[] validSet) {
        loop:
            while (--index >= 0) {
                final byte t = resultTypes[index];
                for (int i = 0; i < validSet.length; ++i) {
                    if (t == validSet[i]) {
                        continue loop;
                    }
                }
                return index + 1;
            }
    return 0;
    }

    /**
     * Set resultTypes from start up to (but not including) limit to newType.
     */
    private void setTypes(int start, int limit, byte newType) {
        for (int i = start; i < limit; ++i) {
            setType(i, newType);
        }
    }

    /**
     * Set resultLevels from start up to (but not including) limit to newLevel.
     */
    private void setLevels(int start, int limit, byte newLevel) {
        for (int i = start; i < limit; ++i) {
            resultLevels[i] = newLevel;
        }
    }

    // --- algorithm internal validation --------------------------------------

    /**
     * Algorithm validation.
     * Assert that all values in resultTypes are in the provided set.
     */
    private void assertOnly(int start, int limit, byte[] codes) {
        loop:
            for (int i = start; i < limit; ++i) {
                final byte t = resultTypes[i];
                for (int j = 0; j < codes.length; ++j) {
                    if (t == codes[j]) {
                        continue loop;
                    }
                }

                throw new Error("invalid bidi code " + getHtmlTypename(t) + " present in assertOnly at position " + i);
            }
    }

    // --- input validation ---------------------------------------------------

    /**
     * Throw exception if type array is invalid.
     */
    private static void validateTypes(byte[] types) {
        if (types == null) {
            throw new IllegalArgumentException("types is null");
        }
        for (int i = 0; i < types.length; ++i) {
            if (types[i] < TYPE_MIN || types[i] > TYPE_MAX) {
                throw new IllegalArgumentException("illegal type value at " + i + ": " + types[i]);
            }
        }
        for (int i = 0; i < types.length - 1; ++i) {
            if (types[i] == B) {
                throw new IllegalArgumentException("B type before end of paragraph at index: " + i);
            }
        }
    }

    /**
     * Throw exception if paragraph embedding level is invalid. Special allowance for -1 so that
     * default processing can still be performed when using this API.
     */
    private static void validateParagraphEmbeddingLevel(byte paragraphEmbeddingLevel) {
        if (paragraphEmbeddingLevel != -1 &&
                paragraphEmbeddingLevel != 0 &&
                paragraphEmbeddingLevel != 1) {
            throw new IllegalArgumentException("illegal paragraph embedding level: " + paragraphEmbeddingLevel);
        }
    }

    /**
     * Throw exception if line breaks array is invalid.
     */
    private static void validateLineBreaks(int[] linebreaks, int textLength) {
        int prev = 0;
        for (int i = 0; i < linebreaks.length; ++i) {
            final int next = linebreaks[i];
            if (next <= prev) {
                throw new IllegalArgumentException("bad linebreak: " + next + " at index: " + i);
            }
            prev = next;
        }
        if (prev != textLength) {
            throw new IllegalArgumentException("last linebreak must be at " + textLength);
        }
    }

    // --- debug utilities ----------------------------------------------------

    /**
     * An interface for tracing the progress of the Bidi reference implementation.
     */
    public static interface BidiTraceHook {

        /**
         * Display the current state of the implementation.
         * <p>
         * The data supplied to the display method represents the current internal state of the implementation.  Note
         * that some phases of the algorithm operate on the data as it appears when the explicit formatting codes and
         * BN have been removed.  When this is the case, start and limit do not correspond directly to the original
         * direction type codes that were passed to the constructor.  However, the values in embeddings, resultTypes,
         * and resultLevels are consistent.
         * <p>
         * @param phase the current phase of the algorithm
         * @param start the start of the run of text being worked on
         * @param limit the limit of the run of text being worked on
         * @param paragraphEmbeddingLevel the paragraph embedding level
         * @param initialTypes the original bidi types provided to the constructor
         * @param embeddings the embeddings and override information resulting from explicit formatting codes
         * @param resultTypes the current resolved bidi types
         * @param resultLevels the current resolved levels (assuming the paragraph is a single line)
         */
        public abstract void display(int phase,
                int start, int limit,
                byte paragraphEmbeddingLevel,
                byte[] initialTypes,
                byte[] embeddings,
                byte[] resultTypes,
                byte[] resultLevels);

        /**
         * Display the results of processing line break information to generate line levels.
         * <p>
         * @param paragraphEmbeddingLevel the paragraph embedding level
         * @param initialTypes the original bidi types provided to the constructor
         * @param embeddings the embeddings and override information resulting from explicit formatting codes
         * @param linebreaks the array of positions where line breaks occur
         * @param resolvedLevels the resolved levels before line processing is performed
         * @param lineLevels the levels after line processing was performed
         */
        public abstract void displayLineLevels(byte paragraphEmbeddingLevel,
                byte[] initialTypes,
                byte[] embeddings,
                int[] linebreaks,
                byte[] resolvedLevels,
                byte[] lineLevels);

        /**
         * Display a message.
         *
         * @param msg the message text
         */
        public abstract void message(String msg);


        /** The phase before any processing on the data bas been performed. */
        public static int PHASE_INIT = 0;

        /** The phase after the base paragraph level has been determined. */
        public static int PHASE_BASELEVEL = 1;

        /** The phase after explicit codes have been processed to generate the embedding information. */
        public static int PHASE_EXPLICIT = 2;

        /** The phase after explicit codes and BN have been removed from the internal data. */
        public static int PHASE_EXPLICIT_REMOVED = 3;

        /** The phase after the weak rule processing has been performed. */
        public static int PHASE_WEAK = 4;

        /** The phase after the neutral rule processing has been performed. */
        public static int PHASE_NEUTRAL = 5;

        /** The phase after the implicit rule processing has been performed. */
        public static int PHASE_IMPLICIT = 6;
    }

    private static BidiTraceHook hook = null; // for tracking the algorithm

    /**
     * Set a trace hook so the progress of the algorithm can be monitored.
     */
    public static synchronized void setTraceHook(BidiTraceHook hook) {
        BidiReference.hook = hook;
    }

    /**
     * Return the trace hook.
     */
    public static BidiTraceHook getTraceHook() {
        return hook;
    }

    /**
     * Call trace hook during major phases of algorithm.
     */
    private void trace(int phase, int start, int limit) {
        if (hook != null) {
            hook.display(phase, start, limit, paragraphEmbeddingLevel,
                    initialTypes, embeddings, resultTypes, resultLevels);
        }
    }

    /**
     * Call trace hook when computing line levels based on linebreaks.
     */
    private void traceLineLevels(int[] linebreaks, byte[] lineLevels) {
        if (hook != null) {
            hook.displayLineLevels(paragraphEmbeddingLevel, initialTypes, embeddings, linebreaks, resultLevels, lineLevels);
        }
    }

    private void setRule(String rule) {
        final String[] anchor = rule.split("-");
        this.rule = "<a target='doc' href='http://unicode.org/reports/tr9/#" + anchor[0] + "'>" + rule + "</a>";
    }

    public static String getHtmlTypename(int value) {
        return "<a target='list' href='http://unicode.org/cldr/utility/list-unicodeset.jsp?a=[:bc=" + typenames[value] + ":]'>" + typenames[value] + "</a>";
    }

    private String getRule() {
        return rule;
    }
}

