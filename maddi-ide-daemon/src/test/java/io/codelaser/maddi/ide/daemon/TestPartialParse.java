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
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A compilation unit that does not parse must not cost the REST of the project its analysis.
 * <p>
 * The daemon used to return findings-only whenever {@code summary.haveErrors()}, which on a real tree meant one
 * unresolvable module yielded zero annotations everywhere — measured on Apache Pulsar (2026-08-21): 344 parse
 * errors over 98 source roots, and not one element annotation. That also contradicted the deliberate
 * {@code setFailFast(false)}, which exists so a mid-edit tree still yields something.
 * <p>
 * ⚠ The broken file here extends a type that does not exist. That shape is chosen, not arbitrary: an
 * unresolved reference on its own is filed as a parse WARNING by the openjdk inspector (javac errors are
 * warnings by design, since maddi routinely runs on a deliberately partial classpath), so it would leave
 * {@code haveErrors()} false and never reach the code under test. An unresolvable SUPERTYPE is different — the
 * constructor's implicit {@code super()} gets a null symbol, and {@code ScanCompilationUnit} raises a real
 * parse ERROR. That is exactly the shape seen in the field on {@code CommonMojo extends AbstractMojo}.
 */
public class TestPartialParse {

    // extends a type that does not exist: the implicit super() cannot be attributed -> parse ERROR
    private static final String BROKEN = """
            package p;
            public class Broken extends does.not.Exist {
                public Broken() {
                }
            }
            """;

    // ordinary, self-contained, and analysable: no reference to Broken, so its verdicts are unaffected
    private static final String HEALTHY = """
            package p;
            import java.util.List;
            public class Healthy {
                private final List<String> list;
                public Healthy(List<String> list) {
                    this.list = list;
                }
                public int size() {
                    return list.size();
                }
            }
            """;

    @Test
    public void testBrokenUnitDoesNotSilenceTheProject(@TempDir Path projectDir) throws Exception {
        Map<String, String> sources = new LinkedHashMap<>();
        sources.put("p/Broken.java", BROKEN);
        sources.put("p/Healthy.java", HEALTHY);
        DaemonProtocol.Result result = DaemonAnalysisFixture.analyzeAll(projectDir, sources);

        // the premise: without a real parse ERROR this test proves nothing, so assert it rather than assume it
        assertTrue(result.parseErrorCount() > 0,
                "expected the broken unit to produce a parse error; got " + result.parseErrorCount()
                + ". If this fails the fixture no longer reproduces the shape, NOT that the fix regressed.");

        // the point: the healthy unit was still analysed
        assertFalse(result.elementAnnotations().isEmpty(),
                "a parse error silenced the whole project: no element annotations at all");
        assertTrue(result.elementAnnotations().stream()
                        .anyMatch(e -> e.fqn() != null && e.fqn().contains("Healthy")),
                "the healthy type was not analysed; annotated fqns: "
                + result.elementAnnotations().stream().map(DaemonProtocol.ElementAnnotation::fqn).toList());

        // and it is reported as partial: a run that did not see every reference can never be certified
        assertEquals(DaemonProtocol.OUTCOME_UNKNOWN, result.outcome(),
                "a partial parse must not be reported as a certified fixpoint");
    }

    /** Control: the same healthy source alone parses clean, so the assertions above are about the broken unit. */
    @Test
    public void testHealthyAloneParsesClean(@TempDir Path projectDir) throws Exception {
        DaemonProtocol.Result result = DaemonAnalysisFixture.analyzeAll(projectDir,
                Map.of("p/Healthy.java", HEALTHY));
        assertEquals(0, result.parseErrorCount(), "the healthy source is not supposed to produce parse errors");
        assertFalse(result.elementAnnotations().isEmpty());
    }
}
