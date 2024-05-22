package org.unicode.draft;

/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import com.ibm.icu.lang.UCharacter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Util to process the "Accept-Language" header. Used by facade to implement getLocale() and by
 * StaticInterceptor.
 *
 * <p>Not optimized - it's very slow.
 *
 * @author James Duncan Davidson [duncan@eng.sun.com]
 * @author James Todd [gonzo@eng.sun.com]
 * @author Jason Hunter [jch@eng.sun.com]
 * @author Harish Prabandham
 * @author costin@eng.sun.com
 */
public class AcceptLanguage {
    UCharacter foo;

    public static Locale getLocale(String acceptLanguage) {
        if (acceptLanguage == null) {
            return Locale.getDefault();
        }

        final Hashtable languages = new Hashtable();
        final Vector quality = new Vector();
        processAcceptLanguage(acceptLanguage, languages, quality);

        if (languages.size() == 0) {
            return Locale.getDefault();
        }

        final Vector l = new Vector();
        extractLocales(languages, quality, l);

        return (Locale) l.elementAt(0);
    }

    public static Enumeration getLocales(String acceptLanguage) {
        // Short circuit with an empty enumeration if null header
        if (acceptLanguage == null) {
            final Vector v = new Vector();
            v.addElement(Locale.getDefault());
            return v.elements();
        }

        final Hashtable languages = new Hashtable();
        final Vector quality = new Vector();
        processAcceptLanguage(acceptLanguage, languages, quality);

        if (languages.size() == 0) {
            final Vector v = new Vector();
            v.addElement(Locale.getDefault());
            return v.elements();
        }
        final Vector l = new Vector();
        extractLocales(languages, quality, l);
        return l.elements();
    }

    private static void processAcceptLanguage(
            String acceptLanguage, Hashtable languages, Vector q) {
        final StringTokenizer languageTokenizer = new StringTokenizer(acceptLanguage, ",");

        while (languageTokenizer.hasMoreTokens()) {
            String language = languageTokenizer.nextToken().trim();
            int qValueIndex = language.indexOf(';');
            final int qIndex = language.indexOf('q');
            final int equalIndex = language.indexOf('=');
            Double qValue = new Double(1);

            if (qValueIndex > -1 && qValueIndex < qIndex && qIndex < equalIndex) {
                String qValueStr = language.substring(qValueIndex + 1);
                language = language.substring(0, qValueIndex);
                qValueStr = qValueStr.trim().toLowerCase();
                qValueIndex = qValueStr.indexOf('=');
                qValue = new Double(0);
                if (qValueStr.startsWith("q") && qValueIndex > -1) {
                    qValueStr = qValueStr.substring(qValueIndex + 1);
                    try {
                        qValue = new Double(qValueStr.trim());
                    } catch (final NumberFormatException nfe) {
                    }
                }
            }

            // XXX
            // may need to handle "*" at some point in time

            if (!language.equals("*")) {
                final String key = qValue.toString();
                Vector v;
                if (languages.containsKey(key)) {
                    v = (Vector) languages.get(key);
                } else {
                    v = new Vector();
                    q.addElement(qValue);
                }
                v.addElement(language);
                languages.put(key, v);
            }
        }
    }

    private static void extractLocales(Hashtable languages, Vector q, Vector l) {
        // XXX We will need to order by q value Vector in the Future ?
        final Enumeration e = q.elements();
        while (e.hasMoreElements()) {
            final Vector v = (Vector) languages.get(((Double) e.nextElement()).toString());
            final Enumeration le = v.elements();
            while (le.hasMoreElements()) {
                String language = (String) le.nextElement();
                String country = "";
                final int countryIndex = language.indexOf("-");
                if (countryIndex > -1) {
                    country = language.substring(countryIndex + 1).trim();
                    language = language.substring(0, countryIndex).trim();
                }
                l.addElement(new Locale(language, country));
            }
        }
    }

    public static void main(String[] args) {
        final Hashtable languages = new Hashtable();
        final Vector quality = new Vector();
        processAcceptLanguage("ar-arz, ar-arb, ar, ar-shu;q=0, de;q=0.5", languages, quality);
        System.out.println("languages: " + languages);
        System.out.println("quality: " + quality);
        final Vector locales = new Vector();
        extractLocales(languages, quality, locales);
        System.out.println("locales: " + locales);
    }
}
