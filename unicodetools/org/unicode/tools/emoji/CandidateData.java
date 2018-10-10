package org.unicode.tools.emoji;

import java.text.Collator;
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
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.props.UnicodeRelation;
import org.unicode.props.UnicodeRelation.SetMaker;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.CountEmoji.Bucket;
import org.unicode.tools.emoji.CountEmoji.Category;
import org.unicode.tools.emoji.CountEmoji.ZwjType;
import org.unicode.tools.emoji.EmojiOrder.MajorGroup;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUException;
import com.ibm.icu.util.VersionInfo;

public class CandidateData implements Transform<String, String>, EmojiDataSource {
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

    static final UnicodeSet SEQUENCE_MAKER = new UnicodeSet().add(Emoji.JOINER).add(EmojiData.MODIFIERS).freeze();

    private static final boolean LATER = false;
    //static final CandidateData PROPOSALS = new CandidateData("proposalData.txt");

    static final CandidateData SINGLE = new CandidateData("candidateData.txt");

    private CandidateData(String sourceFile) {
        String category = null;
        String source = null;
        Builder<Integer> _order = ImmutableList.builder();
        Quarter quarter = null;
        String afterItem = null;
        String proposalItem = null;
        Status status = null;

        for (String line : FileUtilities.in(CandidateData.class, sourceFile)) {
            line = line.trim();
            try {
                if (line.startsWith("#") || line.isEmpty()) { // comment
                    continue;
                } else if (line.startsWith("U+")) { // data
                    fixGenderSkin(source); // old source

                    source = Utility.fromHex(line);
                    if (allCharacters.contains(source)) {
                        throw new IllegalArgumentException(Utility.hex(source) + " occurs twice");
                    }
                    statuses.put(source, status);
                    allCharacters.add(source);
                    if (source.codePointCount(0, source.length()) == 1) {
                        singleCharacters.add(source);
                    }
                    quarters.put(source, quarter);
                    after.put(source, afterItem);
                    proposal.put(source, ImmutableSet.copyOf(SPLITTER_COMMA.split(proposalItem.replace('-', '\u2011'))));
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

        allCharacters.addAll(singleCharacters); // just to be sure

        comments.freeze();
        statuses.freeze();
        order = _order.build();
        categories.freeze();
        suborder.freeze();
        names.freeze();
        unames.freeze();
        annotations.freeze();
        attributes.freeze();
        quarters.freeze();
        singleCharacters.freeze();
        allCharacters.freeze();
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
        if (!checkData(this)) {
            throw new IllegalArgumentException("Bad Data");
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
                    || Emoji.MAN_OR_WOMAN.containsSome(item)) {
                continue;
            }

            String name = instance.getName(item);
            Set<String> keywords = instance.getAnnotations(item);
            if (keywords.size() > 5) {
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
                System.out.println("UCD: " + uname + "\t≠\t" + cname);
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

    private void fixGenderSkin(String source) {
        if (source == null) {
            return;
        }
        int single = UnicodeSet.getSingleCodePoint(source);
        if (single == Integer.MAX_VALUE) {
            return;
        }
        boolean isModBase = emoji_Modifier_Base.contains(source);
        if (isModBase) {
            for (String mod : EmojiData.MODIFIERS) {
                addCombo(source, source + mod, "", ": " + EmojiData.EMOJI_DATA.getName(mod));
            }
        }
        boolean isGenderBase = emoji_Gender_Base.contains(source);
        if (isGenderBase) {
            for (String gen : Emoji.GENDER_MARKERS) {
                String genSuffix = Emoji.JOINER_STR + gen + Emoji.EMOJI_VARIANT_STRING;
                String genPrefix = gen.equals(Emoji.MALE) ? "man " : "woman ";
                addCombo(source, source + genSuffix, genPrefix, "");
                if (isModBase) {
                    for (String mod : EmojiData.MODIFIERS) {
                        addCombo(source, source + mod + genSuffix, genPrefix, ": " + EmojiData.EMOJI_DATA.getName(mod));
                    }
                }
            }
        }
        if (isGenderBase && isModBase) {
            addComment(source, "Combinations of gender and skin-tone produce 17 more emoji sequences.");
        } else if (isGenderBase) {
            addComment(source, "Combinations of gender and skin-tone produce 2 more emoji sequences.");
        } else if (isModBase) {
            addComment(source, "Combinations of gender and skin-tone produce 5 more emoji sequences.");
        }
        // Comment=There will be 55 emoji sequences with combinations of gender and skin-tone

    }

    private void addAttribute(String source, UnicodeSet unicodeSet, String title) {
        if (source.codePointCount(0, source.length()) == 1) {
            unicodeSet.add(source);
            attributes.add(source, title);
        }
    }

    private void addCombo(String cp, String combo, String namePrefix, String nameSuffix) {
        String newName = namePrefix + names.get(cp) + nameSuffix;
        //System.out.println("Adding: " + newName);
        allCharacters.add(combo);
        names.put(combo, newName);
        statuses.put(combo, statuses.get(cp));
        after.put(combo, after.get(cp));
        proposal.put(combo, proposal.get(cp));

        setCategoryAndSuborder(combo, categories.get(cp));

        //        if (Emoji.HAIR_PIECES.containsSome(cp)) { // HACK
        //            names.put(combo, 
        //                    EmojiData.EMOJI_DATA.getName(UTF16.valueOf(Character.codePointAt(combo, 0)))
        //                    + ": " +
        //                    getName(Character.codePointBefore(combo, combo.length())));
        //        }
    }

    Comparator<String> comparator = new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            if (o1 == o2) {
                return 0;
            }
            Status s1 = statuses.get(o1);
            Status s2 = statuses.get(o2);
            if (s1 != s2) {
                try {
                    return s1.compareTo(s2);
                } catch (Exception e) {
                    throw new ICUException(e);
                }
            }

            String cat1 = getCategory(o1);
            int catOrder1 = EmojiOrder.STD_ORDER.getGroupOrder(cat1); 
            
            String cat2 = getCategory(o2);
            int catOrder2 = EmojiOrder.STD_ORDER.getGroupOrder(cat2);
            if (catOrder1 != catOrder2) {
                return catOrder1 > catOrder2 ? 1 : -1;
            }

            int order;

            String after1 = after.get(o1);
            String after2 = after.get(o2);
            order = EmojiOrder.STD_ORDER.codepointCompare.compare(after1, after2);
            if (order != 0) {
                return order;
            }

            Integer so1 = suborder.get(o1);
            Integer so2 = suborder.get(o2);
            order = so1-so2;
            if (order != 0) {
                return order;
            }

            return EmojiOrder.FULL_COMPARATOR.compare(o1, o2);
        }
    };

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
        return proposal.get(source);
    }

    public String getProposalHtml(String source) {
        // later add http://www.unicode.org/cgi-bin/GetMatchingDocs.pl?L2/17-023
        StringBuilder result = new StringBuilder();
        Set<String> proposals = getProposal(source);
        if (proposals == null || proposals.isEmpty()) {
            throw new IllegalArgumentException("no proposals available for " + Utility.hex(source));
        }
        for (String proposalItem :  proposals) {
            if (result.length() != 0) {
                result.append(", ");
            }
            result.append("<a target='e-prop' href='http://www.unicode.org/cgi-bin/GetMatchingDocs.pl?" + proposalItem.replace('\u2011', '-') + "'>"
                    + proposalItem + "</a>");
        }
        return result.toString();
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
        return result != null ? result :EmojiOrder.STD_ORDER.getMajorGroupFromCategory(getCategory(s));
    }

    public MajorGroup getMajorGroup(int s) {
        MajorGroup result = EmojiOrder.STD_ORDER.majorGroupings.get(s);
        return result != null ? result :EmojiOrder.STD_ORDER.getMajorGroupFromCategory(getCategory(s));
    }

    public static void main(String[] args) {
        CandidateData instance = CandidateData.getInstance();
        if (false) return;

        //        for (Status status : Status.values()) {
        //            UnicodeSet items = instance.statuses.getSet(status);
        //            System.out.println(status + "\t" + items.size());
        //        }
        //        generateProposalData(instance);
        //        showOrdering(instance);

        showCandidateData(CandidateData.getInstance(), true);
        //showCandidateData(CandidateData.getProposalInstance(), true);
    }

    private static void generateProposalData(CandidateData instance) {
        System.out.println("\nData for proposalData.txt\n");
        //1F931;  L2/16-280,L2/16-282r;   BREAST-FEEDING   
        for (String item : instance.allCharacters) {
            if (instance.statuses.get(item) == Status.Provisional_Candidate
                    || EmojiData.MODIFIERS.containsSome(item)
                    || Emoji.GENDER_MARKERS.containsSome(item)
                    ) {
                continue;
            }
            System.out.println(Utility.hex(item)
                    + "; " + CollectionUtilities.join(instance.proposal.get(item), ", ")
                    + "; " + instance.getName(item));
        }
    }

    private static void showOrdering(CandidateData instance) {
        System.out.println("\nOrdering Data\n");

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
            System.out.println(instance.after.get(subItem) 
                    + "\t" + subItem 
                    + "\t" + instance.suborder.get(subItem) 
                    + "\t" + instance.getName(subItem)
                    + "\tkw:" + instance.getAnnotations(subItem)
                    + "\tucd:" + instance.getUName(subItem)
                    );
            if (instance.getStatus(subItem) != Status.Provisional_Candidate) {
                cm.add(subItem, instance);
            }
        }
        EmojiOrder ordering = EmojiOrder.of(Emoji.VERSION_BETA);
        //        for (String s : instance.allCharacters) {
        //            System.out.println(s + "\t" + ordering.getCategory(s));
        //        }
        //        for (String s : new UnicodeSet("[{👨‍🦰️}{👨‍🦱️}{👨‍🦲️}{👨‍🦳️}{👨🏻‍🦰️}{👨🏻‍🦱️}{👨🏻‍🦲️}{👨🏻‍🦳️}{👨🏼‍🦰️}{👨🏼‍🦱️}{👨🏼‍🦲️}{👨🏼‍🦳️}{👨🏽‍🦰️}{👨🏽‍🦱️}{👨🏽‍🦲️}{👨🏽‍🦳️}{👨🏾‍🦰️}{👨🏾‍🦱️}{👨🏾‍🦲️}{👨🏾‍🦳️}{👨🏿‍🦰️}{👨🏿‍🦱️}{👨🏿‍🦲️}{👨🏿‍🦳️}{👩‍🦰️}{👩‍🦱️}{👩‍🦲️}{👩‍🦳️}{👩🏻‍🦰️}{👩🏻‍🦱️}{👩🏻‍🦲️}{👩🏻‍🦳️}{👩🏼‍🦰️}{👩🏼‍🦱️}{👩🏼‍🦲️}{👩🏼‍🦳️}{👩🏽‍🦰️}{👩🏽‍🦱️}{👩🏽‍🦲️}{👩🏽‍🦳️}{👩🏾‍🦰️}{👩🏾‍🦱️}{👩🏾‍🦲️}{👩🏾‍🦳️}{👩🏿‍🦰️}{👩🏿‍🦱️}{👩🏿‍🦲️}{👩🏿‍🦳️}{🦸️‍♀️}{🦸️‍♂️}{🦹️‍♀️}{🦹️‍♂️}]")) {
        //            System.out.println(s + "\t" + ordering.getCategory(s));
        //        }
        System.out.println("\n\nSO\tType\tCategory\tHex\tCldr Name\tUcd Name");
        int sortOrder = 0;
        for (Category evalue : Category.values()) {
            Bucket bucket = cm.buckets.get(evalue);
            if (bucket == null) continue;
            for (MajorGroup maj : MajorGroup.values()) {
                UnicodeSet uset = bucket.sets.getSet(maj);
                if (uset.isEmpty()) continue;
                Set<String> items = uset.addAllTo(new TreeSet<>(instance.comparator));
                // System.out.println(evalue.toStringPlain() + "\t" + maj.toPlainString() + "\t" + items.size());
                for (String subItem : items) {
                    String uName = instance.getUName(subItem);
                    System.out.println(
                            ++sortOrder
                            + "\t" + evalue.toStringPlain() 
                            + "\t" + maj.toPlainString() 
                            + "\tU+" + Utility.hex(subItem, ", U+")
                            + "\t" + instance.getName(subItem) 
                            + (uName != null ? "\t" + uName : "")
                            );
                }
            }
        }
    }

    private static void showCandidateData(CandidateData cd, boolean candidate) {
        cd.comparator.compare(Utility.fromHex("1F9B5"), Utility.fromHex("1F9B6 1F3FF"));



        System.out.println("Code Point\tChart\tGlyph\tSample\tColored Glyph\tName");
        final UnicodeSet chars2 = cd.getAllCharacters();
        List<String> sorted = new ArrayList<>(chars2.addAllTo(new TreeSet<String>(
                candidate ? cd.comparator : EmojiOrder.STD_ORDER.codepointCompare)));
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
            if (cd.getProposal(s) == null) {
                System.out.println("ERROR IN PROPOSAL!!");
            }
            System.out.println(Utility.hex(s) 
                    + "\t" + s 
                    //                    + "\t" + cd.getQuarter(s) 
                    + "\t" + cd.getName(s)
                    + "\t" + cd.getProposal(s) 
                    );
            for (String annotation :  cd.getAnnotations(s)) {
                System.out.println("• " + annotation);
            }
        }
        System.out.println("# list: " + lastCategory + " = \t" + CollectionUtilities.join(lastCategoryList, " "));
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
            if (source.contains(EmojiData.ZWJ_HANDSHAKE_ZWJ)) {
                temp = EmojiData.EMOJI_DATA.getFallbackName(source);
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

    private UnicodeSet addWithCharFilter(UnicodeSet source, UnicodeSet includeFilter) {
        return addWithCharFilter(source, includeFilter, null);
    }

    private UnicodeSet addWithCharFilter(UnicodeSet source, UnicodeSet includeFilter, UnicodeSet excludeFilter) {
        UnicodeSet result = new UnicodeSet();
        for (String s : source) {
            if ((includeFilter == null || includeFilter.containsSome(s)) 
                    && (excludeFilter == null || !excludeFilter.containsSome(s))) {
                result.add(s);
            }
        }
        return result.freeze();
    }


    @Override
    public UnicodeSet getEmojiComponents() {
        return addWithCharFilter(emoji_Component, null, provisional);
    }

    @Override
    public UnicodeSet getSingletonsWithDefectives() {
        return addWithCharFilter(singleCharacters, null, provisional);
    }

    @Override
    public UnicodeSet getEmojiPresentationSet() {
        return addWithCharFilter(addWithCharFilter(singleCharacters, null, provisional), null, getTextPresentationSet());
    }

    @Override
    public UnicodeSet getModifierBases() {
        return addWithCharFilter(emoji_Modifier_Base, null, provisional);
    }

    @Override
    public UnicodeSet getExtendedPictographic() {
        return addWithCharFilter(singleCharacters, null, provisional);
    }

    @Override
    public UnicodeSet getTagSequences() {
        return addWithCharFilter(allNonProvisional, Emoji.TAGS);
    }

    @Override
    public UnicodeSet getModifierSequences() {
        return addWithCharFilter(allNonProvisional, EmojiData.MODIFIERS, ZWJ_SET);
    }

    @Override
    public UnicodeSet getFlagSequences() {
        return addWithCharFilter(allNonProvisional, Emoji.REGIONAL_INDICATORS);
    }

    @Override
    public UnicodeSet getZwjSequencesNormal() {
        return addWithCharFilter(allNonProvisional, ZWJ_SET);
    }

    @Override
    public UnicodeSet getEmojiWithVariants() {
        return addWithCharFilter(allNonProvisional, Emoji.REGIONAL_INDICATORS);
    }

    @Override
    public UnicodeSet getAllEmojiWithoutDefectives() {
        return addWithCharFilter(allNonProvisional, getEmojiComponents());
    }

    @Override
    public UnicodeSet getTextPresentationSet() {
        return addWithCharFilter(textPresentation, null, provisional);
    }

    @Override
    public UnicodeSet getAllEmojiWithDefectives() {
        return allNonProvisional;
    }

    @Override
    public UnicodeSet getGenderBases() {
        return addWithCharFilter(emoji_Gender_Base, null, provisional);
    }

    @Override
    public UnicodeSet getSingletonsWithoutDefectives() {
        return addWithCharFilter(singleCharacters, allNonProvisional, getEmojiComponents());
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
        Age_Values result = Emoji.getNewest(s);
        return result == Age_Values.Unassigned ? Emoji.UCD11 
                : VersionInfo.getInstance(result.getShortName());
    }

    @Override
    public UnicodeSet getTakesSign() {
        return addWithCharFilter(takesSign, null, provisional);
    }

    public UnicodeSet getAllCharacters(Status status) {
        switch(status) {
        case Provisional_Candidate: return provisional;
        case Draft_Candidate : return draft;
        default: throw new IllegalArgumentException();
        }
    }
}
