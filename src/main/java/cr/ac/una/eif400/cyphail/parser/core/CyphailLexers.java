package cr.ac.una.eif400.cyphail.parser.core;

import java.util.regex.Pattern;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

public class CyphailLexers {

    private static final String SKIP = "(?:\\s|//[^\\n\\r]*|/\\*(?s:.*?)\\*/)*";
    private static final Pattern ONLY_SKIPPABLE = Pattern.compile(SKIP);

    private static Lexer token(String regex, TToken tokenType, String failMessage) {

        var pattern = Pattern.compile(SKIP + "(" + regex + ")");
        return (InputString source) -> {
            var matcher = pattern.matcher(source.input());
            matcher.region(source.index(), source.input().length());

            if (!matcher.lookingAt()) {
                return new Fail<>(failMessage);
            }

            var value = matcher.group(1);
            return new Ok<>(new TokenString(tokenType, value), new InputString(source.input(), matcher.end()));
        };
    }

    public static Lexer Eof() {
        return (InputString source) -> {
            var matcher = ONLY_SKIPPABLE.matcher(source.input());
            matcher.region(source.index(), source.input().length());
            if (matcher.matches()) {
                return new Ok<>(new TokenString(TToken.EOF, ""), new InputString(source.input(), matcher.end()));
            }
            return new Fail<>("Unexpected input: \"" + source.input().substring(source.index()).trim() + "\"");
        };
    }

    // Palabras clave
    public static Lexer Match()  { return token("(?i)MATCH\\b", TToken.MATCH, "Expected MATCH"); }
    public static Lexer Where()  { return token("(?i)WHERE\\b", TToken.WHERE, "Expected WHERE"); }
    public static Lexer Return() { return token("(?i)RETURN\\b", TToken.RETURN, "Expected RETURN"); }
    public static Lexer As()     { return token("(?i)AS\\b", TToken.AS, "Expected AS"); }
    public static Lexer Remove() { return token("(?i)REMOVE\\b", TToken.REMOVE, "Expected REMOVE"); }

    // Símbolos
    public static Lexer LParen() { return token("\\(", TToken.LPAREN, "Expected '('"); }
    public static Lexer RParen() { return token("\\)", TToken.RPAREN, "Expected ')'"); }
    public static Lexer Colon()  { return token(":", TToken.COLON, "Expected ':'"); }
    public static Lexer Dot()    { return token("\\.", TToken.DOT, "Expected '.'"); }
    public static Lexer Comma()  { return token(",", TToken.COMMA, "Expected ','"); }

    // Operadores
    public static Lexer Gt()     { return token(">", TToken.GT, "Expected '>'"); }
    public static Lexer Lt()     { return token("<", TToken.LT, "Expected '<'"); }
    public static Lexer Eq()     { return token("=", TToken.EQ, "Expected '='"); }
    public static Lexer Neq()    { return token("<>", TToken.NEQ, "Expected '<>'"); }

    // Identificadores y Literales
    public static Lexer Id()     { return token("[a-zA-Z_]\\w*", TToken.ID, "Expected Identifier"); }
    public static Lexer Number() { return token("[\\+-]?\\d+", TToken.NUM, "Expected Number"); }
    public static Lexer String() { return token("\"[^\"]*\"|'[^']*'", TToken.STRING, "Expected String"); }

    // Palabras Clave:
    public static Lexer Create() { return token("(?i)CREATE\\b", TToken.CREATE, "Expected CREATE"); }
    public static Lexer Delete() { return token("(?i)DELETE\\b", TToken.DELETE, "Expected DELETE"); }
    public static Lexer Detach() { return token("(?i)DETACH\\b", TToken.DETACH, "Expected DETACH"); }

    // Palabras Clave para Simbolos:
    public static Lexer LBrace() { return token("\\{", TToken.LBRACE, "Expected '{'"); }
    public static Lexer RBrace() { return token("\\}", TToken.RBRACE, "Expected '}'"); }

    // Palabras Clave para Operadores:
    public static Lexer Gte()    { return token(">=", TToken.GTE, "Expected '>='"); }
    public static Lexer Lte()    { return token("<=", TToken.LTE, "Expected '<='"); }
}