package cr.ac.una.eif400.cyphail.validation;

import cr.ac.una.eif400.cyphail.ast.QueryNode;
import cr.ac.una.eif400.cyphail.parser.CyphailParser;
import cr.ac.una.eif400.cyphail.parser.core.Ok;
import org.junit.jupiter.api.Test;

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

// Reglas de nombres de P2 (SPEC II.6/II.7), probadas desde el texto con el parser real
class AnalyzerTest {

    private static QueryNode parse(String text) {
        var result = CyphailParser.parse(text);
        assertInstanceOf(Ok.class, result, "no parsea: " + text + " -> " + result);
        return ((Ok<?, QueryNode, String>) result).token();
    }

    private static List<String> errors(String text) {
        return Analyzer.check(parse(text));
    }

    @Test
    void aValidRelationshipQueryHasNoErrors() {
        assertEquals(List.of(), errors("MATCH (a)-[r:amigo]->(b) WHERE a.age > 20 RETURN a.name, r, b"));
    }

    @Test
    void theEightSpecStatementsAreValid() {
        assertEquals(List.of(), errors("MATCH (a:Person)-[r:amigo]->(b:Person) RETURN a, r, b"));
        assertEquals(List.of(), errors("MATCH (a), (b) CREATE (a)-[:ESCRIBIO]->(b)"));
    }

    @Test
    void aRepeatedRelationshipVariableIsRejected() {
        List<String> errors = errors("MATCH (a)-[r:amigo]->(b)-[r:amigo]->(c) RETURN a");
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("'r'") && errors.get(0).contains("cannot be repeated"), errors.get(0));
    }

    @Test
    void aRelationshipVariableRepeatedBetweenMatchAndCreateIsRejected() {
        assertEquals(1, errors("MATCH (a)-[r:amigo]->(b) CREATE (a)-[r:amigo]->(b)").size());
    }

    @Test
    void aNodeVariableCanBeRepeatedBecauseItIsAReference() {
        assertEquals(List.of(), errors("MATCH (a)-[:amigo]->(b), (b)-[:amigo]->(c) RETURN a, c"));
    }

    @Test
    void theSameNameAsNodeAndRelationshipIsRejected() {
        List<String> nodeFirst = errors("MATCH (x)-[x:amigo]->(b) RETURN b");
        assertEquals(1, nodeFirst.size());
        assertTrue(nodeFirst.get(0).contains("already used as a node"), nodeFirst.get(0));

        List<String> relFirst = errors("MATCH (a)-[x:amigo]->(b), (x) RETURN a");
        assertEquals(1, relFirst.size());
        assertTrue(relFirst.get(0).contains("already used as a relationship"), relFirst.get(0));
    }

    @Test
    void namesStartingWithTwoUnderscoresAreReserved() {
        List<String> errors = errors("MATCH (__a) RETURN __a");
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("reserved"), errors.get(0));
        assertEquals(1, errors("MATCH (a)-[__r:amigo]->(b) RETURN a").size());
        assertEquals(List.of(), errors("MATCH (_a) RETURN _a"));
    }

    @Test
    void deleteOnlyAcceptsVariables() {
        assertEquals(List.of(), errors("MATCH (a)-[r:amigo]->(b) DELETE r"));
        assertEquals(1, errors("MATCH (a) DELETE a.name").size());
    }

    @Test
    void symbolsOfListsTheDeclaredVariablesWithTheirKind() {
        SymbolTable table = Analyzer.symbolsOf(parse("MATCH (a)-[r:amigo]->(b) RETURN a"));

        assertEquals(Map.of("a", SymbolTable.Kind.NODE, "r", SymbolTable.Kind.RELATIONSHIP, "b", SymbolTable.Kind.NODE),
                table.table());
        assertEquals(List.of("a", "r", "b"), List.copyOf(table.table().keySet()));
        assertEquals(SymbolTable.Kind.RELATIONSHIP, table.kindOf("r").orElseThrow());
        assertTrue(table.kindOf("zzz").isEmpty());
    }

    @Test
    void validateThrowsWithAllTheMessages() {
        var ex = assertThrows(SemanticException.class,
                () -> Analyzer.validate(parse("MATCH (a)-[r:amigo]->(b)-[r:amigo]->(c) RETURN z")));
        assertTrue(ex.getMessage().contains("cannot be repeated"));
        assertTrue(ex.getMessage().contains("Undefined variable 'z'"));
    }
}
