package org.unicode.utilities;

import java.util.Set;
import com.google.common.base.Strings;

import org.unicode.text.utility.Utility;
import org.unicode.utilities.StringSetBuilder.StringSetBuilderFactory;

import com.google.common.collect.ImmutableSet;

import com.ibm.icu.text.UTF16;

/**
 * Simple illustrative set for parsing a Character Class, such as a UnicodeSet.
 * @param <T>
 */
public class SimpleUnicodeParser<U, SSB extends StringSetBuilder<U>, SSBF extends StringSetBuilderFactory<SSB>> {
    private String source;
    private int offset;
    private SSBF sinkFactory;
    private int level;
    private boolean logOn;
    private boolean ignoreSpaces = true;
    private String lastMatch = null;

    public SimpleUnicodeParser(SSBF sink) {
	this.sinkFactory = sink;
    }

    public boolean isLogOn() {
        return logOn;
    }

    public SimpleUnicodeParser<U, SSB, SSBF> setLogOn(boolean logOn) {
        this.logOn = logOn;
        return this;
    }

    public SimpleUnicodeParser<U, SSB, SSBF> setIgnoreSpaces(boolean b) {
	this.ignoreSpaces = b;
	return this;
    }
    public boolean isIgnoreSpaces() {
        return ignoreSpaces;
    }

    @Override
    public String toString() {
	return toString("➗");
    }

    public String toString(String marker) {
	return source.substring(0,offset) + marker + source.substring(offset);
    }

    /**
     * Parse a UnicodeSet. Not thread safe.
     * @return
     */
    public SSB parse(String source) {
	this.source = source;
	this.offset = 0;
	this.level = 0;
	log(null);
	SSB result = parseOne();
	throwIf(result == null, "Missing [, \\p{, or \\P{ at start");
	log(result);
	throwIf(offset < source.length(), "Characters after final ] or }.");
	return result;
    }
    /**
     * Returns a UnicodeSet, or null if the text doesn't start with the proper initial characters.
     * @return
     */
    private SSB parseOne() {
	try {
	    eatSpaces();
	    if (matchesThenFixesOffset("[^")) {
		SSB sequence = parseSequence();
		sequence.complement();
		log(sequence);
		return sequence;
	    } else if (matchesThenFixesOffset("[")) {
		return parseSequence();
	    } else if (matchesThenFixesOffset("\\p{")) {
		return parseProperty();
	    } else if (matchesThenFixesOffset("\\P{")) {
		SSB sequence =  parseProperty();
		sequence.complement();
		log(sequence);
		return sequence;
	    } else {
		return null;
	    }
	} finally {
	}
    }

    private void eatSpaces() {
	while (ignoreSpaces && offset < source.length() && source.charAt(offset) == ' ') {
	    ++offset;
	}
    }

    public void log(Object object) {
	if (logOn) {
	    System.out.println(com.google.common.base.Strings.repeat("  ", level) + this + (object == null ? "" : " ➾ " + object));
	}
    }

    /**
     * Basic parse of property
     * @return UnicodeSet found
     */
    private SSB parseProperty() {
	int termination = source.indexOf("}", offset); // TODO change to skip over escaped }
	throwIf(termination < 0, "Unterminated property");
	SSB propSet = sinkFactory.make();
	propSet.setToProperty(source.substring(offset,termination));
	offset = termination+1;
	return propSet;
    }

    /**
     * Parse the elements within […] or [^…]
     * @param level 
     * @return UnicodeSet found
     */
    private SSB parseSequence() {
	SequenceParser sequenceParser = new SequenceParser();
	return sequenceParser.parse();
    }

    /**
     * Fetch next code point
     * @return the code point
     */
    private int nextCodePoint() {
	int cp = source.codePointAt(offset);
	offset += Character.charCount(cp);
	return cp;
    }

    enum Status {START, AFTER_CODEPOINT, AFTER_RANGE_OPERATOR, AFTER_RANGE, AFTER_INNER_SET}

    private class SequenceParser {
	int lastCp = -1;
	Status status = Status.START;
	SSB result = null; // lazily allocate
	StringBuilder localBuffer = new StringBuilder(); // lazily allocate

	private SSB parse() {
	    SSB inside;
	    level++;
	    try {
		while (true) {
		    eatSpaces();
		    if (matchesThenFixesOffset("]")) {
			exceptionIfInRange();
			return result;
		    } else if (null != (inside = parseOne())) { // default union
			exceptionIfInRange();
			if (result == null) {
			    result = inside; // just set the first one
			} else {
			    result.addAll(inside); 
			}
			log(result);
			status = Status.AFTER_INNER_SET;
		    } else if (checkAndApplyOperator()) {
			// work is done in checkAndApplyOperator
			log(result);
		    } else if (checkAndAddString()) {
			// work is done in checkAndAddString
			log(result);
		    } else {
			int cp = nextCodePoint();
			switch (cp) {
			case '-':
			    if (status != Status.START) {
				throwIf(status != Status.AFTER_CODEPOINT, "Double range not allowed.");    
				status = Status.AFTER_RANGE_OPERATOR;
				continue;
			    }
			    break;
			case ' ': 
			    if (ignoreSpaces) {
				continue;
			    }
			    break;
			case '\\':
			    switch(addHexToTemp()) {
			    case 0: 
				cp = nextCodePoint();
				break;
			    case 1:
				cp = localBuffer.codePointAt(0);
				break;
			    default: 
				addLocalBuffer();
				log(result);
				continue;
			    }
			    break;
			}
			addCharacter(cp);
			log(result);
		    }
		}
	    } finally {
		--level;
	    }
	}

	private boolean checkAndAddString() {
	    if (!matchesThenFixesOffset("\\q{")) {
		return false;
	    }
	    if (result == null) {
		result = sinkFactory.make();
	    }
	    throwIf(status == Status.AFTER_RANGE_OPERATOR, "Can't add string after range operator");
	    // when adding a string, the only escapes allowed are \\u{...}, and \ before single character
	    main:
		while (offset < source.length()) {
		    int cp = nextCodePoint();
		    switch (cp) {
		    case '}':
			break main;
		    case ' ':
			if (ignoreSpaces) {
			    continue main;
			}
			break;
		    case '\\':
			if (addHexToTemp() > 0) {
			    continue;
			}
			cp = nextCodePoint();
			break;
		    }
		    localBuffer.appendCodePoint(cp);
		}
	    result.add(localBuffer);
	    localBuffer.setLength(0);
	    status = Status.AFTER_CODEPOINT;
	    return true;
	}

	// Get hex string if there is one at the current position. No escapes allowed. Returns number of code points
	private int addHexToTemp() {
	    if (!matches("u{")) { //  || matches("u") || matches("N{")
		return 0;
	    }
	    String matchString = getMatchString();
	    offset += matchString.length();
	    int cp = 0;
	    int codePointCount = 0;
	    boolean buildingCodePoint = false;
	    while (offset < source.length()) {
		int digit = nextCodePoint();
		switch(digit) {
		case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9':
		    digit -= '0';
		    break;
		case 'a': case 'b': case 'c': case 'd': case 'e': case 'f':
		    digit += 10 - 'a';
		    break;
		case 'A': case 'B': case 'C': case 'D': case 'E': case 'F':
		    digit += 10 - 'A';
		    break;
		case ' ': case ' ':
		    if (buildingCodePoint) {
			localBuffer.appendCodePoint(cp);
			++codePointCount;
		    }
		    cp = 0;
		    buildingCodePoint = false;
		    continue;
		case '}':
		    if (buildingCodePoint) {
			localBuffer.appendCodePoint(cp);
			++codePointCount;
		    }
		    return codePointCount;
		default:
		    throwIf(true, "Illegal character in \\u{…" + Utility.hex(cp));
		}
		cp = (cp << 4) + digit;
		buildingCodePoint = true;
		throwIf(cp > 0x10FFFF, "Illegal character in \\u{…" + Utility.hex(cp));
	    }
	    throwIf(true, "No terminating } for \\u{");
	    return 0;
	}

	private void addCharacter(int cp) {
	    if (result == null) {
		result = sinkFactory.make();
	    }
	    if (status == Status.AFTER_RANGE_OPERATOR) {
		if (lastCp < cp || !result.isSkipValidity()) {
		    throwIf(lastCp >= cp, "Character range start ≥ end.");    
		    result.addAll(lastCp, cp);
		}
		status = Status.AFTER_RANGE;
	    } else {
		result.add(cp);
		lastCp = cp;
		status = Status.AFTER_CODEPOINT;
	    }
	}

	private void addLocalBuffer() {
	    log(localBuffer);
	    if (result == null) {
		result = sinkFactory.make();
	    }
	    int cp;
	    for (int i = 0; i < localBuffer.length(); i += Character.charCount(cp)) {
		cp = localBuffer.codePointAt(i);
		addCharacter(cp);
	    }
	    localBuffer.setLength(0);
	}

	boolean checkAndApplyOperator() {
	    if (status != Status.AFTER_INNER_SET 
		    && status != Status.AFTER_CODEPOINT) {
		return false;
	    }
	    eatSpaces();
	    SetOperator operand = matchOperator();
	    if (operand == null) {
		return false;
	    }
	    exceptionIfInRange();
	    SSB inside = parseOne();
	    throwIf(inside == null, "Must have set after operation");    
	    apply(operand, result, inside);
	    status = Status.AFTER_INNER_SET;
	    return true;
	}

	private void exceptionIfInRange() {
	    throwIf(status == Status.AFTER_RANGE_OPERATOR, "Unfinished character range.");    
	}

	@Override
	public String toString() {
	    return "lastCp=" + lastCp + " ; state=" + status + " ; result: «" + result + "» ; localBuffer: " + localBuffer + "» ; " + SimpleUnicodeParser.this.toString();
	}
    }

    enum SetOperator {
	UNION("||" /*, "|"*/), 
	INTERSECTION("&&" /*, "&"*/), 
	DIFFERENCE("--" /*, "-"*/), 
	SYMMETRIC_DIFFERENCE("~~" /*, "~"*/);
	Set<String> matchStrings;

	private SetOperator(String... matchStrings) {
	    this.matchStrings = ImmutableSet.copyOf(matchStrings);
	}


    }

    private SetOperator matchOperator() {
	for (SetOperator operand : SetOperator.values()) {
	    for (String matchString : operand.matchStrings) {
		if (matchesThenFixesOffset(matchString)) {
		    return operand;
		}
	    }
	}
	return null;
    }

    private SSB apply(SetOperator setOperator, SSB result, SSB inside) {
	switch (setOperator) {
	case UNION: 
	    result.addAll(inside); 
	    break;
	case INTERSECTION: 
	    result.retainAll(inside);  
	    break;
	case DIFFERENCE: 
	    result.removeAll(inside); 
	    break;
	case SYMMETRIC_DIFFERENCE: 
	    result.complementAll(inside); 
	    break;
	}
	return result;
    }

    public String getMatchString() {
	return lastMatch;
    }

    private boolean matches(String other) {
	boolean result = source.regionMatches(offset, other, 0, other.length());
	if (result) {
	    lastMatch = other;
	}
	return result;
    }
    
    private boolean matchesThenFixesOffset(String other) {
	boolean result = matches(other);
	if (result) {
	    offset += lastMatch.length();
	}
	return result;
    }

    void throwIf(boolean condition, String message) {
	if (condition) {
	    throw new UnicodeParserException(message + ": " + SimpleUnicodeParser.this.toString("✖️"));
	}
    }
    public static class UnicodeParserException extends RuntimeException {
	private static final long serialVersionUID = -2166041066527904618L;
	public UnicodeParserException(String message) {
	    super(message);
	}
    }
}