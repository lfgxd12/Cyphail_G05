package cr.ac.una.eif400.cyphail.frontend.handlers;

import cr.ac.una.eif400.cyphail.frontend.ReplCommand;

public class ExitHandler implements ReplCommand {
    @Override
    public void execute(String args) {
        System.out.println("Goodbye! Exiting Cyphail REPL...");
    }
}