package cr.ac.una.eif400.cyphail.broker;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

class PrologResultTest {

    // El switch no tiene default: si se agregara un cuarto caso, esta prueba dejaria de compilar.
    private static String describe(PrologResult result) {
        return switch (result) {
            case PrologResult.Success s -> "success:" + s.rows().size();
            case PrologResult.Failure f -> "failure";
            case PrologResult.PrologError e -> "error:" + e.term();
        };
    }

    @Test
    void theThreeCasesAreTreatedExhaustively() {
        assertEquals("success:1", describe(new PrologResult.Success(List.of(Map.of("X", "a")))));
        assertEquals("failure", describe(new PrologResult.Failure()));
        assertEquals("error:time_limit_exceeded", describe(new PrologResult.PrologError("time_limit_exceeded")));
    }

    @Test
    void successKeepsItsRowsInOrder() {
        var rows = List.of(Map.<String, Object>of("X", "a"), Map.<String, Object>of("X", "b"));
        assertEquals(rows, new PrologResult.Success(rows).rows());
    }

    @Test
    void successDoesNotChangeIfTheOriginalListChanges() {
        var original = new ArrayList<Map<String, Object>>();
        original.add(Map.of("X", 1));
        var success = new PrologResult.Success(original);
        original.add(Map.of("X", 2));
        assertEquals(1, success.rows().size());
    }

    @Test
    void successRowsCannotBeModified() {
        var success = new PrologResult.Success(List.of());
        assertThrows(UnsupportedOperationException.class, () -> success.rows().add(Map.of()));
    }

    @Test
    void failuresAreEqualToEachOther() {
        assertEquals(new PrologResult.Failure(), new PrologResult.Failure());
    }
}
