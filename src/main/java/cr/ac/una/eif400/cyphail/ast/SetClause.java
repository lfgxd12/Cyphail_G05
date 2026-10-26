package cr.ac.una.eif400.cyphail.ast;

import java.util.List;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

// SET p.precio = 19.99, r.grade = 9.5
public record SetClause(List<SetItem> items) implements UpdatingClause {
    public SetClause {
        items = List.copyOf(items);
    }
}