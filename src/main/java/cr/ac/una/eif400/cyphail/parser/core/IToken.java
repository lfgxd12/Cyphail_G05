package cr.ac.una.eif400.cyphail.parser.core;

public interface IToken<T> {
    TToken type();
    T value();
}