package cr.ac.una.eif400.cyphail.ast;

import java.util.List;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

// CREATE pattern: reutiliza la misma regla "pattern" que MATCH.
public record CreateClause(List<NodePattern> patterns) implements UpdatingClause {
    public CreateClause {
        patterns = List.copyOf(patterns);
    }
}