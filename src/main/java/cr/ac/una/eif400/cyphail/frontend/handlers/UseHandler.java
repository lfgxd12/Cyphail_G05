package cr.ac.una.eif400.cyphail.frontend.handlers;

import cr.ac.una.eif400.cyphail.broker.EngineException;
import cr.ac.una.eif400.cyphail.frontend.ReplCommand;
import cr.ac.una.eif400.cyphail.frontend.Session;
import cr.ac.una.eif400.cyphail.output.TablePrinter;

import java.util.List;
import java.util.Optional;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

// .use            -> lista los grafos del engine y marca el que esta en uso
// .use <grafo>    -> cambia el grafo en uso para las siguientes consultas
public class UseHandler implements ReplCommand {

    private final Session session;

    public UseHandler(Session session) {
        this.session = session;
    }

    @Override
    public void execute(String args) {
        try {
            if (args.isBlank()) {
                list();
            } else {
                select(args.strip());
            }
        } catch (EngineException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    private void list() {
        List<String> graphs = session.graphs();
        String[][] data = graphs.stream()
                .map(name -> new String[]{name, name.equals(session.graph()) ? "*" : ""})
                .toArray(String[][]::new);
        TablePrinter.printTable(new String[]{"Graph", "In use"}, data);
        System.out.println("OK. " + graphs.size() + " graph(s) available.");
    }

    private void select(String name) {
        Optional<String> graph = session.use(name);
        if (graph.isPresent()) {
            System.out.printf("OK. \"%s\" graph available.%n", graph.get());
        } else {
            System.out.printf("ERROR: Graph \"%s\" not found.%n", name);
        }
    }
}
