package org.unicode.text.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RenameFiles {
    private static final boolean PREVIEW = false;

    public static void main(String[] args) throws IOException {
        Matcher m = Pattern.compile("emoji_u(.*)\\.png").matcher(""); 
        //File dir = new File(Settings.OTHER_WORKSPACE_DIRECTORY + "DATA/country-flags");
        File dir = new File("/Users/markdavis/Google Drive/workspace/DATA/emoji/samsung");

        FileSystem dfs = FileSystems.getDefault();
        int count = 0;
        for (File f : dir.listFiles()) {
            String name = f.getName();
            String path = f.getPath();
            if (name.startsWith(".")) continue;
            if (!m.reset(name).matches()) {
                throw new IllegalArgumentException(name);
            }
            //Emoji.buildFileName(Emoji.getHexFromFlagCode(m.group(1)), "_")
            String newName = "samsung_" + m.group(1) + ".png";
            System.out.println((count++) + "\t" + f + "\t=> " + newName);
            if (PREVIEW) {
                continue;
            }
            Path oldPath = dfs.getPath(path);            
            Path foo = Files.move(oldPath, oldPath.resolveSibling(newName), StandardCopyOption.ATOMIC_MOVE);
        }
    }
}
