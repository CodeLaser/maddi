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

    private ErrorReport() {
    }

    /**
     * @param summary  the parse summary (may be null); its {@link Summary#parseExceptions()} are enumerated
     * @param terminal the throwable that ended the run (may be null): a {@link Summary.FailFastException}, an
     *                 IO exception, or an analyzer runtime exception
     * @return the number of distinct errors reported
     */
    public static int report(Summary summary, Throwable terminal) {
        int count = 0;
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
            LOGGER.error("Analysis aborted with {}: {}", terminal.getClass().getName(), rootMessage(terminal));
            count++;
        }
        return count;
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
