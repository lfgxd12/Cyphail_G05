package cr.ac.una.eif400.cyphail.parser.core;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

@SuppressWarnings("unused")
public sealed interface Result<I, T, R> permits Ok, Fail {
}