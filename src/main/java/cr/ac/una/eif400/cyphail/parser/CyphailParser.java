package cr.ac.una.eif400.cyphail.parser;

import cr.ac.una.eif400.cyphail.ast.*;
import cr.ac.una.eif400.cyphail.parser.core.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CyphailParser {

    private static Parser<InputString, MatchClause, String> matchClause() {
        var seq = Parsers.Seq(
                CyphailLexers.Match(),
                CyphailLexers.LParen(),
                CyphailLexers.Id(),
                CyphailLexers.Colon(),
                CyphailLexers.Id(),     // etiqueta, ej: "Movie"
                CyphailLexers.RParen()
        );
        return Parsers.Map(seq, tokens ->
                new MatchClause(tokens.get(2).value(), tokens.get(4).value())
        );
    }

    private static Parser<InputString, Expression, String> atom() {
        Parser<InputString, Expression, String> propertyLookup = Parsers.Map(
                Parsers.Seq(CyphailLexers.Id(), CyphailLexers.Dot(), CyphailLexers.Id()),
                tokens -> new PropertyLookup(new VariableExpr(tokens.get(0).value()), tokens.get(2).value())
        );

        Parser<InputString, Expression, String> variable = Parsers.Map(
                CyphailLexers.Id(), t -> new VariableExpr(t.value())
        );

        Parser<InputString, Expression, String> numberLit = Parsers.Map(
                CyphailLexers.Number(), t -> new LiteralExpr(t.value())
        );

        Parser<InputString, Expression, String> stringLit = Parsers.Map(
                CyphailLexers.String(), t -> new LiteralExpr(t.value())
        );

        return Parsers.Or(propertyLookup, Parsers.Or(variable, Parsers.Or(numberLit, stringLit)));
    }

    private static Parser<InputString, TokenString, String> comparisonOp() {
        return Parsers.Or(CyphailLexers.Gt(),
                Parsers.Or(CyphailLexers.Lt(),
                        Parsers.Or(CyphailLexers.Eq(), CyphailLexers.Neq())));
    }

    public static Parser<InputString, Expression, String> expression() {
        return (InputString source) -> {
            var leftResult = atom().parse(source);
            if (leftResult instanceof Fail<InputString, Expression, String> fail) {
                return fail;
            }
            var leftOk = (Ok<InputString, Expression, String>) leftResult;
            Expression left = leftOk.token();
            InputString rest = leftOk.rest();

            var opResult = comparisonOp().parse(rest);
            if (opResult instanceof Fail<InputString, TokenString, String>) {
                return new Ok<>(left, rest);
            }
            var opOk = (Ok<InputString, TokenString, String>) opResult;

            return switch (atom().parse(opOk.rest())) {
                case Fail(String reason) -> new Fail<>(reason);
                case Ok(Expression right, InputString afterRight) ->
                        new Ok<>(new BinaryExpr(left, opOk.token().value(), right), afterRight);
            };
        };
    }

    private static Parser<InputString, ReturnItem, String> returnItem() {
        return (InputString source) -> switch (expression().parse(source)) {
            case Fail(String reason) -> new Fail<>(reason);
            case Ok(Expression expr, InputString afterExpr) -> {
                var aliasResult = Parsers.Opt(
                        Parsers.Map(CyphailLexers.As(), t -> t) // solo para consumir el AS
                ).parse(afterExpr);
                // Si hubo AS, necesitamos el identificador que sigue
                if (aliasResult instanceof Ok(Optional<TokenString> asToken, InputString afterAs) && asToken.isPresent()) {
                    yield switch (CyphailLexers.Id().parse(afterAs)) {
                        case Fail(String reason) -> new Fail<>(reason);
                        case Ok(TokenString aliasId, InputString afterAliasId) ->
                                new Ok<>(new ReturnItem(expr, Optional.of(aliasId.value())), afterAliasId);
                    };
                } else {
                    yield new Ok<>(new ReturnItem(expr, Optional.empty()), afterExpr);
                }
            }
        };
    }

    private static Parser<InputString, ReturnItem, String> commaThenItem() {
        return (InputString source) -> {
            var commaResult = CyphailLexers.Comma().parse(source);
            if (commaResult instanceof Fail<InputString, TokenString, String> fail) {
                return new Fail<>(fail.reason());
            }
            return returnItem().parse(((Ok<InputString, TokenString, String>) commaResult).rest());
        };
    }

    private static Parser<InputString, ReturnClause, String> returnClause() {
        return (InputString source) -> {
            var returnTokenResult = CyphailLexers.Return().parse(source);
            if (returnTokenResult instanceof Fail<InputString, TokenString, String> fail) {
                return new Fail<>(fail.reason());
            }
            InputString afterReturn = ((Ok<InputString, TokenString, String>) returnTokenResult).rest();

            return switch (returnItem().parse(afterReturn)) {
                case Fail(String reason) -> new Fail<>(reason);
                case Ok(ReturnItem first, InputString afterFirst) -> switch (Parsers.Many(commaThenItem()).parse(afterFirst)) {
                    case Fail(String reason) -> new Fail<>(reason);
                    case Ok(List<ReturnItem> more, InputString afterAll) -> {
                        var items = new ArrayList<ReturnItem>();
                        items.add(first);
                        items.addAll(more);
                        yield new Ok<>(new ReturnClause(items), afterAll);
                    }
                };
            };
        };
    }

    private static Parser<InputString, WhereClause, String> whereClause() {
        return (InputString source) -> {
            var whereResult = CyphailLexers.Where().parse(source);
            if (whereResult instanceof Fail<InputString, TokenString, String> fail) {
                return new Fail<>(fail.reason());
            }
            InputString rest = ((Ok<InputString, TokenString, String>) whereResult).rest();

            return switch (expression().parse(rest)) {
                case Fail(String reason) -> new Fail<>(reason);
                case Ok(Expression cond, InputString afterExpr) -> new Ok<>(new WhereClause(cond), afterExpr);
            };
        };
    }

    private static Parser<InputString, PropertyLookup, String> propertyLookupOnly() {
        return Parsers.Map(
                Parsers.Seq(CyphailLexers.Id(), CyphailLexers.Dot(), CyphailLexers.Id()),
                tokens -> new PropertyLookup(new VariableExpr(tokens.get(0).value()), tokens.get(2).value())
        );
    }

    private static Parser<InputString, PropertyLookup, String> commaThenProperty() {
        return (InputString source) -> {
            var commaResult = CyphailLexers.Comma().parse(source);
            if (commaResult instanceof Fail<InputString, TokenString, String> fail) {
                return new Fail<>(fail.reason());
            }
            return propertyLookupOnly().parse(((Ok<InputString, TokenString, String>) commaResult).rest());
        };
    }

    private static Parser<InputString, RemoveClause, String> removeClause() {
        return (InputString source) -> {
            var removeResult = CyphailLexers.Remove().parse(source);
            if (removeResult instanceof Fail<InputString, TokenString, String> fail) {
                return new Fail<>(fail.reason());
            }
            InputString afterRemove = ((Ok<InputString, TokenString, String>) removeResult).rest();

            return switch (propertyLookupOnly().parse(afterRemove)) {
                case Fail(String reason) -> new Fail<>(reason);
                case Ok(PropertyLookup first, InputString afterFirst) -> switch (Parsers.Many(commaThenProperty()).parse(afterFirst)) {
                    case Fail(String reason) -> new Fail<>(reason);
                    case Ok(List<PropertyLookup> more, InputString afterAll) -> {
                        var items = new ArrayList<PropertyLookup>();
                        items.add(first);
                        items.addAll(more);
                        yield new Ok<>(new RemoveClause(items), afterAll);
                    }
                };
            };
        };
    }

    public static Result<InputString, QueryNode, String> parse(String input) {
        InputString source = new InputString(input, 0);

        return switch (matchClause().parse(source)) {
            case Fail(String reason) -> new Fail<>(reason);
            case Ok(MatchClause match, InputString afterMatch) -> switch (Parsers.Opt(whereClause()).parse(afterMatch)) {
                case Fail(String reason) -> new Fail<>(reason);
                case Ok(Optional<WhereClause> where, InputString afterWhere) -> switch (Parsers.Opt(returnClause()).parse(afterWhere)) {
                    case Fail(String reason) -> new Fail<>(reason);
                    case Ok(Optional<ReturnClause> ret, InputString afterReturn) -> switch (Parsers.Opt(removeClause()).parse(afterReturn)) {
                        case Fail(String reason) -> new Fail<>(reason);
                        case Ok(Optional<RemoveClause> remove, InputString afterRemove) ->
                                new Ok<>(new QueryNode(match, where, ret, remove), afterRemove);
                    };
                };
            };
        };
    }
}


