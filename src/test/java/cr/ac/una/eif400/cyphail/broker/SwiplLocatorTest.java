package cr.ac.una.eif400.cyphail.broker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

class SwiplLocatorTest {

    @TempDir
    Path dir;

    @Test
    void theEnvironmentVariableWins() throws IOException {
        Path custom = Files.createFile(dir.resolve("mi-swipl.exe"));
        Optional<Path> found = SwiplLocator.locate(Map.of(SwiplLocator.ENV_VARIABLE, custom.toString()), dir.resolve("nada"));
        assertEquals(Optional.of(custom), found);
    }

    @Test
    void aWrongEnvironmentVariableDoesNotFallBackSilently() throws IOException {
        Path fallback = Files.createFile(dir.resolve("fallback.exe"));
        Optional<Path> found = SwiplLocator.locate(Map.of(SwiplLocator.ENV_VARIABLE, dir.resolve("no-existe").toString()), fallback);
        assertTrue(found.isEmpty());
    }

    @Test
    void searchesThePath() throws IOException {
        Path bin = Files.createDirectory(dir.resolve("bin"));
        Path swipl = Files.createFile(bin.resolve("swipl.exe"));
        Map<String, String> env = Map.of("PATH", dir.resolve("otro") + File.pathSeparator + bin);
        assertEquals(Optional.of(swipl), SwiplLocator.locate(env, dir.resolve("nada")));
    }

    @Test
    void usesTheDefaultLocationLast() throws IOException {
        Path fallback = Files.createFile(dir.resolve("swipl-default.exe"));
        assertEquals(Optional.of(fallback), SwiplLocator.locate(Map.of(), fallback));
    }

    @Test
    void returnsEmptyWhenThereIsNothing() {
        assertTrue(SwiplLocator.locate(Map.of(), dir.resolve("nada")).isEmpty());
    }
}
