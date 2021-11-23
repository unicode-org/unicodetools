package org.unicode.text.utility;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;

import com.ibm.icu.util.VersionInfo;

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
    public static final String latestVersion = "15.0.0";
    public static final String lastVersion = "14.0.0"; // last released version

    private static final String TRIMMED_LATEST_VERSION = trimVersion(latestVersion);

    // See https://github.com/unicode-org/unicodetools/issues/144
    public static final String NEXT_VERSION_FOLDER_NAME = "dev";

    public static final boolean BUILD_FOR_COMPARE =
            CldrUtility.getProperty("BUILD_FOR_COMPARE", "false").startsWith("t");

    public static final boolean SKIP_COPYRIGHT =
            "skip".equalsIgnoreCase(CldrUtility.getProperty("copyright", "skip"));

    /**
     * Removes one or more trailing ".0" substrings, and/or one trailing "-Update".
     * For quick comparison of version strings and folder names
     * without building VersionInfo objects.
     */
    private static final String trimVersion(String version) {
        int length = trimmedVersionLength(version);
        return length == version.length() ? version : version.substring(0, length);
    }

    private static final int trimmedVersionLength(String version) {
        int length = version.length();
        if (version.endsWith("-Update")) {
            length -= "-Update".length();
        }
        while (length >= 2 && version.charAt(length - 2) == '.' && version.charAt(length - 1) == '0') {
            length -= 2;
        }
        return length;
    }

    /** Ignores trailing ".0" and "-Update". */
    private static final boolean isLatestVersion(String version) {
        int length = trimmedVersionLength(version);
        return length == TRIMMED_LATEST_VERSION.length() &&
                TRIMMED_LATEST_VERSION.regionMatches(0, version, 0, length);
    }

    private static final Path getPath(Path parentPath, String relativeDir, String version) {
        if (relativeDir != null) {
            parentPath = parentPath.resolve(relativeDir);
        }
        String versionName = version;
        if (!version.endsWith("-Update") && parentPath.equals(UnicodeTools.UCD_PATH)) {
            versionName += "-Update";
        }
        Path path = parentPath.resolve(versionName);
        // Does the folder exist?
        if (Files.exists(path)) {
            return path;
        }
        // We might be looking for files for the next version.
        // If so, then switch to the "dev" folder.
        if (!isLatestVersion(version)) {
            return null;
        }
        path = parentPath.resolve(NEXT_VERSION_FOLDER_NAME);
        if (!Files.exists(path)) {
            return null;
        }
        return path;
    }

    private static final String getPathString(Path path) {
        return path != null ? path.toString() : null;
    }

    private static final String getPathString(Path parentPath, String relativeDir, String version) {
        return getPathString(getPath(parentPath, relativeDir, version));
    }

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
        // TODO: Try to make this private; see https://github.com/unicode-org/unicodetools/issues/159
        // Call sites should use more specific paths.
        public static final String UNICODETOOLS_DIR = UNICODETOOLS_REPO_DIR + "unicodetools/";
        /**
         * Use this for files such as org/unicode/Whatever.java
         */
        public static final String UNICODETOOLS_JAVA_DIR = UNICODETOOLS_DIR + "src/main/java/";
        /**
         * Use this for package-relative data, such as org/unicode/SomeData.txt
         */
        public static final String UNICODETOOLS_RSRC_DIR = UNICODETOOLS_DIR + "src/main/resources/";
        public static final String UNICODEJSPS_DIR = UNICODETOOLS_REPO_DIR + "UnicodeJsps/";
        public static final String DATA_DIR = UNICODETOOLS_DIR + "data/";
        public static final Path DATA_PATH = Paths.get(DATA_DIR);
        public static final String UCD_DIR = DATA_DIR + "ucd/";
        private static final Path UCD_PATH = DATA_PATH.resolve("ucd");
        // TODO: IDN_DIR is used, but there is no .../data/IDN/ folder. Should this be .../data/idna/ ?
        public static final String IDN_DIR = DATA_DIR + "IDN/";
        // TODO: DICT_DIR is used, but there is no .../data/dict/ folder. ??
        public static final String DICT_DIR = DATA_DIR + "dict/";

        /**
         * Returns a path to the Unicode Tools source data plus the relativeDir and version.
         * Falls back from latestVersion to "dev" as needed.
         * Appends "-Update" to a "ucd" version as needed.
         * Returns null if the folder does not exist.
         */
        public static final Path getDataPath(String relativeDir, String version) {
            return getPath(DATA_PATH, relativeDir, version);
        }

        /**
         * Returns a path string to the Unicode Tools source data plus the relativeDir and version.
         * Falls back from latestVersion to "dev" as needed.
         * Appends "-Update" to a "ucd" version as needed.
         * Returns null if the folder does not exist.
         */
        public static final String getDataPathString(String relativeDir, String version) {
            return getPathString(DATA_PATH, relativeDir, version);
        }

        /**
         * Returns a path to the Unicode Tools source data
         * plus the relativeDir and latest version.
         * Falls back from latestVersion to "dev" as needed.
         * Appends "-Update" to a "ucd" version as needed.
         * Returns null if the folder does not exist.
         */
        public static final Path getDataPathForLatestVersion(String relativeDir) {
            return getPath(DATA_PATH, relativeDir, latestVersion);
        }

        /**
         * Returns a path string to the Unicode Tools source data
         * plus the relativeDir and latest version.
         * Falls back from latestVersion to "dev" as needed.
         * Appends "-Update" to a "ucd" version as needed.
         * Returns null if the folder does not exist.
         */
        public static final String getDataPathStringForLatestVersion(String relativeDir) {
            return getPathString(DATA_PATH, relativeDir, latestVersion);
        }

        /**
         * Constants representing data subdirectories
         * TODO: unused; delete?
         */
        public enum DataDir {
            SECURITY,
            UCD,
            IDNA,
            EMOJI;

            /**
             * This dir as a Path
             * @return
             */
            public Path asPath() {
                return  DATA_PATH.resolve(name().toLowerCase(Locale.ROOT));
            }

            /**
             * This dir as a Path to the version subdir
             * @param forVersion
             * @return
             */
            public Path asPath(VersionInfo forVersion) {
                String versionString = versionToString(forVersion);
                return getPath(asPath(), null, versionString);
            }

            /**
             * Map a version number to a string.
             * @param version
             * @return
             */
            public String versionToString(VersionInfo version) {
                StringBuilder sb = new StringBuilder();
                sb.append(version.getMajor())
                  .append(".")
                  .append(version.getMinor());
                if (this != EMOJI) {
                    // 13.1, 14.0
                    sb.append(".")
                      .append(version.getMilli());
                } // else: 14.0.0
                return sb.toString();
            }
        }
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
        /**
         * Make sure the output dirs exist
         */
        public static void ensureOutputDirs() throws FileNotFoundException {
                if (!(new File(UNICODETOOLS_OUTPUT_DIR)).isDirectory()) {
                        throw new FileNotFoundException("Not a directory: UNICODETOOLS_OUTPUT_DIR=" + UNICODETOOLS_OUTPUT_DIR);
                }
                ensureOutputDir(GEN_DIR);
                ensureOutputDir(GEN_UCD_DIR);
                ensureOutputDir(GEN_UCA_DIR);
                ensureOutputDir(BIN_DIR);
        }
        public static void ensureOutputDir(String dir) {
                if(new File(dir).mkdirs()) {
                        System.err.println("# mkdir " + dir);
                }
        }
    }

    public static final String SRC_DIR = Utility.fixFileName(UnicodeTools.UNICODETOOLS_RSRC_DIR+"org/unicode/text") + "/";
    /**
     * Used for data files
     */
    public static final String SRC_UCA_DIR = SRC_DIR + "UCA/";
    /**
     * Used for data files
     */
    public static final String SRC_UCD_DIR = SRC_DIR + "UCD/";
}
