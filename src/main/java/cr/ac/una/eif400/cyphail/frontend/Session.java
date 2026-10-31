package cr.ac.una.eif400.cyphail.frontend;

import cr.ac.una.eif400.cyphail.broker.GoalRunner;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

// El estado de una sesion del REPL: el grafo en uso y el engine, que se arranca hasta que
// hace falta por primera vez (asi .help, .about y los errores de sintaxis no pagan el arranque de swipl).
public final class Session {

    public static final String DEFAULT_GRAPH = "amigos";

    // Pide al engine los nombres de los grafos que conoce (hechos cy_db/1)
    private static final String GRAPHS_GOAL = "findall(G, cy_db(G), Gs), atomic_list_concat(Gs, ' ', Rows)";

    private final Supplier<GoalRunner> factory;
    private GoalRunner runner;
    private String graph = DEFAULT_GRAPH;

    public Session(Supplier<GoalRunner> factory) {
        this.factory = factory;
    }

    public String graph() {
        return graph;
    }

    // Arranca el engine si todavia no esta corriendo
    public GoalRunner runner() {
        if (runner == null) {
            runner = factory.get();
        }
        return runner;
    }

    public List<String> graphs() {
        String names = runner().run(GRAPHS_GOAL);
        return Arrays.stream(names.split(" ")).filter(name -> !name.isBlank()).toList();
    }

    // Cambia el grafo en uso. Devuelve el nombre canonico, o vacio si el grafo no existe.
    public Optional<String> use(String name) {
        Optional<String> found = graphs().stream().filter(g -> g.equalsIgnoreCase(name.strip())).findFirst();
        found.ifPresent(g -> graph = g);
        return found;
    }

    public void close() {
        if (runner != null) {
            runner.close();
            runner = null;
        }
    }
}
