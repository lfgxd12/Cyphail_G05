package cr.ac.una.eif400.cyphail.codegen;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

// Lo que produce el Codegen para una sentencia:
//   goal       el texto Prolog que se envia al engine
//   columns    los nombres de las columnas de la tabla (a.name, o el alias si hay AS)
//   variables  el mapeo de variables (expresion original -> variable Prolog), para pruebas y depuracion
public record CodegenResult(String goal, List<String> columns, Map<String, String> variables) {
    public CodegenResult {
        columns = List.copyOf(columns);
        variables = Collections.unmodifiableMap(new LinkedHashMap<>(variables));
    }
}