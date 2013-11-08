package org.unicode.text.UCD;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StripDAndCopy {
    
    static final Matcher fileMatcher = Pattern.compile("([^-]*)(-[\\d\\.]+d\\d+)?(\\.[a-z]+)").matcher("");
    
    public static void main(String[] args) throws IOException {
        File kenDir = new File(args[0]);
        File toolDir = new File(args[1]);
        stripAndCopy(kenDir, toolDir);
    }

    private static void stripAndCopy(File sourceDir, File targetDir) throws IOException {
        for (File file : sourceDir.listFiles()) {
            String base = file.getName();
            if (base.startsWith(".")) {
                // do nothing
            } else if (file.isDirectory()) {
                stripAndCopy(file, new File(targetDir.toString() + '/' + base));
            } else if (fileMatcher.reset(base).matches()) {
                String newName = fileMatcher.group(1) + fileMatcher.group(3);
                File newFileName = new File(targetDir.getCanonicalPath() + '/' + newName);
                if (newFileName.exists()) {
                    newFileName.delete();
                    System.out.println("Deleted\t" + newFileName);
                }
                System.out.println("Renamed\t" + file + "\n\tto\t" + newFileName);
                file.renameTo(newFileName);
                // rename file
            } else {
                System.err.println("Can't copy " + file);
            }
        }    
    }
}
