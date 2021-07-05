package org.unicode.tools.emoji.unittest;

import java.util.Set;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.Emoji;
import org.unicode.tools.emoji.Emoji.CharSource;
import org.unicode.unittest.TestFmwkMinusMinus;
import org.unicode.tools.emoji.EmojiData;
import org.unicode.tools.emoji.EmojiDataSource;
import org.unicode.tools.emoji.EmojiDataSourceCombined;
import org.unicode.tools.emoji.GenerateEmoji;
import org.unicode.tools.emoji.ProposalData;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeMap.EntryRange;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.VersionInfo;

public class TestProposalData extends TestFmwkMinusMinus {
    private static final ProposalData proposalData = ProposalData.getInstance();


    @Disabled("Broken")
    @Test
    public void TestACase() {
        String case1 = Utility.fromHex("1F468 1F3FC 200D 1F91D 200D 1F468 1F3FB");
        Set<String> proposals = proposalData.getProposals(case1);

        assertNotEquals(case1, 0, proposals.size());
    }

    @Disabled("Broken")
    @Test
    public void TestCompleteness13_12_1() {
        checkVersionCompleteness2(ERR, Emoji.VERSION12_1, Emoji.VERSION12);
    }

    @Disabled("Broken")
    @Test
    public void TestCompleteness12_1_12() {
        checkVersionCompleteness2(ERR, Emoji.VERSION12_1, Emoji.VERSION12);
    }

    @Disabled("Broken")
    @Test
    public void TestCompleteness12_11() {
        checkVersionCompleteness2(ERR, Emoji.VERSION12, Emoji.VERSION11);
    }

    @Test
    public void TestCompleteness11_5() {
        checkVersionCompleteness2(ERR, Emoji.VERSION11, Emoji.VERSION5);
    }

    @Test
    public void TestCompleteness5_4() {
        checkVersionCompleteness2(ERR, Emoji.VERSION5, Emoji.VERSION4);
    }

    @Test
    public void TestCompleteness4_3() {
        checkVersionCompleteness2(WARN, Emoji.VERSION4, Emoji.VERSION3);
    }

    @Test
    public void TestCompleteness3_2() {
        checkVersionCompleteness2(WARN, Emoji.VERSION3, Emoji.VERSION2);
    }

    @Test
    public void TestCompleteness2_1() {
        checkVersionCompleteness2(WARN, Emoji.VERSION2, Emoji.VERSION1);
    }

    @Test
    public void TestCompleteness1_0() {
        checkVersionCompleteness2(WARN, Emoji.VERSION1, null);
    }

    private void checkVersionCompleteness2(int logOrErr, VersionInfo newer, VersionInfo older) {
        checkVersionCompleteness(logOrErr,
                newer,
                EmojiData.of(newer).getAllEmojiWithoutDefectives(),
                older == null ? UnicodeSet.EMPTY : EmojiData.of(older).getAllEmojiWithoutDefectives());
    }


    private void checkVersionCompleteness(int logOrError, VersionInfo versionInfo, UnicodeSet newerData, UnicodeSet currentData) {
        UnicodeSet missing = new UnicodeSet();
        UnicodeMap<String> found = new UnicodeMap<>();
        checkVersions(currentData, newerData, found, missing);

        if (isVerbose()) {
            logln("\n" + versionInfo + " Found Proposals: " + found.size() + "\n");
            for (EntryRange<String> entry : found.entryRanges()) {
                if (entry.string != null) {
                    logln(ProposalData.showLine(entry.string, entry.value));
                } else {
                    logln(ProposalData.showLine(entry.codepoint, entry.codepointEnd, entry.value));
                }
            }

        }
        if (!missing.isEmpty()) {
            msg("\n" + versionInfo + " Missing Proposals: " + missing.size() + " (use -v to see)\n", logOrError, true, true);
            if (isVerbose()) {
                for (String emoji : missing) {
                    String bestGuess = getBestGuess(emoji);
                    if (bestGuess != null) {
                        System.out.println(ProposalData.showLine(emoji, bestGuess));
                    }
                }
                for (String emoji : missing) {
                    String bestGuess = getBestGuess(emoji);
                    if (bestGuess == null) {
                        System.out.println(ProposalData.showLine(emoji, "MISSING"));
                    }
                }
            }
        }
    }

    private String getBestGuess(String emoji) {
        //        if (Emoji.DINGBATS.contains(emoji)) {
        //            return Emoji.CharSource.ZDings;
        //        }
        //        if (Emoji.JSOURCES.contains(emoji)) {
        //            return Emoji.CharSource.JCarrier;
        //        }
        //        if (Emoji.DING_MAP.containsKey(emoji)) {
        //            return Emoji.CharSource.WDings;
        //        }
        //        if (Emoji.ARIB.contains(emoji)) {
        //            return Emoji.CharSource.ARIB;
        //        }
        String trial = EmojiData.SKIN_SPANNER.replaceFrom(emoji,"");
        if (!trial.isEmpty()) {
            Set<String> proposals = proposalData.getProposals(trial);
            if (!proposals.isEmpty()) {
                return CollectionUtilities.join(proposals, ", ");
            }
        }
        return null;
    }

    private void checkVersions(UnicodeSet oldEmoji, UnicodeSet newEmoji, UnicodeMap<String> found, UnicodeSet missing) {
        for (String emoji : newEmoji) {
            if (oldEmoji.contains(emoji)) {
                continue;
            }
            Set<String> proposals = proposalData.getProposals(emoji);
            String skeleton = ProposalData.getSkeleton(emoji);
            if (proposals.isEmpty()) {
                missing.add(skeleton);
                continue;
            }
            String proposalString = CollectionUtilities.join(proposals, ", ");
            found.put(skeleton, proposalString);
        }
    }
}
