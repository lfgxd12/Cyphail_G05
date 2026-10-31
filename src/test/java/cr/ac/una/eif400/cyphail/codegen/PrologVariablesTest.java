package cr.ac.una.eif400.cyphail.codegen;

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

class PrologVariablesTest {

    @Test
    void variableIsCapitalizedWithThePrefix() {
        var vars = new PrologVariables();
        assertEquals("__A", vars.variable("a"));
        assertEquals("__Person", vars.variable("person"));
    }

    @Test
    void propertyUsesTheCapitalizedVariableAndTheProperty() {
        assertEquals("__Aname", new PrologVariables().property("a", "name"));
    }

    @Test
    void sameExpressionAlwaysGetsTheSameVariable() {
        var vars = new PrologVariables();
        assertEquals(vars.property("a", "age"), vars.property("a", "age"));
        assertEquals(vars.variable("a"), vars.variable("a"));
    }

    @Test
    void variablesThatDifferOnlyInCaseDoNotCollide() {
        var vars = new PrologVariables();
        assertEquals("__A", vars.variable("a"));
        assertEquals("__A_2", vars.variable("A"));
    }

    @Test
    void propertyAndVariableThatBuildTheSameNameDoNotCollide() {
        var vars = new PrologVariables();
        assertEquals("__Aname", vars.property("a", "name"));
        assertEquals("__Aname_2", vars.variable("aname"));
    }

    @Test
    void suffixedNamesAreAlsoReserved() {
        var vars = new PrologVariables();
        assertEquals("__A", vars.variable("a"));
        assertEquals("__A_2", vars.variable("A"));
        assertEquals("__A_2_2", vars.variable("a_2"));
    }

    @Test
    void systemVariableIsReserved() {
        assertEquals("__QueryId_2", new PrologVariables().variable("QueryId"));
    }

    @Test
    void anonymousRelationshipsAreNumbered() {
        var vars = new PrologVariables();
        assertEquals("__E1", vars.nextRelationship());
        assertEquals("__E2", vars.nextRelationship());
    }

    @Test
    void relationshipNamesAndUserVariablesDoNotCollide() {
        var generatedFirst = new PrologVariables();
        assertEquals("__E1", generatedFirst.nextRelationship());
        assertEquals("__E1_2", generatedFirst.variable("e1"));

        var userFirst = new PrologVariables();
        assertEquals("__E1", userFirst.variable("e1"));
        assertEquals("__E2", userFirst.nextRelationship());
    }

    @Test
    void nextNodeNumbersTheNodesCreatedWithoutAVariable() {
        var vars = new PrologVariables();
        assertEquals("__N1", vars.nextNode());
        assertEquals("__N2", vars.nextNode());
        assertEquals("__E1", vars.nextRelationship());
    }

    @Test
    void refreshMakesThePropertyGetAFreshVariable() {
        var vars = new PrologVariables();
        assertEquals("__Aage", vars.property("a", "age"));
        vars.refresh("a", "age");
        assertEquals("__Aage_2", vars.property("a", "age"));
        assertEquals("__Aage_2", vars.property("a", "age"));
    }

    @Test
    void tableKeepsTheOrderAndCannotBeModified() {
        var vars = new PrologVariables();
        vars.variable("b");
        vars.property("a", "name");

        assertEquals(List.of("b", "a.name"), List.copyOf(vars.table().keySet()));
        assertEquals("__Aname", vars.table().get("a.name"));
        assertThrows(UnsupportedOperationException.class, () -> vars.table().put("x", "__X"));
    }
}