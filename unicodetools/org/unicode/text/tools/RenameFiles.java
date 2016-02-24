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

import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

public class RenameFiles {
    
    // Set PREVIEW to true.
    // Modify the dir, regex, and output-platform as needed
    // Run, and verify prospective outcome
    // Set PREVIEW to false, and run for real
    
    private static final boolean PREVIEW_ONLY = false;
    
    private static final String DIR_OF_FILES_TO_CHANGE = "DATA/emoji/flags";
    private static final String FILE_MATCH = "emoji_u(.*)\\.png";

    private static final String OUTPUT_PLATFORM_PREFIX = "ref";

    public static void main(String[] args) throws IOException {
        Matcher m = Pattern.compile(FILE_MATCH).matcher(""); 
        //File dir = new File(Settings.OTHER_WORKSPACE_DIRECTORY + "DATA/country-flags");
        File dir = new File(Settings.OTHER_WORKSPACE_DIRECTORY + DIR_OF_FILES_TO_CHANGE);

        FileSystem dfs = FileSystems.getDefault();
        int count = 0;
        for (File f : dir.listFiles()) {
            String name = f.getName();
            String path = f.getPath();
            if (name.startsWith(".")) continue;
            if (!m.reset(name).matches()) {
                throw new IllegalArgumentException(name);
            }
            final String oldName = m.group(1).replaceAll("[-_]", " ");
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
