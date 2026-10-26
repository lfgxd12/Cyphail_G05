package cr.ac.una.eif400.cyphail.ast;

import java.util.List;
import java.util.Optional;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

// Una relacion dentro de un patron: -[r:TIPO {clave: valor}]->
// El tipo es una lista porque la gramatica permite alternativas [:A|B].
public record RelPattern(Optional<String> variable, List<String> types,
                         List<Property> properties, Direction direction) {
    public RelPattern {
        types = List.copyOf(types);
        properties = List.copyOf(properties);
    }
}