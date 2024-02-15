package org.unicode.jsp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.text.utility.Settings;

public class RebuildPropertyCache {

    public static void main(String[] args) throws IOException {
        final var binDir = new File(Settings.Output.BIN_DIR);
        if (binDir.exists()) {
            final Queue<File> directories = new ArrayDeque<>();
            final List<File> directoriesToDelete = new ArrayList<>();
            final List<File> filesToDelete = new ArrayList<>();
            directories.add(binDir);
            while (!directories.isEmpty()) {
                final File directory = directories.poll();
                for (final var child : directory.listFiles()) {
                    if (child.isDirectory()) {
                        directories.add(child);
                        directoriesToDelete.add(child);
                    } else {
                        filesToDelete.add(child);
                    }
                }
            }
            System.out.println(
                    "Cleaning "
                            + filesToDelete.size()
                            + " existing files in "
                            + directoriesToDelete.size()
                            + " existiing directories under "
                            + Settings.Output.BIN_DIR);
            for (final var f : filesToDelete) {
                if (!f.delete()) {
                    System.err.println("Failed to delete " + f);
                }
            }
            for (final var f : directoriesToDelete) {
                if (!f.delete()) {
                    System.err.println("Failed to delete " + f);
                }
            }
        }

        IndexUnicodeProperties.loadUcdHistory(null, null);
        System.out.println("Rebuilt property cache in " + Settings.Output.BIN_DIR);
    }
}
