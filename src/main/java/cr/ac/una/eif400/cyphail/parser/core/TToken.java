package cr.ac.una.eif400.cyphail.parser.core;

public enum TToken {
    // Palabras reservadas para Cypher,
    // preguntarle al profe si podemos usar
    // "REMOVE" como extra
    MATCH, WHERE, RETURN, AS, CREATE, DELETE, DETACH, SET, REMOVE,

    // Símbolos y operadores
    LPAREN, RPAREN, LBRACE, RBRACE, COLON, COMMA, DOT,
    GT, LT, GTE, LTE, EQ, NEQ,

    // Identificadores y Literales
    ID, NUM, STRING
}