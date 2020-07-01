package org.unicode.tools.emoji;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.props.UnicodeRelation;
import org.unicode.props.UnicodeRelation.SetMaker;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.CountEmoji.Bucket;
import org.unicode.tools.emoji.CountEmoji.Category;
import org.unicode.tools.emoji.EmojiOrder.MajorGroup;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.locale.XCldrStub.ImmutableMap;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.SpanCondition;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.VersionInfo;

public class CandidateData implements Transform<String, String>, EmojiDataSource {
    private static final String TEST_STRING = "👩‍🤝‍👩";
    private static final boolean SHOW_COMBOS = false;
    private static boolean DEBUG = CldrUtility.getProperty("CandidateData:DEBUG", false);

    // TODO Replace after values by using emojiOrdering.
    private static final UnicodeSet ZWJ_SET = new UnicodeSet(Emoji.JOINER,Emoji.JOINER);
    private static final Splitter SPLITTER_COMMA = Splitter.on(',').trimResults().omitEmptyStrings();
    private static final Joiner JOIN_COMMA = Joiner.on(", ");
    static final Splitter barSplit = Splitter.on('|').trimResults().omitEmptyStrings();
    static final Splitter equalSplit = Splitter.on('=').trimResults();

    public static final String CANDIDATE_VERSION = "10.0";

    public enum Quarter {
	_RELEASED,
	_2015Q1, _2015Q2, _2015Q3, _2015Q4,
	_2016Q1, _2016Q2, _2016Q3, _2016Q4,
	_2017Q1, _2017Q2, _2017Q3, _2017Q4
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

    public enum Status {
	Final_Candidate("Code points are final."), // final code points
	Draft_Candidate("Code points are draft."), // draft code points
	Provisional_Candidate("Temporary IDs are assigned, not code points.");  // no code points
	public final String comment;
	private Status(String _comment) {
	    comment= _comment;
	}
	public static Status fromString(String string) {
	    return valueOf(string.replace(' ', '_'));
	}
	public String toString() {
	    return name().replace('_', ' ');
	}
    }

    //    public static final class Info {
    //        String name;
    //        Set<String> keywords;
    //        Quarter quarter;
    //        String after;
    //        boolean Emoji_Modifier_Base;
    //        boolean Emoji_Gender_Base;
    //    }

    public static SetMaker SORTED_TREESET_MAKER = new SetMaker<Comparable>() {
	@Override
	public Set<Comparable> make() {
	    return new TreeSet<Comparable>(Collator.getInstance(Locale.ENGLISH));
	}
    };

    // TODO change to have a CandidateDatum object with this information, instead of separate maps
    private final List<Integer> order;
    private final UnicodeMap<String> categories = new UnicodeMap<>();
    private final UnicodeMap<Integer> suborder = new UnicodeMap<>();
    private final UnicodeMap<String> names = new UnicodeMap<>();
    private final UnicodeMap<String> unames = new UnicodeMap<>();
    private final UnicodeRelation<String> annotations = new UnicodeRelation<>(SORTED_TREESET_MAKER);
    private final UnicodeRelation<String> attributes = new UnicodeRelation<>();
    private final UnicodeMap<Quarter> quarters = new UnicodeMap<>();
    private final UnicodeMap<Status> statuses = new UnicodeMap<>();
    private final UnicodeSet singleCharacters = new UnicodeSet();
    private final UnicodeSet allCharacters = new UnicodeSet();
    private final UnicodeSet allNonProvisional = new UnicodeSet();
    private final UnicodeSet textPresentation = new UnicodeSet();
    private UnicodeSet provisional = new UnicodeSet();
    private UnicodeSet draft = new UnicodeSet();
    private final UnicodeSet emoji_Modifier_Base = new UnicodeSet();
    private final UnicodeSet emoji_Gender_Base = new UnicodeSet();
    private final UnicodeSet takesSign = new UnicodeSet();
    private final UnicodeSet emoji_Component = new UnicodeSet();
    private final UnicodeMap<String> after = new UnicodeMap<>();
    private final UnicodeMap<String> comments = new UnicodeMap<>();

    private final UnicodeMap<Set<String>> proposal = new UnicodeMap<>();

    private Map<String, Set<String>> existingToDraftCandidatesAfter = new HashMap<>();

    static final UnicodeSet SEQUENCE_MAKER = new UnicodeSet().add(Emoji.JOINER).add(EmojiData.MODIFIERS).freeze();

    private static final boolean LATER = false;
    //static final CandidateData PROPOSALS = new CandidateData("proposalData.txt");

    static final CandidateData SINGLE = new CandidateData("candidateData.txt");

    private static final int MAX_PER_LINE = 30;

    private CandidateData(String sourceFile) {
	String category = null;
	String source = null;
	Builder<Integer> _order = ImmutableList.builder();
	Quarter quarter = null;
	String afterItem = null;
	String proposalItem = null;
	Status status = null;

	date = new File(FileUtilities.getRelativeFileName(CandidateData.class, sourceFile)).lastModified();
	for (String line : FileUtilities.in(CandidateData.class, sourceFile)) {
	    line = line.trim();
	    try {
		if (line.startsWith("#") || line.isEmpty()) { // comment
		    continue;
		} else if (line.startsWith("U+")) { // data
		    if (line.contains("U+101900")) {
			int debug = 0;
		    }

		    if (TEST_STRING.equals(source)) {
			int debug = 0;
		    }
		    addCombosWithGenderAndSkin(source); // fix old source. we do it here so we know the properties

		    source = Utility.fromHex(line);

		    if (TEST_STRING.equals(source)) {
			int debug = 0;
		    }

		    if (source.contains("🦯")) {
			int debug = 0;
		    }
		    if (allCharacters.contains(source)) {
			throw new IllegalArgumentException(Utility.hex(source) + " occurs twice");
		    }
		    statuses.put(source, status);
		    allCharacters.add(source);
		    quarters.put(source, quarter);
		    addAfter(source, afterItem);
		    check(allCharacters, after);

		    proposal.put(source.replace(Emoji.EMOJI_VARIANT_STRING,""), ProposalData.cleanProposalString(proposalItem));
		    String afterString = "> " + afterItem;
		    Age_Values age = Emoji.VERSION_ENUM.get(afterItem.codePointAt(0));
		    if (age.compareTo(Age_Values.V10_0) >= 0) {
			afterString += " (" + Utility.hex(afterItem) + ")";
		    }
		    attributes.add(source, afterString);
		    if (SEQUENCE_MAKER.containsSome(source)) {
			attributes.add(source, "∈ sequences");
		    }
		    setCategoryAndSuborder(source, category);

		    // Add this once we decide what to do
		    // needs modification if we add skin tones
		    //                    if (LATER) for (String cp : Emoji.HAIR_BASE) {
		    //                        for (String mod : Emoji.HAIR_PIECES) {
		    //                            addCombo(mod, cp + Emoji.JOINER_STR + mod);
		    //                        }
		    //                    }

		} else { // must be category
		    int equalPos = line.indexOf('=');
		    String leftSide = equalPos < 0 ? line : line.substring(0,equalPos).trim();
		    String rightSide = equalPos < 0 ? null : line.substring(equalPos+1).trim();
		    switch(leftSide) {

		    // go before character
		    case "Status": 
			status = CandidateData.Status.fromString(rightSide);
			break;
		    case "Quarter": 
			quarter = CandidateData.Quarter.fromString(rightSide);
			break;
		    case "After": 
			afterItem = rightSide;
			if (afterItem.equals("🧑‍🦰")) {
			    int debug = 0;
			}
			category = EmojiOrder.STD_ORDER.getCategory(afterItem);
			break;
		    case "Proposal": 
			proposalItem = rightSide;
			break;

			// go after character
		    case "Name": 
			final String name = rightSide;
			if (name.contains("|")) {
			    throw new IllegalArgumentException("Name with | on " + line);
			}
			names.put(source, name.toLowerCase(Locale.ENGLISH));
			names.put(source.replaceAll(Emoji.EMOJI_VARIANT_STRING, ""), name.toLowerCase(Locale.ENGLISH));
			break;
		    case "UName":
			String oldName = names.get(source);
			if (!oldName.equalsIgnoreCase(rightSide)) {
			    final String uname = rightSide.toUpperCase(Locale.ROOT);
			    if (uname.contains("|")) {
				throw new IllegalArgumentException("UName with | on " + line);
			    }
			    unames.put(source, uname);
			}
			break;
		    case "Keywords": 
			if (rightSide.contains("dengue")) {
			    int debug = 0;
			}
			if (rightSide.contains(",")) {
			    System.err.println("Keywords contain: " + rightSide);
			}
			List<String> cleanKeywords = barSplit.splitToList(rightSide);
			for (String item : cleanKeywords) {
			    if (item.isEmpty()) {
				throw new IllegalArgumentException("Empty keyword on " + line);
			    }
			    //                            if (!item.equals(item.toLowerCase(Locale.ENGLISH))) {
			    //                                System.err.println("Warning: Cased Keyword on " + line); 
			    //                            }
			}
			annotations.addAll(source, cleanKeywords);
			break;
		    case "Emoji_Modifier_Base": 
			addAttribute(source, emoji_Modifier_Base, "∈ modifier_base");
			break;
		    case "Emoji_Gender_Base": 
			addAttribute(source, emoji_Gender_Base, "∈ gender_base");
			break;
		    case "Emoji_Component": 
			addAttribute(source, emoji_Component, "∈ component");
			break;
		    case "Comment":
			addComment(source, rightSide);
			break;

		    default: 
			throw new IllegalArgumentException(line);
		    }
		}
	    } catch (Exception e) {
		throw new IllegalArgumentException(line, e);
	    }
	}

	addCombosWithGenderAndSkin(source); // fix last source. We do it here so we know the properties

	// allCharacters.addAll(singleCharacters); // just to be sure
	UnicodeSet duplicates = new UnicodeSet(EmojiData.EMOJI_DATA_RELEASED.getAllEmojiWithDefectives())
		.retainAll(allCharacters)
		.addAll(Emoji.EXCLUSIONS);
	//	allCharacters.removeAll(singleCharacters);

	allCharacters.removeAll(duplicates).freeze();
	statuses.removeAll(duplicates).freeze();
	comments.freeze();
	categories.freeze();
	suborder.freeze();
	names.freeze();
	unames.freeze();
	quarters.freeze();
	after.freeze();
	proposal.freeze();

	check(allCharacters, after);

	annotations.removeAll(duplicates).freeze();
	attributes.removeAll(duplicates).freeze();

	// derived sets

	singleCharacters.addAll(allCharacters).removeAllStrings().freeze();

	Multimap<String,String> _existingToDraftCandidatesAfter = LinkedHashMultimap.create();
	for (Entry<String, String> entry : after.entrySet()) {
	    if (statuses.getValue(entry.getKey()) == Status.Draft_Candidate) {
		_existingToDraftCandidatesAfter.put(entry.getValue(), entry.getKey());
	    }
	}
	// ImmutableMultimaps have values as lists, so rework
	for (Entry<String, Collection<String>> entry : _existingToDraftCandidatesAfter.asMap().entrySet()) {
	    existingToDraftCandidatesAfter.put(entry.getKey(), (Set<String>) entry.getValue());
	}
	existingToDraftCandidatesAfter = ImmutableMap.copyOf(existingToDraftCandidatesAfter);

	order = _order.build();

	provisional = statuses.getSet(Status.Provisional_Candidate);
	draft = statuses.getSet(Status.Draft_Candidate);

	for (String s : allCharacters) {
	    if (!provisional.contains(s)) {
		allNonProvisional.add(s);
	    }
	    if (s.contains(Emoji.JOINER_STR + Emoji.FEMALE + Emoji.EMOJI_VARIANT_STRING) || s.contains(Emoji.JOINER_STR + Emoji.MALE + Emoji.EMOJI_VARIANT_STRING)) {
		takesSign.add(s.substring(0, s.length()-(Emoji.JOINER_STR + Emoji.FEMALE + Emoji.EMOJI_VARIANT_STRING).length()));
	    }

	}
	UnicodeMap<Age_Values> ages = Emoji.LATEST.loadEnum(UcdProperty.Age, Age_Values.class);
	Age_Values minAge = Age_Values.forName(Emoji.VERSION_LAST_RELEASED_UNICODE.getVersionString(2, 2));
	EmojiData releasedData = EmojiData.of(Emoji.VERSION_LAST_RELEASED);
	for (String s : allCharacters) {
	    // if not single code point, we don't care
	    int first = CharSequences.getSingleCodePoint(s);
	    if (first == Integer.MAX_VALUE) {
		continue; 
	    }
	    // if a character is in the released emoji data, we use its value
	    if (releasedData.getAllEmojiWithDefectives().contains(s)) {
		if (!releasedData.getEmojiPresentationSet().contains(s)) {
		    textPresentation.add(s); 
		}
		continue;
	    }
	    // if a character is future (private use) we don't care
	    if (UCharacter.getType(first) == UCharacter.PRIVATE_USE) {
		continue;
	    }
	    // if unassigned, we don't care
	    Age_Values age = ages.get(s);
	    if (age == Age_Values.Unassigned) {
		continue;
	    }
	    // otherwise if old, set to textPresentation
	    if (age.compareTo(minAge) <= 0) {
		textPresentation.add(s);
	    }
	}
	textPresentation.freeze();

	emoji_Modifier_Base.freeze();

	emoji_Gender_Base.freeze();
	takesSign.freeze();
	emoji_Component.freeze();
	proposal.freeze();
	// debugging
	if (!getAllEmojiWithDefectives().contains("🦯")) {
	    int debug = 0;
	}
	if (!checkData(this)) {
	    throw new IllegalArgumentException("Bad Data: Check UName values, etc.");
	}
    }

    private void addAfter(String source, String afterItem) {
	String base = EmojiData.EMOJI_DATA_BETA.getBaseRemovingModsGender(source);
	if (!after.containsKey(base)) {
	    after.put(base, afterItem);
	}
	after.put(source, afterItem);
    }

    private void check(UnicodeSet allCharacters2, UnicodeMap<String> after2) {
	if (allCharacters2.contains("👨‍🤝‍👨")) {
	    int debug = 0;
	}
	if (true) return;
	UnicodeSet keySet = after2.keySet();
	if (!allCharacters2.equals(keySet)) {
	    throw new IllegalArgumentException("missing data");
	}
    }

    private void addComment(String source, String rightSide) {
	String oldComment = comments.get(source);
	if (oldComment != null) {
	    rightSide = oldComment + "\n" + rightSide;
	}
	comments.put(source, rightSide);
    }

    private static boolean checkData(CandidateData instance) {
	boolean result = true;
	IndexUnicodeProperties iup = IndexUnicodeProperties.make();
	for (String item : instance.allCharacters) {
	    if (item.contains("🧱")) {
		int debug = 0;
	    }
	    // check that old emoji have emoji VS
	    // TODO

	    if (Emoji.GENDER_MARKERS.containsSome(item) 
		    || EmojiData.MODIFIERS.containsSome(item)
		    || Emoji.MAN_OR_WOMAN_OR_ADULT.containsSome(item)) {
		continue;
	    }

	    String name = instance.getName(item);
	    Set<String> keywords = instance.getAnnotations(item);
	    if (keywords.size() > 6) {
		System.err.println("Too many keywords? (" + keywords.size()
		+ "): " + name + ": " + keywords);
	    } else if (keywords.size() < 1) {
		System.err.println("Too few keywords? (" + keywords.size()
		+ "): " + name + ": " + keywords); 
	    }
	    if (item.contains(Emoji.JOINER_STR)) {
		continue;
	    }
	    Status status = instance.getStatus(item);
	    if (status != Status.Final_Candidate) {
		continue;
	    }
	    String cname = instance.getUName(item);
	    if (cname == null) {
		cname = name.toUpperCase(Locale.ROOT);
	    }
	    String uname = iup.getName(item," + ");
	    if (!uname.equals(cname)) {
		System.err.println(Utility.hex(item) + " — Names differ UCD: " + uname + "\t≠\tCLDR:" + cname);
		result = false;
	    }
	}
	return result;
    }

    private void setCategoryAndSuborder(String source, String category) {
	if (categories.containsKey(source)) {
	    throw new IllegalArgumentException("Already added!");
	}
	categories.put(source, category);
	suborder.put(source, suborder.size());
    }

    private void addCombosWithGenderAndSkin(String source) {
	if (source == null) {
	    return;
	}
	if (source.equals("👩‍🦯️")) {
	    int debug = 0;
	}

	boolean hasModifierBase = emoji_Modifier_Base.containsSome(source) 
		|| EmojiData.EMOJI_DATA_BETA.getModifierBasesRgi().containsSome(source);
	UnicodeSet all_Emoji_Modifier_Base = null;
	String fromNames = names.get(source);
	if (hasModifierBase) {
	    // find the point where it occurs; not efficient but we don't care
	    all_Emoji_Modifier_Base = new UnicodeSet(emoji_Modifier_Base)
		    .addAll(EmojiData.EMOJI_DATA_BETA.getModifierBases())
		    .remove("🤝") // special hack to remove skin color
		    .freeze();

	    addCombos(source, fromNames, "", source, "", ": ", all_Emoji_Modifier_Base, "", "");
	}

	int single = UnicodeSet.getSingleCodePoint(source);
	if (single == Integer.MAX_VALUE) {
	    return;
	}

	boolean isGenderBase = emoji_Gender_Base.contains(source);
	if (isGenderBase) {
	    boolean isMultiPerson = EmojiData.EMOJI_DATA_BETA.getMultiPersonGroupings().contains(source);

	    for (String gen : Emoji.GENDER_MARKERS) {
		String genSuffix = Emoji.JOINER_STR + gen + Emoji.EMOJI_VARIANT_STRING;
		String sourceName;
		if (isMultiPerson) {
		    String peopleReplacement = gen.equals(Emoji.MALE) ? "men" : "women";
		    sourceName = fromNames.replace("people", peopleReplacement);
		    if (sourceName.equals(fromNames)) {
			sourceName = peopleReplacement + " " + fromNames;
		    }
		} else {
		    String personReplacement = gen.equals(Emoji.MALE) ? "man" : "woman";
		    sourceName = fromNames.replace("person", personReplacement);
		    if (sourceName.equals(fromNames)) {
			sourceName = personReplacement + " " + fromNames;
		    }
		}
		String newSource = source + genSuffix;
		addCombo(source, sourceName, newSource, "");
		if (hasModifierBase) {
		    addCombos(newSource, sourceName, "", newSource, "", ": ", all_Emoji_Modifier_Base, "", "");
		    //
		    //                    for (String mod : EmojiData.MODIFIERS) {
		    //                        addCombo(source, sourceName, source + mod + genSuffix, ": " + EmojiData.EMOJI_DATA_BETA.getName(mod));
		    //                    }
		}
	    }
	}
	//        if (isGenderBase && hasModifierBase) {
	//            addComment(source, "Combinations of gender and skin-tone produce 17 more emoji sequences.");
	//        } else if (isGenderBase) {
	//            addComment(source, "Combinations of gender and skin-tone produce 2 more emoji sequences.");
	//        } else if (hasModifierBase) {
	//            addComment(source, "Combinations of gender and skin-tone produce 5 more emoji sequences.");
	//        }
	// Comment=There will be 55 emoji sequences with combinations of gender and skin-tone

    }

    private void addCombos(String source, String sourceName, String combined, String remainder, String nameSuffix, 
	    String separator, UnicodeSet forSplitting, String lastBase, String lastMod) {
	if (SHOW_COMBOS) System.out.println(
		"source: " + source
		+ ", combined: " + combined
		+ ", remainder: " + remainder
		+ ", nameSuffix: " + nameSuffix
		+ ", separator: " + separator
		);
	int start = forSplitting.span(remainder, SpanCondition.NOT_CONTAINED);
	if (start == remainder.length()) {
	    //          addCombo(source, source + mod + genSuffix, genPrefix, ": " + EmojiData.EMOJI_DATA_BETA.getName(mod));
	    addCombo(source, sourceName, combined + remainder, nameSuffix);
	    return;
	}
	int end = forSplitting.span(remainder, start, SpanCondition.CONTAINED);
	String base = remainder.substring(start, end);
	for (String mod : EmojiData.MODIFIERS) {
	    if (source.contains("🤝")) {
		int debug = 0;
	    }
	    // The following makes the triangle, but we retracted that.
	    // So make this equals (to prevent characters that are equivalent to singles)
	    if (base.equals(lastBase) && mod.compareTo(lastMod) == 0) {
		continue;
	    }
	    addCombos(source, sourceName, combined + remainder.substring(0, end) + mod, 
		    remainder.substring(end), nameSuffix + separator + EmojiData.EMOJI_DATA_BETA.getName(mod), ", ", forSplitting, base, mod);
	}
    }

    private void addAttribute(String source, UnicodeSet unicodeSet, String title) {
	if (source.codePointCount(0, source.length()) == 1) {
	    unicodeSet.add(source);
	    attributes.add(source, title);
	}
    }

    private void addCombo(String source, String sourceName, String combo, String nameSuffix) {
	String newName = sourceName + nameSuffix;
	if (SHOW_COMBOS) System.out.println("*addCombo: "
		+ "cp: " + source
		+ ", combo: " + combo
		+ ", newName: " + newName
		);
	//System.out.println("Adding: " + newName);
	allCharacters.add(combo);
	names.put(combo, newName);
	Status status = statuses.get(source);
	statuses.put(combo, status);
	String afterSource = after.get(source);
	if (afterSource == null) {
	    throw new IllegalArgumentException("Bad after value");
	}
	addAfter(combo, afterSource);
	check(allCharacters, after);

	proposal.put(combo.replace(Emoji.EMOJI_VARIANT_STRING, ""), proposal.get(source));

	setCategoryAndSuborder(combo, categories.get(source));

	//        if (Emoji.HAIR_PIECES.containsSome(cp)) { // HACK
	//            names.put(combo, 
	//                    EmojiData.EMOJI_DATA.getName(UTF16.valueOf(Character.codePointAt(combo, 0)))
	//                    + ": " +
	//                    getName(Character.codePointBefore(combo, combo.length())));
	//        }
    }

    public final Comparator<String> comparator = new Comparator<String>() {
	@Override
	public int compare(String o1, String o2) {
	    if ("🛼".equals(o1) || "🛼".equals(o1)) {
		int debug = 0;
	    }
	    if (o1 == o2) {
		return 0;
	    }
	    
	    // if both items have "real" collation data, use that.
	    int r1 = EmojiOrder.STD_ORDER.mapCollator.getOrdering(o1);
	    int r2 = EmojiOrder.STD_ORDER.mapCollator.getOrdering(o2);
	    if (r1 >= 0 && r2 >= 0) {
		return EmojiOrder.STD_ORDER.codepointCompare.compare(o1, o2);
	    }
	    
	    // if there are no after values, then neither item is in CandidateData
	    String after1 = after.get(o1);
	    String after2 = after.get(o2);
	    if (after1 == null && after1 == null) {
		return EmojiOrder.STD_ORDER.codepointCompare.compare(o1, o2);
	    }

	    //            // this getCategory falls back to the full emojit set.
	    //            String cat1 = getCategory(o1);
	    //            int catOrder1 = EmojiOrder.STD_ORDER.getGroupOrder(cat1); 
	    //
	    //            String cat2 = getCategory(o2);
	    //            int catOrder2 = EmojiOrder.STD_ORDER.getGroupOrder(cat2);
	    //            if (catOrder1 != catOrder2) {
	    //                return catOrder1 > catOrder2 ? 1 : -1;
	    //            }

	    // if the after values are different, return them
	    // either either is null, then the character is outside of 

	    if (after1 == null) {
		after1 = o1;
	    }
	    if (after2 == null) {
		after2 = o2;
	    }
	    if (!after1.equals(after2)) {
		return EmojiOrder.STD_ORDER.codepointCompare.compare(after1, after2);
	    }

	    // The after values are identical, so get the suborders
	    // If one is missing (they both can't be simultaneously missing), use -1 to get before
	    Integer so1 = suborder.get(o1);
	    int so1i = so1 == null ? -1 : so1;
	    Integer so2 = suborder.get(o2);
	    int so2i = so2 == null ? -1 : so2;

	    return so1i-so2i;
	}
    };
    private long date;

    /**
     * @return the characters
     */
    public UnicodeSet getCharacters() {
	return singleCharacters;
    }

    public UnicodeSet getAllCharacters() {
	return allCharacters;
    }

    public static CandidateData getInstance() {
	return SINGLE;
    }

    //    public static CandidateData getProposalInstance() {
    //        return PROPOSALS;
    //    }

    public UnicodeSet keySet() {
	return names.keySet();
    }

    public UnicodeMap<String> namesMap() {
	return names;
    }

    public String getName(String source) {
	return names.get(source);
    }

    public String getName(int source) {
	return names.get(source);
    }

    public String getUName(String source) {
	return unames.get(source);
    }
    public String getUName(int source) {
	return unames.get(source);
    }

    public String getShorterName(String source) {
	return transform(source);
    }

    public Set<String> getAnnotations(String source) {
	Set<String> list = annotations.get(source);
	return list == null ? Collections.<String>emptySet() : new TreeSet<>(list);
    }
    public Set<String> getAnnotations(int source) {
	return CldrUtility.ifNull(annotations.get(source), Collections.<String>emptySet());
    }

    public Set<String> getAttributes(String source) {
	Set<String> list = attributes.get(source);
	return list == null ? Collections.<String>emptySet() : new TreeSet<>(list);
    }

    public Set<String> getProposal(String source) {
	return proposal.get(source.replace(Emoji.EMOJI_VARIANT_STRING, ""));
    }

    public CandidateData.Quarter getQuarter(String source) {
	return quarters.get(source);
    }
    public CandidateData.Quarter getQuarter(int source) {
	return quarters.get(source);
    }

    public Status getStatus(String source) {
	return statuses.get(source);
    }
    public Status getStatus(int source) {
	return statuses.get(source);
    }

    public String getComment(String source) {
	return comments.get(source);
    }
    public String getComment(int source) {
	return comments.get(source);
    }

    public String getCategory(int source) {
	String result = EmojiOrder.STD_ORDER.charactersToOrdering.get(source);
	return result != null ? result : categories.get(source);
    }
    public String getCategory(String source) {
	String result = EmojiOrder.STD_ORDER.charactersToOrdering.get(source);
	return result != null ? result : categories.get(source);
    }

    public List<Integer> getOrder() {
	return order;
    }

    /**
     * @return the MajorGroup
     */
    public MajorGroup getMajorGroup(String s) {
	MajorGroup result = EmojiOrder.STD_ORDER.majorGroupings.get(s);
	return result != null ? result : EmojiOrder.STD_ORDER.getMajorGroupFromCategory(getCategory(s));
    }

    public MajorGroup getMajorGroup(int s) {
	MajorGroup result = EmojiOrder.STD_ORDER.majorGroupings.get(s);
	return result != null ? result :EmojiOrder.STD_ORDER.getMajorGroupFromCategory(getCategory(s));
    }

    public MajorGroup getMajorGroupFromCategory(String category) {
	return EmojiOrder.STD_ORDER.getMajorGroupFromCategory(category);
    }

    public static void main(String[] args) {
	DEBUG = true;
	CandidateData candidateData = CandidateData.getInstance();
	if (args.length == 0) {
	    throw new IllegalArgumentException();
	}
	int count = 0;
	for (String arg : args) {
	    switch (arg) {
	    case "proposals": 
		generateProposalData(candidateData, Status.Provisional_Candidate); 
		generateProposalData(candidateData, Status.Draft_Candidate); 
		generateProposalData(candidateData, Status.Final_Candidate); 
		++count;
		break;
	    case "order": 	
		IndexUnicodeProperties iup = IndexUnicodeProperties.make(Emoji.VERSION_BETA);
		UnicodeMap<General_Category_Values> gc = iup.loadEnum(UcdProperty.General_Category, UcdPropertyValues.General_Category_Values.class);
		UnicodeSet unassigned = gc.getSet(UcdPropertyValues.General_Category_Values.Unassigned);
		showOrdering(candidateData, unassigned);
		++count;
		break;
	    default: 	    
		throw new IllegalArgumentException("Bad argument: " + arg);

	    }
	}
	if (count == 0) {
	    throw new IllegalArgumentException("No arguments found");
	}

	//        try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(System.out))) {
	//            new EmojiDataSourceCombined().showOrderingInterleaved(MAX_PER_LINE, out);
	//        } catch (IOException e) {
	//            throw new ICUUncheckedIOException(e);
	//        }

	//        if (true) return;

	//        for (Status status : Status.values()) {
	//            UnicodeSet items = instance.statuses.getSet(status);
	//            System.out.println(status + "\t" + items.size());
	//        }
	//        generateProposalData(instance);
	//        showOrdering(instance);

	//	    if (true) return;
	//	    System.out.println(candidateData.getEmojiWithVariants());
	//	    if (candidateData.getZwjSequencesNormal().contains("👨‍🤝‍👨"))  {
	//		throw new IllegalArgumentException();
	//	    }
	//	    if (candidateData.getAllCharacters().contains("👨‍🤝‍👨"))  {
	//		throw new IllegalArgumentException();
	//	    }
	//
	//	    UnicodeSet candChars = candidateData.getAllCharacters();
	//	    System.out.println(candChars.size() + "\t" + candChars);
	//	    System.out.println("all\t" + DebugUtilities.composeStringsWhen("•", candChars, s -> s.contains(Emoji.TRANSGENDER)));
	//	    UnicodeSet withoutDefectives = candidateData.getAllEmojiWithoutDefectives();
	//	    System.out.println(withoutDefectives.size() + "\t" + withoutDefectives);
	//	    System.out.println("wd\t" + DebugUtilities.composeStringsWhen("•", withoutDefectives, s -> s.contains(Emoji.TRANSGENDER)));
	//
	//	    String added = EmojiData.EMOJI_DATA_BETA.addEmojiVariants(Emoji.TRANSGENDER);
	//	    System.out.println(Utility.hex(Emoji.TRANSFLAG));
	//	    System.out.println(Utility.hex(added));
	//	    System.out.println("\nCandidates\n");
	//	    //showCandidateData(candidateData, true, true);
	//	    // CandidateData.generateProposalData(CandidateData.getInstance());
	//	    //showCandidateData(CandidateData.getProposalInstance(), true);
	//	    System.out.println("\nCandidates - Ordering\n");
    }

    private static void generateProposalData(CandidateData instance, Status status) {
	System.out.println("\n#Data for proposalData.txt\n");
	//1F931;  L2/16-280,L2/16-282r;   BREAST-FEEDING   
	Set<String> done = new HashSet<>();
	UnicodeSet missing = new UnicodeSet();
	System.out.println("# " + status);
	for (String item : instance.allCharacters) {
	    if (instance.statuses.get(item) != status
//		    || EmojiData.MODIFIERS.containsSome(item)
//		    || Emoji.GENDER_MARKERS.containsSome(item)
		    ) {
		continue;
	    }
	    String skeleton = ProposalData.getSkeleton(item);
	    if (done.contains(skeleton)) {
		continue;
	    }
	    done.add(skeleton);
	    Set<String> proposals = instance.getProposal(item);
	    if (proposals == null) {
		missing.add(item);
	    }
	    System.out.println(Utility.hex(skeleton)
		    + "; " + CollectionUtilities.join(proposals, ", ")
		    + "; " + instance.getName(item)
		    + "; " + proposals
		    );
	}
	if (missing.isEmpty()) {
	    return;
	}
	for (String item : missing) {
	    System.out.println(Utility.hex(item)
		    + "; " + "XXX"
		    + "; " + instance.getName(item));
	}
    }

    public static void showOrdering(CandidateData instance, UnicodeSet discard) {

	boolean xx1 = instance.allCharacters.contains("🤵‍♂");
	boolean xx2 = instance.allCharacters.contains("🏳️‍⚧️");
	instance.comparator.compare("🤵", "🏳️‍⚧️");

	if (DEBUG) System.out.println("\nOrdering Data\n");

	CountEmoji cm = new CountEmoji();
	Set<String> sorted = instance.getAllCharacters().addAllTo(
		new TreeSet<>(instance.comparator));
	//        Map<String,List<String>> baseToList = new TreeMap<>(EmojiOrder.STD_ORDER.codepointCompare);
	//        baseToList.
	//        for (String item : instance.suborder) {
	//            //            if (EmojiData.MODIFIERS.containsSome(item)) {
	//            //                continue; // added automatically
	//            //            }
	//            String base = instance.after.get(item);
	//            List<String> list = baseToList.get(base);
	//            if (list == null) {
	//                baseToList.put(base, list = new ArrayList<>());
	//            }
	//            list.add(item);
	//        }
	for (String subItem : sorted) {
	    if (discard.containsSome(subItem)) {
		continue;
	    }
	    if (DEBUG) System.out.println(
		    instance.after.get(subItem) 
		    + "\t" + subItem 
		    + "\t" + Utility.hex(subItem) 
		    + "\t" + instance.categories.get(subItem) 
		    + "\t" + instance.suborder.get(subItem) 
		    + "\t" + instance.getName(subItem)
		    + "\tkw:" + instance.getAnnotations(subItem)
		    + "\tucd:" + instance.getUName(subItem)
		    );
	    if (instance.getAfter(subItem) == null) {
		throw new IllegalArgumentException();
	    }
	    if (subItem.startsWith("🤵‍♂")) {
		int debug = 0;
	    }
	    if (instance.getStatus(subItem) != Status.Provisional_Candidate) {
		cm.add(subItem, instance);
	    }
	}
	EmojiOrder ordering = EmojiOrder.of(Emoji.VERSION_BETA);
	//        for (String s : instance.allCharacters) {
	//            System.out.println(s + "\t" + ordering.getCategory(s));
	//        }
	//        for (String s : new UnicodeSet("[{👨‍🦰️}{👨‍🦱️}{👨‍🦲️}{👨‍🦳️}{👨🏻‍🦰️}{👨🏻‍🦱️}{👨🏻‍🦲️}{👨🏻‍🦳️}{👨🏼‍🦰️}{👨🏼‍🦱️}{👨🏼‍🦲️}{👨🏼‍🦳️}{👨🏽‍🦰️}{👨🏽‍🦱️}{👨🏽‍🦲️}{👨🏽‍🦳️}{👨🏾‍🦰️}{👨🏾‍🦱️}{👨🏾‍🦲️}{👨🏾‍🦳️}{👨🏿‍🦰️}{👨🏿‍🦱️}{👨🏿‍🦲️}{👨🏿‍🦳️}{👩‍🦰️}{👩‍🦱️}{👩‍🦲️}{👩‍🦳️}{👩🏻‍🦰️}{👩🏻‍🦱️}{👩🏻‍🦲️}{👩🏻‍🦳️}{👩🏼‍🦰️}{👩🏼‍🦱️}{👩🏼‍🦲️}{👩🏼‍🦳️}{👩🏽‍🦰️}{👩🏽‍🦱️}{👩🏽‍🦲️}{👩🏽‍🦳️}{👩🏾‍🦰️}{👩🏾‍🦱️}{👩🏾‍🦲️}{👩🏾‍🦳️}{👩🏿‍🦰️}{👩🏿‍🦱️}{👩🏿‍🦲️}{👩🏿‍🦳️}{🦸️‍♀️}{🦸️‍♂️}{🦹️‍♀️}{🦹️‍♂️}]")) {
	//            if (DEBUG) System.out.println(s + "\t" + ordering.getCategory(s));
	//        }
	if (DEBUG) System.out.println("\n\nSO\tType\tCategory\tHex\tCldr Name\tUcd Name");
	int sortOrder = 0;
	String lastSubitem = null;
	for (Category evalue : Category.values()) {
	    Bucket bucket = cm.buckets.get(evalue);
	    if (bucket == null) continue;
	    for (MajorGroup maj : MajorGroup.values()) {
		UnicodeSet uset = bucket.sets.getSet(maj);
		if (uset.isEmpty()) continue;
		Set<String> items = uset.addAllTo(new TreeSet<>(instance.comparator));
		// if (DEBUG) System.out.println(evalue.toStringPlain() + "\t" + maj.toPlainString() + "\t" + items.size());
		for (String subItem : items) {
		    String uName = instance.getUName(subItem);
		    if (DEBUG) System.out.println(
			    instance.getAfter(subItem) 
			    + "\t" + ++sortOrder
			    + "\t" + evalue.toStringPlain() 
			    + "\t" + maj.toPlainString() 
			    + "\tU+" + Utility.hex(subItem, ", U+")
			    + "\t" + instance.getName(subItem) 
			    + (uName != null ? "\t" + uName : "")
			    );
		    if (lastSubitem != null) {
			if (instance.comparator.compare(lastSubitem, subItem) >= 0) {
			    int debug = 0;
			    // throw new IllegalArgumentException("Failed ordering");
			}
		    }
		    lastSubitem = subItem;
		}
	    }
	}
    }



    public Set<String> getCandidatesAfter(String s) {
	return (Set<String>) existingToDraftCandidatesAfter.get(s);
    }

    public String getAfter(String s) {
	return after.get(s);
    }

    private static void showCandidateData(CandidateData cd, boolean sortWithCandidateComparator, boolean retainOnlyNew) {
	cd.comparator.compare(Utility.fromHex("1F9B5"), Utility.fromHex("1F9B6 1F3FF"));

	if (DEBUG) System.out.println("Code Point\tChart\tGlyph\tSample\tColored Glyph\tName");
	UnicodeSet chars2 = cd.getAllEmojiWithoutDefectives();
	if (retainOnlyNew) {
	    chars2 = new UnicodeSet(chars2).removeAll(EmojiData.EMOJI_DATA_BETA.getAllEmojiWithoutDefectives()).freeze();
	}
	List<String> sorted = new ArrayList<>(chars2.addAllTo(new TreeSet<String>(
		sortWithCandidateComparator 
		? cd.comparator 
			: EmojiOrder.STD_ORDER.codepointCompare)));
	String lastCategory = null;
	MajorGroup lastMajorGroup = null;
	List<String> lastCategoryList = new ArrayList<String>();

	if (DEBUG) {
	    System.out.println("chars2: " + chars2.size()+ "\n" + DebugUtilities.composeStringsWhen("•", chars2, s1 -> s1.contains(Emoji.TRANSGENDER)));
	    System.out.println("sorted: " + sorted.size()+ "\n" + DebugUtilities.composeStringsWhen("•", sorted, s2 -> s2.contains(Emoji.TRANSGENDER)));
	}
	Map<String,String> errors = new LinkedHashMap<>();
	for (String s : sorted) {
	    if (s.contains(Emoji.TRANSGENDER)) {
		int debug = 0;
	    }
	    String category = cd.getCategory(s);
	    MajorGroup majorGroup = cd.getMajorGroup(s);
	    if (majorGroup == null) {
		cd.getMajorGroup(s);  
	    }
	    if (majorGroup != lastMajorGroup) {
		if (DEBUG) System.out.println("\n@ " + majorGroup.name());
		lastMajorGroup = majorGroup; 
	    }
	    if (!Objects.equal(category,lastCategory)) {
		if (lastCategory != null) {
		    if (DEBUG) System.out.println("# lastCategory: " + lastCategory + " = \t" + CollectionUtilities.join(lastCategoryList, " "));
		}
		if (DEBUG) System.out.println(category);
		lastCategory = category; 
		lastCategoryList.clear();
	    }
	    lastCategoryList.add(s);
	    if (cd.getProposal(s) == null) {
		errors.put(s, "No proposal value for: ");
	    }
	    if (DEBUG) System.out.println(Utility.hex(s) 
		    + "\t" + s 
		    //                    + "\t" + cd.getQuarter(s) 
		    + "\t" + cd.getName(s)
		    + "\t" + cd.getProposal(s) 
		    + "\t" + CollectionUtilities.join(cd.getAnnotations(s), " | ")
		    );
	    //            for (String annotation :  cd.getAnnotations(s)) {
	    //                if (DEBUG) System.out.println("• " + annotation);
	    //            }
	}
	if (DEBUG) System.out.println("# list: " + lastCategory + " = \t" + CollectionUtilities.join(lastCategoryList, " "));
	if (errors.isEmpty()) {
	    errors.forEach((String key, String value) -> System.out.println(Utility.hex(key) + "\t" + value));
	    throw new IllegalArgumentException("Failed");
	}
    }

    private static void showLast(UnicodeSet last) {
	if (DEBUG) System.out.println("# Total: " + last.size());
	if (DEBUG) System.out.println("# USet: " + CollectionUtilities.join(
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
	    if (source.contains(EmojiData.ZWJ_HANDSHAKE_ZWJ)) {
		temp = EmojiData.EMOJI_DATA_BETA.getFallbackName(source);
		break main;
	    }
	    switch(CountEmoji.Category.getBucket(source)) {
	    case component:
		temp = UCharacter.getName(EmojiData.removeEmojiVariants(source), "+");
		break;
	    case character:
	    case flag_seq:
	    case keycap_seq:
	    case tag_seq:
		break;
	    default:
		String replacement = null;
		int trailPos = source.lastIndexOf(Emoji.JOINER_STR);
		if (trailPos > 0) {
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
		break;
	    }
	}
	return temp == null ? temp : temp.toLowerCase(Locale.ROOT);
    }

    enum MatchInclusion {includeFilterMatches, excludeFilterMatches}

    private UnicodeSet addWithCharFilter(UnicodeSet source, Predicate<String> filter) {
	UnicodeSet result = new UnicodeSet();
	for (String s : source) {
	    if (filter.test(s)) {
		result.add(s);
	    }
	}
	return result.freeze();
    }

    private UnicodeSet addWithCharFilter(UnicodeSet source, UnicodeSet filter) {
	return new UnicodeSet(source).retainAll(filter).freeze();
    }


    @Override
    public UnicodeSet getEmojiComponents() {
	return addWithCharFilter(emoji_Component, draft);
    }

    @Override
    public UnicodeSet getSingletonsWithDefectives() {
	return addWithCharFilter(singleCharacters, draft);
    }

    @Override
    public UnicodeSet getEmojiPresentationSet() {
	return addWithCharFilter(singleCharacters, s -> draft.contains(s)
		&& !getTextPresentationSet().containsSome(s));
    }

    @Override
    public UnicodeSet getModifierBases() {
	return addWithCharFilter(emoji_Modifier_Base, draft);
    }

    @Override
    public UnicodeSet getExtendedPictographic() {
	return addWithCharFilter(singleCharacters, draft);
    }

    @Override
    public UnicodeSet getTagSequences() {
	return addWithCharFilter(draft, s -> Emoji.TAGS.containsSome(s));
    }

    @Override
    public UnicodeSet getModifierSequences() {
	return addWithCharFilter(draft, s -> EmojiData.MODIFIERS.containsSome(s)
		&& !ZWJ_SET.containsSome(s));
    }

    @Override
    public UnicodeSet getFlagSequences() {
	return addWithCharFilter(draft, s -> Emoji.REGIONAL_INDICATORS.containsAll(s));
    }

    @Override
    public UnicodeSet getZwjSequencesNormal() {
	return addWithCharFilter(draft, s -> ZWJ_SET.containsSome(s));
    }

    @Override
    public UnicodeSet getEmojiWithVariants() {
	UnicodeSet result = new UnicodeSet();
	for (String s : draft) {
	    // check all the draft strings, and return the characters before an Emoji.EMOJI_VARIANT
	    if (Emoji.EMOJI_VARIANTS.containsSome(s)) {
		int last = -1;
		for (int ch : CharSequences.codePoints(s)) {
		    if (ch == Emoji.EMOJI_VARIANT) {
			result.add(last);
		    }
		    last = ch;
		}
	    }
	}
	return result;
    }

    @Override
    public UnicodeSet getAllEmojiWithoutDefectives() {
	return addWithCharFilter(draft, 
		x -> {
		    if (x.contains(Emoji.TRANSGENDER)) {
			int debug = 0;
		    }
		    return x.equals(EmojiData.EMOJI_DATA_BETA.addEmojiVariants(x));
		});
    }

    @Override
    public UnicodeSet getTextPresentationSet() {
	return addWithCharFilter(textPresentation, draft);
    }

    @Override
    public UnicodeSet getAllEmojiWithDefectives() {
	return allNonProvisional;
    }

    @Override
    public UnicodeSet getGenderBases() {
	return addWithCharFilter(emoji_Gender_Base, draft);
    }

    @Override
    public UnicodeSet getSingletonsWithoutDefectives() {
	return addWithCharFilter(singleCharacters, s -> draft.contains(s)
		&& !getEmojiComponents().containsSome(s));
    }

    @Override
    public UnicodeMap<String> getRawNames() {
	return names;
    }

    public String getUnicodeName(String source) {
	String item = unames.get(source);
	return item != null ? item : names.get(source);
    }

    public VersionInfo getNewest(String s) {
	return BirthInfo.getVersionInfo(s);
//	Age_Values result = Emoji.getNewest(s);
//	return result == Age_Values.Unassigned ? Emoji.UCD11 
//		: VersionInfo.getInstance(result.getShortName());
    }

    @Override
    public UnicodeSet getTakesSign() {
	return addWithCharFilter(takesSign, draft);
    }

    public UnicodeSet getAllCharacters(Status status) {
	switch(status) {
	case Provisional_Candidate: return provisional;
	case Draft_Candidate : return draft;
	default: throw new IllegalArgumentException();
	}
    }

    @Override
    public UnicodeSet getKeycapSequences() {
	return UnicodeSet.EMPTY;
    }

    @Override
    public String addEmojiVariants(String s1) {
	throw new UnsupportedOperationException();
    }

    public String addEmojiVariants(String s1, Emoji.Qualified qualified) {
	throw new UnsupportedOperationException();
    }

    @Override
    public String getVersionString() {
	return getPlainVersion();
    }
    
    @Override
    public String getPlainVersion() {
	return "candidates:" + DateFormat.getInstanceForSkeleton("yyyyMMdd", ULocale.ROOT).format(date);
    }

    /** We don't expect to have any more of these */
    @Override
    public UnicodeSet getExplicitGender() {
	return UnicodeSet.EMPTY;
    }

    /** We don't expect to have any more of these */
    @Override
    public UnicodeSet getMultiPersonGroupings() {
	return UnicodeSet.EMPTY;
    }

    @Override
    public UnicodeSet getModifierBasesRgi() {
	return getModifierBases();
    }

    @Override
    public UnicodeSet getAllEmojiWithoutDefectivesOrModifiers() {
	return addWithCharFilter(draft, s -> EmojiData.MODIFIERS.containsNone(s));
    }

}
