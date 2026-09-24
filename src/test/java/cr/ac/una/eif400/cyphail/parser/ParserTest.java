package cr.ac.una.eif400.cyphail.parser;

import cr.ac.una.eif400.cyphail.ast.QueryNode;
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
 *
 * Verifica que los casos de prueba de referencia del profesor (Casos 1-11,
 * SPEC-Sprint-P1-Casos de Prueba) se parseen a un QueryNode correcto.
 *
 * NOTA DE EQUIPO: al momento de escribir esta suite, CyphailParser solo
 * soporta un unico patron de nodo por MATCH, sin propiedades ni multiples
 * etiquetas, y no implementa CREATE ni DELETE. Los casos que dependen de
 * esas caracteristicas quedan marcados @Disabled con el motivo especifico.
 */
class ParserTest {

    @Test
    void case1_singlePatternWithAlias() {
        String query = """
                MATCH (m:Movie)
                RETURN m.title,
                       m.year AS year
                """;

        switch (CyphailParser.parse(query)) {
            case Fail(String reason) -> fail("Case 1 should parse, but failed: " + reason);
            case Ok(QueryNode node, var rest) -> {
                assertEquals("m", node.match().variable());
                assertEquals("Movie", node.match().label());
                assertTrue(node.returnClause().isPresent());
                assertEquals(2, node.returnClause().get().items().size());
            }
        }
    }

    @Test
    void case2_patternWhereComparisonAlias() {
        String query = """
                MATCH (b:Book)
                WHERE b.pages < 300
                RETURN b.title,
                       b.pages AS totalPages
                """;

        switch (CyphailParser.parse(query)) {
            case Fail(String reason) -> fail("Case 2 should parse, but failed: " + reason);
            case Ok(QueryNode node, var rest) -> {
                assertTrue(node.where().isPresent());
                assertTrue(node.returnClause().isPresent());
                assertEquals(2, node.returnClause().get().items().size());
            }
        }
    }

    @Test
    @Disabled("Requiere multiples etiquetas y propiedades en nodePattern (pendiente en CyphailParser)")
    void case3_multipleLabelsWithProperty() {
        // MATCH (a:Person:Employee {id: 1}) WHERE a.age > 30 RETURN a.name AS name, a.age AS age
    }

    @Test
    @Disabled("Requiere multiples patrones separados por coma en MATCH (pendiente en CyphailParser)")
    void case4_doublePatternDisconnected() {
        // MATCH (m:Movie), (p:Person) WHERE m.year > 2000 RETURN m.title AS title, p.name AS actor
    }

    @Test
    @Disabled("Requiere multiples patrones separados por coma en MATCH (pendiente en CyphailParser)")
    void case5_doublePatternDifferentAttributes() {
        // MATCH (m:Movie), (p:Person) WHERE m.year <> p.age RETURN m.title AS title, p.name AS name
    }

    @Test
    @Disabled("Requiere multiples patrones y propiedades en MATCH (pendiente en CyphailParser)")
    void case6_doublePatternEachWithProperty() {
        // MATCH (m:Movie {year: 1999}), (p:Person {age: 40}) WHERE m.year <> p.age RETURN m.title AS title, p.name AS name
    }

    @Test
    @Disabled("Requiere la clausula CREATE (pendiente en CyphailParser)")
    void case7_matchAndCreate() {
        // MATCH (p:Person) WHERE p.age > 18 CREATE (c:Certificate {issuedTo: "adult", year: 2026}) RETURN p.name AS name, p.age AS age
    }

    @Test
    @Disabled("Requiere multiples patrones y propiedades cuyo valor es p.id (pendiente en CyphailParser)")
    void case8_propertyExpressionAsValue() {
        // MATCH (p:Person {id: 1}), (o:Order {personId: p.id}) RETURN p.name AS name, o.total AS total
    }

    @Test
    @Disabled("Requiere multiples patrones, propiedades y CREATE/DELETE (pendiente en CyphailParser)")
    void case9_matchCreateDelete() {
        // MATCH (p:Person), (o:Order {personId: p.id, status: "cancelled"}) WHERE p.age > 60
        // CREATE (a:Archive {id: o.id, name: "retired", year: 2026}) DELETE o RETURN p.name AS name
    }

    @Test
    @Disabled("Requiere multiples patrones en MATCH (pendiente en CyphailParser); ver " +
            "ValidationTest.undefinedVariableInWhere_throwsSemanticException para la version " +
            "equivalente con la gramatica actualmente soportada")
    void case10_undefinedVariable() {
        // MATCH (p:Person), (o:Order {personId: p.id, status: "cancelled"}) WHERE q.age > 60 RETURN q AS name
    }

    @Test
    @Disabled("Requiere multiples patrones, CREATE y DELETE (pendiente en CyphailParser)")
    void case11_undefinedVariableReversedOrder() {
        // MATCH (o:Order {personId: p.id, status: "cancelled"}), (p:Person) WHERE p.age > 60
        // CREATE (a:Archive {id: o.id, name: "retired", year: 2026}) DELETE o RETURN p.name AS name
    }

    @Test
    void removeClause_singlePropertyIsParsed() {
        String query = "MATCH (p:Person) REMOVE p.age";

        switch (CyphailParser.parse(query)) {
            case Fail(String reason) -> fail("REMOVE clause should parse, but failed: " + reason);
            case Ok(QueryNode node, var rest) -> {
                assertTrue(node.remove().isPresent());
                assertEquals(1, node.remove().get().items().size());
                assertEquals("age", node.remove().get().items().get(0).property());
            }
        }
    }
}
