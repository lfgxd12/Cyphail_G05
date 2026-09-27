package cr.ac.una.eif400.cyphail.frontend;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplIntegrationTest {

    @Test
    void shouldRejectSyntaxErrorBeforeBroker() {
        Repl repl = new Repl();

        String output = captureOutput(() ->
                repl.runStatement("MATCH INVALID QUERY")
        );

        assertTrue(output.contains("ERROR: Syntax error."));
    }

    @Test
    void shouldRejectUndefinedVariableBeforeBroker() {
        Repl repl = new Repl();

        String statement =
                "MATCH (p:Person) WHERE x.age > 18 RETURN p.name";

        String output = captureOutput(() ->
                repl.runStatement(statement)
        );

        assertTrue(output.contains("ERROR:"));
        assertTrue(output.toLowerCase().contains("x"));
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
