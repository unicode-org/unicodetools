package org.unicode.draft;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.draft.ExemplarInfo;
import org.unicode.cldr.draft.ExemplarInfo.Status;
import org.unicode.cldr.tool.TablePrinter;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.UnicodeSetPrettyPrinter;
import org.unicode.cldr.util.SupplementalDataInfo.PopulationData;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;


public class GenerateCharacterFrequencyCharts {

    static UnicodeSet SKIP_NAME = new UnicodeSet("[:UnifiedIdeograph:]");

    public static void main(String[] args) throws IOException {
        new GenerateCharacterFrequencyCharts().run();
    }

    private void run() throws IOException {
        final Set<String> missingExemplars = new TreeSet<String>();

        final NumberFormat pf = NumberFormat.getPercentInstance(ULocale.ENGLISH);
        pf.setMinimumFractionDigits(6);
        final NumberFormat nf = NumberFormat.getNumberInstance(ULocale.ENGLISH);
        nf.setGroupingUsed(true);

        final TablePrinter indexTable = new TablePrinter()
        .addColumn("Char Count").setCellAttributes("class='count'").setSortAscending(false).setSortPriority(0).setCellPattern("{0,number,#,##0}")
        .addColumn("%/Total").setCellAttributes("class='pc'").setCellPattern("{0,number,#,##0.00%}")
        .addColumn("Locale").setCellAttributes("class='locale'").setCellPattern("<a href=''{0}.html''>{0}</a>")
        .addColumn("Name")
        .addColumn("Lit. Pop").setCellAttributes("class='count'").setCellPattern("{0,number,#,##0}")
        .addColumn("%World").setCellAttributes("class='pc'").setCellPattern("{0,number,#,##0.00%}")
        .addColumn("Ch/Lit").setCellAttributes("class='pc'").setCellPattern("{0,number,#,##0.00%}")
        .addColumn("Chars")
        ;
        
        final TablePrinter indexTable2 = new TablePrinter()
        .addColumn("Char Count").setCellAttributes("class='count'").setSortAscending(false).setSortPriority(0).setCellPattern("{0,number,#,##0}")
        .addColumn("%/Total").setCellAttributes("class='pc'").setCellPattern("{0,number,#,##0.0000%}")
        .addColumn("Locale").setCellAttributes("class='locale'").setCellPattern("<a href=''{0}.html''>{0}</a>")
        .addColumn("Name")
        .addColumn("Lit. Pop").setCellAttributes("class='count'").setCellPattern("{0,number,#,##0}")
        .addColumn("%World").setCellAttributes("class='pc'").setCellPattern("{0,number,#,##0.0000%}")
        .addColumn("Ch/Lit").setCellAttributes("class='pc'").setCellPattern("{0,number,#,##0.0000%}")
        ;

        final TablePrinter summaryTable = new TablePrinter()
        .addColumn("Locale").setCellAttributes("class='locale'")
        .addColumn("Name")
        .addColumn("Lit. Pop").setCellAttributes("class='count'").setCellPattern("{0,number,#,##0}").setSortAscending(false).setSortPriority(0)
        .addColumn("Chars")
        ;
        final Counter<Integer> mulCounter = CharacterFrequency.getCodePointCounter("mul", false);
        // hack: print top 1000 supplemental characters
        int topSupp = 1000;
        for (final Integer s : mulCounter.getKeysetSortedByCount(false)) {
            final int cp = s;
            if (cp > 0xFFFF) {
                System.out.println(mulCounter.get(s) + "\t" + UCharacter.getName(s));
            }
            if (--topSupp < 0) {
                break;
            }
        }
        final long totalTotal = mulCounter.getTotal();
        //double worldPop = CharacterFrequency.getLanguageToPopulation("mul");
        //final long worldPop = CharacterFrequency.getCodePointCounter("mul", true).getTotal();

        CLDRConfig testInfo = CLDRConfig.getInstance();
        SupplementalDataInfo supplemental = testInfo.getSupplementalDataInfo();

        double worldPop = 0;

        for (final String language : CharacterFrequency.getLanguagesWithCounter()) {
            if (language.equals("und")) {
                continue;
            }
            final String cldrLanguage = ExemplarInfo.getCldrLanguage(language);
            worldPop += getPopulation(supplemental, cldrLanguage);
        }

        for (final String language : CharacterFrequency.getLanguagesWithCounter()) {
            if (language.equals("und")) {
                continue;
            }

            final CharacterSamples indexChars = new CharacterSamples(language);

            final String cldrLanguage = ExemplarInfo.getCldrLanguage(language);
            final Counter<Integer> counter = CharacterFrequency.getCodePointCounter(language, false);
            final long total = counter.getTotal();

            final String htmlFilename = language + ".html";
            final String englishLocaleName = ULocale.getDisplayName(language, "en");
            final String htmlTitle = language + " - " + englishLocaleName;
            System.out.println(htmlTitle);

            PopulationData popInfo = supplemental.getLanguagePopulationData(cldrLanguage);

            final double pop = language.equals("mul") ? worldPop : getPopulation(supplemental, cldrLanguage);


            // get exemplars

            final ExemplarInfo exemplarInfo = ExemplarInfo.make(cldrLanguage, missingExemplars);
            // open files for writing, create table
            final DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(Settings.GEN_DIR + "/frequency/" + language + ".txt"));
            final CompressedDataOutput out = new CompressedDataOutput().set(dataOutputStream);
            final PrintWriter html = FileUtilities.openUTF8Writer(Settings.GEN_DIR + "/frequency-html", htmlFilename);
            html.println("<html><head><meta charset='UTF-8'>\n" +
                    "<link rel='stylesheet' type='text/css' href='index.css' media='screen'/>\n" +
                    "</head><body>\n" +
                    "<h1>" + htmlTitle + "</h1>");
            final TablePrinter table = new TablePrinter()
            .addColumn("Rank").setCellAttributes("class='rank'")
            .addColumn("Count").setCellAttributes("class='count'")
            .addColumn("%").setCellAttributes("class='pc'")
            .addColumn("Type").setCellAttributes("class='type'")
            .addColumn("Ex").setCellAttributes("class=''{0}''").setCellPattern("·{0}·")
            .addColumn("Level").setCellAttributes("class='chars'")
            .addColumn("Chars").setCellAttributes("class='name'")
            .addColumn("Hex").setCellAttributes("class='hex'")
            .addColumn("Name").setCellAttributes("class='name'")
            ;
            final long enough = (long)(total*0.99999d);
            System.out.println(total + "\t" + enough + "\t" + (total - enough) + "\t" + ((total - enough)/(double)total));
            long soFar = 0;
            final Collection<String> typeList = new LinkedHashSet<String>();

            long rank = 0;
            for (final int sequence : counter.getKeysetSortedByCount(false)) {
                final long count = counter.get(sequence);
                //if (count < COUNT_LIMIT) break; // skip for now
                final String hex = Utility.hex(sequence);

                out.writeUnsignedLong(count);
                final String sequence2 = UTF16.valueOf(sequence);
                out.writeUTF(sequence2);

                final String type = getType(sequence2, typeList);
                String name = SKIP_NAME.containsAll(sequence2) ? "" : UCharacter.getName(sequence2, "+");
                if (name == null) {
                    name = "none";
                }
                final ExemplarInfo.Status exemplar = exemplarInfo.getStatus(sequence2);
                indexChars.add(sequence2, exemplar);
                final String percent = pf.format(count/(double)total);
                final String decimal = nf.format(count);
                final String rankStr = nf.format(++rank);
                final String level = exemplarInfo.getEducationLevel(sequence2);
                table.addRow()
                .addCell(rankStr)
                .addCell(decimal)
                .addCell(percent)
                .addCell(type)
                .addCell(exemplar)
                .addCell(level == null ? "-" : "·" + level + "·")
                .addCell(sequence).addCell(hex).addCell(name).finishRow();
                soFar += count;
                if (soFar >= enough) {
                    break;
                }
            }

            double charsOverTotal = total/(double)totalTotal;
            double popOverTotal = pop/worldPop;
            indexTable.addRow()
            .addCell(total)
            .addCell(charsOverTotal)
            .addCell(language)
            .addCell(englishLocaleName)
            .addCell(pop)
            .addCell(popOverTotal)
            .addCell(charsOverTotal/popOverTotal)
            .addCell(indexChars.toString())
            .finishRow();

            indexTable2.addRow()
            .addCell(total)
            .addCell(charsOverTotal)
            .addCell(language)
            .addCell(englishLocaleName)
            .addCell(pop)
            .addCell(popOverTotal)
            .addCell(charsOverTotal/popOverTotal)
            .finishRow();

            summaryTable.addRow()
            .addCell(language)
            .addCell(englishLocaleName)
            .addCell(pop)
            .addCell(indexChars.toString())
            .finishRow();

            //out.close();
            html.println(table);
            html.println("</body></html>");
            html.close();
        }
        printIndex(indexTable, "index.html");
        printIndex(indexTable2, "index2.html");
        printIndex(summaryTable, "summary.html");

        System.out.println("Missing exemplars:\t" + missingExemplars);
    }

    public double getPopulation(SupplementalDataInfo supplemental,
            final String cldrLanguage) {
        PopulationData popInfo = supplemental.getLanguagePopulationData(cldrLanguage);
        if (popInfo == null) {
            String defaultScript = supplemental.getDefaultScript(cldrLanguage);
            if (defaultScript != null) {
                popInfo = supplemental.getLanguagePopulationData(cldrLanguage + "_" + defaultScript);
            }
            if (popInfo == null) {
                System.out.println("Can't get pop data for: " + cldrLanguage);
                return 0;
            }
        }
        return popInfo.getLiteratePopulation();
    }

    private void printIndex(TablePrinter indexTable, String file) throws IOException {
        final PrintWriter index = FileUtilities.openUTF8Writer(Settings.GEN_DIR + "/frequency-html", file);
        index.println("<html><head><meta charset='UTF-8'>\n" +
                "<link rel='stylesheet' type='text/css' href='index.css' media='screen'/>\n" +
                "</head><body>\n");
        index.println(indexTable);
        index.println("</body></html>");
        index.close();
    }


    private static class CharacterSamples {
        static UnicodeSetPrettyPrinter pp = new UnicodeSetPrettyPrinter();
        static final UnicodeSet DIGITS = new UnicodeSet("[:nd:]").freeze();

        List<String> indexChars = new ArrayList<String>();
        UnicodeSet indexSet = new UnicodeSet();
        ExemplarInfo.Status lastExemplarStatus = null;
        boolean isKey = false;
        int sequenceCount = 0;
        UnicodeSet exemplarsLeft;

        CharacterSamples(String language) {
            exemplarsLeft = new UnicodeSet(ExemplarInfo.make(language, null).getExemplars()).removeAll(ExemplarInfo.IGNORE);
            isKey = language.equals("mul");
            pp.setOrdering(Collator.getInstance(new ULocale(language)));
            final RuleBasedCollator spaceComp = (RuleBasedCollator) Collator.getInstance(new ULocale(language));
            spaceComp.setStrength(Collator.PRIMARY);
            pp.setSpaceComparator(spaceComp);
        }
        void add(String sequence, ExemplarInfo.Status exemplar) {
            if (DIGITS.containsAll(sequence)) {
                return;
            }
            if (exemplar == Status.O || exemplar == Status.X || sequenceCount > 1000) {
                return;
            }
            sequenceCount++;
            if (!exemplar.equals(lastExemplarStatus) || (sequenceCount % 10) == 0) {
                finish(indexSet, lastExemplarStatus);
                indexSet.clear();
                lastExemplarStatus = exemplar;
            }
            indexSet.add(sequence);
            exemplarsLeft.removeAll(sequence);
        }
        void finish(UnicodeSet unicodeSet, Status lastExemplarStatus2) {
            if (indexSet.size() != 0) {
                String setString = pp.format(unicodeSet);
                setString = setString.substring(1,setString.length()-1);
                indexChars.add("<td class='" + lastExemplarStatus2 + "'>" + setString + "</td>");
            }
        }
        @Override
        public String toString() {
            if (isKey) {
                return "<table><tr>"
                        + "<td class='M'>Main exemplars</td>"
                        + "<td class='A'>Aux exemplars</td>"
                        + "<td class='S'>Main exemplar scripts</td>"
                        + "<td class='T'>Aux exemplar scripts</td>"
                        + "<td class='N'>Exemplars not found</td>"
                        + "</tr></table>";
            }
            finish(indexSet, lastExemplarStatus);
            if (exemplarsLeft.size() < 1000) {
                finish(exemplarsLeft, Status.N);
            }
            return "<table><tr>" + CollectionUtilities.join(indexChars,"") + "</tr></table>";
        }
    }

    private String getType(String sequence, Collection<String> items) {
        items.clear();
        int cp;
        for (int i = 0; i < sequence.length(); i+=Character.charCount(cp)) {
            cp = sequence.codePointAt(i);
            final int script = UScript.getScript(cp);
            if (script != UScript.UNKNOWN && script != UScript.COMMON && script != UScript.INHERITED) {
                items.add(UScript.getShortName(script));
            } else {
                final int type = UCharacter.getType(cp);
                items.add(UCharacter.getPropertyValueName(UProperty.GENERAL_CATEGORY, type, UProperty.NameChoice.SHORT));
            }
        }
        if (items.size() > 1 && items.contains("Zinh")) {
            items.remove("Zinh");
        }
        if (items.size() > 1 && items.contains("Zyyy")) {
            items.remove("Zyyy");
        }
        return CollectionUtilities.join(items, "/");
    }
}
