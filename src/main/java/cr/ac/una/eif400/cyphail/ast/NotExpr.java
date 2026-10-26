package cr.ac.una.eif400.cyphail.ast;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

// NOT expresion. AND y OR reutilizan BinaryExpr con operador "AND" / "OR".
public record NotExpr(Expression operand) implements Expression { }