package cr.ac.una.eif400.cyphail.frontend;

import java.util.ArrayList;
import java.util.List;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

// El nucleo funcional del REPL: dado el estado y una linea de entrada, decide que hay que ejecutar y
// cual es el estado siguiente. No lee ni escribe nada y no tiene estado propio; el bucle de Repl solo
// lee lineas, llama a step y ejecuta lo que step le pide.
public final class ReplStep {

    // Lo unico que el REPL recuerda entre lineas: la sentencia que todavia no termina
    public record State(String pending) {
        public static final State EMPTY = new State("");
    }

    // Lo que hay que ejecutar: un comando de una linea (.use, .help...) o una sentencia de Cyphail
    public sealed interface Request permits Command, Statement {
    }

    public record Command(String line) implements Request {
    }

    public record Statement(String text) implements Request {
    }

    public record Result(State next, List<Request> requests, boolean exit) {
    }

    private ReplStep() {
    }

    public static Result step(State state, String rawLine) {
        String line = rawLine.strip();

        // Comandos de una sola linea: se ejecutan de inmediato (.tree es una sentencia, no un comando)
        if (state.pending().isEmpty() && line.startsWith(".") && !MultilineInput.isTreeCommand(line)) {
            boolean exit = line.split("\\s+", 2)[0].equalsIgnoreCase(".exit");
            return new Result(state, List.of(new Command(line)), exit);
        }

        // Linea vacia: ejecuta lo pendiente, si hay algo
        if (line.isEmpty()) {
            if (state.pending().isEmpty()) {
                return new Result(state, List.of(), false);
            }
            return new Result(State.EMPTY, List.of(new Statement(state.pending())), false);
        }

        String text = state.pending().isEmpty() ? line : state.pending() + "\n" + line;
        Statements.Split split = Statements.split(text);

        List<Request> requests = new ArrayList<>();
        split.complete().forEach(statement -> requests.add(new Statement(statement)));

        // Lo que sobra sin ";" tambien se ejecuta si ya esta completo (termina en RETURN)
        if (!split.rest().isEmpty() && MultilineInput.isComplete(split.rest())) {
            requests.add(new Statement(split.rest()));
            return new Result(State.EMPTY, requests, false);
        }
        return new Result(new State(split.rest()), requests, false);
    }
}
