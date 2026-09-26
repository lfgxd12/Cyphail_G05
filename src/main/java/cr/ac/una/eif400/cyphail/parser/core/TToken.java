package cr.ac.una.eif400.cyphail.parser.core;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

public enum TToken {
    // Palabras reservadas para Cypher,
    // preguntarle al profe si podemos usar
    // "REMOVE" como extra
    MATCH, WHERE, RETURN, AS, CREATE, DELETE, DETACH, SET, REMOVE,

    // Símbolos y operadores
    LPAREN, RPAREN, LBRACE, RBRACE, COLON, COMMA, DOT,
    GT, LT, GTE, LTE, EQ, NEQ,

    // Identificadores y Literales
    ID, NUM, STRING,

    // End of File
    EOF
}