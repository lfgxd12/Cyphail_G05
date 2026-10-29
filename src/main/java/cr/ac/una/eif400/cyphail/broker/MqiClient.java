package cr.ac.una.eif400.cyphail.broker;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

// Cliente del Machine Query Interface (MQI) de SWI-Prolog sobre un socket TCP.
// Cada mensaje, en los dos sentidos, va enmarcado como "<bytes>.\n<mensaje>.\n", donde <bytes> es la
// longitud en bytes UTF-8 de "<mensaje>.\n". La respuesta a "run" es un JSON que lee MqiResponseParser.
public final class MqiClient implements Closeable {

    // Margen sobre el timeout de la consulta para que el engine alcance a responder time_limit_exceeded
    private static final int SOCKET_MARGIN_MS = 5_000;

    private final Socket socket;
    private final InputStream in;
    private final OutputStream out;

    MqiClient(Socket socket) throws IOException {
        this.socket = socket;
        this.in = socket.getInputStream();
        this.out = socket.getOutputStream();
    }

    // Abre la conexion y se autentica con la clave que el engine imprimio al arrancar
    public static MqiClient connect(int port, String password) throws IOException {
        MqiClient client = new MqiClient(new Socket(InetAddress.getLoopbackAddress(), port));
        try {
            client.socket.setSoTimeout(10_000);
            client.send(password);
            client.receive();
            return client;
        } catch (IOException | RuntimeException e) {
            client.close();
            throw e;
        }
    }

    // Ejecuta el goal (una solucion) y devuelve el resultado ya interpretado
    public PrologResult run(String goal, int timeoutSeconds) throws IOException {
        socket.setSoTimeout(timeoutSeconds * 1000 + SOCKET_MARGIN_MS);
        send(runMessage(goal, timeoutSeconds));
        try {
            return MqiResponseParser.parse(receive());
        } catch (SocketTimeoutException e) {
            throw new IOException("The engine did not answer in time", e);
        }
    }

    // Le pide al engine que cierre esta conexion; si ya estaba cerrada no hace nada
    public void quit() {
        try {
            send("quit");
        } catch (IOException ignored) {
            // el engine ya no esta; no hay nada que cerrar
        }
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }

    // El goal va como termino Prolog (no como string): el MQI lo lee y lo ejecuta tal cual
    static String runMessage(String goal, int timeoutSeconds) {
        return "run((" + goal + "), " + timeoutSeconds + ")";
    }

    static byte[] frame(String message) {
        byte[] body = (message + ".\n").getBytes(StandardCharsets.UTF_8);
        byte[] header = (body.length + ".\n").getBytes(StandardCharsets.UTF_8);
        byte[] all = new byte[header.length + body.length];
        System.arraycopy(header, 0, all, 0, header.length);
        System.arraycopy(body, 0, all, header.length, body.length);
        return all;
    }

    private void send(String message) throws IOException {
        out.write(frame(message));
        out.flush();
    }

    private String receive() throws IOException {
        return readMessage(in);
    }

    // Lee "<bytes>.\n" y despues exactamente esos bytes; tolera \r\n; devuelve el mensaje sin ".\n"
    static String readMessage(InputStream in) throws IOException {
        ByteArrayOutputStream digits = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != '.') {
            if (b < 0) {
                throw new IOException("The engine closed the connection");
            }
            if (b >= '0' && b <= '9') {
                digits.write(b);
            }
        }
        if (digits.size() == 0) {
            throw new IOException("Malformed MQI message: no length");
        }
        int length = Integer.parseInt(digits.toString(StandardCharsets.US_ASCII));
        int next = in.read();
        if (next == '\r') {
            in.read();   // el \n del \r\n
        }
        byte[] body = in.readNBytes(length);
        if (body.length < length) {
            throw new IOException("The engine closed the connection");
        }
        String text = new String(body, StandardCharsets.UTF_8);
        String trimmed = text.stripTrailing();
        return trimmed.endsWith(".") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
