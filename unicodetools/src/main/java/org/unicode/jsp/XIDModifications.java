package org.unicode.jsp;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;

public class XIDModifications {
    private static UnicodeMap<String> allowed = new UnicodeMap(); // "[:XID_Continue:]");
    private static UnicodeMap<String> reasons = new UnicodeMap<String>();

    static class MyReader extends FileUtilities.SemiFileReader {

        @Override
        protected boolean handleLine(int start, int end, String[] items) {
//            String type = items[1];
//            if (type.equalsIgnoreCase("allowed")) {
//                reasons.putAll(start, end, items[2]);
//            } else if (type.equalsIgnoreCase("restricted")) {
//                //        allowed.remove(start, end);
//            } else {
//                throw new IllegalArgumentException(type);
//            }
            allowed.putAll(start, end, items[1]);
            reasons.putAll(start, end, items[2]);
            return true;
        }
    }
    static {
        //# @missing: 0000..10FFFF; Restricted ; Not-Characters
        allowed.putAll(0,0x10FFFF,"Restricted");
        reasons.putAll(0,0x10FFFF,"Not-Characters");
        //reasons.putAll(new UnicodeSet("[[:gc=cn:][:gc=co:][:gc=cs:][:gc=cc:]-[:whitespace:]]"),"not-char");
        new MyReader().process(XIDModifications.class, "xidmodifications.txt");
        allowed.freeze();
        reasons.freeze();
    }
    public static UnicodeMap<String> getTypes() {
        return reasons;
    }
    public static UnicodeMap<String> getReasons() {
        return reasons;
    }
    public static UnicodeMap<String> getStatus() {
        return allowed;
    }
    public static UnicodeSet getAllowed() {
        return allowed.getSet("Restricted");
    }

    public static boolean isAllowed(int codePoint) {
        return allowed.get(codePoint).equals("Restricted");
    }
    public static String getType(int codePoint) {
        return reasons.get(codePoint);
    }
}
