package org.e2immu.language.java.openjdk.expression;

import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestThisSuper extends CommonTest {
    @Language("java")
    public static final String INPUT1 = """
            package a.b;
            import java.util.AbstractList;
            import java.util.List;
            class C<T> {
                List<T> outer;
                List<T> get() {
                    return this.outer;
                }
                abstract class TestList extends AbstractList<T> {
                    @Override public int size() {
                        return C.this.outer.size();
                    }
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo C = scan("a.b.C", INPUT1);
        TypeInfo TestList = C.findSubType("TestList");

        MethodInfo get = C.findUniqueMethod("get", 0);
        VariableExpression ve = (VariableExpression) get.methodBody().statements().getFirst().expression();
        if (ve.variable() instanceof FieldReference fr && fr.scopeVariable() instanceof This thisVar) {
            assertNull(thisVar.explicitlyWriteType());
        }

        MethodInfo size = TestList.findUniqueMethod("size", 0);
        MethodCall mc = (MethodCall) size.methodBody().statements().getFirst().expression();
        if (mc.object() instanceof VariableExpression ve2 && ve2.variable() instanceof FieldReference fr) {
            assertEquals("outer", fr.fieldInfo().name());
            if (fr.scopeVariable() instanceof This thisVar) {
                assertFalse(thisVar.writeSuper());
                assertEquals("a.b.C", thisVar.explicitlyWriteType().fullyQualifiedName());
            }
        } else fail();
        assertEquals("this.outer", mc.object().toString());
    }
}
