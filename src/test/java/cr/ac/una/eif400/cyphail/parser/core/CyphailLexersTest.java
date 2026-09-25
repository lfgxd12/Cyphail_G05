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
}
