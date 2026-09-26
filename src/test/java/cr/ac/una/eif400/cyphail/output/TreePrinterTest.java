package cr.ac.una.eif400.cyphail.output;

import cr.ac.una.eif400.cyphail.ast.QueryNode;
import cr.ac.una.eif400.cyphail.parser.CyphailParser;
import cr.ac.una.eif400.cyphail.parser.core.Fail;
import cr.ac.una.eif400.cyphail.parser.core.Ok;
import org.junit.jupiter.api.Test;

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
}