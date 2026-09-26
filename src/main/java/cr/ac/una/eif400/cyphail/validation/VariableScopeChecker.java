package cr.ac.una.eif400.cyphail.validation;

import cr.ac.una.eif400.cyphail.ast.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

public class VariableScopeChecker {

    // Devuelve TODOS los errores encontrados (lista vacia = consulta valida)
    public static List<String> check(QueryNode query) {
        Set<String> declared = new HashSet<>();
        List<String> errors = new ArrayList<>();

        declarePatterns(query.match().patterns(), declared, errors);

        query.where().ifPresent(where ->
                errors.addAll(undefinedIn(where.condition(), declared, "WHERE clause").toList()));

        for (UpdatingClause update : query.updates()) {
            switch (update) {
                case CreateClause(var patterns) -> declarePatterns(patterns, declared, errors);
                case DeleteClause(var detach, var items) -> items.forEach(item ->
                        errors.addAll(undefinedIn(item, declared, "DELETE clause").toList()));
                case RemoveClause(var items) -> items.forEach(item ->
                        errors.addAll(undefinedIn(item, declared, "REMOVE clause").toList()));
            }
        }

        query.returnClause().ifPresent(ret -> ret.items().forEach(item ->
                errors.addAll(undefinedIn(item.expression(), declared, "RETURN clause").toList())));

        return errors.stream().distinct().toList();
    }

    // Igual que check, pero lanza SemanticException si hay errores
    public static void validate(QueryNode query) {
        List<String> errors = check(query);
        if (!errors.isEmpty()) {
            throw new SemanticException(String.join(System.lineSeparator(), errors));
        }
    }

    // Por cada patron, en orden: primero revisa sus propiedades contra lo ya declarado
    // y DESPUES declara su variable. Asi (o {x: p.id}), (p) da error, pero (p), (o {x: p.id}) no.
    private static void declarePatterns(List<NodePattern> patterns, Set<String> declared, List<String> errors) {
        for (NodePattern node : patterns) {
            String context = "properties of pattern (" + node.variable().orElse("") + ")";
            node.properties().forEach(property ->
                    errors.addAll(undefinedIn(property.value(), declared, context).toList()));
            node.variable().ifPresent(declared::add);
        }
    }

    // Funcion pura: devuelve los errores de una expresion (recorre el arbol con switch).
    private static Stream<String> undefinedIn(Expression expr, Set<String> declared, String context) {
        return switch (expr) {
            case VariableExpr(var name) -> undefinedName(name, declared, context);
            case PropertyLookup(var variable, var property) -> undefinedName(variable.name(), declared, context);
            case BinaryExpr(var left, var operator, var right) ->
                    Stream.concat(undefinedIn(left, declared, context), undefinedIn(right, declared, context));
            case LiteralExpr literal -> Stream.empty();
        };
    }

    private static Stream<String> undefinedName(String name, Set<String> declared, String context) {
        return declared.contains(name)
                ? Stream.empty()
                : Stream.of("Undefined variable '" + name + "' used in " + context
                            + ". Variables must be declared in a pattern before use.");
    }
}