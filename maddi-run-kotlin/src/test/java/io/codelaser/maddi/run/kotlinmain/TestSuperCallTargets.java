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
