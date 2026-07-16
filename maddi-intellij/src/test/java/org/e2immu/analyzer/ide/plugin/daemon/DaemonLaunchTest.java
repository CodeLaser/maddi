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
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M0 end-to-end (plugin side): launch the real daemon distribution and complete a handshake,
 * exactly as {@code MaddiDaemonProcess} will from inside the IDE. The install path is provided by
 * the Gradle {@code test} task (which depends on {@code :maddi-ide-daemon:installDist}).
 */
public class DaemonLaunchTest {

    @Test
    public void launchAndHandshake() throws Exception {
        String install = System.getProperty("maddi.daemon.install");
        assertTrue(install != null && !install.isBlank(),
                "system property maddi.daemon.install must point at the daemon installDist dir");
        Path installDir = Path.of(install);

        DaemonLauncher launcher = new DaemonLauncher();
        // jdkHome=null: use the launcher's default java (the Gradle daemon JVM is 25+ here)
        try (DaemonLauncher.Handle handle = launcher.launch(installDir, null, List.of(), 30_000, null);
             DaemonClient client = new DaemonClient(handle.port(), 5_000)) {

            JsonNode ack = client.handshake(1);
            assertEquals("handshakeAck", ack.path("type").asText());
            assertEquals(1, ack.path("protocolVersion").asInt());

            JsonNode pong = client.ping();
            assertEquals("pong", pong.path("type").asText());

            client.shutdown();
        }
    }
}
