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

import com.ibm.icu.impl.UnicodeRegex;

public class RenameFiles {
    
    // Set PREVIEW to true.
    // Modify the dir, regex, and output-platform as needed
    // Run, and verify prospective outcome
    // Set PREVIEW to false, and run for real
    
    private static final boolean PREVIEW_ONLY = false;
    
    private static final String DIR_OF_FILES_TO_CHANGE = 
            //Settings.UNICODE_DRAFT_DIRECTORY + "/reports/tr51/images/samsung"
            Settings.OTHER_WORKSPACE_DIRECTORY + "DATA/emoji/staging"
            ;
    private static final String FILE_MATCH = "(.*)_256\\.png";
    // U+270C,U+1F3FC_256.png

    private static final String OUTPUT_PLATFORM_PREFIX = "facebook";

    public static void main(String[] args) throws IOException {
        Matcher m = Pattern.compile(FILE_MATCH).matcher(""); 
        File dir = new File(DIR_OF_FILES_TO_CHANGE);
        if (!dir.exists()) {
            throw new IllegalArgumentException("Missing dir: " + dir);
        }

        FileSystem dfs = FileSystems.getDefault();
        int count = 0;
        for (File f : dir.listFiles()) {
            String name = f.getName();
            String path = f.getPath();
            if (name.startsWith(".") || name.endsWith(" (1).png") || name.endsWith(" 2.png")) continue;
            if (!m.reset(name).matches()) {
                throw new IllegalArgumentException(RegexUtilities.showMismatch(m, name));
            }
            final String oldName = m.group(1).replaceAll("[-_,]", " ");
            String oldHex = Utility.fromHex(oldName, false, 2);
            String newHex = Utility.hex(oldHex, "_").toLowerCase(Locale.ENGLISH);
            
            //Emoji.buildFileName(Emoji.getHexFromFlagCode(m.group(1)), "_")
            String newName = OUTPUT_PLATFORM_PREFIX + "_" + newHex + ".png";
            System.out.println((count++) + "\t" + f + "\t=> " + newName);
            if (PREVIEW_ONLY) {
                continue;
            }
            Path oldPath = dfs.getPath(path);            
            Path foo = Files.move(oldPath, oldPath.resolveSibling(newName), StandardCopyOption.ATOMIC_MOVE);
        }
    }
}
