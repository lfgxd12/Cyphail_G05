package cr.ac.una.eif400.cyphail.broker;

import java.util.ArrayList;
import java.util.List;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

// El texto "Rows" que devuelve el engine (contrato C2), ya separado en encabezado y celdas.
//   Encabezado: las columnas separadas por un espacio. Filas: una por linea, cada celda entre comillas
//   con \\, \" y \n escapados. Sin columnas el texto viene vacio; sin filas solo trae el encabezado.
public record RowsTable(List<String> columns, List<List<String>> rows) {

    public RowsTable {
        columns = List.copyOf(columns);
        rows = rows.stream().map(List::copyOf).toList();
    }

    public static RowsTable parse(String text) {
        if (text == null || text.isEmpty()) {
            return new RowsTable(List.of(), List.of());
        }
        String[] lines = text.split("\n", -1);
        List<String> columns = lines[0].isEmpty() ? List.of() : List.of(lines[0].split(" "));
        List<List<String>> rows = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            if (!lines[i].isEmpty()) {
                rows.add(cells(lines[i]));
            }
        }
        return new RowsTable(columns, rows);
    }

    // Los datos tal como los pide TablePrinter
    public String[] headers() {
        return columns.toArray(new String[0]);
    }

    public String[][] data() {
        return rows.stream().map(row -> row.toArray(new String[0])).toArray(String[][]::new);
    }

    private static List<String> cells(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder cell = null;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (cell == null) {
                if (c == '"') {
                    cell = new StringBuilder();
                } else if (c != ' ') {
                    throw new IllegalArgumentException("Malformed Rows line: " + line);
                }
            } else if (c == '\\' && i + 1 < line.length()) {
                char next = line.charAt(++i);
                cell.append(next == 'n' ? '\n' : next);
            } else if (c == '"') {
                cells.add(cell.toString());
                cell = null;
            } else {
                cell.append(c);
            }
        }
        if (cell != null) {
            throw new IllegalArgumentException("Malformed Rows line: " + line);
        }
        return cells;
    }
}
