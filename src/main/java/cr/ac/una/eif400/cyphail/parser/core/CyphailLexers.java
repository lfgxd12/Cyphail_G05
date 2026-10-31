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

    // Cuantificadores posesivos (*+): sin ellos el motor puede "devolver" parte de un comentario y leer
    // como token una palabra que esta dentro de el (// ... CREATE sin MATCH ...)
    private static final String SKIP = "(?:\\s|//[^\\n\\r]*+|/\\*(?s:.*?)\\*/)*+";
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
    public static Lexer Number() { return token("[\\+-]?\\d+(?:\\.\\d+)?", TToken.NUM, "Expected Number"); }
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

    // ---------------- P2: relaciones, SET y operadores logicos ----------------

    // Palabra clave SET (el token SET ya existia en TToken)
    public static Lexer Set()    { return token("(?i)SET\\b", TToken.SET, "Expected SET"); }

    // Operadores logicos, sin distinguir mayusculas; \\b evita leer "ANDROID" como AND
    public static Lexer And()    { return token("(?i)AND\\b", TToken.AND, "Expected AND"); }
    public static Lexer Or()     { return token("(?i)OR\\b", TToken.OR, "Expected OR"); }
    public static Lexer Not()    { return token("(?i)NOT\\b", TToken.NOT, "Expected NOT"); }

    // Corchetes del detalle de una relacion: [r:TIPO {clave: valor}]
    public static Lexer LBracket() { return token("\\[", TToken.LBRACKET, "Expected '['"); }
    public static Lexer RBracket() { return token("\\]", TToken.RBRACKET, "Expected ']'"); }

    // Tipos alternativos de relacion: [:A|B]
    public static Lexer Pipe()   { return token("\\|", TToken.PIPE, "Expected '|'"); }

    // Flechas. OJO: "->" y "<-" miden 2 caracteres. El parser debe probar Arrow ANTES que Dash
    // (si no, Dash se come el guion y sobra ">"). Dash se usa solo donde se espera una relacion.
    public static Lexer Dash()   { return token("-", TToken.DASH, "Expected '-'"); }
    public static Lexer Arrow()  { return token("->", TToken.ARROW, "Expected '->'"); }
    public static Lexer LArrow() { return token("<-", TToken.LARROW, "Expected '<-'"); }
}
