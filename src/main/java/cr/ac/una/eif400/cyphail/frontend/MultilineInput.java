package cr.ac.una.eif400.cyphail.frontend;

import cr.ac.una.eif400.cyphail.ast.QueryNode;
import cr.ac.una.eif400.cyphail.parser.CyphailParser;
import cr.ac.una.eif400.cyphail.parser.core.Fail;
import cr.ac.una.eif400.cyphail.parser.core.Ok;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

public final class MultilineInput {

    private static final String TREE_COMMAND = ".tree";

    private MultilineInput() {
    }

    public static boolean isComplete(String text) {
        String trimmed = text.strip();
        if (trimmed.endsWith(";")) {
            return true;
        }
        String query = isTreeCommand(trimmed) ? withoutTreeCommand(trimmed) : trimmed;
        return switch (CyphailParser.parse(query)) {
            case Ok(QueryNode node, var rest) -> node.returnClause().isPresent();
            case Fail(String reason) -> false;
        };
    }

    // Quita espacios de los extremos y el ';' final (si lo hay)
    public static String clean(String text) {
        String trimmed = text.strip();
        return trimmed.endsWith(";") ? trimmed.substring(0, trimmed.length() - 1).strip() : trimmed;
    }

    // ".tree", ".tree MATCH ..." o ".tree" seguido de un salto de linea
    public static boolean isTreeCommand(String text) {
        return text.strip().split("\\s+", 2)[0].equalsIgnoreCase(TREE_COMMAND);
    }

    // ".tree MATCH (m) ..."  ->  "MATCH (m) ..."
    public static String withoutTreeCommand(String text) {
        String[] parts = text.strip().split("\\s+", 2);
        return parts.length > 1 ? parts[1] : "";
    }
}