package cr.ac.una.eif400.cyphail.ast;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

class LiteralExprTest {

    @Test
    void quotedLiteralsAreStrings() {
        assertTrue(new LiteralExpr("\"adult\"").isString());
        assertTrue(new LiteralExpr("'Marta'").isString());
        assertEquals("adult", new LiteralExpr("\"adult\"").text());
        assertEquals("Marta", new LiteralExpr("'Marta'").text());
    }

    @Test
    void unquotedLiteralsAreNumbers() {
        assertTrue(new LiteralExpr("2026").isNumber());
        assertTrue(new LiteralExpr("19.99").isNumber());
        assertFalse(new LiteralExpr("19.99").isString());
        assertEquals("19.99", new LiteralExpr("19.99").text());
    }
}