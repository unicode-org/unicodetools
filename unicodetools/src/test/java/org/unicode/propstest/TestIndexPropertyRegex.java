package org.unicode.propstest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.PropertyStatus;
import org.unicode.props.UcdProperty;
import org.unicode.text.utility.Settings;

public class TestIndexPropertyRegex {

    @Test
    void testIndexPropertyRegex() throws IOException {
        IndexUnicodeProperties latest = IndexUnicodeProperties.make(Settings.latestVersion);
        for (final UcdProperty prop : UcdProperty.values()) {
            if (PropertyStatus.getPropertyStatus(prop) != PropertyStatus.Deprecated) {
                try {
                    latest.load(prop);
                } catch (Exception e) {
                    System.out.println(prop + "\t" + latest.getUcdVersion());
                    e.printStackTrace();
                    return;
                }
            }
        }
        if (IndexUnicodeProperties.getDataLoadingErrors(latest.getUcdVersion()) != null) {
            final Set<Map.Entry<UcdProperty, Set<String>>> dataLoadingErrors =
                    IndexUnicodeProperties.getDataLoadingErrors(latest.getUcdVersion())
                            .keyValuesSet();
            if (!dataLoadingErrors.isEmpty()) {
                System.err.println(
                        "Data loading errors for "
                                + latest.getUcdVersion().toString()
                                + ": "
                                + dataLoadingErrors.size());
                for (final Map.Entry<UcdProperty, Set<String>> s : dataLoadingErrors) {
                    System.err.println("\t" + s.getKey());
                    int max = 100;
                    for (final String value : s.getValue()) {
                        System.err.println("\t\t" + value);
                        if (--max < 0) {
                            System.err.println("…");
                            break;
                        }
                    }
                }
            }
            assertEquals(
                    0,
                    dataLoadingErrors.size(),
                    "TestIndexPropertyRegex.testIndexPropertyRegex() failed");
        }
    }
}
