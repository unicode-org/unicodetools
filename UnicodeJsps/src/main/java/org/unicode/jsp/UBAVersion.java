package org.unicode.jsp;

import java.util.EnumSet;
import org.unicode.props.UcdPropertyValues.Age_Values;

/** A class to encapsulate the available C UBA versions. */
public class UBAVersion {
    private static EnumSet<Age_Values> C_UBA_AGES =
            EnumSet.of(
                    Age_Values.V6_2,
                    Age_Values.V6_3,
                    Age_Values.V7_0,
                    Age_Values.V8_0,
                    Age_Values.V9_0,
                    Age_Values.V10_0,
                    Age_Values.V11_0,
                    Age_Values.V12_0,
                    Age_Values.V13_0,
                    Age_Values.V14_0
                    /* Current version is always last */
                    );

    public static EnumSet<Age_Values> getVersions() {
        return C_UBA_AGES;
    }

    /**
     * As a select, such as "140"
     *
     * @return
     */
    public static final String toSelect(Age_Values age) {
        return age.getShortName().replaceAll("[\\.]", "");
    }

    /**
     * Current version
     *
     * @return
     */
    public static Age_Values getCurrent() {
        return C_UBA_AGES.toArray(new Age_Values[C_UBA_AGES.size()])[C_UBA_AGES.size() - 1];
    }
}
