package org.unicode.text.utility;

import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;

public class Settings {

    // TODO Many of these settings are crufty and need revision.
    // https://unicode-org.atlassian.net/browse/CLDR-14335 "Rationalize CLDR constants"
    // is for fixing these and CLDR.

    // TODO: Why do we sometimes use CldrUtility.getPath() which normalizes paths via java.nio.file.Paths
    // and sometimes Utility.fixFileName() which normalizes paths via java.io.File?
    // Are they equivalent for our purposes?

    /**
     * Used for the default version.
     */
    public static final String latestVersion = "14.0.0";
    public static final String lastVersion = "13.0.0"; // last released version

    public static final boolean BUILD_FOR_COMPARE =
            CldrUtility.getProperty("BUILD_FOR_COMPARE", "false").startsWith("t");

    public static final boolean SKIP_COPYRIGHT =
            "skip".equalsIgnoreCase(CldrUtility.getProperty("copyright", "skip"));

    private static final String getRequiredPathAndFix(String key) {
        String path = CldrUtility.getProperty(key);
        if (path == null) {
            throw new IllegalArgumentException("Specify the " + key + " environment variable");
        }
        return Utility.fixFileName(path) + "/";
    }

    // Using nested classes for grouping paths,
    // and to throw an exception when a required path is not specified.
    // We want a minimal set of environment variables:
    // One per repo (because we don't want to require
    // a particular layout of multiple repos relative to each other)
    // and one for where to write output files.

    // TODO: Is it possible to tell Eclipse that we want to import only Settings,
    // not Settings.Output etc.?
    // (Writing Settings.Output is much more understandable at the "call" sites.)
    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=305205
    // https://stackoverflow.com/questions/5766489/how-can-i-prevent-eclipse-from-importing-nested-classes

    public static final class CLDR {
        private static final String CLDR_REPO_DIR = CLDRPaths.BASE_DIRECTORY;
        public static final String SVN_DIRECTORY = CldrUtility.getPath(
                CldrUtility.getProperty("SVN_DIR", CLDR_REPO_DIR + "/../"));
        public static final String AUX_DIRECTORY = CldrUtility.getPath(
                CldrUtility.getProperty("CLDR_TMP_DIR",
                        CldrUtility.getPath(SVN_DIRECTORY, "cldr-aux/")));
        public static final String UCD_DATA_DIRECTORY = CldrUtility.getPath(
                SVN_DIRECTORY + "unicodetools/unicodetools/data/");
        public static final String BASE_DIRECTORY = Utility.fixFileName(
                CldrUtility.getProperty("BASE_DIRECTORY", SVN_DIRECTORY + "../")) + "/";
    }

    public static final class UnicodeTools {
        /**
         * The root of the unicodetools repo.
         * Contains the UnicodeJsps and unicodetools folders etc.
         */
        public static final String UNICODETOOLS_REPO_DIR =
                getRequiredPathAndFix("UNICODETOOLS_REPO_DIR");
        public static final String UNICODETOOLS_DIR = UNICODETOOLS_REPO_DIR + "unicodetools/";
        public static final String UNICODEJSPS_DIR = UNICODETOOLS_REPO_DIR + "UnicodeJsps/";
        public static final String DATA_DIR = UNICODETOOLS_DIR + "data/";
        public static final String UCD_DIR = DATA_DIR + "ucd/";
        // TODO: IDN_DIR is used, but there is no .../data/IDN/ folder. Should this be .../data/idna/ ?
        public static final String IDN_DIR = DATA_DIR + "IDN/";
        // TODO: DICT_DIR is used, but there is no .../data/dict/ folder. ??
        public static final String DICT_DIR = DATA_DIR + "dict/";
    }

    public static final class Images {
        /** The root of the images repo. */
        public static final String IMAGES_REPO_DIR = getRequiredPathAndFix("IMAGES_REPO_DIR");
    }

    public static final class Output {
        /** The root of where we write output files. Most go into a "Generated" sub-folder. */
        public static final String UNICODETOOLS_OUTPUT_DIR =
                getRequiredPathAndFix("UNICODETOOLS_OUTPUT_DIR");
        public static final String GEN_DIR = UNICODETOOLS_OUTPUT_DIR + "Generated/";
        public static final String BIN_DIR = GEN_DIR + "BIN/";
        public static final String GEN_UCD_DIR = GEN_DIR + "ucd/";
        public static final String GEN_UCA_DIR = GEN_DIR + "UCA/";
    }

    public static final String SRC_DIR = Utility.fixFileName("org/unicode/text") + "/";
    public static final String SRC_UCA_DIR = SRC_DIR + "UCA/";
    public static final String SRC_UCD_DIR = SRC_DIR + "UCD/";
}
