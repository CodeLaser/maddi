package io.codelaser.maddi.cst.impl.info;

import io.codelaser.maddi.cst.api.element.CompilationUnit;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.impl.runtime.RuntimeImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A method's overrides iterate in the order of their fully qualified names, whatever order they were added in. They
 * were a {@code Set.copyOf}, whose iteration order the JDK salts per JVM, so every "first override that ..." could
 * differ between two runs of the same code (maddi-mod#33, #47).
 */
public class TestOverridesOrder {

    @DisplayName("overrides iterate by name, independent of the order they were added in")
    @Test
    public void test() {
        Runtime runtime = new RuntimeImpl();
        CompilationUnit cu = runtime.newCompilationUnitBuilder().setPackageName("com.foo").build();
        List<MethodInfo> overridden = new ArrayList<>();
        for (String name : List.of("D", "B", "E", "A", "C")) {
            TypeInfo ti = runtime.newTypeInfo(cu, name);
            MethodInfo m = runtime.newMethod(ti, "run", runtime.methodTypeAbstractMethod());
            m.builder().commitParameters().commit();
            overridden.add(m);
        }
        TypeInfo sub = runtime.newTypeInfo(cu, "Sub");
        MethodInfo run = runtime.newMethod(sub, "run", runtime.methodTypeMethod());
        run.builder().addOverrides(overridden).commitParameters().commit();

        assertEquals(List.of("com.foo.A.run()", "com.foo.B.run()", "com.foo.C.run()", "com.foo.D.run()",
                "com.foo.E.run()"), run.overrides().stream().map(MethodInfo::fullyQualifiedName).toList());
        assertThrows(UnsupportedOperationException.class, () -> run.overrides().clear());
    }
}
