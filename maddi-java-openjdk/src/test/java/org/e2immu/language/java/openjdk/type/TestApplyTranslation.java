package org.e2immu.language.java.openjdk.type;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.java.openjdk.CommonTest;
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
        TypeInfo C = scan("a.b.C", INPUT6);

        TypeInfo myException = C.findSubType("MyException");

        // {TypeParameterImpl@5277} "SELF=TP#0 in AbstractAssert" ->
        //      {ParameterizedTypeImpl@5278} "Type param SELF extends org.assertj.core.api.AbstractObjectAssert<SELF,ACTUAL>"
        // {TypeParameterImpl@5279} "SELF=TP#0 in AbstractThrowableAssert" ->
        //      {ParameterizedTypeImpl@5264} "Type ? extends org.assertj.core.api.AbstractThrowableAssert"
        // {TypeParameterImpl@5292} "SELF=TP#0 in AbstractObjectAssert" ->
        //      {ParameterizedTypeImpl@5293} "Type param SELF extends org.assertj.core.api.AbstractThrowableAssert<SELF,ACTUAL>"
        // {TypeParameterImpl@5282} "ACTUAL=TP#1 in AbstractAssert" ->
        //      {ParameterizedTypeImpl@5283} "Type param ACTUAL"
        // {TypeParameterImpl@5284} "ACTUAL=TP#1 in AbstractThrowableAssert" ->
        //      {ParameterizedTypeImpl@5285} "Type a.b.C.MyException"
        // {TypeParameterImpl@5286} "ACTUAL=TP#1 in AbstractObjectAssert" ->
        //      {ParameterizedTypeImpl@5287} "Type param ACTUAL extends Throwable"

        // class hierarchy is  ATA -> AOA -> AA
    }
}
