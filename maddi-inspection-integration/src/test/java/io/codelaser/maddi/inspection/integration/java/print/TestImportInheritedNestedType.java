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

package io.codelaser.maddi.inspection.integration.java.print;

import io.codelaser.maddi.cst.api.info.ImportComputer;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
import io.codelaser.maddi.inspection.integration.java.CommonTest2;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A nested type declared by a SUPERTYPE is inherited into the subclass's scope, so it needs no import — and
 * a {@code protected} one cannot legally be imported from another package at all.
 * <p>
 * ⛔ FOUND ON OPENSEARCH, 2026-08-19, and it did not look like an import bug. Promoting the nested enum
 * {@code RestRequest.Method} to a top-level {@code RestMethod} rewrote 247 files correctly and then added
 * <b>209 imports nobody asked for</b>, every one a nested type of a supertype already in scope:
 * <pre>
 *   176 ×  org.opensearch.rest.RestHandler.Route                    (public — compiles, pure noise)
 *    24 ×  org.opensearch.rest.BaseRestHandler.RestChannelConsumer  (protected — DOES NOT COMPILE)
 * </pre>
 * {@code :server:compileJava} failed with 21 × "RestChannelConsumer has protected access in BaseRestHandler".
 * <p>
 * ⚠ THE PUBLIC 185 ARE THE DANGEROUS HALF. They compile, so a smaller move would have shipped them as diff
 * noise nobody questioned; only a move large enough to touch a protected one turned it into a build failure.
 * That is why {@link #publicInheritedNestedTypeIsNotImportedEither()} exists alongside the protected case:
 * testing only the compile error would leave the silent majority unguarded.
 */
public class TestImportInheritedNestedType extends CommonTest2 {

    @Language("java")
    private static final String BASE = """
            package a;
            public class Base {
                protected interface Handle { }
                public interface Route { }
                public static class Nested {
                    public interface Deep { }
                }
            }
            """;

    /** {@code Sub} is in ANOTHER package and inherits both member types: neither may be imported. */
    @Language("java")
    private static final String SUB = """
            package b;
            import a.Base;
            import java.util.List;
            public class Sub extends Base {
                Handle handle;
                Route route;
                List<String> list;
            }
            """;

    private String importsOf(ParseResult parseResult, String fqn) {
        TypeInfo typeInfo = parseResult.findType(fqn);
        ImportComputer importComputer = javaInspector.importComputer(4, typeInfo.compilationUnit().sourceSet());
        Qualification qualification = javaInspector.runtime().qualificationExistingSources();
        ImportComputer.Result r = importComputer.go(typeInfo.compilationUnit(), qualification);
        return r.imports().stream().map(ImportComputer.ImportDetails::importString)
                .sorted().collect(Collectors.joining(", "));
    }

    /**
     * ⛔ The load-bearing assertion: {@code a.Base.Handle} is protected, so importing it does not compile.
     * Before the fix the computer emitted it, because it derives an import for every simple name it sees
     * used without asking whether the name is already in scope through inheritance.
     */
    @Test
    public void protectedInheritedNestedTypeIsNotImported() throws IOException {
        ParseResult parseResult = init(Map.of("a.Base", BASE, "b.Sub", SUB));
        String imports = importsOf(parseResult, "b.Sub");
        assertEquals("a.Base, java.util.List", imports);
    }

    /** The same shape, public: legal but wrong, and 185 of the 209 OpenSearch cases were this one. */
    @Test
    public void publicInheritedNestedTypeIsNotImportedEither() throws IOException {
        ParseResult parseResult = init(Map.of("a.Base", BASE, "b.Sub", SUB));
        String imports = importsOf(parseResult, "b.Sub");
        assertEquals(-1, imports.indexOf("a.Base.Route"), "inherited public member type must not be imported");
    }

    /**
     * ⚠ THE CONTROL, and it is what stops the fix from over-reaching. A nested type reached through a type
     * that is NOT a supertype is still imported normally — suppressing that would break every ordinary
     * nested-type reference in the codebase, which is a far worse defect than the one being fixed.
     */
    @Language("java")
    private static final String OTHER = """
            package b;
            import a.Base;
            public class Other {
                Base.Nested nested;
            }
            """;

    @Test
    public void nestedTypeOfANonSupertypeIsStillImported() throws IOException {
        ParseResult parseResult = init(Map.of("a.Base", BASE, "b.Other", OTHER));
        String imports = importsOf(parseResult, "b.Other");
        assertEquals("a.Base", imports);
    }

    /**
     * ⚠ Java inherits a member type ONE level: {@code Sub extends Base} does not put {@code Base.Nested.Deep}
     * in scope, because {@code Nested} is not a supertype of {@code Sub}. The fix consults only the DECLARING
     * type for exactly this reason, and this pins that it does.
     */
    @Language("java")
    private static final String DEEP = """
            package b;
            import a.Base;
            public class Deeper extends Base {
                Base.Nested.Deep deep;
            }
            """;

    @Test
    public void aMemberOfAMemberIsNotInheritedIntoScope() throws IOException {
        ParseResult parseResult = init(Map.of("a.Base", BASE, "b.Deeper", DEEP));
        String imports = importsOf(parseResult, "b.Deeper");
        assertEquals(-1, imports.indexOf("a.Base.Nested.Deep"), "the printer renders this down its chain");
    }

    /**
     * ⛔ THE OTHER CONTROL, found the hard way on 2026-09-17: inheritance puts a member type in scope in the
     * class BODY, and the header is not the body. {@code implements Route} resolves only through the import,
     * so the import has to stay even though {@code Impl} inherits {@code Route} — dropping it is
     * "cannot find symbol" at the {@code implements} clause. The bound of a type parameter is header too.
     */
    @Language("java")
    private static final String IMPL = """
            package b;
            import a.Base;
            import a.Base.Route;
            public class Impl extends Base implements Route {
                Route other;
            }
            """;

    @Test
    public void aHeaderIsNotTheBody() throws IOException {
        ParseResult parseResult = init(Map.of("a.Base", BASE, "b.Impl", IMPL));
        String imports = importsOf(parseResult, "b.Impl");
        assertEquals("a.Base, a.Base.Route", imports);
    }

    @Language("java")
    private static final String BOUND = """
            package b;
            import a.Base;
            import a.Base.Route;
            public class Bound<R extends Route> extends Base {
                R route;
            }
            """;

    @Test
    public void aTypeParameterBoundIsHeaderToo() throws IOException {
        ParseResult parseResult = init(Map.of("a.Base", BASE, "b.Bound", BOUND));
        String imports = importsOf(parseResult, "b.Bound");
        assertEquals("a.Base, a.Base.Route", imports);
    }

    /**
     * ⛔ FOUND BY THE SPLIT-SEARCH ENGINE, 2026-09-25. A class that names its superclass fully qualified,
     * {@code extends a.Base}, imports nothing from {@code a}. The inherited member type was correctly left
     * unimported, but was then PRINTED down its declaring chain, {@code Base.Handle}, and {@code Base} does not
     * resolve in that file: javac says "package Base does not exist". Split-method's generator hit it writing a
     * helper parameter and a record component of the inherited type. Inside the body the bare name resolves,
     * so that is what must be printed.
     */
    @Language("java")
    private static final String SUB_WITHOUT_IMPORT = """
            package b;
            public class Sub extends a.Base {
                Handle handle;
            }
            """;

    @Test
    public void anInheritedMemberTypeIsPrintedByItsSimpleName() throws IOException {
        ParseResult parseResult = init(Map.of("a.Base", BASE, "b.Sub", SUB_WITHOUT_IMPORT));
        TypeInfo sub = parseResult.findType("b.Sub");
        ImportComputer importComputer = javaInspector.importComputer(4, sub.compilationUnit().sourceSet());
        ImportComputer.Result r = importComputer.go(sub.compilationUnit(),
                javaInspector.runtime().qualificationExistingSources());
        String printed = sub.getFieldByName("handle", true).type()
                .print(r.qualification(), false, javaInspector.runtime().diamondShowAll()).toString();
        assertEquals("Handle", printed, "how the field's type is printed in b.Sub, which imports nothing from a");
        // a nested printer -- the one that prints a record declared in b.Sub, say -- works on a level of its own
        String nested = sub.getFieldByName("handle", true).type()
                .print(new io.codelaser.maddi.cst.impl.output.QualificationImpl(r.qualification()), false,
                        javaInspector.runtime().diamondShowAll()).toString();
        assertEquals("Handle", nested, "how the field's type is printed one qualification level further in");
    }
}
