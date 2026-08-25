package cr.ac.una.eif400.cyphail;

import cr.ac.una.eif400.cyphail.frontend.Repl;

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