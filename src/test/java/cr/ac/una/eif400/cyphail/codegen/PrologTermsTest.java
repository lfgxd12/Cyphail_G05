package cr.ac.una.eif400.cyphail.codegen;

import cr.ac.una.eif400.cyphail.ast.LiteralExpr;
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

class PrologTermsTest {

    @Test
    void plainAtomsGoWithoutQuotes() {
        assertEquals("amigo", PrologTerms.atom("amigo"));
        assertEquals("age", PrologTerms.atom("age"));
        assertEquals("trabaja_en", PrologTerms.atom("trabaja_en"));
    }

    @Test
    void atomsThatNeedItAreQuoted() {
        assertEquals("'Person'", PrologTerms.atom("Person"));
        assertEquals("'Juan Perez'", PrologTerms.atom("Juan Perez"));
        assertEquals("'4-666-000'", PrologTerms.atom("4-666-000"));
        assertEquals("'50691'", PrologTerms.atom("50691"));
        assertEquals("''", PrologTerms.atom(""));
    }

    @Test
    void quotesBackslashesAndLineBreaksAreEscaped() {
        assertEquals("'it\\'s'", PrologTerms.atom("it's"));
        assertEquals("'a\\\\b'", PrologTerms.atom("a\\b"));
        assertEquals("'line1\\nline2'", PrologTerms.atom("line1\nline2"));
        assertEquals("'col1\\tcol2'", PrologTerms.atom("col1\tcol2"));
    }

    @Test
    void doubleQuotesNeedNoEscapeInsideAnAtom() {
        assertEquals("'Ana \"la\" Perez'", PrologTerms.atom("Ana \"la\" Perez"));
    }

    @Test
    void numbersLoseTheLeadingPlus() {
        assertEquals("5", PrologTerms.number("+5"));
        assertEquals("-3", PrologTerms.number("-3"));
        assertEquals("19.99", PrologTerms.number("19.99"));
    }

    @Test
    void literalsKeepTheirKind() {
        assertEquals("19.99", PrologTerms.literal(new LiteralExpr("19.99")));
        assertEquals("adult", PrologTerms.literal(new LiteralExpr("\"adult\"")));
        assertEquals("'Marta'", PrologTerms.literal(new LiteralExpr("'Marta'")));
        assertEquals("'4-666-000'", PrologTerms.literal(new LiteralExpr("\"4-666-000\"")));
    }

    @Test
    void atomListAlwaysQuotesLikeTheSpecExample() {
        assertEquals("['a.name', 'b.name']", PrologTerms.atomList(List.of("a.name", "b.name")));
        assertEquals("['a']", PrologTerms.atomList(List.of("a")));
        assertEquals("[]", PrologTerms.atomList(List.of()));
    }
}