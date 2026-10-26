package cr.ac.una.eif400.cyphail.ast;

import java.util.List;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

// Un camino: (a) o (a)-[r]->(b)-[s]->(c). Un nodo suelto es un camino sin pasos.
public record PathPattern(NodePattern start, List<Step> steps) {
    public PathPattern {
        steps = List.copyOf(steps);
    }
}