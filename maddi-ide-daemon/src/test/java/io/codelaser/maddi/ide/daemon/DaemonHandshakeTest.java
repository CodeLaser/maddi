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

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** M0 smoke test: the transport answers handshake/ping and stops on shutdown, over a real socket. */
public class DaemonHandshakeTest {

    @Test
    public void handshakePingShutdown() throws Exception {
        AnalyzeHandler stub = (req, status) -> {
            throw new UnsupportedOperationException("not needed for this test");
        };
        DaemonMain daemon = new DaemonMain(stub);
        ServerSocket serverSocket = daemon.bind(0);
        int port = serverSocket.getLocalPort();

        Thread daemonThread = new Thread(() -> daemon.acceptLoop(serverSocket), "daemon-accept");
        daemonThread.setDaemon(true);
        daemonThread.start();

        try (Socket client = new Socket(InetAddress.getLoopbackAddress(), port)) {
            client.setSoTimeout(5_000);
            Writer out = new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8);
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));

            String ack = exchange(out, in, "{\"type\":\"handshake\",\"protocolVersion\":1}");
            assertTrue(ack.contains("\"type\":\"handshakeAck\""), ack);
            assertTrue(ack.contains("\"protocolVersion\":1"), ack);
            assertTrue(ack.contains("\"daemonVersion\""), ack);

            String pong = exchange(out, in, "{\"type\":\"ping\"}");
            assertTrue(pong.contains("\"type\":\"pong\""), pong);

            String unknown = exchange(out, in, "{\"type\":\"nope\",\"requestId\":\"r1\"}");
            assertTrue(unknown.contains("\"type\":\"error\""), unknown);
            assertTrue(unknown.contains("bad-request"), unknown);

            String bye = exchange(out, in, "{\"type\":\"shutdown\"}");
            assertTrue(bye.contains("\"type\":\"bye\""), bye);
        }

        daemonThread.join(5_000);
        assertFalse(daemonThread.isAlive(), "daemon should stop after shutdown");
    }

    private static String exchange(Writer out, BufferedReader in, String request) throws IOException {
        out.write(request);
        out.write('\n');
        out.flush();
        String response = in.readLine();
        if (response == null) throw new IOException("connection closed, no response to: " + request);
        return response;
    }
}
