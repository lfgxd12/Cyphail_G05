package cr.ac.una.eif400.cyphail.parser;

import cr.ac.una.eif400.cyphail.ast.*;
import cr.ac.una.eif400.cyphail.parser.core.*;

import java.util.List;

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

    // ---------------- Expresiones ----------------

    // propertyLookup : variable "." propiedad      ej. m.title
    private static Parser<InputString, PropertyLookup, String> propertyLookup() {
        return Parsers.FlatMap(ID, variable ->
                Parsers.Map(Parsers.Right(CyphailLexers.Dot(), ID),
                        property -> new PropertyLookup(new VariableExpr(variable), property)));
    }

    // atom : m.title | m | 300 | "texto"
    // propertyLookup va primero: si "m" se leyera como variable, ".title" quedaria suelto
    private static Parser<InputString, Expression, String> atom() {
        return Parsers.Choice(
                Parsers.Map(propertyLookup(), lookup -> lookup),
                Parsers.Map(ID, VariableExpr::new),
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

    // expression : atom (operador atom)?
    // Si despues del atom viene un operador, se arma un BinaryExpr; si no, queda el atom solo
    public static Parser<InputString, Expression, String> expression() {
        return Parsers.FlatMap(atom(), left ->
                Parsers.Map(
                        Parsers.Opt(Parsers.FlatMap(comparisonOp(), op ->
                                Parsers.Map(atom(), right -> new BinaryExpr(left, op.value(), right)))),
                        comparison -> comparison.<Expression>map(binary -> binary).orElse(left)));
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

    // pattern : nodePattern ("," nodePattern)*
    private static Parser<InputString, List<NodePattern>, String> pattern() {
        return Parsers.SepBy1(nodePattern(), CyphailLexers.Comma());
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

    // updatingClause : createClause | deleteClause | removeClause
    private static Parser<InputString, UpdatingClause, String> updatingClause() {
        return Parsers.Choice(createClause(), deleteClause(), removeClause());
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

    // query : MATCH [WHERE] updatingClause* [RETURN] EOF
    // El orden sigue la gramatica: readingClause* updatingClause* returnClause?
    // EOF al final garantiza que no sobre texto sin leer
    private static Parser<InputString, QueryNode, String> query() {
        return Parsers.FlatMap(matchClause(), match ->
                Parsers.FlatMap(Parsers.Opt(whereClause()), where ->
                        Parsers.FlatMap(Parsers.Many(updatingClause()), updates ->
                                Parsers.Map(Parsers.Left(Parsers.Opt(returnClause()), CyphailLexers.Eof()),
                                        ret -> new QueryNode(match, where, updates, ret)))));
    }

    public static Result<InputString, QueryNode, String> parse(String input) {
        return query().parse(new InputString(input, 0));
    }
}