package cr.ac.una.eif400.cyphail.frontend.handlers;

import cr.ac.una.eif400.cyphail.frontend.ReplCommand;

/**
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 *          Jostin Jimenez Alfaro, Angel Rojas Ruano
 */
public class ExitHandler implements ReplCommand {
    @Override
    public void execute(String args) {
        System.out.println("Goodbye! Exiting Cyphail REPL...");
    }
}
