package cr.ac.una.eif400.cyphail.broker;

import java.util.List;
import java.util.Map;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */


public sealed interface PrologResult {

    record Success(List<Map<String, Object>> rows) implements PrologResult {
        public Success {
            rows = List.copyOf(rows);
        }
    }

    record Failure() implements PrologResult {
    }

    record PrologError(Object term) implements PrologResult {
    }
}
