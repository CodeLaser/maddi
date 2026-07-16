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

package org.e2immu.analyzer.modification.prepwork;

import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.api.parser.Summary;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.e2immu.language.inspection.integration.JavaInspectorImpl.JAR_WITH_PATH_PREFIX;
import static org.e2immu.language.inspection.integration.JavaInspectorImpl.TEST_PROTOCOL_PREFIX;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@code IMPLEMENTATIONS} is the one analysis value that points <em>against</em> the direction of rewiring:
 * {@code MethodAnalyzer.addImplementation} writes it onto the <em>overridden</em> (abstract) method, which lives in a
 * supertype — i.e. <em>upstream</em> of the implementation. Rewiring only ever replaces a type and what is downstream
 * of it, so the upstream interface is never rewired, and its {@code IMPLEMENTATIONS} keeps referring to the
 * implementation objects that were replaced.
 * <p>
 * Here {@code a.b.Y} implements the abstract {@code a.b.X.getEmail()}. Rewiring Y leaves X alone (correctly: X does not
 * reach Y), so X.getEmail()'s IMPLEMENTATIONS must be re-pointed at the new Y.getEmail() when prep re-runs over the
 * rewired Y.
 */
public class TestImplementationsAfterRewire extends CommonTest {

    @BeforeEach
    @Override
    public void beforeEach() {
        // this test builds its own inspector: the in-house parser needs every testprotocol: source registered
    }

    @Language("java")
    private static final String X = """
            package a.b;
            import java.util.Map;
            abstract class X {
                protected final Map<String, Object> attributes;
                X(Map<String,Object> attributes) {
                    this.attributes = attributes;
                }
                public abstract String getEmail();
            }
            """;

    @Language("java")
    private static final String Y = """
            package a.b;
            import java.util.Map;
            public class Y extends X {
                public Y(Map<String,Object> attributes) { super(attributes); }
                @Override
                public String getEmail() {
                    return (String) attributes.get("email");
                }
            }
            """;

    @DisplayName("IMPLEMENTATIONS on an upstream abstract method must follow a rewire of the implementing type")
    @Test
    public void test() throws IOException {
        ParseResult parseResult = init(Map.of(ABX, X, "a.b.Y", Y));

        TypeInfo x = parseResult.findType(ABX);
        TypeInfo y = parseResult.findType("a.b.Y");
        MethodInfo xGetEmail = x.findUniqueMethod("getEmail", 0);
        MethodInfo yGetEmail = y.findUniqueMethod("getEmail", 0);
        assertTrue(xGetEmail.isAbstract());

        new PrepAnalyzer(runtime).doPrimaryTypes(Set.of(x, y));
        assertEquals(List.of(yGetEmail), implementationsOf(xGetEmail),
                "prep must record Y.getEmail() as the implementation of the abstract X.getEmail()");
        assertSame(yGetEmail, implementationsOf(xGetEmail).getFirst());

        // rewire Y alone: X is upstream of Y, so a real reload would leave X UNCHANGED, exactly as here
        InfoMap infoMap = runtime.newInfoMap(Set.of(y));
        Set<TypeInfo> rewired = infoMap.rewireAll();
        assertEquals(1, rewired.size());
        TypeInfo y2 = rewired.iterator().next();
        assertNotSame(y, y2);
        MethodInfo y2GetEmail = y2.findUniqueMethod("getEmail", 0);
        assertNotSame(yGetEmail, y2GetEmail, "the rewired type must have new MethodInfo objects");
        assertEquals(yGetEmail, y2GetEmail, "...that are structurally equal to the ones they replace");
        assertSame(xGetEmail, y2GetEmail.overrides().iterator().next(),
                "the rewired Y still overrides the very same, upstream X.getEmail()");

        // prep re-runs over the rewired type, as it must: a rewire drops all analysis
        new PrepAnalyzer(runtime).doPrimaryType(y2);

        List<MethodInfo> after = implementationsOf(xGetEmail);
        assertEquals(1, after.size(), "X.getEmail() has exactly one implementation, before and after: " + after);
        assertSame(y2GetEmail, after.getFirst(),
                "IMPLEMENTATIONS must point at the rewired Y.getEmail(), not the object it replaced");
    }

    private static List<MethodInfo> implementationsOf(MethodInfo methodInfo) {
        Value.SetOfMethodInfo set = methodInfo.analysis()
                .getOrDefault(PropertyImpl.IMPLEMENTATIONS, ValueImpl.SetOfMethodInfoImpl.EMPTY);
        return StreamSupport.stream(set.methodInfoSet().spliterator(), false).toList();
    }

    private ParseResult init(Map<String, String> sourcesByFqn) throws IOException {
        Map<String, String> sourcesByURIString = sourcesByFqn.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(e -> TEST_PROTOCOL_PREFIX + e.getKey(), Map.Entry::getValue));
        javaInspector = new JavaInspectorImpl();
        InputConfigurationImpl.Builder builder = new InputConfigurationImpl.Builder()
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .addClassPath(JavaInspectorImpl.E2IMMU_SUPPORT)
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/junit/jupiter/api")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/apiguardian/api")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/junit/platform/commons")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/opentest4j");
        sourcesByURIString.keySet().forEach(builder::addSources);
        InputConfiguration inputConfiguration = builder.build();
        javaInspector.initialize(inputConfiguration);
        runtime = javaInspector.runtime();
        JavaInspector.ParseOptions parseOptions = new JavaInspector.ParseOptions.Builder()
                .setFailFast(true).setDetailedSources(true).setIgnoreModule(true).build();
        Summary summary = javaInspector.parse(sourcesByURIString, parseOptions);
        return summary.parseResult();
    }
}
