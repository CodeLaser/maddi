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

import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.LocalTypeDeclaration;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.openjdk.JavaInspectorImpl;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
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
 * The local-class sibling of {@link TestRewireAnonymousType}. A class declared inside a method body is not part of
 * {@code subTypes()} either — {@code ParseTypeDeclaration.parseLocal} never calls {@code addSubType} — so it is
 * registered lazily, when {@code LocalTypeDeclarationImpl.rewire} reaches the statement that declares it.
 * <p>
 * What made it different from the anonymous case, and what this pins down: an anonymous type reproduces its own
 * name from {@code enclosing + "." + simpleName} ({@code "$0"}), so the rewire copy came out with the original's
 * fully qualified name by construction. A local class does not. It is named
 * {@code <enclosingType>.<index>$<method>$<SimpleName>} while keeping {@code SimpleName} as its simple name, so
 * recomputing produced {@code <enclosingType>.SimpleName} — a copy under a <em>different</em> fqn, which the info
 * map (keyed on fqn + source set: the BASIC RULE OF REWIRING) could never hand back.
 * <p>
 * The symptom was not a wrong name. {@code rewirePhase1} asks the map for the type it has just registered, misses,
 * and {@code InfoMapImpl}'s on-demand registration tries again — unbounded recursion. Measured on a decompiler
 * corpus before the fix: 21 methods across 3 types, each losing a whole primary type. It surfaces only when
 * something rewrites the enclosing method; an ordinary parse never asks, which is how it outlived the anonymous
 * case.
 */
public class TestRewireLocalClass {

    private static final String T_FQN = "a.T";

    @Language("java")
    private static final String SOURCE = """
            package a;
            public class T {
                public int count(int n) {
                    class Node {
                        final int value;
                        Node(int value) { this.value = value; }
                        int twice() { return 2 * value; }
                    }
                    return new Node(n).twice();
                }
            }
            """;

    @TempDir
    Path root;
    private JavaInspector javaInspector;
    private TypeInfo type;

    @BeforeEach
    public void parse() throws IOException {
        Path mainSrc = Files.createDirectories(root.resolve("main-src/a"));
        Files.writeString(mainSrc.resolve("T.java"), SOURCE);

        var main = new SourceSetImpl.Builder().setName("main")
                .setSourceDirectories(List.of(root.resolve("main-src")))
                .setUri(root.resolve("main-src").toUri()).build();
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder().addSourceSets(main)
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES).build();

        javaInspector = new JavaInspectorImpl(true, false);
        javaInspector.initialize(inputConfiguration);
        ParseResult pr = javaInspector.parse(Map.of(), JavaInspectorImpl.DETAILED_SOURCES).parseResult();
        type = pr.findType(T_FQN);
    }

    /**
     * The local class as the parse produced it, reached through the statement that declares it — which is the
     * only way to reach it: it is deliberately <em>not</em> in {@code subTypes()}, and that is the reason the
     * structural rewire phases never see it.
     */
    private TypeInfo localClass() {
        MethodInfo count = type.findUniqueMethod("count", 1);
        assertTrue(type.subTypes().isEmpty(), "a local class is not a subtype: " + type.subTypes());
        return count.methodBody().statements().stream()
                .filter(s -> s instanceof LocalTypeDeclaration)
                .map(s -> ((LocalTypeDeclaration) s).typeInfo())
                .findFirst().orElseThrow(() -> new AssertionError("no local type declaration in count()"));
    }

    @DisplayName("a rewired local class keeps its fully qualified name, and resolves through the info map")
    @Test
    public void testLocalClassIsRewired() {
        TypeInfo node = localClass();
        // the fixture really is a LOCAL class, not an anonymous one: that distinction is the whole bug
        assertFalse(node.isAnonymous(), "a local class keeps its written name: " + node.simpleName());
        assertNotNull(node.enclosingMethod(), "a local class knows the method that declares it");
        assertEquals("Node", node.simpleName());
        assertTrue(node.fullyQualifiedName().endsWith("$count$Node"),
                "the fqn encodes the enclosing method, which the simple name does not reproduce: "
                + node.fullyQualifiedName());

        InfoMap rewire = javaInspector.runtime().newInfoMap(Set.of(type));
        // used to be a StackOverflowError: the copy landed under a.T.Node, so the map never resolved
        // a.T.0$count$Node and the on-demand registration retried forever
        assertDoesNotThrow(rewire::rewireAll, "rewiring a type with a local class must terminate");

        TypeInfo rewired = assertDoesNotThrow(() -> rewire.typeInfo(node), "the local class must resolve");
        assertNotSame(node, rewired, "the rewire produced a fresh object");
        assertEquals(node.fullyQualifiedName(), rewired.fullyQualifiedName(),
                "BASIC RULE OF REWIRING: same identity, new object");
        assertNotNull(rewired.enclosingMethod(), "the copy knows its enclosing method too");
        assertEquals(node.enclosingMethod().fullyQualifiedName(),
                rewired.enclosingMethod().fullyQualifiedName());

        MethodInfo twice = node.findUniqueMethod("twice", 0);
        MethodInfo rewiredTwice = assertDoesNotThrow(() -> rewire.methodInfo(twice),
                "a member of the local class must resolve");
        assertNotSame(twice, rewiredTwice);
        assertEquals(twice.fullyQualifiedName(), rewiredTwice.fullyQualifiedName());
    }
}
