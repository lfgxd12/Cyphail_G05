package cr.ac.una.eif400.cyphail.broker;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

class RowsTableTest {

    @Test
    void emptyTextHasNoColumnsAndNoRows() {
        RowsTable table = RowsTable.parse("");
        assertTrue(table.columns().isEmpty());
        assertTrue(table.rows().isEmpty());
    }

    @Test
    void onlyAHeaderMeansZeroRows() {
        RowsTable table = RowsTable.parse("p.name p.age");
        assertEquals(List.of("p.name", "p.age"), table.columns());
        assertTrue(table.rows().isEmpty());
    }

    @Test
    void parsesTheHeaderAndTheQuotedCells() {
        RowsTable table = RowsTable.parse("p.name p.age\n\"Juan Perez\" \"30\"\n\"Ana Flores\" \"21\"");
        assertEquals(List.of("p.name", "p.age"), table.columns());
        assertEquals(List.of(List.of("Juan Perez", "30"), List.of("Ana Flores", "21")), table.rows());
    }

    @Test
    void unescapesQuotesBackslashesAndNewlines() {
        RowsTable table = RowsTable.parse("x\n\"di\\\"jo\" \"a\\\\b\" \"l1\\nl2\"");
        assertEquals(List.of("di\"jo", "a\\b", "l1\nl2"), table.rows().get(0));
    }

    @Test
    void anEmptyCellIsKept() {
        assertEquals(List.of(""), RowsTable.parse("x\n\"\"").rows().get(0));
    }

    @Test
    void producesTheArraysTablePrinterExpects() {
        RowsTable table = RowsTable.parse("a b\n\"1\" \"2\"");
        assertArrayEquals(new String[]{"a", "b"}, table.headers());
        assertArrayEquals(new String[][]{{"1", "2"}}, table.data());
    }

    @Test
    void malformedLinesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> RowsTable.parse("x\nabc"));
        assertThrows(IllegalArgumentException.class, () -> RowsTable.parse("x\n\"sin cerrar"));
    }
}
