package org.unicode.propstest;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;
import java.util.Arrays;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.props.UnicodeProperty;
import org.unicode.props.VersionToAge;
import org.unicode.text.utility.Utility;

/**
 * Simple tool for checking the stability of a property+value A property value is stable in a
 * version when the only change is when a new character is created. In this listing, those are
 * called "new". The unstable changes are when an existing character changes value: here listed as
 * either "Removed" or "Changed".
 *
 * <p>The first argument is the property name, and the second is the property value. Aliases work
 * for both. The default value if not supplied is "Yes".
 *
 * <p>For example, General_Category = Letter_Number (as of V14) was last unstable in V5.0 2006, with
 * the following: <br>
 * REMOVED: 2183 V3_0 ROMAN NUMERAL REVERSED ONE HUNDRED <br>
 * CHANGED: 10341 V3_1 GOTHIC LETTER NINETY
 */
public class CheckPropertyStability {

    static IndexUnicodeProperties latest = IndexUnicodeProperties.make();
    static UnicodeMap<Age_Values> birth = latest.loadEnum(UcdProperty.Age, Age_Values.class);
    static UnicodeSet nonchar = latest.loadBinary(UcdProperty.Noncharacter_Code_Point);

    static UnicodeMap<General_Category_Values> latestGc =
            latest.loadEnum(UcdProperty.General_Category, General_Category_Values.class);
    static UnicodeSet surrogate = latestGc.getSet(General_Category_Values.Surrogate);
    static UnicodeSet privateUse = latestGc.getSet(General_Category_Values.Private_Use);
    static UnicodeSet control = latestGc.getSet(General_Category_Values.Control);
    static UnicodeSet unassigned = latestGc.getSet(General_Category_Values.Unassigned);

    public static void main(String[] args) {

        // get the arguments

        UcdProperty prop =
                args.length < 1
                        ? UcdProperty.Default_Ignorable_Code_Point
                        : UcdProperty.forString(args[0]);
        if (prop == null) {
            System.out.println("Not supported property: " + args[0]);
            System.out.println("Use one of:\n" + Arrays.asList(UcdProperty.values()));
            return;
        }
        UnicodeProperty uprop = latest.getProperty(prop);

        String propValueRaw = args.length < 2 ? latest.getDefaultValue(prop) : args[1];

        // Check that the value occurs.

        UnicodeSet uset = uprop.getSet(propValueRaw, null);
        if (uset.isEmpty()) {
            System.out.println(
                    "No current code points have the value " + prop + " = " + propValueRaw);
            System.out.println("Use one of:\n" + uprop.getAvailableValues());
            return;
        }

        // get the normalized form of the property

        String propValue = uprop.getValue(uset.charAt(0));

        System.out.println("History of: " + prop + " = " + propValue);

        UnicodeSet lastDi = null;
        UnicodeSet lastUndefined = null;
        for (Age_Values ucdVersion : Age_Values.values()) {
            if (ucdVersion == Age_Values.Unassigned) {
                continue;
            }
            IndexUnicodeProperties iup = IndexUnicodeProperties.make(ucdVersion);
            UnicodeSet di = iup.load(prop).getSet(propValue);

            final UnicodeMap<General_Category_Values> gc =
                    iup.loadEnum(UcdProperty.General_Category, General_Category_Values.class);
            UnicodeSet undefined = gc.getSet(General_Category_Values.Unassigned);

            if (lastDi != null) {
                if (!di.equals(lastDi)) {
                    UnicodeSet removedDi = new UnicodeSet(lastDi).removeAll(di).freeze();
                    UnicodeSet newDi =
                            new UnicodeSet(di).removeAll(lastDi).retainAll(lastUndefined).freeze();
                    UnicodeSet changedDi =
                            new UnicodeSet(di).removeAll(lastDi).removeAll(lastUndefined).freeze();

                    System.out.println(
                            ucdVersion
                                    + "\t"
                                    + VersionToAge.ucd.getYear(ucdVersion)
                                    + "\tnew:\t"
                                    + newDi.size()
                                    + "\tREMOVED:\t"
                                    + removedDi.size()
                                    + "\tCHANGED:\t"
                                    + changedDi.size());
                    show("new:", newDi);
                    show("REMOVED:", removedDi);
                    show("CHANGED:", changedDi);
                }
            }
            lastDi = di;
            lastUndefined = undefined;
        }
    }

    private static void show(String string, UnicodeSet newDi) {
        // abbreviate these classes of items.
        newDi = removeCommon(string, newDi, "surrogates", surrogate);
        newDi = removeCommon(string, newDi, "private-use", privateUse);
        newDi = removeCommon(string, newDi, "nonchar-cp", nonchar);
        newDi = removeCommon(string, newDi, "control", control);
        newDi = removeCommon(string, newDi, "unassigned", unassigned);
        for (String s : newDi) {
            System.out.println(
                    "\t\t"
                            + string
                            + "\t"
                            + Utility.hex(s)
                            + "\t"
                            + birth.get(s)
                            + "\t"
                            + latest.getName(s, "+"));
        }
    }

    private static UnicodeSet removeCommon(
            String string, UnicodeSet newDi, String nameOfSet, UnicodeSet unicodeSet) {
        if (newDi.containsSome(unicodeSet)) {
            UnicodeSet in = new UnicodeSet(newDi).retainAll(unicodeSet);
            newDi = new UnicodeSet(newDi).removeAll(unicodeSet);
            System.out.println("\t\t" + string + "\t" + in.size() + "\t" + nameOfSet + "\t" + in);
        }
        return newDi;
    }
}
