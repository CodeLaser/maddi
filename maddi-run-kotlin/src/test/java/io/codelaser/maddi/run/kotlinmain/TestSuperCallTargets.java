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

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.ExplicitConstructorInvocation;
import io.codelaser.maddi.inspection.mixed.MixedProjectInspector;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import io.codelaser.maddi.kotlin.api.PlaceholderCensus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a Kotlin `super` binds to when the parent is Java -- source or a class file.
 * <ul>
 *   <li>{@code super.visitX(s)} in a class with a superclass AND an interface: {@code super}'s own type is then not
 *   the superclass, and the call was a placeholder -- 112 of detekt's 333 {@code super.visitX(…)}, every rule that
 *   also implements {@code RequiresAnalysisApi}.</li>
 *   <li>{@code class D : V()} where the Java class V declares no constructor: the Java front end's generated one is
 *   synthetic, and the search that skips kotlinc's synthetic overloads skipped it too.</li>
 * </ul>
 */
public class TestSuperCallTargets {

    @Test
    public void superBindsToTheParentsMember(@TempDir Path tmp) throws Exception {
        Path kDir = tmp.resolve("src/main/kotlin");
        Path jDir = tmp.resolve("src/main/java");
        Files.createDirectories(kDir.resolve("a"));
        Files.createDirectories(jDir.resolve("s"));
        Files.writeString(jDir.resolve("s/V.java"), """
                package s;
                public class V { public void visitX(String s) {} }
                """);
        Files.writeString(kDir.resolve("a/K.kt"), """
                package a
                import s.V
                import org.jetbrains.kotlin.psi.KtCallExpression
                import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
                interface Marker
                open class D : V()
                class Plain : D() { override fun visitX(s: String) { super.visitX(s) } }
                class Marked : D(), Marker { override fun visitX(s: String) { super.visitX(s) } }
                class Library : KtTreeVisitorVoid(), Marker {
                    override fun visitCallExpression(expression: KtCallExpression) { super.visitCallExpression(expression) }
                }
                """);
        SourceSet javaSet = new SourceSetImpl.Builder().setName("java/main")
                .setSourceDirectories(List.of(jDir)).setUri(jDir.toUri()).build();
        SourceSet kotlinSet = new SourceSetImpl.Builder().setName("kotlin/main")
                .setSourceDirectories(List.of(kDir)).setUri(kDir.toUri()).setDependencies(List.of(javaSet)).build();
        String cp = System.getProperty("maddi.k2.classpath", "");
        MixedProjectInspector.Result parsed = new MixedProjectInspector().parse(new InputConfigurationImpl.Builder()
                .addSourceSets(javaSet).addSourceSets(kotlinSet)
                .addClassPath(jar(cp, "kotlin-stdlib-\\d[^-]*\\.jar")).addClassPath(jar(cp, "kotlin-compiler-\\d[^-]*\\.jar"))
                .build());
        PlaceholderCensus census = PlaceholderCensus.of(parsed.getKotlinTypes());
        assertEquals(0, census.getTotal(), String.join("\n", census.dumpLines()));

        for (String name : List.of("Plain", "Marked")) {
            MethodInfo target = superCall(type(parsed, "a." + name).findUniqueMethod("visitX", 1)).methodInfo();
            assertEquals("s.V", target.typeInfo().fullyQualifiedName(), name);
        }
        MethodInfo visitCall = superCall(type(parsed, "a.Library").findUniqueMethod("visitCallExpression", 1)).methodInfo();
        assertEquals("visitCallExpression", visitCall.name());
        assertEquals("org.jetbrains.kotlin.psi.KtTreeVisitorVoid", visitCall.typeInfo().fullyQualifiedName(),
                "the library parent's method, not the override");

        ExplicitConstructorInvocation eci = (ExplicitConstructorInvocation) type(parsed, "a.D").findConstructor(0)
                .methodBody().statements().getFirst();
        assertEquals("s.V", eci.methodInfo().typeInfo().fullyQualifiedName());
        assertTrue(eci.methodInfo().isSyntheticConstructor(), "V's generated default constructor");
    }

    private static String jar(String classPath, String pattern) {
        return Stream.of(classPath.split(java.io.File.pathSeparator))
                .filter(p -> Path.of(p).getFileName().toString().matches(pattern)).findFirst().orElseThrow();
    }

    private static TypeInfo type(MixedProjectInspector.Result parsed, String fqn) {
        return parsed.getKotlinTypes().stream().filter(t -> t.fullyQualifiedName().equals(fqn)).findFirst().orElseThrow();
    }

    private static MethodCall superCall(MethodInfo method) {
        List<MethodCall> calls = new ArrayList<>();
        method.methodBody().visit((Element e) -> {
            if (e instanceof MethodCall mc) calls.add(mc);
            return true;
        });
        assertEquals(1, calls.size(), "one call in " + method.fullyQualifiedName());
        return calls.getFirst();
    }
}
