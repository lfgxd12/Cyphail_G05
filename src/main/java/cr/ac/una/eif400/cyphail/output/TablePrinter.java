package cr.ac.una.eif400.cyphail.output;

import com.github.freva.asciitable.AsciiTable;

public class TablePrinter {
    public static void printTable(String[] headers, String[][] data) {
        System.out.println(AsciiTable.getTable(headers, data));
    }
}