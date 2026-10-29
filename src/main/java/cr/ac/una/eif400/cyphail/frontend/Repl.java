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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
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
        Statements may span several lines: finish them with ";" (or an empty line).
        Type ".load <file>" to run a .cyphail script.
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
        commands.put(".load", this::load);
    }

    // Bucle de entrada/salida: lee una linea, ReplStep decide que ejecutar y aqui se ejecuta
    public void start() {
        System.out.println(BANNER);
        Scanner scanner = new Scanner(System.in);
        ReplStep.State state = ReplStep.State.EMPTY;

        while (true) {
            System.out.print(state.pending().isEmpty() ? ">>> " : "... ");
            if (!scanner.hasNextLine()) break;

            ReplStep.Result result = ReplStep.step(state, scanner.nextLine());
            result.requests().forEach(this::run);
            state = result.next();
            if (result.exit()) break;
        }
        session.close();
    }

    private void run(ReplStep.Request request) {
        switch (request) {
            case ReplStep.Command command -> runCommand(command.line());
            case ReplStep.Statement statement -> runStatement(statement.text());
        }
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

    // Devuelve true si la sentencia se ejecuto sin error
    boolean runStatement(String text) {
        String statement = MultilineInput.clean(text);

        if (MultilineInput.isTreeCommand(statement)) {
            commands.get(".tree").execute(MultilineInput.withoutTreeCommand(statement));
            return true;
        }

        return switch (CyphailParser.parse(statement)) {
            case Fail(String reason) -> error("Syntax error. " + reason);
            case Ok(QueryNode query, var rest) -> execute(query);
        };
    }

    // Analyzer -> Codegen -> engine -> tabla. Cada etapa que falla se informa con su propio mensaje.
    private boolean execute(QueryNode query) {
        try {
            Analyzer.validate(query);
            String goal = new PrologCodegen(new PredicateNames(session.graph())).generate(query).goal();
            long start = System.nanoTime();
            RowsTable table = RowsTable.parse(session.runner().run(goal));
            long millis = (System.nanoTime() - start) / 1_000_000;
            print(table, millis);
            return true;
        } catch (SemanticException | EngineException e) {
            return error(e.getMessage());
        } catch (UnsupportedOperationException e) {
            return error("Not supported yet: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return error("The engine answered with an unexpected format. " + e.getMessage());
        }
    }

    private static boolean error(String message) {
        System.out.println("ERROR: " + message);
        return false;
    }

    // .load "ruta.cyphail": ejecuta en orden cada sentencia del archivo y se detiene en la primera que falla
    private void load(String args) {
        String raw = args.strip();
        if (raw.isEmpty()) {
            System.out.println("Usage: .load \"path/to/script.cyphail\"");
            return;
        }
        String text;
        try {
            text = Files.readString(Path.of(unquote(raw)), StandardCharsets.UTF_8);
        } catch (NoSuchFileException e) {
            error("File not found: " + unquote(raw));
            return;
        } catch (IOException | InvalidPathException e) {
            error("Could not read " + unquote(raw) + ": " + e.getMessage());
            return;
        }

        List<String> statements = Statements.all(text);
        for (int i = 0; i < statements.size(); i++) {
            System.out.println("[" + (i + 1) + "/" + statements.size() + "] " + summary(statements.get(i)));
            if (!runStatement(statements.get(i))) {
                error(".load stopped at statement " + (i + 1) + " of " + statements.size() + ".");
                return;
            }
        }
        System.out.println("OK. " + statements.size() + " statement(s) executed.");
    }

    private static String unquote(String text) {
        boolean quoted = text.length() >= 2
                && (text.startsWith("\"") && text.endsWith("\"") || text.startsWith("'") && text.endsWith("'"));
        return quoted ? text.substring(1, text.length() - 1) : text;
    }

    // Primera linea de la sentencia que no sea un comentario, recortada
    private static String summary(String statement) {
        String line = statement.lines().map(String::strip)
                .filter(l -> !l.isEmpty() && !l.startsWith("//")).findFirst().orElse("");
        return line.length() > 70 ? line.substring(0, 70) + "..." : line;
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
