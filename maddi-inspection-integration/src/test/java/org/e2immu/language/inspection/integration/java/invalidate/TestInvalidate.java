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

package org.e2immu.language.inspection.integration.java.invalidate;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.integration.java.CommonTest2;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.e2immu.language.inspection.api.integration.JavaInspector.InvalidationState.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestInvalidate extends CommonTest2 {

    private static final String PROCESSOR_FQN = "a.b.util.Processor";
    private static final String ISOURCE_FQN = "a.b.ISource";

    @Language("java")
    String PROCESSOR = """
            package a.b.util;
            import org.slf4j.Logger;
            import org.slf4j.LoggerFactory;
            public class Processor {
                private static final Logger LOGGER = LoggerFactory.getLogger(Processor.class);
            
                public Processor() {
                }
            
                public record ProcessResult(String someResult) {
                }
            }
            """;

    @Language("java")
    String ISOURCE = """
            package a.b;
            import a.b.util.Processor;
            public interface ISource { Processor.ProcessResult processResult(); }
            """;

    @Language("java")
    String ISOURCE_CHANGED = """
            package a.b;
            import a.b.util.Processor;
            public interface ISource {
                Processor.ProcessResult processResult(); 
            }
            """;

    @Language("java")
    String SOURCE = """
            package a.b;
            import a.b.util.Processor;
            import java.util.Set;
            public record Source(String name, String src, Set<String> tags, Processor.ProcessResult processResult) implements ISource {
            }
            """;


    @Test
    public void testReload() throws IOException {
        Map<String, String> sourcesByFqn = Map.of(ISOURCE_FQN, ISOURCE, "a.b.Source", SOURCE,
                PROCESSOR_FQN, PROCESSOR);
        ParseResult pr1 = init(sourcesByFqn);
        TypeInfo iSource = pr1.findType(ISOURCE_FQN);
        assertEquals("5qzB4ttzbH5oaGHwsCf4Qw==", iSource.compilationUnit().fingerPrintOrNull().toString());
        assertEquals(3, pr1.primaryTypes().size());

        Map<String, String> sourcesByFqn2 = Map.of(ISOURCE_FQN, ISOURCE_CHANGED, "a.b.Source", SOURCE,
                PROCESSOR_FQN, PROCESSOR);
        Map<String, String> sourcesByURIString = sourcesByURIString(sourcesByFqn2);
        JavaInspector.ReloadResult rr = javaInspector.reloadSources(makeInputConfiguration(sourcesByURIString,
                Map.of()), sourcesByURIString);
        assertEquals(0, rr.problems().size());
        assertEquals(1, rr.sourceHasChanged().size());
        assertEquals("[a.b.ISource]", rr.sourceHasChanged().toString());

        // this test is the precursor to test4, where Processor stays unchanged, ISource is invalidated,
        // and Source is rewired.
    }

    /**
     * A rewired type's <em>subtypes</em> must be re-registered too: {@code Processor} holds the nested record
     * {@code ProcessResult}, and rewiring Processor builds a new one.
     * <p>
     * It used to keep the old one: {@code setRewiredType} was called with the primary types only, and updated the
     * single trie entry for the FQN it was given — so the trie went on answering for {@code ProcessResult} with the
     * object the rewire had replaced. Both inspectors now register every type {@code InfoMap.rewiredTypes()} reports.
     */
    @Test
    public void testSubTypesAreReRegistered() throws IOException {
        Map<String, String> sourcesByFqn = Map.of(ISOURCE_FQN, ISOURCE, "a.b.Source", SOURCE,
                PROCESSOR_FQN, PROCESSOR);
        ParseResult pr1 = init(sourcesByFqn);
        TypeInfo processor1 = pr1.findType(PROCESSOR_FQN);
        TypeInfo nested1 = processor1.subTypes().getFirst();
        assertEquals("a.b.util.Processor.ProcessResult", nested1.fullyQualifiedName());
        SourceSet sourceSet = processor1.compilationUnit().sourceSet();
        assertSame(nested1, javaInspector.compiledTypesManager().get(nested1.fullyQualifiedName(), sourceSet));

        JavaInspector.ParseOptions po = new JavaInspector.ParseOptions.Builder()
                .setInvalidated(t -> switch (t.simpleName()) {
                    case "ISource" -> INVALID;
                    case "Processor", "Source" -> REWIRE;
                    default -> throw new UnsupportedOperationException(t.fullyQualifiedName());
                }).build();
        ParseResult pr2 = javaInspector.parse(sourcesByURIString(sourcesByFqn), po).parseResult();

        TypeInfo nested2 = pr2.findType(PROCESSOR_FQN).subTypes().getFirst();
        assertNotSame(nested1, nested2, "the rewire builds a new nested type");
        assertSame(nested2, javaInspector.compiledTypesManager().get(nested1.fullyQualifiedName(), sourceSet),
                "the registry must hand out the nested type the rewired Processor actually holds");
    }

    @Test
    public void test1() throws IOException {
        Map<String, String> sourcesByFqn = Map.of(ISOURCE_FQN, ISOURCE, "a.b.Source", SOURCE,
                PROCESSOR_FQN, PROCESSOR);
        ParseResult pr1 = init(sourcesByFqn);
        TypeInfo processor = pr1.findType(PROCESSOR_FQN);
        assertEquals("swTzAiYtIXTHZ/quxa3mFQ==", processor.compilationUnit().fingerPrintOrNull().toString());

        assertEquals(3, pr1.primaryTypes().size());
        Map<String, String> sourcesByURIString = sourcesByURIString(sourcesByFqn);

        // all unchanged
        JavaInspector.ParseOptions po2 = new JavaInspector.ParseOptions.Builder()
                .setInvalidated(t -> UNCHANGED)
                .build();
        ParseResult pr2 = javaInspector.parse(sourcesByURIString, po2).parseResult();
        assertEquals(3, pr2.primaryTypes().size());
        for (TypeInfo pt1 : pr1.primaryTypes()) {
            TypeInfo pt2 = pr2.findType(pt1.fullyQualifiedName());
            assertSame(pt1, pt2);
        }
    }

    @Test
    public void test2() throws IOException {
        Map<String, String> sourcesByFqn = Map.of("a.b.ISource", ISOURCE, "a.b.Source", SOURCE,
                PROCESSOR_FQN, PROCESSOR);
        ParseResult pr1 = init(sourcesByFqn);
        assertEquals(3, pr1.primaryTypes().size());
        Map<String, String> sourcesByURIString = sourcesByURIString(sourcesByFqn);

        // all changed
        JavaInspector.ParseOptions po2 = new JavaInspector.ParseOptions.Builder()
                .setInvalidated(t -> INVALID)
                .build();
        ParseResult pr2 = javaInspector.parse(sourcesByURIString, po2).parseResult();
        assertEquals(3, pr2.primaryTypes().size());
        for (TypeInfo pt1 : pr1.primaryTypes()) {
            TypeInfo pt2 = pr2.findType(pt1.fullyQualifiedName());
            assertNotSame(pt1, pt2);
            assertEquals(pt1.fullyQualifiedName(), pt2.fullyQualifiedName());
            assertNotSame(pt1.compilationUnit(), pt2.compilationUnit());
            assertSame(pt1.compilationUnit().sourceSet(), pt2.compilationUnit().sourceSet());
        }
    }

    @Test
    public void test3() throws IOException {
        Map<String, String> sourcesByFqn = Map.of("a.b.ISource", ISOURCE, "a.b.Source", SOURCE,
                PROCESSOR_FQN, PROCESSOR);
        ParseResult pr1 = init(sourcesByFqn);
        assertEquals(3, pr1.primaryTypes().size());
        Map<String, String> sourcesByURIString = sourcesByURIString(sourcesByFqn);

        // unchanged: Processor
        JavaInspector.ParseOptions po2 = new JavaInspector.ParseOptions.Builder()
                .setInvalidated(t -> PROCESSOR_FQN.equals(t.fullyQualifiedName()) ? UNCHANGED : INVALID)
                .build();
        ParseResult pr2 = javaInspector.parse(sourcesByURIString, po2).parseResult();
        assertEquals(3, pr2.primaryTypes().size());
        for (TypeInfo pt1 : pr1.primaryTypes()) {
            TypeInfo pt2 = pr2.findType(pt1.fullyQualifiedName());
            if (PROCESSOR_FQN.equals(pt1.fullyQualifiedName())) {
                assertSame(pt1, pt2);
            } else {
                assertNotSame(pt1, pt2);
                assertEquals(pt1.fullyQualifiedName(), pt2.fullyQualifiedName());
                assertNotSame(pt1.compilationUnit(), pt2.compilationUnit());
                assertSame(pt1.compilationUnit().sourceSet(), pt2.compilationUnit().sourceSet());
            }
        }
    }


    @Test
    public void test4() throws IOException {
        Map<String, String> sourcesByFqn = Map.of("a.b.ISource", ISOURCE, "a.b.Source", SOURCE,
                PROCESSOR_FQN, PROCESSOR);
        ParseResult pr1 = init(sourcesByFqn);
        assertEquals(3, pr1.primaryTypes().size());
        TypeInfo iSource1 = pr1.findType(ISOURCE_FQN);
        Map<String, String> sourcesByURIString = sourcesByURIString(sourcesByFqn);

        // unchanged: Processor
        JavaInspector.ParseOptions po2 = new JavaInspector.ParseOptions.Builder()
                .setInvalidated(t -> switch (t.simpleName()) {
                    case "Processor" -> UNCHANGED;
                    case "ISource" -> INVALID;
                    case "Source" -> REWIRE;
                    default -> throw new UnsupportedOperationException();
                })
                .build();
        ParseResult pr2 = javaInspector.parse(sourcesByURIString, po2).parseResult();
        assertEquals(3, pr2.primaryTypes().size());

        // What REWIRE is for: "the type isn't changed at all, but it accesses invalidated (and hence re-parsed, NEW)
        // type info objects". Source implements ISource, which was re-parsed, so the rewired Source must implement
        // the NEW ISource. It used to implement the stale one: the InfoMap was built from the REWIRE set alone, so
        // ISource was not a key and typeInfo() handed back the argument unchanged.
        TypeInfo iSource2 = pr2.findType(ISOURCE_FQN);
        TypeInfo source2 = pr2.findType("a.b.Source");
        assertNotSame(iSource1, iSource2, "ISource is INVALID: it must have been re-parsed");
        TypeInfo implementedByRewired = source2.interfacesImplemented().getFirst().typeInfo();
        assertSame(iSource2, implementedByRewired,
                "the rewired Source must implement the re-parsed ISource, not the stale one");
        assertNotSame(iSource1, implementedByRewired);
        for (TypeInfo pt1 : pr1.primaryTypes()) {
            TypeInfo pt2 = pr2.findType(pt1.fullyQualifiedName());
            if (PROCESSOR_FQN.equals(pt1.fullyQualifiedName())) {
                assertSame(pt1, pt2);
            } else {
                assertNotSame(pt1, pt2);
                assertEquals(pt1.fullyQualifiedName(), pt2.fullyQualifiedName());
                assertEquals(pt1.compilationUnit(), pt2.compilationUnit());
                if ("ISource".equals(pt1.simpleName())) {
                    assertNotSame(pt1.compilationUnit(), pt2.compilationUnit());
                } else {
                    assertSame(pt1.compilationUnit(), pt2.compilationUnit());
                }
                assertSame(pt1.compilationUnit().sourceSet(), pt2.compilationUnit().sourceSet());
            }
        }
    }
}
