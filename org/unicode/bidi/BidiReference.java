package org.unicode.bidi;

import java.util.Arrays;

/*
 * Last Revised: 2013-04-27
 *
 * Credits:
 * Originally written by Doug Felt
 * Updated for Unicode 6.3 by Roozbeh Pournader, with feedback by Aharon Lanin
 *
 * Disclaimer and legal rights:
 * (C) Copyright IBM Corp. 1999, All Rights Reserved
 * (C) Copyright Google Inc. 2013, All Rights Reserved
 *
 * Distributed under the Terms of Use in http://www.unicode.org/copyright.html.
*/

/**
 * Reference implementation of the Unicode Bidirectional Algorithm (UAX #9).
 *
 * <p>
 * This implementation is not optimized for performance. It is intended as a
 * reference implementation that closely follows the specification of the
 * Bidirectional Algorithm in The Unicode Standard version 6.3.
 * <p>
 * <b>Input:</b><br>
 * There are two levels of input to the algorithm, since clients may prefer to
 * supply some information from out-of-band sources rather than relying on the
 * default behavior.
 * <ol>
 * <li>Bidi class array
 * <li>Bidi class array, with externally supplied base line direction
 * </ol>
 * <p>
 * <b>Output:</b><br>
 * Output is separated into several stages as well, to better enable clients to
 * evaluate various aspects of implementation conformance.
 * <ol>
 * <li>levels array over entire paragraph
 * <li>reordering array over entire paragraph
 * <li>levels array over line
 * <li>reordering array over line
 * </ol>
 * Note that for conformance to the Unicode Bidirectional Algorithm,
 * implementations are only required to generate correct reordering and
 * character directionality (odd or even levels) over a line. Generating
 * identical level arrays over a line is not required. Bidi explicit format
 * codes (LRE, RLE, LRO, RLO, PDF) and BN can be assigned arbitrary levels and
 * positions as long as the rest of the input is properly reordered.
 * <p>
 * As the algorithm is defined to operate on a single paragraph at a time, this
 * implementation is written to handle single paragraphs. Thus rule P1 is
 * presumed by this implementation-- the data provided to the implementation is
 * assumed to be a single paragraph, and either contains no 'B' codes, or a
 * single 'B' code at the end of the input. 'B' is allowed as input to
 * illustrate how the algorithm assigns it a level.
 * <p>
 * Also note that rules L3 and L4 depend on the rendering engine that uses the
 * result of the bidi algorithm. This implementation assumes that the rendering
 * engine expects combining marks in visual order (e.g. to the left of their
 * base character in RTL runs) and that it adjusts the glyphs used to render
 * mirrored characters that are in RTL runs so that they render appropriately.
 *
 * @author Doug Felt
 * @author Roozbeh Pournader
 */

public final class BidiReference {
    private final byte[] initialTypes;
    private byte paragraphEmbeddingLevel = -1; // undefined

    private int textLength; // for convenience
    private byte[] resultTypes; // for paragraph, not lines
    private byte[] resultLevels; // for paragraph, not lines

    /*
     * Index of matching PDI for isolate initiator characters. For other
     * characters, the value of matchingPDI will be set to -1. For isolate
     * initiators with no matching PDI, matchingPDI will be set to the length of
     * the input string.
     */
    private int[] matchingPDI;

    /*
     * Index of matching isolate initiator for PDI characters. For other
     * characters, and for PDIs with no matching isolate initiator, the value of
     * matchingIsolateInitiator will be set to -1.
     */
    private int[] matchingIsolateInitiator;

    // The bidi types

    /** Left-to-right */
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

    /** Left-to-Right Isolate */
    public static final byte LRI = 19;

    /** Right-to-Left Isolate */
    public static final byte RLI = 20;

    /** First-Strong Isolate */
    public static final byte FSI = 21;

    /** Pop Directional Isolate */
    public static final byte PDI = 22;

    /** Minimum bidi type value. */
    public static final byte TYPE_MIN = 0;

    /** Maximum bidi type value. */
    public static final byte TYPE_MAX = 22;

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
            "LRI",
            "RLI",
            "FSI",
            "PDI"
    };

    //
    // Input
    //

    /**
     * Initialize using an array of direction types. Types range from TYPE_MIN
     * to TYPE_MAX inclusive and represent the direction codes of the characters
     * in the text.
     *
     * @param types
     *            the types array
     */
    public BidiReference(byte[] types) {
        validateTypes(types);

        initialTypes = types.clone(); // client type array remains unchanged

        runAlgorithm();
    }

    /**
     * Initialize using an array of direction types and an externally supplied
     * paragraph embedding level. The embedding level may be -1, 0, or 1.
     * <p>
     * -1 means to apply the default algorithm (rules P2 and P3), 0 is for LTR
     * paragraphs, and 1 is for RTL paragraphs.
     *
     * @param types
     *            the types array
     * @param paragraphEmbeddingLevel
     *            the externally supplied paragraph embedding level.
     */
    public BidiReference(byte[] types, byte paragraphEmbeddingLevel) {
        validateTypes(types);
        validateParagraphEmbeddingLevel(paragraphEmbeddingLevel);

        initialTypes = types.clone(); // client type array remains unchanged
        this.paragraphEmbeddingLevel = paragraphEmbeddingLevel;

        runAlgorithm();
    }

    /**
     * The algorithm. Does not include line-based processing (Rules L1, L2).
     * These are applied later in the line-based phase of the algorithm.
     */
    private void runAlgorithm() {
        textLength = initialTypes.length;

        // Initialize output types.
        // Result types initialized to input types.
        resultTypes = initialTypes.clone();

        // Preprocessing to find the matching isolates
        determineMatchingIsolates();

        // 1) determining the paragraph level
        // Rule P1 is the requirement for entering this algorithm.
        // Rules P2, P3.
        // If no externally supplied paragraph embedding level, use default.
        if (paragraphEmbeddingLevel == -1) {
            paragraphEmbeddingLevel = determineParagraphEmbeddingLevel(0, textLength);
        }

        // Initialize result levels to paragraph embedding level.
        resultLevels = new byte[textLength];
        setLevels(resultLevels, 0, textLength, paragraphEmbeddingLevel);

        // 2) Explicit levels and directions
        // Rules X1-X8.
        determineExplicitEmbeddingLevels();

        // Rule X9.
        // We now remove the embeddings, the overrides, the PDFs, and the BNs
        // from the
        // string explicitly. But they are not copied into isolating run
        // sequences when they are created, so they are removed for all
        // practical purposes.

        // Rule X10.
        // Run remainder of algorithm one isolating run sequence at a time
        IsolatingRunSequence[] sequences = determineIsolatingRunSequences();

        for (int i = 0; i < sequences.length; ++i) {
            IsolatingRunSequence sequence = sequences[i];
            // 3) resolving weak types
            // Rules W1-W7.
            sequence.resolveWeakTypes();

            // 4) resolving neutral types
            // Rules N1-N3.
            sequence.resolveNeutralTypes();

            // 5) resolving implicit embedding levels
            // Rules I1, I2.
            sequence.resolveImplicitLevels();

            // Apply the computed levels and types
            sequence.applyLevelsAndTypes();
        }

        // Assign appropriate levels to 'hide' LREs, RLEs, LROs, RLOs, PDFs, and
        // BNs. This is for convenience, so the resulting level array will have
        // a value for every character.
        assignLevelsToCharactersRemovedByX9();
    }

    /**
     * Determine the matching PDI for each isolate initiator and vice versa.
     * <p>
     * Definition BD9.
     * <p>
     * At the end of this function:
     * <ul>
     * <li>The member variable matchingPDI is set to point to the index of the
     * matching PDI character for each isolate initiator character. If there is
     * no matching PDI, it is set to the length of the input text. For other
     * characters, it is set to -1.
     * <li>The member variable matchingIsolateInitiator is set to point to the
     * index of the matching isolate initiator character for each PDI character.
     * If there is no matching isolate initiator, or the character is not a PDI,
     * it is set to -1.
     * </ul>
     */
    private void determineMatchingIsolates() {
        matchingPDI = new int[textLength];
        matchingIsolateInitiator = new int[textLength];

        for (int i = 0; i < textLength; ++i) {
            matchingIsolateInitiator[i] = -1;
        }

        for (int i = 0; i < textLength; ++i) {
            matchingPDI[i] = -1;

            byte t = resultTypes[i];
            if (t == LRI || t == RLI || t == FSI) {
                int depthCounter = 1;
                for (int j = i + 1; j < textLength; ++j) {
                    byte u = resultTypes[j];
                    if (u == LRI || u == RLI || u == FSI) {
                        ++depthCounter;
                    } else if (u == PDI) {
                        --depthCounter;
                        if (depthCounter == 0) {
                            matchingPDI[i] = j;
                            matchingIsolateInitiator[j] = i;
                            break;
                        }
                    }
                }
                if (matchingPDI[i] == -1) {
                    matchingPDI[i] = textLength;
                }
            }
        }
    }

    /**
     * Determines the paragraph level based on rules P2, P3. This is also used
     * in rule X5c to find if an FSI should resolve to LRI or RLI.
     *
     * @param startIndex
     *            the index of the beginning of the substring
     * @param endIndex
     *            the index of the character after the end of the string
     *
     * @return the resolved paragraph direction of the substring limited by
     *         startIndex and endIndex
     */
    private byte determineParagraphEmbeddingLevel(int startIndex, int endIndex) {
        byte strongType = -1; // unknown

        // Rule P2.
        for (int i = startIndex; i < endIndex; ++i) {
            byte t = resultTypes[i];
            if (t == L || t == AL || t == R) {
                strongType = t;
                break;
            } else if (t == FSI || t == LRI || t == RLI) {
                i = matchingPDI[i]; // skip over to the matching PDI
                assert (i <= endIndex);
            }
        }

        // Rule P3.
        if (strongType == -1) { // none found
            // default embedding level when no strong types found is 0.
            return 0;
        } else if (strongType == L) {
            return 0;
        } else { // AL, R
            return 1;
        }
    }

    public static final int MAX_DEPTH = 61;

    // This stack will store the embedding levels and override and isolated
    // statuses
    private class directionalStatusStack {
        private int stackCounter = 0;
        private final byte[] embeddingLevelStack = new byte[MAX_DEPTH + 1];
        private final byte[] overrideStatusStack = new byte[MAX_DEPTH + 1];
        private final boolean[] isolateStatusStack = new boolean[MAX_DEPTH + 1];

        public void empty() {
            stackCounter = 0;
        }

        public void push(byte level, byte overrideStatus, boolean isolateStatus) {
            embeddingLevelStack[stackCounter] = level;
            overrideStatusStack[stackCounter] = overrideStatus;
            isolateStatusStack[stackCounter] = isolateStatus;
            ++stackCounter;
        }

        public void pop() {
            --stackCounter;
        }

        public int depth() {
            return stackCounter;
        }

        public byte lastEmbeddingLevel() {
            return embeddingLevelStack[stackCounter - 1];
        }

        public byte lastDirectionalOverrideStatus() {
            return overrideStatusStack[stackCounter - 1];
        }

        public boolean lastDirectionalIsolateStatus() {
            return isolateStatusStack[stackCounter - 1];
        }
    }

    /**
     * Determine explicit levels using rules X1 - X8
     */
    private void determineExplicitEmbeddingLevels() {
        directionalStatusStack stack = new directionalStatusStack();
        int overflowIsolateCount, overflowEmbeddingCount, validIsolateCount;

        // Rule X1.
        stack.empty();
        stack.push(paragraphEmbeddingLevel, ON, false);
        overflowIsolateCount = 0;
        overflowEmbeddingCount = 0;
        validIsolateCount = 0;
        for (int i = 0; i < textLength; ++i) {
            byte t = resultTypes[i];

            // Rules X2, X3, X4, X5, X5a, X5b, X5c
            switch (t) {
            case RLE:
            case LRE:
            case RLO:
            case LRO:
            case RLI:
            case LRI:
            case FSI:
                boolean isIsolate = (t == RLI || t == LRI || t == FSI);
                boolean isRTL = (t == RLE || t == RLO || t == RLI);
                // override if this is an FSI that resolves to RLI
                if (t == FSI) {
                    isRTL = (determineParagraphEmbeddingLevel(i + 1, matchingPDI[i]) == 1);
                }

                if (isIsolate) {
                    resultLevels[i] = stack.lastEmbeddingLevel();
                }

                byte newLevel;
                if (isRTL) {
                    // least greater odd
                    newLevel = (byte) ((stack.lastEmbeddingLevel() + 1) | 1);
                } else {
                    // least greater even
                    newLevel = (byte) ((stack.lastEmbeddingLevel() + 2) & ~1);
                }

                if (newLevel <= MAX_DEPTH && overflowIsolateCount == 0 && overflowEmbeddingCount == 0) {
                    if (isIsolate) {
                        ++validIsolateCount;
                    }
                    // Push new embedding level, override status, and isolated
                    // status.
                    // No check for valid stack counter, since the level check
                    // suffices.
                    stack.push(
                            newLevel,
                            t == LRO ? L : t == RLO ? R : ON,
                            isIsolate);

                    // Not really part of the spec
                    if (!isIsolate) {
                        resultLevels[i] = newLevel;
                    }
                } else {
                    // This is an invalid explicit formatting character,
                    // so apply the "Otherwise" part of rules X2-X5b.
                    if (isIsolate) {
                        ++overflowIsolateCount;
                    } else { // !isIsolate
                        if (overflowIsolateCount == 0) {
                            ++overflowEmbeddingCount;
                        }
                    }
                }
                break;

            // Rule X6a
            case PDI:
                if (overflowIsolateCount > 0) {
                    --overflowIsolateCount;
                } else if (validIsolateCount == 0) {
                    // do nothing
                } else {
                    overflowEmbeddingCount = 0;
                    while (!stack.lastDirectionalIsolateStatus()) {
                        stack.pop();
                    }
                    stack.pop();
                    --validIsolateCount;
                }
                resultLevels[i] = stack.lastEmbeddingLevel();
                break;

            // Rule X7
            case PDF:
                // Not really part of the spec
                resultLevels[i] = stack.lastEmbeddingLevel();

                if (overflowIsolateCount > 0) {
                    // do nothing
                } else if (overflowEmbeddingCount > 0) {
                    --overflowEmbeddingCount;
                } else if (!stack.lastDirectionalIsolateStatus() && stack.depth() >= 2) {
                    stack.pop();
                } else {
                    // do nothing
                }
                break;

            case B:
                // Rule X8.

                // These values are reset for clarity, in this implementation B
                // can only occur as the last code in the array.
                stack.empty();
                overflowIsolateCount = 0;
                overflowEmbeddingCount = 0;
                validIsolateCount = 0;
                resultLevels[i] = paragraphEmbeddingLevel;
                break;

            default:
                resultLevels[i] = stack.lastEmbeddingLevel();
                if (stack.lastDirectionalOverrideStatus() != ON) {
                    resultTypes[i] = stack.lastDirectionalOverrideStatus();
                }
                break;
            }
        }
    }

    private class IsolatingRunSequence {
        private final int[] indexes; // indexes to the original string
        private final byte[] types; // type of each character using the index
        private byte[] resolvedLevels; // resolved levels after application of
                                       // rules
        private final int length; // length of isolating run sequence in
                                  // characters
        private final byte level;
        private final byte sos, eos;

        public IsolatingRunSequence(int[] inputIndexes) {
            indexes = inputIndexes;
            length = indexes.length;

            types = new byte[length];
            for (int i = 0; i < length; ++i) {
                types[i] = resultTypes[indexes[i]];
            }

            level = resultLevels[indexes[0]];

            int prevChar = indexes[0] - 1;
            while (prevChar >= 0 && isRemovedByX9(initialTypes[prevChar])) {
                --prevChar;
            }
            byte prevLevel = prevChar >= 0 ? resultLevels[prevChar] : paragraphEmbeddingLevel;
            sos = typeForLevel(Math.max(prevLevel, level));

            byte lastType = types[length - 1];
            byte succLevel;
            if (lastType == LRI || lastType == RLI || lastType == FSI) {
                succLevel = paragraphEmbeddingLevel;
            } else {
                int limit = indexes[length - 1] + 1; // the first character
                                                     // after the end of
                                                     // run sequence
                while (limit < textLength && isRemovedByX9(initialTypes[limit])) {
                    ++limit;
                }
                succLevel = limit < textLength ? resultLevels[limit] : paragraphEmbeddingLevel;
            }
            eos = typeForLevel(Math.max(succLevel, level));
        }

        /**
         * 3) resolving weak types Rules W1-W7.
         *
         * Note that some weak types (EN, AN) remain after this processing is
         * complete.
         */
        public void resolveWeakTypes() {

            // on entry, only these types remain
            assertOnly(new byte[] { L, R, AL, EN, ES, ET, AN, CS, B, S, WS, ON, NSM, LRI, RLI, FSI, PDI });

            // Rule W1.
            // Changes all NSMs.
            byte preceedingCharacterType = sos;
            for (int i = 0; i < length; ++i) {
                byte t = types[i];
                if (t == NSM) {
                    types[i] = preceedingCharacterType;
                } else {
                    if (t == LRI || t == RLI || t == FSI || t == PDI) {
                        preceedingCharacterType = ON;
                    }
                    preceedingCharacterType = t;
                }
            }

            // Rule W2.
            // EN does not change at the start of the run, because sos != AL.
            for (int i = 0; i < length; ++i) {
                if (types[i] == EN) {
                    for (int j = i - 1; j >= 0; --j) {
                        byte t = types[j];
                        if (t == L || t == R || t == AL) {
                            if (t == AL) {
                                types[i] = AN;
                            }
                            break;
                        }
                    }
                }
            }

            // Rule W3.
            for (int i = 0; i < length; ++i) {
                if (types[i] == AL) {
                    types[i] = R;
                }
            }

            // Rule W4.
            // Since there must be values on both sides for this rule to have an
            // effect, the scan skips the first and last value.
            //
            // Although the scan proceeds left to right, and changes the type
            // values in a way that would appear to affect the computations
            // later in the scan, there is actually no problem. A change in the
            // current value can only affect the value to its immediate right,
            // and only affect it if it is ES or CS. But the current value can
            // only change if the value to its right is not ES or CS. Thus
            // either the current value will not change, or its change will have
            // no effect on the remainder of the analysis.

            for (int i = 1; i < length - 1; ++i) {
                if (types[i] == ES || types[i] == CS) {
                    byte prevSepType = types[i - 1];
                    byte succSepType = types[i + 1];
                    if (prevSepType == EN && succSepType == EN) {
                        types[i] = EN;
                    } else if (types[i] == CS && prevSepType == AN && succSepType == AN) {
                        types[i] = AN;
                    }
                }
            }

            // Rule W5.
            for (int i = 0; i < length; ++i) {
                if (types[i] == ET) {
                    // locate end of sequence
                    int runstart = i;
                    int runlimit = findRunLimit(runstart, length, new byte[] { ET });

                    // check values at ends of sequence
                    byte t = runstart == 0 ? sos : types[runstart - 1];

                    if (t != EN) {
                        t = runlimit == length ? eos : types[runlimit];
                    }

                    if (t == EN) {
                        setTypes(runstart, runlimit, EN);
                    }

                    // continue at end of sequence
                    i = runlimit;
                }
            }

            // Rule W6.
            for (int i = 0; i < length; ++i) {
                byte t = types[i];
                if (t == ES || t == ET || t == CS) {
                    types[i] = ON;
                }
            }

            // Rule W7.
            for (int i = 0; i < length; ++i) {
                if (types[i] == EN) {
                    // set default if we reach start of run
                    byte prevStrongType = sos;
                    for (int j = i - 1; j >= 0; --j) {
                        byte t = types[j];
                        if (t == L || t == R) { // AL's have been changed to R
                            prevStrongType = t;
                            break;
                        }
                    }
                    if (prevStrongType == L) {
                        types[i] = L;
                    }
                }
            }
        }

        /**
         * 6) resolving neutral types Rules N1-N2.
         */
        public void resolveNeutralTypes() {

            // on entry, only these types can be in resultTypes
            assertOnly(new byte[] { L, R, EN, AN, B, S, WS, ON, RLI, LRI, FSI, PDI });

            for (int i = 0; i < length; ++i) {
                byte t = types[i];
                if (t == WS || t == ON || t == B || t == S || t == RLI || t == LRI || t == FSI || t == PDI) {
                    // find bounds of run of neutrals
                    int runstart = i;
                    int runlimit = findRunLimit(runstart, length, new byte[] { B, S, WS, ON, RLI, LRI, FSI, PDI });

                    // determine effective types at ends of run
                    byte leadingType;
                    byte trailingType;

                    // Note that the character found can only be L, R, AN, or
                    // EN.
                    if (runstart == 0) {
                        leadingType = sos;
                    } else {
                        leadingType = types[runstart - 1];
                        if (leadingType == AN || leadingType == EN) {
                            leadingType = R;
                        }
                    }

                    if (runlimit == length) {
                        trailingType = eos;
                    } else {
                        trailingType = types[runlimit];
                        if (trailingType == AN || trailingType == EN) {
                            trailingType = R;
                        }
                    }

                    byte resolvedType;
                    if (leadingType == trailingType) {
                        // Rule N1.
                        resolvedType = leadingType;
                    } else {
                        // Rule N2.
                        // Notice the embedding level of the run is used, not
                        // the paragraph embedding level.
                        resolvedType = typeForLevel(level);
                    }

                    setTypes(runstart, runlimit, resolvedType);

                    // skip over run of (former) neutrals
                    i = runlimit;
                }
            }
        }

        /**
         * 7) resolving implicit embedding levels Rules I1, I2.
         */
        public void resolveImplicitLevels() {

            // on entry, only these types can be in resultTypes
            assertOnly(new byte[] { L, R, EN, AN });

            resolvedLevels = new byte[length];
            setLevels(resolvedLevels, 0, length, level);

            if ((level & 1) == 0) { // even level
                for (int i = 0; i < length; ++i) {
                    byte t = types[i];
                    // Rule I1.
                    if (t == L) {
                        // no change
                    } else if (t == R) {
                        resolvedLevels[i] += 1;
                    } else { // t == AN || t == EN
                        resolvedLevels[i] += 2;
                    }
                }
            } else { // odd level
                for (int i = 0; i < length; ++i) {
                    byte t = types[i];
                    // Rule I2.
                    if (t == R) {
                        // no change
                    } else { // t == L || t == AN || t == EN
                        resolvedLevels[i] += 1;
                    }
                }
            }
        }

        /*
         * Applies the levels and types resolved in rules W1-I2 to the
         * resultLevels array.
         */
        public void applyLevelsAndTypes() {
            for (int i = 0; i < length; ++i) {
                int originalIndex = indexes[i];
                resultTypes[originalIndex] = types[i];
                resultLevels[originalIndex] = resolvedLevels[i];
            }
        }

        /**
         * Return the limit of the run consisting only of the types in validSet
         * starting at index. This checks the value at index, and will return
         * index if that value is not in validSet.
         */
        private int findRunLimit(int index, int limit, byte[] validSet) {
            loop: while (index < limit) {
                byte t = types[index];
                for (int i = 0; i < validSet.length; ++i) {
                    if (t == validSet[i]) {
                        ++index;
                        continue loop;
                    }
                }
                // didn't find a match in validSet
                return index;
            }
            return limit;
        }

        /**
         * Set types from start up to (but not including) limit to newType.
         */
        private void setTypes(int start, int limit, byte newType) {
            for (int i = start; i < limit; ++i) {
                types[i] = newType;
            }
        }

        /**
         * Algorithm validation. Assert that all values in types are in the
         * provided set.
         */
        private void assertOnly(byte[] codes) {
            loop: for (int i = 0; i < length; ++i) {
                byte t = types[i];
                for (int j = 0; j < codes.length; ++j) {
                    if (t == codes[j]) {
                        continue loop;
                    }
                }

                throw new Error("invalid bidi code " + typenames[t] + " present in assertOnly at position " + indexes[i]);
            }
        }
    }

    /**
     * Definition BD13. Determine isolating run sequences.
     *
     * @return an array of isolating run sequences.
     */
    private IsolatingRunSequence[] determineIsolatingRunSequences() {
        int[][] levelRuns = determineLevelRuns();
        int numRuns = levelRuns.length;

        // Compute the run that each character belongs to
        int[] runForCharacter = new int[textLength];
        for (int runNumber = 0; runNumber < numRuns; ++runNumber) {
            for (int i = 0; i < levelRuns[runNumber].length; ++i) {
                int characterIndex = levelRuns[runNumber][i];
                runForCharacter[characterIndex] = runNumber;
            }
        }

        IsolatingRunSequence[] sequences = new IsolatingRunSequence[numRuns];
        int numSequences = 0;
        int[] currentRunSequence = new int[textLength];
        for (int i = 0; i < levelRuns.length; ++i) {
            int firstCharacter = levelRuns[i][0];
            if (initialTypes[firstCharacter] != PDI || matchingIsolateInitiator[firstCharacter] == -1) {
                int currentRunSequenceLength = 0;
                int run = i;
                do {
                    // Copy this level run into currentRunSequence
                    System.arraycopy(levelRuns[run], 0, currentRunSequence, currentRunSequenceLength, levelRuns[run].length);
                    currentRunSequenceLength += levelRuns[run].length;

                    int lastCharacter = currentRunSequence[currentRunSequenceLength - 1];
                    byte lastType = initialTypes[lastCharacter];
                    if ((lastType == LRI || lastType == RLI || lastType == FSI) &&
                            matchingPDI[lastCharacter] != textLength) {
                        run = runForCharacter[matchingPDI[lastCharacter]];
                    } else {
                        break;
                    }
                } while (true);

                sequences[numSequences] = new IsolatingRunSequence(
                        Arrays.copyOf(currentRunSequence, currentRunSequenceLength));
                ++numSequences;
            }
        }
        return Arrays.copyOf(sequences, numSequences);
    }

    /**
     * Determines the level runs. Rule X9 will be applied in determining the
     * runs, in the way that makes sure the characters that are supposed to be
     * removed are not included in the runs.
     *
     * @return an array of level runs. Each level run is described as an array
     *         of indexes into the input string.
     */
    private int[][] determineLevelRuns() {
        // temporary array to hold the run
        int[] temporaryRun = new int[textLength];
        // temporary array to hold the list of runs
        int[][] allRuns = new int[textLength][];
        int numRuns = 0;

        byte currentLevel = (byte) -1;
        int runLength = 0;
        for (int i = 0; i < textLength; ++i) {
            if (!isRemovedByX9(initialTypes[i])) {
                if (resultLevels[i] != currentLevel) { // we just encountered a
                                                       // new run
                    // Wrap up last run
                    if (currentLevel >= 0) { // only wrap it up if there was a run
                        int[] run = Arrays.copyOf(temporaryRun, runLength);
                        allRuns[numRuns] = run;
                        ++numRuns;
                    }
                    // Start new run
                    currentLevel = resultLevels[i];
                    runLength = 0;
                }
                temporaryRun[runLength] = i;
                ++runLength;
            }
        }
        // Wrap up the final run, if any
        if (runLength != 0) {
            int[] run = Arrays.copyOf(temporaryRun, runLength);
            allRuns[numRuns] = run;
            ++numRuns;
        }

        return Arrays.copyOf(allRuns, numRuns);
    }

    /**
     * Assign level information to characters removed by rule X9. This is for
     * ease of relating the level information to the original input data. Note
     * that the levels assigned to these codes are arbitrary, they're chosen so
     * as to avoid breaking level runs.
     *
     * @param textLength
     *            the length of the data after compression
     * @return the length of the data (original length of types array supplied
     *         to constructor)
     */
    private int assignLevelsToCharactersRemovedByX9() {
        for (int i = 0; i < initialTypes.length; ++i) {
            byte t = initialTypes[i];
            if (t == LRE || t == RLE || t == LRO || t == RLO || t == PDF || t == BN) {
                resultTypes[i] = t;
                resultLevels[i] = -1;
            }
        }

        // now propagate forward the levels information (could have
        // propagated backward, the main thing is not to introduce a level
        // break where one doesn't already exist).

        if (resultLevels[0] == -1) {
            resultLevels[0] = paragraphEmbeddingLevel;
        }
        for (int i = 1; i < initialTypes.length; ++i) {
            if (resultLevels[i] == -1) {
                resultLevels[i] = resultLevels[i - 1];
            }
        }

        // Embedding information is for informational purposes only
        // so need not be adjusted.

        return initialTypes.length;
    }

    //
    // Output
    //

    /**
     * Return levels array breaking lines at offsets in linebreaks. <br>
     * Rule L1.
     * <p>
     * The returned levels array contains the resolved level for each bidi code
     * passed to the constructor.
     * <p>
     * The linebreaks array must include at least one value. The values must be
     * in strictly increasing order (no duplicates) between 1 and the length of
     * the text, inclusive. The last value must be the length of the text.
     *
     * @param linebreaks
     *            the offsets at which to break the paragraph
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
        // initial text. Their final placement is not normative.
        // These codes are treated like WS in this implementation,
        // so they don't interrupt sequences of WS.

        validateLineBreaks(linebreaks, textLength);

        byte[] result = resultLevels.clone(); // will be returned to
                                              // caller

        // don't worry about linebreaks since if there is a break within
        // a series of WS values preceding S, the linebreak itself
        // causes the reset.
        for (int i = 0; i < result.length; ++i) {
            byte t = initialTypes[i];
            if (t == B || t == S) {
                // Rule L1, clauses one and two.
                result[i] = paragraphEmbeddingLevel;

                // Rule L1, clause three.
                for (int j = i - 1; j >= 0; --j) {
                    if (isWhitespace(initialTypes[j])) { // including format
                                                         // codes
                        result[j] = paragraphEmbeddingLevel;
                    } else {
                        break;
                    }
                }
            }
        }

        // Rule L1, clause four.
        int start = 0;
        for (int i = 0; i < linebreaks.length; ++i) {
            int limit = linebreaks[i];
            for (int j = limit - 1; j >= start; --j) {
                if (isWhitespace(initialTypes[j])) { // including format codes
                    result[j] = paragraphEmbeddingLevel;
                } else {
                    break;
                }
            }

            start = limit;
        }

        return result;
    }

    /**
     * Return reordering array breaking lines at offsets in linebreaks.
     * <p>
     * The reordering array maps from a visual index to a logical index. Lines
     * are concatenated from left to right. So for example, the fifth character
     * from the left on the third line is
     *
     * <pre>
     * getReordering(linebreaks)[linebreaks[1] + 4]
     * </pre>
     *
     * (linebreaks[1] is the position after the last character of the second
     * line, which is also the index of the first character on the third line,
     * and adding four gets the fifth character from the left).
     * <p>
     * The linebreaks array must include at least one value. The values must be
     * in strictly increasing order (no duplicates) between 1 and the length of
     * the text, inclusive. The last value must be the length of the text.
     *
     * @param linebreaks
     *            the offsets at which to break the paragraph.
     */
    public int[] getReordering(int[] linebreaks) {
        validateLineBreaks(linebreaks, textLength);

        byte[] levels = getLevels(linebreaks);

        return computeMultilineReordering(levels, linebreaks);
    }

    /**
     * Return multiline reordering array for a given level array. Reordering
     * does not occur across a line break.
     */
    private static int[] computeMultilineReordering(byte[] levels, int[] linebreaks) {
        int[] result = new int[levels.length];

        int start = 0;
        for (int i = 0; i < linebreaks.length; ++i) {
            int limit = linebreaks[i];

            byte[] templevels = new byte[limit - start];
            System.arraycopy(levels, start, templevels, 0, templevels.length);

            int[] temporder = computeReordering(templevels);
            for (int j = 0; j < temporder.length; ++j) {
                result[start + j] = temporder[j] + start;
            }

            start = limit;
        }

        return result;
    }

    /**
     * Return reordering array for a given level array. This reorders a single
     * line. The reordering is a visual to logical map. For example, the
     * leftmost char is string.charAt(order[0]). Rule L2.
     */
    private static int[] computeReordering(byte[] levels) {
        int lineLength = levels.length;

        int[] result = new int[lineLength];

        // initialize order
        for (int i = 0; i < lineLength; ++i) {
            result[i] = i;
        }

        // locate highest level found on line.
        // Note the rules say text, but no reordering across line bounds is
        // performed,
        // so this is sufficient.
        byte highestLevel = 0;
        byte lowestOddLevel = 63;
        for (int i = 0; i < lineLength; ++i) {
            byte level = levels[i];
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
                    int start = i;
                    int limit = i + 1;
                    while (limit < lineLength && levels[limit] >= level) {
                        ++limit;
                    }

                    // reverse run
                    for (int j = start, k = limit - 1; j < k; ++j, --k) {
                        int temp = result[j];
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
     * Return true if the type is considered a whitespace type for the line
     * break rules.
     */
    private static boolean isWhitespace(byte biditype) {
        switch (biditype) {
        case LRE:
        case RLE:
        case LRO:
        case RLO:
        case PDF:
        case LRI:
        case RLI:
        case FSI:
        case PDI:
        case BN:
        case WS:
            return true;
        default:
            return false;
        }
    }

    /**
     * Return true if the type is one of the types removed in X9.
     */
    private static boolean isRemovedByX9(byte biditype) {
        switch (biditype) {
        case LRE:
        case RLE:
        case LRO:
        case RLO:
        case PDF:
        case BN:
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
     * Set levels from start up to (but not including) limit to newLevel.
     */
    private void setLevels(byte[] levels, int start, int limit, byte newLevel) {
        for (int i = start; i < limit; ++i) {
            levels[i] = newLevel;
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
     * Throw exception if paragraph embedding level is invalid. Special
     * allowance for -1 so that default processing can still be performed when
     * using this API.
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
            int next = linebreaks[i];
            if (next <= prev) {
                throw new IllegalArgumentException("bad linebreak: " + next + " at index: " + i);
            }
            prev = next;
        }
        if (prev != textLength) {
            throw new IllegalArgumentException("last linebreak must be at " + textLength);
        }
    }
}
