package cr.ac.una.eif400.cyphail.parser.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;


/*
    * Cyphail - Graph Query Engine Prototype
    * EIF400-II-2026 - Escuela de Informatica, UNA
    * Grupo: G05
    * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
    * Jostin Jimenez Alfaro, Angel Rojas Ruano
*/

public class Parsers {

    public static <I, T, R> Parser<I, T, R> Or(Parser<I, T, R> p, Parser<I, T, R> q) {
        return (I source) -> {
            var result = p.parse(source);
            if (!(result instanceof Fail<I, T, R>)) {
                return result;
            }
            return q.parse(source);
        };
    }

    @SafeVarargs
    public static <I, T, R> Parser<I, List<T>, R> Seq(Parser<I, T, R>... parsers) {
        return (I source) -> {
            List<T> results = new ArrayList<>();
            for (var p : parsers) {
                switch (p.parse(source)) {
                    case Fail(R reason) -> {
                        return new Fail<>(reason);
                    }
                    case Ok(T token, I rest) -> {
                        results.add(token);
                        source = rest;
                    }
                }
            }
            return new Ok<>(results, source);
        };
    }

    public static <I, T, U, R> Parser<I, U, R> Map(Parser<I, T, R> p, Function<T, U> mapper) {
        return (I source) -> switch (p.parse(source)) {
            case Ok(T token, I rest) -> new Ok<>(mapper.apply(token), rest);
            case Fail(R reason) -> new Fail<>(reason);
        };
    }

    public static <I, T, R> Parser<I, Optional<T>, R> Opt(Parser<I, T, R> p) {
        return (I source) -> switch (p.parse(source)) {
            case Ok(T token, I rest) -> new Ok<>(Optional.of(token), rest);
            case Fail(R reason) -> new Ok<>(Optional.empty(), source);
        };
    }

    public static <I, T, R> Parser<I, List<T>, R> Many(Parser<I, T, R> p) {
        return (I source) -> {
            List<T> results = new ArrayList<>();
            I currentSource = source;
            while (true) {
                var res = p.parse(currentSource);
                if (res instanceof Ok(T token, I rest)) {
                    results.add(token);
                    currentSource = rest;
                } else {
                    break;
                }
            }
            return new Ok<>(results, currentSource);
        };
    }

    // FlatMap lee p según lo que salió, luego ve si dependiendo si ok o fail, aplica f o no.
    // Si p falla, no aplica f y devuelve fail.
    public static <I, T, U, R> Parser<I, U, R> FlatMap(Parser<I, T, R> p, Function<T, Parser<I, U, R>> f) {
        return (I source) -> switch (p.parse(source)) {
            case Ok(T token, I rest) -> f.apply(token).parse(rest);
            case Fail(R reason) -> new Fail<>(reason);
        };
    }

    // Right agarra el parser p y q y devuelve otro parser pero que
    // conserva solo el resultado de q
    public static <I, A, B, R> Parser<I, B, R> Right(Parser<I, A, R> p, Parser<I, B, R> q) {
        return FlatMap(p, ignored -> q);
    }

    // Left agarra el parser p y q y devuelve otro parser pero que
    // conserva solo el resultado de p
    public static <I, A, B, R> Parser<I, A, R> Left(Parser<I, A, R> p, Parser<I, B, R> q) {
        return FlatMap(p, a -> Map(q, ignored -> a));
    }

    // Between ayuda a leer lo que esté dentro de "(, ), {, }"
    public static <I, A, T, C, R> Parser<I, T, R> Between(Parser<I, A, R> open, Parser<I, T, R> p, Parser<I, C, R> close) {
        return Left(Right(open, p), close);
    }

    // SepBy1 lee 1 o más "p" parsers separados por "sep" reconocido como un separador
    // Ya después retorna una lista de resultados de p. Pero si no hay al menos un p, falla
    public static <I, T, S, R> Parser<I, List<T>, R> SepBy1(Parser<I, T, R> p, Parser<I, S, R> sep) {
        return FlatMap(p, first ->
                Map(Many(Right(sep, p)), others ->
                        Stream.concat(Stream.of(first), others.stream()).toList()));
    }

    // SepBy lee 0 o más "p" parsers separados por "sep" reconocido como un separador
    // Ya después retorna una lista de resultados de p. Pero si no hay ninguno, devuelve una lista vacía
    public static <I, T, S, R> Parser<I, List<T>, R> SepBy(Parser<I, T, R> p, Parser<I, S, R> sep) {
        return Map(Opt(SepBy1(p, sep)), found -> found.orElse(List.of()));
    }
}