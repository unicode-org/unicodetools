package org.unicode.tools.emoji;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.props.UnicodeRelation;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.EmojiOrder.MajorGroup;
import org.unicode.tools.emoji.GenerateEmojiData.ZwjType;

import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

public class CandidateData implements Transform<String, String> {

	public static final String CANDIDATE_VERSION = "10.0";

	public enum Quarter {
		_RELEASED,
		_2015Q1, _2015Q2, _2015Q3, _2015Q4,
		_2016Q1, _2016Q2, _2016Q3, _2016Q4,
		_2017Q1
		;
		public boolean isFuture() {
			return compareTo(_2016Q1) >= 0;
		}
		static Quarter fromString(String item) {
			return valueOf('_'+item);
		}
		public String toString() {
			return name().substring(1);
		}
	}

	private final List<Integer> order;
	private final UnicodeMap<String> categories = new UnicodeMap<>();
	private final UnicodeMap<String> names = new UnicodeMap<>();
	private final UnicodeRelation<String> annotations = new UnicodeRelation<>();
	private final UnicodeMap<CandidateData.Quarter> quarters = new UnicodeMap<>();
	private final UnicodeSet characters;

	static final Splitter TAB = Splitter.on('\t');
	static final CandidateData SINGLE = new CandidateData();

	private CandidateData() {
		UnicodeMap<CandidateData.Quarter> quartersForChars = quarters;
		String category = null;
		String source = null;
		Builder<Integer> _order = ImmutableList.builder();
		for (String line : FileUtilities.in(CandidateData.class, "candidateData.txt")) {
			try {
				if (line.startsWith("#")) { // annotation
					continue;
				} else if (line.startsWith("•") || line.startsWith("×")) { // annotation
					annotations.add(source,line.charAt(0) + " " + line.substring(1).trim());
				} else if (line.startsWith("U+")) { // data
					List<String> parts = TAB.splitToList(line);
					source = Utility.fromHex(parts.get(0));
					_order.add(source.codePointAt(0));
					final String quarter = parts.get(3).trim();
					quartersForChars.put(source, CandidateData.Quarter.fromString(quarter));
					final String name = parts.get(4).trim();
					names.put(source, name);
					//                    if (!EmojiOrder.STD_ORDER.groupOrder.containsKey(category)) {
					//                        throw new IllegalArgumentException("Illegal category: " + category + ". Must be in: " + EmojiOrder.STD_ORDER.groupOrder.keySet());
					//                    }
					categories.put(source, category);
				} else { // must be category
					category = line.trim();
					line= line.trim();
				}
			} catch (Exception e) {
				throw new IllegalArgumentException(line, e);
			}
		}
		order = _order.build();
		categories.freeze();
		names.freeze();
		annotations.freeze();
		quarters.freeze();
		characters = categories.keySet().freeze();
	}

	Comparator<String> comparator = new Comparator<String>() {
		@Override
		public int compare(String o1, String o2) {
			if (o1 == o2) {
				return 0;
			}
			boolean f1 = quarters.get(o1).isFuture();
			boolean f2 = quarters.get(o2).isFuture();
			if (f1 != f2) {
				return f1 ? 1 : -1;
			}
			int catOrder1 = EmojiOrder.STD_ORDER.getGroupOrder(getCategory(o1));
			int catOrder2 = EmojiOrder.STD_ORDER.getGroupOrder(getCategory(o2));
			if (catOrder1 != catOrder2) {
				return catOrder1 > catOrder2 ? 1 : -1;
			}
			return EmojiOrder.FULL_COMPARATOR.compare(o1, o2);
		}
	};

	/**
	 * @return the characters
	 */
	public UnicodeSet getCharacters() {
		return characters;
	}

	public static CandidateData getInstance() {
		return SINGLE;
	}

	public UnicodeSet keySet() {
		return names.keySet();
	}

	public UnicodeMap<String> namesMap() {
		return names;
	}

	public String getName(String source) {
		return names.get(source);
	}

	public String getShorterName(String source) {
		return transform(source);
	}

	public Set<String> getAnnotations(String source) {
		return CldrUtility.ifNull(annotations.get(source), Collections.<String>emptySet());
	}
	public CandidateData.Quarter getQuarter(String source) {
		return quarters.get(source);
	}

	public String getName(int source) {
		return names.get(source);
	}

	public String getCategory(int source) {
		String result = EmojiOrder.STD_ORDER.charactersToOrdering.get(source);
		return result != null ? result : categories.get(source);
	}
	public String getCategory(String source) {
		String result = EmojiOrder.STD_ORDER.charactersToOrdering.get(source);
		return result != null ? result : categories.get(source);
	}

	public Set<String> getAnnotations(int source) {
		return CldrUtility.ifNull(annotations.get(source), Collections.<String>emptySet());
	}
	public CandidateData.Quarter getQuarter(int source) {
		return quarters.get(source);
	}

	public List<Integer> getOrder() {
		return order;
	}

	/**
	 * @return the MajorGroup
	 */
	public MajorGroup getMajorGroup(String s) {
		MajorGroup result = EmojiOrder.STD_ORDER.majorGroupings.get(s);
		return result != null ? result :EmojiOrder.STD_ORDER.getMajorGroupFromCategory(getCategory(s));
	}
	public MajorGroup getMajorGroup(int s) {
		MajorGroup result = EmojiOrder.STD_ORDER.majorGroupings.get(s);
		return result != null ? result :EmojiOrder.STD_ORDER.getMajorGroupFromCategory(getCategory(s));
	}

	public static void main(String[] args) {
		CandidateData cd = CandidateData.getInstance();        
		System.out.println("Code Point\tChart\tGlyph\tSample\tColored Glyph\tName");
		final UnicodeSet chars2 = cd.getCharacters();
		List<String> sorted = new ArrayList<>(chars2.addAllTo(new TreeSet<String>(cd.comparator)));
		String lastCategory = null;
		MajorGroup lastMajorGroup = null;
		List<String> lastCategoryList = new ArrayList<String>();
		for (String s : sorted) {
			String category = cd.getCategory(s);
			MajorGroup majorGroup = cd.getMajorGroup(s);
			if (majorGroup == null) {
				cd.getMajorGroup(s);  
			}
			if (majorGroup != lastMajorGroup) {
				System.out.println("\n@ " + majorGroup.name());
				lastMajorGroup = majorGroup; 
			}
			if (!Objects.equal(category,lastCategory)) {
				if (lastCategory != null) {
					System.out.println("# list: " + lastCategory + " = \t" + CollectionUtilities.join(lastCategoryList, " "));
				}
				System.out.println(category);
				lastCategory = category; 
				lastCategoryList.clear();
			}
			lastCategoryList.add(s);
			System.out.println(Utility.hex(s) 
					+ "\t" + s 
					+ "\t" + cd.getQuarter(s) 
					+ "\t" + cd.getName(s));
			for (String annotation :  cd.getAnnotations(s)) {
				System.out.println("• " + annotation);
			}
		}
		System.out.println("# list: " + lastCategory + " = \t" + CollectionUtilities.join(lastCategoryList, " "));


		//        for (int s : cd.getOrder()) {
		//            String cat = cd.getCategory(s);
		//            if (!cat.equals(oldCat)) {
		//                if (!last.isEmpty()) {
		//                    showLast(last);
		//                }
		//                System.out.println("\n" + cat + "\n");
		//                oldCat = cat;
		//            }
		//            last.add(s);
		//            total.add(s);
		//            System.out.println(Utility.hex(s) + "\t" + cd.getQuarter(s) + "\t" + cd.getName(s) + "\t" + cd.getAnnotations(s));
		//        }
		//        showLast(last);
		//        showLast(total);
	}

	private static void showLast(UnicodeSet last) {
		System.out.println("# Total: " + last.size());
		System.out.println("# USet: " + CollectionUtilities.join(
				last.addAllTo(new LinkedHashSet<>())," ") + "\n");
		last.clear();
	}

	@Override
	public String transform(String source) {
		String temp = getName(source);
		main: {
			if ("I LOVE YOU HAND SIGN".equals(temp)) {
				temp = "LOVE-YOU HAND";
				break main;
			}
			if (temp != null) {
				break main;
			}
			switch(ZwjType.getType(source)) {
			default:
				break;
			case activity:
			case roleWithSign:
			case gestures:
				String replacement = null;
				int trailPos = source.lastIndexOf(Emoji.JOINER_STR);
				if (trailPos < 0) {
					break main;
				}
				String ending = source.substring(trailPos);
				switch (ending.replace(Emoji.EMOJI_VARIANT_STRING, "")) {
				case Emoji.JOINER_STR + Emoji.MALE:
					replacement = "MAN";
				break;
				case Emoji.JOINER_STR + Emoji.FEMALE:
					replacement = "WOMAN";

				}
				if (replacement != null) {
					temp = getName(source.substring(0, source.length() - ending.length()));
				}
				if (temp != null) {
					if (temp.contains("PERSON")) {
						temp = temp.replaceAll("PERSON", replacement);
					} else if (temp.contains("person")) {
						temp = temp.replaceAll("person", replacement);
					} else {
						temp = replacement + " " + temp;
					}
				}
			}
		}
		return temp == null ? temp : temp.toLowerCase(Locale.ROOT);
	}
}
