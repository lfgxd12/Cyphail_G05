package cr.ac.una.eif400.cyphail.validation;

import cr.ac.una.eif400.cyphail.ast.QueryNode;

import java.util.List;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

// Nombre original del analisis de alcance (P1). Ahora lo hace el Analyzer, que ademas aplica las
// reglas de nombres de P2; esta clase se conserva para no cambiar a quienes ya la llaman.
public class VariableScopeChecker {

    public static List<String> check(QueryNode query) {
        return Analyzer.check(query);
    }

    public static void validate(QueryNode query) {
        Analyzer.validate(query);
    }
}
