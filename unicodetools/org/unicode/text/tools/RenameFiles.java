package org.unicode.text.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.RegexUtilities;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Output;

public class RenameFiles {

    // Set PREVIEW to true.
    // Modify the dir, regex, filter, and output-platform as needed
    // Run, and verify prospective outcome
    // Set PREVIEW to false, and run for real

    private static final boolean PREVIEW_ONLY = false;
    private static final boolean RECURSIVE = true;

    private static final String DIR_OF_FILES_TO_CHANGE = 
            Settings.OTHER_WORKSPACE_DIRECTORY + "DATA/emoji/twitter"
            //Settings.UNICODE_DRAFT_DIRECTORY + "/reports/tr51/images/android"
            //"/Users/markdavis/Downloads/PNG 2"
            //Settings.UNICODE_DRAFT_DIRECTORY + "/reports/tr51/images/"
            //Settings.UNICODE_DRAFT_DIRECTORY + "/reports/tr51/images/proposed"
            //Settings.UNICODE_DRAFT_DIRECTORY + "/reports/tr51/images/samsung"
            // Settings.OTHER_WORKSPACE_DIRECTORY + "DATA/emoji/staging"
            ;
    private static final String FILE_MATCH = 
            "(?:([a-zA-Z]+)_[xu])?(.*)\\.png"
            //"proposed_(?:x)?(.*)\\.png";
            // U+270C,U+1F3FC_256.png
            ;

    private static final String OUTPUT_PLATFORM_PREFIX = 
            "twitter"
            //null // null means use old prefix
            // "ref";
            ;

    private static final Pattern REMOVE_FROM_HEX = Pattern.compile("_fe0f");

    private static final UnicodeSet FILTER = 
            null
            // Emoji.BETA.loadEnum(UcdProperty.Age, UcdPropertyValues.Age_Values.class).getSet(Age_Values.V9_0);
            ;


    public static void main(String[] args) throws IOException {
        final Matcher m = Pattern.compile(FILE_MATCH).matcher(""); 
        final File dir = new File(DIR_OF_FILES_TO_CHANGE);
        if (!dir.exists()) {
            throw new IllegalArgumentException("Missing dir: " + dir);
        }

        Output<Integer> count = new Output<>();
        count.value = 0;
        for (File f : dir.listFiles()) {
            process(f, m, count);
        }
    }

    private static void process(File f, Matcher m, Output<Integer> count) {
        if (f.isDirectory()) {
            if (RECURSIVE) {
                for (File f2 : f.listFiles()) {
                    process(f2, m, count);
                }
            }
            return;
        }
        String name = f.getName();
        String path = f.getPath();
        String parent = f.getParent();
        if (name.startsWith(".") 
                || name.endsWith(" (1).png") 
                || name.endsWith(" 2.png") 
                || name.contains("_x")
                || name.endsWith(".gif")
                || name.endsWith(".jpg")
                || parent.endsWith("/other")
                ) {
            return;
        }
        try {
            if (!m.reset(name).matches()) {
                throw new IllegalArgumentException(RegexUtilities.showMismatch(m, name));
            }
            final String oldName = m.group(2).replaceAll("[-_,]", " ");
            String oldHex = Utility.fromHex(oldName, false, 2);
            if (FILTER != null && !FILTER.containsAll(oldHex)) {
                return;
            }
            String newHex = Utility.hex(oldHex, "_").toLowerCase(Locale.ENGLISH);
            newHex = REMOVE_FROM_HEX.matcher(newHex).replaceAll("");

            //Emoji.buildFileName(Emoji.getHexFromFlagCode(m.group(1)), "_")

            final String prefix = OUTPUT_PLATFORM_PREFIX == null ? m.group(1) : OUTPUT_PLATFORM_PREFIX;
            String newName = prefix + "_" + newHex + ".png";
            if (newName.equals(name)) {
                return;
            }
            count.value++;
            System.out.println(count.value + "\t" + f + "\t=> " + newName);
            if (PREVIEW_ONLY) {
                return;
            }
            FileSystem dfs = FileSystems.getDefault();
            Path oldPath = dfs.getPath(path);            
            Path foo = Files.move(oldPath, oldPath.resolveSibling(newName), StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            throw new IllegalArgumentException(parent,e);
        }
    }
}
