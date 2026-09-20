package cr.ac.una.eif400.cyphail.output;

import cr.ac.una.eif400.cyphail.ast.*;

import java.util.List;
import java.util.stream.Collectors;


public class TreePrinter {

    public static String toSExpr(Statement stmt) {
        return switch (stmt) {
            case QueryNode(MatchClause match, var where, var ret, var remove) -> {
                StringBuilder sb = new StringBuilder("(query ");
                sb.append(clauseToSExpr(match));
                where.ifPresent(w -> sb.append(" ").append(clauseToSExpr(w)));
                ret.ifPresent(r -> sb.append(" ").append(clauseToSExpr(r)));
                remove.ifPresent(r -> sb.append(" ").append(clauseToSExpr(r)));
                sb.append(")");
                yield sb.toString();
            }
        };
    }

    private static String clauseToSExpr(Clause clause) {
        return switch (clause) {
            case MatchClause(String variable, String label) ->
                    "(match " + variable + " " + label + ")";
            case WhereClause(Expression condition) ->
                    "(where " + exprToSExpr(condition) + ")";
            case ReturnClause(List<ReturnItem> items) -> {
                String inner = items.stream()
                        .map(TreePrinter::returnItemToSExpr)
                        .collect(Collectors.joining(" "));
                yield "(return " + inner + ")";
            }
            case RemoveClause(List<PropertyLookup> items) -> {
                String inner = items.stream()
                        .map(TreePrinter::exprToSExpr)
                        .collect(Collectors.joining(" "));
                yield "(remove " + inner + ")";
            }
        };
    }

    private static String returnItemToSExpr(ReturnItem item) {
        String base = exprToSExpr(item.expression());
        return item.alias().map(a -> "(as " + base + " " + a + ")").orElse(base);
    }

    private static String exprToSExpr(Expression expr) {
        return switch (expr) {
            case VariableExpr(String name) -> name;
            case LiteralExpr(String value) -> value;
            case PropertyLookup(VariableExpr variable, String property) -> variable.name() + "." + property;
            case BinaryExpr(Expression left, String operator, Expression right) ->
                    "(" + operator + " " + exprToSExpr(left) + " " + exprToSExpr(right) + ")";
        };
    }

    public static String toJson(Statement stmt) {
        return switch (stmt) {
            case QueryNode(MatchClause match, var where, var ret, var remove) -> {
                StringBuilder sb = new StringBuilder("{\"type\":\"Query\",\"match\":");
                sb.append(clauseToJson(match));
                where.ifPresent(w -> sb.append(",\"where\":").append(clauseToJson(w)));
                ret.ifPresent(r -> sb.append(",\"return\":").append(clauseToJson(r)));
                remove.ifPresent(r -> sb.append(",\"remove\":").append(clauseToJson(r)));
                sb.append("}");
                yield sb.toString();
            }
        };
    }

    private static String clauseToJson(Clause clause) {
        return switch (clause) {
            case MatchClause(String variable, String label) -> """
                    {"type":"Match","variable":"%s","label":"%s"}\
                    """.formatted(escape(variable), escape(label));
            case WhereClause(Expression condition) -> """
                    {"type":"Where","condition":%s}\
                    """.formatted(exprToJson(condition));
            case ReturnClause(List<ReturnItem> items) -> {
                String inner = items.stream()
                        .map(TreePrinter::returnItemToJson)
                        .collect(Collectors.joining(","));
                yield "{\"type\":\"Return\",\"items\":[" + inner + "]}";
            }
            case RemoveClause(List<PropertyLookup> items) -> {
                String inner = items.stream()
                        .map(TreePrinter::exprToJson)
                        .collect(Collectors.joining(","));
                yield "{\"type\":\"Remove\",\"items\":[" + inner + "]}";
            }
        };
    }

    private static String returnItemToJson(ReturnItem item) {
        String base = exprToJson(item.expression());
        String aliasField = item.alias()
                .map(a -> ",\"alias\":\"" + escape(a) + "\"")
                .orElse("");
        return "{\"expression\":" + base + aliasField + "}";
    }

    private static String exprToJson(Expression expr) {
        return switch (expr) {
            case VariableExpr(String name) -> """
                    {"type":"Variable","name":"%s"}\
                    """.formatted(escape(name));
            case LiteralExpr(String value) -> """
                    {"type":"Literal","value":"%s"}\
                    """.formatted(escape(value));
            case PropertyLookup(VariableExpr variable, String property) -> """
                    {"type":"PropertyLookup","variable":"%s","property":"%s"}\
                    """.formatted(escape(variable.name()), escape(property));
            case BinaryExpr(Expression left, String operator, Expression right) -> """
                    {"type":"Binary","operator":"%s","left":%s,"right":%s}\
                    """.formatted(escape(operator), exprToJson(left), exprToJson(right));
        };
    }

    private static String escape(String raw) {
        return raw.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}