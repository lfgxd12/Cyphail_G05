package cr.ac.una.eif400.cyphail.engine;

import cr.ac.una.eif400.cyphail.ast.QueryNode;
import cr.ac.una.eif400.cyphail.parser.CyphailParser;
import cr.ac.una.eif400.cyphail.parser.core.Ok;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonFakeBrokerTest {

    @Test
    void shouldPrintTableForValidReturnQuery() {
        String statement =
                "MATCH (m:Movie) RETURN m.title, m.year AS year";

        QueryNode query = parseQuery(statement);

        String output = captureOutput(() ->
                JsonFakeBroker.resolve(query, statement)
        );

        assertTrue(output.contains("Matrix"));
        assertTrue(output.contains("Inception"));
        assertTrue(output.contains("1999"));
        assertTrue(output.contains("2010"));
    }

    @Test
    void shouldPrintConfirmationForCreateQuery() {
        String statement =
                "MATCH (p:Person) WHERE p.age > 18 " +
                "CREATE (c:Certificate {issuedTo: \"adult\", year: 2026}) " +
                "RETURN p.name AS name, p.age AS age";

        QueryNode query = parseQuery(statement);

        String output = captureOutput(() ->
                JsonFakeBroker.resolve(query, statement)
        );

        assertTrue(output.contains("OK. 1 node created."));
    }

    private QueryNode parseQuery(String statement) {
        return switch (CyphailParser.parse(statement)) {
            case Ok(QueryNode query, var rest) -> query;
            default -> throw new AssertionError(
                    "The test query should be valid."
            );
        };
    }

    private String captureOutput(Runnable action) {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try {
            System.setOut(new PrintStream(output));
            action.run();
        } finally {
            System.setOut(originalOut);
        }

        return output.toString();
    }
}