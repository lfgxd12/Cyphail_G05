package cr.ac.una.eif400.cyphail.frontend.handlers;

import cr.ac.una.eif400.cyphail.frontend.ReplCommand;

public class HelpHandler implements ReplCommand {
    @Override
    public void execute(String args) {
        System.out.println("""
            Available REPL Commands:
              .help             Show this help message
              .about            Show project authors and course details
              .use [graph]      List available graphs or select a graph
              .exit             Exit the REPL
            
            Engine Queries:
              Type Cyphail statements directly (e.g., MATCH (p:Persona) RETURN p.nombre, p.edad)
            """);
    }
}