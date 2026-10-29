package cr.ac.una.eif400.cyphail.frontend;

import cr.ac.una.eif400.cyphail.ast.QueryNode;
import cr.ac.una.eif400.cyphail.broker.EngineBroker;
import cr.ac.una.eif400.cyphail.broker.EngineException;
import cr.ac.una.eif400.cyphail.broker.GoalRunner;
import cr.ac.una.eif400.cyphail.broker.RowsTable;
import cr.ac.una.eif400.cyphail.codegen.PredicateNames;
import cr.ac.una.eif400.cyphail.codegen.PrologCodegen;
import cr.ac.una.eif400.cyphail.frontend.handlers.*;
import cr.ac.una.eif400.cyphail.output.TablePrinter;
import cr.ac.una.eif400.cyphail.parser.CyphailParser;
import cr.ac.una.eif400.cyphail.parser.core.Fail;
import cr.ac.una.eif400.cyphail.parser.core.Ok;
import cr.ac.una.eif400.cyphail.validation.Analyzer;
import cr.ac.una.eif400.cyphail.validation.SemanticException;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Supplier;

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
    private final Session session;

    public Repl() {
        this(EngineBroker::start);
    }

    // El engine se arranca hasta la primera consulta; las pruebas pasan uno falso
    public Repl(Supplier<GoalRunner> engine) {
        this.session = new Session(engine);
        commands.put(".help", new HelpHandler());
        commands.put(".about", new AboutHandler());
        commands.put(".use", new UseHandler(session));
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
        session.close();
    }

    // Ejecuta un comando de una linea. Devuelve false si hay que salir del REPL (.exit)
    boolean runCommand(String line) {
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

    void runStatement(String text) {
        String statement = MultilineInput.clean(text);

        if (MultilineInput.isTreeCommand(statement)) {
            commands.get(".tree").execute(MultilineInput.withoutTreeCommand(statement));
            return;
        }

        switch (CyphailParser.parse(statement)) {
            case Fail(String reason) -> System.out.println("ERROR: Syntax error. " + reason);
            case Ok(QueryNode query, var rest) -> execute(query);
        }
    }

    // Analyzer -> Codegen -> engine -> tabla. Cada etapa que falla se informa con su propio mensaje.
    private void execute(QueryNode query) {
        try {
            Analyzer.validate(query);
            String goal = new PrologCodegen(new PredicateNames(session.graph())).generate(query).goal();
            long start = System.nanoTime();
            RowsTable table = RowsTable.parse(session.runner().run(goal));
            long millis = (System.nanoTime() - start) / 1_000_000;
            print(table, millis);
        } catch (SemanticException e) {
            System.out.println("ERROR: " + e.getMessage());
        } catch (UnsupportedOperationException e) {
            System.out.println("ERROR: Not supported yet: " + e.getMessage());
        } catch (EngineException e) {
            System.out.println("ERROR: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("ERROR: The engine answered with an unexpected format. " + e.getMessage());
        }
    }

    private void print(RowsTable table, long millis) {
        if (table.columns().isEmpty()) {
            System.out.println("OK. Query executed in " + millis + " ms.");
            return;
        }
        TablePrinter.printTable(table.headers(), table.data());
        System.out.println("OK. " + table.rows().size() + " row(s) in " + millis + " ms.");
    }
}
