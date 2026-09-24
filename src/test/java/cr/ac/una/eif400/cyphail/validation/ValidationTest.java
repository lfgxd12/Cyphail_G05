package cr.ac.una.eif400.cyphail.validation;

import cr.ac.una.eif400.cyphail.ast.QueryNode;
import cr.ac.una.eif400.cyphail.parser.CyphailParser;
import cr.ac.una.eif400.cyphail.parser.core.Fail;
import cr.ac.una.eif400.cyphail.parser.core.Ok;
import org.junit.jupiter.api.Disabled;
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
        // Version reducida del Caso 10 del profesor, con un unico patron
        // MATCH (p:Person) porque el parser aun no soporta multiples patrones
        // separados por coma. Cubre el mismo concepto semantico: 'q' se usa
        // en WHERE sin haber sido declarada en ningun MATCH.
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
        // m.title usa la propiedad 'title' de la variable 'm', que si esta
        // declarada; 'title' no debe confundirse con una variable no definida.
        QueryNode query = parseOrFail("MATCH (m:Movie) RETURN m.title AS title");
        assertDoesNotThrow(() -> VariableScopeChecker.validate(query));
    }

    @Test
    @Disabled("Requiere multiples patrones en MATCH (pendiente en CyphailParser); ver " +
            "undefinedVariableInWhere_throwsSemanticException para una version equivalente " +
            "con la gramatica actualmente soportada")
    void case10_officialUndefinedVariable() {
        // MATCH (p:Person), (o:Order {personId: p.id, status: "cancelled"}) WHERE q.age > 60 RETURN q AS name
    }

    @Test
    @Disabled("Requiere multiples patrones, CREATE y DELETE (pendiente en CyphailParser)")
    void case11_officialReversedOrderUndefinedVariable() {
        // MATCH (o:Order {personId: p.id, status: "cancelled"}), (p:Person) WHERE p.age > 60
        // CREATE (a:Archive {id: o.id, name: "retired", year: 2026}) DELETE o RETURN p.name AS name
    }

    private static QueryNode parseOrFail(String query) {
        return switch (CyphailParser.parse(query)) {
            case Fail(String reason) -> throw new AssertionError("Query should parse for this test: " + reason);
            case Ok(QueryNode node, var rest) -> node;
        };
    }
}