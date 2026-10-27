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

class RelationshipParserTest {

    private static QueryNode parseOrFail(String query) {
        return switch (CyphailParser.parse(query)) {
            case Fail(String reason) -> fail("Query should parse, but failed: " + reason);
            case Ok(QueryNode node, var rest) -> node;
        };
    }

    private static void assertSyntaxError(String query) {
        assertInstanceOf(Fail.class, CyphailParser.parse(query), "Should be a syntax error: " + query);
    }

    private static NodePattern n(String variable) {
        return new NodePattern(Optional.of(variable), List.of(), List.of());
    }

    private static NodePattern n(String variable, String label) {
        return new NodePattern(Optional.of(variable), List.of(label), List.of());
    }

    private static RelPattern rel(List<String> types, Direction direction) {
        return new RelPattern(Optional.empty(), types, List.of(), direction);
    }

    private static PropertyLookup prop(String variable, String property) {
        return new PropertyLookup(new VariableExpr(variable), property);
    }

    // Los PathPattern del MATCH de la consulta
    private static List<PathPattern> paths(String query) {
        return parseOrFail(query).match().orElseThrow().patterns();
    }

    @Test
    void outgoingRelationshipWithType() {
        assertEquals(
                List.of(new PathPattern(n("a"), List.of(new Step(rel(List.of("KNOWS"), Direction.OUT), n("b"))))),
                paths("MATCH (a)-[:KNOWS]->(b) RETURN a"));
    }

    @Test
    void incomingRelationshipWithType() {
        assertEquals(
                List.of(new PathPattern(n("a"), List.of(new Step(rel(List.of("KNOWS"), Direction.IN), n("b"))))),
                paths("MATCH (a)<-[:KNOWS]-(b) RETURN a"));
    }

    @Test
    void undirectedRelationshipWithType() {
        assertEquals(
                List.of(new PathPattern(n("a"), List.of(new Step(rel(List.of("KNOWS"), Direction.NONE), n("b"))))),
                paths("MATCH (a)-[:KNOWS]-(b) RETURN a"));
    }

    @Test
    void relationshipsWithoutBrackets() {
        assertEquals(List.of(new PathPattern(n("a"), List.of(new Step(rel(List.of(), Direction.OUT), n("b"))))),
                paths("MATCH (a)-->(b) RETURN a"));
        assertEquals(List.of(new PathPattern(n("a"), List.of(new Step(rel(List.of(), Direction.IN), n("b"))))),
                paths("MATCH (a)<--(b) RETURN a"));
        assertEquals(List.of(new PathPattern(n("a"), List.of(new Step(rel(List.of(), Direction.NONE), n("b"))))),
                paths("MATCH (a)--(b) RETURN a"));
    }

    @Test
    void relationshipWithVariableAndProperties() {
        var expected = new RelPattern(Optional.of("r"), List.of("KNOWS"),
                List.of(new Property("since", new LiteralExpr("1999"))), Direction.OUT);
        assertEquals(List.of(new PathPattern(n("a"), List.of(new Step(expected, n("b"))))),
                paths("MATCH (a)-[r:KNOWS {since: 1999}]->(b) RETURN a"));
    }

    @Test
    void relationshipWithVariableOnly() {
        var expected = new RelPattern(Optional.of("r"), List.of(), List.of(), Direction.OUT);
        assertEquals(List.of(new PathPattern(n("a"), List.of(new Step(expected, n("b"))))),
                paths("MATCH (a)-[r]->(b) RETURN a"));
    }

    @Test
    void alternativeRelationshipTypes() {
        assertEquals(
                List.of(new PathPattern(n("a"), List.of(new Step(rel(List.of("A", "B"), Direction.OUT), n("b"))))),
                paths("MATCH (a)-[:A|B]->(b) RETURN a"));
    }

    @Test
    void chainedRelationshipsShareOnePath() {
        assertEquals(
                List.of(new PathPattern(n("a"), List.of(
                        new Step(rel(List.of("R"), Direction.OUT), n("b")),
                        new Step(rel(List.of("S"), Direction.IN), n("c"))))),
                paths("MATCH (a)-[:R]->(b)<-[:S]-(c) RETURN a"));
    }

    @Test
    void spacesAndNewlinesAroundTheRelationshipAreIgnored() {
        assertEquals(
                List.of(new PathPattern(n("a"), List.of(new Step(rel(List.of("amigo"), Direction.OUT), n("b"))))),
                paths("MATCH (a) -[:amigo]->\n   (b)\nRETURN a"));
    }

    @Test
    void plainNodeStillParsesAsPathWithoutSteps() {
        assertEquals(List.of(new PathPattern(n("a", "Person"), List.of())),
                paths("MATCH (a:Person) RETURN a"));
    }

    @Test
    void spec1_fullAst() {
        QueryNode query = parseOrFail("""
                MATCH (p:Persona)-[:TRABAJA_EN]->(e:Empresa),
                      (c:Persona)-[:TRABAJA_EN]->(e)
                WHERE p.nombre <> c.nombre
                RETURN p.nombre, c.nombre, e.nombre
                """);

        var trabaja = rel(List.of("TRABAJA_EN"), Direction.OUT);
        var expected = new QueryNode(
                Optional.of(new MatchClause(List.of(
                        new PathPattern(n("p", "Persona"), List.of(new Step(trabaja, n("e", "Empresa")))),
                        new PathPattern(n("c", "Persona"), List.of(new Step(trabaja, n("e"))))))),
                Optional.of(new WhereClause(new BinaryExpr(prop("p", "nombre"), "<>", prop("c", "nombre")))),
                List.of(),
                Optional.of(new ReturnClause(List.of(
                        new ReturnItem(prop("p", "nombre"), Optional.empty()),
                        new ReturnItem(prop("c", "nombre"), Optional.empty()),
                        new ReturnItem(prop("e", "nombre"), Optional.empty())))));

        assertEquals(expected, query);
    }

    @Test
    void relationshipWithoutTargetNode_isSyntaxError() {
        assertSyntaxError("MATCH (a)-[:KNOWS]-> RETURN a");
    }

    @Test
    void unclosedBracket_isSyntaxError() {
        assertSyntaxError("MATCH (a)-[:KNOWS->(b) RETURN a");
    }

    @Test
    void mixedArrows_isSyntaxError() {
        assertSyntaxError("MATCH (a)<-[:KNOWS]->(b) RETURN a");
    }
}