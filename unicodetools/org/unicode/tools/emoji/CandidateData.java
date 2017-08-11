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
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.props.UnicodeRelation.SetMaker;
import org.unicode.props.UnicodeRelation;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.EmojiOrder.MajorGroup;
import org.unicode.tools.emoji.GenerateEmojiData.ZwjType;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

public class CandidateData implements Transform<String, String> {
    private static final Splitter SPLITTER_COMMA = Splitter.on(',').trimResults().omitEmptyStrings();
    private static final Joiner JOIN_COMMA = Joiner.on(", ");
    static final Splitter barSplit = Splitter.on('|').trimResults();
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
        Final_Candidates("Code points are final."), // final code points
        Draft_Candidates("Code points are draft."), // draft code points
        Provisional_Candidates("Temporary IDs are assigned, not code points.");  // no code points
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
    private final UnicodeMap<String> names = new UnicodeMap<>();
    private final UnicodeMap<String> unames = new UnicodeMap<>();
    private final UnicodeRelation<String> annotations = new UnicodeRelation<>(SORTED_TREESET_MAKER);
    private final UnicodeRelation<String> attributes = new UnicodeRelation<>();
    private final UnicodeMap<Quarter> quarters = new UnicodeMap<>();
    private final UnicodeMap<Status> statuses = new UnicodeMap<>();
    private final UnicodeSet characters = new UnicodeSet();
    private final UnicodeSet emoji_Modifier_Base = new UnicodeSet();
    private final UnicodeSet emoji_Gender_Base = new UnicodeSet();
    private final UnicodeSet emoji_Component = new UnicodeSet();
    private final UnicodeMap<String> after = new UnicodeMap<>();
    private final UnicodeMap<String> comments = new UnicodeMap<>();
    
    private final UnicodeMap<Set<String>> proposal = new UnicodeMap<>();

    static final CandidateData SINGLE = new CandidateData("candidateData.txt");
    //static final CandidateData PROPOSALS = new CandidateData("proposalData.txt");

    private CandidateData(String sourceFile) {
        String category = null;
        int source = -1;
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
                    source = Utility.fromHex(line).codePointAt(0);
                    if (characters.contains(source)) {
                        throw new IllegalArgumentException(Utility.hex(source) + " occurs twice");
                    }
                    statuses.put(source, status);
                    characters.add(source);
                    quarters.put(source, quarter);
                    after.put(source, afterItem);
                    proposal.put(source, ImmutableSet.copyOf(SPLITTER_COMMA.split(proposalItem.replace('-', '\u2011'))));
                    String afterString = "> " + afterItem;
                    Age_Values age = Emoji.VERSION_ENUM.get(afterItem.codePointAt(0));
                    if (age.compareTo(Age_Values.V10_0) >= 0) {
                        afterString += " (" + Utility.hex(afterItem) + ")";
                    }
                    attributes.add(source, afterString);
                    categories.put(source, category);
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
                        names.put(source, name);
                        break;
                    case "UName": 
                        final String uname = rightSide.toUpperCase(Locale.ROOT);
                        unames.put(source, uname);
                        break;
                    case "Keywords": 
                        if (rightSide.contains("dengue")) {
                            int debug = 0;
                        }
                        List<String> cleanKeywords = barSplit.splitToList(rightSide);
                        annotations.addAll(source, cleanKeywords);
                        break;
                    case "Emoji_Modifier_Base": 
                        emoji_Modifier_Base.add(source);
                        attributes.add(source, "∈ modifier_base");
                        break;
                    case "Emoji_Gender_Base": 
                        emoji_Gender_Base.add(source);
                        attributes.add(source, "∈ gender_base");
                        break;
                    case "Emoji_Component": 
                        emoji_Component.add(source);
                        attributes.add(source, "∈ component");
                        break;
                    case "Comment":
                        if (comments.containsKey(source)) {
                            throw new IllegalArgumentException("duplication comment: " + rightSide);
                        }
                        comments.put(source, rightSide);
                        break;

                    default: 
                        throw new IllegalArgumentException(line);
                    }
                }
            } catch (Exception e) {
                throw new IllegalArgumentException(line, e);
            }
        }
        comments.freeze();
        statuses.freeze();
        order = _order.build();
        categories.freeze();
        names.freeze();
        unames.freeze();
        annotations.freeze();
        attributes.freeze();
        quarters.freeze();
        characters.freeze();
        emoji_Modifier_Base.freeze();
        emoji_Gender_Base.freeze();
        emoji_Component.freeze();
        proposal.freeze();
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
                return s1.compareTo(s2);
            }

            int catOrder1 = EmojiOrder.STD_ORDER.getGroupOrder(getCategory(o1));
            int catOrder2 = EmojiOrder.STD_ORDER.getGroupOrder(getCategory(o2));
            if (catOrder1 != catOrder2) {
                return catOrder1 > catOrder2 ? 1 : -1;
            }
            
            String after1 = after.get(o1);
            String after2 = after.get(o2);
            int order = EmojiOrder.STD_ORDER.codepointCompare.compare(after1, after2);
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
        return characters;
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
        for (String proposalItem :  proposal.get(source.codePointAt(0))) {
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
        showCandidateData(CandidateData.getInstance(), true);
        //showCandidateData(CandidateData.getProposalInstance(), true);
    }

    private static void showCandidateData(CandidateData cd, boolean candidate) {
        System.out.println("Code Point\tChart\tGlyph\tSample\tColored Glyph\tName");
        final UnicodeSet chars2 = cd.getCharacters();
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
                    + "\t" + cd.getQuarter(s) 
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
