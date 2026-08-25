package cr.ac.una.eif400.cyphail.engine;

import java.util.LinkedHashMap;
import java.util.Map;

public class FakeResponses {
    public static final Map<String, String[][]> MATCH_QUERIES = new LinkedHashMap<>();
    public static final Map<String, String[]> HEADERS = new LinkedHashMap<>();

    static {
        // Query 1
        String q1 = "MATCH (p:Persona) RETURN p.nombre, p.edad";
        HEADERS.put(q1, new String[]{"p.nombre", "p.edad"});
        MATCH_QUERIES.put(q1, new String[][]{
                {"\"Ana\"", "28"},
                {"\"Luis\"", "31"},
                {"\"Carlos\"", "25"},
                {"\"Beatriz\"", "34"},
                {"\"David\"", "29"},
                {"\"Elena\"", "22"}
        });

        // Query 2
        String q2 = "MATCH (p1:Persona)-[r:AMIGO_DE]->(p2:Persona) RETURN p1.nombre AS Persona, type(r) AS Relacion, p2.nombre AS AmigoDe";
        HEADERS.put(q2, new String[]{"Persona", "Relacion", "AmigoDe"});
        MATCH_QUERIES.put(q2, new String[][]{
                {"\"Ana\"", "\"AMIGO_DE\"", "\"Luis\""},
                {"\"Ana\"", "\"AMIGO_DE\"", "\"Beatriz\""},
                {"\"Luis\"", "\"AMIGO_DE\"", "\"Carlos\""},
                {"\"Luis\"", "\"AMIGO_DE\"", "\"David\""},
                {"\"Carlos\"", "\"AMIGO_DE\"", "\"Elena\""},
                {"\"Beatriz\"", "\"AMIGO_DE\"", "\"Elena\""}
        });
    }
}