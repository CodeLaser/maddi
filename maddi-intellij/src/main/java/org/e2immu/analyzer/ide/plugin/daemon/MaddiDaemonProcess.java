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

import com.fasterxml.jackson.databind.JsonNode;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Owns one long-lived maddi daemon process for the IDE: launches it lazily, keeps it warm across
 * requests, and re-launches if it dies. Thread-safe; a single request runs at a time.
 */
public final class MaddiDaemonProcess implements Closeable {
    private static final int PROTOCOL_VERSION = 1;

    private final DaemonLauncher launcher = new DaemonLauncher();
    private DaemonLauncher.Handle handle;
    private DaemonClient client;

    /** Ensure a warm daemon is running on {@code jdkHome}; (re)launch + handshake if needed. */
    public synchronized void ensureStarted(Path installDir, Path jdkHome, int xmxMb, Path logFile)
            throws IOException, InterruptedException {
        if (handle != null && handle.process().isAlive() && client != null) return;
        closeQuietly();
        List<String> jvmArgs = new ArrayList<>();
        if (xmxMb > 0) {
            jvmArgs.add("-Xmx" + xmxMb + "m");
            jvmArgs.add("-XX:+UseG1GC");
        }
        handle = launcher.launch(installDir, jdkHome, jvmArgs, 60_000, logFile);
        client = new DaemonClient(handle.port(), 600_000); // heartbeats keep long analyses alive
        JsonNode ack = client.handshake(PROTOCOL_VERSION);
        if (!"handshakeAck".equals(ack.path("type").asText())) {
            throw new IOException("daemon handshake failed: " + ack);
        }
    }

    /** Send an analyze request; blocks until the daemon returns {@code result} or {@code error}. */
    public synchronized JsonNode analyze(String requestId, Object config, Consumer<JsonNode> onStatus)
            throws IOException {
        if (client == null) throw new IOException("daemon not started");
        return client.analyze(requestId, config, onStatus);
    }

    public synchronized DaemonClient client() {
        return client;
    }

    public synchronized boolean isAlive() {
        return handle != null && handle.process().isAlive();
    }

    @Override
    public synchronized void close() {
        closeQuietly();
    }

    private void closeQuietly() {
        if (client != null) {
            try {
                client.shutdown();
            } catch (IOException ignored) {
                // daemon may already be gone
            }
            try {
                client.close();
            } catch (IOException ignored) {
                // ignore
            }
            client = null;
        }
        if (handle != null) {
            handle.close();
            handle = null;
        }
    }
}
