package cr.ac.una.eif400.cyphail.validation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

// Tabla de simbolos de una consulta: que variables hay declaradas y de que tipo son.
// declare() aplica las reglas de nombres del SPEC y devuelve el error, si lo hay.
public final class SymbolTable {

    public enum Kind { NODE, RELATIONSHIP }

    // Prefijo reservado para las variables que genera el Codegen (__QueryId, __E1...)
    public static final String RESERVED_PREFIX = "__";

    private final Map<String, Kind> symbols = new LinkedHashMap<>();

    // Declara la variable. Un nodo puede repetirse (es una referencia al mismo nodo); una relacion no,
    // y un mismo nombre no puede ser nodo y relacion a la vez.
    public Optional<String> declare(String name, Kind kind) {
        Kind previous = symbols.putIfAbsent(name, kind);   // se registra aunque sea invalida, para no repetir errores
        if (name.startsWith(RESERVED_PREFIX)) {
            return Optional.of("Variable '" + name + "' is not allowed: names starting with '"
                    + RESERVED_PREFIX + "' are reserved for internal use.");
        }
        if (previous == null) {
            return Optional.empty();
        }
        if (previous != kind) {
            return Optional.of("Variable '" + name + "' is already used as a " + describe(previous)
                    + " and cannot also be a " + describe(kind) + ".");
        }
        if (kind == Kind.RELATIONSHIP) {
            return Optional.of("Relationship variable '" + name + "' is already declared; "
                    + "a relationship variable cannot be repeated.");
        }
        return Optional.empty();
    }

    public boolean contains(String name) {
        return symbols.containsKey(name);
    }

    public Optional<Kind> kindOf(String name) {
        return Optional.ofNullable(symbols.get(name));
    }

    // Copia de solo lectura, en el orden de declaracion
    public Map<String, Kind> table() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(symbols));
    }

    private static String describe(Kind kind) {
        return kind == Kind.NODE ? "node" : "relationship";
    }
}
