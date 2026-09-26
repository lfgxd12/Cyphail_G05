package cr.ac.una.eif400.cyphail.frontend;

import cr.ac.una.eif400.cyphail.engine.FakeQueryHandler;
import cr.ac.una.eif400.cyphail.frontend.handlers.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

public class Repl {
    private static final String BANNER = """
        Welcome to Cyphail-G05 v.0.1. August 2026. ESCINF/UNA EIF400-II-2026
        Visit www.whatiscyphail.com for more information
        Type ".help" for more information and commands
        Type ".exit" to quit
        Queries may span several lines: they run when complete (with RETURN),
        or finish them with ";" or an empty line.
        """;

    private final Map<String, ReplCommand> commands = new HashMap<>();

    public Repl() {
        commands.put(".help", new HelpHandler());
        commands.put(".about", new AboutHandler());
        commands.put(".use", new UseHandler());
        commands.put(".tree", new TreeHandler());
    }

    public void start() {
        System.out.println(BANNER);
        Scanner scanner = new Scanner(System.in);
        StringBuilder pending = new StringBuilder(); // consulta de varias lineas en construccion

        while (true) {
            System.out.print(pending.isEmpty() ? ">>> " : "... ");
            if (!scanner.hasNextLine()) break;

            String line = scanner.nextLine().strip();

            // Comandos de una sola linea (.help, .about, .use, .exit): se ejecutan de inmediato
            if (pending.isEmpty() && line.startsWith(".") && !MultilineInput.isTreeCommand(line)) {
                if (!runCommand(line)) break;
                continue;
            }

            // Linea vacia: ejecuta lo pendiente, si hay algo
            if (line.isEmpty()) {
                if (!pending.isEmpty()) {
                    runStatement(pending.toString());
                    pending.setLength(0);
                }
                continue;
            }

            // Consulta o .tree: se acumula hasta que este completa
            if (!pending.isEmpty()) {
                pending.append('\n');
            }
            pending.append(line);
            if (MultilineInput.isComplete(pending.toString())) {
                runStatement(pending.toString());
                pending.setLength(0);
            }
        }
    }

    // Ejecuta un comando de una linea. Devuelve false si hay que salir del REPL (.exit)
    private boolean runCommand(String line) {
        String[] parts = line.split("\\s+", 2);
        String cmdName = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";

        if (cmdName.equals(".exit")) {
            new ExitHandler().execute("");
            return false;
        }

        ReplCommand cmd = commands.get(cmdName);
        if (cmd != null) {
            cmd.execute(args);
        } else {
            System.out.println("Unknown REPL command: " + cmdName + ". Type .help for available commands.");
        }
        return true;
    }

    // Ejecuta una entrada completa (posiblemente de varias lineas): .tree o consulta al motor fake
    // Pd. arreglar en el siguiente sprint
    private void runStatement(String text) {
        String statement = MultilineInput.clean(text);
        if (MultilineInput.isTreeCommand(statement)) {
            commands.get(".tree").execute(MultilineInput.withoutTreeCommand(statement));
        } else {
            FakeQueryHandler.process(statement);
        }
    }
}