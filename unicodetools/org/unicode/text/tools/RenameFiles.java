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

import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Output;

public class RenameFiles {

    // Set PREVIEW to true.
    // Modify the dir, regex, filter, and output-platform as needed
    // Run, and verify prospective outcome
    // Set PREVIEW to false, and run for real

    private static final boolean PREVIEW_ONLY = false;
    private static final boolean RECURSIVE = true;
    
    private static final String OUTPUT_PLATFORM_PREFIX = 
            "proposed" // null means use old prefix
            ;

    private static final String FILE_MATCH = 
            "proposed_([0-9a-fA-F]+)\\.png"
            // "([-0-9a-fA-F_]+)\\.png" // twitter
            // "(?:[a-zA-Z]+|emoji_thumbnail)?(?:_[xu])?([-0-9a-fA-F_]+)\\.png" // anything else
            //"proposed_(?:x)?(.*)\\.png";
            // U+270C,U+1F3FC_256.png
            ;

    private static final int HEX_ADDITION = 
            0x10000 - 0x100000;
            //  0;

    private static final String DIR_OF_FILES_TO_CHANGE = 
            "/Users/markdavis/Documents/workspace/unicode-draft/reports/tr51/images/proposed"
            // Settings.BASE_DIRECTORY + "Google Drive/workspace/DATA/emoji/twitter/"
            // Settings.UNICODE_DRAFT_DIRECTORY + "/reports/tr51/images/" + OUTPUT_PLATFORM_PREFIX
            ;
    
    private static final Pattern REMOVE_FROM_HEX = Pattern.compile("_fe0f");

    private static final UnicodeSet FILTER = 
            new UnicodeSet(0x10F000,0x10FFFF);
            // Emoji.BETA.loadEnum(UcdProperty.Age, UcdPropertyValues.Age_Values.class).getSet(Age_Values.V9_0);


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
            final String oldName = m.group(1).replaceAll("[-_,]", " ");
            String oldHex = Utility.fromHex(oldName, false, 2);
            if (FILTER != null && !FILTER.containsAll(oldHex)) {
                return;
            }
            if (HEX_ADDITION != 0) {
                oldHex = UTF16.valueOf(HEX_ADDITION + oldHex.codePointAt(0));
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
