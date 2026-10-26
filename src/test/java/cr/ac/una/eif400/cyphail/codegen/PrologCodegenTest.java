package cr.ac.una.eif400.cyphail.codegen;

import cr.ac.una.eif400.cyphail.ast.*;
import cr.ac.una.eif400.cyphail.parser.CyphailParser;
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

class PrologCodegenTest {

    private final PrologCodegen codegen = new PrologCodegen(new PredicateNames("amigos"));

    // ---------- ayudantes ----------

    // El parser todavia no lee relaciones ni AND/OR/NOT: las consultas de nodos salen del parser
    private CodegenResult fromText(String query) {
        QueryNode node = switch (CyphailParser.parse(query)) {
            case Fail(String reason) -> fail("Query should parse: " + reason);
            case Ok(QueryNode parsed, var rest) -> parsed;
        };
        return codegen.generate(node);
    }

    private static NodePattern node(String variable) {
        return new NodePattern(Optional.of(variable), List.of(), List.of());
    }

    private static PropertyLookup prop(String variable, String property) {
        return new PropertyLookup(new VariableExpr(variable), property);
    }

    private static RelPattern rel(String variable, String type, Direction direction) {
        return new RelPattern(Optional.ofNullable(variable), List.of(type), List.of(), direction);
    }

    private static PathPattern path(NodePattern start, RelPattern rel, NodePattern end) {
        return new PathPattern(start, List.of(new Step(rel, end)));
    }

    private static QueryNode read(List<PathPattern> patterns, Expression where, List<ReturnItem> items) {
        return new QueryNode(Optional.of(new MatchClause(patterns)), Optional.ofNullable(where).map(WhereClause::new),
                List.of(), Optional.of(new ReturnClause(items)));
    }

    private static ReturnItem item(Expression expression) {
        return new ReturnItem(expression, Optional.empty());
    }

    // Los goals del cuerpo van uno por linea, separados por coma y con 8 espacios de sangria
    private static String body(String... goals) {
        return String.join(",\n        ", goals);
    }

    // ---------- consultas de nodos (salen del parser) ----------

    @Test
    void nodeWithWhereAndReturn() {
        CodegenResult result = fromText("MATCH (a:Person) WHERE a.age > 18 RETURN a.name");

        assertEquals("""
                cy_create_query(__QueryId,
                    (query_01(__Aname) :-
                        user_amigos_label(__A, 'Person'),
                        user_amigos_prop(__A, age, __Aage),
                        cy_gt(__Aage, 18),
                        user_amigos_prop(__A, name, __Aname))),
                cy_exec_query(__QueryId, ['a.name'], Rows),
                cy_erase_query(__QueryId)""", result.goal());
        assertEquals(List.of("a.name"), result.columns());
    }

    @Test
    void aliasBecomesTheColumnName() {
        CodegenResult result = fromText("MATCH (m:Movie {year: 1999}) RETURN m.title AS title");

        assertTrue(result.goal().contains("user_amigos_prop(__M, year, 1999)"));
        assertTrue(result.goal().contains("query_01(__Mtitle)"));
        assertEquals(List.of("title"), result.columns());
    }

    @Test
    void twoPatternsAreJoinedByTheirVariables() {
        CodegenResult result = fromText(
                "MATCH (m:Movie), (p:Person) WHERE m.year <> p.age RETURN m.title AS title, p.name AS name");

        assertTrue(result.goal().contains("user_amigos_label(__M, 'Movie'),\n        user_amigos_label(__P, 'Person')"));
        assertTrue(result.goal().contains("cy_neq(__Myear, __Page)"));
        assertEquals(List.of("title", "name"), result.columns());
    }

    @Test
    void propertyValueCanBeAnotherVariablesProperty() {
        // Caso C8 del profesor: {personId: p.id}
        CodegenResult result = fromText(
                "MATCH (p:Person {id: 1}), (o:Order {personId: p.id}) RETURN p.name AS name, o.total AS total");

        assertTrue(result.goal().contains(body(
                "user_amigos_label(__P, 'Person')",
                "user_amigos_prop(__P, id, 1)",
                "user_amigos_label(__O, 'Order')",
                "user_amigos_prop(__P, id, __Pid)",
                "user_amigos_prop(__O, personId, __Pid)")));
    }

    @Test
    void stringLiteralsBecomeAtoms() {
        CodegenResult result = fromText("MATCH (p:Person {name: \"Juan Perez\", kind: 'adult'}) RETURN p");

        assertTrue(result.goal().contains("user_amigos_prop(__P, name, 'Juan Perez')"));
        assertTrue(result.goal().contains("user_amigos_prop(__P, kind, adult)"));
    }

    @Test
    void returningAPatternVariableReturnsItsId() {
        CodegenResult result = fromText("MATCH (a) RETURN a");

        assertEquals("""
                cy_create_query(__QueryId,
                    (query_01(__A) :-
                        user_amigos_node(__A))),
                cy_exec_query(__QueryId, ['a'], Rows),
                cy_erase_query(__QueryId)""", result.goal());
    }

    @Test
    void anonymousNodeIsAnAnonymousPrologVariable() {
        CodegenResult result = fromText("MATCH (:Movie) RETURN 1");

        assertTrue(result.goal().contains("user_amigos_label(_, 'Movie')"));
        assertTrue(result.goal().contains("query_01(1)"));
    }

    @Test
    void repeatedGoalsAreEmittedOnce() {
        CodegenResult result = fromText("MATCH (a:Person) WHERE a.age > 18 RETURN a.age, a.age AS again");

        assertEquals(1, result.goal().split("user_amigos_prop\\(__A, age, __Aage\\)", -1).length - 1);
    }

    // ---------- relaciones (el AST se arma a mano hasta que el parser las lea) ----------

    @Test
    void specExample_mutualFriends() {
        // MATCH (a)-[:amigo]->(b), (b)-[:amigo]->(a)
        // WHERE a.age > 18 AND a.age = b.age  RETURN a.name, b.name
        var where = new BinaryExpr(
                new BinaryExpr(prop("a", "age"), ">", new LiteralExpr("18")), "AND",
                new BinaryExpr(prop("a", "age"), "=", prop("b", "age")));
        var query = read(List.of(
                        path(node("a"), rel(null, "amigo", Direction.OUT), node("b")),
                        path(node("b"), rel(null, "amigo", Direction.OUT), node("a"))),
                where, List.of(item(prop("a", "name")), item(prop("b", "name"))));

        CodegenResult result = codegen.generate(query);

        assertEquals("""
                cy_create_query(__QueryId,
                    (query_01(__Aname, __Bname) :-
                        user_amigos_edge(__E1, amigo, __A, __B),
                        user_amigos_edge(__E2, amigo, __B, __A),
                        cy_neq(__E1, __E2),
                        user_amigos_prop(__A, age, __Aage),
                        cy_gt(__Aage, 18),
                        user_amigos_prop(__B, age, __Bage),
                        cy_eq(__Aage, __Bage),
                        user_amigos_prop(__A, name, __Aname),
                        user_amigos_prop(__B, name, __Bname))),
                cy_exec_query(__QueryId, ['a.name', 'b.name'], Rows),
                cy_erase_query(__QueryId)""", result.goal());
        assertEquals(List.of("a.name", "b.name"), result.columns());
        assertEquals("__Aname", result.variables().get("a.name"));
    }

    @Test
    void directionInSwapsTheEndpoints() {
        // (a)<-[:amigo]-(b)
        var query = read(List.of(path(node("a"), rel(null, "amigo", Direction.IN), node("b"))),
                null, List.of(item(new VariableExpr("a"))));

        assertTrue(codegen.generate(query).goal().contains("user_amigos_edge(__E1, amigo, __B, __A)"));
    }

    @Test
    void directionNoneAcceptsBothWays() {
        // (a)-[:amigo]-(b)
        var query = read(List.of(path(node("a"), rel(null, "amigo", Direction.NONE), node("b"))),
                null, List.of(item(new VariableExpr("a"))));

        assertTrue(codegen.generate(query).goal().contains(
                "( user_amigos_edge(__E1, amigo, __A, __B) ; user_amigos_edge(__E1, amigo, __B, __A) )"));
    }

    @Test
    void namedRelationshipWithPropertiesCanBeReadAndFiltered() {
        // (a)-[r:amigo {since: 2000}]->(b) WHERE r.since < 2010 RETURN r.since
        var relationship = new RelPattern(Optional.of("r"), List.of("amigo"),
                List.of(new Property("since", new LiteralExpr("2000"))), Direction.OUT);
        var where = new BinaryExpr(prop("r", "since"), "<", new LiteralExpr("2010"));
        var query = read(List.of(path(node("a"), relationship, node("b"))), where, List.of(item(prop("r", "since"))));

        CodegenResult result = codegen.generate(query);

        assertTrue(result.goal().contains(body(
                "user_amigos_edge(__R, amigo, __A, __B)",
                "user_amigos_prop(__R, since, 2000)",
                "user_amigos_prop(__R, since, __Rsince)",
                "cy_lt(__Rsince, 2010)")));
    }

    @Test
    void relationshipsOfTheSameMatchMustBeDifferent() {
        // (a)-[:x]->(b), (b)-[:x]->(c), (c)-[:x]->(d): los tres arcos son distintos entre si
        var query = read(List.of(
                        path(node("a"), rel(null, "x", Direction.OUT), node("b")),
                        path(node("b"), rel(null, "x", Direction.OUT), node("c")),
                        path(node("c"), rel(null, "x", Direction.OUT), node("d"))),
                null, List.of(item(new VariableExpr("a"))));

        String goal = codegen.generate(query).goal();

        assertTrue(goal.contains("cy_neq(__E1, __E2)"));
        assertTrue(goal.contains("cy_neq(__E1, __E3)"));
        assertTrue(goal.contains("cy_neq(__E2, __E3)"));
    }

    @Test
    void labelsOnRelationshipEndpointsComeAroundTheEdge() {
        // (a:Person)-[:amigo]->(b:Person)
        var a = new NodePattern(Optional.of("a"), List.of("Person"), List.of());
        var b = new NodePattern(Optional.of("b"), List.of("Person"), List.of());
        var query = read(List.of(path(a, rel(null, "amigo", Direction.OUT), b)), null, List.of(item(new VariableExpr("a"))));

        assertTrue(codegen.generate(query).goal().contains(body(
                "user_amigos_label(__A, 'Person')",
                "user_amigos_edge(__E1, amigo, __A, __B)",
                "user_amigos_label(__B, 'Person')")));
    }

    // ---------- AND, OR y NOT ----------

    @Test
    void orAndNotBecomePrologGoals() {
        // WHERE NOT (a.age < 18) OR a.age = 30
        var where = new BinaryExpr(
                new NotExpr(new BinaryExpr(prop("a", "age"), "<", new LiteralExpr("18"))), "OR",
                new BinaryExpr(prop("a", "age"), "=", new LiteralExpr("30")));
        var query = read(List.of(new PathPattern(node("a"), List.of())), where, List.of(item(new VariableExpr("a"))));

        assertTrue(codegen.generate(query).goal().contains("(\\+ cy_lt(__Aage, 18) ; cy_eq(__Aage, 30))"));
    }

    // ---------- lo que todavia no se traduce ----------

    @Test
    void updatesAreNotSupportedYet() {
        QueryNode query = new QueryNode(
                Optional.of(new MatchClause(List.of(new PathPattern(node("a"), List.of())))), Optional.empty(),
                List.of(new DeleteClause(false, List.of(new VariableExpr("a")))), Optional.empty());

        assertThrows(UnsupportedOperationException.class, () -> codegen.generate(query));
    }

    @Test
    void aReadQueryNeedsMatchAndReturn() {
        var onlyMatch = new QueryNode(Optional.of(new MatchClause(List.of(new PathPattern(node("a"), List.of())))),
                Optional.empty(), List.of(), Optional.empty());
        var onlyReturn = new QueryNode(Optional.empty(), Optional.empty(), List.of(),
                Optional.of(new ReturnClause(List.of(item(new LiteralExpr("1"))))));

        assertThrows(UnsupportedOperationException.class, () -> codegen.generate(onlyMatch));
        assertThrows(UnsupportedOperationException.class, () -> codegen.generate(onlyReturn));
    }

    @Test
    void alternativeRelationshipTypesAreNotSupportedYet() {
        var alternatives = new RelPattern(Optional.empty(), List.of("a", "b"), List.of(), Direction.OUT);
        var query = read(List.of(path(node("a"), alternatives, node("b"))), null, List.of(item(new VariableExpr("a"))));

        assertThrows(UnsupportedOperationException.class, () -> codegen.generate(query));
    }
}
