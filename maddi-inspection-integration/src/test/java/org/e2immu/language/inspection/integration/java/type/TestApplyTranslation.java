package org.e2immu.language.inspection.integration.java.type;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.AbstractThrowableAssert;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestApplyTranslation extends CommonTest {
    @Language("java")
    private static final String INPUT6 = """
            package a.b;
            import org.assertj.core.api.AbstractThrowableAssert;
            import static org.assertj.core.api.Assertions.assertThat;
            import static org.junit.jupiter.api.Assertions.assertThrows;
            class C {
                 static class MyException extends RuntimeException {
                    long errorCode;
                    MyException(long ec) {
                        this.errorCode = ec;
                    }
                    static final long EC = 5;
                }
                void throwsTheException() {
                    throw new MyException(MyException.EC);
                }
                void method1() {
                    MyException exception = assertThrows(MyException.class, ()-> throwsTheException());
                    AbstractThrowableAssert<?,a.b.C.MyException> notNull = assertThat(exception).isNotNull();
                    notNull.extracting(ex -> ex.errorCode).isEqualTo(MyException.EC);
                }
            }
            """;

    @DisplayName("test related to TestLambda,6: recursive resolution of SELF type parameters")
    @Test
    public void test() {
        TypeInfo C = javaInspector.parse(INPUT6);
        Runtime runtime = javaInspector.runtime();

        TypeInfo aa = javaInspector.compiledTypesManager().getOrLoad(AbstractAssert.class);
        ParameterizedType pt = aa.asParameterizedType();
        assertEquals("""
                Type org.assertj.core.api.AbstractAssert<SELF extends org.assertj.core.api.AbstractAssert<SELF,ACTUAL>,ACTUAL>\
                """, pt.toString());
        TypeParameter aa0 = aa.typeParameters().getFirst();
        TypeParameter aa1 = aa.typeParameters().getLast();
        ParameterizedType aa0Pt = runtime.newParameterizedType(aa0, 0, null);
        assertEquals("Type param SELF extends org.assertj.core.api.AbstractAssert<SELF,ACTUAL>",
                aa0Pt.toString());
        TypeInfo aoa = javaInspector.compiledTypesManager().getOrLoad(AbstractObjectAssert.class);
        TypeParameter aoa0 = aoa.typeParameters().getFirst();
        TypeParameter aoa1 = aoa.typeParameters().getLast();

        ParameterizedType aoa0Pt = runtime.newParameterizedType(aoa0, 0, null);
        assertEquals("""
                Type param SELF extends org.assertj.core.api.AbstractObjectAssert<SELF,ACTUAL>\
                """, aoa0Pt.toString());
        ParameterizedType aoa1Pt = runtime.newParameterizedType(aoa1, 0, null);
        assertEquals("Type param ACTUAL", aoa1Pt.toString());

        TypeInfo ata = javaInspector.compiledTypesManager().getOrLoad(AbstractThrowableAssert.class);
        TypeParameter ata0 = ata.typeParameters().getFirst();
        TypeParameter ata1 = ata.typeParameters().getLast();
        ParameterizedType ata0Pt = runtime.newParameterizedType(ata0, 0, null);
        assertEquals("Type param SELF extends org.assertj.core.api.AbstractThrowableAssert<SELF,ACTUAL>", ata0Pt.toString());
        ParameterizedType ata1Pt = runtime.newParameterizedType(ata1, 0, null);
        assertEquals("Type param ACTUAL extends Throwable", ata1Pt.toString());
        ParameterizedType qAta = runtime.newParameterizedType(ata, 0, runtime.wildcardExtends(), List.of());
        assertEquals("Type ? extends org.assertj.core.api.AbstractThrowableAssert", qAta.toString());

        TypeInfo myException = C.findSubType("MyException");

        //{TypeParameterImpl@5277} "SELF=TP#0 in AbstractAssert" ->
        //      {ParameterizedTypeImpl@5278} "Type param SELF extends org.assertj.core.api.AbstractObjectAssert<SELF,ACTUAL>"
        //{TypeParameterImpl@5279} "SELF=TP#0 in AbstractThrowableAssert" ->
        //      {ParameterizedTypeImpl@5264} "Type ? extends org.assertj.core.api.AbstractThrowableAssert"
        //{TypeParameterImpl@5292} "SELF=TP#0 in AbstractObjectAssert" ->
        //      {ParameterizedTypeImpl@5293} "Type param SELF extends org.assertj.core.api.AbstractThrowableAssert<SELF,ACTUAL>"
        //{TypeParameterImpl@5282} "ACTUAL=TP#1 in AbstractAssert" ->
        //      {ParameterizedTypeImpl@5283} "Type param ACTUAL"
        //{TypeParameterImpl@5284} "ACTUAL=TP#1 in AbstractThrowableAssert" ->
        //      {ParameterizedTypeImpl@5285} "Type a.b.C.MyException"
        //{TypeParameterImpl@5286} "ACTUAL=TP#1 in AbstractObjectAssert" ->
        //      {ParameterizedTypeImpl@5287} "Type param ACTUAL extends Throwable"

        // class hierarchy is  ATA -> AOA -> AA

        Map<NamedType, ParameterizedType> map = new HashMap<>();
        map.put(aa0, aoa0Pt);
        map.put(aa1, aoa1Pt);
        map.put(aoa0, ata0Pt);
        map.put(aoa1, ata1Pt);
        map.put(ata0, qAta);
        map.put(ata1, myException.asParameterizedType());
        ParameterizedType translatedTp = aa0Pt.applyTranslation(runtime, map);
        // FIXME
        //  what we expect the algorithm to do is to go up in the resolution of type parameters, to AOA and then to ATA
        assertEquals("""
                Type org.assertj.core.api.AbstractThrowableAssert<?,a.b.C.MyException>\
                """, translatedTp.toString());
    }
}
