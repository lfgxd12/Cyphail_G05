package cr.ac.una.eif400.cyphail.codegen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

class PredicateNamesTest {

    @Test
    void graphPredicatesCarryTheGraphName() {
        var names = new PredicateNames("amigos");
        assertEquals("user_amigos_node", names.node());
        assertEquals("user_amigos_label", names.label());
        assertEquals("user_amigos_edge", names.edge());
        assertEquals("user_amigos_prop", names.prop());
    }

    @Test
    void invalidGraphNamesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new PredicateNames("Amigos"));
        assertThrows(IllegalArgumentException.class, () -> new PredicateNames("1x"));
        assertThrows(IllegalArgumentException.class, () -> new PredicateNames("mi grafo"));
        assertThrows(IllegalArgumentException.class, () -> new PredicateNames(""));
    }

    @Test
    void enginePredicatesHaveTheirFixedNames() {
        assertEquals("cy_create_query", PredicateNames.createQuery());
        assertEquals("cy_exec_query", PredicateNames.execQuery());
        assertEquals("cy_erase_query", PredicateNames.eraseQuery());
    }

    @Test
    void everyComparisonOperatorHasAnEnginePredicate() {
        assertEquals("cy_eq", PredicateNames.comparator("="));
        assertEquals("cy_neq", PredicateNames.comparator("<>"));
        assertEquals("cy_gt", PredicateNames.comparator(">"));
        assertEquals("cy_lt", PredicateNames.comparator("<"));
        assertEquals("cy_gte", PredicateNames.comparator(">="));
        assertEquals("cy_lte", PredicateNames.comparator("<="));
    }

    @Test
    void anythingElseIsNotAComparison() {
        assertThrows(IllegalArgumentException.class, () -> PredicateNames.comparator("AND"));
    }
}