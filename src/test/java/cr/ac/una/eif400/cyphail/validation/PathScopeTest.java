package cr.ac.una.eif400.cyphail.validation;

import cr.ac.una.eif400.cyphail.ast.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

/**
 * Alcance de variables en caminos con relaciones. El parser todavia no lee relaciones,
 * asi que estos AST se arman a mano: fijan el contrato del AST nuevo.
 */
class PathScopeTest {

    private static NodePattern node(String variable) {
        return new NodePattern(Optional.of(variable), List.of(), List.of());
    }

    private static RelPattern rel(String variable, List<Property> properties) {
        return new RelPattern(Optional.of(variable), List.of("amigo"), properties, Direction.OUT);
    }

    private static QueryNode match(PathPattern path, List<UpdatingClause> updates) {
        return new QueryNode(Optional.of(new MatchClause(List.of(path))), Optional.empty(), updates, Optional.empty());
    }

    private static SetClause setProperty(String variable, String property) {
        return new SetClause(List.of(new SetItem(
                new PropertyLookup(new VariableExpr(variable), property), new LiteralExpr("9.5"))));
    }

    @Test
    void relationshipVariable_isDeclaredForLaterUse() {
        var path = new PathPattern(node("a"), List.of(new Step(rel("r", List.of()), node("b"))));
        assertEquals(List.of(), VariableScopeChecker.check(match(path, List.of(setProperty("r", "grade")))));
    }

    @Test
    void undeclaredVariableInSet_isReported() {
        var path = new PathPattern(node("a"), List.of(new Step(rel("r", List.of()), node("b"))));
        assertEquals(1, VariableScopeChecker.check(match(path, List.of(setProperty("x", "grade")))).size());
    }

    @Test
    void relationshipPropertyCanUseEarlierNode_butNotALaterOne() {
        var usesEarlier = new Property("since", new PropertyLookup(new VariableExpr("a"), "year"));
        var usesLater = new Property("since", new PropertyLookup(new VariableExpr("b"), "year"));

        var ok = new PathPattern(node("a"), List.of(new Step(rel("r", List.of(usesEarlier)), node("b"))));
        var bad = new PathPattern(node("a"), List.of(new Step(rel("r", List.of(usesLater)), node("b"))));

        assertEquals(List.of(), VariableScopeChecker.check(match(ok, List.of())));
        assertEquals(1, VariableScopeChecker.check(match(bad, List.of())).size());
    }

    @Test
    void createWithoutMatch_isAccepted() {
        var path = new PathPattern(node("a"), List.of());
        var query = new QueryNode(Optional.empty(), Optional.empty(),
                List.of(new CreateClause(List.of(path))), Optional.empty());
        assertEquals(List.of(), VariableScopeChecker.check(query));
    }

    @Test
    void notExpression_isCheckedInside() {
        var path = new PathPattern(node("a"), List.of());
        var where = new WhereClause(new NotExpr(new PropertyLookup(new VariableExpr("z"), "age")));
        var query = new QueryNode(Optional.of(new MatchClause(List.of(path))), Optional.of(where),
                List.of(), Optional.empty());
        assertEquals(1, VariableScopeChecker.check(query).size());
    }
}