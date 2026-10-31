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

// Los casos de prueba unitarios del profesor para el Sprint P2 (EIF400-II-2026-Cypahil-P2-10-Casos_Prueba_Unitarios),
// con sus comentarios "//" tal como vienen, mas los literales true / false que piden los casos 8 y 10.
class ProfessorP2CasesParserTest {

    private static QueryNode parseOrFail(String query) {
        return switch (CyphailParser.parse(query)) {
            case Fail(String reason) -> fail("Query should parse, but failed: " + reason);
            case Ok(QueryNode node, var rest) -> node;
        };
    }

    private static void assertSyntaxError(String query) {
        assertInstanceOf(Fail.class, CyphailParser.parse(query), "Should be a syntax error: " + query);
    }

    private static NodePattern n(String variable, String... labels) {
        return new NodePattern(Optional.of(variable), List.of(labels), List.of());
    }

    private static NodePattern n(String variable, List<String> labels, Property... properties) {
        return new NodePattern(Optional.of(variable), labels, List.of(properties));
    }

    private static RelPattern rel(String variable, List<String> types, Direction direction, Property... properties) {
        return new RelPattern(variable == null ? Optional.empty() : Optional.of(variable), types,
                List.of(properties), direction);
    }

    private static Property prop(String key, String literal) {
        return new Property(key, new LiteralExpr(literal));
    }

    private static PropertyLookup look(String variable, String property) {
        return new PropertyLookup(new VariableExpr(variable), property);
    }

    private static BinaryExpr cmp(Expression left, String op, Expression right) {
        return new BinaryExpr(left, op, right);
    }

    private static LiteralExpr lit(String value) {
        return new LiteralExpr(value);
    }

    private static ReturnItem item(Expression expression) {
        return new ReturnItem(expression, Optional.empty());
    }

    private static PathPattern path(NodePattern start, RelPattern rel, NodePattern end) {
        return new PathPattern(start, List.of(new Step(rel, end)));
    }

    // ---------------- Casos principales ----------------

    @Test
    void case1_relationshipVariableWithoutTypeOrProperties() {
        QueryNode query = parseOrFail("""
                // Ejercita: sintaxis basica -[var]->, variable de relacion sin tipo ni propiedades.
                MATCH (p:Person)-[r]->(q:Person)
                RETURN r""");
        assertEquals(List.of(path(n("p", "Person"), rel("r", List.of(), Direction.OUT), n("q", "Person"))),
                query.match().orElseThrow().patterns());
        assertEquals(List.of(item(new VariableExpr("r"))), query.returnClause().orElseThrow().items());
    }

    @Test
    void case2_relationshipTypeWithoutVariable() {
        QueryNode query = parseOrFail("""
                // Ejercita: tipo de relacion (:TIPO), relacion sin variable.
                MATCH (a:Persona)-[:CONOCE]->(b:Persona)
                RETURN a.nombre, b.nombre""");
        assertEquals(List.of(path(n("a", "Persona"), rel(null, List.of("CONOCE"), Direction.OUT), n("b", "Persona"))),
                query.match().orElseThrow().patterns());
        assertEquals(List.of(item(look("a", "nombre")), item(look("b", "nombre"))),
                query.returnClause().orElseThrow().items());
    }

    @Test
    void case3_inverseDirection() {
        QueryNode query = parseOrFail("""
                // Ejercita: direccion inversa (<-).
                MATCH (e:Empresa)<-[r:TRABAJA_EN]-(p:Persona)
                RETURN p.nombre, e.nombre""");
        assertEquals(List.of(path(n("e", "Empresa"), rel("r", List.of("TRABAJA_EN"), Direction.IN), n("p", "Persona"))),
                query.match().orElseThrow().patterns());
    }

    @Test
    void case3_withoutDirection() {
        QueryNode query = parseOrFail("""
                // Ejercita: relacion sin direccion (-).
                MATCH (a:Persona)-[r:CONOCE]-(b:Persona)
                RETURN a.nombre, b.nombre""");
        assertEquals(List.of(path(n("a", "Persona"), rel("r", List.of("CONOCE"), Direction.NONE), n("b", "Persona"))),
                query.match().orElseThrow().patterns());
    }

    @Test
    void case4_inlinePropertiesOnTheRelationship() {
        QueryNode query = parseOrFail("""
                // Ejercita: mapa de propiedades inline en la relacion y acceso r.prop en RETURN.
                MATCH (a:Persona)-[r:CONOCE {desde: 2015}]->(b:Persona)
                RETURN a.nombre, b.nombre, r.desde""");
        assertEquals(List.of(path(n("a", "Persona"),
                        rel("r", List.of("CONOCE"), Direction.OUT, prop("desde", "2015")), n("b", "Persona"))),
                query.match().orElseThrow().patterns());
        assertEquals(look("r", "desde"), query.returnClause().orElseThrow().items().get(2).expression());
    }

    @Test
    void case5_whereOnRelationshipAndNodeProperties() {
        QueryNode query = parseOrFail("""
                // Ejercita: WHERE sobre propiedades de relacion y de nodo a la vez.
                MATCH (a:Persona)-[r:CONOCE]->(b:Persona)
                WHERE r.desde > 2010 AND b.edad >= 30
                RETURN a.nombre, b.nombre, r.desde""");
        assertEquals(cmp(cmp(look("r", "desde"), ">", lit("2010")), "AND", cmp(look("b", "edad"), ">=", lit("30"))),
                query.where().orElseThrow().condition());
    }

    @Test
    void case6_orAndNotWithInlinePropertiesOnTheSameRelation() {
        QueryNode query = parseOrFail("""
                // Ejercita: propiedades inline + WHERE sobre la misma relacion, operadores OR/NOT.
                MATCH (p:Persona)-[t:TRABAJA_EN {cargo: 'Dev'}]->(e:Empresa)
                WHERE t.desde >= 2018 OR NOT e.pais = 'CR'
                RETURN p.nombre, e.nombre, t.cargo, t.desde""");
        assertEquals(List.of(path(n("p", "Persona"),
                        rel("t", List.of("TRABAJA_EN"), Direction.OUT, prop("cargo", "'Dev'")), n("e", "Empresa"))),
                query.match().orElseThrow().patterns());
        assertEquals(cmp(cmp(look("t", "desde"), ">=", lit("2018")), "OR",
                        new NotExpr(cmp(look("e", "pais"), "=", lit("'CR'")))),
                query.where().orElseThrow().condition());
        assertEquals(4, query.returnClause().orElseThrow().items().size());
    }

    @Test
    void case7_createReusesMatchedVariables() {
        QueryNode query = parseOrFail("""
                // Ejercita: MATCH con patrones separados por coma, CREATE que reutiliza variables ya enlazadas.
                MATCH (a:Persona {nombre: 'Ana'}), (b:Persona {nombre: 'Luis'})
                CREATE (a)-[:CONOCE {desde: 2020, nivel: 'amigo'}]->(b)""");
        assertEquals(2, query.match().orElseThrow().patterns().size());
        NodePattern bareA = new NodePattern(Optional.of("a"), List.of(), List.of());
        NodePattern bareB = new NodePattern(Optional.of("b"), List.of(), List.of());
        assertEquals(List.of(new CreateClause(List.of(path(bareA,
                        rel(null, List.of("CONOCE"), Direction.OUT, prop("desde", "2020"), prop("nivel", "'amigo'")),
                        bareB)))),
                query.updates());
    }

    @Test
    void case8_createWithoutMatchAndABooleanValue() {
        QueryNode query = parseOrFail("""
                // Ejercita: CREATE sin MATCH previo, nodos y relacion a la vez, valor booleano.
                CREATE (a:Persona {nombre: 'Marta'})-[r:TRABAJA_EN {cargo: 'Dev', activo: true}]->(e:Empresa {nombre: 'Acme'})
                RETURN a, r, e""");
        assertTrue(query.match().isEmpty());
        assertEquals(List.of(new CreateClause(List.of(path(
                        n("a", List.of("Persona"), prop("nombre", "'Marta'")),
                        rel("r", List.of("TRABAJA_EN"), Direction.OUT, prop("cargo", "'Dev'"), prop("activo", "true")),
                        n("e", List.of("Empresa"), prop("nombre", "'Acme'")))))),
                query.updates());
        assertEquals(3, query.returnClause().orElseThrow().items().size());
    }

    @Test
    void case9_deleteARelationship() {
        QueryNode query = parseOrFail("""
                // Ejercita: DELETE apuntando a una relacion con WHERE.
                MATCH (a:Persona)-[r:CONOCE]->(b:Persona)
                WHERE r.desde < 2000
                DELETE r""");
        assertEquals(cmp(look("r", "desde"), "<", lit("2000")), query.where().orElseThrow().condition());
        assertEquals(List.of(new DeleteClause(false, List.of(new VariableExpr("r")))), query.updates());
        assertTrue(query.returnClause().isEmpty());
    }

    @Test
    void case10_integrator() {
        QueryNode query = parseOrFail("""
                // Ejercita (integrador): tres patrones con una sola relacion cada uno, separados por coma;
                // propiedades inline, WHERE con parentesis y AND/OR/NOT, decimales, RETURN mixto.
                MATCH (p:Persona {activo: true})-[t:TRABAJA_EN {cargo: 'Dev'}]->(e:Empresa),
                      (q:Persona)-[s:SOCIO_DE]->(e),
                      (p)-[k:CONOCE]-(q)
                WHERE (t.desde >= 2018 OR s.porcentaje > 25.5) AND NOT k.desde = 2020
                RETURN p.nombre, q.nombre, e.nombre, t.cargo, s.porcentaje, k.desde""");
        List<PathPattern> patterns = query.match().orElseThrow().patterns();
        assertEquals(3, patterns.size());
        assertEquals(path(n("p", List.of("Persona"), prop("activo", "true")),
                        rel("t", List.of("TRABAJA_EN"), Direction.OUT, prop("cargo", "'Dev'")), n("e", "Empresa")),
                patterns.get(0));
        assertEquals(path(n("q", "Persona"), rel("s", List.of("SOCIO_DE"), Direction.OUT),
                        new NodePattern(Optional.of("e"), List.of(), List.of())),
                patterns.get(1));
        assertEquals(Direction.NONE, patterns.get(2).steps().get(0).relationship().direction());
        assertEquals(cmp(cmp(cmp(look("t", "desde"), ">=", lit("2018")), "OR",
                                cmp(look("s", "porcentaje"), ">", lit("25.5"))),
                        "AND", new NotExpr(cmp(look("k", "desde"), "=", lit("2020")))),
                query.where().orElseThrow().condition());
        assertEquals(6, query.returnClause().orElseThrow().items().size());
    }

    // ---------------- Casos complementarios ----------------

    @Test
    void case8b_twoPatternsSharingAVariable() {
        QueryNode query = parseOrFail("""
                // Ejercita: dos patrones separados por coma que comparten la variable e.
                MATCH (p:Persona)-[:TRABAJA_EN]->(e:Empresa),
                      (c:Persona)-[:TRABAJA_EN]->(e)
                WHERE p.nombre <> c.nombre
                RETURN p.nombre, c.nombre, e.nombre""");
        assertEquals(2, query.match().orElseThrow().patterns().size());
        assertEquals(cmp(look("p", "nombre"), "<>", look("c", "nombre")), query.where().orElseThrow().condition());
    }

    @Test
    void case8c_disconnectedPatterns() {
        QueryNode query = parseOrFail("""
                // Ejercita: patrones desconectados, sin variables compartidas.
                MATCH (a)-[r]->(b), (c)-[s]->(d)
                RETURN r, s""");
        assertEquals(2, query.match().orElseThrow().patterns().size());
        assertEquals(List.of(item(new VariableExpr("r")), item(new VariableExpr("s"))),
                query.returnClause().orElseThrow().items());
    }

    // ---------------- Literales true / false ----------------

    @Test
    void trueAndFalseAreBooleanLiterals() {
        QueryNode query = parseOrFail("MATCH (a {activo: true, baja: false}) RETURN a");
        assertEquals(List.of(prop("activo", "true"), prop("baja", "false")),
                query.match().orElseThrow().patterns().get(0).start().properties());
    }

    @Test
    void booleansAreCaseInsensitiveAndStoredInLowercase() {
        QueryNode query = parseOrFail("MATCH (a {activo: TRUE, baja: False}) RETURN a");
        assertEquals(List.of(prop("activo", "true"), prop("baja", "false")),
                query.match().orElseThrow().patterns().get(0).start().properties());
    }

    @Test
    void booleanInAWhereComparison() {
        QueryNode query = parseOrFail("MATCH (a) WHERE a.activo = true RETURN a");
        assertEquals(cmp(look("a", "activo"), "=", lit("true")), query.where().orElseThrow().condition());
    }

    @Test
    void booleanAsTheValueOfASet() {
        QueryNode query = parseOrFail("MATCH (a) SET a.activo = false");
        assertEquals(List.of(new SetClause(List.of(new SetItem(look("a", "activo"), lit("false"))))),
                query.updates());
    }

    @Test
    void booleanIsNotAVariable() {
        assertSyntaxError("MATCH (a) WHERE true.x = 1 RETURN a");
    }

    @Test
    void aPropertyCanBeNamedTrue() {
        QueryNode query = parseOrFail("MATCH (a) RETURN a.true");
        assertEquals(List.of(item(look("a", "true"))), query.returnClause().orElseThrow().items());
    }
}
