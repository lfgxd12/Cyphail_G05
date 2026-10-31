package cr.ac.una.eif400.cyphail.frontend;

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

// El comportamiento de entrada del REPL (comandos, multilinea, ";") sin consola ni engine
class ReplStepTest {

    private static ReplStep.Result step(ReplStep.State state, String line) {
        return ReplStep.step(state, line);
    }

    @Test
    void aCommandRunsImmediately() {
        var result = step(ReplStep.State.EMPTY, ".use amigos");
        assertEquals(List.of(new ReplStep.Command(".use amigos")), result.requests());
        assertEquals(ReplStep.State.EMPTY, result.next());
        assertFalse(result.exit());
    }

    @Test
    void exitIsFlagged() {
        assertTrue(step(ReplStep.State.EMPTY, ".EXIT").exit());
    }

    @Test
    void aBlankLineDoesNothingWhenNothingIsPending() {
        var result = step(ReplStep.State.EMPTY, "   ");
        assertTrue(result.requests().isEmpty());
        assertEquals(ReplStep.State.EMPTY, result.next());
    }

    @Test
    void aStatementWithoutSemicolonWaitsForMoreLines() {
        var first = step(ReplStep.State.EMPTY, "CREATE (a:Persona {nombre: 'Marta'})");
        assertTrue(first.requests().isEmpty());
        assertEquals("CREATE (a:Persona {nombre: 'Marta'})", first.next().pending());

        var second = step(first.next(), "  -[:TRABAJA_EN]->(e:Empresa);");
        assertEquals(List.of(new ReplStep.Statement(
                "CREATE (a:Persona {nombre: 'Marta'})\n-[:TRABAJA_EN]->(e:Empresa)")), second.requests());
        assertEquals(ReplStep.State.EMPTY, second.next());
    }

    @Test
    void aBlankLineRunsWhatIsPending() {
        var pending = new ReplStep.State("CREATE (a)");
        var result = step(pending, "");
        assertEquals(List.of(new ReplStep.Statement("CREATE (a)")), result.requests());
        assertEquals(ReplStep.State.EMPTY, result.next());
    }

    @Test
    void aQueryEndingInReturnRunsWithoutSemicolon() {
        var result = step(ReplStep.State.EMPTY, "MATCH (p:Person) RETURN p.name");
        assertEquals(List.of(new ReplStep.Statement("MATCH (p:Person) RETURN p.name")), result.requests());
    }

    @Test
    void severalStatementsInOneLineAllRun() {
        var result = step(ReplStep.State.EMPTY, "CREATE (a); CREATE (b); MATCH (c)");
        assertEquals(List.of(new ReplStep.Statement("CREATE (a)"), new ReplStep.Statement("CREATE (b)")),
                result.requests());
        assertEquals("MATCH (c)", result.next().pending());
    }

    @Test
    void aCommandInsideAPendingStatementIsPartOfIt() {
        var result = step(new ReplStep.State("MATCH (a)"), ".use algo");
        assertTrue(result.requests().isEmpty());
        assertEquals("MATCH (a)\n.use algo", result.next().pending());
    }

    @Test
    void treeIsAStatementNotACommand() {
        var result = step(ReplStep.State.EMPTY, ".tree MATCH (a) RETURN a;");
        assertEquals(List.of(new ReplStep.Statement(".tree MATCH (a) RETURN a")), result.requests());
    }

    @Test
    void stepDoesNotModifyTheStateItReceives() {
        var state = new ReplStep.State("MATCH (a)");
        step(state, "RETURN a;");
        assertEquals("MATCH (a)", state.pending());
    }
}
