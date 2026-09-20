package cr.ac.una.eif400.cyphail.parser.core;

import java.util.regex.Pattern;

public class CyphailLexers {

    private static Lexer token(String regex, TToken tokenType, String failMessage) {
        var pattern = Pattern.compile("\\s*(" + regex + ")");
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
}