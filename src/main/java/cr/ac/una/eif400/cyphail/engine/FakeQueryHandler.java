package cr.ac.una.eif400.cyphail.engine;

import cr.ac.una.eif400.cyphail.output.TablePrinter;

public class FakeQueryHandler {
    public static void process(String query) {
        String normalizedQuery = query.replaceAll("\\s+", " ").trim();

        for (String key : FakeResponses.MATCH_QUERIES.keySet()) {
            if (key.equalsIgnoreCase(normalizedQuery)) {
                String[] headers = FakeResponses.HEADERS.get(key);
                String[][] data = FakeResponses.MATCH_QUERIES.get(key);

                TablePrinter.printTable(headers, data);
                System.out.println("OK. Query resolved after 42 ms.");
                return;
            }
        }

        System.out.println("ERROR: Syntax error or query not supported in sprint P1.1.");
    }
}