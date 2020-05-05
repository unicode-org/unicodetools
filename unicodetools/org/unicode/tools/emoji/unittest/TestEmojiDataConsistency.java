package org.unicode.tools.emoji.unittest;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.unittest.TestFmwkPlus;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Tabber;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.Emoji;
import org.unicode.tools.emoji.EmojiData;
import org.unicode.tools.emoji.EmojiDataSourceCombined;
import org.unicode.tools.emoji.GenerateEmojiData;
import org.unicode.tools.emoji.TempPrintWriter;

import com.google.common.base.Splitter;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.ICUException;

public class TestEmojiDataConsistency extends TestFmwkPlus {
    EmojiData DATA_TO_TEST = TestAll.DATA_TO_TEST;
    EmojiData DATA_TO_TEST_PREVIOUS = TestAll.DATA_TO_TEST_PREVIOUS;
    
    public static void main(String[] args) {
        new TestEmojiDataConsistency().run(args);
    }
    
    public void TestHair() {
        assertTrue("", DATA_TO_TEST_PREVIOUS.getZwjSequencesNormal().contains(EmojiData.MAN_WITH_RED_HAIR));
        assertTrue("", DATA_TO_TEST.getZwjSequencesNormal().contains(EmojiData.MAN_WITH_RED_HAIR));
        assertTrue("", EmojiDataSourceCombined.EMOJI_DATA.getZwjSequencesNormal().contains(EmojiData.MAN_WITH_RED_HAIR));
    }
    
    public void TestVersions() {
        for (String s : DATA_TO_TEST_PREVIOUS.getAllEmojiWithoutDefectives()) {
            if (DATA_TO_TEST.getAllEmojiWithoutDefectives().contains(s)) {
                continue;
            }
            errln("Compatibility failure with\t" + Utility.hex(s) + "; \t" + s + "; \t" + DATA_TO_TEST_PREVIOUS.getName(s));
        }
    }

    public static final Splitter semiOnly = Splitter.onPattern(";").trimResults();
    public static final Splitter hashOnly = Splitter.onPattern("#").trimResults();

    public void TestFiles() {
	checkFiles(TestAll.VERSION_TO_TEST_PREVIOUS_STRING, TestAll.VERSION_TO_TEST_STRING);
    }
    
    public void checkFiles(String oldVersion, String newVersion) {
        File oldDir = new File(GenerateEmojiData.OUTPUT_DIR_BASE + oldVersion);
        File newDir = new File(GenerateEmojiData.OUTPUT_DIR_BASE + newVersion);
        Function<String,String> cleaner = x -> x.replaceAll("\\s+", " ")
                .replace("component", "fully-qualified")
                .replace("minimally-qualified", "non-fully-qualified")
                .replace("unqualified", "non-fully-qualified")
                ;
        Map<String, UnicodeMap<String>> oldProps = new LinkedHashMap<>();
        Map<String, UnicodeMap<String>> newProps = new LinkedHashMap<>();
        Map<String, String> oldPropToFile = new TreeMap<>();
        Map<String, String> newPropToFile = new TreeMap<>();
        for (String oldFileName : oldDir.list()) {
            if (oldFileName.startsWith("ReadMe") 
        	    || oldFileName.startsWith(".DS_Store") 
        	    || new File(oldDir, oldFileName).isDirectory()) {
                continue;
            }
            logln("adding " + oldDir + "/" + oldFileName);
            unicodeToPropToLine(oldDir.toString(), oldFileName, cleaner, oldProps, oldPropToFile);
            logln("adding " + newDir + "/" + oldFileName);
            unicodeToPropToLine(newDir.toString(), oldFileName, cleaner, newProps, newPropToFile);
        }

        UnicodeMap<String> empty = new UnicodeMap<String>().freeze();

        try (TempPrintWriter out = new TempPrintWriter(GenerateEmojiData.OUTPUT_DIR, "internal/emoji-diff.txt")) {
            Set<String> props = new LinkedHashSet<>(oldProps.keySet());
            props.addAll(newProps.keySet());
            for (String prop : props) {
                UnicodeMap<String> oldMap = CldrUtility.ifNull(oldProps.get(prop), empty);
                UnicodeMap<String> newMap = CldrUtility.ifNull(newProps.get(prop), empty);
                boolean isError = false;
                String fileName = oldPropToFile.get(prop) == null ? newPropToFile.get(prop) : oldPropToFile.get(prop);
                String prefix = "# " + fileName + "/" + prop + ": " + newVersion;
                if (oldMap.keySet().equals(newMap.keySet())) {
                    logOrError(LOG, out, prefix + " = " +  oldVersion); 
                    continue;
                } else if (newMap.keySet().containsAll(oldMap.keySet())) {
                    logOrError(LOG, out, prefix + " ⊇ " +  oldVersion);
                } else {
                    logOrError(ERR, out, prefix + " ⊉ " +  oldVersion);
                    isError = true;
                }
                // # emoji-test.txt, fully-qualified: 12.0 ⊇ 11.0
                inFirstButNotSecond(isVerbose() || isError, out, oldVersion, oldMap.keySet(),
                        newMap.keySet(), prop, "ONLY IN " + oldVersion);
                inFirstButNotSecond(isVerbose() || isError, out, newVersion, newMap.keySet(), oldMap.keySet(),
                        prop, "ONLY IN " + newVersion);
            }
        }

        // check that all the other properties add up to fully-qualified
        UnicodeSet fully_qualified = null;
        UnicodeSet other = new UnicodeSet();
        for (Entry<String, UnicodeMap<String>> entry : newProps.entrySet()) {
            String key = entry.getKey();
            UnicodeMap<String> value = entry.getValue();
            switch (key) {
            case "fully-qualified": 
                fully_qualified = value.keySet(); 
                break;
            default:
                break;
            case "Basic_Emoji": 
            case "Emoji_ZWJ_Sequence": 
            case "Emoji_Keycap_Sequence": 
            case "Emoji_Flag_Sequence": 
            case "Emoji_Tag_Sequence": 
            case "Emoji_Modifier_Sequence": 
                System.out.println("Adding " + key + ", " + value.size());
                other.add(value.keySet());
                break;
            }
        }
        if (!fully_qualified.equals(other)) {
            errln("fully-qualified ≠ main-props");
            inFirstButNotSecond(true, null, oldVersion, fully_qualified, other, "", "ONLY IN " + "fully-qualified - main-props");
            inFirstButNotSecond(true, null, newVersion, other, fully_qualified, "", "ONLY IN " + "main-props - fully-qualified");
        }
    }
    /* 
NEW
#       component           — an Emoji_Component,
#                             excluding Regional_Indicators, ASCII, and non-Emoji.
#       fully-qualified     — a fully-qualified emoji (see ED-18 in UTS #51),
#                             excluding Emoji_Component
#       minimally-qualified — a minimally-qualified emoji (see ED-18a in UTS #51)
#       unqualified         — a unqualified emoji (See ED-19 in UTS #51)
OLD
#       fully-qualified — see “Emoji Implementation Notes” in UTS #51
#       non-fully-qualified — see “Emoji Implementation Notes” in UTS #51
     */

    private void logOrError(int logOrError, TempPrintWriter out, String message) {
        msg(message, logOrError, true, true);
        out.println(message);
    }

    Tabber tabber = new Tabber.MonoTabber()
            .add(40, Tabber.LEFT)
            .add(20, Tabber.LEFT);
    //    tabber.add(2, Tabber.LEFT) // hash
    //    .add(4, Tabber.RIGHT) // version
    //    .add(6, Tabber.RIGHT) // count
    //    .add(10, Tabber.LEFT) // character
    //    ;

    private void inFirstButNotSecond(boolean writeToConsole, TempPrintWriter out, String version, 
            UnicodeSet oldSet, UnicodeSet newSet, String prop, String message) {
        UnicodeSet oldMinusNew = new UnicodeSet(oldSet).removeAll(newSet);
        if (oldMinusNew.isEmpty()) {
            printlnAndLog(writeToConsole, out, "# " + message + " = ∅");
            return;
        }
        printlnAndLog(writeToConsole, out, "# " + message + " = ↙️");
        for (UnicodeSetIterator it = new UnicodeSetIterator(oldMinusNew); it.nextRange(); ) {
            // # 😀 grinning face
            if (it.string != null) {
                printlnAndLog(writeToConsole, out, tabber.process(
                        Utility.hex(it.string) 
                        + "; \t" + prop 
                        + "\t# " + it.string 
                        + "    " + getName(it.string)));
            } else if (it.codepoint == it.codepointEnd) {
                printlnAndLog(writeToConsole, out, tabber.process(
                        Utility.hex(it.codepoint) 
                        + "; \t" + prop
                        + "\t# " + UTF16.valueOf(it.codepoint) 
                        + "    " + getName(it.codepoint)));
            } else {
                printlnAndLog(writeToConsole, out, tabber.process(
                        Utility.hex(it.codepoint) + ".." + Utility.hex(it.codepointEnd) 
                        + "; \t" + prop
                        + "\t# " + UTF16.valueOf(it.codepoint) + ".." + UTF16.valueOf(it.codepointEnd) 
                        + " " + getName(it.codepoint) + ".." + getName(it.codepointEnd)));
            }
        }
    }

    private String getName(int emoji) {
        try {
            return DATA_TO_TEST.getName(emoji);
        } catch (Exception e) {
            return "<noname " + Utility.hex(emoji) + ">";
        }
    }

    private String getName(String emoji) {
        try {
            return DATA_TO_TEST.getName(emoji);
        } catch (Exception e) {
            return "<noname " + Utility.hex(emoji) + ">";
        }
    }


    private void printlnAndLog(boolean writeToConsole, TempPrintWriter out, String message) {
        if (writeToConsole) {
            System.out.println(message);
        }
        if (out != null) {
            out.println(message);
        }
    }

    /**
     * returns keys of the form a; b, with the hashes removed, whitespace trimmed/normalized
     * @param oldDir
     * @param oldFileName
     * @param oldPropToFile 
     * @param oldProps 
     * @return
     */
    private Map<String, UnicodeMap<String>> unicodeToPropToLine(String oldDir, String oldFileName, Function<String,String> transform, 
            Map<String, UnicodeMap<String>> result, Map<String, String> propToFile) {
        int linesSkipped = 0;
        int linesRead = 0;
        for (String line : FileUtilities.in(oldDir, oldFileName)) {
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            List<String> coreList = hashOnly.splitToList(line);
            List<String> list = semiOnly.splitToList(coreList.get(0));
            if (list.size() < 2)  {
                linesSkipped++;
                continue;
            }
            linesRead++;
            String f0 = list.get(0);
            int pos = f0.indexOf("..");
            final String property = transform.apply(list.get(1));
            UnicodeMap<String> submap = result.get(property);
            if (submap == null) {
                result.put(property, submap = new UnicodeMap<>());
            }
            String oldFile = propToFile.get(property);
            if (oldFile == null) {
                propToFile.put(property, oldFileName);
            } else if (!oldFile.equals(oldFileName)) {
                throw new ICUException("Property " + property + " in 2 files: " + oldFile + ", " + oldFileName);
            }
            if (pos < 0) {
                String source = Utility.fromHex(f0);
                submap.put(source, line);
            } else {
                int codePointStart = Integer.parseInt(f0.substring(0,pos), 16);
                int codePointEnd = Integer.parseInt(f0.substring(pos+2), 16);
                submap.putAll(codePointStart, codePointEnd, line);
            }
        }
        logln(oldFileName + " linesSkipped: " + linesSkipped + "; linesRead " + linesRead);
        return result;
    }
}
