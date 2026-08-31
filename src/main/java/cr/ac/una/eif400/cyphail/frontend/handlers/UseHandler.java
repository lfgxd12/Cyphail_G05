package cr.ac.una.eif400.cyphail.frontend.handlers;

import cr.ac.una.eif400.cyphail.frontend.ReplCommand;
import cr.ac.una.eif400.cyphail.model.GraphInfo;
import cr.ac.una.eif400.cyphail.output.TablePrinter;

import java.util.List;

/**
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 *          Jostin Jimenez Alfaro, Angel Rojas Ruano
 */
public class UseHandler implements ReplCommand {
    private final List<GraphInfo> graphs = List.of(
            new GraphInfo("amigos", "Social Network"),
            new GraphInfo("tasks", "Tasks and resources"),
            new GraphInfo("teams", "Soccer Teams"),
            new GraphInfo("planets", "Planets in Solar System")
    );

    @Override
    public void execute(String args) {
        if (args.isBlank()) {
            String[] headers = {"Graph", "Description"};
            String[][] data = graphs.stream()
                    .map(g -> new String[]{g.name(), g.description()})
                    .toArray(String[][]::new);

            TablePrinter.printTable(headers, data);
            System.out.println("OK. Query available after 5 ms.");
        } else {
            boolean exists = graphs.stream().anyMatch(g -> g.name().equalsIgnoreCase(args));
            if (exists) {
                System.out.printf("OK. \"%s\" graph available after 1 ms.%n", args);
            } else {
                System.out.printf("ERROR: Graph \"%s\" not found.%n", args);
            }
        }
    }
}
