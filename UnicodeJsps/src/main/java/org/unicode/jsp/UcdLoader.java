package org.unicode.jsp;

import com.ibm.icu.util.VersionInfo;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebServlet;
import java.io.IOException;
import java.util.jar.Manifest;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.text.utility.Settings;

@WebServlet
public class UcdLoader implements jakarta.servlet.Servlet {

    // Allow access to the last (published) and latest (dev) versions lazily in tests, though these
    // will get fully loaded by this servlet before actually serving the JSPs.
    static VersionInfo oldestLoadedUcd = Settings.LAST_VERSION_INFO;

    public static synchronized VersionInfo getOldestLoadedUcd() {
        return oldestLoadedUcd;
    }

    public static synchronized void setOldestLoadedUcd(VersionInfo v) {
        if (v.compareTo(oldestLoadedUcd) < 0) {
            oldestLoadedUcd = v;
        }
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
        try {
            UnicodeJsp.MANIFEST =
                    new Manifest(
                            config.getServletContext()
                                    .getResourceAsStream("/META-INF/MANIFEST.MF"));
        } catch (IOException e) {
            throw new InternalError(e);
        }
        IndexUnicodeProperties.loadUcdHistory(
                Settings.LATEST_VERSION_INFO, UcdLoader::setOldestLoadedUcd, true);
        new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                IndexUnicodeProperties.loadUcdHistory(
                                        null, UcdLoader::setOldestLoadedUcd, true);
                            }
                        })
                .start();
    }

    @Override
    public void service(ServletRequest request, ServletResponse response)
            throws ServletException, IOException {}
}
