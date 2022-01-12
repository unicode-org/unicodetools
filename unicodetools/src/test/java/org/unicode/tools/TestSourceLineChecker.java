package org.unicode.tools;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.tools.SourceLineChecker.SourceError;
import org.unicode.tools.SourceLineChecker.TokenInfo;
import org.unicode.tools.SourceLineChecker.Tokenizer;
import org.unicode.tools.SourceLineChecker.Tokenizer.Type;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.Bidi;

public class TestSourceLineChecker extends TestFmwk {

	private static final boolean SHOW_DETAILS = true;

	static final String hebrewWord = "\u05D0\u05D1\u05D2";
	static final String hebrewWord2 = "\u05D3\u05D4\u05D5";

	public static void main(String[] args) {
		new TestSourceLineChecker().run(args);
	}

	public void TestOrdering() {
		char[] items = {
				'a', 'b', //'c', 
				'\u05D0', '\u05D1', //'\u05D2', 
				'1', '2', //'3', 
				'_', '$', // '%'
		};
		StringBuilder logicalBuffer = new StringBuilder();
		StringBuilder visualBuffer = new StringBuilder();
		TreeMultimap<String, String> visualToLogicalMap = TreeMultimap.create();
		int max = 100;
		int debugCount = 0;
		for (int counter = 0; ; ++counter) {
			logicalBuffer.setLength(0);
			visualBuffer.setLength(0);
			int remainder = counter;
			do  {
				int current = remainder % items.length;
				remainder /= items.length;
				logicalBuffer.append(items[current]);
			} while (remainder > 0);
			final String logical = logicalBuffer.toString();
			if (logical.length() > items.length) {
				break;
			}

			// eliminate pointless cases
			if (logical.contains("b") && !logical.contains("a")
					|| logical.contains("\u05D1") && !logical.contains("\u05D0")
					|| logical.contains("2") && !logical.contains("1")
					|| logical.contains("$") && !logical.contains("_")
					|| logical.contains("aa")
					|| logical.contains("bb")
					|| logical.contains("\u05D0\u05D0")
					|| logical.contains("\u05D1\u05D1")
					|| logical.contains("11")
					|| logical.contains("22")
					|| logical.contains("__")
					|| logical.contains("$$")
					|| logical.startsWith("1")
					|| logical.startsWith("2")
					|| logical.startsWith("_")
					|| logical.startsWith("$")
					
					) {
				continue;
			}

			Bidi bidi = new Bidi(logical, Bidi.DIRECTION_LEFT_TO_RIGHT);
			int[] visualToLogical = bidi.getVisualMap();
			for (int i = 0; i < visualToLogical.length; ++i) {
				visualBuffer.append(logical.charAt(visualToLogical[i]));
			}
			final String visual = visualBuffer.toString();
			visualToLogicalMap.put(visual, logical);
			NavigableSet<String> debug = visualToLogicalMap.get(visual);
			if (debug.size() <= 1) {
				continue;
			}
			debugCount++;
			if (debugCount > max) {
				break;
			}
//			if (logical.contains("1")) {
//				continue;
//			}
//			if ((debugCount % 100) == 0) {
//				int debug2 = 0;
//			}
//			System.out.print("visual: \t" + show(visual) + "\t logical: \t");
//			debug.forEach(x -> System.out.print(show(x) + " \t "));
//			System.out.println();
		}
		for (Entry<String, Collection<String>> entry : visualToLogicalMap.asMap().entrySet()) {
			Collection<String> visuallyEquivalent = entry.getValue();
			if (visuallyEquivalent.size() > 1) {
				System.out.println(visuallyEquivalent);
			}
		}
	}

	private String show(String x) {
		StringBuilder logicalBuffer = new StringBuilder();
		for (int i = 0; i < x.length(); ++i) {
			logicalBuffer.append(SourceLineChecker.LRM);
			logicalBuffer.append(x.charAt(i));
		}
		logicalBuffer.append(SourceLineChecker.LRM);
		return logicalBuffer.toString();
	}

	public void TestPrintout() {
		List<String> lines = ImmutableList.of(
				//"s = 100\t+ x100 + \"abc\" + 'c';",
				"printf(R\"(\u05D0(\u05D1)\"?password:\")\");",
				"s = a1\u05D0;",
				"s = a\u05D01;",
				"s = " + hebrewWord + "abc;",
				"s = " + hebrewWord + "-100;",
				"s = 100-" + hebrewWord + ";",
				"s = \"abc\" + \"" + hebrewWord + "\";",
				"s = \"" + hebrewWord + "\" + \"100\";",
				"return \"" + hebrewWord + "\"; // \"100\";",
				"s = " + hebrewWord + " /* " + hebrewWord2 + "*/;",
				"s = " + hebrewWord + "; // " + hebrewWord2
				);

		SourceLineChecker sourceLineChecker = new SourceLineChecker(new SampleTokenizer());
		System.out.println();
		for (String line : lines) {
			System.out.println(line);
			SourceError result = sourceLineChecker.checkLine(line);
			System.out.println(result + "\t" + sourceLineChecker.getErrorStart() 
			+ "\t«" + line.substring(sourceLineChecker.getErrorStart(), sourceLineChecker.getErrorLimit()) + "»");
			if (SHOW_DETAILS || result != result.OK) {
				sourceLineChecker.showDetails(line);

			}
			System.out.println();
		}
	}

	private static class SampleTokenInfo implements TokenInfo {
		private Type type;
		private int start;
		private int limit;

		public SampleTokenInfo(Type type, int start, int limit) {
			this.type = type;
			this.start = start;
			this.limit = limit;
		}

		@Override
		public Type getType() {
			return type;
		}

		@Override
		public int getStart() {
			return start;
		}

		@Override
		public int getLimit() {
			return limit;
		}
	}

	/**
	 * Sample tokenizer for illustration. Does not attempt to be full-featured or optimized. Uses regexes internally for brevity.
	 * Each programming language could need a different tokenizer.
	 */
	public static class SampleTokenizer implements Tokenizer {

		private static final Map<Type, Pattern> regexes = new EnumMap<>(Type.class);
		static {
			// Java regex has pretty lame support for Unicode properties, so these are hardcoded in this sample
			regexes.put(Type.WSPACE, Pattern.compile("[ \\t\\n\\r]+"));
			regexes.put(Type.RSTRNG, Pattern.compile("R\"(.)\""));
			regexes.put(Type.IDENTI, Pattern.compile("\\p{L}[\\p{L}\\p{M}\\p{Nd}\\p{Pc}]*"));
			regexes.put(Type.NUMBER, Pattern.compile("\\p{Nd}+([.]\\p{Nd}+)?"));
			regexes.put(Type.STRING, Pattern.compile("\"[^\"]*\""));
			regexes.put(Type.CPOINT, Pattern.compile("'.'"));
			regexes.put(Type.LINECOM, Pattern.compile("//.*"));
			regexes.put(Type.COMMENT, Pattern.compile("/\\*.+?\\*/"));
			regexes.put(Type.SYMBOL, Pattern.compile("[\\p{S}\\p{P}]"));
			regexes.put(Type.OTHER, Pattern.compile("."));
			// make sure complete, for debugging
			if (regexes.size() != Type.values().length) {
				throw new IllegalArgumentException("regexes: " + regexes.keySet() + "; values" + Arrays.asList(Type.values()));
			}
		}

		private List<Integer> cpIndexToTokenIndex;
		private List<SampleTokenInfo> tokenIndexToInfo;
		private String source;

		public int getTokenIndex(int codePointIndex) {
			return cpIndexToTokenIndex.get(codePointIndex);
		}

		public int getTokenCount() {
			return tokenIndexToInfo.size();
		}

		public TokenInfo getInfoFromTokenIndex(int tokenIndex) {
			return tokenIndexToInfo.get(tokenIndex);
		}

		public SampleTokenizer set(String source) {
			this.source = source;
			int start = 0;
			int limit = 0;
			Builder<SampleTokenInfo> result = ImmutableList.<SampleTokenInfo>builder();
			Builder<Integer> result2 = ImmutableList.<Integer>builder();
			int tokenNumber = -1;
			while (limit < source.length()) {
				start = limit;
				Type currentType = null;
				for (Entry<Type, Pattern> typeAndPattern : regexes.entrySet()) {
					Type type = typeAndPattern.getKey();
					Matcher matcher = typeAndPattern.getValue().matcher(source);
					matcher.region(start, source.length());
					if (matcher.lookingAt()) {
						limit = matcher.end();
						currentType = type;
						break;
					}
				}
				++tokenNumber;
				for (int i = start; i < limit; ++i) {
					result2.add(tokenNumber);
				}
				result.add(new SampleTokenInfo(currentType, start, limit));
			}
			cpIndexToTokenIndex = result2.build();
			tokenIndexToInfo =  result.build();
			return this;
		}

		/**
		 * Provide an extra check for tokens 
		 */
		@Override
		public SourceError checkLine(int[] visualToLogical, int start, int limit) {
			Type type = getInfoFromTokenIndex(getTokenIndex(start)).getType();
			switch (type) {
			case LINECOM: // make sure that the start is the same visually as logically
				int cp1 = source.codePointAt(visualToLogical[start]);
				if (cp1 == '/') {
					int cp2 = source.codePointAt(visualToLogical[start+1]);
					if (cp2 == '/') {
						break;
					}
				}
				return SourceError.BAD_CODEPOINT_ORDER_IN_TOKEN;
			default: 
				break;
				// With this syntax, STRING, CPOINT, COMMENT are ok, because
				// we've check that the direction is monotonic, and the start and end are symmetric
				// With other tokens, there is no "initiating" sequence.
			}
			return SourceError.OK;
		}
	}
}
