package org.unicode.text.utility;

import org.unicode.cldr.util.CldrUtility;

public class Settings {
    public static final boolean SKIP_COPYRIGHT = "skip".equalsIgnoreCase(org.unicode.cldr.util.CldrUtility.getProperty("copyright", "skip"));

    public static final String DATA_DIR = Utility.fixFileName(org.unicode.cldr.util.CldrUtility.getProperty("UCD_DIR", "data/")) + "/";
    public static final String UCD_DIR = DATA_DIR + "ucd/";
    public static final String IDN_DIR = DATA_DIR + "IDN/";
    public static final String DICT_DIR = DATA_DIR + "dict/";
    
    public static final String CHARTS_GEN_DIR = DATA_DIR + "charts/";

    public static final String GEN_DIR = Utility.fixFileName(org.unicode.cldr.util.CldrUtility.getProperty("GEN_DIR", "../Generated")) + "/";
    public static final String BIN_DIR = GEN_DIR + "BIN/";
    public static final String GEN_UCD_DIR = GEN_DIR + "ucd/";
    
    public static final String WORKSPACE_DIRECTORY = "/Users/markdavis/Google Drive/Backup-2012-10-09/Documents/indigo/";
    public static final String UNICODETOOLS_DIRECTORY = "/Users/markdavis/workspace/unicodetools/";
    public static final String UNICODE_DRAFT_DIRECTORY = "/Users/markdavis/workspace/unicode-draft/";

    public static final String SRC_DIR = Utility.fixFileName("org/unicode/text") + "/";
    public static final String SRC_UCA_DIR = SRC_DIR + "UCA/";
    public static final String SRC_UCD_DIR = SRC_DIR + "UCD/";

}
