package io.codelaser.maddi.java.openjdk.other;

import static org.junit.jupiter.api.Assertions.*;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.codelaser.maddi.cst.api.expression.Assignment;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.ExpressionAsStatement;
import io.codelaser.maddi.cst.api.statement.LocalVariableCreation;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.java.openjdk.CommonTest;

/**
 * CodeLaser/maddi#142: an inner class of a generic class, used through an instantiation of the outer class, carries
 * the outer type's arguments, first in {@code parameters()}, as the written form {@code Store<String>.Entry} already
 * did. javac has them only on the enclosing type of its ClassType, so every RESOLVED use (a call's result, a field
 * read through the outer instance, a {@code var}) was a bare {@code Holder.Store.Entry}. Inside the outer class
 * itself the type stays as the source writes it, {@code Entry}.
 */
public class TestInnerClassOfGenericOuter extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            public class Holder {
                static final class Store<T> {
                    final class Entry { T value; }
                    Entry add(T value) { return new Entry(); }
                    Entry first;
                }
                private Store<String>.Entry cached;
                void keep(Store<String> store) {
                    cached = store.add("x");
                    var e = store.add("y");
                    var f = store.first;
                }
            }
            """;

    @DisplayName("resolved uses through Store<String> carry String, as the written one does")
    @Test
    public void outside() {
        TypeInfo holder = scan("a.b.Holder", INPUT);
        assertOuter("[Type String]", holder.getFieldByName("cached", true).type());
        MethodInfo keep = holder.findUniqueMethod("keep", 1);
        Assignment a = (Assignment) ((ExpressionAsStatement) keep.methodBody().statements().getFirst()).expression();
        assertOuter("[Type String]", a.value().parameterizedType());
        assertOuter("[Type String]", local(keep, 1));
        assertOuter("[Type String]", local(keep, 2));
    }

    @DisplayName("inside the outer class, Entry stays Entry")
    @Test
    public void inside() {
        TypeInfo store = scan("a.b.Holder", INPUT).findSubType("Store");
        assertEquals("[]", store.findUniqueMethod("add", 1).returnType().parameters().toString());
        assertEquals("[]", store.getFieldByName("first", true).type().parameters().toString());
    }

    @Language("java")
    private static final String NESTED = """
            package a.b;
            public class Outer<A> {
                class Mid {
                    class Inner { }
                    Inner make() { return new Inner(); }
                }
                Mid mid() { return new Mid(); }
                static void use(Outer<Integer> o) {
                    var inner = o.mid().make();
                }
            }
            """;

    @DisplayName("two levels: Outer<Integer>.Mid.Inner carries Integer, Mid's own body does not")
    @Test
    public void nested() {
        TypeInfo outer = scan("a.b.Outer", NESTED);
        ParameterizedType inner = local(outer.findUniqueMethod("use", 1), 0);
        assertEquals("a.b.Outer.Mid.Inner", inner.typeInfo().fullyQualifiedName());
        assertEquals("[Type Integer]", inner.parameters().toString());
        TypeInfo mid = outer.findSubType("Mid");
        assertEquals("[]", mid.findUniqueMethod("make", 0).returnType().parameters().toString());
        assertEquals("[]", outer.findUniqueMethod("mid", 0).returnType().parameters().toString());
    }

    private static ParameterizedType local(MethodInfo mi, int i) {
        return ((LocalVariableCreation) mi.methodBody().statements().get(i)).localVariable().parameterizedType();
    }

    private static void assertOuter(String expected, ParameterizedType pt) {
        assertEquals("a.b.Holder.Store.Entry", pt.typeInfo().fullyQualifiedName());
        assertEquals(expected, pt.parameters().toString());
        assertEquals("Type Store<String>.Entry", pt.toString());
    }
}
