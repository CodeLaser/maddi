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

package io.codelaser.maddi.java.openjdk.other;

import org.junit.jupiter.api.Test;
import org.parsers.java.JavaParser;
import org.parsers.java.ParseException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The congocc grammar does not accept a RECEIVER PARAMETER — the {@code Type this} first parameter
 * Java allows on an instance method, which exists so annotations can be put on the receiver
 * (JLS 8.4.1). javac accepts it; the detailed-source pre-scan does not.
 *
 * <p>Surfaced by hibernate-orm while recording that corpus's baseline (2026-09-30). Three files in
 * {@code hibernate-core} use it, and the position reported in each is exactly the {@code this}:
 *
 * <pre>
 * stat/internal/StatisticsImpl.java:216              private void resetStart(StatisticsImpl this)
 * query/sqm/tree/spi/jpa/AbstractJpaTupleElement.java:56   AbstractJpaTupleElement&lt;T&gt; this,
 * query/sqm/tree/spi/select/SqmSubQuery.java:821     private void applyInferableType(SqmSubQuery&lt;T&gt; this, ...)
 * </pre>
 *
 * each as:
 *
 * <pre>
 * WARN ScanCompilationUnits -- Detailed-source pre-scan failed for …StatisticsImpl.java;
 *      continuing without detailed sources: org.parsers.java.ParseException: Encountered an error at input:216:41
 * </pre>
 *
 * ⚠ CONSEQUENCE, AND IT IS MILDER THAN IT LOOKS. This is a WARNING, not an error: javac reported no
 * problem for these files, so the front-end keeps the compilation unit and continues without its
 * detailed sources (see TestDetailedSourcePreScanFailureDegrades for that path). Nothing fails and no
 * type is lost — what is lost is source positions for those three files. So it does not block
 * hibernate-orm; it degrades it.
 *
 * <p>The grammar is asked directly here rather than through a scan, because the claim is about the
 * grammar and nothing else: the control case below is the same method WITHOUT the receiver parameter.
 *
 * ⚠ This test documents a DEFECT. The first case passes today and must keep passing; the second
 * records that the grammar rejects valid Java, and will fail once the grammar accepts it — at which
 * point replace its {@code assertThrows} with the {@code assertDoesNotThrow} above it.
 */
public class TestReceiverParameterParses {

    private static final String WITHOUT_RECEIVER = """
            package a.b;
            public class X {
                private void resetStart() {
                }
            }
            """;

    private static final String WITH_RECEIVER = """
            package a.b;
            public class X {
                private void resetStart(X this) {
                }
            }
            """;

    private static void parse(String source) {
        JavaParser parser = new JavaParser(source);
        parser.setParserTolerant(false);
        parser.CompilationUnit();
    }

    @Test
    public void theControlCaseParses() {
        assertDoesNotThrow(() -> parse(WITHOUT_RECEIVER));
    }

    @Test
    public void aReceiverParameterIsRejected() {
        ParseException e = assertThrows(ParseException.class, () -> parse(WITH_RECEIVER),
                "the grammar accepts a receiver parameter now; replace this with assertDoesNotThrow");
        // javac accepts this source, so the only thing wrong is the grammar.
        System.out.println("grammar rejects `X this`: " + e.getMessage());
    }
}
