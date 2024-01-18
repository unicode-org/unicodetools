package org.unicode.jsp;

import com.ibm.icu.util.ICUException;
import com.ibm.icu.util.VersionInfo;
import java.io.IOException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.text.utility.Settings;

@WebServlet
public class UcdLoader implements javax.servlet.Servlet {

    // Allow access to the last (published) and latest (dev) versions lazily in tests, though these
    // will get fully loaded by this servlet before actually serving the JSPs.
    static VersionInfo oldestLoadedUcd = Settings.LAST_VERSION_INFO;

    public static synchronized VersionInfo getOldestLoadedUcd() {
        return oldestLoadedUcd;
    }

    private static synchronized void setOldestLoadedUcd(VersionInfo v) {
        oldestLoadedUcd = v;
    }

    private static void loadUcdHistory(VersionInfo earliest) {
        System.out.println("Loading back to " + earliest + "...");
        Age_Values[] ages = Age_Values.values();
        final long overallStart = System.currentTimeMillis();
        for (int i = ages.length - 1; i >= 0; --i) {
            final var age = ages[i];
            if (age == Age_Values.Unassigned) {
                continue;
            }
            final long ucdStart = System.currentTimeMillis();
            System.out.println("Loading UCD " + age.getShortName() + "...");
            for (boolean unihan : new boolean[] {false, true}) {
                final long partStart = System.currentTimeMillis();
                final String name = unihan ? "Unihan" : "non-Unihan properties";
                final var properties = IndexUnicodeProperties.make(age.getShortName());
                for (UcdProperty property : UcdProperty.values()) {
                    if (property.getShortName().startsWith("cjk") == unihan) {
                        try {
                            properties.load(property);
                        } catch (ICUException e) {
                            e.printStackTrace();
                        }
                    }
                }
                System.out.println(
                        "Loaded "
                                + name
                                + " for "
                                + age.getShortName()
                                + " ("
                                + (System.currentTimeMillis() - partStart)
                                + " ms)");
            }
            System.out.println(
                    "Loaded UCD "
                            + age.getShortName()
                            + " in "
                            + (System.currentTimeMillis() - ucdStart)
                            + " ms");
            var version = VersionInfo.getInstance(age.getShortName());
            setOldestLoadedUcd(version);
            if (version == earliest) {
                break;
            }
        }
        System.out.println(
                "Loaded all UCD history in "
                        + (System.currentTimeMillis() - overallStart) / 1000
                        + " s");
    }

    @Override
    public void destroy() {}

    @Override
    public ServletConfig getServletConfig() {
        return null;
    }

    @Override
    public String getServletInfo() {
        return null;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        loadUcdHistory(Settings.LAST_VERSION_INFO);
        new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                loadUcdHistory(null);
                            }
                        })
                .start();
    }

    @Override
    public void service(ServletRequest request, ServletResponse response)
            throws ServletException, IOException {}
}
