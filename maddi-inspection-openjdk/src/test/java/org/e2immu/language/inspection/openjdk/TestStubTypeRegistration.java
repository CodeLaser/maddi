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

package org.e2immu.language.inspection.openjdk;

import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.impl.runtime.RuntimeImpl;
import org.e2immu.language.inspection.resource.InfoByFqn;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * ⛔ <b>A STUB TYPE HAS NO SOURCE SET, AND THE REGISTRY ASSERTED ON IT.</b> When a class file names a type
 * that is not on the class path, {@code ClassSymbolScanner} mints a stub — {@code newCompilationUnitStub},
 * whose compilation unit carries <b>no source set</b> (GAP #163: null is the designed representation of "no
 * input set", not a sentinel). {@link InfoByFqn#put} knows this; its own comment says <i>"the sourceSet can
 * be null, when the type was not properly loaded"</i>. One line further down it dereferenced that null.
 * <p>
 * The overwrite branch even LOGGED the state before failing on it:
 * <pre>
 * INFO  InfoByFqn -- Overwriting type unnamed package.XmlAdapter, null -&gt; null
 * java.lang.NullPointerException: ... CompilationUnit.sourceSet() is null
 *     at InfoByFqn.put(InfoByFqn.java:159)
 *     at ClassSymbolScanner.lazilyLoadPrimaryTypeFromClassFile(ClassSymbolScanner.java:351)
 * </pre>
 * <p>
 * ⚠ <b>It only fires with assertions enabled</b>, which is every Gradle test JVM and no production run —
 * so the corpus that found it is a test corpus. Parsing timefold-solver (65 source sets, several of which
 * reference {@code jakarta.xml.bind} and {@code org.openrewrite} types that are not on the class path) came
 * back with <b>133 dropped compilation units</b>, none of them the units that name the missing types.
 */
public class TestStubTypeRegistration {

    private static SourceSet sourceSet(String name) {
        return new SourceSetImpl.Builder().setName(name).setUri(URI.create("file:/" + name)).build();
    }

    /**
     * Two stubs for one name, which is what two class-file scans of the same missing type produce. Neither
     * carries a source set, so neither is "in" the set being scanned: the second overwrites the first.
     */
    @DisplayName("registering a second stub for the same name overwrites rather than throwing")
    @Test
    public void twoStubsForOneMissingType() {
        Runtime runtime = new RuntimeImpl();
        SourceSet main = sourceSet("main");
        InfoByFqn registry = new InfoByFqn();

        TypeInfo first = stub(runtime, "XmlAdapter");
        registry.put(first.fullyQualifiedName(), first, main);
        TypeInfo second = stub(runtime, "XmlAdapter");
        registry.put(second.fullyQualifiedName(), second, main);

        // what getType answers is the registry's business -- that the second put does not THROW is this test's
        assertNotNull(registry.getType(second.fullyQualifiedName(), main));
    }

    /**
     * The other half of the same null: a stub meeting a properly loaded type of that name. The real type
     * belongs to the set being scanned, the stub belongs to nothing, so they are not one type registered twice.
     */
    @DisplayName("a stub and a real type of the same name are not a duplicate registration")
    @Test
    public void aStubMeetingARealType() {
        Runtime runtime = new RuntimeImpl();
        SourceSet main = sourceSet("main");
        InfoByFqn registry = new InfoByFqn();

        TypeInfo stub = stub(runtime, "Widget");
        registry.put(stub.fullyQualifiedName(), stub, main);

        CompilationUnit cu = runtime.newCompilationUnitBuilder().setPackageName("").setSourceSet(main)
                .setURI(URI.create("file:/main/Widget.java")).build();
        TypeInfo real = runtime.newTypeInfo(cu, "Widget");
        registry.put(real.fullyQualifiedName(), real, main);

        assertNotNull(registry.getType(real.fullyQualifiedName(), main));
        assertNull(registry.getType("NotRegistered", main));
    }

    private static TypeInfo stub(Runtime runtime, String simpleName) {
        return runtime.newTypeInfo(runtime.newCompilationUnitStub(""), simpleName);
    }
}
