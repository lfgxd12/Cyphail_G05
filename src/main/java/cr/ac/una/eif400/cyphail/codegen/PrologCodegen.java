package cr.ac.una.eif400.cyphail.codegen;

import cr.ac.una.eif400.cyphail.ast.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

// El Codegen: recorre el AST de una consulta y produce el texto Prolog que se envia al engine
// (el "target" del SPEC P2). Cada sentencia genera UNA peticion:
//
//   cy_create_query(__QueryId, (query_01(<columnas>) :- <goals>)),
//   cy_exec_query(__QueryId, [<nombres de columna>], Rows),
//   cy_erase_query(__QueryId)
//
// Traduce consultas de lectura (MATCH [WHERE] RETURN) y de escritura (CREATE, SET, REMOVE, DELETE).
// Sin RETURN la cabeza no tiene argumentos (query_01) y la tabla no tiene columnas.
public final class PrologCodegen {

    private static final String QUERY_NAME = "query_01";

    private final PredicateNames names;

    public PrologCodegen(PredicateNames names) {
        this.names = names;
    }

    public CodegenResult generate(QueryNode query) {
        return new Translation(names).translate(query);
    }

    private static UnsupportedOperationException unsupported(String what) {
        return new UnsupportedOperationException(what + " is not supported yet");
    }

    // El estado de UNA traduccion: el mapa de variables y los goals acumulados, en orden y sin repetir.
    private static final class Translation {

        private final PredicateNames names;
        private final PrologVariables vars = new PrologVariables();
        private final Set<String> goals = new LinkedHashSet<>();   // sin repetir: un goal repetido no cambia el resultado
        private final List<String> edges = new ArrayList<>();      // variables de las relaciones ya generadas
        private final Set<String> bound = new HashSet<>();         // variables de Cyphail ya ligadas (por MATCH o por un CREATE anterior)

        Translation(PredicateNames names) {
            this.names = names;
        }

        CodegenResult translate(QueryNode query) {
            if (query.match().isEmpty() && query.updates().isEmpty()) {
                throw unsupported("A query without MATCH or updating clauses");
            }
            if (query.updates().isEmpty() && query.returnClause().isEmpty()) {
                throw unsupported("A read query without RETURN");
            }

            // Orden de la gramatica: MATCH, WHERE, escrituras y por ultimo RETURN
            query.match().ifPresent(match -> match.patterns().forEach(this::path));
            query.where().ifPresent(where ->
                    conjuncts(where.condition()).forEach(condition -> goals.add(goal(condition))));
            query.updates().forEach(this::update);

            List<String> head = new ArrayList<>();
            List<String> columns = new ArrayList<>();
            for (ReturnItem item : query.returnClause().map(ReturnClause::items).orElse(List.of())) {
                head.add(term(item.expression()));
                columns.add(item.alias().orElseGet(() -> columnName(item.expression())));
            }

            return new CodegenResult(assemble(head, columns), columns, vars.table());
        }

        // ---------- el texto final ----------

        private String assemble(List<String> head, List<String> columns) {
            String queryId = PrologVariables.QUERY_ID;
            return PredicateNames.createQuery() + "(" + queryId + ",\n"
                    + "    (" + QUERY_NAME + (head.isEmpty() ? "" : "(" + String.join(", ", head) + ")") + " :-\n"
                    + "        " + String.join(",\n        ", goals) + ")),\n"
                    + PredicateNames.execQuery() + "(" + queryId + ", " + PrologTerms.atomList(columns) + ", Rows),\n"
                    + PredicateNames.eraseQuery() + "(" + queryId + ")";
        }

        // ---------- patrones ----------

        // Un camino: el nodo inicial y, por cada paso, la relacion y el nodo al que llega
        private void path(PathPattern path) {
            String from = nodeTerm(path.start());
            nodeGoals(path.start(), from, !path.steps().isEmpty());
            for (Step step : path.steps()) {
                String to = nodeTerm(step.node());
                relationship(step.relationship(), from, to);
                nodeGoals(step.node(), to, true);
                from = to;
            }
        }

        // La variable Prolog de un nodo; un nodo sin variable es una variable anonima de Prolog
        private String nodeTerm(NodePattern node) {
            node.variable().ifPresent(bound::add);
            return node.variable().map(vars::variable).orElse("_");
        }

        // Etiquetas y propiedades de un nodo. Un nodo suelto sin etiquetas necesita su propio goal
        // (si esta conectado a una relacion, la relacion ya lo liga).
        private void nodeGoals(NodePattern node, String term, boolean connected) {
            node.labels().forEach(label ->
                    goals.add(names.label() + "(" + term + ", " + PrologTerms.atom(label) + ")"));
            if (node.labels().isEmpty() && !connected) {
                goals.add(names.node() + "(" + term + ")");
            }
            properties(term, node.properties());
        }

        // -[r:TIPO {props}]->  segun su direccion. Cada relacion debe ser distinta de las anteriores
        // del mismo MATCH (cy_neq): "el arco se usa solo una vez".
        private void relationship(RelPattern rel, String from, String to) {
            rel.variable().ifPresent(bound::add);
            String edge = rel.variable().map(vars::variable).orElseGet(vars::nextRelationship);
            String type = switch (rel.types().size()) {
                case 0 -> "_";
                case 1 -> PrologTerms.atom(rel.types().get(0));
                default -> throw unsupported("Alternative relationship types");
            };

            goals.add(switch (rel.direction()) {
                case OUT -> edgeGoal(edge, type, from, to);
                case IN -> edgeGoal(edge, type, to, from);
                case NONE -> "( " + edgeGoal(edge, type, from, to) + " ; " + edgeGoal(edge, type, to, from) + " )";
            });
            edges.forEach(previous ->
                    goals.add(PredicateNames.comparator("<>") + "(" + previous + ", " + edge + ")"));
            edges.add(edge);
            properties(edge, rel.properties());
        }

        private String edgeGoal(String edge, String type, String from, String to) {
            return names.edge() + "(" + edge + ", " + type + ", " + from + ", " + to + ")";
        }

        // {clave: valor} de un nodo o de una relacion: prop(Id, clave, Valor)
        private void properties(String id, List<Property> properties) {
            for (Property property : properties) {
                goals.add(names.prop() + "(" + id + ", " + PrologTerms.atom(property.key()) + ", "
                        + term(property.value()) + ")");
            }
        }

        // ---------- escritura (CREATE, SET, REMOVE, DELETE) ----------
        // Los goals de escritura van en el cuerpo de la consulta, despues de los del MATCH/WHERE:
        // el engine los ejecuta una vez por cada solucion del MATCH.

        private void update(UpdatingClause clause) {
            switch (clause) {
                case CreateClause(var patterns) -> patterns.forEach(this::create);
                case SetClause(var items) -> items.forEach(item -> {
                    goals.add(call(names.setProp(), names.graph(), id(item.target().variable()),
                            PrologTerms.atom(item.target().property()), term(item.value())));
                    vars.refresh(item.target().variable().name(), item.target().property());
                });
                case RemoveClause(var items) -> items.forEach(item -> {
                    goals.add(call(names.removeProp(), names.graph(), id(item.variable()),
                            PrologTerms.atom(item.property())));
                    vars.refresh(item.variable().name(), item.property());
                });
                case DeleteClause(var detach, var items) -> items.forEach(item -> goals.add(call(
                        detach ? names.detachDelete() : names.delete(), names.graph(), id(item))));
            }
        }

        // CREATE: crea los nodos nuevos y luego las relaciones. Un nodo cuya variable ya esta ligada
        // (por el MATCH o por un CREATE anterior) es una referencia: no se crea otra vez.
        private void create(PathPattern path) {
            String from = createNode(path.start());
            for (Step step : path.steps()) {
                String to = createNode(step.node());
                createRelationship(step.relationship(), from, to);
                from = to;
            }
        }

        private String createNode(NodePattern node) {
            if (node.variable().filter(bound::contains).isPresent()) {
                return vars.variable(node.variable().get());
            }
            String id = node.variable().map(vars::variable).orElseGet(vars::nextNode);
            node.variable().ifPresent(bound::add);
            goals.add(call(names.createNode(), names.graph(), labelList(node.labels()), propertyList(node.properties()), id));
            return id;
        }

        private void createRelationship(RelPattern rel, String from, String to) {
            if (rel.types().size() != 1) {
                throw unsupported("CREATE of a relationship without exactly one type");
            }
            String type = PrologTerms.atom(rel.types().get(0));
            String edge = rel.variable().map(vars::variable).orElseGet(vars::nextRelationship);
            rel.variable().ifPresent(bound::add);
            goals.add(switch (rel.direction()) {
                case OUT -> call(names.createEdge(), names.graph(), type, from, to, propertyList(rel.properties()), edge);
                case IN -> call(names.createEdge(), names.graph(), type, to, from, propertyList(rel.properties()), edge);
                case NONE -> throw unsupported("CREATE of a relationship without direction");
            });
        }

        // La variable Prolog que guarda el id de un nodo o relacion; SET/REMOVE/DELETE solo aceptan variables
        private String id(VariableExpr variable) {
            return vars.variable(variable.name());
        }

        private String id(Expression expression) {
            if (expression instanceof VariableExpr variable) {
                return id(variable);
            }
            throw unsupported("DELETE of something that is not a variable");
        }

        // ['Persona', 'Empresa']
        private static String labelList(List<String> labels) {
            return labels.stream().map(PrologTerms::atom).collect(Collectors.joining(", ", "[", "]"));
        }

        // [nombre-'Marta', age-30]  (un numero negativo va entre parentesis: age-(-3))
        private String propertyList(List<Property> properties) {
            return properties.stream()
                    .map(property -> PrologTerms.atom(property.key()) + "-" + wrapNegative(term(property.value())))
                    .collect(Collectors.joining(", ", "[", "]"));
        }

        private static String wrapNegative(String term) {
            return term.startsWith("-") ? "(" + term + ")" : term;
        }

        private static String call(String predicate, String... arguments) {
            return predicate + "(" + String.join(", ", arguments) + ")";
        }

        // ---------- expresiones ----------

        // El valor de una expresion (un termino Prolog). a.name pide su propio goal prop(...), una sola vez.
        private String term(Expression expression) {
            return switch (expression) {
                case LiteralExpr literal -> PrologTerms.literal(literal);
                case VariableExpr(var name) -> vars.variable(name);
                case PropertyLookup(var variable, var property) -> lookup(variable.name(), property);
                case BinaryExpr binary -> throw unsupported("A comparison used as a value");
                case NotExpr not -> throw unsupported("NOT used as a value");
            };
        }

        private String lookup(String variable, String property) {
            String value = vars.property(variable, property);
            goals.add(names.prop() + "(" + vars.variable(variable) + ", " + PrologTerms.atom(property) + ", " + value + ")");
            return value;
        }

        // Una condicion del WHERE como goal Prolog
        private String goal(Expression condition) {
            return switch (condition) {
                case BinaryExpr(var left, var operator, var right) -> switch (operator) {
                    case "AND" -> "(" + goal(left) + ", " + goal(right) + ")";
                    case "OR" -> "(" + goal(left) + " ; " + goal(right) + ")";
                    default -> PredicateNames.comparator(operator) + "(" + term(left) + ", " + term(right) + ")";
                };
                case NotExpr(var operand) -> "\\+ " + goal(operand);
                default -> throw unsupported("A condition that is not a comparison");
            };
        }

        // a AND b AND c -> [a, b, c]: cada una queda como un goal aparte, como en el ejemplo del SPEC
        private List<Expression> conjuncts(Expression condition) {
            if (condition instanceof BinaryExpr(var left, var operator, var right) && operator.equals("AND")) {
                return Stream.concat(conjuncts(left).stream(), conjuncts(right).stream()).toList();
            }
            return List.of(condition);
        }

        // El nombre de la columna cuando no hay alias: a.name, o a
        private String columnName(Expression expression) {
            return switch (expression) {
                case VariableExpr(var name) -> name;
                case PropertyLookup(var variable, var property) -> variable.name() + "." + property;
                case LiteralExpr literal -> literal.value();
                default -> throw unsupported("RETURN of this expression");
            };
        }
    }
}