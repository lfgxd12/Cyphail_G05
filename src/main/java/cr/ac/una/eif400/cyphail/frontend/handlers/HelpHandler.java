package cr.ac.una.eif400.cyphail.frontend.handlers;

import cr.ac.una.eif400.cyphail.frontend.ReplCommand;

/**
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 *          Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

public class HelpHandler implements ReplCommand {
    @Override
    public void execute(String args) {
        System.out.println("""
            Available REPL Commands:
              .help             Show this help message
              .about            Show project authors and course details
              .use [graph]      List the engine graphs or select the graph in use
              .tree <query>     Show the AST of a query (SPEC tree format)
              .load "file"      Run every statement of a .cyphail script, in order
              .exit             Exit the REPL

            Engine Queries:
              Type Cyphail statements directly (e.g., MATCH (p:Persona) RETURN p.nombre, p.edad)
              Statements may span several lines: they end with ";" (or an empty line).
            """);
    }
}
