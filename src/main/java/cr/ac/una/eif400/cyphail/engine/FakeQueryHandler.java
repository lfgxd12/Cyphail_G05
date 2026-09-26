package cr.ac.una.eif400.cyphail.engine;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import cr.ac.una.eif400.cyphail.output.TablePrinter;

import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

public class FakeQueryHandler {

    public static void process(String query) {
        Path jsonPath = Path.of("data", "test_cases.json");

        if (!Files.exists(jsonPath)) {
            System.out.println("ERROR: File /data/test_cases.json not found.");
            return;
        }

        try (FileReader reader = new FileReader(jsonPath.toFile())) {
            Gson gson = new Gson();
            JsonArray cases = gson.fromJson(reader, JsonArray.class);

            if (cases == null) {
                System.out.println("ERROR: /data/test_cases.json is empty or invalid.");
                return;
            }

            String normalizedInput = query.replaceAll("\\s+", " ").trim();

            for (JsonElement elem : cases) {
                JsonObject obj = elem.getAsJsonObject();
                String jsonQuery = obj.get("query").getAsString().replaceAll("\\s+", " ").trim();

                if (jsonQuery.equalsIgnoreCase(normalizedInput)) {
                    // Leer encabezados
                    JsonArray headersJson = obj.getAsJsonArray("headers");
                    String[] headers = new String[headersJson.size()];
                    for (int i = 0; i < headersJson.size(); i++) {
                        headers[i] = headersJson.get(i).getAsString();
                    }

                    // Leer filas de datos
                    JsonArray dataJson = obj.getAsJsonArray("data");
                    String[][] data = new String[dataJson.size()][headers.length];
                    for (int i = 0; i < dataJson.size(); i++) {
                        JsonArray row = dataJson.get(i).getAsJsonArray();
                        for (int j = 0; j < row.size(); j++) {
                            data[i][j] = row.get(j).getAsString();
                        }
                    }

                    TablePrinter.printTable(headers, data);
                    System.out.println("OK. Query resolved after 42 ms.");
                    return;
                }
            }

            System.out.println("ERROR: Syntax error or query not supported in Sprint P1.");

        } catch (Exception e) {
            System.out.println("ERROR reading JSON fake data: " + e.getMessage());
        }
    }
}