package org.e2immu.analyzer.run.openjdkmain.javac;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MakeJavacList {
    private static final Logger LOGGER = LoggerFactory.getLogger(MakeJavacList.class);

    public List<String> grep(String cleanCommand, String compileWithDebug, String grepPattern) throws IOException {
        executeShell(null, cleanCommand);
        return executeShell(Pattern.compile(grepPattern), compileWithDebug);
    }

    static List<String> executeShell(Pattern pattern, String command) throws IOException {
        LOGGER.info("Executing command: {}", command);
        try(Process process = new ProcessBuilder().command(command).start()) {
            List<String> matchingStdOutLines = new LinkedList<>();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher m = pattern == null ? null : pattern.matcher(line);
                if (m != null && m.matches()) {
                    matchingStdOutLines.add(line);
                }
            }
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errorReader.readLine()) != null) {
                System.err.println(line);
            }
            if (pattern != null) {
                LOGGER.info("Grepped {} javac lines", matchingStdOutLines.size());
            }
            return matchingStdOutLines;
        }
    }
}
