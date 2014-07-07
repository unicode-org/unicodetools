package org.unicode.text.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RenameFiles {
    public static void main(String[] args) throws IOException {
        Matcher m = Pattern.compile("([A-Z]{2})\\.png").matcher(""); 
        //File dir = new File("/Users/markdavis/Google Drive/Backup-2012-10-09/Documents/indigo/DATA/country-flags");
        File dir = new File("/Users/markdavis/Google Drive/Backup-2012-10-09/Documents/indigo/DATA/AppleEmoji");
       
        FileSystem dfs = FileSystems.getDefault();

        for (File f : dir.listFiles()) {
            String name = f.getName();
            String path = f.getPath();
            if (!name.endsWith("'")) {
                continue;
            }
            String newName = name.substring(0, name.length()-1);
//            if (name.startsWith(".")) continue;
//            if (!m.reset(name).matches()) {
//                throw new IllegalArgumentException(name);
//            }
//            String newName = "ref_" + Emoji.buildFileName(Emoji.getHexFromFlagCode(m.group(1)), "_") + ".png";
            
            Path oldPath = dfs.getPath(path);            
            Path foo = Files.move(oldPath, oldPath.resolveSibling(newName), StandardCopyOption.ATOMIC_MOVE);
            
//            a.move(source, target, options)
//            if (!f.renameTo(newFile)) {
//                throw new IllegalArgumentException(newFile.toString());
//            }
            //System.out.println(f + "\t=> " + newFile);
        }
    }
}
