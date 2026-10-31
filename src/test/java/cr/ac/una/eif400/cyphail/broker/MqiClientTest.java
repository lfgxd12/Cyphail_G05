package cr.ac.una.eif400.cyphail.broker;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

class MqiClientTest {

    @Test
    void frameCountsTheBytesOfTheMessageWithItsEnding() {
        // "quit.\n" son 6 bytes
        assertEquals("6.\nquit.\n", new String(MqiClient.frame("quit"), StandardCharsets.UTF_8));
    }

    @Test
    void frameCountsBytesNotCharacters() {
        // la e con tilde ocupa 2 bytes en UTF-8: "é.\n" son 4 bytes
        assertEquals("4.\né.\n", new String(MqiClient.frame("é"), StandardCharsets.UTF_8));
    }

    @Test
    void theGoalIsSentAsATermWithItsTimeout() {
        assertEquals("run((a(1),\nb), 10)", MqiClient.runMessage("a(1),\nb", 10));
    }

    @Test
    void readsOneMessageWithoutItsEnding() throws IOException {
        assertEquals("true", MqiClient.readMessage(stream("6.\ntrue.\n")));
    }

    @Test
    void toleratesWindowsLineEndings() throws IOException {
        assertEquals("true", MqiClient.readMessage(stream("7.\r\ntrue.\r\n")));
    }

    @Test
    void readsTwoMessagesInARow() throws IOException {
        var in = stream("3.\na.\n3.\nb.\n");
        assertEquals("a", MqiClient.readMessage(in));
        assertEquals("b", MqiClient.readMessage(in));
    }

    @Test
    void readsMultibyteTextByBytes() throws IOException {
        assertEquals("é", MqiClient.readMessage(stream("4.\né.\n")));
    }

    @Test
    void aClosedConnectionIsAnError() {
        assertThrows(IOException.class, () -> MqiClient.readMessage(stream("")));
        assertThrows(IOException.class, () -> MqiClient.readMessage(stream("50.\ncorto")));
    }

    private static ByteArrayInputStream stream(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }
}
