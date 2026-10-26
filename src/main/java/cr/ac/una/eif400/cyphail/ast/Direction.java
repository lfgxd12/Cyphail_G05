package cr.ac.una.eif400.cyphail.ast;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

// Direccion de una relacion: (a)-[r]->(b) es OUT, (a)<-[r]-(b) es IN y (a)-[r]-(b) es NONE.
public enum Direction { OUT, IN, NONE }