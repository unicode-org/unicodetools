package org.unicode.jsptest;

import com.ibm.icu.text.UnicodeSet;

import org.unicode.jsp.UnicodeUtilities;
import org.unicode.unittest.TestFmwkMinusMinus;

public class TestFmwk2 extends TestFmwkMinusMinus {

    public void checkContained(final String setPattern, final String containedPattern) {
        checkContained(setPattern, containedPattern, true);
    }

    public void checkContained(final String setPattern, final String containedPattern, boolean expected) {
        String[] message = {""};
        UnicodeSet container = UnicodeUtilities.parseSimpleSet(setPattern, message);
        UnicodeSet contained = UnicodeUtilities.parseSimpleSet(containedPattern, message);
        if (container == null) {
            errln(setPattern + " fails to parse");
        } else if (contained == null) {
            errln(containedPattern + " fails to parse");
        } else if (container.containsAll(contained) != expected) {
                errln(toPattern(setPattern, container) + " doesn't contain " + toPattern(containedPattern, contained));
        } else {
            logln(toPattern(setPattern, container) + " contains " + toPattern(containedPattern, contained));
        }
    }

    @Override
    public void msg(String message, int level, boolean incCount, boolean newln) {
        super.msg(message.length() > 200 ? message.substring(0,200) + "…" : message, level, incCount, newln);
    }

}
