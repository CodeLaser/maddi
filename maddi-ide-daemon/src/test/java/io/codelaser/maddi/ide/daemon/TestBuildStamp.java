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

package io.codelaser.maddi.ide.daemon;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The daemon must be able to say WHICH BUILD it is. The IDE runs the daemon its plugin bundles, and on
 * 2026-08-24 a bundle built from another worktree — one commit before a fix, with a second, uncommitted change
 * on top — announced exactly what a current build announces, so a defect that had been fixed read as a
 * regression; only {@code javap} on the installed jar settled it.
 * <p>
 * Each assertion here stands for one way that check can silently stop working: the generated resource is not
 * packaged, it is packaged somewhere the daemon does not look, it is read but not reported, or it is reported
 * on startup but not on the wire (the handshake is the IDE's only channel).
 */
public class TestBuildStamp {

    /**
     * Under Gradle {@code processResources} has run, so the resource IS on the test classpath — deliberately
     * asserted rather than assumed away, since "absent" is the failure this test exists to catch.
     */
    @Test
    public void generatedResourceIsOnTheClassPath() throws IOException {
        try (InputStream in = DaemonMain.class.getResourceAsStream("build-stamp.properties")) {
            assertNotNull(in, "build-stamp.properties is not next to DaemonMain on the class path;"
                             + " generateBuildStamp did not run, or wrote it elsewhere");
            Properties properties = new Properties();
            properties.load(in);
            String source = properties.getProperty("source");
            assertNotNull(source, "no 'source' key: " + properties);
            assertTrue(source.matches("[0-9a-f]{9}(\\+[0-9a-f]{8})?|nogit"),
                    "not a source-state stamp: '" + source + "'");
        }
    }

    /** The read must find it: a stamp that is generated but reported as "unknown" is no stamp at all. */
    @Test
    public void daemonReportsTheGeneratedStamp() throws IOException {
        Properties properties = new Properties();
        try (InputStream in = DaemonMain.class.getResourceAsStream("build-stamp.properties")) {
            assertNotNull(in);
            properties.load(in);
        }
        assertEquals(properties.getProperty("source"), DaemonMain.BUILD_STAMP);
        assertNotEquals("unknown", DaemonMain.BUILD_STAMP);
    }

    /** And it must reach the IDE, which never sees the daemon's own log until something has already gone wrong. */
    @Test
    public void handshakeAckCarriesTheStamp() throws Exception {
        AnalyzeHandler stub = (req, status) -> {
            throw new UnsupportedOperationException("not needed for this test");
        };
        DaemonMain daemon = new DaemonMain(stub);
        ServerSocket serverSocket = daemon.bind(0);
        Thread daemonThread = new Thread(() -> daemon.acceptLoop(serverSocket), "daemon-accept");
        daemonThread.setDaemon(true);
        daemonThread.start();

        try (Socket client = new Socket(InetAddress.getLoopbackAddress(), serverSocket.getLocalPort())) {
            client.setSoTimeout(5_000);
            Writer out = new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));

            out.write("{\"type\":\"handshake\",\"protocolVersion\":1}\n");
            out.flush();
            String ack = in.readLine();
            assertNotNull(ack);
            assertTrue(ack.contains("\"buildStamp\":\"" + DaemonMain.BUILD_STAMP + "\""), ack);

            out.write("{\"type\":\"shutdown\"}\n");
            out.flush();
            in.readLine();
        }
        daemonThread.join(5_000);
    }
}
