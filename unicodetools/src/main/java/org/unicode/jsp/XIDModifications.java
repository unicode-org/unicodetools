package org.unicode.jsp;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;

public class XIDModifications {
    static UnicodeSet allowed = new UnicodeSet();
    static UnicodeMap<String> reasons = new UnicodeMap<String>();

    static class MyReader extends FileUtilities.SemiFileReader {
        // add other
        //  # @missing: 0000..10FFFF; restricted ; not-chars
        @Override
        protected boolean handleLine(int start, int end, String[] items) {
            final String type = items[1];
            if (type.equals("allowed")) {
                allowed.add(start, end);
            } else if (type.equals("restricted")) {
                //        allowed.remove(start, end);
            } else {
                throw new IllegalArgumentException(type);
            }
            reasons.putAll(start, end, items[2]);
            return true;
        }
    }
    static {
        reasons.putAll(0,0x10FFFF,"not-chars");
        //reasons.putAll(new UnicodeSet("[[:gc=cn:][:gc=co:][:gc=cs:][:gc=cc:]-[:whitespace:]]"),"not-char");
        new MyReader().process(XIDModifications.class, "xidmodifications.txt");
        allowed.freeze();
        reasons.freeze();
    }
    public static UnicodeSet getAllowed() {
        return allowed;
    }
    public static UnicodeMap<String> getReasons() {
        return reasons;
    }
}
