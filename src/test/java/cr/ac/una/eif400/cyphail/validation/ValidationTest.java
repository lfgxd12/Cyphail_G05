package cr.ac.una.eif400.cyphail.validation;

import cr.ac.una.eif400.cyphail.ast.QueryNode;
import cr.ac.una.eif400.cyphail.parser.CyphailParser;
import cr.ac.una.eif400.cyphail.parser.core.Fail;
import cr.ac.una.eif400.cyphail.parser.core.Ok;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 *          Jostin Jimenez Alfaro, Angel Rojas Ruano
 */
class ValidationTest {

    @Test
    void definedVariable_doesNotThrow() {
        QueryNode query = parseOrFail("MATCH (p:Person) WHERE p.age > 60 RETURN p.name AS name");
        assertDoesNotThrow(() -> VariableScopeChecker.validate(query));
    }

    @Test
    void undefinedVariableInWhere_throwsSemanticException() {
        QueryNode query = parseOrFail("MATCH (p:Person) WHERE q.age > 60 RETURN q AS name");

        SemanticException ex = assertThrows(SemanticException.class,
                () -> VariableScopeChecker.validate(query));
        assertTrue(ex.getMessage().contains("q"));
    }

    @Test
    void undefinedVariableInReturn_throwsSemanticException() {
        QueryNode query = parseOrFail("MATCH (p:Person) RETURN q.name AS name");
        assertThrows(SemanticException.class, () -> VariableScopeChecker.validate(query));
    }

    @Test
    void undefinedVariableInRemove_throwsSemanticException() {
        QueryNode query = parseOrFail("MATCH (p:Person) REMOVE q.age");
        assertThrows(SemanticException.class, () -> VariableScopeChecker.validate(query));
    }

    @Test
    void propertyOfDeclaredVariable_isNotFlaggedAsUndefined() {
        QueryNode query = parseOrFail("MATCH (m:Movie) RETURN m.title AS title");
        assertDoesNotThrow(() -> VariableScopeChecker.validate(query));
    }

    @Test
    void case8_propertyUsesPreviouslyDeclaredVariable_isValid() {
        QueryNode query = parseOrFail("""
                MATCH (p:Person {id: 1}), (o:Order {personId: p.id})
                RETURN p.name AS name,
                       o.total AS total
                """);
        assertEquals(List.of(), VariableScopeChecker.check(query));
    }

    @Test
    void case9_createAndDeleteWithDeclaredVariables_isValid() {
        QueryNode query = parseOrFail("""
                MATCH (p:Person), (o:Order {personId: p.id, status: "cancelled"})
                WHERE p.age > 60
                CREATE (a:Archive {id: o.id, name: "retired", year: 2026})
                DELETE o
                RETURN p.name AS name
                """);
        assertEquals(List.of(), VariableScopeChecker.check(query));
    }

    @Test
    void case10_officialUndefinedVariable_reportsBothUses() {
        QueryNode query = parseOrFail("""
                MATCH (p:Person), (o:Order {personId: p.id, status: "cancelled"})
                WHERE q.age > 60
                RETURN q AS name
                """);

        List<String> errors = VariableScopeChecker.check(query);
        assertEquals(2, errors.size());
        assertTrue(errors.get(0).contains("'q'") && errors.get(0).contains("WHERE"));
        assertTrue(errors.get(1).contains("'q'") && errors.get(1).contains("RETURN"));
    }

    @Test
    void case11_officialReversedOrder_reportsVariableUsedBeforeDeclaration() {
        QueryNode query = parseOrFail("""
                MATCH (o:Order {personId: p.id, status: "cancelled"}), (p:Person)
                WHERE p.age > 60
                CREATE (a:Archive {id: o.id, name: "retired", year: 2026})
                DELETE o
                RETURN p.name AS name
                """);

        List<String> errors = VariableScopeChecker.check(query);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("'p'") && errors.get(0).contains("(o)"));
    }

    // Otras reglas de alcance
    @Test
    void variableDeclaredInCreate_canBeUsedAfterwards() {
        QueryNode query = parseOrFail("MATCH (p:Person) CREATE (c:Certificate {owner: p.name}) RETURN c");
        assertEquals(List.of(), VariableScopeChecker.check(query));
    }

    @Test
    void undefinedVariableInDelete_isReported() {
        QueryNode query = parseOrFail("MATCH (p:Person) DELETE x");
        assertEquals(1, VariableScopeChecker.check(query).size());
    }

    @Test
    void repeatedUndefinedUse_isReportedOnce() {
        QueryNode query = parseOrFail("MATCH (p:Person) RETURN q.name, q.name");
        assertEquals(1, VariableScopeChecker.check(query).size());
    }

    private static QueryNode parseOrFail(String query) {
        return switch (CyphailParser.parse(query)) {
            case Fail(String reason) -> throw new AssertionError("Query should parse for this test: " + reason);
            case Ok(QueryNode node, var rest) -> node;
        };
    }
}