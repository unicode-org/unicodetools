package org.unicode.propstest;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeMap.EntryRange;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.unicode.cldr.util.Counter;
import org.unicode.props.GenerateEnums;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.PropertyNames;
import org.unicode.props.PropertyValueSets;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.props.UcdPropertyValues.Binary;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.props.UcdPropertyValues.Line_Break_Values;
import org.unicode.props.UcdPropertyValues.Numeric_Type_Values;
import org.unicode.props.UcdPropertyValues.Script_Values;
import org.unicode.props.ValueCardinality;
import org.unicode.tools.emoji.EmojiData;
import org.unicode.unittest.TestFmwkMinusMinus;

public class TestProperties extends TestFmwkMinusMinus {

    // TODO generate list of versions, plus 'latest'

    private static final IndexUnicodeProperties iup =
            IndexUnicodeProperties.make(GenerateEnums.ENUM_VERSION);
    private static final UnicodeMap<String> newName = iup.load(UcdProperty.Name);
    private static final UnicodeMap<String> blocks = iup.load(UcdProperty.Block);
    private static final IndexUnicodeProperties lastVersion = IndexUnicodeProperties.make("6.3");
    private static final UnicodeMap<String> generalCategory =
            iup.load(UcdProperty.General_Category);
    private static final UnicodeSet newChars =
            iup.load(UcdProperty.Age).getSet(UcdPropertyValues.Age_Values.V7_0.name());
    private static final UnicodeMap<String> nameMap = iup.load(UcdProperty.Name);

    @Test
    public void TestPropertyValuesSetCoverage() {
        EnumSet<General_Category_Values> all = EnumSet.allOf(General_Category_Values.class);
        all.removeAll(PropertyValueSets.CONTROL);
        all.removeAll(PropertyValueSets.LETTER);
        all.removeAll(PropertyValueSets.MARK);
        all.removeAll(PropertyValueSets.NUMBER);
        all.removeAll(PropertyValueSets.PUNCTUATION);
        all.removeAll(PropertyValueSets.SEPARATOR);
        all.removeAll(PropertyValueSets.SYMBOL);
        all.removeAll(
                EnumSet.of(
                        General_Category_Values.Other,
                        General_Category_Values.Letter,
                        General_Category_Values.Cased_Letter,
                        General_Category_Values.Mark,
                        General_Category_Values.Number,
                        General_Category_Values.Punctuation,
                        General_Category_Values.Separator,
                        General_Category_Values.Symbol));
        assertEquals("", Collections.EMPTY_SET, all);
    }

    @Test
    public void TestNames() {
        for (Binary value : Binary.values()) {
            PropertyNames<Binary> propNames = value.getNames();
            for (String name : propNames.getAllNames()) {
                assertEquals("", value, Binary.forName(name));
            }
        }
        for (Line_Break_Values value : Line_Break_Values.values()) {
            PropertyNames<Line_Break_Values> propValueNames = value.getNames();
            for (String name : propValueNames.getAllNames()) {
                assertEquals(name, value, Line_Break_Values.forName(name));
            }
        }
    }

    @Disabled("Broken")
    @Test
    public void TestAAEmoji() {
        EmojiData emojiData = EmojiData.EMOJI_DATA;
        {
            UnicodeMap<Binary> emoji = iup.loadEnum(UcdProperty.Emoji, Binary.class);
            assertSameContents(
                    "Emoji", emojiData.getSingletonsWithoutDefectives(), emoji.getSet(Binary.Yes));
        }
        {
            UnicodeMap<Binary> presentation =
                    iup.loadEnum(UcdProperty.Emoji_Presentation, Binary.class);
            assertSameContents(
                    "Emoji_Presentation",
                    emojiData.getEmojiPresentationSet(),
                    presentation.getSet(Binary.Yes));
        }
        {
            UnicodeMap<Binary> modifiers = iup.loadEnum(UcdProperty.Emoji_Modifier, Binary.class);
            assertSameContents("Emoji_Modifier", EmojiData.MODIFIERS, modifiers.getSet(Binary.Yes));
        }
        {
            UnicodeMap<Binary> bases = iup.loadEnum(UcdProperty.Emoji_Modifier_Base, Binary.class);
            assertSameContents(
                    "Emoji_Modifier_Base", emojiData.getModifierBases(), bases.getSet(Binary.Yes));
        }
    }

    private void assertSameContents(String string, UnicodeSet a, UnicodeSet b) {
        if (!a.equals(b)) {
            assertEquals(
                    string + " (missing)", "[]", new UnicodeSet(a).removeAll(b).toPattern(false));
            assertEquals(
                    string + " (extra)", "[]", new UnicodeSet(b).removeAll(a).toPattern(false));
        } else {
            logln(string + " OK:\t" + a.toPattern(false));
        }
    }

    private <T> void showByValue(
            UcdProperty prop, UnicodeMap<T> emojiStyle, ValueCardinality cardinality) {
        TreeSet<T> sorted =
                cardinality == ValueCardinality.Singleton
                        ? new TreeSet<T>()
                        : new TreeSet<T>(new SetComparator());
        sorted.addAll(emojiStyle.values());
        for (T value : sorted) {
            if (value == Binary.No) {
                continue;
            }
            UnicodeSet us = emojiStyle.getSet(value);
            logln("\t" + value + ":\t" + us.size() + "\t" + us.toPattern(false));
        }
    }

    //    public static <T extends Comparable, U extends Set<T>> int compare(U o1, U o2) {
    //        int diff = o1.size() - o2.size();
    //        if (diff != 0) {
    //            return diff;
    //        }
    //        Collection<T> x1 = SortedSet.class.isInstance(o1) ? o1 : new TreeSet<T>(o1);
    //        Collection<T> x2 = SortedSet.class.isInstance(o2) ? o2 : new TreeSet<T>(o2);
    //        return CollectionUtilities.compare(x1, x2);
    //    }

    public static class SetComparator<T extends Comparable> implements Comparator<Set<T>> {
        public int compare(Set<T> o1, Set<T> o2) {
            return CollectionUtilities.compare((Collection<T>) o1, (Collection<T>) o2);
        }
    }
    ;

    @Test
    public void TestAAScripts() {
        UnicodeMap<String> scriptInfo = iup.load(UcdProperty.Script);
        UnicodeSet unknownScript =
                scriptInfo.getSet(UcdPropertyValues.Script_Values.Unknown.toString());
        unknownScript
                .removeAll(
                        generalCategory.getSet(
                                UcdPropertyValues.General_Category_Values.Unassigned.toString()))
                .removeAll(
                        generalCategory.getSet(
                                UcdPropertyValues.General_Category_Values.Private_Use.toString()))
                .removeAll(
                        generalCategory.getSet(
                                UcdPropertyValues.General_Category_Values.Surrogate.toString()));
        UnicodeSet unknownMarks =
                new UnicodeSet(
                                generalCategory.getSet(
                                        UcdPropertyValues.General_Category_Values.Nonspacing_Mark
                                                .toString()))
                        .addAll(
                                generalCategory.getSet(
                                        UcdPropertyValues.General_Category_Values.Enclosing_Mark
                                                .toString()))
                        .addAll(
                                generalCategory.getSet(
                                        UcdPropertyValues.General_Category_Values.Spacing_Mark
                                                .toString()))
                        .retainAll(unknownScript);
        unknownScript.removeAll(unknownMarks);
        assertEquals("Missing Inherited", UnicodeSet.EMPTY, unknownMarks);
        assertEquals("Missing Common", UnicodeSet.EMPTY, unknownScript);
        for (UnicodeSetIterator it = new UnicodeSetIterator(unknownScript); it.nextRange(); ) {
            logln(
                    Utility.hex(it.codepoint)
                            + ".."
                            + Utility.hex(it.codepointEnd)
                            + "; Common # ("
                            + generalCategory.get(it.codepoint)
                            + ".."
                            + generalCategory.get(it.codepointEnd)
                            + ") "
                            + nameMap.get(it.codepoint)
                            + ".."
                            + nameMap.get(it.codepointEnd));
        }
    }

    @Test
    public void TestJoiningGroupConsistency() {
        // TODO(egg): I would like to be able to put that in the invariants tests as « the partition
        // defined by Joining_Group is finer than that defined by Joining_Type ».
        UnicodeMap<String> joiningGroup = iup.load(UcdProperty.Joining_Group);
        UnicodeMap<String> joiningType = iup.load(UcdProperty.Joining_Type);
        var charactersByJoiningGroup = new HashMap<String, UnicodeSet>();
        joiningGroup.addInverseTo(charactersByJoiningGroup).remove("No_Joining_Group");
        charactersByJoiningGroup.forEach(
                (group, set) -> {
                    final int first = set.getRangeStart(0);
                    final String firstType = joiningType.get(first);
                    set.forEach(
                            (c) -> {
                                assertEquals(
                                        Utility.hex(c)
                                                + " and "
                                                + Utility.hex(first)
                                                + " have different joining types but are in the"
                                                + " same joining group "
                                                + group,
                                        joiningType.get(c),
                                        firstType);
                            });
                });
    }

    @Test
    public void TestScripts() {

        logln("New chars: " + newChars.size());
        {
            LinkedHashSet values = new LinkedHashSet(Arrays.asList(Script_Values.values()));
            values.remove(Script_Values.Unknown);
            values.remove(Script_Values.Katakana_Or_Hiragana);
            listValues(
                    UcdProperty.Script,
                    values,
                    new Transform<Script_Values, String>() {
                        @Override
                        public String transform(Script_Values source) {
                            return source.getShortName();
                        }
                    });
        }
        {
            LinkedHashSet values =
                    new LinkedHashSet(Arrays.asList(General_Category_Values.values()));
            listValues(
                    UcdProperty.General_Category,
                    values,
                    new Transform<General_Category_Values, String>() {
                        @Override
                        public String transform(General_Category_Values source) {
                            return source.getShortName();
                        }
                    });
        }
    }

    public <T extends Enum<T>> void listValues(
            UcdProperty ucdProperty, Collection<T> values, Transform<T, String> transform) {
        UnicodeMap<String> scripts = iup.load(ucdProperty);
        UnicodeMap<String> oldScripts = lastVersion.load(ucdProperty);

        Counter<T> newScriptCounts = new Counter<T>();
        Counter<T> oldScriptCounts = new Counter<T>();
        for (T s : values) {
            String name = s.name();
            UnicodeSet set = scripts.getSet(name);
            if (set.size() == 0) {
                continue;
            }
            UnicodeSet newSet = new UnicodeSet(set).retainAll(newChars);
            int oldSize = set.size() - newSet.size();
            if (oldSize > 0) {
                oldScriptCounts.add(s, oldSize);
            }
            if (newSet.size() > 0) {
                newScriptCounts.add(s, newSet.size());
            }
        }
        logln("#counts");
        LinkedHashSet<T> sorted = new LinkedHashSet(newScriptCounts.getKeysetSortedByCount(false));
        sorted.addAll(oldScriptCounts.getKeysetSortedByCount(false));
        NumberFormat nf = NumberFormat.getInstance();
        for (T c : sorted) {
            logln(
                    nf.format(oldScriptCounts.get(c))
                            + "\t"
                            + nf.format(newScriptCounts.get(c))
                            + "\t"
                            + transform.transform(c)
                            + "\t"
                            + c);
        }
    }

    public HashMap<String, String> getSkeletonMap(
            Collection<String> collection, boolean skeletonToNormal) {
        HashMap<String, String> capNewScripts = new HashMap<String, String>();
        for (String normal : collection) {
            String skeleton = normal.toUpperCase(Locale.ENGLISH).replace("_", "");
            if (skeletonToNormal) {
                capNewScripts.put(skeleton, normal);
            } else {
                capNewScripts.put(normal, skeleton);
            }
        }
        return capNewScripts;
    }

    private void showValues(UnicodeSet us) {
        for (UnicodeSetIterator it = new UnicodeSetIterator(us); it.nextRange(); ) {
            int start = it.codepoint;
            int end = it.codepointEnd;
            logln(
                    Utility.hex(start)
                            + (start == end ? "" : ".." + Utility.hex(end))
                            + "\t#\t"
                            + newName.get(start)
                            + (start == end ? "" : ".." + newName.get(end)));
        }
    }

    private <T> void showValues(UnicodeMap<T> us) {
        Iterable<EntryRange<T>> ers = us.entryRanges();
        for (EntryRange<T> it : ers) {
            if (it.value == null) {
                continue;
            }
            int start = it.codepoint;
            int end = it.codepointEnd;
            logln(
                    Utility.hex(start)
                            + (start == end ? "" : ".." + Utility.hex(end))
                            + " ;\t"
                            + it.value
                            + "\t#\t"
                            + newName.get(start)
                            + (start == end ? "" : ".." + newName.get(end)));
        }
    }

    @Test
    public void TestIdn() {
        show(UcdProperty.Idn_Status);
        show(UcdProperty.Idn_2008);
        show(UcdProperty.Idn_Mapping);
    }

    @Test
    public void TestIdmod() {
        show(UcdProperty.Identifier_Status);
        show(UcdProperty.Identifier_Type);
        show(UcdProperty.Confusable_MA);
    }

    static class ExemplarExceptions {
        static final Map<String, ExemplarExceptions> exemplarExceptions = new HashMap<>();
        UnicodeSet additions = new UnicodeSet();
        UnicodeSet subtractions = new UnicodeSet();

        ExemplarExceptions add(String additions) {
            if (additions != null) {
                this.additions.addAll(new UnicodeSet(additions));
            }
            return this;
        }

        ExemplarExceptions remove(String subtractions) {
            if (subtractions != null) {
                this.subtractions.addAll(subtractions);
            }
            return this;
        }

        static ExemplarExceptions get(String locale) {
            ExemplarExceptions ee = exemplarExceptions.get(locale);
            if (ee == null) {
                exemplarExceptions.put(locale, ee = new ExemplarExceptions());
            }
            return ee;
        }

        public static void add(String locale, String chars) {
            ExemplarExceptions.get(locale).add(chars);
        }

        public static void remove(String locale, String chars) {
            ExemplarExceptions.get(locale).remove(chars);
        }

        static {
            add("en", "[0-9]"); // good enough
            add("ar", "[٠-٩]"); // arab
            add("fa", "[۰-۹]"); // arabext
            add("ks", "[۰-۹]"); // arabext
            add("pa_Arab", "[۰-۹]"); // arabext
            add("ps", "[۰-۹]"); // arabext
            add("ur_IN", "[۰-۹]"); // arabext
            add("uz_Arab", "[۰-۹]"); // arabext
            add("as", "[০-৯]"); // beng
            add("bn", "[০-৯]"); // beng
            add("mr", "[०-९]"); // deva
            add("ne", "[०-९]"); // deva
            add("my", "[၀-၉]"); // mymr
            add("dz", "[༠-༩]"); // tibt
            remove("ks", "[ٖٗٚٛٮ۪ۭ]");
            remove("kn", "[ೞ]");
            remove("km", "[់-៑]");
            remove("si", "[ෟ]");
        }
    }

    public String getCodeAndName(String cp) {
        return Utility.hex(cp) + " (" + cp + ") " + nameMap.get(cp);
    }

    private UnicodeMap<String> show(UcdProperty ucdProperty) {
        UnicodeMap<String> propMap = iup.load(ucdProperty);
        int count = 0;
        for (String value : propMap.values()) {
            if (++count > 50) {
                logln("...");
                break;
            }
            UnicodeSet set = propMap.getSet(value);
            logln(ucdProperty + "\t" + value + "\t" + set);
        }
        return propMap;
    }

    @Test
    public void TestValues() {
        for (final UcdProperty prop : UcdProperty.values()) {
            logln(prop + "\t" + prop.getNames() + "\t" + prop.getEnums());
            //            Collection<Enum> values = PropertyValues.valuesOf(prop);
            //            logln("values: " + values);
        }
        for (final UcdPropertyValues.General_Category_Values prop :
                UcdPropertyValues.General_Category_Values.values()) {
            logln(prop + "\t" + prop.getNames());
            //            Collection<Enum> values = PropertyValues.valuesOf(prop);
            //            logln("values: " + values);
        }

        final UcdPropertyValues.General_Category_Values q =
                UcdPropertyValues.General_Category_Values.Unassigned;
        logln(q.getNames().toString());

        //        Enum x = PropertyValues.forValueName(UcdProperty.General_Category, "Cc");
        //        //Bidi_Mirrored_Values y = Properties.Bidi_Mirrored_Values.No;
        //        Enum z = PropertyValues.forValueName(UcdProperty.Bidi_Mirrored, "N");
        //        Enum w = PropertyValues.forValueName(UcdProperty.General_Category, "Cc");
        //        logln(x + " " + z + " " + w);
    }

    @Test
    public void TestNumbers() {
        for (final Age_Values age : Age_Values.values()) {
            if (age == Age_Values.Unassigned) { //  || age.compareTo(Age_Values.V4_0) < 0
                continue;
            }
            final PropertyNames<Age_Values> names = age.getNames();
            // logln(names.getShortName());
            final IndexUnicodeProperties props = IndexUnicodeProperties.make(names.getShortName());
            final UnicodeMap<String> gc = props.load(UcdProperty.General_Category);
            final UnicodeMap<String> nt = props.load(UcdProperty.Numeric_Type);
            final UnicodeSet gcNum =
                    new UnicodeSet()
                            .addAll(gc.getSet(General_Category_Values.Decimal_Number.toString()))
                            .addAll(gc.getSet(General_Category_Values.Letter_Number.toString()))
                            .addAll(gc.getSet(General_Category_Values.Other_Number.toString()));
            final UnicodeSet ntNum =
                    new UnicodeSet()
                            .addAll(nt.getSet(Numeric_Type_Values.Decimal.toString()))
                            .addAll(nt.getSet(Numeric_Type_Values.Digit.toString()))
                            .addAll(nt.getSet(Numeric_Type_Values.Numeric.toString()));
            UnicodeSet diff;
            //            diff = new UnicodeSet(ntNum).removeAll(gcNum);
            //            logln(age + ", nt-gc:N" + diff);
            diff = new UnicodeSet(gcNum).removeAll(ntNum);
            logln(age + ", gc:N-nt" + diff);
        }
    }
}
