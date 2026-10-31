package cr.ac.una.eif400.cyphail.frontend;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

import cr.ac.una.eif400.cyphail.broker.EngineException;
import cr.ac.una.eif400.cyphail.broker.GoalRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ReplIntegrationTest {

    @TempDir
    Path dir;

    // Engine falso: guarda los goals que recibe y contesta con el Rows que se le indique
    private static final class FakeEngine implements GoalRunner {
        final List<String> goals = new ArrayList<>();
        String rows = "";
        RuntimeException failure;

        @Override
        public String run(String goal) {
            goals.add(goal);
            if (failure != null) {
                throw failure;
            }
            return goal.contains("cy_db(G)") ? "amigos otro" : rows;
        }
    }

    @Test
    void shouldRejectSyntaxErrorBeforeBroker() {
        FakeEngine engine = new FakeEngine();

        String output = captureOutput(() -> new Repl(() -> engine).runStatement("MATCH INVALID QUERY"));

        assertTrue(output.contains("ERROR: Syntax error."));
        assertTrue(engine.goals.isEmpty());
    }

    @Test
    void shouldRejectUndefinedVariableBeforeBroker() {
        FakeEngine engine = new FakeEngine();

        String output = captureOutput(() ->
                new Repl(() -> engine).runStatement("MATCH (p:Person) WHERE x.age > 18 RETURN p.name"));

        assertTrue(output.contains("ERROR:"));
        assertTrue(output.toLowerCase().contains("x"));
        assertTrue(engine.goals.isEmpty());
    }

    @Test
    void shouldRejectARepeatedRelationshipVariableBeforeBroker() {
        FakeEngine engine = new FakeEngine();

        String output = captureOutput(() ->
                new Repl(() -> engine).runStatement("MATCH (a)-[r:amigo]->(b)-[r:amigo]->(c) RETURN a"));

        assertTrue(output.contains("cannot be repeated"), output);
        assertTrue(engine.goals.isEmpty());
    }

    @Test
    void aValidQueryReachesTheEngineAndPrintsTheTable() {
        FakeEngine engine = new FakeEngine();
        engine.rows = "p.name\n\"Jorge Soto\"";

        String output = captureOutput(() ->
                new Repl(() -> engine).runStatement("MATCH (p:Person) WHERE p.age > 40 RETURN p.name;"));

        assertEquals(1, engine.goals.size());
        assertTrue(engine.goals.get(0).contains("user_amigos_label(__P, 'Person')"));
        assertTrue(output.contains("Jorge Soto"), output);
        assertTrue(output.contains("OK. 1 row(s)"), output);
    }

    @Test
    void aWriteQueryWithoutReturnOnlyReportsSuccess() {
        FakeEngine engine = new FakeEngine();

        String output = captureOutput(() ->
                new Repl(() -> engine).runStatement("CREATE (:Planeta {nombre: 'Marte'})"));

        assertTrue(output.contains("OK. Query executed"), output);
    }

    @Test
    void anEngineFailureIsShownAsAnErrorLine() {
        FakeEngine engine = new FakeEngine();
        engine.failure = new EngineException("The query exceeded the time limit");

        String output = captureOutput(() ->
                new Repl(() -> engine).runStatement("MATCH (p:Person) RETURN p.name"));

        assertTrue(output.contains("ERROR: The query exceeded the time limit"), output);
    }

    @Test
    void anUnsupportedFeatureIsShownAsAnErrorLine() {
        FakeEngine engine = new FakeEngine();

        String output = captureOutput(() ->
                new Repl(() -> engine).runStatement("MATCH (a) DELETE a.name"));

        assertTrue(output.contains("ERROR:"), output);
        assertTrue(engine.goals.isEmpty());
    }

    @Test
    void theEngineStartsOnlyWhenTheFirstQueryNeedsIt() {
        AtomicInteger starts = new AtomicInteger();
        FakeEngine engine = new FakeEngine();
        Repl repl = new Repl(() -> {
            starts.incrementAndGet();
            return engine;
        });

        captureOutput(() -> {
            repl.runStatement("MATCH INVALID");
            repl.runStatement(".tree MATCH (p) RETURN p");
        });
        assertEquals(0, starts.get());

        captureOutput(() -> {
            repl.runStatement("MATCH (p) RETURN p");
            repl.runStatement("MATCH (p) RETURN p");
        });
        assertEquals(1, starts.get());
    }

    @Test
    void useSwitchesTheGraphTheNextQueriesRunOn() {
        FakeEngine engine = new FakeEngine();
        Repl repl = new Repl(() -> engine);

        String output = captureOutput(() -> {
            repl.runStatement("MATCH (p) RETURN p");
            repl.runCommand(".use otro");
        });
        captureOutput(() -> repl.runStatement("MATCH (p) RETURN p"));

        assertTrue(output.contains("\"otro\" graph available"), output);
        assertTrue(engine.goals.get(0).contains("user_amigos_node(__P)"));
        assertTrue(engine.goals.get(engine.goals.size() - 1).contains("user_otro_node(__P)"));
    }

    @Test
    void useWithAnUnknownGraphReportsAnError() {
        FakeEngine engine = new FakeEngine();

        String output = captureOutput(() -> new Repl(() -> engine).runCommand(".use nada"));

        assertTrue(output.contains("ERROR: Graph \"nada\" not found."), output);
    }

    @Test
    void loadRunsEveryStatementOfTheFileInOrder() throws IOException {
        Path script = Files.writeString(dir.resolve("demo.cyphail"),
                "// demo\nCREATE (:Planeta {nombre: 'Marte'});\nMATCH (p:Planeta)\nRETURN p.nombre;\n");
        FakeEngine engine = new FakeEngine();
        engine.rows = "p.nombre\n\"Marte\"";

        String output = captureOutput(() -> new Repl(() -> engine).runCommand(".load \"" + script + "\""));

        assertEquals(2, engine.goals.size());
        assertTrue(engine.goals.get(0).contains("cy_create_node"));
        assertTrue(output.contains("[1/2]") && output.contains("[2/2]"), output);
        assertTrue(output.contains("OK. 2 statement(s) executed."), output);
    }

    @Test
    void loadStopsAtTheFirstStatementThatFails() throws IOException {
        Path script = Files.writeString(dir.resolve("roto.cyphail"),
                "MATCH (a) RETURN a;\nMATCH INVALID;\nMATCH (b) RETURN b;\n");
        FakeEngine engine = new FakeEngine();

        String output = captureOutput(() -> new Repl(() -> engine).runCommand(".load " + script));

        assertEquals(1, engine.goals.size());
        assertTrue(output.contains("Syntax error"), output);
        assertTrue(output.contains(".load stopped at statement 2 of 3."), output);
    }

    @Test
    void loadReportsAMissingFile() {
        String output = captureOutput(() ->
                new Repl(() -> new FakeEngine()).runCommand(".load \"" + dir.resolve("nada.cyphail") + "\""));

        assertTrue(output.contains("ERROR: File not found:"), output);
    }

    @Test
    void loadWithoutAPathShowsTheUsage() {
        String output = captureOutput(() -> new Repl(() -> new FakeEngine()).runCommand(".load"));

        assertTrue(output.contains("Usage: .load"), output);
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
