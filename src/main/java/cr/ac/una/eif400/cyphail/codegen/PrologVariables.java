package cr.ac.una.eif400.cyphail.codegen;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

// El "mapeo de variables" del SPEC P2: asocia cada variable (o propiedad) del query en Cyphail
// con la variable Prolog que genera el Codegen.
//
//   a        ->  __A        (la variable en mayuscula, con el prefijo __)
//   a.name   ->  __Aname    (la variable en mayuscula y la propiedad tal cual)
//
// Dos expresiones distintas nunca reciben la misma variable Prolog: si el nombre ya lo usa otra
// (por ejemplo "a" y "A", o la variable "aname" y la propiedad "a.name"), se le agrega un sufijo:
// __A, __A_2. La misma expresion siempre recibe la misma variable.

public final class PrologVariables {

    // Variable del sistema que lleva el id de la consulta (SPEC: cy_create_query(__QueryId, ...))
    public static final String QUERY_ID = "__QueryId";

    private final Map<String, String> assigned = new LinkedHashMap<>();   // expresion original -> variable Prolog
    private final Set<String> used = new HashSet<>(Set.of(QUERY_ID));    // nombres ya ocupados (incluye los del sistema)
    private int relationships = 0;

    // a -> __A
    public String variable(String name) {
        return assign(name, "__" + capitalize(name));
    }

    // a.name -> __Aname
    public String property(String variable, String property) {
        return assign(variable + "." + property, "__" + capitalize(variable) + property);
    }

    // __E1, __E2, ... para las relaciones sin nombre. Salta los nombres que ya estan ocupados.
    public String nextRelationship() {
        String name;
        do {
            relationships++;
            name = "__E" + relationships;
        } while (used.contains(name));
        used.add(name);
        return name;
    }

    // Copia de solo lectura del mapeo, en el orden en que se asigno
    public Map<String, String> table() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(assigned));
    }

    private String assign(String expression, String candidate) {
        return assigned.computeIfAbsent(expression, key -> freeName(candidate));
    }

    // El candidato si esta libre; si no, candidato_2, candidato_3...
    private String freeName(String candidate) {
        String name = candidate;
        for (int n = 2; used.contains(name); n++) {
            name = candidate + "_" + n;
        }
        used.add(name);
        return name;
    }

    private static String capitalize(String text) {
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}