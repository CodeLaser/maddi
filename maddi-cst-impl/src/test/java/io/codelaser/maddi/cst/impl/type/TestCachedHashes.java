package io.codelaser.maddi.cst.impl.type;

import io.codelaser.maddi.cst.api.element.CompilationUnit;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.impl.runtime.RuntimeImpl;
import io.codelaser.maddi.support.Either;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The cached hashes of ParameterizedTypeImpl and TypeParameterImpl (docs/status/performance-2026-10-22.md) keep the
 * values Objects.hash gave, which hash-ordered iteration across the analyzer depends on; and a method's type parameter
 * is not cached before its method is inspected, whose fully qualified name its hash includes.
 */
public class TestCachedHashes {

    private static int reference(ParameterizedTypeImpl pt, int wildcardHash) {
        return Objects.hash(pt.typeParameter(), pt.typeInfo(), pt.arrays(), wildcardHash, pt.parameters());
    }

    @DisplayName("a parameterized type's hash is Objects.hash of its parts, cached")
    @Test
    public void parameterizedType() {
        Runtime runtime = new RuntimeImpl();
        CompilationUnit cu = runtime.newCompilationUnitBuilder().setPackageName("com.foo").build();
        TypeInfo box = runtime.newTypeInfo(cu, "Box");
        TypeInfo item = runtime.newTypeInfo(cu, "Item");
        ParameterizedTypeImpl itemType = new ParameterizedTypeImpl(item);
        ParameterizedTypeImpl boxOfItem = new ParameterizedTypeImpl(box, List.of(itemType));
        ParameterizedTypeImpl extendsItem = new ParameterizedTypeImpl(item, WildcardEnum.EXTENDS);
        ParameterizedTypeImpl boxOfExtends = new ParameterizedTypeImpl(box, List.<ParameterizedType>of(extendsItem));

        assertEquals(reference(itemType, 0), itemType.hashCode());
        assertEquals(reference(extendsItem, 1), extendsItem.hashCode());
        assertEquals(reference(boxOfItem, 0), boxOfItem.hashCode());
        assertEquals(reference(boxOfExtends, 0), boxOfExtends.hashCode());
        assertTrue(boxOfItem.hashStable());
        assertEquals(boxOfItem.hashCode(), boxOfItem.hashCode());
    }

    @DisplayName("a method's type parameter is cached only once its method is inspected")
    @Test
    public void methodTypeParameter() {
        Runtime runtime = new RuntimeImpl();
        CompilationUnit cu = runtime.newCompilationUnitBuilder().setPackageName("com.foo").build();
        TypeInfo owner = runtime.newTypeInfo(cu, "Owner");
        MethodInfo method = runtime.newMethod(owner, "make", runtime.methodTypeMethod());
        TypeParameterImpl t = (TypeParameterImpl) runtime.newTypeParameter(0, "T", method);
        ParameterizedTypeImpl tType = new ParameterizedTypeImpl(t, 0);
        method.builder().commitParameters();

        assertFalse(t.hashStable());
        assertFalse(tType.hashStable());

        method.builder().commit();
        assertTrue(t.hashStable());
        assertEquals(Objects.hash(0, Either.right(method)), t.hashCode());
        assertEquals(reference(tType, 0), tType.hashCode());
        assertTrue(tType.hashStable());

        TypeParameterImpl u = (TypeParameterImpl) runtime.newTypeParameter(1, "U", owner);
        assertTrue(u.hashStable());
        assertEquals(Objects.hash(1, Either.left(owner)), u.hashCode());
    }
}
