package cr.ac.una.eif400.cyphail.ast;

import java.util.List;
import java.util.Optional;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

// Una consulta: MATCH [WHERE] (CREATE | DELETE | REMOVE)* [RETURN]
public record QueryNode(
        MatchClause match,
        Optional<WhereClause> where,
        List<UpdatingClause> updates,
        Optional<ReturnClause> returnClause
) implements Statement {
    public QueryNode {
        updates = List.copyOf(updates);
    }
}