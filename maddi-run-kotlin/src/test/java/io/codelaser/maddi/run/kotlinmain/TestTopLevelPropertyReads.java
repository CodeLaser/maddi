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
package io.codelaser.maddi.run.kotlinmain;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.mixed.MixedProjectInspector;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import io.codelaser.maddi.kotlin.api.PlaceholderCensus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A top-level property read from ANOTHER file or a library, by its bare name -- detekt's `NL` and ktlint's
 * `INDENT_SIZE_PROPERTY` (36×): Java reads it through the facade, a `const val` or `@JvmField` as the static field and
 * anything else through the static getter.
 */
public class TestTopLevelPropertyReads {
    @Test
    public void readThroughTheFacade(@TempDir Path tmp) throws Exception {
        Path kDir = tmp.resolve("src/main/kotlin");
        Path jDir = tmp.resolve("src/main/java");
        Files.createDirectories(kDir.resolve("a"));
        Files.createDirectories(jDir.resolve("s"));
        Files.writeString(jDir.resolve("s/Trivial.java"), "package s;\npublic class Trivial { public int n; }\n");
        Files.writeString(kDir.resolve("a/Core.kt"), """
                package a
                val NL: String = "\\n"
                const val MAX: Int = 3
                @JvmField val J: String = "j"
                """);
        Files.writeString(kDir.resolve("a/Use.kt"), """
                package a
                import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
                import kotlin.math.PI
                import kotlin.time.Duration
                import kotlin.time.DurationUnit
                import kotlin.time.toDuration
                class Use {
                    fun nl(): String = NL
                    fun max(): Int = MAX
                    fun j(): String = J
                    fun pi(): Double = PI
                    fun suspended(): Any = COROUTINE_SUSPENDED
                    // a call into a library facade that ALSO holds a const (DurationKt.NANOS_IN_MILLIS): building its
                    // field once threw an NPE (its access combined with the facade's, not yet computed), stopping detekt
                    fun dur(): Duration = 5.toDuration(DurationUnit.SECONDS)
                    // a facade holding PRIVATE consts (the stdlib's `State_Ready`, …): no reader, so no field
                    fun seq(): Sequence<Int> = sequence { }
                }
                """);
        SourceSet javaSet = new SourceSetImpl.Builder().setName("java/main")
                .setSourceDirectories(List.of(jDir)).setUri(jDir.toUri()).build();
        SourceSet kotlinSet = new SourceSetImpl.Builder().setName("kotlin/main")
                .setSourceDirectories(List.of(kDir)).setUri(kDir.toUri()).setDependencies(List.of(javaSet)).build();
        String cp = System.getProperty("maddi.k2.classpath", "");
        String stdlib = Stream.of(cp.split(java.io.File.pathSeparator))
                .filter(p -> Path.of(p).getFileName().toString().matches("kotlin-stdlib-\\d[^-]*\\.jar")).findFirst().orElseThrow();
        MixedProjectInspector.Result parsed = new MixedProjectInspector().parse(new InputConfigurationImpl.Builder()
                .addSourceSets(javaSet).addSourceSets(kotlinSet).addClassPath(stdlib).build());
        PlaceholderCensus census = PlaceholderCensus.of(parsed.getKotlinTypes());
        assertEquals(0, census.getTotal(), String.join("\n", census.dumpLines()));
        TypeInfo use = parsed.getKotlinTypes().stream().filter(t -> t.simpleName().equals("Use")).findFirst().orElseThrow();
        StringBuilder actual = new StringBuilder();
        use.methods().forEach(m -> actual.append(m.name()).append(": ").append(m.methodBody().statements()).append('\n'));
        // ⚠ a `const` is folded to its value before this route is reached (`MAX`, `PI`): pre-existing, and not a
        // placeholder either way. ⚠ A library getter binds to the multi-file PART class (`IntrinsicsKt__IntrinsicsKt`)
        // where Java names the facade (`IntrinsicsKt`): the facade locator every library top-level function shares.
        assertEquals("""
                nl: [return CoreKt.getNL();]
                max: [return 3;]
                j: [return CoreKt.J;]
                pi: [return 3.141592653589793;]
                suspended: [return IntrinsicsKt__IntrinsicsKt.getCOROUTINE_SUSPENDED();]
                dur: [return DurationKt.toDuration(5,DurationUnit.SECONDS);]
                seq: [return SequencesKt__SequenceBuilderKt.sequence($receiver->{});]
                """, actual.toString());
        TypeInfo sequenceBuilder = ((MethodCall) use.findUniqueMethod("seq", 0).methodBody().statements().getFirst()
                .expression()).methodInfo().typeInfo();
        assertEquals(List.of(), sequenceBuilder.fields().stream().map(f -> f.name()).toList(),
                "a private const has no reader outside its file");
    }
}
