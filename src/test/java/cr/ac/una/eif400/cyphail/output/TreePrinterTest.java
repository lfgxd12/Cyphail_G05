package cr.ac.una.eif400.cyphail.output;

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

class TreePrinterTest {

    private static QueryNode parseOrFail(String query) {
        return switch (CyphailParser.parse(query)) {
            case Fail(String reason) -> fail("Query should parse, but failed: " + reason);
            case Ok(QueryNode node, var rest) -> node;
        };
    }

    @Test
    void specExample_isPrintedInSpecFormat() {
        QueryNode query = parseOrFail("""
                MATCH (m:Movie)
                WHERE m.year > 1990
                RETURN m.title AS title,
                       m.year AS year
                """);

        String expected = """
                Query{
                  Match: {
                    Patterns: [
                      PatternNode: {
                        var: m
                        labels: [ Movie ]
                        properties: []
                      }
                    ]
                  }
                  Where: {
                    Expr: (> (. m year) 1990)
                  }
                  Updates: []
                  Return: {
                    Projection: {
                      Items: [
                        {as (. m title) title}
                        {as (. m year) year}
                      ]
                      Modifiers: []
                    }
                  }
                }""";

        assertEquals(expected, TreePrinter.print(query));
    }

    @Test
    void updatesAndPropertiesArePrinted() {
        QueryNode query = parseOrFail("MATCH (p:Person), (o:Order {personId: p.id}) DELETE o");
        String tree = TreePrinter.print(query);

        assertTrue(tree.contains("properties: [ {personId: (. p id)} ]"));
        assertTrue(tree.contains("Delete: {"));
        assertFalse(tree.contains("Return:"), "Sin RETURN no debe imprimirse esa seccion");
    }

    @Test
    void expressionsArePrintedInPreorder() {
        QueryNode query = parseOrFail("MATCH (m:Movie), (p:Person) WHERE m.year <> p.age RETURN m");
        assertTrue(TreePrinter.print(query).contains("Expr: (<> (. m year) (. p age))"));
    }

    @Test
    void pathWithRelationship_isPrintedWithItsDirection() {
        var a = new NodePattern(Optional.of("a"), List.of("Person"), List.of());
        var b = new NodePattern(Optional.of("b"), List.of("Person"), List.of());
        var rel = new RelPattern(Optional.of("r"), List.of("amigo"),
                List.of(new Property("since", new LiteralExpr("2000"))), Direction.OUT);
        var path = new PathPattern(a, List.of(new Step(rel, b)));
        var query = new QueryNode(Optional.of(new MatchClause(List.of(path))), Optional.empty(),
                List.of(), Optional.empty());

        String expected = """
                Query{
                  Match: {
                    Patterns: [
                      Path: {
                        PatternNode: {
                          var: a
                          labels: [ Person ]
                          properties: []
                        }
                        Steps: [
                          Step: {
                            RelPattern: {
                              var: r
                              types: [ amigo ]
                              properties: [ {since: 2000} ]
                              direction: OUT
                            }
                            PatternNode: {
                              var: b
                              labels: [ Person ]
                              properties: []
                            }
                          }
                        ]
                      }
                    ]
                  }
                  Updates: []
                }""";

        assertEquals(expected, TreePrinter.print(query));
    }

    @Test
    void setAndNot_arePrintedInPreorder() {
        var node = new PathPattern(new NodePattern(Optional.of("p"), List.of(), List.of()), List.of());
        var where = new WhereClause(new NotExpr(new BinaryExpr(
                new PropertyLookup(new VariableExpr("p"), "age"), "<", new LiteralExpr("18"))));
        var set = new SetClause(List.of(new SetItem(
                new PropertyLookup(new VariableExpr("p"), "precio"), new LiteralExpr("19.99"))));
        var query = new QueryNode(Optional.of(new MatchClause(List.of(node))), Optional.of(where),
                List.of(set), Optional.empty());

        String tree = TreePrinter.print(query);
        assertTrue(tree.contains("Expr: (NOT (< (. p age) 18))"));
        assertTrue(tree.contains("(= (. p precio) 19.99)"));
    }

    @Test
    void createWithoutMatch_hasNoMatchSection() {
        var path = new PathPattern(new NodePattern(Optional.of("a"), List.of("Persona"), List.of()), List.of());
        var query = new QueryNode(Optional.empty(), Optional.empty(),
                List.of(new CreateClause(List.of(path))), Optional.empty());

        assertFalse(TreePrinter.print(query).contains("Match:"));
        assertTrue(TreePrinter.print(query).contains("Create: {"));
    }
}