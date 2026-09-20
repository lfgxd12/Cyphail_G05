package cr.ac.una.eif400.cyphail.ast;

import java.util.Optional;

public record QueryNode(
        MatchClause match,
        Optional<WhereClause> where,
        Optional<ReturnClause> returnClause,
        Optional<RemoveClause> remove
) implements Statement { }
