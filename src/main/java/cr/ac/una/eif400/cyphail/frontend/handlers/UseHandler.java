package cr.ac.una.eif400.cyphail.frontend.handlers;

import com.google.gson.Gson;
import cr.ac.una.eif400.cyphail.frontend.ReplCommand;
import cr.ac.una.eif400.cyphail.model.GraphInfo;
import cr.ac.una.eif400.cyphail.output.TablePrinter;

import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 *          Jostin Jimenez Alfaro, Angel Rojas Ruano
 */
public class UseHandler implements ReplCommand {

    private static final Path GRAPHS_PATH =
            Path.of("data", "graphs.json");

    @Override
    public void execute(String args) {

        if (!Files.exists(GRAPHS_PATH)) {
            System.out.println("ERROR: File /data/graphs.json not found.");
            return;
        }

        try (FileReader reader = new FileReader(GRAPHS_PATH.toFile())) {

            Gson gson = new Gson();
            GraphInfo[] graphs =
                    gson.fromJson(reader, GraphInfo[].class);

            if (graphs == null) {
                System.out.println(
                        "ERROR: /data/graphs.json is empty or invalid."
                );
                return;
            }

            if (args.isBlank()) {

                String[] headers = {"Graph", "Description"};
                String[][] data = new String[graphs.length][2];

                for (int i = 0; i < graphs.length; i++) {
                    data[i][0] = graphs[i].name();
                    data[i][1] = graphs[i].description();
                }

                TablePrinter.printTable(headers, data);
                System.out.println(
                        "OK. Query available after 5 ms."
                );

            } else {

                boolean exists = false;

                for (GraphInfo graph : graphs) {
                    if (graph.name().equalsIgnoreCase(args.trim())) {
                        exists = true;
                        break;
                    }
                }

                if (exists) {
                    System.out.printf(
                            "OK. \"%s\" graph available after 1 ms.%n",
                            args.trim()
                    );
                } else {
                    System.out.printf(
                            "ERROR: Graph \"%s\" not found.%n",
                            args.trim()
                    );
                }
            }

        } catch (Exception e) {
            System.out.println(
                    "ERROR reading graphs JSON: " + e.getMessage()
            );
        }
    }
}