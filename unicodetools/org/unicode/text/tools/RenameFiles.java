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
import org.unicode.tools.emoji.Emoji;

import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Output;

public class RenameFiles {

    // Set PREVIEW to true.
    private static final boolean PREVIEW_ONLY = false;
    private static final Choice choice = Choice.samsung;
    
    private static final boolean RECURSIVE = true;

    // Modify the dir, regex, filter, and output-platform as needed
    // Run, and verify prospective outcome
    // Set PREVIEW to false, and run for real

    enum Choice {
        fix_emoji_u("/Users/markdavis/Documents/workspace/unicodetools/data/images/svg", 
                "emoji_u.*",
                "emoji_u(?<hex>.*)\\.svg", 
                "emoji"), 
        flags(
                "/Users/markdavis/Downloads/svg-flags", 
                null,
                "(?<name>[A-Z]+)([-](?<codes>[A-Z]+))?\\.svg", 
                "emoji"),
        emojipedia(
                "/Users/markdavis/Downloads/Emojipedia 11.0 Sample Images 72px", 
                null,
                "x?(?<codes>[-_A-Za-z0-9]+)?\\.png", 
                "emojipedia"),
        samsung(
                "/Users/markdavis/Downloads/Samsung_Emoji_72x72_0322", 
                "^.*[^s].png$",
                "samsung_(?<codes>[-_A-Za-z0-9]+)\\.png", 
                "samsung"),
        ;
        final String sourceDir;
        final Matcher filter;
        final Matcher fileMatch;
        final String outputPlatformPrefix;

        Choice(String sourceDir, String filter, String fileMatch, String outputPlatformPrefix) {
            this.sourceDir = sourceDir;
            this.filter = filter == null ? null : Pattern.compile(filter).matcher("");
            this.fileMatch = fileMatch == null ? null : Pattern.compile(fileMatch).matcher("");
            this.outputPlatformPrefix = outputPlatformPrefix;
        }
    }



    // FileMatch
    //"(?<name>[A-Z]+)([-](?<codes>[A-Z]+))?\\.svg"
    //"(?<name>[a-zA-Z]+)[-_](?<codes>[-0-9a-fA-F_]+)\\.png"
    // "([-0-9a-fA-F_]+)\\.png" // twitter
    // "(?:[a-zA-Z]+|emoji_thumbnail)?(?:_[xu])?([-0-9a-fA-F_]+)\\.png" // anything else
    //"proposed_(?:x)?(.*)\\.png";
    // U+270C,U+1F3FC_256.png


    private static final int HEX_ADDITION = 
            // 0x10000 - 0x100000;
            0;

    // sourceDir
    //"/Users/markdavis/Downloads/svg-flags"
    //"/Users/markdavis/Documents/workspace/unicode-draft/reports/tr51/images/proposed"
    // Settings.BASE_DIRECTORY + "Google Drive/workspace/DATA/emoji/twitter/"
    // Settings.UNICODE_DRAFT_DIRECTORY + "/reports/tr51/images/" + OUTPUT_PLATFORM_PREFIX


    private static final Pattern REMOVE_FROM_HEX = Pattern.compile("_fe0f");

    public static void main(String[] args) throws IOException {
        final Matcher m = choice.fileMatch; 
        final File dir = new File(choice.sourceDir);
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
            if (choice.filter != null && !choice.filter.reset(name).matches()) {
                return;
            }
            if (!m.reset(name).matches()) {
                throw new IllegalArgumentException(RegexUtilities.showMismatch(m, name) + "\nHex: " + Utility.hex(name, " "));
            }
            String suffix = ".png";
            String oldHex;
            switch(choice) {
            case flags: {
                final String country = m.group("name");
                final String subdivision = m.group("codes");
                if (subdivision == null) {
                    oldHex = Emoji.getHexFromFlagCode(country);
                } else {
                    oldHex = Emoji.getHexFromSubdivision(country+subdivision);
                }
                suffix = ".svg";
            } 
            case fix_emoji_u: {
                oldHex = Utility.fromHex(m.group("hex").replaceAll("[_]", " "));
                suffix = ".svg";
                break;
            }
            default: {
                final String oldName = m.group("codes").replaceAll("[-_,]", " ").trim();
                oldHex = Utility.fromHex(oldName, false, 2);
                // HACK for J.
//                final String oldPrefix = m.group("name");
//                if (oldPrefix != null) {
//                    // curlyhair | 1f3fb-200d-2640-fe0f = curly + zwj + woman
//                    oldHex = oldHex.replace("\ufe0f", "").replace("\u200d", "");
//                    // => 1f3fb-200d-2640
//                    int first = oldHex.endsWith("\u2640") ? 0x1F469 : oldHex.endsWith("\u2642") ? 0x1F468 : -1; 
//                    oldHex = oldHex.substring(0, oldHex.length()-1);
//                    // => 1f3fb-200d
//                    int last;
//                    switch(oldPrefix) {
//                    case "curlyhair": last = 0x1F9B1; break;
//                    case "nohair": last = 0x1F9B2; break;
//                    case "redhair": last = 0x1F9B0; break;
//                    case "whitehair": last = 0x1F9B3; break;
//                    default: throw new IllegalArgumentException("bad hair day");
//                    }
//                    // 1F469 1F3FB 200D 1F9B1 = woman, light skin, zwj curly
//                    oldHex = UTF16.valueOf(first) + oldHex + UTF16.valueOf(0x200d) + UTF16.valueOf(last);
//                }
                if (HEX_ADDITION != 0) {
                    oldHex = UTF16.valueOf(HEX_ADDITION + oldHex.codePointAt(0));
                }
            }
            }
            String newHex = Utility.hex(oldHex, "_").toLowerCase(Locale.ENGLISH);
            newHex = REMOVE_FROM_HEX.matcher(newHex).replaceAll("");

            //Emoji.buildFileName(Emoji.getHexFromFlagCode(m.group(1)), "_")

            final String prefix = choice.outputPlatformPrefix == null ? m.group(1) : choice.outputPlatformPrefix;
            String newName = prefix + "_" + newHex + suffix;
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
