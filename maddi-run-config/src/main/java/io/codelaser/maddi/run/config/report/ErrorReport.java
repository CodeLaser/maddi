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

package org.e2immu.analyzer.run.config.report;

import org.e2immu.language.cst.api.analysis.Message;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.inspection.api.parser.Summary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Harmonized error rendering for every runner. All three front-ends (openjdk, in-house/main, kotlin) accumulate
 * parse/inspection errors into a {@link Summary}; a run can additionally end on a terminal throwable (a fail-fast
 * abort, an IO failure, or an analyzer exception). This turns both into a readable, enumerated report so the
 * collected errors — previously write-only — actually reach the user.
 */
public final class ErrorReport {
    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorReport.class);
    private static final int MAX_WARNINGS_SHOWN = 50;

    private ErrorReport() {
    }

    /**
     * @param summary  the parse summary (may be null); its {@link Summary#parseExceptions()} are enumerated
     * @param terminal the throwable that ended the run (may be null): a {@link Summary.FailFastException}, an
     *                 IO exception, or an analyzer runtime exception
     * @return the number of distinct errors reported
     */
    public static int report(Summary summary, Throwable terminal) {
        return report(summary, terminal, List.of());
    }

    /**
     * @param summary          the parse summary (may be null); its {@link Summary#parseExceptions()} are enumerated
     * @param terminal         the throwable that ended the run (may be null): a {@link Summary.FailFastException},
     *                         an IO exception, or an analyzer runtime exception
     * @param analysisMessages findings about the analyzed code, collected by the (shallow or iterating) analyzers;
     *                         enumerated with their category, location, and cause chain
     * @return the number of distinct errors reported
     */
    public static int report(Summary summary, Throwable terminal, List<Message> analysisMessages) {
        int count = 0;
        List<Message> analysisErrors = analysisMessages.stream().filter(m -> m.level().isError()).toList();
        if (!analysisErrors.isEmpty()) {
            LOGGER.error("Analysis produced {} error(s):", analysisErrors.size());
            int i = 1;
            for (Message message : analysisErrors) {
                LOGGER.error("  [{}] {}", i++, render(message));
                reportCauses(message, "      ", true);
            }
            count += analysisErrors.size();
        }
        List<Message> analysisWarnings = analysisMessages.stream().filter(m -> m.level().isWarning()).toList();
        if (!analysisWarnings.isEmpty()) {
            LOGGER.warn("Analysis produced {} warning(s):", analysisWarnings.size());
            int shownAnalysis = Math.min(analysisWarnings.size(), MAX_WARNINGS_SHOWN);
            for (int i = 0; i < shownAnalysis; i++) {
                Message message = analysisWarnings.get(i);
                LOGGER.warn("  [{}] {}", i + 1, render(message));
                reportCauses(message, "      ", false);
            }
            if (analysisWarnings.size() > shownAnalysis) {
                LOGGER.warn("  ... and {} more", analysisWarnings.size() - shownAnalysis);
            }
        }
        if (summary != null && summary.haveErrors()) {
            List<Summary.ParseException> errors = summary.parseExceptions();
            LOGGER.error("Parsing/inspection produced {} error(s):", errors.size());
            int i = 1;
            for (Summary.ParseException parseException : errors) {
                LOGGER.error("  [{}] {}", i++, parseException.getMessage());
            }
            count += errors.size();
        }
        Summary.ParseException terminalParse = asParseException(terminal);
        if (terminalParse != null) {
            LOGGER.error("Aborted on error: {}", terminalParse.getMessage());
            count++;
        } else if (terminal != null) {
            LOGGER.error("Analysis aborted with {}: {}", terminal.getClass().getName(), rootMessage(terminal),
                    terminal);
            count++;
        }
        if (summary != null && !summary.parseWarnings().isEmpty()) {
            List<Summary.ParseException> warnings = summary.parseWarnings();
            LOGGER.warn("Parsing produced {} warning(s) (unresolved references on the partial classpath):",
                    warnings.size());
            int shown = Math.min(warnings.size(), MAX_WARNINGS_SHOWN);
            for (int i = 0; i < shown; i++) {
                LOGGER.warn("  [{}] {}", i + 1, warnings.get(i).getMessage());
            }
            if (warnings.size() > shown) {
                LOGGER.warn("  ... and {} more", warnings.size() - shown);
            }
        }
        return count;
    }

    /** One line per finding: location [category] message. Same "uri:line-col" shape as Summary.ParseException. */
    private static String render(Message message) {
        return location(message) + " [" + message.category() + "] " + message.message();
    }

    private static String location(Message message) {
        Info info = message.info();
        String at;
        if (info == null) {
            at = "?";
        } else {
            at = info.compilationUnit() == null || info.compilationUnit().uri() == null
                    ? info.fullyQualifiedName() : info.compilationUnit().uri().toString();
        }
        Source source = message.source();
        if (source != null && !source.isNoSource()) at += ":" + source.compact();
        return at;
    }

    /** The evidence trail of a finding, one indentation level per cause depth. */
    private static void reportCauses(Message message, String indent, boolean asError) {
        for (Message cause : message.causes()) {
            if (asError) {
                LOGGER.error("{}because: {}", indent, render(cause));
            } else {
                LOGGER.warn("{}because: {}", indent, render(cause));
            }
            reportCauses(cause, indent + "  ", asError);
        }
    }

    /** Unwrap a {@link Summary.FailFastException} (or a directly-thrown {@link Summary.ParseException}). */
    private static Summary.ParseException asParseException(Throwable terminal) {
        if (terminal instanceof Summary.ParseException parseException) return parseException;
        if (terminal instanceof Summary.FailFastException failFast
            && failFast.getCause() instanceof Summary.ParseException parseException) return parseException;
        return null;
    }

    private static String rootMessage(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null && cause.getCause() != cause) cause = cause.getCause();
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }
}
