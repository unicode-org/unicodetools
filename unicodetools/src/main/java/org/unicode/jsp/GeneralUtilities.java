package org.unicode.jsp;

import java.util.Locale;

public class GeneralUtilities {

    /**
     * Used to set a static debug flag from an environment variable. Allows static final flags to be set for debugging information even in environments
     * where the source cannot be altered. For a given class StringPrepData, the debug flag is -Dstringprepdata_<flagname> (that is, all lowercase).
     * <p> Example:
     * <pre>
     * private static final boolean DEBUG_SHOW_DETAILS = GeneralUtilities.getDebugFlag(StringPrepData.class, "show_details");
     * </pre>
     * @param class1 Typically the class where the boolean is defined.
     * @param flagName a specialized name, such as show_details.
     * @return whether flag was present.
     */
    public static boolean getDebugFlag(Class<?> class1, String flagName) {
        String className = class1.getName();
        final int lastPart = className.lastIndexOf('.');
        if (lastPart >= 0) {
            className = className.substring(lastPart+1);
        }
        return System.getProperty((className+"_" + flagName).toLowerCase(Locale.ROOT)) != null;
    }

    /**
     * Used to set a static debug flag from an environment variable. Allows static final flags to be set for debugging information even in environments
     * where the source cannot be altered. For a given class StringPrepData, the debug flag is -Dstringprepdata_<flagname> (that is, all lowercase).
     * <p> Example:
     * <pre>
     * private static final boolean DEBUG_SHOW_DETAILS = GeneralUtilities.getDebugFlag(StringPrepData.class, "show_details", DEBUG);
     * </pre>
     * @param class1 Typically the class where the boolean is defined.
     * @param flagName a specialized name, such as show_details.
     * @param onlyif allows the test to be subject to a general flag.
     * @return whether flag was present.
     */
    public static boolean getDebugFlag(Class<?> class1, String flagName, boolean onlyif) {
        return onlyif && getDebugFlag(class1, flagName);
    }

    /**
     * Convenience method, where the flagname is "debug".
     * @param class1 Typically the class where the boolean is defined.
     * @return whether flag was present.
     */
    public static boolean getDebugFlag(Class<?> class1) {
        return getDebugFlag(class1, "debug");
    }

}
