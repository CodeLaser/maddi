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

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Launches the bundled maddi daemon distribution (the {@code installDist} layout: a {@code bin/}
 * launcher + {@code lib/*.jar}) on a chosen JDK (25+), and reads back the ephemeral port it prints.
 * Plain JDK only — no IntelliJ types — so it is unit-testable against a real install.
 */
public final class DaemonLauncher {
    private static final Pattern PORT_LINE = Pattern.compile("DAEMON_PORT=(\\d+)");
    private static final Pattern PID_LINE = Pattern.compile("DAEMON_PID=(\\d+)");

    /** A running daemon: its process, the port it listens on, and (best-effort) its pid. */
    public record Handle(Process process, int port, long pid) implements Closeable {
        @Override
        public void close() {
            process.destroy();
            try {
                if (!process.waitFor(5, TimeUnit.SECONDS)) process.destroyForcibly();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
        }
    }

    /**
     * @param installDir  the daemon {@code installDist} directory (contains {@code bin/} and {@code lib/})
     * @param jdkHome     JDK 25+ home to run the daemon on; {@code null} uses the launcher script default (PATH/JAVA_HOME)
     * @param jvmArgs     extra JVM args (e.g. {@code -Xmx8g}); may be empty
     * @param startTimeoutMillis how long to wait for the {@code DAEMON_PORT=} line
     */
    public Handle launch(Path installDir, Path jdkHome, List<String> jvmArgs, long startTimeoutMillis)
            throws IOException, InterruptedException {
        Path launcher = launcherScript(installDir);
        List<String> command = new ArrayList<>();
        command.add(launcher.toAbsolutePath().toString());
        command.add("--port");
        command.add("0");

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(installDir.toFile());
        pb.redirectErrorStream(true);
        if (jdkHome != null) {
            pb.environment().put("JAVA_HOME", jdkHome.toAbsolutePath().toString());
        }
        if (jvmArgs != null && !jvmArgs.isEmpty()) {
            // the Gradle 'application' start script honours JAVA_OPTS / DEFAULT_JVM_OPTS
            pb.environment().put("JAVA_OPTS", String.join(" ", jvmArgs));
        }

        Process process = pb.start();
        BufferedReader stdout = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));

        int port = -1;
        long pid = process.pid();
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(startTimeoutMillis);
        String line;
        while (System.nanoTime() < deadline && (line = stdout.readLine()) != null) {
            Matcher pm = PORT_LINE.matcher(line);
            if (pm.find()) {
                port = Integer.parseInt(pm.group(1));
                break;
            }
            Matcher idm = PID_LINE.matcher(line);
            if (idm.find()) pid = Long.parseLong(idm.group(1));
        }
        if (port < 0) {
            process.destroyForcibly();
            throw new IOException("daemon did not announce DAEMON_PORT within " + startTimeoutMillis + "ms");
        }
        // keep draining stdout so the process never blocks on a full pipe (daemon logs there)
        drainInBackground(stdout);
        return new Handle(process, port, pid);
    }

    private static Path launcherScript(Path installDir) throws IOException {
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        String name = windows ? "maddi-ide-daemon.bat" : "maddi-ide-daemon";
        Path script = installDir.resolve("bin").resolve(name);
        if (!Files.isRegularFile(script)) {
            throw new IOException("daemon launcher not found: " + script + " (did :maddi-ide-daemon:installDist run?)");
        }
        return script;
    }

    private static void drainInBackground(BufferedReader stdout) {
        Thread t = new Thread(() -> {
            try {
                while (stdout.readLine() != null) { /* discard daemon log noise */ }
            } catch (IOException ignored) {
                // process ended
            }
        }, "maddi-daemon-stdout-drain");
        t.setDaemon(true);
        t.start();
    }
}
