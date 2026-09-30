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

package io.codelaser.maddi.modification.analyzer.modification;

import io.codelaser.maddi.cst.api.analysis.Value;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;
import io.codelaser.maddi.modification.analyzer.CommonTest;
import io.codelaser.maddi.modification.analyzer.impl.IteratingAnalyzerImpl;
import io.codelaser.maddi.modification.link.impl.ReceiverLevel;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The receiver rules of {@link ReceiverLevel}, on the shape that capped vavr's persistent collections (VAVR.md, G1):
 * a stateful type shares the collection interface with the persistent ones, so the interface's abstract method is
 * modifying by the union over its implementations, and a persistent type calling that method on a field of the
 * interface type modified the field, and itself with it. The cone rule judges the call by the implementations
 * below the field's static type; the level rule (mirrored in the shadow pass) keeps the result under MODREACH.
 */
public class TestReceiverLevel extends CommonTest {

    @Language("java")
    private static final String G1 = """
            package a.b;
            class G1 {
                interface Value { boolean isEmpty(); }
                // stateful: consuming makes isEmpty() modifying, and so Value.isEmpty() by the union
                static class Iterator implements Value {
                    private int n;
                    @Override public boolean isEmpty() { n++; return n > 3; }
                }
                interface Seq extends Value { Seq tail(); }
                static class Nil implements Seq {
                    @Override public boolean isEmpty() { return true; }
                    @Override public Seq tail() { throw new UnsupportedOperationException(); }
                }
                static class Cons implements Seq {
                    private final Seq tail;
                    Cons(Seq tail) { this.tail = tail; }
                    @Override public boolean isEmpty() { return false; }
                    @Override public Seq tail() { return tail; }
                    // receiver of static type Seq: the cone is Nil, Cons; neither modifies
                    boolean tailIsEmpty() { return tail.isEmpty(); }
                    // receiver of static type Value: the cone is everything, Iterator included
                    static boolean anyEmpty(Value v) { return v.isEmpty(); }
                }
            }
            """;

    @AfterEach
    public void gateOff() {
        ReceiverLevel.setEnabled(false);
    }

    private void analyze(TypeInfo typeInfo, boolean modReach) {
        List<Info> ao = prepWork(typeInfo);
        new IteratingAnalyzerImpl(javaInspector, new IteratingAnalyzerImpl.ConfigurationBuilder()
                .setMaxIterations(20)
                .setModificationViaReachability(modReach)
                .build()).analyze(ao);
    }

    private static MethodInfo method(TypeInfo typeInfo, String name) {
        return typeInfo.methods().stream().filter(m -> m.name().equals(name)).findFirst().orElseThrow();
    }

    private static Value.Immutable immutable(TypeInfo typeInfo) {
        return typeInfo.analysis().getOrNull(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.class);
    }

    private static Value.Bool unmodified(FieldInfo fieldInfo) {
        return fieldInfo.analysis().getOrDefault(PropertyImpl.UNMODIFIED_FIELD, ValueImpl.BoolImpl.FALSE);
    }

    @DisplayName("gate off: the union caps the persistent type (the shape as it stands)")
    @Test
    public void gateOff_unionCaps() {
        ReceiverLevel.setEnabled(false);
        TypeInfo X = javaInspector.parse("a.b.G1", G1);
        analyze(X, true);
        TypeInfo cons = X.findSubType("Cons");
        assertFalse(method(X.findSubType("Value"), "isEmpty").isNonModifying(), "Iterator consumes: the union modifies");
        assertFalse(method(cons, "tailIsEmpty").isNonModifying(), "tail.isEmpty() reaches the union");
        assertTrue(unmodified(cons.getFieldByName("tail", true)).isFalse(), "the field is modified through the call");
        assertEquals(ValueImpl.ImmutableImpl.FINAL_FIELDS, immutable(cons));
    }

    @DisplayName("gate on: the cone rule judges tail.isEmpty() by Nil and Cons; the level rule holds under MODREACH")
    @Test
    public void gateOn_cone() {
        for (boolean modReach : new boolean[]{false, true}) {
            ReceiverLevel.setEnabled(true);
            String name = modReach ? "G1R" : "G1";
            TypeInfo X = javaInspector.parse("a.b." + name, G1.replace("class G1 {", "class " + name + " {"));
            analyze(X, modReach);
            String where = " (modReach=" + modReach + ")";
            TypeInfo cons = X.findSubType("Cons");
            assertFalse(method(X.findSubType("Value"), "isEmpty").isNonModifying(), "the union itself stands" + where);
            assertTrue(method(cons, "tailIsEmpty").isNonModifying(), "no modifying implementation below Seq" + where);
            assertTrue(unmodified(cons.getFieldByName("tail", true)).isTrue(), "the field is not modified" + where);
            // the modification side is lifted; the types stay at @FinalFields through the INDEPENDENCE cycle
            // (VAVR.md G2): Seq.tail() returns a Seq, Cons.tail() hands out the field, the field's independence
            // waits on Seq's immutability, and cycle breaking resolves the wait at FINAL_FIELDS. A separate lever.
            assertEquals(ValueImpl.ImmutableImpl.FINAL_FIELDS, immutable(cons), "Cons" + where);
            assertEquals(ValueImpl.ImmutableImpl.FINAL_FIELDS, immutable(X.findSubType("Seq")), "Seq" + where);
            // the cone of Value holds Iterator: the union stands where the receiver's static type allows it
            assertFalse(method(cons, "anyEmpty").parameters().getFirst().isUnmodified(), "v may be an Iterator" + where);
            assertEquals(ValueImpl.ImmutableImpl.MUTABLE, immutable(X.findSubType("Iterator")), where);
        }
    }
}
