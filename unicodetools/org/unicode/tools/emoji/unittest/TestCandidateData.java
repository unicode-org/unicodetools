package org.unicode.tools.emoji.unittest;

import org.unicode.cldr.unittest.TestFmwkPlus;
import org.unicode.tools.emoji.CandidateData;
import org.unicode.tools.emoji.Emoji;

public class TestCandidateData extends TestFmwkPlus {
    public static void main(String[] args) {
        System.out.println("Version: " + Emoji.VERSION_TO_GENERATE + "; isBeta: " + Emoji.IS_BETA);
        new TestCandidateData().run(args);
    }
    
    CandidateData CANDIDATES = CandidateData.getInstance();
    
    public void TestEmojification() {
        assertTrue("X265F: chess pawn", CANDIDATES.getAllCharacters().contains(0x265F));
        assertTrue("X267E: infinite", CANDIDATES.getAllCharacters().contains(0x267E));
    }
}
