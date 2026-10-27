package cr.ac.una.eif400.cyphail.broker;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

class PrologCompoundTest {

    @Test
    void nestedCompoundsRepresentAnExistenceError() {
        // existence_error(procedure, cy_prueba/1)
        var indicator = new PrologCompound("/", List.of("cy_prueba", 1L));
        var error = new PrologCompound("existence_error", List.of("procedure", indicator));
        assertEquals("existence_error", error.functor());
        assertEquals(indicator, error.args().get(1));
    }

    @Test
    void compoundsWithTheSameContentAreEqual() {
        assertEquals(new PrologCompound("f", List.of("a", 1L)), new PrologCompound("f", List.of("a", 1L)));
        assertNotEquals(new PrologCompound("f", List.of("a")), new PrologCompound("g", List.of("a")));
    }

    @Test
    void argumentsMayBeNull() {
        var compound = new PrologCompound("f", Arrays.asList("a", null));
        assertNull(compound.args().get(1));
    }

    @Test
    void argumentsCannotBeModified() {
        var compound = new PrologCompound("f", List.of("a"));
        assertThrows(UnsupportedOperationException.class, () -> compound.args().add("b"));
    }

    @Test
    void argumentsDoNotChangeIfTheOriginalListChanges() {
        var original = new ArrayList<Object>(List.of("a"));
        var compound = new PrologCompound("f", original);
        original.add("b");
        assertEquals(1, compound.args().size());
    }
}