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

import io.codelaser.maddi.cst.api.element.SourceSet;
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
 * A LIBRARY function with a vararg parameter, called omitting a defaulted argument or passing one after the vararg,
 * in the class-file world: `path.writeText(s)` is `writeText(Path, CharSequence, Charset, OpenOption...)` with the
 * charset omitted; `splitToSequence(".")` has two defaulted parameters AFTER its vararg, so the JVM parameter is a
 * plain `String[]`. A library callee has no `$default` here: the omitted parameters are filled with their zero value
 * (see KotlinBodyConverter.callArguments).
 */
public class TestLibraryVarargCalls {
    @Test
    public void theVarargIsBoundAsKotlincBindsIt(@TempDir Path tmp) throws Exception {
        Path kDir = tmp.resolve("src/main/kotlin");
        Path jDir = tmp.resolve("src/main/java");
        Files.createDirectories(kDir.resolve("a"));
        Files.createDirectories(jDir.resolve("s"));
        Files.writeString(jDir.resolve("s/Trivial.java"), "package s;\npublic class Trivial { public int n; }\n");
        Files.writeString(kDir.resolve("a/K.kt"), """
                package a
                import java.nio.file.Path
                import kotlin.io.path.writeText
                import com.intellij.psi.PsiElement
                import org.jetbrains.kotlin.psi.KtElement
                import org.jetbrains.kotlin.psi.KtNamedFunction
                import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypesAndPredicate
                class K {
                    fun a(p: Path) { p.writeText("x") }
                    fun b(s: String): Sequence<String> = s.splitToSequence(".")
                    fun c(e: PsiElement): KtElement? =
                        e.getParentOfTypesAndPredicate(true, KtNamedFunction::class.java, KtElement::class.java) { true }
                }
                """);
        SourceSet javaSet = new SourceSetImpl.Builder().setName("java/main")
                .setSourceDirectories(List.of(jDir)).setUri(jDir.toUri()).build();
        SourceSet kotlinSet = new SourceSetImpl.Builder().setName("kotlin/main")
                .setSourceDirectories(List.of(kDir)).setUri(kDir.toUri()).setDependencies(List.of(javaSet)).build();
        String cp = System.getProperty("maddi.k2.classpath", "");
        MixedProjectInspector.Result parsed = new MixedProjectInspector().parse(new InputConfigurationImpl.Builder()
                .addSourceSets(javaSet).addSourceSets(kotlinSet)
                .addClassPath(jar(cp, "kotlin-stdlib-\\d[^-]*\\.jar")).addClassPath(jar(cp, "kotlin-compiler-\\d[^-]*\\.jar")).build());
        PlaceholderCensus census = PlaceholderCensus.of(parsed.getKotlinTypes());
        assertEquals(0, census.getTotal(), String.join("\n", census.dumpLines()));
        TypeInfo k = parsed.getKotlinTypes().stream().filter(t -> t.simpleName().equals("K")).findFirst().orElseThrow();
        StringBuilder actual = new StringBuilder();
        k.methods().forEach(m -> actual.append(m.name()).append(": ").append(m.methodBody().statements()).append('\n'));
        assertEquals("""
                a: [PathsKt.writeText(p,"x",null);]
                b: [return StringsKt.splitToSequence(s,new String[]{"."},false,0);]
                c: [return PsiUtilsKt.getParentOfTypesAndPredicate(e,true,new Class[]{KtNamedFunction.class,KtElement.class},it->true);]
                """, actual.toString());
    }

    private static String jar(String classPath, String pattern) {
        return Stream.of(classPath.split(java.io.File.pathSeparator))
                .filter(p -> Path.of(p).getFileName().toString().matches(pattern)).findFirst().orElseThrow();
    }
}
