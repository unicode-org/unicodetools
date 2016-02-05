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
    private static final String PLATFORM = "emojixpress";
    private static final String GENERATED_DIR = "DATA/emoji/emojixpress";
    private static final String FILE_MATCH = ".*_x?(.*)\\.png";
    private static final boolean PREVIEW = false;

    public static void main(String[] args) throws IOException {
        Matcher m = Pattern.compile(FILE_MATCH).matcher(""); 
        //File dir = new File(Settings.OTHER_WORKSPACE_DIRECTORY + "DATA/country-flags");
        File dir = new File(Settings.OTHER_WORKSPACE_DIRECTORY + GENERATED_DIR);

        FileSystem dfs = FileSystems.getDefault();
        int count = 0;
        for (File f : dir.listFiles()) {
            String name = f.getName();
            String path = f.getPath();
            if (name.startsWith(".")) continue;
            if (!m.reset(name).matches()) {
                throw new IllegalArgumentException(name);
            }
            final String oldName = m.group(1).replace('-',' ');
            String oldHex = Utility.fromHex(oldName, false, 2);
            String newHex = Utility.hex(oldHex, "_").toLowerCase(Locale.ENGLISH);
            
            //Emoji.buildFileName(Emoji.getHexFromFlagCode(m.group(1)), "_")
            String newName = PLATFORM + "_" + newHex + ".png";
            System.out.println((count++) + "\t" + f + "\t=> " + newName);
            if (PREVIEW) {
                continue;
            }
            Path oldPath = dfs.getPath(path);            
            Path foo = Files.move(oldPath, oldPath.resolveSibling(newName), StandardCopyOption.ATOMIC_MOVE);
        }
    }
}
