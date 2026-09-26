package cr.ac.una.eif400.cyphail.ast;

import java.util.List;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

// [DETACH] DELETE expr, expr, ...   detach = true si venia la palabra DETACH.
public record DeleteClause(boolean detach, List<Expression> items) implements UpdatingClause {
    public DeleteClause {
        items = List.copyOf(items);
    }
}