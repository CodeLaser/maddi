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

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A local variable declared inside an arrow-form {@code switch} case block, and read by a LATER
 * declaration in that same block, is not found on the element stack.
 *
 * <p>Surfaced by hibernate-orm, {@code hibernate-core/src/main/java/org/hibernate/dialect/H2Dialect.java},
 * while recording that corpus's baseline (2026-09-30). The real code is:
 *
 * <pre>
 * case 23505 -&gt; {
 *     final String constraint = extractUsingTemplate( "violation: \"", "\"", sqle.getMessage() );
 *     final int onIndex = constraint == null ? -1 : constraint.indexOf( " ON " );
 *     yield onIndex &gt; 0 ? constraint.substring( 0, onIndex ) : constraint;
 * }
 * </pre>
 *
 * and the scan reports, three times over and then once as a non-tolerable failure:
 *
 * <pre>
 * ERROR ScanCompilationUnit -- Caught exception in visitVariable
 *       final int onIndex = constraint == null ? -1 : constraint.indexOf(" ON ")
 * java.lang.UnsupportedOperationException: Cannot find element 'constraint' on stack
 * </pre>
 *
 * ⛔ CONSEQUENCE: the whole compilation unit fails, which fails the source set, which fails the run.
 * hibernate-orm cannot be parsed at all, so it can have no recorded baseline and no corpus test —
 * even though its clone, build and configuration all succeed (49 source sets).
 *
 * <p><b>THE LAMBDA IS PART OF THE TRIGGER.</b> The two tests below are the same case block in two
 * places. In a plain method body it PASSES; inside a lambda whose body is the switch expression it
 * FAILS. So the per-case block's scope is pushed on the ordinary path and not on the lambda path.
 * The stack trace says where:
 *
 * <pre>
 * visitLambdaExpression       (ScanCompilationUnit:2780)
 *   visitSwitchExpression     (:3497)
 *     doSwitchEntries         (:3579)
 *       parseBlock            (:1469 -&gt; :1496 -&gt; :1534)
 *         visitVariable       (:2084)   the `final int onIndex = ...` declaration
 *           visitIdentifier   (:2906)   looking up `constraint`
 *             ElementStack.find (:29)   throws
 * </pre>
 *
 * <p><b>HOW THIS IS ARRANGED, AND WHY.</b> Three tests, and the suite stays green:
 * <ul>
 *   <li>{@link #theLambdaCaseFailsToday()} RUNS and asserts the exception. It is the proof that the
 *   defect exists, and it is the thing that will go red the day the defect is fixed — read its
 *   message then, and finish the job below.</li>
 *   <li>{@link #theSameBlockInAPlainMethodBody()} RUNS and passes. It is the contrast that narrows
 *   the defect to the lambda path.</li>
 *   <li>{@link #aLocalDeclaredInAnArrowCaseBlockIsVisibleToTheNextDeclaration()} is DISABLED and
 *   asserts what should happen. Enabling it, and deleting the first test, is what "fixed" means.</li>
 * </ul>
 * A permanently red test stops being information after a day, and the promotion battery would carry
 * it forever; a disabled test on its own proves nothing. This is both.
 */
public class TestSwitchArrowCaseLocalOnStack extends CommonTest {

    // The construct reduced to what is needed: a lambda whose body is a switch EXPRESSION, one arrow
    // case with a block, and two declarations in that block where the second reads the first.
    private static final String INPUT = """
            package a.b;
            public class X {
                interface Extractor {
                    String extract(RuntimeException e);
                }
                static int code(RuntimeException e) {
                    return 23505;
                }
                static String name(RuntimeException e) {
                    return e.getMessage();
                }
                static final Extractor EXTRACTOR = e -> switch (code(e)) {
                    case 23505 -> {
                        final String constraint = name(e);
                        final int onIndex = constraint == null ? -1 : constraint.indexOf(" ON ");
                        yield onIndex > 0 ? constraint.substring(0, onIndex) : constraint;
                    }
                    default -> null;
                };
            }
            """;

    @Test
    public void theLambdaCaseFailsToday() {
        UnsupportedOperationException e = assertThrows(UnsupportedOperationException.class,
                () -> scan("a.b.X", INPUT),
                "the scan no longer loses the local: the defect is fixed, so enable the disabled test "
                + "below and delete this one");
        assertTrue(e.getMessage().contains("Cannot find element 'constraint' on stack"),
                "same construct, different failure -- read it before assuming this is the known gap: "
                + e.getMessage());
    }

    @Disabled("the defect this file documents; enable it, and delete theLambdaCaseFailsToday, when fixed")
    @Test
    public void aLocalDeclaredInAnArrowCaseBlockIsVisibleToTheNextDeclaration() {
        TypeInfo typeInfo = scan("a.b.X", INPUT);
        assertNotNull(typeInfo);
        assertNotNull(typeInfo.fields().stream().filter(f -> "EXTRACTOR".equals(f.name())).findFirst()
                .orElse(null), "the field holding the lambda must be inspected");
    }

    // The same block, in a plain method body instead of a lambda. Narrows the defect: if this passes and
    // the one above fails, the lambda is part of the trigger; if both fail, any arrow-case block is.
    private static final String WITHOUT_LAMBDA = """
            package a.b;
            public class Y {
                static String name(RuntimeException e) {
                    return e.getMessage();
                }
                static String extract(RuntimeException e, int code) {
                    return switch (code) {
                        case 23505 -> {
                            final String constraint = name(e);
                            final int onIndex = constraint == null ? -1 : constraint.indexOf(" ON ");
                            yield onIndex > 0 ? constraint.substring(0, onIndex) : constraint;
                        }
                        default -> null;
                    };
                }
            }
            """;

    @Test
    public void theSameBlockInAPlainMethodBody() {
        TypeInfo typeInfo = scan("a.b.Y", WITHOUT_LAMBDA);
        assertNotNull(typeInfo);
    }
}
