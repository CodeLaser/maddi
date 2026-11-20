/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.project.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.List;

/**
 * A build that failed, or compiled nothing: the message names the exit code and the build log, and
 * {@link #buildLogTail()} has its last lines, so the error comes back without the debug noise.
 */
public final class CaptureException extends IOException {
    public static final int TAIL_LINES = 40;

    private final int exitCode;
    private final Path buildLog;
    private final List<String> buildLogTail;

    public CaptureException(String message, int exitCode, Path buildLog) {
        super(message + (buildLog == null ? "" : "; its output is in " + buildLog));
        this.exitCode = exitCode;
        this.buildLog = buildLog;
        this.buildLogTail = tail(buildLog);
    }

    /** The build's exit code; 0 when it succeeded but compiled nothing. */
    public int exitCode() {
        return exitCode;
    }

    public Path buildLog() {
        return buildLog;
    }

    public List<String> buildLogTail() {
        return buildLogTail;
    }

    private static List<String> tail(Path log) {
        if (log == null || !Files.isRegularFile(log)) return List.of();
        ArrayDeque<String> last = new ArrayDeque<>();
        try (var lines = Files.lines(log)) {
            lines.forEach(line -> {
                if (last.size() == TAIL_LINES) last.removeFirst();
                last.addLast(line);
            });
        } catch (IOException | UncheckedIOException e) {
            return List.of();
        }
        return List.copyOf(last);
    }
}
