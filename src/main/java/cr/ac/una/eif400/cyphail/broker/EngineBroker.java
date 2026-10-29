package cr.ac.una.eif400.cyphail.broker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

// Arranca el engine (swipl con cyphail_engine.pl y el servidor MQI) como proceso hijo y le envia
// los goals que genera el Codegen. Una sola instancia por sesion del REPL; close() la termina.
public final class EngineBroker implements AutoCloseable {

    public static final int DEFAULT_TIMEOUT_SECONDS = 10;
    private static final int STARTUP_SECONDS = 20;
    private static final String ENGINE_RESOURCE = "/prolog/cyphail_engine.pl";

    private final Process process;
    private final MqiClient client;
    private final Path engineFile;
    private final Thread shutdownHook;
    private final int timeoutSeconds;
    private boolean closed;

    private EngineBroker(Process process, MqiClient client, Path engineFile, int timeoutSeconds) {
        this.process = process;
        this.client = client;
        this.engineFile = engineFile;
        this.timeoutSeconds = timeoutSeconds;
        this.shutdownHook = new Thread(this::close);
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    public static EngineBroker start() {
        Path swipl = SwiplLocator.locate().orElseThrow(() -> new EngineException(
                "SWI-Prolog was not found. Install it, add it to the PATH or set "
                        + SwiplLocator.ENV_VARIABLE + " to the full path of swipl."));
        return start(swipl, DEFAULT_TIMEOUT_SECONDS);
    }

    public static EngineBroker start(Path swipl, int timeoutSeconds) {
        Path engineFile = extractEngine();
        Process process = null;
        try {
            process = new ProcessBuilder(swipl.toString(), "-l", engineFile.toString(),
                    "mqi", "--write_connection_values=true")
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            String[] values = readConnectionValues(process);
            MqiClient client = MqiClient.connect(Integer.parseInt(values[0]), values[1]);
            return new EngineBroker(process, client, engineFile, timeoutSeconds);
        } catch (IOException | RuntimeException e) {
            if (process != null) {
                process.destroyForcibly();
            }
            deleteQuietly(engineFile);
            if (e instanceof EngineException engineException) {
                throw engineException;
            }
            throw new EngineException("The Prolog engine could not be started: " + e.getMessage(), e);
        }
    }

    // Ejecuta el goal y devuelve el texto Rows del engine
    public synchronized String run(String goal) {
        if (closed) {
            throw new EngineException("The Prolog engine is not running");
        }
        PrologResult result;
        try {
            result = client.run(goal, timeoutSeconds);
        } catch (IOException e) {
            throw new EngineException("The connection with the Prolog engine was lost: " + e.getMessage(), e);
        }
        return switch (result) {
            case PrologResult.Success success -> rowsOf(success);
            case PrologResult.Failure ignored ->
                    throw new EngineException("The engine could not resolve the query");
            case PrologResult.PrologError error -> throw new EngineException(describe(error.term()));
        };
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        } catch (IllegalStateException ignored) {
            // la JVM ya esta terminando; el hook es el que nos llamo
        }
        client.quit();
        try {
            client.close();
        } catch (IOException ignored) {
            // ya estaba cerrada
        }
        try {
            if (!process.waitFor(2, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
        }
        deleteQuietly(engineFile);
    }

    private static String rowsOf(PrologResult.Success success) {
        for (Map<String, Object> row : success.rows()) {
            if (row.get("Rows") instanceof String rows) {
                return rows;
            }
        }
        throw new EngineException("The engine answered without a result");
    }

    // Texto para el usuario a partir del termino de error de Prolog
    static String describe(Object term) {
        if ("time_limit_exceeded".equals(term)) {
            return "The query exceeded the time limit";
        }
        return "Engine error: " + format(term);
    }

    private static String format(Object term) {
        if (term instanceof PrologCompound compound) {
            return compound.functor() + "(" + compound.args().stream()
                    .map(EngineBroker::format).collect(Collectors.joining(", ")) + ")";
        }
        if (term instanceof List<?> list) {
            return list.stream().map(EngineBroker::format).collect(Collectors.joining(", ", "[", "]"));
        }
        return String.valueOf(term);
    }

    // Las dos primeras lineas que imprime el servidor MQI son el puerto y la clave
    private static String[] readConnectionValues(Process process) {
        CompletableFuture<String[]> values = new CompletableFuture<>();
        Thread reader = new Thread(() -> {
            try (BufferedReader out = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String port = out.readLine();
                String password = out.readLine();
                values.complete(new String[]{port, password});
                while (out.readLine() != null) {
                    // se sigue leyendo para que el engine nunca se bloquee escribiendo en stdout
                }
            } catch (IOException e) {
                values.completeExceptionally(e);
            }
        }, "cyphail-engine-stdout");
        reader.setDaemon(true);
        reader.start();
        try {
            String[] result = values.get(STARTUP_SECONDS, TimeUnit.SECONDS);
            if (result[0] == null || result[1] == null) {
                throw new EngineException("SWI-Prolog exited before the engine was ready");
            }
            return result;
        } catch (TimeoutException e) {
            throw new EngineException("The Prolog engine did not start in " + STARTUP_SECONDS + " seconds");
        } catch (InterruptedException | ExecutionException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new EngineException("The Prolog engine could not be started", e);
        }
    }

    // swipl necesita el .pl como archivo: se copia del jar a un temporal
    private static Path extractEngine() {
        try (InputStream in = EngineBroker.class.getResourceAsStream(ENGINE_RESOURCE)) {
            if (in == null) {
                throw new EngineException("cyphail_engine.pl is missing from the application");
            }
            Path file = Files.createTempFile("cyphail_engine_", ".pl");
            Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
            return file;
        } catch (IOException e) {
            throw new EngineException("The Prolog engine could not be prepared: " + e.getMessage(), e);
        }
    }

    private static void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // es un temporal; el sistema lo limpiara
        }
    }
}
