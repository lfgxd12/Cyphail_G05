package cr.ac.una.eif400.cyphail.parser;

import cr.ac.una.eif400.cyphail.ast.*;
import cr.ac.una.eif400.cyphail.parser.core.Fail;
import cr.ac.una.eif400.cyphail.parser.core.Ok;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;


/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

class ParserTest {

    private static QueryNode parseOrFail(String query) {
        return switch (CyphailParser.parse(query)) {
            case Fail(String reason) -> fail("Query should parse, but failed: " + reason);
            case Ok(QueryNode node, var rest) -> node;
        };
    }

    private static void assertSyntaxError(String query) {
        assertInstanceOf(Fail.class, CyphailParser.parse(query), "Should be a syntax error: " + query);
    }

    private static NodePattern node(String variable, List<String> labels, List<Property> properties) {
        return new NodePattern(Optional.of(variable), labels, properties);
    }

    private static PropertyLookup prop(String variable, String property) {
        return new PropertyLookup(new VariableExpr(variable), property);
    }

    @Test
    void case1_singlePatternWithAlias() {
        QueryNode query = parseOrFail("""
                MATCH (m:Movie)
                RETURN m.title,
                       m.year AS year
                """);

        assertEquals(List.of(node("m", List.of("Movie"), List.of())), query.match().patterns());
        assertTrue(query.where().isEmpty());
        assertTrue(query.updates().isEmpty());
        assertEquals(List.of(
                new ReturnItem(prop("m", "title"), Optional.empty()),
                new ReturnItem(prop("m", "year"), Optional.of("year"))
        ), query.returnClause().orElseThrow().items());
    }

    @Test
    void case2_patternWhereComparisonAlias() {
        QueryNode query = parseOrFail("""
                MATCH (b:Book)
                WHERE b.pages < 300
                RETURN b.title,
                       b.pages AS totalPages
                """);

        assertEquals(new BinaryExpr(prop("b", "pages"), "<", new LiteralExpr("300")),
                query.where().orElseThrow().condition());
        assertEquals(2, query.returnClause().orElseThrow().items().size());
    }

    @Test
    void case3_multipleLabelsWithProperty() {
        QueryNode query = parseOrFail("""
                MATCH (a:Person:Employee {id: 1})
                WHERE a.age > 30
                RETURN a.name AS name,
                       a.age AS age
                """);

        assertEquals(List.of(node("a", List.of("Person", "Employee"),
                        List.of(new Property("id", new LiteralExpr("1"))))),
                query.match().patterns());
    }

    @Test
    void case4_doublePatternDisconnected() {
        QueryNode query = parseOrFail("""
                MATCH (m:Movie), (p:Person)
                WHERE m.year > 2000
                RETURN m.title AS title,
                       p.name AS actor
                """);

        assertEquals(List.of(
                node("m", List.of("Movie"), List.of()),
                node("p", List.of("Person"), List.of())
        ), query.match().patterns());
    }

    @Test
    void case5_doublePatternDifferentAttributes() {
        QueryNode query = parseOrFail("""
                MATCH (m:Movie), (p:Person)
                WHERE m.year <> p.age
                RETURN m.title AS title,
                       p.name AS name
                """);

        assertEquals(new BinaryExpr(prop("m", "year"), "<>", prop("p", "age")),
                query.where().orElseThrow().condition());
    }

    @Test
    void case6_doublePatternEachWithProperty() {
        QueryNode query = parseOrFail("""
                MATCH (m:
                Movie {year: 1999}), (p:Person {age: 40})
                WHERE m.year <> p.age
                RETURN m.title AS title,
                       p.name AS name
                """);

        assertEquals(List.of(
                node("m", List.of("Movie"), List.of(new Property("year", new LiteralExpr("1999")))),
                node("p", List.of("Person"), List.of(new Property("age", new LiteralExpr("40"))))
        ), query.match().patterns());
    }

    @Test
    void case7_matchAndCreate() {
        QueryNode query = parseOrFail("""
                MATCH (p:Person)
                WHERE p.age > 18
                CREATE (c:Certificate {issuedTo: "adult", year: 2026})
                RETURN p.name AS name,
                       p.age AS age
                """);

        assertEquals(List.of(new CreateClause(List.of(node("c", List.of("Certificate"), List.of(
                new Property("issuedTo", new LiteralExpr("\"adult\"")),
                new Property("year", new LiteralExpr("2026"))))))
        ), query.updates());
    }

    @Test
    void case8_propertyExpressionAsValue() {
        QueryNode query = parseOrFail("""
                MATCH (p:Person {id: 1}), (o:Order {personId: p.id})
                RETURN p.name AS name,
                       o.total AS total
                """);

        NodePattern order = query.match().patterns().get(1);
        assertEquals(List.of(new Property("personId", prop("p", "id"))), order.properties());
    }

    @Test
    void case9_matchCreateDelete() {
        QueryNode query = parseOrFail("""
                MATCH (p:Person), (o:Order {personId: p.id, status: "cancelled"})
                WHERE p.age > 60
                CREATE (a:Archive {id: o.id, name: "retired", year: 2026})
                DELETE o
                RETURN p.name AS name
                """);

        assertEquals(2, query.updates().size());
        CreateClause create = assertInstanceOf(CreateClause.class, query.updates().get(0));
        assertEquals(List.of("Archive"), create.patterns().get(0).labels());
        assertEquals(new DeleteClause(false, List.of(new VariableExpr("o"))), query.updates().get(1));
    }

    @Test
    void case10_undefinedVariableStillParses() {
        QueryNode query = parseOrFail("""
                MATCH (p:Person), (o:Order {personId: p.id, status: "cancelled"})
                WHERE q.age > 60
                RETURN q AS name
                """);

        assertEquals(new ReturnItem(new VariableExpr("q"), Optional.of("name")),
                query.returnClause().orElseThrow().items().get(0));
    }

    @Test
    void case11_reversedOrderStillParses() {
        QueryNode query = parseOrFail("""
                MATCH (o:Order {personId: p.id, status: "cancelled"}), (p:Person)
                WHERE p.age > 60
                CREATE (a:Archive {id: o.id, name: "retired", year: 2026})
                DELETE o
                RETURN p.name AS name
                """);

        assertEquals(Optional.of("o"), query.match().patterns().get(0).variable());
        assertEquals(2, query.updates().size());
    }

    // Otras construcciones de la gramatica
    @Test
    void removeBeforeReturn_followsGrammarOrder() {
        QueryNode query = parseOrFail("MATCH (p:Person) REMOVE p.age, p.name RETURN p");

        assertEquals(new RemoveClause(List.of(prop("p", "age"), prop("p", "name"))), query.updates().get(0));
        assertTrue(query.returnClause().isPresent());
    }

    @Test
    void detachDelete_isParsed() {
        QueryNode query = parseOrFail("MATCH (p:Person {name: \"Ana\"}) DETACH DELETE p");
        assertEquals(new DeleteClause(true, List.of(new VariableExpr("p"))), query.updates().get(0));
    }

    @Test
    void anonymousNodeAndComments_areAccepted() {
        QueryNode query = parseOrFail("MATCH () // nodo sin variable ni etiqueta\n RETURN 1");
        assertEquals(List.of(new NodePattern(Optional.empty(), List.of(), List.of())), query.match().patterns());
    }

    @Test
    void whereWithNotEqualsOperator_isParsed() {
        QueryNode query = parseOrFail("MATCH (m:Movie) WHERE m.year <> 1999 RETURN m.title");
        BinaryExpr condition = assertInstanceOf(BinaryExpr.class, query.where().orElseThrow().condition());
        assertEquals("<>", condition.operator());
    }

    // Errores de sintaxis
    @Test
    void danglingCommaInPattern_isSyntaxError() {
        assertSyntaxError("MATCH (m:Movie), RETURN m");
    }

    @Test
    void missingClosingParenthesis_isSyntaxError() {
        assertSyntaxError("MATCH (m:Movie RETURN m");
    }

    @Test
    void trailingGarbage_isSyntaxError() {
        assertSyntaxError("MATCH (m:Movie) RETURN m.title AS");
    }

    @Test
    void queryWithoutMatch_isSyntaxError() {
        assertSyntaxError("RETURN 1");
    }
}
