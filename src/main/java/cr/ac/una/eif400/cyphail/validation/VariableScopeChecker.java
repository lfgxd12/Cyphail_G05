package cr.ac.una.eif400.cyphail.validation;

import cr.ac.una.eif400.cyphail.ast.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 *          Jostin Jimenez Alfaro, Angel Rojas Ruano
 *
 * Verifica el ambito (scope) de las variables de una consulta: toda variable
 * usada en WHERE, RETURN o REMOVE debe haber sido declarada previamente en
 * un patron MATCH. No confunde una propiedad (m.title) con una variable no
 * definida: solo revisa el nombre de la variable, no el nombre de la propiedad.
 */
public class VariableScopeChecker {

    /**
     * @throws SemanticException si alguna variable referenciada no fue
     *                           declarada en el patron MATCH de la consulta.
     */
    public static void validate(QueryNode query) {
        Set<String> declared = declaredVariables(query.match());

        query.where().ifPresent(w -> checkExpression(w.condition(), declared));

        query.returnClause().ifPresent(r ->
                r.items().forEach(item -> checkExpression(item.expression(), declared)));

        query.remove().ifPresent(remove ->
                remove.items().forEach(lookup ->
                        checkVariable(lookup.variable().name(), declared, "REMOVE clause")));
    }


    private static Set<String> declaredVariables(MatchClause match) {
        Set<String> declared = new HashSet<>();
        declared.add(match.variable());
        return declared;
    }

    private static void checkExpression(Expression expr, Set<String> declared) {
        switch (expr) {
            case VariableExpr(String name) ->
                    checkVariable(name, declared, "expression");
            case PropertyLookup(VariableExpr variable, String property) ->
                    checkVariable(variable.name(), declared, "property access ('" + property + "')");
            case BinaryExpr(Expression left, String operator, Expression right) -> {
                checkExpression(left, declared);
                checkExpression(right, declared);
            }
            case LiteralExpr ignored -> {
                // Un literal (numero, string) no referencia ninguna variable.
            }
        }
    }

    private static void checkVariable(String name, Set<String> declared, String context) {
        if (!declared.contains(name)) {
            throw new SemanticException(
                    "Undefined variable '" + name + "' used in " + context
                            + ". Variable must be declared in a MATCH pattern before use.");
        }
    }
}