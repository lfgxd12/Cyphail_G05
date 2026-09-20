package cr.ac.una.eif400.cyphail.parser.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

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
}