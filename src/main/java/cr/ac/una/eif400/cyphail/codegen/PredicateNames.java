package cr.ac.una.eif400.cyphail.codegen;

import java.util.regex.Pattern;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

// Los nombres de los predicados Prolog, en UN solo lugar. Los del usuario describen el grafo
// (user_<grafo>_...) y los del engine empiezan con cy_ (SPEC P2). Si el esquema del grafo cambia,
// este es el unico archivo de Java que se toca.
public final class PredicateNames {

    private static final Pattern GRAPH = Pattern.compile("[a-z][a-zA-Z0-9_]*");

    private final String graph;

    public PredicateNames(String graph) {
        if (!GRAPH.matcher(graph).matches()) {
            throw new IllegalArgumentException("Invalid graph name: '" + graph + "'");
        }
        this.graph = graph;
    }

    // ---------- el grafo del usuario ----------

    public String node() {
        return "user_" + graph + "_node";    // user_amigos_node(Id)
    }

    public String label() {
        return "user_" + graph + "_label";   // user_amigos_label(NodeId, Label)
    }

    public String edge() {
        return "user_" + graph + "_edge";    // user_amigos_edge(EdgeId, Type, From, To)
    }

    public String prop() {
        return "user_" + graph + "_prop";    // user_amigos_prop(Id, Key, Value)  (nodos y relaciones)
    }

    // ---------- el engine ----------

    public static String createQuery() {
        return "cy_create_query";
    }

    public static String execQuery() {
        return "cy_exec_query";
    }

    public static String eraseQuery() {
        return "cy_erase_query";
    }

    // El predicado del engine para un operador de comparacion de Cyphail
    public static String comparator(String operator) {
        return switch (operator) {
            case "=" -> "cy_eq";
            case "<>" -> "cy_neq";
            case ">" -> "cy_gt";
            case "<" -> "cy_lt";
            case ">=" -> "cy_gte";
            case "<=" -> "cy_lte";
            default -> throw new IllegalArgumentException("Not a comparison operator: '" + operator + "'");
        };
    }
}