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

package org.e2immu.analyzer.ide.plugin.daemon;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Loopback NDJSON client for the maddi daemon. Plain JDK + Jackson only (no IntelliJ or maddi types),
 * so the launch/connect logic is unit-testable and stays comfortably on JBR 21.
 * <p>
 * Not thread-safe: v1 sends one request and reads its response before the next (the daemon is
 * single-request-at-a-time). Streamed {@code status} lines that precede a {@code result} are skipped
 * by {@link #requestUntil}.
 */
public final class DaemonClient implements Closeable {
    // lenient: result/status frames carry a "type" field the DTOs don't declare
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final Socket socket;
    private final BufferedReader in;
    private final BufferedWriter out;

    public DaemonClient(int port, int soTimeoutMillis) throws IOException {
        this.socket = new Socket(InetAddress.getLoopbackAddress(), port);
        this.socket.setSoTimeout(soTimeoutMillis);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
    }

    public ObjectMapper objectMapper() {
        return mapper;
    }

    /**
     * Send an {@code analyzeProject} request and stream {@code status} frames to {@code onStatus} until the
     * terminal {@code result} (or {@code error}) frame, which is returned. {@code config} is any
     * Jackson-serializable object matching the daemon's {@code AnalyzeConfig} shape.
     */
    public JsonNode analyze(String requestId, Object config, java.util.function.Consumer<JsonNode> onStatus)
            throws IOException {
        ObjectNode msg = mapper.createObjectNode();
        msg.put("type", "analyzeProject");
        msg.put("requestId", requestId);
        msg.set("config", mapper.valueToTree(config));
        out.write(mapper.writeValueAsString(msg));
        out.write('\n');
        out.flush();
        while (true) {
            JsonNode node = readLine();
            String type = node.path("type").asText("");
            if ("result".equals(type) || "error".equals(type)) return node;
            if (onStatus != null) onStatus.accept(node);
        }
    }

    /** Send one message and read exactly one response line. */
    public JsonNode request(Map<String, Object> message) throws IOException {
        writeLine(message);
        return readLine();
    }

    /**
     * Send one message, then read lines until one whose {@code type} is {@code finalType} (or
     * {@code error}). Intermediate lines (e.g. {@code status}) are delivered to {@code onIntermediate}.
     */
    public JsonNode requestUntil(Map<String, Object> message, String finalType,
                                 java.util.function.Consumer<JsonNode> onIntermediate) throws IOException {
        writeLine(message);
        while (true) {
            JsonNode node = readLine();
            String type = node.path("type").asText("");
            if (type.equals(finalType) || type.equals("error")) return node;
            if (onIntermediate != null) onIntermediate.accept(node);
        }
    }

    public JsonNode handshake(int protocolVersion) throws IOException {
        return request(Map.of("type", "handshake", "protocolVersion", protocolVersion));
    }

    public JsonNode ping() throws IOException {
        return request(Map.of("type", "ping"));
    }

    public void shutdown() throws IOException {
        writeLine(Map.of("type", "shutdown"));
        // response ("bye") is best-effort; the daemon closes right after
    }

    private void writeLine(Map<String, Object> message) throws IOException {
        ObjectNode node = mapper.valueToTree(message);
        out.write(mapper.writeValueAsString(node));
        out.write('\n');
        out.flush();
    }

    private JsonNode readLine() throws IOException {
        String line = in.readLine();
        if (line == null) throw new IOException("daemon closed the connection");
        return mapper.readTree(line);
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
