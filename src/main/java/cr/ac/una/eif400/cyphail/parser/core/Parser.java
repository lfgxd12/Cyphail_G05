package cr.ac.una.eif400.cyphail.parser.core;

@FunctionalInterface
public interface Parser<I, T, R> {
    Result<I, T, R> parse(I source);
}