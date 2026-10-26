package cr.ac.una.eif400.cyphail.ast;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

// Un paso de un camino: una relacion y el nodo al que llega.
public record Step(RelPattern relationship, NodePattern node) { }