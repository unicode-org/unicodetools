package org.unicode.tools;

import com.ibm.icu.util.VersionInfo;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.unicode.cldr.draft.FileUtilities;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.text.tools.GenerateSubtagNames;
import org.unicode.text.utility.Settings;

public class UpdateJspFiles {
    static CopyOption[] options =
            new CopyOption[] {
                StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES
            };
    static final Path JSP_RESOURCE_DATA =
            Paths.get(Settings.UnicodeTools.UNICODEJSPS_DIR, "src/main/resources/org/unicode/jsp/");
    static final Path UNICODE_TOOLS_DIR = Paths.get(Settings.UnicodeTools.UNICODETOOLS_DIR);
    static final Path TRIM_PARENT = Paths.get(Settings.UnicodeTools.UNICODETOOLS_REPO_DIR);

    static final String trim(Path p) {
        if (p.startsWith(TRIM_PARENT)) {
            return "{...}/" + p.subpath(TRIM_PARENT.getNameCount(), p.getNameCount()).toString();
        } else {
            return p.toString();
        }
    }

    public static void main(String args[]) throws IOException {
        generateSubtagNames(); // this takes a sec, so run it first

        IndexUnicodeProperties latest = IndexUnicodeProperties.make();
        VersionInfo ucdVersion = latest.getUcdVersion();
        System.out.println(
                "Updating all JSP files for "
                        + ucdVersion
                        + " into "
                        + TRIM_PARENT.toAbsolutePath());

        copyTextFiles(ucdVersion);

        copyOtherProps(ucdVersion);

        // Sublaunch listProps
        System.out.println("Sublaunching ListProps..");
        ListProps.main(args);

        // Sublaunch CopyPropsToUnicodeJsp
        System.out.println("Sublaunching CopyPropsToUnicodeJsp");
        CopyPropsToUnicodeJsp.main(args);

        System.out.println(
                "DONE! Now go run 'mvn org.eclipse.jetty:jetty-maven-plugin:run' to fire up the JSP");
    }

    private static void copyTextFiles(VersionInfo fromVersion) throws IOException {
        System.out.println("1. Copying text files from " + fromVersion);
        copyTextFiles(
                fromVersion,
                Settings.UnicodeTools.DataDir.SECURITY,
                "confusables.txt",
                "IdentifierStatus.txt",
                "IdentifierType.txt");
        copyTextFiles(
                fromVersion,
                Settings.UnicodeTools.DataDir.UCD,
                "NameAliases.txt",
                "NamesList.txt",
                "ScriptExtensions.txt",
                "StandardizedVariants.txt");
        copyTextFiles(fromVersion, Settings.UnicodeTools.DataDir.IDNA, "IdnaMappingTable.txt");
        copyTextFiles(
                fromVersion,
                Settings.UnicodeTools.DataDir.EMOJI,
                "emoji-sequences.txt",
                "emoji-zwj-sequences.txt");
        System.err.println("TODO: <emoji-variants>");
    }

    private static void copyTextFiles(
            VersionInfo fromVersion, Settings.UnicodeTools.DataDir dir, String... filenames)
            throws IOException {
        final Path targDir = JSP_RESOURCE_DATA;
        copyTextFiles(targDir, fromVersion, dir, filenames);
    }

    private static void copyTextFiles(
            final Path targDir,
            VersionInfo fromVersion,
            Settings.UnicodeTools.DataDir dir,
            String... filenames)
            throws IOException {
        final Path srcDir = dir.asPath(fromVersion);
        System.out.println(" Copy from " + dir.name() + " copying from " + trim(srcDir));
        for (final String file : filenames) {
            final Path srcFile = srcDir.resolve(file);
            if (!srcFile.toFile().canRead()) {
                throw new IllegalArgumentException(
                        dir.name() + "/" + file + " not readable: " + srcFile.toAbsolutePath());
            }
            copyFile(srcFile, targDir.resolve(file));
        }
    }

    private static void copyTextFiles(Path srcDir, Path targDir, String... filenames)
            throws IOException {
        System.out.println(" Copying from " + trim(srcDir) + " to " + trim(targDir));
        for (final String file : filenames) {
            final Path srcFile = srcDir.resolve(file);
            if (!srcFile.toFile().canRead()) {
                throw new IllegalArgumentException("Not readable: " + srcFile.toAbsolutePath());
            }
            copyFile(srcFile, targDir.resolve(file));
        }
    }

    private static void copyFile(final Path srcFile, final Path targFile) throws IOException {
        if (srcFile.getFileName().equals(targFile.getFileName())) {
            System.out.println(trim(targFile) + " <-- " + trim(srcFile.getParent()));
        } else {
            System.out.println(trim(targFile) + " <-- " + trim(srcFile));
        }
        Files.copy(srcFile, targFile, options);
    }

    private static void generateSubtagNames() throws IOException {
        System.out.println("Generating " + GenerateSubtagNames.SUBTAG_NAMES_TXT);
        try (PrintWriter pw =
                FileUtilities.openUTF8Writer(
                        JSP_RESOURCE_DATA.toFile(), GenerateSubtagNames.SUBTAG_NAMES_TXT); ) {
            int count = GenerateSubtagNames.generate(pw);
            System.out.println(
                    "Wrote " + count + " entries to " + GenerateSubtagNames.SUBTAG_NAMES_TXT);
        }
    }

    private static void copyOtherProps(VersionInfo fromVersion) throws IOException {
        copyTextFiles(
                UNICODE_TOOLS_DIR.resolve("src/main/resources/org/unicode/props"), // TODO: will
                // break with
                // mavenize
                JSP_RESOURCE_DATA.resolve("data"),
                "ExtraPropertyAliases.txt",
                "ExtraPropertyValueAliases.txt");

        // Nota Bene! These aren't in the earlier list, becaause they are in the /data and not /ucd
        // dir
        copyTextFiles(
                JSP_RESOURCE_DATA.resolve("data"),
                fromVersion,
                Settings.UnicodeTools.DataDir.UCD,
                "PropertyAliases.txt",
                "PropertyValueAliases.txt");
    }
}
