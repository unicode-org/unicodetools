package org.unicode.text.tools;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.text.UCD.IdentifierInfo.IdentifierStatus;
import org.unicode.text.UCD.IdentifierInfo.IdentifierType;

import com.ibm.icu.dev.util.UnicodeMap;

public class XIDModifications {
    private UnicodeMap<IdentifierStatus> identifierStatus = new UnicodeMap<>();
    private UnicodeMap<IdentifierType> identifierType = new UnicodeMap<>();

    public XIDModifications(String directory) {
        identifierType.putAll(0,0x10FFFF, IdentifierType.not_chars);
        identifierStatus.putAll(0,0x10FFFF, IdentifierType.not_chars.identifierStatus);
        new MyReader().process(directory, "xidModifications.txt");
        identifierStatus.freeze();
        identifierType.freeze();
    }

    public UnicodeMap<IdentifierStatus> getStatus() {
        return identifierStatus;
    }
    public UnicodeMap<IdentifierType> getType() {
        return identifierType;
    }

    private class MyReader extends FileUtilities.SemiFileReader {
        @Override
        protected boolean handleLine(int lineCount, int start, int end, String[] items) {
            identifierStatus.putAll(start, end, IdentifierStatus.fromString(items[1]));
            identifierType.putAll(start, end, IdentifierType.fromString(items[2]));
            return true;
        }
    }

}
