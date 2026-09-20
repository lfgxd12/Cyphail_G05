package cr.ac.una.eif400.cyphail.parser.core;

@SuppressWarnings("unused")
public sealed interface Result<I, T, R> permits Ok, Fail {
}