package cr.ac.una.eif400.cyphail.ast;

import java.util.List;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

public record RemoveClause(List<PropertyLookup> items) implements UpdatingClause {
    public RemoveClause {
        items = List.copyOf(items);
    }
}