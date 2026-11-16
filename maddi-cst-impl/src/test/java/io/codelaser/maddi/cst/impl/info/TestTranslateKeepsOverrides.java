package io.codelaser.maddi.cst.impl.info;

import io.codelaser.maddi.cst.api.element.CompilationUnit;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.impl.runtime.RuntimeImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * A method copied by {@code translate} keeps the methods it overrides. {@code withMethodBody} used to drop them, so
 * an implementation whose body was rewritten no longer counted among its interface method's implementations, and
 * the interface method could be judged non-modifying from the remaining ones.
 */
public class TestTranslateKeepsOverrides {

    @DisplayName("withMethodBody keeps the overrides")
    @Test
    public void test() {
        Runtime runtime = new RuntimeImpl();
        CompilationUnit cu = runtime.newCompilationUnitBuilder().setPackageName("com.foo").build();
        TypeInfo i = runtime.newTypeInfo(cu, "I");
        MethodInfo abstractCheck = runtime.newMethod(i, "check", runtime.methodTypeAbstractMethod());
        abstractCheck.builder().setReturnType(runtime.voidParameterizedType()).commitParameters().commit();

        TypeInfo c = runtime.newTypeInfo(cu, "C");
        MethodInfo check = runtime.newMethod(c, "check", runtime.methodTypeMethod());
        check.builder().setReturnType(runtime.voidParameterizedType()).addOverrides(List.of(abstractCheck))
                .setMethodBody(runtime.newBlockBuilder().build()).commitParameters().commit();

        MethodInfo copy = check.withMethodBody(runtime.newBlockBuilder().build());
        assertNotSame(check, copy);
        assertEquals(List.of(abstractCheck), List.copyOf(copy.overrides()));
    }
}
