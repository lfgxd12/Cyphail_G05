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

// Un patron de nodo: (variable:Etiqueta1:Etiqueta2 {clave: valor, ...})
// La variable es opcional segun la gramatica: nodePattern : "(" variable? nodeLabels? properties? ")"
public record NodePattern(Optional<String> variable, List<String> labels, List<Property> properties) {
    public NodePattern {
        labels = List.copyOf(labels);
        properties = List.copyOf(properties);
    }
}