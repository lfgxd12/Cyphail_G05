package cr.ac.una.eif400.cyphail.frontend;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

// Corta un texto en sentencias por el ";" final de cada una. Un ";" dentro de un string
// ('a;b' o "a;b") o de un comentario (// ... o /* ... */) no termina nada.
// Son funciones puras: las usan el REPL (lineas que se van acumulando) y .load (un archivo completo).
public final class Statements {

    // Las sentencias ya terminadas y el texto que quedo sin terminar (puede ser vacio)
    public record Split(List<String> complete, String rest) {
    }

    private static final Pattern COMMENTS = Pattern.compile("//[^\\n\\r]*|/\\*(?s:.*?)\\*/");

    private Statements() {
    }

    public static Split split(String text) {
        List<String> complete = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        char quote = 0;              // comilla del string abierto, o 0 si no hay
        boolean lineComment = false;
        boolean blockComment = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            char next = i + 1 < text.length() ? text.charAt(i + 1) : 0;

            if (lineComment) {
                lineComment = c != '\n';
            } else if (blockComment) {
                if (c == '*' && next == '/') {
                    current.append(c);
                    c = next;
                    i++;
                    blockComment = false;
                }
            } else if (quote != 0) {
                if (c == '\\' && next != 0) {
                    current.append(c);
                    c = next;
                    i++;
                } else if (c == quote) {
                    quote = 0;
                }
            } else if (c == '\'' || c == '"') {
                quote = c;
            } else if (c == '/' && next == '/') {
                lineComment = true;
            } else if (c == '/' && next == '*') {
                blockComment = true;
            } else if (c == ';') {
                addIfNotEmpty(complete, current.toString());
                current.setLength(0);
                continue;
            }
            current.append(c);
        }
        String rest = current.toString();
        return new Split(complete, isBlank(rest) ? "" : rest.strip());
    }

    // Todas las sentencias de un texto; lo que quede sin ";" al final tambien cuenta como una
    public static List<String> all(String text) {
        Split split = split(text);
        List<String> statements = new ArrayList<>(split.complete());
        addIfNotEmpty(statements, split.rest());
        return statements;
    }

    private static void addIfNotEmpty(List<String> statements, String statement) {
        if (!isBlank(statement)) {
            statements.add(statement.strip());
        }
    }

    // Vacia, o con solo comentarios
    private static boolean isBlank(String text) {
        return COMMENTS.matcher(text).replaceAll("").isBlank();
    }
}
