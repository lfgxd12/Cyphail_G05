package cr.ac.una.eif400.cyphail.broker;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

// Encuentra el ejecutable de SWI-Prolog: primero la variable CYPHAIL_SWIPL, luego el PATH y,
// por ultimo, la ruta de instalacion por defecto en Windows.
public final class SwiplLocator {

    public static final String ENV_VARIABLE = "CYPHAIL_SWIPL";
    private static final Path WINDOWS_DEFAULT = Path.of("C:\\Program Files\\swipl\\bin\\swipl.exe");

    private SwiplLocator() {
    }

    public static Optional<Path> locate() {
        return locate(System.getenv(), WINDOWS_DEFAULT);
    }

    static Optional<Path> locate(Map<String, String> env, Path fallback) {
        String configured = env.get(ENV_VARIABLE);
        if (configured != null && !configured.isBlank()) {
            Path path = Path.of(configured.trim());
            return Files.isRegularFile(path) ? Optional.of(path) : Optional.empty();
        }
        for (Path candidate : candidatesInPath(env.get("PATH"))) {
            if (Files.isRegularFile(candidate)) {
                return Optional.of(candidate);
            }
        }
        return Files.isRegularFile(fallback) ? Optional.of(fallback) : Optional.empty();
    }

    private static List<Path> candidatesInPath(String pathVariable) {
        List<Path> candidates = new ArrayList<>();
        if (pathVariable == null) {
            return candidates;
        }
        for (String dir : pathVariable.split(File.pathSeparator)) {
            if (!dir.isBlank()) {
                candidates.add(Path.of(dir.trim(), "swipl.exe"));
                candidates.add(Path.of(dir.trim(), "swipl"));
            }
        }
        return candidates;
    }
}
