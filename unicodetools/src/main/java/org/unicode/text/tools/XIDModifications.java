package org.unicode.text.tools;

import java.io.File;
import java.util.Collections;
import java.util.Set;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.text.UCD.IdentifierInfo.Identifier_Status;
import org.unicode.text.UCD.IdentifierInfo.Identifier_Type;
import org.unicode.text.utility.Settings;

import com.google.common.collect.ImmutableSet;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.util.VersionInfo;

public class XIDModifications {
    private UnicodeMap<Identifier_Status> identifierStatus = new UnicodeMap<>();
    private UnicodeMap<Set<Identifier_Type>> identifierType = new UnicodeMap<>();

    public XIDModifications(String directory) {
        File dir = new File(directory);
        String finalPart = dir.getName();
        if (finalPart.equals(Settings.NEXT_VERSION_FOLDER_NAME)) {
            finalPart = Settings.latestVersion;
        }
        VersionInfo version = VersionInfo.getInstance(finalPart);
        identifierStatus.putAll(0,0x10FFFF, Identifier_Status.restricted);
        if (version.getMajor() == 9) {
            // Version 9 IdentifierType.txt:
            // Any missing values have the value: IdentifierType={Recommended}
            identifierType.putAll(0, 0x10FFFF, Collections.singleton(Identifier_Type.recommended));
        } else {
            // Version 10+ IdentifierType.txt:
            // Any missing code points have the IdentifierType value Not_Character
            identifierType.putAll(0, 0x10FFFF, Collections.singleton(Identifier_Type.not_characters));
        }
        if (version.getMajor() <= 8) {
            new MyReader().process(directory, "xidmodifications.txt");
        } else {
            new MyReaderType().process(directory, "IdentifierType.txt");
            new MyReaderStatus().process(directory, "IdentifierStatus.txt");
        }
        identifierStatus.freeze();
        identifierType.freeze();
    }

    public UnicodeMap<Identifier_Status> getStatus() {
        return identifierStatus;
    }
    public UnicodeMap<Set<Identifier_Type>> getType() {
        return identifierType;
    }

    private class MyReaderType extends FileUtilities.SemiFileReader {
        @Override
        protected boolean handleLine(int lineCount, int start, int end, String[] items) {
            ImmutableSet.Builder<Identifier_Type> b = ImmutableSet.builder();
            for (String s : items[1].split(" ")) {
                b.add(Identifier_Type.fromString(s));
            }
            identifierType.putAll(start, end, b.build());
            return true;
        }
    }
    private class MyReaderStatus extends FileUtilities.SemiFileReader {
        @Override
        protected boolean handleLine(int lineCount, int start, int end, String[] items) {
            identifierStatus.putAll(start, end, Identifier_Status.fromString(items[1]));
            return true;
        }
    }

    private class MyReader extends FileUtilities.SemiFileReader {
        @Override
        protected boolean handleLine(int lineCount, int start, int end, String[] items) {
            identifierStatus.putAll(start, end, Identifier_Status.fromString(items[1]));
            identifierType.putAll(start, end, Collections.singleton(Identifier_Type.fromString(items[2])));
            return true;
        }
    }
}
