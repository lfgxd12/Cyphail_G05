package cr.ac.una.eif400.cyphail.frontend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

class MultilineInputTest {

    @Test
    void singleLineQueryWithReturn_isComplete() {
        assertTrue(MultilineInput.isComplete("MATCH (m:Movie) RETURN m.title, m.year AS year"));
    }

    @Test
    void firstLinesOfCase1_areNotComplete() {
        assertFalse(MultilineInput.isComplete("MATCH (m:Movie)"));
        assertFalse(MultilineInput.isComplete("MATCH (m:Movie)\nRETURN m.title,"));
    }

    @Test
    void fullCase1_isComplete() {
        assertTrue(MultilineInput.isComplete("MATCH (m:Movie)\nRETURN m.title,\nm.year AS year"));
    }

    @Test
    void queryWithoutReturn_needsSemicolon() {
        assertFalse(MultilineInput.isComplete("MATCH (p:Person) DELETE p"));
        assertTrue(MultilineInput.isComplete("MATCH (p:Person) DELETE p;"));
    }

    @Test
    void treeCommand_waitsForItsQuery() {
        assertFalse(MultilineInput.isComplete(".tree"));
        assertFalse(MultilineInput.isComplete(".tree\nMATCH (m:Movie)"));
        assertTrue(MultilineInput.isComplete(".tree\nMATCH (m:Movie)\nRETURN m.title"));
    }

    @Test
    void clean_removesFinalSemicolon() {
        assertEquals("MATCH (p:Person) DELETE p", MultilineInput.clean("  MATCH (p:Person) DELETE p ;  "));
    }

    @Test
    void treeCommandIsRecognizedAndRemoved() {
        assertTrue(MultilineInput.isTreeCommand(".TREE MATCH (m) RETURN m"));
        assertFalse(MultilineInput.isTreeCommand(".treeX"));
        assertEquals("MATCH (m:Movie)\nRETURN m", MultilineInput.withoutTreeCommand(".tree\nMATCH (m:Movie)\nRETURN m"));
    }
}