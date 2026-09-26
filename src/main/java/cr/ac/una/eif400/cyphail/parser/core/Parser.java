package cr.ac.una.eif400.cyphail.parser.core;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

@FunctionalInterface
public interface Parser<I, T, R> {
    Result<I, T, R> parse(I source);
}