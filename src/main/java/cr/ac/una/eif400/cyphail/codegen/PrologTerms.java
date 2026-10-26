package cr.ac.una.eif400.cyphail.codegen;

import cr.ac.una.eif400.cyphail.ast.LiteralExpr;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

// Convierte valores de Cyphail en terminos de Prolog (texto). Son funciones puras.
public final class PrologTerms {

    // Un atomo que Prolog acepta sin comillas: empieza con minuscula y sigue con letras, digitos o _
    private static final Pattern PLAIN_ATOM = Pattern.compile("[a-z][a-zA-Z0-9_]*");

    private PrologTerms() {
    }

    // amigo -> amigo     Juan Perez -> 'Juan Perez'     Person -> 'Person'     it's -> 'it\'s'
    // Solo lleva comillas cuando hace falta, como en los ejemplos del SPEC (amigo, age).
    public static String atom(String text) {
        return PLAIN_ATOM.matcher(text).matches() ? text : quoted(text);
    }

    // Siempre entre comillas simples, escapando lo que Prolog no admite tal cual
    public static String quoted(String text) {
        return "'" + text.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "'";
    }

    // +5 -> 5     -3 -> -3     19.99 -> 19.99     (Prolog no acepta un + delante de un numero)
    public static String number(String text) {
        return text.startsWith("+") ? text.substring(1) : text;
    }

    // Un literal de Cyphail: un numero queda numero y un string queda atomo
    public static String literal(LiteralExpr literal) {
        return literal.isNumber() ? number(literal.text()) : atom(literal.text());
    }

    // ['a.name', 'b.name']
    public static String atomList(List<String> items) {
        return items.stream().map(PrologTerms::quoted).collect(Collectors.joining(", ", "[", "]"));
    }
}