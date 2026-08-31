package cr.ac.una.eif400.cyphail.output;

import com.github.freva.asciitable.AsciiTable;

/**
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 *          Jostin Jimenez Alfaro, Angel Rojas Ruano
 */
public class TablePrinter {
    public static void printTable(String[] headers, String[][] data) {
        System.out.println(AsciiTable.getTable(headers, data));
    }
}
