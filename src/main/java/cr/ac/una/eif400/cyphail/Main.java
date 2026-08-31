package cr.ac.una.eif400.cyphail;

import cr.ac.una.eif400.cyphail.frontend.Repl;

/**
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 *          Jostin Jimenez Alfaro, Angel Rojas Ruano
 */
public class Main {
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("repl")) {
            new Repl().start();
        } else {
            System.out.println("Usage: cyphail repl");
            System.exit(1);
        }
    }
}
