/*
 * maddi: a static analyzer for effective and eventual immutability
 * Copyright 2020-2025, Bart Naudts, https://www.e2immu.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 */

package io.codelaser.maddi.java.openjdk.other;

import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ⛔ <b>A METHOD HALF-SCANNED BY A DROPPED COMPILATION UNIT IS NEVER FINISHED, AND ITS TYPE IS COMMITTED
 * AROUND IT.</b> The shape behind {@code docs/handoff-uninspected-methods-null-access.md} (2026-08-23, 18 methods,
 * 104 null {@code access()} reads, every one of them a method in which the source scan threw):
 * <ol>
 *     <li>{@code ScanCompilationUnit.visitMethod} registers the {@link MethodInfo} on its type AND in the
 *     scanner's method-symbol map (the {@code typeData.put}) BEFORE it converts annotations, and calls
 *     {@code computeAccess()} as its very LAST statement. An annotation whose type javac could not resolve throws
 *     in between: the method exists, is findable by its symbol, and has neither access nor commit.</li>
 *     <li>The compilation unit is dropped (accumulate mode), so the end-of-scan walk that commits a primary type's
 *     methods never visits it — but the type is registered with the class-symbol scanner, so it is still "loaded
 *     for this source set".</li>
 *     <li>{@code JavaInspectorImpl}'s copy-into-CTM loop commits every loaded, uninspected primary type through
 *     {@code ClassSymbolScanner.commitType} → {@code loadType(COMPLETE)}. There, {@code addMemberToType} SKIPS any
 *     member whose symbol is already in the map — "the source scan handled it" — which is exactly the half-scanned
 *     method. The {@code computeAccess().commit()} one line below is unreachable for it, and the TYPE is then
 *     committed.</li>
 * </ol>
 * The result is a committed type holding an uncommitted method whose {@code access()} answers null forever;
 * the first reader to ask {@code isPrivate()} dies (the guard phase) or is isolated (the analysis).
 * <p>
 * This test replays step 3 exactly as the product does it (same predicate, same call), on a unit dropped by an
 * unresolvable annotation.
 * <p>
 * ⚠ The annotation carries an unresolvable ARGUMENT, {@code @Missing(Gone.X)}, named nowhere else in the file (an
 * imported name is stubbed too). A bare {@code @Missing} dropped the unit up to JDK 26; JDK 27's javac gives it
 * an error type that maddi stubs, the unit survives, and every fixture here stopped testing anything. The
 * argument still throws at the same point: inside the annotation's conversion, after the member is registered.
 */
public class TestDroppedUnitMethodAccess extends CommonTest {

    /** The unresolvable annotation is on a PARAMETER: the parameter is added, then its annotation throws. */
    @Language("java")
    private static final String OUTER = """
            package a.b;
            import org.nowhere.Missing;
            public interface Dropped {
                String first();
                void go(@Missing(Gone.X) String s);
                String last();
                interface Builder {
                    Builder setName(String name);
                }
            }
            """;

    /**
     * The corpus's majority shape: the throw is inside a NESTED type (14 of the 18 were in a {@code Builder}),
     * so the owner's methods are all scanned and the nested type is half-scanned. The nested type is reached
     * through {@code COMPLETE_SUB}, where {@code alwaysLoad} is true and the same "already in the map" conjunct
     * still skips the half-scanned method.
     */
    @Language("java")
    private static final String NESTED = """
            package a.b;
            import org.nowhere.Missing;
            public interface Dropped {
                String first();
                interface Builder {
                    Builder setFirst(String first);
                    Builder setName(@Missing(Gone.X) String name);
                    Builder setLast(String last);
                }
            }
            """;

    /**
     * The corpus's other shape ({@code MaddiDaemonProcess}): the scan throws in {@code go}, and completing the
     * type from its symbol then throws AGAIN, on a member the scan never reached whose parameter type does not
     * resolve. {@code commitType} fails, the type stays uncommitted -- and stays reachable. Two methods used to
     * come out of this with a null access: {@code go} (abandoned by the scan) and {@code later} (abandoned by
     * {@code addMethodToType}, which had the same register-first-compute-access-last order).
     */
    @Language("java")
    private static final String COMMIT_THROWS_TOO = """
            package a.b;
            import org.nowhere.Missing;
            import org.nowhere.Gone;
            public interface Dropped {
                String first();
                void go(@Missing(Absent.X) String s);
                void later(Gone g);
                interface Builder {
                    Builder setName(String name);
                }
            }
            """;

    /** The field twin: {@code visitVariable} registers the field, then converts its annotations, then the access. */
    @Language("java")
    private static final String FIELD = """
            package a.b;
            import org.nowhere.Missing;
            public class Dropped {
                public final String first = "f";
                @Missing(Gone.X) private final String hidden = "h";
                public static final int LAST = 3;
                interface Builder {
                    Builder setName(String name);
                }
            }
            """;

    /** A surviving unit that uses the dropped type: this is how the analysis reaches its members. */
    @Language("java")
    private static final String USER = """
            package a.b;
            public class User {
                Object use(Dropped d, Dropped.Builder b) {
                    return b.setName("y") + " " + d;
                }
            }
            """;

    @DisplayName("the scan throws in a method of the primary type")
    @Test
    public void inTheOuterType() {
        check(OUTER, true);
    }

    @DisplayName("the scan throws in a field's annotation")
    @Test
    public void inAField() {
        check(FIELD, true);
    }

    @DisplayName("the scan throws in a method of a nested type")
    @Test
    public void inTheNestedType() {
        check(NESTED, true);
    }

    @DisplayName("the scan throws in one method, and completing the type throws in another")
    @Test
    public void commitThrowsToo() {
        // ⚠ Up to JDK 26 completing the type threw on 'later(Gone g)'. JDK 27's javac stubs Gone, so the commit
        // succeeds and every member must come out committed: the failed-commit branch has no fixture on 27.
        check(COMMIT_THROWS_TOO, java.lang.Runtime.version().feature() >= 27);
    }

    private void check(String source, boolean commitSucceeds) {
        // as the product runs a project without lombok: with a processor, javac skips every method body after
        // the first error, and the class's implicit constructor then throws before any field is reached
        annotationProcessing = false;
        Map<String, TypeInfo> types = scan(true, "a.b.Dropped", source, "a.b.User", USER);
        assertNull(types.get("a.b.Dropped"), "the unit is dropped: " + types.keySet());
        assertNotNull(types.get("a.b.User"), "the unit that uses it survives: " + types.keySet());

        // JavaInspectorImpl, "copy into CTM": every loaded, uninspected primary type is committed through the
        // class-symbol scanner, dropped ones included. Same predicate, same call, same tolerance of a failure.
        List<String> commitFailures = new ArrayList<>();
        // a copy, as JavaInspectorImpl takes one: committing can load (stub) more types into the live collection
        for (TypeInfo typeInfo : List.copyOf(classSymbolScanner.typesLoaded())) {
            if (typeInfo.isPrimaryType() && !typeInfo.hasBeenInspected()) {
                try {
                    classSymbolScanner.commitType(typeInfo);
                } catch (RuntimeException re) {
                    commitFailures.add(typeInfo.fullyQualifiedName() + ": " + re.getMessage());
                }
            }
        }

        TypeInfo dropped = infoByFqn.getType("a.b.Dropped", sourceSet);
        assertNotNull(dropped, "the dropped type stays reachable through the registry");
        TypeInfo builder = dropped.findSubType("Builder");
        assertEquals(commitSucceeds, commitFailures.isEmpty(), commitFailures.toString());
        assertEquals(commitSucceeds, dropped.hasBeenInspected(), "commitType committed the type");

        // the whole picture, not the first failure: which methods have no access, which are not committed
        List<String> problems = new ArrayList<>();
        Stream.concat(dropped.methods().stream(), builder.methods().stream()).forEach(mi -> {
            if (mi.access() == null) problems.add("null access: " + mi);
            // ⛔ ACCESS WAS NOT THE ONLY THING COMPUTED AFTER THE THROW POINT. visitMethod sets the return type
            // AFTER converting the annotations (:905) and the access BEFORE them (:867, moved there by the
            // 2026-08-23 fix), so a method abandoned between the two now answers access() and NOT returnType() --
            // and finishing it only ever filled in the fields that had already been seen to be null. Measured
            // 2026-08-25 in the IDE daemon on the CodeLaser tree: 0 null-access reads and 862 null-returnType
            // ones, the same defect one field along. Ask for everything the compiled path sets, not for the
            // field that failed last time.
            if (mi.returnType() == null) problems.add("null returnType: " + mi);
            if (commitSucceeds && !mi.hasBeenInspected()) problems.add("uncommitted in a committed type: " + mi);
        });
        for (FieldInfo fi : dropped.fields()) {
            if (fi.access() == null) problems.add("null access: " + fi);
            if (commitSucceeds && !fi.hasBeenInspected()) problems.add("uncommitted in a committed type: " + fi);
        }
        assertEquals(List.of(), problems, String.join("\n", problems));
    }
}
