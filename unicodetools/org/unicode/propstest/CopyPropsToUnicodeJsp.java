package org.unicode.propstest;

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.PropertyStatus;
import org.unicode.props.UcdProperty;
import org.unicode.text.utility.Settings;

import com.google.common.collect.ImmutableSet;

public class CopyPropsToUnicodeJsp {
    public static void main(String[] args) throws IOException {
        IndexUnicodeProperties latest = IndexUnicodeProperties.make();

        String fromDir = Settings.Output.BIN_DIR + latest.getUcdVersion() + "/";
        String toDir = Settings.UnicodeTools.UNICODEJSPS_DIR + "src/org/unicode/jsp/props/";
        //overwrite existing file, if exists
        CopyOption[] options = new CopyOption[] {StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES};
        Set<String> kExceptions = ImmutableSet.of("kAccountingNumeric.bin", "kOtherNumeric.bin", "kPrimaryNumeric.bin", "kSimplifiedVariant.bin", "kTraditionalVariant.bin");

        for (String name : new File(fromDir).list()) {
            if (!name.endsWith(".bin")) {
                System.out.println("Skipping1 " + name);
                continue;
            }
            if (name.startsWith("k")) {
                if (!kExceptions.contains(name)) {
                    System.out.println("Skipping2 " + name);
                    continue;
                } else {
                    System.out.println("Retaining2 " + name);
                }
            }
            String pname = name.substring(0, name.length()-4);
            UcdProperty prop = UcdProperty.forString(pname);
            EnumSet<PropertyStatus> status = PropertyStatus.getPropertyStatusSet(prop);

            if (!Collections.disjoint(status, ListProps.SKIP_JSP_STATUS)) {
                System.out.println("Skipping3 " + prop);
                continue;
            }
            Path FROM = Paths.get(fromDir + name);
            Path TO = Paths.get(toDir + name);
            Files.copy(FROM, TO, options);
        }
    }
}
