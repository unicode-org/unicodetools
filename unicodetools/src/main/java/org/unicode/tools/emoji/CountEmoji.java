package org.unicode.tools.emoji;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.tool.Option;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.tool.Option.Params;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.TransliteratorUtilities;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.EmojiOrder.MajorGroup;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.UnicodeSet;

public class CountEmoji {
    public static final String EMOJI_COUNT_KEY = "<a target='info' href='../format.html#col-totals'>Emoji Counts Key</a>";
    public static final String STRUCTURE = "<a target='info' href='../format.html#col-totals'>Structure</a>";
    private static final EmojiData EMOJI_DATA_PREVIOUS = EmojiData.of(Emoji.VERSION_LAST_RELEASED);
    private static final EmojiData EMOJI_DATA_BETA = EmojiData.EMOJI_DATA_BETA;
    private static final EmojiOrder ORDER = EmojiOrder.BETA_ORDER;
    private static final boolean SHOW_SAMPLE = true;

    enum MyOptions {
        nonincrementalCount(new Params()),
        countVs(new Params()),
        invalid(new Params()),
        verbose(new Params().setHelp("verbose debugging messages")), 
        list(new Params()),
        major(new Params()),
        ;

        // BOILERPLATE TO COPY
        final Option option;
        private MyOptions(Params params) {
            option = new Option(this, params);
        }
        private static Options myOptions = new Options();
        static {
            for (MyOptions option : MyOptions.values()) {
                myOptions.add(option, option.option);
            }
        }
        private static Set<String> parse(String[] args, boolean showArguments) {
            return myOptions.parse(MyOptions.values()[0], args, true);
        }
    }

    public static void main(String[] args) {
        CountEmoji.MyOptions.parse(args, true);
        boolean done = false;
        if (CountEmoji.MyOptions.countVs.option.doesOccur()) {
            countVs();
            done=true;
        }
        //        if (MyOptions.invalid.option.doesOccur()) {
        //            countInvalid();
        //            done=true;
        //        }
        if (CountEmoji.MyOptions.nonincrementalCount.option.doesOccur()) {
            countNonincremental();
            done=true;
        }
        if (CountEmoji.MyOptions.list.option.doesOccur()) {
            //Category bucket = Category.getBucket("👨‍⚖️");
            UnicodeSet toDisplay = EmojiData.of(Emoji.VERSION_BETA).getAllEmojiWithoutDefectives();
            System.out.println("\nEmoji v11");
            listCategories(toDisplay);

            EmojiData EMOJI_DATA_PREVIOUS = EmojiData.of(Emoji.VERSION_TO_GENERATE_PREVIOUS);
            UnicodeSet onlyNew = new UnicodeSet(EmojiData.EMOJI_DATA.getAllEmojiWithoutDefectives())
                    .removeAll(EMOJI_DATA_PREVIOUS.getAllEmojiWithoutDefectives());
            System.out.println("\nEmoji v11-v5");
            listCategories(onlyNew);
            done = true;
        }
        if (CountEmoji.MyOptions.major.option.doesOccur()) {
            UnicodeSet toDisplay = EmojiData.of(Emoji.VERSION_BETA).getAllEmojiWithoutDefectives();
            doMajor(toDisplay);
            done = true;

        }
        if (!done) {
            countNew();
        }
    }

    private static void doMajor(UnicodeSet toDisplay) {
        Counter<MajorGroup> counts = new Counter<>();
        Counter<String> subgroups = new Counter<>();
        Set<String> order = new LinkedHashSet<>();
        UnicodeSet longPress = new UnicodeSet();
        TreeSet<String> sorted = toDisplay.addAllTo(new TreeSet<String>(EmojiOrder.STD_ORDER.codepointCompare));
        for (String emoji : sorted) {
            Category cat = Category.getBucket(emoji);
            Set<Attribute> attr = cat.getAttributes();
            if (attr.contains(Attribute.hair) || attr.contains(Attribute.skin)) {
                longPress.add(emoji);
                continue;
            }            
            String group = EmojiOrder.STD_ORDER.getCategory(emoji);
            order.add(group);
            subgroups.add(group, 1);
            MajorGroup majorGroup = EmojiOrder.STD_ORDER.getMajorGroupFromCategory(group);
            counts.add(majorGroup, 1);
        }
        for (MajorGroup mgroup : MajorGroup.values()) {
            System.out.println(mgroup.toPlainString() + "\t" + counts.get(mgroup));
        }
        for (String subgroup : order) {
            MajorGroup majorGroup = EmojiOrder.STD_ORDER.getMajorGroupFromCategory(subgroup);
            System.out.println(majorGroup.toPlainString() + "\t" + subgroup + "\t" + subgroups.get(subgroup));
        }
    }

    private static void listCategories(UnicodeSet toDisplay) {
        Multimap<Category,String> items = TreeMultimap.create(Ordering.natural(), EmojiOrder.STD_ORDER.codepointCompare);
        for (Category x : Category.values()) {
            System.out.println(x.displayName + ":\t" + x.html);
        }
        for (String item : toDisplay) {
            Category cat = Category.getBucket(item);
            items.put(cat, item);
        }
        for (Category cat : Category.values()) {
            Collection<String> set = items.get(cat);
            System.out.println(cat.toStringPlain()
                    + "\t" + CollectionUtilities.join(cat.getAttributes(), " ")
                    + "\t" + set.size()
                    + "\t" + CollectionUtilities.join(set, " "));
        }
    }

    private static void countNonincremental() {
        UnicodeSet all = EMOJI_DATA_BETA.getAllEmojiWithDefectives();

        UnicodeSet missing = new UnicodeSet();
        for (String zwj : EMOJI_DATA_BETA.getZwjSequencesNormal()) {
            int pos = zwj.length();
            while (true) {
                int lastZwjIndex = zwj.lastIndexOf(Emoji.JOINER, pos);
                if (lastZwjIndex < 0) break;
                String prev = zwj.substring(0, lastZwjIndex);
                if (!all.contains(prev) 
                        && !all.contains(prev.replace(Emoji.EMOJI_VARIANT_STRING, "")) 
                        && !missing.contains(prev)) {
                    System.out.println(prev  + "\t" + Utility.hex(prev));
                    missing.add(prev);
                }
                pos = lastZwjIndex-1;
            }
        }
        System.out.println("ZSeq Count: " + EMOJI_DATA_BETA.getZwjSequencesNormal().size());
        System.out.println("NonIncremental count: " + missing.size());
    }

    private static void countNew() {
        UnicodeSet current = new UnicodeSet(EMOJI_DATA_BETA.getAllEmojiWithoutDefectives())
                .addAll(EMOJI_DATA_BETA.getEmojiComponents())
                .freeze();
        UnicodeSet previous = new UnicodeSet(EMOJI_DATA_PREVIOUS.getAllEmojiWithoutDefectives())
                .addAll(EMOJI_DATA_BETA.getEmojiComponents())
                .freeze();
        UnicodeSet ARE_NEW = new UnicodeSet(current)
                .removeAll(previous)
                .freeze();

        String vPrevious = "v" + Emoji.VERSION_LAST_RELEASED.getVersionString(2, 2);
        String vCurrent = "v" + Emoji.VERSION_BETA.getVersionString(2, 2);

        new CountEmoji().countItems(vCurrent + "-" + vPrevious, ARE_NEW);
        new CountEmoji().countItems(vPrevious, previous);
        new CountEmoji().countItems(vCurrent, current);
    }

    final Map<Category, Bucket> buckets = new EnumMap<>(Category.class);

    public void clear() {
        buckets.clear();
    }

    @Override
    public String toString() {
        return buckets.toString();
    }

    static class Bucket {
        final Counter<MajorGroup> majors = new Counter<>();
        final UnicodeMap<MajorGroup> sets= new UnicodeMap<>();
        public void add(MajorGroup maj, String cat, String s) {
            majors.add(maj, 1);
            sets.put(s,maj);
        }
        @Override
        public String toString() {
            return "[majors:" + majors + "; sets:" + sets + "]";
        }
    }

    void add(String s) {
        add(s, null);
    }
    void add(String s, CandidateData candidateData) {
        String cat = ORDER.getCategory(s);
        if (cat == null && candidateData != null) {
            cat = candidateData.getCategory(s);
        }
        MajorGroup maj = cat == null ? MajorGroup.Other : ORDER.getMajorGroupFromCategory(cat);
        Category category = Category.getBucket(s);
        Bucket bucket = buckets.get(category);
        if (bucket == null) {
            buckets.put(category, bucket = new Bucket());
        }
        bucket.add(maj, cat, s);
        return;
    }

    public void showCounts(PrintWriter out, boolean showCharacters, PrintWriter outPlain) {
        out.println("<table>\n");
        String row = "<tr>";
        String td = "<td class='rchars'>";
        String th = "<th class='rchars'>";
        String thNowrap = "<th style='white-space: nowrap;'>";
        String th9 = "<th class='rchars' style='width:10%'>";
        MajorGroup[] groups = MajorGroup.values();
        out.print(row + thNowrap + STRUCTURE + "</th>");
        for (MajorGroup maj : groups) {
            if (maj == MajorGroup.Other) {
                continue;
            }
            out.print(th9 + maj.toHTMLString() + "</th>");
        }
        out.println(th9 + "Total" + "</th>" + "</tr>");
        boolean didSub = false;

        Counter<MajorGroup> columnCount = new Counter<MajorGroup>();
        boolean doneSubtotal = false;
        for (Category evalue : Category.values()) {
            Bucket bucket = buckets.get(evalue);
            if (bucket == null) {
                continue;
            }
            if ((evalue == Category.component || evalue == Category.ungendered ) // || evalue == Category.typical_dup_group | evalue == Category.typical_dup_sign) 
                    && !doneSubtotal) {
                showTotalLine(out, "Subtotal", row, th, groups, columnCount);
                doneSubtotal = true;
            }
            out.print(row + thNowrap + evalue + "</th>");
            long rowTotal1 = 0;
            for (MajorGroup maj : groups) {
                if (maj == MajorGroup.Other) {
                    continue;
                }
                long count = bucket.majors.get(maj);
                rowTotal1 += count;
                columnCount.add(maj, count);
                UnicodeSet set = bucket.sets.getSet(maj);
                String tdTitle = SHOW_SAMPLE && count != 0 ? "<td class='rchars' title='" 
                        + getBestSample(set) + "'>" : td;
                out.print(tdTitle + (count == 0 ? "" : count) + "</td>");
                if (count != 0 && outPlain != null) {
                    outPlain.println(evalue.toStringPlain() 
                            + "\t" + maj.toPlainString()
                            + "\t" + count 
                            + "\t" + EmojiData.getWithoutMods(set).toPattern(false));
                }
            }
            out.println(th + rowTotal1 + "</th>" + "</tr>");
        }
        showTotalLine(out, "Total", row, th, groups, columnCount);

        if (showCharacters) {
            out.println();
            for (Category evalue : Category.values()) {
                Bucket bucket = buckets.get(evalue);
                if (bucket == null) {
                    continue;
                }
                out.print(evalue);
                for (MajorGroup maj : groups) {
                    out.print("\t" + bucket.sets.getSet(maj).toPattern(false));
                }
                out.println();
            }
        }
        out.println("</table>\n");
    }

    private String getBestSample(UnicodeSet set) {
        String best = null;
        boolean isEmojiPresentation = false;
        int year = Integer.MAX_VALUE;
        if (set != null && !set.isEmpty()) {
            for (String s : set) {
                if (best == null) {
                    best = s;
                    isEmojiPresentation = EMOJI_DATA_BETA.getEmojiPresentationSet().contains(s.codePointAt(0));
                    year = BirthInfo.getYear(s);
                    continue;
                }
                boolean sEmojiPresentation = EMOJI_DATA_BETA.getEmojiPresentationSet().contains(s.codePointAt(0));
                int sYear = BirthInfo.getYear(s);

                if (!isEmojiPresentation 
                        || isEmojiPresentation == sEmojiPresentation && year > sYear) {
                    best = s;
                    isEmojiPresentation = sEmojiPresentation;
                    year = sYear;
                }
            }

            // Normalize any skin tones to EMOJI MODIFIER FITZPATRICK TYPE-6.
            best = EmojiData.MODS_SPANNER.replaceFrom(best, "🏿");
        }
        return EMOJI_DATA_BETA.addEmojiVariants(best);
    }

    private void showTotalLine(PrintWriter out, String title2, String row, String th, MajorGroup[] groups,
            Counter<MajorGroup> columnCount) {
        long rowTotal = 0;
        out.print(row + th + title2 + "</th>");
        for (MajorGroup maj : groups) {
            if (maj == MajorGroup.Other) {
                continue;
            }
            long count = columnCount.get(maj);
            rowTotal += count;
            out.print(th + count + "</th>");
        }
        out.println(th + rowTotal + "</th>" + "</tr>");
    }

    enum Attribute {
        zwj("Ⓩ"), 
        gender(Emoji.FEMALE),
        role(Emoji.WOMAN_STR),
        family(Emoji.NEUTRAL_FAMILY), 
        hair("🦰"), 
        singleton("Ⓒ"), 
        dup("🧑"),
        skin("🏿"),
        ;

        private final String label;
        private Attribute(String label) {
            this.label = label;
        }
    }

    static final char NNBSP = '\u202F';

    public enum Category {
        character(Attribute.singleton), 
        mod_seq(Attribute.skin), 
        zwj_seq_hair(Attribute.zwj, Attribute.hair),
        zwj_seq_mod_hair(Attribute.zwj, Attribute.skin, Attribute.hair),
        zwj_seq_gender(Attribute.zwj, Attribute.gender), 
        zwj_seq_gender_mod(Attribute.zwj, Attribute.gender, Attribute.skin),
        zwj_seq_role(Attribute.zwj, Attribute.role),
        zwj_seq_role_mod(Attribute.zwj, Attribute.role, Attribute.skin), 
        zwj_seq_fam(Attribute.zwj, Attribute.family), 
        //zwj_seq_fam_mod("" + zwjLabel + " "+Emoji.NEUTRAL_FAMILY + "&skin"), 
        //zwj_seq_mod("" + zwjLabel + " other&skin", Attribute.zwj, Attribute.skin),
        zwj_seq_fam_mod(Attribute.zwj, Attribute.family, Attribute.skin), 
        zwj_seq_mod(Attribute.zwj, Attribute.skin),
        zwj_seq_other(Attribute.zwj),
        keycap_seq("#️⃣"),
        flag_seq("🏁"),
        tag_seq("🏴"), 
        ungendered(Attribute.dup), 
        ungendered_skin(Attribute.skin, Attribute.dup), 
        component("🔗"), 
        //        typical_dup_sign,
        //        typical_dup_group, 
        ;

        final public String displayName;
        final public String html;
        final Set<Attribute> attributes;
        Category() {
            this((String)null);
        }
        Category(Attribute... _baseCategories) {
            this(null, _baseCategories);
        }
        Category(String _name, Attribute... _baseCategories) {
            attributes = _baseCategories.length == 0 ? Collections.emptySortedSet() 
                    : ImmutableSortedSet.copyOf(EnumSet.copyOf(Arrays.asList(_baseCategories)));
            String title = null;
            if (!attributes.isEmpty()) {
                if (_name != null) {
                    throw new IllegalArgumentException();
                }
                StringBuilder sb = new StringBuilder();
                StringBuilder sbLong = new StringBuilder();
                if (attributes.size() == 1 & attributes.contains(Attribute.skin)) {
                    sb.append(Attribute.singleton.label);
                    sbLong.append(Attribute.singleton.toString());
                }
                //                if (!attributes.contains(Attribute.dup) && !attributes.contains(Attribute.zwj)) {
                //                    sb.append(Attribute.singleton.label);
                //                    sbLong.append(Attribute.singleton.toString());
                //                }
                for (Attribute a : attributes) {
                    if (sb.length() != 0) {
                        sb.append(NNBSP+"‧"+NNBSP);
                        sbLong.append(" + ");
                    }
                    sb.append(a.label);
                    sbLong.append(a.toString());
                }
                if (attributes.contains(Attribute.zwj) && attributes.size() == 1) {
                    sb.append(NNBSP+"‧"+NNBSP).append(Attribute.singleton.label);
                    sbLong.append(" + ").append(Attribute.singleton.toString());
                }
                displayName = sb.toString();
                title = TransliteratorUtilities.toHTML.transform(sbLong.toString());
            } else {
                if (_name == null) {
                    _name = name();
                }
                displayName = _name.replace('_', '-');
                title = name().replace('_', '-');
            }
            String _html = TransliteratorUtilities.toHTML.transform(displayName);
            html = title == null ? _html : "<span title='" + title + "'>" + _html + "</span>";
        }
        static Map<Set<Attribute>, Category> attributesToCategory;
        static {
            Map<Set<Attribute>, Category> _attributesToCategory = new HashMap<>();
            Map<String, Category> names = new HashMap<>(); // check uniqueness
            for (Category cat : Category.values()) {
                _attributesToCategory.put(cat.attributes, cat);
                Category old = names.get(cat.displayName);
                if (old != null) {
                    throw new IllegalArgumentException(
                            "Duplicate display name: " + cat.displayName 
                            + " for " + old + " and " + cat);
                }
                names.put(cat.displayName, cat);
            }
            attributesToCategory = ImmutableMap.copyOf(_attributesToCategory);
        }
        @Override
        public String toString() {
            return html;
        }
        public String toStringPlain() {
            return displayName;
        }
        /** added to make migration easier */
        static public Category getType(String s) {
            return getBucket(s);
        }
        static public Category getBucket(String s) {
            try {
                String noVariants = EmojiData.removeEmojiVariants(s);
                Category bucket = null;
                if (noVariants.startsWith(Emoji.MALE) || noVariants.startsWith(Emoji.FEMALE)) {
                    int debug = 0;
                }
                if (noVariants.isEmpty() 
                        || CountEmoji.EMOJI_DATA_BETA.getEmojiComponents().contains(noVariants)
                        || Emoji.FULL_GENDER_MARKERS.contains(noVariants)) {
                    bucket = component;
                } else if (CharSequences.getSingleCodePoint(noVariants) < Integer.MAX_VALUE) {
                    bucket = character;
                } else if (noVariants.contains(Emoji.KEYCAP_MARK_STRING)) {
                    bucket = keycap_seq;
                } else if (noVariants.contains(Emoji.TAG_TERM)) {
                    bucket = tag_seq;
                } else if (Emoji.REGIONAL_INDICATORS.containsSome(noVariants)) {
                    bucket = flag_seq;
                } else {
                    EnumSet<Attribute> attributes = EnumSet.noneOf(Attribute.class);

                    boolean mods = EmojiData.MODIFIERS.containsSome(noVariants);
                    if (mods) {
                        attributes.add(Attribute.skin);
                    }
                    boolean hair = Emoji.HAIR_PIECES.containsSome(noVariants);
                    if (hair) {
                        attributes.add(Attribute.hair);
                    }
                    boolean gender = Emoji.GENDER_MARKERS.containsSome(noVariants);
                    if (gender) {
                        attributes.add(Attribute.gender);
                    }
                    boolean zwj = noVariants.contains(Emoji.JOINER_STR);
                    if (zwj) {
                        attributes.add(Attribute.zwj);
                    }

                    int first = noVariants.codePointAt(0);
                    String butFirst = noVariants.substring(Character.charCount(first));
                    boolean role = Emoji.MAN_OR_WOMAN_OR_ADULT.contains(first) 
                            && Emoji.PROFESSION_OBJECT.containsSome(noVariants);
                    if (role) {
                        attributes.add(Attribute.role);
                    }
                    boolean family = noVariants.contains(EmojiData.ZWJ_HANDSHAKE_ZWJ)
                            || Emoji.FAMILY_MARKERS.contains(first) && Emoji.FAMILY_MARKERS.containsSome(butFirst);
                    if (family) {
                        attributes.add(Attribute.family);
                    }
                    bucket = getCategory(attributes);
                    //                
                    //
                    //                if (!zwj) {
                    //                    if (mods) {
                    //                        bucket = mod_seq;
                    //                    } else {
                    //                        throw new IllegalArgumentException("should never happen");
                    //                    }
                    //                } else { // zwj
                    //                    if (gender) {
                    //                        bucket = getVariety(mods, hair, zwj_seq_gender, zwj_seq_gender_mod, null, null);
                    //                    } else if (role) {
                    //                        bucket = getVariety(mods, hair, zwj_seq_role, zwj_seq_role_mod, null, null);
                    //                    } else if (family) {
                    //                        bucket = getVariety(mods, hair, zwj_seq_fam, null, null, null);
                    //                    } else {
                    //                        bucket = getVariety(mods, hair, zwj_seq_other, zwj_seq_mod, zwj_seq_hair, zwj_seq_mod_hair);
                    //                    }
                    //                }
                }
                if (!s.isEmpty() && EmojiData.EMOJI_DATA.isTypicallyDuplicateSign(s)) {
                    EnumSet<Attribute> attributes2 = EnumSet.of(Attribute.dup);
                    attributes2.addAll(bucket.attributes);
                    bucket = getCategory(attributes2);
                }
                return bucket;
            } catch (NoCategoryException e) {
                throw new IllegalArgumentException("for «" + s + "» "+ Utility.hex(s), e);
            }
        }

        private static Category getCategory(Set<Attribute> attributes) {
            Category result = attributesToCategory.get(attributes);
            if (result == null) {
                throw new NoCategoryException("no category available for: " + attributes);
            }
            return result;
        }

        public static class NoCategoryException extends RuntimeException {
            public NoCategoryException(String string) {
                super(string);
            }
        }


        public boolean hasAttribute(Attribute baseCategory) {
            return attributes.contains(baseCategory);
        }
        public Set<Attribute> getAttributes() {
            return attributes;
        }
    }

    /**@deprecated Replace by the {@link CountEmoji.Category}*/
    public enum ZwjType {
        roleWithHair, roleWithObject, roleWithSign, gestures, activity, family, other, na;
        public static ZwjType getType(String s) {
            if (!s.contains(Emoji.JOINER_STRING)) {
                return na;
            }
            int[] cps = CharSequences.codePoints(s);
            ZwjType zwjType = ZwjType.other;
            if (Emoji.HAIR_PIECES.containsSome(s)) {
                zwjType = roleWithHair;
            } else if (Emoji.FAMILY_MARKERS.contains(cps[cps.length - 1])) { // last character is in boy..woman
                zwjType = family;
            } else if (Emoji.ACTIVITY_MARKER.containsSome(s)) {
                zwjType = activity;
            } else if (Emoji.ROLE_MARKER.containsSome(s)) { //  || Emoji.FAMILY_MARKERS.containsSome(s)
                zwjType = Emoji.GENDER_MARKERS.containsSome(s) ? roleWithSign : roleWithObject;
            } else if (Emoji.GENDER_MARKERS.containsSome(s)) {
                zwjType = gestures;
            }
            return zwjType;
        }
    }

    private void countItems(String title, UnicodeSet uset) {
        Set<String> sorted = uset.addAllTo(new TreeSet<>(ORDER.codepointCompare));
        for (String s : sorted) {
            add(s);
        }
        System.out.println("\n" + title);
        PrintWriter pw = new PrintWriter(System.out);
        pw.println("<p>For a key to the format of the table, see "
                + EMOJI_COUNT_KEY
                + ".</p>");
        showCounts(pw, false, null);
        pw.close();
    }

    //        private static <T> void showCount(Counter<T> majorsNoSkin) {
    //                for (T maj : majorsNoSkin) {
    //                        long count = majorsNoSkin.get(maj);
    //                        System.out.println("||" + count + "||" + maj + "||");
    //                }
    //                System.out.println("||TOTAL||" + majorsNoSkin.getTotal() + "||");
    //        }

    public static void countVs() {
        // find the items whose filtered form needs a VS
        int countPlain = 0;
        int countFirst = 0;
        int countFull = 0;
        int countOther = 0;
        for (String itemFull : EMOJI_DATA_BETA.getAllEmojiWithoutDefectives()) {
            String itemWithout = itemFull.replace(Emoji.EMOJI_VARIANT_STRING, "");
            if (itemWithout.equals(itemFull)) {
                countPlain++;
                continue;
            }
            //without=first=full
            //without=first≠full
            //without≠first≠full
            //without≠first=full
            String itemFirst = EMOJI_DATA_BETA.getOnlyFirstVariant(itemFull);
            if (!itemFirst.equals(itemFull)) {
                if (!itemFirst.equals(itemWithout)) {
                    countOther++;
                    showLine(countOther, "without≠first≠full", itemWithout, itemFirst, itemFull);
                }
                countFirst++;
                showLine(countFirst, "without=first≠full", itemWithout, itemFirst, itemFull);
                continue;
            }
            ++countFull;
            showLine(countFull, "without≠first=full", itemWithout, itemFirst, itemFull);
        }
        System.out.println("countPlain: " + countPlain);
        System.out.println("without≠first=full: " + countFull);
        System.out.println("without=first≠full: " + countFirst);
        System.out.println("without≠first≠full: " + countOther);

    }
    private static void showLine(int countFirst, String title, String itemWithout, String itemFirst, String itemFull) {
        System.out.println(title 
                + "\t" + countFirst
                + "\t" + Utility.hex(itemWithout, " ")
                + "\t" + Utility.hex(itemFirst, " ")
                + "\t" + Utility.hex(itemFull, " ")
                + "\t(" + itemFull + ")"
                + "\t" + EMOJI_DATA_BETA.getName(itemFull));
    }

    public void addAll(Iterable<String> chars) {
        for (String s : chars) {
            if (s.contains("🕵") && !EmojiData.MODIFIERS.containsSome(s)) {
                int debug = 0;
            }
            add(s);
        }        
    }

}
