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

package org.e2immu.analyzer.modification.common.defaults;

import org.e2immu.analyzer.modification.common.CommonTest;
import org.e2immu.language.cst.api.analysis.Property;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stage 1 of eventual immutability (road to immutability §060): the {@code @Mark}/{@code @Only}/{@code @TestMark}
 * family and the {@code after="…"} parameters are read from source into properties. Nothing computes them yet;
 * these are contracts.
 */
public class TestEventualContracts {

    private JavaInspector javaInspector;
    private ContractReader contractReader;

    @BeforeEach
    public void beforeEach() throws IOException {
        javaInspector = CommonTest.javaInspectorFactory().withSources(SourceSetImpl.testProtocolSourceSet());
        contractReader = new ContractReader(javaInspector.runtime());
    }

    private TypeInfo parse(String fqn, String source) {
        return javaInspector.parse(Map.of(fqn, source), new JavaInspector.ParseOptions.Builder()
                        .setFailFast(true)
                        .setIgnoreModule(true)
                        .build())
                .parseResult().findType(fqn);
    }

    private Value.Eventual eventual(MethodInfo methodInfo) {
        return (Value.Eventual) contractReader.contracts(methodInfo).get(PropertyImpl.EVENTUAL_METHOD);
    }

    // the boolean-flag pattern of the book's SimpleImmutableSet1, plus the @Final(after=) on the flag itself
    private static final String FREEZE = """
            package a;
            import org.e2immu.annotation.*;
            import org.e2immu.annotation.eventual.*;
            @ImmutableContainer(after = "frozen", hc = true)
            public abstract class X {
                @Final(after = "frozen")
                private boolean frozen;
                @Mark("frozen")
                public void freeze() { frozen = true; }
                @TestMark("frozen")
                public boolean isFrozen() { return frozen; }
                @TestMark(value = "frozen", before = true)
                public boolean isNotFrozen() { return !frozen; }
                @Only(before = "frozen")
                public void ensureNotFrozen() { if (frozen) throw new IllegalStateException(); }
                @Only(after = "frozen")
                public void ensureFrozen() { if (!frozen) throw new IllegalStateException(); }
                public int size() { return 0; }
            }
            """;

    @Test
    public void testType() {
        TypeInfo X = parse("a.X", FREEZE);
        Map<Property, Value> contracts = contractReader.contracts(X);

        Value.EventuallyImmutable ev = (Value.EventuallyImmutable) contracts
                .get(PropertyImpl.EVENTUALLY_IMMUTABLE_TYPE);
        assertNotNull(ev);
        assertTrue(ev.isEventual());
        assertEquals("frozen", ev.markLabel());
        assertTrue(ev.immutableAfterMark().isImmutableHC());
        assertEquals("@Immutable(hc=true)(after=\"frozen\")", ev.toString());

        // stage 1 is purely additive: IMMUTABLE_TYPE and CONTAINER_TYPE keep saying exactly what they said before
        assertSame(ValueImpl.ImmutableImpl.IMMUTABLE_HC, contracts.get(PropertyImpl.IMMUTABLE_TYPE));
        assertSame(ValueImpl.BoolImpl.TRUE, contracts.get(PropertyImpl.CONTAINER_TYPE));
    }

    @Test
    public void testMarkOnlyTestMark() {
        TypeInfo X = parse("a.X", FREEZE);

        Value.Eventual freeze = eventual(X.findUniqueMethod("freeze", 0));
        assertTrue(freeze.isMark());
        assertFalse(freeze.isOnly());
        assertFalse(freeze.isTestMark());
        assertEquals(Set.of("frozen"), freeze.fields());
        assertEquals("@Mark(\"frozen\")", freeze.toString());

        Value.Eventual isFrozen = eventual(X.findUniqueMethod("isFrozen", 0));
        assertTrue(isFrozen.isTestMark());
        assertEquals(Boolean.TRUE, isFrozen.test());
        assertEquals("@TestMark(\"frozen\")", isFrozen.toString());

        // before=true inverts the sense: the method returns true in the 'before' state
        Value.Eventual isNotFrozen = eventual(X.findUniqueMethod("isNotFrozen", 0));
        assertEquals(Boolean.FALSE, isNotFrozen.test());
        assertEquals("@TestMark(\"frozen\", before=true)", isNotFrozen.toString());

        Value.Eventual ensureNotFrozen = eventual(X.findUniqueMethod("ensureNotFrozen", 0));
        assertTrue(ensureNotFrozen.isOnly());
        assertEquals(Boolean.FALSE, ensureNotFrozen.after());
        assertEquals("@Only(before=\"frozen\")", ensureNotFrozen.toString());

        Value.Eventual ensureFrozen = eventual(X.findUniqueMethod("ensureFrozen", 0));
        assertTrue(ensureFrozen.isOnly());
        assertEquals(Boolean.TRUE, ensureFrozen.after());
        assertEquals("@Only(after=\"frozen\")", ensureFrozen.toString());

        // a method that can be called in either state carries no eventual contract at all
        assertNull(eventual(X.findUniqueMethod("size", 0)));

        // all four belong to the same state transition
        assertTrue(freeze.consistentWith(ensureFrozen));
        assertTrue(isFrozen.consistentWith(isNotFrozen));
    }

    @Test
    public void testEventuallyFinalField() {
        TypeInfo X = parse("a.X", FREEZE);
        FieldInfo frozen = X.getFieldByName("frozen", true);
        Value.SetOfStrings marks = (Value.SetOfStrings) contractReader.contracts(frozen)
                .get(PropertyImpl.EVENTUALLY_FINAL_FIELD);
        assertNotNull(marks);
        assertEquals(Set.of("frozen"), marks.set());
    }

    // several fields carrying one transition: the label is a comma-separated list, order-insensitive
    @Test
    public void testMultipleFields() {
        TypeInfo X = parse("a.X", """
                package a;
                import org.e2immu.annotation.*;
                import org.e2immu.annotation.eventual.*;
                @FinalFields(after = "b, a")
                public abstract class X {
                    @Mark("a,b")
                    public abstract void mark();
                }
                """);
        Value.EventuallyImmutable ev = (Value.EventuallyImmutable) contractReader.contracts(X)
                .get(PropertyImpl.EVENTUALLY_IMMUTABLE_TYPE);
        assertTrue(ev.immutableAfterMark().isFinalFields());

        Value.Eventual mark = eventual(X.findUniqueMethod("mark", 0));
        assertEquals(Set.of("a", "b"), mark.fields());
        assertEquals("a,b", mark.markLabel());
    }

    // a plain @Immutable, without after=, must not acquire an eventual contract
    @Test
    public void testNotEventual() {
        TypeInfo X = parse("a.X", """
                package a;
                import org.e2immu.annotation.*;
                @ImmutableContainer
                public class X {
                    public final int i = 3;
                }
                """);
        Map<Property, Value> contracts = contractReader.contracts(X);
        assertFalse(contracts.containsKey(PropertyImpl.EVENTUALLY_IMMUTABLE_TYPE));
        assertSame(ValueImpl.ImmutableImpl.IMMUTABLE, contracts.get(PropertyImpl.IMMUTABLE_TYPE));
    }

    /**
     * The support classes are the leaves of the whole design: everything else in maddi becomes eventually
     * immutable by holding one of them. They arrive from the class path, so this also covers reading the
     * annotations back out of byte code rather than out of source.
     */
    @Test
    public void testSupportClasses() {
        TypeInfo X = parse("a.X", """
                package a;
                import org.e2immu.support.*;
                public class X {
                    private final SetOnce<String> setOnce = new SetOnce<>();
                    private final EventuallyFinalOnDemand<String> onDemand = new EventuallyFinalOnDemand<>();
                }
                """);
        TypeInfo setOnce = X.getFieldByName("setOnce", true).type().typeInfo();
        Value.EventuallyImmutable ev = (Value.EventuallyImmutable) contractReader.contracts(setOnce)
                .get(PropertyImpl.EVENTUALLY_IMMUTABLE_TYPE);
        assertNotNull(ev, "SetOnce must be recognized as eventually immutable");
        assertEquals("t", ev.markLabel());

        // TypeInfoImpl.inspection is an EventuallyFinalOnDemand: the type maddi's own eventuality rests on
        TypeInfo onDemand = X.getFieldByName("onDemand", true).type().typeInfo();
        Value.EventuallyImmutable ev2 = (Value.EventuallyImmutable) contractReader.contracts(onDemand)
                .get(PropertyImpl.EVENTUALLY_IMMUTABLE_TYPE);
        assertNotNull(ev2, "EventuallyFinalOnDemand backs TypeInfoImpl.inspection; it must be eventual");
        assertEquals("isFinal", ev2.markLabel());
        assertTrue(eventual(onDemand.findUniqueMethod("setFinal", 1)).isMark());
        assertTrue(eventual(onDemand.findUniqueMethod("setVariable", 1)).isOnly());
        assertEquals(Boolean.TRUE, eventual(onDemand.findUniqueMethod("isFinal", 0)).test());
        assertEquals(Boolean.FALSE, eventual(onDemand.findUniqueMethod("isVariable", 0)).test());
    }
}
