package org.unicode.text.tools;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.With;
import org.unicode.props.GenerateEnums;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.ToolUnicodePropertySource;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeProperty;
import com.ibm.icu.dev.util.UnicodeProperty.RegexMatcher;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.impl.Row.R5;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public class ShowCharacters {
    static IndexUnicodeProperties iup = IndexUnicodeProperties.make("9.0");
    static UnicodeMap<String> names = iup.load(UcdProperty.Name);
    static UnicodeMap<Age_Values> ages = iup.loadEnum(UcdProperty.Age, UcdPropertyValues.Age_Values.class);
    static CandidateData CD = CandidateData.getInstance();
    
    public static void main(String[] args) {
        show("New", CD.keySet());
        System.out.println();
        show("Gendered", new UnicodeSet("[👲 👳 💂 🎅 👯 👰 🕴 ☃ ⛄ \\U0001F57A \\U0001F934 \\U0001F936 \\U0001F935]"));

        show("Emoji_Flag_Base", new UnicodeSet("[\\U0001F3F3]"));
        
        show("Emoji_Gender_Base", new UnicodeSet("[👲 👳  💂  👯  🕴   👱 👮  🕵 💆"
                + " 💇 👰 🙍 🙎 🙅 🙆 💁 🙋 🗣 👤 👥 🙇 🚶 🏃 🚴 🚵 🚣 🛀 🏄 🏊 ⛹ 🏋"
                + " \\U0001F935 \\U0001F926 \\U0001F937 \\U0001F938 \\U0001F93B \\U0001F93C \\U0001F93D \\U0001F93E]"));

        show("Emoji_Hair_Base", new UnicodeSet("[👶 👮 👲 👳 👸 🕵 👼 💆 💇 👰 🙍 🙎 🙅 🙆 💁 🙋 🙇 "
                + "🚶 🏃 💃 🚣  🏄 🏊 ⛹ 🏋 "
                + "👦 👧  👩 👴 👵  "
                + "\\U0001F935 \\U0001F926 \\U0001F937 \\U0001F938 \\U0001F93B \\U0001F93C \\U0001F93D \\U0001F93E"
                + "\\U0001F934 \\U0001F936 \\U0001F57A \\U0001F930]"));

        show("Emoji_Direction_Base", new UnicodeSet(
                "[😘 🚶 🏃 ✌ ✋ 👋-👍 👏 💪 👀 🤘 💨 ✈ 🎷 🎺 🔨 ⛏ 🗡 🔫 🚬 "
                + "\\U0001F93A \\U0001F93D \\U0001F93E \\U0001F946]"));
    }
    
    private static void show(String prop, UnicodeSet unicodeSet) {
        System.out.println("# All omitted code points have " + prop + "=No\n"
                + "# @missing: 0000..10FFFF  ; " + prop + " ; No\n");
        for (String s : unicodeSet) {
            System.out.println(Utility.hex(s) + " ;\t" + prop 
                    + "\t# " + getAge(s)
                    + " (" + s + ") " 
                    + getName(s));
        }
        System.out.println("# total:\t" + unicodeSet.size() + "\n# uset: \t" + unicodeSet.toPattern(false) + "\n");
    }

    private static String getAge(String s) {
        final Age_Values ageValue = ages.get(s);
        return ageValue == Age_Values.Unassigned ? "9.0" : ageValue.getShortName();
    }

    private static String getName(String s) {
        return CldrUtility.ifNull(CD.getName(s),names.get(s));
    }
    
    public void test(String[] args) {
        final ToolUnicodePropertySource pSource = ToolUnicodePropertySource.make(null);
        final Map<R5<String, String, String, String, String>,UnicodeSet> data = new TreeMap();
        final UnicodeProperty subhead = pSource.getProperty("subhead");
        final UnicodeProperty name = pSource.getProperty("name");
        final UnicodeProperty block = pSource.getProperty("block");
        final UnicodeProperty cased = pSource.getProperty("changeswhenuppercased");
        final UnicodeProperty lower = pSource.getProperty("Lowercase");
        final UnicodeProperty upper = pSource.getProperty("Uppercase");
        final UnicodeProperty gc = pSource.getProperty("gc");
        final UnicodeProperty script = pSource.getProperty("sc");
        final UnicodeProperty dt = pSource.getProperty("dt");
        final UnicodeProperty alphabetic = pSource.getProperty("alphabetic");

        final UnicodeSet source1 = new UnicodeSet()
        .addAll(pSource.getSet("gc=Lm"))
        .addAll(pSource.getSet("dt=super"))
        .addAll(pSource.getSet("dt=sub"))
        .freeze();

        final RegexMatcher regexMatcher = new RegexMatcher();

        final UnicodeSet source2 = new UnicodeSet()
        //        .addAll(subhead.getSet(regexMatcher.set("(?i)enclosed")))
        .addAll(block.getSet(regexMatcher.set("(?i)enclosed")))
        .addAll(name.getSet(regexMatcher.set("(?i)(circled|parenthesized|squared)")))
        .addAll(dt.getSet("Circled"))
        .removeAll(gc.getSet("Cn"))
        .removeAll(gc.getSet("sm"))
        .removeAll(gc.getSet("Lo"))
        .removeAll(gc.getSet("Po"))
        .freeze();

        // [:subhead=/(?i)Enclosed/:][:name=/CIRCLED/:][:name=/PARENTHESIZED/:]\p{name=/(?i)squared/}\p{block=/(?i)enclosed/}-\p{cn}-\p{sm}-\p{Lo}-\p{Po}

        final UnicodeSet source = args.length == 0 ? source1 : source2;

        final UnicodeSet okScripts = new UnicodeSet();
        for (final String okScript : Arrays.asList("Zyyy", "Armn", "Copt", "Dest", "Glag", "Cyrl", "Grek", "Geor", "Latn")) {
            okScripts.addAll(script.getSet(okScript));
        }
        okScripts.freeze();

        for (final String s : source) {
            final int ch = s.codePointAt(0);
            final String nfkdForm = Default.nfkd().normalize(s);
            final String scriptValue = getScript(s, script);
            if (okScripts != null && !okScripts.containsSome(s) && !okScripts.containsSome(nfkdForm)) {
                continue;
            }
            final String dscriptValue = getScript(nfkdForm, script);

            final String casing = alphabeticStatus(s, lower, upper, alphabetic);
            final String dcasing = alphabeticStatus(nfkdForm, lower, upper, alphabetic);
            final R5<String, String, String, String, String> row = Row.of(
                    scriptValue,
                    dscriptValue,
                    casing,
                    dcasing,
                    gc.getValue(ch,true)
                    );
            UnicodeSet us = data.get(row);
            if (us == null) {
                data.put(row, us = new UnicodeSet());
            }
            us.add(ch);
        }

        final PrintWriter log = Utility.openPrintWriter(Settings.GEN_DIR, "showChars.html", Utility.UTF8_WINDOWS);
        log.println("<html><body>");
        log.println("<table style='border-collapse:collapse' border='1'>");
        log.println("<tr>"
                +"<th>Count</th>"
                +"<th>SC</th>"
                +"<th>dSC</th>"
                +"<th>L/U/A</th>"
                +"<th>dL/U/A</th>"
                +"<th>GC</th>"
                +"<th>Code</th>"
                +"<th>Char</th>"
                +"<th>Name</th>"
                +"</tr>"
                );
        int count = 0;
        for (final Entry<R5<String, String, String, String, String>, UnicodeSet> entry : data.entrySet()) {
            final R5<String, String, String, String, String> row = entry.getKey();
            for (final UnicodeSetIterator it = new UnicodeSetIterator(entry.getValue()); it.nextRange();) {
                final R3<String, String, String> chardata = getCharData(it);
                final int start = count;
                count += (it.codepointEnd - it.codepoint + 1);
                log.println("<tr>"
                        +"<td style='text-align:right'>'"+(count-start)+"</td>"
                        +"<td style='text-align:center'>"+row.get0()+"</td>"
                        +"<td style='text-align:center'>"+row.get1()+"</td>"
                        +"<td style='text-align:center'>"+row.get2()+"</td>"
                        +"<td style='text-align:center'>"+row.get3()+"</td>"
                        +"<td style='text-align:center'>"+row.get4()+"</td>"
                        +"<td style='text-align:center'><code>'"+chardata.get0()+"<code></td>"
                        +"<td style='text-align:center; font-size:200%'>"+chardata.get1()+"</td>"
                        +"<td><code>"+chardata.get2()+"<code></td>"
                        +"</tr>"
                        );
            }
        }
        log.println("</table>");
        log.println("<p>Count: " + count + "</p>");
        log.println("</body></html>");
        log.close();
        System.out.println("Count: " + count);
    }

    private static String getScript(String string, UnicodeProperty script) {
        final Set<String> results = new HashSet<String>();
        for (final int ch : With.codePointArray(string)) {
            results.add( script.getValue(ch,true));
        }
        // remove common. If results are unique, return. Otherwise return common
        results.remove("Zyyy");
        results.remove("Zinh");
        return results.size() == 1 ? results.iterator().next() : "Zyyy";
    }


    private static String alphabeticStatus(String string, UnicodeProperty lower, UnicodeProperty upper, UnicodeProperty alphabetic) {
        final Set<String> results = new HashSet<String>();

        for (final int ch : With.codePointArray(string)) {
            boolean didOne = false;
            if (lower.getValue(ch,true).equals("Y")) {
                results.add("l");
                didOne = true;
            }
            if (upper.getValue(ch,true).equals("Y")) {
                results.add("U");
                didOne = true;
            }
            if (alphabetic.getValue(ch,true).equals("Y")) {
                results.add("A");
                didOne = true;
            }
            if (!didOne) {
                return "-";
            }
        }
        results.remove("A");
        return results.size() == 1 ? results.iterator().next() : "A";
    }

    private static R3<String, String, String> getCharData(UnicodeSetIterator it) {
        if (it.codepoint == it.codepointEnd) {
            return Row.of(Utility.hex(it.codepoint),
                    UTF16.valueOf(it.codepoint),
                    Default.ucd().getName(it.codepoint));
        } else {
            final String sep = it.codepointEnd == it.codepoint + 1 ? "," : "…";
            return Row.of(Utility.hex(it.codepoint) + sep + Utility.hex(it.codepointEnd),
                    UTF16.valueOf(it.codepoint) + sep + UTF16.valueOf(it.codepointEnd),
                    Default.ucd().getName(it.codepoint) + sep + Default.ucd().getName(it.codepointEnd)
                    );
        }

    }
}
