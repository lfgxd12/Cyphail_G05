package cr.ac.una.eif400.cyphail.parser;

import cr.ac.una.eif400.cyphail.ast.*;
import cr.ac.una.eif400.cyphail.parser.core.*;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

public class CyphailParser {

    // Un identificador, devolviendo solo su texto
    private static final Parser<InputString, String, String> ID =
            Parsers.Map(CyphailLexers.Id(), TokenString::value);

    // Palabras reservadas: el lexer Id() las reconoce como identificadores cualquiera, asi que sin esto
    // "WHERE NOT" se leeria como la variable "NOT" y "... AND RETURN" como la variable "RETURN".
    private static final Set<String> RESERVED = Set.of(
            "MATCH", "WHERE", "RETURN", "AS", "CREATE", "DELETE", "DETACH", "SET", "REMOVE", "AND", "OR", "NOT",
            "TRUE", "FALSE");

    // Una variable en una expresion: un identificador que no es palabra reservada.
    // El parser interno no consume nada: solo decide si deja pasar al nombre que ya se leyo.
    private static Parser<InputString, String, String> variableName() {
        return Parsers.FlatMap(ID, name -> source -> {
            if (RESERVED.contains(name.toUpperCase(Locale.ROOT))) {
                return new Fail<>("'" + name + "' is a reserved word and cannot be used as a variable");
            }
            return new Ok<>(name, source);
        });
    }

    // ---------------- Expresiones ----------------

    // propertyLookup : variable "." propiedad      ej. m.title
    private static Parser<InputString, PropertyLookup, String> propertyLookup() {
        return Parsers.FlatMap(variableName(), variable ->
                Parsers.Map(Parsers.Right(CyphailLexers.Dot(), ID),
                        property -> new PropertyLookup(new VariableExpr(variable), property)));
    }

    // atom : m.title | m | 300 | "texto"
    // propertyLookup va primero: si "m" se leyera como variable, ".title" quedaria suelto
    // true / false (sin importar mayusculas). Se guardan en minuscula, como los escribe Prolog: LiteralExpr("true").
    // Son palabras reservadas, asi que variableName() nunca los lee como variable.
    private static Parser<InputString, Expression, String> booleanLiteral() {
        return Parsers.FlatMap(ID, name -> source -> {
            if (name.equalsIgnoreCase("true") || name.equalsIgnoreCase("false")) {
                return new Ok<>(new LiteralExpr(name.toLowerCase(Locale.ROOT)), source);
            }
            return new Fail<>("Expected true or false");
        });
    }

    private static Parser<InputString, Expression, String> atom() {
        return Parsers.Choice(
                Parsers.Map(propertyLookup(), lookup -> lookup),
                booleanLiteral(),
                Parsers.Map(variableName(), VariableExpr::new),
                Parsers.Map(CyphailLexers.Number(), token -> new LiteralExpr(token.value())),
                Parsers.Map(CyphailLexers.String(), token -> new LiteralExpr(token.value())));
    }

    // Primero los operadores de 2 caracteres
    // Pd. fixeado el <>
    private static Parser<InputString, TokenString, String> comparisonOp() {
        return Parsers.Choice(
                CyphailLexers.Neq(), CyphailLexers.Lte(), CyphailLexers.Gte(),
                CyphailLexers.Lt(), CyphailLexers.Gt(), CyphailLexers.Eq());
    }

    // La gramatica de condiciones, de menor a mayor precedencia (la precedencia esta en la FORMA de la gramatica):
    //   expression := orExpr
    //   orExpr     := andExpr ("OR" andExpr)*
    //   andExpr    := notExpr ("AND" notExpr)*
    //   notExpr    := "NOT" notExpr | comparison
    //   comparison := primary (operador primary)?
    //   primary    := atom | "(" expression ")"
    // Contrato con el Codegen: AND y OR se guardan como BinaryExpr con "AND" / "OR" en mayuscula.

    // expression() y notExpr() se llaman a si mismas (por el parentesis y por NOT NOT x).
    // Si se construyeran de golpe, la construccion no terminaria nunca: la lambda difiere el parser
    // hasta el momento de parsear.
    private static Parser<InputString, Expression, String> lazyExpression() {
        return source -> expression().parse(source);
    }

    private static Parser<InputString, Expression, String> lazyNotExpr() {
        return source -> notExpr().parse(source);
    }

    // primary : atom | "(" expression ")"
    // Dentro de una expresion no hay nodos, asi que el parentesis no se confunde con el de un nodo
    private static Parser<InputString, Expression, String> primary() {
        return Parsers.Choice(
                atom(),
                Parsers.Between(CyphailLexers.LParen(), lazyExpression(), CyphailLexers.RParen()));
    }

    // comparison : primary (operador primary)?
    // Si despues del primary viene un operador, se arma un BinaryExpr; si no, queda el primary solo
    private static Parser<InputString, Expression, String> comparison() {
        return Parsers.FlatMap(primary(), left ->
                Parsers.Map(
                        Parsers.Opt(Parsers.FlatMap(comparisonOp(), op ->
                                Parsers.Map(primary(), right -> new BinaryExpr(left, op.value(), right)))),
                        found -> found.<Expression>map(binary -> binary).orElse(left)));
    }

    // notExpr : "NOT" notExpr | comparison
    private static Parser<InputString, Expression, String> notExpr() {
        Parser<InputString, Expression, String> negated =
                Parsers.Map(Parsers.Right(CyphailLexers.Not(), lazyNotExpr()), NotExpr::new);
        return Parsers.Choice(negated, comparison());
    }

    // operando (palabra operando)*  -> asociativo a la izquierda: a AND b AND c = (a AND b) AND c
    // El pliegue (reduce) arma el BinaryExpr de izquierda a derecha, sin ciclos ni variables mutables.
    private static Parser<InputString, Expression, String> leftAssociative(
            Parser<InputString, Expression, String> operand,
            Parser<InputString, TokenString, String> keyword,
            String operator) {
        return Parsers.FlatMap(operand, first ->
                Parsers.Map(Parsers.Many(Parsers.Right(keyword, operand)),
                        rest -> rest.stream().reduce(first,
                                (left, right) -> new BinaryExpr(left, operator, right))));
    }

    // andExpr : notExpr ("AND" notExpr)*
    private static Parser<InputString, Expression, String> andExpr() {
        return leftAssociative(notExpr(), CyphailLexers.And(), "AND");
    }

    // expression : andExpr ("OR" andExpr)*
    public static Parser<InputString, Expression, String> expression() {
        return leftAssociative(andExpr(), CyphailLexers.Or(), "OR");
    }

    // ---------------- Patrones ----------------

    // property : clave ":" expression      ej. id: 1   o   personId: p.id
    private static Parser<InputString, Property, String> property() {
        return Parsers.FlatMap(Parsers.Left(ID, CyphailLexers.Colon()), key ->
                Parsers.Map(expression(), value -> new Property(key, value)));
    }

    // properties : "{" (property ("," property)*)? "}"
    private static Parser<InputString, List<Property>, String> properties() {
        return Parsers.Between(CyphailLexers.LBrace(),
                Parsers.SepBy(property(), CyphailLexers.Comma()),
                CyphailLexers.RBrace());
    }

    // nodePattern : "(" variable? (":" etiqueta)* properties? ")"
    private static Parser<InputString, NodePattern, String> nodePattern() {
        var labels = Parsers.Many(Parsers.Right(CyphailLexers.Colon(), ID));
        var props = Parsers.Map(Parsers.Opt(properties()), found -> found.orElse(List.of()));
        var inside = Parsers.FlatMap(Parsers.Opt(ID), variable ->
                Parsers.FlatMap(labels, labelList ->
                        Parsers.Map(props, propertyList -> new NodePattern(variable, labelList, propertyList))));
        return Parsers.Between(CyphailLexers.LParen(), inside, CyphailLexers.RParen());
    }

    // ---------------- Relaciones (P2) ----------------

    // Lo que va entre corchetes: [r:TIPO|OTRO {clave: valor}]. Todo es opcional.
    private record RelDetail(Optional<String> variable, List<String> types, List<Property> properties) {
    }

    private static final RelDetail NO_DETAIL = new RelDetail(Optional.empty(), List.of(), List.of());

    // types : (":" tipo ("|" tipo)*)?
    private static Parser<InputString, List<String>, String> relTypes() {
        Parser<InputString, List<String>, String> typeList =
                Parsers.Right(CyphailLexers.Colon(), Parsers.SepBy1(ID, CyphailLexers.Pipe()));
        return Parsers.Map(Parsers.Opt(typeList), found -> found.orElse(List.of()));
    }

    // detail : "[" variable? types? properties? "]"
    private static Parser<InputString, RelDetail, String> relDetail() {
        Parser<InputString, List<Property>, String> props =
                Parsers.Map(Parsers.Opt(properties()), found -> found.orElse(List.of()));
        Parser<InputString, RelDetail, String> inside =
                Parsers.FlatMap(Parsers.Opt(ID), variable ->
                        Parsers.FlatMap(relTypes(), types ->
                                Parsers.Map(props, propList -> new RelDetail(variable, types, propList))));
        return Parsers.Between(CyphailLexers.LBracket(), inside, CyphailLexers.RBracket());
    }

    // El detalle puede no venir: (a)-->(b)
    private static Parser<InputString, RelDetail, String> optionalDetail() {
        return Parsers.Map(Parsers.Opt(relDetail()), found -> found.orElse(NO_DETAIL));
    }

    private static RelPattern rel(RelDetail detail, Direction direction) {
        return new RelPattern(detail.variable(), detail.types(), detail.properties(), direction);
    }

    // relationship : "<-" detail? "-"           -> IN    (a)<-[r]-(b)   (a)<--(b)
    //              | "-" detail? "->"            -> OUT   (a)-[r]->(b)   (a)-->(b)
    //              | "-" detail? "-"             -> NONE  (a)-[r]-(b)    (a)--(b)
    // La direccion se decide DESPUES de leer el detalle, por eso se usa FlatMap.
    // "->" se prueba antes que "-": si no, el guion se comeria la flecha.
    private static Parser<InputString, RelPattern, String> relationship() {
        Parser<InputString, RelPattern, String> incoming =
                Parsers.FlatMap(CyphailLexers.LArrow(), ignored ->
                        Parsers.FlatMap(optionalDetail(), detail ->
                                Parsers.Map(CyphailLexers.Dash(), dash -> rel(detail, Direction.IN))));

        Parser<InputString, RelPattern, String> outgoingOrNone =
                Parsers.FlatMap(CyphailLexers.Dash(), ignored ->
                        Parsers.FlatMap(optionalDetail(), detail -> {
                            Parser<InputString, RelPattern, String> out =
                                    Parsers.Map(CyphailLexers.Arrow(), arrow -> rel(detail, Direction.OUT));
                            Parser<InputString, RelPattern, String> none =
                                    Parsers.Map(CyphailLexers.Dash(), dash -> rel(detail, Direction.NONE));
                            return Parsers.Or(out, none);
                        }));

        return Parsers.Or(incoming, outgoingOrNone);
    }

    // step : relationship nodePattern
    private static Parser<InputString, Step, String> step() {
        return Parsers.FlatMap(relationship(), relation ->
                Parsers.Map(nodePattern(), node -> new Step(relation, node)));
    }

    // path : nodePattern step*        (sin pasos es un nodo suelto)
    private static Parser<InputString, PathPattern, String> path() {
        return Parsers.FlatMap(nodePattern(), start ->
                Parsers.Map(Parsers.Many(step()), steps -> new PathPattern(start, steps)));
    }

    // pattern : path ("," path)*
    private static Parser<InputString, List<PathPattern>, String> pattern() {
        return Parsers.SepBy1(path(), CyphailLexers.Comma());
    }

    // ---------------- Clausulas ----------------

    private static Parser<InputString, MatchClause, String> matchClause() {
        return Parsers.Map(Parsers.Right(CyphailLexers.Match(), pattern()), MatchClause::new);
    }

    private static Parser<InputString, WhereClause, String> whereClause() {
        return Parsers.Map(Parsers.Right(CyphailLexers.Where(), expression()), WhereClause::new);
    }

    private static Parser<InputString, UpdatingClause, String> createClause() {
        return Parsers.Map(Parsers.Right(CyphailLexers.Create(), pattern()), CreateClause::new);
    }

    // deleteClause : "DETACH"? "DELETE" expression ("," expression)*
    private static Parser<InputString, UpdatingClause, String> deleteClause() {
        return Parsers.FlatMap(Parsers.Opt(CyphailLexers.Detach()), detach ->
                Parsers.Map(Parsers.Right(CyphailLexers.Delete(), Parsers.SepBy1(expression(), CyphailLexers.Comma())),
                        items -> new DeleteClause(detach.isPresent(), items)));
    }

    private static Parser<InputString, UpdatingClause, String> removeClause() {
        return Parsers.Map(Parsers.Right(CyphailLexers.Remove(), Parsers.SepBy1(propertyLookup(), CyphailLexers.Comma())),
                RemoveClause::new);
    }

    // setItem : propertyLookup "=" expression      ej. p.precio = 19.99
    private static Parser<InputString, SetItem, String> setItem() {
        return Parsers.FlatMap(Parsers.Left(propertyLookup(), CyphailLexers.Eq()), target ->
                Parsers.Map(expression(), value -> new SetItem(target, value)));
    }

    // setClause : "SET" setItem ("," setItem)*
    private static Parser<InputString, UpdatingClause, String> setClause() {
        return Parsers.Map(Parsers.Right(CyphailLexers.Set(), Parsers.SepBy1(setItem(), CyphailLexers.Comma())),
                SetClause::new);
    }

    // updatingClause : createClause | setClause | deleteClause | removeClause
    private static Parser<InputString, UpdatingClause, String> updatingClause() {
        return Parsers.Choice(createClause(), setClause(), deleteClause(), removeClause());
    }

    // returnItem : expression ("AS" alias)?
    private static Parser<InputString, ReturnItem, String> returnItem() {
        return Parsers.FlatMap(expression(), expr ->
                Parsers.Map(Parsers.Opt(Parsers.Right(CyphailLexers.As(), ID)),
                        alias -> new ReturnItem(expr, alias)));
    }

    private static Parser<InputString, ReturnClause, String> returnClause() {
        return Parsers.Map(Parsers.Right(CyphailLexers.Return(), Parsers.SepBy1(returnItem(), CyphailLexers.Comma())),
                ReturnClause::new);
    }

    // ---------------- Consulta completa ----------------

    // query : MATCH pattern [WHERE expr] updatingClause* [RETURN] EOF
    //       |                           updatingClause+ [RETURN] EOF      (sin MATCH: CREATE (a:Persona) ...)
    // El orden sigue la gramatica: readingClause* updatingClause* returnClause?
    // EOF al final garantiza que no sobre texto sin leer. El WHERE pertenece al MATCH.

    // [RETURN] EOF : lo que cierra toda consulta
    private static Parser<InputString, Optional<ReturnClause>, String> returnAndEof() {
        return Parsers.Left(Parsers.Opt(returnClause()), CyphailLexers.Eof());
    }

    private static Parser<InputString, QueryNode, String> queryWithMatch() {
        return Parsers.FlatMap(matchClause(), match ->
                Parsers.FlatMap(Parsers.Opt(whereClause()), where ->
                        Parsers.FlatMap(Parsers.Many(updatingClause()), updates ->
                                Parsers.Map(returnAndEof(),
                                        ret -> new QueryNode(Optional.of(match), where, updates, ret)))));
    }

    // Sin MATCH hace falta al menos una clausula de actualizacion: "RETURN 1" solo sigue siendo error
    private static Parser<InputString, QueryNode, String> queryWithoutMatch() {
        return Parsers.FlatMap(updatingClause(), first ->
                Parsers.FlatMap(Parsers.Many(updatingClause()), more ->
                        Parsers.Map(returnAndEof(),
                                ret -> new QueryNode(Optional.empty(), Optional.empty(),
                                        Stream.concat(Stream.of(first), more.stream()).toList(), ret))));
    }

    // Solo mira la siguiente palabra, no consume nada
    private static boolean startsWith(Parser<InputString, TokenString, String> keyword, InputString source) {
        return keyword.parse(source) instanceof Ok<InputString, TokenString, String>;
    }

    // Se decide por la primera palabra, asi un error dentro de un MATCH o de un CREATE
    // se informa tal cual y no como "se esperaba otra cosa" de la otra alternativa.
    private static Parser<InputString, QueryNode, String> query() {
        Parser<InputString, TokenString, String> updatingKeyword = Parsers.Choice(
                CyphailLexers.Create(), CyphailLexers.Set(), CyphailLexers.Delete(),
                CyphailLexers.Detach(), CyphailLexers.Remove());
        return source -> {
            if (startsWith(CyphailLexers.Match(), source)) {
                return queryWithMatch().parse(source);
            }
            if (startsWith(updatingKeyword, source)) {
                return queryWithoutMatch().parse(source);
            }
            return new Fail<>("Expected MATCH, CREATE, SET, DELETE or REMOVE");
        };
    }

    public static Result<InputString, QueryNode, String> parse(String input) {
        return query().parse(new InputString(input, 0));
    }
}