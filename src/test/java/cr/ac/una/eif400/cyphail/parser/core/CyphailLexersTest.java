package cr.ac.una.eif400.cyphail.parser.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

public class CyphailLexersTest {

    private static InputString in(String text) {
        return new InputString(text, 0);
    }

    // Texto del token leido, o "FAIL" si el lexer no reconocio la entrada
    private static String lexed(Lexer lexer, String text) {
        return switch (lexer.parse(in(text))) {
            case Ok<InputString, TokenString, String> ok -> ok.token().value();
            case Fail<InputString, TokenString, String> fail -> "FAIL";
        };
    }

    // Posicion donde quedo la lectura, o -1 si fallo
    private static int endOf(Lexer lexer, String text) {
        return switch (lexer.parse(in(text))) {
            case Ok<InputString, TokenString, String> ok -> ok.rest().index();
            case Fail<InputString, TokenString, String> fail -> -1;
        };
    }

    @Test
    void lineAndBlockCommentsAreSkipped() {
        assertInstanceOf(Ok.class, CyphailLexers.Match().parse(in("// comentario\n /* otro\n comentario */ MATCH")));
    }

    @Test
    void keywordDoesNotMatchPrefixOfLongerWord() {
        assertInstanceOf(Fail.class, CyphailLexers.Create().parse(in("CREATED")));
    }

    @Test
    void bracesAreRecognized() {
        assertInstanceOf(Ok.class, CyphailLexers.LBrace().parse(in("  {")));
        assertInstanceOf(Ok.class, CyphailLexers.RBrace().parse(in("}")));
    }

    @Test
    void eofAcceptsOnlyBlanksAndComments() {
        assertInstanceOf(Ok.class, CyphailLexers.Eof().parse(in("   // fin\n")));
        assertInstanceOf(Fail.class, CyphailLexers.Eof().parse(in("  RETURN x")));
    }

    @Test
    void eofReportsTheUnexpectedText() {
        switch (CyphailLexers.Eof().parse(in(" , (p:Person)"))) {
            case Fail(String reason) -> assertTrue(reason.contains(", (p:Person)"));
            case Ok<InputString, TokenString, String> ok -> fail("Expected Fail but got Ok");
        }
    }

    // ---------------- P2: relaciones, SET, AND/OR/NOT y decimales ----------------

    @Test
    void bracketsAndPipeAreRecognized() {
        assertEquals("[", lexed(CyphailLexers.LBracket(), "  [r]"));
        assertEquals("]", lexed(CyphailLexers.RBracket(), "]"));
        assertEquals("|", lexed(CyphailLexers.Pipe(), " |B"));
        assertEquals("FAIL", lexed(CyphailLexers.LBracket(), "(a)"));
    }

    @Test
    void arrowIsASingleTwoCharacterToken() {
        assertEquals("->", lexed(CyphailLexers.Arrow(), "->(b)"));
        assertEquals(2, endOf(CyphailLexers.Arrow(), "->(b)"));
        // un guion solo NO es una flecha
        assertEquals("FAIL", lexed(CyphailLexers.Arrow(), "-[r]"));
    }

    @Test
    void leftArrowIsASingleTwoCharacterToken() {
        assertEquals("<-", lexed(CyphailLexers.LArrow(), "<-[r]"));
        assertEquals(2, endOf(CyphailLexers.LArrow(), "<-[r]"));
        // los comparadores no se confunden con la flecha
        assertEquals("FAIL", lexed(CyphailLexers.LArrow(), "<= 3"));
        assertEquals("FAIL", lexed(CyphailLexers.LArrow(), "<> 3"));
    }

    @Test
    void dashIsOneCharacter() {
        assertEquals("-", lexed(CyphailLexers.Dash(), "-[r]->"));
        assertEquals(1, endOf(CyphailLexers.Dash(), "-[r]->"));
        // Dash lee solo el primer guion de "->": por eso el parser prueba Arrow antes que Dash
        assertEquals(1, endOf(CyphailLexers.Dash(), "->"));
    }

    @Test
    void setAndLogicalKeywordsIgnoreCase() {
        assertEquals("SET", lexed(CyphailLexers.Set(), "SET p.x = 1"));
        assertEquals("set", lexed(CyphailLexers.Set(), "  set p.x = 1"));
        assertEquals("and", lexed(CyphailLexers.And(), "and b"));
        assertEquals("OR", lexed(CyphailLexers.Or(), "OR b"));
        assertEquals("Not", lexed(CyphailLexers.Not(), "Not (a)"));
    }

    @Test
    void logicalKeywordsDoNotMatchPrefixOfLongerWord() {
        assertEquals("FAIL", lexed(CyphailLexers.Set(), "SETTING"));
        assertEquals("FAIL", lexed(CyphailLexers.And(), "ANDROID"));
        assertEquals("FAIL", lexed(CyphailLexers.Or(), "ORDER"));
        assertEquals("FAIL", lexed(CyphailLexers.Not(), "NOTE"));
    }

    @Test
    void decimalNumberIsASingleToken() {
        assertEquals("19.99", lexed(CyphailLexers.Number(), "19.99"));
        assertEquals(5, endOf(CyphailLexers.Number(), "19.99"));
        assertEquals("9.5", lexed(CyphailLexers.Number(), " 9.5 RETURN"));
    }

    @Test
    void integersAndSignedNumbersStillWork() {
        assertEquals("2026", lexed(CyphailLexers.Number(), "2026"));
        assertEquals("-3", lexed(CyphailLexers.Number(), "-3"));
        // un guion de relacion no es un numero
        assertEquals("FAIL", lexed(CyphailLexers.Number(), "-[r]->"));
        assertEquals("FAIL", lexed(CyphailLexers.Number(), "->"));
    }

    @Test
    void numberDoesNotConsumeThePropertyDot() {
        // "p.x" no tiene digitos: no es numero; y "5.x" deja el ".x" para el parser
        assertEquals("FAIL", lexed(CyphailLexers.Number(), "p.x"));
        assertEquals("5", lexed(CyphailLexers.Number(), "5.x"));
        assertEquals(1, endOf(CyphailLexers.Number(), "5.x"));
    }
}

