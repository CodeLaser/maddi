/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.e2immu.analyzer.ide.daemon;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * The maddi analysis daemon: a long-lived JDK-25 process that the IntelliJ plugin launches and
 * talks to over a loopback TCP socket (NDJSON, see {@link DaemonProtocol}).
 * <p>
 * On startup it binds an ephemeral loopback port and prints two machine-readable lines to stdout
 * for the launcher to parse:
 * <pre>
 *   DAEMON_PORT=&lt;port&gt;
 *   DAEMON_PID=&lt;pid&gt;
 * </pre>
 * Everything else on stdout/stderr is log noise; the protocol lives on the socket.
 * <p>
 * v1 is single-client and processes one request at a time.
 */
public final class DaemonMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(DaemonMain.class);

    private static final String DAEMON_VERSION = "0.1.0-dev";

    // lenient: request frames carry a "type" discriminator that the payload records don't declare
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final AnalyzeHandler handler;

    public DaemonMain(AnalyzeHandler handler) {
        this.handler = handler;
    }

    public static void main(String[] args) throws IOException {
        int port = 0; // 0 = ephemeral, OS picks a free port
        for (int i = 0; i < args.length; i++) {
            if ("--port".equals(args[i]) && i + 1 < args.length) {
                port = Integer.parseInt(args[++i]);
            }
        }
        new DaemonMain(new WarmAnalysisService()).run(port);
    }

    public void run(int port) throws IOException {
        acceptLoop(bind(port));
    }

    /** Bind an (ephemeral, when {@code port==0}) loopback server socket and announce it on stdout. */
    ServerSocket bind(int port) throws IOException {
        InetAddress loopback = InetAddress.getLoopbackAddress();
        ServerSocket serverSocket = new ServerSocket(port, 4, loopback);
        int actualPort = serverSocket.getLocalPort();
        // machine-readable handshake for the launcher, on stdout, flushed immediately
        System.out.println("DAEMON_PORT=" + actualPort);
        System.out.println("DAEMON_PID=" + ProcessHandle.current().pid());
        System.out.flush();
        LOGGER.info("maddi daemon {} listening on {}:{}", DAEMON_VERSION, loopback.getHostAddress(), actualPort);
        return serverSocket;
    }

    /** Accept clients until a {@code shutdown} message arrives; closes the server socket on exit. */
    void acceptLoop(ServerSocket serverSocket) {
        try (serverSocket) {
            boolean running = true;
            while (running) {
                try (Socket client = serverSocket.accept()) {
                    LOGGER.info("client connected from {}", client.getRemoteSocketAddress());
                    running = serveClient(client);
                } catch (IOException e) {
                    LOGGER.warn("client connection error: {}", e.toString());
                }
            }
        } catch (IOException e) {
            LOGGER.warn("server socket error: {}", e.toString());
        }
        LOGGER.info("maddi daemon shutting down");
    }

    /** @return false to stop the daemon (shutdown received), true to keep accepting clients. */
    private boolean serveClient(Socket client) throws IOException {
        BufferedReader in = new BufferedReader(
                new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
        Writer out = new BufferedWriter(
                new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8));
        String line;
        while ((line = in.readLine()) != null) {
            if (line.isBlank()) continue;
            JsonNode msg;
            try {
                msg = mapper.readTree(line);
            } catch (IOException parseError) {
                send(out, DaemonProtocol.T_ERROR,
                        new DaemonProtocol.ErrorMsg(null, "bad-request", "invalid JSON: " + parseError.getMessage()));
                continue;
            }
            String type = msg.path("type").asText("");
            switch (type) {
                case DaemonProtocol.T_HANDSHAKE -> send(out, DaemonProtocol.T_HANDSHAKE_ACK,
                        new DaemonProtocol.HandshakeAck(DaemonProtocol.PROTOCOL_VERSION, DAEMON_VERSION, maddiVersion()));
                case DaemonProtocol.T_PING -> send(out, DaemonProtocol.T_PONG,
                        new DaemonProtocol.Pong(System.nanoTime()));
                case DaemonProtocol.T_ANALYZE_PROJECT -> handleAnalyze(out, msg);
                case DaemonProtocol.T_SHUTDOWN -> {
                    send(out, DaemonProtocol.T_BYE, null);
                    return false;
                }
                default -> send(out, DaemonProtocol.T_ERROR,
                        new DaemonProtocol.ErrorMsg(msg.path("requestId").asText(null), "bad-request",
                                "unknown message type: '" + type + "'"));
            }
        }
        return true; // client disconnected; keep the daemon warm for a reconnect
    }

    private void handleAnalyze(Writer out, JsonNode msg) throws IOException {
        DaemonProtocol.AnalyzeProject request;
        try {
            request = mapper.treeToValue(msg, DaemonProtocol.AnalyzeProject.class);
        } catch (IOException e) {
            send(out, DaemonProtocol.T_ERROR,
                    new DaemonProtocol.ErrorMsg(msg.path("requestId").asText(null), "bad-request",
                            "malformed analyzeProject: " + e.getMessage()));
            return;
        }
        String requestId = request.requestId();
        try {
            DaemonProtocol.Result result = handler.analyze(request, new AnalyzeHandler.StatusSink() {
                @Override
                public void status(DaemonProtocol.Status status) {
                    try {
                        send(out, DaemonProtocol.T_STATUS, status);
                    } catch (IOException io) {
                        LOGGER.warn("failed to send status: {}", io.toString());
                    }
                }

                @Override
                public void partialResult(DaemonProtocol.PartialResult partial) {
                    try {
                        send(out, DaemonProtocol.T_PARTIAL_RESULT, partial);
                    } catch (IOException io) {
                        // the run itself is unaffected; the client just waits for the terminal result
                        LOGGER.warn("failed to send partial result: {}", io.toString());
                    }
                }
            });
            send(out, DaemonProtocol.T_RESULT, result);
        } catch (Exception e) {
            // daemon stays alive; report as an error message
            LOGGER.warn("analysis failed for request {}", requestId, e);
            send(out, DaemonProtocol.T_ERROR,
                    new DaemonProtocol.ErrorMsg(requestId, "analyze", String.valueOf(e.getMessage())));
        }
    }

    /** Serialize a payload record, stamp the {@code type} field, write it as one NDJSON line. */
    private synchronized void send(Writer out, String type, Object payload) throws IOException {
        ObjectNode node = payload == null
                ? mapper.createObjectNode()
                : mapper.valueToTree(payload);
        node.put("type", type);
        out.write(mapper.writeValueAsString(node));
        out.write('\n');
        out.flush();
    }

    private static String maddiVersion() {
        String fromManifest = DaemonMain.class.getPackage().getImplementationVersion();
        if (fromManifest != null) return fromManifest;
        return System.getProperty("maddi.version", "unknown");
    }
}
