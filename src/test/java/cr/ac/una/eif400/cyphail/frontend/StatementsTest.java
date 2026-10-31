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

class StatementsTest {

    @Test
    void splitsOnEachSemicolon() {
        var split = Statements.split("MATCH (a) RETURN a; MATCH (b) RETURN b;");
        assertEquals(List.of("MATCH (a) RETURN a", "MATCH (b) RETURN b"), split.complete());
        assertEquals("", split.rest());
    }

    @Test
    void whatComesAfterTheLastSemicolonIsTheRest() {
        var split = Statements.split("CREATE (a); MATCH (b)\nWHERE");
        assertEquals(List.of("CREATE (a)"), split.complete());
        assertEquals("MATCH (b)\nWHERE", split.rest());
    }

    @Test
    void textWithoutSemicolonIsAllRest() {
        var split = Statements.split("MATCH (a)");
        assertTrue(split.complete().isEmpty());
        assertEquals("MATCH (a)", split.rest());
    }

    @Test
    void aSemicolonInsideAStringDoesNotEndTheStatement() {
        assertEquals(List.of("CREATE (:A {t: 'uno;dos'})", "CREATE (:B {t: \"x;y\"})"),
                Statements.split("CREATE (:A {t: 'uno;dos'}); CREATE (:B {t: \"x;y\"});").complete());
    }

    @Test
    void anEscapedQuoteDoesNotCloseTheString() {
        assertEquals(List.of("CREATE (:A {t: 'it\\'s; ok'})"),
                Statements.split("CREATE (:A {t: 'it\\'s; ok'});").complete());
    }

    @Test
    void aSemicolonInsideACommentDoesNotEndTheStatement() {
        var split = Statements.split("// uno; dos\nMATCH (a) /* x; y */ RETURN a;");
        assertEquals(1, split.complete().size());
        assertTrue(split.complete().get(0).contains("MATCH (a)"));
    }

    @Test
    void emptyStatementsAndCommentOnlyTextAreDropped() {
        assertTrue(Statements.split(";;  ;").complete().isEmpty());
        var split = Statements.split("// solo un comentario\n");
        assertTrue(split.complete().isEmpty());
        assertEquals("", split.rest());
    }

    @Test
    void allIncludesTheTailWithoutSemicolon() {
        assertEquals(List.of("CREATE (a)", "MATCH (a) RETURN a"),
                Statements.all("CREATE (a);\nMATCH (a) RETURN a"));
    }

    @Test
    void anUnclosedStringKeepsEverythingAsRest() {
        var split = Statements.split("CREATE (:A {t: 'abre; y no cierra");
        assertTrue(split.complete().isEmpty());
        assertTrue(split.rest().startsWith("CREATE"));
    }
}
