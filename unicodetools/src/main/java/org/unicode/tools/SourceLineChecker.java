package org.unicode.tools;

import org.unicode.tools.SourceLineChecker.Tokenizer.Type;

import com.ibm.icu.text.Bidi;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.SpanCondition;

public class SourceLineChecker {

	enum SourceError {
		OK, 
		/** At the indicated position, there is a character that needs to be escaped */
		REQUIRED_ESCAPE, 
		/** At the indicated position, a token earlier in the string is visually later */
		BAD_TOKEN_ORDER_IN_LINE, 
		/** At the indicated position, a token does not have uniform  */
		BAD_CODEPOINT_ORDER_IN_TOKEN
	};

	private int errorStart;
	private int errorLimit;
	private Tokenizer tokenizer;

	public int getErrorStart() {
		return errorStart;
	}

	public int getErrorLimit() {
		return errorLimit;
	}

	public Tokenizer getTokenizer() {
		return tokenizer;
	}

	public SourceLineChecker(Tokenizer tokenizer) {
		this.tokenizer = tokenizer;
	}

	public SourceError checkLine(String line) {
		// If there are characters that require escaping, return the position of the first one
		int firstRequiredEscape = Tokenizer.REQUIRES_ESCAPE.span(line, SpanCondition.NOT_CONTAINED);
		if (firstRequiredEscape < line.length()) {
			errorStart = firstRequiredEscape;
			errorLimit = firstRequiredEscape + Character.charCount(line.codePointAt(firstRequiredEscape));
			return SourceError.REQUIRED_ESCAPE;
		}
		// If there are BIDI characters, check the ordering
		if (Tokenizer.HAS_BIDI.containsSome(line)) {

			tokenizer.set(line);
			Bidi bidi = new Bidi(line, Bidi.DIRECTION_LEFT_TO_RIGHT);

			int[] visualToLogical = bidi.getVisualMap();

			int lastTokenIndex = -1;
			int lastTokenStart = -1;
			for (int i = 0; i < visualToLogical.length; ++i) {
				int currentTokenIndex = tokenizer.getTokenIndex(visualToLogical[i]);
				if (lastTokenIndex == currentTokenIndex) {
					continue;
				}

				// First check that the tokens are visually in ascending order
				if (lastTokenIndex > currentTokenIndex) {
					errorStart = lastTokenStart;
					errorLimit = i;
					return SourceError.BAD_TOKEN_ORDER_IN_LINE;
				} else { // lastTokenIndex < currentTokenIndex, so we have hit a new token
					// check that the last token we saw is ok; that all of its characters are in a uniform direction
					if (lastTokenStart >= 0) {
						SourceError status = inconsistentDirection(visualToLogical, lastTokenStart, i);
						if (status != SourceError.OK) {
							return status;
						}
					}
					lastTokenIndex = currentTokenIndex;
					lastTokenStart = i;
				}
			}
			SourceError status = inconsistentDirection(visualToLogical, lastTokenStart, visualToLogical.length);
			if (status != SourceError.OK) {
				return status;
			}
		}
		errorStart = 0;
		errorLimit = 0;
		return SourceError.OK;
	}

	private SourceError inconsistentDirection(int[] visualToLogical, int start, int limit) {
		int lastPosition = visualToLogical[start];
		int direction = 0;
		for (int j = start+1; j < limit; ++j) {
			int nextPosition = visualToLogical[j];
			int currDirection = lastPosition < nextPosition ? -1 : 1;
			if (direction == 0) {
				direction = currDirection;
			} else if (direction != currDirection) {
				errorStart = start;
				errorLimit = limit;
				return SourceError.BAD_CODEPOINT_ORDER_IN_TOKEN;
			}
		}
		
		return tokenizer.checkLine(visualToLogical, start, limit);
	}

	public interface Tokenizer {
		public enum Type {
			/** Whitespace */
			WSPACE,
			
			/** Identifier */
			IDENTI,
			
			/** Number (integer or decimal) */
			NUMBER,
			
			/** String, of the form "..." */
			STRING,
			
			/** Code point (character), of the form '...' */
			CPOINT,
			
			/** Comment, of the form //... */
			LINECOM,
			
			/** Comment, of the form /*...* / */
			COMMENT,
			
			/** Any other symbol */
			SYMBOL,
			
			/** Any other character occurring outside of string or codepoint syntax (probably already caught by compiler) */
			OTHER;
		}

		public static final UnicodeSet HAS_BIDI = new UnicodeSet("["
				+ "\\p{bc=AL}"
				+ "\\p{bc=AN}"
				+ "\\p{bc=LRE}"
				+ "\\p{bc=RLE}"
				+ "\\p{bc=LRO}"
				+ "\\p{bc=RLO}"
				+ "\\p{bc=PDF}"
				+ "\\p{bc=FSI}"
				+ "\\p{bc=RLI}"
				+ "\\p{bc=LRI}"
				+ "\\p{bc=PDI}"
				+ "\\p{bc=R}"
				+ "]").freeze();


		public static final UnicodeSet REQUIRES_ESCAPE = new UnicodeSet("["
				+ "\\p{deprecated}" // deprecated characters
				+ "\\p{c}" // controls, format, unassigned, private use, surrogates. Includes \\u202A-\\u202E\\u2066-\\u2069 — stateful bidi characters
				+ "[\\u115F\\u1160\\u3164\\uFFA0]" // hangul fillers (should be Cf)
				+ "-[\\x{20}\\t\\n\\r]" // allow program whitespace
				+ "-\\p{emoji_component}" // allow emoji components
				+ "-[\\u200b-\\u200f\\u2060\\u061C]" // allow ZWS, ZWNBSP, ZWNJ, ZWJ, BIDI marks
				+ "]").freeze();


		public Tokenizer set(String line);

		public SourceError checkLine(int[] visualToLogical, int start, int limit);

		public int getTokenIndex(int i);

		public int getTokenCount();

		public TokenInfo getInfoFromTokenIndex(int i);
	}

	public interface TokenInfo {
		Type getType();
		int getStart();
		int getLimit();
	}

	public void showDetails(String line) {
		Bidi bidi = new Bidi(line, Bidi.DIRECTION_LEFT_TO_RIGHT);

		int[] map = bidi.getVisualMap();

		System.out.print("memory: \t");
		for (int i = 0; i < map.length; ++i) {
			System.out.print(line.substring(i,i+1) + "\u200E\t\u200E");
		}
		System.out.println();

		System.out.print("visible:\t");
		for (int i = 0; i < map.length; ++i) {
			System.out.print(line.substring(map[i],map[i]+1) + "\u200E\t\u200E");
		}
		System.out.println();

		System.out.print("cp#:    \t");
		for (int i = 0; i < map.length; ++i) {
			System.out.print(map[i] + "\t");
		}
		System.out.println();

		System.out.print("token#: \t");
		for (int i = 0; i < map.length; ++i) {
			System.out.print(tokenizer.getTokenIndex(map[i]) + "\t");
		}

		System.out.println();

		for (int i = 0; i < tokenizer.getTokenCount(); ++i) {
			final TokenInfo tokenInfo = tokenizer.getInfoFromTokenIndex(i);
			System.out.println(i+ "\t" + tokenInfo.getType() + "\t«" + line.substring(tokenInfo.getStart(), tokenInfo.getLimit()) + "»");
		}

	}
}
