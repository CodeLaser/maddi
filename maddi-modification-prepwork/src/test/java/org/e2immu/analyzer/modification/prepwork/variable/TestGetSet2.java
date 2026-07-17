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

package org.e2immu.analyzer.modification.prepwork.variable;

import org.e2immu.analyzer.modification.common.getset.ApplyGetSetTranslation;
import org.e2immu.analyzer.modification.prepwork.CommonTest;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestGetSet2 extends CommonTest {
    @Language("java")
    public static final String INPUT1 = """
            import java.util.List;
            import java.util.ArrayList;
            class X {
                private final List<Integer> myList = new ArrayList<>();
            
                public Integer get(int i) {
                    return myList.get(i);
                }
            
                public Integer getMyList(int i) {
                    return myList.get(i);
                }
            
                public List<Integer> getMyList() {
                    return myList;
                }
            
                public void set(int i, int k) {
                    myList.set(i, k);
                }
            
                // not a setter!
                public void add(int k) {
                    myList.add(k);
                }
            
                // should always return true
                public boolean method(int pos) {
                    return myList.get(pos).equals(get(pos));
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse("X", INPUT1);
        new PrepAnalyzer(runtime).doPrimaryType(X);
        MethodInfo method = X.findUniqueMethod("method", 1);

        MethodInfo get = X.findUniqueMethod("get", 1);
        assertEquals("GetSetValueImpl[field=X.myList, setter=false, parameterIndexOfIndex=0, list=true]",
                get.getSetField().toString());

        Expression getE = get.methodBody().lastStatement().expression();
        if (getE instanceof MethodCall mc) {
            // the inner call is java.util.List.get(int); it now carries the restored synthetic-list GetSet field
            // (was field=null before the getset-list-element-getter-npe.md fix, which broke getSetVariable)
            assertEquals("GetSetValueImpl[field=java.util.List._synthetic_list, setter=false,"
                         + " parameterIndexOfIndex=0, list=true]",
                    mc.methodInfo().getSetField().toString());
        } else fail();

        MethodInfo getMyList = X.findUniqueMethod("getMyList", 1);
        assertEquals("GetSetValueImpl[field=X.myList, setter=false, parameterIndexOfIndex=0, list=true]",
                getMyList.getSetField().toString());

        MethodInfo getMyList0 = X.findUniqueMethod("getMyList", 0);
        assertEquals("GetSetValueImpl[field=X.myList, setter=false, parameterIndexOfIndex=-1, list=false]",
                getMyList0.getSetField().toString());

        MethodInfo add = X.findUniqueMethod("add", 1);
        assertEquals(ValueImpl.GetSetValueImpl.EMPTY, add.getSetField());

        Expression expression = method.methodBody().lastStatement().expression();
        if (expression instanceof MethodCall mc) {
            Expression get1 = mc.object();
            assertEquals("this.myList.get(pos)", get1.toString());
            assertEquals("myList.get(pos)",
                    get1.print(runtime.qualificationQualifyFromPrimaryType()).toString());
            assertEquals("get(pos)", mc.parameterExpressions().getFirst().toString());
        } else fail();
    }

    /**
     * Regression for notes/getset-list-element-getter-npe.md: resolving a call to a user's indexed list-getter
     * (X.get(int), whose field is a List&lt;Integer&gt;) to the variable it denotes needs java.util.List's own
     * {@code _synthetic_list} element field. That field is created by CreateSyntheticFieldsForGetSet during
     * inspection; a refactor (a54eb89c) had dropped the hard-coded java.util.List watcher, so List loaded from
     * bytecode/openjdk (which carries no physical @GetSet) no longer got it, and getSetVariable NPE'd at
     * RuntimeImpl:359 ("Called on wrong method"). This exercises the full getterVariable path that stdbase hits.
     */
    @Test
    public void test1getterVariable() {
        TypeInfo X = javaInspector.parse("X", INPUT1);
        new PrepAnalyzer(runtime).doPrimaryType(X);

        // the fix itself: java.util.List.get(int) must carry a non-null GetSet field '_synthetic_list'
        FieldInfo myList = X.getFieldByName("myList", true);
        TypeInfo list = myList.type().typeInfo();
        assertEquals("java.util.List", list.fullyQualifiedName());
        MethodInfo listGet = list.findUniqueMethod("get", 1);
        Value.FieldValue listGetField = listGet.getSetField();
        assertNotNull(listGetField.field(), "java.util.List.get must have a synthetic GetSet field");
        assertEquals("_synthetic_list", listGetField.field().name());
        assertTrue(listGetField.list());

        // the reported crash: getterVariable on the call to the user's getter get(pos)
        MethodInfo method = X.findUniqueMethod("method", 1);
        Expression expression = method.methodBody().lastStatement().expression();
        MethodCall equalsCall = (MethodCall) expression;               // myList.get(pos).equals(get(pos))
        MethodCall userGetCall = (MethodCall) equalsCall.parameterExpressions().getFirst();  // get(pos)
        assertEquals("get(pos)", userGetCall.toString());

        Variable resolved = runtime.getterVariable(userGetCall);       // used to NPE at RuntimeImpl:359
        assertNotNull(resolved);
        assertInstanceOf(DependentVariable.class, resolved);
        assertTrue(resolved.toString().contains("_synthetic_list"),
                "expected the resolved element variable to reference the synthetic list field, got: " + resolved);
    }

    @Language("java")
    public static final String INPUT2 = """
            import java.util.ArrayList;import java.util.List;
            record X(List<String> list, int k) {
                static X make() {
                    X x = new X(new ArrayList<>(), 3);
                    x.list().add("x".repeat(x.k()));
                    return x;
                }
            }
            """;

    @DisplayName("getters in record")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse("X", INPUT2);
        new PrepAnalyzer(runtime).doPrimaryType(X);
        assertTrue(X.typeNature().isRecord());
        MethodInfo k = X.findUniqueMethod("k", 0);
        Value.FieldValue fieldValueK = k.getSetField();
        assertFalse(fieldValueK.setter());

        MethodInfo method = X.findUniqueMethod("make", 0);
        Expression e1 = method.methodBody().statements().get(1).expression();
        assertEquals("x.list.add(\"x\".repeat(x.k))",
                e1.translate(new ApplyGetSetTranslation(runtime)).toString());
    }
}