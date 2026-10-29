package cr.ac.una.eif400.cyphail.broker;

import cr.ac.una.eif400.cyphail.ast.QueryNode;
import cr.ac.una.eif400.cyphail.codegen.PredicateNames;
import cr.ac.una.eif400.cyphail.codegen.PrologCodegen;
import cr.ac.una.eif400.cyphail.parser.CyphailParser;
import cr.ac.una.eif400.cyphail.parser.core.Ok;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

// Prueba el recorrido completo contra un SWI-Prolog real. Si swipl no esta instalado se omite.
class EngineBrokerTest {

    private static EngineBroker broker;

    @BeforeAll
    static void startEngine() {
        Assumptions.assumeTrue(SwiplLocator.locate().isPresent(), "SWI-Prolog is not installed");
        broker = EngineBroker.start();
    }

    @AfterAll
    static void stopEngine() {
        if (broker != null) {
            broker.close();
        }
    }

    @Test
    void answersAReadQueryOverTheSampleGraph() {
        RowsTable table = run("MATCH (p:Person) WHERE p.age > 40 RETURN p.name, p.age");

        assertEquals(List.of("p.name", "p.age"), table.columns());
        assertEquals(List.of(List.of("Jorge Soto", "43")), table.rows());
    }

    @Test
    void aQueryWithoutResultsReturnsOnlyTheHeader() {
        RowsTable table = run("MATCH (p:Person) WHERE p.age > 100 RETURN p.name");

        assertEquals(List.of("p.name"), table.columns());
        assertTrue(table.rows().isEmpty());
    }

    @Test
    void relationshipsAreTraversedInTheirDirection() {
        RowsTable table = run("MATCH (a)-[:amigo]->(b) WHERE a.name = 'Jorge Soto' RETURN b.name");

        assertEquals(List.of(List.of("Juan Perez")), table.rows());
    }

    @Test
    void anEngineErrorBecomesAnEngineException() {
        EngineException e = assertThrows(EngineException.class,
                () -> broker.run("throw(error(cy_error(prueba(1)), _))"));
        assertTrue(e.getMessage().contains("cy_error(prueba(1))"), e.getMessage());
    }

    @Test
    void aTimeoutIsReportedAndTheEngineKeepsWorking() {
        try (EngineBroker slow = EngineBroker.start(SwiplLocator.locate().orElseThrow(), 1)) {
            EngineException e = assertThrows(EngineException.class, () -> slow.run("sleep(3)"));
            assertEquals("The query exceeded the time limit", e.getMessage());
            assertEquals("ok", assertDoesNotThrowRows(slow));
        }
    }

    private static String assertDoesNotThrowRows(EngineBroker b) {
        return b.run("Rows = ok");
    }

    @Test
    void closingTwiceIsHarmlessAndRunAfterCloseFails() {
        EngineBroker other = EngineBroker.start();
        other.close();
        other.close();
        assertThrows(EngineException.class, () -> other.run("true"));
    }

    private static RowsTable run(String cyphail) {
        QueryNode query = ((Ok<?, QueryNode, String>) CyphailParser.parse(cyphail)).token();
        String goal = new PrologCodegen(new PredicateNames("amigos")).generate(query).goal();
        return RowsTable.parse(broker.run(goal));
    }
}
