package cr.ac.una.eif400.cyphail.output;

import cr.ac.una.eif400.cyphail.ast.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */


public class TreePrinter {

    private static final String INDENT = "  ";

    public static String print(Statement statement) {
        return switch (statement) {
            // WHERE y RETURN solo aparecen si existen; Updates siempre (aunque sea [])
            case QueryNode(var match, var where, var updates, var returnClause) -> block("Query{", "}",
                    Stream.of(
                            Stream.of(clause(match)),
                            where.map(TreePrinter::clause).stream(),
                            Stream.of(block("Updates: [", "]", updates.stream().map(TreePrinter::clause).toList())),
                            returnClause.map(TreePrinter::clause).stream()
                    ).flatMap(part -> part).toList());
        };
    }

    private static String clause(Clause clause) {
        return switch (clause) {
            case MatchClause(var patterns) -> block("Match: {", "}", List.of(patternList(patterns)));
            case WhereClause(var condition) -> block("Where: {", "}", List.of("Expr: " + expression(condition)));
            case CreateClause(var patterns) -> block("Create: {", "}", List.of(patternList(patterns)));
            case DeleteClause(var detach, var items) -> block(detach ? "DetachDelete: {" : "Delete: {", "}",
                    List.of(block("Items: [", "]", items.stream().map(TreePrinter::expression).toList())));
            case RemoveClause(var items) -> block("Remove: {", "}",
                    List.of(block("Items: [", "]", items.stream().map(TreePrinter::expression).toList())));
            case ReturnClause(var items) -> block("Return: {", "}", List.of(
                    block("Projection: {", "}", List.of(
                            block("Items: [", "]", items.stream().map(TreePrinter::returnItem).toList()),
                            "Modifiers: []"))));
        };
    }

    private static String patternList(List<NodePattern> patterns) {
        return block("Patterns: [", "]", patterns.stream().map(TreePrinter::nodePattern).toList());
    }

    private static String nodePattern(NodePattern node) {
        return block("PatternNode: {", "}", List.of(
                "var: " + node.variable().orElse("_"),
                "labels: " + inlineList(node.labels()),
                "properties: " + inlineList(node.properties().stream()
                        .map(p -> "{" + p.key() + ": " + expression(p.value()) + "}")
                        .toList())));
    }

    // {as (. m title) title}  si tiene alias,   {(. m title)}  si no
    private static String returnItem(ReturnItem item) {
        String value = expression(item.expression());
        return item.alias()
                .map(alias -> "{as " + value + " " + alias + "}")
                .orElse("{" + value + "}");
    }

    // Expresion en preorden (operador primero)
    public static String expression(Expression expr) {
        return switch (expr) {
            case VariableExpr(var name) -> name;
            case LiteralExpr(var value) -> value;
            case PropertyLookup(var variable, var property) -> "(. " + variable.name() + " " + property + ")";
            case BinaryExpr(var left, var operator, var right) ->
                    "(" + operator + " " + expression(left) + " " + expression(right) + ")";
        };
    }

    // Un bloque: titulo, cada hijo con sangria en su propia linea, y el cierre.
    // Si no hay hijos queda en una linea: "Updates: []"
    private static String block(String open, String close, List<String> children) {
        if (children.isEmpty()) {
            return open + close;
        }
        String body = children.stream().map(TreePrinter::indent).collect(Collectors.joining("\n"));
        return open + "\n" + body + "\n" + close;
    }

    // Agrega sangria a TODAS las lineas de un texto (un hijo puede tener varias lineas)
    private static String indent(String text) {
        return text.lines().map(line -> INDENT + line).collect(Collectors.joining("\n"));
    }

    // [ Person Employee ]  o  []  si esta vacia
    private static String inlineList(List<String> values) {
        return values.isEmpty() ? "[]" : "[ " + String.join(" ", values) + " ]";
    }
}