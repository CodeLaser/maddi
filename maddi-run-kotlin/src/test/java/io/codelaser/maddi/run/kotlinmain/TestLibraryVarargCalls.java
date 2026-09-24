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
                a: [PathsKt__PathReadWriteKt.writeText(p,"x",null);]
                b: [return StringsKt__StringsKt.splitToSequence(s,new String[]{"."},false,0);]
                c: [return PsiUtilsKt.getParentOfTypesAndPredicate(e,true,new Class[]{KtNamedFunction.class,KtElement.class},it->true);]
                """, actual.toString());
    }

    private static String jar(String classPath, String pattern) {
        return Stream.of(classPath.split(java.io.File.pathSeparator))
                .filter(p -> Path.of(p).getFileName().toString().matches(pattern)).findFirst().orElseThrow();
    }
}
