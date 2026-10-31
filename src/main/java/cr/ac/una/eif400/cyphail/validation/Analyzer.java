package cr.ac.una.eif400.cyphail.validation;

import cr.ac.una.eif400.cyphail.ast.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

// Analisis semantico de una consulta: alcance de variables en orden de escritura (una variable se usa
// despues de declararse en un patron) y las reglas de nombres del SPEC (ver SymbolTable).
public final class Analyzer {

    private final SymbolTable symbols = new SymbolTable();
    private final List<String> errors = new ArrayList<>();

    private Analyzer() {
    }

    // Devuelve TODOS los errores encontrados (lista vacia = consulta valida)
    public static List<String> check(QueryNode query) {
        return analyze(query).errors;
    }

    // La tabla de simbolos de una consulta (la usa .tree)
    public static SymbolTable symbolsOf(QueryNode query) {
        return analyze(query).symbols;
    }

    // Igual que check, pero lanza SemanticException si hay errores
    public static void validate(QueryNode query) {
        List<String> errors = check(query);
        if (!errors.isEmpty()) {
            throw new SemanticException(String.join(System.lineSeparator(), errors));
        }
    }

    private static Analyzer analyze(QueryNode query) {
        Analyzer analyzer = new Analyzer();
        analyzer.run(query);
        return analyzer;
    }

    private void run(QueryNode query) {
        declarePatterns(query.match().map(MatchClause::patterns).orElse(List.of()));

        query.where().ifPresent(where -> use(where.condition(), "WHERE clause"));

        for (UpdatingClause update : query.updates()) {
            switch (update) {
                case CreateClause(var patterns) -> declarePatterns(patterns);
                case DeleteClause(var detach, var items) -> items.forEach(this::checkDeleted);
                case RemoveClause(var items) -> items.forEach(item -> use(item, "REMOVE clause"));
                case SetClause(var items) -> items.forEach(item -> {
                    use(item.target(), "SET clause");
                    use(item.value(), "SET clause");
                });
            }
        }

        query.returnClause().ifPresent(ret ->
                ret.items().forEach(item -> use(item.expression(), "RETURN clause")));

        List<String> distinct = errors.stream().distinct().toList();
        errors.clear();
        errors.addAll(distinct);
    }

    // DELETE solo borra variables (un nodo o una relacion), no expresiones
    private void checkDeleted(Expression item) {
        if (item instanceof VariableExpr) {
            use(item, "DELETE clause");
        } else {
            errors.add("DELETE only accepts variables of a node or a relationship.");
        }
    }

    // Por cada patron, en orden: primero revisa sus propiedades contra lo ya declarado
    // y DESPUES declara su variable. Asi (o {x: p.id}), (p) da error, pero (p), (o {x: p.id}) no.
    // Dentro de un camino se respeta el orden de escritura: nodo, relacion, nodo, relacion...
    private void declarePatterns(List<PathPattern> paths) {
        for (PathPattern path : paths) {
            declareNode(path.start());
            for (Step step : path.steps()) {
                declareRelationship(step.relationship());
                declareNode(step.node());
            }
        }
    }

    private void declareNode(NodePattern node) {
        String context = "properties of pattern (" + node.variable().orElse("") + ")";
        node.properties().forEach(property -> use(property.value(), context));
        node.variable().ifPresent(name -> declare(name, SymbolTable.Kind.NODE));
    }

    private void declareRelationship(RelPattern rel) {
        String context = "properties of relationship [" + rel.variable().orElse("") + "]";
        rel.properties().forEach(property -> use(property.value(), context));
        rel.variable().ifPresent(name -> declare(name, SymbolTable.Kind.RELATIONSHIP));
    }

    private void declare(String name, SymbolTable.Kind kind) {
        symbols.declare(name, kind).ifPresent(errors::add);
    }

    private void use(Expression expr, String context) {
        undefinedIn(expr, context).forEach(errors::add);
    }

    // Recorre la expresion y devuelve los errores de las variables sin declarar
    private Stream<String> undefinedIn(Expression expr, String context) {
        return switch (expr) {
            case VariableExpr(var name) -> undefinedName(name, context);
            case PropertyLookup(var variable, var property) -> undefinedName(variable.name(), context);
            case BinaryExpr(var left, var operator, var right) ->
                    Stream.concat(undefinedIn(left, context), undefinedIn(right, context));
            case NotExpr(var operand) -> undefinedIn(operand, context);
            case LiteralExpr literal -> Stream.empty();
        };
    }

    private Stream<String> undefinedName(String name, String context) {
        return symbols.contains(name)
                ? Stream.empty()
                : Stream.of("Undefined variable '" + name + "' used in " + context
                            + ". Variables must be declared in a pattern before use.");
    }
}
