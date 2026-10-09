package org.unicode.unittest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.unicode.text.utility.DiffingPrintWriter;

public class DiffingPrintWriterTest {
    @TempDir Path directory;

    private static final String OLD =
            "# data.txt\n# Date: old\n# © 2026 Unicode®, Inc.\n# Version: 19.0.0\n0041 ; a\n";
    private static final String NEW_HEADER =
            OLD.replace("Date: old", "Date: new").replace("2026", "2027");

    private Path reference() {
        return directory.resolve("reference.txt");
    }

    private Path output() {
        return directory.resolve("output.txt");
    }

    private void generate(String text) {
        try (DiffingPrintWriter out =
                new DiffingPrintWriter(
                        output().toFile(), reference().toFile(), /* skipCopyright= */ true)) {
            out.write(text.toCharArray(), 0, text.length());
        }
    }

    @Test
    public void testUnchangedFileKeepsReferenceHeader() throws IOException {
        Files.writeString(reference(), OLD);
        generate(NEW_HEADER);
        assertEquals(OLD, Files.readString(output()));
        assertEquals(OLD, Files.readString(reference()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"# © 2026 Unicode®, Inc.", "# Copyright (c) 2026 Unicode, Inc."})
    public void testCopyrightOnlyChange(String copyright) throws IOException {
        String old = OLD.replace("# © 2026 Unicode®, Inc.", copyright);
        Files.writeString(reference(), old);
        generate(old.replace("2026", "2027"));
        assertEquals(old, Files.readString(output()));
    }

    @Test
    public void testChangedFileGetsNewHeader() throws IOException {
        Files.writeString(reference(), OLD);
        Files.writeString(output(), OLD);
        String changed = NEW_HEADER.replace("0041 ; a", "0042 ; b");
        generate(changed);
        assertEquals(changed, Files.readString(output()));
        assertEquals(OLD, Files.readString(reference()));
    }

    @Test
    public void testVersionChangeGetsNewHeader() throws IOException {
        Files.writeString(reference(), OLD);
        String changed = NEW_HEADER.replace("19.0.0", "20.0.0");
        generate(changed);
        assertEquals(changed, Files.readString(output()));
    }

    @Test
    public void testRepeatedGenerationKeepsOutputHeader() throws IOException {
        Files.writeString(reference(), OLD);
        String changed = NEW_HEADER.replace("0041 ; a", "0042 ; b");
        generate(changed);
        generate(changed.replace("Date: new", "Date: newer").replace("2027", "2028"));
        assertEquals(changed, Files.readString(output()));
    }

    @Test
    public void testNewFile() throws IOException {
        generate(NEW_HEADER);
        assertEquals(NEW_HEADER, Files.readString(output()));
    }

    @Test
    public void testInternalFileKeepsExistingHeader() throws IOException {
        Files.writeString(output(), OLD);
        generate(NEW_HEADER);
        assertEquals(OLD, Files.readString(output()));
    }

    @Test
    public void testReferenceMatchReplacesStaleOutput() throws IOException {
        Files.writeString(reference(), OLD);
        Files.writeString(output(), NEW_HEADER.replace("0041 ; a", "0042 ; b"));
        generate(NEW_HEADER);
        assertEquals(OLD, Files.readString(output()));
    }

    @Test
    public void testDefaultConstructorDoesNotIgnoreCopyright() throws IOException {
        Files.writeString(output(), OLD);
        try (DiffingPrintWriter out = new DiffingPrintWriter(output().toFile())) {
            out.write(NEW_HEADER.toCharArray(), 0, NEW_HEADER.length());
        }
        assertEquals(NEW_HEADER, Files.readString(output()));
    }
}
