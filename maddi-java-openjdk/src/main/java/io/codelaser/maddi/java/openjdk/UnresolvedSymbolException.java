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

package io.codelaser.maddi.java.openjdk;

/**
 * A referenced type, constructor or method could not be resolved during the openjdk scan — typically because
 * maddi runs on a deliberately partial classpath, so a library symbol simply is not present (javac hands us an
 * error/NIL symbol). It extends {@link UnsupportedOperationException} — the historical throw type at these sites,
 * so existing catch clauses are unaffected — but it is <em>typed</em> so the compilation-unit-level fault
 * isolation in {@link ScanCompilationUnits} can classify it as a tolerable warning rather than a hard error.
 */
public class UnresolvedSymbolException extends UnsupportedOperationException {
    public UnresolvedSymbolException(String message) {
        super(message);
    }

    /**
     * <b>The single authority on what fault isolation may downgrade to a warning.</b> Both call sites — the
     * scan-time drop in {@link ScanCompilationUnits} and the commit-time catch in {@code JavaInspectorImpl} —
     * ask this, so the two cannot drift apart; each previously tested for {@code UnresolvedSymbolException}
     * alone, in its own private {@code hasCause}.
     * <p>
     * ⛔ {@code CompletionFailure} IS TOLERABLE, AND THAT IS THE POINT. javac throws it when a class file that a
     * classpath type refers to is absent — routine here, because maddi runs on a deliberately partial classpath
     * and javac resolves LAZILY: it never looks up what the analysed code does not use, so it compiles green
     * while we, committing every type we meet, do not.
     * <p>
     * Measured on trino (2026-08-12): {@code iceberg-core}'s {@code RESTUtil} refers to
     * {@code org.apache.iceberg.relocated…Joiner$MapJoiner}, which lives in {@code iceberg-bundled-guava} — a
     * dependency trino correctly declares at {@code runtime} scope, so it is absent from the compile classpath
     * we capture. Nothing in trino's own source names that type. That ONE absent class, in a jar nothing
     * analysed touches, made {@code SummaryImpl} refuse a {@code ParseResult} for the entire project:
     * <b>11,173 types, 2.08M lines</b>, while javac had compiled all 209 source sets successfully. camel fails
     * the same way from the same cause, and so will any codebase with optional or shaded dependencies — which
     * is most of them.
     * <p>
     * ⚠ TOLERATED IS NOT IGNORED. The unit is still dropped and still reported, as a warning naming the artifact
     * and the missing class; a result that depended on it is incomplete, not wrong. Should a
     * {@code CompletionFailure} ever arise from a genuinely broken artifact rather than a partial classpath — a
     * corrupt or version-mismatched jar, the case {@code SummaryImpl#refusalMessage} exists for — it is that
     * warning's job to name it loudly, not this predicate's job to be fatal.
     */
    public static boolean isTolerable(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof UnresolvedSymbolException) return true;
            if (c instanceof com.sun.tools.javac.code.Symbol.CompletionFailure) return true;
        }
        return false;
    }
}
