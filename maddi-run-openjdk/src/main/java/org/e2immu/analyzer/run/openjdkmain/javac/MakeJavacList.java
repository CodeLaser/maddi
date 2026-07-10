package org.e2immu.analyzer.run.openjdkmain.javac;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class MakeJavacList {
    private static final Logger LOGGER = LoggerFactory.getLogger(MakeJavacList.class);

    public List<String> grep(String cleanCommand, String compileWithDebug, String grepPattern) throws IOException {
        executeShell(null, cleanCommand);
        return executeShell(Pattern.compile(grepPattern), compileWithDebug);
    }

    static List<String> executeShell(Pattern pattern, String command) throws IOException {
        LOGGER.info("Executing command: {}", command);
        // `command` is a full command line (e.g. "mvn -X compile", or "cd x && ./gradlew ..."), not a single
        // program: run it through a shell. The previous ProcessBuilder.command(command) passed the whole string
        // as argv[0], so it was executed as one executable literally named with the embedded spaces. (POSIX shell;
        // PATH is inherited from this JVM's environment, and this is Unix-only -- Windows would need "cmd", "/c".)
        ProcessBuilder processBuilder = new ProcessBuilder("/bin/sh", "-c", command)
                // stream the subprocess's stderr straight through ours, so a full stderr pipe buffer cannot
                // deadlock the stdout reader below (previously stderr was drained only after stdout reached EOF).
                .redirectError(ProcessBuilder.Redirect.INHERIT);
        try (Process process = processBuilder.start()) {
            List<String> matchingStdOutLines = new LinkedList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (pattern != null && pattern.matcher(line).matches()) {
                        matchingStdOutLines.add(line);
                    }
                }
            }
            int exitValue = process.waitFor();
            if (exitValue != 0) {
                LOGGER.warn("Command exited with value {}: {}", exitValue, command);
            }
            if (pattern != null) {
                LOGGER.info("Grepped {} javac lines", matchingStdOutLines.size());
            }
            return matchingStdOutLines;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for command: " + command, e);
        }
    }
}
