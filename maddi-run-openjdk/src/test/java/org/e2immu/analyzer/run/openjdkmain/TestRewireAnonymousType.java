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

package org.e2immu.analyzer.run.openjdkmain;

import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.openjdk.JavaInspectorImpl;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression for the anonymous-types rewire gap (see {@code docs/handoff-anonymous-types-rewire.md}). Anonymous and
 * lambda types are not part of {@code subTypes()}, so neither the structural rewire phases nor
 * {@code InfoMapImpl.seed} walk their members. A carried analysis value (e.g. {@code METHOD_LINKS}) can name such a
 * member, and looking it up used to fail hard in {@code InfoMapImpl} ("Cannot find ...$0...").
 * <p>
 * The dominant production trigger was the <em>seed</em> path: a re-scanned (rebuilt) source set is re-parsed whole
 * and its unchanged siblings' analysis is carried onto their rebuilt objects through the seed map — old resolves to
 * new by fqn + source set. But seed never registered anonymous members, so carrying a value that reached one blew up.
 * Rebuilt objects <em>are</em> the new objects, so the correct answer is identity; this checks exactly that, plus
 * that a full rewire still resolves anonymous members to their rewired copies.
 */
public class TestRewireAnonymousType {

    private static final String T_FQN = "a.T";

    @Language("java")
    private static final String SOURCE = """
            package a;
            import java.util.function.Supplier;
            public class T {
                public Supplier<String> make(String s) {
                    return new Supplier<String>() {
                        @Override public String get() { return s; }
                    };
                }
            }
            """;

    @TempDir
    Path root;
    private JavaInspector javaInspector;

    private InputConfiguration inputConfiguration() {
        var main = new SourceSetImpl.Builder().setName("main")
                .setSourceDirectories(List.of(root.resolve("main-src")))
                .setUri(root.resolve("main-src").toUri()).build();
        return new InputConfigurationImpl.Builder().addSourceSets(main)
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES).build();
    }

    private MethodInfo anonMethodOf(TypeInfo type) {
        List<Info> order = new PrepAnalyzer(javaInspector.runtime(),
                new PrepAnalyzer.Options.Builder().setFaultTolerant(true).build()).doPrimaryType(type);
        // the analysis order includes the anonymous type's methods; take the one whose owner is anonymous
        return order.stream()
                .filter(i -> i instanceof MethodInfo m && m.typeInfo().isAnonymous())
                .map(i -> (MethodInfo) i)
                .findFirst().orElseThrow(() -> new AssertionError("no anonymous method in the analysis order"));
    }

    @DisplayName("looking up an anonymous member of a rebuilt (seeded) type resolves to identity, not a failure")
    @Test
    public void testSeededAnonymousMemberResolvesToIdentity() throws IOException {
        Path mainSrc = Files.createDirectories(root.resolve("main-src/a"));
        Files.writeString(mainSrc.resolve("T.java"), SOURCE);

        javaInspector = new JavaInspectorImpl(true, false);
        javaInspector.initialize(inputConfiguration());
        ParseResult pr = javaInspector.parse(Map.of(), JavaInspectorImpl.DETAILED_SOURCES).parseResult();
        TypeInfo type = pr.findType(T_FQN);
        MethodInfo anonGet = anonMethodOf(type);
        assertTrue(anonGet.typeInfo().isAnonymous() && anonGet.fullyQualifiedName().contains("$"),
                "the fixture really has an anonymous member: " + anonGet.fullyQualifiedName());

        // T as a rebuilt (rescanned) primary type: seeded onto itself, nothing to rewire. seed() does not walk the
        // anonymous member, so the strict lookup used to NPE ("Cannot find a.T.$0.get()"). It must return identity.
        InfoMap seeded = javaInspector.runtime().newInfoMap(Set.of(), Set.of(type));
        MethodInfo throughSeed = assertDoesNotThrow(() -> seeded.methodInfo(anonGet),
                "carrying a value through a rebuilt type's anonymous member must not fail");
        assertSame(anonGet, throughSeed, "a rebuilt object is already the new object: identity");
        assertSame(anonGet.typeInfo(), seeded.typeInfo(anonGet.typeInfo()), "same for the anonymous type itself");

        // A full rewire, by contrast, produces fresh copies: the anonymous member must resolve to a rewired object.
        InfoMap rewire = javaInspector.runtime().newInfoMap(Set.of(type), Set.of());
        rewire.rewireAll();
        MethodInfo rewired = assertDoesNotThrow(() -> rewire.methodInfo(anonGet),
                "a rewired anonymous member must resolve");
        assertNotSame(anonGet, rewired, "the rewire produced a fresh anonymous member");
        assertEquals(anonGet.fullyQualifiedName(), rewired.fullyQualifiedName(), "same identity, new object");
    }
}
