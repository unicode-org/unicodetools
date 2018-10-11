package org.unicode.tools.emoji.unittest;

import org.unicode.cldr.unittest.TestFmwkPlus;
import org.unicode.tools.emoji.CandidateData;
import org.unicode.tools.emoji.Emoji;

public class TestCandidateData extends TestFmwkPlus {
    public static void main(String[] args) {
        new TestCandidateData().run(args);
    }
    
    CandidateData CANDIDATES = CandidateData.getInstance();
    
    public void TestA() {
        System.out.print(" (Version: " + CANDIDATES.getVersionString() + ") ");
    }
    
    public void TestEmojification() {
        assertTrue("X265F: chess pawn", CANDIDATES.getAllCharacters().contains(0x265F));
        assertTrue("X267E: infinite", CANDIDATES.getAllCharacters().contains(0x267E));
    }
}
