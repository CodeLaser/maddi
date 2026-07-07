package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.impl.translate.VariableTranslationAllowHierarchy;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.api.variable.Variable;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Unit test for {@link VariableTranslationAllowHierarchy}: a {@code TranslationMap} that, on a miss for a field
 * reference, retries with the same-named field re-homed onto each supertype of the field's owner. This is how a
 * mapping registered for a field declared in a super-type (e.g. an interface getter turned into a synthetic field)
 * is found for a field reference whose field sits on the implementing/overriding type.
 */
public class TestVariableTranslationAllowHierarchy extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class X {
                static class P { public Object f; }
                static class C extends P { public Object f; } // shadows P.f: both declare 'f'
            }
            """;

    private record Fixture(FieldInfo fOnP, FieldInfo fOnC, VariableExpression scope, LocalVariable target) {
    }

    private Fixture fixture() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT);
        TypeInfo p = X.findSubType("P");
        TypeInfo c = X.findSubType("C");
        FieldInfo fOnP = p.getFieldByName("f", true);
        FieldInfo fOnC = c.getFieldByName("f", true);
        assertSame(p, fOnP.owner());
        assertSame(c, fOnC.owner());
        LocalVariable v = runtime.newLocalVariable("v", c.asSimpleParameterizedType());
        LocalVariable target = runtime.newLocalVariable("target", runtime.objectParameterizedType());
        return new Fixture(fOnP, fOnC, runtime.newVariableExpression(v), target);
    }

    @DisplayName("direct hit: an exact mapping is returned")
    @Test
    public void directHit() {
        Fixture f = fixture();
        VariableTranslationAllowHierarchy tm = new VariableTranslationAllowHierarchy(runtime);
        FieldReference frPf = runtime.newFieldReference(f.fOnP(), f.scope(), f.fOnP().type());
        tm.put(frPf, f.target());
        assertSame(f.target(), tm.translateVariable(frPf));
    }

    @DisplayName("hierarchy: a mapping for the super-type field is found for the subtype's field reference")
    @Test
    public void viaSuperType() {
        Fixture f = fixture();
        VariableTranslationAllowHierarchy tm = new VariableTranslationAllowHierarchy(runtime);
        // map registered for P.f (the super-type's field)
        FieldReference frPf = runtime.newFieldReference(f.fOnP(), f.scope(), f.fOnP().type());
        tm.put(frPf, f.target());
        // looked up with C.f (the subtype's shadowing field) -> resolved via C's supertype P
        FieldReference frCf = runtime.newFieldReference(f.fOnC(), f.scope(), f.fOnC().type());
        assertEquals(f.target(), tm.translateVariable(frCf));
    }

    @DisplayName("no change: an unmapped variable (not resolvable via a supertype) is returned unchanged")
    @Test
    public void noChange() {
        Fixture f = fixture();
        VariableTranslationAllowHierarchy tm = new VariableTranslationAllowHierarchy(runtime);
        // empty map: a plain local variable is returned as-is
        Variable v = ((VariableExpression) f.scope()).variable();
        assertSame(v, tm.translateVariable(v));
        // a field reference with no matching mapping (even via supertypes) is returned as-is
        FieldReference frCf = runtime.newFieldReference(f.fOnC(), f.scope(), f.fOnC().type());
        assertSame(frCf, tm.translateVariable(frCf));
    }

    @DisplayName("translateVariableRecursively delegates and resolves via the hierarchy too")
    @Test
    public void recursive() {
        Fixture f = fixture();
        VariableTranslationAllowHierarchy tm = new VariableTranslationAllowHierarchy(runtime);
        FieldReference frPf = runtime.newFieldReference(f.fOnP(), f.scope(), f.fOnP().type());
        tm.put(frPf, f.target());
        FieldReference frCf = runtime.newFieldReference(f.fOnC(), f.scope(), f.fOnC().type());
        assertEquals(f.target(), tm.translateVariableRecursively(frCf));
    }
}
