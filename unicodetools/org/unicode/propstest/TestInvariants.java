package org.unicode.propstest;

import org.unicode.cldr.unittest.TestFmwkPlus;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Idn_Status_Values;
import org.unicode.text.utility.Settings;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;

public class TestInvariants extends TestFmwkPlus{
    public static void main(String[] args) {
        new TestInvariants().run(args);
    }

    private static final IndexUnicodeProperties iup = IndexUnicodeProperties.make(Settings.latestVersion);
    private static final IndexUnicodeProperties iupLast = IndexUnicodeProperties.make(Settings.lastVersion);

    public void TestIdna() {
        UnicodeMap<String> currentStatus = iup.load(UcdProperty.Idn_Status);
        UnicodeMap<String> oldStatus = iupLast.load(UcdProperty.Idn_Status);
        for (Idn_Status_Values status : Idn_Status_Values.values()) {
            if (status == Idn_Status_Values.disallowed) {
                continue;
            }
            UnicodeSet oldSet = oldStatus.getSet(status.toString());
            UnicodeSet newSet = currentStatus.getSet(status.toString());
            if (!newSet.containsAll(oldSet)) {
                UnicodeSet missing = new UnicodeSet(oldSet).removeAll(newSet);
                errln("Compat problem, «" + status + "» needs to contain " + missing.toPattern(false));
            }
            //assertRelation(status.toString(), true, newSet, CONTAINS_US, oldSet);
        }
    }
}
