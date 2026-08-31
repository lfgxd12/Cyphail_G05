package cr.ac.una.eif400.cyphail.frontend;

import cr.ac.una.eif400.cyphail.engine.FakeQueryHandler;
import cr.ac.una.eif400.cyphail.frontend.handlers.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 *          Jostin Jimenez Alfaro, Angel Rojas Ruano
 */
public class Repl {
    private static final String BANNER = """
        Welcome to Cyphail-05-10am v.0.1. August 2026. ESCINF/UNA EIF400-II-2026
        Visit www.whatiscyphail.com for more information
        Type ".help" for more information and commands
        Type ".exit" to quit
        """;

    private final Map<String, ReplCommand> commands = new HashMap<>();

    public Repl() {
        // Registrar comandos disponibles con .
        commands.put(".help", new HelpHandler());
        commands.put(".about", new AboutHandler());
        commands.put(".use", new UseHandler());
    }

    public void start() {
        System.out.println(BANNER);
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print(">>> ");
            if (!scanner.hasNextLine()) break;

            String line = scanner.nextLine().trim();

            if (line.isEmpty()) {
                continue; // Comando nulo: vuelve a imprimir prompt
            }

            if (line.startsWith(".")) {
                String[] parts = line.split("\\s+", 2);
                String cmdName = parts[0].toLowerCase();
                String args = parts.length > 1 ? parts[1] : "";

                if (cmdName.equals(".exit")) {
                    new ExitHandler().execute("");
                    break;
                }

                ReplCommand cmd = commands.get(cmdName);
                if (cmd != null) {
                    cmd.execute(args);
                } else {
                    System.out.println("Unknown REPL command: " + cmdName + ". Type .help for available commands.");
                }
            } else {
                // Consulta MATCH fingida para el motor Cyphail
                FakeQueryHandler.process(line);
            }
        }
    }
}
