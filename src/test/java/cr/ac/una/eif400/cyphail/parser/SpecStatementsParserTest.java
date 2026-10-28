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

// Los estatutos de examples/spec_p2.cyphail (sin el ";" final) y las construcciones nuevas del Paso 3:
// consulta sin MATCH, SET y decimales. El estatuto 1 (relaciones) tiene su AST completo en RelationshipParserTest.
class SpecStatementsParserTest {

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

    private static NodePattern n(String variable, List<String> labels, Property... properties) {
        return new NodePattern(Optional.of(variable), labels, List.of(properties));
    }

    private static Property p(String key, String literal) {
        return new Property(key, new LiteralExpr(literal));
    }

    private static PropertyLookup prop(String variable, String property) {
        return new PropertyLookup(new VariableExpr(variable), property);
    }

    private static ReturnItem item(Expression expression) {
        return new ReturnItem(expression, Optional.empty());
    }

    // ---------------- Estatutos del SPEC ----------------

    @Test
    void spec1_relationshipsWithWhere() {
        QueryNode query = parseOrFail("""
                MATCH (p:Persona)-[:TRABAJA_EN]->(e:Empresa),
                      (c:Persona)-[:TRABAJA_EN]->(e)
                WHERE p.nombre <> c.nombre
                RETURN p.nombre, c.nombre, e.nombre""");
        assertEquals(2, query.match().orElseThrow().patterns().size());
        assertEquals(new BinaryExpr(prop("p", "nombre"), "<>", prop("c", "nombre")),
                query.where().orElseThrow().condition());
        assertEquals(3, query.returnClause().orElseThrow().items().size());
    }

    @Test
    void spec2_createRelationshipBetweenMatchedNodes() {
        QueryNode query = parseOrFail("""
                MATCH (a:Autor {nombre: 'Bill Gates'}),
                      (b:Libro {titulo: 'Fake Windows Millenium'})
                CREATE (a)-[:ESCRIBIO]->(b)""");
        assertEquals(List.of(
                        new PathPattern(n("a", List.of("Autor"), p("nombre", "'Bill Gates'")), List.of()),
                        new PathPattern(n("b", List.of("Libro"), p("titulo", "'Fake Windows Millenium'")), List.of())),
                query.match().orElseThrow().patterns());
        RelPattern escribio = new RelPattern(Optional.empty(), List.of("ESCRIBIO"), List.of(), Direction.OUT);
        assertEquals(List.of(new CreateClause(List.of(
                        new PathPattern(n("a"), List.of(new Step(escribio, n("b"))))))),
                query.updates());
        assertTrue(query.returnClause().isEmpty());
    }

    @Test
    void spec3_createWithoutMatch() {
        QueryNode query = parseOrFail("""
                CREATE (a:Persona {nombre: 'Marta'})
                    -[r:TRABAJA_EN {rol: 'Dev'}]->(e:Empresa
                                                {nombre: 'UCR'})
                RETURN a, r, e""");
        assertTrue(query.match().isEmpty());
        assertTrue(query.where().isEmpty());
        RelPattern trabaja = new RelPattern(Optional.of("r"), List.of("TRABAJA_EN"), List.of(p("rol", "'Dev'")),
                Direction.OUT);
        assertEquals(List.of(new CreateClause(List.of(new PathPattern(
                        n("a", List.of("Persona"), p("nombre", "'Marta'")),
                        List.of(new Step(trabaja, n("e", List.of("Empresa"), p("nombre", "'UCR'")))))))),
                query.updates());
        assertEquals(List.of(item(new VariableExpr("a")), item(new VariableExpr("r")), item(new VariableExpr("e"))),
                query.returnClause().orElseThrow().items());
    }

    @Test
    void spec4_multipleLabelsAndDelete() {
        QueryNode query = parseOrFail("""
                MATCH (a:Person:Female)-[r:KNOWS]->(b:Person:Male)
                WHERE r.since < 2000
                DELETE r""");
        PathPattern path = query.match().orElseThrow().patterns().get(0);
        assertEquals(List.of("Person", "Female"), path.start().labels());
        assertEquals(List.of("Person", "Male"), path.steps().get(0).node().labels());
        assertEquals(new BinaryExpr(prop("r", "since"), "<", new LiteralExpr("2000")),
                query.where().orElseThrow().condition());
        assertEquals(List.of(new DeleteClause(false, List.of(new VariableExpr("r")))), query.updates());
        assertTrue(query.returnClause().isEmpty());
    }

    @Test
    void spec5_setWithDecimal() {
        QueryNode query = parseOrFail("""
                MATCH (p:Product {code: "PROD-99"})
                SET p.precio = 19.99
                RETURN p""");
        assertEquals(List.of(new SetClause(List.of(new SetItem(prop("p", "precio"), new LiteralExpr("19.99"))))),
                query.updates());
        assertEquals(List.of(item(new VariableExpr("p"))), query.returnClause().orElseThrow().items());
    }

    @Test
    void spec6_remove() {
        QueryNode query = parseOrFail("""
                MATCH (p:Product {code: "PROD-99"})
                REMOVE p.discount
                RETURN p.id""");
        assertEquals(List.of(new RemoveClause(List.of(prop("p", "discount")))), query.updates());
        assertEquals(List.of(item(prop("p", "id"))), query.returnClause().orElseThrow().items());
    }

    @Test
    void spec7_setOnARelationshipWrittenOnSeveralLines() {
        QueryNode query = parseOrFail("""
                MATCH
                (u:Student {id: "4-666-000"})
                -[r:attends]->
                (c:Course {nrc: "50691"})
                SET r.grade = 9.5
                RETURN u.id, r.grade""");
        RelPattern attends = new RelPattern(Optional.of("r"), List.of("attends"), List.of(), Direction.OUT);
        assertEquals(List.of(new PathPattern(
                        n("u", List.of("Student"), p("id", "\"4-666-000\"")),
                        List.of(new Step(attends, n("c", List.of("Course"), p("nrc", "\"50691\"")))))),
                query.match().orElseThrow().patterns());
        assertEquals(List.of(new SetClause(List.of(new SetItem(prop("r", "grade"), new LiteralExpr("9.5"))))),
                query.updates());
        assertEquals(List.of(item(prop("u", "id")), item(prop("r", "grade"))),
                query.returnClause().orElseThrow().items());
    }

    @Test
    void spec8_friendsExampleWithAnd() {
        QueryNode query = parseOrFail("""
                MATCH (a) -[:amigo]-> (b),
                      (b) -[:amigo]-> (a)
                WHERE a.age > 18 AND a.age = b.age
                RETURN a.name, b.name""");
        assertEquals(2, query.match().orElseThrow().patterns().size());
        assertEquals(new BinaryExpr(
                        new BinaryExpr(prop("a", "age"), ">", new LiteralExpr("18")),
                        "AND",
                        new BinaryExpr(prop("a", "age"), "=", prop("b", "age"))),
                query.where().orElseThrow().condition());
        assertEquals(List.of(item(prop("a", "name")), item(prop("b", "name"))),
                query.returnClause().orElseThrow().items());
    }

    // ---------------- SET ----------------

    @Test
    void setWithSeveralItems() {
        QueryNode query = parseOrFail("MATCH (p) SET p.a = 1, p.b = 'x', p.c = q.c RETURN p");
        assertEquals(List.of(new SetClause(List.of(
                        new SetItem(prop("p", "a"), new LiteralExpr("1")),
                        new SetItem(prop("p", "b"), new LiteralExpr("'x'")),
                        new SetItem(prop("p", "c"), prop("q", "c"))))),
                query.updates());
    }

    @Test
    void setWithNegativeAndDecimalNumbers() {
        QueryNode query = parseOrFail("MATCH (p) SET p.t = -3, p.u = -0.5");
        assertEquals(List.of(new SetClause(List.of(
                        new SetItem(prop("p", "t"), new LiteralExpr("-3")),
                        new SetItem(prop("p", "u"), new LiteralExpr("-0.5"))))),
                query.updates());
    }

    @Test
    void setKeywordIsCaseInsensitive() {
        QueryNode query = parseOrFail("MATCH (p) set p.a = 1");
        assertEquals(1, query.updates().size());
        assertInstanceOf(SetClause.class, query.updates().get(0));
    }

    @Test
    void updatesKeepTheirWrittenOrder() {
        QueryNode query = parseOrFail("MATCH (p) SET p.a = 1 REMOVE p.b DELETE p");
        assertInstanceOf(SetClause.class, query.updates().get(0));
        assertInstanceOf(RemoveClause.class, query.updates().get(1));
        assertInstanceOf(DeleteClause.class, query.updates().get(2));
    }

    // ---------------- Consulta sin MATCH ----------------

    @Test
    void createAloneIsAValidQuery() {
        QueryNode query = parseOrFail("CREATE (a:Persona {nombre: 'Marta'})");
        assertTrue(query.match().isEmpty());
        assertTrue(query.where().isEmpty());
        assertEquals(1, query.updates().size());
        assertTrue(query.returnClause().isEmpty());
    }

    @Test
    void severalUpdatesWithoutMatch() {
        QueryNode query = parseOrFail("CREATE (a:A) CREATE (b:B)");
        assertEquals(2, query.updates().size());
    }

    @Test
    void emptyInputIsSyntaxError() {
        assertSyntaxError("");
        assertSyntaxError("   \n  ");
    }

    @Test
    void returnAloneIsStillSyntaxError() {
        assertSyntaxError("RETURN 1");
    }

    @Test
    void whereWithoutMatchIsSyntaxError() {
        // El WHERE pertenece al MATCH
        assertSyntaxError("CREATE (a:A) WHERE a.x = 1");
    }

    @Test
    void matchAfterAnUpdateIsSyntaxError() {
        assertSyntaxError("CREATE (a:A) MATCH (b)");
    }

    @Test
    void errorInsideAMatchIsReportedAsThatError() {
        // Un MATCH mal escrito informa su propio problema, no "se esperaba CREATE"
        Fail<?, ?, String> failure = assertInstanceOf(Fail.class, CyphailParser.parse("MATCH (m:Movie RETURN m"));
        assertEquals("Expected ')'", failure.reason());
    }

    // ---------------- Errores de SET ----------------

    @Test
    void setWithoutEqualsIsSyntaxError() {
        assertSyntaxError("MATCH (p) SET p.a RETURN p");
    }

    @Test
    void setWithoutValueIsSyntaxError() {
        assertSyntaxError("MATCH (p) SET p.a =");
    }

    @Test
    void setOfAPlainVariableIsSyntaxError() {
        // SET p = {...} y SET p:Etiqueta no entran en P2
        assertSyntaxError("MATCH (p) SET p = 1");
    }

    @Test
    void setWithTrailingCommaIsSyntaxError() {
        assertSyntaxError("MATCH (p) SET p.a = 1,");
    }
}
