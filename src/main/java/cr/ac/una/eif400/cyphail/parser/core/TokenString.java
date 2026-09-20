package cr.ac.una.eif400.cyphail.parser.core;

public record TokenString(TToken type, String value) implements IToken<String> {
}