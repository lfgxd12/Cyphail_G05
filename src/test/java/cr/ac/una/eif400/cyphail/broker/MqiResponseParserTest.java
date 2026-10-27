package cr.ac.una.eif400.cyphail.broker;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

class MqiResponseParserTest {

    private static PrologResult.Success success(String json) {
        return assertInstanceOf(PrologResult.Success.class, MqiResponseParser.parse(json));
    }

    @Test
    void successWithTwoSolutions() {
        var result = success("""
                {"args":[[[{"args":["X","a"],"functor":"="}],[{"args":["X","b"],"functor":"="}]]],"functor":"true"}""");
        assertEquals(List.of(Map.of("X", "a"), Map.of("X", "b")), result.rows());
    }

    @Test
    void failureArrivesAsTheJsonStringFalse() {
        // La trampa: el goal fail llega como el STRING "false", no como el booleano
        assertInstanceOf(PrologResult.Failure.class, MqiResponseParser.parse("\"false\""));
    }

    @Test
    void failureAlsoAcceptsTheJsonBooleanFalse() {
        assertInstanceOf(PrologResult.Failure.class, MqiResponseParser.parse("false"));
    }

    @Test
    void successWithoutBindingsIsOneEmptySolution() {
        var result = success("{\"args\":[[[]]],\"functor\":\"true\"}");
        assertEquals(1, result.rows().size());
        assertTrue(result.rows().get(0).isEmpty());
    }

    @Test
    void exceptionKeepsTheCompoundTerm() {
        var result = MqiResponseParser.parse("""
                {"args":[{"args":["procedure",{"args":["cy_prueba",1],"functor":"/"}],"functor":"existence_error"}],"functor":"exception"}""");
        var error = assertInstanceOf(PrologResult.PrologError.class, result);
        var expected = new PrologCompound("existence_error",
                List.of("procedure", new PrologCompound("/", List.of("cy_prueba", 1L))));
        assertEquals(expected, error.term());
    }

    @Test
    void exceptionWithAnAtomTermIsAString() {
        var result = MqiResponseParser.parse("{\"args\":[\"time_limit_exceeded\"],\"functor\":\"exception\"}");
        assertEquals("time_limit_exceeded", assertInstanceOf(PrologResult.PrologError.class, result).term());
    }

    @Test
    void listOfListsIsConvertedRecursively() {
        var result = success("""
                {"args":[[[{"args":["Rows",[[1,"a"],[1,"b"],[2,"a"],[2,"b"]]],"functor":"="}]]],"functor":"true"}""");
        assertEquals(List.of(List.of(1L, "a"), List.of(1L, "b"), List.of(2L, "a"), List.of(2L, "b")),
                result.rows().get(0).get("Rows"));
    }

    @Test
    void accentedTextArrivesIntact() {
        var result = success("{\"args\":[[[{\"args\":[\"X\",\"María\"],\"functor\":\"=\"}]]],\"functor\":\"true\"}");
        assertEquals("María", result.rows().get(0).get("X"));
    }

    @Test
    void integersAreLongAndDecimalsAreDouble() {
        var integer = success("{\"args\":[[[{\"args\":[\"L\",5],\"functor\":\"=\"}]]],\"functor\":\"true\"}");
        assertEquals(5L, integer.rows().get(0).get("L"));
        var decimal = success("{\"args\":[[[{\"args\":[\"P\",19.99],\"functor\":\"=\"}]]],\"functor\":\"true\"}");
        assertEquals(19.99, decimal.rows().get(0).get("P"));
    }

    @Test
    void bigIntegerDoesNotOverflow() {
        var result = success("{\"args\":[[[{\"args\":[\"N\",123456789012345678901234567890],\"functor\":\"=\"}]]],\"functor\":\"true\"}");
        assertEquals(new BigInteger("123456789012345678901234567890"), result.rows().get(0).get("N"));
    }

    @Test
    void unboundVariablesComeBackAsUnderscoreAndKeepTheirKey() {
        var result = success("""
                {"args":[[[{"args":["__A","_"],"functor":"="},{"args":["Rows","a.name\\n\\"Juan\\""],"functor":"="}]]],"functor":"true"}""");
        var row = result.rows().get(0);
        assertEquals("_", row.get("__A"));
        assertEquals("a.name\n\"Juan\"", row.get("Rows"));
    }

    @Test
    void multipleBindingsPerSolutionKeepTheirOrder() {
        var result = success("""
                {"args":[[[{"args":["X",1],"functor":"="},{"args":["Y",2],"functor":"="}]]],"functor":"true"}""");
        assertEquals(List.of("X", "Y"), List.copyOf(result.rows().get(0).keySet()));
    }

    @Test
    void jsonNullAndBooleanValuesAreConverted() {
        var result = success("{\"args\":[[[{\"args\":[\"A\",null],\"functor\":\"=\"},{\"args\":[\"B\",true],\"functor\":\"=\"}]]],\"functor\":\"true\"}");
        var row = result.rows().get(0);
        assertTrue(row.containsKey("A"));
        assertNull(row.get("A"));
        assertEquals(Boolean.TRUE, row.get("B"));
    }

    @Test
    void resultsAreImmutable() {
        var result = success("{\"args\":[[[{\"args\":[\"X\",1],\"functor\":\"=\"}]]],\"functor\":\"true\"}");
        assertThrows(UnsupportedOperationException.class, () -> result.rows().add(Map.of()));
        assertThrows(UnsupportedOperationException.class, () -> result.rows().get(0).put("Y", 2));
    }

    @Test
    void malformedResponsesAreRejectedWithAClearError() {
        assertThrows(IllegalArgumentException.class, () -> MqiResponseParser.parse("{not json"));
        assertThrows(IllegalArgumentException.class, () -> MqiResponseParser.parse("42"));
        assertThrows(IllegalArgumentException.class, () -> MqiResponseParser.parse("\"true\""));
        assertThrows(IllegalArgumentException.class, () -> MqiResponseParser.parse("{\"functor\":\"maybe\",\"args\":[]}"));
        assertThrows(IllegalArgumentException.class, () -> MqiResponseParser.parse("{\"functor\":\"true\"}"));
        assertThrows(IllegalArgumentException.class, () -> MqiResponseParser.parse("{\"functor\":\"true\",\"args\":[\"x\"]}"));
    }
}