package org.unicode.tools.emoji.unittest;

import com.ibm.icu.text.UnicodeSet;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.unicode.tools.emoji.CandidateData;
import org.unicode.tools.emoji.EmojiDataSource;
import org.unicode.tools.emoji.EmojiOrder.MajorGroup;
import org.unicode.unittest.TestFmwkMinusMinus;

@Disabled("broken?")
public class TestCandidateData extends TestFmwkMinusMinus {

    CandidateData CANDIDATES = CandidateData.getInstance();

    @Test
    public void TestA() {
        System.out.print(" (Version: " + CANDIDATES.getVersionString() + ") ");
    }

    /**
     * These need to be changed each release; they may be empty in a release. In that case,
     * logKnownError
     */
    @Test
    @Disabled("broken?")
    public void TestEmojification() {
        assertTrue("U+26a7: chess pawn", CANDIDATES.getAllCharacters().contains(0x26a7));
    }

    /**
     * These need to be changed each release; they may be empty in a release. In that case,
     * logKnownError
     */
    @Test
    public void TestCandidateCombinations() {
        UnicodeSet all = CANDIDATES.getAllCharacters();
        Asserts.assertContains(
                this,
                "",
                "zwj-sequence",
                all,
                "Mx Claus",
                new StringBuilder()
                        .appendCodePoint(0x1F9D1)
                        .appendCodePoint(0x200D)
                        .appendCodePoint(0x1F384)
                        .toString());
    }

    @Test
    public void TestGroupEmoji() {
        EmojiDataSource source = CANDIDATES;
        Asserts.assertContains(
                this,
                "",
                "modifierBases",
                source.getModifierBases(),
                "multipersonGroupings",
                source.getMultiPersonGroupings());
        System.out.print(" (modifierBases: " + Asserts.flat(source.getModifierBases()) + ") ");
        System.out.print(
                " (multipersonGroupings: " + Asserts.flat(source.getMultiPersonGroupings()) + ") ");
        //        Asserts.assertContains(this, "", "👯🤼", source.getGenderBases(),
        // "multipersonGroupings", new UnicodeSet("[👯🤼]"));
        System.out.print(" (genderBases: " + Asserts.flat(source.getGenderBases()) + ") ");
    }

    @Test
    public void TestMajorMinorGroups() {
        String cat = CANDIDATES.getCategory("🦯");
        assertNotNull("1F9AF category", cat);
        MajorGroup majorGroup = CANDIDATES.getMajorGroupFromCategory(cat);
        assertNotNull("MajorGroup for " + cat, majorGroup.toSourceString());
        assertNotEquals("MajorGroup for " + cat, "other", majorGroup.toSourceString());
    }
}
