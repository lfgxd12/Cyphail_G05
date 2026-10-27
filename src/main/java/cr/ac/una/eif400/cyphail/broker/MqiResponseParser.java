package cr.ac.una.eif400.cyphail.broker;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

public final class MqiResponseParser {

    private MqiResponseParser() {
    }

    public static PrologResult parse(String json) {
        JsonElement root;
        try {
            root = JsonParser.parseString(json);
        } catch (JsonParseException e) {
            throw new IllegalArgumentException("MQI response is not valid JSON: " + json, e);
        }
        return switch (root) {
            case JsonPrimitive p when isFalse(p) -> new PrologResult.Failure();
            case JsonObject o -> fromObject(o);
            default -> throw new IllegalArgumentException("Unexpected MQI response: " + json);
        };
    }

    private static boolean isFalse(JsonPrimitive p) {
        return (p.isString() && p.getAsString().equals("false"))
                || (p.isBoolean() && !p.getAsBoolean());
    }

    private static PrologResult fromObject(JsonObject o) {
        String functor = functorOf(o);
        JsonArray args = argsOf(o);
        return switch (functor) {
            case "true" -> new PrologResult.Success(solutions(args));
            case "exception" -> new PrologResult.PrologError(args.isEmpty() ? null : toJava(args.get(0)));
            default -> throw new IllegalArgumentException("Unexpected MQI functor: " + functor);
        };
    }

    private static List<Map<String, Object>> solutions(JsonArray args) {
        if (args.isEmpty() || !(args.get(0) instanceof JsonArray solutions)) {
            throw new IllegalArgumentException("MQI success without a list of solutions");
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (JsonElement solution : solutions) {
            if (!(solution instanceof JsonArray bindings)) {
                throw new IllegalArgumentException("MQI solution is not a list of bindings");
            }
            Map<String, Object> row = new LinkedHashMap<>();
            for (JsonElement binding : bindings) {
                putBinding(row, binding);
            }
            rows.add(Collections.unmodifiableMap(row));
        }
        return rows;
    }

    private static void putBinding(Map<String, Object> row, JsonElement binding) {
        if (binding instanceof JsonObject o && "=".equals(functorOf(o)) && argsOf(o).size() == 2) {
            JsonArray pair = argsOf(o);
            row.put(pair.get(0).getAsString(), toJava(pair.get(1)));
        } else {
            throw new IllegalArgumentException("MQI binding is not Variable = Value: " + binding);
        }
    }

    private static Object toJava(JsonElement e) {
        return switch (e) {
            case JsonNull ignored -> null;
            case JsonPrimitive p -> primitive(p);
            case JsonArray a -> list(a);
            case JsonObject o -> new PrologCompound(functorOf(o), list(argsOf(o)));
            default -> throw new IllegalArgumentException("Unexpected JSON element: " + e);
        };
    }

    private static Object primitive(JsonPrimitive p) {
        if (p.isBoolean()) {
            return p.getAsBoolean();
        }
        if (p.isNumber()) {
            String text = p.getAsString();
            // 30 es Long y 19.99 es Double; un entero gigante no cabe en Long
            if (text.contains(".") || text.contains("e") || text.contains("E")) {
                return p.getAsDouble();
            }
            BigInteger big = new BigInteger(text);
            return big.bitLength() < 64 ? (Object) big.longValue() : big;
        }
        return p.getAsString();
    }

    private static List<Object> list(JsonArray a) {
        List<Object> items = new ArrayList<>();
        for (JsonElement item : a) {
            items.add(toJava(item));
        }
        return items;
    }

    private static String functorOf(JsonObject o) {
        if (!o.has("functor") || !o.get("functor").isJsonPrimitive()) {
            throw new IllegalArgumentException("MQI term without functor: " + o);
        }
        return o.get("functor").getAsString();
    }

    private static JsonArray argsOf(JsonObject o) {
        if (!o.has("args") || !o.get("args").isJsonArray()) {
            throw new IllegalArgumentException("MQI term without args: " + o);
        }
        return o.getAsJsonArray("args");
    }
}