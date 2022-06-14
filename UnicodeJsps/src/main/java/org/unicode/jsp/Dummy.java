package org.unicode.jsp;

import org.unicode.cldr.tool.TablePrinter;

public class Dummy {
    public static String getTest() {
        TablePrinter tablePrinter =
                new TablePrinter()
                        .setTableAttributes("style='border-collapse: collapse' border='1'")
                        .addColumn("Language")
                        .setSpanRows(true)
                        .setSortPriority(0)
                        .setBreakSpans(true)
                        .addColumn("Junk")
                        .setSpanRows(true)
                        .addColumn("Territory")
                        .setHeaderAttributes("bgcolor='green'")
                        .setCellAttributes("align='right'")
                        .setSpanRows(true)
                        .setSortPriority(1)
                        .setSortAscending(false);
        Comparable[][] data = {
            {"German", 1.3d, 3},
            {"French", 1.3d, 2},
            {"English", 1.3d, 2},
            {"English", 1.3d, 4},
            {"English", 1.3d, 6},
            {"English", 1.3d, 8},
            {"Arabic", 1.3d, 5},
            {"Zebra", 1.3d, 10}
        };
        tablePrinter.addRows(data);
        tablePrinter.addRow().addCell("Foo").addCell(1.5d).addCell(99).finishRow();

        String s = tablePrinter.toTable();
        return s;
    }
}
